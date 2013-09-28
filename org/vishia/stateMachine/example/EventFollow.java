package org.vishia.stateMachine.example;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;

public class EventFollow  {
  
  

  enum ENull{}
  
  boolean cond1, cond2;
  
  
  EventSource evSrc = new EventSource("TestEventFollow"){
    
  };

  
  EventConsumer evDst = new EventConsumer(){

    @Override public int processEvent(Event ev) {
      state.trans(ev);
      return 1;
    }
    
    @Override public String toString(){ return "TestEventFollow"; }
  };
  
  private final EventThread evThread;

  
  EventFollow(EventThread eventThread){
    this.evThread = eventThread;
  }
  
  
  public void startState(){
    state.entry(null);
  }
  
  private class MainState extends StateTopBase<MainState>{
  
    protected MainState() { super("StateM", evThread, null);       setDefaultState(start); }


  
    /*
    @Override protected int switchState(Event ev){
      int cont = StateSimpleBase.consumed + StateSimpleBase.complete;
      switch(stateNr()){
        case Null:          cont = start.entry(StateSimpleBase.runToComplete); break;
        case Start:         cont = start.trans(ev); break;
        case StateA:        cont = stateA.trans(ev); break;
        case StateB:        cont = stateB.trans(ev); break;
        case StateC:        cont = stateC.trans(ev); break;
        } 
      return cont;
    }
    */
    
    private final class Start extends StateSimpleBase<MainState> {
      
      protected Start(MainState superState) { super(superState, "Start", false); }


      //protected Start(EventFollow enclosingState) { super(enclosingState); }
  
      @Override public void entryAction(Event<?,?> ev){
        EventTimerMng.addGlobalTimeOrder(System.currentTimeMillis() + 1000, evDst, evThread);
      }
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EventTimerMng.TimeEvent){
          EvX ev1 = new EvX(evSrc, evDst, evThread);
          cond1 = true;
          cond2 = true;
          ev1.sendEvent(EvX.Cmd.EvX);
          return exit().stateA.entry(ev);
        } else {
          return 0;
        }
      }
    }
    
    Start start = new Start(this);

    private final class StateA extends StateSimpleBase<MainState> {
      
      protected StateA(MainState superState) { super(superState, "StateA"); }

      //protected StateA(EventFollow enclosingState) { super(enclosingState); }
  
      
      @Override public int trans(Event ev){
        if(cond1){
          return exit().stateB.entry(null);
        } else {
          return 0;
        }
      }
    }
    
    StateA stateA = new StateA(this);

    private final class StateB extends StateSimpleBase<MainState> {
      
      protected StateB(MainState superState) { super(superState, "StateB"); }
      
      //protected StateB(EventFollow enclosingState) { super(enclosingState); }
  
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EvX && ev.getCmd() == EvX.Cmd.EvX){
          return exit().stateC.entry(ev);
        } else {
          return 0;
        }
      }
    }
  
    StateB stateB = new StateB(this);

  
    private final class StateC extends StateSimpleBase<MainState> {
      
      protected StateC(MainState superState) { super(superState, "StateC"); }

      //protected StateC(EventFollow enclosingState) { super(enclosingState); }
  
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EvX && ev.getCmd() == EvX.Cmd.EvX){
          return exit().stateD.entry(ev);
        } else {
          return StateSimpleBase.eventNotConsumed;
        }
      }
    }
  
    StateC stateC = new StateC(this);

  
    
    
    private class StateD extends StateSimpleBase<MainState> {
      
      protected StateD(MainState superState) { super(superState, "StateD"); }

      //protected StateD(EventFollow enclosingState) { super(enclosingState); }
      
    
      @Override public int trans(Event ev) {
        return 0;
      }
    }

    StateD stateD = new StateD(this);

    
  }
  
  private final MainState state = new MainState();
  
  
}
