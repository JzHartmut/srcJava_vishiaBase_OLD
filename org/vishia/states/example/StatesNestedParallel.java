package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.states.StateParallel;
import org.vishia.states.StateComposite;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on;  
  
  
    boolean start1, start2, start3;
    
    boolean offAfterRunning;
    
    boolean cont;
  }
  
  
  enum CmdEvent { start, ready};
  
  class EventA extends Event<CmdEvent, Event.NoOpponent>{}
  
  Conditions cond = new Conditions();
  
  EventA event = new EventA();
  
  
  EventThread thread = new EventThread("thread");

  EventTimerMng timer = new EventTimerMng("timer");

  class States extends StateMachine
  {
    States(EventThread thread, EventTimerMng timer){ super(thread, timer); }

    class StateOff extends StateSimple
    {
      boolean isDefault;
      
      @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }
      
      //Trans ready = new Trans(StateWork.StateReady.class);
      
      //Trans history = new Trans(StateWork.StateActive.class);
      
      @Override protected Trans selectTrans(Event<?,?> ev) { return super.selectTrans(ev); }
      /*
      if(cond.on) {
          //choice
          if(cond.cont) return history;
          else return ready;
        } else return null;
      }
      */
      
      
      public Choice on = new Choice() { 
        
        @Override protected int check(Event<?,?> ev) {
          if(ev.getCmd() == CmdEvent.start) return mEventConsumed;
          else return 0;
        } 
        
        Trans cont = new Trans(StateWork.class){ 
          @Override protected int check(Event<?, ?> ev) {
            if(cond.cont) return mDeepHistory;
            else return 0;
          } 
        };
 
        Trans hist = new Trans(StateWork.StateReady.class){ 
          @Override protected int check(Event<?, ?> ev) {
             return mTransit;
          } 
        };
      };
      
      
      
      Trans on_Ready(EventA ev, Trans trans) {
        if(trans == null) return new Trans(StateWork.StateReady.class); 
        //Class<? extends StateSimple> dst = StateWork.StateReady.class;
        //int id = StateWork.class.hashCode();
        if(cond.on){
          trans.retTrans |= mEventConsumed;
          trans.doExit();
          
          trans.doEntry(ev);
        }
        return trans;
      }
      
    };
    
    
    class StateWork extends StateComposite
    {
      @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }

      class StateReady extends StateSimple
      {
        final boolean isDefault = true;
        
        @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
        
        @Override protected void exit(){ System.out.println(" exit " + stateId); }

        Trans start1 = new Trans(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class){ 
          @Override protected int check(Event<?, ?> ev) {
            if(cond.start1) { 
              retTrans = mEventConsumed;
              doExit();
              doEntry(ev);
              return retTrans;
            }
            else return 0;
          }
          
        };
        
        class Start2 extends Trans 
        { Start2(){ super(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class); } 
          
          @Override protected int check(Event<?, ?> ev) {
            if(cond.start2) {
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
        
        
        
        
        
        Trans start3(Event<?,?> ev, Trans transP){
          if(transP == null){
            return new Trans(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class);
          } 
          else if(cond.start3) {
            transP.retTrans =  mEventConsumed;
            transP.doExit();
            //no action
            transP.doEntry(ev);
            return transP;
          }
          else return null;
          
        }
        
        
      }
      
      
      class StateActive extends StateComposite
      {
        @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

        @Override protected void exit(){ System.out.println(" exit " + stateId); }

       class StateActive1 extends StateParallel
        {
          @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

          @Override protected void exit(){ System.out.println(" exit " + stateId); }

          class StateRunning extends StateSimple
          { final boolean isDefault = true;
            @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            Timeout timeout = new Timeout(5000, StateFinit.class) {
              @Override protected void action(Event<?,?> ev){
                System.err.println("timeout");
              }
            };
          }
          
          
          class StateFinit extends StateSimple
          { @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            Trans to_off(Event<?,?> ev, Trans trans) {
              if(trans == null) return new Trans(StateOff.class);
              else if(stateMachine.isInState(StateActive1.StateFinit.class) && stateMachine.isInState(StateActive2.StateShouldOff.class)) {
                return trans;
              } 
              else return null;
            }
            

          }
          
          //StateSimple stateFinit = new StateSimple(){
          //};
          
        } 
        
        
        class StateActive2 extends StateParallel
        { @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

          @Override protected void exit(){ System.out.println(" exit " + stateId); }

          class StateRemainOn extends StateSimple
          { final boolean isDefault = true;
          
            @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return mRunToComplete; }
  
            @Override protected void exit(){ System.out.println(" exit " + stateId); }
 
            Trans toShouldOff(Event<?,?> ev, Trans trans){
              if(trans == null) return new Trans(StateShouldOff.class);
              else if(cond.offAfterRunning) return trans;
              else return null;
            }
            
          }
          
          class StateShouldOff extends StateSimple
          {
            @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
            
            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            
          }
        }
        
      }//class StateActive
    }
  }
  
  
  final States states;
  
  
  private StatesNestedParallel(){
    states = new States(thread, timer);
  }
  
  private void executeCondions() {
    cond.on = true;
    cond.start3 = true;
    cond.offAfterRunning = true;
    if(event.occupy(null, states, null, true)){
      event.sendEvent(CmdEvent.start);
      //instead:
      //states.processEvent(event);
    }
    //cond.on = false;
    while(!states.isInState(States.StateOff.class)) {
      try{ Thread.sleep(100);
      } catch(InterruptedException exc) {}
    }
    try{
      timer.close();
      thread.close();
    } catch(Exception exc){}
  }
  
  
  
  public static void main(String[] args){
    StatesNestedParallel main = new StatesNestedParallel();
    main.executeCondions();
  }
  
}
