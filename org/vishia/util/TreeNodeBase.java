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
 * <br><b>Children with key, sorted selection enabled:</b>
 * <br><br>
 *  * <pre>
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
 * <br>
 * <b>Usage for more as one data with the same key</b>:<br>
 * <pre>
 *                                    !Meta-Node for all nodes 
 *             !parent                !with same key 
 *             TreeNodeUniqueKey      TreeNodeBase             TreeNodeBase or  
 *             -name                     |                     TreeNodeUniqueKey
 *                |                      |                     !the child
 *                |---data-->T           |--data->null            |
 *   <---parent---|                      |--idxChildren-->null    |
 *                |--idxChildren---name*>|                        |---data-->T the data
 *                |                      |--childNodes----------*>|
 *                |                      |                        |--idxChildren--->maybe deeper nodes
 *                |                      |                        |
 *                |<-----------parent----|<---------parent--------|
 *                |                                               |
 *                |--childNodes---------------------------------*>|              
 *                                                                                          
 * </pre>
 * Sometimes a container should refer more as one data with the same key. 
 * That is not possible if the java.util.TreeMap is used, it supports only one key per data.
 * For example for XML data an element can contain more as one sub element with the same tag name
 * <ul>
 * <li>The parent TreeNodeBase refers to all children independent of their key 
 *   with the {@link #childNodes} reference. That is because this list of children represents
 *   the creation order. For example for XML nodes it is the order in the XML file.
 * <li>The parent TreeNodeBase refers to a TreeNodeBase which is a meta node for the key.
 *   It is referenced with the parent's {@link #idxChildren}.
 *   This meta node refers all children with the same key with its {@link #childNodes} list.
 *   The order is the creation order.
 * <li>The field {@link #data} of the meta node is left empty, because the data are located in 
 *   {@link #childNodes}.{@link #data}. The field {@link #idxChildren} is left empty too because
 *   there are no children with other keys than the only one.  
 * <li>The child node refers the meta node as its parent. That is important for detach. 
 *   The parent of the meta node refers the real parent.
 * </ul>

 *
 *
 * @author Hartmut Schorrig
 *
 * @param <T> The type of data of the node.
 */
public class TreeNodeBase<T> implements SortedTree<TreeNodeBase<T>>
{

  /**Version, history and license.
   * <ul>
   * <li>2012-11-01 Hartmut Constructor with parent is faulty because the parent will be set on parent.addNode(...).
   * <li>2012-11-01 Hartmut This class contains the sorted and the unsorted management of nodes
   *   inclusive multiple nodes with the same key. 
   *   This class is used for {@link org.vishia.xmlSimple.XmlNodeSimple} and has reduced the complexity
   *   of that class.
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

  protected final String key;
  
  protected TreeNodeBase<T> parent;
  
  /**The List of child nodes and text in order of adding. 
   * Because the interface reference is used, it is possible that a node is another instance else XmlNodeSimple. 
   */
  protected List<TreeNodeBase<T>> childNodes;
  
  protected Map<String, TreeNodeBase<T>> idxChildren;

  
  /**Instances which are a leaf of this node. That leafs don't need a node to wrap it. 
   * Therefore the tree is not visible from a leaf.
   * If you want to refer from a leaf to its parent, use a node without childNodes and left this list emtpy.
   * See 
   */
  protected List<T> leafData;
  
  public T data;
  


  /**Creates a new unbounded node. 
   * @param key The key will be used if the tree is sorted. It can be null,
   *   then the tree is not sorted. The key is used to search this nodes by its key
   *   in the parent node or from any grandparent node with the path.
   *   
   * @param data User data of this node.
   */
  public TreeNodeBase(String key, T data){
    this.key = key;
    this.parent = null;
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
  
  

  
  /**create a child node and adds it.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param leaf The data of the node.
   */
  public void addNode(String itsKey, T leaf)
  {
    TreeNodeBase<T> childNode = new TreeNodeBase<T>(itsKey, leaf);
    addNode(childNode);
  }
  
  
  
  /**Adds a child node.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param leaf The data of the node.
   */
  public void addNode(TreeNodeBase<T> childNode)
  { if(childNode.parent !=null){
      throw new IllegalArgumentException("Node has a parent, it is contained anywhere other, invoke detach!");
    }
    if(childNode.key == null){
      childNode.parent = this;
    } 
    else {
      if(idxChildren == null)
      { idxChildren = new TreeMap<String, TreeNodeBase<T>>();
      }
      TreeNodeBase<T> metaNode = idxChildren.get(childNode.key);
      if(metaNode == null){
        //only one entry for this key, 
        //because it may be the first one, it is in uncertain 
        //whether it may be more as one leaf with the same key in the future.
        childNode.parent = this;
        idxChildren.put(childNode.key, childNode);
      } else {
        //a node with the same leaf was found.
        if(metaNode.childNodes ==null){
          metaNode.childNodes = new LinkedList<TreeNodeBase<T>>();
        }
        if(metaNode.data !=null && metaNode.childNodes.size() == 0){  //yet an unique key
          //set the yet only one leaf in the childNodes:
          TreeNodeBase<T> node4ChildLast =  new TreeNodeBase<T>(childNode.key, metaNode.data);
          metaNode.childNodes.add(node4ChildLast);
          metaNode.data = null;
        }
        childNode.parent = metaNode;
      }
    }
    if(childNodes == null){ 
      childNodes = new LinkedList<TreeNodeBase<T>>();
    }
    childNodes.add(childNode);  //parent refers this or metaNode.
  }
  

  
  
  
  public void detach(){
    if(parent !=null){
      parent.childNodes.remove(this);
      if(parent.idxChildren!=null){
        idxChildren.remove(this);
      } else {
        if(parent.childNodes.size()==0 && parent.data == null){
          //it is a meta node for multi key nodes
          parent.detach();  //detach the meta node.
        }
      }
      parent = null;
    }
  }
  
  
  public void removeChildren(){
    for(TreeNodeBase<T> child: childNodes){
      child.detach();
    }
  }
  
  
  
  public String getKey(){ return key; }
  
  public T getParentData(){
    if(parent ==null){ return null;}
    else {
      return parent.data;
    }
  }
  
  
  TreeNodeBase<T> getNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeBase<T> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        break;
      }
      TreeNodeBase<T> child1 = child.idxChildren.get(name);
      if(child1 == null){ 
        break;
      } else {
        child = child1;
      }
    }
    return child;
  }
  
  

  
  /**Problem: obj?
   * @param path
   * @param separator
   * @return
   */
  TreeNodeBase<T> getOrCreateNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeBase<T> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        idxChildren = new TreeMap<String, TreeNodeBase<T>>();
        TreeNodeBase<T> child1 = new TreeNodeBase<T>(name, null);
        child.addNode(child1);
        child = child1;
      
      } else {
        TreeNodeBase<T> child1 = child.idxChildren.get(name);
        if(child1 == null){ 
          child1 = new TreeNodeBase<T>(name, null);
          child.addNode(child1);
          child = child1;
        } else {
          child = child1;
        }
      }
    }
    return child;
  }
  


  
  
  

  //@Override
  /**Returns the node with the given key. If there are more as one node with the same key,
   * the first node is returned. 
   * @param sKey
   * @return null if a node with the key is not referred from this node directly.
   */
  @Override public TreeNodeBase<T> getChild(String sKey)
  {
    if(idxChildren == null){
      return null;
    }
    TreeNodeBase<T> nodeChild = idxChildren.get(sKey);
    if(nodeChild !=null && nodeChild.data == null){
      nodeChild = nodeChild.childNodes.get(0);
    }
    return nodeChild;
  }

  
  @Override public String toString(){ return key; }


  @Override
  public Iterator<TreeNodeBase<T>> iterChildren()
  {
    return childNodes.iterator();
  }


  @Override
  public Iterator<TreeNodeBase<T>> iterChildren(String sKey)
  { if(idxChildren ==null){ return null; }
    else { 
      TreeNodeBase<T> children = idxChildren.get(sKey);
      if(children ==null|| children.childNodes ==null){ return null; }
      else { return children.childNodes.iterator(); }
    }
  }


  @Override
  public List<TreeNodeBase<T>> listChildren()
  {
    // TODO Auto-generated method stub
    return childNodes;
  }


  /**Searches the child node with the given key.
   * If the child node is a meta node for more as one child, its {@link #childNodes(TreeNodeBase)}
   * list is the proper return value. But if the child node refers only one child,
   * a list with this one element is build in {@link #childNodes}. 
   * @return A List of all children with the given key or null if there is no such child.
   * @see org.vishia.util.SortedTree#listChildren(java.lang.String)
   */
  @Override public List<TreeNodeBase<T>> listChildren(String sKey)
  {
    if(idxChildren ==null) { return null; }
    else {
      TreeNodeBase<T> childMetaNode = idxChildren.get(sKey);
      if(childMetaNode == null){ return null; }
      else{ 
        if(childMetaNode.childNodes ==null){
          //only one child, but a List is expected
          LinkedList<TreeNodeBase<T>> childNodes = new LinkedList<TreeNodeBase<T>>();
          TreeNodeBase<T> childNode = new TreeNodeBase<T>(sKey, childMetaNode.data);
          childNodes.add(childNode);
          return childNodes;
        } else {
          return childMetaNode.childNodes;
        }
      }
    }
  }


  protected List<TreeNodeBase<T>> childNodes(TreeNodeBase<T> node){ return node.childNodes; }
  
}
