/*
 * 
 * Vear 2017  * 
 */
package jb2.util;

/**
 *
 * @author vear
 */
public class JbException extends RuntimeException {

    /**
     * Creates a new instance of <code>JbException</code> without detail
     * message.
     */
    public JbException() {
    }

    /**
     * Constructs an instance of <code>JbException</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public JbException(String msg) {
        super(msg);
    }
}
