package org.vishia.stateMachine.example;

import org.vishia.event.EventSource;
import org.vishia.event.EventThread;

public class MainSampleStateMachine
{

  private final EventThread eventThread = new EventThread("eventThread");
  
  private final EventSource eventSource = new EventSource("Main"){
    
  };
  
  private final EventFollow eventFollow = new EventFollow(eventThread);
  
  private final CompositeStates compositeStates = new CompositeStates(eventThread);
  
  public MainSampleStateMachine() {
    //super("EventThread - mainSample");
    eventThread.startThread();
    eventFollow.startState();       //start the statemachine, first call without event.
  }
  
  
  void exec(){
    EvX ev = new EvX(eventSource, compositeStates, eventThread);
    ev.sendEvent(EvX.Cmd.EvX);
    wait100();
    ev = new EvX(eventSource, compositeStates, eventThread);
    ev.sendEvent(EvX.Cmd.EvZ);
    wait100();
    ev = new EvX(eventSource, compositeStates, eventThread);
    ev.sendEvent(EvX.Cmd.EvX);
    wait100();
    
  }

  
  private void wait100(){
    synchronized (this) {
      try{ 
        wait(100);
      }catch(InterruptedException exc){}
      
    }
    
  }
  
  
  public static void main(String[] args){
    MainSampleStateMachine main = new MainSampleStateMachine();
    main.exec();
  }
  
}
