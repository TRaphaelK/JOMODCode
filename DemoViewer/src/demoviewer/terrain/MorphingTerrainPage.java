
package demoviewer.terrain;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.FixedNode;
import com.jme.scene.Geometry;
import com.jme.scene.Spatial;
import com.jme.system.DisplaySystem;
import com.jme.system.JmeException;
import com.jmex.terrain.TerrainBlock;

/**
* <code>MorphingTerrainPage</code> is used to build a quad tree of terrain blocks.
* The <code>TerrainPage</code> will have four children, either four pages or
* four blocks. The size of the page must be (2^N + 1), to allow for even
* splitting of the blocks. Organization of the page into a quad tree allows for
* very fast culling of the terrain. In some instances, using Clod will also
* improve rendering speeds. The total size of the heightmap is provided, as
* well as the desired end size for a block. Appropriate values for the end
* block size is completely dependant on the application. In some cases, a large
* size will give performance gains, in others, a small size is the best option.
* It is recommended that different combinations are tried.
* Based on com.jmex.terrain.TerrainPage
* 
*/
public class MorphingTerrainPage extends FixedNode implements Terrain {

   private static final long serialVersionUID = 1L;

    private Vector2f offset;

   private int totalSize;

   private int size;

   private Vector3f stepScale;

   private int offsetAmount;

   protected int quadrant = 1;

   private static Vector3f calcVec1 = new Vector3f();

   private int[] heightMap;
   
   MorphingTerrainManager manager;
   
   int u,v;
   
   Vector3f prevCamLocation=new Vector3f();
   float [] childdistance=new float[4];
   int [] childorder=new int[4];
   

   /**
    * Creates a TerrainPage to be filled later.  Usually, users don't want to call this function
    * unless they have a terrain page already built.
    * @param name The name of the page node.
    */
   public MorphingTerrainPage(String name){
       this(name,true);
   }
   
   public MorphingTerrainPage(String name, boolean root){
       super(name,4,root);
       //super(name);
   }
   
   /**
    * Constructor instantiates a new <code>TerrainPage</code> object. The
    * data is then split into either 4 new <code>TerrainPages</code> or 4 new
    * <code>TerrainBlock</code>.
    *
    * @param name
    *            the name of the page.
    * @param blockSize
    *            the size of the leaf nodes. This is used to determine if four
    *            new <code>TerrainPage</code> objects should be the child or
    *            four new <code>TerrainBlock</code> objects.
    * @param size
    *            the size of the heightmap for this page.
    * @param stepScale
    *            the scale of the axes.
    * @param heightMap
    *            the height data.
    * @param clod
    *            true will use level of detail, false will not.
    */
   public MorphingTerrainPage(String name, int blockSize, int size,
           Vector3f stepScale, int[] heightMap, boolean clod) {
       this(name, blockSize, size, stepScale, heightMap, Terrain.BLOCK|Terrain.CLOD, null);
   }


   public MorphingTerrainPage(String name, int blockSize, int size,
           Vector3f stepScale, int[] heightMap, int type, MorphingTerrainManager manager) {
       this(name, blockSize, size, stepScale, heightMap, type, size,
               new Vector2f(), 0, manager);
       // this page will be the root
       this.heightMap=heightMap;
   }
   
   /**
    * Constructor instantiates a new <code>TerrainPage</code> object. The
    * data is then split into either 4 new <code>TerrainPages</code> or 4 new
    * <code>TerrainBlock</code>.
    *
    * @param name
    *            the name of the page.
    * @param blockSize
    *            the size of the leaf nodes. This is used to determine if four
    *            new <code>TerrainPage</code> objects should be the child or
    *            four new <code>TerrainBlock</code> objects.
    * @param size
    *            the size of the heightmap for this page.
    * @param stepScale
    *            the scale of the axes.
    * @param heightMap
    *            the height data.
    * @param type
    *            Type of blocks to create (See Terrain)
    * @param totalSize
    *            the total terrain size, used if the page is an internal node
    *            of a terrain system.
    * @param offset
    *            the texture offset for the page.
    * @param offsetAmount
    *            the amount of the offset.
    */
   public MorphingTerrainPage(String name, int blockSize, int size,
           Vector3f stepScale, int[] heightMap, int type, int totalSize,
           Vector2f offset, int offsetAmount, MorphingTerrainManager manager) {
       super(name,4,false);
       //super(name);
       //children=new ArrayList(4);children.add(null);children.add(null);children.add(null);children.add(null);
       if (!FastMath.isPowerOfTwo(size - 1)) { throw new JmeException(
               "size given: "+size+"  Terrain page sizes may only be (2^N + 1)"); }

       this.offset = offset;
       this.offsetAmount = offsetAmount;
       this.totalSize = totalSize;
       this.size = size;
       this.stepScale = stepScale;
       this.manager=manager;
       split(blockSize, heightMap, type);
   }

   public int getType() {
       return (super.getType() | Spatial.TERRAIN_PAGE);
   }

   public int[] getHeightMap() {
       return this.heightMap;
   } 


   /**
    *
    * <code>getHeight</code> returns the height of an arbitrary point on the
    * terrain. If the point is between height point values, the height is
    * linearly interpolated. This provides smooth height calculations. If the
    * point provided is not within the bounds of the height map, the NaN
    * float value is returned (Float.NaN).
    *
    * @param position the vector representing the height location to check.
    * @return the height at the provided location.
    */
   public float getHeight(Vector2f position) {
       return getHeight(position.x, position.y);
   }

   /**
    *
    * <code>getHeight</code> returns the height of an arbitrary point on the
    * terrain. If the point is between height point values, the height is
    * linearly interpolated. This provides smooth height calculations. If the
    * point provided is not within the bounds of the height map, the NaN
    * float value is returned (Float.NaN).
    *
    * @param position the vector representing the height location to check.
    *      Only the x and z values are used.
    * @return the height at the provided location.
    */
   public float getHeight(Vector3f position) {
       return getHeight(position.x, position.z);
   }

   /**
    *
    * <code>getHeight</code> returns the height of an arbitrary point on the
    * terrain. If the point is between height point values, the height is
    * linearly interpolated. This provides smooth height calculations. If the
    * point provided is not within the bounds of the height map, the NaN
    * float value is returned (Float.NaN).
    *
    * @param x the x coordinate to check.
    * @param z the z coordinate to check.
    * @return the height at the provided location.
    */
   public float getHeight(float x, float z) {
       int split = (size - 1) >> 1;
       float halfmapx = split * stepScale.x, halfmapz = split * stepScale.z;
       if(heightMap!=null) {
           // this page holds the heightmap, so no need to traverse down to pages for this information
            return TerrainUtils.getAlteratingTriangulatedHeight(x+halfmapx, z+halfmapz, heightMap, stepScale);
       }
       //determine which quadrant this is in.
       Spatial child = null;
       float newX = 0, newZ = 0;
       if (x == 0) x+=.001f;
       if (z == 0) z+=.001f;
       if (x > 0) {
           if (z > 0) {
               // upper right
               child = getChild(3);
               newX = x;
               newZ = z;
           } else {
               // lower right
               child = getChild(2);
               newX = x;
               newZ = z + halfmapz;
           }
       } else {
           if (z > 0) {
               // upper left
               child = getChild(1);
               newX = x + halfmapx;
               newZ = z;
           } else {
               // lower left...
               child = getChild(0);
               if (x == 0) x -=.1f;
               if (z == 0) z -=.1f;
               newX = x + halfmapx;
               newZ = z + halfmapz;
           }
       }
       if ((child.getType() & Spatial.TERRAIN_PAGE) != 0)
               return ((MorphingTerrainPage) child).getHeight(x
                       - ((MorphingTerrainPage) child).getLocalTranslation().x, z
                       - ((MorphingTerrainPage) child).getLocalTranslation().z);
       else if ((child.getType() & Spatial.TERRAIN_BLOCK) != 0)
           return ((Terrain) child).getHeight(newX, newZ);
       return Float.NaN;
   }

   /**
    * <code>getHeightFromWorld</code> returns the height of an arbitrary
    * point on the terrain when given world coordinates. If the point is
    * between height point values, the height is linearly interpolated. This
    * provides smooth height calculations. If the point provided is not within
    * the bounds of the height map, the NaN float value is returned
    * (Float.NaN).
    *
    * @param position
    *            the vector representing the height location to check.
    * @return the height at the provided location.
    */
   public float getHeightFromWorld(Vector3f position) {
       Vector3f locationPos = calcVec1.set(position).subtractLocal(
               localTranslation);//.divideLocal(stepScale);
       //locationPos.multLocal(getStepScale());

       return getHeight(locationPos.x, locationPos.z);
   }

   /**
    * <code>split</code> divides the heightmap data for four children. The
    * children are either pages or blocks. This is dependent on the size of the
    * children. If the child's size is less than or equal to the set block
    * size, then blocks are created, otherwise, pages are created.
    *
    * @param blockSize
    *            the blocks size to test against.
    * @param heightMap
    *            the height data.
    * @param type
    *            type of blocks to create (See Terrain)
    */
   private void split(int blockSize, int[] heightMap, int type) {
       if ((size >> 1) + 1 <= blockSize) {
           createQuadBlock(heightMap, type);
       } else {
           createQuadPage(blockSize, heightMap, type);
       }

   }

   /**
    * <code>createQuadPage</code> generates four new pages from this page.
    */
   private void createQuadPage(int blockSize, int[] heightMap, int type) {
       //      create 4 terrain pages
       int quarterSize = size >> 2;

       int split = (size + 1) >> 1;

       Vector2f tempOffset = new Vector2f();
       //int offsetAmount=0;
               //this.offsetAmount;
       offsetAmount += quarterSize;

       //1 upper left
       int[] heightBlock1 = TerrainUtils.createHeightSubBlock(heightMap,0,0,split);

       Vector3f origin1 = new Vector3f(-quarterSize * stepScale.x, 0,
               -quarterSize * stepScale.z);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += origin1.x;
       tempOffset.y += origin1.z;

       MorphingTerrainPage page1 = new MorphingTerrainPage(getName() + "Page1", blockSize, split,
               stepScale, heightBlock1, type, totalSize, tempOffset,
               offsetAmount, manager);
       page1.setLocalTranslation(origin1);
       page1.quadrant = 1;
       this.attachChildAt(page1, page1.quadrant-1);

       //2 lower left
       int[] heightBlock2 = TerrainUtils.createHeightSubBlock(heightMap,0,split-1,split);

       Vector3f origin2 = new Vector3f(-quarterSize * stepScale.x, 0,
               quarterSize * stepScale.z);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += origin2.x;
       tempOffset.y += origin2.z;

       MorphingTerrainPage page2 = new MorphingTerrainPage(getName() + "Page2", blockSize, split,
               stepScale, heightBlock2, type, totalSize, tempOffset,
               offsetAmount, manager);
       page2.setLocalTranslation(origin2);
       page2.quadrant = 2;
       this.attachChildAt(page2, page2.quadrant-1);

       //3 upper right
       int[] heightBlock3 = TerrainUtils.createHeightSubBlock(heightMap,split-1,0,split);

       Vector3f origin3 = new Vector3f(quarterSize * stepScale.x, 0,
               -quarterSize * stepScale.z);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += origin3.x;
       tempOffset.y += origin3.z;

       MorphingTerrainPage page3 = new MorphingTerrainPage(getName() + "Page3", blockSize, split,
               stepScale, heightBlock3, type, totalSize, tempOffset,
               offsetAmount, manager);
       page3.setLocalTranslation(origin3);
       page3.quadrant = 3;
       this.attachChildAt(page3, page3.quadrant-1);
       
       ////
       //4 lower right
       int[] heightBlock4 = TerrainUtils.createHeightSubBlock(heightMap,split-1,split-1,split);

       Vector3f origin4 = new Vector3f(quarterSize * stepScale.x, 0,
               quarterSize * stepScale.z);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += origin4.x;
       tempOffset.y += origin4.z;

       MorphingTerrainPage page4 = new MorphingTerrainPage(getName() + "Page4", blockSize, split,
               stepScale, heightBlock4, type, totalSize, tempOffset,
               offsetAmount, manager);
       page4.setLocalTranslation(origin4);
       page4.quadrant = 4;
       this.attachChildAt(page4, page4.quadrant-1);
       
   }

   private void createBlock(int type, int quadrant, int split, Vector3f stepScale, 
                            int[] heightBlock, Vector3f origin, int totalSize, 
                            Vector2f tempOffset, int offsetAmount) {
       Terrain block1=null;
       if((type&Terrain.MORPH)!=0) {
          block1 = new MorphingTerrainBlock(getName() + "Block"+quadrant, split,
               stepScale, heightBlock, origin, totalSize, tempOffset,
               offsetAmount, type, manager);
       }/* else if((type&Terrain.BLOCK)!=0) {
          block1 = new TerrainBlock(getName() + "Block"+quadrant, split,
               stepScale, heightBlock, origin, ((type&Terrain.CLOD)!=0), totalSize, tempOffset,
               offsetAmount);
       }
         */
       // if share is needed create it
       if((type&Terrain.SHARED)!=0) {
           ((Spatial)block1).setCullMode(Spatial.CULL_NEVER);
           ((Spatial)block1).setRenderQueueMode(Renderer.QUEUE_SKIP);
           SharedTerrainBlock share=new SharedTerrainBlock(getName() + "Share"+quadrant, block1, false);
           block1=share;
       }

       block1.setQuadrant(quadrant);
       ((Spatial)block1).setCullMode(Spatial.CULL_DYNAMIC);
       ((Spatial)block1).setRenderQueueMode(manager.getRenderQueueMode());
       this.attachChildAt(((Spatial)block1), quadrant-1);
       if((((Spatial)block1).getType() & Spatial.GEOMETRY)!=0) {
           ((Geometry)block1).setModelBound(new BoundingBox());
           ((Geometry)block1).updateModelBound();
       }
   }
   
   /**
    * <code>createQuadBlock</code> creates four child blocks from this page.
    *
    */
   private void createQuadBlock(int[] heightMap, int type) {
       //create 4 terrain blocks
       int quarterSize = size >> 2;
       int halfSize = size >> 1;
       int split = (size + 1) >> 1;

       Vector2f tempOffset = new Vector2f();
       offsetAmount += quarterSize;

       //1 upper left
       int[] heightBlock1 = TerrainUtils.createHeightSubBlock(heightMap,0,0,split);

       Vector3f origin1 = new Vector3f(-halfSize * stepScale.x, 0, -halfSize
               * stepScale.z);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += origin1.x / 2;
       tempOffset.y += origin1.z / 2;

       createBlock(type, 1, split, stepScale, 
                   heightBlock1, origin1, totalSize, 
                   tempOffset, offsetAmount);

       //2 lower left
       int[] heightBlock2 = TerrainUtils.createHeightSubBlock(heightMap,0,split-1,split);

       Vector3f origin2 = new Vector3f(-halfSize * stepScale.x, 0, 0);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += origin1.x / 2;
       tempOffset.y += quarterSize * stepScale.z;

       createBlock(type, 2, split, stepScale, 
                   heightBlock2, origin2, totalSize, 
                   tempOffset, offsetAmount);
       

       //3 upper right
       int[] heightBlock3 = TerrainUtils.createHeightSubBlock(heightMap,split-1,0,split);

       Vector3f origin3 = new Vector3f(0, 0, -halfSize * stepScale.z);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += quarterSize * stepScale.x;
       tempOffset.y += origin3.z / 2;

       createBlock(type, 3, split, stepScale, 
                   heightBlock3, origin3, totalSize, 
                   tempOffset, offsetAmount);
       
       //4 lower right
       int[] heightBlock4 = TerrainUtils.createHeightSubBlock(heightMap,split-1,split-1,split);

       Vector3f origin4 = new Vector3f(0, 0, 0);

       tempOffset.x = offset.x;
       tempOffset.y = offset.y;
       tempOffset.x += quarterSize * stepScale.x;
       tempOffset.y += quarterSize * stepScale.z;

       createBlock(type, 4, split, stepScale, 
                   heightBlock4, origin4, totalSize, 
                   tempOffset, offsetAmount);
       
  }

   /**
    * Returns the current offset amount.  This is used when building texture coordinates.
    * @return The current offset amount.
    */
   public Vector2f getOffset() {
       return offset;
   }

   /**
    * Returns the total size of the terrain.
    * @return The terrain's total size.
    */
   public int getTotalSize() {
       return totalSize;
   }

   /**
    * Returns the size of this terrain page.
    * @return The current block size.
    */
   public int getSize() {
       return size;
   }

   /**
    * Returns the step scale that stretches the height map.
    * @return The current step scale.
    */
   public Vector3f getStepScale() {
       return stepScale;
   }

   /**
    * Returns the offset amount this terrain block uses for textures.
    * @return The current offset amount.
    */
   public int getOffsetAmount() {
       return offsetAmount;
   }

   /**
    * Sets the value for the current offset amount to use when building texture coordinates.
    * Note that this does <b>NOT</b> rebuild the terrain at all.  This is mostly used for
    * outside constructors of terrain blocks.
    * @param offset The new texture offset.
    */
   public void setOffset(Vector2f offset) {
       this.offset = offset;
   }

   /**
    * Sets the total size of the terrain .  Note that this does <b>NOT</b> rebuild the terrain
    * at all.  This is mostly used for outside constructors of terrain blocks.
    * @param totalSize The new total size.
    */
   public void setTotalSize(int totalSize) {
       this.totalSize = totalSize;
   }

   /**
    * Sets the size of this terrain page.  Note that this does <b>NOT</b> rebuild the terrain
    * at all.  This is mostly used for outside constructors of terrain blocks.
    * @param size The new size.
    */
   public void setSize(int size) {
       this.size = size;
   }

   /**
    * Sets the step scale of this terrain page's height map.  Note that this does <b>NOT</b> rebuild
    * the terrain at all.  This is mostly used for outside constructors of terrain blocks.
    * @param stepScale The new step scale.
    */
   public void setStepScale(Vector3f stepScale) {
       this.stepScale = stepScale;
   }

   /**
    * Sets the offset of this terrain texture map.  Note that this does <b>NOT</b> rebuild
    * the terrain at all.  This is mostly used for outside constructors of terrain blocks.
    * @param offsetAmount The new texture offset.
    */
   public void setOffsetAmount(int offsetAmount) {
       this.offsetAmount = offsetAmount;
   }
           
   protected Terrain getBlock(int quad) {
       Spatial tb = (Spatial)getChild(quad-1);
       if ( tb!=null && (tb.getType() & Spatial.TERRAIN_BLOCK) != 0) {
           return (Terrain)tb;
       }
       return null;
   }

   protected MorphingTerrainPage getPage(int quad) {
       Spatial child = (Spatial)getChild(quad-1);
       if (child!=null && (child.getType() & Spatial.TERRAIN_PAGE) != 0) {
           return (MorphingTerrainPage)child;
       }
       return null;
   }

   protected Terrain findRightBlock(Terrain tb) {
       if (tb.getQuadrant() == 1)
           return getBlock(3);
       else if (tb.getQuadrant() == 2)
           return getBlock(4);
       else if (tb.getQuadrant() == 3) {
           // find the page to the right and ask it for child 1.
           MorphingTerrainPage tp = _findRightPage();
           if (tp != null)
               return tp.getBlock(1);
       } else if (tb.getQuadrant() == 4) {
           // find the page to the right and ask it for child 2.
           MorphingTerrainPage tp = _findRightPage();
           if (tp != null)
               return tp.getBlock(2);
       }

       return null;
   }
   
   protected Terrain findLeftBlock(Terrain tb) {
       if (tb.getQuadrant() == 1) {
            // find the page to the left and ask it for child 3.
           MorphingTerrainPage tp = _findLeftPage();
           if (tp != null)
               return tp.getBlock(3);
       } else if (tb.getQuadrant() == 2) {
           MorphingTerrainPage tp = _findLeftPage();
           if (tp != null)
               return tp.getBlock(4);
       } else if (tb.getQuadrant() == 3) {
               return getBlock(1);
       } else if (tb.getQuadrant() == 4) {
               return getBlock(2);
       }

       return null;
   }
   
   protected Terrain findDownBlock(Terrain tb) {
       if (tb.getQuadrant() == 1)
           return getBlock(2);
       else if (tb.getQuadrant() == 3)
           return getBlock(4);
       else if (tb.getQuadrant() == 2) {
           // find the page below and ask it for child 1.
           MorphingTerrainPage tp = _findDownPage();
           if (tp != null)
               return tp.getBlock(1);
       } else if (tb.getQuadrant() == 4) {
           MorphingTerrainPage tp = _findDownPage();
           if (tp != null)
               return tp.getBlock(3);
       }

       return null;
   }
   
   protected Terrain findUpBlock(Terrain tb) {
       if (tb.getQuadrant() == 1) {
           MorphingTerrainPage tp = _findUpPage();
           if (tp != null)
               return tp.getBlock(2);
       } else if (tb.getQuadrant() == 2) {
           return getBlock(1);
       } else if (tb.getQuadrant() == 3) {
           // find the page below and ask it for child 1.
           MorphingTerrainPage tp = _findUpPage();
           if (tp != null)
               return tp.getBlock(4);
       } else if (tb.getQuadrant() == 4) {
               return getBlock(3);
       }

       return null;
   }
   
   private MorphingTerrainPage _findRightPage() {
       if (getParent() == null || (getParent().getType() & Spatial.TERRAIN_PAGE) == 0) return null;

       MorphingTerrainPage pPage = ((MorphingTerrainPage) getParent());

       if (quadrant == 1)
           return pPage.getPage(3);
       else if (quadrant == 2)
           return pPage.getPage(4);
       else if (quadrant == 3) {
           MorphingTerrainPage tp = pPage._findRightPage();
           if (tp != null)
               return tp.getPage(1);
       } else if (quadrant == 4) {
           MorphingTerrainPage tp = pPage._findRightPage();
           if (tp != null)
               return tp.getPage(2);
       }

       return null;
   }

   private MorphingTerrainPage _findLeftPage() {
       if (getParent() == null || (getParent().getType() & Spatial.TERRAIN_PAGE) == 0) return null;

       MorphingTerrainPage pPage = (MorphingTerrainPage)getParent();

       if (quadrant == 1) {
           MorphingTerrainPage tp = pPage._findLeftPage();
           if (tp != null)
               return tp.getPage(3);
       }
       else if (quadrant == 2){
           MorphingTerrainPage tp = pPage._findLeftPage();
           if (tp != null)
               return tp.getPage(4);
       }
       else if (quadrant == 3) {
           return pPage.getPage(1);
       } else if (quadrant == 4) {
           return pPage.getPage(2);
       }

       return null;
   }
   
   private MorphingTerrainPage _findDownPage() {
       if (getParent() == null || (getParent().getType() & Spatial.TERRAIN_PAGE) == 0) return null;

       MorphingTerrainPage pPage = (MorphingTerrainPage)getParent();

       if (quadrant == 1)
           return pPage.getPage(2);
       else if (quadrant == 3)
           return pPage.getPage(4);
       else if (quadrant == 2) {
           MorphingTerrainPage tp = pPage._findDownPage();
           if (tp != null)
               return tp.getPage(1);
       } else if (quadrant == 4) {
           MorphingTerrainPage tp = pPage._findDownPage();
           if (tp != null)
               return tp.getPage(3);
       }
       return null;
   }
   
   private MorphingTerrainPage _findUpPage() {
       if (getParent() == null || (getParent().getType() & Spatial.TERRAIN_PAGE) == 0) return null;

       MorphingTerrainPage pPage = (MorphingTerrainPage)getParent();

       if (quadrant == 1) {
           MorphingTerrainPage tp = pPage._findUpPage();
           if (tp != null)
               return tp.getPage(2);
       } else if (quadrant == 3) {
           MorphingTerrainPage tp = pPage._findUpPage();
           if (tp != null)
               return tp.getPage(4);
       } else if (quadrant == 2) {
               return pPage.getPage(1);
       } else if (quadrant == 4) {
               return pPage.getPage(3);       
       }
       return null;
   }

   public void updateLod(Camera cam) {
       checkFrustrum(cam);
       if(this.getLastFrustumIntersection()!=Camera.OUTSIDE_FRUSTUM) {
           calculateChildOrder(cam.getLocation());
           // draw children in front to back order
            Spatial child;
            for (int i = 0; i < children.size(); i++) {
                if(childorder[i]!=-1 && children.get(childorder[i])!=null) {
                    ((Terrain)children.get(childorder[i])).updateLod(cam);
                }
            }
       }
   }
   
   public void updateWorldData(float time) {
       if(this.getLastFrustumIntersection()!=Camera.OUTSIDE_FRUSTUM) {
            super.updateWorldData(time);
       }
   }

   protected void checkFrustrum(Camera camera) {
       int state = camera.getPlaneState();
       int frustrumIntersects = (parent != null ? parent.getLastFrustumIntersection() : Camera.INTERSECTS_FRUSTUM);
       if (frustrumIntersects == Camera.INTERSECTS_FRUSTUM) {
           frustrumIntersects = camera.contains(worldBound);
       }
       this.setLastFrustumIntersection(frustrumIntersects);
       camera.setPlaneState(state);
   }
   
   private void calculateChildOrder(Vector3f camLocation) {
       if(!prevCamLocation.equals(camLocation)) {
           prevCamLocation.set(camLocation);
            Spatial child;
            for (int i = 0; i < children.size(); i++) {
                child = children.get(i);
                if (child != null) {
                    childdistance[i]=child.getWorldBound().distanceTo(camLocation);
                } else {
                    childdistance[i]=Float.POSITIVE_INFINITY;
                }
            }
            for (int i = 0; i < children.size(); i++) {
                float mindist=Float.POSITIVE_INFINITY;
                int minidx=-1;
                for (int j = 0; j < children.size(); j++) {
                    if(childdistance[j]<mindist) {
                        mindist=childdistance[j];
                        minidx=j;
                    }
                }
                childorder[i]=minidx;
                if(minidx!=-1) {
                    childdistance[minidx]=Float.POSITIVE_INFINITY;
                }
            }
       }
   }
   
   public void draw(Renderer r) {
       calculateChildOrder(r.getCamera().getLocation());
       // draw children in front to back order
        Spatial child;
        //for (int i = pages.length-1; i >=0 ; i--) {
        for (int i = 0; i < children.size() ; i++) {
            if(childorder[i]!=-1 && children.get(childorder[i])!=null) {
                children.get(childorder[i]).onDraw(r);
            }
        }
   }
   
   public void onDraw(Renderer r) {
       
       if(this.getLastFrustumIntersection()!=Camera.OUTSIDE_FRUSTUM) 
        draw(r);
   }
   
   // a position can be in multiple quadrants, so use a bit anded value.
   private int findQuadrant(int x, int y) {
       int split = (size + 1) >> 1;
       int quads = 0;
       if (x < split && y < split)
           quads |= 1;
       if (x < split && y >= split-1)
           quads |= 2;
       if (x >= split-1 && y < split)
           quads |= 4;
       if (x >= split-1 && y >= split-1)
           quads |= 8;
       return quads;
   }

   /**
    * @return Returns the quadrant.
    */
   public int getQuadrant() {
       return quadrant;
   }

    public int getTerrainType() {
        return Terrain.PAGE;
    }

    public void setQuadrant(int i) {
        this.quadrant=(short)i;
    }

    public int getU() {
        return u;
    }

    public int getV() {
        return v;
    }
} 