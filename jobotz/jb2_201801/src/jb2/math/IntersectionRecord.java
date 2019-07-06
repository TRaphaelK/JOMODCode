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

/**
 *
 * @author vear
 */
public class IntersectionRecord {
private float[] distances;
    private Vector3f[] points;
    private int length=0;
    
    private static final int INITIAL_LENGTH = 2;
    
    /**
     * Instantiates a new IntersectionRecord with no distances or points assigned.
     *
     */
    public IntersectionRecord() {
        resize(INITIAL_LENGTH);
        length=0;
    }

    /**
     * Instantiates a new IntersectionRecord defining the distances and points. 
     * If the size of the distance and point arrays do not match, an exception
     * is thrown.
     * @param distances the distances of this intersection.
     * @param points the points of this intersection.
     */
    public IntersectionRecord(int capacity) {
        resize(capacity);
    }
    
    public float[] getDistances(int capacity) {
        if(capacity > length)
            resize(capacity);
        return distances;
    }
    
    public Vector3f[] getPoints(int capacity) {
        if(capacity > length)
            resize(capacity);
        return points;
    }

    protected void resize(int capacity) {
        if(points != null && capacity <= points.length) {
            length = capacity;
            return;
        }
            
        float[] newdistances = new float[capacity];
        Vector3f[] newpoints = new Vector3f[capacity];
        if(distances!=null)
            System.arraycopy(distances, 0, newdistances, 0, distances.length);
        int len = length;
        if(points!=null) {
            len = points.length;
            System.arraycopy(points, 0, newpoints, 0, points.length);
        }
        // create new point vectors
        for(int i=len; i < capacity; i++ )
            newpoints[i] = new Vector3f();
        points = newpoints;
        distances = newdistances;        
        length = capacity;
    }
    
    /**
     * Returns the number of intersections that occured.
     * @return the number of intersections that occured.
     */
    public int getQuantity() {
        return length;
    }

    /**
     * Returns an intersection point at a provided index.
     * @param index the index of the point to obtain.
     * @return the point at the index of the array.
     */
    public Vector3f getIntersectionPoint(int index) {
        return points[index];
    }

    /**
     * Returns an intersection distance at a provided index.
     * @param index the index of the distance to obtain.
     * @return the distance at the index of the array.
     */
    public float getIntersectionDistance(int index) {
        return distances[index];
    }

    /**
     * Returns the smallest distance in the distance array.
     * @return the smallest distance in the distance array.
     */
    public float getClosestDistance() {
        float min = Float.MAX_VALUE;
        if (distances != null) {
            for (int i = length; --i >= 0;) {
                float val = distances[i];
                if (val < min) {
                    min = val;
                }
            }
        }
        return min;
    }

    /**
     * Returns the point that has the smallest associated distance value.
     * @return the point that has the smallest associated distance value.
     */
    public int getClosestPoint() {
        float min = Float.MAX_VALUE;
        int point = 0;
        if (distances != null) {
            for (int i = length; --i >= 0;) {
                float val = distances[i];
                if (val < min) {
                    min = val;
                    point = i;
                }
            }
        }
        return point;
    }

    /**
     * Returns the point that has the largest associated distance value.
     * @return the point that has the largest associated distance value.
     */
    public int getFarthestPoint() {
        float max = Float.MIN_VALUE;
        int point = 0;
        if (distances != null) {
            for (int i = length; --i >= 0;) {
                float val = distances[i];
                if (val > max) {
                    max = val;
                    point = i;
                }
            }
        }
        return point;
    }
}
