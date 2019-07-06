/*
 * ShaderedMesh2.java
 *
 * Created on 2006. május 12., 20:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.render;

import com.jme.bounding.BoundingVolume;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.VBOInfo;
import com.jme.scene.state.RenderState;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class MeshModel {
    
    /** Spatial's world absolute rotation. */
    protected Quaternion worldRotation;

    /** Spatial's world absolute translation. */
    protected Vector3f worldTranslation;

    /** Spatial's world absolute scale. */
    protected Vector3f worldScale;
    
    /** The render states of this spatial. */
    protected RenderState[] renderStateList;
    
    /** This spatial's name. */
    protected String name;

    // scale values
    protected int frustrumIntersects = Camera.INTERSECTS_FRUSTUM;
    
    /** The local bounds of this Geometry object. */
    protected BoundingVolume bound;
    
    /** The geometry's per vertex normal information. */
    protected transient FloatBuffer normBuf;

    /** The geometry's vertex information. */
    protected transient FloatBuffer vertBuf;

    /** The geometry's per Texture per vertex texture coordinate information. */
    protected transient ArrayList<FloatBuffer> texBuf;

    /** The geometry's VBO information. */
    protected VBOInfo vboInfo;

    protected ArrayList<Material> batchList;
    
    // min and max enabled batches
        protected int minEnabledBatch=0, maxEnabledBatch=-1;
        
    /** Creates a new instance of ShaderedMesh2 */
    public MeshModel(String name) {
        this.name = name;
        worldRotation = new Quaternion();
        worldTranslation = new Vector3f();
        worldScale = new Vector3f(1.0f, 1.0f, 1.0f);
    }
    
    public Quaternion getWorldRotation() {
        return worldRotation;
    }

    /**
     *
     * <code>getWorldTranslation</code> retrieves the absolute translation of
     * the spatial.
     *
     * @return the world's tranlsation vector.
     */
    public Vector3f getWorldTranslation() {
        return worldTranslation;
    }

    /**
     *
     * <code>getWorldScale</code> retrieves the absolute scale factor of the
     * spatial.
     *
     * @return the world's scale factor.
     */
    public Vector3f getWorldScale() {
        return worldScale;
    }
    
}
