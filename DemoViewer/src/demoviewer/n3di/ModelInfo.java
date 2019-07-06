/*
 * ModelInfo.java
 *
 * Created on 2006. február 18., 15:09
 *
 */

package demoviewer.n3di;

/**
 *
 * @author vear
 */
public class ModelInfo {
       
    // type_id of the object
    private int id;
    public static final int TYPE_PERSON = 0;
    public static final int TYPE_VEHICLE = 1;
    public static final int TYPE_OBJECT = 2;
    public static final int TYPE_BUILDING = 3;
    public static final int TYPE_DECORATION = 4;
    public static final int TYPE_FOLIAGE = 5;
    public static final int TYPE_MARKER = 6;
    public static final int TYPE_POWERUP = 7;
    // type
    private int type;
    // name
    private String description;
    // sid
    private String sid;
    // model name
    private String graphic;
    // husk name
    private String husk;
    
    /** Creates a new instance of ModelInfo */
    public ModelInfo() {
    }

    public int getTypeId() {
        return id;
    }

    public void setTypeId(int type_id) {
        this.id = type_id;
    }

    public String getModelName() {
        return graphic;
    }

    public void setModelName(String modelname) {
        this.graphic = modelname;
    }

    public String getHusk() {
        return husk;
    }

    public void setHusk(String husk) {
        this.husk = husk;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
    
}
