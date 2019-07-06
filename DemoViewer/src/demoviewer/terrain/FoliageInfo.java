/*
 * nFoliage.java
 *
 * Created on 2006. január 22., 22:51
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer.terrain;

/**
 *
 * @author vear
 */
public class FoliageInfo {
    
    private String model;
    private int map_index;
    
    /** Creates a new instance of nFoliage */
    public FoliageInfo() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMapIndex() {
        return map_index;
    }

    public void setMapIndex(int mapindex) {
        this.map_index = mapindex;
    }
    
}
