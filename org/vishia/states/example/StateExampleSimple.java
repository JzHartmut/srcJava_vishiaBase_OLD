package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.states.StateComposite;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple.Trans;

public class StateExampleSimple
{
  

  enum Ecmd{Step};
  
  Event<Ecmd, Event.NoOpponent> ev = new Event<Ecmd, Event.NoOpponent>();
  
  
  //class States extends StateTop
  StateMachine states1 = new StateMachine()
  {
    
    class StateIdle extends StateSimple {

      Trans step_State1 = new Trans(StateCompositeExample.class){ 
        @Override protected int check(Event<?, ?> ev)
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
        Trans addRequest = new Trans(){ 
          @Override protected int check(Event<?, ?> ev) {
            retTrans = mEventConsumed;
            doExit();
            doEntry(ev);
            return retTrans;
          }
        };
      }
      
      @SuppressWarnings("unused") 
      class State2 extends StateSimple {

        Trans addRequest = new Trans() { 
          @Override protected int check(Event<?, ?> ev) {
            retTrans = mEventConsumed;
            doExit();
            doEntry(ev);
            return retTrans;
          }
        };
     
        
        Trans trans2_Idle(Event<?,?> ev, Trans trans){
          if(trans == null){
            return new Trans(StateIdle.class);
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
    main.states1.processEvent(main.ev);
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.processEvent(main.ev);
    main.states1.processEvent(main.ev);
    main.states1.processEvent(main.ev);
  }
  
  
}
