/*
 * SelectionBox.java
 *
 * Created on 2006. április 19., 20:04
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.gui;

import com.jme.math.Vector2f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Line;
import com.jme.scene.VBOInfo;
import com.jme.scene.shape.Quad;
import com.jme.util.geom.BufferUtils;
import java.nio.FloatBuffer;

/**
 *
 * @author vear
 */
public class SelectionBox extends Quad {
    
    private Vector2f minP=new Vector2f();
    private Vector2f maxP=new Vector2f();
    
    private boolean enabled=false;
    
    /** Creates a new instance of SelectionBox */
    public SelectionBox(String name) {
        // create vertices
        /*      0       2
         *      
         *      1       3
         */ 
         super(name, 0, 0);
         //this.setLineWidth(1.0f);
         this.setVBOInfo(null);
         //this.setMode(Line.LOOP);
         //this.setStipplePattern((short)0xFFFE);
         this.setEnabled(false);
         this.setRenderQueueMode(Renderer.QUEUE_ORTHO);
         this.setDefaultColor(new ColorRGBA(0,0.1f,0f,0.3f));
    }
    
    
    public void setPosition(Vector2f minP, Vector2f maxP) {
        this.setLocalTranslation(this.getLocalTranslation().set((minP.x+maxP.x)/2f,(minP.y+maxP.y)/2f,0));
        this.resize((maxP.x-minP.x), (maxP.y-minP.y));
        this.minP.set(minP);
        this.maxP.set(maxP);
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void onDraw(Renderer r) {
        if(enabled) draw(r);
    }
    
    public boolean checkSelection(SelectFrame sf) {
        if(!enabled || minP==null || maxP==null) return false;
        Vector2f fmi=sf.getAreaMin();
        Vector2f fma=sf.getAreaMax();
        if(minP.x>fma.x || maxP.x<fmi.x || minP.y>fma.y || maxP.y<fmi.y)
            return false;
        return true;
    }
}
