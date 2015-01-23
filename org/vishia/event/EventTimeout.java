package org.vishia.event;

public class EventTimeout extends EventWithDst
{
  private static final long serialVersionUID = 2695620140769906847L;

  /**If not 0, it is the time to execute it. Elsewhere it should be delayed. */
  protected long timeExecution;
  
  /**If not 0, it is the last time to execute it if the execution will be deferred by additional invocation of
   * {@link #activateAt(long)} while it is activated already. 
   */
  protected long timeExecutionLatest;

  /**It is counted only. Used for debug. Possible to set. */
  public int dbgctWindup = 0;
  

  /**Creates an event as a static object for re-usage. Use {@link #occupy(EventSource, EventConsumer, EventThread, boolean)}
   * before first usage. Use {@link #relinquish()} to release the usage.
   * Usual the parameterized method {@link EventTimeout#EventTimeout(EventSource, EventConsumer, EventThreadIfc)} 
   * should be used.
   */
  public EventTimeout(){ super(); }

  
  /**Creates an event as static or dynamic object for usage. See also {@link EventWithDst#EventWithDst(EventSource, EventConsumer, EventThreadIfc)}
   * The timeout instance may be static usual (permanent referenced) and not allocated on demand (with new)
   * because it is used whenever a special state is entered.
   * @param consumer The destination object for the event. If it is null nothing will be executed if the event is expired.
   * @param thread thread to handle the time order. It is obligatory.
   */
  public EventTimeout(EventConsumer consumer, EventThread thread){
    super(null, consumer, thread);
  }
  
  
  
  /**Activate the event for the given laps of time.
   * If the event is activated already for a shorter time, the activation time is deferred to this given time
   * but not later than a latest time given with {@link #activateAt(long, long)}. 
   * @param millisec if a negative value or a value less then 3 is given the event is processed immediately.
   * @see #activateAt(long, long).
   */
  public void activate(int millisec){ activateAt(System.currentTimeMillis() + millisec, 0);}
  
  /**Activates the timeout event to the given timestamp.
   * With this method the event or time order is enqueued in its thread given by construction.
   * @param date The absolute time stamp in seconds after 1970 UTC like given with {@link java.lang.System#currentTimeMillis()}.
   *   To set a relative time you must write <pre>
   *     myEvent.activateAt(System.currentTimeMillis() + delay);
   *   </pre>
   */
  public void activateAt(long date) { activateAt(date, 0); }
  
  
  /**Activate the event at the given absolute time 
   * If the event is activated already for a shorter time, the activation time is deferred to this given time
   * but not later than a latest time given with {@link #activateAt(long, long)}. 
   * @param date The time stamp.
   * @param latest The latest time stamp where the event should be processed though it is deferred.
   *   If the event is activated already for a earlier latest time, this argument is ignored. 
   *   The earlier latest time is valid. Use {@link #deactivate()} before this method to set the latest processing time newly. 
   * @see #activateAt(long, long).
   */
  public void activateAt(long date, long latest) {
    
    if(timeExecutionLatest ==0 && latest !=0) {
      timeExecutionLatest = latest;  //set it only one time if requested.
    }
    if(timeExecution !=0 && ((timeExecution - System.currentTimeMillis()) < -5000)){ 
      //should be executed since 5 second, it hangs or countExecution was not called:
      timeExecution = 0;  //remove.
      evDstThread.removeTimeOrder(this);
    }
    if(timeExecution ==0 ) {
      this.dateCreation.set(System.currentTimeMillis()); //instead occupy
    } else { 
      //already added:
      if(timeExecutionLatest !=0 && (date - timeExecutionLatest) >0 ) return;  //do nothing because new after last execution.
      dbgctWindup +=1;
      //else: shift order to future:
      //remove and add new, because its state added in queue or not may be false.
      evDstThread.removeTimeOrder(this);
    }
    timeExecution = date;  //set it.
    evDstThread.addTimeOrder(this);

  }
  
  
  /**Remove this from the queue of timer events and orders 
   */
  public void deactivate(){
    timeExecution = 0;
    timeExecutionLatest = 0;
    evDstThread.removeTimeOrder(this);
  }
  
  /**Returns the time stamp where the time is elapsed
   * @return milliseconds after 1970, 0 if the event is not activated yet.
   */
  public long timeExecution(){ return timeExecution; }
 
  
  /**Returns the time stamp where the time is elapsed latest.
   * @return milliseconds after 1970, 0 if the event is not activated yet.
   */
  public long timeExecutionLatest(){ return timeExecutionLatest; }
 
  

  
  public boolean used(){ return timeExecution !=0; }


  /**Checks whether it should be executed.
   * @return time in milliseconds for first execution or value <0 to execute immediately.
   */
  public int timeToExecution(){ 
    return timeExecution == 0 ? -1 : (int)( timeExecution - System.currentTimeMillis()); 
  }
  

}
