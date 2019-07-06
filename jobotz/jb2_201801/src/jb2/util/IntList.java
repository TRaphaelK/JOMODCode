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
 *
 * @author vear
 */
public class IntList {
    protected int[] values;
    public int size;
    
    public IntList() {
        this(10);
    }
    
    public IntList( int initialSize ) {
        values=new int[initialSize];
        size = 0;
    }

    public IntList(int[] array) {
        setArray(array);
    }

    public int size() {
        return size;
    }
    
    public int get(int index) {
        return values[index];
    }

    public void add(int element) {
        ensureCapacity(size+1);
        values[size] = element;
        size++;
    }
    
    public void clear() {
        if(size>0)
            Arrays.fill(values, 0, size, 0);
        size = 0;
    }
    
    public int indexOf(int element) {
        int index = -1;
        for(int i=0; i<size&&index==-1; i++) {
            if(values[i]==element) {
                index=i;
            }
        }
        return index;
    }

    public void removeElement(int element) {
        int index = indexOf(element);
        if(index!=-1) {
            removeElementAt(index);
        }
    }
    
    public void removeElementAt(int index) {
        if(index!=size-1) {
            // copy elements
            System.arraycopy(values, index+1, values, index, size-index-1);
        }
        size--;
        values[size]=0;
    }
    
    public void ensureCapacity(int capacity) {
        if(capacity>values.length) {
            if(capacity<values.length*3/2)
                capacity=values.length*3/2;
            // create new array to hold data, as copy of values
            values = Arrays.copyOf(values, capacity);
        }
    }
    
    public void set(int index, int element) {
        if(index>=size) {
            ensureCapacity(index+1);
            size=index+1;
        }
        values[index] = element;
    }

    public void addAll(IntList other) {
        if(other.size>0) {
            ensureCapacity(size+other.size);
            System.arraycopy(other.values, 0, values, size, other.size);
            size+=other.size;
        }
    }
    
    public boolean contains(int element) {
        return indexOf(element) >= 0;
    }
    
    public int[] getArray() {
        return values;
    }
    
    public void setArray(int[] array) {
        values = array;
        size = values.length;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof IntList))
            return false;
        IntList otherList = (IntList) other;
        if(size!=otherList.size) 
            return false;
        for(int i=0; i<size; i++)
            if(values[i]!=otherList.values[i])
                return false;
        return true;
    }
    
    public boolean containsAll(IntList other) {
        if(other == null && size!=0)
            return false;
        if(size>other.size)
            return false;
        for(int i=0; i<size; i++) {
            boolean found = false;
            for(int j=0; j<other.size && !found; j++) {
                if(values[i] == other.values[j]) {
                    found = true;
                }
            }
            if(!found)
                return false;
        }
        return true;
    }
}
