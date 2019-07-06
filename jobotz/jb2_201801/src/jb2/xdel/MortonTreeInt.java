/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.IntIntMap;
import jb2.util.IntList;
import jb2.util.JbException;
import jb2.util.Morton3Dint;

/**
 * Tree with morton int hashes and int values
 * @author vear
 */
public class MortonTreeInt {
    
    protected final int nodeSize = 4;
    // the packed content of the octree, contains index into leaf keys
    // for each node the range over the items
    protected IntList nodes; // nodesize*4: range start, range end in keys, 
                             // left node index, right node index

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
    
    // the hashmap of values to array indices
    // items are removed from here, filled after rebuild
    protected IntIntMap valuesMap;
    // the hashmap of added values to keys indices (which are over the root)
    // cleared after build
    protected IntIntMap addedValues;
    protected IntList addedValuesList;
    protected IntList addedKeysList;
    
    // the octree is a quadtree on top levels, and switches to octree after that
    // the reason is that we need much less separation on vertical coordinate than on the others

    // all the objects in the map range from (xcenter - xhalf, zcenter - zhalf) to (xcenter + xhalf, zcenter + zhalf)
    protected double xoffset, yoffset, zoffset, xmult, ymult, zmult;
    protected float xmin, xmax, ymin, ymax, zmin, zmax;
    
    protected int numModifications = 0;
    
    // the array holding the actual coordinates of added vectors, it is used for precision
    // checks after the morton code matching
        
    public MortonTreeInt(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        clear(xmin,  xmax,  ymin,  ymax,  zmin,  zmax);
    }
    
    /**
     * Clear the hash, in principle at each iteration we will be clearing the tree
     * and constructing it again
     */
    public void clear() {
        if(keys==null) {
            keys=new IntList(2048);
            values=new IntList(2048);
            valuesMap=new IntIntMap(2048);
            addedValues=new IntIntMap(2048);
            addedValuesList=new IntList(2048);
            addedKeysList=new IntList(2048);
        } else {
            keys.size=0;
            values.size=0;
            valuesMap.clear();
            addedValues.clear();
            addedValuesList.size= 0;
            addedKeysList.size = 0;
        }
        numModifications=0;
    }
    
    public void clear(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.zmin = zmin;
        this.zmax = zmax;

        // calculate the offsets and multipliers for morton codes
        xoffset = -xmin;
        yoffset = -ymin;
        zoffset = -zmin;
        
        // how much precision can we store in 21 bits?
        xmult = (double)((1<<Morton3Dint.xbits) -1) / (double)(xmax - xmin);
        ymult = (double)((1<<Morton3Dint.ybits) -1) / (double)(ymax - ymin);
        zmult = (double)((1<<Morton3Dint.zbits) -1) / (double)(zmax - zmin);
        clear();
    }
    
    /**
     * Add an object identified by value with the given coordinates
     * @param x
     * @param y
     * @param z
     * @param value 
     */
    public int put(float x, float y, float z, int value) {
        // calculate the morton code with offset and multiplier
        int morton = getMortonCode(x,y,z);
        put(morton, value);
        return morton;
    }
    
    public int put(Vector3f vec, int value) {
        return put(vec.x, vec.y, vec.z, value);
    }
    
    public int put(int morton, int value) {
        if(morton==0)
            throw new JbException("Tried to insert 0 morton code");
        if(value==0)
            throw new JbException("Tried to insert 0 value");
        
        int index=0;
        if(valuesMap.size()>0) {
            index = valuesMap.remove(value);
            if(index!=0) {
                // clear out the value from the main map
                values.set(index, 0);
            }
        }

        // add it to added, so that checking considers this too
        addedValues.put(value, morton);
        //numModifications++;
        return morton;
    }
    
    public void remove(int value) {
        int index = valuesMap.remove(value);
        if(index!=0) {
            // clear out the value from the main map
            values.set(index, 0);
        }
        // also remove from new additions if its there
        addedValues.remove(value);

        numModifications++;
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
        numModifications=0;
    }
    
    public int getNumModifications() {
        return addedValues.size() + numModifications;
    }
    
    public int getSize() {
        return keys.size;
    }
    
    /**
     * Radix sort a long array
     * @param array 
     */
    protected void radixSort() {
        
        // go trough all the bits
        int mask = 1;
        
        // use local variables for counters, should be cached t oregisters
        int radixCounter0, radixCounter1;
        
        addedValues.getKeysAndValues(addedValuesList, addedKeysList);
        // add all added keys and value
        keys.addAll(addedKeysList);
        values.addAll(addedValuesList);
        // clear the added lists
        addedValues.clear();
        
        if(sortbuffer==null) {
            sortbuffer = new IntList(keys.size());
            sortbuffervalues = new IntList(keys.size());
        } else {
             sortbuffer.ensureCapacity(keys.size);
             sortbuffervalues.ensureCapacity(keys.size);
        }

        for(int bit=0; bit <32; bit++) {
            sortbuffer.size = 0;
            sortbuffervalues.size = 0;
            
            // shift the mask one bit
            mask = 1 << (bit);
            
            // clear the counters
            radixCounter0 = 0;
            radixCounter1 = 0;
            boolean hasempty=false;
            
            // count all the values with bit 0 or 1
            for(int item=0; item < keys.size; item++) {
                // skip the removed items
                int value = values.get(item);
                if(value==0) {
                    hasempty=true;
                    continue;
                }
                int key = keys.get(item);
                if((key&mask) == 0) {
                    // we got a 0 bit at the position
                    radixCounter0++;
                } else {
                    radixCounter1++;
                }
            }
            // if any of the two counts is 0, then we don't need any rearrangement in this iteration
            if((radixCounter0==0 || radixCounter1==0) && !hasempty) {
                continue; // for loop
            }
            // the position index for 1's (for 0 it is 0)
            radixCounter1 = radixCounter0;
            radixCounter0 = 0;
            
            // rearrange items into sortbuffer
            for(int item=0; item < keys.size(); item++) {
                int value = values.get(item);
                if(value==0)
                    continue;
                int key = keys.get(item);
                if((key&mask) == 0) {
                    sortbuffer.set(radixCounter0, key);
                    sortbuffervalues.set(radixCounter0, value);
                    radixCounter0++;
                } else {
                    sortbuffer.set(radixCounter1, key);
                    sortbuffervalues.set(radixCounter1, value);
                    radixCounter1++;
                }
            }
            
            IntList swaparray;
            // swap keys with sortbuffer
            swaparray = keys;
            keys = sortbuffer;
            sortbuffer = swaparray;
            IntList swaparrayvalues;
            swaparrayvalues = values;
            values = sortbuffervalues;
            sortbuffervalues = swaparrayvalues;

        }

        valuesMap.clear();

        // write the established order into the map
        for(int i=0; i<values.size; i++) {
            valuesMap.put(values.get(i), i);
        }
    }

    /**
     * The nodes structure is a helper for binary search
     * to know where to split the search range
     */
    protected void buildNodes() {

        nodes.clear();
        // clear out the added list
        addedValues.clear();
        
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
                    int splitCode = keys.get(newSplit);
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
        int xm = (int)((x+xoffset)*xmult);
        int ym = (int)((y+yoffset)*ymult);
        int zm = (int)((z+zoffset)*zmult);
        
        /*
        xm=Math.min(Math.max(xm, 0),(1<<xbits)-1);
        ym=Math.min(Math.max(ym, 0),(1<<ybits)-1);
        zm=Math.min(Math.max(zm, 0),(1<<zbits)-1);
        */

        return Morton3Dint.encode(xm, ym, zm);
    }

    public int getMortonCodeClamp(float x, float y, float z) {
        int xm = (int) ((x+xoffset)*xmult);
        int ym = (int)((y+yoffset)*ymult);
        int zm = (int)((z+zoffset)*zmult);
                
        xm=Math.min(Math.max(xm, 0),(1<<Morton3Dint.xbits)-1);
        ym=Math.min(Math.max(ym, 0),(1<<Morton3Dint.ybits)-1);
        zm=Math.min(Math.max(zm, 0),(1<<Morton3Dint.zbits)-1);

        return Morton3Dint.encode(xm, ym, zm);
    }
    
    public Vector3f decodeMortonCode(int morton, Vector3f store) {
        if(store==null)
            store = new Vector3f();
        
        int xm = Morton3Dint.decodeX(morton);
        int ym = Morton3Dint.decodeY(morton);
        int zm = Morton3Dint.decodeZ(morton);
        
        store.x = (float) (((float)xm/xmult)-xoffset);
        store.y = (float) (((float)ym/ymult)-yoffset);
        store.z = (float) (((float)zm/zmult)-zoffset);
        
        return store;
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
        float xmi = Math.max(bb.center.x - bb.extents.x, this.xmin);
        float xma = Math.min(bb.center.x + bb.extents.x, this.xmax);

        float ymi = Math.max(bb.center.y - bb.extents.y, this.ymin);
        float yma = Math.min(bb.center.y + bb.extents.y, this.ymax);

        float zmi = Math.max(bb.center.z - bb.extents.z, this.zmin);
        float zma = Math.min(bb.center.z + bb.extents.z, this.zmax);
        
        int minmorton = getMortonCodeClamp(xmi, ymi, zmi);
        int maxmorton = getMortonCodeClamp(xma, yma, zma);
        
        int morton = getMortonCodeClamp(xmi, ymi, zma);
        if(Integer.compareUnsigned(minmorton,morton) > 0){
            minmorton = morton;
        }
        if(Integer.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        
        morton = getMortonCodeClamp(xmi, yma, zmi);
        if(Integer.compareUnsigned(minmorton,morton) > 0){
            minmorton = morton;
        }
        if(Integer.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        morton = getMortonCodeClamp(xmi, yma, zma);
        if(Integer.compareUnsigned(minmorton,morton) > 0){
            minmorton = morton;
        }
        if(Integer.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        morton = getMortonCodeClamp(xma, ymi, zmi);
        if(Integer.compareUnsigned(minmorton,morton) > 0){
            minmorton = morton;
        }
        if(Integer.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        morton = getMortonCodeClamp(xma, ymi, zma);
        if(Integer.compareUnsigned(minmorton,morton) > 0){
            minmorton = morton;
        }
        if(Integer.compareUnsigned(maxmorton,morton) < 0) {
            maxmorton = morton;
        }
        morton = getMortonCodeClamp(xma, yma, zmi);
        if(Integer.compareUnsigned(minmorton,morton) > 0){
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
        
        int miny = minmorton & 0x3f;
        int maxy = maxmorton & 0x3f;
        int minx = Morton3Dint.decodeX(minmorton);
        int maxx = Morton3Dint.decodeX(maxmorton);
        int minz = Morton3Dint.decodeZ(minmorton);
        int maxz = Morton3Dint.decodeZ(maxmorton);
        
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
            int y = Morton3Dint.decodeY(key);
            if(Integer.compareUnsigned(key,minmorton)<0)
                continue;
            if(Integer.compareUnsigned(key,maxmorton)<=0) {
                if(y>=miny && y<=maxy) {
                    int value = values.get(i);
                    // if its not a removed value
                    if(value!=0)
                        store.add(value);
                }
                    
                continue;
            }
            // no need to continue
            break;
        }
        
        // go trough and check all the changed/added values
        if(addedValues.size()>0) {
            int addenum = addedValues.getRawSize();
            for(int i=0;i<addenum; i++) {
                int key = addedValues.getKeyByIndex(i);
                if(Integer.compareUnsigned(key,minmorton)<0)
                    continue;
                if(Integer.compareUnsigned(key,maxmorton)<=0) {
                  //  if(y>=miny && y<=maxy)
                    int value = addedValues.getValueByIndex(i);
                        // if its not a removed value
                    if(value!=0)
                        store.add(value);
                    continue;
                }
            }
        }
        return store;
    }
    
    public IntList getContained2D(BoundingBox bb, IntList store) {
        if(store==null) {
            store = new IntList();
        } else {
            store.clear();
        }
        
        // no nodes
        if(nodes.size()==0)
            return store;
        
        float xmi = Math.max(bb.center.x - bb.extents.x, this.xmin);
        float xma = Math.min(bb.center.x + bb.extents.x, this.xmax);

        float zmi = Math.max(bb.center.z - bb.extents.z, this.zmin);
        float zma = Math.min(bb.center.z + bb.extents.z, this.zmax);
        
        int minmorton = getMortonCodeClamp(xmi, 0, zmi);
        int maxmorton = getMortonCodeClamp(xma, 0, zma);
        
        int lowestContainingNode = 0;
        int childNode = 0;
        minmorton=minmorton>>>6;
        maxmorton=maxmorton>>>6;
        
        int childCode1 = keys.get(nodes.get(0))>>>6;
        int childCode2 = keys.get(nodes.get(0 + 1))>>>6;

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
                if( checkNode2D(childNode, minmorton, maxmorton)) {
                    lowestContainingNode = childNode;
                    continue;
                }
            }
            // check the other child node
            childNode = nodes.get(lowestContainingNode + 3);
            if (childNode != 0) {
                if( checkNode2D(childNode, minmorton, maxmorton)) {
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
            //int y = key & 0x3f;
            key = key >>>6;
            if(Integer.compareUnsigned(key,minmorton)<0)
                continue;
            if(Integer.compareUnsigned(key,maxmorton)<=0) {
              //  if(y>=miny && y<=maxy)
                int value = values.get(i);
                    // if its not a removed value
                if(value!=0)
                    store.add(value);
                continue;
            }
            // no need to continue
            break;
        }
        // go trough and check all the changed/added values
        if(addedValues.size()>0) {
            int addenum = addedValues.getRawSize();
            for(int i=0;i<addenum; i++) {
                int key = addedValues.getKeyByIndex(i);
                key = key >>>6;
                if(Integer.compareUnsigned(key,minmorton)<0)
                    continue;
                if(Integer.compareUnsigned(key,maxmorton)<=0) {
                  //  if(y>=miny && y<=maxy)
                    int value = addedValues.getValueByIndex(i);
                        // if its not a removed value
                    if(value!=0)
                        store.add(value);
                    continue;
                }
            }
        }
        
        return store;
    }
    
    protected boolean checkNode(int nodePtr, int minmorton, int maxmorton) {
        // get min and max codes of the node
        int childCode1 = keys.get(nodes.get(nodePtr));
        int childCode2 = keys.get(nodes.get(nodePtr + 1));

        if ( Integer.compareUnsigned(childCode1, minmorton ) <= 0  
                && Integer.compareUnsigned(maxmorton, childCode2) <= 0 ) {
                return true;
        }
        return false;
    }
    
    protected boolean checkNode2D(int nodePtr, int minmorton, int maxmorton) {
        // get min and max codes of the node
        int childCode1 = keys.get(nodes.get(nodePtr))>>>6;
        int childCode2 = keys.get(nodes.get(nodePtr + 1))>>>6;

        if ( Integer.compareUnsigned(childCode1, minmorton ) <= 0  
                && Integer.compareUnsigned(maxmorton, childCode2) <= 0 ) {
                return true;
        }
        return false;
    }
}
