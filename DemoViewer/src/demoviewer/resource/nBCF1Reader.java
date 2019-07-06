/*
 * nBCF1Reader.java
 *
 * Created on 2006. február 5., 0:30
 *
 * Reads in BCF1 compressed file
 */

package demoviewer.resource;

import com.jme.util.LoggingSystem;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.zip.Inflater;

/**
 *
 * @author vear
 */
public class nBCF1Reader {
    
    /** Creates a new instance of nBCF1Reader */
    public nBCF1Reader() {
    }
    
    public boolean decompressTo(String file, String fileto) {
        try {
            FileInputStream fs=new FileInputStream(file);
            // read header
            int hdr=fs.read()|fs.read()<<8|fs.read()<<16|fs.read()<<24;
            if(hdr!=0x31434642) {
                // not a BFC1, simply copy out
                fs.close();
                BufferedInputStream bfs=new BufferedInputStream(new FileInputStream(file));
                byte[] ind=new byte[bfs.available()];
                bfs.read(ind);
                bfs.close();
                BufferedOutputStream fso=new BufferedOutputStream(new FileOutputStream(fileto));
                fso.write(ind);
                fso.close();
                return true;
            }
            // read the length of the decompressed
            int declen=(fs.read()&0xff) |
                ((fs.read()&0xff) << 8) |
                ((fs.read()&0xff) << 16) |
                ((fs.read()&0xff) << 24);

            int fl=fs.available();
            byte[] indata=new byte[fl];
            fs.read(indata);
            fs.close();

            byte[] outdata=new byte[declen];
            // Decompress the bytes
            Inflater decompresser = new Inflater();
            decompresser.setInput(indata, 0, fl);
            int resultLength = decompresser.inflate(outdata);
            decompresser.end();

            BufferedOutputStream fso=new BufferedOutputStream(new FileOutputStream(fileto));
            fso.write(outdata);                
            fso.close();
            LoggingSystem.getLogger().log(Level.INFO, "Decompressed "+file+" to "+fileto);
        } catch(Exception e) {
            LoggingSystem.getLogger().log(Level.WARNING, "Could not extract texture file "+file+" to "+fileto, e);
            return false;
        }
        return true;
    }
}
