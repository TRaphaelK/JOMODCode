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

import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.jo.TypeMap;

/**
 * PlayerEntity tracks player movement and records it for later evaluation.
 * @author vear
 */
public class PlayerEntity extends Entity implements EntityEventHandler {
    
    protected static final Logger log = Logger.getLogger(PlayerEntity.class.getName());
            
    // the entity tracker for the player
    protected EntityTracker tracker;
    
    // bounding box around the player position, used to find already existing nodes
    
    public PlayerEntity(int itc, long ssnptr, long playerptr) {
        super(itc, ssnptr, playerptr, false);
        typeFolder = TypeMap.TypeFolder.Player;
        tracker=new EntityTracker(this);
    }
    
    @Override
    public void startEvent() {
        tracker.startEvent();
    }
    
    @Override
    public void movedEvent() {
        tracker.movedEvent();
    }
    
    @Override
    public void teamChangedEvent() {
        tracker.teamChangedEvent();
        log.log(Level.INFO, "Player {0} changed team to {1}", 
                new Object[]{
                    this.name, 
                    this.team
                });
    }
    
    @Override
    public void deadEvent() {
        log.log(Level.INFO, "Player {0} died", 
            this.name
        );
        tracker.deadEvent();
    }
    
    @Override
    public void aliveEvent() {
        log.log(Level.INFO, "Player {0} respawned", 
            this.name
        );
        tracker.aliveEvent();
    }
    
    @Override
    public void attachedEvent() {
        log.log(Level.INFO, "Player {0} attached to {1}", 
            new Object[]{
                this.name,
                this.attachedTo.name
            }
        );        
        tracker.attachedEvent();
    }
    
    @Override
    public void detachedEvent() {
        log.log(Level.INFO, "Player {0} detached", 
            this.name
        );
        tracker.detachedEvent();
    }

    @Override
    public void idleEvent() {
        tracker.idleEvent();
    }

}
