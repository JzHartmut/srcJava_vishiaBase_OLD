package org.vishia.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**This class contains a list of orders which can be run immediately or delayed. <br>
 * The class has a {@link #step(long)} method which should be invoked cyclically.
 * New  
 * @author Hartmut Schorrig
 *
 */
public class OrderListExecuter
{
  
  
  /**Version and history:
   * <ul>
   * <li>
   * <li>2014-02-23 Hartmut created: The algorithm is copied from {@link org.vishia.gral.base.GralGraphicThread},
   *   this class is the base class of them now. The algorithm is able to use outside of that graphic too.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String sVersion = "2014-02-23";

  
  /**This interface should be implemented by that thread, that cooperates with this order thread.
   */
  public interface ConnectionExecThread {
    
    
    /**Wakes up the execution thread. Usual it is a notify() for a wait. But for graphic or other
     * system depending usage it may be another routine. */
    void wakeup();
    
    /**Returns true so long as the execution thread runs. The timer thread should be terminated
     * if the execution thread has stopped.
     * @return
     */
    boolean isRunning();
  }
  
  
  
  
  private final ConnectionExecThread execThread;
  
  /**The thread which executes delayed wake up. */
  private final Thread threadTimer;


  /**Queue of orders to execute in the graphic thread before dispatching system events. 
   * Any instance will be invoked in the dispatch-loop.
   * See {@link #addDispatchOrder(Runnable)}. 
   * An order can be stayed in this queue for ever. It is invoked any time after the graphic thread 
   * is woken up and before the dispatching of graphic-system-event will be started.
   * An order may be run only one time, than it should delete itself from this queue in its run-method.
   * */
  private final ConcurrentLinkedQueue<OrderForList> queueGraphicOrders = new ConcurrentLinkedQueue<OrderForList>();
  
  /**Queue of orders which are executed with delay yet. */
  private final ConcurrentLinkedQueue<OrderForList> queueDelayedGraphicOrders = new ConcurrentLinkedQueue<OrderForList>();
  
  /**Temporary used instance of delayed orders while {@link #runTimer} organizes the delayed orders.
   * This queue is empty outside running one step of runTimer(). */
  private final ConcurrentLinkedQueue<OrderForList> queueDelayedTempGraphicOrders = new ConcurrentLinkedQueue<OrderForList>();
  
  /**Mutex mechanism: This variable is set true under mutex while the timer waits. Then it should
   * be notified in {@link #addDispatchOrder(OrderForList)} with delayed order. */
  private boolean bTimeIsWaiting;
  
  /**Set if any external event is set. Then the dispatcher shouldn't sleep after finishing dispatching. 
   * This is important if the external event occurs while the GUI is busy in the operation-system-dispatching loop.
   */
  private final AtomicBoolean extEventSet = new AtomicBoolean(false);

  public OrderListExecuter(ConnectionExecThread execThread){
    this.execThread = execThread;
    threadTimer = new Thread(runTimer, "graphictime");
  }

  
  public void start(){
    threadTimer.start();
  }
  
  /** Adds a method which will be called in anytime in the dispatch loop until the listener will remove itself.
   * @deprecated: This method sholdn't be called by user, see {@link OrderForList#addToList(GralGraphicThread, int)}. 
   * @see org.vishia.gral.ifc.GralWindowMng_ifc#addDispatchListener(org.vishia.gral.base.OrderForList)
   * @param order
   */
  public void addDispatchOrder(OrderForList order){ 
    if(order.timeToExecution() >=0){
      queueDelayedGraphicOrders.offer(order);
      synchronized(runTimer){
        if(bTimeIsWaiting){
          runTimer.notify();  
        }
      }
    } else {
      queueGraphicOrders.add(order);
      //it is possible that the GUI is busy with dispatching and doesn't sleep yet.
      //therefore:
      extEventSet.getAndSet(true);
      if(execThread.isRunning()){
        
        execThread.wakeup();  //to wake up the GUI-thread, to run the listener at least one time.
      }
  
    }
  }
  
  
  /**Removes a order, which was called in the dispatch loop.
   * Hint: Use {@link OrderForList#removeFromList(GralGraphicThread)}
   * to remove thread safe with signification. Don't call this routine yourself.
   * @param listener
   */
  public void removeDispatchListener(OrderForList listener)
  { queueGraphicOrders.remove(listener);
    queueDelayedGraphicOrders.remove(listener);
  }
  
  


  
  /**Executes the orders called from any thread which also does other thinks.
   * @param nrofOrders -1 to execute all stored orders, >0 to limit the number of orders to execute.
   * @param millisecAbs The current time
   * @return true then the execThread should not wait
   */
  public boolean step(int nrofOrders, long millisecAbs){
    OrderForList listener;
    extEventSet.set(false); //the list will be tested!
    while( (listener = queueGraphicOrders.poll()) !=null){
    //for(OrderForList listener: queueGraphicOrders){
          //use isWakedUpOnly for run as parameter?
      try{
        listener.doBeforeDispatching(false);
      } catch(Exception exc){
        System.err.println("GralGraphicThread-" + exc.getMessage());
        exc.printStackTrace();
      }
    }
    return extEventSet.get();
  }

  /**Wakes up the {@link #runTimer} queue to execute delayed requests.
   * 
   */
  public void notifyTimer(){
    synchronized(runTimer){
      if(bTimeIsWaiting){
        runTimer.notify();  
      }
    }
  }
  
  
  public Runnable runTimer = new Runnable(){
    @Override public void run(){
      while(execThread.isRunning()){
        int timeWait = 1000;
        boolean bWake = false;
        { OrderForList order;
          while( (order = queueDelayedGraphicOrders.poll()) !=null){
            int timeToExecution = order.timeToExecution();
            if(timeToExecution >=0){
              //not yet to proceed
              if(timeWait > timeToExecution){ timeWait = timeToExecution; }
              queueDelayedTempGraphicOrders.offer(order);
            } else {
              queueGraphicOrders.offer(order);
              bWake = true;
            }
          }
          //delayedChangeRequest is tested and empty now.
          //offer the requ back from the temp queue
          while( (order = queueDelayedTempGraphicOrders.poll()) !=null){
            queueDelayedGraphicOrders.offer(order); 
          }
        }
        if(bWake){
          execThread.wakeup(); //process changeRequests in the graphic thread.
        }
        synchronized(this){
          bTimeIsWaiting = true;
          if(timeWait < 10){
            timeWait = 10; //at least 10 ms, especially prevent usage of 0 and negative values.
          }
          try{ wait(timeWait);} catch(InterruptedException exc){}
          bTimeIsWaiting = false;
        }
      }
    }
  };

}
