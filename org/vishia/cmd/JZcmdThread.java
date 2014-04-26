package org.vishia.cmd;

import org.vishia.util.DataAccess;
import org.vishia.util.MessageQueue;
import org.vishia.util.StringFormatter;

public class JZcmdThread implements Runnable
{
  private JZcmdExecuter.ExecuteLevel executeLevel;

  private JZcmdScript.ThreadBlock statement;
  


  /**Exception text. If not null then an exception is thrown and maybe thrown for the next level.
   * This text can be gotten by the "error" variable.
   */
  DataAccess.Variable<Object> error = new DataAccess.Variable<Object>('S', "error", null);
  
  Throwable exception;
  
  /**State of thread execution. */
  char state = 'i';

  
  private MessageQueue<Object> msg1, cmd1;
  
  
  public JZcmdThread()
  {
  }

  protected void startThread(String name, JZcmdExecuter.ExecuteLevel startLevel
      , JZcmdScript.ThreadBlock statementArg) {
    this.executeLevel = startLevel;
    this.statement = statementArg;
    Thread threadmng = new Thread(this, name);
    threadmng.start();  

  }
  
  
  @Override public void run(){ 
    state = 'r';
    executeLevel.runThread(executeLevel, statement, this); 
    state = 'y';
    executeLevel.finishThread(this);
  }

  
  protected void clear(){
    state = 'i';
  }
  
  
  /**State of thread execution. 
   * <ul>
   * <li>i: init
   * <li>r: runs
   * <li>y: finished
   * </ul>
   */
  public char state(){ return state; }
  
  
  public void sendcmd(Object data){
    if(cmd1 == null){ cmd1 = new MessageQueue<Object>(); }
    cmd1.put(data);
  }
  
  public Object awaitcmd(int timeout){
    if(cmd1 == null){ cmd1 = new MessageQueue<Object>(); }
    return cmd1.await(timeout);
  }
  
  public void sendmsg(Object data){
    if(msg1 == null){ msg1 = new MessageQueue<Object>(); }
    msg1.put(data);
  }
  
  public Object awaitmsg(int timeout){
    if(msg1 == null){ msg1 = new MessageQueue<Object>(); }
    return msg1.await(timeout);
  }
  
  /**Waits for finishing the thread.
   * @param time timeout for waiting
   * @return true if the thread is finished, false on timeout.
   */
  public boolean join(int time){
    synchronized(this){
      if(state =='r'){
        try {
          wait(time);
        } catch (InterruptedException e) { }
      }
    }
    return state == 'y';
  }
  
}

  

