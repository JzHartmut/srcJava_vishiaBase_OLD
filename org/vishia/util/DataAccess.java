package org.vishia.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
   * <li>2012-03-10 Hartmut new: Now supports access to elements of the super class (TODO: outer classes).
   * <li>2012-01-13 Hartmut chg: {@link #getData(List, Object, Map, boolean, boolean)} can be invoked with null for dataPool
   *   to invoke new or static methods.
   * <li>2013-01-12 Hartmut new: {@link #checkAndConvertArgTypes(List, Class[])} improved, 
   *   new {@link #invokeStaticMethod(DatapathElement, Object, boolean, boolean)}
   * <li>2013-01-05 Hartmut new: reads $$ENV_VAR.
   * <li>2013-01-02 Hartmut new: Supports access to methods whith parameter with automatic cast from CharSequence to String and to File.
   *   Uses the {@link DatapathElement#fnArgs} and {@link #getData(List, Object, Map, boolean, boolean)}.
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
   * <li>2012-10-21 Hartmut created. Some algorithm are copied from {@link org.vishia.zTextGen.TextGenerator} in this class.
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
  static final public int version = 20130310;


  static final Class<?> ifcMainCmdLogging_ifc = getClass("org.vishia.mainCmd.MainCmdLogging_ifc");
  
  
  private final static Class<?> getClass(String name){
    try{
      return Class.forName(name);
    } catch(Exception exc){
      return null;
    }
  }
  
  
  
  /**Creates an object with the given class name und the given constructor arguments.
   * @param classname className The fully qualified name of the desired class. See {@link java.lang.Class#forName(String)}.
   * @param args empty, null or some arguments. 
   * @return
   * @throws ClassNotFoundException
   * @throws NoSuchMethodException
   * @throws SecurityException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   */
  public static Object create(String classname, Object ... args) 
  throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
    Class<?> clazz = Class.forName(classname);
    Constructor<?> ctor = clazz.getConstructor(ifcMainCmdLogging_ifc);  //args[0].getClass()); //
    Object ret = ctor.newInstance(args);
    return ret;
    
  }
  
  
  
  
  /**Accesses data. 
   * The argument path contains elements, which describes the access path.
   * <ul>
   * <li>The datapath can start with an element designated with {@link DatapathElement#whatisit} == 'e'. 
   *   Then the result of the access is the String representation of the environment variable of the operation system
   *   or null if that environment variable is not found. Only this element of datapath is used, it should be the only one usual. 
   * <li>The datapath can start with an optional ,,startVariable,, as {@link DatapathElement#whatisit} == 'v'. 
   *   Then the param namedDataPool should provide additional data references, which are addressed by the  {@link DatapathElement#ident}
   *   of the first element.
   * <li>If the datapath does not start with a ,,startVariable,, or it is not an environment variable access, the access starts 
   *   on ghe given datapool. 
   * </ul>
   * The elements of datapath describe the access to data. Any element before supplies a reference for the path 
   * of the next element.
   * <br><br>
   * <b>Access with an element with {@link DatapathElement#whatisit} == 'f'</b>:
   * The  {@link DatapathElement#ident} may determine a field of the current data reference or it may be a key for a indexed container.
   * The {@link #getData(String, Object, boolean, boolean)} is invoked, see there for further explanation. 
   * <br><br>
   * <b>Creating an instance or invocation of methods</b>:
   * <li>An element with {@link DatapathElement#whatisit} == 'n' is the creation of instance maybe with or without arguments 
   *   in {@link DatapathElement#fnArgs}
   * <li>An element with {@link DatapathElement#whatisit} == 's' is a call of a static routine maybe with or without arguments 
   *   in {@link DatapathElement#fnArgs}
   * <li>An element with {@link DatapathElement#whatisit} == 'r' is a method invocation maybe with or without arguments.
   *   in {@link DatapathElement#fnArgs}
   * </ul>
   * <b>Assignment of arguments of methods or constructor</b>:<br>
   * If the method or class is found per name, all methods with this name respectively all constructors are tested 
   * whether they match to the {@link DatapathElement#fnArgs}. The number of args should match and the types should be compatibel.
   * See {@link #checkAndConvertArgTypes(List, Class[])}.
   * 
   * @param datapath The path, elements in any list element.
   * @param dataPool the object where the path starts from. It can be null. Static methods or creation of instances is possible then.
   * @param namedDataPool variables valid for the current block. It can be null, if not used in datapath.
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfully
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param bContainer If the element is a container, returns it. Elsewhere build a List
   *    to return a container for iteration. A container is any object implementing java.util.Map or java.util.Iterable
   * @return Any data object addressed by the path. Returns null if the last datapath element refers null.
   * 
   * @throws IllegalArgumentException if the datapath does not address an element. The exception message contains a String
   *  as hint which part does not match.
   */
  public static Object getData(
      List<DatapathElement> datapath
      , Object dataPool
      , Map<String, Object> namedDataPool
      //, boolean noException 
      , boolean accessPrivate,  boolean bContainer)
  throws NoSuchFieldException
  {
    Object data1 = dataPool;
    Iterator<DatapathElement> iter = datapath.iterator();
    DatapathElement element = iter.next();
    //if(element.constValue !=null){
    //  data1 = element.constValue;
    //  element = null;  //no more following
    //}
    //else 
    if(element.ident.startsWith("$position")){
      Assert.stop();
    }
    if(element.ident.startsWith("XXXXXXXX$$")){
      data1 = System.getenv(element.ident.substring(2));
      element = null;  //no next elements expected.
    }
    else if(element.whatisit == 'e'){
      data1 = System.getenv(element.ident);
      element = null;  //no next elements expected.
    }
    else if(element.whatisit == 'v'){
      if(namedDataPool ==null){
        throw new NoSuchFieldException("$?missing-datapool?");
      }
      if(!namedDataPool.containsKey(element.ident)){
        throw new NoSuchFieldException(element.ident);
      }
      data1 = namedDataPool.get(element.ident);  //maybe null if the value of the key is null.
      element = iter.hasNext() ? iter.next() : null;
    }
    else if(element.ident.startsWith("XXXXXXXXXX$")){
      if(namedDataPool ==null){
        throw new NoSuchFieldException("$?missing-datapool?");
      }
      if(!namedDataPool.containsKey(element.ident.substring(1))){
        throw new NoSuchFieldException(element.ident);
      }
      data1 = namedDataPool.get(element.ident.substring(1));  //maybe null if the value of the key is null.
      element = iter.hasNext() ? iter.next() : null;
    }
    while(element !=null){
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
          if(data1 !=null){
            data1 = invokeMethod(element, data1, accessPrivate, bContainer); 
          }
          //else: let data1=null, return null
        } break;
        case 's': data1 = invokeStaticMethod(element, accessPrivate, bContainer); break;
        default:
          if(data1 !=null){
            data1 = getData(element.ident, data1, accessPrivate, bContainer);
          }
          //else: let data1=null, return null
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
  
  
  
  
  static Object invokeMethod(      
    DatapathElement element
  , Object dataPool
  , boolean accessPrivate
  , boolean bContainer 
  ){
    Object data1 = null;
    Class<?> clazz = dataPool.getClass();
    if(element.ident.equals("checkNewless"))
      Assert.stop();
    try{ 
      Method[] methods = clazz.getDeclaredMethods();
      boolean bOk = false;
      for(Method method: methods){
        bOk = false;
        if(method.getName().equals(element.ident)){
          Class<?>[] paramTypes = method.getParameterTypes();
          
          Object[] actArgs = checkAndConvertArgTypes(element.fnArgs, paramTypes);
          if(actArgs !=null){
            bOk = true;
            try{ 
              data1 = method.invoke(dataPool, actArgs);
            } catch(IllegalAccessException exc){
              CharSequence stackInfo = Assert.stackInfo(" called ", 3, 5);
              throw new NoSuchMethodException("DataAccess - method access problem: " + clazz.getName() + "." + element.ident + "(...)" + stackInfo);
            } catch(InvocationTargetException exc){
              Assert.stop();
              throw exc;
            }
            
            break;  //method found.
          }
        }
      }
      if(!bOk) {
        Assert.stackInfo("", 5);
        CharSequence stackInfo = Assert.stackInfo(" called: ", 3, 5);
        throw new NoSuchMethodException("DataAccess - method not found: " + clazz.getName() + "." + element.ident + "(...)" + stackInfo);
      }
      //Method method = clazz.getDeclaredMethod(element.ident);
      //data1 = method.invoke(dataPool);
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //} catch 
    return data1;    
  }
  
  
  
  static Object invokeStaticMethod(      
      DatapathElement element
    , boolean accessPrivate
    , boolean bContainer 
    ) //throws ClassNotFoundException{
  { Object data1 = null;
    if(element.ident.equals("checkNewless"))
      Assert.stop();
    try{ 
      int posClass = element.ident.lastIndexOf('.');
      String sClass = element.ident.substring(0, posClass);
      String sMethod = element.ident.substring(posClass +1);
      Class<?> clazz = Class.forName(sClass);
      Method[] methods = clazz.getMethods();
      boolean bOk = false;
      for(Method method: methods){
        bOk = false;
        String sMethodName = method.getName();
        if(sMethodName.equals(sMethod)){
          Class<?>[] paramTypes = method.getParameterTypes();
          
          Object[] actArgs = checkAndConvertArgTypes(element.fnArgs, paramTypes);
          if(actArgs !=null){
            bOk = true;
            try{ 
              data1 = method.invoke(null, actArgs);
            } catch(IllegalAccessException exc){
              CharSequence stackInfo = Assert.stackInfo(" called ", 3, 5);
              throw new NoSuchMethodException("DataAccess - method access problem: " + clazz.getName() + "." + element.ident + "(...)" + stackInfo);
            }
            break;  //method found.
          }
        }
      }
      if(!bOk) {
        Assert.stackInfo("", 5);
        CharSequence stackInfo = Assert.stackInfo(" called: ", 3, 5);
        throw new NoSuchMethodException("DataAccess - method not found: " + clazz.getName() + "." + element.ident + "(...)" + stackInfo);
      }
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //} catch 
    return data1;    
  }
  
  
  /**Checks whether the given arguments matches to the necessary arguments of a method or constructor invocation.
   * Converts the arguments if possible and necessary:
   * <ul>
   * <li>provideArg  -> argType: conversion
   * <li>{@link java.lang.CharSequence} -> {@link java.lang.String}: arg.toString()
   * <li>{@link java.lang.CharSequence} -> {@link java.io.File} : new File(arg)
   * </ul>
   * @param providedArgs Given arguments
   * @param argTypes requested argument types
   * @return actArgs This array will be filled with converted parameter if all parameter matches.
   *   <br>If the number of args is 0, then Object[0] is returned.
   *   <br>null if the number of argtypes is not equal to the number of providedArgs or if the providedArgs and argTypes does not match. 
   *   The array have to be created with the size proper to 
   */
  static Object[] checkAndConvertArgTypes(List<Object> providedArgs, Class<?>[] argTypes){
    Object[] actArgs;
    if(argTypes.length == 0 && providedArgs == null){
      actArgs = new Object[0]; //matches, but no args.
    }
    else if(providedArgs !=null && argTypes.length == providedArgs.size()){
      //check it
      boolean bOk = true;
      int iParam = 0;
      //check the matching of parameter types inclusive convertibility.
      for(Object arg: providedArgs){
        Class<?> actType = arg.getClass();
        //check super classes and all interface types.
        //if(arg instanceof paramTypes[iParam]){ actArgs[iParam] = arg; }
        String typeName = argTypes[iParam].getName();
        if(actType == argTypes[iParam]){ bOk = true; }
        else if(arg instanceof CharSequence){
          if(argTypes[iParam] == File.class){ bOk = true; }
          else if(argTypes[iParam] == String.class){ bOk = true;  }
          else {bOk = false; }
        } else if(typeName.equals("Z") || typeName.equals("boolean")){
          bOk = true; //all can converted to boolean
        } else {
          Type[] ifcs = actType.getGenericInterfaces();
          Class[] clazzs = actType.getInterfaces();
          bOk = false; 
        }
        if(!bOk) { break; }
        iParam +=1;
      } //for, terminated with some breaks.
      if(bOk){
        actArgs = new Object[argTypes.length];
        iParam = 0;  //now convert instances:
        for(Object arg: providedArgs){
          Object actArg;
          String typeName = argTypes[iParam].getName();
          if(arg instanceof CharSequence){
            if(argTypes[iParam] == File.class){ actArg = new File(((CharSequence)arg).toString()); }
            else if(argTypes[iParam] == String.class){ actArg = ((CharSequence)arg).toString(); }
            else {
              actArg = arg;
            }
          } else if( (typeName = argTypes[iParam].getName()).equals("Z") || typeName.equals("boolean")){
            if(arg instanceof Boolean){ actArg = ((Boolean)arg).booleanValue(); }
            if(arg instanceof Byte){ actArg = ((Byte)arg).byteValue() == 0 ? false : true; }
            if(arg instanceof Short){ actArg = ((Short)arg).shortValue() == 0 ? false : true; }
            if(arg instanceof Integer){ actArg = ((Integer)arg).intValue() == 0 ? false : true; }
            if(arg instanceof Long){ actArg = ((Long)arg).longValue() == 0 ? false : true; }
            else { actArg = arg == null ? false: true; }
          } else {
            actArg = arg;
          }
          actArgs[iParam] = actArg;
          iParam +=1;
        } //for, terminated with some breaks.
      } else {
        actArgs = null;
      }
    } else { //faulty number of arguments
      actArgs = null;
    }
    return actArgs;
  }
  
  /**Gets data from a field or from an indexed container.
   *    
   * <ul>
   * <li>If the actual reference before is instanceof Map and the key is a String, then the next object
   *    is gotten from the map with the name used as the key.
   * <li>Elsewhere a field with ident as name is searched.
   * <li>If the actual reference before is instanceof {@link TreeNodeBase} and the field identifier is not found in this instance,
   *    a child node with the given name is searched. 
   *    The TreeNodeBase is the super class of {@link org.vishia.xmlSimple.XmlNodeSimple}
   *    which is used to present a ZBNF parse result. Therewith the {@link org.vishia.zbnf.ZbnfParser#getResultTree()}
   *    can be used as data input. The tag names of that result tree follow the semantic in the string given Syntax script.
   * </ul>
   * @param name Name of the field or key for container
   * @param dataPool The data where the field or element is searched
   * @param accessPrivate true than accesses also private data. 
   * @param bContainer only used for a TreeNodeBase: If true then returns the List of children as container, If false returns the first child with that name. 
   * @return The reference described by name.
   * @throws NoSuchFieldException If not found.
   */
  public static Object getData(
      String name
      , Object dataPool
      , boolean accessPrivate
      , boolean bContainer) 
  throws NoSuchFieldException
  {
    Object data1 = null;
    if(dataPool instanceof Map<?, ?>){
      data1 = ((Map<?,?>)dataPool).get(name);
    } 
    else {
      Class<?> clazz = dataPool.getClass();
      do{
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
          }
        }
        clazz = clazz.getSuperclass();
      } while(data1 ==null && clazz !=null);
      if(data1 ==null) {
        throw new NoSuchFieldException(name + " in " + dataPool.getClass().getName()); 
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
     * <li>'e': An environment variable.
     * <li>'v': A variable from the additional data pool.
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
    
    
    public void removeAllActualArguments(){ if(fnArgs !=null){ fnArgs.clear(); }}
    
    /**For debugging.*/
    @Override public String toString(){
      if(whatisit !='r'){ return ident;}
      else{
        return ident + "(...)";
      }
    }
  }

  
}
