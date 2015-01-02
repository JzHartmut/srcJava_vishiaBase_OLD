package org.vishia.stateMachine.example;

import org.vishia.event.EventMsg2;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;

public class EvX extends EventMsg2<EvX.Cmd,EventMsg2.NoOpponent>{
  
  
  enum Cmd{EvX, EvY, EvZ};
  
  EvX(EventSource src, EventConsumer dst, EventThread thread){
    super(src,dst, thread);
  }
  
  @Override public boolean sendEvent(Cmd cmd){ return sendEvent(cmd); }
   
}
