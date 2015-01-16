package org.vishia.event;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vishia.states.StateMachine;
import org.vishia.util.Assert;
import org.vishia.util.InfoAppend;

/**Class to manage all events, especially {@link EventTimeout} with an extra thread.
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
public class TimeOrderMng implements EventThreadIfc, Closeable, InfoAppend
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
    
    
    /**Adds one order as Event object.
     * @param event
     */
    void addEvent(EventObject event);
    
    /**Wakes up the execution thread. Usual it is a notify() for a wait. But for graphic or other
     * system depending usage it may be another routine. */
    void wakeup();
    
    /**Returns true so long as the execution thread runs. The timer thread should be terminated
     * if the execution thread has stopped.
     * @return
     */
    boolean isRunning();
  }
  
  
  
  


  /**Bit variable to control some System.out.printf debug outputs. 
   * 
   * 
   * */
  private int debugPrint;
  
  protected final String threadName;

  /**The thread which executes delayed wake up. */
  private Thread threadTimer;



  private final ConnectionExecThread execThread;

  /**Queue of orders to execute in the graphic thread before dispatching system events. 
   * Any instance will be invoked in the dispatch-loop.
   * See {@link #addTimeOrder(Runnable)}. 
   * An order can be stayed in this queue for ever. It is invoked any time after the graphic thread 
   * is woken up and before the dispatching of graphic-system-event will be started.
   * An order may be run only one time, than it should delete itself from this queue in its run-method.
   * */
  private final ConcurrentLinkedQueue<EventTimeout> queueOrdersToExecute;
  
  /**Queue of orders which are executed with delay yet. */
  private final ConcurrentLinkedQueue<EventObject> queueEvents = new ConcurrentLinkedQueue<EventObject>();
  
  /**Queue of orders which are executed with delay yet. */
  private final ConcurrentLinkedQueue<EventTimeout> queueDelayedOrders = new ConcurrentLinkedQueue<EventTimeout>();
  
  /**Temporary used instance of delayed orders while {@link #runTimer} organizes the delayed orders.
   * This queue is empty outside running one step of runTimer(). */
  private final ConcurrentLinkedQueue<EventTimeout> queueDelayedTempOrders = new ConcurrentLinkedQueue<EventTimeout>();
  
  private final ArrayList<EventConsumer> listConsumer = new ArrayList<EventConsumer>();
  
  private int[] consumerShouldRun = new int[20];  //up to 320 consumer. TODO increase with greater listConsumer. 

  private boolean bThreadRun;
  
  /**timestamp for a new time entry. It is set in synchronized operation between {@link #addTimeOrder(TimeEvent, long)}
   * and the wait in the {@link #run()} operation.
   * 
   */
  private long timeCheckNew = System.currentTimeMillis() + 1000 * 3600 * 24;

  /**The time on start waiting*/
  private long timeSleep;

  
  private final boolean bExecutesTheOrder;
  
  /**State of the thread, used for debug and mutex mechanism. This variable is set 'W' under mutex while the timer waits. Then it should
   * be notified in {@link #addTimeOrder(EventTimeout)} with delayed order. */
  char stateThreadTimer = '?';
  
  /**Set if any external event is set. Then the dispatcher shouldn't sleep after finishing dispatching. 
   * This is important if the external event occurs while the GUI is busy in the operation-system-dispatching loop.
   */
  private final AtomicBoolean extEventSet = new AtomicBoolean(false);

  private boolean startOnDemand;
  
  private int ctWaitEmptyQueue;
  
  protected int maxCtWaitEmptyQueue = 5;
  
  
  private boolean preserveRecursiveInfoAppend;

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
    this.threadName = "TimeOrderMng";
    //threadTimer = new Thread(runTimer, threadName);
    bExecutesTheOrder = false;
    queueOrdersToExecute = new ConcurrentLinkedQueue<EventTimeout>();
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
    this.threadName = "TimeOrderMng";
    //threadTimer = new Thread(runTimer, threadName);
    bExecutesTheOrder = executesTheOrder;
    if(executesTheOrder){
      queueOrdersToExecute = null;  //unnecessary.
    } else {
      queueOrdersToExecute = new ConcurrentLinkedQueue<EventTimeout>();
    }
  }

  
  public TimeOrderMng(String threadName)
  {
    this.threadName = threadName;
    queueOrdersToExecute = null;  //unnecessary.
    execThread = null;
    bExecutesTheOrder = true;
  }
  

  
  public void start(){ startThread(); }
  

  /**Creates and starts the thread. If this routine is called from the user, the thread runs
   * till the close() method was called. If this method is not invoked from the user,
   * the thread is created and started automatically if {@link #storeEvent(EventCmdType)} was called.
   * In that case the thread stops its execution if the event queue is empty and about 5 seconds
   * are gone.  */
  public void startThread(){ 
    if(threadTimer == null && !bThreadRun) {
      threadTimer = new Thread(runTimer, threadName);
      startOnDemand = false;
      threadTimer.start(); 
    }
  }
  

  
  /**Activates one time running of the registered {@link EventConsumer} without an event.
   * @param ixRegisteredConsumer
   * @see #registerConsumer(EventConsumer)
   */
  public synchronized void shouldRun(int ixRegisteredConsumer) {
    int ix = ixRegisteredConsumer >>5;
    int bit = 1 << (ixRegisteredConsumer - ix);
    consumerShouldRun[ix] |= bit;
    startOrNotify();
  }
  
  
  
  /**Stores an event in the queue, able to invoke from any thread.
   * @param ev
   */
  public void storeEvent(EventObject ev){
    if(ev instanceof EventWithDst) { ((EventWithDst)ev).stateOfEvent = 'q'; }
    queueEvents.offer(ev);
    startOrNotify();
  }
  

  private void startOrNotify(){
    if(threadTimer == null){
      startThread();
      startOnDemand = true;
    } else {
      synchronized(runTimer){
        if(stateThreadTimer == 'W'){
          runTimer.notify();
        } else {
          //stateOfThread = 'c';
        }
      }
    }
  }


  /**Registers a consumer that can be run in this thread without storing an event.
   * @param obj the consumer, it should be override {@link EventConsumer#shouldRun(boolean)} which should call 
   *   {@link #shouldRun(int)} of this class with the index which is returned from this method. See example in 
   *   {@link org.vishia.states.StateMachine#shouldRun(boolean)}.
   * @return The index of registering.
   */
  public synchronized int registerConsumer(EventConsumer obj) {
    listConsumer.add(obj);
    return listConsumer.size()-1;  //the add position.
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
   * @deprecated: This method sholdn't be called by user, see {@link EventTimeout#addToList(GralGraphicThread, int)}. 
   * @see org.vishia.gral.ifc.GralWindowMng_ifc#addDispatchListener(org.vishia.gral.base.EventTimeOrder)
   * @param order
   */
  public void addTimeOrder(EventTimeout order){ 
    long delay = order.timeToExecution(); 
    if(delay >=0){
      queueDelayedOrders.offer(order);
      long delayAfterCheckNew = order.timeExecution - timeCheckNew;
      if((delayAfterCheckNew) < -2) {  //an imprecision of 2 ms are admissible, don't wakeup because calculation imprecisions.
        timeCheckNew = order.timeExecution;  //earlier.
        boolean notified;
        synchronized(runTimer){
          notified = stateThreadTimer == 'W';
          if(notified){
            runTimer.notify();  
          }
        }
        if(notified){
          if((debugPrint & 0x100)!=0) System.out.printf("TimeOrderMng notify %d\n", delayAfterCheckNew);
        } else {
          if((debugPrint & 0x200)!=0) System.out.printf("TimeOrderMng not notified because checking %d\n", delayAfterCheckNew);
        }
      } else {
        //don't notify because the time order is later than the planned check time (or not so far sooner)
        if((debugPrint & 0x400)!=0) System.out.printf("TimeOrderMng not notified, future %d\n", delayAfterCheckNew);
      }
    } else {
      if((debugPrint & 0x800)!=0) System.out.printf("TimeOrderMng yet %d\n", delay);
      if(bExecutesTheOrder) {
        executeOrEnqueuTimeOrder(order);
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
   * Hint: Use {@link EventTimeout#removeFromList(GralGraphicThread)}
   * to remove thread safe with signification. Don't call this routine yourself.
   * @param order
   */
  public void removeTimeOrder(EventTimeout order)
  { boolean found = queueDelayedOrders.remove(order);
    if(!found){ removeFromQueue(order); }  //it is possible that it hangs in the event queue.
    if(!bExecutesTheOrder) {queueOrdersToExecute.remove(order); }
  }
  
  
  /**Removes this event from its queue if it is in the queue.
   * If the element is found in the queue, it is designated with stateOfEvent = 'a'
   * @param ev
   * @return true if found.
   */
  public boolean removeFromQueue(EventObject ev){
    boolean found = queueEvents.remove(ev);
    if(found && ev instanceof EventWithDst){ 
      ((EventWithDst)ev).stateOfEvent = 'a'; 
    }
    return found;
  }
  

  
  
  /**Applies an event from the queue to the destination in the event thread. 
   * This method should be overridden if other events then {@link EventCmdType} are used because the destination of an event
   * is not defined for a java.util.EventObject. Therefore it should be defined in a user-specific way in the overridden method.
   * This method is proper for events of type {@link EventCmdType} which knows their destination.
   * @param ev
   */
  protected void applyEvent(EventObject ev)
  {
    if(ev instanceof EventWithDst){
      EventWithDst event = (EventWithDst) ev;
      event.stateOfEvent = 'e';
      event.notifyDequeued();
      try{
        //event.donotRelinquish = false;   //may be overridden in processEvent if the event is stored in another queue
        event.stateOfEvent = 'r';
        event.evDst().processEvent(event);
      } catch(Exception exc) {
        CharSequence excMsg = Assert.exceptionInfo("EventThread.applyEvent exception", exc, 0, 50);
        System.err.append(excMsg);
        //exc.printStackTrace(System.err);
      }
      if(event.stateOfEvent == 'r') {  //doNotRelinquish was not invoked inside processEvent().
        event.relinquish();  //the event can be reused, a waiting thread will be notified.
      }
    }
  }
  
  
  

  /**
   * @return true if any action was done because an event was found. false if the queue is empty.
   */
  private boolean checkEventAndRun()
  { boolean processedOne = false;
    try{ //never let the thread crash
      EventObject event;
      if( (event = queueEvents.poll()) !=null){
        this.ctWaitEmptyQueue = 0;
        synchronized(this){
          if(stateThreadTimer != 'x'){
            stateThreadTimer = 'b'; //busy
          }
        }
        if(stateThreadTimer == 'b'){
          applyEvent(event);
          processedOne = true;
        }
      } else {
        //all events processed:
        for(int ix = 0; ix < consumerShouldRun.length; ++ix){
          int bits = consumerShouldRun[ix];
          int ixConsumer = ix <<5;
          int bitReset = 0xfffffffe;
          while(bits !=0){
            if( (bits & 1)!=0 ){
              EventConsumer consumer = listConsumer.get(ixConsumer);
              if(consumer !=null){
                consumer.processEvent(null);  //run it without event. To execute conditions.
                processedOne = true;
              }
            }
            synchronized(this){
              consumerShouldRun[ix] &= bitReset;
            }
            bitReset <<=1;
            bits = (bits >>1) & 0x7fffffff;  //shift without sign!
          }
        }
      }
    } catch(Exception exc){
      CharSequence text = Assert.exceptionInfo("EventThread unexpected Exception - ", exc, 0, 50);
      System.err.append(text);
    }
    return processedOne;
  }
  
  

  private void executeOrEnqueuTimeOrder(EventTimeout ev) {
    if(ev.evDstThread !=null) { //any other thread to execute.
      //the order should be executed in any other thread, store it there:
      ev.evDstThread.storeEvent(ev);
    } else if(ev.evDst !=null){
      ev.evDst.processEvent(ev);  //especially if it is a timeout.
    } else if(ev instanceof TimeOrderBase){
      ((TimeOrderBase)ev).execute();   //executes immediately in this thread.
    }
  }
  
  
  
  
  private int checkTimeOrders(){
    boolean bWake = false;
    int timeWait = 10000; //10 seconds.
    timeCheckNew = System.currentTimeMillis() + timeWait;  //the next check time in 10 seconds.
    { EventTimeout order;
      long timeNow = System.currentTimeMillis();
      while( (order = queueDelayedOrders.poll()) !=null){
        long delay = order.timeExecution - timeNow; 
        if((delay) < 3){  //if it is expired in 2 milliseconds, execute now.
          if(bExecutesTheOrder) {
            executeOrEnqueuTimeOrder(order);
            timeNow = System.currentTimeMillis();  //it may be later.
          } else {
            queueOrdersToExecute.offer(order);  //stores to execute contemporarry.
            bWake = execThread != null;
          }
        }
        else {
          //not yet to proceed
          if(delay < timeWait) {
            timeCheckNew = order.timeExecution;  //earlier
            timeWait = (int) delay;
          }
          queueDelayedTempOrders.offer(order);
        }
      }
      //delayedChangeRequest is tested and empty now.
      //copy the non-expired orders back to queueDelayedOrders.
      while( (order = queueDelayedTempOrders.poll()) !=null){
        queueDelayedOrders.offer(order); 
      }
    }
    if(bWake){
      execThread.wakeup(); //process changeRequests in the graphic thread.
    }
    return timeWait;
  }
  
  
  
  /**The core run routine for the timer thread.
   * The timer thread is in an delay till
   * @return time to wait. 
   */
  private int stepThread()
  {
    boolean bExecute;
    int timeWait;
    do {
      stateThreadTimer = 'c';
      timeSleep = System.currentTimeMillis();
      timeWait = (int)(timeCheckNew - timeSleep);
      if(timeWait < 0){ //firstly check all time orders if one of them is expired.
        timeWait = checkTimeOrders();
      }
      bExecute = false;
      while(checkEventAndRun()){
        bExecute = true;
      }
      if(bExecute){
        //else: check the orders and events newly. One of them may near to execute.
        if((debugPrint & 0x0002)!=0) System.out.printf("TimeOrderMng not wait %d\n", timeWait);
      }
      //if any event was executed, it should be supposed that 2.. milliseconds have elapsed.
      //therefore check time newly. don't wait, run in this loop.
    } while(bExecute);
    //wait only the calculated timeWait if no additional event has executed.
    return timeWait;
  }
  
  
  
  /**Instance as Runnable contains invocation of {@link TimeOrderMng#stepThread()}
   * and the {@link Object#wait()} with the calculated timeWait.
   * 
   */
  //NOTE: On debugging and changing the stepThread can be repeated because the CPU stays in the wait or breaks here.
  // This routine cannot be changed on the fly.
  public Runnable runTimer = new Runnable(){
    @Override public void run(){ 
      bThreadRun = true;
      stateThreadTimer = 'r';
      while(stateThreadTimer == 'r' && bThreadRun && (execThread == null || execThread.isRunning())){
        int timeWait = TimeOrderMng.this.stepThread();
        if((debugPrint & 0x0001)!=0) System.out.printf("TimeOrderMng wait %d\n", timeWait);
        if(timeWait <2){
          timeWait = 2;  //should not 0  
        }
        synchronized(runTimer){
          stateThreadTimer = 'W';
          //====>wait
          try{ runTimer.wait(timeWait);} catch(InterruptedException exc){}
          if(stateThreadTimer == 'W'){ //can be changed while waiting, set only to 'r' if 'W' is still present
            stateThreadTimer = 'r';
          }
        } //synchronized
      } //while runs
      stateThreadTimer = 'f';
    }
  };

  /**Returns true if the current thread is the thread which is aggregate to this EventThread.
   * It means the {@link #run()} method has called this method.
   * @return false if a statement in another thread checks whether this EventThread runs.
   */
  public boolean isCurrentThread() {
    return threadTimer == Thread.currentThread();
  }
  
  public char getState(){ return stateThreadTimer; }


  
  /**Executes the orders called from any thread which also does other things.
   * @param nrofOrders -1 to execute all stored orders, >0 to limit the number of orders to execute.
   * @param millisecAbs The current time
   * @return true then the execThread should not wait
   */
  public boolean step(int nrofOrders, long millisecAbs){
    if(bExecutesTheOrder) return false;  //does nothing.
    EventTimeout listener;
    extEventSet.set(false); //the list will be tested!
    boolean bSleep = true;
    while( (listener = queueOrdersToExecute.poll()) !=null){
    //for(OrderForList listener: queueGraphicOrders){
          //use isWakedUpOnly for run as parameter?
      try{
        ((TimeOrderBase)listener).execute();
      } catch(Exception exc){
        System.err.println("GralGraphicThread-" + exc.getMessage());
        exc.printStackTrace();
      }
      bSleep = false;
    }
    return !bSleep;  //extEventSet.get();
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
  
  
  
  @Override public CharSequence infoAppend(StringBuilder u) {
    if(u == null) { u = new StringBuilder(); }
    u.append("Thread ");
    u.append("; ");
    return u;
  }

  /*no: Returns only the thread name. Note: Prevent recursively call for gathering info.
   */
  @Override public String toString() { if(preserveRecursiveInfoAppend) return threadName; else return infoAppend(null).toString(); } 
  
  
}
