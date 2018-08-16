package org.vishia.xmlReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.util.IndexMultiTable;

public class TreeContentFromXmlReader
{
  final String tag;
  
  Map<String, TreeContentFromXmlReader> nodes = new IndexMultiTable<String, TreeContentFromXmlReader>(IndexMultiTable.providerString);
  
  List<String> attribs = new ArrayList<String>();
  
  public TreeContentFromXmlReader(String tag){ this.tag = tag; }
  
  public TreeContentFromXmlReader addElement(String tag) { 
    TreeContentFromXmlReader ret = nodes.get(tag); //use existent one with same tag to strore further content.
    if(ret == null) {
      ret = new TreeContentFromXmlReader(tag); 
      nodes.put( tag, ret);
    }
    return ret; 
  }
  
  
  public void setAttribute(String name) { attribs.add(name); }

}
