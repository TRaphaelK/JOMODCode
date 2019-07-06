/*
 * FootMoveModel.java
 *
 * Created on 2006. február 14., 22:49
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.movemodels;

import com.jme.math.Vector3f;
import demoviewer.terrain.Terrain;

/**
 *
 * @author vear
 */
public class FootMoveModel extends MoveConstraint {
    
    // the height of camera above the terrain
    private float objheight;
    // the terrain we are traversing
    private Terrain t;
    
    /** Creates a new instance of FootMoveModel */
    public FootMoveModel(Terrain t, float objheight) {
        this.t=t;
        this.objheight=objheight;
    }
    
    /*
     * Returns is movement to a location is allowed
     */
    // returns the position resulting when trying to go
    // from prev to loc
    // always returns the fixed loc
    public Vector3f isMoveAllowed(Vector3f prev, Vector3f loc) {
        // check height at prev
        //float ph=t.getHeightFromWorld(prev);
        //if(!Float.isNaN(ph)) {
            // check terrain height at location
            float h = t.getHeightFromWorld(loc);
            if(!Float.isNaN(h)) {
                // if height is NaN, allow movement
                // if height is more than in loc, dont allow
                if(loc.y<h+objheight) {
                    loc.y=h+objheight;
                }
            }
            
        //}
           
        return loc;
    }
}
