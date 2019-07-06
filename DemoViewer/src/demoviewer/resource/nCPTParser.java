/*
 * nCPTParser.java
 *
 * Created on 2006. február 3., 18:35
 *
 * Original algorith by DevilsClaw, 
 * ported to Java by VeaR
 */

package demoviewer.resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author vear
 */
public class nCPTParser {
    
    int []heightMap;
    
    int [] buff;
    
    /** Creates a new instance of nCPTParser */
    public nCPTParser() {
    }
    
    public void loadBuff(String file) throws java.io.IOException
    {
        InputStream is=new BufferedInputStream(new FileInputStream(file));
        // the buffer
        int InBuff_Size=is.available();
        buff = new int[InBuff_Size];
        int i=0;
        while(i<InBuff_Size)
        {
            buff[i]=is.read();
            i++;
        }
        is.close();
    }
    
    public int[] getHeightMap()
    {
        return heightMap;
    }
    
    private long getULong(int CDEPLoc)
    {
        return (buff[CDEPLoc] + (buff[CDEPLoc+1] << 8) + (buff[CDEPLoc+2] << 16) + (buff[CDEPLoc+3] << 24));
    }
       
    public boolean parseCPT() 
    {

        heightMap = new int[1024*1024];
        
         int CDEPLoc = 0;  //CONTAINS InBuff's OFFSET  
         long CPTSize1 = 0; //CONTAINS NUMBER OF BLOCKS   
         long CPTSize2 = 0; //CONTAINS SIZE OF A UNCOMPRESSED BLOCK  
         long V = 0; //THIS IS USED TO MANIPULATE A LOT OF VARIABLES ALL OVER  
         long CDEPPrep1 = 0; //CONTAINS DATA FROM STAGE 1 AND USED IN STAGE 2  
         long CDEPPrep2 = 0; //CONTAINS DATA FROM STAGE 1 AND USED IN STAGE 2  
         long CDEPPrep3 = 0; //CONTAINS DATA FROM STAGE 1 AND USED IN STAGE 2  
         long DECVal = 0; //CONTAINS VALUE FOR FINAL DECOMPRESSION  
         int OUTLOC = 0; //CONTAINS OutBuff OFFSET  

         //CHECK FOR CDEP HEADER. IF NOT FOUND RETURN FALSE  
         for(int i=0;(i < buff.length)&&((buff[i]!=0x43)||(buff[i+1]!=0x44)||(buff[i+2]!=0x45)||(buff[i+3]!=0x50));i++)
         {
             CDEPLoc = i+1;
         }  
         if(CDEPLoc >= buff.length){
             return false;
         }  

         CDEPLoc+=4;  

         //GRAB NUMBER OF BLOCKS IF ZERO BLOCKS RETURN FALSE  
         CPTSize1 = buff[CDEPLoc] + buff[CDEPLoc+1]<<8;
         
                 //(*((long*)(InBuff+CDEPLoc)) & 0xFFFF);  
         if(CPTSize1 == 0)return(false);

         CDEPLoc+=2;  

         //GRAB UNCOMPRESSED BLOCK SIZE  
         CPTSize2 = buff[CDEPLoc] + buff[CDEPLoc+1]<<8;
         
                 //(*((long*)(InBuff+CDEPLoc)) & 0xFFFF);  
         CDEPLoc+=2;  
         V=0;  

         //ALL THE CODE LOOKS FINE BUT SOMETHING IS NOT RIGTH  
         //ENTER MAIN LOOP
         for(int LargCPT = 0; LargCPT < CPTSize1; LargCPT++)
         {
              //WORK ON GETTING THE INFO ON THE CURRENT BLOCK
              // read bits per height value
              CDEPPrep1 = (buff[CDEPLoc] >>> V) &0xf;
              CDEPLoc+=((V+=4) >>> 3);  
              V = V&7;  
              CDEPPrep2 = (getULong(CDEPLoc) >>> V) &0xffff;
              //CDEPLoc+=2; //this is the same as CDEPLoc+=((V+=16) >> 3); but i wanted to keep same algro just incase   
              CDEPLoc+=((V+=16) >>> 3);
              V = V&7;  
              //CDEPPrep3 = ((unsigned long)(CDEPPrep3=1) << CDEPPrep1);  
              // create bitmask for values
              CDEPPrep3 = (1 << CDEPPrep1);
              CDEPPrep3--;
              if(CPTSize2 > 0) {
                   for(int SmallCPT = 0; SmallCPT < CPTSize2; SmallCPT++)
                   {
                        //AFTER COLLECTING INFO. BEGIN DECOMPRESSION OF THIS BLOCK  
                        //DECVal = ((*((unsigned long*)(InBuff+CDEPLoc)) >> V) & CDEPPrep3);  
                       DECVal = ((getULong(CDEPLoc) >>> V) & CDEPPrep3);
                        V += CDEPPrep1;  
                        CDEPLoc+=(V >>> 3);  
                        V = V&7;  
                        heightMap[OUTLOC]=((int)(DECVal+CDEPPrep2));
                        OUTLOC++;
                   }
              }
         }
         return true;
    }
    
    public void saveRAW(String filename) throws java.io.FileNotFoundException,
                                                java.io.IOException
    {
        OutputStream flo=new BufferedOutputStream(new FileOutputStream(filename));
        for(int i=0;i<heightMap.length;i++) {
            flo.write((heightMap[i] >>> 0) & 0xFF);
            flo.write((heightMap[i] >>> 8) & 0xFF);
        }
        flo.close();
    }
    
    public static void main(String[] args) {
        nCPTParser prs=new nCPTParser();
        // test CPT RAW extraction
        try {
            prs.loadBuff("extracted/Dvxc1.cpt");
            if(prs.parseCPT())
            {
            // save out the generated raw
                prs.saveRAW("decompressed/Dvxc1.cpt.raw");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
                
    }
    
}
