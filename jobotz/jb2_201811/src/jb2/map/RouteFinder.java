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
package jb2.map;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import jb2.AppContext;
import jb2.math.Vector3f;
import jb2.util.IntMap;
import jb2.util.ThreadTask;

/**
 *
 * @author vear
 */
public class RouteFinder extends ThreadTask {
    
    // queue of route finding jobs
    protected ConcurrentLinkedQueue<NavRoute> waiting = new ConcurrentLinkedQueue<>();
        
    // the maintainer of node costs
    //protected NavMapCalculator calc = new NavMapCalculator();
    protected long nextWeightReset = 0;
    protected long nextWeightDecay = 0;

    
    
    public RouteFinder() {
        super("RouteFinder");
    }

    public void findRoute(NavRoute route) {
        boolean doDirectFind = true; //!LocalContext.isUseMultithreading();
        if(doDirectFind) {
            // do the route determiantion here
            findRouteDirect(route);
            return;
        }
        
        waiting.add(route);

        // notify worker thread that there is work to do
        this.notifyWork();
    }
    
    public void findRouteDirect(NavRoute route) {
        // check if node decay is needed
        //calc.doWork();
        
        if(AppContext.gameMatch.timestamp>nextWeightDecay) {
            decayMapWeights(false);
        }
                
        long runtime = System.currentTimeMillis();
            
            
        PriorityQueue<RouteNode> openQueue = new PriorityQueue<>();
        IntMap<RouteNode> open = new IntMap<>();
        IntMap<RouteNode> closed = new IntMap<>();
        
        //route.targetPos = new Vector3f();
        //if(route.targetEntity!=null) {
        //    route.targetPos.set(route.targetEntity.position);
        //} else if(route.targetArea != null) {
        //    route.targetPos.set(route.targetArea.center);
        //} else if(route.targetNode != null) {
            //route.targetPos.set(route.targetNode.bounds.center);
        //}
        
        RouteNode current = new RouteNode(route);

        route.nearestNode = current;
        route.nearDistance = current.estimate;
        
        int numSteps = 0;
        // add the start node to open queue
        openQueue.add(current);
        // and open set
        open.put(current.node.nodeid, current);
        
        while(!openQueue.isEmpty()) {
            // get the top on the queue
            current = openQueue.poll();
            if(closed.get(current.node.nodeid)!=null) {
                // this node was alread processed, ignore
                continue;
            }
            if(current.estimate<route.nearDistance) {
                route.nearDistance = current.estimate;
                route.nearestNode = current;
            }
            if(current.node==route.targetNode) {
                // landed on destination, build up the route
                // limit route length to 200 items so that cyclic referenced don'T lead
                // to crash
                while(current!=null && current.linkTo!=null) { // && route.routeStack.size<200
                    // place the node into the route stack
                    if(route.routeStack.contains(current.node)) {
                        // cyclic route
                        route.routeStack.clear();
                        route.unreachable = true;
                        log.log(Level.WARNING, "Cyclic route ignored");
                        return;
                    }
                    route.routeStack.add(current.node);
                    current = current.parent;
                }
                // set route to ready
                route.ready = true;
                runtime = System.currentTimeMillis() - runtime;
                //log.log(Level.FINER, "Route found in {0} ms", runtime);
                return;
            }
            // process all the links of the node
            for(int i=0; i<current.node.links.size(); i++) {
                NavLink link = current.node.links.get(i);
                
                if(link.isLinkStuck(route.bot.navParams) 
                        //|| (route.twoD && link.isVertical())
                        || (route.onlyTraversed && !link.isTraversed())) {
                    continue;
                }
                // check if node not on land level
                if(route.twoD && route.onlyTraversed) {
                    if(!link.to.isOnLandLevel()) {
                        // two-d route search, and not on terrain level
                        continue;
                    }
                }

                RouteNode child;
                
                child = closed.get(link.to.nodeid);
                if(child!=null) {
                    // already processed node, ignore
                    continue;
                    //log.log(Level.FINEST, "Found closed");
                }
                
                child = open.get(link.to.nodeid);
                if(child!= null) {
                    // replace parent on child
                    if(!child.checkAndSwitch(current, link)) {
                        // this solution is worse, skip
                        continue;
                    }
                    openQueue.remove(child);
                    // re/add children as their children can be improved too
                    openQueue.add(child);

                } else {
                    if(current.depth<200) {
                        child=new RouteNode(route, current, link);
                        open.put(link.to.nodeid, child);
                        openQueue.add(child);
                    }
                }
            }
            // add processed to closed
            closed.put(current.node.nodeid, current);
            numSteps++;
            if(numSteps>500) {
                log.log(Level.WARNING, "RouteFinder can''t find destination after {0} iterations", numSteps);
                // reset weigths if 5 minutes past last reset
                // reset weight stats, hopefully that helps
                if(AppContext.gameMatch.timestamp>nextWeightDecay) {
                    decayMapWeights(false);
                }
                break;
            }
        }
        // mark the route as incomplete
        route.unreachable = true;
        // unwind from the nearest found node
        // landed on destination, build up the route
        current = route.nearestNode;
        while(current!=null && current.linkTo!=null) { // && route.routeStack.size<200
            if(route.routeStack.contains(current.node)) {
                // cyclic route
                route.routeStack.clear();
                route.unreachable = true;
                log.log(Level.WARNING, "Cyclic route ignored");
                return;
            }
            //current.linkTo.to.addRouteInfluence(route.botTeam);
            // place the node into the route stack
            route.routeStack.add(current.node);
            current = current.parent;
        }
    }

    public void decayMapWeights(boolean reset) {
        if(AppContext.gameMatch.timestamp > nextWeightReset || reset) {
            AppContext.navGrid.forEachLeaf(leaf->leaf.resetWeights());
            log.log(Level.INFO, "Node weigths reset");
            nextWeightReset=AppContext.gameMatch.timestamp+5*60*1000;            
        } else {
            AppContext.navGrid.forEachLeaf(leaf->leaf.decayWeights());
            log.log(Level.INFO, "Node weigths decay");            
        }
        nextWeightDecay=AppContext.gameMatch.timestamp+10*1000;
    }

    @Override
    protected void doWork() {
        // process the queue for work
        while(!waiting.isEmpty()) {
            if(AppContext.gameMatch.exit)
                break;
            NavRoute route = waiting.poll();
            
            findRouteDirect(route);
            
        }
    }

    @Override
    protected boolean shouldWakeUp() {
        return !waiting.isEmpty();
    }
}
