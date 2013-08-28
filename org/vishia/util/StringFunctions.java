
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
  
  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the indentier starts.
   * @param endq max number of chars to check
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  public static int posAfterIdentifier(CharSequence src, int start, int endMax){ return posAfterIdentifier(src, start, endMax, null, null); }

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
  

  
  @Java4C.define public static int parseIntRadix(final String srcP, final int pos, final int sizeP, final int radix, final int[] parsedChars)
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
    ret = parseIntRadix(src, pos, size, 10, zParsed);
    parsedChars += zParsed[0];  //maybe 0 if .123 is written
    int ixsrc = pos + zParsed[0]; size -= zParsed[0];
    if(src.charAt(ixsrc)=='.'){
      float fracPart = parseIntRadix(src, ixsrc +1, size-1, 10, zParsed);
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

  
  /**Copies the content of a String in the given byte buffer with the requested encoding.
   * If this method is used in a C/C++ environment, it is programmed in a special maybe more simple way
   * because not all encodings are supported. Usual a String is only a byte array itself, it is copied.  
   * @param src The src String
   * @param buffer The desination buffer with given length. The last byte will be set to 0.
   * @param encoding The encoding. If null, then use the UTF8-encoding.
   * @return number of bytes in buffer.
   */
  @Java4C.exclude public static int copyToBuffer(String src, byte[] buffer, Charset encoding){
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
  @Java4C.exclude public static int copyToBuffer(String src, char[] buffer){
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
  @Java4C.exclude public static String z_StringJc(char[] buffer){
    int ix=-1;
    while(++ix < buffer.length && buffer[ix] !='0');
    return new String(buffer, 0, ix);
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
  
  
  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}.
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
  public static boolean equals(CharSequence s1, CharSequence s2){
    int zz = s1.length();
    if( zz != s2.length()) return false;
    else {
      for(int ii = 0; ii<zz; ++ii){
        if(s1.charAt(ii) != s2.charAt(ii)) return false;
      }
      return true;
    }
  }
  

  
  /**Checks whether the given CharSequence starts with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   */
  public static boolean startsWith(CharSequence sq, CharSequence start){
    return compare(sq, 0, start, 0, start.length()) == 0;
  }
  

  /**Checks whether the given CharSequence ends with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
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
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, char ch, int fromIndex){
    int max = sq.length();
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
  

  /**Searches the first occurrence of the given CharSequence in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int lastIndexOf(CharSequence sq, char ch){
    int ii = sq.length();
    while(--ii >=0){
      if(sq.charAt(ii) == ch) {
        return ii;
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
  public static int indexOf(CharSequence sq, CharSequence str, int fromIndex){
    int max = sq.length() - str.length()+1;
    int ii = fromIndex-1;  //pre-increment
    if (fromIndex < 0) {
        ii = -1;
    } else if (fromIndex >= max) {
        return -1;
    }
    char ch = str.charAt(0);
    while(++ii < max){
      if(sq.charAt(ii) == ch) {
        int s1 = 0;
        for(int jj = ii+1; jj < ii + str.length(); ++jj){
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >0) return ii;  //found.
      }
    }
    return -1;  //not found;
  }
  
}
