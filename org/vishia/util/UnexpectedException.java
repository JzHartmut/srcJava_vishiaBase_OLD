package org.vishia.util;

/**Use this exception type if an exception is not expected. Use it before you let the case empty!
 * @author Hartmut Schorrig
 *
 */
public class UnexpectedException extends RuntimeException{

  private static final long serialVersionUID = -7395481201677194625L;

  public UnexpectedException(Exception exc){
    super("This exception is not expected. It seems to be a software failure. ", exc);
  }
}
