package org.vishia.fileRemote;

import java.util.Queue;

import org.vishia.util.SelectMask;

/**Class can be associated with a {@link FileRemote} to store comparison or mark information.
 * @author Hartmut Schorrig
 *
 */
public class FileMark extends SelectMask 
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


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char charCmpContentEqual = '=';


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char charCmpContentEqualWithoutComments = '#';


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char charCmpContentEqualWithoutEndlines = '$';


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char charCmpContentEqualwithoutSpaces = '+';


  /**Flags as result of an comparison: the other file does not exist, or any files of an directory does not exists
   * or there are differences. */
  public static final int cmpAlone = 0x10000000;


  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public static final int cmpContentEqual = 0x04000000;


  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public static final int cmpContentNotEqual = 0x08000000;


  /**Flags as result of an comparison: the other file does not exist, or any files of an directory does not exists
   * or there are differences. */
  public static final int cmpFileDifferences = 0x30000000;


  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public static final int cmpLenEqual = 0x02000000;


  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public static final int cmpLenTimeEqual = 0x03000000;


  /**Flags as result of an comparison: the other file does not exist, or any files of an directory does not exists
   * or there are differences. */
  public static final int cmpMissingFiles = 0x20000000;


  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public static final int cmpTimeEqual = 0x01000000;

  
  protected final FileRemote itsFile;
  
  int nrofFilesSelected;
  
  
  
  long nrofBytesSelected;

  
  public FileMark(FileRemote itsFile){
    this.itsFile = itsFile;
  }
  
  public int nrofFilesSelected(){ return nrofFilesSelected; } 
  
  
  @Override public int setNonMarked(int mask, Object data)
  { int selectOld = super.setNonMarked(mask, null);
    if(itsFile.isDirectory()){
      
    }
    FileRemote parent = itsFile;
    while( (parent = parent.getParentFile()) !=null
      && parent.mark !=null   //abort while-loop if the parent is not marked 
      ){
      parent.mark.nrofFilesSelected -=this.nrofFilesSelected;
      if(parent.mark.nrofFilesSelected <=0){
        parent.mark.nrofFilesSelected = 0;
        parent.mark.selectMask &= ~mask;
      }
      parent.mark.nrofBytesSelected -=this.nrofBytesSelected;
      if(parent.mark.nrofBytesSelected <=0){
        parent.mark.nrofBytesSelected = 0;
      }
    }
    this.nrofBytesSelected = 0;
    this.nrofFilesSelected = 0;
    return selectOld;
  }

  @Override
  public int setMarked(int mask, Object data)
  { int selectOld = super.setMarked(mask, null);
    //FileRemote file = (FileRemote)data;
    if(itsFile.isDirectory()){
      //remain selection info set from children
      //but it may be 0.
    } else {
      this.nrofBytesSelected = itsFile.length();
      this.nrofFilesSelected = 1;
      /*
      FileRemote parent = file;
      while( (parent = parent.getParentFile()) !=null){
        //if()
      }
      //inform all parents about a selection into. Mark it with select it too.
      while( (parent = parent.getParentFile()) !=null){
        if(parent.mark == null){
          parent.mark = new Filemark(parent);
        }
        parent.mark.selectMask |= mask;
        parent.mark.nrofFilesSelected += this.nrofFilesSelected;
        parent.mark.nrofBytesSelected += this.nrofBytesSelected;
      }
      */
    }
    return selectOld;
  }
  
}
