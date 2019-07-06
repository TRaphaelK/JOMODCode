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
package test;
/*
import com.aparapi.Kernel;
import com.aparapi.Range;
import com.aparapi.device.Device;
import com.aparapi.device.OpenCLDevice;
import com.aparapi.internal.kernel.KernelManager;
import com.aparapi.internal.kernel.KernelPreferences;
import com.aparapi.internal.opencl.OpenCLPlatform;
import java.util.List;
*/

/**
 *
 * @author vear
 */
public class Test04Squares {

    /*
    public static void main(String[] args) {
        info( );
        square();
        
    }
    
    public static void square() {
        final int size = 512;

      // Input float array for which square values need to be computed. 
      final float[] values = new float[size];

      /// Initialize input array. 
      for (int i = 0; i < size; i++) {
         values[i] = i;
      }

      // Output array which will be populated with square values of corresponding input array elements. 
      final float[] squares = new float[size];

      // Aparapi Kernel which computes squares of input array elements and populates them in corresponding elements of 
      Kernel kernel = new Kernel(){
         @Override public void run() {
            int gid = getGlobalId();
            squares[gid] = values[gid] * values[gid];
         }
      };

      // Execute Kernel.

      kernel.execute(Range.create(512));

      // Report target execution mode: GPU or JTP (Java Thread Pool).
      System.out.println("Device = " + kernel.getTargetDevice().getShortDescription());

      // Display computed square values.
      for (int i = 0; i < size; i++) {
         System.out.printf("%6.0f %8.0f\n", values[i], squares[i]);
      }

      // Dispose Kernel resources.
      kernel.dispose();
    }
    
    public static void info() {
        System.out.println("com.aparapi.examples.info.Main");
      List<OpenCLPlatform> platforms = (new OpenCLPlatform()).getOpenCLPlatforms();
      System.out.println("Machine contains " + platforms.size() + " OpenCL platforms");
      int platformc = 0;
      for (OpenCLPlatform platform : platforms) {
         System.out.println("Platform " + platformc + "{");
         System.out.println("   Name    : \"" + platform.getName() + "\"");
         System.out.println("   Vendor  : \"" + platform.getVendor() + "\"");
         System.out.println("   Version : \"" + platform.getVersion() + "\"");
         List<OpenCLDevice> devices = platform.getOpenCLDevices();
         System.out.println("   Platform contains " + devices.size() + " OpenCL devices");
         int devicec = 0;
         for (OpenCLDevice device : devices) {
            System.out.println("   Device " + devicec + "{");
            System.out.println("       Type                  : " + device.getType());
            System.out.println("       GlobalMemSize         : " + device.getGlobalMemSize());
            System.out.println("       LocalMemSize          : " + device.getLocalMemSize());
            System.out.println("       MaxComputeUnits       : " + device.getMaxComputeUnits());
            System.out.println("       MaxWorkGroupSizes     : " + device.getMaxWorkGroupSize());
            System.out.println("       MaxWorkItemDimensions : " + device.getMaxWorkItemDimensions());
            System.out.println("   }");
            devicec++;
         }
         System.out.println("}");
         platformc++;
      }

      KernelPreferences preferences = KernelManager.instance().getDefaultPreferences();
      System.out.println("\nDevices in preferred order:\n");

      for (Device device : preferences.getPreferredDevices(null)) {
         System.out.println(device);
         System.out.println();
      }
    }
*/
    
}
