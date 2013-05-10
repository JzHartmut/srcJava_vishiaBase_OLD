package org.vishia.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class EventTimerMng extends Thread implements Closeable{

  public static class TimeOrder{
    /**Absolute time when the event should be occurred. */
    final long dateEvent;
    
    final Event<TimeEvent.Cmd, Event.NoOpponent> event;
    
    final EventConsumer dst;
    
    final EventThread threadDst;
    
    TimeOrder(Event<TimeEvent.Cmd, Event.NoOpponent> ev, long date){
      this.dateEvent = date;
      this.event = ev;
      this.dst = null;  //stored in ev
      this.threadDst = null;
    }

    TimeOrder(long date, EventConsumer dst, EventThread dstThread){
      this.dateEvent = date;
      this.event = null;  //should be create if need.
      this.dst = dst;  //stored in ev
      this.threadDst = dstThread;
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
    
    /**Checks whether the event is a TimeEvent.
     * @param ev The untyped event
     * @return null if the event does not match the type, elsewhere the casted event.
     */
    public static TimeEvent typeof(Event<?,?> ev){
      if(ev instanceof TimeEvent){
        return (TimeEvent)(ev);
      } else {
        return null;   //causes an NullPointerException if not expected from caller.
      }
    }

  }
  
  
  EventSource evSource = new EventSource("TimerMng"){
    
  };
  
  private static EventTimerMng singleton;
  
  private boolean run;
  
  /**timestamp for a new time entry. It is set in synchronized operation between {@link #addTimeOrder(Event, long)}
   * and the wait in the {@link #run()} operation.
   * 
   */
  long dateCheckNew;
  
  /**true if the wait operation runs in {@link #run()} */
  private boolean bWait;
  
  ConcurrentLinkedQueue<TimeOrder> timeEntries = new ConcurrentLinkedQueue<TimeOrder>();
  
  ConcurrentLinkedQueue<TimeOrder> timeEntriesNew = new ConcurrentLinkedQueue<TimeOrder>();
  
  
  /**The next time where the thread will be release its wait(millisec) routine without extra notifying.  
   * If a new order will be added which's end time is before that, the timer thread will be notified
   * to accept this early timestamp. 
   */
  long dateCheck;
  
  
  public EventTimerMng(String sNameThread){
    super(sNameThread);
    start();
  }
  
  
  public static TimeOrder addGlobalTimeOrder(long date, Event<TimeEvent.Cmd, Event.NoOpponent> evTime){
    if(singleton == null){
      singleton = new EventTimerMng("EventTimerMng");
    }
    return singleton.addTimeOrder(date, evTime);
  }
  
  
  public static TimeOrder addGlobalTimeOrder(long date, EventConsumer dst, EventThread thread){
    if(singleton == null){
      singleton = new EventTimerMng("EventTimerMng");
    }
    return singleton.addTimeOrder(date, new TimeEvent(dst, thread));
  }
  

  public TimeOrder addTimeOrder(long date, EventConsumer dst, EventThread dstThread){
    TimeOrder order = new TimeOrder(date, dst, dstThread);
    addTimeOrder(order);
    return order;
  }

  
  
  public TimeOrder addTimeOrder(long date, Event<TimeEvent.Cmd, Event.NoOpponent> evTime){
    if(evTime !=null){
      Assert.checkMsg (evTime instanceof TimeEvent, "The Event should be a org.vishia.util.EventTimerMng.TimeEvent");
      Assert.checkMsg (evTime.hasDst(), "The Event must have a destination.");
      Assert.checkMsg (!evTime.isOccupied(), "The Event must not be occupied.");
    }
    TimeOrder entry = new TimeOrder(evTime, date);
    addTimeOrder(entry);
    return entry;
  }
  
  
  private void addTimeOrder(TimeOrder order){
    timeEntriesNew.add(order);
    //notify the run process to shorten the wait time.
    synchronized(this){
      if((order.dateEvent - dateCheck) < 0){
        if(bWait){
          notify();
        } else; //not necessary to notify, because it is active yet.
      } else; //let it run, it will be waken up before date.
    }
  }
  
  
  public boolean removeTimeOrder(TimeOrder order){
    if(timeEntries.remove(order)) return true;
    else return timeEntriesNew.remove(order);
  }
  
  
  @Override public void run(){
    run = true;
    while(run){
      long timeAct = System.currentTimeMillis();
      if((timeAct - dateCheck) >=0){
        //woken up after dateCheck, now check all timer events.
        long dateCheck1 = Long.MAX_VALUE;
        Iterator<TimeOrder> iter = timeEntries.iterator();
        while(iter.hasNext()){
          TimeOrder entry = iter.next();
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
      TimeOrder entryNew;
      while( (entryNew = timeEntriesNew.poll()) !=null) {
        if((entryNew.dateEvent - dateCheck) < 0 ){
          dateCheck = entryNew.dateEvent;
        }
        timeEntries.add(entryNew);
      }
      try{
        synchronized(this){
          long timeWait = dateCheck - timeAct;
          if(timeWait > 0                     //if timeWait is more early than now, process it, don't wait.
            && timeEntriesNew.peek() == null  //if any entry in the new-list, process it, don't wait.
            && run
            ){
            bWait = true;
            wait(timeWait);
            bWait = false;
          }
        }
      }catch(InterruptedException exc){
        
      }
    }
    
  }


  
  private void executeTime(TimeOrder entry){
    final Event<TimeEvent.Cmd, Event.NoOpponent> ev = entry.event !=null ? entry.event: 
      new TimeEvent(entry.dst, entry.threadDst);
    ev.occupy(evSource, true);
    ev.sendEvent(TimeEvent.Cmd.Time);
  }
  
  

  @Override public void close() throws IOException {
    run = false;
    synchronized(this){
      if(bWait){
        notify();
      } else; //not necessary to notify, because it is active yet.
    }    
  }
}
