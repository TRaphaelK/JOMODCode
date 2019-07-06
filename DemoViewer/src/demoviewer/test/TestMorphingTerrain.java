
package demoviewer.test;

import com.jme.image.Texture;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.CameraNode;
import com.jme.scene.Spatial;
import com.jme.scene.Text;
import com.jme.scene.state.CullState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.LoggingSystem;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainPage;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.RawHeightMap;
import demoviewer.DemoBaseGame;
import demoviewer.movemodels.RtsInputHandler;
import demoviewer.terrain.MorphingTerrainManager;
import demoviewer.terrain.MorphingTerrainPage;
import demoviewer.terrain.Terrain;
import demoviewer.terrain.TerrainUtils;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TestMorphingTerrain extends DemoBaseGame {

    private Logger _log = LoggingSystem.getLogger();
  private CameraNode camNode;
private TerrainPage page;

  /**
   * Entry point for the test,
   *
   * @param args
   */
  public static void main(String[] args) {
    TestMorphingTerrain app = new TestMorphingTerrain();
    if(args!=null && args.length>0) {
        for(int i=0;i<args.length;i++)
            if(args[i].toLowerCase().equals("raw")) {
                app.terrain=1;
            } else if(args[i].toLowerCase().equals("nomorph")) {
                System.out.println("Morphing disabled");
                app.domorph=false;
            } else if(args[i].toLowerCase().equals("attrarray")) {
                System.out.println("Using shader attribute arrays");
                app.usecompiledmorphdata=false;
            } else if(args[i].toLowerCase().equals("novbo")) {
                System.out.println("VBO disabled");
                app.usevbo=false;
            }
    }
    app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
    app.start();
  }

  MorphingTerrainManager mgr;
  MorphingTerrainPage pg;
  public boolean domorph=true;
  boolean usenormap=false;
  boolean useblendmap=false;
  boolean usecolormap=true;
  boolean usestrips=false;
  boolean usecompiledmorphdata=true;
  boolean usevbo=true;
  int[] heightMap;
  // generated heightmap
  int[] genHeightMap;
  // loaded raw heightmap
  int[] rawHeightMap;
  Vector3f terrainScale;
  int blocksize=129;
  float lodstep=1;
  boolean alphaqueue=false;
  public int terrain=0;
  float maxfilter=-1;
  float filter=1.0f;
  float prevfilter=0;
  
  private void createTerrainPage() {
      if(mgr==null) mgr=MorphingTerrainManager.getInstance();
      
      switch(terrain) {
          case 0: {
              if(genHeightMap==null) {
                  genHeightMap = TerrainUtils.subSize(new MidPointHeightMap(2048, 0.9f).getHeightMap(),1025);
              }
              heightMap=genHeightMap;
          } break;
          case 1: {
              if(rawHeightMap==null) {
                    rawHeightMap = new RawHeightMap("media/kauai1025_8.raw", 1025, RawHeightMap.FORMAT_8BIT, false).getHeightMap();  
              }
              heightMap=rawHeightMap;
          } break;
      }

    if(terrainScale==null) terrainScale = new Vector3f(32,8,32);//new Vector3f(100,20,100);
      // clear out previous data if there was
    mgr.clearIndexes();
    if(pg!=null) rootNode.detachChild(pg);
    pg=null;
    
    System.gc();
    
    mgr.getShaderCreator().setUseMorphing(domorph);
    mgr.setUseVBO(usevbo);
    mgr.setUseCompiledMorphData(usecompiledmorphdata);
    mgr.setRenderQueueMode((alphaqueue?Renderer.QUEUE_TRANSPARENT:Renderer.QUEUE_SKIP));
    mgr.setUseStrips(usestrips);
    
    recreateTerrain();
    
    pg=new MorphingTerrainPage("Terrain", blocksize, 1025, terrainScale, heightMap, Terrain.BLOCK | Terrain.MORPH | Terrain.SHARED, mgr);

    pg.setLocalTranslation(new Vector3f(0,0,0));
    //pg.updateModelBound();
    //pg.setLocalTranslation(new Vector3f(0,0,0));
    pg.setRenderState(cs);
    pg.setRenderState(ts);
    pg.setRenderState(m);
    pg.updateRenderState();

    //pg.updateRenderState();
    pg.updateGeometricState(0,false);
    pg.lockTransforms();
    pg.lockBounds();
    rootNode.attachChild(pg);
    rootNode.setRenderState(ts);
    rootNode.updateRenderState();
  }
   
  protected void recreateTerrain() {
      
      createTexture();
      // set terrain attributes
      if(usenormap)
        mgr.getShaderCreator().setUseNormalMap(1);
      else
        mgr.getShaderCreator().setUseNormalMap(-1);
      mgr.getShaderCreator().setUseColormap(0);
      mgr.setUseStrips(usestrips);
      if(useblendmap) {
        mgr.getShaderCreator().setDetailsCount(2);//2
      } else {
        mgr.getShaderCreator().setDetailsCount(0);//2
      }
      mgr.getShaderCreator().setDetailTiling(128);
      mgr.getShaderCreator().setColorToDetailRatio(2f);
      mgr.setLodStepDistance(lodstep*terrainScale.x);
      mgr.getShaderCreator().setUsePredefinedShaders(true);
      mgr.setDynamicShaderSwitch(true);
      mgr.createShaders();
  }
  
  Texture t1;
  Texture tg;
  Texture tw;
  Texture t2=null;
  Texture t3;
  Texture t4;
  Texture t5;
  MaterialState m;
  TextureState ts;
  int texunits=-1;
  
  protected void createTexture() {
    if(ts==null) {
        ts = display.getRenderer().createTextureState();
        maxfilter=ts.getMaxAnisotropic();
        ts.setEnabled(true);        
        texunits=ts.getNumberOfUnits();
       LoggingSystem.getLogger().log(Level.INFO, "Supported texture units "+texunits+" max aniso "+maxfilter);
    }
    if(texunits<5) {
        // not enough tex units for detail
        useblendmap=false;
    }
    if(texunits<2) {
        // not enough tex units for normal map
        usenormap=false;
    }    
    if(usecolormap) {
        if(terrain==1) {
            if(t1==null || prevfilter!=filter) {
                    t1 = TextureManager.loadTexture(
                    "media/kauai_2048_2.jpg",
                    Texture.MM_NEAREST_NEAREST,
                    Texture.FM_LINEAR,
                    filter,
                    true);
                t1.setApply(Texture.AM_REPLACE);
                t1.setCombineFuncRGB(Texture.ACF_MODULATE);
                t1.setCombineSrc0RGB(Texture.ACS_TEXTURE);
                t1.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
                t1.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
                t1.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
                t1.setCombineScaleRGB(1.0f);
                t1.setWrap(Texture.WM_WRAP_S_WRAP_T);
            }
            //t1.setAnisoLevel(filter);
            //t1.setNeedsFilterRefresh(true);
            ts.setTexture(t1, 0);
        } else {
            if(tg==null || prevfilter!=filter) {
                tg = TextureManager.loadTexture(
                "media/grassb.png",
                Texture.MM_LINEAR_LINEAR,
                Texture.FM_LINEAR,
                filter,
                true);
                tg.setApply(Texture.AM_REPLACE);
                tg.setCombineFuncRGB(Texture.ACF_MODULATE);
                tg.setCombineSrc0RGB(Texture.ACS_TEXTURE);
                tg.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
                tg.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
                tg.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
                tg.setCombineScaleRGB(1.0f);
                tg.setWrap(Texture.WM_WRAP_S_WRAP_T);
            }
            //tg.setAnisoLevel(filter);
            //tg.setNeedsFilterRefresh(true);
            ts.setTexture(tg, 0);
        }
    } else {
        if(tw==null) {
            tw = TextureManager.loadTexture(
            "media/white.png",
            Texture.MM_NONE,
            Texture.FM_LINEAR);
        }
        ts.setTexture(tw, 0);
    }
    //t1.setAnisoLevel(ts.getMaxAnisotropic());
    if(texunits>1 ) {
        if(t2==null) {
            t2=new Texture();
            t2.setImage(TerrainUtils.getUpRightLeftDownAlteratingNormalMap(heightMap, terrainScale, 1024, 3));
            t2.setCorrection(Texture.CM_PERSPECTIVE);
            t2.setFilter(Texture.FM_LINEAR);
            t2.setMipmapState(Texture.MM_NEAREST_NEAREST);
            t2.setWrap(Texture.WM_WRAP_S_WRAP_T);
            t2.setApply(Texture.AM_REPLACE);
            t2.setCombineFuncRGB(Texture.ACF_ADD_SIGNED);
            t2.setCombineSrc0RGB(Texture.ACS_TEXTURE);
            t2.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
            t2.setCombineSrc1RGB(Texture.ACS_PREVIOUS);
            t2.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
            t2.setCombineScaleRGB(1.0f);
        }
        ts.setTexture(t2, 1);
    }
    /*
    if(usenormap) {
        //t2.setAnisoLevel(1);
        //t2.setNeedsFilterRefresh(true);
        
    }
     */
  
    if(texunits>4) {
      if(t3==null) {
            // load blendmap
            t3 = TextureManager.loadTexture(
                "media/kauai_blendmap.png",
                Texture.MM_LINEAR_LINEAR,
                Texture.FM_LINEAR,
                filter,
                true);
            t3.setApply(Texture.AM_REPLACE);
            t3.setCombineFuncRGB(Texture.ACF_MODULATE);
            t3.setCombineSrc0RGB(Texture.ACS_TEXTURE);
            t3.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
            t3.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
            t3.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
            t3.setCombineScaleRGB(1.0f);
            t3.setWrap(Texture.WM_WRAP_S_WRAP_T);
      }
      //t1.setAnisoLevel(ts.getMaxAnisotropic());
      ts.setTexture(t3, 2);
        // two detail maps
      if(t4==null) {
        // rock
        t4 = TextureManager.loadTexture(
            "media/dirt_darkedges_mk_.dds",
            Texture.MM_LINEAR_LINEAR,
            Texture.FM_LINEAR,
            filter,
            false);
        t4.setApply(Texture.AM_REPLACE);
        t4.setCombineFuncRGB(Texture.ACF_MODULATE);
        t4.setCombineSrc0RGB(Texture.ACS_TEXTURE);
        t4.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
        t4.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
        t4.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
        t4.setCombineScaleRGB(1.0f);
        t4.setWrap(Texture.WM_WRAP_S_WRAP_T);
      }
      //t1.setAnisoLevel(ts.getMaxAnisotropic());
      ts.setTexture(t4, 3);
      if(t5==null) {
        // grass
        t5 = TextureManager.loadTexture(
            "media/roughsand.png",
            Texture.MM_LINEAR_LINEAR,
            Texture.FM_LINEAR,
            filter,
            false);
        t5.setApply(Texture.AM_REPLACE);
        t5.setCombineFuncRGB(Texture.ACF_MODULATE);
        t5.setCombineSrc0RGB(Texture.ACS_TEXTURE);
        t5.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
        t5.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
        t5.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
        t5.setCombineScaleRGB(1.0f);
        t5.setWrap(Texture.WM_WRAP_S_WRAP_T);
      }
      //t1.setAnisoLevel(ts.getMaxAnisotropic());
      ts.setTexture(t5, 4);
    }

    if(m==null) {
        m = display.getRenderer().createMaterialState();
        m.setAmbient(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
        m.setDiffuse(new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f));
    }

    //ts.apply();
    prevfilter=filter;
  }

  DirectionalLight dl;
  
  private void createLight() {
    lightState.detachAll();
    dl = new DirectionalLight();
    dl.setDiffuse(new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f));
    dl.setAmbient(new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
    dl.setDirection(new Vector3f(-1.0f, -1.0f, 0f));
    dl.setEnabled(true);
    lightState.attach(dl);
    PointLight pl=new PointLight();
    pl.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
    pl.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
    pl.setSpecular(new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
    pl.setLocation(new Vector3f(10000,10000,10000));
    lightState.attach(pl);
    pg.setRenderState(lightState);
    pg.updateRenderState();
  }
  
  CullState cs;
          
  /**
   *
   * @see com.jme.app.SimpleGame#initGame()
   */
  protected void simpleInitGame() {
      if(!DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState().isSupported()) {
          LoggingSystem.getLogger().log(Level.SEVERE, "GLSL Not supported");
          System.exit(1);
      }
      rootNode.setRenderQueueMode(Renderer.QUEUE_OPAQUE);
      fpsNode.setRenderQueueMode(Renderer.QUEUE_ORTHO);
   
    cam.setFrustum(1.0f, 30000.0f, -0.55f, 0.55f, 0.4125f, -0.4125f);
    cam.update();

    camNode = new CameraNode("Camera Node", cam);
    //camNode.setLocalTranslation(new Vector3f(-6983.3555f, 913.74896f, -1335.8132f));
    //camNode.setLocalTranslation(new Vector3f(12.054109f, 1176.1604f, 1091.8995f));
    camNode.setLocalTranslation(new Vector3f(0f, 1000f, 0f));
    
    camNode.updateWorldData(0);
    
    rootNode.attachChild(camNode);
    
    display.setTitle("Morphing Terrain Test");
    display.getRenderer().setBackgroundColor(new ColorRGBA(0.5f,0.5f,0.5f,1));
    //camNode.lookAt(new Vector3f(0f,500f,0f), new Vector3f(0,0,1));

    cs = display.getRenderer().createCullState();
    cs.setCullMode(CullState.CS_BACK);
    cs.setEnabled(true);
    
    rootNode.setRenderState(cs);
    createTerrainPage();
    //createTerrainBlock();
    
    //camNode.lookAt(tb.getWorldTranslation(), new Vector3f(0,0,1));
    camNode.updateWorldData(0);
    
    //createModel();
    //camNode.lookAt(mdl.getWorldTranslation(), new Vector3f(0,0,1));
    createLight();
    
    //input = new FpsNodeHandler(camNode, 300, 1, m);
    input = new RtsInputHandler(camNode, rootNode, null, 300, 1, pg, 30f);
    
    rootNode.updateRenderState();
    lodt = Text.createDefaultTextLabel("LOD label");
    lodt.setCullMode(Spatial.CULL_NEVER);
    lodt.setTextureCombineMode(TextureState.REPLACE);
    lodt.setLocalTranslation(new Vector3f(-10,20,0));
    fpsNode.attachChild(lodt);
    
    KeyBindingManager.getKeyBindingManager().set("blockdown", KeyInput.KEY_1);
    KeyBindingManager.getKeyBindingManager().set("blockup", KeyInput.KEY_2);
    KeyBindingManager.getKeyBindingManager().set("loddown", KeyInput.KEY_3);
    KeyBindingManager.getKeyBindingManager().set("lodup", KeyInput.KEY_4);
    KeyBindingManager.getKeyBindingManager().set("switch_colormap", KeyInput.KEY_5);
    KeyBindingManager.getKeyBindingManager().set("switch_normal", KeyInput.KEY_6);
    KeyBindingManager.getKeyBindingManager().set("switch_strips", KeyInput.KEY_7);
    KeyBindingManager.getKeyBindingManager().set("switch_attrarrays", KeyInput.KEY_8);
    //KeyBindingManager.getKeyBindingManager().set("switch_queuemode", KeyInput.KEY_9);
    KeyBindingManager.getKeyBindingManager().set("switch_daynight", KeyInput.KEY_0);
    //KeyBindingManager.getKeyBindingManager().set("switch_filter", KeyInput.KEY_F);
    //KeyBindingManager.getKeyBindingManager().set("switch_terrain", KeyInput.KEY_G);
  }
  
  float time=16f;
  float cycle=24f;
  protected Text lodt;
  boolean daynight=false;
  boolean started=false;
          
  protected void simpleUpdate() {
      super.simpleUpdate();
     if (KeyBindingManager.getKeyBindingManager().isValidCommand("blockdown", false)) {
          // get previous state
          blocksize=((blocksize-1)/2)+1;
          if(lodstep<=17) blocksize=17;
          // rebuild
          createTerrainPage();
     }
     if (KeyBindingManager.getKeyBindingManager().isValidCommand("blockup", false)) {
          // get previous state
          blocksize=((blocksize-1)*2)+1;
          if(blocksize>=1025) blocksize=1025;
          // rebuild
          createTerrainPage();
      }
     if (KeyBindingManager.getKeyBindingManager().isValidCommand("loddown", false)) {
          // get previous state
          lodstep--;
          if(lodstep<1) lodstep=1;
          // rebuild
          recreateTerrain();
     }
     if (KeyBindingManager.getKeyBindingManager().isValidCommand("lodup", false)) {
          // get previous state
          lodstep++;
          if(lodstep>128) lodstep=128;
          // rebuild
          recreateTerrain();
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_colormap", false)) {
          // get previous state
          if(texunits>4) {
              if(usecolormap && useblendmap) {
                  usecolormap=false;
                  useblendmap=false;
              } else if(usecolormap) {
                    useblendmap=!useblendmap;
              } else {
                  usecolormap=true;
              }
          } else {
              usecolormap=!usecolormap;
          }
          
          // rebuild
          recreateTerrain();
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_normal", false)) {
          // get previous state
          usenormap=!usenormap;
          // rebuild
          recreateTerrain();
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_strips", false)) {
          // get previous state
          usestrips=!usestrips;
          // rebuild
          recreateTerrain();
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_daynight", false)) {
          // get previous state
          daynight=!daynight;
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_attrarrays", false)) {
          // get previous state
          usecompiledmorphdata=!usecompiledmorphdata;
          createTerrainPage();
      }
      /*
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_queuemode", false)) {
          // get previous state
          alphaqueue=!alphaqueue;
          createTerrainPage();
      }
       */
      /*
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_filter", false)) {
          // get previous state
          filter+=1f;
          if(filter>this.maxfilter) filter=0;
          ts=null;
          // rebuild
          recreateTerrain();
      }
       
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_terrain", false)) {
          // get previous state
          terrain++;
          if(terrain>1) terrain=0;
          // rebuild
          t2=null;
          createTerrainPage();
      }
      */
    updateBuffer.setLength(0);
    String blocks=" "+String.valueOf(blocksize);
    updateBuffer.append(" [12]Size ").append(blocks.subSequence(blocks.length()-3,blocks.length()));
    String lods=" 0000"+String.valueOf((int)lodstep);
    updateBuffer.append(" [34]Step ").append(lods.subSequence(lods.length()-4,lods.length()));
    if(usecolormap) {
        updateBuffer.append(" [5]Colr");
    } else {
        updateBuffer.append(" [5]    ");
    }
    if(useblendmap) {
        updateBuffer.append("Detl");
    } else {
        updateBuffer.append("    ");
    }
    updateBuffer.append(" [6]Norm");
    if(usenormap) {
        updateBuffer.append("Map ");
    } else {
        updateBuffer.append("Vert");
    }
    if(usestrips) {
        updateBuffer.append(" [7]Strp");
    } else {
        updateBuffer.append(" [7]List");
    }
    if(usecompiledmorphdata) {
        updateBuffer.append(" [8]ClAr");
    } else {
        updateBuffer.append(" [8]AtAr");
    }
    /*
    if(alphaqueue) {
        updateBuffer.append(" [9]QTran");
    } else {
        updateBuffer.append(" [9]QSkip");
    }
     */
    //updateBuffer.append(" [F]Fltr ").append((String.valueOf(filter)+"0000").subSequence(0,4));
    
      if(daynight || !started) {
          started=true;
          time+=tpf;
          if(time>cycle) {
              time-=cycle;
          }
          Vector3f dld=dl.getDirection();
          dld.x=FastMath.sin(2f*FastMath.PI*((time+5f)/cycle));//2*(time/cycle);
          dld.z=FastMath.cos(2f*FastMath.PI*((time+5f)/cycle));//1-2*(time/cycle);
          dld.y=FastMath.sin(2f*FastMath.PI*((time+5f)/cycle));
          dl.setDirection(dld);
          lightState.apply();
      }
    String tm="000"+String.valueOf((int)(time*100f));
    updateBuffer.append(" [0]Tme ").append(tm.subSequence(tm.length()-4,tm.length()));
    lodt.print(updateBuffer);
    camNode.updateWorldData(tpf);
    pg.updateLod(camNode.getCamera());
  }
  
}
