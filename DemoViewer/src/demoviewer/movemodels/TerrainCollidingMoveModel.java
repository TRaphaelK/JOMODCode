/*
 * TerrainCollidingMoveModel.java
 *
 * Created on 2006. február 14., 22:55
 *
 * Model which supports colliding with terrain
 */

package demoviewer.movemodels;

import com.jme.math.Vector3f;
import demoviewer.terrain.Terrain;

/**
 *
 * @author vear
 */
public class TerrainCollidingMoveModel extends MoveConstraint {
    
    // the terrain we are traversing
    private Terrain t;
    // the height of camera above the terrain
    private float objheight;
    
    /** Creates a new instance of TerrainCollidingMoveModel */
    public TerrainCollidingMoveModel(Terrain t, float objheight) {
        this.t=t;
        this.objheight=objheight;
    }
    
    // returns the position resulting when trying to go
    // from prev to loc
    // always returns the fixed loc
    public Vector3f isMoveAllowed(Vector3f prev, Vector3f loc) {
        // check height at prev
        float h =Float.NaN;
        if(t!=null) {
            // check terrain height at location
            h = t.getHeightFromWorld(loc);
            if(!Float.isNaN(h)) {
                // if height is NaN, allow movement
                // if height is more than in loc, dont allow
                if(loc.y<h+objheight) {
                    loc.y=h+objheight;
                }
            }
            
        }
           
        return loc;
    }
}
