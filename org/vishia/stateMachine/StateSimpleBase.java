package org.vishia.stateMachine;

import org.vishia.util.Event;

/**Base class of a State in a State machine.
 * The user should override at least the {@link #trans(Event)} method. This method is the transition to another state.
 * The user can override {@link #entry(int)} and {@link #exit()} if the state has actions on entry and exit. 
 * But one should call super.entry(); and super.exit(); as first statement!
 * 
 * A State is a small set of data to refer its enclosing state and a set of methods. 
 * @author Hartmut Schorrig
 *
 * @param <EnclosingState> The derived type of the enclosing state which contains this State.
 */
public abstract class StateSimpleBase<EnclosingState extends StateCompositeBase<EnclosingState,?>>{
    
  /**Version, history and license
   * <ul>
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
  public static final int version = 20120917;

  /**Bit in return value of a Statemachine's {@link #trans(Event)} or entry method for designation, 
   * that the given Event object was used to switch.
   */
  public final static int mEventConsumed =0x1;
  
  /**Specification of the consumed Bit in return value of a Statemachine's {@link #trans(Event)} or {@link #entry(int)} 
   * method for designation, that the given Event object was not used to switch. The value of this is 0.
   */
  public final static int eventNotConsumed =0x0;
  
  /**Bit in return value of a Statemachine's entry method for designation, 
   * that the given State has non-event-driven transitions, therefore the trans method should be called
   * in the same cycle.
   */
  public final static int mRunToComplete =0x2;
  
  /**Bit in return value of a Statemachine's entry method for designation, that either 
   * the given State has only event-driven transitions, therefore the trans method should not be called in the same cycle
   * or there is no state switch. The value is 0.
   */
  public final static int stateCompleted =0x0;
  
  
  /**If a composite state is leaved, any other parallel composite states don't processed then. */
  public final static int mStateLeaved = 0x10;
  
  
  /**Reference to the enclosing state. With this, the structure of state nesting can be observed in data structures.
   * The State knows its enclosing state to set and check the state identification there. 
   */
  private final EnclosingState enclState;
  
  /**The own identification of the state. It is given by constructor. */
  //final EnumState stateId;
  
  final String stateId;
  
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
   */
  protected StateSimpleBase(EnclosingState superState, String stateId) {
    this.enclState = superState;
    this.stateId = stateId;
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
  
  /**This method sets this state in the enclosing composite state. It should be overridden 
   * if a entry action is necessary in any state. The overridden form should call this method in form super.entry(isConsumed):
   * <pre>
  @Override public int entry(isConsumed){
    super.entry(0);
    //statements for entry action.
    return isConsumed | runToComplete;  //if the trans action should be entered immediately after the entry.
    return isConsumed | complete;       //if the trans action should not be tested.
   * </pre>  
   * 
   * @param isConsumed Information about the usage of an event in a transition, given as input and returned as output.
   * @return The parameter isConsumed may be completed with the bit {@link #mRunToComplete} if this state's {@link #trans(Event)}-
   *   method has non-event but conditional state transitions. Setting of this bit {@link #mRunToComplete} causes
   *   the invocation of the {@link #trans(Event)} method in the control flow of the {@link StateCompositeBase#process(Event)} method.
   *   This method sets {@link #mRunToComplete}.
   */
  public int entry(int isConsumed){
    enclState.setState(this);
    ctEntry +=1;
    dateLastEntry = System.currentTimeMillis();
    durationLast = 0;
    return isConsumed | mRunToComplete;
  }
  
  /**Processes the state. It invokes the {@link #trans(Event)}-method. This method is overridden in the class
   * {@link StateCompositeBase}.The user should not override this method! 
   * @param ev Any event
   * @return see {@link #trans(Event)}.
   */
  public int xxxprocess(Event<?> ev){ return trans(ev); }

  
  /**Checks the trigger and conditions of a state transition. The user should override this method in form (example)
   * <pre>
  @Override public int trans(Event ev){
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
   *   but it is used in parallel states. See {@link StateCompositeBase#process(Event)} and {@link StateParallelBase#process(Event)}.
   */
  public abstract int trans(Event<?> ev);
  
  /**Exit the state; this method may be overridden with exit actions:
   * <pre>
  @Override public EnclosingStateType exit(){
    TypeOfEnclosingState enclState = super.exit();  //call firstly!
    statementsOfExit();
    return enclSate;
  }
   * </pre> 
   * @return The enclosing state, which can used for entry immediately.
   */
  public EnclosingState exit(){ 
    durationLast = System.currentTimeMillis() - dateLastEntry;
    enclState.isActive = false;
    return enclState; 
  }
  
  public EnclosingState enclState(){ return enclState; }
  
  /**Returns the state Id and maybe some more debug information.
   * @see java.lang.Object#toString()
   */
  @Override public final String toString(){ return stateId.toString(); }





}
