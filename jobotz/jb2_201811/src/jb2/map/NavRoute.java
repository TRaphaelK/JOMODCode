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

import jb2.ent.BotEntity;
import jb2.ent.Entity;
import jb2.math.Vector3f;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class NavRoute {
    
    // 2d terrain level pathfinding
    public boolean twoD;
    // filter not yet traversed links
    public boolean onlyTraversed;
    
    // the bot for which this route is for
    public BotEntity bot;
    public NavCell start;
    public Entity targetEntity;
    //public BoundingBox targetArea;
    public NavCell targetNode;
    
    //public Vector3f targetPos;
    public float nearDistance;
    public RouteNode nearestNode;
    
    // route is ready 
    public boolean ready;
    // route finding failed
    public boolean unreachable;
    // bot started on the route
    //public boolean started=false;
    
    // the stack of destination nodes, next dest is on the end
    public FastList<NavCell> routeStack = new FastList<>();
    // the previous node in the list
    //public NavCell prevNode;
    // the current node in the list
    //public NavCell currentNode;
    // the node link the bot is currently traversing
    //public NavLink currentLink;
    
    
    public boolean isEmpty() {
        return routeStack.isEmpty();
    }

    
    public NavRoute makeCopyOfParameters() {
        NavRoute other = new NavRoute();
        other.twoD = twoD;
        other.onlyTraversed = onlyTraversed;
        other.bot = bot;
        other.start = start;
        other.targetEntity = targetEntity;
        //other.targetArea = targetArea;
        other.targetNode = targetNode;
        other.nearDistance = nearDistance;
        other.nearestNode = nearestNode;
        // TODO: add the other attributes to the clone too
        return other;
    }
}
