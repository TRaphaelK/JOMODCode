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

/*
 * Copyright (c) 2008 VL Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'VL Engine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import java.util.Arrays;

/**
 * A HashMap for int,int values
 * @author vear (Arpad Vekas)
 */
public class IntIntMap {
    private static final short DEFAULT_NULL_KEY = 0;
    private static final short DEFAULT_NULL_VALUE = 0;
    
    int nullKey = 0;
    int nullValue = 0;
    
    // the number of entryies in the map
    int size = 0;
    
    // the array holding the old values
    int[] oldKey;
    int[] oldValue;
    
    // the array holding the new values
    int[] newKey;
    int[] newValue;
    
    public IntIntMap() {
        this(16,DEFAULT_NULL_KEY,DEFAULT_NULL_VALUE);
    }
    
    private static int nextPowerOfTwo(int num) {
 	int cap=16;
        while(cap<num)
            cap=cap<<1;        
        return cap;
    }
    
    public IntIntMap(int capacity) {
        this(capacity,DEFAULT_NULL_KEY,DEFAULT_NULL_VALUE);
    }

    public IntIntMap(int capacity, int nullKey, int nullValue) {
        newKey=new int[nextPowerOfTwo(capacity)];
        newValue = new int[newKey.length];
        this.nullKey = nullKey;
        this.nullValue = nullValue;
        clear();
    }
    
    public void clear() {
        oldKey=newKey;
        oldValue=newValue;
        Arrays.fill(newKey, nullKey );
        Arrays.fill(newValue, nullValue);
        size = 0;
    }

    private int get(int key, int[] keys, int[] values) {
        if (key == nullKey)
           return nullValue;
        
        int mask = keys.length -1;
        int hash = key & mask;

        while (true) {
            int mapKey = keys[hash];
            if (mapKey == key) {
                int value = values[hash];
                if ( value != nullValue || keys == newKey)
                    return value;
                // if key found with null value in old map, check new map
                return get(key, newKey, newValue);
            } else if (mapKey == nullKey ) {
                if (keys == newKey)
                    return nullValue;
                // not found in old array,
                // check entry in new array
                return get(key, newKey, newValue);
            }
            hash = (hash + 1) & mask;
        }
   }

    public int get(int key) {
        // try to get from the old entryes first
        return get(key, oldKey, oldValue);
    }

    public void put(int key, int value) {
        if (key == nullKey) {
            return;
        }

        int mask = newKey.length -1;
        int hash = key & mask;

        while (true) {

            int testKey = newKey[hash];

            if (testKey == nullKey ) {
                newKey[hash] = key;
                newValue[hash] = value;
                size++;
                // if resizing needs to be done, and is not already going on
                if (newKey.length <= 2 * size && newKey == oldKey)
                    resize();
                return;
            } else if (key != testKey ) {
                hash = (hash + 1) & mask;
                continue;
            } else {
                newKey[hash] = key;
                newValue[hash] = value;
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
        int[] newValues = new int[newSize];
        
        // initialize with null values
        Arrays.fill(newKeys, nullKey );
        Arrays.fill(newValues, nullValue );
        
        // switch new to newly created, so others can write it
        newKey = newKeys;
        newValue = newValues;

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
                int value = oldValue[i];
                
                if( value == nullValue) {
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
                        newValue[hash] = value;
                        
                        oldValue[i] = nullValue;
                        added++;
                        break;
                    } if (key != testKey ) {
                        hash = (hash + 1) & mask;
                   } else {
                        // put occured while we copy
                        // discard old value
                        oldValue[i] = nullValue;
                        break;
                   }
                }
            }
            if(!multithreading)
                found=false;
        }
        // resizing finished, the old is not needed any more
        oldKey = newKey;
        oldValue = newValue;
        // the new size, with items added during resize, and the items brought over
        // from old map
        size = size - oldsize + added;
    }
    
    public int remove(int key) {
        return remove(key, oldKey, oldValue);
    }

    private int remove(int key, int[] keys, int[] values)  {
        if (key == nullKey)
          return nullValue;
        
         int mask = keys.length - 1;
         int hash = key & mask;

         while (true) {

           int mapKey = keys[hash];

           if (mapKey == nullKey) {
               if (keys == newKey)
                    return nullValue;
                // not found in old array,
                // check entry in new array
                return remove(key, newKey, newValue);
           } else if (mapKey == key) {
                int oldVal = values[hash];
                values[hash] = nullValue;
                if(keys != newKey) {
                    int newVal = remove(key, newKey, newValue);
                    return newVal != nullValue ? newVal : oldVal;
                } else {
                    return oldVal;
                }
           }
           hash = (hash + 1) & mask;
        }
     }

    public IntList getValues(IntList store) {
        if(store==null)
            store = new IntList();
        store.clear();
        for(int i=0, mx=oldKey.length; i<mx; i++) {
            int key = oldKey[i];
            if(key!=nullKey) {
                if(oldValue[i]!=nullValue)
                    store.add(oldValue[i]);
                else if(oldKey != newKey) {
                    int newVal = this.get(key, newKey, newValue);
                    if(newVal != nullValue ) {
                        store.add(newVal);
                    }
                }
            }
        }
        return store;
    }
    
    public IntList getKeys(IntList store) {
        if(store==null)
            store = new IntList();
        store.clear();
        for(int i=0, mx=oldKey.length; i<mx; i++) {
            int key = oldKey[i];
            if(key!=nullKey) {
                if(oldValue[i]!=nullValue)
                    store.add(key);
                else if(oldKey != newKey) {
                    int newVal = this.get(key, newKey, newValue);
                    if(newVal != nullValue ) {
                        store.add(key);
                    }
                }
            }
        }
        return store;
    }
    
    public void getKeysAndValues(IntList keys, IntList values) {
        keys.size=0;
        values.size=0;
        for(int i=0, mx=oldKey.length; i<mx; i++) {
            int key = oldKey[i];
            if(key!=nullKey) {
                if(oldValue[i]!=nullValue) {
                    keys.add(key);
                    values.add(oldValue[i]);
                } else if(oldKey != newKey) {
                    int newVal = this.get(key, newKey, newValue);
                    if(newVal != nullValue ) {
                        keys.add(key);
                        values.add(newVal);
                    }
                }
            }
        }
    }
    
    public int getKeyByIndex(int index) {
        int key = oldKey[index];
        if(key==nullKey)
            return nullKey;
        int value = oldValue[index];
        if(value==nullValue) {
            value=this.get(key, newKey, newValue);
            if(value==nullValue)
                return nullKey;
        }
        return key;
    }
    
    public int getValueByIndex(int index) {
        int key = oldKey[index];
        if(key==nullKey)
            return nullValue;
        int value = oldValue[index];
        if(value==nullValue) {
            value=this.get(key, newKey, newValue);
        }
        return value;
    }
    
    public int getRawSize() {
        return oldKey.length;
    }

    public int size() {
        return size;
    }
}
