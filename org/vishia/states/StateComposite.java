package org.vishia.states;

import java.lang.reflect.Constructor;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;


import org.vishia.util.DataAccess;

public class StateComposite extends StateSimple
{
  /**Version, history and license.
   * <ul>
   * <li>2014-11-09 Hartmut chg: Capability of StateParallel contained here, class StateParallel removed: 
   *   It is possible to have a StateComposite with its own sub states, but a second or more parallel states
   *   in an own composite, which is yet the class StateAddParallel. It is {@link StateParallel}. More simple, more flexibility. 
   * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateCompositeBase}, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
   * <li>2013-05-11 Hartmut new: It is a {@link EventConsumer} yet. Especially a timer event needs a destination
   *   which is this class.
   * <li>2013-04-27 Hartmut adapt: The {@link #entry(EventMsg2)} and the {@link #entryAction(EventMsg2)} should get the event
   *   from the transition. See {@link StateSimpleBase}.
   * <li>2013-04-13 Hartmut re-engineering: 
   *   <ul>
   *   <li>New method {@link #setDefaultState(StateSimpleBase)}
   *   <li>{@link #entryDefaultState()} is package private now, it regards requirements of {@link StateParallelBase}.
   *   <li>The old override-able method entryDefault() was removed.
   *   <li>The overridden entry() was removed, replaced by #entryComposite, which is called in {@link StateSimpleBase#entry(int)}
   *     if the instance is this type.    
   *   </ul>
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
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
  public static final int version = 20130511;

  protected int maxStateSwitchesInLoop = 1000;
  
  /**Stores whether this composite state is active. Note that the #stateAct is set as history state
   * as well the state is not active. This bit is set to false too if the current state is exited
   * and a new state is not entered yet, while temporary transition processing. It helps to prevent double
   * execution of the {@link #exitTheState()} routine if exit of the enclosing state is processed.*/
  boolean isActive;
  
  /**If set true, then any state transition is logged with System.out.printf("..."). One can use the 
   * {@link org.vishia.msgDispatch.MsgRedirectConsole} to use a proper log system. 
   */
  private boolean debugState = false;
  
  /*package private*/ StateSimple stateAct;
  
  StateSimple stateDefault;
  
  
  
  /**List of all sub states of this composite state. This list is necessary to build the transition paths on startup.
   * It is nice to have for debugging. Therefore its name starts with 'a' to set it on top of variable list in debugging. */
  final StateSimple[] aSubstates;
  
  
  
  /**A composite state can contain either only parallel states, or one or more additional parallel state.
   * In the second case the {@link #aSubstates} of this are the first parallel state composition.
   */
  final StateComposite[] aParallelstates;
  
  /**Only for the {@link StateMachine#topState} and GenStm*/
  public StateComposite(StateMachine stateMachine, StateSimple[] aSubstates, StateComposite[] aParallelstates){
    this.aSubstates = aSubstates;
    this.aParallelstates = aParallelstates;
    this.stateMachine = stateMachine;
    this.stateId = "";
  };
  
  
  
  
  /**The constructor of any StateComposite checks the class for inner classes which are the states.
   * Each inner class which is instance of {@link StateSimple} is instantiated and stored both in the {@link StateMachine#stateMap} 
   * to find all states by its class.hashCode
   * and in {@link #aSubstates} for debugging only.
   * <br><br>
   * After them {@link #buildStatePathSubstates()} is invoked to store the state path in all states.
   * Then {@link #createTransitionListSubstate(int)} is invoked which checks the transition of all states recursively. 
   * Therewith all necessary data for the state machines's processing are created on construction. 
   * 
   * @see StateMachine#StateMachine()
   * @see StateComposite#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateComposite#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   */
  public StateComposite() {
    super();
    Class<?> clazz = this.getClass();
    //Class<?> clazzTop1 = clazz;
    Object env = this;
    do {
      Class<?> clazzTop2 = env.getClass();
      if(DataAccess.isOrExtends(clazzTop2, StateMachine.class)){
        this.stateMachine = (StateMachine)env;
        //firstly create the stateMap. Note: This is called in constructor of the StateTop-superclass.
      } else {
        env = DataAccess.getEnclosingInstance(env);
      }
    } while(stateMachine == null && env !=null);
      
    if(stateMachine == null){
      throw new IllegalArgumentException("");
    }
    Class<?>[] innerClasses = clazz.getDeclaredClasses();
    if(innerClasses.length >0) {  //it is a composite state.
      List<StateSimple> listSubstates = null;
      List<StateParallel> listParallelstates = null;
      int ixSubstates = -1;
      try{
        for(Class<?> clazz1: innerClasses) {
          if(DataAccess.isOrExtends(clazz1, StateSimple.class)) {
            Constructor<?>[] ctor1 = clazz1.getDeclaredConstructors();
            //Constructor<?>[] ctor = clazz2.getDeclaredConstructors();
            ctor1[0].setAccessible(true);
            final Object oState = ctor1[0].newInstance(this);   //creates the instance, maybe a StateComposite or a StateAddParallel.
            //Note that the inner states are processed already in the yet called constructor.
            final StateSimple state;
            if(oState instanceof StateParallel) {
              if(listParallelstates ==null) { listParallelstates = new LinkedList<StateParallel>(); }
              final StateParallel stateParallel = (StateParallel)oState;
              listParallelstates.add(stateParallel);
              state = stateParallel;
            } else {
              if(listSubstates ==null) { listSubstates = new LinkedList<StateSimple>(); }
              state = (StateSimple)oState;
              listSubstates.add(state);
            }
            state.stateId = clazz1.getSimpleName();
            state.stateMachine = this.stateMachine;
            state.enclState = this;
            int idState = clazz1.hashCode();
            stateMachine.stateMap.put(idState, state);
            try { 
              clazz1.getDeclaredField("isDefault");
              if(this.stateDefault != null){ 
                throw new IllegalArgumentException("StateComposite - more as one default state in;" + stateId); 
              }
              this.stateDefault = state;  //The first state is the default one.
            } catch(NoSuchFieldException exc){} //empty!
          }
        }
      } catch(Exception exc){
        throw new RuntimeException(exc);
      }
      if(listSubstates !=null ){
        this.aSubstates = listSubstates.toArray(new StateSimple[listSubstates.size()]); 
      } else {
        this.aSubstates = null;
      }
      if(stateDefault == null && aSubstates !=null){ 
        throw new IllegalArgumentException("StateMachine - a default state is necessary. Define \"final boolean isDefault = true\" in one of an inner class State;" + stateId); 
      }
      if(listParallelstates !=null ){
        this.aParallelstates = listParallelstates.toArray(new StateParallel[listParallelstates.size()]); 
      } else {
        this.aParallelstates = null;
      }
    } else { //no inner states
      this.aSubstates = null;
      this.aParallelstates = null;
    }
  }
  
  
  
  /**It is called from outside if the state machine is build other than with Reflection.
   * @param key the key which is used to find the state in {@link StateMachine#stateMap} while preparing the transitions
   * @param state a new inner state.
   */
  public void addState(int key, StateSimple state){
    int ix = 0;
    if(state instanceof StateParallel) {
      while(ix < aParallelstates.length && aParallelstates[ix] !=null){ ix +=1; } //search next free
      if(ix >= aParallelstates.length) throw new IllegalArgumentException("too many parallel states to add");
      aParallelstates[ix] = (StateParallel)state;
    } else {
      while(ix < aSubstates.length && aSubstates[ix] !=null){ ix +=1; } //search next free
      if(ix >= aSubstates.length) throw new IllegalArgumentException("too many states to add");
      aSubstates[ix] = state;
    }
    stateMachine.stateMap.put(state.hashCode(), state);
    if(stateDefault ==null){
      stateDefault = state;  //the first state is the default state.
    }
  }
  
  
  
  
  /**Sets the path to the state for this and all {@link #aSubstates}, recursively call.
   * @param enclState
   * @param recurs
   */
  void buildStatePathSubstates(StateComposite enclState, int recurs) {
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty");
    this.buildStatePath(enclState);
    if(aSubstates !=null) {
      for(StateSimple subState: this.aSubstates){
        if(subState instanceof StateComposite){
          ((StateComposite) subState).buildStatePathSubstates(this, recurs +1);
        } else {
          subState.buildStatePath(this);
        }
      }
    }
    if(aParallelstates !=null) {
      for(StateComposite parallelState: this.aParallelstates){
        parallelState.buildStatePathSubstates(this, recurs +1);
      }
    }
  }



  
  
  /**Create all transition list for this state and all {@link #aSubstates}, recursively call.
   * @param recurs
   */
  void createTransitionListSubstate(int recurs){
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty, too many subStates; state=" + stateId);
    this.createTransitionList(this, null, 0);  
    if(aSubstates !=null) {
      for(StateSimple subState: this.aSubstates){
        if(subState instanceof StateComposite){
          ((StateComposite)subState).createTransitionListSubstate(recurs+1);
        } else {
          subState.createTransitionList(subState, null,0);  //simple state.
        }
      } 
    }
    if(aParallelstates !=null) {
      for(StateComposite parallelState: this.aParallelstates){
        parallelState.createTransitionListSubstate(recurs+1);
      } 
    }
  }



  
  
  


  /**Sets the default state of this composite. 
   * This routine has to be called in the constructor of the derived state after calling the super constructor.
   * @param stateDefault The state which will be set if the composite class was selected 
   *   but an inner state was not set. The {@link #entryTheState(EventMsg2)} to the default state is invoked
   *   if this state will be {@link #processEvent(EventMsg2)}.
   */
  public void XXXsetDefaultState(StateSimple stateDefault ){
    assert(stateDefault == null);  //invoke only one time.
    this.stateDefault = stateDefault;
  }
  
  
  /**Check whether this composite state has the given state as direct actual sub state
   * @param state Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  public final boolean isInState(StateSimple state){ 
    return isInState()             //this state is active too, or it is the top state.
        && (  stateAct == state   //the given state is the active.
           || aSubstates == null  //a poor parallel state container.
           );   
  }
  
  /**This method is used to entry the default state if the actual state is null (first invocation).  */
  /*package private*/ final int entryDefaultState(){ 
    int ret = 0;
    if(aParallelstates !=null) {
      for(StateComposite state: aParallelstates){
        ret |= state.entryDefaultState();
      }
    }
    if(aSubstates !=null) {
      ret |= aSubstates[0].entryTheState(null,0);
    }
    /*
    if(this instanceof StateParallel){
      return ((StateParallel)this).entryDefaultParallelStates();
    } else {
      return stateDefault.entryTheState(null);
    }
    */
    return ret;
  }

  
  
  /**This method should be called from outside if the history state should be entered and all history states
   * should be entered in sub states.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryDeepHistory(EventObject ev){
    StateSimple stateActHistory = stateAct;  //save it
    int cont = entryTheState(ev,0);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    if(stateActHistory instanceof StateComposite){
      cont = ((StateComposite)stateActHistory).entryDeepHistory(ev);
    } else {
      cont = stateActHistory.entryTheState(ev,0);           //entry in the history sub state.
    }
    return cont;
  }
  
  
  /**This method should be called from outside if the history state should be entered but the default state of any
   * sub state should be entered.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryFlatHistory(EventObject ev){
    StateSimple stateActHistory = stateAct;  //save it
    int cont = entryTheState(ev,0);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    cont = stateActHistory.entryTheState(ev,0);             //entry in the history sub state.
    return cont;
  }
  
  

  
  /**This method sets this state in the enclosing composite state and sets the own {@link #stateAct} to null 
   * to force entry of the default state if {@link #process(Event)} is running firstly after them.
   * If this method will be called recursively in an {@link #entry(int)} of an inner state,
   * that entry sets the {@link #stateAct} in {@link #setState(StateSimpleBase)} after them so it is not null.
   * <br><br> 
   * This method should be overridden if a entry action is necessary in any state. 
   * The overridden form should call this method in form super.entry(isConsumed):
   * <pre>
  public int entry(isConsumed){
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
  //@Override 
  /**package private*/ final void XXXentryComposite(){
    //super.entry(isConsumed);
    stateAct = null;
    isActive = true;
    //return isConsumed | mRunToComplete;
  }
  

  
  
  public final int XXXentry(Class state, EventObject ev){
    int id = state.hashCode();
    StateSimple entryState = stateMachine.stateMap.get(new Integer(id));
    return entryState.entryTheState(ev,0);
  }

  
  
  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking either its {@link StateComposite#_processEvent(EventMsg2)}
   * or its {@link #checkTransitions(EventMsg2)} method.
   * If this method returns {@link StateSimpleBase#mRunToComplete} that invocation is repeated in a loop, to call
   * the transition of the new state too. But if the event was consumed by the last invocation, it is not supplied again
   * in the loop, the event parameter is set to null instead. It means only conditional transitions are possible.
   * This behavior is conform with the UML definition.
   * If the loop would not terminate because any state have a valid transition and the state machine switches forever,
   * the loop is terminated with an exception for a number of {@link #maxStateSwitchesInLoop}. This exception occurs
   * if the user stateMachine conditions are faulty only.
   * <br><br>
   * This method is not attempt to override by the user. Only the class {@link StateParallelBase} overrides it
   * to invoke the processing of all parallel boughs.
   * @param evP The event supplied to the {@link #checkTransitions(EventMsg2)} method.
   * @return The bits {@link StateSimpleBase#mEventConsumed} as result of the inside called {@link #checkTransitions(EventMsg2)}.
   *   Note that if an event is consumed in an inner state, it should not be applied to its enclosing state transitions. 
   */
  /*package private*/ int _processEvent(final EventObject evP){  //NOTE: should be protected.
    int cont = 0;
    if(aSubstates !=null) {
      cont = processEventOwnStates(evP);
    }
    //
    //Process for all parallel states
    if(aParallelstates !=null) {
      for(StateComposite stateParallel : aParallelstates) {
        cont |= stateParallel._processEvent(evP);
      }
    }
    //
    //cont &= mEventConsumed;
    //
    //Process to leave this state.
    //
    //If the event was consumed in any inner transition, it is not present for the own transitions. UML-conform.
    EventObject evTrans = (cont & StateSimple.mEventConsumed)==0 ? evP: null;  
    //
    if(  evTrans != null   //evTrans is null if it was consumed in inner transitions. 
      || (modeTrans & StateSimple.mRunToComplete) !=0  //state has only conditional transitions
      ){
      //process the own transition. Do it after processing the inner state (omg.org)
      //and only if either an event is present or the state has only conditional transitions.
      StateSimple statePrev = stateAct;
      int trans = checkTransitions(evTrans); 
      if(debugState && (cont & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, trans); }
      cont |= trans;
    }
    return cont;  //runToComplete.bit may be set from an inner state transition too.
  }

  
  
  
  private int processEventOwnStates(EventObject ev) {
    //this composite has own substates, not only a container for parallel states:
    EventObject evTrans = ev;
    int catastrophicalCount =  maxStateSwitchesInLoop;
    int contLoop;
    do{

      //
      //
      contLoop = 0;
      if(stateAct == null){
        contLoop |= entryDefaultState();  //regards also Parallel states.
        if(debugState && (contLoop & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(null, evTrans, contLoop); }
      } 
      if(stateAct instanceof StateComposite){
        //recursively call for the composite inner state
        contLoop |= ((StateComposite)stateAct)._processEvent(evTrans); 
      } else {
        StateSimple statePrev = stateAct;
        int trans = stateAct.checkTransitions(evTrans);
        if(debugState && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, trans); }
        contLoop |= trans;
      }
      //
      if((contLoop & StateSimple.mEventConsumed) != 0){
        evTrans = null;
      }
      if(catastrophicalCount == 4) {
        catastrophicalCount = 3;  //set break point! to debug the loop
      }
    } while(isActive   //leave the loop if this composite state is exited.
        && (contLoop & mRunToComplete) !=0    //loop if runToComplete-bit is set, the new state should be checked.
        && --catastrophicalCount >=0
        );
    if(catastrophicalCount <0) {
      throw new RuntimeException("unterminated loop in state switches");
    }
    return contLoop;
    
  }
  
  
  
  
  
  private void printStateSwitchInfo(StateSimple statePrev, EventObject evTrans, int cont) {
    //DateOrder date = new DateOrder();
    //Thread currThread = Thread.currentThread();
    //String sThread = currThread.getName();
    String sStatePrev = statePrev !=null ? statePrev.stateId : "INIT";
    //String sActiveState = getActiveState();
    StringBuilder uStateNext = new StringBuilder();
    if(stateAct == null){ uStateNext.append("--inactive--"); }
    else {
      StateSimple stateAct1 = stateAct;
      uStateNext.append(stateAct.stateId);
      while(stateAct1 instanceof StateComposite) {
        stateAct1 = ((StateComposite)stateAct1).stateAct;
        if(stateAct1 !=null) { 
          uStateNext.insert(0, '.').insert(0,  stateAct1.stateId);
        }
      }
    }
    if(!isActive){
      System.out.println("StateCompositeBase - leaved; " + sStatePrev + " ==> " + uStateNext + "; event=" + evTrans + ";");
    } else if((cont & StateSimple.mEventConsumed)!=0) {  //statePrev != stateAct){  //from the same in the same state!
      System.out.println("StateCompositeBase - switch;" + sStatePrev + " ==> "  + uStateNext + "; event=" + evTrans + ";");
    } else if(evTrans !=null){ 
      System.out.println("StateCompositeBase - switch;" + sStatePrev + " ==> "  + uStateNext + "; not used event=" + evTrans + ";");
    }
    
  }

  
  


  /**Sets the state of the composite state.
   * This method should be called
   * @param stateSimple Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  /*package private*/ final void XXXsetStateParallel(StateSimple stateSimple) { //, EnumState stateNr) {
   this.stateAct = stateSimple;
   this.isActive = true;
  }

  /**Exits first the actual sub state (and that exits its actual sub state), after them this state is exited.
  /**This method may be overridden with exit actions:
   * <pre>
  public EnclosingStateType exit(){
    TypeOfEnclosingState enclState = super.exit(); //call firstly! It exits sub states.
    statementsOfExit();
    return enclSate;
  }
   * </pre> 
   * @return The enclosing state, which can used for entry immediately.
   * @see org.vishia.stateMachine.StateSimpleBase#exit()
   */
  @Override public StateComposite exitTheState(){ 
    if(isActive && stateAct !=null){
      stateAct.exitTheState();    //recursively call for all inner states which are yet active.
      isActive = false; //NOTE that StateSimpleBase.exit() sets isActive to false already. It is done twice.
    }
    if(aParallelstates !=null) {
      for(StateComposite parallelState : aParallelstates) {
        parallelState.exitTheState();
      }
    }
    return super.exitTheState();
  }

  
  /**Returns the name of the active state of this composite state or the active state of any other state in the chart.
   * @return
   */
  protected String getActiveState(){
    if(isActive) return getStatePath().toString();
    else if(enclState !=null){ return enclState.getActiveState(); }
    else return "--inactive--";
  }
  
  
  @Override   public CharSequence getStatePath(){
    StringBuilder uPath = new StringBuilder(120);
    StateSimple state = this;
    while((state = state.enclState) !=null){
      uPath.append(':').append(state.stateId);
    }
    state = this;
    //*
    do{
      uPath.append('.').append(state.stateId);
      if(state instanceof StateComposite){
        state = ((StateComposite)state).stateAct;
      } else { state = null; }
    } while(state !=null);
    //*/
    return uPath;
  }
  
  
  
  public void toString(StringBuilder u) {
    String separator = "";
    if(aSubstates !=null) {
      u.append(stateId);
      if(isActive) {
        u.append(stateAct.toString());
      }
      separator = " || ";
    }
    /*
    return stateId + ":" + (!isActive ? "-inactive-": 
      (stateAct instanceof StateComposite ? stateAct.toString()  //recursive call of toString 
          : (stateAct == null ? "null" : stateAct.stateId)));   //attention: StateSimple shows the state structure backward. Use only stateId! 

    */
    if(aParallelstates !=null) {
      for(StateComposite stateParallel: aParallelstates){
        u.append(separator);
        stateParallel.toString(u);
        separator = " || ";
      }
    }
  
    
  }
  
  
  @Override public String toString(){ 
    StringBuilder u = new StringBuilder();
    toString(u);
    return u.toString();
  }


}
