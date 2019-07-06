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

import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.map.NavCell;
import jb2.map.NavRoute;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class RouteFindBehavior implements EntityEventHandler {

    protected static final Logger log = Logger.getLogger(RouteFindBehavior.class.getName());
    protected BotEntity parent;
    protected NavRoute secureRoute;
    protected NavRoute trialRoute;

    public RouteFindBehavior(BotEntity parent) {
        this.parent = parent;
    }

    @Override
    public void startEvent() {
        // then create a route
        createRoute();
    }

    @Override
    public void movedEvent() {

    }

    @Override
    public void teamChangedEvent() {

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

    }

    protected void createRoute() {
        parent.navRoute = null;
        parent.targetLink = null;
        parent.setStop();

        if (parent.currentNode == null) {
            log.log(Level.WARNING, "Bot {0} has no start node", parent.SSN);
            parent.botState = BotEntity.BotState.NoRouteStayPut;
            return;
        }

        // find a VIP node target
        // find route
        // follow route
        NavRoute newRoute = new NavRoute();
        // find the nearest node for start
        newRoute.start = parent.currentNode;
        newRoute.bot = parent;

        // choose random target
        FastList<VIPNodeEntity> targets = AppContext.gameMatch.teamTargets[parent.team - 1];
        if (targets.isEmpty()) {
            log.log(Level.WARNING, "Bot team {0} has no targets", parent.team);
            parent.botState = BotEntity.BotState.NoRouteStayPut;
            return;
        }
        int idx = FastMath.rand.nextInt(targets.size());
        newRoute.targetEntity = targets.get(idx);
        BoundingBox targetArea = new BoundingBox();
        targetArea.center.set(newRoute.targetEntity.position);
        targetArea.extents.set(10f, 10f, 10f);
        if (!targetArea.intersects(parent.bounds)) {
            // set 2d pathfinding
            newRoute.twoD = true;
        } else {
            log.log(Level.FINER, "Bot {0} already in target area {1}",
                    new Object[]{
                        parent.SSN,
                        newRoute.targetEntity.SSN}
            );
            newRoute.twoD = false;
        }

        FastList<NavCell> nodesInArea = AppContext.navGrid.getCells2d(targetArea, null);
        if (nodesInArea.isEmpty()) {
            log.log(Level.WARNING, "No nav nodes in LFP {0} {1}",
                    new Object[]{
                        newRoute.targetEntity.SSN,
                        newRoute.targetEntity.name
                    });
            newRoute.targetEntity = null;
            parent.botState = BotEntity.BotState.NoRouteStayPut;
            return;
        }

        NavCell nodeInAir = null;
        while (!nodesInArea.isEmpty()) {
            // pick a random node inside the area
            idx = FastMath.rand.nextInt(nodesInArea.size());
            NavCell targNode = nodesInArea.remove(idx);
            // until we find a node that is a bit further
            if (targNode.bounds.intersects(parent.bounds)) {
                continue;
            }
            nodeInAir = targNode;
            if (!targNode.isOnLandLevel()) {
                continue;
            }
            // set the destination to be the node
            newRoute.targetNode = targNode;
            break;
        }
        if (newRoute.targetNode == null) {
            // no node found on land, use an air node
            newRoute.targetNode = nodeInAir;
        }

        // no target node
        if (newRoute.targetNode == null) {
            parent.botState = BotEntity.BotState.NoRouteStayPut;
            return;
        }

        // submit both routes to be calculated
        trialRoute = newRoute;
        secureRoute = newRoute.makeCopyOfParameters();
        // on secure route only use already traversed nodes
        secureRoute.onlyTraversed = true;

        // initialise performance tracker with destination
        parent.startEvaluation(newRoute.targetNode.bounds.center);

        AppContext.routeFinder.findRoute(secureRoute);
        if (secureRoute.routeStack.size == 0) {
            AppContext.routeFinder.findRoute(trialRoute);
            if (trialRoute.routeStack.size > 0) {
                parent.navRoute = trialRoute;
            } else {
                // no route found
                log.log(Level.WARNING, "Failed finding route from node {0} to entity {1}",
                        new Object[]{
                            secureRoute.start.nodeid,
                            secureRoute.targetEntity.SSN
                        });
                parent.navRoute = null;
                // failed
                parent.botState = BotEntity.BotState.Roam;
                return;
            }
        } else {
            parent.navRoute = secureRoute;
        }
        // change state to follow route
        parent.botState = BotEntity.BotState.FollowRoute;
    }

}
