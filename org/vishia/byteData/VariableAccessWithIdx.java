package org.vishia.byteData;

/**This class supports the access to any type of variable with given indices and bit masks.
 * The indices and the bit mask are not part of a variable because a variable is an entity to
 * access from any target system. A variable is not a proper entity to get any value. 
 * Reason: If some bits should be read from a variable, and then other bits should be read,
 * the variable should be gotten from the target device only one time. Therefore it is only one
 * variable. But the access to several bits - or indices are described here.
 * @author Hartmut Schorrig
 *
 */
public class VariableAccessWithIdx
{
  
  
  /**Version, history and license
   * <ul>
   * <li>2012-03-31 Hartmut created. The problem was: In a {@link org.vishia.gral.ifc.GralWidget}
   *   the element {@link #idx} was contained but not any mask. The mask will be used now. But indices
   *   and masks are not a problem of the widget in a GUI, it is a problem of access to the variable.
   *   But the implementation of {@link VariableAccess_ifc} should not containt this things
   *   because a variable is an entity of access to a target device data element.
   *   Therefore this meta class should be proper.
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
   * 
   */
  public static final int version = 20120331;

  public VariableAccessWithIdx(VariableAccess_ifc variable, int[] idx, int bit, int mask){
    this.variable = variable;
    this.ixArray = idx;
    this.bit = bit;
    this.mask = mask;
  }
  
  public VariableAccessWithIdx(VariableAccess_ifc variable, int[] idx){
    this(variable, idx, 0, -1);
  }
  
  public VariableAccessWithIdx(VariableAccess_ifc variable){
    this(variable, null, 0, -1);
  }
  
  public VariableAccessWithIdx(VariableAccess_ifc variable, int bit, int mask){
    this(variable, null, bit, mask);
  }
  
  /**The entity of a variable. */
  protected final VariableAccess_ifc variable;
  
  /**Array of indices to access if the variable has more as one dimension.
   */
  protected final int[] ixArray;
  
  /**The mask to access to some  bits. The mask is shifted to right with bit.
   * For example if bit 3..1 should be gotten, mask = 0x7 and bit=1. It is -1 (all bits set)
   * if the value should not be mask.
   */
  protected final int mask;
  
  /**The position of the last significant bit if the value of the variable is placed in some bits
   * or specially in only one bit, a boolean value. 0 per default.
   */
  protected final int bit;

  public double getDouble(){ return variable.getDouble(ixArray); }

  public float getFloat(){ return variable.getFloat(ixArray); }

  public String getString(){ return variable.getString(ixArray); }

  public char getType(){ return variable.getType(); }

  /**Returns the integer representation of the value of the variable.
   * Note: If the variable itself is a byte or 16-bit-type, only 1 or 2 bytes are gotten from the variable
   * respectively the index is regarded to the length of the variable.
   * Note: The bit and mask will be regarded.
   * @return The value
   */
  public int getInt(){ 
    int value = variable.getInt(ixArray);
    int bValue = (value >> bit) & mask;
    return bValue;
  }

  
  public void setString(String src){ variable.setString(src, ixArray); }
  
}
