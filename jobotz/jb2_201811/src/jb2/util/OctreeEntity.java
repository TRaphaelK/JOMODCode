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
package jb2.util;

import java.util.logging.Logger;
import jb2.ent.Entity;
import jb2.math.BoundingBox;

/**
 *
 * @author vear
 */
public class OctreeEntity extends Octree {

    protected static final Logger log = Logger.getLogger(OctreeEntity.class.getName());
    
    public OctreeEntity() {
        super();
    }
    
    // get objects by only considering x and z coordinates
    public FastList<Entity> getEntities2d(BoundingBox bb, FastList<Entity> store) {
        return this.getIntersecting2d(bb, store);
    }
    
    public FastList<Entity> getEntities(BoundingBox bb, FastList<Entity> store) {
        return this.getIntersecting(bb, store);
    }
    
    public void remove(Entity ent) {
        if(ent.parentNode ==null)
            return;
        
        remove(ent.parentNode);
        
        ent.parentNode=null;
    }
    
    public void add(Entity ent) {
        Leaf leaf;
        if(ent.parentNode!=null) {
            leaf = ent.parentNode;
            // update leaf bounds
            leaf.bounds.set(ent.bounds);
            if(leaf.parentIndex>7) {
                // if the node still contains the entity, nothing to do
                if(leaf.parent.bounds.contains(ent.bounds))
                    return;
            } else {
                if(leaf.bounds.sameSide(leaf.parent.bounds.center)) {
                    // if on an octree child node, then the index should match too
                    int newIndex=this.getIndexOf(leaf.parent, leaf);
                    if(newIndex==leaf.parentIndex) {
                        // leaf stays where it is
                        return;
                    }                    
                }
            }
            // remove the leaf from its parent
            leaf.detachFromNode();
            leaf.parent = null;
            leaf.parentIndex=-1;
            
            // ensure octree dimensions can hold the leaf
            ensureOctreeSize(leaf);
            //(re) insert leaf
            insertLeaf(root, leaf);
        } else {
            // create new leaf
            leaf = add(ent, ent.bounds);
            ent.parentNode = leaf;
        }
    }
}
