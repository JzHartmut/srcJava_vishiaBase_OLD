package org.vishia.event;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.util.Assert;

/**This class organizes the execution of events in a own thread. The class contains the event queue.
 * @author Hartmut Schorrig
 *
 */
public class EventThread implements Runnable, Closeable
{
  /**Version, history and license.
   * <ul>
   * <li>2013-05-12 Hartmut chg: Now the thread starts and ends automatically if {@link #startThread()}
   *   is not invoked.
   * <li>2012...improved
   * <li>2011-12-27 Hartmut creation for event concept.
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final int version = 20130513;

  
  
  
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
  
  protected final String threadName;

  protected Thread thread;
  
  private boolean startOnDemand;
  
  private int ctWaitEmptyQueue;
  
  protected int maxCtWaitEmptyQueue = 5;
  
  public EventThread(String threadName)
  {
    this.threadName = threadName;
  }
  
  
  /**Creates and starts the thread. If this routine is called from the user, the thread runs
   * till the close() method was called. If this method is not invoked from the user,
   * the thread is created and started automatically if {@link #storeEvent(Event)} was called.
   * In that case the thread stops its execution if the event queue is empty and about 5 seconds
   * are gone.  */
  public void startThread(){ 
    thread = new Thread(this, threadName);
    startOnDemand = false;
    thread.start(); 
  }
  
  
  public void storeEvent(Event ev){
    ev.stateOfEvent = 'q';
    queueEvents.offer(ev);
    if(thread == null){
      startThread();
      startOnDemand = true;
    } else {
      synchronized(this){
        if(stateOfThread == 'w'){
          notify();
        } else {
          //stateOfThread = 'c';
        }
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
    do { 
      try{ //never let the thread crash
        Event event;
        if( (event = queueEvents.poll()) !=null){
          this.ctWaitEmptyQueue = 0;
          synchronized(this){
            if(stateOfThread != 'x'){
              stateOfThread = 'b'; //busy
            }
          }
          if(stateOfThread == 'b'){
            event.stateOfEvent = 'e';
            event.notifyDequeued();
            try{
              event.donotRelinquish = false;
              event.evDst().processEvent(event);
            } catch(Exception exc) {
              System.err.println("Exception while processing an event: " + exc.getMessage());
              exc.printStackTrace(System.err);
            }
            event.relinquish();
          }
        } else {
          if(!startOnDemand   //wait anytime 
            || ++this.ctWaitEmptyQueue < this.maxCtWaitEmptyQueue //or count the empty wait cycles.
            ){
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
        }
      } catch(Exception exc){
        System.out.println(Assert.exceptionInfo("EventThread - unexpected Exception; ", exc, 0, 7));
        exc.printStackTrace(System.err);
      }

    } while(stateOfThread != 'c' && this.ctWaitEmptyQueue < this.maxCtWaitEmptyQueue);
    stateOfThread = 'x';
    thread = null;
  }

  
  /**Returns true if the current thread is the thread which is aggregate to this EventThread.
   * It means the {@link #run()} method has called this method.
   * @return false if a statement in another thread checks whether this EventThread runs.
   */
  public boolean isCurrentThread() {
    return thread == Thread.currentThread();
  }
  
  public char getState(){ return stateOfThread; }

  @Override public void close() throws IOException
  { synchronized(this){
      if(stateOfThread == 'w'){
        notify();
      }
      stateOfThread = 'c';
    }  
  }
    
  
}
