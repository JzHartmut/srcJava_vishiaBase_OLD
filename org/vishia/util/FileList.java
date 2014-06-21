package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.CRC32;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;

/**This class creates a list which information about a file tree and supports change timestamp of given files (touch). 
 * A file list can be created from any directory tree. 
 * It contains properties of files: timestamp, length, relative path inside this file tree
 * and a CRC checksum of the file content.
 * The list can be used to 
 * <ul>
 * <li>touch the timestamp of files if their length and CRC matches.
 * <li>compare files with the content of the list - detect changes, maybe used for version check.
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public class FileList
{

  /**Version, history and license.
   * <ul>
   * <li>2014-01-14 Hartmut chg: round up and down to 10 seconds, to ignore second differences on writing.
   * <li>2013-08-09 Hartmut created: The FileList was written by me in 1992..2001 in C++-Language.
   *   Now it is available for Java usage. One of the motivation was the necessity of correction of
   *   time stamps of reverted files from Bazaar and git.
   * </ul>
   * <br><br>
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
   */
  public static final int version = 20130808;

  
  public static class Args
  {
    public char cCmd;
    
    /**Directory where the list is regarded to. */
    public String sDirectory;
    
    public String sMask;
    
    /**Any output for operation. One line per file. */
    public Appendable out;
    
    //public String sDateFormat = "yyyy-MM-dd_HH:mm:ss";
    
    public boolean crc = true;
    
    /**Name, maybe path of the file list relative to {@link #sDirectory}. */
    public String sFileList;
  }
  
  final Args args;
  
  
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
  
  final StringFormatter formatter = new StringFormatter(new StringBuilder(100));

  final CRC32 crcCalculator = new CRC32();

  
  public FileList(Args args){
    this.args = args;
    if(args.out == null){
      args.out = System.out;
    }
  }
  
  
  /**Static method to create a list from any directory maybe with selected files.
   * @param dir path to any directory.
   * @param mask Use "*" to select all files.
   * @param sFilelist Name of the file list relative to the dir, can contain a relative path.
   * @throws IOException
   */
  public static void list(String dir, String mask, String sFilelist) throws IOException
  {
    FileList.Args args = new FileList.Args();
    args.sDirectory = dir;
    args.crc = true;
    args.sMask = mask;
    args.sFileList = dir + "/" + sFilelist;
    FileList main = new FileList(args);
    main.list();
  }
  
  
  /**Creates a list of all files which's path and mask is given by {@link #args}. 
   * @throws IOException */
  protected void list() throws IOException
  {
    List<FileSystem.FileAndBasePath> list = new LinkedList<FileSystem.FileAndBasePath>();
    FileSystem.addFilesWithBasePath(new File(args.sDirectory), args.sMask, list);
    Map<String, FileSystem.FileAndBasePath> sort = new TreeMap<String, FileSystem.FileAndBasePath>();
    for(FileSystem.FileAndBasePath entry: list){
      boolean bExclude = false;
      if(entry.localPath.startsWith("/.bzr")){ 
        bExclude = true;
      }
      if(!bExclude){  
        sort.put(entry.localPath, entry);  //sort alphabetical
      }
    }
    Writer out = null;
    out = new java.io.FileWriter(args.sFileList);
    for(Map.Entry<String, FileSystem.FileAndBasePath> entry: sort.entrySet()){
      writeOneFile(out, entry.getValue());
    }
    if(out !=null) { try{ out.close(); } catch(IOException exc){}}
  }
  
  
  
  
  
  @SuppressWarnings("boxing")
  private void writeOneFile(Writer out, FileSystem.FileAndBasePath entry) throws IOException
  {
    long date = entry.file.lastModified();
    date = ((date + 5000) /10000) * 10000;      //round up and down to 10 seconds, to ignore second differences
    long length = entry.file.length();
    formatter.reset();
    formatter.addint(length, "2222'222'222'222 ");
    StringBuilder flags = new StringBuilder("       ");
    if(entry.file.isDirectory()){ flags.setCharAt(1, 'D'); }
    if(entry.file.canExecute()){ flags.setCharAt(2, 'x'); }
    if(!entry.file.canRead()){ flags.setCharAt(3, 'h'); }
    if(!entry.file.canWrite()){ flags.setCharAt(4, 'r'); }
    if(entry.file.isHidden()){ flags.setCharAt(5, 'H'); }
    int crc = 0;
    if(args.crc && entry.file.isDirectory()){
      formatter.add("==DIR===");
    } else {
      try{
        crcCalculator.reset();
        InputStream inp = new FileInputStream(entry.file);
        byte[] buffer = new byte[4096];
        int bytes;
        while((bytes = inp.read(buffer)) >0){
          crcCalculator.update(buffer, 0, bytes);
        }
        inp.close();
        crc = (int)crcCalculator.getValue();
        formatter.addHex(crc, 8);
      } catch(Exception exc){
        formatter.add("????????");        
      }
    }
    formatter.add(' ');
    out.append(dateFormat.format(new Date(date)));
    out.append(flags);
    out.append(formatter.getBuffer());
    out.append(entry.localPath);
    out.append("\n");
  }
  
  
  
  /**Static method to create a list from any directory maybe with selected files.
   * @param dir path to any directory.
   * @param mask Use "*" to select all files.
   * @param sFilelist Name of the file list relative to the dir, can contain a relative path.
   * @throws IOException
   */
  public static void touch(String dir, String sFilelist, Appendable out) throws IOException
  {
    FileList.Args args = new FileList.Args();
    args.out = out;
    args.sDirectory = dir;
    args.crc = true;
    args.sMask = "*";
    args.sFileList = dir + "/" + sFilelist;
    FileList main = new FileList(args);
    main.touch();
  }
  
  

  
  
  public void touch(){
    BufferedReader inp = null;
    File dir = new File(args.sDirectory);
    try {
      inp = new BufferedReader(new FileReader(args.sFileList));
      String sLine;
      while((sLine = inp.readLine())!=null){
        readOneLine(sLine, dir);  
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if(inp !=null) { try{ inp.close(); } catch(IOException exc){}}
  }
  
  
  private void readOneLine(String sLine, File dir){
    Date filetime;
    String sPath;
    try {
      filetime = dateFormat.parse(sLine);
      long listtime = filetime.getTime();
      long listlen = StringFunctions.parseLong(sLine, 26, 16, 10, null, " '");
      sPath = sLine.substring(53);
      File file = new File(dir, sPath);
      if(!file.exists()){
        args.out.append("FileList - touch, file not exist; ").append(sPath).append("\n"); 
      } else if(file.isDirectory()){
        //do nothing for a directory.
      } else {
        long lastModify = file.lastModified();
        if(file.length() == listlen){
          if(Math.abs(listtime - lastModify) > 6000){ //round effect of seconds to 10!
            //check crc
            crcCalculator.reset();
            InputStream inp = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytes;
            while((bytes = inp.read(buffer)) >0){
              crcCalculator.update(buffer, 0, bytes);
            }
            inp.close();
            int crc = (int)crcCalculator.getValue();
            int crclist = StringFunctions.parseIntRadix(sLine, 43, 8, 16, null);
            if(crc == crclist){
              file.setLastModified(listtime);
              args.out.append("FileList - touching; ").append(sPath).append("\n"); 
            } else {
              args.out.append("FileList - touch, file with same length is changed; ").append(sPath).append("\n"); 
            }
          } else {
            //file may not be changed, has the correct timestamp
          }
        } else {
          args.out.append("FileList - touch, file is changed; ").append(sPath).append("\n"); 
        }
      }
      //System.out.println(filetime.toGMTString() + " " + sPath);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
  
  
  
  
  public static void main(String[] sArgs){
    smain(sArgs, true);
  }
  
  
  /**Invocation from another java program without exit the JVM
   * @param sArgs same like {@link #main(String[])}
   * @return "" or an error String
   */
  public static String smain(String[] sArgs){ return smain(sArgs, false); }

  
  private static String smain(String[] sArgs, boolean shouldExitVM){
    String sRet = null;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    try{
      try{ mainCmdLine.parseArguments(); }
      catch(Exception exception)
      { sRet = "Jbat - Argument error ;" + exception.getMessage();
        mainCmdLine.report(sRet, exception);
        mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      }
      if(args.cCmd !=0){
        FileList main = new FileList(args);     //the main instance
        if(sRet == null)
        { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
              to hold the contact to the command line execution.
          */
          try{ 
            switch(args.cCmd){
              case 'L': main.list(); break;
              case 'T': main.touch(); break;
            }
          }
          catch(Exception exception)
          { //catch the last level of error. No error is reported direct on command line!
            sRet = "Jbat - Any internal error;" + exception.getMessage();
            mainCmdLine.report(sRet, exception);
            exception.printStackTrace(System.out);
            mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
          }
        }
      } else {
        mainCmdLine.writeHelpInfo(null);
      }
    } catch(Exception exc){
      sRet = exc.getMessage();
    }
    
    if(shouldExitVM) { mainCmdLine.exit(); }
    return sRet;
  }



  
  
  protected static class CmdLine extends MainCmd
  {

    public final Args argData;

    protected final MainCmd.Argument[] argList =
    { new MainCmd.Argument("", "[L|T|C|D]: List, Touch date, Compare, last files per Date"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.cCmd = val.charAt(0); return true;
        }})
    , new MainCmd.Argument("-l", ":<files.txt> The list file "
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sFileList = val; return true;
        }})
    , new MainCmd.Argument("-d", ":<directory path>  "
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sDirectory = val; return true;
        }})
    , new MainCmd.Argument("-m", ":<Mask> mask of files"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sMask = val; return true;
        }})
    };


    protected CmdLine(Args argData, String[] sCmdlineArgs){
      super(sCmdlineArgs);
      this.argData = argData;
      super.addAboutInfo("Generating a list of files or evaluating it");
      super.addAboutInfo("made by HSchorrig, Version 1.0, 2013-08-07..2013-08-07");
      super.addArgument(argList);
      super.addStandardHelpInfo();
      
    }
    
    @Override protected void callWithoutArguments()
    { //overwrite with empty method - it is admissible.
    }

    
    @Override protected boolean checkArguments()
    {
      return true;
    } 
    
  }
  
}
