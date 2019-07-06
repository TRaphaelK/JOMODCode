/*
 * QuadNode.java
 *
 * Created on 2006. április 16., 18:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.map;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState;
import java.util.Stack;

/**
 *
 * @author vear
 */
public class QuadNode extends Node {
    
    boolean enabled=true;
    
    // each QuadNode has 4 child nodes and a number of real child nodes
    float minNodeSize;
    boolean rootNode;
    
    /** Creates a new instance of QuadNode */
    public QuadNode(String name) {
        this(name, true);
    }
    
    protected QuadNode(String name, boolean root) {
        super(name);
        rootNode=root;
    }
    
    public void draw(Renderer r) {
        if(!enabled) return;
        
        super.draw(r);
    }
    
    public void updateWorldBound() {
        if(worldBound==null) {
            if(children!=null && children.size()>0) {
                Spatial child = (Spatial) children.get(0);
                worldBound=new BoundingBox();
                worldBound.setCenter(new Vector3f(child.getWorldBound().getCenter()));
            }
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled=enabled;
    }
}
