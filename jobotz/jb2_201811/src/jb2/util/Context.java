/*
 * 
 * Vear 2017  * 
 */
package jb2.util;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import jb2.ent.Entity;
import jb2.jo.GameObject;
import jb2.map.NavCell;
import jb2.math.BoundingBox;
import jb2.math.Vector2f;
import jb2.math.Vector3f;

/**
 *
 * @author vear
 */
public class Context {
       public final IntByReference readBytes = new IntByReference();
    public final Memory destBuffer = new Memory(256);
    // initialise destbuffer
    public final Pointer destBufferPointer = destBuffer.share(0, 128);
    public final Pointer tmpPointer = new Pointer(0);
    
    public final float[] Quaternion_toThetaPhiOmega_tmpArr = new float[3];
    
    public final FastList<GameObject> GameObjectFactory_refreshEntities_gameObjectList = new FastList();
    public final FastList<Entity> GameObjectFactory_refreshEntities_entityList = new FastList();
    public final Vector3f GameObject_refresh_objectposition = new Vector3f();
    
    public final Vector2f NavMap_addConnection_nodeanglevector = new Vector2f();
//    public final FastList<Octree2.Element> Octree_getIntersecting_checknodes = new FastList<>(128);
//    public final FastList<OctreeOld.OctreeElement> Octree_getIntersecting_addnodes = new FastList<>(128); 
    //public final IntList Octree_getIntersecting_checknodes = new IntList(128);
    //public final IntList Octree_getIntersecting_addnodes = new IntList(128);
    //public final FastList<NavCell> Octree_getNearestNode_store = new FastList<>();
    public final BoundingBox Octree_getNearestNode_findnodes = new BoundingBox();

    public final BoundingBox EntityTracker_trackPosition_findarea = new BoundingBox();
    //public final FastList<NavCell> PlayerEntity_movedEvent_foundnodes = new FastList<NavCell>();
    
    public final BoundingBox BotEntity_trackPosition_findarea = new BoundingBox();
    //public final FastList<NavCell> BotEntity_trackPosition_foundnodes = new FastList<NavCell>();
    public final BoundingBox EntityTracker = new BoundingBox();
    
    public final BoundingBox NavMap_createNode_box = new BoundingBox();
    //public final FastList<NavCell> NavMap_createNode_nodes = new FastList<NavCell>();
    public final BoundingBox NavCell_generateLinks_bb = new BoundingBox();
    public final FastList<NavCell> NavCell_generateLinks_nodeList = new FastList<NavCell>();
    public final FastList<NavCell> NavCell_getAll_addnodes = new FastList<NavCell>();
    public final FastList<NavCell> NavCell_forEachLeaf_check = new FastList<>();
    public final FastList<NavCell> NavCell_getIntersecting_checknodes = new FastList<NavCell>();
    public final FastList<NavCell> NavCell_forEachIntersectingLeaf_checknodes = new FastList<>();
    
    public final FastList<NavCell> NavCell_initialSplit_check = new FastList<NavCell>();
    public final FastList<NavCell> NavCell_generateAllLinks_check = new FastList<NavCell>();
    public final FastList<NavCell> NavGrid_getCells_roots = new FastList<NavCell>();
        
    public final Vector3f Vector3f_tmpVec1 = new Vector3f();
    
    public final BoundingBox NavCell_createChild_tmpBox = new BoundingBox();
    
         // tmp variables for BoundingBox
         public final Vector3f _compVect1 = new Vector3f();
         public final Vector3f _compVect2 = new Vector3f();
         //public final Matrix3f _compMat = new Matrix3f();
         public final BoundingBox tempBoundingBox = new BoundingBox();
         public final BoundingBox tempBoundingBox2 = new BoundingBox();
         public final float[] fWdU = new float[3];
         public final float[] fAWdU = new float[3];
         public final float[] fDdU = new float[3];
         public final float[] fADdU = new float[3];
         public final float[] fAWxDdU = new float[3];
	 public final Vector3f[] bbverts = new Vector3f[3];
	 public final Vector3f[] bbtarget = new Vector3f[3];
}
