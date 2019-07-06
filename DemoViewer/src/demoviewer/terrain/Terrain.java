/*
 * Terrain.java
 *
 * Created on 2006. február 14., 19:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import com.jme.bounding.BoundingVolume;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

/**
 *
 * @author vear
 */
public interface Terrain {
    // terrain types
    // TerrainBlock classical terrain block
    public static final int BLOCK = 1;
    // TerrainBlock terrain block using clod
    public static final int CLOD = 2;
    // TerrainPage terrain page with fixed quadtree
    public static final int PAGE = 4;
    // SharedTerrainBlock for sharing blocks
    public static final int SHARED = 8;
    // MorphingTerrainBlock using the vertex morphing shader
    public static final int MORPH = 16;
    // SectoredTerrain manager for repeating terrain
    public static final int SECTORED = 32;

    // flag for the terrain componenet, that this is the root
    // for getting height
    public static final int ROOT = 128;
    
    public float getHeightFromWorld(Vector3f loc);

    float getHeight(float newX, float newZ);
    /*
     * Returns the type of the terrain system item
     */
    int getTerrainType();

    int getQuadrant();

    void setQuadrant(int i);

    int getSize();
    
    int getU();
    
    int getV();
    
    int getType();

    void updateLod(Camera cam);
}
