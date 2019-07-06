/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Random;
import jb2.ent.Entity;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.IntList;
import jb2.util.IntMap;
import jb2.util.LocalContext;
import jb2.xdel.OctreeInt;

/**
 *
 * @author vear
 */
public class Test13Octree {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LocalContext.getContext();
        long start = System.currentTimeMillis();
        
        int numvectors = 1000;
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
        
        OctreeInt tree = new OctreeInt(xmin, xmin+xrange, ymin, ymin+yrange, zmin, zmin+zrange);
        tree.clear();
        
        //IntMap vecMap = new IntMap();
                
        Entity[] vectors = new Entity[numvectors];
        //IntMap vecMap = new IntMap();
        
        // add 450 objects, then 50 object bounds
        for(int i=0; i<numvectors; i++) {
            int idx = i + 1;
            
            vectors[i] = new Entity(idx);
            
            vectors[i].position.x = xmin+r.nextFloat()*xrange;
            if(i%50==0) {
                // only every 50'th objext is in air
                vectors[i].position.y = ymin+r.nextFloat()*yrange;
            } else {
                // typically objects are on the ground
                vectors[i].position.y = ymin+r.nextFloat()*(yrange/5);
            }
            vectors[i].position.z = zmin+r.nextFloat()*zrange;
            
            tree.add(vectors[i].position, idx);
            
            //vecMap.put(idx, vectors[i]);
        }
        
        long creation = System.currentTimeMillis() - start;
        
        
        // skip test data preparation, and precompute the correct results
        FastList<IntList> correct = new FastList<IntList>();
        FastList<IntList> results = new FastList<IntList>();
        //FastList<FastList<Vector3f>> entResults = new FastList<>();
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
            BoundingBox bb = new BoundingBox(vectors[veci].position, 10,10,10);
            boxes.add(bb);
        }
        // precompute the correct results
        for(int i=0; i<boxes.size(); i++) {
            BoundingBox bb = boxes.get(i);
            IntList result = new IntList();
            for(int j=0; j<numvectors; j++) {
                int idx = j+1;
                if(bb.contains(vectors[j].position)) {
                    result.add(idx);
                }
            }
            correct.set(i, result);
        }
                
        results.clear();
        
        start = System.currentTimeMillis();
        for(int i=0; i<boxes.size(); i++) {
            BoundingBox bb = boxes.get(i);
            IntList result=new IntList();
            // find indices from the tree
            result = tree.getContained(bb, result);
            results.set(i, result);
            /*
            FastList<Vector3f> entResult = new FastList<>();
            
            for(int j=0; j<result.size(); j++) {
                Vector3f vec = (Vector3f) vecMap.get(result.get(j));
                entResult.add(vec);
            }
            entResults.set(i, entResult);
            */
        }
        long testime = System.currentTimeMillis() - start;
        
        int sumgood=0;
        int sumbad=0;
        int summistake=0;
        
        for(int i=0; i<boxes.size(); i++) {
            
            IntList result=results.get(i);
            //FastList<Vector3f> entResult=entResults.get(i);
            
            int good=0;
            int bad=0;
            int mistake=0;
            // check against correct results
            IntList goods = correct.get(i);
            
            for(int j=0; j<goods.size(); j++) {
                int idx = goods.get(j);
                int found = result.indexOf(idx);
                
                //Entity ent = (Entity) vecMap.get(idx);
                
                if(found>-1 ) {
                    good++;
                    result.removeElementAt(found);
                    //entResult.remove(found);
                } else {
                    bad++;
                }
            }
            // if something remains in results, its a mistake
            mistake+= result.size();
            
            if(mistake>0 ||bad>0) {
                System.out.printf("Good %d, mistake %d, bad %d\n", good, mistake, bad);
            }
            sumgood+=good;
            summistake+=mistake;
            sumbad+=bad;
        }
        
        
        System.out.printf("Good %f, mistake %f, bad %f\n", sumgood/tests, summistake/tests, sumbad/tests);
        System.out.printf("Construction time time %f sec\n", (float)(creation/1000f));
        System.out.printf("Test time time %f sec\n", (float)(testime/1000f));
    }
}
