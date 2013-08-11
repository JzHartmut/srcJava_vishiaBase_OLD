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
  
  /**Version, history and license
   * <ul>
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
  public final static int version = 0x20120219; 

  
  public @interface StackInstance{}
	
  /**Sets that the following array has a fix size. It may be with or without a head - ObjectArrayJc.
   * Use {@link SimpleArray} to designate whether it has a head structure or not.
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FixArraySize{ int value(); }
	
  public @interface SimpleArray{  }
	
  /**Designates that the following variable is a variable for dynamic call, a method-table-reference. */
  public @interface DynamicCall{  }
	
  /**The String is used only non-Persist. Written on definition of String variable. */
  public @interface nonPersistent{}
  
  /**The String is designated as non-persistent, especially located in ThreadContext. */
  public @interface toStringNonPersist{}
  
  /**The method is not translated to C. It means the method with given name is programmed in C direct. */
  public @interface exclude{}
  
  /**Declare the String as const char* in C-language. */
  public @interface zeroTermString{}
  
  /**The method is translated building a simple macro. */
  public @interface define{}
  
}
