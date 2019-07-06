/*
 * ApacheTest.java
 *
 * Created on 2006. május 9., 19:41
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.test;

import com.jme.app.SimpleGame;
import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.input.KeyInput;
import com.jme.input.action.KeyScreenShotAction;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jmex.terrain.TerrainBlock;
import com.jmex.terrain.util.MidPointHeightMap;
import com.jmex.terrain.util.ProceduralTextureGenerator;

/**
 *
 * @author vear
 */
public class ApacheTest extends SimpleGame  {

	public static void main(String[] args) {
		ApacheTest test = new ApacheTest();
		test.setDialogBehaviour(SimpleGame.ALWAYS_SHOW_PROPS_DIALOG);
		test.start();
	}
	
	private Vector3f camLocation = new Vector3f();
	private Quad chopperBody;
	
	protected void simpleInitGame() {
		input.addAction(new KeyScreenShotAction(), "screenshot", KeyInput.KEY_F12, false);
                //input.addAction(new KeyScreenShotAction(), "step", KeyInput.KEY_F11, false);
		createTerrain();
                /*
                for(int blend=0;blend<1;blend++)//2
                    for(int sf=0;sf<1;sf++)//7
                        for(int df=0;df<1;df++)//7
                            for(int tst=0;tst<8;tst++)//7
                                for(float val=0;val<=1.0;val+=0.1)
                                createApache(blend, sf, df, tst, val);
                 */
                createApache();
	}
	
	private void createTerrain() {
		lightState.setTwoSidedLighting(true);
	    ((PointLight)lightState.get(0)).setLocation(new Vector3f(100.0f, 500.0f, 50.0f));
	    MidPointHeightMap heightMap = new MidPointHeightMap(128, 1.9f);
	    Vector3f terrainScale = new Vector3f(5.0f, 0.1f, 5.0f);
	    TerrainBlock tb = new TerrainBlock("Terrain", heightMap.getSize(), terrainScale, heightMap.getHeightMap(), new Vector3f(0.0f, 0.0f, 0.0f), false);
	    tb.setDistanceTolerance(1.0f);
	    tb.setDetailTexture(1, 16);
	    tb.setModelBound(new BoundingBox());
	    tb.updateModelBound();
	    tb.setLocalTranslation(new Vector3f(-150.0f, -50.0f, -150.0f));
	    rootNode.attachChild(tb);
		
		ProceduralTextureGenerator pt = new ProceduralTextureGenerator(heightMap);
		pt.addTexture(new javax.swing.ImageIcon(ApacheTest.class.getResource("/jmetest/data/images/grassb.png")), -128, 0, 128);
		pt.addTexture(new javax.swing.ImageIcon(ApacheTest.class.getResource("/jmetest/data/images/dirt.jpg")), 0, 128, 255);
		pt.addTexture(new javax.swing.ImageIcon(ApacheTest.class.getResource("/jmetest/data/images/highest.jpg")), 128, 255, 384);
		pt.createTexture(64);
		
		TextureState ts = display.getRenderer().createTextureState();
		ts.setEnabled(true);
		Texture t1 = TextureManager.loadTexture(pt.getImageIcon().getImage(), Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
		ts.setTexture(t1, 0);
		
		Texture t2 = TextureManager.loadTexture(ApacheTest.class.getClassLoader().getResource("jmetest/data/images/Detail.jpg"), Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR);
		ts.setTexture(t2, 1);
		t2.setWrap(Texture.WM_WRAP_S_WRAP_T);
		t1.setApply(Texture.AM_COMBINE);
		t1.setCombineFuncRGB(Texture.ACF_MODULATE);
		t1.setCombineSrc0RGB(Texture.ACS_TEXTURE);
		t1.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
		t1.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
		t1.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
		t1.setCombineScaleRGB(1.0f);
		
		t2.setApply(Texture.AM_COMBINE);
		t2.setCombineFuncRGB(Texture.ACF_ADD_SIGNED);
		t2.setCombineSrc0RGB(Texture.ACS_TEXTURE);
		t2.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
		t2.setCombineSrc1RGB(Texture.ACS_PREVIOUS);
		t2.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
		t2.setCombineScaleRGB(1.0f);
		rootNode.setRenderState(ts);
	}
	
	private void createApache(int blend, int sf, int df, int tst, float val) {
		chopperBody = new Quad(blend+","+sf+","+df+","+tst+","+val,5.0f,5.0f);//new Box("Box", new Vector3f(), 5.0f, 5.0f, 0.1f);
		chopperBody.getLocalRotation().fromAngleAxis(FastMath.PI * 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
		rootNode.attachChild(chopperBody);
		
		TextureState ts1 = display.getRenderer().createTextureState();
		Texture t = TextureManager.loadTexture(ApacheTest.class.getResource("/jmetest/data/images/apache_body.tga"), 
                        Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, Image.RGBA8888, 1.0f, false);
		t.setWrap(Texture.WM_WRAP_S_WRAP_T);
		t.setTranslation(new Vector3f());
                t.setApply(Texture.AM_REPLACE);
                /*
		t.setCombineFuncRGB(Texture.ACF_REPLACE);
		t.setCombineSrc0RGB(Texture.ACS_TEXTURE);
		t.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
		t.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
		t.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
		t.setCombineScaleRGB(1.0f);
                //t.setCombineFuncAlpha(Texture.ACF_REPLACE);
                 */
		ts1.setTexture(t);
		chopperBody.setRenderState(ts1);
        
                
		AlphaState as = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		as.setBlendEnabled(blend==0?false:true);
                //as.setSrcFunction(sf);
                //as.setDstFunction(df);
                if(tst>-1) {
                    as.setTestEnabled(true);
                    as.setTestFunction(tst);
                    as.setReference(val);
                } else {
                    as.setTestEnabled(false);
                }
		as.setEnabled(true);
		
		chopperBody.setRenderState(as);
                
		chopperBody.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
                float xcord=(blend + sf*2 + df*2*7 + tst*2*7*7 + val*10*2*7*7);
                float ycord=(xcord/60f)*5;
                xcord%=60;xcord*=5;
                chopperBody.setLocalTranslation(new Vector3f(xcord,0f,ycord));
	}
	
	private void createApache() {
		chopperBody = new Quad("apache",5.0f,5.0f);
		chopperBody.getLocalRotation().fromAngleAxis(FastMath.PI * 0.5f, new Vector3f(1.0f, 0.0f, 0.0f));
		rootNode.attachChild(chopperBody);
		
		TextureState ts1 = display.getRenderer().createTextureState();
		Texture t = TextureManager.loadTexture(ApacheTest.class.getResource("/jmetest/data/images/apache_body.png"), 
                        Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, Image.RGBA8888, 1.0f, false);
		t.setWrap(Texture.WM_WRAP_S_WRAP_T);
		t.setTranslation(new Vector3f());
                //t.setApply(Texture.AM_REPLACE);
                chopperBody.setTextureCombineMode(TextureState.REPLACE);
                /*
		t.setCombineFuncRGB(Texture.ACF_REPLACE);
		t.setCombineSrc0RGB(Texture.ACS_TEXTURE);
		t.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
		t.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
		t.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
		t.setCombineScaleRGB(1.0f);
                //t.setCombineFuncAlpha(Texture.ACF_REPLACE);
                 */
		ts1.setTexture(t);
		chopperBody.setRenderState(ts1);
        
                
                AlphaState alpha = display.getRenderer().createAlphaState();
                alpha.setSrcFunction(AlphaState.SB_SRC_ALPHA);
                alpha.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
                alpha.setTestFunction(AlphaState.TF_GREATER);
                alpha.setBlendEnabled(true);
                chopperBody.setRenderState(alpha);
                
		chopperBody.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
                chopperBody.setLocalTranslation(new Vector3f(0,0f,0));
	}
        
	protected void simpleUpdate() {
		/*camLocation.set(chopperBody.getLocalTranslation());
		camLocation.addLocal(0.0f, 50.0f, 0.0f);
		cam.setLocation(camLocation);*/
		//System.out.println(cam.getLocation() + " / " + camLocation);
	}
}
