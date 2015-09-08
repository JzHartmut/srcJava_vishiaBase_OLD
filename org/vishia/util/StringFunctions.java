
package org.vishia.util;

import java.nio.charset.Charset;


/**This class contains static String functions without any other dependency. 
 * In C the functions are contained in the Fwc/fw_String.c.
 * @author Hartmut Schorrig
 *
 */
public class StringFunctions {

  /**Version, history and license.
   * <ul>
   * <li>2015-06-05 Hartmut chg: {@link #equals(CharSequence, int, int, CharSequence)} regards null-pointer too.
   * <li>2014-09-05 Hartmut new: Twice methods {@link #indexOf(CharSequence, int, int, String)} and {@link #indexOf(CharSequence, int, int, CharSequence)}.
   *   Twice methods {@link #lastIndexOf(CharSequence, int, int, String)} and {@link #lastIndexOf(CharSequence, int, int, CharSequence)}.
   *   The methods are the same in Java. But in C the handling of reference is different. In Java2C translation a StringJc does not base on CharSequence
   *   because it is a simple reference to char[] and a length only. CharSequence needs ObjectJc and virtual methods. 
   * <li>2014-05-04 Hartmut new: {@link #indexOfAnyChar(CharSequence, int, int, String)}: Algorithm transfered from 
   *   {@link StringPart#indexOfAnyChar(String, int, int, char, char, char)} to this class for common usage,
   *   called in StringPart. TODO do it with all that algoritm.
   * <li>2014-05-04 Hartmut new {@link #cEndOfText} now defined here, parallel to {@link StringPart#cEndOfText}.
   *   Note: that char is ASCII but not UTF.   
   * <li>2014-03-11 Hartmut new: {@link #indent2(int)}
   * <li>2013-09-07 Hartmut new: {@link #parseFloat(String, int, int, char, int[])} with choiceable separator (123,45, german decimal point)
   * <li>2013-09-07 Hartmut new: {@link #convertTranscription(CharSequence, char)} used form {@link SpecialCharStrings#resolveCircumScription(String)}
   * <li>2013-08-29 Hartmut bugfix: {@link #compare(CharSequence, int, CharSequence, int, int)}, {@link #indexOf(CharSequence, CharSequence, int)}
   * <li>2013-08-10 Hartmut new: {@link #parseIntRadix(String, int, int, int, int[], String)} now can skip
   *   over some characters. In this kind a number like 2"123'456.1 is able to read.
   * <li>2013-08-10 Hartmut new: {@link #parseLong(String, int, int, int, int[], String)} as counterpart to parseInt  
   * <li>2013-07-28 Hartmut new: {@link #isEmptyOrOnlyWhitespaces(CharSequence)} 
   * <li>2013-05-04 Hartmut new some methods for usage CharSequence: {@link #compare(CharSequence, int, CharSequence, int, int)},
   *   {@link #startsWith(CharSequence, CharSequence)}, {@link #endsWith(CharSequence, CharSequence)},
   *   {@link #indexOf(CharSequence, char, int)}, {@link #indexOf(CharSequence, CharSequence, int)}
   *   Generally usage of CharSequence as StringBuilder instance saves calculation time in comparison with usage String,
   *   because a new allocation is saved. This saving can be done any time if the StringBuilder is non thread-shared
   *   and its reference is not stored permanently but only used immediately in the thread.
   * <li>2013-02-03 Hartmut new  {@link #compare(CharSequence, CharSequence)} and {@link #equals(Object)}.
   * <li>2012-08-22 Hartmut new {@link #copyToBuffer(String, char[])} and {@link #copyToBuffer(String, byte[], Charset)}:
   *   This methods are existent at the C-level. They are usefully if dynamic memory usage should be prevented.
   *   They are need for Java-usage with static data too. 
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
  public final static int version = 20130810; 
  
  
  /** The char used to code end of text. It is defined in ASCII as EOT. 
   * In Unicode it is the same like {@value Character#TITLECASE_LETTER}, another meaning. */  
  public static final char cEndOfText = (char)(0x3);


  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the identifier starts.
   * @param endq max number of chars to check
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
  
  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the indentier starts.
   * @param endq max number of chars to check
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  public static int posAfterIdentifier(CharSequence src, int start, int endMax){ return posAfterIdentifier(src, start, endMax, null, null); }


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
  public static int parseIntRadix(final String srcP, final int pos, final int sizeP, final int radix
      , final int[] parsedChars, final String spaceChars)
  { int val = 0;
    boolean bNegativ;
    int digit;
    char cc;
    int ixSrc = pos;
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    if(size > 0 && srcP.charAt(ixSrc) == '-') { 
      ixSrc+=1; size -=1; bNegativ = true; 
    }
    else { bNegativ = false; }
    while(--size >= 0){
      cc = srcP.charAt(ixSrc);
      if(spaceChars !=null && spaceChars.indexOf(cc)>=0){
        ixSrc +=1;
      } else if((digit = cc - '0') >=0 
          && (  cc <= maxDigit 
              || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix
                               || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
           )  )                )
      { val = radix * val + digit;
        ixSrc+=1;
      } else {
        break;
      }
    }
    if(bNegativ){ val = -val; }
    if(parsedChars !=null){
      parsedChars[0] = ixSrc - pos;
    }
    return( val);
  }
  

  
  @Java4C.Inline public static int parseIntRadix(final String srcP, final int pos, final int sizeP, final int radix, final int[] parsedChars)
  {
    return parseIntRadix(srcP, pos, sizeP, radix, parsedChars, null);
  }

  
  /**Adequate method for long values, see {@link #parseIntRadix(String, int, int, int, int[], String)}.
   */
  public static long parseLong(final String srcP, final int pos, final int sizeP, final int radix
      , final int[] parsedChars, final String spaceChars)
  { long val = 0;
    //exact same lines as parseInt
    boolean bNegativ;
    int digit;
    char cc;
    int ixSrc = pos;
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    if(srcP.charAt(ixSrc) == '-') { ixSrc+=1; size -=1; bNegativ = true; }
    else { bNegativ = false; }
    while(--size >= 0){
      cc = srcP.charAt(ixSrc);
      if(spaceChars !=null && spaceChars.indexOf(cc)>=0){
        ixSrc +=1;
      } else if((digit = cc - '0') >=0 
          && (  cc <= maxDigit 
              || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix
                               || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
           )  )                )
      { val = radix * val + digit;
        ixSrc+=1;
      } else {
        break;
      }
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
  
  
  public static float parseFloat(String src, int pos, int sizeP, int[] parsedCharsP)
  { return parseFloat(src, pos, sizeP, '.', parsedCharsP);
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
  public static float parseFloat(String src, int pos, int sizeP, char decimalpoint, int[] parsedCharsP)
  {
    int parsedChars = 0;
    float ret;
    int restlen = src.length() - pos;
    if(restlen > sizeP){ restlen = sizeP; }
    int[] zParsed = new int[1];
    ret = parseIntRadix(src, pos, restlen, 10, zParsed);
    parsedChars += zParsed[0];  //maybe 0 if .123 is written
    int ixsrc = pos + zParsed[0]; 
    restlen -= zParsed[0];
    if(ixsrc < (restlen+pos) && src.charAt(ixsrc)==decimalpoint){
      float fracPart = parseIntRadix(src, ixsrc +1, restlen-1, 10, zParsed);
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
      restlen -= zParsed[0]-1;
    }
    //TODO exponent
    if(parsedCharsP !=null){
      parsedCharsP[0] = parsedChars;
    }
    return ret;
  }

  
  /**Copies the content of a String in the given byte buffer with the requested encoding.
   * If this method is used in a C/C++ environment, it is programmed in a special maybe more simple way
   * because not all encodings are supported. Usual a String is only a byte array itself, it is copied.  
   * @param src The src String
   * @param buffer The desination buffer with given length. The last byte will be set to 0.
   * @param encoding The encoding. If null, then use the UTF8-encoding.
   * @return number of bytes in buffer.
   */
  @Java4C.Exclude public static int copyToBuffer(String src, byte[] buffer, Charset encoding){
    if(encoding == null){ 
      encoding = Charset.forName("UTF8"); 
    }
    byte[] bytes = src.getBytes(encoding);
    int nChars = bytes.length;
    if(buffer.length < nChars){
      nChars = buffer.length -1;
      int ix;
      for(ix=0; ix<nChars; ++ix){
        char cc = src.charAt(ix);
        buffer[ix] = bytes[ix];  
      }
      buffer[ix] = 0;
    }
    return nChars;
  }
  
  
  /**Copies the content of a String in the given char buffer.
   * If this method is used in a C/C++ environment, the char buffer may be a byte buffer really. 
   * @param src The src String
   * @param buffer The desination buffer with given length. The last byte will be set to 0.
   * @return number of chars in buffer.
   */
  @Java4C.Exclude public static int copyToBuffer(String src, char[] buffer){
    int nChars = src.length();
    if(buffer.length < nChars){
      nChars = buffer.length -1;
    }
    int ix;
    for(ix=0; ix<nChars; ++ix){
      char cc = src.charAt(ix);
      buffer[ix] = src.charAt(ix);  
    }
    buffer[ix] = 0;
    return nChars;
  }
  
  
  
  /**Converts a String in a buffer in a java.lang.String.
   * @param buffer It is zero-terminated.
   * @return A String which contains all characters till the first '\0' or the whole buffer.
   */
  @Java4C.Exclude public static String z_StringJc(char[] buffer){
    int ix=-1;
    while(++ix < buffer.length && buffer[ix] !='0');
    return new String(buffer, 0, ix);
  }
  

  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}.
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   *  
   * @param s1 left char sequence
   * @param s2 right char sequence
   * @return 0 if all characters are equal, positive if s1 > s2, s1 < s2.
   *   <br>The absolute of return is the number of equal characters +1. 
   *   <br>-1 means, the first character is different whereby s1.charAt(0) < s2.charAt(0)
   *   <br> 1 means, the first character is different whereby s1.charAt(0) > s2.charAt(0)
   *   
   */
  public static int comparePos(CharSequence s1, CharSequence s2){
    return comparePos(s1, 0, s2, 0, -1);
  }
  
  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.)
   * It detects that position where the Strings are different. That is not done by {@link String#compareTo(String)}
   * or {@link #compare(CharSequence, int, CharSequence, int, int)}
   *  
   * @param s1 left char sequence
   * @param from1 start position
   * @param s2 right char sequence
   * @param from2 start position
   * @param nrofChars maximal number of chars to compare. It can be negative or {@link java.lang.Integer#MAX_VALUE}
   *   to compare all characters to the end. Use -1 to compare all characters is recommended.
   *   Note: if 0, the return value of this method is 0 because all (=0) characters are equal. This may be important for some extrem situations.
   * @return 0 if all characters are equal, positive if the part of s1 > s2,  negative if s1 < s2.
   *   <br>The absolute of return is the number of equal characters +1.
   *   <br>Note that the different character is charAt(returnValue -1) or the length of the shorter CharSeqence is returnVal -1.
   *     This convention is necessary because 0 means equal. It should be distinguish from the result charAt(0) is different.
   *   <br>-1 means, the first character is different whereby s1.charAt(0) < s2.charAt(0) or s1.length()==0 && s2.length() >0
   *   <br> 1 means, the first character is different whereby s1.charAt(0) > s2.charAt(0) or s1.length() >= && s2.length()==0
   *   <br> The comparison of "abcx" with "abcy" results -4 because 'x' < 'y' on the position 3.
   *   <br> The comparison of "abc" with "abcy" results -4 because 'x' < 'y' on the position 3.
   *   
   */
  public static int comparePos(CharSequence s1, int from1, CharSequence s2, int from2, int nrofChars){
    int i1 = from1;
    int i2 = from2;  //post-increment
    int z1 = s1.length();
    int z2 = s2.length();
    if(nrofChars ==0) return 0; //NOTE: following while compares at least one char
    int zChars =  nrofChars >=0 ? Math.min(nrofChars, Math.min(z1- i1, z2-i2)) : Math.min(z1-i1, z2-i2);
    //z1 -=1; z2 -=1;  //compare before increment then.
    char c1, c2;
    do {
      c1 = s1.charAt(i1++);
      c2 = s2.charAt(i2++);
    } while(c1 == c2 && --zChars >0);
    if(zChars ==0){
      //all characters compared, maybe difference in length.
      if(i2 < z2) return -(i1 - from1 +1);  //s2 is longer, s1 is less.
      else if(i1 < z1) return i1 - from1 +1;  //positive value: s1 is greater because i1 < z2, is longer and c1==c2 
      else return 0;  //both equal, comparison to end. 
    } 
    else {
      //not all possible characters compared, difference in character
      if(c1 < c2) return -(i1 - from1);  //c1 !=c2, then compare the last characters. <0 because s1 is lesser.
      else return (i1 - from1);               //note: == i2 - from2, s2 is lesser.
    }
  }
  
  
  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}.
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   *  
   * @param s1 left char sequence
   * @param from1 start position
   * @param s2 right char sequence
   * @param from2 start position
   * @param nrofChars maximal number of chars to compare. It can be {@link java.lang.Integer#MAX_VALUE}
   *   to compare all characters to the end.
   * @return 0 if all characters are equal, 1 if the part of s1 > s2,  -1 if s1 < s2.
   */
  public static int compare(CharSequence s1, int from1, CharSequence s2, int from2, int nrofChars){
    int i1 = from1 -1;
    int i2 = from2 -1;  //pre-increment
    int z = nrofChars + from1;
    int returnEq = 0;
    if(z > s1.length()){ 
      z = s1.length();
      int nrofChars1 = z - from1;
      int z2= from2 + nrofChars1;
      if( z2 == s2.length()){ returnEq = 0; }  //both have the same length after shorten.
      else if(z2 > s2.length()){
        int nrofChars2 = s2.length() - from2;
        z = from1 + nrofChars2;   //reduce length because s2
        returnEq = 1;  //returns 1 if equal because s2 is shorter
      }
      else {returnEq = -1; }    //returns -1 if equal because s1 is shorter
    } 
    else if((from2 + nrofChars) > s2.length()){ 
      //s2 is shorter than the requested or adjusted length:
      z = (s2.length()-from2) + from1;
      returnEq = 1;     //returns 1 if equal because s2 is shorter
    } 
    while(++i1 < z){
      char c1 = s1.charAt(i1), c2 =s2.charAt(++i2);
      if(c1 != c2){
        if(c1 < c2){ return -1; }
        else if(c1 > c2){ return 1; }
      }
    }
    //all chars till z are equal.
    return returnEq;
  }
  
  
  /**Compares two Strings or StringBuilder-content or any other CharSequence.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}. 
   * @param s1
   * @param s2
   * @return 0 if all characters are equal, 1 if s1 > s2,  -1 if s1 < s2
   */
  public static int compare(CharSequence s1, CharSequence s2){
    return compare(s1, 0, s2, 0, Integer.MAX_VALUE);
  }  
  
  
  /**Compares two charsequences. It is similar String.equals(String), but works with CharSequence and accepts null-pointer.
   * @param s1 first, if null then returns true if s2== null. Equals is both null too.
   * @param from start position in s1
   * @param to exclusive end position in s1, if <0, especially -1 or > s1.length, then till length of s1. That is 'endsWith'.
   * @param s2 to compare with
   * @return true if all chars equals or both null.
   */
  public static boolean equals(CharSequence s1, int from, int to, CharSequence s2){
    int z1 = s1.length();
    if(s1 == null || s2 == null){ return s1 == s2; }  //equals is both null too
    int zz = to < 0 || to > z1 ? z1 - from : to - from;
    if( zz != s2.length()) return false;
    else {
      for(int ii = 0; ii<zz; ++ii){
        if(s1.charAt(from + ii) != s2.charAt(ii)) return false;
      }
      return true;
    }
  }
  

  /**Compares two Strings or StringBuilder-content or any other CharSequence.
   * It is the adequate functionality like {@link java.lang.String#equals(Object)}.
   * But the  {@link java.lang.String#equals(Object)} does only compare instances of Strings,
   * it does not compare a String with any other {@link java.lang.CharSequence} whether there are equal.
   * Not that: <pre>
   * String str = "abc";
   * String str2 = "abc";
   * StringBuilder sb = new StringBuilder(str);
   * assert(str.equals(str2));
   * assert(str.contentEquals(sb));  //special String comparator
   * assert( ! str.equals(sb));      //it is not equals, sb is not a String.
   * assert(StringFunctions.equals(str, sb));
   * assert(StringFunctions.equals(sb, str)); //compares any CharSequences
   * </pre>
   * @param s1
   * @param s2
   * @return 0 if all characters are equal, 1 if s1 > s2,  -1 if s1 < s2
   */
  public static boolean equals(CharSequence s1, CharSequence s2){
    return s1 == null ? s2 == null : equals(s1, 0, s1.length(), s2);
  }

  
  
  /**Checks whether the given CharSequence starts with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   */
  public static boolean startsWith(CharSequence sq, CharSequence start){
    return compare(sq, 0, start, 0, start.length()) == 0;
  }
  

  /**Checks whether the given CharSequence starts with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   */
  public static boolean startsWith(CharSequence sq, int from, int to, CharSequence start){
    int zstart = start.length();
    if((to - from) < zstart) return false;
    return compare(sq, from, start, 0, zstart) == 0;
  }
  

  /**Checks whether the given CharSequence ends with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   */
  public static boolean endsWith(CharSequence sq, CharSequence end){
    int z = end.length();
    if(z > sq.length()) return false;
    else return compare(sq, sq.length()-z, end, 0, z) == 0;
  }
  

  
  /**Returns false if at least one char was found in text which is not a whitespace.
   * A whitespace is one of " \t\n\r" 
   * @param text to check
   * @return true if text is empty or contains only whitespaces.
   */
  public static boolean isEmptyOrOnlyWhitespaces(CharSequence text){
    char cc;
    int zz = text.length();
    int ii = -1;
    while(++ii < zz){
      cc = text.charAt(ii);
      if(" \t\n\r".indexOf(cc) <0){ return false; } //other character than whitespace
    }
    return true;
  }
  
  
  
  /**Searches the first occurrence of the given CharSequence in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq search into
   * @param fromIndex start search
   * @param to end search, exclusive. If end > sq.length() then search till end. 
   *   Especially Integer.MAX_VALUE can be used. Alternatively use {@link #indexOf(CharSequence, char, int)}.
   * @param ch The character which is searched.
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, int fromIndex, int to, char ch){
    int zsq = sq.length();
    int max = to > zsq ? zsq : to;
    int ii = fromIndex-1;  //pre-increment
    if (fromIndex < 0) {
        ii = -1;
    } else if (fromIndex >= max) {
        return -1;
    }
    while(++ii < max){
      if(sq.charAt(ii) == ch) {
        return ii;
      }
    }
    return -1;  //not found;
  }
  

  
  /**Searches the first occurrence of the given Character in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, char ch, int fromIndex){
    return indexOf(sq, fromIndex, Integer.MAX_VALUE, ch);
  }
  
  

  /**Searches the first occurrence of the given Character in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, char ch){
    return indexOf(sq, 0, Integer.MAX_VALUE, ch);
  }
  
  

  
  /**Searches any char inside sChars in the given Charsequence
   * @param begin start position to search in sq
   * @param end length of sq or end position to test
   * @param sChars Some chars to search in sq
   *   If sChars contains a EOT character (code 03, {@link #cEndOfText}) then the search stops at this character 
   *   or it is continued to the end of the range in sq. Then the length of the text range is returned
   *   if another character in sChars is not found. 
   *   It means: The end of the text range is adequate to an EOT-character. Note that EOT is not unicode,
   *   but it is an ASCII control character.  
   * @return The first position in sq of one of the character in sChars or -1 if not found in the given range.
   */
  public static int indexOfAnyChar(CharSequence sq, int begin, int end, String sChars)
  { int pos = begin-1;  //pre-increment
    while(++pos < end && sChars.indexOf(sq.charAt(pos)) < 0){ }  //while any of char in sChars not found:
    if(pos < end 
      || (pos == end && sChars.indexOf(cEndOfText) >= 0)
      )
    { return pos;
    }
    else  return -1;
  }

  

  
  /**Searches the last occurrence of the given char in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#lastIndexOf(char, fromEnd)}. 
   * @param sq Any sequence
   * @param from range, it ends searching on from
   * @param to if > sq.length() uses sq.length(), it starts searching on to-1
   * @param ch to search
   * @return -1 if not found, elsewhere the position inside sq, >=fromIndex and < to 
   */
  public static int lastIndexOf(CharSequence sq, int from, int to, char ch){
    int zsq = sq.length();
    int ii = to > zsq ? zsq : to;
    while(--ii >= from){
      if(sq.charAt(ii) == ch) {
        return ii;
      }
    }
    return -1;  //not found;
  }
  

  /**Searches the last occurrence of the given char in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#lastIndexOf(char)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int lastIndexOf(CharSequence sq, char ch){
    return lastIndexOf(sq, 0, Integer.MAX_VALUE, ch);
  }

  /**Searches the last occurrence of the given char in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#lastIndexOf(char, fromEnd)}. 
   * @param sq Any sequence
   * @param from range, it ends searching on from
   *   if from < 0 it throws IndexOutOfBoundsException
   * @param to if > sq.length() uses sq.length(), it starts searching on to-1
   *   if to < from then returns -1 always.
   *   if to < 0 then returns 
   * @param chars to search
   * @return -1 if not found, elsewhere the position inside sq, >=fromIndex and < to 
   * @throws IndexOutOfBoundsException if from < 0 or to < 0
   */
  public static int lastIndexOfAnyChar(CharSequence sq, int from, int to, String chars){
    int zsq = sq.length();
    int ii = to > zsq ? zsq : to;
    if(from <0) throw new IndexOutOfBoundsException("StringFunctions.lastIndexOfAnyChar - form <0; " + from);
    while(--ii >= from && chars.indexOf(sq.charAt(ii))<0) {} //pre-decrement.
    return ii >= from? ii+1 : -1;  //not found;
  }
  

  /**Checks whether the given CharSequence contains the other given CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence of str in sq which is >= fromIndex. 
   */
  public static int indexOf(CharSequence sq, int fromIndex, int to, String str){
    int zsq = sq.length();
    int max = (to >= zsq ? zsq : to) - str.length()+1 ;
    int ii = fromIndex-1;  //pre-increment
    if (fromIndex < 0) {
        ii = -1;
    } else if (fromIndex >= max) {
        return -1;
    }
    char ch = str.charAt(0);   //search first char of str
    while(++ii < max){
      if(sq.charAt(ii) == ch) { //search first char of str
        int s1 = 0;
        for(int jj = ii+1; jj < ii + str.length(); ++jj){
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >=0) return ii;  //found.
      }
    }
    return -1;  //not found;
  }
  
  
  /**Checks whether the given CharSequence contains the other given CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence of str in sq which is >= fromIndex. 
   */
  public static int indexOf(CharSequence sq, int fromIndex, int to, CharSequence str){
    int zsq = sq.length();
    int max = (to >= zsq ? zsq : to) - str.length()+1 ;
    int ii = fromIndex-1;  //pre-increment
    if (fromIndex < 0) {
        ii = -1;
    } else if (fromIndex >= max) {
        return -1;
    }
    char ch = str.charAt(0);   //search first char of str
    while(++ii < max){
      if(sq.charAt(ii) == ch) { //search first char of str
        int s1 = 0;
        for(int jj = ii+1; jj < ii + str.length(); ++jj){
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >=0) return ii;  //found.
      }
    }
    return -1;  //not found;
  }
  
  
  /**Searches the first occurrence of the given CharSequence in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence where {@link #equals(CharSequence sq, int return , int MAX_VALUE, CharSequence str)} ==0. 
   */
  public static int indexOf(CharSequence sq, CharSequence str, int fromIndex){
    return indexOf(sq, fromIndex, Integer.MAX_VALUE, str);
  }
  
  
  /**Searches the first occurrence of the given CharSequence in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @return -1 if not found, else first occurrence where {@link #equals(CharSequence sq, int return , int MAX_VALUE, CharSequence str)} ==0. 
   */
  public static int indexOf(CharSequence sq, CharSequence str){
    return indexOf(sq, 0, Integer.MAX_VALUE, str);
  }
  
  
  /**Checks whether the given CharSequence contains the given String.
   * It is the adequate functionality like {@link java.lang.String#lastIndexOf(String, int)}. 
   * @param sq Any sequence where to search in
   * @param from range, it ends searching on from
   * @param to if > sq.length() uses sq.length(), it starts searching on to-str.lsength()
   * @param str comparison String, check whether contained fully.
   * @return -1 if not found, elsewhere the position inside sq >=fromIndex and <= to - str.length()
   */
  public static int lastIndexOf(CharSequence sq, int fromIndex, int to, String str){
    int zsq = sq.length();
    int max = (to >= zsq ? zsq : to) - str.length()+1 ;
    if (fromIndex >= max) {
      return -1;
    }
    char ch = str.charAt(0);
    while(--max >= fromIndex){
      if(sq.charAt(max) == ch) {
        int s1 = 0;
        for(int jj = max+1; jj < max + str.length(); ++jj){
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >0) return max;  //found.
      }
    }
    return -1;  //not found;
  }
  
  
  
  /**Checks whether the given CharSequence contains the other given CharSequence.
   * Note: The algorithm and source lines are the same like in {@link #lastIndexOfAnyChar(CharSequence, int, int, String)}.
   * The difference is by translating to C source.
   * @param sq Any sequence where to search in
   * @param from range, it ends searching on from
   * @param to if > sq.length() uses sq.length(), it starts searching on to-str.lsength()
   * @param str comparison sequence, check whether contained fully.
   * @return -1 if not found, elsewhere the position inside sq >=fromIndex and <= to - str.length()
   */
  public static int lastIndexOf(CharSequence sq, int fromIndex, int to, CharSequence str){
    int zsq = sq.length();
    int max = (to >= zsq ? zsq : to) - str.length()+1 ;
    if (fromIndex >= max) {
      return -1;
    }
    char ch = str.charAt(0);
    while(--max >= fromIndex){
      if(sq.charAt(max) == ch) {
        int s1 = 0;
        for(int jj = max+1; jj < max + str.length(); ++jj){
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >0) return max;  //found.
      }
    }
    return -1;  //not found;
  }
  
  
  
  
  
  /**Resolves the given String containing some transcription chars (usual backslash) 
   * to a string with the appropriate character codes.
   * In the result String all char-pairs beginning with the transciptionChar are replaced by
   * one char. If the String doesn't contain a transcriptChar, the method returns the input string
   * in a as soon as possible calculation time.
   * <ul>
   * <li>The transcript char following by n r t f b will converted to the known control character codes:
   * <li>\n newline 0x0a
   * <li>etc TODO
   * <li>\s will converted to a single space. It is useful in input situations
   *     where a space will have another effect.
   * <li>\a will converted to the code 0x02, known in this class {@link cStartOfText}.
   *     It is useful wether a String may be contain a code for start of text.
   * <li>\e will converted to the code 0x03, known in this class {@link cEndOfText}.
   * <li>\x0123 Convert from given hex code TODO
   * <li>\\ is the backslash itself.
   * <li>All other chars after the transcription char will be converted to the same char, 
   *     for example "\{" to "{". Don't use this feature for normal alphabetic chars
   *     because some extensions in a future may be conflict with them. But this feature
   *     may be useful if an input text uses the special characters in a special way.
   * </ul> 
   * @param src The input string
   * @param transcriptChar the transcript character, usual a '\\'
   * @return The output string with replaces backslash pairs. It is a non-referenced StringBuilder
   *   if the src contains transcription chars, it is src itself if src does not contain transcription chars.
   */
  public static CharSequence convertTransliteration(CharSequence src, char transcriptChar)
  { CharSequence sResult;
    int posSwitch = indexOf(src,0, src.length(), transcriptChar);
    if(posSwitch < 0)
    { sResult = src;
    }
    else
    { //escape character is found before end
      StringBuilder sbReturn = new StringBuilder(src);
      while(posSwitch >=0)
      { if(posSwitch < sbReturn.length()-1)
        { sbReturn.deleteCharAt(posSwitch);
          /*do not delete a \ as last character, because the next algorithm failed
           *in such case. The \ will kept. It is a possible input sequence of a user,
           *and it shouldn't be throw an IndexOutofBoundaryException!
           */  
        }
        char cNext = sbReturn.charAt(posSwitch);
        int iChangedChar;
        if( (iChangedChar = "snrtfb".indexOf(cNext)) >=0)
        { sbReturn.setCharAt(posSwitch, " \n\r\t\f\b".charAt(iChangedChar));
        }
        else if( cNext == 'a')
        { // \a means end of file, coded inside with 4 = EOT (end of transmission).
          sbReturn.setCharAt(posSwitch, SpecialCharStrings.cStartOfText);
        }
        else if( cNext == 'e')
        { // \e means end of file, coded inside with 4 = EOT (end of transmission).
          sbReturn.setCharAt(posSwitch, SpecialCharStrings.cEndOfText);
        }
        else
        { //the char after cEscape is valid and not changed!
        }
        posSwitch = sbReturn.toString().indexOf(transcriptChar, posSwitch+1);
      }
      sResult = sbReturn;
    }
    return sResult;
  }

  
  static String indentString = "\n                                                                                                    ";
  
  /**Returns a String with 2*indent spaces for indentation.
   * If indent is >50, return only 100 spaces. 
   * @param indent indentation
   * @return
   */
  public static String indent2(int indent){
    if(2*indent < indentString.length()-1){ return indentString.substring(1, 1 + 2*indent); }
    else return indentString.substring(1);
  }
  
  /**Returns a String with a newline \n character and 2*indent spaces for indentation.
   * If indent is >50, return only 100 spaces. 
   * @param indent indentation
   * @return
   */
  public static String nl_indent2(int indent){
    if(2*indent < indentString.length()-1){ return indentString.substring(0, 1 + 2*indent); }
    else return indentString;
  }
  
  
  
  
}
