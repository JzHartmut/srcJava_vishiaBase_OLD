package org.vishia.states;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;



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
 * StateSimpleBase<StateComposite> stateA = new StateSimpleBase(this, "stateA", true){
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
public final static int eventNotConsumed =0x0;

/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has non-event-driven transitions, therefore the trans method should be called
 * in the same cycle.
 */
public final static int mRunToComplete =0x2;

/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has entered yet. If this bit is not set, an {@link #entry(Event)} action is not called.
 * It means a state switch has not occurred. Used for debug.
 */
public final static int mStateEntered = 0x4;


/**If a composite state is leaved, any other parallel composite states don't processed then. */
public final static int mStateLeaved = 0x10;


/**Reference to the enclosing state. With this, the structure of state nesting can be observed in data structures.
 * The State knows its enclosing state to set and check the state identification there. 
 */
protected StateComposite enclState;

/**Path to this state from the top state. The element [0] is the top state.
 * The last element is the enclosing state.
 */
protected StateSimple[] statePath;

/**The own identification of the state. It is given by constructor. */
//final EnumState stateId;

protected String stateId;

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

private StateTrans[] transitions;


/**An instance of this class represents a transition of the enclosing SimpleState to one or more destination states.
 * There are three forms to program transitions in a state class:
 * <br><br><b>Simple transition method in a state class:</b><br>
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
 * <br><br>
 * <b>Transition class:</b><br>
 * 
 * The user should build derived anonymous classes for each transition which should be initialized with the destination state classes.
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
  
  public int retTrans;
  
  /**All destination classes from constructor. They are not necessary furthermore after {@link #buildTransitionPath()}
   * because they are processed in the {@link #exitStates} and {@link #entryStates} lists.
   */
  final Class<?>[] dst;
  
  /**If a condition instance is given, its {@link StateAction#action(Event)}-method is called to test whether the transition should fire.
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
 
  /**All states which's {@link StateSimple#exitTheState()} have to be processed if the transition is fired. */
  StateSimple[] exitStates;
  
  /**All states which's {@link StateSimple#entry(Event)} have to be processed if the transition is fired. */
  StateSimple[][] entryStates;
  
  private final String description;
  
  
  /**This constructor is used to initialize a derived anonymous class with one or more destination state of this transition.
   * This constructor should not be used if a transition class contains {@link #choice}-transitions.
   */
  public StateTrans(String description, Class<?> ...dst){
    if(dst.length ==0) throw new IllegalArgumentException("should have at least one destination state: " + stateId + ": trans " + description);
    this.dst = dst;
    this.description = description;
  }
  
  
  /**Constructs a transition with {@link #choice} sub-transitions.
   */
  public StateTrans(String description){
    this.dst = null;
    this.description = description;
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
    
    /**Current index to write {@link StateTrans#entryStates}. Pre-increment. */
    int ixEntryStates = -1;
    
    
    public BuildTransitionPath(StateSimple[] exitStates)
    {
      this.exitStates = exitStates;
    }
    
    void execute(){
      buildDstStates();
      searchStateCommon();
      buildExitPath();
      buildEntryPath();      
      
    }
    
    private void buildDstStates() {
      //search all dst state instances from the given class. In constructor only the class is known.
      if(dst !=null) for(int ixdst = 0; ixdst < dst.length; ++ixdst){
        dstStates[ixdst] = enclState.stateMachine.stateMap.get(new Integer(dst[ixdst].hashCode()));
      }
    }
    
    private void searchStateCommon() {
      //search the common state for all transitions. 
      StateSimple stateCommon;  //the common state between this and dstState.
      int zStatePath =1;
      int ixSrcPath = statePath.length -2;  //Note: the commonState is never this itself, at least one exit.
      do {
        stateCommon = StateSimple.this.statePath[ixSrcPath];  
        int ixdst = -1;
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
      } while(stateCommon == null && --ixSrcPath >=0);     
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
    
    
    private void buildEntryPath() {
      
      //this array contains which dst paths a the same. 
      
      
      
      //for()
      int lengthEntryStates = 0;
      for(int ixDst = 0; ixDst < dstStates.length; ++ixDst){
        int l1 = dstStates[ixDst].statePath.length - ixDstPath[ixDst];
        if(l1 > lengthEntryStates){
          lengthEntryStates = l1;
        }
      }
      StateTrans.this.entryStates = new StateSimple[lengthEntryStates][];
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
        if(dst1 instanceof StateParallel) {
          //check whether all parallel states are entere Elsewhere use the default state in the non-entered branch. 
          
        }
        int nrofBranchs = ixEntryStates < 0 ? 1 : StateTrans.this.entryStates[ixEntryStates].length;
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
        bAllDstReached = writeEntryPathItem(nrofBranchs);
      } while(!bAllDstReached);
      
    }
    
    
    void walkThroughEntries() {
      
    }
    
    
    private boolean writeEntryPathItem(int nrofBranchs) {
      boolean bAllDstReached = true;
      StateTrans.this.entryStates[++ixEntryStates] = new StateSimple[nrofBranchs];
      int ixEntry = -1;
      for(int ixDst = 0; ixDst < dstStates.length; ++ixDst) {
        //don't store the same entry twice, check:
        if(samePaths[ixDst] == ixDst) { //it is an own path
          StateTrans.this.entryStates[ixEntryStates][++ixEntry] = dstStates[ixDst].statePath[ixDstPath[ixDst]];
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
  
  
  /**Checks the trigger and conditions of a state transition. The user should override this method in form (example)
   * <pre>
  public int trans(Event ev){
    TypeOfStateComposite enclState;
    if(ev instanceof MyEvent and ((MyEvent)ev).getCmd() == myExpectedCmd){
      enclState = exit();
      statementsOfTransition();
      return enclState.otherState.entry(consumed);
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
    if(condition !=null){
      int bCond = condition.action(ev);
      if(bCond ==0) return 0;  //don't fire.
      if(choice !=null) {
        //check all choice, one of them have to be true
        for(StateTrans choice1: choice) {
          
        }
      }
    }
    return 0;
  }

  
  
  /**Executes the exit from this and all enclosing States to fire the transition.
   * 
   */
  public void doExit()
  {
    for(StateSimple state: exitStates){
      state.exitTheState();
    }
  }
  
  
  
  public int doEntry(Event<?,?> ev)
  {
    for(StateSimple[] stateParallel: entryStates){
      for(StateSimple state: stateParallel) {
        state.entryTheState(ev);
      }
    }
    retTrans = mEventConsumed;
    return 0;
  }
  
  @Override public String toString(){ return description; }
  
  protected class Choice {
    
    protected Choice(Class<?> ...dst){
      
    }
    
    protected Runnable condition;
    
    protected Runnable action;    
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


/**This method may be overridden for a entry action. In that case the {@link #entry} or {@link #entryMethod} is either not used
 * or it is used especially in the overridden method responsible to the user.
 * If this method is not overridden, a given {@link StateAction} stored on {@link #entry} is executed.
 * That may be the execution of the {@link #entryMethod}.
 * @param ev The data of the event can be used for the entry action.
 * @return {@link #mRunToComplete} if next states should be tested for conditions.
 */
protected int entry(Event<?,?> ev) {
  if(entry !=null) { return entry.action(ev); }
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



void createTransitionList(){
  List<StateTrans>transitions = new LinkedList<StateTrans>();
  try{
    Class<?> clazz = this.getClass();
    Field[] fields = clazz.getDeclaredFields();
    for(Field field: fields){
      field.setAccessible(true);
      Object oField = field.get(this);
      if(oField !=null && DataAccess.isOrExtends(oField.getClass(), StateTrans.class)) {
        StateTrans trans = (StateTrans) oField;
        transitions.add(trans);
        trans.buildTransitionPath();
      }
    }
    Method[] methods = this.getClass().getDeclaredMethods();
    for(Method method: methods) {
      String name = method.getName();
      Class<?>[] argTypes = method.getParameterTypes();
      Class<?> retType = method.getReturnType();
      if(argTypes.length==2 && argTypes[0] == Event.class && retType == StateTrans.class && argTypes[1] == StateTrans.class) {
        //a transition method
        StateTrans trans = null;
        try{ 
          method.setAccessible(true);
          Object oTrans = method.invoke(this,  null, null); 
          trans = (StateTrans)oTrans;
          trans.condition = new StateActionMethod(trans, method);
          trans.buildTransitionPath();
          transitions.add(trans);
        } catch(Exception exc){ throw new IllegalArgumentException("stateTrans-method failure"); }
      }
    }
  } catch(Exception exc){
    exc.printStackTrace();
  }   
  this.transitions = transitions.toArray(new StateTrans[transitions.size()]);
}


//public static int id(){ return .hashCode(); }


/**Check and returns true if the enclosing state has this state as active one.
 * @return
 */
public final boolean isInState(){
  return enclState == null           //it is the top state.
       || enclState.isInState(this);  //the enclosing state has this as active one.
}

/**This method sets this state in its enclosing composite state.
 * If the enclosing state is not the active state of its enclosing state, then the entry is invoked recursively firstly for the enclosing state.
 * It is possible to program the entry in a deep nested state, the entry action for all enclosing states are executed automatically be recursive call.
 * <br><br>
 * This is the only one {@link #entry(Event)} for all derived state types. It is final, not able to override. 
 * For the composite and parallel states the necessary actions are done here too. 
 * That may be more simple than overridden methods for composite and parallel states. All what is done can be seen in only one method.
 * <br><br>
 * It calls {@link #entryAction()}. 
 * @param ev The event from the transition. It is transferred to the {@link #entryAction(Event)}, especially event data
 *   can be used there. If the event is not used for transition but given, this entry action have to be called with entry(null).
 * @return Bits: 
 * <ul>
 * <li>If ev not null, then bit {@link #mEventConsumed}. 
 * <li>If the state is constructed with {@link #StateSimpleBase(StateCompositeBase, String, boolean)} with true
 *   because it has non-event transitions, the bit {@link #mRunToComplete}, see {@link #modeTrans}.
 * <li>{@value #mStateEntered} anyway.  
 * </ul>
 */
//@SafeVarargs 
public final int XXXswitchto(Event<?,?> ev, Class<? /*extends StateSimple*/> ... dstStateClass ) {
  
  int id = dstStateClass[0].hashCode();
  //Hint: only the stateTop itself has not an enclState.
  StateSimple dstState = enclState.stateMachine.stateMap.get(new Integer(id));
  //
  //exit to the current state till a common state with the entryState is found.
  boolean exitFound = false;
  StateSimple stateCommon;  //the common state between this and dstState.
  int zStatePath;
  StateSimple exitState = this;
  do{
    exitState = exitState.exitTheState();      //*** exit();
    stateCommon = dstState;
    zStatePath = 1;
    while(stateCommon !=null && !exitFound){
      if( stateCommon == exitState){ 
        exitFound = true;
      } else {
        stateCommon = stateCommon.enclState;
        zStatePath +=1;
      }
    }
  } while( !exitFound );
  //
  //    |---->(----->dst0 )
  //--->|            
  //    |---->(----->|--->dst1
  //                 |--->dst2
  //
  //check whether there are more as one entry states:
  StateParallel[][] statesParallel;
  if( dstStateClass.length >1 ) {
    statesParallel = new StateParallel[dstStateClass.length][];
    for(int ixdst = 0; ixdst < dstStateClass.length; ++ixdst) {
      Class<?> dstClass3 = dstStateClass[ixdst];
      StateSimple stateP2 = enclState.stateMachine.stateMap.get(new Integer(dstClass3.hashCode()));
      if(stateP2 instanceof StateParallel){
        //if(statesParallel[ixdst])
      }
      if(stateP2 !=null){
        
      }
    }
  }
  //
  //now entry in the dstState, but across the dst path:
  StateSimple[] statePath = new StateSimple[zStatePath];
  int ixStatePath;
  //build the state path backward from dstState to the common State.
  //the common state is reached in zStatePath steps.
  for(ixStatePath = zStatePath-1; ixStatePath >=0; --ixStatePath){
    statePath[ixStatePath] = dstState;
    dstState = dstState.enclState;
  }
  //now entry all states.
  int res = 0;
  for(ixStatePath = 0; ixStatePath < zStatePath; ++ixStatePath){
    StateSimple dst1 = statePath[ixStatePath];
    if(dst1 instanceof StateParallel) {
      //check whether any further dst state has this parallel state:
      StateParallel stateParallel = (StateParallel)dst1;
      for(StateComposite stP1: stateParallel.states){
        
      }
      
      
    }
    res = dst1.entryTheState(ev);
  }  
  return res;
}






/**Check all transitions and fire one transition if true.
 * @param ev Given event
 * @return
 */
final int checkTransitions(Event<?,?> ev){
  int res = 0;
  for(StateTrans trans: transitions){ //check all transitions
    res = trans.trans(ev);
    if((res & mEventConsumed)!=0) break;      //transition is used. TODO
  }
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
  entry(ev);  //run the user's entry action.
  if(ev !=null) return mStateEntered | mEventConsumed | modeTrans;
  else return mStateEntered | modeTrans;
  //return isConsumed | modeTrans;
}




/**Exit the state. This method must not be overridden by the user, only the {@link StateCompositeBase} overrides it.
 * Override {@link #exitAction()} for user specific exit behavior.
 * @return The enclosing state, which can used for entry immediately.
 */
StateComposite exitTheState(){ 
  durationLast = System.currentTimeMillis() - dateLastEntry;
  enclState.isActive = false;
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


/**Returns the state Id and maybe some more debug information.
 * @see java.lang.Object#toString()
 */
@Override public String toString(){ return getStatePath().toString(); }


private class EntryMethodAction implements StateAction {
  public int action(Event<?,?> ev) {
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
  
  TransitionMethod(String description, Method method){
    super(description);
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



private class StateActionMethod implements StateAction {

  final Method transMethod;
  
  final StateTrans trans;
  
  StateActionMethod(StateTrans trans, Method transMethod){
    this.transMethod = transMethod;
    this.trans = trans;
  }
  
  
  @Override public int action(Event<?, ?> event)
  {
    try{ transMethod.invoke(StateSimple.this, event, trans); }
    catch(Exception exc){
      System.err.println(exc);
      return 0;
    }
    return trans.retTrans;
  }
  
}


  
}
