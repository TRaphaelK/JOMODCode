/*
 * XMLUtils.java
 *
 * Created on 2006. január 23., 18:56
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer;

import com.jme.util.LoggingSystem;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;

/**
 *
 * @author vear
 */
public class XMLUtils {
    
    private static Logger _log = LoggingSystem.getLogger();
    
    /** Creates a new instance of XMLUtils */
    public XMLUtils() {
    }
    
    public static boolean saveXML(String filename, Element e)
    {
        try {
            StreamingSerializerFactory ssf=new StreamingSerializerFactory();
            StreamingSerializer ser=null;
            OutputStream os=null;
            // plain XML
            os=new BufferedOutputStream(new FileOutputStream(filename));
            ser=ssf.createXMLSerializer(os, "UTF-8");
            ser.writeXMLDeclaration();
            ser.write(e);
            ser.flush();
            ser.writeEndDocument();
            os.close();
            return true;
        } catch(Exception ex) {
            _log.log(Level.WARNING, "Cannot save "+filename, ex);
            return false;
        }
    }
    
    public static Element loadXML(String filename)
    {
        try {
            InputStream is=new FileInputStream(filename);
            Builder builder = new Builder();
            Document doc = builder.build(is);
            if(doc!=null)
            {
                return doc.getRootElement();
            }
        } catch(Exception e) {
            _log.log(Level.WARNING, "Cannot load "+filename, e);
        }
        return null;
    }
}
