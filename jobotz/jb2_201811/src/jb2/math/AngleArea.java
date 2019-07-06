/*
 * The MIT License
 *
 * Copyright 2018 vear.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jb2.math;

/**
 *
 * @author vear
 */
public class AngleArea {
    public float min, max;
    
    public AngleArea() {
    }
        
    public AngleArea boxAngleFromPoint(Vector2f fromPosition, BoundingBox bb) {
        Vector2f otherVect = new Vector2f();
        otherVect.set(bb.center.x-bb.extents.x, bb.center.z-bb.extents.z);
        min = fromPosition.heading(otherVect);
        max = min;
        
        otherVect.set(bb.center.x-bb.extents.x, bb.center.z+bb.extents.z);
        // normalise bearing to same range as previous
        float bearing = normalizeAngle(fromPosition.heading(otherVect));
        // adjust min and max
        minMax(bearing);
        
        otherVect.set(bb.center.x+bb.extents.x, bb.center.z-bb.extents.z);
        bearing = normalizeAngle(fromPosition.heading(otherVect));
        // adjust min and max
        minMax(bearing);

        otherVect.set(bb.center.x+bb.extents.x, bb.center.z+bb.extents.z);
        bearing = normalizeAngle(fromPosition.heading(otherVect));
        // adjust min and max
        minMax(bearing);
        return this;
    }
    
    protected void minMax(float value) {
        if(value<min) {
            min=value;
        }
        if(value>max) {
            max=value;
        }
    }

    protected float normalizeAngle(float second) {
        if(second>min+180) {
            second -= 360;
        }
        if(second<min-180) {
            second += 360;
        }
        return second;
    }
    
    /**
     * Shrink angle range with other range
     * @param other
     * @return false if the two ranges do not have a common part
     */
    public boolean shrink(AngleArea other) {
        float omin = normalizeAngle(other.min);
        if(omin>max)
            return false;
        float omax = normalizeAngle(other.max);
        if(omax<min)
            return false;
        if(omin>min)
            min=omin;
        if(omax<max)
            max=omax;
        return true;
    }
}
