/*
 * nl3DiMaterial.java
 *
 * Created on 2006. április 15., 12:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.n3di;

import com.jme.image.Texture;
import com.jme.scene.state.RenderState;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class nl3DiMaterial {
    
    public static class MaterialTexture {
        public String textureName;
        public Texture tex;
        public int id;
    }
    
    public String name;
    public ArrayList<MaterialTexture> texturelist=new ArrayList<MaterialTexture>();
    public RenderState[] states=new RenderState[RenderState.RS_MAX_STATE];
    public boolean usesNormals=true;
    public boolean usesTexCoords=true;
    public int basicTexture=2;
    public int basicTexture2=-1;
    public int modulateTexture=-1;
            
    /** Creates a new instance of nl3DiMaterial */
    public nl3DiMaterial() {
    }
    
    public void setRenderState(RenderState state) {
        states[state.getType()] = state;
    }
    
    public String getShaderName() {
        return name;
    }
}
