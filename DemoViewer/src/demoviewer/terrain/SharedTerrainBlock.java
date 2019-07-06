/*
 * SharedTerrainBlock.java
 *
 * Created on 2006. március 11., 18:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import com.jme.bounding.BoundingVolume;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.util.AreaUtils;
import com.jme.util.LoggingSystem;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;

/**
 *
 * @author vear
 */
public class SharedTerrainBlock extends TriMesh implements Terrain {
    
    float stepDistance=129*4;
    private float lastDistance = 0f;
    private Vector3f lastCameraAngle=new Vector3f();
    private Vector3f lastCameraPosition=new Vector3f();
    private Vector3f camLocation=new Vector3f();
    private float maxLod;
    private float lastLod=0f;
    // greedyLod means that if our lod is bigger
    // by 1 of our neighbour, we upper limit it to neighbours+1
    // opposite is, that we bottom limit out lod to neighbours-1
    private boolean greedyLod=true;
    
    private boolean updatesCollisionTree;
    
    // the target terrainblock
    private MorphingTerrainBlock targetm;
    
    // the adjacent terrain blocks
    private SharedTerrainBlock up;
    private SharedTerrainBlock right;
    private SharedTerrainBlock down;
    private SharedTerrainBlock left;
    
    // quadrant to use with terrainpage
    private int quadrant;
    private int u,v;
    
    float uplod;
    float rightlod;
    float downlod;
    float leftlod;
    
    // data specific to SectoredTerrain2
    private int usage=0;
    
    /** Creates a new instance of SharedTerrainBlock */
    public SharedTerrainBlock(String name, Terrain target) {
        this(name, target, true);
    }
    
    /**
     * Constructor creates a new <code>SharedMesh</code> object.
     *	
     * @param name
     *            the name of this shared mesh.
     * @param target
     *            the TriMesh to share the data.
     * @param updatesCollisionTree
     *            Sets wether calls to <code>updateCollisionTree</code> of this 
     *            </code>SharedMesh</code> will be passed to the target Mesh. 				            
     */
    public SharedTerrainBlock(String name, Terrain target, boolean updatesCollisionTree) {
            super(name);
            setUpdatesCollisionTree(updatesCollisionTree);
            
            setTarget((MorphingTerrainBlock)target);
            
            this.localRotation.set(targetm.getLocalRotation());
            this.localScale.set(targetm.getLocalScale());
            this.localTranslation.set(targetm.getLocalTranslation());
    }
    
    public int getType() {
            return (Spatial.GEOMETRY | Spatial.TRIMESH | Spatial.TERRAIN_BLOCK);
    }
    
    /**
     * <code>setTarget</code> sets the shared data mesh.
     * 
     * @param target
     *            the TriMesh to share the data.
     */
    public void setTarget(MorphingTerrainBlock target) {
        this.targetm=target;
        this.stepDistance=targetm.getManager().getLodStepDistance();
        for (int i = 0; i < RenderState.RS_MAX_STATE; i++) {
            RenderState renderState = target.getRenderState( i );
            if (renderState != null) {
                setRenderState(renderState );
            }
        }
    }
    
    public Terrain getTarget() {
        return targetm;
    }
    
    public void reconstruct(FloatBuffer vertices, FloatBuffer normals,
                    FloatBuffer colors, FloatBuffer textureCoords) {
            //LoggingSystem.getLogger().log(Level.INFO, "SharedTerrainBlock will ignore reconstruct.");
    }
    
    /**
     * <code>updateWorldBound</code> updates the bounding volume that contains
     * this geometry. The location of the geometry is based on the location of
     * all this node's parents. Also stores information on this blocks neighbours.
     * 
     * @see com.jme.scene.Spatial#updateWorldBound()
     */
    public void updateWorldBound() {
        if((lockedMode &LOCKED_BOUNDS)!=0) return;
        if (targetm!=null) {
                BoundingVolume bv=targetm.getWorldBound();
                if(bv==null) targetm.updateModelBound();
                bv=targetm.getWorldBound();
                worldBound = bv.transform(worldRotation,
                                worldTranslation, worldScale, worldBound);
        }
        // at point of updating bounds, we also get information
        // on our neighbours
        updateNeighBours();
    }
    
    public void lockBounds() {
        if((lockedMode & LOCKED_BOUNDS)==0) {
            updateGeometricState(0, true);
            lockedMode |= LOCKED_BOUNDS;
            if(up!=null)
                up.updateNeighBours();
            if(right!=null)
                right.updateNeighBours();
            if(down!=null)
                down.updateNeighBours();
            if(left!=null)
                left.updateNeighBours();
        }
    }

    public void updateNeighBours() {
        // is our parent a MorphingTerrainPage?
        if((parent.getType() | Spatial.TERRAIN_PAGE)!=0) {
            MorphingTerrainPage pg=(MorphingTerrainPage)parent;
            Terrain t;
            t=pg.findUpBlock(this);
            if(t!=null && (t.getTerrainType() | Terrain.SHARED)!=0) {
                up=(SharedTerrainBlock)t;
            } else {
                up=null;
            }
            t=pg.findRightBlock(this);
            if(t!=null && (t.getTerrainType() | Terrain.SHARED)!=0) {
                right=(SharedTerrainBlock)t;
            } else {
                right=null;
            }
            t=pg.findDownBlock(this);
            if(t!=null && (t.getTerrainType() | Terrain.SHARED)!=0) {
                down=(SharedTerrainBlock)t;
            } else {
                down=null;
            }
            t=pg.findLeftBlock(this);
            if(t!=null && (t.getTerrainType() | Terrain.SHARED)!=0) {
                left=(SharedTerrainBlock)t;
            } else {
                left=null;
            }
        }
    }   
        
    private void setNeighBoursLods() {
        // after checking all neighbours, we should be set up
        // to get the resulting lods
        float nuplod=lastLod;
        float nrightlod=lastLod;
        float ndownlod=lastLod;
        float nleftlod=lastLod;
        if(up!=null) {
            nuplod=Math.max(up.getLastLod(),nuplod);
        }
        if(right!=null) {
            nrightlod=Math.max(right.getLastLod(),nrightlod);
        }
        if(down!=null) {
            ndownlod=Math.max(down.getLastLod(),ndownlod);
        }
        if(left!=null) {
            nleftlod=Math.max(left.getLastLod(),nleftlod);
        }
        if((int)nuplod!=(int)uplod || (int)nrightlod!=(int)rightlod || (int)ndownlod!=(int)downlod || (int)nleftlod!=leftlod) {
            // lod change will occur

            /*
            targets.setLocalTranslation(worldTranslation);
            targets.setLocalRotation(worldRotation);
            targets.setLocalScale(worldScale);
            targetm.setLodLevel(lastLod, uplod, rightlod, downlod, leftlod);
             */
            //targetm.generateDisplayList();

        }
        uplod=nuplod;
        rightlod=nrightlod;
        downlod=ndownlod;
        leftlod=nleftlod;
    }
    
    public void updateLod(Camera cam) {
        checkFrustrum(cam);
        // lod calculation moved here
            // determine the lod level
        if(targetm!=null) {
            if(this.getLastFrustumIntersection()!=Camera.OUTSIDE_FRUSTUM) {
                setCameraLocation(cam.getLocation());
                this.stepDistance=targetm.getManager().getLodStepDistance();
                boolean check=getDistanceLod();//||(lastLod<=maxLod+1);
                if(true) {
                    //if(lastLod<maxLod+2) {
                        // check on which sides it need to be fixed
                    checkNeighbours();
                    //} else {
                     //   lastLod=maxLod;
                    //}
                }
            }
        }
    }
    
    public void updateWorldData(float time) {
        updateWorldVectors();
    }
    
    /**
     * draw renders the target mesh, at the translation, rotation and scale of
     * this shared mesh.
     * 
     * @see com.jme.scene.Spatial#draw(com.jme.renderer.Renderer)
     */
    public void draw(Renderer r) {

            if (!r.isProcessingQueue()) {
                    if (r.checkAndAdd(this))
                            return;
            }
            /*
            if(notChecked) {
            
                setCameraLocation(r.getCamera().getLocation());
                checkLod();
                
            }
             */
            setNeighBoursLods();
            
            targetm.setLocalTranslation(worldTranslation);
            targetm.setLocalRotation(worldRotation);
            targetm.setLocalScale(worldScale);
            targetm.setLodLevel(lastLod, uplod, rightlod, downlod, leftlod);
            //targets.updateWorldVectors();
            targetm.setRenderStates(this.getBatch(0).states);
            //applyStates();
            // call draw of target
            targetm.draw(r);
            //r.draw(target);
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
   
   public void onDraw(Renderer r) {
       
       if(this.getLastFrustumIntersection()!=Camera.OUTSIDE_FRUSTUM)
           draw(r);
   }
   
	/**
     * This function checks for intersection between the target trimesh and the given
     * one. On the first intersection, true is returned.
     * 
     * @param toCheck
     *            The intersection testing mesh.
     * @return True if they intersect.
     */
    public boolean hasTriangleCollision(TriMesh toCheck) {
        targetm.setLocalTranslation(worldTranslation);
                targetm.setLocalRotation(worldRotation);
                targetm.setLocalScale(worldScale);
                targetm.updateWorldBound();
                return targetm.hasTriangleCollision(toCheck);
    }
    
    /**
     * This function finds all intersections between this trimesh and the
     * checking one. The intersections are stored as Integer objects of Triangle
     * indexes in each of the parameters.
     * 
     * @param toCheck
     *            The TriMesh to check.
     * @param thisIndex
     *            The array of triangle indexes intersecting in this mesh.
     * @param otherIndex
     *            The array of triangle indexes intersecting in the given mesh.
     */
    public void findTriangleCollision(TriMesh toCheck, int batch1, int batch2, ArrayList thisIndex,
            ArrayList otherIndex) {
    	targetm.setLocalTranslation(worldTranslation);
		targetm.setLocalRotation(worldRotation);
		targetm.setLocalScale(worldScale);
		targetm.updateWorldBound();
		targetm.findTriangleCollision(toCheck, batch1, batch2, thisIndex, otherIndex);
    }
    
    /**
     * 
     * <code>findTrianglePick</code> determines the triangles of the target trimesh
     * that are being touched by the ray. The indices of the triangles are
     * stored in the provided ArrayList.
     * 
     * @param toTest
     *            the ray to test.
     * @param results
     *            the indices to the triangles.
     */
    public void findTrianglePick(Ray toTest, ArrayList results) {
    	targetm.setLocalTranslation(worldTranslation);
		targetm.setLocalRotation(worldRotation);
		targetm.setLocalScale(worldScale);
		targetm.updateWorldBound();
		targetm.findTrianglePick(toTest, results, 0);
    }
    
    /**
     * <code>getUpdatesCollisionTree</code> returns wether calls to 
     * <code>updateCollisionTree</code> will be passed to the target mesh.
     * 
     * @return true if these method calls are forwared.
     */ 
     public boolean getUpdatesCollisionTree() {
            return updatesCollisionTree;
    }

    /**
     * code>setUpdatesCollisionTree</code> sets wether calls to 
     * <code>updateCollisionTree</code> are passed to the target mesh.
     * 
     * @param updatesCollisionTree
     *            true to enable. 
     */ 
    public void setUpdatesCollisionTree(boolean updatesCollisionTree) {
            this.updatesCollisionTree = updatesCollisionTree;
    }
  
  protected boolean getDistanceLod() {
      // determine our base lod, unchanged by neighbours
      if(lastCameraPosition.equals(camLocation)) {
          return false;
      }
      
      //if(newDistance==lastDistance) {
          // check if camera angle is same as previously too
          //if(lastCameraAngle.equals(r.getCamera().getDirection())) {
              // our lod is already calculated and fixed
              
          //}
      //}
      lastCameraPosition.set(camLocation);
      float newDistance = Math.min(getWorldBound().distanceTo(lastCameraPosition),getWorldBound().distanceToEdge(lastCameraPosition));
      
      //lastCameraAngle.set(camLocation);
      maxLod=targetm.getMaxLod();
      float newLod=newDistance/stepDistance;
      /*
      if(newDistance<lastDistance && newLod<lastLod) {
          lastLod=newLod;
      } else if(newDistance>lastDistance && newLod>lastLod) {
          lastLod=newLod;
      }
       */
      lastLod=newLod;
      lastDistance = newDistance;
      return true;
  }
  
  protected void setCameraLocation(Vector3f camLocation) {
      this.camLocation=camLocation;
  }
  
  /*
   * Modifyes our lod based on a bordering blocks,
   * returns true if modification was done
   */
  protected boolean lodRule(float newLod, float neighboursLod) {
    if(greedyLod) {
        if(newLod >  neighboursLod+1) {
            lastLod=neighboursLod+1f;
            // we have to recheck our lod with our neighbours
            return true;
        }
    } else {
        if(newLod < neighboursLod-1) {
            lastLod=Math.max(newLod, neighboursLod-1);
            // we have to recheck our lod with our neighbours
            return true;
        }
    }
    return false;
  }
  
  protected boolean checkNeighbour(SharedTerrainBlock neighbour) {
    boolean check=true;
    neighbour.setCameraLocation(camLocation);
    //neighbour.getDistanceLod();
    boolean culled=this.getLastFrustumIntersection()==Camera.OUTSIDE_FRUSTUM;
    check= (!culled)
        | (neighbour.getLastFrustumIntersection()!=Camera.OUTSIDE_FRUSTUM)
         //neighbour.getLastLod()<=maxLod;
            //(neighbour.getDistanceLod(r)<=maxLod);
        //| (Math.abs(-lastLod)>1)
        ;
    if(check ) {
        float nlod=neighbour.chooseDistanceLod(lastLod);
        return lodRule(lastLod, nlod);
    }
    return false;
  }
  
  protected void checkNeighbours() {
        
        boolean docheck=true;
        // until we manage to balance all our visible neighbours
        while(docheck) {
            docheck=false;
            // determine if our last culling was successful

            // check on which sides it need to be fixed
            if(up!=null) { 
                // if we are outside frustrum, only check neighbour if its
                // inside the frustrum
               docheck|=checkNeighbour(up);
            }
            if(right!=null) {
                docheck|=checkNeighbour(right);
            }
            if(down!=null ) {
                docheck|=checkNeighbour(down);
            }
            if(left!=null ) {
                docheck|=checkNeighbour(left);
            }
        }
  }
  
  protected float chooseDistanceLod(float neighboursLod) {
    // do we have to limit our lod
    if(getDistanceLod() || lodRule(lastLod, neighboursLod)) {
        // our neighbours lod affects ours and vice versa, 
        // so we have to check it with our neighbours too
        if(targetm!=null && lastLod<=maxLod) {
            checkNeighbours();
        }
    }
    return lastLod;
  }
  
    public int getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(int quadrant) {
        this.quadrant = quadrant;
    }
    
    public void setModelBound(BoundingVolume modelBound) {
        this.worldBound = null;
        if(targetm!=null) {
            targetm.setModelBound(modelBound);
        }
    }

    public void updateModelBound() {
        if (targetm != null) {
            targetm.updateModelBound();
            updateWorldBound();
        }
    }
/*
    public void updateFromHeightMap() {
        if (targetm != null) {
            targetm.updateFromHeightMap();
        }
    }
 */

    public float getHeight(float newX, float newZ) {
        if (targetm != null) {
            return targetm.getHeight(newX, newZ);
        } else {
            return Float.NaN;
        }
    }

    public Vector3f getSurfaceNormal(float newX, float newZ, Vector3f store) {
        if (targetm != null) {
            return targetm.getSurfaceNormal(newX, newZ, store);
        }
        return null;
    }

    public float getHeightFromWorld(Vector3f loc) {
        if (targetm != null) {
            // TODO recalculte to our translation
            //return target.getHeight(loc);
        }
        return Float.NaN;
    }

    public int getTerrainType() {
        return Terrain.SHARED;
    }
/*
    public void multHeightMapValue(int col, int row, int toMult) {
        targetm.multHeightMapValue(col, row, toMult);
    }
 */

    public void setDetailTexture(int unit, int repeat) {
        targetm.setDetailTexture(unit, repeat);
    }

    public int getSize() {
        return targetm.getSize();
    }
/*
    public void addHeightMapValue(int col, int row, int toAdd) {
        targetm.addHeightMapValue(col, row, toAdd);
    }
 */
/*
    public void setHeightMapValue(int col, int row, int newVal) {
    }
 */
    /*
    protected void applyRenderState(Stack[] states) {
        super.applyRenderState(states);
    }
     */

    public float getLastLod() {
        return lastLod;
    }
        
    public void findTrianglePick(Ray toTest, ArrayList results, int batchIndex) {
            targetm.setLocalTranslation(worldTranslation);
            targetm.setLocalRotation(worldRotation);
            targetm.setLocalScale(worldScale);
            targetm.updateWorldBound();
            targetm.setLodLevel(lastLod, uplod, rightlod, downlod, leftlod);
            targetm.findTrianglePick(toTest, results, 0);
    }

    public int getU() {
        return u;
    }

    public int getV() {
        return v;
    }

    public void setUV(int u, int v) {
        this.u=u;
        this.v=v;
    }
    
    public int getNeeded() {
        return usage;
    }

    public void setNeeded(boolean needed) {
        if(needed) 
            usage++;
        else
            usage--;
    }
    
    public void setNeeded(int needed) {
        usage=needed;
    }
}
