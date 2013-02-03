package org.vishia.stateMachine;

import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Event;




/**Base class for parallel states.
 * @author Hartmut Schorrig
 *
 * @param <DerivedState> The class type of the state which is derived from this class. 
 *   This parameter is used to designate the {@link #stateAct} and all parameters which works with {@link #stateAct}
 *   for example {@link #isInState(StateSimpleBase)}. It helps to prevent confusions with other state instances else
 *   the own one. A mistake is detected to compile time already.
 * @param <EnclosingState> The class type which is the derived type of the enclosing state where this state is member of.
 */
public abstract class StateParallelBase 
< DerivedState extends StateParallelBase<DerivedState, EnclosingState>
, EnclosingState extends StateCompositeBase<EnclosingState,?>
>
extends StateCompositeBase<DerivedState, EnclosingState>
{

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

  List<StateCompositeBase<?, DerivedState>> states = new LinkedList<StateCompositeBase<?, DerivedState>>();
  
  protected StateParallelBase(EnclosingState enclState, String stateId) {
    super(enclState, stateId);
  }

  
  final public void addState(StateCompositeBase<?, DerivedState> state){
    states.add(state);
  }
  
  
  @Override public int entryDefault() {
    for(StateCompositeBase<?, DerivedState> state: states){
      state.entryDefault();
    }
    return 0;
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
    for(StateCompositeBase<?, DerivedState> state: states){
      state.setState(null);
    }
    return isProcessed | mRunToComplete;
  }


  
  
  @Override public int process(Event ev){
    int cont = 0;
    for(StateCompositeBase<?, DerivedState> state: states){
      cont |= state.process(ev);
    }
    if((cont & StateSimpleBase.mEventConsumed) != 0){
      ev = null;
    }
    trans(ev);  //the own trans
    return cont;
  }
  
 
 
  /**Exits first the actual sub state (and tha exits its actual sub state), after them this state is exited.
   * @see org.vishia.stateMachine.StateSimpleBase#exit()
   */
  @Override public EnclosingState exit(){ 
    for(StateCompositeBase<?, DerivedState> state: states){
      state.exit();
    }
    return super.exit();
  }


  
  
}
