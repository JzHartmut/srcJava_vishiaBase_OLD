package org.vishia.util;

/**Supports individual handling of assertions especially in debug phase. 
 * The application can create any special Assert class which extends this class
 * and override the both methods {@link #assertion(boolean)} and {@link #assertion(boolean, String)}.
 * The special instance can be set for the whole application calling {@link #setAssertionInstance(Assert)}.
 * It is also be possible to use a special assertion instance.
 * <br><br>
 * To check any assertion one can use the static methods 
 * <pre>
 * Assert.check(condition);
 * Assert.check(condition, msg);
 * </pre>
 * Then either this class (self instantiating) or the application wide Assert object is used.
 * <br>
 * The other possibility is, use a special modul-wide Assert object:
 * <pre>
 * Assert assert = new MyAssert();
 * ...
 * assert.assertion(condition);
 * assert.assertion(condition, msg);
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public class Assert
{

  /**Version, history and license
   * <ul>
   * <li>2012-09-02 Hartmut new {@link #exceptionInfo(String, Throwable, int, int)} and {@link #stackInfo(String, int)}
   *   to support a short info output for example for messages. Not the whole stacktrace!
   * <li>2012-08-30 Hartmut some enhancements, especially assert with send a message to System.err.
   * <li>2012-01-19 Hartmut created. The reason was: set an individual breakpoint on assertion statement.
   *   The second reason: flexibility for debugging. The java language 'assert' is too less in functionality. 
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
   * 
   */
  public static final int version = 20120828;

  
  
  /**This routine instantiates the assertion handling with a special user instance.
   * All invocations of the static assertion call Assert.{@link #check(boolean)} 
   * invokes the user {@link #assertion(boolean)} routine up to now.
   * @param instance The users assertion instance.
   */
  public static void setAssertionInstance(Assert instance){
    o = instance;
  }
  
  /**Checks whether an assertion is met.
   * This routine can invoke a special handling of assertion, if
   * @param shouldTrue
   */
  public static void check(boolean shouldTrue){
    if(o == null){ 
      o = new Assert(); //if no assertion instance is given, create this. 
    }
    o.assertion(shouldTrue);
  }
  
  
  /**Checks whether an assertion is met.
   * This routine can invoke a special handling of assertion, if
   * @param shouldTrue
   */
  public static void checkMsg(boolean shouldTrue, String msg){
    if(o == null){ 
      o = new Assert(); //if no assertion instance is given, create this. 
    }
    o.assertMsg(shouldTrue, msg);
  }
  
  
  public static CharSequence exceptionInfo(String startText, Throwable exc, int firstLevel, int nrofLevels){
    StringBuilder u = new StringBuilder(500);
    u.append(startText).append("; ");
    u.append(exc.getMessage()).append("; ");
    StackTraceElement[] stack = exc.getStackTrace();
    int zStack = stack.length;
    if(firstLevel >= zStack){ firstLevel = zStack-1; }
    if(zStack> firstLevel + nrofLevels){ zStack = firstLevel + nrofLevels; }
    for(int ix = 0; ix < zStack; ++ix){
      u.append(stack[ix].getMethodName())
      .append("(").append(stack[ix].getFileName())
      .append(":").append(stack[ix].getLineNumber())
      .append("); ");
    }
    return u;
  }
  

  public static CharSequence stackInfo(String startText, int nrofLevel){
    final CharSequence s;
    try{ throw new RuntimeException("stackInfo");
    } catch(RuntimeException exc){
      s = exceptionInfo(startText, exc, 1, nrofLevel);
    }
    return s;
  }

  
  /**This routine can handle a assertion to support debugging or reporting.
   * The Stacktrace can help to detect where the assertion occurs.
   * 
   * @param shouldTrue
   */
  public void assertion(boolean shouldTrue){
    if(!shouldTrue)
      assert(shouldTrue);
      
  }
  
  /**This routine can handle a assertion to support debugging or reporting.
   * The Stacktrace can help to detect where the assertion occurs.
   * 
   * @param shouldTrue
   */
  public void assertion(boolean shouldTrue, String msg){
    if(!shouldTrue)
      throw new RuntimeException(msg);
      
  }
  
  /**Assert the condition, writes a message to System.err if it is false.
   * @param shouldTrue
   * @param msg If it is null, an info from stacktrace is build.
   */
  public void assertMsg(boolean shouldTrue,  CharSequence msg){
    if(!shouldTrue){
      if(msg == null){
        msg = stackInfo("assertMsg ", 4);
      }
      System.err.println(msg);
    }
  }
  
  private static Assert o;
  

}
