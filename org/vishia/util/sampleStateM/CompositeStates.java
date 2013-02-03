package org.vishia.util.sampleStateM;

import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateParallelBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventThread;

public class CompositeStates  extends EventConsumer {


  
  private class Idle extends StateSimpleBase<StateTop>{
    
    
    Idle(StateTop superState) { super(superState, "Idle"); }

    @Override public int trans(Event ev) {
      if(ev instanceof EvX && ((EvX)ev).getCmd() == EvX.Cmd.EvX){
        StateTop enclState1 = exit();
        int cont = enclState1.Work.C.C1.C1b.entry(mEventConsumed);
        cont |= enclState1.Work.C.C2.C2a.entry(mEventConsumed);
        return cont;
      }
      else return eventNotConsumed;
    }
  }
      

  private class A extends StateSimpleBase<Work>{
    
    
    A(Work superState) { super(superState, "A"); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int trans(Event ev) {
      return StateSimpleBase.stateCompleted;
    }
  }

  private static class B extends StateCompositeBase<B, Work>{
    
    B(Work superState){ super(superState, "B"); }
    
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }
    
    @Override public int entryDefault(){
      return B1.entry(eventNotConsumed);
    }
  
    /*
    @Override protected int switchState(Event ev) {
      int complete = 0;
      switch(stateNr()){
        case B1: complete = B1.trans(ev); break; 
        case B2: complete = B1.trans(ev); break; 
        case B3: complete = B1.trans(ev); break; 
      }
      return complete;
    }
    */

    private static class B1 extends StateSimpleBase<B>{
      
      B1(B superState) { super(superState, "B1"); }
      
      @Override public int trans(Event ev) {
        return StateSimpleBase.stateCompleted;
      }
    }
    B1 B1 = new B1(this);
    
    private static class B2 extends StateSimpleBase<B>{
      
      B2(B superState) { super(superState, "B2"); }
      
      @Override public int trans(Event ev) {
        return StateSimpleBase.stateCompleted;
      }
    }
    B2 B2 = new B2(this);
    
    
    private static class B3 extends StateSimpleBase<B>{
      
      B3(B superState) { super(superState, "B3"); }
      
      @Override public int trans(Event ev) {
        return StateSimpleBase.stateCompleted;
      }
    }
    B3 B3 = new B3(this);
    

  
  }

  
  private class C1a extends StateSimpleBase<C1>{
    
    C1a(C1 superState){ super(superState, "C1a"); }

  
    @Override public int trans(Event ev) {
      return eventNotConsumed;
    }
  }
          
  private class C1b extends StateSimpleBase<C1>{
    
    C1b(C1 superState){ super(superState, "C1b"); }

  
    @Override public int trans(Event ev) {
      if(ev instanceof EvX && ((EvX)ev).getCmd() == EvX.Cmd.EvZ){
        return exit().C1d.entry(mEventConsumed);
      }
      return StateSimpleBase.eventNotConsumed;
    }
  }

  
  private class C1c extends StateSimpleBase<C1>{
    
    C1c(C1 superState){ super(superState, "C1c"); }

  
    @Override public int trans(Event ev) {
      return StateSimpleBase.eventNotConsumed;
    }
  }

  
  private class C1d extends StateSimpleBase<C1>{
    
    C1d(C1 superState){ super(superState, "C1d"); }

  
    @Override public int trans(Event ev) {
      return StateSimpleBase.eventNotConsumed;
    }
  }

  
  private class C1 extends StateCompositeBase<C1, C> { //StateComboBase<C, EWork, EState_C1, CompositeStates> {

    C1(C superState){ super(superState, "C1"); } // super(superState, EWork.C, EState_C1.Null); }
    
    @Override public int entryDefault(){
      return C1a.entry(eventNotConsumed);
    }
  
    @Override public int entry(int consumed) {
      super.entry(consumed);
      return consumed;
    }
  
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }
    C1a C1a = new C1a(this);
    C1b C1b = new C1b(this);
    C1c C1c = new C1c(this);
    C1d C1d = new C1d(this);
            
  }


  
  private static class C2a extends StateSimpleBase<C2>{
    
    C2a(C2 superState){ super(superState, "C2a"); }

  
    @Override public int trans(Event ev) {
      if(ev instanceof EvX && ((EvX)ev).getCmd() == EvX.Cmd.EvX){
        return exit().C2b.entry(mEventConsumed);
      }
      //
      return 0;
    }
  }

  
  
  private static class C2b extends StateSimpleBase<C2>{
    
    C2b(C2 superState){ super(superState, "C2b"); }

  
    @Override public int trans(Event ev) {
      return StateSimpleBase.stateCompleted;
    }
  }


  private class C2 extends StateCompositeBase<C2, C> {

    C2(C superState){ super(superState, "C2"); }
    
    @Override public int entryDefault(){
      return C2a.entry(eventNotConsumed);
    }
  
    @Override public int entry(int consumed) {
      super.entry(consumed);
      return consumed;
    }
  
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }
    
    
    C2a C2a = new C2a(this);
    C2b C2b = new C2b(this);
    
  }
  
            
    
    
  private class C extends StateParallelBase<C, Work> { //StateCombo<Work, EState_C1>{
    
    //final Work derivedEnclState;
    
    
    
    C2 C2 = new C2(this);
    C1 C1 = new C1(this);

    C(Work enclState){ 
      super(enclState, "C"); 
      //derivedEnclState = enclState;
      addState(C1);
      addState(C2);
    }
    
    

  
    @Override public int entry(int consumed) {
      super.entry(consumed);
      
      return consumed;
    }
  
    public int entry(int consumed, StateSimpleBase c1, StateSimpleBase c2) {
      super.entry(consumed);
      //setState(EWork.C);
      //C1.entry(consumed);
      C1.C1a.entry(consumed);
      C2.C2a.entry(consumed);
      
      return consumed;
    }
  
    /**First the event is checked whether it should switch the state itself.
     * Because this state has 2 parallel combined states intern it calls the {@link #trans(Event)}
     * of both parallel states with the given event.
     * If 
     * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.util.Event)
     */
    @Override public int trans(Event ev) {
      
      if(C1.isInState(C1.C1d) && C2.isInState(C2.C2a)) {
        Work enclState1 = exit();
        enclState1.entry(mEventConsumed);
        enclState1.B.B3.entry(mEventConsumed);
      }
      return StateSimpleBase.stateCompleted;
      
    }



    
            
  }

  private class Work extends StateCompositeBase<Work, StateTop>{

    protected Work(StateTop superState) {
      super(superState, "Work");
    }

    @Override public int entryDefault(){
      return B.entry(eventNotConsumed);
    }
  
    @Override public int trans(Event ev){
      return eventNotConsumed;
    }

    /*
    @Override public int switchState(Event ev) {
      switch(stateNr()){
        case A: break;
      }
      // TODO Auto-generated method stub
      return 0;
    }
    */
    
    C C = new C(this);
          
    B B = new B(this);
        
    A A = new A(this);
        

  
  }

  
  private class StateTop extends StateTopBase<StateTop>{

    protected StateTop() {
      super("StateTop");
    }

    @Override public int entryDefault(){
      return Idle.entry(eventNotConsumed);
    }
  
    @Override public int entry(int consumed){
      Work.entry(0);
      return consumed;
    }

    
    /*
    @Override public int switchState(Event ev) {
      int cont = 0;
      switch(stateNr()){
        case Null: cont = Idle.entry(runToComplete | notConsumed); break;
        case Idle: cont = Idle.trans(ev); break;
        case Work: cont = Work.process(ev); break;
      }
      return cont;
    }
    */
    ///

    
    Idle Idle = new Idle(this);
    Work Work = new Work(this);
  }
  
  
  StateTop state = new StateTop();
  
    

        
  
  
  
          
      
  
  
  private final EventThread evThread;

  
  CompositeStates(EventThread eventThread){
    super("CompositeStates");
    this.evThread = eventThread;
  }
  

  
  @Override protected boolean processEvent_(Event ev) {
    state.process(ev);
    return true;
  }
  
  

  
}
