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
package jb2.ent;

import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.ent.BotEntity.BotState;
import jb2.map.NavCell;
import jb2.map.NavLink;
import jb2.math.AngleArea;
import jb2.math.Vector2f;

/**
 *
 * @author vear
 */
public class RouteFollowBehavior implements EntityEventHandler {

    protected static final Logger log = Logger.getLogger(RouteFollowBehavior.class.getName());
    protected BotEntity parent;

    // since when is the bot stuck?
    protected long stuckTime = 0;
    // how much we wait to unstuck
    //protected long stuckWaitTime=5*1000;
    // how much we wait until we proclaim it failed
    protected long stuckFailTime = 15 * 1000;
    protected boolean moved = false;

    public RouteFollowBehavior(BotEntity parent) {
        this.parent = parent;
    }

    @Override
    public void startEvent() {
        parent.targetLink=null;
        findWorkToDo();
        parent.idleTime = 1000;
    }

    @Override
    public void movedEvent() {
        moved = true;
        findWorkToDo();
        parent.idleTime = 1000;
    }

    @Override
    public void teamChangedEvent() {

    }

    @Override
    public void deadEvent() {
        // treat as failed, peanalize link
        routeFailed();
        parent.botState = BotState.Respawn;
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
        moved = false;
        findWorkToDo();
        parent.idleTime = 1000;
    }

    protected void findWorkToDo() {
        // if no route create it
        if (parent.navRoute == null) {
            // no route, go to stay put
            parent.botState = BotEntity.BotState.Roam;
            return;
        }
        
        if(parent.currentNode == null
                || !parent.currentNode.bounds.contains(parent.position) ) {
            parent.reevaluateCurrentNavCell();
        }

        if(parent.currentNode == null) {
            // outside bounds, falling, etc
            // no route, go to stay put
            parent.botState = BotEntity.BotState.Roam;
            return;
        }

        if (parent.navRoute.unreachable) {
            if (parent.navRoute.routeStack.isEmpty()) {
                log.log(Level.WARNING, "Failed finding route from node {0} to entity {1}",
                        new Object[]{
                            parent.navRoute.start.nodeid,
                            parent.navRoute.targetEntity.SSN
                        });
                routeFailed();
                return;
            }
            log.log(Level.INFO, "Partial route size {0}", parent.navRoute.routeStack.size);
            parent.navRoute.ready = true;
            parent.navRoute.unreachable = false;
        }

        // there is a route calculating on another thread, wait
        if (!parent.navRoute.ready) {
            return;
        }

        if (parent.navRoute.routeStack.size() == 0) {
            log.log(Level.WARNING, "Route with 0 length");
            routeFailed();
            return;
        }

        if (parent.targetLink == null) {
            // penalize the nodes as they are now planned to be traversed
            parent.navRoute.routeStack.forEachEntry(cell -> {
                cell.addPlannedTraversal(parent.team);
            });

            log.log(Level.FINER, "Bot {0} following route from node {1} to node {2} length {3}",
                    new Object[]{
                        parent.SSN,
                        parent.navRoute.start.nodeid,
                        parent.navRoute.routeStack.get(0).nodeid,
                        parent.navRoute.routeStack.size()
                    });
        }

        NavCell routeCell = null;
        if(parent.navRoute.start == parent.currentNode) {
            routeCell = parent.currentNode;            
        }

        if(routeCell==null) {
            // if the current node is already in the route stack
            int idx = parent.navRoute.routeStack.indexOf(parent.currentNode);
            if (idx != -1) {
                // remove all elements after the current node from the route
                // inclusive the current node
                parent.navRoute.routeStack.size = idx;
                parent.targetLink = null;
                routeCell = parent.currentNode;
            }
        }
        
        if(routeCell == null) {
            // check if bot intersects with one of the nodes
            for (int nidx = parent.navRoute.routeStack.size() - 1; nidx >= 0; nidx--) {
                NavCell nd = parent.navRoute.routeStack.get(nidx);
                if (!nd.bounds.intersects(parent.bounds)) {
                    continue;
                }
                // found a node containing bot coordinates, remove it and all after
                parent.navRoute.routeStack.size = nidx;
                routeCell = nd;
            }
        }

        if (parent.navRoute.routeStack.isEmpty() ) {
            // end of route reached?
            parent.navRoute = null;
            //parent.setStop();
            // continue back to route finding
            parent.botState = BotEntity.BotState.FindTarget;
            return;
        }
        
        if( routeCell == null ) {
            // no link found to route
            log.log(Level.INFO, "Bot {0} current node {1} has no link to route",
                    new Object[]{parent.SSN, parent.currentNode.nodeid});
            parent.navRoute = null;
            parent.botState = BotEntity.BotState.FindTarget;
            parent.stuckCount++;
            return;
        }

        // check against the last element
        NavLink newLink = null;
        for (int nidx = parent.navRoute.routeStack.size() - 1; nidx >= 0; nidx--) {
            NavLink link = routeCell.getLink(parent.navRoute.routeStack.get(nidx));
            // by default get the last link in route
            if(newLink == null)
                newLink = link;
            if (link == null) {
                continue;
            }
            newLink = link;
            // found the relevant link the bot should follow, clear remaining elements from the route
            // as they are irrelevant now
            parent.navRoute.routeStack.size = nidx + 1;
            break;
        }
        
        if (newLink == null) {
            // no link found to route
            log.log(Level.INFO, "Bot {0} current node {1} has no link to route",
                    new Object[]{parent.SSN, parent.currentNode.nodeid});
            parent.navRoute = null;
            parent.botState = BotEntity.BotState.FindTarget;
            parent.stuckCount++;
            return;
        }
        
        NavCell setNode = null;
        
        // if the current link is the same as the found, see if bot got stuck following it
        if (newLink == parent.targetLink) {
            checkStuck();
            if(parent.navRoute == null) {
                return;
            }
            //if(parent.targetLink.to.bounds.intersects(parent.bounds) ) {
                // if its close-by then
                // reassing to new target inside the box
            //    setNode = parent.targetLink.to;
            //}
        } else {
            // set it as the target link, reset stuck as situation changed
            resetStuck();
            parent.targetLink = newLink;
            setNode = parent.targetLink.to;
            
            // get the direction range (theta only) to the node
            AngleArea a = new AngleArea();
            Vector2f pos = new Vector2f();
            pos.set(parent.position.x, parent.position.z);
            a.boxAngleFromPoint(pos, parent.targetLink.to.bounds);
            
            AngleArea a2 = new AngleArea();
            for(int nidx=parent.navRoute.routeStack.size-1; nidx>=0; nidx--) {
                NavCell nextNode = parent.navRoute.routeStack.get(nidx);
                a2.boxAngleFromPoint(pos, nextNode.bounds);
                // the common part of the angle range
                if(!a.shrink(a2)) {
                    // no common part, bail out
                    break;
                }
                // the nextNode can be reached with direct line movement
                // trough the previous node
                // the setNode is now the next node
                // TODO: consider also chosing a targe position that is inside the cone
                // TODO: also consider the turn angle between two links for the cost calculation
                // bots should prefer routes with less turns

                setNode = nextNode;
            }
        }

        if (setNode != null) {
            // set waypoint position
            setNode.bounds.fillRandomPointInside(parent.moveMarker.position);
            // TOOD: if next node is in same direction too, use that as target
            // do that until encountered a turn
            
            // extend target position by 20%
            parent.moveMarker.position.subtractLocal(parent.position).multLocal(1.2f).addLocal(parent.position);
            float dist = parent.position.distance2d( parent.moveMarker.position);
            // get height at position
            parent.moveMarker.position.y = setNode.getWalkableHeight(parent.moveMarker.position);
            //parent.currentLink = parent.currentNode.getLink(setNode);
            // set stuck times
            // set bot target position
            
            //stuckWaitTime = (long) (dist / 3 * 1000);
            stuckFailTime = (long) (dist * 5 * 1000);
            stuckTime = AppContext.gameMatch.timestamp;
            
            //parent.moveMarker.radius = FastMath.max(1f, setNode.bounds.extents.x/8f);
            //parent.moveMarker.setRadius((short) parent.moveMarker.radius);
            
            parent.moveMarker.setPosition(parent.moveMarker.position);
            // set waypoint destination for the bot
            parent.setMoveTarget(parent.moveMarker.SSN, 125);
        }
    }

    protected void checkStuck() {
        // check if we need to end the blind
        if (stuckTime == 0 || parent.shooting) {
            if(parent.shooting) {
                if(AppContext.gameMatch.timestamp - stuckTime / 1000 % 2 == 1) {
                    parent.setConfused();
                } else {
                    parent.setAggro();
                }
                
            }
            stuckTime = AppContext.gameMatch.timestamp;
            return;
        }
        if (AppContext.gameMatch.timestamp > stuckTime + stuckFailTime) {
            // failed, mark the link the bot was on as failed
            routeFailed();
        }
    }

    protected void routeFailed() {
        // penalise the node, and remove the link on which it failed
        if (parent.targetLink != null) {
            parent.targetLink.addStuckCount();
            log.log(Level.INFO, "Penalizing link between {0} to {1}", new Object[]{
                parent.targetLink.from.nodeid,
                parent.targetLink.to.nodeid
            });
        }
        if(parent.navRoute.start.bounds.intersects(parent.bounds)) {
            // failed route and did not move anywhere
            parent.stuckCount++;
            if(parent.stuckCount>5) {
                log.log(Level.INFO, "Bot {0} got stuck 5 times in a place, killing bot", parent.SSN);
                parent.setDead();
                parent.stuckCount=0;
            }
        } else {
            parent.stuckCount=0;
        }
        // clear out the route, and go into pause
        parent.navRoute = null;
        parent.botState = BotState.Respawn;
    }

    protected void resetStuck() {
        stuckTime = 0;
        //parent.setAggro();
    }
}
