package org.vishia.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



/**This class holds a path to a file with its parts (base path, directory path, name, extension, drive letter etc)
 * and supports getting String-paths in several forms - absolute, relative, name only, extension, with slash or backslash etc.
 * It supports using of script variables too.
 * 
 * For building the absolute path or using script variables the user should provide an implementation 
 * of the {@link FilePathEnvAccess}
 * which supports access to String-given variables and the current directory of the user's environment.
 * 
 * @author Hartmut Schorrig
 *
 */
public class FilePath
{
  /**Version, history and license.
   * <ul>   
   * <li>2014-04-05 Hartmut create from org/vishia/cmd/JZcmdFilepath, usage more universal.     
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
   static final public String sVersion = "2014-04-09";

  /**An implementation of this interface should be provided by the user if absolute paths and script variables should be used. 
   * It may be a short simple implementation if that features are unused. See the {@link FilePath#test()} method. 
   * @author Hartmut Schorrig
   *
   */
  public interface FilePathEnvAccess {
    
    /**Returns the Object which is addressed by the name. It may be a script variable, able to find in one
     * or more container sorted by name. 
     * @param variable The name
     * @return either an instance of FilePath or a CharSequence or null if the variable is not found (optional). 
     * @throws NoSuchFieldException if the variable is not found (optional, recommended).
     */
    Object getValue(String variable) throws NoSuchFieldException;
    
    /**Returns the current directory of the context.
     */
    CharSequence getCurrentDir();
  }
  
  
  
  /**If given, then the basePath() starts with it. 
   */
  public String scriptVariable, envVariable;
  
  /**The drive letter if a drive is given. */
  public String drive;
  
  /**Set if the path starts with '/' or '\' maybe after drive letter. */
  public boolean absPath;
  
  /**Path-part before a ':'. It is null if the basepath is not given. */
  public String basepath;
  
  /**Localpath after ':' or the whole path. It is an empty "" if a directory is not given. 
   * It does not contain a slash on end. */
  public String localdir = "";
  
  /**From Zbnf: The filename without extension. */
  public String name = "";
  
  
  /**From Zbnf: The extension inclusive the leading dot. */
  public String ext = "";
  
  /**Set to true if a "*" was found in any directory part.*/
  public boolean allTree;
  
  /**Set to true if a "*" was found in the name or extension.*/
  public boolean someFiles;
  
  
  /**An empty file path which is used as argument if a common base path is not given. */
  static FilePath emptyParent = new FilePath();
  


  public FilePath(){}
  

  
  /**parses the string given path. The path is split in its parts.
   * <pre>
   * D:/base/path:local/path.name.ext
   * </pre>
   * <ul>
   * <li>One can use '/' or '\' as path separator. Internally the '/' is stored.
   * <li>If a ':' is found on charAt(1) the character before is the drive letter.
   * <li>If a '/' on first position or after the second ':': it is an absolute path.
   * <li>A ':' not on second position: The separator between base path and local path.
   * <li>The extension is the part inclusive the last dot.
   * <li>A simple '.' or '..' is stored as the name 
   * </ul> 
   * The syntax may be described in ZBNF-Form: 
   * <pre>
 prepFilePath::=<$NoWhiteSpaces><! *?>
 [ $$<$?@envVariable> [\\|/|]     ##path can start with a environment variable's content
 | $<$?@scriptVariable> [\\|/|]   ##path can start with a scriptvariable's content
 | [<!.?@drive>:]                   ## only 1 char with followed : is the drive letter
   [ [/|\\]<?@absPath>]           ## starting with / maybe after d: is absolute path
 |]
 [ <*:?@pathbase>[?:=]:]            ## all until : is pathbase, but not till a :=
 [ <toLastChar:/\\?@path>[\\|/|]] ## all until last \ or / is path
 [ <toLastChar:.?@name>             ## all until exclusive dot is the name
   <*\e?@ext>                      ## from dot to end is the extension
 | <*\e?@name>                     ## No dot is found, all is the name.
 ] . 
   * </pre>
   * But the parsing is done with java basics (indexOf etc.)
   * See the method {@link #test()} for examples.
   * */
  public FilePath(String pathP){
    String path = pathP.replace('\\', '/');
    int zpath = path.length();
    int posColon = path.indexOf(':');
    int pos1slash = path.indexOf('/');
    int posbase;   //start of base path, ==poslocal if not given
    int poslocal;  //start of local path or whole path, after ':' or after root-/ 
    int pos1;
    if(zpath >=1 && path.charAt(0) == '&'){ //starts with a script variable:
      int pos9 = posColon > 0 && (posColon < pos1slash || pos1slash < 0) ? posColon : pos1slash > 0 ? pos1slash : zpath;
      this.scriptVariable = path.substring(1, pos9);
      absPath = false;  //hint: it may be an absolute path depending of content of scriptVariable 
      if(pos9 == pos1slash){
        pos1 = pos9 +1;   //rest of path starts after slash as separator. A colon may be found behind.
      } else {
        pos1 = pos9;      //rest of path starts on colon, it is an empty base path
      }
    } else if(posColon == 1){ //it means a ':' is found anywhere: check if it is a drive designation
      drive = path.substring(0,1);
      posColon = path.indexOf(':', 2);
      absPath = pos1slash == 2;
      pos1 = absPath ? 3 : 2;
    } else { //no variable, no drive
      absPath = pos1slash == 0;
      pos1 = absPath ? 1 : 0;
    }
    if(posColon >0){
      posbase = pos1;
      poslocal = posColon+1;
    } else {
      posbase = -1;
      poslocal = pos1;
    }
    //drive, absPath is set.
    //posbase, poslocal is set.
    //
    if(posbase >=0){
      basepath = path.substring(posbase, poslocal-1);  //may be ""
    } else { 
      basepath = null; // left empty
    }
    int posname = path.lastIndexOf('/') +1;
    if(posname < poslocal){ posname = poslocal; }
    //
    if(posname > poslocal){
      localdir = path.substring(poslocal, posname-1);
    } else {
      localdir = "";
    }
    int posext = path.lastIndexOf('.');
    if(posext <= posname){  //not found, or any '.' before start of name
      posext = zpath;  //no extension.
    } 
    name = path.substring(posname, posext);
    ext = path.substring(posext);  //with "."
    if(posname +1 == posext && posname +2 == zpath && path.charAt(posname) == '.'){
      //special form.
      name = "..";
      ext = "";
    }
  }
  
  
  /**An empty instance has not a localdir or basepath or name or drive.
   * @return true if nothing is given.
   */
  public boolean isNotEmpty(){
    return basepath !=null || localdir.length() >0 || name.length() >0 || drive !=null
    || scriptVariable !=null;
      
  }
  
  
  /**It should return the input String.
   * @see java.lang.Object#toString()
   */
  @Override public String toString() {
    StringBuilder u = new StringBuilder();
    if(scriptVariable !=null){ u.append('&').append(scriptVariable); }
    if(drive!=null) { u.append(drive).append(':'); }
    if(absPath) { u.append("/"); }
    if(basepath!=null) { u.append(basepath).append(":"); }
    if(localdir.length()>0) { u.append(localdir).append("/"); }
    u.append(name).append(ext);
    return u.toString();
  }
  

  
  
  /**Inserts the given drive letter and the root designation on start of buffer. It does nothing if the path is relative.
   * @param u The buffer
   * @param commonBasepath An common base path in file set
   * @param accesspath An access path while using a file set
   * @return true if it is a root path or it has a drive letter.
   */
  private CharSequence driveRoot(CharSequence basepathP, FilePath commonBasepath, FilePath accesspath){
    boolean isRoot1 = false;
    CharSequence ret = basepathP;
    if(this.absPath || commonBasepath !=null && commonBasepath.absPath || accesspath !=null && accesspath.absPath){ 
      StringBuilder u = basepathP instanceof StringBuilder? (StringBuilder)basepathP : new StringBuilder(basepathP);
      ret = u;
      u.insert(0, '/'); 
      isRoot1 = true; 
    }
    if(this.drive !=null){
      StringBuilder u = basepathP instanceof StringBuilder? (StringBuilder)basepathP : new StringBuilder(basepathP);
      ret = u;
      u.insert(0, this.drive).insert(1, ':'); 
      isRoot1 = true;
    }
    else if(commonBasepath !=null && commonBasepath.drive !=null){
      StringBuilder u = basepathP instanceof StringBuilder? (StringBuilder)basepathP : new StringBuilder(basepathP);
      ret = u;
      u.insert(0, commonBasepath.drive).insert(1, ':'); 
      isRoot1 = true;
    }
    else if(accesspath !=null && accesspath.drive !=null){
      StringBuilder u = basepathP instanceof StringBuilder? (StringBuilder)basepathP : new StringBuilder(basepathP);
      ret = u;
      u.insert(0, accesspath.drive).insert(1, ':'); 
      isRoot1 = true;
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
  public static int isRootpath(CharSequence textPath){  ///
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
   * <ul>
   * <li>If this file contains a basepath, all other access, common and a variable is used as full file path
   *   as prefix of this base path.
   * <li>If this does contain a variable but does not contain a base path, the variable's base path is used as base path
   *   and the variable's local path is the prefix for the local path.    
   * <li>If this does not contain a basepath, a given common and access path is checked whether it contains 
   *   a base path. Then that basepath is used.
   * <li>If a given common or access path does not contain a basepath, the common and access path is used
   *   as base path.
   * <li>This behavior is complementary to the behavior of {@link #localDir(StringBuilder, FilePath, FilePath)}.
   * </ul>
   * <br><br>
   * The following true table shows the constellation possibilities and there outputs.
   * <ul>
   * <li>common: represents the common and/or the access path.
   * <li>varfile: represents a {@link #scriptVariable} maybe with a {@link ScriptVariable#filepath}
   *   or the textual content of a {@link #scriptVariable}.
   *   Only a {@link ScriptVariable#filepath} can represent a <code>base</code>.
   *   An textual variable can represent a <code>abs</code>, it is checked with {@link #isRootpath(CharSequence)}.
   * <li>The common or variable reference can be given (1) or it is null (0).
   * <li>base: The element has a basepath and a local part.
   * <li>abs: The element is given as absolute path
   * <li>x means, it does not decide.
   * <li>1 means, it is given.
   * <li>0 means, it is not given.
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
   * the {@link #localFile(StringBuilder, FilePath, FilePath)} is added too.
   *         
   * @param uRet If not null then append the result in it.  
   * @param generalPath if not null then its file() is used as part before the own basepath
   *   but only if this is not an absolute path.
   * @param accesspath a String given path which is written before the given base path if the path is not absolute in this.
   *   If null, it is ignored. If this path is absolute, the result is a absolute path of course.
   * @param returnFileIfNoBasepath null or false, then returns the basepath. 
   *   true then returns the whole file path if an extra basepath is not given or it returns the basepath. 
   *   In the last case the element is set to false.
   *   This argument is used for recursive calling to get the basepath() from a scriptVariable given
   *   FilePath or from a accessPath or commonPath. The basepath is the whole path from such an
   *   prefix element if the prefix has not a basepath itself. 
   * @return the whole base path of the constellation.
   *   Either as absolute or as relative path how it is given.
   *   The return instance is the given uRet if uRet is not null. 
   *   It is a StringBuilder if the path is assembled from more as one parts.
   *   It is a String if uRet is null and the basepath is simple.
   *   A returned StringBuilder may be used to append some other parts furthermore.
   * @throws NoSuchFieldException 
   *  
   */
  public CharSequence basepath(StringBuilder uRetP, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) throws NoSuchFieldException 
  { 
    //if(generalPath == null){ generalPath = emptyParent; }
    //first check singularly conditions
    ///
    int test = 1;
    int pos;
    final FilePath varfile;
    final CharSequence varpath;
    Object varO;
    if(this.scriptVariable !=null){
      Object oValue = env.getValue(this.scriptVariable);
      if(oValue == null) throw new NoSuchFieldException("FilePath.basepath - scriptVariable not found; "+ this.scriptVariable);
      if(oValue instanceof FilePath){
        varfile = (FilePath)oValue;
        varpath = null;
      } else if(oValue instanceof CharSequence){
        varpath = (CharSequence)oValue;
        varfile = null;
      } else {
        throw new  NoSuchFieldException("FilePath.basepath - scriptVariable faulty type; "+ this.scriptVariable + ";" + oValue.getClass().getSimpleName());
      }
    } else {
      varfile = null; varpath = null;  //not given.
    }
    /*
    if((this.basepath !=null || returnFileIfNoBasepath !=null && returnFileIfNoBasepath[0]) && this.scriptVariable !=null){
      //get the variable if a base path is given or the file may be used as base path
      varO = env.getValue(this.scriptVariable);
      varfile = varO instanceof FilePath ? (FilePath) varO : null;
    } else { 
      varfile = null;
      varO = null;
    }
    */
    if(this.absPath){
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x  x      x  x   x     1    1      /thisBase                   : thisLocal
      //  x  x      x  x   x     0    1      "/"                         : thisLocal
      if(this.drive !=null || this.basepath !=null || uRetP !=null || varfile !=null || this.envVariable !=null){
        StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
        uRet.setLength(0);
        if(this.drive !=null){ 
          uRet.append(this.drive).append(":");
        }
        uRet.append("/");
        if(this.basepath !=null){ 
          uRet.append(this.basepath);
        } 
        return uRet;
      } else {
        assert(uRetP == null && this.basepath ==null && this.drive == null);
        return "/";
      }
    }
    else if(this.basepath !=null){
      //
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
      //  0         1  x   0     1    0      varFile + thisBase
      //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
      //  0         0            1    0      thisBase                    : thisLocal
      //  1  x      0            1    0      commonFile + thisBase       : thisLocal
      if(commonPath !=null || accessPath !=null || varfile !=null || varpath !=null){
        //a StringBuilder is necessary to assemble to path:
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
        //  0         1  x   0     1    0      varFile + thisBase
        //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
        //  1  x      0            1    0      commonFile + thisBase       : thisLocal
        CharSequence prepath;
        StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
        
        if(varfile !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
          //  0         1  x   0     1    0      varFile + thisBase
          //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
          prepath = varfile.file(uRet, commonPath, accessPath, env);
        }
        else if(commonPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          prepath = commonPath.file(uRet, accessPath, env);
        } 
        else if(accessPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          prepath = accessPath.file(uRet, null, env);
        } else {
          //possible to have a variable with text or environment variable.
          prepath = uRet;
        }
        if(this.basepath.length() >0 || varfile !=null && varfile == null || varpath !=null){
          //need to add somewhat, build the StringBuilder if not done.
          if(prepath instanceof StringBuilder){
            uRet = (StringBuilder)prepath;
          } else {
            assert(uRet == null);  //elsewhere it might be used for prepath
            uRet = prepath !=null ? new StringBuilder(prepath) : new StringBuilder();
          }
        }
        final CharSequence text;
        if(varpath !=null && varfile == null){
          text = varpath.toString();
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
        if(this.basepath.length() ==0){
          //it is possible to have an empty basepat, writing $variable:
          return uRet !=null ? uRet : prepath;
        } else {
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(this.basepath);
          return uRet;
        }
      } else {
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //        0         0            1    0      thisBase                    : thisLocal
        return this.basepath;
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
      assert(this.basepath == null); //check whether other parts have a basepath
      //The varFile is part of the localpath, because basepath == null. Don't use here.
      //Get the basepath from the whole common or access.
      //It one of them defines a basepath only that is returned. 
      CharSequence prepath;
      boolean[] returnFileIfNoBasepathSub = new boolean[1];
      returnFileIfNoBasepathSub[0] = true;  //use the file of commonPath or accessPath as base path. 
      StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
      if(varfile !=null && (varfile.basepath !=null)){
        //use the variableFile if it is called recursively. 
        //The variable is that one from commonPath or accessPath of the caller.
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

        //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
        //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
        prepath = varfile.basepath(uRet, commonPath, accessPath, env);
      }
      else if(commonPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = commonPath.basepath(uRet, null, accessPath, env);
      } 
      else if(accessPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = accessPath.basepath(uRet, null, null, env);
      } 
      else {
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  0  x      0            0    0      ""                          : thisLocal
        if(uRet !=null){ prepath = uRet; }
        else prepath = "";
      }
      return prepath;
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
   *   If the localdir is empty, "" is returned (not ".", see {@link #localdir(FilePathEnvAccess)}).
   * @throws NoSuchFieldException if a requested scriptvariable was not found.
   *  
   */
  public CharSequence localDir(StringBuilder uRetP, FilePath commonPath, FilePath accessPath
      , FilePathEnvAccess env) 
  throws NoSuchFieldException {
    ///
    if(  this.basepath !=null     //if a basepath is given, then only this localpath is valid.
      ){  //Then only the localdir of this is used. A scriptVariable is the prefix of the base path.
      if(uRetP == null){
        return localdir;
      } else {
        uRetP.append(localdir);
        return uRetP;
      }
    }
    else { //this does not contain a basepath, therefore the localDir can be defined in :
      assert(this.basepath == null);
      //use a StringBuilder to concatenate anyway.
      StringBuilder uRet = (uRetP == null)? new StringBuilder() : uRetP; //it is necessary. Build it if null.
      //NOTE: appends localDir on end.
      //Firstly get localFile from scriptVariable, commonPath, accessPath as prefix.
      //If one of that has a basePath, return its localDir/file.ext.
      if(this.scriptVariable !=null){
        Object oValue = env.getValue(this.scriptVariable);
        if(oValue instanceof FilePath){
          FilePath valfile = (FilePath)oValue;
          //get the localFile from the scriptVariable, not only the localDir because it is the dir.
          valfile.localFile(uRet, commonPath, accessPath, env); //append localDir of variable 
        } else if(oValue instanceof CharSequence){
          uRet.append((CharSequence)oValue);
        } else {
          uRet.append(oValue);
        }
      }
      else if(commonPath !=null){
        commonPath.localFile(uRet, null, accessPath, env);
      } 
      else if(accessPath !=null){
        accessPath.localFile(uRet, null, null, env);
      }
      //
      int pos;
      if( this.localdir.length() >0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.localdir);
      return uRet;
    }
  }
  
  
  public CharSequence localFile(StringBuilder uRetP, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) 
  throws NoSuchFieldException {
    if(localdir.length() ==0 && ext.length() == 0 && commonPath ==null && accessPath == null && this.scriptVariable == null){ 
      //simplest case: only name is given.
      if(uRetP == null){ return name; }
      else { uRetP.append(name); return uRetP; }
    } else {
      CharSequence dir = localDir(uRetP, commonPath, accessPath, env);
      final StringBuilder uRet = dir instanceof StringBuilder ? (StringBuilder)dir: new StringBuilder(dir);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.name).append(this.ext);
      return uRet;
    }
  }
  
  
  

  
  
  /**Builds the absolute path with given basepath maybe absolute or not, maybe with drive letter or not. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   */
  private CharSequence absbasepath(CharSequence basepath, FilePathEnvAccess env){ 
    CharSequence ret = basepath;
    if(isRootpath(ret) ==0){ //a relative path: insert the currdir of the script only if it is not a root directory already:
      if(ret.length() >0){      //only if ret is not ""
        StringBuilder uRet;
        if(ret instanceof StringBuilder){
          uRet = (StringBuilder)ret;
        } else {
          ret = uRet = new StringBuilder(ret);
        }
        CharSequence sCurrDir = env.getCurrentDir();
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
        ret = env.getCurrentDir();
      }
    }
    ret = FileSystem.normalizePath(ret);
    return ret;
  }
  
  /**Method can be called in the generation script: <*absbasepath()>. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   * @throws NoSuchFieldException 
   *  
   */
  public CharSequence absbasepath(FilePathEnvAccess env) throws NoSuchFieldException { 
    CharSequence basepath = basepath(null, emptyParent, null, env);
    //basepath = driveRoot(basepath, emptyParent,null);
    return absbasepath(basepath, env);
  }
  
  public CharSequence absbasepathW(FilePathEnvAccess env) throws NoSuchFieldException { return toWindows(absbasepath(env)); }
  

  
  /**Method can be called in the generation script: <*path.absdir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   *   
   */
  public CharSequence absdir(FilePathEnvAccess env) throws NoSuchFieldException  { 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    return localDir(uRet, null, null, env);
    /*
    int zpath = (this.localdir == null) ? 0 : this.localdir.length();
    if(zpath > 0){ //not empty
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.localdir);
    }
    return uRet;
    */
  }
  
  public CharSequence absdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absdir(env)); }
  
  
  /**Method can be called in the generation script: <*this.absname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   Either as absolute or as relative path.
   * @throws NoSuchFieldException 
   */
  public CharSequence absname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  public CharSequence absnameW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absname(env)); }
  


  
  /**Method can be called in the generation script: <*path.absfile()>. 
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   */
  public CharSequence absfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    uRet.append(this.ext);
    return uRet;
  }
  
  public CharSequence absfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absfile(env)); }
  
  
  /**Method can be called in the generation script: <*path.absfile()>.
   * @param replWildc With them localdir and name a wildcard in this.localdir and this.name is replaced.
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   */
  public CharSequence absfile(FilePath replWildc, FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet, replWildc);
    uRet.append(this.ext);
    return uRet;
  }
  
  /**Method can be called in the generation script: <*basepath()>. 
   * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
   *   till a ':' in the input path or an empty string.
   *   Either as absolute or as relative path how it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence basepath(FilePathEnvAccess env) throws NoSuchFieldException{ return basepath(null, emptyParent, null, env); }
   
  

  
  public CharSequence basepathW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(basepath(env)); }
  
  
  
  /**Method can be called in the generation script: <*path.dir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence dir(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int zpath = (this.localdir == null) ? 0 : this.localdir.length();
    if(zpath > 0){
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.localdir);
    }
    return uRet;
  }

  
  
  public CharSequence dirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(dir(env)); }
  
  /**Method can be called in the generation script: <*this.pathname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence pathname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  public CharSequence pathnameW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(pathname(env)); }
  


  /**Returns the file path maybe with given commonBasepath and a access path. 
   * @param accesspath Access path may be given by usage.
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence file(StringBuilder uRet, FilePath accesspath, FilePathEnvAccess env) throws NoSuchFieldException{
    return file(uRet,null,accesspath, env);
  }
  
  /**Returns the file path maybe with given commonBasepath and a access path. 
   * @param accesspath Access path may be given by usage.
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence file(StringBuilder uRet, FilePath commonPath, FilePath accesspath, FilePathEnvAccess env) 
  throws NoSuchFieldException { 
    CharSequence basePath = basepath(null, commonPath, accesspath, env);
    uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    localDir(uRet, emptyParent, accesspath, env);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name).append(this.ext);
    return uRet.append(this.ext);
  }
  
  
  
  /**Method can be called in the generation script: <*this.file()>. 
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence file(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    return uRet.append(this.ext);
  }
  
  public CharSequence fileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(file(env)); }
  
  
  
  /**Method can be called in the generation script: <*this.base_localdir()>. 
   * @return the basepath:localpath in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence base_localdir(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(this.localdir);
    return uRet;
  }
  
  public CharSequence base_localdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(base_localdir(env)); }
  
  
  /**Method can be called in the generation script: <*this.base_localfile()>. 
   * @return the basepath:localpath/name.ext in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence base_localfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(this.localdir);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    uRet.append(this.ext);
    return uRet;
  }
  
  public CharSequence base_localfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(base_localfile(env)); }
  
  
  

  /**Method can be called in the generation script: <*path.localdir()>. 
   * @return the local path part of the directory of the file without ending slash. 
   *   If no directory is given in the local part, it returns ".". 
   * @throws NoSuchFieldException 
   */
  public CharSequence localdir(FilePathEnvAccess env) throws NoSuchFieldException{
    CharSequence ret = localDir(null, null, null, env);
    if(ret.length() == 0){ return "."; }
    else return ret;
  }
  
  /**Method can be called in the generation script: <*path.localDir()>. 
   * @return the local path part with file without extension.
   * @throws NoSuchFieldException 
   */
  public CharSequence localdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(localdir(env)); }
  

  
  /**Method can be called in the generation script: <*path.localname()>. 
   * @return the local path part with file without extension.
   * @throws NoSuchFieldException 
   */
  public CharSequence localname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    StringBuilder uRet = new StringBuilder();
    return addLocalName(uRet, env); 
  }
  
  public CharSequence localnameW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(localname(env)); }

  
  /**Method can be called in the generation script: <*path.localfile()>. 
   * @return the local path to this file inclusive name and extension of the file.
   * @throws NoSuchFieldException 
   */
  public CharSequence localfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    StringBuilder uRet = new StringBuilder();
    addLocalName(uRet, env);
    uRet.append(this.ext);
    return uRet;
  }

  public CharSequence localfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(localfile(env)); }

  
  
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @return uRet itself to concatenate.
   */
  private CharSequence addLocalName(StringBuilder uRet){ 
    int pos;
    if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @return uRet itself to concatenate.
   * @throws NoSuchFieldException 
   */
  private CharSequence addLocalName(StringBuilder uRet, FilePathEnvAccess env) throws NoSuchFieldException{ 
    int pos;
    CharSequence localdir1 = localdir(env);
    if(!StringFunctions.equals(localdir1, ".")){
      if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(localdir1);
    }
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @param replWildc With that localdir and name a wildcard in this is replaced.
   * @return uRet itself to concatenate.
   */
  private CharSequence addLocalName(StringBuilder uRet, FilePath replWildc){ 
    int pos;
    if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    int posW = this.localdir.indexOf('*');
    int posW2 = this.localdir.length() > posW+1 && this.localdir.charAt(posW+1) == '*' ? posW+2 : posW+1;
    if(posW >=0){
      uRet.append(this.localdir.substring(0,posW));
      uRet.append(replWildc.localdir);
      uRet.append(this.localdir.substring(posW2));
    } else{
      uRet.append(this.localdir);
    }
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    posW = this.name.indexOf('*');
    posW2 = this.name.indexOf('*', posW+1);
    if(posW >=0){
      uRet.append(this.name.substring(0,posW));     //may be empty
      uRet.append(replWildc.name);
      if(posW2 >=0){
        uRet.append(this.name.substring(posW+1, posW2));
        if(replWildc.ext.length() >1){ uRet.append(replWildc.ext.substring(1)); }  //without leading dot
        uRet.append(this.name.substring(posW2+1));  //may be empty
      } else {
        uRet.append(this.name.substring(posW+1));   //may be empty
      }
    } else{
      uRet.append(this.name);
    }
    return uRet;
  }
  
  
  /**Method can be called in the generation script: <*path.name()>. 
   * @return the name of the file without extension.
   */
  public CharSequence name(){ return this.name; }
  
  /**Method can be called in the generation script: <*path.namext()>. 
   * @return the file name with extension.
   */
  public CharSequence namext(){ 
    StringBuilder uRet = new StringBuilder(); 
    uRet.append(this.name);
    uRet.append(this.ext);
    return uRet;
  }
  
  /**Method can be called in the generation script: <*path.ext()>. 
   * @return the file extension.
   */
  public CharSequence ext(){ return this.ext; }
  
  
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
  
  /**Builds non-wildcard instance for any found file and add all these instances to the given list
   * for all files, which are selected by this instance maybe with wild-cards and the given commonPath and accessPath.
   * The expansion with wild-cards is a capability of {@link FileSystem#addFilesWithBasePath(File, String, List)}
   * which is used here as core routine.
   * <br><br>
   * The possible given commonPath and accessPath will be dissolved. The absolute base path will be gotten from that,
   * see {@link #basepath(StringBuilder, FilePath, FilePath, boolean[], FilePathEnvAccess)}.
   * The result instances of FilePath will be contain the resultig base path maybe as absolute or relative one.
   * <br><br>
   * The instances from given FilePath with wild-cards will be gotten from the currently content of the file system
   * using the absolute path from {@link FilePathEnvAccess#getCurrentDir()}. 
   * That current dir will be set in the created Filepath-instances, therewith they are absolute. 
   * But note that the given local path build maybe from the commonPath or accessPath too will be gotten from the source.
   * <br><br>
   * For example the accessPath contains "myDir:subdir", this file contains <code>"*.*"</code> 
   * and the env.getCurrentDir is <code>"D:/dir"</code>.
   * Then the basepath is <code>"D:/dir/myDir"</code> and all files have a localPath <code>"subdir/...."</code>. 
   * 
   * @param listToadd List which will be completed with all found files
   * @param commonPath if not null, the path before this given file path. 
   * @param accessPath if not null, the path before a commonPath and before this given file path.
   * @throws NoSuchFieldException 
   */
  public void expandFiles(List<FilePath> listToadd, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) throws NoSuchFieldException{
    int test = 5;
    final String driveChildren;
    CharSequence basePathChildren = basepath(new StringBuilder(), commonPath, accessPath, env);
    final CharSequence absBasepath = absbasepath(basePathChildren, env);
    final String sBasePathChildren;  
    if(absBasepath.charAt(1)==':'){
      driveChildren = String.valueOf(absBasepath.charAt(0));
      sBasePathChildren = absBasepath.subSequence(3, absBasepath.length()).toString();
    } else { 
      //only on unix systems.
      driveChildren = null;  
      sBasePathChildren = absBasepath.subSequence(1, absBasepath.length()).toString();
    }
    //
    List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
    final CharSequence localfilePath = this.localFile(null, commonPath, accessPath, env); //getPartsFromFilepath(file, null, "file").toString();
    final String sPathSearch = absBasepath + ":" + localfilePath;
    try{ FileSystem.addFilesWithBasePath(null, sPathSearch, listFiles);
    } catch(Exception exc){
      //let it empty. Files may not be found.
    }
    for(FileSystem.FileAndBasePath file1: listFiles){
      //Build a new instance for any found file
      //with the same structure like the given input paths.
      //
      FilePath filepath2 = new FilePath();  //new instance for found file.
      filepath2.absPath = true;
      filepath2.drive = driveChildren;
      filepath2.basepath = sBasePathChildren;  //it is the same. Maybe null
      int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
      int posExt = file1.localPath.lastIndexOf('.');
      if(posExt < posName){ 
        posExt = -1; }        //no extension given.  
      final String sPath = posName >0 ? file1.localPath.substring(0, posName-1) : "";  //"" if only name
      final String sName;
      final String sExt;
      if(posExt < 0){ sExt = ""; sName = file1.localPath.substring(posName); }
      else { sExt = file1.localPath.substring(posExt); sName = file1.localPath.substring(posName, posExt); }
      filepath2.localdir = sPath;
      assert(!sPath.endsWith("/"));
      filepath2.name = sName;
      filepath2.ext = sExt;
      listToadd.add(filepath2);
    }

  }
  

  
  
  
  
  
  /**This class is used only temporary while processing the parse result into a instance of {@link Filepath}
   * while running {@link ZbnfJavaOutput}. 
   */
  public static class ZbnfFilepath{
    
    /**The instance which are filled with the components content. It is used for the user's data tree. */
    public final FilePath filepath;
    
    
    public ZbnfFilepath(){
      filepath = new FilePath();
    }
    
    /**FromZbnf. */
    public void set_drive(String val){ filepath.drive = val; }
    
    
    /**FromZbnf. */
    public void set_absPath(){ filepath.absPath = true; }
    
    /**FromZbnf. */
    public void set_scriptVariable(String val){ filepath.scriptVariable = val; }
    
    
    /**FromZbnf. */
    public void set_envVariable(String val){ filepath.envVariable = val; }
    
    

    
    //public void set_someFiles(){ someFiles = true; }
    //public void set_wildcardExt(){ wildcardExt = true; }
    //public void set_allTree(){ allTree = true; }
    
    /**FromZbnf. */
    public void set_pathbase(String val){
      filepath.basepath = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_path(String val){
      filepath.localdir = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_name(String val){
      filepath.name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.someFiles |= val.indexOf('*')>=0;
    }
    
    /**FromZbnf. If the name is empty, it is not the extension but the name.*/
    public void set_ext(String val){
      if(val.equals(".") && filepath.name.equals(".")){
        filepath.name = "..";
        //filepath.localdir += "../";
      }
      else if((val.length() >0 && val.charAt(0) == '.') || filepath.name.length() >0  ){ 
        filepath.ext = val;  // it is really the extension 
      } else { 
        //a file name is not given, only an extension is parsed. Use it as file name because it is not an extension!
        filepath.name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      }
      filepath.someFiles |= val.indexOf('*')>=0;
    }
    

  }
  
  
  
  @SuppressWarnings("unused")
  public static void test(){
    
    FilePathEnvAccess env = new FilePathEnvAccess(){

      Map<String, Object> container = new TreeMap<String, Object>();
      { fillContainerEnv();
      }
      
      void fillContainerEnv(){
        FilePath p1 = new FilePath("d:/varbase/path:varlocal/path/");
        container.put("d-base-local", p1);
        p1 = new FilePath("varlocal/path/");
        container.put("local", p1);
      }
      
      @Override public Object getValue(String variable) throws NoSuchFieldException
      { return container.get(variable); }
      
      @Override public CharSequence getCurrentDir(){ return "D:/test/currentdir"; }
    };
    try{
      CharSequence file, basepath, localdir, localfile;
      FilePath p0 = new FilePath("d:/base/path:local/path/name.ext");   
      FilePath p1 = new FilePath("name");   
      FilePath p2 = new FilePath("name.ext");   
      FilePath p3 = new FilePath("local/path/name");   
      FilePath p4 = new FilePath("base/path:local/path/name.name2.ext");   
      FilePath p5 = new FilePath("d:local/path.name");   
      FilePath p6 = new FilePath("d:/local/path.name");   
      FilePath p7 = new FilePath("d:base/path:local/path.name.ext");   
      FilePath p8 = new FilePath("d:/base/path:name.ext");   
      FilePath p9 = new FilePath("&variable");   
      FilePath p10 = new FilePath("&variable/base/path:name.ext");   
      
      FilePath p11 = new FilePath("&d-base-local/local/path/name.ext");
      basepath = p11.localdir(env);
      localdir = p11.localdir(env);
      localfile = p11.localfile(env);
      assert(StringFunctions.equals(localdir, "varlocal/path/local/path"));
      
      p11 = new FilePath("&d-base-local:local/path/name.ext");
      localdir = p11.localdir(env);
      assert(StringFunctions.equals(localdir, "local/path"));
      
      p11 = new FilePath("&d-base-local:name.ext");
      localdir = p11.localdir(env);
      assert(StringFunctions.equals(localdir, "."));
      
      FilePath p12 = new FilePath("&variable/name.ext");   
      FilePath p13 = new FilePath("&variable/base/path:local/path/name.ext");   
      Debugutil.stop();
    } catch(NoSuchFieldException exc){
      System.out.println(exc.getMessage());
    }
  }

  
  public static void main(String[] noArgs){ test(); }
}
