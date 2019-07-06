/*
 * Main.java
 *
 * Created on 2006. január 4., 22:59
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer.test;

import com.jme.bounding.BoundingBox;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.CameraNode;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;
import demoviewer.DemoBaseGame;
import demoviewer.gui.SelectFrame;
import demoviewer.movemodels.FpsNodeHandler;
import demoviewer.movemodels.MoveConstraint;
import demoviewer.n3di.nl3DiFileLoader3;
import demoviewer.render.ShaderManager2;
import demoviewer.render.ShaderedMesh;
import demoviewer.resource.ResourceManager;
import java.nio.FloatBuffer;
import java.util.ArrayList;


/**
 *
 * @author vear
 */
public class TestModelViewer extends DemoBaseGame {
    
  private Quaternion rotQuat = new Quaternion();
  private float angle = 0;
  private Vector3f axis = new Vector3f(1, 1, 0);
  ShaderedMesh t;
  ArrayList<ShaderedMesh> m;
  private CameraNode camNode;
  
    /** Creates a new instance of Main */
    public TestModelViewer() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        TestModelViewer app = new TestModelViewer();       
        app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
        app.start();
    }
   
    ShaderManager2 sm;
    
  protected Node createXMark(Vector3f position, float size, float scale) {
      FloatBuffer verts=BufferUtils.createFloatBuffer(3*2*3);
      verts.clear();
      verts.put(0).put(0).put(0);
      verts.put(size).put(0).put(0);
      verts.put(0).put(0).put(0);
      verts.put(0).put(size).put(0);
      verts.put(0).put(0).put(0);
      verts.put(0).put(0).put(size);
      verts.rewind();
      FloatBuffer colors=BufferUtils.createFloatBuffer(3*2*4);
      // red X
      colors.put(1).put(0).put(0).put(1);
      colors.put(1).put(0).put(0).put(1);
      // green Y
      colors.put(0).put(1).put(0).put(1);
      colors.put(0).put(1).put(0).put(1);
      // blue Z
      colors.put(0).put(0).put(1).put(1);
      colors.put(0).put(0).put(1).put(1);
      colors.rewind();
      Line l=new Line("X Mark", verts, null, colors, null);
      l.setModelBound(new BoundingBox());
      l.setLineWidth(1f);
      l.setMode(Line.SEGMENTS);
      l.setAntialiased(false);
      l.updateModelBound();
      l.setLocalTranslation(position);
      //l.setSolidColor(ColorRGBA.green);
      Node n=new Node("X Mark");
      
      CullState cs=display.getRenderer().createCullState();
      cs.setCullMode(CullState.CS_NONE);
      cs.setEnabled(true);
      n.setRenderState(cs);
      
      n.attachChild(l);
      n.updateRenderState();
      n.setLocalScale(scale);
      n.updateWorldData(0);
      n.setLightCombineMode(LightState.OFF);
      n.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
      return n;
  }
  
  SelectFrame sf;
  protected SelectFrame createFrameBox(BoundingBox box) {
      sf=new SelectFrame("frame", box);
      sf.setEnabled(true);
      return sf;
  }
  
  /**
   *
   * builds the trimesh.
   * @see com.jme.app.SimpleGame#initGame()
   */
  protected void simpleInitGame() {
    display.setTitle("3Di loader test");
    cam.setLocation(new Vector3f(0f, 20f, 0f));
       cam.setDirection(new Vector3f(0f, -10f, 0f));
       cam.setUp(new Vector3f(0f, 0f, -1f));
    camNode = new CameraNode("Camera Node", cam);
    camNode.setLocalTranslation(new Vector3f(50f, 0f, 0f));
    camNode.updateWorldData(0);
    rootNode.attachChild(camNode);
    lightState.detachAll();
    // directional light
    DirectionalLight dl = new DirectionalLight();
    dl.setDiffuse(new ColorRGBA(1f, 1f, 1f, 1f));
    dl.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
    dl.setDirection(new Vector3f(-0.5f, -0.5f, 0f));
    dl.setEnabled(true);
    lightState.attach(dl);
    // point light
    PointLight pl=new PointLight();
    pl.setDiffuse(new ColorRGBA(1f, 1f, 1f, 1.0f));
    pl.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
    pl.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
    pl.setLocation(new Vector3f(0,30,-30));
    pl.setEnabled(true);
    //lightState.attach(pl);
        
    // create the shader factory
    sm=ResourceManager.getInstance().getShaderManager();
    // set in static data
    sm.setCamNode(camNode);
    sm.setPointLight(pl);
    sm.preloadShaders();
    
    //for(int ldcnt=0;ldcnt<1;ldcnt++)
    //{//m.lods.size()
    nl3DiFileLoader3 ldr=new nl3DiFileLoader3();
    m=ldr.load("extracted/DAAV1.3di");
    //m=ldr.load("extracted/Fsldr01.3di");
    //m=ldr.load("C:\\temp\\joripped\\demoview\\"+"extracted/Armry02.3di");
    //m=ldr.load("C:\\temp\\joripped\\demoview\\"+"extracted/Stilts01.3di");
    //m=ldr.load("C:\\temp\\joripped\\demoview\\"+"extracted/JHut1.3di");
    //m=ldr.load("C:\\temp\\joripped\\demoview\\"+"extracted/Dtower1.3di");
    //m=ldr.load("C:\\l\\jomod\\bhdmodtools\\Sample Files\\"+"shed_up.3di");
    //m=ldr.load("C:\\l\\jomod\\bhdmodtools\\Sample Files\\"+"shed_down.3di");
    //m=ldr.load("C:\\l\\jomod\\bhdmodtools\\Sample Files\\"+"shed_ground.3di");
    // get the mesh
    t = m.get(0);
    float scale=10f;
    t.setLocalScale(scale);
    System.out.println("FF Tex units: "+TextureState.getNumberOfFixedUnits()
        +" Vertex shader Texture units "+TextureState.getNumberOfVertexUnits()
        +" Fragment shader Texture units "+TextureState.getNumberOfFragmentUnits());
        
    // create a star shape at reference point
    Vector3f refp = t.getGroundPoint();
     
    CullState cs=display.getRenderer().createCullState();
    //cs.setCullMode(CullState.CS_NONE);
    cs.setCullMode(CullState.CS_BACK);
    t.setRenderState(cs);
    rootNode.setRenderState(cs);
    
    t.setRenderQueueMode(Renderer.QUEUE_SKIP);
    rootNode.attachChild(t);
    t.updateRenderState();
    t.setModelBound(new BoundingBox());
    t.updateModelBound();
    t.updateWorldBound();
    t.lockTransforms();
    t.lockBounds();
    guiNode.attachChild(this.createFrameBox((BoundingBox) t.getWorldBound()));
    rootNode.attachChild(this.createXMark(refp, 20f, scale));
    

    // ensure required shaders exi
    //sm.preloadShaders(t);

    input = new FpsNodeHandler(camNode, 300, 1, new MoveConstraint(), -1);
    display.getRenderer().setBackgroundColor(new ColorRGBA(0.5f,0.5f,0.5f,1));
    rootNode.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
    rootNode.updateRenderState();
  }
  
  protected void simpleRender() {
      Renderer r = display.getRenderer();
      sf.checkOnScreen(r);
  }
  
  protected void simpleUpdate() {
    
      /*
      if (timer.getTimePerFrame() < 1) {
      angle = angle + (timer.getTimePerFrame() * 1);
      if (angle > 360)
        angle = 0;
    }
       */
      
    sm.update();
    //rotQuat.fromAngleAxis(angle, axis);
    //t.setLocalRotation(rotQuat);
  }
  
}
