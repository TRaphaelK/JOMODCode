/*
 * SectoredTerrain2.java
 *
 * Created on 2006. április 2., 19:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.image.Texture;
import com.jme.intersection.CollisionResults;
import com.jme.intersection.PickResults;
import com.jme.math.FastMath;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.LoggingSystem;
import demoviewer.Config;
import demoviewer.resource.ResourceManager;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author vear
 */
public class SectoredTerrain2 extends Node implements Terrain {
    
    private Logger _log = LoggingSystem.getLogger();
    
    MorphingTerrainManager manager;
    
    // the block types
    HashMap<Integer, MorphingTerrainBlock> blocktypes=new HashMap<Integer,MorphingTerrainBlock>();

    // the actual positioned SharedTerrainBlock blocks for faster U,V access
    protected ConcurrentHashMap<Integer, SharedTerrainBlock> blocks;
    
// number of sectors loaded from TRN
    int sectorsize;
    // blocks per sector
    int bpsector=(Config.sectorSize-1)/Config.cellSize;
    // compositon of the block ID
    // bits for sector (0-4)
    int sectorBits=3;
    int sectorMask=(int)FastMath.pow(2,sectorBits)-1;
    // bits for block in the sector (log(bpsector*bpsector,2)) 4 bpsector=4
    int blockBits=(int)FastMath.log(bpsector*bpsector,2);
    int blockMask=(int)FastMath.pow(2,blockBits)-1;

    // left and up fixes each take another (sectorBits+blockBits)
    int fixBits=blockBits+sectorBits;
    int fixMask=(int)FastMath.pow(2,fixBits)-1;
    // page is shifted by blockBits+2*fixBits
    // block is shifted by 2*fixBits anded by blockMask
    // left fix is shifted by fixBits anded by fixMask
    // up fix is anded by fixMask
            
    // the full sector subdivided to the base blocks
    // info.sectorsize*4
    private int blockcount;
    
    
    // blocks*blocks int's
    private int blockmap[];
    
    
    // the heightmmap
    // heightmap 4*size*size ints, each sector as separate array
    private int[][] heightMap;

    Vector3f stepScale=Config.terrainScale;
    
    int sectorSize=Config.sectorSize;
    // size of the heightmap
    int totalSizei=sectorSize*2;
    
    // block size: 32
    int tileSize=Config.cellSize;
    // do wrap all in x and y direction
    boolean wrapx;
    boolean wrapy;
    String trn;
    
    // data for runtime visibility building
    protected Vector3f camLocation=new Vector3f();
    
    protected boolean needTileCheck;
    protected float cu;
    protected float cv;
    protected float ch;
    protected int indexU;
    protected int indexV;
    protected int indexH;
    // block size in world scale
    protected float blockSize;
    
    // has the terrain state changed
    boolean changed=true;
    
    // do use terrain cache?
    boolean usecache=false;
    
    // do a full check of the map field?
    private boolean fullcheck=true;
    
    // the page strucure holding SharedTerrainBlock nodes
    SectoredTerrainPage2 pg;

    private float sightDistance=Config.terrainSightDistance;
    
    // reference node for bounds, no blocks outside are created
    private Node boundRefNode;
    private Vector3f boxcent=new Vector3f();
    
    /** Creates a new instance of SectoredTerrain2 */
    public SectoredTerrain2(String trn) {
        super(trn);
        this.trn=trn;
    }
    
    public void buildTerrain(TerrainInfo inf) {
        // get the terrininfo for the terrain name
        if(inf==null) {
            inf=ResourceManager.getInstance().getTerrainInfo(trn);
        }
        // request the TRN definition
        if(inf==null) {
            _log.severe("Cannot build "+trn+" terrain, terraininfo not set");
            return;
        }
        if(Config.terrainScale.x!=Config.terrainScale.z) {
            _log.severe("Terrain scale x and z must match!");
            return;
        }
        
        manager=MorphingTerrainManager.getInstance();
        
        blockSize = tileSize * stepScale.x; //tileSize-1
                
        needTileCheck = true;

        pg=new SectoredTerrainPage2(this.name+"/");
        pg.setCullMode(Spatial.CULL_DYNAMIC);
        pg.setRenderQueueMode(Renderer.QUEUE_SKIP);
        this.attachChild(pg);
        this.setCullMode(Spatial.CULL_DYNAMIC);
        
        // get the heightmap
        createHeightMap(inf);
        // get the TRN
        createSectorDefinition(inf);
        // do the texturing/shader generation
        setRenderOptions(inf);
        // create the sectors
        //createSectors();
        // create blocks from the blockmap and heightmaps, making different blocks
        // based on how they border with others
        createSectoredBlockMap();
        
        // TODO foliage layer
        
        // initialize runtime info
        blocks = new ConcurrentHashMap<Integer, SharedTerrainBlock>();
        
        // reposition the terrain to the center
        float orx=inf.getOriginx()*(bpsector*blockSize);
        float orz=inf.getOriginy()*(bpsector*blockSize);
        // reposition by this many sectors
        
        this.setLocalTranslation(new Vector3f(orx,0,orz));
        setLocalScale(1);
        this.updateWorldBound();
        this.lockTransforms();
        
    }
    
    private void createHeightMap(TerrainInfo inf) {
        ResourceManager mgr=ResourceManager.getInstance();
        // okay here we start
        // get the heightmap
        String hgt=inf.getHeightfieldFile();
        // get the heightfield from the resource manager
        heightMap = mgr.getHeightMap(hgt);
        if(heightMap==null) {
            _log.severe("Cannot build terrain, cannot load heightmap");
            return;
        }
    }
    
    private void createSectorDefinition(TerrainInfo inf) {
        ResourceManager mgr=ResourceManager.getInstance();
        // create the blockmap
        this.sectorsize=inf.getSectorCount();
        blockcount = sectorsize*bpsector;
        blockmap = new int[blockcount*blockcount];
        //loop at the sectors in definition, and create the blockmap accordingly
        for(int i=0;i<sectorsize;i++)
            for(int j=0;j<sectorsize;j++) {
            int s=inf.getSector(j,i);
            for(int by=0;by<bpsector;by++)
                for(int bx=0;bx<bpsector;bx++) {
                    int bp=(i*bpsector+by)*blockcount+(j*bpsector+bx);
                    if(s!=0) {
                        blockmap[bp]=this.blockId(s,bx*bpsector+by);
                    } else {
                        blockmap[bp]=0;
                    }
                }
            }
        // set the wraps
        if(inf.getWrapx()==1) {
            this.wrapx=true;
        } else {
            this.wrapx=false;
        }
        if(inf.getWrapy()==1) {
            this.wrapy=true;
        } else {
            this.wrapy=false;
        }
        // go trough the blockmap the second time, putting in
        // left and up block references into block ids
        for(int by=0;by<blockcount;by++)
            for(int bx=0;bx<blockcount;bx++) {
                int id=blockmap[by*blockcount+bx];
                blockmap[by*blockcount+bx]=blockId(getPageFromID(id), getBlockFromID(id), getBlockTypeAt(bx-1, by), getBlockTypeAt(bx, by-1), getBlockTypeAt(bx-1, by-1));
            }
        
    }

    // creates the blocks not taking into consideration bordering
    // blocks
    private void createSectors() {
        for(int s=1;s<=4;s++) {
            // create the terrainblocks on this page
            createSector(s);
        }
    }

    private void createSector(int id) {
        // create a single page 128 in size
        // create 16 blocks of size 32x32 in size
        for(int i=0;i<bpsector*bpsector;i++) {
            int blid=blockId(id, i);
            MorphingTerrainBlock block=null;
            // create the detail lod of block
            /*
            if(usecache) {
                block=this.loadBlock(blid, detailLod);
            }
             */
            if(block==null) {
                block=createBlock(blid);
            }
            if(block!=null) {
                addBlock(blid, block);
            }
        }
    }
    
    /**
     * Creates the blocks from actual values in the blockmap
     */
    private void createSectoredBlockMap() {
        for(int by=0;by<blockcount;by++)
            for(int bx=0;bx<blockcount;bx++) {
                int blid=blockmap[by*blockcount+bx];
                if(blid!=0 && !blocktypes.containsKey(new Integer(blid))) {
                    MorphingTerrainBlock block=null;
                    // create the detail lod of block
                    /*
                    if(usecache) {
                        block=this.loadBlock(blid, detailLod);
                    }
                     */
                    if(block==null) {
                        block=createBlock(blid);
                    }
                    if(block!=null) {
                        addBlock(blid, block);
                    }
                }
            }
    }
    
    private int[] getHeightBlock(int blid) {
        int hbpg=this.getPageFromID(blid)-1;
        int bpx=getSectorHeightMapOffsetX(blid);
        int bpy=getSectorHeightMapOffsetY(blid);
        return TerrainUtils.createHeightSubBlock(heightMap[hbpg], bpx, bpy, tileSize+1);
    }
    
    private MorphingTerrainBlock createBlock(int blid) {
        // if lod =-1, create full detailed
        // otherwise lod is the size
        // if lod=1, then take the edges of the heightmap block
        int[] heightBlock=getHeightBlock(blid);
        
        String blname=getName()+"."+blid;
        
        // fix up the heightmap with left and up block border values
        int leftid=getBorderFromID(blid,1);
        if(leftid!=0) {
            // copy values from left blocks rightmost row into leftmost row
            int[] left=getHeightBlock(leftid);
            for(int rw=0;rw<=tileSize;rw++)
                heightBlock[rw*(tileSize+1)]=left[rw*(tileSize+1)+tileSize];
        }
        int upid=getBorderFromID(blid,2);
        if(upid!=0) {
            // copy values from up blocks bottom row into topmost row
            int[] up=getHeightBlock(upid);
            for(int cl=0;cl<=tileSize;cl++)
                heightBlock[cl]=up[tileSize*(tileSize+1)+cl];
        }
        
        int upleftid=getBorderFromID(blid,3);
        if(upleftid!=0) {
            int[] upleft=getHeightBlock(upleftid);
            // copy one value from upleft block bottom-right value to this blocks upper left
            heightBlock[0]=upleft[tileSize*(tileSize+1)+tileSize];
        }
        
        // offset of the block
        Vector2f offset=new Vector2f();
        int ofx=this.getTextureMapOffsetX(blid);
        int ofy=this.getTexturetMapOffsetY(blid);
        offset.x=(float)ofx*stepScale.x;
        offset.y=(float)ofy*stepScale.z;
        MorphingTerrainBlock block = new MorphingTerrainBlock(blname, tileSize+1, stepScale,
           heightBlock, new Vector3f(), totalSizei, offset, 0, Terrain.MORPH, manager);
        block.setModelBound(new BoundingBox());
        block.updateModelBound();
        
        // TODO: id needed?
        //block.setId(blid);
        // save it to the cache
        /*
        if(usecache) {
            block.write();
        }
         */
        return block;
    }
    
    private int getTextureMapOffsetX(int id) {
        int pgid=getPageFromID(id);
        int blid=getBlockFromID(id);
        return ((pgid-1)/2)*(totalSizei/2-1) + (blid/bpsector)*(tileSize);
    }
    
    private int getTexturetMapOffsetY(int id) {
        int pgid=getPageFromID(id);
        int blid=getBlockFromID(id);
        return ((pgid-1)%2)*(totalSizei/2-1) + (blid%bpsector)*(tileSize);
    }
    
    private int getSectorHeightMapOffsetX(int id) {
        int blid=getBlockFromID(id);
        return ((blid/bpsector)*tileSize);
    }
    
    private int getSectorHeightMapOffsetY(int id) {
        int blid=getBlockFromID(id);
        return ((blid%bpsector)*tileSize);
    }
    
    
    public int blockId(int page, int block) {
        if(page==0) return 0;
        return (page&sectorMask) | ((block&blockMask)<<(sectorBits));
    }
    
    public int blockId(int page, int block, int left, int up, int upleft) {
        if(page==0) return 0;
        return blockId(page, block) | ((left&fixMask)<<fixBits) | ((up&fixMask)<<(fixBits*2)) | ((upleft&fixMask)<<(fixBits*3));
    }
    
    public int getPageFromID(int id) {
        return id&sectorMask;
    }
    
    public int getBlockFromID(int id) {
        return (id>>>(sectorBits))&blockMask;
    }
    
    /**
     *  Gets a borders block ID from the ID (1-left, 2-up, 3-upleft)
     */
    public int getBorderFromID(int id, int border) {
        return (id>>>(fixBits*border))&fixMask;
    }
    
    private void addBlock(int id, MorphingTerrainBlock blk) {
        setBlockProperties(blk);
        blocktypes.put(new Integer(id), blk);
    }
    
    private MorphingTerrainBlock getBlock(int block) {
        if(block==0) return null;
        return blocktypes.get(new Integer(block));
    }
    
    private void setBlockProperties(Geometry pg) {
        pg.setModelBound(new BoundingBox());
        pg.updateModelBound();
        pg.setLocalScale(1);
        pg.setCullMode(Spatial.CULL_DYNAMIC);
        pg.setRenderQueueMode(Renderer.QUEUE_SKIP);
    }
    
    private void setRenderOptions(TerrainInfo inf) {
        ResourceManager mgr=ResourceManager.getInstance();
        MorphingTerrainManager mmgr=MorphingTerrainManager.getInstance();
        // set propertyes for terrain creation
        // only one shader all the way
        mmgr.setDynamicShaderSwitch(false);
        mmgr.setSortVertices(true);
        // single shader for all
        mmgr.setMaxShaderTypes(1);
        // nvidia compatible
        mmgr.setUseCompiledMorphData(true);
        mmgr.setUseMultipleVertexBuffers(false);
        mmgr.setUseStrips(false);
        mmgr.setUseVBO(true);
        mmgr.setUsePerBlockShaders(false);
        // set propertyes for shader creation
        MorphingTerrainShaderCreator mtsc=mmgr.getShaderCreator();
        // we decide shader creation
        mtsc.setUsePredefinedShaders(false);
        mtsc.setBlendNormals(false);
        
        // reset all settings
        mtsc.setUseColormap(-1);
        mtsc.setUseNormalMap(-1);
        // TODO flat shaded
        // TODO blends
        mtsc.setBlendmap(-1);
        mtsc.setUseMorphing(false);
        // use nvidia compatible morph data (passed trough color buffer)
        mtsc.setUseCompiledData(true);
        mtsc.setLightingMode(Config.terrainRenderMode);
        Texture t0;// = new Texture(1f);
        // load the colormap
        t0=mgr.getTexture(inf.getColormap(), true);
        if(t0!=null) {
            // texture the terrain
            DisplaySystem display=DisplaySystem.getDisplaySystem();
            TextureState ts = display.getRenderer().createTextureState();
            ts.setEnabled(true);
            // prepare the colormap
            setTexturePropertyes(t0);
            ts.setTexture(t0, 0);
            // we have colormap
            mtsc.setUseColormap(0);
            mtsc.setColorMapTiling(1f);
            pg.setRenderState(ts);
            int currentTexture=1;
            
            // do we support/need a shader?
            GLSLShaderObjectsState so = display.getRenderer().createGLSLShaderObjectsState();
            if(so.isSupported() &&
                    (
                    Config.terrainRenderMode>3
                    || Config.useMorphing
                    || Config.terrainDetailDistance!=0
                    )) {
                // can we switch on normal mapping?
                if( TextureState.getNumberOfFragmentUnits()>=2 
                  && ( Config.terrainRenderMode==MorphingTerrainShaderCreator.LIGHTING_NORMALMAP3 
                    || Config.terrainRenderMode==MorphingTerrainShaderCreator.LIGHTING_NORMALMAP2 ) ) {
                    // prepare the normal map
                    Texture tn=mgr.getNormalMap(inf.getHeightfieldFile());
                    if(tn!=null) {
                        // got a normal map
                        ts.setTexture(tn, currentTexture);
                        // set in shader
                        mtsc.setUseNormalMap(currentTexture);
                        currentTexture++;
                    }
                }

                // can we switch on morphing?
                if( Config.useMorphing) {
                    mtsc.setUseMorphing(true);
                }

                if(Config.terrainDetailDistance!=0 ) {
                    // supports shaders
                    int reqTex=1;
                    String blend=inf.getDetailblendmap();
                    String[] dc=new String[3];
                    dc[0]=inf.getDetailmapC1(); if(dc[0]!=null) reqTex++;
                    dc[1]=inf.getDetailmapC2(); if(dc[1]!=null) reqTex++;
                    dc[2]=inf.getDetailmapC3(); if(dc[2]!=null) reqTex++;
                    // if we have ready the four textures for detail texturing
                    // supports enugh texture units for doing nice terrain
                    if( TextureState.getNumberOfFragmentUnits()>currentTexture+reqTex && blend!=null && (dc[0]!=null || dc[1]!=null || dc[2]!=null)) {
                        // get the blend texture
                        Texture t1=mgr.getTexture(blend, true);
                        setTexturePropertyes(t1);
                        ts.setTexture(t1, currentTexture);
                        mtsc.setBlendmap(currentTexture);
                        currentTexture++;

                        Texture[] t=new Texture[3];
                        int numdetails=0;
                        for(int i=0;i<dc.length;i++) {
                            if(dc[i]!=null) t[i]= mgr.getTexture(dc[i], false);
                            if(t[i]!=null) {
                                numdetails=i+1;
                                mtsc.setDetailsCount(numdetails);
                                setTexturePropertyes(t[i]);
                                t[i].setWrap(Texture.WM_WRAP_S_WRAP_T);
                                ts.setTexture(t[i], currentTexture);
                                mtsc.setDetail(i, currentTexture);
                                currentTexture++;
                            }
                        }
                        // detail tiling ratio
                        // TODO it depends on total terrain size
                        mtsc.setDetailTiling(32*Config.mainScale);
                        mtsc.setColorToDetailRatio(0.7f);
                        mtsc.setDetailBlendPattern(false);
                        mtsc.setDetailDistance(Config.terrainDetailDistance);
                    }
                }
                // if our parent has fog enabled, enable shader fog
                if(getParent()!=null && getParent().getRenderState(RenderState.RS_FOG).isEnabled()) {
                    mtsc.setUseFog(true);
                } else mtsc.setUseFog(false);
                // create the shader, and set it
                so=mtsc.createShader();
                pg.setRenderState(so);
                mmgr.setUsePerBlockShaders(true);
                mmgr.createShaders();
            }
            ts.apply();
            // create material for lighting
            MaterialState m = display.getRenderer().createMaterialState();
            m.setAmbient(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
            m.setDiffuse(new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f));
            pg.setRenderState(m);
            ZBufferState zbs=display.getRenderer().createZBufferState();
            zbs.setEnabled(true);
            zbs.setWritable(true);
            zbs.setFunction(ZBufferState.CF_LEQUAL);
            pg.setRenderState(zbs);
        }
        
    }
    
    private void setTexturePropertyes(Texture t1) {
        t1.setCorrection(Texture.CM_PERSPECTIVE);
        t1.setFilter(Texture.FM_LINEAR);
        t1.setMipmapState(Texture.MM_LINEAR_LINEAR);
        t1.setApply(Texture.AM_COMBINE);
        t1.setCombineFuncRGB(Texture.ACF_MODULATE);
        t1.setCombineSrc0RGB(Texture.ACS_TEXTURE);
        t1.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
        t1.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
        t1.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
        t1.setCombineScaleRGB(1.0f);
    }
    
    /**
     * set the cam location. This will be used to check which terrains should be loaded
     * so that you can see the surrounding blocks
     * @param loc cam location in world coordinates
     */
    private void setCamLocation(Vector3f loc) {
        // get cam location in model coordinates
        camLocation.set(loc).subtractLocal(getWorldTranslation());
        cu = getTileU(camLocation);
        cv = getTileV(camLocation);
        ch = camLocation.y/blockSize;
        if ((int)Math.floor(cu) != indexU || (int)Math.floor(cv) != indexV || (int)Math.floor(ch) != indexH) {
            needTileCheck = true;
        }
    }
    
    /**
     * translate a 3D position into a float horizontal index
     * @param loc the 3D position
     * @return the horizontal index
     */
    protected float getTileU(Vector3f loc) {
            return loc.x / blockSize;
    }

    /**
     * translate a 3D position into a float vertical index
     * @param loc the 3D position
     * @return the vertical index
     */
    protected float getTileV(Vector3f loc) {
            return loc.z / blockSize;
    }

    /**
     * build an integer hash from horizontal and vertical index
     * @param u horizontal index
     * @param v vertical index
     * @return integer hash
     */
    protected int getHashIndex(int u, int v) {
            return ((short)(u+16384)) | ( ((short)(v+16384)) << 16 );
    }
    
    // u -horiz
    // v -vert
    private int getBlockTypeAt(int u, int v) {
        int getx;
        int gety;
        // returns which block type is in use for a given u,v coordinate
        // get the wrapped for u (horizontal)
        if( u<0 || u>=blockcount ) {
            // out of range
            if(wrapx) {
                // wrap on whole terrain
                getx=u%blockcount;
                if(getx<0) {
                    getx=blockcount+getx;
                }
            } else {
                if(u<0) {
                    // wrap on first sector
                    getx=u%4;
                    if(getx<0) {
                        getx=4+getx;
                    }
                } else {
                    // wrap on last sector
                    getx=blockcount-4+(u%4);
                }
            }
        } else {
            // in main range
            getx=u;
        }
        // get wrapped on y
        if( v<0 || v>=blockcount ) {
            // out of range
            if(wrapy) {
                // wrap on whole terrain
                gety=v%blockcount;
                if(gety<0) {
                    gety=blockcount+gety;
                }
            } else {
                if(v<0) {
                    // wrap on first sector
                    gety=v%4;
                    if(gety<0) {
                        gety=4+gety;
                    }
                } else {
                    // wrap on last sector
                    gety=blockcount-4+(v%4);
                }
            }
        } else {
            // in main range
            gety=v;
        }
        if( getx<0 || getx>=blockcount || gety<0 || gety>=blockcount ) {
            return 0;
        }
        // return the block type at u,v
        return blockmap[gety*blockcount+getx];
    }
    
    /**
     * retrieve the height of the terrain at the specified position
     * @param loc the location to check in world coordinates
     * @return the height at this point
     */
    public float getHeightFromWorld(Vector3f loc) {

        loc=loc.subtract(this.getWorldTranslation());
        float cu = getTileU(loc);
        float cv = getTileV(loc);
        int intU = (int)Math.floor(cu);
        int intV = (int)Math.floor(cv);
        // do not calculate by instanciated blocks,
        // but by looking at the localy stored heightmap
        int bt=getBlockTypeAt(intU, intV);
        if(bt==0) {
            return Float.NaN;
        }
        int pg=this.getPageFromID(bt)-1;
        int bpx=this.getSectorHeightMapOffsetX(bt);
        int bpy=this.getSectorHeightMapOffsetY(bt);
        Vector3f translation = getTranslation(intU, intV);
        Vector3f local = loc.subtract(translation);

        float x = local.x/stepScale.x + bpx;
        float z = local.z/stepScale.z + bpy;

        float col = FastMath.floor(x);
        float row = FastMath.floor(z);

       //if (col < 0 || row < 0 || col >= size - 1 || row >= size - 1) { return Float.NaN; }
       float intOnX = x - col, intOnZ = z - row;

       float topLeft, topRight, bottomLeft, bottomRight;

       int focalSpot = (int) (col + row * sectorSize);

       // find the heightmap point closest to this position (but will always
       // be to the left ( < x) and above (< z) of the spot.
       topLeft = heightMap[pg][focalSpot] * stepScale.y;

       // now find the next point to the right of topLeft's position...
       topRight = heightMap[pg][focalSpot + 1] * stepScale.y;

       // now find the next point below topLeft's position...
       bottomLeft = heightMap[pg][focalSpot + sectorSize] * stepScale.y;

       // now find the next point below and to the right of topLeft's
       // position...
       bottomRight = heightMap[pg][focalSpot + sectorSize + 1] * stepScale.y;

       // Use linear interpolation to find the height.
       return FastMath.LERP(intOnZ, FastMath.LERP(intOnX, topLeft, topRight),
               FastMath.LERP(intOnX, bottomLeft, bottomRight));
    }
    
    protected Vector3f getTranslation(int bu, int bv) {
        Vector3f loc = new Vector3f(bu * blockSize , 0, //+ blockSize / 2
                        bv * blockSize ); //+ blockSize / 2
        return loc;
    }

    public int getType() {
        return Spatial.NODE;
    }

    public float getHeight(float newX, float newZ) {
        return this.getHeightFromWorld(new Vector3f(newX,0,newZ));
    }

    public int getTerrainType() {
        return Terrain.SECTORED;
    }

    public int getQuadrant() {
        return -1;
    }

    public void setQuadrant(int i) {
        
    }

    public int getSize() {
        return this.totalSizei;
    }

    public int getU() {
        return 0;
    }

    public int getV() {
        return 0;
    }

    public void updateLod(Camera cam) {
        // set camera location
        setCamLocation(cam.getLocation());
        // update tiles
        updateTiles();
        // update lod of tiles
        pg.updateLod(cam);
    }

    /**
     * update the terrain page, i.e. load/unload blocks
     * this function will create/destroy terrain blocks according to the cam location
     * and the sight distance
     */
    public void updateTiles() {
        indexU = (int)Math.floor(cu);
        indexV = (int)Math.floor(cv);
        indexH = (int)Math.floor(ch);
        if (needTileCheck) {
                needTileCheck=false;
                changed = false;
                long start, run;
                start = System.currentTimeMillis();
                if(fullcheck) {
                    checkTilesFull();
                } else {
                    checkTilesDistant();
                }
                if(changed && fullcheck) {
                    pg.updateRenderState();
                }
                pg.lockTransforms();
                pg.lockBounds();
                if(changed) this.updateWorldBound();
                fullcheck=false;
        }
    }
    
    /**
     * check all tiles (blocks) in sight, load missing ones
     * and unload tiles out of sight
     */
    protected void checkTilesFull() {
        if(Config.removeOldShares) {
            for (SharedTerrainBlock itb : blocks.values())
                    itb.setNeeded(false);
        }
        checkTile(indexU, indexV);
        int offset = 1;
        while (neededDistance(offset)) {
            checkTilesOffset(offset);
            offset++;
        }
        
        if(Config.removeOldShares) {
            for (Integer i : blocks.keySet())
                if ( blocks.get(i).getNeeded() < 0) // only destroy if the block was not needed two times
                    markForDestroy(i);
        }
        needTileCheck = false;
    }
    
    private boolean isInBounds(int u, int v) {
        if(boundRefNode!=null) {
            BoundingVolume bv=boundRefNode.getWorldBound();
            boxcent.set(bv.getCenter()).subtractLocal(getLocalTranslation()).divideLocal(blockSize);
            boolean inbound=false;
            float uext;
            float vext;
            if(bv.getType()==BoundingVolume.BOUNDING_SPHERE) {
                BoundingSphere bs=(BoundingSphere) bv;
                uext=bs.radius/blockSize;
                vext=bs.radius/blockSize;
            } else {
                BoundingBox bb=(BoundingBox) bv;
                uext=bb.xExtent/blockSize;
                vext=bb.zExtent/blockSize;                
            }
            if((int)FastMath.floor(boxcent.x-uext)<=u && (int)FastMath.ceil(boxcent.x+uext)>=u
              && (int)FastMath.floor(boxcent.z-vext)<=v && (int)FastMath.ceil(boxcent.z+vext)>=v)
                return true;
            else
                return false;
        }
        return true;
    }
    
    /**
     * Returns if a block at given distance (measured in blocks)
     * should be kept or discarded
     */
    private boolean neededDistance(int blockdistance) {
        if(blockdistance<=sightDistance/blockSize) return true;
        return false;
    }
    
    /**
     * load a tile (block) if it's not there already
     * it will also set a found terrain block to "necessary" so that it won't be unloaded
     * @param indexU horizontal index
     * @param indexV vertical index
     */
    protected void checkTile(int u, int v) {
        if(!isInBounds(u,v)) return;
        int bt=getBlockTypeAt(u, v);
        if(bt!=0) {
            if (! isLoaded(u, v))
                getBlockAt(u, v);
            else
                blocks.get(getHashIndex(u, v)).setNeeded(1);
        }
    }
    
    /**
     * check whether a terrainblock is loaded
     * @param u the horizontal index
     * @param v the vertical index
     * @return true if already loaded
     */
    protected boolean isLoaded(int u, int v) {
        SharedTerrainBlock bl = blocks.get(new Integer(getHashIndex(u, v)));
        if( bl!=null ) {
            return true;
        }
        return false;
    }
    
    /**
     * get a block type needed at position, create new blocks as needed
     * @param u horizontal index
     * @param v vertical index
     * @return true on success
     */
    protected boolean getBlockAt(int u, int v) {
        changed=true;
        // remove block that was there previously
        Integer hi=new Integer(getHashIndex(u, v));
        if(blocks.containsKey(hi)) {
            return true;
        }

        // get the block type at the position
        int bt=getBlockTypeAt(u, v);
        if(bt==0) {
            return false;
        }
        MorphingTerrainBlock mtb=this.getBlock(bt);
        SharedTerrainBlock tb=new SharedTerrainBlock(name+"("+u+","+v+")"+"."+bt, mtb);
        //tb.setModelBound(new BoundingBox());
        tb.unlockBounds();
        tb.setLocalTranslation(getTranslation(u, v));
        tb.setNeeded(1);
        tb.setUV(u,v);

        pg.attachChild(tb);
        //tb.updateWorldBound();
        //tb.updateWorldVectors();
        if(!fullcheck) {
            tb.updateRenderState();
            //tb.lockTransforms();
            //tb.lockBounds();
        }
        blocks.put(hi, tb);
        return true;
    }
    
    protected void markForDestroy(int hashIndex) {
        SharedTerrainBlock itb = blocks.get(hashIndex);
        if(itb!=null) {
            SectoredTerrainPage2 pg=((SectoredTerrainPage2)itb.getParent());
            if(pg!=null) {
                pg.detachChild(itb);
                pg.unlockBounds();
            }
            blocks.remove(hashIndex);
        }
    }
    
    protected void checkTilesOffset(int offset) {
        if(!neededDistance(offset)) return;
            
        if(offset==0) {
            // single
            checkTile(indexU, indexV);
            return;
        }
        int cu, cv;
        cu = indexU + offset;
        cv = indexV;
        checkTile(cu, cv);
        for (int d = 1; d <= offset; d++) {
            checkTile(cu, cv-d);
            checkTile(cu, cv+d);
        }
        cu = indexU - offset;
        checkTile(cu, cv);
        for (int d = 1; d <= offset; d++) {
            checkTile(cu, cv-d);
            checkTile(cu, cv+d);
        }
        cu = indexU;
        cv = indexV + offset;
        checkTile(cu, cv);
        for (int d = 1; d < offset; d++) {
            checkTile(cu-d, cv);
            checkTile(cu+d, cv);
        }
        cv = indexV - offset;
        checkTile(cu, cv);
        for (int d = 1; d < offset; d++) {
            checkTile(cu-d, cv);
            checkTile(cu+d, cv);
        }            
    }
    
    protected void checkTilesDistant() {
        // check tiles on visibility distance
        int mhDist = (int)(sightDistance / blockSize);
        if(Config.removeOldShares) {
            for (SharedTerrainBlock itb : blocks.values()) {
                int tu=(Math.abs(itb.getU()-indexU));
                int tv=(Math.abs(itb.getV()-indexV));
                if( tu>=mhDist || tv>=mhDist )
                        itb.setNeeded(false);
            }
        }
        
        // check tiles on visibility boundary
        checkTilesOffset(mhDist);

        if(Config.removeOldShares) {
            //Collection c=blocks.values()
            //for(int i=0;)
            for (Integer i : blocks.keySet())
                if ( blocks.get(i).getNeeded()<0)
                    markForDestroy(i);
        }
    }
    
    public void updateWorldDate(float time) {
        // do nothing
    }

    public Node getBoundRefNode() {
        return boundRefNode;
    }

    public void setBoundRefNode(Node boundRefNode) {
        this.boundRefNode = boundRefNode;
    }
}
