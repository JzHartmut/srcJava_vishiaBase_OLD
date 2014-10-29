package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.states.StateAddParallel;
import org.vishia.states.StateComposite;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on;  
  
  
    boolean start;
    
    boolean offAfterRunning;
    
    boolean cont;
  }
  
  
  Conditions cond = new Conditions();
  
  Event<?,?> event = new Event();
  
  class States extends StateMachine
  {
    States(EventThread thread, EventTimerMng timer){ super(thread, timer); }

    class StateOff extends StateSimple
    {
      @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
      
      StateTrans ready = new StateTrans(StateWork.StateReady.class);
      
      StateTrans history = new StateTrans(StateWork.StateActive.class);
      
      @Override protected StateTrans selectTrans(Event<?,?> ev) { return super.selectTrans(ev); }
      /*
      if(cond.on) {
          //choice
          if(cond.cont) return history;
          else return ready;
        } else return null;
      }
      */
      
      
      public StateTrans on = new StateTrans() { 
        @Override protected int condition(Event<?, ?> ev) {
          if(cond.on) return mEventConsumed;
          else return 0;
        } 
        
        StateTrans cont = new StateTrans(StateWork.StateReady.class){ 
          @Override protected int condition(Event<?, ?> ev) {
            if(cond.cont) return mEventConsumed;
            else return 0;
          } 
        };
 
        StateTrans hist = new StateTrans(StateWork.StateReady.class){ 
          @Override protected int condition(Event<?, ?> ev) {
             return mEventConsumed;
          } 
        };
      };
      
      
      
      StateTrans on_Ready(Event<?, ?> ev, StateTrans trans) {
        if(trans == null) return new StateTrans(StateWork.StateReady.class); 
        //Class<? extends StateSimple> dst = StateWork.StateReady.class;
        //int id = StateWork.class.hashCode();
        if(cond.on){
          trans.doExit();
          trans.doEntry(ev);
        }
        return trans;
      }
      
    };
    
    
    class StateWork extends StateComposite
    {
      @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

      class StateReady extends StateSimple
      {
        
        @Override public int entry(Event<?,?> ev){
          System.out.println("entry Ready");
          return 0;
        }
        
        StateTrans start = new StateTrans(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class){ 
          @Override protected int trans(Event<?, ?> ev) {
            if(cond.start) { 
              retTrans = mEventConsumed;
              doExit();
              doEntry(ev);
              return retTrans;
            }
            else return 0;
          }
          
        };
        
        class Start extends StateTrans 
        { Start(){ super(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class); } 
          
          @Override protected int trans(Event<?, ?> ev) {
            if(cond.start) {
              retTrans = mEventConsumed;
              doExit();
              doEntry(ev);
              return retTrans;
            }
            else return 0;
          }
          
          @Override protected void action(Event<?,?>ev){
          }
        } //class Start
        
        
        
        
        
        StateTrans start2(Event<?,?> ev, StateTrans transP){
          if(transP == null){
            return new StateTrans(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class);
          } 
          if(cond.start) {
            transP.doExit();
            //no action
            transP.doEntry(ev);
            transP.retTrans =  mEventConsumed;
            return transP;
          }
          else return null;
          
        }
        
        
      }
      
      
      class StateActive extends StateParallel
      {
        @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

        class StateActive1 extends StateAddParallel
        {
          @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

          class StateRunning extends StateSimple
          { @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

            Timeout timeout = new Timeout(5000, StateFinit.class) {
              @Override protected void action(Event<?,?> ev){
                System.err.println("timeout");
              }
            };
          }
          
          
          class StateFinit extends StateSimple
          {  @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

            
          }
          
          //StateSimple stateFinit = new StateSimple(){
          //};
          
        } 
        
        
        class StateActive2 extends StateAddParallel
        { @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

          class StateRemainOn extends StateSimple
          {
            @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
  
          }
          
          class StateShouldOff extends StateSimple
          {
            @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
            
          }
        }
      }
    }
  }
  
  
  final States states;
  
  
  private StatesNestedParallel(){
    EventThread thread = new EventThread("thread");
    EventTimerMng timer = new EventTimerMng("timer");
    states = new States(thread, timer);
  }
  
  private void executeCondions() {
    cond.on = true;
    cond.start = true;
    cond.offAfterRunning = true;
    event.occupy(null, true);
    states.applyEvent(event);
    //cond.on = false;
    while(!states.isInState(States.StateOff.class)) {
      try{ Thread.sleep(100);
      } catch(InterruptedException exc) {}
    }
  }
  
  
  
  public static void main(String[] args){
    StatesNestedParallel main = new StatesNestedParallel();
    main.executeCondions();
  }
  
}
