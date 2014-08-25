package org.vishia.byteData;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.vishia.util.Debugutil;
import org.vishia.util.Java4C;
import org.vishia.util.StringFormatter;

/**This class is the base class for ByteDataAccess. It works without dynamic methods proper for C usage.
 * All variables are package private because they should be changed only with methods of this class.
 * Only the derived Class {@link ByteDataAccess} uses the variables direct.
 * @java2c = noObject.
 * @author Hartmut Schorrig
 *
 */
public abstract class ByteDataAccessBase
{
  
  /**The version. 
   * <ul>
   * <li>2014-08-23 Hartmut chg: Member idxFirstChild removed, the information is contained in {@link #sizeHead}. 
   * <li>2014-08-23 Hartmut new: this class ByteDataAccessBase works without virtual methods (overrideable), it is proper for usage in C-language
   *   and it is not necessary. The {@link #sizeHead} is set by construction. Only for the compatible now deprecated {@link ByteDataAccess} the sizeHead 
   *   is able to set via the package-private {@link #setSizeHead(int)}.  
   * <li>2014-01-12 Hartmut new: Java4C.inline for C-compilation. 
   * <li>2013-12-08 Hartmut new: {@link #ByteDataAccess(int, int)} as super constructor with given head and data size.
   *   {@link #addChild(ByteDataAccess)} accepts an initialized not used child. Uses {@link #kInitializedWithLength}.
   *   That is the possibility to work without dynamic linked methods {@link #specifyLengthElement()} etc. for proper work
   *   especially for C usage. The overridden methods {@link #specifyEmptyDefaultData()}, {@link #notifyAddChild()} etc.
   *   are proper to use in C too with the concept of the dynamic linked methods, but there does not be necessary.
   *   It is an optimizing for C. Maybe in future the {@link #specifyLengthElement()} and {@link #specifyLengthElementHead()}
   *   may be depreciated because the new variant of initialized children is better to use, more simple for usage. 
   *   But it should be compatible with older versions. 
   * <li>2013-12-08 Hartmut chg: {@link #reset(int, int)} is protected now and has a second parameter. necessary public? 
   * <li>2012-12-15 Hartmut chg: Some changes are done which cleans up this class. If any problem occurs, the {@link ByteDataAccessOld}
   *   can be used. It is compatible with the last version before this changes.
   *   <ul>
   *   <li>The currentChild is not necessary. The only reason to have that association is: changes of size in parent
   *     force changes in the current child. But it is contrary: The changes should be done in the child and should affect
   *     the indices in all parents which should be known all in the child. If a child is added and it is never used furthermore
   *     then an unnecessary dangling reference exists. If the child instance is reused and detach is not called, mistakes are happen.
   *   <li>the next() and rewind() are removed. It is an unused old concept.  
   *   </ul>
   * <li>2012-04-07 Hartmut new: {@link #reset(int)}, some comments.
   * <li>2012-03-00 Hartmut Note: compare it with java.nio.ByteBuffer. But a ByteBuffer is abstract.
   * <li>2010-12-20: Hartmut chg: remove the toString-method using StringFormatter, because it is too complex for C-usage.
   *   The toString was only able to use for debugging.
   * <li>2010-02-02: Hartmut new:  getChildFloat(), getChildDouble().
   * <li>2010-01-16: Hartmut chg:  setBigEndian in now public. It should be better because the same user data may be interpreted in both versions depending on a parameter.
   * <li>2005..2009: Hartmut: some changes
   * <li>2005 Hartmut created
   * </ul>
   * 
   */
  public static final String sVersion = "2014-08-26";
  
  /**Number of Memory locations (usual bytes) for the head of this instance's Type.  
   * Set on construction.
   */
  private int sizeHead;
  
  /** The array containing the binary data.*/
  protected byte[] data;

  /**Index of the beginning of the actual element in data*/
  protected int idxBegin;

  /** Index of the end of the actual element in data. If {@link #bExpand} is set, this idxEnd and the idxEnd of all parents are increased
   * if an child was added. If bExpand==false then this value is set via the {@link #addChild(ByteDataAccessBase, int)} or {@link #addChildAt(int, ByteDataAccessBase, int)}.*/
  protected int idxEnd;

  /** Index within the data at position of the current child element.
   * If no current child is known, this index is -1. */
  protected int idxCurrentChild;

  /**Index of the currents child end.
   * If no current child is known this index is equal idxBegin + sizeHead, it is the position after the head. 
   * If the length of the current child is not known, this index is <= idxCurrentChild.
   */
  protected int idxCurrentChildEnd;

  /**True if the {@link #idxEnd} should be increment on adding children. */
  protected boolean bExpand;

  /** Flag is set or get data in big endian or little endian (if false)*/
  protected boolean bBigEndian;

  /** The parent element, necessary only for add() and expand().
   */
  protected ByteDataAccessBase parent;



  /**The charset to build Strings.*/
  protected String charset;   //NOTE: String(..., Charset) is only support from Java 6
  
  /**Use especially for test, only used in toString(). */ 
  @Java4C.exclude
  protected StringFormatter toStringformatter = null;


  /**Any instance of ByteDataAccessBase is associated to a determined derived instance which has defined head size.
   * The sizeHead can be given with -1 only for derived instances of {@link ByteDataAccess}.
   * That class defines a method {@link ByteDataAccess#specifyLengthElementHead()} to get the head's size.
   * @param sizeHead The size of head data, it is the number of bytes.
   */
  protected ByteDataAccessBase(int sizeHead){
    this.sizeHead = sizeHead;
  }
  
  
  /** Constructs a new empty instance. Use assign() to work with it. 
   * @param sizeHead number of bytes for the head. See {@link #assignData(byte[], int, int, int)}.
   *   <ul>
   *   <li>If negative, especially -1, the overridden method {@link #specifyLengthElementHead()} is called
   *     to get the length of the head. It depends on the definition of the derived class.
   *   <li>If 0 or >0, it is the length. Then the overridden method is not called, especially for usage in C.
   *   </ul>               
   * @param sizeData number of significant bytes in data for this child.
   *   <ul>
   *   <li>If sizeChild is to large in respect to data.length, an exception may be thrown on access.
   *   <li>If the sizeChild is < 0 (especially -1), it means, it is not known outside.
   *     Than the element is initialized with its known head length calling {@link #specifyLengthElement()}.
   *   <li>If the length is >0, it defines the size of this access. Between the head and this length
   *     some children can be added to access that data.
   *   </ul>  
   * */
  protected ByteDataAccessBase(int sizeHead, int sizeData){
    this.sizeHead = sizeHead;
    idxBegin = 0;
    idxEnd = sizeData;
    idxCurrentChild = -1;  //to mark start.
    idxCurrentChildEnd = -1;
    parent = null;
    //currentChild = null;
    //charset = Charset.forName("ISO-8859-1");  //NOTE: String(..., Charset) is only support from Java 6
    charset = "ISO-8859-1";
  }


  
  
  /**Sets the big or little endian mode. 
   *
   * @param val true if big endian, hi byte at lower adress, false if little endian.
   */
  @Java4C.inline
  public final void setBigEndian(boolean val)
  { bBigEndian = val;
  }



  
  
  /**This method is package private because it is only used for the methodes of ByteDataAccess (with virtual methods).
   * @param size The element {@link #sizeHead} is changed (should be final).
   */
  @Java4C.inline
  final void setSizeHead(int size){
    if(sizeHead < 0){
      sizeHead = size;
    } else if(sizeHead == size){
      //do nothing.      
    } else {
      assert(false);  //don't change the sizeHead.
    }
    
  }
  
  
  /**Resets the view to the buffer. The data in the buffer may be set newly. Either they are declared
   * as empty, then call <pre>
   * reset(0);  //all data are invalid.
   * <pre>
   * In that case the head data are initialized calling {@link #specifyEmptyDefaultData()}
   * from the derived class.
   * <br><br>
   * Or the data are set newly with any designated content, then call <pre>
   * reset(length);
   * with the known length. Then the data can be evaluate by calling {@link #addChild(ByteDataAccess)}.
   * or by getting data from the head only if children should ot be used.
   * <br><br>
   * See {@link #assignEmpty(byte[])}, {@link #assignData(byte[], int)}. This routine
   * is called there after setting the data reference and the {@link #idxBegin}. In opposite to the
   * newly assignment of data, the {@link #data}-reference is not changed, the {@link #idxBegin}
   * is not changed and a {@link #parent} is not changed. It means, a reset can be invoked for any child
   * of data without changing the context.
   * 
   * @param lengthHead Number of bytes for the head of this element. It may equal lengthData.
   * @param lengthData Number of bytes for this element.
   *   
   */
  protected final void clear(int lengthData){
    assert(sizeHead >=0);
    bExpand = lengthData <= 0;  //expand if the data have no head.
    idxCurrentChild = -1;
    idxCurrentChildEnd = idxBegin + sizeHead;
    //NOTE: problem in last version? The idxBegin ... idxEnd should be the number of given data.
    //lengthData is inclusively head. Other variants are calling problems.
    idxEnd = bExpand ? idxBegin + sizeHead : idxBegin + lengthData;
    { //@Java4C.exclude
      if(idxEnd > data.length)
      { @Java4C.StringBuilderInThreadCxt String msg = "not enough data bytes, requested=" + idxEnd + ", buffer-length=" + data.length;
        throw new IllegalArgumentException(msg);
      }
    }
  }

  
  /** Returns the content of 1 to 8 bytes inside the actual element as a long number,
   * big- or little-endian depending on setBigEndian().
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   *
   * @param idxInChild The position of leading byte in the actual element, the data are taken from data[idxBegin+idx].
   * @param nrofBytesAndSign If positiv, than the method returns the unsigned interpretation of the bytes.
   *   If negative, than the return value is negative, if the last significant bit of the given number of bytes is set.
   *   The value represents the number of bytes to interprete as integer. It may be 1..8 respectively -1...-8.   
   * @return the long value in range adequate nrof bytes.
   * @since 2009-09-30: regards negative nrofBytesAndSign. Prior Versions: returns a signed value always.
   * */
  protected final long _getLong(final int idxInChild, final int nrofBytesAndSign)
  { long val = 0;
    int idxStep;
    int idx;
    final int nrofBytes;
    final boolean bSigned;
    if(nrofBytesAndSign >=0)
    { nrofBytes = nrofBytesAndSign;
      bSigned = false;
    }
    else{
      nrofBytes = - nrofBytesAndSign;
      bSigned = true;
    }
    if(bBigEndian)
    { idx = idxBegin + idxInChild;
      idxStep = 1;
    }
    else
    { idx = idxBegin + idxInChild + nrofBytes -1;
      idxStep = -1;
    }
    int nByteCnt = nrofBytes;
    do
    { val |= data[idx] & 0xff;
      if(--nByteCnt <= 0) break;  //TRICKY: break in mid of loop, no shift operation.
      val <<=8;
      idx += idxStep;
    }while(true);  //see break;
    if(bSigned){
      int posSign = (nrofBytes*8)-1;  //position of sign of the appropriate nrofBytes 
      long maskSign = 1L<<posSign;
      if( (val & maskSign) != 0)
      { long bitsSign = 0xffffffffffffffffL << (posSign);
        val |= bitsSign;  //supplement the rest bits of long with the sign value,it's negativ.   
      }
    }  
    return val;
  }

  
  
  /**sets the content of 1 to 8 bytes inside the actual element as a long number,
   * big- or little-endian depending on setBigEndian().
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   *
   * @param idx the position of leading byte in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The number of bytes of the value. 
   * @param val the long value in range adequate nrof bytes.
   * */
  protected final void _setLong(int idx, int nrofBytes, long val)
  { int idxStep;
    if(bBigEndian)
    { idx = idxBegin + idx + nrofBytes -1;
      idxStep = -1;
    }
    else
    { idx = idxBegin + idx;
      idxStep = 1;
    }
    do
    { data[idx] = (byte)(val);
      if(--nrofBytes <= 0) break;
      val >>=8;
      idx += idxStep;
    }while(true);  //see break;
  }

  
  


  

  
  /**Increments the idxEnd if a new child is added. It is called 
   * inside method addChild(child) and recursively to correct
   * in all parents.
   */
  final void expand(int idxCurrentChildEndNew)
  { if(bExpand) 
    { //do it only in expand mode
      idxEnd = idxCurrentChildEndNew;
    }
    assert(idxCurrentChildEndNew >= idxBegin + sizeHead);
    idxCurrentChildEnd = idxCurrentChildEndNew;
    if(parent != null)
    { parent.expand(idxCurrentChildEndNew);
    }
  }


  /**Assigns new data to this element at given index in data. 
   * This method is called on {@link #addChild(ByteDataAccess)}.
   * <br>
   * This method is also usefull to deassign a current data buffer, call <code>assign(null, 0);</code>.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * @param data The dataP. The length of data may be greater as the number of the significant bytes.
   * @param lengthHead number of bytes for the head. It have to be the necessary number of bytes for the current child type. 
   *   <ul>
   *   <li>If negative, especially -1, the overridden method {@link #specifyLengthElementHead()} is called
   *     to get the length of the head. It depends on the definition of the derived class.
   *   <li>If 0 or >0, it is the length. Then the overridden method is not called, especially for usage in C.
   *   </ul>               
   * @param lengthData absolute Number of significant bytes in data from idx=0.
   *   <ul>
   *   <li>If length is > data.length, an exception is thrown.
   *   <li>If the length is <0 (especially -1), it means, it is not known outside.
   *     Than the element is initialized with its known head length.
   *     The length mustn't not ==0, it is tested. Use -1 also if the head length is 0.
   *   <li>If the length is >0, it defines the size of this access. Between the head and this length
   *     some children can be added to access that data.
   *   </ul>  
   * @param index Start position in data 
   * @throws IllegalArgumentException 
   */
  public final void assign(byte[] dataP, int lengthData, int index) 
  throws IllegalArgumentException
  { assert (index >= 0 && this.sizeHead >=0);
    this.data = dataP;
    idxBegin = index; 
    parent = null;
    clear(lengthData);
  }
  
  @Java4C.inline
  public final void assign(byte[] dataP, int lengthData){ assign(dataP, lengthData, 0); } 
  
  @Java4C.inline
  public final void assign(byte[] dataP){ assign(dataP, dataP.length, 0); } 
  
  /**Initializes a top level, the data are considered as non initalized.
   * The length of the head should be a constant value, given from
   * method call {@link specifyLengthElementHead()}. The child Positions
   * are set to the end of head, no childs are presumed.
   * The head should be filled with data after that calling some methods like
   * {@link setInt32(int, int)}.<br>
   * The children should be added by calling {@link addChild(ByteDataAccess)}
   * and filled with data after that.
   *
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * Example to shown the principle:<pre>
   *   ...........the data undefined with defined length.........
   *   +++++                   Head, the length should be known.
   *        ####****#####****  Space for children,
   * </pre>
   * @param data The data. The reference should be initialized, it means
   *        the data have a defined maximum of length. But it is not tested here.
   * @throws IllegalArgumentException 
   */
  @Java4C.inline
  final public void assignClear(byte[] data) 
  { Arrays.fill(data, (byte)0);
    assign(data, -1, 0);
  }

  

  /**assigns the element to the given position of the parents data to present a child of the parent
   * with a defined length.
   * The difference to {@link addChild(ByteDataAccess)} is: The position is given here
   * directly, it should not be the current child but a free child.  
   * <br>
   * The data reference is copied, the idxBegin of this element
   * is set to the idxChild given as parameter.
   * All other indices are set calling {@link specifyLengthElementHead()}: idxChild
   * and {@link specifyLengthElement()}: idxEnd.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * @param parent The parent. It should reference data.
   * @param lengthChild Number of the bytes of the free child.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  @Java4C.inline
  final protected void assignAt(int idxChildInParent, int lengthChild, ByteDataAccess parent)
  throws IllegalArgumentException
  { this.bBigEndian = parent.bBigEndian;
    this.bExpand = parent.bExpand;
    assign(parent.data, parent.idxBegin + idxChildInParent + lengthChild, parent.idxBegin + idxChildInParent);
    setBigEndian(parent.bBigEndian);
  }


  /**assigns the element to the given position of the parents data to present a child of the parent.
   * The length of the child is limited to TODO the length of head - or not limited.
   * @param parent The parent. It should reference data.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  @Java4C.inline
  final public void assignAt(int idxChildInParent, ByteDataAccess parent)
  throws IllegalArgumentException
  { assignAt(idxChildInParent, sizeHead, parent);
  }


  

  

  /**Assigns this element to the same position in data, but it is another view. 
   * This method should be called inside a assignCasted() method if a inner head structure is known
   * and the conclusion of this structure is possible. At result, both ByteDataAccess instances reference the same data,
   * in different views.
   * @param src The known data access
   * @param offsetCastToInput typical 0 if single inherition is used.
   * @throws IllegalArgumentException if a length of the new type is specified but the byte[]-data are shorter. 
   *                         The length of byte[] is tested. 
   */
  @Java4C.inline
  final protected void assignCasted_i(ByteDataAccess src, int offsetCastToInput, int lengthDst)
  throws IllegalArgumentException
  { this.bBigEndian = src.bBigEndian;
    bExpand = src.bExpand;
    //assignData(src.data, src.idxBegin + lengthDst + offsetCastToInput, src.idxBegin + offsetCastToInput);
    assign(src.data, src.idxEnd, src.idxBegin + offsetCastToInput);
    //lengthDst is unsused, not necessary because lengthElementHead is knwon!
  }


  

  
  /** Returns the data buffer itself. The actual total length is getted with getLengthTotal().
   * @return The number of bytes of the data in the buffer.
   */
  @Java4C.retinline
  final public byte[] getData()
  { return data;
  }




  /** starts the calling loop of next().
   * The calling of next() after them supplies the first child element.
   *
   */
  @Java4C.inline
  public final void rewind()
  { idxCurrentChild = -1;
  }




  /**Returns the length of the head. This method returns the size of the head given on construction
   * or set with {@link #setSizeHead(int)} (package private). The size of the head is not changed normally for an existing instance.
   */ 
  @Java4C.retinline
  public final int getLengthHead(){ return sizeHead; }
  
  
  
  /** Returns the length of the existing actual element.
   * @return The number of bytes of the actual element in the buffer.
   *         It is (idxEnd - idxBegin).
   */
  @Java4C.retinline
  final public int getLength()
  { return idxEnd - idxBegin;
  }




  /** Returns the length of the data.
   * @return The number of bytes of data in the buffer.
   *         It is idxEnd.
   */
  @Java4C.retinline
  final public int getLengthTotal()
  { return idxEnd;
  }




  /**returns the number number of bytes there are max available from position of the current child. 
   * ,
   * the number of bytes to the end of buffer is returned.
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   */ 
  @Java4C.retinline
  final public int getMaxNrofBytes()
  { return data.length - idxBegin;
  }


  @Java4C.retinline final public boolean getBigEndian(){ return bBigEndian; }
  

  /**Sets the length of the element in this and all {@link #parent} of this. 
   * If the element is a child of any parent, it should be the current child of the parent. 
   * The {@link #idxEnd} and the {@link #idxCurrentChildEnd} of this and all its parents is set
   * with the (this.{@link #idxBegin}+length).
   * <br><br>
   * This routine is usefully if data are set in a child directly without sub-tree child structure
   * (without using {@link #addChild(ByteDataAccess)}).
   * It is if the element has data after the head with different length without an own children structure.
   * 
   * @param length The length of data of this current (last) child.
   */
  @Java4C.inline
  final public void setLengthElement(int length)
  { //if(!bExpand && )
    expand(idxBegin + length);
  }




  /**Sets all data of the head of this element to 0.
   * Note: If the element has not a head, this method does nothing.
   */
  @Java4C.inline
  public final void clearHead(){
    Arrays.fill(data, idxBegin, idxBegin + sizeHead, (byte)0);
  }




  /**Sets all data of this element to 0.
   * Note: The idxEnd should be set to the end of the element.
   * This method is proper to use for a simple element only.
   */
  @Java4C.inline
  public final void clearData(){
    Arrays.fill(data, idxBegin, idxEnd, (byte)0);
  }




  /**returns true if the given number of bytes is sufficing in the data from position of next child. 
   * 
   * @param nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   * @return true if it is okay, false if the nrofBytes are negative or to large.
   * @throws IllegalArgumentException see {@link getMaxNrofBytesForNextChild()} 
   */ 
  final public boolean sufficingBytesForNextChild(int nrofBytes) 
  throws IllegalArgumentException
  { int maxNrofBytesChild = getMaxNrofBytesForNextChild();
    return nrofBytes < 0 ? false : maxNrofBytesChild >= nrofBytes;
  }




  /**returns the number number of bytes there are max available from position of a next current child. 
   * ,
   * the number of bytes to the end of buffer is returned.
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   * @throws IllegalArgumentException if the length of the current child is not determined yet.
   *         Either the method specifyLengthElement() should be overwritten or the method 
   *         {@link setLengthElement(int)} for the child or {@link setLengthCurrentChildElement(int)}
   *         should be called to prevent this exception.  
   */ 
  @Java4C.retinline
  final public int getMaxNrofBytesForNextChild() throws IllegalArgumentException
  { if(idxCurrentChildEnd < idxCurrentChild)
      throw new IllegalArgumentException("length of current child is undefined."); 
    return idxEnd - idxCurrentChildEnd;
  }




  /**adds a child Element after the current child or as first child after head.
   * With the aid of the child Element the data can be read or write structured.
   * <br><br>
   * The child instance will be initialized newly. Any old usage of the child instance will be ignored.
   * The child is only a helper to manage indices in the parent, to get data and to manage its own indices
   * while further children were added to itself.
   * <br><br>
   * 
   *<br>
   * Some children can be added after a parent like following sample:
   * <pre>
   * ByteAccessDerivation child = new ByteAccessDerivation();  //empty and unassigned.
   * parent.addChild(child);        //The byte[] data of parent are assigned, index after current child index of parent.
   * child.addChild(grandchild);    //By adding a child to this child, also the parent's index is corrected.
   * </pre>
   *
   * @param child The child will be assigned with the data of this at index after the current child's end-index.
   * @throws IllegalArgumentException if the length of the old current child is not determined yet.
   *         Either the method specifyLengthElement() should be overwritten or the method 
   *         {@link setLengthElement(int)} for the child or {@link setLengthCurrentChildElement(int)}
   *         should be called to prevent this exception.  
   * @throws IllegalArgumentException if the length of the head of the new current child is to far for the data.
   *         It means, child.idxEnd > data.length. 
   */
  final public boolean addChild(ByteDataAccessBase child, int sizeChild) 
  throws IllegalArgumentException
  { child.bBigEndian = bBigEndian;
    child.bExpand = bExpand;
    setIdxtoNextCurrentChild();
    /**@java2c=dynamic-call.  */
    final int sizeChild1;
    if(sizeChild <= this.sizeHead){  //especially -1, the size is unknown.
      if(bExpand){ sizeChild1 = -1; }  //initialize with specifyLength()
      else { sizeChild1 = idxEnd - idxCurrentChild; }    //the child fills the parent.
    } else {
      sizeChild1 = sizeChild;   //given size is valid.
    }
    child.assign(data, sizeChild1, idxCurrentChild);
    child.parent = this;
    //this.currentChild = child;
    int idxEndNew = child.idxEnd > child.idxCurrentChildEnd ? child.idxEnd : child.idxCurrentChildEnd;
    expand(idxEndNew);  
    return bExpand;
  }

  
  
  @Java4C.retinline
  final public boolean addChild(ByteDataAccessBase child){ return addChild(child, -1); } 
  
  final public boolean addChildAt(int idxChild, ByteDataAccessBase child, int sizeChild) 
  throws IllegalArgumentException
  { child.data = data;
    int idxBegin = this.idxBegin + idxChild;
    child.idxBegin = idxBegin;
    child.idxEnd = idxBegin + sizeChild;
    child.idxCurrentChild = idxBegin + child.sizeHead;
    child.idxCurrentChildEnd = -1;
    child.bBigEndian = bBigEndian;
    child.bExpand = bExpand;
    child.parent = this;
    expand(child.idxEnd);  
    return bExpand;
  }

  
  @Java4C.retinline
  final public boolean addChildAt(int idxChild, ByteDataAccessBase child) 
  throws IllegalArgumentException
  { return addChildAt(idxChild, child, child.sizeHead);
  }  
  
  

  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final void addChildInteger(int nrofBytes, long value) 
  throws IllegalArgumentException
  { setIdxtoNextCurrentChild();
    if(data.length < idxCurrentChild + nrofBytes){
      @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (idxCurrentChild + nrofBytes);
      throw new IllegalArgumentException(msg);
    }
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setLong(idxCurrentChild - idxBegin, nrofBytes, value);  
    expand(idxCurrentChild + nrofBytes);
  }




  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final void addChildFloat(float value) 
  throws IllegalArgumentException
  { setIdxtoNextCurrentChild();
    if(data.length < idxCurrentChild + 4){
      @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (idxCurrentChild + 4);
      throw new IllegalArgumentException(msg);
    }
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    setFloat(idxCurrentChild - idxBegin, value);  
    expand(idxCurrentChild + 4);
  }




  /**Adds a child with String value.
   * 
   * @param value String to add
   * @param sEncoding If null then use the standard encoding of the system-environment.
   * @param preventCtrlChars true then values < 0x20 are not set. 
   *                         If the String value contain a control character with code < 0x20,
   *                         a '?' is written. This behavior guarantees, that byte-values < 0x20 
   *                         can use to detect no-String elements, see {@link getByteNextChild()}. 
   * @throws IllegalArgumentException
   * @throws UnsupportedEncodingException
   */
  public final void addChildString(String value, String sEncoding, boolean preventCtrlChars) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { setIdxtoNextCurrentChild();
    int nrofBytes = value.length();
    if(data.length < idxCurrentChild + nrofBytes){
      @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (idxCurrentChild + nrofBytes);
      throw new IllegalArgumentException(msg);
    }
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setString(idxCurrentChild - idxBegin, nrofBytes, value, sEncoding, preventCtrlChars);  
    expand(idxCurrentChild + nrofBytes);
  }




  /**Adds a child with String value.
   * 
   * @param valueCs String to add, @pjava2c=nonPersistent.
   * @param sEncoding String describes the encoding for translation from UTF16 to bytes. 
   * @throws IllegalArgumentException XXX
   * @throws UnsupportedEncodingException 
   */
  public final void addChildString(CharSequence valueCs, String sEncoding) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { setIdxtoNextCurrentChild();
    int nrofBytes = valueCs.length();
    if(data.length < idxCurrentChild + nrofBytes){
      @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (idxCurrentChild + nrofBytes);
      throw new IllegalArgumentException(msg);
    }
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    for(int ii=0; ii<nrofBytes; ++ii){
      byte charByte = (byte)(valueCs.charAt(ii));  //TODO encoding
      data[idxCurrentChild+ii] = charByte;
    }
    expand(idxCurrentChild + nrofBytes);
  }




  /**Adds a child with String value.
   * 
   * @param value String to add, @pjava2c=nonPersistent.
   * @throws IllegalArgumentException
   */
  @Java4C.inline
  public final void addChildString(CharSequence value) throws IllegalArgumentException
  { try{ addChildString(value, null); } 
    catch(UnsupportedEncodingException exc){ throw new RuntimeException(exc);} //it isn't able.
  }




  /**Adds a child for 1 integer value without a child instance, but returns the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final long getChildInteger(int nrofBytes) 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(Math.abs(nrofBytes)))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      long value = _getLong(idxCurrentChild - idxBegin, nrofBytes);  
      return value;
    }
    else throw new RuntimeException("Not available in expand mode.");
  }
  
  

  
  /**Adds a child for 1 float value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final float getChildFloat() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(4))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      int intRepresentation = (int)_getLong(idxCurrentChild - idxBegin, 4);  
      return Float.intBitsToFloat(intRepresentation);
     }
    else throw new RuntimeException("Not available in expand mode.");
  }
  
  

  
  /**Adds a child for 1 double value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final double getChildDouble() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(8))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      long intRepresentation = _getLong(idxCurrentChild - idxBegin, 8);  
      return Double.longBitsToDouble(intRepresentation);
     }
    else throw new RuntimeException("Not available in expand mode.");
  }
  
  

  
  
  /**Adds a child for a String value without a child instance, but returns the value as String.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   * @throws UnsupportedEncodingException 
   */
  public final String getChildString(int nrofBytes) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(nrofBytes))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      return _getString(idxCurrentChild - idxBegin, nrofBytes);  
    }
    else throw new RuntimeException("Not available in expand mode.  ");
  }
  
  
  /**remove the current child to assign another current child instead of the first one.
   * This method is usefull if data are tested with several structures.
   * It mustn't be called in expand mode. In expand mode you have to be consider about your children.
   * 
   * @param child
   * @throws IllegalArgumentException
   */
  @Java4C.inline
  final public void removeChild() 
  throws IllegalArgumentException
  { if(bExpand) throw new RuntimeException("don't call it in expand mode");
    //revert the current child.
    idxCurrentChildEnd = idxCurrentChild;
    idxCurrentChild = -1;
  }




  /**Remove all children. Let the head unchanged.
   * @since 2010-11-16
   */
  @Java4C.inline
  public final void removeChildren()
  { 
    idxCurrentChildEnd = idxBegin + sizeHead;
    if(bExpand){
      idxEnd = idxBegin + sizeHead;
    }
    idxCurrentChild = -1;
  }




  /**Remove all connections. Especially for children. */
  @Java4C.inline
  final public void detach()
  { data = null;
    parent = null;
    idxBegin = idxEnd = 0;
    idxCurrentChild = idxCurrentChildEnd = sizeHead;
    bExpand = false;
  }




  /**Returns the position of the Element data in the assigned buffer.
   * 
   * @return index of this element in the data buffer.
   */  
  @Java4C.retinline
  final public int getPositionInBuffer()
  { return idxBegin;
  }




  /**Returns the position of the current child in the assigned buffer.
   * 
   * @return index of the current child of this element in the data buffer.
   */  
  @Java4C.retinline
  final public int getPositionNextChildInBuffer()
  { return idxCurrentChildEnd;
  }




  /**copies the data from another references data into this data.
   * The src is positioned to any child. That's data are copied in this data at the position of this child.
   * The length of both children should be equal. TODO to test.
   * Note this method is never used in actual implementations of vishia. Check whether it is necessary.
   * */
  @Java4C.inline
  final public void copyDataFrom(ByteDataAccess src)
  throws IllegalArgumentException
  { int len = src.getLength();
    if(data.length < len){
      /** @Java4C.StringBuilderInThreadCxt*/
      throw new IndexOutOfBoundsException("copy, dst to small" + len);
    }
    System.arraycopy(src.data,src.idxBegin,data,idxBegin,len);
  }




  /**copies some data to a int[], primarily to debug a content. 
   * @param dst This array is field, but only from data of the current element between idxBegin and idxEnd
   */
  public final void copyData(int[] dst)
  { int idxMax = idxEnd - idxBegin;
    if(idxMax/4 > dst.length) idxMax = 4* dst.length;
    int iDst = 0;
    for(int idx = 0; idx < idxMax; idx+=4)
    { dst[iDst++] = (int)_getLong(idx,4);
    }
  }




  @Java4C.retinline
  public final boolean assertNotExpandable()
  {
    assert(idxCurrentChild >0 && idxEnd > 0 && !bExpand);
    return true;
  }




  /**sets the idxCurrentChild to the known idxCurrentChildEnd.
   * This method is called while addChild. The state before is:
   * <ul><li>idxCurrentChild is the index of the up to now current Child, or -1 if no child was added before.
   * <li>idxCurrentChildEnd is the actual end index of the current Child, 
   *     or the index of the first child (after head, may be also 0 if the head has 0 bytes), 
   *     if no child was added before.
   * <ul>
   * The state after is:    
   * <ul><li>idxCurrentChild is set to the idxCurrentChildEnd from state before. 
   * <li>idxCurrentChildEnd is set to -1, because it is not defined yet.
   * <ul>
   * If idxCurrentChildEnd >= idxCurrentChild, it means that this operation respectively {@link next()}
   * was called before. Than this operation is done already, a second call does nothing.
   * <br>
   * The length of the current child should be set after this operation and before this operation respectively the calling operation addChild() 
   * will be called a second one.
   * This is done in the calling routines. 
   */
  final void setIdxtoNextCurrentChild() 
  //throws IllegalArgumentException
  {
    if(idxCurrentChildEnd >= idxCurrentChild )
    { //This is the standard case.
      //NOTE: idxCurrentChild = -1 is assert if no child is added before.
      idxCurrentChild = idxCurrentChildEnd;
    }
    else if(idxCurrentChildEnd == -2)
    { //next() was called before:
      //do nothing, because next() was performed before.
    }
    else
    { throw new RuntimeException("unexpected idxCurrentChildEnd"); //its a programming error.
    }
    idxCurrentChildEnd = -1;  //the child content is not checked, this index will be set if setLengthCurrentChildElement() is called.
  }




  /**Returns a String from the given position inside the actual element .
   * The bytes are interpreted in the given encoding.
   * 
   * @param idx The start position inside the child.
   * @param nmax Maximal number of bytes
   * @return The String representation of the bytes.
   */
  protected final String getString(int idx, int nmax)
  { String sRet;
    try{ sRet = new String(data, idxBegin + idx, nmax, "ISO-8859-1");} 
    catch (UnsupportedEncodingException e)  {sRet = null; }
    int pos0 = sRet.indexOf(0);
    if(pos0 >0 )
    { //The data are zero terminated!
      sRet = sRet.substring(0, pos0);
    }
    return sRet;
  }
  
  
  

  /**Sets a String to the the given position inside the actual element .
   * The bytes are interpreted in the given encoding.
   * 
   * @param idx The start position inside the child.
   * @param nmax Maximal number of bytes
   * @param ss The String representation of the bytes.
   * @return Number of written chars.
   * @deprecated, use {@link _setString(int, int, String)}
   * @throws  
   */
  protected final int setString(int idx, int nmax, String ss)
  { if(ss.length()>nmax){ ss = ss.substring(0, nmax); } //truncate.
    /**Use a @java2c=ByteStringJc. In C there may not be a difference between the String
     * and the string of byte[].*/
    byte[] byteRepresentation;
    try { byteRepresentation = ss.getBytes("ISO-8859-1");} 
    catch (UnsupportedEncodingException e){ byteRepresentation = null; }
    int len = byteRepresentation.length;
    if(len > nmax){ len = nmax; } //truncate.
    System.arraycopy(byteRepresentation, 0, data, idxBegin + idx, len);
    return len;
  }
  
  

  /**sets the content inside the acutal element with the character bytes from the given String.
   * No value < 0x20 is setted. If the String value contain a control character with code < 0x20,
   * a '?' is written. This behavior protected, that bytevalues < 0x20 can use to detect no String elements,
   * see {@link getByteNextChild()}.  
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   * 
   * @param idx the position in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The length of the byte[] area to set. 
   *        If the String value is longer as nrofBytes, it will be truncated. No exception occurs.
   *        If the String is shorter as nrofBytes, the rest is filled with 0.
   * @param value The String value.
   * @return The String which is stored at the designated area. 
   *         @pjava2c=nonPersistent. It references the String at the source area only. 
   * @throws UnsupportedEncodingException
   */
  protected final String _getString(final int idx, final int nrofBytes) 
  throws UnsupportedEncodingException
  {
    int idxData = idx + idxBegin;
    //int idxEnd = idxData + nrofBytes;
    String value = new String(data, idxData, nrofBytes, charset);
    int end = value.indexOf(0);
    if(end >=0){ value = value.substring(0, end); }
    return value;
  }
  
  
  
  /**sets the content inside the actual element with the character bytes from the given String.
   *  
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   * 
   * @param idx the position in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The length of the byte[] area to set. 
   *        If the String value is longer as nrofBytes, it will be truncated. No exception occurs.
   *        If the String is shorter as nrofBytes, the rest is filled with 0.
   * @param value The String value.
   * @param sEncoding The encoding of the String. null: Use standard encoding.
   * @param preventCtrlChars true then values < 0x20 are not set. 
   *                         If the String value contain a control character with code < 0x20,
   *                         a '?' is written. This behavior guarantees, that byte-values < 0x20 
   *                         can use to detect no-String elements, see {@link getByteNextChild()}. 
   * @throws UnsupportedEncodingException
   */
  protected final void _setString(final int idx, final int nrofBytes, final String value
    , String sEncoding, boolean preventCtrlChars) 
  throws UnsupportedEncodingException
  {
    int idxData = idx + idxBegin;
    int idxEnd = idxData + nrofBytes;
    /**@java2c=ByteStringJc. */
    byte[] chars;
    if(sEncoding == null){ sEncoding = "ISO-8859-1"; }
    chars = value.getBytes(sEncoding); 
    int srcLen = chars.length;
    for(int ii=0; ii<srcLen && ii < nrofBytes; ii++)
    { byte cc = chars[ii];
      if(preventCtrlChars && cc < 0x20){ cc = 0x3f; } //'?' in ASCII
      data[idxData++] = cc;
    }
    //fill up the rest of the string with 0-chars. 
    while(idxData < idxEnd)
    { data[idxData++] = 0;
    }
  }
  
  
  
  /** Gets a float value from the content of 4 byte. The float value is red
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.intBitsToFloat().
   */
  @Java4C.retinline
  protected final float getFloat(int idx)
  {
    int intRepresentation = getInt32(idx);
    float value = Float.intBitsToFloat(intRepresentation);
    return value;
  }
  
  @Java4C.retinline
  protected final double getDouble(int idx)
  {
    long intRepresentation = _getLong(idx,8);
    return Double.longBitsToDouble(intRepresentation);
   
  }
  
  @Java4C.retinline
  protected final long getInt64(int idx)
  { int nLo,nHi;
    if(bBigEndian)
    { nLo = getInt32(idx);
      nHi =  getInt32(idx + 4);
    }
    else
    { nLo = getInt32(idx+4);
      nHi =  getInt32(idx);
    }
    long val = nHi << 32;
    val |= nLo & 0xFFFFFFFF;
    return val;
  }

  /** Returns the content of 4 bytes inside the actual element as a integer number between -2147483648 and 2147483647,
   * big- or little-endian depending on setBigEndian().
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -2147483648 and 2147483647
   * */
  protected final int getInt32(int idx)
  { int val;
    if(bBigEndian)
    { val =  ((  data[idxBegin + idx])<<24)  //NOTE all 24 low-bits are 0
          |  (( (data[idxBegin + idx+1])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( (data[idxBegin + idx+2])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[idxBegin + idx+3])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (  (data[idxBegin + idx+3])<<24)  //NOTE all 24 low-bits are 0
          |  (( (data[idxBegin + idx+2])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( (data[idxBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[idxBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  
  @Java4C.retinline
  protected final int getUint32(int idx)
  { return getInt32(idx);
  }
  
  /** Returns the content of 2 bytes as a positive nr between 0..65535, big-endian
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final int getUint16(int idx)
  { int val;
    if(bBigEndian)
    { val =  (( (data[idxBegin + idx  ])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[idxBegin + idx+1])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( (data[idxBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[idxBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  /** Returns the content of 2 bytes as a positive nr between 0..65535 inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final short getInt16(int idx)
  { int val;
    if(bBigEndian)
    { val =  (( (data[idxBegin + idx  ])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( (data[idxBegin + idx+1])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( (data[idxBegin + idx+1])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( (data[idxBegin + idx  ])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return (short)val;
  }

  /** Returns the content of 1 bytes as ASCII
   * inside the actual element.
   *
   * @param idx the position of char in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the ASCII value
   * */
  protected final char getChar(int idx)
  { char val;
    val = (char) data[idxBegin + idx];
    return val;
  }
  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final byte getInt8(int idx)
  { byte val;
    val = data[idxBegin + idx];
    return val;
  }

  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final int getUint8(int idx)
  { int val;
    val = data[idxBegin + idx] & 0xff;
    return val;
  }
 
  
  
  

  
  protected final int getUint32(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
    return getUint32(idxBytes + 4*idxArray);
  }
  
  protected final int getInt32(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt32:" + idxArray);
    return getInt32(idxBytes + 4*idxArray);
  }
  
  protected final int getInt16(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    return getInt16(idxBytes + 2*idxArray);
  }
  
  protected final int getInt8(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected final int getUint16(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
    return getUint16(idxBytes + 2*idxArray);
  }
  
  protected final int getUint8(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected final float getFloat(int idxBytes, int idxArray, int lengthArray)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getFloat:" + idxArray);
    return getFloat(idxBytes + 4*idxArray);
  }
  
  
  
  

  /** Set the content of 4 byte from a float variable. The float value is stored
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.floatToRawIntBits().
   */
  @Java4C.inline
  protected final void setFloat(int idx, float value)
  {
    int intRepresentation = Float.floatToRawIntBits(value);
    _setLong(idx, 4, intRepresentation);
  }



  /** Set the content of 8 byte from a double variable. The double value is stored
   * according to the IEEE 754 floating-point "double format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Double.doubleToRawLongBits().
   */
  @Java4C.inline
  protected final void setDouble(int idx, double value)
  {
    long intRepresentation = Double.doubleToRawLongBits(value);
    _setLong(idx, 8, intRepresentation);
  }



  /** Set the content of 4 bytes as a integer number between -2147483648 and 2147483647,
   * big- or little-endian depended from setBigEndian().
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffffffff.
   * */
  protected final void setInt32(int idx, int value)
  { if(bBigEndian)
    { data[idxBegin + idx]   = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[idxBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 1 bytes as a positive nr between 0..256.
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffff.
   * */
  @Java4C.inline
  protected final void setInt8(int idx, int value)
  { data[idxBegin + idx] = (byte)(value & 0xff);
  }



  /** Set the content of 1 bytes as a positive nr between 0..255, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xff.
  * */
  @Java4C.inline
  protected final void setUint8(int idx, int value)
  { setInt8(idx, value);  //its the same because modulo!
  }

  /** Set the content of 4 bytes as a positive nr between 0..2pow32-1, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value as long. The value is taken modulo 0xffffffff.
  * */
  protected final void setUint32(int idx, long value)
  { //the same algorithm in source, but other action on machine level,
    //because value is long!
    if(bBigEndian)
    { data[idxBegin + idx]   = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[idxBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 2 bytes from an integer between -32768..32768,
   * or from an integer number between 0..65535. The value is interpreted
   * from the input parameter with modulo 0x10000.
   * Big- or little-endian.
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffff.
   * */
  protected final void setInt16(int idx, int value)
  { if(bBigEndian)
    { data[idxBegin + idx]   = (byte)((value>>8) & 0xff);
      data[idxBegin + idx+1] = (byte)(value & 0xff);
    }
    else
    { data[idxBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx]   = (byte)(value & 0xff);
    }
  }
  
  
  /** Set the content of 2 bytes as a positive nr between 0..65535, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xffff.
  * */
  @Java4C.inline
  protected final void setUint16(int idx, int value)
  { setInt16(idx, value);  //its the same because modulo!
  }


  
  @Java4C.inline
  protected final void setUint32(int idxBytes, int idxArray, int lengthArray, int val)
  { /** @Java4C.StringBuilderInThreadCxt TestRest */
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("setUint32:" + idxArray);
    setUint32(idxBytes + 4*idxArray, val);
  }
  
  @Java4C.inline
  protected final void setInt32(int idxBytes, int idxArray, int lengthArray, int val)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("setInt32:" + idxArray);
    setInt32(idxBytes + 4*idxArray, val);
  }
  
  @Java4C.inline
  protected final void setInt16(int idxBytes, int idxArray, int lengthArray, int val)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt16(idxBytes + 2*idxArray, val);
  }
  
  @Java4C.inline
  protected final void setInt8(int idxBytes, int idxArray, int lengthArray, int val)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt8(idxBytes + idxArray, val);
  }
  
  
  @Java4C.inline
  protected final void setUint16(int idxBytes, int idxArray, int lengthArray, int val)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint16(idxBytes + 2*idxArray, val);
  }
  
  @Java4C.inline
  protected final void setUint8(int idxBytes, int idxArray, int lengthArray, int val)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint8(idxBytes + idxArray, val);
  }
  
  @Java4C.inline
  protected final void setFloat(int idxBytes, int idxArray, int lengthArray, float val)
  { /** @Java4C.StringBuilderInThreadCxt*/
    if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setFloat(idxBytes + 4 * idxArray, val);
  }
  
  
  
  
  /** Increments the idxEnd and the idxCurrentChildEnd if a new child is added. Called only
   * inside method addChild(child) and recursively to correct
   * in all parents.
   */
  final protected void correctCurrentChildEnd(int idxEndNew)
  { if(idxEnd < idxEndNew) 
    { idxEnd = idxEndNew;
    }
    if(idxCurrentChildEnd < idxEndNew) 
    { idxCurrentChildEnd = idxEndNew;
    }
    if(parent != null)
    { parent.correctCurrentChildEnd(idxEndNew);
    }
  }




  /**sets the idxCurrentChildEnd and idxEnd. There are two modi:
    * <ul><li>Expand data: the idxEnd == idxCurrenChild. In this case the idxEnd will be expanded.
    *     <li>Use existing data: idxEnd > idxCurrentChild: 
    *         In this case the idxEnd should be >= idxCurrentChild + nrofBytes.
    * </ul>        
    * @param nrofBytes of the child
    * @return true if the data are expanded.
    * @throws IllegalArgumentException if there are not enough data. 
    *         In expanded mode the data.length are to less.
    *         In using existing data: idxEnd are to less. 
    */
   protected final boolean setIdxCurrentChildEnd(int nrofBytes) 
   throws IllegalArgumentException
   { if(bExpand)
     { if(data.length < idxCurrentChild + nrofBytes)
       { @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (idxCurrentChild + nrofBytes);
         throw new IllegalArgumentException(msg);
       }
     }
     else
     { if(idxEnd < idxCurrentChildEnd)
       { //not expand, but the nrof data are to few
         @Java4C.StringBuilderInThreadCxt String msg = "to few user data:"+ (idxCurrentChild + nrofBytes);
         throw new IllegalArgumentException(msg);
       }
     }
     expand(idxCurrentChild + nrofBytes);  //also of all parents
     return bExpand;
   }


   
   
   /**This method is especially usefully to debug in eclipse. 
    * It shows the first bytes of head, the position of child and the first bytes of the child.
    */
   @Override
   @Java4C.exclude
   public String toString() 
   { //NOTE: do not create a new object in every call, it is uneffective.
     if(data==null){ return "no data"; }
     else
     { if(toStringformatter == null){ toStringformatter = new StringFormatter(); }
       else { toStringformatter.reset(); }
       int sizeHead = getLengthHead();
       toStringformatter.addint(idxBegin, "33331").add("..").addint(idxBegin + sizeHead,"333331").add(":");
       if(sizeHead > 16){ sizeHead = 16; }
       if(sizeHead <0){ sizeHead = 4; }
       if(idxBegin + sizeHead > data.length){ sizeHead = data.length - idxBegin; }  
       toStringformatter.addHexLine(data, idxBegin, sizeHead, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
       toStringformatter.add(" child ").addint(idxCurrentChild,"-3331").add("..").addint(idxCurrentChildEnd,"-33331").add(":");
       if(idxCurrentChild >= idxBegin)
       { 
         sizeHead = idxCurrentChildEnd - idxCurrentChild;
         if(sizeHead > 16){ sizeHead = 16; }
         if(sizeHead <0){ sizeHead = 4; }
         if(idxCurrentChild + sizeHead > data.length){ sizeHead = data.length - idxBegin; }  
         toStringformatter.addHexLine(data, idxCurrentChild, sizeHead, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
       }
       final String ret = toStringformatter.toString();
       return ret;
     }  
   }


}
