/*
 * MorphingTerrainBlock.java
 *
 * Created on 2006. március 31., 20:32
 *
 * New implementation of MorphingTerrainBlock, based on compositemesh
 * and batches. No strips, less shared data, but hopefully working 
 * display lists.
 */

package demoviewer.terrain;

import com.jme.bounding.BoundingVolume;
import com.jme.bounding.OBBTree;
import com.jme.math.FastMath;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.lwjgl.LWJGLRenderer;
import com.jme.scene.SceneElement;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.GeomBatch;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.system.JmeException;
import com.jme.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class MorphingTerrainBlock extends TriMesh implements Terrain {
    
    //size of the block, totalSize is the total size of the heightmap if this
       //block is just a small section of it.
       private int size;

       private int totalSize;

       //x/z step
       private Vector3f stepScale;

        //center of the block in relation to (0,0,0)
       private Vector2f offset;

       //amount the block has been shifted.
       private int offsetAmount;

       // heightmap values used to create this block
       private int[] heightMap;

       // the lod level of the block to draw
       // it ranges from 0 to log2(size-1)
       // eg for a block of size 33, it ranges from 0 to 5 inclusive
       // always the floor(lodlevel) index range is drawn
       private float lodlevel=0;
       // the lodlevel at previous draw
       private int shaderidx=-1;

       // the lod levels of our neighbours
       private float uplod=-1;
       private float rightlod=-1;
       private float downlod=-1;
       private float leftlod=-1;

       // the max lod level
       private int maxLod;

       boolean usecompiledheight;

       // float buffers for sparse height array
       // contains height values for each vertex which it has, when
       // no more drawn. Vertices morph towards this value, when
       // they reach a lod level. There are 16 versions of this
       // array, 1 for each fix-up version.
       // it is instance dependant
       //FloatBuffer [] morpharray= new FloatBuffer [16];
       FloatBuffer morpharray;
       // the blended height/mophed height array
       FloatBuffer heightarray;

       // normal buffer, to keep it safe her
       FloatBuffer normbuff;

       // byte buffer for max lod value (at which the vertex dissapears)
       // can be shared among instances of same size
       //ByteBuffer [] maxlodarray= new ByteBuffer [16];
       // byte buffer for each vertex to know which of its neigbours lod to use
       // can be shared among instances of same size
       //FloatBuffer lodindexarray;

       private int quadrant;

       // the mask for fixing up with bordering blocks lod level
       public static final int FIX_UP = 1;
       public static final int FIX_RIGHT = 2;
       public static final int FIX_DOWN = 4;
       public static final int FIX_LEFT = 8;

       // the current fixing mask 
       private int fixMask=0;

       // temp calc vector
       private static Vector3f calcVec1 = new Vector3f();

       // the previous index range drawn
       int prevdrawnrange=-1;

       // the manager for this block
       MorphingTerrainManager manager;
       // sort table for vertices, is shared among blocks of same size
       int [] vertexSortTable;
       
       static float[] prevlod= {-1,-1,-1,-1,-1};
       
       int activebatch=-1;
       
       TriangleBatch batch;
       
    /** Creates a new instance of MorphingTerrainBlock */
    public MorphingTerrainBlock(String name) {
        super(name);
    }
    
   public MorphingTerrainBlock(String name, int size, Vector3f stepScale,
           int[] heightMap, Vector3f origin, int totalSize,
           Vector2f offset, int offsetAmount, int type, MorphingTerrainManager manager) {
       super(name);
       
       if (!FastMath.isPowerOfTwo(size - 1)) { throw new JmeException(
               "size given: "+size+"  Heightmap sizes may only be (2^N + 1)"); }
       
       this.size = size;
       this.stepScale = stepScale;
       this.totalSize = totalSize;
       this.offsetAmount = offsetAmount;
       this.offset = offset;
       this.heightMap = heightMap;
       this.manager=manager;

       setLocalTranslation(origin);
       
       //setActiveBatch(0);
       manager.setUseCompiledMorphData(true);
       manager.setUseStrips(false);
       if(manager!=null && manager.getIndex(size).isUseSortedVertices()) {
           // get the vertex sort table
           vertexSortTable=manager.getIndex(size).getVerticesOrder();
       }
       this.clearBatches();
       batch=new TriangleBatch();
       this.addBatch(batch);
               
       buildVertices();
       maxLod=(int)FastMath.log(size-1,2);
       //buildIndices();
       buildTextureCoordinates();
       if(manager.isUsePerBlockShaders() 
        || manager.isDynamicShaderSwitch() 
        || manager.getShaderCreator().getLightingMode()==MorphingTerrainShaderCreator.LIGHTING_VERTEX
       )
        buildNormals();
       buildColors();
       
       this.usecompiledheight=manager.isUseCompiledMorphData();
       // we build the vertex morph array
       //
       if(manager.isUsePerBlockShaders() 
        || manager.isDynamicShaderSwitch() 
        || manager.getShaderCreator().isUseMorphing()
       )
            compileMorph();
       
       //if(usecompiledheight) {
            // is morphing data set?

       /*} else {
          buildMorphArray2();
       }
        */
       /*
       
        */
       
       // now we have a single batch with everything but the indices set
       // get all the indices and shaders and fill them in 
       createBatches();
       
       if((type&Terrain.ROOT)==0) {
           // clear out the heightmap, so not to waste memory
           this.heightMap =null;
       }
   }

   private void buildVertices() {
       
       VBOInfo vbo = new VBOInfo(manager.isUseVBO());
       vbo.setVBOIndexEnabled(manager.isUseVBO());vbo.copy();
       batch.setVBOInfo(vbo);
       
       batch.setVertexCount(heightMap.length);
       batch.setVertexBuffer(BufferUtils.createVector3Buffer(batch.getVertexBuffer(), batch.getVertexCount()));
       FloatBuffer vertBuf = batch.getVertexBuffer();
       vertBuf.clear();
       int pos;
       for (int y = 0; y < size; y++) {
           for (int x = 0; x < size; x++) {
               pos = x+y*size;
               if(vertexSortTable!=null) {
                   pos=vertexSortTable[pos];
                   //vertBuf.position(vertexSortTable[x+y*size]*3);
               }
               vertBuf.put(pos*3, x * stepScale.x).put(pos*3+1, heightMap[x + (y * size)]* stepScale.y).put(pos*3+2, y * stepScale.z);
           }
       }
   }

   /**
    * <code>buildTextureCoordinates</code> calculates the texture coordinates
    * of the terrain.
    *
    */
   private void buildTextureCoordinates() {
       offset.x += (int) (offsetAmount * stepScale.x);
       offset.y += (int) (offsetAmount * stepScale.z);
       
       FloatBuffer texs = BufferUtils.createVector2Buffer(((FloatBuffer)batch.getTextureBuffers().get(0)), batch.getVertexCount());
       batch.setTextureBuffer(texs, 0);
       texs.clear();

       FloatBuffer vertBuf=batch.getVertexBuffer();
       vertBuf.rewind();
       for (int i = 0; i < batch.getVertexCount(); i++) {
           texs.put((vertBuf.get() + offset.x)
                   / (stepScale.x * (totalSize - 1)));
           vertBuf.get(); // ignore vert y coord.
           texs.put((vertBuf.get() + offset.y)
                   / (stepScale.z * (totalSize - 1)));
       }
   }
   
   public MorphingTerrainManager getManager() {
       return this.manager;
   }
   
   /**
    *
    * <code>buildNormals</code> calculates the normals of each vertex that
    * makes up the block of terrain.
    *
    *
    */
   private void buildNormals() {
       normbuff=TerrainUtils.buildUpRightLeftDownAlteratingNormals(heightMap, stepScale, batch.getNormalBuffer(), vertexSortTable);
       batch.setNormalBuffer(normbuff);
   }
   
   /**
    * Sets the colors for each vertex to the color white.
    */
   private void buildColors() {
       setDefaultColor(null);
   }
   
   /*
    * The lod array contains information for the shader, on how to
    * morph the resulting vertices based on lod level and the fix mask
    * applyed
    */

   private void buildMorphArray2() {
       // change to
       // floatbuffer for height values
       // bytebuffer for max lod value
       // bytebuffer for which lod value to use
       
       // the height when vertice lost
       morpharray=BufferUtils.createFloatBuffer(size*size);
       //morpharray[fixmask]=lodarray;
       morpharray.clear();
       int mstart=0; int mend=size-1; int cstart=0; int cend=size-1;
       int dl, pos;
       int av1,av2;
       
       // the four corners have at maxLod value heir original heights (they do not morph)
       pos=mstart*size+cstart;
       dl=maxLod;
       morpharray.put(pos, heightMap[pos]*stepScale.y);
       // 0,size-1
       pos=cend + (mstart * size);
       morpharray.put(pos, heightMap[pos]*stepScale.y);
       // size-1,0
       pos=cstart + (mend * size);
       morpharray.put(pos, heightMap[pos]*stepScale.y);
       // size-1,size-1
       pos=cend + (mend * size);
       morpharray.put(pos, heightMap[pos]*stepScale.y);
       
       for(int lod=maxLod;lod>=1;lod--) {
           // calculate
           int step=1<<lod;
           dl=lod-1;
           int x,y;
           // calculate data for remaining middle region vertices
           for (y = mstart; y <= mend; y+=step) {
               for (x = cstart; x <= cend; x+=step) {
                  // calculate averages
                  if(x+step<=cend) {
                      // top middle
                      pos=y*size+x+step/2;
                      // average on upper left and upper right
                      av1=y*size+x;
                      av2=y*size+x+step;
                      // average of heights, and lod value
                      morpharray.put(pos, (heightMap[av1]*stepScale.y+heightMap[av2]*stepScale.y)/2f);
                  }
                  if(y+step<=mend) {
                      // middle left
                      pos=(y+step/2)*size+x;
                      // average on upper left and lower left
                      av1=y*size+x;
                      av2=(y+step)*size+x;
                      // average of heights, and lod value
                      // set into buffers
                      morpharray.put(pos, (heightMap[av1]*stepScale.y+heightMap[av2]*stepScale.y)/2f);
                  }
                  if(y+step<=mend && x+step<=cend) {
                      // middle
                      pos=(y+step/2)*size+x+step/2;
                      if((x+y)/step%2==0) {
                          // even with top left, bottom right
                          av1=y*size+x;
                          av2=(y+step)*size+x+step;
                      } else {
                          // odd with top right, bottom left
                          av1=y*size+x+step;
                          av2=(y+step)*size+x;
                      }
                      // average on upper right and lower left
                      // set into buffer
                      morpharray.put(pos, (heightMap[av1]*stepScale.y+heightMap[av2]*stepScale.y)/2f);
                  }
               }
           }
       }
       // center point at maxlod-1
       pos=(cend-cstart)/2 + ((mend-mstart)/2 * size);
       // average on upper left, lower right
       av1=cend;
       av2=mend*size;
       morpharray.put(pos, (heightMap[av1]*stepScale.y+heightMap[av2]*stepScale.y)/2f);
       if(manager!=null) {
           morpharray=manager.getIndex(size).reorderFloatBuffer(morpharray);
       }
   }
     
   private void chooseShader(int lod, GeomBatch batch) {
       GLSLShaderObjectsState so=(GLSLShaderObjectsState) batch.getRenderState(RenderState.RS_GLSL_SHADER_OBJECTS);
        int newshaderidx=lod<2?0:lod<maxLod?1:2;
        GLSLShaderObjectsState nso=manager.getShader(newshaderidx);
        if(nso!=so && batch.getDisplayListID()!=-1) {
            DisplaySystem.getDisplaySystem().getRenderer().releaseDisplayList(batch.getDisplayListID());
            batch.setDisplayListID(-1);
        }
        so=nso;
        batch.setRenderState(so);
   }
   
   private void applyLod() {
       GLSLShaderObjectsState so=(GLSLShaderObjectsState)batch.getRenderState(RenderState.RS_GLSL_SHADER_OBJECTS);
       if(so!=null && batch.getColorBuffer()!=null) {
            // set the lod values into shader as uniforms
            if((int)lodlevel<maxLod) {
               if(Renderer.getCurrentState(RenderState.RS_GLSL_SHADER_OBJECTS)!=so || lodlevel!=prevlod[0] || uplod!=prevlod[1] || rightlod!=prevlod[2] || downlod!=prevlod[3] || leftlod!=prevlod[4] ) {
                    // set shader uniforms and attributes
                    // lod level to use for middle part
                    so.setUniform("MiddleLod", lodlevel);
                    // lods for border vertices
                    so.setUniform("BorderLod", uplod, rightlod, downlod, leftlod);
                    Renderer.clearCurrentState(RenderState.RS_GLSL_SHADER_OBJECTS);
                    prevlod[0]=lodlevel; prevlod[1]=uplod; prevlod[2]=rightlod; prevlod[3]=downlod; prevlod[4]=leftlod; 
               }
            }
       }
   }
   
   public void draw(Renderer r) {
       
       /*
        if (!r.isProcessingQueue()) {
            if (r.checkAndAdd(this))
                return;
        }
        */
        if(manager.isDynamicShaderSwitch())
            chooseShader((int)lodlevel, batch);
        
        applyLod();
        //
        //if((int)lodlevel==maxLod) 
         //   this.generateDisplayList(r);
        //((LWJGLRenderer)r).applyStates(this.getBatch(0).states);
        //batch.onDraw(r);
        
        //super.draw(r);
        r.draw(batch);
        
    }
   
    public float getLodLevel() {
        return lodlevel;
    }

    public void setLodLevel(float middle, float up, float right, float down, float left) {
        this.lodlevel = Math.max(0,Math.min(middle,maxLod));
        this.uplod = Math.max(0,Math.min(up,maxLod));
        this.rightlod = Math.max(0,Math.min(right,maxLod));
        this.downlod = Math.max(0,Math.min(down,maxLod));
        this.leftlod = Math.max(0,Math.min(left,maxLod));
        fixMask=0;
        if(((int)uplod)>((int)lodlevel)) {
            fixMask|=MorphingTerrainBlock.FIX_UP;
        }
        if(((int)rightlod)>((int)lodlevel)) {
            fixMask|=MorphingTerrainBlock.FIX_RIGHT;
        }
        if(((int)downlod)>((int)lodlevel)) {
            fixMask|=MorphingTerrainBlock.FIX_DOWN;
        }
        if(((int)leftlod)>((int)lodlevel)) {
            fixMask|=MorphingTerrainBlock.FIX_LEFT;
        }
        this.setFixMask(fixMask);
    }

    public float getHeight(float x, float z) {
        return TerrainUtils.getAlteratingTriangulatedHeight(x, z, heightMap, stepScale);
    }
    
   public Vector3f getSurfaceNormal(float x, float z, Vector3f store) {
       return TerrainUtils.getSurfaceNormal(x,z, store, stepScale, batch.getNormalBuffer());
   }

    public float getHeightFromWorld(Vector3f position) {
       Vector3f locationPos = calcVec1.set(position).subtractLocal(
               localTranslation).divideLocal(stepScale);
       locationPos.multLocal(stepScale);

       return getHeight(locationPos.x, locationPos.z);
    }

    public int getTerrainType() {
        return Terrain.BLOCK | Terrain.MORPH;
    }

    public int getQuadrant() {
        return quadrant;
    }

    public void setDetailTexture(int unit, int repeat) {
    }

    public void setQuadrant(int i) {
        quadrant=i;
    }

    public int getSize() {
        return (int)FastMath.sqrt(batch.getVertexCount());
    }

    public int getMaxLod() {
        return maxLod;
    }

    public int getFixMask() {
        return fixMask;
    }

    public void setFixMask(int fixmask) {
        // we allow only 0 fixmask with highest index range
        this.fixMask = fixmask&(lodlevel>=maxLod?0x0:0xf);
        int choosenindex=((int)lodlevel)*16+fixMask;
        batch.setEnabled(false);
        this.activebatch=choosenindex;
        batch=this.getBatch(activebatch);
        batch.setEnabled(true);
    }

    /* Compiles the height and morphed height values into a single
     * buffer*/
    private void compileHeight() {
        heightarray=BufferUtils.createVector2Buffer(size*size);
        heightarray.clear();
        if(morpharray==null) {
            buildMorphArray2();
        }
        morpharray.rewind();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                heightarray.put(heightMap[x + (y * size)]* stepScale.y);
                heightarray.put(morpharray.get());
            }
        }
        morpharray=null;
    }

    private void compileMorph() {
        if(batch.getColorBuffer()==null) {
            // morph target not yet set
            if(heightarray==null) {
                MorphingTerrainIndex idx=manager.getIndex(size);
                FloatBuffer lodindexarray=idx.getMorphLodIndexArray();
                FloatBuffer levelarray=idx.getMorphLodLevelArray();
                compileMorph(lodindexarray, levelarray);
            }
            heightarray.rewind();
            batch.setColorBuffer(heightarray);
        }
    }
    
    /* Compiles morph height, morph index and morph level
     * into a single RGBA array (plus 0 for A)
     */
    private void compileMorph(FloatBuffer lodindexarray, FloatBuffer levelarray) {
        heightarray=BufferUtils.createColorBuffer(size*size);
        if(morpharray==null) {
            buildMorphArray2();
        }
        morpharray.rewind();
        lodindexarray.rewind();
        levelarray.rewind();
        heightarray.clear();
        for(int i=0;i<size*size;i++) {
            heightarray.put(morpharray.get()).put(lodindexarray.get()).put(levelarray.get()).put(0);
        }
        morpharray=null;
    }

    public int getU() {
        return -1;
    }

    public int getV() {
        return -1;
    }

    private void createBatches() {
        TriangleBatch tb0=(TriangleBatch) this.getBatch(0);
        MorphingTerrainIndex idx=manager.getIndex(size);
        this.clearBatches();
        FloatBuffer vBuff=tb0.getVertexBuffer();
        FloatBuffer nBuff=tb0.getNormalBuffer();
        FloatBuffer cBuff=tb0.getColorBuffer();
        FloatBuffer tBuff=tb0.getTextureBuffer(0);
        RenderState[] st=null;
        for(int i=0;i<=maxLod;i++) {
            int maxfix=15;
            if(i==maxLod) maxfix=0;
            for(int fix=0;fix<=maxfix;fix++) {
                int choosenindex=i*16+fix;
                // create the batch
                TriangleBatch tb=new TriangleBatch();
                if(st==null) st=tb.states;
                tb.states=st;
                // copy all data
                if(manager.useMultVertBuffers) {
                    int vsize=manager.getIndex(size).getUsedVertQuantity(choosenindex);
                    tb.setVertexBuffer((FloatBuffer) vBuff.duplicate().limit(vsize*3));
                    tb.setNormalBuffer((FloatBuffer) nBuff.duplicate().limit(vsize*3));
                    tb.setColorBuffer((FloatBuffer) cBuff.duplicate().limit(vsize*4));
                    tb.setTextureBuffer((FloatBuffer) tBuff.duplicate().limit(vsize*2), 0);
                } else {
                    tb.setVertexBuffer(vBuff);
                    tb.setNormalBuffer(nBuff);
                    tb.setColorBuffer(cBuff);
                    tb.setTextureBuffer(tBuff,0);
                }
                // create a new VBO info
                VBOInfo vbo = new VBOInfo(manager.isUseVBO());
                vbo.setVBOIndexEnabled(manager.isUseVBO());
                tb.setVBOInfo(vbo);
                // get the proper index buffer
                
                tb.setIndexBuffer(idx.getIndexBuffer(choosenindex));
                tb.setTriangleQuantity(tb.getIndexBuffer().limit()/3);
                tb.setVertexCount(idx.getUsedVertQuantity(choosenindex));
                // get the shader for this lod
                if(manager.isUsePerBlockShaders()) {
                    chooseShader(i, tb);
                }
                tb.setEnabled(false);
                tb.setCullMode(SceneElement.CULL_NEVER);
                this.addBatch(tb);
            }
        }
        vBuff.limit(vBuff.capacity());
    }

    public void updateLod(Camera cam) {
    }
    
    public void findTrianglePick(Ray toTest, ArrayList results, int batchIndex) {
        super.findTrianglePick(toTest, results, 0);
        /*
        if (worldBound == null || !isCollidable) {
            return;
        }
        
        if (worldBound.intersects(toTest)) {
            TriangleBatch triBatch = this.getBatch(0);
            if (triBatch.getCollisionTree() == null) {
               triBatch.setCollisionTree(new OBBTree());
               triBatch.getCollisionTree().construct(triBatch, this, true);
            }
            triBatch.getCollisionTree().bounds.transform(worldRotation, worldTranslation,
	                    worldScale, getTriangleBatch().getCollisionTree().worldBounds);
            triBatch.getCollisionTree().intersect(toTest, results);
        }
         */
    }
    
    public void setRenderStates(RenderState[] states) {
        batch.states=states;
    }
    
    public void updateModelBound() {
        GeomBatch batch =  batchList.get(0);
        if (batch != null) {
            batch.updateModelBound();
        }
        updateWorldBound();
    }
    
    public void updateWorldBound() {
        GeomBatch child =  getBatch(0);
        worldBound = (BoundingVolume) child.getModelBound().transform(worldRotation, worldTranslation, worldScale, worldBound);
    }
    
     public void setModelBound(BoundingVolume modelBound) {
        this.worldBound = null;
        GeomBatch batch = getBatch(0);
        batch.setModelBound(modelBound);
    }   
}
