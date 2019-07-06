/*
 * TerrainShaderCreator.java
 *
 * Created on 2006. március 22., 10:37
 *
 * Creates shader versions for the terrain
 */

package demoviewer.terrain;

import com.jme.renderer.Renderer;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.system.DisplaySystem;
import com.jme.util.LoggingSystem;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;

/**
 *
 * @author vear
 */
public class MorphingTerrainShaderCreator {
    
    // do we put morph data into texcoords (to enable VBOs)
    boolean usecompiled=false;
    
    // do we use a colormap texture
    boolean colormap=true;
    // colormap tex unit
    int colorTex=0;
    // the fixed colormap scale
    float colormapscale=1f;
    // do we use morphing
    boolean morph=true;
    // do we use normal map
    boolean normalmap=true;
    // normal map tex unit
    int normalTex=1;
    // do we blend vertex normals with normal map
    boolean blendnormals=false;
    // do we use blended details
    boolean usedetails=true;
    // how many blended details we use 0(off) - 4(max)
    int details=0;
    // blend map tex unit
    int blendTex=2;
    // details tex units
    int [] detailTex={3,4,5,6};
    // the fixed detail tiling ratio
    float detailtile=16f;
    // the detail vanishing distance (used when not all blocks have details)
    // set to -1 to disable distance blending
    private float detaildistance=80f;
    // the ratio of color to detail (0.7)
    float colorratio=0.7f;
    // blend detail as pattern, means that colorratio is only applyed to detail color
    boolean detailblenpattern=true;
    // terrain lighting mode:
    public static final int LIGHTING_NOFRAG=0;
    public static final int LIGHTING_FLAT=1;
    public static final int LIGHTING_LIGHTMAP=2;
    public static final int LIGHTING_VERTEX=3;
    public static final int LIGHTING_NORMALMAP3=4;
    public static final int LIGHTING_NORMALMAP2=5;
    int lightingmode=0;
    
    private boolean usefog=true;
    
    boolean usepredefined=true;
   
    
    
    /** Creates a new instance of TerrainShaderCreator */
    public MorphingTerrainShaderCreator() {
        
    }
    
    public void setUseCompiledData(boolean usetexcoorddata) {
        usecompiled=usetexcoorddata;
    }
    
    public void setLightingMode(int lightmode) {
        this.lightingmode=lightmode;
    }
    
    public int getLightingMode() {
        return this.lightingmode;
    }
    
    public void setUseColormap(int texunit) {
        if(texunit==-1) {
            this.colormap=false;
        } else {
            this.colormap=true;
            this.colorTex=texunit;
        }
    }
    
    public void setUseNormalMap(int texunit) {
        if(texunit==-1) {
            normalmap=false;
        } else {
            normalmap=true;
        }
        normalTex=texunit;
    }
    
    public void setUseMorphing(boolean domorph) {
        morph=domorph;
    }
    
    public boolean isUseMorphing() {
        return morph;
    }
    
    public void setBlendNormals(boolean blend) {
        blendnormals=blend;
    }
    
    public void setDetailsCount(int count) {
        details=count;
        if(detailTex.length<details) {
            int[] dt=new int[details];
            Arrays.fill(dt,-1);
            for(int i=0;i<detailTex.length;i++) {
                dt[i]=detailTex[i];
            }
            detailTex=dt;
        }
    }
    
    public void setBlendmap(int texunit) {
        blendTex=texunit;
    }
    
    /**
     * Set which detail map is in which texture unit
     * @param number The number corresponds to the blendmap channels used for blending in 
     * the detail texture:
     * 0 - r
     * 1 - g
     * 2 - b
     * 3 - a
     * @param texunit Texture unit the detail colormap resides in. Set to -1 to not use the given channel.
     */
    public void setDetail(int number, int texunit) {
        detailTex[number]=texunit;
    }
    
    public void setDetailTiling(float detailTileRatio) {
        detailtile=detailTileRatio;
    }
    
    public void setColorToDetailRatio(float colorRatio) {
        colorratio=colorRatio;
    }
    
    public void setDetailBlendPattern(boolean asPattern) {
        this.detailblenpattern=asPattern;
    }
    
    public void setColorMapTiling(float colorMapTileRatio) {
        this.colormapscale=colorMapTileRatio;
    }
    
    public void setDetailDistance(float dist) {
        this.detaildistance=dist;
    }
    
    public void setUseFog(boolean usefog) {
        this.usefog=usefog;
    }
    
    /*
     *  Creates a shader based on distance index
     */
    public GLSLShaderObjectsState createShader(int level) {
        if(this.usepredefined) {
            switch(level) {
                case 0: {
                    // most detailed (level 0)
                    // use vert normals only, its the same as the
                    // normal map in this lod
                    normalmap=true;
                    usedetails=true;
                    morph=true;
                } break;
                case 1: {
                    normalmap=true;
                    usedetails=true;
                    morph=true;
                } break;
                /*
                case 2: {
                    normalmap=true;
                    usedetails=false;
                    morph=true;
                } break;
                 */
                default: {
                    // anything distant
                    normalmap=true;
                    usedetails=true;
                    morph=true;
                } break;
            }
        }
        return createShader();
    }
    
    public GLSLShaderObjectsState createShader() {
        // make the defines
        StringBuffer header=new StringBuffer();
        header.append("\n");
        if(colormap) {
            header.append("#define COLORMAP\n");
            if(colormapscale!=1) {
                header.append("#define COLORMAPTILE ").append(Float.toString(colormapscale)).append("\n");
            }
        }
        if(morph) {
            header.append("#define MORPH\n");
            if(usecompiled)
                header.append("#define MORPHUSECOMPILEDARRAY\n");
        }
        if(this.lightingmode==this.LIGHTING_VERTEX) {
            header.append("#define VTXNORMAL\n");
        }
        if((this.lightingmode==this.LIGHTING_NORMALMAP3 || this.lightingmode==this.LIGHTING_NORMALMAP2) && normalTex!=-1) {
            header.append("#define MAPNORMAL\n");
            if(blendnormals) {
                header.append("#define ADDNORMALS\n");
            }
            if(this.lightingmode==this.LIGHTING_NORMALMAP3) {
                header.append("#define MAPNORMAL3\n");
            }
            if(this.lightingmode==this.LIGHTING_NORMALMAP2) {
                header.append("#define MAPNORMAL2\n");
            }
        }
        if(details>0 && usedetails && blendTex!=-1 && detaildistance!=0) {
            header.append("#define DETAILBLENDMAP\n");
            header.append("#define DETAIL1\n");
            if(details>1) header.append("#define DETAIL2\n");
            if(details>2) header.append("#define DETAIL3\n");
            if(details>3) header.append("#define DETAIL4\n");
            header.append("#define DETAILTILE ").append(Float.toString(detailtile)).append("\n");
            if(colormap)
                header.append("#define COLORRATIO ").append(Float.toString(colorratio)).append("\n");
            if(detaildistance!=-1) 
                header.append("#define DETAILDISTANCE ").append(Float.toString(detaildistance)).append("\n");
            if(detailblenpattern)
                header.append("#define DETAILBLENDPATTERN\n");
        }
        if(usefog) {
            header.append("#define FOG");
        }
        // end main
        // create the shader
        GLSLShaderObjectsState shader=DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
        if(this.lightingmode!=0) {
            String vert=null;
            String frag=null;
            try {
                // load the vert into string
                vert=readShader(MorphingTerrainShaderCreator.class.getClassLoader().getResource(
                            "demoviewer/terrain/terrainfull.vert"));
                frag=readShader(MorphingTerrainShaderCreator.class.getClassLoader().getResource(
                        "demoviewer/terrain/terrainfull.frag"));
                shader.load(header+vert, header+frag);
            } catch (Exception ex) {
                LoggingSystem.getLogger().log(Level.WARNING, "Cannot load terrain shader",ex);
                return null;
            }            
        }
        // preset necessary uniforms
        if(colormap) {
            shader.setUniform("baseMap",colorTex);
        }
        if(normalmap && normalTex!=-1) {
            shader.setUniform("normalMap",normalTex);
        }
        if(details>0 && usedetails && blendTex!=-1) {
            shader.setUniform("blend", blendTex);
            if(detailTex[0]!=-1)
                shader.setUniform("colr1", detailTex[0]);
            if(details>1 && detailTex[1]!=-1)
                shader.setUniform("colr2", detailTex[1]);
            if(details>2 && detailTex[2]!=-1)
                shader.setUniform("colr3", detailTex[2]);
            if(details>3 && detailTex[3]!=-1)
                shader.setUniform("colr4", detailTex[3]);
        }
        shader.setEnabled(true);
        return shader;
    }
    
    private String readShader(URL template) throws IOException {
        StringBuffer acum=new StringBuffer();
        char[] buffer=new char[255];
        int read=0;
        Reader ch=new InputStreamReader(template.openStream());
        
        while( (read=ch.read(buffer)) > -1 ) {
            acum.append(buffer, 0, read);
        }
        return acum.toString();
    }
    
    public void setUsePredefinedShaders(boolean predefined) {
        this.usepredefined=predefined;
    }
}
