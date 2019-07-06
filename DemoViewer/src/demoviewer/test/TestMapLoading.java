

package demoviewer.test;

import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.CameraNode;
import com.jme.scene.Spatial;
import com.jme.scene.state.CullState;
import com.jme.scene.state.FogState;
import com.jme.util.LoggingSystem;
import demoviewer.Config;
import demoviewer.DemoBaseGame;
import demoviewer.gui.SelectionBox;
import demoviewer.map.ClientMessage;
import demoviewer.map.GameClient;
import demoviewer.map.SmartCamera;
import demoviewer.movemodels.RtsInputHandler;
import demoviewer.render.ShaderManager2;
import demoviewer.resource.ResourceManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import demoviewer.map.MapHeader;
import demoviewer.map.loaders.NpjLoader;
import demoviewer.terrain.SectoredTerrain2;


/**
 * <code>TestTerrainPage</code>
 *
 * @author Mark Powell
 * @version $Id: TestTerrainPage.java,v 1.31 2005/10/15 13:23:05 irrisor Exp $
 */
public class TestMapLoading extends DemoBaseGame {

    private Logger _log = LoggingSystem.getLogger();
  private SmartCamera camNode;


  /**
   * Entry point for the test,
   *
   * @param args
   */
  public static void main(String[] args) {
    TestMapLoading app = new TestMapLoading();
    app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
    app.start();
  }

  ResourceManager mgr;
 MapHeader hdr; 
          
 private void loadMap() {
     
     NpjLoader nl=new NpjLoader();
     hdr.setDumpedRefpointMode(false);
     
     //nl.loadNpj(hdr, "C:\\vear_fun\\job\\mdmp\\dumpacks\\referencedormantvolcano\\ASH_I5Aref.NPJ");
     
     //nl.loadNpj(hdr, "C:\\vear_fun\\job\\mymaps\\coordtest.npj");
     //nl.loadNpj(hdr, "C:\\vear_fun\\job\\mymaps\\vehicles.npj");
     nl.loadNpj(hdr, "C:\\vear_fun\\job\\mymaps\\AS-KaroBOTZ.npj"); 
     
     //hdr.setDumpedRefpointMode(true);
     
     //nl.loadNpj(hdr, "C:\\vear_fun\\job\\mymaps\\envtest.npj"); 
     //nl.loadNpj(hdr, "C:\\vear_fun\\job\\mymaps\\botmozgas5.npj");
     //nl.loadNpj(hdr, "C:\\vear_fun\\job\\mdmp\\raw\\joe\\aas\\ASH_G11A.NPJ");
 }
  
  GameClient gc;
        
  protected void simpleInitGame() {
      LoggingSystem.getLogger().setLevel(Level.OFF);
      mgr=ResourceManager.getInstance();
      mgr.parseItemsDef();
      hdr=new MapHeader(rootNode);
      
      // create a new local client
      GameClient gc=new GameClient(0,3);
      gc.setCanControl(1,true);
      ClientMessage cm=new ClientMessage(ClientMessage.LOGIN_LOCAL);
      cm.setClient(gc);
      hdr.addClientMessage(cm);

      gc=hdr.getLocalClient();
      if(gc==null) {
          LoggingSystem.getLogger().log(Level.WARNING, "No local login possible");
          this.finish();
          return;
      }
      gc.setGuiNode(guiNode);
      
          rootNode.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
          fpsNode.setRenderQueueMode(Renderer.QUEUE_ORTHO);

        cam.setFrustum(1.0f, Config.modelSightdistance, -0.55f, 0.55f, 0.4125f, -0.4125f);
        cam.update();

        camNode = new SmartCamera("Camera Node", cam);
        //camNode.setLocalTranslation(new Vector3f(-6983.3555f, 913.74896f, -1335.8132f));
        //camNode.setLocalTranslation(new Vector3f(12.054109f, 1176.1604f, 1091.8995f));
        camNode.setLocalTranslation(new Vector3f(0f, 500f, 0f));
        //
        camNode.updateWorldData(0);

        rootNode.attachChild(camNode);

        display.setTitle("Map Loading Test");
        display.getRenderer().setBackgroundColor(new ColorRGBA(0.5f,0.5f,0.5f,1));

        camNode.lookAt(new Vector3f(0f,500f,0f), new Vector3f(0,0,1));

        camNode.updateWorldData(0);

        CullState cs = display.getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);
        cs.setEnabled(true);
        rootNode.setRenderState(cs);
        
        lightState.detachAll();
        DirectionalLight dl = new DirectionalLight();
        dl.setDiffuse(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        dl.setAmbient(new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f));
        dl.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f));
        dl.setEnabled(true);
        lightState.attach(dl);
        PointLight pl=new PointLight();
        pl.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        pl.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        pl.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
        pl.setLocation(new Vector3f(10000,10000,10000));
        lightState.attach(pl);

        if(Config.modelRender==1) {
            mgr.getShaderManager().setCamNode(camNode);
            mgr.getShaderManager().setPointLight(pl);
            mgr.getShaderManager().preloadShaders();
        }
        
    loadMap();

    // create the selection box quad
    
    
    // create the input handler
    input = new RtsInputHandler(camNode, rootNode, gc, 300, 1, hdr.getTerrain(), 16f);

    rootNode.setCullMode(Spatial.CULL_NEVER);
  }

  protected void simpleUpdate()
  {
      camNode.updateWorldData(tpf);
      Vector3f cm=cam.getLocation();
      SectoredTerrain2 strn=hdr.getTerrain();
      if(strn!=null) {
          strn.updateLod(cam);
      }
      ShaderManager2 sm=mgr.getShaderManager();
      sm.update();
      super.simpleUpdate();

  }
  
}
