package org.vishia.util;

/**A State is a small set of data to refer its enclosing state and a set of methods. 
 * @author Hartmut
 *
 * @param <EnclosingState> The derived type of the enclosing state which contains this State.
 */
public abstract class StateSimpleBase<EnclosingState extends StateCompositeBase<EnclosingState,?>>{
    
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

  /**Bit in return value of a Statemachine's {@link #trans(Event)} or entry method for designation, 
   * that the given Event object was used to switch.
   */
  public final static int consumed =0x1;
  
  /**Specification of the consumed Bit in return value of a Statemachine's {@link #trans(Event)} or {@link #entry(int)} 
   * method for designation, that the given Event object was not used to switch. The value of this is 0.
   */
  public final static int notConsumed =0x0;
  
  /**Bit in return value of a Statemachine's entry method for designation, 
   * that the given State has non-event-driven transitions, therefore the trans method should be called
   * in the same cycle.
   */
  public final static int runToComplete =0x2;
  
  /**Bit in return value of a Statemachine's entry method for designation, that either 
   * the given State has only event-driven transitions, therefore the trans method should not be called in the same cycle
   * or there is no state switch. The value is 0.
   */
  public final static int complete =0x0;
  
  
  /**Reference to the enclosing state. With this, the structure of state nesting can be observed in data structures.
   * The State knows its enclosing state to set and check the state identification there. 
   */
  private final EnclosingState enclState;
  
  /**The own identification of the state. It is given by constructor. */
  //final EnumState stateId;
  
  final String stateId;
  
  /**Reference to the data of the class where the statemachine is member off. */
  //protected Environment env;
  
  int ctEntry;
  long dateLastEntry;
  long durationLast;

  
  protected StateSimpleBase(EnclosingState superState, String stateId) {
    this.enclState = superState;
    //this.env = env;
    this.stateId = stateId;
    //this.stateId = stateId;
  }
  
  
  
  /*package private*/ final boolean enclHasThisState(){ return enclState == null || enclState.isInState(this); }

  
  /**This method sets the correct state ident in the enclosing combination state. It should be overridden 
   * if a entry action is necessary in any state. The overridden form should call this method in form super.entry(isConsumed).
   * @param isConsumed Information about the usage of an event in a transition, given as input and expected as output.
   * @return The parameter isConsumed may be completed with the bit {@link #runToComplete} if this state's {@link #trans(Event)}-
   *   method has non-event but conditional state transitions. Setting of this bit {@link #runToComplete} causes
   *   the invocation of the {@link #trans(Event)} method in the control flow of the {@link StateCompositeBase#process(Event)} method.
   */
  public int entry(int isConsumed){
    if( !enclState.enclHasThisState()) {  
        //&& enclState.enclState.stateNr() != enclState.stateId){ //and the superstate of combo has not the correct number
      enclState.entry(isConsumed);  //executes the entry of the enclosing state.
    }
    enclState.setState(this);
    ctEntry +=1;
    dateLastEntry = System.currentTimeMillis();
    durationLast = 0;
    return isConsumed | runToComplete;
  }
  
  public int process(Event ev){ return trans(ev); }

  public abstract int trans(Event ev);
  
  /**Exit the state; this method may be overridden with exit actions, and invoking super.exit(); after them.
   * @return The enclosing state, which can used for entry immediately.
   */
  public EnclosingState exit(){ 
    durationLast = System.currentTimeMillis() - dateLastEntry;
    return enclState; 
  }
  
  @Override public final String toString(){ return stateId.toString(); }





}
