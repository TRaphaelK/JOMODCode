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
package jb2.gai;

import jb2.math.FastMath;
import jb2.math.Tensor;

/**
 *
 * @author vear
 */
public class EntParameters {
    public static enum ParamType {
        // path-finding parameters
        // 0 from 0 distance, 1 manhattan distance
        EstimateDistanceType(0f, 1f, 0f),
        // node weights
        // 1 planWeight
        NodePlan(0f, 1f, 1f),
        // 2 traversalWeight
        NodeTraversal(0f, 1f, 1f),
        // 3 deathWeight (0 to 200)
        NodeDeath(0f, 1f, 1f),
        // link weights
        // 4 with which passed/failed ratio to consider a link as stuck
        LinkStuckRatio(0.5f, 1f, 0.5f),
        // with which ratio to consider a link to be vertical, and exempt from 2d pathfinindg        
        // 5 from 0 distance, 1 manhattan distance
        LinkDistanceType(0f, 1f, 0f),
        // 6 distWeight
        LinkDistance(0.5f, 1f, 1f),
        // 7 height difference weight
        LinkHeightDiff(0f, 10f, 0f),
        // 8 stuckWeight (0 to 200)
        LinkStuck( 0.5f, 10f, 1f),
        // prev determination weights
        // 9 parent cost weight from 0 to 1
        PrevCost( 0.7f, 1f, 1f),
        // priority queue balancing
        // 10 estimate to cost ratio for queue priority
        PriorityEstimate( 0.3f, 1f, 1f),
        // 11 cost ratio for prio queue
        PriorityCost( 0.3f, 1f, 1f),
        ;
    
        public final float min;
        public final float max;
        public final float def;
        
        private ParamType(float wmin, float wmax, float wdef) {
            min = wmin;
            max = wmax;
            def = wdef;
        }
    }
    
    // the number of weights we manage
    public static final int weightsCount = ParamType.values().length;
    
    // the succesfullness of this parameter variation (XP points collected by the bot)
    public float xp;
    
    public final Tensor weights = new Tensor(weightsCount, 1);
    
    public void setDefault() {
        for (EntParameters.ParamType t : EntParameters.ParamType.values()) {
            weights.setComponent(t.ordinal(), 0, t.def);
        }
    }
    
    public float getParameter(ParamType p) {
        return weights.get(p.ordinal(), 0);
    }
    
    public void combine(EntParameters main, EntParameters other, Tensor minMaxValues) {
        weights.ensureCapacity(main.weights.size());
        
        for(int i=0; i<main.weights.size(); i++) {
            float valm= main.weights.get(i, 0);
            float valo= other.weights.get(i, 0);
            
            float val;
            
            // 2/3 of atributes from main, 1/3 from other
            if(FastMath.rand.nextFloat()<0.75f) {
                val=valm;
            } else if(FastMath.rand.nextFloat()<0.95f) {
                val=valo;
            } else {
                float min = minMaxValues.get(i, 0);
                float max = minMaxValues.get(i, 1);
                val = (float) (min + FastMath.rand.nextDouble()* (max - min));
            }
            weights.setComponent(i, 0, val);
        }
    }
}
