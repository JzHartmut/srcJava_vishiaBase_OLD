package org.vishia.states;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.util.DataAccess;

public class StateMachine implements EventConsumer
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
  
  StateComposite topState; 
  
  /**Map of all states defined as inner classes of the derived class, filled with reflection. */
  HashMap<Integer, StateSimple> stateMap = new HashMap<Integer, StateSimple>();
  

  public StateMachine(){ 
    //super();  //it initializes stateMap

    final StateSimple[] aSubstates;
    Class<?>[] innerClasses = this.getClass().getDeclaredClasses();
    if(innerClasses.length >0) {  //it is a composite state.
      aSubstates = new StateSimple[innerClasses.length];  //assume that all inner classes are states. Elsewhere the rest of elements are left null.
      topState = new StateComposite(this, aSubstates);
      int ixSubstates = -1;
      try{
        for(Class<?> clazz1: innerClasses) {
          if(DataAccess.isOrExtends(clazz1, StateSimple.class)) {
            Constructor<?>[] ctor1 = clazz1.getDeclaredConstructors();
            ctor1[0].setAccessible(true);
            Object oState = ctor1[0].newInstance(this);
            StateSimple state = (StateSimple)oState;
            aSubstates[++ixSubstates] = state;
            state.stateId = clazz1.getSimpleName();
            state.enclState = topState;
            int idState = clazz1.hashCode();
            this.stateMap.put(idState, state);
            if(topState.stateDefault == null){
              topState.stateDefault = state;  //The first state is the default one.
            }
          }
        }
        //after construction of all subStates: complete.
        topState.stateId = "StateTop";
        topState.buildStatePathSubstates(null,0);  //for all states recursively
        topState.createTransitionListSubstate(0);
      } catch(Exception exc){
        exc.printStackTrace();
      }   
    } else { //no inner states
      //this.aSubstates = null;
    }

    
    
  }

  
  /**Sets the path to the state for this and all {@link #aSubstates}, recursively call.
   * @param enclState
   * @param recurs
   */
  void buildStatePathSubstates() {
    //this.buildStatePath(topState);
    for(StateSimple subState: topState.aSubstates){
      if(subState instanceof StateComposite){
        ((StateComposite) subState).buildStatePathSubstates(topState, 1);
      } else {
        subState.buildStatePath(topState);
      }
    }
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
  
  
  public int processEvent(final Event<?,?> evP){ return topState._processEvent(evP); }
  
  
  public boolean isInState(Class<? extends StateSimple> stateClass){
    int id = stateClass.hashCode();
    StateSimple state = stateMap.get(id);
    if(state == null) throw new IllegalArgumentException("not a state class: " + stateClass.getCanonicalName());
    return state.isInState();
  }
  
  
  /**Gets the state instance to the State class inside this state machine.
   * The state instance can be used to ask {@link StateSimple#isInState()} or for some statistically checks
   * like {@link StateSimple#dateLastEntry} etc. 
   * @param stateClass write <code>getState(MyStateMachine.StateClass.InnerStateClass.class)</code>
   * @return null if the stateClass is faulty.
   */
  public StateSimple getState(Class<StateSimple> stateClass) {
    int id = stateClass.hashCode();
    StateSimple state = stateMap.get(id);
    return state;
  }
  
  
  /**Adds a timeout time to the given {@link EventTimerMng} with the given event {@link EventTimerMng.TimeEvent} of this class. 
   * @param date The timestamp in milliseconds after 1970 for the timeout.
   * @return The time order for debugging.
   */
  public EventTimerMng.TimeOrder timeout(long date){
    evTime.occupyRecall(null, true);
    return theTimer.addTimeOrder(date, evTime);
  }
  

}
