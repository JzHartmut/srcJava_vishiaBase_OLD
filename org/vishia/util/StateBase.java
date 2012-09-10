package org.vishia.util;


/**A State is a small set of data to refer its enclosing state and a set of methods. 
 * @author Hartmut Schorrig
 *
 * @param <SuperState> The enclosing state which contains this State.
 * @param <EnumState> The enumeration of state identification in the enclosing state
 */
public abstract class StateBase<SuperState extends StateComboBase<?,?,EnumState, Environment>, EnumState extends Enum<EnumState>, Environment> {
  
  /**Version, history and license
   * <ul>
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
  public static final int version = 20120830;

  /**Bit in return value of a Statemachine's trans or entry method for designation, 
   * that the given Event object was used to switch.
   */
  public final static int consumed =0x1;
  
  /**Specification of the consumed Bit in returnvalue of a Statemachine's trans or entry method for designation, 
   * that the given Event object was not used to switch. The value of this is 0.
   */
  public final static int notConsumed =0x0;
  
  /**Bit in returnvalue of a Statemachine's entry method for designation, 
   * that the given State has non-event-driven transitions, therefore the trans method should be called
   * in the same cycle.
   */
  public final static int runToComplete =0x2;
  
  /**Bit in returnvalue of a Statemachine's entry method for designation, that either 
   * the given State has only event-driven transitions, therefore the trans method should not be called in the same cycle
   * or there is no state switch. The value is 0.
   */
  public final static int complete =0x0;
  
  
  protected Environment env;
  
  /**Reference to the enclosing state. With this, the structure of state nesting can be observed in data structures.
   * The State knows its enclosing state to set and check the state identification there. 
   */
  protected final SuperState superState;
  
  /**The own identification of the state. It is given by constructor. */
  final EnumState stateId;
  
  protected StateBase(SuperState superState, EnumState stateId) {
    this(superState, stateId, superState.env);
  }
  
  protected StateBase(SuperState superState, EnumState stateId, Environment env) {
    this.superState = superState;
    this.env = env;
    this.stateId = stateId;
  }
  
  /**This method sets the correct state ident in the enclosing combination state.
   * @param consumed
   * @return
   */
  public int entry(int consumed){
    if(   superState.superState !=null //a superstate of its combo state is given
       && superState.superState.stateNr() != superState.stateId){ //and the superstate of combo has not the correct number
      superState.entry(consumed);  //executes the entry of the enclosing state.
    }
    superState.setStateNr(stateId);
    return consumed;
  }

  public int process(Event ev){ return trans(ev); }
  
  public abstract int trans(Event ev);
  
  public void exit(){}
  
  
}
