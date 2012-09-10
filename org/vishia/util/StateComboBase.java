package org.vishia.util;

/**
 * @author Hartmut
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
  
  private EnumState stateNr;
  
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
    stateNr = stateNull; //Enum.valueOf(EnumState.class, "Null");
  }
  
  public EnumState stateNr(){ return stateNr; }
  
  @Override final public int process(Event ev){
    int cont = 0;
    Event evTrans = ev;
    cont = trans(ev);
    if(superState == null || superState.stateNr == stateId) { //(bChg & StateBase.consumed) ==0){ 
      do{
        cont &= ~runToComplete;
        DateOrder date = new DateOrder();
        System.out.println("." + date.order + " EventFollow;" + stateNr() + ";" + evTrans);
        cont |= switchState(ev);
        if((cont & StateBase.consumed) != 0){
          evTrans = null;
        }
      } while((cont & runToComplete) !=0);
    }
    return cont;
  }

  protected abstract int switchState(Event ev);

  public void setStateNr(EnumState stateNr) {
    this.stateNr = stateNr;
  }

}
