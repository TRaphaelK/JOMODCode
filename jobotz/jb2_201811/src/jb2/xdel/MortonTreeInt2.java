/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.IntList;
import jb2.util.Morton3Dint;

/**
 * Tree with morton int hashes and int values
 * @author vear
 */
@Deprecated
public class MortonTreeInt2 {
    
    // the packed content of the octree, contains index into leaf keys
    // for each node the range over the items
    protected IntList nodes; // nodesize*5: range start, range end, range limit in keys, left node index, right node index

    // leaf keys contains Morton codes of actual objects, Morton codes also define the bounds of objects
    // if the object has volume (buildings, vehicles)
    // this is a radix sorted array of Morton codes
    // the Morton codes are offset by the center. meaning xcenter - xhalf becomes 0 (xhalf is added to every x)
    // the same is done with z. 
    protected IntList keys;
    protected IntList sortbuffer;
    
    // leaf values, contains SSN id or navnode id
    protected IntList values;    
    protected IntList sortbuffervalues;
    
    // the octree is a quadtree on top levels, and switches to octree after that
    // the reason is that we need much less separation on vertical coordinate than on the others

    // all the objects in the map range from (xcenter - xhalf, zcenter - zhalf) to (xcenter + xhalf, zcenter + zhalf)
    protected double xoffset, yoffset, zoffset, xmult, ymult, zmult;
    
    // the min and max indices that changed
    protected int minChangedIdx, maxChangedIdx;
        
    public MortonTreeInt2(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        clear(xmin,  xmax,  ymin,  ymax,  zmin,  zmax);
    }
    
    /**
     * Clear the hash, in principle at each iteration we will be clearing the tree
     * and constructing it again
     */
    public void clear() {
        if(keys==null) {
            keys=new IntList(128);
            values=new IntList(128);
            nodes = new IntList(128);
        } else {
            keys.clear();
            values.clear();
            nodes.clear();
        }
        minChangedIdx = Integer.MAX_VALUE;
        maxChangedIdx = Integer.MIN_VALUE;
    }

    public void clear(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        // calculate the offsets and multipliers for morton codes
        xoffset = -xmin;
        yoffset = -ymin;
        zoffset = -zmin;
        
        // how much precision can we store in 21 bits?
        xmult = (double)(1<<12 -1) / (xmax - xmin);
        ymult = (double)(1<<8 -1) / (ymax - ymin);
        zmult = (double)(1<<12 -1) / (zmax - zmin);
        clear();
    }

    protected int setNode(int index, int start, int end, int left, int right) {
        nodes.ensureCapacity(index+4);
        
        nodes.set(index, start);
        nodes.set(index+1, end);
        nodes.set(index+2, left);
        nodes.set(index+3, right);
        return index;
    }

    protected int findPosition(int morton) {
        if (keys.size == 0) {
            return 0;
        }

        int node = 0;

        while (true) {
            int start = nodes.get(node);
            int end = nodes.get(node + 1);

            int left = nodes.get(node + 2);
            int right = nodes.get(node + 3);

            // get the codes
            int firstCode = keys.get(start);
            int lastCode = keys.get(end);

            if (Integer.compareUnsigned(morton, firstCode) < 0) {
                // before the first root
                return start;
            }

            if (Integer.compareUnsigned(lastCode, morton) < 0) {
                // after the current last
                return end + 1;
            }

            if (left == 0) {
                // no children, this is a leaf node with max 8 values
                // search keys by checking one by one
                for (int i = start; i <= end; i++) {
                    int code = keys.get(i);
                    // if a removed key, skip it
                    if(code==0)
                        continue;
                    if (Integer.compareUnsigned(morton, code) < 0) {
                        // found the first position that is bigger
                        return i;
                    }
                }
                // not found, insert it after the last, situation should have been already handled
                return end+1;
            }

            // check the most significant bit to see which node to continue to
            int commonPrefix = Integer.numberOfLeadingZeros(firstCode ^ lastCode);
            // create the mask
            int mask = (1 << 31) >>> commonPrefix;
            if ((morton & mask) == 0) {
                // the most signicant bit is 0, continue on left node
                node = left;
            } else {
                node = right;
            }
        }
    }

    protected void insertKey2(int morton, int value) {
        if (keys.size == 0) {
            // create root node
            setNode(0, 0, 0, 0, 0);
            keys.set(0, morton);
            values.set(0, value);
            // reserve space for keys, capcacity for each new node is 8
            keys.ensureCapacity(8);
            return;
        }
        
        // ind the insertion position
        int insertPos = findPosition(morton);
        int realInsertpos = insertPos;
        int changeRangeEnd = insertPos;
        
        // is there an empty space before?
        while (realInsertpos > 0 && keys.get(realInsertpos - 1) == 0) {
            realInsertpos--;
        }

        int insertkey = morton;
        int insertvalue = value;
        boolean shift = true;
        if (keys.get(realInsertpos) == 0) {
            // put it at the previous empty space, no need to shift the other items
            // or renumber the nodes
            keys.set(realInsertpos, insertkey);
            values.set(realInsertpos, insertvalue);
            shift=false;
        }
        if(keys.size == realInsertpos) {
            // put it on the end of the map
            keys.ensureCapacity(realInsertpos+1);
            keys.set(realInsertpos, insertkey);
            values.set(realInsertpos, insertvalue);
            shift=false;
        }
        if(shift) {
            // find the next 0
            for (int i=realInsertpos+1; i < keys.size; i++) {
                int code = keys.get(i);
                // found an empty spot, do an array copy
                if (code == 0) {
                    // remember the end of the changed range
                    changeRangeEnd = i;
                    System.arraycopy(keys, realInsertpos, keys, realInsertpos+1, i-realInsertpos);
                    System.arraycopy(values, realInsertpos, values, realInsertpos+1, i-realInsertpos);
                    // insert value into space
                    keys.set(realInsertpos, insertkey);
                    values.set(realInsertpos, insertvalue);
                    shift=false;
                    break;
                }
            }
        }
        if(shift) {
            // we encountered the end of the keys array, need to increase the keys arrays
            int oldCap = keys.size;
            keys.ensureCapacity(oldCap+4);
            values.ensureCapacity(oldCap+4);
            // then do a copy, but leave out some space
            System.arraycopy(keys, realInsertpos, keys, realInsertpos+4, oldCap-realInsertpos);
            System.arraycopy(values, realInsertpos, values, realInsertpos+4, oldCap-realInsertpos);
            // and fill in the value
            keys.set(realInsertpos, insertkey);
            values.set(realInsertpos, insertvalue);
            // clear out the 3 new empty spaces
            for(int i=1; i<4; i++) {
                keys.set(realInsertpos+i, 0);
                values.set(realInsertpos+i, 0);
            }
            // the changed range is until the end of array
            changeRangeEnd = oldCap+4;
        }
    }
    
    protected void updateChangedRange(int value) {
        if (value < minChangedIdx) {
            minChangedIdx = value;
        }
        if(value > maxChangedIdx) {
            maxChangedIdx = value;
        }
    }
    
    protected void createSplit(int pos) {
        // nodes start with the split in most significant bit
        int start = nodes.get(pos);
        int end = nodes.get(pos + 1);
        int firstCode = keys.get(start);
        int lastCode = keys.get(end);

        if (start == end
                || firstCode == lastCode) {
            // create a leaf node
            // no child nodes
           nodes.set(pos+2, 0);
           nodes.set(pos+3, 0);
           return;
        }

        // the depth of the bit, where the two codes differ fist
        int commonPrefix = Integer.numberOfLeadingZeros(firstCode ^ lastCode);
        int split = start; // initial guess
        int step = end - start;

        do {
            step = (step + 1) >> 1; // exponential decrease
            int newSplit = split + step; // proposed new position

            if (newSplit < end) {
                int splitCode = keys.get(newSplit);
                // skip empty keys
                while(splitCode==0) {
                    newSplit++;
                    splitCode = keys.get(newSplit);
                }
                int splitPrefix = Integer.numberOfLeadingZeros(firstCode ^ splitCode);
                if (splitPrefix > commonPrefix) {
                    split = newSplit; // accept proposal
                }
            }
        } while (step > 1);

        // create two nodes for the children
        int childpos = nodes.get(pos+2);
        if(childpos!=0) {
            // the child already exists, split it again
            if(nodes.get(childpos) != start
                    || nodes.get(childpos+1) != split) {
                // need to resplit the child
                nodes.set(childpos, start);
                nodes.set(childpos + 1, split);
                createSplit(childpos);
            }
        } else {
            childpos = nodes.size();
            nodes.set(childpos, start);
            nodes.set(childpos + 1, split);
            nodes.set(childpos + 2, 0);
            nodes.set(childpos + 3, 0);
            // ref to first child
            nodes.set(pos + 2, childpos);
        }

        // allocate an unprocessed child record
        // it will be processed in a later iteration on this for loop
        

        childpos = nodes.get(pos+3);
        if(childpos!=0) {
            if(nodes.get(childpos) != split+1
                    || nodes.get(childpos+1) != end) {
                // need to resplit the child
                nodes.set(childpos, split + 1);
                nodes.set(childpos + 1, end);
                createSplit(childpos);
            }
            
        } else {
            childpos = nodes.size();
            nodes.set(childpos, split + 1);
            nodes.set(childpos + 1, end);
            nodes.set(childpos + 2, 0);
            nodes.set(childpos + 3, 0);
            // ref to second child
            nodes.set(pos + 3, childpos);
        }
    }
    
    /**
     * Add an object identified by value with the given coordinates
     * @param x
     * @param y
     * @param z
     * @param value 
     */
    public void add(float x, float y, float z, int value) {
        
        int morton = getMortonCode(x,y,z);
        
        // descend into the tree, find the insertion position
        int currnode = 0;
        

        
        /*
        int index = keys.size();
        keys.ensureCapacity(index);
        values.ensureCapacity(index);
        
        // calculate the morton code with offset and multiplier
        
        keys.set(index, morton);
        values.set(index, value);
        */
    }
    
    public void add(Vector3f vec, int value) {
        add(vec.x, vec.y, vec.z, value);
    }
    
    public void add(BoundingBox bb, int value) {
        // add the min and max extents
        add(bb.center.x-bb.extents.x, bb.center.y-bb.extents.y, bb.center.z-bb.extents.z, value);
        add(bb.center.x+bb.extents.x, bb.center.y+bb.extents.y, bb.center.z+bb.extents.z, value);
    }
    

    /**
     * The nodes structure is a helper for binary search
     * to know where to split the search range
     */
    protected void buildNodes() {

        // prepare the root
        //nodes.add(0);
        nodes.add(0);
        nodes.add(keys.size - 1);
        nodes.add(0);
        nodes.add(0);

        // repeat/follow added nodes and process them
        for (int pos = 0; pos < nodes.size(); pos += 4) {
            // nodes start with the split in most significant bit
            int start = nodes.get(pos);
            int end = nodes.get(pos + 1);
            int firstCode = keys.get(start);
            int lastCode = keys.get(end);

            if (start == end
                    || firstCode == lastCode) {
                // create a leaf node
                // no child nodes
                continue;
            }

            // the depth of the bit, where the two codes differ fist
            int commonPrefix = Integer.numberOfLeadingZeros(firstCode ^ lastCode);
            int split = start; // initial guess
            int step = end - start;

            do {
                step = (step + 1) >> 1; // exponential decrease
                int newSplit = split + step; // proposed new position

                if (newSplit < end) {
                    int splitCode = keys.get(newSplit);;
                    
                    while(splitCode==0) {
                        newSplit++;
                        splitCode = keys.get(newSplit);
                    }

                    int splitPrefix = Integer.numberOfLeadingZeros(firstCode ^ splitCode);
                    if (splitPrefix > commonPrefix) {
                        split = newSplit; // accept proposal
                    }
                }
            } while (step > 1);

            // create two nodes for the children
            int childpos = nodes.size();
            // allocate an unprocessed child record
            // it will be processed in a later iteration on this for loop
            nodes.set(childpos, start);
            nodes.set(childpos + 1, split);
            nodes.set(childpos + 2, 0);
            nodes.set(childpos + 3, 0);
            // ref to first child
            nodes.set(pos + 2, childpos);

            childpos = nodes.size();
            nodes.set(childpos, split + 1);
            nodes.set(childpos + 1, end);
            nodes.set(childpos + 2, 0);
            nodes.set(childpos + 3, 0);
            // ref to second child
            nodes.set(pos + 3, childpos);

        }
    }
    
    protected int getCommonBits(int firstCode, int secondCode) {
        return Integer.numberOfLeadingZeros(firstCode ^ secondCode);
    }
    
    public int getMortonCode(float x, float y, float z) {
        int xm = (int) ((x+xoffset)*xmult);
        int ym = (int)((y+yoffset)*ymult);
        int zm = (int)((z+zoffset)*zmult);
        
        xm=Math.min(Math.max(xm, 0),(1<<12)-1);
        ym=Math.min(Math.max(ym, 0),(1<<8)-1);
        zm=Math.min(Math.max(zm, 0),(1<<12)-1);

        return Morton3Dint.encode(xm, ym, zm);
    }
    
    public int getMortonCode(Vector3f vec) {
        return getMortonCode(vec.x, vec.y, vec.z);
    }
    
    protected int getPrefixMask(int commonPrefix) {
        if(commonPrefix==0) {
                return 0;
            }
        return (1<<31)>>(commonPrefix-1);
    }
    
    /**
     * Returns object keys that are either inside or 
     * @param bb
     * @return 
     */
    public IntList getContained(BoundingBox bb, IntList store) {
        if(store==null) {
            store = new IntList();
        } else {
            store.clear();
        }
        
        // no nodes
        if(nodes.size()==0)
            return store;
        
        // calculate the common part of the bounding boxes coordinates morton codes
        int minmorton = getMortonCode(bb.center.x - bb.extents.x, bb.center.y - bb.extents.y, bb.center.z - bb.extents.z);
        int maxmorton = minmorton;
        int morton = getMortonCode(bb.center.x + bb.extents.x, bb.center.y + bb.extents.y, bb.center.z + bb.extents.z);        
        if(Integer.compareUnsigned(minmorton,morton) > 0) {
            minmorton = morton;
        }
        if(Integer.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        
        int lowestContainingNode = 0;
        int childNode = 0;
        
        int childCode1 = keys.get(nodes.get(0));
        int childCode2 = keys.get(nodes.get(0 + 1));

        if ( Integer.compareUnsigned(maxmorton, childCode1 ) < 0 || Integer.compareUnsigned(childCode2, minmorton) <0 ) {
            // completely out of range
            return store;
        }
        // fix up the min and max with the maps min and max
        if(Integer.compareUnsigned(minmorton, childCode1 ) < 0) {
            // discard search range outside of the map
            minmorton = childCode1;
        }

        if(Integer.compareUnsigned(childCode2, maxmorton) < 0) {
            // discard search range outside of map
            maxmorton = childCode2;
        }
        
        // portion of the range is common, do check against top node
        /*
        if(!checkNode(lowestContainingNode, minmorton, maxmorton)) {
            // the root node does not contain the bb, exit
            return store;
        }*/

        do {
            // check if the mask matches one of the children
            childNode = nodes.get(lowestContainingNode + 2);
            if (childNode != 0) {
                if( checkNode(childNode, minmorton, maxmorton)) {
                    lowestContainingNode = childNode;
                    continue;
                }
            }
            // check the other child node
            childNode = nodes.get(lowestContainingNode + 3);
            if (childNode != 0) {
                if( checkNode(childNode, minmorton, maxmorton)) {
                    lowestContainingNode = childNode;
                    continue;
                }
            }

            childNode = 0;
        } while (childNode != 0);
        
        // go trough all the indices int the range of the node, and return them
        int start = nodes.get(lowestContainingNode);
        int end = nodes.get(lowestContainingNode+1);
        for(int i=start; i<= end; i++ ) {
            int key = keys.get(i);
            if(key>=minmorton && key<=maxmorton)
                store.add(values.get(i));
        }
        
        return store;
    }
    
    protected boolean checkNode(int nodePtr, int minmorton, int maxmorton) {
        // get min and max codes of the node
        int childCode1 = keys.get(nodes.get(nodePtr));
        int childCode2 = keys.get(nodes.get(nodePtr + 1));

        if ( Integer.compareUnsigned(childCode1, minmorton) <= 0  && Integer.compareUnsigned(childCode2, maxmorton) >= 0 ) {
                return true;
        }
        return false;
    }
}
