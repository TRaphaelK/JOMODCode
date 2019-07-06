/*
 * ShaderedMesh.java
 *
 * Created on 2006. április 13., 20:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.render;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingVolume;
import com.jme.light.Light;
import com.jme.light.PointLight;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.util.ShaderAttributeStore;
import com.jme.util.ShaderUniform;
import demoviewer.map.GameObject;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class ShaderedMesh extends TriMesh {
           
    // ground reference point in model scale
    private Vector3f groundPoint;
    // bottom point in model scale
    private Vector3f bottomPoint;
    // top point in model scale
    private Vector3f topPoint;
    
    boolean nostates=false;
    boolean checkedstates=false;
    int listid=-1;
    
    // bounding box used for selection box
    BoundingBox selBox;
    
    /** Creates a new instance of ShaderedMesh */
    public ShaderedMesh(String name) {
        super(name);
        selBox=new BoundingBox();
    }
    
    public Vector3f getGroundPoint() {
        return groundPoint;
    }

    public void setGroundPoint(Vector3f referencePoint) {
        this.groundPoint = referencePoint;
    }

    public Vector3f getBottomPoint() {
        return bottomPoint;
    }

    public void setBottomPoint(Vector3f bottomPoint) {
        this.bottomPoint = bottomPoint;
    }

    public Vector3f getTopPoint() {
        return topPoint;
    }

    public void setTopPoint(Vector3f topPoint) {
        this.topPoint = topPoint;
    }
    
    public void updateModelBound() {
        if (selBox != null ) {
                selBox.computeFromBatches(batchList);
        }
        super.updateModelBound();
    }
    
    public BoundingBox getSelBox() {
        return selBox;
    }
    /*
    private void updatePointLight(GLSLShaderObjectsState shader) {
        ShaderAttributeStore store=shader.getStore();
        ShaderUniform su=store.getShaderUniform("fvLightPosition");
        if(su!=null && su.type==-1) {
            // finds the nearest point light to the mesh and sets its properyes into material
            LightState st=(LightState) states[RenderState.RS_LIGHT];
            if(st!=null) {
                PointLight cl=null;
                float dist=Float.POSITIVE_INFINITY;
                float nd;
                for(int i=0;i<st.getQuantity();i++) {
                    Light l=st.get(i);
                    if(l.getType()==Light.LT_POINT) {
                        // got our point light
                        if(cl==null) {
                            cl=(PointLight)l;
                            dist=cl.getLocation().distance(this.getWorldTranslation());
                        } else {
                            nd=((PointLight)l).getLocation().distance(this.getWorldTranslation());
                            if(nd<dist) {
                                dist=nd;
                                cl=(PointLight)l;
                            }
                        }
                    }
                }
                if(cl!=null) {
                    // okay got the light, put it into material
                    store.setUniform("fvLightPosition", cl.getLocation());
                    // material properties from material
                    store.setUniform("fvAmbient", cl.getAmbient());
                    store.setUniform("fvSpecular", cl.getSpecular());
                    store.setUniform("fvDiffuse", cl.getDiffuse());
                    // the specular power is the distance?
                    store.setUniform("fSpecularPower", dist);
                }
            }
        }
    }
    */
}
