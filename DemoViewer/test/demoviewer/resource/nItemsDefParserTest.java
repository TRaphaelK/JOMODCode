/*
 * nItemsDefParserTest.java
 * JUnit based test
 *
 * Created on 2006. február 18., 22:48
 */

package demoviewer.resource;

import junit.framework.*;
import demoviewer.n3di.ModelInfo;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class nItemsDefParserTest extends TestCase {
    
    public nItemsDefParserTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(nItemsDefParserTest.class);
        
        return suite;
    }

    /**
     * Test of parseItemsDef method, of class demoviewer.resource.nItemsDefParser.
     */
    public void testParseItemsDef() throws Exception {
        System.out.println("parseItemsDef");
        
        String file = "C:\\vear_fun\\job\\demoview\\decompressed/items.def";
        nItemsDefParser instance = new nItemsDefParser();
        
        instance.parseItemsDef(file);
        ModelStore store=ResourceManager.getInstance().getModelStore();
        ModelInfo mi = store.getModelInfo(1356);
        if(mi==null) fail("Model not known");
    }
    
}
