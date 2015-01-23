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
  

  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventThread)}
   * before first usage. Use {@link #relinquish()} to release the usage. 
   * 
   */
  public EventTimeout(){ super(); }

  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventTimeout(EventSource source, EventConsumer consumer, EventThreadIfc thread){
    super(source, consumer, thread);
  }
  
  
  /**Activates the timeout event to the given timestamp.
   * With this method the event or time order is enqueued in its thread given by construction.
   * @param date The absolute time stamp in seconds after 1970 UTC like given with {@link java.lang.System#currentTimeMillis()}.
   *   To set a relative time you must write <pre>
   *     myEvent.activateAt(System.currentTimeMillis() + delay);
   *   </pre>
   */
  public void activateAt(long date) {
    this.timeExecution = date;
    super.evDstThread.addTimeOrder(this);
  }
  
  
  public void activateAt(long date, long latest) {
    if(timeExecutionLatest ==0 && latest >0) {
      timeExecutionLatest = latest;  //set it only one time if requested.
    }
    if(timeExecution !=0 && ((timeExecution - System.currentTimeMillis()) < -5000)){ 
      //should be executed since 5 second, it hangs or countExecution was not called:
      timeExecution = 0;  //remove.
      evDstThread.removeTimeOrder(this);
    }
    if(timeExecution !=0 ){ //already added:
      if(timeExecutionLatest !=0 && (date - timeExecutionLatest) >0 ) return;  //do nothing because new after last execution.
      dbgctWindup +=1;
      //else: shift order to future:
      //remove and add new, because its state added in queue or not may be false.
      evDstThread.removeTimeOrder(this);
    }
    timeExecution = date;  //set it.
    evDstThread.addTimeOrder(this);

  }
  
  
  public boolean used(){ return timeExecution !=0; }
  

  
  /**Checks whether it should be executed.
   * @return time in milliseconds for first execution or value <0 to execute immediately.
   */
  public int timeToExecution(){ 
    return timeExecution == 0 ? -1 : (int)( timeExecution - System.currentTimeMillis()); 
  }
  

}
