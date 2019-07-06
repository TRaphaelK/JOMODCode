/*
 * 
 * Vear 2017  * 
 */
package jb2.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import jb2.AppContext;
import static jb2.map.RouteFinder.log;

/**
 *
 * @author vear
 */
public abstract class ThreadTask implements Runnable {

    protected static final Logger log = Logger.getLogger(ThreadTask.class.getName());
    
    protected boolean running = false;
    protected String name;
    protected long waitTime = 0;
    
    private static final class Lock { }
    private final Object lock = new Lock();
    
    public ThreadTask(String name) {
        this.name = name;
    }
    
    public void stopThread() {
       // signal that thread is to be stopped
        synchronized(lock) {
            lock.notify();
        }
    }
    
    public void startThread() {
        if(running) {
            throw new JbException("Can't start "+name+" thread, already running");
        }
        Thread newThread = new Thread(this);
        newThread.start();
    }
        
    @Override
    public void run() {
        if(running) {
            log.log(Level.SEVERE, name + " thread already running");
            return;
        }

        log.log(Level.INFO, name + " thread started");
        
        while(!AppContext.gameMatch.exit) {
            try {
                doWork();

                if(AppContext.gameMatch.exit)
                    break;
                synchronized(lock) {
                    while(!shouldWakeUp()) {
                        lock.wait(waitTime);
                    }
                }
            } catch(InterruptedException ie) {
            } catch(Exception e) {
                log.log(Level.SEVERE, "Exception in "+name+" thread", e);
                // propage error to main thread
                AppContext.gameMatch.exit = true;
            }
        }
        
        log.log(Level.INFO, name+" thread exiting");
    }
    
    protected abstract boolean shouldWakeUp();
    protected abstract void doWork();
    
    protected void notifyWork() {
        if(!running) {
            // if finder thread is not yet running
            startThread();
        } else {
            synchronized(this) {
                this.notify();
            }
        }
    }
}
