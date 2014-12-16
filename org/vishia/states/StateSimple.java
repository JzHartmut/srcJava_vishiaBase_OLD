package org.vishia.states;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventTimerMng;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;



/**Base class of a State in a State machine.
 * The user should override at least the {@link #checkTransitions(Event)} method. This method is the transition to another state.
 * The user can override {@link #entryAction(Event)} and {@link #exitTheState()} if the state has actions on entry and exit. 
 * But one should call super.exit(); as first statement!
 * <br><br>
 * A State is a small set of data to refer its enclosing state and a set of methods. 
 * <br><br>
 * A simple state can be instantiated with an anonymous class inside its enclosing state. 
 * It has not more necessary additional elements which should access from outside:
 * <pre>
 * StateSimple stateA = new StateSimple(this, "stateA", true){
 *   (add)Override protected int trans(Event<?, ?> evP){
 *     //...transitions
 *   }
 * };
 * </pre>
 * <br><br>
 * @author Hartmut Schorrig
 *
 * @param <StateComposite> The derived type of the enclosing state which contains this State. This parameter is necessary
 *   to check the correct state arrangement in compile time. Any state should be member of an enclosing state.
 */
public abstract class StateSimple
{
  
/**Version, history and license.
 * <ul>
 * <li>2014-11-09 Hartmut new: {@link #setAuxInfo(Object)}, {@link #auxInfo()} used for state machine generation for C/C++
 * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateSimpleBase}, changed concept: 
 *   Nested writing of states, less code, using reflection for missing instances and data. 
 * <li>2013-05-11 Hartmut chg: Override {@link #exitAction()} instead {@link #exitTheState()}!
 * <li>2013-04-27 Hartmut chg: The {@link #entry(Event)} and the {@link #entryAction(Event)} should get the event
 *   from the transition. It needs adaption in users code. The general advantage is: The entry action can use data
 *   from the event. A user algorithm does not need to process the events data only in the transition. A user code
 *   can be executed both in a special transition to a state and as entry action. Both possibilities do not distinguish
 *   in formal possibilities. The second advantage is: If the event is used, it should given to the next entry. If it is
 *   not used, a 'null' should be given. The user need not pay attention in the correct usage of {@link #mEventConsumed} or not.
 * <li>2013-04-27 Hartmut new: {@link #mStateEntered} is returned on any {@link #entry(Event)}. If the state is not changed,
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

/**Bit in return value of a Statemachine's {@link #checkTransitions(Event)} or entry method for designation, 
 * that the given Event object was used to switch. It is 0x01.
 */
public final static int mEventConsumed = EventConsumer.mEventConsumed;


/**Specification of the consumed Bit in return value of a Statemachine's {@link #checkTransitions(Event)} or {@link #entry(int)} 
 * method for designation, that the given Event object was not used to switch. The value of this is 0.
 */
public final static int notTransit =0x0;


/**Bit in return value of a Statemachine's trans method for designation, 
 * that the given Transition is true. The bit should be set together with {@link #mEventConsumed} if the event forces true.
 * If this bit is set without {@link #mEventConsumed} though an event is given the transition has not tested the event.
 * In that case the event may be used in the following run-to-complete transitions.
 */
public final static int mTransit = 0x2;


/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has entered yet. If this bit is not set, an {@link #entry(Event)} action is not called.
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

/**Set to true if the {@link #selectTrans(Event)} is not overridden. Then check the {@link #transitions} -list */
private boolean bCheckTransitionArray;

private StateTrans[] transitions;


/**An instance of this class represents a transition of the enclosing SimpleState to one or more destination states.
 * There are three forms to program transitions in a state class:
 * <br><br>
 * <b>Simple transition method in a state class:</b><br>
 * The user can write a method with the following form: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     StateTrans trans1_State2(Event<?,?> ev, StateTrans trans) {
 *       if(trans == null) return new StateTrans(MyState_B.class);
 *       if(condition && ev instanceof MyEvent && (ev.getCmd == MyCmd){
 *         doExit();
 *         myAction();
 *         doEntry();
 *       }
 *       return trans;
 *     }  
 * </pre>
 * The method will be detected by checking the State class content via reflection.
 * The StateTrans instance will be build by invoking this method with null as ev and trans parameter.
 * That method is used as condition method via an instance of {@link StateTransitionMethod}. 
 * That method have to contain the action and the {@link #doExit()}, the action and the {@link #doEntry(Event)} like shown above. 
 * <br><br>
 * <b>Transition class:</b><br>
 * The user can write a class derived from {@link StateTrans} for each transition. 
 * The parameterless constructor of this should invoke the super constructor with the destination state(s) class which should be initialized with the destination state classes.
 * The user should override the method {@link #trans(Event)} which should contain the condition and actions: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     StateTrans trans1_State2 = new StateTrans(MyState_B.class) {
 *       QOverride int trans(Event<?,?> ev) {
 *         if(condition && ev instanceof MyEvent && (ev.getCmd == MyCmd){
 *           doExit();
 *           myAction();
 *           doEntry();
 *         }
 *       }  
 *     }; 
 * </pre>
 * <br><br>
 * <b>Transition class or transition instance:</b><br>
 * The user can build either a class derived from {@link StateTrans} or as instance with a derived anonymous classes of {@link StateTrans}
 * for each transition. which should be initialized with the destination state classes.
 * The user should override the method {@link #trans(Event)} which should contain the condition and actions: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     StateTrans trans1_State2 = new StateTrans(MyState_B.class) {
 *       QOverride int trans(Event<?,?> ev) {
 *         if(condition && ev instanceof MyEvent && (ev.getCmd == MyCmd){
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
 */
public class StateTrans
{
  
  /**Return value of the last used transition test. For debugging and for the {@link TransitionMethod}. */
  public int retTrans;
  
  /**All destination classes from constructor. They are not necessary furthermore after {@link #buildTransitionPath()}
   * because they are processed in the {@link #exitStates} and {@link #entryStates} lists.
   */
  //final Class<?>[] dst;
  final int[] dst;
  
  
  /**If a condition instance is given, its {@link StateAction#exec(Event)}-method is called to test whether the transition should fire.
   * The condition can contain action statements. But that is not allowed, if some {@link #choice} conditions are set.
   * if the condition contains only a condition, it should return 0 if it is false and not {@link StateSimple#mEventConsumed}, if it is true.
   * The event can be used for condition or not. If events are not used <code>null</code> can be supplied instead the event. 
   * 
   */
  protected StateAction condition;
  
  /**If an action is given, the condition should not contain an action itself. The action is executed after the exitState()-operation
   * and before the entryState()-operation. */
  protected StateAction action;
  
  /**More as one choice can be given if this transition is a choice-transition. 
   * The transition is only fired if at least one choice-condition is true. The {@link #action} of the state and the {@link #action} 
   * of the choice is executed one after another after exit() and before entry() only if the transition is fired.
   * If some choice are given, the choice should contain the destination state. The constructor of this transition have to be empty.
   */
  protected StateTrans[] choice;
 
  
  /**It is null for a simple transition in a state. It contains the parent transition for a choice, which is an inner transition. */
  StateTrans parent;
  
  /**All states which's {@link StateSimple#exitTheState()} have to be processed if the transition is fired. */
  StateSimple[] exitStates;
  
  /**All states which's {@link StateSimple#entry(Event)} have to be processed if the transition is fired. */
  StateSimple[] entryStates;
  StateSimple[][] entryStatesOld;
  
  String transId;
  
  
  /**This constructor is used to initialize a derived anonymous class with one or more destination state of this transition.
   * This constructor should not be used if a transition class contains {@link #choice}-transitions.
   */
  public StateTrans(Class<?> ...dst){
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
  public StateTrans(int[] dstKeys){
    this.dst = dstKeys;
  }
  
  /**Constructs a transition with {@link #choice} sub-transitions.
   */
  public StateTrans(){
    this.dst = null;
  }
  
  
  /**An instance of this class is created only temporary to build the transition path, set the #exitStates
   * and #entryStates.
   */
  private class BuildTransitionPath {
    /**The destination states filled with {@link #buildDstStates()} with the length of {@link #dst}.
     * Left null if no dst states are given, especially on existing {@link StateTrans#choice}.
     */
    final StateSimple[] dstStates = StateTrans.this.dst == null ? null : new StateSimple[StateTrans.this.dst.length];
    
    final StateSimple[] exitStates;
    
    /**List of 'same path'.
     * If the samePaths[ix] == ixDst then it is the tested path.
     * if samePaths[ix] == ixDst then this path in dstStates[ixDst] is the same like in dstStates[ix].
     * 
     */
    int[] samePaths = this.dstStates == null ? null : new int[this.dstStates.length];  //all filled with 0

    /**The current indices in {@link #dstStates}.{@link StateSimple#statePath} while evaluating the entry path. */
    int[] ixDstPath = this.dstStates == null ? null : new int[this.dstStates.length];
    
    /**Current index to write {@link StateTrans#entryStatesOld}. Pre-increment. */
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
      StateTrans.this.exitStates = new StateSimple[StateSimple.this.statePath.length - (ixSrcPath +1)];  //don't exit the stateCommon
      for(int ixdst = 0; ixdst < dst.length; ++ixdst){
        ixDstPath[ixdst] +=1;  //first entry in state after stateCommon.  
      }
    }
    
    
    
    private void buildExitPath() {
      int ixSrcPath = StateSimple.this.statePath.length;
      for(int ixExitStates = 0; ixExitStates < StateTrans.this.exitStates.length; ++ixExitStates){
        StateTrans.this.exitStates[ixExitStates] = StateSimple.this.statePath[--ixSrcPath];  //copy the statePath from this backward to exitStates-List.
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
      StateTrans.this.entryStates = listEntries.toArray(new StateSimple[listEntries.size()]);
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
      StateTrans.this.entryStatesOld = new StateSimple[lengthEntryStates][];
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
        int nrofBranchs = ixEntryStatesOld < 0 ? 1 : StateTrans.this.entryStatesOld[ixEntryStatesOld].length;
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
      StateTrans.this.entryStatesOld[++ixEntryStatesOld] = new StateSimple[nrofBranchs];
      int ixEntry = -1;
      for(int ixDst = 0; ixDst < dstStates.length; ++ixDst) {
        //don't store the same entry twice, check:
        if(samePaths[ixDst] == ixDst) { //it is an own path
          StateTrans.this.entryStatesOld[ixEntryStatesOld][++ixEntry] = dstStates[ixDst].statePath[ixDstPath[ixDst]];
          if(ixDstPath[ixDst] < dstStates[ixDst].statePath.length -1){
            bAllDstReached = false;  //continue!
          }
        }
        ixDstPath[ixDst] +=1;  //increment all.
      }
      return bAllDstReached;
    }
    
  }
  
  
  /**Builds the transition path from given state to all dst states. Called on startup.
   * 
   */
  void buildTransitionPath(){
    if(choice !=null) {
      for(StateTrans choice1: choice) {
        BuildTransitionPath d = choice1.new BuildTransitionPath(this.exitStates);
        d.execute();
      }
    } else {
      BuildTransitionPath d = new BuildTransitionPath(this.exitStates);
      d.execute();
    }
  }
  

  
  /**The non-overridden condition checks the {@link #condition} or returns 0 if it is not given.
   * @param ev event
   * @return not 0, especially {@link StateSimple#mTransit} or {@link StateSimple#mEventConsumed} then true, transition should fire. 
   */
  protected int condition(Event<?,?> ev) {
    if(condition !=null){
      return condition.exec(ev);  //may set mEventConsumed or
    } else return 0;  //don't fire.
  }
  
  
  
  /**The non-overridden action executes the {@link #action} or returns 0 if it is not given.
   * @param ev event
   * @return especially {@link StateSimple#mRunToComplete} for state control, elsewhere 0. 
   */
  protected void action(Event<?,?> ev) {
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
   *   but it is used in parallel states. See {@link StateCompositeBase#processEvent(Event)} and {@link StateParallelBase#processEvent(Event)}.
   *   Returns 0 if a state switch is not processed. Elsewhere {@link #mStateEntered}. {@link #mStateLeaved}
   */
  protected int trans(Event<?,?> ev){
    retTrans = condition(ev);
    if(retTrans ==0) return 0;  //don't fire.
    else {
      if(choice !=null) {
        //check all choice, one of them have to be true
        for(StateTrans choice1: choice) {  //check all choices
          retTrans = choice1.trans(ev);   //the choice determines the return. If the conditions returns mEventConsumed it is not consumed if no choice.     
          if(retTrans !=0) return retTrans;          //if true than return, don't check the other!
        }
        return 0; //if no choice found
      } else {
        doExit();  //exit the current state(s)
        doAction(this.parent, ev, 0);
        doEntry(ev);
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
  private void doAction(StateTrans parent, Event<?,?> ev, int recurs) {
    if(recurs > 20) throw new IllegalArgumentException("too many recursions");
    if(parent !=null) {
      doAction(parent.parent, ev, recurs+1);  //recursion to the first parent
    }
    action(ev);
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
  }
  
  
  
  /**Entry in all states for this transition.
   * The states will be entered in the order from outer to inner state
   * and for all parallel states then from outer to inner.
   * @param ev
   */
  public final void doEntry(Event<?,?> ev)
  { 
    //for(StateSimple stateParallel: entryStatesOld){
      for(StateSimple state: entryStates) { //stateParallel) {
        retTrans |= state.entryTheState(ev);
      }
    //}
  }
  
  @Override public String toString(){ return transId == null ? "-unknown transId" : transId; }
  

  
}



/**If a state contains a field of this class, a timeout is given for that state.
 */
public class Timeout extends StateTrans
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
protected int entry(Event<?,?> ev) {
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
void createTransitionList(Object encl, StateTrans parent, int nRecurs){
  List<StateTrans>transitions = new LinkedList<StateTrans>();
  Class<?> clazz = encl.getClass();
  if(nRecurs > 2) {
    Debugutil.stop();
  }
  if(nRecurs > 10) throw new IllegalArgumentException("too many recursion, choice-transition; state=" + stateId);
  try{
    Class<?>[] clazzs = clazz.getDeclaredClasses();
    for(Class<?> clazz1: clazzs){
      if(DataAccess.isOrExtends(clazz1, StateTrans.class)){
        @SuppressWarnings("unused")  //only test
        Constructor<?>[] ctora = clazz1.getDeclaredConstructors();
        //Note: the constructor needs the enclosing class as one parameter because it is non-static.
        //The enclosing instance is the StateSimple always, also on choice-transitions.
        Constructor<?> ctor1 = clazz1.getDeclaredConstructor(StateSimple.this.getClass());
        ctor1.setAccessible(true);
        StateTrans trans = (StateTrans)ctor1.newInstance(this);
        trans.transId = clazz1.getSimpleName();
        transitions.add(trans);
        trans.parent = parent;  //null for a state transition
        checkBuiltTransition(trans, nRecurs);
      }
    }
    Field[] fields = clazz.getDeclaredFields();
    for(Field field: fields){
      field.setAccessible(true);
      Object oField = field.get(encl);
      if(oField !=null && DataAccess.isOrExtends(oField.getClass(), StateTrans.class)) {
        String sFieldName = field.getName();
        if(!DataAccess.isReferenceToEnclosing(field)) { //don't test the enclosing instance, it is named this$0 etc. 
          StateTrans trans = (StateTrans) oField;
          trans.transId = sFieldName;
          transitions.add(trans);
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
      if(argTypes.length==2 && argTypes[0] == Event.class && retType == StateTrans.class && argTypes[1] == StateTrans.class) {
        //a transition method
        StateTrans trans = null;
        try{ 
          method.setAccessible(true);
          Object oTrans = method.invoke(encl,  null, null); 
          trans = (StateTrans)oTrans;
          trans.transId = method.getName();
          trans.condition = new StateTransitionMethod(trans, method);
          trans.buildTransitionPath();
          trans.parent = parent;  //null for a state transition
          transitions.add(trans);
        } catch(Exception exc){ throw new IllegalArgumentException("stateTrans-method failure"); }
      }
    }
  } catch(Exception exc){
    exc.printStackTrace();
  }  
  if(transitions.size() >0) {
    if(parent == null) {
      this.transitions = transitions.toArray(new StateTrans[transitions.size()]);
    } else {
      parent.choice = transitions.toArray(new StateTrans[transitions.size()]);
    }
  }
}



protected void prepareTransitions() {
  if(transitions !=null) {
    for(StateTrans trans: transitions) {
      checkBuiltTransition(trans, 0);
    }
  }
}




/**Creates the empty yet array of transitions. 
 * Invoked if transitions should be defined from any Java program outside, not from the Source of this class. Used for StateMgen.
 * @param nrofTransitions Number should be the number of transitions to add, see {@link #addTransition(StateTrans)}
 */
public void createTransitions(int nrofTransitions) {
  transitions = new StateTrans[nrofTransitions];
}


/**Adds a transition.
 * Invoked if transitions should be defined from any Java program outside, not from the Source of this class. Used for StateMgen.
 * @param trans
 */
public void addTransition(StateSimple.StateTrans trans) {
  int ix = 0;
  while(ix < transitions.length && transitions[ix] !=null){ ix +=1; } //search next free
  if(ix >= transitions.length) throw new IllegalArgumentException("too many states to add");
  transitions[ix] = trans;

}





private void checkBuiltTransition(StateTrans trans, int nRecurs) {
  if(trans.dst !=null) {
    trans.buildTransitionPath();
  }
  if(trans instanceof Timeout) {
    Timeout timeoutTrans = (Timeout)trans;
    trans.condition = new ConditionTimeout();
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
    parent.timeOrder = stateMachine.theTimer.new TimeOrder(stateMachine.processEvent, stateMachine.theThread);
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
 * Q Override StateTrans selectTrans(Event<?,?> ev) { return super.selectTrans(ev); }
 * <br><br>
 * If it is overridden then the returned transition should be fired or nothing is to do if null is returned.
 * <br>Template for that method overriding: <pre>
 * Q Override StateTrans selectTrans(Event<?,?> ev) {
 *   if(ev instanceof MyEvent) return trans_A;
 *   else if(otherCondition) return trans_B;
 *   else return null;
 * }</pre> 
 * The Transitions which are used here should be declared as fields of {@link StateTrans} which may be overridden anonymously.
 * Especially the {@link StateTrans#action(Event)} can be overridden or the field {@link StateTrans#action} can be set. 
 * An overridden {@link StateTrans#condition(Event)} or a set {@link StateTrans#condition} field of that transitions is not regarded.
 * <br>Template for the necessary transition definition:<pre>
 * StateTrans trans_A = new StateTrans(Destination.class);   //without action
 * 
 * StateTrans trans_B = new StateTrans(Destination.class) {
 *   protected void action(Event<?,?> ev) {
 *     //do action code
 *   }
 * };
 * 
 * StateTrans trans_C = new StateTrans(Destination.class) {
 *   { action = ref.action;  //instanceof StateAction, callback 
 *   }
 * };
 * 
 * @param ev The event to test.
 * @return The transition which should be fired or null.
 */
protected StateTrans selectTrans(Event<?,?> ev) { bCheckTransitionArray = true; return null; }


/**Check all transitions and fire one transition if true.
 * This method is called primary in {@link StateComposite#_processEvent(Event)} if the state is active.
 * @param ev Given event
 * @return Bits {@link #mRunToComplete}, {@value #mTransit}, {@link #mEventConsumed}, {@link #mStateEntered} to control the event processing.
 */
final int checkTransitions(Event<?,?> ev){
  int res = 0;
  if(!bCheckTransitionArray) {
    //either the first time or overridden check method: Use it.
    StateTrans trans = selectTrans(ev);
    if(trans !=null){
      trans.doExit();  //exit the current state(s)
      trans.doAction(trans.parent, ev, 0);
      trans.doEntry(ev);
      return trans.retTrans;
    }
  }
  if(bCheckTransitionArray && transitions !=null) {
    //not overridden check method.
    for(StateTrans trans1: transitions){ //check all transitions
      res = trans1.trans(ev);
      if((res & (mTransit | mEventConsumed))!=0) break;      //transition is used. TODO
  } }
  return res;
}



/**
 * @param ev
 * @return
 */
final int entryTheState(Event<?,?> ev) { //int isConsumed){
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
    //first entry for a composite state forces the default state on first usage.
    ((StateComposite)this).stateAct = null;   
    ((StateComposite)this).isActive = true;
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
  public int exec(Event<?,?> ev) {
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



class TransitionMethod extends StateTrans {

  private final Method transitionMethod;
  
  TransitionMethod(String transId, Method method){
    super();
    super.transId = transId;
    transitionMethod = method;
  }
  
  @Override protected int trans(Event<?, ?> ev)
  {
    try { 
      Object result = transitionMethod.invoke(StateSimple.this, ev);
      return ((Integer)result).intValue();
    } catch(Exception exc) {
      System.err.println(Assert.exceptionInfo("exit " + stateId, exc, 1, 10));
      return 0;
    }
  }
  
}



/**Used for a StateAction if any method with the expected signature is given as action method,
 * not a StateAction class and not a StateAction instance.
 */
private class StateTransitionMethod implements StateAction {

  final Method transMethod;
  
  final StateTrans trans;
  
  StateTransitionMethod(StateTrans trans, Method transMethod){
    this.transMethod = transMethod;
    this.trans = trans;
  }
  
  
  @Override public int exec(Event<?, ?> event)
  { trans.retTrans = 0;  //initial, it should be set by the transMethod. 
    try{ transMethod.invoke(StateSimple.this, event, trans); }
    catch(Exception exc){
      System.err.println(exc);
      return 0;
    }
    return trans.retTrans; //Note that the transMethod returns the transition instance for initializing.
    //The transition method should set that retTrans to provide the return value of trans.
  }
  
}



/**An instance of this class is used for {@link StateSimple.StateTrans#condition} to check whether it is a timeout.
 * @author hartmut
 *
 */
private class ConditionTimeout implements StateAction {

  @Override public int exec(Event<?, ?> event)
  {
    // TODO Auto-generated method stub
    return event instanceof EventTimerMng.TimeEvent ? 1 : 0;
  }
  
}


  
}
