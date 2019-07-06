/*
 * ResourceInfoTest.java
 * JUnit based test
 *
 * Created on 2006. február 18., 12:15
 */

package demoviewer.resource;

import junit.framework.*;
import com.jme.util.LoggingSystem;
import demoviewer.XMLUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

/**
 *
 * @author vear
 */
public class ResourceInfoTest extends TestCase {
    
    public ResourceInfoTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ResourceInfoTest.class);
        
        return suite;
    }


    /**
     * Test of refreshPFFList method, of class demoviewer.resource.ResourceInfo.
     */
    public void testRefreshPFFList() {
        System.out.println("refreshPFFList");
        
        ResourceInfo instance = new ResourceInfo();
        instance.setJodir("C:\\Games\\JO");
        instance.refreshPFFList();
        
    }    
}
