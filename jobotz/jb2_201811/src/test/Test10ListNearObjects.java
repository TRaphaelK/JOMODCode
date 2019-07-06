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

import java.util.logging.Logger;
import jb2.AppContext;
import jb2.ent.Entity;
import jb2.jo.GameObject;
import jb2.jo.GameObjectFactory;
import jb2.jo.JOAdd;
import jb2.jo.JOGame;
import jb2.jo.ServerInfo;
import jb2.math.BoundingBox;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class Test10ListNearObjects {

    protected static Logger log = Logger.getLogger(Test10ListNearObjects.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        AppContext.init();

        // Connect to JO
        if (JOGame.openProcessByWindowName() == false) {
            log.severe("Failed to connect");
            System.exit(1);
        }

        ServerInfo si = AppContext.serverInfo;
        si.refresh();

        if (si.serverState != JOAdd.ServerState.RUN) {
            log.severe("No map running");
            System.exit(1);
        }
        // refersh all map entities
        GameObjectFactory gof = AppContext.gameObjectFactory;
        gof.baseRefresh();
        // do a second refresh, which will construct the morton map
        gof.refreshEntities(true);
        //AppContext.navMap.build();

        // get the first player
        GameObject player = gof.getPlayers().get(0);
        // construct a 20 size bb around the player
        BoundingBox bb = new BoundingBox(player.position, 20, 3, 20);
        // get all object inside the bb, the player should be in there too

        // go trough all the classes and output objects
        long querytime = System.currentTimeMillis();
        FastList<Entity> objects = gof.getContained(bb, null);
        querytime = System.currentTimeMillis() - querytime;
        log.info("Query time " + querytime);
        if (objects != null) {

            for (int objn = 0; objn < objects.size(); objn++) {
                GameObject go = objects.get(objn);
                System.out.println(go.toString());
                /*
                if(go.SSN == 1011) {
                    go.position.z = 332;
                    go.writePosition();
                }
                 */
 /*
                if(go.SSN == 1004) {
                    go.team = 2;
                    go.writeTeam();
                }
                 */

            }
        }
        JOGame.closeProcess();
    }

}
