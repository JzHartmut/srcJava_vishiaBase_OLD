package org.vishia.stateMachine;

import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventThread;
import org.vishia.util.EventTimerMng;

/**Base class for a state machine, for the main or top state.
 * An application should create its top state class using the following pattern:
 * <pre>
  class UsersTopState extends StateTopBase<UsersTopState>{

    /**inner state...* /
    UsersCompositeState stateExample = new UsersCompositeState(this);
    
    
    protected UsersTopState() {
      super("UsersTopState");
      setDefaultState(stateExample);
    }

  }
 * </pre>
 * @param <DerivedState> The class type of the state which is derived from this class. 
 *   This parameter is used to designate the {@link #stateAct} and all parameters which works with {@link #stateAct}
 *   for example {@link #isInState(StateSimpleBase)}. It helps to prevent confusions with other state instances else
 *   the own one. A mistake is detected to compile time already.
 * @author Hartmut Schorrig
 *
 */
public abstract class StateTopBase <DerivedState extends StateCompositeBase<DerivedState,?>> 
//extends StateCompositeBase<DerivedState,DerivedState>
extends StateCompositeBase<DerivedState,StateCompositeNull>
implements EventConsumer
{

  /**Version, history and license.
   * <ul>
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
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

  
  /**Aggregation to the used event queue or the thread for this statemachine.
   * It can be used to send events to from outer.
   * Note: Do not send events from any other thread to this directly.
   * The state machine should processed in only one thread.
   * But this aggregation may be null if the state machine will be processed in a users thread.
   */
  public final EventThread theThread;
  
  /**Aggregation to the used timer for time events. See {@link #addTimeOrder(long)}.
   * It may be null if queued time events are not necessary for this.
   */
  public final EventTimerMng theTimer;
  
  

  /**Super constructor of a top level state.
   * @param stateId The name of the state, only used for log and debug.
   * @param thread maybe null, the used event queue or the thread for this statemachine.
   *   see {@link #theThread}
   * @param timer maybe null, the timer which is used for {@link #addTimeOrder(long)}.
   *   If this statemachine does not use timer events, it may be null.
   */
  protected StateTopBase(String stateId, EventThread thread, EventTimerMng timer) {
    super(stateId);
    this.theThread = thread;
    this.theTimer = timer;
  }


  /**The trans routine is implemented as an empty routine here. The StateTop has not a capability
   * to switch any states.
   * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.util.Event)
   */
  @Override final public int trans(Event ev){ return 0; }

  /**This method is defined in {@link StateCompositeBase#processEvent(org.vishia.util.Event)}.
   * It means that a top State is an {@link EventConsumer}. 
   * It calls the routine {@link StateCompositeBase#processEvent(Event)}} of its superclass
   * which has the same name and signature, but that routine does not implement the {@link EventConsumer}
   * Only a top state should accept events from outside.
   */
  @Override public int processEvent(final Event<?,?> evP){
    return super.processEvent(evP);
  }

  
  public EventTimerMng.TimeOrder addTimeOrder(long date){
    return theTimer.addTimeOrder(date, this, theThread);
  }
  
  
}

/**Package private helper class is dummy-enclosing state for the top state. 
 * It is used formally as second parameter of {@link StateTopBase}.
 */
class StateCompositeNull extends StateCompositeBase<StateCompositeNull, StateCompositeNull>{

  protected StateCompositeNull(StateCompositeNull enclState, String stateId) {
    super(enclState, stateId);
  }

  @Override public int trans(Event<?,?> ev) {
    return 0;
  }
}
