/*
 * D3iFileLoader.java
 *
 * Created on 2006. január 4., 23:05
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package demoviewer.n3di;
import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.lwjgl.LWJGLShaderObjectsState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;
import demoviewer.Config;
import demoviewer.render.ShaderedMesh;
import demoviewer.resource.ResourceManager;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author vear
 */
public class nl3DiFileLoader3_1 {
    
    String filename;
    private static int currenttex=0;
    private static int shadertex=0;
    
    DataInputStream dis=null;
    int skip;
    
    // the lod node
    ArrayList<ShaderedMesh> lods;
    // the geometry data for a mesh
    FloatBuffer fb = null;
    FloatBuffer txb=null;
    FloatBuffer nrb=null;
    FloatBuffer txb2=null;
    // binormal buffer
    FloatBuffer bnb=null;
    // tangent buffer
    FloatBuffer tnb=null;
        
    ArrayList<nl3DiMaterial> matlist;
    
    // textures for all meshes
    HashMap<String,Texture> textures=new HashMap<String,Texture>();
    // the current mesh
    ShaderedMesh mesh;
    // the raw indices of the mesh
    int[] indices;
    // read texcoord1, binormal and tangents too?
    boolean readall=false;
    // create single vertex buffers or separate for each batch?
    boolean singlebuffers=true;
    
    Vector3f refpoint=new Vector3f();
    
    private class Strip {
        int MaterialIndex;//Texture Index??
        int IndexOffset;//Offset of current index, add NumIndex from prev strip to total.
        int NumIndex;
        int NumIndexVertex; //NumIndex/3
        int StripOrList;
        int StartVertex;
        int NumVertex;
        float MinX;
        float MinY;
        float MinZ;
        float MaxX;
        float MaxY;
        float MaxZ;
    }
    
    boolean dolog=false;
    
    /** Creates a new instance of D3iFileLoader */
    public nl3DiFileLoader3_1() {
    }
    
    public void log(String txt) {
        System.out.println(txt);
    }
    
    private String readChars(int len) throws EOFException, IOException
    {
        byte b[]=new byte[len];
        dis.read(b);
        String hdr=new String(b);
        return hdr;
    }
    
    private String readString() throws EOFException, IOException
    {
        String rt="";
        char c = (char)dis.readUnsignedByte();;
        while(c!=0)
        {
            rt+=c;
            c = (char)dis.readUnsignedByte();
        }
 
        return rt;
    }
    
    private int read3ByteLen() throws EOFException, IOException
    {
        return (dis.read()&0xff) |
        ((dis.read()&0xff) << 8) |
        ((dis.read()&0xff) << 16);
    }
    
    private int readInt() throws EOFException, IOException
    {
        return
            (dis.read()&0xff) |
            ((dis.read()&0xff) << 8) |
            ((dis.read()&0xff) << 16) |
            ((dis.read()&0xff) << 24);
    }
    
    private int readShort() throws EOFException, IOException
    {
        return
            (dis.read()&0xff) |
            ((dis.read()&0xff) << 8);
    }
    
    private float readFloat() throws EOFException, IOException
    {
        return Float.intBitsToFloat(readInt());
    }
    
    public ArrayList<ShaderedMesh> load(String filename) {
        try {
            if(dolog) log("Loading "+filename);
            this.filename=filename;
            File fle=new File(filename);
            long length=fle.length();
            if(dolog) log("File size "+String.valueOf(length));
           
            dis=new DataInputStream(new BufferedInputStream(new FileInputStream(fle)));
            
            // should begin with "3DI3"

            String hdr=readChars(4);
            if(!hdr.equals("3DI3")) {
                log("Wrong header "+hdr);
                dis.close();
                return null;
            } else {
                if(dolog) log("Header: "+hdr);
            }
            int vr=dis.readInt();
            if(dolog) log("Version: "+String.valueOf(vr));
            // create the ArrayList to hold nodes
            lods=new ArrayList<ShaderedMesh>();
            
            try {
               while(true) {
                   hdr=readChars(4);
                   int len=read3ByteLen();
                   int doskip=dis.readUnsignedByte();
                   skip=len;
                   if(doskip==128) {
                       skip=0;
                   }
                   if(dolog) log("Block "+hdr+" len "+String.valueOf(len)+" skipcode "+String.valueOf(doskip));
                   
                   // based on block type process the block
                   if(hdr.equals("RMDL")) {
                       handleRMDL();
                   } else if(hdr.equals("VERT")) {
                       handleVERT(len);
                   } else if(hdr.equals("STRP")) {
                       handleSTRP(len);
                   } else if(hdr.equals("INDX")) {
                       handleINDX(len);
                   } else if(hdr.equals("MTRL")) {
                       handleMTRL(len);
                    } else if(hdr.equals("USRP")) {
                       handleUSRP(len);
                   //} else if(hdr.equals("COBJ")) {
                   //    handleCOBJ(len);
                   
                   //} else if(hdr.equals("ROBJ")) {
                   //    handleROBJ(len);
                   }
                   
                   if(skip>0) {
                       dis.skipBytes(skip);
                   }
               }
            } catch(EOFException eo) {
                // this is ok, we reached the end
                dis.close();
                /*
                // postprocess local translations
                if(dolog) log("Size by header"+max.subtract(min));
                if(dolog) log("Size by measure"+mmax.subtract(mmin));
                if(dolog) log("Extent low "+mmin+" high "+mmax);
                // set the offset into models
                for(int i=0;i<lods.size();i++) {
                    lods.get(i).setReferencePoint(offset);
                }
                 */
                return lods;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    // handler methods for file blocks
    
    // new RMDL block
    private void handleRMDL() throws EOFException, IOException {
        // get the model type
        String name=readChars(4);
        mesh=new ShaderedMesh(name);
        skip-=4;
        mesh.setGroundPoint(this.refpoint);
        //log("New model "+lods.size()+" type "+name);
        fb = null;
        txb=null;
        nrb=null;
        txb2=null;
        // binormal buffer
        bnb=null;
        // tangent buffer
        tnb=null;
        indices=null;
    }
    
    private void handleVERT(int lenght) throws EOFException, IOException {
        // skip 12 bytes
        int hdr1=readInt();
        int hdr2=readInt();
        int hdr3=readInt();
        lenght-=12;
        
        if(dolog) log("Header "+hdr1+","+hdr2+","+hdr3);
        
        int verts=lenght/hdr2;
        // check if this lod model is needed for loading
        if(lods.size()>Config.modelLods) {
            boolean found=false;
            for(int i=0;i<lods.size() && !found;i++) {
                ShaderedMesh mesh=lods.get(i);
                if(verts>mesh.getTotalVertices()) {
                    // we need to insert this mesh before
                    if(i==0) {
                        // no need to add this model
                        mesh=null;
                        skip=lenght;
                        return;
                    } else {
                        found=true;
                    }
                }
            }
        }
        
        int vtx=0;
        fb = BufferUtils.createVector3Buffer((lenght/hdr2));
        txb=BufferUtils.createVector2Buffer((lenght/hdr2));
        nrb=BufferUtils.createVector3Buffer((lenght/hdr2));
        txb2=null;
        // binormal buffer
        bnb=null;
        // tangent buffer
        tnb=null;
        if(readall) {
            txb2=BufferUtils.createVector2Buffer((lenght/hdr2));
            if(hdr2>40) {
                bnb=BufferUtils.createVector3Buffer((lenght/hdr2));
                tnb=BufferUtils.createVector3Buffer((lenght/hdr2));
            }
        }
        float x,y,z;
        Vector3f mmin=new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f mmax=new Vector3f(Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY);
        int cnt=0;
        while(lenght>0) {
            // switch hadedness, x and z
            // read vertice
            x=readFloat();
            y=readFloat();
            z=readFloat();
            
            // calculate model ranges
            mmin.x=Math.min(mmin.x,z); mmax.x=Math.max(mmax.x,z);
            mmin.y=Math.min(mmin.y,y); mmax.y=Math.max(mmax.y,y);
            mmin.z=Math.min(mmin.z,x); mmax.z=Math.max(mmax.z,x);

            fb.put(z).put(y).put(x);
            // read normal
            x=readFloat(); /* = BHD.unk1 */
            y=readFloat(); /* = BHD.unk2 */
            z=readFloat(); /* = BHD.unk3 */
            nrb.put(z).put(y).put(x);
            // read texcoord
            x=readFloat(); /* unk4; /* = BHD.unk4 */
            y=readFloat(); /* unk5; /* = BHD.unk5 */
            //System.out.println("Texcoord tu="+tu+" tv="+tv);
            txb.put(x).put(y);
            if(readall) {
                // read texcoord 2
                x=readFloat(); /* = BHD.unk4 */
                y=readFloat(); /* = BHD.unk5 */
                txb2.put(x).put(y);
                if(hdr2>40) {
                    // read binormals
                    x=readFloat();
                    y=readFloat();
                    z=readFloat();
                    bnb.put(z).put(y).put(x);
                    // read tangents
                    x=readFloat();
                    y=readFloat();
                    z=readFloat();
                    tnb.put(z).put(y).put(x);
                    if(hdr2>64) {
                        // dump out unknown data
                        /*
                        String unk="Unknown:";
                        int ln=64;
                        while(ln<hdr2) {
                            unk+=" "+readFloat();
                            ln+=4;
                        }
                        System.out.println(unk);
                         */
                        dis.skipBytes(hdr2-64);
                    }
                }
            } else {
                dis.skipBytes(hdr2-32);
            }
            lenght-=hdr2;
            
            vtx++;
        }
        // put in min and max
        float cx=(mmin.x+mmax.x)/2f;
        float cz=(mmin.z+mmax.z)/2f;
        mmin.x=cx; mmax.x=cx;
        mmax.z=cz; mmax.z=cz;
        mesh.setBottomPoint(mmin);
        mesh.setTopPoint(mmax);
        skip=lenght;
    }
    
    private void handleSTRP(int length) throws EOFException, IOException {
        int vtx=0;
        int hdr1=readInt();
        int hdr2=readInt();
        //log("Strips "+hdr1+" striplen "+hdr2);
        length-=8;
        if(mesh==null) {
            skip=length;
            return;
        }
        // strips collected
        ArrayList [] strips=new ArrayList[matlist.size()];
        
        while(vtx<hdr1) {
            Strip s=new Strip();
            s.MaterialIndex=readInt();//Texture Index??
            s.IndexOffset=readInt();//Offset of current index, add NumIndex from prev strip to total.
            s.NumIndex=readShort();
            s.NumIndexVertex=readShort(); //NumIndex/3
            s.StripOrList=readInt();
            s.StartVertex=readInt();
            s.NumVertex=readInt();
            s.MinX=readFloat();
            s.MinY=readFloat();
            s.MinZ=readFloat();
            s.MaxX=readFloat();
            s.MaxY=readFloat();
            s.MaxZ=readFloat();
            
            if(strips[s.MaterialIndex] == null) {
                strips[s.MaterialIndex] = new ArrayList();
            }
            strips[s.MaterialIndex].add(s);

            if(dolog) log("Indexrange "+String.valueOf(vtx)+" MaterialIndex "+s.MaterialIndex+" IndexOffset "+s.IndexOffset
                    +" NumIndex "+s.NumIndex+" NumIndexVertex "+s.NumIndexVertex+" StripOrList "+s.StripOrList
                    +" StartVertex "+s.StartVertex+" NumVertex "+s.NumVertex+" MinX "+s.MinX
                    +" MinY "+s.MinY+" MinZ "+s.MinZ+" MaxX "+s.MaxX+" MaxY "+s.MaxY+" MaxZ "+s.MaxZ);
            if(hdr2-48>0) {
                // TODO dump out the unknown data
                dis.skipBytes(hdr2-48);
            }
            length-=hdr2;
            vtx++;
        }
        
        mesh.removeBatch(0);
        // create a texturestate to hold textures
        TextureState ts=DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
        ts.setEnabled(true);
        
        // extract states to the mesh if possible, but not the shader state
        RenderState [] common=new RenderState[RenderState.RS_MAX_STATE];
        boolean[] notcommon=new boolean[RenderState.RS_MAX_STATE];
                
        // does all the textures fit into a single texturestate?
        int totaltextures=0;
        boolean allwithshaders=true;
        boolean first=true;
        for(int i=0;i<strips.length;i++) {
            if(strips[i]!=null) {
                nl3DiMaterial mat=matlist.get(i);
                for(int j=0;j<RenderState.RS_MAX_STATE;j++) {
                    if(j!=RenderState.RS_GLSL_SHADER_OBJECTS && !notcommon[j]) {
                        if(first) {
                            common[j]=mat.states[j];
                            first=false;
                        } else if(common[j]!=mat.states[j]) {
                            notcommon[j]=true;
                            common[j]=null;
                        }
                    }
                }
                totaltextures+=mat.texturelist.size();
                if(mat.states[RenderState.RS_GLSL_SHADER_OBJECTS]==null) {
                    // material not using shader, not possible to pack all textures into one
                    allwithshaders=false;
                }
            }
        }
        // copy common renderstate to mesh level
        for(int j=0;j<RenderState.RS_MAX_STATE;j++) {
            if(common[j]!=null) {
                mesh.setRenderState(common[j]);
            }
        }
        
        // can we use a single texturestate?
        boolean onets=false;
        if(allwithshaders && totaltextures < ts.getNumberOfUnits()) {
            onets=true;
            if(shadertex+totaltextures > ts.getNumberOfFragmentUnits()) {
                shadertex=0;
            }
        }
        if(onets) {
            mesh.setRenderState(ts);
        }
        // process the collected strips
        for(int i=0;i<strips.length;i++) {
            if(strips[i]!=null) {
                ArrayList st=strips[i];
                // strips with same material
                // collect the required buffer sizes
                int numverts=0;
                int numind=0;
                for(int j=0;j<st.size();j++) {
                    Strip s=(Strip) st.get(j);
                    numverts+=s.NumVertex;
                    numind+=s.NumIndex;
                }
                if(numverts>0 && numind>0) {
                    // create a new batch for strips with this material
                    TriangleBatch tb=null;
                    tb=new TriangleBatch();
                    mesh.addBatch(tb);
                    
                    nl3DiMaterial mat=matlist.get(i);
                    
                    // copy not common renderstates into the batch
                    for(int rsn=0;rsn<mat.states.length;rsn++) {
                        if(mat.states[rsn]!=null && notcommon[rsn])
                            tb.setRenderState(mat.states[rsn]);
                    }
                    
                    // clone the shader
                    GLSLShaderObjectsState so=null;
                    if(mat.states[RenderState.RS_GLSL_SHADER_OBJECTS]!=null) {
                        so=((LWJGLShaderObjectsState) mat.states[RenderState.RS_GLSL_SHADER_OBJECTS]).createClone();
                        so.setEnabled(true);
                        tb.setRenderState(so);
                    }
                    
                    FloatBuffer vb;
                    FloatBuffer nb=null;
                    FloatBuffer tx0;
                    FloatBuffer tx1;
                    FloatBuffer bn=null;
                    FloatBuffer tn=null;
                    
                    IntBuffer idb;
                    // create the index buffer
                    idb=BufferUtils.createIntBuffer(numind);
                    if(!singlebuffers) {
                        // create buffers as needed
                        vb=BufferUtils.createFloatBuffer(3*numverts);
                        if(mat.usesNormals)
                            nb=BufferUtils.createFloatBuffer(3*numverts);
                        tx0=BufferUtils.createFloatBuffer(2*numverts);
                        tx1=null;
                        if(txb2 != null) tx1=BufferUtils.createFloatBuffer(2*numverts);
                        bn=null;
                        tn=null;
                        if(bnb != null && so!=null) bn=BufferUtils.createFloatBuffer(3*numverts);
                        if(tnb != null && so!=null) tn=BufferUtils.createFloatBuffer(3*numverts);
                    } else {
                        vb=fb;
                        if(mat.usesNormals)
                            nb=nrb;
                        tx0=txb;
                        tx1=txb2;
                        if(so!=null) {
                            bn=bnb;
                            tn=tnb;
                        }
                    }
                    int startvertex=0;
                    for(int j=0;j<st.size();j++) {
                        Strip s=(Strip) st.get(j);
                        if(singlebuffers)
                            startvertex=s.StartVertex;
                        if(!singlebuffers) {
                            // collect buffers
                            fb.limit((s.StartVertex+s.NumVertex)*3);
                            fb.position(s.StartVertex*3);
                            vb.put(fb);
                            if(nb!=null) {
                                nrb.limit((s.StartVertex+s.NumVertex)*3);
                                nrb.position(s.StartVertex*3);
                                nb.put(nrb);
                            }
                            txb.limit((s.StartVertex+s.NumVertex)*2);
                            txb.position(s.StartVertex*2);
                            tx0.put(txb);
                            
                            if(tx1!=null) {
                                txb2.limit((s.StartVertex+s.NumVertex)*2);
                                txb2.position(s.StartVertex*2);
                                tx1.put(txb);
                            }
                            if(bn!=null) {
                                bnb.limit((s.StartVertex+s.NumVertex)*3);
                                bnb.position(s.StartVertex*3);
                                bn.put(bnb);
                            }
                            if(tn!=null) {
                                tnb.limit((s.StartVertex+s.NumVertex)*3);
                                tnb.position(s.StartVertex*3);
                                tn.put(tnb);
                            }
                        }
                        // renumber and put indices
                        for(int idcnt=s.IndexOffset; idcnt<s.IndexOffset+s.NumIndex; idcnt+=3) {
                            // fill indexbuffer reordering vertices for correct handedness
                            idb.put(indices[idcnt]+startvertex).put(indices[idcnt+2]+startvertex).put(indices[idcnt+1]+startvertex);
                        }
                        if(!singlebuffers) {
                            // if not single buffer, 
                            startvertex+=s.NumVertex;
                        }
                    }
                    // set the buffers into the batch
                    vb.limit(vb.capacity());
                    vb.rewind();
                    tb.setVertexBuffer(vb);
                    if(nb!=null) {
                        nb.limit(nb.capacity());
                        nb.rewind();
                        tb.setNormalBuffer(nb);
                    }
                    tx0.limit(tx0.capacity());
                    tx0.rewind();
                    tb.setTextureBuffer(tx0,0);
                    if(tx1!=null) {
                        tx1.limit(tx1.capacity());
                        tx1.rewind();
                        tb.setTextureBuffer(tx1,1);
                    }
                    if(bn!=null && tn!=null) {
                        bn.limit(bn.capacity());
                        bn.rewind();
                        so.setAttributePointer("binormal", 3, false, 0, bn);
                        tn.limit(tn.capacity());
                        tn.rewind();
                        so.setAttributePointer("tangent", 3, false, 0, tn);
                    }
                    idb.rewind();
                    tb.setIndexBuffer(idb);
                    
                    VBOInfo vbo=new VBOInfo(true);
                    vbo.setVBOColorEnabled(false);
                    vbo.setVBOIndexEnabled(true);
                    tb.setVBOInfo(vbo);
                    
                    // prepare the textures for the material
                    // does all the textures fit into the curent texturestate
                    if( !onets 
                     && ( ts==null || mat.texturelist.size()+ts.getNumberOfSetTextures()>ts.getNumberOfUnits() || so==null)) {
                        // a new texturestate is required
                        ts=DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
                        ts.setEnabled(true);
                    }
                    if(so==null) {
                        if(currenttex+mat.texturelist.size()>ts.getNumberOfFixedUnits()) {
                            currenttex=0;
                        }
                    } else {
                        if(shadertex+mat.texturelist.size()>ts.getNumberOfFragmentUnits()) {
                            shadertex=0;
                        }
                    }
                    //if(mat.states[RenderState.RS_TEXTURE]==null) {
                        int settex=0;
                        for(int tx=0;tx<mat.texturelist.size();tx++) {
                            nl3DiMaterial.MaterialTexture mtx=mat.texturelist.get(tx);
                            
                            int u=ts.getNumberOfSetTextures();
                            
                            if(so!=null) {
                                u=Math.max(u,this.shadertex++);
                            }
                            
                            Texture txt=mtx.tex;
                            if(txt==null && mtx.textureName!=null) {
                                txt=loadTexture(mtx.textureName);
                                mtx.tex=txt;
                            }
                            if(txt!=null) {
                                    txt.setWrap(Texture.WM_WRAP_S_WRAP_T);
                                    txt.setCorrection(Texture.CM_PERSPECTIVE);
                                    txt.setFilter(Texture.FM_LINEAR);
                                    txt.setMipmapState(Texture.MM_LINEAR_LINEAR);

                                    if(so==null) {
                                        if(settex==0)
                                            txt.setApply(Texture.AM_REPLACE);
                                        else
                                            txt.setApply(Texture.AM_MODULATE);
                                        /*
                                        txt.setCombineFuncRGB(Texture.ACF_MODULATE);
                                        txt.setCombineSrc0RGB(Texture.ACS_PREVIOUS);
                                        txt.setCombineOp0RGB(Texture.ACO_SRC_COLOR);
                                        txt.setCombineSrc1RGB(Texture.ACS_TEXTURE);
                                        txt.setCombineOp1RGB(Texture.ACO_SRC_COLOR);
                                         */
                                        txt.setCombineScaleRGB(1.0f);
                                    }
                                    textures.put(mtx.textureName, txt);
                                    ts.setTexture(txt,u);
                                    settex++;
                                if(so!=null) {
                                    int txi=(mtx.id&0xff);
                                    String texnu=getTexUniname(txi, mat);
                                    so.getStore().setUniform(texnu, u);
                                }
                            }
                        }
                        if(!onets) {
                            tb.setRenderState(ts);
                            if(so==null) ts=null;
                        }
                    //}
                     
                    // put the batch into mesh
                    //mesh.addBatch(tb);
                }
            }
        }
        if(mesh.getBatchCount()>0) {
            //mesh.setActiveBatch(0);
            //mesh.setBatchRange(0,mesh.getBatchCount()-1); // removed
            mesh.setModelBound(new BoundingSphere());
            mesh.updateModelBound();
            mesh.updateRenderState();
            mesh.setLightCombineMode(LightState.INHERIT);
            mesh.setRenderQueueMode(Renderer.QUEUE_SKIP);
            //mesh.setDefaultColor(null);
            mesh.setDefaultColor(ColorRGBA.white);
            // choose where to put this mesh
            int verts=mesh.getTotalVertices();
            boolean found=false;
            for(int i=0;i<lods.size() && !found;i++) {
                ShaderedMesh mesh=lods.get(i);
                if(verts>mesh.getTotalVertices()) {
                    // we need to insert this mesh before
                    lods.add(i, mesh);
                    found=true;
                }
            }
            if(!found)
                lods.add(mesh);
            if(lods.size()>Config.modelLods) {
                lods.remove(0);
            }
            if(dolog) log("Added mesh with "+mesh.getBatchCount()+" batches");
        } else {
            log("Mesh with empty batches: "+filename);
        }
        //mesh.setDefaultColor(ColorRGBA.green);
        if(length>0) {
            log("Unused "+length);
        }
        skip=length;
    }
    
    private void handleINDX(int length) throws EOFException, IOException
    {
        int hdr1=readInt();
        int hdr2=readInt();
        //log("Header "+hdr1+","+hdr2);
        length-=8;
        if(mesh==null) {
            skip=length;
            return;
        }
        indices=new int[hdr1];
        
        if(hdr2!=2) {
            log("Unknown index record size "+hdr2);
        } else {
            int cnt=0;
            while((cnt<hdr1)&&(length>0)) {
                indices[cnt]=readShort();
                length-=2;
                cnt++;
            }
        }
        if(length>0) {
            log("Unused "+length);
        }
        skip=length;
    }
    
    /*
     * In the 3DI files, TGA files are realy DDS files
     */
    private String getTextureFile(String name) {
        String nf=null;
        if(name.toUpperCase().endsWith(".TGA")) {
            nf=name.substring(0, name.length()-4)+".DDS";
        } else {
            nf=name;
        }
        return nf;
    }
    
    private Texture loadTexture(String name) {
        Texture txt=textures.get(name);
        if(txt==null) {
            txt=ResourceManager.getInstance().getTexture(name,false);
        }
        return txt;
    }
    
    private String getTexUniname(int id, nl3DiMaterial mat) {
        String name=null;
        if(id==mat.basicTexture || id==mat.basicTexture2) {
            name="basicTexture";
        } else if(id==mat.modulateTexture) {
            name="basicTexture2";
        }
        return name;
    }
    
    private void handleMTRL(int length) throws EOFException, IOException {
        int hdr1=readInt();
        int hdr2=readInt();
        //log("MTRL Header "+hdr1+","+hdr2);
        length-=8;
        int cnt=0;
        // materials for strips
        matlist=new ArrayList(hdr1);
        while(cnt<hdr1) {
            String name=readString();
            dis.skipBytes(32-(name.length()+1));
            if(name==null) {
                log("Null material type name");
            }
            // material for mesh
            nl3DiMaterial mat=new nl3DiMaterial();
            mat.name=name;
            matlist.add(mat);
            // texturestate for the mesh
            //TextureState ts;
            //ts=DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
            // get the states/shader for this material
            ResourceManager.getInstance().getShaderManager().preloadShaders(mat);
            GLSLShaderObjectsState so=(GLSLShaderObjectsState) mat.states[RenderState.RS_GLSL_SHADER_OBJECTS];
            boolean exit=false;
            int txs=0;
            int rdb=32;
            while(!exit) {
                int id=readInt();
                String filename=readString();
                filename=getTextureFile(filename);
                // todo dont load mdt's
                if(!filename.equals("")) {
                    if(!filename.toLowerCase().endsWith(".mdt")) {
                        int txi=(id&0xff);
                        // check if the texture is used in the shader
                        String tn=getTexUniname(txi, mat);
                        if( tn!=null && ( so== null || so.getStore().getShaderUniform(tn)!=null) ) {
                            // load the texture
                            nl3DiMaterial.MaterialTexture mtx=new nl3DiMaterial.MaterialTexture();
                            mtx.id=id;
                            mtx.textureName=filename;
                            mat.texturelist.add(mtx);
                            if(dolog) log("Material "+cnt+" rendermode "+name+" texture "+id+" file "+filename);
                        } else {
                            if(dolog) log("Ignored material "+cnt+" rendermode "+name+" texture "+id+" file "+filename);
                        }
                        txs++;
                    } else {
                            if(dolog) log("Ignored material "+cnt+" rendermode "+name+" texture "+id+" file "+filename);
                    }
                } else {
                    exit=true;
                }
                dis.skipBytes(20-(4+1+filename.length()));
                rdb+=20;
            }
            /*
            if(txs>0) {
                ts.setEnabled(true);
                // set the texture state on the material
                v.setRenderState(ts);
            } else {
                if(dolog) log("Material "+cnt+" rendermode "+name);
            }
             */
            dis.skipBytes(hdr2-rdb);
            cnt++;
            length-=hdr2;
        }
        if(length>0) {
            log("Unused "+length);
        }
        skip=length;
    }
    
    /*
    private void handleCOBJ(int length) throws EOFException, IOException {
        int hdr1=readInt();
        int hdr2=readInt();
        length-=8;
        int nl=readInt();
	int NumVertices=readInt();
	int NumTriangles=readInt();
	int NumPolygons=readInt();
	int NumBoundingBox=readInt();
	nl=readInt();
	nl=readInt();
	nl=readInt();
	nl=readInt();
	offset.x=readInt()/65536;
	offset.z=readInt()/65536;
	offset.y=readInt()/65536;
	min.x=readInt()/65536;
	min.z=readInt()/65536;
	min.y=readInt()/65536;
	max.x=readInt()/65536;
	max.z=readInt()/65536;
	max.y=readInt()/65536;
	median.x=readInt()/65536;
	median.z=readInt()/65536;
	median.y=readInt()/65536;
	int BSphereRad=readInt()/65536;
        if(dolog) log("Object: NumVertices "+NumVertices+" NumTriangles "+NumTriangles+" NumPolygons "+NumPolygons+" NumBoundingBox "+NumBoundingBox);
        if(dolog) log("Weights: Offset "+offset);
        if(dolog) log("Low "+min+" High "+max);
        if(dolog) log("Median "+median+" BSphereRad "+BSphereRad);
        length-=22*4;
        skip=length;
    }
     */

    private void handleROBJ(int len) throws EOFException, IOException {
        dis.skipBytes(0x30);
        len-=0x30;
        // skip to vertical offset
        Vector3f off=new Vector3f();
        off.y=readFloat();
        //if(off.y!=0) {
            //if(dolog) 
                log("Mesh offset "+off);
        //}
        mesh.setGroundPoint(off);
        len-=4;
        skip=len;
    }

    private void handleUSRP(int len) throws EOFException, IOException {
        int hdr1=readInt();
        int hdr2=readInt();
        len-=8;
        for(int i=0;i<hdr1;i++) {
            float z=readInt()/65536f;
            float x=readInt()/65536f;
            float y=readInt()/65536f;
            len-=12;
            // is this the ground?
            dis.skipBytes(10);
            len-=10;
            byte uon=dis.readByte();
            len-=1;
            if(uon==1) {
                // this is it
                refpoint.set(x,y,z);
                if(dolog) log("Ground point "+refpoint);
            }
        }
        skip=len;
    }
    
}
