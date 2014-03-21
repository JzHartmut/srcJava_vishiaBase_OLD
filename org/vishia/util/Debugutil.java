package org.vishia.util;

public class Debugutil
{
  /**This method is used either to set a breakpoint in it or to set a breakpoint on its call. It does nothing.
   * @return not used. A return statement is only contained to set the breakpoint. It is not remove by optimizing
   * because the method itself does not know anything about the ignoring of the return value on its call.
   */
  public static int stop(){
    return 0;
  }
}
