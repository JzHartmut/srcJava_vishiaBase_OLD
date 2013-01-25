package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.vishia.mainCmd.MainCmd;

//import org.apache.tools.zip.ZipEntry;

/**This class supports working with zip files using the standard java zip methods.
 * This class supports a base path and wildcards like described 
 * in {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
 * <br><br>
 * Usage template:
 * <pre>
    Zip zip = new Zip();   //instance can be re-used in the same thread.
    zip.addSource(directory, path);
    zip.addSource(directory2, path2);
    String sError = zip.exec(dst, compressionLevel, comment);
    //the added sources are processed and removed.
    //next usage:
    zip.addSource(directory, path);
    String sError = zip.exec(dst, compressionLevel, comment);
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public class Zip {

  
  /**Version, history and license.
   * <ul>
   * <li>2013-01-20 Hartmut creation: New idea to use the zip facility of Java.
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
   */
  public final static int version = 20130120;

  
  
  
  private class Src { 
    String path; File dir;
    Src(String path, File dir) { this.path = path;  this.dir = dir; } 
  }
  
  private final List<Src> listSrc = new LinkedList<Src>();
  
  
  public Zip(){}
  
  
  /**Adds a source file or some files designated with wildcards
   * @param src Path may contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. The localpath may contain wildcards. The basepath may be an absolute path or it is located
   *   in the systems current directory..
   *   If the sPath does not contain a basepath (especially it is a simple path to a file), this path is used in the zipfile.
   *   Especially the path can start from the current directory.
   *   For usage of basepath, localpath and wildcards see {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
   */
  public void addSource(String src){
    listSrc.add(new Src(src, null));
  }
  

  
  /**Adds a source file or some files designated with wildcards
   * @param dir the directory where the source path starts.
   * @param src Path may contain a basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. The localpath may contain wildcards.
   *   If the sPath does not contain a basepath (especially it is a simple path to a file), this path is used in the zipfile.
   *   For usage of basepath, localpath and wildcards see {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
   */
  public void addSource(File dir, String src){
    listSrc.add(new Src(src, dir));
  }
  

  
  /**Executes the zip with the given source files to a dst file.
   * @param dst The destination file.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @return an error hint or null if successful.
   */
  public String exec(File dst, int compressionLevel, String comment){
    String ret;
    final byte[] buffer = new byte[0x4000];
    try {
      ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(dst));
      
      outZip.setComment(comment);
      outZip.setLevel(compressionLevel);
      
      
      for(Src src: listSrc){
        //get the files.
        List<FileSystem.FileAndBasePath> listFiles= new ArrayList<FileSystem.FileAndBasePath>();
        
        FileSystem.addFilesWithBasePath (src.dir, src.path, listFiles);
        
        for(FileSystem.FileAndBasePath filentry: listFiles){
          ZipEntry zipEntry = new ZipEntry(filentry.localPath);
          outZip.putNextEntry(zipEntry);
          InputStream in = new FileInputStream(filentry.file);
          int bytes;
          while( (bytes = in.read(buffer))>0){
            outZip.write(buffer, 0, bytes);
          }
          in.close();
          outZip.closeEntry();
        }
      }  
      outZip.close();
      
      ret = null;
      
    } catch (FileNotFoundException e) {
      ret = "<?? vishia.zip.Zip - File not found: "+ e.getMessage() + "??>";
    } catch (IOException e) {
      ret = "<?? vishia.zip.Zip - Any file writing problem: "+ e.getMessage() + "??>";
    }
    listSrc.clear();
    return ret;
    
  }
  
  
  
  
  /**Zips some files in a dst file.
   * @param dst The destination file.
   * @param sPath Path should contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. For usage of basepath, localpath see {@link org.vishia.util.FileSystem#addFilesWithBasePath(File, String, List)}.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @return an error hint or null if successful.
   */
  public static String zipfiles(File dst, File srcdir, String sPath, int compressionLevel, String comment){
    Zip zip = new Zip();
    zip.addSource(sPath);
    return zip.exec(dst, compressionLevel, comment);
  }

  
  
  public static void main(String[] args){
    
    Zip zip = new Zip();
    Cmdline cmd = zip.new Cmdline();
    try{ cmd.parseArguments(args);
    
    } catch(ParseException exc){
      cmd.setExitErrorLevel(MainCmd.exitWithArgumentError);
      cmd.exit();
    }
    cmd.executeMain();
  }
  
  
  interface SetArgument{ boolean setArgument(String val); }
  
  static class Arguments{ 
    final String arg; final String help; final SetArgument set;
    Arguments(String arg, String help, SetArgument set){
      this.arg = arg; this.help = help; this.set = set;
    }
  }
  

  class Cmdline extends MainCmd {

    int compress = 5;
    
    File fOut;
    
    SetArgument setCompress = new SetArgument(){ @Override public boolean setArgument(String val){ 
      char cc;
      if(val.length()== 1 && (cc=val.charAt(0))>='0' && cc <='9'){
        compress = cc-'0';
        return true;
      } else return false;
    }};
    
    SetArgument setInput = new SetArgument(){ @Override public boolean setArgument(String val){ 
      addSource(val);
      return true;
    }};
    
    SetArgument setOutput = new SetArgument(){ @Override public boolean setArgument(String val){ 
      fOut = new File(val);
      return true;
    }};
    
    Arguments[] argList =
    { new Arguments("-compress", "set the compression rate 0=non .. 90max", setCompress)
    , new Arguments("-o", "the output file for zip output", setOutput)
    , new Arguments("", "an input file possible with wildcards also in path like \"path/**/dir*/name*.ext*\"", setInput)
    };
    
    
    @Override
    protected boolean checkArguments() {
      if(fOut !=null){ return true; }
      else return false;
    }

    @Override
    protected boolean testArgument(String argc, int nArg) throws ParseException {
      for(Arguments argTest : argList){
        int argLen = argTest.arg.length();
        int argclen = argc.length();
        if(argLen == 0 
          || (argc.startsWith(argTest.arg)                //correct prefix 
             && (  argclen == argLen                      //only the prefix
                || ":=".indexOf(argc.charAt(argLen))>=0)  //or prefix ends with the separator characters.
                )
          ){ //then the argument is correct and associated to this argTest.
          String argval = argclen == argLen //no additional value, use argument 
                        || argLen == 0      //argument without prefix (no option)
                        ? argc              //then use the whole argument as value.
                        : argc.substring(argLen+1);  //use the argument after the separator as value.
          boolean bOk = argTest.set.setArgument(argval);   //call the user method for this argument.
          if(!bOk) throw new ParseException("Argument value error: " + argc, nArg);
          return true;
        }
      }
      //argument not found (not returned in for-loop):
      return false;
    }
    
    
    
    void executeMain(){
      exec(fOut,compress, "");
    }
  }
  
  
  
  public static void test2(){
    String err;
    if( (err = zipfiles(new File("testzip.zip"), null, "./.:org/**/*", 9, "zip comment")) !=null){
      System.err.println(err);
    }
  }
  
  
}
