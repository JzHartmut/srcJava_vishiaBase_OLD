/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2008-01-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.xmlSimple;

import java.util.Map;

import org.vishia.util.SortedTree;
import org.vishia.util.TreeNodeBase;

/**This is a simple variant of processing XML.*/

/**Interface to Access to a XML node. It contains a tree of nodes or text content. 
 * This class is a simple way using XML, some features will not supported yet:
 * <ul><li>Namespaces on attributes.
 * <li>Check of namespace correctness: The user works with the aliases (keys). They will be written in the node.
 *     if the aliases are incorrect, the XML-tree is incorrect.
 * </ul>    
 */ 
public interface XmlNode extends SortedTree<XmlNode>
{  /**Version, history and license.
   * <ul>
   * <li>2012-10-05: The {@link XmlNodeSimple} is removed as dependency, so this class is the same like in the past.
   * <li>2012-10-04: The mainly usage class {@link XmlNodeSimple} is derived from 
   *   {@link org.vishia.util.TreeNodeBase} yet. Because both inherit from {@link SortedTree},
   *   the generic type have to be the same. Up to now it is not possible to use
   *   a indirect related type, the {@link XmlNodeSimple} is used here.  It is not able yet
   *   to mix different implementations of XmlNode in one tree (is it necessary?)
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

  /**creates a XmlNode from the same implementation type as this, but without adding to the XML-tree.
   * The node can be added later, using {@link #addContent(XmlNode)}, because it may be necessary to built
   * a sub tree with the new node first, and than add, if there are multi threads.
   * @param name
   * @param namespaceKey The this-node should know the key, otherwise an XmlException is thrown.
   * @return
   */ 
  public XmlNode createNode(String name, String namespaceKey) throws XmlException;

  /**Sets an attribute by name.
   * 
   * @param name
   * @param value
   */
  public void setAttribute(String name, String value);
  
  /**Adds namespace declaration on the specified node. 
   * 
   * @param name namespace alias (key)
   * @param value namespace uri.
   */
  public void addNamespaceDeclaration(String name, String value);
  
  /**Gets all namespace declaration at this node. */
  public Map<String, String> getNamespaces();

  /**Adds textual content. */
  public XmlNode addContent(String text);
  
  /**adds a child node. 
   * 
   * @param node
   * @return this itself to add something else.
   * @throws XmlException
   */
  public XmlNode addContent(XmlNode node) throws XmlException;
  
  /**adds and returns a new child node.
   * 
   * @param name
   * @param namespaceKey
   * @return the new created node.
   * @throws XmlException
   */
  public XmlNode addNewNode(String name, String namespaceKey) throws XmlException;
  
  /**removes all children, also textual content, but not attributes. */
  public void removeChildren();  
  
  /**returns true if the node is a text, not a XML element. A textual content is also represent as node. 
   * 
   * @return
   */ 
  public boolean isTextNode();
  
  /**Returns the text of the node. 
   * If it isn't a text node, the summary text of all child text-nodes is returned. 
   */
  public String getText();
  
  /**Returns the tagname of the node. If it is a text-node, the text is returned. 
   */
  public String getName();
  
  /**returns the alias or key of the namespace. */
  public String getNamespaceKey();
  
  /**returns the requested attribute or null if it is not existing. */
  public String getAttribute(String name);

  /**returns all attributes. */
  public Map<String, String> getAttributes();

  /**removes the requested attribute. If the attribute is not existing, this method does nothing. */
  public String removeAttribute(String name);

  /**returns the first child with the given name.
   * @param name If the child has a namespace, its key have to be given in form "key:name"
   * @return the node or null.
   * @see org.vishia.util.SortedTree#getChild(java.lang.String)
   */
  //public XmlNode getChild(String name);

  /**gets the parent of the node. */
  //public XmlNode getParent();

  
  
}


