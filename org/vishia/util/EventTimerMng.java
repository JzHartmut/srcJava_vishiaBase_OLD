package org.vishia.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class EventTimerMng extends Thread implements Closeable{

  public static class TimeEntry{
    /**Absolute time when the event should be occurred. */
    final long dateEvent;
    
    final Event<TimeEvent.Cmd, Event.NoOpponent> event;
    
    TimeEntry(Event<TimeEvent.Cmd, Event.NoOpponent> ev, long date){
      this.dateEvent = date;
      this.event = ev;
    }
  }
  
  
  public static class TimeEvent extends Event<TimeEvent.Cmd, Event.NoOpponent>{
    enum Cmd{Time};
    
    
    /**The constructor
     * @param dst A destination object should be given
     * @param thread The destination thread may be null, then the {@link EventConsumer#processEvent(Event)} method
     *   is called in the timer thread.
     */
    TimeEvent(EventConsumer dst, EventThread thread){
      super(null, dst, thread);
    }
  }
  
  
  EventSource evSource = new EventSource("TimerMng"){
    
  };
  
  private static EventTimerMng singleton;
  
  private boolean run;
  
  /**timestamp for a new time entry. It is set in synchronized operation between {@link #addTimeOrder(Event, long)}
   * and the wait in the {@link #run()} opeation.
   * 
   */
  long dateCheckNew;
  
  /**true if the wait operation runs in {@link #run()} */
  private boolean bWait;
  
  ConcurrentLinkedQueue<TimeEntry> timeEntries = new ConcurrentLinkedQueue<TimeEntry>();
  
  ConcurrentLinkedQueue<TimeEntry> timeEntriesNew = new ConcurrentLinkedQueue<TimeEntry>();
  
  long dateCheck;
  
  
  public EventTimerMng(){
    start();
  }
  
  
  public static void addTimeOrder(long date, Event<TimeEvent.Cmd, Event.NoOpponent> evTime){
    if(singleton == null){
      singleton = new EventTimerMng();
    }
    singleton.addTimeOrder_(date, evTime);
  }
  
  
  public static void addTimeOrder(long date, EventConsumer dst, EventThread thread){
    if(singleton == null){
      singleton = new EventTimerMng();
    }
    singleton.addTimeOrder_(date, new TimeEvent(dst, thread));
  }
  
  
  private void addTimeOrder_(long date, Event<TimeEvent.Cmd, Event.NoOpponent> evTime){
    Assert.checkMsg (evTime instanceof TimeEvent, "The Event should be a org.vishia.util.EventTimerMng.TimeEvent");
    Assert.checkMsg (evTime.hasDst(), "The Event must have a destination.");
    Assert.checkMsg (!evTime.isOccupied(), "The Event must not be occupied.");
    TimeEntry entry = new TimeEntry(evTime, date);
    timeEntriesNew.add(entry);
    //notify the run process to shorten the wait time.
    synchronized(this){
      if((date - dateCheck) < 0){
        if(bWait){
          notify();
        } else {
          
        }
      }
    }
  }
  
  
  @Override public void run(){
    run = true;
    while(run){
      long timeAct = System.currentTimeMillis();
      if((timeAct - dateCheck) >=0){
        //woken up after dateCheck, now check all timer events.
        long dateCheck1 = Long.MAX_VALUE;
        Iterator<TimeEntry> iter = timeEntries.iterator();
        while(iter.hasNext()){
          TimeEntry entry = iter.next();
          long wait = entry.dateEvent - timeAct;
          if(wait <=0){
            iter.remove();
            executeTime(entry);
          } else if(dateCheck1 > entry.dateEvent){ 
            dateCheck1 = entry.dateEvent; 
          }
        }
        this.dateCheck = dateCheck1;
      }
      TimeEntry entryNew;
      while( (entryNew = timeEntriesNew.poll()) !=null) {
        if((entryNew.dateEvent - dateCheck) < 0 ){
          dateCheck = entryNew.dateEvent;
        }
        timeEntries.add(entryNew);
      }
      try{
        synchronized(this){
          long timeWait = dateCheck - timeAct;
          if(timeWait > 0 && timeEntriesNew.peek() == null){
            bWait = true;
            wait(timeWait);
            bWait = false;
          }
        }
      }catch(InterruptedException exc){
        
      }
    }
    
  }


  
  private void executeTime(TimeEntry entry){
    entry.event.occupy(evSource, true);
    entry.event.sendEvent(TimeEvent.Cmd.Time);
  }
  
  

  @Override public void close() throws IOException {
    run = false;
    
  }
}
