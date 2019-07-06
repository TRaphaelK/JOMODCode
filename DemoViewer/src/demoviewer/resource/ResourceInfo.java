/*
 * ResourceManager.java
 *
 * Created on 2006. január 22., 20:46
 *
 * Manages resources. Keeps track of availible files, and invoces converters,
 * if needed.
 */

package demoviewer.resource;
import com.jme.util.LoggingSystem;
import demoviewer.XMLUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

/**
 *
 * @author vear
 */
public class ResourceInfo {
    
    private Logger _log = LoggingSystem.getLogger();
    
    // the forder to JO
    private String jodir;
    // the list of PFF files <String, nPFFFile>
    private HashMap<String, nPFFFile> pfflist;
    // the folder to extracted files
    private String extract;
    // the folder to decompressed files
    private String decomp;
    // the folder for prepared files
    private String prepared;
    // the folder to cached files
    private String cached;
    
    // the map of the resources, by their original names <String, nResource>
    HashMap<String, nResource> resources=new HashMap<String, nResource>();
    
    /** Creates a new instance of ResourceManager */
    public ResourceInfo() {
    }
    
    // populates the manager from the XML config file
    public void fromXML() {
        try {
            Builder builder = new Builder();
            Document doc = builder.build(new File("resources.xml"));
            setJodir(doc.getRootElement().getFirstChildElement("jodir").getValue());
            setExtract(doc.getRootElement().getFirstChildElement("extract").getValue());
            setDecomp(doc.getRootElement().getFirstChildElement("decomp").getValue());
            setPrepared(doc.getRootElement().getFirstChildElement("prepared").getValue());
            setCached(doc.getRootElement().getFirstChildElement("cached").getValue());
            // get the resources
            Elements fls=doc.getRootElement().getFirstChildElement("resources").getChildElements();
            for(int i=0;i<fls.size();i++) {
                Element el=fls.get(i);
                nResource nr=new nResource();
                nr.fromXML(el);
                resources.put(nr.getName(), nr);
            }
        } catch(Exception e) {
            _log.log(Level.SEVERE, "Cannot load resource info", e);
        }
    }
    
    public void toXML() {
        Element el=new Element("ResourceInfo");
        Element c;
        c=new Element("jodir"); c.appendChild(String.valueOf(jodir)); el.appendChild(c);
        c=new Element("extract"); c.appendChild(String.valueOf(extract)); el.appendChild(c);
        c=new Element("decomp"); c.appendChild(String.valueOf(decomp)); el.appendChild(c);
        c=new Element("prepared"); c.appendChild(String.valueOf(prepared)); el.appendChild(c);
        c=new Element("cached"); c.appendChild(String.valueOf(cached)); el.appendChild(c);

        // save resource list
        if(!resources.isEmpty())
        {
            c=new Element("resources");
            Iterator<nResource> ri=resources.values().iterator();
            while(ri.hasNext())
            {
                nResource nr=ri.next();
                c.appendChild(nr.toXML());
            }
            el.appendChild(c);
        }
        
        
        XMLUtils.saveXML("resources.xml", el);
    }
    
    private ArrayList<String> getSubFolders(String folder) {
        ArrayList<String> files=new ArrayList<String>();
        File dir=new File(folder);
        File [] drl=dir.listFiles();
        if(drl!=null) {
            for(int i=0;i<drl.length;i++) {
                if(drl[i].isDirectory()) {
                    files.add(drl[i].getName());
                }
            }
        }
        return files;
    }
    
    /*
     * Get a list of PFF files in a given folder
     */
    private ArrayList<String> getPFFFiles(String folder) {
        ArrayList<String> files=new ArrayList<String>();
        File dir=new File(folder);
        String [] drl=dir.list();
        if(drl!=null) {
            for(int i=0;i<drl.length;i++) {
                if(drl[i].toLowerCase().endsWith(".pff")) {
                    // pff file
                    files.add(drl[i]);
                    //_log.
                }
            }
        }
        return files;
    }
    
    private void refreshPFFList(String expdir) {
        String dr=jodir;
        if(!expdir.equals("")) {
            dr+="/"+expdir;
        }
        // list of files in the dir
        ArrayList<String> pfl=getPFFFiles(dr);
        Iterator<String> pfi=pfl.iterator();
        while(pfi.hasNext()) {
            String pf=pfi.next();
            nPFFFile p=new nPFFFile(jodir,expdir,pf);
            if(!expdir.equals("")) {
                pf=expdir+"/"+pf;
            }
            pfflist.put(pf,p);
            _log.info("PFF "+pf);
        }
    }
    
    /*
     * Registers PFF files from games folders
     */
    public void refreshPFFList() {
        if(pfflist==null) { pfflist=new HashMap<String, nPFFFile>(); }
        else { pfflist.clear(); };
        
        // list files in the base JO dir
        refreshPFFList("");
        // get the list of expansions
        ArrayList exps=getSubFolders(jodir+"/expansion");
        Iterator<String> sbi=exps.iterator();
        while(sbi.hasNext()) {
            String sub=sbi.next();
            // load up the pffs in the expansion
            refreshPFFList("expansion/"+sub);
        }
    }
    
    public nResource getResource(String resname)
    {
        return resources.get(resname);
    }
    
    public void addResource(nResource res)
    {
        resources.put(res.getName(), res);
    }
    
    public String getJodir() {
        return jodir;
    }
    
    public void setJodir(String jodir) {
        this.jodir = jodir;
    }
    
    public String getExtract() {
        return extract;
    }
    
    public void setExtract(String extract) {
        this.extract = extract;
    }
    
    public String getDecomp() {
        return decomp;
    }
    
    public void setDecomp(String decomp) {
        this.decomp = decomp;
    }
    
    public String getPrepared() {
        return prepared;
    }
    
    public void setPrepared(String prepared) {
        this.prepared = prepared;
    }
    
    public String getCached() {
        return cached;
    }
    
    public void setCached(String cached) {
        this.cached = cached;
    }

    public HashMap<String, nPFFFile> getPfflist() {
        return pfflist;
    }
}
