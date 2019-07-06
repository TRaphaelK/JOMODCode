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
package jb2.ent;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import jb2.AppContext;
import jb2.Config;

/**
 * Responsible for saving/loading the state events file
 * @author vear
 */
public class StateFile {
    
    protected static Logger log = Logger.getLogger(StateFile.class.getName());
    protected Writer output;

    // generated file name: map name, date, time
    public StateFile() {
    }
    
    public void init() {
        String filename = AppContext.serverInfo.missionFilename;
        filename = filename.replace(".", "_");
        long ts = System.currentTimeMillis();
        filename = Config.basedir + "\\evt\\" + filename + "_" + Long.toHexString(ts) + "_evt.gz";
        try {
            output = new OutputStreamWriter(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename))), "UTF-8");
            log.info("Tracing events to "+filename);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Failed starting the event log", ex);
        }
    }
    
    public void addEvent(TrackState state) {
        if(output==null) return;
        
        long time = state.timestamp - AppContext.gameMatch.starttime;
        StringBuilder sb = new StringBuilder();
        sb.append(time);
        sb.append(";");
        sb.append(state.type);
        sb.append(";");
        sb.append(state.SSN);
        sb.append(";");
        sb.append(state.typeId);
        sb.append(";");
        sb.append(state.team);
        sb.append(";");
        sb.append(state.lfpGroup);
        sb.append(";");
        sb.append(state.position.x);
        sb.append(";");
        sb.append(state.position.y);
        sb.append(";");
        sb.append(state.position.z);
        sb.append(";");
        sb.append(state.dead?0:1);
        sb.append(";");
        sb.append(state.killer_SSN);
        sb.append(";");
        sb.append(state.attachedTo_SSN);
        sb.append(";");
        sb.append(state.ftheta);
        sb.append("\n");
        try {
            output.append(sb);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
    
    public void close() {
        try {
            output.flush();
            output.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        output = null;
    }
}
