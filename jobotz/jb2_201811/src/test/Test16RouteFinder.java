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
package test;

import java.util.Random;
import jb2.AppContext;
import jb2.ent.BotEntity;
import jb2.jo.GameObject;
import jb2.jo.TypeMap;
import jb2.map.NavCell;
import jb2.map.NavGrid;
import jb2.map.NavLink;
import jb2.map.NavRoute;
import jb2.map.RouteFinder;
import jb2.math.BoundingBox;
import jb2.util.FastList;
import jb2.util.LocalContext;


/**
 *
 * @author vear
 */
public class Test16RouteFinder {
    public static void main(String[] args) {
        LocalContext.getContext();
        AppContext.init();
        
        Random r = new Random(12345678);
        
        NavGrid grid = AppContext.navGrid = new NavGrid();
        AppContext.routeFinder = new RouteFinder();
        
        FastList<GameObject> gameObjects = new FastList<>();
                
        // create 10000 random objects
        int numObjects = 2000;
        float areaDim = 1000;
        float linkDim = 50;
        
        BoundingBox nearArea = new BoundingBox();
        nearArea.extents.set(linkDim, linkDim, linkDim);
        
        // counter for ignored nodes
        float areaStart = -areaDim/2f;
        
        for(int i=0; i<numObjects; i++) {
            GameObject go = new GameObject(i+1);
            
            go.position.set(areaStart+r.nextFloat()*areaDim, r.nextFloat()*30, areaStart+r.nextFloat()*areaDim );
            go.bounds.extents.set(1f, 1f, 1f);
            go.bounds.center.set(go.position);
            go.typeFolder = TypeMap.TypeFolder.Static;
            
            // add to our list
            gameObjects.add(go);
        }

        // generate the map
        grid.generateEstimateMap(gameObjects);
        // get the number of nodes in the grid
        
        
        System.out.println("Number of objects: "+gameObjects.size);
        System.out.println("Number of nodes: "+grid.getLastNodeId());
        System.out.println("Number of leafs "+grid.countLeafs());
        System.out.println("Number of created links "+grid.countLinks());
        
        BotEntity dummyBot = new BotEntity(0);
        dummyBot.team = 1;
        
        for(int routeNo=0; routeNo<10; routeNo++) {
            // create a route
            NavRoute newRoute = new NavRoute();
            GameObject start = gameObjects.get(r.nextInt(gameObjects.size));
            newRoute.start = grid.getCell(start.position);
            newRoute.bot = dummyBot;
            newRoute.targetEntity = null;
            GameObject end = gameObjects.get(r.nextInt(gameObjects.size));
            newRoute.targetNode = grid.getCell(end.position);
            
            //newRoute.targetArea = endNode.bounds;

            AppContext.routeFinder.findRouteDirect(newRoute);

            System.out.println("Start "+newRoute.start.nodeid);
            System.out.println("End "+newRoute.targetNode.nodeid);
            if(newRoute.unreachable) {
                System.out.println("Route unreachable");
                continue;
            }
            if(!newRoute.ready) {
                System.out.println("Route not ready");
                continue;
            }
            // check if node links contains the target
            // gather the distances
            NavCell current = newRoute.start;
            float length = 0;
            for(int i=newRoute.routeStack.size-1; i>=0; i--) {
                NavCell nn = newRoute.routeStack.get(i);
                NavLink link = current.getLink(nn);
                if(link == null) {
                    System.out.println("No link from "+current.nodeid+" to "+nn.nodeid);
                } else {
                    System.out.println("Visit node "+nn.nodeid);
                }
                length+=nn.bounds.center.distance(current.bounds.center);
                current = nn;
            }
            System.out.println("Route length "+length);
        }
    }
}
