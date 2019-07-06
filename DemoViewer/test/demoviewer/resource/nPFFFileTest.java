/*
 * nPFFFileTest.java
 * JUnit based test
 *
 * Created on 2006. február 18., 11:22
 */

package demoviewer.resource;

import java.io.File;
import junit.framework.*;
import com.jme.util.LittleEndien;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;
import nu.xom.Element;

/**
 *
 * @author vear
 */
public class nPFFFileTest extends TestCase {
    
    public nPFFFileTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(nPFFFileTest.class);
        
        return suite;
    }

    /**
     * Test of refreshFileList method, of class demoviewer.resource.nPFFFile.
     */
    public void testRefreshFileList() {
        System.out.println("refreshFileList");
        
        nPFFFile instance = new nPFFFile("C:\\Games\\JO1515", "", "resource.pff");
        
        instance.refreshFileList();
        
    }

    /**
     * Test of extractTo method, of class demoviewer.resource.nPFFFile.
     */
    public void testExtractTo() {
        System.out.println("extractTo");
                
        nPFFFile p=new nPFFFile("C:\\Games\\JO1515", "", "resource.pff");
        p.refreshFileList();
        p.addExtractable("A_Buggy.dds");
        p.addExtractable("DSTRYKR1.3DI");
        p.addExtractable("FSLDR04.3DI");
        String path="C:\\temp\\joripped\\demoview\\extracted";
        p.extractTo(path);
        if(!new File(path+"/"+"A_Buggy.dds").exists()||!new File(path+"/"+"FSLDR04.3DI").exists()) {
            fail("No extracted files");
        }
    }
    
}
