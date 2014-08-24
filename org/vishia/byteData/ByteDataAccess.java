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
 * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
 */
package org.vishia.byteData;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.vishia.util.StringFormatter;
import org.vishia.util.Java4C;



/**This class is a base class to control the access to binary data.
 * The binary data may typically used or produced from a part of software written in C or C++.
 * There the binary data are struct-constructs. Another example - build of datagram structures.
 * <br>
 * This class is a base class which should be derived for user's necessities. 
 * The methods {@link #getInt16(int)} etc. are protected. That should prevent erratic free accesses to data 
 * at application level. A derived class of this class structures the software for byte data access.
 * <br><br> 
 * It is able to support several kinds of structured data access:<ul>
 * <li>Simple C-like <code>struct</code> are adequate mapped with a simple derived class of this class, 
 *     using the protected commonly access methods like {@link #_getLong(int, int)} with predefined indexes 
 *     in special methods like getValueXyz().</li>
 * <li>Complex <code>struct</code> with nested <code>struct</code> inside are mapped 
 *     with one derived class per <code>struct</code>, define one reference per nested struct 
 *     and overwriting the method {@link #assignDataToFixChilds()}</li>
 * <li>Base <code>struct</code> inside a <code>struct</code> (inheritance in C) can be mapped with 
 *     extra derived classes for the base struct and usind the
 *     {@link assignCasted_i(ByteDataAccess, int)}-method.</li>
 * <li>A pack of data with several struct may be mapped using the {@addChild(ByteDataAccess)}-method.
 *     Thereby a parent should be defined, and the structs of the pack are children of this parent. 
 *     That structs need not be from the same type.</li>
 * <li>packs of struct with parent are nestable, it is constructable as a tree of pack of structs. 
 *     The parent of the pack is the tree node. It is likewise a XML tree. 
 *     The data may be also transformed to a XML data representation
 *     or the data structure may be explained with a XML tree, but they are not
 *     XML data straight.</li>
 * </ul>
 * This application possibilities show a capable of development system to access binary data. 
 * The other, hand made way was calculate with indices of the byte[idx] specially user programmed. 
 * This class helps to make such complex index calculation needlessly. 
 * One struct at C level corresponds with one derivated class of ByteDataAccess. 
 * But last also a generation of the java code from C/C++-header files containing the structs is able to.
 * So the definition of byte positions are made only on one source. The C/C++ is primary thereby. 
 *      
 * <h2>children, currentChild, addChild</h2>
 * Children are used to evaluate or write different data structures after a known structure. 
 * The children may be from several derived types of this class. 
 * With children and children inside children a tree of different data can be built or evaluated.
 * 
 * If no child is added yet, the indices have the following values:
 * <ul>
 * <li>idxCurrentChild = -1.
 * <li>idxCurrentChildEnd = index after known (head-) data.
 * </ul>
 * <ul>
 * <li>idxCurrentChild = the index after the last known child or known (head-) data.
 *     It is idxCurrentChildEnd from state before.    
 * <li>idxCurrentChildEnd = -2, because the length of the child is unknown. 
 *     The -2 is used to mark call of next().
 * </ul>
 * A call of {@link addChild()} or its adequate addChildXY() sets the indices to the given current child:
 * <ul>
 * <li>idxCurrentChild = the index after known (first head-) data, the index of the child.
 * <li>idxCurrentChildEnd = idxCurrentChild + {@link specifyLengthElement()} if this method returns >=0.     
 * <li>idxCurrentChildEnd = idxCurrentChild + {@link specifyLengthElementHead()} if this method returns >=0.     
 * <li>idxCurrentChildEnd = -1, because the length of the child is not known yet if both methods return -1. 
 * </ul>
 * The length of the current Child may be set while evaluating the child's data. 
 * The user should call {@link #setLengthElement(int)} with the child 
 * or {@link #setLengthCurrentChildElement(int)} with the parent, respectively with this.
 * <ul>
 * <li>idxCurrentChild = is still the index of the child.
 * <li>idxCurrentChildEnd = idxCurrentChild + given length.
 * </ul>
 * If this methods are not called, but next() or addChild...() is called however, without a known length 
 * but this (the parent) knows the rules to determine the length of its possible children, 
 * it is possible to do that. The method {@link specifyLengthCurrentChildElement()} supplied the number of bytes.
 * But if this method is not overwritten in the inherited class, an exception is thrown.
 * 
 * 
 * <br>
 * The UML structure of such an class in a environment may be shown with the
 * followed object model diagram, <br>
 * <code> <+>---> </code>is a composition,
 * <code> <>---> </code>is a aggregation, <code> <|---- </code>is a inherition.
 *  <pre>
 *                      +-------------------------------+                 +----------+
 *                      | ByteDataAccess                |----data-------->| byte[]   |
 *                      |-------------------------------|                 +----------+
 *                      |idxBegin:int                   |
 *                      |idxChild:int                   |<---------------+ a known parent
 *  +-------------+     |idxEnd:int                     |---parent-------+ setted in addChild()
 *  | derivated   |     |-------------------------------|
 *  | user        |---|>|specifyLengthElement()         |
 *  | classes     |     |specifyLengthElementHead()     |
 *  +-------------+     |specifyLengthCurrentChild()    |
 *                      +-------------------------------+
 * </pre>
 *
 */
public abstract class ByteDataAccess extends ByteDataAccessBase
{
  /**The version. 
   * <ul>
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
  public static final int _version_ = 0x20101231;
  
  
  /** Definition of the code of end of information, return from next()*/
  public static final byte kEndOfElements = 0;

  /** Definition of the code of no information, return from next()*/
  public static final byte kNothing = (byte)(0xff);

  /** Aussage: es ist ein String (XML: text()), kein Tag im String*/
  public static final byte kText    = 1;

  /** coding: the value is undefined*/
  public static final byte kUndefined  = -0x3f;

  
  /**Designation whether a instance is initialized with a local length.
   * See {@link #ByteDataAccess(int, int)}.
   */
  protected static final int kInitializedWithLength = -3;
  
  /** Index in the data, position of element code*/
  protected static final int kIdxElementCode = 0;


  /** Constructs a new empty instance. Use assign() to work with it. */
  protected ByteDataAccess()
  { super(-1);    //sizeHead is unknown, get it from specifyLengthElementHead()
    this.data = null;
    this.bBigEndian = false;
    bExpand = false;
    idxBegin = 0;
    idxEnd = 0;
    idxFirstChild = 0;
    idxCurrentChild = -1;  //to mark start.
    idxCurrentChildEnd = 0;
    parent = null;
    //currentChild = null;
    //charset = Charset.forName("ISO-8859-1");  //NOTE: String(..., Charset) is only support from Java 6
    charset = "ISO-8859-1";
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
  protected ByteDataAccess(int sizeHead, int sizeData){
    super(sizeHead, sizeData);
    idxCurrentChildEnd = kInitializedWithLength;
  }


  /** Sets the elements data to the default empty data.
   * This method should not called outside of this base class, likewise not in the derived class itself.
   * This method is only called inside this base class itself inside the methods addChild() and assignEmpty().
   * But only the derived class knows how to set empty data.
   * It must specify that like the followed sample:
   * <pre>
   *protected void specifyEmptyDefaultData()
   *{ data[idxBegin + 0] = 0;
   *  data[idxBegin + kIdxMid] = 0;
   *  data[idxBegin + kIdxMin] = kNotAValue;
   *  data[idxBegin + kIdxMax] = kNotAValue;
   *}
   *</pre>
   * The default implementation is empty.
   */
  protected abstract void specifyEmptyDefaultData();



  /**Specifies the length of the head data of this element. This is the index of the first child relative to the start position of the element.
   * If the element has not specific head data 0 should returned. The head data are a constant value often. Only if the head is a data-content-depending size,
   * it is not constant.
   * <br><br>
   * This method is called inside this base class itself inside some methods. Outside the method may be called
   * for example to calculate the size of data.</br>
   * Only the derived class knows the position of the first child element.
   * It must specify that like the followed sample:
   * <pre>
   *protected void specifyLengthElementHead()
   *{ return kIdxFirstChild;  //kIdxFistChild should be defined as static final int.
   *}
   *</pre>
   *  @return Position of the first child element relative to the start position.
   */
  public abstract int specifyLengthElementHead();


  /** Returns the actual length of the whole data presenting with this element.
   * This method should be specified in the derived class.
   * The method is called inside this base class in the methods assignAsChild()
   * to calculate the {@link idxEnd}.
   * <br>
   * The calculation of the whole length of the element should be considered
   * all existing children inside this element. The structure is user-specific. Only some examples
   * may be given here:
   * <ul>
   * <li>If all children have a constant length and the number of children is known for example in head data,
   *   the calculation of the length is simple: <code>return lengthOfHead + getInt16(kNrofChildren) * lengthOfChilds</code>.
   *   whereby <code>kNrofChildren<code> is the index in the head.
   * <li>If the children have a variably length, all children should be checked about its length and the length should be accumulated.
   * <li>If the children have different length, but the length of all children is contained in the head, it is simple:
   *   For example the length of the head and all child is written in the first 2 bytes of the head, then
   *   <code> return getInt16(0)</code>
   * </ul>
   *
   * @return The length of this existing element or -1, if the length is not fix yet.
   * @throws IllegalArgumentException If the data inside are corrupted, the user can throw this exception.
   */
  protected abstract int specifyLengthElement()
  throws IllegalArgumentException;



  /** Notifies, that a child is added. This method may be overload,
   * if the user must take somme actions, count the number of childs or others.
   */
  protected void notifyAddChild()
  { //in default, do nothing with this.
  }


  /** Returns the length of a child element at current position specified in  the derivated class.
   * The derivated class must known its possible child elements and must get there length
   * with enquiry of specifyLengthElement() of the current child types.
   * This method is called inside the getLengthCurrentChildElement()-method
   * esspecially called inside the next()-method to increment the index idxChild.
   * This method is not called if setLengthCurrentChildElement() was called after calling next().
   * The user may precluding the call of this method if he calls setLengthCurrentChildElement in sequence
   * of the followed sample:
   * <pre>
   * int eChildCode = dataCode.next();
   * switch(eChildCode)
   * { case ...:
   *   { child.assignAsChild(dataCode);             //the child is known yet!
   *     dataCode.setLengthCurrentChildElement(child.getLength());
   *     ....
   *   }
   * <pre>
   * If the user always calls setLengthCurrentChildElement in this manner,
   * he don't need to overwrite specifyLengthCurrentChildElement.
   * @return The length of the existing child element at the current position idxChild.
   */
  protected int specifyLengthCurrentChildElement()
  throws IllegalArgumentException
  { throw new IllegalArgumentException("The length of the child is undefined, no user specification is known");
  }


  /**This method is called inside all assign...() methods. 
   * It should be overridden by a users derivation if any fix childs are present. 
   * In the overridden routines the method {@link assignDataAtIndex(byte[], int)} should be called for the fix childs.
   * example:<pre>
   * @Override protected void assignDataToFixChilds()
   * { myFixChild.assignDataAtIndex(super.data, super.idxBegin +kIdxmyFixChild);
   * } 
   * <pre>
   * Note that the indices are relative to idxBegin. use <code>super.</code> because an own element data or idxBegin may be present.
   * <br>
   * The originally implementation is empty.
   */
  protected void assignDataToFixChilds() throws IllegalArgumentException
  {
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
  protected final void reset(int lengthHead, int lengthData){
    int lengthHeadSpecified = lengthHead < 0 ? specifyLengthElementHead() : lengthHead;
    if(lengthData <= 0){
      specifyEmptyDefaultData();
    }
    super.setSizeHead(lengthHeadSpecified);
    super.clear(lengthData);
  }

  
  /**Assigns new data to this element at given index in data. 
   * This method is called on {@link #addChild(ByteDataAccess)}.
   * <br>
   * The user may overwrite this method and call super.assignData(data, length) inside
   * if some additional actions should be done.
   * <br/>
   * This method is also usefull to deassign a current data buffer, call <code>assign(null, 0);</code>.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * @param data The data. The length of data may be greater as
   *             the number of the significant bytes.
   * @param lengthHead number of bytes for the head.
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
  public final void assignData(byte[] dataP, int lengthHead, int lengthData, int index) 
  throws IllegalArgumentException
  { this.data = dataP;
    if(index < 0)
    { throw new RuntimeException("idx have to be >=0");
    }
    int lengthHeadSpecified = lengthHead < 0 ? specifyLengthElementHead() : lengthHead;
    if(lengthData <= 0){
      specifyEmptyDefaultData();
    }
    super.setSizeHead(lengthHeadSpecified);
    super.assign(dataP, lengthData, index);
    assignDataToFixChilds();
  }
  
  
  
  /**Assigns data without a given length of head. The length of the head is gotten
   * with call of the overridden method {@link #specifyLengthElementHead()} of the users class.
   * It calls {@link #assignData(byte[], int, int, int)} with lengthHead = -1.
   * @param data
   * @param lengthData
   * @param index
   * @throws IllegalArgumentException
   */
  @Java4C.inline
  public final void assignData(byte[] data, int lengthData, int index) 
  throws IllegalArgumentException
  { assignData(data, -1, lengthData, index);
  }
  
  
  

  /**Assigns new data to this element. <br>

   * The user may overwrite this method and call super.assignData(data, length) inside
   * if some additional actions should be done.<br/>
   * This method is also usefull to deassign a current data buffer, call <code>assign(null, 0);</code>.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * @param data The data. The length of data may be greater as
   *             the number of the significant bytes.
   * @param length Number of significant bytes in data.
   *               If length is > data.length, an exception may be thrown
   *               in any state of the evaluation.
   * @throws IllegalArgumentException if the length is > data.length
   */
  @Java4C.inline
  public final void assignData(byte[] data, int length) 
  throws IllegalArgumentException
  { if(length == 0)
    { length = -1;  //because elsewhere an exception is thrown. At least the head length are present.
    }
    assignData(data, length, 0);  
  }
  
  
  
  
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
  final public void assignEmpty(byte[] data) 
  { try{ assignData(data, -1, 0);} catch(IllegalArgumentException e){}

    specifyEmptyDefaultData();  //overrideable
  }

  
  
  
  
  /**Older form, see protected method {@link assignCasted_i(ByteDataAccess, int )}
   * If a cast is possible, it should be programmed in the derivated class.
   * @ deprecated because it is not necessary it's a downcast, it may be also upcast or sidecast. 
   * @param input
   * @throws IllegalArgumentException
   */
  @Java4C.inline
  final protected void assignDowncast_i(ByteDataAccess input)
  throws IllegalArgumentException
  { assignCasted_i(input, 0, -1);
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
  final public void assignAtIndex(int idxChildInParent, int lengthChild, ByteDataAccess parent)
  throws IllegalArgumentException
  { super.assignAt(idxChildInParent, lengthChild, parent);
  }

  

  /**assigns the element to the given position of the parents data to present a child of the parent.
   * The length of the child is limited to TODO the length of head - or not limited.
   * @param parent The parent. It should reference data.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  final public void assignAtIndex(int idxChildInParent, ByteDataAccess parent)
  throws IllegalArgumentException
  { @SuppressWarnings("unused")
    int lengthHead = getLengthHead();
    int lengthData;
    if(idxCurrentChildEnd ==kInitializedWithLength){
      lengthData = idxEnd;
    } else {
      lengthData = -1;  //unknown
    }
    assignData(parent.data, lengthHead, lengthData, parent.idxBegin + idxChildInParent);
    setBigEndian(parent.bBigEndian);
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
  final public boolean addChild1(ByteDataAccess child) 
  throws IllegalArgumentException
  { notifyAddChild();
    int sizeChildHead, sizeChild;
    if(child.idxCurrentChildEnd == kInitializedWithLength){
      //initialized child with its local length:
      child.setSizeHead(child.idxFirstChild);
      sizeChild = child.idxEnd;
    } else {
      //uninitialized child with length, or reused child:
      sizeChild = -1;
      child.setSizeHead(child.specifyLengthElementHead());
    }
    return super.addChild(child, sizeChild);
  }

  

  /**assigns the element to the current child position of parent,
   * to represent the current child of the parent.
   * This method should be used by reading out byte[] data 
   * if it is detected, that the content of data matches of this derivated type of
   * ByteDataAccess. {@link next()} should be called before. The pattern of using is:
   * <pre>
   * 
    int nWhat;    //code of the element
    while( (nWhat = dataElement.next()) != ByteDataAccess.kEndOfElements )
    { switch(nWhat)  //test the first byte of the current element
      { case ByteDataAccess.kText:
        { sText = dataElement.getText();
        } break;
        case code1:
        { code1Element.assignAsChild(dataElement);
          evaluateValue(code1Element);
        } break:
        case code2:
        { code1Element.assignAsChild(dataElement);
          evaluateValue(code1Element);
        } break:
        default: throw new IllegalArgumentException("unknown Element", dataElement);
      }
    }
    </pre>
   * The difference to {@link addChild(ByteDataAccess)} is: addChild() is used 
   * to writeout to data, addChild() appends the child always after idxEnd,
   * but this method is used to read from data and appends the child at position of the current child.
   * <br>
   * The data reference is copied, the idxBegin of this element
   * is set to the idxChild of parent, it is the current child position.
   * All other indices are setted calling {@link specifyLengthElementHead()}: idxChild
   * and {@link specifyLengthElement()}: idxEnd.
   * The idxChildEnd of parent is setted, so calling next() after this operation
   * increments in data after this new child.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * @param parent The parent. It should reference data, and a current child position
   *        should be set by calling next() before. See sample at {@link next()}.
   * @throws IllegalArgumentException If the data are wrong. The exception is thrown
   *        orginal from {@link specifyLengthElement()}.
   * @deprecated use addChild()
   */
  @Java4C.inline
  @Deprecated
  final public void assignAsChild(ByteDataAccess parent)
  throws IllegalArgumentException
  { parent.addChild(this);
  }


  ////
  
  
  /**Adds a child Element at current end of data to write data. 
   * The child's data are initialized with call of <code>child.specifyEmptyDefaultData().</code>
   *
   * @param child The child will associated to this and should be used
   *              to add some content.
   * @throws IllegalArgumentException 
   */
  @Java4C.inline
  final public void addChildEmpty(ByteDataAccess child) 
  throws IllegalArgumentException
  { addChild(child);
    child.specifyEmptyDefaultData();
  }
  
  



  
  

  /** Returns the length of the current child element.
   * The length may be setted outside by calling setLengthCurrentChildElement()
   * from user level after any calling of next()
   * or after calling getText() if it is a text content.
   * In this case this method returns this length in a simple way. <br>
   * If the user don't have called setLengthCurrentChildElement() after the last next(),
   * the users defined specifyLengthCurrentChildElement() is called.
   * @return the length in bytes of the current child element.
   * @throws IllegalArgumentException if the user has not defined a overloaded methode specifyLengthCurrentChildElement()
   *                              or this method has thrown the exception because the length is not determinable.
   */
  final public int getLengthCurrentChildElement()
  throws IllegalArgumentException
  {
    if(idxCurrentChildEnd > idxCurrentChild)
    { //a get method, especially getText() was called,
      //so the end of the child is known yet, use it!
      return idxCurrentChildEnd - idxCurrentChild;
    }
    /*changed: it cannot be assumed that the coding use textbytes!
     * If they are textbytes, the user has called getText normally,
     * therefor the idxChildEnd is setted there.
    else if(isTextByte(data[idxChild]))
    { //text bytes are overreaded:
      idxChildEnd = idxChild;
      do
      { idxChildEnd +=1;
      } while(idxChildEnd < idxEnd && isTextByte(data[idxChildEnd]));
      return idxChildEnd - idxChild;
    }
    */
    else
    { //only the user can define the length.
      return specifyLengthCurrentChildElement();
    }
  }



  /** Sets the length of the current child element after calling next().
   *  The user may set the length due to the knowledge of the type and content of the actual child element.
   *  So the calling of specifyLengthCurrentChildElement() will be precluded.
   *  The idxChildEnd is setted.
  final public void setLengthCurrentChildElement(int lengthOfCurrentChild)
  { if(currentChild != null)
    { currentChild.setLengthElement(lengthOfCurrentChild);
    }
  }
   */


  /** Expands the end index of the parent, it means the management
   * of the expanse of the data.
   * @deprecated. 
   */
  @Deprecated final public void expandParent()
  throws IllegalArgumentException
  { if(idxBegin == 0 && parent == null)
    { //it is the top level element, do nothing
    }
    else if(parent != null)
    { if(parent.idxEnd < idxEnd)
      { parent.idxEnd = idxEnd;
      }
      ((ByteDataAccess)parent).expandParent();
    }
    else throw new IllegalArgumentException("invalid expandParent()");
  }


  

/** Counts the idxChild by given index, idxChild is ByteCount from idxBegin
 * 
 * @param indexObjectArray Index of Array
 */
  //@Java4C.inline: don't set inline because it contains call of virtual methods. 2014-08 
  public final void elementAt(int indexObjectArray) {
    idxCurrentChild = idxBegin + specifyLengthElementHead() + specifyLengthCurrentChildElement() * indexObjectArray;
  }


  /*
  public final ByteDataAccess getCurrentChild() 
  {
     
      return currentChild;
  }
  */

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
      toStringformatter.addint(idxBegin, "33331").add("..").addint(idxFirstChild,"333331").add(":");
      int nrofBytes = idxFirstChild - idxBegin;
      if(nrofBytes > 16){ nrofBytes = 16; }
      if(nrofBytes <0){ nrofBytes = 4; }
      if(idxBegin + nrofBytes > data.length){ nrofBytes = data.length - idxBegin; }  
      toStringformatter.addHexLine(data, idxBegin, nrofBytes, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
      toStringformatter.add(" child ").addint(idxCurrentChild,"-3331").add("..").addint(idxCurrentChildEnd,"-33331").add(":");
      if(idxCurrentChild >= idxBegin)
      { 
        nrofBytes = idxCurrentChildEnd - idxCurrentChild;
        if(nrofBytes > 16){ nrofBytes = 16; }
        if(nrofBytes <0){ nrofBytes = 4; }
        if(idxCurrentChild + nrofBytes > data.length){ nrofBytes = data.length - idxBegin; }  
        toStringformatter.addHexLine(data, idxCurrentChild, nrofBytes, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
      }
      final String ret = toStringformatter.toString();
      return ret;
    }  
  }
  

  
}
