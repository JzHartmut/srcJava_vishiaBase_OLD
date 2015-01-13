package org.vishia.event;

public class EventTimeout extends EventWithDst
{
  private static final long serialVersionUID = 2695620140769906847L;

  /**If not 0, it is the first time to execute it. Elsewhere it should be delayed. */
  protected long timeExecution;
  
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
  
  
  /**Checks whether it should be executed.
   * @return time in milliseconds for first execution or value <0 to execute immediately.
   */
  public int timeToExecution(){ 
    return timeExecution == 0 ? -1 : (int)( timeExecution - System.currentTimeMillis()); 
  }
  

}
