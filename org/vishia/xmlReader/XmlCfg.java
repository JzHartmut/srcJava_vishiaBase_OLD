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


  /**Assignment between nameSpace-value and nameSpace-alias gotten from the xmlns:ns="value" declaration in the read cfg.XML file. 
   * If this table is null than the config file is read yet. */
  Map<String, String> xmlnsAssign;


  XmlNode rootNode = new XmlNode(this, "root");



  void transferNamespaceAssignment(Map<String, String> src) {
    xmlnsAssign = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
    for(Map.Entry<String, String > ens: src.entrySet()) {
      String nsKey = ens.getKey();
      String nsPath = ens.getValue();
      xmlnsAssign.put(nsPath, nsKey);  //translate the found path in the XML source to the nameSpace key used in the config.xml 
    }
    src.clear();
    
  }

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
    DataAccess.DatapathElement elementStorePath;
  
    /**Key (attribute name with xmlns:name) and reflection path to store the attribute value.
     * Attributes of the read input which are not found here are not stored.
     */
    Map<String, DataAccess.DatapathElement> attribs;
    
    /**If set, the attrib dst for not found attributes to store in a common way. */
    DataAccess.DatapathElement attribsUnspec;
    
    /**Key (tag name with xmlns:name) and configuration for a sub node.
     * Nodes of the read input which are not found here are not stored.
     */
    Map<String, XmlNode> subnodes;
    
    /**If set, the subnode for not found elements to store in a common way.. */
    XmlNode subNodeUnspec;
    
    /**Reflection path to store the content as String. If null than the content won't be stored. */
    String content;
  
    final XmlCfg cfg;
    
    final String tag;
  
    XmlNode(XmlCfg cfg, String tag){ this.cfg = cfg; this.tag = tag; }
  
    /**Sets the path for the "new element" invocation.
     * @param dstPath either a method or an access to a field.
     */
    public void setNewElementPath(String dstPath) {
      elementStorePath = new DataAccess.DatapathElement(dstPath);
    }
  
    /**This method is invoked from the xml configuration reader to create a new attribute entry for the found attribute.
     * @param key ns:name of the found attribute in the cfg.xml 
     * @param dstPath datapath which is found as value in the cfg.xml. The datapath is used for the user.xml to store the attribute value. 
     */
    public void addAttribStorePath(String key, String dstPath) {
      if(attribs == null) { attribs = new IndexMultiTable<String, DataAccess.DatapathElement>(IndexMultiTable.providerString); }
      attribs.put(key, new DataAccess.DatapathElement(dstPath));
    }

    void addSubnode(String key, XmlNode node) {
      if(subnodes == null) { subnodes = new IndexMultiTable<String, XmlNode>(IndexMultiTable.providerString); }
      subnodes.put(key, node);
    }
  
  
    /**This method is invoked from the xml configuration reader to create a new subNode for a found elmeent.
     * @param name ns:tag of the found element in the cfg.xml 
     * @return the sub node
     */
    XmlNode newElement(CharSequence name) {
      String sname = name.toString();
      XmlNode subNode = new XmlNode(cfg, sname);  //A sub node for the config.
      //subNode.elementStorePath = this.elementStorePath;  //the same routine to create the next sub node.
      //subNode.subNodeUnspec = this.subNodeUnspec;
      //subNode.attribsUnspec = this.attribsUnspec;
      //subNode.attribs = this.attribs;
      addSubnode(sname, subNode);
      return subNode;
    }
  
  
  
  
  
  }

}
