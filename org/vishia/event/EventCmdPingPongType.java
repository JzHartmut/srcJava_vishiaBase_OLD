package org.vishia.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.vishia.fileRemote.FileRemote;
import org.vishia.util.DateOrder;


/**Base class for all events in a event driven software or for communications.
 * Events may contain data. Special data are contained in derived classes of this. The type of the event
 * can be checked using <code>event instanceof DerivedEvent</code>. The id may be only a hint.
 * <br><br>
 * <b>Overview- content</b><br>
 * <a href="#whatis">1. What is a event driven software</a><br>
 * <a href="#usage"> 2. Where is this Event class able to use</a><br>
 * <a href="#lifecycle">3. Life cycle of an event object</a><br>
 * <a href="#conflicts">4. Conflicts in usage of Event objects</a><br>
 * <a href="#debug">5. Debug helpers</a><br>
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
 * can invoke a callback using {@link #callback}.{@link EventConsumer#processEvent(EventCmdPingPongType)}. Note that this callback
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
 *                                             |
 * </pre>
 * or more simple:
 * <pre>
 *   src
 *    |                                       dst:{@link EventConsumer}
 *    |                                        |
 *    |----processEvent(new Event(cmd)-------->|
 *    |                                        + read data from event
 *                                             |
 * </pre>
 * The event object is created on demand with {@link #Event(Object, EventConsumer, EventThread)}
 * and send via {@link #sendEvent(int)} to the destination or it is stored in the queue of the {@link EventThread}
 * and then processed. Alternatively it may created with {@link #Event(Enum)} and applied to any 
 * {@link EventConsumer#processEvent(EventCmdPingPongType)}. <br>
 * While processing, the data are gotten from the event. After them it is no longer necessary. 
 * Because it is not referenced any more, the garbage collector will be removed it from memory.
 * <br><br>
 * Template for usage:
 * <pre>
  Event ev = new MyEvent();
  ev.setSomeData(...)
  ev.sendEvent(MyCmd);
 * </pre>
 * <br><br>
 * It is a convention to <b>process events without blocking</b> (waiting for any resource) <b>in a short time</b>. 
 * Events are often used in a cyclic loop maybe for a state machine operation or inside interrupts. 
 * Therefore the calculation time between dequeue the event and start the {@link EventConsumer#processEvent(EventCmdPingPongType)} 
 * and the end of this routine respectively the time to call 
 * {@link #relinquish()} is short. Short means a few milliseconds or less.
 * 
 * <br><br>
 * <b>Permanent event instances</b>:
 * <br><br>
 * Another approach is to use permanent instances. If a functionality between 2 classes should be used only once in the same time 
 * simultaneously, then the usage of a permanent event object is the same like the allocation of a new instance, relinquish it
 * and allocate a new one after them. If the functionality should be processed more as once in the same time, 
 * a permanent instance can't be used of course. Permanent instances have disadvantages for free usages because a multiple 
 * occupancy should be excluded. Therefore the dynamic data handling in Java with Garbage Collection is an important 
 * feature and advantage of this language and programming system. But the dynamic approach has a disadvantage: The amount of
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
 *    |                                        + ev.relinquish() 
 *    |                                        |
 *    |
 *    |                               dst2
 *    +ev.occupy(dst2)                  |
 *    |                                 |
 *    |------ev.sendEvent(cmd)--------->|
 *    |                                 + read data from event
 *    |                                 + ev.relinquish() 
 *    |                                 |
 *    |
 * </pre>
 * 
 * If an event object should be used permanently, it should be created with {@link #Event()} at startup time
 * and referenced in the source.
 * If the event object is needed, the method {@link #occupy(Object, EventConsumer, EventThread)} has to be called 
 * instead new {@link #Event(Object, EventConsumer, EventThread)}. This method returns false if the event object is in use.
 * The handling on the destination side is the same. After read data from the event it should be described as
 * {@link #relinquish()}. Then the occupying is relinquished. Because the event is referred in the source, 
 * it is not removed by the Garbage Collector in Java. 
 * <br><br> 
 * <br><br>
 * Template for usage:
 * <pre>
  Event ev = new MyEvent(); //in ctor
  //
  ev.occupy(evSrc, evDst, dstThread);
  ev.setSomeData(...)
  ev.sendEvent(MyCmd);
 * </pre>
 * <br><br>
 * <b>Time of relinquish</b>:<br>
 * If an Event is used, it may be used from some other following actions again. A user cannot determine that the event
 * is not need anymore. The Event can be only relinquished
 * <ul>
 * <li>either it is processed from a queue, and the execution sequence returns back to the check point of the queue
 *   to get the next event. Only then the Event is processed overall. Then it can be relinquished.
 * <li>or the event is processed by only one routine immediately called in the {@link #sendEvent(Enum)} routine
 *   because no queuing is done. Then only this routine is called with the event. Therefore the Event is relinquished
 *   automatically in the {@link #sendEvent(Enum)} routine after calling {@link EventConsumer#processEvent(EventCmdPingPongType)}.      
 * </ul> 
 * It is possible that the event is stored in any other queue ('deferred events') inside the 
 * {@link EventConsumer#processEvent(EventCmdPingPongType)}. Then the method {@link #donotRelinquish()} can be called therefore
 * which prevents the relinguish from the event after processing.
 * 
 * <br><br>
 * <b>Exceptions while processing the event</b>:<br>
 * If an exception occurs, the whole process of execution should not be stopped. Especially the {@link #relinquish()}
 * of the event should be done nevertheless. Therefore the exception is caught both in the {@link #sendEvent(Enum)} routine
 * if {@link EventConsumer#processEvent(EventCmdPingPongType)} is called immediately and in the {@link EventThread} loop. The exception
 * is reported to the System.err output stream. An application can use its own try and catch constructs inside the 
 * {@link EventConsumer#processEvent(EventCmdPingPongType)} routine. This caught has only its effect when an error is not caught elsewhere.
 * 
 * <br><br>
 * <b>Opponent event and reusing</b>:<br>
 * An event can be used either in a 'ping-pong-play' with the source and the destination or it can be used as self-created
 * Events for the own state machine to force living of them. Because the event can't be relinquish on demand
 * a second Event Object of the event is needed. Only one additional Event Object is sufficient. 
 * Only that two instances are needed, both can be used permanently. Therefore this Event class can refer an opponent event:
 * <pre>
 *         ....for one functionality........
 *   src
 *    |                                       dst
 *    |ev = new Event(dst)                     |
 *    |----------ev.sendEvent(cmd)------------>|
 *    |                                        + use the ev
 *    |                                        |                  (used in destination)
 *    |                                        |----ev.getOpponent.sendEvent(cmd)-----+
 *    |                                        + ev.relinquish()                      |
 *    |                                        |<-------------------------------------+
 *    |                                        + use the event;
 *    |<--ev.getOpponent.sendEvent(cmd)--------|
 *    |    (used for callback)                 + ev.relinquish()
 *    |--ev.getOpponent.sendEvent(cmd)-------->|
 *    + ev.relinquish()                        + ev.relinquish()
 *    |                                        |
 * </pre>
 * Example to use the opponent:
 * <pre>
 *   boolean trans_StateX(Event evP) {         //a transition method of a state
 *     MyEvent ev;                             //Use casted qualified events.
 *     if(ev instanceof MyEvent && (ev = (MyEvent)evP).getCmd() == cmd) { //condition for state switch)
 *       //do something
 *       ev.getOpponent().sendEvent(cmdNext);  //sends a event to get the state switch process alive
 *     .....  
 * </pre>
 * The event itself, the reference <code>evP</code> or <code>ev</code>, is in use. It can't be used for the next step.
 * But its opponent is free for next use. The <code>instanceof</code> test and cast to <code>MyEvent</code> in this example
 * is used because the cmd arguments should be qualified, see {@link #sendEvent(Enum)} and {@link #getCmd_()}.   
 * The time of relinquish has passed whenever the thread using the event is finished with the 'run to completion'
 * of current the Event Object. The time of sendEvent is in front of them. Therefore the Event Object itself can't be used
 * but its opponent. If a quick game is played, it is possible that the thread which reused the opponent of the received Event
 * acts faster and prior than the thread which has sent the event. It means it may be possible that the opponent of a
 * received Event is not relinquished before that opponent is reused. This is a effect of the very quickly game. 
 * If the thread works in orderly and fair times, that situation does not occur. If threads works only for a short time
 * a thread switch occurs only if the currently thread enters in a wait situation. Forced thread displacement occurs
 * only if a thread works too long. Therefore that kind of usage the Event Object and its opponent can be used.
 * <br><br>
 * If the opponent is not relinquished before it is re-used, the {@link #sendEvent(Enum)} is prevented. If one of the 
 * methods {@link #occupy()} was called before {@link #sendEvent(Enum)}, this situation is evidently because {@link #occupy()}
 * returns false if the Event Object is not relinquished. Last and least the invocation of
 * {@link #occupyRecall(int)} or {@link #occupyRecall(int, Object, EventConsumer, EventThread)} can help to get
 * a well-ordering thread execution. The thread which invokes {@link #occupyRecall(int)} waits a moment (some milliseconds)
 * to let the other thread work.
 * 
 * 
 * <pre>
 *   src
 *    |                                       dst
 *    |ev = new Event(dst)                     |
 *    |----------ev.sendEvent(cmd)------------>|
 *    |                                        + use the ev
 *    |<--ev.getOpponent.sendEvent(cmd)--------|
 *    |    (used for callback)                 ~ (no time for relinquish)
 *    |--ev.getOpponent.sendEvent(cmd)--->
 *    |     this invocation is prevented 
 *    |     because the opponent Eent is still in use  
 * </pre>
 * <br>
 * <pre>
 *   src
 *    |                                       dst
 *    |ev = new Event(dst)                     |
 *    |----------ev.sendEvent(cmd)------------>|
 *    |                                        + use the ev
 *    |<--ev.getOpponent.sendEvent(cmd)--------|
 *    |                                        ~ (no time for relinquish)
 *    + ev.getOppenent.occupy(1000)            |
 *    ~ let the other thread work              + ev.relinquish()
 *    | <------------------------------notify--|
 *    |                                        |
 *    |--ev.getOpponent.sendEvent(cmd)-------->|
 *    |     now it works!
 * </pre>
 *  * 
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
 * {@link #relinquish()} in the next time. Therefore this routines wait a maximal timeout till the event is consumed. 
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
 * <br>
 * <br>
 * <a name="debug" />
 * <b>5. Debug helpers</b>
 * <br>
 * @param <CmdEnum> The type of Cmd for this enum, see {@link #getCmd()}
 * @param <CmdBack> The type of the Cmd of the opponent Event, see {@link #getOpponent()}. 
 *   Use {@link EventCmdPingPongType.NoOpponent} for this generic parameter if the event has not a opponent.
 *   See {@link #Event(EventSource, Object, EventConsumer, EventThread, EventCmdPingPongType)}, the last parameter should be null then. 
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
public class EventCmdPingPongType<CmdEnum extends Enum<CmdEnum>, CmdBack extends Enum<CmdBack>> extends EventCmdType<CmdEnum>
{
  
  /**Version, history and license
   * <ul>
   * <li>2014-01.03 Hartmut created: Separated from older Event class in this package respectively from EventMsg.
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final String version = "2015-01-03";

  
  public enum Consumed{Consumed, RunToCompleted}

  /**Cmd which designates that the event has not an opponent. This type is able to use as second parameter
   * for Event<Cmd, Event.NoOpponent>
   */
  public enum NoOpponent{ }
  

  /**An event can have an opponent or counterpart for return information. The event and its counterpart may refer one together.
   */
  private EventCmdPingPongType<CmdBack, CmdEnum> opponent;
  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventThread)}
   * before first usage. Use {@link #relinquish()} to release the usage. 
   * 
   */
  public EventCmdPingPongType(){
    super(); //EventSource.nullSource);
  }
  

  /**Creates an event as a dynamic object for direct usage without a given {@link EventConsumer}.
   * This event should be used as parameter immediately for an event consuming routine.
   * The event is set as occupied already after creation. 
   * Don't call {@link #occupy(EventSource, boolean)} or {@link #occupy(EventSource, EventConsumer, EventThread, boolean)} should be use
   * Don't call {@link #sendEvent()} because a destination is not given.
   * 
   * @param cmd a given Command. It may be null, it can be overwritten later with {@link #setCmd(Enum)}
   *   or using {@link #sendEvent(Enum)}.
   */
  public EventCmdPingPongType(CmdEnum cmd){
    super(cmd);
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventCmdPingPongType(EventSource source, EventConsumer consumer, EventThread thread){
    super(source, consumer, thread);
    this.opponent = null;
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   * @param callback Another event to interplay with the source of this event.
   */
  public EventCmdPingPongType(EventSource source, EventConsumer consumer, EventThread thread
      , EventCmdPingPongType<CmdBack, CmdEnum> callback){
    super(); //EventSource.nullSource);
    if(source == null || consumer == null){
      this.dateCreation.set(0);
    } else {
      super.source = source;
      DateOrder date = new DateOrder();
      this.dateCreation.set(date.date);
      this.dateOrder = date.order;
    }
    this.evDst = consumer; this.evDstThread = thread;
    this.opponent = callback;
    if(callback !=null) {
      callback.opponent = this;  //Refer this in the callback event. 
    }
  }
  
  
  /**Sets the command into the event. The event should be occupied already.
   * @param cmd any admissible command
   * @return this to concatenate
   */
  public EventCmdPingPongType<CmdEnum, CmdBack> setCmd(CmdEnum cmd){ cmde = cmd; return this; }
  
  public EventCmdPingPongType<CmdBack, CmdEnum> getOpponent(){ return opponent; }
  
  public boolean hasOpponent(){ return opponent !=null; }
  
  @Override public String toString(){ 
    long nDate = dateCreation.get();
    if(nDate == 0) return "Event not occupied";
    Date date = new Date(nDate);
    String sCmd = cmde == null ? "null" : cmde.toString();
    EventSource source1 = this.sourceMsg;
    return "Event cmd=" + sCmd + "; " + (nDate == 0 ? "nonOccupied" : toStringDateFormat.format(date) + "." + dateOrder) + "; src=" + (source1 !=null ? source1.toString() : " noSrc") + "; dst="+ (evDst !=null ? evDst.toString() : " noDst"); 
  }
  
  
}
