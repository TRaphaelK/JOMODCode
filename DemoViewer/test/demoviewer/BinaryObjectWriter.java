/*
 * BinaryObjectWriter.java
 *
 * Created on 2006. január 22., 12:24
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;
import nux.xom.xquery.StreamingPathFilter;
import nux.xom.xquery.StreamingTransform;

/**
 *
 * @author vear
 */
public class BinaryObjectWriter {
    
    /** Creates a new instance of BinaryObjectWriter */
    public BinaryObjectWriter() {
    }
    
    public class TestObj {
        public int value;
        
        public int getValue()
        {
            return value;
        }
        
        public void setValue(int value)
        {
            this.value=value;
        }
        
        public Element toXML()
        {
            Element xe=new Element("event");
            Element te=new Element("value"); te.appendChild(String.valueOf(value)); xe.appendChild(te);
            return xe;
        }
        
        public void fromXML(Element xe)
        {
            Element te=xe.getFirstChildElement("value");
            value=Integer.parseInt(te.getValue());
        }
    }
    
    public void ObjTest()
    {
        TestObj to=new TestObj();
        to.setValue(1);
        Element e=to.toXML();
        TestObj to2=new TestObj();
        to2.fromXML(e);
        System.out.println("Value: "+to2.getValue());
    }
    
    ArrayList objs=new ArrayList();
    public void createObjects()
    {
        // create the array
        for(int i=0;i<100000;i++)
        {
            TestObj ob=new TestObj();
            ob.setValue(i);
            objs.add(ob);
        }
    }
    
    public void testObjectStream(int dotest) throws Exception
    {
        // dataoutput without compression
        ObjectOutputStream oos;
        if(dotest==1)
        {
            oos=new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream("data.oos.gz"))));
        } else {
            oos=new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("data.oos")));
        }
        Iterator obi=objs.iterator();
        while(obi.hasNext())
        {
            TestObj evt=(TestObj)obi.next();
            oos.writeObject(evt);
            oos.flush();
        }
        oos.close();
    }
    
    public void testObjectStreamRead(int dotest)
    {
        /*
        ObjectInputStream ois;
        if(dozip)
        {
            ois=new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream("data.ogz"))));
        } else {
            ois=new ObjectInputStream(new BufferedInputStream(new FileInputStream("data.oos")));
        }
        ArrayList arr=(ArrayList)ois.readObject();
         */
    }
    
    public void testNuxWrite(int dotest) throws Exception
    {
        StreamingSerializerFactory ssf=new StreamingSerializerFactory();
        StreamingSerializer ser=null;
        OutputStream os=null;
        if(dotest==3)
        {
            // plain XML
            os=new BufferedOutputStream(new FileOutputStream("data.xml"));
            ser=ssf.createXMLSerializer(os, "UTF-8");
        } else if(dotest==4) {
            // gzip-d xml
            os=new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream("data.xml.gz")));
            ser=ssf.createXMLSerializer(os, "UTF-8");
        } else if(dotest==5) {
            // bnux without compression
            os=new BufferedOutputStream(new FileOutputStream("data.bnux.0"));
            ser = ssf.createBinaryXMLSerializer(os,0);
        } else if(dotest==6) {
            // bnux with compression 1
            os=new BufferedOutputStream(new FileOutputStream("data.bnux.1"));
            ser = ssf.createBinaryXMLSerializer(os,1);
        } else if(dotest==7) {
            // bnux with compression 9
            os=new BufferedOutputStream(new FileOutputStream("data.bnux.9"));
            ser = ssf.createBinaryXMLSerializer(os,9);
        } else if(dotest==8) {
            // bnux with gzip compression
            os=new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream("data.bnux.gz")));
            ser = ssf.createBinaryXMLSerializer(os,0);
        }
        ser.writeXMLDeclaration();
        ser.writeStartTag(new Element("events"));
        Iterator obi=objs.iterator();
        while(obi.hasNext())
        {
            TestObj evt=(TestObj)obi.next();
            ser.write(evt.toXML());
            ser.flush();
        }
        ser.writeEndTag();
        ser.writeEndDocument();
        os.close();
    }
    
    public void testNuxRead(int dotest) throws Exception
    {
        StreamingTransform myTransform = new StreamingTransform() {
             public Nodes transform(Element event) {
                 // save to file if needed,
                 // create event
                 if(event.getLocalName().equals("event"))
                 {
                     TestObj evt=new TestObj();
                     evt.fromXML(event);
                     objs.add(evt);
                 }
                 return new Nodes(); // mark current element as subject to garbage collection
             }
         };
         // parse document with a filtering Builder
         System.setProperty("nu.xom.Verifier.checkPCDATA", "false");
         System.setProperty("nu.xom.Verifier.checkURI", "false");
         InputStream is=null;
         // which input?
         if((dotest==9)||(dotest==10))
         {
             // plain XML
             if(dotest==9)
             {
                 is=new FileInputStream("data.xml");
             } else {
                 is=new GZIPInputStream(new FileInputStream("data.xml.gz"));
             }
             Builder builder = new Builder(new StreamingPathFilter("/events/event", null).createNodeFactory(null, myTransform));
             Document doc = builder.build(is);
         } else {
             // bnux format
             if(dotest==11)
             {
                 is=new FileInputStream("data.bnux.0");
             } else if(dotest==12)
             {
                 is=new FileInputStream("data.bnux.1");
             } else if(dotest==13)
             {
                 is=new FileInputStream("data.bnux.9");
             } else if(dotest==14)
             {
                 // gzip'd
                 is=new GZIPInputStream(new FileInputStream("data.bnux.gz"));
             }
             BinaryXMLCodec codec = new BinaryXMLCodec();
             Document doc2 = codec.deserialize(is, new StreamingPathFilter("/events/event", null).createNodeFactory(null, myTransform));
         }
         is.close();
    }
    
    public static void main(String[] args) {
        BinaryObjectWriter wrtr=new BinaryObjectWriter();
        wrtr.createObjects();
        int dotest=0;
        for(dotest=0;dotest<15;dotest++)
        {
            try {
                if(dotest==0)
                {
                    wrtr.ObjTest();
                } else {
                    // for reading tests, clear out the array
                    if(dotest>8)
                    {
                        wrtr.objs.clear();
                    }
                    long ts=System.currentTimeMillis();

                    if((dotest==1)||(dotest==2))
                    {
                        //wrtr.testObjectStream(dotest);
                    } else if(dotest<9) {

                        // bux tests
                        wrtr.testNuxWrite(dotest);
                    } else {
                        wrtr.testNuxRead(dotest);
                    }
                    System.out.println("Test: "+dotest+" time "+(System.currentTimeMillis()-ts)+" array size "+wrtr.objs.size());
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
