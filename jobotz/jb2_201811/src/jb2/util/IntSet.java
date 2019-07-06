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

import java.util.Arrays;

/**
 * A HashSet for int keys
 * @author vear (Arpad Vekas)
 */
public class IntSet {
    private static final short DEFAULT_NULL_KEY = 0;
    
    int nullKey = 0;
    
    // the number of entryies in the map
    int size = 0;
    
    // the array holding the new values
    int[] keys;
    
    public IntSet() {
        this(16,DEFAULT_NULL_KEY);
    }
    
    private static int nextPowerOfTwo(int num) {
 	int cap=16;
        while(cap<num)
            cap=cap<<1;        
        return cap;
    }
    
    public IntSet(int capacity) {
        this(capacity,DEFAULT_NULL_KEY);
    }

    public IntSet(int capacity, int nullKey) {
        keys=new int[nextPowerOfTwo(capacity)];
        this.nullKey = nullKey;
        clear();
    }
    
    public void clear() {
        Arrays.fill(keys, nullKey );
        size = 0;
    }

    public boolean get(int key) {
        if (key == nullKey)
           return false;
        
        int mask = keys.length -1;
        int hash = key & mask;

        while (true) {
            int mapKey = keys[hash];
            if (mapKey == key) {
                return true;
            } else if (mapKey == nullKey ) {
                return false;
            }
            hash = (hash + 1) & mask;
        }
   }

    public void set(int key) {
        if (key == nullKey) {
            return;
        }

        int mask = keys.length -1;
        int hash = key & mask;

        while (true) {
            int testKey = keys[hash];
            if (testKey == nullKey ) {
                keys[hash] = key;
                size++;
                // if resizing needs to be done, and is not already going on
                if (keys.length <= 2 * size)
                    resize();
                return;
            } else if (key != testKey ) {
                hash = (hash + 1) & mask;
                continue;
            } else {
                //already in
                return;
            }
        }
    }
    
    private synchronized void resize() {
        // no need for resize, or its already going on
        if (keys.length > 2 * size)
            return;
        // create new array
        int newSize = 2 * keys.length;
        int [] newKeys = new int[newSize];
        
        // initialize with null values
        Arrays.fill(newKeys, nullKey );
        
        int mask = newKeys.length - 1;

        int oldsize = size;
        int added = 0;
        
        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            if ( key == nullKey ) {
                continue;
            }

            // reinsert into new
            int hash = key & mask;
             while (true) {
                int testKey = newKeys[hash];
                if (testKey == nullKey ) {
                    newKeys[hash] = key;
                    added++;
                    break;
                } if (key != testKey ) {
                    hash = (hash + 1) & mask;
               } else {
                    break;
               }
            }
        }
        keys = newKeys;
        // the new size, with items added during resize, and the items brought over
        // from old map
        size = size - oldsize + added;
    }
    
    public boolean remove(int key)  {
        if (key == nullKey)
          return false;
        
         int mask = keys.length - 1;
         int hash = key & mask;

         while (true) {
           int mapKey = keys[hash];
           if (mapKey == nullKey) {
                return false;
           } else if (mapKey == key) {
                keys[hash]=nullKey;
                return true;
           }
           hash = (hash + 1) & mask;
        }
     }

    public IntList getValues(IntList store) {
        if(store==null)
            store = new IntList();
        store.clear();
        for(int i=0, mx=keys.length; i<mx; i++) {
            int key = keys[i];
            if(key!=nullKey) {
                store.add(key);
            }
        }
        return store;
    }
}
