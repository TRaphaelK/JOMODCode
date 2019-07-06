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

import static jb2.math.FastMath.RAD_TO_DEG;
import static jb2.math.FastMath.atan2;

/**
 *
 * @author vear
 */
public class Vector2f {
    
    public float x;
    public float y;
    
    public Vector2f(){
        
    }

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f set(float X, float Y) {
        this.x = X;
        this.y = Y;
        return this;
    }

    public Vector2f normalizeLocal() {    
        float length = x * x + y * y;
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            set(x * length, y * length);
        }
        return this;
    }
    
    /**
     * Angle index (0 to 8) of this unit vector
     * 0 1 2 ( z < -0,3)
     * 3   4 ( z > -0,3 && z < 0.3 )
     * 5 6 7 ( z > 0.3)
     * @return 
     */
    public int angleIndex() {
        
        int index=0;
        // ignore y
        // check which side of z
        float onethird = 1f/3f;
        if(y>=-onethird && y<onethird) {
            // center
            index=3;
            // depending if x is positive or negative its 3 or 4
            if(x>0) {
                return index+1;
            }
            return index;
        }
        // if bottom part, set index
        if (y >= onethird) {
            index = 5;
        }
        // depending in which third x is
        if (x < -onethird) {
            return index;
        }
        if (x < onethird) {
            return index + 1;
        }
        return index + 2;
    }

    // angle between 2 normalized vectors
    public float angleBetween(Vector2f otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }
    
    public float dot(Vector2f vec) {
        return x * vec.x + y * vec.y;
    }
    
    public Vector2f subtractLocal(Vector2f other) {
        x -= other.x;
        y -= other.y;
        return this;
    }
    
    public Vector2f fromHeading(float angle) {
        x = FastMath.sin(angle);
        y = FastMath.cos(angle);
        return this;
    }

    public float heading(Vector2f to) {
        // if (a1 = b1 and a2 = b2) throw an error 
        float theta = atan2(to.x - this.x, to.y - this.y);
        return theta;
    }
        
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
