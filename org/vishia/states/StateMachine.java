package org.vishia.states;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;
import java.util.HashMap;

import org.vishia.event.EventConsumer;
import org.vishia.event.EventCmdtype;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.util.DataAccess;
import org.vishia.util.InfoAppend;

/**
 * To build a state machine you should build a maybe inner class derived from {@link org.vishia.states.StateMachine}. This class builds the frame for all states.
 * Any state should be defined as inner class derived from either ...or  
 * <ul>
 * <li>{@link org.vishia.states.StateSimple}: A simple state with transitions, an optional entry- and an exit-action.
 * <li>{@link org.vishia.states.StateComposite}: It contains more as one sub states.
 * <li>{@link org.vishia.states.StateParallel}: It contains more as one {@link org.vishia.states.StateParallel} which are {@link org.vishia.states.StateComposite} 
 * for parallel behavior.
 * </ul>
 * See example pattern {@link org.vishia.states.example.StateExampleSimple}.
 * <br><br>
 * Any state can contain an entry- and an exit action which can be build either ... or with:
 * <ul>
 * <li>Overriding the method {@link org.vishia.states.StateSimple#entry(org.vishia.event.EventCmdPingPongType)} respectively {@link StateSimple#exit()}.
 * <li>Set the association {@link org.vishia.states.StateSimple#entry} respectively {@link org.vishia.states.StateSimple#exit} 
 * with an Implementation of {@link org.vishia.states.StateAction}
 *   respectively {@link java.lang.Runnable} which can be located in any other class, usual as inner anonymous class. 
 *   In that cases the entry and exit can use data from any other class.
 * </ul>  
 * <br><br>
 * Any state can contain some transitions which can be build either ... or with:
 * <ul>
 * <li>An inner class derived from {@link org.vishia.states.StateSimple.Trans}.
 * <li>An instance of an anonymous inner derived class of {@link org.vishia.states.StateSimple.Trans}.
 * <li>An method with return value {@link org.vishia.states.StateSimple.Trans} 
 *   and 2 arguments {@link org.vishia.event.EventCmdPingPongType} and {@link org.vishia.states.StateSimple.Trans}
 *   which contains the creation of a StateTrans object, the condition test, an action and the {@link org.vishia.states.StateSimple.Trans#doExit()}
 *   and {@link org.vishia.states.StateSimple.Trans#doEntry(org.vishia.event.EventCmdPingPongType)} invocation. This form shows only a simple method
 *   in a browser tree of the source code (outline in Eclipse).  
 * </ul>
 * The three forms of transition phrases gives possibilities for more or less complex transitions:
 * <ul>
 * <li>A simple transition with simple or without action, only with a state switch: Use a simple transition method.
 * <li>A transition which should call an action given with association to another instance: Use a derived transition class.
 * <li>A transition can have choice sub-transitions.
 * <li>A transition can fork to more as one parallel states.
 * <li>A transition can join parallel states.
 * <li>The difference between an anonymous derived instance and a derived class is less.
 * </ul>
 * On construction of the whole {@link StateMachine} all state classes are instantiated and listed in the StateMachine. 
 * All transitions are evaluated and transformed in a unified list of transition objects.
 * For all transitions the necessary exitState()- and entryState() operations are detected and stored, so that complex state switches
 * are executed correctly. The designation of destination states in the written source code uses the class names of the states. 
 * The transition evaluation process on construction searches and lists the instances of the states.
 * <br><br>
 * A state instance can be gotten with {@link org.vishia.states.StateMachine#getState(Class)}
 * <br><br>
 * The {@link StateMachine} can be quested whether a state is active yet: {@link org.vishia.states.StateMachine#isInState(Class)} 
 * or {@link StateMachine#isInState(Class)}
 * <br><br>
 * <br>Creation of a StateMachine with or without a timer and an event queue</b>:<br>
 * One can execute the state machine in 2 modes:
 * <ul>
 * <li>State execution inside a thread which processes events. That thread can process more as one state machine. 
 *   The limit is: If many events can be occured, the execution of all state transitions should be able to done in a limited time. 
 *   The advantage is: All state machines are executed in the same thread.
 *   The transitions are <em>fibers</em> of that thread. They are non-interrupt-able. If more as one state machines work with the same data,
 *   no additional mutexes are necessary.
 * <li>State execution inside the calling thread. That is the more simple variant. 
 *   Events to trigger the state machine are possible but not necessary. The user should have control over mutual exclusion access to data 
 *   if some more threads are used.  
 * </ul>
 * In the first case an instance of {@link EventTimerThread#EventThread(String)} are necessary which is a parameter for the constructor
 * {@link #StateMachine(EventTimerThread, EventTimerMng)}.
 * <br><br>
 * If an {@link EventTimerThread} is used an instance of {@link EventTimerMng} is possible and recommended . 
 * The timer manager can manage any number of time orders.
 * It sends an {@link EventTimerMng.TimeEvent} to this StateMachine it the time is expired.
 * <br><br>
 * The state machine can be animated using {@link org.vishia.states.StateMachine#applyEvent(EventMsg2)} with or without events
 * by given a null argument, for example cyclically if an {@link EventTimerThread} is not used.
 * <br><br>
 * 
 * To see how transitions and timeouts should be written see on {@link StateSimple.Trans} and {@link StateSimple.Timeout}.
 * 
 * @author hartmut Schorrig
 *
 */
public class StateMachine implements EventConsumer, InfoAppend
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-01-11 Hartmut chg:  
   * <li>2014-12-30 Hartmut chg: test, gardening 
   * <li>2014-10-12 Hartmut chg: Not does not inherit from {@link StateComposite}, instance of {@link #topState}. 
   * <li>2014-09-28 Hartmut chg: Copied from org.vishia.stateMachine.TopState, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
   * <li>2013-04-07 Hartmut adapt: Event<?,?> with 2 generic parameter
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2001 in C-language and Java.
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public static final String version = "2014-10-14";

  
  /**If set true, then any state transition is logged with System.out.printf("..."). One can use the 
   * {@link org.vishia.msgDispatch.MsgRedirectConsole} to use a proper log system. 
   */
  protected boolean debugState = false;
  
  
  /**True then permits exceptions in one state transition. send a log output to System.err but check other transitions. */
  public boolean permitException;
  
  /**Aggregation to the used event queue or the thread for this statemachine.
   * It can be used to send events to from outer.
   * Note: Do not send events from any other thread to this directly.
   * The state machine should processed in only one thread.
   * But this aggregation may be null if the state machine will be processed in a users thread.
   */
  final EventTimerThread theThread;
  
  
  final EventWithDst triggerEvent;
  
  
  /**Aggregation to the used timer for time events. See {@link #addTimeOrder(long)}.
   * It may be null if queued time events are not necessary for this.
   */
  //final EventTimerMng theTimer;
  
  //EventTimerMng.TimeEvent evTime;
  
  protected final StateCompositeTop topState; 
  
  /**Map of all states defined as inner classes of the derived class, filled with reflection. */
  HashMap<Integer, StateSimple> stateMap = new HashMap<Integer, StateSimple>();
  
  private final String name;

  protected static class StateCompositeTop extends StateComposite
  {
    StateCompositeTop(StateMachine stateMachine, StateSimple[] aSubstates, StateComposite[] aParallelstates) { super(stateMachine, aSubstates, aParallelstates); } 
    
    
    public void prepare() {
      buildStatePathSubstates(null,0);  //for all states recursively
      
      //createTransitionListSubstate(0);
    }
  }
  
  
  
  /**Creates a state machine which is executed directly by {@link #processEvent}. {@link StateSimple.Timeout} is not possible.
   * 
   */
  public StateMachine(String name) { this(name, null);}
  
  /**Constructs a state machine with a given thread and a given timer manager.
   * The constructor of the whole stateMachine does the same as the {@link StateComposite#StateComposite()}: 
   * It checks the class for inner classes which are the states.
   * Each inner class which is instance of {@link StateSimple} is instantiated and stored both in the {@link #stateMap} to find all states by its class.hashCode
   * and in the {@link #topState} {@link StateComposite#aSubstates} for debugging only.
   * <br><br>
   * After them {@link StateComposite#buildStatePathSubstates()} is invoked for the topstate and recursively for all states 
   * to store the state path in all states.
   * Then {@link StateComposite#createTransitionListSubstate(int)} is invoked for the {@link #topState} 
   * which checks the transition of all states recursively. Therewith all necessary data for the state machines's processing
   * are created on construction. 
   * 
   * @see StateComposite#StateComposite()
   * @see StateComposite#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateComposite#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   * 
   * @param thread if given all events are stored in the thread's event queue, the state machine is executed only in that thread.
   * @param if given timer events can be created. 
   */
  public StateMachine(String name, EventTimerThread thread) //, EventTimerMng timer)
  { this.name = name;
    this.theThread = thread;
    //this.theTimer = timer;
    if(thread !=null){
      triggerEvent = new EventWithDst(null, this, theThread);
    } else {
      triggerEvent = null;
    }
    final StateSimple[] aSubstates;
    Class<?>[] innerClasses = this.getClass().getDeclaredClasses();
    if(innerClasses.length ==0) throw new IllegalArgumentException("The StateMachine should have inner classes which are the states.");  //expected.
    aSubstates = new StateSimple[innerClasses.length];  //assume that all inner classes are states. Elsewhere the rest of elements are left null.
    topState = new StateCompositeTop(this, aSubstates, null);
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
          state.stateMachine = this;
          state.enclState = topState;
          int idState = clazz1.hashCode();
          this.stateMap.put(idState, state);
          try { 
            clazz1.getDeclaredField("isDefault");
            if(topState.stateDefault != null){ 
              throw new IllegalArgumentException("StateMachine - more as one default state in;" + topState.stateId); 
            }
            topState.stateDefault = state;  //The first state is the default one.
          } catch(NoSuchFieldException exc){} //empty!
        }
      }
      if(topState.stateDefault == null){ 
        throw new IllegalArgumentException("StateMachine - a default state is necessary. Define \"final boolean isDefault = true\" in one of an inner class State;" + topState.stateId); 
      }
      //after construction of all subStates: complete.
      topState.stateId = "StateTop";
      topState.buildStatePathSubstates(null,0);  //for all states recursively
      topState.createTransitionListSubstate(0);
    } catch(InvocationTargetException exc){
      Throwable exc1 = exc.getCause();
      if(exc1 !=null) throw new RuntimeException(exc1);
    } catch(Exception exc){ 
      throw new RuntimeException(exc);
    }
  }

  
  
  /**Special constructor for StateMGen, with given topState, without reflection analysis. 
   * @param topState
   */
  protected StateMachine(StateSimple[] aSubstates) {
    this.name = "StateMachine";
    this.topState = new StateCompositeTop(this, aSubstates, null);
    //theTimer = null;
    triggerEvent = null;
    theThread = null;
  }
  
  

  /**Triggers a run cycle for the statemachine in the execution thread
   * or runs the state machine immediately if the execution thread is this thread or the execution thread is not given.
   * @param val
   * @return
   */
  public boolean triggerRun(){
    return processEvent(triggerEvent) !=0;  //runs immediately if theThread ==null, then triggerEvent == null is not used.
    /*
    if(ixInThread >=0) {
      theThread.shouldRun(ixInThread);
      return true;
    }
    else return processEvent(null) !=0;
    */
  }
  
  

  /**Gets the state instance to the State class inside this state machine.
   * The state instance can be used to ask {@link StateSimple#isInState()} or for some statistically checks
   * like {@link StateSimple#dateLastEntry} etc. 
   * @param stateClass write <code>getState(MyStateMachine.StateClass.InnerStateClass.class)</code>
   * @return null if the stateClass is faulty.
   */
  public StateSimple getState(Class<? extends StateSimple> stateClass) {
    int id = stateClass.hashCode();
    StateSimple state = stateMap.get(id);
    return state;
  }


  public boolean isInState(Class<? extends StateSimple> stateClass){
    int id = stateClass.hashCode();
    StateSimple state = stateMap.get(id);
    if(state == null) throw new IllegalArgumentException("not a state class: " + stateClass.getCanonicalName());
    return state.isInState();
  }


  
  /**This method can be overridden if the moment of applying an event should be debugged
   * or for example logged in the derived class. It should be done with the pattern:
   * <pre>
   * (at)Override public int eventToTopDebug(EventObject ev) {
   *   //any statements ...
   *   return super.eventToTopDebug(ev);
   * }
   * </pre>
   * @param ev The event from user or from queue
   * @return the return value of {@link StateComposite#processEvent}
   */
  public int eventToTopDebug(EventObject ev) {
    return topState._processEvent(ev); 
  }
  

  /**Applies an event to this state machine. This method is invoked from {@link EventMsg2#sendEvent(Enum)} if this class is given
   * as {@link EventConsumer}. If the statemachine is aggregated with a {@link EventTimerThread} and this routine is invoked from another thread
   * then the event will be stored in {@link #theThread}. It is done if the transmitter of the event does not know about the EventThread.
   * @see org.vishia.event.EventConsumer#processEvent(org.vishia.event.EventCmdPingPongType)
   */
  @Override public int processEvent(EventObject ev)
  { if(theThread == null || theThread.isCurrentThread()) {
      return eventToTopDebug(ev); 
    } else if(ev !=null){
      if(ev instanceof EventWithDst){
        EventWithDst ev1 = (EventWithDst)ev;
        //ev1.donotRelinquish();  
        ev1.setDst(this);  //only this may be the destination of the event.
      }
      theThread.storeEvent(ev);
      return mEventConsumed         //because it should not applied to other states in the Run-to-complete cycle. 
           | mEventDonotRelinquish; //because it is stored here. Relinquishes after dequeuing!
    }
    else return 0;  //null argument.
  }
  
    
  @Override public CharSequence infoAppend(StringBuilder u){
    if(u == null){ u = new StringBuilder(200); }
    u.append(name).append(':');
    topState.infoAppend(u);  //fills the buffer with all aktive sub states.
    u.append("; ");
    if(theThread !=null){
      theThread.infoAppend(u);
    }
    return u;
  }
  
  
  
  /**Shows the name of the Statemachine and all active states.
   * @see java.lang.Object#toString()
   */
  @Override public String toString() {
    return infoAppend(null).toString();
  }

}
