package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.util.FileRemote.CmdEvent;
import org.vishia.util.FileRemote.FileRemoteAccessorSelector;
import org.vishia.util.FileRemote.CallbackEvent;

/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile extends FileRemoteAccessor
{
  
  /**Version, history and license.
   * <ul>
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
  public static final int version = 20120721;

  private static FileRemoteAccessor instance;
  
  /**State of execution commissions.
   * '?': not started. 'w': waiting for commission, 'b': busy, 'x': should finish, 'z': finished
   */
  private char commissionState = '?';
  
  
  /**List of all commissions to do. */
  private final ConcurrentLinkedQueue<FileRemote.CallbackEvent> commissions = new ConcurrentLinkedQueue<FileRemote.CallbackEvent>();
  
  
  /**The thread to run all commissions. */
  /*
  protected Runnable runCommissions = new Runnable(){
    @Override public void run(){
      runCommissions();
    }
  };
  */
  
  //private Thread thread = new Thread(runCommissions, "vishia.FileLocal");
  //{ thread.start(); }
  
  
  EventThread singleThreadForCommission = new EventThread("FileAccessor-local");
  
  
  
  private Copy copy = new Copy();
  
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
    if(fileRemote.oFile == null){
      String path = fileRemote.getPath();
      fileRemote.oFile = new File(path);
    }
    return (File)fileRemote.oFile;
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
  @Override public void refreshFileProperties(final FileRemote fileRemote, final Event callback)
  { 
  
    
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new Runnable(){
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
            if(workingDir == null){
              workingDir = new FileRemote(FileSystem.getCanonicalPath(new File(".")));  //NOTE: should be absolute
            }
            fileRemote.setReferenceFile(workingDir);  
          }
        } else { //fileLocal not exists:
          //designate it as tested, mExists isn't set.
          fileRemote._setProperties(0, 0, FileRemote.mTested, fileLocal);
        }
        if(callback !=null){
          callback.sendEvent(0);
        }
      }
    };
  
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);
      threadObj.start(); //run in an exttra thread, the caller doesn't wait.
    }
  
  }  
    

  
  @Override public void refreshFilePropertiesAndChildren(final FileRemote fileRemote, final Event callback){
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new Runnable(){
      public void run(){
        refreshFileProperties(fileRemote, null);
        File fileLocal = getLocalFile(fileRemote);
        //fileRemote.flags |= FileRemote.mChildrenGotten;
        if(fileLocal.exists()){
          File[] files = fileLocal.listFiles();
          if(files !=null){
            fileRemote.children = new FileRemote[files.length];
            int iFile = -1;
            for(File file1: files){
              fileRemote.children[++iFile] = newFile(file1);
            }
          }
        }
        if(callback !=null){
          callback.sendEvent(0);
        }
        fileRemote.flags &= ~FileRemote.mThreadIsRunning;
      }
    };
      
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      if((fileRemote.flags & FileRemote.mThreadIsRunning) ==0) {
        fileRemote.flags |= FileRemote.mThreadIsRunning;
        Thread threadObj = new Thread(thread);
        threadObj.start(); //run in an exttra thread, the caller doesn't wait.
      } else {
        callback.consumedRetain(); //ignore it.
      }
    }
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
    if(parent.oFile == null){
      
    }
    File dir = (File)parent.oFile;
    if(dir.exists()){
      File[] files = dir.listFiles();
      if(files !=null){
        retFiles = new FileRemote[files.length];
        int iFile = -1;
        for(File fileLocal: files){
          retFiles[++iFile] = newFile(fileLocal);
        }
      }
    }
    return retFiles;
  }

  
  public FileRemote newFile(File fileLocal){
    String name = fileLocal.getName();
    String sDir = fileLocal.getParent().replace('\\', '/');
    FileRemote dir = FileRemote.fromFile(fileLocal.getParentFile());
    FileRemote fileRemote = new FileRemote(this, dir, sDir, name, 0, 0, 0, fileLocal);
    refreshFileProperties(fileRemote, null);  
    return fileRemote;
  }
  
  
  @Override public boolean delete(FileRemote file, FileRemote.CallbackEvent callback){
    File fileLocal = getLocalFile(file);
    if(callback == null){
      return fileLocal.delete();
    } else {
      boolean bOk = fileLocal.delete();
      callback.sendEvent(bOk ? 0: FileRemote.acknErrorDelete);
      return bOk;
    }
  }

  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  
  /**
   * @see org.vishia.util.FileRemoteAccessor#addCommission(org.vishia.util.FileRemote.CallbackEvent, int)
   */
  @Override public void addCommission(FileRemote.CallbackEvent evBack, int cmd){ 
    ////
    if(evBack.hasCallback()){ 
      throw new IllegalArgumentException("The event should only be a single callback event.");
    }
    FileRemote.CmdEvent ev = new FileRemote.CmdEvent(null, executerCommission, singleThreadForCommission, evBack);
    ev.filesrc = evBack.filesrc;
    ev.filedst = evBack.filedst;
    ev.data1 = evBack.data1;
    ev.sendEvent(cmd);
    //singleThreadForCommission.storeEvent(ev);
  }
  
  
  void execCommission(FileRemote.CmdEvent commission){
    switch(commission.cmd()){
    case FileRemote.cmdCheckFile: copy.checkCopy(commission); break;
    case FileRemote.cmdCopy: copy.execCopy(commission); break;
    case FileRemote.cmdMove: copy.execMove(commission); break;
    case FileRemote.cmdChgProps:  execChgProps(commission); break;
    case FileRemote.cmdChgPropsRec:  execChgPropsRecurs(commission); break;
    case FileRemote.cmdCountLength:  execCountLength(commission); break;
    case FileRemote.cmdDel:  execDel(commission); break;
      
    }
  }
  
  
  private void execChgProps(FileRemote.CmdEvent co){
    File dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName !=null && ! co.newName.equals(co.filesrc.getName())){
      dst = new File(co.filesrc.getParent(), co.newName);
      ok &= co.filesrc.renameTo(dst);
    } else {
      dst = co.filesrc;
    }
    ok = chgFile(dst, co, ok);
    int cmd;
    if(ok){
      cmd = FileRemoteAccessor.kFinishOk; 
    } else {
      cmd = FileRemoteAccessor.kFinishNok; 
    }
    FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)co.callbackEvent();
    
    evback.sendEvent(cmd);
  }
  
  
  private void execChgPropsRecurs(FileRemote.CmdEvent co){
    File dst;
    boolean ok;
    if(co.newName !=null && ! co.newName.equals(co.filesrc.getName())){
      dst = new File(co.filesrc.getParent(), co.newName);
      ok = co.filesrc.renameTo(dst);
    } else {
      dst = co.filesrc;
      ok = true;
    }
    ok &= chgPropsRecursive(dst, co, ok, 0);
    int cmd;
    if(ok){
      cmd = FileRemoteAccessor.kFinishOk; 
    } else {
      cmd = FileRemoteAccessor.kFinishNok; 
    }
    FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)co.callbackEvent();
    co.sendEvent(cmd);
  }
  
  
  
  private boolean chgPropsRecursive(File dst, FileRemote.CmdEvent co, boolean ok, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(dst.isDirectory()){
      File[] filesSrc = dst.listFiles();
      for(File fileSrc: filesSrc){
        ok = chgPropsRecursive(fileSrc, co, ok, recursion +1);
      }
    } else {
      ok = chgFile(dst, co, ok);
    }
    return ok;
  }
  

  
  private boolean chgFile(File dst, FileRemote.CmdEvent co, boolean ok){
    if(ok && (co.maskFlags & FileRemote.mCanWrite) !=0){ ok = dst.setWritable((co.newFlags & FileRemote.mCanWrite) !=0, true); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setWritable((co.newFlags & FileRemote.mCanWriteAny) !=0); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setReadable((co.newFlags & FileRemote.mCanWriteAny) !=0, true); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setReadable((co.newFlags & FileRemote.mCanWriteAny) !=0); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setExecutable((co.newFlags & FileRemote.mCanWriteAny) !=0, true); }
    if(ok && (co.maskFlags & FileRemote.mCanWriteAny) !=0){ ok = dst.setExecutable((co.newFlags & FileRemote.mCanWriteAny) !=0); }
    if(ok && co.newDate !=0 && co.newDate !=-1 ){ ok = dst.setLastModified(co.newDate); }
    return ok;
  }
  
  
  
  private void execCountLength(FileRemote.CmdEvent co){
    long length = countLengthDir(co.filesrc, 0, 0);    
    int cmd;
    FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)co.callbackEvent();
    if(length >=0){
      cmd = FileRemoteAccessor.kFinishOk;
      evback.nrofBytesAll = length;
    } else {
      cmd = FileRemoteAccessor.kFinishNok; 
    }
    evback.sendEvent(cmd);
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
  {
    synchronized(this){
      if(commissionState == 'w'){ notify(); }
      commissionState = 'x';
    }
  }
  
  
  EventConsumer executerCommission = new EventConsumer(){
    @Override public boolean processEvent(Event ev) {
      if(ev instanceof FileRemote.CmdEvent){
        execCommission((FileRemote.CmdEvent)ev);
        return true;
      } else {
        return false;
      }
    }
    
  };
  
  
  protected static class Copy
  {
  
    private class DataSetCopy1Recurs{
      FileRemote src;
      FileRemote dst;
      
      /**null if this card describes a file only. The content of src*/
      FileRemote[] listSrc;
      int ixSrc;
      FileInputStream in = null;
      FileOutputStream out = null;
      
    }
    
    private final Event evCpy = new Event();
    
    private FileRemote.CallbackEvent evBack;
    
    
    /**commands to send inside this state machine. */
    private final static int cmdCopyDirFile = 0xc0b70001, cmdCopyFilePart = 0xc0b70002, cmdCopyFile = 0xc0b0003
      , cmdCopyDir = 0xc0b7d13; 
    
    /**Main state class*/
    public enum StateCopy{ Null, Ready, Process };
    
    /**Inner state class inside Process. */
    public enum StateCopyProcess{ Null, CopyDirFile, CopyFileContent, Ask, CopyFileFinished };
    
    /**Main state. */
    StateCopy stateCopy = StateCopy.Null;
    
    /**Inner state inside Process. */
    StateCopyProcess stateCopyProcess = StateCopyProcess.Null;
    
    /**Mode of copy, see {@link FileRemote#modeCopyCreateAsk}, {@link FileRemote#modeCopyReadOnlyAks}, {@link FileRemote#modeCopyExistAsk}. */
    int mode; 
    
    /**True if a skip file was set. */
    boolean bSkip;
    
    long timestart;
    
    //boolean bAbortFile, bAbortDir, bAbortAll;
    
    int zFilesCheck, zFilesCopy;
    
    long zBytesCheck, zBytesCopy;
    
    /**Buffer for copy. It is allocated static. 
     * Only used in this thread {@link FileRemoteAccessorLocalFile#runCommissions}. 
     * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
     * at least the size of 1 record of the file system. */
    byte[] buffer = new byte[0x100000];  //1 MByte 16 kByte buffer
    
    
    /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}
     * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}. */
    private final List<File> listCopyFiles = new LinkedList<File>();
    
    private final List<Event> storedCopyEvents = new LinkedList<Event>();
    
    private final Stack<DataSetCopy1Recurs> recursDirs = new Stack<DataSetCopy1Recurs>();
    
    private DataSetCopy1Recurs actData;
    
    
    private File currentFile;

    private int ctWorkingId = 0;

    private int checkId;
    
    void checkCopy(FileRemote.CmdEvent ev){
      this.currentFile= ev.filesrc;
      this.mode = ev.data1;
      listCopyFiles.clear();
      FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)ev.callbackEvent();
      if(currentFile.isDirectory()){ 
        zBytesCheck = 0;
        zFilesCheck = 0;
        checkDir(currentFile, 1);
      } else {
        listCopyFiles.add(currentFile);
        zBytesCheck = ev.filesrc.length();
        zFilesCheck = 1;
      }
      
      evback.data1 = checkId = ++ctWorkingId;
      evback.nrofBytesAll = (int)zBytesCheck;  //number between 0...1000
      evback.nrofFiles = zFilesCheck;  //number between 0...1000
      evback.sendEvent(FileRemoteAccessor.kNrofFilesAndBytes);
    }
    
    
    
    void checkDir(File dir, int recursion){
      //try{
        File[] files = dir.listFiles();
        for(File file: files){
          if(file.isDirectory()){
            if(recursion < 100){ //prevent loop with itself
              checkDir(file, recursion+1);  //recursively
            }
          } else {
            listCopyFiles.add(file);
            zFilesCheck +=1;
            zBytesCheck += file.length();
          }
        }
      //}catch(IOException exc){
        
      //}
    }

    
    
    private void execMove(FileRemote.CmdEvent co){
      timestart = System.currentTimeMillis();
      int cmd;
      FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)co.callbackEvent();
      if(co.filesrc.renameTo(co.filedst)){
        cmd = FileRemoteAccessor.kFinishOk; 
      } else {
        cmd = FileRemoteAccessor.kFinishNok; 
      }
      evback.sendEvent(cmd);
    }

    
    private static class State{
      boolean entry(FileRemote.CmdEvent ev){ return true; }
      
      boolean trans(FileRemote.CmdEvent ev){ return false; }
        
    }
    
    
      
    boolean entry_CopyDirList(FileRemote.CmdEvent ev){
      ev.consumedRetain();
      if(actData.ixSrc < actData.listSrc.length){
        FileRemote src = actData.listSrc[actData.ixSrc++];
        FileRemote dst = new FileRemote(actData.dst, src.getName());
        
        newDataSetCopy(src, dst);
        entry_CopyDirFile(ev);
      } else {
        actData = recursDirs.pop();
        if(actData.listSrc !=null){
          evCpy.sendEvent(cmdCopyDir);
          return true;
        } else {
          entry_CopyDirFile(ev);
          return true;
        }
      }
      return false; 
    }
    
    
    
    boolean entry_CopyReady(FileRemote.CmdEvent ev){
      stateCopy = StateCopy.Ready;
      return true;
    }
    
    
    boolean trans_CopyReady(FileRemote.CmdEvent ev){
      if(ev.cmd() == FileRemote.cmdCopy){
        newDataSetCopy(ev.filesrc, ev.filedst);
        evBack = (FileRemote.CallbackEvent) ev.callbackEvent();
        entry_CopyProcess(ev);
        entry_CopyDirFile(ev);
        return true;
      }
      else {
        return false;
      }
    }
    
    
    boolean entry_CopyProcess(FileRemote.CmdEvent ev){
      stateCopy = StateCopy.Process;
      stateCopyProcess = StateCopyProcess.Null;
      return true;
    }
    
    
    boolean trans_CopyProcess(FileRemote.CmdEvent ev){
      if(ev.cmd() == FileRemote.cmdCopy){
        storedCopyEvents.add(ev);  //save it, execute later if that cmdCopy is finished.
      }
      if(ev.cmd() == FileRemote.cmdAbortAll){
        return entry_CopyReady(ev);
      } else {
        switch(stateCopyProcess){
        case Null: return entry_CopyDirFile(ev);
        case CopyDirFile: return trans_CopyDirFile(ev); 
        case CopyFileContent: return trans_CopyFileContent(ev); 
        case CopyFileFinished: return trans_CopyFileFinished(ev); 
        case Ask: return trans_CopyAsk(ev);
        default: return false;
        }//switch
      }
    }
    
    
    boolean entry_CopyDirFile(FileRemote.CmdEvent ev){
      if(stateCopy != StateCopy.Process){
        entry_CopyProcess(ev);
      }
      stateCopyProcess = StateCopyProcess.CopyDirFile;
      if(actData.src.isDirectory()){
        //first send a callback
        if(evBack.reserveRecall()){
          evBack.fileName[0] = 'x';
          evBack.sendEvent(FileRemoteAccessor.kOperation);
        }
        //This action may need some time.
        actData.listSrc = actData.src.listFiles();
        actData.ixSrc = 0;
        
        //evCpy.sendEvent(cmdCopyDir);
        return entry_CopyDirList(ev);
      } else {
        //evCpy.sendEvent(cmdCopyFile);
        return entry_CopyFileContent();
      }
      //return true;
    }
    
    
    boolean trans_CopyDirFile(FileRemote.CmdEvent ev){

      if(ev.cmd() == cmdCopyDirFile){
        ev.consumedRetain();
        DataSetCopy1Recurs data = recursDirs.lastElement();
        if(data.src.isDirectory()){
          data.dst.mkdirs();
          data.listSrc = data.src.listFiles();
          data.ixSrc = 0;
          DataSetCopy1Recurs data2 = new DataSetCopy1Recurs();
          data2.src = data.listSrc[data.ixSrc];
          data2.dst = new FileRemote(data.dst, data.src.getName());
          ev.sendEvent(cmdCopyDirFile);
          return false;
        } else {
          try{
          data.in = new FileInputStream(data.src);
          data.out = new FileOutputStream(data.dst);
          ev.sendEvent(cmdCopyFile);
          return entry_CopyFileContent();
          
          } catch(IOException exc){
            Event evback = ev.callbackEvent();
            evback.sendEvent(FileRemote.acknErrorOpen);
            return entry_CopyAsk();
            
          }
        }
      } else if(ev.cmd() == cmdCopyFile){
        if(false ){ //openCreateFile(recursDirs.lastElement(), ev)){
          ev.consumedRetain();
          ev.sendEvent(cmdCopyFilePart);
          return entry_CopyFileContent();
        } else {
          return entry_CopyAsk();
        }
      } else {
        return false;
      }
    }
    
    
    boolean entry_CopyFileStart(FileRemote.CmdEvent ev){
      boolean bOk = true;
      FileRemote fileSrc, fileDst;
      if(actData.listSrc !=null){
        //File from list
        fileSrc = actData.listSrc[actData.ixSrc++];
        fileDst = new FileRemote(actData.dst, fileSrc.getName());
        if(fileSrc.isDirectory()){
          newDataSetCopy(fileSrc, fileDst);
          entry_CopyDirFile(ev);
        }
      }
      else {
        
      }
      //newDataSetCopy(src, dst);
      entry_CopyDirFile(ev);
      try{
        if(ev.reserve()){
          actData.in = new FileInputStream(actData.src);
          actData.out = new FileOutputStream(actData.dst);
          ev.sendEvent(cmdCopyFilePart);
          return entry_CopyFileContent();
        }
      } catch(IOException exc){
        Event evback = ev.callbackEvent();
        evback.sendEvent(FileRemote.acknErrorOpen);
        return entry_CopyAsk();
        
      }
      return bOk;
    }
    
    
    boolean entry_CopyFileContent(){
      stateCopyProcess = StateCopyProcess.CopyFileContent;
      return true;
    }
    
    
    boolean trans_CopyFileContent(FileRemote.CmdEvent ev){
      return false;
    }
    
    
    boolean entry_CopyFileFinished(FileRemote.CmdEvent ev){
      stateCopyProcess = StateCopyProcess.CopyFileFinished;
      return true;
    }
    
    
    boolean trans_CopyFileFinished(FileRemote.CmdEvent ev){
      return false;
    }
    
    
    
    boolean entry_CopyAsk(){
      stateCopyProcess = StateCopyProcess.Ask;
      return true;
    }
    
    
    boolean trans_CopyAsk(FileRemote.CmdEvent ev){

      if(ev.cmd() == 1){
        bSkip = true;
        return entry_CopyFileContent();
      } else if(ev.cmd() == 2){
        bSkip = true;
        return entry_CopyFileContent();
      }
      return false;
     
    }
    
    
    /**Executes copy for 1 file or 1 segment of file. State-controlled. 
     * <pre>
     *                                         ?src.isFile-----~cmdFilePart----------->(CopyFilePart)
     * (Init)---cmdCopy--->(Copy)---cmdDir-----?src.isDirectory]---+
     *                        |<------~cmdDir----------------------+
     *                        |
     *                        |-----cmdFilePart-----
     * </pre>
     * <ul>
     * <li>State {@link #stateCopyReady}: Ready for opeation any copy action.
     *   <ul>
     *   <li>ev ev {@link FileRemote#cmdCopy}: ~ev {@link #cmdCopyDirFile} --> {@link #stateCopyDirFile}
     *     with that: start copying any directory or file.
     *   </ul>
     * <li>State {@link #stateCopyDirFile}: The state where a directory is analyzed to copy.
     *   <ul>
     *   <li>ev {@link FileRemote#cmdCopy}: store in {@link #storedCopyEvents}, --> {@link #stateCopyDirFile}
     *     Therewith new incomming cmdCopy will be stored, not ignored, but not executed yet.
     *   <li>ev {@value #cmdCopyDirFile}:  
     *     check whether it is a dir or file:
     *     <ul>
     *     <li>A dir: create entry in {@link #recursDirs}; get {@link File#listFiles()}; ~{@link #cmdCopyDirFile}
     *         --> {@link #stateCopyDirFile}
     *         <br>It is a
     *     <li>A file: open it, create dst, save in {@link DataSetCopy1Recurs#in} and out.
     *       <ul> 
     *       <li> If exeception on open: callback({@link FileRemote#acknErrorOpen}, --> {@link #stateCopyDirFile}.
     *         It waits for any response. If response did not come, the rest of copy of other files are waiting forever.
     *         The user is informed about this state.
     *       <li> If the open and creation is successfull, ~ {@link #cmdCopyFilePart}, --> {@link #stateCopyFileContent}  
     *       </ul>
     *     </ul>
     *   </ul>
     * <li>State {@link #stateCopyFileContent}
     *   <ul>
     *   <li>ev {@link #cmdCopyFilePart}: copy parts for 300 ms or max for that file.
     *     <ul>
     *     <li>If the file is finished:
     *     <li>If the file is not finished:
     *     </ul>
     *   </ul>  
     * <li>State {@link #stateCopyFileFinished}: Check continue:
     *   <pre>
     *    boolean bCont = false;
          do{
          if(entry){
            if(++entry.isSrc > listSrc.length(){
              remove entry;
              bCont = true;
            } else {
              ~cmdCopyDirFile(listSrc[ixSrc]); 
          } else {
            ~cmdFinish
          } while(bCont);
     *   </pre>
     *      
     * </ul>                       
     * */
    void execCopyState(FileRemote.CmdEvent ev){
      boolean bChg = false;
      do{
        switch(stateCopy){
        case Null:          bChg = entry_CopyReady(ev); break;
        case Ready:         bChg = trans_CopyReady(ev); break;
        case Process:       bChg = trans_CopyProcess(ev); break;
        } 
      } while(bChg);
      
    }
    
    
    
    /**Creates a new entry for the file deepnes stack.
     * @param src
     * @param dst
     */
    void newDataSetCopy(FileRemote src, FileRemote dst){
      if(actData !=null){ recursDirs.push(actData); }
      actData = new DataSetCopy1Recurs();
      actData.src = src;
      actData.dst = dst;
      ///
    }
    
    
    private void execCopy(FileRemote.CmdEvent ev){
      timestart = System.currentTimeMillis();
      zFilesCopy = 0;
      zBytesCopy = 0;
      
      
      if(ev.filesrc.isDirectory()){
        execCopyDir(ev, ev.filesrc, ev.filedst);
      } else {
        execCopyFile(ev, ev.filesrc, ev.filedst);
      }
      FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)ev.callbackEvent();
      if(ev.cmd()== FileRemote.cmdAbortAll){
        evback.sendEvent(FileRemote.acknAbortAll);
      } else {
        //ev.cmd = FileRemoteAccessor.kFinishOk; //zBytesCopyFile == zBytesMax ? FileRemoteAccessor.kFinishOk : FileRemoteAccessor.kFinishNok;
        evback.sendEvent(FileRemoteAccessor.kFinishOk);
      }
    }

    
    private void execCopyDir(FileRemote.CmdEvent co, File src, File dst){
      assert(src.isDirectory());
      FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)co.callbackEvent();
      dst.mkdirs();
      File[] filesSrc = src.listFiles();
      for(File fileSrc: filesSrc){
        if(fileSrc.isDirectory()){
          File dirDst = new File(dst, fileSrc.getName());
          execCopyDir(co, fileSrc, dirDst);
        } else {
          File fileDst = new File(dst, fileSrc.getName());
          execCopyFile(co, fileSrc, fileDst);
        }
        int evCmd = co.cmd();
        if(evCmd == FileRemote.cmdAbortDir || evCmd == FileRemote.cmdAbortAll){
          if(evCmd == FileRemote.cmdAbortDir){
            //FileSystem.rmdir(dst);
            evback.sendEvent(FileRemote.acknAbortDir);
          }
          break;
        }
      }
    }
    
    ///    
    private void execCopyFile(FileRemote.CmdEvent ev, File src, File dst){
      FileInputStream in = null;
      FileOutputStream out = null;
      final long zBytesMax = src.length();
      long zBytesCopyFile = 0;
      int evCmd;
      boolean bAsk, bSkip;
      FileRemote.CallbackEvent evback = (FileRemote.CallbackEvent)ev.callbackEvent();
      try{
        if(dst.exists()){
          if(!dst.canWrite()){
            switch(mode & FileRemote.modeCopyReadOnlyMask){
            case FileRemote.modeCopyReadOnlyNever: bSkip = true; bAsk = false; break;
            case FileRemote.modeCopyReadOnlyOverwrite: bSkip = false; bAsk = false; break;
            case FileRemote.modeCopyReadOnlyAks: bSkip = false; bAsk = true; break;
            default: bSkip = true; bAsk = true;
            } //switch
          }
          switch(mode & FileRemote.modeCopyExistMask){
          case FileRemote.modeCopyExistAll: bSkip = false; bAsk = false; break;
          case FileRemote.modeCopyExistNewer: bSkip = src.lastModified() > dst.lastModified(); bAsk = false; break;
          case FileRemote.modeCopyExistOlder: bSkip = src.lastModified() < dst.lastModified(); bAsk = false; break;
          case FileRemote.modeCopyExistAsk: bSkip = false; bAsk = true; break;
          default: bSkip = true; bAsk = true;
          } //switch
        } else {
          switch(mode & FileRemote.modeCopyCreateMask){
          case FileRemote.modeCopyCreateYes: bSkip = false; bAsk = false; break;
          case FileRemote.modeCopyCreateNever: bSkip = true; bAsk = false; break;
          case FileRemote.modeCopyCreateAsk: bSkip = false; bAsk = true; break;
          default: bSkip = true; bAsk = true;
          } //switch
        }
        if(bAsk){
          
        }
        if(!bSkip){
          in = new FileInputStream(src);
          out = new FileOutputStream(dst);
          boolean bContCopy;
          do {
            int zBytes = in.read(buffer);
            if(zBytes > 0){
              bContCopy = true;
              zBytesCopyFile += zBytes;
              zBytesCopy += zBytes;
              out.write(buffer, 0, zBytes);
              long time = System.currentTimeMillis();
              //
              //feedback of progression after about 0.3 second. 
              if(time > timestart + 300){
                ////
                evback.data1 = (int)((float)zBytesCopyFile / zBytesMax * 1000);  //number between 0...1000
                evback.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
                evback.nrofFiles = zFilesCheck - zFilesCopy;
                evback.nrofBytesInFile = (int)zBytesCopy;
                String name = src.getName();
                int zName = name.length();
                if(zName > evback.fileName.length){ 
                  zName = evback.fileName.length;    //shorten the name, it is only an info 
                }
                System.arraycopy(name.toCharArray(), 0, evback.fileName, 0, zName);
                Arrays.fill(evback.fileName, zName, evback.fileName.length, '\0');
                evback.sendEvent(FileRemoteAccessor.kOperation);
                timestart = time;
              }
            } else if(zBytes == -1){
              bContCopy = false;
              out.close();
            } else {
              //0 bytes ?
              bContCopy = true;
            }
            evCmd = evback.cmd();
          }while(bContCopy && evCmd != FileRemote.cmdAbortFile && evCmd != FileRemote.cmdAbortDir && evCmd != FileRemote.cmdAbortAll);
        } else { //bSkip
          evCmd = 0;
        }
      } catch(IOException exc){
        evCmd = 0;
        System.err.println("Copy exc "+ exc.getMessage());
        evback.data1 = (int)((float)zBytesCopyFile / zBytesMax * 1000);  //number between 0...1000
        evback.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
        evback.nrofFiles = zFilesCheck - zFilesCopy;
        evback.sendEvent(FileRemoteAccessor.kFinishError);
      }
      try{
        if(in !=null) { in.close(); }
        if(out !=null) { out.close(); }
        if(evCmd == FileRemote.cmdAbortFile){
          boolean bOkdel = dst.delete();
          if(bOkdel){
            
          }
          evback.sendEvent(0);
        }
      }catch(IOException exc){}
      try {
        long date = src.lastModified();
        dst.setLastModified(date);
      } catch(Exception exc){
        System.err.println("can't modified date: " + dst.getAbsolutePath());
      }
      zFilesCopy +=1;
    }
  }  

  
  public static FileRemoteAccessorSelector selectLocalFileAlways = new FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileRemoteAccessorLocalFile.getInstance();
    }
  };
  
}
