
package demoviewer.test;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.CameraNode;
import com.jme.scene.Node;
import com.jme.scene.state.CullState;
import com.jme.util.LoggingSystem;
import demoviewer.DemoBaseGame;
import demoviewer.movemodels.RtsInputHandler;
import demoviewer.gui.SelectionBox;
import demoviewer.render.ShaderManager2;
import demoviewer.resource.ResourceManager;
import demoviewer.terrain.SectoredTerrain2;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TestTerrainLoading extends DemoBaseGame {

    private Logger _log = LoggingSystem.getLogger();
  private CameraNode camNode;

  /**
   * Entry point for the test,
   *
   * @param args
   */
  public static void main(String[] args) {
    TestTerrainLoading app = new TestTerrainLoading();
    app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
    app.start();
  }

  SectoredTerrain2 strn=null;
  private void createTerrain() {
      try {
        ResourceManager mgr=ResourceManager.getInstance();
        //strn=new SectoredTerrain2("Dvxi4_d.trn");
        //strn=new SectoredTerrain2("Dvxi5.trn");
        //strn=new SectoredTerrain2("Dvxc1.trn");
        strn=new SectoredTerrain2("Dvxg8.trn");
        strn.buildTerrain(null);
        rootNode.attachChild(strn);
        
      } catch(Exception e) {
          _log.log(Level.WARNING,"Cannot build terrain",e);
      }
  }
  
  float speed=0;
  Node mdl;
  ShaderManager2 sm;
          
  private void createModel() {
      // test a model loading
      ResourceManager mgr=ResourceManager.getInstance();
      sm=mgr.getShaderManager();
      
      mgr.parseItemsDef();
      
      // prepared, request a model
      mdl=mgr.getObjectInstance(1,2070, null);
      mdl.setLocalTranslation(new Vector3f(100,500,0));
      rootNode.attachChild(mdl);
  }
  
  /**
   * builds the trimesh.
   *
   * @see com.jme.app.SimpleGame#initGame()
   */
  protected void simpleInitGame() {
      rootNode.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
      fpsNode.setRenderQueueMode(Renderer.QUEUE_ORTHO);
   
    cam.setFrustum(1.0f, 30000.0f, -0.55f, 0.55f, 0.4125f, -0.4125f);
    cam.update();

    camNode = new CameraNode("Camera Node", cam);
    ResourceManager.getInstance().getShaderManager().setCamNode(camNode);
    //camNode.setLocalTranslation(new Vector3f(-6983.3555f, 913.74896f, -1335.8132f));
    //camNode.setLocalTranslation(new Vector3f(12.054109f, 1176.1604f, 1091.8995f));
    camNode.setLocalTranslation(new Vector3f(0f, 500f, 0f));
    
    //
    camNode.updateWorldData(0);
    
    rootNode.attachChild(camNode);
    
    display.setTitle("Terrain Loading Test");
    display.getRenderer().setBackgroundColor(new ColorRGBA(0.5f,0.5f,0.5f,1));
    
    camNode.updateWorldData(0);

    CullState cs = display.getRenderer().createCullState();
    cs.setCullMode(CullState.CS_BACK);
    cs.setEnabled(true);
    rootNode.setRenderState(cs);
    
    createTerrain();
    
    createModel();
    simpleUpdate();
    
    input = new RtsInputHandler(camNode, rootNode, null, 300, 1, strn, 16f);
    
    rootNode.updateRenderState();
    rootNode.updateWorldBound();
  }

  protected void simpleUpdate() {
      camNode.updateWorldData(tpf);
      Vector3f cm=cam.getLocation();
      if(strn!=null) {
          strn.updateLod(cam);
      }
      if(sm!=null) {
          sm.update();
      }
  }
  
}
