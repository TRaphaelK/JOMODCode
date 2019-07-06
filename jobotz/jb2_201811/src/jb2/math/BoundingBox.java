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
public class BoundingBox {
    // center of the AABB
    public final Vector3f center = new Vector3f();
    // extents of the BB in corresponding axes
    public final Vector3f extents = new Vector3f();
    
    public BoundingBox(Vector3f cent, float xext, float yext, float zext) {
        center.set(cent);
        extents.set(xext, yext, zext);
    }
    
     public BoundingBox(float x, float y, float z, float xext, float yext, float zext) {
        center.set(x, y, z);
        extents.set(xext, yext, zext);
    }

    public BoundingBox() {
    }
    
    public BoundingBox set(BoundingBox source) {
        center.set(source.center);
        extents.set(source.extents);
        return this;
    }

    public boolean contains(Vector3f point) {
        return contains(point.x, point.y, point.z);
    }
    
    /**
     * When the whole box is on a same side of the origin
     * @return 
     */
    public boolean sameSide(Vector3f origin) {
        if(center.x-extents.x<=origin.x 
                && center.x+extents.x>=origin.x )
            return false;
        if(center.y-extents.y<=origin.y 
                && center.y+extents.y>=origin.y )
            return false;
        if(center.z-extents.z<=origin.z 
                && center.z+extents.z>=origin.z )
            return false;
        return true;
    }
    
    public boolean contains2d(Vector3f point) {
        return contains2d(point.x, point.z);
    }
    
    public boolean contains(float x, float y, float z) {
        if(x<center.x-extents.x)
            return false;
        if(x>center.x+extents.x)
            return false;
        if(y<center.y-extents.y)
            return false;
        if(y>center.y+extents.y)
            return false;
        if(z<center.z-extents.z)
            return false;
        if(z>center.z+extents.z)
            return false;
        return true;
    }
    
    public boolean contains2d(float x, float z) {
        if(x<center.x-extents.x)
            return false;
        if(x>center.x+extents.x)
            return false;
        if(z<center.z-extents.z)
            return false;
        if(z>center.z+extents.z)
            return false;
        return true;
    }
    
    public boolean intersects(BoundingBox bb, boolean twoD) {
        if(twoD) {
            return intersects2d(bb);
        }
        return intersects(bb);
    }
    
    public boolean intersects(BoundingBox bb) {
        if (center.x + extents.x < bb.center.x - bb.extents.x
                || center.x - extents.x > bb.center.x + bb.extents.x)
            return false;
        else if (center.y + extents.y < bb.center.y - bb.extents.y
                || center.y - extents.y > bb.center.y + bb.extents.y)
            return false;
        else if (center.z + extents.z < bb.center.z - bb.extents.z
                || center.z - extents.z > bb.center.z + bb.extents.z)
            return false;
        else
            return true;
    }
   
    public boolean intersectsy(BoundingBox bb) {
        if (center.y + extents.y < bb.center.y - bb.extents.y
                || center.y - extents.y > bb.center.y + bb.extents.y)
            return false;
        return true;
    }

    public boolean intersects2d(BoundingBox bb) {
        if (center.x + extents.x < bb.center.x - bb.extents.x
                || center.x - extents.x > bb.center.x + bb.extents.x)
            return false;
//        else if (center.y + extents.y < bb.center.y - bb.extents.y
//                || center.y - extents.y > bb.center.y + bb.extents.y)
//            return false;
        else if (center.z + extents.z < bb.center.z - bb.extents.z
                || center.z - extents.z > bb.center.z + bb.extents.z)
            return false;
        else
            return true;
    }
    
    public boolean intersects(float[] bbcenters, float[] bbextents, int index) {
        float centersx = bbcenters[index];
        float centersy = bbcenters[index+1];
        float centersz = bbcenters[index+2];

        float extentsx = bbextents[index];
        float extentsy = bbextents[index+1];
        float extentsz = bbextents[index+2];
        
        if (center.x + extents.x < centersx - extentsx
                || center.x - extents.x > centersx + extentsx)
            return false;
        else if (center.y + extents.y < centersy - extentsy
                || center.y - extents.y > centersy + extentsy)
            return false;
        else if (center.z + extents.z < centersz - extentsz
                || center.z - extents.z > centersz + extentsz)
            return false;
        else
            return true;
    }
    
    public boolean intersects(float[] bbdata, int index) {
        float centersx = bbdata[index];
        float centersy = bbdata[index+1];
        float centersz = bbdata[index+2];

        float extentsx = bbdata[index+3];
        float extentsy = bbdata[index+4];
        float extentsz = bbdata[index+5];
        
        if (center.x + extents.x < centersx - extentsx
                || center.x - extents.x > centersx + extentsx)
            return false;
        else if (center.y + extents.y < centersy - extentsy
                || center.y - extents.y > centersy + extentsy)
            return false;
        else if (center.z + extents.z < centersz - extentsz
                || center.z - extents.z > centersz + extentsz)
            return false;
        else
            return true;
    }
    
    public boolean contains(BoundingBox other, boolean twoD) {
        if(twoD) {
            return contains2d(other);
        }
        return contains(other);
    }
    
    public boolean contains(BoundingBox other) {
        if( other.center.x - other.extents.x < center.x - extents.x )
            return false;
        if( other.center.x + other.extents.x > center.x + extents.x )
            return false;
        if( other.center.y - other.extents.y < center.y - extents.y )
            return false;
        if( other.center.y + other.extents.y > center.y + extents.y )
            return false;
        if( other.center.z - other.extents.z < center.z - extents.z )
            return false;
        if( other.center.z + other.extents.z > center.z + extents.z )
            return false;
        return true;
    }
    
    public boolean containsExt(BoundingBox other, boolean twoD) {
        if( other.center.x - other.extents.x < center.x - extents.x )
            return false;
        if( other.center.x + other.extents.x > center.x + extents.x )
            return false;
        if( other.center.z - other.extents.z < center.z - extents.z )
            return false;
        if( other.center.z + other.extents.z > center.z + extents.z )
            return false;
        if(!twoD) {
            // use intersect with y
            if(center.y + extents.y < other.center.y - other.extents.y
                    || center.y - extents.y > other.center.y + other.extents.y)
                return false;
        }
        return true;
    }
    
    public boolean contains2d(BoundingBox other) {
        if( other.center.x - other.extents.x < center.x - extents.x )
            return false;
        if( other.center.x + other.extents.x > center.x + extents.x )
            return false;
        //if( other.center.y - other.extents.y < center.y - extents.y )
        //    return false;
        //if( other.center.y + other.extents.y > center.y + extents.y )
        //    return false;
        if( other.center.z - other.extents.z < center.z - extents.z )
            return false;
        if( other.center.z + other.extents.z > center.z + extents.z )
            return false;
        return true;
    }
    
    public boolean contains(float[] bbcenters, float[] bbextents, int index) {
        float centersx = bbcenters[index];
        float centersy = bbcenters[index+1];
        float centersz = bbcenters[index+2];

        float extentsx = bbextents[index];
        float extentsy = bbextents[index+1];
        float extentsz = bbextents[index+2];
        
        if( centersx - extentsx < center.x - extents.x )
            return false;
        if( centersx + extentsx > center.x + extents.x )
            return false;
        if( centersy - extentsy < center.y - extents.y )
            return false;
        if( centersy + extentsy > center.y + extents.y )
            return false;
        if( centersz - extentsz < center.z - extents.z )
            return false;
        if( centersz + extentsz > center.z + extents.z )
            return false;
        return true;
    }
    
    public boolean contains(float[] bbdata, int index) {
        float centersx = bbdata[index];
        float centersy = bbdata[index+1];
        float centersz = bbdata[index+2];

        float extentsx = bbdata[index+3];
        float extentsy = bbdata[index+4];
        float extentsz = bbdata[index+5];
        
        if( centersx - extentsx < center.x - extents.x )
            return false;
        if( centersx + extentsx > center.x + extents.x )
            return false;
        if( centersy - extentsy < center.y - extents.y )
            return false;
        if( centersy + extentsy > center.y + extents.y )
            return false;
        if( centersz - extentsz < center.z - extents.z )
            return false;
        if( centersz + extentsz > center.z + extents.z )
            return false;
        return true;
    }
    
    public int collide(float[] bbdata, int index) {
        float centersx = bbdata[index];
        float centersy = bbdata[index+1];
        float centersz = bbdata[index+2];

        float extentsx = bbdata[index+3];
        float extentsy = bbdata[index+4];
        float extentsz = bbdata[index+5];
        
        
        if (center.x + extents.x < centersx - extentsx
                || center.x - extents.x > centersx + extentsx)
            return 0;
        else if (center.y + extents.y < centersy - extentsy
                || center.y - extents.y > centersy + extentsy)
            return 0;
        else if (center.z + extents.z < centersz - extentsz
                || center.z - extents.z > centersz + extentsz)
            return 0;
        
        if( centersx - extentsx < center.x - extents.x )
            return -1;
        if( centersx + extentsx > center.x + extents.x )
            return -1;
        if( centersy - extentsy < center.y - extents.y )
            return -1;
        if( centersy + extentsy > center.y + extents.y )
            return -1;
        if( centersz - extentsz < center.z - extents.z )
            return -1;
        if( centersz + extentsz > center.z + extents.z )
            return -1;
        
        return 1;
    }
    
    /**
     * determines if this bounding box intersects with a given ray object. If an
     * intersection has occurred, true is returned, otherwise false is returned.
     * 
     * @param ray
     * @see com.jme.bounding.BoundingVolume#intersects(com.jme.math.Ray)
     */
    public boolean intersects(Ray ray) {
        float rhs;

        Context tmp = LocalContext.getContext();
        Vector3f diff = tmp._compVect1;
        Vector3f wCrossD = tmp._compVect2;
        
        diff.set(ray.origin).subtractLocal(center);

        float[] fWdU = tmp.fWdU;
        float[] fAWdU = tmp.fAWdU;
        float[] fDdU = tmp.fDdU;
        float[] fADdU = tmp.fADdU;
        float[] fAWxDdU = tmp.fAWxDdU;
         
        fWdU[0] = ray.direction.dot(Vector3f.UNIT_X);
        fAWdU[0] = FastMath.abs(fWdU[0]);
        fDdU[0] = diff.dot(Vector3f.UNIT_X);
        fADdU[0] = FastMath.abs(fDdU[0]);
        if (fADdU[0] > extents.x && fDdU[0] * fWdU[0] >= 0.0) {
            return false;
        }

        fWdU[1] = ray.direction.dot(Vector3f.UNIT_Y);
        fAWdU[1] = FastMath.abs(fWdU[1]);
        fDdU[1] = diff.dot(Vector3f.UNIT_Y);
        fADdU[1] = FastMath.abs(fDdU[1]);
        if (fADdU[1] > extents.y && fDdU[1] * fWdU[1] >= 0.0) {
            return false;
        }

        fWdU[2] = ray.direction.dot(Vector3f.UNIT_Z);
        fAWdU[2] = FastMath.abs(fWdU[2]);
        fDdU[2] = diff.dot(Vector3f.UNIT_Z);
        fADdU[2] = FastMath.abs(fDdU[2]);
        if (fADdU[2] > extents.z && fDdU[2] * fWdU[2] >= 0.0) {
            return false;
        }

        wCrossD.set(ray.direction).crossLocal(diff);

        fAWxDdU[0] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_X));
        rhs = extents.y * fAWdU[2] + extents.z * fAWdU[1];
        if (fAWxDdU[0] > rhs) {
            return false;
        }

        fAWxDdU[1] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Y));
        rhs = extents.x * tmp.fAWdU[2] + extents.z * tmp.fAWdU[0];
        if (fAWxDdU[1] > rhs) {
            return false;
        }

        fAWxDdU[2] = FastMath.abs(wCrossD.dot(Vector3f.UNIT_Z));
        rhs = extents.x * fAWdU[1] + extents.y * fAWdU[0];
        if (fAWxDdU[2] > rhs) {
            return false;
        }

        return true;
    }

    /**
     * @see com.vlengine.bounding.BoundingVolume#intersectsWhere(com.vlengine.math.Ray)
     */
    public IntersectionRecord intersectsWhere(Ray ray, IntersectionRecord record) {
        if(record==null)
            record=new IntersectionRecord();
        
        Context tmp=LocalContext.getContext();
        
        Vector3f diff = tmp._compVect1.set(ray.origin).subtractLocal(center);
        // convert ray to box coordinates
        Vector3f direction = tmp._compVect2.set(ray.direction);
        
        float[] t = record.getDistances(2);
        t[0] = 0f; t[1] = Float.POSITIVE_INFINITY;
        
        float saveT0 = t[0], saveT1 = t[1];
        boolean notEntirelyClipped = clip(+direction.x, -diff.x - extents.x, t)
                && clip(-direction.x, +diff.x - extents.x, t)
                && clip(+direction.y, -diff.y - extents.y, t)
                && clip(-direction.y, +diff.y - extents.y, t)
                && clip(+direction.z, -diff.z - extents.z, t)
                && clip(-direction.z, +diff.z - extents.z, t);
        
        if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
            if (t[1] > t[0]) {
                Vector3f[] points = record.getPoints(2);
                points[0].set(ray.direction).multLocal(t[0]).addLocal(ray.origin);
                points[1].set(ray.direction).multLocal(t[1]).addLocal(ray.origin);
                return record;
            } 
            
            float[] distances = record.getDistances(1);
            distances[0] = t[0];
            Vector3f[] points = record.getPoints(1);
            points[0].set(ray.direction).multLocal(distances[0]).addLocal(ray.origin);
            return record;            
        } 
        record.getDistances(0);
        return record;
       
    }
    
    /**
     * <code>clip</code> determines if a line segment intersects the current
     * test plane.
     * 
     * @param denom
     *            the denominator of the line segment.
     * @param numer
     *            the numerator of the line segment.
     * @param t
     *            test values of the plane.
     * @return true if the line segment intersects the plane, false otherwise.
     */
    private boolean clip(float denom, float numer, float[] t) {
        // Return value is 'true' if line segment intersects the current test
        // plane. Otherwise 'false' is returned in which case the line segment
        // is entirely clipped.
        if (denom > 0.0f) {
            if (numer > denom * t[1])
                return false;
            if (numer > denom * t[0])
                t[0] = numer / denom;
            return true;
        } else if (denom < 0.0f) {
            if (numer > denom * t[0])
                return false;
            if (numer > denom * t[1])
                t[1] = numer / denom;
            return true;
        } else {
            return numer <= 0.0;
        }
    }
    
    public Vector3f fillRandomPointInside(Vector3f store) {
        store.x = center.x-extents.x+2*(FastMath.rand.nextFloat()*extents.x);
        store.z = center.z-extents.z+2*(FastMath.rand.nextFloat()*extents.z);
        store.y = center.y-extents.y+2*(FastMath.rand.nextFloat()*extents.y);
        return store;
    }
    
    public void toFile(DataOutputStream output) throws IOException {
        output.writeFloat(center.x);
        output.writeFloat(center.y);
        output.writeFloat(center.z);
        
        output.writeFloat(extents.x);
        output.writeFloat(extents.y);
        output.writeFloat(extents.z);
    }
    
    public void fromFile(DataInputStream input) throws IOException {
        center.x = input.readFloat();
        center.y = input.readFloat();
        center.z = input.readFloat();
        
        extents.x = input.readFloat();
        extents.y = input.readFloat();
        extents.z = input.readFloat();
    }
    
    public void add(Vector3f coord) {
        float minx = center.x - extents.x;
        float maxx = center.x + extents.x;
        float minz = center.z - extents.z;
        float maxz = center.z + extents.z;
        float miny = center.y - extents.y;
        float maxy = center.y + extents.y;
        
        if(coord.x<minx)
            minx=coord.x;
        if(coord.x>maxx)
            maxx=coord.x;
        if(coord.z<minz)
            minz=coord.z;
        if(coord.z>maxz)
            maxz=coord.z;
        if(coord.y<miny)
            miny=coord.y;
        if(coord.y>maxy)
            maxy=coord.y;
        center.set((minx+maxx)/2f, (miny+maxy)/2f, (minz+maxz)/2f);
        extents.set((maxx-minx)/2f, (maxy-miny)/2f, (maxz-minz)/2f);
    }
    
    @Override
    public String toString() {
        return "{" + center + "," + extents + '}';
        
    }    
    
}
