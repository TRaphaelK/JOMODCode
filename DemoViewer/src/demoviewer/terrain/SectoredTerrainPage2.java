/*
 * SectoredTerrainPage2.java
 *
 * Created on 2006. március 28., 17:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.util.LoggingSystem;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 *
 * @author vear
 */
public class SectoredTerrainPage2 extends MorphingTerrainPage {
    
   // the extend of this page
   private int xsectorstart;
   private int xsectorend;
   private int ysectorstart;
   private int ysectorend;
   // the number of sectors incorporated
   // pages divide themselves, until they reach
   // the length of 2
   private int sectorlength;
   
    /** Creates a new instance of SectoredTerrainPage2 */
    
   public SectoredTerrainPage2(String name) {
       super(name, true);
       sectorlength=2;
       xsectorstart=-1;
       xsectorend=0;
       ysectorstart=-1;
       ysectorend=0;
   }
   
   public SectoredTerrainPage2(String name, int xstart, int xend, int ystart, int yend) {
       super(name, false);
       sectorlength=xend-xstart+1;
       if(yend-ystart!=sectorlength) {
           // bad case, 
       }
       this.xsectorend=xend;
       this.xsectorstart=xstart;
       this.ysectorend=yend;
       this.ysectorstart=ystart;
       this.u=xsectorstart;
       this.v=ysectorstart;
   }
    
   public int attachChild(Spatial child) {
       // unlock our bounds, but not of our children
       unlockBounds();
       unlockTransforms();
       if((child.getType() & Spatial.TERRAIN_BLOCK) !=0 ) {
           SharedTerrainBlock b=(SharedTerrainBlock)child;
           if(isRoot()) {
               // root has special handling, it increases its size as nodes are
               // added, creates nodes and puts its children into it
               // root is always centered around 0,0 point and has power of two extent
               // create so much subblocks as to hold the desired size
               while(this.getSubQuadrant(b) == 0) {
                    LoggingSystem.getLogger().log(Level.INFO, "SectoredTerrainPage2 increases size");
                   // the root is not big enough, incease it
                   sectorlength*=2;
                   // increase our sector size
                   xsectorstart=-sectorlength/2;
                   xsectorend=(sectorlength/2)-1;
                   ysectorstart=-sectorlength/2;
                   ysectorend=(sectorlength/2)-1;
                   // for each of our children create a new page, and put it into
                   // rewrite its quadrant to match the new quadrant in the new page
                   
                   int size=sectorlength/2;
                   for(int i=0;i<getQuantity();i++) {
                       Spatial s=getChild(i);
                       // is it a page?
                       if(s!=null && (s.getType() & Spatial.TERRAIN_PAGE) !=0) {
                           SectoredTerrainPage2 p = (SectoredTerrainPage2)s;
                           // create a new page with bigger size
                           int q=i+1;
                           int qn;
                           SectoredTerrainPage2 tohold=createSubPage(q);
                           // set the proper quadrant on the old child
                           if(q==1) {
                               qn=4;
                           } else if(q==2) {
                               qn=3;
                           } else if(q==3) {
                               qn=2;
                           } else {
                               qn=1;
                           }
                           // push down the previous child
                           detachChildAt(i);
                           //p.setQuadrant(qn);
                           attachChild(tohold);
                           //setChild(i, tohold);
                           tohold.attachChild(p);
                       }
                   }
               }
           }
           // check if we should send down the block to one of our children
           if(sectorlength > 2) {
               // determine the quadrant
               int q=this.getSubQuadrant(b);
               if(q==0) {
                   LoggingSystem.getLogger().log(Level.WARNING, "Invalid quadrant");
               } else {
                   // find the page to hold this child
                   SectoredTerrainPage2 tohold=(SectoredTerrainPage2)this.getPage(q);

                   // if no page yet, create it
                   if(tohold==null) {
                       tohold=createSubPage(q);
                       this.attachChildAt(tohold, q-1);
                   }
                   // send it down
                   tohold.attachChild(child);
               }
           } else {
               // ours
               int q=this.getSubQuadrant((Terrain)child);
               if(q==0) {
                   LoggingSystem.getLogger().log(Level.WARNING, "Invalid quadrant");
               } else {
                 ((Terrain)child).setQuadrant(q);
                 attachChildAt(child, q-1);
               }
           }
       } else if((child.getType() & Spatial.TERRAIN_PAGE)!=0) {
           SectoredTerrainPage2 p=(SectoredTerrainPage2)child;
           int q=this.getSubQuadrant((Terrain)child);
           if(q==0) {
             LoggingSystem.getLogger().log(Level.WARNING, "Invalid quadrant");
           } else { 
             ((Terrain)child).setQuadrant(q);
             attachChildAt(child, q-1);
           }
       }
       return getQuantity();
   }

   private int getSubQuadrant(Terrain child) {
       int q=0;
       int xs;
       int ys;
       xs=child.getU();
       ys=child.getV();
       if(xs>=xsectorstart && xs<=xsectorend 
         && ys>=ysectorstart && ys<=ysectorend) {
           q=1;
           // it is half our size
           int size=sectorlength/2;
           if(xs>=(this.xsectorstart + size)) {
               // right
               q+=2;
           }
           if(ys>=(this.ysectorstart + size)) {
               // lower
               q+=1;
           }
       }
       return q;
   }
   
   private SectoredTerrainPage2 createSubPage(int q) {
       int size=sectorlength/2;
       int xs;
       int xe;
       if(q<3) {
           // left
           xs=xsectorstart;
           xe=xsectorstart+size-1;
       } else {
           // right
           xs=xsectorstart+size;
           xe=xsectorend;
       }
       int ys;
       int ye;
       if(q%2!=0) {
           // upper
           ys=ysectorstart;
           ye=ysectorstart+size-1;
       } else {
           // lower
           ys=ysectorstart+size;
           ye=ysectorend;
       }
       // create the new page
       SectoredTerrainPage2 tohold=new SectoredTerrainPage2(this.name+q, xs, xe, ys, ye);
       tohold.setCullMode(Spatial.CULL_DYNAMIC);
       tohold.setRenderQueueMode(Renderer.QUEUE_SKIP);
       tohold.setQuadrant(q);
       return tohold;
   }
   
    public int getXSectorStart() {
        return xsectorstart;
    }

    public int getXSectorEnd() {
        return xsectorend;
    }

    public int getYSectorStart() {
        return ysectorstart;
    }

    public int getYsectorEnd() {
        return ysectorend;
    }
}
