package org.vishia.util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.vishia.util.FileRemote.Callback;
import org.vishia.util.FileRemote.FileRemoteAccessorSelector;

/**
 *  * 
 * UML-Diagramm, presentation style see {@link Docu_UML_simpleNotation}:
 * <pre>
 * 
 *                                    TreeNodeBase
 *                                        |---data---> T (may be null)
 *       TreeNodeBase <---parent----------|
 *       (maybe null)                     |                    TreeNodeBase
 *                                        |                        |
 *                                        |------childNodes------*>|
 *                                        |                        |---data------> T 
 *                                        |<------------parent-----|        (a leaf if it hasn't 
 *                                        |                                  childNodes)
 *                                        |----leafData-----*> T (leafs)                                                  
 * </pre>
 * <ul>
 * <li>The TreeNode refers to its node data.
 * <li>The TreeNode refers to its children, 
 * <li>The TreeNode refers to its parent. 
 * </ul>
 * The data may refer its associated node. It may be a final reference (aggregation).
 * That means, the data represents a node. Then both instances should be joined in the
 * constructor of data:
 * Sample:
 * <pre>
 * class MyData{
 *   final TreeNodeBase node;
 *   MyData(MyData parent){
 *     this.node = new TreeNodeBase(parent.node, this);
 *   }
 * }
 * </pre>  
 *
 *
 * @author Hartmut Schorrig
 *
 * @param <T> The type of data of the node.
 */
public class TreeNodeBase<T> {

  /**Version, history and license.
   * <ul>
   * <li>2012-07-20 Hartmut creation: it is the base class for a node which is unsorted firstly.
   *   The derived class {@link TreeNodeUniqueKey} is sorted than.
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
  public void addNode(T data)
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
  public void addNode(TreeNodeBase<T> node)
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
