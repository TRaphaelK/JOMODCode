/*
 * nItemsDefParser.java
 *
 * Created on 2006. február 18., 22:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.resource;

import com.jme.util.LoggingSystem;
import demoviewer.n3di.ModelInfo;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author vear
 */
public class nItemsDefParser {
    
    private Logger _log = LoggingSystem.getLogger();
    
    /** Creates a new instance of nItemsDefParser */
    public nItemsDefParser() {
    }
    
    public void parseItemsDef(String file) throws FileNotFoundException, IOException {
        _log.info("Parsing "+file);
        // get the ModelStore to fill
        ModelStore ms=ResourceManager.getInstance().getModelStore();
        // model which holds info
        ModelInfo mi=null;
        BufferedReader rdr=new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("US-ASCII")));
        String ln=rdr.readLine();
        while(ln!=null)
        {
            ArrayList<String> spl=splitLine(ln);
            // handle the tags
            if(spl.size()>0) {
                String cmd=spl.get(0).toLowerCase();
                if(cmd.equals("begin")) {
                    mi=new ModelInfo();
                    String ds="";
                    for(int i=1;i<spl.size();i++) {
                        // set the name, but strip the quotes
                        ds+=spl.get(i).replaceAll("\"","");
                        if(i<spl.size()-1) {
                            ds+=" ";
                        }
                    }
                    mi.setDescription(ds);
                } else if(cmd.equals("id")) {
                    String s=spl.get(1);
                    if((s!=null)&&(mi!=null))
                        mi.setTypeId(Integer.parseInt(s)-100000);
                    else {
                        _log.log(Level.WARNING,"Null id for "+mi.getDescription());
                    }
                } else if(cmd.equals("sid")) {
                    mi.setSid(spl.get(1));
                } else if(cmd.equals("type")) {
                    String tp=spl.get(1).toLowerCase();
                    if(tp.equals("vehicle")) {
                        mi.setType(ModelInfo.TYPE_VEHICLE);
                    } else if(tp.equals("object")) {
                        mi.setType(ModelInfo.TYPE_OBJECT);
                    } else if(tp.equals("building")) {
                        mi.setType(ModelInfo.TYPE_BUILDING);
                    } else if(tp.equals("decoration")) {
                        mi.setType(ModelInfo.TYPE_BUILDING);
                    } else if(tp.equals("marker")) {
                        mi.setType(ModelInfo.TYPE_MARKER);
                    } else if(tp.equals("foliage")) {
                        mi.setType(ModelInfo.TYPE_FOLIAGE);
                    } else if(tp.equals("person")) {
                        mi.setType(ModelInfo.TYPE_PERSON);
                    } else if(tp.equals("powerup")) {
                        mi.setType(ModelInfo.TYPE_POWERUP);
                    }
                } else if(cmd.equals("graphic")) {
                    mi.setModelName(spl.get(1));
                } else if(cmd.equals("husk")) {
                    mi.setHusk(spl.get(1));
                } else if(cmd.equals("end")) {
                    // put the prepared modelinfo into store
                    ms.addModelInfo(mi);
                    mi=null;
                }
            }
            ln=rdr.readLine();
        }
    }
    
    private ArrayList<String> splitLine(String ln)
    {
        ArrayList<String> ar=new ArrayList<String>();
        //first cut off comment part
        String [] sta=ln.split("[;]");
        if(sta.length==0) return ar;
        ln=sta[0];
        sta=ln.split("[ ;,\\t]");
        
        for(int i=0;i<sta.length;i++)
        {
            if(!sta[i].trim().equals(""))
            {
                ar.add(sta[i].trim());
            }
        }
        return ar;
    }
}
