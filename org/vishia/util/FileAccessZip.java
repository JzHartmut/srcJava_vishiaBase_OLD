package org.vishia.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileAccessZip extends FileRemoteAccessor // extends FileRemoteAccessorLocalFile
{
  /**Version, history and license.
   * <ul>
   * <li>2012-07-28 Hartmut completed. The class FileZip is not neccessary yet.
   * <li>2012-07-21 Hartmut creation: See {@link FileRemoteAccessor}. Support files in zip-File
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
  public static final int version = 20120721;
  
  
  private static FileAccessZip instance;
  
  //@Override 
  public FileRemote[] XXXlistFiles(FileRemote parent){
    FileZipData data = (FileZipData)parent.oFile;
    int zChildren = data == null ? 0 : data.children == null ? 0 : (data.children.childNodes == null ? 0
        : data.children.childNodes.size())
        + (data.children.leafData == null ? 0 : data.children.leafData.size());
    FileRemote[] retFiles = new FileRemote[zChildren]; //may be [0]
    if (zChildren > 0) {
      int ii = -1;
      if (data.children.childNodes != null){
        for (TreeNodeBase.TreeNode<FileRemote> node1 : data.children.childNodes) {
          retFiles[++ii] = node1.data;
      } }
      if (data.children.leafData != null){
        for (FileRemote node1 : data.children.leafData) {
          retFiles[++ii] = node1;
      } }
    }
    return retFiles;
  }
  
  /**Returns the singleton instance of this class.
   * Note: The instance will be created and the thread will be started if this routine was called firstly.
   * @return The singleton instance.
   */
  public static FileAccessZip getInstance(){
    if(instance == null){
      instance = new FileAccessZip();
    }
    return instance;
  }
  

  
  /**It opens a zip file for reading. Some {@link FileRemote} instances are created, 
   * one for each zip file entry. 
   * @param fileZip
   * @return A new instance which refers all children. The children are the files inside 
   *   the zip files.
   */
  public static FileRemote openZipFile(FileRemote fileZip){
    FileZipData dataParent = new FileZipData();
    FileAccessZip zipAccess = getInstance();
    int parentProperties = FileRemote.mDirectory | FileRemote.mExist | FileRemote.mCanRead ; //| FileRemote.mChildrenGotten;
    String sDirParent = fileZip.getAbsolutePath() + '/';
    FileRemote fileParent = new FileRemote(zipAccess, fileZip, sDirParent, null, fileZip.length(),fileZip.lastModified(), parentProperties, dataParent);
    dataParent.children = new TreeNodeBase.TreeNode<FileRemote>("/", fileParent);
    ZipFile jZipFile = null;
    try {
      jZipFile = new ZipFile(fileZip);
    } catch (Exception exc) {
      jZipFile = null;
    }
    dataParent.theFile = fileZip;
    dataParent.zipFile = jZipFile;
    dataParent.zipEntry = null;
    Enumeration<? extends ZipEntry> entries = jZipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String sPathEntry = entry.getName();
      int sep = sPathEntry.lastIndexOf('/');
      String sDirInZip;
      String sNameChild;
      int zipEntryProperties = FileRemote.mTested | FileRemote.mCanRead | FileRemote.mExist ;
      if (sep >= 0) {
        sDirInZip = sPathEntry.substring(0, sep);  //without '/'
        sNameChild = sPathEntry.substring(sep + 1); //after '/'
        if(sNameChild.length() ==0){
          //a directory in the zip file, it ends with '/'
          zipEntryProperties |= FileRemote.mDirectory ; // | FileRemote.mChildrenGotten;
          sep = sDirInZip.lastIndexOf('/');
          sNameChild = sDirInZip.substring(sep + 1); //after '/'
          if(sep >=0){
            sDirInZip = sDirInZip.substring(0, sep);  //without '/'
          } else {
            sDirInZip = "";
          }
        } else {
          zipEntryProperties |= FileRemote.mFile ;
        }
      } else { //a file in zipfile
        zipEntryProperties |= FileRemote.mFile ;
        sNameChild = sPathEntry;
        sDirInZip = null;
      }
      TreeNodeBase.TreeNode<FileRemote> parentDirNode;
      if (sep >= 0) {
        parentDirNode = dataParent.children.getNode(sDirInZip, "/");
      } else {
        parentDirNode = dataParent.children;
      }
      FileZipData dataChild = new FileZipData(); //theFile, zipFile, entry);
      dataChild.theFile = fileZip;
      dataChild.zipFile = jZipFile;
      dataChild.zipEntry = entry;
      String sDirChild = sDirParent + sDirInZip;
      long sizeChild = entry.getSize();
      long dateChild = entry.getTime();
      FileRemote dir = parentDirNode.data;
      FileRemote fileChild = new FileRemote(zipAccess, dir, sDirChild, sNameChild, sizeChild, dateChild, zipEntryProperties, dataChild);
      if((zipEntryProperties & FileRemote.mDirectory) !=0){
        dataChild.children = new TreeNodeBase.TreeNode<FileRemote>(sNameChild, fileChild);
        parentDirNode.addNode(dataChild.children);
      } else {
        parentDirNode.addNode(sNameChild, fileChild);        
      }
    }
    return fileParent;
  }
  


  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override public void refreshFileProperties(FileRemote file, FileRemote.CallbackEvent callback) {
    // TODO Auto-generated method stub
    if(callback !=null){
      callback.sendEvent(FileRemote.CallbackCmd.done);
    }
  }

  @Override public void refreshFilePropertiesAndChildren(FileRemote file, FileRemote.CallbackEvent callback) {
    FileZipData data = (FileZipData)file.oFile;
    int zChildren = data == null ? 0 : data.children == null ? 0 : (data.children.childNodes == null ? 0
        : data.children.childNodes.size())
        + (data.children.leafData == null ? 0 : data.children.leafData.size());
    file.children = new FileRemote[zChildren]; //may be [0]
    if (zChildren > 0) {
      int ii = -1;
      if (data.children.childNodes != null){
        for (TreeNodeBase.TreeNode<FileRemote> node1 : data.children.childNodes) {
          file.children[++ii] = node1.data;
      } }
      if (data.children.leafData != null){
        for (FileRemote node1 : data.children.leafData) {
          file.children[++ii] = node1;
      } }
    }
    if(callback !=null){
      callback.sendEvent(FileRemote.CallbackCmd.done);
    }
  }

  
  
  
  @Override public List<File> getChildren(FileRemote file, FileFilter filter){
    FileZipData data = (FileZipData)file.oFile;
    List<File> list = new ArrayList<File>();
    int zChildren = data == null ? 0 : data.children == null ? 0 : (data.children.childNodes == null ? 0
        : data.children.childNodes.size())
        + (data.children.leafData == null ? 0 : data.children.leafData.size());
    if (zChildren > 0) {
      int ii = -1;
      if (data.children.childNodes != null){
        for (TreeNodeBase.TreeNode<FileRemote> node1 : data.children.childNodes) {
          if ((filter == null) || filter.accept(node1.data)){
            list.add(node1.data);
          }
      } }
      if (data.children.leafData != null){
        for (FileRemote node1 : data.children.leafData) {
          if ((filter == null) || filter.accept(node1)){
            list.add(node1);
          }
      } }
    }
    return list;
  }
  
  
  
  @Override public boolean delete(FileRemote file, FileRemote.CallbackEvent callback){
    if(callback !=null){
      callback.sendEvent(FileRemote.CallbackCmd.errorDelete);
    }
    return false;   // not implement: changing of file.
  }
  
  @Override
  public ReadableByteChannel openRead(FileRemote file, long passPhase) {
    // TODO Auto-generated method stub
    return null;
  }

  
  @Override public InputStream openInputStream(FileRemote file, long passPhase){
    FileZipData data = (FileZipData)file.oFile;
    ///
    try{ 
      InputStream stream = data.zipFile.getInputStream(data.zipEntry);
      return stream;
    } catch(IOException exc){
      return null;
    }
    
  }
  


  
  
  @Override
  public WritableByteChannel openWrite(FileRemote file, long passPhase) {
    // TODO Auto-generated method stub
    return null;
  }

  
  @Override public FileRemote.CmdEvent prepareCmdEvent(FileRemote.CallbackEvent evBack){
    return null; //TODO
  }


  

  @Override
  public boolean isLocalFileSystem() {
    // TODO Auto-generated method stub
    return true;
  }
  
  
  
  static class FileZipData
  {
    File theFile;
    ZipFile zipFile;
    ZipEntry zipEntry;
    TreeNodeBase.TreeNode<FileRemote> children;
  }

  
}
