/*
 * Config.java
 *
 * Created on 2006. február 12., 0:45
 *
 * Stores configuration parameters
 */

package demoviewer;

import com.jme.math.Vector3f;
import demoviewer.terrain.MorphingTerrainShaderCreator;

/**
 *
 * @author vear
 */
public class Config {
    
    // main scaling factor
    public static float mainScale=4f;

    // detail level for terrain
    // use flat shading
    public static int terrainRenderMode=MorphingTerrainShaderCreator.LIGHTING_VERTEX;//.LIGHTING_NORMALMAP3;//.LIGHTING_FLAT;//
    // use terrain morphing?
    public static boolean useMorphing=false;
    // use detail colors?
    //public static boolean useDetailColors=true;
    // the detail color vanishing distance (-1 detail always visible, 0-details are off)
    public static float terrainDetailDistance=0;//128f*mainScale;//0;//
        
    // the distance under which models are shown (this is max visibility distance)
    public static float modelSightdistance=1024f*mainScale;
    // decrease the number of model lods by this number (always remains 1)
    public static int modelLods=2;
    // shader used to render models
    public static int modelRender=0; // 0-FF, 1-phong shader
    
    // heightmap sector size 65,129,257,513 (original would be 513)
    public static int sectorSize=129;
    // tile (block) cell size 16,32,64,128,256,512 (+1 for vertex row size)
    public static int cellSize=64;
    
    public static float origSectorsize=512;
    
    
    public static float terrainScaleMult=(129f/(float)sectorSize)*(origSectorsize/(float)(sectorSize-1));
    // the clipping distance for terrain
    public static float terrainSightDistance=4000*terrainScaleMult;
    
    // scales
    public static Vector3f terrainScale = new Vector3f(mainScale*terrainScaleMult,mainScale*terrainScaleMult/1024f,mainScale*terrainScaleMult);
    public static Vector3f modelScale=new Vector3f(mainScale,mainScale,mainScale);
    public static Vector3f modelTransScale=new Vector3f(mainScale,mainScale,mainScale);

    // compression for textures
    // can be 0,1,2,3 (none, DXT1,DXT3,DXT5)
    public static int textureCompression=3;
    // compression for normal-map
    public static int normalMapCompression=3;
    // size of the normal map texture
    public static int normalMapSize=1024;
    
    // do we remove old (unused block shares)
    public static boolean removeOldShares=false;
    
    // resourcemanager params
    // delete extracted/decompressed resources which are already prepared
    public static boolean deleteProcessed=false;

    public static int normalMapFormat=(terrainRenderMode==MorphingTerrainShaderCreator.LIGHTING_NORMALMAP2?2:3);
            
    /** Creates a new instance of Config */
    public Config() {
    }
    
}
