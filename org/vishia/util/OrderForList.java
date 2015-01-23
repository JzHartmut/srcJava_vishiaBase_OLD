package org.vishia.util;

import org.vishia.event.EventTimeOrder;

/**Deprecated older class instead TimeOrder
 * @author Hartmut Schorrig
 * @deprecated use {@link EventTimeOrder}
 */
@Deprecated public abstract class OrderForList extends EventTimeOrder
{

  @Deprecated public OrderForList(String name)
  {
    super(name);
  }
  
  
  /**Handle any request in the graphic thread before the system's dispatching routine starts.
   * @param onlyWakeup unused, deprecated
   */
  @Deprecated public abstract void doBeforeDispatch(boolean onlyWakeup);

  
  @Override public final void executeOrder(){ doBeforeDispatch(false); } 

  
}
