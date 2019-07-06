/*
 * 
 * Vear 2017  * 
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
package jb2.ent;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.GameMatch;
import jb2.jo.JOAdd;
import jb2.jo.TypeMap;
import jb2.math.BoundingBox;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class VIPNodeEntity extends Entity implements EntityEventHandler {
    
    protected static final Logger log = Logger.getLogger(VIPNodeEntity.class.getName());
    
    public static final boolean botTakeOver = false;
    
    public final int lfpGroup;
    public int flagHeight;
    public int lfpCampState=0;
    public long lastCampCheckTime=0;
    public int[] teamCountInside = new int[3];
    //public FastList<BotEntity> respawnQueue= new FastList<>();;
    public float mindDistToTarget;
    // the original wait time
    public long waitTime = 0;
    public BoundingBox influenceBox = new BoundingBox();
    
    public VIPNodeEntity(int itc, long ssnptr) {
        super(itc, ssnptr, 0, false);
        lfpGroup = ssnbase.getByte(JOAdd.GameObjectStruct.LFPGroup.value);
        typeFolder = TypeMap.TypeFolder.LFP;
    }
    
    @Override
    public void refresh() {
        super.refresh();
        
        // set influence box size
        influenceBox.set(bounds);
        if(this.radius!=0) {
            influenceBox.extents.set(radius, influenceBox.extents.y, radius);
        }
        
        // read in the fully campedness
        if(lfpGroup!=0) {
            flagHeight = ssnbase.getInt(JOAdd.GameObjectStruct.LFPFlagheight);
            if(flagHeight == 0) {
                lfpCampState = -1;
            } else if(flagHeight==65536) {
                lfpCampState = 1;
            } else {
                lfpCampState = 0;
            }
        }

        state.lfpCampState = lfpCampState;
        state.lfpGroup = lfpGroup;
        
        if(prevState==null || state.team != prevState.team
                || state.lfpCampState != prevState.lfpCampState) {
            // mark in GameMatch, that VIP nodes needs recalcualted
            if(AppContext.gameMatch!=null)
                AppContext.gameMatch.vipNodeRecalcNeeded = true;
            state.changed = true;
        }
    }
    
    @Override
    protected void processEvents() {
        if(prevState!=null && state.lfpCampState != prevState.lfpCampState) {
            campedStateChangeEvent();
            return;
        }
        super.processEvents();
    }

    @Override
    public void startEvent() {
        // we wake up every 2 seconds to check and adjust the team an flag height
        this.idleTime = 2000;
        findWorkToDo();
    }

    @Override
    public void movedEvent() {
        //no way
    }

    @Override
    public void teamChangedEvent() {
        findWorkToDo();
        // reset weight stats
        AppContext.routeFinder.decayMapWeights(true);
    }

    @Override
    public void deadEvent() {
        
    }

    @Override
    public void aliveEvent() {
        
    }

    @Override
    public void attachedEvent() {
        
    }

    @Override
    public void detachedEvent() {
        
    }

    @Override
    public void idleEvent() {
        // do work
        findWorkToDo();
    }

    private void campedStateChangeEvent() {
        findWorkToDo();
    }

    protected void findWorkToDo() {
        if(lfpGroup==0) {
            this.idleTime = 10000;
            return;
        }

        // check how much time passed since last run
        if(lastCampCheckTime==0) {
            lastCampCheckTime = AppContext.gameMatch.timestamp;
            return;
        }
        
        long campPassedTime = AppContext.gameMatch.timestamp - lastCampCheckTime;
        if(campPassedTime<2000) {
            // don't check sooner than 2 seconds
            return;
        }
        lastCampCheckTime = AppContext.gameMatch.timestamp;
        
        // subtract the check time from queue wait time
        waitTime -= campPassedTime;
        if(waitTime<=0)
            waitTime=0;
        
        // only influence flag height, if it is in the contested group
        if(!AppContext.gameMatch.isContestedLFP(lfpGroup))
            return;
        
        boolean onlyPlayers = true;
        Arrays.fill(teamCountInside, 0);
        // get all entities inside the influence radius of LFP
        FastList<Entity> allEnts = AppContext.mapOctreeEntities.getEntities2d(influenceBox, null);
        for(int i=0; i<allEnts.size; i++) {
            Entity ent = allEnts.get(i);
            // check that its a player or bot
            if(ent.itemClass!=0)
                continue;
            if(ent.team !=1 && ent.team != 2)
                continue;
            // check that it is actually inside the radius
            float dist = this.position.distance(ent.position);
            if(dist>this.radius)
                continue;            
            // depending on the team increase the counter
            teamCountInside[ent.team]++;
            // if bot, then reset the flag
            if(ent.isBot())
                onlyPlayers=false;
        }
        
        if(onlyPlayers) {
            // only players on LFP, let the game calculate the LFP itself
            return;
        }
        // calc which direction to move the flag, depending on the difference in
        // bots&players inside
        int diff = 0;
        int neutralTeam=0;
        if(this.team==0) {
            diff = teamCountInside[1] - teamCountInside[2];
            if(diff>0) {
                neutralTeam = 1;
                diff = -diff;
            } else {
                neutralTeam = 2;
            }
        } else if(this.team==1) {
            diff = teamCountInside[1] - teamCountInside[2];
        } else {
            diff = teamCountInside[2] - teamCountInside[1];
        }
        // add the diff value to flag, multiplied by seconds passed
        int newFlagHeight = (int) (flagHeight + ((diff*910f)* ((float)campPassedTime/1000f)));
        int newTeam = team;
        
        if(botTakeOver) {
            // clamp the value
            if(newFlagHeight<0) {
                // switch the side
                newFlagHeight = -newFlagHeight;
                if(neutralTeam!=0) {
                    newTeam = neutralTeam;
                } else if(this.team==1) {
                    newTeam = 2;
                } else if(this.team==2) {
                    newTeam = 1;
                }
            }
            // clam pthe max value
            if(newFlagHeight>65536)
                newFlagHeight=65536;
            if(newTeam!=this.team) {
                // TODO: bot capturing is buggy, does not advance to the next LFP group
                // so ignore for now
                //ssnbase.setByte(JOAdd.GameObjectStruct.Team, (byte) newTeam);
            }
        } else {
            if(newFlagHeight<0 && newFlagHeight<flagHeight)
                newFlagHeight=0;
            if(newFlagHeight>65536 && newFlagHeight>flagHeight)
                newFlagHeight=65536;
        }
        // change the flag height and team of the LFP
        if(newFlagHeight!=this.flagHeight) {
            ssnbase.setInt(JOAdd.GameObjectStruct.LFPFlagheight, newFlagHeight);
        }
    }
    
}
