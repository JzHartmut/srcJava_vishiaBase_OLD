package org.vishia.stateMachine;

import org.vishia.util.Event;
import org.vishia.util.EventConsumer;

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
 * StateSimpleBase<EnclosingState> stateA = new StateSimpleBase(this, "stateA", true){
 *   (add)Override protected int trans(Event<?, ?> evP){
 *     //...transitions
 *   }
 * };
 * </pre>
 * <br><br>
 * @author Hartmut Schorrig
 *
 * @param <EnclosingState> The derived type of the enclosing state which contains this State. This parameter is necessary
 *   to check the correct state arrangement in compile time. Any state should be member of an enclosing state.
 */
public abstract class StateSimpleBase<EnclosingState extends StateCompositeBase<EnclosingState,?>>{
    
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
  protected final EnclosingState enclState;
  
  /**The own identification of the state. It is given by constructor. */
  //final EnumState stateId;
  
  protected final String stateId;
  
  
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

  /**The super constructor. 
   * @param superState The enclosing state which contains this state. It is either a {@link StateTopBase} or a {@link StateCompositeBase}.
   * @param stateId Any String for debugging. The string should never use for algorithm. It should be used maybe for display the state.
   * @param transHasConditionals true if the {@link #trans(Event)} routine has conditional transitions.
   *   Then the entry of this state returns {@link #mRunToComplete} to force invoking of {@link #trans(Event)} in the same cycle.
   */
  protected StateSimpleBase(EnclosingState superState, String stateId, boolean transHasConditionals) {
    this.enclState = superState;
    this.stateId = stateId;
    assert(enclState !=null && stateId !=null);
    //assert(stateId !=null);
    this.modeTrans = transHasConditionals ? mRunToComplete : 0;
  }
  
  
  /**The super constructor only for the top state. 
   * @param superState The enclosing state which contains this state. It is either a {@link StateTopBase} or a {@link StateCompositeBase}.
   * @param stateId Any String for debugging. The string should never use for algorithm. It should be used maybe for display the state.
   * @param transHasConditionals true if the {@link #trans(Event)} routine has conditional transitions.
   *   Then the entry of this state returns {@link #mRunToComplete} to force invoking of {@link #trans(Event)} in the same cycle.
   */
  protected StateSimpleBase(String stateId) {
    this.enclState = null;
    this.stateId = stateId;
    this.modeTrans = 0;
  }
  
  
  /**The super constructor for states which has only event-forced transitions.
   * @param superState The enclosing state which contains this state. It is either a {@link StateTopBase} or a {@link StateCompositeBase}.
   * @param stateId Any String for debugging. The string should never use for algorithm. It should be used maybe for display the state.
   */
  protected StateSimpleBase(EnclosingState superState, String stateId) {
    this.enclState = superState;
    this.stateId = stateId;
    assert(enclState !=null && stateId !=null);
    this.modeTrans = mRunToComplete;  //default: call trans(), it has not disadvantages
  }
  
  
  
  /**Check whether the enclosing state is in this state
   * @return true if it is.
   */
  /*package private*/ final boolean enclHasThisState(){ return enclState == null || enclState.isInState(this); }

  
  /**Check and returns true if the enclosing state has this state as active one.
   * @return
   */
  public final boolean isInState(){
    return enclState == null           //it is the top state.
         || enclState.isInState(this);  //the enclosing state has this as active one.
  }
  
  /**This method sets this state in the enclosing composite state. It calls {@link #entryAction()}. 
   * @param isConsumed Information about the usage of an event in a transition, given as input and returned as output.
   * @return The parameter isConsumed may be completed with the bit {@link #mRunToComplete} if this state's {@link #trans(Event)}-
   *   method has non-event but conditional state transitions. Setting of this bit {@link #mRunToComplete} causes
   *   the invocation of the {@link #trans(Event)} method in the control flow of the {@link StateCompositeBase#processEvent(Event)} method.
   *   This method sets {@link #mRunToComplete}.
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
  public final int entry(Event<?,?> ev) { //int isConsumed){
    enclState.setState(ev, this);
    ctEntry +=1;
    dateLastEntry = System.currentTimeMillis();
    durationLast = 0;
    if(this instanceof StateAdditionalParallelBase<?,?>){
      ((StateAdditionalParallelBase<?,?>)this).entryAdditionalParallelBase();
    }
    else if(this instanceof StateParallelBase<?,?>){
      ((StateParallelBase<?,?>)this).entryParallelBase(ev);
    }
    else if(this instanceof StateCompositeBase<?,?>){
      ((StateCompositeBase<?,?>)this).entryComposite();
    }
    entryAction(ev);
    if(ev !=null) return mStateEntered | mEventConsumed | modeTrans;
    else return mStateEntered | modeTrans;
    //return isConsumed | modeTrans;
  }
  
  
  /**This method should be overridden if the state needs any entry action. This default method is empty. */
  protected void entryAction(Event<?,?> ev){}
  
  
  
  /**Checks the trigger and conditions of a state transition. The user should override this method in form (example)
   * <pre>
  public int trans(Event ev){
    TypeOfEnclosingState enclState;
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
  
  
  /**Exit the state. This method must not be overridden by the user, only the {@link StateCompositeBase} overrides it.
   * Override {@link #exitAction()} for user specific exit behavior.
   * @return The enclosing state, which can used for entry immediately.
   */
  public EnclosingState exit(){ 
    durationLast = System.currentTimeMillis() - dateLastEntry;
    enclState.isActive = false;
    exitAction();
    return enclState; 
  }
  
  
  /**This method should be overridden if the state needs any exit action. This default method is empty. */
  protected void exitAction(){}
  
  public EnclosingState enclState(){ return enclState; }
  
  
  /**Gets the path to this state. The path is build from the {@link #stateId} of all enclosing states
   * separated with a dot and at least this stateId.
   * For example "topStateName.compositeState.thisState". 
   */
  public CharSequence getStatePath(){
    StringBuilder uPath = new StringBuilder(120);
    StateSimpleBase<?> state = this;
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
