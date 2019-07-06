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
package jb2.map;

import jb2.gai.EntParameters;
import jb2.math.FastMath;


/**
 *
 * @author vear
 */
public class NavLink {

    public static final int numLinkCosts = 4;
    public NavCell from;
    public NavCell to;
    public float[] linkcost = new float[numLinkCosts]; //[numnodes][9][numcosts]
    
    // link costs
    // 0    distance (units)
    // 1    bot completed route counter
    // 2    bot failed/stuck on route counter
    // 3    traversed trough unguided movement
    
    // not a good check for verticality, as it does not consider terrain height
    /*
    public boolean isVertical() {
        float dist2d = from.bounds.center.distance2d(to.bounds.center);
        float height = FastMath.abs(from.bounds.center.y - to.bounds.center.y);
        if(height!=0) {
            if(dist2d/height < 0.2f) {
                return true;
            }
        }
        return false;
    }
*/
    
    public boolean isLinkStuck(EntParameters parms) {
        float total = linkcost[1] + linkcost[2];
        if(total == 0) 
            return false;
        if(total < 3 && isTraversed() ) {
            // too few samples
            return false;
        }
        float stuckratio = 0.5f;
        if(parms!=null)
            stuckratio = parms.getParameter(EntParameters.ParamType.LinkStuckRatio);
        return linkcost[2] / total>stuckratio;
    }
    
    public boolean isTraversed() {
        return linkcost[1] > 0 || linkcost[3]>0;
    }

    public float getAbsBotStuckCount() {
        return FastMath.max(0, linkcost[2] - linkcost[1]);
    }

    public void addTrackedTraverseCount() {
        linkcost[3]++;
    }
    
    public void addBotTraverseCount() {
        linkcost[1]++;
    }

    public void addStuckCount() {
        linkcost[2]++;
    }

    @Override
    public String toString() {
        return "{" + "" + from.nodeid + "," + to.nodeid + '}';
    }
}
