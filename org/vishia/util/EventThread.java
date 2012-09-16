package org.vishia.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**This class organizes the execution of events in a own thread. The class contains the event queue.
 * @author Hartmut Schorrig
 *
 */
public class EventThread implements Runnable, Closeable
{
  
  
  
  
  protected final ConcurrentLinkedQueue<Event> queueEvents = new ConcurrentLinkedQueue<Event>();

  /**The state of the thread
   * <ul>
   * <li>'.' non initialized.
   * <li>'s' started
   * <li>'r' Running, an event is processed
   * <li>'w' Waiting because the queue is empty
   * <li>'c' Should be closed, finished
   * <li>'x' finished.
   * 
   */
  protected char stateOfThread = '.';  
  

  protected final Thread thread;
  
  public EventThread(String threadName)
  {
    thread = new Thread(this, threadName);
  }
  
  
  public void startThread(){ thread.start(); }
  
  
  public void storeEvent(Event ev){
    ev.stateOfEvent = 'q';
    queueEvents.offer(ev);
    synchronized(this){
      if(stateOfThread == 'w'){
        notify();
      } else {
        //stateOfThread = 'c';
      }
    }
  }
  
  
  
  /**Removes this element from its queue if it is in the queue.
   * If the element is found in the queue, it is designated with 
   * @param ev
   * @return
   */
  public boolean removeFromQueue(Event ev){
    boolean found = queueEvents.remove(ev);
    if(found){ 
      ev.stateOfEvent = 'a'; 
    }
    return found;
  }
  
  
  
  @Override public void run()
  { stateOfThread = 'r';
    while(stateOfThread != 'x'){
      try{ //never let the thread crash
        Event event;
        if( (event = queueEvents.poll()) !=null){
          synchronized(this){
            if(stateOfThread != 'x'){
              stateOfThread = 'b'; //busy
            }
          }
          if(stateOfThread == 'b'){
            event.stateOfEvent = 'e';
            event.notifyDequeued();
            try{
              event.evDst().doprocessEvent(event);
            } catch(Exception exc) {
              System.err.println("Exception while processing an event: " + exc.getMessage());
              exc.printStackTrace(System.err);
            }
            event.relinquish();
          }
        } else {
          synchronized(this){
            if(stateOfThread != 'x'){  //exit?
              stateOfThread = 'w';      //w = waiting, notify necessary
              try{ wait(1000); } catch(InterruptedException exc){}
              if(stateOfThread == 'w'){ //can be changed while waiting, set only to 'r' if 'w' is still present
                stateOfThread = 'r';
              }
            }
          }
        }
      } catch(Exception exc){
        System.err.println("Unexpected exception " + exc.getMessage());
        exc.printStackTrace(System.err);
      }

    }
    stateOfThread = 'x';
  }


  @Override public void close() throws IOException
  { synchronized(this){
      if(stateOfThread == 'w'){
        notify();
      }
      stateOfThread = 'c';
    }  
  }
    
  
}
