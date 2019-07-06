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
import jb2.math.Vector3f;
import jb2.util.JbException;

/**
 *
 * @author vear
 */
public class RouteNode implements Comparable {
    
    public NavRoute route;
    public RouteNode parent;
    public NavCell node;
    // current link to reach this node
    public NavLink linkTo;
    
    // estimate cost to target
    public float estimate;
    // node cost
    //public float nodecost;
    // own cost (depends only on node)
    //public float linkcost;
    // cost to this node (depends only on links to this node)
    //public float prevcost;
    //public float diminishingPrevcost;
    // route cost to this node
    //public float routecost;
    // queue priority
    public float cost;
    // the depth of node, depth is limited to 200 items
    public int depth;
        
    
    public RouteNode(NavRoute r) {
        route=r;
        node = route.start;
        //setOwncost(dest);
        // for initial node, own cost is 0
        setEstimate();
        // no node cost on root
        //calculateNodeCost();
        depth = 0;
        // for start node, there is no previous cost, just the estimate
        cost = 0;
        linkTo= null;
        parent= null;
    }
    
    public RouteNode(NavRoute r, RouteNode p, NavLink link) {
        route=r;
        node = link.to;
        setEstimate();
        parent = p;
        depth = parent.depth + 1;
        linkTo = link;
        cost = getCost(p, link);
    }

    protected float getCost(RouteNode newParent, NavLink newLink) {
        // 25 estimate
        // 26 estimate offset
        // 27 link
        // 28 link offset
        // 29 node
        // 30 node offset
        // 31 parent
        // 32 parent offset
        // 33 diminishing parent
        // 34 diminishing parent offset

        float newPrev = 0;
        //float newDimPrev = 0;
        
        EntParameters parms = route.bot.navParams;
        
        if(newParent!=null) {
            newPrev = newParent.cost * parms.getParameter(EntParameters.ParamType.PrevCost);
        }

        float newLinkCost = 0;
        if(newLink!=null) {
            newLinkCost = calculateLinkCost(newLink);
        }
        
        //    1f, // 1 planWeight
        //    0f, // 2 plan bias (-200 to 200)
        //    1f, // 3 traversalWeight
        //    0f, // 4 traversal bias (-200 to 200)
        //    20f, // 5 deathWeight (0 to 200)
        //    0f, // 6 death bias (-200 to 200)
        float nodeCost = FastMath.max(node.getWeight(route.bot.team, 0) * parms.getParameter(EntParameters.ParamType.NodePlan) + parms.getParameter(EntParameters.ParamType.NodePlanBias),0);
        nodeCost += FastMath.max(node.getWeight(route.bot.team, 1) * parms.getParameter(EntParameters.ParamType.NodeTraversal) + parms.getParameter(EntParameters.ParamType.NodeTraversalBias),0);
        nodeCost += FastMath.max(node.getWeight(route.bot.team, 2) * parms.getParameter(EntParameters.ParamType.NodeDeath) + parms.getParameter(EntParameters.ParamType.NodeDeathBias),0);
        
        // change to estimate only to have something actually working   
        float priority = 
//                FastMath.max(estimate*parms.getParameter(EntParameters.ParamType.PriorityEstimate)+parms.getParameter(EntParameters.ParamType.PriorityEstimateBias),0)
//                +
                FastMath.max(newLinkCost*parms.getParameter(EntParameters.ParamType.PriorityLink)+parms.getParameter(EntParameters.ParamType.PriorityLinkBias),0)
                +FastMath.max(nodeCost*parms.getParameter(EntParameters.ParamType.PriorityNode)+parms.getParameter(EntParameters.ParamType.PriorityNodeBias),0)
                +FastMath.max(newPrev*parms.getParameter(EntParameters.ParamType.PriorityPrev)+parms.getParameter(EntParameters.ParamType.PriorityPrevBias),0)
                //+FastMath.max(newDimPrev*parms.getParameter(EntParameters.ParamType.PriorityPrev)+parms.getParameter(EntParameters.ParamType.PriorityPrevBias),0)
                ;
        return priority;
    }

    protected float calculateLinkCost( NavLink withLink) {
        if(withLink==null)
            return 0f;
        float dist = withLink.from.bounds.center.distance(withLink.to.bounds.center);
        float manh = withLink.from.bounds.center.manhattanDistance(withLink.to.bounds.center);
        float heightDist = FastMath.abs(withLink.from.bounds.center.y - withLink.to.bounds.center.y);
        float diff = manh - dist;
        
        // link weights
        // 7 from 0 distance, 1 manhattan distance
        // 8 distWeight
        // 9 height difference weight
        // 10 height difference bias (-200 to 200)
        // 11 stuckWeight (0 to 200)
        // 12 stuck bias (-200 to 200)
        
        EntParameters parms = route.bot.navParams;
        
        float own=0;
        // cost of the link as distance
        own = (dist + diff*parms.getParameter(EntParameters.ParamType.LinkDistanceType)) *parms.getParameter(EntParameters.ParamType.LinkDistance) ;
        // cost of the link as height
        own += FastMath.max(heightDist * parms.getParameter(EntParameters.ParamType.LinkHeightDiff) + parms.getParameter(EntParameters.ParamType.LinkHeightDiffBias),0);
        // if bot was stuck on this route
        own += FastMath.max(withLink.getAbsBotStuckCount()*parms.getParameter(EntParameters.ParamType.LinkStuck)+parms.getParameter(EntParameters.ParamType.LinkStuckBias),0);
        return own;
    }

    public boolean checkAndSwitch(RouteNode newp, NavLink newLink) {
        if(node!=newLink.to) {
            throw new JbException("checkAndSwitch with different node than previously");
        }
        float newCost = getCost(newp, newLink);
            
        if(newCost<cost) {
            if(parent==null) {
                throw new JbException("checkAndSwitch with root node");
            }
            parent = newp;
            depth = parent.depth + 1;
            linkTo = newLink;
            cost = newCost;
            return true;
        }
        return false;
    }

    protected void setEstimate() {
        // estimate to target area
        float dist = node.bounds.center.distance(route.targetNode.bounds.center, route.twoD);
        float manh = node.bounds.center.manhattanDistance(route.targetNode.bounds.center, route.twoD) - dist;
        EntParameters parms = route.bot.navParams;
        estimate = dist + (manh *  parms.getParameter(EntParameters.ParamType.EstimateDistanceType));
    }

    @Override
    public int compareTo(Object o) {
        RouteNode other = (RouteNode) o;
        int res = Float.compare(cost+estimate, other.cost+other.estimate);
        if(res == 0)
            res = Float.compare(estimate, other.estimate);
        return res;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + this.node.nodeid;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RouteNode other = (RouteNode) obj;
        if (this.node.nodeid != other.node.nodeid) {
            return false;
        }
        return true;
    }
    
}
