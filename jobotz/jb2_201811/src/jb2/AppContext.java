/*
 * 
 * Vear 2017  * 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package jb2;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import jb2.ent.StateFile;
import jb2.gai.GaiOptimizer;
import jb2.jo.GameObjectFactory;
import jb2.jo.ServerInfo;
import jb2.map.HeightMap;
import jb2.map.MapRenderer;
import jb2.map.NavGrid;
import jb2.map.RouteFinder;
import jb2.util.OctreeEntity;

/**
 * Contains references to singleton instances
 * @author vear
 */
public class AppContext {
    
    static {
        
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        //System.setProperty("java.util.logging.config.file","logging.properties");
    }
    
    //public static NavMap navMap;
    public static NavGrid navGrid;
    public static GameObjectFactory gameObjectFactory;
    public static GameMatch gameMatch;
    public static StateFile stateFile;
    public static ServerInfo serverInfo;
    public static OctreeEntity mapOctreeEntities;
    //public static OctreeNavNode mapOctreeNavNodes;
    // threaded tasks
    public static RouteFinder routeFinder;
    //public static NavMapCalculator navMapCalculator;
    public static HeightMap heightMap;
    public static MapRenderer renderer;
    
    // logfile
    public static FileHandler fileHandler;
    
    // parameter optimizer
    public static GaiOptimizer optimizer;
        
    public static void clear() {
        //navMap = null;
        navGrid = null;
        gameObjectFactory = null;
        gameMatch=null;
        stateFile=null;
        mapOctreeEntities=null;
        //mapOctreeNavNodes=null;
        if(routeFinder!=null)
            routeFinder.stopThread();
        routeFinder = null;
        heightMap = null;
        renderer = null;
        optimizer=null;
        if(fileHandler!=null) {
            fileHandler.close();
            Logger.getLogger("").removeHandler(fileHandler);
            fileHandler= null;
        }
    }
    
    public static void init() {
        gameObjectFactory = new GameObjectFactory();
        //navMap = new NavMap();
        navGrid = new NavGrid();
        if(serverInfo==null)
            serverInfo=new ServerInfo();
        stateFile = new StateFile();
        mapOctreeEntities = new OctreeEntity();
        routeFinder = new RouteFinder();
        //System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        setLogLevel(Config.logLevel);
        heightMap = new HeightMap();
        renderer= new MapRenderer();
        optimizer = new GaiOptimizer();
        
    }
    
    public static void setLogLevel(Level level) {
        if(fileHandler==null) {
            try {
                // init logfile
                fileHandler = new FileHandler(Config.basedir + File.separator + "jobotz_%g.log", 1000*1000, 10);
                fileHandler.setFormatter(new SimpleFormatter());

            } catch (IOException ex) {
                Logger.getLogger(AppContext.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(AppContext.class.getName()).log(Level.SEVERE, null, ex);
            }
            Logger.getLogger("").addHandler(fileHandler);
        }

        final Handler[] handlers = Logger.getLogger("").getHandlers();
        for (final Handler handler : handlers) {
           handler.setLevel(level);
        }
        Logger.getLogger("").setLevel(level);
    }
    
    public static void flushLogFile() {
        if(fileHandler==null)
            return;
        fileHandler.flush();
    }
}
