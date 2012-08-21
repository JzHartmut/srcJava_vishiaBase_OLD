package org.vishia.util;

/**This is a standard implementation of the universal interface to mark instances as select-able and to support selection. 
 * It supports up to 32 sources or users for selection. The management of users should be defined
 * at user level. A source is represented by a bit in the 32-bit-value of the mask.
 * Usual only one source may existing.
 * 
 * This class can be used either as super class for anything which should be marked as selected
 * or as composition in that class whereby the data item {@link #selectMask} is the only one data of that composition.
 * 
 * @author Hartmut Schorrig
 *
 */
public class SelectMask implements SelectMask_ifc
{
  int selectMask;
  
  @Override public int getSelection()
  { return selectMask; }
  
  @Override public int setDeselect(int mask)
  { int selectMask1 = selectMask;
    selectMask &= ~mask;
    return selectMask1;
  }
  
  @Override public int setSelect(int mask)
  { int selectMask1 = selectMask;
    selectMask |= mask;
    return selectMask1;
  }
}
