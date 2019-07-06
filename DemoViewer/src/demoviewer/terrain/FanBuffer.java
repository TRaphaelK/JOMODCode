/*
 * FanBuffer.java
 *
 * Created on 2006. március 18., 12:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import com.jme.util.geom.BufferUtils;
import java.nio.IntBuffer;

/**
 *
 * @author vear
 */
public class FanBuffer implements GeomBuffer {
    
    IntBuffer buf;
    TriangleBuffer tb;
    boolean dbg;
    int v1,v2,v3;
    boolean start=false;
    
    /** Creates a new instance of FanBuffer */
    public FanBuffer(int buffersize, boolean dumptriangles) {
        buf=BufferUtils.createIntBuffer(buffersize);
        buf.clear();
        dbg=dumptriangles;
        tb=new TriangleBuffer(buffersize, dumptriangles);
    }

    public FanBuffer put(int p1, int p2, int p3) {
        if(!start) {
            v1=p1; v2=p2; v3=p3;
            buf.put(p1).put(p2).put(p3);
            tb.put(p1,p2,p3);
            start=true;
            return this;
        }
        return null;
    }
    
    public FanBuffer put(int p1, int p2) {
        if(!start) {
            v1=p1; v3=p2;
            buf.put(p1).put(p2);
            start=true;
            return this;
        }
        return null;
    }
    
    public FanBuffer put(int p1) {
        if(start) {
            v2=v3; v3=p1;
            buf.put(p1);
            tb.put(v1,v2,v3);
            return this;
        }
        return null;
    }
    
    public IntBuffer finishAndGetBuffer() {
        buf.limit(buf.position());
        IntBuffer br=buf;
        buf=null;
        return br;
    }

    public IntBuffer getTriangleBuffer() {
        return tb.getTriangleBuffer();
    }
    
}
