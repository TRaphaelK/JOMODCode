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
import jb2.map.NavGrid;
import jb2.map.NavLink;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.Context;
import jb2.util.JbException;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class EntityTracker implements EntityEventHandler {

    protected static final Logger log = Logger.getLogger(EntityTracker.class.getName());
    protected Entity parent;
    

    public EntityTracker(Entity parent) {
        this.parent = parent;
    }

    @Override
    public void startEvent() {
        parent.prevNode = null;
        parent.currentNode = null;
        trackPosition();
    }

    protected void trackPosition() {
        // if alive, not attached
        // TODO: allow tracking vehicles
        // ignore if falling or on ladder
        if (parent.dead ) {
            parent.prevNode = null;
            parent.currentNode = null;
            return;
        }

        if (parent.currentNode != null && !parent.currentNode.isLeaf()) {
            // clear current cell if it got promoted to node
            parent.currentNode = null;
        }

        // record height at bot position
        AppContext.heightMap.record(parent);

        // is there a previous node that is still near the entity
        // if so, exit
        if (parent.currentNode != null && parent.currentNode.bounds.contains(parent.position)) {
            parent.currentNode.recordHeight(parent);
            return;
        }

        // move current node to be the prev node
        parent.prevNode = parent.currentNode;
        parent.currentNode = null;

        NavGrid grid = AppContext.navGrid;
        Context ctx = LocalContext.getContext();

        // try find a new nav node near the player
        parent.currentNode = grid.getCell(parent.position);
        if (parent.currentNode == null) {
            // this could only happen if the player is outside map bounds
            // increase map size if needed
            grid.checkMapRanges(parent.position);
            parent.currentNode = grid.getCell(parent.position);
        }

        if (parent.currentNode == null) {
            // still no current node, error
            if(parent.isPlayer()) {
                log.log(Level.INFO, "Player {0} no current node, not tracking", parent.name);
                return;
            } else if(parent.isBot()) {
                log.log(Level.WARNING, "Bot {0} no current node, killing bot", parent.SSN);
                parent.dead = true;
                parent.setDead();
                return;
            }
        }

        log.log(Level.FINEST, "{0} in cell {1}",
                new Object[]{
                    parent.SSN,
                    parent.currentNode.nodeid
                });
        //log.info("Found node "+currentNode.nodeid);
        if (parent.prevNode != null) {
            if (parent.currentNode.nodeid == parent.prevNode.nodeid) {
                log.log(Level.SEVERE, "currentNode equals to prevNode");
                parent.prevNode = null;
                return;
            }

            // if cellis found, check if it has a valid connection from the
            // previous cell
            NavLink link = parent.prevNode.getLink(parent.currentNode);
            if (link == null) {
                // no link, forget the previous cell
                parent.prevNode = null;
            } else {
                // add traversal to the link
                link.addTrackedTraverseCount();
                if(parent instanceof BotEntity) {
                    NavLink targLink = ((BotEntity)parent).targetLink;
                    if(targLink == link) {
                        targLink.addBotTraverseCount();
                        //log.log(Level.FINER, "Link traversal from {0} to {1}", 
                        //        new Object[]{targLink.from.nodeid, targLink.to.nodeid});
                    }
                }
            }
        }
        if( parent.shouldbeTracked() ) {
            parent.currentNode.addTraversal(parent);
            parent.currentNode.recordHeight(parent);
        }
    }

    @Override
    public void movedEvent() {
        trackPosition();
    }

    @Override
    public void teamChangedEvent() {
        parent.prevNode = null;
        parent.currentNode = null;
    }

    @Override
    public void deadEvent() {
        // check if still on prevNode
        if (parent.currentNode != null) {
            if (parent.currentNode.bounds.intersects(parent.bounds)) {
                log.log(Level.INFO, "Recording {0} death influence", parent.SSN);

                // get all nodes in 10m radius
                BoundingBox bb = new BoundingBox();
                bb.center.set(parent.position);
                bb.extents.set(2f, 2f, 2f);
                AppContext.navGrid.forEachIntersectingLeaf(bb, true, leaf -> leaf.addDeath(parent.team));
            }
        }

        parent.prevNode = null;
        parent.currentNode = null;
    }

    @Override
    public void aliveEvent() {
        parent.prevNode = null;
        parent.currentNode = null;
        trackPosition();
    }

    @Override
    public void attachedEvent() {
        parent.prevNode = null;
        parent.currentNode = null;
    }

    @Override
    public void detachedEvent() {
        parent.prevNode = null;
        parent.currentNode = null;
        trackPosition();
    }

    @Override
    public void idleEvent() {

    }

}
