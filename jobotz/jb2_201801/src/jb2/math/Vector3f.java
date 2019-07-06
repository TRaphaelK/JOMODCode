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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import jb2.util.Context;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public final class Vector3f {
    
    public final static Vector3f ZERO = new Vector3f(0, 0, 0);

    public final static Vector3f UNIT_X = new Vector3f(1, 0, 0);
    public final static Vector3f UNIT_Y = new Vector3f(0, 1, 0);
    public final static Vector3f UNIT_Z = new Vector3f(0, 0, 1);
    public final static Vector3f UNIT_XYZ = new Vector3f(1, 1, 1);
    
    public final static Vector3f UNIT_MINNUS_X = new Vector3f( -1.0f, 0.0f, 0.0f );
    public final static Vector3f UNIT_MINNUS_Z = new Vector3f( 0.0f, 0f, -1.0f );
    
    public final static Vector3f MAX_NEGATIVE = new Vector3f( -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE );
    public final static Vector3f MAX_POSITIVE = new Vector3f( Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE );
    
    public float x;
    public float y;
    public float z;
    
    // bucket storage (reset each frame)
    // with array backing

    // adapted JOOML implementation
    // https://github.com/JOML-CI/JOML/blob/master/src/org/joml/Vector3f.java
    // adapted from jmonkeyengine
    // https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/Vector3f.java

    public Vector3f() {
        
    }
    
    public Vector3f(float setx, float sety, float setz) {
        x=setx;
        y=sety;
        z=setz;
    }

    public Vector3f(Vector3f other) {
        set(other);
    }
    public Vector3f set(Vector3f v) {
        x=v.x;
        y=v.y;
        z=v.z;
        return this;
    }
    
    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    public static final Vector3f subtract(Vector3f v1, Vector3f v2, Vector3f result) {
        result.x = v1.x - v2.x;
        result.y = v1.y - v2.y;
        result.z = v1.z - v2.z;
        return result;
    }
    
    public static final void subtract(Vector3f v1, Vector3f v2, float[] resultArray, int resultIndex) {
        resultArray[resultIndex] = v1.x - v2.x;
        resultArray[resultIndex+1] = v1.y - v2.y;
        resultArray[resultIndex+2] = v1.z - v2.z;
    }
    
    public Vector3f subtractLocal(Vector3f v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }
    
    
    public Vector3f subtractLocal(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }
    
    public Vector3f addLocal(Vector3f v) {
        x += v.x;
        y += v.y;
        z += v.z;
        return this;
    }
    
    public Vector3f add(Vector3f v, Vector3f result) {
        result.set(x + v.x, y + v.y, z + v.z);
        return result;
    }
    
    public Vector3f addLocal(float x, float y, float z) {
        set(x + x, y + y, z + z);
        return this;
    }
    
    public Vector3f multLocal(Vector3f v) {
        set(x * v.x, y * v.y, z * v.z);
        return this;
    }

    public Vector3f multLocal(float scalar) {
        set(x * scalar, y * scalar, z * scalar);
        return this;
    }

    public Vector3f multLocal(float x, float y, float z) {
        set(x * x, y * y, z * z);
        return this;
    }
    
    public Vector3f mult(float scalar, Vector3f result) {        
        return result.set(x * scalar, y * scalar, z * scalar);
    }
    
    public Vector3f mult(Vector3f v, Vector3f result) {
        result.set(x * v.x, y * v.y, z * v.z);
        return result;
    }
    
    public Vector3f divideLocal(Vector3f v) {
        set(x / v.x, y / v.y, z / v.z);
        return this;
    }

    public Vector3f divide(Vector3f v, Vector3f result) {
        result.set(x / v.x, y / v.y, z / v.z);
        return result;
    }
    
    public Vector3f divideLocal(float scalar) {
        scalar = 1f/scalar;
        set(x * scalar, y * scalar, z * scalar);
        return this;
    }
    
    public Vector3f normalizeLocal() {    
        float length = x * x + y * y + z * z;
        if (length != 1f && length != 0f){
            length = 1.0f / FastMath.sqrt(length);
            set(x * length, y * length, z * length);
        }
        return this;
    }
    
    public Vector3f multProject(Matrix4f mat, Vector3f dest) {        
        float invW = 1.0f / (mat.m03() * x + mat.m13() * y + mat.m23() * z + mat.m33());
        dest.set((mat.m00() * x + mat.m10() * y + mat.m20() * z + mat.m30()) * invW,
                 (mat.m01() * x + mat.m11() * y + mat.m21() * z + mat.m31()) * invW,
                 (mat.m02() * x + mat.m12() * y + mat.m22() * z + mat.m32()) * invW);
        return dest;
    }
    
    public Vector3f multProject(Matrix4f mat) {
        return multProject(mat, this);
    }
    
    public Vector3f mult(Matrix3f mat, Vector3f dest) {        
        dest.set(mat.m00() * x + mat.m10() * y + mat.m20() * z,
                 mat.m01() * x + mat.m11() * y + mat.m21() * z,
                 mat.m02() * x + mat.m12() * y + mat.m22() * z);
        return dest;
    }
    
    public Vector3f multLocal(Matrix3f mat) {
        return mult(mat, this);
    }
    
    public Vector3f scaleAddLocal(float scalar, Vector3f add) {        
        set( x * scalar + add.x, y * scalar + add.y, z * scalar + add.z);
        return this;
    }
    
    public float dot(Vector3f vec) {
        return x * vec.x + y * vec.y + z * vec.z;
    }
    
    public Vector3f crossLocal(float otherX, float otherY, float otherZ) {
        float tempx = ( y * otherZ ) - ( z * otherY );
        float tempy = ( z * otherX ) - ( x * otherZ );

        set((x * otherY) - (y * otherX), 
             tempx,
             tempy);
        return this;
    }
    
    public Vector3f crossLocal(Vector3f v) {
        return crossLocal(v.x, v.y, v.z);
    }
    
    public float length() {
        return FastMath.sqrt(lengthSquared());
    }
    
    public float lengthSquared() {
        return x * x + y * y + z * z;
    }
    
    public Vector3f projectLocal(Vector3f other){
        
        float n = this.dot(other); // A . B
        float d = other.lengthSquared(); // |B|^2
        float npd = n/d;
        return set(other).multLocal(n/d);
    }
    
    public float distanceSquared(Vector3f v) {
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        return (float) (dx * dx + dy * dy + dz * dz);
    }
    
    public float distance(Vector3f v) {
        return FastMath.sqrt(distanceSquared(v));
    }
    
    public float distance(Vector3f v, boolean twoD) {
        if(twoD) {
            return distance2d(v);
        }
        return distance(v);
    }
    
    public float manhattanDistance(Vector3f v, boolean twoD) {
        if(twoD) {
            return manhattanDistance2d(v);
        }
        return manhattanDistance(v);
    }
    
    public float manhattanDistance(Vector3f v) {
        return FastMath.abs(v.x - x) + FastMath.abs(v.y -y) + FastMath.abs(v.z - z);
    }
    
    public float manhattanDistance2d(Vector3f v) {
        return FastMath.abs(v.x - x) + FastMath.abs(v.z - z);
    }
    
    public float distance2d(Vector3f v) {
        double dx = x - v.x;
        double dz = z - v.z;
        return FastMath.sqrt((float) (dx * dx + dz * dz));
    }
    
    public Vector3f negateLocal() {
        set( -x, -y, -z);
        return this;
    }
    
    public Vector3f maxLocal(Vector3f other){    
        if(other.x > x)
            x = other.x;
        if(other.y > y)
            y = other.y;
        if(other.z > z)
            z = other.z;        
        return this;
    }
    
    public Vector3f minLocal(Vector3f other){
        if(other.x < x)
            x = other.x;
        if(other.y < y)
            y = other.y;
        if(other.z < z)
            z = other.z;
        return this;
    }
    
    public Vector3f minLocal(BoundingBox other){
        float otherx = other.center.x-other.extents.x; float othery = other.center.y-other.extents.y; float otherz = other.center.z-other.extents.z;
        
        if(otherx < x)
            x = otherx;
        if(othery < y)
            y = othery;
        if(otherz < z)
            z = otherz;
        return this;
    }
    
    public Vector3f maxLocal(BoundingBox other){
        float otherx = other.center.x+other.extents.x; float othery = other.center.y+other.extents.y; float otherz = other.center.z+other.extents.z;
        
        if(otherx > x)
            x = otherx;
        if(othery > y)
            y = othery;
        if(otherz > z)
            z = otherz;
        return this;
    }
    
    /**
     * Sets this vector to the interpolation by changeAmnt from this to the finalVec
     * this=(1-changeAmnt)*this + changeAmnt * finalVec
     * @param other The final vector to interpolate towards
     * @param scale An amount between 0.0 - 1.0 representing a precentage
     *  change from this towards finalVec
     */
    public Vector3f interpolateLocal(float scale, Vector3f other) {
        if (scale <= 0f) {
            // 
            return this;
        }
        if (scale >= 1f) {
            set(other);
            return this;
        }

        float otherx = other.x; float othery = other.y; float otherz = other.z;
        set(x*(1-scale) + otherx*scale,
            y*(1-scale) + othery*scale,
            z*(1-scale) + otherz*scale
                );
        
        return this;
    }
    
    /**
     * <code>angleBetween</code> returns (in radians) the angle between two vectors.
     * It is assumed that both this vector and the given vector are unit vectors (iow, normalized).
     * 
     * @param otherVector a unit vector to find the angle against
     * @return the angle in radians.
     */
    public float angleBetween(Vector3f otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }
    
    public float slopeOfY(Vector3f other) {
        Context ctx = LocalContext.getContext();
        Vector3f tmpVec1 = ctx.Vector3f_tmpVec1;
        tmpVec1.set(this).subtractLocal(other);
        float angle = (y - other.y)/tmpVec1.length();
        return FastMath.atan(angle);
    }

    /**
     * Sets this vector to the interpolation by changeAmnt from beginVec to finalVec
     * this=(1-changeAmnt)*beginVec + changeAmnt * finalVec
     * @param beginVec the beging vector (changeAmnt=0)
     * @param finalVec The final vector to interpolate towards
     * @param changeAmnt An amount between 0.0 - 1.0 representing a precentage
     *  change from beginVec towards finalVec
     */
    public Vector3f interpolateLocal(float changeAmnt, Vector3f beginVec, Vector3f finalVec) {
        x = (1-changeAmnt)*beginVec.x + changeAmnt*finalVec.x;
        y = (1-changeAmnt)*beginVec.y + changeAmnt*finalVec.y;
        z = (1-changeAmnt)*beginVec.z + changeAmnt*finalVec.z;
        return this;
    }
    
    public static final Vector3f computeNormal(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f result){
        // a1 = v1 - v2
        float a1x = v1.x - v2.x;
        float a1y = v1.x - v2.x;
        float a1z = v1.x - v2.x;
        
        // a2 = v3 - v2
        float a2x = v3.x - v2.x;
        float a2y = v3.x - v2.x;
        float a2z = v3.x - v2.x;
        
        // R = a2 X a1
        float x = ( a2x * a1y ) - ( a2y * a1x );
        float y = ( a2y * a1z ) - ( a2z * a1y );
        float z = ( a2z * a1x ) - ( a2x * a1z );
                
        // normalize R
        float length = x * x + y * y + z * z;
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            x = x * length; y = y * length; z = z * length;
        }
        
        result.set(x, y, z);
        return result;
    }
    
    /**
     * <code>hashCode</code> returns a unique code for this vector object based
     * on it's values. If two vectors are logically equivalent, they will return
     * the same hash code value.
     * @return the hash code value of this vector.
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash += 37 * hash + Float.floatToIntBits(x);
        hash += 37 * hash + Float.floatToIntBits(y);
        hash += 37 * hash + Float.floatToIntBits(z);
        return hash;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("("); 
        sb.append(x); sb.append(","); 
        sb.append(y); sb.append(","); 
        sb.append(z); 
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector3f)) { return false; }

        if (this == o) { return true; }

        Vector3f comp = (Vector3f) o;
        if (Float.compare(x,comp.x) != 0) return false;
        if (Float.compare(y,comp.y) != 0) return false;
        if (Float.compare(z,comp.z) != 0) return false;
        return true;
    }
    
    public boolean issame(Vector3f comp) {
        if(Math.abs(x-comp.x)>1f) return false;
        if(Math.abs(y-comp.y)>1f) return false;
        if(Math.abs(z-comp.z)>1f) return false;
        return true;
    }
    
    public void floorLocal() {
        x=FastMath.floor(x);
        y=FastMath.floor(y);
        z=FastMath.floor(z);
    }
    
    public void toFile(DataOutputStream output) throws IOException {
        output.writeFloat(x);
        output.writeFloat(y);
        output.writeFloat(z);
        
    }
    
    public void fromFile(DataInputStream input) throws IOException {
        x = input.readFloat();
        y = input.readFloat();
        z = input.readFloat();
        
    }
    
    
}
