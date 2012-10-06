package org.vishia.util.sampleStateM;

import org.vishia.util.Event;
import org.vishia.util.StateCompositeBase;
import org.vishia.util.StateSimpleBase;
import org.vishia.util.StateTopBase;

public class StateTemplate {

  
  enum EStateTop{ Null, Idle, Ready, Process }
  
  enum EStateProcess{ Null, P1, P2 }
  
  
  
  private class StateTop extends StateTopBase<StateTop>{

    protected StateTop() {
      super("StateM");
    }

    @Override public int entryDefault(){
      return stReady.entry(eventNotConsumed);
    }
  

    /*
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
    */
  }
  StateTop stTop = new StateTop();

  
  private class StateReady extends StateSimpleBase<StateTop>{
    
    
    StateReady(StateTop superState) { super(superState, "Ready"); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int trans(Event ev) {
      return StateSimpleBase.eventNotConsumed;
    }
  }
  StateReady stReady = new StateReady(stTop);

  
  private class StateProcess extends StateCompositeBase<StateProcess, StateTop>{

    protected StateProcess(StateTop superState) { super(superState, "Process"); }

    @Override public int entryDefault(){
      return mRunToComplete; //B1.entry(notConsumed);
    }
  
    @Override public int trans(Event ev){
      return StateSimpleBase.eventNotConsumed;
    }

    /*
    @Override public int switchState(Event ev) {
      int cont = StateSimpleBase.consumed + StateSimpleBase.complete;
      switch(stateNr()){
        //case Idle: cont = stIdle.trans(ev); break;
      }
      // TODO Auto-generated method stub
      return cont;
    }
    */
  }
  StateProcess stProcess = new StateProcess(stTop);


  
  
  
}
