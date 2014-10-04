package org.vishia.states;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.util.DataAccess;



/**Base class of a State in a State machine.
 * The user should override at least the {@link #trans(Event)} method. This method is the transition to another state.
 * The user can override {@link #entryAction(Event)} and {@link #exit()} if the state has actions on entry and exit. 
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
 * <li>2013-05-11 Hartmut chg: Override {@link #exitAction()} instead {@link #exit()}!
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

/**Bit in return value of a Statemachine's {@link #trans(Event)} or entry method for designation, 
 * that the given Event object was used to switch.
 */
public final static int mEventConsumed = EventConsumer.mEventConsumed;

/**Specification of the consumed Bit in return value of a Statemachine's {@link #trans(Event)} or {@link #entry(int)} 
 * method for designation, that the given Event object was not used to switch. The value of this is 0.
 */
public final static int eventNotConsumed =0x0;

public final static int mTrans = 0x01;

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


private StateTrans[] transitions;


/**An instance of this class represents a transition of the enclosing SimpleState to one or more destination states.
 * The user should build derived anonymous classes for each transition which should be initialized with the destination state classes.
 * The user should override the method {@link #trans(Event)} which should contain the condition and actions: <pre>
 *   class MyState_A extends StateSimple
 *   {
 *     StateTrans trans1_State2 = new StateTrans(MyState_B.class) {
 *       @Override int trans(Event<?,?> ev) {
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
public abstract class StateTrans
{
  
  /**All destination classes from constructor. They are not necessary furthermore after {@link #buildTransitionPath()}
   * because they are processed in the {@link #exitStates} and {@link #entryStates} lists.
   */
  final Class<?>[] dst;
  
  protected Runnable action;
  
  protected Runnable[] choiceConditions;
 
  /**All states which's {@link StateSimple#exit()} have to be processed if the transition is fired. */
  StateSimple[] exitStates;
  
  /**All states which's {@link StateSimple#entry(Event)} have to be processed if the transition is fired. */
  StateSimple[][] entryStates;
  
  /**This constructor is used to initialize the derived anonymous class. */
  public StateTrans(Class<?> ...dst){
    this.dst = dst;
  }
  
  
  private class BuildTransitionPath {
    /**The destination states gotten from the class hashCode and the {@link StateTop#stateMap}.
     * They are the end points of the {@link StateTrans#entryStates}, therefore stored temporary here.
     */
    StateSimple[] dstStates = new StateSimple[dst.length];
    
    
    /**List of 'same path'.
     * If the samePaths[ix] == ixDst then it is the tested path.
     * if samePaths[ix] == ixDst then this path in dstStates[ixDst] is the same like in dstStates[ix].
     * 
     */
    int[] samePaths = new int[dstStates.length];  //all filled with 0

    /**The current indices in {@link #dstStates}.{@link StateSimple#statePath} while evaluating the entry path. */
    int[] ixDstPath = new int[dst.length];
    
    /**Current index to write {@link StateTrans#entryStates}. Pre-increment. */
    int ixEntryStates = -1;
    
    void execute(){
      buildDstStates();
      searchStateCommon();
      buildExitPath();
      buildEntryPath();      
      
    }
    
    private void buildDstStates() {
      //search all dst state instances from the given class. In constructor only the class is known.
      for(int ixdst = 0; ixdst < dst.length; ++ixdst){
        dstStates[ixdst] = enclState.stateTop.stateMap.get(new Integer(dst[ixdst].hashCode()));
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
    BuildTransitionPath d = new BuildTransitionPath();
    d.execute();
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
  protected abstract int trans(Event<?,?> ev);

  
  
  protected class Choice {
    
    protected Choice(Class<?> ...dst){
      
    }
    
    protected Runnable condition;
    
    protected Runnable action;    
  }

  
}


protected StateSimple(){
  this.modeTrans = mRunToComplete;  //default: call trans(), it has not disadvantages
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
      if(DataAccess.isOrExtends(oField.getClass(), StateTrans.class)) {
        StateTrans trans = (StateTrans) oField;
        transitions.add(trans);
        trans.buildTransitionPath();
      }
    }
  } catch(Exception exc){
    exc.getStackTrace();
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
public final int switchto(Event<?,?> ev, Class<? /*extends StateSimple*/> ... dstStateClass ) {
  
  int id = dstStateClass[0].hashCode();
  //Hint: only the stateTop itself has not an enclState.
  StateSimple dstState = enclState.stateTop.stateMap.get(new Integer(id));
  //
  //exit to the current state till a common state with the entryState is found.
  boolean exitFound = false;
  StateSimple stateCommon;  //the common state between this and dstState.
  int zStatePath;
  StateSimple exitState = this;
  do{
    exitState = exitState.exit();      //*** exit();
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
      StateSimple stateP2 = enclState.stateTop.stateMap.get(new Integer(dstClass3.hashCode()));
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
    res = dst1.entry(ev);
  }  
  return res;
}






public final int entry(Event<?,?> ev) { //int isConsumed){
  return entry(ev, 0);
}
  
  
public final int entry(Event<?,?> ev, int recursion) { //int isConsumed){
  if( enclState.enclState != null && !enclState.enclState.isInState(enclState)) {  
    enclState.entry(ev, recursion+1);          //executes the entry action of the enclosing state firstly, recursively call of entry. 
  }
  enclState.stateAct = this;
  enclState.isActive = true;
  //
  ctEntry +=1;
  dateLastEntry = System.currentTimeMillis();
  durationLast = 0;
  //if(this instanceof StateAddParallel){
  //  ((StateAddParallel)this).entryAdditionalParallelBase();
  //}
  if(this instanceof StateParallel && !((StateComposite)this).isActive){
    ((StateComposite)this).isActive = true;  //now set it active.
    //Sets all parallel states to the default state, only on first time of entry this state, because after them it is active.
    //((StateParallel)this).entryParallelBase(ev);
    for(StateComposite state: ((StateParallel)this).states){
      state.stateAct = null;
      state.isActive = true;
    }

  }
  if(recursion == 0 && this instanceof StateComposite){ //quest after StateParallel handling because isActive is set to true.
    //first entry for a composite state forces the default state on first usage.
    ((StateComposite)this).stateAct = null;   
    ((StateComposite)this).isActive = true;
  }
  entryAction(ev);
  if(ev !=null) return mStateEntered | mEventConsumed | modeTrans;
  else return mStateEntered | modeTrans;
  //return isConsumed | modeTrans;
}



/**This method should be overridden if the state needs any entry action. This default method is empty. */
protected void entryAction(Event<?,?> ev){}


final int trans(Event<?,?> ev){
  int res = 0;
  for(StateTrans trans: transitions){ //check all transitions
    res = trans.trans(ev);
    if((res & mTrans)!=0) break;      //transition is used.
  }
  return res;
}


/**Exit the state. This method must not be overridden by the user, only the {@link StateCompositeBase} overrides it.
 * Override {@link #exitAction()} for user specific exit behavior.
 * @return The enclosing state, which can used for entry immediately.
 */
public StateComposite exit(){ 
  durationLast = System.currentTimeMillis() - dateLastEntry;
  enclState.isActive = false;
  exitAction();
  return enclState; 
}


/**This method should be overridden if the state needs any exit action. This default method is empty. */
protected void exitAction(){}

public StateComposite enclState(){ return enclState; }


/**Gets the path to this state. The path is build from the {@link #stateId} of all enclosing states
 * separated with a dot and at least this stateId.
 * For example "topStateName.compositeState.thisState". 
 */
public CharSequence getStatePath(){
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





  
}
