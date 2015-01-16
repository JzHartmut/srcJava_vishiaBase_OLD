package org.vishia.states.example;

import java.util.EventObject;

import org.vishia.event.EventCmdPingPongType;
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
  
  
  /**Commands for the event.
   */
  enum CmdEvent { start, ready, cyclic};
  
  /**An event type reuseable for the state machine animation. */
  class EventA extends EventCmdPingPongType<CmdEvent, EventCmdPingPongType.NoOpponent>{}
  
  /**Some conditions for transition in this example. */
  Conditions cond = new Conditions();
  
  /**An event instance reused for the state machine animation.*/
  EventA event = new EventA();
  
  
  /**The thread to execute the state machine. */
  EventThread thread = new EventThread("thread");

  /**Timer organisation. */
  //EventTimerMng timer = new EventTimerMng("timer");

  /**The state machine. It is an inner class because it should not contain any other things as states. This class is analysed by reflection
   * to complete the data for state machine execution. 
   */
  class States extends StateMachine
  {
    States(EventThread thread){ super("ExampleNestedParallel", thread); }

    class StateOff extends StateSimple
    {
      boolean isDefault;
      
      @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }
      
      //Trans ready = new Trans(StateWork.StateReady.class);
      
      //Trans history = new Trans(StateWork.StateActive.class);
      
      @Override protected Trans selectTrans(EventObject ev) { return super.selectTrans(ev); }
      /*
      if(cond.on) {
          //choice
          if(cond.cont) return history;
          else return ready;
        } else return null;
      }
      */
      
      
      public Choice on = new Choice() { 
        
        @Override protected void check(EventObject ev) {
          if(  ev instanceof EventA 
            && ((EventA)ev).getCmd() == CmdEvent.start) { 
            retTrans = mEventConsumed;
          }
        } 
        
        Trans cont = new Trans(StateWork.class){ 
          @Override protected void check(EventObject ev) {
            if(cond.cont) retTrans = mDeepHistory;
          } 
        };
 
        Trans hist = new Trans(StateWork.StateReady.class){ 
          @Override protected void check(EventObject ev) {
             retTrans = mTransit;
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
      @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }

      class StateReady extends StateSimple
      {
        final boolean isDefault = true;
        
        @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
        
        @Override protected void exit(){ System.out.println(" exit " + stateId); }

        Trans start1 = new Trans(1, StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class){ 
          @Override protected void check(EventObject ev) {
            if(cond.start1) { 
              retTrans = mEventConsumed;
              doExit();
              doEntry(ev);
            }
          }
          
        };
        
        class Start2 extends Trans 
        { Start2(){ super(3, StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class); } 
          
          @Override protected void check(EventObject ev) {
            if(cond.start2) {
              retTrans = mEventConsumed;
              doExit();
              doEntry(ev);
            }
          }
          
          @Override protected void action(EventObject ev){
          }
        } //class Start
        
        
        
        
        
        Trans start3(EventObject ev, Trans transP){
          if(transP == null){
            return new Trans(2, StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class);
          } 
          else if(cond.start3) {
            transP.retTrans =  StateSimple.mEventConsumed;
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
        @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

        @Override protected void exit(){ System.out.println(" exit " + stateId); }

       class StateActive1 extends StateParallel
        {
          @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

          @Override protected void exit(){ System.out.println(" exit " + stateId); }

          class StateRunning extends StateSimple
          { final boolean isDefault = true;
            @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            Timeout timeout = new Timeout(5000, StateFinit.class) {
              @Override protected void action(EventObject ev){
                System.err.println("timeout");
              }
            };
          }
          
          
          class StateFinit extends StateSimple
          { @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            Trans to_off(EventObject ev, Trans trans) {
              if(trans == null) return new Trans(StateOff.class);
              if(stateMachine.isInState(StateActive1.StateFinit.class) && stateMachine.isInState(StateActive2.StateShouldOff.class)) {
                trans.retTrans |= mTransit;
              } 
              return null;
            }
            

          }
          
          //StateSimple stateFinit = new StateSimple(){
          //};
          
        } 
        
        
        class StateActive2 extends StateParallel
        { @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

          @Override protected void exit(){ System.out.println(" exit " + stateId); }

          class StateRemainOn extends StateSimple
          { final boolean isDefault = true;
          
            @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return mRunToComplete; }
  
            @Override protected void exit(){ System.out.println(" exit " + stateId); }
 
            Trans toShouldOff(EventObject ev, Trans trans){
              if(trans == null) return new Trans(StateShouldOff.class);
              else if(cond.offAfterRunning) {trans.retTrans |= mTransit; }
              return null;
            }
            
          }
          
          class StateShouldOff extends StateSimple
          {
            @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
            
            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            
          }
        }
        
      }//class StateActive
    }
  }
  
  
  final States states;
  
  
  private StatesNestedParallel(){
    states = new States(thread);
  }
  
  private void executeCondions() {
    cond.on = true;
    cond.start3 = true;
    cond.offAfterRunning = true;
    if(event.occupy(null, states, thread, true)){
      event.sendEvent(CmdEvent.start);
      //instead:
      //states.processEvent(event);
    }
    //cond.on = false;
    while(!states.isInState(States.StateOff.class)) {
      try{ Thread.sleep(100);
      } catch(InterruptedException exc) {}
      if(event.occupy(null, states, thread, true)){
        event.sendEvent(CmdEvent.cyclic);  //animate the state machine cyclically to check some conditions.
      }
    }
    try{
      //timer.close();
      thread.close();
    } catch(Exception exc){}
  }
  
  
  
  public static void main(String[] args){
    StatesNestedParallel main = new StatesNestedParallel();
    main.executeCondions();
  }
  
}
