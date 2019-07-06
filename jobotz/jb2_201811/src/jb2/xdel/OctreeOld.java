/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.map.NavCell;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.Context;
import jb2.util.FastList;
import jb2.util.LocalContext;

/**
 * Octree. Have compared 3 versions of octree: morton tree, octree with packed arrays 
 * and tree with object references. Can't fool the JVM, the object version is as fast as the packed 
 * array version. Perhaps with bigger data volumes there would be a difference, but with the amount
 * of game objects and nav nodes we are working, the object based is as fast.
 * @author vear
 */
public class OctreeOld {
    
    protected static final Logger log = Logger.getLogger(OctreeOld.class.getName());
    
    protected float xmin, xmax, ymin, ymax, zmin, zmax;
    protected OctreeNode root;
    
    public enum OctreeElementType {
        Node,
        List,
        Leaf
    }
    
    protected class OctreeElement {
        OctreeElementType type;
        BoundingBox bounds = new BoundingBox();
        OctreeElement parent;
        int parentIndex;
    }
    
    public class OctreeNode extends OctreeElement {
        OctreeElement[] childNodes;
        OctreeNode() {
            type=OctreeElementType.Node;
            childNodes=new OctreeElement[8];
        }
    }
    
    public class OctreeList extends OctreeElement {
        FastList<OctreeLeaf> children=new FastList<>();
        OctreeList() {
            type=OctreeElementType.List;
        }
    }
    
    public class OctreeLeaf extends OctreeElement {
        Object object;
        // pointer to current/changing posiotion
        public Vector3f position;
        // pointer to position given at time of insertion into the tree
        // used for detecting changes
        public Vector3f origPosition = new Vector3f();
    }
    
    public class OctreeItearator implements Iterator<Object> {

        protected OctreeElement currentNode;
        protected int currentIndex;
        FastList<OctreeElement> checknodes;
        protected Object next;
        
        OctreeItearator() {
            // add the root of the octree as start
            checknodes.add(root);
            // determine next
            determineNext();
        }
        
        @Override
        public boolean hasNext() {
            return (next!=null);
        }

        public void determineNext() {
            OctreeNode node;
            OctreeList list;
            OctreeLeaf leaf;
            
            next = null;
            while(currentNode!=null || !checknodes.isEmpty() ) {
                
                 if(currentNode==null) {
                    // pop a new node from stack
                     currentNode = checknodes.remove();
                     currentIndex = 0;
                 }
                 
                 OctreeElement element=null;
                 if(currentNode!=null) {
                    // first determine the element
                    // and if the current node is still valid
                    if(currentNode.type == OctreeElementType.Node) {
                        node = (OctreeNode) currentNode;
                        element = node.childNodes[currentIndex];
                        currentIndex++;
                        if(currentIndex>node.childNodes.length)
                            currentNode=null;
                    } else if(currentNode.type == OctreeElementType.List) {
                        list = (OctreeList) currentNode;
                        element = list.children.get(currentIndex);
                        currentIndex++;
                        if(currentIndex>list.children.size())
                            currentNode=null;
                    }
                    
                    // check if next is a leaf or node
                    if(element!=null) {
                        if(element.type == OctreeElementType.Node
                                || element.type == OctreeElementType.List) {
                            // new level node
                            checknodes.add(element);
                        } else if(element.type == OctreeElementType.Leaf ){
                            // extract the next
                            leaf = (OctreeLeaf) element;
                            next = leaf.object;
                            // we have a next object
                            if(next!=null)
                                return;
                        }
                    }
                 }                 
            }
        }

        @Override
        public Object next() {
            Object oldNext = next;
            determineNext();
            return oldNext;
        }
    }
    
    public OctreeOld(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        clear(xmin, xmax, ymin, ymax, zmin, zmax);
    }
    
    public OctreeItearator iterator() {
        return new OctreeItearator();
    }
    
    public void clear(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.zmin = zmin;
        this.zmax = zmax;
        clear();
    }
    
    public boolean checksize(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        if(xmin<this.xmin)
            return true;
        if(xmax>this.xmax)
            return true;
        if(ymin<this.ymin)
            return true;
        if(ymax>this.ymax)
            return true;
        if(zmin<this.zmin)
            return true;
        if(zmax>this.zmax)
            return true;
        return false;
    }
    
    public void clear() {
        root=new OctreeNode();
        root.bounds.center.x = (xmin+xmax)/2f;
        root.bounds.center.y = (ymin+ymax)/2f;
        root.bounds.center.z = (zmin+zmax)/2f;

        root.bounds.extents.x = (xmax-xmin)/2f;
        root.bounds.extents.y = (ymax-ymin)/2f;
        root.bounds.extents.z = (zmax-zmin)/2f;
    }
    
    protected void insertLeaf(OctreeElement element, OctreeLeaf leaf) {
        
        float x = leaf.origPosition.x;
        float y = leaf.origPosition.y;
        float z = leaf.origPosition.z;
        
        while (true) {
            
            if(element.type == OctreeElementType.List) {
                OctreeList list = (OctreeList) element;
                list.children.add(leaf);
                leaf.parent = element;
                leaf.parentIndex = -1;
                return;
            }

            OctreeNode node = (OctreeNode) element;
            
            // determine which section it belongs to
            float nodex = node.bounds.center.x;
            float nodey = node.bounds.center.y;
            float nodez = node.bounds.center.z;

            // determine the index
            int index = 0;
            if (x >= nodex) {
                index += 4;
            }
            if (y >= nodey) {
                index += 2;
            }
            if (z >= nodez) {
                index += 1;
            }

            OctreeElement childNode = node.childNodes[index];
            if (childNode != null) {
                // check if child is leaf
                if (childNode.type == OctreeElementType.Leaf) {
                    // replace leaf with node
                    // create a new node and link the leaf to the child node
                    OctreeElement newNode = createNode(node, index);

                    // insert the displaced leaf leaf into new node
                    insertLeaf(newNode, (OctreeLeaf) childNode);

                    childNode = newNode;
                }
                // reinsert into child as continuation
                element = childNode;
                continue;
            }
            // there is no node yet, insert the leaf as child node
            node.childNodes[index] = leaf;
            leaf.parent = node;
            leaf.parentIndex = index;
            break;
        }
    }
    
    protected void remove(OctreeLeaf leaf) {
        // remove leaf from parent
        if(leaf.parent.type == OctreeElementType.Node)
            ((OctreeNode)leaf.parent).childNodes[leaf.parentIndex] = null;
        else if(leaf.parent.type == OctreeElementType.List) {
            ((OctreeList)leaf.parent).children.remove(leaf);
        }
        // clear out references
        leaf.parent=null;
        leaf.object=null;
        leaf.position=null;
    }

    protected OctreeElement createNode(OctreeNode parent, int index) {
        // set ref from parent to this node
        float halfx = parent.bounds.extents.x / 2f;
        float halfy = parent.bounds.extents.y / 2f;
        float halfz = parent.bounds.extents.z / 2f;

        float centerx = parent.bounds.center.x;
        float centery = parent.bounds.center.y;
        float centerz = parent.bounds.center.z;
        
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
        
        OctreeElement node;
        if(halfx<=0.1f 
                && halfy<=0.1f
                && halfz<=0.1f) {
            // if box is smaller than 0.1 units
            node = new OctreeList();
            log.log(Level.INFO, "Created OctreeList");
        } else {
            node = new OctreeNode();
        }
        
        node.bounds.center.x = centerx;
        node.bounds.center.y = centery;
        node.bounds.center.z = centerz;

        // store the halved extents
        node.bounds.extents.x = halfx;
        node.bounds.extents.y = halfy;
        node.bounds.extents.z = halfz;
        node.parent = parent;
        node.parentIndex = index;

        // store ref to child in parent        
        parent.childNodes[index] = node;
        
        return node;
    }

    protected void getIntersecting(OctreeElement element, BoundingBox bb, 
                    FastList store, 
                    boolean threeD) {
        
        FastList<OctreeElement> checknodes;
        FastList<OctreeElement> addnodes;
        
        /*if(LocalContext.isUseThreadLocals()) {
            Context ctx = LocalContext.getContext();
            checknodes = ctx.Octree_getIntersecting_checknodes;
            addnodes = ctx.Octree_getIntersecting_addnodes;
        } else {
        */
            checknodes = new FastList<>(64);
            addnodes = new FastList<>(64);
        //}
        OctreeLeaf leaf;
        //OctreeNode node;
        boolean bbcheck;
        
        checknodes.add(element);
        while(checknodes.size>0) {
            // pop the last element from the stack
            element = checknodes.remove();
        
            if(element.type == OctreeElementType.Leaf) {
                leaf = ((OctreeLeaf)element);
                if(threeD)
                    bbcheck = bb.contains(leaf.position);
                else
                    bbcheck = bb.contains2d(leaf.position);
                if(bbcheck) {
                    store.add(leaf.object);
                }
                continue;
            }

            //node = ((OctreeNode)element);

            if(threeD)
                bbcheck = bb.intersects(element.bounds);
            else
                bbcheck = bb.intersects2d(element.bounds);
            // is there an overlap
            if(!bbcheck) {
                // no intersection at all
                continue;
            }
            
            if(threeD)
                bbcheck = bb.contains(element.bounds);
            else
                bbcheck = bb.contains2d(element.bounds);
            
            // bb contains everything in the bb
            if(bbcheck) {
                // bb contains everything in this node and its children
                addnodes.add(element);
                while(addnodes.size>0) {
                    element = addnodes.remove();
                    
                    if(element.type == OctreeElementType.Leaf) {
                        leaf = ((OctreeLeaf)element);
                        // add the leaf key
                        store.add(leaf.object);
                        continue;
                    }
                    
                    if(element.type == OctreeElementType.Node ) {
                        OctreeNode node = ((OctreeNode)element);
                        // call it on child nodes
                        for(int index=0; index<8; index++) {
                            element = node.childNodes[index];
                            if(element!= null) {
                                // recurse into that child too
                                addnodes.add(element);
                            }
                        }
                        continue;
                    }
                    
                    if(element.type == OctreeElementType.List ) {
                        OctreeList node = ((OctreeList)element);
                        // call it on child nodes
                        for(int index=0; index<node.children.size(); index++) {
                            element = node.children.get(index);
                            if(element!= null) {
                                // recurse into that child too
                                addnodes.add(element);
                            }
                        }
                    }
                }
                // node completely added
                continue;
            }

            // 
            if(element.type == OctreeElementType.List) {
                OctreeList node = (OctreeList) element;
                // check child nodes selectively
                for(int index=0; index<node.children.size(); index++) {
                    element = node.children.get(index);
                    if(element!= null) {
                        // recurse into that child too
                        checknodes.add(element);
                    }
                }
                continue;
            }
            
            if(element.type == OctreeElementType.Node) {
                OctreeNode node = (OctreeNode) element;
                // check child nodes selectively
                for(int index=0; index<8; index++) {
                    element = node.childNodes[index];
                    if(element!= null) {
                        // recurse into that child too
                        checknodes.add(element);
                    }
                }
            }
        }
    }
    
    public int countNodes() {
        FastList<OctreeElement> checknodes;

        
        //if(LocalContext.isUseThreadLocals()) {
        //    Context ctx = LocalContext.getContext();
        //   checknodes = ctx.Octree_getIntersecting_checknodes;
        //} else {
            checknodes = new FastList<>(64);
        //}
        
        OctreeElement element;
        int count=0;
        
        checknodes.add(root);
        while(checknodes.size>0) {
            count++;
            // pop the last element from the stack
            element = checknodes.remove();
            if(element.type == OctreeElementType.Node) {
                OctreeNode node = (OctreeNode) element;
                // check child nodes selectively
                for(int index=0; index<8; index++) {
                    element = node.childNodes[index];
                    if(element== null)
                        continue;
                    if(element.type == OctreeElementType.Node
                            || element.type == OctreeElementType.List) {
                        // recurse into that child node too
                        checknodes.add(element);
                    }
                }
                continue;
            } 
            if(element.type == OctreeElementType.List) {
                OctreeList node = (OctreeList) element;
                // check child nodes selectively
                for(int index=0; index<node.children.size(); index++) {
                    element = node.children.get(index);
                    if(element== null)
                        continue;
                    if(element.type == OctreeElementType.Node
                            || element.type == OctreeElementType.List) {
                        // recurse into that child node too
                        checknodes.add(element);
                    }
                }
            }
        }
        
        return count;
    }
    
    public void rebuild(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        // re-insert all the elements from the other octree
        FastList<OctreeElement> checknodes;
        
        //if(LocalContext.isUseThreadLocals()) {
        //    Context ctx = LocalContext.getContext();
        //    checknodes = ctx.Octree_getIntersecting_checknodes;
        //} else {
            checknodes = new FastList<>(64);
        //}
        checknodes.add(root);
        // clear out with new dimensions and create a new root
        clear(xmin, xmax, ymin, ymax, zmin, zmax);
        
        while (checknodes.size > 0) {
            // pop the last element from the stack
            OctreeElement element = checknodes.remove();
            if (element.type == OctreeElementType.Node) {
                OctreeNode node = (OctreeNode) element;
                // check child nodes selectively
                for (int index = 0; index < 8; index++) {
                    element = node.childNodes[index];
                    if (element == null) {
                        continue;
                    }
                    if (element.type == OctreeElementType.Leaf) {
                        OctreeLeaf leaf = (OctreeLeaf) element;
                        // store the leaf with current position
                        leaf.origPosition.set(leaf.position);
                        // reuse leafs
                        this.insertLeaf(root, leaf);
                    } else {
                        // recurse into that child node too
                        checknodes.add(element);
                    }
                }
                continue;
            }

            if (element.type == OctreeElementType.List) {
                OctreeList node = (OctreeList) element;
                // check child nodes selectively
                for (int index = 0; index < node.children.size(); index++) {
                    element = node.children.get(index);
                    if (element == null) {
                        continue;
                    }
                    if (element.type == OctreeElementType.Leaf) {
                        OctreeLeaf leaf = (OctreeLeaf) element;
                        // store the leaf with current position
                        leaf.origPosition.set(leaf.position);
                        // reuse leafs
                        this.insertLeaf(root, leaf);
                    } else {
                        // recurse into that child node too
                        checknodes.add(element);
                    }
                }
            }
        }
    }
    
    public FastList getAll() {
        FastList store = new FastList();

        FastList<OctreeElement> addnodes;

        //if (LocalContext.isUseThreadLocals()) {
        //    Context ctx = LocalContext.getContext();
        //    addnodes = ctx.Octree_getIntersecting_addnodes;
        //} else {
            addnodes = new FastList<>(64);
        //}
        OctreeLeaf leaf;
        OctreeNode node;
        OctreeElement element;

        // bb contains everything in this node and its children
        addnodes.add(root);
        while (addnodes.size > 0) {
            element = addnodes.get(addnodes.size - 1);
            // pop the last element from the stack
            addnodes.remove(addnodes.size - 1);
            if (element.type == OctreeElementType.Leaf) {
                leaf = ((OctreeLeaf) element);
                // add the leaf key
                store.add((NavCell) leaf.object);
                continue;
            }
            if (element.type == OctreeElementType.Node ) {
                node = ((OctreeNode) element);
                // call it on child nodes
                for (int index = 0; index < 8; index++) {
                    element = node.childNodes[index];
                    if (element != null) {
                        // recurse into that child too
                        addnodes.add(element);
                    }
                }
            }
        }
        return store;
    }
}
