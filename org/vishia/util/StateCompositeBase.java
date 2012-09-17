package org.vishia.util;

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
  public final boolean isInState(StateSimpleBase<? extends DerivedState> state){ return stateAct == state; }
  
  
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
   * @return The parameter isConsumed may be completed with the bit {@link #runToComplete} if this state's {@link #trans(Event)}-
   *   method has non-event but conditional state transitions. Setting of this bit {@link #runToComplete} causes
   *   the invocation of the {@link #trans(Event)} method in the control flow of the {@link StateCompositeBase#process(Event)} method.
   *   This method sets {@link #runToComplete}.
   */
  @Override public int entry(int isProcessed){
    super.entry(isProcessed);
    stateAct = null;
    return isProcessed | runToComplete;
  }
  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking the {@link #switchState(Event)} method.
   * If this method returns {@link StateSimpleBase#runToComplete} the {@link #switchState(Event)} method is called again
   * in a loop. The loop is terminated with an exception only if it is not terminated.
   * @param evP The event
   * @return The bits {@link StateSimpleBase#consumed} or {@link StateSimpleBase#runToComplete}
   *   as result of the inside called {@link #trans(Event)} method and the called {@link #entry(int)} method. 
   */
  @Override public int process(final Event evP){
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
      cont = stateAct.process(evTrans); //switchState(evTrans);
      //
      DateOrder date = new DateOrder();
      if(statePrev != stateAct){
        System.out.println("." + date.order + " StateSwitch " + toString() + ";" + statePrev + "-->(" + evTrans + ")-->" + stateAct);
      } else if(evTrans !=null){
        System.out.println("." + date.order + " Event not used " + toString() + ";" + stateAct + ":(" + evTrans + ")");
      }
      if((cont & StateSimpleBase.consumed) != 0){
        evTrans = null;
      }
      
    } while((cont & runToComplete) !=0 && --catastrophicalCount >=0);
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
   * @see org.vishia.util.StateSimpleBase#exit()
   */
  @Override public EnclosingState exit(){ 
    if(stateAct !=null){
      stateAct.exit();
    }
    return super.exit();
  }

  

  
}
