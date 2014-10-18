package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.states.StateComposite;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple.StateTrans;

public class StateExampleSimple
{
  

  enum Ecmd{Step};
  
  Event<Ecmd, Event.NoOpponent> ev = new Event<Ecmd, Event.NoOpponent>();
  
  
  //class States extends StateTop
  StateMachine states1 = new StateMachine()
  {
    
    class StateIdle extends StateSimple {

      StateTrans step_State1 = new StateTrans(StateCompositeExample.class){ 
        @Override protected int trans(Event<?, ?> ev)
        { retTrans = mEventConsumed;
          doExit();
          doEntry(ev);
          return retTrans;
        }
      };
    
      
    }
    
    
    class StateCompositeExample extends StateComposite 
    {
      class State1 extends StateSimple {

        @SuppressWarnings("unused") 
        StateTrans addRequest = new StateTrans(){ 
          @Override protected int trans(Event<?, ?> ev) {
            retTrans = mEventConsumed;
            doExit();
            doEntry(ev);
            return retTrans;
          }
        };
      }
      
      @SuppressWarnings("unused") 
      class State2 extends StateSimple {

        StateTrans addRequest = new StateTrans() { 
          @Override protected int trans(Event<?, ?> ev) {
            retTrans = mEventConsumed;
            doExit();
            doEntry(ev);
            return retTrans;
          }
        };
     
        
        StateTrans trans2_Idle(Event<?,?> ev, StateTrans trans){
          if(trans == null){
            return new StateTrans(StateIdle.class);
          } 
          trans.retTrans = mEventConsumed;
          trans.doExit();
          trans.doEntry(ev);
          return trans;
        }
        
        
        
      }
      
    }
    
    
    
    
    
  };
  

  
  
  final public static void main(String[] args) {
    
    StateExampleSimple main = new StateExampleSimple();
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.applyEvent(main.ev);
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.applyEvent(main.ev);
    main.states1.applyEvent(main.ev);
    main.states1.applyEvent(main.ev);
  }
  
  
}
