package org.vishia.util;

/**This is a universal interface to mark instances as select-able and to support selection. 
 * It supports up to 32 sources or users for selection. The management of users should be defined
 * at user level. A source is represented by a bit in the 32-bit-value of the mask.
 * Usual only one source for selection may existing. 
 * It is possible that two or less more sources do a selection independently.
 * 
 * The class {@link SelectMask} is the standard implementation. A derived class can inheritance 
 * from that class if another superclass isn't necessary. This may be usual for small data classes.
 *  
 * @author Hartmut Schorrig
 *
 */
public interface SelectMask_ifc
{
  /**Returns the selection mask of this object. If the returned value is 0, the object is not selected.
   * If it is not 0, any source (represented by each bit) has selected this source. 
   * @return bits of selection, 0 if it is not selected.
   */
  public int getSelection();
  
  
  /**Removes the selection of the object for the given source.  
   * @param mask The bit which presents the source.
   * @return The select mask before this action is done. 
   */
  public int setDeselect(int mask);
  
  /**Sets the selection of the object for the given source.  
   * @param mask The bit which presents the source.
   * @return The select mask before this action is done. If it is 0, this source is the only one which has select the object. 
   */
  public int setSelect(int mask);

}
