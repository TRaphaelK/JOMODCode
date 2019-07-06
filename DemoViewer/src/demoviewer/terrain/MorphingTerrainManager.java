/*
 * MorphingTerrainIndexManager.java
 *
 * Created on 2006. március 21., 17:53
 *
 * Stores shared indix data for MorphingTerrainBlocks
 */

package demoviewer.terrain;

import com.jme.renderer.Renderer;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.system.DisplaySystem;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author vear
 */
public class MorphingTerrainManager {
    
    static MorphingTerrainManager instance;
    
    MorphingTerrainShaderCreator shaderCreator;
    // how many shader versions we have?
    int maxshadertypes=1;
            
    // the indexbuffers associated with a specific block size
    ArrayList indexes;
    // the shaders associated with a level
    ArrayList shaders;
    // should blocks use strips instead of lists
    boolean usestrips=true;
    // should morph data be sent in attrib arrrays or texcoord data
    boolean usecompiledmorphdata=false;
    
    public boolean[] relinked={false,false,false};
    
    // the step distance for lod
    float stepDistance=512;//2500;

    int queuemode=Renderer.QUEUE_SKIP;
    
    boolean usevbo=true;
    
    boolean sortvertices=false;
    boolean useMultVertBuffers=false;
    // blocks use different shaders?
    boolean perblockshaders=true;
    // blocks can switch shaders after creation?
    boolean dynamicshaderswitch=false;
    
    /** Creates a new instance of MorphingTerrainIndexManager */
    public MorphingTerrainManager() {
        indexes=new ArrayList();
        shaders=new ArrayList();
        shaderCreator=new MorphingTerrainShaderCreator();
    }
    
    public static MorphingTerrainManager getInstance() {
        if(instance==null) {
            instance=new MorphingTerrainManager();
        }
        return instance;
    }
    
    public boolean isDynamicShaderSwitch() {
        return dynamicshaderswitch;
    }
    
    public void setDynamicShaderSwitch(boolean dynamicshaderswitch) {
        this.dynamicshaderswitch=dynamicshaderswitch;
    }
    
    public void setUsePerBlockShaders(boolean perblockshader) {
        perblockshaders=perblockshader;
    }
    
    public boolean isUsePerBlockShaders() {
        return this.perblockshaders;
    }
    
    public int getRenderQueueMode() {
        return queuemode;
    }
    
    public void setRenderQueueMode(int queuemode) {
        this.queuemode=queuemode;
    }
    
    public float getLodStepDistance() {
        return stepDistance;
    }
    
    public void setLodStepDistance(float lodStepDistance) {
        stepDistance=lodStepDistance;
    }
    
    public void setUseCompiledMorphData(boolean usecompiled) {
        usecompiledmorphdata=usecompiled;
    }
    
    public boolean isUseCompiledMorphData() {
        return usecompiledmorphdata;
    }
    
    public void setUseStrips(boolean usestrips) {
        this.usestrips=usestrips;
    }
    
    public void setUseVBO(boolean usevbo) {
        this.usevbo=usevbo;
    }
    
    public boolean isUseVBO() {
        return this.usevbo;
    }
    
    public void setSortVertices(boolean sortVertices) {
        sortvertices=sortVertices;
    }
    
    public void setUseMultipleVertexBuffers(boolean useMultVertBuffers) {
        this.useMultVertBuffers=useMultVertBuffers;
    }
    
    public void setMaxShaderTypes(int shaderTypeCount) {
        maxshadertypes=shaderTypeCount;
    }
    
    public MorphingTerrainIndex getIndex(int size) {
        // return the bufferstore for the proper size,
        // if not exists, create it
        MorphingTerrainIndex idx=null;
        for(int i=0;i<indexes.size() && idx==null;i++) {
            if(((MorphingTerrainIndex)indexes.get(i)).getSize()==size && ((MorphingTerrainIndex)indexes.get(i)).isUseStrips()==usestrips) {
                idx=(MorphingTerrainIndex)indexes.get(i);
            }
        }
        if(idx==null) {
            // no bufferstore with the required size, create it
            idx=new MorphingTerrainIndex(size, usestrips, sortvertices);
            indexes.add(idx);
        }
        return idx;
    }
    
    public void clearIndexes() {
        indexes.clear();
    }
    
    public MorphingTerrainShaderCreator getShaderCreator() {
        return shaderCreator;
    }
    
    public GLSLShaderObjectsState getShader(int level) {
        if(level>maxshadertypes-1) level=maxshadertypes-1;
        if(level<0) return null;
        return (GLSLShaderObjectsState)shaders.get(level);
    }
    
    public void clearShaders() {
        for(int i=0;i<shaders.size();i++) ((GLSLShaderObjectsState)shaders.get(i)).release();
        shaders.clear();
        Arrays.fill(relinked, false);
    }
    
    public void createShaders() {
        clearShaders();
        shaderCreator.setUseCompiledData(usecompiledmorphdata);
        for(int i=0;i<maxshadertypes;i++) {
            // precreate all the necessary shaders
            shaders.add(shaderCreator.createShader(i));
        }
    }
    
    public boolean isShaderRelinked(int index) {
        return relinked[index];
    }
    
    public void setShaderIsRelinked(int index) {
        relinked[index]=true;
    }
}
