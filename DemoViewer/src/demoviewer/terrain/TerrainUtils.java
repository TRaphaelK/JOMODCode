/*
 * TerrainUtils.java
 *
 * Created on 2006. március 13., 15:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.terrain;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.util.geom.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 *
 * @author vear
 */
public class TerrainUtils {
    
    /** Creates a new instance of TerrainUtils */
    public TerrainUtils() {
    }
    
    public static FloatBuffer buildUpRightLeftDownAlteratingNormals(int[] heightMap, Vector3f stepScale, FloatBuffer normBuf, int[] vertexOrder) {
        int vertQuantity=heightMap.length;
        int size=(int)FastMath.sqrt(vertQuantity);
        
       normBuf = BufferUtils.createVector3Buffer(normBuf, vertQuantity);
       // compute face normals
       // there are two triangles for each cell, and one less
       // cell as the number of vertices
       Vector3f[] planenormals=new Vector3f[(size-1)*(size-1)*2];
       Vector3f v1=new Vector3f();
       Vector3f v2=new Vector3f();
       Vector3f v3=new Vector3f();
       int px,py;
       // calculate normals from heightmap directly
       // this is useful if we want to calculate the normal map for the
       // whole terrain, not just for one composing block
       for(int y=0;y<size-1;y+=1)
           for(int x=0;x<size-1;x+=1) {
                if((x+y)%2==0) {
                    // cell with up right down left
                    // lower left triangle
                    // upper left
                    px=x; py=y;
                    v1.set(px * stepScale.x, heightMap[px + (py * size)]* stepScale.y, py * stepScale.z);
                    // lower left
                    px=x; py=y+1;
                    v2.set(px * stepScale.x, heightMap[px + (py * size)]* stepScale.y, py * stepScale.z);
                    // lower right
                    px=x+1; py=y+1;
                    v3.set(px * stepScale.x, heightMap[px + (py * size)]* stepScale.y, py * stepScale.z);
                    // calculate the plane normal
                    v1.subtractLocal(v3);
                    planenormals[(y*(size-1)+x)*2] = v1.cross(v3.subtractLocal(v2)).normalizeLocal();
                    // upper right triangle
                    // upper right
                    px=x+1; py=y;
                    v1.set(px * stepScale.x, heightMap[px + (py * size)]* stepScale.y, py * stepScale.z);
                    // upper left
                    px=x;py=y;
                    v2.set(px * stepScale.x, heightMap[px + (py * size)]* stepScale.y, py * stepScale.z);
                    // lower right
                    px=x+1;py=y+1;
                    v3.set(px * stepScale.x, heightMap[px + (py * size)]* stepScale.y, py * stepScale.z);
                    // calculate the plane normal
                    v1.subtractLocal(v3);
                    planenormals[(y*(size-1)+x)*2+1] = v1.cross(v3.subtractLocal(v2)).normalizeLocal();
                } else {
                    // cell with up left, down right
                    // upper left triangle
                    // upper left
                    v1.set(x * stepScale.x, heightMap[x + (y * size)]* stepScale.y, y * stepScale.z);
                    // lower left
                    v2.set(x * stepScale.x, heightMap[x + ((y+1) * size)]* stepScale.y, (y+1) * stepScale.z);
                    // upper right
                    v3.set((x+1) * stepScale.x, heightMap[(x+1) + (y * size)]* stepScale.y, y * stepScale.z);
                    // calculate the plane normal
                    v1.subtractLocal(v3);
                    planenormals[(y*(size-1)+x)*2] = v1.cross(v3.subtractLocal(v2)).normalizeLocal();
                    // lower right triangle
                    // lower left
                    v1.set(x * stepScale.x, heightMap[x + ((y+1) * size)]* stepScale.y, (y+1) * stepScale.z);
                    // lower right
                    v2.set((x+1) * stepScale.x, heightMap[(x+1) + ((y+1) * size)]* stepScale.y, (y+1) * stepScale.z);
                    // upper right
                    v3.set((x+1) * stepScale.x, heightMap[(x+1) + (y * size)]* stepScale.y, y * stepScale.z);
                    // calculate the plane normal
                    v1.subtractLocal(v3);
                    planenormals[(y*(size-1)+x)*2+1] = v1.cross(v3.subtractLocal(v2)).normalizeLocal();
                }
           }
       // compute vertex normals as average of face normals
       // ________________
       // |\  |  /|\  |  /|
       // |_ \|/__|__\|/__|
       // |  /|\  |  /|\  |
       // |/__|__\|/__|__\|
       // two kind of vertices:
       // even vertices have max 8 triangles, odd vertices have 4 triangles
       // on borders of map, there are missing triangles
       normBuf.clear();
       Vector3f sum = new Vector3f();
       int tris = 0;
       int pos;
       for(int y=0;y<size;y+=1)
           for(int x=0;x<size;x+=1) {
                sum.zero();
                tris=0;
                if((x+y)%2==0) {
                    // above left
                    if(y>0 && x>0) {
                        sum.addLocal(planenormals[((y-1)*(size-1)+x-1)*2]);
                        tris++;
                    }
                    // above left 2
                    if(y>0 && x>0) {
                        sum.addLocal(planenormals[((y-1)*(size-1)+x-1)*2+1]);
                        tris++;
                    }
                    // above
                    if(y>0 && x<size-1) {
                        sum.addLocal(planenormals[((y-1)*(size-1)+x)*2]);
                        tris++;
                    }
                    // above 2
                    if(y>0 && x<size-1) {
                        sum.addLocal(planenormals[((y-1)*(size-1)+x)*2+1]);
                        tris++;
                    }
                    //  right 1
                    if(y<size-1 && x<size-1) {
                        sum.addLocal(planenormals[(y*(size-1)+x)*2]);
                        tris++;
                    }
                    //  right 2
                    if(y<size-1 && x<size-1) {
                        sum.addLocal(planenormals[(y*(size-1)+x)*2+1]);
                        tris++;
                    }
                    // left
                    if(y<size-1 && x>0) {
                        sum.addLocal(planenormals[(y*(size-1)+x-1)*2]);
                        tris++;
                    }
                    // left 2
                    if(y<size-1 && x>0) {
                        sum.addLocal(planenormals[(y*(size-1)+x-1)*2+1]);
                        tris++;
                    }
                } else {
                    // above left 2
                    if(y>0 && x>0) {
                        sum.addLocal(planenormals[((y-1)*(size-1)+x)*2-1]);
                        tris++;
                    }
                    // above 1
                    if(y>0 && x<size-1) {
                        sum.addLocal(planenormals[((y-1)*(size-1)+x)*2]);
                        tris++;
                    }
                    // right
                    if(y<size-1 && x<size-1) {
                        sum.addLocal(planenormals[(y*(size-1)+x)*2]);
                        tris++;
                    }
                    // left 2
                    if(y<size-1 && x>0) {
                        sum.addLocal(planenormals[(y*(size-1)+x)*2-1]);
                        tris++;
                    }
                }
                sum.divideLocal((float) (-tris)).normalizeLocal();
                pos=x+size*y;
                if(vertexOrder!=null) {
                    pos=vertexOrder[x+y*size];
                }
                normBuf.put(pos*3, sum.x).put(pos*3+1, sum.y).put(pos*3+2, sum.z);
           }
       return normBuf;
    }
    
    public static FloatBuffer buildUpLeftDownRightTrianglulatedNormals(int[] heightMap, Vector3f stepScale, FloatBuffer normBuf) {
        int vertQuantity=heightMap.length;
        int size=(int)FastMath.sqrt(vertQuantity);
        
       normBuf = BufferUtils.createVector3Buffer(normBuf, vertQuantity);
       // compute face normals
       // there are two triangles for each cell, and one less
       // cell as the number of vertices
       Vector3f[] planenormals=new Vector3f[(size-1)*(size-1)*2];
       Vector3f v1=new Vector3f();
       Vector3f v2=new Vector3f();
       Vector3f v3=new Vector3f();
       // calculate normals from heightmap directly
       // this is useful if we want to calculate the normal map for the
       // whole terrain, not just for one composing block
       for(int y=0;y<size-1;y+=1)
           for(int x=0;x<size-1;x+=1) {
                // upper left triangle
                // upper left
                v1.set(x * stepScale.x, heightMap[x + (y * size)]* stepScale.y, y * stepScale.z);
                // lower left
                v2.set(x * stepScale.x, heightMap[x + ((y+1) * size)]* stepScale.y, (y+1) * stepScale.z);
                // upper right
                v3.set((x+1) * stepScale.x, heightMap[(x+1) + (y * size)]* stepScale.y, y * stepScale.z);
                // calculate the plane normal
                v1.subtractLocal(v3);
                planenormals[(y*(size-1)+x)*2] = v1.cross(v3.subtractLocal(v2)).normalizeLocal();
                // lower right triangle
                // lower left
                v1.set(x * stepScale.x, heightMap[x + ((y+1) * size)]* stepScale.y, (y+1) * stepScale.z);
                // lower right
                v2.set((x+1) * stepScale.x, heightMap[(x+1) + ((y+1) * size)]* stepScale.y, (y+1) * stepScale.z);
                // upper right
                v3.set((x+1) * stepScale.x, heightMap[(x+1) + (y * size)]* stepScale.y, y * stepScale.z);
                // calculate the plane normal
                v1.subtractLocal(v3);
                planenormals[(y*(size-1)+x)*2+1] = v1.cross(v3.subtractLocal(v2)).normalizeLocal();
           }
       // compute vertex normals as average of face normals
       // ________
       // |  /|  /|
       // |/__|/__|
       // |  /|  /|
       // |/__|/__|
       // there can be 6 triangles a vertex is member of.
       // on borders of map, there are missing triangles
       normBuf.clear();
       Vector3f sum = new Vector3f();
       int tris = 0;
       int pos;
       for(int y=0;y<size;y+=1)
           for(int x=0;x<size;x+=1) {
                sum.zero();
                tris=0;
                // above left 2
                if(y>0 && x>0) {
                    sum.addLocal(planenormals[((y-1)*(size-1)+x)*2-1]);
                    tris++;
                }
                // above 1
                if(y>0 && x<size-1) {
                    sum.addLocal(planenormals[((y-1)*(size-1)+x)*2]);
                    tris++;
                }
                // above 2
                if(y>0 && x<size-1) {
                    sum.addLocal(planenormals[((y-1)*(size-1)+x)*2+1]);
                    tris++;
                }
                // left 1
                if(y<size-1 && x>0) {
                    sum.addLocal(planenormals[(y*(size-1)+x)*2-2]);
                    tris++;
                }
                // left 2
                if(y<size-1 && x>0) {
                    sum.addLocal(planenormals[(y*(size-1)+x)*2-1]);
                    tris++;
                }
                // right
                if(y<size-1 && x<size-1) {
                    sum.addLocal(planenormals[(y*(size-1)+x)*2]);
                    tris++;
                }
                sum.divideLocal((float) (-tris)).normalizeLocal();
                normBuf.put(sum.x).put(sum.y).put(sum.z);
           }
       return normBuf;
    }
    
   public static com.jme.image.Image getUpLeftDownRightTrianglulatedNormalMap(int[] heightMap, Vector3f stepScale, int imageSize) {
       // first create the normal arrray
       FloatBuffer normBuf=buildUpLeftDownRightTrianglulatedNormals(heightMap, stepScale, null);
       return getUpLeftDownRightTrianglulatedNormalMap(normBuf,imageSize);
   }

   public static com.jme.image.Image getUpRightLeftDownAlteratingNormalMap(int[] heightMap, Vector3f stepScale, int imageSize, int components) {
       // first create the normal arrray
       FloatBuffer normBuf=buildUpRightLeftDownAlteratingNormals(heightMap, stepScale, null, null);
       return getInterpolatedNormalMap(normBuf,imageSize, components);
   }
   
   /*
    * Converts the normal array into normal map texture, to
    * prevent ligthing from popping on the morphing terrain
    */
    /**
     * 
     * @param normBuf Normal buffer to extract data from
     * @param imageSize Size of the texture image in pixels
     * @param components Components (channels) to create: 4-XYZ to RGBA (empty alpha), 3-XYZ to RGB, 
     * 2-XZ to RA (channel 0 and 1)
     * @return 
     */
   public static com.jme.image.Image getInterpolatedNormalMap(FloatBuffer normBuf, int imageSize, int components) {
       
       // the normal map texture has to be power of two
       // so get the bigger power of two number, bigger that the
       // heightmap
       int size=(int)FastMath.sqrt(normBuf.limit()/3);
       int ts=imageSize;
       
       ByteBuffer scratch = ByteBuffer.allocateDirect(ts*ts*components);
       scratch.clear();
       Vector3f tempNorm = new Vector3f();
       Vector3f v1 = new Vector3f();
       Vector3f v2 = new Vector3f();
       Vector3f v3 = new Vector3f();
       Vector3f v4 = new Vector3f();
       // the ratio of the resampling
       float ratio=((float)ts-1)/((float)size-1);//-1
       // resample the normal array into the bigger texture
       for(int y=0;y<ts;y++)
           for(int x=0;x<ts;x++) {
                // get the four values we calculte the average on
                // this is much the same as calculating the interpolated height
                // calculate the reference point in the normal array
                float refx=x/ratio; int rx=(int)FastMath.floor(refx); float fracx=refx-rx;
                float refy=y/ratio; int ry=(int)FastMath.floor(refy); float fracy=refy-ry;
                // upper left
                BufferUtils.populateFromBuffer(v1, normBuf, ry*size+rx);
                if( rx < size-1) {
                    // upper right
                    BufferUtils.populateFromBuffer(v2, normBuf, ry*size+rx+1);
                }
                if(ry < size-1) {
                    // lower left
                    BufferUtils.populateFromBuffer(v3, normBuf, (ry+1)*size+rx);
                }
                if(ry < size-1 && rx< size-1) {
                    // lower right
                    BufferUtils.populateFromBuffer(v4, normBuf, (ry+1)*size+rx+1);
                }
                /*
                if(fracx<1-fracy) {
                    // upper right triangle
                    // lower left, upper left, upper right
                    if( rx < size-1 && ry < size-1)
                        tempNorm.set(v3.multLocal(fracy).addLocal(v1.multLocal(1-fracy-fracx)).addLocal(v2.multLocal(fracx)));
                    else if ( rx < size-1 )
                        tempNorm.set(v1.multLocal(1-fracx).addLocal(v2.multLocal(fracx)));
                    else if (ry < size-1)
                        tempNorm.set(v3.multLocal(fracy).addLocal(v1.multLocal(1-fracy)));
                    else 
                        tempNorm.set(v1);
                } else {
                    // lower left triangle
                    // lower left, lower right, upper right
                    if( rx < size-1 && ry < size-1)
                        tempNorm.set(v3.multLocal(1-fracx).addLocal(v4.multLocal(fracx+fracy-1).addLocal(v2.multLocal(1-fracy))));
                    else if ( rx < size-1 )
                        tempNorm.set(v2);
                    else if ( ry < size-1)
                        tempNorm.set(v3.multLocal(1-fracx).addLocal(v2.multLocal(fracx)));
                    else
                        tempNorm.set(v1);
                }
                 */
                if(rx<size-1)
                    v1.multLocal(1-fracx).addLocal(v2.multLocal(fracx));
                if(ry<size-1 && rx<size-1)
                    v3.multLocal(1-fracx).addLocal(v4.multLocal(fracx));
                if(ry<size-1)
                    v1.multLocal(1-fracy).addLocal(v3.multLocal(fracy));
                tempNorm.set(v1);
                tempNorm.normalizeLocal();
                // v1 now contains the interpolated normal
                // put into texture
                scratch.put((byte)(tempNorm.x*127+127));
                if(components>2)
                    scratch.put((byte)(tempNorm.y*127+127));
                scratch.put((byte)(tempNorm.z*127+127));
                if(components>3)
                    scratch.put((byte)0);
           }
       
       //Get a pointer to the image memory
       scratch.rewind();
        // Create the jme.image.Image object
       com.jme.image.Image textureImage = new com.jme.image.Image();
       if(components==4)
            textureImage.setType(com.jme.image.Image.RGBA8888);
       else if(components==3)
            textureImage.setType(com.jme.image.Image.RGB888);
        if(components==2)
            textureImage.setType(com.jme.image.Image.RA88);
       textureImage.setWidth(ts);
       textureImage.setHeight(ts);
       textureImage.setData(scratch);
       return textureImage;
   }
   
   /*
    * Converts the normal array into normal map texture, to
    * prevent ligthing from popping on the morphing terrain
    */
   public static com.jme.image.Image getUpLeftDownRightTrianglulatedNormalMap(FloatBuffer normBuf, int imageSize) {
       
       // the normal map texture has to be power of two
       // so get the bigger power of two number, bigger that the
       // heightmap
       int size=(int)FastMath.sqrt(normBuf.limit()/3);
       int ts=imageSize;
       
       ByteBuffer scratch = ByteBuffer.allocateDirect(ts*ts*4);
       scratch.clear();
       Vector3f tempNorm = new Vector3f();
       Vector3f v1 = new Vector3f();
       Vector3f v2 = new Vector3f();
       Vector3f v3 = new Vector3f();
       Vector3f v4 = new Vector3f();
       // the ratio of the resampling
       float ratio=((float)ts-1)/((float)size-1);//-1
       // resample the normal array into the bigger texture
       for(int y=0;y<ts;y++)
           for(int x=0;x<ts;x++) {
                // get the four values we calculte the average on
                // this is much the same as calculating the interpolated height
                // calculate the reference point in the normal array
                float refx=x/ratio; int rx=(int)FastMath.floor(refx); float fracx=refx-rx;
                float refy=y/ratio; int ry=(int)FastMath.floor(refy); float fracy=refy-ry;
                // upper left
                BufferUtils.populateFromBuffer(v1, normBuf, ry*size+rx);
                if( rx < size-1) {
                    // upper right
                    BufferUtils.populateFromBuffer(v2, normBuf, ry*size+rx+1);
                }
                if(ry < size-1) {
                    // lower left
                    BufferUtils.populateFromBuffer(v3, normBuf, (ry+1)*size+rx);
                }
                if(ry < size-1 && rx< size-1) {
                    // lower right
                    BufferUtils.populateFromBuffer(v4, normBuf, (ry+1)*size+rx+1);
                }
                if(fracx<1-fracy) {
                    // upper right triangle
                    // lower left, upper left, upper right
                    if( rx < size-1 && ry < size-1)
                        tempNorm.set(v3.multLocal(fracy).addLocal(v1.multLocal(1-fracy-fracx)).addLocal(v2.multLocal(fracx)));
                    else if ( rx < size-1 )
                        tempNorm.set(v1.multLocal(1-fracx).addLocal(v2.multLocal(fracx)));
                    else if (ry < size-1)
                        tempNorm.set(v3.multLocal(fracy).addLocal(v1.multLocal(1-fracy)));
                    else 
                        tempNorm.set(v1);
                } else {
                    // lower left triangle
                    // lower left, lower right, upper right
                    if( rx < size-1 && ry < size-1)
                        tempNorm.set(v3.multLocal(1-fracx).addLocal(v4.multLocal(fracx+fracy-1).addLocal(v2.multLocal(1-fracy))));
                    else if ( rx < size-1 )
                        tempNorm.set(v2);
                    else if ( ry < size-1)
                        tempNorm.set(v3.multLocal(1-fracx).addLocal(v2.multLocal(fracx)));
                    else
                        tempNorm.set(v1);
                }
        /*
                v1.multLocal(1-fracx).addLocal(v2.multLocal(fracx));
                v3.multLocal(1-fracx).addLocal(v4.multLocal(fracx));
                v1.multLocal(1-fracy).addLocal(v3.multLocal(fracy));
         */
                tempNorm.normalizeLocal();
                // v1 now contains the interpolated normal
                // put into texture
                scratch.put((byte)(tempNorm.x*127+127));
                scratch.put((byte)(tempNorm.y*127+127));
                scratch.put((byte)(tempNorm.z*127+127));
                scratch.put((byte)0);
           }
       
       //Get a pointer to the image memory
       scratch.rewind();
        // Create the jme.image.Image object
       com.jme.image.Image textureImage = new com.jme.image.Image();
       textureImage.setType(com.jme.image.Image.RGBA8888);
       textureImage.setWidth(ts);
       textureImage.setHeight(ts);
       textureImage.setData(scratch);
       return textureImage;
   }
   
   public static final int[] createHeightSubBlock(int[] heightMap, int x, int y, int side) {
       int[] rVal = new int[side*side];
       int bsize = (int)FastMath.sqrt(heightMap.length);
       int count = 0;
       for (int i = y; i < side+y; i++) {
           for (int j = x; j < side+x; j++) {
               if (j < bsize && i < bsize)
                   rVal[count] = heightMap[j + (i * bsize)];
               count++;
           }
       }
       return rVal;
   }
   
    public static float getLinearInterpolatedHeight(float x, float z, int[] heightMap, Vector3f stepScale) {
        if(heightMap!=null) {
           int size=(int)FastMath.sqrt(heightMap.length);
           x /= stepScale.x;
           z /= stepScale.z;
           float col = FastMath.floor(x);
           float row = FastMath.floor(z);

           if (col < 0 || row < 0 || col >= size - 1 || row >= size - 1) { return Float.NaN; }
           float intOnX = x - col, intOnZ = z - row;

           float topLeft, topRight, bottomLeft, bottomRight;

           int focalSpot = (int) (col + row * size);

           // find the heightmap point closest to this position (but will always
           // be to the left ( < x) and above (< z) of the spot.
           topLeft = heightMap[focalSpot] * stepScale.y;

           // now find the next point to the right of topLeft's position...
           topRight = heightMap[focalSpot + 1] * stepScale.y;

           // now find the next point below topLeft's position...
           bottomLeft = heightMap[focalSpot + size] * stepScale.y;

           // now find the next point below and to the right of topLeft's
           // position...
           bottomRight = heightMap[focalSpot + size + 1] * stepScale.y;

           // Use linear interpolation to find the height.
           return FastMath.LERP(intOnZ, FastMath.LERP(intOnX, topLeft, topRight),
                   FastMath.LERP(intOnX, bottomLeft, bottomRight));
       } else {
            return Float.NaN;
       }
    }
    
    public static float getAlteratingTriangulatedHeight(float x, float z, int[] heightMap, Vector3f stepScale) {
        if(heightMap!=null) {
           int size=(int)FastMath.sqrt(heightMap.length);
           x /= stepScale.x;
           z /= stepScale.z;
           float col = FastMath.floor(x);
           float row = FastMath.floor(z);

           if (col < 0 || row < 0 || col >= size - 1 || row >= size - 1) { return Float.NaN; }
           float intOnX = x - col, intOnZ = z - row;

           float topLeft, topRight, bottomLeft, bottomRight;

           int focalSpot = (int) (col + row * size);

           // find the heightmap point closest to this position (but will always
           // be to the left ( < x) and above (< z) of the spot.
           topLeft = heightMap[focalSpot] * stepScale.y;

           // now find the next point to the right of topLeft's position...
           topRight = heightMap[focalSpot + 1] * stepScale.y;

           // now find the next point below topLeft's position...
           bottomLeft = heightMap[focalSpot + size] * stepScale.y;

           // now find the next point below and to the right of topLeft's
           // position...
           bottomRight = heightMap[focalSpot + size + 1] * stepScale.y;

           // which type cell
           if((col+row)%2==0) {
               // cell with up right down left
               return bottomLeft*intOnZ + topLeft*(1-intOnZ-intOnX) + topRight*intOnX;
           } else {
               // up left, down right 
               return bottomRight*(1-intOnX) + bottomRight*(intOnX+intOnZ-1) + topRight*(1-intOnZ);
           }
           // Use linear interpolation to find the height.
           //return FastMath.LERP(intOnZ, FastMath.LERP(intOnX, topLeft, topRight),
            //       FastMath.LERP(intOnX, bottomLeft, bottomRight));
       } else {
            return Float.NaN;
       }
    }
    
   private static Vector3f calcVec1 = new Vector3f();
   private static Vector3f calcVec2 = new Vector3f();
   private static Vector3f calcVec3 = new Vector3f();
   
   public static Vector3f getSurfaceNormal(float x, float z, Vector3f store, Vector3f stepScale, FloatBuffer normBuf) {
       x /= stepScale.x;
       z /= stepScale.z;
       float col = FastMath.floor(x);
       float row = FastMath.floor(z);
       int size=(int)FastMath.sqrt((normBuf.limit()/3));
       
       if (col < 0 || row < 0 || col >= size - 1 || row >= size - 1) { return null; }
       float intOnX = x - col, intOnZ = z - row;

       if (store == null) store = new Vector3f();

       Vector3f topLeft = store, topRight = calcVec1, bottomLeft = calcVec2, bottomRight = calcVec3;

       int focalSpot = (int) (col + row * size);

       // find the heightmap point closest to this position (but will always
       // be to the left ( < x) and above (< z) of the spot.
       BufferUtils.populateFromBuffer(topLeft, normBuf, focalSpot);

       // now find the next point to the right of topLeft's position...
       BufferUtils.populateFromBuffer(topRight, normBuf, focalSpot + 1);

       // now find the next point below topLeft's position...
       BufferUtils.populateFromBuffer(bottomLeft, normBuf, focalSpot + size);

       // now find the next point below and to the right of topLeft's
       // position...
       BufferUtils.populateFromBuffer(bottomRight, normBuf, focalSpot + size
               + 1);

       // Use linear interpolation to find the height.
       topLeft.interpolate(topRight, intOnX);
       bottomLeft.interpolate(bottomRight, intOnX);
       topLeft.interpolate(bottomLeft, intOnZ);
       return topLeft.normalizeLocal();
   }
   
    public static int[] subSize(int[] heightmap, int newsize) {
        int hsize=(int)FastMath.sqrt(heightmap.length);
        int[] newheightdata = new int[newsize*newsize];
        Arrays.fill(newheightdata,0);
        int[] counts = new int[newsize*newsize];
        Arrays.fill(counts,0);
        // the ratio
        float ratio=((float)newsize/hsize);
        if(ratio<1) {
            // create a smaller block, by averaging the
            // values to target
            for(int y=0;y<hsize;y++)
                for(int x=0;x<hsize;x++) {
                    int tx=(int)Math.floor(x*ratio);
                    int ty=(int)Math.floor(y*ratio);
                    newheightdata[ty*newsize+tx]+=heightmap[y*hsize+x];
                    counts[ty*newsize+tx]++;
                }
            for(int i=0;i<newheightdata.length;i++) {
                newheightdata[i]/=counts[i];
            }
        } else if(ratio>1) {
            // interpolate height values
        } else {
            // 1 to 1
            newheightdata=heightmap;
        }

        return newheightdata;
    }
    
   
   private Vector3f getBaryCentric(float p1x, float p1y, float p2x, float p2y, float p3x, float p3y, float px, float py, Vector3f store) {
       if(store==null) store=new Vector3f();
       float bx=p3x-p1x;
       float by=p3y-p1y;
       float cx=p2x-p1x;
       float cy=p2y-p1y;
       float x=px-p1x;
       float y=py-p1y;
       float a2 = (bx * y - by * x) / (bx * cy - by * cx);
       float a3 = (x * cy - y * cx) / (bx * cy - by * cx);
       if(a2<0 || a3<0 || a2+a3>1) {
           return store.set(Float.NaN,Float.NaN,Float.NaN);
       }
       float a1 = 1-a2-a3;
       return store.set(a1,a2,a3);
   }
}
