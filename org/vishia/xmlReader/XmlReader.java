package org.vishia.xmlReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.IllegalFormatException;
import java.util.Map;

import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;

/**This is the main class to read an XML file.
 * A configuration file, written in XML too, contains the data path for the XML elements which should be stored in Java data.
 * A main method does not exists because it is only proper to invoke the read routine inside a Java application. 
 * @author Hartmut Schorrig.
 *
 */
public class XmlReader
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
  
  
  /**To store the read configuration. */
  XmlCfg cfg = new XmlCfg();
  
  
  /**Configuration to read a config file. */
  final XmlCfg cfgcfg = new XmlCfg();
  
  
  StringBuilder value = new StringBuilder(10000);  //for some lines in <tag ..>content </tag>
  
  
  int sizeBuffer = 2000;
  
  
  Map<String, String> namespaces = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
   
   
  XmlReader() {
    cfgcfg.xmlnsAssign.put("org.vishia.xmlReader-V1.0", "xmlinput");
    XmlCfg.XmlNode nodes = new XmlCfg.XmlNode();
    nodes.subNodeUnspec = nodes;  //recursively, all children are unspec.
    nodes.new_Node = new DataAccess.DatapathElement("newSubnode()");
    cfgcfg.rootNode.addSubnode("xmlinput:main", nodes);
     
  }   
   
   
  public void readXmlCfg(File input) {
    cfg = new XmlCfg();
    //read
  }
  

  public void readXml(File input, Object output, CharSequence xmlCfg) {
  }

  
  public void readXml(File input, Object output, XmlCfg xmlCfg) {
  
    try {
      StringPartFromFileLines inp = new StringPartFromFileLines(input, sizeBuffer, "encoding", null);
      readXml(inp, output, xmlCfg);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(Exception exc) {
      exc.printStackTrace();
      Debugutil.stop();
    }
  }
  
  
  
  private void readXml(StringPartFromFileLines inp, Object output, XmlCfg cfg1) 
  throws Exception
  { inp.setIgnoreWhitespaces(true);
    inp.seekEnd("<"); //search the first start "<". Skip over all other characters. Only spaces are expected.
    if(inp.scan("?").scanOk()) { //skip over the first "<?xml declaration line ?>
      inp.seekEnd("?>");    //skip over the <? head info ?>. Note: The encoding is regarded from StringPartFromFileLines
      inp.scanOk();    //sets the scan start to this position.
    }
    while(inp.seekEnd("<").found()) {
      inp.scanOk();
      inp.readnextContentFromFile(sizeBuffer/2);
      if(inp.scan().scan("---").scanOk()) { //comment line
       inp.seekEnd("--->");
      }
      else {
        parseElement(inp, output, cfg1.rootNode);
      }
    }
  }



  /**Parse a whole element with all inner content
   * @param inp scanOk-Position after the "<" before the identifier.
   * @param output
   * @param cfg1
   * @throws Exception 
   */
  private void parseElement(StringPartFromFileLines inp, Object output, XmlCfg.XmlNode cfgNode) 
  throws Exception
  {
    if(!inp.scanIdentifier(null, "-:").scanOk()) throw new IllegalArgumentException("tag name expected");
    CharSequence sTag = inp.getLastScannedString();
    
    XmlCfg.XmlNode subCfgNode = cfgNode.subnodes == null ? null : cfgNode.subnodes.get(sTag);
    if(subCfgNode == null) { subCfgNode = cfgNode.subNodeUnspec; }
    final Object subOutput;
    if(subCfgNode !=null) {
      subCfgNode.new_Node.setActualArguments(sTag);
      subOutput = DataAccess.invokeMethod(subCfgNode.new_Node, null, output, true, false);  //create method for a new node.
      //subOutput = subCfgNode.dstNode.access(output, true, false);  //create method for a new node.
    } else {
      subOutput = null; //don't store output. 
    }
    //
    //read all attributes:
    while(inp.scanIdentifier(null, "-:").scan("=").scanOk()) {
      CharSequence sAttrName = inp.getLastScannedString();
      if(!inp.scanQuotion("\"", "\"", null).scanOk()) throw new IllegalArgumentException("attr value expected");
      CharSequence sAttrValue = inp.getLastScannedString();
      int posNs = StringFunctions.indexOf(sAttrName, ':');
      if(posNs >=0) {
        //Namespace
        CharSequence ns = sAttrName.subSequence(0, posNs);
        sAttrName = sAttrName.subSequence(posNs+1, sAttrName.length());
        String nsName = sAttrName.toString();
        if(StringFunctions.equals(ns, "xmlns")){
          String nsValue = sAttrValue.toString();
          namespaces.put(nsName, nsValue);
          sAttrName = null;
        } else {
          String nsValue = namespaces.get(nsName);
          //Todo search defined name in cfg for the namespace.
          sAttrName = nsValue + ":" + sAttrName;
        }
      }
      if(sAttrName !=null) {
        String dstPath = cfgNode.attribs == null ? null : cfgNode.attribs.get(sAttrName);
        if(dstPath == null) { dstPath = cfgNode.attribsUnspec; }
        if(dstPath !=null) {
          setOutputAttr(output, dstPath, sAttrName, sAttrValue);
        }
      }
      inp.readnextContentFromFile(sizeBuffer/2);
    }
    //check content.
    if(inp.scan("/").scan(">").scanOk()) {
      //end of element
    }
    else if(inp.scan(">").scanOk()) {
      //textual content
      do {
        inp.readnextContentFromFile(sizeBuffer/2);
        if(inp.scan("<").scanOk()) {
          parseElement(inp, subOutput, subCfgNode);  //nested element.
        } else {
          parseContent(inp);
        }
      } while( ! inp.scan().scan("<").scan("/").scanOk());
      inp.readnextContentFromFile(sizeBuffer/2);
        
      if(!inp.scanIdentifier(null, "-:").scanOk())  throw new IllegalArgumentException("</tag expected");
      inp.setLengthMax();  //for next parsing
      if(!inp.scan(">").scanOk())  throw new IllegalArgumentException("</tag > expected");
        
    } else {
      throw new IllegalArgumentException("either \">\" or \"/>\" expected");
    }
    inp.setLengthMax();  //for next parsing
  }



  @SuppressWarnings("static-method")
  private void setOutputAttr(Object output, String dstPath, CharSequence name, CharSequence value)
  {
    if(dstPath.equals("@") && output instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> oMap = (Map<String, Object>)output;
      oMap.put("@"+ name, value.toString());
    }
  }



  private void parseContent(StringPartFromFileLines inp)
  {
    inp.lento('<');  //The next "<" is either the end with </tag) or a nested element.
    CharSequence content1 = inp.getCurrentPart();
    StringBuffer content = new StringBuffer(content1); 
    inp.fromEnd();
    int posAmp;
    while( (posAmp  = content.indexOf("&")) >=0) {
      if(StringFunctions.startsWith(content, posAmp+1, posAmp+4, "lt;")) { content.replace(posAmp, posAmp+4, "<");  }
      else if(StringFunctions.startsWith(content, posAmp+1, posAmp+4, "gt;")) { content.replace(posAmp, posAmp+4, ">");  }
      else if(StringFunctions.startsWith(content, posAmp+1, posAmp+4, "amp;")) { content.replace(posAmp, posAmp+4, "&");  }
      else if(StringFunctions.startsWith(content, posAmp+1, posAmp+4, "auml;")) { content.replace(posAmp, posAmp+4, "ä");  }
    }
    
  }

  public static void main(String[] args)
  { XmlReader main = new XmlReader();
    
    main.readXml(new File("D:\\vishia\\graphDesign\\draw.tplxml"), main.cfg.rootNode, main.cfgcfg);
    //main.readXml(new File("D:\\vishia\\graphDesign\\test1.fodg"), null, null);
    Debugutil.stop();
  }


}
