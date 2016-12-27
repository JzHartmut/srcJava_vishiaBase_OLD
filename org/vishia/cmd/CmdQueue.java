package org.vishia.cmd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.cmd.CmdStore.CmdBlock;
//import org.vishia.mainCmd.MainCmd_ifc;
//import org.vishia.mainCmd.Report;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;

/**This class stores some prepared commands for execution and executes it one after another.
 * The commands can contain placeholder for files. The commands may be operation system commands or {@link JZcmdExecuter} invocation of sub routines.
 * @author hartmut Schorrig
 *
 */
public class CmdQueue implements Closeable
{
  
  /**Version, history and license.
   * <ul>
   * <li>2016-09-18 Hartmut new: {@link #addCmd(CmdBlock, CmdGetterArguments)} new concept supports only {@link JZcmdExecuter}
   *   The {@link #addCmd(String, File, boolean)} and {@link #addCmd(String[], File, boolean)} is newly designed for simple operation system
   *   command execution without JZcmd concept. The {@link #addCmd(CmdBlock, File[], File)} which prepares file parts is designated as deprecated.
   *   Instead the JZcmdExecuter with more capability should be used. 
   * <li>2016-09-18 Hartmut chg: ctor with null as log, don't use log and JZcmd for simple applications. 
   * <li>2015-07-18 Hartmut chg: Now associated to an Appendable instead a PrintStream for error alive and error messages.
   *   A System.out is an Appendable too, for this kind the application is unchanged. Other output channels may support
   *   an Appendable rather than an PrintStream because it is more simple and substantial. 
   * <li>2013-09-08 Hartmut new: {@link #jzcmdExecuter} now included. TODO: it should use the {@link #executer}
   *   instead create a new one per call.
   * <li>2013-02-09 Hartmut chg: {@link #abortCmd()} now clears the queue too. The clearing of the command queue is a good idea, because while a program execution hangs, some unnecessary requests
   * may be initiated. 
   * <li>2013-02-03 Hartmut chg: better execution exception, uses log in {@link #execCmds(Appendable)}
   * <li>2013-02-03 Hartmut chg: {@link #execCmds(Appendable)} with only one parameter, the second one was unused.
   * <li>2011-12-03 Hartmut chg: The {@link #pendingCmds}-queue contains {@link PendingCmd} instead
   *   {@link CmdStore.CmdBlock} now. It is one level near on the really execution. A queued
   *   command may not stored in a CmdBlock. 
   * <li>2011-11-17 Hartmut new {@link #close()} to stop threads.
   * <li>2011-07-00 created
   * </ul>
   * 
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
   */
  public static final String version = "2016-12-27";
  
  private static class PendingCmd implements CmdGetFileArgs_ifc
  {
    //final CmdStore.CmdBlock cmdBlock;
    //public final List<PrepareCmd> listCmds;
    @Deprecated final PrepareCmd cmd;
    
    final String[] cmdAndArgs;
    
    final boolean bSilent;
    
    final JZcmdScript.Subroutine jbat;
    
    @Deprecated final File[] files;
    
    final Map<String, DataAccess.Variable<Object>> args;
    
    File currentDir;
    
    /**Constructs a cmd which is added to a queue to execute in another thread.
     * @param cmd The prepared cmd
     * @param files Some files which may be used by the command.
     *   Note that a reference to another {@link CmdGetFileArgs_ifc} can't be used
     *   if that selection is valid only on calling time. Therefore the yet selected files
     *   should be referenced.CmdGetFileArgs_ifc
     * @param currentDir
     */
    PendingCmd(PrepareCmd cmd, File[] files, File currentDir)
    { this.cmd = cmd;
      this.jbat = null;
      this.files = files;
      this.args = null;
      this.cmdAndArgs = null;
      this.currentDir = currentDir;
      bSilent = false;
    }

    /**Constructs a cmd which is added to a queue to execute in another thread.
     * @param cmd The prepared cmd
     * @param files Some files which may be used by the command.
     *   Note that a reference to another {@link CmdGetFileArgs_ifc} can't be used
     *   if that selection is valid only on calling time. Therefore the yet selected files
     *   should be referenced.CmdGetFileArgs_ifc
     * @param currentDir
     */
    PendingCmd(String[] cmd, File currentDir, boolean silent)
    { this.cmd = null;
      this.cmdAndArgs = cmd;
      this.jbat = null;
      this.files = null;
      this.args = null;
      this.currentDir = currentDir;
      this.bSilent = silent;
    }

    /**Constructs a cmd which is added to a queue to execute in another thread.
     * @param cmd The prepared cmd
     * @param files Some files which may be used by the command.
     *   Note that a reference to another {@link CmdGetFileArgs_ifc} can't be used
     *   if that selection is valid only on calling time. Therefore the yet selected files
     *   should be referenced.CmdGetFileArgs_ifc
     * @param currentDir
     */
    PendingCmd(JZcmdScript.Subroutine cmd, Map<String, DataAccess.Variable<Object>> args, File currentDir)
    { this.cmd = null;
      this.cmdAndArgs = null;
      this.jbat = cmd;
      this.files = null;
      this.args = args;
      this.currentDir = currentDir;
      bSilent = false; //not used.
    }

    @Override public void  prepareFileSelection()
    { }

    @Override public File getFileSelect()
    { return files[0];
    }
    
    @Override public File getFile1() { return files[0]; }
    
    @Override public File getFile2() { return files[1]; }
    
    @Override public File getFile3() { return files[2]; }

  }
  
  private final ConcurrentLinkedQueue<PendingCmd> pendingCmds = new ConcurrentLinkedQueue<PendingCmd>();
  
  private final CmdExecuter executer = new CmdExecuter();
  
  private final JZcmdExecuter jzcmdExecuter;
  
  //private final MainCmd_ifc mainCmd;

  //private final PrintStream log;
  private final Appendable log;
  
  private boolean busy;
  
  //private final ProcessBuilder processBuilder = new ProcessBuilder();
  
  private Appendable cmdOutput = System.out;
  
  private Appendable cmdError = System.err;
  
  /**Constructs an executer which can process {@link JZcmdExecuter} subroutines too.
   * @param log Log output used for {@link JZcmdExecuter}.
   */
  public CmdQueue(Appendable log)
  {
    this.log = log;
    MainCmdLoggingStream logMainCmd = new MainCmdLoggingStream("MMM-dd HH:mm:ss.SSS: ", log, MainCmdLogging_ifc.info);
    jzcmdExecuter = new JZcmdExecuter(logMainCmd);
  }
  

  /**Constructs an executer which can process {@link JZcmdExecuter} subroutines too.
   * @param log Log output used for {@link JZcmdExecuter}.
   */
  public CmdQueue()
  {
    this.log = null;
    jzcmdExecuter = null;
  }
  

  /**Sets the output and error stream destination
   * @param userOut Destination for output of all executed commands. 
   *   If null then any cmd invocation is done without awaiting the end of execution.
   *   The output and error output isn't used then.
   * @param userErr may be null, then the error is redirected to output
   */
  public void setOutput(Appendable userOut, Appendable userErr)
  {
    cmdOutput = userOut;
    cmdError = userErr !=null ? userErr : userOut; //maybe same as out
  }
  
  
  
  public void initExecuter(JZcmdScript script, String sCurrdir) throws Throwable{
    jzcmdExecuter.initialize(script, false, null, sCurrdir);
  }
  
  
  /**@deprecated it does nothing.
   * @param file
   */
  @Deprecated
  public void setWorkingDir(File file)
  { @SuppressWarnings("unused")
    final File dir;
    if(!file.isDirectory()){
      dir = file.getParentFile();
    } else {
      dir = file;
    }
    //processBuilder.directory(dir);
  }
  

  
  /**Adds a command to the queue to execute in {@link #execCmds(Appendable)}. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmd An operation system command as line with spaces to separate arguments. 
   * @param currentDir directory as current to execute the operation system command.
   * @param silent Execution without waiting and without output. The next command, if given, is started parallel with an own process.
   * @return Number of members in queue pending for execution. It is a hint whether the list maybe jam-packed because the execution hangs. 
   */
  public int addCmd(String cmd, File currentDir, boolean silent)
  {
    pendingCmds.add(new PendingCmd(cmd.split(" "), currentDir, silent));  //to execute.
    return pendingCmds.size();
  }

  
  /**Adds a command to the queue to execute in {@link #execCmds(Appendable)}. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmd An operation system command, cmd[0] is the command, cmd[1...] are arguments.
   * @param currentDir directory as current to execute the operation system command.
   * @param silent Execution without waiting and without output. The next command, if given, is started parallel with an own process.
   * @return Number of members in queue pending for execution. It is a hint whether the list maybe jam-packed because the execution hangs. 
   */
  public int addCmd(String[] cmd, File currentDir, boolean silent)
  {
    pendingCmds.add(new PendingCmd(cmd, currentDir, silent));  //to execute.
    return pendingCmds.size();
  }

  
  /**Adds a command to the queue to execute in {@link #execCmds(Appendable)}. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmdBlock The command block
   * @param args Some arguments especially for {@link JZcmdExecuter#execSub(org.vishia.cmd.JZcmdScript.Subroutine, Map, boolean, Appendable, File)}
   *   but maybe by a line command too.
   * @param currentDir directory as current to execute the operation system command.
   * @return Number of members in queue pending for execution. It is a hint whether the list maybe jam-packed because the execution hangs. 
   */
  public int addCmd(CmdBlock cmdBlock, Map<String, DataAccess.Variable<Object>> args, File currentDir)
  {
    pendingCmds.add(new PendingCmd(cmdBlock.getJZcmd(), args, currentDir));  //to execute.
    return pendingCmds.size();
  }

  
  /**Adds a command to the queue to execute in {@link #execCmds(Appendable)}. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmdBlock The command block
   * @param getterArguments Instance which implements getting arguments from any application. 
   * @param currentDir directory as current to execute the operation system command.
   * @return Number of members in queue pending for execution. It is a hint whether the list maybe jam-packed because the execution hangs. 
   */
  public int addCmd(CmdBlock cmdBlock, CmdGetterArguments getterArguments)
  {
    Map<String, DataAccess.Variable<Object>> args = getterArguments.getArguments(cmdBlock);
    File currDir = getterArguments.getCurrDir();
    pendingCmds.add(new PendingCmd(cmdBlock.getJZcmd(), args, currDir));  //to execute.
    return pendingCmds.size();
  }

  
  /**Adds a command to execute later. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmdBlock The command block
   * @param files Some files
   * @return Number of members in queue pending for execution.
   */
  @Deprecated public int addCmd(CmdBlock cmdBlock, File[] args, File currentDir)
  {
    for(PrepareCmd cmd: cmdBlock.getCmds()){
      pendingCmds.add(new PendingCmd(cmd, args, currentDir));  //to execute.
    }
    return pendingCmds.size();
  }

  
  /**Adds a command to the queue which should be executed in another thread.
   * @param sCmd The command maybe with <*file> etc. like described in {@link PrepareCmd}
   * @param files null or some files for execution
   * @param currentDir directory of execution
   * @param kindOfExecution See {@link PrepareCmd#PrepareCmd(char)}
   * @return number of pending commands.
   */
  @Deprecated public int addCmd(String sCmd, File[] files, File currentDir, char kindOfExecution)
  {
    PrepareCmd cmd = new PrepareCmd(kindOfExecution);
    cmd.set_cmd(sCmd);
    pendingCmds.add(new PendingCmd(cmd, files, currentDir));  //to execute.
    return pendingCmds.size();
    
  }
  
  
  
  /**Executes the pending commands.
   * This method should be called in a specified user thread. It returns if the command queue is empty.
   * It should be called cyclically or the thread should be woken up after a {@link #addCmd(CmdBlock, File[], File)}.
   * <br>
   * It checks {@link #pendingCmds}. An instance of a cmd is {@link PendingCmd}. It can contain either an operation system command entry
   * or an {@link JZcmdScript.Subroutine} start point for {@link #jzcmdExecuter}.
   * <br>
   * The command was added to the {@link #pendingCmds} either with {@link #addCmd(CmdBlock, File[], File)}, {@link #addCmd(CmdBlock, Map, File)}
   * or {@link #addCmd(String, File[], File, char)}. The {@link CmdBlock} can be created as 'possibility' for a command for example
   * to select it from a list or created on demand.  
   * @param outStatus A channel to write the execution status for example for a GUI-notification.
   *   A '\0' will be append to it if the queue is empty after execution.
   */
  public final void execCmds(Appendable outStatus)
  {
    PendingCmd cmd1;
    boolean someExecute = false;
    while( (cmd1 = pendingCmds.poll())!=null){
      busy = true;
      someExecute = true;
      try{
        StringBuilder sCmdShow = new StringBuilder(120);
        sCmdShow.append("CmdQueue - exec; ");
        if(cmd1.currentDir !=null){
          executer.setCurrentDir(cmd1.currentDir);
          sCmdShow.append(cmd1.currentDir).append(">");
        }
        if(cmd1.jbat !=null){
          if(outStatus !=null){ outStatus.append(cmd1.jbat.toString()); }
          jzcmdExecuter.execSub(cmd1.jbat, cmd1.args, false, cmdOutput, cmd1.currentDir);
        } else {
          //a operation system command:
          String[] sCmd;
          char kindOfExecution;
          if(cmd1.cmdAndArgs !=null) {
            sCmd = cmd1.cmdAndArgs;
            if(sCmd[0].charAt(0) == '&') {
              kindOfExecution = '&';
              sCmd[0] = sCmd[0].substring(1);
            } else {
              kindOfExecution = cmd1.bSilent ? '&' : '>';
            }
          } else {
            sCmd = cmd1.cmd.prepareCmd(cmd1);
            kindOfExecution = cmd1.cmd.getKindOfExecution();
          }
          for(String s1: sCmd){
            sCmdShow.append(s1).append(" ");
          }
          if(">%".indexOf(kindOfExecution) >=0){
            if(outStatus !=null){ outStatus.append(">" + sCmd[0]); }
            if(cmdOutput !=null){
              if(cmd1.currentDir !=null){ cmdOutput.append(cmd1.currentDir.getCanonicalPath()).append(">"); }
              else { cmdOutput.append("??>");}
              for(String s:sCmd){
                if(s !=null){ cmdOutput.append(s).append(" "); }
              }
              cmdOutput.append("\n");
            }
            if(log !=null) { log.append(sCmdShow).append('\n'); }
            //mainCmd.writeInfoln(sCmdShow.toString());
            
            int exitCode = executer.execute(sCmd, null, cmdOutput, cmdError);
            if(exitCode == 0){ cmdOutput.append("\nJavaCmd: cmd execution successfull\n"); }
            else {cmdOutput.append("\nJavaCmd: cmd execution errorlevel = " + exitCode + "\n"); }
          } else if(kindOfExecution == '&'){
            if(log !=null) { log.append(sCmdShow).append('\n'); }
            //mainCmd.writeInfoln(sCmdShow.toString());
            if(outStatus !=null){ outStatus.append("&" + sCmd[0]); }
            try{
              executer.execute(sCmd, true, null, null, null, null);  //don't wait for execution.
            } catch(Exception exc){
              if(log !=null) { log.append("\nCmdQueue - execution exception; " + exc.getMessage()).append('\n'); }
              else { System.err.println("\nCmdQueue - execution exception; " + exc.getMessage()); }
            }
            //cmdOutput.append("JavaCmd; started; " + sCmd + "\n");
          } else {
            //mainCmd.writeInfoln("CmdQueue - unexpected kind of execution; " + kindOfExecution);
            if(log !=null) { log.append("\nCmdQueue - unexpected kind of execution; " + kindOfExecution).append('\n'); }
            else { System.err.println("\nCmdQueue - unexpected kind of execution; " + kindOfExecution);}
          }
        }
      } catch(Throwable exc){ 
        CharSequence msg = Assert.exceptionInfo("CmdQueue - exception,", exc, 0, 20);
        try{ cmdError.append(msg); 
        } catch(IOException exc1){ System.err.append(msg); }
      }
    }
    busy = false;
    if(someExecute && outStatus !=null){
      //clean status line after execution.
      try{ outStatus.append('\0');} catch(IOException exc){}
    }
  }


  /**Aborts a running cmd and clears the queue. 
   * The clearing of the command queue is a good idea, because while a program execution hangs, some unnecessary requests
   * may be initiated. Therefore all is cleared.
   * @return true if any cmd was aborted.
   */
  public boolean abortCmd()
  { pendingCmds.clear();
    boolean bAborted = false;
    if(executer.abortCmd()) bAborted = true;
    if(jzcmdExecuter.abortCmdExecution()) bAborted = true;
    return bAborted;
  }
  
  
  /**Returns true if the execution is running. 
   * Does not return true if any command was stored but the execution with {@link #execCmds(Appendable)} is not started.
   * @return
   */
  public boolean isBusy(){ return busy; }
  
  
  @Override public void close(){
    executer.close();
  }

}
