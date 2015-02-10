package org.vishia.states;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.DataAccess;
import org.vishia.util.InfoAppend;

public class StateCompositeFlat extends StateSimple implements InfoAppend
{
  StateSimple stateDefault;

  /**List of all sub states of this composite state. This list is necessary to build the transition paths on startup.
   * It is nice to have for debugging. Therefore its name starts with 'a' to set it on top of variable list in debugging. */
  final StateSimple[] aSubstates;

  /**Only used for the {@link StateMachine#topState} 
   * and as special constructor to build a state machine from other data. See org.vishia.stateMGen.StateMGen. 
   */
  public StateCompositeFlat(String name, StateMachine stateMachine, StateSimple[] aSubstates){
    this.aSubstates = aSubstates;
    this.stateMachine = stateMachine;
    this.stateId = name;
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
  public StateCompositeFlat() {
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
            if(listSubstates ==null) { listSubstates = new LinkedList<StateSimple>(); }
            state = (StateSimple)oState;
            listSubstates.add(state);
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
    } else { //no inner states
      this.aSubstates = null;
    }
  }
  
  
  
  /**It is called from outside if the state machine is build other than with Reflection.
   * @param key the key which is used to find the state in {@link StateMachine#stateMap} while preparing the transitions
   * @param state a new inner state.
   */
  public void addState(int key, StateSimple state){
    int ix = 0;
    while(ix < aSubstates.length && aSubstates[ix] !=null){ ix +=1; } //search next free
    if(ix >= aSubstates.length) throw new IllegalArgumentException("too many states to add");
    aSubstates[ix] = state;
    stateMachine.stateMap.put(state.hashCode(), state);
    if(stateDefault ==null){
      stateDefault = state;  //the first state is the default state.
    }
  }
  
  
  
  
  /**Sets the path to the state for this and all {@link #aSubstates}, recursively call.
   * This method is invoked in the constructor of the state machine only one time.
   * It is not for application.
   * @param enclState
   * @param recurs
   */
  @Override void buildStatePathSubstates(StateSimple enclState, int recurs) {
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty");

    this.buildStatePath(enclState);
    if(aSubstates !=null) {
      for(StateSimple subState: this.aSubstates){
        subState.buildStatePathSubstates(this, recurs +1);
      }
    }
  }



  
  
  /**Create all transition list for this state and all {@link #aSubstates}, recursively call.
   * This method is invoked in the constructor of the state machine only one time.
   * It is not for application.
   * @param recurs
   */
  void createTransitionListSubstate(int recurs){
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty, too many subStates; state=" + stateId);
    this.createTransitionList(this, null, 0);  
    if(aSubstates !=null) {
      for(StateSimple subState: this.aSubstates){
        subState.createTransitionListSubstate(recurs+1);
      } 
    }
  }



  
  
  
  /**This method is used to entry the default state of this composite if the actual state is null (first invocation).  */
  /*package private*/ final int entryDefaultState(){ 
    int ret = 0;
    if(stateDefault !=null) {
      ret |= stateDefault.entryTheState(null,0);
    }
    return ret;
  }


  
  
  

}
