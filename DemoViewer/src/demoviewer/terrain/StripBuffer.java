/*
 * StripBuffer.java
 *
 * Created on 2006. március 14., 17:16
 *
 * Helps in stripifiing
 */

package demoviewer.terrain;

import com.jme.util.geom.BufferUtils;
import java.nio.IntBuffer;

/**
 *
 * @author vear
 */
public class StripBuffer implements GeomBuffer {
    
    // buffer for the strip
    IntBuffer buf;
    
    // GeomBuffer for triangles
    TriangleBuffer tb;
    
    /* Order of triangles:
     *  0 A	B	C
        1 C	B	D
        2 C	D	E
        3 E	D	F
        4 E	F	G
        5 G	F	H
        G	H	I
        I	H	J
        I	J	K
        K	J	L
        K	L	M
        M	L	N
        M	N	O
        O	N	P
     *  
     */
    // the state:
    // 0- start (v1,v2,v3)
    // 1- v3, v2, v4
    // 2- v3, v4, v1
    // 3- v1, v4, v2
    // 4- v1, v2, v3 (loops back to 1)
    // The third row is always the new index
    
    // If the buffer receives a pattern out of order, it tryes to skip to
    // possible pattern, by inserting degenerate indices/triangles
    // current state: 1 v1, v2, v3 target state p1,p2,p3
    // put v2, 
    // 1- v3, v2, v3
    // 2- v3, v3, p2
    // 3- p2, v3, p2
    // 4- p2, p2, p1
    // 1- p1, p2, p3
    
    // state 2: v3, v2, v4 target state p1,p2,p3
    // 2- v3, v4, v4
    // 3- v4, v4, p1
    // 4- v4, p1, p1
    // 1- p1, p1, p2
    // 2- p1, p2, p3
    
    // state 3: v3, v4, v1 target state p1,p2,p3
    // 3- v1, v4, v1
    // 4- v1, v1, p2
    // 1- p2, v1, p2
    // 2- p2, p2, p1
    // 3- p1, p2, p3
    
    // state 4: v1, v4, v2 target state p1,p2,p3
    // 4- v1, v2, v2
    // 1- v2, v2, p1
    // 2- v2, p1, p1
    // 3- p1, p1, p2
    // 4- p1, p2, p3
    int state=0;
    int v1,v2,v3,v4;
    int last;
    boolean dbg;
    
    /** Creates a new instance of StripBuffer */
    public StripBuffer(int buffersize) {
        this(buffersize, false);
    }
    
    public StripBuffer(int buffersize, boolean dumptriangles) {
        buf=BufferUtils.createIntBuffer(buffersize);
        dbg=dumptriangles;
        buf.clear();
        tb=new TriangleBuffer(buffersize, dumptriangles);
    }
    
    private void set(int t0, int t1, int t2) {
        tb.put(t0,t1,t2);
    }
    
    public StripBuffer put(int p3) {
        if(state==0) return null;
        else {
            last = p3;
            if(state==1) {
                v4=p3;
                set(v3, v2, v4);
            }
            else if(state==2) {
                v1=p3;
                set(v3, v4, v1);
            }
            else if(state==3) {
                v2=p3;
                set(v1, v4, v2);
            }
            else if(state==4) {
                v3=p3;
                set(v1, v2, v3);
            }
            buf.put(p3);
            state++;
            if(state==5) state=1;
        }
        return this;
    }
    
    public StripBuffer put(int p1, int p2, int p3) {
        if(state==0) {
            // just put in everything
            v1=p1;v2=p2;v3=p3;
            set(v1,v2,v3);
            buf.put(v1).put(v2).put(v3);
            state++;
            return this;
        }/* else if(state==1) {
            // p1=v3, p2=v2, p3 new
            if(p1==v3 && p2==v2) {
                return put(p3);
            }
        } else if(state==2) {
            // p1=v3, p2=v4 p3 new
            if(p1==v3 && p2==v4) {
                return put(p3);
            }
        } else if(state==3) {
            // p1=v1, p2=v4, p3 new
            if(p1==v1 && p2==v4) {
                return put(p3);
            }
        } else if(state==4) {
            // p1=v1, p2=v2, p3 new
            if(p1==v1 && p2==v2) {
                return put(p3);
            }
        }
          */
        // could not put as required,
        // reach requested state by putting in degenerate triangles
        if(state==1) {
            put(v3); put(p2); put(p2); put(p1); put(p3);
            return this;
        } else if(state==2) {
            put(v4); put(p1); put(p1); put(p2); put(p3);
            return this;
        } else if(state==3) {
            put(v1); put(p2); put(p2); put(p1); put(p3);
            return this;
        } else if(state==4) {
            put(v2); put(p1); put(p1); put(p2); put(p3);
            return this;
        }
        return null;
    }
    
    public StripBuffer repeat() {
        return put(last);
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
