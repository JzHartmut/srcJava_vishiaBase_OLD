package org.vishia.cmd;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.FilePath;

/**This class describes a zmake target used in a JZcmd script.
 * @author Hartmut Schorrig
 *
 */
public class ZmakeTarget
{

  /**Version, history and license.
   * <ul>
   * <li>2014-03-07 created. From srcJava_Zbnf/org/vishia/zmake/ZmakeUserScript.ZmakeTarget.
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
  static final public String sVersion = "2014-03-07";

  public static class Input {
    JZcmdFileset fileset;
    JZcmdFilepath accesspathFilepath;
  }
  
 
  
  List<Input> inputs;
  
  /**The output file of the target in the ready-to-use form in a JZcmd Script.
   * One can invoke for example 'target.output.absdirW()' to get the absolute directory path with backslash.
   */
  public JZcmdFilepath output;
  
  public final String name;
  
  private final JZcmdExecuter.ExecuteLevel zgenlevel;
  
  
  public ZmakeTarget(JZcmdExecuter.ExecuteLevel zgenlevel, String name){
    this.zgenlevel = zgenlevel;
    this.name = name;
  }
  
  public List<JZcmdFilepath> allInputFiles() throws NoSuchFieldException{
    return prepareFiles(inputs, false);
  }
  
  /**Prepares all files which are given with a parameter.
   * In the ZmakeUserScript it can be given in form (examples)
   * <pre>
   * ...target(..., accesspath &fileset1, accesspath & + fileset2, ...);
   * ...target(..., file1+file2,...)
   * ...target(..., file,...)
   * ...target(..., fileset,...)
   * </pre>
   * All files and members of a fileset of this parameter are combined in one List 
   * which can be used as container for JZcmd script.
   * @return A list of {@link JZcmdFilepath} independent of a {@link JZcmdFileset}.
   * @throws NoSuchFieldException If a Filepath uses a variable and this variable is not found.
   */
  public List<JZcmdFilepath> allInputFilesExpanded() throws NoSuchFieldException{
    return prepareFiles(inputs, true);
  }

  
  /**Prepares the input of the target.
   * @param filesOrFilesets A TargetInput contains either some files or some filesets or both.
   * @param expandFiles true then resolve wildcards and return only existing files.
   * @return A list of files.
   * @throws NoSuchFieldException If a Filepath has a variable, and that is not found. 
   */
  private List<JZcmdFilepath> prepareFiles( List<Input> filesOrFilesets, boolean expandFiles) throws NoSuchFieldException {
    //
    //check whether the target has a parameter srcpath=... or commonpath = ....
    JZcmdFilepath commonPathTarget = null;
    List<JZcmdFilepath> files = new LinkedList<JZcmdFilepath>();
    //UserFileset inputfileset = null; 
    for(Input targetInputParam: filesOrFilesets){
      { //expand file or fileset:
        //
        if(targetInputParam.fileset !=null){
          targetInputParam.fileset.listFilesExpanded(files, targetInputParam.accesspathFilepath, expandFiles);
        }
        else if(targetInputParam.accesspathFilepath !=null){
          if(expandFiles){
            FilePath.FilePathEnvAccess env = targetInputParam.accesspathFilepath;
            List<FilePath> files1 = new LinkedList<FilePath>();
            targetInputParam.accesspathFilepath.data.expandFiles(files1, commonPathTarget.data, null, env);
            for(FilePath file: files1){
              JZcmdFilepath zgenFile = new JZcmdFilepath(zgenlevel, file);
              files.add(zgenFile);
            }
          } else {
            JZcmdFilepath targetsrc = new JZcmdFilepath(zgenlevel, targetInputParam.accesspathFilepath, commonPathTarget, null);
            files.add(targetsrc);  
          }
        } else { 
        }
      }
    }
    return files;
  }
  

}


  
  
  
  
