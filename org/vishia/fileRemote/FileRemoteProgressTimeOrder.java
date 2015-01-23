package org.vishia.fileRemote;

import org.vishia.event.EventConsumer;
import org.vishia.event.EventTimeOrderBase;
import org.vishia.event.EventThread;
import org.vishia.fileLocalAccessor.FileLocalAccessorCopyStateM;
import org.vishia.states.StateMachine;

/**This TimeOrder is used for progress showing in the callers area. It should be extended from the application
 * to start any showing process for the progress.  The extension should override the method {@link #executeOrder()} from the super class. 
 */
public abstract class FileRemoteProgressTimeOrder  extends EventTimeOrderBase
{
  /**The manager for this time order to execute it for showing, often a graphic thread adaption.
   */
  final EventThread mng;
  
  protected int delay;

  /**super constructor:
   * @param name Only for toString(), debug
   * @param mng The manager for this time order to execute it for showing, often a graphic thread adaption.
   *  For example use {@link org.vishia.gral.base.GralMng#gralDevice()} and there {@link org.vishia.gral.base.GralGraphicThread#orderList()}. 
   * @param delay The delay to start the oder execution after #show()
   */
  protected FileRemoteProgressTimeOrder(String name, EventThread mng, int delay){ 
    super(name, mng); 
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
  private FileRemote.CallbackCmd quest;
  
  private FileRemote.Cmd answer;
  
  private FileRemote.Cmd cmd;
  
  /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
  public int modeCopyOper;
    
  public boolean bDone;

  
  /**If not null the state machine waits for an answer. */
  private FileRemote.CmdEvent eventAnswer;
  //FileLocalAccessorCopyStateM.EventCpy eventAnswer;
  
  private StateMachine consumerAnswer;
  
  public FileRemote.CallbackCmd quest(){ return quest; }
  
  public FileRemote.Cmd answer(){ return answer; }
  
  public FileRemote.Cmd cmd(){ return cmd; }
  
  public void clearAnswer(){ answer = FileRemote.Cmd.free; } //remove the cmd as event-like; }
  
  /**Invoked from any FileRemote operation, to show the state.
   * 
   * @param stateM the state machine which can be triggered to run or influenced by a pause event.
   */
  public void show(FileRemote.CallbackCmd state, StateMachine stateM) {
    this.consumerAnswer = stateM;
    this.quest = state;
    addToList(mng, delay);  //Note: it does not add twice if it is added already.
  }
  
  
  public void XXXrequAnswer(FileRemote.CallbackCmd cmd, FileRemote.CmdEvent ev) {
    this.quest = cmd;
    this.eventAnswer = ev;
    addToList(mng, delay);
  }
  
  /**Invoked from any FileRemote operation, provides the state with requiring an answer.
   * The information in this instance should be filled actually.
   * @param cmd The quest
   * @param stateM instance which should be triggered to run on the answer.
   */
  public void requAnswer(FileRemote.CallbackCmd quest, StateMachine stateM) {
    this.quest = quest;
    this.consumerAnswer = stateM;
    addToList(mng, delay);   //to execute the request
  }
  
  
  /**An answer if somewhat is ask. 
   * This method should be called from the operator. */
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
  
  
  public void triggerStateMachine(FileRemote.Cmd cmd){
    if(consumerAnswer !=null) {
      this.cmd = cmd;
      consumerAnswer.triggerRun();
    }
  }
  
  
  
}
