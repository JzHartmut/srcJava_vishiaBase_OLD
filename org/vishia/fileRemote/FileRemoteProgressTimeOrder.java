package org.vishia.fileRemote;

import org.vishia.event.EventConsumer;
import org.vishia.fileLocalAccessor.FileLocalAccessorCopyStateM;
import org.vishia.states.StateMachine;
import org.vishia.util.TimeOrderBase;
import org.vishia.util.TimeOrderMng;

/**This TimeOrder is used for progress showing in the callers area. It should be extended from the application
 * to start any showing process for the progress. 
 */
public abstract class FileRemoteProgressTimeOrder  extends TimeOrderBase
{
  /**The manager for this time order to execute it for showing, often a graphic thread adaption.
   */
  final TimeOrderMng mng;
  
  protected int delay;

  /**super constructor:
   * @param name Only for toString(), debug
   * @param mng The manager for this time order to execute it for showing, often a graphic thread adaption.
   *  For example use {@link org.vishia.gral.base.GralMng#gralDevice()} and there {@link org.vishia.gral.base.GralGraphicThread#orderList()}. 
   * @param delay The delay to start the oder execution after #show()
   */
  protected FileRemoteProgressTimeOrder(String name, TimeOrderMng mng, int delay){ 
    super(name); 
    this.mng = mng;
    this.delay = delay;
  }
  
  
  /**Current processed file. */
  public FileRemote currFile, currDir;
  
  /**Number of available directories and files, filled on check. */
  public int nrDirAvail, nrFilesAvail;
  
  /**Processed bytes. */
  public long nrofBytesAll, nrofBytesFile, nrofBytesFileCopied;
  
  /**Number of processed directories and files. */
  public int nrDirProcessed, nrFilesProcessed;
  
  /**Number of Files which are handled special. */
  public int nrofFilesMarked;
  
  /**Command for asking or showing somewhat. */
  public FileRemote.CallbackCmd cmd;
  
  public FileRemote.Cmd answer;
  
  /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
  public int modeCopyOper;
    
  public boolean bDone;

  
  /**If not null the state machine waits for an answer. */
  private FileRemote.CmdEvent eventAnswer;
  //FileLocalAccessorCopyStateM.EventCpy eventAnswer;
  
  private StateMachine consumerAnswer;
  
  
  public void show() {
    if(currFile == null || bDone){
      addToList(mng, delay);
    }
  }
  
  
  public void XXXrequAnswer(FileRemote.CallbackCmd cmd, FileRemote.CmdEvent ev) {
    this.cmd = cmd;
    this.eventAnswer = ev;
    addToList(mng, delay);
  }
  
  public void requAnswer(FileRemote.CallbackCmd cmd, StateMachine stateM) {
    this.cmd = cmd;
    this.consumerAnswer = stateM;
    addToList(mng, delay);   //to execute the request
  }
  
  
  /**An answer if somewhat is ask. */
  public void answer(FileRemote.Cmd answer) {
    if(eventAnswer !=null){
      eventAnswer.modeCopyOper = modeCopyOper;
      eventAnswer.sendEvent(answer);
    }
    if(consumerAnswer !=null) {
      this.answer = answer; 
      consumerAnswer.triggerRun();
    }
  }
  
}
