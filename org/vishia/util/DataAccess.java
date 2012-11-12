package org.vishia.util;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**This class contains methods to access data, usual with reflection mechanism.
 * @author Hartmut Schorrig
 *
 */
public class DataAccess {
  /**Version and history
   * <ul>
   * <li>2012-11-04 chg: parameter bContainer in getData(...): Sometimes a container is ispected
   *   to iterate though only one element is found, sometimes only a simple element is expected
   *   though a container is addressed maybe with one element. 
   * <li>2012-10-21 created. Some algorithm are copied from {@link org.vishia.textGenerator.TextGenerator} in this class.
   *   That algorithm are able to use independent in some applications.
   * </ul>
   * 
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
   * 
   */
  static final public int version = 20121021;

  /**Reads content from the data.
   * <ul>
   * <li>The namedDataPool provides additional data, which are addressed by the first part of path.
   *    The the data from this pool are used instead dataPool. Elsewhere the first part of the path should be 
   *    able to found in the start instance of dataPool.
   *  <li>If any data object found inside the path is instanceof Map and the key is a String, then the sub object
   *    is gotten from the map with the next part of the path used as the key.
   *  <li>methods with constant string or numeric parameters are admissible as part of the path (TODO)  
   * </ul>
   * @param path The path, elements separated with dot.
   * @param dataPool the object where the path starts from.
   * @param namedDataPool variables valid for the current block
   * @param noException returns an info string starting with "<?" if there is any error.
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   *  @param bContainer If the element is a container, returns it. Elsewhere build a List
   *    to return a container for iteration. A container is any object implementing java.util.Map or java.util.Iterable
   * @return Any data object addressed by the path.
   * @throws IllegalArgumentException
   */
  public static Object getData(
      List<String> path
      , Object dataPool
      , Map<String, Object> namedDataPool
      , boolean noException, boolean accessPrivate,  boolean bContainer)
  throws IllegalArgumentException
  {
    Class<?> clazz1;
    Object data1 = dataPool;
    Iterator<String> iter = path.iterator();
    String sElement = iter.next();
    if(sElement.equals("subState"))
      sElement +="";  //dummy
    data1 = namedDataPool.get(sElement);
    if(data1 !=null){
      sElement = iter.hasNext() ? iter.next() : null;
    } else {
      data1 = dataPool;
    }
    while(sElement !=null && data1 !=null){
      if(sElement.equals("state1"))
        sElement +="";  //dummy
      if(data1 instanceof Map<?,?>){  //search data with the String key in a map:
        Map<String,?> dataMap = (Map)data1;
        data1 = dataMap.get(sElement);
        sElement = iter.hasNext() ? iter.next() : null;
      }
      else {
        try{ 
          clazz1 = data1.getClass();
          Field element = clazz1.getDeclaredField(sElement);
          element.setAccessible(accessPrivate);
          try{ data1 = element.get(data1);
          
          } catch(IllegalAccessException exc){
            //try special types:
            if(noException){
              return "<? path access: " + path.toString() + "?>";
            } else {
              throw new IllegalArgumentException("IllegalAccessException, hint: should be public;" + sElement); 
            }
          }
          sElement = iter.hasNext() ? iter.next() : null;
        } catch(NoSuchFieldException exc){
          //TODO method
          if(data1 instanceof TreeNodeBase<?,?,?>){
            TreeNodeBase<?,?,?> treeNode = (TreeNodeBase<?,?,?>)data1;
            if(bContainer){
              data1 = treeNode.listChildren(sElement);
            }
            if(!bContainer || data1 == null){
              data1 = treeNode.getChild(sElement);
            }
            //NOTE: data1 may be null. But it accepted as correct.
            sElement = iter.hasNext() ? iter.next() : null;
            
          } else {
            if(noException){
              return "<? path fault: " + path.toString() + "?>";
            } else {
              throw new IllegalArgumentException("NoSuchFieldException;" + sElement); 
            }
          }
        }
      }
    }
    if(data1 !=null && bContainer && !((data1 instanceof Iterable<?>)||data1 instanceof Map)){ //should return a container
      List<Object> list1 = new LinkedList<Object>();
      list1.add(data1);
      data1 = list1;
    }
    return data1;
  }
  
  
  
  
  public static String getStringFromObject(Object content){
    String sContent;
    if(content == null){
    sContent = "";
    }
    else if(content instanceof String){ 
      sContent = (String) content; 
    } else if(content instanceof Integer){ 
      int value = ((Integer)content).intValue();
      sContent = "" + value;
    } else {
      sContent = content.toString();
    }
    return sContent;
  }
  

}
