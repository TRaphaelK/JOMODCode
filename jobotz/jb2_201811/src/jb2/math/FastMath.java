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

import java.util.Random;

/**
 *
 * @author vear
 */
final public class FastMath {
    private FastMath() {
    }
    //sources:
    //https://github.com/postspectacular/toxiclibs/blob/master/src.core/toxi/math/MathUtils.java
    //https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/FastMath.java
    //https://github.com/JOML-CI/JOML/blob/master/src/org/joml/Math.java
    
    /** A "close to zero" double epsilon value for use*/
    public static final double DBL_EPSILON = 2.220446049250313E-16d;
    /** A "close to zero" float epsilon value for use*/
    public static final float FLT_EPSILON = 1.1920928955078125E-7f;
    /** A "close to zero" float epsilon value for use*/
    public static final float ZERO_TOLERANCE = 0.0001f;
    public static final float ONE_THIRD = 1f / 3f;
    /** The value PI as a float. */
    public static final float PI = (float) Math.PI;
    /** The value 2PI as a float. */
    public static final float TWO_PI = 2.0f * PI;
    /** The value PI/2 as a float. */
    public static final float HALF_PI = 0.5f * PI;
    /** The value 1/PI as a float. */
    public static final float INV_PI = 1.0f / PI;
    public static final double INV_PI_D = 1.0 / PI;
    /** The value 1/(2PI) as a float. */
    public static final float INV_TWO_PI = 1.0f / TWO_PI;
    /** A value to multiply a degree value by, to convert it to radians. */
    public static final float DEG_TO_RAD = PI / 180.0f;
    /** A value to multiply a radian value by, to convert it to degrees. */
    public static final float RAD_TO_DEG = 180.0f / PI;
    // value of log(2)
    public static final float LOG2 = (float) Math.log(2);
    /** A precreated random object for random numbers. */
    public static final Random rand = new Random(System.currentTimeMillis());
    
    /**
     * Returns true if the number is a power of 2 (2,4,8,16...)
     * 
     * A good implementation found on the Java boards. note: a number is a power
     * of two if and only if it is the smallest number with that number of
     * significant bits. Therefore, if you subtract 1, you know that the new
     * number will have fewer bits, so ANDing the original number with anything
     * less than it will give 0.
     * 
     * @param number
     *            The number to test.
     * @return True if it is a power of two.
     */
    public static boolean isPowerOfTwo(int number) {
        return (number > 0) && (number & (number - 1)) == 0;
    }
    
    /**
	 * @param number
	 *            the number to test.  (NB: This implementation only handles numbers <= 1073741824)
	 * @return the next power of two greater than or equal to the given number.
	 */
    public static int nearestPowerOfTwo(int number) {
        --number;
        number |= number >> 16;
        number |= number >> 8;
        number |= number >> 4;
        number |= number >> 2;
        number |= number >> 1;
        ++number;
        return number;
    }

    /**
     * Linear interpolation from startValue to endValue by the given percent.
     * Basically: ((1 - percent) * startValue) + (percent * endValue)
     * 
     * @param scale
     *            scale value to use. if 1, use endValue, if 0, use startValue.
     * @param startValue
     *            Beginning value. 0% of f
     * @param endValue
     *            ending value. 100% of f
     * @return The interpolated value between startValue and endValue.
     */
    public static float interpolateLinear(float scale, float startValue, float endValue) {
        if (startValue == endValue) {
            return startValue;
        }
        if (scale <= 0f) {
            return startValue;
        }
        if (scale >= 1f) {
            return endValue;
        }
        return ((1f - scale) * startValue) + (scale * endValue);
    }
    
    /**
     * Linear extrapolation from startValue to endValue by the given scale.
     * if scale is between 0 and 1 this method returns the same result as interpolateLinear
     * if the scale is over 1 the value is linearly extrapolated.
     * Note that the end value is the value for a scale of 1.
     * @param scale the scale for extrapolation
     * @param startValue the starting value (scale = 0)
     * @param endValue the end value (scale = 1)
     * @return an extrapolation for the given parameters
     */
    public static float lerp(float scale, float startValue, float endValue) {
        return ((1f - scale) * startValue) + (scale * endValue);
    }

    /**Interpolate a spline between at least 4 control points following the Catmull-Rom equation.
     * here is the interpolation matrix
     * m = [ 0.0  1.0  0.0   0.0 ]
     *     [-T    0.0  T     0.0 ]
     *     [ 2T   T-3  3-2T  -T  ]
     *     [-T    2-T  T-2   T   ]
     * where T is the curve tension
     * the result is a value between p1 and p2, t=0 for p1, t=1 for p2
     * @param u value from 0 to 1
     * @param T The tension of the curve
     * @param p0 control point 0
     * @param p1 control point 1
     * @param p2 control point 2
     * @param p3 control point 3
     * @return Catmull–Rom interpolation
     */
    public static float interpolateCatmullRom(float u, float T, float p0, float p1, float p2, float p3) {
        float c1, c2, c3, c4;
        c1 = p1;
        c2 = -1.0f * T * p0 + T * p2;
        c3 = 2 * T * p0 + (T - 3) * p1 + (3 - 2 * T) * p2 + -T * p3;
        c4 = -T * p0 + (2 - T) * p1 + (T - 2) * p2 + T * p3;

        return (float) (((c4 * u + c3) * u + c2) * u + c1);
    }
    
    /**Interpolate a spline between at least 4 control points following the Bezier equation.
     * here is the interpolation matrix
     * m = [ -1.0   3.0  -3.0    1.0 ]
     *     [  3.0  -6.0   3.0    0.0 ]
     *     [ -3.0   3.0   0.0    0.0 ]
     *     [  1.0   0.0   0.0    0.0 ]
     * where T is the curve tension
     * the result is a value between p1 and p3, t=0 for p1, t=1 for p3
     * @param u value from 0 to 1
     * @param p0 control point 0
     * @param p1 control point 1
     * @param p2 control point 2
     * @param p3 control point 3
     * @return Bezier interpolation
     */
    public static float interpolateBezier(float u, float p0, float p1, float p2, float p3) {
        float oneMinusU = 1.0f - u;
        float oneMinusU2 = oneMinusU * oneMinusU;
        float u2 = u * u;
        return p0 * oneMinusU2 * oneMinusU
                + 3.0f * p1 * u * oneMinusU2
                + 3.0f * p2 * u2 * oneMinusU
                + p3 * u2 * u;
    }
    
    /**
     * Returns the arc cosine of a value.<br>
     * Special cases:
     * <ul><li>If fValue is smaller than -1, then the result is PI.
     * <li>If the argument is greater than 1, then the result is 0.</ul>
     * @param fValue The value to arc cosine.
     * @return The angle, in radians.
     * @see java.lang.Math#acos(double)
     */
    public static float acos(float fValue) {
        if (-1.0f < fValue) {
            if (fValue < 1.0f) {
                return (float) Math.acos(fValue);
            }
            return 0.0f;
        }
        return PI;
    }

    /**
     * Returns the arc sine of a value.<br>
     * Special cases:
     * <ul><li>If fValue is smaller than -1, then the result is -HALF_PI.
     * <li>If the argument is greater than 1, then the result is HALF_PI.</ul>
     * @param fValue The value to arc sine.
     * @return the angle in radians.
     * @see java.lang.Math#asin(double)
     */
    public static float asin(float fValue) {
        if (-1.0f < fValue) {
            if (fValue < 1.0f) {
                return (float) Math.asin(fValue);
            }

            return HALF_PI;
        }

        return -HALF_PI;
    }

    /**
     * Returns the arc tangent of an angle given in radians.<br>
     * @param fValue The angle, in radians.
     * @return fValue's atan
     * @see java.lang.Math#atan(double)
     */
    public static float atan(float fValue) {
        return (float) Math.atan(fValue);
    }

    /**
     * A direct call to Math.atan2.
     * @param fY
     * @param fX
     * @return Math.atan2(fY,fX)
     * @see java.lang.Math#atan2(double, double)
     */
    public static float atan2(float fY, float fX) {
        return (float) Math.atan2(fY, fX);
    }

    /**
     * Rounds a fValue up.  A call to Math.ceil
     * @param fValue The value.
     * @return The fValue rounded up
     * @see java.lang.Math#ceil(double)
     */
    public static float ceil(float fValue) {
        return (float) Math.ceil(fValue);
    }
    
    /**
     * Fast Trig functions for x86. This forces the trig functiosn to stay
     * within the safe area on the x86 processor (-45 degrees to +45 degrees)
     * The results may be very slightly off from what the Math and StrictMath
     * trig functions give due to rounding in the angle reduction but it will be
     * very very close. 
     * 
     * note: code from wiki posting on java.net by jeffpk
     */
    public static float reduceSinAngle(float radians) {
        radians %= TWO_PI; // put us in -2PI to +2PI space
        if (Math.abs(radians) > PI) { // put us in -PI to +PI space
            radians = radians - (TWO_PI);
        }
        if (Math.abs(radians) > HALF_PI) {// put us in -PI/2 to +PI/2 space
            radians = PI - radians;
        }

        return radians;
    }
    
    private static final double k1 = Double.longBitsToDouble(-4628199217061079959L);
    private static final double k2 = Double.longBitsToDouble(4575957461383549981L);
    private static final double k3 = Double.longBitsToDouble(-4671919876307284301L);
    private static final double k4 = Double.longBitsToDouble(4523617213632129738L);
    private static final double k5 = Double.longBitsToDouble(-4730215344060517252L);
    private static final double k6 = Double.longBitsToDouble(4460268259291226124L);
    private static final double k7 = Double.longBitsToDouble(-4798040743777455072L);
    /**
     * Reference: <a href="http://www.java-gaming.org/topics/joml-1-8-0-release/37491/msg/361815/view.html#msg361815">http://www.java-gaming.org/</a>
     */
    private static double sin_roquen_newk(double v) {
      double i  = java.lang.Math.rint(v*INV_PI_D);
      double x  = v - i * Math.PI;
      double qs = 1-2*((int)i & 1);
      double x2 = x*x;
      double r;
      x = qs*x;
      r =        k7;
      r = r*x2 + k6;
      r = r*x2 + k5;
      r = r*x2 + k4;
      r = r*x2 + k3;
      r = r*x2 + k2;
      r = r*x2 + k1;
      return x + x*x2*r;
    }
    
/**
     * Returns sine of a value. 
     * 
     * note: code from wiki posting on java.net by jeffpk
     * 
     * @param fValue
     *            The value to sine, in radians.
     * @return The sine of fValue.
     * @see java.lang.Math#sin(double)
     */
    public static float sin(float fValue) {
        return (float) sin_roquen_newk(fValue);
    }
    
/**
     * Returns cos of a value.
     * 
     * @param fValue
     *            The value to cosine, in radians.
     * @return The cosine of fValue.
     * @see java.lang.Math#cos(double)
     */
    public static float cos(float fValue) {
        return sin(fValue + HALF_PI);
    }
    
    /**
     * Returns E^fValue
     * @param fValue Value to raise to a power.
     * @return The value E^fValue
     * @see java.lang.Math#exp(double)
     */
    public static float exp(float fValue) {
        return (float) Math.exp(fValue);
    }
    
    public static float abs(float r) {
        return java.lang.Math.abs(r);
    }
    
    /**
     * This method is a *lot* faster than using Math.floor(x).
     * 
     * @param x
     *            value to be floored
     * @return floored value
     * @since 0012
     */
    public static final float floor(float x) {
        int y = (int) x;
        if (x < 0 && x != y) {
            y--;
        }
        return (float)y;
    }
    
    public static float sqrt(float fValue) {
        return (float) Math.sqrt(fValue);
    }
    
/**
     * Returns 1/sqrt(fValue)
     * @param fValue The value to process.
     * @return 1/sqrt(fValue)
     * @see java.lang.Math#sqrt(double)
     */
    public static float invSqrt(float fValue) {
        return (float) (1.0f / Math.sqrt(fValue));
    }

    public static float fastInvSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x); // get bits for floating value
        i = 0x5f375a86 - (i >> 1); // gives initial guess y0
        x = Float.intBitsToFloat(i); // convert bits back to float
        x = x * (1.5f - xhalf * x * x); // Newton step, repeating increases accuracy
        return x;
    }
    
    /**
     * Returns the log base E of a value.
     * @param fValue The value to log.
     * @return The log of fValue base E
     * @see java.lang.Math#log(double)
     */
    public static float log(float fValue) {
        return (float) Math.log(fValue);
    }

    /**
     * Returns the logarithm of value with given base, calculated as log(value)/log(base), 
     * so that pow(base, return)==value (contributed by vear)
     * @param value The value to log.
     * @param base Base of logarithm.
     * @return The logarithm of value with given base
     */
    public static float log(float value, float base) {
        return (float) (Math.log(value) / Math.log(base));
    }
    
    /**
     * Returns the logarithm of value with base 2, calculated as log(value)/log(2), 
     * so that pow(2, return)==value (contributed by vear)
     * @param value The value to log.
     * @return The logarithm of value with base 2
     */
    public static float log2(float value) {
        return (float) (Math.log(value) / LOG2);
    }
    
    /**
     * Returns a number raised to an exponent power.  fBase^fExponent
     * @param fBase The base value (IE 2)
     * @param fExponent The exponent value (IE 3)
     * @return base raised to exponent (IE 8)
     * @see java.lang.Math#pow(double, double)
     */
    public static float pow(float fBase, float fExponent) {
        return (float) Math.pow(fBase, fExponent);
    }
    
    /**
     * Computes a fast approximation to <code>Math.pow(a, b)</code>. Adapted
     * from http://www.dctsystems.co.uk/Software/power.html.
     * 
     * @param a
     *            a positive number
     * @param b
     *            a number
     * @return a^b
     * 
     */
    private static final float SHIFT23 = 1 << 23;
    private static final float INV_SHIFT23 = 1.0f / SHIFT23;
    
    public static final float fastPow(float a, float b) {
        float x = Float.floatToRawIntBits(a);
        x *= INV_SHIFT23;
        x -= 127;
        float y = x - (x >= 0 ? (int) x : (int) x - 1);
        b *= x + (y - y * y) * 0.346607f;
        y = b - (b >= 0 ? (int) b : (int) b - 1);
        y = (y - y * y) * 0.33971f;
        return Float.intBitsToFloat((int) ((b + 127 - y) * SHIFT23));
    }
    
/**
     * Returns the tangent of a value.  If USE_FAST_TRIG is enabled, an approximate value
     * is returned.  Otherwise, a direct value is used.
     * @param fValue The value to tangent, in radians.
     * @return The tangent of fValue.
     * @see java.lang.Math#tan(double)
     */
    public static float tan(float fValue) {
        return (float) Math.tan(fValue);
    }
    
    /**
     * Given 3 points in a 2d plane, this function computes if the points going from A-B-C
     * are moving counter clock wise.
     * @param p0 Point 0.
     * @param p1 Point 1.
     * @param p2 Point 2.
     * @return 1 If they are CCW, -1 if they are not CCW, 0 if p2 is between p0 and p1.
     */
    public static int counterClockwise(Vector2f p0, Vector2f p1, Vector2f p2) {
        float dx1, dx2, dy1, dy2;
        dx1 = p1.x - p0.x;
        dy1 = p1.y - p0.y;
        dx2 = p2.x - p0.x;
        dy2 = p2.y - p0.y;
        if (dx1 * dy2 > dy1 * dx2) {
            return 1;
        }
        if (dx1 * dy2 < dy1 * dx2) {
            return -1;
        }
        if ((dx1 * dx2 < 0) || (dy1 * dy2 < 0)) {
            return -1;
        }
        if ((dx1 * dx1 + dy1 * dy1) < (dx2 * dx2 + dy2 * dy2)) {
            return 1;
        }
        return 0;
    }

    /**
     * Test if a point is inside a triangle.  1 if the point is on the ccw side,
     * -1 if the point is on the cw side, and 0 if it is on neither.
     * @param t0 First point of the triangle.
     * @param t1 Second point of the triangle.
     * @param t2 Third point of the triangle.
     * @param p The point to test.
     * @return Value 1 or -1 if inside triangle, 0 otherwise.
     */
    public static int pointInsideTriangle(Vector2f t0, Vector2f t1, Vector2f t2, Vector2f p) {
        int val1 = counterClockwise(t0, t1, p);
        if (val1 == 0) {
            return 1;
        }
        int val2 = counterClockwise(t1, t2, p);
        if (val2 == 0) {
            return 1;
        }
        if (val2 != val1) {
            return 0;
        }
        int val3 = counterClockwise(t2, t0, p);
        if (val3 == 0) {
            return 1;
        }
        if (val3 != val1) {
            return 0;
        }
        return val3;
    }
    
    /**
     * Takes an value and expresses it in terms of min to max.
     * 
     * @param val -
     *            the angle to normalize (in radians)
     * @return the normalized angle (also in radians)
     */
    public static float normalize(float val, float min, float max) {
        if (Float.isInfinite(val) || Float.isNaN(val)) {
            return 0f;
        }
        float range = max - min;
        while (val > max) {
            val -= range;
        }
        while (val < min) {
            val += range;
        }
        return val;
    }

    public static int sign(float x) {
        return x < 0 ? -1 : (x > 0 ? 1 : 0);
    }

    public static int sign(int x) {
        return x < 0 ? -1 : (x > 0 ? 1 : 0);
    }
    
    /**
     * Take a float input and clamp it between min and max.
     * 
     * @param input
     * @param min
     * @param max
     * @return clamped input
     */
    public static float clamp(float input, float min, float max) {
        return (input < min) ? min : (input > max) ? max : input;
    }
    
    /**
     * Calculate the mathematical modulo with float values. The Java % operator is
     * the mathematical remainder, this method calculates the proper 
     * mathematical modulus. The result of Java % operator and this function only
     * differs for negative values.
     * 
     * @param dividend
     * @param divisor
     * @return modulus
     */
    public static float mod(float dividend, float divisor) {
        float i=dividend % divisor;
        if( i * divisor  < 0 )
          i+= divisor;
        return i;
    }
    
    /**
     * Calculate the mathematical modulo with float values. The Java % operator is
     * the mathematical remainder, this method calculates the proper 
     * mathematical modulus. The result of Java % operator and this function only
     * differs for negative values.
     * 
     * @param dividend
     * @param divisor
     * @return modulus
     */
   
    public static int mod(int dividend, int divisor) {
        int i=dividend % divisor;
        if(dividend < 0)
            i+= divisor;
        return i;
    }
    
    public static int modExact(int dividend, int divisor) {
        if(divisor==0)
            return dividend;
        int i=dividend % divisor;
        if( i * (long)divisor  < 0 )
          i+= divisor;
        return i;
    }
    
    public static float modExcat(float dividend, float divisor) {
        if(divisor==0)
            return dividend;
        float i=dividend % divisor;
        if( i * divisor  < 0 )
          i+= divisor;
        return i;
    }
    
    /**
     * Converts a single precision (32 bit) floating point value
     * into half precision (16 bit).
     *
     * <p>Source: <a href="ftp://www.fox-toolkit.org/pub/fasthalffloatconversion.pdf</a>
     *
     * @param half The half floating point value as a short.
     * @return floating point value of the half.
     */
    public static float convertHalfToFloat(short half) {
        switch ((int) half) {
            case 0x0000:
                return 0f;
            case 0x8000:
                return -0f;
            case 0x7c00:
                return Float.POSITIVE_INFINITY;
            case 0xfc00:
                return Float.NEGATIVE_INFINITY;
            // TODO: Support for NaN?
            default:
                return Float.intBitsToFloat(((half & 0x8000) << 16)
                        | (((half & 0x7c00) + 0x1C000) << 13)
                        | ((half & 0x03FF) << 13));
        }
    }

    public static short convertFloatToHalf(float flt) {
        if (Float.isNaN(flt)) {
            throw new UnsupportedOperationException("NaN to half conversion not supported!");
        } else if (flt == Float.POSITIVE_INFINITY) {
            return (short) 0x7c00;
        } else if (flt == Float.NEGATIVE_INFINITY) {
            return (short) 0xfc00;
        } else if (flt == 0f) {
            return (short) 0x0000;
        } else if (flt == -0f) {
            return (short) 0x8000;
        } else if (flt > 65504f) {
            // max value supported by half float
            return 0x7bff;
        } else if (flt < -65504f) {
            return (short) (0x7bff | 0x8000);
        } else if (flt > 0f && flt < 3.054738E-5f) {
            return 0x0001;
        } else if (flt < 0f && flt > -3.054738E-5f) {
            return (short) 0x8001;
        }

        int f = Float.floatToIntBits(flt);
        return (short) (((f >> 16) & 0x8000)
                | ((((f & 0x7f800000) - 0x38000000) >> 13) & 0x7c00)
                | ((f >> 13) & 0x03ff));
    }

    public static float min(float a, float b) {
        return a<b?a:b;
    }

    public static float max(float a, float b) {
        return a>b?a:b;
    }
    
    public static final double nanoTimeToSeconds(long nanoTime) {
        return (double)nanoTime / 1000000000.0;
    }
    
    public static final double nanoTimeToMilis(long nanoTime) {
        return (double)nanoTime / 1000000.0;
    }
    
    public static final double milisToSeconds(long nanoTime) {
        return (double)nanoTime / 1000.0;
    }
    
    public static boolean intersectRange(float x1, float r1, float x2, float r2) {
        if(x1+r1<x2-r2)
            return false;
        if(x1-r2>x2+r2)
            return false;
        return true;
    }
    
    public static final float frac(float value) {
        return value - (int)value;
    }
}
