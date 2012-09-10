package org.vishia.util.sampleStateM;

import org.vishia.util.EventSource;
import org.vishia.util.EventThread;

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
    while(true){
      synchronized (this) {
        try{ 
          wait(1000);
        }catch(InterruptedException exc){}
        
      }
      EvX ev = new EvX(eventSource, compositeStates, eventThread);
      ev.sendEvent(EvX.Cmd.EvX);
    }
    
  }
  
  
  public static void main(String[] args){
    MainSampleStateMachine main = new MainSampleStateMachine();
    main.exec();
  }
  
}
