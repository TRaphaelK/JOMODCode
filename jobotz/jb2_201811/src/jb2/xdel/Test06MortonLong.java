/*
 * 
 * Vear 2017  * 
 */
package jb2.xdel;

import java.util.Random;
import jb2.util.Morton3Dint;
import jb2.math.Vector3f;
import jb2.util.Morton3Dlong;

/**
 *
 * @author vear
 */
public class Test06MortonLong {

    /**
     * @param args the command line arguments
     */

  
    public static void main(String[] args) {

        testMorton(0,0,64);
        testMorton(0,0,256);
          
        /*
        for (int x = 0; x < 1 << 12; x++) {
            for (int y = 0; y < (1 << 8); y++) {
                for (int z = 0; z < 1 << 12; z++) {
                    testMorton(x, y, z);
                }
            }
        }
          */
        
        
    }
    
    public static void testMorton(int x, int y, int z) {
        long morton;
        int[] test = new int[3];

        morton = Morton3Dlong.encode(x, y, z);
        Morton3Dlong.decode(morton, test);
        if (x != test[0] || y != test[1] || z != test[2]) {
            System.out.printf("Error x=%d,%d y=%d,%d z=%d,%d, morton=%d\n", x, test[0], y, test[1], z, test[2], morton);
            System.exit(1);
        }

    }

}
