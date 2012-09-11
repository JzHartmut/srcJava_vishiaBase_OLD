package org.vishia.util.sampleStateM;

import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventThread;
import org.vishia.util.StateBase;
import org.vishia.util.StateComboBase;
import org.vishia.util.StateTopBase;

public class CompositeStates  extends EventConsumer {


  /**enum for state definitions. */
  enum EMainState {Null, Idle, Work };

  enum EWork {Null, A, B, C };

  enum EState_B{Null, B1, B2, B3};
  
  enum EState_C1{Null, C1a, C1b};
  
  enum EState_C2{Null, C2a, C2b};
  
  /**class type definition of generic type of StateComboBase for StateTop. */
  private abstract static class StateTop_ComboState extends StateTopBase<EMainState, CompositeStates>{
    protected StateTop_ComboState(EMainState id, CompositeStates env) {
      super(EMainState.Null, env);
    }
  }
  
  ///
  /**class type definition of generic type of StateBase for StateTop. */
  private abstract static class StateTop_State extends StateBase<StateTop, EMainState, CompositeStates>{
    protected StateTop_State(StateTop superState, EMainState stateId) {
      super(superState, stateId);
    }
  }

  
  private static class Idle extends StateTop_State{
    
    
    Idle(StateTop superState) { super(superState, EMainState.Idle); }

    @Override public int trans(Event ev) {
      if(ev instanceof EvX && ((EvX)ev).getCmd() == EvX.Cmd.EvX){
        int cont = enclState.Work.C.C1.C1a.entry(consumed);
        cont |= enclState.Work.C.C1.C1a.entry(consumed);
        return cont;
      }
      else return notConsumed;
    }
  }
      

  private static class A extends StateBase<Work, EWork, CompositeStates>{
    
    
    A(Work superState) { super(superState, EWork.A); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int trans(Event ev) {
      return StateBase.complete;
    }
  }

  private static class B extends StateComboBase<Work, EWork, EState_B, CompositeStates>{
    
    B(Work superState){ super(superState, EWork.B, EState_B.Null); }
    
    @Override public int trans(Event ev){
      return notConsumed;
    }
  
    @Override protected int switchState(Event ev) {
      int complete = 0;
      switch(stateNr()){
        case B1: complete = B1.trans(ev); break; 
        case B2: complete = B1.trans(ev); break; 
        case B3: complete = B1.trans(ev); break; 
      }
      return complete;
    }

    private static class B1 extends StateBase<B, EState_B, CompositeStates>{
      
      B1(B superState) { super(superState, EState_B.B1); }
      
      @Override public int trans(Event ev) {
        return StateBase.complete;
      }
    }
    B1 B1 = new B1(this);
    
    private static class B2 extends StateBase<B, EState_B, CompositeStates>{
      
      B2(B superState) { super(superState, EState_B.B2); }
      
      @Override public int trans(Event ev) {
        return StateBase.complete;
      }
    }
    B2 B2 = new B2(this);
    
    
    private static class B3 extends StateBase<B, EState_B, CompositeStates>{
      
      B3(B superState) { super(superState, EState_B.B3); }
      
      @Override public int trans(Event ev) {
        return StateBase.complete;
      }
    }
    B3 B3 = new B3(this);
    

  
  }
  
  private static class C extends StateComboBase<Work, EWork, EWork, CompositeStates> { //StateCombo<Work, EState_C1>{
    
    C(Work superState){ super(superState, EWork.C, EWork.Null); }
    
    
    @Override public int entry(int consumed) {
      super.entry(consumed);
      C1.setStateNr(EState_C1.Null);
      C2.setStateNr(EState_C2.Null);
      //C1.C1a.entry(consumed);
      //C2.C2a.entry(consumed);
      
      return consumed;
    }
  
    public int entry(int consumed, StateBase c1, StateBase c2) {
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
     * @see org.vishia.util.StateBase#trans(org.vishia.util.Event)
     */
    @Override public int trans(Event ev) {
      
      C1.trans(ev);
      C2.trans(ev);
      if(ev instanceof EvX && C1.stateNr() == EState_C1.C1b  && C2.stateNr() == EState_C2.C2b){
        exit();
        enclState.B.entry(consumed);
        enclState.B.B3.entry(consumed);
      }
      return StateBase.complete;
      
    }
    
    
    @Override protected int switchState(Event ev) {
      return StateBase.complete;
    }

    
    private abstract static class C_States extends StateComboBase<C, EWork, EState_C1, CompositeStates> {
      C_States(C superState){ super(superState, EWork.C, EState_C1.Null); }
      
    }

    
    
    
    private static class C1 extends C_States { //StateComboBase<C, EWork, EState_C1, CompositeStates> {

      C1(C superState){ super(superState); } // super(superState, EWork.C, EState_C1.Null); }
      
      @Override public int entry(int consumed) {
        super.entry(consumed);
        return consumed;
      }
    
      @Override public int trans(Event ev){
        return notConsumed;
      }
      @Override protected int switchState(Event ev) {
        return StateBase.complete;
      }
      private class C1a extends StateBase<C1, EState_C1, CompositeStates>{
        
        C1a(C1 superState){ super(superState, EState_C1.C1a); }

      
        @Override public int trans(Event ev) {
          return StateBase.complete;
        }
      }
      C1a C1a = new C1a(this);
              
      private static class C1b extends StateBase<C1, EState_C1, CompositeStates>{
        
        C1b(C1 superState){ super(superState, EState_C1.C1b); }

      
        @Override public int trans(Event ev) {
          return StateBase.complete;
        }
      }
      C1b C1b = new C1b(this);
              
    }

    private static class C2 extends StateComboBase<C, EWork, EState_C2, CompositeStates> {

      C2(C superState){ super(superState, EWork.C, EState_C2.Null); }
      
      @Override public int entry(int consumed) {
        super.entry(consumed);
        return consumed;
      }
    
      @Override public int trans(Event ev){
        return notConsumed;
      }
      @Override protected int switchState(Event ev) {
        return StateBase.complete;
      }
      
      
      private static class C2a extends StateBase<C2, EState_C2, CompositeStates>{
        
        C2a(C2 superState){ super(superState, EState_C2.C2a); }

      
        @Override public int trans(Event ev) {
          return StateBase.complete;
        }
      }
      C2a C2a = new C2a(this);
              
      
      
      private static class C2b extends StateBase<C2, EState_C2, CompositeStates>{
        
        C2b(C2 superState){ super(superState, EState_C2.C2b); }

      
        @Override public int trans(Event ev) {
          return StateBase.complete;
        }
      }
      C2b C2b = new C2b(this);
              
    }
    C2 C2 = new C2(this);
    C1 C1 = new C1(this);
            
  }

  private static class Work extends StateComboBase<StateTop, EMainState, EWork, CompositeStates>{

    protected Work(StateTop superState) {
      super(superState, EMainState.Work, EWork.Null);
    }

    @Override public int trans(Event ev){
      return notConsumed;
    }

    @Override public int switchState(Event ev) {
      switch(stateNr()){
        case A: break;
      }
      // TODO Auto-generated method stub
      return 0;
    }

    
    C C = new C(this);
          
    B B = new B(this);
        
    A A = new A(this);
        

  
  }

  
  private static class StateTop extends StateTop_ComboState{

    protected StateTop(CompositeStates env) {
      super(EMainState.Null, env);
    }

    @Override public int entry(int consumed){
      Work.entry(0);
      return consumed;
    }

    @Override public int switchState(Event ev) {
      int cont = 0;
      switch(stateNr()){
        case Null: cont = Idle.entry(runToComplete | notConsumed); break;
        case Idle: cont = Idle.trans(ev); break;
        case Work: cont = Work.process(ev); break;
      }
      return cont;
    }

    ///

    
    Idle Idle = new Idle(this);
    Work Work = new Work(this);
  }
  
  
  StateTop state = new StateTop(this);
  
    

        
  
  
  
          
      
  
  
  private final EventThread evThread;

  
  CompositeStates(EventThread eventThread){
    super("CompositeStates");
    this.evThread = eventThread;
  }
  

  
  @Override public boolean processEvent(Event ev) {
    state.process(ev);
    return true;
  }
  
  

  
}
