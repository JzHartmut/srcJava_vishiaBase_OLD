package org.vishia.xmlReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPartFromFileLines;


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
  
  
  /**Assignment between nameSpace-alias and nameSpace-value gotten from the xmlns:ns="value" declaration in the read XML file. */
   Map<String, String> namespaces = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
   
   
  public XmlReader() {
    //cfgcfg.xmlnsAssign.put("org.vishia.xmlReader-V1.0", "xmlinput");
    XmlCfg.XmlNode nodes = new XmlCfg.XmlNode(cfgcfg, "xfgRoot");
    nodes.subNodeUnspec = nodes;  //recursively, all children are unspec.
    nodes.setNewElementPath("newElement(tag)");
    nodes.addAttribStorePath("xmlinput:data", "setNewElementPath(value)");
    nodes.attribsUnspec = new DataAccess.DatapathElement("addAttribStorePath(name, value)");  //use addAttributeStorePath in the dst node to add.
    cfgcfg.rootNode.subNodeUnspec = nodes;  //accept all nodes in the cfg.xml,  
    //cfgcfg.rootNode.addSubnode("xmlinput:main", nodes);
  }   
   
   
  public void readXmlCfg(File input) {
    cfg = new XmlCfg();
    //read
  }
  

  public void readXml(File input, Object output, CharSequence xmlCfg) {
  }

  
  public void readXml(File input, Object output, XmlCfg xmlCfg) {
    StringPartFromFileLines inp = null;
    try {
      inp = new StringPartFromFileLines(input, sizeBuffer, "encoding", null);
      readXml(inp, output, xmlCfg);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(Exception exc) {
      exc.printStackTrace();
      Debugutil.stop();
    } finally {
      if(inp !=null) { inp.close(); }
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
    //
    //The tag name of the element:
    CharSequence sTag = inp.getLastScannedString();
    //TODO replace alias.
    //
    //search the tag name in the cfg:
    //
    final Object subOutput;
    XmlCfg.XmlNode subCfgNode;
    if(cfgNode == null) {
      subOutput = null;     //this element should not be evaluated.
      subCfgNode = null;
    } else {
      Assert.check(output !=null);
      subCfgNode = cfgNode.subnodes == null ? null : cfgNode.subnodes.get(sTag);
      if(subCfgNode == null) { subCfgNode = cfgNode.subNodeUnspec; }
      if(subCfgNode !=null) { //the tag was found, the xml element is expected.
        subOutput = getDataForTheElement(output, subCfgNode, sTag);
        //
        if(subOutput == null) {
          Debugutil.stop();
        }
        //
      } else {
        subOutput = null; //don't store output. 
      }
    }
    //
    parseAttributes(inp, subOutput, subCfgNode);
    //
    //check content.
    //
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










  private void parseAttributes(StringPartFromFileLines inp, Object output, XmlCfg.XmlNode cfgNode) 
  throws Exception
  {
    //read all attributes:
    while(inp.scanIdentifier(null, "-:").scan("=").scanOk()) {
      final CharSequence sAttrNsNameRaw = inp.getLastScannedString();
      if(!inp.scanQuotion("\"", "\"", null).scanOk()) throw new IllegalArgumentException("attr value expected");
      if(cfgNode !=null && output !=null) {
        CharSequence sAttrValue = inp.getLastScannedString();
        int posNs = StringFunctions.indexOf(sAttrNsNameRaw, ':');
        final CharSequence sAttrNsName;
        if(posNs >=0) {
          //Namespace
          CharSequence ns = sAttrNsNameRaw.subSequence(0, posNs);
          final CharSequence sAttrName = sAttrNsNameRaw.subSequence(posNs+1, sAttrNsNameRaw.length());
          //String nsName = sAttrName.toString();
          if(StringFunctions.equals(ns, "xmlns")){
            String nsValue = sAttrValue.toString();
            namespaces.put(sAttrName.toString(), nsValue);
            sAttrNsName = null;
          } else {
            String nsValue = namespaces.get(ns);  //defined in this read xml file.
            if(nsValue == null) {
              sAttrNsName = null;  //Namespace not registered in the input file, especially "xml".
            } else if(cfgNode.cfg.xmlnsAssign !=null) {
              String nsCfg = cfgNode.cfg.xmlnsAssign.get(nsValue);
              //Todo search defined name in cfg for the nameSpace.
              if(nsCfg == null) { 
                sAttrNsName = null;  //Namespace not in cfg registered. 
              } else {
                sAttrNsName = nsCfg + ":" + sAttrName; //nameSpace alias from cfg file. Unified.
              }
            } else {
              //read the config file, here the xmlnsAssign is null, use the attribute name as given.
              sAttrNsName = sAttrNsNameRaw;
            }
          }
        }
        else {
          sAttrNsName = sAttrNsNameRaw;
        }
        if(sAttrNsName !=null) {
          DataAccess.DatapathElement dstPath = cfgNode.attribs == null ? null : cfgNode.attribs.get(sAttrNsName);
          if(dstPath == null) { dstPath = cfgNode.attribsUnspec; }
          if(dstPath !=null) {
            storeAttrData(output, dstPath, sAttrNsName, sAttrValue);
          }
        }
      }
      inp.readnextContentFromFile(sizeBuffer/2);
    } //while
  }





  /**Invokes the associated method to get/create the appropriate data instance for the element.
   * @param output
   * @param subCfgNode
   * @param sTag
   * @return
   * @throws Exception
   */
  @SuppressWarnings("static-method")
  Object getDataForTheElement( Object output, XmlCfg.XmlNode subCfgNode, CharSequence sTag) 
  {
    Object subOutput;
    if(subCfgNode.elementStorePath == null) { //no attribute xmlinput.data="pathNewElement" is given:
      subOutput = output; //use the same output. No new element in data.
    }
    else {
      try{ 
        int nrArgs = subCfgNode.elementStorePath.nrArgNames();
        Object[] args;
        if(nrArgs >0) {
          args = new Object[nrArgs]; 
          for(int ix = 0; ix < nrArgs; ++ix) {
            String argName = subCfgNode.elementStorePath.argName(ix);
            if(argName.equals("tag")) { args[ix] = sTag; }
            else throw new IllegalArgumentException("argname");
          }
          subOutput = DataAccess.invokeMethod(subCfgNode.elementStorePath, null, output, true, true, args);
        } else {
          subOutput = DataAccess.access(subCfgNode.elementStorePath, output, true, false, false, null);
        }
        if(subOutput == null) {
          System.err.println("getDataForTheElement returns null");
        }
      } catch(Exception exc) {
        subOutput = null;
        System.err.println("getDataForTheElement");
      }
    }
    return subOutput;
  }



  /**Invokes the associated method to store the attribute value.
   * <ul>
   * <li> This method is invoked while reading the cfg.xml to store the attribute value with XmlNode#addAttributeStorePath().
   * <li> This method is invoked while reading the user.xml to store the users attribute value in the users data.
   * </ul>
   * @param output It is an instance of {@link XmlNode} while reading the cfg.xml, it is an user instance while reading the user.xml
   * @param dstPath The dataAccess which should be executed.
   * @param sAttrNsName
   * @param sAttrValue
   */
  @SuppressWarnings("static-method")
  void storeAttrData( Object output, DataAccess.DatapathElement dstPath, CharSequence sAttrNsName, CharSequence sAttrValue) 
  {
    try{ 
      int nrArgs = dstPath.nrArgNames();
      Object[] args;
      if(nrArgs >0) {
        args = new Object[nrArgs]; 
        for(int ix = 0; ix < nrArgs; ++ix) {
          String argName = dstPath.argName(ix);
          if(argName.equals("name")) { args[ix] = sAttrNsName; }
          else if(argName.equals("value")) { args[ix] = sAttrValue; }
          else throw new IllegalArgumentException("argname");
        }
        DataAccess.invokeMethod(dstPath, null, output, true, true, args);
      } else {
        DataAccess.storeValue(dstPath, output, sAttrValue, true);
      }
    } catch(Exception exc) {
      System.err.println("storeAttrData");
    }
  }




  @SuppressWarnings("static-method")
  private void setOutputAttr(Object output, DataAccess.DatapathElement dstPath, CharSequence name, CharSequence value)
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

  public void readCfg(File file) {
    readXml(new File("D:\\vishia\\graphDesign\\draw.tplxml"), this.cfg.rootNode, this.cfgcfg);
    cfg.transferNamespaceAssignment(this.namespaces);
  }



  public void readXml(File file, Object dst) {
    this.readXml(file, dst, this.cfg);
  }  
    
  



}
