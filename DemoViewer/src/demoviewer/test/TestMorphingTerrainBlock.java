
package demoviewer.test;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.CameraNode;
import com.jme.scene.Spatial;
import com.jme.scene.Text;
import com.jme.scene.state.CullState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.LoggingSystem;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainBlock;
import com.jmex.terrain.TerrainPage;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.RawHeightMap;
import demoviewer.DemoBaseGame;
import demoviewer.movemodels.RtsInputHandler;
import demoviewer.terrain.MorphingTerrainManager;
import demoviewer.terrain.MorphingTerrainBlock;
import demoviewer.terrain.MorphingTerrainShaderCreator;
import demoviewer.terrain.Terrain;
import demoviewer.terrain.TerrainUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestMorphingTerrainBlock extends DemoBaseGame {

    private Logger _log = LoggingSystem.getLogger();
  private CameraNode camNode;
private TerrainPage page;

  /**
   * Entry point for the test,
   *
   * @param args
   */
  public static void main(String[] args) {
    TestMorphingTerrainBlock app = new TestMorphingTerrainBlock();
    if(args!=null && args.length>0) {
        for(int i=0;i<args.length;i++)
            /*if(args[i].toLowerCase().equals("raw")) {
                app.terrain=1;
            } else*/ if(args[i].toLowerCase().equals("nomorph")) {
                app.domorph=false;
            } else if(args[i].toLowerCase().equals("attrarray")) {
                app.useattribarray=true;
            }
    }
    app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
    app.start();
  }

  public boolean useattribarray=false;
  public boolean domorph=true;
  // the current choosen block type (0-4)
  int setterraintype=1;
  int currentterraintype=-1;
  // block with tristrip
  MorphingTerrainBlock tbs;
  // block with trilist
  //MorphingTerrainBlock tbl;
  // classic TerrainBlock
  TerrainBlock teb;
  // classical terrainblock with clod
  TerrainBlock tebc;
  
  int lighting=0; // 0-nofrag, 1-flat, 2-lightmap, 3-vtx, 4-normal3, 5-normal2
  
  boolean colortex=false;

  int[] heightMap=null;
  Vector3f terrainScale = new Vector3f(5,1,5);;
  MorphingTerrainManager mgr;
  
  private void createTerrainBlock() {
      // check for GLSL support
      
      if(currentterraintype!=setterraintype) {
          // detach previous block
          switch(currentterraintype) {
              case 0: if(tbs!=null) rootNode.detachChild(tbs); 
                        break;
              case 1: if(tbs!=null) rootNode.detachChild(tbs); 
                        break;
              case 2: if(teb!=null) rootNode.detachChild(teb); 
                        break;
              case 3: if(tebc!=null) rootNode.detachChild(tebc);
                        break;
          }
          if(setterraintype==3 && tebc==null) setterraintype=0;
          if(setterraintype==0) setterraintype=1;
      }
    
      if(mgr==null) mgr=MorphingTerrainManager.getInstance();
      
      mgr.setDynamicShaderSwitch(true);
      mgr.setUseCompiledMorphData(true);
      mgr.setSortVertices(true);
      mgr.setUseMultipleVertexBuffers(true);
      MorphingTerrainShaderCreator sc=mgr.getShaderCreator();
      sc.setUsePredefinedShaders(false);
      sc.setLightingMode(lighting);
      sc.setBlendNormals(false);
      if(lighting==4 || lighting==5) {
        sc.setUseNormalMap(1);
      } else
        sc.setUseNormalMap(-1);
      if((setterraintype==0 || setterraintype==1) && domorph)
          sc.setUseMorphing(true);
      else
          sc.setUseMorphing(false);
      sc.setUseColormap(0);
      mgr.createShaders();
      
      if(heightMap==null) {
        RawHeightMap hm = new RawHeightMap(new MidPointHeightMap(256, 1.9f).getHeightMap());
        heightMap = TerrainUtils.subSize(hm.getHeightMap(),129);
      }
      // update textures
      createTexture(heightMap, terrainScale);
      
    if(tbs==null) {
        tbs = new MorphingTerrainBlock("MorphingTerrainBlock", 129, terrainScale,
       heightMap,
       new Vector3f(0, 0, 0), 129, new Vector2f(0,0), 0, Terrain.BLOCK|Terrain.MORPH, mgr);
        tbs.setModelBound(new BoundingBox());
        tbs.updateModelBound();
        tbs.setLocalTranslation(new Vector3f(0,0,0));
        tbs.setRenderState(m);
        tbs.setRenderState(cs);
        tbs.setDefaultColor(ColorRGBA.green);
        tbs.lockTransforms();
        tbs.lockTransforms();
    }
    if(teb==null) {
        teb = new TerrainBlock("TerrainBlock", 129, terrainScale, heightMap, new Vector3f(0, 0, 0), false);
        teb.setModelBound(new BoundingBox());
        teb.updateModelBound();
        teb.setLocalTranslation(new Vector3f(0,0,0));
        teb.setRenderState(cs);
        teb.setDefaultColor(ColorRGBA.green);
        teb.setRenderState(m);
        teb.lockTransforms();
        teb.lockTransforms();
    }
/*
    if(tebc==null) {
        tebc = new TerrainBlock("TerrainBlock", 129, terrainScale, heightMap, new Vector3f(0, 0, 0), true);
        tebc.setModelBound(new BoundingBox());
        tebc.updateModelBound();
        tebc.setLocalTranslation(new Vector3f(0,0,0));
        tebc.setRenderState(cs);
        tebc.setDefaultColor(ColorRGBA.green);
        tebc.setRenderState(m);
    }
 */
    if(currentterraintype!=setterraintype) {
          // attach new one
          currentterraintype=setterraintype;
          
          switch(currentterraintype) {
              case 0: rootNode.attachChild(tbs); break;
              case 1: rootNode.attachChild(tbs); break;
              case 2: rootNode.attachChild(teb); break;
              case 3: {
                  if(tebc!=null)
                    rootNode.attachChild(tebc); break;
              }
          }
    }
      /*
      if(currentterraintype==0) {
          mgr.setUseStrips(true);
      } else {
          mgr.setUseStrips(false);
      }
       */
      

      // update shader
      switch(currentterraintype) {
          case 0: {
              //GLSLShaderObjectsState so=MorphingTerrainManager.getInstance().getShader(1);
              //tbs.setRenderState(so);
              tbs.setRenderState(ts[this.lighting]);
              /*
              if(usenormap) 
              else tbs.setRenderState(tsc);
               */
              //createMorphShader(tbs);
              tbs.updateRenderState();
          } break;
          case 1: {
              //GLSLShaderObjectsState so=MorphingTerrainManager.getInstance().getShader(1);
              //tbs.setRenderState(so);
              tbs.setRenderState(ts[this.lighting]);
              /*
              if(usenormap) 
              else tbs.setRenderState(tsc);
               */
              //createMorphShader(tbl);
              tbs.updateRenderState();
          } break;              
          case 2: {
              GLSLShaderObjectsState so=MorphingTerrainManager.getInstance().getShader(1);
              teb.setRenderState(so);
              teb.setRenderState(ts[this.lighting]);
              /*
              if(usenormap) 
              else teb.setRenderState(tsc);
               */
              teb.updateRenderState();
          }
          case 3: {
              if(tebc!=null) {
                  GLSLShaderObjectsState so=MorphingTerrainManager.getInstance().getShader(1);
                  tebc.setRenderState(so);
                  tebc.updateRenderState();
                  tebc.setRenderState(ts[this.lighting]);
                  /*if(usenormap) 
                  else tebc.setRenderState(tsc);
                   */
                  tebc.updateRenderState();
              }
          }
      }
    rootNode.updateGeometricState(0,true);
    rootNode.updateRenderState();
  }
  
  
  private void reCreate() {
        createTerrainBlock();
  }

  Texture t1;
  Texture tg;
  Texture tw;
  Texture t2;
  Texture t3;
  GLSLShaderObjectsState son;
  GLSLShaderObjectsState som;
  TextureState [] ts=new TextureState[6];
  int texunits=-1;
  
  protected void createTexture(int[] heightMap, Vector3f terrainScale) {
    
    for(int i=0;i<ts.length;i++) {
        if(ts[i]==null) {
            ts[i] = display.getRenderer().createTextureState();
            ts[i].setEnabled(true);
        }
    }
    if(texunits==-1) {
       texunits=ts[0].getNumberOfUnits();
       LoggingSystem.getLogger().log(Level.INFO, "Supported texture units "+texunits);
    }
    if(texunits<2) {
        lighting=0;
    }
    if(tg==null) {
        tg = TextureManager.loadTexture(
            "media/grassb.png",
            Texture.MM_LINEAR_LINEAR,
            Texture.FM_LINEAR,
            1.0f,//tsnm.getMaxAnisotropic(),
            false);
    } 
    if(tw==null) {
        tw = TextureManager.loadTexture(
            "media/white.png",
            Texture.MM_LINEAR_LINEAR,
            Texture.FM_LINEAR);
    }
    if(colortex) {
        t1=tg;
    } else {
        t1=tw;
    }
    t1.setApply(Texture.AM_REPLACE);
    t1.setCombineFuncRGB(Texture.ACF_MODULATE);
    t1.setCombineSrc0RGB(Texture.ACS_TEXTURE);
    t1.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
    t1.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
    t1.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
    t1.setCombineScaleRGB(1.0f);
    t1.setWrap(Texture.WM_WRAP_S_WRAP_T);
    //t1.setAnisoLevel(tsc.getMaxAnisotropic());
    //tsc.setTexture(t1, 0);
    for(int i=0;i<ts.length;i++)
        ts[i].setTexture(t1, 0);
    
    if(t2==null) {
        t2=new Texture();
        t2.setImage(TerrainUtils.getUpRightLeftDownAlteratingNormalMap(heightMap, terrainScale, 128, 3));
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
        //t2.setAnisoLevel(tsnm.getMaxAnisotropic());
    }
    ts[4].setTexture(t2, 1);
    if(t3==null) {
        t3=new Texture();
        t3.setImage(TerrainUtils.getUpRightLeftDownAlteratingNormalMap(heightMap, terrainScale, 128, 2));
        t3.setFilter(Texture.FM_LINEAR);
        t3.setMipmapState(Texture.MM_NEAREST_NEAREST);
        t3.setWrap(Texture.WM_WRAP_S_WRAP_T);
        t3.setApply(Texture.AM_REPLACE);
        t3.setCombineFuncRGB(Texture.ACF_ADD_SIGNED);
        t3.setCombineSrc0RGB(Texture.ACS_TEXTURE);
        t3.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
        t3.setCombineSrc1RGB(Texture.ACS_PREVIOUS);
        t3.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
        t3.setCombineScaleRGB(1.0f);
        //t2.setAnisoLevel(tsnm.getMaxAnisotropic());
    }
    ts[5].setTexture(t3, 1);
    if(m==null) {
        m = display.getRenderer().createMaterialState();
        m.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        m.setDiffuse(new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f));
    }
  }
  
  MaterialState m;
  
  private void createLight() {
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
    lightState.setEnabled(true);
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
    camNode.setLocalTranslation(new Vector3f(0f, 500f, 0f));
    
    //
    camNode.updateWorldData(0);
    
    rootNode.attachChild(camNode);
    
    display.setTitle("Morphing Terrain Test");
    display.getRenderer().setBackgroundColor(new ColorRGBA(0.5f,0.5f,0.5f,1));
    //camNode.lookAt(new Vector3f(0f,500f,0f), new Vector3f(0,0,1));


    cs = display.getRenderer().createCullState();
    cs.setCullMode(CullState.CS_BACK);
    cs.setEnabled(true);
    
    rootNode.setRenderState(cs);
    
    //camNode.lookAt(tb.getWorldTranslation(), new Vector3f(0,0,1));
    
    createLight();
    
    //input = new FpsNodeHandler(camNode, 300, 1, m);
    input = new RtsInputHandler(camNode, rootNode, null, 300, 1, null, 16f);
    
    rootNode.updateRenderState();
    
    lodt = Text.createDefaultTextLabel("FPS label");
    lodt.setCullMode(Spatial.CULL_NEVER);
    lodt.setTextureCombineMode(TextureState.REPLACE);
    lodt.setLocalTranslation(new Vector3f(-10,20,0));
    fpsNode.attachChild(lodt);
    KeyBindingManager.getKeyBindingManager().set("fixmask_1", KeyInput.KEY_1);
    KeyBindingManager.getKeyBindingManager().set("fixmask_2", KeyInput.KEY_2);
    KeyBindingManager.getKeyBindingManager().set("fixmask_4", KeyInput.KEY_3);
    KeyBindingManager.getKeyBindingManager().set("fixmask_8", KeyInput.KEY_4);
    KeyBindingManager.getKeyBindingManager().set("switch_colormap", KeyInput.KEY_5);
    KeyBindingManager.getKeyBindingManager().set("switch_normal", KeyInput.KEY_6);
    KeyBindingManager.getKeyBindingManager().set("switch_strips", KeyInput.KEY_7);
    
    createTerrainBlock();
  }

  float lod=0;
  float updir=0.001f;
  int fixmask=0;
  protected Text lodt;
  
  protected void simpleUpdate()
  {
      // check for fixmask change
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("fixmask_1", false)) {
          // get previous state
          fixmask^=1;
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("fixmask_2", false)) {
          // get previous state
          fixmask^=2;
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("fixmask_4", false)) {
          // get previous state
          fixmask^=4;
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("fixmask_8", false)) {
          // get previous state
          fixmask^=8;
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_colormap", false)) {
          // get previous state
          colortex=!colortex;
          // rebuild
          reCreate();
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_normal", false)) {
          // get previous state
          lighting++;
          if(lighting==6) lighting=0;
          // rebuild
          reCreate();
      }
      if (KeyBindingManager.getKeyBindingManager().isValidCommand("switch_strips", false)) {
          // get previous state
          setterraintype++;
          if(setterraintype==4) setterraintype=0;
          lod=0;
          // rebuild
          reCreate();
      }
      if(currentterraintype==0 || currentterraintype==1) {
          if(tbs!=null) {
              int ml=tbs.getMaxLod();
              lod+=updir;
              if(lod>=ml+1) {
                  updir=-0.001f;
                  lod=ml+1+updir;
              } else if(lod<0) {
                  updir=0.001f;
                  lod=0f;
              }
              float uplod=lod;
              float rightlod=lod;
              float downlod=lod;
              float leftlod=lod;
              if((fixmask&1)!=0) {
                  uplod+=1f;
              }
              if((fixmask&2)!=0) {
                  rightlod+=1f;
              }
              if((fixmask&4)!=0) {
                  downlod+=1f;
              }
              if((fixmask&8)!=0) {
                  leftlod+=1f;
              }
              tbs.setLodLevel(lod, uplod, rightlod, downlod, leftlod);
              
              //tbs.lockMeshes(DisplaySystem.getDisplaySystem().getRenderer());
              //tbs.generateDisplayList(DisplaySystem.getDisplaySystem().getRenderer());
              //tbs.setFixMask(fixmask);
          }
      } else {
          lod=0;
      }
          
        updateBuffer.setLength(0);
        updateBuffer.append(" LOD: ").append((String.valueOf(lod)+"0000").subSequence(0,4));
        updateBuffer.append(" [1234]FixMask ").append(fixmask);
        if(colortex) {
            updateBuffer.append(" [5]Clr");
        } else {
            updateBuffer.append(" [5]   ");
        }
        updateBuffer.append(" [6]");
        switch(lighting) {
            case 0: updateBuffer.append("NoFragSh"); break;
            case 1: updateBuffer.append("Flat    "); break;
            case 2: updateBuffer.append("LightMap"); break;
            case 3: updateBuffer.append("VertNorm"); break;
            case 4: updateBuffer.append("NormMap3"); break;
            case 5: updateBuffer.append("NormMap2"); break;
        }
        updateBuffer.append(" [7]");
        switch(currentterraintype) {
            case 0: updateBuffer.append("MorphStrip"); break;
            case 1: updateBuffer.append("MorphList"); break;
            case 2: updateBuffer.append("Block"); break;
            case 3: updateBuffer.append("BlockClod"); break;
        }
        /** Send the fps to our fps bar at the bottom. */
        lodt.print(updateBuffer);
      //MorphingTerrainBlock.FIX_UP | MorphingTerrainBlock.FIX_LEFT | MorphingTerrainBlock.FIX_RIGHT      
  }
  
}
