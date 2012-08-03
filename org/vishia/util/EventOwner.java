package org.vishia.util;

/**Access to the current owner of a event. It is referred inside any event.
 * The owner is that class, which uses the event for callback.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface EventOwner
{
  /**Version, history and license
   * <ul>
   * <li>2012-03-10 Hartmut created: 
   *   It is a problem if a request may be crased in a remote device, but the event is reserved 
   *   for answer in the proxy. It should be freed. Events may be re-used. 
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
  public static final int version = 20120311;
  
  /**Remove the event from its ownership. The event is not used anymore for any callback from this owner
   * then.
   * @param ev
   * @return true if it is released. It should return true.
   */
  boolean remove(Event ev);
}
