package org.vishia.fileRemote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EventObject;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.event.EventConsumer;
import org.vishia.event.EventMsg2;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;
import org.vishia.states.StateComposite;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple;
import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;

/**This class contains a state machine as core functionality for all functionalities which may need user-interaction.
 * That is especially for copy files: During the copying the source files may be locked, no more existing etc. 
 * The destination may be read only etc. In that cases a user communication is necessary (should overwrite the read only file? etc).
 * It means the process need a longer time of the user interaction, it remains in any state till the user has responded.
 * A second problem is: Access to remote files for example in network need a longer time, they should be able to abort from the user.
 *   
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteStateM implements EventConsumer
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-01-04 Hartmut chg: The new state execution is used. 
   *   The source is moved from the file org.vishia.fileLocalAccessor.Copy_FileLocalAccJava7 to this location 
   *   because it is independent of the File system. Both sources are compare-able, the functionality is the same.
   * <li>2013-07-29 Hartmut chg: {@link #execMove(org.vishia.fileRemote.FileRemote.CmdEvent) now have a parameter with file names. 
   * <li>2013-04-21 Hartmut redesign check, copy
   * <li>2013-04-00 Hartmut created, extra class dissolved from {@link FileAccessorLocalJava7}
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
  public static final String version = "2015-01-04";

  
  /**Some internal commands for the {@link EventCpy}
   */
  private enum CmdIntern { //Event.Cmd {
    //free, reserve,
    
    /**Start a copy process. Event cmd for transition from {@link StateCopy#Start} to {@link StateCopyProcess#DirOrFile} */
    start, 
    
    /**A new {@link DataSetCopy1Recurs} instance was created in {@link Copy#actData}. It should be checked
     * whether it is a directory or file. Event cmd for transition to {@link StateCopyProcess#DirOrFile}. */
    //dirFile,
    //openSubDir,
    subDir, 
    copyFileContent,
    dir, 
    ask,
    /**Sent if a subdir is found but it is empty. */
    emptyDir,
    /**Continue the check action. */
    check
    
  }

  
  /**The event type for intern events. One permanent instance of this class will be created. 
   * The opponent will be used alternately because 2 instances may need contemporary. */
  public final class EventCpy extends EventMsg2<CmdIntern, CmdIntern>{
    
    private static final long serialVersionUID = 2904627756828656797L;
  
  
    /**The simple constructor calls {@link EventMsg2#Event(Object, EventConsumer, EventThread)}
     * with the {@link FileAccessorLocalJava7#executerCommission}
     * and the {@link FileAccessorLocalJava7#singleThreadForCommission}
     * because the event should be send to that.
     */
    EventCpy(EventConsumer dst){
      super(null, dst, stateThread, new EventCpy(dst, true));
    }
    
    /**Creates a simple event as opponent. */
    EventCpy(EventConsumer dst, boolean second){
      super(null, dst, stateThread, null);
    }
    
    
    @Override public EventCpy getOpponent(){ return (EventCpy)super.getOpponent(); }
  }



  /**This data set holds the information about the currently processed directory or file
   * while copying.
   *
   */
  private static class DataSetCopy1Recurs{
    
    /**The source and destination file or directory. */
    FileRemote src, dst;
    
    /**True if inside at least one file was selected. */
    boolean selectAnyFile;

    final DataSetCopy1Recurs parent;
    
    /**List of entities which can describe a file to copy with {@link #src} and {@link #dst}
     * or which can describe a directory with its {@link #src} and {@link #dst} file object
     * and the list of entities as its {@link #listChildren}. 
     */
    Queue<DataSetCopy1Recurs> listChildren = null; 
    
    DataSetCopy1Recurs(DataSetCopy1Recurs parent){
      this.parent = parent;
    }
    
    DataSetCopy1Recurs addNewEntry(FileRemote src, FileRemote dst){
      DataSetCopy1Recurs act = new DataSetCopy1Recurs(this);
      act.src = src;
      act.dst = dst;
      if(listChildren == null){ listChildren = new ConcurrentLinkedQueue<DataSetCopy1Recurs>();}
      this.listChildren.add(act);
      return act;
    }
    
    @Override public String toString(){return (src !=null ? src.toString() : "NoSrc") + (listChildren !=null ?  ", listDirs.size=" + listChildren.size() : ""); }
  }

  
  /**Source of the eventMsg. */
  EventSource evSrc = new EventSource("FileRemoteCopy"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };
  
  /**Some bits as result of actions. */
  static final int mAsk = 0x10000, mNextFile = 0x20000, mCopyContent = 0x40000;
  
  
  /**The thread which executes the state machine. */
  EventThread stateThread = new EventThread("FileRemoteCopy");
  

  
  /**The only one instance for this event. It is allocated permanently. That is possible because the event is fired 
   * only once at the same time in this class locally and private.
   * The event and its oposite are used one after another. That is because the event is relinguished after it is need twice.
   */
  final EventCpy evCpy;
  


  /**Mode of copy, see {@link FileRemote#modeCopyCreateAsk}, {@link FileRemote#modeCopyReadOnlyAks}, {@link FileRemote#modeCopyExistAsk}. */
  int modeCopyOper; 
  
  /**The currently processed command. */
  FileRemote.Cmd cmdFile;
  
  /**Set one time after receive event to skip in {@link States.Process.Ask}. 
   * Reset if used in the next state transition of the following {@link States.Process.CopyFile} or {@link States.Process.NextFile}. */
  boolean bAbortDirectory;
  
  /**Set one time after receive event in {@link States.Process.Ask} to overwrite the one enquired file. 
   * Reset if used in the next state transition of the following {@link States.Process.CopyFile}, {@link #startCopyFile()}. */
  boolean bOverwrfile;
  
  /**Stores the start time for an action, the length of one action is delimited to {@link #timeActionMax} milliseconds. */
  long timestart;
  
  /**The maximal time for one non-interrupted copy action. */
  long timeActionMax = 300;
  
  //boolean bAbortFile, bAbortDir, bAbortAll;
  
  /**Opened file for read to copy or null. */
  FileInputStream in = null;
  
  /**Opened file for write to copy or null. */
  FileOutputStream out = null;
  
  /**The number of bytes which are copied yet in a copy process to calculate how many percent is copied. */
  int zBytesFileCopied;
  
  /**The number of bytes of a copying file to calculate how many percent is copied. */
  private long zBytesFile;
  
  
  private int zFilesCheck, zFilesCopied;
  
  private long zBytesAllCheck, zBytesAllCopied;
  
  /**Buffer for copy. It is allocated static. 
   * Only used in the {@link States.Process.CopyFileContent}. 
   * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
   * at least the size of 1 record of the file system. */
  byte[] buffer = new byte[0x100000];  //static Buffer for one copy action.
  
  
  /**Signal that one file has successfully copied and should be closed. */
  boolean bCopyFinished;
  
  /**Used to generate the order idents. */
  private long checkOrderCounter;
  
  /**Container of all check commands. */
  private final Map<Long, FileRemoteCopyOrder> checkedOrders = new TreeMap<Long, FileRemoteCopyOrder>();
  
  /**More as one order to check and copy is executed after {@link StateCheckProcess} or {@link StateCopyProcess}
   * is leaved. The event is kept for future execution in the state {@link StateCopyReady}
   */
  final ConcurrentLinkedQueue<EventMsg2<FileRemote.Cmd, FileRemote.CallbackCmd>> storedCopyEvents = new ConcurrentLinkedQueue<EventMsg2<FileRemote.Cmd, FileRemote.CallbackCmd>>();
  
  /**The actual handled file set with source file, destination file, list files
   *  while running any state or switching between states while check or copy.
   *  The actData have a parent if it is not the root. It may have children.
   */
  DataSetCopy1Recurs actData;
  
  
  FileRemote fileSrc;
  
  boolean bOnlyOneFile;
  
  /**The destination directory for all members in {@link #checkedFiles}.*/
  FileRemote dirDst;
  
  /**Stored callback event when the state machine is in state {@link EStateCopy#Process}. 
   * 
   */
  FileRemote.CallbackEvent evBackInfo;
  
  public final States statesCopy;
  
  public FileRemoteStateM()
  {
    statesCopy = new States();
    evCpy = new EventCpy(statesCopy);
  }
  
  @Override public String getStateInfo() {
    return statesCopy.toString();
  }
  
  
  /**Delegates all events to {@link #statesCopy}.
   */
  @Override public int processEvent(EventObject ev){ 
    return statesCopy.processEvent(ev);
  }
  
  
  
  void sendEventInternal(CmdIntern cmd){
    final EventCpy ev;
    if(evCpy.occupy(evSrc, false)){
      ev = evCpy;
    } else {
      ev = evCpy.getOpponent();
      ev.occupy(evSrc, true);
    }
    ev.sendEvent(cmd);
  }
  
  

  /**Prepares the callback event for ask anything.
   * @param cmd
   */
  void sendEventAsk(File pathShow, FileRemote.CallbackCmd cmd){
    Assert.check(evBackInfo !=null);
    if(0 != evBackInfo.occupyRecall(1000, evSrc, true)){
      String absPath = pathShow.getAbsolutePath();
      if(absPath.length() > evBackInfo.fileName.length-1){
        absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
      }
      StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
      evBackInfo.sendEvent(cmd);
    } else {
      Assert.checkMsg (false, null);
    }
        
  }
 
  
  
  
  int execCheckSubdir() {
    int res = 0;
    if(actData.src !=null && evBackInfo.occupyRecall(evSrc, false)){
      String absPath = actData.src.getAbsolutePath();
      if(absPath.length() > evBackInfo.fileName.length-1){
        absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
      }
      StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
      evBackInfo.sendEvent(FileRemote.CallbackCmd.copyDir);
      if(cmdFile == FileRemote.Cmd.copyChecked || cmdFile == FileRemote.Cmd.moveChecked){
        actData.dst.mkdirs();
      }
    }
    //fill child files to handle to the actData.listChildren, independent whether there are files or directories.
    if(actData.src.children() !=null){
      //Copy all children to preserve ConcurrentModificationException because children may moved or deleted.
      //Only that children which are processed already may removed by the statemachine's process itself.
      //Other modifications are able, that files are recognized, but their existence are quest.
      
      //List<FileRemote> children = new ArrayList<FileRemote>(actData.src.children().values());
      for(Map.Entry<String, FileRemote> item: actData.src.children().entrySet()){
        FileRemote child = item.getValue();
      //for(FileRemote child: children){
        if(child.exists() && child.isMarked(1)){
          String sName = child.getName();
          child.resetMarked(1);
          FileRemote childDst = actData.dst == null ? null : actData.dst.child(sName);
          actData.addNewEntry(child, childDst);
        }
      }
    }

    //This action may need some time.
    //-actData.listSrc = (FileRemote[])actData.src.listFiles();
    //-actData.ixSrc = 0;
    if(actData.listChildren == null || actData.listChildren.size() ==0) { //-actData.listSrc.length ==0){
      //an empty directory:
      sendEventInternal(CmdIntern.emptyDir);
    } else {
      //-FileRemote srcInDir = actData.listSrc[0];
      //-FileRemote dstDir = FileRemote.fromFile(actData.dst);
      //
      //use the first entry of the dir:
      //
      //-newDataSetCopy(srcInDir, new FileRemote(dstDir, srcInDir.getName()));
      //-actData.iterChildren = actData.listChildren.iterator();
      //-actData = actData.iterChildren.next();
      actData = actData.listChildren.poll();
      sendEventInternal(CmdIntern.subDir);  //continue with stateCopyDirOrFile
    }
    //return consumed;  //return to the queue

    return res;
  } //execCheckSubdir

  
  
  public void execMove(FileRemote.CmdEvent co){
    timestart = System.currentTimeMillis();
    FileRemote.CallbackCmd cmd;
    FileRemote.CallbackEvent evback = co.getOpponent();
    File filedst;
    boolean bOk = false;      
      
    if(co.namesSrc !=null && co.namesSrc.length() >0){
      String[] afilesSrc = co.namesSrc.split(":");
      for(String sFileSrc : afilesSrc){
        FileRemote fileSrc1 = co.filesrc.child(sFileSrc.trim());
        FileRemote fileDst1 = co.filedst.child(sFileSrc.trim());
        bOk = fileSrc1.renameTo(fileDst1);
        if(!bOk) break;
      }
    } else {
      if(co.filedst.exists()){
        if(co.filedst.isDirectory()){
          filedst = co.filedst.child(co.filesrc.getName());
        } else {
          filedst = null;
        }
      } else {
        filedst = co.filedst;  //non exists, create it.
      }
      if(filedst == null){
        cmd = FileRemote.CallbackCmd.error;  
        System.err.println("FileAccessorLocalJava7 - forbidden move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
      } else {
        bOk =  co.filesrc.renameTo(filedst);
      }
    
    }
    if(bOk){
      cmd = FileRemote.CallbackCmd.done ; 
      System.out.println("FileAccessorLocalJava7 - move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
    } else {
      cmd = FileRemote.CallbackCmd.error ; 
      System.err.println("FileAccessorLocalJava7 - error move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
    }
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd);
  }

  
 
  
  /**Checks the destination, sets {@link #mAsk} or opens both files for copy.
   * @return mResult
   */
  int startCopyFile() {
    int mResult = 0;
    bCopyFinished = false;
    //It is a file. try to open/create
    //
    if(actData.dst.exists()){
      if(bOverwrfile){  //The last event was "overwrite", therefore overwrite only this one file.
        bOverwrfile = false;
        boolean bOk = true;
        if(!actData.dst.canWrite()){
          bOk = actData.dst.setWritable(true);
        }
        if(!bOk){
          sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
          mResult |= mAsk;
        }
      }
      else if((FileRemoteStateM.this.modeCopyOper & FileRemote.modeCopyExistMask) == FileRemote.modeCopyExistSkip){
        //generally don't overwrite existing files:
        mResult |= mNextFile;
      }
      else if(actData.dst.canWrite()){
        //can overwrite, but check timestamp
        long timeSrc = actData.src.lastModified();
        long timeDst = actData.dst.lastModified();
        //The destination file exists and it is writeable. Check:
        switch(FileRemoteStateM.this.modeCopyOper & FileRemote.modeCopyExistMask){
          case FileRemote.modeCopyExistAsk: 
            sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstOverwr );
            mResult |= mAsk;
            break; 
          case FileRemote.modeCopyExistNewer: 
            if( (timeSrc - timeDst) < 0){
              mResult |= mNextFile;
            } 
            break; //else: do copy
          case FileRemote.modeCopyExistOlder: 
            if( (timeSrc - timeDst) > 0){
              mResult |= mNextFile;
            }  //else: do copy
            break;
        }
      } else {  //can not write, readonly
        //The destination file exists and it is readonly. Check:
        switch(FileRemoteStateM.this.modeCopyOper & FileRemote.modeCopyReadOnlyMask){
          case FileRemote.modeCopyReadOnlyAks: 
            sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstReadonly );
            mResult |= mAsk;
            break;
          case FileRemote.modeCopyReadOnlyNever: 
            mResult |= mNextFile;
            break;
          case FileRemote.modeCopyReadOnlyOverwrite: {
            boolean bOk = actData.dst.setWritable(true);
            if(!bOk){
              sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
              mResult |= mAsk;
              }  //else now try to open to overwrite.
          } break;  
        }
        
      }
    }
    if((mResult & mAsk) ==0) {
      FileRemoteStateM.this.zBytesFile = actData.src.length();
      FileRemoteStateM.this.zBytesFileCopied = 0;
      try{ 
        FileSystem.mkDirPath(actData.dst);
        FileRemoteStateM.this.out = new FileOutputStream(actData.dst);
      } catch(IOException exc){
        sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorDstCreate );
        mResult |= mAsk;
      }
  
      try{ FileRemoteStateM.this.in = new FileInputStream(actData.src);
      } catch(IOException exc){
        sendEventAsk(actData.src, FileRemote.CallbackCmd.askErrorSrcOpen );
        mResult |= mAsk;
      }
    }
    if((mResult & mAsk) ==0) {
      sendEventInternal(CmdIntern.copyFileContent);  //Note: An abort event may be stored firstly, it has priority. Therefore don't use run to completion.
    }
    return mResult; 
  }//entry

  
  
  /**Copies the file content. This routine is called as entry in State {@link States.Process.CopyFileContent}.
   * @return Bits for continue or ask.
   */
  int execCopyFileContent()  ////
  { int res = 0;
    //boolean bContCopy;
    boolean bCont = true;
    do {
      try{
        int zBytes = FileRemoteStateM.this.in.read(buffer);
        if(zBytes > 0){
          FileRemoteStateM.this.zBytesFileCopied += zBytes;
          zBytesAllCopied += zBytes;
          FileRemoteStateM.this.out.write(buffer, 0, zBytes);
          //synchronized(this){ try{ wait(50);} catch(InterruptedException exc){} } //only test.
          long time = System.currentTimeMillis();
          //
          //feedback of progression after about 0.3 second. 
          if(time > timestart + timeActionMax){
            timestart = time;
            if(evBackInfo.occupyRecall(evSrc, false)){
                
              evBackInfo.promilleCopiedBytes = (int)(((float)(FileRemoteStateM.this.zBytesFileCopied) / FileRemoteStateM.this.zBytesFile) * 1000);  //number between 0...1000
              if(zBytesAllCheck >0){
                evBackInfo.promilleCopiedFiles = (int)(((float)(zBytesAllCopied) / zBytesAllCheck) * 1000);  //number between 0...1000
              } else {
                evBackInfo.promilleCopiedFiles = 0;
              }
              evBackInfo.nrofFiles = zFilesCheck - zFilesCopied;
              evBackInfo.nrofBytesInFile = zBytesFile - zBytesFileCopied;
              evBackInfo.nrofBytesAll = zBytesAllCheck - zBytesAllCopied;
              String absPath = actData.src.getAbsolutePath();
              if(absPath.length() > evBackInfo.fileName.length-1){
                absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
              }
              StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
              evBackInfo.sendEvent(FileRemote.CallbackCmd.nrofFilesAndBytes );
            }
            sendEventInternal(CmdIntern.copyFileContent); //keep alive the copy process.
            bCont = false;
          } 
        } else if(zBytes == -1){
          //bContCopy = false; ///
          bCopyFinished = true;
          res |= mNextFile;
          bCont = false;
        } else {
          //0 bytes ?
          //bContCopy = true;
          //newState = false;
        }
      } catch(IOException exc){
        //bContCopy = false;
        System.err.printf("FileAccessorLocalJava7 - Copy error; %s\n", exc.getMessage());
        sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorCopy );
        res |= mAsk;
        bCont = false;
      }
    }while(bCont);

    return res;
  }

  
  
  
  void closeCopyFileContent() {  
    try{
      if(this.in!=null){ 
        zFilesCopied +=1;
        FileRemoteStateM.this.in.close(); 
        actData.src.resetMarked(1 | FileMark.mCmpFile);
        actData.src.setDirShouldRefresh();
        actData.dst.setDirShouldRefresh();
        this.in = null;
      }
      if(FileRemoteStateM.this.out!=null){ 
        this.out.close(); 
        this.out = null;
        //delete the closed file. It can be found in actData.dst
        if(actData !=null && actData.dst !=null){
          if(!bCopyFinished){ ///
            if(!actData.dst.delete()) {
              System.err.println("FileAccessorLocalJava7 - Problem delete after abort; " + actData.dst.getAbsolutePath());
            }
          } else {
            long date = actData.src.lastModified();
            actData.dst.setLastModified(date);
          }
        }
      }
    } catch(IOException exc){
      System.err.printf("FileAccessorLocalJava7 - Problem close; %s\n", actData.dst.getAbsolutePath());
    }
  }

  
  
  
  
  private DataSetCopy1Recurs getNextCopyData(){
    //actData = checkedFiles.poll();  //start with the first entry.
    actData = new DataSetCopy1Recurs(null);
    actData.src = fileSrc;
    return actData;
  }
  
  
  void copyAbort(){
    zBytesAllCheck = 0;
    zFilesCheck = 0;
    try{
      storedCopyEvents.clear();
      zBytesAllCheck = zBytesAllCopied = 0;
      zFilesCheck = zFilesCopied = 0;
      try{
        if(out !=null){
          out.close();
          out = null;
        }
        if(in !=null){
          zFilesCopied +=1;
          in.close();
          in = null;
        }
      } catch(IOException exc){
        
      }
    } catch(Exception exc){
      System.err.println("Copy_FileLocalAccJava7.abort() - Exception; " + exc.getMessage());
    }
    actData = null;
  }
  

  
  public final class States  extends StateMachine
  {
    
    public States()
    {
      super("FileRemoteCopy", stateThread, null);
      super.permitException = true;
    }
    
    
    @Override public int eventToTopDebug(EventObject ev) {
      return super.eventToTopDebug(ev);
    }
    
    final class Ready extends StateSimple
    {
      final boolean isDefault = true;
      
      /**Start transition to {@link Process.DirOrFile} */
      Trans start_dirOrFile(EventObject ev, Trans trans) {
        if(trans == null) return new Trans(Process.DirOrFile.class);
        if(!(ev instanceof FileRemote.CmdEvent)) return null;  //don't fire.
        FileRemote.CmdEvent ev1 = (FileRemote.CmdEvent)ev;
        FileRemote.Cmd cmdFile = ev1.getCmd();
        if(cmdFile == FileRemote.Cmd.copyChecked || cmdFile == FileRemote.Cmd.moveChecked) {
          FileRemoteStateM.this.cmdFile = cmdFile;
          trans.retTrans = mEventConsumed;
          trans.doExit();
          //action:
          FileRemoteStateM.this.modeCopyOper = ev1.modeCopyOper;
          dirDst = ev1.filedst();
          actData = getNextCopyData();
          actData.dst = dirDst;
          actData.src = ev1.filesrc();
          //newDataSetCopy(ev.filesrc, ev.filedst);
          evBackInfo = ev1.getOpponent();                 //available for all back information while this copying
          timestart = System.currentTimeMillis();
          zBytesAllCopied = zFilesCopied = 0;
          //entry in DirOrFile.
        }
        return null;
      }
    }
    
    
    final class Process extends StateComposite {
      
      /**Set in any sub state entry if the state {@link Ask} should be activated with run to complete. */
      //boolean bAsk;
      
      /**Set in any sub state entry if the state {@link NextFile} should be activated with run to complete. */
      //boolean bNextFile;
      
      /**Set in any sub state entry if the state {@link Ask} or {@link NextFile} should be activated with run to complete. */
      int mResult;
      
      Trans transAsk = new Trans(Ask.class);
      
      Trans transNextFile = new Trans(NextFile.class);
      
      Trans transAbort = new Trans(States.Ready.class);
      
      
      @Override protected Trans selectTrans(EventObject ev){
        if(ev instanceof FileRemote.CmdEvent && ((FileRemote.CmdEvent)ev).getCmd() == FileRemote.Cmd.abortAll) return transAbort;
        else if((mResult & mAsk) !=0) return transAsk;
        else if((mResult & mNextFile) !=0) return transNextFile;
        else return null;
      }
      
      /*
      class TransAsk extends Trans {
        TransAsk(){ super(2, Ask.class);}
        @Override protected void check(EventObject ev){ if(bAsk){ retTrans |= mTransit;} }
      }

      class TransNextFile extends Trans {
        TransNextFile(){ super(3, NextFile.class);}
        @Override protected void check(EventObject ev){ if(bNextFile){ retTrans |= mTransit;} }
      }

      class TransAbort extends Trans {
        TransAbort(){ super(1, States.Ready.class);}
        @Override protected void check(EventObject ev){ 
          if(ev instanceof FileRemote.CmdEvent && ((FileRemote.CmdEvent)ev).getCmd() == FileRemote.Cmd.abortAll) { 
            retTrans |= mEventConsumed;} 
        }
      }
      */
      
      /**sets {@link #bAsk} and {@link #bNextFile} to false because it is set in the states then.
       */
      @Override protected int entry(EventObject ev) { 
        mResult = 0;
        return 0; 
      } 
      
      @Override protected void exit() {  } //left empty because used on transitions from to inside.
      
      
      class MoveFile extends StateSimple {
        
        boolean bmove = false, bskip = false;
      
        @Override protected int entry(EventObject ev){
          if(actData.src.exists()){  //do nothing if src does not exists
            if(!actData.src.canWrite()){  //can't move it!
              if((modeCopyOper & FileRemote.modeCopyReadOnlyNever)!=0){
                bmove = false;
              } else if((modeCopyOper & FileRemote.modeCopyReadOnlyAks)!=0){
                mResult |= mAsk;
              }
              else if(!actData.src.setWritable(true)){
                mResult |= mAsk;
              } else {
                bmove = true;
              }
            }
            if(bmove && actData.dst.exists()){
              if(!actData.dst.canWrite()){
                //TODO check dst
              } else {
                if(!actData.dst.delete()) {  //delete the destination before move.
                  mResult |= mAsk;
                }
              }
            }
          } else { //not exists
            bskip = true;
          }
          if(bmove){
            if(!actData.src.renameTo(actData.dst)) {
              mResult |= mAsk;
            }
          }
          mResult |= mNextFile;
          return mRunToComplete;
        }//entry
        
        
         
      }

      class DelFile extends StateSimple {
        
        boolean bdelete = false, bskip = false;
        
        @Override protected int entry(EventObject ev){
          if(actData.src.getName().equals("src-html"))
            Debugutil.stop();
          if(actData.src.exists()){  //do nothing if src does not exists
            if(!actData.src.canWrite()){
              if((modeCopyOper & FileRemote.modeCopyReadOnlyNever)!=0){
                bdelete = false;
              } else if((modeCopyOper & FileRemote.modeCopyReadOnlyAks)!=0){
                mResult |= mAsk;
              }
              else if(!actData.src.setWritable(true)){
                mResult |= mAsk;
              } else {
                bdelete = true;
              }
            } else {
              bdelete = true;
            }
          } else { //not exists
            bskip = true;
          }
          if(bdelete){
            if(!actData.src.delete()) {
              mResult |= mAsk;
            }
          }
          return 0;
        } //entry
          
        
        
        
        
      }//class DelFile

      /**Prepare the copy process for the given file or directory.
       * {@link #actData} is set with {@link DataSetCopy1Recurs#src} and {@link DataSetCopy1Recurs#dst}.
       * The {@link DataSetCopy1Recurs#listSrc} is not prepared yet.
       * This state checks whether it is a file or directory. 
       * <ul>
       * <li>If it is a file, the file src and dst files will be opened.
       *   <ul>
       *   <li>If it is succeed, an self-event {@link #cmdCopyFile} was sent and the state {@link EStateCopyProcess#FileContent} 
       *     is entered immediately. In this state
       *   <li>If the open fails, a callback is sent and the the state {@link EStateCopyProcess#Ask} entered immediately.
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
       * This state processes a new {@link DataSetCopy1Recurs} stored in {@link #actData}.
       * It branches to the necessary next state:
       * <ul>
       * <li>(())-----[src.isDirectory()]---->{@link StateSubDir.StateCopySubDir}
       * <li>(())-----/open, create Files --->{@link Copy.StateCopyFileContent}
       *   <ul>
       *   <li>? exception ----->{@link StateAsk.StateCopyAsk}
       *   </ul>                   
       * </ul>
       */
      class DirOrFile extends StateSimple {

        final boolean isDefault = true;

        Trans transCopySubdir(EventObject ev, Trans trans){
          if(trans == null) return new Trans(1, Subdir.class);
          if(actData.src.isDirectory()){
            trans.retTrans = mTransit;
          }
          return null;
        }
        
        Trans transDelFile(EventObject ev, Trans trans){
          if(trans == null) return new Trans(2, DelFile.class);
          if(cmdFile == FileRemote.Cmd.delChecked){
            trans.retTrans = mTransit;
          }
          return null;
        }
        
        Trans transMoveFile(EventObject ev, Trans trans){
          if(trans == null) return new Trans(3, MoveFile.class);
          if(cmdFile == FileRemote.Cmd.move){
            trans.retTrans = mTransit;
          }
          return null;
        }
        
        Trans transStartCopy(EventObject ev, Trans trans){
          if(trans == null) return new Trans(4, CopyFile.class);
          else trans.retTrans = mTransit;   //else transition.
          return null;
        }
        
        
        
      }
      
      class Subdir extends StateSimple {
  
        Trans transSubdir_DirOrFile = new Trans(DirOrFile.class);
        Trans transEmptydir_NextFile = new Trans(NextFile.class);
        
        @Override protected int entry(EventObject ev) { mResult = execCheckSubdir(); return mRunToComplete; }
        
        @Override protected Trans selectTrans(EventObject ev) {
          if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdIntern.subDir){
            return transSubdir_DirOrFile;
          } else if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdIntern.emptyDir){
            return transNextFile;
          } else return null;
        }
        
      }
      
      class CopyFile extends StateSimple {
        
        /**Transition to {@link CopyFileContent}. Note: See {@link Process#transAsk} and {@link Process#transNextFile}. */
        Trans trans_CopyFileContent = new Trans(CopyFileContent.class);
        
        /**Checks the file.
         */
        @Override protected int entry(EventObject ev) { mResult = startCopyFile(); return mRunToComplete; }
          
        @Override protected Trans selectTrans(EventObject ev) {
          if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdIntern.copyFileContent) { 
            return trans_CopyFileContent;
          } else return null;
        }
        
        
      }
      
      
      
      
      
      /**The entry action of this state is used to execute the copy of content of one file.
       * The copying of content is done for one whole file or for at maximum {@link #timeActionMax} milliseconds. Then a short delay is given
       * with the self-generated event {@link FileRemoteStateM.CmdIntern#copyFileContent}.
       * <br><br> 
       * The src and dst files are opened already either from copying content before or from the {@link FileRemoteStateM#startCopyFile()}
       * in the state {@link FileRemoteStateM.States.Process.CopyFile#entry(EventObject)}
       * <br><br>
       * On exit the files are closed, except the self generated event {@link FileRemoteStateM.CmdIntern#copyFileContent} was fired
       * in its own transition.
       */
      class CopyFileContent extends StateSimple {

        
        /**Set on transition with event {@link FileRemoteStateM.CmdIntern#copyFileContent} to prevent close.*/
        boolean bContinue_dontClose;
        
        @Override protected int entry(EventObject ev) { 
          bContinue_dontClose = false;
          mResult = execCopyFileContent(); 
          return mRunToComplete; 
        }
        
        
        @Override protected void exit(){ if(bContinue_dontClose == false) { closeCopyFileContent(); } }
        
        
        Trans transCopyContent_continue = new Trans(CopyFileContent.class);
        
        
        /**Note: {@link #exit()} closes the current file. */
        Trans transAbortFile = new Trans(NextFile.class);

        Trans transAbortCopyDir = new Trans(NextFile.class){
          @Override protected void action(EventObject ev){
            bAbortDirectory = true;
          }
        };

        
        Trans transNextFile = new Trans(NextFile.class);
        
        
        @Override protected Trans selectTrans(EventObject ev) {
          FileRemote.CmdEvent ev1 = null;
          if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdIntern.copyFileContent) {
            bContinue_dontClose = true;
            return transCopyContent_continue; 
          } else if(ev instanceof FileRemote.CmdEvent && (ev1 = ((FileRemote.CmdEvent)ev)).getCmd() == FileRemote.Cmd.abortCopyFile) {  
            modeCopyOper  = ev1.modeCopyOper;
            return transAbortFile;
          } else if(ev instanceof FileRemote.CmdEvent && ev1.getCmd() == FileRemote.Cmd.abortCopyDir) {  
            return transAbortCopyDir;
          } else return null;   //Note: ask and next file are regarded on Process.selectTrans().
        }

      }
      
      
      
      
      
      class NextFile extends StateSimple {
        @Override protected int entry(EventObject ev) {
          boolean bCont;
          //close currently file if this state is entered from stateAsk. The regular close() is executed on exit of stateCopyFile.
          actData.src.resetMarked(1); ///
          if(FileRemoteStateM.this.in !=null){
            zFilesCopied +=1;
            try{ FileRemoteStateM.this.in.close();
            } catch(IOException exc){
              System.err.printf("FileAccessorLocalJava7 -Copy close src while abort, unexpected error; %s\n", exc.getMessage());
            }
            FileRemoteStateM.this.in = null;
          }
          if(FileRemoteStateM.this.out !=null){
            try{ FileRemoteStateM.this.out.close();
            } catch(IOException exc){
              System.err.printf("FileAccessorLocalJava7 -Copy close dst while abort, unexpected error; %s\n", exc.getMessage());
            }
            FileRemoteStateM.this.out = null;
          }
          do{
            if(actData == null || actData.parent ==null) {
              actData = null;
              bCont = false;
            } else {
              DataSetCopy1Recurs nextChild;
              actData = actData.parent;  //back to directory or parent directory.
              if(! bAbortDirectory
                && actData.listChildren !=null //-actData.listSrc !=null //copy a directory tree?
                && (nextChild = actData.listChildren.poll()) !=null //-actData.iterChildren.hasNext() //-++actData.ixSrc < actData.listSrc.length
                ){
                actData = nextChild;
                //-FileRemote src = actData.listSrc[actData.ixSrc];
                //-FileRemote dst = new FileRemote(FileRemote.fromFile(actData.dst), src.getName());
                //-newDataSetCopy(src, dst);
                //-actData = actData.iterChildren.next();
                //sendEv(CmdCpyIntern.dirFile);
                bCont = false;
              } else {
                bCont = true;  //get parent till another actData is found
                if(actData.src !=null){
                  actData.src.resetMarked(1);  //It is processed.
                }
              }
            }
            bAbortDirectory = false;  //don't abort its parent.
          }while(bCont);
          //return isConsumed | StateSimpleBase.mRunToComplete;
          if(actData == null){
            //actData = getNextCopyData();  //maybe more, maybe null
          }
          return mRunToComplete;
        }//entry
        

        Trans transReady(EventObject ev, Trans trans){ 
          if(trans == null) return new Trans(1, States.Ready.class);
          if(actData == null){
            trans.retTrans = mTransit;
            //send done Back
            if(0 != evBackInfo.occupyRecall(1000, evSrc, false)){
              evBackInfo.sendEvent(FileRemote.CallbackCmd.done);
              EventMsg2<?,?> ev1;
              while( (ev1 = storedCopyEvents.poll() ) !=null) {
                ev1.sendEvent();
              }
              
            }
          } 
          return null;
        }

        /**If another file or directory is found. else-Transition.
         */
        Trans transDirOrFile(EventObject ev, Trans trans){ 
          if(trans == null) return new Trans(2, DirOrFile.class);
          trans.retTrans = mTransit;
          return null; 
        }

      }//class NextFile
      
      class Ask extends StateSimple {

        @Override protected int entry(EventObject ev) { 
          closeCopyFileContent();  //it may be there are opened files.
          return 0;
        }
        
        
        
        Trans transDirOrFile(EventObject ev, Trans trans){ 
          if(trans == null) return new Trans(2, DirOrFile.class);
          if(!(ev instanceof FileRemote.CmdEvent)) return null;  //don't fire.
          FileRemote.CmdEvent ev1 = (FileRemote.CmdEvent)ev;
          if(ev1.getCmd() == FileRemote.Cmd.overwr){
            modeCopyOper  = ev1.modeCopyOper;
            bOverwrfile = true;
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
        Trans transAbortFile(EventObject ev, Trans trans){ 
          if(trans == null) return new Trans(2, NextFile.class);
          if(!(ev instanceof FileRemote.CmdEvent)) return null;  //don't fire.
          FileRemote.CmdEvent ev1 = (FileRemote.CmdEvent)ev;
          if(ev1.getCmd() == FileRemote.Cmd.abortCopyFile){
            modeCopyOper  = ev1.modeCopyOper;
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
        Trans transAbortDir(EventObject ev, Trans trans){ 
          if(trans == null) return new Trans(2, NextFile.class);
          if(!(ev instanceof FileRemote.CmdEvent)) return null;  //don't fire.
          FileRemote.CmdEvent ev1 = (FileRemote.CmdEvent)ev;
          if(ev1.getCmd() == FileRemote.Cmd.abortCopyDir){
            modeCopyOper  = ev1.modeCopyOper;
            bAbortDirectory = true;
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
        Trans transAbortAll(EventObject ev, Trans trans){ 
          if(trans == null) return new Trans(2, NextFile.class);
          if(!(ev instanceof FileRemote.CmdEvent)) return null;  //don't fire.
          FileRemote.CmdEvent ev1 = (FileRemote.CmdEvent)ev;
          if(ev1.getCmd() == FileRemote.Cmd.abortAll){
            modeCopyOper  = ev1.modeCopyOper;
            copyAbort();
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
      }
      
      
    }
  }//class States  
}
