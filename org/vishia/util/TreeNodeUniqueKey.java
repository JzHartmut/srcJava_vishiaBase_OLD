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
 *                                        |------obj-------> T (data, may be null)
 *       TreeNode <---parent-------------|
 *       (maybe null)                     |------idxChildren-------name*>TreeNode (children) unique
 *                                        |                                 |
 *                                        |------unsortedChildren---------*>|
 *                                        |                                 |-------obj-------> T 
 *                                        |<------------parent--------------|              (the leaf if another node 
 *                                                                                          is not referenced)
 * </pre>
 * <ul>
 * <li>The TreeNode refers to its node data. It has a key.
 * <li>The TreeNode refers to its children, both with its ambiguous key (sorted) and unsorted.
 * <li>The TreeNode refers to its parent. 
 *
 *
 *
 *
 * @author Hartmut Schorrig
 *
 * @param <T> The type of data of the node.
 */
public class TreeNodeUniqueKey <T> extends TreeNodeBase<T> // implements SortedTree<? extends TreeNodeBase<T>>
{


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
  public void addNodeLeaf(String itsKey, T leaf)
  { 
    if(idxChildren == null)
    { idxChildren = new TreeMap<String, TreeNodeUniqueKey<T>>();
    }
    TreeNodeUniqueKey<T> node4Leaf = new TreeNodeUniqueKey<T>(this, itsKey, leaf);
    idxChildren.put(itsKey, node4Leaf);
    //
    super.addChildNode(node4Leaf);
  }
  

  /**adds a child node.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param newElement The child to add.
   */
  public void addNode(String itsKey, TreeNodeUniqueKey<T> newNode)
  { 
    if(idxChildren == null)
    { idxChildren = new TreeMap<String, TreeNodeUniqueKey<T>>();
    }
    idxChildren.put(itsKey, newNode);
    //
    super.addChildNode(newNode);
  }
  

  
  
  /**Problem: obj?
   * @param path
   * @param separator
   * @return
   */
  TreeNodeUniqueKey<T> getCreateNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeUniqueKey<T> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        idxChildren = new TreeMap<String, TreeNodeUniqueKey<T>>();
        TreeNodeUniqueKey<T> child1 = new TreeNodeUniqueKey<T>(child, name, null);
        child.addNode(name, child1);
        child = child1;
      
      } else {
        TreeNodeUniqueKey<T> child1 = child.idxChildren.get(name);
        if(child1 == null){ 
          child1 = new TreeNodeUniqueKey<T>(child, name, null);
          child.addNode(name, child1);
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
  
  
  
