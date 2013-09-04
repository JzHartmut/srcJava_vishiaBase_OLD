package org.vishia.fileLocalAccessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.fileRemote.FileRemote;
import org.vishia.stateMachine.StateCompositeBase;
import org.vishia.stateMachine.StateSimpleBase;
import org.vishia.stateMachine.StateTopBase;
import org.vishia.util.Assert;
import org.vishia.util.Event;
import org.vishia.util.EventThread;
import org.vishia.util.EventTimerMng;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;

public class Copy_FileLocalAcc
{

  /**Version, history and license.
   * <ul>
   * <li>2013-07-29 Hartmut chg: {@link #execMove(org.vishia.fileRemote.FileRemote.CmdEvent) now have a parameter with file names. 
   * <li>2013-04-21 Hartmut redesign check, copy
   * <li>2013-04-00 Hartmut created, extra class dissolved from {@link FileRemoteAccessorLocalFile}
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
  public static final int version = 20130421;

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
  
  
  
  /**The event type for intern events. One permanent instance of this class will be created. */
  final class EventCpy extends Event<CmdCpyIntern, CmdCpyIntern>{
    
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
  FileRemote.CallbackEvent evBackInfo;
  
  
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
    emptyDir,
    /**Continue the check action. */
    check
    
  }

  /**Local helper class (only instantiated local) to check which type of event.
   */
  private static class PrepareEventCmd{
    final FileRemote.Cmd cmde;
    final CmdCpyIntern cmdi;
    final FileRemote.CmdEvent eve;
    final EventCpy evi;
    
    PrepareEventCmd(Event<?,?> evP){
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
  
  FileRemote.Cmd cmdFile;
  
  /**Set one time after receive event to skip in {@link StateAsk.StateCopyAsk}. 
   * Reset if used in the next state transition of the following {@link Copy.StateCopyFileContent}. */
  boolean bAbortDirectory;
  
  /**Set one time after receive event to overwrite in {@link StateAsk.StateCopyAsk}. 
   * Reset if used in the next state transition of the following {@link Copy.StateCopyFileContent}. */
  boolean bOverwrfile;
  
   long timestart;
  
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
   * Only used in this thread {@link FileRemoteAccessorLocalFile#runCommissions}. 
   * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
   * at least the size of 1 record of the file system. */
  byte[] buffer = new byte[0x100000];  //1 MByte 16 kByte buffer
  
  
  boolean bCopyFinished;
  
  /**Used to generate the order idents. */
  private long checkOrderCounter;
  
  /**Container of all check commands. */
  private final Map<Long, CopyOrder> checkedOrders = new TreeMap<Long, CopyOrder>();
  
  /**More as one order to check and copy is executed after {@link StateCheckProcess} or {@link StateCopyProcess}
   * is leaved. The event is kept for future execution in the state {@link StateCopyReady}
   */
  final ConcurrentLinkedQueue<Event<FileRemote.Cmd, FileRemote.CallbackCmd>> storedCopyEvents = new ConcurrentLinkedQueue<Event<FileRemote.Cmd, FileRemote.CallbackCmd>>();
  
  
  
  /**Selection mask for files. See {@link FileRemote#check(String, String, org.vishia.fileRemote.FileRemote.CallbackEvent)}. */
  String[] sMaskCheck;
  
  
  //final Stack<DataSetCopy1Recurs> XXXrecursDirs = new Stack<DataSetCopy1Recurs>();
  
  /**The actual handled file set with source file, destination file, list files
   *  while running any state or switching between states while check or copy.
   *  The actData have a parent if it is not the root. It may have children.
   */
  DataSetCopy1Recurs actData;
  

  
  
  /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}
   * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}. */
  final Queue<DataSetCopy1Recurs> XXXcheckedFiles = new ConcurrentLinkedQueue<DataSetCopy1Recurs>();
  
  FileRemote fileSrc;
  
  boolean bOnlyOneFile;
  
  /**The destination directory for all members in {@link #checkedFiles}.*/
  FileRemote dirDst;
  
  //private File currentFile;

  private int ctWorkingId = 0;

  
  Copy_FileLocalAcc(FileRemoteAccessorLocalFile accessor){
    this.outer = accessor;
    evCpy = new EventCpy(accessor);
    stateCopy = new StateCopyTop(accessor.singleThreadForCommission, null);
  }
  

  
  /**Cmd check
   * @param ev
   */
  void checkCopy(FileRemote.CmdEvent ev){ 
    CopyOrder order = new CopyOrder(++checkOrderCounter);
    order.fileSrc = ev.filesrc;
    this.modeCopyOper = ev.modeCopyOper;
    order.listCopyFiles.clear();
    FileRemote.CallbackEvent evback = ev.getOpponent();
    if(order.fileSrc.isDirectory()){ 
      zBytesAllCheck = 0;
      zFilesCheck = 0;
      checkDir(order, order.fileSrc, 1);
    } else {
      order.listCopyFiles.add(order.fileSrc);
      zBytesAllCheck = ev.filesrc.length();
      zFilesCheck = 1;
    }
    checkedOrders.put(new Long(order.ident), order);
    evback.occupy(outer.evSrc, ++ctWorkingId, true);
    evback.nrofBytesAll = (int)zBytesAllCheck;  //number between 0...1000
    evback.nrofFiles = zFilesCheck;  //number between 0...1000
    evback.sendEvent(FileRemote.CallbackCmd.doneCheck);
  }
  
  
  
  /**subroutine for {@link #checkCopy(CmdEvent)}
   * @param dir
   * @param recursion
   */
  void checkDir(CopyOrder order, File dir, int recursion){
    //try{
      File[] files = dir.listFiles();
      for(File file: files){
        if(file.isDirectory()){
          if(recursion < 100){ //prevent loop with itself
            checkDir(order, file, recursion+1);  //recursively
          }
        } else {
          order.listCopyFiles.add(file);
          zFilesCheck +=1;
          zBytesAllCheck += file.length();
        }
      }
    //}catch(IOException exc){
      
    //}
  }

  
  
  void sendEventInternal(CmdCpyIntern cmd){
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
        System.err.println("FileRemoteAccessorLocalFile - forbidden move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
      } else {
        bOk =  co.filesrc.renameTo(filedst);
      }
    
    }
    if(bOk){
      cmd = FileRemote.CallbackCmd.done ; 
      System.out.println("FileRemoteAccessorLocalFile - move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
    } else {
      cmd = FileRemote.CallbackCmd.error ; 
      System.err.println("FileRemoteAccessorLocalFile - error move file;" + co.filesrc + "; to "+ co.filedst + "; success=" + cmd);
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
   * <li>State {@link #stateNextFile}: Check continue:
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
  
  
  
  void startCheck(FileRemote.CmdEvent ev){
    //gets and store data from the event:  
    //checkedFiles.clear();
    fileSrc = ev.filesrc;
    zBytesAllCheck = 0;
    zFilesCheck = 0;
    if(ev.maskSrc == null || ev.maskSrc.isEmpty()){ sMaskCheck = null; }
    else{
      sMaskCheck = ev.maskSrc.split(":");
      for(int ii=0; ii<sMaskCheck.length; ++ii){ sMaskCheck[ii] = sMaskCheck[ii].trim(); }
    }
    actData = new DataSetCopy1Recurs(null);  //The root instance of actData for check. It will be removed after check.
    if(ev.namesSrc !=null && ev.namesSrc.length() >0){
      bOnlyOneFile = false;
      actData.src = null;
      String[] sFilesSrc1 = ev.namesSrc.split(":");
      for(String sFileSrc: sFilesSrc1){
        FileRemote fileSrc1 = ev.filesrc.child(sFileSrc.trim());
        actData.addNewEntry(fileSrc1, null); //adds this file to the toplevel actData
      }
      actData = actData.listChildren.poll();  //start with the first valid file. Back to the root will taken the next file.
    } else { //only one file
      bOnlyOneFile = true;
      //ev.filesrc.selected = true;
      actData.src = ev.filesrc;
      //checkedFiles.add(actData);  //add to the root instance for copy.
    }
    //actData = checkedFiles.newEntry(ev.filesrc, ev.filedst);  //calls check() processing this actData.
    evBackInfo = ev.getOpponent();                  //available for back information.
    timestart = System.currentTimeMillis();
    
  }
  
  
  
  
  /**Process the ckeck in {@link StateCopyProcess#stateCheck}
   * @return true if the check has finished, false if the time is over and the check should be continued.
   */
  /**
   * @return
   */
  boolean processCheck(){
    boolean bFinit = false, bCont = false;   
    do {//continue in loop if there is work and the time is not out.
      try{
        System.out.println("Copy_FileLocalAcc - check;" + actData.toString());
        //if(!actData.src.isTested()){
        actData.src.refreshPropertiesAndChildren(null);
        //}
        if(actData.src.isDirectory()){
          //process all sub files
          long time1 = System.currentTimeMillis();
          for(Map.Entry<String, FileRemote> item: actData.src.children().entrySet()){
            FileRemote child = item.getValue();
            if(child.isDirectory()){
              //build a new recursive instance for the child to process in a next step.
              child.refreshProperties(null);
              DataSetCopy1Recurs childData = actData.addNewEntry(child, null);
            }
            else { //it's a file, check name, TODO parent dir
              boolean select = sMaskCheck == null;  //select if no maks given.
              String sChild = child.getName();
              //select=false;
              int ixMask = -1;
              while(!select && ixMask < sMaskCheck.length-1) {
                String s1 = sMaskCheck[++ixMask];
                FileRemote.Ecmp cmp;
                int z1 = s1.length();
                int posNotlastAsterisk = s1.lastIndexOf('*', z1-2);
                int posSlash = s1.lastIndexOf('/');
                
                String sCmpName; 
                if(s1.charAt(z1-1) == '*' && posNotlastAsterisk >=0){
                  sCmpName = s1.substring(posNotlastAsterisk+1, z1-1); 
                  cmp = posSlash >= z1-2 ? FileRemote.Ecmp.always 
                      : posNotlastAsterisk > posSlash ? FileRemote.Ecmp.contains
                      : FileRemote.Ecmp.starts;
                } else { 
                  sCmpName = s1.substring(posNotlastAsterisk+1, z1);
                  cmp = posNotlastAsterisk > posSlash ? FileRemote.Ecmp.ends: FileRemote.Ecmp.equals;
                }
                switch(cmp){
                  case starts: select= sChild.startsWith(sCmpName); break;
                  case ends: select= sChild.endsWith(sCmpName); break;
                  case contains: select= sChild.contains(sCmpName); break;
                  case equals: select= sChild.equals(sCmpName); break;
                  case always: select=true;
                }
              }
              if(select){
                actData.selectAnyFile = true;
                if(!child.isTested(time1-55000)) { //don't refresh if it was refreshed for 5 seconds.
                  child.refreshProperties(null);   ////
                }
                child.setMarked(1);  //a selected file.
                zBytesAllCheck += child.length();
                zFilesCheck += 1;
              } else if(child.isMarked(1)){
                //count selected files too, selected in the past.
                zBytesAllCheck += child.length();
                zFilesCheck += 1;
              }
            }
          }
          if(actData.selectAnyFile){
            actData.src.setMarked(1);  //select the directory.
          }
        } else {
          actData.src.setMarked(1);
          zBytesAllCheck += actData.src.length();
          zFilesCheck += 1;
          //actData = actData.parent;
        }
        //next file:
        DataSetCopy1Recurs nextChild = null;
        if(actData.listChildren !=null){
          nextChild = actData.listChildren.poll();
          actData = nextChild;  //recursion in data. execute in next step. 
        }
        if(nextChild == null){
          do{
            
            DataSetCopy1Recurs parentData = actData.parent;
            if(parentData !=null){
              parentData.selectAnyFile |= actData.selectAnyFile;
              if(parentData.selectAnyFile && parentData.src !=null){
                parentData.src.setMarked(1);
              }
            }
            actData = parentData;
            if(actData !=null && actData.listChildren !=null){
              nextChild = actData.listChildren.poll();
              if(nextChild !=null){ actData = nextChild; }
              //else { actData = actData.parent; }
            }
          } while( nextChild == null && actData !=null);
        }
        bCont = actData !=null;
        if(actData ==null){
          //the root instance is reached yet, this check is finished.
          bCont = false;
          bFinit = true;
        }
        if(bCont){
          //synchronized(this){ try{ wait(20);} catch(InterruptedException exc){} } //only test.
          long time = System.currentTimeMillis();
          //
          //feedback of progression after about 0.3 second. 
          if(time > timestart + 300){
            timestart = time;
            bCont = false;
          }
        }
          
      } catch(Exception exc){
        //bContCopy = false;
        System.err.printf("FileRemoteAccessorLocalFile - Copy error; %s\n", exc.getMessage());
        sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorCopy );
        bCont = false;
      }
    }while(bCont);
    FileRemote.CallbackCmd backCmd; 
    if(bFinit){
      backCmd = FileRemote.CallbackCmd.doneCheck;
    } else {
      backCmd = FileRemote.CallbackCmd.nrofFilesAndBytes;
    }
    //synchronized(this){ try{ wait(20);} catch(InterruptedException exc){} } //only test.
    if(backCmd != FileRemote.CallbackCmd.free){
      if(evBackInfo.occupyRecall(outer.evSrc, false)){
        
        evBackInfo.promilleCopiedBytes = (int)(((float)(Copy_FileLocalAcc.this.zBytesFileCopied) / Copy_FileLocalAcc.this.zBytesFile) * 1000);  //number between 0...1000
        if(zBytesAllCheck >0){
          evBackInfo.promilleCopiedFiles = (int)(((float)(zBytesAllCopied) / zBytesAllCheck) * 1000);  //number between 0...1000
        } else {
          evBackInfo.promilleCopiedFiles = 0;
        }
        evBackInfo.nrofFiles = zFilesCheck;
        evBackInfo.nrofBytesAll = (int)zBytesAllCheck;
        if(actData !=null){
          String absPath = actData.src.getAbsolutePath();
          if(absPath.length() > evBackInfo.fileName.length-1){
            absPath = "..." + absPath.substring(evBackInfo.fileName.length -4);  //the trailing part.
          }
          StringFunctions.copyToBuffer(absPath, evBackInfo.fileName);
        } else {
          StringFunctions.copyToBuffer("finished check", evBackInfo.fileName);
        }
        evBackInfo.sendEvent(backCmd );
      }
    }
    return bFinit;
  }
  
  
  private DataSetCopy1Recurs getNextCopyData(){
    //actData = checkedFiles.poll();  //start with the first entry.
    actData = new DataSetCopy1Recurs(null);
    actData.src = fileSrc;
    if(bOnlyOneFile && dirDst.exists() && dirDst.isDirectory()){
      String sName = actData.src.getName();
      FileRemote fileDst = dirDst == null ? null : dirDst.child(sName); 
      actData.dst = fileDst;                                 //complete actData with dst.
    } else {
      actData.dst = dirDst;  //it is a file.
    }
    /*
    if(actData !=null){
      String sName = actData.src.getName();
      FileRemote fileDst = dirDst.child(sName); 
      actData.dst = fileDst;                                 //complete actData with dst.
    }
    */
    return actData;
  }
  
  
  
  
  
  void copyAbort(){
    zBytesAllCheck = 0;
    zFilesCheck = 0;
    try{
      storedCopyEvents.clear();
      //fileSrc.resetSelectedRecurs(1,null);
      zBytesAllCheck = zBytesAllCopied = 0;
      zFilesCheck = zFilesCopied = 0;
      /*
      for(DataSetCopy1Recurs data: checkedFiles){
        FileRemote src = data.src;
        if(src !=null){  //maybe null at toplevel.
          src.resetSelected(1);
          if(src.isDirectory()){
            for(Map.Entry<String, FileRemote> item: src.children().entrySet()){
              FileRemote child = item.getValue();
              if(child.isSelected(1)){
                child.resetSelected(1);
                if(child.isDirectory()){
                  
                }
                } else {
                  //a file
                }
            }
          } else {
            src.resetSelected(1);
          }
        }
      }//for
      */
      //checkedFiles.clear();
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
      System.err.println("Copy_FileLocalAcc.abort() - Exception; " + exc.getMessage());
    }
    actData = null;
  }
  
  
  
  final StateCopyTop stateCopy;
  
  public class StateCopyTop extends StateTopBase<StateCopyTop>{

    StateCopyReady stateReady = new StateCopyReady(this);
    //StateCopyStart start = new StateCopyStart(this);
    StateCopyProcess stateProcess = new StateCopyProcess(this);

    protected StateCopyTop(EventThread thread, EventTimerMng timer) {
      super("StateCopyTop", thread, timer);
      setDefaultState(stateReady);
    }

  }

  
  private class StateCopyReady extends StateSimpleBase<StateCopyTop>{
    
    
    StateCopyReady(StateCopyTop superState) { super(superState, "Ready"); }

    //A(MainState enclosingState){ super(enclosingState); }
  
    @Override public int trans(Event<?,?> evP) {
      if(evP instanceof FileRemote.CmdEvent){
        FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
        cmdFile = ev.getCmd();
        if(cmdFile == FileRemote.Cmd.copyChecked || cmdFile == FileRemote.Cmd.delChecked || cmdFile == FileRemote.Cmd.moveChecked ){
          //gets and store data from the event:
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          dirDst = ev.filedst;
          actData = getNextCopyData();
          //newDataSetCopy(ev.filesrc, ev.filedst);
          evBackInfo = ev.getOpponent();                 //available for back information.
          timestart = System.currentTimeMillis();
          zBytesAllCopied = zFilesCopied = 0;
          return exit().stateProcess.stateDirOrFile.entry(ev);
        }
        else if(cmdFile == FileRemote.Cmd.check){
          startCheck(ev);
          sendEventInternal(CmdCpyIntern.check);   //continue in stateCheckProcess
          return exit().stateProcess.stateCheck.entry(ev);
        } 
        else if(cmdFile == FileRemote.Cmd.abortAll){
          copyAbort();
          return exit().stateReady.entry(ev);
        }
        else {
          return 0;
        }
      }
      else {
        return eventNotConsumed;
      }
    }
  }

      
  private class StateCopyProcess extends StateCompositeBase<StateCopyProcess, StateCopyTop>{

    StateDirOrFile stateDirOrFile = new StateDirOrFile(this);
    StateSubDir stateSubdir = new StateSubDir(this);
    StateCopyFileContent stateCopyFileContent = new StateCopyFileContent(this);
    StateNextFile stateNextFile = new StateNextFile(this);
    StateAsk stateAsk = new StateAsk(this);

    protected StateCopyProcess(StateCopyTop superState) { super(superState, "Process");  setDefaultState(stateDirOrFile);}


    @Override public int trans(Event<?,?> evP){
      PrepareEventCmd ev = new PrepareEventCmd(evP);
      if(ev.cmde == FileRemote.Cmd.check || ev.cmde == FileRemote.Cmd.copyChecked){
        ev.eve.donotRelinquish();
        storedCopyEvents.add(ev.eve);  //save it, execute later if that cmdCopy is finished.
        return StateSimpleBase.mEventConsumed;
      }
      else if(ev.cmde == FileRemote.Cmd.abortAll){
        copyAbort();
        return exit().stateReady.entry(evP);
      } else {
        return 0;
      }
    }

    
    private final StateSimpleBase<StateCopyProcess> stateCheck = new StateSimpleBase<StateCopyProcess>(this, "Check", false){
  
      int transCheck(){
        sendEventInternal(CmdCpyIntern.check); //keep alive the copy process.
        return mEventConsumed; 
      }

      int transReady(Event<?, ?> ev){ return exit().exit().stateReady.entry(ev); }

      @Override protected int trans(Event<?, ?> evP)
      { PrepareEventCmd ev = new PrepareEventCmd(evP);  
        if(ev.cmdi == CmdCpyIntern.check){
          if(processCheck()){
            return transReady(evP); //exit().exit().stateReady.entry(ev); 
          }
          else return transCheck(); //mEventConsumed;  //remain in state.
        } else return 0;
      }
    };
    
    
    
 
    
    
    protected StateMoveFile stateMoveFile = new StateMoveFile(this);
    
    /**Executes move of one file. The move action will be done on entry.
     * The result of move is set in internal bits. 
     * The trans routine will be called as runToCompletion. 
     * Depending on the result of move either state Ask or NextFile will be entered.
     *
     */
    private final class StateMoveFile extends StateSimpleBase<StateCopyProcess>{

      boolean bmove = false, bskip = false, bask = false, berror = false;

      StateMoveFile(StateCopyProcess superState) { super(superState, "MoveFile", true); }
      
      @Override protected void entryAction(Event<?,?> ev){
        if(actData.src.exists()){  //do nothing if src does not exists
          if(!actData.src.canWrite()){  //can't move it!
            if((modeCopyOper & FileRemote.modeCopyReadOnlyNever)!=0){
              bmove = false;
            } else if((modeCopyOper & FileRemote.modeCopyReadOnlyAks)!=0){
              bask = true;
            }
            else if(!actData.src.setWritable(true)){
              berror = true;
            } else {
              bmove = true;
            }
          }
          if(bmove && actData.dst.exists()){
            if(!actData.dst.canWrite()){
              //TODO check dst
            } else {
              berror = !actData.dst.delete();  //delete the destination before move.
            }
          }
        } else { //not exists
          bskip = true;
        }
        if(bmove){
          berror = !actData.src.renameTo(actData.dst);
        }
      }

      private int transAsk(Event<?,?> ev){
        if(bask){ return exit().stateAsk.entry(ev); }
        else if(berror){ return exit().stateAsk.entry(ev); }
        else return 0;
      }
      
      private int transNextFile(Event<?,?> ev){
        return exit().stateNextFile.entry(ev);
      }

      @Override protected int trans(Event<?,?> ev)
      { int ret;
        if((ret = transAsk(ev))!=0) return ret;
        else return transNextFile(ev);
      }
      
      
    }

    
    protected StateDelFile stateDelFile = new StateDelFile(this);
    
    private final class StateDelFile extends StateSimpleBase<StateCopyProcess>{

      boolean bdelete = false, bskip = false, bask = false, berror = false;

      StateDelFile(StateCopyProcess superState) { super(superState, "DelFile", true); }
      
      @Override protected void entryAction(Event<?,?> ev){
        if(actData.src.exists()){  //do nothing if src does not exists
          if(!actData.src.canWrite()){
            if((modeCopyOper & FileRemote.modeCopyReadOnlyNever)!=0){
              bdelete = false;
            } else if((modeCopyOper & FileRemote.modeCopyReadOnlyAks)!=0){
              bask = true;
            }
            else if(!actData.src.setWritable(true)){
              berror = true;
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
          berror = !actData.src.delete();
        }
      }
      
      
      private int transAsk(Event<?,?> ev){
        if(bask){ return exit().stateAsk.entry(ev); }
        else if(berror){ return exit().stateAsk.entry(ev); }
        else return 0;
      }
      
      private int transNextFile(Event<?,?> ev){
        return exit().stateNextFile.entry(ev);
      }

      @Override protected int trans(Event<?,?> ev)
      { int ret;
        if((ret = transAsk(ev))!=0) return ret;
        else return transNextFile(ev);
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
  private class StateDirOrFile extends StateSimpleBase<StateCopyProcess>{
    
    
    StateDirOrFile(StateCopyProcess superState) { super(superState, "DirOrFile"); }

    
    private int transCopySubdir(Event<?,?> ev){
      if(actData.src.isDirectory()){
        return exit().stateSubdir.entry(ev);
      } else return 0;
    }
    
    private int transDelFile(Event<?,?> ev){
      if(cmdFile == FileRemote.Cmd.delChecked){
        return exit().stateDelFile.entry(ev);
      } else return 0;
    }
    
    private int transMoveFile(Event<?,?> ev){
      if(cmdFile == FileRemote.Cmd.move){
        return exit().stateMoveFile.entry(ev);
      } else return 0;
    }
    
    /**This state processes a new {@link DataSetCopy1Recurs} stored in {@link #actData}.
     * It branches to the necessary next state:
     * <ul>
     * <li>(())-----[src.isDirectory()]---->{@link StateSubDir.StateCopySubDir}
     * <li>(())-----/open, create Files --->{@link Copy.StateCopyFileContent}
     *   <ul>
     *   <li>? exception ----->{@link StateAsk.StateCopyAsk}
     *   </ul>                   
     * </ul>
     * @param ev
     * @return
     */
    @Override public int trans(Event<?,?> ev) {
      //EventCpy ev = evP;
      if(actData.src.getName().equals("Iba"))
        Assert.stop();
      int ret;
      if( (ret = transCopySubdir(ev)) !=0) return ret;
      if( (ret = transDelFile(ev)) !=0) return ret;
      if( (ret = transMoveFile(ev)) !=0) return ret;
      else {
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
              return exitState.stateAsk.entry(null); 
            }
          }
          else if((Copy_FileLocalAcc.this.modeCopyOper & FileRemote.modeCopyExistMask) == FileRemote.modeCopyExistSkip){
            //generally don't overwrite existing files:
            return exitState.stateNextFile.entry(null); 
          }
          else if(actData.dst.canWrite()){
            //can overwrite, but check timestamp
            long timeSrc = actData.src.lastModified();
            long timeDst = actData.dst.lastModified();
            //The destination file exists and it is writeable. Check:
            switch(Copy_FileLocalAcc.this.modeCopyOper & FileRemote.modeCopyExistMask){
              case FileRemote.modeCopyExistAsk: 
                sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstOverwr );
                return exitState.stateAsk.entry(null); 
              case FileRemote.modeCopyExistNewer: 
                if( (timeSrc - timeDst) < 0){
                  return exitState.stateNextFile.entry(null); 
                } break; //else: do copy
              case FileRemote.modeCopyExistOlder: 
                if( (timeSrc - timeDst) > 0){
                  return exitState.stateNextFile.entry(null); 
                }  //else: do copy
            }
          } else {  //can not write, readonly
            //The destination file exists and it is readonly. Check:
            switch(Copy_FileLocalAcc.this.modeCopyOper & FileRemote.modeCopyReadOnlyMask){
              case FileRemote.modeCopyReadOnlyAks: 
                sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstReadonly );
                return exitState.stateAsk.entry(null); 
              case FileRemote.modeCopyReadOnlyNever: 
                return exitState.stateNextFile.entry(null); 
              case FileRemote.modeCopyReadOnlyOverwrite: {
                boolean bOk = actData.dst.setWritable(true);
                if(!bOk){
                  sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
                  return exitState.stateAsk.entry(null); 
                }  //else now try to open to overwrite.
              } break;  
            }
            
          }
        }
        Copy_FileLocalAcc.this.zBytesFile = actData.src.length();
        Copy_FileLocalAcc.this.zBytesFileCopied = 0;
        try{ 
          FileSystem.mkDirPath(actData.dst);
          Copy_FileLocalAcc.this.out = new FileOutputStream(actData.dst);
        } catch(IOException exc){
          sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorDstCreate );
          return exitState.stateAsk.entry(null);
        }

        try{ Copy_FileLocalAcc.this.in = new FileInputStream(actData.src);
        } catch(IOException exc){
          sendEventAsk(actData.src, FileRemote.CallbackCmd.askErrorSrcOpen );
          return exitState.stateAsk.entry(null);
        }
        return exitState.stateCopyFileContent.entry(null);
      }
    }
  }


  
  private class StateSubDir extends StateSimpleBase<StateCopyProcess>{
    
    
    StateSubDir(StateCopyProcess superState) { super(superState, "SubDir", false); }

    @Override public void entryAction(Event<?,?> ev){
      //super.entry(consumed);
      //onentry action
      //first send a callback
      if(actData.src !=null && evBackInfo.occupyRecall(outer.evSrc, false)){
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
        List<FileRemote> children = new ArrayList<FileRemote>(actData.src.children().values());
        //for(Map.Entry<String, FileRemote> item: actData.src.children().entrySet()){
        //  FileRemote child = item.getValue();
        for(FileRemote child: children){
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
        sendEventInternal(CmdCpyIntern.emptyDir);
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
        sendEventInternal(CmdCpyIntern.subDir);  //continue with stateCopyDirOrFile
      }
      //return consumed;  //return to the queue

    }
    
    /**
     * <ul>
     * <li>(())---{@link CmdCpyIntern#subDir}----->{@link #entry_CopyDirFile(EventCpy)}
     * </ul>
     * @param ev
     * @return
     */
    @Override public int trans(Event<?,?> ev) {
      if(ev == null) return 0;
      if(ev.getCmd() == CmdCpyIntern.subDir){
        return exit().stateDirOrFile.entry(ev);  //exit and entry in the same state.
      } else if(ev.getCmd() == CmdCpyIntern.emptyDir){ 
        return exit().stateNextFile.entry(ev);  //exit and entry in the same state.
      } else {
        return eventNotConsumed;
      }
    }
      
  }


  private class StateCopyFileContent extends StateSimpleBase<StateCopyProcess>{
    
    
    StateCopyFileContent(StateCopyProcess superState) { super(superState, "FileContent"); }

    @Override public void entryAction(Event<?,?> ev){
      sendEventInternal(CmdCpyIntern.file); 
      bCopyFinished = false;
      //return super.entry(isConsumed);
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
    @Override public int trans(Event<?,?> evP) {
      PrepareEventCmd ev = new PrepareEventCmd(evP);
      int newState;   
      if(ev.cmdi == CmdCpyIntern.file){
        //boolean bContCopy;
        newState = -1;   //it set, then the loop should terminated
        do {
          try{
            int zBytes = Copy_FileLocalAcc.this.in.read(buffer);
            if(zBytes > 0){
              Copy_FileLocalAcc.this.zBytesFileCopied += zBytes;
              zBytesAllCopied += zBytes;
              Copy_FileLocalAcc.this.out.write(buffer, 0, zBytes);
              //synchronized(this){ try{ wait(50);} catch(InterruptedException exc){} } //only test.
              long time = System.currentTimeMillis();
              //
              //feedback of progression after about 0.3 second. 
              if(time > timestart + 300){
                timestart = time;
                if(evBackInfo.occupyRecall(outer.evSrc, false)){
                    
                  evBackInfo.promilleCopiedBytes = (int)(((float)(Copy_FileLocalAcc.this.zBytesFileCopied) / Copy_FileLocalAcc.this.zBytesFile) * 1000);  //number between 0...1000
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
                sendEventInternal(CmdCpyIntern.file); //keep alive the copy process.
                newState = mEventConsumed;
              } 
            } else if(zBytes == -1){
              //bContCopy = false; ///
              bCopyFinished = true;
              newState = exit().stateNextFile.entry(evP);
              
            } else {
              //0 bytes ?
              //bContCopy = true;
              //newState = false;
            }
          } catch(IOException exc){
            //bContCopy = false;
            System.err.printf("FileRemoteAccessorLocalFile - Copy error; %s\n", exc.getMessage());
            sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorCopy );
            newState = exit().stateAsk.entry(evP);
          }
        }while(newState == -1);
        
      } else if(ev.cmde == FileRemote.Cmd.abortCopyFile ){
        
        newState = exit().stateNextFile.entry(evP);
      } else {
        newState = eventNotConsumed;  //+eventConsumed
      }
      return newState;
    }
    
    @Override protected void exitAction(){
      try{
        if(Copy_FileLocalAcc.this.in!=null){ 
          zFilesCopied +=1;
          Copy_FileLocalAcc.this.in.close(); 
          actData.src.resetMarked(1);
        }
        Copy_FileLocalAcc.this.in = null;
        if(Copy_FileLocalAcc.this.out!=null){ Copy_FileLocalAcc.this.out.close(); }
        Copy_FileLocalAcc.this.out = null;
        if(actData !=null && actData.dst !=null){
          if(!bCopyFinished){ ///
            if(!actData.dst.delete()) {
              System.err.println("FileRemoteAccessorLocalFile - Problem delete after abort; " + actData.dst.getAbsolutePath());
            }
          } else {
            long date = actData.src.lastModified();
            actData.dst.setLastModified(date);
          }
        }
      } catch(IOException exc){
        System.err.printf("FileRemoteAccessorLocalFile - Problem close; %s\n", actData.dst.getAbsolutePath());
      }
    }
  }

  

  /**This state is activated after one file was processed from {@link StateCopyFileContent}
   * or if the file was skipped (from {@link StateDirOrFile}. 
   * <ul>
   * <li>entry: Checks whether there is a further file to copy in the same directory 
   *   or it returns to the parent directory and checks the further files there.
   *   If all files are copied the {@link Copy#actData} are set to null. 
   * <li>trans [actData==null] ==> {@link StateCopyReady}
   * <li>trans [else] ==> {@link StateDirOrFile}
   * </ul>
   * The state has only conditional transitions.
   * <br><br>
   * 
   * @author Hartmut
   *
   */
  private class StateNextFile extends StateSimpleBase<StateCopyProcess>{
    
    StateNextFile(StateCopyProcess superState) { super(superState, "NextFile", true); }

    @Override public void entryAction(Event<?,?> ev){
      //super.entry(isConsumed);
      //stateCopyProcess = EStateCopyProcess.FileFinished;
      boolean bCont;
      //close currently file if this state is entered from stateAsk. The regular close() is executed on exit of stateCopyFile.
      actData.src.resetMarked(1); ///
      if(Copy_FileLocalAcc.this.in !=null){
        zFilesCopied +=1;
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
    }
    
    
    
    /**If another file or directory is found.
     */
    private int transDirOrFile(Event<?,?> evP){ return exit().stateDirOrFile.entry(evP); }

    private int transReady(Event<?,?> evP){ 
      if(actData == null){
        //send done Back
        if(evBackInfo.occupyRecall(1000, outer.evSrc, false)){
          evBackInfo.sendEvent(FileRemote.CallbackCmd.done);
          Event<?,?> ev1;
          while( (ev1 = storedCopyEvents.poll() ) !=null) {
            ev1.sendEvent();
          }
          
        }
        return exit().exit().stateReady.entry(evP);
      } else return 0;
    }

    
    
    @Override public int trans(Event<?,?> evP) {
      int ret;
      if((ret = transReady(evP)) !=0) return ret;
      else return transDirOrFile(evP); //another file or directory
    }
  }


  private class StateAsk extends StateSimpleBase<StateCopyProcess>{
    
    
    StateAsk(StateCopyProcess superState) { super(superState, "Ask"); }

    @Override public void entryAction(Event<?,?> ev){
      //onyl to set a breakpoint.
      //return super.entry(isConsumed);
    }
    
    
    @Override public int trans(Event<?,?> evP) {
      if(evP ==null){ 
        return 0;
      } else {
        FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
        FileRemote.Cmd cmd = ev.getCmd();
        //
        if(cmd == FileRemote.Cmd.overwr){
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          bOverwrfile = true;
          return exit().stateDirOrFile.entry(ev);
        } 
        else if(cmd == FileRemote.Cmd.abortCopyFile){   //it is the skip file.
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          return exit().stateNextFile.entry(ev);
        } 
        else if(cmd == FileRemote.Cmd.abortCopyDir){
          Copy_FileLocalAcc.this.modeCopyOper = ev.modeCopyOper;
          bAbortDirectory = true;
          return exit().stateNextFile.entry(ev);
        } 
        else if(cmd == FileRemote.Cmd.abortAll){
          copyAbort();
          return exit().exit().stateReady.entry(ev);
          
        }else{
          return eventNotConsumed;
        }
      }
    }
  }
  


}
