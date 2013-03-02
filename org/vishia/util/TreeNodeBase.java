package org.vishia.util;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**This class is a base class for data, which are organized itself in a tree node structure.
 * The difference to data which are organized in a {@link java.util.TreeMap} for example is:
 * Members of a TreeMap are organized in a tree, but there are not nodes itself. The TreeMap 
 * is one of more possibly access forms. The same data may be organized in another TreeMap with
 * another key at the same time, it may be organized in a List in the same time or in an array.
 * The node of a TreeMap is not visible for usage.
 * <br>
 * Data which based on this class are intrinsic nodes which are beneficial and visible as node.
 * Every node of them has one master key to search it in a tree of nodes.
 * <br><br>
 * A known example for such nodes are elements of a XML tree. Any element has its parent
 * and some children. It is a node in an intrinsic modality. The master key is the tag name
 * combined with the nameSpace key.
 * The XPATH searches nodes in a tree with given tag names.
 * 
 * <br><br> 
 * UML-Diagram, presentation style see {@link Docu_UML_simpleNotation}:
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
 *                                      -key
 *                                        |------data-------> T (data, may be null)
 *       TreeNode <---parent--------------|
 *       (maybe null)                     |------idxChildren-----------key*>|
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
 *             TreeNodeBase           TreeNodeBase             TreeNodeBase  
 *                |                   -key                     !one of the children
 *                |                      |                     -key
 *                |---data-->T           |--data->null            |
 *   <---parent---|                      |--idxChildren-->null    |
 *                |--idxChildren----key*>|                        |---data-->T the data
 *                |                      |--childNodes----------*>|
 *                |                      |                        |--idxChildren--->maybe deeper nodes or null
 *                |                      |                        |--childNodes---->maybe deeper nodes or null
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
 * @param <Data> The type of additional data which are referenced in this node. Use Object if data are not used.
 * @param <DerivedNode> Type of the TreeNode which is build with this class as superclass.
 *   If one will only use the underived TreeNodeBase, use {@link TreeNodeBase.TreeNode} instead.
 * @param IfcType Type used in {@link SortedTree}. It has to be a super class or interface
 *   of the DerivedNode, elsewhere there are errors in runtime. 
 *   Because both DerivedNode extends SortedTree and IfcType extends SortedTree,
 *   the IfcType can be a super type of DerivedNode. But it should be!
 */
public class TreeNodeBase
<DerivedNode extends TreeNodeBase<DerivedNode,Data, IfcType> & SortedTree<IfcType>
, Data
, IfcType extends SortedTree<IfcType> 
> 
implements SortedTree<IfcType>
{

  /**Version, history and license.
   * <ul>
   * <li>2013-03-02 Hartmut chg: Usage of ArrayList instead LinkedList for children- better ability to debug.
   * <li>2012-11-03 Hartmut new: Generic type DerivedNode and IfcType to support derived types of this. The returned
   *   container types have elements of this DerivedNode type or the IfcType. The IfcType is used on methods
   *   which are defined in {@link SortedTree}.
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
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de, www.vishia.org
   * 
   */
  public static final int version = 20120728;

  /**This instance is used if the node is a meta node to refer more as one child with the same key. 
   * 
   */
  private static String metaNodeKey = "--metanode-key--";
  
  protected final String key;
  
  protected TreeNodeBase<DerivedNode,Data, ?> parent;
  
  /**The List of child nodes in order of adding. All nodes in this list are type of the DerivedNode. 
   */
  protected List<DerivedNode> childNodes;
  
  /**The child nodes sorted to the key. The key is given with the child itself, attribute {@link #key}.
   * This index can contain a so named 'meta node' which holds more as one child with the same key.
   * Note that the meta node is not registered in the {@link #childNodes}.
   */
  protected Map<String, TreeNodeBase<DerivedNode,Data,IfcType>> idxChildren;

  
  /**Instances which are a leaf of this node. That leafs should not need a node to wrap it. 
   * The tree is not visible from a leaf because such a leaf is not a TreeNode.
   * If you want to refer from a leaf to its parent, use a node without childNodes and left this list empty.
   * @deprecated. It is too much complexity for a treeNode and not typical for it. Should contain in {@link #data}.
   */
  protected List<Data> leafData;
  
  /**Any additional data associated with this node. */
  public final Data data;
  


  /**Creates a new unbounded node. 
   * @param key The key will be used if the tree is sorted. It can be null,
   *   then the tree is not sorted. The key is used to search this nodes by its key
   *   in the parent node or from any grandparent node with the path.
   *   
   * @param data User data of this node.
   */
  public TreeNodeBase(String key, Data data){
    this.key = key;
    this.parent = null;
    this.data = data;
  }

  
  /**This method has to be overridden in the derived class. It has to be created an instance
   * of the DerivedNode type.
   * @param key see {@link #TreeNodeBase(String, Object)}
   * @param data see {@link #TreeNodeBase(String, Object)}
   * @return Instance of the DerivedNode class
   */
  protected DerivedNode newNode(String key, Data data){
    throw new IllegalArgumentException("This method has to be overridden.");
  }
  
  
  /**adds a leaf.
   * 
   * @param leaf The data to add.
   * @deprecated
   */
  @Deprecated
  public void addLeaf(Data leaf)
  { 
    if(leafData == null){
      leafData = new ArrayList<Data>(); 
    }
    leafData.add(leaf);
  }
  
  

  
  /**create a child node and adds it.
   * This method is only proper to use if the DerivedNode has not a more complex constructor.
   * As opposite one can create a DerivedNode in the application level and invoke {@link #addNode(TreeNodeBase)}
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param leaf The data of the node.
   */
  public DerivedNode addNode(String itsKey, Data leaf)
  {
    DerivedNode childNode = newNode(itsKey, leaf);
    addNode(childNode);
    return childNode;
  }
  
  
  
  /**Adds a given child node.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param leaf The data of the node.
   */
  public void addNode(DerivedNode childNode)
  { if(childNode.parent !=null){
      throw new IllegalArgumentException("Node has a parent, it is contained anywhere other, invoke detach!");
    }
    if(childNode.key == null){
      childNode.parent = this;
    } 
    else {
      if(idxChildren == null)
      { idxChildren = new TreeMap<String, TreeNodeBase<DerivedNode,Data,IfcType>>();
      }
      TreeNodeBase<DerivedNode,Data,IfcType> childNodeFound = idxChildren.get(childNode.key);
      if(childNodeFound == null){
        //only one entry for this key, 
        //because it may be the first one, it is in uncertain 
        //whether it may be more as one leaf with the same key in the future.
        childNode.parent = this;
        idxChildren.put(childNode.key, childNode);
      } else {
        //a node with the same leaf was found.
        TreeNodeBase<DerivedNode,Data,IfcType> metaNode;  //a meta node is needed.
        if(childNodeFound.key == metaNodeKey){
          //it is a meta node already.
          metaNode = childNodeFound;
        } else {
          //it is the only one child with this key. Build a meta node yet:
          metaNode =  new TreeNodeBase<DerivedNode,Data,IfcType>(metaNodeKey, null);
          metaNode.parent = this;
          idxChildren.put(childNode.key, metaNode);  //replaces the exitsting node.
          metaNode.childNodes = new ArrayList<DerivedNode>();
        }
        metaNode.childNodes.add(childNode);
        childNode.parent = metaNode;
      }
    }
    if(childNodes == null){ 
      childNodes = new ArrayList<DerivedNode>();
    }
    childNodes.add(childNode);  //parent refers this or metaNode.
  }
  

  
  
  
  /**Detaches the node from its tree. A node can only be member of one tree of TreeNodeBase, 
   * not member of more as one. Any TreeNodeBase has its {@link #getParent()}.
   * Nevertheless you can refer any node within another structure of data. 
   * <br><br>
   * If a node is detached, it can be added in another tree of the same type.
   */
  public void detach(){
    if(parent !=null){
      parent.childNodes.remove(this);
      if(parent.idxChildren!=null){
        idxChildren.remove(this);
      } 
      if(parent.key == metaNodeKey){
        //remove the child in the real parent.
        parent.parent.childNodes.remove(this);
        //NOTE: It is not found in the idxChildren, only the meta node is there!
        if(parent.childNodes.size()==0){
          //The meta node has not children yet. remove or not?
          parent.parent.idxChildren.remove(parent);
        }
      }
      parent = null;
    }
  }
  
  
  /**Remove all children of this node. The immediate children will be detached from this node.
   * The children of that children would not be detached from its parent. But if the children
   * are not referenced anyway other they are removed from space (garbage collected).
   * Note: Use {@link #detach()} for any child node if it and their sub tree should be used furthermore.
   */
  public void removeChildren(){
    for(TreeNodeBase<DerivedNode,Data,IfcType> child: childNodes){
      child.detach();
    }
  }
  
  
  
  /**Returns the key of this node.
   * @return the key, null if the node is not sorted. 
   */
  public String getKey(){ return key; }
  
  public Data getParentData(){
    if(parent ==null){ return null;}
    else {
      return parent.data;
    }
  }
  
  
  /**Returns any selected node in the tree.
   * @param path The path may have more as one key to traverse into the boughs of the tree.
   * @param separator Separator between key elements in path.
   * @return
   */
  public DerivedNode getNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeBase<DerivedNode,Data,IfcType> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        child = null;
        break;
      }
      TreeNodeBase<DerivedNode,Data,IfcType> child1 = child.idxChildren.get(name);
      if(child1 == null){ 
        child = null;
        break;
      } else {
        child = child1;
      }
    }
    @SuppressWarnings("unchecked")
    DerivedNode retChild = (DerivedNode)child;
    return retChild;
  }
  
  

  
  /**Searches a node with the path, creates it if it is not found.
   * @param path path of keys, see {@link #getNode(String, String)}
   * @param separator The separator in path.
   * @return always a node with given key
   */
  @SuppressWarnings("unchecked")
  DerivedNode getOrCreateNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeBase<DerivedNode,Data,IfcType> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        idxChildren = new TreeMap<String, TreeNodeBase<DerivedNode,Data,IfcType>>();
        DerivedNode child1 = newNode(name, null);
        child.addNode(child1);
        child = child1;
      
      } else {
        TreeNodeBase<DerivedNode,Data,IfcType> child1 = child.idxChildren.get(name);
        if(child1 == null){ 
          child1 = newNode(name, null);
          child.addNode((DerivedNode)child1);
          child = child1;
        } else {
          child = child1;
        }
      }
    }
    return (DerivedNode)child;
  }
  


  
  
  /**Gets the parent of this node or null if it is the root node or it is {@link #detach()}.
   * @see org.vishia.util.SortedTree#getParent()
   */
  @Override public IfcType getParent(){
    @SuppressWarnings("unchecked")
    IfcType ret = (IfcType)parent;
    return ret;
  }

  
  
  /**Returns the node with the given key. If there are more as one node with the same key,
   * the first node is returned. 
   * @param sKey
   * @return null if a node with the key is not referred from this node directly.
   */
  @Override 
  public IfcType getChild(String sKey)
  {
    if(idxChildren == null){
      return null;
    }
    TreeNodeBase<DerivedNode,Data,IfcType> nodeChild = idxChildren.get(sKey);
    if(nodeChild !=null && nodeChild.key == metaNodeKey){
      if(nodeChild.childNodes.size()>=1){
        nodeChild = nodeChild.childNodes.get(0);
      } else {
        nodeChild = null;  //meta node exists but it has not childs because all of them are detached.
      }
    }
    @SuppressWarnings("unchecked")
    IfcType ret = (IfcType)nodeChild;
    return ret;
  }

  
  @Override public String toString(){ return key; }


  @Override
  public Iterator<IfcType> iterChildren()
  { 
    @SuppressWarnings("unchecked")
    Iterator<IfcType> ret = childNodes == null ? null : (Iterator<IfcType>)childNodes.iterator();
    return ret;
  }


  @Override
  public Iterator<IfcType> iterChildren(String sKey)
  { List<IfcType> listChildren = listChildren(sKey);
    return listChildren==null ? null : listChildren.iterator();
    /*
    if(idxChildren ==null){ return null; }
    else { 
      TreeNodeBase<DerivedNode,Data,IfcType> children = idxChildren.get(sKey);
      if(children ==null|| children.childNodes ==null){ return null; }
      else {
        @SuppressWarnings("unchecked")
        Iterator<IfcType> ret = (Iterator<IfcType>)children.childNodes.iterator();
        return ret; 
      }
    }
    */
  }


  @Override
  public List<IfcType> listChildren()
  {
    @SuppressWarnings("unchecked")
    List<IfcType> ret = (List<IfcType>)childNodes;
    return ret;
  }


  /**Searches the child node with the given key.
   * If the child node is a meta node for more as one child, its {@link #childNodes(TreeNodeBase)}
   * list is the proper return value. But if the child node refers only one child,
   * a list with this one element is built temporary because this method should return a list anyway. 
   * @return A List of all children with the given key or null if there is no such child.
   * @see org.vishia.util.SortedTree#listChildren(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override 
  public List<IfcType>listChildren(String sKey)
  { if(idxChildren ==null) { return null; }
    else {
      TreeNodeBase<DerivedNode,Data,IfcType> childMetaNode = idxChildren.get(sKey);
      if(childMetaNode == null){ return null; }
      else{
        List<IfcType> ret;
        if(childMetaNode.key != metaNodeKey){
          //only one child, but a List is expected
          ArrayList<IfcType> childNodes = new ArrayList<IfcType>();
          //IfcType childNode = (IfcType)newNode(sKey, childMetaNode.data);
          childNodes.add((IfcType)childMetaNode);
          ret = childNodes;
        } else {
          ret = (List<IfcType>) childMetaNode.childNodes;
        }
        return ret;
      }
    }
  }


  /**Returns the container with children with the same key.
   * @param sKey
   * @return null if it is not a container.
   */
  public List<DerivedNode>getContainerChildren(String sKey)
  { if(idxChildren ==null) { return null; }
    else {
      TreeNodeBase<DerivedNode,Data,IfcType> childMetaNode = idxChildren.get(sKey);
      if(childMetaNode == null){ return null; }
      else{
        List<DerivedNode> ret;
        if(childMetaNode.key != metaNodeKey){
          return null;
          //only one child, but a List is expected
        } else {
          ret = childMetaNode.childNodes;
        }
        return ret;
      }
    }
  }


  protected List<DerivedNode> childNodes(TreeNodeBase<DerivedNode,Data,IfcType> node){ return node.childNodes; }


  /**This class provides a ready to use TreeNode without additional functionality like TreeNodeBase.
   * The DerivedNode is this Type.
   * @author Hartmut
   *
   * @param <Data>
   */
  public final static class TreeNode<Data> extends TreeNodeBase<TreeNode<Data>, Data,TreeNode<Data>>
  {
    public TreeNode(String key, Data data) {
      super(key, data);
    }
    
    @Override protected TreeNode<Data> newNode(String key, Data data){
      return new TreeNode<Data>(key, data);
    }
  }
}
