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

import java.util.logging.Level;
import jb2.gai.EntParameters;
import java.util.logging.Logger;
import jb2.AppContext;
import jb2.math.Vector3f;

/**
 *
 * @author vear
 */
public class BotPerformanceTracker implements EntityEventHandler {
    protected static final Logger log = Logger.getLogger(BotPerformanceTracker.class.getName());
    protected BotEntity parent;
    
    // the position where the bot respawned
    protected Vector3f startPosition=new Vector3f();
    protected Vector3f targetPosition=new Vector3f();

    // the time when the bot respawned
    protected long startTime;

    public BotPerformanceTracker(BotEntity parent) {
        this.parent = parent;
    }
        
    @Override
    public void startEvent() {
        //startEvaluation();
    }

    @Override
    public void movedEvent() {
        
    }

    @Override
    public void teamChangedEvent() {
        
    }

    @Override
    public void deadEvent() {
        endEvaluation();
    }

    @Override
    public void aliveEvent() {
        //startEvaluation();
    }

    @Override
    public void attachedEvent() {
        // evaluate performance
        endEvaluation();
    }

    @Override
    public void detachedEvent() {
        // init new parameters
        //startEvaluation();
    }

    @Override
    public void idleEvent() {
        /*
        if(parent.shooting)
            return;
        
        // if bot idling too long, then reset
        if(AppContext.gameMatch.timestamp - startTime > 120000) {
            // two minutes passed, assess and init a new genome
            endEvaluation();
            startEvaluation();
        }
        */
    }
    
    public void startEvaluation(Vector3f target) {
        // set target position
        targetPosition.set(target);

        // set start position
        startPosition.set(parent.position);
        startTime=AppContext.gameMatch.timestamp;
        
        parent.navParams = AppContext.optimizer.getEntParameters();
    }

    public void endEvaluation() {
        // if a bot is stuck, is dead, or reaches a target LPF the first time after dieing evaluate its XP
        // then request new parameters
        
        // calculate the score
        EntParameters parms = parent.navParams;
        if(parms==null)
            return;
        parent.navParams = null;
        
        float totalDist = targetPosition.distance2d(startPosition);
        float nearDist = targetPosition.distance2d(parent.position);
        float movedDist = startPosition.distance2d(parent.position);

        // ignore if total distance is short
        if(totalDist<30)
            return;
        // ignore if moved distance is short
        if(movedDist<30)
            return;
        // if further away than was previously, no xp
        if(nearDist > totalDist)
            return;
        parms.xp =  ( nearDist / totalDist ) * 100f;
        // if relative distance is also short
        if(parms.xp<50) {
            return;
        }
        log.log(Level.INFO, "Added param settings with XP {0}", parms.xp);
        AppContext.optimizer.addEntParameters(parms);
    }
}
