package org.vishia.states;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.util.DataAccess;

/**
 * To build a state machine you should build a maybe inner class derived from {@link org.vishia.states.StateMachine}. This class builds the frame for all states.
 * Any state should be defined as inner class derived from either ...or  
 * <ul>
 * <li>{@link org.vishia.states.StateSimple}: A simple state with transitions, an optional entry- and an exit-action.
 * <li>{@link org.vishia.states.StateComposite}: It contains more as one sub states.
 * <li>{@link org.vishia.states.StateParallel}: It contains more as one {@link org.vishia.states.StateAddParallel} which are {@link org.vishia.states.StateComposite} 
 * for parallel behavior.
 * </ul>
 * See example pattern {@link org.vishia.states.example.StateExampleSimple}.
 * <br><br>
 * Any state can contain an entry- and an exit action which can be build either ... or with:
 * <ul>
 * <li>Overriding the method {@link org.vishia.states.StateSimple#entry(org.vishia.event.Event)} respectively {@link StateSimple#exit()}.
 * <li>Set the association {@link org.vishia.states.StateSimple#entry} respectively {@link org.vishia.states.StateSimple#exit} 
 * with an Implementation of {@link org.vishia.states.StateAction}
 *   respectively {@link java.lang.Runnable} which can be located in any other class, usual as inner anonymous class. 
 *   In that cases the entry and exit can use data from any other class.
 * </ul>  
 * <br><br>
 * Any state can contain some transitions which can be build either ... or with:
 * <ul>
 * <li>An inner class derived from {@link org.vishia.states.StateSimple.StateTrans}.
 * <li>An instance of an anonymous inner derived class of {@link org.vishia.states.StateSimple.StateTrans}.
 * <li>An method with return value {@link org.vishia.states.StateSimple.StateTrans} 
 *   and 2 arguments {@link org.vishia.event.Event} and {@link org.vishia.states.StateSimple.StateTrans}
 *   which contains the creation of a StateTrans object, the condition test, an action and the {@link org.vishia.states.StateSimple.StateTrans#doExit()}
 *   and {@link org.vishia.states.StateSimple.StateTrans#doEntry(org.vishia.event.Event)} invocation. This form shows only a simple method
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
 * The {@link org.vishia.states.StateMachine} can be quested whether a state is active yet: {@link org.vishia.states.StateMachine#isInState(Class)} 
 * or {@link org.vishia.states.StateMachine#isInState(StateSimple)}
 * <br><br>
 * The state machine can be animated using {@link org.vishia.states.StateMachine#processEvent(org.vishia.event.Event)} from an event queue or without events
 * by given a null argument, for example cyclically.
 * <br><br>
 * The state machine can be associated with an event queue and a timer using 
 * {@link org.vishia.states.StateMachine#setTimerAndThread(org.vishia.event.EventTimerMng, org.vishia.event.EventThread)}.
 * 
 * @author hartmut Schorrig
 *
 */
public class StateMachine
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-10-12 Hartmut chg: Not does not inherit from {@link StateComposite}, instance of {@link #topState}. 
   * <li>2014-09-28 Hartmut chg: Copied from org.vishia.stateMachine.TopState, changed concept
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
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
  
  //EventTimerMng.TimeEvent evTime;
  
  StateComposite topState; 
  
  /**Map of all states defined as inner classes of the derived class, filled with reflection. */
  HashMap<Integer, StateSimple> stateMap = new HashMap<Integer, StateSimple>();
  

  /**Applies an event to this state machine. This method is invoked from the {@link EventThread} if given for the events
   * which are stored directly to the {@link EventThread#storeEvent(Event)} with this class as destination 
   * or which are given with {@link #storeEvent(Event)}. It should not invoke by the user. 
   * @see org.vishia.event.EventConsumer#processEvent(org.vishia.event.Event)
   */
  EventConsumer processEvent = new EventConsumer() {
    @Override public int processEvent(final Event<?,?> evP){ return topState._processEvent(evP); }
  };
  
  
  
  public StateMachine(){ this(null, null);}
  
  /**The constructor of the whole stateMachine does the same as the {@link StateComposite#StateComposite()}: 
   * It checks the class for inner classes which are the states.
   * Each inner class which is instance of {@link StateSimple} is instantiated and stored both in the {@link #stateMap} to find all states by its class.hashCode
   * and in the {@link #topState} {@link StateComposite#aSubstates} for debugging only.
   * <br><br>
   * After them {@link #buildStatePathSubstates()} is invoked to store the state path in all states.
   * Then {@link StateComposite#createTransitionListSubstate(int)} is invoked for the {@link #topState} 
   * which checks the transition of all states recursively. Therewith all necessary data for the state machines's processing
   * are created on construction. 
   * @see StateComposite#StateComposite()
   * @see StateComposite#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateComposite#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   */
  public StateMachine(EventThread thread, EventTimerMng timer)
  {
    this.theThread = thread;
    this.theTimer = timer;
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
            state.stateMachine = this;
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

  
  /**Sets the timer manager and a optional thread for the event queue after construction.
   * This method should be called only one time for any object. Elsewhere a IllegalStateException is thrown. 
   * @param timer Source for timer events. It is necessary if the state machine uses timer events.
   * @param queue Queue for events. Left null if the state machine should be invoked in the same thread where the events are created.
   */
  public void XXXsetTimerAndThread(EventTimerMng timer, EventThread queue){
    if(theTimer !=null) throw new IllegalStateException("setTimeMng() of a StateTop should be invoked only one time");
    theTimer = timer;
    theThread = queue;
    //evTime = new EventTimerMng.TimeEvent(processEvent, theThread, 0);
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


  /**Applies and event to this statemachine. The event is stored in the queue of the {@link EventThread} if it is given 
   * or it is processed in this thread calling {@link #processEvent(Event)}.
   * @param ev
   */
  public void applyEvent(final Event<?,?> ev) {
    if(theThread !=null) {
      ev.setDst(processEvent);
      theThread.storeEvent(ev);
    } else {
      topState._processEvent(ev);
    }
  }
  
  
  

}
