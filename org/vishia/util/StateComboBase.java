package org.vishia.util;

/**Base class for composite states.
 * @author Hartmut Schorrig
 *
 * @param <SuperState> The combine state which encloses this state.
 * @param <EnumSuperState> The state ident type of the enclosing state.
 * @param <EnumState> The state ident type of this combine state.
 */
public abstract class StateComboBase
< SuperState extends StateComboBase<?,?,EnumSuperState, Environment>
, EnumSuperState extends Enum<EnumSuperState>
, EnumState extends Enum<EnumState>
, Environment
> extends StateBase<SuperState, EnumSuperState, Environment> { 
  
  protected int maxStateSwitchesInLoop = 1000;
  
  
  private EnumState stateNr;
  
  private final EnumState stateNull;
  
  protected StateComboBase(SuperState superState, EnumSuperState stateId, EnumState stateNull) {
    this(superState, stateId, stateNull, superState.env);
  }
  /**
   * @param superState The enclosing state. 
   * @param stateId The id of this state in the enclosing state.
   * @param stateNull The id of the null-state of type EnumState to start
   */
  protected StateComboBase(SuperState superState, EnumSuperState stateId, EnumState stateNull, Environment env) {
    super(superState, stateId, env);
    this.stateNull = stateNull;
    stateNr = stateNull; //Enum.valueOf(EnumState.class, "Null");
  }
  
  public EnumState stateNr(){ return stateNr; }
  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking the {@link #switchState(Event)} method.
   * If this method returns {@link StateBase#runToComplete} the {@link #switchState(Event)} method is called again
   * in a loop. The loop is terminated with an exception only if it is not terminated.
   * @param evP The event
   * @return The bits {@link StateBase#consumed} or {@link StateBase#runToComplete}
   *   as result of the inside called {@link #trans(Event)} method and the called {@link #entry(int)} method. 
   */
  final public int process(final Event evP){
    int cont;
    Event evTrans = evP;
    int catastrophicalCount =  maxStateSwitchesInLoop;
    do{
      DateOrder date = new DateOrder();
      System.out.println("." + date.order + " EventFollow;" + stateNr() + ";" + evTrans);
      //
      //
      cont = switchState(evTrans);
      //
      if((cont & StateBase.consumed) != 0){
        evTrans = null;
      }
      if((cont & StateBase.stateError) != 0){
        int faultState = stateNr.ordinal();
        stateNr = stateNull;                  //prevent hang forever.
        throw new IllegalArgumentException("fault state" + faultState);
      }
    } while((cont & runToComplete) !=0 && --catastrophicalCount >=0);
    if(catastrophicalCount <0) {
      throw new RuntimeException("unterminated loop in state switches");
    }
    cont = trans(evTrans);  //evTrans is null if it was consumed in inner transitions.
    return cont;  //runToComplete.bit may be set.
  }

  /**Switches to the current state and calls the {@link #process(Event)} or {@link #trans(Event)} method.
   * This method should be implemented by the user with the pattern:
   * <pre>
      @Override public int switchState(Event ev) {
        int cont;
        switch(stateNr()){
          case Null:        cont = stateA.entry(StateBase.notConsumed + StateBase.runToComplete); break;
          case StateA:      cont = stateA.trans(ev); break;
          case StateB:      cont = stateB.process(ev); break;
          default:          cont = stateError;
        }
        return cont;
      }
   * </pre>
   * @param ev The event
   * @return The bits {@link StateBase#consumed} or {@link StateBase#runToComplete}
   *   as result of the inside called {@link #trans(Event)} method and the called {@link #entry(int)} method. 
   *   Or the bit 
   */
  protected abstract int switchState(Event ev);

  public void setStateNr(EnumState stateNr) {
    this.stateNr = stateNr;
  }

}
