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

import jb2.jo.JOAdd;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class Quaternion {
     public float x, y, z, w;
     public Quaternion() {
        x = 0;
        y = 0;
        z = 0;
        w = 1;
    }
     
    /**
     * <code>fromAngles</code> builds a quaternion from the Euler rotation
     * angles (x,y,z).
     *
     * @param xAngle
     *            the Euler xangle of rotation (in radians).
     * @param yAngle
     *            the Euler yangle of rotation (in radians).
     * @param zAngle
     *            the Euler zangle of rotation (in radians).
     */
    public void fromAngles(float xAngle, float yAngle, float zAngle) {
        float angle;
        float sr, sp, sy, cr, cp, cy;
        angle = zAngle * 0.5f;
        sy = FastMath.sin(angle);
        cy = FastMath.cos(angle);
        angle = yAngle * 0.5f;
        sp = FastMath.sin(angle);
        cp = FastMath.cos(angle);
        angle = xAngle * 0.5f;
        sr = FastMath.sin(angle);
        cr = FastMath.cos(angle);

        float crcp = cr * cp;
        float srsp = sr * sp;

        x = (sr * cp * cy - cr * sp * sy);
        y = (cr * sp * cy + sr * cp * sy);
        z = (crcp * sy - srsp * cy);
        w = (crcp * cy + srsp * sy);
    }
    
    public void fromThetaPhiOmega(int theta, int phi, int omega) {
        float th=(float) (360f+((float)theta/JOAdd.C_GAMEOBJECT_DEGMUL)+90f)*FastMath.DEG_TO_RAD;
        float ph=(float)phi/JOAdd.C_GAMEOBJECT_DEGMUL;
        float om=(float)omega/JOAdd.C_GAMEOBJECT_DEGMUL;
        fromAngles(th, ph, om);
    }
    
    public float[] toAngles(float[] angles) {
        if (angles == null)
                angles = new float[3];

        float sqw = w * w;
        float sqx = x * x;
        float sqy = y * y;
        float sqz = z * z;
        float unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise
                                            // is correction factor
        float test = x * y + z * w;
        if (test > 0.499 * unit) { // singularity at north pole
                angles[1] = 2 * FastMath.atan2(x, w);
                angles[2] = FastMath.HALF_PI;
                angles[0] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
                angles[1] = -2 * FastMath.atan2(x, w);
                angles[2] = -FastMath.HALF_PI;
                angles[0] = 0;
        } else {
                angles[1] = FastMath.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw);
                angles[2] = FastMath.asin(2 * test / unit);
                angles[0] = FastMath.atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw);
        }
        return angles;
    }
    
    public int[] toThetaPhiOmega(int[] store) {
        if(store==null)
            store=new int[3];
        float[] tmpArr = LocalContext.getContext().Quaternion_toThetaPhiOmega_tmpArr;
        toAngles(tmpArr);
        tmpArr[0] = (tmpArr[0]*FastMath.RAD_TO_DEG -90);
        if(tmpArr[0]>180) {
            // normalize degrees
            tmpArr[0]-=360;
        }
        if(tmpArr[0]<180) {
            // normalize degrees
            tmpArr[0]+=360;
        }
        store[0] = (int) (tmpArr[0]*FastMath.RAD_TO_DEG*JOAdd.C_GAMEOBJECT_DEGMUL);
        store[1] = (int) (tmpArr[1]*FastMath.RAD_TO_DEG*JOAdd.C_GAMEOBJECT_DEGMUL);
        store[2] = (int) (tmpArr[2]*FastMath.RAD_TO_DEG*JOAdd.C_GAMEOBJECT_DEGMUL);
        return store;
    }
    
    public Vector3f multLocal(Vector3f v) {
        float tempX, tempY;
        tempX = w * w * v.x + 2 * y * w * v.z - 2 * z * w * v.y + x * x * v.x
                + 2 * y * x * v.y + 2 * z * x * v.z - z * z * v.x - y * y * v.x;
        tempY = 2 * x * y * v.x + y * y * v.y + 2 * z * y * v.z + 2 * w * z
                * v.x - z * z * v.y + w * w * v.y - 2 * x * w * v.z - x * x
                * v.y;
        v.z = 2 * x * z * v.x + 2 * y * z * v.y + z * z * v.z - 2 * w * y * v.x
                - y * y * v.z + 2 * w * x * v.y - x * x * v.z + w * w * v.z;
        v.x = tempX;
        v.y = tempY;
        return v;
    }
}
