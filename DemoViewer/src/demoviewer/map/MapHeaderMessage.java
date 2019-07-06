/*
 * MapHeaderMessage.java
 *
 * Created on 2006. február 19., 16:21
 *
 * Global message, represents global nodes in an NPJ
 */

package demoviewer.map;

import com.jme.renderer.ColorRGBA;
import com.jme.util.LoggingSystem;
import java.util.logging.Logger;
import xml.Element;

/**
 *
 * @author vear
 */
public class MapHeaderMessage {
    
    private String missionname;
    private String maptype;
    private String terrainfile;
    private String envfile;
    private float waterlevel;
    private float watermurk;
    private ColorRGBA watercolor;
    private float fogdistance;
    private int fogtype=-1;
    private ColorRGBA fogcolor;
    private float skyheight;
    private float skyspeed;
    private ColorRGBA skyfogcolor;
    private float timeofday;
    private float todrate;
    // TODO other properties
    
    /** Creates a new instance of MapHeaderMessage */
    public MapHeaderMessage() {
    }
    
    public void fromXML(Element e) {
        if(e.getName().equals("MISSION_NAME")) {
            setMissionname(e.getText());
        } else if(e.getName().equals("TERRAIN")) {
            Element ce;
            ce=e.getChild("TERRAIN_FILE");
            if(ce!=null) {
                setTerrainfile(ce.getText());
            }
            ce=e.getChild("ENVIRONMENT_FILE");
            if(ce!=null) {
                setEnvFile(ce.getText());
            }
            ce=e.getChild("WATER_LEVEL");
            if(ce!=null) {
                setWaterlevel(ce.getTextfloat());
            }
            ce=e.getChild("WATER_MURK");
            if(ce!=null) {
                setWatermurk(ce.getTextfloat());
            }
            ce=e.getChild("WATER_COLOR");
            if(ce!=null) {
                setWaterColor(ce.getText());
            }
            ce=e.getChild("FOG_DISTANCE");
            if(ce!=null) {
                setFogEndDistance(ce.getTextfloat());
            }
            ce=e.getChild("FOG_TYPE");
            if(ce!=null) {
                setFogType(ce.getTextbyte());
            }
            ce=e.getChild("FOG_COLOR");
            if(ce!=null) {
                setFogColor(ce.getText());
            }
            ce=e.getChild("SKY_HEIGHT");
            if(ce!=null) {
                setSkyheight(ce.getTextfloat());
            }
            ce=e.getChild("SKY_SPEED");
            if(ce!=null) {
                setSkySpeed(ce.getTextfloat());
            }
            ce=e.getChild("SKYFOG_COLOR");
            if(ce!=null) {
                setSkyFogColor(ce.getText());
            }
            ce=e.getChild("TIME_OF_DAY");
            if(ce!=null) {
                setTimeOfDay(ce.getText());
            }
            ce=e.getChild("TIME_OF_DAY_RATE");
            if(ce!=null) {
                setTimeOfDayRate(ce.getText());
            }
        }
    }

    public String getMissionname() {
        return missionname;
    }

    public void setMissionname(String missionname) {
        this.missionname = missionname;
    }

    public String getMaptype() {
        return maptype;
    }

    public void setMaptype(String maptype) {
        this.maptype = maptype;
    }

    public String getTerrainfile() {
        return terrainfile;
    }

    public void setTerrainfile(String terrainfile) {
        this.terrainfile = terrainfile;
    }

    public void setEnvFile(String string) {
        this.envfile=string;
    }
    
    public String getEnvFile() {
        return envfile;
    }
    
    public float getWaterlevel() {
        return waterlevel;
    }

    public void setWaterlevel(float waterlevel) {
        this.waterlevel = waterlevel;
    }

    public float getWatermurk() {
        return watermurk;
    }

    public void setWatermurk(float watermurk) {
        this.watermurk = watermurk;
    }

    public void setWaterColor(String string) {
        // 00383B27
        // ignore,R,G,B
        // parse hex
        float r=Integer.parseInt(string.substring(2,3),16)/255f;
        float g=Integer.parseInt(string.substring(4,5),16)/255f;
        float b=Integer.parseInt(string.substring(6,7),16)/255f;
        watercolor=new ColorRGBA(r,g,b,watermurk);
    }
    
    public ColorRGBA getWaterColor() {
        return watercolor;
    }
    
    public float getFogEndDistance() {
        return fogdistance;
    }

    public void setFogEndDistance(float fogdistance) {
        this.fogdistance = fogdistance;
    }

    private void setFogType(byte b) {
        this.fogtype=b;
    }
    
    public int getFogType() {
        return fogtype;
    }

    public float getFogStartDistance() {
        float fs=fogdistance;
        switch(fogtype) {
            case 0: fs=0; break; // linear
            case 1: fs/=4; break;  //linear 25%
            case 2: fs/=2; break;  // linear 50%
            default: fs/=2; break;  // unknown
        }
        return fs;
    }
    
    public float getFogIntensity() {
        return 0.5f;
    }
    
    public void setFogColor(String string) {
        // 9AB6FF
        float r=Integer.parseInt(string.substring(0,1),16)/255f;
        float g=Integer.parseInt(string.substring(2,3),16)/255f;
        float b=Integer.parseInt(string.substring(4,5),16)/255f;
        fogcolor=new ColorRGBA(r,g,b,1f);
    }
  
    public ColorRGBA getFogColor() {
        if(fogcolor==null) return ColorRGBA.gray;
        return fogcolor;
    }

    public float getSkyheight() {
        return skyheight;
    }

    public void setSkyheight(float skyheight) {
        this.skyheight = skyheight;
    }

    private void setSkySpeed(float f) {
        this.skyspeed=f;
    }

    private void setSkyFogColor(String string) {
        // 9AB6FF
        float r=Integer.parseInt(string.substring(0,1),16)/255f;
        float g=Integer.parseInt(string.substring(2,3),16)/255f;
        float b=Integer.parseInt(string.substring(4,5),16)/255f;
        skyfogcolor=new ColorRGBA(r,g,b,0.5f);
    }
  
    public ColorRGBA getSkyFogColor() {
        return skyfogcolor;
    }

    private void setTimeOfDay(String string) {
        timeofday=0;
        // C0000 = 12:00:00
        if(string.length()>4) {
            // hours
            timeofday+=Integer.parseInt(string.substring(0,string.length()-4),16)*60*60;
            string=string.substring(string.length()-4, string.length());
        }
        if(string.length()>2) {
            // minutes
            timeofday+=Integer.parseInt(string.substring(0,1),16)*60;
            string=string.substring(string.length()-2, string.length());
        }
        if(string.length()>1) {
            // seconds
            timeofday+=Integer.parseInt(string.substring(0,1),16);
        }
    }

    private void setTimeOfDayRate(String string) {
        // 5A3
        // time in real time under which a full 24h ingame time passese
        todrate=Integer.parseInt(string,16);
    }
    
}
