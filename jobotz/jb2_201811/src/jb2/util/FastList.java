/*
 * 
 * Vear 2017  * 
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
import java.util.Comparator;
import java.util.function.Consumer;

/**
 *
 * @author vear (Arpad Vekas)
 * @param <E>
 */
public class FastList<E extends Object> {

    private E[] values;
    public int size;
    
    public FastList(E[] elements) {
        values = elements;
        size=values.length;
    }

    public FastList() {
        this(8);
    }

    public FastList(int initialSize) {
        values=(E[]) new Object[initialSize];
        size = 0;
    }

    public int size() {
        return size;
    }
    
    public E get(int index) {
        return (E) values[index];
    }
    
    public void add(E element) {
        ensureCapacity(size+1);
        values[size] = element;
        size++;
    }
    
    public E get() {
        return values[size-1];
    }
    
    public E remove() {
        size--;
        E val = values[size];
        values[size]=null;
        return val;
    }
    
    public E last() {
        return size>0? values[size-1]:null;
    }
    
    public void clear() {
        if(size>0)
            Arrays.fill(values, 0, size, null);
        size = 0;
    }
    
    public int indexOf(E element) {
        int index = -1;
        for(int i=0; i<size&&index==-1; i++) {
            if(values[i]==element) {
                index=i;
            }
        }
        return index;
    }

    public void remove(E element) {
        int index = indexOf(element);
        if(index!=-1) {
            remove(index);
        }
    }
    
    public E remove(int index) {
        E ret = values[index];
        if(index!=size-1) {
            // copy elements
            System.arraycopy(values, index+1, values, index, size-index-1);
        }
        size--;
        values[size]=null;
        return ret;
    }

    public void ensureCapacity(int capacity) {
        if(capacity>values.length) {
            // create new array to hold data, as copy of values
            if(capacity<values.length*3/2)
                capacity=values.length*3/2;
            values = Arrays.copyOf(values, capacity);
        }
    }
    
    public void set(int index, E element) {
        if(index>=size) {
            ensureCapacity(index+1);
            size=index+1;
        }
        values[index] = element;
    }

    public void addAll(FastList<E> other) {
        if(other.size>0) {
            ensureCapacity(size+other.size);
            System.arraycopy(other.values, 0, values, size, other.size);
            size+=other.size;
        }
    }
    
    public boolean contains(E element) {
        return indexOf(element) >= 0;
    }
    
    public E[] getArray() {
        return (E[]) values;
    }
    
    public void sort(Comparator c) {
        Arrays.sort(values, 0, size, c);
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public void add(int index, E element) {
        if(index>=size) {
            ensureCapacity(index+1);
            values[index]=element;
            size=index+1;
        } else {
            ensureCapacity(size+1);
            // copy elements above index one up
            System.arraycopy(values, index, values, index+1, size-index);
            values[index]=element;
            size=size+1;
        }

    }
    
    public E[] toArray(E[] store) {
        if(store==null)
            store=(E[]) new Object[size];
        System.arraycopy(values, 0, store, 0, size);
        return store;
    }

    public void addAllOrNull(E[] array) {
        if(array==null)
            return;
        ensureCapacity(size+array.length);
        System.arraycopy(array, 0, values, size, array.length);
        size+=array.length;
    }
    
    public void addAll(E[] array) {
        if(array==null)
            return;
        for(int i=0; i<array.length; i++) {
            E item = array[i];
            if(item==null)
                continue;
            add(item);
        }
    }

    public void forEachEntry(Consumer<E> f) {
        for(int i=0; i<size; i++) {
            E entry = values[i];
            if(entry==null)
                continue;
            f.accept(entry);
        }
    }
}