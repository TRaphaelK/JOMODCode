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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import jb2.AppContext;
import jb2.Config;
import jb2.ent.BotEntity;
import jb2.ent.Entity;
import jb2.ent.Waypoint;
import jb2.jo.GameObject;
import jb2.jo.TypeMap;
import jb2.math.FastMath;
import jb2.math.Vector3f;
import jb2.util.FastList;
import jb2.util.IntMap;
import jb2.util.JbException;

/**
 *
 * @author vear
 */
public class MapRenderer {

    protected static final Logger log = Logger.getLogger(MapRenderer.class.getName());

    protected BufferedImage image;
    protected Graphics2D graph;
    protected FastList<GameObject> gameObjects;
    protected static final float scale = 3.0f;
    protected Vector3f min = new Vector3f(Vector3f.MAX_POSITIVE);
    protected Vector3f max = new Vector3f(Vector3f.MAX_NEGATIVE);
    protected int dimX, dimZ;
    protected IntMap<NavCell> cells;

    protected long lastrun = 0;

    protected Font textFont;

    public MapRenderer() {
        textFont = new Font("SansSerif", Font.PLAIN, 12);
    }

    public void snapshot() {
        // save an image every 60 seconds
        long ts = AppContext.gameMatch.timestamp;
        if (lastrun != 0 && ts - lastrun < 60000) {
            return;
        }
        lastrun = ts;
        log.log(Level.INFO, "Starting snapshot thread");
        new Thread(new Runnable() {
            @Override
            public void run() {
                startFrame();
                renderCells();
                renderEnts();
                saveFrame();
            }
        }).start();
    }

    public void startFrame() {
        // get height and width from map
        gameObjects = AppContext.gameObjectFactory.getObjects(null);
        for (int i = 0; i < gameObjects.size; i++) {
            GameObject go = gameObjects.get(i);
            min.minLocal(go.position);
            max.maxLocal(go.position);
        }

        // retrieve the grid
        cells = new IntMap<>();
        AppContext.navGrid.forEachLeaf(cell -> {
            min.minLocal(cell.bounds);
            max.maxLocal(cell.bounds);
            // check if the cell intersects the heightmap
            if (cell.isOnLandLevel()) {
                cells.put(cell.nodeid, cell);
            }
        });

        // round the min and max
        min.set(FastMath.floor(min.x), FastMath.floor(min.y), FastMath.floor(min.z));
        max.set(FastMath.ceil(max.x), FastMath.ceil(max.y), FastMath.ceil(max.z));

        dimX = (int) (FastMath.abs(max.x - min.x) * scale);
        dimZ = (int) (FastMath.abs(max.z - min.z) * scale);

        image = new BufferedImage(dimZ, dimX, BufferedImage.TYPE_INT_ARGB);
        graph = image.createGraphics();
        graph.setBackground(Color.black);
        graph.clearRect(0, 0, dimZ, dimX);
        graph.setFont(textFont);

    }

    public void renderCells() {
        graph.setColor(Color.yellow);
        // render the map to a PNG image
        cells.forEachValue(cell -> {
            drawCell(cell);
        });
    }

    public void renderEnts() {
        gameObjects.forEachEntry(gameObject -> {
            drawEnt(gameObject);
        });
    }

    protected void drawCell(NavCell cell) {
        int z = (int) ((cell.bounds.center.z - cell.bounds.extents.z - min.z) * scale);
        int x = (int) ((cell.bounds.center.x - cell.bounds.extents.x - min.x) * scale);
        int width = (int) ((cell.bounds.extents.z * 2f) * scale);
        int height = (int) ((cell.bounds.extents.x * 2f) * scale);

        graph.drawRect(z, x, width, height);

        if (cell.bounds.extents.z > 12f / scale) {
            // write the cell id
            String nodeid = String.valueOf(cell.nodeid);
            float tz = z;
            float tx = (x + height / 2f) - 4f;
            //graph.scale(0.2f, 0.2f);
            graph.drawString(nodeid, tz, tx);
            //graph.scale(5f, 5f);
        }
    }

    protected void drawLine(Vector3f from, Vector3f to) {
        int fz = (int) ((from.z - min.z) * scale);
        int fx = (int) ((from.x - min.x) * scale);

        int tz = (int) ((to.z - min.z) * scale);
        int tx = (int) ((to.x - min.x) * scale);

        graph.drawLine(fz, fx, tz, tx);
    }

    protected void drawEnt(GameObject gameObject) {
        // draw bots with their color, if scale is >10 then write their number
        if (!(gameObject instanceof Entity)) {
            return;
        }
        Entity ent = (Entity) gameObject;
        if (ent.dead) {
            return;
        }
        int z, x, width, height;

        if (ent instanceof Waypoint) {
            graph.setColor(Color.gray);
            z = (int) ((ent.bounds.center.z - min.z) * scale);
            x = (int) ((ent.bounds.center.x - min.x) * scale);
            graph.drawRect(z, x, 1, 1);
            return;
        }

        // draw LFP-s, if scale> 10 then write their number
        z = (int) ((ent.bounds.center.z - ent.bounds.extents.z - min.z) * scale);
        x = (int) ((ent.bounds.center.x - ent.bounds.extents.x - min.x) * scale);
        width = (int) ((ent.bounds.extents.z * 2f) * scale);
        height = (int) ((ent.bounds.extents.x * 2f) * scale);

        if (ent.team == 1) {
            graph.setColor(Color.blue);
        } else if (ent.team == 2) {
            graph.setColor(Color.red);
        } else {
            graph.setColor(Color.white);
        }

        if (ent.typeFolder == TypeMap.TypeFolder.AI
                || ent.typeFolder == TypeMap.TypeFolder.Player) {
            graph.drawOval(z, x, width, height);
        } else {
            graph.drawRect(z, x, width, height);
        }

        // if bot has route, then draw lines on the route
        if (!(ent instanceof BotEntity)) {
            return;
        }
        BotEntity bot = (BotEntity) ent;
        // draw current move target with arrow
        Vector3f first = new Vector3f();
        first.set(bot.position);

        if (bot.moveTarget != 0) {
            // bot is moving towards a marker
            drawLine(first, bot.moveMarker.position);
            first.set(bot.moveMarker.position);
        }
        if (bot.navRoute == null) {
            return;
        }
        if (!bot.navRoute.ready) {
            return;
        }

        //first.set(bot.moveMarker.position);
        /*
        graph.setColor(Color.green);
        drawLine(bot.position, first);
        if(ent.team==1) {
            graph.setColor(Color.blue);
        } else if(ent.team==2) {
            graph.setColor(Color.red);
        } else {
            graph.setColor(Color.white);
        }
         */
        for (int i = bot.navRoute.routeStack.size - 2; i >= 0; i--) {
            NavCell rc = bot.navRoute.routeStack.get(i);
            // draw to center of nodes
            drawLine(first, rc.bounds.center);
            first.set(rc.bounds.center);
        }
    }

    public void render(NavRoute route) {
        // render a route to PNG
        // render all cells, but render cells in route with green

    }

    public void saveFrame() {
        // construct file name
        String fileName = AppContext.serverInfo.missionFilename;
        fileName = fileName.replace(".", "_");
        fileName = Config.basedir + "\\" + fileName + "_map.png";

        try {
            ImageIO.write(image, "PNG", new File(fileName));
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}
