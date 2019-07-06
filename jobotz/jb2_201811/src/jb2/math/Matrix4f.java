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
public abstract class Matrix4f {
    public abstract float m00();
    public abstract float m01();
    public abstract float m02();
    public abstract float m03();
    public abstract float m10();
    public abstract float m11();
    public abstract float m12();
    public abstract float m13();
    public abstract float m20();
    public abstract float m21();
    public abstract float m22();
    public abstract float m23();
    public abstract float m30();
    public abstract float m31();
    public abstract float m32();
    public abstract float m33();
    
    /**
     * Returns the determinant of a 4x4 matrix.
     */
    public float determinant( ) {
        double m00 = m00(); double m01 = m01(); double m02 = m02(); double m03 = m03();
        double m10 = m10(); double m11 = m11(); double m12 = m12(); double m13 = m13();
        double m20 = m20(); double m21 = m21(); double m22 = m22(); double m23 = m23();
        double m30 = m30(); double m31 = m31(); double m32 = m32(); double m33 = m33();
        
        double det01 = m20 * m31 - m21 * m30;
        double det02 = m20 * m32 - m22 * m30;
        double det03 = m20 * m33 - m23 * m30;
        double det12 = m21 * m32 - m22 * m31;
        double det13 = m21 * m33 - m23 * m31;
        double det23 = m22 * m33 - m23 * m32;
        return (float) (m00 * (m11 * det23 - m12 * det13 + m13 * det12) - m01
                * (m10 * det23 - m12 * det03 + m13 * det02) + m02
                * (m10 * det13 - m11 * det03 + m13 * det01) - m03
                * (m10 * det12 - m11 * det02 + m12 * det01));
    }
}
