/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**This class contains annotations for the Java-to-C translation. Therefore it is named 'Java for C'.
 * @author Hartmut Schorrig
 *
 */
public interface Java4C {
  
  /**Version, history and license.
   * <ul>2014-09-05 Hartmut chg all annotation written beginning with upper case letters.
   * <li>2012-08-22 Hartmut new {@link exclude} for elements and classes which should not be generated in C
   * <li>2011-01-05 Hartmut created: It is better to use java language annotations instead annotations in comment,
   *   because an selection/auto completion is available for them for example in Eclipse environment.
   * </ul>
   * <br><br>
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
   */
  public final static String sVersion = "2014-01-12"; 


  /**Only a help for forcing a Java2C parsing error especially for sophisticated Java2XMI conversion behaviour, only for test. */ 
  public @interface ParseError{}

  
  /**Defines that the <code>class Type{ ... }</code> does not base on ObjectJc in C. It is a simple struct without ObjectJc head data.
   * Note that such an instance does not support overridden methods and does not support Reflection of derived types with a base reference. 
   * A derivation is possible. */
  public @interface NoObject{}
    
  /**Defines that the <code>Type instance = new Type(args);</code> is only used in this method, in Java garbaged after the end of the method
   * and therefore it is possible to create an instance as Stack variable in C language.
   * Assure that a reference of this instance is not stored outside of the routine!
   */
  public @interface StackInstance{}
    
  /**Sets that the following array has not a Array head structure - not an ObjectArrayJ.
   * It is a embedded array if the variable is final and construct in the same line:
   * <pre>
   * @Java4C.SimpleArray final int myArray[100]
   * <Pre>
   * produces in C a definition:<pre>
   *   int myArray[100];
   * </pre>  
   */
  public @interface SimpleArray{ }
    
  /**Sets that the following array as a simple embedded Array with the given size.
   * It is possible to construct the array later in the constructor in Java because the size should be calculated for Java usage.
   * In C the size is the given value.
   * <pre>
   * @Java4C.SimpleArraySize(100) int myArray;
   * <Pre>
   * produces in C a definition:<pre>
   *   int myArray[100];
   * </pre>  
   * TODO needed? usability?
   */
  public @interface SimpleArraySize{ int value(); }
    
  /**Sets that the following array has a fix size. It may be with or without a head - ObjectArrayJc.
   * Use {@link SimpleArray} to designate whether it has a head structure or not.
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FixArraySize{ int value(); }
    
  /**Sets the following association as simple pointer in C, without garbage collection usage. 
   * For example <pre>
   *   MyClass element; 
   * </pre>
   * will be translated in C to <pre>
   *   MyClass_pkg* element;
   * </pre>  
   */
  public @interface SimpleRef{  }
  
  /**Produces a const modifier for a reference (refers a const object). 
   * Note that the const of C/C++ is not supported by the Java language. But it may be important for C/C++.
   * In Java it should asserted that the referenced Object won't be changed.
   * In opposite, a final modifier on the reference means in Java and in C: The reference itself won't be changed.
   */
  public @interface ConstRef{ }
    
  /**Produces a const modifier for the thiz- reference. The method does not change any data of this. 
   * Note that the const of C/C++ is not supported by the Java language. But it may be important for C/C++.
   * In Java it should asserted that the referenced Object won't be changed.
   * In opposite, a final modifier on the reference means in Java and in C: The reference itself won't be changed.
   */
  public @interface ConstThis{ }
    
  /**Sets the following array as simple reference. An byte[] in Java is a int8* in C. */
  public @interface SimpleArrayRef{  }
    
  /**Sets the following array as simple reference. An byte[] in Java is a int8* in C. 
   * It is the same as SimpleArrayRef. But in Java only array[0] should be used as referenced type. */
  public @interface SimpleVariableRef{  }
  
  
  /**The array which is designated with PtrVal is provided in C with a PtrVal_Type reference. 
   * The array should have only 1 dimension. The PtrVal_Type is defined in C like <pre>
   * struct { Type* ptr__, int32 value__} PtrVal_Type;
   * </pre>
   * This definition is contained in os_types_def.h because it may depend on the platform's C-compiler. 
   * The struct should pass values in 2 register of the processor. It is the same like a MemC-reference, but the ptr__ is type-specific.
   * */
  public @interface PtrVal{  }
  
  
  /**The byte[] array is provided in C like a MemC reference. */
  public @interface ByteStringJc { }
  
  
  /**Designates that the following variable is a variable for dynamic call, a method-table-reference. */
  public @interface DynamicCall{  }
  
  /**The String is used only non-Persist or a CharSequence is used as non-Persist String. 
   * Written on definition of String variable. In C the String or CharSequence can be hold in a StringJc
   * which can refer a String in the Stack. In Java this reference should only be a Stack reference.
   * The reference should not be stored on any other location. */
  public @interface NonPersistent{}
  
  /**The CharSequence reference should be used as a StringJc instance.
   * It is applicable if the CharSequence variable in Java is set only from a StringBuilder or a String,
   * it is without conversion. String and StringBuilder/StringBuffer implements the CharSequence interface.
   * If the CharSequence comes from any other instance, it can't be converted to a StringJc in C,
   * then this annotation should not be used.
   * */
  public @interface StringJc{}
  
  /**The String is designated as non-persistent, especially located in ThreadContext. */
  public @interface ToStringNonPersist{}
  
  public @interface StringBuilderInThreadCxt{}

  public @interface StringBuilderInStack{ int value();}
  
  /**The method or field is not existing for C-translation. Both the definition and the implementation
   * is not translated to C. */
  public @interface Exclude{}
  
  /**The implementation of the method is not translated to C. 
   * Usual the method with given name is programmed in C direct. */
  public @interface ExcludeImpl{}
  
  
  /**The class does not implement the named interface in C. */
  public @interface ExcludeInterface{ String value(); }
  
  /**Declare the String as const char* in C-language. */
  public @interface ZeroTermString{}
  
  /**The method is translated building a simple macro or inline method for C++. */
  public @interface Inline{}
  
  /**The method is translated building a simple macro or inline method for C++ which returns a value. */
  public @interface Retinline{}
  
  /**The class contains only methods. The super class should be enhanced with that methods whithout build
   * a special class. */
  public @interface ExtendsOnlyMethods{}
  
}
