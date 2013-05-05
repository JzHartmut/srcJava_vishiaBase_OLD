package org.vishia.fileRemote;

import org.vishia.util.Assert;
import org.vishia.util.FileRemote;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringPartBase;

/**This class combines some {@link FileRemote} instances for common usage.
 * It ensures that the same FileRemote object is used for the same string given path.
 * @author Hartmut Schorrig
 *
 */
public class FileCluster
{
  /**Version, history and license.
   * <ul>
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

  
  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getFile( final CharSequence sPath){
   return(getFile(sPath, null));
  }

  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getFile( final CharSequence sDirP, final CharSequence sName){
    return getFile(sDirP, sName, false);
  }  
  
  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getFile( final CharSequence sDirP, final CharSequence sName, boolean strict){
    CharSequence sDir1 = FileSystem.normalizePath(sDirP); //sPath.replace('\\', '/');
    String sPath;
    if(sName !=null){
      //the name should be appended:
      StringBuilder uPath;  //to concatenate
      if( (sDirP == sDir1 || !(sDir1 instanceof StringBuilder))){
        //either sDirP is the same unchanged StringBuilder as sDir1, then let it unchanged! or sDir1 is not a StringBuilder
        uPath = new StringBuilder(sDir1);
      } else {
        //sDir1 is a StringBuilder created temporary from FileSystem.normalizePath(), use it.
        uPath = (StringBuilder)sDir1;  
      }
      if(uPath.charAt(uPath.length()-1) !='/'){ uPath.append('/'); }
      uPath.append(sName); 
      sPath = uPath.toString();
    } else {
      sPath = sDir1.toString(); //unchanged.
    }
    FileRemote dirRet = idxPaths.search(sPath.toString());
    if(dirRet == null){
      dirRet = new FileRemote(this, null, null, sPath, 0, 0, 0, null, true);
      idxPaths.put(sPath.toString(), dirRet);
    } else {
      String sPathRet = dirRet.getAbsolutePath();
      int zPathRet = sPathRet.length();
      if(sPath.startsWith(sPathRet)){  //maybe equal too
        if(sPathRet.length() < sPath.length()){
          if(sPath.charAt(zPathRet) == '/'){
            //a directory of the file was found.
            StringPartBase pathchild = new StringPartBase(sPath, zPathRet+1);
            dirRet = dirRet.child(pathchild);
          } else { //other directory name
            dirRet = new FileRemote(this, null, null, sPath, 0, 0, 0, null, true);
          }
        } //else equal.
      } else {
        //another directory
        dirRet = new FileRemote(this, null, null, sPath, 0, 0, 0, null, true);
      }
      idxPaths.put(sPath, dirRet);
    }
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
