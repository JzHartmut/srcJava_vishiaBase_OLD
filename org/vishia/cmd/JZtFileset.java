package org.vishia.cmd;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.FilePath;

/**A Fileset instance in a JZcmd script especially for zmake. It is assigned to a script variable 
 * with the syntax (See {@link org.vishia.zcmd.JZcmdSyntax})
 * <pre>
 * Fileset myFileset = ( filepath1, filepath2 );
 * </pre>
 * If the fileset is used in a target, it is associated to the target to get the absolute paths of the files
 * temporary while processing that target.
 * <br><br>
 * The Zbnf syntax for parsing is defined as
 * <pre>
 * fileset::= { basepath = <file?basepath> | <file> ? , }.
 * </pre>
 * It refers a {@link FileSet}.
 * @see {@link JZtAccessFileset}. That class refers this and contains an access path, used as argument in a zmake call
 *   or as Argument build with <code>call ...( name = Fileset accesspath&FilesetVariable)</code> in a JZcmd script.
 */
public class JZtFileset
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-06-22 Hartmut chg: creation of {@link FileSet} as extra class. 
   * <li>2014-06-14 Hartmut chg: {@link ExecuteLevel} implements {@link FilePath.FilePathEnvAccess} now,
   *   therewith a {@link #listFiles(List, JZtFilepath, boolean, org.vishia.util.FilePath.FilePathEnvAccess)}
   *   does not need an accessPath, it may be empty respectively null.
   * <li>2014-03-07 created. From srcJava_Zbnf/org/vishia/zmake/ZmakeUserScript.UserFileset.
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
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-06-22";

  final JZtExecuter.ExecuteLevel zgenlevel;
  final JZtScript.UserFileset data;
  
  public JZtFileset(JZtExecuter.ExecuteLevel zgenlevel, JZtScript.UserFileset data){
    this.zgenlevel = zgenlevel;
    this.data = data;
  }
  
  
  
  void listFiles(List<JZtFilepath> files, JZtFilepath zgenAccesspath, boolean expandFiles) throws NoSuchFieldException {  ////
    List<FilePath> files1 = new LinkedList<FilePath>();
    FilePath accesspath = zgenAccesspath == null ? null : zgenAccesspath.data;
    data.fileset.listFiles(files1, accesspath, zgenlevel, expandFiles);
    //wrap all FilePath in JZcmdFilepath
    for(FilePath file: files1){
      JZtFilepath jzfile = new JZtFilepath(zgenlevel, file);
      files.add(jzfile);
    }
  }

  /**Returns a new list of all {@link JZtFilepath} with all files which are found in the file system
   *   in the given environment. The base path and local path is build from the members of the fileset
   *   and the {@link #accesspath} in that kind, that the shortest given local path is valid.
   * @param accesspath The access path to the members of this fileset.
   * @param expandFiles true then 
   * @return
   * @throws NoSuchFieldException
   */
  public List<JZtFilepath> listFiles(JZtFilepath accesspath, boolean expandFiles) throws NoSuchFieldException { 
    List<JZtFilepath> files = new ArrayList<JZtFilepath>();
    listFiles(files, accesspath, expandFiles);
    return files;
  }
  
  
  /**Returns a new list of all {@link JZtFilepath} with all files which are found in the file system
   *   in the given environment. The base path and local path is build from the members of the fileset
   *   in that kind, that the shortest given local path is valid.
   * @return
   * @throws NoSuchFieldException
   */
  public List<JZtFilepath> listFiles() throws NoSuchFieldException { return listFiles(null, false); }

    
  /**Returns a new list of all {@link JZtFilepath} with all files which are found in the file system
   *   in the given environment. The base path and local path is build from the members of the fileset
   *   in that kind, that the shortest given local path is valid.
   * @return
   * @throws NoSuchFieldException
   */
  public List<JZtFilepath> listFilesExpanded() throws NoSuchFieldException { return listFiles(null, true); }

    
    
  @Override
  public String toString(){ return data.toString(); } 

  
  
}
