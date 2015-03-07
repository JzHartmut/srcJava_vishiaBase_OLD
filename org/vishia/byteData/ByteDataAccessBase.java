package org.vishia.byteData;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.vishia.bridgeC.IllegalArgumentExceptionJc;
import org.vishia.util.Debugutil;
import org.vishia.util.Java4C;
import org.vishia.util.StringFormatter;

/**This class is a base class to control the access to binary data.
 * The binary data may typically used or produced from a part of software written in C or C++.
 * Therewith the binary data are struct-constructs. Another example - build of datagram structures.
 * <br>
 * This class is a base class which should be derived for user's necessities. 
 * The methods {@link #getInt16(int)} etc. are protected. That should prevent erratic free accesses to data 
 * on application level. A derived class of this class structures the software for byte data access.
 * <br><br> 
 * It is able to support several kinds of structured data access:<ul>
 * <li>Simple C-like <code>struct</code> are adequate mapped with a simple derived class of this class, 
 *     using the protected commonly access methods like {@link #_getLong(int, int)} with predefined indexes 
 *     in special methods like getValueXyz().
 * <li>Complex <code>struct</code> with nested <code>struct</code> inside are mapped 
 *     with one derived class per <code>struct</code>, define one reference per nested struct 
 * <li>Base <code>struct</code> inside a <code>struct</code> (inheritance in C) can be mapped with 
 *     extra derived classes for the base struct and usind the
 *     {@link #assignCasted_i(ByteDataAccessBase, int, int)}-method.
 * <li>A pack of data with several struct may be mapped using the {@link #addChild(ByteDataAccessBase)}-method.
 *     Thereby a parent should be defined, and the structs of the pack are children of this parent. 
 *     That structures need not be from the same type.
 * <li>packs of struct with parent are able to nesting, it is able to construct as a tree of pack of structures. 
 *     The parent of the pack is the tree node. It is likewise a XML tree. 
 *     The data may be also transformed to a XML data representation
 *     or the data structure may be explained with a XML tree, but they are not
 *     XML data straight.
 * </ul>
 * This application possibilities show a capable of development system to access binary data. 
 * The other, hand made way was calculate with indices of the byte[idx] specially user programmed. 
 * This class helps to make such complex index calculation needlessly. 
 * One struct at C level corresponds with one derivated class of ByteDataAccessBase. 
 * But last also a generation of the java code from C/C++-header files containing the structs is able to.
 * So the definition of byte positions are made only on one source. The C/C++ is primary thereby. 
 *      
 * <br>
 * The UML structure of such an class in a environment may be shown with the
 * followed object model diagram, <br>
 * <code> <+>---> </code>is a composition,
 * <code> <>---> </code>is a aggregation, <code> <|---- </code>is a inherition.
 *  <pre>
 *                      +-------------------------------+                 +----------+
 *                      | ByteDataAccessBase(sizeHead)  |----data-------->| byte[]   |
 *                      |-------------------------------|                 |          |
 *                      |- idxBegin:int                 |                 | d a t a  |
 *                      |- idxChild:int                 |                 +----------+
 *  +-------------+     |- idxEnd:int                   |
 *  | derivated   |     |-------------------------------|<---------------+ a known parent
 *  | user        |---|>|+ addChild(child)              |                |
 *  | classes     |     |+ addChildFloat()              |---parent-------+ set in addChild()
 *  +-------------+     |% getInt32()                   |
 *                      +-------------------------------+
 * </pre>
 *
 * This class is the base class for ByteDataAccess. It works without dynamic methods proper for C usage.
 * All variables are package private because they should be changed only with methods of this class.
 * <br><br>
 * <h2>Initialization and instances</h2>
 * The root instance of data should be initialized with
 * <pre>
 * MyByteDataAccessRoot accessRoot = new MyByteDataAccessRoot(); 
 *   //... invokes super.ByteDataAccessBase(lengthHead);
 * accessRoot.assign(myData, dataLength);
 * </pre>  
 * Any instances which represents a sub structure in data can be created as re-useable instances, which can be added
 * as child of the root instance or as child of any already added child on demand:
 * <pre>
 * MySubStruct subStruct = new SubStruct();  //an instance for accessing 
 *   ....
 *   accessRoot.addChild(subStruct);       //adds on current position.
 *   int value = subStruct.getValuexyz();  //now can access the data.
 * </pre>
 * 
 * 
 * 
 * <br>  
 * <h2>children, currentChild, addChild</h2>
 * Children are used to evaluate or write different data structures after a known structure. 
 * The children may be from several derived types of this class. 
 * With children and children inside children a tree of different data can be built or evaluated.
 * 
 * If no child is added yet, the indices have the following values:
 * <ul>
 * <li>ixChild = -1.
 * <li>ixChildEnd = index after known (head-) data.
 * </ul>
 * A call of {@link addChild()} or its adequate addChildXY() sets the indices to the given current child:
 * <ul>
 * <li>ixChild = the index after known (first head-) data, the index of the child.
 * <li>ixChildEnd = ixChild + {@link #sizeHead} of the child.     
 * </ul>
 * The length of the current Child may be set while evaluating the child's data. 
 * The user should call {@link #setLengthElement(int)} with the child 
 * <ul>
 * <li>ixChild = is still the index of the child.
 * <li>ixChildEnd = ixChild + given length.
 * </ul>
 * 
 * 
 * 
 * @java2c = noObject.
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class ByteDataAccessBase
{
  
  /**The version. 
   * <ul>
   * <li>2014-09-05 Hartmut chg: ixChild removed, unnecessary, instead {@link #currentChild}, proper for debug. 
   * <li>2014-09-05 Hartmut chg: Some problems fixed in C-Application. Runs in Java. Meaning of {@link #bExpand} = TODO Store this version as well running.
   * <li>2014-09-05 Hartmut chg: Designation with some Java4C annotation.
   * <li>2014-09-05 Hartmut chg: now based on a {@link #charset} of type {@link java.nio.charset.Charset}. In C it is a dummy yet.
   *  In Java the Charset can be used immediately, more simple.
   * <li>2014-09-05 Hartmut bugfix: Some references changed from ByteDataAccess to ByteDataAccessBase.
   * <li>2014-09-05 Hartmut chg: {@link #getString(int, int)} now may contain 0-character. Only the 0-bytes on end are removed because they are fill-bytes
   *   for a 4-byte-alignment. _getString(int, int) removed because it was the same.
   * <li>2014-08-23 Hartmut chg: Member idxFirstChild removed, the information is contained in {@link #sizeHead}. 
   * <li>2014-08-23 Hartmut new: this class ByteDataAccessBase works without virtual methods (overrideable), it is proper for usage in C-language
   *   and it is not necessary. The {@link #sizeHead} is set by construction. Only for the compatible now deprecated {@link ByteDataAccess} the sizeHead 
   *   is able to set via the package-private {@link #_setSizeHead(int)}.  
   * <li>2014-01-12 Hartmut new: Java4C.inline for C-compilation. 
   * <li>2013-12-08 Hartmut new: {@link #ByteDataAccessBase(int, int)} as super constructor with given head and data size.
   *   {@link #addChild(ByteDataAccessBase)} accepts an initialized not used child. Uses {@link #kInitializedWithLength}.
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
  protected @Java4C.PtrVal byte[] data;
  
  /**Index of the beginning of the actual element in data*/
  protected int ixBegin;


  /**Index of the currents child end.
   * If no current child is known this index is equal ixBegin + sizeHead, it is the position after the head. 
   * If the length of the current child is not known, this index is <= -1. That is after {@link #addChild(ByteDataAccessBase, int)} with lenght=-1.
   */
  protected int ixChildEnd;

  /** Index of the end of the actual element in data. If {@link #bExpand} is set, this idxEnd and the idxEnd of all parents are increased
   * if an child was added. If bExpand==false then this value is set via the {@link #addChild(ByteDataAccessBase, int)} or {@link #addChildAt(int, ByteDataAccessBase, int)}.*/
  protected int ixEnd;

  /**True if the {@link #ixEnd} should not be set to the {@link #sizeHead} on removing children. */
  protected boolean bExpand;

  /** Flag is set or get data in big endian or little endian (if false)*/
  protected boolean bBigEndian;

  /** The parent element, necessary only for add() and expand().
   */
  protected ByteDataAccessBase parent;

  /**The last added child, null either if a child is not added or a child was added but the instance is used meanwhile otherwise.
   * This reference is used in the productive code only for {@link #removeChild()}. It is contained here especially for debugging.
   * Therefore it is private to forbid other usage.
   */
  private ByteDataAccessBase currentChild;

  /**The charset to build Strings.*/
  @Java4C.SimpleRef
  private Charset charset = Charset.forName("ISO-8859-1") ;   //NOTE: String(..., Charset) is only support from Java 6
  
  /**Use especially for test, only used in toString(). */ 
  @Java4C.Exclude
  protected StringFormatter toStringformatter = null;


  /**Any instance of ByteDataAccessBase is associated to a determined derived instance which has defined head size.
   * The sizeHead can be given with -1 only for derived instances of {@link ByteDataAccess}.
   * That class defines a method {@link ByteDataAccess#specifyLengthElementHead()} to get the head's size.
   * @param sizeHead The size of head data, it is the number of bytes.
   */
  protected ByteDataAccessBase(int sizeHead){
    this.sizeHead = sizeHead;
  }
  
  
  /**Constructs a new empty instance with a given head size and a given size for children.
   * That instance is not expandable. 
   * @param sizeHead The size of head data, it is the number of bytes.
   * @param sizeData number of significant bytes in data for all children.
   * */
  protected ByteDataAccessBase(int sizeHead, int sizeData){
    assert(sizeHead >=0);
    assert(sizeData >0);
    this.sizeHead = sizeHead;
    ixBegin = 0;
    ixEnd = sizeData;
    ixChildEnd = sizeHead;
    parent = null;
  }


  
  
  /**Sets the big or little endian mode. 
   *
   * @param val true if big endian, hi byte at lower adress, false if little endian.
   */
  @Java4C.Inline
  public final void setBigEndian(boolean val)
  { bBigEndian = val;
  }


  public final void setCharset(String value) {
    charset = Charset.forName(value);
  }
  
  
  /**This method is package private because it is only used for the methodes of ByteDataAccess (with virtual methods).
   * @param size The element {@link #sizeHead} is changed (should be final).
   */
  @Java4C.Inline
  /*package private*/ final void _setSizeHead(int size){
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
   * with the known length. Then the data can be evaluate by calling {@link #addChild(ByteDataAccessBase)}.
   * or by getting data from the head only if children should ot be used.
   * <br><br>
   * See {@link #assignEmpty(byte[])}, {@link #assignData(byte[], int)}. This routine
   * is called there after setting the data reference and the {@link #ixBegin}. In opposite to the
   * newly assignment of data, the {@link #data}-reference is not changed, the {@link #ixBegin}
   * is not changed and a {@link #parent} is not changed. It means, a reset can be invoked for any child
   * of data without changing the context.
   * 
   * @param lengthHead Number of bytes for the head of this element. It may equal lengthData.
   * @param lengthData Number of bytes for this element. If <=0 (usual 0 or -1), the element is set to expandable.
   *   
   */
  /*package private*/ final void _reset(int lengthData){
    assert(sizeHead >=0);
    bExpand = lengthData <= 0;  //expand if the data have no head.
    ixChildEnd = ixBegin + sizeHead;
    //lengthData is inclusively head. maybe checl lengthData, >=sizeHead or 0.
    this.ixEnd = bExpand ? this.ixBegin + this.sizeHead : this.ixBegin + lengthData;
    { //@Java4C.Exclude
      if(ixEnd > data.length)
      { @Java4C.StringBuilderInThreadCxt String msg = "not enough data bytes, requested=" + ixEnd + ", buffer-length=" + data.length;
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
    { idx = ixBegin + idxInChild;
      idxStep = 1;
    }
    else
    { idx = ixBegin + idxInChild + nrofBytes -1;
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
    { idx = ixBegin + idx + nrofBytes -1;
      idxStep = -1;
    }
    else
    { idx = ixBegin + idx;
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
  /*package private*/ final void _expand(int ixChildEndNew)
  { if(ixEnd < ixChildEndNew) 
    { //do it only in expand mode
      ixEnd = ixChildEndNew;
    }
    assert(ixChildEndNew >= ixBegin + sizeHead);
    if(ixChildEnd < ixChildEndNew) 
    { ixChildEnd = ixChildEndNew;
    }
    if(parent != null)
    { parent._expand(ixChildEndNew);
    }
  }

  
  

  /**Assigns new data to this element at given index in data. 
   * This method is called on {@link #addChild(ByteDataAccessBase)}.
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
  public final void assign(@Java4C.PtrVal byte[] dataP, int lengthData, int index) 
  throws IllegalArgumentException
  { assert (index >= 0 && this.sizeHead >=0);
    detach();
    this.data = dataP;
    ixBegin = index;
    parent = null;
    _reset(lengthData);
  }
  
  @Java4C.Inline
  public final void assign(@Java4C.PtrVal byte[] dataP, int lengthData){ assign(dataP, lengthData, 0); } 
  
  @Java4C.Inline
  public final void assign(@Java4C.PtrVal byte[] dataP){ assign(dataP, dataP.length, 0); } 
  
  /**Initializes a top level instance, the data are considered as non initialized.
   * The current length of the valid data is the length of the head of this element
   * set by the constructor.
   * The head should be filled with data after that calling some methods like
   * {@link #setInt32(int, int)}.
   * <br>
   * The children should be added by calling {@link addChild(ByteDataAccessBase)}
   * and filled with data after that.
   *
   * <br>
   * If this instance is using before, its connection to an other parent is dissolved.
   * <br>
   * Example to shown the principle:<pre>
   *   ...........the data undefined with defined length.........
   *   +++++                   Head, the length should be known.
   *        ####****#####****  Space for children,
   * </pre>
   * @param data The data. It should be not null. The data are filled with 0 initial. 
   */
  @Java4C.Inline
  final public void assignClear(@Java4C.PtrVal byte[] data) 
  { Arrays.fill(data, (byte)0);
    assign(data, -1, 0);
  }

  

  /**assigns the element to the given position of the parents data to present a child of the parent
   * with a defined length.
   * The difference to {@link addChild(ByteDataAccessBase)} is: The position is given here
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
  @Java4C.Inline
  final public void assignAt(int idxChildInParent, int lengthChild, ByteDataAccessBase parent)
  throws IllegalArgumentException
  { this.bBigEndian = parent.bBigEndian;
    this.bExpand = parent.bExpand;
    assign(parent.data, parent.ixBegin + idxChildInParent + lengthChild, parent.ixBegin + idxChildInParent);
    setBigEndian(parent.bBigEndian);
  }


  /**assigns the element to the given position of the parents data to present a child of the parent.
   * The length of the child is limited to TODO the length of head - or not limited.
   * @param parent The parent. It should reference data.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  @Java4C.Inline
  final public void assignAt(int idxChildInParent, ByteDataAccessBase parent)
  throws IllegalArgumentException
  { assignAt(idxChildInParent, sizeHead, parent);
  }


  

  

  /**Assigns this element to the same position in data, but it is another view. 
   * This method should be called inside a assignCasted() method if a inner head structure is known
   * and the conclusion of this structure is possible. At result, both ByteDataAccessBase instances reference the same data,
   * in different views.
   * @param src The known data access
   * @param offsetCastToInput typical 0 if single inherition is used.
   * @throws IllegalArgumentException if a length of the new type is specified but the byte[]-data are shorter. 
   *                         The length of byte[] is tested. 
   */
  @Java4C.Inline
  final protected void assignCasted_i(ByteDataAccessBase src, int offsetCastToInput, int lengthDst)
  throws IllegalArgumentException
  { this.bBigEndian = src.bBigEndian;
    bExpand = src.bExpand;
    //assignData(src.data, src.idxBegin + lengthDst + offsetCastToInput, src.idxBegin + offsetCastToInput);
    assign(src.data, src.ixEnd, src.ixBegin + offsetCastToInput);
    //lengthDst is unsused, not necessary because lengthElementHead is knwon!
  }


  

  
  /** Returns the data buffer itself. The actual total length is getted with getLengthTotal().
   * @return The number of bytes of the data in the buffer.
   */
  @Java4C.Retinline @Java4C.PtrVal 
  final public byte[] getData()
  { return  data;
  }




  /** starts the calling loop of next().
   * The calling of next() after them supplies the first child element.
   *
   */
  @Java4C.Inline
  public final void rewind()
  { ixChildEnd = ixBegin + sizeHead;
    if(currentChild !=null){
      currentChild.detach();
      currentChild = null;
    }
  }




  /**Returns the length of the head. This method returns the size of the head given on construction
   * or set with {@link #_setSizeHead(int)} (package private). The size of the head is not changed normally for an existing instance.
   */ 
  @Java4C.Retinline
  public final int getLengthHead(){ return sizeHead; }
  
  
  
  /**Returns the length of the element with all yet added children. 
   * It is the current used length of this element. Note that {@link #getLength()} returns the length
   * which may be not evaluated with children up to know but given with {@link #assign(byte[], int)}.
   * 
   * @return index of the current child's end of this element relative to this start position.
   */  
  @Java4C.Retinline
  final public int getLengthCurrent()
  { return ixChildEnd - ixBegin; 
  }


  /** Returns the length of the existing actual element.
   * @return The number of bytes of the actual element in the buffer.
   *         It is (idxEnd - idxBegin).
   */
  @Java4C.Retinline
  final public int getLength()
  { return ixEnd - ixBegin;
  }




  /** Returns the length of the data.
   * @return The number of bytes of data in the buffer.
   *         It is idxEnd.
   */
  @Java4C.Retinline
  final public int getLengthTotal()
  { return ixEnd;
  }




  /**returns the number number of bytes there are max available from position of the current child. 
   * ,
   * the number of bytes to the end of buffer is returned.
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   */ 
  @Java4C.Retinline
  final public int getMaxNrofBytes()
  { return data.length - ixBegin;
  }


  @Java4C.Retinline final public boolean getBigEndian(){ return bBigEndian; }
  

  /**Sets the length of the element in this and all {@link #parent} of this. 
   * If the element is a child of any parent, it should be the current child of the parent. 
   * The {@link #ixEnd} and the {@link #ixChildEnd} of this and all its parents is set
   * with the (this.{@link #ixBegin}+length).
   * <br><br>
   * This routine is usefully if data are set in a child directly without sub-tree child structure
   * (without using {@link #addChild(ByteDataAccessBase)}).
   * It is if the element has data after the head with different length without an own children structure.
   * 
   * @param length The length of data of this current (last) child.
   */
  @Java4C.Inline
  final public void setLengthElement(int length)
  { //if(!bExpand && )
    _expand(ixBegin + length);
  }




  /**Sets all data of the head of this element to 0.
   * Note: If the element has not a head, this method does nothing.
   */
  @Java4C.Inline
  public final void clearHead(){
    Arrays.fill(data, ixBegin, ixBegin + sizeHead, (byte)0);
  }




  /**Sets all data of this element to 0.
   * Note: The idxEnd should be set to the end of the element.
   * This method is proper to use for a simple element only.
   */
  @Java4C.Inline
  public final void clearData(){
    Arrays.fill(data, ixBegin, ixEnd, (byte)0);
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




  /**returns the maximal number of bytes which are available from position of a next current child. 
   * It returns (ixEnd - ixChildEnd). With this information any child which head length is less or equal can be added to check whether it is matched to the data. 
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   * @throws IllegalArgumentException if the length of the current child is not determined yet.
   *         Either the method specifyLengthElement() should be overwritten or the method 
   *         {@link setLengthElement(int)} for the child or {@link setLengthCurrentChildElement(int)}
   *         should be called to prevent this exception.  
   */ 
  @Java4C.Retinline
  final public int getMaxNrofBytesForNextChild() throws IllegalArgumentException
  { return ixEnd - ixChildEnd;
  }




  /**Adds a child Element after the current child or as first child after head.
   * With the aid of the child Element the data can be read or write structured.
   * <br><br>
   * The child instance will be initialized newly. Any old usage of the child instance will be ignored.
   * The child is a helper to get data and to manage its own indices and in the parent.
   * The child can be used to add further children to build a tree of children.
   * <br><br>
   * 
   *<br>
   * Some children can be added after a parent like following sample:
   * <pre>
   * ByteAccessDerivation child = new ByteAccessDerivation();  //empty and unassigned.
   * ...
   * parent.addChild(child);        //The byte[] data of parent are assigned, index after current child index of parent.
   * child.addChild(grandchild);    //By adding a child to this child, also the parent's index is corrected.
   * </pre>
   * <b>Indices in the parent and child</b>:<br>
   * A new assigned instance of ByteDataAccessBase which has not added a child, only the head data are known, 
   * the indices are set to following:
   * <ul>
   * <li>parent.ixChild = -1 because there is not a child.
   * <li>parent.ixChildEnd = index after known (head-) data.
   * <li>parent.ixEnd = ... end of data
   * </ul>
   * A call of this method {@link #addChild(ByteDataAccessBase, int)} or its adequate addChildXY() sets the indices 
   * to the given current child:
   * <ul>
   * <li>parent.ixChild = the index after known (first head-) data, the index of the current child.
   * <li>parent.ixChildEnd = ixChild + {@link #sizeHead} of the child.
   * <li>parent.ixEnd = ... >= ixChildEnd, incremented if necessary and {@link #bExpand} is true.     
   * <li>child.ixChild = -1.
   * <li>child.ixChildEnd = index after known (head-) data of the child.
   * <li>child.ixEnd = 
   * </ul>
   * The length of the current Child may be set while evaluating the child's data. 
   * The user should call {@link #setLengthElement(int)} with the child 
   * <ul>
   * <li>ixChild = is still the index of the child.
   * <li>ixChildEnd = ixChild + given length.
   * </ul>
   *
   * @param child The child will be assigned with the data of this at index after the current child's end-index.
   *   Note that the child's sizeHead should be set correctly.
   * @param sizeChild The number of bytes which are used from the child or 0. If it is 0, then the child's sizeHead is used 
   *   to set this.{@link #ixChildEnd}, elsewhere {@link #ixChildEnd} is set using that value.
   *   The child itself does not use this value.
   * @throws IllegalArgumentException if the length of the old current child is not determined yet.
   *         Either the method specifyLengthElement() should be overwritten or the method 
   *         {@link setLengthElement(int)} for the child or {@link setLengthCurrentChildElement(int)}
   *         should be called to prevent this exception.  
   * @throws IllegalArgumentException if the length of the head of the new current child is to far for the data.
   *         It means, child.idxEnd > data.length. 
   */
  final public void addChild(ByteDataAccessBase child, int sizeChild) 
  throws IllegalArgumentException
  { if(child.parent !=null && child.parent.currentChild == child){ child.parent.currentChild = null; } //detatch
    assert(sizeChild == 0 || sizeChild >= child.sizeHead);
    assert(child.sizeHead >=0);
    child.ixBegin = setIdxtoNextCurrentChild(sizeChild ==0 ? child.sizeHead: sizeChild);
    child.bBigEndian = bBigEndian;
    child.bExpand = bExpand;
    child.data = this.data;
    child.parent = this;
    child.charset = this.charset;
    child.ixChildEnd = child.ixBegin + child.sizeHead;  //the child does not contain grand children.
    child.ixEnd = bExpand ? child.ixChildEnd : this.ixEnd;  //use the full data range maybe for child.
    currentChild = child;
    assert(child.ixEnd <= data.length);
    if(bExpand){ _expand(child.ixEnd); }  
    //return bExpand;
  }

  
  
  /**Adds a child with its given head size without additional data space.
   * It calls {@link #addChild(ByteDataAccessBase, int)}.
   * @param child The child will be initialized newly.
   */
  @Java4C.Retinline
  final public void addChild(ByteDataAccessBase child){ addChild(child, child.sizeHead); } 
  
  
  final public void addChildEmpty(ByteDataAccessBase child) 
  { addChild(child);  //first add the child
    child.clearHead(); //then clears its data.
  }
  
  
  
  final public void addChildEmpty(ByteDataAccessBase child, int sizeChild) 
  { addChild(child, sizeChild);  //first add the child
    child.clearData(); //then clears its data.
  }
  

  
  /**Adds a child at any position. This method is usefully if the data structure is known and specific elements should be accessed.
   * The child can be used to detect data in a sub structure.  
   * @param idxChild The index from the position of this (from this.{@link #ixBegin}).
   *   It is not the index inside the {@link #data}. 
   *   If {@link #ixBegin} ==0 because this is the root ByteDataAccess instance, it is the index in data.
   * @param child An empty instance for a child with given child.{@link #sizeHead}. Usual instances are re-used (especially in a C-environment). 
   *   child will be filled completely. A content before is cleared except the child.{@link #sizeHead}. This value should be known in the child.
   * @param sizeChild The size of the child. It may be 0, but >=0 (asserted)
   * @throws IllegalArgumentException
   */
  final public void addChildAt(int idxChild, ByteDataAccessBase child, int sizeChild) 
  throws IllegalArgumentException
  { assert(child.sizeHead >=0);
    assert(sizeChild >= child.sizeHead);
    if(child.parent !=null && child.parent.currentChild == child){ child.parent.currentChild = null; } //detatch
    child.data = data;
    int idxBegin = this.ixBegin + idxChild;
    child.ixBegin = idxBegin;
    child.ixEnd = idxBegin + sizeChild;
    child.ixChildEnd = idxBegin + child.sizeHead;
    child.bBigEndian = bBigEndian;
    child.bExpand = bExpand;
    child.parent = this;
    _expand(child.ixEnd);  
    //return bExpand;
  }

  
  /**Adds a child at any position with its head size. 
   * This method is usefully if the data structure is known and specific elements should be accessed.
   * The child can be used to detect data in a sub structure.  
   * @param idxChild The index from the position of this (from this.{@link #ixBegin}).
   *   It is not the index inside the {@link #data}. 
   *   If {@link #ixBegin} ==0 because this is the root ByteDataAccess instance, it is the index in data.
   * @param child An empty instance for a child with given child.{@link #sizeHead}. Usual instances are re-used (especially in a C-environment). 
   *   child will be filled completely. A content before is cleared except the child.{@link #sizeHead}. This value should be known in the child.
   * @throws IllegalArgumentException
   * @throws IllegalArgumentException
   */
  @Java4C.Retinline
  final public void addChildAt(int idxChild, ByteDataAccessBase child) 
  throws IllegalArgumentException
  { addChildAt(idxChild, child, child.sizeHead);
  }  
  
  

  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final void addChildInteger(int nrofBytes, long value) 
  throws IllegalArgumentException
  { assert(nrofBytes >0);
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    if(data.length < ixChild1 + nrofBytes){
      @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (ixChild1 + nrofBytes);
      throw new IllegalArgumentException(msg);
    }
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setLong(ixChild1 - ixBegin, nrofBytes, value);  
  }




  /**Adds a child for 1 float value without a child instance, and sets the value as float.
   * The indices of this are incremented to the next child position after the added float value.
   * 
   * The byte representation is the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values.
   * See {@link java.lang.Float#floatToRawIntBits(float)}.
   * 
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final void addChildFloat(float value) 
  throws IllegalArgumentException
  { int ixChild1 = setIdxtoNextCurrentChild(4);
    if(data.length < ixChild1 + 4){
      @Java4C.StringBuilderInThreadCxt String msg = "data length to small:"+ (ixChild1 + 4);
      throw new IllegalArgumentException(msg);
    }
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    setFloat(ixChild1 - ixBegin, value);  
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
  { int nrofBytes = value.length();
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setString(ixChild1 - ixBegin, nrofBytes, value, sEncoding, preventCtrlChars);  
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
  { int nrofBytes = valueCs.length();
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    for(int ii=0; ii<nrofBytes; ++ii){
      byte charByte = (byte)(valueCs.charAt(ii));  //TODO encoding
      data[ixChild1+ii] = charByte;
    }
  }




  /**Adds a child with String value.
   * 
   * @param value String to add, @pjava2c=nonPersistent.
   * @throws IllegalArgumentException
   */
  @Java4C.Inline
  public final void addChildString(CharSequence value) throws IllegalArgumentException
  { try{ addChildString(value, null); } 
    catch(UnsupportedEncodingException exc){ throw new RuntimeException(exc);} //it isn't able.
  }



  /**Adds a child for 1 short value without a child instance, returns the value as short.
   * 
   * @return The value which is represented by the next bytes after the current child before call this routine.
   */
  @Java4C.Retinline
  public final short getChildInt16()
  { int ixChild1 = setIdxtoNextCurrentChild(2);
    return getInt16(ixChild1 - ixBegin);
  }
  

  /**Adds a child for 1 short value without a child instance, returns the value as short.
   * 
   * @return The value which is represented by the next bytes after the current child before call this routine.
   */
  @Java4C.Retinline
  public final int getChildUint16()
  { int ixChild1 = setIdxtoNextCurrentChild(2);
    return getUint16(ixChild1 - ixBegin);
  }
  

  /**Adds a child for 1 short value without a child instance, returns the value as short.
   * 
   * @return The value which is represented by the next bytes after the current child before call this routine.
   */
  @Java4C.Retinline
  public final short getChildUint8()
  { int ixChild1 = setIdxtoNextCurrentChild(1);
    return getUint8(ixChild1 - ixBegin);
  }
  

  /**Adds a child for 1 integer value without a child instance, but returns the value as integer.
   * 
   * @param nrofBytes of the integer, if negative then gets as signed integer, elsewhere as unsigned
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final long getChildInteger(int nrofBytes) 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int bytes1 = nrofBytes < 0 ? -nrofBytes : nrofBytes;
    int ixChild1 = setIdxtoNextCurrentChild(bytes1);
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    long value = _getLong(ixChild1 - ixBegin, nrofBytes);  
    return value;
  }
  
  

  
  /**Adds a child for 1 float value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final float getChildFloat() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int ixChild1 = setIdxtoNextCurrentChild(4);
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    int intRepresentation = (int)_getLong(ixChild1 - ixBegin, 4);  
    return Float.intBitsToFloat(intRepresentation);
  }
  
  

  
  /**Adds a child for 1 double value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final double getChildDouble() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int ixChild1 = setIdxtoNextCurrentChild(8);
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    long intRepresentation = _getLong(ixChild1 - ixBegin, 8);  
    return Double.longBitsToDouble(intRepresentation);
  }
  
  

  
  
  /**Adds a child for a String value without a child instance, but returns the value as String.
   * 
   * @param nrofBytes of the String maybe with 0-bytes on end which will be removed (alignment).
   * @return value as String
   * @throws IllegalArgumentException if not data has not enough bytes.
   * @throws UnsupportedEncodingException 
   */
  public final String getChildString(int nrofBytes) 
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    assert(nrofBytes >=0);
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    return getString(ixChild1 - ixBegin, nrofBytes);  
   
  }
  
  
  /**Removes the current child to assign another current child instead on the position of the current child.
   * This method is usefully if data are tested with several structures.
   * See {@link #removeChild(ByteDataAccessBase)}.
   * 
   * @param child
   * @throws IllegalArgumentException
   */
  @Java4C.Inline
  final public void removeChild() 
  throws IllegalArgumentException
  { if(currentChild ==null) throw new IllegalStateException("programming error - a current child is not known yet.");
    removeChild(currentChild);
  }


  
  /**Shorten the evaluated content of the data to the position of the given child. The given child is removed.
   * See {@link #removeChild()}. The {@link #ixEnd} is set to {@link #ixChildEnd} if the expand mode is set.
   * In expand mode the {@link #ixEnd} is set to the {@link #ixChildEnd}, to the current child position. 
   * The content which was written before at following positions is not removed. It means a content can be written 
   * with one child type, and checked with another child type after them.
   *  
   * @param child It should be a child of this. The parent of the child should be this.
   */
  final public void removeChild(ByteDataAccessBase child)
  {
    if(child.parent !=this) throw new IllegalArgumentException("programming error - child is not parent of this.");
    this.ixChildEnd = child.ixBegin;   //set end index to the child's start
    if(bExpand) {
      this.ixEnd = this.ixChildEnd;
    }
    if(currentChild != null) { currentChild.detach(); }
  }
  
  


  /**Remove all children. Let the head unchanged.
   * @since 2010-11-16
   */
  @Java4C.Inline
  public final void removeChildren()
  { if(currentChild !=null){
      currentChild.detach();
      currentChild = null;
    }
    ixChildEnd = ixBegin + sizeHead;
    if(bExpand){
      ixEnd = ixBegin + sizeHead;  //reset idxEnd only in expand method, let it unchanged in read mode.
    }
  }




  /**Remove all connections. Especially for children. */
  @Java4C.Inline
  final public void detach()
  { if(parent !=null && parent.currentChild == this){ 
      parent.currentChild = null;  //detach in parent
    }
    if(currentChild !=null) {
      currentChild.detach();
      currentChild = null;  //necessary if currentChild don't refers this parent because any error before.
    }
    data = null;
    parent = null;
    ixBegin = ixEnd = 0;
    ixChildEnd = 0;
    bExpand = false;
  }




  /**Returns the position of the Element data in the assigned buffer.
   * 
   * @return index of this element in the data buffer.
   */  
  @Java4C.Retinline
  final public int getPositionInBuffer()
  { return ixBegin;
  }




  /**Returns the position of a next child which can be added in the assigned buffer.
   * See {@link #getLengthCurrent()}.
   * 
   * @return index of the current child's end of this element in the data buffer.
   */  
  @Java4C.Retinline
  final public int getPositionNextChildInBuffer()
  { return ixChildEnd;  ////
  }




  /**copies the data from another references data into this data.
   * The src is positioned to any child. That's data are copied in this data at the position of this child.
   * The length of both children should be equal. TODO to test.
   * Note this method is never used in actual implementations of vishia. Check whether it is necessary.
   * */
  //@Java4C.Inline
  final public void copyDataFrom(ByteDataAccessBase src)
  throws IllegalArgumentException
  { int len = src.getLength();
    if(data.length < len){
      throw new IndexOutOfBoundsException("copy, dst to small" + len);
    }
    ////TODO System.arraycopy(src.data,src.idxBegin,data,idxBegin,len);
  }




  /**copies some data to a int[], primarily to debug a content. 
   * @param dst This array is field, but only from data of the current element between idxBegin and idxEnd
   */
  public final void copyData(int[] dst)
  { int idxMax = ixEnd - ixBegin;
    if(idxMax/4 > dst.length) idxMax = 4* dst.length;
    int iDst = 0;
    for(int idx = 0; idx < idxMax; idx+=4)
    { dst[iDst++] = (int)_getLong(idx,4);
    }
  }






  /**Returns a String from the given position inside the actual element .
   * The bytes are interpreted in the given encoding. 
   * 0-characters on end of the spread in child till idx + nrofBytes are not taken as String characters.
   * They are 4-byte-alignment fill bytes usually. 
   * 0-bytes inside the String are taken as normal characters.
   * 
   * @param idx The start position inside the child.
   * @param nrofBytes The number of bytes to build the String.
   * @return The String representation of the bytes.
   *   Note: In C the String refers the bytes in {@link #data}.
   * @throws IndexOutOfBoundsException if any index is faulty.  
   */
  protected final String getString(int idx, int nrofBytes)
  {
    int idxData = idx + ixBegin;
    int idxEnd1 = idxData + nrofBytes;
    assert(idxEnd1 <= ixEnd && idxEnd1 <= data.length);
    while(data[--idxEnd1] ==0 && idxEnd1 > idxData);  //skip 0 character on end
    int len = idxEnd1 +1 - idxData; //resulting len without 0-character.
    String value;
    value = new String(data, idxData, len, charset);
    return value;
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
    @Java4C.ByteStringJc byte[] byteRepresentation;
    try { byteRepresentation = ss.getBytes("ISO-8859-1");} 
    catch (UnsupportedEncodingException e){ byteRepresentation = null; }
    int len = byteRepresentation.length;
    if(len > nmax){ len = nmax; } //truncate.
    System.arraycopy(byteRepresentation, 0, data, ixBegin + idx, len);
    return len;
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
    int idxData = idx + ixBegin;
    int idxEnd = idxData + nrofBytes;
    /**@java2c=ByteStringJc. */
    byte[] chars;
    if(sEncoding == null){ sEncoding = "ISO-8859-1"; }
    chars = value.getBytes(sEncoding); 
    int srcLen = chars.length;
    if(srcLen > nrofBytes){ srcLen = nrofBytes; }
    for(int ii=0; ii < srcLen; ++ii)
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
  @Java4C.Retinline
  protected final float getFloat(int idx)
  {
    int intRepresentation = getInt32(idx);
    float value = Float.intBitsToFloat(intRepresentation);
    return value;
  }
  
  @Java4C.Retinline
  protected final double getDouble(int idx)
  {
    long intRepresentation = _getLong(idx,8);
    return Double.longBitsToDouble(intRepresentation);
   
  }
  
  @Java4C.Retinline
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
    { val =  ((  data[ixBegin + idx])<<24)  //NOTE all 24 low-bits are 0
          |  (( (data[ixBegin + idx+1])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( (data[ixBegin + idx+2])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx+3])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (  (data[ixBegin + idx+3])<<24)  //NOTE all 24 low-bits are 0
          |  (( (data[ixBegin + idx+2])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( (data[ixBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  
  @Java4C.Retinline
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
    { val =  (( (data[ixBegin + idx  ])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx+1])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( (data[ixBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
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
    { val =  (( (data[ixBegin + idx  ])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx+1])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( (data[ixBegin + idx+1])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx  ])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
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
    val = (char) data[ixBegin + idx];
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
    val = data[ixBegin + idx];
    return val;
  }

  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final short getUint8(int idx)
  { short val;
    val = data[ixBegin + idx];
    if(val < 0){ val += 0x100; }
    return val;
  }
 
  
  
  

  
  protected final int getUint32(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
    return getUint32(idxBytes + 4*idxArray);
  }
  
  protected final int getInt32(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt32:" + idxArray);
    return getInt32(idxBytes + 4*idxArray);
  }
  
  protected final int getInt16(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    return getInt16(idxBytes + 2*idxArray);
  }
  
  protected final int getInt8(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected final int getUint16(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
    return getUint16(idxBytes + 2*idxArray);
  }
  
  protected final int getUint8(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected final float getFloat(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getFloat:" + idxArray);
    return getFloat(idxBytes + 4*idxArray);
  }
  
  
  
  

  /** Set the content of 4 byte from a float variable. The float value is stored
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.floatToRawIntBits().
   */
  @Java4C.Inline
  protected final void setFloat(int idx, float value)
  {
    int intRepresentation = Float.floatToRawIntBits(value);
    _setLong(idx, 4, intRepresentation);
  }



  /** Set the content of 8 byte from a double variable. The double value is stored
   * according to the IEEE 754 floating-point "double format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Double.doubleToRawLongBits().
   */
  @Java4C.Inline
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
    { data[ixBegin + idx]   = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[ixBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 1 bytes as a positive nr between 0..256.
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffff.
   * */
  @Java4C.Inline
  protected final void setInt8(int idx, int value)
  { data[ixBegin + idx] = (byte)(value & 0xff);
  }



  /** Set the content of 1 bytes as a positive nr between 0..255, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xff.
  * */
  @Java4C.Inline
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
    { data[ixBegin + idx]   = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[ixBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx]   = (byte)(value & 0xff);
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
    { data[ixBegin + idx]   = (byte)((value>>8) & 0xff);
      data[ixBegin + idx+1] = (byte)(value & 0xff);
    }
    else
    { data[ixBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx]   = (byte)(value & 0xff);
    }
  }
  
  
  /** Set the content of 2 bytes as a positive nr between 0..65535, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xffff.
  * */
  @Java4C.Inline
  protected final void setUint16(int idx, int value)
  { setInt16(idx, value);  //its the same because modulo!
  }


  private final void throwexc(String text, int idxArray){
    @Java4C.StringBuilderInThreadCxt String textExc = text + idxArray; 
    throw new IndexOutOfBoundsException(textExc);
  }
  
  
  @Java4C.Inline
  protected final void setUint32(int idxBytes, int idxArray, int lengthArray, int val)
  { 
    if(idxArray >= lengthArray || idxArray < 0) throwexc("setUint32:", idxArray);   
    setUint32(idxBytes + 4*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setInt32(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throwexc("setInt32:", idxArray);
    setInt32(idxBytes + 4*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setInt16(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt16(idxBytes + 2*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setInt8(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt8(idxBytes + idxArray, val);
  }
  
  
  @Java4C.Inline
  protected final void setUint16(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint16(idxBytes + 2*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setUint8(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint8(idxBytes + idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setFloat(int idxBytes, int idxArray, int lengthArray, float val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setFloat(idxBytes + 4 * idxArray, val);
  }
  
  
  
  
  /**Prepares a new child for this. It sets the this.{@link #ixChildEnd} to the end of the new child given by argument sizeChild.
   * The size of the new child, at least its head size, should be known. It is given by the calling argument sizeChild.
   * <br><br>
   * This method is called while addChild. The state before is:
   * <ul>
   * <li>ixChildEnd is the actual end index of the current Child, 
   *     or the index of the first child (after head, may be also 0 if the head has 0 bytes), 
   *     if no child was added before. ixChildEnd is always positive and should be valid.
   * </ul>
   * The state after is:    
   * <ul>
   * <li>ixChildEnd is set to the ixChild + argument sizeChild.
   * </ul>
   * The size of the child may be increased later by calling {@link #addChild(ByteDataAccessBase)} for an added child or by calling 
   * {@link #setLengthElement(int)} for the child. Therefore an argument =0 is possible.
   * <br><br>
   * The method is package private because it should not invoked from the user directly. 
   * 
   * @argument sizeChild yet known size of the child to add. It have to be >=0. 
   */
  private final int setIdxtoNextCurrentChild(int sizeChild) 
  //throws IllegalArgumentException
  { assert(sizeChild >=0);
    assert(ixChildEnd >=0);          //==0 os possible on an empty element without head.
    int ixChild1 = ixChildEnd;
    ixChildEnd += sizeChild;  
    if(bExpand) { 
      _expand(ixChildEnd); 
    }
    if(data.length < ixChildEnd){
      throw new IllegalArgumentExceptionJc("ByteDataAccess: less data", ixChildEnd);
    }
    return ixChild1;
  }


  
  /**Helper routine to show indices and content.
   * @param obj
   */
  private void addToString(ByteDataAccessBase obj, int recurs)
  {
    toStringformatter.addint(obj.ixBegin, "33331")
    .add("..").addint(obj.ixBegin + obj.sizeHead,"333331")
    .add("..").addint(obj.ixChildEnd,"333331")
    .add(obj.bExpand ? '+' : ':').addint(obj.ixEnd,"333331").add(":");
    //show content of head
    int bytesHex = obj.getLengthHead();
    if(bytesHex > 16){ bytesHex = 16; }
    if(bytesHex <0){ bytesHex = 0; }
    if(obj.ixBegin + bytesHex > data.length){ bytesHex = data.length - obj.ixBegin; }  
    toStringformatter.addHexLine(data, obj.ixBegin, bytesHex, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
    if(bytesHex < 24 && obj.currentChild ==null) {
      int bytesHexChild = obj.ixEnd - obj.ixBegin - obj.sizeHead;
      if(bytesHexChild >(24 - bytesHex)) { bytesHexChild = (24 - bytesHex); }  //don't show more as 24 bytes in sum.
      if(bytesHexChild >0) {
        toStringformatter.add(": ").addHexLine(data, obj.ixBegin + obj.sizeHead, bytesHexChild, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
      }
    }
    else if(obj.currentChild !=null) {
      if(recurs > 10){
        toStringformatter.add(", too many children");
      } else {
        toStringformatter.add(", child ");
        addToString(obj.currentChild, recurs+1);
      }
    }
  }
  
  
 

  /**This method is especially usefully to debug in eclipse. 
    * It shows the first bytes of head, the position of child and the first bytes of the child.
    */
   @Override
   @Java4C.Exclude
   public String toString() 
   { //NOTE: do not create a new object in every call, it is uneffective.
     if(data==null){ return "no data"; }
     else
     { if(toStringformatter == null){ toStringformatter = new StringFormatter(); }
       else { toStringformatter.reset(); }
       addToString(this,0);  //recursively call for all children, limitaded with recursCount.
       final String ret = toStringformatter.toString();
       return ret;
     }  
   }


}
