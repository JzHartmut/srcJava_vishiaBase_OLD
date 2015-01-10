package org.vishia.util;

import java.io.Closeable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vishia.states.StateMachine;

/**Class to manage {@link TimeOrderBase} with an extra thread.
 * This class can be used in twice forms:
 * <ul>
 * <li>The thread executes the time orders. This may be a thread for @link {@link StateMachine}
 *   especially. 
 * <li>The thread manages only the time for the orders. If the time is expired for any order
 *   that order is copied in a List of ready-to-run orders which can be executed from another thread
 *   which does some other things elsewhere too.  For this working principle the method {@link #step(int, long)} is given
 *   which should be called in the other thread usual cyclically. 
 * </ul>
 * For the second form with an extra execution thread the execution thread may implement the interface @link {@link ConnectionExecThread}. 
 * That is necessary if the thread may sleep a longer time, waits for work. But that is not necessary. 
 * The constructor @link {@link TimeOrderMng#TimeOrderMng(ConnectionExecThread)} can be called with null for this argument. 
 * But in that case the method {@link #close()} have to be called on end of the application.
 * <br><br>
 * This class contains a list of orders which can be run immediately or delayed. <br>
 * The class has a {@link #step(long)} method which should be invoked cyclically.
 * New  
 * @author Hartmut Schorrig
 *
 */
public class TimeOrderMng implements Closeable
{
  
  
  /**Version and history:
   * <ul>
   * <li>2015-01-10 Hartmut chg: Better algorithm with {@link #timeCheckNew}
   * <li>2015-01-10 Hartmut renamed from <code>OrderListExecuter</code>
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
  public final static String sVersion = "2015-01-11";

  
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
  
  
  
  
  public Runnable runTimer = new Runnable(){
    @Override public void run(){ TimeOrderMng.this.runThread(); }
  };

  
  
  /**Bit variable to control some System.out.printf debug outputs. 
   * 
   * 
   * */
  private int debugPrint;
  

  /**The thread which executes delayed wake up. */
  private final Thread threadTimer;



  private final ConnectionExecThread execThread;

  /**Queue of orders to execute in the graphic thread before dispatching system events. 
   * Any instance will be invoked in the dispatch-loop.
   * See {@link #addTimeOrder(Runnable)}. 
   * An order can be stayed in this queue for ever. It is invoked any time after the graphic thread 
   * is woken up and before the dispatching of graphic-system-event will be started.
   * An order may be run only one time, than it should delete itself from this queue in its run-method.
   * */
  private final ConcurrentLinkedQueue<TimeOrderBase> queueOrdersToExecute;
  
  /**Queue of orders which are executed with delay yet. */
  private final ConcurrentLinkedQueue<TimeOrderBase> queueDelayedOrders = new ConcurrentLinkedQueue<TimeOrderBase>();
  
  /**Temporary used instance of delayed orders while {@link #runTimer} organizes the delayed orders.
   * This queue is empty outside running one step of runTimer(). */
  private final ConcurrentLinkedQueue<TimeOrderBase> queueDelayedTempOrders = new ConcurrentLinkedQueue<TimeOrderBase>();
  
  private boolean bThreadRun;
  
  /**timestamp for a new time entry. It is set in synchronized operation between {@link #addTimeOrder(TimeEvent, long)}
   * and the wait in the {@link #run()} operation.
   * 
   */
  private long timeCheckNew;

  private final boolean bExecutesTheOrder;
  
  /**State of the thread, used for debug and mutex mechanism. This variable is set 'W' under mutex while the timer waits. Then it should
   * be notified in {@link #addTimeOrder(TimeOrderBase)} with delayed order. */
  char stateThreadTimer = '?';
  
  
  
  /**Set if any external event is set. Then the dispatcher shouldn't sleep after finishing dispatching. 
   * This is important if the external event occurs while the GUI is busy in the operation-system-dispatching loop.
   */
  private final AtomicBoolean extEventSet = new AtomicBoolean(false);

  /**Creates the Manager for time orders with a given thread which may be for example the graphic thread in a graphic application
   * or another cyclic thread.
   * {@link #close()} is not necessary to invoke if the execThread is given and supports its @link {@link ConnectionExecThread#isRunning()}.
   * It is necessary to invoke {@link #step(int, long)} either cyclically or whenever @link {@link ConnectionExecThread#wakeup()} has woken up
   * the execution thread.
   *  
   * @param execThread maybe null.
   */
  public TimeOrderMng(ConnectionExecThread execThread){
    this.execThread = execThread;
    threadTimer = new Thread(runTimer, "TimeOrderMng");
    bExecutesTheOrder = false;
    queueOrdersToExecute = new ConcurrentLinkedQueue<TimeOrderBase>();
  }

  
  /**Creates the Manager for time orders.
   * It is necessary to invoke {@link #start()} at begin and {@link #close()} on end of an application. 
   * @param executesTheOrder true then the method {@link #step(int, long)} need not be called (not necessary) because the TimeOrder
   *   are executed from the threadTimer already. 
   *   <br>
   *   false then the {@link #step(int, long)} have to be called cyclically from any other thread.
   */
  public TimeOrderMng(boolean executesTheOrder){
    this.execThread = null;
    threadTimer = new Thread(runTimer, "TimeOrderMng");
    bExecutesTheOrder = executesTheOrder;
    if(executesTheOrder){
      queueOrdersToExecute = null;  //unnecessary.
    } else {
      queueOrdersToExecute = new ConcurrentLinkedQueue<TimeOrderBase>();
    }
  }

  
  public void start(){
    if(!bThreadRun) {
      threadTimer.start();
    }
  }
  
  /**Should only be called on end of the whole application to finish the timer thread. This method does not need to be called
   * if a @link {@link ConnectionExecThread} is given as Argument of @link {@link TimeOrderMng#TimeOrderMng(ConnectionExecThread)}
   * and this instance implements the @link {@link ConnectionExecThread#isRunning()} method. If that method returns false
   * then the timer thread is finished too.
   * 
   * @see java.io.Closeable#close()
   */
  @Override public void close(){
    bThreadRun = false;
    notifyTimer();
  }


  /**Adds a method which will be called in anytime in the dispatch loop until the listener will remove itself.
   * @deprecated: This method sholdn't be called by user, see {@link TimeOrderBase#addToList(GralGraphicThread, int)}. 
   * @see org.vishia.gral.ifc.GralWindowMng_ifc#addDispatchListener(org.vishia.gral.base.EventTimeOrder)
   * @param order
   */
  public void addTimeOrder(TimeOrderBase order){ 
    if(order.timeToExecution() >=0){
      queueDelayedOrders.offer(order);
      if(order.timeExecution < timeCheckNew - 20) {
        boolean notified;
        synchronized(runTimer){
          notified = stateThreadTimer == 'W';
          if(notified){
            runTimer.notify();  
          }
        }
        if(notified){
          if((debugPrint & 0x100)!=0) System.out.printf("TimeOrderMng notify %d\n", order.timeExecution - timeCheckNew);
        } else {
          if((debugPrint & 0x200)!=0) System.out.printf("TimeOrderMng not notified because checking %d\n", order.timeExecution - timeCheckNew);
        }
      } else {
        //don't notify because the time order is later than the planned check time (or not so far sooner)
        if((debugPrint & 0x400)!=0) System.out.printf("TimeOrderMng not notified, future %d\n", order.timeExecution - timeCheckNew);
      }
    } else {
      if((debugPrint & 0x800)!=0) System.out.printf("TimeOrderMng yet %d\n", order.timeExecution - timeCheckNew);
      if(bExecutesTheOrder) {
        order.executeOrder();             //executes immediately in this thread.
      } else {
        queueOrdersToExecute.add(order);  //stores to execute contemporarry.
        //it is possible that the GUI is busy with dispatching and doesn't sleep yet.
        //therefore:
        extEventSet.getAndSet(true);
        if(execThread !=null && execThread.isRunning()){
          
          execThread.wakeup();  //to wake up the GUI-thread, to run the listener at least one time.
        }
      }
    }
  }
  
  
  /**Removes a order, which was called in the dispatch loop.
   * Hint: Use {@link TimeOrderBase#removeFromList(GralGraphicThread)}
   * to remove thread safe with signification. Don't call this routine yourself.
   * @param order
   */
  public void removeTimeOrder(TimeOrderBase order)
  { queueDelayedOrders.remove(order);
    if(!bExecutesTheOrder) {queueOrdersToExecute.remove(order); }
  }
  
  

  /**The core run routine for the timer thread.
   * The timer thread is in an delay till 
   */
  private void runThread()
  {
    bThreadRun = true;
    while(bThreadRun && (execThread == null || execThread.isRunning())){
      //int timeWait = 1000;
      stateThreadTimer = 'c';
      long timeNow = System.currentTimeMillis();
      timeCheckNew = timeNow + 10000;
      boolean bWake = false;
      { TimeOrderBase order;
        while( (order = queueDelayedOrders.poll()) !=null){
          if(order.timeExecution < timeCheckNew){
            timeCheckNew = order.timeExecution;   //prior to 
          }
          //int timeToExecution = order.timeToExecution();
          if(order.timeExecution > timeNow) { //timeToExecution >=0){
            //not yet to proceed
            //if(timeWait > timeToExecution){ timeWait = timeToExecution; }
            queueDelayedTempOrders.offer(order);
          } else {
            if(bExecutesTheOrder) {
              order.executeOrder();             //executes immediately in this thread.
            } else {
              queueOrdersToExecute.offer(order);  //stores to execute contemporarry.
              bWake = execThread != null;
            }
          }
        }
        //delayedChangeRequest is tested and empty now.
        //offer the requ back from the temp queue
        while( (order = queueDelayedTempOrders.poll()) !=null){
          queueDelayedOrders.offer(order); 
        }
      }
      if(bWake){
        execThread.wakeup(); //process changeRequests in the graphic thread.
      }
      int timeWait = (int) (timeCheckNew - System.currentTimeMillis());  //the time now to the next order.
      if(timeWait > 2) { //for 2 milliseconds don't wait!
        if((debugPrint & 0x0001)!=0) System.out.printf("TimeOrderMng wait %d\n", timeWait);
        synchronized(runTimer){
          stateThreadTimer = 'W';
          if(timeWait < 10){
            timeWait = 10; //at least 10 ms, especially prevent usage of 0 and negative values.
          }
          try{ runTimer.wait(timeWait);} catch(InterruptedException exc){}
          stateThreadTimer = 'r';
        }
      } else {
        //else: check the orders newly. One of them is near to execute.
        if((debugPrint & 0x0002)!=0) System.out.printf("TimeOrderMng not wait %d\n", timeWait);
      }
    } //while runs
    stateThreadTimer = 'f';
  }
  
  
  

  
  /**Executes the orders called from any thread which also does other things.
   * @param nrofOrders -1 to execute all stored orders, >0 to limit the number of orders to execute.
   * @param millisecAbs The current time
   * @return true then the execThread should not wait
   */
  public boolean step(int nrofOrders, long millisecAbs){
    if(bExecutesTheOrder) return false;  //does nothing.
    TimeOrderBase listener;
    extEventSet.set(false); //the list will be tested!
    while( (listener = queueOrdersToExecute.poll()) !=null){
    //for(OrderForList listener: queueGraphicOrders){
          //use isWakedUpOnly for run as parameter?
      try{
        listener.executeOrder();
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
      if(stateThreadTimer == 'W'){
        runTimer.notify();  
      }
    }
  }
  
  
  
}
