package org.vishia.util;

import org.vishia.event.EventThread;

/**Deprecated older class instead TimeOrderMng
 * @author Hartmut Schorrig
 * @deprecated use {@link EventThread}
 */
@Deprecated public class OrderListExecuter extends EventThread
{

  public OrderListExecuter(String name)
  {
    super(name);
  }
  
}
