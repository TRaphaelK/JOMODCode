/*
 * GameObjectStore.java
 *
 * Created on 2006. február 19., 11:18
 *
 * Stores in a map the gameobjects, creates/deletes/
 * passes messages to objects
 */

package demoviewer.map;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.FogState;
import com.jme.system.DisplaySystem;
import com.jme.util.LoggingSystem;
import demoviewer.Config;
import demoviewer.resource.ResourceManager;
import demoviewer.terrain.SectoredTerrain2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vear
 */
public class MapHeader {
    
    private Logger _log = LoggingSystem.getLogger();
        
    MapHeaderMessage header;
    // the terrain
    private SectoredTerrain2 t;
    // the gameobjects by their ssn id
    HashMap<Integer,GameObject> items;
            
    // the root node
    Node rootNode;
    // quadnode for static objects
    QuadNode staticNode;
    // quadnode for dynamic (moving) objects
    QuadNode dynNode;
    
    // the status of mapplay
    // 0 - not loaded
    // 1-loading
    // 2-starting
    // 3-playing
    // 4-ending
    
    int status=0;
    
    // is this instance directly connecting to the game
    // server backend
    private boolean server;
    // referenc point as read from memory as opposed to as read from NPJ
    private boolean dumpedRefpointMode=true;
    
    // the connected clients
    HashMap<Integer, GameClient> clients;
    // the counter for client ID's
    int clientID=0;
    
    // the max teams in the map (0-civil, 1-jo, 2-rebell)
    private int maxTeams=3;
    
    // the controller clients for teams
    GameClient[] controller;
    
    // the local client
    private GameClient client;
    
    public MapHeader(Node rootNode) {
        this.rootNode=rootNode;
        clients=new HashMap<Integer, GameClient>();
        controller=new GameClient[maxTeams];
    }
    
    public void addMapHeaderMessage(MapHeaderMessage msg) {
        // TODO if there was a previous map loaded, clear it
        clearMap();
        header=msg;
        // create the map
        createMap();
    }
    
    public void addMapItemMessage(MapItemMessage msg) {
        boolean addmodelbottom=false;
        if(msg.getPosition()!=null) {
            // switch x and z
            float x=msg.getPosition().x;
            msg.getPosition().x=msg.getPosition().z;
            msg.getPosition().z=x;
            msg.setPosition(msg.getPosition().multLocal(Config.modelTransScale));
            // fix theta rotation
            //msg.getRotationAngles()[1]+=FastMath.HALF_PI;
            // if its terraign aligned, get the relative height
            if(!msg.isAbsoluteHeight()) {
                // reference point is the bottom of the object, add this to the
                // height
                addmodelbottom=true;
                // and convert to absolute height
                msg.getPosition().y+=t.getHeightFromWorld(msg.getPosition());
                msg.setAbsoluteHeight(true);
            }
        }
        GameObject go=items.get(new Integer(msg.getID()));
        // check if this object already exists
        if(go==null) {
            // create the object
            go=new GameObject(msg, this);
            // create the model for it too
            ResourceManager mgr=ResourceManager.getInstance();
            mgr.getObjectInstance(msg.getID(),msg.getType_id(), go);
            go.setCullMode(Spatial.CULL_DYNAMIC);
            go.setLocalScale(Config.modelScale);
            if(go.hasModels()) {
                // TODO based on object type, attach to staticNode or dynNode
                staticNode.attachChild(go);
                go.updateRenderState();
            }
            // add the gameobject to the map
            items.put(new Integer(msg.getID()), go);
            /*
            ShaderedCompositeMesh s=go.getModel();
            if(s!=null) {
                //s.setLocalTranslation(s.getReferencePoint().mult(modelScale).mult(-1));
                s.setLocalTranslation(s.getLocalTranslation().set(0f,s.getReferencePoint().y*modelScale.y,0f));
            }
             */
        } else {
            // send the message to the object
            go.addState(msg);
        }
        //if(addmodelbottom) {
            // add model offset to the model
            //  get the model from the gameobject
        msg.getPosition().subtractLocal(go.getReferencePoint().mult(Config.modelScale));
        
        /*
            ShaderedCompositeMesh s=go.getModel();
            if(s!=null) {
                //s.setLocalTranslation(s.getReferencePoint().mult(modelScale).mult(-1));
                msg.getPosition().subtractLocal(s.getGroundPoint().mult(Config.modelScale));
            }
         */
        //}
    }

    private void createMap() {
        status=1;
        // set properties on resourcemanager
        ResourceManager rm=ResourceManager.getInstance();
        
        staticNode=new QuadNode("Statics");
        rootNode.attachChild(staticNode);
        staticNode.updateRenderState();
        staticNode.setEnabled(true);
        dynNode=new QuadNode("Dynamics");
        rootNode.attachChild(dynNode);
        dynNode.updateRenderState();
        
        // create the terrain
          try {
            // create fog
            if(header.getFogType()!=-1) {
                FogState fs = DisplaySystem.getDisplaySystem().getRenderer().createFogState();
                fs.setDensity(header.getFogIntensity());
                fs.setEnabled(true);
                fs.setColor(header.getFogColor());//ColorRGBA.gray);//
                fs.setStart(header.getFogStartDistance()*Config.mainScale);
                fs.setEnd(header.getFogEndDistance()*Config.mainScale);
                fs.setDensityFunction(FogState.DF_LINEAR);
                fs.setApplyFunction(FogState.AF_PER_VERTEX);
                rootNode.setRenderState(fs);
                
                // set camera far plane to double fog distance
                SmartCamera sc=(SmartCamera) rootNode.getChild("Camera Node");
                Camera c=sc.getCamera();
                c.setFrustumFar(header.getFogEndDistance()*2f*Config.mainScale);
            }
            
            t=new SectoredTerrain2(header.getTerrainfile());
            // TODO: no terrain
            rootNode.attachChild(t);
            t.setBoundRefNode(staticNode);
            t.buildTerrain(null);
            
          } catch(Exception e) {
              _log.log(Level.SEVERE,"Cannot build terrain",e);
              status=0;
          }
        if(items==null) items=new HashMap<Integer,GameObject>();
    }

    private void clearMap() {
        status=4;
        if(t!=null) {
            rootNode.detachChild(t);
            t=null;
        }
        rootNode.detachChild(staticNode);
        rootNode.detachChild(dynNode);
        // do a gc to free up resources
        System.gc();
    }

    public SectoredTerrain2 getTerrain() {
        return t;
    }
    
    public void addClientMessage(ClientMessage msg) {
        msg.handleMessage(this);
    }

    public boolean isServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public GameClient getLocalClient() {
        return client;
    }

    public void setLocalClient(GameClient client) {
        this.client = client;
    }
    
    public GameClient createClient() {
        GameClient gc=new GameClient(++clientID, maxTeams);
        clients.put(gc.getId(), gc);
        return gc;
    }
    
    public GameClient removeClient(int id) {
        GameClient gc=clients.get(id);
        if(gc!=null) {
            // removel client
            clients.remove(id);
            // removel local login
            if(client==gc) {
                client=null;
            }
            // remove controller
            for(int i=0;i<controller.length;i++)
                if(controller[i]==gc)
                    controller[i]=null;
        }
        return gc;
    }
    
    public void setController(int team, GameClient client) {
        controller[team]=client;
    }
    
    public GameClient getController(int team) {
        return controller[team];
    }

    public int getMaxTeams() {
        return maxTeams;
    }

    public boolean isDumpedRefpointMode() {
        return dumpedRefpointMode;
    }

    public void setDumpedRefpointMode(boolean dumpedRefpointMode) {
        this.dumpedRefpointMode = dumpedRefpointMode;
    }
}
