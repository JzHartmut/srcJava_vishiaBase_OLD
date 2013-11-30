package org.vishia.cmd;

public class ZGenTester
{


  /**Returns a information about the given obj:
   * It returns the "Type=< clazz.getCanonicalType()>; toString=< toStringValue>;"
   * @param obj Any object
   * @return the information string.
   */
  public CharSequence info(Object obj){
    
    //Build an information string about the object:
    StringBuilder u = new StringBuilder();
    Class<?> clazz = obj.getClass();
    u.append("Type=");
    u.append(clazz.getCanonicalName());
    u.append("; toString=").append(obj.toString());
    u.append("; ");
    return u;

  }

  
  /**Same as {@link #info(Object)} as one line
   * @param start text before "Type=..."
   * @param obj Any object
   * @return the information string.
   */
  public CharSequence infoln(CharSequence start, Object obj){
    //Build an information string about the object:
    StringBuilder u = new StringBuilder();
    u.append(start);
    Class<?> clazz = obj.getClass();
    u.append("Type=");
    u.append(clazz.getCanonicalName());
    u.append("; toString=").append(obj.toString());
    u.append(";\n");
    return u;
    
  }

  
  
  /**This method is only intend to set a breakpoint into it.
   * @return
   */
  public int stop(){
    return 0;
  }
  

}
