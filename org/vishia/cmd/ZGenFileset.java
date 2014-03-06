package org.vishia.cmd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZGenFileset
{
  
  /**Version, history and license.
   * <ul>
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

  final ZGenExecuter.ExecuteLevel zgenlevel;
  final ZGenScript.UserFileset data;
  
  public ZGenFileset(ZGenExecuter.ExecuteLevel zgenlevel, ZGenScript.UserFileset data){
    this.zgenlevel = zgenlevel;
    this.data = data;
  }
  
  
  
  void listFilesExpanded(List<ZGenFilepath> files, ZGenFilepath accesspath, boolean expandFiles) {  ////
    File currdir = zgenlevel.currdir();
    for(ZGenScript.UserFilepath scriptFilepath: data.filesOfFileset){
      ZGenFilepath filepath = new ZGenFilepath(zgenlevel, scriptFilepath);
      ZGenFilepath commonBasepath = data.commonBasepath ==null ? null : new ZGenFilepath(zgenlevel, data.commonBasepath);
      if(expandFiles && (filepath.data.someFiles || filepath.data.allTree)){
        filepath.expandFiles(files, commonBasepath, accesspath, currdir);
      } else {
        //clone filepath! add srcpath
        ZGenFilepath targetsrc = new ZGenFilepath(zgenlevel, filepath, commonBasepath, accesspath);
        files.add(targetsrc);
      }
    }
  }

  public List<ZGenFilepath> listFilesExpanded(ZGenFilepath accesspath, boolean expandFiles) { 
    List<ZGenFilepath> files = new ArrayList<ZGenFilepath>();
    listFilesExpanded(files, accesspath, expandFiles);
    return files;
  }
  
  
  public List<ZGenFilepath> listFilesExpanded() { return listFilesExpanded(null, true); }

    
    
  @Override
  public String toString(){ 
    StringBuilder u = new StringBuilder();
    if(data.commonBasepath !=null) u.append("basepath="+data.commonBasepath+", ");
    u.append(data.filesOfFileset);
    return u.toString();
  }

  
  
}
