/*
 * nTRNParserTest.java
 * JUnit based test
 *
 * Created on 2006. január 22., 23:48
 */

package demoviewer.terrain;

import demoviewer.resource.nTRNParser;
import junit.framework.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class nTRNParserTest extends TestCase {
    
    public nTRNParserTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(nTRNParserTest.class);
        
        return suite;
    }

    /**
     * Test of parseTRN method, of class demoviewer.terrain.nTRNParser.
     */
    public void testParseTRN() {
        System.out.println("testParseTRN");
        TerrainInfo trn=null;
        nTRNParser prsr=new nTRNParser();
        
        boolean fail=false;
        try {
            trn=prsr.parseTRN("C:/vear_fun/job/demoview/extracted/Dvxi4_d.trn");
            
        } catch(Exception e) {
            e.printStackTrace();
            fail=true;
        }
        
        
        if(fail||trn==null||!"Dvxi4".equals(trn.getTerrainName()))
        {
            fail("Failed nTRNParser");
        }
    }
    
}
