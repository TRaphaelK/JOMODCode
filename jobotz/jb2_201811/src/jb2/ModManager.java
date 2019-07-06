/*
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
package jb2;

import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.jo.JOAdd;
import jb2.jo.JOGame;
import jb2.jo.ServerInfo;

/**
 * Main program, tracks opening of JO and states of operation
 * @author vear
 */
public class ModManager {

    protected static final Logger log = Logger.getLogger(ModManager.class.getSimpleName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        AppContext.init();
        //log.setLevel(Config.logLevel);
        //TODO: start web-interface
        // start monitoring (http thread)        
        log.log(Level.INFO, "JOBotz 2 v0.1 startup");
        // check if Jo is running
        // check serverinfo for running map
        // if bots are enabled for current map, take control
        mainLoop();
    }
    
    public static boolean exit = false;
    protected static boolean pollingstart = false;
    
    protected static void mainLoop() {
        
        while(!exit) {
            if(JOGame.openProcessByWindowName()) {
                // process succesfully opened
                log.info("JO process opened");
                try {
                    while(!exit) {
                        // check if server is running
                        ServerInfo si = AppContext.serverInfo;
                        si.refresh();
                        if(si.serverState==JOAdd.ServerState.RUN) {
                            // TODO: start the match
                                AppContext.gameMatch = new GameMatch();
                                AppContext.gameMatch.runMatch();
                                pollingstart=false;
                        }
                        if(!exit) {
                            // wait 
                            if(!pollingstart) {
                                log.info("Polling for server startup every 3 sec");
                                pollingstart=true;
                            }
                            pause();
                        }
                    }
                    
                } catch(Exception e) {
                    log.log(Level.SEVERE, "Exception cought in ModManage", e);
                } catch(java.lang.Error er) {
                    log.log(Level.SEVERE, "Error cought in ModManage", er);
                }
                // close the process in the end
                JOGame.closeProcess();
                log.info("JO process closed");
            }
            if(!exit) {
                log.info("Waiting 3 sec for JO process");
                // wait couple of
                pause();
            }
            
        }
    }
    
    protected static void pause() {
        AppContext.flushLogFile();
        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException ex) {
            
        }
    }
    
}
