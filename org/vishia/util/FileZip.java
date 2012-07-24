package org.vishia.util;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**Entry in a zip file which can use as a FileRemote.
 * @author Hartmut Schorrig
 *
 */
public class FileZip extends FileRemote
{
  /**The ZipFile data. java.util.zip */
  final ZipFile zipFile;
  
  /**The entry of the file in the zip file. java.util.zip */
  final ZipEntry zipEntry;
  
  /**The path of the file inside the zip file. */
  final String sPathZip;
  
  //final List<ZipEntry> entries = new LinkedList<ZipEntry>();
  
  /**All files which are contained in that directory if it is a directory entry in the zip file
   * or if it is the top node in zipfile. This aggregation is null, if this instance represents only a file entry
   * in the zip file (a leaf in tree).
   */
  TreeNodeUniqueKey<FileZip> children;
  
  public FileZip(File parent){
    super(parent.getName());
    
    ZipFile zipFile = null;
    children = new TreeNodeUniqueKey<FileZip>(null, "/", this);
    try{
      zipFile = new ZipFile(parent);
    } catch(Exception exc){
      zipFile = null;
    }
    this.zipFile = zipFile;
    this.zipEntry = null;
    this.sPathZip = "";
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while(entries.hasMoreElements()){
      ZipEntry entry = entries.nextElement();
      String sPathEntry = entry.getName();
      int sep = sPathEntry.lastIndexOf('/');
      if(sep >=0){
        String sDir = sPathEntry.substring(0, sep);
        String sName = sPathEntry.substring(sep+1);
        if(sName.length() >0){
          TreeNodeUniqueKey<FileZip> dir = children.getCreateNode(sDir, "/");
          FileZip child = new FileZip(zipFile, entry);
          dir.addNodeLeaf(sName, child);
        }
      }
      //this.entries.add(entry);
    }
  }
  
  
  public FileZip(ZipFile zipFile, ZipEntry zipEntry){
    super(zipFile.getName());
    this.zipFile = zipFile;
    this.zipEntry = zipEntry;
    this.sPathZip = zipEntry.getName();
  }
  
  
  @Override public FileZip[] listFiles(){
    int zChildren = children ==null ? 0 :
        (children.childNodes ==null ? 0 : children.childNodes.size())
      + (children.leafData ==null ? 0 : children.leafData.size())   
          ;
    if(zChildren >0){
      int ii = -1;
      FileZip[] ret = new FileZip[zChildren];
      if(children.childNodes !=null) for(TreeNodeBase<FileZip> node: children.childNodes){
        ret[++ii] = node.data;
      }
      if(children.leafData !=null) for(FileZip node: children.leafData){
        ret[++ii] = node;
      }
      return ret;
    } else {
      return null;
    }
  }
  
  
  @Override public boolean isDirectory(){
    return children !=null && children.childNodes !=null;
  }
  
  @Override public boolean exists(){
    return true;
  }
  
  /**Only for test.
   * @param args
   */
  public static void main(String[] args){
    File file = new File("D:/vishia/Java/srcJava_Zbnf.zip");
    FileZip fileZip = new FileZip(file);
    fileZip.listFiles();
  }
  
  
  
  @Override public String toString(){ return sPathZip; }

    
}
