package org.vishia.xmlReader;

import java.util.Map;

import org.vishia.util.DataAccess;
import org.vishia.util.IndexMultiTable;

/**This class contains the configuration data to assign xml elements to Java data.
 * @author Hartmut Schorrig
 *
 */
public class XmlCfg
{
  /**Version, License and History:
   * <ul>
   * <li>2011-09-25 created.
   * </ul>
   * 
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
  public static final String version = "2016-09-25";


  Map<String, String> xmlnsAssign = new IndexMultiTable<String, String>(IndexMultiTable.providerString);

  Map<String, String> elements = new IndexMultiTable<String, String>(IndexMultiTable.providerString);


  XmlNode rootNode = new XmlNode();

  /**This class describes one node as pattern how the content of a parsed xml file should be stored. 
   * @author hartmut
   *
   */
  public static class XmlNode
  {
    /**Reflection path where the content of that node will be stored.
     * Because of more as one sub node can be present in the parsed file the destination is usual a list or a map.
     * If a map is found the key is stored as key in that map too.
     * 
     */
    DataAccess.DatapathElement new_Node;
  
    /**Key (attribute name with xmlns:name) and reflection path to store the attribute value.
     * Attributes of the read input which are not found here are not stored.
     */
    Map<String, String> attribs;
    
    /**If set, the attrib dst for not found attributes to store in a common way. */
    String attribsUnspec;
    
    /**Key (tag name with xmlns:name) and configuration for a sub node.
     * Nodes of the read input which are not found here are not stored.
     */
    Map<String, XmlNode> subnodes;
    
    /**If set, the subnode for not found elements to store in a common way.. */
    XmlNode subNodeUnspec;
    
    /**Reflection path to store the content as String. If null than the content won't be stored. */
    String content;
  
    void addAttrib(String key, String dstPath) {
      if(attribs == null) { attribs = new IndexMultiTable<String, String>(IndexMultiTable.providerString); }
      attribs.put(key, dstPath);
    }

    void addSubnode(String key, XmlNode node) {
      if(subnodes == null) { subnodes = new IndexMultiTable<String, XmlNode>(IndexMultiTable.providerString); }
      subnodes.put(key, node);
    }
  
  
    /**This method is invoked from the xml configuration reader to define a sub node.
     * @return
     */
    XmlNode newSubnode(CharSequence name) {
      XmlNode subNode = new XmlNode();
      addSubnode(name.toString(), subNode);
      return subNode;
    }
  
  }

}
