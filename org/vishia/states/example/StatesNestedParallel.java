package org.vishia.states.example;

import org.vishia.event.Event;
import org.vishia.states.StateAction;
import org.vishia.states.StateAddParallel;
import org.vishia.states.StateComposite;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on_ready;  
  
  
    boolean start;
  }
  
  
  Conditions cond = new Conditions();
  
  class States extends StateMachine
  {
    class StateOff extends StateSimple
    {
      StateTrans start = new StateTrans("start", StateWork.StateActive.StateActive1.StateRunning.class, StateWork.StateActive.StateActive2.StateRemainOn.class){ 
        { condition = new StateAction() {
            @Override public int action(Event<?, ?> ev) {
              if(cond.start){ 
                doExit();
                //no action
                return doEntry(ev) | mEventConsumed;
              }
              else return 0;
            };
          };
      } };

      
      StateTrans on_Ready = new StateTrans("on_Ready", StateWork.StateReady.class){ 
        @Override protected int trans(Event<?, ?> ev) {
        if(cond.on_ready){
          doExit();
          //no action
          return doEntry(ev) | mEventConsumed;
          //return switchto(ev, StateWork.StateReady.class);
        }
        else return 0;
      } };
      
      
      
      /*
      StateTrans testChoice = new StateTrans("testChoice"){
        @Override protected int trans(Event<?, ?> ev) {
          return 0;
        }        
        
        Choice choice1 = new Choice(StateWork.StateReady.class) {
          {
            condition = new Runnable(){@Override public void run()
            { }};
          }
        };
        
        
      };
      */
      
      
      StateTrans on_Ready(Event<?, ?> ev, StateTrans trans) {
        if(trans == null) return new StateTrans("on_Ready", StateWork.StateReady.class); 
        //Class<? extends StateSimple> dst = StateWork.StateReady.class;
        //int id = StateWork.class.hashCode();
        if(cond.on_ready){
          trans.doExit();
          trans.doEntry(ev);
        }
        return trans;
      }
      
    }
    
    
    class StateWork extends StateComposite
    {
      class StateReady extends StateSimple
      {
        
        @Override public int entry(Event<?,?> ev){
          System.out.println("entry Ready");
          return 0;
        }
        
        StateTrans start = new StateTrans("start", StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class){ 
          @Override protected int trans(Event<?, ?> ev) {
            if(cond.start) {
              doExit();
              //no action
              return doEntry(ev) | mEventConsumed;
            }
            else return 0;
          }
          
        };
        
        class Start extends StateTrans {
          
          Start(){ super("Start", StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class); } 
          
          @Override protected int trans(Event<?, ?> ev) {
            if(cond.start) {
              doExit();
              //no action
              return doEntry(ev) | mEventConsumed;
            }
            else return 0;
          }
          
          int action(Event<?,?>ev){
            return 0;
          }
        } //class Start
        
        StateTrans start2(Event<?,?> ev, StateTrans transP){
          if(transP == null){
            return new StateTrans("start2", StateActive.StateActive1.StateRunning.class, StateActive.StateActive2.StateRemainOn.class);
          } 
          if(cond.start) {
            transP.doExit();
            //no action
            transP.doEntry(ev);
            transP.retTrans =  mEventConsumed;
            return transP;
          }
          else return null;
          
        }
        
        
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
    states.applyEvent(null);
  }
  
  
  
  public static void main(String[] args){
    StatesNestedParallel main = new StatesNestedParallel();
    main.execute();
  }
  
}
