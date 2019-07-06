/*
 * TerrainInfo.java
 *
 * Created on 2006. január 22., 20:43
 *
 * Stores all information about the terrain
 */

package demoviewer.terrain;
import java.util.HashMap;
import nu.xom.Element;
import nu.xom.Elements;

/**
 *
 * @author vear
 */
public class TerrainInfo {
    
    private String terrain_name;
    // the water height, final water height is got from the map file
    private int water_height;
    // colormap of the terrain
    private String colormap;
    // base detailmap of the terrain
    private String detailmap;
    // detailmap_c1
    private String detailmap_c1;
    // detailmap_c2
    private String detailmap_c2;
    // detailmap_c3
    private String detailmap_c3;
    // detailblendmap of terrain
    private String detailblendmap;
    
    // the heightmap of the terrain
    private String polydata;
    // indexed colormap image of 
    private String foliagemap;
    private float water_rgb[] = new float[3];
    private int sectorcount;
    private int wrapx;
    private int wrapy;
    private float originx;
    private float originy;
    // the sectorcount*sectorcount number of sectors
    int sectors[];
    int sector_locks[]=new int [8];
            
    // the foliages for this map <Integer,nFoliage>
    HashMap<Integer, FoliageInfo> foliage=new HashMap<Integer,FoliageInfo>();
    
    /** Creates a new instance of TerrainInfo */
    public TerrainInfo() {
    }
    
    public Element toXML()
    {
        Element el=new Element("TerrainInfo");
        Element c;
        c=new Element("terrain_name"); c.appendChild(String.valueOf(terrain_name)); el.appendChild(c);
        c=new Element("water_height"); c.appendChild(String.valueOf(water_height)); el.appendChild(c);
        c=new Element("colormap"); c.appendChild(String.valueOf(colormap)); el.appendChild(c);
        c=new Element("detailmap"); c.appendChild(String.valueOf(detailmap)); el.appendChild(c);
        c=new Element("detailmap_c1"); c.appendChild(String.valueOf(detailmap_c1)); el.appendChild(c);
        c=new Element("detailmap_c2"); c.appendChild(String.valueOf(detailmap_c2)); el.appendChild(c);
        c=new Element("detailmap_c3"); c.appendChild(String.valueOf(detailmap_c3)); el.appendChild(c);
        c=new Element("detailblendmap"); c.appendChild(String.valueOf(detailblendmap)); el.appendChild(c);
        c=new Element("polydata"); c.appendChild(String.valueOf(polydata)); el.appendChild(c);
        c=new Element("foliagemap"); c.appendChild(String.valueOf(foliagemap)); el.appendChild(c);
        c=new Element("water_r"); c.appendChild(String.valueOf(water_rgb[0])); el.appendChild(c);
        c=new Element("water_g"); c.appendChild(String.valueOf(water_rgb[1])); el.appendChild(c);
        c=new Element("water_b"); c.appendChild(String.valueOf(water_rgb[2])); el.appendChild(c);
        c=new Element("sectorcount"); c.appendChild(String.valueOf(sectorcount)); el.appendChild(c);
        c=new Element("wrapx"); c.appendChild(String.valueOf(wrapx)); el.appendChild(c);
        c=new Element("wrapy"); c.appendChild(String.valueOf(wrapy)); el.appendChild(c);
        c=new Element("originx"); c.appendChild(String.valueOf(originx)); el.appendChild(c);
        c=new Element("originy"); c.appendChild(String.valueOf(originy)); el.appendChild(c);
        if(sectors!=null)
        {
            c=new Element("sectors");
            Element s;
            for(int i=0;i<sectors.length;i++)
            {
                s=new Element("sector"); s.appendChild(String.valueOf(sectors[i])); c.appendChild(s);
            }
            el.appendChild(c);
        }
        if(sector_locks!=null)
        {
            c=new Element("sector_locks");
            Element s;
            for(int i=0;i<sector_locks.length;i++)
            {
                s=new Element("sector_lock"); s.appendChild(String.valueOf(sector_locks[i])); c.appendChild(s);
            }
            el.appendChild(c);
        }
        // TODO save foliage
        
        return el;
    }
    
    public void fromXML(Element e)
    {
        terrain_name=e.getFirstChildElement("terrain_name").getValue();
        water_height=Integer.parseInt(e.getFirstChildElement("water_height").getValue());
        colormap=e.getFirstChildElement("colormap").getValue();
        detailmap=e.getFirstChildElement("detailmap").getValue();
        detailmap_c1=e.getFirstChildElement("detailmap_c1").getValue();
        detailmap_c2=e.getFirstChildElement("detailmap_c2").getValue();
        detailmap_c3=e.getFirstChildElement("detailmap_c3").getValue();
        detailblendmap=e.getFirstChildElement("detailblendmap").getValue();
        polydata=e.getFirstChildElement("polydata").getValue();
        foliagemap=e.getFirstChildElement("foliagemap").getValue();
        water_rgb[0]=Float.parseFloat(e.getFirstChildElement("water_r").getValue());
        water_rgb[1]=Float.parseFloat(e.getFirstChildElement("water_g").getValue());
        water_rgb[2]=Float.parseFloat(e.getFirstChildElement("water_b").getValue());
        sectorcount=Integer.parseInt(e.getFirstChildElement("sectorcount").getValue());
        wrapx=Integer.parseInt(e.getFirstChildElement("wrapx").getValue());
        wrapy=Integer.parseInt(e.getFirstChildElement("wrapy").getValue());
        originx=Float.parseFloat(e.getFirstChildElement("originx").getValue());
        originy=Float.parseFloat(e.getFirstChildElement("originy").getValue());
        sectors=new int[sectorcount*sectorcount];
        Elements se=e.getFirstChildElement("sectors").getChildElements();
        for(int i=0;i<se.size();i++) {
            Element s=se.get(i);
            sectors[i]=Integer.parseInt(s.getValue());
        }
        se=e.getChildElements("sector_locks");
        for(int i=0;i<se.size();i++) {
            sector_locks[i]=Integer.parseInt(se.get(i).getValue());
        }
        // TODO load foliage
    }
    
    public void addFoliageInfo(FoliageInfo flg)
    {
        foliage.put(new Integer(flg.getMapIndex()), flg);
    }
    
    public FoliageInfo getFoliageInfo(int index)
    {
        return foliage.get(new Integer(index));
    }
    
    public String getTerrainName() {
        return terrain_name;
    }

    public void setTerrainName(String terrain_name) {
        this.terrain_name = terrain_name;
    }

    public int getWaterHeight() {
        return water_height;
    }

    public void setWaterHeight(int water_height) {
        this.water_height = water_height;
    }

    public String getColormap() {
        return colormap;
    }

    public void setColormap(String colormap) {
        this.colormap = colormap;
    }

    public String getDetailmap() {
        return detailmap;
    }

    public void setDetailmap(String detailmap) {
        this.detailmap = detailmap;
    }

    public String getHeightfieldFile() {
        return polydata;
    }

    public void setHeightfieldFile(String file) {
        this.polydata = file;
    }

    public String getFoliageMap() {
        return foliagemap;
    }

    public void setFoliageMap(String foliagemap) {
        this.foliagemap = foliagemap;
    }

    public float[] getWaterRgb() {
        return water_rgb;
    }

    public void setWaterRgb(float[] water_rgb) {
        this.water_rgb = water_rgb;
    }

    public int getSectorCount() {
        return sectorcount;
    }

    public void setSectorCount(int sectorcount) {
        if(this.sectorcount != sectorcount)
        {
            this.sectorcount = sectorcount;
            sectors=new int[sectorcount*sectorcount];
        }
    }
    
    public int getSector(int posx, int posy)
    {
        return sectors[posy*sectorcount+posx];
    }
    
    public void setSector(int posx, int posy, int sector)
    {
        sectors[posy*sectorcount+posx]=sector;
    }
    
    public int getWrapx() {
        return wrapx;
    }

    public void setWrapx(int wrapx) {
        this.wrapx = wrapx;
    }

    public int getWrapy() {
        return wrapy;
    }

    public void setWrapy(int wrapy) {
        this.wrapy = wrapy;
    }

    public float getOriginx() {
        return originx;
    }

    public void setOriginx(float originx) {
        this.originx = originx;
    }

    public float getOriginy() {
        return originy;
    }

    public void setOriginy(float originy) {
        this.originy = originy;
    }

    public String getDetailmapC1() {
        return detailmap_c1;
    }

    public void setDetailmapC1(String detailmap_c1) {
        this.detailmap_c1 = detailmap_c1;
    }

    public String getDetailmapC2() {
        return detailmap_c2;
    }

    public void setDetailmapC2(String detailmap_c2) {
        this.detailmap_c2 = detailmap_c2;
    }

    public String getDetailmapC3() {
        return detailmap_c3;
    }

    public void setDetailmapC3(String detailmap_c3) {
        this.detailmap_c3 = detailmap_c3;
    }

    public String getDetailblendmap() {
        return detailblendmap;
    }

    public void setDetailblendmap(String detailblendmap) {
        this.detailblendmap = detailblendmap;
    }
    
}
