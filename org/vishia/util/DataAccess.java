package org.vishia.util;

import java.lang.reflect.Constructor;
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
   * <li>2012-12-26 Hartmut new: {@link #create(String, Object...)}, the {@link DatapathElement#whatisit} can contain 'n'
   *   to force creation of a new instance.
   * <li>2012-12-23 Hartmut chg, new: {@link #getStringFromObject(Object, String)} now uses a format string.
   * <li>2012-12-22 Hartmut new: {@link DatapathElement#constValue} as general possibility, usual for the first element of a path.
   * <li>2012-12-08 Hartmut new: {@link #getData(String, Object, boolean)} as subroutine in {@link #getData(List, Object, Map, boolean, boolean)}
   *   and able to use to get with non treed path, only direct but with all facilities to get from Map etc..
   * <li>2012-11-24 Hartmut new: {@link DatapathElement} for describing more complex path for access.
   * <li>2012-11-18 Hartmut new: {@link #setBit(int, int, boolean)} as little universal routine.
   * <li>2012-11-16 Hartmut new: {@link #getInt(Object)}, {@link #getFloat(Object)} from {@link ObjectValue}, last one is deprecated now.
   * <li>2012-11-04 Hartmut chg: parameter bContainer in getData(...): Sometimes a container is ispected
   *   to iterate though only one element is found, sometimes only a simple element is expected
   *   though a container is addressed maybe with one element. 
   * <li>2012-10-21 Hartmut created. Some algorithm are copied from {@link org.vishia.textGenerator.TextGenerator} in this class.
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


  static final Class<?> ifcMainCmdLogging_ifc = getClass("org.vishia.mainCmd.MainCmdLogging_ifc");
  
  
  private final static Class<?> getClass(String name){
    try{
      return Class.forName(name);
    } catch(Exception exc){
      return null;
    }
  }
  
  
  
  public static Object create(String classname, Object ... args) 
  throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
    Class<?> clazz = Class.forName(classname);
    Constructor<?> ctor = clazz.getConstructor(ifcMainCmdLogging_ifc);  //args[0].getClass()); //
    Object ret = ctor.newInstance(args);
    return ret;
    
  }
  
  
  
  
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
    //if(element.constValue !=null){
    //  data1 = element.constValue;
    //  element = null;  //no more following
    //}
    //else 
    if(element.ident.startsWith("$position")){
      Assert.stop();
    }
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
      switch(element.whatisit) {
        case 'n': {  //create a new instance, call constructor
          Object[] oArgs;
          if(element.fnArgs !=null){
            oArgs =element.fnArgs.toArray();
          } else {
            oArgs = null;
          }
          try{
            data1 = create(element.ident, oArgs);
          } catch(Exception exc){
            exc.printStackTrace();
              
          }
        } break;
        case 'r': {
          try{ 
            clazz1 = data1.getClass();
              Method method = clazz1.getDeclaredMethod(element.ident);
              data1 = method.invoke(data1);
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
        } break;
        default:
          data1 = getData(element.ident, data1, accessPrivate, bContainer);
      }//switch
      element = iter.hasNext() ? iter.next() : null;
    }
    if(data1 !=null && bContainer && !((data1 instanceof Iterable<?>)||data1 instanceof Map)){ //should return a container
      List<Object> list1 = new LinkedList<Object>();
      list1.add(data1);
      data1 = list1;
    }
    return data1;
  }
  
  
  
  public static Object getData(
      String name
      , Object dataPool
      , boolean accessPrivate
      , boolean bContainer) 
  throws NoSuchFieldException
  {
    Object data1;
    if(dataPool instanceof Map<?, ?>){
      data1 = ((Map<?,?>)dataPool).get(name);
    } 
    else {
      Class<?> clazz = dataPool.getClass();
      try{
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(accessPrivate);
        try{ 
          data1 = field.get(dataPool);
        } catch(IllegalAccessException exc){
          //try special types:
          throw new NoSuchFieldException(name); 
        }
      }catch(NoSuchFieldException exc){
        if(dataPool instanceof TreeNodeBase<?,?,?>){
          TreeNodeBase<?,?,?> treeNode = (TreeNodeBase<?,?,?>)dataPool;
          if(bContainer){ data1 = treeNode.listChildren(name); }
          else { data1 = treeNode.getChild(name); }  //if more as one element with that name, select the first one.
        } else {
          throw new NoSuchFieldException(name + " in " + clazz.getName()); 
        }
        
      }
    }
    return data1;  //maybe null
  }
  
  
  
  /**Returns a string representation of the object.
   * <ul>
   * <li>content == null returns an empty string.
   * <li>content is a numerical type, returns a formatted string from it.
   * <li>else return content.toString().
   * </ul>
   * @param content any object
   * @param format may be null, if not null it is used with {@link java.lang.String#format(String, Object...)}.
   * @return A string which represents content.
   */
  public static String getStringFromObject(Object content, String format){
    String sContent;
    if(content == null){
      sContent = "";
    }
    else if(content instanceof String){ 
      sContent = (String) content; 
    } else if(content instanceof Integer){ 
      if(format !=null){
        try{ sContent = String.format(format, content); 
        } catch(Exception exc){ sContent = "<??format:"+ format + " exception:" + exc.getMessage() + "??>"; }
      } else {
        int value = ((Integer)content).intValue();
        sContent = Integer.toString(value);
      }
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
  public static class DatapathElement
  {
    /**Name of the element or method in any instance.
     * From Zbnf <$?ident>
     */
    public String ident;
    
    /**Maybe a constant value, also a String. */
    //public Object constValue;

    /**Kind of element
     * <ul>
     * <li>'f': a field with ident as name.
     * <li>'n': new ident, creation of instance maybe with or without arguments in {@link #fnArgs}
     * <li>'s'; call of a static routine maybe with or without arguments in {@link #fnArgs}
     * <li>'r': subroutine maybe with or without arguments in {@link #fnArgs}.
     * </ul>
     */
    public char whatisit;
    
    /**List of arguments of a method. If null, the method has not arguments. */
    private List<Object> fnArgs;
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    //public void set_intValue(long val){ constValue = new Long(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    //public void set_floatValue(double val){ constValue = new Double(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    //public void set_textValue(String val){ constValue = val; }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    //public void set_charValue(String val){ constValue = new Character(val.charAt(0)); }
    
    /**Set a textual argument of a access method. From Zbnf <""?textArg>. */
    //public void set_textArg(String arg){ addToList(arg); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    //public void set_intArg(long arg){ addToList(arg); }
    
    /**Set a float (double) argument of a access method. From Zbnf <#f?floatArg>. */
    //public void set_floatArg(double arg){ addToList(arg); }
    
    /**Adds any argument. This method is called from {@link #set_floatArg(double)} etc. */
    public void addActualArgument(Object arg){
      if(fnArgs == null){
        fnArgs = new LinkedList<Object>();
      }
      fnArgs.add(arg);
    }
    
    /**For debugging.*/
    @Override public String toString(){
      if(whatisit !='r'){ return ident;}
      else{
        return ident + "(...)";
      }
    }
  }

  
}
