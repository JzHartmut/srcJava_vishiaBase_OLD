package org.vishia.util;

import java.io.Closeable;

/**This class builds a Thread which runs a routine in a cycle.
 * The intension for an extra class is: On debugging, if a source is changed while running a program,
 * it can reloaded normally for example on eclipse working. But sometimes that is only possible if
 * the CPU does not run in the byteCode of that source. If the thread's loop is part of that source,
 * the class may not able to reload and the debugging is 'out of sync' with the sources.
 * This extra class enables the change of the working class because the CPU is on waiting in this class
 * usual, respectively a thread can hold here by setting a breakpoint. 
 * @author Hartmut Schorrig
 *
 */
public class ThreadRun implements Closeable
{
  /**Version, history and license.
   * <ul>
   * <li>2013-12-11 Hartmut: Created
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
  public static final int version = 0x20131211;

  
  private final Thread thread;
  
  /**Helper variable to stop one or more threads without changing the source.
   */
  protected boolean stopThread;

  
  /**If this variable is true, the thread runs. Set false on close(); */
  private boolean runThread;

  
  private boolean bThreadWaits;
  
  
  private boolean bWork;
  
  /**The cyclically wait. */
  private int waitMillisec = 100;
  
  protected int ctTimeoverflow;
  
  protected int stepTimeMeasure;
  
  protected float stepTimeMeasureMid;
  
  private final int cycletime;
  
  private int nextCycle;
  
  /**The step routine.
   * 
   */
  private final Step step;

  
  
  public ThreadRun(String name, Step step, int cycletime){
    thread = new Thread(run, name);
    this.step = step;
    this.cycletime = cycletime; 
    this.nextCycle = (int)(System.currentTimeMillis() + cycletime);
  }
  

  public void start(){
    thread.start();
  }
  
  
  /**Force the execution of step of this thread invoked from another thread.
   * If this routine is invoked from the own thread (in its step.run()-Routine),
   * then the next wait() is skipped, the step() routine is invoked without wait
   * for the next time. 
   * @param forceNextStep if it is in its step-routine, it does not wait after the current step
   *   but starts the next step immediately. If it is in its wait cycle, this value effects nothing.
   */
  public void forceStep(boolean forceNextStep){
    synchronized(this){
      bWork = forceNextStep;                  //if it is in step, it does not wait after the current step.
      if(bThreadWaits){ notify(); }  //wakes up.
    }
  }
  
  
  /**Closes this thread. It will end if the {@link #step}-routine will finished or if the thread waits yet.
   * If the thread waits it will be waken up to finish. 
   * If the step routine hangs or has its own timing, waits of anything other etc.
   * the close action is delayed. Not that a step routine should not hang in a proper application.  
   * @see java.io.Closeable#close()
   */
  @Override public void close(){
    runThread = false;
    synchronized(this){
      if(bThreadWaits){ notify(); }  //wakes up.
    }
  }
  
  
  
  /**The threads run routine.
   * @see java.lang.Runnable#run()
   */
  protected void run(){
    runThread = true;
    bWork = step.start();
    while(runThread){
      synchronized(this){
        if(cycletime > 0 && !bWork){ //NOTE: its possible that 
          nextCycle += cycletime;
          int shortTime = ((int)System.currentTimeMillis());
          waitMillisec = nextCycle - shortTime;
          if(waitMillisec > cycletime || waitMillisec < -cycletime){
            waitMillisec = cycletime;   //synchronize with a faulty cycletime, maybe on time error.
            nextCycle =  shortTime;
          } else if(waitMillisec <= 0) {
            waitMillisec = (cycletime / 16)+1;  //wait at least 1 ms. 
            ctTimeoverflow +=1;
            waitMillisec = cycletime;   //synchronize with a faulty cycletime, maybe on time error.
            nextCycle =  shortTime;
          } else {
            stepTimeMeasure = cycletime - waitMillisec;
            stepTimeMeasureMid += 0.01f * (stepTimeMeasure - stepTimeMeasureMid);
          }
          bThreadWaits = true;
          if(waitMillisec < 200){
            System.out.println("ThreadRun - less 200 ms;");
          }
          try{ wait(waitMillisec); } catch(InterruptedException exc){}
          bThreadWaits = false;
        } else {
          Assert.stop();
        }
      }
      if(runThread){
        try{
          bWork = step.step(cycletime);
        }catch(Throwable exc){
          System.err.println(Assert.exceptionInfo("InspcMng - unexpected Exception; ", exc, 0, 7));
          exc.printStackTrace(System.err);
        }

      }
    }
  }

 
  private final Runnable run = new Runnable(){
    /**The threads run routine.
     * @see java.lang.Thread#run()
     */
    @Override public void run(){ ThreadRun.this.run(); }
  };//run  
  
  public interface Step{
    
    public boolean start();
    
    public boolean step(int cycletime);
  }
  
  
}
