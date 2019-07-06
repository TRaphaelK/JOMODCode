/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Random;
import jb2.math.BoundingBox;
import jb2.xdel.MortonTreeInt;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.IntList;
import jb2.xdel.MortonTreeLong;

/**
 *
 * @author vear
 */
public class Test05MortonTreeLong {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        long passedTime = System.currentTimeMillis();
        
        int numvectors = 1000;
        int numbbs = 10;
        int guaranteedhittest = 200;
        int randomtest = 500;
        float tests = guaranteedhittest + randomtest;
        
        Random r = new Random(12345678);
        float xmin = (r.nextFloat()-0.5f) * 2000f;
        float xrange = 500 + r.nextFloat() * 2000f;
        
        float ymin = (r.nextFloat()-0.5f) * 10f;
        float yrange = 150 + (r.nextFloat() * 100f);
        
        float zmin = (r.nextFloat()-0.5f) * 2000f;
        float zrange = 500+ r.nextFloat() * 2000f;
        
        MortonTreeLong tree = new MortonTreeLong(xmin, xmin+xrange, ymin, ymin+yrange, zmin, zmin+zrange);
        tree.clear();
        
        Vector3f[] vectors = new Vector3f[numvectors];
        BoundingBox[] bbs = new BoundingBox[numbbs];
        
        // add 450 objects, then 50 object bounds
        for(int i=0; i<numvectors; i++) {
            
            vectors[i] = new Vector3f();
            
            vectors[i].x = xmin+r.nextFloat()*xrange;
            if(i%50==0) {
                // only every 50'th objext is in air
                vectors[i].y = ymin+r.nextFloat()*yrange;
            } else {
                // typically objects are on the ground
                vectors[i].y = ymin+r.nextFloat()*(yrange/5);
            }
            vectors[i].z = zmin+r.nextFloat()*zrange;
            
            tree.add(vectors[i], i);
        }
        
        // add bounding boxes
        for(int i=0; i<numbbs; i++) {
            
            BoundingBox bb = new BoundingBox();
            bbs[i] = bb;
            bb.extents.x = 10 + r.nextFloat()*50;
            bb.extents.y = 10 + r.nextFloat()*50;
            bb.extents.z = 10 + r.nextFloat()*50;
            
            bb.center.x = xmin+bb.extents.x+r.nextFloat()*(xrange-2*bb.extents.x);
            bb.center.y = ymin+bb.extents.y+r.nextFloat()*(yrange-2*bb.extents.y);
            bb.center.z = zmin+bb.extents.z+r.nextFloat()*(zrange-2*bb.extents.z);
            
            int idx = i+numvectors;
            tree.add(bb, idx);
        }
        tree.build();
        
        IntList results = new IntList();
        FastList<BoundingBox> boxes = new FastList<BoundingBox>();
        // create random testing bounding boxes
        for(int i=0; i<randomtest; i++) {
            // generate random bb, only every 5'th has height, the others are full y range
            BoundingBox bb = new BoundingBox();
            bb.extents.x = 10 + r.nextFloat()*50;
            if(i%5==0) {
                bb.extents.y = 10 + r.nextFloat()*50;
                bb.center.y = ymin+bb.extents.y+r.nextFloat()*(yrange-2*bb.extents.y);
            } else {
                // full height box, for horizontal selection
                bb.extents.y = yrange/2;
                bb.center.y = ymin + yrange/2;
            }
            bb.extents.z = 10 + r.nextFloat()*50;
            
            bb.center.x = xmin+bb.extents.x+r.nextFloat()*(xrange-2*bb.extents.x);
            bb.center.z = zmin+bb.extents.z+r.nextFloat()*(zrange-2*bb.extents.z);
            boxes.add(bb);
        }
        // create some boxes around known points
        for(int i=0; i<guaranteedhittest; i++) {
            int veci = r.nextInt(numvectors);
            BoundingBox bb = new BoundingBox(vectors[veci], 10,10,10);
            boxes.add(bb);
        }
            int good=0;
            int bad=0;
            int mistake=0;
        for(int i=0; i<boxes.size(); i++) {
            BoundingBox bb = boxes.get(i);
            // find indices from the tree
            results = tree.getColliding(bb, results);

            // find the actual results by brute-force check
            // all brute force checks should be contained in the tree search
            // but the tree search could return values that are not matches
            // make a stat of how many false results we got
            // also make a stat of how much time both evaluations took
            for(int j=0; j<numvectors; j++) {
                if(bb.contains(vectors[j])) {
                    // check if teh resutls contains the index
                    if(results.contains(j)) {
                        good++;
                    } else {
                        bad++;
                        results = tree.getColliding(bb, results);
                    }
                } else {
                    if(results.contains(j)) {
                        mistake++;
                    }
                }
            }
            //System.out.printf("Good %d, mistake %d, bad %d\n", good, mistake, bad);
        }
        
        passedTime = System.currentTimeMillis() - passedTime;
        System.out.printf("Good %f, mistake %f, bad %f\n", good/tests, mistake/tests, bad/tests);
        System.out.printf("Passed time %f sec\n", (float)(passedTime/1000f));
        // TODO: test with bounding boxes too
    }
}
