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
//import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

public class StringPartFromFileLines extends StringPartScan
{
  
  /**Version, history and license.
   * list of changes:
   * <ul>
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
  public static final int version = 20121222;
  
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
  final BufferedReader readIn;

  IntegerBlockArray linePositions = new IntegerBlockArray(1000);
  
  int maxIxLinePosition;
  
  
  /**fills a StringPart from a File. If the file is less than the maxBuffer size,
   * the whole file is inputted into the StringPart, otherwise the StringPart is 
   * reloaded if the first area is proceed. 
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
  public StringPartFromFileLines(File fromFile)
  throws FileNotFoundException, IOException, IllegalCharsetNameException, UnsupportedCharsetException
  {
    this(fromFile, 0, null, null);
  }
  
  
  
  /**fills a StringPart from a File. If the file is less than the maxBuffer size,
   * the whole file is inputted into the StringPart, otherwise the StringPart is 
   * reloaded if the first area is proceed. 
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
  { super();
    setInputfile(FileSystem.normalizePath(fromFile).toString());
    bEof = false;
    long nMaxBytes = fromFile.length();
    if(maxBuffer <= 0 || nMaxBytes < (maxBuffer -10))
    { buffer = new StringBuilder((int)(nMaxBytes));  //buffer size appropriate to the file size.
    }
    else buffer = new StringBuilder(maxBuffer);  //to large file
    linePositions.set(++maxIxLinePosition, 0);  //start entry: After position 0 is line 1  

    
    if(sEncodingDetect != null)
    { //test the first line to detect a charset, maybe the charset exceptions.
      FileInputStream input = new FileInputStream(fromFile);
      byte[] inBuffer = new byte[256];
      int nrofFirstChars = input.read(inBuffer);
      input.close();
      String sFirstLine = new String(inBuffer, 0, nrofFirstChars);
      int posNewline = sFirstLine.indexOf('\n'); 
      //@chg:JcHartmut-2010-0912: test 2 lines instead of the first only, because in a bash-shell script it can't be the first line!
      if(posNewline >= 0 && posNewline < nrofFirstChars){    //= nrofFirstBytes, then an IndexOutOfBoundsException is thrown because
        posNewline = sFirstLine.indexOf('\n', posNewline +1); //from the second line. 
      }
      if(posNewline < 0) posNewline = nrofFirstChars;
      StringPartScan spFirstLine = new StringPartScan(sFirstLine.substring(0, posNewline));
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
      }
    }
    
    if(charset != null)
    { readIn = new BufferedReader(new InputStreamReader(new FileInputStream(fromFile), charset));
    }
    else
    { readIn = new BufferedReader(new FileReader(fromFile));
    }
    boolean notAllContent = readnextContentFromFile();
    if(buffer.length() >0 && buffer.charAt(0) == '\ufeff')
    { /**ignore a BOM Byte Order Mark.*/
      assign(buffer.substring(1));
    }
    else
    { assign(buffer);
    }
    if(!notAllContent){
      readIn.close();
    }
  }

  @Override public int XXXgetLineCt(){ 
    int line = linePositions.binarySearch(this.begin, maxIxLinePosition); 
    if(line <0){ line = -line; }
    return line;
  }

  
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
      column[0] = this.begin - posLineStart; 
    }
    return line;
  }

  
  
  boolean readnextContentFromFile()
  throws IOException
  { {
      boolean bBufferFull = false;
      while(!bEof && !bBufferFull)
      { int zBuffer = buffer.length();  //length before add the line. Start with 0
        int nRestBytes = buffer.capacity() - zBuffer;
        if(nRestBytes >= nLine)
        { if(sLine != null) //only null on start
          { //stores position in Buffer to the line number. Pre-increment, maxIxLinePosition is the line number
            if(nLine > 0){ buffer.append(sLine.substring(0, nLine)); }
            buffer.append('\n');
            linePositions.set(++maxIxLinePosition, buffer.length());  
          }
          //read the next lines and try to fill in
          sLine = readIn.readLine();
          if(sLine == null)
          { bEof = true;
            nLine = 0;
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
    }
    return false;
  }


  @Override
  public void close(){
    if(readIn != null){
      try{ readIn.close(); } catch(IOException exc){}
      //readIn = null;
    }
    nLineCt = 0;
    super.close();
  }
  
}
