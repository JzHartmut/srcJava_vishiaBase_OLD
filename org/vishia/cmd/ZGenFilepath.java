package org.vishia.cmd;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;

/**This class describes a file entity in a executer level of ZGen. The file entity can contain wild cards.
 * It can refer to a variable which contains the base path.
 * It may be a absolute or a relative path. It can have a base path and a local path part.
 * This class contains only the reference to the {@link ZGenExecuter.ExecuteLevel} where this variable is located
 * and a reference to the {@link ZGenScript.UserFilepath}. The last one contains all information about the
 * file entity. This class is used to get all presentation possibilities of the file. Therefore the current directory
 * should be known which is given in the ZGen executer level. 
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
public final class ZGenFilepath {

  
  
  /**Version, history and license.
   * <ul>   
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   *   This file was copied from srcJava_Zbnf/org/vishia/zmake/Userfilepath.
   *   The data of a file are referenced with {@link #data}. The original fields are contained in
   *   {@link ZGenScript.UserFilepath}. Both are separated because the parts in ZGenScript are set completely
   *   by parsing the script. This class contains the access methods which uses the reference {@link #zgenlevel}.
   * <li>2013-03-10 Hartmut new: {@link FileSystem#normalizePath(CharSequence)} called in {@link #absbasepath(CharSequence)}
   *   offers the normalize path for all absolute file paths. 
   * <li>2013-03-10 Hartmut new: Replace wildcards: {@link #absfile(ZGenFilepath)} (TODO for some more access methods)
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
  
  private final ZGenExecuter.ExecuteLevel zgenlevel;
  

  final ZGenScript.UserFilepath data;
  
  /**An empty file path which is used as argument if a common base path is not given. */
  static ZGenFilepath emptyParent = new ZGenFilepath();
  
  /**Only for {@link #emptyParent}. */
  private ZGenFilepath(){
    this.zgenlevel = null;
    data = new ZGenScript.UserFilepath(); //with empty elements. 
  }
  
  
  
  
  ZGenFilepath(ZGenExecuter.ExecuteLevel script, ZGenScript.UserFilepath filepath){
    this.zgenlevel = script;
    this.data = filepath;
  }
  
  /**Creates a ZGenFilepath entry with an additonal pathbase.
   * if the basepath of src is given and the pathbase0 is given, both are joined: pathbase0/src.pathbase.
   * @param script  Reference to the script, necessary for the current directory
   * @param src The source (clone source)
   * @param basepath An additional basepath usual stored as <code>basepath=path, ...</code> in a fileset, maybe null
   * @param pathbase0 additional pre-pathbase before base, maybe null
   *  
   */
  ZGenFilepath(ZGenExecuter.ExecuteLevel zgenlevel, ZGenFilepath src, ZGenFilepath commonPath, ZGenFilepath accessPath) {
    this.zgenlevel = zgenlevel;
    data = new ZGenScript.UserFilepath();  //an empty instance to hold information from sources.
    CharSequence basePath = src.basepath(null, commonPath, accessPath, null);
    CharSequence localDir = src.localDir(null, commonPath, accessPath);
    int posbase = isRootpath(basePath);
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
  

  
  /**Inserts the given drive letter and the root designation on start of buffer. It does nothing if the path is relative.
   * @param u The buffer
   * @param commonBasepath An common base path in fileset
   * @param accesspath An access path while using a fileset
   * @return true if it is a root path or it has a drive letter.
   */
  CharSequence driveRoot(CharSequence basepath, ZGenFilepath commonBasepath, ZGenFilepath accesspath){
    boolean isRoot = false;
    CharSequence ret = basepath;
    if(data.absPath || commonBasepath !=null && commonBasepath.data.absPath || accesspath !=null && accesspath.data.absPath){ 
      StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
      ret = u;
      u.insert(0, '/'); 
      isRoot = true; 
    }
    if(data.drive !=null){
      StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
      ret = u;
      u.insert(0, data.drive).insert(1, ':'); 
      isRoot = true;
    }
    else if(commonBasepath !=null && commonBasepath.data.drive !=null){
      StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
      ret = u;
      u.insert(0, commonBasepath.data.drive).insert(1, ':'); 
      isRoot = true;
    }
    else if(accesspath !=null && accesspath.data.drive !=null){
      StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
      ret = u;
      u.insert(0, accesspath.data.drive).insert(1, ':'); 
      isRoot = true;
    }
    return ret;
  }
  
  
  
  /**Checks whether the given path describes a root directory or drive letter.
   * returns
   * <ul>
   * <li>0 if it is a relative path.
   * <li>1 if it is a absolute path without drive letter: "/path" or "\path"
   * <li>2 if it is a relative path with a drive letter: "D:path"
   * <li>3 if it is an absolute path with a drive letter: "D:/path" or "D:\path"
   * </ul>
   * The return value is the start of the non-root path part in textPath.
   * If return=2 or 3, the drive letter is textPath.charAt(0).
   * @param textPath
   * @return 
   */
  static int isRootpath(CharSequence textPath){  ///
    int start;
    if(textPath.length() >=1 && "\\/".indexOf(textPath.charAt(0)) >=0 ){
      start =1;
    } 
    else if(textPath.length() >=2 && textPath.charAt(1) == ':'){
      if(textPath.length() >=3 &&  "\\/".indexOf(textPath.charAt(2)) >=0 ){
        start = 3;
      } else {
        start = 2;
      }
    } else {
      start = 0;  //relative path
    }
    return start;
  }
  
  
  
  /**Gets the base path part of this. This method regards the existence of a common and an access path.
   * The common path may be given by a {@link UserFileset#commonBasepath}. The access path may be given 
   * by application of a fileset in a {@link UserTarget} especially while preparing the input files or files
   * for parameter in {@link UserTarget#prepareFiles(List, boolean)}.
   * <br><br>
   * If this file contains a basepath, all other access, common and a variable is used as full file path
   * as prefix of this base path. If this doesn't contain a basepath, either the common and access path presents
   * the base path, or of one of them contains a basepath, that is the basepath. This behavior is complementary 
   * to the behavior of {@link #localDir(StringBuilder, ZGenFilepath, ZGenFilepath)}.
   * <br><br>
   * The following true table shows the constellation possibilities and there outputs.
   * <ul>
   * <li>common: represents the common and/or the access path.
   * <li>varfile: represents a {@link #scriptVariable} maybe with a {@link ScriptVariable#filepath}
   *   or the textual content of a {@link #scriptVariable} or the {@link #envVariable}.
   *   Only a {@link ScriptVariable#filepath} can represent a <code>base</code>.
   *   An environment variable can represent a <code>abs</code>, it is checked with {@link #isRootpath(CharSequence)}.
   * <li>The common or variable reference can be given (1) or it is null (0).
   * <li>base: The element has a basepath and a local part.
   * <li>abs: The element is given as absolute path
   * </ul>
   * Results:
   * <ul>
   * <li>/: Absolute path maybe with drive letter
   * <li>thisBase, varBase, commonBase: Use the base part of the element.
   * <li>varFile, commonFile: Use the whole path of the element.
   * </ul> 
   * <pre>
   *  common    variable    this         basepath build with         : localdir build with   
   *  | base    | base abs  base abs
   *  x  x      x  x   x     1    1      /thisBase                   : thisLocal
   *  x  x      x  x   x     0    1      "/"                         : thisLocal
   *  
   *  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
   *  0         1  x   0     1    0      varFile + thisBase
   *  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
   *  0         0            1    0      thisBase                    : thisLocal
   *  1  x      0            1    0      commonFile + thisBase       : thisLocal
 
   *  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

   *  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
   *  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
   *    
   *  x  x      1  0   1     0    0      /                           : varLocalFile + thisLocal
  
   *  0  x      1  0   0     0    0      ""                          : varLocalFile + thisLocal
   *  1  0      1  0   0     0    0      commonFile                  : varLocalFile + thisLocal
   *  1  1      1  0   0     0    0      commonBase                  : varLocalFile + thisLocal
   *    
   *  0  x      0            0    0      ""                          : thisLocal
   *  1  0      0            0    0      commonFile                  : thisLocal
   *  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
   * </pre>
   * To implement this true table with less tests the algorithm is recursive. If this has not a {@link #basepath},
   * this routine is called recursively for an existing {@link ScriptVariable#filepath} or for the existing
   * commonPath or accessPath each with given left side element (variable with common and access, common with access).
   * If any of the elements commonPath or accessPath have not a base path, 
   * the {@link #localFile(StringBuilder, ZGenFilepath, ZGenFilepath)} is added too.
   *         
   * @param uRet If not null then append the result in it.  
   * @param generalPath if not null then its file() is used as part before the own basepath
   *   but only if this is not an absolute path.
   * @param accesspath a String given path which is written before the given base path if the path is not absolute in this.
   *   If null, it is ignored. If this path is absolute, the result is a absolute path of course.
   * @param useBaseFile null or false, the return the basepath only. 
   *   true then returns the local path part if a base path is not given inside. This element is set to false
   *   if the element has a base path and therefore the local path part of the caller should not be added.    
   * @return the whole base path of the constellation.
   *   Either as absolute or as relative path how it is given.
   *   The return instance is the given uRet if uRet is not null. 
   *   It is a StringBuilder if the path is assembled from more as one parts.
   *   It is a String if uRet is null and the basepath is simple.
   *   A returned StringBuilder may be used to append some other parts furthermore.
   *  
   */
  protected CharSequence basepath(StringBuilder uRet, ZGenFilepath commonPath, ZGenFilepath accessPath, boolean[] useBaseFile) 
  { 
    //if(generalPath == null){ generalPath = emptyParent; }
    //first check singulary conditions
    ///
    int pos;
    ZGenScript.FilesetVariable var;
    ZGenFilepath varfile;
    if((data.basepath !=null || useBaseFile !=null && useBaseFile[0]) && data.scriptVariable !=null){
      //get the variable if a base path is given or the file may be used as base path
      DataAccess.Variable<Object> var1 = zgenlevel.localVariables.get(data.scriptVariable);
      if(var1 !=null){
        Object var0 = var1.value();
        if(var0 instanceof ZGenScript.FilesetVariable){
          //varfile = ((ZGenScript.FilesetVariable)var0).filepath;
        }
      }
      var = null;
      varfile = null;  //TODO var.filepath;
    } else { 
      var = null;
      varfile = null;
    }
    if(data.absPath){
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x  x      x  x   x     1    1      /thisBase                   : thisLocal
      //  x  x      x  x   x     0    1      "/"                         : thisLocal
      if(data.drive !=null || data.basepath !=null || uRet !=null || var !=null || data.envVariable !=null){
        if(uRet == null){ uRet = new StringBuilder(); }  //it is necessary.
        else { uRet.setLength(0); }
        if(data.drive !=null){ 
          uRet.append(data.drive).append(":");
        }
        uRet.append("/");
        if(data.basepath !=null){ 
          if(useBaseFile !=null){ useBaseFile[0] = false; }  //designate it to the caller
          uRet.append(data.basepath);
        } else if(useBaseFile !=null && useBaseFile[0]) {
          addLocalName(uRet);
        }
        return uRet;
      } else {
        assert(uRet == null && data.basepath ==null && data.drive == null);
        return "/";
      }
    }
    else if(data.basepath !=null){
      if(useBaseFile !=null){ useBaseFile[0] = false; }  //designate it to the caller
      //
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
      //  0         1  x   0     1    0      varFile + thisBase
      //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
      //  0         0            1    0      thisBase                    : thisLocal
      //  1  x      0            1    0      commonFile + thisBase       : thisLocal
      if(commonPath !=null || accessPath !=null || var !=null || data.envVariable !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
        //  0         1  x   0     1    0      varFile + thisBase
        //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
        //  1  x      0            1    0      commonFile + thisBase       : thisLocal
        CharSequence prepath;
        
        if(varfile !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
          //  0         1  x   0     1    0      varFile + thisBase
          //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
          prepath = varfile.file(uRet, commonPath, accessPath);
        }
        else if(commonPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          prepath = commonPath.file(uRet, accessPath);
        } 
        else if(accessPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          prepath = accessPath.file(uRet, null);
        } else {
          //possible to have a variable with text or environment variable.
          prepath = uRet;
        }
        if(data.basepath.length() >0 || var !=null && varfile == null || data.envVariable !=null){
          //need to add somewhat, build the StringBuilder if not done.
          if(prepath instanceof StringBuilder){
            uRet = (StringBuilder)prepath;
          } else {
            assert(uRet == null);  //elsewhere it might be used for prepath
            uRet = prepath !=null ? new StringBuilder(prepath) : new StringBuilder();
          }
        }
        final CharSequence text;
        if(var !=null && varfile == null){
          try{ text = var.text(); }
          catch(Exception exc){ throw new IllegalArgumentException(exc.getMessage()); }
        }  
        else if(data.envVariable !=null){
          text = System.getenv(data.envVariable);
        } else {
          text = null;
        }
        if(text!=null && text.length() >0){
          pos = uRet.length();
          if(pos >0 && isRootpath(text)>0){
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
            uRet.setLength(0);
          }
          else {
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  0         1  x   0     1    0      varFile + thisBase
            //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
            if( pos >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          }
          uRet.append(text);
        }
        if(data.basepath.length() ==0){
          //it is possible to have an empty basepat, writing $variable:
          return uRet !=null ? uRet : prepath;
        } else {
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(this.data.basepath);
          return uRet;
        }
      } else {
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //        0         0            1    0      thisBase                    : thisLocal
        return data.basepath;
      }
    } else { 
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

      //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
      //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
      //    
      //  x  x      1  0   1     0    0      /                           : varLocalFile + thisLocal
     
      //  0  x      1  0   0     0    0      ""                          : varLocalFile + thisLocal
      //  1  0      1  0   0     0    0      commonFile                  : varLocalFile + thisLocal
      //  1  1      1  0   0     0    0      commonBase                  : varLocalFile + thisLocal
      //    
      //  0  x      0            0    0      ""                          : thisLocal
      //  1  0      0            0    0      commonFile                  : thisLocal
      //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
      assert(data.basepath == null); //check whether other parts have a basepath
      //The varFile is part of the localpath, because basepath == null. Don't use here.
      //Get the basepath from the whole common or access.
      //It one of them defines a basepath only that is returned. 
      CharSequence prepath;
      boolean[] useBaseFileSub = new boolean[1];
      useBaseFileSub[0] = true;  //use the file of commonPath or accessPath as base path. 
      if(varfile !=null && (varfile.data.basepath !=null || useBaseFile !=null && useBaseFile[0])){
        //use the variableFile if it is called recursively. 
        //The variable is that one from commonPath or accessPath of the caller.
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

        //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
        //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
        prepath = varfile.basepath(uRet, commonPath, accessPath, useBaseFileSub);
      }
      else if(commonPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = commonPath.basepath(uRet, null, accessPath, useBaseFileSub);
      } 
      else if(accessPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = accessPath.basepath(uRet, null, null, useBaseFileSub);
      } 
      else {
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  0  x      0            0    0      ""                          : thisLocal
        if(uRet !=null){ prepath = uRet; }
        else prepath = "";
      }
      if(useBaseFileSub[0] && useBaseFile !=null && useBaseFile[0]) {
        //it is called recursively, therefore use the file as base part, and the left element has not a base part.
        if(!(prepath instanceof StringBuilder)){
          assert(uRet == null);   //if it is not null, it is used for prepath.
          uRet = new StringBuilder(prepath);
        }
        return addLocalName(uRet);
      } else {
        return prepath;
      }
    }
  }
  
  

  

  
  /**Gets the local directory path.
   * <ul>
   * <li>If this instance has a basepath, the local directory is the local directory path part of this.
   *   In this case the generalPath and the accesspath are not used.
   * <li>If this has not a basepath but the generalPath is split in a basepath and a localpath,
   *   the local path part of the generalPath is used as local directory path too.
   * <li>If this has not a basepath and the generalPath hasn't a basepath or it is null 
   *   but the accesspath is split in a basepath and a local path,
   *   the local path part of the accessPath and the whole generalPath is used as local directory path too.
   * <li>If this has not a basepath but both the generalPath and the accessPath has not a basepath too,
   *   only this {@link #localdir} is returned as local directory path. In this case the accessPath and the generalPath
   *   acts as basepath.
   * </ul>  
   * If the 
   * @param uRet If not null, then the local directory path is appended to it and uRet is returned.
   * @param generalPath
   * @param accessPath
   * @return Either the {@link #localdir} as String or a StringBuilder instance. If uRet is given, it is returned.
   *  
   */
  public CharSequence localDir(StringBuilder uRet, ZGenFilepath commonPath, ZGenFilepath accessPath) {
    ///
    if(  data.basepath !=null     //if a basepath is given, then only this localpath is valid.
      || (  (commonPath == null || commonPath.data.basepath ==null) //either no commonPath or commonPath is a basepath completely
         && (accessPath == null || accessPath.data.basepath ==null) //either no accessPath or accessPath is a basepath completely
         && data.scriptVariable == null  //no Varable
         && data.envVariable == null
      )  ){  //Then only the localdir of this is used. 
      if(uRet == null){
        return data.localdir;
      } else {
        uRet.append(data.localdir);
        return uRet;
      }
    }
    else {
      assert(data.basepath == null);
      if(uRet ==null){ uRet = new StringBuilder(); } //it is necessary. Build it if null.
      ZGenScript.FilesetVariable var;
      ZGenFilepath varfile;
      if(data.scriptVariable !=null){
        var = null; //script.filesetVar.get(data.scriptVariable);
        varfile = null; //TODO var.filepath;
      } else { 
        var = null;
        varfile = null;
      }
      if(varfile !=null){
        varfile.localFile(uRet, commonPath, accessPath);  //The full varfile.localfile should use.
      }
      else if(commonPath !=null){
        commonPath.localFile(uRet, null, accessPath);
      } 
      else if(accessPath !=null){
        accessPath.localFile(uRet, null, null);
      }
      //
      if(var !=null && var.filepath == null){
        try{ 
          CharSequence varpath = var.text();
          uRet.append(varpath);
        } 
        catch(Exception exc){ throw new IllegalArgumentException(exc.getMessage()); }
      }
      if(data.envVariable !=null){
        CharSequence varpath = System.getenv(data.envVariable);
        uRet.append(varpath);
      }
      int pos;
      if( data.localdir.length() >0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }

      uRet.append(data.localdir);
      return uRet;
    }
  }
  
  
  public CharSequence localFile(StringBuilder uRet, ZGenFilepath commonPath, ZGenFilepath accessPath) {
    CharSequence dir = localDir(uRet, commonPath, accessPath);
    if(uRet ==null){
      uRet = dir instanceof StringBuilder ? (StringBuilder)dir: new StringBuilder(dir);
    }
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name).append(data.ext);
    return uRet;
  }
  
  
  

  
  
  /**Builds the absolute path with given basepath maybe absolute or not, maybe with drive letter or not. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   */
  private CharSequence absbasepath(CharSequence basepath){ 
    CharSequence ret = basepath;
    if(isRootpath(ret) ==0){ //a relative path: insert the currdir of the script only if it is not a root directory already:
      if(ret.length() >0){      //only if ret is not ""
        StringBuilder uRet;
        if(ret instanceof StringBuilder){
          uRet = (StringBuilder)ret;
        } else {
          ret = uRet = new StringBuilder(ret);
        }
        CharSequence sCurrDir = zgenlevel.sCurrdir();
        if(uRet.length() >=2 && uRet.charAt(1) == ':'){
          //a drive is present but it is not a root path
          if(sCurrDir.length()>=2 && sCurrDir.charAt(1)==':' && uRet.charAt(0) == sCurrDir.charAt(0)){
            //Same drive letter like sCurrDir: replace the absolute path.
            uRet.replace(0, 2, sCurrDir.toString());
          }
          else {
            //a drive is present, but it is another drive else in sCurrDir But the path is not absolute:
            //TODO nothing yet, 
          }
        }
        else {  //a drive is not present.
          uRet.insert(0, sCurrDir);
        }
      }
      else {
        //ret is "", then return the current dir only.
        ret = zgenlevel.sCurrdir();
      }
    }
    ret = FileSystem.normalizePath(ret);
    return ret;
  }
  
  /**Method can be called in the generation script: <*absbasepath()>. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   *  
   */
  public CharSequence absbasepath() { 
    CharSequence basepath = basepath(null, emptyParent, null, null);
    //basepath = driveRoot(basepath, emptyParent,null);
    return absbasepath(basepath);
  }
  
  public CharSequence absbasepathW() { return toWindows(absbasepath()); }
  

  
  /**Method can be called in the generation script: <*path.absdir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   *   
   */
  public CharSequence absdir()  { 
    CharSequence basePath = absbasepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int zpath = (data.localdir == null) ? 0 : data.localdir.length();
    if(zpath > 0){ //not empty
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(data.localdir);
    }
    return uRet;
  }
  
  public CharSequence absdirW(){ return toWindows(absdir()); }
  
  
  /**Method can be called in the generation script: <*data.absname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   Either as absolute or as relative path.
   */
  public CharSequence absname(){ 
    CharSequence basePath = absbasepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name);
    return uRet;
  }
  
  public CharSequence absnameW(){ return toWindows(absname()); }
  


  
  /**Method can be called in the generation script: <*path.absfile()>. 
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   */
  public CharSequence absfile(){ 
    CharSequence basePath = absbasepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    uRet.append(data.ext);
    return uRet;
  }
  
  public CharSequence absfileW(){ return toWindows(absfile()); }
  
  
  /**Method can be called in the generation script: <*path.absfile()>.
   * @param replWildc With them localdir and name a wildcard in this.localdir and this.name is replaced.
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   */
  public CharSequence absfile(ZGenFilepath replWildc){ 
    CharSequence basePath = absbasepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet, replWildc);
    uRet.append(data.ext);
    return uRet;
  }
  
  /**Method can be called in the generation script: <*basepath()>. 
   * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
   *   till a ':' in the input path or an empty string.
   *   Either as absolute or as relative path how it is given.
   */
  public CharSequence basepath(){ return basepath(null, emptyParent, null, null); }
   
  

  
  public CharSequence basepathW(){ return toWindows(basepath()); }
  
  
  
  /**Method can be called in the generation script: <*path.dir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   */
  public CharSequence dir(){ 
    CharSequence basePath = basepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int zpath = (data.localdir == null) ? 0 : data.localdir.length();
    if(zpath > 0){
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(data.localdir);
    }
    return uRet;
  }

  
  
  public CharSequence dirW(){ return toWindows(dir()); }
  
  /**Method can be called in the generation script: <*data.pathname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   */
  public CharSequence pathname(){ 
    CharSequence basePath = basepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name);
    return uRet;
  }
  
  public CharSequence pathnameW(){ return toWindows(pathname()); }
  


  /**Returns the file path maybe with given commonBasepath and a access path. 
   * @param accesspath Access path may be given by usage.
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   */
  public CharSequence file(StringBuilder uRet, ZGenFilepath accesspath){
    return file(uRet,null,accesspath);
  }
  
  /**Returns the file path maybe with given commonBasepath and a access path. 
   * @param accesspath Access path may be given by usage.
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   */
  public CharSequence file(StringBuilder uRet, ZGenFilepath commonPath, ZGenFilepath accesspath){ 
    CharSequence basePath = basepath(null, commonPath, accesspath, null);
    uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    localDir(uRet, emptyParent, accesspath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name).append(data.ext);
    return uRet.append(data.ext);
  }
  
  
  
  /**Method can be called in the generation script: <*data.file()>. 
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   */
  public CharSequence file(){ 
    CharSequence basePath = basepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    return uRet.append(data.ext);
  }
  
  public CharSequence fileW(){ return toWindows(file()); }
  
  
  
  /**Method can be called in the generation script: <*data.base_localdir()>. 
   * @return the basepath:localpath in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   */
  public CharSequence base_localdir(){ 
    CharSequence basePath = basepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(data.localdir);
    return uRet;
  }
  
  public CharSequence base_localdirW(){ return toWindows(base_localdir()); }
  
  
  /**Method can be called in the generation script: <*data.base_localfile()>. 
   * @return the basepath:localpath/name.ext in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   */
  public CharSequence base_localfile(){ 
    CharSequence basePath = basepath();
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(this.data.localdir);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.data.name);
    uRet.append(this.data.ext);
    return uRet;
  }
  
  public CharSequence base_localfileW(){ return toWindows(base_localfile()); }
  
  
  

  /**Method can be called in the generation script: <*path.localdir()>. 
   * @return the local path part of the directory of the file without ending slash. 
   *   If no directory is given in the local part, it returns ".". 
   */
  public String localdir(){
    int length = data.localdir == null ? 0 : data.localdir.length();
    return length == 0 ? "." : data.localdir; 
  }
  
  /**Method can be called in the generation script: <*path.localDir()>. 
   * @return the local path part with file without extension.
   */
  public String localdirW(){ return data.localdir.replace('/', '\\'); }
  

  
  /**Method can be called in the generation script: <*path.localname()>. 
   * @return the local path part with file without extension.
   */
  public CharSequence localname(){ 
    StringBuilder uRet = new StringBuilder();
    return addLocalName(uRet); 
  }
  
  public CharSequence localnameW(){ return toWindows(localname()); }

  
  /**Method can be called in the generation script: <*path.localfile()>. 
   * @return the local path to this file inclusive name and extension of the file.
   */
  public CharSequence localfile(){ 
    StringBuilder uRet = new StringBuilder();
    addLocalName(uRet);
    uRet.append(this.data.ext);
    return uRet;
  }

  public CharSequence localfileW(){ return toWindows(localfile()); }

  
  
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @return uRet itself to concatenate.
   */
  private CharSequence addLocalName(StringBuilder uRet){ 
    int pos;
    if( this.data.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.data.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name);
    return uRet;
  }
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @param replWildc With that localdir and name a wildcard in this is replaced.
   * @return uRet itself to concatenate.
   */
  private CharSequence addLocalName(StringBuilder uRet, ZGenFilepath replWildc){ 
    int pos;
    if( this.data.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    int posW = data.localdir.indexOf('*');
    int posW2 = data.localdir.length() > posW+1 && data.localdir.charAt(posW+1) == '*' ? posW+2 : posW+1;
    if(posW >=0){
      uRet.append(data.localdir.substring(0,posW));
      uRet.append(replWildc.data.localdir);
      uRet.append(data.localdir.substring(posW2));
    } else{
      uRet.append(data.localdir);
    }
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    posW = data.name.indexOf('*');
    posW2 = data.name.indexOf('*', posW+1);
    if(posW >=0){
      uRet.append(data.name.substring(0,posW));     //may be empty
      uRet.append(replWildc.data.name);
      if(posW2 >=0){
        uRet.append(data.name.substring(posW+1, posW2));
        if(replWildc.data.ext.length() >1){ uRet.append(replWildc.data.ext.substring(1)); }  //without leading dot
        uRet.append(data.name.substring(posW2+1));  //may be empty
      } else {
        uRet.append(data.name.substring(posW+1));   //may be empty
      }
    } else{
      uRet.append(data.name);
    }
    return uRet;
  }
  
  
  /**Method can be called in the generation script: <*path.name()>. 
   * @return the name of the file without extension.
   */
  public CharSequence name(){ return data.name; }
  
  /**Method can be called in the generation script: <*path.namext()>. 
   * @return the file name with extension.
   */
  public CharSequence namext(){ 
    StringBuilder uRet = new StringBuilder(); 
    uRet.append(data.name);
    uRet.append(data.ext);
    return uRet;
  }
  
  /**Method can be called in the generation script: <*path.ext()>. 
   * @return the file extension.
   */
  public CharSequence ext(){ return data.ext; }
  
  
  static CharSequence toWindows(CharSequence inp)
  {
    if(inp instanceof StringBuilder){
      StringBuilder uRet = (StringBuilder)inp;
      for(int ii=0; ii<inp.length(); ++ii){
        if(uRet.charAt(ii)=='/'){ uRet.setCharAt(ii, '\\'); }
      }
      return uRet;
    }
    else { //it is String!
      return ((String)inp).replace('/', '\\');
    }
  }
  
  /**Fills this.{@link #filesOfFilesetExpanded} with all files, which are selected by the filepathWildcards and the absPath.
   * The filepathWildcards does not need to contain in this UserFileset, it may be contained in another one too.
   * Especially the {@link UserTarget} in form of this base class can be filled.
   * @param filepathWildcards
   * @param absPath
   */
  public void expandFiles(List<ZGenFilepath> listToadd, ZGenFilepath commonPath, ZGenFilepath accessPath, File currdir){
    List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
    final CharSequence basepath1 = this.basepath(null, commonPath, accessPath, null); //getPartsFromFilepath(file, null, "absBasePath").toString();
    int posRoot = isRootpath(basepath1);
    final CharSequence basePathNonRoot = posRoot == 0 ? basepath1: basepath1.subSequence(posRoot, basepath1.length());
    final String basepath = basePathNonRoot.toString();  //persistent content.
    //CharSequence basepathroot = driveRoot(basepath, commonPath, accessPath);
    //boolean absPath = isRootpath(basepathroot) >0;
    //String drive = basepathroot.length() >=2 && basepathroot.charAt(1) == ':' ? String.valueOf(basepathroot.charAt(0)): null;
    String drive = posRoot >=2 ? Character.toString(basepath1.charAt(0)) : null;
    boolean absPath = posRoot == 1 || posRoot == 3;
    
    final CharSequence absBasepath = absbasepath(basepath1);
    
    final CharSequence localfilePath = this.localFile(null, commonPath, accessPath); //getPartsFromFilepath(file, null, "file").toString();
    final String sPathSearch = absBasepath + ":" + localfilePath;
    try{ FileSystem.addFilesWithBasePath(null, sPathSearch, listFiles);
    } catch(Exception exc){
      //let it empty.
    }
    for(FileSystem.FileAndBasePath file1: listFiles){
      ZGenFilepath filepath2 = new ZGenFilepath(zgenlevel, data);
      filepath2.data.absPath = absPath;
      filepath2.data.drive = drive;
      filepath2.data.basepath = basepath;  //it is the same. Maybe null
      int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
      int posExt = file1.localPath.lastIndexOf('.');
      final String sPath = file1.localPath.substring(0, posName);  //"" if only name
      final String sName;
      final String sExt;
      if(posExt < 0){ sExt = ""; sName = file1.localPath.substring(posName); }
      else { sExt = file1.localPath.substring(posExt); sName = file1.localPath.substring(posName, posExt); }
      filepath2.data.localdir = sPath;
      assert(!sPath.endsWith("/"));
      filepath2.data.name = sName;
      filepath2.data.ext = sExt;
      listToadd.add(filepath2);
    }

  }
  

  
  @Override
  public String toString()
  { return base_localfile().toString();
  }

}
