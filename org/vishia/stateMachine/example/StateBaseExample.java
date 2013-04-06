package org.vishia.stateMachine.example;

import org.vishia.stateMachine.StateAdditionalParallelBase;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventSource;

public class StateBaseExample {


  private final int delay = 10;
  
  /**For cyclic call. */
  private long time = -1;
  
  
  public final EventConsumer processState = new EventConsumer("StateBaseExample"){

    @Override protected boolean processEvent_(Event<?,?> ev) {
      stateTop.process(ev);
      return true;
    }
    
  };
  
  
  public final EventSource eventSource = new EventSource("StateBaseExample"){
    
  };
  
  enum Cmd{ Null, step, on_ready, on_cont, off, offAfterRunning, start, stop, abort};
  
  public class EventBaseExample extends Event<Cmd, Event.NoOpponent>{
    public EventBaseExample() {
      super(eventSource, StateBaseExample.this, processState, null);
    }
  }
  
  private class StateTop extends StateTopBase<StateTop>{

    protected StateTop() {
      super("StateTop");
    }

    @Override public int entryDefault(){
      return stateOff.entry(eventNotConsumed);
    }
  
    @Override public int entry(int consumed){
      super.entry(0);
      //Work.entry(0);
      return consumed;
    }

    @Override public int process(Event<?,?> evP){
      return super.process(evP);
    }
    
    
    StateOff stateOff = new StateOff(this);
    StateWork stateWork = new StateWork(this);
  }
  
  
  StateTop stateTop = new StateTop();
  

  
  
  private class StateOff extends StateSimpleBase<StateTop>{

    protected StateOff(StateTop superState) {
      super(superState, "Off");
    }

    @Override public int trans(Event<?,?> evP) {
      EventBaseExample ev = (EventBaseExample)evP;
      Cmd cmd = ev == null ? Cmd.Null: ev.getCmd();
      if(cmd == Cmd.on_cont){
        return exit().stateWork.entryDeepHistory(mEventConsumed);
      }
      else if(cmd == Cmd.on_ready){
        StateTop stateEncl = exit();
        return stateEncl.stateWork.entry(mEventConsumed)
              | stateEncl.stateWork.stateReady.entry(mEventConsumed);
      }
      else return 0;
    }
    
  }

  
  private class StateWork extends StateCompositeBase<StateWork, StateTop>{

    protected StateWork(StateTop superState) {
      super(superState, "Work");
    }

    @Override public int entryDefault(){
      return stateReady.entry(0);
    }
  
    @Override public int trans(Event ev){
      return 0;
    }

    @Override public StateTop exit(){
      stateActive2.exit();
      return super.exit();
    }
    
    StateReady stateReady = new StateReady(this);
    //StateActive stateActive = new StateActive(this);
    StateRunningW stateRunning = new StateRunningW(this);
    StateFinitW stateFinit = new StateFinitW(this);
    StateActive2W stateActive2 = new StateActive2W(this);
  }

  
  private class StateReady extends StateSimpleBase<StateWork>{

    protected StateReady(StateWork superState) {
      super(superState, "Ready");
    }

    @Override public int trans(Event<?,?> evP) {
      EventBaseExample ev = (EventBaseExample)evP;
      Cmd cmd = ev == null ? Cmd.Null: ev.getCmd();
      if(cmd == Cmd.start){
        StateWork enclState = exit();
        return enclState.stateActive2.entry(mEventConsumed)
              | enclState.stateRunning.entry(mEventConsumed);
      }
      return 0;
    }
    
  }
  

  private class StateRunningW extends StateSimpleBase<StateWork>{

    StateRunningW(StateWork superState) {
      super(superState, "Running");
    }
    
    @Override public int entry(int cont){
      super.entry(0);
      time = System.currentTimeMillis() + delay;
      return 0;
    }

    @Override public int trans(Event<?,?> ev) {
      //call trans of its parallel states
      int evConsumedInParallel = enclState().stateActive2.process(ev);
      if((evConsumedInParallel & mStateLeaved) !=0) return evConsumedInParallel;
      else{
        if(System.currentTimeMillis() - time >=0){
          return evConsumedInParallel | exit().stateFinit.entry(mRunToComplete);
        }
        else return evConsumedInParallel;
      }
    }
    
  }
  
  private class StateFinitW extends StateSimpleBase<StateWork>{

    StateFinitW(StateWork superState) {
      super(superState, "Finit");
    }

    @Override public int trans(Event<?,?> ev) {
      int evConsumedInParallel = enclState().stateActive2.process(ev);
      if((evConsumedInParallel | mStateLeaved) !=0) return evConsumedInParallel;
      else{
        if(stateTop.stateWork.stateActive2.stateRemainOn.isInState()){
          return evConsumedInParallel | exit().stateReady.entry(eventNotConsumed);
        }
        else if(stateTop.stateWork.stateActive2.stateShouldOff.isInState()){
          return evConsumedInParallel | exit().exit().stateOff.entry(eventNotConsumed);
        }
        
        else return 0;
      }
    }
    
  }
  
  
  
  private class StateActive extends StateParallelBase<StateActive, StateWork> { //StateCombo<Work, EState_C1>{
    
    StateActive(StateWork enclState){ 
      super(enclState, "Active"); 
      addState(stateActive1);
      addState(stateActive2);
    }
    
    @Override public int entry(int consumed) {
      super.entry(consumed);
      
      return consumed;
    }
  
    public int entry(int consumed, StateSimpleBase c1, StateSimpleBase c2) {
      super.entry(consumed);
      
      return consumed;
    }
  
    /**First the event is checked whether it should switch the state itself.
     * Because this state has 2 parallel combined states intern it calls the {@link #trans(Event)}
     * of both parallel states with the given event.
     * If 
     * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.util.Event)
     */
    @Override public int trans(Event ev) {
      
      return StateSimpleBase.stateCompleted;
      
    }
    
    StateActive1 stateActive1 = new StateActive1(this); 
    StateActive2 stateActive2 = new StateActive2(this); 

  }

  
  private class StateActive1 extends StateCompositeBase<StateActive1, StateActive>{

    protected StateActive1(StateActive superState) {
      super(superState, "Active1");
    }

    @Override public int entryDefault(){
      return stateRunning.entry(0);
    }
  
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }

    
    StateRunning stateRunning = new StateRunning(this);
    StateFinit stateFinit = new StateFinit(this);

  
  }


  
  private class StateRunning extends StateSimpleBase<StateActive1>{

    StateRunning(StateActive1 superState) {
      super(superState, "Running");
    }
    
    @Override public int entry(int cont){
      super.entry(0);
      time = System.currentTimeMillis() + delay;
      return 0;
    }

    @Override public int trans(Event<?,?> ev) {
      if(System.currentTimeMillis() - time >=0){
        return exit().stateFinit.entry(mRunToComplete);
      }
      else return 0;
    }
    
  }
  
  private class StateFinit extends StateSimpleBase<StateActive1>{

    StateFinit(StateActive1 superState) {
      super(superState, "Finit");
    }

    @Override public int trans(Event<?,?> ev) {
      if(enclState().enclState().stateActive2.stateRemainOn.isInState()){
        exit().exit().exit().stateReady.entry(mStateLeaved);
      }
      else if(enclState().enclState().stateActive2.stateShouldOff.isInState()){
        exit().exit().exit().exit().stateOff.entry(mStateLeaved);
      }
      
      return 0;
    }
    
  }
  
  
  
  private class StateActive2 extends StateCompositeBase<StateActive2, StateActive>{

    protected StateActive2(StateActive superState) {
      super(superState, "Active2");
    }

    @Override public int entryDefault(){
      return stateRemainOn.entry(0);
    }
  
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }

    
    StateRemainOn stateRemainOn = new StateRemainOn(this);
    StateShouldOff stateShouldOff = new StateShouldOff(this);

  
  }


  
  private class StateRemainOn extends StateSimpleBase<StateActive2>{

    StateRemainOn(StateActive2 superState) {
      super(superState, "RemainOn");
    }

    @Override public int entry(int consumed){
      return super.entry(consumed);
    }
    
    @Override public int trans(Event<?,?> evP) {
      EventBaseExample ev = (EventBaseExample)evP;
      Cmd cmd = ev == null ? Cmd.Null: ev.getCmd();
      if(cmd == Cmd.abort){
        return exit().exit().exit().exit().stateOff.entry(mEventConsumed | mStateLeaved);
      }
      else return 0;
    }
    
  }
  
  private class StateShouldOff extends StateSimpleBase<StateActive2>{

    StateShouldOff(StateActive2 superState) {
      super(superState, "ShouldOff");
    }

    @Override public int trans(Event<?,?> ev) {
      return 0;
    }
    
  }
  
  
  private class StateActive2W extends StateAdditionalParallelBase<StateActive2W, StateWork>{

    protected StateActive2W(StateWork superState) {
      super(superState, "Active2");
    }

    @Override public int entry(int xx){
      return super.entry(xx);
    }
    
    
    @Override public int entryDefault(){
      return stateRemainOn.entry(0);
    }
  
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }

    
    StateRemainOnW stateRemainOn = new StateRemainOnW(this);
    StateShouldOffW stateShouldOff = new StateShouldOffW(this);

  
  }


  
  private class StateRemainOnW extends StateSimpleBase<StateActive2W>{

    StateRemainOnW(StateActive2W superState) {
      super(superState, "RemainOn");
    }

    @Override public int trans(Event<?,?> evP) {
      EventBaseExample ev = (EventBaseExample)evP;
      Cmd cmd = ev == null ? Cmd.Null: ev.getCmd();
      if(cmd == Cmd.offAfterRunning){
        return exit().stateShouldOff.entry(mEventConsumed);
      }
      else return 0;
    }
    
  }
  
  private class StateShouldOffW extends StateSimpleBase<StateActive2W>{

    StateShouldOffW(StateActive2W superState) {
      super(superState, "ShouldOff");
    }

    @Override public int trans(Event<?,?> evP) {
      EventBaseExample ev = (EventBaseExample)evP;
      Cmd cmd = ev == null ? Cmd.Null: ev.getCmd();
      if(cmd == Cmd.abort){
        return exit().exit().exit().stateOff.entry(mEventConsumed | mStateLeaved);
      }
      else return 0;
    }
    
  }
  
  
  private void execute(){
    for(int ii=0; ii < 100; ++ii){
      if(time >=0){ time -=1; }
      //stimuli
      EventBaseExample ev = new EventBaseExample();
      switch(ii){
        case 1: ev.sendEvent(Cmd.on_cont); break;
        case 3: ev.sendEvent(Cmd.start); break;
        case 5: ev.sendEvent(Cmd.offAfterRunning); break;
        case 7: ev.sendEvent(Cmd.abort); break;
        default: ev.sendEvent(Cmd.step);
      }
      try{
        synchronized(this){ wait(50); }
      } catch(InterruptedException exc){
        System.err.println("StateBaseExample - wait interrupted;");
      }
    }
  }
  
  
  public static final void main(String[] args){
    StateBaseExample main = new StateBaseExample();
    main.execute();
  }
  
  
  
}
