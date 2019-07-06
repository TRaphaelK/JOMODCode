/*
 * ShaderedMaterialTexture.java
 *
 * Created on 2006. február 25., 22:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.render;

/**
 *
 * @author vear
 */
public class ShaderedMaterialTexture {
    
    // file name to fetch from
    private String filename;
    // the texture unit it is currently in
    private int unit;


    public ShaderedMaterialTexture(String name) {
        this.filename=name;
    }

    public ShaderedMaterialTexture clone() {
        ShaderedMaterialTexture c=new ShaderedMaterialTexture(filename);
        c.unit=unit;
        return c;
    }

    public void setUnit(int unit) {
        this.unit=unit;
    }

    public int getUnit() {
        return unit;
    }

    public String getTextureName() {
        return filename;
    }

    public void setTextureName(String filename) {
        this.filename = filename;
    }
    
}
