package org.vishia.util;

/**This class extends the capability of StringPartBase with the same properties.
 * @author Hartmut Schorrig
 *
 */
public class StringPartScan extends StringPartBase
{
  public StringPartScan(CharSequence src, int start, int end)
  { super(src, start, end);
  }

  public StringPartScan(CharSequence src)
  { super(src);
  }

  
  
  /** Set the mode of ignoring comments.
   * If it is set, comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The string introduces and finishes a comment is setted by calling 
   * setIgnoreComment(String sStart, String sEnd). The default value is "/ *" and "* /" like in java-programming. 
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of this feature.
   */
  public boolean setIgnoreComment(boolean bSet)
  { boolean bRet = (bitMode & mSkipOverCommentInsideText_mode) != 0;
    if(bSet) bitMode |= mSkipOverCommentInsideText_mode;
    else     bitMode &= ~mSkipOverCommentInsideText_mode;
    return bRet;
  }
  
  
  /** Set the character string of inline commentmode of ignoring comments.
   * After this call, comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart Start character string of a inline comment
   * @param sEnd End character string of a inline comment
   * @return The last definition of the feature setIgnoreComment(boolean).
   */
  public boolean setIgnoreComment(String sStart, String sEnd)
  { boolean bRet = (bitMode & mSkipOverCommentInsideText_mode) != 0;
    bitMode |= mSkipOverCommentInsideText_mode;
    sCommentStart = sStart; 
    sCommentEnd   = sEnd;
    return bRet;
  }
  
  
  /** Set the mode of ignoring comments to end of line.
   * If it is set, end-line-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The string introduces a endofline-comment is setted by calling 
   * setEndlineCommentString(). The default value is "//" like in java-programming. 
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of the feature setIgnoreComment(boolean).
   */
  public boolean setIgnoreEndlineComment(boolean bSet) 
  { boolean bRet = (bitMode & mSkipOverCommentToEol_mode) != 0;
    if(bSet) bitMode |= mSkipOverCommentToEol_mode;
    else     bitMode &= ~mSkipOverCommentToEol_mode;
    return bRet;
  }
  

  
  /** Set the character string introducing the comments to end of line.
   * After this call, endline-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart String introducing end line comment
   * @return The last definition of this feature.
   */
  public boolean setIgnoreEndlineComment(String sStart) 
  { boolean bRet = (bitMode & mSkipOverCommentToEol_mode) != 0;
    bitMode |= mSkipOverCommentToEol_mode;
    sCommentToEol = sStart;
    return bRet;
  }
  
  /** Set the mode of ignoring whitespaces.
   * If it is set, whitespaces are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The chars accepted as whitespace are setted by calling 
   * setWhiteSpaceCharacters(). The default value is " \t\r\n\f" like in java-programming.
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of this feature.
   */
  public boolean setIgnoreWhitespaces(boolean bSet)
  { boolean bRet = (bitMode & mSkipOverWhitespace_mode) != 0;
    if(bSet) bitMode |= mSkipOverWhitespace_mode;
    else     bitMode &= ~mSkipOverWhitespace_mode;
    return bRet;
  }
  
  
  
  

  
}
