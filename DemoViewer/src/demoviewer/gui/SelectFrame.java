/*
 * SelectFrame.java
 *
 * Created on 2006. április 30., 13:16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.gui;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Line;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.GeomBatch;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.CullState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;
import demoviewer.map.GameObject;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author vear
 */
public class SelectFrame extends Line {
    
    private static Vector3f extent=new Vector3f();
    private static Vector3f center=new Vector3f();
    private static Vector3f wc=new Vector3f();
    private static Vector3f sc=new Vector3f();
    private static Vector2f sc2=new Vector2f();
    
    
    private Vector2f mins=new Vector2f();
    private Vector2f maxs=new Vector2f();
    
    private BoundingBox selBox;
    private boolean enabled=false;
    
    /** Creates a new instance of SelectFrame */
    public SelectFrame(String name, BoundingBox selBox) {
        super(name);
        this.selBox=selBox;
        //this.clearBatches();
        //TriangleBatch batch=new TriangleBatch();
        // create vertex buffer
        FloatBuffer vertBuff=BufferUtils.createFloatBuffer(8*3);
        //if(this.getBatchCount()==0)
         //   this.addBatch(new GeomBatch());
        this.getBatch(0).setVertexBuffer(vertBuff);
        // create index buffer
        IntBuffer idxBuffer=BufferUtils.createIntBuffer(6*2);
        idxBuffer.put(0).put(1);
        idxBuffer.put(1).put(2);
        idxBuffer.put(2).put(3);
        idxBuffer.put(4).put(5);
        idxBuffer.put(5).put(6);
        idxBuffer.put(6).put(7);
        idxBuffer.rewind();
        this.setIndexBuffer(idxBuffer);
        this.setVBOInfo(null);
        this.setRenderQueueMode(Renderer.QUEUE_ORTHO);
        setLineWidth(1f);
        setMode(Line.SEGMENTS);
        this.setStipplePattern((short)0xFFFF);
        this.setStippleFactor((short)0xFFFF);
        setAntialiased(false);
        //updateModelBound();
        setLocalTranslation(new Vector3f());
      /*
        CullState cs=DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_NONE);
        cs.setEnabled(true);
        setRenderState(cs);
       */
    }
    
    public void onDraw(Renderer r) {
        if(enabled) 
            draw(r);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled=enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean checkOnScreen(Renderer r) {
        // calculate screen coordinates of gameobjects bounding box
        if(selBox==null) {
            return false;
        }
        Camera c=r.getCamera();
        selBox.getExtent(extent);
        selBox.getCenter(center);
        mins.set(Float.MAX_VALUE, Float.MAX_VALUE);
        maxs.set(Float.MIN_VALUE, Float.MIN_VALUE);
        // get the edge points of the box
        wc.set(center.x-extent.x, center.y-extent.y, center.z-extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);

        wc.set(center.x-extent.x, center.y-extent.y, center.z+extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);
        
        wc.set(center.x-extent.x, center.y+extent.y, center.z-extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);
        
        wc.set(center.x-extent.x, center.y+extent.y, center.z+extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);
        
        wc.set(center.x+extent.x, center.y-extent.y, center.z-extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);
        
        wc.set(center.x+extent.x, center.y-extent.y, center.z+extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);
        
        wc.set(center.x+extent.x, center.y+extent.y, center.z-extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);
        
        wc.set(center.x+extent.x, center.y+extent.y, center.z+extent.z);
        c.getScreenCoordinates(wc, sc);
        mins.minLocal(sc2.set(sc)); maxs.maxLocal(sc2);

        if( maxs.x<0 || maxs.y<0 || mins.x>r.getWidth() || mins.y>r.getHeight()) {
            this.enabled=false;
            return false;
        }
        
        float width=((maxs.x-mins.x)/2f)*1.1f;
        float height=((maxs.y-mins.y)/2f)*1.1f;
        setLocalTranslation(getLocalTranslation().set((maxs.x+mins.x)/2f,(maxs.y+mins.y)/2f,0f));
        this.updateWorldVectors();
        // fill the buffer 
        FloatBuffer vb=this.getBatch(0).getVertexBuffer();
        vb.clear();
        //
        vb.put(-width).put(-height/3f).put(0);
        vb.put(-width).put(-height).put(0);
        
        //vb.put(-width).put(-height).put(0);
        vb.put(width).put(-height).put(0);
        
        //vb.put(width).put(-height).put(0);
        vb.put(width).put(-height/3f).put(0);
        
        vb.put(width).put(height/3f).put(0);
        vb.put(width).put(height).put(0);
        
        //vb.put(width).put(height).put(0);
        vb.put(-width).put(height).put(0);
        
        //vb.put(-width).put(height).put(0);
        vb.put(-width).put(height/3f).put(0);
        
        vb.rewind();
        
        this.enabled=true;
        return true;
    }
    
    public Vector2f getAreaMin() {
        return mins;
    }
    
    public Vector2f getAreaMax() {
        return maxs;
    }
}
