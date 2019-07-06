/*
 * XMLReader.java
 *
 * Created on 2006. április 22., 19:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package xml;

import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author vear
 */
public class XMLReader {
    
        private char[] buffer=new char[255];
        private StringBuffer acum=new StringBuffer();
        private Reader ch;
        private int prevstart=-1, prevend=-1;
        private boolean finishread=false;
        private String currentValue;
        private String nextValue;
        private int start=0;
                
    /** Creates a new instance of XMLReader */
    public XMLReader(Reader ch) {
        this.ch=ch;
    }
       
    public String remove() {
        String next=currentValue;
        if(prevstart>-1) {
            //acum.delete(prevstart,prevend);
            start=prevend;
            prevstart=-1;
            prevend=-1;
            currentValue=null;
        }
        return next;
    }
    
    public void skip() throws IOException {
        String val=getNext();
        if(val!=null && val.startsWith("<")) {
            back();
        }
    }
    
    public void back() {
        nextValue=currentValue;
    }
    
    public String getNext() throws IOException {
        if(nextValue!=null) {
            currentValue=nextValue;
            nextValue=null;
            return currentValue;
        }
        remove();
        
        int read=0;
        while((!finishread || acum.length()>0) && prevstart==-1) {
            
            if(acum.length()>0) {
                if(start<acum.length()) {
                    // do we have a tag element
                    if(acum.charAt(start)=='<') {
                        prevend=acum.indexOf(">",start);
                        if(prevend>-1) {
                            prevstart=start;
                            prevend++;
                        }
                    }
                    if(prevstart==-1) {
                        // do we have a text element
                        prevend=acum.indexOf("<",start);
                        if(prevend>-1 && prevend>start) {
                            prevstart=start;
                            //prevend;
                        }
                    }
                }
                if(prevstart==-1 && finishread) {
                    prevstart=start;
                    prevend=acum.length();
                }
            }
            // no new elements found, read the input some more
            if(prevstart==-1 && !finishread) {
                if( (read=ch.read(buffer)) > -1 ) {
                    // remove already processed
                    acum.delete(0,start);
                    start=0;
                    acum.append(buffer, 0, read);
                } else {
                    finishread=true;
                }
            }
        }
        if(prevstart>-1) {
            currentValue=acum.substring(prevstart, prevend);
        }
        return currentValue;
    }
    
    public String skipRootElement() throws IOException {
        String se;
        se=getNext(); skip();
        if(se.startsWith("<?xml")) {
            se=getNext(); se=se.substring(1,se.length()-1); skip();
        }
        return se;
    }
    
    public void close() throws IOException {
        if(ch!=null) {
            ch.close();
            ch=null;
            finishread=true;
        }
    }
    
    public Element getNextChildElement() throws IOException {
        Element e=null;
        String ne=getNext();
        if(ne.startsWith("</")) {
            skip();
        } else if(ne.startsWith("<") && ne.endsWith("/>")) {
            ne=ne.substring(1,ne.length()-2).trim();
            e=new Element(ne);
            skip();
        } else if(ne.startsWith("<")) {
            ne=ne.substring(1,ne.length()-1);
            e=new Element(ne);
            e.fromXML(this);
        } else back();
        return e;
    }
}
