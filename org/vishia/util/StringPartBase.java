package org.vishia.util;

/**This is an alternative to the {@link java.lang.String} which uses a shared reference to the char sequence.
 * This class is able to use if String processing is done in a closed thread. This class must not be used 
 * instead java.lang.String if the String would referenced persistently and used from more as one thread.
 * String with this class are not immutable.
 * @author Hartmut Schorrig
 *
 */
public class StringPartBase implements CharSequence, Comparable<CharSequence>
{
  /**Version, history and license.
   * <ul>
   * <li>2013-05-05 Hartmut created: Alternative implementation of StringPart using a CharSequence as buffer instead a String.
   *   It supports more flexibility.
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
  public final static int version = 20130504; 

  
  /** Flag to force setting the start position after the seeking string. See description on seek(String, int).
   */
   public static final int seekEnd = 1;

   /** Flag bit to force seeking backward. This value is contens impilicit in the mSeekBackFromStart or ~End,
       using to detect internal the backward mode.
   */
   private static final int mSeekBackward_ = 0x10;

   /** Flag bit to force seeking left from start (Backward). This value is contens impilicit in the seekBackFromStart
       using to detect internal the seekBackFromStart-mode.
   */
   private static final int mSeekToLeft_ = 0x40;

   /** Flag to force seeking backward from the start position. See description on seek(String).
   */
   public static final int seekToLeft = mSeekToLeft_ + mSeekBackward_;


   /** Flag to force seeking backward from the end position. See description on seek(String).
   */
   public static final int seekBack = 0x20 + mSeekBackward_;

   /** Flag to force seeking forward. See description on seek(String).
   */
   public static final int seekNormal = 0;



  
  final char[] c;
  
  final StringBuilder u;
  
  final String s;
  
  final CharSequence cq;
  
  /** The actual start position of the valid part.*/
  protected int begin;
  /** The actual exclusive end position of the valid part.*/
  protected int end;

  
  /** True if the last operation of lento__(), seek etc. has found anything. See {@link #found()}. */
  boolean bFound = true;
  
  
  
  
  /** The leftest possible start position. We speak about the 'maximal Part':
      The actual valid part can not exceed the borders startMin and endMax of the maximal part after any operation.
      The content of the associated string outside the maximal part is unconsidered. The atrributes startMin and endMax
      are not set by any operations except for the constructors and the set()-methods.
      <br/>Set to 0 if constructed from a string,
      determined by the actual start if constructed from a StringPart.
      <hr/><u>In the explanation of the methods the following notation is used as samples:</u><pre>
abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
  =====================     The === indicates the maximal part
    -----------             The --- indicates the valid part before operation
               ++++++++     The +++ indicates the valid part after operation
      </pre>
  */
  protected int begiMin;



  /** The rightest possible exclusive end position. See explanation on startMin.
      <br/>Set to content.length() if constructed from a string,
      determined by the actual end if constructed from a StringPart*/
  protected int endMax;


  public StringPartBase(CharSequence src, int start){
    this(src, start, src.length());
  }
  
  public StringPartBase(CharSequence src, int start, int end){
    cq = src;
    this.begiMin = this.begin = start;
    this.endMax = this.end = end;
    if(src instanceof StringBuilder){
      u = (StringBuilder)src;
      s = null;
      c = null;
    } else {
      c = new char[src.length()];
      s = null;
      u = null;
    }
  }
  
  
  @Override
  public char charAt(int index){ return cq.charAt(begin + index); }

  @Override
  public int length(){ return end - begin; }

  @Override
  public CharSequence subSequence(int start, int end)
  { //if(this.begin+end > this.end ) throw new IndexOutOfBoundsException();
    return new StringPartBase(this.cq, start, end);
  }

  
  
  
  
  /** Sets the start of the part to the exclusively end, set the end to the end of the content.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
         -----              The valid part before
              +++++         The valid part after.
      </pre>
      @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPartBase fromEnd()
  {
    begin = end;
    end = endMax;
    return this;
  }


  /** Sets the end position of the part of string to exclusively the char cc.
  If the char cc is not found, the end position is set to start position, so the part of string is empty.
  It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
  That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
  of the last part.
  <hr/><u>example:</u><pre>
  abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part of src
       -----              The valid part of src before calling the method
       +                  after calling lento('w') the end is set to start
                          position, the length() is 0, because the 'w' is outside.
       ++++++++++         calling set0end() is possible and produce this result.
    </pre>
    @java2c=return-this.
    @param cc char to determine the exclusively end char.
    @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
      Sets {@link #bFound} to false if the end char is not found.
  */
  public StringPartBase lento(char cc)
  { end = begin-1;
    while(++end < endMax){
      if(cq.charAt(end) == cc) { bFound = true; return this; }
    }
    end = begin;  //not found
    bFound = false;
    return this;
  }

  
  
  
  /**Sets the length to the end of the maximal part if the length is 0. This method could be called at example
  if a end char is not detected and for that reason the part is valid to the end.
   * @java2c=return-this.
   @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   */
  public StringPartBase len0end()
  { if(end <= begin) end = endMax;
    return this;
  }
  
  
  

  
  /** Displaces the start of the part for some chars to left or to right.
  If the seek operation would exceeds the maximal part borders, a StringIndexOutOfBoundsException is thrown.

  <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
=================         The maximal part
     -----              The valid part before
   +++++++              The valid part after calling seek(-2).
       +++              The valid part after calling seek(2).
         +              The valid part after calling seek(5).
                        The start is set to end, the lenght() is 0.
++++++++++++              The valid part after calling seek(-5).
  </pre>
   *  @java2c=return-this.
  @param nr of positions to displace. Negative: Displace to left.
  @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   */
  public StringPartBase seek(int nr)
  { //startLast = start;
    begin += nr;
    if(begin > end)
      /**@java2c=StringBuilderInThreadCxt.*/ 
      throwIndexOutOfBoundsException("seek=" + nr + " start=" + (begin-nr) + " end=" + end);
    else if(begin < begiMin) 
      /**@java2c=StringBuilderInThreadCxt.*/
      throwIndexOutOfBoundsException("seek=" + nr + " start=" + (begin-nr) + " start-min=" + begiMin);
    bFound = true;
    return this;
  }


  /** Returns true, if the last called seek__(), lento__() or skipWhitespaceAndComment()
   * operation founds the requested condition. This methods posits the current Part in a appropriate manner
   * if the seek or lento-conditions were not prosperous. In this kinds this method returns false.
   * @return true if the last seek__(), lento__() or skipWhitespaceAndComment()
   * operation matches the condition.
   */
  public boolean found()
  { return bFound;
  }
  
  


  /** Central mehtod to invoke excpetion, usefull to set a breakpoint in debug
   * or to add some standard informations.
   * @param sMsg
   * @throws IndexOutOfBoundsException
   */
  private void throwIndexOutOfBoundsException(String sMsg)
  throws IndexOutOfBoundsException
  { throw new IndexOutOfBoundsException(sMsg);
  }
  
  

  @Override public int compareTo(CharSequence str2)
  { return StringFunctions.compare(this, 0, str2, 0, Integer.MAX_VALUE);
  }
  


  
  @Override public String toString(){ 
    char[] chars = new char[this.end - this.begin];  //Copy the content, but it is temporary only. Neccesary for String(char[])
    int ichars = -1;
    for(int ii=this.begin; ii<this.end; ++ii){
      chars[++ichars] = cq.charAt(ii);
    }
    return new String(chars);    //Copy the content twice, 
  }

  
}
