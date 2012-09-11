package org.vishia.util.sampleStateM;

import org.vishia.util.Event;
import org.vishia.util.StateBase;
import org.vishia.util.StateComboBase;
import org.vishia.util.StateTopBase;

public class StateTemplate {

  
  enum EStateTop{ Null, Idle, Ready, Process }
  
  enum EStateProcess{ Null, P1, P2 }
  
  
  
  private static class StateTop extends StateTopBase<EStateTop, StateTemplate>{

    protected StateTop(StateTemplate env) {
      super(EStateTop.Null, env);
    }

    @Override public int entry(int isConsumed){
      return env.stReady.entry(isConsumed);
    }

    @Override public int switchState(Event ev) {
      int cont = notConsumed + complete;
      switch(stateNr()){
        //case Idle: cont = stIdle.process(ev); break;
        case Null:    cont = env.stTop.entry(notConsumed);
        case Ready:   cont = env.stReady.trans(ev); break;
        case Process: cont = env.stProcess.process(ev); break;
      } //switch
      // TODO Auto-generated method stub
      return cont;
    }
  }
  StateTop stTop = new StateTop(this);

  
  private class StateReady extends StateBase<StateTop, EStateTop, StateTemplate>{
    
    
    StateReady(StateTop superState) { super(superState, EStateTop.Null); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int trans(Event ev) {
      return StateBase.notConsumed;
    }
  }
  StateReady stReady = new StateReady(stTop);

  
  private static class StateProcess extends StateComboBase<StateTop, EStateTop, EStateProcess, StateTemplate>{

    protected StateProcess(StateTop superState) { super(superState, EStateTop.Process, EStateProcess.Null); }

    @Override public int trans(Event ev){
      return StateBase.notConsumed;
    }

    @Override public int switchState(Event ev) {
      int cont = StateBase.consumed + StateBase.complete;
      switch(stateNr()){
        //case Idle: cont = stIdle.trans(ev); break;
      }
      // TODO Auto-generated method stub
      return cont;
    }
  }
  StateProcess stProcess = new StateProcess(stTop);


  
  
  
}
