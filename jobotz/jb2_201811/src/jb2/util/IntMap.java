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
import java.util.function.Consumer;

/**
 * A HashMap for int,Object values
 * @author vear (Arpad Vekas)
 */
public class IntMap<T> {
    private static final short DEFAULT_NULL_KEY = 0;
    int nullKey = 0;
    
    // the number of entryies in the map
    int size = 0;
    
    // the array holding the old values
    int[] oldKey;
    T[] oldValue;
    
    // the array holding the new values
    int[] newKey;
    T[] newValue;
    
    public IntMap() {
        this(16,DEFAULT_NULL_KEY);
    }
    
    private static int nextPowerOfTwo(int num) {
 	int cap=16;
        while(cap<num)
            cap=cap<<1;  
        return cap;
    }
    
    public IntMap(int capacity) {
        this(capacity,DEFAULT_NULL_KEY);
    }

    public IntMap(int capacity, int nullKey) {
        newKey=new int[nextPowerOfTwo(capacity)];
        newValue = (T[]) new Object[newKey.length];
        this.nullKey = nullKey;
        clear();
    }
    
    public void clear() {
        oldKey=newKey;
        oldValue=newValue;
        Arrays.fill(newKey, nullKey );
        Arrays.fill(newValue, (Object)null );
        size = 0;
    }

    private T get(int key, int[] keys, Object[] values) {
        if (key == nullKey)
           return null;
        
        int mask = keys.length -1;
        int hash = key & mask;

        while (true) {
            int mapKey = keys[hash];
            if (mapKey == key) {
                Object value = values[hash];
                if ( value != null || keys == newKey)
                    return (T) value;
                // if key found with null value in old map, check new map
                return get(key, newKey, newValue);
            } else if (mapKey == nullKey ) {
                if (keys == newKey)
                    return null;
                // not found in old array,
                // check entry in new array
                return get(key, newKey, newValue);
            }
            hash = (hash + 1) & mask;
        }
   }

    public T get(int key) {
        // try to get from the old entryes first
        return get(key, oldKey, oldValue);
    }

    public void put(int key, T value) {
        if (key == nullKey) {
            return;
        }

        int mask = newKey.length -1;
        int hash = key & mask;

        while (true) {

            int testKey = newKey[hash];

            if (testKey == nullKey ) {
                newKey[hash] = key;
                newValue[hash] = (T) value;
                size++;
                // if resizing needs to be done, and is not already goig on
                if (newKey.length <= 2 * size && newKey == oldKey)
                    resize();
                return;
            } else if (key != testKey ) {
                hash = (hash + 1) & mask;
                continue;
            } else {
                newKey[hash] = key;
                newValue[hash] = (T) value;
                return;
            }
        }
    }
    
    private synchronized void resize() {
        // no need for resize, or its already going on
        if (newKey.length > 2 * size || newKey != oldKey)
            return;
        // create new array
        int newSize = 2 * newKey.length;
        int [] newKeys = new int[newSize];
        Object[] newValues = new Object[newSize];
        
        // initialize with null values
        Arrays.fill(newKeys, nullKey );
        Arrays.fill(newValues, null );
        
        // switch new to newly created, so others can write it
        newKey = newKeys;
        newValue = (T[]) newValues;

        int mask = newKey.length - 1;
        
        boolean found = true;

        int oldsize = size;
        int added = 0;
        
        boolean multithreading = LocalContext.isUseMultithreading();
        
        while(found) {
            found = false;
            for (int i = 0; i < oldKey.length; i++) {

                int key = oldKey[i];
                
                if ( key == nullKey ) {
                    continue;
                }
                Object value = oldValue[i];
                
                if( value == null) {
                    // decrement size when encountered a removed entry
                    continue;
                }

                // we found something to work on, recheck needed
                found=true;
                // reinsert into new
                int hash = key & mask;

                 while (true) {
                    int testKey = newKey[hash];
                    
                    if (testKey == nullKey ) {
                        newKey[hash] = key;
                        newValue[hash] = (T) value;
                        
                        oldValue[i] = null;
                        added++;
                        break;
                    } if (key != testKey ) {
                        hash = (hash + 1) & mask;
                   } else {
                        // put occured while we copy
                        // discard old value
                        oldValue[i] = null;
                        break;
                   }
                }
            }
            if(!multithreading)
                found=false;
        }
        // resizing finished, the old is not needed any more
        oldKey = newKey;
        // should be made thread safe
        oldValue = newValue;
        // the new size, with items added during resize, and the items brought over
        // from old map
        size = size - oldsize + added;
    }
    
    public Object remove(int key) {
        return remove(key, oldKey, oldValue);
    }

    private Object remove(int key, int[] keys, Object[] values)  {
        if (key == nullKey)
          return null;
        
         int mask = keys.length - 1;
         int hash = key & mask;

         while (true) {

           int mapKey = keys[hash];

           if (mapKey == nullKey) {
               if (keys == newKey)
                    return null;
                // not found in old array,
                // check entry in new array
                return remove(key, newKey, newValue);
           } else if (mapKey == key) {
                Object oldVal = values[hash];
                values[hash] = null;
                if(keys != newKey) {
                    Object newVal = remove(key, newKey, newValue);
                    return newVal != null ? newVal : oldVal;
                } else {
                    return oldVal;
                }
           }
           hash = (hash + 1) & mask;
        }
     }

    public FastList getValues(FastList store) {
        if(store==null)
            store = new FastList(size);
        store.clear();
        for(int i=0, mx=oldKey.length; i<mx; i++) {
            int key = oldKey[i];
            if(key!=nullKey) {
                if(oldValue[i]!=null)
                    store.add(oldValue[i]);
                else if(oldKey != newKey) {
                    Object newVal = this.get(key, newKey, newValue);
                    if(newVal != null ) {
                        store.add(newVal);
                    }
                }
            }
        }
        return store;
    }
    
    public void forEachValue(Consumer<T> f) {
        for(int i=0, mx=oldKey.length; i<mx; i++) {
            int key = oldKey[i];
            if(key!=nullKey) {
                if(oldValue[i]!=null)
                    f.accept(oldValue[i]);
                else if(oldKey != newKey) {
                    T newVal = this.get(key, newKey, newValue);
                    if(newVal != null ) {
                        f.accept(newVal);
                    }
                }
            }
        }
    }
}
