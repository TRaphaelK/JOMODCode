/*
 * nResource.java
 *
 * Created on 2006. január 22., 21:45
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer.resource;

import nu.xom.Element;

/**
 *
 * @author vear
 */
public class nResource {
    
    /* location: 0-not present
     *              1-pff file
     *              2-extracted
     *              3-decompressed
     *              4-prepared
     *              5-cached
     */
    public static final int LOCATION_NONE = 0;
    public static final int LOCATION_PFF = 1;
    public static final int LOCATION_EXTRACTED = 2;
    public static final int LOCATION_DECOMPRESSED = 3;
    public static final int LOCATION_PREPARED = 4;
    public static final int LOCATION_CACHED = 5;
    
    private int location;
    // the original name of the resource
    public String originalname;

    /** Creates a new instance of nResource */
    public nResource() {
    }
        
    public String getName()
    {
        return originalname;
    }
    
    public void setName(String name)
    {
        originalname=name;
    }
    
    public void fromXML(Element el)
    {
        originalname=el.getFirstChildElement("originalname").getValue();
        location=Integer.parseInt(el.getFirstChildElement("location").getValue());
    }
    
    public Element toXML() {
        Element el=new Element("resource");
        Element c;
        c=new Element("originalname"); c.appendChild(String.valueOf(originalname)); el.appendChild(c);
        c=new Element("location"); c.appendChild(String.valueOf(location)); el.appendChild(c);
        return el;
    }

    
    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }
}
