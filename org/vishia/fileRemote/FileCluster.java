package org.vishia.fileRemote;

import org.vishia.util.FileRemote;
import org.vishia.util.IndexMultiTable;

/**This class combines {@link FileRemote} instances for one usage.
 * @author Hartmut Schorrig
 *
 */
public class FileCluster
{
  
  IndexMultiTable.Provide<String> idxPathsProvider = new IndexMultiTable.Provide<String>(){

    @Override public String[] genArray(int size){ return new String[size]; }

    @Override public String genMax(){ return "\255\255\255\255\255\255\255\255\255"; }

    @Override public String genMin(){ return " "; }
  };
  
  IndexMultiTable<String, FileRemote> idxPaths = new IndexMultiTable<String, FileRemote>(idxPathsProvider);
  
  

  
  public FileCluster(){
  }
  
  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote get( final String sDirP, final String sName){
    StringBuilder uPath = new StringBuilder(sDirP.replace('\\', '/'));
    if(uPath.charAt(uPath.length()-1) !='/'){ uPath.append('/'); }
    if(sName !=null) { uPath.append(sName); }
    String sPath = uPath.toString();
    FileRemote ret = idxPaths.get(sPath);
    if(ret == null){
      ret = new FileRemote(this, null, null, sDirP, sName, 0, 0, 0, null, true);
      idxPaths.put(sPath, ret);
    }
    return ret;
  }

  

  
  
}
