package org.vishia.fileLocalAccessor;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.fileRemote.FileRemote;



/**Order for copy, created on check.
 * @author Hartmut Schorrig
 *
 */
class CopyOrder
{

  final long ident;
  
  /**If the order is to old, remove it. */
  long dateCreation;
  
  /**File or dir to copy. */
  FileRemote fileSrc, fileDst;
  
  
  /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}
   * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}. */
  final List<File> listCopyFiles = new LinkedList<File>();
  

  
  CopyOrder(long ident){
    this.ident = ident;
  }
  
  
}
