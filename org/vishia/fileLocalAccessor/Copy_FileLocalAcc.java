package org.vishia.fileLocalAccessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Assert;
import org.vishia.util.Event;
import org.vishia.util.FileRemote;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;

public class Copy_FileLocalAcc
{

  /**This data set holds the information about the currently processed directory or file
   * while copying.
   *
   */
  private class DataSetCopy1Recurs{
    
    /**The source and destination file or directory. */
    File src, dst;

    /**null if this card describes a file and not a directory. The content of src*/
    File[] listSrc;
    /**current index in listSrc while in State {@link EStateCopy#Process}.*/
    int ixSrc;
    
   
  }
  
  
  
  /**The event type for intern events. One permanent instance of this class will be created. */
  final class EventCpy extends Event{
    
    /**The simple constructor calls {@link Event#Event(Object, EventConsumer, EventThread)}
     * with the {@link FileRemoteAccessorLocalFile#executerCommission}
     * and the {@link FileRemoteAccessorLocalFile#singleThreadForCommission}
     * because the event should be send to that.
     */
    EventCpy(FileRemoteAccessorLocalFile accessor){
      super(null, accessor.executerCommission, accessor.singleThreadForCommission, new EventCpy(accessor,true));
    }
    
    /**Creates a simple event as opponent. */
    EventCpy(FileRemoteAccessorLocalFile accessor, boolean second){
      super(null, accessor.executerCommission, accessor.singleThreadForCommission, null);
    }
    
    /**Qualified sendEvent with the correct enum type of command code.
     * @param cmd
     * @return true if success.
     */
    boolean sendEvent(CmdCpyIntern cmd){ return super.sendEvent(cmd); }
    
    /**Qualified getCmd with the correct enum type of command code.
     * @return The command inside the received event.
     */
    @Override public CmdCpyIntern getCmd(){ return (CmdCpyIntern)super.getCmd(); }
    
    
    @Override public EventCpy getOpponent(){ return (EventCpy)super.getOpponent(); }
  };
  
  /**The only one instance for this event. It is allocated permanently. That is possible because the event is fired 
   * only once at the same time in this class locally and private.
   * The event and its oposite are used one after another. That is because the event is relinguished after it is need twice.
   */
  final EventCpy evCpy;
  
  /**Stored callback event when the state machine is in state {@link EStateCopy#Process}. 
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
    //dirFile,
    //openSubDir,
    subDir, 
    file,
    dir, 
    ask,
    /**Sent if a subdir is found but it is empty. */
    emptyDir
    
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

  
  
  final FileRemoteAccessorLocalFile outer;
  
  /**Main state. */
  //EStateCopy stateCopy = EStateCopy.Null;
  
  /**Inner state inside Process. */
  //EStateCopyProcess stateCopyProcess = EStateCopyProcess.Null;
  
  /**Mode of copy, see {@link FileRemote#modeCopyCreateAsk}, {@link FileRemote#modeCopyReadOnlyAks}, {@link FileRemote#modeCopyExistAsk}. */
  int modeCopyOper; 
  
  /**Set one time after receive event to skip in {@link Copy.StateCopyAsk}. 
   * Reset if used in the next state transition of the following {@link Copy.StateCopyFileContent}. */
  boolean bAbortDirectory;
  
  /**Set one time after receive event to overwrite in {@link Copy.StateCopyAsk}. 
   * Reset if used in the next state transition of the following {@link Copy.StateCopyFileContent}. */
  boolean bOverwrfile;
  
   long timestart;
  
  //boolean bAbortFile, bAbortDir, bAbortAll;
  
  /**Opened file for read to copy or null. */
  FileInputStream in = null;
  
  /**Opened file for write to copy or null. */
  FileOutputStream out = null;
  
  /**The number of bytes which are copied yet in a copy process to calculate how many percent is copied. */
  int zBytesCopyFile;
  
  /**The number of bytes of a copying file to calculate how many percent is copied. */
  long zBytesFile;
  
  
  int zFilesCheck, zFilesCopy;
  
  long zBytesCheck, zBytesCopy;
  
  /**Buffer for copy. It is allocated static. 
   * Only used in this thread {@link FileRemoteAccessorLocalFile#runCommissions}. 
   * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
   * at least the size of 1 record of the file system. */
  byte[] buffer = new byte[0x100000];  //1 MByte 16 kByte buffer
  
  
  boolean bCopyFinished;
  
 /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}
   * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}. */
  private final List<File> listCopyFiles = new LinkedList<File>();
  
  private final ConcurrentLinkedQueue<Event> storedCopyEvents = new ConcurrentLinkedQueue<Event>();
  
  private final Stack<DataSetCopy1Recurs> recursDirs = new Stack<DataSetCopy1Recurs>();
  
  private DataSetCopy1Recurs actData;
  
  
  private File currentFile;

  private int ctWorkingId = 0;

  
  Copy_FileLocalAcc(FileRemoteAccessorLocalFile accessor){
    this.outer = accessor;
    evCpy = new EventCpy(accessor);
  }
  
  /**Creates a new entry for the file deepnes stack.
   * @param src
   * @param dst
   */
  void newDataSetCopy(File src, File dst){
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
    this.modeCopyOper = ev.modeCopyOper;
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
    
    evback.occupy(outer.evSrc, ++ctWorkingId, true);
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
  void sendEventAsk(File pathShow, FileRemote.CallbackCmd cmd){
    Assert.check(evBackInfo !=null);
    if(evBackInfo.occupyRecall(1000, outer.evSrc, true)){
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
  
  
  
  
  void execMove(FileRemote.CmdEvent co){
    timestart = System.currentTimeMillis();
    FileRemote.CallbackCmd cmd;
    FileRemote.CallbackEvent evback = co.getOpponent();
    if(co.filesrc.renameTo(co.filedst)){
      cmd = FileRemote.CallbackCmd.done ; 
    } else {
      cmd = FileRemote.CallbackCmd.error ; 
    }
    System.out.println("FileRemoteAccessorLocalFile - move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
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
  void XXXtrans_CopyState(Event ev){
  }
  
  
  
  
  
  
  
  StateCopyTop stateCopy = new StateCopyTop();
  
  public class StateCopyTop extends StateTopBase<StateCopyTop>{

    StateCopyReady stateReady = new StateCopyReady(this);
    //StateCopyStart start = new StateCopyStart(this);
    StateCopyProcess stateProcess = new StateCopyProcess(this);

    protected StateCopyTop() {
      super("StateCopyTop");
    }

    @Override public int entryDefault(){
      return stateReady.entry(eventNotConsumed);
    }
  
    @Override public int entry(int consumed){
      stateCopy.stateReady.entry(0);
      return consumed;
    }

  }

  
  private class StateCopyReady extends StateSimpleBase<StateCopyTop>{
    
    
    StateCopyReady(StateCopyTop superState) { super(superState, "Ready"); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int trans(Event evP) {
      if(evP instanceof FileRemote.CmdEvent){
        FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
        FileRemote.Cmd cmd = ev.getCmd();
        if(cmd == FileRemote.Cmd.copy){
          //gets and store data from the event:
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          newDataSetCopy(ev.filesrc, ev.filedst);
          evBackInfo = ev.getOpponent();
          StateCopyTop exitState = exit();
          int cont = exitState.stateProcess.entry(mEventConsumed);
          timestart = System.currentTimeMillis();
          return exitState.stateProcess.stateCopyDirOrFile.entry(cont);
        }
        else {
          return eventNotConsumed;
        }
      }
      else {
        return eventNotConsumed;
      }
    }
  }

      
  private class StateCopyProcess extends StateCompositeBase<StateCopyProcess, StateCopyTop>{

    StateCopyDirOrFile stateCopyDirOrFile = new StateCopyDirOrFile(this);
    StateCopySubDir stateCopySubdir = new StateCopySubDir(this);
    StateCopyFileContent stateCopyFileContent = new StateCopyFileContent(this);
    StateCopyFileFinished stateCopyFileFinished = new StateCopyFileFinished(this);
    StateCopyAsk stateCopyAsk = new StateCopyAsk(this);

    protected StateCopyProcess(StateCopyTop superState) { super(superState, "Process"); }

    @Override public int entryDefault(){
      return stateCopyDirOrFile.entry(eventNotConsumed);
    }
  

    @Override public int trans(Event evP){
      PrepareEventCmd ev = new PrepareEventCmd(evP);
      if(ev.cmde == FileRemote.Cmd.copy){
        ev.eve.donotRelinquish();
        storedCopyEvents.add(ev.eve);  //save it, execute later if that cmdCopy is finished.
        return 0;
      }
      else if(ev.cmde == FileRemote.Cmd.abortAll){
        
        storedCopyEvents.clear();
        return exit().stateReady.entry(StateSimpleBase.mEventConsumed);
      } else {
        return 0;
      }
    }

  }

  
  
  /**Prepare copying the given file or directory.
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
   * @param ev
   * @return
   */
  private class StateCopyDirOrFile extends StateSimpleBase<StateCopyProcess>{
    
    
    StateCopyDirOrFile(StateCopyProcess superState) { super(superState, "DirOrFile"); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int entry(int isConsumed){
      return super.entry(isConsumed | StateSimpleBase.mRunToComplete);
    }

    
    /**This state processes a new {@link DataSetCopy1Recurs} stored in {@link #actData}.
     * It branches to the necessary next state:
     * <ul>
     * <li>(())-----[src.isDirectory()]---->{@link Copy.StateCopySubDir}
     * <li>(())-----/open, create Files --->{@link Copy.StateCopyFileContent}
     *   <ul>
     *   <li>? exception ----->{@link Copy.StateCopyAsk}
     *   </ul>                   
     * </ul>
     * @param ev
     * @return
     */
    @Override public int trans(Event ev) {
      //EventCpy ev = evP;
      if(actData.src.isDirectory()){
        return exit().stateCopySubdir.entry(mEventConsumed);  //exit and entry in the same state.
      } else {
        //It is a file. try to open/create
        StateCopyProcess exitState= exit();  //exit this state to tran to another.
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
              return exitState.stateCopyAsk.entry(eventNotConsumed); 
            }
          }
          else if((Copy_FileLocalAcc.this.modeCopyOper & FileRemote.modeCopyExistMask) == FileRemote.modeCopyExistSkip){
            //generally don't overwrite existing files:
            return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
          }
          else if(actData.dst.canWrite()){
            //can overwrite, but check timestamp
            long timeSrc = actData.src.lastModified();
            long timeDst = actData.dst.lastModified();
            //The destination file exists and it is writeable. Check:
            switch(Copy_FileLocalAcc.this.modeCopyOper & FileRemote.modeCopyExistMask){
              case FileRemote.modeCopyExistAsk: 
                sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstOverwr );
                return exitState.stateCopyAsk.entry(eventNotConsumed); 
              case FileRemote.modeCopyExistNewer: 
                if( (timeSrc - timeDst) < 0){
                  return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
                }  //else: do copy
              case FileRemote.modeCopyExistOlder: 
                if( (timeSrc - timeDst) > 0){
                  return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
                }  //else: do copy
            }
          } else {  //can not write, readonly
            //The destination file exists and it is readonly. Check:
            switch(Copy_FileLocalAcc.this.modeCopyOper & FileRemote.modeCopyReadOnlyMask){
              case FileRemote.modeCopyReadOnlyAks: 
                sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstReadonly );
                return exitState.stateCopyAsk.entry(eventNotConsumed); 
              case FileRemote.modeCopyReadOnlyNever: 
                return exitState.stateCopyFileFinished.entry(eventNotConsumed); 
              case FileRemote.modeCopyReadOnlyOverwrite: {
                boolean bOk = actData.dst.setWritable(true);
                if(!bOk){
                  sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
                  return exitState.stateCopyAsk.entry(eventNotConsumed); 
                }  //else now try to open to overwrite.
              } break;  
            }
            
          }
        }
        Copy_FileLocalAcc.this.zBytesFile = actData.src.length();
        Copy_FileLocalAcc.this.zBytesCopyFile = 0;
        try{ 
          FileSystem.mkDirPath(actData.dst);
          Copy_FileLocalAcc.this.out = new FileOutputStream(actData.dst);
        } catch(IOException exc){
          sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorDstCreate );
          return exitState.stateCopyAsk.entry(eventNotConsumed);
        }

        try{ Copy_FileLocalAcc.this.in = new FileInputStream(actData.src);
        } catch(IOException exc){
          sendEventAsk(actData.src, FileRemote.CallbackCmd.askErrorSrcOpen );
          return exitState.stateCopyAsk.entry(eventNotConsumed);
        }
        return exitState.stateCopyFileContent.entry(eventNotConsumed);
      }
    }
  }


  
  private class StateCopySubDir extends StateSimpleBase<StateCopyProcess>{
    
    
    StateCopySubDir(StateCopyProcess superState) { super(superState, "SubDir"); }

    @Override public int entry(int consumed){
      super.entry(consumed);
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
      actData.dst.mkdirs();
      actData.listSrc = actData.src.listFiles();
      actData.ixSrc = 0;
      if(actData.listSrc.length ==0){
        //an empty directory:
        sendEv(CmdCpyIntern.emptyDir);
      } else {
        File srcInDir = actData.listSrc[0];
        FileRemote dstDir = FileRemote.fromFile(actData.dst);
        //
        //use the first entry of the dir:
        //
        recursDirs.push(actData);
        actData = new DataSetCopy1Recurs();
        actData.src = srcInDir;
        actData.dst = new FileRemote(dstDir, srcInDir.getName());
        sendEv(CmdCpyIntern.subDir);
      }
      return consumed;  //return to the queue

    }
    
    /**
     * <ul>
     * <li>(())---{@link CmdCpyIntern#subDir}----->{@link #entry_CopyDirFile(EventCpy)}
     * </ul>
     * @param ev
     * @return
     */
    @Override public int trans(Event ev) {
      if(ev.getCmd() == CmdCpyIntern.subDir){
        return exit().stateCopyDirOrFile.entry(mEventConsumed);  //exit and entry in the same state.
      } else if(ev.getCmd() == CmdCpyIntern.emptyDir){ 
        return exit().stateCopyFileFinished.entry(mEventConsumed);  //exit and entry in the same state.
      } else {
        return eventNotConsumed;
      }
    }
      
  }


  private class StateCopyFileContent extends StateSimpleBase<StateCopyProcess>{
    
    
    StateCopyFileContent(StateCopyProcess superState) { super(superState, "FileContent"); }

    @Override public int entry(int isConsumed){
      sendEv(CmdCpyIntern.file); 
      bCopyFinished = false;
      return super.entry(isConsumed);
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
    @Override public int trans(Event evP) {
      PrepareEventCmd ev = new PrepareEventCmd(evP);
      int newState;   
      if(ev.cmdi == CmdCpyIntern.file){
        //boolean bContCopy;
        newState = -1;   //it set, then the loop should terminated
        do {
          try{
            int zBytes = Copy_FileLocalAcc.this.in.read(buffer);
            if(zBytes > 0){
              Copy_FileLocalAcc.this.zBytesCopyFile += zBytes;
              zBytesCopy += zBytes;
              Copy_FileLocalAcc.this.out.write(buffer, 0, zBytes);
              //synchronized(this){ try{ wait(20);} catch(InterruptedException exc){} } //only test.
              long time = System.currentTimeMillis();
              //
              //feedback of progression after about 0.3 second. 
              if(time > timestart + 300){
                timestart = time;
                if(evBackInfo.occupyRecall(outer.evSrc, false)){
                    
                  evBackInfo.promilleCopiedBytes = (int)(((float)(Copy_FileLocalAcc.this.zBytesCopyFile) / Copy_FileLocalAcc.this.zBytesFile) * 1000);  //number between 0...1000
                  if(zBytesCheck >0){
                    evBackInfo.promilleCopiedFiles = (int)(((float)(zBytesCopy) / zBytesCheck) * 1000);  //number between 0...1000
                  } else {
                    evBackInfo.promilleCopiedFiles = 0;
                  }
                  evBackInfo.nrofFiles = zFilesCheck - zFilesCopy;
                  evBackInfo.nrofBytesInFile = (int)zBytesCopy;
                  String absPath = actData.src.getAbsolutePath();
                  if(absPath.length() > evBackInfo.fileName.length-1){
                    absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
                  }
                  StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
                  evBackInfo.sendEvent(FileRemote.CallbackCmd.nrofFilesAndBytes );
                }
                sendEv(CmdCpyIntern.file); //keep alive the copy process.
                newState = stateCompleted + mEventConsumed;
              } 
            } else if(zBytes == -1){
              //bContCopy = false;
              bCopyFinished = true;
              newState = exit().stateCopyFileFinished.entry(mEventConsumed);
              
            } else {
              //0 bytes ?
              //bContCopy = true;
              //newState = false;
            }
          } catch(IOException exc){
            //bContCopy = false;
            System.err.printf("FileRemoteAccessorLocalFile - Copy error; %s\n", exc.getMessage());
            sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorCopy );
            newState = exit().stateCopyAsk.entry(mEventConsumed);
          }
        }while(newState == -1);
        
      } else if(ev.cmde == FileRemote.Cmd.abortCopyFile ){
        
        newState = exit().stateCopyFileFinished.entry(mEventConsumed);
      } else {
        newState = eventNotConsumed;
      }
      return newState;
    }
    
    @Override public StateCopyProcess exit(){
      StateCopyProcess encl = super.exit();
      try{
        if(Copy_FileLocalAcc.this.in!=null){ Copy_FileLocalAcc.this.in.close(); }
        Copy_FileLocalAcc.this.in = null;
        if(Copy_FileLocalAcc.this.out!=null){ Copy_FileLocalAcc.this.out.close(); }
        Copy_FileLocalAcc.this.out = null;
        if(!bCopyFinished){
          if(!actData.dst.delete()) {
            System.err.println("FileRemoteAccessorLocalFile - Problem delete after abort; " + actData.dst.getAbsolutePath());
          }
        } else {
          long date = actData.src.lastModified();
          actData.dst.setLastModified(date);
        }
      } catch(IOException exc){
        System.err.printf("FileRemoteAccessorLocalFile - Problem close; %s\n", actData.dst.getAbsolutePath());
      }
      
      return encl;
    }
  }

  

  /**This state is activated after the copy process of one file was finished from {@link StateCopyFileContent}
   * or if the file was skipped (from {@link StateCopyDirOrFile}. 
   * <ul>
   * <li>entry: Checks whether there is a further file to copy in the same directory 
   *   or it returns to the parent directory and checks the further files there.
   *   If all files are copied the {@link Copy#actData} are set to null. 
   * <li>trans [actData==null] ==> {@link StateCopyReady}
   * <li>trans [else] ==> {@link StateCopyDirOrFile}
   * <br><br>
   * 
   * @author Hartmut
   *
   */
  private class StateCopyFileFinished extends StateSimpleBase<StateCopyProcess>{
    
    StateCopyFileFinished(StateCopyProcess superState) { super(superState, "FileFinished"); }

    @Override public int entry(int isConsumed){
      super.entry(isConsumed);
      //stateCopyProcess = EStateCopyProcess.FileFinished;
      boolean bCont;
      //close currently file if this state is entered from stateAsk. The regular close() is executed on exit of stateCopyFile.
      if(Copy_FileLocalAcc.this.in !=null){
        try{ Copy_FileLocalAcc.this.in.close();
        } catch(IOException exc){
          System.err.printf("FileRemoteAccessorLocalFile -Copy close src while abort, unexpected error; %s\n", exc.getMessage());
        }
        Copy_FileLocalAcc.this.in = null;
      }
      if(Copy_FileLocalAcc.this.out !=null){
        try{ Copy_FileLocalAcc.this.out.close();
        } catch(IOException exc){
          System.err.printf("FileRemoteAccessorLocalFile -Copy close dst while abort, unexpected error; %s\n", exc.getMessage());
        }
        Copy_FileLocalAcc.this.out = null;
      }
      do{
        if(recursDirs.empty()) {
          actData = null;
          bCont = false;
        } else {
          actData = recursDirs.pop();  //back to directory or parent directory.
          if(! bAbortDirectory
            && actData.listSrc !=null //copy a directory tree?
            && ++actData.ixSrc < actData.listSrc.length
            ){
            File src = actData.listSrc[actData.ixSrc];
            FileRemote dst = new FileRemote(FileRemote.fromFile(actData.dst), src.getName());
            newDataSetCopy(src, dst);
            //sendEv(CmdCpyIntern.dirFile);
            bCont = false;
          } else {
            bCont = true;
          }
        }
        bAbortDirectory = false;  //don't abort its parent.
      }while(bCont);
      return isConsumed | StateSimpleBase.mRunToComplete;
    }
    
    
  
    @Override public int trans(Event evP) {
      //EventCpy ev = (EventCpy)evP;
      /*
      if(ev.getCmd() == CmdCpyIntern.dirFile){
        return exit().dirOrFile.entry(consumed);
      }
      else*/ 
      if(actData == null){
        //send done Back
        if(evBackInfo.occupyRecall(1000, outer.evSrc, false)){
          evBackInfo.sendEvent(FileRemote.CallbackCmd.done);
          ///
          Event ev1;
          while( (ev1 = storedCopyEvents.poll() ) !=null) {
            ev1.sendEventAgain();
          }
          
        }
        return exit().exit().stateReady.entry(eventNotConsumed);
      }
      else {
        //another file or directory
        return exit().stateCopyDirOrFile.entry(eventNotConsumed);
      }
    }
  }


  private class StateCopyAsk extends StateSimpleBase<StateCopyProcess>{
    
    
    StateCopyAsk(StateCopyProcess superState) { super(superState, "Ask"); }

    @Override public int entry(int isConsumed){
      //onyl to set a breakpoint.
      return super.entry(isConsumed);
    }
    
    
    @Override public int trans(Event evP) {
      if(evP ==null){ 
        return 0;
      } else {
        FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
        FileRemote.Cmd cmd = ev.getCmd();
        //
        if(cmd == FileRemote.Cmd.overwr){
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          bOverwrfile = true;
          ///actData.dst.setWritable(true);
          return exit().stateCopyDirOrFile.entry(mEventConsumed);
        } 
        else if(cmd == FileRemote.Cmd.abortCopyFile){   //it is the skip file.
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          return exit().stateCopyFileFinished.entry(mEventConsumed);
        } 
        else if(cmd == FileRemote.Cmd.abortCopyDir){
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          bAbortDirectory = true;
          return exit().stateCopyFileFinished.entry(mEventConsumed);
        } 
        else if(cmd == FileRemote.Cmd.abortAll){
          Copy_FileLocalAcc.this.storedCopyEvents.clear();   //abort all other files too.
          return exit().exit().stateReady.entry(mEventConsumed);
          
        }else{
          return eventNotConsumed;
        }
      }
    }
  }
  


}
