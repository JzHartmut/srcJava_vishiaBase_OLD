package org.vishia.xmlSimple;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.TreeNodeBase;


/**This is a simple variant of processing XML.*/

/**Representation of a XML node. It contains a tree of nodes or text content. */ 
public class XmlNodeSimple<UserData> extends TreeNodeBase<XmlNodeSimple<UserData>, UserData, XmlNode> implements XmlNode
{ 
  /**Version, history and license.
   * <ul>
   * <li>2012-11-03 Hartmut Now this class is derived from TreeNodeBase directly. It is a TreeNode by itself.
   * <li>2012-11-01 Hartmut The {@link TreeNodeBase} is used for the node structure. 
   *   Reference {@link #node}. The algorithm to manage the node structure is deployed in the
   *   TreeNodeBase yet. 
   * <li>2008-04-02: Hartmut some changes
   * <li>2008-01-15: Hartmut www.vishia.org creation
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
  public static final int version = 20121104;


  
  /**The tag name of the node or the text if namespaceKey is "$". 
   * Note: It is not the key in the {@link TreeNodeBase}. The key there is namespaceKey:name. */
  final String name;
  
  /**The namespace-key. If it is "$", the node is a terminate text node. */
  String namespaceKey;

  /**Sorted attributes. */
  TreeMap<String, String> attributes;

  /**All nodes, especially child nodes, the parent too. */
  //final TreeNodeBase<XmlNode,XmlNode> node; 
  
  //public UserData data;
  
  /**The List of child nodes and text in order of adding. 
   * Because the interface reference is used, it is possible that a node is another instance else XmlNodeSimple. 
   */
  //List<XmlNode> content;

  /**Sorted child nodes. The nodes are sorted with tag-names inclusive name-space.*/
  //TreeMap<String, List<XmlNode>> sortedChildNodes;
  
  /**List of namespace declaration, typical null because only top elements has it. */
  TreeMap<String, String> namespaces;

  
  /**The parent node. */
  //XmlNode parent;
  
  public XmlNodeSimple(String name)
  { super(name, null);
    this.name = name;
    //node = new TreeNodeBase<XmlNode,XmlNode>(name, this);
    //this.name = name;
  }
  
  public XmlNodeSimple(String name, UserData data)
  { super(name, data);
    this.name = name;
    //node = new TreeNodeBase<XmlNode,XmlNode>(name, this);
    //this.name = name;
  }
  
  public XmlNodeSimple(String text, boolean isText)
  { super("$", null);
    this.name = text;
    //node = new TreeNodeBase<XmlNode,XmlNode>(calcKey(name, namespaceKey), this);
    //this.name = name;
    this.namespaceKey = "$";
  }
  
  public XmlNodeSimple(String name, String namespaceKey, UserData data)
  { super(calcKey(name, namespaceKey), data);
    this.name = name;
    //node = new TreeNodeBase<XmlNode,XmlNode>(calcKey(name, namespaceKey), this);
    //this.name = name;
    this.namespaceKey = namespaceKey;
  }
  
  public XmlNodeSimple(String name, String namespaceKey)
  { super(calcKey(name, namespaceKey), null);
    this.name = name;
    //node = new TreeNodeBase<XmlNode,XmlNode>(calcKey(name, namespaceKey), this);
    //this.name = name;
    this.namespaceKey = namespaceKey;
  }
  
  public XmlNodeSimple(String name, String namespaceKey, String namespace)
  { super(calcKey(name, namespaceKey), null);
    this.name = name;
    //node = new TreeNodeBase<XmlNode,XmlNode>(calcKey(name, namespaceKey), this);
    //this.name = name;
    this.namespaceKey = namespaceKey;
  }
  
  
  private static String calcKey(String name, String namespaceKey){
    String key;  //build the key namespace:tagname or tagname
    if(namespaceKey != null) { key =  namespaceKey + ":" + name; }
    else { key = name; }
    return key;
  }
  
  @Override protected XmlNodeSimple<UserData> newNode(String key, UserData data){
    XmlNodeSimple<UserData> newNode = new XmlNodeSimple<UserData>(key, this.namespaceKey, data);
    return newNode;
    //TreeNodeBase<A, T> node = new TreeNodeBase(key, data);
    //return (A)node;
  }

  
  public XmlNode createNode(String name, String namespaceKey)
  { return new XmlNodeSimple<UserData>(name,namespaceKey);
  }

  
  
  public void setAttribute(String name, String value)
  { if(attributes == null){ attributes = new TreeMap<String, String>(); }
    attributes.put(name, value);
  }
  
  public void addNamespaceDeclaration(String name, String value)
  { if(namespaces == null){ namespaces = new TreeMap<String, String>(); }
    namespaces.put(name, value);
  }
  
  public XmlNode addContent(String text)
  { XmlNodeSimple<UserData> child = new XmlNodeSimple<UserData>(text, true);
    addNode(child);
    return this;
  }
  

  public XmlNode addNewNode(String name, String namespaceKey) throws XmlException
  { XmlNode node = new XmlNodeSimple<UserData>(name, namespaceKey);
    addContent(node);
    return node;
  }
  
  
  
  @SuppressWarnings("unchecked")
  public XmlNode addContent(XmlNode child) 
  throws XmlException 
  { 
    if(child instanceof XmlNodeSimple<?>){
      addNode((XmlNodeSimple<UserData>)child);
    } else {
      String name = child.getName();
      String namespaceKey = child.getNamespaceKey();
      String key = calcKey(name, namespaceKey);
      //TreeNodeBase<XmlNode,XmlNode> childnode = new TreeNodeBase<XmlNode,XmlNode>(key, child);
      XmlNodeSimple<UserData> childnode = new XmlNodeSimple<UserData>(name, key);
      addNode((XmlNodeSimple<UserData>)childnode);
    }
    return this;
  }

  
  
  
  public boolean isTextNode(){ return namespaceKey != null && namespaceKey.equals("$"); }
  
  /**Returns the text of the node. If it isn't a text node, the tagName is returned. */
  public String getText()
  { if(namespaceKey.equals("$"))
    { //it is a text node.
      return name;
    }
    else
    { List<XmlNode> textNodes = listChildren("$");
      String sText = "";
      for(XmlNode textNode: textNodes){
        sText += textNode.getText();
      }
      return sText;
    }
  }
  
  /**Returns the text if it is a text node. If it isn't a text node, the tagName is returned. */
  public String getName(){ return name; }
  
  public String getNamespaceKey(){ return namespaceKey; }
  
  public String getAttribute(String name)
  {
    if(attributes != null)
    { return attributes.get(name);
    }
    else return null;
  }

  public Map<String, String> getAttributes()
  {
    return attributes;
  }

  public Map<String, String> getNamespaces()
  {
    return namespaces;
  }

  public String removeAttribute(String name)
  {
    if(attributes != null)
    { return attributes.remove(name);
    }
    else return null;
  }





  public String toString()
  { if(namespaceKey !=null && namespaceKey.equals("$")) return name;  //it is the text
    else return "<" + name + ">"; //any container
  }

  @Override
  public XmlNodeSimple<UserData> getParent()
  {
    // TODO Auto-generated method stub
    return null;
  }


  
}


