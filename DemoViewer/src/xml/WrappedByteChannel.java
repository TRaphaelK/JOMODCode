/*
 * WrappedByteChannel.java
 *
 * Created on 2006. április 22., 18:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package xml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author vear
 */
public class WrappedByteChannel implements WritableByteChannel {
    
    OutputStream os;
    /** Creates a new instance of WrappedByteChannel */
    public WrappedByteChannel(OutputStream os) {
        this.os=os;
    }

    public int write(ByteBuffer src) throws IOException {
        os.write(src.array());
        return src.array().length;
    }

    public boolean isOpen() {
        return true;
    }

    public void close() throws IOException {
        os.close();
    }
    
}
