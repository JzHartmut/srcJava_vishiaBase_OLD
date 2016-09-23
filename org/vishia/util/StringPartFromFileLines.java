/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig www.vishia.org/Java
 *
 ****************************************************************************/
package org.vishia.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
//import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import com.sun.swing.internal.plaf.basic.resources.basic_zh_HK;

public class StringPartFromFileLines extends StringPartScan
{
  
  /**Version, history and license.
   * list of changes:
   * <ul>
   * <li>2015-10-24 Hartmut new: {@link StringPartFromFileLines#StringPartFromFileLines(InputStream, String, int, String, Charset)}
   *   to use with <code>ClassLoader.getSystemClassLoader().getResourceAsStream("path");   
   * <li>2015-06-07 Hartmut chg: {@link #getLineAndColumn(int[])}: column counts from 1 on leftest position.
   * <li>2014-05-22 Hartmut new: {@link #setInputfile(String)} invoked with input file 
   * <li>2014-04-22 Hartmut chg: improved line numbers 
   * <li>2012-12-22 Hartmut chg: close() the file in constructor if the whole file was read.
   * <li>2010-02-11 Hartmut new: The ctor StringPartFromFileLines(File fromFile) is added, 
   *   it is compatible to StringPartFromFile now.
   * <li>2009-05-31 Hartmut detecting and ignoring a BOM (byte order mark) as first char.
   * <li>2009-04-09 Hartmut encoding detection korrig.
   * <li>2006-05-00 Hartmut creation
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final String version = "2015-10-24";
  
  final StringBuilder buffer;
  //char[] fileBuffer = new char[1024];
  //int nCharsFileBuffer = 0;
  
  /** A readed line from file.*/
  String sLine = null;
  
  /** Nr of chars in line without trailing spaces.*/
  int nLine = 0;
  
  /**Current line. */
  int nLineCt = 0;
  
  boolean bEof;
  
  /** The reader maybe with correct charset.
   * 
   */
  BufferedReader readIn;

  IntegerBlockArray linePositions = new IntegerBlockArray(1000);
  
  int maxIxLinePosition;
  
  
  String sNewline = "\n";
  
  
  /**Fills a StringPart from a File. If the file is less than the maxBuffer size,
   * the whole file is inputed into the StringPart, otherwise the StringPart is 
   * reloaded if the first area is proceed. This constructor ivokes {@link #StringPartFromFileLines(File, int, String, Charset)}
   * with default arguments for charset and buffer size. 
   * 
   * @param fromFile The file to read<br>
   *        
   * @throws FileNotFoundException If the file is not found
   * @throws IOException If any other exception is thrown
   */
  public StringPartFromFileLines(File fromFile)
  throws FileNotFoundException, IOException, IllegalCharsetNameException, UnsupportedCharsetException
  {
    this(fromFile, 0, null, null);
  }
  
  

    /**fills a StringPart from a File. If the file is less than the maxBuffer size,
   * the whole file is inputted into the StringPart, otherwise the StringPart is 
   * reloaded if the first area is proceed. This constructor ivokes {@link #StringPartFromFileLines(InputStream, String, int, String, Charset)}
   * with the opened file. 
   * 
   * @param fromFile The file to read<br>
   * 
   * @param maxBuffer The maximum of length of the associated StringBuffer.<br>
   * 
   * @param sEncodingDetect If not null, this string is searched in the first line,
   *        readed in US-ASCII or UTF-16-Format. If this string is found, the followed
   *        string in quotion marks or as identifier with addition '-' char is readed
   *        and used as charset name. If the charset name is failed, a CharsetException is thrown.
   *        It means, a failed content of file may cause a charset exception.<br>
   *        
   * @param charset If not null, this charset is used as default, if no other charset is found in the files first line,
   *        see param sEncodingDetect. If null and not charset is found in file, the systems default charset is used.<br>
   *        
   * @throws FileNotFoundException If the file is not found
   * @throws IOException If any other exception is thrown
   */
  public StringPartFromFileLines(File fromFile, int maxBuffer, String sEncodingDetect, Charset charset)
  throws FileNotFoundException, IOException, IllegalCharsetNameException, UnsupportedCharsetException
  {
    this( new FileInputStream(fromFile)
        , FileSystem.normalizePath(fromFile).toString()
        , maxBuffer <= 0 || fromFile.length() < maxBuffer -10 ? (int)fromFile.length() : maxBuffer
        , sEncodingDetect, charset);    
  }
  
  
  /**Fills a StringPart from a opened Stream. It can be used for example with 
   * <code>ClassLoader.getSystemClassLoader().getResourceAsStream("path"); </code>.
   * It is the core method called in the other constructors using a File input.
   * 
   * @param input Any input stream, maybe a ClassLoader getRessourceAsStream<br>
   * 
   * @param sInputPath Hint for error messages from which input is it.
   * 
   * @param maxBuffer The maximum of length of the associated StringBuffer.<br>
   * 
   * @param sEncodingDetect If not null, this string is searched in the first 2 lines,
   *        read in US-ASCII or UTF-16-Format. If this string is found, the followed
   *        string in quotion marks or as identifier with addition '-' char is read
   *        and used as charset name. If the charset name is failed, a CharsetException is thrown.
   *        It means, a failed content of file may cause a charset exception.<br>
   *        
   * @param charset If not null, this charset is used as default, if no other charset is found in the files first line,
   *        see param sEncodingDetect. If null and not charset is found in file, the systems default charset is used.<br>
   *        
   * @TODO read char per char for sEncodingDetect, don't use mark and reset()!
   */
  public StringPartFromFileLines(InputStream input, String sInputPath, int sizeBuffer, String sEncodingDetect, Charset charset)
  throws IOException, IllegalCharsetNameException, UnsupportedCharsetException
  { super();
    setInputfile(sInputPath);
    bEof = false;
    buffer = new StringBuilder(sizeBuffer);  //to large file
    linePositions.set(++maxIxLinePosition, 0);  //start entry: After position 0 is line 1  
    boolean bom = false;;
    if(/*input.markSupported() && */sEncodingDetect !=null){  //TODO read char per char, don't use mark!
      //input.mark(260);
      if(sEncodingDetect != null)
      { //test the first line to detect a charset, maybe the charset exceptions.
        byte[] inBuffer = new byte[256];
        int nrofFirstChars = input.read(inBuffer);
        //TODO check BOM
        int startPos = 0;
        if(inBuffer[0] == -1 && inBuffer[1] == -2) {  //0xfffe
          bom = true;
          startPos = 2;
        } else if(inBuffer[0] == -2 && inBuffer[1] == -1) { //0xfeff
          bom = true;
          startPos = 2;
        } 
        //the sLine is used as first content for readnextContentFromFile() too, the start text.
        sLine = new String(inBuffer, startPos, nrofFirstChars);  
        int posNewline = sLine.indexOf('\n'); 
        //@chg:JcHartmut-2010-0912: test 2 lines instead of the first only, because in a bash-shell script it can't be the first line!
        if(posNewline >= 0 && posNewline < nrofFirstChars){    //= nrofFirstBytes, then an IndexOutOfBoundsException is thrown because
          posNewline = sLine.indexOf('\n', posNewline +1); //from the second line. 
        }
        if(posNewline < 0) posNewline = nrofFirstChars;
        StringPartScan spFirstLine = new StringPartScan(sLine.substring(0, posNewline));
        spFirstLine.setIgnoreWhitespaces(true);
        /**Check whether the encoding keyword is found: */
        if(spFirstLine.seek(sEncodingDetect, StringPartScan.seekEnd).found()
          && spFirstLine.scan("=").scanOk() 
          )
        { String sCharset;
          spFirstLine.seekNoWhitespace();
          if(spFirstLine.getCurrentChar() == '\"')
          { sCharset = spFirstLine.seek(1).lentoQuotionEnd('\"', 100).getCurrentPart().toString();
            if(sCharset.length()>0) sCharset = sCharset.substring(0, sCharset.length()-1);
          }
          else
          { sCharset = spFirstLine.lentoIdentifier(null, "-").getCurrentPart().toString();
          }
          if(sCharset.length() > 0)
          { //the charset is defined in the first line:
            charset = Charset.forName(sCharset);  //replace the current charset
          }
          //re-read the string with the correct charset
          sLine = new String(inBuffer, startPos, nrofFirstChars, charset);
        }
        nLine = sLine.length();
      }
    }
    final Reader readIn1;
    if(charset != null)
    { readIn1 = new InputStreamReader(input, charset);
    }
    else
    { readIn1 = new InputStreamReader(input); //new FileReader(fromFile));
    }
    readIn = new BufferedReader(readIn1);
    boolean notAllContent = readnextContentFromFile(0);   //read the start content.
    assign(buffer);
    if(bEof){
      readIn.close();
      readIn = null;
    }
  }


  
  /**Returns the line and column of the current position.
   * The line comes from an array which stores all start positions of the line, filled if the line is used.
   * The conversion between the position (used internally) and the line (only for user interface)
   * is done by binarySearch in this array. The array will be increased by demand. It is a {@link IntegerBlockArray}
   * which uses blocks of a constant size.
   * @param column The leftest position in a line is 1, like usual in editors.
   * @return line, 1 is the first line.
   * @see org.vishia.util.StringPart#getLineAndColumn(int[])
   */
  @Override public int getLineAndColumn(int[] column){ 
    int line = linePositions.binarySearch(this.begin, maxIxLinePosition); 
    if(line <0){ 
      //usual negative if not found exactly.
      //The binarySearch returns the 'insertion point', it is the next line start position.
      //But the line is the previous one.
      line = -line -2; 
    } 
    if(column !=null){
      int posLineStart = linePositions.get(line);
      column[0] = this.begin - posLineStart +1; 
    }
    return line;
  }

  
  
  public boolean readnextContentFromFile(int size)
  throws IOException
  { boolean bBufferFull = false;
    int zBuffer = buffer.length();  //length before add the line. Start with 0
    int nRestBytes = buffer.capacity() - zBuffer;
    //check, shift only the buffer if necessary, to save calculation time. Prevent unnecessary shift.
    if(nRestBytes < size && super.begin >0) {
      int sh = super.begin;
      //shift the content from begin to the start of the buffer.
      buffer.delete(0, sh);  //this shifts the content.
      super.begin = 0;
      super.begiMin = 0;
      super.beginLast = 0;
      super.beginScan = 0;
      super.end -=sh;
      super.endLast -=sh;
      super.endMax -=sh;
    }
    boolean bFirstsLinewithhalfnl = false;
    while(!bEof && !bBufferFull)
    { if(nRestBytes >= nLine)
      { if(sLine != null) //only null on start
        { //stores position in Buffer to the line number. Pre-increment, maxIxLinePosition is the line number
          if(nLine > 0){ buffer.append(sLine.substring(0, nLine)); }
          int posNl =0;
          boolean appendNlAftersLine = true;
          while( (posNl = StringFunctions.indexOfAnyChar(sLine, posNl, Integer.MAX_VALUE, "\r\n")) >=0) {
            appendNlAftersLine = false;  //only in the first content newline characters can occur. Then the end is not a simple line.
            if(sLine.length() > posNl +2) {
              if( sLine.charAt(posNl) == '\r' && sLine.charAt(posNl+1) == '\n') {
                sNewline = "\r\n";
                posNl +=2;
              } else if( sLine.charAt(posNl) == '\n' && sLine.charAt(posNl+1) == '\r' ) {
                sNewline = "\r\n";
                posNl +=2;
              } else {
                sNewline = "" + sLine.charAt(posNl);
                posNl +=1;
              }
              linePositions.set(++maxIxLinePosition, posNl);
            } else {
              posNl +=1;
              //no character after the detected \n
              bFirstsLinewithhalfnl = sNewline.length() >1; 
              if(bFirstsLinewithhalfnl) {
                buffer.append(sNewline.charAt(1));  //append only after a readLine().
                linePositions.set(++maxIxLinePosition, posNl+1);
              } else {
                linePositions.set(++maxIxLinePosition, posNl);
              }
            }
            //the only one problem yet is: \r as last character in sLine, \n is the first character of the next line.  
          }
          if(appendNlAftersLine) {
            buffer.append('\n');  //append only after a readLine().
            linePositions.set(++maxIxLinePosition, buffer.length());  
          }
        }
        //read the next lines and try to fill in
        sLine = readIn.readLine();
        if(sLine == null)
        { bEof = true;
          nLine = 0;
        }
        else if(bFirstsLinewithhalfnl && sLine.length() ==0) {
          //it is the empty line between \r and \n
          bFirstsLinewithhalfnl = false;
          sLine = null; //not used.
        }
        else
        { int idxSpaceTest = sLine.length()-1;
          while(idxSpaceTest >= 0 && sLine.charAt(idxSpaceTest) == ' '){ idxSpaceTest -=1; }
          nLine = idxSpaceTest +1;
        }
      }
      else
      { //keep the next content in sLine, fill in later.
        bBufferFull = true;
      }
    }
    return !bBufferFull;
  }


  @Override
  public void close(){
    if(readIn != null){
      try{ readIn.close(); } catch(IOException exc){}
      readIn = null;
    }
    nLineCt = 0;
    super.close();
  }
  
}
