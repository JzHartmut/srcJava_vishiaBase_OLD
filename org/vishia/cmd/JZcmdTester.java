package org.vishia.cmd;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.vishia.util.StringFunctions;

public class JZcmdTester
{


  /**Returns an information about the given obj:
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

  
  
  public void mapToFile(Map<String, Object> map, String sFile) throws IOException{
    Writer wr = new FileWriter(sFile); 
      mapToFile(map, wr, 0);
    wr.close();
  }
  
  
  public void mapToFile(Map<String, Object> map, Appendable wr, int indent) throws IOException{
    for(Map.Entry<String, Object> entry: map.entrySet()){
      String key = entry.getKey();
      Object value = entry.getValue();
      wr.append(StringFunctions.indent2(indent)).append(key).append("; ").append(value.toString()).append("\n");  
      if(value instanceof Map<?, ?>){
        @SuppressWarnings("unchecked")
        Map<String, Object> submap = (Map<String, Object>) value;  
        mapToFile(submap, wr, indent+1);
      }
    }
  }
  
  
  
  
  /**This method is only intend to set a breakpoint into it.
   * @return
   */
  public int stop(){
    return 0;
  }
  

}
