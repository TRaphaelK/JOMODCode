/*
 * 
 * Vear 2017-2018  * 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package jb2.jo;

import jb2.AppContext;
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.JbException;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class GameObject {
    
    public final int itemClass;
    // our own assigned usage of the object
    // this can change if we reclassify an object
    public TypeMap.TypeFolder typeFolder;
    public TypeMap.TypeId type;
    public final JOPointer ssnbase;
    // pointer to player data if player
    public final JOPointer plbase;
    // is it dedicated hosz
    public final boolean host;
    
    public final Vector3f position = new Vector3f();
    public final BoundingBox bounds=new BoundingBox();
    
    // the heading, looking direction
    public float ftheta; // direction X and Z plane
    public float phi; // elevation on Y
    public float omega; // roll

    // xz extent estimate
    public float radius;
    public final int SSN;
    public final String name;
//    public final String typeName;
    public final int typeId;
    
    public final JOPointer botbase;
    // move waypoint, 125 to move to ssn
    // below 125 seat positions when attached to a vehicle
    public int moveWpn;
    // move to marked ssn or enter vehicle
    public int moveTarget;
    // team
    public int team;
    // renderflags (can chanche often)
    public int renderFlags;
    // dead
    public boolean dead;
    // fireing
    public boolean shooting;
    // jumping/falling
    public boolean falling;
    // on ladder
    public boolean onladder;
    // vehicles driver
    public GameObject vehiclesDriver;
    // last killer
    public GameObject lastKiller;
    // attached to vehicle or emplacement
    public GameObject attachedTo;
    public final Vector3f spawnPosition = new Vector3f();
    
    public GameObject(int SSN) {
        // create dummy object
        itemClass=-1;
        ssnbase=null;
        plbase =null;
        host=false;
        this.SSN = SSN;
        this.name = null;
        this.typeId = 0;
        this.botbase = null;
    }
    
    public GameObject(int itc, long ssnptr, long playerptr, boolean host) {
        itemClass = itc;
        ssnbase = new JOPointer(ssnptr);
        if(playerptr!=0)
            plbase = new JOPointer(playerptr);
        else
            plbase = null;
        this.host = host;
        // derive items that do not change
        SSN = JOGame.readInt(ssnbase, JOAdd.GameObjectStruct.SSN.value);
        // read the typebase
        JOPointer typeBase=new JOPointer(0);
        typeId = JOGame.getTypeId(ssnbase, typeBase );
        if(typeBase.get() == 0) {
            typeBase = null;
        }
        type = TypeMap.getType(typeId);
        
        float extent = 1f;
        float yextent = 1f;
        if(type!=null) {
            extent = type.xzextent;
            yextent = type.yextent;
        }
        
        bounds.extents.set(extent, yextent, extent);
        
        if(plbase!=null) {
            name=JOGame.readStringZ(ssnbase, JOAdd.GameObjectStruct.PlayerName.value, 32);
        } else if(typeBase!=null) {
            name=typeBase.getString(JOAdd.TypeStruct.Objname.value);
        } else {
            name=null;
        }
        if(plbase==null) {
            // not a player, try to read the bot based
            long botbaseaddr = ssnbase.getPointerAsLong(JOAdd.GameObjectStruct.BotdataPtr.value);
            if(botbaseaddr!=0) {
                botbase = new JOPointer(botbaseaddr);
            } else {
                botbase = null;
            }
        } else {
            botbase = null;
        }
    }
   
    public void refresh() {
        GameObjectFactory gof = AppContext.gameObjectFactory;
        
        // reread the position
        Vector3f pos = LocalContext.getContext().GameObject_refresh_objectposition;
        
        JOGame.readObjectPosition(ssnbase, JOAdd.GameObjectStruct.RawZ.value, pos);
        
        // failed reads
        if(pos.x==0 && pos.y == 0 && pos.z==0)
            return;
        
        position.set(pos);
        bounds.center.set(pos);
        this.radius = ssnbase.getShort(JOAdd.GameObjectStruct.Radius);
        
        // if bot, read move wnp and ssn
        if(botbase!=null) {
            this.moveWpn = botbase.getInt(JOAdd.BotDataStruct.MoveWpn.value);
            this.moveTarget = botbase.getInt(JOAdd.BotDataStruct.MoveSSN.value);
        }
        // team can change on some objects, so read it on every refresh
        this.team = ssnbase.getByte(JOAdd.GameObjectStruct.Team.value);
        
        // read the renderflags
        renderFlags = ssnbase.getInt(JOAdd.GameObjectStruct.RenderFlags.value);
        if((itemClass==0)&&(renderFlags&JOAdd.RenderFlag.Kill.value)!=0) {
            this.dead = true;
        } else if((itemClass==1)&&(renderFlags&JOAdd.RenderFlag.Destroyed.value)!=0) {
            this.dead = true;
        } else {
            this.dead=false;
        }
        falling = false;
        if((renderFlags&JOAdd.RenderFlag.Jumping.value)!=0) {
            // re-read enderflags, just in case
            falling = true;
        }
        if((itemClass==0)&&(renderFlags&JOAdd.RenderFlag.Shooting.value)!=0) {
            this.shooting = true;
        } else {
            this.shooting = false;
        }
        if((renderFlags&JOAdd.RenderFlag.OnLadder.value)!=0) {
            onladder = true;
        } else {
            onladder = false;
        }
        
        if(itemClass==1) {
            int drvptr = ssnbase.getInt(JOAdd.GameObjectStruct.VehiclesDriver);
            if(drvptr!=0)
                vehiclesDriver = gof.getObjectByBase(drvptr);
            else
                vehiclesDriver = null;
        }
        int lastkptr = ssnbase.getInt(JOAdd.GameObjectStruct.KilledBy);
        if(lastkptr!=0) {
            lastKiller = gof.getObjectByBase(lastkptr);
        } else {
            lastKiller = null;
        }
        int attaptr = ssnbase.getInt(JOAdd.GameObjectStruct.AttachedToPtr);
        if(attaptr!=0) {
            attachedTo = gof.getObjectByBase(attaptr);
        } else {
            attachedTo = null;
        }
        ssnbase.getVector3f(JOAdd.GameObjectStruct.OrgPosZ.value, spawnPosition);
        ftheta = ssnbase.getInt(JOAdd.GameObjectStruct.Theta)/JOAdd.C_GAMEOBJECT_DEGMUL;
        phi=ssnbase.getInt(JOAdd.GameObjectStruct.Phi)/JOAdd.C_GAMEOBJECT_DEGMUL;
        omega=ssnbase.getInt(JOAdd.GameObjectStruct.Omega)/JOAdd.C_GAMEOBJECT_DEGMUL;
        if(ftheta!=0||phi!=0||omega!=0) {
            ftheta = 360-ftheta+90;
            while(ftheta>=360) ftheta-=360;
        }

    }

    public boolean shouldbeTracked() {
        return !dead && attachedTo == null && !isInAir() && !isOnLadder();
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
        setPosition();
    }
    
    public void setPosition() {
        JOGame.writeObjectPosition(ssnbase, JOAdd.GameObjectStruct.RawZ.value, position);
    }

    public void setSpawnPosition(Vector3f position) {
        this.spawnPosition.set(position);
        setSpawnPosition();
    }
    
    public void setSpawnPosition() {
        JOGame.writeObjectPosition(ssnbase, JOAdd.GameObjectStruct.OrgPosZ.value, position);
    }
    
    public void setTeam(int team) {
        this.team = team;
        ssnbase.setByte(JOAdd.GameObjectStruct.Team.value, (byte) team);
    }
    
    public void setMoveTarget(int movetarget, int movewpn) {
        if(botbase!=null) {
            moveTarget = movetarget;
            moveWpn = movewpn;
            botbase.setInt(JOAdd.BotDataStruct.MoveSSN.value, this.moveTarget);
            botbase.setInt(JOAdd.BotDataStruct.MoveWpn.value, this.moveWpn);
        }
    }

    public boolean isPlayer() {
        return plbase!=null;
    }
    
    public boolean isBot() {
        return itemClass==0 && plbase==null;
    }
    
    public boolean isVehicle() {
        return itemClass==0 && botbase!=null;
    }
    
    public boolean hasRenderFlag(JOAdd.RenderFlag flag) {
        return (renderFlags&flag.value)!=0;
    }
    
    public void setRenderFlag(JOAdd.RenderFlag flag) {
        // read render flags as it can change vey quickly
        renderFlags = ssnbase.getInt(JOAdd.GameObjectStruct.RenderFlags.value);
        renderFlags = (renderFlags|flag.value);
        ssnbase.setInt(JOAdd.GameObjectStruct.RenderFlags, renderFlags);
    }
    
    public void clearRenderFlag(JOAdd.RenderFlag flag) {
        renderFlags = ssnbase.getInt(JOAdd.GameObjectStruct.RenderFlags.value);
        renderFlags = (renderFlags&(~flag.value));
        ssnbase.setInt(JOAdd.GameObjectStruct.RenderFlags, renderFlags);
    }
    
    /**
     * Detaches bot from vehicle or emplaced weapon
     */
    public void detachBot() {
        if(isBot()) {
            setMoveTarget(0,0);
            clearRenderFlag(JOAdd.RenderFlag.Sit);
            ssnbase.setInt(JOAdd.GameObjectStruct.AttachedToPtr, 0);
        }
    }
    
    public void setStop() {
        setMoveTarget(0,0);
    }
    
    public boolean isAttached() {
        return attachedTo!=null;
    }
    
    public boolean isInAir() {
        return falling;
    }
    
    public boolean isOnLadder() {
        return onladder;
    }
    
    /**
     * Set a bot blind and not agro
     * @param distance
     */
    public void setBotVision(short attdistance, short minvis, short maxvis) {
        if(botbase==null)
            throw new JbException("setBotBlind called on a non-bot");
        botbase.setShort(JOAdd.BotDataStruct.AttDis, attdistance);
        botbase.setShort(JOAdd.BotDataStruct.MinVis, minvis); //(short)0);
        botbase.setShort(JOAdd.BotDataStruct.MaxVis, maxvis);
    }    
    
    protected void setRunning() {
        if(botbase==null)
            throw new JbException("setBotBlind called on a non-bot");
        botbase.setInt(JOAdd.BotDataStruct.Posture, 2);
    }
    
    // set waypoint (marker's) radius
    public void setRadius(short radius) {
        ssnbase.setShort(JOAdd.GameObjectStruct.Radius, radius);
    }
    
    public void setDead() {
        if(isVehicle()) {
            this.setRenderFlag(JOAdd.RenderFlag.Destroyed);
        } else if(isBot()) {
            this.setRenderFlag(JOAdd.RenderFlag.Kill);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        
        sb.append("[BASE=" ); sb.append(Long.toHexString(ssnbase.get())); sb.append("]");
        if(plbase!=null){
            sb.append("[PLBASE="); sb.append(Long.toHexString(plbase.get())); sb.append("]");
            if(host==true) {
                sb.append("[HOST="); sb.append(this.host); sb.append("]");
            }
        }

        sb.append("[SSN=");sb.append(SSN); sb.append("]");
        if(name!=null){
            sb.append("[NAME=");sb.append(name);sb.append("]");
        }
        if(typeId!=0) {
            sb.append("[TYPEID=");sb.append(typeId);sb.append("]");
        }
        sb.append("[CAT="); sb.append(itemClass); sb.append("]");
        if(team!=0) {
            sb.append("[TEAM="); sb.append(team); sb.append("]");
        }
        if(dead) {
            sb.append("[ISDEAD="); sb.append(dead); sb.append("]");
        }
        
        if(plbase!=null){
            /*
                os=os.concat("[EXP="+getExperience()+"]");
                os=os.concat("[KILL="+getKills()+"]");
                os=os.concat("[DEATH="+getDeaths()+"]");
            */
        }
        /*
        long idle=playerIdle();
        if(idle!=0){
            os+="[IDLE="+String.valueOf(idle)+"]";
        }
        */

        if(botbase!=null) {
            sb.append("[BOTBASE="); sb.append(Long.toHexString(botbase.get())); sb.append("]");
            if(this.moveWpn!=0) {
                sb.append("[MOVEWPN="); sb.append(this.moveWpn); sb.append("]");
                sb.append("[MOVETARGET="); sb.append(this.moveTarget); sb.append("]");
            }
        }

        if(vehiclesDriver!=null) {
            sb.append("[DRIVER=").append(vehiclesDriver.SSN).append("]");
        }
        if(attachedTo!=null) {
            sb.append("[ATTACHEDTO=").append(attachedTo.SSN).append("]");
        }
        if(lastKiller!=null) {
            sb.append("[KILLEDBY=").append(lastKiller.SSN).append("]");
        }
        /*
        if(oclas==1){
            // for the LFP's try to get the flag height
            long flagheight=getFlagHeight();
            if(flagheight>0)
            {
                os=os+"[FLAGHEIGHT="+String.valueOf(flagheight)+"]";
            }
        }
         */
            sb.append("[POS="); 
                sb.append(position.x); sb.append(","); 
                sb.append(position.y); sb.append(","); 
                sb.append(position.z); 
            sb.append("]");
            sb.append("[SPAWNPOS="); 
                sb.append(spawnPosition.x); sb.append(","); 
                sb.append(spawnPosition.y); sb.append(","); 
                sb.append(spawnPosition.z); 
            sb.append("]");
        // headings
        if(ftheta!=0||phi!=0||omega!=0){
            sb.append("[HEAD=");
            if(ftheta!=0){
                sb.append("th");sb.append(ftheta);
            }
            if(phi!=0){
                sb.append("ph");sb.append(phi);
            }
            if(omega!=0) {
                sb.append("om");sb.append(omega);
            }
            sb.append("]");
        }
        if(this.isInAir()) {
            sb.append("[INAIR]");
        }
            /*
        int spd=getSpeed();
        if(spd!=0)
        {
            os+="[SPD="+String.valueOf(spd)+"]";
        }
            */
            /*
        int rds=getMarkerRadius();
        if(rds!=0)
        {
            os+="[RDS="+String.valueOf(rds)+"]";
        }
            */
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + this.SSN;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GameObject other = (GameObject) obj;
        if (this.SSN != other.SSN) {
            return false;
        }
        return true;
    }
    
    
}
