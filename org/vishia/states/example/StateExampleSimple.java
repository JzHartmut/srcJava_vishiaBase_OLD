package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.states.StateComposite;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple.StateTrans;
import org.vishia.states.example.StatesNestedParallel.States.StateWork.StateActive;

public class StateExampleSimple
{
  

  enum Ecmd{Step};
  
  Event<Ecmd, Event.NoOpponent> ev = new Event<Ecmd, Event.NoOpponent>();
  
  
  //class States extends StateTop
  StateMachine states1 = new StateMachine()
  {
    
    class StateIdle extends StateSimple {

      StateTrans step_State1 = new StateTrans("descr"){ @Override protected int trans(Event<?, ?> ev)
      { exitState();
        return entryState(ev) | mEventConsumed;
      }};
    
      
    }
    
    
    class StateCompositeExample extends StateComposite 
    {
      class State1 extends StateSimple {

        @SuppressWarnings("unused") 
        StateTrans addRequest = new StateTrans("descr"){ @Override protected int trans(Event<?, ?> ev)
        { exitState();
          return entryState(ev) | mEventConsumed;
        }};
      
        
      }
      
      @SuppressWarnings("unused") 
      class State2 extends StateSimple {

        StateTrans addRequest = new StateTrans("descr"){ @Override protected int trans(Event<?, ?> ev)
        { exitState();
        return entryState(ev) | mEventConsumed;
        }};
     
        
        StateTrans trans2_Idle(Event<?,?> ev, StateTrans trans){
          if(trans == null){
            return new StateTrans("start2", StateIdle.class);
          } 
          trans.exitState();
          trans.retTrans = trans.entryState(ev) | mEventConsumed;
          return trans;
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
