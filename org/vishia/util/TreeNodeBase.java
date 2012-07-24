package org.vishia.util;

import java.util.LinkedList;
import java.util.List;
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
public class TreeNodeBase<T> {

  protected TreeNodeBase<T> parent;
  
  /**The List of child nodes and text in order of adding. 
   * Because the interface reference is used, it is possible that a node is another instance else XmlNodeSimple. 
   */
  protected List<TreeNodeBase<T>> childNodes;
  
  /**Instances which are a leaf of this node. That leafs don't need a node to wrap it. 
   * Therefore the tree is not visible from a leaf.
   * If you want to refer from a leaf to its parent, use a node without childNodes and left this list emtpy.
   * See 
   */
  protected List<T> leafData;
  
  public final T data;
  


  public TreeNodeBase(TreeNodeBase<T> parent, T data){
    this.parent = parent;
    this.data = data;
  }

  
  /**adds a leaf.
   * 
   * @param leaf The data to add.
   */
  public void addLeaf(T leaf)
  { 
    if(leafData == null){
      leafData = new LinkedList<T>(); 
    }
    leafData.add(leaf);
  }
  
  
  /**adds a node.
   * 
   * @param leaf The data to add.
   */
  public void addChildNode(T data)
  { 
    if(childNodes == null){
      childNodes = new LinkedList<TreeNodeBase<T>>(); 
    }
    TreeNodeBase<T> node = new TreeNodeBase<T>(this, data);
    childNodes.add(node);
  }
  
  
  /**adds a node.
   * 
   * @param leaf The data to add.
   */
  public void addChildNode(TreeNodeBase<T> node)
  { 
    if(childNodes == null){
      childNodes = new LinkedList<TreeNodeBase<T>>(); 
    }
    childNodes.add(node);
  }
  
  
  public void detach(TreeNodeBase<T> node){
    if(node.parent !=null){
      node.parent.childNodes.remove(node);
      node.parent = null;
    }
  }
  
  
}
