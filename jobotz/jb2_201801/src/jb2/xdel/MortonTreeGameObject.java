/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import jb2.xdel.MortonTreeInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.jo.GameObject;
import jb2.jo.GameObjectFactory;
import jb2.math.BoundingBox;
import jb2.util.FastList;
import jb2.util.IntList;

/**
 * Tree with morton int hashes and int values
 * @author vear
 */
public class MortonTreeGameObject {
    
    protected static final Logger log = Logger.getLogger(GameObjectFactory.class.getName());
    
    protected MortonTreeInt backer;
    
    protected IntList results;
    
    protected float xmin, xmax, ymin, ymax, zmin, zmax;
    protected boolean cleared;
    
    public MortonTreeGameObject(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.zmin = zmin;
        this.zmax = zmax;
        backer = new MortonTreeInt(xmin, xmax, ymin, ymax, zmin, zmax);
        cleared=true;
    }
    
    public boolean isCleared() {
        return cleared;
    }
    
    public void clear(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
        if(this.xmin != xmin
                || this.xmax != xmax
                || this.ymin != ymin
                || this.ymax != ymax
                || this.zmin != zmin
                || this.zmax != zmax
                ) {
            this.xmin = xmin;
            this.xmax = xmax;
            this.ymin = ymin;
            this.ymax = ymax;
            this.zmin = zmin;
            this.zmax = zmax;
            backer.clear(xmin, xmax, ymin, ymax, zmin, zmax);
            cleared=true;
        }
    }

    // TODO: commented out
    public void put(GameObject go) {
        /*
        // calculate new morton code for game object
        int morton = backer.getMortonCode(go.position);

        if(go.positionMorton != morton 
                || cleared==true) {
            //if(cleared==false) {
            //    log.log(Level.INFO, "Object "+go.name + ","+ go.SSN+","+ go.position.toString());
            //}

            // need to insert it
            go.positionMorton = morton;
            backer.put(morton, go.SSN);
        }
          */
    }

    public void remove(GameObject go) {
        backer.remove(go.SSN);
    }

    public void build() {
        if(cleared|| backer.getNumModifications() > 10) {
            // if cleared or the number of modification is at least 5% of the size
            //long rebuildtime=System.currentTimeMillis();
            backer.build();
            //rebuildtime = System.currentTimeMillis() -rebuildtime;
            //log.log(Level.INFO, "Morton tree rebuild time "+rebuildtime);
            cleared=false;
        }
    }
    

    public FastList<GameObject> getContained2D(BoundingBox bb, FastList<GameObject> store) {
        if(results!=null) {
            results.clear();
        }
        
        results = backer.getContained2D(bb, results);
        if(store==null) {
            store = new FastList<GameObject>(results.size);
        }
        
        for(int i=0; i<results.size; i++) {
            GameObject go = AppContext.gameObjectFactory.getObjectBySSN(results.get(i));
            // check if the game object actually is inside of the bounding box
            if(bb.contains2d(go.position)) {
                store.add(go);
            }
        }
        return store;
    }

    public FastList<GameObject> getContained(BoundingBox bb, FastList<GameObject> store) {
        if(results!=null) {
            results.clear();
        }
        
        results = backer.getContained(bb, results);
        if(store==null) {
            store = new FastList<GameObject>(results.size);
        }
        for(int i=0; i<results.size; i++) {
            GameObject go = AppContext.gameObjectFactory.getObjectBySSN(results.get(i));
            if(go==null)
                continue;
            // check if the game object actually is inside of the bounding box
            if(bb.contains(go.position)) {
                store.add(go);
            }
        }
        return store;
    }

}
