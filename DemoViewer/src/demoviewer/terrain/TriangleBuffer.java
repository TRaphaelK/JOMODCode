/*
 * TriangleBuffer.java
 *
 * Created on 2006. március 18., 12:16
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
public class TriangleBuffer implements GeomBuffer {
    
    IntBuffer buf;
    boolean dbg=false;
    
    /** Creates a new instance of TriangleBuffer */
    public TriangleBuffer(int buffersize, boolean dumptriangles) {
        buf=BufferUtils.createIntBuffer(buffersize);
        buf.clear();
        dbg=dumptriangles;
    }

    public boolean put(int p1, int p2, int p3) {
        if(p1==p2 || p1==p3 || p2==p3) {
            if(dbg) {
                System.out.print("Degenerate triangle ");
                System.out.print(p1);System.out.print(" ");
                System.out.print(p2);System.out.print(" ");
                System.out.print(p3);
            }
            // ignore degenerate triangles
            return false;
        }
        if(dbg) {
            System.out.println("Triangle "+p1+","+p2+","+p3);
        }
        buf.put(p1).put(p2).put(p3);
        return true;
    }
    
    public IntBuffer finishAndGetBuffer() {
        buf.limit(buf.position());
        IntBuffer br=buf;
        buf=null;
        return br;
    }

    public IntBuffer getTriangleBuffer() {
        return finishAndGetBuffer();
    }
    
}
