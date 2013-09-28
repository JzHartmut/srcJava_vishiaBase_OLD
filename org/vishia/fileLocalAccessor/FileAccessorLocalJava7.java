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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;
import org.vishia.fileRemote.FileCluster;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteAccessor;
import org.vishia.fileRemote.FileRemote.CallbackEvent;
import org.vishia.fileRemote.FileRemote.Cmd;
import org.vishia.fileRemote.FileRemote.CmdEvent;
import org.vishia.fileRemote.FileRemoteAccessor.CallbackFile;
import org.vishia.fileRemote.FileRemoteAccessor.CallbackFile.Result;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;

public class FileAccessorLocalJava7 implements FileRemoteAccessor
{
  /**Version, history and license.
   * <ul>  
   * <li>2013-09-21 Hartmut creation: Derived from {@link FileAccessorLocalJava7}
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
  
  
  /**Type of the attributes of files. Set on constructor depending on the operation system.
   * 
   */
  protected final Class<? extends BasicFileAttributes> systemAttribtype;
  
  
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
  EventConsumer executerCommission = new EventConsumer(){
    @Override public int processEvent(Event ev) {
      if(ev instanceof Copy_FileLocalAccJava7.EventCpy){ //internal Event
        copy.stateCopy.processEvent(ev);
        return 1;
      } else if(ev instanceof FileRemote.CmdEvent){  //event from extern
            execCommission((FileRemote.CmdEvent)ev);
        return 1;
      } else {
        return 0;
      }
    }
    @Override public String toString(){ return "FileRemoteAccessorLocal - executerCommision"; }

  };
  

  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileAccessorLocalJava7)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  private final Copy_FileLocalAccJava7 copy = new Copy_FileLocalAccJava7(this);  
  
  private FileRemote workingDir;
  
  public FileAccessorLocalJava7() {
    //singleThreadForCommission.startThread();
    systemAttribtype = DosFileAttributes.class;
  }
  
  
  
  /**Returns the singleton instance of this class.
   * Note: The instance will be created and the thread will be started if this routine was called firstly.
   * @return The singleton instance.
   */
  public static FileRemoteAccessor getInstance(){
    if(instance == null){
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      try{ classLoader.loadClass("java.nio.file.Files");
        instance = new FileAccessorLocalJava7();
      } catch(ClassNotFoundException exc){
        instance = new FileRemoteAccessorLocalFile();  //use fallback strategy
      }
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
  
  
  
  
  protected static void setAttributes(FileRemote fileRemote, Path path, BasicFileAttributes attribs){
    FileTime fileTime = attribs.lastModifiedTime();
    long dateLastModified = fileTime.toMillis();
    long dateCreation = attribs.creationTime().toMillis();
    long dateLastAccess = attribs.lastAccessTime().toMillis();
    long length = attribs.size();
    int flags = FileRemote.mExist | FileRemote.mTested;
    if(attribs.isDirectory()){ flags |= FileRemote.mDirectory; }
    if(attribs.isSymbolicLink()){
      try{
        Path target = Files.readSymbolicLink(path);
        fileRemote.setSymbolicLinkedPath(target.toAbsolutePath().toString());
      }catch(IOException exc){
        System.err.println("FileAccessorLocalJava7 - Problem on SymbolicLinkPath; " + fileRemote.getAbsolutePath());
        fileRemote.setCanonicalAbsPath(fileRemote.getAbsolutePath());
      }
    } else {
      fileRemote.setCanonicalAbsPath(fileRemote.getAbsolutePath());
    }
    int flagMask = FileRemote.mExist | FileRemote.mTested | FileRemote.mDirectory;
    if(attribs instanceof DosFileAttributes){
      DosFileAttributes dosAttribs = (DosFileAttributes)attribs;
      flagMask |= FileRemote.mHidden | FileRemote.mCanWrite| FileRemote.mCanRead; 
      if(dosAttribs.isHidden()){ flags |= FileRemote.mHidden; }
      if(!dosAttribs.isReadOnly()){ flags |= FileRemote.mCanWrite; }
      if(attribs.isRegularFile()){ flags |= FileRemote.mCanRead; }
      //if(dosAttribs.canExecute()){ flags |= FileRemote.mExecute; }
    }
    fileRemote.internalAccess().setFlagBits(flagMask, flags);
    fileRemote.internalAccess().setLengthAndDate(length, dateLastModified, dateCreation, dateLastAccess);
  }
  
  
  

  
  /**Sets the file properties from the local file.
   * checks whether the file exists and set the {@link FileRemote#mTested} flag any time.
   * If the file exists, the properties of the file were set, elsewhere they were set to 0.
   * @see {@link org.vishia.fileRemote.FileRemoteAccessor#refreshFileProperties(org.vishia.fileRemote.FileRemote)}
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
    //a temporary instance for the thread routine.
    RunRefreshWithChildren thread = new RunRefreshWithChildren(fileRemote, callback);
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      if((fileRemote.getFlags() & FileRemote.mThreadIsRunning) ==0) { //check whether another thread is running with this file.
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
    if(children !=null){
      for(File file1: children){
        list.add(file1);
      }
    }
    return list;
  }

  /**Variant of getChildren for non-Java-7. Firstly all children without its properties are gotten
   * from the operation system using {@link java.io.File#list()}. Therefore {@link #refreshFilePropertiesAndChildren(FileRemote, CallbackEvent)}
   * will be called. Then this list is iterated and the file properties are gotten using 
   * {@link #refreshFileProperties(FileRemote, CallbackEvent)}. In any iteration step the file
   * is offered to the application calling {@link FileRemoteAccessor.CallbackFile#offerFile(FileRemote)}.
   * 
   * @see org.vishia.fileRemote.FileRemoteAccessor#walkFileTree(org.vishia.fileRemote.FileRemote, java.io.FileFilter, int, org.vishia.fileRemote.FileRemoteAccessor.CallbackFile)
   */
  public void walkFileTree(FileRemote dir, FileFilter filter, int depth, CallbackFile callback)
  { ///
    callback.start();
    String sPath = dir.getAbsolutePath();
    Path pathdir = Paths.get(sPath);
    FileVisitor<Path> visitor = new WalkFileTreeVisitor(dir.itsCluster, true, callback);
    Set<FileVisitOption> options = new TreeSet<FileVisitOption>();
    try{ 
      Files.walkFileTree(pathdir, options, depth, visitor);  
    } catch(IOException exc){
      
    }
    callback.finished();
  }

  
  public void XwalkFileTree(FileRemote file, FileFilter filter, int depth, CallbackFile callback)
  {
    callback.start();
    walkSubTree(file, filter, depth, callback);
    callback.finished();
  }
    
  public FileRemoteAccessor.CallbackFile.Result walkSubTree(FileRemote file, FileFilter filter, int depth, CallbackFile callback)
  {
    refreshFilePropertiesAndChildren(file, null);
    Map<String, FileRemote> children = file.children();
    FileRemoteAccessor.CallbackFile.Result result = FileRemoteAccessor.CallbackFile.Result.cont;
    if(children !=null){
      result = callback.offerDir(file);
      if(result == FileRemoteAccessor.CallbackFile.Result.cont){ //only walk through subdir if cont
        Iterator<Map.Entry<String, FileRemote>> iter = children.entrySet().iterator();
        while(result == FileRemoteAccessor.CallbackFile.Result.cont && iter.hasNext()) {
          Map.Entry<String, FileRemote> file1 = iter.next();
          FileRemote file2 = file1.getValue();
          refreshFileProperties(file2, null);
          if(file2.isDirectory()){
            if(depth >1){
              result = walkSubTree(file2, filter, depth-1, callback);  
            } else {
              result = callback.offerFile(file2);  //show it as file instead walk through tree
            }
          } else {
            result = callback.offerFile(file2);
          }
        }
      } 
    }
    if(result != FileRemoteAccessor.CallbackFile.Result.terminate){
      //continue with parent. Also if offerDir returns skipSubdir or any file returns skipSiblings.
      result = FileRemoteAccessor.CallbackFile.Result.cont;
    }
    return result;  //maybe terminate
  }

  
  
  @Override public boolean setLastModified(FileRemote file, long time)
  { File ffile = (File)file.oFile();
    if(ffile !=null){ return ffile.setLastModified(time); }
    else return false;
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

  
  
  @Override public boolean createNewFile(FileRemote file, FileRemote.CallbackEvent callback) throws IOException{
    File file1;
    if(file.oFile() == null){
      file.setFileObject(file1 = new File(file.getAbsolutePath()));
    } else {
      file1 = (File) file.oFile();
    }
    return file1.createNewFile();
  }



  
  @Override public boolean mkdir(FileRemote file, boolean subdirs, FileRemote.CallbackEvent evback){
    File file1 = (File)file.oFile();
    if(file1 == null){ 
      file1 = new File(file.getAbsolutePath());
      file.setFileObject(file1);
    }
    if(evback == null){ 
      if(subdirs){ return file1.mkdirs(); }
      else { return file1.mkdir(); }
    } else {
      FileRemote.CmdEvent ev = prepareCmdEvent(evback);
      ev.filesrc = file;
      ev.filedst = null;
      ev.sendEvent(subdirs ? Cmd.mkDirs: Cmd.mkDir);
      return true;
    }
  }

  
  private void mkdir(boolean recursively, FileRemote.CmdEvent ev) {
    boolean bOk = mkdir(ev.filesrc, recursively, null);  //call direct
    FileRemote.CallbackEvent evback = ev.getOpponent();
    if(evback.occupy(evSrc, true)){
      evback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.nok);
    }
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
   * @see org.vishia.fileRemote.FileRemoteAccessor#prepareCmdEvent(org.vishia.fileRemote.FileRemote.CallbackEvent)
   */
  @Override public FileRemote.CmdEvent prepareCmdEvent(Event<?, FileRemote.Cmd>  evBack){
    FileRemote.CmdEvent cmdEvent1;
    if(evBack !=null && (cmdEvent1 = (FileRemote.CmdEvent)evBack.getOpponent()) !=null){
      if(!cmdEvent1.occupy(evSrc, executerCommission, singleThreadForCommission, false)){
        return null;
      }
    } else {
      cmdEvent1 = new FileRemote.CmdEvent(evSrc, executerCommission, singleThreadForCommission, (FileRemote.CallbackEvent)evBack);
    }
    return  cmdEvent1; 
  }
  
  
  void execCommission(FileRemote.CmdEvent commission){
    FileRemote.Cmd cmd = commission.getCmd();
    switch(cmd){
      case check: //copy.checkCopy(commission); break;
      case overwr:
      case abortAll:     //should abort the state machine!
      case abortCopyDir:
      case abortCopyFile:
      case delChecked:
      case moveChecked:
      case copyChecked: 
        copy.stateCopy.processEvent(commission); break;
      case move: copy.execMove(commission); break;
      case chgProps:  execChgProps(commission); break;
      case chgPropsRecurs:  execChgPropsRecurs(commission); break;
      case countLength:  execCountLength(commission); break;
      case delete:  execDel(commission); break;
      case mkDir: mkdir(false, commission); break;
      case mkDirs: mkdir(true, commission); break;
      case getChildren: getChildren(commission); break;

      
    }
  }
  
  
  
  private void getChildren(FileRemote.CmdEvent ev){
    FileRemote.ChildrenEvent evback = ev.getOpponentChildrenEvent();
    walkFileTree(ev.filesrc(), evback.filter, evback.depth, evback.callbackChildren);
  }
  
  
  
  private void execChgProps(FileRemote.CmdEvent co){
    FileRemote dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc().getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(co.filesrc.itsCluster, fileRenamed);
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
      FileRemote fileRenamed = co.filesrc.getParentFile().child(co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = fileRenamed;
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
      default: { bOk = true; }   //TODO only writeable supported yet, do rest
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
    System.err.println("FileRemoteLocal - execDel not implemented yet.");
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
    
    public void run() {///
      String sPath = fileRemote.getAbsolutePath();
      Path pathfile = Paths.get(sPath);
      try{
        BasicFileAttributes attribs = Files.readAttributes(pathfile, systemAttribtype);
        setAttributes(fileRemote, pathfile, attribs);
      }catch(IOException exc){
        fileRemote.internalAccess().clrFlagBit(FileRemote.mExist);
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
    
    public void run(){  ////
      try{
        time = System.currentTimeMillis();
        refreshFileProperties(fileRemote, null);
        File fileLocal = getLocalFile(fileRemote);
        //fileRemote.flags |= FileRemote.mChildrenGotten;
        if(fileLocal.exists()){
          long time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - start listFiles; dt=" + (time1 - time));
          
          File[] files = fileLocal.listFiles();
          time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - ok listFiles; dt=" + (time1 - time));
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
                String name1 = file1.getName();
                FileRemote child = null;   
                if(oldChildren !=null){ child = oldChildren.remove(name1); }
                if(child == null){ 
                  int flags = file1.isDirectory() ? FileRemote.mDirectory : 0;
                  child = fileRemote.internalAccess().newChild(name1, 0, 0,0,0, flags, file1); 
                  //child.refreshProperties(null);    //should show all sub files with its properties, but not files in sub directories.
                } else {
                  if(!child.isTested(time - 1000)){
                    //child.refreshProperties(null);    //should show all sub files with its properties, but not files in sub directories.
                  }
                }
                fileRemote.internalAccess().putNewChild(child);
              }
              //oldChildren contains yet removed files.
              System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - ok refresh; " + files.length + " files; dt=" + (System.currentTimeMillis() - time));
            }
          }
        }
        fileRemote.timeChildren = System.currentTimeMillis();
        if(callback !=null){
          callback.occupy(evSrc, true);
          long time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - callback listFiles; dt=" + (time1 - time));
          callback.sendEvent(FileRemote.CallbackCmd.done);
          time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - finish listFiles; dt=" + (time1 - time));
        }
        fileRemote.internalAccess().clrFlagBit(FileRemote.mThreadIsRunning);
      }
      catch(Exception exc){
        System.err.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - Thread Excpetion;" + exc.getMessage());
      }
    }
  }
    

  
  /**This class is the general FileVisitor for the adaption layer to FileRemote.
   * It will be created on the fly if any request is proceed with the given
   * {@link FileRemoteAccessor.CallbackFile} callback interface of the FileRemote layer.
   * All callback are translated 1:1 between the both interfaces. But an instance of 
   * FileRemote is created or gotten from the {@link FileCluster} and delivered to the
   * FileRemote-Callback. Additional the children are refreshed if all children are walked through.
   * @author hartmut
   *
   */
  protected static class WalkFileTreeVisitor implements FileVisitor<Path>
  {
    
    
    
    /**Data chained from a first parent to deepness of dir tree for each level.
     * This data are created while {@link FileAccessorLocalJava7#walkFileTree(FileRemote, FileFilter, int, CallbackFile)} runs. 
     */
    private class CurrDirChildren{
      /**The directory of the level. */
      FileRemote dir;
      /**parallel structure of all children.
       * This children are set on {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}
       * to the {@link #dir}.
       */
      Map<String,FileRemote> children;
      /**The parent. null on first parent. */
      CurrDirChildren parent;
      
      CurrDirChildren(FileRemote dir, CurrDirChildren parent){
        this.dir = dir; this.parent = parent;
        if(refresh){
          children = new TreeMap<String,FileRemote>();
        }
      }
    }
    
    final FileCluster fileCluster;
    final boolean refresh;
    final FileRemoteAccessor.CallbackFile callback;
    
    private CurrDirChildren curr;
    
    
    public WalkFileTreeVisitor(FileCluster fileCluster, boolean refresh,
        CallbackFile callback)
    {
      this.fileCluster = fileCluster;
      this.refresh = refresh;
      this.callback = callback;
      curr = null;  //starts without parent.
    }

    private FileVisitResult translateResult(FileRemoteAccessor.CallbackFile.Result result){
      FileVisitResult ret;
      switch(result){
        case cont: ret = FileVisitResult.CONTINUE; break;
        case skipSiblings: ret = FileVisitResult.SKIP_SIBLINGS; break;
        case skipSubtree: ret = FileVisitResult.SKIP_SUBTREE; break;
        case terminate: ret = FileVisitResult.TERMINATE; break;
        default: ret = FileVisitResult.TERMINATE;
      }
      return ret;      
    }
    
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException
    {
      Path namepath = dir.getFileName();
      String name = namepath == null ? "/" : namepath.toString();
      CharSequence cPath = FileSystem.normalizePath(dir.toString());
      FileRemote dir1 = fileCluster.getDir(cPath);
      setAttributes(dir1, dir, attrs);
      if(refresh && curr !=null){
        curr.children.put(name, dir1);
      }
      FileRemoteAccessor.CallbackFile.Result result = callback.offerDir(dir1);
      if(result == Result.cont){
        curr = new CurrDirChildren(dir1, curr);
        System.out.println("FileRemoteAccessorLocalJava7 - callback - pre dir; " + curr.dir.getAbsolutePath());
      } else {
        System.out.println("FileRemoteAccessorLocalJava7 - callback - pre dir don't entry; " + curr.dir.getAbsolutePath());
      }
      return translateResult(result);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
        throws IOException
    { FileRemoteAccessor.CallbackFile.Result result = callback.finishedDir(curr.dir);
      if(refresh){  //es fehlen alle, die nicht als file erscheinen weil das dir auf Gegenseite nicht vorhanden ist.
        curr.dir.internalAccess().setChildren(curr.children);  //Replace the map.
        curr.dir.timeChildren = System.currentTimeMillis();
        curr.dir.internalAccess().setChildrenRefreshed();
      }
      System.out.println("FileRemoteAccessorLocalJava7 - callback - post dir; " + curr.dir.getAbsolutePath());
      curr = curr.parent;
      return translateResult(result);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
      String name = file.getFileName().toString();
      System.out.println("FileRemoteAccessorLocalJava7 - callback - file; " + name);
      if(name.equals("SupportBase.rpy"))
        Assert.stop();
      FileRemote fileRemote = curr.dir.child(name);
      setAttributes(fileRemote, file, attrs);
      if(refresh){
        curr.children.put(name, fileRemote);
        curr.dir.internalAccess().setRefreshed();

      }
      FileRemoteAccessor.CallbackFile.Result result = callback.offerFile(fileRemote);
      return translateResult(result);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
        throws IOException
    {
      return FileVisitResult.CONTINUE;
    }
 
  }
  
  
  
  /**Access selector which uses {@link FileAccessorLocalJava7} for any path.
   * It is the standard for normal PC programs.
   * 
   */
  public static FileRemote.FileRemoteAccessorSelector selectLocalFileAlways = new FileRemote.FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileAccessorLocalJava7.getInstance();
    }
  };

  
}
