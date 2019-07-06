/*
 * RtsInputHandler.java
 *
 * Created on 2006. február 15., 21:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.movemodels;

import com.jme.image.Texture;
import com.jme.input.AbsoluteMouse;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.RelativeMouse;
import com.jme.input.action.KeyNodeRotateLeftAction;
import com.jme.input.action.KeyNodeRotateRightAction;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.CameraNode;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import demoviewer.gui.SelectionBox;
import demoviewer.map.GameClient;
import demoviewer.movemodels.action.FpsKeyNodeBackwardAction;
import demoviewer.movemodels.action.FpsKeyNodeForwardAction;
import demoviewer.movemodels.action.FpsKeyNodeLookDownAction;
import demoviewer.movemodels.action.FpsKeyNodeLookUpAction;
import demoviewer.movemodels.action.FpsKeyNodeStrafeLeftAction;
import demoviewer.movemodels.action.FpsKeyNodeStrafeRightAction;
import demoviewer.movemodels.action.FpsNodeMouseLook;
import demoviewer.movemodels.action.RtsMousePick;
import demoviewer.movemodels.action.RtsMouseSelect;
import demoviewer.terrain.Terrain;
import java.net.URL;

/**
 *
 * @author vear
 */
public class RtsInputHandler extends InputHandler {
    
    AbsoluteMouse am;
    private CameraNode camNode;
    Terrain t;
    RtsMousePick pick;
    FpsNodeMouseLook look;
    // the target for movement and rotation
    Vector3f tloc;
    Quaternion trot;
    // movement speed of the camera
    float movespeed;
    
    FpsNodeMouseLook mouseLook;
        
    // the rootnode for picking
    private Node rootNode;
    // to rootnode for selecting
    private Node selectNode;
    // the input handler for selecting
    RtsMouseSelect select;
    SelectionBox selectBox;
    
    // the colliding movemodel
    private MoveConstraint m;
    
    private GameClient gc;
    
    /** Creates a new instance of RtsInputHandler */
    public RtsInputHandler(CameraNode node, Node rootNode, GameClient gc, float keySpeed, float mouseSpeed, Terrain t, float hoverHeight) {
        this.camNode=node;
        this.t=t;
        this.rootNode=rootNode;
        this.gc=gc;
        selectBox = new SelectionBox("Selector");
        if(gc!=null) {
            gc.getGuiNode().attachChild(selectBox);
            gc.setSelectionBox(selectBox);
        }
        
        // create mouse navigator
        createMousePointer();
        rootNode.attachChild(am);
        tloc=new Vector3f(camNode.getWorldTranslation());
        trot=new Quaternion(camNode.getWorldRotation());
        pick = new RtsMousePick(this, rootNode, am, tloc, trot);
        this.addAction(pick);
        select=new RtsMouseSelect(this, am, selectBox);
        this.addAction(select);
        // create the movement mmodel
        m = new TerrainCollidingMoveModel(t, hoverHeight);
    
        //create fps controls
        setKeyBindings();
        setUpLookMouse(node, mouseSpeed, 1);
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
    private void setUpLookMouse( Spatial node, float mouseSpeed, int lookButton ) {
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
    
    private void createMousePointer() {
        DisplaySystem display=DisplaySystem.getDisplaySystem();
        // Create a new mouse. Restrict its movements to the display screen.
        am = new AbsoluteMouse("The Mouse", display.getWidth(), display
                        .getHeight());

        // Get a picture for my mouse.
        TextureState ts = display.getRenderer().createTextureState();
        URL cursorLoc;
        cursorLoc = RtsInputHandler.class.getClassLoader().getResource(
                        "demoviewer/movemodels/cursor1.png");
        Texture t = TextureManager.loadTexture(cursorLoc, Texture.MM_LINEAR,
                        Texture.FM_LINEAR);
        ts.setTexture(t);
        am.setRenderState(ts);

        // Make the mouse's background blend with what's already there
        AlphaState as = display.getRenderer().createAlphaState();
        as.setBlendEnabled(true);
        as.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        as.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
        as.setTestEnabled(true);
        as.setTestFunction(AlphaState.TF_GREATER);
        am.setRenderState(as);

        // Move the mouse to the middle of the screen to start with
        am.setLocalTranslation(new Vector3f(display.getWidth() / 2, display
                        .getHeight() / 2, 0));
            // Assign the mouse to an input handler
        am.registerWithInputHandler( this );
    }
    
    public void update(float time) {
        super.update(time);
        Vector3f cl=camNode.getLocalTranslation();
        Quaternion cr=camNode.getLocalRotation();
        boolean moved=false;
        
        if(m.isAutoCamera()) {
            if(!cr.equals(trot)) {
                // turn on to face the block
                float steps=FastMath.sqrt(FastMath.abs(cl.distance(tloc)-tloc.y/2));
                if(steps>0) {
                    cr.slerp(trot,1/steps);
                } else {
                    cr.set(trot);
                }
                moved=true;
            }
            // move and rotate camera to target
            if(!cl.equals(tloc)) {
                if(cl.distance(tloc)<tloc.y) {
                    tloc.set(cl);
                } else {
                    camNode.setLocalTranslation(
                            m.isMoveAllowed(
                                camNode.getLocalTranslation(), 
                                cl.addLocal(((tloc.x-cl.x)/2)*time, 
                                        ((tloc.y-cl.y)/2)*time, 
                                         ((tloc.z-cl.z)/2)*time)));
                }
                moved=true;
            }
            m.setAutocamera(moved);
        }
    }

    public CameraNode getCameraNode() {
        return camNode;
    }

    public void setCameraNode(CameraNode camNode) {
        this.camNode = camNode;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }

    public MoveConstraint getMoveModel() {
        return m;
    }

    public void setMoveModel(MoveConstraint m) {
        this.m = m;
    }

    public Terrain getTerrain() {
        return t;
    }
    
    public GameClient getGameClient() {
        return gc;
    }
    
    public void setGameClient(GameClient gc) {
        this.gc=gc;
    }
}
