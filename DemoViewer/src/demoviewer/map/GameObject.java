/*
 * GameObject.java
 *
 * Created on 2006. február 19., 10:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.map;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingVolume;
import com.jme.intersection.PickResults;
import com.jme.math.Quaternion;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState;
import demoviewer.Config;
import demoviewer.gui.SelectFrame;
import demoviewer.gui.SelectionBox;
import demoviewer.n3di.ModelInfo;
import demoviewer.render.ShaderedMesh;
import demoviewer.resource.ModelStore;
import demoviewer.resource.ResourceManager;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

/**
 *
 * @author vear
 */
public class GameObject extends Node {
    
    // renderstates on this gameobject
    public RenderState[] states = new RenderState[RenderState.RS_MAX_STATE];
    
    private static Quaternion tempR1 = new Quaternion();
    private static Quaternion tempR2 = new Quaternion();

    // managing map header we are interacting with
    MapHeader hdr;
    
    // time of last update
    float currenttime;
    // ssn id of the object
    private int id;
    
    // the current state attributes
    MapItemMessage attrib;
    // the next state attributes
    MapItemMessage next;
    // TODO time based store of attributes
    LinkedList<MapItemMessage> nextstates;
    
    // the model(type) information
    private ModelInfo info;

    // reference point
    Vector3f refpoint;
    
    // different state models, with different lods
    // 0 -normal
    // 1 -husk
    // 2 -nothing (respawning)
    SharedMesh [][] models;
    int currentModel;
    int currentLod;
    SharedMesh currentMesh=null;
       
    // need to recalculate which lod model to use
    boolean needsLodRefresh=true;
    // previous camera positions
    Vector3f prevCamPosition=new Vector3f();
    Vector3f worldCenter=new Vector3f();

    // selection box in world coodiantes
    private BoundingBox selBox;
    // is the objects selection box needed (is selectable)
    boolean selectbox=true;
    // the selection frame
    SelectFrame sf;
    // is the object selected by client
    private boolean selected=false;
    
    // color
    ColorRGBA color=ColorRGBA.white;
    // attributes of the object
    int team;
    
    /*
     * Create the object by getting data from
     * a create message
     */
    public GameObject(MapItemMessage attrib, MapHeader hdr) {
        super(String.valueOf(attrib.getID()));
        this.hdr=hdr;
        
        id=attrib.getID();
        setState(attrib);
        // get the moddel info
        ModelStore ms=ResourceManager.getInstance().getModelStore();
        info=ms.getModelInfo(attrib.getType_id());
        nextstates=new LinkedList<MapItemMessage>();
        if(info.getType()==ModelInfo.TYPE_PERSON || info.getType()==ModelInfo.TYPE_VEHICLE) {
            selectbox=true;
        }
        children=new ArrayList();
        //children.add(null);
    }
    
    public void addState(MapItemMessage attrib) {
        nextstates.add(attrib);
    }
    
    public void setState(MapItemMessage attrib) {
        this.attrib=attrib;
        this.next=attrib;
        this.currenttime=attrib.getTimestamp();
    }
    
    public void updateGeometricState(float time, boolean initiator) {
        updateWorldData(time);
        /*
        if ((lockedMode & LOCKED_BOUNDS) == 0) {
            updateWorldBound();
            if (initiator) {
                propagateBoundToRoot();
            }
        }
         */
    }
    
    public void updateWorldData(float time) {
        currenttime+=time;
        if(next.getTimestamp()<=currenttime) {
            // apply statechange type of attributes
            team=next.getTeam();
            //team=team==-1?0:team;
        }
        // if the next states time is less than the current time
        // and we still have some unprocessed messages
        while(next.getTimestamp()<currenttime && !nextstates.isEmpty() ) {
            // TODO do not just replace, but set all attributes separately,
            // based on which data is avalible
            attrib=next;
            next=nextstates.remove();
        }
        if(next.getTimestamp()>=currenttime) {
            // calculate inbetween time 0-1
            float slerp=0;
            if(attrib.getTimestamp()!=next.getTimestamp()) {
                slerp=(currenttime-attrib.getTimestamp())/(next.getTimestamp()-attrib.getTimestamp());
            }
            // transition between states
            this.getLocalTranslation().interpolate(attrib.getPosition(),next.getPosition(),slerp);
            this.setLocalTranslation(this.getLocalTranslation());
            // slerp the rotation
            this.getLocalRotation().slerp(attrib.getRotationQuaternion(tempR1), next.getRotationQuaternion(tempR2), slerp);
            this.setLocalRotation(this.getLocalRotation());
            worldRotation.set(localRotation);
            worldTranslation.set(localTranslation);
            updateWorldBound();

            Spatial prnt=parent;
            while(prnt!=null) {
                if(((Node)prnt).getWorldBound()==null) {
                    prnt.updateWorldBound();
                }
                prnt.getWorldBound().mergeLocal(this.getWorldBound());
                prnt=prnt.getParent();
            }
            
            // mark that we moved, and potentially lod model needs to be updated
            needsLodRefresh=true;
            
        }
        if(this.getLastFrustumIntersection()==Camera.OUTSIDE_FRUSTUM) {
            if(sf!=null && sf.isEnabled()) {
                sf.setEnabled(false);
                if(sf.getParent()!=null)
                    sf.getParent().detachChild(sf);
            }
        }
    }

    public void updateWorldBound() {
        getModel();
        if (currentMesh != null) {
            ShaderedMesh child=(ShaderedMesh) currentMesh.getTarget();
            BoundingVolume vb=child.getWorldBound();
            /*
            if(vb==null) child.updateWorldBound();
            vb=child.getWorldBound();
             */
            if(vb!=null) {
                worldBound = vb.transform(worldRotation,
                                    worldTranslation, worldScale, worldBound);
                if(selectbox) {
                    BoundingBox sb=child.getSelBox();
                    if(sb!=null) {
                        selBox=(BoundingBox) sb.transform(worldRotation, worldTranslation, worldScale, selBox);
                    } else selBox=null;
                }
            }
        }
    }
    
    public int getId() {
        return id;
    }
    
    public SharedMesh getModel() {
        if(models!= null && currentModel < models.length && models[currentModel] != null && currentLod < models[currentModel].length) {
                currentMesh=models[currentModel][currentLod];
        } else
            currentMesh=null;
        return currentMesh;
    }
    
    public SharedMesh getCurrentMesh() {
        return currentMesh;
    }
    
    public Vector3f getReferencePoint() {
        if(refpoint==null) {
            getModel();
            ShaderedMesh model=(ShaderedMesh)((currentMesh!=null)?currentMesh.getTarget():null);
            if(model!=null) {
                if(hdr.isDumpedRefpointMode()) {
                    if( info.getType()!=ModelInfo.TYPE_OBJECT) {
                        refpoint=model.getGroundPoint();
                    } else refpoint=new Vector3f();
                } else {
                    if(info!=null) {
                        if(info.getType()==ModelInfo.TYPE_VEHICLE) {
                            refpoint=model.getBottomPoint();//refpoint=new Vector3f();
                        } else if( info.getType()!=ModelInfo.TYPE_OBJECT) {
                            refpoint=model.getGroundPoint();
                        } else {
                            refpoint=new Vector3f(model.getGroundPoint().x, model.getBottomPoint().y, model.getGroundPoint().z);
                        }
                    } else 
                      refpoint=new Vector3f(model.getGroundPoint().x, model.getBottomPoint().y, model.getGroundPoint().z);
                        //refpoint = new Vector3f();
                }
            } else refpoint=new Vector3f();
        } 
        return refpoint;
    }

    public ModelInfo getModelInfo() {
        return info;
    }

    public void setModelInfo(ModelInfo info) {
        this.info = info;
    }
    
    public void setModels(SharedMesh [][] models) {
        this.models=models;
        currentModel=0;
        currentLod=0;
        needsLodRefresh=true;
        this.detachAllChildren();
        for(int i=0;i<models.length;i++)
            if(models[i]!=null)
                for(int j=0;j<models[i].length;j++)
                    if(models[i][j]!=null)
                        this.attachChild(models[i][j]);
    }
            
    public void draw(Renderer r) {
        // do we have a models to draw?
        if(models==null) return;
        // do we need to update lod
        if(models[currentModel].length>1) {
            if(!r.getCamera().getLocation().equals(this.prevCamPosition)) {
                this.needsLodRefresh=true;
            }
        }
        prevCamPosition.set(r.getCamera().getLocation());
        if(needsLodRefresh) {
            updateLod();
        }
        GameClient gc=hdr.getLocalClient();
        
        if(sf!=null) {
            sf.checkOnScreen(r);
            if(sf.isEnabled()) {
                if(sf.getParent()==null) {
                    if(gc!=null) {
                        gc.getGuiNode().attachChild(sf);
                        sf.updateRenderState();
                    }
                }
                
                if(!selected && gc.canSelect(this)) {
                    // check if clients selection touches the box
                    SelectionBox sb=gc.getSelectionBox();
                    if(sb.isEnabled()) {
                        if(sb.checkSelection(sf)) {
                            // this object is selected
                            gc.addSelected(this);
                        }
                    }
                }
                if(selected)
                    sf.setLineWidth(3);
                else
                    sf.setLineWidth(1);
                if(team==1) {
                    sf.setDefaultColor(ColorRGBA.blue);
                } else if(team==2) {
                    sf.setDefaultColor(ColorRGBA.red);
                } else sf.setDefaultColor(ColorRGBA.green);
            } else {
                if(sf.getParent()!=null)
                    sf.getParent().detachChild(sf);
            }
        }
        if(gc.canView(this)) {
            if(currentMesh!=null) {
                //currentMesh.setParent(this);
                currentMesh.setLocalTranslation(worldTranslation);
                currentMesh.setLocalRotation(worldRotation);
                currentMesh.setLocalScale(worldScale);
                currentMesh.draw(r);
                /*
                Spatial child;
                for (int i = 0, cSize = children.size(); i < cSize; i++) {
                    child = (Spatial) children.get(i);
                    if (child != null) {
                        child.setLocalTranslation(worldTranslation);
                        child.setLocalRotation(worldRotation);
                        child.setLocalScale(worldScale);
                        //child.setDefaultColor(color);
                        child.draw(r);
                    }
                }
                 */
                //currentMesh.setParent(null);
            }
        }
    }
    
    protected void updateLod() {
        float distance=worldCenter.set(localTranslation).subtractLocal(prevCamPosition).length();
        float stepdist=Config.modelSightdistance/(float)models[currentModel].length;
        currentLod=(int)(distance/stepdist);
        // get the curent visible model
        getModel();
        if(selectbox ) {
            // update selection box if needed
            GameClient gc=hdr.getLocalClient();
            if(gc!=null && gc.canViewGuiBox(this)) {
                if(sf==null) {
                    sf=new SelectFrame(getName()+"guiBox", selBox);
                }
            } else {
                if(sf!=null) {
                    sf.setEnabled(false);
                    if(sf.getParent()!=null) {
                        sf.getParent().detachChild(sf);
                    }
                    sf=null;
                }
            }
        }
    }
    
    public void applyStates() {
        RenderState tempState;
        for (int i = 0; i < states.length; i++) {
                tempState = states[i];
                if (tempState != null) {
                    // TODO: save old states?
                    currentMesh.setRenderState(tempState);
                }
        }
    }
    
    protected void applyRenderState(Stack[] states) {
        for (int x = 0; x < states.length; x++) {
                if (states[x].size() > 0) {
                        this.states[x] = ((RenderState) states[x].peek()).extract(
                                        states[x], this);
                }
        }
        super.applyRenderState(states);
    }

    public boolean hasModels() {
        return this.models!=null && models[0].length>0?true:false;
    }
    
    public void findTrianglePick(Ray toTest, ArrayList results, int batchIndex) {
        SharedMesh target=getModel();
        if(target!=null) {
            target.setLocalTranslation(worldTranslation);
            target.setLocalRotation(worldRotation);
            target.setLocalScale(worldScale);
            target.updateWorldBound();
            target.findTrianglePick(toTest, results, batchIndex);
        }
    }
    
    public void findPick(Ray toTest, PickResults results) {
        if (getWorldBound() != null && isCollidable) {
            if (getWorldBound().intersects(toTest)) {
                // further checking needed.
                for (int i = 0; i < getQuantity(); i++) {
                    Spatial target=(Spatial) children.get(i);
                    if(target!=null) {
                        target.setLocalTranslation(worldTranslation);
                        target.setLocalRotation(worldRotation);
                        target.setLocalScale(worldScale);
                        target.updateWorldBound();
                        target.findPick(toTest, results);
                    }
                }
            }
        }
    }
    
    private void checkSelected() {
        if(selectbox) {
            GameClient gc=hdr.getLocalClient();
            if(gc!=null) {
                SelectionBox sb=gc.getSelectionBox();
                if(sb!=null && sb.isEnabled()) {
                    
                }
            }
        }
    }
    
    public int getTeam() {
        return team;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected=selected;
    }
}
