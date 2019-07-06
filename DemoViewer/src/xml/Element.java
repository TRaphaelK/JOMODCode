/*
 * Element.java
 *
 * Created on 2006. április 22., 15:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

/**
 *
 * @author vear
 */
public class Element {
       
    private ArrayList<Element> children;
    private String name;
    private int dtype=0;
    private static final int VALUE_NULL=0;
    private static final int VALUE_NODE=1;
    private static final int VALUE_OBJ=2;   // data stored in objValue
    private static final int VALUE_STR=4;
    private static final int VALUE_INT=8;   // data stored in intValue
    private static final int VALUE_SHORT=16;
    private static final int VALUE_FLOAT=32;
    private static final int VALUE_BOOLEAN=64;
    private static final int VALUE_BYTE=128;
    private static final int VALUE_BITSET=256;
    
    private Object objValue;
    private int intValue;
    private float floatValue;
    
    private Element parent;
    
    /** Creates a new instance of Element */
    public Element(String name) {
        this.name=name;
    }
    
    public String getName() {
        return name;
    }
    
    public Element setText(String string) {
        objValue=string;
        dtype=VALUE_STR|VALUE_OBJ;
        return this;
    }

    public Element setText(Integer integer) {
        intValue=integer.intValue();
        dtype=VALUE_INT;
        return this;
    }
    
    public Element setText(float floatvalue) {
        floatValue=floatvalue;
        dtype=VALUE_FLOAT;
        return this;
    }
    
    public Element setText(short shortvalue) {
        intValue=shortvalue;
        dtype=VALUE_SHORT|VALUE_INT;
        return this;
    }
    
    public Element setText(byte bytevalue) {
        intValue=bytevalue;
        dtype=VALUE_BYTE|VALUE_INT;
        return this;
    }
    
    public Element setText(boolean booleanvalue) {
        intValue=booleanvalue?1:0;
        dtype=VALUE_BOOLEAN|VALUE_INT;
        return this;
    }
    
    public Element setText(boolean[] bitset) {
        intValue=0;
        for(int i=0;i<bitset.length;i++) {
            intValue|=(bitset[i]?1:0)<<i;
        }
        dtype=VALUE_BITSET|VALUE_INT;
        return this;
    }
    
    public short getTextshort() {
        return (short)getTextint();
    }
    
    public Integer getTextInteger() {
        return new Integer(getTextint());
    }
    
    public int getTextint() {
        if((dtype&VALUE_INT)!=0)
            return intValue;
        else if((dtype&VALUE_FLOAT)!=0) 
            return (int)floatValue;
        else
            return Integer.parseInt(getText());
    }

    public byte getTextbyte() {
        return (byte)getTextint();
    }
    
    public boolean getTextboolean() {
        if((dtype&VALUE_BOOLEAN)!=0)
            return intValue!=0;
        else
            return getText().equals("true");
    }
    
    public float getTextfloat() {
        if((dtype&VALUE_FLOAT)!=0) 
            return floatValue;
        else if((dtype&VALUE_INT)!=0)
            return intValue;
        else
            return Float.parseFloat(getText());
    }
    
    public boolean[] getTextbitset(boolean[] store) {
        int val=0;
        if((dtype&VALUE_INT)!=0) {
            val=intValue;
        } else {
            val=Integer.parseInt(getText());
        }
        int bits=Integer.highestOneBit(val);
        if(store==null) store=new boolean[bits+1];
        else
            bits=store.length<32?store.length-1:31;
        for(int i=0;i<=bits;i++)
            store[i]=(val&(1<<i))!=0;
        return store;
    }
    
    public String getText() {
        if((dtype&VALUE_STR)!=0)
            return (String) objValue;
        else if((dtype&VALUE_OBJ)!=0)
            return objValue.toString();
        else if((dtype&VALUE_BOOLEAN)!=0)
            return intValue!=0?"true":"false";
        else if((dtype&VALUE_INT)!=0)
            return String.valueOf(intValue);
        else if((dtype&VALUE_FLOAT)!=0)
            return String.valueOf(floatValue);
        return null;
    }
    
    private void assureChildren() {
        if(children==null) {
            children=new ArrayList();
            dtype=VALUE_NODE;
        }
    }
    
    public Element addContent(Element child) {
        assureChildren();
        children.add(child);
        child.parent=this;
        return this;
    }
    
    public Element addContent(String string) {
        Element child=new Element(string);
        addContent(child);
        return child;
    }
    
    public void removeContent() {
        dtype=VALUE_NULL;
        if(children!=null) children.clear();
    }
    
    public Element getChild(String child) {
        if(children==null || dtype!=VALUE_NODE) return null;
        Element ch=null;
        Element che=null;
        for(int i=0, j=children.size(); i<j && ch==null;i++) {
            che=children.get(i);
            if(che.getName().equals(child)) ch=che;
        }
        return ch;
    }
    
    public ArrayList getChildren() {
        if(dtype!=VALUE_NODE)
            return null;
        return children;
    }

    public ArrayList getChildren(String child) {
        if(dtype!=VALUE_NODE)
            return null;
        ArrayList sch=new ArrayList();
        Element che=null;
        for(int i=0, j=children.size(); i<j;i++) {
            che=children.get(i);
            if(che.getName().equals(child)) sch.add(che);
        }
        return sch;
    }
    
    public Element setChild(String child) {
        Element che=getChild(child);
        if(che==null) {
            che=new Element(child);
            addContent(che);
        }
        return che;
    }
    
    public String getChildText(String child) {
        Element che=getChild(child);
        return che==null?null:che.getText();
    }
    
    public Integer getChildInteger(String child) {
        Element ce=getChild(child);
        return ce==null?null:ce.getTextInteger();
    }
    
    public short getChildshort(String child) {
        Element ce=getChild(child);
        return ce==null?0:ce.getTextshort();
    }
    
    public boolean getChildboolean(String child) {
        Element ce=getChild(child);
        return ce==null?false:ce.getTextboolean();
    }

    public byte getChildbyte(String child) {
        Element ce=getChild(child);
        return ce==null?0:ce.getTextbyte();
    }
    
    public boolean[] getChildbitset(String child, boolean[] store) {
        Element ce=getChild(child);
        return ce==null?null:ce.getTextbitset(store);
    }
    
    public void toXML(WritableByteChannel ch) throws IOException {
        
        ByteBuffer startTag=ByteBuffer.allocate(name.length()+2);
        startTag.put("<".getBytes());
        startTag.put(name.getBytes());
        startTag.put(">".getBytes());
        ch.write(startTag);
        String val=null;
        if(dtype==VALUE_NODE) {
            if(children!=null) {
                for(int i=0,j=children.size();i<j;i++) {
                    children.get(i).toXML(ch);
                }
            }
        } else {
            val=getText();
            if(val!=null)
                ch.write(ByteBuffer.wrap((val).getBytes()));
        }
        ByteBuffer endTag=ByteBuffer.allocate(name.length()+3);
        endTag.put("</".getBytes());
        endTag.put(name.getBytes());
        endTag.put(">".getBytes());
        ch.write(endTag);
    }
    
    public void fromXML(XMLReader r) throws IOException {
        // start reading
        boolean cont=true;
        String ne;
        while(cont) {
            ne=r.getNext();
            if(ne==null) {
                cont=false;
            } else if(ne.startsWith("</")) {
                cont=false;
                r.skip();
            } else if(ne.startsWith("<") && ne.endsWith("/>")) {
                ne=ne.substring(1,ne.length()-2).trim();
                Element sube=new Element(ne);
                addContent(sube);
                r.skip();
            } else if(ne.startsWith("<")) {
                // new child
                ne=ne.substring(1,ne.length()-1);
                Element sube=new Element(ne);
                sube.fromXML(r);
                addContent(sube);
            } else {
                String close=r.getNext();
                if(close.length()>2 && close.substring(2,close.length()-1).equals(name)) {
                    // fixed value
                    setText(ne);
                    cont=false;
                    r.skip();
                } else
                    r.back();
            }
        }
    }
    
    protected void toString(StringBuffer app) {
        app.append("[").append(name);
        if(dtype==VALUE_NODE) {
            if(children!=null) {
                for(int i=0,j=children.size();i<j;i++) {
                    children.get(i).toString(app);
                }
            }
        }
        app.append("]");
    }
    
    public String toString() {
        StringBuffer apb=new StringBuffer();
        toString(apb);
        return apb.toString();
    }
}
