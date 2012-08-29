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

import org.vishia.util.FileRemote.CallbackCmd;
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
  
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };

  

  
  EventThread singleThreadForCommission = new EventThread("FileAccessor-local");
  
  EventConsumer executerCommission = new EventConsumer("FileRemoteAccessorLocal - executerCommision"){
    @Override public boolean processEvent(Event ev) {
      if(ev instanceof Copy.EventCpy){
        copy.trans_CopyState(ev);
        return true;
      } else if(ev instanceof FileRemote.CmdEvent){
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
  private final Copy copy = new Copy(this);  
  
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
  @Override public void refreshFileProperties(final FileRemote fileRemote, final FileRemote.CallbackEvent callback)
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
          callback.occupy(evSrc, true);
          callback.sendEvent(FileRemote.CallbackCmd.done);
        }
      }
    };
  
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);
      threadObj.start(); //run in an extra thread, the caller doesn't wait.
    }
  
  }  
    

  
  @Override public void refreshFilePropertiesAndChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
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
          callback.occupy(evSrc, true);
          callback.sendEvent(FileRemote.CallbackCmd.done);
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
        callback.relinquish(); //ignore it.
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
      callback.occupy(evSrc, true);
      callback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.errorDelete );
      return bOk;
    }
  }

  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  
  
  
  @Override public FileRemote.CmdEvent prepareCmdEvent(FileRemote.CallbackEvent evBack){
    Event cmdEvent1 = evBack.getOpponent();
    if(cmdEvent1 !=null){
      if(!cmdEvent1.occupy(evSrc, null, executerCommission, singleThreadForCommission, false)){
        return null;
      }
    } else {
      cmdEvent1 = new FileRemote.CmdEvent(evSrc, null, executerCommission, singleThreadForCommission, evBack);
    }
    return (FileRemote.CmdEvent) cmdEvent1; 
  }
  
  
  void execCommission(FileRemote.CmdEvent commission){
    FileRemote.Cmd cmd = commission.getCmd();
    switch(cmd){
      case check: copy.checkCopy(commission); break;
      case abortAll:
      case abortCopyDir:
      case abortCopyFile:
      case copy: 
        copy.trans_CopyState(commission); break;
      case move: copy.execMove(commission); break;
      case chgProps:  execChgProps(commission); break;
      case chgPropsRecurs:  execChgPropsRecurs(commission); break;
      case countLength:  execCountLength(commission); break;
      case delete:  execDel(commission); break;
      
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
  
  
  
  protected static class Copy
  {
  
    /**This data set holds the information about the currently processed directory or file
     * while copying.
     *
     */
    private class DataSetCopy1Recurs{
      FileRemote src;
      FileRemote dst;
      
      /**null if this card describes a file and not a directory. The content of src*/
      FileRemote[] listSrc;
      /**current index in listSrc while in State {@link StateCopy#Process}.*/
      int ixSrc;
      FileInputStream in = null;
      FileOutputStream out = null;
      int zBytesCopyFile;
      
      long zBytesFile;
      
    }
    
    
    
    /**The event type for intern events. One permanent instance of this class will be created. */
    private final class EventCpy extends Event{
      
      /**The simple constructor calls {@link Event#Event(Object, EventConsumer, EventThread)}
       * with the {@link FileRemoteAccessorLocalFile#executerCommission}
       * and the {@link FileRemoteAccessorLocalFile#singleThreadForCommission}
       * because the event should be send to that.
       */
      EventCpy(FileRemoteAccessorLocalFile accessor){
        super(null, null, accessor.executerCommission, accessor.singleThreadForCommission, new EventCpy(accessor,true));
      }
      
      /**Creates a simple event as opponent. */
      EventCpy(FileRemoteAccessorLocalFile accessor, boolean second){
        super(null, null, accessor.executerCommission, accessor.singleThreadForCommission, null);
      }
      
      /**Qualified sendEvent with the correct enum type of command code.
       * @param cmd
       * @return true if success.
       */
      boolean sendEvent(CmdCpyIntern cmd){ return super.sendEvent_(cmd); }
      
      /**Qualified getCmd with the correct enum type of command code.
       * @return The command inside the received event.
       */
      @Override public CmdCpyIntern getCmd(){ return (CmdCpyIntern)super.getCmd(); }
      
      
      @Override public EventCpy getOpponent(){ return (EventCpy)super.getOpponent(); }
    };
    
    /**The only one instance for this event. It is used permanently. That is possible because the event is fired 
     * only once at the same time in this class locally and private.
     */
    final EventCpy evCpy;
    
    /**Stored callback event when the state machine is in state {@link StateCopy#Process}. 
     * 
     */
    private FileRemote.CallbackEvent evBackInfo;
    
    
    /**commands to send inside this state machine. */
    //private final static int cmdCopyDirFile = 0xc0b70001, cmdCopyFilePart = 0xc0b70002, cmdCopyFile = 0xc0b0003
    //  , cmdCopyDir = 0xc0b7d13, cmdCopyAsk = 0xc0b70a58; 
    
    
    private enum CmdCpyIntern { //Event.Cmd {
      free, reserve,
      
      /**Start a copy process. Event cmd for transition from {@link StateCopy#Start} to {@link StateCopyProcess#DirOrFile} */
      start, 
      
      /**A new {@link DataSetCopy1Recurs} instance was created in {@link Copy#actData}. It should be checked
       * whether it is a directory or file. Event cmd for transition to {@link StateCopyProcess#DirOrFile}. */
      dirFile,
      filePart, 
      file,
      dir, 
      ask
      
    }

    /**Local helper class (only instantiated local) to check which type of event.
     */
    private static class PrepareEventCmd{
      final FileRemote.Cmd cmde;
      final CmdCpyIntern cmdi;
      final FileRemote.CmdEvent eve;
      final EventCpy evi;
      
      PrepareEventCmd(Event evP){
        if(evP instanceof FileRemote.CmdEvent){
          eve = (FileRemote.CmdEvent) evP;
          cmde = eve.getCmd();
          evi = null;
          cmdi = CmdCpyIntern.free;
        } else if(evP instanceof EventCpy){
          eve = null;
          cmde = FileRemote.Cmd.free;
          evi = (EventCpy) evP;
          cmdi = evi.getCmd();
        } else {
          eve = null;
          cmde = FileRemote.Cmd.free;
          evi = null;
          cmdi = CmdCpyIntern.free;
        }
      }
    }

    
    /**Main state class*/
    public enum StateCopy{ Null, Ready, Start, Process };
    
    /**Inner state class inside Process. */
    public enum StateCopyProcess{ Null, DirOrFile, Subdir, FileContent, Ask, FileFinished };
    
    final FileRemoteAccessorLocalFile outer;
    
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
    
    
    Copy(FileRemoteAccessorLocalFile accessor){
      this.outer = accessor;
      evCpy = new EventCpy(accessor);
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
    

    
    /**Cmd check
     * @param ev
     */
    void checkCopy(FileRemote.CmdEvent ev){
      this.currentFile= ev.filesrc;
      this.mode = ev.data1;
      listCopyFiles.clear();
      FileRemote.CallbackEvent evback = ev.getOpponent();
      if(currentFile.isDirectory()){ 
        zBytesCheck = 0;
        zFilesCheck = 0;
        checkDir(currentFile, 1);
      } else {
        listCopyFiles.add(currentFile);
        zBytesCheck = ev.filesrc.length();
        zFilesCheck = 1;
      }
      
      evback.occupy(outer.evSrc, true);
      evback.data1 = checkId = ++ctWorkingId;
      evback.nrofBytesAll = (int)zBytesCheck;  //number between 0...1000
      evback.nrofFiles = zFilesCheck;  //number between 0...1000
      evback.sendEvent(FileRemote.CallbackCmd.doneCheck);
    }
    
    
    
    /**subroutine for {@link #checkCopy(CmdEvent)}
     * @param dir
     * @param recursion
     */
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

    
    
    private void sendEv(CmdCpyIntern cmd){
      final EventCpy ev;
      if(evCpy.occupy(outer.evSrc, false)){
        ev = evCpy;
      } else {
        ev = evCpy.getOpponent();
        ev.occupy(outer.evSrc, true);
      }
      ev.sendEvent(cmd);
    }
    
    
    /**Prepares the callback event for ask anything.
     * @param cmd
     */
    void sendEventAsk(FileRemote.CallbackCmd cmd){
      Assert.check(evBackInfo !=null);
      if(evBackInfo.occupyRecall(1000, outer.evSrc, true)){
        String absPath = actData.src.getAbsolutePath();
        if(absPath.length() > evBackInfo.fileName.length-1){
          absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
        }
        StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
          evBackInfo.sendEvent(cmd);
      } else {
        Assert.checkMsg (false, null);
      }
          
    }
    
    
    
    
    private void execMove(FileRemote.CmdEvent co){
      timestart = System.currentTimeMillis();
      FileRemote.CallbackCmd cmd;
      FileRemote.CallbackEvent evback = co.getOpponent();
      if(co.filesrc.renameTo(co.filedst)){
        cmd = FileRemote.CallbackCmd.done ; 
      } else {
        cmd = FileRemote.CallbackCmd.error ; 
      }
      evback.occupy(outer.evSrc, true);
      evback.sendEvent(cmd);
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
    void trans_CopyState(Event ev){
      boolean bChg = false;
      do{
        System.out.println("FileRemote-Copy;" + (stateCopy == StateCopy.Process ? stateCopyProcess : stateCopy) + ";" + ev.toString());
        switch(stateCopy){
        case Null:          bChg = entry_CopyReady(); break;
        case Ready:         bChg = trans_CopyReady(ev); break;
        case Start:         bChg = trans_CopyStart(ev); break;
        case Process:       bChg = trans_CopyProcess(ev); break;
        } 
      } while(bChg);
      
    }
    
    
    
    
    boolean entry_CopyReady(){
      stateCopy = StateCopy.Ready;
      evBackInfo = null;
      return true;
    }
    
    
    boolean trans_CopyReady(Event evP){
      if(evP instanceof FileRemote.CmdEvent){
        FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
        FileRemote.Cmd cmd = ev.getCmd();
        if(cmd == FileRemote.Cmd.copy){
          //gets and store data from the event:
          newDataSetCopy(ev.filesrc, ev.filedst);
          evBackInfo = ev.getOpponent();
          entry_CopyStart(evCpy);
          return true;
        }
        else {
          return false;
        }
      }
      else {
        return false;
      }
        
    }
    
    
    /**sends the event {@link CmdCpyIntern#start} to enter the {@link StateCopyProcess#DirOrFile} state.
     * @param ev use its opponent to send.
     * @return true
     */
    boolean entry_CopyStart(EventCpy ev){
      stateCopy = StateCopy.Start;
      sendEv(CmdCpyIntern.start); 
      return true;
    }
    
    
    /**The state Start blocks the event cmd {@link FileRemote.Cmd#copy}. The state {@link StateCopy#Process}
     * will be entered with an internal even {@link CmdCpyIntern#start} from this state.
     */
    boolean trans_CopyStart(Event evP){
      if(evP instanceof EventCpy && ((EventCpy)evP).getCmd() == CmdCpyIntern.start){
        entry_CopyProcess(evP);
        entry_CopyDirFile((EventCpy)evP);
        return true;
      }
      else if(evP instanceof FileRemote.CmdEvent && ((FileRemote.CmdEvent)evP).getCmd() == FileRemote.Cmd.abortAll){
        return entry_CopyReady();
      }
      else {
        return false;
      }
        
    }
    
    
    boolean entry_CopyProcess(Event ev){
      stateCopy = StateCopy.Process;
      stateCopyProcess = StateCopyProcess.Null;
      return true;
    }
    
    
    boolean trans_CopyProcess(Event evP){
      PrepareEventCmd ev = new PrepareEventCmd(evP);
      if(ev.cmde == FileRemote.Cmd.copy){
        storedCopyEvents.add(ev.eve);  //save it, execute later if that cmdCopy is finished.
        return false;
      }
      else if(ev.cmde == FileRemote.Cmd.abortAll){
        if(stateCopyProcess == StateCopyProcess.FileContent){
          //close and delete!
        }
        storedCopyEvents.clear();
        return entry_CopyReady();
      } else {
        switch(stateCopyProcess){
        case Null: return entry_CopyDirFile(ev.evi);
        case DirOrFile: return trans_CopyDirFile(ev.evi); 
        case Subdir: return trans_Subdir(ev.evi); 
        case FileContent: return trans_CopyFileContent(ev.evi); 
        case FileFinished: return trans_CopyFileFinished(ev.evi); 
        case Ask: return trans_CopyAsk(ev.eve);
        default: return false;
        }//switch
      }
    }
    
    void exit_CopyProcess(){
    }
    
    /**Prepare copying the given file or directory.
     * {@link #actData} is set with {@link DataSetCopy1Recurs#src} and {@link DataSetCopy1Recurs#dst}.
     * The {@link DataSetCopy1Recurs#listSrc} is not prepared yet.
     * This state checks whether it is a file or directory. 
     * <ul>
     * <li>If it is a file, the file src and dst files will be opened.
     *   <ul>
     *   <li>If it is succeed, an self-event {@link #cmdCopyFile} was sent and the state {@link StateCopyProcess#FileContent} 
     *     is entered immediately. In this state
     *   <li>If the open fails, a callback is sent and the the state {@link StateCopyProcess#Ask} entered immediately.
     *   </ul>
     * <li>If it is a directory, first a backevent is sent which informs about the directory path.
     *   Then the list of files are gotten. This may spend some time.
     *   If it is ready, the first file of the list is processed. To do that, a new instance of {@link DataSetCopy1Recurs}
     *   is created and stored as {@link #actData}.
     *   An event {@link #cmdCopyDirFile} is sent to this itself and the state remains. It is the recursion in the directory.
     *   <br>
     *   It means that the program flow returns to the event queue (inversion of control). Therefore a abort or skip event
     *   have a chance to processing.
     * <li>      
     * @param ev
     * @return
     */
    boolean entry_CopyDirFile(EventCpy ev){
      if(stateCopy != StateCopy.Process){
        entry_CopyProcess(ev);
      }
      stateCopyProcess = StateCopyProcess.DirOrFile;
      return true;
    }
    
    
    /**This state processes a new {@link DataSetCopy1Recurs} stored in {@link #actData}.
     * It branches to the necessary next state:
     * <ul>
     * <li>
     * </ul>
     * @param ev
     * @return
     */
    boolean trans_CopyDirFile(EventCpy ev){
      //EventCpy ev = evP;
      if(actData.src.isDirectory()){
        return entry_Subdir(ev);
      } else {
        //It is a file. try to open/create
        //
        try{
          actData.zBytesFile = actData.src.length();
          actData.in = new FileInputStream(actData.src);
          actData.zBytesCopyFile = 0;
          actData.out = new FileOutputStream(actData.dst);
          return entry_CopyFileContent();
        } catch(IOException exc){
          String absPath = actData.src.getAbsolutePath();
          if(absPath.length() > evBackInfo.fileName.length-1){
            absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
          }
          StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
          sendEventAsk(FileRemote.CallbackCmd.askErrorDstCreate );
          return entry_CopyAsk();
        }
      }
    }
    
    boolean entry_Subdir(EventCpy ev){
      stateCopyProcess = StateCopyProcess.Subdir;
      //onentry action
      //first send a callback
      if(evBackInfo.occupyRecall(outer.evSrc, false)){
        String absPath = actData.src.getAbsolutePath();
        if(absPath.length() > evBackInfo.fileName.length-1){
          absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
        }
        StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
        evBackInfo.sendEvent(FileRemote.CallbackCmd.copyDir);
      }
      //This action may need some time.
      actData.listSrc = actData.src.listFiles();
      actData.ixSrc = 0;
      FileRemote srcInDir = actData.listSrc[0];
      FileRemote dstDir = actData.dst;
      //
      //use the first entry of the dir:
      //
      actData.dst.mkdirs();
      recursDirs.push(actData);
      actData = new DataSetCopy1Recurs();
      actData.src = srcInDir;
      actData.dst = new FileRemote(dstDir, srcInDir.getName());
      sendEv(CmdCpyIntern.dirFile); 
      return true;  //return to the queue
    }
    
    
    boolean trans_Subdir(EventCpy ev){
      if(ev.getCmd() == CmdCpyIntern.dirFile){
        return entry_CopyDirFile(ev);  //exit and entry in the same state.
      /*
      } else if(ev.getCmd() == CmdCpyIntern.file){
        timestart = System.currentTimeMillis();
        return entry_CopyFileContent();  //state switch, process them on entry action.
      */
      } else {
        return false;
      }
    }
    
    
    boolean entry_CopyFileContent(){
      stateCopyProcess = StateCopyProcess.FileContent;
      sendEv(CmdCpyIntern.file); 
      return true;
    }
    
    
    
    /**Executes copying the file or a part of the file for 300 ms or abort copying the file.
     * <uL>
     * <li>If the event cmd {@link #cmdCopyFile} is received, the file is copied or a part of the file is copied
     *   till approximately 300 ms are exhausted. 
     *   <ul>
     *   <li>If the whole file is not processed  an event {@link #cmdCopyfile} is sent to itself
     *     to continue the copying in a new cycle of the state machine.   
     *   <li>If the file is processed, it will be closed and the state {@link #entry_CopyFileFinished(CmdEvent)}
     *     will be entered without condition.
     *   </ul>
     * <li>If the event cmd {@link FileRemote#cmdAbortFile} is gotten, the dst file will be closed and deleted.
     *   Then the {@link #entry_CopyFileFinished(CmdEvent)} will be entered unconditionally.
     * </ul>
     * @param ev
     * @return
     */
    boolean trans_CopyFileContent(Event evP){
      PrepareEventCmd ev = new PrepareEventCmd(evP);
      if(ev.cmdi == CmdCpyIntern.file){
        boolean bContCopy;
        boolean bNewState;
        do {
          try{
            int zBytes = actData.in.read(buffer);
            if(zBytes > 0){
              actData.zBytesCopyFile += zBytes;
              zBytesCopy += zBytes;
              actData.out.write(buffer, 0, zBytes);
              long time = System.currentTimeMillis();
              //
              //feedback of progression after about 0.3 second. 
              if(time > timestart + 300){
                ////
                if(evBackInfo.occupyRecall(outer.evSrc, false)){
                    
                  evBackInfo.data1 = (int)((float)actData.zBytesCopyFile / actData.zBytesFile * 1000);  //number between 0...1000
                  evBackInfo.data2 = (int)((float)zBytesCopy / zBytesCheck * 1000);  //number between 0...1000
                  evBackInfo.nrofFiles = zFilesCheck - zFilesCopy;
                  evBackInfo.nrofBytesInFile = (int)zBytesCopy;
                  String absPath = actData.src.getAbsolutePath();
                  if(absPath.length() > evBackInfo.fileName.length-1){
                    absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
                  }
                  StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
                  evBackInfo.sendEvent(FileRemote.CallbackCmd.nrofFilesAndBytes );
                }
                timestart = time;
                sendEv(CmdCpyIntern.file);
                bContCopy = false;
                bNewState = false;
              } else {
                bContCopy = true;
                bNewState = false;
              }
            } else if(zBytes == -1){
              bContCopy = false;
              actData.out.close();
              bNewState = entry_CopyFileFinished(ev.evi);
              
            } else {
              //0 bytes ?
              bContCopy = true;
              bNewState = false;
            }
          } catch(IOException exc){
            bContCopy = false;
            sendEventAsk(FileRemote.CallbackCmd.askErrorCopy );
            bNewState = entry_CopyAsk();
          }
        }while(bContCopy);

        
        return bNewState;
        
      } else if(ev.cmde == FileRemote.Cmd.abortCopyFile ){
        try{
          actData.out.close();
          actData.out = null;
          if(!actData.dst.delete()) {
            System.err.println("FileRemoteAccessorLocalFile - Problem delete after abort; " + actData.dst.getAbsolutePath());
          }
        } catch(IOException exc){
          System.err.println("FileRemoteAccessorLocalFile - Problem close; " + actData.dst.getAbsolutePath());
        }
        
        return entry_CopyFileFinished(ev.evi);
      } else {
        return false;
      }
    }
    
    
    boolean entry_CopyFileFinished(EventCpy ev){
      stateCopyProcess = StateCopyProcess.FileFinished;
      if(recursDirs.empty()) {
        actData = null;
      } else {
        actData = recursDirs.pop();
      }
      return true;  //Note: executes trans_CopyFileFinished
    }
    
    
    boolean trans_CopyFileFinished(EventCpy ev){
      if(actData == null){
        if(evBackInfo.occupyRecall(1000, outer.evSrc, false)){
          evBackInfo.sendEvent(FileRemote.CallbackCmd.done);
          
        }
        return entry_CopyReady();
      }
      else if(   actData.listSrc !=null) { //copy a directory tree?
        if(++actData.ixSrc < actData.listSrc.length){
          FileRemote src = actData.listSrc[actData.ixSrc];
          FileRemote dst = new FileRemote(actData.dst, src.getName());
          newDataSetCopy(src, dst);
          return entry_CopyDirFile(ev);
        } else {
          return entry_CopyFileFinished(ev);
        }
      } else {
        return entry_CopyFileFinished(ev);
      }
      /*
        if(recursDirs.empty()) {
        } else {
          actData = recursDirs.pop();
          if(actData.listSrc !=null){
            ev.getOpponent().sendEvent(CmdCpyIntern.dir);
            return true;
          } else {
            entry_CopyDirFile(ev);
            return true;
          }
        }
      }
      */
    }
    
    
    
    boolean entry_CopyAsk(){
      stateCopyProcess = StateCopyProcess.Ask;
      return true;
    }
    
    
    boolean trans_CopyAsk(FileRemote.CmdEvent ev){
      FileRemote.Cmd cmd = ev.getCmd();
      if(cmd == FileRemote.Cmd.abortCopyFile){
        bSkip = true;
        return entry_CopyFileFinished(null);
      } else if(cmd == FileRemote.Cmd.abortCopyDir){
        bSkip = true;
        return entry_CopyFileContent();
      }
      return false;
     
    }
    
    
    
/*    
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
        evback.sendEvent(FileRemote.CallbackCmd.acknAbortAll);
      } else {
        //ev.cmd = FileRemoteAccessor.kFinishOk; //zBytesCopyFile == zBytesMax ? FileRemoteAccessor.kFinishOk : FileRemoteAccessor.kFinishNok;
        evback.sendEvent(FileRemote.CallbackCmd.done);
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
            evback.sendEvent(FileRemote.CallbackCmd.acknAbortDir);
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
                evback.sendEvent(FileRemote.CallbackCmd.nrofFilesAndBytes);
                timestart = time;
              }
            } else if(zBytes == -1){
              bContCopy = false;
              out.close();
            } else {
              //0 bytes ?
              bContCopy = true;
            }
            evCmd = 0; //evback.cmd();
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
        evback.sendEvent(FileRemote.CallbackCmd.error);
      }
      try{
        if(in !=null) { in.close(); }
        if(out !=null) { out.close(); }
        if(evCmd == FileRemote.cmdAbortFile){
          boolean bOkdel = dst.delete();
          if(bOkdel){
            
          }
          evback.sendEvent(FileRemote.CallbackCmd.acknAbortFile );
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
    */
  }  
  
  public static FileRemoteAccessorSelector selectLocalFileAlways = new FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileRemoteAccessorLocalFile.getInstance();
    }
  };
  
}
