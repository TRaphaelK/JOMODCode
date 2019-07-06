/*
 * RtsMouseSelect.java
 *
 * Created on 2006. április 19., 19:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.movemodels.action;

import com.jme.input.AbsoluteMouse;
import com.jme.input.KeyInput;
import com.jme.input.MouseInput;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.MouseInputAction;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.CameraNode;
import com.jme.scene.Node;
import demoviewer.map.GameClient;
import demoviewer.movemodels.RtsInputHandler;
import demoviewer.gui.SelectionBox;

/**
 *
 * @author vear
 */
public class RtsMouseSelect extends MouseInputAction {
    
    private AbsoluteMouse am;
    private CameraNode camera;
    RtsInputHandler handler;
    private float shotTime = 0;
    private float clicktime;
    boolean clicked=false;
    boolean held=false;
    boolean dragged=false;
    // current screen position
    Vector2f screenPos = new Vector2f();
    // min, max positions in selecting
    Vector2f spos = new Vector2f();
    Vector2f minPos = new Vector2f();
    Vector2f maxPos = new Vector2f();
    SelectionBox selectBox;
    
    /** Creates a new instance of RtsMouseSelect */
    public RtsMouseSelect(RtsInputHandler handler, AbsoluteMouse am, SelectionBox selectBox) {
        this.handler=handler;
        this.am=am;
        this.selectBox=selectBox;
        selectBox.setEnabled(false);
        selectBox.setPosition(minPos, maxPos);
    }

    public void performAction(InputActionEvent evt) {
        shotTime += evt.getTime();
        
        // check for camera click
        
        if(MouseInput.get().isButtonDown(0)) {
            clicktime+=evt.getTime();
            if( !clicked ) {
                cameraPress();
            }
            if(clicktime>0.1f) {
                // we have a cameramousedrag
                cameraMouseDrag(evt);
            }
            clicked=true;
        } else {
            if(!dragged) {
                // single fast click
                cameraClick();
            }
            // clear out 
            clicktime=0;
            clicked=false;
            held=false;
            dragged=false;
            selectBox.setEnabled(false);
        }
    }

    private void cameraPress() {
    }
    
    private void cameraClick() {
    }

    private void cameraMouseDrag(InputActionEvent evt) {

        screenPos.set(am.getHotSpotPosition());
        if(!held) {
            spos.set(screenPos);
            minPos.set(screenPos);
            maxPos.set(screenPos);
            held=true;
        } else {
            if(!dragged) {
                if(screenPos.x!=spos.x || screenPos.y!=spos.y) {
                    // started drag
                    // if shift is not down, clear current selection
                    if(!KeyInput.get().isKeyDown(KeyInput.KEY_LSHIFT)) {
                        GameClient gc=handler.getGameClient();
                        if(gc!=null) {
                            gc.clearSelected();
                        }
                    }
                    dragged=true;
                    selectBox.setEnabled(true);
                }
            }
            minPos.set(Math.min(spos.x,screenPos.x),Math.min(spos.y,screenPos.y));
            maxPos.set(Math.max(spos.x,screenPos.x),Math.max(spos.y,screenPos.y));
            selectBox.setPosition(minPos, maxPos);
        }
    }
    
}
