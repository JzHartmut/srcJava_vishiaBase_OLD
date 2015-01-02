package org.vishia.stateMachine;

import org.vishia.event.EventMsg;
import org.vishia.event.EventMsg2;
import org.vishia.event.EventConsumer;
import org.vishia.util.DateOrder;

/**Base class for composite states.
 *
 * @author Hartmut Schorrig
 *
 * @param <DerivedState> The class type of the state which is derived from this class. 
 *   This parameter is used to designate the {@link #stateAct} and all parameters which works with {@link #stateAct}
 *   for example {@link #isInState(StateSimpleBase)}. It helps to prevent confusions with other state instances else
 *   the own one. A mistake is detected to compile time already.
 * @param <EnclosingState> The class type which is the derived type of the enclosing state where this state is member of.
 *   See {@link StateTopBase}.
 */
public abstract class StateCompositeBase
<DerivedState extends StateCompositeBase<DerivedState,?>, EnclosingState extends StateCompositeBase<EnclosingState,?>>
 extends StateSimpleBase<EnclosingState>
 //implements EventConsumer
{ 
  
  /**Version, history and license
   * <ul>
   * <li>2013-05-11 Hartmut new: It is a {@link EventConsumer} yet. Especially a timer event needs a destination
   *   which is this class.
   * <li>2013-04-27 Hartmut adapt: The {@link #entry(EventMsg2)} and the {@link #entryAction(EventMsg2)} should get the event
   *   from the transition. See {@link StateSimpleBase}.
   * <li>2013-04-13 Hartmut re-engineering: 
   *   <ul>
   *   <li>New method {@link #setDefaultState(StateSimpleBase)}
   *   <li>{@link #entryDefaultState()} is package private now, it regards requirements of {@link StateParallelBase}.
   *   <li>The old override-able method entryDefault() was removed.
   *   <li>The overridden entry() was removed, replaced by #entryComposite, which is called in {@link StateSimpleBase#entry(int)}
   *     if the instance is this type.    
   *   </ul>
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2003 in C-language.
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

  protected int maxStateSwitchesInLoop = 1000;
  
  /**Stores whether this composite state is active. Note that the #stateAct is set as history state
   * as well the state is not active. This bit is set to false too if the current state is exited
   * and a new state is not entered yet, while temporary transition processing. It helps to prevent double
   * execution of the {@link #exit()} routine if exit of the enclosing state is processed.*/
  boolean isActive;
  
  /**If set true, then any state transition is logged with System.out.printf("..."). One can use the 
   * {@link org.vishia.msgDispatch.MsgRedirectConsole} to use a proper log system. 
   */
  public static boolean debugState = true;
  
  private StateSimpleBase<DerivedState> stateAct;
  
  private StateSimpleBase<DerivedState> stateDefault;
  
  protected StateCompositeBase(EnclosingState enclState, String stateId, boolean transHasConditionals) {
    super(enclState, stateId, transHasConditionals);
    stateAct = null;
  }
  
  
  protected StateCompositeBase(EnclosingState enclState, String stateId) {
    super(enclState, stateId, false);
    stateAct = null;
  }
  
  
  protected StateCompositeBase(String stateId) {
    super(stateId);
    stateAct = null;
  }
  
  
  /**Sets the default state of this composite. 
   * This routine has to be called in the constructor of the derived state after calling the super constructor.
   * @param stateDefault The state which will be set if the composite class was selected 
   *   but an inner state was not set. The {@link #entry(EventMsg2)} to the default state is ivoked
   *   if this state will be {@link #processEvent(EventMsg2)}.
   */
  protected void setDefaultState(StateSimpleBase<DerivedState> stateDefault ){
    this.stateDefault = stateDefault;
  }
  
  
  /**Check whether this composite state has the given state as direct actual sub state
   * @param state Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  public final boolean isInState(StateSimpleBase<? extends DerivedState> state){ 
    return isInState()             //this state is active too, or it is the top state.
            && stateAct == state;   //the given state is the active.
  }
  
  
  /**This method is used to entry the default state if the actual state is null (first invocation).  */
  /*package private*/ final void entryDefaultState(){ 
    if(this instanceof StateParallelBase<?,?>){
      ((StateParallelBase<?,?>)this).entryDefaultParallelStates();
    } else {
      stateDefault.entry(null);
    }
  }

  
  
  /**This method should be called from outside if the history state should be entered and all history states
   * should be entered in sub states.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryDeepHistory(EventMsg<?> ev){
    StateSimpleBase<DerivedState> stateActHistory = stateAct;  //save it
    int cont = entry(ev);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    if(stateActHistory instanceof StateCompositeBase<?,?>){
      cont = ((StateCompositeBase<?,?>)stateActHistory).entryDeepHistory(ev);
    } else {
      cont = stateActHistory.entry(ev);           //entry in the history sub state.
    }
    return cont;
  }
  
  
  /**This method should be called from outside if the history state should be entered but the default state of any
   * sub state should be entered.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryFlatHistory(EventMsg<?> ev){
    StateSimpleBase<DerivedState> stateActHistory = stateAct;  //save it
    int cont = entry(ev);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    cont = stateActHistory.entry(ev);             //entry in the history sub state.
    return cont;
  }
  
  
  /**This method sets this state in the enclosing composite state and sets the own {@link #stateAct} to null 
   * to force entry of the default state if {@link #process(Event)} is running firstly after them.
   * If this method will be called recursively in an {@link #entry(int)} of an inner state,
   * that entry sets the {@link #stateAct} in {@link #setState(StateSimpleBase)} after them so it is not null.
   * <br><br> 
   * This method should be overridden if a entry action is necessary in any state. 
   * The overridden form should call this method in form super.entry(isConsumed):
   * <pre>
  public int entry(isConsumed){
    super.entry(0);
    //statements of entry action.
    return isConsumed | runToComplete;  //if the trans action should be entered immediately after the entry.
    return isConsumed | complete;       //if the trans action should not be tested.
  }
   * </pre>  
   * 
   * @param isConsumed Information about the usage of an event in a transition, given as input and returned as output.
   * @return The parameter isConsumed may be completed with the bit {@link #mRunToComplete} if this state's {@link #trans(Event)}-
   *   method has non-event but conditional state transitions. Setting of this bit {@link #mRunToComplete} causes
   *   the invocation of the {@link #trans(Event)} method in the control flow of the {@link StateCompositeBase#process(Event)} method.
   *   This method sets {@link #mRunToComplete}.
   */
  //@Override 
  /**package private*/ final void entryComposite(){
    //super.entry(isConsumed);
    stateAct = null;
    isActive = true;
    //return isConsumed | mRunToComplete;
  }
  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking either its {@link StateCompositeBase#processEvent(EventMsg2)}
   * or its {@link #trans(EventMsg2)} method.
   * If this method returns {@link StateSimpleBase#mRunToComplete} that invocation is repeated in a loop, to call
   * the transition of the new state too. But if the event was consumed by the last invocation, it is not supplied again
   * in the loop, the event parameter is set to null instead. It means only conditional transitions are possible.
   * This behavior is conform with the UML definition.
   * If the loop would not terminate because any state have a valid transtion and the state machine switches forever,
   * the loop is terminated with an exception for a number of {@link #maxStateSwitchesInLoop}. This exception occurs
   * if the user stateMachine conditions are faulty only.
   * <br><br>
   * This method is not attempt to override by the user. Only the class {@link StateParallelBase} overrides it
   * to invoke the processing of all parallel boughs.
   * @param evP The event supplied to the {@link #trans(EventMsg2)} method.
   * @return The bits {@link StateSimpleBase#mEventConsumed} as result of the inside called {@link #trans(EventMsg2)}.
   *   Note that if an event is consumed in an inner state, it should not be applied to its enclosing state transitions. 
   */
  public int processEvent(final EventMsg<?> evP){  //NOTE: should be protected.
    int cont;
    EventMsg<?> evTrans = evP;
    int catastrophicalCount =  maxStateSwitchesInLoop;
    do{

      //
      //
      if(stateAct == null){
        entryDefaultState();  //regards also Parallel states.
      } 
      if(stateAct instanceof StateCompositeBase<?,?>){
        //recursively call for the composite inner state
        cont = ((StateCompositeBase<?,?>)stateAct).processEvent(evTrans); 
      } else {
        StateSimpleBase<DerivedState> statePrev = stateAct;
        cont = stateAct.trans(evTrans);
        if(debugState && (cont & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, cont); }
      }
      //
      if((cont & StateSimpleBase.mEventConsumed) != 0){
        evTrans = null;
      }
      if(catastrophicalCount == 4)
        catastrophicalCount = 4;  //set break point! to debug the loop
      
    } while(isActive   //leave the loop if this composite state is exited.
        && (cont & mRunToComplete) !=0    //loop if runToComplete-bit is set, the new state should be checked.
        && --catastrophicalCount >=0
        );
    if(catastrophicalCount <0) {
      throw new RuntimeException("unterminated loop in state switches");
    }
    if(  evTrans != null   //evTrans is null if it was consumed in inner transitions. 
      || (modeTrans & StateSimpleBase.mRunToComplete) !=0  //state has only conditional transitions
      ){
      //process the own transition. Do it after processing the inner state (omg.org)
      //and only if either an event is present or the state has only conditional transitions.
      StateSimpleBase<DerivedState> statePrev = stateAct;
      cont = trans(evTrans); 
      if(debugState && (cont & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, cont); }
    }
    return cont;  //runToComplete.bit may be set from an inner state transition too.
  }

  
  
  private void printStateSwitchInfo(StateSimpleBase<DerivedState> statePrev, EventMsg<?> evTrans, int cont) {
    DateOrder date = new DateOrder();
    Thread currThread = Thread.currentThread();
    String sThread = currThread.getName();
    String sActiveState = getActiveState();
    if(!isActive){
      System.out.println("StateCompositeBase - State leaved " + sThread + ";." + date.order + "; state " + statePrev + " ==> " + sActiveState + "; ev=" + evTrans + ";");
    } else if((cont & StateSimpleBase.mEventConsumed)!=0) {  //statePrev != stateAct){  //from the same in the same state!
      System.out.println("StateCompositeBase - state switch " + sThread + ";." + date.order + "; state " + statePrev + " ==> "  + sActiveState + "; ev=" + evTrans + ";");
    } else if(evTrans !=null){ 
      System.out.println("StateCompositeBase - state switch " + sThread + " - Ev not used ;." + date.order + "; state " + statePrev + " ==> "  + sActiveState + "; ev=" + evTrans + ";");
    }
    
  }

  
  

  /**Sets the state of the composite state.
   * This method should be called
   * @param stateSimple Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  /*package private*/ void setState(EventMsg<?> ev, StateSimpleBase<DerivedState> stateSimple) { //, EnumState stateNr) {
    if( !enclHasThisState()) {  
      entry(ev);          //executes the entry action of this enclosing state to notify the state by its enclosingState.
    }
   this.stateAct = stateSimple;
   this.isActive = true;
  }

  /**Sets the state of the composite state.
   * This method should be called
   * @param stateSimple Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  /*package private*/ final void setStateParallel(StateSimpleBase<DerivedState> stateSimple) { //, EnumState stateNr) {
   this.stateAct = stateSimple;
   this.isActive = true;
  }

  /**Exits first the actual sub state (and that exits its actual sub state), after them this state is exited.
  /**This method may be overridden with exit actions:
   * <pre>
  public EnclosingStateType exit(){
    TypeOfEnclosingState enclState = super.exit(); //call firstly! It exits sub states.
    statementsOfExit();
    return enclSate;
  }
   * </pre> 
   * @return The enclosing state, which can used for entry immediately.
   * @see org.vishia.stateMachine.StateSimpleBase#exit()
   */
  @Override public EnclosingState exit(){ 
    if(isActive){
      stateAct.exit();
      isActive = false; //NOTE that StateSimpleBase.exit() sets isActive to false already. It is done twice.
    }
    return super.exit();
  }

  
  /**Returns the name of the active state of this composite state or the active state of any other state in the chart.
   * @return
   */
  protected String getActiveState(){
    if(isActive) return getStatePath().toString();
    else if(enclState !=null){ return enclState.getActiveState(); }
    else return "--inactive--";
  }
  
  
  @Override   public CharSequence getStatePath(){
    StringBuilder uPath = new StringBuilder(120);
    StateSimpleBase<?> state = this;
    while((state = state.enclState) !=null){
      uPath.insert(0,'.').insert(0, state.stateId);
    }
    state = this;
    do{
      uPath.append('.').append(state.stateId);
      if(state instanceof StateCompositeBase<?,?>){
        state = ((StateCompositeBase<?,?>)state).stateAct;
      } else { state = null; }
    } while(state !=null);
    return uPath;
  }
  
  
  @Override public String toString(){ return stateId.toString() + ":" + (!isActive ? "-inactive-": stateAct.stateId); }

  
}
