/*
 * 
 * Vear 2017-2018  * 
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import jb2.AppContext;
import jb2.Config;
import jb2.jo.GameObject;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.JbException;

/**
 *
 * @author vear
 */
public class HeightMap {
    protected static final Logger log = Logger.getLogger(HeightMap.class.getName());

    // grid of 128x128 tiles, tiles are further subdivided by 2
    // when they contain buildings, further subdividing until reaching 1 units on an extent
    // if a dim is increased, further blocks are added 
    protected Vector3f min = new Vector3f(Vector3f.MAX_POSITIVE);
    protected Vector3f max = new Vector3f(Vector3f.MAX_NEGATIVE);

    protected String fileName;
    protected String bakFile;

    //public Tensor nodeCosts;

    // grid of top level nodes on x,y,z
    protected byte[] grid;
    
    protected int dimX, dimZ;
    
    protected void ensureMapDimension(Vector3f position) {
        float newXmin = min.x;
        int newXIndices = 0;
        if (position.x < min.x) {
            newXmin = FastMath.floor(position.x);
            newXIndices = (int) (Math.abs(newXmin - min.x));
        }
        float newXmax = max.x;
        if (position.x > max.x) {
            newXmax = FastMath.ceil(position.x);
        }
        float newYmin = min.y;
        if (position.y < min.y) {
            newYmin = FastMath.floor(position.y);
        }
        float newYmax = max.y;
        if (position.y > max.y) {
            newYmax = FastMath.ceil(position.y);
        }
        float newZmin = min.z;
        int newZIndices = 0;
        if (position.z < min.z) {
            newZmin = FastMath.floor(position.z);
            newZIndices = (int) (Math.abs(newZmin - min.z));
        }
        float newZmax = max.z;
        if (position.z > max.z) {
            newZmax = FastMath.ceil(position.z);
        }
        int newDimX = (int) (FastMath.abs(newXmax - newXmin));
        int newDimZ = (int) (FastMath.abs(newZmax - newZmin));
        // generate missing cells
        if(dimX==newDimX && dimZ==newDimZ) {
            return;
        }
        // create new array
        byte[] newGrid = new byte[newDimX*newDimZ];
        for(int x=0; x<dimX; x++) {
            // source x
            int destX = x + newXIndices;
            // copy area adjusted with new dimensions
            System.arraycopy(grid, x*dimZ, newGrid, destX*newDimZ+newZIndices, dimZ);
        }
        // set values
        min.set(newXmin, newYmin, newZmin);
        max.set(newXmax, newYmax, newZmax);
        dimX = newDimX;
        dimZ = newDimZ;
        // replace the grid
        grid = newGrid;
        log.log(Level.INFO, "Heightmap new dimension {0} by {1}", new Object[]{dimX, dimZ});
        
    }
    
    public void create(FastList<GameObject> gameObjects) {

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
        min.set(FastMath.floor(min.x), FastMath.floor(min.y), FastMath.floor(min.z));
        max.set(FastMath.ceil(max.x), FastMath.ceil(max.y), FastMath.ceil(max.z));

        dimX = (int) (FastMath.abs(max.x - min.x));
        dimZ = (int) (FastMath.abs(max.z - min.z));

        grid = new byte[dimX*dimZ];
        log.log(Level.INFO, "Heightmap created with new dimension {0} by {1}", new Object[]{dimX, dimZ});
    }
    
    public void record(Vector3f position) {
        ensureMapDimension(position);
        // LERP betweeen the adjacent points
        int xpos = (int) Math.round(position.x-min.x);        
        int zpos = (int) Math.round(position.z-min.z);
        
        // intepolate between old and new values
        // and fill the height value around the position
        int index = xpos*dimZ + zpos;
        if(grid[index]==0 
                || grid[index]>position.y) {
            grid[index] = (byte) position.y;
        }
        /*
        xpos = (int) Math.floor(position.x-min.x);        
        zpos = (int) Math.floor(position.z-min.z);
        index = xpos*dimZ + zpos;
        if(grid[index]==0) {
            grid[index] = (byte) position.y;
        }
        index = xpos*dimZ + (zpos+1);
        if(grid[index]==0) {
            grid[index] = (byte) position.y;
        }
        index = (xpos+1)*dimZ + (zpos+1);
        if(grid[index]==0) {
            grid[index] = (byte) position.y;
        }
          */
    }

    public float getHeight(Vector3f position) {
        if(position.x < min.x
                || position.x > max.x
                || position.z < min.z
                || position.z > max.z) {
            // out of bounds
            return 0;
        }
        // interpolate between surrounding values
        // LERP betweeen the adjacent points
        int xpos = (int) FastMath.floor(position.x-min.x);
        float xratio = FastMath.frac(position.x);
        
        int zpos = (int) FastMath.floor(position.z-min.z);
        float zratio = FastMath.frac(position.z);
        
        int index = xpos*dimZ + zpos;
        if(index<0 || index>grid.length) {
            return 0;
        }
        float val1 = grid[index];
        index = xpos*dimZ + zpos+1;
        float val2 = grid[index];
        index = (xpos+1)*dimZ + zpos;
        if(index<0 || index>grid.length) {
            return 0;
        }        
        float val3 = grid[index];
        index = (xpos+1)*dimZ + zpos+1;
        float val4 = grid[index];

        if(val1!=0 && val2!=0) {
            val1 = val1*(1-zratio) + val2*(zratio);
        } else if(val2!=0) {
            val1=val2;
        }
        if(val3!=0 && val4!=0) {
            val3 = val3*(1-zratio) + val4*(zratio);
        } else if(val4!=0) {
            val3=val4;
        }

        if(val1!=0 && val3!=0) {
            val1 = val1*(1-xratio) + val3*(xratio);
        } else if(val3!=0) {
            val1=val3;
        }
        
        return val1;
    }
    
    public void getAreaMinMax(BoundingBox bbox, float[] minmax) {
        if(minmax == null)
            minmax = new float[2];
        minmax[0] = 0;
        minmax[1] = 0;
        float x1 = bbox.center.x - bbox.extents.x;
        float x2 = bbox.center.x + bbox.extents.x;
        float z1 = bbox.center.z - bbox.extents.z;
        float z2 = bbox.center.z + bbox.extents.z;
        float y1 = bbox.center.y - bbox.extents.y;
        float y2 = bbox.center.y + bbox.extents.y;

        if(x1<min.x)
            x1=min.x;
        if(x2>max.x)
            x2=max.x;
        if(z1<min.z)
            z1=min.z;
        if(z2>max.z)
            z2=max.z;

        if(x1>max.x || x1>x2 || x2<min.x)
            return;
        if(z1>max.z || z1>z2 || z2<min.z)
            return;
        int xstart = (int) FastMath.floor(x1-min.x);
        int xend = (int) FastMath.floor(x2-min.x);
        int zstart = (int) FastMath.floor(z1-min.z);
        int zend = (int) FastMath.floor(z2-min.z);
        for(int xpos=xstart; xpos <= xend; xpos++) {
            for(int zpos=zstart; zpos <= zend; zpos++) {
                int index = xpos*dimZ + zpos;
                float val = grid[index];
                if(val==0 || val<y1 || val>y2)
                    continue;
                
                if(minmax[0]==0 || minmax[0]>val)
                    minmax[0] = val;
                if(minmax[1]==0 || minmax[1]<val)
                    minmax[1] = val;
                
            }
        }
    }
    

    public void saveHeightMap() {

        File bakfile = new File(bakFile);
        // delete bak file if it exists
        if (bakfile.exists()) {
            bakfile.delete();
        }
        // copy previous file to bakfile
        File file = new File(fileName);
        if (file.exists()) {
            log.log(Level.INFO, "Backing up old height map to {0}", bakFile);
            file.renameTo(bakfile);
        }
        
        // generate new filename with dimensions
        

        // save nodes data to new file
        try {
            DataOutputStream output = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(fileName)));
            output.writeInt((int) min.x);
            output.writeInt((int) min.y);
            output.writeInt((int) min.z);

            output.writeInt((int) max.x);
            output.writeInt((int) max.y);
            output.writeInt((int) max.z);

            output.writeInt((int) dimX);
            output.writeInt((int) dimZ);

            output.write(grid);
            output.close();
            log.log(Level.INFO, "Navigation map saved to {0}", fileName);
            log.log(Level.INFO, "Header len {0} bytes", 8*4);
            log.log(Level.INFO, "Dimesion {0} by {1}", new Object[]{dimX, dimZ});
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    protected void generateFilenames() {
        fileName = AppContext.serverInfo.missionFilename;
        fileName = fileName.replace(".", "_");
        fileName = Config.basedir + "\\" + fileName + "_heightmap";

        bakFile = fileName + "_bak.gz";
        fileName = fileName + ".gz";
    }
    
    public void loadHeightMap() {
        
        generateFilenames();
        
        File file = new File(fileName);
        if (!file.exists()) {
            log.log(Level.WARNING, "No heightmap file exists");
            // create an empty heightmap
            create(null);
            return;
        }

        if (!file.canRead()) {
            // no file exists, exit
            return;
        }

        try {
            // open file read it line by line
            DataInputStream input = new DataInputStream(new GZIPInputStream(new FileInputStream(fileName)));
            
            min.x = input.readInt();
            min.y = input.readInt();
            min.z = input.readInt();

            max.x = input.readInt();
            max.y = input.readInt();
            max.z = input.readInt();
            
            dimX = input.readInt();
            dimZ = input.readInt();
            
            grid = new byte[dimX*dimZ];
            input.readFully(grid);
            
            input.close();
            log.log(Level.INFO, "Heightmap loaded from {0}", fileName);
            // if nodemap is new, but heightmap was loaded, then update the node heights from the heightmap
            if(AppContext.navGrid.isGridNew()) {
                log.log(Level.INFO, "Updating height in cells");
                AppContext.navGrid.forEachLeaf(f->f.recalcMinMaxHeight());
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error loading heightmap file {0}", fileName);
            create(null);
        }
    }

}
