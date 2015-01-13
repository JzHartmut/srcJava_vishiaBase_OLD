package org.vishia.event;


import org.vishia.util.Assert;


public class EventTimerMng extends TimeOrderMng {

  
  /**Version, history and license.
   * <ul>
   * <li>2014-10-17 Hartmut chg: {@link TimeEventOrder} now non-static, knows it EventTimeMng. Possible static usage of instance. 
   * <li>2013-05-12 Hartmut new: {@link #identNrEvent} and {@link TimeEvent#isMatchingto(TimeEventOrder)}
   * <li>2013-05-11 Hartmut new: {@link #addTimeOrder(TimeEventOrder)} not only for singleton.
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

  public static class TimeEvent extends EventCmdType<TimeEvent.Cmd>{
    enum Cmd{Time};
    
    final int identNrEvent;
    
    /**The constructor
     * @param dst A destination object should be given
     * @param thread The destination thread may be null, then the {@link EventConsumer#processEvent(EventCmdType)} method
     *   is called in the timer thread.
     */
    public TimeEvent(EventConsumer dst, EventThread thread, int identNrEvent){
      super(null, dst, thread);
      this.identNrEvent = identNrEvent;
    }
    
    /**Checks whether the event is a TimeEvent.
     * @param ev The untyped event
     * @return null if the event does not match the type, elsewhere the casted event.
     */
    public static TimeEvent typeof(EventCmdType<?> ev){
      if(ev instanceof TimeEvent){
        return (TimeEvent)(ev);
      } else {
        return null;   //causes an NullPointerException if not expected from caller.
      }
    }
  
    /**Checks whether the received event is matching to the created time order.
     * An event can be received from an older time order if it is not successfully removed
     * from an exitAction of any state which has created the time order as its timeout.
     * It should prevented that a non matching event uses for timeout.
     * Use the following pattern for timeouts in states:
     * <pre>
     * MyState state ...{
     *   private EventTimerMng.TimeOrder timeOrder;
     *   ...
     *  protected void entryAction(Event<?,?> ev){
     *    timeOrder = stateTop.addTimeOrder(System.currentTimeMillis() + delay);
     *  ...  
     *  protected void exitAction(){
     *    if(timeOrder !=null){ timer.removeTimeOrder(timeOrder); }
     *  ....  
     *  private int transXy(EventTimerMng.TimeEvent ev){
     *    if(ev !=null && ev.isMatchingto(timeOrder)){
     *      timeOrder = null;
     * </pre>
     * @param order
     * @return true if it is the proper event to the time order.
     */
    public boolean isMatchingto(TimeEventOrder order){ return identNrEvent == order.identNrEvent; }
    
  }


  public class TimeEventOrder extends TimeOrderBase
  {
    /**Absolute time when the event should be occurred. */
    //long dateEvent;
   
    final int identNrEvent;
    
    final TimeEvent event;
    
    //final EventConsumer dst;
    
    //final EventThread threadDst;
    
    //private boolean used;
    
    TimeEventOrder(TimeEvent ev, long date, int identNrEvent){
      super("EventTimeOrder " + identNrEvent);
      this.timeExecution = date;
      this.event = ev;
      //this.dst = null;  //stored in ev
      //this.threadDst = null;
      this.identNrEvent = identNrEvent;
    }

    TimeEventOrder(long date, EventConsumer dst, EventThread dstThread, int identNrEvent){
      super("EventTimeOrder " + identNrEvent);
      this.timeExecution = date;
      this.event = new TimeEvent(dst, dstThread, identNrEvent);  //should be create if need.
      //this.dst = dst;  //stored in ev
      //this.threadDst = dstThread;
      this.identNrEvent = identNrEvent;
    }

    public TimeEventOrder(EventConsumer dst, EventThread dstThread){
      super("EventTimeOrder ");
      this.timeExecution = 0;
      this.event = new TimeEvent(dst, dstThread, 0);
      //this.dst = dst;  //stored in ev
      //this.threadDst = dstThread;
      this.identNrEvent = 0;
    }

    
    public void activate(long date) {
      this.timeExecution = date;
      EventTimerMng.this.addTimeOrder(this);
    }
    
    //void setUsed() { used = true; }
    
    //void setUnused(){ used = false; } 
    
    //public boolean used() { return used; }
    
    
    /**Sends the time event to its destination maybe in the queue of the destination thread.
     * The source of occupy is the @link {@link EventSource} of this instance.
     */
    public void executeOrder(){
      event.occupy(evSource, true);
      event.sendEvent(TimeEvent.Cmd.Time);
    }

  }
  
  
  final EventSource evSource = new EventSource("TimerMng"){
    
  };
  
  private static EventTimerMng singleton;
  
  
  int identNrEvent;
  
  
  /**The next time where the thread will be release its wait(millisec) routine without extra notifying.  
   * If a new order will be added which's end time is before that, the timer thread will be notified
   * to accept this early timestamp. 
   */
  //long dateCheck;
  
  
  public EventTimerMng(String sNameThread){
    super(true);
    start();
  }
  
  
  public static TimeEventOrder addGlobalTimeOrder(long date, TimeEvent evTime){
    if(singleton == null){
      singleton = new EventTimerMng("EventTimerMng");
    }
    return singleton.addTimeOrder(date, evTime);
  }
  
  
  public static TimeEventOrder addGlobalTimeOrder(long date, EventConsumer dst, EventThread thread){
    if(singleton == null){
      singleton = new EventTimerMng("EventTimerMng");
    }
    return singleton.addTimeOrder(date, new TimeEvent(dst, thread, ++singleton.identNrEvent));
  }
  

  public TimeEventOrder addTimeOrder(long date, EventConsumer dst, EventThread dstThread){
    TimeEventOrder order = new TimeEventOrder(date, dst, dstThread, ++this.identNrEvent);
    addTimeOrder(order);
    return order;
  }

  
  
  public TimeEventOrder addTimeOrder(long date, TimeEvent evTime){
    if(evTime !=null){
      Assert.checkMsg (evTime instanceof TimeEvent, "The Event should be a org.vishia.util.EventTimerMng.TimeEvent");
      Assert.checkMsg (evTime.hasDst(), "The Event must have a destination.");
      Assert.checkMsg (evTime.isOccupied(), "The Event must be occupied.");
    }
    TimeEventOrder entry = new TimeEventOrder(evTime, date, ++this.identNrEvent);
    addTimeOrder(entry);
    return entry;
  }
  
  
}
