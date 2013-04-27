package org.vishia.fileLocalAccessor;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Event;
import org.vishia.util.EventConsumer;
import org.vishia.util.EventSource;
import org.vishia.util.EventThread;
import org.vishia.util.FileAccessZip;
import org.vishia.util.FileRemote;
import org.vishia.util.FileRemoteAccessor;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.FileRemote.CallbackCmd;
import org.vishia.util.FileRemote.CallbackEvent;
import org.vishia.util.FileRemote.Cmd;
import org.vishia.util.FileRemote.FileRemoteAccessorSelector;


/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile extends FileRemoteAccessor
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-03-31 Hartmut bugfix: number of percent in backevent while copy
   * <li>2012-11-17 Hartmut chg: review of {@link #execChgProps(org.vishia.util.FileRemote.CmdEvent)} etc. It should not work before.
   *   yet not all is tested. 
   * <li>2012-10-01 Hartmut chg: Some adaption because {@link FileRemote#listFiles()} returns File[] and not FileRemote[].
   * <li>2012-10-01 Hartmut experience {@link #useFileChildren}
   * <li>2012-10-01 Hartmut new: {@link #refreshFilePropertiesAndChildren(FileRemote, org.vishia.util.FileRemote.CallbackEvent)} time measurement
   * <li>2012-09-26 Hartmut new: {@link #refreshFileProperties(FileRemote, org.vishia.util.FileRemote.CallbackEvent)} 
   *   thread with exception msg.
   * <li>2012-08-05 Hartmut new: If the oFile reference is null, the java.io.File instance for the local file will be created anyway.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-30 Hartmut new: execution of {@link #refreshFileProperties(FileRemote, Event)} and {@link #refreshFilePropertiesAndChildren(FileRemote, Event)}
   *   in an extra thread if a callback is given. It is substantial for a fluently working with files, if an access
   *   for example in network hangs.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: implementation of the {@link FileRemote#chgProps(String, int, int, long, org.vishia.util.FileRemote.CallbackEvent)} etc.
   * <li>2012-02-02 Hartmut chg: {@link #refreshFileProperties(FileRemote, File)}: There was an faulty recursive loop,
   *   more checks. 
   * <li>2012-01-09 Hartmut new: {@link #close()} terminates the thread.
   * <li>2012-01-06 Hartmut new: {@link #refreshFileProperties(FileRemote)} etc.
   * <li>2012-01-04 Hartmut new: copy file trees started from a given directory
   * <li>2011-12-31 Hartmut new {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. 
   * <li>2011-12-31 Hartmut new {@link #runCommissions} as extra thread.  
   * <li>2011-12-10 Hartmut creation: See {@link FileRemoteAccessor}.
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
   */
  public static final int version = 20130331;

  /**Some experience possible: if true, then store File objects in {@link FileRemote#children} instead
   * {@link FileRemote} objects. The File objects may be replaces by FileRemote later if necessary. This may be done
   * in applications. The problem is: Wrapping a File with FileRemote does not change the reference in {@link FileRemote#children}
   * automatically. It should be done by any algorithm. Therefore this compiler switch is set to false yet.
   */
  private final static boolean useFileChildren = false;
  
  
  private static FileRemoteAccessor instance;
  
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };

  

  
  EventThread singleThreadForCommission = new EventThread("FileAccessor-local");
  
  /**Destination for all events which forces actions in the execution thread.
   * 
   */
  EventConsumer executerCommission = new EventConsumer("FileRemoteAccessorLocal - executerCommision"){
    @Override protected boolean processEvent_(Event ev) {
      if(ev instanceof Copy_FileLocalAcc.EventCpy){ //internal Event
        copy.stateCopy.process(ev);
        return true;
      } else if(ev instanceof FileRemote.CmdEvent){  //event from extern
            execCommission((FileRemote.CmdEvent)ev);
        return true;
      } else {
        return false;
      }
    }
    
  };
  

  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileRemoteAccessorLocalFile)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  private final Copy_FileLocalAcc copy = new Copy_FileLocalAcc(this);  
  
  private FileRemote workingDir;
  
  public FileRemoteAccessorLocalFile() {
    singleThreadForCommission.startThread();
  }
  
  
  
  /**Returns the singleton instance of this class.
   * Note: The instance will be created and the thread will be started if this routine was called firstly.
   * @return The singleton instance.
   */
  public static FileRemoteAccessor getInstance(){
    if(instance == null){
      instance = new FileRemoteAccessorLocalFile();
    }
    return instance;
  }
  

  
  
  private File getLocalFile(FileRemote fileRemote){
    //NOTE: use the superclass File only as interface, use a second instance.
    //the access to super methods does not work. Therefore access to non-inherited File.methods.
    if(fileRemote.oFile() == null){
      String path = fileRemote.getPath();
      fileRemote.setFileObject(new File(path));
    }
    return (File)fileRemote.oFile();
  }
  
  
  /*
  @Override public Object createFileObject(FileRemote file)
  { Object oFile = new File(file.path, file.name);
    return oFile;
  }
  */
  
  
  /**Sets the file properties from the local file.
   * checks whether the file exists and set the {@link FileRemote#mTested} flag any time.
   * If the file exists, the properties of the file were set, elsewhere they were set to 0.
   * @see {@link org.vishia.util.FileRemoteAccessor#refreshFileProperties(org.vishia.util.FileRemote)}
   */
  @Override public void refreshFileProperties(final FileRemote fileRemote, final FileRemote.CallbackEvent callback)
  { 
  
    
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new RunRefresh(fileRemote, callback);
  
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);
      threadObj.start(); //run in an extra thread, the caller doesn't wait.
    }
  
  }  
    

  
  @Override public void refreshFilePropertiesAndChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){

    RunRefreshWithChildren thread = new RunRefreshWithChildren(fileRemote, callback);
    
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      if((fileRemote.getFlags() & FileRemote.mThreadIsRunning) ==0) {
        fileRemote.internalAccess().setFlagBit(FileRemote.mThreadIsRunning);
        Thread threadObj = new Thread(thread);
        thread.time = System.currentTimeMillis();
        threadObj.start(); //run in an extra thread, the caller doesn't wait.
      } else {
        System.err.println("FileRemoteAccessLocalFile.refreshFilePropertiesAndChildren - double call, ignored;");
        callback.relinquish(); //ignore it.
      }
    }
  }

  
  @Override
  public List<File> getChildren(FileRemote file, FileFilter filter){
    File data = (File)file.oFile();
    File[] children = data.listFiles(filter);
    List<File> list = new LinkedList<File>();
    for(File file1: children){
      list.add(file1);
    }
    return list;
  }

  
  
  @Override public ReadableByteChannel openRead(FileRemote file, long passPhase)
  { try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  
  @Override public InputStream openInputStream(FileRemote file, long passPhase){
    try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream;
    } catch(FileNotFoundException exc){
      return null;
    }
    
  }
  

  
  @Override public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
    FileOutputStream stream = new FileOutputStream(file);
    return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  //@Override 
  public FileRemote[] XXXlistFiles(FileRemote parent){
    FileRemote[] retFiles = null;
    if(parent.oFile() == null){
      
    }
    File dir = (File)parent.oFile();
    if(dir.exists()){
      File[] files = dir.listFiles();
      if(files !=null){
        retFiles = new FileRemote[files.length];
        int iFile = -1;
        for(File fileLocal: files){
          retFiles[++iFile] = newFileInDirectory(fileLocal, parent);
        }
      }
    }
    return retFiles;
  }

  
  private FileRemote newFileInDirectory(File fileLocal, FileRemote dir){
    String name;
    //if(fileLocal.isDirectory()){
      //name = fileLocal.getName() + "/";
    //} else {
      name = fileLocal.getName();
    //}
    //File parent = fileLocal.getParentFile();
    String sDir = dir.getAbsolutePath().replace('\\', '/');
    //String sDir = fileLocal.getParent().replace('\\', '/');
    //FileRemote dir = null; //FileRemote.fromFile(parent);
    int flags = fileLocal.isDirectory() ? FileRemote.mDirectory:0;
    FileRemote fileRemote = new FileRemote(dir.itsCluster, this, dir, sDir, name, 0, 0, flags, fileLocal, true);
    //refreshFileProperties(fileRemote, null);  
    return fileRemote;
  }
  
  
  @Override public boolean delete(FileRemote file, FileRemote.CallbackEvent callback){
    File fileLocal = getLocalFile(file);
    if(callback == null){
      return fileLocal.delete();
    } else {
      boolean bOk = fileLocal.delete();
      callback.occupy(evSrc, true);
      callback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.errorDelete );
      return bOk;
    }
  }

  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  
  
  
  /**Creates an CmdEvent if necessary, elsewhere uses the opponent of the given evBack and occupies it.
   * While occupying the Cmdevent is completed with the destination, it is {@link #executerCommission}.
   * @see org.vishia.util.FileRemoteAccessor#prepareCmdEvent(org.vishia.util.FileRemote.CallbackEvent)
   */
  @Override public FileRemote.CmdEvent prepareCmdEvent(FileRemote.CallbackEvent evBack){
    FileRemote.CmdEvent cmdEvent1;
    if(evBack !=null && (cmdEvent1 = evBack.getOpponent()) !=null){
      if(!cmdEvent1.occupy(evSrc, executerCommission, singleThreadForCommission, false)){
        return null;
      }
    } else {
      cmdEvent1 = new FileRemote.CmdEvent(evSrc, executerCommission, singleThreadForCommission, evBack);
    }
    return  cmdEvent1; 
  }
  
  
  void execCommission(FileRemote.CmdEvent commission){
    FileRemote.Cmd cmd = commission.getCmd();
    switch(cmd){
      case check: //copy.checkCopy(commission); break;
      case overwr:
      case abortAll:
      case abortCopyDir:
      case abortCopyFile:
      case copy: copy.stateCopy.process(commission); break;
      case move: copy.execMove(commission); break;
      case chgProps:  execChgProps(commission); break;
      case chgPropsRecurs:  execChgPropsRecurs(commission); break;
      case countLength:  execCountLength(commission); break;
      case delete:  execDel(commission); break;
      
    }
  }
  
  
  private void execChgProps(FileRemote.CmdEvent co){
    FileRemote dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc().getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(fileRenamed);
    } else {
      dst = co.filesrc;
    }
    ok = chgFile(dst, co.maskFlags(), co.newFlags(), ok);
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done; 
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();
    
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd );
  }
  
  
  private void execChgPropsRecurs(FileRemote.CmdEvent co){
    FileRemote dst;
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc.getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(fileRenamed);
    } else {
      dst = co.filesrc;
    }
    ok &= chgPropsRecursive(dst, co.maskFlags(), co.newFlags(), ok, 0);
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done ; 
    } else {
      cmd = FileRemote.CallbackCmd.error ; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd);
  }
  
  
  
  private boolean chgPropsRecursive(File dst, int maskFlags, int newFlags, boolean ok, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(dst.isDirectory()){
      File[] filesSrc = dst.listFiles();
      for(File fileSrc: filesSrc){
        ok = chgPropsRecursive(fileSrc, maskFlags, newFlags, ok, recursion +1);
      }
    } else {
      ok = chgFile(dst, maskFlags, newFlags, ok);
    }
    return ok;
  }
  

  
  private boolean chgFile(File dst, int maskFlags, int newFlags, boolean ok){
    //if(dst instanceof FileRemote)
    //int flagsNow = dst.getFlags();
    //int chg = (flagsNow ^ newFlags) & maskFlags;  //changed and masked
    int chg = maskFlags;
    int mask = 1;
    while(mask !=0){
      if((chg & mask)!=0){ 
        if(!chgFile1(dst, mask, newFlags)){
          ok = false;
        }
      }
      mask <<=1;
    }
    return ok;
  }
  
  
  private boolean chgFile1(File dst, int maskFlags, int newFlags){
    boolean bOk;
    boolean set = (newFlags & maskFlags ) !=0;
    switch(maskFlags){
      case FileRemote.mCanWrite:{ bOk = dst.setWritable(set); } break;
      case FileRemote.mCanWriteAny:{ bOk = dst.setWritable(set, true); } break;
      default: { bOk = false; }
    }//switch
    if(bOk && dst instanceof FileRemote){
      FileRemote dst1 = (FileRemote)dst;
      dst1.internalAccess().setOrClrFlagBit(maskFlags, set);
    }
    return bOk;
  }
  
  
  
  private void execCountLength(FileRemote.CmdEvent co){
    long length = countLengthDir(co.filesrc, 0, 0);    
    FileRemote.CallbackEvent evback = co.getOpponent();
    evback.occupy(evSrc, true);
    FileRemote.CallbackCmd cmd;
    if(length >=0){
      cmd = FileRemote.CallbackCmd.done; 
      evback.nrofBytesAll = length;
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    evback.sendEvent(cmd );
  }
  
  
  private long countLengthDir(File file, long sum, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(file.isDirectory()){
      File[] filesSrc = file.listFiles();
      for(File fileSrc: filesSrc){
        sum = countLengthDir(fileSrc, sum, recursion+1);
      }
    } else {
      sum += file.length();
    }
    return sum;
  }
  
  
  
  void execDel(FileRemote.CmdEvent co){
    
  }


  @Override public void close() throws IOException
  { singleThreadForCommission.close();
  }
  
  
  
  /**A thread which gets all file properties independent of a caller of the #re
   */
  private class RunRefresh implements Runnable{
    final FileRemote fileRemote;
    
    final FileRemote.CallbackEvent callback;
    
    RunRefresh(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
      this.fileRemote= fileRemote;
      this.callback = callback;
    }
    
    public void run(){
      File fileLocal = getLocalFile(fileRemote);
      String path = fileRemote.getPath();
      if(fileLocal.exists()){
        String canonicalPath = FileSystem.getCanonicalPath(fileLocal);
        long date = fileLocal.lastModified();
        long length = fileLocal.length();
        int flags = FileRemote.mExist | FileRemote.mTested;
        if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
        if(fileLocal.isHidden()){ flags |= FileRemote.mHidden; }
        if(fileLocal.canWrite()){ flags |= FileRemote.mCanWrite; }
        if(fileLocal.canRead()){ flags |= FileRemote.mCanRead; }
        if(fileLocal.canExecute()){ flags |= FileRemote.mExecute; }
        if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
        if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
        fileRemote._setProperties(length, date, flags, fileLocal);
        if(fileLocal.isAbsolute()){
          String pathCleaned = FileSystem.cleanAbsolutePath(path);
          if(!canonicalPath.startsWith(pathCleaned)){
            fileRemote.setSymbolicLinkedPath(canonicalPath);
          } else {
            fileRemote.setCanonicalAbsPath(canonicalPath);
          }
        } else { //relative path
          Assert.stop();
          /*
          if(workingDir == null){
            workingDir = new FileRemote(FileSystem.getCanonicalPath(new File(".")));  //NOTE: should be absolute
          }
          fileRemote.setReferenceFile(workingDir);
          */  
        }
      } else { //fileLocal not exists:
        //designate it as tested, mExists isn't set.
        fileRemote._setProperties(0, 0, FileRemote.mTested, fileLocal);
      }
      fileRemote.timeRefresh = System.currentTimeMillis();
      if(callback !=null){
        callback.occupy(evSrc, true);
        callback.sendEvent(FileRemote.CallbackCmd.done);
      }
    }
    
    
  }
  
  
  /**A thread which gets all file properties inclusive children independent of a caller of the #re
   */
  private class RunRefreshWithChildren implements Runnable{
    long time;
    
    final FileRemote fileRemote;
    
    final FileRemote.CallbackEvent callback;
    
    RunRefreshWithChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
      this.fileRemote= fileRemote;
      this.callback = callback;
    }
    
    public void run(){
      try{
        refreshFileProperties(fileRemote, null);
        File fileLocal = getLocalFile(fileRemote);
        //fileRemote.flags |= FileRemote.mChildrenGotten;
        if(fileLocal.exists()){
          long time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - start listFiles; dt=" + (time1 - time));
          
          File[] files = fileLocal.listFiles();
          time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - ok listFiles; dt=" + (time1 - time));
          if(files !=null){
            if(useFileChildren){
              //fileRemote.children = files;
            } else {
              //re-use given children because they may have additional designation in flags.
              Map<String, FileRemote> oldChildren = fileRemote.children();
              //but create a new list to prevent keeping old files.
              fileRemote.internalAccess().newChildren();
              int iFile = -1;
              for(File file1: files){
                FileRemote child = null;   
                if(oldChildren !=null){ child = oldChildren.remove(file1.getName()); }
                if(child == null){ child = newFileInDirectory(file1, fileRemote); }
                fileRemote.putChildren(child);
                child.refreshProperties(null);
              }
              //oldChildren contains yet removed files.
            }
          }
        }
        fileRemote.timeChildren = System.currentTimeMillis();
        if(callback !=null){
          callback.occupy(evSrc, true);
          long time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - callback listFiles; dt=" + (time1 - time));
          callback.sendEvent(FileRemote.CallbackCmd.done);
          time1 = System.currentTimeMillis();
          System.out.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - finish listFiles; dt=" + (time1 - time));
        }
        fileRemote.internalAccess().clrFlagBit(FileRemote.mThreadIsRunning);
      }
      catch(Exception exc){
        System.err.println("FileRemoteAccessorLocalFile.refreshFilePropertiesAndChildren - Thread Excpetion;" + exc.getMessage());
      }
    }
  }
    

  
  
  
  public static FileRemote.FileRemoteAccessorSelector selectLocalFileAlways = new FileRemote.FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileRemoteAccessorLocalFile.getInstance();
    }
  };
  
}
