package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.util.StringPart;

/**This class organizes the execution of commands with thread-parallel getting of the process outputs.
 * @author Hartmut Schorrig
 *
 */
public class CmdExecuter implements Closeable
{
  /**Version and History:
   * <ul>
   * <li>2013-07-12 Hartmut new: {@link #execute(String[], boolean, String, List, List)} now with donotwait
   * <li>2013-07-12 Hartmut new: {@link #execute(String[], String, List, List)} for more as one output or error.
   *   It is nice to write a process output similar to the System.out and to a internal buffer while the process runs.
   *   Catching in a buffer and write to System.out after the process is finished is not fine if the process needs some time or hangs.
   * <li>2012-11-19 Hartmut bug: Command line arguments in "": now regarded in {@link #splitArgs(String)}.
   * <li>2012-02-02 Hartmut bug: Calling {@link #abortCmd()}: There was a situation were in {@link OutThread#run()} readline() hangs,
   *   though the {@link #process} was destroyed. It isn't solved yet. Test whether it may be better
   *   to read the InputStread direct without wrapping with an BufferedReader. 
   * <li>2011-12-31 Hartmut bugfix: The OutThread doesn't realize that the process was finished,
   *   because BufferedReader.ready() returns false and the buffer was not checked. So 'end of file'
   *   was not detected. Therefore {@link OutThread#bProcessIsRunning} set to false to abort waiting.
   * <li>2011-11-17 Hartmut new {@link #close()} to stop threads.
   * <li>2011-10-09 Hartmut chg: rename 'execWait' to {@link #execute(String, String, Appendable, Appendable)}
   *   because it has the capability of no-wait too.
   * <li>2011-10-09 Hartmut new: {@link #abortCmd()}. experience: On windows the output getting thread may block.   
   * <li>2011-10-08 chg the {@link #execute(String[], String, Appendable, Appendable)} waits until 
   *   all outputs are gotten in the {@link #outThread} and errThread. Extra class {@link OutThread}
   *   for both errThread and outThread.
   * <li>2011-10-02 chg some experiences: It needs parallel threads to capture the output. Extra threads
   *   for out and err, because a read() waits in the out-Buffer and blocks while an error-info is present etc.
   *   The outputs should presented while the process runs, not only if it is finished. It is because
   *   the user should be informed why a process blocks or waits for something etc. This fact is implemented already
   *   in {@link org.vishia.mainCmd.MainCmd#executeCmdLine(String[], ProcessBuilder, int, Appendable, String)}.
   * <li>TODO handling input pipe.
   * <li>TODO non waiting process.  
   * <li>older: TODO
   * </ul>
   */
  public static final int version = 0x20111002;

  
  /**Composite instance of the java.lang.ProcessBuilder. */
  private final ProcessBuilder processBuilder;

  /**The running process or null. Note that this class supports only running of one process at one time. */
  private Process process;
  
  /**True for ever so long this class is used, it maybe so long this application runs. */
  boolean bRunThreads;
  
  /**Both thread instances runs for ever so long bRunThreads is true.
   * They handle the output and error output if a process runs, and waits elsewhere. */
  private final OutThread outThread = new OutThread(), errThread = new OutThread();
  
  
  /**True if a process is started, false if it is finished. */
  //boolean bRunExec;
  
  //boolean bFinishedExec;

  //BufferedReader out1;
  //BufferedReader err1;
  //BufferedWriter processIn;

  //Appendable userOutput;
  //Appendable userError;

  final Thread threadExecOut;
  final Thread threadExecIn;
  final Thread threadExecError;
  
  String[] sConsoleInvocation;
  
  /**Constructs the class and starts the threads to getting output and error stream
   * and putting the input stream to a process. This three threads runs anytime unless the class
   * is garbaged or {@link #finalize()} is called manually.
   * <br><br>
   * Note: Starting of threads only if a command is executed results in a longer waiting time
   * for fast simple commands. The input and output putting/getting threads wait in a while-loop
   * until a command is executed. 
   * 
   */
  public CmdExecuter()
  { this.processBuilder = new ProcessBuilder("");
    threadExecOut = new Thread(outThread, "execOut");
    threadExecError = new Thread(errThread, "execError");
    threadExecIn = null; //TODO new Thread(inputThread, "execIn");
    bRunThreads = true;
    threadExecOut.start();
    threadExecError.start();
    //threadExecIn.start();
  }
  
  /**Sets the current directory for the next execution.
   * @param dir any directory in the users file system.
   */
  public void setCurrentDir(File dir)
  {
    processBuilder.directory(dir);
  }
  
  
  public Map<String,String> environment(){
    return processBuilder.environment();
  }
  
  
  public void setConsoleInvocation(String cmd){
    sConsoleInvocation = splitArgs(cmd);
  }
  
  
  /**Executes a command with arguments and waits for its finishing.
   * @param cmdLine The command and its arguments in one line. 
   *        To separate the command and its argument the method {@link #splitArgs(String)} is used.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output 
   */
  public int execute(String cmdLine
  , String input
  , Appendable output
  , Appendable error
  )
  { String[] cmdArgs = splitArgs(cmdLine);
    return execute(cmdArgs, input, output, error);
  }
  
  
  /**Executes a command with arguments and waits for its finishing.
   * @param cmdLine The command and its arguments in one line. 
   *        To separate the command and its argument the method {@link #splitArgs(String)} is used.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output 
   */
  public int execute(String cmdLine
  , String input
  , Appendable output
  , Appendable error
  , boolean useShell
  )
  { String[] cmdArgs = splitArgs(cmdLine);
    return execute(cmdArgs, input, output, error);
  }
  

  
  /**Executes a command with arguments and maybe waits for its finishing.
   * @param cmdArgs The command and its arguments. The command is cmdArgs[0]. 
   *        Any argument have to be given with one element of this String array.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   *        If output ==null then no output is expected and the end of command execution is not awaited.
   *        But in this case error should not be ==null because errors of command invocation are written there.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output 
   * @return exit code if output !=null. If output==null the end of command execution is not awaited.       
   */
  public int execute(String[] cmdArgs
  , String input
  , Appendable output
  , Appendable error
  )
  { boolean donotwait = true;
    List<Appendable> outputs, errors;
    if(output !=null){
      donotwait = false;
      outputs = new LinkedList<Appendable>();
      outputs.add(output);
    } else {
      outputs = null;
    }
    if(error !=null){
      donotwait = false;
      errors = new LinkedList<Appendable>();
      errors.add(error);
    } else {
      errors = null;
    }
    return execute(cmdArgs, donotwait, input, outputs, errors);
  }
  
  
  /**Same as {@link #execute(String[], String, Appendable, Appendable, boolean)}
   * but do not wait for finish if both outputs and errors are null.
   * @param cmdArgs
   * @param input
   * @param outputs
   * @param errors
   * @return
   */
  public int execute(String[] cmdArgs
      , String input
      , List<Appendable> outputs
      , List<Appendable> errors
      )
  { return execute(cmdArgs, outputs == null && errors == null, input, outputs, errors);
  }
  
  /**Executes a command with arguments and maybe waits for its finishing.
   * @param cmdArgs The command and its arguments. The command is cmdArgs[0]. 
   *        Any argument have to be given with one element of this String array.
   * @param donotwait true and all outputs, errors and input == null, then starts the process without wait.
   *   If one of input, outputs and errors is not null, the execution should wait in this thread because
   *   the threads for input, output and error should be running to capture that data. Then this flag is not used.       
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   *        If output ==null then no output is expected and the end of command execution is not awaited.
   *        But in this case error should not be ==null because errors of command invocation are written there.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output
   * @return exit code if it is waiting for execution, elsewhere 0.       
   */
  public int execute(String[] cmdArgs
  , boolean donotwait
  , String input
  , List<Appendable> outputs
  , List<Appendable> errors
  )
  { int exitCode;
    processBuilder.command(cmdArgs);
    if(errors == null){ //merge errors in the output stream. It is a feature of ProcessBuilder.
      processBuilder.redirectErrorStream(true); 
    }
    try
    {
      process = processBuilder.start();       //starts another process on operation system.
      //
      if(errors !=null){                     //it follows immediately: capture the error output from the process
        errThread.bProcessIsRunning = true;  //in the error thread.
        errThread.processOut = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        errThread.outs = errors;             
        synchronized(errThread){ errThread.notify(); }  //wake up to work!
        //processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      }
      if(outputs !=null){                    //it follows immediately: capture the output from the process
        outThread.bProcessIsRunning = true;  //in the output thread.
        outThread.processOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        outThread.outs = outputs;
        synchronized(outThread){ outThread.notify(); }  //wake up to work!
      }
      if(input !=null){
        //processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      }
      //
      //wait for
      if(input !=null || outputs !=null || errors !=null){
        exitCode = process.waitFor();  //wait for finishing the process
        //If the outThread or errThread will be attempt to wait, it realizes that the process has been finished. 
        outThread.bProcessIsRunning =false;
        errThread.bProcessIsRunning =false;
        //It is possible that the last output isn't gotten because the outThread or errThread
        //is not run in the last time. Run it till it has recognized the end of process itself.
        synchronized(outThread){
          if(outThread.processOut !=null){ //will be set to null on 'end of file' detection.
            outThread.wait();   //wait for finishing getting output. It will be notified if end of file is detected
          }
        }
        synchronized(errThread){
          if(errThread.processOut !=null){ //may be null if err isn't used, will be set to null on 'end of file' detection
            errThread.wait();   //wait for finishing getting error output. It will be notified if end of file is detected 
          }
        }
      } else if(donotwait){
        exitCode = 0; //don't wait
      } else {
        exitCode = process.waitFor(); //wait without input, outputs, errors
      }
      synchronized(this){
        process = null;  //no more used
      }
    } 
    catch(Exception exception) { 
      if(errors !=null){
        try{
          String sError = "CmdExecuter - Problem;" + exception.getMessage();
          for(Appendable error : errors){
            error.append( sError);
          }
        }
        catch(IOException exc){ throw new RuntimeException(exc); }
      } else {
        throw new RuntimeException(exception);
      }
      exitCode = -1; //Exception
    }
    return exitCode;
  }
  
  
  
  
  /**Aborts the running cmd. 
   * @return true if any cmd is aborted.
   */
  public boolean abortCmd()
  { boolean destroyed = false;
    synchronized(this){
      if(process !=null){
        process.destroy();
        destroyed = true;
      }
      try{ 
        //problem, if it is in readline(), then deadlock! because readkube and close() are synchronized.
        //how to abort the readline???
        //if(outThread.processOut !=null) { outThread.processOut.close(); } 
        //if(errThread.processOut !=null) { errThread.processOut.close(); } 
      } catch(Exception exc){ 
        System.err.println("error close");
        exc.printStackTrace();
      }
      outThread.outs = null;  //to abort
      errThread.outs = null;
    }
    //TODO doesn't work:
    /*
    if(outThread.processOut !=null){
      try{ outThread.processOut.close(); }
      catch(IOException exc){
        stop();
      }
    }
    if(errThread.processOut !=null){
      try{ errThread.processOut.close(); }
      catch(IOException exc){
        stop();
      }
    }
    */
    return destroyed;
  }
  
  
  
  /**Splits command line arguments.
   * The arguments can be separated with one or more as one spaces (typical for command lines)
   * or with white spaces. If arguments are quoted with "" it are taken as one argument.
   * The line can be a text with more as one line, for example one line per argument.
   * If a "##" is contained, the text until the end of line is ignored.
   * @param line The line or more as one line with arguments
   * @return All arguments written in one String per element.
   */
  public static String[] splitArgs(String line)
  {
    StringPart spLine = new StringPart(line);
    spLine.setIgnoreWhitespaces(true);
    spLine.setIgnoreEndlineComment("##");
    int ixArg = -1;
    int[] posArgs = new int[1000];  //only local, enought size
    int posArg = 0;
    while(spLine.length() >0){
      spLine.seekNoWhitespaceOrComments();
      posArg = (int)spLine.getCurrentPosition();
      int length;
      if(spLine.length() >0 && spLine.getCurrentChar()=='\"'){
        posArgs[++ixArg] = posArg+1;
        spLine.lentoQuotionEnd('\"', Integer.MAX_VALUE);
        length = spLine.length();
        if(length <=2){ //especially 0 if no end quotion found
          spLine.setLengthMax();
          length = spLine.length() - 1;  //without leading "
        } else {
          length -= 1;  //without leading ", the trailing " is not   
        }
      } else { //non quoted:
        posArgs[++ixArg] = posArg;
        spLine.lentoAnyCharOutsideQuotion(" \t\n\r", Integer.MAX_VALUE);
        spLine.len0end();
        length = spLine.length();
      }
      posArgs[++ixArg] = posArg + length;
      spLine.fromEnd();
    }
    String[] ret = new String[(ixArg+1)/2];
    ixArg = -1;
    for(int ixRet = 0; ixRet < ret.length; ++ixRet){
      ret[ixRet] = line.substring(posArgs[++ixArg], posArgs[++ixArg]);
    }
    return ret;
  }
  
  
  void stop(){};
  
  
  
  class OutThread implements Runnable
  {
    /**Output or ErrorOutput from process. */
    BufferedReader processOut;
    
    /**Output to write.*/
    List<Appendable> outs;
    
    char state = '.';
    
    /**Set to true before a process is started, set to false if the process has finished. */
    boolean bProcessIsRunning;
    
    @Override public void run()
    { state = 'r';
      while(bRunThreads){
        try{
          if(processOut !=null && outs !=null){  //ask only if processOutput is Set.
            String sLine;
            if(outs != null && processOut.ready()){
              if( (sLine= processOut.readLine()) !=null){
                for(Appendable out :outs){
                  out.append(sLine).append('\n');
                }
              } else {
                //Because processOut returns null, it is "end of file" for the output stream of the started process.
                //It means, the process is terminated now.
                processOut = null;  //Set to null because it will not be used up to now. Garbage.
              }
            } else {
              //yet no output available. The process may be run still, but doesn't produce output.
              //The process may be finished.
              if(!bProcessIsRunning){
                outs = null;  
              } else {
                Thread.sleep(100);
              }
            }
            
          } else {
            //no process is active, wait
            try { synchronized(this){ wait(1000); } } catch (InterruptedException exc) { }
          }
          if(outs == null && processOut !=null){  //aborted or process is finished.
            //if(process !=null){
              synchronized(this){ 
                processOut.close();
                processOut = null;
                notify();
              }  //notify it!  
            //}
          }
        } catch(Exception exc){
          
        }
      }
      state = 'x';
    }
  }
  
  

  class InThread implements Runnable
  { 
    boolean bRunExec = false;

    /**Output or ErrorOutput from process. */
    BufferedWriter processIn;
    
    /**Output to write.*/
    BufferedReader userInput;    /**Output or ErrorOutput from process. */
    
    
    
    @Override public void run()
    { while(bRunThreads){
        if(bRunExec){
          String sLine;
          boolean bFinished = true;
          try{
            if( (processIn.append("")) !=null){
              bFinished = false;
            }
          } catch(IOException exc){
            stop();
          }
          if(bFinished){
            bRunExec = false;
          }
          try { Thread.sleep(50); } catch (InterruptedException exc) { }
          process.destroy();
        } else {
          try { Thread.sleep(100); } catch (InterruptedException exc) { }
        }
        
      }
    }
  }
  
  
  /**Closes the functionality, finished the threads.
   * 
   */
  @Override public void close()
  {
    bRunThreads = false;
    while(  outThread !=null && outThread.state == 'r'
         || errThread !=null && errThread.state == 'r'
         //|| inThread !=null && inThread == 'r'
         ){
      synchronized(this){
        try{ wait(100); } catch(InterruptedException exc){}
      }
    }
  }
  
  
  /**Stops the threads to get output.
   * @see java.lang.Object#finalize()
   */
  @Override public void finalize()
  {
    bRunThreads = false;
  }
  
  
  /**A test main programm only for class test espesically in debug mode.
   * @param args not used.
   */
  public final static void main(String[] args)
  {
    CmdExecuter main = new CmdExecuter();
    main.execute("cmd /C", null, null, null);
    main.finalize();
  }
  

}
