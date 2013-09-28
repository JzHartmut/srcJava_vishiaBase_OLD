package org.vishia.stateMachine.example;

import java.io.Closeable;
import java.io.IOException;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;
import org.vishia.event.EventTimerMng;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Assert;
import org.vishia.util.Timeshort;

public class ExampleStateComposite implements Closeable{

  /**A state machine or some state machines in several threads may need a timer to create timer events.
   * It is possible to create a singleton instance using the static methods of EventTimerMng,
   * but it is also possible to create some instances of the timer manager. Each instance creates a thread.
   */
  final EventTimerMng timer = new EventTimerMng("timer");
  
  
  /**The state machine should be processed in only one thread. Then all transitions can be written
   * non thread safe, which is some more simple for the user. This instance creates a thread
   * and contains a queue for events. All events will be stored here and processed in this thread.
   */
  final EventThread eventQueue = new EventThread("eventQueue");
  
  
  
  /**Delay-Parameter for timeout of {@link StateTop.StateProcess.StateActive} */
  int delay = 1200;
  
  
  public ExampleStateComposite(){
    eventQueue.startThread();  
  }
  
  
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
    
    /**Creates an event as a dynamic object for direct usage without a given {@link EventConsumer}.
     * This event should be used as parameter immediately for an event consuming routine.
     */
    public UserEvent(){
      super();
    }
    
    /**Creates an event as a dynamic object for direct usage without a given {@link EventConsumer}.
     * This event should be used as parameter immediately for an event consuming routine.
     */
    public UserEvent(EventSource source, EventConsumer dst, EventThread thread){
      super(source, dst, thread);
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
  
  
  
  private final StateTop stateTop = new StateTop();
  /**The Top state of the state machine contains all inner states.
   */
  private final class StateTop extends StateTopBase<StateTop>{

    protected StateTop() { 
      super("ExampleStateComposite", eventQueue, timer);
      setDefaultState(stateOff);  //one of the states
    }

    
    /**This method is overridden only to set a derived class specific breakpoint for debugging.
     * @see org.vishia.stateMachine.StateTopBase#processEvent(org.vishia.event.Event)
     */
    @Override public int processEvent(final Event<?,?> evP){
      return super.processEvent(evP);   //<<<<<<<<<< set breakpoint here
    }


    
    
    StateOff stateOff = new StateOff(this);
    /**State ready, this state is the idle state.
     */
    private final class StateOff extends StateSimpleBase<StateTop>{
      
      StateOff(StateTop enclState){ super(enclState, "Off", false); }
      
      /**Transition to process, triggered with EventTemplate.cmd == {@link UserCmd#On}. 
       * Template hint: use an extra method for any transition to see it in the overview (Outline). */
      private int transProcess(UserEvent ev){
        if(ev !=null && ev.getCmd() == UserEvent.Cmd.on){
          //Template: some actions for the transition should notice here.
          return exit().stateProcess.stateReady.entry(ev);
        }
        else return 0;
      }
      
      /**The trans method need not be commented. 
       * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.event.Event)
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
        if(ev !=null && ev.getCmd() == UserEvent.Cmd.off){
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
        
        /**Transition to stateActive, triggered with EventTemplate.cmd == {@link UserCmd#start}. */
        private int transActive(UserEvent ev){
          if(ev !=null && ev.getCmd() == UserEvent.Cmd.start){
            //Template: some actions for the transition should notice here.
            return exit().stateActive.entry(ev);
          }
          else return 0;
        }
        
        /**The trans method need not be commented. 
         * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.event.Event)
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
          timeOrder = stateTop.addTimeOrder(System.currentTimeMillis() + delay);
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
          if(ev !=null && ev.isMatchingto(timeOrder)){
            timeOrder = null;  
            return exit().stateReady.entry(ev);
          }
          else return 0;
        }
        
        /**The trans method need not be commented. 
         * @see org.vishia.stateMachine.StateSimpleBase#trans(org.vishia.event.Event)
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

  
  /**This instance is helpfull for debugging. It is not need for event processing. */
  EventSource evSource = new EventSource("ExampleStateComposite"){
    
  };
  
  /**Routine for the out-effect. It may set any binary output maybe via any communication.
   * In the example it calls println("...");
   * @param val true or false.
   */
  void setOut(boolean val){
    System.out.println("ExampleStateComposite - setOut;" + val);
  }
  
  @Override public void close(){
    try{ 
      timer.close();
      eventQueue.close();
    } catch(IOException exc){ System.err.println("ExampleStateComposite - unexpected;" + exc.getMessage()); }
  }
  
  /**This method is given only for the simple test of this class.
   * @param args not used
   */
  public static final void main(String[] args){
    ExampleStateComposite main = new ExampleStateComposite();
    //main.test1();
    main.test2();
    main.close();
  }
  
  
  /**This pattern processes the statemachine in the own thread. 
   * The pattern is able to use especially by condition-triggered state machine, which are processed
   * in a cyclically time slice. But there should not used a {@link EventTimerMng.TimeEvent}
   * which is processed in another, the timer thread. Therefore this pattern is not proper
   * for this example. But it may helpfully for first debugging in the  statemachine's technique.
   */
  private void test1(){
    UserEvent ev = new UserEvent();
    stateTop.processEvent(ev.setCmd(UserEvent.Cmd.on));
    Timeshort.sleep(500);
    stateTop.processEvent(ev.setCmd(UserEvent.Cmd.start));
    Timeshort.sleep(500);
    stateTop.processEvent(ev.setCmd(UserEvent.Cmd.off));
    stateTop.processEvent(ev.setCmd(UserEvent.Cmd.on));
    stateTop.processEvent(ev.setCmd(UserEvent.Cmd.start));
    Timeshort.sleep(3000);
    Assert.stop();    
  }
  

  
  /**This pattern processes the statemachine in its specific thread,
   * which's queue stores the timer event too. It is the recommended pattern for complex ones.
   * The event's where created and fired in this thread, in the example. 
   * In reality the events may be created in different threads from several sources.
   */
  private void test2(){
    UserEvent ev = new UserEvent(evSource, stateTop, stateTop.theThread);
    ev.sendEvent(UserEvent.Cmd.on);
    
    Timeshort.sleep(500);
    ev = new UserEvent(evSource, stateTop, stateTop.theThread);
    ev.sendEvent(UserEvent.Cmd.start);
    
    Timeshort.sleep(500);
    ev = new UserEvent(evSource, stateTop, stateTop.theThread);
    ev.sendEvent(UserEvent.Cmd.off);
    
    ev = new UserEvent(evSource, stateTop, stateTop.theThread);
    ev.sendEvent(UserEvent.Cmd.on);
    
    ev = new UserEvent(evSource, stateTop, stateTop.theThread);
    ev.sendEvent(UserEvent.Cmd.start);
    
    Timeshort.sleep(3000);
    Assert.stop();    
  }
  
  
}
