
package org.vishia.util;

/**This class contains static String functions without any other dependency. 
 * In C the functions are contained in the Fwc/fw_String.c.
 * @author Hartmut Schorrig
 *
 */
/**
 * @author hartmut
 *
 */
public class StringFunctions {

  /**Version, history and license
   * <ul>
   * <li>2012-04-01 Hartmut new {@link #parseIntRadix(String, int, int, int, int[])} etc.
   *   taken from C-Sources CRunntimeJavalike/source/Fwc/fw_Simple.c
   * <li>2012-02-19 Hartmut created: basic functions also existent in C (Java2C-usage).
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
  
  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the indentier starts.
   * @param end max number of chars to check
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  public static int posAfterIdentifier(CharSequence src, int start, int endMax){ return posAfterIdentifier(src, start, endMax, null, null); }

  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the identifier starts.
   * @param end max number of chars to check
   * @param additionalStartChars maybe null, some chars as additional start chars of an identifier.
   * @param additionalChars maybe null, some chars as additional chars of an identifier.
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  public static int posAfterIdentifier(CharSequence src, int start, int endMax, String additionalStartChars, String additionalChars){
    int pos = start;
    char cc = src.charAt(pos);
    if(   cc == '_' 
      || (cc >= 'A' && cc <='Z') 
      || (cc >= 'a' && cc <='z') 
      || (additionalStartChars != null && additionalStartChars.indexOf(cc)>=0)
      )
    { pos +=1;
      while(  pos < endMax 
           && (  (cc = src.charAt(pos)) == '_' 
              || (cc >= '0' && cc <='9') 
              || (cc >= 'A' && cc <='Z') 
              || (cc >= 'a' && cc <='z') 
              || (additionalChars != null && additionalChars.indexOf(cc)>=0)
           )  )
      { pos +=1; }
    }
    return pos;
  }
  

  /**Parses a given String and convert it to the integer number.
   * The String may start with a negative sign ('-') and should contain digits after them.
   * The digits for radix > 10 where built by the numbers 'A'..'Z' respectively 'a'..'z',
   * known as hexa numbers A..F or a..f. 
   * @param srcP The String, non 0-terminated, see ,,size,,.
   * @param pos The position in src to start.
   * @param size The number of chars of the String.
   * @param radix The radix of the number, typical 2, 10 or 16, max 36.
   * @param parsedChars number of chars which is used to parse the integer. The pointer may be null if not necessary.
   * @return the Number.
   * @throws never. All possible digits where scanned, the rest of non-scanable digits are returned.
   *  At example the String contains "-123.45" it returns -123, and the retSize is 3.
   */
  public static int parseIntRadix(final String srcP, final int pos, final int sizeP, final int radix, final int[] parsedChars)
  { int val = 0;
    boolean bNegativ;
    int digit;
    char cc;
    int ixSrc = pos;
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    int maxHexDigitLower = 'A' + radix - 11; 
    int maxHexDigitUpper = 'a' + radix - 11; 
    if(srcP.charAt(ixSrc) == '-') { ixSrc+=1; size -=1; bNegativ = true; }
    else { bNegativ = false; }
    while(size > 0 && (digit = (cc = srcP.charAt(ixSrc)) - '0') >=0 
         && (  cc <= maxDigit 
            || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix
                             || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
         )  )                )
    { val = radix * val + digit;
      ixSrc+=1;
      size -=1;
    }
    if(bNegativ){ val = -val; }
    if(parsedChars !=null){
      parsedChars[0] = ixSrc - pos;
    }
    return( val);
  }
  
  
  /**Parses a given String backward and convert it to the integer number.
   * The String may start with a negative sign ('-') and should contain digits after them.
   * The digits for radix > 10 where built by the numbers 'A'..'Z' respectively 'a'..'z',
   * known as hexa numbers A..F or a..f. 
   * @param srcP The String.
   * @param pos The position in src to the last digit.
   * @param size The maximum of chars to parse. Should be less or equal pos.
   * @param radix The radix of the number, typical 2, 10 or 16, max 36.
   * @param parsedChars number of chars which is used to parse the integer. The pointer may be null if not necessary.
   * @return the Number.
   * @throws never. All possible digits where scanned, the rest of non-scanable digits are returned.
   *  At example the String contains "-123.45" it returns -123, and the retSize is 3.
   */
  public static int parseIntRadixBack(final String srcP, final int pos, final int sizeP, final int radix, final int[] parsedChars)
  { int val = 0;
    boolean bNegativ;
    int digit;
    char cc;
    int ixSrc = pos;
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    int maxHexDigitLower = 'A' + radix - 11; 
    int maxHexDigitUpper = 'a' + radix - 11; 
    int multPosition = 1;
    while(size > 0 && ixSrc >=0 && (digit = (cc = srcP.charAt(ixSrc)) - '0') >=0 
         && (  cc <= maxDigit 
            || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix
                             || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
         )  )                )
    { val += multPosition * digit;
      multPosition *= radix;
      ixSrc-=1;
      size -=1;
    }
    if(size > 0 && ixSrc >=0 && srcP.charAt(ixSrc) == '-') { 
      ixSrc-=1; size -=1; 
      val = -val;
    }
    if(parsedChars !=null){
      parsedChars[0] = pos - ixSrc;
    }
    return( val);
  }
  
  
  
  
  
  /**Parses a given String and convert it to the float number.
   * An exponent is not regarded yet (TODO).
   * @param src The String, see ,,size,,.
   * @param pos The position in src to start.
   * @param sizeP The number of chars to regard max (the String may be longer or shorter.
   * @param radix The radix of the number, typical 2, 10 or 16, max 36.
   * @param parsedChars number of chars which is used to parse the integer. The pointer may be null if not necessary.
   * @return the Number.
   * @param src
   * @param size
   * @param parsedCharsP
   * @return
   */
  public static float parseFloat(String src, int pos, int sizeP, int[] parsedCharsP)
  {
    int parsedChars = 0;
    float ret;
    int size = src.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int[] zParsed = new int[1];
    ret = (float)parseIntRadix(src, pos, size, 10, zParsed);
    parsedChars += zParsed[0];  //maybe 0 if .123 is written
    int ixsrc = pos + zParsed[0]; size -= zParsed[0];
    if(src.charAt(ixsrc)=='.'){
      float fracPart = (float)parseIntRadix(src, ixsrc +1, size-1, 10, zParsed);
      if(zParsed[0] >0){
        switch(zParsed[0]){
        case 1: fracPart *= 0.1f; break;
        case 2: fracPart *= 0.01f; break;
        case 3: fracPart *= 0.001f; break;
        case 4: fracPart *= 0.0001f; break;
        case 5: fracPart *= 1.0e-5f; break;
        case 6: fracPart *= 1.0e-6f; break;
        case 7: fracPart *= 1.0e-7f; break;
        case 8: fracPart *= 1.0e-8f; break;
        case 9: fracPart *= 1.0e-9f; break;
        case 10: fracPart *= 1.0e-10f; break;
        }
        ret += fracPart;
      }
      parsedChars += zParsed[0]+1;  //maybe 0 if .123 is written
      size -= zParsed[0]-1;
    }
    //TODO exponent
    if(parsedCharsP !=null){
      parsedCharsP[0] = parsedChars;
    }
    return ret;
  }

}
