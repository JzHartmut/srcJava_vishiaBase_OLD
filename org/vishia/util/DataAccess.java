package org.vishia.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**This class contains methods to access data, usual with reflection mechanism.
 * @author Hartmut Schorrig
 *
 */
public class DataAccess {
  /**Version, history and license.
   * <ul>
   * <li>2012-11-24 new: {@link DatapathElement} for describing more complex path for access.
   * <li>2012-11-18 new: {@link #setBit(int, int, boolean)} as little universal routine.
   * <li>2012-11-16 new: {@link #getInt(Object)}, {@link #getFloat(Object)} from {@link ObjectValue}, last one is deprecated now.
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
   * <li>The namedDataPool provides additional data, which may be addressed by the first part of path if it starts
   *   with a "$" (a variable). Then the data from this pool are used instead dataPool. 
   *   Elsewhere the first part of the path should be able to found in the start instance of dataPool.
   *  <li>If any data object found inside the path is instanceof Map and the key is a String, then the sub object
   *    is gotten from the map with the next part of the path used as the key.
   *  <li>If any data object is instanceof {@link TreeNodeBase} and the field identifier is not found in this instance,
   *    a child node with the given name is searched. The TreeNodeBase is the super class of {@link org.vishia.xmlSimple.XmlNodeSimple}
   *    which is used to present a ZBNF parse result. Therewith the {@link org.vishia.zbnf.ZbnfParser#getResultTree()}
   *    can be used as data input. The tag names of that result tree follow the semantic in the string given Syntax script.
   *  <li>Nevertheless any Java data objects can be used as dataPool. It is independent of that special possibilities.   
   *  <li>methods with constant string or numeric parameters are admissible as part of the path (TODO)  
   * </ul>
   * @param path The path, elements in any list element.
   * @param dataPool the object where the path starts from.
   * @param namedDataPool variables valid for the current block
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param bContainer If the element is a container, returns it. Elsewhere build a List
   *    to return a container for iteration. A container is any object implementing java.util.Map or java.util.Iterable
   * @return Any data object addressed by the path.
   * @throws IllegalArgumentException
   */
  public static Object getData(
      List<DatapathElement> path
      , Object dataPool
      , Map<String, Object> namedDataPool
      //, boolean noException 
      , boolean accessPrivate,  boolean bContainer)
  throws NoSuchFieldException
  {
    Object data1 = dataPool;
    Iterator<DatapathElement> iter = path.iterator();
    DatapathElement element = iter.next();
    if(element.ident.equals("$input"))
      element.ident +="";  //dummy
    if(element.ident.startsWith("$")){
      if(namedDataPool ==null){
        throw new NoSuchFieldException("$?missing-datapool?");
      }
      data1 = namedDataPool.get(element.ident.substring(1));
      if(data1 ==null){
        throw new NoSuchFieldException(element.ident);
      } else {
        element = iter.hasNext() ? iter.next() : null;
      } 
    }
      Class<?> clazz1;
      while(element !=null && data1 !=null){
        if(element.ident.equals("state1"))
          element.ident +="";  //dummy
        if(data1 instanceof Map<?,?>){  //search data with the String key in a map:
          Map<String,?> dataMap = (Map)data1;
          data1 = dataMap.get(element.ident);
          element = iter.hasNext() ? iter.next() : null;
        }
        else {
          try{ 
            clazz1 = data1.getClass();
            if(element.fn){
              Method method = clazz1.getDeclaredMethod(element.ident);
              data1 = method.invoke(data1);
            } else {
              Field field = clazz1.getDeclaredField(element.ident);
              field.setAccessible(accessPrivate);
              try{ data1 = field.get(data1);
              
              } catch(IllegalAccessException exc){
                //try special types:
                throw new NoSuchFieldException(element.ident + " in " + path); 
              }
            }
          } catch(NoSuchFieldException exc){
            //TODO method
            if(data1 instanceof TreeNodeBase<?,?,?>){
              TreeNodeBase<?,?,?> treeNode = (TreeNodeBase<?,?,?>)data1;
              if(bContainer){
                data1 = treeNode.listChildren(element.ident + " in " + path);
              }
              if(!bContainer || data1 == null){
                data1 = treeNode.getChild(element.ident);
              }
              //NOTE: data1 may be null. But it accepted as correct.
              element = iter.hasNext() ? iter.next() : null;
              
            } else {
              throw new NoSuchFieldException(element.ident); 
            }
          } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          element = iter.hasNext() ? iter.next() : null;
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
  
  
  /**Gets the int value from any Object. If the Object can represent a int val, convert and returns it.
   * Elsewhere it returns 0. TODO return int from a String (CharSequence) with conversion.
   * @param val The Object. An int value is returned from all numerical wrappers: Byte, ... Float, Double.
   * @return the value.
   */
  public static int getInt(Object val){
    if(val instanceof Byte){ return ((Byte)val).byteValue(); }
    else if(val instanceof Short){ return ((Short)val).shortValue(); }
    else if(val instanceof Integer){ return ((Integer)val).intValue(); }
    else if(val instanceof Long){ return (int)((Long)val).longValue(); }
    else if(val instanceof Float){ return (int)((Float)val).floatValue(); }
    else if(val instanceof Double){ return (int)((Double)val).doubleValue(); }
    else return 0;
  }
  
  
  /**Gets the float value from any Object. If the Object can represent a float val, convert and returns it.
   * Elsewhere it returns 0. TODO return int from a String (CharSequence) with conversion.
   * @param val The Object. An float value is returned from all numerical wrappers: Byte, ... Float, Double.
   * @return the value.
   */
  public static float getFloat(Object val){
    if(val instanceof Byte){ return ((Byte)val).byteValue(); }
    else if(val instanceof Short){ return ((Short)val).shortValue(); }
    else if(val instanceof Integer){ return ((Integer)val).intValue(); }
    else if(val instanceof Long){ return ((Long)val).longValue(); }
    else if(val instanceof Float){ return ((Float)val).floatValue(); }
    else if(val instanceof Double){ return (float)((Double)val).doubleValue(); }
    else return 0;
  }


  /**Sets a bit in a int word
   * @param value The actual value of the word
   * @param mask Designation of bits to change. Usual only one bit. Tip: Use symbolic names.
   * @param set true: set this bits to 1, false: reset the bits to 0.
   * @return The new value of the word. You should invoke: myBitword = setBit(myBitword, ....);
   */
  public static int setBit(int value, int mask, boolean set){
    return set ? value | mask : value & ~mask;
  }
  

  
  
  /**Class holds one element for access to data.
   * Instances of this class can be created using {@link org.vishia.zbnf.ZbnfJavaOutput} to fill from a parse result.
   * Therefore some methods have a special naming which matches to the semantic of the used parser script.
   */
  public static final class DatapathElement
  {
    /**Name of the element or method in any instance.
     * From Zbnf <$?ident>
     */
    public String ident;

    /**True if it is a method.  From Zbnf <$?fn> */
    public boolean fn;
    
    /**List of arguments of a method. If null, the method has not arguments. */
    private List<Object> fnArgs;
    
    /**Set a textual argument of a access method. From Zbnf <""?textArg>. */
    public void set_textArg(String arg){ addToList(arg); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_intArg(long arg){ addToList(arg); }
    
    /**Set a float (double) argument of a access method. From Zbnf <#f?floatArg>. */
    public void set_floatArg(double arg){ addToList(arg); }
    
    /**Adds any argument. This method is called from {@link #set_floatArg(double)} etc. */
    public void addToList(Object arg){
      if(fnArgs == null){
        fnArgs = new LinkedList<Object>();
      }
      fnArgs.add(arg);
    }
    
    /**For debugging.*/
    @Override public String toString(){
      if(!fn){ return ident;}
      else{
        return ident + "(...)";
      }
    }
  }

  
}
