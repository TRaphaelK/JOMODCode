/*
 * AbstractMoveModel.java
 *
 * Created on 2006. február 14., 22:54
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.movemodels;

import com.jme.math.Vector3f;

/**
 *
 * @author vear
 */
public class MoveConstraint {
    // is auto camera move on
    private boolean autocamera;
    
    public Vector3f isMoveAllowed(Vector3f prev, Vector3f loc) {
        return loc;
    }
    
    public boolean isAutoCamera() {
        return autocamera;
    }

    public void setAutocamera(boolean autocamera) {
        this.autocamera = autocamera;
    }
    
}
