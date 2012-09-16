package org.vishia.util;

import java.util.LinkedList;
import java.util.List;




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
  


  
  
  @Override public int process(Event ev){
    int cont = 0;
    for(StateCompositeBase<?, DerivedState> state: states){
      cont |= state.process(ev);
    }
    if((cont & StateSimpleBase.consumed) != 0){
      ev = null;
    }
    trans(ev);  //the own trans
    return cont;
  }
  
 
 
  /**Exits first the actual sub state (and tha exits its actual sub state), after them this state is exited.
   * @see org.vishia.util.StateSimpleBase#exit()
   */
  @Override public EnclosingState exit(){ 
    for(StateCompositeBase<?, DerivedState> state: states){
      state.exit();
    }
    return super.exit();
  }


  
  
}
