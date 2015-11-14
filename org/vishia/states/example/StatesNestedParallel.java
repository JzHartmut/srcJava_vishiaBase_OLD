package org.vishia.states.example;

import java.util.EventObject;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventTimeout;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.event.TimeOrder;
import org.vishia.msgDispatch.MsgRedirectConsole;
import org.vishia.states.StateComposite;
import org.vishia.states.StateCompositeFlat;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on_ready, on_cont;  
  
  
    boolean start;
    
    boolean offAfterRunning;
    
    boolean cont;
    
    boolean off;
    
    boolean finished;
  }
  
  
  /**Commands for the event.
   */
  enum CmdEvent { start, ready, cyclic, off, on_cont};
  
  /**An event type reuseable for the state machine animation. */
  class EventA extends EventCmdtype<CmdEvent>{}
  
  /**Some conditions for transition in this example. */
  Conditions cond = new Conditions();
  
  /**An event instance reused for the state machine animation.*/
  EventA event = new EventA();
  
  
  /**The thread to execute the state machine. */
  EventTimerThread threadEventTimer = new EventTimerThread("thread");

  /**Timer organisation. */
  //EventTimerMng timer = new EventTimerMng("timer");

  /**The state machine. It is an inner class because it should not contain any other things as states. This class is analysed by reflection
   * to complete the data for state machine execution. 
   */
  class States extends StateMachine
  {
    States(EventTimerThread thread){ super("ExampleNestedParallel", thread); }

    @Override protected int eventDebug(EventObject ev){ return super.eventDebug(ev); }
    
    class StateOff extends StateSimple
    {
      boolean isDefault;
      
      @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }
      
      //Trans ready = new Trans(StateWork.StateReady.class);
      
      //Trans history = new Trans(StateWork.StateActive.class);
      
      //@Override protected Trans selectTrans(EventObject ev) { return super.selectTrans(ev); }
      /*
      if(cond.on) {
          //choice
          if(cond.cont) return history;
          else return ready;
        } else return null;
      }
      */
      
      
      public TransChoice on = new TransChoice() { 
        
        Trans cont_history = new TransDeepHistory(StateWork.class); 
        Trans ready = new Trans(StateWork.StateReady.class);
          
        @Override public Trans choice() {
          if(cond.cont) return cont_history;
          else return ready;
        }
        
      };
      
      
      
      Trans on_Ready = new Trans(StateWork.StateReady.class); 
      
      
      @Override protected Trans checkTrans(EventObject ev) {
        if(  ev instanceof EventA 
            && ((EventA)ev).getCmd() == CmdEvent.start ){
          return on.choice().eventConsumed();
        }
        else if(cond.on_cont){
          cond.on_cont = false;
          return on.choice();
        }
        //else if(cond.on) return on_Ready;
        else return null;
      }
      
    };
    
    
    class StateWork extends StateComposite
    {
      @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }

      Trans to_off = new Trans(StateOff.class);
      
      @Override protected Trans checkTrans(EventObject ev){
        if(cond.off) {
          cond.off = false;
          return to_off;
        }
        else return null;
      }
      
      class StateReady extends StateSimple
      {
        final boolean isDefault = true;
        
        @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
        
        @Override protected void exit(){ System.out.println(" exit " + stateId); }

        Trans start_Running = new Trans( StateActive.StateActive1.StateRunning.StateRunning1.class
                                       , StateActive.StateActive1.StateRunning.StateRunning2.StateRunning21.class
                                       , StateActive.StateActive2.StateRemainOn.class);
        
        @Override protected Trans checkTrans(EventObject ev){
          if(cond.start) return start_Running;
          else return null;
        }
          
      }//StateReady
      
      
      class StateActive extends StateParallel
      {
        @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

        @Override protected void exit(){ System.out.println(" exit " + stateId); }

        //TransJoin to_off = (new TransJoin(StateOff.class)).srcStates(StateActive2.StateShouldOff.class, StateActive1.StateFinit.class);
        TransJoin to_off = new TransJoin(StateOff.class) {
          @Override protected void action(EventObject ev) {
            cond.finished = true;
          }
        };
        { to_off.srcStates(StateActive2.StateShouldOff.class, StateActive1.StateFinit.class); }
        
        @Override protected Trans checkTrans(EventObject ev){
          if(to_off.joined()) {
            return to_off;
          }
          else return null;
        }

        class StateActive1 extends StateComposite
        {
          @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

          @Override protected void exit(){ System.out.println(" exit " + stateId); }

          class StateRunning extends StateCompositeFlat
          { final boolean isDefault = true;
            @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

            @Override protected void exit(){ System.out.println(" exit " + stateId); }

            Timeout timeout = new Timeout(5000, StateFinit.class) {
              @Override protected void action(EventObject ev){
                System.out.println("  timeout");
              }
            };
            
            @Override protected Trans checkTrans(EventObject ev){ return null; }
            
            
            class StateRunning1 extends StateSimple {
              final boolean isDefault = true;
              @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
              @Override protected void exit(){ System.out.println(" exit " + stateId); }
            }
            
            class StateRunning2 extends StateCompositeFlat {
              class StateRunning21 extends StateSimple {
                final boolean isDefault = true;
                @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
                @Override protected void exit(){ System.out.println(" exit " + stateId); }
              }
              
            }
          }
          
          
          class StateFinit extends StateSimple
          { @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

            @Override protected void exit(){ System.out.println(" exit " + stateId); }

          }//StateFinit
          
        } 
        
        
        class StateActive2 extends StateComposite
        { @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

          @Override protected void exit(){ System.out.println(" exit " + stateId); }

          class StateRemainOn extends StateSimple
          { final boolean isDefault = true;
          
            @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return mRunToComplete; }
  
            @Override protected void exit(){ System.out.println(" exit " + stateId); }
 
            Trans toShouldOff = new Trans(StateShouldOff.class);
            
            @Override protected Trans checkTrans(EventObject ev){
              if(cond.offAfterRunning) return toShouldOff;
              else return null;
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
    states = new States(threadEventTimer);
  }
  
  private void execute() {
    cond.on_ready = true;
    cond.start = true;
    cond.offAfterRunning = true;
    if(event.occupy(null, states, threadEventTimer, true)){
      event.sendEvent(CmdEvent.start);
      //instead:
      //states.processEvent(event);
    }
    //EventTimeout evOff = new EventTimeout(states, threadEventTimer);
    //evOff.activate(1000);
    
    @SuppressWarnings("serial") 
    TimeOrder evOff1 = new TimeOrder("off", threadEventTimer){
      @Override protected void executeOrder(){ cond.off = true;  cond.on_cont = true; cond.cont = true;}
    };
    evOff1.occupy(null, true);
    evOff1.activate(1000);
    //cond.on = false;
    do {
      try{ Thread.sleep(100);
      } catch(InterruptedException exc) {}
      if(event.occupy(null, states, threadEventTimer, true)){
        event.sendEvent(CmdEvent.cyclic);  //animate the state machine cyclically to check some conditions.
        
      }
    } while(!cond.finished); //!states.isInState(States.StateOff.class));
    try{
      //timer.close();
      threadEventTimer.close();
    } catch(Exception exc){}
    try{ Thread.sleep(100);  //wait for end of the threadEventTimer.
    } catch(InterruptedException exc) {}

  }
  
  
  
  public static void main(String[] args){
    MsgRedirectConsole msg = new MsgRedirectConsole();
    StatesNestedParallel main = new StatesNestedParallel();
    main.execute();
  }

  
}
