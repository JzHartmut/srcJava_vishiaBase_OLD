package org.vishia.util;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Entry in a zip file which can use as a FileRemote.
 * 
 * @author Hartmut Schorrig
 * 
 */
public class FileZip extends FileRemote {
  private static final long serialVersionUID = 8789821733982259444L;

  final File theFile;

  /** The ZipFile data. java.util.zip */
  final ZipFile zipFile;

  /** The entry of the file in the zip file. java.util.zip */
  final ZipEntry zipEntry;

  /** The path of the file inside the zip file. */
  final String sPathZip;

  // final List<ZipEntry> entries = new LinkedList<ZipEntry>();

  /**
   * All files which are contained in that directory if it is a directory entry
   * in the zip file or if it is the top node in zipfile. This aggregation is
   * null, if this instance represents only a file entry in the zip file (a leaf
   * in tree).
   */
  TreeNodeUniqueKey<FileZip> children;

  public FileZip(File parent) {
    super(parent.getName());
    this.theFile = parent;
    ZipFile zipFile = null;
    children = new TreeNodeUniqueKey<FileZip>(null, "/", this);
    try {
      zipFile = new ZipFile(parent);
    } catch (Exception exc) {
      zipFile = null;
    }
    this.zipFile = zipFile;
    this.zipEntry = null;
    this.sPathZip = "";
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String sPathEntry = entry.getName();
      int sep = sPathEntry.lastIndexOf('/');
      if (sep >= 0) {
        String sDir = sPathEntry.substring(0, sep);
        String sName = sPathEntry.substring(sep + 1);
        if (sName.length() > 0) {
          TreeNodeUniqueKey<FileZip> dir = children.xxxgetCreateNode(sDir, "/");
          FileZip child = new FileZip(theFile, zipFile, entry);
          dir.addLeaf(sName, child);
        } else {
          // a directory entry found, it ends with '/'
          sep = sDir.lastIndexOf('/');
          TreeNodeUniqueKey<FileZip> parentDir;
          if (sep >= 0) {
            parentDir = children.xxxgetCreateNode(sDir, "/");
          } else {
            parentDir = children;
          }
          FileZip zipDir = new FileZip(parentDir.data, theFile, zipFile, entry);
        }
      }
      // this.entries.add(entry);
    }
  }

  public FileZip(FileZip parent, File theFile, ZipFile zipFile, ZipEntry zipEntry) {
    super(null, null, zipFile.getName() + '/' + zipEntry.getName(), null, zipEntry.getSize()
        , zipEntry.getTime(), 0, null); 
    this.sPathZip = zipEntry.getName();
    this.theFile = theFile;
    String sEntryPath = zipEntry.getName();
    int p1, p2 = sEntryPath.length();
    if(sEntryPath.endsWith("/")){
      p2 -=1;
    }
    p1 = sEntryPath.lastIndexOf('/', p2-1);  //maybe -1, then string from 0
    String sEntryName = sEntryPath.substring(p1+1, p2);
    this.children = new TreeNodeUniqueKey<FileZip>(parent.children, sEntryName, this);
    parent.children.addNode(this.children);
    this.zipFile = zipFile;
    this.zipEntry = zipEntry;
  }

  public FileZip(File theFile, ZipFile zipFile, ZipEntry zipEntry) {
    super(zipFile.getName());
    this.sPathZip = zipEntry.getName();
    this.theFile = theFile;
    this.children = null;
    this.zipFile = zipFile;
    this.zipEntry = zipEntry;
  }


  @Override
  public FileZip[] listFiles() {
    int zChildren = children == null ? 0 : (children.childNodes == null ? 0
        : children.childNodes.size())
        + (children.leafData == null ? 0 : children.leafData.size());
    if (zChildren > 0) {
      int ii = -1;
      FileZip[] ret = new FileZip[zChildren];
      if (children.childNodes != null)
        for (TreeNodeBase<FileZip> node1 : children.childNodes) {
          ret[++ii] = node1.data;
        }
      if (children.leafData != null)
        for (FileZip node1 : children.leafData) {
          ret[++ii] = node1;
        }
      return ret;
    } else {
      return null;
    }
  }

  @Override
  public boolean isDirectory() {
    return children != null && children.childNodes != null;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String getName() {
    if (zipEntry != null) {
      return zipEntry.getName();
    } else {
      return theFile.getName();
    }
  }

  @Override
  public FileRemote getParentFile() {
    File parent;
    if (children != null && children.parent != null) {
      parent = children.parent.data;
    } else {
      parent = theFile;
    }
    return FileRemote.fromFile(parent);
  }

  @Override
  public String getParent() {
    File parent = getParentFile();
    return parent.getAbsolutePath();
  }

  class Test extends TreeNodeUniqueKey<FileZip> {

    public Test(TreeNodeUniqueKey<FileZip> parent, String key, FileZip data) {
      super(parent, key, data);
      // TODO Auto-generated constructor stub
      boolean x = isDirectory();
    }
  }

  @Override
  public String toString() {
    return sPathZip;
  }

  /**
   * Only for test.
   * 
   * @param args
   */
  public static void main(String[] args) {
    File file = new File("/home/hartmut/vishia/Java/srcJava_Zbnf.zip");
    FileZip fileZip = new FileZip(file);
    Test test = fileZip.new Test(fileZip.children, "", null);
    // boolean x = test.isDirectory();
    // boolean y = test instanceof FileZip;
    fileZip.listFiles();
  }

}
