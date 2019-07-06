/*
 * MorphingTerrainIndex.java
 *
 * Created on 2006. március 21., 17:54
 *
 * Stores a number of index buffers. Each instance of this class
 * is associated with a MorphingTerrainBlock size.
 */

package demoviewer.terrain;

import com.jme.math.FastMath;
import com.jme.scene.CompositeMesh;
import com.jme.scene.CompositeMesh.IndexRange;
import com.jme.util.geom.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 *
 * @author vear
 */
public class MorphingTerrainIndex {
    
// all index ranges
   CompositeMesh.IndexRange[] allranges;
   // all index buffers, as strips/fans
   IntBuffer[] allindices;
   // index buffer as triangle list
   //IntBuffer[] trianglebuffers;
   // byte buffer for each vertex to know which of its neigbours lod to use
   // can be shared among instances of same size
   FloatBuffer lodindexarray;
   // the maximum level of vertices still visible (can be shared)
   FloatBuffer levelarray;
   // the compiled array with lodindex, level
   FloatBuffer lodArray;
   
    // the vertex order when reordering
   int[] vertOrder;
   boolean reorderVerts;
   // the number of vertices used for each index range
   // used when separate vertex buffers are used for each index (batch)
   int[] vertQuantities;
   boolean useMultVertBuffers;
           
   // counter for renumbering vertices
   int currentIndex=0;
   
    int size;
    boolean usestrips;
    int maxLod;
    
    /** Creates a new instance of MorphingTerrainIndex */
    public MorphingTerrainIndex(int size, boolean usestrips, boolean reorderVerts) {
        this.size=size;
        this.usestrips=usestrips;
        this.reorderVerts=reorderVerts;
        if(reorderVerts) vertOrder=new int[size*size];
        
        // build triangle list/strip indices
        buildIndices();

    }
    
    /* 
     * Each batch uses separate vertex buffer, sized to hold only
     * vertices needed for that lod
     */
    public void setUseMultipleBuffers(boolean useMultVertBuffers) {
        this.useMultVertBuffers=useMultVertBuffers;
    }
    
    public boolean isUseMultipleBuffers() {
        return this.useMultVertBuffers;
    }
    
    public int getSize() {
        return size;
    }
    
    public boolean isUseStrips() {
        return usestrips;
    }
    
    public boolean isUseSortedVertices() {
        return reorderVerts;
    }
    
    public int[] getVerticesOrder() {
        return vertOrder;
    }
    
    public IntBuffer getIndexBuffer(int number) {
        return allindices[number];
    }
    
    public CompositeMesh.IndexRange getIndexRange(int number) {
        return allranges[number];
    }
    
    public FloatBuffer getMorphLodLevelArray() {
        if(levelarray==null) {
            // build the max lod array
            buildLevelArray();
        }
        return levelarray;
    }
    
    public FloatBuffer getMorphLodIndexArray() {
        if(lodindexarray==null) {
            // build the lod index array (which vertice uses which neighbours lod)
            buildLodIndexArray();
        }
        return lodindexarray;
    }
    /*
     * Returns the compiled (lod index, level) array
     */
    public FloatBuffer getMorpLodArray() {
        if(lodArray==null) {
            // build the two arrays and put the together
            getMorphLodLevelArray();
            getMorphLodIndexArray();
            lodArray=BufferUtils.createVector2Buffer(size*size);
            lodArray.clear();
            lodindexarray.rewind();
            levelarray.rewind();
            for(int i=0;i<size*size;i++) {
                lodArray.put(lodindexarray.get()).put(levelarray.get());
            }
            // delete the two arrays
            levelarray=null;
            lodindexarray=null;
        }
        return lodArray;
    }
    
   private void buildIndices() {
       // the maximum lod number
       maxLod=(int)FastMath.log(size-1,2);
       
       // reordered vertices
       if(reorderVerts) {
           // clear out the vertex order array
           Arrays.fill(vertOrder,-1);
       }
       currentIndex=0;
       // max vertex needed for specific index range when sorted
       if(useMultVertBuffers) {
           vertQuantities=new int[maxLod*16+1];
       }
       
       // the array of indices, 16 for each lod level
       // and 1 for the least detailed
       allindices=new IntBuffer[maxLod*16+1];
       // the array for all ranges
       allranges=new IndexRange[maxLod*16+1];
       
       //trianglebuffers=new IntBuffer[maxLod*16+1];
       
       // flag for strip or fan
       //boolean[] isstrip=new boolean[maxLod*16+1];
       
       //set up the indices for all the lod levels
       for(int i=maxLod;i>=0;i--) {
           // number of cells in this lod
           int cells=(int)Math.pow(2,(maxLod-i));
           // create 16 index buffer versions for each lod, except for
           // maxlod, which dont have versions
           int indextypes=16;
           if(i==maxLod) {
               indextypes=1;
           }
           for(int idt=0;idt<indextypes;idt++) {
               // bitmasked versions of index ranges:
               // bitmask values: 	
               // 1- upper fixed
               // 2- right fixed
               // 4- down fixed
               // 8- left fixed
               // estimate the number of indices for this lod
               // as it would be composed of triangles only
               int ni=cells*cells*2*3*2;
               if(i==maxLod-1) {
                   // at max-1 we need some more
                   ni*=4;
               }
               
               // stepping for choosing vertices
               int step=1<<i;
               // step line size
               int stepsize=step*size;
               // maxcolumn
               int maxcolumn=size-1;
               int maxrow=size-1;
               
               // do the top and bottom rows need special handling
               int mstart=0;
               int mend=maxrow;
               // up fixing needed
               if((idt&1)!=0) {
                   mstart=step;
               }
               // down fixing needed
               if((idt&4)!=0) {
                   mend=maxrow-step;
               }
               // do right and left sides need special handling
               int cstart=0;
               int cend=maxcolumn;
               // right fixing needed
               if((idt&2)!=0) {
                   cend=maxcolumn-step;
               }
               // left fixing needed
               if((idt&8)!=0) {
                   cstart=step;
               }
               
               boolean isstrip=true;
                // check to generate list, strip, or fan
               GeomBuffer gb;
               if(i==maxLod-1) {
                   // at maxlod-1, generate fan
                   FanBuffer idx=new FanBuffer(24,false);
                   gb=idx;
                   // manualy construct the fan
                   isstrip=false;
                   // center, right top
                   idx.put((maxrow/2)*size+(maxcolumn/2), maxcolumn);
                   if((idt&1)==0) {
                       // no top fix, center top
                       idx.put(maxcolumn/2);
                   }
                   // left top
                   idx.put(0);
                   if((idt&8)==0) {
                       // no right fix, center right
                       idx.put((maxrow/2)*size);
                   }
                   // left bottom
                   idx.put(maxrow*size);
                   if((idt&4)==0) {
                       // no bottom fix, center bottom
                       idx.put(maxrow*size+maxcolumn/2);
                   }
                   // right bottom
                   idx.put(maxrow*size+maxcolumn);
                   if((idt&2)==0) {
                       // no right fix, center right
                       idx.put((maxrow/2)*size+maxcolumn);
                   }
                   // finish, right top
                   idx.put(maxcolumn);
               } else {    
               
                   // create the stripbuffer, which helps fixing up
                   // the strip with degenerate triangles
                   StripBuffer idx=new StripBuffer(ni, false);
                   gb=idx;

                   // strip draw order
                   switch(idt) {
                       case 0: {
                           // all to middle
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                       } break;
                       case 1: {
                           // middle start right, top, 
                           topStrip(idx, idt, step, maxcolumn);
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                       } break;
                       case 2: {
                           // middle start left, right
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                       } break;
                       case 3: {
                           // middle leftstart, right, top
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                           topStrip(idx, idt, step, maxcolumn);
                       } break;
                       case 4: {
                           // bottom, middle
                           
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                       } break;
                       case 5: {
                           // bottom, middle, top
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           topStrip(idx, idt, step, maxcolumn);
                       } break;
                       case 6: {
                           // bottom, right, middle rightstart
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                       } break;
                       case 7: {
                           // bottom, right, top, middle leftstart
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                           topStrip(idx, idt, step, maxcolumn);
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                       } break;
                       case 8: {
                           // left, middle leftstart
                           leftStrip(idx, idt, step, maxrow);
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                       } break;
                       case 9: {
                           // top, left, middle leftstart
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                           topStrip(idx, idt, step, maxcolumn);
                           leftStrip(idx, idt, step, maxrow);

                       } break;
                       case 10: {
                           // right, middle rightstart, left
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                           leftStrip(idx, idt, step, maxrow);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                       } break;
                       case 11: {
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           // right, top, left, middle leftstart
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                           topStrip(idx, idt, step, maxcolumn);
                           //idx.put(0, (2*step)*size, (step)*size+step);
                           leftStrip(idx, idt, step, maxrow);
                           //idx.put(maxrow*size+step);
                           
                       } break;
                       case 12: {
                           // left, bottom, middle rightstart
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                           leftStrip(idx, idt, step, maxrow);
                       } break;
                       case 13: {
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 0);
                           // top, left, bottom, middle rightstart
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                           topStrip(idx, idt, step, maxcolumn);
                           leftStrip(idx, idt, step, maxrow);
                       } break;
                       case 14: {
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           // left, bottom, right, middle rightstart
                           leftStrip(idx, idt, step, maxrow);
                           idx.put(maxrow*size);
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);
                           idx.put(maxrow*size+maxcolumn);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);
                           //idx.put(maxrow*size);
                       } break;
                       case 15: {
                           middleStrip(idx, idt, step, mstart, mend, cstart, cend, 1);
                           topStrip(idx, idt, step, maxcolumn);
                           
                           leftStrip(idx, idt, step, maxrow);
                           idx.put(maxrow*size);
                           bottomStrip(idx, idt, step, maxrow, maxcolumn);

                           idx.put(maxrow*size+maxcolumn);
                           rightStrip(idx, idt, step, maxrow, maxcolumn);

                       } break;
                   }
               }
               IntBuffer indbuff;
               if(usestrips) {
                    // put into temp array
                    indbuff=gb.finishAndGetBuffer();
                    //trianglebuffers[i*16+idt]=gb.getTriangleBuffer();
               } else {
                   indbuff=gb.getTriangleBuffer();
                   //trianglebuffers[i*16+idt]=indbuff;
               }
               indbuff.rewind();
               ni=indbuff.limit();
               // if vertex reordering is needed, renumber the index array
               indbuff=renumberIndices(i*16+idt, indbuff);
               // repack it so its capacity is the limit
               allindices[i*16+idt]=BufferUtils.createIntBuffer(ni);
               allindices[i*16+idt].clear();
               allindices[i*16+idt].put(indbuff);
               allindices[i*16+idt].rewind();
               IndexRange id;
               if(usestrips) {
                   if(isstrip)
                        id=CompositeMesh.createTriangleStrip(ni);
                   else
                       id=CompositeMesh.createTriangleFan(ni);
               } else {
                   id=CompositeMesh.createTriangleRange(ni);
               }
               allranges[i*16+idt]=id;
           }
       }
       /*
       // process the index buffers into a single one
       // calculate the total size
       int numIndices=0;
       for(int i=0;i<idxb.length;i++) {
           numIndices+=idxb[i].limit();
       }
       // create the index buffer
       // TODO determine this at end, from StripBuffer info
       
       getTriangleBatch().setIndexBuffer(BufferUtils.createIntBuffer(numIndices));
       IntBuffer indexBuffer=getTriangleBatch().getIndexBuffer();
       indexBuffer.clear();
       // the array of index ranges, 16 for each lod level
       // and 1 for the least detailed
       IndexRange[] ranges=new IndexRange[maxLod*16+1];
       int totaltris=0;
       // put together all the index ranges
       for(int i=0;i<idxb.length;i++) {
               // the length of the range
               int ni=idxb[i].limit();
               if(ni>2) {
                   // create a new indexrange
                   IndexRange id;
                   if(isstrip[i])
                        id=this.createTriangleStrip(ni);
                   else
                       id=this.createTriangleFan(ni);
                   ranges[i]=id;
                   idxb[i].rewind() ;
                   // copy the limited buffer into index buffer
                   indexBuffer.put(idxb[i]);
                   totaltris+=ranges[i].getTriangleQuantityEquivalent();
               }
       }
        
       getTriangleBatch().setTriangleQuantity(totaltris);
        */
   }
   
   private void middleStrip(StripBuffer idx, int idt, int step, int mstart, int mend, int cstart, int cend, int startright) {               
       int left=1;
       boolean skipleft=false;
       boolean skipright=false;
       switch(idt&11) {
           case 1: {
               // top
               left=0;
           } break;
           case 2: {
               // right
               left=0;
               //skipright=true;
               skipleft=true;
           } break;
           case 3: {
               // top+right
               left=1;
               skipleft=true;
           } break;
           case 8: {
               // left
               left=0;
               skipright=true;
           } break;
           case 9: {
               // top+left
               left=0;
               skipleft=true;
           } break;
           case 10: {
               // left+right
               left=0;
               //skipleft=true;
           } break;
           case 11: {
               // top+right+left
               left=1;
               //skipright=true;
               //skipleft=true;
           } break;
           case 12: {
               // left+bottom
               left=0;
               skipright=true;
           } break;
           case 13: {
               // top+left+bottom
               left=0;
               skipleft=true;
           } break;
           case 14: {
               // right+left+bottom
               left=0;
           } break;
           case 15: {
               // top+right+left+bottom
               left=1;
           } break;

       }
       if(mend!=mstart && cend!=cstart) {
           // alterating version
           for(int nrow=mstart;nrow<=mend-step;nrow+=step) {
               if(((nrow-mstart)/step)%2==left) {
                   // odd rows, left to right
                   int cs=cstart-(skipleft?step:0);
                   for(int column=cs;column<=cend;column+=step*2) {
                        if(column==cs) {
                           // first top
                            // bottom, middle top, middle bottom, middle top
                            // right bottom, right tope
                            if(skipleft) 
                                idx.put(nrow*size+column+step,nrow*size+column+step,nrow*size+column+step);
                            else
                                idx.put(nrow*size+column, (nrow+step)*size+column, nrow*size+column+step);
                            idx.put((nrow+step)*size+column+step);
                            if(cend>=cs+step) {
                                idx.put(nrow*size+column+step);
                                if(column<=cend-step*2) {
                                    idx.put((nrow+step)*size+column+step*2);
                                    idx.put(nrow*size+column+step*2);
                                }
                            }
                        } else if(column==cend) {
                            // right edge
                            // 
                            //if(cend>cstart+step) {
                            //idx.put(nrow*size+column);
                            //}
                        } else {
                            // normal cell
                            // 
                            // bottom, middle top, middle bottom, middle top
                            // right bottom, right top
                            idx.put((nrow+step)*size+column).put(nrow*size+column+step);
                            idx.put((nrow+step)*size+column+step);
                            if(column+step*2<=cend) {
                                idx.put(nrow*size+column+step).put((nrow+step)*size+column+step*2);

                                idx.put(nrow*size+column+step*2);
                            }
                        }
                   }
                   if(idt==0 || idt==1 || idt==2 || idt==3 || idt==4 
                   || idt==5 || idt==6 || idt==7 || idt==9 || idt==10 
                   || idt==11|| idt==13|| idt==14|| idt==15) {
                       idx.repeat();
                   }
                   /*
                   if(((cend-cstart)/step*2)%2==0)
                    idx.repeat();
                    */
              } else {
                   int ce=cend+(skipright?step:0);
                   // even rows, right to left
                   for(int column=ce;column>=cstart;column-=step*2) {
                        if(column==ce) {
                           //first bottom
                            //top, middle bottom, top middle, middle bottom
                            //middle bottom, left top, left bottom
                            if(skipright)
                                idx.put((nrow+step)*size+column-step,(nrow+step)*size+column-step,(nrow+step)*size+column-step);
                            else
                                idx.put((nrow+step)*size+column, nrow*size+column, (nrow+step)*size+column-step);
                            idx.put(nrow*size+column-step);
                            if(ce>=cstart+step) {
                                idx.put((nrow+step)*size+column-step);
                                if(column>=cstart+step*2) {
                                    idx.put(nrow*size+column-step*2);
                                    idx.put((nrow+step)*size+column-step*2);
                                }
                            }
                        } else if(column==cstart) {
                            // left edge
                            // top
                              //idx.put((nrow+step)*size+column);
                            //idx.put(nrow*size+column);
                        } else {
                            //top, middle bottom, top middle, middle bottom
                            //middle bottom, left top, left bottom
                            idx.put(nrow*size+column).put((nrow+step)*size+column-step);
                            idx.put(nrow*size+column-step);
                            if(column-step*2>=cstart) {
                                idx.put((nrow+step)*size+column-step);
                                idx.put(nrow*size+column-step*2);
                                idx.put((nrow+step)*size+column-step*2);
                            }
                        }
                   }
                   //if(((cend-cstart)/step*2)%2!=0) idx.repeat();
                   //idx.repeat().repeat().repeat();
                   if(idt==0 || idt==1 || idt==4  || idt==8|| idt==5 
                   || idt==10 || idt==11|| idt==12 || idt==14 || idt==15 ) {
                       idx.repeat();
                   }
              }
           }
       }
           // uniform triangle version
           /*
           // normal center block with no fixing needed
           for(int nrow=mend-step;nrow>=mstart;nrow-=step) {
               if(((mend-nrow)/step)%2==startright) {
                   // odd rows, left to right
                   for(int column=cstart;column<=cend;column+=step) {
                        if(column==cstart) {
                           // top, bottom, right top (next is bottom)
                            idx.put(nrow*size+column, (nrow+step)*size+column, nrow*size+column+step);
                        } else if(column==cend) {
                            // right edge
                            // bottom
                            idx.put((nrow+step)*size+column);
                        } else {
                            // normal cell
                            // bottom, right top
                            idx.put((nrow+step)*size+column).put(nrow*size+column+step);
                        }
                   }
               } else {
                   // even rows, right to left
                   for(int column=cend;column>=cstart;column-=step) {
                        if(column==cend) {
                           // bottom, top, left bottom (next is top)
                            idx.put((nrow+step)*size+column, nrow*size+column, (nrow+step)*size+column-step);
                        } else if(column==cstart) {
                            // left edge
                            // top
                            idx.put(nrow*size+column);
                        } else {
                            // normal cell
                            // top, left bottom
                            idx.put(nrow*size+column).put((nrow+step)*size+column-step);
                        }
                   }
               }
           }
            
       }*/
   }
   
   private void topStrip(StripBuffer idx, int idt, int step, int maxcolumn) {

       // top border wih only top fixing (bottom does not matter)
       if((idt&11)==1) {
           for(int column=maxcolumn;column>=0;column-=step*2) {
                if(column==maxcolumn) {
                   // bottom, top, center bottom (next is top)
                    idx.put(step*size+column, column, step*size+column-step);
                    // left top, left bottom
                    idx.put(column-step*2).put(step*size+column-step*2);
                    // next start out at top
                } else if(column==0) {
                    // left edge
                    // top
                    idx.put(column);
                } else if(column==2*step) {
                    // top, left center bottom
                    idx.put(column).put(step*size+column-step);
                    // left top, left bottom
                    idx.put(column-step*2).put(step*size+column-step*2);
                } else {
                    // normal cell
                    // top, left center bottom
                    idx.put(column).put(step*size+column-step);
                    // left top, left bottom
                    idx.put(column-step*2).put(step*size+column-step*2);
                }
           }
       }
       // top border wih top and left fixing
       if((idt&11)==9) {
           for(int column=maxcolumn;column>=0;column-=step*2) {
                if(column==maxcolumn) {
                   // bottom, top, center bottom (next is top)
                    idx.put(step*size+column, column, step*size+column-step);
                    // left top, left bottom
                    idx.put(column-step*2);
                    if(column!=step*2) {
                        idx.put(step*size+column-step*2);
                    }
                    // next start out at top
                } else if(column==0) {
                } else if(column==step*2) {
                    // left edge
                    // top
                    //idx.put(column);
                    // top, left center bottom
                    idx.put(column).put(step*size+column-step);
                    // left top
                    if(maxcolumn!=step*2) {
                        idx.put(column-step*2);
                    }
                } else {
                    // normal cell
                    // top, left center bottom
                    idx.put(column).put(step*size+column-step);
                    // left top, left bottom
                    idx.put(column-step*2).put(step*size+column-step*2);
                }
           }
       }
       // top border wih top and right fixing
       if((idt&11)==3 ) {
           for(int column=maxcolumn;column>=0;column-=step*2) {
                if(column==maxcolumn) {
                   // top, left top, center bottom
                    idx.put(column, column-step*2, step*size+column-step);
                    // left bottom
                    idx.put(step*size+column-step*2);
                    // next start out bottom
                } else if(column==0) {
                    // left edge
                    // top
                    if(maxcolumn!=step*2) {
                        idx.put(step*size+column).put(column);
                    }
                } else {
                    // normal cell
                    // bottom top
                    idx.put(step*size+column).put(column);
                    // center bottom, left top, left bottom
                    idx.put(step*size+column-step).put(column-step*2);//.put(step*size+column-step*2);
                }
           }
       }
       
       // top border wih top left and right fixing
       if((idt&11)==11 ) {
           for(int column=maxcolumn;column>=0;column-=step*2) {
                if(column==maxcolumn) {
                   // top, left top, center bottom
                    idx.put(column, column-step*2, step*size+column-step);
                    if(maxcolumn!=step*2) {
                    // left bottom
                        idx.put(step*size+column-step*2);
                    } else {
                        idx.put(step*size+column-step);
                    }
                    // next start out bottom
                } else if(column==0) {
                    // left edge
                    // top
                    //idx.put(column);
                } else if(column==step*2) {
                    // normal cell
                    // bottom top
                    idx.put(step*size+column).put(column);
                    idx.put(step*size+column-step).put(column-step*2);
                } else {
                    // normal cell
                    // bottom top
                    idx.put(step*size+column).put(column);
                    // center bottom, left top, left bottom
                    idx.put(step*size+column-step).put(column-step*2);//.put(step*size+column-step*2);
                }
           }
       }
        
   }
   
   private void rightStrip(StripBuffer idx, int idt, int step, int maxrow, int maxcolumn) {
       // fixed right border with normal top and bottom border
       if((idt&7)==2 ) {
           for(int row=maxrow;row>=0;row-=step*2) {
                if(row==maxrow) {
                   // left bottom, right bottom, left top
                    idx.put(maxrow*size+maxcolumn-step, maxrow*size+maxcolumn, (maxrow-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((maxrow-2*step)*size+maxcolumn).put((maxrow-2*step)*size+maxcolumn-step);
                    // next start out at top
                } else if(row==0) {
                    // left edge
                    // top
                    //idx.put(column);
                } else {
                    // normal cell
                    //
                    //right, center left
                    idx.put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((row-2*step)*size+maxcolumn).put((row-2*step)*size+maxcolumn-step);
                    //idx.put(row*size+maxcolumn-step).put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right
                    //idx.put((row-step*2)*size+maxcolumn);
                }
           }
       }
       // fixed right border with fixed top border
       if((idt&7)==3 ) {
           for(int row=maxrow;row>=0;row-=step*2) {
                if(row==maxrow) {
                   // left bottom, right bottom, left top
                    idx.put(maxrow*size+maxcolumn-step, maxrow*size+maxcolumn, (maxrow-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((maxrow-2*step)*size+maxcolumn);
                    // next start out at top
                } else if(row==0) {
                    // top edge
                    // top
                    //idx.put(column);
                } else if(row==2*step) {
                    idx.put(row*size+maxcolumn-step);
                    //right, center left
                    idx.put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((row-2*step)*size+maxcolumn);
                } else {
                    // normal cell
                    //
                    idx.put(row*size+maxcolumn-step);
                    //right, center left
                    idx.put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((row-2*step)*size+maxcolumn);
                    //idx.put(row*size+maxcolumn-step).put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right
                    //idx.put((row-step*2)*size+maxcolumn);
                }
           }
       }
       // fixed right border with fixed top and bottom border
       if((idt&7)==7 ) {
           for(int row=maxrow;row>=0;row-=step*2) {
                if(row==maxrow) {
                   // right bottom, right top, left center
                    idx.put(row*size+maxcolumn, (row-2*step)*size+maxcolumn, (row-step)*size+maxcolumn-step);
                    // up left
                    if(maxrow!=step*2)
                        idx.put((row-2*step)*size+maxcolumn-step);
                    else
                        idx.put((row-step)*size+maxcolumn-step);
                    // next start out left
                } else if(row==0) {
                    // left
                    //idx.put(maxcolumn-step);
                } else {
                    // normal cell
                    //
                    // left, right
                    idx.put(row*size+maxcolumn-step).put(row*size+maxcolumn);
                    // center
                    idx.put((row-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((row-2*step)*size+maxcolumn);
                    //.put((row-2*step)*size+maxcolumn-step);
                    //idx.put(row*size+maxcolumn-step).put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right
                    //idx.put((row-step*2)*size+maxcolumn);
                }
           }
       }
       // fixed right border with fixed bottom border
       if((idt&7)==6 ) {
           for(int row=maxrow;row>=0;row-=step*2) {
                if(row==maxrow) {
                   // right bottom, right top, left center
                    idx.put(row*size+maxcolumn, (row-2*step)*size+maxcolumn, (row-step)*size+maxcolumn-step);
                    // up left
                    idx.put((row-2*step)*size+maxcolumn-step);
                    // next start out left
                } else if(row==0) {
                    // left
                    //if(maxrow!=step*2)
                        idx.put(maxcolumn-step);
                } else {
                    // normal cell
                    //
                    // left, right
                    idx.put(row*size+maxcolumn-step).put(row*size+maxcolumn);
                    // center
                    idx.put((row-step)*size+maxcolumn-step);
                    // up right, up left
                    idx.put((row-2*step)*size+maxcolumn);//.put((row-2*step)*size+maxcolumn-step);
                    //idx.put(row*size+maxcolumn-step).put(row*size+maxcolumn).put((row-step)*size+maxcolumn-step);
                    // up right
                    //idx.put((row-step*2)*size+maxcolumn);
                }
           }
       }
   }
 
   private void bottomStrip(StripBuffer idx, int idt, int step, int maxrow, int maxcolumn) {
               
       // bottom border wih only bottom needed fixing
       if((idt&14)==4) {
           for(int column=0;column<=maxcolumn;column+=step*2) {
                if(column==0) {
                   // bottom, top, center bottom (next is top)
                    // A left up, B left down, C center up
                    // H right down I right up, J right down
                    idx.put((maxrow-step)*size, maxrow*size, (maxrow-step)*size+step);
                } else if(column==maxcolumn) {
                    // right edge
                    // down up
                    idx.put(maxrow*size+column).put((maxrow-step)*size+column);
                    //degenerate
                    idx.put((maxrow-step)*size+column);
                } else {
                    // normal cell
                    // D down, E up, F down, G center up,
                    idx.put(maxrow*size+column).put((maxrow-step)*size+column).put(maxrow*size+column);
                    // G center up
                    idx.put((maxrow-step)*size+column+step);
                }
           }
       }
       // bottom with right
       if((idt&14)==6) {
           for(int column=0;column<=maxcolumn;column+=step*2) {
                if(column==0) {
                   // bottom, top, center bottom (next is top)
                    // A left up, B left down, C center up
                    // H right down I right up, J right down
                    idx.put((maxrow-step)*size, maxrow*size, (maxrow-step)*size+step);
                    //idx.put((maxrow-step)*size, maxrow*size, (maxrow-step)*size+step);
                } else if(column==maxcolumn) {
                    // right edge
                    // down
                    //if(maxcolumn!=step*2)
                        idx.put(maxrow*size+column);
                    // degenerate
                    //idx.put(maxrow*size+column);
                } else {
                    // normal cell
                    // D down, E up, F down, G center up,
                    idx.put(maxrow*size+column).put((maxrow-step)*size+column).put(maxrow*size+column);
                    // G center up
                    idx.put((maxrow-step)*size+column+step);
                }
           }
       }
       // bottom with left
       if((idt&14)==12) {
           for(int column=0;column<=maxcolumn;column+=step*2) {
                if(column==0) {
                   // bottom, top, center bottom (next is top)
                    // A left down, B right down C center up 
                    idx.put(maxrow*size, maxrow*size+step*2, (maxrow-step)*size+step);
                    // D right up, E right up, F right down
                    idx.put((maxrow-step)*size+step*2).put((maxrow-step)*size+step*2).put(maxrow*size+column+step*2);
                } else if(column==maxcolumn) {
                    // right edge
                    // down up
                    //idx.put(maxrow*size+column).put((maxrow-step)*size+column);
                } else {
                    // normal cell
                    // G center up, H right down, I right up, J right up, 
                    idx.put((maxrow-step)*size+column+step).put(maxrow*size+column+2*step);
                    idx.put((maxrow-step)*size+column+2*step).put(maxrow*size+column+2*step);
                }
           }
       }
       // bottom with left and right
       if((idt&14)==14) {
           for(int column=0;column<=maxcolumn;column+=step*2) {
                if(column==0) {
                   // bottom, top, center bottom (next is top)
                    // A left down, B right down C center up 
                    idx.put(maxrow*size, maxrow*size+step*2, (maxrow-step)*size+step);
                    // D right up, E right up, F right down
                    if(maxrow!=step*2)
                        idx.put((maxrow-step)*size+step*2).put((maxrow-step)*size+step*2).put(maxrow*size+column+step*2);
                } else if(column==maxcolumn) {
                    // right edge
                    // down up
                    //idx.put(maxrow*size+column).put((maxrow-step)*size+column);
                } else if(column==maxcolumn-step*2) {
                    idx.put((maxrow-step)*size+column+step).put(maxrow*size+column+2*step);
                    idx.put(maxrow*size+column+2*step);
                    //idx.put((maxrow-step)*size+column+2*step).put(maxrow*size+column+2*step);                            
                } else {
                    // normal cell
                    // G center up, H right down, I right up, J right up, 
                    idx.put((maxrow-step)*size+column+step).put(maxrow*size+column+2*step);
                    idx.put((maxrow-step)*size+column+2*step).put(maxrow*size+column+2*step);
                }
           }
       }
   }
   
   private void leftStrip(StripBuffer idx, int idt, int step, int maxrow) {
       // left with only left fixing
       if((idt&13)==8 ) {
           for(int row=0;row<=maxrow;row+=step*2) {
                if(row==0) {
                   // A right, B left, C middle center
                    idx.put(step, 0, step*size+step);
                   // D bottom left, E bottom right
                   idx.put((step*2)*size).put((step*2)*size+step);
                } else if(row==maxrow) {
                    idx.put(row*size+step);
                } else {
                    // normal cell
                   // F left, G right center
                   idx.put(row*size).put((row+step)*size+step);
                   // H left bottom, I right bottom
                   idx.put((row+2*step)*size).put((row+2*step)*size+step);                           
                }
           }
       }
       
       // left with top
       if((idt&13)==9 ) {
           for(int row=0;row<=maxrow;row+=step*2) {
                if(row==0) {
                   // A left up B left down C right middle
                   // D right bottom
                    idx.put(0, (2*step)*size, (step)*size+step);
                    idx.put((2*step)*size+step);
                } else if(row==maxrow) {
                    // right
                    //if(maxrow!=step*2*size)
                        idx.put(row*size+step);
                } else {
                    // normal cell
                    //right, left, right middle
                    idx.put(row*size+step).put(row*size).put((row+step)*size+step);
                    // left bottom
                    idx.put((row+2*step)*size);
                }
           }
       }
       
       // left with top and bottom
       if((idt&13)==13 ) {
           for(int row=0;row<=maxrow;row+=step*2) {
                if(row==0) {
                   // A left up B left down C right middle
                   // D right bottom
                    idx.put(0, (2*step)*size, step*size+step);
                    if(maxrow!=step*2)
                        idx.put((2*step)*size+step);
                    else
                        idx.put(step*size+step);
                } else if(row==maxrow) {
                    // right
                    //if(maxrow!=step*2)
                        idx.put(row*size);
                } else {
                    // normal cell
                    //right, left, right middle
                    idx.put(row*size+step).put(row*size).put((row+step)*size+step);
                    // left bottom
                    idx.put((row+2*step)*size);
                }
           }
       }
       
       // left with bottom
       if((idt&13)==12 ) {
           for(int row=0;row<=maxrow;row+=step*2) {
                if(row==0) {
                   // A right, B left, C middle center
                    idx.put(step, 0, step*size+step);
                   // D bottom left, E bottom right
                   idx.put((step*2)*size);
                   if(maxrow!=step*2)
                        idx.put((step*2)*size+step);
                   else
                       idx.put(step*size+step);
                } else if(row==maxrow) {
                    //idx.put(row*size+step);
                    idx.put(row*size);
                } else {
                    // normal cell
                   // F left, G right center
                   idx.put(row*size).put((row+step)*size+step);
                   // H left bottom, I right bottom
                   idx.put((row+2*step)*size);
                   if(row!=maxrow-2*step)
                    idx.put((row+2*step)*size+step);
                }
           }
       }
   }
   
   private void buildLodIndexArray() {
       // which lod value to use at the vertex (0-center, 1-top, 2-right, 3-bottom, 4-left)
       lodindexarray=BufferUtils.createFloatBuffer(size*size);
       lodindexarray.clear();
       // fill the index array, which is simple:
       // (0-center, 1-top, 2-right, 3-bottom, 4-left)
       for (int y = 0; y < size; y++) {
               for (int x = 0; x < size; x++) {
                   if(y==0) {
                       // top edge: 1
                       lodindexarray.put(1);
                   } else if(x==size-1) {
                       // right: 2
                       lodindexarray.put(2);
                   } else if(y==size-1) {
                       // bottom
                       lodindexarray.put(3);
                   } else if(x==0) {
                       // right
                       lodindexarray.put(4);
                   } else {
                       // middle
                       lodindexarray.put(0);
                   }
               }
       }
       lodindexarray=reorderFloatBuffer(lodindexarray);
   }
   
   private void buildLodIndexArray2() {
       // which lod value to use at the vertex (0-center, 1-top, 2-right, 3-bottom, 4-left)
       lodindexarray=BufferUtils.createFloatBuffer(size*size);
       lodindexarray.clear();
       // fill the index array, which is simple:
       // (0-center, 1-top, 2-right, 3-bottom, 4-left)
       for (int y = 0; y < size; y++) {
               for (int x = 0; x < size; x++) {
                   if(x>y && x<size-y) {
                       // top edge: 1
                       lodindexarray.put(1);
                   } else if(x>size-y && x>y) {
                       // right: 2
                       lodindexarray.put(2);
                   } else if(x<y && x>size-y) {
                       // bottom
                       lodindexarray.put(3);
                   } else if(x<y && x<size-y) {
                       // right
                       lodindexarray.put(4);
                   } else {
                       // middle
                       lodindexarray.put(0);
                   }
               }
       }
       lodindexarray=reorderFloatBuffer(lodindexarray);
   }
   
   private void buildLevelArray() {
       // the level when the vertex is lost
       levelarray=BufferUtils.createFloatBuffer(size*size);
       //maxlodarray[fixmask]=levelarray;
       levelarray.clear();
       int mstart=0; int mend=size-1; int cstart=0; int cend=size-1;
       int dl, pos;
       
       // the four corners have at maxLod value heir original heights (they do not morph)
       pos=mstart*size+cstart;
       dl=maxLod;
       levelarray.put(pos, dl);
       // 0,size-1
       pos=cend + (mstart * size);
       levelarray.put(pos, dl);
       // size-1,0
       pos=cstart + (mend * size);
       levelarray.put(pos, dl);
       // size-1,size-1
       pos=cend + (mend * size);
       levelarray.put(pos, dl);
       
       for(int lod=maxLod;lod>=1;lod--) {
           // calculate
           int step=1<<lod;
           dl=lod-1;
           int x,y;
           // calculate data for remaining middle region vertices
           for (y = mstart; y <= mend; y+=step) {
               for (x = cstart; x <= cend; x+=step) {
                  int av1,av2;
                  // calculate averages
                  if(x+step<=cend) {
                      // top middle
                      pos=y*size+x+step/2;
                      levelarray.put(pos, dl);
                  }
                  if(y+step<=mend) {
                      // middle left
                      pos=(y+step/2)*size+x;
                      levelarray.put(pos, dl);
                  }
                  if(y+step<=mend && x+step<=cend) {
                      // middle
                      pos=(y+step/2)*size+x+step/2;
                      levelarray.put(pos, dl);
                  }
               }
           }
       }
       // center point at maxlod-1
       pos=(cend-cstart)/2 + ((mend-mstart)/2 * size);
       // average on upper left, lower right
       levelarray.put(pos, maxLod-1);
       levelarray=reorderFloatBuffer(levelarray);
   }

    private IntBuffer renumberIndices(int indextype, IntBuffer indbuff) {
        if(vertOrder==null) return indbuff;
        int maxIndex=0;
        // fetch each index, if its new, then assign it a new number,
        // if its old then use its index
        IntBuffer reordered=BufferUtils.createIntBuffer(indbuff.limit());
        reordered.clear();
        indbuff.rewind();
        int idx;
        for(int i=0;i<indbuff.limit();i++) {
            idx=indbuff.get();
            if(vertOrder[idx]==-1) {
               vertOrder[idx]= currentIndex++;
            }
            if(vertOrder[idx]>maxIndex) {
                maxIndex=vertOrder[idx];
            }
            reordered.put(vertOrder[idx]);
        }
        if(useMultVertBuffers) {
            // store max vertex's number required for this index
            vertQuantities[indextype]=maxIndex;
        }
        reordered.rewind();
        return reordered;
    }
    
    public FloatBuffer reorderFloatBuffer(FloatBuffer source) {
        if(vertOrder==null) return source;
        FloatBuffer reordered=BufferUtils.createFloatBuffer(source.limit());
        reordered.clear();
        source.rewind();
        for(int i=0;i<source.limit();i++) {
            reordered.put(vertOrder[i], source.get());
       }
       return reordered;
    }
    
    public int getUsedVertQuantity(int indextype) {
        if(useMultVertBuffers) {
            return vertQuantities[indextype]+1;
        }
        return size*size;
    }
}
