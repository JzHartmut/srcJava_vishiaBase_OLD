package org.vishia.msgDispatch;

public class ExceptionInfo {

  
  public static String exceptionMsg1(Exception exc){
    StackTraceElement[] stack = exc.getStackTrace();
    if(stack.length >=1){
      return exc.getMessage() + "; in "+ stack[0].getFileName() + ":" + stack[0].getLineNumber();
    } else {
      return exc.getMessage() + "; in - no stacktrace info.";
    }
  }
}
