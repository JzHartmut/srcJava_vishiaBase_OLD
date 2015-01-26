package org.vishia.event.test;

import java.io.Closeable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.event.EventTimeOrder;

public class ExecThread extends Thread 
{ 
  
  private ConcurrentLinkedQueue<EventTimeOrder> queueOrders = new ConcurrentLinkedQueue<EventTimeOrder>();
  
  private boolean bRunning;
  private boolean bWaiting;
  
  ExecThread(){ super("execThread"); }  
  
  @Override public void run(){
    bRunning = true;
    do {
      EventTimeOrder order;
      if( (order = queueOrders.poll())!=null) {
        order.doExecute();
      }
      synchronized(this){
        bWaiting = true;
        try{ wait(2000); } catch(InterruptedException exc){}
        bWaiting = false;
      }
    }while(bRunning);
  }
  
  
  public void addOrder(EventTimeOrder order) {
    queueOrders.offer(order);
    synchronized(this){
      if(bWaiting) notify();
    }
  }
  
  public void close(){
    bRunning = false;
  }
  
}

