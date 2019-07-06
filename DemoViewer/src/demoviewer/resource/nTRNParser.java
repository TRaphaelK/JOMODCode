/*
 * nTRNParser.java
 *
 * Created on 2006. január 22., 20:44
 *
 * Parses information in a TRN file into a TerrainInfo object
 */

package demoviewer.resource;

import demoviewer.terrain.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;


/**
 *
 * @author vear
 */
public class nTRNParser {
    
    /** Creates a new instance of nTRNParser */
    public nTRNParser() {
    }
    
    // parses the TRN file, and loads it up into a TerrainInfo object
    public TerrainInfo parseTRN(String trnfile) throws java.io.FileNotFoundException, java.io.IOException {
        TerrainInfo trn=new TerrainInfo();
        BufferedReader rdr=new BufferedReader(new InputStreamReader(new FileInputStream(trnfile), Charset.forName("US-ASCII")));
        int sectorlines=0;
        FoliageInfo foliage=null;
        String ln=rdr.readLine();
        while(ln!=null) {
            ArrayList<String> spl=splitLine(ln);
            if(spl.size()>0) {
                String cmd=spl.get(0);
                if(cmd.equals("terrain_name")) {
                    String tn=spl.get(1);
                    trn.setTerrainName(tn.substring(1, tn.length()-1));
                } else if(cmd.equals("water_height")) {
                    trn.setWaterHeight(Integer.parseInt(spl.get(1)));
                } else if(cmd.equals("polytrn_colormap")) {
                    trn.setColormap(spl.get(1));
                } else if(cmd.equals("polytrn_detailmap")) {
                    trn.setDetailmap(spl.get(1));
                } else if(cmd.equals("polytrn_detailmap_c1")) {
                    trn.setDetailmapC1(spl.get(1));
                } else if(cmd.equals("polytrn_detailmap_c2")) {
                    trn.setDetailmapC2(spl.get(1));
                } else if(cmd.equals("polytrn_detailmap_c3")) {
                    trn.setDetailmapC3(spl.get(1));
                } else if(cmd.equals("polytrn_detailblendmap")) {
                    trn.setDetailblendmap(spl.get(1));
                } else if(cmd.equals("polytrn_polydata")) {
                    trn.setHeightfieldFile(spl.get(1));
                } else if(cmd.equals("polytrn_foliagemap")) {
                    trn.setFoliageMap(spl.get(1));
                } else if(cmd.equals("water_rgb")) {
                    trn.setWaterRgb(new float[] {Float.parseFloat(spl.get(1)), Float.parseFloat(spl.get(2)), Float.parseFloat(spl.get(3))});
                } else if(cmd.equals("polytrn_sectorcount")) {
                    trn.setSectorCount(Integer.parseInt(spl.get(1)));
                } else if(cmd.equals("polytrn_wrapx")) {
                    trn.setWrapx(Integer.parseInt(spl.get(1)));
                } else if(cmd.equals("polytrn_wrapy")) {
                    trn.setWrapy(Integer.parseInt(spl.get(1)));
                } else if(cmd.equals("polytrn_origin")) {
                    trn.setOriginx(Float.parseFloat(spl.get(1)));
                    trn.setOriginy(Float.parseFloat(spl.get(2)));
                } else if(cmd.equals("polytrn_sectors")) {
                    // parse a line of sectors
                    for(int i=0;i<trn.getSectorCount();i++)
                    {
                        trn.setSector(i, sectorlines, Integer.parseInt(spl.get(i+1)));
                    }
                    // increase the number of sector lines read
                    sectorlines++;
                } else if(cmd.equals("foliage")) {
                    // create a new foliage resource
                    foliage=new FoliageInfo();
                }  else if(cmd.equals("end")) {
                    // proper end of a foliage block, add it to terraininfo
                    if(foliage!=null) {
                        trn.addFoliageInfo(foliage);
                        foliage=null;
                    }
                } else if(cmd.equals("graphic")) {
                    foliage.setModel(spl.get(1));
                } else if(cmd.equals("match")) {
                    foliage.setMapIndex(Integer.parseInt(spl.get(1)));
                }
                
                // TODO read locks?
                
            }
            ln=rdr.readLine();
        }
        return trn;
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
