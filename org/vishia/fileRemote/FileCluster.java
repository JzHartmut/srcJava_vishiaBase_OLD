package org.vishia.fileRemote;

import java.util.Iterator;
import java.util.ListIterator;

import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;

/**This class combines some {@link FileRemote} instances for common usage.
 * It ensures that the same FileRemote object is used for the same string given path.
 * @author Hartmut Schorrig
 *
 */
public class FileCluster
{
  /**Version, history and license.
   * <ul>
   * <li>2014-12-20 Hartmut bugfix: Some files were created more as one with the same path. It were files with lesser pathname 
   *   which are not found in the {@link #idxPaths}: fix: Check backward, therefore the {@link IndexMultiTable#iterator(Comparable)}
   *   is a {@link ListIterator} up to now. 
   * <li>2013-06-15 Hartmut bugfix: create a FileRemote as directory. It is the convention, that the 
   *   property whether or not a FileRemote is a directory does not depend on a {@link FileRemote#isTested()}.
   * <li>2013-05-05 Hartmut chg: get(...) now renamed to getFile(...). {@link #getFile(CharSequence, CharSequence, boolean)}
   *   checks whether a parent instance or deeper child instance is registered already and use that.
   * <li>2013-05-05 Hartmut new: {@link #check(CharSequence, CharSequence)}  
   * <li>2013-04-22 Hartmut created. The base idea was: Select files and use this selection in another part of application.
   *   It should be ensured that the same instance is used for the selection and other usage.
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
  public static final int version = 20130505;
  
  /**This index contains the association between paths and its FileRemote instances.
   */
  IndexMultiTable<String, FileRemote> idxPaths = new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);
  

  /**Number of selected bytes in all selected files. */
  long[] selectBytes = new long[2];
  
  /**Number of selected files. */
  int[] selectFiles = new int[2];
  
  
  /**The directory where the selection should be done.
   * 
   */
  FileRemote dirBaseOfSelection;

  
  public FileCluster(){
  }

  
  
  /**Gets the existing directory instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getDir( final CharSequence sPath){
   return(getFile(sPath, null));
  }

  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getFile( final CharSequence sDirP, final CharSequence sName){
    return getFile(sDirP, sName, false);
  }  
  
  
  /**Gets the existing file instance with this path from the FileCluster or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate.
   * The path is not checked against the file system. 
   */
  public FileRemote getFile( final CharSequence sDirP, final CharSequence sName, boolean strict){
    CharSequence sDir = FileSystem.normalizePath(sDirP); //sPath.replace('\\', '/');
    String ssDir = sDir.toString();
    //Sets the iterator after the exact found position or between a possible position:
    FileRemote dirRet; // = idxPaths.search(sDir.toString());
    int flagDir = sName == null ? FileRemote.mDirectory : 0;  //if name is not given, it is a directory. Elsewhere a file.
    boolean bFound = false;
    boolean putit;
    ListIterator<FileRemote> iter = idxPaths.iterator(ssDir);
    if(!iter.hasPrevious()) {
      //this occurs only if a path lesser than the sDir does not exists, then any parent is not registered.
      //Register this path as first occurrence. 
      dirRet = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
      idxPaths.put(ssDir, dirRet);
      bFound = true;
      putit = false;
    } else {
      do {
        dirRet = iter.previous();
        putit = true;
        bFound = false;
        String sPathRet = dirRet.getAbsolutePath();
        int zPathRet = sPathRet.length();
        if(StringFunctions.startsWith(sDir,sPathRet)) {  //maybe equal too, check whether sPathRet is a parent dir of sDir
          if(sPathRet.length() < sDir.length()){ //any super directory found.
            if(sDir.charAt(zPathRet) == '/'){    //is it that?
              //any directory of the file was found. Create the child directory.
              StringPart pathchild = new StringPart(sDir, zPathRet+1, sDir.length());
              dirRet = dirRet.child(pathchild);
              putit = false;  //it is existed as child of any file in the cluster.
              bFound = true;
            } else { //other directory name, maybe shorter for ex. "path" vs. "path2".
              //dirRet = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
            }
          } else {
            //the same pathm found.
            putit = false; //it is the same, found.
            bFound = true;
          }
        } else if(StringFunctions.startsWith(sPathRet, sDir)) {  //check whether dir is a parent of sPathRet
          //because no other parent is found, entry it but refer in parent too.
          int zDir = sDir.length();
          while( (zPathRet = sPathRet.lastIndexOf('/'))
                 >= zDir) {
            sPathRet = sPathRet.substring(0, zPathRet);
            if(dirRet.parent ==null) {
              FileRemote parent = new FileRemote(this, dirRet.device, null, sPathRet, 0, 0, 0, 0, flagDir, null, true);
              parent.putNewChild(dirRet);
              dirRet = parent;
            } else {
              //it should be the existing parent:
              dirRet = dirRet.parent;
              if(!dirRet.getAbsolutePath().equals(sPathRet)) 
              { throw new IllegalStateException("FileCluster - faulty parent found."); }
            }
            idxPaths.put(sPathRet, dirRet);  //Store it for later search.
          }
          bFound = true;
        }
        if(!bFound) {
          //another directory
          if(!iter.hasPrevious()) {
            dirRet = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
            bFound = true;
            putit = false;
          }
        }
      } while(!bFound);
        if(putit){ 
          idxPaths.put(sDir.toString(), dirRet);
          //check whether next entries in the FileCluster are children of this
          //and register it as children.
          iter.next(); //start with next entry, not with its own.
          int zDir = sDir.length();
          //zDir should be the position of the '/' in the child's paths.
          //if sDir is a root directory, it contains the '/' on end.
          //therefore reduce zDir, if sDir ens with '/':
          if(sDir.charAt(zDir-1) == '/'){
            zDir -=1;  
          }
          while(iter.hasNext()){
            FileRemote childNext = iter.next();
            String pathNext = childNext.getAbsolutePath();
            int zpath = pathNext.length();
            if(zDir < zpath && StringFunctions.startsWith(pathNext, sDir)){
              CharSequence sChild = pathNext.subSequence(zDir+1, zpath);
              int pos1 = 0, pos2;
              FileRemote dir2 = dirRet; //parent of any child level
              while( (pos2 = StringFunctions.indexOf(sChild, '/', pos1))  >pos1){
                CharSequence sChild2 = sChild.subSequence(pos1, pos2);  //child levels
                //dir2 = dir2.child(sChild2, FileRemote.mDirectory, 0,0,0,0,0);  //child level assigned to parent level, maybe exising, maybe a new FileRemote.
                FileRemote child = dir2.getChild(sChild2);
                if(child == null){
                  child = dir2.internalAccess().newChild(sChild2,0, 0, 0, 0 , FileRemote.mDirectory, null);
                  dir2.putNewChild(child);
                }
                dir2 = child;
                pos1 = pos2 +1;
              }
              dir2.putNewChild(childNext);
            } else {
              break; //other base path
            }
          }
        }
      }
    //create the named file in the directory if given.
    if(sName !=null){
      FileRemote fileRet = dirRet.child(sName);
      return fileRet;
    } else {
      return dirRet;
    }
  }


  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   * @param sDirP
   * @param sName
   * @return null if the file is not registered.
   */
  public FileRemote check( final CharSequence sDirP, final CharSequence sName){
    CharSequence sDir1 = FileSystem.normalizePath(sDirP); //sPath.replace('\\', '/');
    StringBuilder uPath = sDir1 instanceof StringBuilder ? (StringBuilder)sDir1: new StringBuilder(sDir1);
    if(sName !=null) {
      if(uPath.charAt(uPath.length()-1) !='/'){ uPath.append('/'); }
      uPath.append(sName); 
    }
    String sPath = uPath.toString();
    FileRemote ret = idxPaths.get(sPath);
    return ret;
  }
  
  
}
