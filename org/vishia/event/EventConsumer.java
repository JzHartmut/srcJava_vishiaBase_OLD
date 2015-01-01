package org.vishia.event;

/**This interface describe the consumer interface for events.
 * The class which implements this interface is able to get events for example from a common queue
 * and executes the {@link #processEvent(Event)} method with the event.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface EventConsumer
{
  /**Version, history and license
   * <ul>
   * <li>2013-05-11 Hartmut chg: It is an interface up to now. The idea to store a name for debugging on 
   *   anonymous overridden instances is able to implement with an <code>toString(){ return "name"}</code> alternatively.
   *   The method doProcessEvent is renamed to {@link #processEvent(Event)}. 
   *   The advantage of interface: multi inheritance. 
   *   It is used as interface for {@link org.vishia.stateMachine.StateCompositeBase}.
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
   * <li>2012-08-30 Hartmut new: This is an abstract class now instead of an interface. 
   *   The application should build an anonymous inner class with it. Because better debugging suppert 
   *   the class has a name which is present to the toString()-method of an event.
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
  public static final int version = 20130511;

  //public final String name;

  //public EventConsumer(String name){ this.name = name; }
  
  /**This routine should be overwritten to processes an event. 
   * @param ev The event. It contains some data. The type of the event is not specified here. Any events
   *   can be processed.
   * @return true if this method can process this type of event. False if the event doesn't match.
   *   It is possible to build a chain of responsibility. It is possible too to process a event from 
   *   more as one instance. 
   */
  int processEvent(Event<?,?> ev); //{ return false; }
  
  /**Bit in return value of a Statemachine's {@link #check(Event)} or entry method for designation, 
   * that the given Event object was used to switch.
   */
  public final static int mEventConsumed =0x1;
  
  //@Override public String toString(){ return name; }
}
