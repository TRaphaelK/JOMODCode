/*
 * 
 * Vear 2017  * 
 */
package jb2.jo;

/**
 *
 * @author vear
 */
public class FailedMemoryAccess extends RuntimeException {

    /**
     * Creates a new instance of <code>FailedRead</code> without detail message.
     */
    public FailedMemoryAccess() {
    }

    /**
     * Constructs an instance of <code>FailedRead</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public FailedMemoryAccess(String msg) {
        super(msg);
    }
}
