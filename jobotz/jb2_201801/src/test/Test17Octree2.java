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
import jb2.math.BoundingBox;
import jb2.util.FastList;
import jb2.util.Octree;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class Test17Octree2 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        LocalContext.getContext();

        double creationavg = 0;
        double testavg = 0;
        int treenodes = 0;

        Random r = new Random(12345678);

        int sumgood = 0;
        int sumbad = 0;
        int summistake = 0;

        int numvectors = 1000;
        int guaranteedhittest = 200;
        int randomtest = 500;
        int tests = 1;

        for (int count = 0; count < tests; count++) {
            long start = System.currentTimeMillis();

            float xmin = (r.nextFloat() - 0.5f) * 2000f;
            float xrange = 500 + r.nextFloat() * 2000f;

            float ymin = (r.nextFloat() - 0.5f) * 10f;
            float yrange = 150 + (r.nextFloat() * 100f);

            float zmin = (r.nextFloat() - 0.5f) * 2000f;
            float zrange = 500 + r.nextFloat() * 2000f;

            Octree tree = new Octree();
            tree.clear();

            //IntMap vecMap = new IntMap();
            BoundingBox[] vectors = new BoundingBox[numvectors];

            // add 450 objects, then 50 object bounds
            for (int i = 0; i < numvectors; i++) {
                vectors[i] = new BoundingBox();

                vectors[i].center.x = xmin + r.nextFloat() * xrange;
                if (i % 50 == 0) {
                    // only every 50'th objext is in air
                    vectors[i].center.y = ymin + r.nextFloat() * yrange;
                } else {
                    // typically objects are on the ground
                    vectors[i].center.y = ymin + r.nextFloat() * (yrange / 5);
                }
                vectors[i].center.z = zmin + r.nextFloat() * zrange;
                vectors[i].extents.x = r.nextFloat() * 10f;
                vectors[i].extents.y = r.nextFloat() * 2f;
                vectors[i].extents.z = r.nextFloat() * 10f;

                tree.add(vectors[i], vectors[i]);
            }

            long creation = System.currentTimeMillis() - start;

            treenodes = tree.countNodes();

            // skip test data preparation, and precompute the correct results
            FastList<FastList<BoundingBox>> correct = new FastList<>();
            FastList<FastList<BoundingBox>> results = new FastList<>();
            //FastList<FastList<Vector3f>> entResults = new FastList<>();
            FastList<BoundingBox> boxes = new FastList<BoundingBox>();
            // create random testing bounding boxes
            for (int i = 0; i < randomtest; i++) {
                // generate random bb, only every 5'th has height, the others are full y range
                BoundingBox bb = new BoundingBox();
                bb.extents.x = 10 + r.nextFloat() * 50;
                if (i % 5 == 0) {
                    bb.extents.y = 10 + r.nextFloat() * 50;
                    bb.center.y = ymin + bb.extents.y + r.nextFloat() * (yrange - 2 * bb.extents.y);
                } else {
                    // full height box, for horizontal selection
                    bb.extents.y = yrange / 2;
                    bb.center.y = ymin + yrange / 2;
                }
                bb.extents.z = 10 + r.nextFloat() * 50;

                bb.center.x = xmin + bb.extents.x + r.nextFloat() * (xrange - 2 * bb.extents.x);
                bb.center.z = zmin + bb.extents.z + r.nextFloat() * (zrange - 2 * bb.extents.z);
                boxes.add(bb);
            }
            // create some boxes around known points
            for (int i = 0; i < guaranteedhittest; i++) {
                int veci = r.nextInt(numvectors);
                BoundingBox bb = new BoundingBox(vectors[veci].center, 10, 10, 10);
                boxes.add(bb);
            }
            // precompute the correct results
            for (int i = 0; i < boxes.size(); i++) {
                BoundingBox bb = boxes.get(i);
                FastList<BoundingBox> result = new FastList<>();
                for (int j = 0; j < numvectors; j++) {
                    if (bb.intersects(vectors[j])) {
                        result.add(vectors[j]);
                    }
                }
                correct.set(i, result);
            }

            results.clear();

            start = System.currentTimeMillis();
            for (int i = 0; i < boxes.size(); i++) {
                BoundingBox bb = boxes.get(i);
                FastList<BoundingBox> result = new FastList<>();
                // find indices from the tree
                result = tree.getIntersecting(bb, result);
                results.set(i, result);
            }
            long testime = System.currentTimeMillis() - start;

            for (int i = 0; i < boxes.size(); i++) {

                FastList<BoundingBox> result = results.get(i);
                //FastList<Vector3f> entResult=entResults.get(i);

                int good = 0;
                int bad = 0;
                int mistake = 0;
                // check against correct results
                FastList<BoundingBox> goods = correct.get(i);

                for (int j = 0; j < goods.size(); j++) {
                    BoundingBox ent = goods.get(j);
                    int found = result.indexOf(ent);

                    if (found > -1) {
                        good++;
                        result.remove(found);
                        //entResult.remove(found);
                    } else {
                        bad++;
                    }
                }
                // if something remains in results, its a mistake
                mistake += result.size();

                if (mistake > 0 || bad > 0) {
                    System.out.printf("Good %d, mistake %d, bad %d\n", good, mistake, bad);
                    // get the result again, for debugging
                    BoundingBox bb = boxes.get(i);
                    result = tree.getIntersecting(bb, result);
                }
                sumgood += good;
                summistake += mistake;
                sumbad += bad;
            }
            creationavg += creation;
            testavg += testime;
        }

        creationavg /= 1000d;

        testavg /= 1000d;

        System.out.printf("Good %f, mistake %f, bad %f\n", (float)sumgood / tests, (float)summistake / tests, (float)sumbad / tests);
        System.out.printf("Construction time time %f sec\n", creationavg);
        System.out.printf("Test time time %f sec\n", testavg);
        System.out.printf("Tree nodes %d\n", treenodes);
    }
}
