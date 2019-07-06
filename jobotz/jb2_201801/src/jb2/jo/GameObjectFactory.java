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

import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.ent.BotEntity;
import jb2.ent.EmplacementEntity;
import jb2.ent.Entity;
import jb2.ent.PlayerEntity;
import jb2.ent.StaticObject;
import jb2.ent.VIPNodeEntity;
import jb2.ent.VehicleEntity;
import jb2.ent.Waypoint;
import jb2.math.BoundingBox;
import jb2.util.Context;
import jb2.util.FastList;
import jb2.util.IntIntMap;
import jb2.util.IntMap;
import jb2.util.JbException;
import jb2.util.LocalContext;

/**
 *
 * @author vear
 */
public class GameObjectFactory {

    protected static final Logger log = Logger.getLogger(GameObjectFactory.class.getName());

    public enum ItemClass {
        Person(0, 0),
        Vehicle(1, 1),
        Static(2, 2),
        Marker(3, 3),
        Other(4, 4);
        public final int index;
        public final int classtable;

        ItemClass(int idx, int ctbl) {
            index = idx;
            classtable = ctbl;
        }
    }
    

    // various categories: 0-dedicated host, 1-player, 2-bots, 3-vehicles, 4-static, 5-marker, 6-other
    // IntMap contains objects according to their SSN
    
    protected IntMap gameObjects = new IntMap(512);
    //protected IntMap oldObjects = new IntMap(512);


    protected IntMap[] entities = new IntMap[12];
    protected FastList<GameObject> loggedOut = new FastList();
    protected FastList<GameObject> loggedIn = new FastList();
    protected int serverCapacity;
    protected IntIntMap playerAddrs;

    protected IntMap players;
    protected IntMap oldPlayers;

    protected JOPointer classbase = new JOPointer(0);
    // the game objects according to address
    protected IntMap objectAddrs = new IntMap(512);
    // the game objects according to SSN
    //protected IntMap gameObjectsAll = new IntMap(512);

    public GameObjectFactory() {
        for(int i=0; i< TypeMap.numFolders; i++) {
            entities[i] = new IntMap(64);
        }
            
    }

    public void baseRefresh() {

        //IntMap old = oldObjects;
        //oldObjects = gameObjects;
        //gameObjects = old;
        gameObjects.clear();

        // refresh players
        refreshPlayerClass();
        // refresh other classes
        for (int cl = 0; cl <= 4; cl++) {
            refreshClass(cl);
        }
        FastList<GameObject> gameObjectList = new FastList();
        gameObjects.getValues(gameObjectList);
        // extract min,max coordinates so that we have it for constructing the morton tree
        for (int objn = 0; objn < gameObjectList.size(); objn++) {
            GameObject go = gameObjectList.get(objn);
            go.refresh();
            // no need to pass entities from here to the NavMap
            // the new NavGrid will get the list of ents as it needs
            //AppContext.navMap.checkMapRanges(go.position);
        }
    }
    

    public void refreshEntities(boolean firstTime) {
        
        //IntMap old = oldObjects;
        //oldObjects = gameObjects;
        //gameObjects = old;
        //gameObjects.clear();

        refreshPlayerClass();
        for(int cl=0; cl<=4; cl++) {
            refreshClass(cl);
        }

        Context ctx = LocalContext.getContext();
        // get thread local temp arra
        FastList<GameObject> gameObjectList = ctx.GameObjectFactory_refreshEntities_gameObjectList;
        gameObjectList.clear();
        
        // refresh all dynamic objects in entity classes
        for (int fol=0; fol < TypeMap.numFolders; fol++) {
            if (fol == TypeMap.TypeFolder.Host.ordinal()) {
                continue;
            }
            if (!firstTime) {
                if (fol == TypeMap.TypeFolder.Static.ordinal()) {
                    // no need to update static objects each cycle
                    continue;
                }
                if (fol == TypeMap.TypeFolder.Sound.ordinal()
                        || fol == TypeMap.TypeFolder.Effect.ordinal()) {
                    // no need to add sounds and effects to the map
                    // TODO: skip other types too
                    continue;
                }
            }

            IntMap vals = entities[fol];
            vals.getValues(gameObjectList);
            for (int objn = 0; objn < gameObjectList.size(); objn++) {
                GameObject go = gameObjectList.get(objn);
                go.refresh();
                //AppContext.navMap.checkAndAdd(go);
                if(go.typeFolder!=null) {
                    Entity ent = (Entity) go;
                    AppContext.mapOctreeEntities.add(ent);
                }
            }
        }
    }

    public void refreshPlayerClass() {

        int clnum = 0;
        // go trough all class 0 objects
        classbase.set(JOAdd.Adress.ClassTableBase.value.get() + (long) (clnum * 16));
        long ssnbase = classbase.getPointerAsLong(0);
        int strulen = classbase.getInt(4);
        int items = classbase.getInt(8);

        if (playerAddrs == null) {
            // get server capacity
            serverCapacity = JOAdd.Adress.PlayerDataMaxPlayers.value.getInt(0);
            playerAddrs = new IntIntMap(serverCapacity);
            players = new IntMap(serverCapacity);
            oldPlayers = new IntMap(serverCapacity);
        } else {
            playerAddrs.clear();
        }

        // read pointer to players structure
        int playersptr = JOAdd.Adress.PlayerdataPlayersPtr.value.getInt(0);
        JOPointer playersit = new JOPointer(0);

        for (int i = 0; i < serverCapacity; i++) {
            int pladdr = playersptr + (i * JOAdd.PlayerDataStruct.Strulen.value);
            playersit.set(pladdr);
            int ssnptr = playersit.getInt(JOAdd.PlayerDataStruct.GameObjectPtr.value);
            if (ssnptr == 0) {
                continue;
            }
            playerAddrs.put(ssnptr, pladdr);
        }
        // clear out current list of players
        IntMap tmp = oldPlayers;
        oldPlayers = players;
        players = tmp;

        loggedIn.clear();

        // rebuild a new hashmap with objects from old
        // or new entries from game
        JOPointer ssnptr = new JOPointer(0);

        for (int i = 0; i < items; i++) {
            ssnptr.set(ssnbase + (i * strulen));
            long playerptr = 0;
            playerptr = playerAddrs.get((int) ssnptr.get());
            // skip non-players
            if(playerptr==0)
                continue;

            int SSN = ssnptr.getInt(JOAdd.GameObjectStruct.SSN.value);
            if (SSN == 0) {
                // this is probably a logged out player or a norespawn bot
                GameObject go = (GameObject) objectAddrs.remove((int) ssnptr.get());
                if (go != null) {
                    gameObjects.remove(go.SSN);
                    log.info("Object deallocated " + go.name);
                    // remove it from the tree too
                    if (go.typeFolder != null) {
                        AppContext.mapOctreeEntities.remove((Entity) go);
                    }
                    // is its an entity, remove from entities
                    if (go.typeFolder != null) {
                        entities[go.typeFolder.ordinal()].remove(go.SSN);
                    }
                }
                //TODO: if there was tracking for this object, cancel it
                // TODO: remove from entities too
                // skip this item
                continue;
            }
            boolean host = false;

            GameObject go = (GameObject) gameObjects.get(SSN);
            if (go != null) {
                if (go.plbase != null
                        && go.plbase.get() != playerptr) {
                    gameObjects.remove(SSN);
                    log.severe("Found player with same SSN but different playerbase");
                    // TODO: if there was some tracking/learning attached to old player, discard it
                    // same SSN number, but different playerbase, wtf?
                    go = null;
                }
            } else {
                TypeMap.TypeFolder type = this.getTypeAt(clnum, ssnptr);
                if (type != null) {
                    switch (type) {
                        case Host: {
                            // dedicated host
                            host = true;
                            go = new GameObject(clnum, ssnptr.get(), playerptr, host);
                            log.log(Level.INFO, "{0} is hosting", go.name);
                        }
                        break;
                        case Player: {
                            go = new PlayerEntity(clnum, ssnptr.get(), playerptr);
                            log.log(Level.INFO, "{0} logged in", go.name);
                        }
                        break;
                    }
                    // if object was created as an entity, add it to the corresponding map
                    entities[type.ordinal()].put(SSN, go);
                }

                if (go == null) {
                    // default to just gameobjecct, which is ignored
                    go = new GameObject(clnum, ssnptr.get(), playerptr, host);
                }
                objectAddrs.put((int) ssnptr.get(), go);
                gameObjects.put(SSN, go);
            }
            if (go.plbase != null && !go.host) {
                players.put(SSN, go);
                oldPlayers.remove(SSN);
                loggedIn.add(go);
            }
            
        }
        // now read all the remaining gameobjets, these should be the logged out ones
        oldPlayers.getValues(loggedOut);
        if (!loggedOut.isEmpty()) {
            // we have some logged out players
            for (int i = 0; i < loggedOut.size(); i++) {
                GameObject goOut = loggedOut.get(i);
                log.log(Level.INFO, "{0} logged out", goOut.name);
                AppContext.mapOctreeEntities.remove((Entity) goOut);
                entities[goOut.typeFolder.ordinal()].remove(goOut.SSN);
                gameObjects.remove(goOut.SSN);
            }
            loggedOut.clear();
        }
        oldPlayers.clear();
    }

    public void refreshClass(int clnum) {

        // go trough all class 0 objects
        classbase.set(JOAdd.Adress.ClassTableBase.value.get() + (long)(clnum * 16));
        long ssnbase = classbase.getPointerAsLong(0);
        int strulen = classbase.getInt(4);
        int items = classbase.getInt(8);

        // determine real class number based on structure length
        if (clnum > 4) {
            // class not found based on structure length
            log.severe("Object class could not be determined based on strucure length");
            throw new JbException();
        }

        JOPointer ssnptr = new JOPointer(0);

        for (int i = 0; i < items; i++) {
            ssnptr.set(ssnbase + (i * strulen));
            if (clnum == 0) {
                long playerptr = playerAddrs.get((int) ssnptr.get());
                //skip players
                if(playerptr!=0)
                    continue;
            }

            int SSN = ssnptr.getInt(JOAdd.GameObjectStruct.SSN.value);
            if(SSN==0) {
                // this is probably a logged out player or a norespawn bot
                GameObject go = (GameObject) objectAddrs.remove((int) ssnptr.get());
                if(go!=null) {
                    gameObjects.remove(go.SSN);
                    log.log(Level.INFO, "Object deallocated {0}", go.name);
                    // remove it from the tree too
                    if(go.typeFolder!=null) {
                        // is its an entity, remove from entities
                        AppContext.mapOctreeEntities.remove((Entity)go);
                        // and from the octree
                        entities[go.typeFolder.ordinal()].remove(go.SSN);
                    }
                }
                //TODO: if there was tracking for this object, cancel it
                // TODO: remove from entities too
                // skip this item
                continue;
            }
            GameObject go = (GameObject) gameObjects.get(SSN);
            if (go == null) {
                TypeMap.TypeFolder type = this.getTypeAt(clnum, ssnptr);
                if(type!=null) {
                    switch(type) {
                        case AI: {
                            go = new BotEntity(clnum, ssnptr.get());
                            } break;
                        case Vehicle: {
                            go = new VehicleEntity(clnum, ssnptr.get());
                            } break;
                        case LFP: {
                            go = new VIPNodeEntity(clnum, ssnptr.get());
                            } break;
                        case Emplaced: {
                            go = new EmplacementEntity(clnum, ssnptr.get());
                            } break;
                        case Static: {
                            go = new StaticObject(clnum, ssnptr.get());
                            } break;
                        case Waypoint: {
                            go = new Waypoint(clnum, ssnptr.get());
                            } break;
                    }
                    // if object was created as an entity, add it to the corresponding map
                    entities[type.ordinal()].put(SSN, go);
                }
                
                if(go==null) {
                    // default to just gameobjecct, which is ignored
                    go = new GameObject(clnum, ssnptr.get(), 0, false);
                }
                objectAddrs.put((int) ssnptr.get(), go);
                gameObjects.put(SSN, go);
            }
        }
    }

    public FastList<GameObject> getPlayers() {
        return players.getValues(loggedIn);
    }

    public FastList<GameObject> getObjects() {
        return gameObjects.getValues(null);
    }
    
    public FastList<GameObject> getObjects(FastList<GameObject> store) {
        return gameObjects.getValues(store);
    }

    public FastList getEntities(TypeMap.TypeFolder type, FastList store) {
        IntMap entmap = entities[type.ordinal()];
        return entmap.getValues(store);
    }
    
    public FastList<BotEntity> getBots(FastList<GameObject> store) {
        return getEntities(TypeMap.TypeFolder.AI, store);
    }
    
    public FastList<VehicleEntity> getVehicles(FastList<GameObject> store) {
        return getEntities(TypeMap.TypeFolder.Vehicle, store);
    }
    

    public FastList<VIPNodeEntity> getLFP(FastList<VIPNodeEntity> store) {
        return getEntities(TypeMap.TypeFolder.LFP, store);
    }

    /**
     * Removes an entity from being managed by the addon, it will be ignored afterwards
     * @param type
     * @param SSN 
     */
    public void removeEntity(Entity ent) {
        IntMap entmap = entities[ent.typeFolder.ordinal()];
        entmap.remove(ent.SSN);
        // remove it from the tree too
        AppContext.mapOctreeEntities.remove(ent);
    }
    
    public void removeGameObject(GameObject go) {
        if(go.typeFolder!=null)
            removeEntity((Entity) go);
        // TODO: no other thing to remove
    }

    public GameObject getObjectBySSN(int SSN) {
        return (GameObject) gameObjects.get(SSN);
    }
    
    public GameObject getObjectByBase(int base) {
        return (GameObject) objectAddrs.get(base);
    }
    
    public TypeMap.TypeFolder getTypeAt(int clnum, JOPointer ssnptr) {
        int SSN = ssnptr.getInt(JOAdd.GameObjectStruct.SSN.value);
        if(SSN==0) {
            // deallocated object
            return null;
        }
        
        int team = ssnptr.getByte(JOAdd.GameObjectStruct.Team.value);
        // determine if it has an LFP group
        int lfpGroup = ssnptr.getByte(JOAdd.GameObjectStruct.LFPGroup.value);
            
        if(clnum==0) {
            long playerptr = playerAddrs.get((int) ssnptr.get());
            if(playerptr!=0) {
                if (SSN == 10000) {
                    // read the player slot
                    int slot = JOGame.readInt(playerptr, JOAdd.PlayerDataStruct.Slot.value);
                    if(slot==0) {
                        // this is the host
                        return TypeMap.TypeFolder.Host;
                    }
                }
                return TypeMap.TypeFolder.Player;
            }
            if(team==1 || team== 2) {
                // we don't need civilians
                return TypeMap.TypeFolder.AI;
            }
            return null;
        }
        // we must determine the type id
        int typeid = JOGame.getTypeId(ssnptr);
        TypeMap.TypeId type =TypeMap.getType(typeid);
        TypeMap.TypeFolder typeFolder = null;
        if(type!=null) {
            typeFolder = type.folder;
        }
        if(clnum==1) {
            // either vehicle or emplaced
            long botbaseaddr = ssnptr.getPointerAsLong(JOAdd.GameObjectStruct.BotdataPtr.value);
            if(botbaseaddr!=0) {
                // vehicle
                return TypeMap.TypeFolder.Vehicle;
            }
            if(lfpGroup!=0) {
                return TypeMap.TypeFolder.LFP;
            }
            if(typeFolder == TypeMap.TypeFolder.Emplaced) {
                // put it into emplaced
                return TypeMap.TypeFolder.Emplaced;
            }
            return TypeMap.TypeFolder.Static;
        }
        
        if(clnum==2) {
            // put everything else that is needed in static, everything not needed, just GameObject
            return TypeMap.TypeFolder.Static;
        }

        if(clnum==3) {
            if(typeFolder==TypeMap.TypeFolder.Marker) {
                // spawn points, set as VIP
                return TypeMap.TypeFolder.LFP;
            }
            if(typeFolder==TypeMap.TypeFolder.Waypoint) {
                // create it as waypoint
                return TypeMap.TypeFolder.Waypoint;
            }
        }
        return null;
    }
    
    public FastList<Entity> getContained(BoundingBox bb, FastList<Entity> store) {
        return AppContext.mapOctreeEntities.getEntities(bb, store);
    }
    
    public FastList<Entity> getContained2d(BoundingBox bb, FastList<Entity> store) {
        return AppContext.mapOctreeEntities.getEntities2d(bb, store);
    }
    
}
