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
  
  
  public final boolean isInState(StateSimpleBase<? extends DerivedState> state){ return stateAct == state; }
  
  
  public abstract int entryDefault();
  
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

  /**Switches to the current state and calls the {@link #process(Event)} or {@link #trans(Event)} method.
   * This method should be implemented by the user with the pattern:
   * <pre>
      @Override public int switchState(Event ev) {
        int cont;
        switch(stateNr()){
          case Null:        cont = stateA.entry(StateSimpleBase.notConsumed + StateSimpleBase.runToComplete); break;
          case StateA:      cont = stateA.trans(ev); break;
          case StateB:      cont = stateB.process(ev); break;
          default:          cont = stateError;
        }
        return cont;
      }
   * </pre>
   * @param ev The event
   * @return The bits {@link StateSimpleBase#consumed} or {@link StateSimpleBase#runToComplete}
   *   as result of the inside called {@link #trans(Event)} method and the called {@link #entry(int)} method. 
   *   Or the bit 
   */
  //protected abstract int switchState(Event ev);

  public void setState(StateSimpleBase<DerivedState> stateSimple) { //, EnumState stateNr) {
    //this.stateNr = stateNr;
    this.stateAct = stateSimple;
  }

  /**Exits first the actual sub state (and tha exits its actual sub state), after them this state is exited.
   * @see org.vishia.util.StateSimpleBase#exit()
   */
  @Override public EnclosingState exit(){ 
    if(stateAct !=null){
      stateAct.exit();
    }
    return super.exit();
  }

  

  
}
