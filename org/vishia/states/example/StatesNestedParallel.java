package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.states.StateAddParallel;
import org.vishia.states.StateComposite;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.states.StateTop;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on_ready;  
  
  
    boolean start;
  }
  
  
  Conditions cond = new Conditions();
  
  class States extends StateTop
  {
    class StateOff extends StateSimple
    {
      StateTrans start = new StateTrans(StateWork.StateActive.StateActive1.StateRunning.class, StateWork.StateActive.StateActive2.StateRemainOn.class){ 
        @Override protected int trans(Event<?, ?> ev) {
        if(cond.start) return exit().entry(StateWork.StateActive.StateActive1.StateRunning.class, ev);
        else return 0;
      } };

      
      StateTrans on_Ready = new StateTrans(StateWork.StateReady.class){ 
        @Override protected int trans(Event<?, ?> ev) {
        if(cond.on_ready){
          return switchto(ev, StateWork.StateReady.class);
        }
        else return 0;
      } };
      
      
      
      StateTrans testChoice = new StateTrans(){
        @Override protected int trans(Event<?, ?> ev) {
          return 0;
        }        
        
        Choice choice1 = new Choice(StateWork.StateReady.class) {
          
        };
        
        
      };
      
      
      
      int on_Ready(Event<?, ?> ev) {
        //Class<? extends StateSimple> dst = StateWork.StateReady.class;
        //int id = StateWork.class.hashCode();
        if(cond.on_ready) return switchto(ev, StateWork.StateReady.class);
        else return 0;
      }
      
    }
    
    
    class StateWork extends StateComposite
    {
      class StateReady extends StateSimple
      {
        
        StateTrans start = new StateTrans(StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class){ 
          @Override protected int trans(Event<?, ?> ev) {
          if(cond.start) {
            return exit().entry(StateActive.StateActive1.StateRunning.class, ev);
          }
          else return 0;
          }
          
          {
            action = new Runnable(){@Override public void run()
            {
              // TODO Auto-generated method stub
              
            }};
          }
        };
        
      }
      
      
      class StateActive extends StateParallel
      {
        
        class StateActive1 extends StateAddParallel
        {
          
          class StateRunning extends StateSimple
          {
            
          }
          
          
          class StateFinit extends StateSimple
          {
            
          }
          
          //StateSimple stateFinit = new StateSimple(){
          //};
          
        } 
        StateActive1 stateActive1; // = new StateActive1();
        
        
        class StateActive2 extends StateAddParallel
        {
          class StateRemainOn extends StateSimple
          {
            
          }
          
          class StateShouldOff extends StateSimple
          {
            
          }
        }
      }
    }
  }
  
  
  final States states;
  
  
  private StatesNestedParallel(){
    states = new States();
  }
  
  private void execute(){
    cond.on_ready = true;
    states.processEvent(null);
  }
  
  
  
  public static void main(String[] args){
    StatesNestedParallel main = new StatesNestedParallel();
    main.execute();
  }
  
}
