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
        EstimateDistanceType(0f, 1f),
        // node weights
        // 1 planWeight
        NodePlan(-10f, 10f),
        // 2 plan bias (-200 to 200)
        NodePlanBias(-100f, 100f),
        // 3 traversalWeight
        NodeTraversal(-10f, 10f),
        // 4 traversal bias (-200 to 200)
        NodeTraversalBias(-100f, 100f),
            // 5 deathWeight (0 to 200)
        NodeDeath(-200, 200),
            // 6 death bias (-200 to 200)
        NodeDeathBias(-200, 200),
        
        // link weights
        // with which passed/failed ratio to consider a link as stuck
        LinkStuckRatio(0.5f, 1f),
        // with which ratio to consider a link to be vertical, and exempt from 2d pathfinindg
        
        // from 0 distance, 1 manhattan distance
        LinkDistanceType(0f, 1f),
        // 8 distWeight
        LinkDistance(-10f, 10f),
        // 9 height difference weight
        LinkHeightDiff(-10f, 10f),
        // 10 height difference bias (-200 to 200)
        LinkHeightDiffBias( -100f, 100f),
        // 11 stuckWeight (0 to 200)
        LinkStuck( -20f, 20f),
        // 12 stuck bias (-200 to 200)
        LinkStuckBias( -200f, 200f),
        
        // prev determination weights
        // 13 parent cost weight from 0 to 1
        PrevCost( 0, 1),
        // 14 parent link weight from 0 to 1 (not used)
        PrevLink( 0, 1),
        // 15 parent node weight from 0 to 1 (not used)
        PrevNode( 0, 1),
        
        // diminishing parent determination weights
         // 16 parent diminishing prev weight from 0 to 1 (not used)
        DimPrevCost( 0, 1),
        // 17 parent link weight from 0 to 1 (not used)
        DimPrevLink( 0, 1),
        // 18 parent node weight from 0 to 1 (not used)
        DimPrevNode( 0, 1),
        
        // parent determination
        // 19 link weight (0 to 1)
        ParentLink( -10, 10),
        // 20 link bias (-200 to 200)
        ParentLinkBias( -100, 100),
        // 21 prev weigth (0 to 1)
        ParentPrev( -10, 10),
        // 22 prev offset (-200 to 200)
        ParentPrevBias( -100, 100),
        // 23 diminishing parent weight (0 to 1)
        ParentDimPrev( -10, 10),
        // 24 diminishing parent offset (-200 to 200)
        ParentDimPrevBias( -100, 100),
        
        // priority queue
        // 25 estimate
        PriorityEstimate( -1, 1),
        // 26 estimate offset
        PriorityEstimateBias( -10, 10),
        // 27 link
        PriorityLink( -1, 1),
        // 28 link bias
        PriorityLinkBias( -10, 10),
        // 29 node
        PriorityNode( -1, 1),
        // 30 node offset
        PriorityNodeBias( -10, 10),
        // 31 parent
        PriorityPrev( -1, 1),
        // 32 parent offset
        PriorityPrevBias( -10, 10),
        // 33 diminishing parent
        PriorityDimPrev( -1, 1),
        // 34 diminishing parent bias
        PriorityDimPrevBias( -10, 10),
        ;
    
        public final float min;
        public final float max;
        
        private ParamType(float wmin, float wmax) {
            min = wmin;
            max = wmax;
        }
    }
    
    // the number of weights we manage
    public static final int weightsCount = ParamType.values().length;
    
    // the succesfullness of this parameter variation (XP points collected by the bot)
    public float xp;
    
    public static final float defaultWeights[] = new float[] {
            // estimate determination
            0f, // 0 from 0 distance, 1 manhattan distance

            // node weights
            1f, // 1 planWeight
            0f, // 2 plan bias (-200 to 200)
            1f, // 3 traversalWeight
            0f, // 4 traversal bias (-200 to 200)
            20f, // 5 deathWeight (0 to 200)
            0f, // 6 death bias (-200 to 200)
            
            // link weights
            0f, // 7 from 0 distance, 1 manhattan distance
            1f, // 8 distWeight
            0f,  // 9 height difference weight
            0f,  // 10 height difference bias (-200 to 200)
            1f, // 11 stuckWeight (0 to 200)
            0f, // 12 stuck bias (-200 to 200)

            // prev determination weights
            1f, // 13 parent prev weight from 0 to 1
            1f, // 14 parent link weight from 0 to 1
            1f, // 15 parent node weight from 0 to 1
            
            // diminishing parent determination weights
            0.1f, // 16 parent diminishing prev weight from 0 to 1
            0.1f, // 17 parent link weight from 0 to 1
            0.1f, // 18 parent node weight from 0 to 1
            
            // parent determination
            1f, // 19 link weight (0 to 1)
            0f, // 20 link offset (-200 to 200)
            1f, // 21 prev weigth (0 to 1)
            0f, // 22 prev offset (-200 to 200)
            0f, // 23 diminishing parent weight (0 to 1)
            0f, // 24 diminishing parent offset (-200 to 200)
            
            // priority queue
            1f, // 25 estimate
            0f, // 26 estimate offset
            0.2f, // 27 link
            0f, // 28 link offset
            0.3f, // 29 node
            0f, // 30 node offset
            0f, // 31 parent
            0f, // 32 parent offset
            0f, // 33 diminishing parent
            0f  // 34 diminishing parent offset
        };
    
    public final Tensor weights = new Tensor(weightsCount, 1);
    
    public void setDefault() {
        weights.set(defaultWeights);
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
