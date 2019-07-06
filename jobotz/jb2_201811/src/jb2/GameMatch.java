/*
 * 
 * Vear 2017-2018  * 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package jb2;

import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.ent.BotEntity;
import jb2.gai.EntParameters;
import jb2.ent.Entity;
import jb2.ent.VIPNodeEntity;
import jb2.ent.VehicleEntity;
import jb2.ent.Waypoint;
import jb2.jo.GameObject;
import jb2.jo.GameObjectFactory;
import jb2.jo.JOAdd;
import jb2.jo.ServerInfo;
import jb2.jo.TypeMap;
import jb2.jo.TypeMap.TypeFolder;
import jb2.util.FastList;
import jb2.util.IntList;


/**
 * Class for managing trough a game match
 * @author vear
 */
public class GameMatch {
    
    public static final Logger log = Logger.getLogger(GameMatch.class.getName());
    // global flags
    // do we need to recalculate VIP nodes in current loop
    //public boolean vipNodeRecalcNeeded = true;
    
    public boolean exit;
    protected boolean paused;
    
    // match start time
    public long starttime;
    // current timestamp
    public long timestamp;
    // seconds since last update
    public float frameTime;
   
    public int[] teamTargetLFPGroups = new int[2];
    public IntList contestedTargetLFPGroups = new IntList();
    public FastList<VIPNodeEntity>[] teamTargets = new FastList[]{new FastList<>(), new FastList<>()};
    public FastList<VIPNodeEntity>[] teamRespawn = new FastList[]{new FastList<>(), new FastList<>()};
    // if the match is started, and we are to respawn as many bots to forward LFP-s as possible
    // once a respawn fails, or there are no more respawn slots, the initial respawning stops
    public boolean firstFrame = true;
    
    
    // the navigation map data suitable for processing on GPU
    public GameMatch() {
        
    }
    
    public void runMatch() {

        // no need to create a new instance of serverinfo, as that does not hold any internal state
        ServerInfo si = AppContext.serverInfo;
        si.refresh();
        
        // determine map name
        if(si.missionFilename==null)
            return;
        if(!(si.serverState==JOAdd.ServerState.START
           ||si.serverState==JOAdd.ServerState.RUN)) {
            return;
        }
        
        AppContext.init();
        //LocalContext.setLogLevel(Config.logLevel);
        GameObjectFactory gof = AppContext.gameObjectFactory;
                
        String origMissionFilename = si.missionFilename;
        
        // make a base refresh on all objects
        gof.baseRefresh();
        
        exit = false;
        
        // initalize bot navigation, assign a marker to each bot and vehicle
        // check if map is enabled for bots
        // assign markers to bots
        createEntities();
        
        timestamp=starttime = System.currentTimeMillis();
        //boolean firstTime = true;
        
        // initialize event saving
        AppContext.stateFile.init();
        // init the hparams
        AppContext.optimizer.loadParams();
        
        while(!exit) {
            
            long time = System.currentTimeMillis();
            
            // calculate frame time
            frameTime = ((float)(time-timestamp))/1000.0f;
            timestamp = time;

            // refresh entities again, and populate them into the octree
            gof.refreshEntities(firstFrame);                

            // re-build the octree as neeeded
            //AppContext.navMap.build();

            if(firstFrame) {
                // load the nav nodes
                // we need to do this here, so that the dimensions of the octree are given
                AppContext.navGrid.loadNodes();
                // load or init a heightmap
                AppContext.heightMap.loadHeightMap();
                gof.refreshEntities(firstFrame);
                memoryInfo();
            }
            // check if players are on the server
            // if no players, then stop bots and pause            
            // if there are no active players, pause game
            if(gof.getPlayers().size() == 0) {
                // no players on server
                log.info("No players on server");
                if(!paused) {
                    log.info("Pausing bots");
                    // save the nodes, just in case
                    AppContext.navGrid.saveNodes();
                    AppContext.heightMap.saveHeightMap();
                    // pause bots: invalidate any tracking, and wait on the thread for a bit
                    paused=true;
                }
            } else {
                if(paused) {
                    log.info("Resuming bots");
                    paused=false;
                    //vipNodeRecalcNeeded = true;
                }
                //if(vipNodeRecalcNeeded) {
                    // determine capturable LFP groups
                    determineContested();
                    // determine potential bot targets
                    determineVIPTargets();
                    // determine bot respawn positions
                    determineRespawnTargets();
                    //vipNodeRecalcNeeded = false;
                //}

                // track players and bots, save result to file for analysis
                // trigger events
                trackObjects();
                
                // make a snapshot image
                AppContext.renderer.snapshot();
            }

            // finished processing of first frame
            firstFrame = false;
            // flush logfile
            AppContext.flushLogFile();
           // wait so that 1 or multiple of a whole second passes
           long cycletime = ( System.currentTimeMillis() - timestamp ) % 1000;
            try {
                Thread.sleep(cycletime);
            } catch (InterruptedException ex) {
            }
            // refresh the serverinfo
           si.refresh();
            // loop until map end, or map changed, or exit requested, or exception
            // process list of players, new players, dropped players
           if(si.serverState==JOAdd.ServerState.END
                   &&origMissionFilename.equals(si.missionFilename)) {
               // if we got to the map-end, then mark that we should save the navigatio nodes
               log.log(Level.INFO, "Map ending, exiting");               
               exit=true;
               continue;
           }
           if(si.serverState!=JOAdd.ServerState.RUN
                   ||!origMissionFilename.equals(si.missionFilename)) {
               log.log(Level.INFO, "Map ended, exiting");
               // if map is not running any more, exit
               exit=true;
               continue;
           }
        }

        // save the gathered nav map
        AppContext.navGrid.saveNodes();
        AppContext.heightMap.saveHeightMap();
        // running map is finished
        // save the gathered events
        AppContext.stateFile.close();
        // save the changed hparams
        // save bot genomes
        AppContext.optimizer.saveParams();
        // flush log file
        AppContext.flushLogFile();
        // cleanup
        AppContext.clear();
    }

    protected void createEntities() {
        
        GameObjectFactory gof = AppContext.gameObjectFactory;
        
        // enumerate all objects
        
        // get all markers which can be used for navigation
        // all the markers
        //TypeMap.TypeFolder.Waypoint
        FastList<GameObject> waypoints = gof.getEntities(TypeMap.TypeFolder.Waypoint, null);
        if(waypoints.isEmpty()) {
            log.severe("No waypoints availible in the map, map is ignored. Please add single waypoint markers to the map."
                    + "The number of waypoint markers should be at least as much as the number of bots and vehicles in the map. "
                    + "The initial position of the waypoint is irrelevant, the addon will move the waypoints as necessary.");
            exit=true;
            return;
        }
        
        // all the bots
        // TypeMap.TypeFolder.AI
        FastList<BotEntity> bots = gof.getBots(null);
        if(bots.isEmpty()) {
            log.severe("No bots in the map. Please add bots to the map. Set their team corresponding to 1 or 2."
                    + "Set their initial respawn position to their teams corresponding base."
                    + "Do not use any WAC script to manage the bots.");
            exit=true;
            return;
        }
                
        int[] botcount = new int[2];
        // assign a waypoint to each bot
        for(int boti=0;boti<bots.size();boti++) {
            BotEntity bot = bots.get(boti);
            // get the first waypoint availible
            if(waypoints.size() > 0) {
                // remove the last element from the list of availible waypoints
                Waypoint wp = (Waypoint) waypoints.remove();
                
                // assign the waypoint to the bot
                bot.moveMarker = wp;
                botcount[bot.team-1] ++;
            } else {
                // no more waypoints availible, log warning and ignore bot
                log.log(Level.WARNING, "No more waypoints availible, bot {1} will be ignored", bot.SSN);
                // remove the bot, so it will be ignored
                gof.removeEntity(bot);
            }
        }
        log.log(Level.INFO, "Team 1 managing {0} bots", botcount[0]);
        log.log(Level.INFO, "Team 2 managing {0} bots", botcount[1]);
        
        int vehcount = 0;
        // all the vehicles
        //TypeMap.TypeFolder.Vehicle
        FastList<VehicleEntity> vehicles = gof.getVehicles(null);
        // assign a waypoint to each vehicle
        for(int vehi=0;vehi<vehicles.size();vehi++) {
            VehicleEntity veh = vehicles.get(vehi);
            // get the first waypoint availible
            if(waypoints.size() > 0) {
                // remove the last element from the list of availible waypoints
                Waypoint wp = (Waypoint) waypoints.remove();
                
                vehcount++;
                
                // assign the waypoint to the vehicle
                veh.moveMarker = wp;
            } else {
                // no more waypoints availible, log warning and ignore vehicle
                log.log(Level.WARNING, "No more waypoints availible, vehicle SSN={0} will be ignored", veh.SSN);
                // remove the bot, so it will be ignored
                gof.removeEntity(veh);
            }
        }
        
        log.log(Level.INFO, "Managing {0} vehicles", vehcount);
        
        // all the emplaced
        //TypeMap.TypeFolder.Emplaced
        
        // all the spawn points
        // all the VIP points
        //TypeMap.TypeFolder.LFP
        
    }
    
    protected void trackObjects() {        
        GameObjectFactory gof = AppContext.gameObjectFactory;
        
        // get list of players
        FastList gos = gof.getEntities(TypeMap.TypeFolder.Player, null);
        Entity go;
        for(int i=0; i<gos.size(); i++) {
            go=(Entity) gos.get(i);
            go.processObjectState();
        }
        
        // get list of bots
        gos = gof.getBots(gos);
        for(int i=0; i<gos.size(); i++) {
            go=(Entity) gos.get(i);
            go.processObjectState();
        }

        // vehicles
        gos = gof.getVehicles(gos);
        for(int i=0; i<gos.size(); i++) {
            go=(Entity) gos.get(i);
            go.processObjectState();
        }

        // LFP status
        gos = gof.getLFP(gos);
        for(int i=0; i<gos.size(); i++) {
            go=(Entity) gos.get(i);
            go.processObjectState();
        }
        
        // emplaceds
        gos = gof.getEntities(TypeFolder.Emplaced, gos);
        for(int i=0; i<gos.size(); i++) {
            go=(Entity) gos.get(i);
            go.processObjectState();
        }        
    }
    
    protected void determineContested() {
        IntList groupTeam = new IntList(20);
        groupTeam.clear();
        
        int prevTargetGroup = -1;
        if(contestedTargetLFPGroups.size>0)
            prevTargetGroup = contestedTargetLFPGroups.get(0);
        int prevSecTargetGroup = -1;
        if(contestedTargetLFPGroups.size>1)
            prevSecTargetGroup = contestedTargetLFPGroups.get(1);
        
        contestedTargetLFPGroups.clear();
        
        // the possible VIP nodes to consider as targets
        FastList<VIPNodeEntity> targets = AppContext.gameObjectFactory.getLFP(null);
        // determine lowest LFP group, get its team
        int lowestLfpGroup = -1;
        int highestLfpGroup = -1;
        int lowestLfpTeam = -1;
        int highestLfpTeam = -1;

        for (int i = 0; i < targets.size(); i++) {
            VIPNodeEntity vipEnt = targets.get(i);
            // reset all flags on the VIP entity
            
            int vipGroup = vipEnt.lfpGroup;
            // no LFP group ignore
            if (vipGroup == 0) {
                continue;
            }
            groupTeam.ensureCapacity(vipGroup + 1);
            int vipTeam = vipEnt.team;
            if(vipTeam==0) vipTeam = -1;

            int currGroupTeam = groupTeam.get(vipGroup);
            if (currGroupTeam == 0) {
                // team not yet set, set it
                groupTeam.set(vipGroup, vipTeam);
            } else if (vipTeam == -1) {
                // there is a neutral LFP, the whole group is netutral
                groupTeam.set(vipGroup, vipTeam);
            } else if (vipTeam != currGroupTeam) {
                // set the lfp group to neutral, as there is a mix of
                // team ownership there
                groupTeam.set(vipGroup, -1);
            }

            if (lowestLfpGroup == -1 || vipGroup < lowestLfpGroup) {
                // the lowest LFP group
                lowestLfpGroup = vipGroup;
                // to which team it belongs
                lowestLfpTeam = vipTeam;
            }
            if (highestLfpGroup == -1 || vipGroup > highestLfpGroup) {
                highestLfpGroup = vipGroup;
            }
        }

        // no LFP groups, exit
        if (lowestLfpGroup == -1) {
            return;
        }

        // the second team index
        highestLfpTeam=lowestLfpTeam==1?2:1;

        // from the find the next LFG group that is either
        // netural or mixed
        // of the enemy
        int firstTeamTargetGroup = -1;
        int secondTeamTargetGroup = -1;
        
        for (int i = lowestLfpGroup; i <= highestLfpGroup; i++) {
            int team = groupTeam.get(i);
            if(team==0) {
                // group is not used, ignore
                continue;
            }
            // by default set whatever comes
            if (firstTeamTargetGroup == -1) {
                firstTeamTargetGroup = i;
            }
            if (secondTeamTargetGroup == -1) {
                secondTeamTargetGroup = i;
            }
            if (groupTeam.get(firstTeamTargetGroup) == lowestLfpTeam) {
                // overwtire first teams target if this is the first
                // group that is not owned by the first team
                firstTeamTargetGroup = i;
            }
            if (team!=highestLfpTeam) {
                // overwrite second team target if we got a higher grouped target
                secondTeamTargetGroup = i;
            }
        }
        contestedTargetLFPGroups.add(firstTeamTargetGroup);
        if(prevTargetGroup!=firstTeamTargetGroup) {
            log.log(Level.INFO, "Contested LFP group {0}", firstTeamTargetGroup);
        }
        if(secondTeamTargetGroup!=firstTeamTargetGroup) {
            contestedTargetLFPGroups.add(secondTeamTargetGroup);
            if(prevSecTargetGroup!=secondTeamTargetGroup) {
                log.log(Level.INFO, "Contested LFP group {0}", secondTeamTargetGroup);
            }
        }
    }
    
    public boolean isContestedLFP(int group ) {
        return contestedTargetLFPGroups.contains(group);
    }

    protected void determineVIPTargets() {
        // clear previous targets
        teamTargets[0].clear(); // team 1 targets
        teamTargets[1].clear(); // team 2 targets

        // the team of specific LFP groups
        IntList groupTeam = new IntList(20);
        groupTeam.clear();

        // the possible VIP nodes to consider as targets
        FastList<VIPNodeEntity> targets = AppContext.gameObjectFactory.getLFP(null);
        // determine lowest LFP group, get its team
        int lowestLfpGroup = -1;
        int highestLfpGroup = -1;
        int lowestLfpTeam = -1;
        int highestLfpTeam = -1;

        for (int i = 0; i < targets.size(); i++) {
            VIPNodeEntity vipEnt = targets.get(i);
            // reset all flags on the VIP entity
            
            int vipGroup = vipEnt.lfpGroup;
            // no LFP group ignore
            if (vipGroup == 0) {
                continue;
            }
            groupTeam.ensureCapacity(vipGroup + 1);
            int vipTeam = vipEnt.team;
            if(vipTeam==0) vipTeam = -1;

            int currGroupTeam = groupTeam.get(vipGroup);
            if(vipEnt.lfpCampState!=1) {
                // group has an LFP that is not fully camped
                // consider the group as if neutral
                groupTeam.set(vipGroup, -1);
            } else if (currGroupTeam == 0) {
                // team not yet set, set it
                groupTeam.set(vipGroup, vipTeam);
            } else if (vipTeam == -1) {
                // there is a neutral LFP, the whole group is netutral
                groupTeam.set(vipGroup, vipTeam);
            } else if (vipTeam != currGroupTeam) {
                // set the lfp group to neutral, as there is a mix of
                // team ownership there
                groupTeam.set(vipGroup, -1);
            }

            if (lowestLfpGroup == -1 || vipGroup < lowestLfpGroup) {
                // the lowest LFP group
                lowestLfpGroup = vipGroup;
                // to which team it belongs
                lowestLfpTeam = vipTeam;
            }
            if (highestLfpGroup == -1 || vipGroup > highestLfpGroup) {
                highestLfpGroup = vipGroup;
            }
        }

        // no LFP groups, exit
        if (lowestLfpGroup == -1) {
            return;
        }

        // the second team index
        highestLfpTeam=lowestLfpTeam==1?2:1;

        // from the find the next LFG group that is either
        // netural or mixed
        // of the enemy
        int firstTeamTargetGroup = -1;
        int secondTeamTargetGroup = -1;
        
        for (int i = lowestLfpGroup; i <= highestLfpGroup; i++) {
            int team = groupTeam.get(i);
            if(team==0) {
                // group is not used, ignore
                continue;
            }
            // by default set whatever comes
            if (firstTeamTargetGroup == -1) {
                firstTeamTargetGroup = i;
            }
            if (secondTeamTargetGroup == -1) {
                secondTeamTargetGroup = i;
            }
            if (groupTeam.get(firstTeamTargetGroup) == lowestLfpTeam) {
                // overwtire first teams target if this is the first
                // group that is not owned by the first team
                firstTeamTargetGroup = i;
            }
            if (team!=highestLfpTeam) {
                // overwrite second team target if we got a higher grouped target
                secondTeamTargetGroup = i;
            }
        }
        
        if(teamTargetLFPGroups[lowestLfpTeam-1]!=firstTeamTargetGroup) {
            log.log(Level.INFO, "Team {0} target LFP group {1}", new Object[] {lowestLfpTeam, firstTeamTargetGroup});
        }
        if(teamTargetLFPGroups[highestLfpTeam-1]!=secondTeamTargetGroup) {
            log.log(Level.INFO, "Team {0} target LFP group {1}", new Object[] {highestLfpTeam, secondTeamTargetGroup});
        }
        
        teamTargetLFPGroups[lowestLfpTeam-1]=firstTeamTargetGroup;
        teamTargetLFPGroups[highestLfpTeam-1]=secondTeamTargetGroup;

        // can we expect attack targets in first teams target group
        if(groupTeam.get(firstTeamTargetGroup) != lowestLfpTeam) {
            // get and add all non team LFP-s as target
            for (int i = 0; i < targets.size(); i++) {
                VIPNodeEntity vipEnt = targets.get(i);
                if(vipEnt.lfpGroup!=firstTeamTargetGroup)
                    continue;
                if(vipEnt.team == lowestLfpTeam
                        && vipEnt.lfpCampState==1)
                    continue;
                teamTargets[lowestLfpTeam-1].add(vipEnt);
            }
        } else {
            // just add all LFP-s in the group as target
            for (int i = 0; i < targets.size(); i++) {
                VIPNodeEntity vipEnt = targets.get(i);
                if(vipEnt.lfpGroup!=firstTeamTargetGroup)
                    continue;
                teamTargets[lowestLfpTeam-1].add(vipEnt);
            }
        }
        
        if(groupTeam.get(secondTeamTargetGroup) != highestLfpTeam) {
            // get and add all non team LFP-s as target
            for (int i = 0; i < targets.size(); i++) {
                VIPNodeEntity vipEnt = targets.get(i);
                if(vipEnt.lfpGroup!=secondTeamTargetGroup)
                    continue;
                if(vipEnt.team == highestLfpTeam
                        && vipEnt.lfpCampState==1)
                    continue;
                teamTargets[highestLfpTeam-1].add(vipEnt);
            }
        } else {
            // just add all LFP-s in the group as target
            for (int i = 0; i < targets.size(); i++) {
                VIPNodeEntity vipEnt = targets.get(i);
                if(vipEnt.lfpGroup!=secondTeamTargetGroup)
                    continue;
                teamTargets[highestLfpTeam-1].add(vipEnt);
            }
        }
    }

    public int getTargetLFPGroup(int team) {
        return teamTargetLFPGroups[team-1];
    }

    protected void determineRespawnTargets() {
        VIPNodeEntity team0respawn = null;
        VIPNodeEntity team1respawn = null;
        if(teamRespawn[0].size()>0)
            team0respawn = teamRespawn[0].get(0);
        if(teamRespawn[1].size()>0)
            team1respawn = teamRespawn[1].get(0);
        
        teamRespawn[0].clear();
        teamRespawn[1].clear();

        //FastList<VIPNodeEntity> checkQueues = new FastList<>();
        // get all respawn locations
        FastList<VIPNodeEntity> allEnts = AppContext.gameObjectFactory.getLFP(null);
        for(int i=0; i<allEnts.size; i++) {
            VIPNodeEntity ent = allEnts.get(i);
            if(ent.team!=1 && ent.team!=2)
                continue;
            if(ent.lfpGroup==0)
                continue;
            // if not fully camped, remove as possible respawn target
            if(ent.lfpCampState!=1) {
                // if this is not a valid respawn point any more,
                // its queue needs to be checked
                //if(!ent.respawnQueue.isEmpty())
                //    checkQueues.add(ent);
                ent.waitTime=0;
                continue;
            }
            ent.mindDistToTarget = Float.MAX_VALUE;
            // get all targets for given team, get the shortest as 
            for(int j=0; j<teamTargets[ent.team-1].size; j++) {
                VIPNodeEntity targ = teamTargets[ent.team-1].get(j);
                float dist = ent.position.distance2d(targ.position);
                if(dist < ent.mindDistToTarget)
                    ent.mindDistToTarget = dist;
            }
            teamRespawn[ent.team-1].add(ent);
        }

        // go trough and get the respawn postion with least cost, that is not full
        for(int team=0; team<2; team++) {
            // sort the respawn targets according to cost
            teamRespawn[team].sort((Comparator) (Object o1, Object o2) -> {
                VIPNodeEntity ent = (VIPNodeEntity) o1;
                VIPNodeEntity ent2 = (VIPNodeEntity) o2;
                return Float.compare(ent.mindDistToTarget, ent2.mindDistToTarget);
            });
            VIPNodeEntity teamCurrentRespawn = teamRespawn[team].get(0);

            if(teamCurrentRespawn!=null 
                    && ((team==0 && teamCurrentRespawn != team0respawn) 
                        || (team==1 && teamCurrentRespawn != team1respawn))) {
                log.log(Level.INFO, "Team {0} best respawn LFP {1}", 
                        new Object[]{team, teamCurrentRespawn.name});
            }
        }
    }

    public void memoryInfo() {
        System.gc();
        long freem=java.lang.Runtime.getRuntime().freeMemory()/(1024*1024);
        long used=(java.lang.Runtime.getRuntime().totalMemory()/(1024*1024)) - freem;
        long max =java.lang.Runtime.getRuntime().maxMemory()/(1024*1024);
        log.log(Level.INFO, "Memory (free/used/max): {0}M/{1}M/{2}m", 
                new Object[]{
                    freem, 
                    String.valueOf(used), 
                    String.valueOf(max)
                });
    }
    
    public FastList<VIPNodeEntity> findRespawnPosition(int team) {
        // recalcualte best respawn position
        FastList<VIPNodeEntity> respawns = new FastList<>();
        respawns.addAll(teamRespawn[team-1]);
        return respawns;
    }
    

}
