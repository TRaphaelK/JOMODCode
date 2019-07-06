/*
 * GeomBuffer.java
 *
 * Created on 2006. március 18., 11:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import java.nio.IntBuffer;

/**
 *
 * @author vear
 */
public interface GeomBuffer {
    
    // return the collected data as implemented (list, strip, fan)
    public IntBuffer finishAndGetBuffer();
    
    // returns the collected triangles as triange buffer
    public IntBuffer getTriangleBuffer();
}
