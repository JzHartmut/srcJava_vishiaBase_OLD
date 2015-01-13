package org.vishia.util;

import org.vishia.event.TimeOrderMng;

/**Deprecated older class instead TimeOrderMng
 * @author Hartmut Schorrig
 * @deprecated use {@link TimeOrderMng}
 */
@Deprecated public class OrderListExecuter extends TimeOrderMng
{

  public OrderListExecuter(TimeOrderMng.ConnectionExecThread execThread)
  {
    super(execThread);
  }
  
}
