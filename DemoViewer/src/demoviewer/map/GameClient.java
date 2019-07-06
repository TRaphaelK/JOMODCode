/*
 * GameClient.java
 *
 * Created on 2006. április 28., 18:58
 *
 * A client viewing/controlling the game
 */

package demoviewer.map;

import com.jme.scene.Node;
import demoviewer.gui.SelectionBox;
import demoviewer.n3di.ModelInfo;
import java.util.ArrayList;
import java.util.Arrays;
import xml.Element;

/**
 *
 * @author vear
 */
public class GameClient {
    
    private int id=0;
    
    // the current selection box (not replicated)
    private SelectionBox sb;
    // gui node (not replicated)
    private Node guiNode;
    // the objects selected by the local client
    ArrayList<GameObject> selected;
    
    // the teams it can view boxes
    boolean guiBoxTeam[];
    // the teams it can view
    boolean viewTeam[];
    // the teams it can control
    boolean controlTeam[];

    public GameClient() {
        
    }
    
    /** Creates a new instance of GameClient */
    public GameClient(int id, int numTeams) {
        this.id=id;
        guiBoxTeam=new boolean[numTeams];
        viewTeam=new boolean[numTeams];
        controlTeam=new boolean[numTeams];
        selected=new ArrayList<GameObject>();
    }

    public int getId() {
        return id;
    }
        
    // sends a drop message to the client, and disconnects it
    public void drop() {
        
    }

    public SelectionBox getSelectionBox() {
        return sb;
    }

    public void setSelectionBox(SelectionBox sb) {
        this.sb = sb;
    }

    public Node getGuiNode() {
        return guiNode;
    }

    public void setGuiNode(Node guiNode) {
        this.guiNode = guiNode;
    }
    
    public boolean canView(GameObject go) {
        ModelInfo mi=go.getModelInfo();
        if(mi==null) return false;
        if(mi.getType()!=ModelInfo.TYPE_PERSON && mi.getType()!=ModelInfo.TYPE_VEHICLE)
            return true;
        int team=go.getTeam();
        if(team==-1 || team==0) return true;
        return viewTeam[team]|controlTeam[team];
    }
    
    public boolean canViewGuiBox(GameObject go) {
        ModelInfo mi=go.getModelInfo();
        if(mi==null) return false;
        // TODO check for emplaced, PSP and LFP's
        if(mi.getType()!=ModelInfo.TYPE_PERSON && mi.getType()!=ModelInfo.TYPE_VEHICLE)
            return false;
        int team=go.getTeam();
        if(team==-1 || team==0) return true;
        return guiBoxTeam[team]|viewTeam[team]|controlTeam[team];
    }
    
    public boolean canControl(GameObject go) {
        ModelInfo mi=go.getModelInfo();
        if(mi==null) return false;
        // TODO check for emplaced, PSP and LFP's
        if(mi.getType()!=ModelInfo.TYPE_PERSON && mi.getType()!=ModelInfo.TYPE_VEHICLE)
            return false;
        int team=go.getTeam();
        if(team==-1 || team==0) return false;
        return controlTeam[team];
    }
    
    public boolean canSelect(GameObject go) {
        ModelInfo mi=go.getModelInfo();
        if(mi==null) return false;
        // TODO check for emplaced, PSP and LFP's
        if(mi.getType()!=ModelInfo.TYPE_PERSON && mi.getType()!=ModelInfo.TYPE_VEHICLE)
            return false;
        int team=go.getTeam();
        if(team==-1 || team==0) {
            if(mi.getType()==ModelInfo.TYPE_VEHICLE) return true;
            return false;
        }
        return controlTeam[team];
    }
    
    public boolean isCanControl(int team) {
        return controlTeam[team];
    }
    
    public void setCanControl(int team, boolean contr) {
        controlTeam[team]=contr;
    }
    
    public Element toXML() {
        Element el=new Element("GameClient");
        if(id!=0) el.setChild("id").setText((byte)id);
        if(viewTeam!=null) {
            el.setChild("viewTeam").setText(viewTeam);
        }
        if(controlTeam!=null) {
            el.setChild("controlTeam").setText(controlTeam);
        }
        return el;
    }
    
    public void fromXML(Element el) {
        id=el.getChildbyte("id");
        el.getChildbitset("viewTeam", viewTeam);
        el.getChildbitset("controlTeam", controlTeam);
    }
    
    public void fromOther(GameClient other) {
        // copy data from networked message into game
        // object
        // do not copy id
        if(other.viewTeam!=null) {
            for(int i=0;i<other.viewTeam.length && i<viewTeam.length; i++) {
                viewTeam[i]=other.viewTeam[i];
            }
        }
        if(other.controlTeam!=null) {
            for(int i=0;i<other.controlTeam.length && i<controlTeam.length; i++) {
                controlTeam[i]=other.controlTeam[i];
            }
        }
    }
    
    public void addSelected(GameObject go) {
        if(!go.isSelected()) {
            selected.add(go);
            go.setSelected(true);
        }
    }
    
    public void clearSelected() {
        for(int i=0,j=selected.size();i<j;i++) {
            GameObject go=selected.get(i);
            go.setSelected(false);
        }
        selected.clear();
    }
    
    public ArrayList<GameObject> getSelected() {
        return selected;
    }
}
