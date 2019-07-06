/*
 * nPFFFile.java
 *
 * Created on 2006. február 17., 20:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.resource;

import com.jme.util.LittleEndien;
import com.jme.util.LoggingSystem;
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
public class nPFFFile {
    
    private Logger _log = LoggingSystem.getLogger();
    
    // the name of this PFF file
    private String name;
    // the relative filename from JO dir
    private String path;
    // the path to JO folder
    private String jopath;
    
    LittleEndien in;
    
    private class nPFFHeader {
          int size;  //00000000h Size of Header   
          int tag;  //00000004h PFF3 Tag For Type Checking   
          int numfiles;  //00000008h Number of files in the pff   
          int namesize;  //0000000Ch Name Segment Size   
          int namelist;  //00000010h Pointer to Name List Section   
            //00000014h Start of files
    }
    
    nPFFHeader header;
            
    // represents data about a file inside the PFF archive
    // its 0x24 bytes long
    public class nPFFItem {
        int deleted;        //00000000h Delete Trigger 0 = not deleted 1 = Deleted   
        int position;    //00000004h Pointer to where the file is located   
        int size;    //00000008h Size of File   
        long dateutd;    //0000000Ch When file was Packed Date Stamp UDT UNIX   
        String name;    //00000010h File Name 0Fh in size   
            //0000001Fh 1 Null byte   
        long modudt;    //00000020h Last Date File Was Modded Date Stamp 
    }
    
    // the filelist in the PFF file
    HashMap<String,nPFFItem> filelist=null;
    
    // the filelist of extractable items
    // items are collected to this list, then
    // extracted in a single pass
    // this is a Vector, because its handy to have
    // cross-thread synchronization
    HashSet<String> extractable=new HashSet<String>();
    
    private void log(String txt) {
        _log.fine(txt);
    }
    
    /** Creates a new instance of nPFFFile */
    public nPFFFile() {
        
    }
    
    public nPFFFile(String jopath, String path, String name) {
        this.jopath=jopath;
        this.name=name;
        this.path=path;
    }
    
    private void checkFileList() {
        if(filelist==null) {
            filelist=new HashMap<String,nPFFItem>();
            refreshFileList();
        }
    }
    
    public String getFullFileName() {
        String fp=jopath;
        if(path!=null && !path.equals(""))
            fp+="/"+path;
        fp+="/"+name;
        return fp;
    }
    
    /*
     * Reads in the file list from the PFF, also chaches the header
     */
    public void refreshFileList() {
        // clear out the previous content of filelist
        filelist.clear();
        try {
            // open the file
            openFile();
            // read in the header
            readHeader();
            // read in the filelist
            readFileList();
            // close the file
            closeFile();
        } catch (Exception ex) {
            //_log.log(Level.WARNING, "Cannot read PFF file "+getFullFileName(),ex);
            log("Cannot read PFF file "+getFullFileName());
        }
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getSubPath() {
        return path;
    }

    public void setSubPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return jopath;
    }

    public void setPath(String jopath) {
        this.jopath = jopath;
    }
    
    private void openFile() throws FileNotFoundException {
        in=new LittleEndien(new FileInputStream(getFullFileName()));
    }
    
    private void readHeader() throws IOException {
        // create a header
        header=new nPFFHeader();
        header.size=in.readInt();
        header.tag=in.readInt();
        header.numfiles=in.readInt();
        header.namesize=in.readInt();
        header.namelist=in.readInt();
        //_log.fine("PFF: files "+header.numfiles);
        log("PFF: files "+header.numfiles+" nameblock size "+header.namesize+" nameblock position "+header.namelist);
    }
    
    private void closeFile() throws IOException {
        in.close();
        in=null;
    }

    /*
     * Reads in a zero-terminated string
     */
    private String readString() throws IOException {
        String s="";
        char c=0;
        do {
            c=(char)in.readUnsignedByte();
            if(c!=0) s+=c;
        } while(c!=0);
        return s;
    }
    
    private final long readUInt() throws IOException{
        long rt=in.readUnsignedByte()&0xff;
        rt|=(in.readUnsignedByte()&0xff) << 8;
        rt|=(in.readUnsignedByte()&0xff) << 16;
        rt|=(((long)in.readUnsignedByte()&0xff)) << 24;
        return rt;
    }
    
    private void skipBytes(int num) throws IOException {        
        int skipped=0;
        do {
            skipped+=in.skipBytes(num-skipped);
        } while(skipped<num);
    }
    
    private void readFileList() throws IOException {
        DateFormat df=DateFormat.getDateInstance();
        
        // skip to filelist position
        skipBytes(header.namelist-0x14);
        
        // read in numfiles number of records
        for(int numread=0;numread<header.numfiles;numread++) {
            // read in data for one item
            nPFFItem itm=new nPFFItem();
            itm.deleted=in.readInt();        //00000000h Delete Trigger 0 = not deleted 1 = Deleted
            itm.position=in.readInt();    //00000004h Pointer to where the file is located
            itm.size=in.readInt();    //00000008h Size of File
            itm.dateutd=readUInt();    //0000000Ch When file was Packed Date Stamp UDT UNIX
            itm.name=readString();    //00000010h File Name 0Fh in size
            skipBytes(0xf-itm.name.length());
            //0000001Fh 1 Null byte
            itm.modudt=readUInt();    //00000020h Last Date File Was Modded Date Stamp
            // put the item into the map
            if(!"<DEAD SPACE>".equals(itm.name)) {
                filelist.put(itm.name.toLowerCase(),itm);
            }//df.format(new Date(
            log("PFF: "+itm.name+" "+itm.position+" "+itm.size+" "+itm.dateutd+" "+itm.deleted+" "+itm.modudt);
            
        }
    }
    
    /*
     * Mark a resource to be extracted by an extraction run
     */
    public void addExtractable(String name) {
        extractable.add(name.toLowerCase());
    }
    
    /*
     * Extracts files marker for extraction into
     * the given path
     */
    public void extractTo(String path) {
        // create a sorted list of files
        TreeMap<Integer,nPFFItem> list=new TreeMap<Integer,nPFFItem>();
        Iterator<String> fli=extractable.iterator();
        while( fli.hasNext() ) {
            String fl=fli.next();
            nPFFItem itm=filelist.get(fl);
            if(itm!=null) {
                list.put(new Integer(itm.position),itm);
            }
        }
        if(!list.isEmpty()) {
            try {
                // open the file
                openFile();
                // set current position to 0
                int cp=0;
                while(!list.isEmpty()) {
                    //get the position
                    Integer fp=list.firstKey();
                    int pos=fp.intValue();
                    // skip to position
                    skipBytes(pos-cp);
                    // get the descriptor
                    nPFFItem itm=list.remove(fp);
                    // create array to hold the file
                    byte[] fla=new byte[itm.size];
                    // read in the file
                    in.readFully(fla);
                    // save the file
                    FileOutputStream fos=new FileOutputStream(path+"/"+itm.name);
                    fos.write(fla);
                    fos.close();
                    _log.info("Extracted "+itm.name);
                    // mark the new position
                    cp=pos+itm.size;
                    // remove item from extractable
                    extractable.remove(itm.name.toLowerCase());
                }
                // close the file
                closeFile();
            } catch (Exception ex) {
                ex.printStackTrace();
                log("Could not extract from PFF "+this.getName());
            }
        }
    }
    
    /*
     * Returns true is the PFF contains a given file
     */
    public boolean containsFile(String name) {
        checkFileList();
        return filelist.containsKey(name.toLowerCase());
    }
}
