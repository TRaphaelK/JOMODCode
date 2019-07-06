/*
 * 
 * Vear 2017  * 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package jb2.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.ent.Entity;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.IntMap;
import jb2.util.JbException;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class NavCell {
    protected static final Logger log = Logger.getLogger(NavCell.class.getName());
    
    protected static final float autoSplitDownExtent = 4f;
    protected static final float splitDownExtent = 2f;
    protected static final float splitDownExtentY = 1f;
    protected static final float cellDiffCreateTreshold = 30f;
    protected static final float cellRatioCreateTreshold = 3f;
    
    // index into the navnode arrays
    public int nodeid;
    // position of the node
    public BoundingBox bounds=new BoundingBox();
    
    // the links of this node int,Link
    public FastList<NavLink> links;// =  new FastList<>();
    // the links to this node, for reverse lookup
    public FastList<NavLink> reverseLinks;// =  new FastList<>();
    
    // the octree of children
    protected NavCell[] children;
    
    // the count bots succesfully occupied a potential child node
    // if potential child nodes show different occupation patterns
    // or they stuck going to a specific position occupied by a child
    // node, then split up the nodes (TODO: remove node splitting, only pregenerated maps)
    // stats per child bots succesfully occupied the node
    //protected float[] cellStat;
    
    public static final int numAttrs = 9;
    // the collected costs of he node,
    // not saved and loaded, reset after every LFP change
    // 0 - planned usage team 1
    // 1 - current usage team 1
    // 2 - death team 1
    
    // 3 - planned usage team 2
    // 4 - current usage team 2
    // 5 - death team 2
    
    // 6 - total occupation of node (not decayed)
    // 7 - min terrain height
    // 8 - max terrain height 
    protected float[] nodeAttr;
    
    // splitting of node disabled
    //boolean nosplit;
    
    // node center on terrain level
    //public boolean terrainLevel;
    //public boolean terrainLevelDetermined;
    

    public NavCell(int nodeId) {
        this.nodeid = nodeId;
    }

    protected void getAllLeafs(FastList<NavCell> store) {
        // this is a leaf
        if(children==null)
            return;
        
        FastList<NavCell> addnodes= LocalContext.getContext().NavCell_getAll_addnodes;
        addnodes.clear();
        addnodes.add(this);
        while (addnodes.size > 0) {
            // pop the last element from the stack
            NavCell node = addnodes.remove();
            for (int index = 0; index < node.children.length; index++) {
                NavCell element = node.children[index];
                if (element == null)
                    continue;
                if (element.children ==null) {
                    // add the leaf
                    store.add(element);
                    continue;
                }
                addnodes.add(element);
            }
        }
    }
    
    protected void getAll(FastList<NavCell> store) {
        // this is a leaf
        if(children==null)
            return;
        
        FastList<NavCell> addnodes= LocalContext.getContext().NavCell_getAll_addnodes;
        addnodes.clear();
        addnodes.add(this);
        while (addnodes.size > 0) {
            // pop the last element from the stack
            NavCell node = addnodes.remove();
            for (int index = 0; index < node.children.length; index++) {
                NavCell element = node.children[index];
                if (element == null)
                    continue;
                // add the element
                store.add(element);
                if (element.children ==null) {
                    continue;
                }
                // process children
                addnodes.add(element);
            }
        }
    }
    
    protected void getIntersecting(BoundingBox bb, 
                    FastList<NavCell> store, 
                    boolean threeD) {
        
        if(this.children == null) {
            // node is a leaf, add it
            // its here, because intersection might be called on a leaf too
            store.add(this);
            return;
        }

        FastList<NavCell> checknodes= LocalContext.getContext().NavCell_getIntersecting_checknodes;
        checknodes.clear();
        
        boolean bbcheck;
        
        checknodes.add(this);
        while(checknodes.size>0) {
            // pop the last element from the stack
            NavCell node = checknodes.remove();

            if(threeD)
                bbcheck = bb.intersects(node.bounds);
            else
                bbcheck = bb.intersects2d(node.bounds);
            // is there an overlap
            if(!bbcheck) {
                // no intersection at all
                continue;
            }
            
            if(threeD)
                bbcheck = bb.contains(node.bounds);
            else
                bbcheck = bb.contains2d(node.bounds);
            
            // bb contains everything in the bb
            if(bbcheck) {
                // bb contains everything in this node and its children
                // add everything unconditionally
                node.getAllLeafs(store);
                // node completely added
                continue;
            }
            
            // check child nodes selectively
            for(int index=0; index<node.children.length; index++) {
                NavCell element = node.children[index];
                if(element== null)
                    continue;
                if(element.children == null) {
                    if(threeD)
                        bbcheck = bb.intersects(element.bounds);
                    else
                        bbcheck = bb.intersects2d(element.bounds);
                    if(bbcheck) {
                        store.add(element);
                    }
                    continue;
                }
                // recurse into that child too
                checknodes.add(element);
            }
        }
    }
    
    public NavLink createLink(NavCell to) {
        if(this==to)
            return null;
        // check if already exists
        NavLink link = getLink(to);
        if(link!=null) 
            return link;

        link = new NavLink();
        link.from = this;
        link.to = to;

        // fill in distance
        link.linkcost[0] = this.bounds.center.distance(to.bounds.center);
        links.add(link);        
        // add into reverse links of to
        to.reverseLinks.add(link);
        
        return link;
    }
    
    public NavLink getLink(NavCell linkTo) {
        if(links==null) {
            return null;
        }
        for(int i=0; i<links.size; i++) {
            NavLink link = links.get(i);
            if(link.to == linkTo)
                return link;
        }
        return null;
    }
    
    public void initialSplit(BoundingBox area) {
        // subdivide space until splitDownExtent size boxes on the the given area
        
        if(!area.intersects(bounds)) {
            // no intersection
            return;
        }
        
        // node too small to be split further
        if(this.bounds.extents.x<splitDownExtent) {
            return;
        }
        //if(this.nosplit)
        //   return;
        
        FastList<NavCell> check = LocalContext.getContext().NavCell_initialSplit_check;
        check.clear();
        check.add(this);
        while(!check.isEmpty()) {
            NavCell cell = check.remove();

            // check if the cell is big enough for subdivision
            if(cell.bounds.extents.x<autoSplitDownExtent)
                continue;
            if(cell.children==null) {
                // split the cell, don't process links
                cell.split();
            }
            // the node is already subdivided, check which child needs further processing
            for(int i=0; i<cell.children.length; i++) {
                NavCell child = cell.children[i];
                if(!area.intersects(child.bounds)) {
                    // child is not intersecting the area, no need to subdivide further
                    continue;
                }
                // add for further subdivision
                check.add(child);
            }
        }
    }
    
    protected void split() {
        if(children!=null)
            return;
        // an octree of quadtree?
        if(bounds.extents.y>splitDownExtentY) {
            // its an octree
            children = new NavCell[8];
        } else {
            // subdivision on y extent is not necesarry, its a quadtree
            children = new NavCell[4];
        }
        // create child nodes
        for(int i=0; i<children.length; i++) {
            // based on index create the bounds
            createChild(i);
        }
    }

    protected int getIndex(Vector3f position) {
        boolean asoctree = false;
        if(children!=null) {
            if(children.length>4) {
                asoctree = true;
            }
        } else {
            // if it were split, would it be an octree?
            if(bounds.extents.y>splitDownExtentY) {
                asoctree = true;
            }
        }
        int index = 0;
        if(asoctree) {
            // consider Y
            if(position.y>=this.bounds.center.y)
                index+=4;
        }
        if(position.x>=this.bounds.center.x)
            index+=2;
        if(position.z>=this.bounds.center.z)
            index+=1;
        
        return index;
    }

    protected NavCell createChild(int index) {
        // set ref from parent to this node
        float halfx = bounds.extents.x / 2f;
        float halfy = bounds.extents.y / 2f;
        float halfz = bounds.extents.z / 2f;

        float centerx = bounds.center.x;
        float centery = bounds.center.y;
        float centerz = bounds.center.z;
        
        int whichhalf = index;
        // start with y as this extent is optional
        if(whichhalf>=4) {
            centery += halfy;
            whichhalf-=4;
        } else {
            // if octree
            if(children.length>4)
                centery -= halfy;
            else {
                // quadtree, center and extent on y remains the same
                halfy = bounds.extents.y;
            }
        }
        if(whichhalf>=2) {
            centerx += halfx;
            whichhalf-=2;
        } else {
            centerx -= halfx;
        }
        if(whichhalf>=1) {
            centerz += halfz;
        } else {
            centerz -= halfz;
        }
        
        NavCell cell = AppContext.navGrid.createCellDirect();
        BoundingBox tmpBounds = cell.bounds;
        tmpBounds.center.x = centerx;
        tmpBounds.center.y = centery;
        tmpBounds.center.z = centerz;

        // store the halved extents
        tmpBounds.extents.x = halfx;
        tmpBounds.extents.y = halfy;
        tmpBounds.extents.z = halfz;
                
        //if(halfx<splitDownExtent) {
        //    cell.nosplit = true;
        //}
        
        // store ref to child in parent        
        children[index] = cell;
        
        return cell;
    }

    public NavCell getLeaf(Vector3f position) {
        // position is not contained
        if(!this.bounds.contains(position))
            return null;

        if(this.children == null) {
            // node is a leaf containing the position, return it
            return this;
        }

        NavCell node = this;
        
        while(node!=null) {

            // determine the index into the child array
            int index = node.getIndex(position);
            
            // is there a child at that position?
            
            // check child nodes selectively
            NavCell element = node.children[index];
            if(element== null) {
                // should never happen as all children should be initialised 
                throw new JbException("Uninitilised child");
            }
            if(!element.bounds.contains(position)) {
                // this should never happen
                // buit it does because of Float imprecision, so just ignore it
                //throw new JbException("Child with proper index does not contain position");
            }
            if(element.children == null) {
                // this is the leaf we are looking for
                return element;
            }
            // recurse into the child
            node = element;
        }
        return null;
    }

    public boolean isLeaf() {
        return children==null;
    }

    /*
    public void addStat(Vector3f position, float value) {
        // find the bottom child node containing the point
        NavCell cell = getLeaf(position);
        if(cell==null) {
            return;
        }

        // is it possible to further subdivide the node?
        if(cell.nosplit ) {
            // not possible to split the node, ignore
            return;
        }

        // initialise cell stats
        if(cell.cellStat==null) {
            // an octree of quadtree?
            if(cell.bounds.extents.y>splitDownExtentY) {
                // its an octree
                cell.cellStat = new float[8];
            } else {
                // subdivision on y extent is not necesarry, its a quadtree
                cell.cellStat = new float[4];
            }
        }

        // determine the index into child array
        int index = cell.getIndex(position);
        
        // increase the counter on the quadrant the point points to
        cell.cellStat[index]+=value;

        // calculate the min and max values
        float minVal = cell.cellStat[0];
        float maxVal = cell.cellStat[0];

        for(int i=1; i<cell.cellStat.length; i++) {
            if(cell.cellStat[i]<minVal)
                minVal=cell.cellStat[i];
            if(cell.cellStat[i]>maxVal)
                maxVal=cell.cellStat[i];
        }
        // check if the value went over a treshold
        if(!(maxVal-minVal>=cellDiffCreateTreshold
                && ( minVal == 0 || maxVal/minVal >= cellRatioCreateTreshold))) {
            // not past treshold yet
            return;
        }

        // create the children, adjust the links
        cell.split(true);
        log.log(Level.FINER, "Cell {0} split up", cell.nodeid);
    }
*/
    
    public void fromFile(DataInputStream input, IntMap<NavCell> knownNodes) throws IOException {

        bounds.fromFile(input);
        
        // the number of links on a separate line
        int numChildRen = input.readInt();
        if(numChildRen>0) {
            this.children = new NavCell[numChildRen];
            for(int i=0; i<numChildRen; i++) {
                int childId = input.readInt();
                if(childId==0)
                    continue;
                NavCell child = knownNodes.get(childId);
                if(child==null) {
                    // not yet in know nodes, add it
                    // it will be filled later
                    child = new NavCell(childId);
                    knownNodes.put(childId, child);
                }
                children[i]=child;
            }
        }

        // attributes
        int numStats = input.readInt();
        if(numStats>0) {
            this.initAttr();
            for(int i=0; i<numStats; i++) {
                nodeAttr[i] = input.readFloat();
            }
        }
        
        // split
        //this.nosplit=input.readBoolean();
        
        if(this.children==null) {
            if(links==null)
                links = new FastList<>();
            if(reverseLinks==null)
                reverseLinks = new FastList<>();
        }

        // the number of links on a separate line
        int numLoadedLinks = input.readInt();
        // read the links, each on a new line
        for(int i=0; i<numLoadedLinks; i++) {
            NavLink link = new NavLink();
            link.from = this;
            int linkTo = input.readInt();
            link.to = (NavCell) knownNodes.get(linkTo);
            if(link.to==null) {
                // not yet in know nodes, add it
                // it will be filled later
                link.to = new NavCell(linkTo);
                knownNodes.put(linkTo, link.to);
            }
            // process the link costs
            for(int j=0; j<link.linkcost.length; j++) {
                link.linkcost[j] = input.readFloat();
            }
            links.add(link);
            if(link.to.reverseLinks==null)
                link.to.reverseLinks = new FastList<>();
            link.to.reverseLinks.add(link);
        }
    }
    
    public boolean isOnLandLevel() {
        if(!Float.isNaN(nodeAttr[7])) {
            return true;
        }
        float height = AppContext.heightMap.getHeight(bounds.center);
        if(height == 0) {
            return false;
        }
        return isValidHeight(height);
    }
    
    public boolean isTerrainLevelKnown() {
        if(!Float.isNaN(nodeAttr[7]))
            return true;

        float height = AppContext.heightMap.getHeight(bounds.center);
        if(height == 0) {
            return false;
        }
        return true;
    }
    
    public void recalcMinMaxHeight() {
        float[] minmax = new float[2];
        AppContext.heightMap.getAreaMinMax(bounds, minmax);
        if(minmax[0]!=0) {
            nodeAttr[7] = minmax[0];
        }
        if(minmax[1]!=0) {
            nodeAttr[8] = minmax[1];
        }
    }
    
    public boolean isValidHeight(float height) {
        return height >= bounds.center.y - bounds.extents.y && height <= bounds.center.y + bounds.extents.y;
    }
    
    public float getWalkableHeight(Vector3f position) {
        float height = AppContext.heightMap.getHeight(position);
        if(height != 0 && isValidHeight(height)) {
            // terrain level
            return height;
        }
        // minimum known height for the cell
        height=getMinHeight();
        if(height!=0) {
            return height;
        }
        // unknown, then return the bottom of the cell
        return bounds.center.y - bounds.extents.y;
    }

    public float getMinHeight() {
        if(nodeAttr==null)
            return 0;
        if(Float.isNaN(nodeAttr[7]))
            return 0;
        return nodeAttr[7];
    }
    
    public void toFile(DataOutputStream output) throws IOException {
        
        bounds.toFile(output);

        if(children==null) {
            output.writeInt(0);
        } else {
            output.writeInt(children.length);
            for(int i=0;i<children.length; i++) {
                NavCell cell = children[i];
                if(cell==null) {
                    output.writeInt(0);
                } else {
                    output.writeInt(cell.nodeid);
                }
            }
        }

        if(nodeAttr==null) {
            output.writeInt(0);
        } else {
            output.writeInt(nodeAttr.length);
            // node costs on a new line
            for(int i=0; i<nodeAttr.length; i++) {
                output.writeFloat(nodeAttr[i]);
            }
        }
        //output.writeBoolean(nosplit);

        if(links==null) {
            output.writeInt(0);
        } else {
            // retrieve the links
            output.writeInt(links.size());

            // each link data on new line
            for(int i=0; i<links.size(); i++) {
                NavLink link = links.get(i);
                output.writeInt(link.to.nodeid);
                for(int j=0; j<link.linkcost.length; j++) {
                    output.writeFloat(link.linkcost[j]);
                }
            }
        }
    }
   
    
    public void prepareLeafs() {
        forEachLeaf(
                leaf -> {
                    leaf.initAttr();
                    leaf.generateLinks();
                }
        );
    }
    
    public void forEachLeaf(Consumer<NavCell> f) {
        // this is a leaf
        if(children==null) {
            f.accept(this);
            return;
        }
        
        FastList<NavCell> check= LocalContext.getContext().NavCell_forEachLeaf_check;
        check.clear();
        check.add(this);
        while (check.size > 0) {
            // pop the last element from the stack
            NavCell node = check.remove();
            for (int index = 0; index < node.children.length; index++) {
                NavCell element = node.children[index];
                if (element == null)
                    continue;
                if (element.isLeaf()) {
                    // add the leaf
                    f.accept(element);
                    continue;
                }
                check.add(element);
            }
        }
    }

    public void forEachIntersectingLeaf(BoundingBox bb, boolean twoD,
                    Consumer<NavCell> f) {
        
        if(this.isLeaf()) {
            // node is a leaf, add it
            // its here, because intersection might be called on a leaf too
            if(bb.intersects(bounds, twoD))
                f.accept(this);
            return;
        }

        FastList<NavCell> checknodes= LocalContext.getContext().NavCell_forEachIntersectingLeaf_checknodes;
        checknodes.clear();
        
        boolean bbcheck;
        
        checknodes.add(this);
        while(checknodes.size>0) {
            // pop the last element from the stack
            NavCell node = checknodes.remove();

            bbcheck = bb.intersects(node.bounds, twoD);
            // is there an overlap
            if(!bbcheck) {
                // no intersection at all
                continue;
            }
            
            bbcheck = bb.contains(node.bounds, twoD);
            // bb contains everything in the bb
            if(bbcheck) {
                // bb contains everything in this node and its children
                // apply to everything unconditionally
                node.forEachLeaf(f);
                // node completely added
                continue;
            }
            
            // check child nodes selectively
            for(int index=0; index<node.children.length; index++) {
                NavCell element = node.children[index];
                if(element== null)
                    continue;
                if(element.children == null) {
                    bbcheck = bb.intersects(element.bounds, twoD);
                    if(bbcheck) {
                        f.accept(element);
                    }
                    continue;
                }
                // recurse into that child too
                checknodes.add(element);
            }
        }
    }
    
    protected void generateLinks() {
        if(links==null)
            links = new FastList<>();
        if(reverseLinks==null)
            reverseLinks = new FastList<>();
        
        BoundingBox bb = LocalContext.getContext().NavCell_generateLinks_bb;
        bb.set(bounds);
        // get all the already existing nodes around the node
        bb.extents.x += 0.1f;
        bb.extents.z += 0.1f;
        bb.extents.y += 0.1f;
        FastList<NavCell> nodeList = LocalContext.getContext().NavCell_generateLinks_nodeList;
        nodeList.clear();
        nodeList = AppContext.navGrid.getCells(bb, nodeList);
        //nodeList.remove(createdNode);
        // connect with all nodes, two-way, ignoring self
        for (int i = 0; i < nodeList.size; i++) {
            NavCell other = nodeList.get(i);
            if(other==this)
                continue;
            if(other.links==null)
                other.links = new FastList<>();
            if(other.reverseLinks==null)
                other.reverseLinks = new FastList<>();
            other.createLink(this);
            this.createLink(other);
        }
    }
    
    protected void initAttr() {
        if(nodeAttr !=null) 
            return;
        nodeAttr = new float[numAttrs];
        // init min height to max and max to min
        nodeAttr[7] = Float.NaN;
        nodeAttr[8] = Float.NaN;
    }
    
    public void addPlannedTraversal(int team) {
        nodeAttr[(team-1)*3+0]++;
    }

    public void addTraversal(Entity ent) {
        if(ent.position.y > bounds.center.y + bounds.extents.y
                || ent.position.y < bounds.center.y - bounds.extents.y) {
            throw new JbException("Trying to add traversal on non-conatining node");
        }

        // dacaying traversal
        nodeAttr[(ent.team-1)*3+1]++;
        // total traversal
        nodeAttr[6]++;
    }
    
    public void recordHeight(Entity ent) {
        if(ent.position.y > bounds.center.y + bounds.extents.y
                || ent.position.y < bounds.center.y - bounds.extents.y) {
            throw new JbException("Trying to add traversal on non-conatining node");
        }
        // adjust min/max terrain height of the node
        if(Float.isNaN(nodeAttr[7]) || ent.position.y < nodeAttr[7])
            nodeAttr[7] = ent.position.y;
        if(Float.isNaN(nodeAttr[8]) || ent.position.y > nodeAttr[8])
            nodeAttr[8] = ent.position.y;
    }

    public void addDeath(int team) {
        nodeAttr[(team-1)*3+2]++;
    }
    
    public void resetWeights() {
        if(nodeAttr==null)
            return;
        nodeAttr[0]=0;
        nodeAttr[1]=0;
        nodeAttr[2]=0;
    }
    
    public void decayWeights() {
        if(nodeAttr==null)
            return;
        nodeAttr[0]*=0.9f;
        nodeAttr[1]*=0.9f;
        nodeAttr[2]*=0.9f;
    }
    
    public float getWeight(int team, int wno) {
        if(nodeAttr==null)
            return 0;
        return nodeAttr[(team-1)*3+wno];
    }
    
    public float getTotalVisited() {
        return nodeAttr[6];
    }
    
    public void fillRandomPointOnLandLevel(Vector3f point) {
        bounds.fillRandomPointInside(point);
        float height = AppContext.heightMap.getHeight(point);
        if (height == 0) {
            height = getMinHeight();
        }
        if (height != 0) {
            point.y = height;
        }
    }

    @Override
    public String toString() {
        return "NavCell{" + nodeid + "," + bounds + "," + (children==null) + '}';
    }
    
    
}
