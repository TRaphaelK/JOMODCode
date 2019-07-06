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
package jb2.math;

import java.util.Arrays;
import jb2.util.JbException;

/**
 *
 * @author vear
 */
public class Tensor {
   
    protected float[] values;
    protected int size;
    protected int components;
    
    public Tensor() {
        this(10, 3);
    }
    
    public Tensor( int initialSize, int components ) {
        this.components=components;
        values=new float[initialSize*this.components];
        size = 0;
    }

    public Tensor(float[] array, int components) {
        setArray(array, components);
    }

    public int size() {
        return size;
    }

    public int components() {
        return components;
    }

    public float get(int index, int component) {
        return values[index*components+component];
    }

    public void add(float value, int component) {
        ensureCapacity(size+1);
        values[size*components+component] = value;
        size++;
    }
    
    public void add(float[] values) {
        add(values, this.components);
    }
    
    public int add(Vector3f value) {
        ensureCapacity(size+1);
        int index = size;
        if(components>0) {
            values[index*components] = value.x;
        }
        if(components>1) {
            values[index*components+1] = value.y;
        }
        if(components>2) {
            values[index*components+2] = value.z;
        }
        size++;
        return index;
    }
    
    public int add(float x, float y, float z) {
        ensureCapacity(size+1);
        int index = size;
        if(components>0) {
            values[index*components] = x;
        }
        if(components>1) {
            values[index*components+1] = y;
        }
        if(components>2) {
            values[index*components+2] = z;
        }
        size++;
        return index;
    }
    
    public void set(int destIndex, Tensor other, int sourceIndex) {
        int mincomponents = this.components<other.components?components:other.components;
        ensureCapacity(destIndex+1);
        for(int i=0; i<mincomponents; i++ ) {
            values[destIndex*components+i] = other.values[sourceIndex*other.components+i];
        }
    }

    public void add(float[] values, int components) {
        if(components>this.components)
            throw new JbException("Trying to add items with more elements than possible");
        
        int elements=(values.length/components);
        ensureCapacity((size+elements)+1);
        
        for(int i=0; i< elements; i++) {
            for(int j=0; j<components; j++) {
                values[(size+i)*this.components+j] = values[i*components+j];
            }
        }
        size+=elements;
    }

    public void clear() {
        if(size>0)
            Arrays.fill(values, 0, size*components, 0);
        size = 0;
    }
    
    public int indexOf(float value, int component) {
        int index = -1;
        for(int i=component; i<size&&index==-1; i+=this.components) {
            if(values[i]==value) {
                index=i;
                break;
            }
        }
        return index;
    }
    
    public void removeElementAt(int index) {
        if(index!=size-1) {
            // copy elements
            System.arraycopy(values, index*components+1, values, index*components, (size-index-1)*components);
        }
        size--;
        values[size]=0;
    }
    
    public void ensureCapacity(int capacity) {
        int cap = values.length/components;
        if(capacity>cap) {
            if(capacity<cap*3/2)
                capacity=cap*3/2;
            // create new array to hold data, as copy of values
            values = Arrays.copyOf(values, capacity*components);
        }
    }
    
    public void setComponent(int index, int component, float value) {
        if(index>=size) {
            ensureCapacity(index+1);
            size=index+1;
        }
        values[index*components+component] = value;
    }
    
    public int set(int index, float value1, float value2) {
        if(components<2) {
            throw new JbException("Trying to set 2 values on a tensor with only 1 component");
        }
        if(index>=size) {
            ensureCapacity(index+1);
            size=index+1;
        }
        values[index*components] = value1;
        values[index*components+1] = value2;
        return index;
    }

    public void addAll(Tensor other) {
        if(other.size>0) {            
            ensureCapacity(size+other.size);
            if(components==other.components) {
                System.arraycopy(other.values, 0, values, size*components, other.size*other.components);
            } else {
                int mincomp=components<other.components?components:other.components;
                
                // add one by one                
                for(int i=0; i<other.size; i++) {
                    for(int j=0; j<mincomp; j++) {
                        values[(size+i)*components+j]=other.values[i*other.components+j];
                    }
                }
            }
            size+=other.size;
        }
    }
    
    public boolean contains(float value, int element) {
        return indexOf(value, element) >= 0;
    }

    public void set(float[] array) {
        if(values.length < array.length)
            values = Arrays.copyOf(array, array.length);
        size = values.length/components;
    }
    
    public float[] getArray() {
        return values;
    }
    
    public void setArray(float[] array) {
        values = array;
        size = values.length/components;
    }
    
    public void setArray(float[] array, int components) {
        values = array;
        size = values.length/components;
        this.components = components;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof Tensor))
            return false;
        Tensor otherList = (Tensor) other;
        if(size!=otherList.size) 
            return false;
        if(components!=otherList.components) 
            return false;
        for(int i=0; i<size; i++)
            if(values[i]!=otherList.values[i])
                return false;
        return true;
    }
    
    public boolean containsAll(Tensor other) {
        if(other == null && size!=0)
            return false;
        if(size>other.size)
            return false;
        if(components!=other.components) 
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

    public void setRandomValues(Tensor minMaxValues) {
        ensureCapacity(minMaxValues.size);
        size = minMaxValues.size;
        for(int index=0; index<size; index++) {
            float min = minMaxValues.get(index, 0);
            float max = minMaxValues.get(index, 1);
            float value = (float) (min + FastMath.rand.nextDouble()* (max - min));
            values[index*components+0] = value;
        }
    }
}
