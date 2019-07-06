/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Arrays;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.IntIntMap;
import jb2.util.IntList;
import jb2.util.JbException;

/**
 *
 * @author vear
 */
public class OctreeInt {
    
    protected static final int defaultNodeCount=2048;
    protected int nodeSize=0;
    // center x,y,z of node
    protected float[] nodeCenter=new float[defaultNodeCount*3];
    // extent x,y,z of node
    protected float[] nodeExtents=new float[defaultNodeCount*3];
    // if leaf, then its an actual oject, not a node
    protected boolean [] isleaf = new boolean[defaultNodeCount];
    // child nodes by x-+,y-+,z-+
    protected int[] childNodes=new int[defaultNodeCount*8];
    // the map of which leaf each key belongs
    protected IntIntMap keyLeafs = new IntIntMap(defaultNodeCount);
    
    protected float xmin, xmax, ymin, ymax, zmin, zmax;
        
    public OctreeInt(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.zmin = zmin;
        this.zmax = zmax;
        clear();
    }

    public void clear() {
        nodeSize=0;
        // clear out the arrays
        Arrays.fill(nodeCenter, 0);
        Arrays.fill(nodeExtents, 0);
        Arrays.fill(childNodes, 0);
        Arrays.fill(isleaf, false);

        // create a root node
        nodeCenter[nodeSize*3] = (xmin+xmax)/2f;
        nodeCenter[nodeSize*3+1] = (ymin+ymax)/2f;
        nodeCenter[nodeSize*3+2] = (zmin+zmax)/2f;

        nodeExtents[nodeSize*3] = (xmax-xmin)/2f;
        nodeExtents[nodeSize*3+1] = (ymax-ymin)/2f;
        nodeExtents[nodeSize*3+2] = (zmax-zmin)/2f;
        isleaf[nodeSize] = false;
        // the rest is empty
        nodeSize++;
    }
    
    public void add(float x, float y, float z, int key) {
        
        if(key==0)
            throw new JbException("Null key not allowed for octree items");
        if(x<xmin || x>xmax || y<ymin || y>ymax || z<zmin || z>zmax) {
            throw new JbException("Item out of octree range");
        }

        // we need to remove the key from the old leaf
        // if it was atached
        remove(key);
        
        // add to root
        // create a new leaf node
        int leaf = createLeaf(x, y, z, key);
        
        insertLeaf(0, leaf);
    }
    
    public void add(Vector3f vec, int key) {
        add(vec.x, vec.y, vec.z, key);
    }

    protected int createLeaf(float x, float y, float z, int key) {
        
        ensureCapacity(nodeSize);
        
        // put the new node to the end
        int node = nodeSize;
        
        // set ref from parent to this node
        nodeCenter[node*3] = x;
        nodeCenter[node*3+1] = y;
        nodeCenter[node*3+2] = z;

        // store key
        childNodes[node*8] = key;
        
        // mark as leaf
        isleaf[node] = true;
        
        // node added
        nodeSize++;
        
        return node;
    }
    
    protected void copyLeaf(int leaf, int target) {
        // set ref from parent to this node
        nodeCenter[target*3] =  nodeCenter[leaf*3];
        nodeCenter[target*3+1] =  nodeCenter[leaf*3+1];
        nodeCenter[target*3+2] =  nodeCenter[leaf*3]+2;

        // store key
        childNodes[target*8] = childNodes[leaf*8];
        
        // mark as leaf
        isleaf[target] = true;
    }
    
    protected void insertLeaf(int node, int leaf) {
        
        while(true) {
            // determine which section it belongs to
            float nodex = nodeCenter[node*3];
            float nodey = nodeCenter[node*3+1];
            float nodez = nodeCenter[node*3+2];

            float x = nodeCenter[leaf*3];
            float y = nodeCenter[leaf*3+1];
            float z = nodeCenter[leaf*3+2];

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
            int childNode = childNodes[node*8+index];
            if(childNode!= 0) {
                // check if child is leaf
                if(isleaf[childNode]) {
                    // check if its an empty leaf
                    if(childNodes[childNode*8]==0) {
                        // its an empty node, copy over the new leaf to this one,
                        // and free up if its the last one
                        copyLeaf(leaf, childNode);
                        if(leaf==nodeSize-1) {
                            // deallocate the newly created leaf
                            nodeSize--;
                        }
                        leaf=childNode;
                        keyLeafs.put(childNodes[leaf*8], leaf);
                        return;
                    }
                    // create a new node and link the leaf to the child node
                    int newNode = createNode(node, index);
                    // replace leaf with node
                    //childNodes[index] = newNode;
                    // insert leaf into new node
                    insertLeaf(newNode, childNode);

                    childNode=newNode;
                }
                // insert the leaf into child node
                // do this as continuation
                //insertLeaf(childNode, leaf);
                node = childNode;
                continue;
            }
            // there is no node yet, insert the leaf as child node
            childNodes[node*8+index] = leaf;
            keyLeafs.put(childNodes[leaf*8], leaf);
            break;
        }
    }
    
    public void remove(int key) {
        int leaf = keyLeafs.remove(key);
        if(leaf!=0) {
            // deactivate the leaf
            childNodes[leaf*8] = 0;
        }
    }
    
    protected void ensureCapacity(int cap) {
        while(cap >= isleaf.length) {
            // need to extend the arrays
            nodeCenter = Arrays.copyOf(nodeCenter, cap*2*3);
            nodeExtents = Arrays.copyOf(nodeExtents, cap*2*3);
            childNodes = Arrays.copyOf(childNodes, cap*2*8);
            isleaf = Arrays.copyOf(isleaf, isleaf.length*2);
        }        
    }
    
    protected int createNode(int parent, int index) {
        
        ensureCapacity(nodeSize);
        
        // put the new node to the end
        int node = nodeSize;
        
        // set ref from parent to this node
        float halfx = nodeExtents[parent*3] / 2f;
        float halfy = nodeExtents[parent*3+1] / 2f;
        float halfz = nodeExtents[parent*3+2] / 2f;

        float centerx = nodeCenter[parent*3];
        float centery = nodeCenter[parent*3+1];
        float centerz = nodeCenter[parent*3+2];
        
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
        nodeCenter[node*3] = centerx;
        nodeCenter[node*3+1] = centery;
        nodeCenter[node*3+2] = centerz;

        // store the halved extents
        nodeExtents[node*3] = halfx;
        nodeExtents[node*3+1] = halfy;
        nodeExtents[node*3+2] = halfz;
        
        isleaf[node] = false;
        
        // store ref to child in parent
        childNodes[parent*8+index] = node;
        // increase the node size
        nodeSize++;
        
        return node;
    }
    
    public IntList getContained(BoundingBox bb, IntList store) {
        if(store==null) {
            store = new IntList();
        } else {
            store.clear();
        }
        
        getIntersecting(0, bb, store);
       
        return store;
    }
    
    protected void getIntersecting(int node, BoundingBox bb, IntList store) {
        
        IntList checknodes = new IntList(64);
        IntList addnodes = new IntList(64);
        
        checknodes.add(node);
        while(checknodes.size()>0) {
            node = checknodes.get(checknodes.size()-1);
            // pop the last element from the stack
            checknodes.removeElementAt(checknodes.size()-1);
        
            if(isleaf[node]) {
                if(bb.contains(nodeCenter[node*3], nodeCenter[node*3+1], nodeCenter[node*3+2])) {
                    store.add(childNodes[node*8]);
                }
                continue;
            }

            // construct bb for node
            if(!bb.intersects(nodeCenter, nodeExtents, node*3)) {
                // no intersection
                continue;
            }
            if(bb.contains(nodeCenter, nodeExtents, node*3)) {
                // bb contains everything in this node and its children
                addnodes.add(node);
                while(addnodes.size()>0) {
                    node = addnodes.get(addnodes.size()-1);
                    // pop the last element from the stack
                    addnodes.removeElementAt(addnodes.size()-1);
                    if(isleaf[node]) {
                        // add the leaf key
                        store.add(childNodes[node*8]);
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

            // call it on child nodes
            for(int index=0; index<8; index++) {
                int childNode = childNodes[node*8+index];
                if(childNode!= 0) {
                    // recurse into that child too
                    checknodes.add(childNode);
                }
            }
        }
    }
}
