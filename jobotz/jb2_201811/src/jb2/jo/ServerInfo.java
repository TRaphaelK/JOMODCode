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
package jb2.jo;

import java.util.logging.Logger;

/**
 *
 * @author vear
 */
public class ServerInfo {
    
    protected static final Logger log = Logger.getLogger(ServerInfo.class.getName());
    
    // pointer to server info if server is running
    protected JOPointer serverInfoBase;
    
    public String serverName;
    public String missionName;
    public int gameType;
    public String customText;
    public String missionFilename;
    public String mapName;

    public JOAdd.ServerState serverState;
    public boolean dedicated;
    
    public void refresh() {
            int offset=0;
            boolean cont=true;
            serverInfoBase=JOAdd.Adress.ServerinfoBasePtr.value.getPointer(0, serverInfoBase);
            if(serverInfoBase.get()==0) {
                // no server running
                cont = false;
                serverName = "";
                missionName = "";
                gameType = 0;
                customText = "";
                missionFilename = "";
                mapName = "";
            }
            while(cont) {
                String varName=JOGame.readStringZ(serverInfoBase, offset, 30);
                offset+=varName.length()+1;
                int inflen=JOGame.readInt(serverInfoBase, offset);
                offset+=4;
                if(varName.equals("GAMETYPE")) {
                    gameType=JOGame.readInt(serverInfoBase, offset);
                    offset+=4;
                } else {
                    String varVal = JOGame.readStringZ(serverInfoBase, offset, inflen);
                    offset+=varVal.length()+1;

                    if(varName.equals("SERVERNAME")) {
                        serverName=varVal;
                    } else if(varName.equals("MISSIONNAME")) {
                        missionName=varVal;
                    } else if(varName.equals("CUSTOMTEXT")) {
                        customText=varVal;
                    } else if(varName.equals("MISSIONFILENAME")) {
                        missionFilename=varVal;
                        // split the extension from the mapname
                        mapName=missionFilename.substring(0, missionFilename.length()-4);
                    } else {
                        // unknown key, exit
                        cont=false;
                    }
                }
            }
            // read server state
            int state = JOAdd.Adress.ServerinfoServerstate.value.getInt(0);
            switch (state) {
                //case 0:
                //    serverState = JOAdd.ServerState.NOP;
                //    break;
                case 1:
                    serverState = JOAdd.ServerState.LOAD;
                    break;
                case 2:
                    serverState = JOAdd.ServerState.START;
                    break;
                case 3:
                    serverState = JOAdd.ServerState.RUN;
                    break;
                case 4:
                    serverState = JOAdd.ServerState.END;
                    break;
                default:
                    serverState = JOAdd.ServerState.NOP;
            }
            // read dedicated flag
            dedicated = JOAdd.Adress.ServerinfoDedicated.value.getInt(0) == 1;
    }
}
