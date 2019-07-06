/*
 * MapItemMessage.java
 *
 * Created on 2006. február 19., 11:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.map;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import demoviewer.n3di.ModelInfo;
import demoviewer.resource.ModelStore;
import demoviewer.resource.ResourceManager;
import xml.Element;

/**
 *
 * @author vear
 */
public class MapItemMessage {
    
    public static final String [] TYPE_TAGS = {"AI", "VEHICLE", "DYNAMIC", "STATIC", "STATIC", "STATIC", "MARKER", "DYNAMIC" };
    
    // the time this message is current
    // the time is counted from map start
    private float timestamp;
    // the ssn
    private int ssn;
    // type_id
    private int type_id=-1;
    // the object type (from ModelInfo)
    private int type;
    // the type, from element
    private String type_string;
    // team
    private byte team=-1;
    // position
    private Vector3f position;
    // absolute position, if not set, the object is terrain aligned
    private boolean absolute;
    // rotation angles
    private float[] rotation;
    
    /** Creates a new instance of MapItemMessage */
    public MapItemMessage() {
    }
    
    public void fromXML(Element e) {
        Element ce;
        ce=e.getChild("TIME");
        if(ce!=null) {
            this.setTimestamp(e.getTextfloat());
        }
        ce=e.getChild("ID");
        if(ce!=null) {
            this.setID(ce.getTextint());
        }
        ce=e.getChild("TYPE_ID");
        if(ce!=null) {
            this.setType_id(ce.getTextshort());
            // get the type by elements name
            type_string=e.getName();
        }
        ce=e.getChild("TEAM");
        if(ce!=null) {
            this.setTeam(ce.getTextbyte());
        }
        ce=e.getChild("POSITION");
        if(ce!=null) {
            if(position==null) position=new Vector3f();
            position.set(0,0,0);
            if(rotation==null) rotation=new float[3];
            rotation[0]=0;rotation[1]=0;rotation[2]=0;
            Element pe;
            pe=ce.getChild("X");
            if(pe!=null) position.x=pe.getTextfloat();
            pe=ce.getChild("Y");
            if(pe!=null) position.y=pe.getTextfloat();
            pe=ce.getChild("Z");
            if(pe!=null) position.z=pe.getTextfloat();
            pe=ce.getChild("ABSOLUTE");
            if(pe!=null) {
                if(pe.getText().toLowerCase().equals("true"))
                    this.setAbsoluteHeight(true);
                else
                    this.setAbsoluteHeight(false);
            }
            pe=ce.getChild("THETA");
            if(pe!=null) rotation[1]=pe.getTextfloat()*FastMath.DEG_TO_RAD;
            pe=ce.getChild("PHI");
            if(pe!=null) rotation[2]=pe.getTextfloat()*FastMath.DEG_TO_RAD;
            pe=ce.getChild("OMEGA");
            if(pe!=null) rotation[0]=pe.getTextfloat()*FastMath.DEG_TO_RAD;
        }
        
    }

    public Element toXML(boolean stream) {
        Element e;
        String tag;
        if(type_string==null && type!=-1) {
            type_string = TYPE_TAGS[type];
        }
        if(type_string==null) {
            tag="OBJECT";
        } else {
            tag=type_string;
        }
        e=new Element(tag);
        Element ce;
        
        // if streaming, add our own data too
        if(stream) {
            e.addContent(new Element("TIME").setText(timestamp));
        }
        if(ssn!=0) {
            e.addContent(new Element("ID").setText(ssn));
        }
        if(type_id!=-1) {
            e.addContent(new Element("TYPE_ID").setText((short)type_id));
        }
        if(team!=-1) {
            e.addContent(new Element("TEAM").setText((byte)team));
        }
        if(position!=null || rotation != null) {
            ce=new Element("POSITION");
            if(position!=null) {
                ce.setChild("X").setText(position.x);
                ce.setChild("Y").setText(position.y);
                ce.setChild("Z").setText(position.z);
                ce.setChild("ABSOLUTE").setText(absolute);
            }
            if(rotation != null) {
                ce.addContent(new Element("THETA").setText(rotation[1]*FastMath.RAD_TO_DEG));
                ce.addContent(new Element("PHI").setText(rotation[0]*FastMath.RAD_TO_DEG));
                ce.addContent(new Element("OMEGA").setText(rotation[2]*FastMath.RAD_TO_DEG));
            }
            e.addContent(ce);
        }
        return e;
    }
    
    public float getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(float timestamp) {
        this.timestamp = timestamp;
    }

    public int getID() {
        return ssn;
    }

    public void setID(int ssn) {
        this.ssn = ssn;
    }

    public int getType_id() {
        return type_id;
    }

    public void setType_id(int type_id) {
        this.type_id = type_id;
        // get the type and type string from modelstore
        ModelStore ms=ResourceManager.getInstance().getModelStore();
        ModelInfo mi=ms.getModelInfo(type_id);
        if(mi!=null) {
            this.type=mi.getType();
        } else {
            type=-1;
        }
    }

    public byte getTeam() {
        return team;
    }

    public void setTeam(byte team) {
        this.team = team;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public boolean isAbsoluteHeight() {
        return absolute;
    }

    public void setAbsoluteHeight(boolean absolute) {
        this.absolute = absolute;
    }

    public float[] getRotationAngles() {
        return rotation;
    }

    public void setRotationAngles(float[] rotation) {
        this.rotation = rotation;
    }
    
    public Quaternion getRotationQuaternion(Quaternion store) {
        if(rotation==null) rotation=new float[] {0,0,0};
        if(store==null) store = new Quaternion();
        store.fromAngles(rotation);
        return store;
    }
}
