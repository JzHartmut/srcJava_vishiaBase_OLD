package org.vishia.stateMachine.example;

import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Event;

public class StateTemplate {

  
  enum EStateTop{ Null, Idle, Ready, Process }
  
  enum EStateProcess{ Null, P1, P2 }
  
  
  
  private class StateTop extends StateTopBase<StateTop>{

    protected StateTop() {
      super("StateM");
      setDefaultState(stateReady);
    }

    
    private final StateSimpleBase<StateTop> stateReady = new StateSimpleBase<StateTop>(this, "Ready", false){
      @Override public int trans(Event<?,?> ev) {
        return StateSimpleBase.eventNotConsumed;
      }
    };

    private class StateProcess extends StateCompositeBase<StateProcess, StateTop>{

      protected StateProcess(StateTop superState) { super(superState, "Process"); setDefaultState(stateA); }

      @Override public int trans(Event<?,?> ev){
        return StateSimpleBase.eventNotConsumed;
      }

      private final StateSimpleBase<StateProcess> stateA = new StateSimpleBase<StateProcess>(this, "A", false){
        @Override public int trans(Event<?,?> ev) {
          return StateSimpleBase.eventNotConsumed;
        }
      };

      private final StateSimpleBase<StateProcess> stateB = new StateSimpleBase<StateProcess>(this, "A", false){
        @Override public int trans(Event<?,?> ev) {
          return StateSimpleBase.eventNotConsumed;
        }
      };

    }
    StateProcess stateProcess = new StateProcess(stTop);

    
    
  }
  StateTop stTop = new StateTop();

  
  

  
  
  
}
