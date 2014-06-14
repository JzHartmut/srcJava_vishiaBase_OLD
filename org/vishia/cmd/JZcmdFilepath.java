package org.vishia.cmd;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.FilePath;
import org.vishia.util.FileSystem;

/**This class describes a file entity in a executer level of JZcmd. The file entity can contain wild cards.
 * It can refer to a variable which contains the base path.
 * It may be a absolute or a relative path. It can have a base path and a local path part.
 * This class contains only the reference to the {@link JZcmdExecuter.ExecuteLevel} where this variable is located
 * and a reference to the {@link JZcmdScript.Filepath}. The last one contains all information about the
 * file entity. This class is used to get all presentation possibilities of the file. Therefore the current directory
 * should be known which is given in the JZcmd executer level. 
 * <ul>
 * <li><b>localpath</b>:
 *   If you write <code>anyPath/path:localPath/path/file.ext</code> then it describes a path which part 
 *    from <code>localPath</code> can be used as path to the file in some scripts. 
 *    <ul>
 *    <li>For example in C-compilation object-files can be stored in sub directories of the objects destination directory 
 *      which follows this local path designation.
 *    <li>Another example: copying some files from one directory location to another in designated sub directories.
 *    </ul> 
 * <li><b>General path</b>: If this file entry is member of a file set, the file set can have a general path.
 *   It is given by the {@link UserFileset#srcpath}. A given general path is used for all methods. 
 *   Only this entry describes an absolute path the general path is not used. 
 * <li><b>Drive letter or select path</b>: On windows systems a drive letter can be used in form <code>A:</code>.
 *   The path should not be absolute. For example <code>"X:mypath\file.ext"</code> describes a file starting from the 
 *   current directory of the <code>X</code> drive. Any drive has its own current directory. A user can use this capability
 *   of windows to set different current directories in special maybe substitute drives.
 * <li><b>Drive letter as select path</b>:  
 *   It may be possible (future extension) to use this capability independent of windows in this class. 
 *   For that the {@link #itsFileset} can have some paths associated to drive letters with local meaning,
 *   If the path starts with a drive letter, the associated path is searched in the parents drive list. 
 * <li><b>Absolute path</b>: If this entry starts with slash or backslash, maybe after a drive designation for windows systems,
 *   it is an absolute path. Elsewhere the parent's general path can be absolute. If an absolute path is requested
 *   calling {@link #absFile()} or adequate and the path is not given as absolute path, then the current directory is used
 *   as prefix for the path. The current directory is a property of the {@link UserScript#sCurrDir}. The current directory
 *   of the operation system is not used for that. 
 * <li><b>operation systems current directory</b>: In opposite if you generate a relative path and the executing system
 *   expects a normal path then it may use the operation system's current directory. But that behaviour is outside of this tool.
 * <li><b>Slash or backslash</b>: The user script can contain slash characters for path directory separation also for windows systems.
 *   It is recommended to use slash. The script which should be generate may expect back slashes on windows systems.
 *   Therefore all methods which returns a path are provided in 2 forms: <b>With "W" on end</b> of there name it is the <b>Windows version</b>
 *   which converts given slash characters in backslash in its return value. So the generated script will contain backslash characters.
 *   Note that some tools in windows accept a separation with slash too. Especial in C-sources an <code>#include <path/file.h></code>
 *   should be written with slash or URLs (hyperlinks) should be written with slash in any case.    
 * <li><b>Return value of methods</b>: All methods which assembles parts of the path returns a {@link java.lang.CharSequence}.
 *   The instance type of the CharSequence is either {@link java.lang.String} or {@link java.lang.StringBuilder}.
 *   It is not recommended that a user casts the instance type to StringBuilder, then changes it, stores references and
 *   expects that is unchanged. Usual either references to {@link java.lang.CharSequence} or {@link java.lang.String} are expected
 *   as argument type for further processing. If a String is need, one can invoke returnValue.toString(); 
 *   <br><br>
 *   The usage of a {@link java.lang.CharSequence} saves memory space in opposite to concatenate Strings and then return
 *   a new String. In user algorithms it may be recommended to use  {@link java.lang.CharSequence} argument types 
 *   instead {@link java.lang.String} if the reference is not stored permanently but processed immediately.
 *   <br><br> 
 *   If a reference is stored for a longer time in multithreading or in complex algorithms, a {@link java.lang.String}
 *   preserves that the content of the referenced String is unchanged in any case. A {@link java.lang.CharSequence} does not
 *   assert that it is unchanged in any case. Therefore in that case the usage of {@link java.lang.String} is recommended.
 * </ul>
 * <br><br>
 * <b>Handling of Wildcards</b>:<br>
 * If a UserFilepath is used in {@link Zmake}, the {@link ZmakeUserScript.UserTarget#allInputFilesExpanded()}
 * expands given files with wildcards in a list of existing files. That's result are instances of this class which
 * does no more contain wildcards, because they are expanded already. The expand algorithm is given with
 * {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
 * <br><br>
 * If the file designation of this class contains wildcards (in Zmake for example an <code><*$target.output.file()></code>
 * either the wildcards are present in the returned value of the access methods or that type of access methods
 * which have a argument 'replWildc' are used.
 * <br><br>
 * <b>Replace wildcards</b>:<br>
 * All methods with 'replWildc'-argument does the following:
 * <ul>
 * <li>The first "*" or "**" wildcard in the local path part is replaced by the whole local path part of the replWildc-argument.
 *   Usual the local path part of this consists only of a "**" or it consists of a path ending with "**" or it contains
 *   a prefix and a postfix like "pre/ ** /post". All variants may be proper. 
 * <li>The first "*" in the name is replaced by the whole name of the 'replWildc' argument. Usual the name may consist only
 *   of a single "*".
 * <li>The second "*" in the name is replaced by the whole ext of the 'replWildc' argument. Usual the name.ext of this 
 *   may consist of "*.*.ext". See examples.      
 * </ul>       
 * Examples
 * <table><tr><th>this </th><th> replWildc</th><th>result</th></tr>
 * <tr><td>** / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>rlocal/rpath/rname.rxt.ext</td></tr>
 * <tr><td>local/path / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>local/path/rname.rxt.ext</td></tr>
 * <tr><td>local/path/ ** / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>local/path/rlocal/rpath/rname.rxt.ext</td></tr>
 * <tr><td>local/ ** /path / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>local/rlocal/rpath/path/rname.rxt.ext</td></tr>
 * <tr><td>*.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>rname.rxt.ext</td></tr>
 * <tr><td>*.ext</td><td>rlocal/rpath/rname.rxt</td><td>rname.ext</td></tr>
 * <tr><td>*.*</td><td>rlocal/rpath/rname.rxt</td><td>rname.rxt</td></tr>
 * </table>
 * <br><br>
 * ZBNF-syntax parsing an Zmake input script for this class:
 * <pre>
prepFilePath::=<$NoWhiteSpaces><! *?>
[ $$<$?@envVariable> [\\|/|]     ##path can start with a environment variable's content
| $<$?@scriptVariable> [\\|/|]   ##path can start with a scriptvariable's content
| [<!.?@drive>:]                 ## only 1 char with followed : is the drive letter
[ [/|\\]<?@absPath>]           ## starting with / is absolute path
|]  
[ <*:?@pathbase>[?:=]:]          ## all until : is pathbase, but not till a :=
[ <toLastChar:/\\?@path>[\\|/|]] ## all until last \\ or / is path
[ <toLastChar:.?@name>           ## all until exclusive dot is the name
<*\e?@ext>                     ## from dot to end is the extension
| <*\e?@name>                    ## No dot is found, all is the name. 
] .
 * </pre>
 */
public final class JZcmdFilepath {

  
  
  /**Version, history and license.
   * <ul>   
   * <li>2014-06-10 Hartmut chg: {@link ExecuteLevel} implements {@link FilePath.FilePathEnvAccess} now
   *   instead this, therewith a {@link JZcmdFileset#listFiles(List, JZcmdFilepath, boolean, org.vishia.util.FilePath.FilePathEnvAccess)}
   *   does not need an accessPath, it may be empty respectively null.
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   *   This file was copied from srcJava_Zbnf/org/vishia/zmake/Userfilepath.
   *   The data of a file are referenced with {@link #data}. The original fields are contained in
   *   {@link JZcmdScript.Filepath}. Both are separated because the parts in JZcmdScript are set completely
   *   by parsing the script. This class contains the access methods which uses the reference {@link #zgenlevel}.
   * <li>2013-03-10 Hartmut new: {@link FileSystem#normalizePath(CharSequence)} called in {@link #absbasepath(CharSequence)}
   *   offers the normalize path for all absolute file paths. 
   * <li>2013-03-10 Hartmut new: Replace wildcards: {@link #absfile(JZcmdFilepath)} (TODO for some more access methods)
   * <li>2013-02-12 Hartmut chg: dissolved from inner class in {@link ZmakeUserScript}
   * </ul>
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
   static final public int version = 20130310;


  
  /**Aggregation to a given {@link UserFileset} where this is member of. 
   * A {@link UserFileset#commonBasepath} which is valid for all files of the {@link #itsFileset} is gotten from there, 
   * if it is given (not null).
   * <br> 
   * This aggregation can be null, especially if this is a member of a list returned from
   * {@link UserTarget#allInputFiles()} if more as one fileSets are used as the target's input or an accessPath is given.
   * In that kind the {@link UserFileset#filesOfFileset} are cloned without this aggregation and the commonBasePath
   * and the accessPath are part of the {@link #basepath} of this.
   */
  //private final UserFileset itsFileset;
  
  private final JZcmdExecuter.ExecuteLevel zgenlevel;
  

  final FilePath data;
  
  /**An empty file path which is used as argument if a common base path is not given. */
  static JZcmdFilepath emptyParent = new JZcmdFilepath();
  
  /**Only for {@link #emptyParent}. */
  private JZcmdFilepath(){
    this.zgenlevel = null;
    data = new FilePath(); //with empty elements. 
  }
  
  
  
  
  /**Creates an empty instance with empty data which can be filled.
   * @param zgenlevel
   */
  JZcmdFilepath(JZcmdExecuter.ExecuteLevel zgenlevel){
    this.zgenlevel = zgenlevel;
    this.data = new FilePath(); //with empty elements.
  }
  
  /**Creates an instance with given data.
   * @param zgenlevel
   * @param filepath given data
   */
  JZcmdFilepath(JZcmdExecuter.ExecuteLevel zgenlevel, FilePath filepath){
    this.zgenlevel = zgenlevel;
    this.data = filepath;
  }
  
  /**Creates an instance with given data.
   * @param zgenlevel
   * @param filepath given data
   */
  JZcmdFilepath(JZcmdExecuter.ExecuteLevel zgenlevel, String filepath){
    this.zgenlevel = zgenlevel;
    this.data = new FilePath(filepath);
  }
  
  /**Creates a JZcmdFilepath entry with an additonal pathbase.
   * if the basepath of src is given and the pathbase0 is given, both are joined: pathbase0/src.pathbase.
   * @param zgenlevel  Reference to the zgenlevel, necessary for the current directory
   * @param src The source (clone source)
   * @param basepath An additional basepath usual stored as <code>basepath=path, ...</code> in a fileset, maybe null
   * @param pathbase0 additional pre-pathbase before base, maybe null
   * @throws NoSuchFieldException 
   *  
   */
  JZcmdFilepath(JZcmdExecuter.ExecuteLevel zgenlevel, JZcmdFilepath src, JZcmdFilepath commonPath, JZcmdFilepath accessPath) throws NoSuchFieldException {
    this.zgenlevel = zgenlevel;
    data = new FilePath();  //an empty instance to hold information from sources.
    FilePath commonFilePath = commonPath !=null ? commonPath.data : null;
    FilePath accessFilePath = accessPath !=null ? accessPath.data : null;
    CharSequence basePath = src.data.basepath(null, commonFilePath, accessFilePath, zgenlevel);
    CharSequence localDir = src.data.localDir(null, commonFilePath, accessFilePath, zgenlevel);
    int posbase = FilePath.isRootpath(basePath);
    data.drive = posbase >=2 ? Character.toString(basePath.charAt(0)) : null;
    data.absPath = posbase == 1 || posbase == 3;
    data.basepath = basePath.subSequence(posbase, basePath.length()).toString();
    data.localdir = localDir.toString();
    if(!data.localdir.endsWith("/"))
      Assert.stop();
    else
      Assert.stop();
    data.name = src.data.name;
    data.ext = src.data.ext;
    data.allTree = data.localdir.indexOf('*') >=0;
    data.someFiles = src.data.someFiles;
  }
  

  
  
  
  
  /**Method can be called in the generation script: <*absbasepath()>. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   * @throws NoSuchFieldException 
   *  
   */
  public CharSequence absbasepath() throws NoSuchFieldException { return data.absbasepath(zgenlevel); }
  
  public CharSequence absbasepathW() throws NoSuchFieldException { return data.absbasepathW(zgenlevel); }
  

  
  /**Method can be called in the generation script: <*path.absdir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   *   
   */
  public CharSequence absdir() throws NoSuchFieldException  { return data.absdir(zgenlevel); } 
  
  public CharSequence absdirW() throws NoSuchFieldException{ return data.absdirW(zgenlevel); }
  
  
  /**Method can be called in the generation script: <*data.absname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   Either as absolute or as relative path.
   * @throws NoSuchFieldException 
   */
  public CharSequence absname() throws NoSuchFieldException{ return data.absname(zgenlevel); }
  
  public CharSequence absnameW() throws NoSuchFieldException{ return data.absnameW(zgenlevel); }
  


  
  /**Method can be called in the generation script: <*path.absfile()>. 
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   */
  public CharSequence absfile() throws NoSuchFieldException{ return data.absfile(zgenlevel); }
  
  public CharSequence absfileW() throws NoSuchFieldException{ return data.absfileW(zgenlevel); }
  
  
  
  /**Method can be called in the generation script: <*basepath()>. 
   * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
   *   till a ':' in the input path or an empty string.
   *   Either as absolute or as relative path how it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence basepath() throws NoSuchFieldException{ return data.basepath(zgenlevel); }
   
  

  
  public CharSequence basepathW() throws NoSuchFieldException{ return data.basepathW(zgenlevel); }
  
  
  
  /**Method can be called in the generation script: <*path.dir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence dir() throws NoSuchFieldException{ return data.dir(zgenlevel); } 
  
  
  public CharSequence dirW() throws NoSuchFieldException{ return data.dirW(zgenlevel); }
  
  /**Method can be called in the generation script: <*data.pathname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence pathname() throws NoSuchFieldException{ return data.pathname(zgenlevel); }
  
  public CharSequence pathnameW() throws NoSuchFieldException{ return data.pathnameW(zgenlevel); }
  

  /**Method can be called in the generation script: <*data.file()>. 
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence file() throws NoSuchFieldException{ return data.file(zgenlevel); } 
  
  public CharSequence fileW() throws NoSuchFieldException{ return data.fileW(zgenlevel); }
  
  
  
  /**Method can be called in the generation script: <*data.base_localdir()>. 
   * @return the basepath:localpath in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence base_localdir() throws NoSuchFieldException{ return data.base_localdir(zgenlevel); } 
  
  public CharSequence base_localdirW() throws NoSuchFieldException{ return data.base_localdirW(zgenlevel); }
  
  
  /**Method can be called in the generation script: <*data.base_localfile()>. 
   * @return the basepath:localpath/name.ext in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence base_localfile() throws NoSuchFieldException{ return data.base_localfile(zgenlevel); }
  
  public CharSequence base_localfileW() throws NoSuchFieldException{ return data.base_localfileW(zgenlevel); }
  
  
  

  /**Method can be called in the generation script: <*path.localdir()>. 
   * @return the local path part of the directory of the file without ending slash. 
   *   If no directory is given in the local part, it returns ".". 
   * @throws NoSuchFieldException 
   */
  public CharSequence localdir() throws NoSuchFieldException{ return data.localdir(zgenlevel); }
  
  /**Method can be called in the generation script: <*path.localDir()>. 
   * @return the local path part with file without extension.
   * @throws NoSuchFieldException 
   */
  public CharSequence localdirW() throws NoSuchFieldException{ return data.localdirW(zgenlevel); }
  

  
  /**Method can be called in the generation script: <*path.localname()>. 
   * @return the local path part with file without extension.
   * @throws NoSuchFieldException 
   */
  public CharSequence localname() throws NoSuchFieldException{ return data.localname(zgenlevel); }
  
  public CharSequence localnameW() throws NoSuchFieldException{return data.localnameW(zgenlevel); }

  
  /**Method can be called in the generation script: <*path.localfile()>. 
   * @return the local path to this file inclusive name and extension of the file.
   * @throws NoSuchFieldException 
   */
  public CharSequence localfile() throws NoSuchFieldException{ return data.localfile(zgenlevel); }

  public CharSequence localfileW() throws NoSuchFieldException{ return data.localfileW(zgenlevel); }

  
  
  
  /**Method can be called in the generation script: <*path.name()>. 
   * @return the name of the file without extension.
   */
  public CharSequence name(){ return data.name(); }
  
  /**Method can be called in the generation script: <*path.namext()>. 
   * @return the file name with extension.
   */
  public CharSequence namext(){ return data.namext(); }
  
  /**Method can be called in the generation script: <*path.ext()>. 
   * @return the file extension.
   */
  public CharSequence ext(){ return data.ext(); }
  
  
  

  
  @Override
  public String toString()
  { //try{ 
    return data.toString(); //} //base_localfile().toString();}
    //catch(NoSuchFieldException exc){
    //  return "faulty variable";
    //}
  }




}
