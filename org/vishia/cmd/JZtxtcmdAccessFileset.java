package org.vishia.cmd;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.util.DataAccess;
import org.vishia.util.FilePath;

/**This class contains a reference to the access path and the reference to the Fileset which is accessed.
 * That objects contains a reference to the {@link JZtxtcmdExecuter.ExecuteLevel} which contains the
 * maybe used variables.
 * Use as Parameter for JZcmd call(..., name= Fileset access&fileset) and for Zmake inputs.
 * @author Hartmut Schorrig
 *
 */
public class JZtxtcmdAccessFileset
{
  /**Version, history and license.
   * <ul>
   * <li>2014-05-04 created. From srcJava_Zbnf/org/vishia/zmake/ZmakeUserScript.UserFileset.
   * </ul>
   * 
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
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-05-04";
  
  
  private final JZtxtcmdFilepath accesspath;
  private final JZtxtcmdFileset fileset;

  
  
  
  
  /**Creates an instance from given inputs.
   * @param accessPath
   * @param sFilesetVariable
   * @param jzlevel
   * @return the instance
   * @throws Exception 
   */
  public JZtxtcmdAccessFileset(JZtxtcmdScript.AccessFilesetname statement, String sFilesetVariable, JZtxtcmdExecuter.ExecuteLevel jzlevel) 
  throws Exception
  {
    
    final FilePath accessPath;
    //if(statement.accessPath ==null){
      CharSequence sAccesspath = jzlevel.evalString(statement);
      accessPath = new FilePath(sAccesspath.toString());
    //} else {
    //  accessPath = statement.accessPath;
    //}
    //search the named file set. It is stored in a ready-to-use form in any variable.
    DataAccess.Variable<Object> filesetV = jzlevel.localVariables.get(sFilesetVariable);
    if(filesetV == null) throw new NoSuchFieldException("JZcmdAccessFileset - fileset not found;" + sFilesetVariable);
    Object filesetO = filesetV.value();
    if(!(filesetO instanceof JZtxtcmdFileset)) throw new NoSuchFieldException("JZcmd.execZmake - fileset faulty type;" + sFilesetVariable);
    //store the file set and the path before:
    this.fileset = (JZtxtcmdFileset) filesetO;
    if(accessPath !=null){
      this.accesspath = new JZtxtcmdFilepath(jzlevel, accessPath);
    } else {
      this.accesspath = null;
    }
  }
  
  
  /**Returns a new list of all {@link JZtxtcmdFilepath} whith all files which are found in the file system
   *   in the given environment. The base path and local path is build from the members of the fileset
   *   and the {@link #accesspath} in that kind, that the shortest given local path is valid.
   * @return
   * @throws NoSuchFieldException
   */
  public List<JZtxtcmdFilepath> listFilesExpanded() throws NoSuchFieldException { 
    return fileset.listFiles(accesspath, true); 
  }

  
  
  /**Appends the files of this set to the list.
   * @param files Given list of files.
   * @param zgenlevel
   * @param expandFiles true then show to the file storage medium and get all existent files proper to wildcards.
   *   false then return a FilePath with wildcards.
   * @throws NoSuchFieldException If a variable is not found.
   */
  public void listFiles(List<JZtxtcmdFilepath> files, final JZtxtcmdExecuter.ExecuteLevel zgenlevel, boolean expandFiles) 
  throws NoSuchFieldException{
    if(this.fileset !=null){
      this.fileset.listFiles(files, this.accesspath, expandFiles);
    }
    else if(this.accesspath !=null){ //fileset is null, only this one file.
      if(expandFiles){
        List<FilePath> files1 = new LinkedList<FilePath>();
        this.accesspath.data.expandFiles(files1, null, null, zgenlevel);
        for(FilePath file: files1){
          JZtxtcmdFilepath zgenFile = new JZtxtcmdFilepath(zgenlevel, file);
          files.add(zgenFile);
        }
      } else {
        JZtxtcmdFilepath targetsrc = new JZtxtcmdFilepath(zgenlevel, this.accesspath, null, null);
        files.add(targetsrc);  
      }
    } else { 
    }

  }
  
  
  
}
