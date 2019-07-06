/*
 * NPJFile.java
 *
 * Created on 2005. szeptember 10., 9:46
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer.map.loaders;

import com.jme.util.LoggingSystem;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nu.xom.Builder;
import nu.xom.Document;
import xml.Element;
import xml.MyXML;


/**
 *
 * @author vear
 */
public class NPJFile {
          
    private Logger _log = LoggingSystem.getLogger();
    
    /** Creates a new instance of NPJFile */
    public NPJFile() {
    }
    
    private StringReader getfiltered(FileReader flr) throws java.io.IOException {
        // nile places the & character in the file, which is invalid by itself
        // so filter it out
        StringWriter wrtr=new StringWriter();
        int c;
        while((c=flr.read())!=-1) {
            if(c!='&') {
                wrtr.write(c);
            }
        }
        
        BufferedReader rdr=new BufferedReader(new StringReader(wrtr.toString()));
        wrtr=new StringWriter();
        PrintWriter prw = new PrintWriter(wrtr);
        // filter out search/replace lines with bad XML tags
        // <CORNER2_X>.*</CORNER1_X>
        String rdln;
        Pattern p1 = Pattern.compile("[^<]*<CORNER2_X>([^<]*)</CORNER1_X>");
        Pattern p2 = Pattern.compile("[^<]*<CORNER2_Y>([^<]*)</CORNER1_Y>");
        Pattern p3 = Pattern.compile("[^<]*<CORNER2_Z>([^<]*)</CORNER1_Z>");
        while((rdln=rdr.readLine())!=null) {
            Matcher m = p1.matcher(rdln);
            if(m.matches()&&m.groupCount()==1) {
                // found that offending line
                rdln = "<CORNER2_X>" + m.group(1) + "</CORNER2_X>";
            } else {
                m = p2.matcher(rdln);
                if(m.matches()&&m.groupCount()==1) {
                    // found that offending line
                    rdln = "<CORNER2_Y>" + m.group(1) + "</CORNER2_Y>";
                } else {
                m = p3.matcher(rdln);
                    if(m.matches()&&m.groupCount()==1) {
                        // found that offending line
                        rdln = "<CORNER2_Z>" + m.group(1) + "</CORNER2_Z>";
                    } else {
                        // other workarounds?
                    }
                }
            }
            prw.println(rdln);
        }
        prw.close();
        StringReader rds = new StringReader(wrtr.toString());
        return rds;
    }
    
    public Element fromXML(String filename) {
        try {
            return MyXML.fromXML(getfiltered(new FileReader(filename)));
        } catch (Exception ex) {
            LoggingSystem.getLogger().log(Level.WARNING, "Cannot load NPJ file "+filename,ex);
        }
        return null;
    }
    
    public void toXML(Element npje, String filename) {
        MyXML.toXML(filename, npje, false);
    }

}
