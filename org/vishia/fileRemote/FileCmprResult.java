package org.vishia.fileRemote;

import org.vishia.util.SelectMask;

/**Class can be associated with a {@link FileRemote} to store comparison or select information.
 * @author Hartmut Schorrig
 *
 */
public class FileCmprResult extends SelectMask 
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-05-22 Hartmut created: A {@link FileRemote} may have information about a select status
   *   or about a comparison result. It is stored in a referred instance of this type. Experience.
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
  public static final int version = 20130513;

  
  protected final FileRemote file;
  
  int nrofFilesSelected;
  
  
  
  long nrofBytesSelected;

  
  public FileCmprResult(FileRemote itsFile){
    this.file = itsFile;
  }
  
  public int nrofFilesSelected(){ return nrofFilesSelected; } 
  
  
  @Override public int setDeselect(int mask, Object data)
  { int selectOld = super.setDeselect(mask, null);
    if(file.isDirectory()){
      
    }
    FileRemote parent = file;
    while( (parent = parent.getParentFile()) !=null
      && parent.cmprResult !=null   //abort while-loop if the parent is not marked 
      ){
      parent.cmprResult.nrofFilesSelected -=this.nrofFilesSelected;
      if(parent.cmprResult.nrofFilesSelected <=0){
        parent.cmprResult.nrofFilesSelected = 0;
        parent.cmprResult.selectMask &= ~mask;
      }
      parent.cmprResult.nrofBytesSelected -=this.nrofBytesSelected;
      if(parent.cmprResult.nrofBytesSelected <=0){
        parent.cmprResult.nrofBytesSelected = 0;
      }
    }
    this.nrofBytesSelected = 0;
    this.nrofFilesSelected = 0;
    return selectOld;
  }

  @Override
  public int setSelect(int mask, Object data)
  { int selectOld = super.setSelect(mask, null);
    //FileRemote file = (FileRemote)data;
    if(file.isDirectory()){
      //remain selection info set from children
      //but it may be 0.
    } else {
      this.nrofBytesSelected = file.length();
      this.nrofFilesSelected = 1;
      FileRemote parent = file;
      //inform all parents about a selection into. Mark it with select it too.
      while( (parent = parent.getParentFile()) !=null){
        if(parent.cmprResult == null){
          parent.cmprResult = new FileCmprResult(parent);
        }
        parent.cmprResult.selectMask |= mask;
        parent.cmprResult.nrofFilesSelected += this.nrofFilesSelected;
        parent.cmprResult.nrofBytesSelected += this.nrofBytesSelected;
      }
    }
    return selectOld;
  }
  
}
