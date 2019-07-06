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
package jb2.ent;

import jb2.AppContext;
import jb2.jo.GameObject;
import jb2.jo.TypeMap.TypeFolder;
import jb2.map.NavCell;
import jb2.math.BoundingBox;
import jb2.math.FastMath;
import jb2.util.Octree;

/**
 *
 * @author vear
 */
public class Entity extends GameObject {

    public TrackState prevState;
    public TrackState state= new TrackState();
    public Octree.Leaf parentNode;

    // timestamp of the last event
    protected long lastEventTime;
    protected long idleTime;

    // tracking, place navnodes where the player/bot/vehicle moves
    // unused for other entity categories
    protected NavCell prevNode;
    protected NavCell currentNode;

    public Entity(int itc, long ssnptr, long playerptr, boolean host) {
        super(itc, ssnptr, playerptr, host);
    }

    public Entity(int SSN) {
        super(SSN);
    }

    @Override
    public void refresh() {
        super.refresh();
        if (typeFolder == TypeFolder.Waypoint) {
            return;
        }
        
        // fill in the trackstate
        state.type = typeFolder.ordinal();
        state.SSN = SSN;
        state.typeId = typeId;
        state.changed = false;
        if (prevState == null) {
            state.changed = true;
        }
        state.position.set(position);
        //state.positionMorton = positionMorton;
        if (prevState == null || !state.position.issame(prevState.position)) {
            state.changed = true;
        }
        state.team = team;
        if (prevState != null && state.team == prevState.team) {
        } else {
            state.changed = true;
        }
        state.dead = this.dead;
        if (prevState != null && state.dead == prevState.dead) {
        } else {
            state.changed = true;
        }
        if (lastKiller != null) {
            state.killer_SSN = lastKiller.SSN;
        } else {
            state.killer_SSN = 0;
        }
        if (prevState != null && state.killer_SSN == prevState.killer_SSN) {
        } else {
            state.changed = true;
        }
        if (attachedTo != null) {
            state.attachedTo_SSN = attachedTo.SSN;
        } else {
            state.attachedTo_SSN = 0;
        }
        if (prevState != null && state.attachedTo_SSN == prevState.attachedTo_SSN) {
        } else {
            state.changed = true;
        }
        state.ftheta = ftheta;
        if (prevState == null || Math.abs(state.ftheta - prevState.ftheta) > 1f) {
            // only trigger state change when player
            if(this.isPlayer())
                state.changed = true;
        }
    }

    public void processObjectState() {

        if (prevState == null
                || state.changed) {

            // fill in the timestamp
            state.timestamp = AppContext.gameMatch.timestamp;
            // save state
            AppContext.stateFile.addEvent(state);
        }

        // dispatch events to entity listener
        processEvents();

        // only swap state if changed
        // this is needed so that position and ftheta events are triggered
        if (prevState == null) {
            prevState = new TrackState();
        }

        if(state.changed ){
            TrackState tmpTr = prevState;
            prevState = state;
            state = tmpTr;
        }
    }

    protected void processEvents() {
        // if its does not have event handlers
        if (!(this instanceof EntityEventHandler)) {
            return;
        }

        boolean hadEvent = false;
        EntityEventHandler evtHandler = (EntityEventHandler) this;

        if (prevState == null
                || state.changed) {

            // call events on the entity
            if (prevState == null) {
                evtHandler.startEvent();
                hadEvent = true;
            } else {
                if (state.team != prevState.team) {
                    evtHandler.teamChangedEvent();
                    hadEvent = true;
                }
                if (state.dead != prevState.dead) {
                    if (state.dead) {
                        evtHandler.deadEvent();
                        hadEvent = true;
                    } else {
                        evtHandler.aliveEvent();
                        hadEvent = true;
                    }
                } else if (state.dead) {
                    // do not trigger other events when dead
                } else if (state.attachedTo_SSN != prevState.attachedTo_SSN) {
                    if (state.attachedTo_SSN != 0) {
                        evtHandler.attachedEvent();
                        hadEvent = true;
                    } else {
                        evtHandler.detachedEvent();
                        hadEvent = true;
                    }
                } else if (!state.position.issame(prevState.position)
                        || (isPlayer() && Math.abs(state.ftheta - prevState.ftheta) > 1f)) {
                    // moved
                    // or player turned
                    evtHandler.movedEvent();
                    hadEvent = true;
                }
            }
        }

        if (hadEvent == true) {
            lastEventTime = AppContext.gameMatch.timestamp;
        } else {
            // should we wake up?
            if (!this.dead) {
                // only call idle when entity is alive
                long time = AppContext.gameMatch.timestamp - lastEventTime;
                long wakeuptime;
                if (idleTime != 0) {
                    wakeuptime = idleTime;
                } else {
                    wakeuptime = FastMath.rand.nextInt(2 * 1000);
                }
                if (time > wakeuptime) {
                    // reset idle time
                    idleTime = 0;
                    // wake-up event schedule in case idling
                    // minimum 5 seconds, and additional random 10 seconds
                    evtHandler.idleEvent();
                    // set event timestamp
                    lastEventTime = AppContext.gameMatch.timestamp;
                }
            }
        }
    }
}
