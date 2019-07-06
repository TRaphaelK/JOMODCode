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
import jb2.jo.GameObject;
import jb2.jo.GameObjectFactory;
import jb2.jo.JOAdd;
import jb2.jo.JOGame;
import jb2.jo.ServerInfo;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class Test01ListObjects {

    protected static Logger log = Logger.getLogger(Test01ListObjects.class.getName());
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        
        // Connect to JO
        if(JOGame.openProcessByWindowName() == false) {
            log.severe("Failed to connect");
            System.exit(1);
        }
        
        AppContext.init();
        
        ServerInfo si = AppContext.serverInfo;
        si.refresh();
        
        if (si.serverState != JOAdd.ServerState.RUN) {
            log.severe("No map running");
            System.exit(1);
        }
        // refersh all map entities
        GameObjectFactory gof = AppContext.gameObjectFactory;
        gof.baseRefresh();
        // go trough all the classes and output objects
        
            FastList<GameObject> objects = gof.getObjects(null);
            if(objects!=null) {
                
            for(int objn=0; objn< objects.size(); objn++) {
                GameObject go = objects.get(objn);
                /*
                if(go.position.x<2000 
                        && go.position.x > -2000
                        && go.position.z > -500
                        && go.position.z < 2500)
                    continue;
                */
                if(!go.isBot() && !go.isPlayer())
                    continue;
                //if(go.position.x < -1000)
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
