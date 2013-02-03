package org.vishia.stateMachine;

import org.vishia.util.DateOrder;
import org.vishia.util.Event;

/**Base class for composite states.
 *
 * @author Hartmut Schorrig
 *
 * @param <DerivedState> The class type of the state which is derived from this class. 
 *   This parameter is used to designate the {@link #stateAct} and all parameters which works with {@link #stateAct}
 *   for example {@link #isInState(StateSimpleBase)}. It helps to prevent confusions with other state instances else
 *   the own one. A mistake is detected to compile time already.
 * @param <EnclosingState> The class type which is the derived type of the enclosing state where this state is member of.
 */
public abstract class StateCompositeBase
<DerivedState extends StateCompositeBase<DerivedState,?>, EnclosingState extends StateCompositeBase<EnclosingState,?>>
 extends StateSimpleBase<EnclosingState> 
{ 
  
  /**Version, history and license
   * <ul>
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
  public static final int version = 20120917;

  protected int maxStateSwitchesInLoop = 1000;
  
  /**Stores whether this composite state is active. Note that the #stateAct is set as history state
   * as well the state is not active. This bit is set to false too if the current state is exited
   * and a new state is not entered yet, while temporary transition processing. It helps to prevent double
   * execution of the {@link #exit()} routine if exit of the enclosing state is processed.*/
  boolean isActive;
  
  private StateSimpleBase<DerivedState> stateAct;
  
  protected StateCompositeBase(EnclosingState enclState, String stateId) {
    super(enclState, stateId);
    stateAct = null;
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
  
  
  /**This method is used to entry the default state if the actual state is null (first invocation).
   * The user should override:
   * <pre>
    @Override public int entryDefault(){
      return defaultState.entry(notConsumed);
    }
   * </pre>
   * @return 0
   */
  public abstract int entryDefault();
  
  
  /**This method should be called from outside if the history state should be entered and all history states
   * should be entered in sub states.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryDeepHistory(int isProcessed){
    StateSimpleBase<DerivedState> stateActHistory = stateAct;  //save it
    int cont = entry(isProcessed);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    if(stateActHistory instanceof StateCompositeBase<?,?>){
      cont = ((StateCompositeBase<?,?>)stateActHistory).entryDeepHistory(cont);
    } else {
      cont = stateActHistory.entry(cont);           //entry in the history sub state.
    }
    return cont;
  }
  
  
  /**This method should be called from outside if the history state should be entered but the default state of any
   * sub state should be entered.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryFlatHistory(int isProcessed){
    StateSimpleBase<DerivedState> stateActHistory = stateAct;  //save it
    int cont = entry(isProcessed);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    cont = stateActHistory.entry(cont);             //entry in the history sub state.
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
  @Override public int entry(isConsumed){
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
  @Override public int entry(int isProcessed){
    super.entry(isProcessed);
    stateAct = null;
    isActive = true;
    return isProcessed | mRunToComplete;
  }
  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking either its {@link StateCompositeBase#process(Event)}
   * or its {@link #trans(Event)} method.
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
   * @param evP The event supplied to the {@link #trans(Event)} method.
   * @return The bits {@link StateSimpleBase#mEventConsumed} as result of the inside called {@link #trans(Event)}.
   *   Note that if an event is consumed in an inner state, it should not be applied to its enclosing state transitions. 
   */
  public int process(final Event<?> evP){
    int cont;
    Event evTrans = evP;
    int catastrophicalCount =  maxStateSwitchesInLoop;
    do{

      StateSimpleBase<DerivedState> statePrev = stateAct;
      //
      //
      if(stateAct == null){
        entryDefault();
      } 
      if(stateAct instanceof StateCompositeBase<?,?>){
        cont = ((StateCompositeBase<?,?>)stateAct).process(evTrans); 
      } else {
        cont = stateAct.trans(evTrans);
      }
      //
      DateOrder date = new DateOrder();
      if(!isActive){
        System.out.println("." + date.order + " State leaved " + toString() + ";" + statePrev + "-->(" + evTrans + ")");
      } else if(statePrev != stateAct){
        System.out.println("." + date.order + " StateSwitch " + toString() + ";" + statePrev + "-->(" + evTrans + ")-->" + stateAct);
      } else if(evTrans !=null){
        System.out.println("." + date.order + " Event not used " + toString() + ";" + stateAct + ":(" + evTrans + ")");
      }
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
    cont = trans(evTrans);  //evTrans is null if it was consumed in inner transitions.
    return cont;  //runToComplete.bit may be set.
  }


  /**Sets the state of the composite state.
   * This method should be called
   * @param state Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  /*package private*/ void setState(StateSimpleBase<DerivedState> stateSimple) { //, EnumState stateNr) {
    if( !enclHasThisState()) {  
      entry(0);  //executes the entry action of this enclosing state.
    }
   this.stateAct = stateSimple;
   this.isActive = true;
  }

  /**Sets the state of the composite state.
   * This method should be called
   * @param state Only states of the own composite are advisable. It is checked in compile time
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
  @Override public EnclosingStateType exit(){
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

  

  
}
