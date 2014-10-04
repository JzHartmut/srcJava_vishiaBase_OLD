package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.states.StateComposite;
import org.vishia.states.StateSimple;
import org.vishia.states.StateTop;

public class StateExampleSimple
{
  

  enum Ecmd{Step};
  
  Event<Ecmd, Event.NoOpponent> ev = new Event<Ecmd, Event.NoOpponent>();
  
  
  //class States extends StateTop
  StateTop states1 = new StateTop()
  {
    
    class StateIdle extends StateSimple {

      StateTrans step_State1 = new StateTrans(){ @Override protected int trans(Event<?, ?> ev)
      { return exit().entry(StateCompositeExample.State1.class, ev);
      }};
    
      
    }
    
    
    class StateCompositeExample extends StateComposite 
    {
      class State1 extends StateSimple {

        @SuppressWarnings("unused") 
        StateTrans addRequest = new StateTrans(){ @Override protected int trans(Event<?, ?> ev)
        { return exit().entry(StateIdle.class, ev);
        }};
      
        
      }
      
      @SuppressWarnings("unused") 
      class State2 extends StateSimple {

        StateTrans addRequest = new StateTrans(){ @Override protected int trans(Event<?, ?> ev)
        { return exit().entry(StateIdle.class, ev);
        }};
     
        
        int addRequest_Idle(Event<?,?> ev){
          return exit().entry(StateIdle.class, ev);
        }
        
        
        
      }
      
    }
    
    
    
    
    
  };
  

  
  
  final public static void main(String[] args) {
    
    StateExampleSimple main = new StateExampleSimple();
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.processEvent(main.ev);
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.processEvent(main.ev);
    main.states1.processEvent(main.ev);
    main.states1.processEvent(main.ev);
  }
  
  
}
