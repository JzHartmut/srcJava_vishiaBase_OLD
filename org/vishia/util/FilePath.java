package org.vishia.util;

import java.util.LinkedList;
import java.util.List;



/**This class holds a path to a file with its parts base path, directory path, name, extension, drive letter etc
 * and supports getting String-paths in several forms - absolute, relative, name only, extension, with slash or backslash etc.
 * It supports using of script variables too.
 * 
 * For access to the path the user should provide an implementation of the {@link FilePathEnvAccess}
 * which supports access to String-given variables and the current directory of the user's environment.
 * 
 * @author hartmut
 *
 */
public class FilePath
{
  /**Version, history and license.
   * <ul>   
   * <li>2014-04-05 Hartmut create from org/vishia/cmd/ZGenFilepath, usage more universal.     
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   *   This file was copied from srcJava_Zbnf/org/vishia/zmake/Userfilepath.
   *   The data of a file are referenced with {@link #data}. The original fields are contained in
   *   {@link ZGenScript.Filepath}. Both are separated because the parts in ZGenScript are set completely
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

  public interface FilePathEnvAccess {
    Object getValue(String variable) throws NoSuchFieldException;
    
    CharSequence getCurrentDir();
  }
  
  
  
  FilePath data = this;
  
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
  

  
  /**parses the string given path. */
  public FilePath(String pathP){
    String path = pathP.replace('\\', '/');
    int zpath = path.length();
    int posColon = path.indexOf(':');
    int pos1slash = path.indexOf('/');
    int posbase;   //start of base path, ==poslocal if not given
    int poslocal;  //start of local path or whole path, after ':' or after root-/ 
    if(zpath >=1 && path.charAt(0) == '&'){ //starts with a script variable:
      int pos9 = posColon > 0 && (posColon < pos1slash || pos1slash < 0) ? posColon : pos1slash > 0 ? pos1slash : zpath;
      this.scriptVariable = path.substring(1, pos9);
      absPath = false;  //hint: it may be an absolute path depending of content of scriptVariable 
      if(pos9 < zpath){  
        //following content after variable:
        posbase = pos9+1;
        poslocal = posColon >0 ? posColon+1 : pos9+1;
      } else {
        posbase = poslocal = -1;
      }
    } else if(posColon == 1){ //it means a ':' is found anywhere: check if it is a drive designation
      drive = path.substring(0,1);
      posColon = path.indexOf(':', 2);
      int pos1;
      if(pos1slash == 2){
        posbase = 3;
        absPath = true;
      } else {
        posbase = 2;
        absPath = false;
      }
      poslocal = posColon >0 ? posColon+1 : posbase;
    } else {
      if(pos1slash == 0){
        posbase = 1;
        absPath = true;
      } else {
        posbase = 0;
        absPath = false;
      }
      poslocal = posColon >0 ? posColon+1 : posbase;
    }
    //drive, absPath is set.
    //posbase, poslocal is set.
    //
    if(posbase < 0){ //nothing given
      basepath = null;
      localdir = "";
      name = "";
      ext = "";
    } else {
      int posname = path.lastIndexOf('/') +1;
      if(posname < poslocal){ posname = poslocal; }
      //
      if(poslocal > posbase){  //':' found, note posbase may be -1 
        basepath = path.substring(posbase, poslocal-1);
      } else { 
        basepath = null; // left empty
      }
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
    }
  }
  
  
  public boolean isNotEmpty(){
    return basepath !=null || localdir.length() >0 || name.length() >0 || drive !=null;
      
  }
  
  
  @Override public String toString() {
    StringBuilder u = new StringBuilder();
    if(drive!=null) { u.append(drive); }
    if(absPath) { u.append("/"); }
    if(basepath!=null) { u.append(basepath).append(":"); }
    if(localdir.length()>0) { u.append(localdir).append("/"); }
    u.append(name).append(ext);
    return u.toString();
  }
  

  
  
  /**Inserts the given drive letter and the root designation on start of buffer. It does nothing if the path is relative.
   * @param u The buffer
   * @param commonBasepath An common base path in fileset
   * @param accesspath An access path while using a fileset
   * @return true if it is a root path or it has a drive letter.
   */
  CharSequence driveRoot(CharSequence basepath, FilePath commonBasepath, FilePath accesspath){
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
   * If this file contains a basepath, all other access, common and a variable is used as full file path
   * as prefix of this base path. If this doesn't contain a basepath, either the common and access path presents
   * the base path, or of one of them contains a basepath, that is the basepath. This behavior is complementary 
   * to the behavior of {@link #localDir(StringBuilder, FilePath, FilePath)}.
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
   * the {@link #localFile(StringBuilder, FilePath, FilePath)} is added too.
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
   * @throws NoSuchFieldException 
   *  
   */
  public CharSequence basepath(StringBuilder uRetP, FilePath commonPath, FilePath accessPath, boolean[] useBaseFile, FilePathEnvAccess env) throws NoSuchFieldException 
  { 
    //if(generalPath == null){ generalPath = emptyParent; }
    //first check singulary conditions
    ///
    int pos;
    FilePath varfile;
    Object varO;
    if((data.basepath !=null || useBaseFile !=null && useBaseFile[0]) && data.scriptVariable !=null){
      //get the variable if a base path is given or the file may be used as base path
      varO = env.getValue(data.scriptVariable);
      varfile = varO instanceof FilePath ? (FilePath) varO : null;
    } else { 
      varfile = null;
      varO = null;
    }
    if(data.absPath){
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x  x      x  x   x     1    1      /thisBase                   : thisLocal
      //  x  x      x  x   x     0    1      "/"                         : thisLocal
      if(data.drive !=null || data.basepath !=null || uRetP !=null || varO !=null || data.envVariable !=null){
        StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
        uRet.setLength(0);
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
        assert(uRetP == null && data.basepath ==null && data.drive == null);
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
      if(commonPath !=null || accessPath !=null || varO !=null || data.envVariable !=null){
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
        if(data.basepath.length() >0 || varO !=null && varfile == null || data.envVariable !=null){
          //need to add somewhat, build the StringBuilder if not done.
          if(prepath instanceof StringBuilder){
            uRet = (StringBuilder)prepath;
          } else {
            assert(uRet == null);  //elsewhere it might be used for prepath
            uRet = prepath !=null ? new StringBuilder(prepath) : new StringBuilder();
          }
        }
        final CharSequence text;
        if(varO !=null && varfile == null){
          text = varO.toString();
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
      StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
      if(varfile !=null && (varfile.data.basepath !=null || useBaseFile !=null && useBaseFile[0])){
        //use the variableFile if it is called recursively. 
        //The variable is that one from commonPath or accessPath of the caller.
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

        //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
        //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
        prepath = varfile.basepath(uRet, commonPath, accessPath, useBaseFileSub, env);
      }
      else if(commonPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = commonPath.basepath(uRet, null, accessPath, useBaseFileSub, env);
      } 
      else if(accessPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = accessPath.basepath(uRet, null, null, useBaseFileSub, env);
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
   * @throws NoSuchFieldException if a requested scriptvariable was not found.
   *  
   */
  public CharSequence localDir(StringBuilder uRet, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) throws NoSuchFieldException {
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
      //ZGenScript.FilesetVariable var;
      FilePath varfile;
      Object varO;
      if(data.scriptVariable !=null){
        varO = env.getValue(data.scriptVariable);
        varfile = varO instanceof FilePath ? (FilePath) varO : null;
      } else { 
        varfile = null;
        varO = null;
      }
      if(varfile !=null){
        varfile.localFile(uRet, commonPath, accessPath, env);  //The full varfile.localfile should use.
      }
      else if(commonPath !=null){
        commonPath.localFile(uRet, null, accessPath, env);
      } 
      else if(accessPath !=null){
        accessPath.localFile(uRet, null, null, env);
      }
      //
      if(varfile ==null && varO != null){
        uRet.append(varO.toString());   //another variable than a Filepath, it is used as String
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
  
  
  public CharSequence localFile(StringBuilder uRet, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) 
  throws NoSuchFieldException {
    CharSequence dir = localDir(uRet, commonPath, accessPath, env);
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
    CharSequence basepath = basepath(null, emptyParent, null, null, env);
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
    int zpath = (data.localdir == null) ? 0 : data.localdir.length();
    if(zpath > 0){ //not empty
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(data.localdir);
    }
    return uRet;
    */
  }
  
  public CharSequence absdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absdir(env)); }
  
  
  /**Method can be called in the generation script: <*data.absname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   Either as absolute or as relative path.
   * @throws NoSuchFieldException 
   */
  public CharSequence absname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name);
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
    uRet.append(data.ext);
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
    uRet.append(data.ext);
    return uRet;
  }
  
  /**Method can be called in the generation script: <*basepath()>. 
   * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
   *   till a ':' in the input path or an empty string.
   *   Either as absolute or as relative path how it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence basepath(FilePathEnvAccess env) throws NoSuchFieldException{ return basepath(null, emptyParent, null, null, env); }
   
  

  
  public CharSequence basepathW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(basepath(env)); }
  
  
  
  /**Method can be called in the generation script: <*path.dir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence dir(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int zpath = (data.localdir == null) ? 0 : data.localdir.length();
    if(zpath > 0){
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(data.localdir);
    }
    return uRet;
  }

  
  
  public CharSequence dirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(dir(env)); }
  
  /**Method can be called in the generation script: <*data.pathname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence pathname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name);
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
    CharSequence basePath = basepath(null, commonPath, accesspath, null, env);
    uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    localDir(uRet, emptyParent, accesspath, env);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(data.name).append(data.ext);
    return uRet.append(data.ext);
  }
  
  
  
  /**Method can be called in the generation script: <*data.file()>. 
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence file(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    return uRet.append(data.ext);
  }
  
  public CharSequence fileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(file(env)); }
  
  
  
  /**Method can be called in the generation script: <*data.base_localdir()>. 
   * @return the basepath:localpath in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence base_localdir(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(data.localdir);
    return uRet;
  }
  
  public CharSequence base_localdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(base_localdir(env)); }
  
  
  /**Method can be called in the generation script: <*data.base_localfile()>. 
   * @return the basepath:localpath/name.ext in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence base_localfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(this.data.localdir);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.data.name);
    uRet.append(this.data.ext);
    return uRet;
  }
  
  public CharSequence base_localfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(base_localfile(env)); }
  
  
  

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
  private CharSequence addLocalName(StringBuilder uRet, FilePath replWildc){ 
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
  
  /**Builds non-wildcard instance for any found file and add all these instances to the given list
   * for all files, which are selected by this instance maybe with wild-cards and the given commonPath and accessPath.
   * The expansion with wild-cards is a capability of {@link FileSystem#addFilesWithBasePath(File, String, List)}
   * which is used here as core routine.
   * 
   * @param listToadd List which will be completed with all found files
   * @param commonPath if not null, the path before this given file path. 
   * @param accessPath if not null, the path before a commonPath and before this given file path.
   * @throws NoSuchFieldException 
   */
  public void expandFiles(List<FilePath> listToadd, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) throws NoSuchFieldException{
    List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
    final CharSequence basepath1 = this.basepath(null, commonPath, accessPath, null, env); //getPartsFromFilepath(file, null, "absBasePath").toString();
    int posRoot = isRootpath(basepath1);
    final CharSequence basePathNonRoot = posRoot == 0 ? basepath1: basepath1.subSequence(posRoot, basepath1.length());
    final String basepath = basePathNonRoot.toString();  //persistent content.
    String drive = posRoot >=2 ? Character.toString(basepath1.charAt(0)) : null;
    boolean absPath = posRoot == 1 || posRoot == 3;
    
    final CharSequence absBasepath = absbasepath(basepath1, env);
    
    final CharSequence localfilePath = this.localFile(null, commonPath, accessPath, env); //getPartsFromFilepath(file, null, "file").toString();
    final String sPathSearch = absBasepath + ":" + localfilePath;
    try{ FileSystem.addFilesWithBasePath(null, sPathSearch, listFiles);
    } catch(Exception exc){
      //let it empty. Files may not be found.
    }
    for(FileSystem.FileAndBasePath file1: listFiles){
      FilePath filepath2 = new FilePath();  //new instance for found file.
      filepath2.data.absPath = absPath;
      filepath2.data.drive = drive;
      filepath2.data.basepath = basepath;  //it is the same. Maybe null
      int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
      int posExt = file1.localPath.lastIndexOf('.');
      final String sPath = posName >0 ? file1.localPath.substring(0, posName-1) : "";  //"" if only name
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
    FilePath p11 = new FilePath("&variable:name.ext");   
    FilePath p12 = new FilePath("&variable/name.ext");   
    FilePath p13 = new FilePath("&variable/base/path:local/path/name.ext");   
    Debugutil.stop();
  }

  
  public static void main(String[] noArgs){ test(); }
}
