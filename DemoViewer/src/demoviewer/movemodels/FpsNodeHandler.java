/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package demoviewer.movemodels;

import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.RelativeMouse;
import demoviewer.movemodels.action.FpsKeyNodeLookDownAction;
import demoviewer.movemodels.action.FpsKeyNodeLookUpAction;
import com.jme.input.action.KeyNodeRotateLeftAction;
import com.jme.input.action.KeyNodeRotateRightAction;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import demoviewer.movemodels.action.FpsKeyNodeBackwardAction;
import demoviewer.movemodels.action.FpsKeyNodeForwardAction;
import demoviewer.movemodels.action.FpsKeyNodeStrafeLeftAction;
import demoviewer.movemodels.action.FpsKeyNodeStrafeRightAction;
import demoviewer.movemodels.action.FpsNodeMouseLook;

/**
 * <code>NodeHandler</code> defines an InputHandler that sets
 * a node that can be controlled via keyboard and mouse inputs. By default the
 * commands are, WSAD moves the node forward, backward and strafes. The
 * arrow keys rotate and tilt the node and the mouse also rotates and tilts
 * the node.
 * @author Mark Powell
 * @version $Id: NodeHandler.java,v 1.14 2006/01/13 19:39:27 renanse Exp $
 */
public class FpsNodeHandler extends InputHandler {

    FpsNodeMouseLook mouseLook;
    
    /**
     * Constructor instantiates a new <code>NodeHandler</code> object. The
     * application is set for the use of the exit action. The node is set to
     * control, while the api defines which input api is to be used.
     * @param node the node to control.
     * @param keySpeed action speed for key actions (move)
     * @param mouseSpeed action speed for mouse actions (rotate)
     */
    public FpsNodeHandler(Spatial node, float keySpeed, float mouseSpeed, MoveConstraint m, int lookButton) {

        setKeyBindings();
        setUpMouse(node, mouseSpeed, lookButton);
        setActions(node, keySpeed, mouseSpeed * 0.01f, m );
    }

    /**
     *
     * <code>setKeyBindings</code> binds the keys to use for the actions.
     */
    private void setKeyBindings() {
        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();

        keyboard.set("forward", KeyInput.KEY_W);
        keyboard.set("backward", KeyInput.KEY_S);
        keyboard.set("strafeLeft", KeyInput.KEY_A);
        keyboard.set("strafeRight", KeyInput.KEY_D);
        keyboard.set("lookUp", KeyInput.KEY_UP);
        keyboard.set("lookDown", KeyInput.KEY_DOWN);
        keyboard.set("turnRight", KeyInput.KEY_RIGHT);
        keyboard.set("turnLeft", KeyInput.KEY_LEFT);
    }

    /**
     *
     * <code>setUpMouse</code> sets the mouse look object.
     * @param node the node to use for rotations.
     * @param mouseSpeed
     */
    private void setUpMouse( Spatial node, float mouseSpeed, int lookButton ) {
        RelativeMouse mouse = new RelativeMouse("Mouse Input");
        mouse.registerWithInputHandler( this );

        mouseLook = new FpsNodeMouseLook(mouse, node, 0.1f, lookButton);
        mouseLook.setSpeed( mouseSpeed );
        mouseLook.setLockAxis(new Vector3f(0, 1, 0));
         
        addAction(mouseLook);
    }

    /**
     *
     * <code>setActions</code> sets the keyboard actions with the corresponding
     * key command.
     * @param node the node to control.
     * @param moveSpeed
     * @param turnSpeed
     */
    private void setActions( Spatial node, float moveSpeed, float turnSpeed, MoveConstraint m ) {
        addAction( new FpsKeyNodeForwardAction( node, moveSpeed, m ), "forward", true );
        addAction( new FpsKeyNodeBackwardAction( node, moveSpeed, m ), "backward", true );
        addAction( new FpsKeyNodeStrafeLeftAction( node, moveSpeed, m ), "strafeLeft", true );
        addAction( new FpsKeyNodeStrafeRightAction( node, moveSpeed, m ), "strafeRight", true );
        addAction( new FpsKeyNodeLookUpAction( node, turnSpeed ), "lookUp", true );
        addAction( new FpsKeyNodeLookDownAction( node, turnSpeed ), "lookDown", true );
        KeyNodeRotateRightAction rotateRight = new KeyNodeRotateRightAction( node, turnSpeed );
        rotateRight.setLockAxis( node.getLocalRotation().getRotationColumn( 1 ) );
        addAction( rotateRight, "turnRight", true );
        KeyNodeRotateLeftAction rotateLeft = new KeyNodeRotateLeftAction( node, turnSpeed );
        rotateLeft.setLockAxis( node.getLocalRotation().getRotationColumn( 1 ) );
        addAction( rotateLeft, "turnLeft", true );
    }
}
