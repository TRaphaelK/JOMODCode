/*
 * NpjLoader.java
 *
 * Created on 2006. február 19., 11:18
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.map.loaders;

import demoviewer.map.MapHeader;
import demoviewer.map.MapHeaderMessage;
import demoviewer.map.MapItemMessage;
import java.util.ArrayList;
import xml.Element;

/**
 *
 * @author vear
 */
public class NpjLoader {
    
    /** Creates a new instance of NpjLoader */
    public NpjLoader() {
    }
    
    public void loadNpj(MapHeader map, String npj) {
        // get the npj XML
        NPJFile nf=new NPJFile();
        Element np=nf.fromXML(npj);
        if(np!=null) {
            // first, create the header element
            MapHeaderMessage mh = new MapHeaderMessage();
            // get MISSION_NAME
            Element ce;
            ce=np.getChild("MISSION_NAME");
            if(ce!=null) {
                mh.fromXML(ce);
            }
            ce=np.getChild("TERRAIN");
            if(ce!=null) {
                mh.fromXML(ce);
            }
            // send the header to the map
            map.addMapHeaderMessage(mh);
            
            // get elements with ID in it
            ArrayList<Element> es=np.getChildren();
            for(int i=0;i<es.size();i++) {
                ce=es.get(i);
                Element ide;
                ide=ce.getChild("ID");
                if( ide!=null && !ce.getName().equals("GROUP") && !ce.getName().equals("WAYPOINT_LIST") ) {
                    // create an object from this
                    MapItemMessage mi=new MapItemMessage();
                    mi.fromXML(ce);
                    // send it to the map handler
                    map.addMapItemMessage(mi);
                }
            }
        }
    }
}
