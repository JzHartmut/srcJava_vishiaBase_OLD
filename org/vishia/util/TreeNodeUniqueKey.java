package org.vishia.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 *  * 
 * UML-Diagramm, presentation style see {@link Docu_UML_simpleNotation}:
 * <pre>
 * 
 *                                    TreeNode
 *                                      -name
 *                                        |------data-------> T (data, may be null)
 *       TreeNode <---parent--------------|
 *       (maybe null)                     |------idxChildren----------name*>|
 *                                        |                               TreeNode (children) unique
 *                                        |------unsortedChildren---------*>|
 *                                        |                                 |-------data-------> T 
 *                                        |<------------parent--------------|              (the leaf if another node 
 *                                                                                          is not referenced)
 * </pre>
 * <ul>
 * <li>The TreeNode refers to its node data. It has a key.
 * <li>The TreeNode refers to its children, both with its ambiguous key (sorted) and unsorted.
 * <li>The TreeNode refers to its parent. 
 * </ul>
 * @see TreeNodeBase.
 *
 *
 * @author Hartmut Schorrig
 *
 * @param <T> The type of data of the node.
 */
public class TreeNodeUniqueKey <T> extends TreeNodeBase<T> // implements SortedTree<? extends TreeNodeBase<T>>
{

  /**Version, history and license.
   * <ul>
   * <li>2012-07-20 Hartmut creation: it is similar but not equal to the {@link SortedTreeNode}.
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
  public static final int version = 20120728;

  protected Map<String, TreeNodeUniqueKey<T>> idxChildren;

  //Map<String, TreeNode<T>> idxLeaf;

  protected final String key;
  
  public TreeNodeUniqueKey(TreeNodeUniqueKey<T> parent, String key, T data){
    super(parent, data);
    this.key = key;
  }
  
  
  TreeNodeUniqueKey<T> getNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeUniqueKey<T> child = this;
    for(String name: elements){
      TreeNodeUniqueKey<T> child1 = child.idxChildren.get(name);
      if(child1 == null){ 
        break;
      } else {
        child = child1;
      }
    }
    return child;
  }
  
  
  /**adds a child as leaf.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param newElement The child to add.
   */
  public void addLeaf(String itsKey, T leaf)
  { 
    if(idxChildren == null)
    { idxChildren = new TreeMap<String, TreeNodeUniqueKey<T>>();
    }
    TreeNodeUniqueKey<T> node4Leaf = new TreeNodeUniqueKey<T>(this, itsKey, leaf);
    idxChildren.put(itsKey, node4Leaf);
    //
    super.addNode(node4Leaf);
  }
  

  /**adds a child node.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param newElement The child to add.
   */
  @Override public void addNode(TreeNodeBase<T> newNode)
  { 
    if(idxChildren == null)
    { idxChildren = new TreeMap<String, TreeNodeUniqueKey<T>>();
    }
    if(newNode instanceof TreeNodeUniqueKey){
      TreeNodeUniqueKey<T> newNodeKey = (TreeNodeUniqueKey<T>)newNode;
      idxChildren.put(newNodeKey.key, newNodeKey);
    } else {
      throw new IllegalArgumentException("Node of proper Type expected.");
    }
    //
    super.addNode(newNode);
  }
  

  
  
  /**create a child node and adds it.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param data The data of the node.
   */
  public TreeNodeUniqueKey<T> addNode(String itsKey, T data)
  { 
    TreeNodeUniqueKey<T> node = new TreeNodeUniqueKey<T>(this, itsKey, data);
    addNode(node);
    return node;
  }
  

  
  
  /**Problem: obj?
   * @param path
   * @param separator
   * @return
   */
  TreeNodeUniqueKey<T> xxxgetCreateNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeUniqueKey<T> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        idxChildren = new TreeMap<String, TreeNodeUniqueKey<T>>();
        TreeNodeUniqueKey<T> child1 = new TreeNodeUniqueKey<T>(child, name, null);
        child.addNode(child1);
        child = child1;
      
      } else {
        TreeNodeUniqueKey<T> child1 = child.idxChildren.get(name);
        if(child1 == null){ 
          child1 = new TreeNodeUniqueKey<T>(child, name, null);
          child.addNode(child1);
          child = child1;
        } else {
          child = child1;
        }
      }
    }
    return child;
  }
  
  
  
  
  
  /*
  
  @Override public TreeNodeUniqueKey<T> getChild(String sKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterator<TreeNodeUniqueKey<T>> iterChildren() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterator<TreeNodeUniqueKey<T>> iterChildren(String sKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<? extends TreeNodeBase<T>> listChildren() {
    // TODO Auto-generated method stub
    return childNodes;
  }

  @Override
  public List<TreeNodeUniqueKey<T>> listChildren(String sKey) {
    // TODO Auto-generated method stub
    return null;
  }
  
  */
  
  @Override public String toString(){ return key; }

}
  
  
  
