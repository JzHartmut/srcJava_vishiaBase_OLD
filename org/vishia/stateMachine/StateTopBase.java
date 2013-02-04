package org.vishia.stateMachine;

import org.vishia.util.Event;

/**Base class for a state machine, for the main state.
 * @author Hartmut Schorrig
 *
 * @param <DerivedState> The class type of the state which is derived from this class. 
 *   This parameter is used to designate the {@link #stateAct} and all parameters which works with {@link #stateAct}
 *   for example {@link #isInState(StateSimpleBase)}. It helps to prevent confusions with other state instances else
 *   the own one. A mistake is detected to compile time already.
 */
public abstract class StateTopBase <DerivedState extends StateCompositeBase<DerivedState,?>> 
//extends StateCompositeBase<DerivedState,DerivedState>
extends StateCompositeBase<DerivedState,StateCompositeNull>
{

  /**Version, history and license.
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


  protected StateTopBase(String stateId) {
    super(null, stateId);
  }


  @Override final public int trans(Event ev){ return 0; }


  
}

/**Package private helper class is dummy-enclosing state for the top state. 
 * It is used formally as second parameter of {@link StateTopBase}.
 */
class StateCompositeNull extends StateCompositeBase<StateCompositeNull, StateCompositeNull>{

  protected StateCompositeNull(StateCompositeNull enclState, String stateId) {
    super(enclState, stateId);
  }

  @Override public int entryDefault() {
    return 0;
  }

  @Override public int trans(Event<?> ev) {
    return 0;
  }
}