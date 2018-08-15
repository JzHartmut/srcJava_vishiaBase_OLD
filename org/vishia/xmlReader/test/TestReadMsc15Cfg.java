package org.vishia.xmlReader.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.util.IndexMultiTable;
import org.vishia.xmlReader.XmlCfg;
import org.vishia.xmlReader.XmlReader;

public class TestReadMsc15Cfg
{

  public static class Content {
    
    final String tag;
    
    Content(String tag){ this.tag = tag; }
    
    Map<String, Content> nodes = new IndexMultiTable<String, Content>(IndexMultiTable.providerString);
    
    List<String> attribs = new ArrayList<String>();
    
    public Content addElement(String tag) { Content ret = new Content(tag); nodes.put( tag, ret); return ret; }
    
    
    public void setAttribute(String name) { attribs.add(name); }
    
  }
  
  
  
  
  public static void main(String args[]) {
    
    
    XmlReader xmlReader = new XmlReader();
    
    File fXmlIn = new File("c:/Users/hartmut/Documents/Visual Studio 2015/Settings/CurrentSettings.vssettings");
    
    Content data = new Content("root");
    
    xmlReader.readXml(fXmlIn, data, XmlCfg.newCfgReadStruct());
    
    
  }
  
  
  
}
