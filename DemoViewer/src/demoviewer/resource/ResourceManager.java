/*
 * ResourceManager.java
 *
 * Created on 2006. január 22., 22:30
 *
 */

package demoviewer.resource;

import com.jme.image.Texture;
import com.jme.renderer.Renderer;
import com.jme.scene.DistanceSwitchModel;
import com.jme.scene.Node;
import com.jme.scene.SceneElement;
import com.jme.scene.Spatial;
import demoviewer.map.GameObject;
import demoviewer.n3di.nl3DiFileLoader3;
import demoviewer.render.ShaderedMesh;
import com.jme.scene.SwitchNode;
import com.jme.scene.lod.DiscreteLodNode;
import com.jme.util.LoggingSystem;
import demoviewer.Config;
import demoviewer.n3di.ModelInfo;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;
import com.jmex.terrain.util.RawHeightMap;
import demoviewer.XMLUtils;
import demoviewer.render.ShaderManager2;
import demoviewer.terrain.TerrainInfo;
import demoviewer.terrain.TerrainUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import nu.xom.Element;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jme.image.Image;
import com.jme.scene.SharedMesh;

/**
 *
 * @author vear
 */
public class ResourceManager {
    
    private Logger _log = LoggingSystem.getLogger();
    private final boolean dodelete=Config.deleteProcessed;
    
    private static ResourceManager instance=null;
    
    ResourceInfo info;
    
    // the store for models
    private ModelStore store=new ModelStore();
    
    // the shader manager
    ShaderManager2 sm=new ShaderManager2();
    
    // TODO load up the model info into ModelStore
    
    /** Creates a new instance of ResourceManager */
    public ResourceManager() throws nu.xom.ParsingException, java.io.IOException {
        // create and load the resourceinfo
        info=new ResourceInfo();
        // load up the resource propertyes
        info.fromXML();
        if(info.getJodir()!=null) {
            // load up the list of pff files
            info.refreshPFFList();
        }
    }
    
    public static ResourceManager getInstance() {
        if(instance==null) {
            try {
                instance=new ResourceManager();
                LoggingSystem.getLogger().log(Level.INFO, "Resource manager initiated");
            } catch (Exception ex) {
                LoggingSystem.getLogger().log(Level.SEVERE, "Could not init resource manager", ex);
            }
        }
        return instance;
    }
    
    /*
     * Returns the PFF file containing the named resource
     */
    private nPFFFile getContainer(String name) {
        nPFFFile file=null;
        HashMap<String, nPFFFile> list=info.getPfflist();
        // go trough until found
        Iterator<String> li = list.keySet().iterator();
        while(file==null && li.hasNext()) {
            String pn=li.next();
            nPFFFile pf=list.get(pn);
            if(pf.containsFile(name)) {
                file=pf;
            }
        }
        return file;
    }
    
    /*
     * Extracts a file from PFF
     */
    private boolean extractFile(String name) {
        // get in which the said file is
        nPFFFile file=getContainer(name);
        if(file!=null) {
            file.addExtractable(name);
            // extract into extract folder
            file.extractTo(info.getExtract());
        }
        if(new File(info.getExtract()+"/"+name).exists()) return true;
        return false;
    }
    
    private nResource getExtractResource(String name) {
        nResource tr=null;
        if(tr==null) {
            // check if the file is availible in extracted
            if(new File(info.getExtract()+"/"+name).exists()) {
                // if its in the extracted, add it to the resource manager
                tr = new nResource();
                tr.setName(name);
                tr.setLocation(nResource.LOCATION_EXTRACTED);
                info.addResource(tr);
            }
        }
        if(tr==null) {
            // check if it can be got from a PFF
            if(extractFile(name)) {
                // ok, it is now ready in extracted
                tr = new nResource();
                tr.setName(name);
                tr.setLocation(nResource.LOCATION_EXTRACTED);
                info.addResource(tr);
            }
        }
        // last fallback, no location
        if(tr==null) {
            // if its in the extracted, add it to the resource manager
            tr = new nResource();
            tr.setName(name);
            tr.setLocation(nResource.LOCATION_NONE);
            info.addResource(tr);
        }
        return tr;
    }
    
    /* Returns a terrain info object
     * Prepares it if not yet prepared for use
     */
    
    public TerrainInfo getTerrainInfo(String name) {
        TerrainInfo trn=null;
        if(!name.toLowerCase().endsWith(".trn")) {
            name+=".trn";
        }
        nResource tr=info.getResource(name);
        if(tr==null) {
            // check if the xml is in th prepared
            if(new File(info.getPrepared()+"/"+name+".xml").exists()) {
                tr = new nResource();
                tr.setName(name);
                tr.setLocation(nResource.LOCATION_PREPARED);
                info.addResource(tr);
            }
        }
        if(tr==null) {
            tr=getExtractResource(name);
        }
        if(tr!=null) {
            if(tr.getLocation()==nResource.LOCATION_EXTRACTED) {
                // convert it to XML
                nTRNParser prsr=new nTRNParser();
                try {
                    trn=prsr.parseTRN(info.getExtract()+"/"+name);
                } catch(Exception e) {
                    _log.log(Level.WARNING, "Cannot parse TRN file", e);
                    return null;
                }
                // succesfully loaded
                // save it to prepared
                XMLUtils.saveXML(info.getPrepared()+"/"+name+".xml", trn.toXML());
                // change the resource
                tr.setLocation(nResource.LOCATION_PREPARED);
                // delete TRN file
                if(dodelete) {
                    new File(info.getExtract()+"/"+name).delete();
                }
            } else if(tr.getLocation()==nResource.LOCATION_PREPARED) {
                // its not loaded yet, but its in XML format, load it
                Element tre=XMLUtils.loadXML(info.getPrepared()+"/"+name+".xml");
                if(tre!=null)
                {
                    trn=new TerrainInfo();
                    trn.fromXML(tre);
                }
            }
        }
        return trn;
    }
    
    
    /*
     * Retrieves a sectored heighmap resource
     */
    public int[][] getHeightMap(String cptfile) {
        nResource tr=info.getResource(cptfile);
        String hgtfile=cptfile+"."+Config.sectorSize+".hgt";
        String nrmmapfile=cptfile+"."+Config.normalMapSize+"."+Config.normalMapFormat+".nrm";
        if(tr==null) {
            // try to find the raw in the prepared
            if(new File(info.getPrepared()+"/"+hgtfile).exists() && new File(info.getPrepared()+"/"+nrmmapfile).exists()) {
                // if its in prepared, add it to the resource manager
                tr = new nResource();
                tr.setName(cptfile);
                tr.setLocation(nResource.LOCATION_PREPARED);
                info.addResource(tr);
            }
        }
        if(tr==null) {
            // try to find the raw in the extracted
            String raw=cptfile+".raw";
            if(new File(info.getDecomp()+"/"+raw).exists()) {
                // if its in theprepared, add it to the resource manager
                tr = new nResource();
                tr.setName(cptfile);
                tr.setLocation(nResource.LOCATION_DECOMPRESSED);
                info.addResource(tr);
            }
        }
        if(tr==null) {
            tr=this.getExtractResource(cptfile);
        }
        if(tr!=null) {
            // if location in extracted, convert to XML
            if(tr.getLocation()==nResource.LOCATION_EXTRACTED) {
                // this is cpt
                // extract raw from cpt into decompressed
                nCPTParser prsr=new nCPTParser();
                try {
                    prsr.loadBuff(info.getExtract()+"/"+cptfile);
                } catch(Exception e) {
                    _log.log(Level.WARNING, "Cannot load CPT terrain", e);
                    return null;
                }
                if(!prsr.parseCPT()) {
                    _log.log(Level.WARNING,"Cannot extract CPT terrain");
                    return null;
                }
                // save it to decompressed
                try {
                    prsr.saveRAW(info.getDecomp()+"/"+cptfile+".raw");
                } catch(Exception e) {
                    _log.log(Level.WARNING, "Cannot save RAW terrain", e);
                    return null;
                }
                tr.setLocation(nResource.LOCATION_DECOMPRESSED);
                // delete the extracted CPT
                if(dodelete) {
                    new File(info.getExtract()+"/"+cptfile).delete();
                }
            } 
            if(tr.getLocation()==nResource.LOCATION_DECOMPRESSED) {
                // this is extracted raw file, load it
                String raw=info.getDecomp()+"/"+cptfile+".raw";
                RawHeightMap heightMap= new RawHeightMap(raw, 1024, RawHeightMap.FORMAT_16BITLE, false);
                // create 4 pages, by extracting and rescaling heightmaps
                int[][] rawh=new int[4][];
                rawh[0]=TerrainUtils.subSize(TerrainUtils.createHeightSubBlock(heightMap.getHeightMap(), 0, 0, 512),Config.sectorSize);
                rawh[1]=TerrainUtils.subSize(TerrainUtils.createHeightSubBlock(heightMap.getHeightMap(), 0, 512, 512),Config.sectorSize);
                rawh[2]=TerrainUtils.subSize(TerrainUtils.createHeightSubBlock(heightMap.getHeightMap(), 512, 0, 512),Config.sectorSize);
                rawh[3]=TerrainUtils.subSize(TerrainUtils.createHeightSubBlock(heightMap.getHeightMap(), 512, 512, 512),Config.sectorSize);
                // save it to prepared
                try {
                    saveHeightMaps(rawh, hgtfile);
                    // is saved, mark it such
                    tr.setLocation(nResource.LOCATION_PREPARED);
                    if(dodelete) {
                        new File(raw).delete();
                    }
                } catch(Exception e) {
                    _log.log(Level.WARNING, "Cannot save heightmap", e);
                }
                // generate the normal map from the detailed heightmap
                com.jme.image.Image nrmmap=TerrainUtils.getUpRightLeftDownAlteratingNormalMap(heightMap.getHeightMap(), Config.terrainScale, Config.normalMapSize, Config.normalMapFormat);
                if(nrmmap!=null) {
                    try {
                        // save the normal map
                        saveByteBuffer(nrmmap.getData(), info.getPrepared()+"/"+nrmmapfile);
                    } catch (Exception ex) {
                        _log.log(Level.WARNING, "Cannot save normal map", ex);
                    }
                }
                return rawh;
            } else if(tr.getLocation()==nResource.LOCATION_PREPARED) {
                String raw=info.getPrepared()+"/"+cptfile+".hgt";
                try {
                //RawHeightMap heightMap= new RawHeightMap(raw, 257, RawHeightMap.FORMAT_16BITBE, false);
                    return loadHeightMaps(raw,4,129);
                        //heightMap.getHeightMap();
                } catch(Exception e) {
                    _log.log(Level.WARNING, "Cannot load heightmap", e);
                }
            }
        }
        return null;
    }
    /*
     * Generates/retrieves a normal-map from heightmap
     */
    public Texture getNormalMap(String cptfile) {
        nResource tr=info.getResource(cptfile);
        String nrmmapfile=cptfile+"."+Config.normalMapSize+"."+Config.normalMapFormat+".nrm";
        if(tr==null) {
            // try load the heightmap
            getHeightMap(cptfile);
            tr=info.getResource(cptfile);
            if(tr==null || tr.getLocation()!=nResource.LOCATION_PREPARED) {
                // still no success
                return null;
            }
        }
        Texture tn=null;
        try {
            // load the normal map
            ByteBuffer data=loadByteBuffer(info.getPrepared()+"/"+nrmmapfile);
            if(data!=null) {
                com.jme.image.Image img=new com.jme.image.Image();
                img.setData(data);
                img.setHeight(Config.normalMapSize);
                img.setWidth(Config.normalMapSize);
                int tp=Image.RGB888;
                if(Config.normalMapFormat==4) {
                   if(Config.textureCompression==1) {
                        tp=Image.RGBA8888_DXT1A;
                    } else if(Config.textureCompression==2) {
                        tp=Image.RGBA8888_DXT3;
                    } else if(Config.textureCompression==3) {
                        tp=Image.RGBA8888_DXT5;
                    } else 
                        tp=Image.RGBA8888;
                } else if(Config.normalMapFormat==3) {
                    if(Config.textureCompression>0) {
                        tp=Image.RGB888_DXT1;
                    } else tp=Image.RGB888;
                } else if(Config.normalMapFormat==2) {
                    tp=Image.RA88;
                }
                img.setType(tp);
                img.setMipMapSizes(null);
                tn=new Texture();
                tn.setImage(img);
                tn.setCorrection(Texture.CM_PERSPECTIVE);
                tn.setFilter(Texture.FM_LINEAR);
                tn.setMipmapState(Texture.MM_LINEAR);
                tn.setWrap(Texture.WM_WRAP_S_WRAP_T);
                tn.setApply(Texture.AM_REPLACE);
            }
        } catch (Exception ex) {
            _log.log(Level.WARNING, "Cannot load normal map", ex);
            return null;
        }
        return tn;
    }
    
    /*
     * Retrieves a texture resource
     */
    public Texture getTexture(String name, boolean flipped) {
        String dcname;
        if(name.toLowerCase().endsWith(".mdt")) {
            // MDT files are TGA realy
            dcname=name+".tga";
        } else {
            dcname=name;
        }
        Texture t=null;
        nResource tr=info.getResource(name);
        if(tr==null) {
            // check in the decompresses
            // check if it exists
            if(new File(info.getDecomp()+"/"+dcname).exists()) {
                // it is in decompressed
                tr = new nResource();
                tr.setName(name);
                tr.setLocation(nResource.LOCATION_DECOMPRESSED);
                info.addResource(tr);
            }
        }
        
        if(tr==null) {
            tr=this.getExtractResource(name);
        }
        if(tr!=null) {
            if(tr.getLocation()==nResource.LOCATION_EXTRACTED)
            {
                // decompress it
                nBCF1Reader rdr=new nBCF1Reader();
                // also rename MDT into TGA
                if(rdr.decompressTo(info.getExtract()+"/"+name, info.getDecomp()+"/"+dcname)) {
                    // succeeded, delete the extracted
                    tr.setLocation(nResource.LOCATION_DECOMPRESSED);
                    if(dodelete) {
                        new File(info.getExtract()+"/"+name).delete();
                    }
                } else {
                    // failed
                    return null;
                }
            }
            if(tr.getLocation()==nResource.LOCATION_DECOMPRESSED) {
                // fetch the file into the texture
                try {
                    /*
                    if(dcname.toLowerCase().endsWith(".tga")&& flipped) {
                        // get the TGA image trough resource manager
                        // this is only used for loading terrain colormaps, so no big waste in no caching
                        Image imageData = TGALoader.loadImage(new URL("file:" + info.getDecomp()+"/"+dcname).openStream(),flipped, true);
                        int tp=imageData.getType();
                        if(tp==Image.RGB888) {
                            // no alpha
                            if(Config.textureCompression>0) {
                                tp=Image.RGB888_DXT1;
                            }
                        } else {
                            // alpha
                            if(Config.textureCompression==1) {
                                tp=Image.RGBA8888_DXT1A;
                            } else if(Config.textureCompression==2) {
                                tp=Image.RGBA8888_DXT3;
                            } else if(Config.textureCompression==3) {
                                tp=Image.RGBA8888_DXT5;
                            }
                        }
                        imageData.setType(tp);
                        t=new Texture();
                        t.setImage(imageData);
                        t.setMipmapState(Texture.MM_LINEAR_LINEAR);
                        t.setFilter(Texture.FM_LINEAR);
                    } else {
                     */
                        //if(dcname.toLowerCase().endsWith(".dds"))
                        t=TextureManager.loadTexture(
                                        info.getDecomp()+"/"+dcname,
                                        Texture.MM_LINEAR_LINEAR,
                                        Texture.FM_LINEAR,
                                        -1,
                                        1.0f,
                                        flipped);
                    //}
                    if(t!=null) {
                        _log.log(Level.INFO, "Loaded texture "+info.getDecomp()+"/"+dcname);
                    } else {
                        _log.log(Level.WARNING, "Could not load texture file"+info.getDecomp()+"/"+dcname);
                    }
                } catch(Exception e) {
                    _log.log(Level.WARNING, "Could not load texture file", e);
                    return null;
                }
            }
        }
        if(tr==null)
        {
            _log.log(Level.WARNING, "Texture "+name+" not found.");
        }
        return t;
    }
    
    /*
     * Returns a 3DI model, it is used in ModelStore, and should not
     * be directly added to the scene
     */
    public ArrayList<ShaderedMesh> getModel(String mdlname, ModelInfo mdi) {
        ArrayList<ShaderedMesh> md=null;
        
        nResource tr=info.getResource(mdlname);
        
        if(tr==null) {
            // try to find the model in cached
            if(new File(info.getCached()+"/"+mdlname+".0.jme").exists()) {
                // if its in theprepared, add it to the resource manager
                tr = new nResource();
                tr.setName(mdlname);
                tr.setLocation(nResource.LOCATION_CACHED);
                info.addResource(tr);
            }
        }
        if(tr==null) {
            tr=this.getExtractResource(mdlname);
        }
        if(tr!=null) {
            /*
            if(tr.getLocation()==nResource.LOCATION_CACHED) {
                // this is saved jme file, load each separate lod
                int i=0;
                String fn;
                boolean loaded=false;
                fn=info.getCached()+"/"+mdlname+"."+i+".jme";
                while(new File(fn).exists()) {
                    
                    if(md==null) md = new ArrayList<ShaderedCompositeMesh>();
                    JmeBinaryReader rdr=new JmeBinaryReader();
                    ShaderedCompositeMesh n;
                    try {
                        n = rdr.loadBinaryFormat(new BufferedInputStream(new FileInputStream(fn)));
                        if(n!=null) {
                            md.put(new Integer(i),n);
                            loaded=true;
                        }
                    } catch (Exception ex) {
                        _log.log(Level.WARNING, "Cannot load cached file "+fn, ex);
                    }
                    i++;
                    fn=info.getCached()+"/"+mdlname+"."+i+".jme";
                } 
            }
             */
            // if location in extracted, convert to XML
            if(tr.getLocation()==nResource.LOCATION_EXTRACTED) {
                // load the model from 3DI
                nl3DiFileLoader3 ldr=new nl3DiFileLoader3();
                md = ldr.load(info.getExtract()+"/"+mdlname);
                /*
                // apply some attributes
                for(int i=0;i<md.size();i++) {
                    ShaderedMesh ms=md.get(i);
                    ms.setCullMode(Spatial.CULL_NEVER);
                    // set properies on the mesh
                    ms.setRenderQueueMode(Renderer.QUEUE_SKIP);
                }
                 */
                
                boolean cached=true;
                boolean saved=false;
                /*
                // save it to cache
                JmeBinaryWriter wr=new JmeBinaryWriter();
                for(int i=0;i<md.size();i++) {
                    ShaderedCompositeMesh n=md.get(i);
                    if(n!=null) {
                        String fn=info.getCached()+"/"+mdlname+"."+i+".jme";
                        try {
                            // put it into cache
                            wr.writeScene(n, new BufferedOutputStream(new FileOutputStream(fn)));
                            saved=true;
                        } catch (Exception ex) {
                            _log.log(Level.WARNING, "Could not cache file ", ex);
                            cached=false;
                        }
                    }
                }
                if(saved && cached) {
                    // set status to cached
                    tr.setLocation(nResource.LOCATION_CACHED);
                }
                 
                // delete the original 3di
                if(dodelete) {
                    new File(info.getExtract()+"/"+mdlname).delete();
                }
                 */
            }
        }
        return md;
    }
    
    private ArrayList<ShaderedMesh> getCashedModel(String name, ModelInfo mdi) {
        ArrayList<ShaderedMesh> h=null;
        if(name!=null) {
            h=store.getModel(name);
            if(h==null) {
                h=this.getModel(name, mdi);
                if(h!=null) {
                    store.addModel(name,h);
                }
            }
        }
        return h;
    }
    
    private DistanceSwitchModel createSwitchModel(int lods) {
        // create as many lod distances, as many lod meshes
        DistanceSwitchModel model = new DistanceSwitchModel(lods);
        // TODO substitute 500 with maps fog distance
        float dist=Config.modelSightdistance/(float)lods;
        for(int i=0;i<lods;i++) {
            model.setModelDistance(i, i*dist, (i+1)*dist);
        }
        return model;
    }
    
    private ArrayList<ShaderedMesh> sortByVerticesCount(ArrayList<ShaderedMesh> model) {
        ArrayList<ShaderedMesh> sort=new ArrayList<ShaderedMesh>(model.size());
        for(int i=0;i<model.size();i++) {
            ShaderedMesh m=model.get(i);
            boolean found=false;
            for(int j=0;j<sort.size() && !found;j++) {
                ShaderedMesh m1=sort.get(j);
                if(m.getTotalVertices()>m1.getTotalVertices()) {
                    // insert it here
                    found=true;
                    sort.add(i, m);
                }
            }
            if(!found) {
                sort.add(m);
            }
        }
        while(sort.size()>Config.modelLods && sort.size()>1 ) {
            sort.remove(0);
        }
        return sort;
    }
    
    private SharedMesh[][] createModelArray(ArrayList<ShaderedMesh> model, ArrayList<ShaderedMesh> husk) {
        SharedMesh[][] models=new SharedMesh[2][];
        // process the models
        //models[0]=new SharedMesh[model.size()];
        ArrayList<ShaderedMesh> sort = sortByVerticesCount(model);
        models[0]=new SharedMesh[sort.size()];
        for(int i=0;i<sort.size();i++) {
            ShaderedMesh sm=sort.get(i);
            models[0][i]=new SharedMesh(sm.getName(), sm);
        }
        // process husk
        if(husk!=null && husk.size()>0) {
            sort = sortByVerticesCount(husk);
            models[1]=new SharedMesh[sort.size()];
            
            for(int i=0;i<sort.size();i++) {
                ShaderedMesh sm=sort.get(i);
                SharedMesh shm=new SharedMesh(sm.getName(), sm);
                models[1][i]=shm;
                shm.setCullMode(SceneElement.CULL_NEVER);
                
                for(int j=0;j<shm.getBatchCount();j++) {
                    shm.getBatch(j).setCullMode(sm.getBatch(j).getCullMode());
                    shm.getBatch(j).setRenderQueueMode(sm.getBatch(j).getRenderQueueMode());
                }
            }
        }
        return models;
    }
    
    private DiscreteLodNode createShares(String id, ArrayList<ShaderedMesh> children) {
        
        // order the lods in their triangle count order
        HashMap<Integer,ShaderedMesh> lods=new HashMap<Integer,ShaderedMesh>();
        Iterator<ShaderedMesh> li=children.iterator();
        while(li.hasNext()) {
            ShaderedMesh m=li.next();
            int tc=m.getTotalVertices();
            lods.put(new Integer(-tc), m);
        }
        // sort the models by inverse number of triangles
        // so that model with most triangles comes first
        ArrayList<Integer> keys=new ArrayList<Integer>();
        keys.addAll(lods.keySet());
        Collections.sort(keys);
        while(keys.size()>Config.modelLods && keys.size()>1 ) {
            keys.remove(0);
        }
        
        DiscreteLodNode dh=new DiscreteLodNode("Obj"+id, createSwitchModel(keys.size()));
        dh.setCullMode(Spatial.CULL_NEVER);
        // get children in order, and attach them to parent
        Iterator<Integer> it=keys.iterator();
        while(it.hasNext()) {
            Integer k=it.next();
            ShaderedMesh c=lods.get(k);
            SharedMesh sh=new SharedMesh("Obj"+id+"l"+k, c);
            sh.setCullMode(Spatial.CULL_NEVER);
            sh.setRenderQueueMode(Renderer.QUEUE_SKIP);
            dh.attachChild(sh);
        }
        return dh;
    }
    
    /*
     * Returns an objects model, ready to be inserted into the scene
     */
    
    public Node getObjectInstance(int id, int type_id, GameObject go) {
        Node o=null;
        // get model info for the requested type
        ModelInfo mdi=store.getModelInfo(type_id);
        if(mdi==null) {
            _log.warning("No information on type_id "+type_id);
        } else {
            if(mdi.getModelName()==null) {
                // not graphic object
                // TODO handle sounds
                return null;
            }
            // get the model from the store
            ArrayList<ShaderedMesh> m=getCashedModel(mdi.getModelName()+".3di", mdi);
            if(m!=null) {
                // if it has a husk, load that too
                ArrayList<ShaderedMesh> h=null;
                if(mdi.getHusk()!=null) {
                    h=getCashedModel(mdi.getHusk()+".3di", mdi);
                }
                if(go==null) {
                    // for anything other create shared nodes
                    DiscreteLodNode dl=createShares(""+id, m);

                    // if there is a husk create two nodes
                    if(h!=null) {
                        o = new SwitchNode("Obj"+id);
                        o.attachChild(dl);
                        o.setRenderQueueMode(Renderer.QUEUE_SKIP);
                        o.setCullMode(Spatial.CULL_NEVER);
                        ((SwitchNode)o).setActiveChild(0);
                        // create the husk node

                        DiscreteLodNode dh=createShares(id+"h", h);
                        o.attachChild(dh);
                    } else {
                        // no husk, simple
                        o=dl;
                    }
                } else {
                    // create the array of models with lods
                    SharedMesh[][] models=createModelArray(m, h);
                    //go.setModelInfo(mdi);
                    go.setModels(models);
                }
            }
        }
        return o;
    }
    
    public ModelStore getModelStore() {
        return store;
    }
    
    private ArrayList<String> getDirFiles(String folder, String end) {
        ArrayList<String> files=new ArrayList<String>();
        File dir=new File(folder);
        String [] drl=dir.list();
        if(drl!=null) {
            end=end.toLowerCase();
            for(int i=0;i<drl.length;i++) {
                if(drl[i].toLowerCase().endsWith(end)) {
                    files.add(drl[i]);
                }
            }
        }
        return files;
    }
    
    public void parseItemsDef() {
      nItemsDefParser p=new nItemsDefParser();
        try {
            // get all files ending in items.def
            ArrayList<String> defs=getDirFiles(info.getDecomp(), "items.def");
            Iterator<String> dfi=defs.iterator();
            while(dfi.hasNext()) {
                p.parseItemsDef(info.getDecomp()+"/"+dfi.next());
            }
        } catch (Exception ex) {
            _log.log(Level.SEVERE, "Could not parse items.def", ex);
        }
    }
    
    public ShaderManager2 getShaderManager() {
        return sm;
    }
    
    public void saveHeightMaps(int[][] array, String name) throws java.io.FileNotFoundException,
                                                              java.io.IOException {
        DataOutputStream dos=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)));
        for(int i=0;i<array.length;i++) {
            int[] a=array[i];
            for(int j=0;j<a.length;j++) {
                dos.writeShort(a[j]);
            }
        }
        dos.close();
    }

    public int[][] loadHeightMaps(String name, int count, int size) throws FileNotFoundException, IOException {
        DataInputStream dis=new DataInputStream(new BufferedInputStream(new FileInputStream(name)));
        int[][] hm=new int[count][];
        for(int i=0;i<count;i++) {
            hm[i]=new int[size*size];
            for(int j=0;j<size*size;j++) {
                hm[i][j]=dis.readShort();
            }
        }
        dis.close();
        return hm;
    }
    
    /*
     * Saves a byte buffer to the given file
     */
    private void saveByteBuffer(ByteBuffer byteBuffer, String name) throws FileNotFoundException, IOException {
        FileChannel fc=(new FileOutputStream(name)).getChannel();
        fc.truncate(0);
        fc.write(byteBuffer);
        fc.close();
    }
    
    /*
     * Loads a byte buffer from a given file
     */
    private ByteBuffer loadByteBuffer(String name) throws FileNotFoundException, IOException {
        FileChannel fc=(new FileInputStream(name)).getChannel();
        ByteBuffer bb=BufferUtils.createByteBuffer((ByteBuffer)null, (int)fc.size());
        while(fc.position()<fc.size())
            fc.read(bb);
        return bb;
    }
}
