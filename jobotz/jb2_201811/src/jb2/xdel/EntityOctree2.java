/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Arrays;
import jb2.ent.Entity;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.math.Vector3f;
import jb2.util.Context;
import jb2.util.FastList;
import jb2.util.IntList;
import jb2.util.JbException;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class EntityOctree2 {
    /*
    protected static final int defaultNodeCount=2048;
    protected int lastNode=0;
    // center x,y,z of node 0,1,2
    // extent x,y,z of node 3,4,5
    protected float[] nodeBounds=new float[defaultNodeCount*6];
    // child nodes by x-+,y-+,z-+
    protected int[] childNodes=new int[defaultNodeCount*8];
    
    protected int lastLeaf=0;
    // parent and index of this leaf (parent*8 + index)
    protected int posParent[]=new int[defaultNodeCount];
//    protected float[] leafCoord=new float[defaultNodeCount*3];
    protected Entity[] objects = new Entity[defaultNodeCount];
    
    protected float xmin, xmax, ymin, ymax, zmin, zmax;
        
    public EntityOctree2(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.zmin = zmin;
        this.zmax = zmax;
        clear();
    }

    public void clear() {
        lastNode=0;
        // clear out the arrays
        Arrays.fill(nodeBounds, 0);
        Arrays.fill(childNodes, 0);
        lastLeaf=0;
        Arrays.fill(posParent, 0);
        Arrays.fill(objects, null);
        //Arrays.fill(leafCoord, 0);

        // create a root node
        lastNode++;
        int nodepos = lastNode*6;
        nodeBounds[nodepos] = (xmin+xmax)/2f;
        nodeBounds[nodepos+1] = (ymin+ymax)/2f;
        nodeBounds[nodepos+2] = (zmin+zmax)/2f;

        nodeBounds[nodepos+3] = FastMath.abs(xmax-xmin)/2f;
        nodeBounds[nodepos+4] = FastMath.abs(ymax-ymin)/2f;
        nodeBounds[nodepos+5] = FastMath.abs(zmax-zmin)/2f;
    }
    
    public void add(Entity ent) {
        
        if(ent.position.x<xmin || ent.position.x>xmax 
                || ent.position.y<ymin || ent.position.y>ymax 
                || ent.position.z<zmin || ent.position.z>zmax) {
            throw new JbException("Item out of octree range");
        }

        int leaf;
        // repurpose the key 
        if(ent.parentNodeId!=0) {
            leaf = ent.parentNodeId;
            // remove the leaf from its parent
            childNodes[posParent[leaf]] = 0;
            posParent[leaf] = 0;
        } else {
            // allocate a new leaf
            lastLeaf++;
            leaf=lastLeaf;
            ensureLeafCapacity();
            objects[leaf]=ent;
            //int leafpos = leaf*3;
            //leafCoord[leafpos] = ent.position.x;
            //leafCoord[leafpos+1] = ent.position.y;
            //leafCoord[leafpos+2] = ent.position.z;
            ent.parentNodeId=leaf;
        }
        // insert leaf to root
        insertLeaf(1, leaf);
    }
    
    protected void insertLeaf(int node, int leaf) {
        
        Entity ent=objects[leaf];
        //int leafpos = leaf*3;
        //float x = leafCoord[leafpos];
        //float y = leafCoord[leafpos+1];
        //float z = leafCoord[leafpos+2];
        float x = ent.position.x;
        float y = ent.position.y;
        float z = ent.position.z;

        while(true) {
            int nodepos = node*6;
            // determine which section it belongs to
            float nodex = nodeBounds[nodepos];
            float nodey = nodeBounds[nodepos+1];
            float nodez = nodeBounds[nodepos+2];

            // determine the index
            int index = 0;
            if(x>nodex) {
                index += 4;
            }
            if(y>nodey) {
                index += 2;
            }
            if(z>nodez) {
                index += 1;
            }
            int childPos = node*8+index;
            int childNode = childNodes[childPos];
            if(childNode!= 0) {
                // check if child is leaf
                if(childNode<0) {
                    // create a new node and link the leaf to the child node
                    int newNode = createNode(node, index);
                    // insert the displaced leaf into new node
                    insertLeaf(newNode, -childNode);

                    childNode=newNode;
                }
                // try to insert the leaf into the child node
                node = childNode;
                continue;
            }
            // there is no node yet, insert the leaf as child node
            // with negative sign, so that we can distinguish
            childNodes[childPos] = -leaf;
            posParent[leaf]=childPos;
            break;
        }
    }
    
    public void remove(Entity ent) {
        int leaf = ent.parentNodeId;
        if(leaf!=0) {
            // deactivate the leaf
            childNodes[posParent[leaf]] = 0;
            if(leaf == lastLeaf) {
                // clear out the leaf value too
                posParent[leaf]=0;
                objects[leaf]=null;
            } else {
                // copy leaf info from last to leaf
                posParent[leaf] = posParent[lastLeaf];
                objects[leaf] = objects[lastLeaf];
                // shift the last element back by copying the last element
                childNodes[posParent[leaf]] = -leaf;
                
                //int leafpos = leaf*3;
                //int lastleafpos = lastLeaf*3;
                //leafCoord[leafpos] = leafCoord[lastleafpos];
                //leafCoord[leafpos+1] = leafCoord[lastleafpos+1];
                //leafCoord[leafpos+2] = leafCoord[lastleafpos+2];
                
            }
            // reclaim the space                
            lastLeaf--;
        }
    }

    protected void ensureNodeCapacity() {
        while(lastNode*6 >= nodeBounds.length) {
            // need to extend the arrays
            nodeBounds = Arrays.copyOf(nodeBounds, nodeBounds.length*2);
            childNodes = Arrays.copyOf(childNodes, childNodes.length*2);
        }        
    }
    
    protected void ensureLeafCapacity() {
        while(lastLeaf >= posParent.length) {
            // need to extend the arrays
            posParent = Arrays.copyOf(posParent, posParent.length*2);
            objects = Arrays.copyOf(objects, objects.length*2);
            //leafCoord=Arrays.copyOf(leafCoord, leafCoord.length*2);
        }        
    }
    
    protected int createNode(int parent, int index) {
                
        // increase the node size
        lastNode++;
        ensureNodeCapacity();
        
        // put the new node to the end
        int node = lastNode;
        
        int parentPos = parent*6;
        
        // set ref from parent to this node
        
        float centerx = nodeBounds[parentPos];
        float centery = nodeBounds[parentPos+1];
        float centerz = nodeBounds[parentPos+2];
        
        float halfx = nodeBounds[parentPos+3] / 2f;
        float halfy = nodeBounds[parentPos+4] / 2f;
        float halfz = nodeBounds[parentPos+5] / 2f;
        
        int whichhalf = index;
        if(whichhalf>=4) {
            centerx += halfx;
            whichhalf-=4;
        } else {
            centerx -= halfx;
        }
        if(whichhalf>=2) {
            centery += halfy;
            whichhalf-=2;
        } else {
            centery -= halfy;
        }
        if(whichhalf>=1) {
            centerz += halfz;
        } else {
            centerz -= halfz;
        }
        
        int nodePos = node*6;
        
        nodeBounds[nodePos] = centerx;
        nodeBounds[nodePos+1] = centery;
        nodeBounds[nodePos+2] = centerz;

        // store the halved extents
        nodeBounds[nodePos+3] = halfx;
        nodeBounds[nodePos+4] = halfy;
        nodeBounds[nodePos+5] = halfz;
        
        // store ref to child in parent
        childNodes[parent*8+index] = node;
        
        return node;
    }
    
    public FastList<Entity> getContained(BoundingBox bb, FastList<Entity> store) {
        if(store==null) {
            store = new FastList<>();
        } else {
            store.size=0;
        }
        
        getIntersecting(1, bb, store);
       
        return store;
    }
    
    protected void getIntersecting(int node, BoundingBox bb, FastList<Entity> store) {
        
        IntList checknodes;
        IntList addnodes;
        if(LocalContext.isUseThreadLocals()) {
            Context ctx = LocalContext.getContext();
            checknodes = ctx.Octree_getIntersecting_checknodes;
            addnodes = ctx.Octree_getIntersecting_addnodes;
            
            checknodes.size=0;
            addnodes.size=0;
        } else {
            checknodes = new IntList(64);
            addnodes = new IntList(64);
        }
        
        checknodes.add(node);
        while(checknodes.size>0) {
            node = checknodes.get(checknodes.size-1);
            // pop the last element from the stack
            checknodes.removeElementAt(checknodes.size-1);
        
            if(node<0) {
                // leaf
                int leaf = -node;
                Entity ent = objects[leaf];
                if(bb.contains(ent.position)) {
                    store.add(objects[leaf]);
                }
                continue;
            }

            int nodepos = node*6;
            
            int collision = bb.collide(nodeBounds, nodepos);
            
            // construct bb for node
            if(collision==0) {
                // no intersection
                continue;
            }

            if(collision==1) {
                // bb contains everything in this node and its children
                // add everything without checking
                addnodes.add(node);
                while(addnodes.size>0) {
                    node = addnodes.get(addnodes.size-1);
                    // pop the last element from the stack
                    addnodes.removeElementAt(addnodes.size-1);
                    if(node<0) {
                        // leaf
                        store.add(objects[-node]);
                        continue;
                    }
                    // call it on child nodes
                    for(int index=0; index<8; index++) {
                        int childNode = childNodes[node*8+index];
                        if(childNode!= 0) {
                            // recurse into that child too
                            addnodes.add(childNode);
                        }
                    }
                }
                continue;
            }

            // check child nodes
            for(int index=0; index<8; index++) {
                int childNode = childNodes[node*8+index];
                if(childNode!= 0) {
                    // recurse into that child too
                    checknodes.add(childNode);
                }
            }
        }
    }
*/
}
