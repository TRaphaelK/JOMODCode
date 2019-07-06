/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Arrays;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.IntList;
import jb2.util.IntList;
import jb2.util.Morton3Dlong;


/**
 * Tree with morton int hashes and int values
 * @author vear
 */
public class MortonTreeLong {
    
    // the packed content of the octree, contains index into leaf keys
    // for each node the range over the items
    protected IntList nodes; // nodesize*5: bitmask, range start, range end in keys, left node index, right node index

    // the size of the keys map
    protected int size;
    // leaf keys contains Morton codes of actual objects, Morton codes also define the bounds of objects
    // if the object has volume (buildings, vehicles)
    // this is a radix sorted array of Morton codes
    // the Morton codes are offset by the center. meaning xcenter - xhalf becomes 0 (xhalf is added to every x)
    // the same is done with z. 
    protected long[] keys;
    // leaf values, contains SSN id or navnode id
    protected int[] values;
    
    // the octree is a quadtree on top levels, and switches to octree after that
    // the reason is that we need much less separation on vertical coordinate than on the others

    // all the objects in the map range from (xcenter - xhalf, zcenter - zhalf) to (xcenter + xhalf, zcenter + zhalf)
    protected double xoffset, yoffset, zoffset, xmult, ymult, zmult;
    
    // the array holding the actual coordinates of added vectors, it is used for precision
    // checks after the morton code matching
    
    
    // the helper array for radix sorting
    protected long[] sortbuffer;
    protected int[] sortbuffervalues;
    
    public MortonTreeLong(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        size = 0;
        // calculate the offsets and multipliers for morton codes
        xoffset = -xmin;
        yoffset = -ymin;
        zoffset = -zmin;
        
        // how much precision can we store in 21 bits?
        xmult = (double)(1<<21 -1) / (xmax - xmin);
        ymult = (double)(1<<21 -1) / (ymax - ymin);
        zmult = (double)(1<<21 -1) / (zmax - zmin);
    }
    
    /**
     * Clear the octree, in principle at each iteration we will be clearing the tree
     * and constructing it again
     */
    public void clear() {
        size=0;
        if(keys==null) {
            keys=new long[128];
            values=new int[128];
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
        // calculate the morton code with offset and multiplier
        long morton = getMortonCode(x,y,z);
        if(size>=keys.length) {
            // expand array
            keys = Arrays.copyOf(keys, keys.length*2);
            values = Arrays.copyOf(values, keys.length);
        }

        // add the morton code to the list
        keys[size] = morton;
        values[size] = value;
        size++;
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
     * Build the octree from added items
     */
    public void build() {
        // sort the morton codes
        radixSort();
        if(nodes==null) {
            nodes = new IntList();
        }
        buildNodes();
    }
    
    /**
     * Radix sort a long array
     * @param array 
     */
    protected void radixSort() {
        
        // go trough all the bits
        long mask = 1;
        
        // use local variables for counters, should be cached t oregisters
        int radixCounter0, radixCounter1;
        
        if(sortbuffer==null || sortbuffer.length < size) {
            sortbuffer = new long[size];
            sortbuffervalues = new int[size];
        }
        
        long[] swaparray;
        int[] swaparrayvalues;

        for(int bit=0; bit <64; bit++) {
            // shift the mask one bit
            mask = 1l << (bit);
            
            // clear the counters
            radixCounter0 = 0;
            radixCounter1 = 0;
            
            // count all the values with bit 0 or 1
            for(int item=0; item < size; item++) {
                if((keys[item]&mask) == 0) {
                    // we got a 0 bit at the position
                    radixCounter0++;
                } else {
                    radixCounter1++;
                }
            }
            // if any of the two counts is 0, then we don't need any rearrangement in this iteration
            if(radixCounter0==0 || radixCounter1==0) {
                continue; // for loop
            }
            // the position index for 1's (for 0 it is 0)
            radixCounter1 = radixCounter0;
            radixCounter0 = 0;
            
            // rearrange items into sortbuffer
            for(int item=0; item < size; item++) {
                if((keys[item]&mask) == 0) {
                    sortbuffer[radixCounter0] = keys[item];
                    sortbuffervalues[radixCounter0] = values[item];
                    radixCounter0++;
                } else {
                    sortbuffer[radixCounter1] = keys[item];
                    sortbuffervalues[radixCounter1] = values[item];
                    radixCounter1++;
                }
            }
            
            // swap keys with sortbuffer
            swaparray = keys;
            keys = sortbuffer;
            sortbuffer = swaparray;
            
            swaparrayvalues = values;
            values = sortbuffervalues;
            sortbuffervalues = swaparrayvalues;

        }
    }
    
    /**
     * The nodes structure is a helper for binary search
     * to know where to split the search range
     */
    protected void buildNodes() {

        // prepare the root
        //nodes.add(0);
        nodes.add(0);
        nodes.add(size - 1);
        nodes.add(0);
        nodes.add(0);

        // repeat/follow added nodes and process them
        for (int pos = 0; pos < nodes.size(); pos += 4) {
            // nodes start with the split in most significant bit
            int start = nodes.get(pos);
            int end = nodes.get(pos + 1);
            long firstCode = keys[start];
            long lastCode = keys[end];

            if (start == end
                    || firstCode == lastCode) {
                // create a leaf node
                // no child nodes
                continue;
            }

            // the depth of the bit, where the two codes differ fist
            long commonPrefix = Long.numberOfLeadingZeros(firstCode ^ lastCode);
            int split = start; // initial guess
            int step = end - start;

            do {
                step = (step + 1) >> 1; // exponential decrease
                int newSplit = split + step; // proposed new position

                if (newSplit < end) {
                    long splitCode = keys[newSplit];
                    long splitPrefix = Long.numberOfLeadingZeros(firstCode ^ splitCode);
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
    
    public long getMortonCode(float x, float y, float z) {
        int xm = (int) ((x+xoffset)*xmult);
        int ym = (int)((y+yoffset)*ymult);
        int zm = (int)((z+zoffset)*zmult);
        
        xm=Math.min(Math.max(xm, 0),(1<<21)-1);
        ym=Math.min(Math.max(ym, 0),(1<<21)-1);
        zm=Math.min(Math.max(zm, 0),(1<<21)-1);

        return Morton3Dlong.encode(xm, ym, zm);
    }
    
    public long getMortonCode(Vector3f vec) {
        return getMortonCode(vec.x, vec.y, vec.z);
    }
    
    protected int getPrefixMask(int commonPrefix) {
        if(commonPrefix==0) {
                return 0;
            }
        return (1<<63)>>(commonPrefix-1);
    }
    
    /**
     * Returns object keys that are either inside or 
     * @param bb
     * @return 
     */
    public IntList getColliding(BoundingBox bb, IntList store) {
        if(store==null) {
            store = new IntList();
        } else {
            store.clear();
        }
        
        // no nodes
        if(nodes.size()==0)
            return store;
        
        // calculate the common part of the bounding boxes coordinates morton codes
        long minmorton = getMortonCode(bb.center.x - bb.extents.x, bb.center.y - bb.extents.y, bb.center.z - bb.extents.z);
        long maxmorton = minmorton;
        long morton = getMortonCode(bb.center.x + bb.extents.x, bb.center.y + bb.extents.y, bb.center.z + bb.extents.z);        
        if(Long.compareUnsigned(minmorton,morton) > 0) {
            minmorton = morton;
        }
        if(Long.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        
        int lowestContainingNode = 0;
        int childNode = 0;
        
        long childCode1 = keys[nodes.get(0)];
        long childCode2 = keys[nodes.get(0 + 1)];

        if ( Long.compareUnsigned(maxmorton, childCode1 ) < 0 || Long.compareUnsigned(childCode2, minmorton) <0 ) {
            // completely out of range
            return store;
        }
        // fix up the min and max with the maps min and max
        if(Long.compareUnsigned(minmorton, childCode1 ) < 0) {
            // discard search range outside of the map
            minmorton = childCode1;
        }

        if(Long.compareUnsigned(childCode2, maxmorton) < 0) {
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
            if(keys[i]>=minmorton && keys[i]<=maxmorton)
                store.add(values[i]);
        }
        
        return store;
    }
    
    protected boolean checkNode(int nodePtr, long minmorton, long maxmorton) {
        long childCode1 = keys[nodes.get(nodePtr)];
        long childCode2 = keys[nodes.get(nodePtr + 1)];

        if ( Long.compareUnsigned(childCode1, minmorton) <= 0  && Long.compareUnsigned(childCode2, maxmorton) >= 0 ) {
                return true;
        }
        return false;
    }
}
