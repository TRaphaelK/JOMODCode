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
import jb2.AppContext;
import static jb2.ent.PlayerEntity.log;
import jb2.math.FastMath;

/**
 *
 * @author vear
 */
public class BotWaitBehavior implements EntityEventHandler {
    protected static final Logger log = Logger.getLogger(BotWaitBehavior.class.getName());
    protected BotEntity parent;
    // bot is ordered to stay put for some time
    // during this time no new routes are searched for
    protected long stayPutStartTime=0;
    protected long stayPutDuration=0;
    // the desired next state
    protected BotEntity.BotState nextState;
    
    public BotWaitBehavior(BotEntity parent, BotEntity.BotState state) {
        this.parent = parent;
        this.nextState = state;
    }

    @Override
    public void startEvent() {
        stayPutDuration=FastMath.rand.nextInt(3*1000);
        log.log(Level.INFO, "Bot {0} stays put for {1} ms", 
                        new Object[]{
                            parent.SSN, 
                            this.stayPutDuration
                        });
       stayPutStartTime=AppContext.gameMatch.timestamp;
       parent.idleTime = stayPutDuration;
       // stop the bot from moving around
       parent.setStop();
    }

    @Override
    public void movedEvent() {
        
    }

    @Override
    public void teamChangedEvent() {
        
    }

    @Override
    public void deadEvent() {
        
    }

    @Override
    public void aliveEvent() {
        
    }

    @Override
    public void attachedEvent() {
        
    }

    @Override
    public void detachedEvent() {
        findWorkToDo();
    }

    @Override
    public void idleEvent() {
        findWorkToDo();
    }
    
    protected void findWorkToDo() {
        if(parent.attachedTo!=null) {
            // even if time expired, wait since the bot is attached
            return;
        }

        long stayPutWaited = AppContext.gameMatch.timestamp-stayPutStartTime;
        if(stayPutWaited<stayPutDuration)
            return;
        stayPutDuration = 0;
        // continue to next state
        parent.botState = nextState;
    }
}
