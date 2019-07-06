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

import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.ent.BotEntity.BotState;
import jb2.gai.EntParameters;
import jb2.map.NavCell;
import jb2.map.NavLink;
import jb2.math.FastMath;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class BotRoamBehavior implements EntityEventHandler {
    protected static final Logger log = Logger.getLogger(BotRoamBehavior.class.getName());
    protected BotEntity parent;
    protected FastList<NavLink> possibleLinks = new FastList<>();
    //protected long waitTime;
    protected long stayPutEndTime=0;
    protected FastList<NavCell> visited;
    
    public BotRoamBehavior(BotEntity parent) {
        this.parent = parent;
    }
    
    @Override
    public void startEvent() {
        stayPutEndTime=AppContext.gameMatch.timestamp+FastMath.rand.nextInt(20*1000);
        parent.targetLink = null;
        findWorkToDo();
    }

    @Override
    public void movedEvent() {
        findWorkToDo();
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
        findWorkToDo();
    }

    protected void findWorkToDo() {
        //if(AppContext.gameMatch.timestamp<waitTime) {
        //    return;
        //}

        // each iteration change bot vision
        parent.setApproach();

        if(AppContext.gameMatch.timestamp>stayPutEndTime) {
            parent.setStop();
            parent.botState = BotState.FindTarget;
            return;
        }
                    
        if(parent.currentNode == null || !parent.currentNode.isLeaf()) {
            // clear current cell if it got promoted to node
            parent.reevaluateCurrentNavCell();
        }
        NavCell current = parent.currentNode;
        if(current==null) {
            // no current, just wait
            log.log(Level.INFO, "Bot {0} no current node, waiting", parent.SSN);
            return; 
        }
        if(visited==null) {
             visited = new FastList<>();
             visited.add(current);
        }
        if(parent.targetLink!=null) {
            if(current==parent.targetLink.from) {
              // has not yet left the from node
              return;
            } else if(current==parent.targetLink.to) {
                // we reached the target adjacent cell, mark it as success
                
                visited.add(current);
            } else {
                // we reached somewhere else, mark as failure
                parent.targetLink.addStuckCount();
                /*
                if(current==targetLink.from && possibleLinks.size==0) {
                    // nothing more to select, go to waiting
                    parent.botState = BotState.FindTarget;
                    return; 
                }*/
                if(!visited.contains(current))
                    visited.add(current);
            }
            parent.targetLink=null;
        }
        findTarget();
    }

    protected void findTarget() {
        // get the current node of the bot
        NavCell current = parent.currentNode;
        /*
        if(targetLink!=null && current!=targetLink.from) {
            targetLink = null;
        }
        if(targetLink==null) {
        */
            // we are not starting as from the previous
            possibleLinks.clear();
            if(current.links==null) {
                parent.botState = BotState.FindTarget;
                return;
            }
            for(int i=0; i<current.links.size; i++) {
                NavLink link = current.links.get(i);
                if(visited.contains(link.to))
                    continue;
                if(link.isLinkStuck(parent.navParams))
                    continue;
                //if(link.isVertical())
                //    continue;
                possibleLinks.add(link);
            }
            if(possibleLinks.isEmpty()) {
                parent.botState = BotState.FindTarget;
                return;
            }
        //}
        // if a linked node's height is not yet determined, then send the bot there
        for(int i=0; i<possibleLinks.size; i++) {
            NavLink link = possibleLinks.get(i);
            // check if node height is unknown
            if(link.to.isTerrainLevelKnown())
                continue;
            // add this as a target
            parent.targetLink=link;
            break;
        }
        
        if(parent.targetLink==null) {
            // remove those nodes that are not on terrain level
            for(int i=0; i<possibleLinks.size; i++) {
                // check if node height is unknown
                while(i < possibleLinks.size &&
                        !possibleLinks.get(i).to.isOnLandLevel()) {
                    // remove all nodes not on land level
                    possibleLinks.remove(i);
                }
            }
            // sort it according to least traversed
            possibleLinks.sort((Comparator<NavLink>) (NavLink o1, NavLink o2) -> {
                    float c1 = o1.linkcost[1]+o1.linkcost[2];
                    float c2 = o2.linkcost[1]+o2.linkcost[2];
                    return Float.compare(c1, c2);
                });
            
            parent.targetLink = possibleLinks.get(0);
        }
                
        if(parent.targetLink==null) {
            // nothing more to select, go to waiting
            parent.botState = BotState.FindTarget;
            return; 
        }
        
        // random position inside the cell
        //parent.moveMarker.position.set(targetLink.to.bounds.center);
        parent.targetLink.to.fillRandomPointOnLandLevel(parent.moveMarker.position);
        parent.moveMarker.setPosition(parent.moveMarker.position);
        // set waypoint destination for the bot
        parent.setMoveTarget(parent.moveMarker.SSN, 125);
        parent.idleTime = (long) (parent.position.distance(parent.moveMarker.position) * 5000);
        //waitTime = (long) (AppContext.gameMatch.timestamp + parent.idleTime );
    }
    
    /*
    protected void findTarget() {
        //fill in the target
        // find a nearby random generated node and go towards it
        BoundingBox bb = new BoundingBox();
        bb.center.set(parent.position);
        bb.extents.set(5,2,5);
        FastList<NavCell> targets = AppContext.navGrid.getCells2d(bb, null);
        if(target!=null) {
            targets.remove(target);
        }
        
        // TODO: sort the nodes, chose the least visited node
        // also considering nodes closer to the target
        
        // filter out the generated node
        //keepOnlyGeneratedNodes(targets);
        
        if(targets.size == 0) {
            // try with bigger radius
            bb.extents.set(50,2,50);
            targets = AppContext.navGrid.getCells2d(bb, null);
            if(target!=null) {
                targets.remove(target);
            }
            //keepOnlyGeneratedNodes(targets);
            if(targets.size == 0) {
                parent.botState = BotState.FindTarget;
                return;
            }
        }
        target=targets.get(FastMath.rand.nextInt(targets.size));
        // random position inside the cell
        target.bounds.fillRandomPointInside(parent.moveMarker.position);
        parent.moveMarker.setPosition(parent.moveMarker.position);
        // set waypoint destination for the bot
        parent.setMoveTarget(parent.moveMarker.SSN, 125);
        parent.idleTime = (long) (parent.position.distance(target.bounds.center) * 2000);
        waitTime = (long) (AppContext.gameMatch.timestamp + parent.idleTime );
    }
    */
    /*
    protected void keepOnlyGeneratedNodes(FastList<NavCell> targets) {
        // only keep generated nodes for roaming
        int i= 0;
        while(i<targets.size) {
            NavCell nn = targets.get(i);
            if(nn.source!=2) {
                targets.remove(i);
                continue;
            }
            i++;
        }
    }
*/
}
