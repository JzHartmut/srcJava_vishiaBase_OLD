package org.vishia.util.sampleStateM;

import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventSource;
import org.vishia.util.EventThread;
import org.vishia.util.EventTimerMng;

public class EventFollow  {
  
  

  enum ENull{}
  
  boolean cond1, cond2;
  
  
  EventSource evSrc = new EventSource("TestEventFollow"){
    
  };

  
  EventConsumer evDst = new EventConsumer("TestEventFollow"){

    @Override protected boolean processEvent_(Event ev) {
      state.trans(ev);
      return true;
    }
  };
  
  private final EventThread evThread;

  
  EventFollow(EventThread eventThread){
    this.evThread = eventThread;
  }
  
  
  public void startState(){
    state.entry(0);
  }
  
  private class MainState extends StateTopBase<MainState>{
  
    protected MainState() { super("StateM"); }


    @Override public int entryDefault(){
      return start.entry(eventNotConsumed);
    }
  
    @Override public int entry(int consumed){
      //setState(EState.Null);
      start.entry(consumed);
      return consumed;
    }
    
  
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
      
      protected Start(MainState superState) { super(superState, "Start"); }


      //protected Start(EventFollow enclosingState) { super(enclosingState); }
  
      @Override public int entry(int consumed){
        super.entry(consumed);
        EventTimerMng.addTimeOrder(System.currentTimeMillis() + 1000, evDst, evThread);
        return consumed;
      }
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EventTimerMng.TimeEvent){
          EvX ev1 = new EvX(evSrc, evDst, evThread);
          cond1 = true;
          cond2 = true;
          ev1.sendEvent(EvX.Cmd.EvX);
          return exit().stateA.entry(StateSimpleBase.mEventConsumed);
        } else {
          return StateSimpleBase.stateCompleted;
        }
      }
    }
    
    Start start = new Start(this);

    private final class StateA extends StateSimpleBase<MainState> {
      
      protected StateA(MainState superState) { super(superState, "StateA"); }

      //protected StateA(EventFollow enclosingState) { super(enclosingState); }
  
      @Override public int entry(int consumed){
        super.entry(consumed);
        return consumed | StateSimpleBase.mRunToComplete;   //true because this state has condition transitions.
      }
      
      
      @Override public int trans(Event ev){
        if(cond1){
          return exit().stateB.entry(StateSimpleBase.eventNotConsumed);
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
          return exit().stateC.entry(StateSimpleBase.mEventConsumed);
        } else {
          return StateSimpleBase.stateCompleted;
        }
      }
    }
  
    StateB stateB = new StateB(this);

  
    private final class StateC extends StateSimpleBase<MainState> {
      
      protected StateC(MainState superState) { super(superState, "StateC"); }

      //protected StateC(EventFollow enclosingState) { super(enclosingState); }
  
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EvX && ev.getCmd() == EvX.Cmd.EvX){
          return exit().stateD.entry(StateSimpleBase.mEventConsumed);
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
        return StateSimpleBase.stateCompleted;
      }
    }

    StateD stateD = new StateD(this);

    
  }
  
  private final MainState state = new MainState();
  
  
}
