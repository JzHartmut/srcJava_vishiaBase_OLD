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
import org.vishia.event.EventMsg;
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

public class FileRemoteCopy_NEW
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

  
  private enum CmdCpyIntern { //Event.Cmd {
    free, reserve,
    
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

  
  EventSource evSrc = new EventSource("FileRemoteCopy"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };
  
  
  EventThread copyThread = new EventThread("FileRemoteCopy");
  

  /**The event type for intern events. One permanent instance of this class will be created. */
  public final class EventCpy extends EventMsg2<CmdCpyIntern, CmdCpyIntern>{
    
    private static final long serialVersionUID = 2904627756828656797L;


    /**The simple constructor calls {@link EventMsg2#Event(Object, EventConsumer, EventThread)}
     * with the {@link FileAccessorLocalJava7#executerCommission}
     * and the {@link FileAccessorLocalJava7#singleThreadForCommission}
     * because the event should be send to that.
     */
    EventCpy(EventConsumer dst){
      super(null, dst, copyThread, new EventCpy(dst, true));
    }
    
    /**Creates a simple event as opponent. */
    EventCpy(EventConsumer dst, boolean second){
      super(null, dst, copyThread, null);
    }
    
    
    @Override public EventCpy getOpponent(){ return (EventCpy)super.getOpponent(); }
  };
  
  /**The only one instance for this event. It is allocated permanently. That is possible because the event is fired 
   * only once at the same time in this class locally and private.
   * The event and its oposite are used one after another. That is because the event is relinguished after it is need twice.
   */
  final EventCpy evCpy;
  


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
   * Only used in this thread {@link FileAccessorLocalJava7#runCommissions}. 
   * The size of 1 MByte may be enough for fast copy. 16 kByte is too less. It should be approximately
   * at least the size of 1 record of the file system. */
  byte[] buffer = new byte[0x100000];  //1 MByte 16 kByte buffer
  
  
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
  
  
  //FileRemote.Cmd cmdFile;
  

  FileRemote fileSrc;
  
  boolean bOnlyOneFile;
  
  /**The destination directory for all members in {@link #checkedFiles}.*/
  FileRemote dirDst;
  
  private int ctWorkingId = 0;

  /**Stored callback event when the state machine is in state {@link EStateCopy#Process}. 
   * 
   */
  FileRemote.CallbackEvent evBackInfo;
  
  public final States statesCopy;
  
  public FileRemoteCopy_NEW()
  {
    statesCopy = new States();
    evCpy = new EventCpy(statesCopy);
  }
  
  
  
  void sendEventInternal(CmdCpyIntern cmd){
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
    
    final class Ready extends StateSimple
    {
      /**Start transition to {@link Process.DirOrFile} */
      Trans start_dirOrFile(FileRemote.CmdEvent ev, Trans trans) {
        if(trans == null) return new Trans(Process.DirOrFile.class);
        FileRemote.Cmd cmdFile = ev.getCmd();
        if(cmdFile == FileRemote.Cmd.copyChecked || cmdFile == FileRemote.Cmd.moveChecked) {
          trans.doExit();
          //action:
          FileRemoteCopy_NEW.this.modeCopyOper = ev.modeCopyOper;
          dirDst = ev.filedst;
          actData = getNextCopyData();
          //newDataSetCopy(ev.filesrc, ev.filedst);
          evBackInfo = ev.getOpponent();                 //available for all back information while this copying
          timestart = System.currentTimeMillis();
          zBytesAllCopied = zFilesCopied = 0;
          //entry in DirOrFile.
        }
        return null;
      }
    }
    
    
    final class Process extends StateComposite {
      
      /**Set in any sub state entry if the state {@link Ask} should be activated with run to complete. */
      boolean bAsk;
      
      /**Set in any sub state entry if the state {@link NextFile} should be activated with run to complete. */
      boolean bNextFile;
      
      class TransAsk extends Trans {
        TransAsk(){ super(1, Ask.class);}
        @Override protected void check(EventObject ev){ if(bAsk){ retTrans |= mTransit;} }
      }

      class TransNextFile extends Trans {
        TransNextFile(){ super(2, NextFile.class);}
        @Override protected void check(EventObject ev){ if(bNextFile){ retTrans |= mTransit;} }
      }

      /**sets {@link #bAsk} and {@link #bNextFile} to false because it is set in the states then.
       */
      @Override protected int entry(EventObject ev) { 
        bAsk = bNextFile = false;
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
                bAsk = true;
              }
              else if(!actData.src.setWritable(true)){
                bAsk = true;
              } else {
                bmove = true;
              }
            }
            if(bmove && actData.dst.exists()){
              if(!actData.dst.canWrite()){
                //TODO check dst
              } else {
                bAsk = !actData.dst.delete();  //delete the destination before move.
              }
            }
          } else { //not exists
            bskip = true;
          }
          if(bmove){
            bAsk = !actData.src.renameTo(actData.dst);
          }
          bNextFile = true;
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
                bAsk = true;
              }
              else if(!actData.src.setWritable(true)){
                bAsk = true;
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
            bAsk = !actData.src.delete();
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
  
        Trans transCopySubdir(EventMsg<?> ev, Trans trans){
          if(trans == null) return new Trans(1, Subdir.class);
          if(actData.src.isDirectory()){
            trans.retTrans = mTransit;
          }
          return null;
        }
        
        Trans transDelFile(EventMsg<?> ev, Trans trans){
          if(trans == null) return new Trans(2, DelFile.class);
          if(cmdFile == FileRemote.Cmd.delChecked){
            trans.retTrans = mTransit;
          }
          return null;
        }
        
        Trans transMoveFile(EventMsg<?> ev, Trans trans){
          if(trans == null) return new Trans(3, MoveFile.class);
          if(cmdFile == FileRemote.Cmd.move){
            trans.retTrans = mTransit;
          }
          return null;
        }
        
        Trans transStartCopy(EventMsg<?> ev, Trans trans){
          if(trans == null) return new Trans(4, CopyFile.class);
          else trans.retTrans = mTransit;   //else transition.
          return null;
        }
        
        
        
      }
      
      class Subdir extends StateSimple {
        @Override protected int entry(EventObject ev){
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

          return mRunToComplete;
        } //entry
        
        
        class TransSubdir extends Trans {
          TransSubdir(){ super(1, DirOrFile.class);}
          @Override protected void check(EventObject ev){ 
            if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdCpyIntern.subDir){
              retTrans = mEventConsumed;
            }
          }
        }
        
 
        class TransEmptyDir extends Trans {
          TransEmptyDir(){ super(1, NextFile.class);}
          @Override protected void check(EventObject ev){ 
            if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdCpyIntern.emptyDir){
              retTrans = mEventConsumed;
            }
          }
        }
        
 
        
      }
      
      class CopyFile extends StateSimple {
        
        /**Checks the file.
         */
        @Override protected int entry(EventObject ev){
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
                bAsk = true;
                return 0; 
              }
            }
            else if((FileRemoteCopy_NEW.this.modeCopyOper & FileRemote.modeCopyExistMask) == FileRemote.modeCopyExistSkip){
              //generally don't overwrite existing files:
              bNextFile = true;
              return 0; 
            }
            else if(actData.dst.canWrite()){
              //can overwrite, but check timestamp
              long timeSrc = actData.src.lastModified();
              long timeDst = actData.dst.lastModified();
              //The destination file exists and it is writeable. Check:
              switch(FileRemoteCopy_NEW.this.modeCopyOper & FileRemote.modeCopyExistMask){
                case FileRemote.modeCopyExistAsk: 
                  sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstOverwr );
                  bAsk = true;
                  return 0; 
                case FileRemote.modeCopyExistNewer: 
                  if( (timeSrc - timeDst) < 0){
                    bNextFile = true;
                    return 0; 
                  } 
                  break; //else: do copy
                case FileRemote.modeCopyExistOlder: 
                  if( (timeSrc - timeDst) > 0){
                    bNextFile = true;
                    return 0; 
                  }  //else: do copy
                  break;
              }
            } else {  //can not write, readonly
              //The destination file exists and it is readonly. Check:
              switch(FileRemoteCopy_NEW.this.modeCopyOper & FileRemote.modeCopyReadOnlyMask){
                case FileRemote.modeCopyReadOnlyAks: 
                  sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstReadonly );
                  bAsk = true;
                  return 0; 
                  case FileRemote.modeCopyReadOnlyNever: 
                  bNextFile = true;
                  return 0; 
               case FileRemote.modeCopyReadOnlyOverwrite: {
                  boolean bOk = actData.dst.setWritable(true);
                  if(!bOk){
                    sendEventAsk(actData.dst, FileRemote.CallbackCmd.askDstNotAbletoOverwr );
                    bAsk = true;
                    return 0; 
                    }  //else now try to open to overwrite.
                } break;  
              }
              
            }
          }
          FileRemoteCopy_NEW.this.zBytesFile = actData.src.length();
          FileRemoteCopy_NEW.this.zBytesFileCopied = 0;
          try{ 
            FileSystem.mkDirPath(actData.dst);
            FileRemoteCopy_NEW.this.out = new FileOutputStream(actData.dst);
          } catch(IOException exc){
            sendEventAsk(actData.dst, FileRemote.CallbackCmd.askErrorDstCreate );
            bAsk = true;
            return 0; 
          }
  
          try{ FileRemoteCopy_NEW.this.in = new FileInputStream(actData.src);
          } catch(IOException exc){
            sendEventAsk(actData.src, FileRemote.CallbackCmd.askErrorSrcOpen );
            bAsk = true;
            return 0; 
          }
          sendEventInternal(CmdCpyIntern.copyFileContent);
          return 0; 
        }//entry
        
        /**Transition, the 3. to {@link CopyFileContent} */
        class TransCopyContent extends Trans {
          TransCopyContent(){ super(3, CopyFileContent.class);}
          @Override protected void check(EventObject ev) { 
            if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdCpyIntern.copyFileContent) { 
              retTrans |= mTransit;
            }
          }
        }
        
      }
      
      
      
      
      
      class CopyFileContent extends StateSimple {

        @Override protected int entry(EventObject ev) {
          //boolean bContCopy;
          boolean bCont = true;
          do {
            try{
              int zBytes = FileRemoteCopy_NEW.this.in.read(buffer);
              if(zBytes > 0){
                FileRemoteCopy_NEW.this.zBytesFileCopied += zBytes;
                zBytesAllCopied += zBytes;
                FileRemoteCopy_NEW.this.out.write(buffer, 0, zBytes);
                //synchronized(this){ try{ wait(50);} catch(InterruptedException exc){} } //only test.
                long time = System.currentTimeMillis();
                //
                //feedback of progression after about 0.3 second. 
                if(time > timestart + 300){
                  timestart = time;
                  if(evBackInfo.occupyRecall(evSrc, false)){
                      
                    evBackInfo.promilleCopiedBytes = (int)(((float)(FileRemoteCopy_NEW.this.zBytesFileCopied) / FileRemoteCopy_NEW.this.zBytesFile) * 1000);  //number between 0...1000
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
                  sendEventInternal(CmdCpyIntern.copyFileContent); //keep alive the copy process.
                  bCont = false;
                } 
              } else if(zBytes == -1){
                //bContCopy = false; ///
                bCopyFinished = true;
                bNextFile = true;
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
              bAsk = true;
              bCont = false;
            }
          }while(bCont);

          return mRunToComplete;
        }//entry
        
        
        @Override protected void exit(){
          try{
            if(FileRemoteCopy_NEW.this.in!=null){ 
              zFilesCopied +=1;
              FileRemoteCopy_NEW.this.in.close(); 
              actData.src.resetMarked(1);
              actData.src.resetMarked(FileMark.mCmpFile);
              actData.dst.resetMarked(FileMark.mCmpFile);
              actData.src.setDirShouldRefresh();
              actData.dst.setDirShouldRefresh();
            }
            FileRemoteCopy_NEW.this.in = null;
            if(FileRemoteCopy_NEW.this.out!=null){ FileRemoteCopy_NEW.this.out.close(); }
            FileRemoteCopy_NEW.this.out = null;
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
          } catch(IOException exc){
            System.err.printf("FileAccessorLocalJava7 - Problem close; %s\n", actData.dst.getAbsolutePath());
          }
        }

        /**Transition to this state itself to continue copying with event. 
         * It is possible that an abort event may occure. */
        class TransCopyContent extends Trans {
          TransCopyContent(){ super(CopyFileContent.class);}  //transition to this state itself.
          @Override protected void check(EventObject ev) { 
            if(ev instanceof EventCpy && ((EventCpy)ev).getCmd() == CmdCpyIntern.copyFileContent) { 
              retTrans |= mTransit;
            }
          }
        }
        

 
        /**Transition because an abort-file event is received. Switches to {@link NextFile} 
         * It is possible that an abort event may occure. */
        class TransAbortCopyFile extends Trans {
          TransAbortCopyFile(){ super(NextFile.class);}  //transition to this state itself.
          @Override protected void check(EventObject ev) { 
            FileRemote.CmdEvent ev1;
            if(ev instanceof FileRemote.CmdEvent && (ev1 = ((FileRemote.CmdEvent)ev)).getCmd() == FileRemote.Cmd.abortCopyDir) { 
              modeCopyOper  = ev1.modeCopyOper;
              retTrans |= mEventConsumed;
            }
          }
        }
        

        /**Transition because an abort-file event is received. Switches to {@link NextFile} 
         * It is possible that an abort event may occure. */
        class TransAbortCopyDir extends Trans {
          TransAbortCopyDir(){ super(NextFile.class);}  //transition to this state itself.
          @Override protected void check(EventObject ev) { 
            FileRemote.CmdEvent ev1;
            if(ev instanceof FileRemote.CmdEvent && (ev1 = ((FileRemote.CmdEvent)ev)).getCmd() == FileRemote.Cmd.abortCopyDir) { 
              modeCopyOper  = ev1.modeCopyOper;
              bAbortDirectory = true;
              retTrans |= mEventConsumed;
            }
          }
        }
        

      }
      
      
      
      
      
      class NextFile extends StateSimple {
        @Override protected int entry(EventObject ev) {
          boolean bCont;
          //close currently file if this state is entered from stateAsk. The regular close() is executed on exit of stateCopyFile.
          actData.src.resetMarked(1); ///
          if(FileRemoteCopy_NEW.this.in !=null){
            zFilesCopied +=1;
            try{ FileRemoteCopy_NEW.this.in.close();
            } catch(IOException exc){
              System.err.printf("FileAccessorLocalJava7 -Copy close src while abort, unexpected error; %s\n", exc.getMessage());
            }
            FileRemoteCopy_NEW.this.in = null;
          }
          if(FileRemoteCopy_NEW.this.out !=null){
            try{ FileRemoteCopy_NEW.this.out.close();
            } catch(IOException exc){
              System.err.printf("FileAccessorLocalJava7 -Copy close dst while abort, unexpected error; %s\n", exc.getMessage());
            }
            FileRemoteCopy_NEW.this.out = null;
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

        Trans transDirOrFile(FileRemote.CmdEvent ev, Trans trans){ 
          if(trans == null) return new Trans(2, DirOrFile.class);
          if(ev.getCmd() == FileRemote.Cmd.overwr){
            modeCopyOper  = ev.modeCopyOper;
            bOverwrfile = true;
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
        Trans transAbortFile(FileRemote.CmdEvent ev, Trans trans){ 
          if(trans == null) return new Trans(2, NextFile.class);
          if(ev.getCmd() == FileRemote.Cmd.abortCopyFile){
            modeCopyOper  = ev.modeCopyOper;
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
        Trans transAbortDir(FileRemote.CmdEvent ev, Trans trans){ 
          if(trans == null) return new Trans(2, NextFile.class);
          if(ev.getCmd() == FileRemote.Cmd.abortCopyDir){
            modeCopyOper  = ev.modeCopyOper;
            bAbortDirectory = true;
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
        Trans transAbortAll(FileRemote.CmdEvent ev, Trans trans){ 
          if(trans == null) return new Trans(2, NextFile.class);
          if(ev.getCmd() == FileRemote.Cmd.abortAll){
            modeCopyOper  = ev.modeCopyOper;
            copyAbort();
            trans.retTrans = mEventConsumed;
          }
          return null; 
        }
        
      }
      
      
    }
  }//class States  
}
