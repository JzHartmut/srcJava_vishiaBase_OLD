package org.vishia.states.example;

import java.util.EventObject;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.states.StateComposite;
import org.vishia.states.StateCompositeFlat;
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
  
  
  /**Commands for the event.
   */
  enum CmdEvent { start, ready, cyclic};
  
  /**An event type reuseable for the state machine animation. */
  class EventA extends EventCmdtype<CmdEvent>{}
  
  /**Some conditions for transition in this example. */
  Conditions cond = new Conditions();
  
  /**An event instance reused for the state machine animation.*/
  EventA event = new EventA();
  
  
  /**The thread to execute the state machine. */
  EventTimerThread thread = new EventTimerThread("thread");

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
      
      
      public Choice on = new Choice() { 
        
        
        Trans cont_history = new TransDeepHistory(StateWork.class); 
        Trans ready = new Trans(StateWork.StateReady.class);
          
        @Override public Trans checkTrans() {
          if(cond.cont) return cont_history;
          else return ready;
        }
        
      };
      
      
      
      Trans on_Ready = new Trans(StateWork.StateReady.class); 
      
      
      @Override protected Trans selectTrans(EventObject ev) {
        if(  ev instanceof EventA 
            && ((EventA)ev).getCmd() == CmdEvent.start ){
          return on.checkTrans().eventConsumed();
        }
        //else if(cond.on) return on_Ready;
        else return null;
      }
      
    };
    
    
    class StateWork extends StateCompositeFlat
    {
      @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
      
      @Override protected void exit(){ System.out.println(" exit " + stateId); }

      class StateReady extends StateSimple
      {
        final boolean isDefault = true;
        
        @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
        
        @Override protected void exit(){ System.out.println(" exit " + stateId); }

        Trans start_Running = new Trans( StateActive.StateActive1.StateRunning.StateRunning1.class
                                       , StateActive.StateActive1.StateRunning.StateRunning2.StateRunning21.class
                                       , StateActive.StateActive2.StateRemainOn.class);
        
        @Override protected Trans selectTrans(EventObject ev){
          if(cond.start) return start_Running;
          else return null;
        }
          
      }//StateReady
      
      
      class StateActive extends StateParallel
      {
        @Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }

        @Override protected void exit(){ System.out.println(" exit " + stateId); }

        Join to_off = join(new Join(StateOff.class), StateActive1.StateFinit.class, StateActive2.StateShouldOff.class);

        @Override protected Trans selectTrans(EventObject ev){
          if(to_off.joined()) {
          //if(stateMachine.isInState(StateActive1.StateFinit.class) && stateMachine.isInState(StateActive2.StateShouldOff.class)) {
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
            
            @Override protected Trans selectTrans(EventObject ev){
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
    states = new States(thread);
  }
  
  private void execute() {
    cond.on = true;
    cond.start = true;
    cond.offAfterRunning = true;
    if(event.occupy(null, states, thread, true)){
      event.sendEvent(CmdEvent.start);
      //instead:
      //states.processEvent(event);
    }
    //cond.on = false;
    do {
      try{ Thread.sleep(100);
      } catch(InterruptedException exc) {}
      if(event.occupy(null, states, thread, true)){
        event.sendEvent(CmdEvent.cyclic);  //animate the state machine cyclically to check some conditions.
      }
    } while(!states.isInState(States.StateOff.class));
    try{
      //timer.close();
      thread.close();
    } catch(Exception exc){}
  }
  
  
  
  public static void main(String[] args){
    StatesNestedParallel main = new StatesNestedParallel();
    main.execute();
  }

  
}
