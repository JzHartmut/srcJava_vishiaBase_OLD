package org.vishia.states;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import org.vishia.event.EventMsg2;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventTimerMng;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;



/**Base class of a State in a State machine. It is the super class of {@link StateComposite} too.
 * The user should build states in a {@link StateMachine} by writing derived classes of this class or of {@link StateComposite}
 * which contains transitions and may override the {@link #entry(EventMsg2)} and {@link #exit()} method: <pre>
 * class MyStateMachine extends StateMachine
 * {
 *   class StateA extends StateSimple  //one  state
 *   {
 *     (add)Override protected int entry(Event<?,?> ev) { ...code of entry... } 
 *     (add)Override protected void exit() { ...code of exit... }
 *     
 *     //Form of a transition as method which is detected by reflection:
 *     Trans checkX_DstState(Event<?,?> ev, Trans trans){
          if(trans == null){
            return new Trans(DstState.class);   //called on startup, programmed the destination state(s)
          }  
          else if(...condition...) {            //Check any condition
            trans.retTrans =  mEventConsumed;  //if an event is used as condition
            trans.doExit();
            ...transition action code...
            trans.doEntry(ev);
            return trans;
          }
          else return null;
        }
      
        //Form of a transition as instance of an derived anonymous class:
        Trans checkY_StateY = new Trans(StateY.class) {
        
          (add)Override protected int check(Event<?, ?> ev) {  //overrides the trans method:
            if(...condition...) { 
              retTrans = mEventConsumed;
              doExit();
              ...transition action code...
              doEntry(ev);
              return retTrans;
            }
            else return 0;
          }
        } 
         
 *   }
 * }
 * </pre>
 * For building a {@link StateComposite} see there.
 * <ul>
 * <li>A state can contains either transition methods with the shown arguments and {@link Trans} as return instance, 
 * <li>or a transition instances as derived anonymous types of Trans 
 * <li>or a class derived from {@link Trans] without an instance. It is not shown in this example, see {@link Trans}.
 * </ul>
 * The decision between this three forms maybe a topic of view in a browser tree of the program structure (Outline view on Eclipse).
 * <br><br> 
 * <b>How does it work</b>:<br> 
 * On startup all of this kinds of transitions are detected by reflection and stored in the private array {@link #aTransitions}.
 * On runtime the transitions are tested in the order of its priority. If any transition returns not 0 or null they do fire.
 * Either the {@link Trans#doExit()}, {@link Trans#doAction(EventMsg2, int)} and {@link Trans#doEntry(EventMsg2, int)} is programmed
 * in the if-branch of the condition before return, or that methods are executed from the state execution process automatically.
 * Manual programmed - it can be tested (step-debugger) in the users programm. Not manually, therefore automatically executed - 
 * one should set a breakpoint in the user-overridden {@link #exit()}, {@link #entry(EventMsg2)} or {@link Trans#action(EventMsg2)} methods
 * or the debugging step should be done over the package private method {@link #checkTransitions(EventMsg2)} of this class. 
 * Note that there are private boolean variables {@link Trans#doneExit}, {@link Trans#doneAction} and {@link Trans#doneEntry} 
 * to detect whether that methods are executed already.
 * <br><br>
 * <br>
 * @author Hartmut Schorrig
 *
 */
public abstract class StateSimple
{
  
/**Version, history and license.
 * <ul>
 * <li>2014-11-09 Hartmut new: {@link #setAuxInfo(Object)}, {@link #auxInfo()} used for state machine generation for C/C++
 * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateSimpleBase}, changed concept: 
 *   Nested writing of states, less code, using reflection for missing instances and data. 
 * <li>2013-05-11 Hartmut chg: Override {@link #exitAction()} instead {@link #exitTheState()}!
 * <li>2013-04-27 Hartmut chg: The {@link #entry(EventMsg2)} and the {@link #entryAction(EventMsg2)} should get the event
 *   from the transition. It needs adaption in users code. The general advantage is: The entry action can use data
 *   from the event. A user algorithm does not need to process the events data only in the transition. A user code
 *   can be executed both in a special transition to a state and as entry action. Both possibilities do not distinguish
 *   in formal possibilities. The second advantage is: If the event is used, it should given to the next entry. If it is
 *   not used, a 'null' should be given. The user need not pay attention in the correct usage of {@link #mEventConsumed} or not.
 * <li>2013-04-27 Hartmut new: {@link #mStateEntered} is returned on any {@link #entry(EventMsg2)}. If the state is not changed,
 *   a 'return 0;' should be written in the transition code. With the new bit especially the debugging can distinguish
 *   a state changed from a non-switching transition.    
 * <li>2013-04-13 Hartmut re-engineering: 
 *   <ul>
 *   <li>The property whether or not there are non-event transitions is set on ctor. It is a property
 *     established in source code, therefore it should be known in runtime after construction already.
 *     The entry method does not need to modified because non-event transitions. It may be better because the
 *     entry method's code should not depend on the trans method's content.  
 *   <li>entry is final, it can be final now. For overwriting the {@link #entryAction()} is given. It is more simple to use.  
 *   <li>The trans method is protected: It should not be called from outside in any case.
 *   </ul>
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
public static final int version = 20130511;

/**Specification of the consumed Bit in return value of a Statemachine's {@link #checkTransitions(EventMsg2)} or {@link #entry(int)} 
 * method for designation, that the given Event object was not used to switch. The value of this is 0.
 */
public final static int notTransit =0x0;


/**Bit in return value of a Statemachine's {@link #checkTransitions(EventMsg2)} or entry method for designation, 
 * that the given Event object was used to switch. It is 0x01.
 */
public final static int mEventConsumed = EventConsumer.mEventConsumed; //== 0x1


/**Bit in return value of a Statemachine's trans method for designation, 
 * that the given Transition is true. The bit should be set together with {@link #mEventConsumed} if the event forces true.
 * If this bit is set without {@link #mEventConsumed} though an event is given the transition has not tested the event.
 * In that case the event may be used in the following run-to-complete transitions.
 */
public final static int mTransit = 0x2;


/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has entered yet. If this bit is not set, an {@link #entry(EventMsg2)} action is not called.
 * It means a state switch has not occurred. Used for debug.
 */
public final static int mStateEntered = 0x4;


/**If a composite state is leaved, any other parallel composite states don't processed then. */
public final static int mStateLeaved = 0x8;


/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has non-event-driven transitions, therefore the trans method should be called
 * in the same cycle.
 */
public final static int mRunToComplete =0x10;



public final static int mFlatHistory = 0x40;

public final static int mDeepHistory = 0x80;

/**Aggregation to the whole state machine. Note: it cannot be final because it will be set on preparing only. */
protected StateMachine stateMachine;


/**Reference to the enclosing state. With this, the structure of state nesting can be observed in data structures.
 * The State knows its enclosing state to set and check the state identification there. 
 * Note: it cannot be final because it will be set on preparing only. 
 */
protected StateComposite enclState;


/**Any additional information. Used for special cases. */
private Object auxInfo;

/**Path to this state from the top state. The element [0] is the top state.
 * The last element is the enclosing state.
 */
protected StateSimple[] statePath;

/**The own identification of the state. Note: it cannot be final because it will be set on preparing only. */
protected String stateId;

/**If set, on state entry the timer for timeout is set. */
int timeout;


/**The timeout event set if necessary. This is a static instance, reused. It is the same instance in all non-parallel states. */
//EventTimerMng.TimeEvent evTimeout;

/**Set so long a timeout is active. */
EventTimerMng.TimeOrder timeOrder;

/**It is either 0 or {@link #mRunToComplete}. Or to the return value of entry. */
protected final int modeTrans;

/**Reference to the data of the class where the statemachine is member off. */
//protected Environment env;

/**Debug helper. This counter counts any time on entry this state. Because it is public, the user can read it 
 * and reset it to 0 for some debug inventions. This variable should never be used for algorithm. */
public int ctEntry;


/**Debug helper. This timstamp is set for System.currentTimeMilliSec() any time on entry this state. 
 * Because it is public, the user can read it for some debug inventions. This variable should never be used for algorithm. */
public long dateLastEntry;

/**Debug helper. This time difference is set any time on exit this state. It is the time in milliseconds stayed in this state.
 * Because it is public, the user can read it for some debug inventions. This variable should never be used for algorithm. */
public long durationLast;


/**Action for entry and exit. It can be set by the code block constructor in a derived class. Left empty if unused. */
protected StateAction entry;

protected Runnable exit;

private Method entryMethod, exitMethod;

/**Set to true if the {@link #selectTrans(EventMsg2)} is not overridden. Then check the {@link #aTransitions} -list */
private boolean bCheckTransitionArray;

private Trans[] aTransitions;


/**An instance of this class represents a transition of the enclosing SimpleState to one or more destination states.
 * There are three forms to program transitions in a state class:
 * <br><br>
 * <b>Simple transition method in a state class:</b><br>
 * The user can write a method with the following form: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     Trans trans1_State2(Event<?,?> ev, Trans trans) {
 *       if(trans == null) return new Trans(MyState_B.class);
 *       else if(a_condition && ev instanceof MyEvent && (ev.getCmd() == MyCmd) {
 *         trans.retTrans =  mEventConsumed
 *         doExit();
 *         myAction();
 *         doEntry();
 *         return trans;
 *       }
 *       else return null;
 *     }  
 * </pre>
 * <ul>
 * <li><code>if(trans == null)</code>: The transition method have to be create a <code>new Trans(dststate(s))</code> if <code>trans==null</code> is given as argument. 
 * That is only in the startup phase. 
 * <li><code>else if(</code>: The method should check a condition with or without checking the given {@link EventMsg2} instance (maybe null). 
 * If it is true then the trans should be returned. The {@link #doExit()} etc. can be called or not, see there. 
 * <li><code>else</code>: If the transition should not be fired because the condition is false null should be returned.
 * </ul>
 * <br><br>
 * How does it works:
 * <ul>
 * <li>The method will be detected by checking the State class content via reflection.
 * <li>The Trans instance will be build by invoking this method with the argument null for ev and trans.
 *   Therefore a new Trans(dstStates, ...) should be programmed for that case. This is only done in the startup phase.
 * <li>The transition instance is stored in the private {@link StateSimple#aTransitions} array.
 * <li>The given method detected as <code>java.lang.reflect.Method</code> is used via an instance of {@link StateTransitionMethod}.
 *   which is used as {@link #check} in the transition instance.
 * <li>While checking transitions calling the non-overridden {@link #check(EventMsg2)} this method will be called with this Trans-instance.
 *   The {@link #check(EventMsg2)} uses the {@link #check} action, which is the instance of {@link StateTransitionMethod}.
 * <li>If the method returns null, the transition is not fired, elsewhere it is fired.
 * <li>The {@link #doExit()} and {@link #doEntry(EventMsg2)} to execute the stateSwitch will be called
 *   only if they are not be called inside the {@link #check(EventMsg2)} method. There are markers {@link #doneExit} etc.  
 * <li>The {@link #doAction(EventMsg2, int)} is invoked but it is empty because the {@link #action} == null and the {@link #action(EventMsg2)}
 *   is not overridden.   
 * </ul> 
 * <br><br>
 * <b>Transition class:</b><br>
 * The user can write a class derived from this class for each transition. 
 * The parameterless constructor of this should invoke the super constructor with the destination state(s) class which should be initialized with the destination state classes.
 * The user should override the method {@link #check(EventMsg2)} which should contain the condition and actions: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     class MyTrans extends Trans {
 *       MyTrans(){ super((MyState_B.class)); }
 *       
 *       QOverride int check(Event<?,?> ev) {
 *         if(condition && ev instanceof MyEvent && (ev.getCmd == MyCmd){
 *           retTrans = StateSimple.mEventConsumed;
 *           doExit();
 *           myAction();
 *           doEntry();
 *         }
 *       }  
 *     }; 
 * </pre>
 * In this class some other things can be programmed for example set {@link #check} and/or {@link #action} with references to any other instances
 * or override the {@link #action(EventMsg2)}, see examples there.
 * <br><br>
 * <b>Transition class or transition instance:</b><br>
 * The user can build either a class derived from {@link Trans} or as instance with a derived anonymous classes of {@link Trans}
 * for each transition. which should be initialized with the destination state classes.
 * The user should override the method {@link #check(EventMsg2)} which should contain the condition and actions: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     Trans trans1_State2 = new Trans(MyState_B.class) {
 *       QOverride int check(Event<?,?> ev) {
 *         if(condition && ev instanceof MyEvent && (ev.getCmd == MyCmd){
 *           retTrans = StateSimple.mEventConsumed;
 *           doExit();
 *           myAction();
 *           doEntry();
 *         }
 *       }  
 *     }; 
 * </pre>
 * The method {@link #buildTransitionPath()} is invoked only on startup of the statemachine. 
 * It creates the {@link #exitStates} and {@link #entryStates} -list.
 * The inner class BuildTransitionPath is used only inside that method {@link #buildTransitionPath()}.
 * <br><br>
 * How does it works:
 * <ul>
 * <li>The derived Trans class or the element of type Trans will be detected by checking the State class content via reflection.
 * <li>If a class is found the instance is build with the reflection method {@link java.lang.Class#newInstance()}.
 * <li>If a Trans instance is used directly. 
 * <li>The transition instance is stored in the private {@link StateSimple#aTransitions} array.
 * <li>While checking transitions the maybe overridden {@link #check(EventMsg2)} method will be called with this Trans-instance.
 * <li>If the check method returns 0, the transition is not fired, elsewhere it is fired.
 * </ul>
 * <br><br>
 * Further possibilities:
 * <ul>
 * <li>The {@link #doExit()} and {@link #doEntry(EventMsg2)} need not be called in a {@link #check(EventMsg2)} method. It is executed after it too,
 *   controlled by the variables {@link #doneExit} and {@link #doneEntry}. If any statements are programmed as action, they are executed
 *   after {@link #doExit()}. That is not exactly UML-conform, but in most cases not relevant.
 * <li>Don't override the {@link #check(EventMsg2)}, instead set the {@link #check} aggregation, see there.
 * <li>{@link #action(EventMsg2)} can be overridden, then the {@link #doEntry(EventMsg2)} and any actions should not be invoked in the check method.
 * </ul> 
 */
public class Trans
{
  
  /**Return value of the last used transition test. For debugging and for the {@link TransitionMethod}. */
  public int retTrans;
  
  /**All destination classes from constructor. They are not necessary furthermore after {@link #buildTransitionPath()}
   * because they are processed in the {@link #exitStates} and {@link #entryStates} lists.
   */
  //final Class<?>[] dst;
  final int[] dst;
  
  
  final int priority;
  
  /**If a condition instance is given, its {@link StateAction#exec(EventMsg2)}-method is called to test whether the transition should fire.
   * The condition can contain action statements. But that is not allowed, if some {@link #choice} conditions are set.
   * if the condition contains only a condition, it should return 0 if it is false and not {@link StateSimple#mEventConsumed}, if it is true.
   * The event can be used for condition or not. If events are not used <code>null</code> can be supplied instead the event. 
   * <br><br>
   * The condition field is used if a transition method is given and automatically detected via reflection in a state class.
   * Thereby an instance of StateAction is created on startup.
   * <br><br>
   * The condition can be given by any StateAction instance in any other class. It can be set by construction.
   */
  protected StateAction check;
  
  /**If an action is given, the condition should not contain an action itself. The action is executed after the exitState()-operation
   * and before the entryState()-operation. 
   * <br><br>
   * The action can be given by any StateAction instance in any other class. It can be set by construction.
   */
  protected StateAction action;
  
  /**More as one choice can be given if this transition is a choice-transition. 
   * The transition is only fired if at least one choice-condition is true. The {@link #action} of the state and the {@link #action} 
   * of the choice is executed one after another after exit() and before entry() only if the transition is fired.
   * If some choice are given, the choice should contain the destination state. The constructor of this transition have to be empty.
   */
  protected Trans[] choice;
 
  
  /**It is null for a simple transition in a state. It contains the parent transition for a choice, which is an inner transition. */
  Trans parent;
  
  /**All states which's {@link StateSimple#exitTheState()} have to be processed if the transition is fired. */
  StateSimple[] exitStates;
  
  /**All states which's {@link StateSimple#entry(EventMsg2)} have to be processed if the transition is fired. */
  StateSimple[] entryStates;
  StateSimple[][] entryStatesOld;
  
  String transId;
  
  /**Set it to false on start of check this transition. The methods {@link #doEntry(EventMsg2)}, {@link #doAction(Trans, EventMsg2, int)} and {@link #doExit()} sets it to true. 
   * If a {@link #check(EventMsg2)} method has not invoked this methods, they are invoked from the {@link StateSimple#checkTransitions(EventMsg2)}. */
  boolean doneExit, doneAction, doneEntry;
  
  /**This constructor is used to initialize a derived anonymous class with one or more destination state of this transition
   * or as super constructor for derived classes.
   * This constructor should not be used if a transition class contains {@link #choice}-transitions.
   */
  public Trans(Class<?> ...dst){ this(1000, dst); }

    
  /**This constructor is used to initialize a derived anonymous class with one or more destination state of this transition
   * or as super constructor for derived classes if a priority should be given. All transtions without priority are lower.
   * This constructor should not be used if a transition class contains {@link #choice}-transitions.
   * @param priority Number starting from 1 (highest) for priority to max. 999. A more prior transition fires if more as one transition have the same condtions.
   *   The transitions are tested in order of its priority. If a transition fires all transitions with lesser priority are not tested.
   */
  public Trans(int priority, Class<?> ...dst){
    this.priority = priority;
    if(dst.length ==0) this.dst = null;
    else {
      this.dst = new int[dst.length];
      for(int ix = 0; ix < dst.length; ++ix){
        this.dst[ix] = dst[ix].hashCode();     //store the hashcode to find it.
      }
    }
  }
  
  
  /**This constructor should be used if the destination states are given from an outer algorithm.
   * @param dstKeys
   */
  public Trans(int priority, int[] dstKeys){
    this.priority = priority;
    this.dst = dstKeys;
  }
  

  
  /**Builds the transition path from given state to all dst states. Called on startup.
   * 
   */
  void buildTransitionPath(){
    if(choice !=null) {
      for(Trans choice1: choice) {
        BuildTransitionPath d = choice1.new BuildTransitionPath(this.exitStates);
        d.execute();
      }
    } else {
      BuildTransitionPath d = new BuildTransitionPath(this.exitStates);
      d.execute();
    }
  }
  

  
  /**The non-overridden condition checks the {@link #check} or returns 0 if it is not given.
   * This method can be overridden by a users method to check a transition. 
   * The method have to be set {@link #retTrans} if the transition should be fired.
   * If the event is used then the bit @{@link StateSimple#mEventConsumed} has to be set in {@link #retTrans}
   * to indicate that the event is consumed with this transition. The event is not applied to other transitions in enclosing states then.
   * But it is applied in parallel states, like defined in UML.
   * 
   * @param ev event Any event 
   */
  protected void check(EventObject ev) {
    if(check !=null){
      int ret = check.exec(ev);  //may set mEventConsumed or mTransit
      retTrans |= ret;  //may set mEventConsumed or mmTransit
    } 
  }
  
  
  
  /**The non-overridden action executes the {@link #action} or returns 0 if it is not given.
   * @param ev event
   * @return especially {@link StateSimple#mRunToComplete} for state control, elsewhere 0. 
   */
  protected void action(EventObject ev) {
    if(action !=null){
      action.exec(ev);  //may set mEventConsumed or
    }
  }
  
  
  
  
  /**Checks the trigger and conditions of a state transition. The user should override this method in form (example)
   * <pre>
  public int trans(Event ev){
    TypeOfStateComposite enclState;
    if(ev instanceof MyEvent and ((MyEvent)ev).getCmd() == myExpectedCmd){
      retTrans = mEventConsumed | mTransit;
      doExit();
      if(this.action !=null) {
        this.action.action(ev); //executes given action
      }
      statementsOfTransition();
      retTrans |= doEntry();
      return retTrans;
    } 
    else if( otherCondition) {
      return exit().otherState.entry(consumed);
    }
    else return notConsumed;
  }
   * </pre>
   * @param ev Any event, maybe null if the user does not need it
   * @return It should return {@link #mEventConsumed} if the event is consumed
   *   or it should return {@link #eventNotConsumed} especially if no transition is fired.
   *   That return value is essential for processing events in composite and cascade states.
   *   If an event is consumed it is not used for another switch in the same state machine
   *   but it is used in parallel states. See {@link StateCompositeBase#processEvent(EventMsg2)} and {@link StateParallelBase#processEvent(EventMsg2)}.
   *   Returns 0 if a state switch is not processed. Elsewhere {@link #mStateEntered}. {@link #mStateLeaved}
   */
  final int doTrans(EventObject ev){
    this.retTrans = 0;  //set in check
    check(ev);
    if(this.retTrans ==0) return 0;  //don't fire.
    else {
      if(this.choice !=null) {
        //check all choice, one of them have to be true
        for(Trans choice1: choice) {  //check all choices
          int retChoice = choice1.doTrans(ev);                //the choice determines the return. If the conditions returns mEventConsumed it is not consumed if no choice.     
          if(retChoice !=0) return this.retTrans | retChoice;   //if true than return, don't check the other!
        }
        //only if no choice, elsewhere return in loop.
        return 0; 
      } else {
        //no choice, execute exit, action, entry if it is not done already.
        if(choice == null) { //a choice condition has no exit and entry, the action is executed later.
          if(!doneExit) { doExit(); }
          if(!doneAction) { doAction(ev,0); }
          if(!doneEntry){ doEntry(ev); }
        }
        return retTrans;
      }
    }
  }

  
  
  /**Processes the action of the transition. If it is a choice transition: firstly process the action of the condition before.
   * It is calling recursively for more as one choice.
   * @param parent The transition before this choice or null on State-transition
   * @param ev The given event.
   * @param recurs recursion count. throws IllegalArgumentException if > 20 
   */
  public final void doAction(EventObject ev, int recurs) {
    if(recurs > 20) throw new IllegalArgumentException("too many recursions");
    if(parent !=null) {
      parent.doAction(ev, recurs+1);  //recursion to the first parent
    }
    action(ev);  //maybe overridden, default checks whether a StateAction action is given.
    doneAction = true;
  }
  
  
  
  /**Executes the exit from this and all enclosing States to fire the transition.
   * 
   */
  public final void doExit()
  {
    retTrans |= mStateLeaved;
    for(StateSimple state: exitStates){
      state.exitTheState();
    }
    doneExit = true;
  }
  

  
  /**Entry in all states for this transition.
   * The states will be entered in the order from outer to inner state
   * and for all parallel states then from outer to inner.
   * @param ev
   */
  public final void doEntry(EventObject ev)
  { doEntry(ev, 0);
  }
  
  /**Entry in all states for this transition maybe with history entry.
   * The states will be entered in the order from outer to inner state
   * and for all parallel states then from outer to inner.
   * @param ev The data of the event can be used by entry actions.
   * @param history If one of the bits {@link StateSimple#mFlatHistory} or {@link StateSimple#mDeepHistory} are set
   *   then the last state is entered again. 
   */
  public final void doEntry(EventObject ev, int history)
  { 
    for(int ix = 0; ix < entryStates.length; ++ix) { //stateParallel) {
      StateSimple state = entryStates[ix];
      int hist1 = (ix == entryStates.length -1) ? history: 0;
      retTrans |= state.entryTheState(ev, hist1);
    }
    doneEntry = true;
  }
  
  @Override public String toString(){ return transId == null ? "-unknown transId" : transId; }



  /**An instance of this class is created only temporary to build the transition path, set the #exitStates
   * and #entryStates.
   */
  private class BuildTransitionPath {
    /**The destination states filled with {@link #buildDstStates()} with the length of {@link #dst}.
     * Left null if no dst states are given, especially on existing {@link Trans#choice}.
     */
    final StateSimple[] dstStates = Trans.this.dst == null ? null : new StateSimple[Trans.this.dst.length];
    
    final StateSimple[] exitStates;
    
    /**List of 'same path'.
     * If the samePaths[ix] == ixDst then it is the tested path.
     * if samePaths[ix] == ixDst then this path in dstStates[ixDst] is the same like in dstStates[ix].
     * 
     */
    int[] samePaths = this.dstStates == null ? null : new int[this.dstStates.length];  //all filled with 0
  
    /**The current indices in {@link #dstStates}.{@link StateSimple#statePath} while evaluating the entry path. */
    int[] ixDstPath = this.dstStates == null ? null : new int[this.dstStates.length];
    
    /**Current index to write {@link Trans#entryStatesOld}. Pre-increment. */
    int ixEntryStatesOld = -1;
    
    
    public BuildTransitionPath(StateSimple[] exitStates)
    {
      this.exitStates = exitStates;
    }
    
    void execute(){
      if(stateId.equals("Ready"))
        Debugutil.stop();
  
      buildDstStates();
      searchStateCommon(); //after them ixDstPath was set.
      buildExitPath();
      buildEntryStates();      
      
    }
    
    private void buildDstStates() {
      //search all dst state instances from the given class. In constructor only the class is known.
      if(dst !=null) for(int ixdst = 0; ixdst < dst.length; ++ixdst){
        dstStates[ixdst] = enclState.stateMachine.stateMap.get(new Integer(dst[ixdst]/*.hashCode()*/));
      }
    }
    
    /**Searches the common state between all {@link #dstStates} and the source state.
     * All states till the common state should be exited. All states from the common state should be entered.
     * As result the {@link #ixDstPath} is set with the index after the found stateCommon 
     */
    private void searchStateCommon() {
      //search the common state for all transitions. 
      StateSimple stateCommon;  //the common state between this and all dstState.
      int zStatePath =1;
      int ixSrcPath = statePath.length -2;  //Note: the commonState is never this itself, at least one exit.
      do {
        stateCommon = StateSimple.this.statePath[ixSrcPath];  
        int ixdst = -1;
        //check all dst states whether the common state is in that state path. If it is not in the statePath, set stateCommon = null to abort the search.
        //If the stateCommon is in the statePath of all dstStates then ixDstPath[] is set with the index of the stateCommon in the dstPath.
        //It should the same index for all states because the statePath starts from the stateTop for all.
        while(stateCommon !=null && ++ixdst < dstStates.length) {  //commonSearch: abort this while if at least one 
          //check all dstStates down to either the exitState or down to to top state.
          //If the exit state is equal any entry enclosing state or both are null (top state), then it is the common state.
          StateSimple dstState = dstStates[ixdst];
          ixDstPath[ixdst] = dstState.statePath.length-1;  //Note: start with dstState itself because (dst    (src)---->) transition from this to its enclosing state
          while(stateCommon !=null && dstState.statePath[ixDstPath[ixdst]] != stateCommon){
            if(--ixDstPath[ixdst] < 0) { //end is reached without found stateCommon
              stateCommon = null;  //abort test.
            }
          }
        }
      } while(stateCommon == null && --ixSrcPath >=0); //continue if the stateCommon is not member of all dst state paths.    
      if(stateCommon == null){
        throw new IllegalArgumentException("no common state found");
      }
      Trans.this.exitStates = new StateSimple[StateSimple.this.statePath.length - (ixSrcPath +1)];  //don't exit the stateCommon
      for(int ixdst = 0; ixdst < dst.length; ++ixdst){
        ixDstPath[ixdst] +=1;  //first entry in state after stateCommon.  
      }
    }
    
    
    
    private void buildExitPath() {
      int ixSrcPath = StateSimple.this.statePath.length;
      for(int ixExitStates = 0; ixExitStates < Trans.this.exitStates.length; ++ixExitStates){
        Trans.this.exitStates[ixExitStates] = StateSimple.this.statePath[--ixSrcPath];  //copy the statePath from this backward to exitStates-List.
      }
      
    }
    
    
    
    private void buildEntryStates() 
    {
      int[] entries1 = new int[dstStates.length];
      List<StateSimple> listEntries = new LinkedList<StateSimple>();
      int ixEntry = ixDstPath[0];  //start with the states after the common state.
      int dstNotReached;  // if 0 then all reached.
      do {
        dstNotReached = ixDstPath.length;  // if 0 then all reached.
        int ixDst, ixDst2;
        for(ixDst = 0; ixDst < ixDstPath.length; ++ixDst) { entries1[ixDst] = 0; } //clean
        //
        for(ixDst = 0; ixDst < ixDstPath.length; ++ixDst) {
          StateSimple[] statePath = dstStates[ixDst].statePath;
          if(ixEntry < statePath.length ) { // if >=, it is reached
            StateSimple entryState = statePath[ixEntry];
            //search whether this state is processed already:
            int hashEntryState = entryState.hashCode();
            if(ixDst == 0) { 
              ixDst2 = entries1.length;       //first is the first: add the state.
            } else {
              ixDst2 = 0;
              while(ixDst2 < entries1.length && entries1[ixDst2] != hashEntryState) {
                ixDst2 +=1;  //search in short array.
              }
            }
            if(ixDst2 == entries1.length) { //not found or first
              entries1[ixDst] = hashEntryState;   //set it to the place for this dstState.
              listEntries.add(entryState);
            }
          }
          else {
            dstNotReached -=1;  //dst state was reached.
          }
        }
        ixEntry +=1;
      } while( dstNotReached >0);
      Trans.this.entryStates = listEntries.toArray(new StateSimple[listEntries.size()]);
    }
    
    
    
    
    
    
    private void buildEntryPathOld() {
      
      //this array contains which dst paths a the same. 
      
      //for()
      int lengthEntryStates = 0;
      for(int ixDst = 0; ixDst < dstStates.length; ++ixDst){
        int l1 = dstStates[ixDst].statePath.length - ixDstPath[ixDst];
        if(l1 > lengthEntryStates){
          lengthEntryStates = l1;
        }
      }
      Trans.this.entryStatesOld = new StateSimple[lengthEntryStates][];
      //
      //          |---->(----->dst1 )
      //--->()--->|            
      //          |---->(----->|--->dst0  )
      //                       |--->dst2  )
      //
      boolean bAllDstReached;
      do {
        //walk through all dstStates[..].statePath
        StateSimple dst1 = dstStates[0].statePath[ixDstPath[0]];
        int nrofBranchs = ixEntryStatesOld < 0 ? 1 : Trans.this.entryStatesOld[ixEntryStatesOld].length;
        for(int ixDst = 0; ixDst < dstStates.length; ++ixDst){
          //check the same path
          StateSimple dst = dstStates[ixDst].statePath[ixDstPath[ixDst]];
          //check whether it is the same in all other paths:
          for(int ixDst2 = 0; ixDst2 < dstStates.length; ++ixDst2) {
            //compare the same path
            if(ixDst !=ixDst2 && samePaths[ixDst2] == ixDst) { //yet the same path:
              //this is the same path yet, test whether it remains the same:
              StateSimple dst2 = dstStates[ixDst2].statePath[ixDstPath[ixDst2]];
              if(dst2 != dst){
                //now no more the same:
                samePaths[ixDst2] = ixDst2;  //own one.
                nrofBranchs +=1;
              }
            }
          }
              
        }
        bAllDstReached = XXXwriteEntryPathItem(nrofBranchs);
      } while(!bAllDstReached);
      
    }
    
    
    void walkThroughEntries() {
      
    }
    
    
    private boolean XXXwriteEntryPathItem(int nrofBranchs) {
      boolean bAllDstReached = true;
      Trans.this.entryStatesOld[++ixEntryStatesOld] = new StateSimple[nrofBranchs];
      int ixEntry = -1;
      for(int ixDst = 0; ixDst < dstStates.length; ++ixDst) {
        //don't store the same entry twice, check:
        if(samePaths[ixDst] == ixDst) { //it is an own path
          Trans.this.entryStatesOld[ixEntryStatesOld][++ixEntry] = dstStates[ixDst].statePath[ixDstPath[ixDst]];
          if(ixDstPath[ixDst] < dstStates[ixDst].statePath.length -1){
            bAllDstReached = false;  //continue!
          }
        }
        ixDstPath[ixDst] +=1;  //increment all.
      }
      return bAllDstReached;
    }
    
  }
  

  
}



public class Choice extends Trans
{
  
  /**Constructs a transition with {@link #choice} sub-transitions.
   */
  public Choice(){
    super();
  }
  
  

}





/**If a state contains a field of this class, a timeout is given for that state.
 */
public class Timeout extends Trans
{
  int millisec;
  
  /**Creates the timeout.
   * @param millisec The milliseconds to the timeout as positive number > 0 
   * @param dstStates The destination state(s)
   */
  public Timeout(int millisec, Class<?> ...dstStates) {
    super(dstStates);
    transId = "timeout";
    this.millisec = millisec;
  }
}




protected StateSimple(){
  this.modeTrans = mRunToComplete;  //default: call trans(), it has not disadvantages
  //search method exit and entry and transition methods
  /*
  Method[] methods = this.getClass().getDeclaredMethods();
  
  for(Method method: methods) {
    String name = method.getName();
    Class<?>[] argTypes = method.getParameterTypes();
    Class<?> retType = method.getReturnType();
    if(name.equals("entry")){
      if(argTypes.length != 1 || argTypes[0] != Event.class){
        throw new IllegalArgumentException("entry method found, but with failed argument list. Should be \"entry(Event<?,?> ev)\" ");
      }
      entryMethod = method;
      entry = new EntryMethodAction();  //use internal action which calls entryMethod
    }
    else if(name.equals("exit")){
      if(argTypes.length != 0){
        throw new IllegalArgumentException("exit method found, but with argument list. Should be \"exit()\" ");
      }
      exitMethod = method;
      exit = new ExitMethodAction();    //use internal action which calls exitMethod
    }
    else {
      //all other methods should be transition methods.
      
    }
  }
  */
}


public void setAuxInfo(Object info) { auxInfo = info; }

public Object auxInfo() { return auxInfo; }



/**This method may be overridden for a entry action. In that case the {@link #entry} or {@link #entryMethod} is either not used
 * or it is used especially in the overridden method responsible to the user.
 * If this method is not overridden, a given {@link StateAction} stored on {@link #entry} is executed.
 * That may be the execution of the {@link #entryMethod}.
 * @param ev The data of the event can be used for the entry action.
 * @return {@link #mRunToComplete} if next states should be tested for conditions.
 */
protected int entry(EventObject ev) {
  if(entry !=null) { return entry.exec(ev); }
  else return 0;
}




/**This method may be overridden for a exit action. In that case the {@link #exit} or {@link #exitMethod} is either not used
 * or it is used especially in the overridden method responsible to the user.
 * If this method is not overridden, a given {@link Runnable} stored on {@link #exit} is executed.
 * That may be the execution of the {@link #exitMethod}.
 */
protected void exit() {
  if(exit !=null) { exit.run(); }
}








/**Sets the path to the state for this and all {@link #aSubstates}, recursively call.
 * @param enclState
 * @param recurs
 */
void buildStatePath(StateComposite enclState) {
  if(enclState == null) { 
    //special handling for TopState
    statePath = new StateSimple[1];
    statePath[0] = this;              //first element is the top state.
  } else {
    //copy the path from the top state to the new dst state. It is one element lengths.
    int topPathLength = enclState.statePath.length;
    this.statePath = new StateSimple[topPathLength +1];
    System.arraycopy(enclState.statePath, 0, this.statePath, 0 , topPathLength);
    statePath[topPathLength] = this;  //last element is this itself.
  }
}



/**Creates the list of all transitions for this state, invoked one time after constructing the statemachine.
 * 
 */
void createTransitionList(Object encl, Trans parent, int nRecurs){
  IndexMultiTable<String, Trans> transitions1 = new IndexMultiTable<String, Trans>(IndexMultiTable.providerString);
  //List<Trans>transitions = new LinkedList<Trans>();
  Class<?> clazz = encl.getClass();
  if(nRecurs > 2) {
    Debugutil.stop();
  }
  if(nRecurs > 10) throw new IllegalArgumentException("too many recursion, choice-transition; state=" + stateId);
  try{
    Class<?>[] clazzs = clazz.getDeclaredClasses();
    for(Class<?> clazz1: clazzs){
      if(DataAccess.isOrExtends(clazz1, Trans.class)){
        @SuppressWarnings("unused")  //only test
        Constructor<?>[] ctora = clazz1.getDeclaredConstructors();
        //Note: the constructor needs the enclosing class as one parameter because it is non-static.
        //The enclosing instance is the StateSimple always, also on choice-transitions.
        Constructor<?> ctor1 = clazz1.getDeclaredConstructor(StateSimple.this.getClass());
        ctor1.setAccessible(true);
        Trans trans = (Trans)ctor1.newInstance(this);
        trans.transId = clazz1.getSimpleName();
        //transitions.add(trans);
        String priority = String.format("%04d", trans.priority);
        transitions1.add(priority, trans);
        trans.parent = parent;  //null for a state transition
        checkBuiltTransition(trans, nRecurs);
      }
    }
    Field[] fields = clazz.getDeclaredFields();
    for(Field field: fields){
      field.setAccessible(true);
      Object oField = field.get(encl);
      if(oField !=null && DataAccess.isOrExtends(oField.getClass(), Trans.class)) {
        String sFieldName = field.getName();
        if(!DataAccess.isReferenceToEnclosing(field)) { //don't test the enclosing instance, it is named this$0 etc. 
          Trans trans = (Trans) oField;
          trans.transId = sFieldName;
          //transitions.add(trans);
          String priority = String.format("%04d", trans.priority);
          transitions1.add(priority, trans);
          trans.parent = parent;  //null for a state transition
          checkBuiltTransition(trans, nRecurs);
        }
      }
    }
    Method[] methods = clazz.getDeclaredMethods();
    for(Method method: methods) {
      String name = method.getName();
      Class<?>[] argTypes = method.getParameterTypes();
      Class<?> retType = method.getReturnType();
      if(argTypes.length==2 && argTypes[0] == EventObject.class && retType == Trans.class && argTypes[1] == Trans.class) {
        //a transition method
        Trans trans = null;
        try{ 
          method.setAccessible(true);
          Object oTrans = method.invoke(encl,  null, null); 
          trans = (Trans)oTrans;
          trans.transId = method.getName();
          trans.check = new StateTransitionMethod(trans, method);
          trans.buildTransitionPath();
          trans.parent = parent;  //null for a state transition
          //transitions.add(trans);
          String priority = String.format("%04d", trans.priority);
          transitions1.add(priority, trans);
        } catch(Exception exc){ throw new IllegalArgumentException("stateTrans-method failure"); }
      }
    }
  } catch(Exception exc){
    exc.printStackTrace();
  }  
  if(transitions1.size() >0) {
    int nrofTransitions = transitions1.size();
    Trans[] aTrans = new Trans[nrofTransitions];
    int ixTrans = -1;
    for(Map.Entry<String, Trans> entry: transitions1.entrySet()){
      aTrans[++ixTrans] = entry.getValue();
    }
    if(parent == null) {
      this.aTransitions = aTrans; // transitions.toArray(new Trans[transitions.size()]);
    } else {
      parent.choice = aTrans; //transitions.toArray(new Trans[transitions.size()]);
    }
  }
}



protected void prepareTransitions() {
  if(aTransitions !=null) {
    for(Trans trans: aTransitions) {
      checkBuiltTransition(trans, 0);
    }
  }
}




/**Creates the empty yet array of transitions. 
 * Invoked if transitions should be defined from any Java program outside, not from the Source of this class. Used for StateMgen.
 * @param nrofTransitions Number should be the number of transitions to add, see {@link #addTransition(Trans)}
 */
public void createTransitions(int nrofTransitions) {
  aTransitions = new Trans[nrofTransitions];
}


/**Adds a transition.
 * Invoked if transitions should be defined from any Java program outside, not from the Source of this class. Used for StateMgen.
 * @param trans
 */
public void addTransition(StateSimple.Trans trans) {
  int ix = 0;
  while(ix < aTransitions.length && aTransitions[ix] !=null){ ix +=1; } //search next free
  if(ix >= aTransitions.length) throw new IllegalArgumentException("too many states to add");
  aTransitions[ix] = trans;

}





private void checkBuiltTransition(Trans trans, int nRecurs) {
  if(trans.dst !=null) {
    trans.buildTransitionPath();
  }
  if(trans instanceof Timeout) {
    Timeout timeoutTrans = (Timeout)trans;
    trans.check = new ConditionTimeout();
    this.timeout = timeoutTrans.millisec;
    searchOrCreateTimerEvent();
  }
  createTransitionList(trans, trans, nRecurs+1);  //for choices
}




/**Gets the timeout event from the top state or the {@link StateParallel} 
 * or creates that event.
 * All non-parallel states need only one timeout event instance because only one of them is used.
 */
private void searchOrCreateTimerEvent() {
  if(stateMachine.theTimer == null || stateMachine.theThread == null) 
    throw new IllegalArgumentException("This statemachine needs a thread and a timer manager because timeouts are used. Use StateMachine(thread, timer); to construct it");
  StateSimple parent = this;
  while(parent.enclState !=null && !(parent instanceof StateParallel)) {
    parent = parent.enclState;
  }
  //parent is either the top state or a StateAddParallel
  if(parent.timeOrder == null) {
    parent.timeOrder = stateMachine.theTimer.new TimeOrder(stateMachine, stateMachine.theThread);
  }
  this.timeOrder = parent.timeOrder;
}


//public static int id(){ return .hashCode(); }


/**Check and returns true if the enclosing state has this state as active one.
 * @return
 */
public final boolean isInState(){
  return enclState == null           //it is the top state.
       || enclState.isInState(this);  //the enclosing state has this as active one.
}


/**This method can be overridden by the user if the user want to write all transition condition in one method
 * or if the user is attempt to set a debug breakpoint for transition check in a specific state.
 * <br><br>
 * If only a breakpoint should be able to set the user should write:
 * Q Override Trans selectTrans(Event<?,?> ev) { return super.selectTrans(ev); }
 * <br><br>
 * If it is overridden then the returned transition should be fired or nothing is to do if null is returned.
 * <br>Template for that method overriding: <pre>
 * Q Override Trans selectTrans(Event<?,?> ev) {
 *   if(ev instanceof MyEvent) return trans_A;
 *   else if(otherCondition) return trans_B;
 *   else return null;
 * }</pre> 
 * The Transitions which are used here should be declared as fields of {@link Trans} which may be overridden anonymously.
 * Especially the {@link Trans#action(EventMsg2)} can be overridden or the field {@link Trans#action} can be set. 
 * An overridden {@link Trans#condition(EventMsg2)} or a set {@link Trans#check} field of that transitions is not regarded.
 * <br>Template for the necessary transition definition:<pre>
 * Trans trans_A = new Trans(Destination.class);   //without action
 * 
 * Trans trans_B = new Trans(Destination.class) {
 *   protected void action(Event<?,?> ev) {
 *     //do action code
 *   }
 * };
 * 
 * Trans trans_C = new Trans(Destination.class) {
 *   { action = ref.action;  //instanceof StateAction, callback 
 *   }
 * };
 * 
 * @param ev The event to test.
 * @return The transition which should be fired or null.
 */
protected Trans selectTrans(EventObject ev) { bCheckTransitionArray = true; return null; }


/**Check all transitions and fire one transition if true.
 * This method is called primary in {@link StateComposite#_processEvent(EventMsg2)} if the state is active.
 * @param ev Given event
 * @return Bits {@link #mRunToComplete}, {@value #mTransit}, {@link #mEventConsumed}, {@link #mStateEntered} to control the event processing.
 */
final int checkTransitions(EventObject ev){
  int res = 0;
  if(!bCheckTransitionArray) {
    //either the first time or overridden check method: Use it.
    Trans trans = selectTrans(ev);
    if(trans !=null){
      trans.doExit();  //exit the current state(s)
      trans.doAction(ev, 0);
      trans.doEntry(ev,0);
      return trans.retTrans;
    }
  }
  if(bCheckTransitionArray && aTransitions !=null) {
    //not overridden check method.
    for(Trans trans1: aTransitions){ //check all transitions
      trans1.doneExit = trans1.doneAction = trans1.doneEntry = false;
      res = trans1.doTrans(ev);
      if((res & (mTransit | mEventConsumed))!=0){
        break;      //transition is used. therefore donot check the rest.
      }
  } }
  return res;
}



/**
 * @param ev
 * @return
 */
final int entryTheState(EventObject ev, int history) { //int isConsumed){
  enclState.stateAct = this;
  enclState.isActive = true;
  //
  ctEntry +=1;
  dateLastEntry = System.currentTimeMillis();
  durationLast = 0;
  //if(this instanceof StateAddParallel){
  //  ((StateAddParallel)this).entryAdditionalParallelBase();
  //}
  if(this instanceof StateComposite){ //quest after StateParallel handling because isActive is set to true.
    StateComposite cthis = (StateComposite)this;
    if((history & (mDeepHistory | mFlatHistory)) !=0) {
      //TODO      
      cthis.stateAct = null;   
      cthis.isActive = true;
    } else {
      //entry for a composite state forces the default state if entries for deeper states are not following.
      cthis.stateAct = null;   
      cthis.isActive = true;
    }
  }
  if(this.timeout !=0 && timeOrder !=null){
    timeOrder.activate(System.currentTimeMillis() + this.timeout);
  }
  entry(ev);  //run the user's entry action.
  return mStateEntered | modeTrans;
  //return isConsumed | modeTrans;
}




/**Exit the state. This method must not be overridden by the user, only the {@link StateCompositeBase} overrides it.
 * Override {@link #exitAction()} for user specific exit behavior.
 * @return The enclosing state, which can used for entry immediately.
 */
StateComposite exitTheState(){ 
  durationLast = System.currentTimeMillis() - dateLastEntry;
  enclState.isActive = false;
  if(timeOrder !=null && timeOrder.used()) {
    stateMachine.theTimer.removeTimeOrder(timeOrder);
  }
  exit();  //run the user's exit action.
  return enclState; 
}



/**Gets the path to this state. The path is build from the {@link #stateId} of all enclosing states
 * separated with a dot and at least this stateId.
 * For example "topStateName.compositeState.thisState". 
 */
CharSequence getStatePath(){
  StringBuilder uPath = new StringBuilder(120);
  StateSimple state = this;
  while((state = state.enclState) !=null){
    uPath.insert(0,'.').insert(0, state.stateId);
  }
  uPath.append('.').append(stateId);
  return uPath;
}



/**Returns the name of the state, for debugging.
 * @return
 */
public String getName(){ return stateId; }

/**Returns the state Id and maybe some more debug information.
 * @see java.lang.Object#toString()
 */
@Override public String toString(){ return stateId; } //return getStatePath().toString(); }


private class EntryMethodAction implements StateAction {
  public int exec(EventObject ev) {
    try { 
      entryMethod.invoke(StateSimple.this, ev);
    } catch(Exception exc) {
      System.err.println(Assert.exceptionInfo("entry " + stateId, exc, 1, 10));  
    }
    return 0;   
  }
}



class ExitMethodAction implements Runnable {
  public void run() {
    try { 
      exitMethod.invoke(StateSimple.this);
    } catch(Exception exc) {
      System.err.println(Assert.exceptionInfo("exit " + stateId, exc, 1, 10));  
    }
  }
};  



class TransitionMethod extends Trans {

  private final Method transitionMethod;
  
  TransitionMethod(String transId, Method method){
    super();
    super.transId = transId;
    transitionMethod = method;
  }
  
  @Override protected void check(EventObject ev)
  {
    try { 
      transitionMethod.invoke(StateSimple.this, ev);
    } catch(Exception exc) {
      System.err.println(Assert.exceptionInfo("exit " + stateId, exc, 1, 10));
    }
  }
  
}



/**Used for a StateAction if any method with the expected signature is given as action method.
 */
private class StateTransitionMethod implements StateAction {

  /**Gotten via reflection - analysis if a State class. */
  final Method transMethod;
  
  /**Instance of the transition with this instance as {@link StateSimple.Trans#check} - aggregation. */
  final Trans trans;
  
  /**Used only in {@link StateSimple#createTransitionList(Object, Trans, int)}. This instance is stored as {@link StateSimple.Trans#check} reference.
   * @param trans The new Trans instance
   * @param transMethod The method detected via reflection-analysis.
   */
  StateTransitionMethod(Trans trans, Method transMethod){
    this.transMethod = transMethod;
    this.trans = trans;
  }
  
  
  /**It is invoked in the non-overridden {@link StateSimple.Trans#check(EventMsg2)}.
   */
  @Override public int exec(EventObject event)
  { Object ret = null;
    try{ 
      transMethod.invoke(StateSimple.this, event, trans);  //invokes a method with signature void method(Event, Trans), only this is detected by reflection anylysis.
      //Note: the method has to set trans.retTrans = mTransit | mEventConsumed to indicate a fired transition.
    }
    catch(IllegalAccessException exc){
      System.err.println(exc);  //should not occur. 
    }
    catch(InvocationTargetException exc){
      Throwable cause = exc.getCause();
      throw new RuntimeException("StateSimple.execTransMethod - exception", cause); //should not occur but should detected.
    }
    return trans.retTrans;  //retTrans should be set inside transMethod.invoke(...)
  }
  
}



/**An instance of this class is used for {@link StateSimple.Trans#check} to check whether it is a timeout.
 * @author hartmut
 *
 */
private class ConditionTimeout implements StateAction {

  @Override public int exec(EventObject event)
  {
    // TODO Auto-generated method stub
    return event instanceof EventTimerMng.TimeEvent ? mEventConsumed : 0;
  }
  
}


  
}
