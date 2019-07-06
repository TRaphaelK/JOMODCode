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

/**
 *
 * @author vear
 */
public class Test08MortonTree {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        long start = System.currentTimeMillis();
        
        int numvectors = 1000;
        //int numbbs = 10;
        int guaranteedhittest = 200;
        int randomtest = 500;
        boolean do2d = true;
        
        float tests = guaranteedhittest + randomtest;
        
        Random r = new Random(12345678);
        float xmin = (r.nextFloat()-0.5f) * 2000f;
        float xrange = 500 + r.nextFloat() * 2000f;
        
        float ymin = (r.nextFloat()-0.5f) * 10f;
        float yrange = 150 + (r.nextFloat() * 100f);
        
        float zmin = (r.nextFloat()-0.5f) * 2000f;
        float zrange = 500+ r.nextFloat() * 2000f;
        
        MortonTreeInt tree = new MortonTreeInt(xmin, xmin+xrange, ymin, ymin+yrange, zmin, zmin+zrange);
        tree.clear();
        
        Vector3f[] vectors = new Vector3f[numvectors];
        //BoundingBox[] bbs = new BoundingBox[numbbs];
        
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
            int idx = i + 1;
            tree.put(vectors[i], idx);
        }
        
        /*
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
        */
        
        tree.build();
        long creation = System.currentTimeMillis() - start;
        
        FastList<IntList> correct = new FastList<>();
        FastList<BoundingBox> boxes = new FastList<>();
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
        // precompute the correct results
        for(int i=0; i<boxes.size(); i++) {
            BoundingBox bb = boxes.get(i);
            IntList result = new IntList();
            for(int j=0; j<numvectors; j++) {
                int idx = j+1;
                if(do2d) {
                    if(bb.contains2d(vectors[j])) {
                        result.add(idx);
                    }                    
                } else {
                    if(bb.contains(vectors[j])) {
                        result.add(idx);
                    }
                }
            }
            correct.set(i, result);
        }
        
        start = System.currentTimeMillis();
        int sumgood=0;
        int sumbad=0;
        int summistake=0;
        IntList candidates=new IntList();
        IntList results=new IntList();
        
        for(int i=0; i<boxes.size(); i++) {
            BoundingBox bb = boxes.get(i);
            // find indices from the tree
            candidates.clear();
            if(do2d)
                candidates = tree.getContained2D(bb, candidates);
            else
                candidates = tree.getContained(bb, candidates);
            // second phase remove the mistakes
            results.clear();
            for(int j=0; j<candidates.size(); j++) {
                int idx = candidates.get(j);
                //if(bb.contains(vectors[idx-1])) {
                    results.add(idx);
                //}
            }
            int good=0;
            int bad=0;
            int mistake=0;
            // check against correct results
            IntList goods = correct.get(i);
            
            for(int j=0; j<goods.size(); j++) {
                int idx = goods.get(j);
                int found = results.indexOf(idx);
                
                if(found>-1) {
                    good++;
                    results.removeElementAt(found);
                } else {
                    bad++;
                }
            }
            // if something remains in results, its a mistake
            mistake+= results.size();
            
            if(mistake>500 ||bad>0) {//
                //System.out.printf("Good %d, mistake %d, bad %d\n", good, mistake, bad);
                // get the results again, for testing
                //results.clear();
                //results = tree.getContained(bb, results);
            }
            sumgood+=good;
            summistake+=mistake;
            sumbad+=bad;
        }
        
        long testime = System.currentTimeMillis() - start;
        System.out.printf("Good %f, mistake %f, bad %f\n", sumgood/tests, summistake/tests, sumbad/tests);
        System.out.printf("Construction time time %f sec\n", (float)(creation/1000f));
        System.out.printf("Test time time %f sec\n", (float)(testime/1000f));
    }
}
