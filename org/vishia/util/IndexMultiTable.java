package org.vishia.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.vishia.bridgeC.AllocInBlock;



/**This class contains sorted references of objects (values) with a sorting key (usual String or Integer) 
 * in one or more tables of a defined size. 
 * <ul>
 * <li>This class can be used similar like {@link java.util.TreeMap}. 
 *   In opposite to a {@link java.util.TreeMap} the key need not be unique for all values.
 *   More as one value can have the same key. The method {@link #get(Object)} defined in 
 *   {@link java.util.Map} searches the first Object with the given key. 
 *   The method {@link #put(Comparable, Object)} replaces an existing value with the same key like defined in Map
 *   where the methods {@link #add(Comparable, Object)}, {@link #addBefore(Comparable, Object, Object)}
 *   and {@link #append(Comparable, Object)} puts the new value with an existing key beside the last existing one.  
 * <li>There is a method {@link #iterator(Comparable)} which starts on the first occurrence of the search key
 *   or after that key which is one lesser as the key.
 *   It iterates both to the end or backward of the whole collection. The user can check the key in the returned {@link java.util.Map.Entry}
 *   whether the key is proper. In this kind a sorted view of a part of the content can be done.
 * <li>The container consist of one or more instances of tables which have a limited size. If a table in one instance is filled,
 *   two new instances will be created in memory which contains the half amount of elements and the original
 *   instance is changed to a hyper table. The size of the instances are equal and less. It means, the search time
 *   is less and the memory requirement is constant. In this kind this class is able to use in C programming
 *   with a block heap of equal size blocks in an non-dynamic memory management system (long living
 *   fast real time system).    
 * </ul>
 * The inner class {@link Table} contains the table of objects and the table of key of type Comparable. 
 * The tables are simple arrays of a fix size.
 * <br>
 * An instance of Table class may be either a hyper table which have some children, 
 * or it is a end-table of keys and its references.
 * A end table may have a parent, which is a hyper table, and some sibling instances.
 * If there isn't so, only the end-table exists. It is, if the number of objects is less than 
 * the max capacity of one table, typical for less data. 
 * In that case, the data structure is very simple. Commonly a search process should regard 
 * the tree of tables. But the tree is not deep, because it is a square function. 
 * With a table of 100 entries, a 2-ary tree may contain up to 10000.
 * <br>
 * Objects with the same key can stored more as one time.
 *   
 * <br> 
 * This concept have some advantages:
 * <ul>
 * <li>A system of simple arrays for the key and a parallel array of the associated object
 *     allows fast search using java.util.Arrays.binarysearch(int[], int). 
 *     It is a simple algorithm. Only less memory objects are needed. 
 *     It doesn't need nodes for TreeMap etc. 
 *     It is a simple layout for fast embedded control applications. 
 * <li>But using only one array of keys and one array of objects, 
 *   <ul>
 *   <li>first, the number of objects are limited or a large portion of memory should be provided,
 *   <li>second, if a new object is inserted, and the amount of objects is large, 
 *       a larger calculation time of System.arraycopy() would be necessary.
 *   </ul>
 * <li>Therefore the portioning in more as one table needs no large memory objects. 
 *     If the number of objects is increased, a additional new memory object with limited size 
 *     is necessary - no resize of existing object.
 * <li>The calculation time for insertion an object is limited because one table is limited,
 *     also if the number of objects is large.
 * <li>If thread safety is necessary, only that table should be locked or exclusive copied, 
 *     which is touched, for a limited time too. So the number of clashes is less. 
 *     In the time this class doesn't support thread safety, but it is planned in two ways, 
 *     using synchronized and using the lock free atomic mechanism. 
 * <li>Using lock free programming, the change of data should be done in an copy of the table.
 *     This copy have less memory size.
 * <li>The limited number of memory space is convenient for the application in fast real time systems.
 * </ul>                     
 * 
 * @author Hartmut Schorrig
 *
 * @param <Type>
 */
@SuppressWarnings("synthetic-access") 
public class IndexMultiTable<Key extends Comparable<Key>, Type> 
implements Map<Key,Type>, Iterable<Type>  //TODO: , NavigableMap<Key, Type>
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-07-09 Hartmut chg: All methods are synchronized now because instances are used in more as one thread usual.
   *   The non-synchronized variant may be a special case. It has some more less calculation time.
   *   Idea: Offer non-synchronized methods with special name to save calculation time for applications
   *   which's instances are guaranteed not used in more as one thread.    
   * <li>2015-07-05 Hartmut bugfix: The calculation of {@link Table#sizeAll} was faulty. {@link Table#check()} improved,
   *   it checks the sizeAll and checks all child tables. Calculation of sizeAll improved. 
   * <li>2015-03-07 Hartmut bugfix false instanceof check, sub-tables has not worked.
   * <li>2014-12-21 Hartmut chg structure: Now a {@link Table} is an own instance. Improved and simplified. 
   * <li>2014-12-20 Hartmut new The iterator is a ListIterator now with {@link ListIterator#hasPrevious()} etc.
   *   The advantage and usage: Set the iterator to a expected point with {@link #iterator(Comparable)} and traverse back- and forward.
   *   There are some changes in algorithm. 
   * <li>2014-01-12 Hartmut chg: {@link #sortin(int, Comparable, Object)} new Algorithm
   * <li>2014-01-12 Hartmut chg: toString better for viewing, with all keys 
   * <li>2013-12-02 Hartmut new: Implementation of {@link #remove(Object)} was missing, {@link #searchInTables(Comparable, boolean, IndexBox)}
   *   restructured. It returns the table and index, able to use for internal searching. 
   * <li>2013-09-15 Hartmut new: Implementation of {@link #delete(int)} and {@link EntrySetIterator#remove()}.
   * <li>2013-09-15 Hartmut chg: rename and public {@link #search(Comparable, boolean, boolean[])}.  
   * <li>2013-09-07 Hartmut chg: {@link #put(Comparable, Object)} should not create more as one object
   *   with the same key. Use add to do so.
   * <li>2013-08-07 Hartmut improved.
   * <li>2013-04-21 Hartmut created, derived from {@link IndexMultiTableInteger}.
   * <li>2009-05-08 Hartmut corr/bugfix: IteratorImpl(IndexMultiTableInteger<Type> firstTable, int startKey) used in iterator(startkey):
   *                    If a non-exakt start key is found, the iterator starts from the key after it. 
   *                    If no data are available, hasnext() returns false now. TODO test if one hyperTable contains the same key as the next, some more same keys!
   * <li>2009-04-28 Hartmut new: iterator(key) to start the iterator from a current position. It is the first position with the given key. 
   *                    corr: Some empty methods are signed with xxxName, this methods were come from planned but not implemented interface
   *                    meditated: implement ListInterface instead Interface to supply getPrevious() to iterate starting from a key forward and backward.
   *                    corr: binarySearch self implemented, regarding the first occurrence of a key.
   * <li>2009-04-26 Hartmut meditated: Several key types in one file isn't good! The search routines are optimal only if the key type is fix.
   *                                There are some adequate classes necessary for int, long keys.
   *                    docu
   *                    chg: Using of class AllocInBlock to get the size of a block.
   *                    corr: Now more deepness as 2 tables is programmed and tested. Older versions support only 2 tables.
   *                          It was 1000000 entries max. with a table size of 1000.
   * <li>2009-03-01 Hartmut new: IndexMultiTableInteger(int size, char type): in preparation of using, functionality not ready yet.
   *                    planned: Type may be int, long, String, size are able to choose. The arrays should be assigned not as embedded instances in C. 
   *                    new: method get() does anything, not tested in all cases.
   * <li>2007-06-00 Hartmut Created.                  
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
   * 
   */
  public static final String sVersion = "2014-12-24";

  private enum KindofAdd{ addOptimized, addLast, addBefore, replace};
  
  /**This class is the Iterator for the outer class. Every {@link #iterator()}-call
   * produces one instance. It is possible to create some instances simultaneously.
   * The iterator iterates over all values in all tables, independent of their keys.
   * But because the values are sorted with the keys, the values are gotten in its key-sorted order.
   *
   * @param <Type>
   */
  protected final class IteratorImpl implements ListIterator<Type>
  {
  
    /**The helperNext and helperPrev contain the values of the iterator. Because there is a tree of tables.
     * The current IteratorHelpers of the current table depths is referenced.
     * A IteratorHelper contains a parent and child reference. If the end of table
     * of the current depth level is reached, the parent of the current helperNext is referenced here
     * than, the next table is got, than the child is initialized and referenced here.  
     * There are 2 helpers necessary because next and previous can have different tables.
     */
    IndexMultiTable.IteratorHelper<Key, Type> helperPrev, helperNext;
    
    /**Comparison of this value with {@link IndexMultiTable#modcount} to detect changes. */
    private final int modcount;
    
    /**Information whether {@link #next()} or {@link #previous()} was invoked at last.
     * It is important for the functionality of {@link #remove()}, {@link #add(Object)} and {@link #set(Object)}
     * If {@link #next()} was called at last, the last element or insertion point is that element which is referred by {@link #helperPrev}:
     * <br>
     * Before next():
     * <pre>
     *         prev--+      +--next
     *               |  ,   |
     *      [1]     [2]    [3]    [4]
     *                      |  ^   |
     *                prev--+      +--next
     * </pre>       
     * <b>next()</b> has returned [3].       
     * The {@link #remove()} operation has to remove the element at position 3, which is referred yet as the previous. 
     * The operation {@link #set(Object)} should be changed the element at position [3].
     * <br><br>
     * <be>previous()</b> has returned [2], after them:
     * <pre>
     *         prev--+      +--next
     *               |  ,   |
     *       [1]    [2]    [3]    [4]
     *        |  ^   |
     *  prev--+      +--next
     * </pre>       
     * The element [2] which is yet referred from {@link #helperNext} should be deleted or after [2] is to change.
     */
    private boolean bLastWasNext = false;

    /**The value which is returned by the last {@link #next()} and {@link #previous()}. */
    Key lastKey; Type lastValue;
    
    
    //private Type lastReturnedElement;
    
    /**Creates the iterator before the first element. 
     * @param firstTable
     */
    IteratorImpl()
    { this.modcount = IndexMultiTable.this.modcount;
      helperNext = new IteratorHelper<Key, Type>(false);
      helperNext.table = root;
      helperNext.idx = 0;
      helperNext.checkHyperTable();  //maybe create sub tables.
      //lastkeyNext = bHasNext ? helperNext.table.aKeys[helperNext.idx] : null;
      
      helperPrev = new IteratorHelper<Key, Type>(true);
      helperPrev.table = root;
      helperPrev.idx = -1;
      helperPrev.currKey = null;
      helperPrev.currValue = null;
      //bHasPrev = false;  //the first left.
      //lastkeyNext = null;
      //lastkeyPrev = null;
    }
    
    
    /**Ctor for the Iterator which starts on any position inside the table.
     * The previous element {@link #previous()} is any element which's key is lesser or equal the given key.
     * The next element {@link #next()} is any element which's key is greater the given key.
     * The previous element is null {@link #hasPrevious()} == false if no element is found which is lesser or equal the given key.
     * The next element is null, {@link #hasNext()} == false if not element is found which's key is greater. 
     * @param firstTable The root table.
     * @param startKey The key
     * 
     */
    IteratorImpl(Key startKey)
    { this.modcount = IndexMultiTable.this.modcount;
      Table<Key, Type> tableStart = root;
      while(tableStart.isHyperBlock)
      { //call it recursively with sub index.
        int idx = tableStart.binarySearchFirstKey(tableStart.aKeys, 0, tableStart.sizeBlock, startKey); //, sizeBlock, key1);
        if(idx < 0)
        { /**an non exact found, accept it.
           * use the table with the key lesser than the requested key
           */
          idx = -idx-1; //insertion point is index of previous
        }
        assert(tableStart.aValues[idx] instanceof Table);
        @SuppressWarnings("unchecked") 
        Table<Key, Type> tableNext = (Table)tableStart.aValues[idx];
        tableStart = tableNext;
      }
      helperPrev = new IteratorHelper<Key, Type>(true);
      helperNext = new IteratorHelper<Key, Type>(false);
      helperPrev.table = helperNext.table = tableStart;
      
      int idx = tableStart.binarySearchFirstKey(tableStart.aKeys, 0, tableStart.sizeBlock,  startKey); //, sizeBlock, key1);
      if(idx < 0)
      { /**an non exact found, accept it.
         * start from the element with first key greater than the requested key
         */
        idx = -idx-1;  //it is the position of the next element.
        helperPrev.idx = idx-1;
        helperPrev.checkHyperTable();
        helperNext.idx = idx;
        helperNext.checkHyperTable();
        
      } else {
        //exact found, it is the previous to add new elements with the given key after that one with the same name.
        helperPrev.idx = idx;  //it has correct parents, it is not a hyper table.
        helperPrev.currKey = helperPrev.table.aKeys[idx];
        helperPrev.checkHyperTable();
        helperNext.idx = idx+1;
        helperNext.checkHyperTable();  //may be a hyper table, search the first not hyper.
        
      }
    }
    
    
    @Override public boolean hasNext() { return helperNext.currKey !=null; }
  
    
    @Override public boolean hasPrevious() {  return helperPrev.currKey !=null; }
  
    
    
    
    
    /**Implements the standard behavior for {@link java.util.Iterator#next()}.
     * For internal usage the {@link #helperNext} is set.
     * With them 
     */
    @SuppressWarnings("unchecked")
    @Override public Type next()
    { checkForModification();
      bLastWasNext = true;
      lastKey = helperNext.currKey;
      lastValue = helperNext.currValue;
      if(lastKey !=null) { //only if has next
        next_i();  //executes always next after last next.
      }
      return lastValue;
    }
  
    /**Implements the standard behavior for {@link java.util.Iterator#next()}.
     * For internal usage the {@link #helperNext} is set.
     * With them 
     */
    @SuppressWarnings("unchecked")
    @Override public Type previous()
    { checkForModification();
      bLastWasNext = false;
      lastKey = helperPrev.currKey;
      lastValue = helperPrev.currValue;
      if(lastKey !=null) { //only if have previous
        prev_i();  //executes always previous after last previous.
      }
      return lastValue;
    }
    
    
    @Override public int nextIndex() { return 0; }
    
    @Override public int previousIndex() { return 0; }
    
    @Override public void add(Type e) { throw new IllegalStateException("not implemented"); }
    
    
    
    /**Sets a new value to the given key!
     * @param value
     */
    @Override public void set(Type value) { throw new IllegalStateException("not implemented"); }
    
  
    Key lastKey(){ return lastKey; }
    
    
    private void checkForModification() {
      if(this.modcount != IndexMultiTable.this.modcount) throw new ConcurrentModificationException();
    }
    
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helperNext is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    //@SuppressWarnings("unchecked")
    private void next_i()
    { checkForModification();
      //the yet next is the new previous:
      helperPrev.copy(helperNext);
      if( ++helperNext.idx >= helperNext.table.sizeBlock) {  //next in current table.
        //The table is left.
        helperNext.checkHyperTable();
      } else {
        //element in the same table:
        helperNext.currKey = helperNext.table.aKeys[helperNext.idx];
        @SuppressWarnings("unchecked") 
        Type value = (Type)helperNext.table.aValues[helperNext.idx];
        helperNext.currValue = value;
        
      }
      
    }
    
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helperNext is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    //@SuppressWarnings("unchecked")
    private void prev_i()
    { checkForModification();
      //the yet next is the new previous:
      helperNext.copy(helperPrev); 
      if(--helperPrev.idx < 0) {  //next in current table.
        //The table is left.
        helperPrev.checkHyperTable(); 
      } else {
        //element in the same table:
        helperPrev.currKey = helperPrev.table.aKeys[helperPrev.idx];
        @SuppressWarnings("unchecked") 
        Type value = (Type)helperPrev.table.aValues[helperPrev.idx];
        helperPrev.currValue = value;
      }
    }
    
    
    public void remove()
    { checkForModification();
      if(bLastWasNext) {
        //because the cursor is set after the next, the previous is the candidate to remove. It was the next before.
        helperPrev.table.delete(helperPrev.idx);  //delete element in table.
        //to preserve the situation, the new previous is one before:
        helperPrev.idx -=1;  //may be negative now
        helperPrev.checkHyperTable();
        //the next is unchanged normally. But because it is on another position, seek it from previous: 
        helperNext.table = helperPrev.table;  //may be a changed table, sibling before, parent etc.
        helperNext.idx = helperPrev.idx+1;  //the next
        helperNext.checkHyperTable();
        //lastkeyNext = bHasNext ? helperNext.table.aKeys[helperNext.idx] : null;
        
      } else {
        //because the cursor is set before the previous, the next is the candidate to remove.
        helperNext.table.delete(helperNext.idx);  //delete element in table.
        //left helperNext.ix unchanged
        helperNext.checkHyperTable();  //maybe end of table, correct next
        //lastkeyNext = bHasNext ? helperNext.table.aKeys[helperNext.idx] : null;
        
      }
    }
  
    @Override public String toString() { return helperPrev.toString() + " ... " + helperNext.toString(); }   
  }

  /**This class contains the data for a {@link IndexMultiTable.IteratorImpl}
   * for one table.
   * 
   *
   */ 
  private static class IteratorHelper<Key extends Comparable<Key>, Type>
  {
    /**Dedication whether it is the instance for previous or for next. */
    private final boolean bPrev;
    
    /**Current index in the associated table for the next or previous entry. It is not the last returned one but the next or previous. */ 
    protected int idx;
    
    /**The associated table appropriate to the idx. */
    Table<Key, Type> table;
    
    /**The current key and value which will be returned from following next() or prev(). */
    Key currKey; Type currValue;
    
    IteratorHelper(boolean bPrev)
    { this.bPrev = bPrev;
      this.table = null;
      idx = -1;
    }
    
    void copy(IteratorHelper<Key, Type> src) {
      this.idx = src.idx;
      this.table = src.table;
      this.currKey = src.currKey;
      this.currValue = src.currValue;
    }
    
    
    /**Checks the helper's idx and table.
     * <ul>
     * <li>If the #idx is > {@link IndexMultiTable#sizeBlock} then use the next table, over parent.
     * <li>If the #idx is < 0 then use the previous table, from parent.
     * <li>If the idx refers a hypertable then use the first non-hypertable.
     * </ul>
     * @param helper
     * @param bPrev
     * @return maybe another table, maybe null if not previous or not next
     */
    private boolean checkHyperTable()
    {
      while(idx >= table.sizeBlock && table.parent !=null) { //on end of any table:
        idx = table.ixInParent +1;  //next entry in parent table
        table = table.parent;
      }
      while(idx < 0 && table.parent !=null) { //on end of any table:
        idx = table.ixInParent -1;  //previous entry in parent table
        table = table.parent;
      }
      while(table.isHyperBlock && idx >=0 && idx < table.sizeBlock) //check whether a hyperBlock is found:
      { //
        @SuppressWarnings("unchecked")
        Table<Key, Type> childTable = (Table<Key, Type>)table.aValues[idx];
        table = childTable;
        if(bPrev) {
          idx = childTable.sizeBlock-1;  //prev is executed.
        } else  {
          idx = 0;  //next is executed.
        }    
      }
      if( idx >=0 && idx < table.sizeBlock) {
        currKey = table.aKeys[idx];
        @SuppressWarnings("unchecked") 
        Type value = (Type)table.aValues[idx];
        currValue = value;
        return true;
      } else {
        currKey = null;
        currValue = null;
        return false;
      }
    }

    @Override public String toString() { return table == null ? "null" : idx < 0 ? "--no-previous--" : idx >= table.sizeBlock ? "--no-next--" : table.aKeys[idx].toString(); } 
  }

  
  /**One table contains keys and values in two array lists in sorted order.
   * A table can be a hyper table. The {@link Table#aValues} contains references to a sub table then. There is a tree of table.
   * The last element of the tree (leaf) is not a hyper-table. It contains instances of Type.
   *
   * @param <Key> Same like the {@link IndexMultiTable}
   * @param <Type>
   */
  static private class Table<Key extends Comparable<Key>, Type>
  {
    /**Array of all keys. Note: It is the first element to see firstly while debugging. */
    protected final Key[] aKeys; // = new Key[maxBlock];
  
    /**Array of objects appropriate to the keys. It is either a {@link Table} if this is a hyper-table, or a Type-instance */
    protected final Object[] aValues = new Object[maxBlock];
  
    /**actual number of objects stored in this table. */
    private int sizeBlock;

    /**actual number of leaf objects stored in the table tree of this and its children. */
    private int sizeAll;

    /**True, than {@link #aValues} contains instances of this class too. */
    protected boolean isHyperBlock;

    /**A identifier number for debugging.*/
    final int identParent = ++identParent_;

    /**Index of this table in its parent. */
    private int ixInParent;
   
    /**The parent if it is a child table. The parent is always a hypertable. */
    private Table<Key, Type> parent;

    /**Reference to the root data. */
    final IndexMultiTable<Key, Type> rootIdxTable;



    /**constructs an empty table. */
    Table(IndexMultiTable<Key, Type> root1)
    { //this(1000, 'I');
      this.rootIdxTable = root1;
      this.aKeys = root1.provider.createSortKeyArray(maxBlock);
      for(int idx = 0; idx < maxBlock; idx++){ aKeys[idx] = root1.maxKey__; }
      sizeBlock = 0;
      ixInParent = -1;
    }



    /**Binary search of the element, which is the first with the given key.
     * The algorithm is modified from the known algorithm (for example in copied from Arrays.binarySearch0(...))
     * The modification is done because more as one entry with the same key can be existing.
     * The algorithm searches the first occurrence of it. 
     * @param a The array
     * @param fromIndex start index, first position
     * @param toIndex end index. exclusive last position in a
     * @param key search key
     * @return index in a where the key is found exactly the first time (lessest index)
     *   or negative number: insertion point index is (-return -1)
     */
    int binarySearchFirstKey(Comparable<Key>[] a, int fromIndex, int toIndex, Key key) 
    {
      int low = fromIndex;
      int high = toIndex - 1;
      int mid =0;
      boolean equal = false;
      while (low <= high) 
      {
        mid = (low + high) >> 1;
        Comparable<Key> midVal = a[mid];
        int cmp = compare(midVal, key);
        if ( cmp < 0)
        { low = mid + 1;
          //equal = false;
        }
        else { // dont check : if(cmp >0) because search always to left to found the leftest position
          high = mid - 1;   //search in left part also if key before mid is equal
          equal = equal || cmp ==0;  //one time equal set, it remain set.
        }
      }
      if(equal) return low > mid ? low : mid;  //one time found, then it is low or mid 
      else return -(low + 1);  // key not found.
    }



    /**Assures that if val1 is a String, the key is converted toString() before comparison.
     * @param val1
     * @param key
     * @return
     */
    protected int compare(Comparable<Key> val1, Key key){
      int cmp;
      if(val1 instanceof CharSequence){
        //prevent String.compareTo(AnyOtherCharSequence) because only String.compareTo(String) works:
        //but enables comparison of any other key type.
        CharSequence key1 = key instanceof CharSequence ? (CharSequence)key : key.toString();
        cmp = StringFunctions.compare((CharSequence)val1, key1);  
      } else {
        cmp = val1.compareTo(key);  //compare CharSequence, not only Strings
      }
      return cmp;
    }



    /**Puts or adds an object in the table. The key may be ambiguous. On adding a new object with the same key is placed
     * after an containing object with this key. On puting that object which is found with the key will be replaces.
     * If the table is full, a new table will be created internally.
     *  
     * 
     */
    private Type putOrAdd(Key sortKey, Type value, Type valueNext, KindofAdd kind)
    { //NOTE: returns inside too.
      check();
      Type lastObj = null;
      if(isHyperBlock && sizeBlock == maxBlock){
        //split the block because it may be insufficient. If it is insufficient,
        //the split from child to parent does not work. split yet.
        if(parent !=null){
          Table<Key, Type> sibling = splitIntoSibling(-1, null, null);
          if(compare(sibling.aKeys[0],sortKey) <=0){
            //do it in the sibling.
            return sibling.putOrAdd(sortKey, value, valueNext, kind);
          }
        } else {
          rootIdxTable.splitTopLevel(-1, null, null);
        }
      }
      check();
      //place object with same key after the last object with the same key.
      int idx = Arrays.binarySearch(aKeys, sortKey); //, sizeBlock, key1);
      if(idx < 0) {
        //not found
        idx = -idx-1;  //NOTE: sortin after that map, which index starts with equal or lesser index.
        if(isHyperBlock)
        { //call it recursively with sub index.
          //the block with the range 
          idx -=1;
          if(idx<0)
          { //a index less than the first block is gotten.
            //sortin it in the first block.
            idx = 0;
            Table<Key, Type> parents = this;
            while(parents != null)
            { //if(key1 < key[0])
              if(compare(sortKey,aKeys[0]) <0)
              { aKeys[0] = sortKey; //correct the key, key1 will be the less of child.
              }
              parents = parents.parent;
            }
            //NOTE: if a new child will be created, the key[0] is set with new childs key.
          }
          @SuppressWarnings("unchecked")
          Table<Key, Type> childTable = (Table<Key, Type>)aValues[idx];
          //check();  //firstly the key should be inserted before check!
          lastObj = childTable.putOrAdd(sortKey, value, valueNext, kind); 
          check();
        }
        else {
          //no hyperblock, has leaf data:
          if(idx <0)
          { idx = -idx -1;
            sortin(idx, sortKey, value);  //idx+1 because sortin after found position.            
            check();
          }
          else
          { sortin(idx, sortKey, value);  //idx+1 because sortin after found position.            
            check();
          }
        }
      }
      else
      { //if key1 is found, sorting after the last value with that index.
        switch(kind){
          case replace: {
            //should replace.
            if(isHyperBlock){
              @SuppressWarnings("unchecked")
              Table<Key, Type> childTable = (Table<Key, Type>)aValues[idx];
              lastObj = childTable.putOrAdd(sortKey, value, valueNext, kind);
            } else {
              @SuppressWarnings("unchecked")
              Type lastObj1 = (Type)aValues[idx];
              lastObj = lastObj1;
              aValues[idx] = value;   //replace the existing one.
            }
          } break;
          case addBefore: {
            boolean ok = searchAndSortin(sortKey, value, idx, valueNext);
            if(!ok){
              searchbackAndSortin(sortKey, value, idx, valueNext);
            }
          } break;
          case addLast: {
            assert(valueNext ==null);
            searchLastAndSortin(sortKey, value, idx);
          } break;
          case addOptimized: {
            if(isHyperBlock){
              @SuppressWarnings("unchecked")
              Table<Key, Type> childTable = (Table<Key, Type>)aValues[idx];
              childTable.putOrAdd(sortKey, value, valueNext, kind);
            } else {
              sortin(idx, sortKey, value);
            }
          } break;
        }//switch
      }
      check();
      return lastObj;
    }



    /**Sorts in the given value after all other values with the same key.
     * <ul>
     * <li>If this table has a parent table and the parent table or its parent has a next child with the same key,
     *   then this method is started in the parent. It searches the last key in the parent firstly. Therewith
     *   it is faster to search the end of entries with this key.
     * <li>Elsewhere the last key is searched in this table. Only this table and maybe its children contains the key.
     * <li>If this table is not a hyper block, the value is {@link #sortin(int, Comparable, Object)} after the last
     *   found key.
     * <li>If this table is a hyper block, the last child table with the same key is entered with this method
     *   to continue in the child.     
     * </ul>
     * @param sortkey The key for sort values.
     * @param value
     * @param ixstart The start index where a this key is found.
     * @return
     */
    private boolean searchLastAndSortin(Key sortkey, Type value, int ixstart){
      boolean cont = true;
      int ix = ixstart;
      Table<Key, Type> parent1 = parent, child1 = this;
      while(parent1 !=null){
        if( child1.ixInParent +1 < parent1.sizeBlock 
          && compare(parent1.aKeys[child1.ixInParent+1], sortkey)==0) {
          //the next sibling starts with the same key, look in the parent!
          //Note that it is recursively, starts with the highest parent with that property,
          //walk trough the parent firstly, therefore it is fast.
          return parent1.searchLastAndSortin(sortkey, value, ixInParent+1);
        }
        else if(child1.ixInParent == parent1.sizeBlock){
          //it is possible that the parent's parent have more same keys:
          child1 = parent1; parent1 = parent1.parent;  //may be null, then abort
        } else {
          parent1 = null; //forces finish searching parent.
        }
      }
      while(cont && ix < sizeBlock){
        if((++ix) == sizeBlock             //end of block reached. The sibling does not contain the key because parent is tested.
          || compare(aKeys[ix],sortkey) != 0  //next child has another key 
          ){
          if(isHyperBlock){
            ix-=1;  //it should be stored in the last hyper block, not in the next one. 
            @SuppressWarnings("unchecked")
            Table<Key, Type> childTable = (Table<Key, Type>)aValues[ix];
            childTable.searchLastAndSortin(sortkey, value, 0);
            cont = false;
          } else {
            cont = false;
            sortin(ix, sortkey, value);   //sortin after 
          }
        }
      }
      check();
      
      return !cont;
    }



    /**Sorts in the given value before the given element with the same key forward.
     * Starting from the given position all elements where iterate.
     * @param sortkey The key for sort values.
     * @param value
     * @param ixstart The start index where a this key is found.
     * @param valueNext the requested next value.
     * @return
     */
    private boolean searchAndSortin(Key sortkey, Type value, int ixstart, Type valueNext){
      boolean cont = true;
      boolean ok = false;
      int ix = ixstart;
      while(cont && ix < sizeBlock){
        if(isHyperBlock){
          @SuppressWarnings("unchecked")
          Table<Key, Type> childTable = (Table<Key, Type>)aValues[ix];
          ok = childTable.searchAndSortin(sortkey, value, ix, valueNext);
          check();
          cont = !ok;
          if(cont){
            ix +=1;  //continue with next.
          }
        } else {
          if(ix < (sizeBlock -1) || aValues[ix+1] == valueNext){
            sortin(ix, sortkey, value);
            check();
            ok = true;
            cont = false;
          }
          else if((++ix) < sizeBlock && compare(aKeys[ix],sortkey) != 0){
            cont = false;
          }
        }
      }
      check();
      return ok;
    }



    /**Sorts in the given value before the given element with the same key backward.
     * Starting from the given position all elements where iterate. TODO not ready yet.
     * @param sortkey The key for sort values.
     * @param value
     * @param ixstart The start index where a this key is found.
     * @param valueNext the requested next value.
     * @return
     */
    private boolean searchbackAndSortin(Key sortkey, Type value, int ixstart, Type valueNext){
      return false;
    }



    /**inserts the given element into the table at given position.
     * If the table is less, it will be split either in an additional sibling 
     * or, if it is the top level table, into two new tables under the top table.
     * Split is done with {@link #splitIntoSibling(int, Comparable, Object)} or {@link #splitTopLevel(int, Comparable, Object)}.
     * If the table is split, the value is inserted in the correct table.
     * @param ix The index position for the actual table where the value should be sorted in.
     * @param sortkey sorting string to insert.
     * @param value value to insert.
     */
    private void sortin(int ix, Key sortkey, Object value)
    { //assert(value instanceof Type || value instanceof Table);
      if(sizeBlock == maxBlock)
      { //divide the block:
        if(isHyperBlock)
          rootIdxTable.stop();
        if(parent != null)
        { //it has a hyper block, use it!
          //create a new sibling of this.
          splitIntoSibling(ix, sortkey, value);
          check();
        }
        else
        { //The top level block, it can be splitted only.
          //divide the content of the current block in 2 blocks.
          rootIdxTable.splitTopLevel(ix, sortkey, value);
          check();
        }
      }
      else
      { //shift all values 1 to right, regard ixInParent if it is a child table.
        if(ix < sizeBlock)
        { //move all following items to right:
          movein(this, this, ix, ix+1, sizeBlock - ix);
        }
        sizeBlock +=1;
        aKeys[ix] = sortkey;
        aValues[ix] = value;
        //if(value instanceof IndexMultiTable<?, ?>){
        if(value instanceof Table){  //a sub table is either an instance of IndexMultiTable.Table or any other Object
           @SuppressWarnings("unchecked")
          Table<Key,Type> childTable = (Table<Key,Type>)value;
          childTable.ixInParent = ix;
          childTable.parent = this;
        } else {
          addSizeAll(1);  //add a leaf.
        }
      }
      //don't check(); because it is not consistent in this state.
    }



    /**
     * @param idx if <0 then do not sortin a key, obj1
     * @param key1
     * @param obj1
     */
    private Table<Key, Type> splitIntoSibling(final int idx, Key key1, Object obj1){
      Table<Key, Type> sibling = new Table<Key, Type>(rootIdxTable);
      @SuppressWarnings("unused") int sizeall1 = parent.check();
      sibling.parent = parent;
      sibling.isHyperBlock = isHyperBlock;
      sibling.ixInParent = this.ixInParent +1;
      //sortin divides the parent in 2 tables if it is full.
      int newSize = sizeBlock/2;
      if(idx > newSize){
        //new element moved into the sibling.
        final int ix1 = idx - newSize;
        sibling.sizeAll = movein(this, sibling, newSize, 0, idx - newSize);
        sibling.aKeys[ix1] = key1;
        sibling.aValues[ix1] = obj1;
        sibling.sizeAll += movein(this, sibling, idx, ix1 +1, sizeBlock - idx);
        sibling.sizeBlock = sizeBlock - newSize +1;
        this.sizeBlock = newSize;
        this.sizeAll -= sibling.sizeAll; //The elements in sibling.
        if(obj1 instanceof Table){
          @SuppressWarnings("unchecked")
          Table<Key,Type> childTable = (Table<Key,Type>)obj1;
          childTable.ixInParent = ix1;
          childTable.parent = sibling;
        }
        clearRestArray(this);
        parent.sortin(sibling.ixInParent, sibling.aKeys[0], sibling);  //sortin the empty table in parent.      
        if(!(obj1 instanceof Table)){//add a leaf
          sibling.addSizeAll(1); //the new element. Add leaf only on ready structure
        }
        parent.check();
      } else {
        //new element moved into this.
        sibling.sizeAll = movein(this, sibling, newSize, 0, sizeBlock - newSize);
        sibling.sizeBlock = sizeBlock - newSize; 
        if(idx >=0){
          if(idx < newSize){ //move only if it is not on the end. idx == newSize: movein not necessary.
            movein(this, this, idx, idx+1, newSize -idx);
          }
          sizeBlock = newSize +1;
          sizeAll = sizeAll - sibling.sizeAll;
          this.aKeys[idx] = key1;
          this.aValues[idx] = obj1;
          if(obj1 instanceof Table){
            @SuppressWarnings("unchecked")
            Table<Key,Type> childTable = (Table<Key,Type>)obj1;
            childTable.ixInParent = idx;
            childTable.parent = this;
          }
        } else { //idx < 0, nothing to add
          sizeBlock = newSize;
          sizeAll = sizeAll - sibling.sizeAll;
        }
        clearRestArray(this);
        parent.sortin(sibling.ixInParent, sibling.aKeys[0], sibling);  //sortin the empty table in parent.      
        if(idx >=0  && !(obj1 instanceof Table)){ //has add a leaf
          this.addSizeAll(1); //the new element. Add leaf only on ready structure
        }
        parent.check();
      }
      return sibling;
    }



    /**Moves some elements of the src table in the dst table. Note that this method does not need any information
     * of this. It is a static method. Only because Key and Src should be known - the same like the calling instance -
     * this method is not static.
     * @param src The source table
     * @param dst The destination table
     * @param ixSrc Position in src
     * @param ixDst Position in dst
     * @param nrof number of elements to move from src to dst.
     * @return the number of elements moved inclusively all elements in children, to build {@link #sizeAll}
     */
    private int movein(Table<Key,Type> src, Table<Key,Type> dst, int ixSrc, int ixDst, int nrof){
      int sizeRet = 0;
      //int ix2 = ixDst + nrof - 1;
      int ct1 = nrof;
      int ix1Src, ix1Dst;
      final int dx;
      if(src == dst && ixSrc < ixDst) {
        //start from end, backward
        dx = -1;
        ix1Src = ixSrc + nrof -1;
        ix1Dst = ixDst + nrof -1;
      } else {
        dx = 1;
        ix1Src = ixSrc;
        ix1Dst = ixDst;
      }
      boolean bHypertable = nrof >0 && (src.aValues[ixSrc] instanceof Table<?,?>);
      while(--ct1 >=0) {
      //for(int ix1 = ixSrc + nrof-1; ix1 >= ixSrc; --ix1){
        Object value = src.aValues[ix1Src];
        if(bHypertable) {
          @SuppressWarnings("unchecked")
          Table<Key,Type> childTable = (Table<Key,Type>)value;
          childTable.ixInParent = ix1Dst;
          childTable.parent = dst;
          sizeRet += childTable.sizeAll;
        } else {
          sizeRet += 1;
        }
        dst.aValues[ix1Dst] = value;
        dst.aKeys[ix1Dst] = src.aKeys[ix1Src];
        ix1Src += dx;  //count forward or backward.
        ix1Dst += dx;
      }
      return sizeRet;
    }



    /**Cleanup the part if {@link #aKeys} and {@link #aValues} which are not used.
     * Note: {@link #sizeBlock} have to be set correctly.
     * @param dst The table where clean up is done.
     */
    private void clearRestArray(Table<Key,Type> dst){
      //Key maxKey = root1.provider.getMaxSortKey();
      for(int ix = dst.sizeBlock; ix < maxBlock; ix++)
      { dst.aKeys[ix] = rootIdxTable.maxKey__; 
        dst.aValues[ix] = null;
      }
      
    }





    /**Deletes the element on ix in the current table.
     * @param ix
     */
    protected void delete(int ix){
      //Key keydel = aKeys[ix];
      sizeBlock -=1;
      if(!(aValues[ix] instanceof Table)) {
        addSizeAll(-1);
      }
      if(ix < sizeBlock){ //Note:sizeBlock is decremented already.
        movein(this, this, ix+1, ix, this.sizeBlock - ix);
      }
      if(ix == 0) {
        correctKey0InParents();
      }
      aKeys[sizeBlock] = rootIdxTable.maxKey__;
      aValues[sizeBlock] = null;   //prevent dangling references!
      ////
      if(sizeBlock == 0) {
        if(parent !=null){
          //this sub-table is empty
          //
          //int ixParent = binarySearchFirstKey(parent.aKeys, 0, parent.sizeBlock, keydel); //, sizeBlock, key1);
          //if(ixParent < 0)
          //{ ixParent = -ixParent-1;  
          //}
          parent.delete(this.ixInParent);  //call recursively.
          //it has delete the child table. The table may be referenced by an iterator still.
          //But the iterator won't detect hasNext() and it continoues on its parent iterator too. 
        } else {
          //The root table is empty. If it was an Hypertable:
          isHyperBlock = false; //elsewhere problems on next put.
        }
      } else { //check only if this table is not deleted.
        check();
      }
    }




    /**Delete all content. 
     * @see java.util.Map#clear()
     */
    public void clear()
    {
      for(int ix=0; ix<sizeBlock; ix++){
        if(isHyperBlock){ 
          @SuppressWarnings("unchecked")
          Table<Key, Type> subTable = (Table<Key, Type>)aValues[ix];
          subTable.clear();
        }
        aValues[ix] = null;
        aKeys[ix] = rootIdxTable.maxKey__; 
      }
      sizeBlock = 0;
      sizeAll = 0;
      isHyperBlock = false;
      check();
    }

    
    /**Searches the key in the tables.
     * @param key1 The key
     * @param exact if true then returns null and retFound[0] = false if the key was not found
     *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
     * @param ixFound should be create newly or initialize. 
     *   If the key was found, the found is set to true. If the key is not found, the found is not
     *   touched. It should be false initially. If the key is found and the value for this key is null, found true.
     *   Only with this the {@link #containsKey(Object)} works probably.
     *   ix is set in any case if this method does not return null. 
     * @return The table where the element is found. 
     *   null if the key is lesser than all other keys (it should the first position).
     *   null if the value for this key is null.
     *   null if exact = true and the key is not found.
     */
    @SuppressWarnings( "unchecked")
    private  Table<Key, Type> searchInTables(Key key1, boolean exact, IndexMultiTable<Key, Type>.IndexBox ixFound)
    { Table<Key, Type> table = this;
      //place object with same key after the last object with the same key.
      while(table.isHyperBlock)
      { int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
        if(idx < 0)
        { //an non exact found index is possible if it is an Hyper block.
          idx = -idx-2;  //NOTE: access to the lesser element before the insertion point.
        }
        if(idx<0)
        { return null;
        }
        else
        { assert(idx < table.sizeBlock);
          table = ((Table<Key, Type>)(table.aValues[idx]));
        }
      }
      int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
      { if(idx < 0){
          if(exact) return null;
          else {
            //ixFound.found remain false
            idx = -idx -2;   //NOTE: access to the lesser element before the insertion point.
          }
        } else {
          ixFound.found = true; 
        }
        if(idx >=0)
        { ixFound.ix = idx;
          return table;
        }
        else  
        { //not found, before first.
          return null;
        }  
      }
    }



    Table<Key, Type> nextSibling(){
      Table<Key, Type> sibling = null;
      if(parent !=null){
        if(ixInParent < sizeBlock-1){
          @SuppressWarnings("unchecked")
          Table<Key, Type> sibling1 = (Table<Key, Type>)parent.aValues[ixInParent+1];
          sibling = sibling1;
        } else {
          Assert.check(false);
        }
      } else {
        Assert.check(false);
      }
      return sibling;
    }

    
    
    /**Change the size in this table and in all parents.
     * @param add usual +1 or -1 for add and delete.
     */
    void addSizeAll(int add){
      this.sizeAll += add;
      if(parent !=null) {
        parent.addSizeAll(add);
      }
    }



    /**Correct the key in the parent if the key on aKeys[0] was changed.
     * This method is invoked on put or delete.
     */
    private void correctKey0InParents()
    {
      int ix2 = 0;
      Table<Key, Type> parentTable = this; 
      while(parentTable.parent !=null && ix2 == 0) {
        parentTable.parent.aKeys[parentTable.ixInParent] = parentTable.aKeys[0];  //The key to the child
        ix2 = parentTable.ixInParent; 
        parentTable = parentTable.parent;
      }
    }
    

    @SuppressWarnings("unchecked")
    int check()
    { int sizeAllCheck = 0; 
      if(rootIdxTable.shouldCheck){
        if(parent!=null){
          rootIdxTable.assert1(parent.aValues[ixInParent] == this);
        }
        //if(sizeBlock >=1){ rootIdxTable.assert1(aValues[0] != null); }  //2015-07-10: removed a value can be null
        for(int ii=1; ii < sizeBlock; ii++)
        { rootIdxTable.assert1(compare(aKeys[ii-1],aKeys[ii]) <= 0);
          //rootIdxTable.assert1(aValues[ii] != null);  //2015-07-10: removed a value can be null
          if(aValues[ii] == null)
            rootIdxTable.stop();
        }
        if(isHyperBlock)
        { for(int ii=0; ii < sizeBlock; ii++)
          { rootIdxTable.assert1(aValues[ii] instanceof Table);
            Table<Key, Type> childtable = (Table<Key, Type>)aValues[ii]; 
            rootIdxTable.assert1(aKeys[ii].equals(childtable.aKeys[0])); //check start key is equal first key in sub table.
            rootIdxTable.assert1(childtable.ixInParent == ii);           //check same index in sub table.
            sizeAllCheck += childtable.check();  //recursively call of check.
          }
        } else {
          sizeAllCheck = this.sizeBlock; //elements to return..
        }
        rootIdxTable.assert1(sizeAllCheck == this.sizeAll);
        for(int ii=sizeBlock; ii < maxBlock; ii++) //check rest of table is empty.
        { rootIdxTable.assert1(aKeys[ii] == rootIdxTable.maxKey__);
          rootIdxTable.assert1(aValues[ii] == null);
        }
      }
      return sizeAllCheck;
    }



    /**Checks the consistency of the table. This method is only proper for test of the algorithm
     * and assurance of correctness.  
     * @param parentP The parent of this table or null for the top table.
     * @param keyParentP The key of this table in the parents entry. null for top table.
     * @param ixInParentP The position of this table in the parent's table. -1 for top table.
     * @param keylastP The last key from the walking through the last child, minimal key for top table.
     * @return The last found key in order of tables.
     */
    private Key checkTable(Table<Key, Type> parentP, Key keyParentP, int ixInParentP, Key keylastP){
      Key keylast = keylastP;
      rootIdxTable.assert1(parentP == null || keyParentP.equals(aKeys[0]));
      rootIdxTable.assert1(this.parent == parentP);
      rootIdxTable.assert1(this.ixInParent == ixInParentP);
      for(int ix = 0; ix < sizeBlock; ++ix){
        rootIdxTable.assert1(compare(aKeys[ix], keylast) >= 0);
        if(isHyperBlock){
          rootIdxTable.assert1(aValues[ix] instanceof Table);
          @SuppressWarnings("unchecked")
          Table<Key,Type> childTable = (Table<Key,Type>)aValues[ix];
          keylast = childTable.checkTable(this, aKeys[ix], ix, keylast);
        } else {
          rootIdxTable.assert1(!(aValues[ix] instanceof Table));
          keylast = aKeys[ix];
        }
      }
      for(int ix=sizeBlock; ix < maxBlock; ix++)
      { rootIdxTable.assert1(aKeys[ix] == rootIdxTable.maxKey__);
        rootIdxTable.assert1(aValues[ix] == null);
      }
      return keylast;
    }



    private void toString(StringBuilder u){
      if(sizeBlock ==0){
        u.append("..emptyIndexMultiTable...");
      }
      else if(isHyperBlock){
        for(int ii=0; ii<sizeBlock; ++ii){
          Table<?,?> subTable = (Table<?,?>)aValues[ii];
          subTable.toString(u);
        }
      } else { 
        for(int ii=0; ii<sizeBlock; ++ii){
          u.append(aKeys[ii]).append(", ");
        }
      }
      
    }



    @Override
    public String toString(){
      StringBuilder u = new StringBuilder();
      if(parent !=null){
        u.append("#").append(parent.identParent);
      }
      if(isHyperBlock){ u.append(':'); } else { u.append('='); }
      toString(u);
      return u.toString();
    }
    
  
    
    
    
  }    

    
  static int identParent_ = 100;

  final Key minKey__;

  final Key maxKey__;

  final Provide<Key> provider;

  private boolean shouldCheck = true;

  /**The maximal nr of elements in a block, maximal value of sizeBlock.
   * It is the same value as obj.length or key.length. */
  protected final static int maxBlock = AllocInBlock.restSizeBlock(IndexMultiTable.class, 160) / 8; //C: 8=sizeof(int) + sizeof(Object*) 

  private final Table<Key, Type> root;



  /**modification access counter for Iterator. */
  //@SuppressWarnings("unused")
  private int modcount;



  /**constructs an empty instance without data. */
  public IndexMultiTable(Provide<Key> provider)
  { //this(1000, 'I');
    this.provider = provider;
    this.minKey__ = provider.getMinSortKey();
    this.maxKey__ = provider.getMaxSortKey();
    this.root = new Table<Key, Type>(this);
  }

  /**Delete all content. 
   * @see java.util.Map#clear()
   */
  public synchronized void clear(){ modcount +=1; root.clear(); }

  /**Puts the (key - value) pair to the container. An existing value with the same key will be replaced
   * like described in the interface. If more as one value with this key are existing, the first one
   * will be replaced only. 
   * <br>
   * See {@link #add(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  @Override public synchronized Type put(Key key, Type value){
    modcount +=1;
    return root.putOrAdd(key, value, null, KindofAdd.replace);
  }

  /**Adds the (key - value) pair to the container. All existing values with the same key will be retained.
   * If one or some values with the same key are contained in the container already, the new value is placed
   * in a non-defined order with that values. The order depends from the search algorithm. It is the fastest
   * variant to sort in a new value. This method should be used if the order of values with the same key
   * are not regardless.
   * <br>
   * See {@link #put(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public synchronized void add(Key key, Type value){
    modcount +=1;
    root.putOrAdd(key, value, null, KindofAdd.addOptimized);
  }

  @SuppressWarnings("unchecked")
  public synchronized boolean containsKey(Object key)
  { boolean[] found = new boolean[1];
    return search((Key)key, true, found) !=null || found[0];
  }

  public void shouldCheck(boolean val){ shouldCheck = val; }
  

  
  /**Appends the (key - value) pair to the container. All existing values with the same key will be retained.
   * If one or some values with the same key are contained in the container already, the new value is placed
   * in order after all other. The values with the same key are sorted by its append order.
   * If some more values with the same key are existing, the searching can be need some calculation time.
   * But the time is less if the table size is less. 
   * This method should be used if the order of values with the same key is need.
   * <br>
   * See {@link #put(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public synchronized void append(Key key, Type obj){
    if(key.equals("ckgro") && root.sizeAll == 19)
      Assert.stop();
    modcount +=1; 
    root.putOrAdd(key, obj, null, KindofAdd.addLast);
  }

  
  /**Adds the (key - value) pair to the container. All existing values with the same key will be retained.
   * The value is placed before the given next value which must have the same key. If the nextValue is not found,
   * the key is placed in a non deterministic order.
   * If some more values with the same key are existing, the searching can be need some calculation time.
   * This method should be used only if that order of values with the same key is need.
   * <br>
   * See {@link #put(Comparable, Object)}, {@link #add(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public synchronized void addBefore(Key key, Type value, Type valueNext){
    modcount +=1; 
    root.putOrAdd(key, value, valueNext, KindofAdd.addBefore);
  }

  
  
  
  
  public boolean containsValue(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<java.util.Map.Entry<Key, Type>> entrySet()
  {
    return entrySet;
  }


  @SuppressWarnings({ "unchecked" })
  @Override public synchronized Type get(Object keyArg){
    assert(keyArg instanceof Comparable<?>);
    IndexBox ixRet = new IndexBox();
    Table<Key, Type> table = root.searchInTables((Key)keyArg, true, ixRet);
    if(table !=null){
      return (Type)table.aValues[ixRet.ix];
    } else return null;
  }



  /**Searches the object with exact this key or the object which's key is the nearest lesser one.
   * For example if given is "Bx, By, Bz" as keys and "Bya" is searched, the value with the key "By" is returned.
   * The user should check the returned object whether it is matching to the key or whether it is able to use.
   * The found key is unknown outside of this routine but the key or adequate properties should able to get 
   * from the returned value.
   * @param key
   * @return
   */
  public synchronized Type search(Key key){ 
    return search(key, false, null);
  }

  
  
  /**Searches the key in the tables.
   * @param keyArg The key
   * @param exact if true then returns null and retFound[0] = false if the key was not found
   *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
   * @param retFound If null then not used. If not null then it must initialized with new boolean[1].
   *   retFound[0] is set to true or false if the key was found or not.
   *   Note: If the key is found and the value for this key is null, retFound[0] is set to true.
   *   Only with this the {@link #containsKey(Object)} works probably. 
   * @return The exact found value or the non exact found value with key before. 
   *   null if the key is lesser than all other keys (it should the first position).
   *   null if the value for this key is null.
   *   null if exact = true and the key is not found.
   */
  public synchronized Type search(Key keyArg, boolean exact, boolean[] retFound)
  { 
    IndexBox ixRet = new IndexBox();
    Table<Key, Type> table = root.searchInTables(keyArg, exact, ixRet);
    if(table !=null){
      if(retFound !=null){ retFound[0] = ixRet.found; }
      @SuppressWarnings("unchecked")
      Type ret = (Type)table.aValues[ixRet.ix];
      return ret;
    } else return null;
  }
  
  
  public boolean isEmpty()
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<Key> keySet()
  {
    // TODO Auto-generated method stub
    return null;
  }


















  public int size()
  { return root.sizeAll;
  }







  @Override public Collection<Type> values()
  {
    //Should return an implementation of Collection which deals with the inner data.
    //see iterator(), entrySet
    // TODO Auto-generated method stub
    return null;
  }











  /**Returns an iterator over the table.
   * Note: The code which uses the iterator may be crimped by a synchronized(...){...}
   * because the table should not be changed.
   * @see java.lang.Iterable#iterator()
   */
  public ListIterator<Type> iterator()
  {
    return new IteratorImpl();
  }



  /**Creates an iterator which is placed in the occurrence of the given key.
   * The {@link ListIterator#previous()} is lesser or equal and the {@link ListIterator#next()}
   * is greater or equal. If a lesser or greater element is not found then the {@link ListIterator#hasPrevious()} 
   * and {@link ListIterator#hasNext()} returns false. 
   * Note: The code which uses the iterator may be crimped by a synchronized(...){...}
   * because the table should not be changed.
   * @param fromKey The start key.
   * @return
   */
  public ListIterator<Type> iterator(Key fromKey)
  {
    return new IteratorImpl(fromKey);
  }



  /**
   * @param keyArg
   * @return
   */
  @SuppressWarnings({ "unchecked" })
  @Override synchronized public Type remove(Object keyArg){
    assert(keyArg instanceof Comparable<?>);
    IndexBox ixRet = new IndexBox();
    Table<Key, Type> table = root.searchInTables((Key)keyArg, true, ixRet);
    if(table !=null){ //null if keyArg is not found.
      Type ret = (Type)table.aValues[ixRet.ix];  //deleted value should be returned.
      table.delete(ixRet.ix);  
      return ret;    
    } else return null;
  }

  
  void stop()
  { //debug
  }
  
  
  
  /**Checks the consistency of the table. This method is only proper for test of the algorithm
   * and assurance of correctness. 
   * The check starts with the top table. It iterates over all children in order of the tables and checks:
   * <ul>
   * <li>{@link #ixInParent} and {@link #parent} of a child hyper table.
   * <li>equality of the {@link #aKeys}[ixInParent] in the parent and {@link #aKeys}[0] in the child hyper table.
   * <li>Order of all sort keys in all tables.
   * <li>All not used entries in {@link #aKeys} should have the {@link Provide#getMaxSortKey()}
   *   and all not used entries in {@link #aValues} should have null.
   * </ul> 
   * If any error is found, an RuntimeException is invoked. It is recommended to test in a debugger.
   * If the algorithm in this class is correct, the exception should not be invoked.
   */ 
  public void checkTable(){ root.checkTable(null, null, -1, provider.getMinSortKey() );}
  
  
  void assert1(boolean cond)
  {
    if(!cond)
    { stop();
      throw new RuntimeException("IndexMultiTable - is corrupted;");
    }  
  }

  
  
  @Override
  public synchronized void putAll(Map<? extends Key, ? extends Type> m)
  {
    for(Map.Entry<? extends Key, ? extends Type> e: m.entrySet()){
      put(e.getKey(), e.getValue());
    }
  }
  
  
  
  /**Splits the top level into 2 child tables and inserts the given element. The top level will refer this 2 tables after them.
   * Note: Splitting of sub tables creates 2 tables from one given, another algorithm.
   * @param idx
   * @param key1
   * @param obj1
   */
  private void splitTopLevel(int idx, Key key1, Object obj1){
    Table<Key, Type> left = new Table<Key, Type>(this);
    Table<Key, Type> right = new Table<Key, Type>(this);
    left.parent = right.parent=root;
    left.isHyperBlock = right.isHyperBlock = root.isHyperBlock;
    left.ixInParent = 0;
    right.ixInParent = 1;
    //the current block is now a hyper block.
    root.isHyperBlock = true;
    int newSize = root.sizeBlock/2;
    if(idx > newSize){  //new object to the right table
      left.sizeAll = root.movein(root, left, 0, 0, newSize);
      left.sizeBlock = newSize;
      right.sizeAll = root.movein(root, right, newSize, 0, idx - newSize);
      int ix1 = idx - newSize;
      right.aKeys[ix1] = key1;
      right.aValues[ix1] = obj1;
      if(obj1 instanceof Table){ //insert a table.
        @SuppressWarnings("unchecked")
        Table<Key,Type> childTable = (Table<Key,Type>)obj1;
        right.sizeAll += childTable.sizeAll;    //don't change sizeAll of parent because there are not new leafs.
        childTable.ixInParent = ix1;
        childTable.parent = right;
      } else { //simple element.
        right.addSizeAll(1);
      }
      right.sizeAll += root.movein(root, right, idx, ix1+1, root.sizeBlock - idx);
      right.sizeBlock = root.sizeBlock - newSize +1;
      root.aValues[0] = left;
      root.aValues[1] = right;
      left.check();
      right.check();
    } else { //new object to the left table.
      if(idx >=0){
        left.sizeAll = root.movein(root, left, 0, 0, idx);
        left.aKeys[idx] = key1;
        left.aValues[idx] = obj1;
        if(obj1 instanceof Table){
          @SuppressWarnings("unchecked")
          Table<Key,Type> childTable = (Table<Key,Type>)obj1;
          childTable.ixInParent = idx;
          childTable.parent = left;
        }
        left.addSizeAll(1);
        left.sizeAll += root.movein(root, left, idx, idx+1, newSize - idx);
        left.sizeBlock = newSize +1;
      } else {
        left.sizeAll = root.movein(root, left, 0, 0, newSize);
        left.sizeBlock = newSize;
      }
      right.sizeAll = root.movein(root, right, newSize, 0, root.sizeBlock - newSize);
      right.sizeBlock = root.sizeBlock - newSize;
      root.aValues[0] = left;
      root.aValues[1] = right;
      //left.check();
      //right.check();
    }
    root.aKeys[0] = left.aKeys[0]; //minKey__;  //because it is possible to sort in lesser keys.
    root.aKeys[1] = right.aKeys[0];
    root.sizeBlock = 2;
    root.clearRestArray(root);
    root.check();
  }

  
  
  
  @Override public String toString(){ return root.toString(); }
  
    /**This interface is necessary to provide tables and the minimum and maximum value for any user specific type.
   * For the standard type String use {@link IndexMultiTable#providerString}.
   * @param <Key>
   */
  public interface Provide<Key>{
    /**Creates an array of the Key type with given size. */
    Key[] createSortKeyArray(int size);
    
    /**Returns the minimal value of the key. It is a static value. */
    Key getMinSortKey();
    
    /**Returns the maximal value of the key. It is a static value. */
    Key getMaxSortKey();
  }

  /**Provider for String keys. Used for {@link #IndexMultiTable(Provide)}. */
  public static final IndexMultiTable.Provide<String> providerString = new IndexMultiTable.Provide<String>(){

    @Override public String[] createSortKeyArray(int size){ return new String[size]; }

    @Override public String getMaxSortKey(){ 
      return "\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffff"; 
    }

    @Override public String getMinSortKey(){ return ""; }
  };
  

  Set<Map.Entry<Key, Type>> entrySet = new Set<java.util.Map.Entry<Key, Type>>()
  {

    @Override
    public boolean add(Map.Entry<Key, Type> e)
    {
      put(e.getKey(), e.getValue());
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends java.util.Map.Entry<Key, Type>> c)
    { for(Map.Entry<Key, Type> e: c){
        put(e.getKey(), e.getValue());
      }
      return true;
    }

    @Override
    public void clear()
    {
      IndexMultiTable.this.clear();
    }

    @Override
    public boolean contains(Object o)
    { return IndexMultiTable.this.containsValue(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    { boolean ok = true;
      for(Object obj: c){
        if(!IndexMultiTable.this.containsValue(obj)){
          ok = false;
        }
      }
      return ok;
    }

    @Override
    public boolean isEmpty()
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Iterator<java.util.Map.Entry<Key, Type>> iterator()
    { return IndexMultiTable.this.new EntrySetIterator();
    }

    @Override
    public boolean remove(Object o)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public Object[] toArray()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <T> T[] toArray(T[] a)
    { 
      // TODO Auto-generated method stub
      return null;
    }
    
  };
  
  
  
  protected class EntrySetIterator implements Iterator<Map.Entry<Key, Type>>
  {

    private final IteratorImpl tableIter = (IteratorImpl)IndexMultiTable.this.iterator();
    
    @Override public boolean hasNext()
    {
      return tableIter.hasNext();
    }

    @Override public Map.Entry<Key, Type> next()
    {
      //IteratorHelper<Key, Type> helperNext = tableIter.helperNext;
      Type value = tableIter.next();
      Key key = tableIter.lastKey();
      return new Entry(key, value); 
    }

    @Override
    public void remove()
    { tableIter.remove();
      //tableIter.helperNext.table.delete(tableIter.helperNext.idx);
      //tableIter.
      /*
      tableIter.helperNext.idx -=1;  //maybe -1 if first was deleted.
      //IndexMultiTable.IteratorHelper<Key, Type> helperTest = tableIter.helperNext;
      while(tableIter.helperNext.parentIter !=null && tableIter.helperNext.table.sizeBlock ==0){
        tableIter.helperNext = tableIter.helperNext.parentIter;
        tableIter.helperNext.idx -=1;  //on idx it is the next, it has deleted the child table!
      }
      */
    }
    
    
    @Override public String toString(){ return tableIter.toString(); }
    
  };
  
  
  protected class Entry implements Map.Entry<Key, Type>{
    final Type value; final Key key;
    Entry(Key key, Type value){ this.key = key; this.value = value; }
    @Override public Key getKey()
    { return key;
    }
    @Override
    public Type getValue()
    { return value;
    }
    
    @Override
    public Type setValue(Type value)
    { throw new IllegalArgumentException("IndexMultiTable.Entry does not support setValue()");
    }
    
    @Override public String toString(){ return "[ " + key + ", " + value + " ]"; }
  }
  
  
  
  class IndexBox{
    int ix;
    boolean found;
  }
  
  
}
