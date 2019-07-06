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
package test;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.map.NavGrid;
import jb2.math.BoundingBox;
import jb2.util.FastList;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class Test19NavGRid {

    protected static final Logger log = Logger.getLogger(Test19NavGRid.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        LocalContext.getContext();

        Random r = new Random(12345678);

        int numvectors = 1000;

        long start = System.currentTimeMillis();

        float xmin = -1000f;
        float xrange = 2000f;

        float ymin = -10f;
        float yrange = 40f;

        float zmin = -1000f;
        float zrange = 2000f;

        NavGrid tree = new NavGrid();

        //IntMap vecMap = new IntMap();
        BoundingBox[] bounds = new BoundingBox[numvectors];

        // add 450 objects, then 50 object bounds
        for (int i = 0; i < numvectors; i++) {
            bounds[i] = new BoundingBox();

            bounds[i].center.x = xmin + r.nextFloat() * xrange;
            bounds[i].center.z = zmin + r.nextFloat() * zrange;
            if (i % 50 == 0) {
                // only every 50'th objext is in air
                bounds[i].center.y = ymin + r.nextFloat() * yrange;
            } else {
                // typically objects are on the ground
                bounds[i].center.y = ymin + r.nextFloat() * (yrange / 5);
            }

            tree.checkMapRanges(bounds[i].center);
        }

        long creation = System.currentTimeMillis() - start;
        long estimategen = System.currentTimeMillis();
        
        tree.generateEstimateMap(new FastList<>());
        estimategen = System.currentTimeMillis() - estimategen;
        
        log.log(Level.INFO, "Creation time {0} ms", creation);
        log.log(Level.INFO, "Estimate map {0} ms", estimategen);
    }
}
