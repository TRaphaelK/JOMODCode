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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.math.Ray;

/**
 * Octree. Have compared 3 versions of octree: morton tree, octree with packed arrays 
 * and tree with object references. Can't fool the JVM, the object version is as fast as the packed 
 * array version. Perhaps with bigger data volumes there would be a difference, but with the amount
 * of game objects and nav nodes we are working, the object based is as fast.
 * @author vear
 */
public class Octree {
    
    protected static final Logger log = Logger.getLogger(Octree.class.getName());
    
    protected Node root;
    
    public enum ElementType {
        Node,
        Leaf
    }

    protected class Element {
        ElementType type;
        BoundingBox bounds = new BoundingBox();
        Node parent;
        int parentIndex;
        
        public void detachFromNode() {
            if(parent==null)
                throw new JbException("Element has no parent node");
            parent.children.set(parentIndex, null);
            if(parentIndex>8 && parentIndex==parent.children.size) {
                // if it was the last element, remove it
                // TODO: this could be enhanced so that the last element
                // is moved back to the removed position
                // and the parentIndex is rewritten in the moved leaf
                parent.children.size--;
            }
            parent = null;
            parentIndex=-1;
        }
    }
    
    public class Node extends Element {
        FastList<Element> children=new FastList<>(8);
        Node() {
            type=ElementType.Node;
        }
    }
    
    public class Leaf extends Element {
        Object object;
        Leaf() {
            type=ElementType.Leaf;
        }

    }
    
    public class OctreeItearator implements Iterator<Object> {

        protected Element currentNode;
        protected int currentIndex;
        FastList<Element> checknodes;
        protected Leaf next;
        
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
            Node node;
            
            next = null;
            while(currentNode!=null || !checknodes.isEmpty() ) {
                
                 if(currentNode==null) {
                    // pop a new node from stack
                     currentNode = checknodes.remove();
                     currentIndex = 0;
                 }
                 
                 Element element=null;
                 if(currentNode!=null) {
                    // first determine the element
                    // and if the current node is still valid
                    if(currentNode.type == ElementType.Node) {
                        node = (Node) currentNode;
                        if(currentIndex<node.children.size) {
                            element= node.children.get(currentIndex);
                            currentIndex++;
                        } else {
                            currentNode=null;
                        }
                    }
                    
                    // check if next is a leaf or node
                    if(element!=null) {
                        if(element.type == ElementType.Node) {
                            // new level node
                            checknodes.add(element);
                        } else if(element.type == ElementType.Leaf ){
                            // extract the next
                            next = (Leaf) element;
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
    
    public Octree(){
        clear();
    }
    
    public OctreeItearator iterator() {
        return new OctreeItearator();
    }
    
    public void clear() {
        createRoot(1024f);
    }

    public void clear(float extents) {
        createRoot(extents);
    }
    
    protected void createRoot(float extents) {
        root = new Node();
        // reserve first 8 child positions
        root.children.size=8;
        
        root.bounds.center.x = 0;
        root.bounds.center.y = 0;
        root.bounds.center.z = 0;

        root.bounds.extents.x = extents;
        root.bounds.extents.y = extents;
        root.bounds.extents.z = extents;
    }
    
    protected int getIndexOf(Node parent, Element child) {
        float x = child.bounds.center.x;
        float y = child.bounds.center.y;
        float z = child.bounds.center.z;
        
        float nodex = parent.bounds.center.x;
        float nodey = parent.bounds.center.y;
        float nodez = parent.bounds.center.z;
            
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
        return index;
    }
    
    protected void insertLeaf(Node node, Leaf leaf) {
        
        float x = leaf.bounds.center.x;
        float y = leaf.bounds.center.y;
        float z = leaf.bounds.center.z;
        
        while (true) {

            // if the leaf would intersact multiple parts of the node, then just store it as a direct child
            if(!leaf.bounds.sameSide(node.bounds.center)) {
                leaf.parentIndex = node.children.size;
                node.children.set(leaf.parentIndex, leaf);                
                leaf.parent = node;
                return;
            }
            
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

            Element childNode = node.children.get(index);
            if (childNode != null) {
                // check if child is leaf
                if (childNode.type == ElementType.Leaf) {
                    // replace leaf with a node
                    // create a new node and link the leaf to the child node
                    Node newNode = createNode(node, index);

                    // insert the displaced leaf leaf into new node
                    insertLeaf(newNode, (Leaf) childNode);

                    childNode = newNode;
                }
                // reinsert into child as continuation
                node = (Node) childNode;
                continue;
            }
            // there is no node yet, insert the leaf as child node
            node.children.set(index, leaf);
            leaf.parent = node;
            leaf.parentIndex = index;
            break;
        }
    }
    
    protected void remove(Leaf leaf) {
        // remove leaf from parent
        if(leaf.parentIndex<leaf.parent.children.size-1) {
            leaf.parent.children.set(leaf.parentIndex,null);
        } else {
            leaf.parent.children.remove(leaf.parentIndex);
        }
        // clear out references
        leaf.parent=null;
    }

    protected Node createNode(Node parent, int index) {
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
        
        Node node = new Node();
        // reserve first 8 child positions
        node.children.size=8;
        
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
        parent.children.set(index, node);
        
        return node;
    }

    public FastList getIntersecting(BoundingBox bb, FastList store) {
        if(store==null) {
            store = new FastList<>();
        } else {
            store.clear();
        }
        
        getIntersecting(root, bb, store, true);
       
        return store;
    }
    
    public FastList getIntersecting2d(BoundingBox bb, FastList store) {
        if(store==null) {
            store = new FastList<>();
        } else {
            store.clear();
        }
        
        getIntersecting(root, bb, store, false);
       
        return store;
    }


    protected void getIntersecting(Node node, BoundingBox bb, 
                    FastList store, 
                    boolean threeD) {
        
        FastList<Node> checknodes= new FastList<>(64);
        
        boolean bbcheck;
        
        checknodes.add(node);
        while(checknodes.size>0) {
            // pop the last element from the stack
            node = checknodes.remove();

            if(threeD)
                bbcheck = bb.intersects(node.bounds);
            else
                bbcheck = bb.intersects2d(node.bounds);
            // is there an overlap
            if(!bbcheck) {
                // no intersection at all
                continue;
            }
            
            if(threeD)
                bbcheck = bb.contains(node.bounds);
            else
                bbcheck = bb.contains2d(node.bounds);
            
            // bb contains everything in the bb
            if(bbcheck) {
                // bb contains everything in this node and its children
                // add everything unconditionally
                getAll(node, store);
                // node completely added
                continue;
            }
            
            // check child nodes selectively
            for(int index=0; index<node.children.size(); index++) {
                Element element = node.children.get(index);
                if(element== null)
                    continue;
                if(element.type == ElementType.Leaf) {
                    Leaf leaf = ((Leaf)element);
                    if(threeD)
                        bbcheck = bb.intersects(leaf.bounds);
                    else
                        bbcheck = bb.intersects2d(leaf.bounds);
                    if(bbcheck) {
                        store.add(leaf.object);
                    }
                    continue;
                }
                // recurse into that child too
                checknodes.add((Node) element);
            }
        }
    }
    
    protected void getAll(Node node, FastList store) {
        FastList<Node> addnodes= new FastList<>(64);
        addnodes.add(node);
        while (addnodes.size > 0) {
            // pop the last element from the stack
            node = addnodes.remove();
            for (int index = 0; index < node.children.size; index++) {
                Element element = node.children.get(index);
                if (element == null)
                    continue;
                if (element.type == ElementType.Leaf) {
                    Leaf leaf = ((Leaf) element);
                    // add the leaf key
                    store.add(leaf.object);
                    continue;
                }
                addnodes.add((Node) element);
            }
        }
    }
    
    public int countNodes() {
        FastList<Node> checknodes= new FastList<>(64);;
        
        int count=0;
        
        checknodes.add(root);
        while(checknodes.size>0) {
            count++;
            // pop the last element from the stack
            Node node = checknodes.remove();
            // check child nodes selectively
            for(int index=0; index<node.children.size; index++) {
                Element element = node.children.get(index);
                if(element== null)
                    continue;
                if(element.type == ElementType.Node) {
                    // recurse into that child node too
                    checknodes.add((Node) element);
                }
            }
        }
        
        return count;
    }
    
    public void expand(float extents) {
        long time = System.nanoTime();
        // re-insert all the elements from the other octree
        FastList<Node> checknodes = new FastList<>(64);
        checknodes.add(root);
        // clear out with new dimensions and create a new root
        clear(extents);
        
        while (checknodes.size > 0) {
            // pop the last element from the stack
            Node node = checknodes.remove();
            // check child nodes selectively
            for (int index = 0; index < node.children.size; index++) {
                Element element = node.children.get(index);
                if (element == null) {
                    continue;
                }
                if (element.type == ElementType.Leaf) {
                    Leaf leaf = (Leaf) element;
                    // reuse leafs
                    this.insertLeaf(root, leaf);
                } else {
                    // recurse into that child node too
                    checknodes.add((Node) element);
                }
            }
        }
        time = System.nanoTime() - time;
        log.log(Level.INFO, "Octree rebuild in {0} ms", FastMath.nanoTimeToMilis(time));
    }
    
    public FastList getAll() {
        FastList store = new FastList();

        getAll(root, store);
        return store;
    }
    
    protected void ensureOctreeSize(Leaf leaf) {
        // only blockers are added
        float extx = FastMath.max(FastMath.abs(leaf.bounds.center.x-leaf.bounds.extents.x),
                FastMath.abs(leaf.bounds.center.x+leaf.bounds.extents.x));
        float exty = FastMath.max(FastMath.abs(leaf.bounds.center.y-leaf.bounds.extents.y),
                FastMath.abs(leaf.bounds.center.y+leaf.bounds.extents.y));
        float extz = FastMath.max(FastMath.abs(leaf.bounds.center.z-leaf.bounds.extents.z),
                FastMath.abs(leaf.bounds.center.z+leaf.bounds.extents.z));

        float ext=extx;
        if(exty>ext)
            ext=exty;
        if(extz>ext)
            ext=extz;
        
        // box to be added is outside bounds, need to rebuild the tree to be bigger
        float rext = root.bounds.extents.x;
        if(rext<ext) {
            // double the size until the element fits
            while(rext<ext)
                rext = rext *2f;
            expand(rext);
        }
    }
    
    public Leaf add(Object refObject, BoundingBox box) {
        Leaf leaf;
        // create new leaf
        leaf = new Leaf();
        leaf.bounds.set(box);
        leaf.object = refObject;
        ensureOctreeSize(leaf);
        
        insertLeaf(root, leaf);
        return leaf;
    }
    
    protected void getIntersecting(Node node, Ray ray, 
                    FastList store) {
        FastList<Node> checknodes= new FastList<>(64);
                
        checknodes.add(node);
        while(checknodes.size>0) {
            // pop the last element from the stack
            node = checknodes.remove();
            // check child nodes selectively
            for(int index=0; index<node.children.size(); index++) {
                Element element = node.children.get(index);
                if(element== null)
                    continue;
                boolean bbcheck = element.bounds.intersects(ray);
                if(!bbcheck)
                    continue;

                if(element.type == ElementType.Leaf) {
                    Leaf leaf = ((Leaf)element);
                    store.add(leaf.object);
                    continue;
                }                    
                // recurse into that child too
                checknodes.add((Node) element);
            }
        }
    }
    
    public boolean hasIntersecting(BoundingBox bb) {
        return hasIntersecting(bb, true);
    }

    public boolean hasIntersecting2d(BoundingBox bb) {
        return hasIntersecting(bb, false);
    }
    
    protected boolean hasIntersecting(BoundingBox bb, boolean threeD) {
        
        FastList<Node> checknodes= new FastList<>(64);
        FastList<Node> addnodes= new FastList<>(64);
        
        boolean bbcheck;
        
        checknodes.add(root);
        while(checknodes.size>0) {
            // pop the last element from the stack
            Node node = checknodes.remove();

            if(threeD)
                bbcheck = bb.intersects(node.bounds);
            else
                bbcheck = bb.intersects2d(node.bounds);
            // is there an overlap
            if(!bbcheck) {
                // no intersection at all
                continue;
            }
            
            if(threeD)
                bbcheck = bb.contains(node.bounds);
            else
                bbcheck = bb.contains2d(node.bounds);
            
            // bb contains everything in the bb
            if(bbcheck) {
                // bb contains everything in this node and its children
                // find the first leaf regardless of bound checks
                addnodes.add(node);
                while (addnodes.size > 0) {
                    // pop the last element from the stack
                    node = addnodes.remove();
                    for (int index = 0; index < node.children.size; index++) {
                        Element element = node.children.get(index);
                        if (element == null)
                            continue;
                        if (element.type == ElementType.Leaf) {
                            Leaf leaf = ((Leaf) element);
                            // if valid leaf found
                            if(leaf.object!=null)
                                return true;
                            continue;
                        }
                        addnodes.add((Node) element);
                    }
                }
                // node completely added
                continue;
            }
            
            // check child nodes selectively
            for(int index=0; index<node.children.size(); index++) {
                Element element = node.children.get(index);
                if(element== null)
                    continue;
                if(element.type == ElementType.Leaf) {
                    Leaf leaf = ((Leaf)element);
                    if(leaf.object==null)
                        continue;
                    if(threeD)
                        bbcheck = bb.intersects(leaf.bounds);
                    else
                        bbcheck = bb.intersects2d(leaf.bounds);
                    if(bbcheck) {
                        // found first intersecting leaf
                        return true;
                    }
                    continue;
                }
                // recurse into that child too
                checknodes.add((Node) element);
            }
        }
        // not found any intersecting leaves
        return false;
    }

}
