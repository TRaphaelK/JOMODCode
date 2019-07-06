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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.Config;
import jb2.jo.GameObject;
import jb2.jo.TypeMap;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.IntMap;
import jb2.util.JbException;
import jb2.util.LocalContext;

/**
 * Combination of NavMap and OctreeNavNode Grid of navigation nodes
 *
 * @author vear
 */
public class NavGrid {

    protected static final Logger log = Logger.getLogger(NavGrid.class.getName());

    // grid of 128x128 tiles, tiles are further subdivided by 2
    // when they contain buildings, further subdividing until reaching 1 units on an extent
    // if a dim is increased, further blocks are added 
    protected Vector3f min = new Vector3f(Vector3f.MAX_POSITIVE);
    protected Vector3f max = new Vector3f(Vector3f.MAX_NEGATIVE);

    protected int lastNodeId = 0;

    protected String fileName;
    protected String bakFile;

    //public Tensor nodeCosts;

    // grid of top level nodes on x,y,z
    protected NavCell[][][] grid;
    protected int dimX, dimY, dimZ;
    protected boolean loadedGrid;


    public void checkMapRanges(Vector3f position) {
        if (position.x < min.x) {
            float newXmin = FastMath.floor(position.x / 128) * 128;
            // increase the x by given number of indices
            int newIndices = (int) (Math.abs(newXmin - min.x) / 128);
            int oldLength = grid.length;
            // copy the x array to right
            grid = Arrays.copyOf(grid, grid.length + newIndices);
            // copy the indices right
            System.arraycopy(grid, 0, grid, newIndices, oldLength);
            min.x = newXmin;
        }
        if (position.x > max.x) {
            float newXmax = FastMath.ceil(position.x / 128) * 128;
            // increase the x by given number of indices
            int newIndices = (int) (Math.abs(newXmax - max.x) / 128);
            // copy the x array to right
            grid = Arrays.copyOf(grid, grid.length + newIndices);
            max.x = newXmax;
        }
        if (position.y < min.y) {
            float newYmin = FastMath.floor(position.y / 128) * 128;
            // increase the x by given number of indices
            int newIndices = (int) Math.ceil(Math.abs(newYmin - min.y) / 128);
            int oldLength = (int) (Math.ceil(FastMath.abs(max.y - min.y) / 128));
            for (int x = 0; x < grid.length; x++) {
                if (grid[x] == null) {
                    continue;
                }
                grid[x] = Arrays.copyOf(grid[x], grid[x].length + newIndices);
                // copy the indices right
                System.arraycopy(grid[x], 0, grid[x], newIndices, oldLength);
            }
            min.y = newYmin;
        }
        if (position.y > max.y) {
            float newYmax = FastMath.ceil(position.y / 128) * 128;
            // increase the x by given number of indices
            int newIndices = (int) Math.ceil(Math.abs(newYmax - max.y) / 128);
            //int oldLength = (int) (Math.ceil(FastMath.abs(ymax - ymin)/128));
            for (int x = 0; x < grid.length; x++) {
                if (grid[x] == null) {
                    continue;
                }
                grid[x] = Arrays.copyOf(grid[x], grid[x].length + newIndices);
            }
            max.y = newYmax;
        }
        if (position.z < min.z) {
            float newZmin = FastMath.floor(position.z / 128) * 128;
            // increase the x by given number of indices
            int newIndices = (int) Math.ceil(Math.abs(newZmin - min.z) / 128);
            int oldLength = (int) (Math.ceil(FastMath.abs(max.z - min.z) / 128));
            for (NavCell[][] gridx : grid) {
                if (gridx == null) {
                    continue;
                }
                for (int y = 0; y < gridx.length; y++) {
                    if (gridx[y] == null) {
                        continue;
                    }
                    gridx[y] = Arrays.copyOf(gridx[y], gridx[y].length + newIndices);
                    // copy the indices right
                    System.arraycopy(gridx[y], 0, gridx[y], newIndices, oldLength);
                }
            }
            min.z = newZmin;
        }
        if (position.z > max.z) {
            float newZmax = FastMath.ceil(position.z / 128) * 128;
            // increase the x by given number of indices
            int newIndices = (int) Math.ceil(Math.abs(newZmax - max.z) / 128);
            //int oldLength = (int) (Math.ceil(FastMath.abs(ymax - ymin)/128));
            for (NavCell[][] gridx : grid) {
                if (gridx == null) {
                    continue;
                }
                for (int y = 0; y < gridx.length; y++) {
                    if (gridx[y] == null) {
                        continue;
                    }
                    gridx[y] = Arrays.copyOf(gridx[y], gridx[y].length + newIndices);
                }
            }
            max.z = newZmax;
        }
        dimX = (int) (FastMath.abs(max.x - min.x) / 128);
        dimY = (int) (FastMath.abs(max.y - min.y) / 128);
        dimZ = (int) (FastMath.abs(max.z - min.z) / 128);
        // generate missing cells
        fillRoots(true);
    }

    public void saveNodes() {

        File bakfile = new File(bakFile);
        // delete bak file if it exists
        if (bakfile.exists()) {
            bakfile.delete();
        }
        // copy previous file to bakfile
        File file = new File(fileName);
        if (file.exists()) {
            log.log(Level.INFO, "Backing up old navigation map to " + bakFile);
            file.renameTo(bakfile);
        }

        // save nodes data to new file
        try {
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));

            // write the grid dimensions
            min.toFile(output);
            max.toFile(output);

            output.writeInt(dimX);
            output.writeInt(dimY);
            output.writeInt(dimZ);
            
            // store all nav nodes
            FastList<NavCell> allNodes = new FastList<>();
            for (int x = 0; x < dimX; x++) {
                for (int y = 0; y < dimY; y++) {
                    for (int z = 0; z < dimZ; z++) {
                        NavCell root = grid[x][y][z];
                        if (root == null) {
                            // shouldn't happen
                            throw new JbException("Null root cell when saving");
                        }
                        // write out root nodes first
                        output.writeInt(root.nodeid);
                        root.toFile(output);
                        // add all children to be written out later
                        root.getAll(allNodes);
                    }
                }
            }
            
            // write the number of child cells
            output.writeInt(allNodes.size);
            while(allNodes.size()>0) {
                NavCell nd = allNodes.remove();
                if (nd == null) {
                    throw new JbException("Null node when saving");
                }
                output.writeInt(nd.nodeid);
                nd.toFile(output);
            }
            output.close();
            log.log(Level.INFO, "Navigation map saved to {0}", fileName);
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public void loadNodes() {
        fileName = AppContext.serverInfo.missionFilename;
        fileName = fileName.replace(".", "_");
        fileName = Config.basedir + "\\" + fileName + "_nodes";

        bakFile = fileName + "_bak.dat";
        fileName = fileName + ".dat";

        File file = new File(fileName);
        if (!file.exists()) {
            log.log(Level.WARNING, "No nav map file exists");
            // go and generate an estimate navnode mesh
            generateEstimateMap(null);
            return;
        }

        if (!file.canRead()) {
            // no file exists, exit
            return;
        }

        try {
            // open file read it line by line
            DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            min.fromFile(input);
            max.fromFile(input);
            
            dimX = input.readInt();
            dimY = input.readInt();
            dimZ = input.readInt();
            
            grid=new NavCell[dimX][dimY][dimZ];
            
            // keep track of all loadd nodes, since they might be forward referenced
            // before they are even loaded
            IntMap<NavCell> allNodes = new IntMap<>();
            for (int x = 0; x < dimX; x++) {
                //grid[x] = new NavCell[dimY][];
                for (int y = 0; y < dimY; y++) {
                    //grid[x][y] = new NavCell[dimZ];
                    for (int z = 0; z < dimZ; z++) {
                        // get the node id
                        int nodeid = input.readInt();
                        if (lastNodeId < nodeid) {
                            lastNodeId = nodeid;
                        }
                        NavCell nn = allNodes.get(nodeid);
                        if (nn == null) {
                            nn = new NavCell(nodeid);
                            allNodes.put(nodeid, nn);
                        }
                        nn.fromFile(input, allNodes);
                        // store the root
                        grid[x][y][z] = nn;
                    }
                }
            }
            
            // read number of child cells
            int numNodes= input.readInt();
            for (int i = 0; i < numNodes; i++) {
                int nodeid = input.readInt();
                if (lastNodeId < nodeid) {
                    lastNodeId = nodeid;
                }
                NavCell nn = allNodes.get(nodeid);
                if (nn == null) {
                    nn = new NavCell(nodeid);
                    allNodes.put(nodeid, nn);
                }
                nn.fromFile(input, allNodes);
            }
            input.close();
            loadedGrid = true;
            log.log(Level.INFO, "Navigation map loaded from {0}", fileName);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error loading map file {0}", fileName);
            // go and generate new nodemap
            generateEstimateMap(null);
        }
    }

    public boolean isGridNew() {
        return !loadedGrid;
    }
    
    // generate base mesh of nodes
    public void generateEstimateMap(FastList<GameObject> gameObjects) {
        log.log(Level.INFO, "Generating estimate cell map");
        // go trough all the entities
        if(gameObjects==null) {
            gameObjects = AppContext.gameObjectFactory.getObjects(null);
        }
        for (int i = 0; i < gameObjects.size; i++) {
            GameObject go = gameObjects.get(i);
            min.minLocal(go.position);
            max.maxLocal(go.position);
        }

        // round the min and max
        min.set(FastMath.floor(min.x / 128) * 128, FastMath.floor(min.y / 128) * 128, FastMath.floor(min.z / 128) * 128);
        max.set(FastMath.ceil(max.x / 128) * 128, FastMath.ceil(max.y / 128) * 128, FastMath.ceil(max.z / 128) * 128);

        dimX = (int) (FastMath.abs(max.x - min.x) / 128);
        dimY = (int) (FastMath.abs(max.y - min.y) / 128);
        dimZ = (int) (FastMath.abs(max.z - min.z) / 128);

        grid = new NavCell[dimX][dimY][dimZ];
        fillRoots(false);

        //BoundingBox areaBox = new BoundingBox();

        // go trough all entities, and subdivide the roots as needed
        FastList<NavCell> roots = new FastList<>();
        for (int i = 0; i < gameObjects.size; i++) {
            GameObject go = gameObjects.get(i);
            // skip non-building-like objects
            if (go.typeFolder != TypeMap.TypeFolder.LFP
                    && go.typeFolder != TypeMap.TypeFolder.Dynamic
                    && go.typeFolder != TypeMap.TypeFolder.Emplaced
                    && go.typeFolder != TypeMap.TypeFolder.Static) {
                continue;
            }

            roots.clear();
            this.getRoots(go.bounds, false, roots);
            for (int ri = 0; ri < roots.size; ri++) {
                NavCell root = roots.get(ri);
                // split up the root below the bounds of the object
                root.initialSplit(go.bounds);
            }
        }
        
        // go trough and generate links for all the leaf nodes
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    // generate links on leaf nodes
                    NavCell root = grid[x][y][z];
                    root.prepareLeafs();
                }
            }
        }

        log.log(Level.INFO, "Genereated {0} nodes", lastNodeId);
    }

    protected void fillRoots(boolean generateLinks) {

        //BoundingBox areaBox = new BoundingBox();
        for (int x = 0; x < dimX; x++) {
            if (grid[x] == null) {
                grid[x] = new NavCell[dimY][];
            }
            for (int y = 0; y < dimY; y++) {
                if (grid[x][y] == null) {
                    grid[x][y] = new NavCell[dimZ];
                }
                for (int z = 0; z < dimZ; z++) {
                    if (grid[x][y][z] != null) {
                        continue;
                    }
                    // create missing root cell
                    grid[x][y][z] = createCellDirect();
                    BoundingBox areaBox = grid[x][y][z].bounds;
                    areaBox.center.x = min.x + x * 128 + 64;
                    areaBox.center.z = min.z + z * 128 + 64;
                    areaBox.center.y = min.y + y * 128 + 64;
                    areaBox.extents.x = 64f;
                    areaBox.extents.y = 64f;
                    areaBox.extents.z = 64f;
                    // generate the links
                    if(generateLinks) {
                        grid[x][y][z].prepareLeafs();
                    }
                }
            }
        }
    }

    public NavCell createCellDirect() {
        // generate unique id
        lastNodeId++;
        NavCell newNode = new NavCell(lastNodeId);
        // make sure the influence weights array is big enough
        //nodeCosts.ensureCapacity(lastNodeId);

        return newNode;
    }

    protected NavCell getRoot(Vector3f position) {
        int xi = getXIndex(position.x);
        int yi = getYIndex(position.y);
        int zi = getZIndex(position.z);
        return grid[xi][yi][zi];
    }

    protected static final int getIndex(float value, float min, float max) {
        if (value > max) {
            return (int) (FastMath.abs(max - min) / 128) -1;
        }
        if(value < min)
            return 0;
        return (int) ((value - min) / 128);
    }
    
    protected final int getXIndex(float value) {
        return getIndex(value, min.x, max.x);
    }
    
    protected final int getYIndex(float value) {
        return getIndex(value, min.y, max.y);
    }

    protected final int getZIndex(float value) {
        return getIndex(value, min.z, max.z);
    }
    
    protected FastList<NavCell> getRoots(BoundingBox bb, boolean twoD, FastList<NavCell> store) {
        if (store == null) {
            store = new FastList<>();
        }
        int xStart = getXIndex(bb.center.x - bb.extents.x);
        int xEnd = getXIndex(bb.center.x + bb.extents.x);

        int yStart;
        int yEnd;
        if (!twoD) {
            yStart = getYIndex(bb.center.y - bb.extents.y);
            yEnd = getYIndex(bb.center.y + bb.extents.y);
        } else {
            yStart = 0;
            yEnd = (int) (FastMath.abs(max.y - min.y) / 128)-1;
        }

        int zStart = getZIndex(bb.center.z - bb.extents.z);
        int zEnd = getZIndex(bb.center.z + bb.extents.z);

        for (int xi = xStart; xi <= xEnd; xi++) {
            if (grid[xi] == null) {
                continue;
            }
            for (int yi = yStart; yi <= yEnd; yi++) {
                if (grid[xi][yi] == null) {
                    continue;
                }
                for (int zi = zStart; zi <= zEnd; zi++) {
                    NavCell cell = grid[xi][yi][zi];
                    if (cell == null) {
                        continue;
                    }
                    store.add(cell);
                }
            }
        }
        return store;
    }

    public FastList<NavCell> getCells2d(BoundingBox bb, FastList<NavCell> store) {
        return getCells(bb, store, true);
    }
    
    public FastList<NavCell> getCells(BoundingBox bb, FastList<NavCell> store) {
        return getCells(bb, store, false);
    }

    public FastList<NavCell> getCells(BoundingBox bb, FastList<NavCell> store, boolean twoD) {
        if(store == null)
            store = new FastList<>();
        FastList<NavCell> roots = LocalContext.getContext().NavGrid_getCells_roots;
        roots.clear();
        roots = getRoots(bb, twoD, roots);
        for (int i = 0; i < roots.size; i++) {
            NavCell root = roots.get(i);
            // get all root cells
            root.getIntersecting(bb, store, !twoD);
        }
        return store;
    }
    
    public NavCell getCell(Vector3f position) {
        NavCell root = getRoot(position);
        if(root==null)
            return null;
        return root.getLeaf(position);
    }

    /*
    public void addTraversalCounter(Vector3f position) {
        // add traversal count to a node

        // get the root
        NavCell cell = this.getRoot(position);
        if (cell == null) {
            return;
        }
        // add the stat, trickling down to a leaf node
        // subdividing the node if treshold is reached
        cell.addStat(position, 1f);
    }
*/
    
    public int getLastNodeId() {
        return this.lastNodeId;
    }
    
    public int countLeafs() {
        final int[] leafs = new int[1];
        forEachRoot((NavCell root) -> {
            root.forEachLeaf(leaf -> {
                leafs[0]++;
            });
        });
        return leafs[0];
    }
    
    public int countLinks() {
        final int[] links = new int[1];
        forEachRoot((NavCell root) -> {
            root.forEachLeaf(leaf -> {
                links[0]+=leaf.links.size;
            });
        });
        return links[0];
    }
    
    public void forEachRoot(Consumer<NavCell> f) {
        for (int x = 0; x < dimX; x++) {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    // generate links on leaf nodes
                    NavCell root = grid[x][y][z];
                    if(root==null)
                        continue;
                    f.accept(root);
                }
            }
        }
    }
    
    public void forEachRoot(BoundingBox bb, boolean twoD, Consumer<NavCell> f) {
        int xStart = getXIndex(bb.center.x - bb.extents.x);
        int xEnd = getXIndex(bb.center.x + bb.extents.x);

        int yStart;
        int yEnd;
        if (!twoD) {
            yStart = getYIndex(bb.center.y - bb.extents.y);
            yEnd = getYIndex(bb.center.y + bb.extents.y);
        } else {
            yStart = 0;
            yEnd = (int) (FastMath.abs(max.y - min.y) / 128)-1;
        }

        int zStart = getZIndex(bb.center.z - bb.extents.z);
        int zEnd = getZIndex(bb.center.z + bb.extents.z);

        for (int xi = xStart; xi <= xEnd; xi++) {
            if (grid[xi] == null) {
                continue;
            }
            for (int yi = yStart; yi <= yEnd; yi++) {
                if (grid[xi][yi] == null) {
                    continue;
                }
                for (int zi = zStart; zi <= zEnd; zi++) {
                    NavCell cell = grid[xi][yi][zi];
                    if (cell == null) {
                        continue;
                    }
                    f.accept(cell);
                }
            }
        }
    }

    public void forEachIntersectingLeaf(BoundingBox bb, boolean twoD,
                    Consumer<NavCell> f) {
        forEachRoot(bb, twoD, root->root.forEachIntersectingLeaf(bb, twoD, f));
    }
    
    public void forEachLeaf(Consumer<NavCell> f) {
        forEachRoot(root->root.forEachLeaf(f));
    }
    

}
