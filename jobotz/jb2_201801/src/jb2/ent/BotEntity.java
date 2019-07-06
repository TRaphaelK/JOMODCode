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

import jb2.gai.EntParameters;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.jo.JOAdd;
import jb2.jo.TypeMap;
import jb2.map.NavCell;
import jb2.map.NavLink;
import jb2.map.NavRoute;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.JbException;

/**
 * Implements real-time logic, tracks real-time events, movements, combat, route
 * following, death, respawn, pausing. Higher order tracking/commanding is done
 * by Behavior classes
 *
 * @author vear
 */
public class BotEntity extends Entity implements EntityEventHandler {

    protected static final Logger log = Logger.getLogger(BotEntity.class.getName());

    protected enum BotState {
        FindTarget,
        NoRouteStayPut,
        FollowRoute,
        Roam,
        RoamEnd,
        Respawn,
    }

    // the dedicated move marker of the bot
    public Waypoint moveMarker;

    // the bots own pathfinding hyperparameters, subject to
    // optimization 
    public EntParameters navParams;

    // route was requested, another thread works on it
    public NavRoute navRoute = null;

    // the various trackers and handlers
    protected FastList<EntityEventHandler> handlers;

    // the entity tracker for the bot
    //protected EntityTracker tracker;
    // the performance tracker for the bot
    //protected BotPerformanceTracker perfTracker;
    // we start by trying to find a target
    protected BotState botState;
    protected EntityEventHandler currentBehavior;
    // if the bot is being moved (route following, roaming), this the link it is supposedly following
    protected NavLink targetLink;

    // is the bot blinded
    boolean blinded = true;

    public BotEntity(int SSN) {
        super(SSN);
    }

    public BotEntity(int itc, long ssnptr) {
        super(itc, ssnptr, 0, false);
        typeFolder = TypeMap.TypeFolder.AI;
        handlers = new FastList<>();
        EntityTracker tracker = new EntityTracker(this);
        handlers.add(tracker);
        BotPerformanceTracker perfTracker = new BotPerformanceTracker(this);
        handlers.add(perfTracker);
    }

    // event handlers
    @Override
    public void startEvent() {
        idleTime = FastMath.rand.nextInt(2 * 1000);

        // reset bot blindness
        setAggro();
        // set bot to stop
        this.setStop();

        // special respawn when game starts, bot is already alive,
        // but still consider given number of bots for respawn
        // if the designated respawn position is an lfp, then move the bot there
        // if not, then just leave the bot where it is
        if (AppContext.gameMatch.startRespawn) {
            // set respawn position
            findRespawnNode();
        }

        // forward start event to handlers
        handlers.forEachEntry(f -> f.startEvent());

        navRoute = null;

        this.currentBehavior = null;
        this.setBehavior();
        // startevent on behaviors is called when a new behavior is activated
    }

    @Override
    public void movedEvent() {
        idleTime = FastMath.rand.nextInt(1 * 1000);

        handlers.forEachEntry(f -> f.movedEvent());

        this.setBehavior();
        if (currentBehavior != null) {
            currentBehavior.movedEvent();
        }
    }

    @Override
    public void teamChangedEvent() {
        // shouldn't ever happen
        handlers.forEachEntry(f -> f.teamChangedEvent());
    }

    @Override
    public void deadEvent() {
        log.log(Level.INFO, "Bot {0} died", this.SSN);

        idleTime = (2 * 1000) + FastMath.rand.nextInt(3 * 1000);
        // clear route
        navRoute = null;
        this.setStop();

        // mark death
        handlers.forEachEntry(f -> f.deadEvent());

        // reset any behavior
        setBehavior();
    }

    @Override
    public void aliveEvent() {
        log.log(Level.INFO, "Bot {0} respawned", this.SSN);
        // default idle time
        idleTime = FastMath.rand.nextInt(2 * 1000);
        navRoute = null;

        // set respawn position
        findRespawnNode();
        this.setStop();

        setAggro();
        handlers.forEachEntry(f -> f.aliveEvent());

        this.currentBehavior = null;
        this.setBehavior();
        if (currentBehavior != null) {
            currentBehavior.aliveEvent();
        }
    }

    @Override
    public void attachedEvent() {
        log.log(Level.INFO, "Bot {0} attached to {1}", new Object[]{this.SSN, this.attachedTo.SSN});
        idleTime = (2 * 1000) + FastMath.rand.nextInt(3 * 1000);

        // clear route
        navRoute = null;

        handlers.forEachEntry(f -> f.attachedEvent());

        // if atttached to emplaced consider for how long its worth to stay there
        // if attached to vehicle check if there is a driver
        this.setBehavior();
        if (currentBehavior != null) {
            currentBehavior.attachedEvent();
        }
    }

    @Override
    public void detachedEvent() {
        log.log(Level.INFO, "Bot {0} detached", this.SSN);
        idleTime = FastMath.rand.nextInt(2 * 1000);
        navRoute = null;

        handlers.forEachEntry(f -> f.detachedEvent());

        this.setBehavior();
        if (currentBehavior != null) {
            currentBehavior.detachedEvent();
        }
    }

    @Override
    public void idleEvent() {
        idleTime = FastMath.rand.nextInt(3 * 1000);
        //log.log(Level.INFO, "Bot {0} idle", this.SSN);        
        handlers.forEachEntry(f -> f.idleEvent());

        this.setBehavior();
        if (currentBehavior != null) {
            currentBehavior.idleEvent();
        }
    }

    protected void setAggro() {
        // unblind the bot
        this.setBotVision((short) 150);
        this.setRunning();
        this.blinded = false;
    }

    protected void setDeagro() {
        this.setBotVision((short) 0);
        this.blinded = true;
        log.log(Level.INFO, "Bot {0} deagro", SSN);
    }

    protected void setBehavior() {

        if (this.dead) {
            botState = BotState.Respawn;
            currentBehavior = null;
            return;
        }
        if (botState == BotState.Respawn) {
            botState = null;
        }
        if (this.attachedTo != null) {
            botState = BotState.NoRouteStayPut;
        }
        // default is to search for target
        if (botState == null) {
            botState = BotState.FindTarget;
        }
        if (botState == BotState.FindTarget
                || botState == BotState.RoamEnd) {
            if (currentBehavior == null
                    || !(currentBehavior instanceof RouteFindBehavior)) {
                currentBehavior = new RouteFindBehavior(this);
                //log.log(Level.INFO, "Bot {0} switched to RouteFindBehavior", this.SSN);
                currentBehavior.startEvent();
            }
            if (botState == BotState.FindTarget) {
                return;
            }
        }
        if (botState == BotState.FollowRoute) {
            if (currentBehavior == null
                    || !(currentBehavior instanceof RouteFollowBehavior)) {
                currentBehavior = new RouteFollowBehavior(this);
                //log.log(Level.INFO, "Bot {0} switched to RouteFollowBehavior", this.SSN);
                currentBehavior.startEvent();
            }
            if (botState == BotState.FollowRoute) {
                return;
            }
        }
        /*
        if (botState == BotState.NoRouteStayPut) {
            if (currentBehavior == null
                    || !(currentBehavior instanceof BotWaitBehavior)) {
                // switch to find target after some waiting
                currentBehavior = new BotWaitBehavior(this, BotState.Roam);
                //log.log(Level.INFO, "Bot {0} switched to BotWaitBehavior", this.SSN);
                currentBehavior.startEvent();
            }
            return;
        }
        */
        if (botState == BotState.Roam
                || botState == BotState.NoRouteStayPut) {
            // clear the parameters
            endEvaluation();

            if (currentBehavior == null
                    || !(currentBehavior instanceof BotRoamBehavior)) {
                currentBehavior = new BotRoamBehavior(this);
                //log.log(Level.INFO, "Bot {0} switched to RouteFollowBehavior", this.SSN);
                currentBehavior.startEvent();
                // TODO: no params for roam
                // request new params
                //requestNewParams();
            }
            return;
        }
        /*
        if (botState == BotState.RoamEnd) {
            if (currentBehavior == null
                    || !(currentBehavior instanceof BotWaitBehavior)) {
                // switch to find target after some waiting
                currentBehavior = new BotWaitBehavior(this, BotState.FindTarget);
                currentBehavior.startEvent();
            }
            return;
        }*/
    }

    public void findRespawnNode() {
        int priorityWait = 0;
        FastList<VIPNodeEntity> respawns = AppContext.gameMatch.findRespawnPosition(this.team);
        while (respawns.size > 0) {
            VIPNodeEntity respawnNode = respawns.remove(0);
            
            // the nearest to the combat, the more limited is the respawn queue
            // or: the further from the combat, the less we care that the respawn time
            // for the queu has passed
            if(priorityWait==0) {
                priorityWait=1;
            } else {
                priorityWait *=2;
            }
            
            if(AppContext.gameMatch.startRespawn) {
                // spawn 10 bots at start
                priorityWait = 10;
            }
        
            
            // too much waiting on the node, ignore
            if (respawnNode.waitTime > priorityWait * 10 * 1000) {
                continue;
            }

            // find a nav node on the 
            Vector3f respPos = new Vector3f();
            BoundingBox respawnBounds = new BoundingBox();
            respawnBounds.center.set(respawnNode.position);
            respawnBounds.extents.y = 2f;
            respawnBounds.extents.x = FastMath.max(respawnNode.bounds.extents.x, 15);
            respawnBounds.extents.z = respawnBounds.extents.x;
            FastList<NavCell> nodes = AppContext.navGrid.getCells(respawnBounds, null);
            NavCell node=null;
            while(nodes.size > 0) {
                int idx = FastMath.rand.nextInt(nodes.size);
                node = nodes.remove(idx);
                if(!node.isOnLandLevel()) {
                    node=null;
                    continue;
                }
                break;
            }
            /*
            int i = 0;
            boolean found = false;
            while (i < 10) {
                // write the spawn position
                // TODO: change this to random position inside the bounds of the node
                respawnBounds.fillRandomPointInside(respPos);
                if (respawnNode.bounds.center.distance2d(respPos) < 5f) {
                    i++;
                    // don't spawn into the center of LFP
                    continue;
                }
                // find terrain height at position
                respPos.y = AppContext.heightMap.getHeight(respPos);
                if (respPos.y == 0) {
                    i++;
                    continue;
                }

                found = true;
                break;
            }
            if (!found) {
                respawnBounds.fillRandomPointInside(respPos);
                respPos.y = respawnNode.position.y;
                found = true;
              }
            */

            if (node!=null) {
                node.fillRandomPointOnLandLevel(respPos);
                // copy respawn position into bots position
                ssnbase.setVector3f(JOAdd.GameObjectStruct.RawZ.value, respPos);

                log.log(Level.FINE, "Bot {0} respawn at {1}", new Object[]{this.SSN, respawnNode.name});
                // add some wait time to the nodes respawn queue time
                respawnNode.waitTime += 10 * 1000;
                return;
            }
        }
        log.log(Level.FINE, "Bot {0} no respawn position found", this.SSN);
        // start automatic respawn on game start, as finding a respawn failed
        AppContext.gameMatch.startRespawn = false;
    }

    public void reevaluateCurrentNavCell() {
        // call teh moved event on the position tracker,
        // which should re-evaluate the bots current cell
        handlers.forEachEntry(f -> {
            if (f instanceof EntityTracker) {
                f.movedEvent();
                return;
            }
        });
    }

    public void startEvaluation(Vector3f target) {
        handlers.forEachEntry(f -> {
            if (f instanceof BotPerformanceTracker) {
                ((BotPerformanceTracker)f).startEvaluation(target);
                return;
            }
        });
    }
    
    public void endEvaluation() {
        handlers.forEachEntry(f -> {
            if (f instanceof BotPerformanceTracker) {
                ((BotPerformanceTracker)f).endEvaluation();
                return;
            }
        });
    }
}
