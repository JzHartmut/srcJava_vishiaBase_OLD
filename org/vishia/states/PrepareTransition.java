package org.vishia.states;

import java.util.LinkedList;
import java.util.List;

import org.vishia.event.EventTimeout;
import org.vishia.util.Debugutil;

/**An instance of this class is created only temporary to build the transition path, set the #exitStates
 * and #entryStates.
 */
class PrepareTransition
{
  final StateSimple.Trans trans;
  
  final StateSimple state;
  
  /**The destination states filled with {@link #buildDstStates()} with the length of {@link #dst}.
   * Left null if no dst states are given, especially on existing {@link Trans#choice}.
   */
  final StateSimple[] dstStates;
  
  final StateSimple[] exitStates;
  
 
  /**The current indices in {@link #dstStates}.{@link StateSimple#statePath} while evaluating the entry path. */
  final int[] ixInStatePath;
  
  /**Current index to write {@link Trans#entryStatesOld}. Pre-increment. */
  //int ixEntryStatesOld = -1;
  
  
  PrepareTransition(StateSimple state, StateSimple.Trans trans, StateSimple[] exitStates)
  { this.trans = trans;
    this.state = state;
    this.dstStates = trans.dst == null ? null : new StateSimple[trans.dst.length];
    this.ixInStatePath = (this.dstStates == null) ? null : new int[this.dstStates.length];
    this.exitStates = exitStates;
  }
  
  void execute(){
    if(state.stateId.equals("StateParallel"))
      Debugutil.stop();

    if(trans.dst !=null) {
      buildDstStates();
      searchStateCommon(); //after them ixDstPath was set.
      buildExitPath();
      buildEntryStates();
    }
    if(trans instanceof StateSimple.Timeout) {
      state.transTimeout = (StateSimple.Timeout)trans;
      //trans.check = new ConditionTimeout();
      state.millisectimeout = state.transTimeout.millisec;
      searchOrCreateTimerEvent();
    }

  }
  
  private void buildDstStates() {
    //search all dst state instances from the given class. In constructor only the class is known.
    for(int ixdst = 0; ixdst < trans.dst.length; ++ixdst){
      dstStates[ixdst] = state.stateMachine.stateMap.get(new Integer(trans.dst[ixdst]/*.hashCode()*/));
    }
  }
  
  /**Searches the common state between all {@link #dstStates} and the source state.
   * All states till the common state should be exited. All states from the common state should be entered.
   * As result the {@link #ixInStatePath} is set with the index after the found stateCommon 
   */
  private void searchStateCommon() {
    //search the common state for all transitions. 
    StateSimple stateCommon;  //the common state between this and all dstState.
    int zStatePath =1;
    int ixSrcPath = state.statePath.length -2;  //Note: the commonState is never this itself, at least one exit.
    do {
      stateCommon = state.statePath[ixSrcPath];  
      int ixdst = -1;
      //check all dst states whether the common state is in that state path. If it is not in the statePath, set stateCommon = null to abort the search.
      //If the stateCommon is in the statePath of all dstStates then ixDstPath[] is set with the index of the stateCommon in the dstPath.
      //It should the same index for all states because the statePath starts from the stateTop for all.
      while(stateCommon !=null && ++ixdst < dstStates.length) {  //commonSearch: abort this while if at least one 
        //check all dstStates down to either the exitState or down to to top state.
        //If the exit state is equal any entry enclosing state or both are null (top state), then it is the common state.
        StateSimple dstState = dstStates[ixdst];
        ixInStatePath[ixdst] = dstState.statePath.length-1;  //Note: start with dstState itself because (dst    (src)---->) transition from this to its enclosing state
        while(stateCommon !=null && dstState.statePath[ixInStatePath[ixdst]] != stateCommon){
          if(--ixInStatePath[ixdst] < 0) { //end is reached without found stateCommon
            stateCommon = null;  //abort test.
          }
        }
      }
    } while(stateCommon == null && --ixSrcPath >=0); //continue if the stateCommon is not member of all dst state paths.    
    if(stateCommon == null){
      throw new IllegalArgumentException("no common state found");
    }
    trans.exitStates = new StateSimple[state.statePath.length - (ixSrcPath +1)];  //don't exit the stateCommon
    for(int ixdst = 0; ixdst < trans.dst.length; ++ixdst){
      ixInStatePath[ixdst] +=1;  //first entry in state after stateCommon.  
    }
  }
  
  
  
  private void buildExitPath() {
    int ixSrcPath = state.statePath.length;
    for(int ixExitStates = 0; ixExitStates < trans.exitStates.length; ++ixExitStates){
      trans.exitStates[ixExitStates] = state.statePath[--ixSrcPath];  //copy the statePath from this backward to exitStates-List.
    }
    if(trans instanceof StateSimple.TransJoin) {
      createJoinTransitionInTheFirstJoinState();
    }

  }
  

  /**Completes a join transition with the found in a {@link StateParallel} with all necessary exit states from all join source states
   * and enters the instance of a join transition in all join source state's {@link StateSimple#transJoins}.
   * Before this operation was called, the {@link Trans#exitStates} contains only that exit states from that state
   * which defines the {@link TransJoin}. But the exit states form all source states are necessary. 
   * 
   * @param trans The found join transition in a parallel state.
   */
  private void createJoinTransitionInTheFirstJoinState()
  { StateSimple.TransJoin transJoin = (StateSimple.TransJoin)this.trans; //the outer class is a TransJoin, use it.
    transJoin.joinStates = new StateSimple[transJoin.joinStateHashes.length];
    int ixSrcStates =-1;
    int ixExitStates = 0;
    for(int joinStateClass: transJoin.joinStateHashes){
      StateSimple srcState = state.stateMachine.stateMap.get(new Integer(joinStateClass));
      if(ixExitStates < srcState.statePath.length) {
        ixExitStates = srcState.statePath.length;
      }
      transJoin.joinStates[++ixSrcStates] = srcState;  
    }
    List<StateSimple> listExitStates = new LinkedList<StateSimple>();
    StateSimple stateCommon = transJoin.exitStates[transJoin.exitStates.length -1].enclState;  //the last, its parent
    StateSimple stateExit3 = null;
    do {
      ixExitStates -=1;
      StateSimple stateExit2 = null;
      for(ixSrcStates = 0; ixSrcStates < transJoin.joinStates.length; ++ixSrcStates){
        StateSimple srcState = transJoin.joinStates[ixSrcStates];
        if(srcState.statePath.length > ixExitStates) {
          stateExit3 = srcState.statePath[ixExitStates];
          if(stateExit3 != stateCommon){
            if(stateExit2 == null) { 
              stateExit2 = stateExit3;
              listExitStates.add(stateExit2);
            } else { //stateExit2 is set for this level already:
              if(stateExit3 != stateExit2) {
                listExitStates.add(stateExit3);
              }
            }
          }
        }
      }
    } while(stateExit3 != stateCommon);  
    //change the exit States to the exit states of all join source states:
    transJoin.exitStates = listExitStates.toArray(new StateSimple[listExitStates.size()]);
    StateSimple srcState1 = transJoin.joinStates[0];
    if(srcState1.transJoins == null) {
      srcState1.transJoins = new StateSimple.TransJoin[1];
    } else {
      StateSimple.TransJoin[] transJoins = new StateSimple.TransJoin[srcState1.transJoins.length +1];
      System.arraycopy(srcState1.transJoins, 0, transJoins, 0, srcState1.transJoins.length);
      srcState1.transJoins = transJoins;
    }
    srcState1.transJoins[srcState1.transJoins.length -1] = transJoin;
  }



  
  /**Builds the {@link Trans#entryStates} with given found common state and given {@link Trans#dstStates} and their {@link StateSimple#statePath}.
   * The first entry state is the StateSimple.statePath[{@link #ixInStatePath}[0]], the first entry state of the first fork-branch.
   * Then the ixInStatePath[..] is incremented for all fork-branches. All states are written in {@link Trans#entryStates} parallel for all branches
   * but states there are the same in the branches are not written twice. Example see {@link Trans#entryStates}.
   */
  private void buildEntryStates() 
  {
    //int[] entries1 = new int[dstStates.length];
    StateSimple[] stateFork = new StateSimple[dstStates.length];
    if(stateFork.length == 3)
      Debugutil.stop();
    if(trans.transId.equals("Trans_Running0"))
      Debugutil.stop();
    List<StateSimple> listEntries = new LinkedList<StateSimple>();
    int ixStatePath = ixInStatePath[0];  //start with the states after the common state. All ixDstPath[...] are the same yet.
    int dstNotReached;  // if 0 then all reached.
    do {
      dstNotReached = ixInStatePath.length;  // if 0 then all reached.
      int ixForkBranch, ixCheckExistingForkBranch;
      for(ixForkBranch = 0; ixForkBranch < ixInStatePath.length; ++ixForkBranch) { /*entries1[ixForkBranch] = 0; */ stateFork[ixForkBranch] = null; } //clean
      //
      for(ixForkBranch = 0; ixForkBranch < ixInStatePath.length; ++ixForkBranch) {  //check all branches of a fork.
        StateSimple[] statePath = dstStates[ixForkBranch].statePath;
        if(ixStatePath < statePath.length ) { // if >=, it is reached
          StateSimple entryState = statePath[ixStatePath];
          //search whether this state is processed already:
          //int hashEntryState = entryState.hashCode();
          if(ixForkBranch == 0) { 
            ixCheckExistingForkBranch = stateFork.length;       //first is the first: add the state.
          } else {
            ixCheckExistingForkBranch = 0;  //search whether the entryState is in the list of stateFork, which are entered already.
            while(ixCheckExistingForkBranch < stateFork.length && stateFork[ixCheckExistingForkBranch] != entryState) { //entries1[ixForkBranch2] != hashEntryState) {
              ixCheckExistingForkBranch +=1;  //search in short array.
            } //isForkBranch2 == entries.length if the entryState is not found, if it is not entered already.
          }
          if(ixCheckExistingForkBranch == stateFork.length) { //not found or first
            //entries1[ixForkBranch] = hashEntryState;   //set it to the place for this dstState.
            stateFork[ixForkBranch] = entryState;
            listEntries.add(entryState);
          }
        }
        else {
          dstNotReached -=1;  //dst state was reached.
        }
      }
      ixStatePath +=1;
    } while( dstNotReached >0);
    trans.entryStates = listEntries.toArray(new StateSimple[listEntries.size()]);
  }
  

  
  /**Gets the timeout event from the top state or the {@link StateParallel} 
   * or creates that event.
   * All non-parallel states need only one timeout event instance because only one of them is used.
   */
  private void searchOrCreateTimerEvent() {
    if(state.stateMachine.theThread == null) {
      throw new IllegalArgumentException("This statemachine needs a thread and a timer manager because timeouts are used. Use StateMachine(thread, timer); to construct it");
    }
    //Search in the most superior StateComposite which is either the top state (has not a enclState)
    //or which is a StateParallel.
    StateSimple topParallel = state;
    while(topParallel.enclState !=null && !(topParallel.enclState instanceof StateParallel)) {
      topParallel = topParallel.enclState;
    }
    //parent is either the top state or a StateComposite inside a StateParallel
    if(topParallel.evTimeout == null) {
      topParallel.evTimeout = new EventTimeout(state.stateMachine, state.stateMachine.theThread);
      //parent.evTimeout = stateMachine.theThread.new TimeEventOrder(stateMachine, stateMachine.theThread);
    }
    //Store the reference to the evTimeout in this state too to detect the own timeout event.
    state.evTimeout = topParallel.evTimeout;
  }


  
  
  
}
