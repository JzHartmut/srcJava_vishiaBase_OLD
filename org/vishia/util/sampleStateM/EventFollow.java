package org.vishia.util.sampleStateM;

import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventSource;
import org.vishia.util.EventThread;
import org.vishia.util.EventTimerMng;
import org.vishia.util.StateBase;
import org.vishia.util.StateTopBase;

public class EventFollow  {
  
  enum EState{
    Null, Start, StateA, StateB, StateC, StateD, Statee
  }
  

  enum ENull{}
  
  boolean cond1, cond2;
  
  
  EventSource evSrc = new EventSource("TestEventFollow"){
    
  };

  
  EventConsumer evDst = new EventConsumer("TestEventFollow"){

    @Override public boolean processEvent(Event ev) {
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
  
  private static class MainState extends StateTopBase<EState, EventFollow>{
  
    protected MainState(EventFollow env) { super(EState.Null, env); }


    @Override public int entry(int consumed){
      //setState(EState.Null);
      start.entry(consumed);
      return consumed;
    }
    
  
    @Override protected int switchState(Event ev){
      int cont = StateBase.consumed + StateBase.complete;
      switch(stateNr()){
        case Null:          cont = start.entry(StateBase.runToComplete); break;
        case Start:         cont = start.trans(ev); break;
        case StateA:        cont = stateA.trans(ev); break;
        case StateB:        cont = stateB.trans(ev); break;
        case StateC:        cont = stateC.trans(ev); break;
        } 
      return cont;
    }

    
    private final static class Start extends StateBase<MainState, EState, EventFollow> {
      
      protected Start(MainState superState) { super(superState, EState.Start); }


      //protected Start(EventFollow enclosingState) { super(enclosingState); }
  
      @Override public int entry(int consumed){
        super.entry(consumed);
        EventTimerMng.addTimeOrder(System.currentTimeMillis() + 1000, env.evDst, env.evThread);
        return consumed;
      }
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EventTimerMng.TimeEvent){
          EvX ev1 = new EvX(env.evSrc, env.evDst, env.evThread);
          env.cond1 = true;
          env.cond2 = true;
          ev1.sendEvent(EvX.Cmd.EvX);
          return enclState.stateA.entry(StateBase.consumed);
        } else {
          return StateBase.complete;
        }
      }
    }
    
    Start start = new Start(this);

    private final static class StateA extends StateBase<MainState, EState, EventFollow> {
      
      protected StateA(MainState superState) { super(superState, EState.StateA); }

      //protected StateA(EventFollow enclosingState) { super(enclosingState); }
  
      @Override public int entry(int consumed){
        super.entry(consumed);
        return consumed | StateBase.runToComplete;   //true because this state has condition transitions.
      }
      
      
      @Override public int trans(Event ev){
        if(env.cond1){
          return enclState.stateB.entry(StateBase.notConsumed);
        } else {
          return 0;
        }
      }
    }
    
    StateA stateA = new StateA(this);

    private final static class StateB extends StateBase<MainState, EState, EventFollow> {
      
      protected StateB(MainState superState) { super(superState, EState.StateB); }
      
      //protected StateB(EventFollow enclosingState) { super(enclosingState); }
  
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EvX && ev.getCmd() == EvX.Cmd.EvX){
          return enclState.stateC.entry(StateBase.consumed);
        } else {
          return StateBase.complete;
        }
      }
    }
  
    StateB stateB = new StateB(this);

  
    private final static class StateC extends StateBase<MainState, EState, EventFollow> {
      
      protected StateC(MainState superState) { super(superState, EState.StateC); }

      //protected StateC(EventFollow enclosingState) { super(enclosingState); }
  
      
      
      @Override public int trans(Event ev){
        if(ev instanceof EvX && ev.getCmd() == EvX.Cmd.EvX){
          return enclState.stateD.entry(StateBase.consumed);
        } else {
          return StateBase.notConsumed;
        }
      }
    }
  
    StateC stateC = new StateC(this);

  
    
    
    private static class StateD extends StateBase<MainState, EState, EventFollow> {
      
      protected StateD(MainState superState) { super(superState, EState.StateD); }

      //protected StateD(EventFollow enclosingState) { super(enclosingState); }
      
    
      @Override public int trans(Event ev) {
        return StateBase.complete;
      }
    }

    StateD stateD = new StateD(this);

    
  }
  
  private final MainState state = new MainState(this);
  
  
}
