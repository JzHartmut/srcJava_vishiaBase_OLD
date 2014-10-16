package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.states.StateAction;
import org.vishia.states.StateAddParallel;
import org.vishia.states.StateComposite;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on_ready;  
  
  
    boolean start;
    
    boolean offAfterRunning;
  }
  
  
  Conditions cond = new Conditions();
  
  Event<?,?> event = new Event();
  
  class States extends StateMachine
  {
    States(EventThread thread, EventTimerMng timer){ super(thread, timer); }

    class StateOff extends StateSimple
    {
      @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }
      
      StateTrans start = new StateTrans("start", StateWork.StateActive.StateActive1.StateRunning.class, StateWork.StateActive.StateActive2.StateRemainOn.class){ 
        { condition = new StateAction() {
            @Override public int action(Event<?, ?> ev) {
              if(cond.start){ 
                cond.start = false;
                return mTransit;
              }
              else return 0;
            };
          };
      } };

      
      StateTrans on_Ready = new StateTrans("on_Ready", StateWork.StateReady.class){ 
        @Override protected int trans(Event<?, ?> ev) {
        if(cond.on_ready){
          doExit();
          //no action
          return doEntry(ev) | mEventConsumed;
          //return switchto(ev, StateWork.StateReady.class);
        }
        else return 0;
      } };
      
      
      
      /*
      StateTrans testChoice = new StateTrans("testChoice"){
        @Override protected int trans(Event<?, ?> ev) {
          return 0;
        }        
        
        Choice choice1 = new Choice(StateWork.StateReady.class) {
          {
            condition = new Runnable(){@Override public void run()
            { }};
          }
        };
        
        
      };
      */
      
      
      StateTrans on_Ready(Event<?, ?> ev, StateTrans trans) {
        if(trans == null) return new StateTrans("on_Ready", StateWork.StateReady.class); 
        //Class<? extends StateSimple> dst = StateWork.StateReady.class;
        //int id = StateWork.class.hashCode();
        if(cond.on_ready){
          trans.doExit();
          trans.doEntry(ev);
        }
        return trans;
      }
      
    }
    
    
    class StateWork extends StateComposite
    {
      @Override protected int entry(Event<?,?> ev){ System.out.println("entry " + stateId); return 0; }

      class StateReady extends StateSimple
      {
        
        @Override public int entry(Event<?,?> ev){
          System.out.println("entry Ready");
          return 0;
        }
        
        StateTrans start = new StateTrans("start", StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class){ 
          @Override protected int trans(Event<?, ?> ev) {
            if(cond.start) {
              doExit();
              //no action
              return doEntry(ev) | mEventConsumed;
            }
            else return 0;
          }
          
        };
        
        class Start extends StateTrans {
          
          Start(){ super("Start", StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class); } 
          
          @Override protected int trans(Event<?, ?> ev) {
            if(cond.start) {
              doExit();
              //no action
              return doEntry(ev) | mEventConsumed;
            }
            else return 0;
          }
          
          int action(Event<?,?>ev){
            return 0;
          }
        } //class Start
        
        StateTrans start2(Event<?,?> ev, StateTrans transP){
          if(transP == null){
            return new StateTrans("start2", StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class);
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

            Timeout timeout = new Timeout(5000, StateFinit.class);
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
    cond.on_ready = true;
    cond.start = true;
    cond.offAfterRunning = true;
    event.occupy(null, true);
    states.applyEvent(event);
    cond.on_ready = false;
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
