/*
 * ModelStore.java
 *
 * Created on 2006. február 18., 14:51
 *
 * Produces instances of models
 */

package demoviewer.resource;

import demoviewer.n3di.ModelInfo;
import demoviewer.render.ShaderedMesh;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author vear
 */
public class ModelStore {
    
    // the mesh store
    HashMap<String,ArrayList<ShaderedMesh>> store=new HashMap<String,ArrayList<ShaderedMesh>>();
    // the type_id, 3di model connection
    HashMap<Integer,ModelInfo> infos=new HashMap<Integer,ModelInfo>();
            
    /** Creates a new instance of ModelStore */
    public ModelStore() {
        
    }
    
    public ModelInfo getModelInfo(int type_id) {
        return infos.get(new Integer(type_id));
    }
    
    public void addModelInfo(ModelInfo inf) {
        infos.put(new Integer(inf.getTypeId()), inf);
    }
    
    public void addModel(String name, ArrayList<ShaderedMesh> mdl) {
        store.put(name, mdl);
    }
    
    public ArrayList<ShaderedMesh> getModel(String name) {
        return store.get(name);
    }
    
}
