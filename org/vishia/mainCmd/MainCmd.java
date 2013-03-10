/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2009-12-15: Hartmut chg: public of some variables for report because a using class has 2 instances, correct it later.
 * 2009-03-08: Hartmut new: openReportfile() as new protected method, used internally, but the report file is able to change.
 * 2009-03-08: Hartmut new: executeCmdLine() is now deprecated, use new executeCmdLine(..., ProcessBuilder,..).  
 * 2008-04-02: JcHartmut some changes
 * 2006-05-00: JcHartmut www.vishia.de creation
 *
 ****************************************************************************/

package org.vishia.mainCmd;

//import org.jdom;
import java.io.*;
import java.util.*;  //List
import java.text.*;  //ParseException

import org.vishia.util.FileSystem;
import org.vishia.util.StringPart;
import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.VaArgBuffer;
import org.vishia.bridgeC.Va_list;
import org.vishia.cmd.CmdExecuter;
import org.vishia.msgDispatch.LogMessage;


/**This is an abstract superclass to support cmd line applications. The class contains the following features:
    <ul><li>Support parsing of command line arguments. </li>
        <li>Management of the system output</li>
        <li>Provides an interface for reporting to a file.</li>
        <li>Gathering of the maximum of the exitErrorLevel, see setExitErrorLevel().</li>
        <li>Provides a method to execute external cmd lines in a simple way for the user, see executeCmdLine()</li>
        <li>store information about the application see {@link #writeAboutInfo()} and {@link #writeHelpInfo()}.
    </ul>
    You can find an application example in {@link SampleCmdLine}.
 * <br>
 * <br>
 * <hr>
 * <b>Output to console or to the command line pipes or?</b>:<br>
 * There are some approaches for outputs from an application:
 * <ul>
 * <li>Simple command line invocation: The output is expected on the console respectively shell or cmd window. 
 *   This kind of output is possible on Graphical Applications too if the application was started in a command line window.
 * <li>Command line invocation with redirected output: One can write <code>cmd args >myOutput</code> or one can use a pipe
 *   construction like <code>cmd args | cmd2 args</code>. In that cases the outputs written with {@link java.lang.System#out}
 *   are written to the given channel (usual a file) respectively they are stored in a pipe to provide the input for the
 *   next command. In this cases usual the standard output {@link java.lang.System#out} is redirected but not the error output.
 * <li>Graphical Applications: The graphical application may have an output window which can present the System.out.
 * </ul>
 * What is need? It is nice to have some information while running an application and while occurring errors. But sometime
 * detail information are need, and sometime the flood of information is not desired.
 * <br>
 * <b>The report approach: </b>: Therefore since the first version of this class a output dispatching system are implemented with the routines
 * {@link #report(int, String)} etc.
 * <ul>
 * <li>For usual output requests the routine {@link #report(int, String)} or {@link #reportln(int, String)} can be used
 *   from an application instead <code>System.out.println(....)</code>.
 *   The first parameter controls whether the output is written to console channel or to the report file. With the
 *   command line arguments <code>--report=FILE</code> and <code>--rlevel=WDR</code> parsed as standard arguments
 *   the usage of levels and the file is controlled without additional programming effort. 
 *   With that approach a output file 'report file' is created anyway in a command line application. The report file 
 *   contains the same or more information than the console output. A command line application doesn't require the
 *   redirection in a file to get its output persistently. A persistent output is available anyway with the report file.  
 * <li>The routines {@link #writeInfo(String)}, {@link #writeWarning(String)} and {@link #writeError(String)} can be used
 *   to output some things as information, warning or error in the application. The output is categorized to one of this
 *   three levels.
 *   This information is written to the console output anyway.
 *   It depends on the 3. position 'W' of the <code>--relevl=WDR</code> whether this output is written in the report file.
 * <li>To output an exception message one can use {@link #writeError(String, Exception)}.  
 * </ul>
 * This is the older system to output something. The interface {@link MainCmdLogging_ifc} (older version {@link Report})
 * supports this system. 
 * <br><br>
 * The problem of the older output management system is: A simple application can distinguish between the 6 levels 
 * {@link MainCmdLogging_ifc#error} to {@link MainCmdLogging_ifc#fineDebug}. But in complex software a module may output
 * something independent of the necessity of output of information of another module. 
 * <br><br>
 * The second problem is: The modules which should output something may not know or use the {@link MainCmdLogging_ifc} 
 * because it may be realized as a special interface.
 * <br>
 * <br>
 * <b>The LogMessage interface or MsgDispatcher</b>: From some embedded applications a message dispatching system was created. This system is implemented in C-language
 * applications too. That system uses Numbers for any output information. The output text can be determined with the 
 * identification number of a output message in different languages. Such capability is done by special software. 
 * The messages can be stored in a simple way in log archives or alarm state storages using only the number and additional
 * measurement values without the message text.
 * <br><br>
 * This message output system with its identification number for any message can be used to dispatch any information
 * to several output channels by using its number. With them any module can output its stuff with a number range. 
 * Whether it is output to display, to a log file or the output is suppressed, it can be controlled by the dispatching
 * control file. That control file associates numbers or number ranges to output channels, 
 * see {@link org.vishia.msgDispatch.MsgDispatcher#setOutputRange(int, int, int, int, int)}
 * <br><br>
 * The interface for this dispatcher is given with the {@link LogMessage} interface. The MainCmd 
 * implements the {@link LogMessage} interface. It is a super interface of {@link MainCmdLogging_ifc}.
 * The implementation of the {@link LogMessage} in MainCmd can redirect the message to the message dispatcher or any
 * other implementor given with {@link #setLogMessageDestination(LogMessage)}. The default behaviour is writing
 * the message via {@link #reportln(int, String)} to console and the report file. Wherby the ident number is taken from
 * the message indent number if it is 1..6 ({@link MainCmdLogging_ifc#error} to {@link MainCmdLogging_ifc#fineDebug}).
 * All other ident numbers are written with <code>reportln({@link MainCmdLogging_ifc#info}, line)</code>. 
 * The line contains the message ident number and the time of creation of the message.
 * That is the simple adaption from the {@link LogMessage} approach without using of a message dispatching system.
 * <br><br>
 * Without change in the user's software the {@link org.vishia.msgDispatch.MsgDispatcher} can be established. One can
 * invoke {@link #setLogMessageDestination(LogMessage)} and use the channels {@link #getLogMessageErrorConsole()},
 * {@link #getLogMessageOutputConsole()} and {@link #getLogMessageOutputFile()} from this class as destination channels
 * for the MessageDispatcher. 
 * <br>
 * <br>
 * <b>Message dispatching without numbers</b>: The problem of the numbers is: Any log output or message should have
 * a unified number. The programmer should handle this numbers in a global way. That number actuation is a pain in the neck
 * if the programmer does not need the numbers at that time.
 * <br><br>
 * To simple to way of life the {@link org.vishia.msgDispatch.MsgPrintStream} can be used to create numbers automatically.
 * With them the simple <code>System.out.printf("message text\n", parameter)</code> is converted to a {@link LogMessage} invocation.
 * The ident numbers of the messages are created automatically in a given number range or it can be set by a control String.
 * <br><br>
 * With them a simple <code>System.out.printf("message text\n", parameter)</code> is sufficient to output something 
 * and to work with the message dispatching and output management system. The dispatching is support with 
 * <br>
 * <br>
 * <b>Conversion from the report output to message outputs</b>: See {@link ReportWrapperLog}. This is the adaption from
 * the older report approach to the message system. 
 * <br><br>
 * <b>What should the user do</b>:
 * <ol>
 * <li>If the user is intent to work with the MainCmd class, one should use the {@link MainCmd_ifc} or its super interface,
 *   the {@link MainCmdLogging_ifc}. One can use both, the {@link MainCmd_ifc#report(int, String)} invocation and the 
 *   {@link LogMessage#sendMsg(int, String, Object...)} approach.
 * <li>If the user should only work with the Message Dispatcher System independent of this MainCmd class, 
 *   one should associate this class or the {@link org.vishia.msgDispatch.MsgDispatcher} class as {@code LogMessage} reference. 
 *   One should organize and use the message ident numbers. 
 * <li>As a independent common approach the user can use the <code>System.out.print...</code>. Therewith independent
 *   sources from any vishia classes can be written. Using a format <br>
 *   <code>System.out.printf("Module - message; text %d %f\n", value1, value2);</code><br>
 *   is a proper format independent of any vishia approaches. That system matches to the redirection and conversion
 *   to the vishia message system with output channels of this class.
 * </ul>   
 * It seems to be a good choice to use the second approach, {@link LogMessage} if the software is specialized for
 * message output, and to use the third approach, System.out for any other independent modules. The organization of both
 * approaches to use the message system can be done at a centralized point in software. The example {@link SampleCmdLine}
 * presents example outputs with that approaches.    
 * <hr>                  
 * <br><br>
    <b>Extension to GUI application</b>:<br>
    This class is extended for example by {@link org.vishia.gral.area9.GralArea9MainCmd} to a class which implements
    the outputs on a GUI window. The methods supporting command line things
    are adapted there for a GUI (Graphical User Interface) -style application. In the GUI frame the user used the same
    interface MainCmd_Ifc to do something with the applications frame.
    <br><br>
    It is an old documentation:    
    The user should write his application class in the followed way:<br/>
    <pre>
    ---|> means an inherition
    <>--> means a composition or aggregation
    ----> means a association


    +-------------+        +-------------+        +-------------+
    |             |------|>| MainCmd     |------|>| MainCmd_Ifc |
    |UserMainClass|        +-------------+        +-------------+
    |             |                                      ^
    |             |        +-------------+    main       |
    |             |<>----->| UserClasses |---------------+
    +-------------+        +-------------+

    in cmdlines:

    class UserMainClass extends MainCmd
    { final UserClasses userClasses = new UserClasses(this);  //... the user's classes
    }

    class UserClasses
    { final MainCmd_Ifc main;        //the aggregation to Main
      UserClasses(MainCmd_Ifc main)
      { this.main = main;
      }
    }
    </pre>
    The UserMainClass based on MainCmd as superclass, MainCmd implements MainCmd_Ifc.
    All composite classes from UserMainclass may known the MainCmd instance respective the UserMainClass instance
    via the interface MainCmd_Ifc.
    With this interface the user can access at all capabilities of support of command line things,
    implemented cardinally in MainCmd, but overwriteable from the user in his UserMainClass.<br/>
    The user should realized the evaluation of the command line arguments, help messages for command line etc. in
    the UserMainClass. But the functionality of the user's application should be realized not in the UserMainClass,
    but in the UserClass and the underlying classes. The functionality should be separated from the command line invoke,
    because this application's functionality can be invoked also from a comprising application, at example by a GUI
    (Graphical Users Interface).
 */

public abstract class MainCmd implements MainCmd_ifc
{

  /**Version, able to read as hex yyyymmdd.
   * Changes:
   * <ul>
   * <li>2013-03-10 Hartmut chg Some adjustments in {@link #addArgument(Argument[])},especially old: setArguments(..)
   *   is renamed to addArgument(). It is prepared that an inherited class's main(...) can add some more arguments
   *   than the base class. 
   * <li>2013-02-24 Hartmut improved {@link #getLogMessageOutputConsole()} etc.
   * <li>2013-02-09 Hartmut new {@link #addArgument(Argument[])} as new method to bundle argument query and help text.
   * <li>2012-03-30 Hartmut new {@link #getLogMessageErrorConsole()}
   * <li>2011-10-11 Hartmut new {@link #setOutputChannels(Appendable, Appendable)}. All outputs are redirect-able now.
   *   Used for output in a graphical text box.
   * <li>2011-07-10 JcHartmut bugfix: The method {@link #executeCmdLine(ProcessBuilder, String, String, int, Appendable, Appendable)}
   *   has produced a problem because 2 spaces are given in the args instead of one. There was an empty argument therefore,
   *   which has had a negative effect to a called command (it was "bzr add", an empty argument forces addition of all files
   *   though the next arguments had contain some named files). The problem is solved with using a more complex algorithm
   *   to split arguments as a simple String.split(" "). See {@link #splitArgs(String)}.  
   * <li>2007..2011 JcHartmut Some changes
   * <li>2007-03-07 JcHartmut  Method getReportFileName
   * <li>2007-03-07 JcHartmut  callWithoutArgument do not exit but throws an exception likewise by an error of arguments.
   * <li>2006-04-02 JcHartmut  writeInfo__ new methods write__Directly, solution of concurrency to write to report.
   * <li>2006-01-17 JcHartmut  testArgument() may thrown an ParseException.
   * <li>2006-01-21 JcHartmut  bugfix callWithoutArguments() should be protected, not package private.
   * <li>2004-06-00 JcHartmut  initial revision
   * </ul>
   */
  public static int version = 0x20130310;
  
  /**Interface for anonymous implementation of setting arguments.
   * The implementation should be written in the simple form:
   * <pre>
   * MainCmd.SetArgument setArgxy = new MainCmd.SetArgument(){
   *   @Override public boolean setArgument(String val){
   *     args.argxy = val;
   *     return true;
   * } }
   * <pre>
   * The implementation should be an instance inside the user's class. This example shows an argument instance
   * named 'args' where the values of the given arguments are stored.
   * <br><br>
   * The implementation method can test the admissibility of the argument's value. It can return false
   * to designate that the value is not valid. For example the existence of a file can be checked.
   */
  public interface SetArgument{ 
    boolean setArgument(String val); 
  }
  
  /**Class to describe one argument. One can be create static instances with constant content. Example:
   * <pre>
   * Argument[] argList =
   * { new Argument("-arg", "short one-line help text", setmethod)
   * , new Argument("-x", "the help text", setx)
   * };
   * </pre>
   *
   */
  public static class Argument{ 
    final String arg; 
    final String help; 
    final SetArgument set;
    
    public Argument(String arg, String help, SetArgument set){
      this.arg = arg; 
      this.help = help; 
      this.set = set;
    }
  }
  
  
  
  protected final List<MainCmd.Argument> argList = new ArrayList<MainCmd.Argument>();
  
  /** The report file. This attribute is set to null, if no report is desired by missing the argument -r REPORTFILE
  */
  public FileWrite fReport;

  /** All reports with a level less than or equal this level will be reported. Other report calls has non effect.*/
  public int nReportLevel = Report.info;  //default: reports errors, warnings and info, but no debug

  private String sFileReport = "report.txt";

  /** All reports with a level less than or equal this level will be written on display.
   *  Note: using {@link Report#writeInfo(String)} etc. writes to display also if this attribute is 0.
   */
  public int nReportLevelDisplay = 0;   //default: don't write reports to display

  /** writeError(), writeWarning(), writeInfo()  are also reported if the level is equal or greater than 1,2,3*/
  public int nLevelDisplayToReport = Report.info;

  /** List of strings contents help Info in form of lines. The list should be filled with method addHelpInfo(String);*/
  public List<String> listHelpInfo = new LinkedList<String>();

  /** List of strings contents about Info in form of lines. The list should be filled with method addHelpInfo(String);*/
  public List<String> listAboutInfo = new LinkedList<String>();

  /** The maximum of the value given with setExitErrorLevel */
  private int nMaxErrorLevel = MainCmd_ifc.exitSuccessfull;


  /** array of arguments from command line*/
  protected String[] cmdLineArgs;

  /** number of the argument is parsing*/
  private int iArgs;

  /** access to some spaces via Report.spaces.substring(0,n)*/
  private static final String report_spaces = "                                                                                ";

  
  private LogMessage redirectLogMessage;
  
  /**Preserve the original System.out and System.err on construction. It is the cmdline output and error output
   * of the commmand line invocation. */
  public final PrintStream outCmdline, errCmdline;
  
  /**Channels for output and error output of the main program. */
  private Appendable outConsole,errConsole;
  

  protected SimpleDateFormat dateFormatMsg = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");

  



  /** Empty Constructor of the main class. 
   * Note: One can use this constructor and {@link #setArguments(Argument[])} and {@link #parseArguments(String[])}
   * instead of {@link #MainCmd(Argument[], String[])}.
   * <br><br>
   * On construction the referenced Printstreams of {@link java.lang.System#out} and {@link java.lang.System#err}
   * are preserved inside this class in {@link #outCmdline} and {@link #errCmdline}. 
   * One can redirect {@link java.lang.System#setOut(PrintStream)} and ...setErr(...) without changing of the MainCmd
   * outputs after construction of this. Especially the System.out and System.err can be redirected to the Message system
   * using {@link org.vishia.msgDispatch.MsgPrintStream} and {@link org.vishia.msgDispatch.MsgDispatcher} with usage of
   * the console outputs preserved in this class using {@link #getLogMessageErrorConsole()}, {@link #getLogMessageOutputConsole()}
   * and {@link #getLogMessageOutputFile()} of this class.
   * 
   */
  protected MainCmd()
  { outConsole = outCmdline = System.out;
    errConsole = errCmdline = System.err;
    this.redirectLogMessage = logMessageImplReport;
  }

  /** Constructor of the main class.
  */
  protected MainCmd(String[] args)
  { this();
    this.cmdLineArgs = args;
  }


  protected MainCmd(Argument[] argList)
  { outConsole = outCmdline = System.out;
    errConsole = errCmdline = System.err;
    this.redirectLogMessage = logMessageImplReport;
    for(Argument arg: argList){
      this.argList.add(arg);
    }
  }

  /** Constructor of the main class.
  */
  protected MainCmd(Argument[] argList, String[] args)
  { this(argList);
    this.cmdLineArgs = args;
  }

  
  
  
  /**Redirects the LogMessage implementation of this class to the given implementor.
   * Usual it may be the {@link org.vishia.msgDispatch.MsgDispatcher}. By default the logmessage capability
   * of this class is implemented in that kind that all messages with the ident numbers from 1 to 6 are output
   * in the same form as {@link #reportln(int, String)} and all other ident numbers are written with
   * <code>reportln({@link MainCmdLogging_ifc#info}, messageString)</code>. The message dispatcher can be help
   * to select the output channels of the messages.
   * @param log
   */
  public void setLogMessageDestination(LogMessage log){
    redirectLogMessage = log;
  }
  
  
  
  /**Sets the argument list for this application 
   * and adds its help info. This method should be called in order of {@link #addHelpInfo(String)}
   * to the correct sequence.
   * @param list see {@link Argument}
   */
  public void addArgument(Argument[] list){ 
    for(Argument arg: list){
      this.argList.add(arg);
      listHelpInfo.add(arg.arg + arg.help);
      
    }
  }

  /** Adds the help info for standard arguments. The text is the followed:<pre>
    addHelpInfo("--about show the help infos");
    addHelpInfo("--help  show the help infos");
    addHelpInfo("--report=FILE  write the report (log) into the given file, create or clear the file.");
    addHelpInfo("--report+=FILE add to the end of given file or create the report file.");
    addHelpInfo("--rlevel=R     set the level of report, R is number from 1 to 6.");
    addHelpInfo("--rlevel=DR    also write reports upto level D on display, sample: ..-rlevel:24");
    addHelpInfo("--rlevel=WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
    </pre>
  */
  protected void addStandardHelpInfo()
  {
    addHelpInfo("--about show the help infos");
    addHelpInfo("--help  show the help infos");
    addHelpInfo("---arg ignore this argument");
    addHelpInfo("--@file use file for further arguments, one argument per line.");
    addHelpInfo("--report=FILE  write the report (log) into the given file, create or clear the file.");
    addHelpInfo("--report+=FILE add to the end of given file or create the report file.");
    addHelpInfo("--rlevel=R     set the level of report, R is number from 1 to 6.");
    addHelpInfo("--rlevel=DR    also write reports upto level D on display, sample: ..-rlevel:24");
    addHelpInfo("--rlevel=WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
  }





  /** Addes a helpinfo-line to the internal list. This method should be called from the user on startup to set the
      help infos. The help info is used by <a href="#writeHelpInfo()"><code>writeHelpInfo()</code>
      called if the argument --help is given or without arguments. </a>.
      @param info String contains 1 line of a help Info.
  */
  protected void addHelpInfo(String info)
  { listHelpInfo.add(info);
  }

  /** Addes a helpinfo-line to the internal list. This method should be called from the user on startup to set the
  help infos. The help info is used by evaluating command line arguments.
  @param info String contains 1 line of a help Info.
  */
  protected void addAboutInfo(String info)
  { listAboutInfo.add(info);
  }

  
  
  

  /** prints the help info to the console output. This Method may be overloaded in MainCmdWin to show the contents in a window.
  */
  public void writeHelpInfo()
  { writeAboutInfo();
    Iterator<String> iHelpInfo = listHelpInfo.iterator();
    while(iHelpInfo.hasNext())
    {
      System.out.println((iHelpInfo.next()));
    }
  }



  /** prints the help info to the console output. This Method may be overloaded in MainCmdWin to show the contents in a window.
  */
  public void writeAboutInfo()
  {
    Iterator<String> iInfo = listAboutInfo.iterator();
    while(iInfo.hasNext())
    {
      System.out.println((iInfo.next()));
    }
  }


  /**Parses the cmdLine-arguments. The args should be set in constructor.
   * @see parseArguments(String [] args)
   * 
   * @throws ParseException
   */
  public final void parseArguments()
  throws ParseException
  {
    parseArguments(cmdLineArgs);
  }

  /**Parses the cmdLine-arguments. The user should invoked this method in his static main method
   * with the following pattern:
   * <pre>
  public static void main(String[] args){
    Args argData = new Args();
    Cmdline cmd = new Cmdline(argData);
    try{ cmd.parseArguments(args);
    } catch(ParseException exc){
      cmd.setExitErrorLevel(MainCmd.exitWithArgumentError);
      cmd.exit();
    }
    UserClass main = new UserClass(argData.x, argData.y);
    main.exec(argData.z);
  }
   * </pre>
   * Within this pattern the parsing of the arguments is independent of the user class. 
   * It may be an advantage of architecture of the user class. Some argument values
   * can be used as initial values on construction of the user class. They can be stored there in 'final' Associations. 
   * <br><br>
   * In that pattern the Argument values are stored in an independent class Args, which is an independent mediator
   * between the MainCmd and the UserClass.
   * <br><br>
   * One can use the {@link #setArguments(Argument[])} to define which arguments are used in which kind.
   * In the older form (till 2012), the user was requested to override this method.
   * <br><br>
   * <b>Standard arguments</b>
   * See {@link #addStandardHelpInfo()}.
   * <ul>
   * <li>--about show the help infos
   * <li>--help  show the help infos");
   * <li>--report=FILE  write the report (log) into the given file, create or clear the file.");
   * <li>--report+=FILE add to the end of given file or create the report file.");
   * <li>--rlevel=R     set the level of report, R is number from 1 to 6.");
   * <li>--rlevel=DR    also write reports upto level D on display, sample: ..-rlevel:24");
   * <li>--rlevel=WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
   * <li>--@ARGFiLE     The ARGFILE contains one argument per line. Especially it is possible to use spaces 
   *   in the argument values without "".
   *   For example a cmd line invocation <code>>cmd -a arg1 --@file -x argx</code> results in the followed arguments:<br/>
   *  <code>-a arg1 arguments-from-file-lines -x argx</code>
   * </ul>
   * <br><br>
   * <b>What does this method</b>:<br>
   * On every not-standard argument this method calles {@link #testArgument(String, int)} inside.
   * That method checks any argument, see there. If that method returns false,
   * a {@link #writeError(String)} with a matched text is called and a ParseException("", N) ist thrown, N is the
   * number of the argument.
   * Standard arguments are parsed here, see <a href="#addStandardHelpInfo()"><code> addStandardHelpInfo()</code></a>.
   * On end the methode <code>checkArguments()</code> is called.
   * If it returns false, an ParseException("", 0) is thrown.
   * The user should had write an output message with writeError() inside its <code>checkArguments()</code><br/>
   */
  public final void parseArguments(String [] args)
  throws ParseException
  { cmdLineArgs = args;
    MainCmd main = this;
        if(cmdLineArgs.length == 0)
    { //call without parameters:
      callWithoutArguments();
    }
    boolean bAppendReport = false;
    { iArgs = 0;
      while(iArgs < cmdLineArgs.length)
      { if(cmdLineArgs[iArgs].startsWith("--@")){
          String sArgFile = getArgument(3);
          File fileArg = new File(sArgFile);
          List<String> listArgs = new LinkedList<String>();
          try{
            int lenFile = (int)fileArg.length();
            BufferedReader inp = new BufferedReader(new FileReader(fileArg));
            while(inp.ready()){
              String sParam = inp.readLine().trim();  
              if(sParam.length()>0 && sParam.charAt(0) != '#'){
                listArgs.add(sParam);
              }  
            }
            //add existing cmdLineArgs and the new args:
            String[] argsNew = new String[cmdLineArgs.length -1 + listArgs.size()];
            System.arraycopy(cmdLineArgs, 0, argsNew, 0, iArgs);
            int iArgNew = iArgs-1;
            for(String arg: listArgs){
              argsNew[++iArgNew] = arg;
            }
            System.arraycopy(cmdLineArgs, iArgs+1, argsNew, iArgNew+1, cmdLineArgs.length - iArgs-1);
            //now all args from file and the existing cmdLineArgs are stored in argsNew, use it instead:
            cmdLineArgs = argsNew;
            iArgs -=1;  //the --@ is removed and replaced with the first argument from file!
          } 
          catch(FileNotFoundException exc){ throw new ParseException("argfile not found: " + fileArg.getAbsolutePath(),0); }
          catch(IOException exc){ throw new ParseException("argfile read error: " + fileArg.getAbsolutePath(),0); }
          
        }
        else if (cmdLineArgs[iArgs].startsWith("--rlevel"))
        { int level;
          try{ level = Integer.parseInt(getArgument(9)); } //;cmdLineArgs[iArgs].substring(7,8)); }
          catch(Exception e)
          { throw new ParseException("ERROR on argument --rlevel=" + cmdLineArgs[iArgs], iArgs);
          }
          main.nReportLevel          = level % 10;
          main.nReportLevelDisplay   = (level / 10) % 10;  //0: no report on display.
          main.nLevelDisplayToReport = (level / 100) % 10;  //0: no display to report.
        }
        else if(cmdLineArgs[iArgs].startsWith("--report+=")) { sFileReport = getArgument(10); bAppendReport = true; }
        else if(cmdLineArgs[iArgs].startsWith("--report:")) { sFileReport = getArgument(9); }
        else if(cmdLineArgs[iArgs].startsWith("--report=")) { sFileReport = getArgument(9); }
        else if(cmdLineArgs[iArgs].startsWith("--about")) { writeAboutInfo(); }
        else if(cmdLineArgs[iArgs].startsWith("--help")) { writeHelpInfo(); }
        else if(cmdLineArgs[iArgs].startsWith("---")) { /*ignore it*/ }
        else
        { if(!main.testArgument(cmdLineArgs[iArgs], iArgs))
          { main.writeError("failed argument:" + cmdLineArgs[iArgs]);
            throw new ParseException("failed argument:" + cmdLineArgs[iArgs], iArgs);  //ParseException used from java.text
          }
        }
        iArgs +=1;
      }
    }
    //debug System.out.println("report level=" + main.nReportLevel + " file:" + sFileReport);
    /** open reportfile: */
    if(nReportLevel > 0)
    { try{ openReportfile(sFileReport, bAppendReport); }
      catch(FileNotFoundException exc)
      { writeError("ERROR creating reportfile-path: " +sFileReport);
        throw new ParseException("ERROR creating reportfile-path: " +sFileReport, 0);
      /*if a requested reportfile is not createable, the programm can't be run. The normal problem reporting fails.
        That's why the program is aborted here.
      */
      }
    }
    if(!checkArguments())
    {
      throw new ParseException("arguments not consistent", 0);  //ParseException used from java.text
    }
  }


  
  
  public void openReportfile(String sFileReport, boolean bAppendReport) 
  throws FileNotFoundException
  { 
    boolean bMkdir= false; 
    this.sFileReport = sFileReport;
    if(fReport != null)
    { try{ fReport.close(); } catch(IOException exc){}
    }
    try{ this.fReport = new FileWrite(sFileReport, bAppendReport); }
    catch(IOException exception)
    { //it is possible that the path not exist.
      bMkdir = true;
    }
    if(bMkdir)
    { { FileSystem.mkDirPath(sFileReport); 
        this.fReport = new FileWrite(sFileReport, bAppendReport);
      }
      
    }  
    
  }
  
  
  
  /** Checks the arguments after parsing from command line, test of consistence. This method must overwrite from
   * the user, may be with a return true. The method is ocalled at last inside parseArguments().
   * If this method returns false, parseArguments throws an exception in the same manner if on argument errors.
   *
   * @return true if all is ok. If false,
   */
  protected abstract boolean checkArguments();




  /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
   * and the application is terminated. The user should overwrite this method if the call without comand line arguments
   * is meaningfull.
   *
   */
  protected void callWithoutArguments()
  throws ParseException
  { writeAboutInfo();
    writeHelpInfo();
    throw new ParseException("no cmdline Arguments", 0);
  }

  /** Returns the argument contents. The user should invoke this method in his testArgument()-method.
      If the actually argument is longer as pos, the contents is the substring starts on pos.
      If the actually argument is no longer as pos( in praxis equal pos), the contents is the contents of the next argument.
      In this way the getArgument()-method supplies both argument methods:
      <ul><li>-kcontents</li>
          <li>-k contents</li>
      </ul>
      In this sample -k is the key to recognize the argument and contens is the appendant content.
      @param pos position of the beginning of the information in a argument. In the sample above use the value 2.
  */
  protected String getArgument(int pos)
  { String sRet;
    if(cmdLineArgs[iArgs].length() > pos) sRet = cmdLineArgs[iArgs].substring(pos);
    else
    { iArgs+=1;
      if(iArgs < cmdLineArgs.length)
      { sRet = cmdLineArgs[iArgs];
      }
      else
      { sRet = "";
      }
    }
    return sRet;
  }




  /** Tests one argument, called from parseArguments() on every argument excluding standard arguments -r and -rlevel.
      The user can overwrite this method to test the application-specific paramters.
      This non-overridden method uses the {@link #setArgumentList(Argument[])}
      @param argc The argument to test
      @param nArg number of argument, the first argument is numbered with 1.
      @return false, if the argument doesn't match, true if ok.
              if the method returns false, an exception in parseArgument is thrown to abort the argument test
              or to abort the programm. @see <a href="#parseArguments()">parseArgument()</a>.<br/>

      @exception ParseException The Methode may thrown this excpetion if a conversion error or such other is thrown.
  */
  protected boolean testArgument(String argc, int nArg)
  {
    if(argList !=null){
      Argument emptyArg = null;
      for(Argument argTest : argList){
        int argLen = argTest.arg.length();
        if(argLen == 0){
          emptyArg = argTest;
        } else {
          int argclen = argc.length();
          if((argc.startsWith(argTest.arg)                //correct prefix 
               && (  argclen == argLen                      //only the prefix
                  || ":=".indexOf(argc.charAt(argLen))>=0)  //or prefix ends with the separator characters.
                  )
            ){ //then the argument is correct and associated to this argTest.
            String argval = argclen == argLen //no additional value, use argument 
                          //|| argLen == 0      //argument without prefix (no option)
                          ? argc              //then use the whole argument as value.
                          : argc.substring(argLen+1);  //use the argument after the separator as value.
            boolean bOk = argTest.set.setArgument(argval);   //call the user method for this argument.
            //if(!bOk) throw new ParseException("Argument value error: " + argc, nArg);
            return bOk;
          }
        }
      }
      //argument start string not found:
      if(emptyArg !=null){
        //set the empty arg.
        return emptyArg.set.setArgument(argc);
      } else {
        //argument not found (not returned in for-loop):
        return false;
      }
    } else {
      System.err.println("MainCmd- Software design error - MainCmd.setArgumentList(...) should be called or the method testArgument(...) should be overridden.");
      return false;
    }
  }







  /*--------------------------------------------------------------------------------------------------------*/
  /** set the exitErrorLevel of the maximum of given level of every call.
      @param level Errorlevel how defined in Report, 0 is the lowest level (successfull), >0 is worse.
  */
  public void setExitErrorLevel(int level)
  { if(level > nMaxErrorLevel)
    {  nMaxErrorLevel = level;  }
  }



  /** get the maximum of errorLevel setted with setExitErrorLevel().
      @return the errorlevel
  */
  public int getExitErrorLevel()
  { return nMaxErrorLevel; 
  }


  
  /**Sets destinations for output and error output.
   * Per default the PrintStreams which are referenced from {@link java.lang.System#out} and {@link java.lang.System#err}
   * on construction of this class are set. That is the console output if this class is construct during processing
   * a public main(String[] args)-routine, this routine is invoked froma cmd line call and the output and error
   * channels of the command line are not redirected.
   * <br><br>
   * This method allows to redirect the output and error used from {@link #writeDirectly(String, short)} and from the
   * implementations of {@link #getLogMessageOutputConsole()} and {@link #getLogMessageErrorConsole()}.
   * If the 
   * 
   * 
   * It is recommended that this channels 
   * shall be unchanged, it means this method should not used. It is reasonable to use this method only for tests.
   * <br>
   * Carefull should be taken: If System.setOut or System.setErr is redirected using {@link org.vishia.msgDispatch.MsgPrintStream}
   * and that output is directed to this class, and then setOuputChannels(System.out, System.err) is set,
   * it escalates in a self-calling loop with stack overflow.
   *  
   * @param outP  Destination for output. If null, current output isn't change.
   * @param errP Destination for error output. If null, current output isn't change.
   */
  @Override public void setOutputChannels(Appendable outP, Appendable errP)
  {
    if(outP !=null) { outConsole = outP; }
    if(errP !=null) { errConsole = errP; }
  }
  
  
  

  /*--------------------------------------------------------------------------------------------------------*/
  /**Exits the cmdline application with the maximum of setted exit error level.
     This method should be called only on end of the application, never inside. If the user will abort
     the application from inside, he should throw an exception instead. So an comprising application
     may catch the exception and works beyond.
     This method is not member of MainCmd_Ifc and is setted protected, because
     the users Main class (UserMain in the introcuction) only should call exit.
  */
  public void exit()
  { System.exit(getExitErrorLevel());
  }



  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/

  /** Execute a command invoke a cmdline call, implements MainCmd_Ifc.
      @deprecated: since Java 1.5 a ProcessBuilder is available. 
      The new form {@link #executeCmdLine(String, ProcessBuilder, int, Appendable, String)} use it.
  */
  public int executeCmdLine(String cmd, int nReportLevel, Appendable output, String input)
  {
    String[] cmdArray = CmdExecuter.splitArgs(cmd);
    return executeCmdLine(cmdArray, nReportLevel, output, input);
  }





  /** Execute a command invoke a cmdline call, implements MainCmd_Ifc.
      The call must not be needed any input (:TODO:?).
      The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
      This class use the method writeInfoln() from here. The writeInfoln-Method writes to console for MainCmd,
      but it may be overloaded, to example for MainCmdWin it may be writed to a box in the GUI.
      @deprecated: since Java 1.5 a ProcessBuilder is available. 
      The new form {@link #executeCmdLine(String[], ProcessBuilder, int, Appendable, String)} use it.
  */
  public int executeCmdLine(String[] cmd, int nReportLevel, Appendable output, String input)
  {
    int exitErrorLevel; // = 0;  //default unused, only because throw
    { StringBuilder sOut = new StringBuilder();
      int iCmd = 0;
      while(iCmd < cmd.length)
      { sOut.append(cmd[iCmd]); iCmd +=1;
        sOut.append(" ");
      }
      sOut.append(":");
      reportln(Report.debug, "MainCmd.executeCmdLine():" + sOut);
      writeInfoln("execute>" + sOut.toString());
    }
    try
    { Process process = Runtime.getRuntime().exec(cmd);
      InputStream processOutput = process.getInputStream();  //reads the output from exec.
      InputStream processError  = process.getErrorStream();  //reads the error from exec.

      Runnable cmdOutput = new ShowCmdOutput(output, nReportLevel, new BufferedReader(new InputStreamReader(processOutput) ));
      Thread threadOutput = new Thread(cmdOutput,"cmdline-out");
      threadOutput.start();  //the thread reads the processOutput and disposes it to the infoln

      Runnable cmdError = new ShowCmdOutput(null, nReportLevel < 0 ? -Report.error: Report.error, new BufferedReader(new InputStreamReader(processError) ));
      Thread threadError = new Thread(cmdError,"cmdline-error");
      threadError.start();   //the thread reads the processError and disposes it to the infoln
      writeInfoln("process ...");
      process.waitFor();
      exitErrorLevel = process.exitValue();
    }
    catch(IOException exception)
    { writeInfoln( "Problem \n" + exception);
      throw new RuntimeException("IOException on commandline");
    }
    catch ( InterruptedException ie )
    {
      writeInfoln( ie.toString() );
      throw new RuntimeException("cmdline interrupted");
    }
    return exitErrorLevel;
  }

  
  
  /**Execute a command invoke a cmdline call, implements MainCmd_Ifc.
    The call must not be needed any input (:TODO:?).
    The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
    This class use the method writeInfoln() from here. The writeInfoln-Method writes to console for MainCmd,
    but it may be overloaded, to example for MainCmdWin it may be writed to a box in the GUI.
   * @deprecated
  */
  @Deprecated
  public int executeCmdLine
  ( String cmd
  , ProcessBuilder processBuilder
  , int nReportLevel
  , Appendable output, String input
  )
  {
    String[] cmdArray = CmdExecuter.splitArgs(cmd);  //split arguments in the array form
    return executeCmdLine(cmdArray, processBuilder, nReportLevel, output, input);
  
  }

  
  /**
   * @param cmd
   * @param processBuilder
   * @param nReportLevel
   * @param output
   * @param input
   * @return
   * @deprecated
   */
  @Deprecated
  public int executeCmdLine
  ( String[] cmd
  , ProcessBuilder processBuilder
  , int nReportLevel
  , Appendable output, String input
  ){
    return executeCmdLine(processBuilder, cmd, input, nReportLevel, output, output);
  }
  
  
  
  /**Executes a command line call maybe as pipe, waiting for finishing..
   * The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
   * @param processBuilder The ProcessBuilder. There may be assigned environment variables and a current directory.
   * @param cmd The cmd and arguments. If it is null, the command assigened to the processBuilder is used.
   *   The command can contain arguments separated with spaces (usual for command lines) or white spaces.
   *   The method {@link #splitArgs(String)} is used.
   * @param input Any pipe-input. It may be null.
   * @param nReportLevel The report level which is used for output. 
   *        If it is 0, then the output isn't written TODO
   * @param output The output pipe.
   * @param error The error pipe. If it is null, then errors are written in the output pipe.
   * @return
   */
  @Override public int executeCmdLine
  ( ProcessBuilder processBuilder
      , String cmd
      , String input
      , int nReportLevel
      , Appendable output
      , Appendable error
  ){
    String[] cmdArray = CmdExecuter.splitArgs(cmd);  //split arguments in the array form
    
    return executeCmdLine(processBuilder, cmdArray, input, nReportLevel, output, error);

  }


  
  /**Executes a command line call maybe as pipe, waiting for finishing..
   * The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
   * @param processBuilder The ProcessBuilder. There may be assigned environment variables and a current directory.
   * @param cmd The cmd and arguments. If it is null, the command assigend to the processBuilder is used.
   * @param input Any pipe-input. It may be null.
   * @param nReportLevel The report level which is used for output. 
   *        If it is 0, then the output isn't written TODO
   * @param output The output pipe.
   * @param error The error pipe. If it is null, then errors are written in the output pipe.
   * @return
   */
  @Override public int executeCmdLine
  ( ProcessBuilder processBuilder
      , String[] cmd
      , String input
      , int nReportLevel
      , Appendable output
      , Appendable error
  )
  { int exitErrorLevel;
  try
  {
    processBuilder.command(cmd);
    Process process = processBuilder.start();
    InputStream processOutput = process.getInputStream();  //reads the output from exec.
    InputStream processError  = process.getErrorStream();  //reads the error from exec.

    Runnable cmdOutput = new ShowCmdOutput(output, nReportLevel, new BufferedReader(new InputStreamReader(processOutput) ));
    Thread threadOutput = new Thread(cmdOutput,"cmdline-out");
    threadOutput.start();  //the thread reads the processOutput and disposes it to the infoln

    Runnable cmdError = new ShowCmdOutput(null, nReportLevel, new BufferedReader(new InputStreamReader(processError) ));
    Thread threadError = new Thread(cmdError,"cmdline-error");
    threadError.start();   //the thread reads the processError and disposes it to the infoln
    //boolean bCont = true;
    writeInfoln("process ...");
    //String sOut = null;
    //String sError = null;
    //int nTime = 0;
    //while(bCont)
    { //if(++nTime > 100000){ writeInfo("."); nTime = 0; }
      //if(in.ready())
      //{ writeInfoln(in   .readLine()); }
      //if(error.ready()) { writeInfoln(error.readLine()); }
      //if(sOut == null || sError == null)
      { //writeInfoln("******* finish **********");
        //bCont = false;
      }
    }

    process.waitFor();
    exitErrorLevel = process.exitValue();
  }
  catch(IOException exception)
  { writeInfoln( "Problem \n" + exception);
       exitErrorLevel = 255;
  //throw new RuntimeException("IOException on commandline");
  }
  catch ( InterruptedException ie )
  {
    writeInfoln( ie.toString() );
    exitErrorLevel = 255;
    //throw new RuntimeException("cmdline interrupted");
  }
  return exitErrorLevel;
  }
  


  /**Starts a command invocation for a independent window.
   * This command does not have any input or output. The command will be started,
   * the finishing isn't await. This command line invocation is proper for commands,
   * which create a new window in the operation system. The new window has its own live cycle then,
   * independent of the invocation.
   * @param cmd The command. Some arguments are possible, they should be separated by space.
   * @param processBuilder The processBuilder.
   * @return
   */
  @Override public int startCmdLine(ProcessBuilder processBuilder, String cmd)
  {
    String[] cmdArray = CmdExecuter.splitArgs(cmd);  //split arguments in the array form
    return startCmdLine(processBuilder, cmdArray);
  
  }
  
  
  /**Starts a command invocation for a independent window.
   * This command does not have any input or output. The command will be started,
   * the finishing isn't await. This command line invocation is proper for commands,
   * which create a new window in the operation system. The new window has its own live cycle then,
   * independent of the invocation.
   * @param cmd The command and some arguments.
   * @param processBuilder The processBuilder.
   * @return 0 on success, 255 if any start error.
   */
  @Override public int startCmdLine(ProcessBuilder processBuilder, String[] cmd)
  { int exitErrorLevel = 0;
    try
    {
      processBuilder.command(cmd);
      Process process = processBuilder.start();
    
    }
    catch(IOException exception)
    { writeError("", exception);
      exitErrorLevel = 255;
      //throw new RuntimeException("IOException on commandline");
    }
    return exitErrorLevel;
  }
  

  @Override public int switchToWindowOrStartCmdline(ProcessBuilder processBuilder, String sCmd, String sWindowTitle)
  {
    throw new IllegalArgumentException("only available in graphical-systems.");
  }



  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/
  /** Writes an information, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeInfoDirectly, it is called here.
  */
  public final void writeInfo(String sInfo)
  { writeDirectly(sInfo, kInfo_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.info)
    { report(Report.info | Report.mNeverOutputToDisplay, sInfo );
    }
  }

  /** Writes an information line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeInfolnDirectly, it is called here.
  */
  public final void writeInfoln(String sInfo)  //##a
  { writeDirectly(sInfo, kInfoln_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.info)
    { reportln(Report.info | Report.mNeverOutputToDisplay, sInfo );
    }
  }


  /** Writes an warning line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeWarningDirectly, it is called here.
  */
  public final void writeWarning(String sInfo)
  { writeDirectly(sInfo, kWarning_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.warning)
    { reportln(Report.warning | Report.mNeverOutputToDisplay, "WARNING:" + sInfo );
    }
  }

  /** Writes an error line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeErrorDirectly, it is called here.
  */
  public final void writeError(String sInfo)
  { writeDirectly(sInfo, kError_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.error)
    { reportln(Report.error | Report.mNeverOutputToDisplay, "ERROR:" + sInfo );
    }
  }


  /** Writes an error line with exception info, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeErrorDirectly, it is called here.
  */
  public final void writeError(String sInfo, Exception exception)
  { writeErrorDirectly(sInfo, exception);
    if(nLevelDisplayToReport >= Report.error)
    { report(sInfo, exception, true );
    }
  }

  
  

  /** write out the stacktrace from a exception. Writes into reportfile and on screen
  */
  public void writeStackTrace(Exception exception)
  { exception.printStackTrace(new PrintStream(fReport, true));
  }



  /** Some bits to mark the kind of output*/
  protected static final short kInfo_writeInfoDirectly     = (short)0x1;
  public static final short kInfoln_writeInfoDirectly   = (short)0x3;
  protected static final short kWarning_writeInfoDirectly  = (short)0x6;
  protected static final short kError_writeInfoDirectly    = (short)0xa;

  /** If this bit is setted in param kind of writeDirectly
   * a newline should be inserted at begin of output
   */
  public static final short mNewln_writeInfoDirectly   = (short)0x2;

  /** If this bit is setted in param kind of writeDirectly
   * a warning is to be outputted
   */
  protected static final short mWarning_writeInfoDirectly = (short)0x4;

  /** If this bit is setted in param kind of writeDirectly
   * an error is to be outputted
   */
  protected static final short mError_writeInfoDirectly   = (short)0x8;


  /**Writes the given sInfo to the output channels {@link #out} or {@link #err}. That channels are gotten from
   * System.out and System.err on construction of this class. Usual that channels are non-overriden, so that it is
   * the console output. 
   * <br><br>
   * If {@link java.lang.System#setErr(PrintStream)} or {@link java.lang.System#setOut(PrintStream)} was invoked,
   * this routine is not influenced. 
   * <br><br>
   * This method is overrideable espscially to redirect the output to a graphical window instead to console.
   * Another possibility to change the output is {@link #setOutputChannels(Appendable, Appendable)}.
   *  @param sInfo The String to write
   *  @param kind Combination of bits {@link #mWarning_writeInfoDirectly}, {@link #mError_writeInfoDirectly}
   *    or {@link #mNewln_writeInfoDirectly}.
   */
  public void writeDirectly(String sInfo, short kind)  //##a
  {
    try{
      if((kind & mWarning_writeInfoDirectly) != 0)
      {
        errConsole.append("\n");
        errConsole.append( "WARNING: ").append(sInfo ).append("\n");
      }
      else if((kind & mError_writeInfoDirectly) != 0)
      {
        errConsole.append("\n");
        errConsole.append( "ERROR: " ).append(sInfo).append("\n");
      }
      else
      { if( (kind & mNewln_writeInfoDirectly) != 0) outConsole.append("\n"); //finishes the previous line
        int posStart = 0;
        int posEol;
        while( posStart < sInfo.length() && (posEol = sInfo.indexOf('\n', posStart)) >=0)
        { outConsole.append(sInfo.substring(posStart, posEol)).append("|");
          posStart = posEol + 1;
        }
        if(posStart < sInfo.length()) outConsole.append(sInfo.substring(posStart));
      }
    } catch(Exception exc){
      System.err.println("Exception while output in MainCmd.writeDirectly(" + sInfo + ")");
    }
  }




  /** Writes an error line by console application directly to System.err.println
      with the String "EXCEPTION: " before and the exception message.
      The user can overwrite this method in the derived class of MainCmd to change to kind of output.
      @param sInfo Text to write in the new line after "EXCEPTION: ".
      @param exception Its getMessage will be written.
  */
  public void writeErrorDirectly(String sInfo, Exception exception)
  {
    System.err.println("");
    System.err.println( "EXCEPTION: " + sInfo); // + exception.getMessage());
    exception.printStackTrace(System.err);  //the exception message will be printed here.
  }

  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/
  /* implements Report.*/


  /** report appending on exist line.
      If the report contains an newline char \n, a char '|' will be written.
      This newline is not interpreted as newline for the report file, but only as stochastic contents in the string.
    * @param nLevel write the report only if the demand level is greater or equal. Use {@link MainCmdLogging_ifc#error}
    * etc.
    * @param ss String to write.
  */
  public void report(int nLevel, String ss)
  { if( (nLevel & mReportLevel) <= nReportLevel && fReport != null)
    { int posStart = 0;
      int posEol;
      /*
      while( posStart < ss.length() && (posEol = ss.indexOf('\n', posStart)) >=0)
      { fReport.write(ss.substring(posStart, posEol) + "|");
        posStart = posEol + 1;
      }
      */
      if(posStart < ss.length()){ fReport.write(ss.substring(posStart)); }
    }
    if( (nLevel & mReportLevel) <= nReportLevelDisplay && (nLevel & mNeverOutputToDisplay) == 0)
    { //writes also an error as info on display.
      writeDirectly(ss, kInfo_writeInfoDirectly);
    }

  }

  /** report begins at a new a line with left margin
    * @param nLevel write the report only if the demand level is greater or equal.
    * @param nLeftMargin determins a left margin. First a new line is outputted, followed by '*' and spaces.
    * @param ss String to write.
  */
  public void reportln(int nLevel, int nLeftMargin, String ss)
  { if( (nLevel & Report.mReportLevel) <= nReportLevel && fReport != null)
    { fReport.writeln("");
      if(nLeftMargin > report_spaces.length()/2) nLeftMargin = report_spaces.length()/2;
      fReport.write("*" + (nLevel & Report.mReportLevel) + ":" + report_spaces.substring(0, 2*nLeftMargin));
    }
    if((nLevel & Report.mReportLevel) <= nReportLevelDisplay && (nLevel & Report.mNeverOutputToDisplay) == 0 )
    { //writes also an error as info on display.
      writeDirectly(ss, kInfoln_writeInfoDirectly);
    }
    report((nLevel | Report.mNeverOutputToDisplay), ss);
  }

  public void reportln(int nLevel, String ss){ reportln(nLevel, 0, ss); }


  /** Reports an exception. This report is written unconditional, like Report.error.
      On Display, this report is only written if display output is enabled for the level Report.error.
      @param sInfo Text to write in the new line after "EXCEPTION: ".
      @param exception Exception info to write
  */
  public void report(String sInfo, Exception exception)
  {
    report(sInfo, exception, false); //not written on display yet.
  }

  /** Internal method to write an exception to report. A third parameter idicates,
   * wether or not the writing to display is always done.
   * @param sInfo Text to write in the new line after "EXCEPTION: "
   *        and before the exception.getMessage is written.<br/>
   * @param exception Exception info to write
   * @param bWrittenOnDisplay true, than the writing to display is always done.
   */
  private void report(String sInfo, Exception exception, boolean bWrittenOnDisplay)
  { if(fReport == null){
      if( !bWrittenOnDisplay){
        writeErrorDirectly(sInfo, exception);
        //exception.printStackTrace(System.out);
      }
    } else { 
      fReport.writeln("\nEXCEPTION: " + sInfo + "  " + exception.getMessage());
      exception.printStackTrace(new PrintStream(fReport, true));
      if(Report.error <= nReportLevelDisplay && !bWrittenOnDisplay)
      { //writes also an error as info on display.
        writeErrorDirectly(sInfo, exception);
      }
    }
  }


  /** Test wether the report is in the level.
  */
  public int getReportLevel(){ return nReportLevel; }
  
  /** Set another level inside programming. It is advisable to restore the old level
   * after the designated operation.
   * @param newLevel The level to be set, use one of the defines Report.info to Report.fineDebug
   * @return the current level, usefull to restore it.
   */
  public int setReportLevel(int newLevel)
  { int oldLevel = nReportLevel;
    nReportLevel = newLevel;
    return oldLevel;
  }


  public void flushReport()
  { if(fReport != null)
    { try{ fReport.flush();}
      catch(IOException exc)
      { //do noting because the report is damaged at all.
      }
    }
  }

  
  
  
  public void setReportLevelToIdent(int nLevel, int nLevelActive)
  { throw new RuntimeException("report idents are not supported here.");
    
  }
  
  
  public int getReportLevelFromIdent(int ident)
  { if(ident >=Report.error && ident <=Report.fineDebug)
    { return ident;
    }
    else throw new RuntimeException("report idents are not supported here.");
  }

  
  
  
  public String getReportFileName()
  { return sFileReport;
  }
  


  /**Sends a message. The timestamp of the message is build with the system time. 
   * All other parameter are identically see {@link #sendMsg(int, OS_TimeStamp, String, Object...)}.
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return TODO
   */  
  @Override public boolean sendMsg(int identNumber, String text, Object... args)
  { return redirectLogMessage.sendMsg(identNumber, text, args);
  }

  /**Sends a message.
   * 
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param creationTime absolute time stamp. @Java2C=perValue.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return TODO
   */
  @Override public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args)
  { return redirectLogMessage.sendMsgTime(identNumber, creationTime, text, args);
  }
  

  
  /**Sends a message. The functionality and the calling parameters are identically 
   * with {@link #sendMsg(int, OS_TimeStamp, String, Object...)}, but the parameter args is varied:  
   * @param identNumber
   * @param creationTime
   * @param text
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *        @java2c=zeroTermString.
   * @param args Reference to a buffer which contains the values for a variable argument list.
   *             <br>
   *             In C implementation it is a reference either to the stack, or to a buffer elsewhere,
   *             but the reference type is appropriate to provide the values in stack
   *             for calling routines with variable argument list such as 
   *             <code>vprintf(buffer, text, args)</code>.
   *             The referenced instance shouldn't accepted as persistent outside processing time 
   *             of the called routine. Therefore stack content is able to provide.
   *             <br>
   *             In Java it is a special class wrapping a Object[] tantamount to a Object...
   *             as variable argument list. Using of this wrapper class is only a concession
   *             to C-programming, because in Java an Object[] would adequate.
   * @return true than okay. It is possible, that a destination for dispatching is not available yet.
   *         Than the routine returns false. That is for special outputs of message dispatcher. 
   *         Normally the user shouldn't realize false here and react anywise. 
   *         If a message isn't able to transport, it is not visible in the creating thread. 
   *         It is possible that a message is lost anywhere in transportation way. In Generally,
   *         to secure a complex systems functionality, any timeouts, repeats 
   *         and backup strategies are necessary 
   *         in the supervise software above sending a single message.           
   */
  @Override public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args){
    return redirectLogMessage.sendMsgVaList(identNumber, creationTime, text, args);
  }

  /**Only preliminary, because Java2C doesn't support implementation of interfaces yet.
   * This method is implemented in C in another kind.
   * @param src 
   * @return the src.
   */
  //public final static LogMessage convertFromMsgDispatcher(LogMessage src){ return src; }
  
  /**A call of this method closes the devices, which processed the message. It is abstract. 
   * It depends from the kind of device, what <code>close</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>close</code> may mean, the processing of messages is finite temporary. 
   * An <code>open</code> occurs automatically, if a new message is dispatched. 
   */
  public void close(){
    redirectLogMessage.close();
  }
  
  /**A call of this method causes an activating of transmission of all messages since last flush. 
   * It is abstract. It depends from the kind of device, what <code>flush</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>flush</code> may mean, the processing of messages is ready to transmit yet. 
   */
  public void flush(){
    redirectLogMessage.flush();
  }
  
  /**Checks whether the message output is available. */
  public boolean isOnline(){
    return redirectLogMessage.isOnline();
  }



    
  private final void sendMsgTimeToAppendableDst(Appendable dst, int identNumber, int reportLevel, OS_TimeStamp creationTime,
      String text, Object... args) {
      final String line;
      if(args.length == 0){
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + text;
      } else {
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + String.format(text,args);
      }
      try{ 
        dst.append(line);
        if(!line.endsWith("\n")){
          dst.append("\n");
        }
      } catch(IOException exc){
        //the exception may be unexpected. Write it to the original System.err on construction of this class,
        //all other channels may be redirected.
        exc.printStackTrace(this.errCmdline);
      }
  }

    

  
  protected LogMessage logMessageImplReport = new LogMessage()
  {

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args) {
      Object oArgs = args.get();
      return sendMsgTime(identNumber, creationTime, text, oArgs);
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args) {
      final int reportLevel = identNumber == 0 ? 
                               Report.info :
                               identNumber <= Report.fineDebug ? identNumber : Report.info;

      final String line;
      if(args.length == 0){
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + text;
      } else {
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + String.format(text, args);
      }
      reportln(reportLevel, line);
      ///
      return true;
    }
    
  };
  


  
  
  
  
  class LogMessageImplConsole implements LogMessage
  {

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime,
        String text, Va_list args) {
      Object oArgs = args.get();
      return sendMsgTime(identNumber, creationTime, text, oArgs);
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args) {
      final int reportLevel = identNumber == 0 ? 
                               Report.info :
                               identNumber <= Report.fineDebug ? identNumber : Report.info;
      sendMsgTimeToAppendableDst(MainCmd.this.outConsole, identNumber, reportLevel, creationTime, text, args); 
      return true;
    }
    
  }
  

  
  
  class LogMessageImplErrConsole implements LogMessage
  {

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime,
        String text, Va_list args) {
      Object oArgs = args.get();
      return sendMsgTime(identNumber, creationTime, text, oArgs);
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args) {
      final int reportLevel = identNumber == 0 ? 
                               Report.info :
                               identNumber <= Report.fineDebug ? identNumber : Report.info;
      sendMsgTimeToAppendableDst(MainCmd.this.errConsole, identNumber, reportLevel, creationTime, text, args); 
      return true;
    }
    
  }
  
  class LogMessageImplFile implements LogMessage
  {
    final private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime,
        String text, Va_list args) {
      String line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.get());
      reportln(Report.info, line);
      return false;
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime,
        String text, Object... args) {
      if(fReport != null)
      { String line = "*" + identNumber + "; " + dateFormat.format(creationTime) + "; ";
        fReport.writeln("");
        fReport.write(line);
        if(args.length == 0){
          //no arguments, no formatting!
          line = text;  //may be more as one line, can contain %-character.
        } else {
          line = String.format(text,args);
        }
        fReport.write(line);  //may be more as one line.
      }
      return false;
    }
    
  }
  
  LogMessageImplConsole logMessageConsole = new LogMessageImplConsole();
  
  LogMessageImplErrConsole logMessageErrConsole = new LogMessageImplErrConsole();
  
  LogMessageImplFile logMessageFile = new LogMessageImplFile();
  
  @Override public LogMessage getLogMessageOutputConsole(){ return logMessageConsole; }
  
  @Override public LogMessage getLogMessageErrorConsole(){ return logMessageErrConsole; }
  
  @Override public LogMessage getLogMessageOutputFile(){ return logMessageFile; }

  
  
  
  /**Its a helper to set a breakpoint for assert
   * @param condition
   */
  public static void XXXassertion(boolean condition){
    if(!condition){
      throw new RuntimeException ("assertion");
    }
  }

  /*===========================================================================================================*/
  /** Class to write any readed stream to the output, running in a separate thread.
   *  Used especially by command invokation
  */
  private class ShowCmdOutput implements Runnable
  {
    /** To response the outer class outer may be used.*/
    //final MainCmd outer = (MainCmd)(this);  //:TODO: why is that an error?

    /** The reader to read the contents*/
    final BufferedReader reader;

    /** Captures the output if not null, see constructor*/
    final Appendable uOutput;

    /**control of the output medium. See constructor, value nCtrl*/
    final int nCtrl;


    /** Creates a new ShowCmdOutput
        The output is directed in the followed manner with the value of nCtrl
        <table border=1>
          <tr><th>nCtrl</th><th>meaning</th></tr>
          <tr><td>Report.error .. Report.fineDebug</td>  <td>Writes the output to the report</td></tr>
          <tr><td>-Report.error (negativ)         </td>   <td>Writes the output via writeError()</td></tr>
          <tr><td>-Report.warning (negativ)       </td><td>Writes the output via writeWarning()</td></tr>
          <tr><td>-Report.info (negativ)          </td><td>Writes the output via writeInfo()
                                                           usw.                            </td></tr>
          <tr><td>other value</td>               <td>cause an exception, it is wrong.</td></tr>
        </table>
        @param nCtrl Controls the output channel: 1..6 using Report.error to Report.fineDebug writes to the report;
                     -1..-3 (negative) using -Report.error to -Report.info  writes via writeInfo
        @param in The input stream to read.
    */
    ShowCmdOutput(Appendable output, int nCtrl, BufferedReader in)
    { this.uOutput = output;
      this.nCtrl   = nCtrl;
      this.reader  = in;
    }


    /** runnable in a separate thread, reads the reader and write to*/
    public void run()
    { String sOut;
      try
      { while( (sOut = reader.readLine()) != null)
        { if(uOutput!=null) uOutput.append(sOut + "\n");

          if     (nCtrl == -Report.error)  writeError(sOut);
          else if(nCtrl == -Report.warning)writeWarning(sOut);
          else if(nCtrl == -Report.info)   writeInfoln(sOut);
          else if(nCtrl == 0);   //no output
          else reportln(nCtrl, sOut);
        }
      }
      catch(IOException exception){ System.out.println(exception.toString()); }
      //System.out.println("***** ende **********");
    }
  }
  /*===========================================================================================================*/

}













                           