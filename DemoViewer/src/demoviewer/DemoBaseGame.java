/*
  * Refactored to not instanciate FirstPersonHandler
 */

package demoviewer;

import com.jme.app.AbstractGame;
import com.jme.image.Texture;
import com.jme.scene.state.AlphaState;
import java.util.logging.Level;
import com.jme.input.InputHandler;
import com.jme.input.InputSystem;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.MouseInput;
import com.jme.input.joystick.JoystickInput;
import com.jme.light.DirectionalLight;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.Text;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.WireframeState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.system.JmeException;
import com.jme.system.PropertiesIO;
import com.jme.system.lwjgl.LWJGLPropertiesDialog;
import com.jme.util.LoggingSystem;
import com.jme.util.Timer;
import com.jme.util.geom.Debugger;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class DemoBaseGame {

    // stuff from abstractgame
    
        /** Flag for running the system. */
    protected boolean finished;

    private final static String JME_VERSION_TAG = "jME version 0.10";
    private final static String DEFAULT_IMAGE = "/jmetest/data/images/Monkey.png";

    /** Never displays a <code>PropertiesDialog</code> on startup, using defaults
     * if no configuration file is found. */
    protected final static int NEVER_SHOW_PROPS_DIALOG = 0;

    /** Displays a <code>PropertiesDialog</code> only if the properties file is not
     * found or could not be loaded. */
    protected final static int FIRSTRUN_OR_NOCONFIGFILE_SHOW_PROPS_DIALOG = 1;

    /** Always displays a <code>PropertiesDialog</code> on startup. */
    protected final static int ALWAYS_SHOW_PROPS_DIALOG = 2;

    //Default to first-run-only behaviour
    private int dialogBehaviour = FIRSTRUN_OR_NOCONFIGFILE_SHOW_PROPS_DIALOG;
    private URL dialogImage = null;

    /** Game display properties. */
    protected PropertiesIO properties;

    /** Renderer used to display the game */
    protected DisplaySystem display;

    //
    //Utility methods common to all game implementations
    //

    /**
     * <code>getVersion</code> returns the version of the API.
     * @return the version of the API.
     */
    public String getVersion() {
        return JME_VERSION_TAG;
    }

    /**
     * <code>assertDisplayCreated</code> determines if the display system
     * was successfully created before use.
     * @throws JmeException if the display system was not successfully created
     */
    protected void assertDisplayCreated() throws JmeException {
        if (display == null) {
            LoggingSystem.getLogger().log(Level.SEVERE, "Display system is null.");

            throw new JmeException("Window must be created during" + " initialization.");
        }
        if (!display.isCreated()) {
            LoggingSystem.getLogger().log(Level.SEVERE, "Display system not initialized.");

            throw new JmeException("Window must be created during" + " initialization.");
        }
    }

    /**
     * <code>setDialogBehaviour</code> defines if and when the display properties
     * dialog should be shown. Setting the behaviour after <code>start</code> has
     * been called has no effect.
     * @param behaviour properties dialog behaviour ID
     */
    public void setDialogBehaviour(int behaviour) {
        URL url = null;
        try {
            url = AbstractGame.class.getResource(DEFAULT_IMAGE);
        } catch (Exception e) {
            LoggingSystem.getLogger().throwing(getClass().toString(), "setDialogBehavior(int)", e);
        }
        if ( url != null ) {
            setDialogBehaviour( behaviour, url );
        }
        else {
            setDialogBehaviour( behaviour, DEFAULT_IMAGE );
        }
    }

    /**
     * <code>setDialogBehaviour</code> defines if and when the display properties
     * dialog should be shown as well as its accompanying image. Setting the
     * behaviour after <code>start</code> has been called has no effect.
     * @param behaviour properties dialog behaviour ID
     * @param image a String specifying the filename of an image to be displayed
     *                       	  with the <code>PropertiesDialog</code>. Passing <code>null</code>
     *                       	  will result in no image being used.
     */
    public void setDialogBehaviour(int behaviour, String image){
        if ( behaviour < NEVER_SHOW_PROPS_DIALOG || behaviour > ALWAYS_SHOW_PROPS_DIALOG ) {
            throw new IllegalArgumentException( "No such properties dialog behaviour" );
        }

        dialogBehaviour = behaviour;

        URL file = null;
        try {
            file = new URL("file:" + image);
        } catch (MalformedURLException e) {}
        dialogImage = file;
    }

    /**
     *
     * <code>setDialogBehaviour</code> sets how the properties dialog should
     * appear. ALWAYS_SHOW_PROPS, NEVER_SHOW_PROPS and FIRSTRUN_OR_NOCONFIGFILE
     * are the three valid choices. The url of an image file is also used so
     * you can customize the dialog.
     * @param behaviour ALWAYS_SHOW_PROPS, NEVER_SHOW_PROPS and
     *      FIRSTRUN_OR_NOCONFIGFILE are the valid choices.
     * @param image the image to display in the box.
     */
    public void setDialogBehaviour(int behaviour, URL image){
        if ( behaviour < NEVER_SHOW_PROPS_DIALOG || behaviour > ALWAYS_SHOW_PROPS_DIALOG ) {
            throw new IllegalArgumentException( "No such properties dialog behaviour" );
        }

        dialogBehaviour = behaviour;
        dialogImage = image;
    }

    /**
     * <code>getAttributes</code> attempts to first obtain the properties
     * information from the "properties.cfg" file, then a dialog depending
     * on the dialog behaviour.
     */
    protected void getAttributes() {
        properties = new PropertiesIO("properties.cfg");
        boolean loaded = properties.load();

        if ((!loaded && dialogBehaviour == FIRSTRUN_OR_NOCONFIGFILE_SHOW_PROPS_DIALOG)
            || dialogBehaviour == ALWAYS_SHOW_PROPS_DIALOG) {

            LWJGLPropertiesDialog dialog = new LWJGLPropertiesDialog(properties, dialogImage);

            while (dialog.isVisible()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    LoggingSystem.getLogger().log(Level.WARNING, "Error waiting for dialog system, using defaults.");
                }
            }

            if (dialog.isCancelled()) {
                //System.exit(0);
                finish();
            }
        }
    }
    
    /**
     * <code>finish</code> breaks out of the main game loop. It is preferable to
     * call <code>finish</code> instead of <code>quit</code>.
     */
    public void finish() {
      finished = true;
    }
    
    // stuff from BaseGame
    
      /**
       * The simplest main game loop possible: render and update as fast as
       * possible.
       */
      public final void start() {
        LoggingSystem.getLogger().log(Level.INFO, "Application started.");
        try {
          getAttributes();

          if (!finished) {
              initSystem();

              assertDisplayCreated();

              initGame();

              //main loop
              while (!finished && !display.isClosing()) {
                //handle input events prior to updating the scene
                // - some applications may want to put this into update of the game state
                InputSystem.update();

                //update game state, do not use interpolation parameter
                update( -1.0f);

                //render, do not use interpolation parameter
                render( -1.0f);

                //swap buffers
                //display.getRenderer().displayBackBuffer();

                Thread.yield();
              }
            }
        }
        catch(Throwable t) {
          t.printStackTrace();
        }

        cleanup();
        LoggingSystem.getLogger().log(Level.INFO, "Application ending.");

        if (display != null)
                display.reset();
        quit();
      }

      /**
       * Closes the display
       * @see AbstractGame#quit()
       */
      protected void quit() {
          if (display != null)
              display.close();
          // added from basegame
          System.exit(0);
      }
  
          /** The camera that we see through. */
    protected Camera cam;

    /** The root of our normal scene graph. */
    protected Node rootNode;

    /** Handles our mouse/keyboard input. */
    protected InputHandler input;

    /** High resolution timer for jME. */
    protected Timer timer;

    /** The root node of our text. */
    protected Node fpsNode;

    /** Displays all the lovely information at the bottom. */
    protected Text fps;

    /** Alpha bits to use for the renderer. */
    protected int alphaBits = 0;

    /** Depth bits to use for the renderer. */
    protected int depthBits = 8;

    /** Stencil bits to use for the renderer. */
    protected int stencilBits = 0;

    /** Number of samples to use for the multisample buffer. */
    protected int samples = 0;
    
    /**
     * Simply an easy way to get at timer.getTimePerFrame(). Also saves time so
     * you don't call it more than once per frame.
     */
    protected float tpf;

    /** True if the renderer should display the depth buffer. */
    protected boolean showDepth = false;

    /** True if the renderer should display bounds. */
    protected boolean showBounds = false;

    /** True if the rnederer should display normals. */
    protected boolean showNormals = false;

    /** A wirestate to turn on and off for the rootNode */
    protected WireframeState wireState;

    /** A lightstate to turn on and off for the rootNode */
    protected LightState lightState;

    /** Location of the font for jME's text at the bottom */
    public static String fontLocation = Text.DEFAULT_FONT;

    /** This is used to display print text. */
    protected StringBuffer updateBuffer = new StringBuffer(30);

    /** This is used to recieve getStatistics calls. */
    protected StringBuffer tempBuffer = new StringBuffer();

    protected boolean pause;

    /** Do show the FPS counter? */
    protected boolean showFPS=true;
    
    protected Node guiNode;
    
    /**
     * Updates the timer, sets tpf, updates the input and updates the fps
     * string. Also checks keys for toggling pause, bounds, normals, lights,
     * wire etc.
     * 
     * @param interpolation
     *            unused in this implementation
     * @see AbstractGame#update(float interpolation)
     */
    protected void update(float interpolation) {
        /** Recalculate the framerate. */
        timer.update();
        /** Update tpf to time per frame according to the Timer. */
        tpf = timer.getTimePerFrame();
        /** Check for key/mouse updates. */
        input.update(tpf);

        updateBuffer.setLength(0);
        updateBuffer.append("FPS: ").append((int) timer.getFrameRate()).append(
                " - ");
        updateBuffer.append(display.getRenderer().getStatistics(tempBuffer));
        /** Send the fps to our fps bar at the bottom. */
        fps.print(updateBuffer);

        /** If toggle_pause is a valid command (via key p), change pause. */
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "toggle_pause", false)) {
            pause = !pause;
        }

        /** If toggle_wire is a valid command (via key T), change wirestates. */
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "toggle_wire", false)) {
            wireState.setEnabled(!wireState.isEnabled());
            rootNode.updateRenderState();
        }
        /** If toggle_lights is a valid command (via key L), change lightstate. */
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "toggle_lights", false)) {
            lightState.setEnabled(!lightState.isEnabled());
            rootNode.updateRenderState();
        }
        /** If toggle_bounds is a valid command (via key B), change bounds. */
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "toggle_bounds", false)) {
            showBounds = !showBounds;
        }
        /** If toggle_depth is a valid command (via key F3), change depth. */
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "toggle_depth", false)) {
            showDepth = !showDepth;
        }
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "toggle_normals", false)) {
            showNormals = !showNormals;
        }
        /** If camera_out is a valid command (via key C), show camera location. */
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "camera_out", false)) {
            System.err.println("Camera at: "
                    + display.getRenderer().getCamera().getLocation());
        }

        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "screen_shot", false)) {
            display.getRenderer().takeScreenShot("SimpleGameScreenShot");
        }
        /*
        if (KeyBindingManager.getKeyBindingManager().isValidCommand(
                "parallel_projection", false)) {
            if (cam.isParallelProjection()) {
                cameraPerspective();
            } else {
                cameraParallel();
            }
        }
         */

        if (KeyBindingManager.getKeyBindingManager().isValidCommand("exit",
                false)) {
            finish();
        }

        if (pause)
            return;
        
        /** Call simpleUpdate in any derived classes of SimpleGame. */
        simpleUpdate();

        /** Update controllers/render states/transforms/bounds for rootNode. */
        rootNode.updateGeometricState(tpf, true);
    }

    /**
     * Clears stats, the buffers and renders bounds and normals if on.
     * 
     * @param interpolation
     *            unused in this implementation
     * @see AbstractGame#render(float interpolation)
     */
    protected void render(float interpolation) {
        Renderer r = display.getRenderer();
        /** Reset display's tracking information for number of triangles/vertexes */
        r.clearStatistics();
        /** Clears the previously rendered information. */
        r.clearBuffers();
        
        /**
         * If showing bounds, draw rootNode's bounds, and the bounds of all its
         * children.
         */
        if (showBounds)
            Debugger.drawBounds(rootNode, r, true);

        if (showNormals)
            Debugger.drawNormals(rootNode, r);

        /** Draw the rootNode and all its children. */
        rootNode.draw(r);
        
        /** Call simpleRender() in any derived classes. */
        simpleRender();
        
        if(guiNode!=null) {
            r.draw(guiNode);
        }
        
        if(showFPS) {
            /** Draw the fps node to show the fancy information at the bottom. */
            r.draw(fpsNode);
        }
        
        if (showDepth) {
            r.renderQueue();
            Debugger.drawBuffer(Texture.RTT_SOURCE_DEPTH, Debugger.NORTHEAST, r);
        }
        r.displayBackBuffer();
    }
    
    /**
     * Creates display, sets up camera, and binds keys. Called in
     * BaseGame.start() directly after the dialog box.
     * 
     * @see AbstractGame#initSystem()
     */
    protected void initSystem() {
        try {
            /**
             * Get a DisplaySystem acording to the renderer selected in the
             * startup box.
             */
            display = DisplaySystem.getDisplaySystem(properties.getRenderer());
            
            display.setMinDepthBits(depthBits);
            display.setMinStencilBits(stencilBits);
            display.setMinAlphaBits(alphaBits);
            display.setMinSamples(samples);
            
            /** Create a window with the startup box's information. */
            display.createWindow(properties.getWidth(), properties.getHeight(),
                    properties.getDepth(), properties.getFreq(), properties
                            .getFullscreen());
            display.setRenderer(new demoviewer.render.DemoRenderer(display.getWidth(),display.getHeight()));
            display.updateStates(display.getRenderer());
            /**
             * Create a camera specific to the DisplaySystem that works with the
             * display's width and height
             */
            cam = display.getRenderer().createCamera(display.getWidth(),
                    display.getHeight());

        } catch (JmeException e) {
            /**
             * If the displaysystem can't be initialized correctly, exit
             * instantly.
             */
            e.printStackTrace();
            System.exit(1);
        }

        /** Set a black background. */
        display.getRenderer().setBackgroundColor(ColorRGBA.black);

        /** Set up how our camera sees. */
        cameraPerspective();
        Vector3f loc = new Vector3f(0.0f, 0.0f, 25.0f);
        Vector3f left = new Vector3f(-1.0f, 0.0f, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f dir = new Vector3f(0.0f, 0f, -1.0f);
        /** Move our camera to a correct place and orientation. */
        cam.setFrame(loc, left, up, dir);
        /** Signal that we've changed our camera's location/frustum. */
        cam.update();
        /** Assign the camera to this renderer. */
        display.getRenderer().setCamera(cam);


        /** Get a high resolution timer for FPS updates. */
        timer = Timer.getTimer(properties.getRenderer());

        /** Sets the title of our display. */
        display.setTitle("SimpleGame");
        /**
         * Signal to the renderer that it should keep track of rendering
         * information.
         */
        display.getRenderer().enableStatistics(true);

        /** Assign key P to action "toggle_pause". */
        KeyBindingManager.getKeyBindingManager().set("toggle_pause",
                KeyInput.KEY_P);
        /** Assign key T to action "toggle_wire". */
        KeyBindingManager.getKeyBindingManager().set("toggle_wire",
                KeyInput.KEY_T);
        /** Assign key L to action "toggle_lights". */
        KeyBindingManager.getKeyBindingManager().set("toggle_lights",
                KeyInput.KEY_L);
        /** Assign key B to action "toggle_bounds". */
        KeyBindingManager.getKeyBindingManager().set("toggle_bounds",
                KeyInput.KEY_B);
        /** Assign key N to action "toggle_normals". */
        KeyBindingManager.getKeyBindingManager().set("toggle_normals",
                KeyInput.KEY_N);
        /** Assign key C to action "camera_out". */
        KeyBindingManager.getKeyBindingManager().set("camera_out",
                KeyInput.KEY_C);
        KeyBindingManager.getKeyBindingManager().set("screen_shot",
                KeyInput.KEY_F1);
        KeyBindingManager.getKeyBindingManager().set("exit",
                KeyInput.KEY_ESCAPE);
        KeyBindingManager.getKeyBindingManager().set("parallel_projection",
                KeyInput.KEY_F2);
        KeyBindingManager.getKeyBindingManager().set("toggle_depth",
                KeyInput.KEY_F3);
    }

    protected void cameraPerspective() {
        cam.setFrustumPerspective(45.0f, (float) display.getWidth()
                / (float) display.getHeight(), 1, 1000);
        cam.setParallelProjection(false);
        cam.update();
    }

    protected void cameraParallel() {
        cam.setParallelProjection(true);
        cam.setParallelProjection(true);
        float aspect = (float) display.getWidth() / display.getHeight();
        cam.setFrustum(-100, 1000, -50 * aspect, 50 * aspect, -50, 50);
        cam.update();
    }
    
    /**
     * Creates rootNode, lighting, statistic text, and other basic render
     * states. Called in BaseGame.start() after initSystem().
     * 
     * @see AbstractGame#initGame()
     */
    protected void initGame() {
        /** Create rootNode */
        rootNode = new Node("rootNode");

        /**
         * Create a wirestate to toggle on and off. Starts disabled with default
         * width of 1 pixel.
         */
        wireState = display.getRenderer().createWireframeState();
        wireState.setEnabled(false);
        rootNode.setRenderState(wireState);

        /**
         * Create a ZBuffer to display pixels closest to the camera above
         * farther ones.
         */
        ZBufferState buf = display.getRenderer().createZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.CF_LEQUAL);
        rootNode.setRenderState(buf);

        // Then our font Text object.
        /** This is what will actually have the text at the bottom. */
        fps = Text.createDefaultTextLabel("FPS label");
        fps.setCullMode(Spatial.CULL_NEVER);
        fps.setTextureCombineMode(TextureState.REPLACE);

        // Finally, a stand alone node (not attached to root on purpose)
        fpsNode = new Node("FPS node");
        fpsNode.setRenderState(fps.getRenderState(RenderState.RS_ALPHA));
        fpsNode.setRenderState(fps.getRenderState(RenderState.RS_TEXTURE));
        fpsNode.attachChild(fps);
        fpsNode.setCullMode(Spatial.CULL_NEVER);

        /** Set up light. */
      
        DirectionalLight light = new DirectionalLight();
        light.setEnabled(true);
        light.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setDirection(new Vector3f(0.5f, -0.5f, 0));

        /*
        PointLight light = new PointLight();
        light.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3f(100, 100, 100));
        light.setEnabled(true);
        */
        
        /** Attach the light to a lightState and the lightState to rootNode. */
        lightState = display.getRenderer().createLightState();
        lightState.setEnabled(true);
        lightState.attach(light);
        rootNode.setRenderState(lightState);

        // create gui node
        createGui();
        
        /** Let derived classes initialize. */
        simpleInitGame();

        timer.reset();
        
        /**
         * Update geometric and rendering information for both the rootNode and
         * fpsNode.
         */
        rootNode.updateGeometricState(0.0f, true);
        rootNode.updateRenderState();
        fpsNode.updateGeometricState(0.0f, true);
        fpsNode.updateRenderState();
        guiNode.updateGeometricState(0.0f, true);
        guiNode.updateRenderState();
    }

    protected void createGui() {
        guiNode = new Node("Gui Node");
        guiNode.setRenderQueueMode(Renderer.QUEUE_ORTHO);
        guiNode.setTextureCombineMode(TextureState.OFF);
        guiNode.setLightCombineMode(LightState.OFF);
        guiNode.setCullMode(Spatial.CULL_NEVER);
        ZBufferState zstate=display.getRenderer().createZBufferState();
        zstate.setWritable(false);
        zstate.setEnabled(false);
        guiNode.setRenderState(zstate);
        guiNode.setZOrder(0);
        AlphaState as = display.getRenderer().createAlphaState();
        as.setEnabled(true);
        as.setBlendEnabled(true);
        /*
        as.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        as.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
         */
        guiNode.setRenderState(as);
        guiNode.setLightCombineMode(LightState.OFF);
    }
    
    /**
     * Called near end of initGame(). Must be defined by derived classes.
     */
    protected abstract void simpleInitGame();

    /**
     * Can be defined in derived classes for custom updating. Called every frame
     * in update.
     */
    protected void simpleUpdate() {
    }

    /**
     * Can be defined in derived classes for custom rendering.
     * Called every frame in render.
     */
    protected void simpleRender() {
    }

    /**
     * unused
     * @see AbstractGame#reinit()
     */
    protected void reinit() {
    }

    /**
     * Cleans up the keyboard.
     * @see AbstractGame#cleanup()
     */
    protected void cleanup() {
        LoggingSystem.getLogger().log(Level.INFO, "Cleaning up resources.");

        KeyInput.destroyIfInitalized();
        MouseInput.destroyIfInitalized();
        JoystickInput.destroyIfInitalized();
    }
    
}
