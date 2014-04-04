package org.vishia.util;

public class FilePath
{
  
  
  
  /**From ZBNF: $<$?scriptVariable>. If given, then the basePath() starts with it. 
   */
  public String scriptVariable, envVariable;
  
  /**The drive letter if a drive is given. */
  public String drive;
  
  /**From Zbnf: [ [/|\\]<?@absPath>]. Set if the path starts with '/' or '\' maybe after drive letter. */
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
