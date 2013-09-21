package org.vishia.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.vishia.bridgeC.AllocInBlock;



/**This class contains sorted references of objects (values) with an comparable key 
 * in one or more tables of a limited size. 
 * <ul>
 * <li>In opposite to a {@link java.util.TreeMap} the key should not unique for all values.
 *   More as one value can have the same key. The method {@link #get(Object)} defined in 
 *   {@link java.util.Map} searches the first Object with the given key. 
 *   The method {@link #put(Comparable, Object)} does not remove an existing value but it puts the new value
 *   after the last existing one.  
 * <li>There is a method {@link #iterator(Comparable)} which starts on the first occurrence of the key
 *   or after a key which is lesser at the next key which is greater than the given key.
 *   It iterates to the end of the collection. The user can check the key in the returned {@link java.util.Map.Entry}
 *   whether the key is proper. In this kind a sorted view of a part of the content can be done.
 * <li>The container consist of one or more instances which have a limited size. If a table in one instance is filled,
 *   two new instances will be created in memory which contains the half amount of elements and the original
 *   instance is changed to a hyper table. The size of the instances are equal and less. It means, the search time
 *   is less and the memory requirement is constant. In this kind this class is able to use in C programming
 *   with a block heap of equal size blocks in an non-dynamic memory management system (long living
 *   fast realtime system).    
 * </ul>
 * This class contains the table of objects and the table of key of type Comparable. 
 * The tables are simple arrays of a fix size.
 * <br>
 * An instance of this class may be either a hyper table which have some children, 
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
public class IndexMultiTable<Key extends Comparable<Key>, Type> 
implements Map<Key,Type>, Iterable<Type>  //TODO: , NavigableMap<Key, Type>
{
  
  /**Version, history and license.
   * <ul>
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
  public static final int version = 20130807;

  final Key minKey__;
  
  final Key maxKey__;
  
  final Provide<Key> provider;
  
  /**The maximal nr of elements in a block, maximal value of sizeBlock.
   * It is the same value as obj.length or key.length. */
  protected final static int maxBlock = AllocInBlock.restSizeBlock(IndexMultiTable.class, 80) / 8; //C: 8=sizeof(int) + sizeof(Object*) 

  /**actual number of objects stored in this table. */
  protected int sizeBlock;
  
  /**The index of the first entry overall. */
  protected int ixValue0;
  
  /**True, than {@link #aValues} contains instances of this class too. */
  protected boolean isHyperBlock;
  
  /**modification access counter for Iterator. */
  //@SuppressWarnings("unused")
  protected int modcount;
  
  /**Array of objects appropritate to the keys. */
  protected final Object[] aValues = new Object[maxBlock];
  
  /**Array of keys, there are sorted ascending. The same key can occure some times. */ 
  //private final Comparable<Key>[] key = new Comparable[maxBlock];
  
  protected final Key[] aKeys; // = new Key[maxBlock];
  
  /**The parent if it is a child table. */
  private IndexMultiTable<Key, Type> parent;
  
  /**Index of this table in its parent. */
  private int ixInParent;
 
  
  
  /**This class is the Iterator for the outer class. Every {@link #iterator()}-call
   * produces one instance. It is possible to create some instances simultaneously.
   * @author HSchorrig
   *
   * @param <Type>
   */
  protected final class IteratorImpl implements Iterator<Type>
  {

    /**The helper contains the values of the iterator. Because there are a tree of tables,
     * the current IteratorHelper of the current table depths is referenced.
     * A IteratorHelper contains a parent and child reference. If the end of table
     * of the current depth level is reached, the parent of the current helper is referenced here
     * than, the next table is got, than the child is initialized and referenced here.  
     * 
     */
    public IndexMultiTable.IteratorHelper<Key, Type> helper;
    
    /**True if hasNext. The value is valid only if {@link bHasNextProcessed} is true.*/
    private boolean bHasNext = false;
      
    /**true if hasnext-test is done. */
    private boolean bHasNextProcessed = false;
    
    @SuppressWarnings("unused")
    private int modcountxxx;
    
    /**Only for test. */
    private Key lastkey; 
    
    protected IteratorImpl(IndexMultiTable<Key, Type> firstTable)
    { helper = new IteratorHelper<Key, Type>(null);
      helper.table = firstTable;
      helper.idx = -1;
      lastkey = minKey__;
    }
    
    
    /**Ctor for the Iterator with a range.
     * @param firstTable
     * @param startKey
     * @param endKey
     */
    IteratorImpl(IndexMultiTable<Key, Type> firstTable, Key startKey)
    { helper = new IteratorHelper<Key, Type>(null);
      helper.table = firstTable;
      helper.idx = -1;
      lastkey = minKey__;
      while(helper.table.isHyperBlock)
      { //call it recursively with sub index.
        int idx = binarySearchFirstKey(helper.table.aKeys, 0, helper.table.sizeBlock, startKey); //, sizeBlock, key1);
        if(idx < 0)
        { /**an non exact found, accept it.
           * use the table with the key lesser than the requested key
           */
          idx = -idx-2; //insertion point -1 
          if(idx < 0)
          { /**Use the first table if the first key in the first table is greater. */
            idx = 0; 
          }
        }
        //idx -=1;  //?
        helper.idx = idx;
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)helper.table.aValues[helper.idx];
        helper.childHelper = new IteratorHelper<Key, Type>(helper); 
        
        helper.childHelper.table = childTable;
        helper = helper.childHelper;  //use the sub-table to iterate.          
      }
      int idx = binarySearchFirstKey(helper.table.aKeys, 0, helper.table.sizeBlock,  startKey); //, sizeBlock, key1);
      if(idx < 0)
      { /**an non exact found, accept it.
         * start from the element with first key greater than the requested key
         */
        idx = -idx-1;  
      }
      helper.idx = idx;
      /**next_i() shouldn't called, because the helper.idx is set with first occurrence. */
      bHasNextProcessed = true;
      /**next() returns true always, except if the idx is 0 and the table contains nothing. */
      bHasNext =  idx < helper.table.sizeBlock;
    }
    
    
    public boolean hasNext()
    { if(!bHasNextProcessed)
      { next_i();  //call of next set bHasNext!
      }
      return bHasNext;
    }

    
    
    
    
    /**Implements the standard behavior for {@link java.util.Iterator#next()}.
     * For internal usage the {@link #helper} is set.
     * With them 
     */
    @SuppressWarnings("unchecked")
    public Type next()
    { if(!bHasNextProcessed)
      {  next_i();
      }
      if(bHasNext)
      { bHasNextProcessed = false;  //call it at next access!
        IndexMultiTable<Key, Type> table = helper.table;
        assert(table.aKeys[helper.idx].compareTo(lastkey) >= 0);  //test
        if(table.aKeys[helper.idx].compareTo(lastkey) < 0) throw new RuntimeException("assert");
        if(table.aKeys[helper.idx].compareTo(lastkey) < 0)
          stop();
        lastkey = table.aKeys[helper.idx];
        return (Type)table.aValues[helper.idx];
      }
      else return null;
    }

    Key getKeyForNext(){ return lastkey; }
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helper is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    @SuppressWarnings("unchecked")
    private void next_i()
    {
      bHasNext = ++helper.idx < helper.table.sizeBlock;  //next in current table.
      if(bHasNext)
      { if(helper.table.isHyperBlock)
        { //
          IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)helper.table.aValues[helper.idx];
          if(helper.childHelper == null)
          { //no child yet. later reuse the instance of child.
            helper.childHelper = new IteratorHelper(helper); 
          }
          helper.childHelper.idx = -1;  //increment as first action.
          helper.childHelper.table = childTable;
          helper = helper.childHelper;  //use the sub-table to iterate.          
        }
        else
        { //else: bHasNext is true.
          bHasNextProcessed = true;
        }
      }
      else
      { if(helper.parentIter != null)
        { //no next, but it is a sub-table. This sub-table is ended.
          //a next obj may be exist in the sibling table.
          helper.table = null;  //the child helper is unused now.
          helper = helper.parentIter; //to top of IteratorHelper, test there.
          /*Because bHasNextProcessed is false, this routine is called recursively, see method description. */
        }
        else
        { //else: bHasNext is false, it is the end.
          bHasNextProcessed = true;
        }
      }
      if(!bHasNextProcessed)
      { next_i();
      }
    }
    
    
    
    public void remove()
    {
      // TODO Auto-generated method stub
      
    }

    private void stop()
    { //debug
    }
    
    
  }

  
  
  
  /**This class contains the data for a {@link IndexMultiTable.IteratorImpl}
   * for one table.
   * 
   *
   */ 
  private static class IteratorHelper<Key extends Comparable<Key>, Type>
  {
    /**If not null, this helper is associated to a deeper level of table, the parent
     * contains the iterator value of the higher table.*/
    protected final IteratorHelper<Key, Type> parentIter;
    
    /**If not null, an either an empty instance for a deeper level of tables is allocated already 
     * or the child is used actual. The child is used, if the child or its child 
     * is the current IteratorHelper stored on {@link IteratorImpl#helper}. */ 
    protected IteratorHelper<Key, Type> childHelper;
    
    /**Current index in the associated table. */ 
    protected int idx;
    
    /**The associated table, null if the instance is not used yet. */
    IndexMultiTable<Key, Type> table;
    
    IteratorHelper(IteratorHelper<Key, Type> parentIter)
    { this.parentIter = parentIter;
      this.table = null;
      idx = -1;
    }
  }
  
  
  
  /**constructs an empty instance without data. */
  public IndexMultiTable(Provide<Key> provider)
  { //this(1000, 'I');
    this.provider = provider;
    this.aKeys = provider.genArray(maxBlock);
    this.minKey__ = provider.genMin();
    this.maxKey__ = provider.genMax();
    for(int idx = 0; idx < maxBlock; idx++){ aKeys[idx] = maxKey__; }
    sizeBlock = 0;
  }
  

  /**constructs an empty instance without data with a given size and key type. 
   * @param size The size of one table.
   * @param type one of char I L or s for int, long or String key.
   */
  /*
  public IndexMultiTable(int size, char type)
  { //TODO: allocate the fields etc. with given size. 
    for(int idx = 0; idx < maxBlock; idx++){ key[idx] = maxKey__; }
    sizeBlock = 0;
    
  }
  */
  

  
  public Type put(Key key, Type obj){
    return putOrAdd(key, obj, false);
  }

  
  public Type add(Key key, Type obj){
    return putOrAdd(key, obj, true);
  }

  
  
  /**Put a object in the table. The key may be ambiguous, a new object with the same key is placed
   * after an containing object with this key. If the table is full, a new table will be created internally.
   *  
   * 
   */
  @SuppressWarnings("unchecked")
  private Type putOrAdd(Key arg0, Type obj1, boolean shouldAdd)
  { Type lastObj = null;
    boolean found;
    //int key1 = arg0.intValue();
    //place object with same key after the last object with the same key.
    int idx = Arrays.binarySearch(aKeys, arg0); //, sizeBlock, key1);
    //if(key1 == -37831)
      //stop();
    if(idx < 0)
    { idx = -idx-1;  //NOTE: sortin after that map, which index starts with equal or lesser index.
      found = false;
    }
    else
    { //if key1 is found, sorting after the last value with that index.
      found = true;
      if(isHyperBlock || shouldAdd){
        while(idx <sizeBlock && aKeys[idx].compareTo(arg0) == 0)  //while the keys are identically
        { idx+=1; 
        }
      }
    }
    if(isHyperBlock)
    { //call it recursively with sub index.
      //the block with the range 
      idx -=1;
      IndexMultiTable<Key, Type> child;
      if(idx<0)
      { //a index less than the first block is getted.
        //sortin it in the first block.
        idx = 0;
        IndexMultiTable<Key, Type> parents = this;
        while(parents != null)
        { //if(key1 < key[0])
          if(arg0.compareTo(aKeys[0]) <0)
          { aKeys[0] = arg0; //correct the key, key1 will be the less of child.
          }
          parents = parents.parent;
        }
        //NOTE: if a new child will be created, the key[0] is set with new childs key.
      }
      if(idx < sizeBlock)
      { if(! (aValues[idx] instanceof IndexMultiTable))
          stop();
        child = ((IndexMultiTable<Key, Type>)(aValues[idx]));
      }
      else
      { //index after the last block.
        child = null;
        stop();
      }
      if(child !=null && child.sizeBlock == maxBlock)
      { //this child is full, divide it before using
        //int idxH = maxBlock / 2;
        if(child.isHyperBlock)
          stop();
        int idxInChild = Arrays.binarySearch(child.aKeys, arg0);
        if(idxInChild <0){ idxInChild = -idxInChild -1; }
        else{ while(idxInChild <sizeBlock && aKeys[idxInChild].compareTo(arg0) == 0){ idxInChild+=1;}}
        
        IndexMultiTable<Key, Type> right;
        
        right = new IndexMultiTable<Key, Type>(provider);
        if(child.isHyperBlock)
        { Key key0right = separateIn2arrays(child,child, right);
          sortin(idx+1, right.aKeys[0], right);
          if(arg0.compareTo(key0right) >= 0) //key0right)
          { right.put(arg0, obj1);
          }
          else
          { child.put(arg0, obj1);
          }
        }
        else
        {
          sortInSeparated2arrays(idxInChild, arg0, obj1, child, child, right);
          sortin(idx+1, right.aKeys[0], right);
        }
      }
      else 
      { //the child has space.
        child.putOrAdd(arg0, obj1, shouldAdd); 
      }
    }
    else
    {
      if(idx <0)
      { idx = -idx -1;
        sortin(idx, arg0, obj1);  //idx+1 because sortin after found position.            
      }
      else
      { //found
        if(!found || shouldAdd){
          sortin(idx, arg0, obj1);  //idx+1 because sortin after found position.            
        } else {
          aValues[idx] = obj1;   //replace the existing one.
        }
      }
    }
    check();
    return lastObj;
  }

  

  
  
  
  
  private void sortin(int idx, Key key1, Object obj1)
  { if(sizeBlock == maxBlock)
    { //divide the block:
      if(isHyperBlock)
        stop();
      if(parent != null)
      { //it has a hyper block, use it!
        //IndexInteger<Type> hyper = parent; 
        stop();
      }
      else
      { //divide the content of the current block in 2 blocks.
        IndexMultiTable<Key, Type> left = new IndexMultiTable<Key, Type>(provider);
        IndexMultiTable<Key, Type> right = new IndexMultiTable<Key, Type>(provider);
        left.parent = this; right.parent=this;
        sortInSeparated2arrays(idx, key1, obj1, this, left, right);
        //the current block is now a hyper block.
        this.isHyperBlock = true;
        aValues[0] = left;
        aValues[1] = right;
        aKeys[0] = left.aKeys[0]; //minKey__;  //because it is possible to sort in lesser keys.
        aKeys[1] = right.aKeys[0];
        left.ixInParent = 0;
        right.ixInParent = 1;
        for(int idxFill = 2; idxFill < maxBlock; idxFill++)
        { aKeys[idxFill] = maxKey__; 
          aValues[idxFill] = null;
        }
        sizeBlock = 2;
        left.check();right.check();
      }
        
    }
    else
    { if(idx < sizeBlock)
      { System.arraycopy(aKeys, idx, aKeys, idx+1, sizeBlock-idx);
        System.arraycopy(aValues, idx, aValues, idx+1, sizeBlock-idx);
      }
      sizeBlock +=1;
      aKeys[idx] = key1;
      aValues[idx] = obj1;
    }
    check();
  }
  
  
  
  
  
  /**separates the src into two arrays with the half size and sort in the object.
   * 
   * @param idx Primordially index of the obj in the src array. 
   * @param key1 The key value
   * @param obj1 The object
   * @param src The src table
   * @param left The left table. It may be the same as src.
   * @param right The right table.
   */
  private void sortInSeparated2arrays
  ( final int idx, Key key1, Object obj1
  , IndexMultiTable<Key, Type> src 
  , IndexMultiTable<Key, Type> left 
  , IndexMultiTable<Key, Type> right
  )
  {
    left.isHyperBlock = src.isHyperBlock; right.isHyperBlock = src.isHyperBlock;  //copy it. 
            
    final int idxH = maxBlock / 2;
    if(idx < idxH)
    { /**sortin the obj1 in the left table. */
      System.arraycopy(src.aKeys, idxH, right.aKeys, 0, src.sizeBlock - idxH);
      System.arraycopy(src.aValues, idxH, right.aValues, 0, src.sizeBlock - idxH);

      System.arraycopy(src.aKeys, 0, left.aKeys, 0, idx);
      System.arraycopy(src.aValues, 0, left.aValues, 0, idx);
      System.arraycopy(src.aKeys, idx, left.aKeys, idx+1, idxH-idx);
      System.arraycopy(src.aValues, idx, left.aValues, idx+1, idxH-idx);
      left.aKeys[idx] = key1;
      left.aValues[idx] = obj1;

    }  
    else
    { /**sortin the obj1 in the right table. */
      System.arraycopy(src.aKeys, 0, left.aKeys, 0, idxH);
      System.arraycopy(src.aValues, 0, left.aValues, 0, idxH);
      
      int idxR = idx-idxH; //valid for right block.
      System.arraycopy(src.aKeys, idxH, right.aKeys, 0, idxR);
      System.arraycopy(src.aValues, idxH, right.aValues, 0, idxR);
      System.arraycopy(src.aKeys, idx, right.aKeys, idxR+1, src.sizeBlock - idx);
      System.arraycopy(src.aValues, idx, right.aValues, idxR+1, src.sizeBlock - idx);
      right.aKeys[idxR] = key1;
      right.aValues[idxR] = obj1;
    }
    /**Set the sizeBlock and clear the content after copy of all block data,
     * because it is possible that src is equal left or right!
     */
    if(idx < idxH)
    { left.sizeBlock = idxH +1;
      right.sizeBlock = maxBlock - idxH;
    }
    else
    { left.sizeBlock = idxH;
      right.sizeBlock = maxBlock - idxH +1;
    }
    for(int idxFill = left.sizeBlock; idxFill < maxBlock; idxFill++)
    { left.aKeys[idxFill] = maxKey__; 
      left.aValues[idxFill] = null;
    }
    for(int idxFill = right.sizeBlock; idxFill < maxBlock; idxFill++)
    { right.aKeys[idxFill] = maxKey__; 
      right.aValues[idxFill] = null;
    }
    src.check();
    left.check();
    right.check();
  }



  
  /**Deletes the element on ix in the current table.
   * @param ix
   */
  protected void delete(int ix){
    Key keydel = aKeys[ix];
    sizeBlock -=1;
    if(ix < sizeBlock){
      System.arraycopy(aKeys, ix+1, aKeys, ix, sizeBlock-ix);
      System.arraycopy(aValues, ix+1, aValues, ix, sizeBlock-ix);
    }
    aKeys[sizeBlock] = maxKey__;
    aValues[sizeBlock] = null;   //prevent dangling references!
    if(sizeBlock == 0 && parent !=null){
      //this sub-table is empty
      ////
      int ixParent = binarySearchFirstKey(parent.aKeys, 0, parent.sizeBlock, keydel); //, sizeBlock, key1);
      if(ixParent < 0)
      { ixParent = -ixParent-1;  
      }
      parent.delete(ixParent);  //call recursively.
      //it has delete the child table. The table may be referenced by an iterator still.
      //But the iterator won't detect hasNext() and it continoues on its parent iterator too. 
    }
  }
  
  


  /**separates the src into two arrays with the half size .
   * 
   * @param src The src table
   * @param left The left table. It may be the same as src.
   * @param right The right table.
   * @return the first key of the right table.
   */
  private Key separateIn2arrays
  ( IndexMultiTable<Key, Type> src 
  , IndexMultiTable<Key, Type> left 
  , IndexMultiTable<Key, Type> right
  )
  {
    left.isHyperBlock = src.isHyperBlock; right.isHyperBlock = src.isHyperBlock;  //copy it. 
            
    final int idxH = maxBlock / 2;
  
    System.arraycopy(src.aKeys, idxH, right.aKeys, 0, src.sizeBlock - idxH);
    System.arraycopy(src.aValues, idxH, right.aValues, 0, src.sizeBlock - idxH);

    System.arraycopy(src.aKeys, 0, left.aKeys, 0, idxH);
    System.arraycopy(src.aValues, 0, left.aValues, 0, idxH);
    /**Set the sizeBlock and clear the content after copy of all block data,
     * because it is possible that src is equal left or right!
     */
    left.sizeBlock = idxH;
    for(int idxFill = idxH; idxFill < maxBlock; idxFill++)
    { left.aKeys[idxFill] = maxKey__; 
      left.aValues[idxFill] = null;
    }
    right.sizeBlock = maxBlock - idxH;
    for(int idxFill = right.sizeBlock; idxFill < maxBlock; idxFill++)
    { right.aKeys[idxFill] = maxKey__; 
      right.aValues[idxFill] = null;
    }
    src.check();
    left.check();
    right.check();
    return right.aKeys[0];
  }







  /**Delete all content. 
   * @see java.util.Map#clear()
   */
  public void clear()
  {
    for(int ix=0; ix<sizeBlock; ix++){
      if(isHyperBlock){ 
        @SuppressWarnings("unchecked")
        IndexMultiTable subTable = (IndexMultiTable)aValues[ix];
        subTable.clear();
      }
      aValues[ix] = null;
      aKeys[ix] = maxKey__; 
    }
    sizeBlock = 0;
    isHyperBlock = false;
  }







  @SuppressWarnings("unchecked")
  public boolean containsKey(Object key)
  { boolean[] found = new boolean[1];
    return search((Key)key, true, found) !=null || found[0];
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


  //public Type get(Object arg0)
  @SuppressWarnings({ "unchecked" })
  @Override public Type get(Object key1){
    assert(key1 instanceof Comparable<?>);
    return search((Key)key1, true, null);
  }


  /**Searches the object with exact this key or the object which's key is the nearest lesser one.
   * For example if given is "Bx, By, Bz" as keys and "Bya" is searched, the value with the key "By" is returned.
   * The user should check the returned object whether it is matching to the key or whether it is able to use.
   * The found key is unknown outside of this routine but the key or adequate properties should able to get 
   * from the returned value.
   * @param key
   * @return
   */
  //@SuppressWarnings("cast")
  @SuppressWarnings("unchecked")
  public Type search(Key key){ 
    return search(key, false, null);
  }

  
  
  /**Assures that if val1 is a String, the key is converted toString() before comparison.
   * @param val1
   * @param key
   * @return
   */
  protected int compare(Comparable<Key> val1, Key key){
    int cmp;
    if(val1 instanceof String){
      cmp = ((String)val1).compareTo(key.toString());
    } else {
      cmp = val1.compareTo(key);  //compare CharSequence, not only Strings
    }
    return cmp;
  }

  /**Searches the key in the tables.
   * @param key1 The key
   * @param exact if true then returns null and retFound[0] = false if the key was not found
   *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
   * @param retFound If null then not used. If not null then it must initialized with new boolean[1].
   *   If the key was found, the retFound[0] is set to true. If the key is not found, the retFound is not
   *   touched. If the key is found and the value for this key is null, retFound[0] is set to true.
   *   Only with this the {@link #containsKey(Object)} works probably. 
   * @return The exact found value or the non exact found value with key before. 
   *   null if the key is lesser than all other keys (it should the first position).
   *   null if the value for this key is null.
   *   null if exact = true and the key is not found.
   */
  @SuppressWarnings("unchecked")
  public Type search(Key key1, boolean exact, boolean[] retFound)
  { IndexMultiTable<Key, Type> table = this;
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
        table = ((IndexMultiTable<Key, Type>)(table.aValues[idx]));
      }
    }
    int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
    { if(idx < 0){
        if(exact) return null;
        else {
          idx = -idx -2;   //NOTE: access to the lesser element before the insertion point.
        }
      } else {
        if(retFound !=null){ retFound[0] = true; } //idx >=0; }
      }
      if(idx >=0)
      { 
        return (Type)table.aValues[idx];
      }
      else  
      { //not found, before first.
        return null;
      }  
    }
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
  {
    // TODO Auto-generated method stub
    return 0;
  }







  public Collection<Type> values()
  {
    // TODO Auto-generated method stub
    return null;
  }











  public Iterator<Type> iterator()
  {
    return new IteratorImpl(this);
  }



  public Iterator<Type> iterator(Key fromKey)
  {
    return new IteratorImpl(this, fromKey);
  }





  public Type remove(Object arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  
  void stop()
  { //debug
  }
  
  
  
  @SuppressWarnings("unchecked")
  void check()
  { boolean shouldCheck = false;
    if(shouldCheck)
    { if(sizeBlock >=1){ assert1(aValues[0] != null); }
      for(int ii=1; ii < sizeBlock; ii++)
      { assert1(aKeys[ii-1].compareTo(aKeys[ii]) <= 0);
        assert1(aValues[ii] != null);
        if(aValues[ii] == null)
          stop();
      }
      if(isHyperBlock)
      { for(int ii=1; ii < sizeBlock; ii++)
        { assert1(aKeys[ii] == ((IndexMultiTable<Key, Type>)aValues[ii]).aKeys[0]); 
        }
      }
      for(int ii=sizeBlock; ii < maxBlock; ii++)
      { assert1(aKeys[ii] == maxKey__);
        assert1(aValues[ii] == null);
      }
    }  
  }
  
  
  void assert1(boolean cond)
  {
    if(!cond)
    { stop();
      throw new RuntimeException("assertiuon");
    }  
  }

  
  
  /**Binaray search of the element, which is the first with the given key.
   * The algorithm is copied from {@link java.util.Arrays}.binarySearch0(Object[], int, int, key) and modified.
   * @param a
   * @param fromIndex
   * @param toIndex
   * @param key
   * @return
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
      //Comparable<Key> midValLeft = mid >fromIndex ? a[mid-1] : minKey__;  
      int cmp = compare(midVal, key);
      if ( cmp < 0)
      { low = mid + 1;
        //equal = false;
      }
      else { // if(cmp >=0){
        high = mid - 1;   //search in left part also if key before mid is equal
        equal = equal || cmp ==0;  //one time equal set, it remain set.
      }
      /*
      else
      { { return mid;  //midValLeft is lesser, than it is the first element with key!
        }
      }
      */
    }
    if(equal) return low > mid ? low : mid;  //one time found, then it is low or mid 
    else return -(low + 1);  // key not found.
  }


  @Override
  public void putAll(Map<? extends Key, ? extends Type> m)
  {
    for(Map.Entry<? extends Key, ? extends Type> e: m.entrySet()){
      put(e.getKey(), e.getValue());
    }
  }
  
  
  @Override
  public String toString(){
    StringBuilder u = new StringBuilder();
    if(isHyperBlock){ u.append("IdxTableHyperBlock; "); } else { u.append("IndexMultiTable; ");}
    if(sizeBlock >1){ u.append(aKeys[0]).append(" ..").append(sizeBlock).append(".. ").append(aKeys[sizeBlock -1]); }
    else { u.append(aKeys[0]); }
    return u.toString();
  }
  
  
  /**This interface is necessary to provide tables and the minimum and maximum value for any user specific type.
   * For the standard type String use {@link IndexMultiTable#providerString}.
   * @param <Key>
   */
  public interface Provide<Key>{
    Key[] genArray(int size);
    Key genMin();
    Key genMax();
  }

  /**Provider for String keys. Used for {@link #IndexMultiTable(Provide)}. */
  public static final IndexMultiTable.Provide<String> providerString = new IndexMultiTable.Provide<String>(){

    @Override public String[] genArray(int size){ return new String[size]; }

    @Override public String genMax(){ return "\255\255\255\255\255\255\255\255\255"; }

    @Override public String genMin(){ return " "; }
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
      IteratorHelper<Key, Type> helper = tableIter.helper;
      Type value = tableIter.next();
      Key key = tableIter.getKeyForNext();
      return new Entry(key, value); 
    }

    @Override
    public void remove()
    {
      tableIter.helper.table.delete(tableIter.helper.idx);
      tableIter.helper.idx -=1;  //maybe -1 if first was deleted.
      //IndexMultiTable.IteratorHelper<Key, Type> helperTest = tableIter.helper;
      while(tableIter.helper.parentIter !=null && tableIter.helper.table.sizeBlock ==0){
        tableIter.helper = tableIter.helper.parentIter;
        tableIter.helper.idx -=1;  //on idx it is the next, it has deleted the child table!
      }
    }
    
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
  
  
}
