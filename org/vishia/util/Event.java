package org.vishia.util;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.vishia.bridgeC.MemC;

/**Base class for all events in a event driven software or for communications.
 * Events may contain data. Special data are contained in derived classes of this. The type of the event
 * can be checked using <code>event instanceof DerivedEvent</code>. The id may be only a hint.
 * <br><br>
 * <a href="#whatis">What is a event driven software</a><br>
 * <a href="#usage"> Where is this Event class able to use</a><br>
 * <a href="#lifecycle">Life cycle of an event object</a><br>
 * <a href="#conflicts"> Conflicts in usage of Event objects</a><br>
 * <br><br>
 * <a name="whatis" />
 * <b>What is a event driven software</b>?<br>
 * There are 4 types of organization of the cooperation in a multi thread system or in a distributed system:
 * <ol>
 * <li><b>request, wait and notify</b>: The action which should be done by another thread or in another remote device
 *   is requested. Then the requesting thread waits for the execution (for response, for answer, for notify). 
 *   
 * <li><b>request and poll</b>: The action is requested. Then the requesting thread does any other action, 
 *   it can't be used the result from the request yet.
 *   But the thread polls cyclically (may be for some milliseconds or longer) the result of the requested order.
 * <li><b>callback</b>: The action which should be done by another thread or in another remote device
 *   is requested. With the request a callback reference is given. If the request is executed (received response etc),
 *   the other thread calls the callback method. In the callback method the results of the request can be processed.
 * <li><b>event driven</b>: The action is requested usual by sending an event to the executer instance. 
 *   The requested thread does nothing with any response in the immediate programming environment. 
 *   It requests only. But the executer instance may send an event back if it has finished the execution
 *   or if any other messages should be given.<br>
 *   Events are stored stored in a queue firstly. The queue will be checked by the 'event execution thread'.
 *   It is possible to have only one event execution thread in the system. In this case the execution will be done
 *   in the same thread, but to any later time (if the event is gotten from the queue).<br>
 *   There may be some more event execution threads, for example a thread which organizes communication.
 *   The execution of any request and the execution of the response is binded to the thread, 
 *   which polls the receivers event queue.<br>
 *   It is possible too to request any action non-event-driven, maybe by sending a request via Ethernet communication
 *   or adequate. But the response may be event-driven. The receiver thread of the communication
 *   puts the event in the queue. The execution of the response is done in the event-queue-thread.
 *   This is a proper mechanism to decouple activity. If the receiver thread would execute anything
 *   in another module, this execution may be hanging or may cause any exceptions which disturbs 
 *   the receiving thread so the whole receiving functionality may hang.  
 * </ol>
 * This 4 types may be present in a users program. The following remarks should be recognized:
 * <ol>
 * <li><b>request wait and notify</b>: This is the simplest and basic methodology for inter-thread-cooperation,
 *    supported by the basic operations Object.wait() and Object.notify() as basic feature provided in Java.
 *    <br>
 *    The waiting thread can't execute any other things while waiting. A timeout of waiting may be good 
 *    for exception situations. 
 *    <br>
 *    A user may use this methodology for simple functionalities in the application software. But it needs
 *    an extra thread if other things should be done also. Especially in a graphic environment (which is event driven
 *    for graphic operations), a wait for less milliseconds may be ok, but if the response is delayed
 *    or it isn't received, the whole graphic execution is hanging on. (Do you know this effect in some
 *    applications on PC operations systems?).   
 *    <br>Therefore it may be recommended that this methodology should not be used as the favorite solution.
 *    in application programming.
 *    <br>The wait and notify is proper for basic implementations for example to organize a event driven system.
 *    The thread waits for events in the queue. If any other thread put an event in the queue, it notifies
 *    the waiting thread. That event execution thread has only one mission: providing the events to its destinations.
 *    If there are no events, it should wait and sleep.
 * <li><b>request and poll</b>: This methodology may be used in cyclic execution systems for example for
 *   analog-like controlling of signals. The cyclic execution tests results from communication or inter-thread-communication
 *   in any cycle of execution. 
 * <li><b>callback</b>: This is a simple methodology to prevent waiting. But the callback method is executed 
 *   in the other thread, which is responsible to the execution of the request or which is the receiving thread
 *   of any communication with a remote device. Maybe some thread mutex mechanism should be necessary.
 * <li><b>event driven</b>: This methodology needs a system support in user space: The event execution thread. 
 *   That thread polls the event queue. It waits if the queue is empty. If an event is gotten, it is
 *   distributed to its destination. The destination is any instance of any class which offers a the 
 *   event processing method. 
 *   <br>
 *   The event driven methodology can be esteemed as the best for complex systems.     
 * </ol>  
 * <br><br>
 * <b>Usage of this Event class</b>:<br>
 * This class supplies a common use-able base for event driven programming. It hasn't any dependencies 
 * to other classes of this package or component except {@link EventConsumer} and {@link EventThread}. 
 * (It may be good if this class is a member of the java/lang package).
 * The event mechanisms are well known and used in some Java applications for example in graphic (AWT, SWT)
 * or in special frameworks. That event classes are similar, but they are embedded in their environment.
 * If they are used, unnecessary dependency are caused. <br>
 * This class supports event driven programming in the language C too. It is designed to use in simple C applications
 * which where translated with the vishia Java2C translator. Thats why the usage of inheritance 
 * for any kind of events isn't the first choice. This Event class contains enough data for simple and usual
 * usages.
 * <br><br>
 * <b>Usage for callback</b>:<br>
 * A request should be called with an reference to an instance to this Event class as parameter. 
 * This Event class contains the reference to the callback instance {@link #callback}. 
 * The callback instance implements the {@link EventConsumer}-interface. In this manner the request receiver
 * can invoke a callback using {@link #callback}.{@link EventConsumer#processEvent(Event)}. Note that this callback
 * is executed in the request receiver's thread.
 * <br><pre>
 *   Thread                     class             another Thread             
 *  Requester               Request preparer     Request executer                      
 *      |                          |                    |                       
 *      |->request(data, Event)--->|-->any queue------->|                                              
 *      |                          | (Event stored)     |                      
 *      |                          |                    |
 *      |                                        {executes request}
 *      |                                               |
 *      |                                       {fills Event with data}
 *      |<-------------Event.dst.processEvent(ev)-------|
 *      |                  it is a callback             |
 * </pre>
 * <br>
 * <br>
 * <b>Usage for event driven mechanism</b>:<br>
 * The difference between callback and event driven is marginal but substantial. Instead invocation of
 * a callback in the other thread, the event is put in a queue. The event execution thread polls the event
 * and invokes the callback method to the destination class to consume the event. The difference is only:
 * The event is stored in a queue, the callback is invoked by another thread. But this difference is
 * substantial.
 * <br><pre>
 *   Thread    Thread           class             another Thread             
 *  Requester EventThread   Request preparer     Request executer                      
 *      |         |                |                    |                       
 *      |->request(data, Event)--->|-->any queue------->|                                              
 *      |         |                | (Event stored)     |                      
 *      |         |                |                    |
 *      |         |                              {executes request}
 *      |         |                                     |
 *      |         |                             {fills Event with data}
 *      |         |<---Event.dst.processEvent(ev)-------|
 *      |         |        it is a callback             |
 *      |         |                                     |
 *      |         |---------------------+               |
 *      |<---Event.dst.processEvent(ev)-+      
 *      |     it is a callback              
 *      |         |                                     |
 *      |         |                                     |
 * </pre>
 * <br><br>
 * <b>Request and response in an event driven system</b>
 * In a common event driven system their isn't a thinking of 'request' and 'response'. Any event inducing
 * is independent of any other event. But usual mechanism uses 'request' and the associated 'response'.
 * For that methodology it is possible to give the event instance as parameter of the request already.
 * The event parameter contains the {@link #callback} for the response. This may be a instance in knowledge
 * of the requesting instance (maybe an inner class for implementation of {@link EventConsumer})
 * or the requesting instance itself. So the requested instance should not be known the requester permanently,
 * but only for the pending request.
 * <br><br>
 * <b>Remote device, communication</b>:<br>
 * If any action should be executed in any other device which is connected via any communication maybe
 * via Ethernet, or serial or any other special, the request for the execution has to be sent. This request
 * can contain this Event class as parameter for the response. <br>  
 * The execution occurs independently in the other device then. The communication may be work with
 * request and response. If the action is received and accepted, a response will be sent back which may
 * contain any answer data. The response of the device is received by the receiver thread of the communication.
 * <br>
 * The communication should store and manage a association between request to the other device and response
 * from the device. The association may be realized using a sequence, order or commission number. If the
 * associated response is received, the Event of the request is found out using the commission or sequence
 * number. Then the communication-receive-thread can use the Event instance to invoke the callback
 * or put the event in the queue of dst.
 * <br><pre>
 *   Thread                     class                Thread             
 *  Requester               Communication-tx     Communication-rx       Remote device
 *      |                          |                    |                     |
 *      |->request(data, Event)--->|---->telg: request to device(data)------->|(receives commission nr)
 *      |                          | (Event stored)     |                     |
 *      |                          |                    |               {executes request}
 *      |                                               |                     |
 *      |                                               |<--telg: response----|(uses same commission nr)
 *      |                                    {searches Event                  |
 *      |                                    {fills Event with data}          |
 *      |<--------------------response(Event)-----------|                     |
 *      |callback or Event queue                        |                     |
 *      |                                               |                     |
 *      |                                               |                     |
 * </pre>
 * <br>
 * <br>
 * <br>
 * <a name="usage" />
 * <b>Where is this Event class able to use</b>:
 * <ul>
 * <li>As item in the event queue using the {@link EventThread}
 * <li>As callback item for communication for example in {@link FileRemote}.
 * </ul> v
 * <br>
 * <br>
 * <br>
 * <a name="lifecycle" />
 * <b>Life cycle of an event object</b>:<br><br>
 * The standard and simple operation with an event object is:
 * <pre>
 *   src
 *    |                                       dst
 *    +ev = new Event(dst)                     |
 *    |                                        |
 *    |-------------ev.sendEvent(cmd)--------->|
 *    |                                        + read data from event
 *                                             + ev.consumed() 
 *                                             |
 * </pre>
 * The event object is created on demand with {@link #Event(Object, EventConsumer, EventThread)}. Then it is {@link #sendEvent(int)}
 * to the destination either with direct call of {@link EventConsumer#processEvent(Event)} or it is stored in the queue of the
 * {@link EventThread} and then processed. <br>
 * While processing, the data are gotten from the event. After them it is no more necessary. One should call {@link #consumed()}.
 * Either the Garbage Collector removes the event object from memory because it is no more referenced, or 
 * the {@link #consumedRetention()} method calls {@link MemC#free(Object)} internally for C/C++ usage without Garbage Collection.
 * <br><br>
 * It is a convention to <b>process events without blocking</b> (waiting for any resource) <b>in a short time</b>. Events are often used in a 
 * cyclic loop maybe for a statemachine operation or inside interrupts. Therefore the calculation time between dequeue the event
 * and start the {@link EventConsumer#processEvent(Event)} and the end of this routine respectively the time to call 
 * {@link #consumed()} or {@link #consumedRetain()} is short. Short means a few milliseconds or less.
 * 
 * <br><br>
 * <b>Static event instances</b>:
 * <br><br>
 * Another approach is to use static instances. If a functionality between 2 classes should be used only one time simultaneously,
 * then the usage of a static event object is the same like the allocation of a new instance, relinquish it
 * and allocate a new one after them. If the functionality should be processed more as one time in the same time, 
 * one static instance can't  used, of course. Static instances have disadvantages for free usages because a multiple 
 * occupancy should be excluded. Therefore the dynamic data handling in Java with Garbage Collection is an important 
 * advantage of this language and programming system. But the dynamic approach has a disadvantage: The amount of
 * usage of any functionality is not limited, till the memory crashes. If the memory has crashed, all other functionality
 * of the application may be blocked too. Therefore in safety critical applications a dynamic usage of memory may be prohibited.
 * <br><br>
 * <pre>
 *   src
 *    |
 *    +this.ev = new Event()
 *    |
 *    ~
 *    ~
 *    |                                       dst
 *    +ev.occupy(dst)                          |
 *    |                                        |
 *    |-------------ev.sendEvent(cmd)--------->|
 *    |                                        + read data from event
 *    |                                        + ev.consumed() 
 *    |                                        |
 *    |
 *    |                               dst2
 *    +ev.occupy(dst2)                  |
 *    |                                 |
 *    |------ev.sendEvent(cmd)--------->|
 *    |                                 + read data from event
 *    |                                 + ev.consumed() 
 *    |                                 |
 *    |
 * </pre>
 * 
 * If an event object should be used statically, it should be created with {@link #Event()} at startup time
 * and referenced in the source.
 * If the event object is needed, the method {@link #occupy(Object, EventConsumer, EventThread)} has to be called 
 * instead new {@link #Event(Object, EventConsumer, EventThread)}. This method returns false if the event object is in use.
 * The handling on the destination side is the same. After read data from the event it should be described as
 * {@link #consumedRetention()}. Then the occupying is relinquished. An internal flag {@link #isStatic} prevents the deletion from
 * the memory for C/C++. Because the event is referred in the source, it isn't removed by the Garbage Collector in Java. 
 *  
 * <br><br> 
 * <b>Re-use of an Event object in the context of one functionality</b>
 * <br><br>
 * Independent of the decision of static Event objects, an Event can be re-used inside the context of functionality.
 * <pre>
 *         ....for one functionality........
 *   src
 *    |                                       dst
 *    |ev = new Event(dst)                     |
 *    |----------ev.sendEvent(cmd)------------>|
 *    |                                        + use the ev
 *    |                                        + ev.consumedRetain();
 *    |                                        |
 *    |                                        + ev.reserve()
 *    |                                        |----ev.sendEvent(cmd)-----+
 *    |                                        |  (reused in destination) |
 *    |                                        |<-------------------------+
 *    |                                        + ev.consumedRetain();
 *    |<----------callback---------------------|
 *    |                                        |
 *    +ev.reserve()                            |
 *    |----------ev.sendEvent(cmd)------------>|  
 *    |  (reuse the same event in src after    +ev.consumed() 
 *    |   recognize a specific callback)       |
 * </pre>
 * That reusing is only done for the same destination and in the context of one functionality. 
 * The destination inside the event object is not changed. Because it is done under the responsibility
 * of the destination itself, it is a safe operation. The destination programming knows that the event is not used anymore. 
 * It is the same operation to relinquish an instance and allocate a new one or to reuse it.
 * <br><br>
 * The reusing can be done also in interplay between the source and destination. An event may send back to the source especially
 * by using the {@link #callbackEvent()} mechanism. The source may assume that the event from source to destination 
 * which is referred in the callback event can be used once more. That property should be declared in the interface.
 * <br><br>
 * <ul>
 * <li>After reading the data from the event {@link #consumedRetain()}. Therewith it is not relinquished but it is designated
 *   as free for next usage.
 * <li>Now either inside the destination or inside the source the event can be reused. If the destination sends a callback
 *   and the event is referred in the callback event it should be used from the source for the answer after callback.
 *   The event is reused inside the interaction between source and destination.
 * <li>The event can be reused inside the destination especially for state switch stimuli inside a state machine.
 * </ul>
 * In both cases the programming of the destination knows that the event is reused, therefore {@link #consumedRetain()}
 * instead {@link #consumed()} is called. Note that both programming algorithm may be independent, a library and an
 * application, or independent modules, or reusing of software.
 * <br><br>
 * Before re-using the Event object after {@link #consumedRetain()} one should invoke {@link #reserve()} before data are
 * stored in the event. That prevents a multiple usage.
 * <br>
 * <br>
 * <br>
 * <a name="conflicts" />
 * <b>Conflicts in usage of Event objects</b>
 * <br>
 * Already the simple usage of events with new allocation without reusing may be conflictual. It may be possible that 
 * the destination hangs for any other reason, and the source sends events to it. Because there is no answer, 
 * the source sends once more. More and more event objects are created, and the destinations queue will be filled with them.
 * There isn't a mechanism to detect that situation, and the application will crash somewhere along the way.
 * <br><br>
 * In some cases, an event is supposed to be sent only once in the same time from the same source to one destination. 
 * In that case a static Event instance is more proper.
 * Therewith the application detects that the event will be used twice and more because there is only one Event object.
 * If there is no answer, it is conceivable that the event was sent 
 * to the destination, but not processed there because the destination hangs. It is possible too that the event was not
 * processed in this time because it needs more time to do that. The application should treat the situation. 
 * That may not be a necessity of the static usage of the event object but a necessity of the application. 
 * <br>
 * If the call should be repeated, the previous event should be removed from the queue if it
 * hangs there and it is not dequeued. That can be done by invoking {@link #forceRelinquish()} or {@link #reserveRecall(int)}.
 * Both routines tries to dequeue the event. If it is succeed, then the event is not used furthermore, it can be reused.
 * If the event is not in a queue, it seems to processed in the moment. It is able to expect that the event object is
 * {@link #consumed()} in the next time. Therefore this routines wait a maximal timeout till the event is consumed. 
 * Because of the convention, that a processing of an event should need only a less amount of time, the waiting time 
 * should less than the timeout. If it isn't so and the timeout is reached, the only one assertion is that the software 
 * may have an error.
 * <br><br>
 * The difference between {@link #forceRelinquish()} and {@link #reserveRecall(int)} is: {@link #forceRelinquish()} frees
 * the event completely either to reuse the event object for any other or to remove the event. Whereby {@link #reserveRecall(int)}
 * reserves the event for usage. The last one can be used in an interaction between callback:
 * <pre>
 *         ....for one functionality........
 *   src
 *    |                                       dst
 *    |----------ev.sendEvent(cmd)------------>|
 *    |                                        + use the ev
 *    |                                        + ev.consumedRetain();
 *    |<----------callback---------------------|
 *    |                                        + ev.reserve()
 *    |                                        |----ev.sendEvent(cmd)-----+
 *    +ev.forceReserve()                       |  (reused in destination) |
 *    |                                        |               ?<---------+
 *    |----------ev.sendEvent(cmd)------------>|  
 *    |                                        + use the ev
 *    |                                        |
 * </pre>
 * Normally the Event object is used inside the destination to stimulate a state machine. The callback is only
 * an information back to the source. It is not expected that the source reacts. But in a special situation
 * the source can force the reservation of the event. That is proper if an abort event should sent for example.
 * The usage of the event inside the destination is disabled in that time.
 *  
 * <br><br>
 * @author Hartmut Schorrig
 *
 */






/*
 * 
 * If the event is not able to reserve or relinquish, it is a problem
 * of the software. The contract to process events is: The processing time should be short, in the processing time
 * there should not be wait for any other things.
 * If the event object is not found in its queue, it is processed yet. Because the processing of one event should need 
 * 
 * 
 * 
 * 
 * If the call is repeated with the same event, the effect is that the event is further ahead in the queue. Then it may be
 * processed promptly. But it can also be suitably to send another message with the same event instance, or dequeue
 * and use another Event object or any other operation for messaging the destination. 
 * <br><br>
 * <b>Callback and sendEvent</b>:<br><br>
 * A special case is a callback without expecting an answer (callback to inform the source) with reusing the event on
 * destination side, but the source will be send the event for example to abort the action concurrently.
 * 
 * 
 * 
 * <br><br>
 * There is a way to remove the event from a queue: One can call {@link #forceReserve(int)} if the event should be used
 * for the same destination or {@link #forceRelinquish()} if the event should be used for another destination.
 * Both routines checks whether the event is in a queue and removes it from them. In this case the event is not in process,
 * it is the same as the event was never sent. Therefore it can be reused. 
 * 
 *  
 *  
 *  
 *  
 *  
 *  
 *  
 *  
 *  <br><br><br>
 * <b>Allocated and remove by garbage collector</b>: An instance of this class can be allocated whenever a event should be send. On constructor the destination and 
 * an optional destination thread have to be defined, it is not able to change. It means that event is only for the
 * determined destination. The event is created and referred only locally in the stack. With executing of {@link #sendEvent(int)}
 * the event is either stored in the event queue of the destination thread, it is not referenced in the creating context. 
 * Or the event is referenced in the execution context (stack local) of the {@link EventConsumer#processEvent(Event)}
 * routine in the destination. 
 * <br><br>
 * If the event is not necessary furthermore, the {@link #consumedRetention()} -routine should be called. This routine removes
 * all references inside the event. Because the event is not referenced furthermore anywhere, in Java it is removed 
 * from memory by the garbage collector.
 * <br><br>
 * <b>Allocated and removed in C/C++r</b>: For C/C++-usage without Garbage Collector the event will be remove 
 * if the routine {@link #consumedRetention()} is called. This routine checks whether the event is allocated dynamically
 * and calls {@link org.vishia.bridgeC.MemC#free(Object)} to remove the instance from memory. The application program
 * should only call the {@link #consumedRetention()} after usage. The memory management is done therewith already.
 * For software written in Java and translated to C/C++ no extra effort is necessary. 
 * <br><br>
 * <b>Allocated and used more as one time inside the destination</b>: It is possible to use the event for the same destination after it is processed.
 * Don't call {@link #consumedRetention()}, instead set the data of the event for the new usage and call {@link #sendEvent(int)} once more.
 * The the event instance is re-used. This is a possibility to continue processing the functionality especially in a state machine.
 * It saves effort to free the event and allocate a new one. 
 * <br><br>
 * 
 * 
 * 
 * 
 */
public class Event
{
  
  /**Version, history and license
   * <ul>
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. The event has more elements for forward and backward now.
   * <li>2012-07-28 renamed src, now {@link #refData}. It is not the source (creator) of the event
   *   but a value reference which may be used especially in the callback ({@link #callback}).
   *   Because it is private and the getter method {@link #getRefData()} is duplicated, the
   *   old routine {@link #getSrc()} is deprecated, it is downward compatible still. 
   * <li>2012-03-10 Hartmut new: {@link #owner}, {@link #forceRelease()}. 
   *   It is a problem if a request may be crased in a remote device, but the event is reserved 
   *   for answer in the proxy. It should be freed. Events may be re-used. 
   * <li>2012-01-22 Hartmut chg: {@link #use(long, int, Object, EventConsumer)} needs the dst as parameter.
   * <li>2012-01-05 Hartmut improved: {@link #callbackThread}, {@link #commisionId} instead order, more {@link #data2} 
   * <li>2011-12-27 Hartmut created, concept of event queue, callback need for remote copy and delete of files
   *   (in another thread too). A adequate universal class in java.lang etc wasn't found.
   * </ul>
   * <br><br>
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
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final int version = 20120819;
  
  /**It is an inner value for the cmd variable to designate the event as not reserved. */
  public static final int cmdFree = 0;
  
  /**It is an inner value for the cmd variable to designate the event as reserved. */
  public static final int cmdReserved = -1;
  
  /**The current owner of the event. It is that instance, which has gotten the event instance
   * by any method invocation as parameter,
   * and stores this event till it is need for answer.
   * TODO not used yet. Maybe a List<EventOwner>
   */
  private EventOwner owner;
  
  
  /**An event can have a counterpart for return information. The event and its counterpart may refer one together.
   */
  private Event callbackEvent;
  
  /**The queue for events of the {@link EventThread} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst.{@link EventConsumer#processEvent(Event)} should be called immediately. */
  private EventThread evDstThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  private EventConsumer evDst;
  
  /**The queue for events of the {@link EventThread} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst.{@link EventConsumer#processEvent(Event)} should be called immediately. */
  //private EventThread callbackThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  ///*package private*/ EventConsumer callback;
  
  /**State of the event: 
   * <ul>
   * <li>0 or '.': unused.
   * <li>a: requested or allocated. The {@link EventConsumer} and the {@link EventThread} is set, but the event 
   *   is not in send to the consumer. It is not in a queue and not in process.
   * <li>q: queued in dstThread
   * <li>e: executing
   * <li>B: queued for callback
   * <li>b: callback invoked
   * 
   * </ul> 
   */
  public char stateOfEvent;
  
  /**'s' if the event is static. Space or 0 if allocated. */
  private boolean isStatic;
  
  boolean bAwaitReserve;
  
  /**Any number to identify. It is dst-specific. */
  private AtomicInteger cmd = new AtomicInteger();
  
  //protected int answer;
  
  /**The commission number for the request, which may be answered by this event. */
  protected long orderId;
  
  /**Timestamp of the request. It is atomic because the timestamp may be an identification
   * that the event instance is in use (for new-obviating usage like C-programming). */
  private AtomicLong dateCreation = new AtomicLong();
  
  /**Any value of this event. Mostly it is a return value. */
  public int data1, data2;
  
  /**Any value reference especially to return any information in the {@link #callback()}. */ 
  public Object oData;
  
  /**A referenced instance for the event. This reference is private because it should be set
   * calling {@link #Event(Object, EventConsumer)} or {@link #use(long, int, Object, EventConsumer)}
   * only. The calling environment determines the type. 
   * see {@link #getRefData()}. */
  protected Object refData;
  
  
  
  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventThread)}
   * before first usage. Use {@link #consumed()} to release the usage. 
   * 
   */
  public Event(){
    dateCreation.set(0);
    isStatic = true;
  }
  
  /**Creates an event as dynamic object for usage. Use {@link #consumed()} after the event is used and it is not referenced
   * anymore. 
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public Event(Object refData, EventConsumer consumer, EventThread thread){
    this.dateCreation.set(System.currentTimeMillis());
    this.refData = refData; this.evDst = consumer; this.evDstThread = thread;
    this.callbackEvent = null;
    isStatic = false;
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #consumed()} after the event is used and it is not referenced
   * anymore. 
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   * @param callback Another event to interplay with the source of this event.
   */
  public Event(Object refData, EventConsumer consumer, EventThread thread, Event callback){
    this.dateCreation.set(System.currentTimeMillis());
    this.refData = refData; this.evDst = consumer; this.evDstThread = thread;
    this.callbackEvent = callback;
    callback.callbackEvent = this;
    isStatic = false;
  }
  
  
  public int cmd(){ return cmd.get(); }
  
  
  public Event callbackEvent(){ return callbackEvent; }
  
  
  public EventConsumer evDst() { return evDst; }
  
  
  /**Returns the time stamp of creation or occupying the event.
   * @return null if the event is not occupied, a free static object.
   */
  public Date dateCreation(){ long date = dateCreation.get(); return date == 0 ? null : new Date(date); }
  
  
  public boolean hasCallback(){ return callbackEvent !=null; }
  

  
  /**Returns the stored src argument of construction.
   * Especially the creator of this Event instance knows the instance type
   * of src because one have written in it. The creator can re-cast to that instance type 
   * and read out any information. This approach is proper because the event was created in the past
   * with a designated data constellation. That constellation is stored with the src-reference
   * in the event till the callback execution. There isn't any other mechanism necessary to store
   * that data constellation.
   * <br><br>
   * A receiver of the event may deal with the src if the event id describes a defined type
   * and a 'instanceof' call is done.
   * 
   * @return the source instance which was stored either calling the ctor {@link #Event(Object, EventConsumer)}
   *   or calling re-usage of an event instance: {@link #use(long, int, Object, EventConsumer)}.  
   */
  public Object getRefData(){ return refData; }
  
  /**@deprecated use {@link #getRefData()}
   * @return
   */
  public Object getSrc(){ return refData; }
  
  
  
  /**Check whether this event is free and occupies it. An event instance can be re-used. If the {@link #dateCreation()}
   * is set, the event is occupied. In a target communication with embedded devices often the communication
   * resources are limited. It means that only one order can be requested at one time, and the execution
   * should be awaited. The usage of an re-used event for such orders can be help to organize the
   * requests step by step. If the answer-event instance is in use, a request is pending.
   *  
   * @return true if the event instance is able to use.
   */
  public boolean occupy(Object refData, EventConsumer dst, EventThread thread){ 
    if(dateCreation.compareAndSet(0, System.currentTimeMillis())){
      this.refData = refData;
      this.evDst = dst;
      this.evDstThread = thread;
      
      this.stateOfEvent = 'a';
      return true;
    }
    else return false;
  }
  
  
  
  /**Returns true if the event is occupied. Events may be re-used. That may be necessary if a non-dynamic
   * memory organization may be need, for example in C-like programming. It is possible anyway if actions
   * are done only one after another. In that kind the event instanc is created one time,
   * and re-used whenever it is needed. A re-using should wait for answer and set {@link #consumedRetention()}.
   * If the answer hangs, {@link #forceRelease()} may be called.
   * 
   * @return false if it is ready to re-use.
   */
  public boolean isOccupied(){ return dateCreation.get() !=0; }
  
  
  
  /**Releases the event instance. It is the opposite to the {@link #occupy(Object, EventConsumer, EventThread)} method.
   * The {@link #dateCreation} is set to 0 especially to designate the free-state of the Event instance.
   * All other data are reseted, so no unused references are hold.  
   */
  public void consumed(){
    consumedRetain();
    this.stateOfEvent= '.';
    this.refData = null;
    this.evDst = null;
    this.evDstThread = null;
    this.callbackEvent = null;
    oData = null;
    owner = null;
    dateCreation.set(0);
    if(!isStatic) { 
      MemC.free(this);
    }
  }

  
  
  /**Forces the release of the event instance to re-usage the event. 
   * If the event is in use, the owner will be notified
   * calling {@link EventOwner#remove(Event)} that the event should be released.
   * @return false only if the event is not released from the owner. 
   * Then the action should be repeated.
   */
  public boolean forceRelinquish() {
    boolean removed = false;
    if(evDstThread !=null){
      removed = evDstThread.removeFromQueue(this);
    }
    if(!removed){
      
    }
    boolean bOk = true;
    if(owner !=null){ bOk = owner.remove(this); }
    if(bOk){
      consumedRetain();
    }
    return bOk;
  }
  
  
  

  
  /**Reserves the event to prepare data and to send it.
   * @return true if it is reserved. False if the event is in use at this time.
   */
  public boolean reserve(){
    boolean bOk = this.cmd.compareAndSet(0, cmdReserved);
    return bOk;
  }
  
  
  /**Reserves the event to prepare data and to send it or waits till it is able to reserve.
   * This method may block if the event is in use, either in a queue or in processing, and the destination process hangs.
   * Therefore a maximum of waiting time is given. If the method returns false the the reservation is failed.
   * Then the application may repeat this method for example after query a operator or do any other proper operation.
   */
  public boolean reserve(int timeout){
    boolean bOk = reserve();
    if(!bOk){
      synchronized(this){
        bAwaitReserve = true;
        try{ wait(timeout); } catch(InterruptedException exc){ }
        bAwaitReserve = false;
      }
      bOk = reserve();
    }
    return bOk;
  }
  
  
  
  /**Try to reserve the event for usage, recall it if it is in stored in an event queue.
   * <ul>
   * <li>If the event is free, it is reserved, the method returns true. 
   * <li>If it is not free, but stored in any queue, it will be removed from the queue,
   *   then reserved for this new usage. The method returns true.
   * <li>If it is used and not found in any queue, then it is processed in this moment.
   *   Then this method returns false. The method doesn't wait. See {@link #reserveRecall(int)}.   
   * </ul>
   * @return true if the event is reserved.
   */
  public boolean reserveRecall(){
    boolean bOk = reserve();
    if(!bOk){
      if(evDstThread !=null){
        bOk = evDstThread.removeFromQueue(this);
        if(bOk){
          //it was in the queue, it means it is not in process.
          //therefore set it as consumed.
          consumedRetain();
          bOk = reserve();
        }
      }
    }
    return bOk;
  }
  
  
  /**Try to reserve the event for usage, recall it if it is in stored in an event queue, wait till it is available.
   * This method may block if the event is yet processing. The method doesn't block forever because the destination process hangs.
   * Therefore a maximum of waiting time is given. If the method returns false the the reservation is failed.
   * Then the application may repeat this method for example after query a human operator or after done any other proper operation.
   * <ul>
   * <li>If the event is free, it is reserved, the method returns immediately with true. 
   * <li>If it is not free, but stored in any queue, it will be removed from the queue,
   *   then reserved for this new usage. The method returns imediately with true.
   * <li>If it is used and not found in any queue, then it is processed in this moment.
   *   This method waits the given timeout till the event is free. If it will be free in the timeout period,
   *   the method reserves it and returns true. 
   * <li>If the timeout is expired, the method returns false. That may be an unexpected situation, because the 
   *   processing of an event should be a short non-blocking algorithm. It may be a hint to an software error. 
   * </ul>
   * See {@link #reserveRecall(int)}.   
   * @return true if the event is reserved.
   */
  public boolean reserveRecall(int timeout){
    boolean bOk = reserveRecall();
    if(!bOk){
      synchronized(this){
        bAwaitReserve = true;
        try{ wait(timeout); } catch(InterruptedException exc){ }
        bAwaitReserve = false;
      }
      bOk = reserve();
    }
    return bOk;
  }
  
  
  
  
  
  
  /**Forces the reservation of the event object for usage.
   * <ul>
   * <li>If the event is free, it is reserved. 
   * <li>If it is not free, but in any queue, it will be removed from the queue,
   *   then set as consumed, and reserved then for this new usage.
   * <li>If the event is in process yet, the method waits till it is consumed. It should be take only at least some milliseconds
   *   because that is the contract to use events objects.
   * If the event 
   */
  public boolean recall(){
    boolean bOk = reserve();
    if(!bOk){
      if(evDstThread !=null){
        bOk = evDstThread.removeFromQueue(this);
        if(bOk){
          //it was in the queue, it means it is not in process.
          //therefore set it as consumed.
          consumedRetain();
          bOk = reserve();
        }
      }
    }
    return bOk;
  }
  
  
  
  
  
  
  
  

  
  
  ////
  
  /**Sends this event to its destination instance.
   * Either the element {@link #evDstThread} is not null, then the event is put in the queue
   * and the event thread is notified.
   * Or the dstQueue is null, then a {@link #evDst}.{@link EventConsumer#processEvent(Event this)}.
   * 
   */
  public boolean sendEvent(int cmd){
    int cmd1 = this.cmd.get();
    boolean bOk = (cmd1 == cmdFree || cmd1 == cmdReserved);
    if(bOk) {
      bOk = this.cmd.compareAndSet(cmd1, cmd);
      if(bOk){
        if(evDst == null) throw new IllegalArgumentException("event should have a destination");
        if(evDstThread !=null){
          evDstThread.storeEvent(this);
        } else {
          evDst.processEvent(this);
        }
      }
    }
    return bOk;
  }
  

  /**Mark the event object as consumed, but not relinquished. The event keeps its destination, but frees the cmd and data.  
   */
  public void consumedRetain(){
    this.stateOfEvent= 'a';
    this.cmd.set(0);
    this.orderId = 0;
    data1 = data2 = 0;
    if(bAwaitReserve){
      synchronized(this){ notify(); }
    }
  }


  
  
  
}
