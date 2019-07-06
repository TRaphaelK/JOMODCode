
/*
 * MyXML.java
 *
 * Created on 2005. augusztus 14., 12:10
 */
package xml;
import com.jme.util.LoggingSystem;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vear
 */
public class MyXML {
    
    private static Logger _log = LoggingSystem.getLogger();
    /** Creates a new instance of MyXML */
    public MyXML() { }
    
    public static Element fromXML(String filename, boolean dozip) {
        Element npj=null;
        try {
            // feldolgozzuk az XML-t
            XMLReader xr=readXML(filename, dozip);
            // skip the header
            String se=xr.skipRootElement();
            npj=new Element(se);
            npj.fromXML(xr);
            xr.close();
        } catch(Exception e) {
            _log.log(Level.SEVERE, "Cannot load XML file "+filename, e);
        }
        return npj;
    }
    
    public static Element fromXML(Reader r) {
        Element npj=null;

        try {
            XMLReader xr=new XMLReader(r);
            String se;
            se = xr.skipRootElement();
            npj=new Element(se);
            npj.fromXML(xr);
            xr.close();
        } catch (IOException ex) {
            _log.log(Level.SEVERE, "Cannot read XML stream", ex);
        }
        return npj;
    }
    
    public static XMLReader readXML(String filename, boolean dozip) throws FileNotFoundException, IOException {
        BufferedReader rdr;
        if(dozip) {
           rdr=new BufferedReader(new InputStreamReader(new java.util.zip.GZIPInputStream(new FileInputStream(filename))));
        } else {
            rdr=new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        }
        XMLReader xr=new XMLReader(rdr);
        return xr;
    }
    
    public static void toXML(String filename, Element doc, boolean dozip) {
        try {
            OutputStream out;
            if(dozip) {
                out = new BufferedOutputStream(new java.util.zip.GZIPOutputStream( new FileOutputStream(filename)));
            } else {
                 out = new BufferedOutputStream(new FileOutputStream(filename));
            }
            
            // print out the header
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
            WrappedByteChannel bc=new WrappedByteChannel(out);
            doc.toXML(bc);
            out.close();
        } catch(Exception e) {
            _log.log(Level.SEVERE, "Cannot save XML file "+filename, e);
        }
    }
}
