/*
 * TestAsyncTexLoading.java
 *
 * Created on 2006. május 5., 16:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.test;

import com.jme.app.SimpleGame;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Box;
import com.jme.scene.state.CullState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureKey;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import threaded.TextureLoader;

/**
 *
 * @author vear
 */
public class TestAsyncTexLoading  extends SimpleGame {
    
    HashMap<TextureKey,TextureState> neededStates=new HashMap<TextureKey,TextureState>();
    
  int loadmode=2;       // 0-load all at start, 1-load one texture every update cycle, 2-load in another thread
  
  public static void main(String[] args) {
    TestAsyncTexLoading app = new TestAsyncTexLoading();
    app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
    app.start();
  }
  
    /** Creates a new instance of TestAsyncTexLoading */
    public TestAsyncTexLoading() {
    }
  
    protected void simpleInitGame() {
        // start the texture loader
        if(loadmode==2)
            new Thread(new TextureLoader()).start();
        
        display.setTitle("Async Texture loading");
        cam.setLocation(new Vector3f(0, 0, 60));
        cam.update();

        Vector3f max = new Vector3f(2, 2, 2);
        Vector3f min = new Vector3f( -2, -2, -2);

        for(int i=0;i<10;i++) {
            TriMesh t = new Box("Box", min, max);
            CullState cs=display.getRenderer().createCullState();
            cs.setCullMode(CullState.CS_BACK);
            t.setRenderState(cs);

            // call init once
            TextureState ts=display.getRenderer().createTextureState();
            ts.setEnabled(false);
            t.setRenderState(ts);
            t.setLocalTranslation(new Vector3f(i*4,0,0));
            rootNode.attachChild(t);
            TextureKey tk=null;
            try {
                tk = TextureLoader.loadTexture(new URL("file:c:/temp/8/tex" + i + ".tga"), Texture.MM_LINEAR, Texture.FM_LINEAR, Image.GUESS_FORMAT, 1.0f, false);
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
                
                    neededStates.put(tk, ts);
            }
        rootNode.updateRenderState();

    }
    public void simpleUpdate() {
        if(!neededStates.isEmpty()) {
            boolean loadedone=false;
            
            // there is to be loaded
            HashSet removable=new HashSet();
            Iterator<TextureKey> tki=neededStates.keySet().iterator();
            while(tki.hasNext() && !(loadmode==1 && loadedone)) {
                TextureKey tKey=tki.next();
                Texture t=TextureLoader.getTexture(tKey);
                if(t!=null) {
                    t.setWrap(Texture.WM_CLAMP_S_CLAMP_T);
                    TextureState ts=neededStates.get(tKey);
                    ts.setTexture(t,ts.getNumberOfSetTextures());
                    removable.add(tKey);
                    loadedone=true;
                }
            }
            tki=removable.iterator();
            while(tki.hasNext()) {
                TextureKey tKey=tki.next();
                TextureState ts=neededStates.remove(tKey);
                if(!neededStates.containsValue(ts)) {
                    // finished with this TextureState
                    ts.setEnabled(true);
                }
            }
            if(neededStates.isEmpty()) {
                // finished loading textures, release the texture loader
                TextureLoader.finish();
            }
        }
    }    
}
