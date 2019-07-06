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
package test;

import jb2.math.AngleArea;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.math.Vector2f;

/**
 *
 * @author vear
 */
public class Test20AngleArea {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // construct at 2
        Vector2f point = new Vector2f(0,2);
        AngleArea a = createArea("A1", point, 0, 10, 1);        
        AngleArea a2 = createArea("A2", point, 1, -10, 1);
        boolean hascommon = a.shrink(a2);
        System.out.println("A1 and A2 has common (should be false): "+hascommon);
        
        AngleArea a3 = createArea("A3", point, 2, 20, 1);
        hascommon = a.shrink(a3);
        System.out.println("A1 and A3 has common (should be true): "+hascommon);
        System.out.println("A1 and A3 common: min: "+a.min+" max: "+a.max);
        
        Vector2f result = new Vector2f();
        result.fromHeading(a.min);
        System.out.println("Vector min: "+result);
        result.fromHeading(a.max);
        System.out.println("Vector max: "+result);

        result.fromHeading(-FastMath.PI);
        System.out.println("Vector -down: "+result);
        result.fromHeading(-FastMath.PI/2f);
        System.out.println("Vector -left: "+result);
        result.fromHeading(0);
        System.out.println("Vector up: "+result);
        result.fromHeading(FastMath.PI/2f);
        System.out.println("Vector right: "+result);
        result.fromHeading(FastMath.PI);
        System.out.println("Vector down: "+result);
        result.fromHeading(3f/2f*FastMath.PI);
        System.out.println("Vector left: "+result);
        result.fromHeading(2f*FastMath.PI);
        System.out.println("Vector +up: "+result);
    }

    public static AngleArea createArea(String name, Vector2f point, float x, float z, float extent) {
        BoundingBox bb = new BoundingBox();
        bb.center.set(x,0,z);
        bb.extents.set(extent,extent,extent);
        AngleArea a = new AngleArea();
        a.boxAngleFromPoint(point, bb);
        System.out.println(name+" min: "+a.min+" max: "+a.max);
        return a;
    }
}
