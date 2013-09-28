package org.vishia.stateMachine.example;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;

public class EvX extends Event<EvX.Cmd,Event.NoOpponent>{
  
  
  enum Cmd{EvX, EvY, EvZ};
  
  EvX(EventSource src, EventConsumer dst, EventThread thread){
    super(src,dst, thread);
  }
  
  @Override public boolean sendEvent(Cmd cmd){ return sendEvent(cmd); }
   
}
