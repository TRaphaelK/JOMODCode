/*
 * TextureLoader.java
 *
 * Created on 2006. május 5., 15:01
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package threaded;

import com.jme.image.Texture;
import com.jme.renderer.lwjgl.LWJGLRenderer;
import com.jme.system.DisplaySystem;
import com.jme.system.JmeException;
import com.jme.system.lwjgl.LWJGLDisplaySystem;
import com.jme.util.LoggingSystem;
import com.jme.util.TextureKey;
import com.jme.util.TextureManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

/**
 *
 * @author vear
 */
public class TextureLoader implements Runnable {
    
    static Vector loadQueue=new Vector();
    static ConcurrentHashMap finishedQueue=new ConcurrentHashMap();
    
    static boolean runing=false;
    static boolean working=false;
    
    private Pbuffer headlessDisplay;
    
    /** Creates a new instance of TextureLoader */
    public TextureLoader() {
        PixelFormat format = new PixelFormat( 32, 0, 0, 0, 1 );
        try {
            headlessDisplay = new Pbuffer( 160, 100, format, null, null );
        } catch (LWJGLException ex) {
            LoggingSystem.getLogger().log(Level.WARNING,
                    "Could not create OpenGL context", ex);
            throw new JmeException("Could not create new OpenGL context");
        }
    }

    public void run() {
        runing=true;
        try {
            headlessDisplay.makeCurrent();
            
            // init a new GL context with this object for this thread
            GLContext.useContext(headlessDisplay.getContext());
        } catch(Exception e) {
            LoggingSystem.getLogger().log(Level.WARNING,
                    "Could not attach OpenGL context", e);
            runing=false;
            throw new JmeException("Could attach OpenGL context");
        }
        LoggingSystem.getLogger().log(Level.INFO,
                    "Texture loader starting");
        // create a new renderer
        //renderer = new LWJGLRenderer(0,0);
        
        while(runing) {
            while(!loadQueue.isEmpty()) {
                TextureKey tKey=(TextureKey) loadQueue.remove(0);
                LoggingSystem.getLogger().log(Level.INFO, "Background loading texture ");
                Texture t=TextureManager.loadTexture(tKey);
                if(t!=null) {
                    synchronized(finishedQueue) {
                        finishedQueue.put(tKey,t);
                    }
                    LoggingSystem.getLogger().log(Level.INFO, "Background loaded texture ");
                }
                Thread.yield();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                
            }
        }
        runing=false;
        LoggingSystem.getLogger().log(Level.INFO, "Textureloader finished ");
    }
    
    public static boolean isRuning() {
        return runing;
    }
    
    public static void finish() {
        runing=false;
    }
    
    public static boolean isWorking() {
        return working;
    }
    
    public static TextureKey loadTexture(URL file, int minFilter,
                            int magFilter, int imageType, float anisoLevel, boolean flipped) {
        
        if (null == file) {
            System.err.println("Could not load image...  URL was null.");
            return null;
        }
        
        String fileName = file.getFile();
        if (fileName == null)
            return null;
        
        TextureKey tkey = new TextureKey(file, minFilter, magFilter,
                anisoLevel, flipped, imageType);
    
        // add it to the job queue
        loadQueue.add(tkey);
        
        return tkey;
    }
    
    public static Texture getTexture(TextureKey tKey) {
        if(!runing) {
            loadQueue.remove(tKey);
            return TextureManager.loadTexture(tKey);
        }
        if(finishedQueue.containsKey(tKey)) {
            Texture tx;
            synchronized(finishedQueue) {
                tx=(Texture) finishedQueue.remove(tKey);
            }
            return tx;
        }
        return null;
    }
}
