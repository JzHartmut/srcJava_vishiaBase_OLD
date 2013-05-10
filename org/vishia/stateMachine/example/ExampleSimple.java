package org.vishia.stateMachine.example;

import java.io.Closeable;
import java.io.IOException;

import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Assert;
import org.vishia.util.Event;
import org.vishia.util.EventTimerMng;
import org.vishia.util.Timeshort;

public class ExampleSimple implements Closeable{

  
  
  /**The type of a user event can be defined in an extra Java source file. 
   * In this example it is a public static inner class. The user event type knows specific command definitions
   * which are defined inside. On the second side an event contains user specific data, which are defined
   * in the specific event type. An application or a state machine can use more as one type of user events.
   */
  public final static class UserEvent extends Event<UserEvent.Cmd, Event.NoOpponent>{

    /**Commands for the {@link UserEvent} type. */
    public enum Cmd{ on, off, start }
    
    /**An event can contain any type specific user data. It is an example. */
    int anySpecificUserData;
    
    /**An event can contain any type specific user data reference. The type Object is a simple example only. Specify it! */
    Object anySpecificUserDataReference;

    /**Creates an event as a dynamic object for direct usage without a given {@link EventConsumer}.
     * This event should be used as parameter immediately for an event consuming routine.
     */
    public UserEvent(Cmd cmd){
      super(cmd);
    }
    
    /**Checks whether the event from its base type to the expected type.
     * @param ev The untyped event
     * @return null if the event does not match the type, elsewhere the casted event.
     */
    public static UserEvent typeof(Event<?,?> ev){
      if(ev instanceof UserEvent){
        return (UserEvent)(ev);
      } else {
        //This line can be a breakpoint line to detect errors.
        //Any special exception can be thrown here.
        return null;   //causes an NullPointerException if not expected from caller.
      }
    }
    
  }
  
  
  
  StateTop stateTop = new StateTop();
  /**The Top state of the state machine contains all inner states.
   */
  private final class StateTop extends StateTopBase<StateTop>{

    protected StateTop() { 
      super("StateTemplate");
      setDefaultState(stateOff);  //one of the states
    }

    
    StateOff stateOff = new StateOff(this);
    /**State ready, this state is the idle state.
     */
    private final class StateOff extends StateSimpleBase<StateTop>{
      
      StateOff(StateTop enclState){ super(enclState, "Off", false); }
      
      /**Transition to process, triggered with EventTemplate.cmd == {@link UserCmd#On}. 
       * Template hint: use an extra method for any transition to see it in the overview (Outline). */
      private int transProcess(UserEvent ev){
        if(ev.getCmd() == UserEvent.Cmd.on){
          //Template: some actions for the transition should notice here.
          return exit().stateProcess.stateReady.entry(ev);
        }
        else return 0;
      }
      
      /**The trans method need not be commented. 
       * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.util.Event)
       */
      @Override public int trans(Event<?,?> ev) {
        UserEvent ev1 = UserEvent.typeof(ev);  //convert the event to the expected derived class.
        int cont;
        //Template: one line per trans-Method. return if !=0
        if( (cont = transProcess(ev1)) !=0) return cont;
        else return 0;
      }
    } 

    
    
    protected StateProcess stateProcess = new StateProcess(this);
    private class StateProcess extends StateCompositeBase<StateProcess, StateTop>{

      protected StateProcess(StateTop enclState) { super(enclState, "Process"); setDefaultState(stateReady); }

      
      private int transOff(UserEvent ev){
        if(ev.getCmd() == UserEvent.Cmd.off){
          return exit().stateOff.entry(ev);
        } else return 0;
      }
      
      
      @Override public int trans(Event<?,?> ev){
        UserEvent ev1 = UserEvent.typeof(ev);  //convert the event to the expected derived class.
        int cont;
        //Template: one line per trans-Method. return if !=0
        if( (cont = transOff(ev1)) !=0) return cont;
        else return 0;
      }

 
      StateReady stateReady = new StateReady(this);
      /**State ready.
       */
      private final class StateReady extends StateSimpleBase<StateProcess>{
        
        StateReady(StateProcess enclState){ super(enclState, "Ready", false); }
        
        /**Transition to process, triggered with EventTemplate.cmd == {@link UserCmd#On}. 
         * Template hint: use an extra method for any transition to see it in the overview (Outline). */
        private int transActive(UserEvent ev){
          if(ev.getCmd() == UserEvent.Cmd.start){
            //Template: some actions for the transition should notice here.
            return exit().stateActive.entry(ev);
          }
          else return 0;
        }
        
        /**The trans method need not be commented. 
         * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.util.Event)
         */
        @Override public int trans(Event<?,?> ev) {
          UserEvent ev1 = UserEvent.typeof(ev);  //convert the event to the expected derived class.
          int cont;
          //Template: one line per trans-Method. return if !=0
          if( (cont = transActive(ev1)) !=0) return cont;
          else return 0;
        }
      } 

      protected StateActive stateActive = new StateActive(this);
      /**State ready.
       */
      private final class StateActive extends StateSimpleBase<StateProcess>{
        
        protected StateActive(StateProcess enclState){ super(enclState, "Active", false); }
        
        private EventTimerMng.TimeOrder timeOrder;
        
        @Override protected void entryAction(Event<?,?> ev){
          timeOrder = timer.addTimeOrder(System.currentTimeMillis() + delay, this.enclState, null);
          setOut(true);      
        }
        
        /**Note: this exit action will be called also if the enclosing state will be exited.
         * The time order is removed.
         */
        @Override protected void exitAction(){
          if(timeOrder !=null){
            timer.removeTimeOrder(timeOrder);
          }
          setOut(false);
        }
        
        /**Transition to process, triggered with EventTemplate.cmd == {@link UserCmd#On}. 
         * Template hint: use an extra method for any transition to see it in the overview (Outline). */
        private int transReady(EventTimerMng.TimeEvent ev){
          if(ev !=null){
            //Template: some actions for the transition should notice here.
            timeOrder = null;
            return exit().stateReady.entry(ev);
          }
          else return 0;
        }
        
        /**The trans method need not be commented. 
         * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.util.Event)
         */
        @Override public int trans(Event<?,?> ev) {
          int cont;
          //Template: one line per trans-Method. return if !=0
          if( (cont = transReady(EventTimerMng.TimeEvent.typeof(ev))) !=0) return cont;
          else return 0;
        }
      } 


    } 

    
    
  }

  
  /**A state machine or some statemachines in several threads may need a timer to create timer events.
   * It is possible to create a singleton instance using the static methods of EventTimerMng,
   * but it is also possible to create some instances of the timer manager. Each instance creates a thread.
   */
  EventTimerMng timer = new EventTimerMng("timer");
  
  
  int delay = 1200;
  
  void setOut(boolean val){
    System.out.println("ExampleStateSimple - setOut;" + val);
  }
  
  @Override public void close(){
    try{ 
      timer.close();
    } catch(IOException exc){ System.err.println("ExampleSimple - unexpected;" + exc.getMessage()); }
  }
  
  /**This method is given only for the simple test of this class.
   * @param args not used
   */
  public static final void main(String[] args){
    ExampleSimple main = new ExampleSimple();
    main.test1();
    main.close();
  }
  
  
  private void test1(){
    stateTop.processEvent(new UserEvent(UserEvent.Cmd.on));
    Timeshort.sleep(500);
    stateTop.processEvent(new UserEvent(UserEvent.Cmd.start));
    Timeshort.sleep(500);
    stateTop.processEvent(new UserEvent(UserEvent.Cmd.off));
    stateTop.processEvent(new UserEvent(UserEvent.Cmd.on));
    stateTop.processEvent(new UserEvent(UserEvent.Cmd.start));
    Timeshort.sleep(3000);
    Assert.stop();    
  }
  
  
}
