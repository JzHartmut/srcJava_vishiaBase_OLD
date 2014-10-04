package org.vishia.states;

import java.util.HashMap;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;

public class StateTop extends StateComposite implements EventConsumer
{
  /**Aggregation to the used event queue or the thread for this statemachine.
   * It can be used to send events to from outer.
   * Note: Do not send events from any other thread to this directly.
   * The state machine should processed in only one thread.
   * But this aggregation may be null if the state machine will be processed in a users thread.
   */
  EventThread theThread;
  
  /**Aggregation to the used timer for time events. See {@link #addTimeOrder(long)}.
   * It may be null if queued time events are not necessary for this.
   */
  EventTimerMng theTimer;
  
  EventTimerMng.TimeEvent evTime;
  
  
  /**Map of all states defined as inner classes of the derived class, filled with reflection. */
  HashMap<Integer, StateSimple> stateMap; // = new HashMap<Integer, StateSimple>();
  

  public StateTop(){ 
    super();  //it initializes stateMap
    stateId = "StateTop";
    buildStatePathSubstates(null,0);  //for all states recursively
    createTransitionListSubstate(0);
  }

  
  /**Sets the timer manager and a optional thread for the event queue after construction.
   * This method should be called only one time for any object. Elsewhere a IllegalStateException is thrown. 
   * @param timer Source for timer events. It is necessary if the state machine uses timer events.
   * @param queue Queue for events. Left null if the state machine should be invoked in the same thread where the events are created.
   */
  public void setTimerAndThread(EventTimerMng timer, EventThread queue){
    if(theTimer !=null) throw new IllegalStateException("setTimeMng() of a StateTop should be invoked only one time");
    theTimer = timer;
    theThread = queue;
    evTime = new EventTimerMng.TimeEvent(this, theThread, 0);
  }
  
  
  public int processEvent(final Event<?,?> evP){ return super._processEvent(evP); }
  
  
  /**Adds a timeout time to the given {@link EventTimerMng} with the given event {@link EventTimerMng.TimeEvent} of this class. 
   * @param date The timestamp in milliseconds after 1970 for the timeout.
   * @return The time order for debugging.
   */
  public EventTimerMng.TimeOrder timeout(long date){
    evTime.occupyRecall(null, true);
    return theTimer.addTimeOrder(date, evTime);
  }
  

}
