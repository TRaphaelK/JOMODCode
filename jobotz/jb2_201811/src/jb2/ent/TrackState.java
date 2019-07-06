/*
 * 
 * Vear 2017  * 
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
package jb2.ent;

import jb2.jo.TypeMap;
import jb2.math.Vector3f;

/**
 * Track state for players, bots and vehicles and game targets
 * @author vear
 */
public class TrackState {
    // we have a timestamp so that when an event is triggered we know how much time has passed
    public long timestamp;
    public int type;
    public int SSN;
    public int typeId;
    public int lfpGroup;
    public int lfpCampState;
    
    public boolean changed;
    public final Vector3f position = new Vector3f();
    //public int positionMorton;
    
    public int team;
    
    public boolean dead;
    
    public int killer_SSN;
    
    public int attachedTo_SSN;
    
    public float ftheta;
}
