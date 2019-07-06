/*
 * ShaderManager.java
 *
 * Created on 2006. február 25., 18:53
 *
 * Manages loaded material shaders
 */

package demoviewer.render;

import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.CameraNode;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.ShaderAttributeStore;
import demoviewer.Config;
import demoviewer.n3di.nl3DiMaterial;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 *
 * @author vear
 */
public class ShaderManager2 {
    
    private int maxShaders = 7;
    // shaders in this library
    GLSLShaderObjectsState[] shaders=new GLSLShaderObjectsState[maxShaders];
    // 1- flat1, 2-flat2, 3-phong1, 4-phong2, 5-normal mapped phong, 6-bump

    // usage of shaders
    boolean [] usedShaders = new boolean[maxShaders];
    
    // refernces to scene data
    private CameraNode camNode;
    Vector3f prevcampos=new Vector3f(Float.NaN,Float.NaN,Float.NaN);
    private PointLight pl;
    Renderer r;
    
    
    /** Creates a new instance of ShaderManager */
    public ShaderManager2() {
        r=DisplaySystem.getDisplaySystem().getRenderer();
        // create the renderstate this managed uses
        createStates();
    }
    
    private void createPhongShader(int tu) {
        if(shaders[2+tu]==null) {
            GLSLShaderObjectsState shaderp=r.createGLSLShaderObjectsState();
            String vert=readShader(ShaderManager2.class.getClassLoader().getResource(
                                                "demoviewer/render/phong.vert"));
            String frag=readShader(ShaderManager2.class.getClassLoader().getResource(
                                                        "demoviewer/render/phong.frag"));
            if(tu==2) {
                frag="#define TEX2\n"+frag;
            }
            shaderp.load(vert,frag);
            
            ShaderAttributeStore st=shaderp.getStore();
            // declare the texunit uniform to be setable in
            st.setUniform("basicTexture");
            if(tu==2) {
                st.setUniform("basicTexture2");
            }
            // declare camera position
            st.setUniform("fvEyePosition", camNode.getLocalTranslation());
            if(pl!=null) {
                // if single point light is used
                // declare ligth position
                st.setUniform("fvLightPosition", pl.getLocation());
                // material properties from material
                st.setUniform("fvAmbient", pl.getAmbient());
                st.setUniform("fvSpecular", pl.getSpecular());
                st.setUniform("fvDiffuse", pl.getDiffuse());
                st.setUniform("fSpecularPower", 0.9f);
                prevcampos.set(camNode.getLocalTranslation());
            } else {
                st.setUniform("fvLightPosition");
                // material properties from material
                st.setUniform("fvAmbient");
                st.setUniform("fvSpecular");
                st.setUniform("fvDiffuse");
                st.setUniform("fSpecularPower");
            }

            // relink the shader, setting the uniforms id
            shaderp.relinkProgram();
            // set initial uniform values
            shaderp.apply();

            shaders[2+tu]=shaderp;
        }
    }
    
    private void createPhongTShader() {
        if(shaders[4]==null) {
            GLSLShaderObjectsState shaderp=r.createGLSLShaderObjectsState();
            shaderp.load(ShaderManager2.class.getClassLoader().getResource(
                                                "demoviewer/render/phongt.vert"),
                                                ShaderManager2.class.getClassLoader().getResource(
                                                        "demoviewer/render/phongt.frag"));
            // declare the texunit uniform to be setable in
            ShaderAttributeStore st=shaderp.getStore();
            // the base colormap
            st.setUniform("tex2map");
            // the normal-map
            st.setUniform("tex1map");
            // declare camera position
            st.setUniform("fvEyePosition", camNode.getLocalTranslation());
            if(pl!=null) {
                // if single point light is used
                // declare ligth position
                st.setUniform("fvLightPosition", pl.getLocation());
                // material properties from material
                st.setUniform("fvAmbient", pl.getAmbient());
                st.setUniform("fvSpecular", pl.getSpecular());
                st.setUniform("fvDiffuse", pl.getDiffuse());
                st.setUniform("fSpecularPower", 0.9f);
                prevcampos.set(camNode.getLocalTranslation());
            } else {
                st.setUniform("fvLightPosition");
                // material properties from material
                st.setUniform("fvAmbient");
                st.setUniform("fvSpecular");
                st.setUniform("fvDiffuse");
                st.setUniform("fSpecularPower");
            }

            // relink the shader, setting the uniforms id
            shaderp.relinkProgram();
            // set initial uniform values
            shaderp.apply();

            shaders[4]=shaderp;
        }
    }
    
    private void createBumpShader() {
        if(shaders[5]==null) {
            GLSLShaderObjectsState shaderp=r.createGLSLShaderObjectsState();
            shaderp.load(ShaderManager2.class.getClassLoader().getResource(
                                                "demoviewer/render/phongbump.vert"),
                                                ShaderManager2.class.getClassLoader().getResource(
                                                        "demoviewer/render/phongbump.frag"));
            // declare the texunit uniform to be setable in
            ShaderAttributeStore st=shaderp.getStore();
            // the base colormap
            st.setUniform("tex2map");
            // the normal-map
            st.setUniform("tex1map");
            // declare camera position
            st.setUniform("fvEyePosition", camNode.getLocalTranslation());
            if(pl!=null) {
                // if single point light is used
                // declare ligth position
                st.setUniform("fvLightPosition", pl.getLocation());
                // material properties from material
                st.setUniform("fvAmbient", pl.getAmbient());
                st.setUniform("fvSpecular", pl.getSpecular());
                st.setUniform("fvDiffuse", pl.getDiffuse());
                st.setUniform("fSpecularPower", 0.9f);
                prevcampos.set(camNode.getLocalTranslation());
            } else {
                st.setUniform("fvLightPosition");
                // material properties from material
                st.setUniform("fvAmbient");
                st.setUniform("fvSpecular");
                st.setUniform("fvDiffuse");
                st.setUniform("fSpecularPower");
            }
            // declare tangent and binormal attributes
            st.setAttribute("tangent");
            st.setAttribute("binormap");

            // relink the shader, setting the uniforms id
            shaderp.relinkProgram();
            // set initial uniform values
            shaderp.apply();

            shaders[5]=shaderp;
        }
    }
    
    private void createFlat2Shader() {
        if(shaders[2]==null) {
            GLSLShaderObjectsState shaderp=r.createGLSLShaderObjectsState();
            shaderp.load(ShaderManager2.class.getClassLoader().getResource(
                                                "demoviewer/render/flat2.vert"),
                                                ShaderManager2.class.getClassLoader().getResource(
                                                        "demoviewer/render/flat2.frag"));
            // declare the texunit uniform to be setable in
            ShaderAttributeStore st=shaderp.getStore();
            st.setUniform("tex2map");
            // relink the shader, setting the uniforms id
            shaderp.relinkProgram();
            shaders[2]=shaderp;
        }
    }
    
    private void createFlat1Shader() {
        if(shaders[1]==null) {
            GLSLShaderObjectsState shaderp=r.createGLSLShaderObjectsState();
            shaderp.load(ShaderManager2.class.getClassLoader().getResource(
                                                "demoviewer/render/flat2.vert"),
                                                ShaderManager2.class.getClassLoader().getResource(
                                                        "demoviewer/render/flat1.frag"));
            // declare the texunit uniform to be setable in
            ShaderAttributeStore st=shaderp.getStore();
            st.setUniform("tex1map");
            // relink the shader, setting the uniforms id
            shaderp.relinkProgram();
            shaders[1]=shaderp;
        }
    }
    
    // cull back
    CullState csb;
    // cull none
    CullState csn;
    // glass alpha
    AlphaState gla;
    // texture alpha
    AlphaState txa;
    // glass color
    MaterialState glc;
    // zbufferstate
    ZBufferState zbs;
    
    private void createStates() {
        if(csb==null) {
            csb = r.createCullState();
            csb.setCullMode(CullState.CS_BACK);
            csb.setEnabled(true);
        }
        if(csn==null) {
            csn = r.createCullState();
            csn.setCullMode(CullState.CS_NONE);
            csn.setEnabled(true);
        }
        // glass alpha state
        if(gla==null) {
            gla=r.createAlphaState();
            gla.setBlendEnabled(true);
            gla.setSrcFunction(AlphaState.SB_ONE);//.SB_SRC_ALPHA
            gla.setDstFunction(AlphaState.DB_SRC_COLOR);
            gla.setTestEnabled(true);
            gla.setTestFunction(AlphaState.TF_GREATER);
            gla.setReference(0.5f);
            gla.setEnabled(true);
        }
        // glass color
        if(glc==null) {
            glc = r.createMaterialState();
            glc.setAmbient(ColorRGBA.darkGray);
            glc.setDiffuse(ColorRGBA.darkGray);
            glc.setEnabled(true);
        }
        // texture alpha state
        if(txa==null) {
            txa=r.createAlphaState();
            txa.setBlendEnabled(true);
            //txa.setSrcFunction(AlphaState.SB_ONE);//.SB_SRC_ALPHA
            //txa.setDstFunction(AlphaState.DB_SRC_COLOR);
            txa.setTestEnabled(true);
            txa.setTestFunction(AlphaState.TF_GREATER);
            txa.setReference(0.9f);
            txa.setEnabled(true);
        }
        // zbuffer state
        if(zbs==null) {
            zbs = r.createZBufferState();
            zbs.setEnabled(true);
        }
    }
    
    private void setGlassMaterial(nl3DiMaterial m) {
        if(m.texturelist.isEmpty()) {
            // no texture, glass alpha mode
            m.setRenderState(gla);
            // if no texture, then set up a darkgray color
            m.setRenderState(glc);
            m.usesTexCoords=false;
        } else {
            // texture alpha mode
            m.setRenderState(txa);
        }
        // both sides of the glass
        m.setRenderState(csn);
        m.usesNormals=false;
    }
    
    private void setFixedFunctionMaterial(nl3DiMaterial m) {
        
    }
    
    private void setFlatShadedMaterial(nl3DiMaterial m) {
        //createFlat2Shader();
        //m.setRenderState(shaders[2]);
        m.usesNormals=true;
    }
    
    private void setPhongShadedMaterial(nl3DiMaterial m) {
        if(m.modulateTexture==-1)
            m.setRenderState(shaders[3]);
        else
            m.setRenderState(shaders[4]);
    }
    
    private void setConfiguredMaterial(nl3DiMaterial m) {
        if(Config.modelRender==1) {
            setPhongShadedMaterial(m);
        } else {
            setFlatShadedMaterial(m);
        }
    }
    
    public void preloadShaders() {
        createPhongShader(1);
        createPhongShader(2);
    }
    
    /*
     * Reads attributes of the mesh and creates appropriate
     * states and shaders for the mesh, but does not apply the shaders
     * in the material
     */
    public void preloadShaders(nl3DiMaterial m) {
        if(m!=null) {
            String shname=m.getShaderName().toUpperCase();
            if(
               shname.equals("FFP_GLASS") ||
               shname.equals("FF_ST_AB")
               ) {
                    m.basicTexture=1;
                    setGlassMaterial(m);
            } else if( shname.equals("FF_ST_OP")) {
                m.basicTexture=1;
                setFlatShadedMaterial(m);
                m.setRenderState(txa);
            } else if( shname.equals("FF_MT_OP")
            || shname.equals("FF_ST_AD_LUM")
            ) {
                m.basicTexture=2;
                m.modulateTexture=1;
                setConfiguredMaterial(m);
            } else if(shname.equals("VS_SKBASIC")
                ) {
                m.basicTexture=2;
                setConfiguredMaterial(m);
            } else if(shname.equals("VS_PHONGT")) {
                m.basicTexture=2;
                setConfiguredMaterial(m);
            } else if(shname.equals("VS_SKBUMPDIFFOBJ")
                || shname.equals("VS_SKBUMPDIFFT")
                || shname.equals("VS_SKBUMPPHONGT")
                || shname.equals("VS_SKBUMPPHONGOBJ")
                || shname.equals("VS_SKBUMPDIFFT2")
                ) {
                m.basicTexture=2;
                setConfiguredMaterial(m);
            } else if(shname.equals("VS_DOT3DIFF")) {
                m.basicTexture=2;
                setConfiguredMaterial(m);
            } else if(shname.equals("VS_DOT3DIFF2")) {
                m.basicTexture=3;
                setConfiguredMaterial(m);
                // normal map: 2
                // 1: displacemap (heightmap)
                //m.setRenderState(txa);
                        
            } else if(shname.equals("VS_FLAG")) {
                // TODO set in material, that this is a flag batch
                // create it as a separate child
                // use ids: 3, 65537, 16842753
                // for different colored flags
                m.basicTexture=1; // 1 or 3
                m.basicTexture2=3;
                setConfiguredMaterial(m);
            }
        }
    }
    
    // updates shaders to reflect new scene data
    public void update() {
        if(camNode!=null) {
            if(prevcampos.equals(camNode.getLocalTranslation())) {
                if(shaders[3]!=null) {
                    shaders[3].getStore().setUniform("fvEyePosition", camNode.getLocalTranslation());
                    shaders[3].apply();                
                }
                if(shaders[4]!=null) {
                    shaders[4].getStore().setUniform("fvEyePosition", camNode.getLocalTranslation());
                    shaders[4].apply();
                }
                if(shaders[5]!=null) {
                    shaders[5].getStore().setUniform("fvEyePosition", camNode.getLocalTranslation());
                    shaders[5].apply();
                }
                prevcampos.set(camNode.getLocalTranslation());
            }
        }
    }

    public CameraNode getCamNode() {
        return camNode;
    }

    public void setCamNode(CameraNode camNode) {
        this.camNode = camNode;
    }

    public PointLight getPointLight() {
        return pl;
    }

    public void setPointLight(PointLight pl) {
        this.pl = pl;
    }
    
    private String readShader(URL template) {
        try {
            StringBuffer acum=new StringBuffer();
            char[] buffer=new char[255];
            int read=0;
            Reader ch=new InputStreamReader(template.openStream());

            while( (read=ch.read(buffer)) > -1 ) {
                acum.append(buffer, 0, read);
            }
            return acum.toString();
        } catch(Exception e) {
            return null;
        }
    }

}
