/*
 * SmartCamera.java
 *
 * Created on 2006. április 28., 17:36
 *
 * Handles the Camera movement and commands, manages the
 * guiNode
 */

package demoviewer.map;

import com.jme.input.InputHandler;
import com.jme.renderer.Camera;
import com.jme.scene.CameraNode;
import com.jme.scene.Node;
import demoviewer.gui.SelectionBox;
import demoviewer.movemodels.RtsInputHandler;

/**
 *
 * @author vear
 */
public class SmartCamera extends CameraNode {
    
    /** Creates a new instance of SmartCamera */
    
    public SmartCamera(String name, Camera cam) {
        super(name, cam);
    }    
    
}
