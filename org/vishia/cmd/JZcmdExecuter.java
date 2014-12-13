package org.vishia.cmd;


import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;




import org.vishia.cmd.CmdExecuter;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.Conversion;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FilePath;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.MessageQueue;
import org.vishia.util.StringFormatter;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPartAppend;
import org.vishia.util.StringSeq;
import org.vishia.util.CalculatorExpr.Value;
import org.vishia.util.IndexMultiTable.Provide;
import org.vishia.xmlSimple.SimpleXmlOutputter;


/**This class is the executer of JZcmd. The translated JZscript is contained in an instance of {@link JZcmdScript}. 
 * It can be executed with a given script:
 * <ul>
 * <li>{@link #execute(JZcmdScript, boolean, boolean, Appendable, String)} executes a given translated script.
 * <li>{@link #execSub(org.vishia.cmd.JZcmdScript.Subroutine, Map, boolean, Appendable, File)} executes one subroutine
 *   from a translated script. It is possible to translate a script one time, and then invoke any subroutine 
 *   from a java context on demand.
 * <li>{@link #initialize(JZcmdScript, boolean, Map, String)} cleans the instance and generates all script variables.
 *   It prepares usage for {@link #execSub(org.vishia.cmd.JZcmdScript.Subroutine, Map, boolean, Appendable, File)}.
 * <li>{@link #reset()} cleans the instance. The scriptvariables will be generate 
 *   by first call of {@link #execute(JZcmdScript, boolean, boolean, Appendable, String)}
 * </ul>
 * To use the Java platform's {@link javax.script.CompiledScript} eval method start {@link JZcmdScript#eval(ScriptContext)}
 * with an instance of {@link #scriptLevel()}.
 * <br><br> 
 * An instance of this class is used while {@link #execute(JZcmdScript, boolean, boolean, Appendable))} is running.
 * You should not use the instance concurrently in more as one thread. But you can use this instance
 * for one after another call of {@link #execute(JZcmdScript, boolean, boolean, Appendable)}.
 * Note that Threads can be created in the script.
 * <br><br>
 * @author Hartmut Schorrig
 *
 */
public class JZcmdExecuter {
  
  
  /**Version, history and license.
   * <ul>
   * <li>2014-12-14 Hartmut new: {@link ExecuteLevel#jzClass} stores the current class statement. Not call of own class methods without the class name is possible. TODO instances  
   * <li>2014-12-12 Hartmut new: If a subroutine's argument has the type 'F': {@link JZcmdFilepath}, then the argument is converted into this type if necessary.
   *   TODO: do so for all non-Obj argument types. It is probably to work with a simple String as argument if a Filepath is expected. Therefore new {@link ExecuteLevel#convert2FilePath(Object)}. 
   * <li>2014-12-06 Hartmut new: don't need a main routine, writes only a warning on System.out. 
   * <li>2014-11-28 Hartmut chg: {@link ExecuteLevel#bForHasNext} instead calling argument for all execute...(...), it was forgotten in one level.
   * <li>2014-11-21 Hartmut chg: Character ::: can be used as indentation characters
   * <li>2014-11-16 Hartmut chg: enhancement of capability of set text column: <:@ nr> nr can be an expression. Not only a constant.   
   * <li>2014-10-20 Hartmut chg: break in a condition should break a loop, forwarding {@link #kBreak} from execution level in {@link ExecuteLevel#executeIfStatement(org.vishia.cmd.JZcmdScript.IfStatement, StringFormatter, int, int)}.
   * <li>2014-10-20 Hartmut new: Map name = { String variable = "value"; Obj next; } now works. Changed 'M' instead 'X' in {@link JZcmdScript.JZcmditem#add_dataStruct(org.vishia.cmd.JZcmdScript.StatementList)}.
   *   {@link ExecuteLevel#evalObject(org.vishia.cmd.JZcmdScript.JZcmditem, boolean)} has accepted such dataStruct already (used in argumentlist), now with 'M' instead 'X'.
   *   {@link ExecuteLevel#exec_DefMapVariable(org.vishia.cmd.JZcmdScript.DefVariable, Map)} newly.  
   * <li>2014-10-20 Hartmut bufgix: some parameter in call of {@link ExecuteLevel#execute(org.vishia.cmd.JZcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}
   *   were set faulty. Last change bug from 2014-07-27. 
   * <li>2014-08-10 Hartmut new: <:>...<.> as statement writes into the current out Buffer. Before: calculates an textexpression which is never used then.
   *   In opposite:<+>...<.> writes to the main text output always, not to the current buffer. 
   * <li>2014-08-10 Hartmut new: !checkXmlFile = filename; 
   * <li>2014-07-27 Hartmut bugfix: {@link ExecuteLevel#exec_hasNext(org.vishia.cmd.JZcmdScript.JZcmditem, StringFormatter, int, boolean, int)}
   * <li>2014-07-27 Hartmut chg: save one level for recursive execution, less stack, better able to view
   *   by calling {@link ExecuteLevel#execute(org.vishia.cmd.JZcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}immediately.
   * <li>2014-06-15 Hartmut chg: improved {@link ExecuteLevel#exec_zmake(org.vishia.cmd.JZcmdScript.Zmake, StringFormatter, int, int)}:
   *   works with a Filepath for output.
   * <li>2014-06-15 Hartmut chg: improved {@link ExecuteLevel#executeCopy(org.vishia.cmd.JZcmdScript.CallStatement)}:
   *   works with a Filepath. TODO for execMove!
   * <li>2014-06-14 Hartmut chg: {@link ExecuteLevel} implements {@link FilePath.FilePathEnvAccess} now,
   *   therewith a {@link JZcmdFileset#listFiles(List, JZcmdFilepath, boolean, org.vishia.util.FilePath.FilePathEnvAccess)}
   *   does not need an accessPath, it may be empty respectively null.
   * <li>2014-06-10 Hartmut chg: improved Exception handling of the script.
   * <li>2014-06-01 Hartmut chg: {@link #genScriptVariables(JZcmdScript, boolean, Map, CharSequence)} and
   *   {@link #initialize(JZcmdScript, boolean, Map, String)} throws on any error.
   * <li>2014-06-01 Hartmut chg: "File :" as conversion type for any objExpr, not in a dataPath.TODO: Do the same for Filepath, Fileset with accessPath
   * <li>2014-06-01 Hartmut chg: uses {@value #kException} as return value of all called 
   *   {@link ExecuteLevel#execute(org.vishia.cmd.JZcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}
   *   instead a new ForwardException. Reason: Stacktrace of Exception is not disturbed.
   *   The ForwardException has put its Stacktrace only. Too complex to read the causer. 
   * <li>2014-05-18 Hartmut chg: DefFilepath now uses a textValue and divides the path to its components
   *   at runtime, doesn't use the prepFilePath in ZBNF. Reason: More flexibility. The path can be assmebled
   *   on runtime.  
   * <li>2014-05-18 Hartmut new: try to implement javax.script interfaces, not ready yet
   * <li>2014-05-18 Hartmut chg: {@link ExecuteLevel#execSubroutine(org.vishia.cmd.JZcmdScript.Subroutine, Map, StringFormatter, int)}
   *   called from {@link org.vishia.zcmd.JZcmd#execSub(File, String, Map, ExecuteLevel)} for a sub routine in a new translated script.
   * <li>2014-05-18 Hartmut chg: Handling of {@link ExecuteLevel#currdir}   
   * <li>2014-05-10 Hartmut new: File: supported as conversion with currdir. See {@link JZcmdScript.JZcmdDataAccess#filepath}.
   * <li>2014-05-10 Hartmut new: {@link ExecuteLevel#cmdExecuter} instantiated in a whole subroutine. It is more faster
   *   instead creating a new instance for any cmd invocation.
   * <li>2014-04-24 Hartmut chg: {@link #execute(JZcmdScript, boolean, boolean, Appendable, String)} 
   *   returns nothing, No confusion between argument out and return value! If out is not given,
   *   then <:>text<.> is not possible. It causes an NullPointerException which may be thrown.
   * <li>2014-04-24 Hartmut new: {@link #textout} the same for all threads, used synchronized. 
   * <li>2014-04-25 Hartmut new: {@link NextNr#start()} etc. more capability.
   * <li>2014-03-08 Hartmut new: debug_dataAccessArgument() able to call from outside, to force breakpoint.
   * <li>2014-03-08 Hartmut new: Filepath as type of a named argument regarded on call, see syntax
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   * <li>2014-03-01 Hartmut new: {@link ExecuteLevel#execForContainer(org.vishia.cmd.JZcmdScript.ForStatement, Appendable, int)}
   *   now supports arrays as container too.
   * <li>2014-03-01 Hartmut new: !argsCheck! functionality.  
   * <li>2014-02-22 Hartmut new: Bool and Num as variable types.
   * <li>2014-02-16 Hartmut chg: Build of script variable currdir, scriptfile, scriptdir with them in {@link JZcmdExecuter#genScriptVariables(JZcmdScript, boolean, Map, CharSequence)}.
   *   {@link #execute(JZcmdScript, boolean, boolean, Appendable, String)} and {@link #execSub(org.vishia.cmd.JZcmdScript.Subroutine, Map, boolean, Appendable, File)}
   *   with sCurrDir.
   * <li>2014-02-01 Hartmut new: onerror errorlevel for cmd now works as statement. {@link ExecuteLevel#cmdErrorlevel}. 
   * <li>2014-02-01 Hartmut chg: now {@link ExecuteLevel#execute(org.vishia.cmd.JZcmdScript.StatementList, Appendable, int, boolean)}
   *   returns the exit designation (break, return) 
   * <li>2014-01-12 Hartmut chg: now uses a Stringjar {@link StringPartAppend} instead StringBuffer in Syntax and execution.
   * <li>2014-01-09 Hartmut new: If the "text" variable or any other Appendable variable has a null-value,
   *   a StringBuilder is instantiated therefore and stored in this variable. It is possible therewith
   *   to create an text only if necessary. We don't need a StringBuilder instance if it is never used. 
   * <li>2014-01-06 Hartmut new: {@link ThreadData} has the error Variable, test thread 
   * <li>2013-12-26 Hartmut new: subroutine returns a value and assigns to any variable.
   * <li>2013-12-26 Hartmut re-engineering: Now the Statement class is obsolete. Instead all statements have the base class
   *   {@link JZcmditem}. That class contains only elements which are necessary for all statements. Some special statements
   *   have its own class with some more elements, especially for the ZBNF parse result. Compare it with the syntax
   *   in {@link org.vishia.zcmd.JZcmdSyntax}.    
   * <li>2013-10-27 Hartmut chg: Definition of a String name [= value] is handled like assign. Stored with 
   *   {@link DataAccess#storeValue(List, Map, Object, boolean)} with special designation in {@link DataAccess.DatapathElement#whatisit}
   * <li>2013-10-20 Hartmut chg: The {@link #scriptVariables} and the {@link ExecuteLevel#localVariables} are of type
   *   {@link DataAccess.Variable} and not only Object. Advantage: The variable references can be changed in the
   *   instance of {@link DataAccess.Variable#val}, this changing is valid for all references to that variable.
   *   In this kind a script-variable can be changed in a subroutine. This may be necessary and should be supported.
   *   Nevertheless a concept of non-changeable script variables may be proper. It should realized in runtime.
   * <li>2013-10-13 Hartmut chg: onerror: only the coherent statements in one block are checked for onerror. See description.
   *   onerror exit treaded.
   * <li>2013-01-13 Hartmut chg: The method getContent is moved and adapted to {@link ZbatchGenScript.ZbatchExpressionSet#ascertainValue(Object, Map, boolean, boolean, boolean)}.
   * <li>2013-01-12 Hartmut chg: improvements while documentation. Some syntax details. Especially handling of visibility of variables.
   * <li>2013-01-02 Hartmut chg: The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   *   But a script-global variable referred with {@link #listScriptVariables} is defined only one time on start of text generation
   *   with the routine {@link ZbatchExecuter#genScriptVariables(ZbatchGenScript, Object, boolean)}.  
   * <li>2012-12-23 Hartmut chg: {@link #getContent(org.vishia.zbatch.ZbatchGenScript.ZbatchExpressionSet, Map, boolean)} now uses
   *   an {@link ZbatchGenScript.ZbatchExpressionSet} instead a List<{@link DataAccess.DatapathElement}>. Therewith const values are able to use
   *   without extra dataPath, only with a ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link ZbatchGenScript.ZbatchExpressionSet#textArg} if a data path is given, use for formatting a numerical value.
   * <li>2012-12-08 Hartmut new: <:subtext:name:formalargs> has formal arguments now. On call it will be checked and
   *   maybe default values will be gotten.
   * <li>2012-12-08 Hartmut chg: {@link #parseGenScript(File, Appendable)}, {@link #genScriptVariables()}, 
   *   {@link #genContent(ZbatchGenScript, Object, boolean, Appendable)} able to call extra especially for Zmake, currDir.
   *   It is possible to define any script variables in the generating script and use it then to control getting data 
   *   from the input data.
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-11-04 Hartmut chg: adaption to DataAccess respectively distinguish between getting a container or an simple element.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-10 Usage of {@link ZbatchGenScript}.
   * <li>2012-10-03 created. Backgorund was the {@link org.vishia.zmake.Zmake} generator, but that is special for make problems.
   *   A generator which converts ZBNF-parsed data from an Java data context to output texts in several form, documenation, C-sources
   *   was need.
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
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-12-14";

  /**Variable for any exception while accessing any java resources. It is the $error variable of the script. */
  protected String accessError = null;
  
  /**Should break a loop, break statement. */
  public static final short kBreak = -1;
  
  /**Should return from subroutine, return statement. */ 
  public static final short kReturn = -2;
  
  /**Only internal designation.*/
  private static final short kFalse = -3;
  
  /**Has thrown an not catch exception. onerror statement not found. */
  private static final short kException = -5;
  
  /**Should continue. */
  public static final short kSuccess = 0;
  
  
  
  
  /**Set it to true if private and protected fields and methods should be used
   * by data access.
   */
  public boolean bAccessPrivate;
  
  protected final MainCmdLogging_ifc log;
  
  /**The text file output, the same for all threads. */
  StringFormatter textout;

  
  /**The java prepared generation script. */
  JZcmdScript jzcmdScript;
  
  /**Instance for the main script part. */
  //Gen_Content genFile;

  /**Generated content of all script variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  //private final IndexMultiTable<String, DataAccess.Variable> scriptVariables = new_Variables();
  
  
  final ExecuteLevel scriptLevel;
  
  final JZcmdThread scriptThread;
  
  /**Generated content of all script environment variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  //final Map<String, String> scriptEnvVariables = new TreeMap<String, String>();
  
  
  protected final Queue<JZcmdThread> threads = new ConcurrentLinkedQueue<JZcmdThread>();
  
  private boolean bScriptVariableGenerated;
  
  /**The time stamp from {@link System#currentTimeMillis()} on start of script. */
  public long startmilli;
  
  /**The time stamp from {@link System#nanoTime()} on start of script. */
  public long startnano;
  
  /**The newline char sequence. */
  String newline = "\r\n";
  
  int nextNr_ = 0;
  
  /**This instance is used to mark a return object as exception return. */
  private static CharSequence retException = new String("Exception");
  
  
  /**Used for formatting Numbers. 
   * Problem in Germany: The numbers are written with , instead with a decimal point. 
   * Using Locale.ENGLISH produces the well used decimal point.
   * Numbers with comma are used only in the german banking sector, not in engineering.
   */
  protected Locale locale = Locale.ENGLISH;
  

  /**Creates a JZcmdExecuter with possible writing exceptions in the output text.
   * 
   * @param log maybe null
   */
  public JZcmdExecuter(MainCmdLogging_ifc log){
    this.log = log;
    scriptThread = new JZcmdThread();
    scriptLevel = new ExecuteLevel(scriptThread);
  }
  
  
  /**Creates a JZcmdExecuter with possible writing exceptions in the output text.
   */
  public JZcmdExecuter(){
    MainCmdLogging_ifc log = MainCmd.getLogging_ifc();  //maybe started with MainCmd
    if(log == null){
      log = new MainCmdLoggingStream(System.out);
    }
    this.log = log;
    scriptThread = new JZcmdThread();
    scriptLevel = new ExecuteLevel(scriptThread);
  }
  
  
  
  
  /**Returns the association to all script variables. The script variables can be changed
   * via this association. Note that change of script variables is a global action, which should not
   * be done for special requests in any subroutine.
   */
  public Map<String, DataAccess.Variable<Object>> scriptVariables(){ return scriptLevel.localVariables; }
  
  /**Returns the script level instance. This instance is created with the constructor of this class (a composite).
   * The {@link ExecuteLevel#localVariables} of the script level are the script variables. 
   * They are filled with the given script on call of {@link #initialize(JZcmdScript, boolean, Map, String)}
   * or if {@link #execute(JZcmdScript, boolean, boolean, Appendable, String)} was called.
   */
  public ExecuteLevel scriptLevel(){ return scriptLevel; }
  
  
  public long calctime(){ return System.currentTimeMillis() - startmilli; }
  
  public void setScriptVariable(String name, char type, Object content, boolean bConst) 
  throws IllegalAccessException{
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, name, type, content, bConst);
  }
  
  
  
  /**Generates script-global variables.
   * 
   * @param genScriptPar It should be the same how used on {@link #execute(JZcmdScript, boolean, boolean, Appendable, String)}
   *   but it may be another one for special cases.
   * @param userData Used userdata for content of scriptvariables. It should be the same how used on 
   *   {@link #genContent(JZcmdScript, Object, boolean, Appendable)} but it may be another one for special cases.
   * @param accessPrivate true than access to private data of userData
   * @return The built script variables. 
   *   One can evaluate some script variables before running {@link #genContent(JZcmdScript, Object, boolean, Appendable)}.
   *   Especially it is used for {@link org.vishia.zmake.Zmake to set the currDir.} 
   * @throws Throwable 
   * @throws SecurityException 
   * @throws NoSuchMethodException 
   * @throws NoSuchFieldException 
   */
  private Map<String, DataAccess.Variable<Object>> genScriptVariables(
      JZcmdScript genScriptPar
    , boolean accessPrivate
    , Map<String, DataAccess.Variable<Object>> srcVariables
    , CharSequence sCurrdirArg
  ) 
  throws ScriptException //Throwable
  {
    this.bAccessPrivate = accessPrivate;
    try{
      if(sCurrdirArg == null && scriptLevel.currdir == null){
        //get from the JVM environment respecitvely from the operation system.
        scriptLevel.currdir = new File("").getAbsoluteFile();  
      } else if(sCurrdirArg !=null) {
        scriptLevel.changeCurrDir(sCurrdirArg);
      }
      if(srcVariables !=null){
        for(Map.Entry<String, DataAccess.Variable<Object>> entry: srcVariables.entrySet()){
          DataAccess.Variable<Object> var = entry.getValue();
          DataAccess.createOrReplaceVariable(scriptLevel.localVariables, var.name(), var.type(), var.value(), var.isConst());
        }
      }
      //do not replace variables which are set from outside.
      //if(scriptLevel.localVariables.get("error") == null){ DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "error", 'A', accessError, true); }
      if(scriptLevel.localVariables.get("console") == null){ DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "console", 'O', log, true); }
      //DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nrElementInContainer", 'O', null);
      if(scriptLevel.localVariables.get("out") == null)  {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "out", 'A', System.out, true); }
      if(scriptLevel.localVariables.get("err") == null)  {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "err", 'A', System.err, true); }
      if(scriptLevel.localVariables.get("null") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "null", 'O', null, true); }
      //if(scriptLevel.localVariables.get("") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "null", 'O', null, true); }
      //if(scriptLevel.localVariables.get("jbat") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "jbat", 'O', this, true); }
      //if(scriptLevel.localVariables.get("zgen") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "zgen", 'O', this, true); }
      if(scriptLevel.localVariables.get("jzcmd") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "jzcmd", 'O', this, true); }
      //if(scriptLevel.localVariables.get("file") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "file", 'O', new FileSystem(), true); }
      if(scriptLevel.localVariables.get("test") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "test", 'O', new JZcmdTester(), true); }
      if(scriptLevel.localVariables.get("conv") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "conv", 'O', new Conversion(), true); }
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "Math", 'C', Class.forName("java.lang.Math"), true);
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "System", 'C', Class.forName("java.lang.System"), true);
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "FileSystem", 'C', Class.forName("org.vishia.util.FileSystem"), true);
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "StringFunctions", 'C', Class.forName("org.vishia.util.StringFunctions"), true);
      File filescript = genScriptPar.fileScript;
      if(scriptLevel.localVariables.get("scriptfile") == null && filescript !=null) { 
        String scriptfile = filescript.getName();
        DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "scriptfile", 'S', scriptfile, true);
        CharSequence scriptdir = FileSystem.normalizePath(FileSystem.getDir(filescript));
        //File dirscript = FileSystem.getDirectory(filescript).getCanonicalFile();
        DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "scriptdir", 'S', scriptdir, true);
      }
      if(scriptLevel.localVariables.get("nextNr") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nextNr", 'O', new NextNr(), true); }
      Method mCurrdir = getClass().getMethod("nextNr");
      DataAccess.ObjMethod objMethod = new DataAccess.ObjMethod(mCurrdir, this);
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nextnr", 'M', objMethod, true);
      //
    } catch(IllegalAccessException exc){
      throw new ScriptException("JZcmdExecuter.genScriptVariable - IllegalAccessException; " + exc.getMessage());
    } catch(Exception exc){
      throw new ScriptException("JZcmdExecuter.genScriptVariable - unexpected exception; " + exc.getMessage());
    }
    //generate all variables in this script:
    short ret;
    ret = scriptLevel.execute(genScriptPar.scriptClass, null, 0, scriptLevel.localVariables, -1);
    if(ret == kException){
      throw new ScriptException(scriptThread.exception.getMessage(), scriptThread.excSrcfile, scriptThread.excLine, scriptThread.excColumn);
    }
    //setCurrdirScript(sCurrdirArg);
    bScriptVariableGenerated = true;
    return scriptLevel.localVariables;
  }
  

  

  public CharSequence currdir(){ return scriptLevel.currdir(); }
  
  /**Initializes without any script variables, clears the instance.
   * The first call of {@link #execute(JZcmdScript, boolean, boolean, Appendable, String)} will be generate the script variables.
   */
  public void reset(){
    bScriptVariableGenerated = false;
    scriptLevel.localVariables.clear();
    this.jzcmdScript = null;
  }
  
  
  /**Initializes, especially generate all script variables. All content before is removed.
   * Especially script variables from a previous usage of the instance are removed.
   * If you want to use a JZcmdExecuter more as one time with different scripts
   * but with the same script variables, one should call this routine one time on start,
   * and then {@link #execute(JZcmdScript, boolean, boolean, Appendable)} with maybe several scripts,
   * which should not contain script variables, or one should call 
   * {@link #execSub(org.vishia.cmd.JZcmdScript.Subroutine, Map, boolean, Appendable)}
   * with one of the subroutines in the given script.
   * 
   * @param genScriptPar Generation script in java-prepared form. It contains the building prescript
   *   for the script variables.
   * @param accessPrivate decision whether private and protected members from Java instances can be accessed.   
   * @throws Throwable 
   * @throws SecurityException not expected
   * @throws NoSuchMethodException not expected 
   */
  public void initialize
      ( JZcmdScript genScriptArg
      , boolean accessPrivate
      , Map<String, DataAccess.Variable<Object>> srcVariables
      , String sCurrdir
      ) 
  throws ScriptException
  {
    this.scriptLevel.localVariables.clear();
    this.bAccessPrivate = accessPrivate;
    this.jzcmdScript = genScriptArg;
    genScriptVariables(genScriptArg, accessPrivate, srcVariables, sCurrdir);
  }

  
  /**Executes the given script.
   * @param genScriptP The script. It sets the {@link #jzcmdScript} internal variable which is used
   *   to search sub routines. 
   * @param accessPrivate 
   * @param bWaitForThreads should set to true if it is a command line invocation of Java,
   *   the exit should wait for all threads. May set to false if calling inside a long running application.
   * @param out Any output. It is used for direct text output <:>text<.> and it is stored as variable "text"
   *   to write "<+text>...<.+>" to output to it.
   * @throws IOException only if out.append throws it.
   * @throws IllegalAccessException if a const scriptVariable are attempt to modify.
   */
  public void execute(
      JZcmdScript genScriptP
    , boolean accessPrivate
    , boolean bWaitForThreads
    , Appendable out
    , String sCurrdir
    ) 
  throws ScriptException //Exception, IllegalAccessException, Throwable
  { this.bAccessPrivate = accessPrivate;
    //this.data = userData;
    this.jzcmdScript = genScriptP;
    short ret;
    //try
    {
      try{
        setScriptVariable("text", 'A', out, true);  //NOTE: out maybe null
      } catch(IllegalAccessException exc){ throw new ScriptException("JZcmd.execute - IllegalAccessException; " + exc.getMessage()); }
      if(!bScriptVariableGenerated){
        genScriptVariables(jzcmdScript, accessPrivate, null, sCurrdir);
      }
      //needs all scriptVariable:
      ExecuteLevel execFile = new ExecuteLevel(jzcmdScript.scriptClass, scriptThread, scriptLevel, null);
      if(jzcmdScript.checkJZcmdXmlFile !=null) {
        CharSequence sFilecheckXml;
        try { sFilecheckXml = scriptLevel.evalString(jzcmdScript.checkJZcmdXmlFile);
        } catch (Exception exc) { throw new ScriptException("JZcmd.execute - String eval error on checkJZcmd; "
            , jzcmdScript.checkJZcmdXmlFile.srcFile, jzcmdScript.checkJZcmdXmlFile.srcLine, jzcmdScript.checkJZcmdXmlFile.srcColumn ); 
        }
        SimpleXmlOutputter xmlOutputter = new SimpleXmlOutputter();
        try{
          OutputStreamWriter xmlWriter = new OutputStreamWriter(new FileOutputStream(sFilecheckXml.toString()));
          xmlOutputter.write(xmlWriter, jzcmdScript.xmlSrc);
          xmlWriter.close();
          jzcmdScript.xmlSrc = null;  //can be garbaged.
        } catch(IOException exc){ throw new ScriptException(exc); }
      	
      }
      if(jzcmdScript.checkJZcmdFile !=null){
        CharSequence sFilecheck;
        try { sFilecheck = execFile.evalString(jzcmdScript.checkJZcmdFile);
        } catch (Exception exc) { throw new ScriptException("JZcmd.execute - String eval error on checkJZcmd; "
            , jzcmdScript.checkJZcmdFile.srcFile, jzcmdScript.checkJZcmdFile.srcLine, jzcmdScript.checkJZcmdFile.srcColumn ); 
        }
        File filecheck = new File(sFilecheck.toString());
        try{
          Writer writer = new FileWriter(filecheck);
          jzcmdScript.writeStruct(writer);
          writer.close();
        } catch(IOException exc){ throw new ScriptException("JZcmd.execute - File error on checkJZcmd; " + filecheck.getAbsolutePath()); }
      }
      JZcmdScript.Subroutine mainRoutine = jzcmdScript.getMain();
      //return execute(execFile, contentScript, true);
      startmilli = System.currentTimeMillis();
      startnano = System.nanoTime();
      StringFormatter outFormatter = new StringFormatter(out, out instanceof Closeable, "\n", 200);
      textout = outFormatter;
      if(mainRoutine !=null) {
        ret = execFile.execute(mainRoutine.statementlist, outFormatter, 0, execFile.localVariables, -1);
      } else {
        System.out.println("JZcmdExecuter - main routine not found.");
        ret = 0;
      }
      try{ outFormatter.close(); } catch(IOException exc){ throw new RuntimeException("unexpected exception on close", exc); }
      if(bWaitForThreads){
        boolean bWait = true;
        while(bWait){
          synchronized(threads){
            bWait = threads.size() !=0;
            if(bWait){
              try{ threads.wait(1000); }
              catch(InterruptedException exc){}
            }
          }
        }
  
      }
    }
    //catch(Exception exc){
    //  ret = kException;
    //}
    if(ret == kException){
      if(scriptThread.exception instanceof ScriptException){
        throw (ScriptException)scriptThread.exception; 
      } else {
        throw new ScriptException(scriptThread.exception.toString(), scriptThread.excSrcfile, scriptThread.excLine, scriptThread.excColumn);
      }
    }
  }

  
  /**Executes the given sub routine invoked from any user application. 
   * The script variables are used from a {@link #initialize(JZcmdScript, boolean)}
   * or one of the last {@link #execute(JZcmdScript, boolean, boolean, Appendable)}.
   * The {@link ExecuteLevel}, the subroutine's context, is created below the script level. 
   * All of the script variables are known in the subroutine. Additional the args are given.
   * The time measurements {@link #startmilli} and {@link #startnano} starts newly.
   * @param statement The subroutine in the script.
   * @param accessPrivate
   * @param out The text output.
   * @return
   * @throws Throwable 
   * @throws IOException
   */
  public String execSub(JZcmdScript.Subroutine statement, Map<String, DataAccess.Variable<Object>> args
      , boolean accessPrivate, Appendable out, File currdir) 
  throws Throwable
  {
    ExecuteLevel level = new ExecuteLevel(jzcmdScript.scriptClass, scriptThread, scriptLevel, null);
    //The args should be added to the localVariables of the subroutines level:
    level.localVariables.putAll(args);
    if(currdir !=null){
      level.changeCurrDir(currdir.getPath());
    }
    setScriptVariable("text", 'A', out, true);
    //Executes the statements of the sub routine:
    startmilli = System.currentTimeMillis();
    startnano = System.nanoTime();
    StringFormatter outLines = new StringFormatter(out, out instanceof Closeable, "\n", 200);
    short ret = level.execute(statement.statementlist, outLines, 0, level.localVariables, -1);
    outLines.close();
    if(ret == kReturn || ret == kBreak){ ret = kSuccess; }
    if(ret == kException){
      throw scriptThread.exception;
    }
    return null;
  }
  
  
  
  
  
  
  
  public DataAccess.Variable<Object> getScriptVariable(String name) throws NoSuchFieldException
  { return DataAccess.getVariable(scriptLevel.localVariables, name, true); }

  
  public DataAccess.Variable<Object> removeScriptVariable(String name)
  { return scriptLevel.localVariables.remove(name);
    
  }

  
  
  


  protected IndexMultiTable<String, DataAccess.Variable<Object>> new_Variables(){
    return new IndexMultiTable<String, DataAccess.Variable<Object>>(IndexMultiTable.providerString);
  }
  
  
  
  
  public String nextNr(){
    return Integer.toString(++nextNr_); 
  }
  
  

  
  /**Wrapper to generate a script with specified localVariables.
   * A new Wrapper is created on any subroutine level. It is used in a {@link CalculatorExpr#calcDataAccess(Map, Object...)} 
   * to generate an expression independent of an environment.
   *
   */
  public final class ExecuteLevel implements ScriptContext, FilePath.FilePathEnvAccess
  {
    /**Not used yet. Only for debug! */
    public final ExecuteLevel parent;
    
    /**Counts nesting of {@link #execute(org.vishia.cmd.JZcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}
     * 1->0: leaf execution of this level, close {@link #cmdExecuter}    */
    int ctNesting = 0;
    
    final JZcmdThread threadData;
    
    
    final JZcmdScript.JZcmdClass jzClass;
    
    /**The current directory of this level. It is an absolute normalized but not Unix-canonical path. 
     * Note that a canonical path resolved symbolic links. */
    File currdir;
    
    /**Flag, write error in the current output if set to true. */
    public boolean bWriteErrorInOutput;
    

    /**Generated content of local variables in this nested level including the {@link ZbatchExecuter#scriptLevel.localVariables}.
     * The variables are type invariant on language level. The type is checked and therefore 
     * errors are detected on runtime only. */
    public final IndexMultiTable<String, DataAccess.Variable<Object>> localVariables;
    
    
    /**Initialize firstly on demand. Don't close to speed up following cmd invocations.
     */
    public CmdExecuter cmdExecuter;

    /**Set on break statement. Used only in a for-container execution to break the loop over the elements. */
    private boolean isBreak;
    
    /**Used while a for-container loop runs. Remain after for if for was broken with a condition. */
    private boolean bForHasNext;
    
    private boolean debug_dataAccessArguments;
    
    /**The error level which is returned from an operation system cmd invocation.
     * It is used for the {@link #execCmdError(org.vishia.cmd.JZcmdScript.Onerror)}.
     */
    public int cmdErrorlevel = 0;
    
    
    /**Constructs data for a local execution level.
     * @param parentVariables if given this variable are copied to the local ones.
     *   They contains the script variables too. If not given (null), only the script variables
     *   are copied into the {@link #localVariables}. Note that subroutines do not know the
     *   local variables of its calling routine! This argument is only set if nested statement blocks
     *   are to execute. 
     */
    protected ExecuteLevel(JZcmdScript.JZcmdClass jzClass, JZcmdThread threadData, ExecuteLevel parent
        , Map<String, DataAccess.Variable<Object>> parentVariables)
    { this.parent = parent;
      this.jzClass = jzClass;
      this.threadData = threadData;
      if(parent !=null) {
        this.currdir = parent.currdir;
      }
      localVariables = new_Variables();
      if(parentVariables != null) {
        localVariables.putAll(parentVariables);  //use the same if it is not a subText, only a 
      } else if(scriptLevel !=null) {
        for(Map.Entry<String, DataAccess.Variable<Object>> e: scriptLevel.localVariables.entrySet()){
          DataAccess.Variable<Object> var = e.getValue();
          String key = e.getKey();
          if(key.equals("jzcmdsub")){
            
          }
          else if(var.isConst()){
            localVariables.put(key, var);
          } else {
            //build a new independent variable, which can be changed.
            DataAccess.Variable<Object> var2 = new DataAccess.Variable<Object>(var);
            localVariables.put(key, var2);
          }
        }
      }
      try{ 
        DataAccess.createOrReplaceVariable(localVariables,  "jzcmdsub", 'O', this, true);
        localVariables.add("error", threadData.error);
      } catch(IllegalAccessException exc){ throw new RuntimeException(exc); }
    }

    
    
    /**Constructs data for the script execution level.
     */
    protected ExecuteLevel(JZcmdThread threadData)
    { this(null, threadData, null, null);
    }

    public JZcmdExecuter executer(){ return JZcmdExecuter.this; }
    
    
    public JZcmdEngine scriptEngine(){ return jzcmdScript.getEngine(); }
    
    
    /**Returns the log interface from the environment class. */
    public MainCmdLogging_ifc log(){ return JZcmdExecuter.this.log; }
    
    
    
    public void setLocalVariable(String name, char type, Object content, boolean isConst) 
    throws IllegalAccessException {
      DataAccess.createOrReplaceVariable(localVariables, name, type, content, isConst);
    }
    
    
    
    
    
    /**Executes an inner script part maybe with a new level of nested local variables.
     * If the contentScript does not contain any variable definition 
     * (see @link {@link JZcmdScript.StatementList#bContainsVariableDef}) then this level is used,
     * it prevents a non necessary instantiation of an {@link ExecuteLevel} and copying of local variables. 
     * If new variables are to build in the statements, a new instance of {@link ExecuteLevel} is created
     * and all variables of this.{@link #localVariables} are copied to it. It means the nested level
     * knows the same variables but it can add new variables without effect to this level. Changing
     * of existing variables effects all outer levels. That is necessary, an outer-defined variable can be changed
     * in an inner level. That is guaranteed because only the reference is changed.
     * @param contentScript of the inner level.
     * @param out The main output
     * @param bContainerHasNext only used for for-statement
     * @return an error hint.
     * @throws IOException
     */
    public short executeNewlevel(JZcmdScript.JZcmdClass jzClass, JZcmdScript.StatementList contentScript, final StringFormatter out, int indentOut
        , int nDebug) 
    throws Exception 
    { final ExecuteLevel level;
      if(contentScript.bContainsVariableDef){
        level = new ExecuteLevel(jzClass, threadData, this, localVariables);
      } else {
        level = this;
      }
      return level.execute(contentScript, out, indentOut, level.localVariables, nDebug);
    }

  

    /**Processes the statement of the current node in the JZcmditem.
     * @param statementList 
     * @param out The current output. Either it is a special output channel for <+channel>...<.+>
     *   or it is the threadData.out or it is null if threadData.out is not initialized yet.
     * @param indentOutArg The indentation in the script.
     * @param bContainerHasNext Especially for <:for:element:container>SCRIPT<.for> to implement <:hasNext>
     * @param newVariables Destination instance for newly created variables. It is {@link #localVariables} usual
     *   but in special cases new variables are stored in an own Map, especially on { <dataStruct> }.
     * @param nDebugP
     * @return
     * @throws Exception
     */
    private short execute(JZcmdScript.StatementList statementList, StringFormatter out, int indentOutArg
        , Map<String, DataAccess.Variable<Object>> newVariables, int nDebugP) 
    //throws Exception 
    {
      this.ctNesting +=1;
      //Generate direct requested output. It is especially on inner content-scripts.
      int indentOut = indentOutArg;
      int ixStatement = -1;
      short ret = 0;
      //Note: don't use an Iterator, use ixStatement because it will be incremented onError.
      while(ret == 0 && ++ixStatement < statementList.statements.size()) { //iter.hasNext() && sError == null){
        JZcmdScript.JZcmditem statement = statementList.statements.get(ixStatement); //iter.next();
        int nDebug1 = 0; //TODO nDebug>0 || debugNext >=0;
        if(statement.elementType() == 'D'){
          nDebug1 = debug(statement);  //debug
          if(++ixStatement < statementList.statements.size()) { //iter.hasNext() && sError == null){
            statement = statementList.statements.get(ixStatement);
          } else {
            //debug was the last statement, it is unecessary.
          }
        } else {
          nDebug1 = nDebugP;
        }
        if(nDebug1 >=0){
          //TODO print a debug text
          Debugutil.stop();  //NOTE: set the local variable nDebug1 to 0: stop debugging for sub levels.
        }
        //for(TextGenScript.ScriptElement statement: contentScript.content){
        try{    
          switch(statement.elementType()){
          case 't': executeText(statement, out, indentOut);break; //<:>...textexpression <.>
          case '@': execSetColumn((JZcmdScript.TextColumn)statement, out);break; //<:@23>
          case 'n': out.append(newline);  break;   //<.n+>
          case '!': out.flush();  break;   //<.n+>
          case '_': out.close();  out = null; break;   //<.n+>
          case '\\': out.append(statement.textArg);  break;   //<:n> transcription
          case 'T': ret = textAppendToVar((JZcmdScript.TextOut)statement, --nDebug1); break; //<+text>...<.+> 
          case ':': ret = textAppendToOut(statement, out, --nDebug1); break; //<+text>...<.+> 
          case 'A': break;  //used for Argument
          //case 'X': break;  //unused for dataStruct in Argument
          case 'U': ret = defineExpr(newVariables, (JZcmdScript.DefVariable)statement); break; //setStringVariable(statement); break; 
          case 'S': ret = defineExpr(newVariables, (JZcmdScript.DefVariable)statement); break; //setStringVariable(statement); break; 
          case 'P': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            executeDefVariable(newVariables, (JZcmdScript.DefVariable)statement, 'P', uBufferVariable, true);
          } break;
          case 'L': {
            Object value = evalObject(statement, true);
            if(value == JZcmdExecuter.retException){ ret = kException; }
            else {
              if(value !=null && !(value instanceof Iterable<?>)) 
                throw new NoSuchFieldException("JZcmdExecuter - exec variable must be of type Iterable ;" + ((JZcmdScript.DefVariable)statement).defVariable);
              if(value ==null){ //initialize the list
                value = new ArrayList<Object>();
              }
              executeDefVariable(newVariables, (JZcmdScript.DefVariable)statement, 'L', value, true);
            }
          } break;
          case 'M': ret = exec_DefMapVariable((JZcmdScript.DefVariable)statement, newVariables); break;
          case 'W': ret = executeOpenfile(newVariables, (JZcmdScript.DefVariable)statement); break;
          case 'C': ret = exec_DefClassVariable((JZcmdScript.DefClassVariable) statement, newVariables); break; 
          case 'J': ret = addClassLoader((JZcmdScript.DefClasspathVariable)statement, newVariables); break;
          case 'O': {
            Object value = evalObject(statement, false);
            if(value == JZcmdExecuter.retException){ ret = kException; }
            else {
              executeDefVariable(newVariables, (JZcmdScript.DefVariable)statement, 'O', value, false);
            }
          } break;
          case 'K': {
            Object value = evalValue(statement, false);
            if(value == JZcmdExecuter.retException){ ret = kException; }
            else {
              executeDefVariable(newVariables, (JZcmdScript.DefVariable)statement, 'K', value, false);
            }
          } break;
          case 'Q': {
            Object cond = new Boolean(evalCondition(statement));
            if(cond == JZcmdExecuter.retException){ ret = kException; }
            else {
              executeDefVariable(newVariables, (JZcmdScript.DefVariable)statement, 'Q', cond, false);
            }
          } break;
          case 'e': ret = executeDatatext((JZcmdScript.DataText)statement, out); break; 
          case 's': ret = execCall((JZcmdScript.CallStatement)statement, null, out, indentOut, --nDebug1); break;  //sub
          case 'x': ret = executeThread(newVariables, (JZcmdScript.ThreadBlock)statement); break;             //thread
          case 'm': executeMove((JZcmdScript.CallStatement)statement); break;             //move
          case 'y': executeCopy((JZcmdScript.CallStatement)statement); break;             //copy
          case 'l': executeDelete((JZcmdScript.CallStatement)statement); break;             //copy
          case 'c': exec_cmdline((JZcmdScript.CmdInvoke)statement); break;              //cmd
          case 'd': ret = executeChangeCurrDir(statement); break;                              //cd
          case 'f': ret = execForContainer((JZcmdScript.ForStatement)statement, out, indentOut, --nDebug1); break;  //for
          case 'B': ret = execNestedLevel(statement, out, indentOut, --nDebug1); break;              //statementBlock
          case 'i': ret = executeIfStatement((JZcmdScript.IfStatement)statement, out, indentOut, --nDebug1); break;
          case 'w': ret = whileStatement((JZcmdScript.CondStatement)statement, out, indentOut, --nDebug1); break;
          case 'u': ret = dowhileStatement((JZcmdScript.CondStatement)statement, out, indentOut, --nDebug1); break;
          case 'N': ret = exec_hasNext(statement, out, indentOut, --nDebug1); break;
          case '=': ret = assignStatement(statement); break;
          case '+': ret = appendExpr((JZcmdScript.AssignExpr)statement); break;        //+=
          case '?': break;  //don't execute a onerror, skip it.  //onerror
          case 'z': throw new JZcmdExecuter.ExitException(((JZcmdScript.ExitStatement)statement).exitValue);  
          case 'r': execThrow(statement); break;
          case 'v': execThrowonerror((JZcmdScript.Onerror)statement); break;
          case ',': bWriteErrorInOutput = statement.textArg !=null; break;
          case 'b': isBreak = true; ret = JZcmdExecuter.kBreak; break;
          case '#': ret = execCmdError((JZcmdScript.Onerror)statement, out, indentOut); break;
          case 'F': ret = createFilepath(newVariables, (JZcmdScript.DefVariable) statement); break;
          case 'G': ret = createFileSet(newVariables, (JZcmdScript.UserFileset) statement); break;
          case 'Z': ret = exec_zmake((JZcmdScript.Zmake) statement, out, indentOut, --nDebug1); break;
          case 'D': break; // a second debug statement one after another or debug on end is ignored.
          default: throw new IllegalArgumentException("JZcmd.execute - unknown statement; ");
          }//switch
          
        } catch(Exception exc){
          //any statement has thrown an exception.
          
          CharSequence errortext;
          if(exc instanceof InvocationTargetException){
            threadData.exception = exc.getCause();
          } else {
            threadData.exception = exc;
          }
          threadData.excStatement = statement;
          threadData.excLine = statement.srcLine;
          threadData.excColumn = statement.srcColumn;
          threadData.excSrcfile = statement.srcFile;
          StringBuilder u = new StringBuilder(1000); 
          u.append(threadData.exception.toString()).append("; in statement: ");
          statement.writeStructLine(u);
          threadData.error.setValue(u);
          errortext = u;
          if(bWriteErrorInOutput){
            try{ out.append("<?? ").append(errortext).append(" ??>");
            } catch(IOException exc1){ throw new RuntimeException(exc1); }
            threadData.error.setValue(null);  //clear for next usage.
            threadData.exception = null;
            threadData.excStatement = null;
          } else {
            ret = kException;
          }
        } //catch
        //
        //handle onerror
        //
        if(ret == kException){
          //check onerror with proper error type anywhere after this statement, it is stored in the statement.
          //continue there.
          boolean found = false;
          char excType;   //NOTE: the errortype in an onerror statement is the first letter of error keyword in syntax; notfound, file, internal, exit
          int errLevel = 0;
          final Throwable exc1 = threadData.exception;
          if(exc1 instanceof ExitException){ excType = 'e'; errLevel = ((ExitException)exc1).exitLevel; }
          else if(exc1 instanceof IOException){ excType = 'f'; }
          else if(exc1 instanceof CmdErrorLevelException){ excType = 'c'; }
          else if(exc1 instanceof NoSuchFieldException || exc1 instanceof NoSuchMethodException){ excType = 'n'; }
          else { excType = 'i'; }
          //Search the block of onerror after this statement.
          //Maybe use an index in any statement, to prevent search time.
          JZcmdScript.JZcmditem onerrorStatement = null;
          while(++ixStatement < statementList.statements.size() && (onerrorStatement = statementList.statements.get(ixStatement)).elementType() != '?');
          if(ixStatement < statementList.statements.size()){
            assert(onerrorStatement !=null);  //because it is found in while above.
            //onerror-block found.
            do { //search the appropriate error type:
              char onerrorType;
              JZcmdScript.Onerror errorStatement = (JZcmdScript.Onerror)onerrorStatement;
              if( ((onerrorType = errorStatement.errorType) == excType
                || (onerrorType == '?' && excType != 'e')   //common onerror is valid for all excluding exit 
                )  ){
                found = excType != 'e' || errLevel >= errorStatement.errorLevel;  //if exit exception, then check errlevel
              }
            } while(!found && ++ixStatement < statementList.statements.size() && (statement = statementList.statements.get(ixStatement)).elementType() == '?');
          }
          if(found){ //onerror found:
            assert(onerrorStatement !=null);  //because it is found in while above.
            ret = execute(onerrorStatement.statementlist, out, indentOut, localVariables, -1);  //executes the onerror block
            //maybe throw exception too, Exception in onerror{...}
            if(ret != kException) {
              threadData.error.setValue(null);  //clear for next usage.
              threadData.exception = null;
              threadData.excStatement = null;
            }
          } else {
            ret = kException;  //terminates this level.
            assert(threadData.exception !=null);
          }
        }
      }//while
      /*
      DataAccess.Variable<Object> retVar = localVariables.get("return");
      if(retVar !=null){
        @SuppressWarnings("unchecked")
        Map<String, DataAccess.Variable<Object>> ret =  (Map<String, DataAccess.Variable<Object>>)retVar.value(); 
        return ret;
      }
      else { return null; }
      */
      endExecution();
      return ret;
    }
    
    
    
    private void endExecution(){
      if(--ctNesting <=0){
        assert(ctNesting ==0);
        //close this level.
        if(cmdExecuter !=null){
          cmdExecuter.close();
          cmdExecuter = null;
        }
      }
    }
    
    
    void executeText(JZcmdScript.JZcmditem statement, Appendable out, int indentOut) throws IOException{
      int posLine = 0;
      int posEnd1, posEnd2;
      if(statement.textArg.startsWith("  "))
        stop();
      int zText = statement.textArg.length();
      do{
        char cEnd = '\n';  
        posEnd1 = statement.textArg.indexOf(cEnd, posLine);
        posEnd2 = statement.textArg.indexOf('\r', posLine);
        if(posEnd2 >= 0 && (posEnd2 < posEnd1 || posEnd1 <0)){
          posEnd1 = posEnd2;  // \r found before \n
          cEnd = '\r';
        }
        if(posEnd1 >= 0){ 
          out.append(statement.textArg.substring(posLine, posEnd1));   
          out.append(newline);  //The newline of JZcmd invocation.
          //skip over posEnd1, skip over the other end line character if found. 
          if(++posEnd1 < zText){
            if(cEnd == '\r'){ if(statement.textArg.charAt(posEnd1)=='\n'){ posEnd1 +=1; }}
            else            { if(statement.textArg.charAt(posEnd1)=='\r'){ posEnd1 +=1; }}
            //posEnd1 refers the start of the next line.
            int indentCt = indentOut;
            char cc;
            while(indentCt > 0 && posEnd1 < zText && ((cc = statement.textArg.charAt(posEnd1)) == ' ' || cc == '\t')) {
              posEnd1 +=1; //skip over all indentation chars
              indentCt -=1;
            }
            while(--indentCt >= 0 && posEnd1 < zText && (cc = statement.textArg.charAt(posEnd1)) == ':') {
              posEnd1 +=1; //skip over all ::: as indentation chars  
            }
            //line starts after :::: which starts before indentation end
            //or line starts after first char which is not a space or tab
            //or line starts on the indent position.
          }
          posLine = posEnd1;
        } else { //the rest till end.
          out.append(statement.textArg.substring(posLine));   
        }
        
      } while(posEnd1 >=0);  //output all lines.
    }
    
    
    void execSetColumn(JZcmdScript.TextColumn statement, StringFormatter out) throws Exception{
      int column = -1;
      int minChars = -1;
      if(statement.expression !=null) {
        CalculatorExpr.Value value = calculateExpression(statement.expression); //.calcDataAccess(localVariables);
        column = value.intValue();
      }
      
      out.pos(column, minChars);
    }
    
    

    void executeDefVariable(Map<String, DataAccess.Variable<Object>> newVariables, JZcmdScript.DefVariable statement, char type, Object value, boolean isConst) 
    throws Exception {
      if(statement.typeVariable !=null){
        Debugutil.stop();
      }
      storeValue(statement.defVariable, newVariables, value, bAccessPrivate);
      //setLocalVariable(statement.name, type, value, isConst);
       
    }

    
    
    short createFilepath(Map<String, DataAccess.Variable<Object>> newVariables, JZcmdScript.DefVariable statement) throws Exception {
      CharSequence sPath = evalString(statement);
      if(sPath == JZcmdExecuter.retException){ return kException; }
      else {
      
        JZcmdFilepath filepath = new JZcmdFilepath(this, sPath.toString());
        storeValue(statement.defVariable, newVariables, filepath, false);
        return kSuccess;
      }
    }
    
      
    short createFileSet(Map<String, DataAccess.Variable<Object>> newVariables, JZcmdScript.UserFileset statement) throws Exception {
      JZcmdFileset filepath = new JZcmdFileset(this, statement);
      storeValue(statement.defVariable, newVariables, filepath, false);
      return kSuccess;
    }
    
      
    private short execForContainer(JZcmdScript.ForStatement statement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception
    {
      JZcmdScript.StatementList subContent = statement.statementlist();  //The same sub content is used for all container elements.
      //Note: don't use an extra ExecuteLevel to save calculation time. Especially the forVariable and some inner defined variables
      //      are existing outside of the for loop body namely, but that property is defined in the JZcmd language description.
      ExecuteLevel forExecuter = this; //do not do so: new ExecuteLevel(threadData, this, localVariables);
      //creates the for-variable in the executer level.
      DataAccess.Variable<Object> forVariable = DataAccess.createOrReplaceVariable(forExecuter.localVariables, statement.forVariable, 'O', null, false);
      //a new level for the for... statements. It contains the foreachData and maybe some more variables.
      Object container = dataAccess(statement.forContainer, localVariables, bAccessPrivate, true, false, null);
      //Object container = statement.forContainer.getDataObj(localVariables, bAccessPrivate, true);
      //DataAccess.Dst dst = new DataAccess.Dst();
      //DataAccess.access(statement.defVariable.datapath(), null, localVariables, bAccessPrivate,false, true, dst);
      boolean cond = true;
      short cont = kSuccess;
      boolean bForHasNextOld = bForHasNext;  //to restore
      if(container instanceof String && ((String)container).startsWith("<?")){
        throw new IllegalArgumentException("JZcmd.execFor - faulty container type;" + (String)container);
      }
      else if(container !=null && container instanceof Iterable<?>){
        Iterator<?> iter = ((Iterable<?>)container).iterator();
        while(cond && !forExecuter.isBreak() && iter.hasNext()){
          cond = (cont == kSuccess);
          if(cond && statement.condition !=null){
            cond = evalCondition(statement.condition);
          }
          if(cond){
            Object foreachData = iter.next();
            forVariable.setValue(foreachData);
            bForHasNext = iter.hasNext();  //an element after it?
            cont = forExecuter.execute(subContent, out, indentOut, forExecuter.localVariables, nDebug);
          }
        }//while of for-loop
      }
      else if(container !=null && container instanceof Map<?,?>){
        Map<?,?> map = (Map<?,?>)container;
        Set<?> entries = map.entrySet();
        Iterator<?> iter = entries.iterator();
        while(cond && !forExecuter.isBreak() && iter.hasNext()){
          cond = (cont == kSuccess);
          if(cond && statement.condition !=null){
            cond = evalCondition(statement.condition);
          }
          if(cond){
            Map.Entry<?, ?> foreachDataEntry = (Map.Entry<?, ?>)iter.next();
            Object foreachData = foreachDataEntry.getValue();
            forVariable.setValue(foreachData);
            bForHasNext = iter.hasNext();  //an element after it?
            cont = forExecuter.execute(subContent, out, indentOut, forExecuter.localVariables, nDebug);
          }
        }
      }
      else if(container !=null && container.getClass().isArray()){
        Object[] aContainer = (Object[])container;
        int zContainer = aContainer.length;
        int iContainer = -1;
        while(cond && !forExecuter.isBreak() && ++iContainer < zContainer){
          cond = (cont == kSuccess);
          if(cond && statement.condition !=null){
            cond = evalCondition(statement.condition);
          }
          if(cond){
            Object foreachData = aContainer[iContainer];
            forVariable.setValue(foreachData);
            bForHasNext = iContainer < zContainer-1;
            cont = forExecuter.execute(subContent, out, indentOut, forExecuter.localVariables, nDebug);
          }
        }
      }
      bForHasNext = bForHasNextOld;  //restore for nested for loops.
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level. It breaks only the own one.
      return cont;
    }
    
    
    
    /**Executes the statements or output the text if in textArg if a for-container has a next element.
     * 
     * @param statement the hasnext-Statement. 
     * @param out
     * @param indentOut
     * @param nDebug
     * @return
     * @throws Exception
     */
    short exec_hasNext(JZcmdScript.JZcmditem statement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception{
      short cont = kSuccess;
      if(this.bForHasNext){
        JZcmdScript.StatementList statementList = statement.statementlist();
        if(statementList !=null){
          cont = execute(statement.statementlist(), out, indentOut, localVariables, nDebug);
        } 
        else if(statement.textArg !=null){
          executeText(statement, out, indentOut);
        }
      }
      return cont;
    }
    
    
    
    /**it contains maybe more as one if block and else. 
     * @throws Exception */
    short executeIfStatement(JZcmdScript.IfStatement ifStatement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception{
      short cont = kFalse;
      Iterator<JZcmdScript.JZcmditem> iter = ifStatement.statementlist.statements.iterator();
      //boolean found = false;  //if block found
      while(iter.hasNext() && cont == kFalse ){
        JZcmdScript.JZcmditem statement = iter.next();
        switch(statement.elementType()){
          case 'g': { //if-block
            //boolean hasNext = iter.hasNext();
            cont = executeIfBlock((JZcmdScript.IfCondition)statement, out, indentOut, nDebug);
          } break;
          case 'E': { //elsef
            cont = execute(statement.statementlist, out, indentOut, localVariables, nDebug);
          } break;
          default:{
            throw new IllegalArgumentException("JZcmd.execIf - unknown statement; " + statement.elementType());
          }
        }//switch
      }//for
      if(cont == kBreak){ cont = kBreak; }   //break in an if block should break at calling level especially in for-container
      if(cont == kFalse){ cont = kSuccess; }
      return cont;
    }
    
    
    
    /**Executes a while statement. 
     * @throws Exception */
    short whileStatement(JZcmdScript.CondStatement whileStatement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception 
    {
      short cont = kSuccess;
      boolean cond;
      do{
        cond =  (cont ==kSuccess)
             && evalCondition(whileStatement.condition); //.calcDataAccess(localVariables);
        if(cond){
          cont = execute(whileStatement.statementlist, out, indentOut, localVariables, nDebug);
        }
      } while(cond);  //if executed, check cond again.  
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    /**Executes a dowhile statement. 
     * @throws Exception */
    short dowhileStatement(JZcmdScript.CondStatement whileStatement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception 
    { short cont;
      boolean cond;
      do{
        cont = execute(whileStatement.statementlist, out, indentOut, localVariables, nDebug);
        cond =  (cont ==kSuccess)
             && evalCondition(whileStatement.condition); //.calcDataAccess(localVariables);
      } while(cond);  //if executed, check cond again.  
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    /**Checks the condition and executes the if-block if the condition is true.
     * If the condition contains elements which are not found in the datapath (throwing {@link NoSuchElementException}),
     * the condition is false, That exception is not thrown forward.
     * @param ifBlock
     * @param out
     * @param bIfHasNext
     * @return true if the condition is true. If it returns false, an elsif or else-Block should be executed.
     * @throws Exception
     */
    short executeIfBlock(JZcmdScript.IfCondition ifBlock, StringFormatter out, int indentOut, int nDebug) 
    throws Exception
    { short cont;
      //Object check = getContent(ifBlock, localVariables, false);
      
      //CalculatorExpr.Value check;
      boolean bCheck;
      bCheck = evalCondition(ifBlock.condition); //.calcDataAccess(localVariables);
      if(bCheck){
        cont = execute(ifBlock.statementlist, out, indentOut, localVariables, nDebug);
      } else cont = kFalse;
      return cont;
    }
    
    
    
    
    
    
    /**Invocation for <+name>text<.+>.
     * It gets the Appendable from the assign variable
     * and executes {@link #execute(org.vishia.cmd.JZcmdScript.StatementList, Appendable, boolean)}
     * with it.
     * @param statement the statement
     * @throws Exception 
     */
    short textAppendToOut(JZcmdScript.JZcmditem statement, StringFormatter out, int nDebug) throws Exception
    { short ret;
      if(statement.statementlist !=null){
        //executes the statement, use the Appendable to output immediately
        synchronized(out){
          ret = execute(statement.statementlist, out, statement.srcColumn, localVariables, nDebug);
        }
      } else {
        //Any other text expression
        CharSequence text = evalString(statement);
        ret = text == JZcmdExecuter.retException ? kException : kSuccess;
        if(text !=null){
          synchronized(out){
            out.append(text);
          }
        }
      }
      //if(bShouldClose){
      //  out1.close();
      //}
      return ret;
    }
    
    

    
    
    
    
    
    /**Invocation for <+name>text<.+>.
     * It gets the Appendable from the assign variable
     * and executes {@link #execute(org.vishia.cmd.JZcmdScript.StatementList, Appendable, boolean)}
     * with it.
     * @param statement the statement
     * @throws Exception 
     */
    short textAppendToVar(JZcmdScript.TextOut statement, int nDebug) throws Exception
    { StringFormatter out1;
      //boolean bShouldClose;
      short ret;
      if(statement.variable !=null){
        Object chn;
        //Object oVar = DataAccess.access(statement.variable.datapath(), null, localVariables, bAccessPrivate, false, true, null);
        Object oVar = dataAccess(statement.variable,localVariables, bAccessPrivate, false, true, null);
        if(oVar instanceof DataAccess.Variable<?>){
          @SuppressWarnings("unchecked")
          DataAccess.Variable<Object> var = (DataAccess.Variable<Object>) oVar;
          chn = var.value();
          if(chn == null && var.type() == 'A'){
            chn = new StringPartAppend();
            var.setValue(chn);            //Creates a new StringPartAppend for an uninitialized variable. Stores there.
          }
        } else {
          //it is not a variable, can be direct stored Appendable:
          chn = oVar;
        }
        if(chn instanceof StringFormatter){
          out1 = (StringFormatter)chn;
          //bShouldClose = false;
        } else if((chn instanceof Appendable)) {
          out1 = new StringFormatter((Appendable)chn, chn instanceof Closeable, "\n", 200);  //append, it may be a StringPartAppend.
          //bShouldClose = true;
        } else {
          throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
          out1 = new StringFormatter();  //NOTE: it is a dummy because the statement above throws.
          //bShouldClose = false;
        }
      } else {
        out1 = textout;  //output to the text output.
        //bShouldClose = false;
      }
      if(statement.statementlist !=null){
        //executes the statement, use the Appendable to output immediately
        synchronized(out1){
          ret = execute(statement.statementlist, out1, statement.srcColumn, localVariables, nDebug);
        }
      } else {
        //Any other text expression
        CharSequence text = evalString(statement);
        ret = text == JZcmdExecuter.retException ? kException : kSuccess;
        if(text !=null){
          synchronized(out1){
            out1.append(text);
          }
        }
      }
      //if(bShouldClose){
      //  out1.close();
      //}
      return ret;
    }
    
    

    
    
    
    
    
    private short execCall(JZcmdScript.CallStatement callStatement, List<DataAccess.Variable<Object>> additionalArgs
        , StringFormatter out, int indentOut, int nDebug) 
    throws IllegalArgumentException, Exception
    { short success = kSuccess;
      final CharSequence nameSubtext;
      /*
      if(statement.name == null){
        //subtext name gotten from any data location, variable name
        Object oName = ascertainText(statement.expression, localVariables); //getContent(statement, localVariables, false);
        nameSubtext = DataAccess.getStringFromObject(oName, null);
      } else {
        nameSubtext = statement.name;
      }*/
      nameSubtext = evalString(callStatement.call_Name); 
      JZcmdScript.Subroutine subroutine = jzClass.subroutines.get(nameSubtext);
      if(subroutine == null) { //not found in this class:    
        subroutine = jzcmdScript.getSubroutine(nameSubtext);  //the subtext script to call
      }
      if(subroutine == null){
        throw new NoSuchElementException("JbatExecuter - subroutine not found; " + nameSubtext);
      } else {
        //TODO use execSubroutine, same code!
        final ExecuteLevel sublevel;
        JZcmdScript.JZcmdClass subClass = (JZcmdScript.JZcmdClass)subroutine.parentList;
        if(subroutine.useLocals) {  //TODO check whether the subClass == this.jzclass 
          sublevel = this; 
        } else { 
          sublevel = new ExecuteLevel(subClass, threadData, this, subroutine.useLocals ? localVariables : null); 
        }
        success = execSubroutine(subroutine, sublevel, callStatement.actualArgs, additionalArgs, out, indentOut, nDebug);
        if(success == kSuccess){
          if(callStatement.variable !=null || callStatement.assignObjs !=null){
            DataAccess.Variable<Object> retVar = sublevel.localVariables.get("return");
            Object value = retVar !=null ? retVar.value() : null;
            assignObj(callStatement, value, false);
          }
          
        }
      }
      return success;
    }
    

    
    
    public Object evalSubroutine(JZcmdScript.Subroutine substatement
        , Map<String, DataAccess.Variable<Object>> args
        , StringFormatter out, int indentOut
    ) throws ScriptException 
    {
      short ok;
      try { 
        ok = execSubroutine(substatement, args, null, -1);
        //executer.execute(genScript, true, bWaitForThreads, null, null);
        //zgenExecuteLevel.execute(genScript.getMain().subContent, u, false);
      } catch (Exception exc) {
        throw new ScriptException(exc);
      }
      if(ok != JZcmdExecuter.kSuccess){
        throw new ScriptException(threadData.exception.getMessage(), threadData.excSrcfile, threadData.excLine, threadData.excColumn);
      }
      return null;
    }    
    
    
    
    /**Executes a subroutine invoked from outside of this class
     * @param substatement Statement of the subroutine
     * @param args Any given arguments in form of a map.
     * @param out output
     * @param indentOut
     * @return null on success, an error message on parameter error
     * @throws Exception
     */
    public short execSubroutine(JZcmdScript.Subroutine subroutine
        , Map<String, DataAccess.Variable<Object>> args
        , StringFormatter out, int indentOut
    )  
    {
      final ExecuteLevel sublevel;
      JZcmdScript.JZcmdClass subClass = (JZcmdScript.JZcmdClass)subroutine.parentList;
      if(subroutine.useLocals) {  //TODO check whether the subClass == this.jzclass 
        sublevel = this; 
      } else { 
        sublevel = new ExecuteLevel(subClass, threadData, this, subroutine.useLocals ? localVariables : null); 
      }
      final List<DataAccess.Variable<Object>> arglist;
      if(args !=null){
        arglist = new LinkedList<DataAccess.Variable<Object>>();
        for(Map.Entry<String, DataAccess.Variable<Object>> entry: args.entrySet()){
          arglist.add(entry.getValue());
        }
      } else {
        arglist = null;
      }
      short success;
      try{
        success = execSubroutine(subroutine, sublevel, null, arglist, out, indentOut, -1);
      } catch(Exception exc){
        success = kException;
      }
      return success;
    }
    
    
    private short execSubroutine(JZcmdScript.Subroutine subtextScript
        , ExecuteLevel sublevel
        , List<JZcmdScript.Argument> actualArgs
        , List<DataAccess.Variable<Object>> additionalArgs
        , StringFormatter out, int indentOut, int nDebug
    ) throws Exception
    {
      //String error = null;
      short success = kSuccess;
      if(subtextScript.formalArgs !=null){
        //
        //build a Map temporary to check which arguments are used:
        //
        TreeMap<String, JZcmdScript.DefVariable> check = new TreeMap<String, JZcmdScript.DefVariable>();
        for(JZcmdScript.DefVariable formalArg: subtextScript.formalArgs) {
          check.put(formalArg.getVariableIdent(), formalArg);
        }
        //
        //process all actual arguments:
        //
        if(actualArgs !=null){
          for( JZcmdScript.Argument actualArg: actualArgs){  //process all actual arguments
            Object ref;
            ref = evalObject(actualArg, false);
            JZcmdScript.DefVariable checkArg = check.remove(actualArg.getIdent());      //is it a requested argument (per name)?
            if(checkArg == null){
              throw new IllegalArgumentException("execSubroutine - unexpected argument; "  + actualArg.identArgJbat);
            } else {
              char cType = checkArg.elementType();
              //creates the argument variable with given actual value and the requested type in the sub level.
              switch(cType){
                case 'F': ref = convert2FilePath(ref); 
              }
              DataAccess.createOrReplaceVariable(sublevel.localVariables, actualArg.identArgJbat, cType, ref, false);
            }
          }
        }
        //
        //process additional arguments
        //
        if(additionalArgs !=null){
          for(DataAccess.Variable<Object> arg: additionalArgs){
            String name = arg.name();
            JZcmdScript.DefVariable checkArg = check.remove(name);      //is it a requested argument (per name)?
            if(checkArg == null){
              throw new IllegalArgumentException("execSubroutine - unexpected additial argument; "  + name);
            } else {
              char cType = checkArg.elementType();
              //creates the argument variable with given actual value and the requested type in the sub level.
              DataAccess.createOrReplaceVariable(sublevel.localVariables, name, cType, arg.value(), false);
            }
          }
        }
        //check whether all formal arguments are given with actual args or get its default values.
        //if not all variables are correct, write error.
        for(Map.Entry<String, JZcmdScript.DefVariable> checkArg : check.entrySet()){
          JZcmdScript.DefVariable arg = checkArg.getValue();
          //Generate on scriptLevel (classLevel) because the formal parameter list should not know things of the calling environment.
          Object ref = scriptLevel.evalObject(arg, false);
          String name = arg.getVariableIdent();
          char cType = arg.elementType();
          if(cType == 'F' && !(ref instanceof JZcmdFilepath) ){
            ref = new JZcmdFilepath(this, ref.toString());
          }
          //creates the argument variable with given default value and the requested type.
          DataAccess.createOrReplaceVariable(sublevel.localVariables, name, cType, ref, false);
        }
      } else if(actualArgs !=null){
        throw new IllegalArgumentException("execSubroutine -  not expected arguments");
      }
      success = sublevel.execute(subtextScript.statementlist, out, indentOut, sublevel.localVariables, nDebug);
      return success;
    }
    
    
    
    /**Executes a Zmake subroutine call. Additional to {@link #execCall(org.vishia.cmd.JZcmdScript.CallStatement, List, StringFormatter, int, int)}
     * a {@link ZmakeTarget} will be prepared and stored as 'target' in the localVariables of the sublevel.
     * @param statement
     * @param out
     * @param indentOut
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private short exec_zmake(JZcmdScript.Zmake statement, StringFormatter out, int indentOut, int nDebug) 
    throws IllegalArgumentException, Exception {
      ZmakeTarget target = new ZmakeTarget(this, statement.name);
      Object oOutput = evalObject(statement.jzoutput, false);
      target.output = convert2FilePath(oOutput); 
      //target.output = new JZcmdFilepath(this, statement.output);  //prepare to ready-to-use form.
      for(JZcmdScript.AccessFilesetname input: statement.input){
        JZcmdAccessFileset zinput = new JZcmdAccessFileset(input, input.filesetVariableName, this);
        if(target.inputs ==null){ target.inputs = new ArrayList<JZcmdAccessFileset>(); }
        target.inputs.add(zinput);
      }
      //Build a temporary list only with the 'target=target' as additionalArgs for the subroutine call
      List<DataAccess.Variable<Object>> args = new LinkedList<DataAccess.Variable<Object>>();
      DataAccess.Variable<Object> targetV = new DataAccess.Variable<Object>('O',"target", target, true);
      args.add(targetV);
      //
      //same as a normal subroutine.
      return execCall(statement, args, out, indentOut, nDebug);
    }
    
    
    
    
    
    
    
    
    
    /**executes statements in another thread.
     * @throws Exception 
     */
    private short executeThread(Map<String, DataAccess.Variable<Object>> newVariables, JZcmdScript.ThreadBlock statement) 
    throws Exception
    { final JZcmdThread thread;
      final String name;
      if(statement.threadVariable !=null){
        try{
          thread = new JZcmdThread();
          name = statement.threadVariable.idents().toString();
          storeValue(statement.threadVariable, newVariables, thread, bAccessPrivate);
        } catch(Exception exc){
          throw new IllegalArgumentException("JZcmd - thread assign failure; path=" + statement.threadVariable.toString());
        }
      } else {
        thread = new JZcmdThread();  //without assignment to a variable.
        name = "JZcmd";
      }
      ExecuteLevel threadLevel = new ExecuteLevel(jzClass, thread, this, localVariables);
      synchronized(threads){
        threads.add(thread);
      }
      thread.startThread(name, threadLevel, statement);
      //it does not wait on finishing this thread.
      return kSuccess;
    }

    
    
    
    /**Executes an internal statement block. If the Statementlist has local variables, a new
     * instance of this is created. Elsewhere the same instance is used.
     * 
     * @param script
     * @param out
     * @return continue information for the calling level, 
     *   see return of {@link #execute(org.vishia.cmd.JZcmdScript.StatementList, Appendable, int, boolean)}
     *   A {@link JZcmdExecuter#kBreak} inside the statementlist is not returned. The break is only valid
     *   inside the block. All other return values of execute are returned.
     * @throws IOException
     */
    private short execNestedLevel(JZcmdScript.JZcmditem script, StringFormatter out, int indentOut, int nDebug) 
    throws Exception
    {
      ExecuteLevel genContent;
      if(false && script.statementlist.bContainsVariableDef){
        genContent = new ExecuteLevel(jzClass, threadData, this, localVariables);
      } else {
        genContent = this;  //don't use an own instance, save memory and calculation time.
      }
      short ret = genContent.execute(script.statementlist, out, indentOut, genContent.localVariables, nDebug);
      if(ret == kBreak){ 
        ret = kSuccess; 
      }
      return ret;
    }

    
    
    private void exec_cmdline(JZcmdScript.CmdInvoke statement) 
    throws IllegalArgumentException, Exception
    {
      boolean ok = true;
      final CharSequence sCmd;
      if(statement.textArg == null){
        //cmd gotten from any data location, variable name
        sCmd = evalString(statement); 
      } else {
        sCmd = statement.textArg;
      }
      List<String> args = new ArrayList<String>();
      args.add(sCmd.toString());
      if(statement.cmdArgs !=null){
        int iArg = 1;
        for(JZcmdScript.JZcmditem arg: statement.cmdArgs){
          if(arg.elementType == 'L'){
            Object oVal = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
            if(oVal instanceof List<?>){
              @SuppressWarnings("unchecked")
              List<Object> arglist = (List<Object>)oVal;
              for(Object oArg: arglist){
                args.add( oArg.toString());
              }
            } else {
              
            }
          } else {
            String sArg = evalString(arg).toString(); 
            args.add(sArg);
          }
        }
      }
      if(statement.bCmdCheck){
        setLocalVariable("argsCheck", 'L', args, true);
      }
      //
      //The standard output of the cmd line invocation is written into destination variable.
      //Gather a list of Appendable for the execution. 
      //
      List<Appendable> outCmd;
      if(statement.variable !=null){
        outCmd = new LinkedList<Appendable>();
        Object oOutCmd = dataAccess(statement.variable, localVariables, bAccessPrivate, false, false, null);
        //Object oOutCmd = statement.variable.getDataObj(localVariables, bAccessPrivate, false);
        if(oOutCmd instanceof Appendable){
          outCmd.add((Appendable)oOutCmd);
        } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
        }
        if(statement.assignObjs !=null){
          for(JZcmdScript.JZcmdDataAccess assignObj1 : statement.assignObjs){
            oOutCmd = dataAccess(assignObj1, localVariables, bAccessPrivate, false, false, null);
            //oOutCmd = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
            if(oOutCmd instanceof Appendable){
              outCmd.add((Appendable)oOutCmd);
            } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
            }
        } }
      } else {
        outCmd = null;
      }
      
      if(cmdExecuter == null){ 
        cmdExecuter = new CmdExecuter(); 
        Map<String,String> env = cmdExecuter.environment();
        Iterator<DataAccess.Variable<Object>> iter = localVariables.iterator("$");
        boolean cont = true;
        //
        //gather all variables starting with "$" as environment variable. 
        //
        while(cont && iter.hasNext()){
          DataAccess.Variable<Object> variable = iter.next();
          String name = variable.name();
          Object oValue = variable.value(); 
          if(name.startsWith("$") && oValue !=null){
            String value = oValue.toString();
            env.put(name.substring(1), value);
          } else {
            cont = false;
          }
        }
        cmdExecuter.setCurrentDir(currdir);
      }
      
      //
      //currdir
      //
      //
      //execute, run other process on operation system. With or without wait.
      //
      String[] sArgs = new String[args.size()]; 
      args.toArray(sArgs);
      this.cmdErrorlevel = cmdExecuter.execute(sArgs, statement.bShouldNotWait, null, outCmd, null);
      //
      //close
      //
    }
    

    short execCmdError(JZcmdScript.Onerror statement, StringFormatter out, int indentOut) throws Exception {
      short ret = 0;
      if(this.cmdErrorlevel >= statement.errorLevel){
        ret = execute(statement.statementlist, out, indentOut, localVariables, -1);
      }
      return ret;
    }



    short executeChangeCurrDir(JZcmdScript.JZcmditem statement)
    throws Exception
    {
      /*if(statement.dataAccess !=null){
        Object oDir = dataAccess(statement.dataAccess, localVariables, bAccessPrivate, false, false, null);
        if(oDir instanceof File){
          
        }
      } else */{
        CharSequence arg = evalString(statement);
        if(arg == JZcmdExecuter.retException){ return kException; }
        else {
          changeCurrDir(arg); 
          return kSuccess;
        }
      }
    }

    
    /**Executes the cd command: changes the directory in this execution level.
     * @param arg maybe a relative path. If it is a StringBuilder, it will be changed on normalizePath.
     * @throws IllegalAccessException 
     */
    protected void changeCurrDir(CharSequence arg) throws IllegalAccessException 
    {
      final CharSequence arg1;
      boolean absPath = FileSystem.isAbsolutePathOrDrive(arg);
      if(absPath){
        //Change the content of the currdir to the absolute directory.
        arg1 = FileSystem.normalizePath(arg);
      } else {
        if(this.currdir == null){
          //only on startup, start with operation system's current directory.
          this.currdir = new File("").getAbsoluteFile();
        }
        StringBuilder sCurrdir = new StringBuilder();
        sCurrdir.append(currdir.getPath()).append(arg);
        arg1 = FileSystem.normalizePath(sCurrdir);
      }
      this.currdir = new File(arg1.toString());
      if(!currdir.exists() || !currdir.isDirectory()){
        throw new IllegalArgumentException("JZcmdExecuter - cd, dir not exists; " + arg);
      }
      setLocalVariable("currdir", 'O', currdir, true);
      //NOTE: don't do so, because it is global for the JVM (not desired!) but does not affect the operation system.
      //it has not any sense.
      //String sOldDir = System.setProperty("user.dir", currdir.getAbsolutePath());
      
    }
    
     
    
    
    
    
    
    /**Inserts <*a_datapath> in the out.
     * @param statement
     * @param out
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private short executeDatatext(JZcmdScript.DataText statement, Appendable out)  //<*datatext>
    throws IllegalArgumentException, Exception
    {
      CharSequence text = "??";
      Object obj = dataAccess(statement.dataAccess, localVariables, bAccessPrivate, false, false, null);
      if(obj == JZcmdExecuter.retException){ return kException; }
      else {
        
        //Object obj = statement.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(statement.format !=null){ //it is a format string:
            if(obj instanceof CalculatorExpr.Value){
              obj = ((CalculatorExpr.Value)obj).objValue();  
              //converted to boxed numeric if numeric.
              //boxed numeric is necessary for format
            }
            
            text = String.format(JZcmdExecuter.this.locale,  statement.format, obj);
        } else if(obj==null){ 
          text = null; //don't append if obj hasn't a content. 
        } else if (obj instanceof CharSequence){
          text = (CharSequence)obj;
        } else {
          text = obj.toString();
        }
        if(text!=null){ out.append(text); }
        return kSuccess;
      }
    }

    
    
    void executeMove(JZcmdScript.CallStatement statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence s1 = evalString(statement.actualArgs.get(0));
      CharSequence s2 = evalString(statement.actualArgs.get(1));
      File fileSrc = new File(s1.toString());
      File fileDst = new File(s2.toString());
      boolean bOk = fileSrc.renameTo(fileDst);
      if(!bOk) throw new IOException("JbatchExecuter - move not successfully; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
    void executeCopy(JZcmdScript.CallStatement statement) 
    throws Exception
    { String src, dst;
      Object osrc = evalObject(statement.actualArgs.get(0), false);
      if(osrc instanceof JZcmdFilepath){
        src = ((JZcmdFilepath)osrc).absfile().toString();
      } else {
        src = osrc.toString();
      }
      Object odst = evalObject(statement.actualArgs.get(1), false);
      if(odst instanceof JZcmdFilepath){
        dst = ((JZcmdFilepath)odst).absfile().toString();
      } else {
        dst = odst.toString();
      }
      //CharSequence s1 = evalString(statement.actualArgs.get(0));
      //CharSequence s2 = evalString(statement.actualArgs.get(1));
      File fileSrc = new File(src);
      File fileDst = new File(dst);
      int nrofBytes = FileSystem.copyFile(fileSrc, fileDst);
      if(nrofBytes <0) throw new FileNotFoundException("JbatchExecuter - copy src not found; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
    void executeDelete(JZcmdScript.CallStatement statement) 
    throws Exception
    {
      CharSequence s1 = evalString(statement.actualArgs.get(0));
      boolean isDeleted = FileSystem.delete(s1.toString());
      if(!isDeleted) throw new FileNotFoundException("JbatchExecuter - del not possible; " + s1);;
    }
    
    /**Creates a new FileWriter with the given name {@link #evalString(org.vishia.cmd.JZcmdScript.Argument)}
     * of the statement. If the file name is local, the actual value of $CD is set as pre-path.
     * The current directory of the file system is not proper to use because the current directory of this 
     * execution level should be taken therefore. If the path is faulty or another exception is thrown,
     * the exception is forwarded to the execution level (onerror-statement).
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    short executeOpenfile(Map<String, DataAccess.Variable<Object>> newVariables, JZcmdScript.DefVariable statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence sqFilename = evalString(statement);
      if(sqFilename == JZcmdExecuter.retException){ return kException; }
      else {
        String sFilename = sqFilename.toString();
        Writer writer;
        if(!FileSystem.isAbsolutePath(sFilename)){
          //build an absolute filename with $CD, the current directory of the file system is not proper to use.
          
          @SuppressWarnings("unused")
          //sFilename = cd + "/" + sFilename;
          File fWriter = new File(currdir, sFilename);
          writer = new FileWriter(fWriter);
        } else {
          writer = new FileWriter(sFilename);  //given absolute path
        }
        storeValue(statement.defVariable, newVariables, writer, bAccessPrivate);
        //setLocalVariable(statement.identArgJbat, 'A', writer);
        return kSuccess;
      }
    }
    
    
    
    private short assignStatement(JZcmdScript.JZcmditem statement) throws IllegalArgumentException, Exception{
      //Object val = evalObject(statement, false);
      return assignObj((JZcmdScript.AssignExpr)statement, null, true); //val); 
    }
    
    
    
    
    
    /**Executes a <code>assignment::= [{ < datapath?assign > = }] < expression > ;.</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param statement
     * @param val value to assign. If not null then bEval should be false.
     * @param bEval if true then the value will be evaluate from the statement.
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private short assignObj(JZcmdScript.AssignExpr statement, Object val, boolean bEval) 
    throws IllegalArgumentException, Exception
    {
      CalculatorExpr.Value value = null;
      Object oVal = val;
      short ret = kSuccess;
      Boolean cond = null;
      JZcmdScript.JZcmdDataAccess assignObj1 = statement.variable;
      if(assignObj1 ==null){
        //no assignment, only an expression which is a procedure call:
        Object oRet = evalObject(statement, false);
        if(oRet == JZcmdExecuter.retException){ ret = kException; }
        else { ret = kSuccess; }
      }
      Iterator<JZcmdScript.JZcmdDataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        //Object dst = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
        DataAccess.Dst dstField = new DataAccess.Dst();
        //List<DataAccess.DatapathElement> datapath = assignObj1.datapath(); 
        //Object dst = DataAccess.access(datapath, null, localVariables, bAccessPrivate, false, true, dstField);
        Object dst = dataAccess(assignObj1,localVariables, bAccessPrivate, false, true, dstField);
        if(dst instanceof DataAccess.Variable<?>){
          @SuppressWarnings("unchecked")
          DataAccess.Variable<Object> var = (DataAccess.Variable<Object>) dst; //assignObj1.accessVariable(localVariables, bAccessPrivate);
          char vartype = var.type();
          switch(vartype){
            case 'K': if(value == null){ value = evalValue(statement, false); } break;
            case 'Q': if(cond == null){ cond = new Boolean(evalCondition(statement)); } break;
            default: if(oVal == null){ oVal = evalObject(statement, false); }
          }
          
          dst = var.value();
          switch(var.type()){
            case 'A': {
              throwIllegalDstArgument("assign to appendable faulty", assignObj1, statement); 
            } break;
            case 'U': {
              assert(dst instanceof StringPartAppend);            
              StringPartAppend u = (StringPartAppend) dst;
              u.clear();
              Object ocVal = oVal == null ? "--null--" : oVal;
              if(!(ocVal instanceof CharSequence)){
                ocVal = ocVal.toString();
              }
              u.append((CharSequence)ocVal);
            } break;
            case 'S':{
              if(oVal == null || val instanceof String || val instanceof StringSeq && ((StringSeq)val).isUnmated()){
                var.setValue(oVal);
              } else {
                var.setValue(oVal.toString());
              }
            } break;
            case 'K': var.setValue(value); break;
            case 'Q': var.setValue(cond); break;
            default:{
              var.setValue(oVal);   //sets the value to the variable.
            }
          }//switch
        } else {
          //check whether the field is compatible with val
          if(oVal == null){ 
            oVal = evalObject(statement, false); 
            if(oVal == JZcmdExecuter.retException){ ret = kException; }
          }
          dstField.set(oVal);
          //DataAccess.storeValue(assignObj1.datapath(), newVariables, val, bAccessPrivate);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
      return ret;
    }
    
    
    
    
    
    
    /**Executes a <code>DefObjVar::= < variable?defVariable>  [ = < objExpr?>]</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    short defineExpr(Map<String, DataAccess.Variable<Object>> newVariables, JZcmdScript.DefVariable statement) 
    throws IllegalArgumentException, Exception
    {
      Object init = evalObject(statement, false);  //maybe null
      if(init == JZcmdExecuter.retException){
        return kException;
      } else {
        Object val;
        switch(statement.elementType()){
          case 'U': {
            if(init == null){
              val = new StringPartAppend();
            } else {
              CharSequence init1 = init instanceof CharSequence ? (CharSequence)init : init.toString();
              StringPartAppend u = new StringPartAppend();
              u.append(init1);
              val = u;
            }
          } break;
          case 'S':{
            if(init == null || init instanceof String /*|| init instanceof StringSeq && ((StringSeq)init).isUnmated()*/){
              val = init;
            } else {
              val = init.toString();
            }
          } break;
          default: val = init;
        }
        //DataAccess.Variable var = (DataAccess.Variable)DataAccess.access(statement.defVariable.datapath(), null, localVariables, bAccessPrivate, false, true, null);
        //var.setValue(val);
        List<DataAccess.DatapathElement> datapath = statement.defVariable.datapath();
        if(datapath.get(0).ident().equals("return") && !localVariables.containsKey("return")) {
          //
          //creates the local variable return on demand:
          DataAccess.Variable<Object> ret = new DataAccess.Variable<Object>('M', "return", new_Variables());
          localVariables.add("return", ret);
        }
        storeValue(statement.defVariable, newVariables, val, bAccessPrivate);
        if(cmdExecuter !=null){
          String name = statement.defVariable.datapath().get(0).ident();
          if(name.startsWith("$")){
            //an environment variable
            cmdExecuter.environment().put(name,val.toString());
        } }
        return kSuccess;
      }
    }
    
    
    
    /**Executes a <code>appendExpr::= [{ < datapath?assign > += }] < expression > ;.</code>.
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    short appendExpr(JZcmdScript.AssignExpr statement) 
    throws IllegalArgumentException, Exception
    {
      short ret;
      Object val = evalObject(statement, false);
      if(val == JZcmdExecuter.retException){ ret = kException; }
      else { ret = kSuccess; }
      JZcmdScript.JZcmdDataAccess assignObj1 = statement.variable;
      Iterator<JZcmdScript.JZcmdDataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        Object dst = dataAccess(assignObj1, localVariables, bAccessPrivate, false, false, null);
        //Object dst = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
        if(dst instanceof Appendable){
          if(!(val instanceof CharSequence)){
            val = val.toString();
          }
          ((Appendable) dst).append((CharSequence)val);
        } else if(dst instanceof List<?>){
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>)dst; 
          if(val instanceof List<?>){
            for(Object entry: (List<Object>)val){
              list.add(entry);
            }
          } else {
            list.add(val);
          }
        } else {
          throwIllegalDstArgument("dst should be Appendable", assignObj1, statement);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
      return ret;
    }
    
    
    
    void execThrow(JZcmdScript.JZcmditem statement) throws Exception {
      CharSequence msg = evalString(statement);
      throw new JZcmdThrow(msg.toString());
    }
    
    
    
    /**Throws an {@link CmdErrorLevelException} if the errorlevel is >= statement.errorLevel.
     * @param statement
     * @throws CmdErrorLevelException
     */
    void execThrowonerror(JZcmdScript.Onerror statement) throws CmdErrorLevelException{
      if(cmdErrorlevel >= statement.errorLevel){
        throw new CmdErrorLevelException(cmdErrorlevel);
      }
    }
    
    
    /**Checks either the {@link JZcmdScript.Argument#dataAccess} or, if it is null,
     * the {@link JZcmdScript.Argument#expression}. Returns either the Object which is gotten
     * by the {@link DataAccess#access(Map, boolean, boolean)} or which is calculated
     * by the expression. Returns an instance of {@link CalculatorExpr.Value} if it is 
     * a result by an expression.
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalDatapathOrExpr(JZcmdScript.Argument arg) throws Exception{
      if(arg.dataAccess !=null){
        Object o = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o; }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        if(value.isObjValue()){ return value.objValue(); }
        else return value;
      } else throw new IllegalArgumentException("JZcmd - unexpected, faulty syntax");
    }
    
    
    

    
    public CharSequence evalString(JZcmdScript.JZcmditem arg) 
    throws Exception 
    {
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        Object o = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else if(o instanceof CharSequence){ return (CharSequence)o; }  //maybe retException
        else {return o.toString(); }
      } else if(arg.statementlist !=null){
        StringFormatter u = new StringFormatter();
        //StringPartAppend u = new StringPartAppend();
        short ret = executeNewlevel(jzClass, arg.statementlist, u, 0, -1);
        if(ret == kException){ return JZcmdExecuter.retException; }
        else { return u.getBuffer(); }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        return value.stringValue();
      } else return null;  //it is admissible.
      //} else throw new IllegalArgumentException("JZcmd - unexpected, faulty syntax");
    }
    
    
    
    /**Accesses the data. Before execution all arguments of methods inside the {@link #datapath()}
     * are calculated with the capability of JZcmd.
     * @param dataPool
     * @param accessPrivate
     * @param bContainer
     * @param bVariable
     * @param dst
     * @return
     * @throws Exception
     */
    Object dataAccess(JZcmdScript.JZcmdDataAccess dataAccess
    , Map<String, DataAccess.Variable<Object>> dataPool
    , boolean accessPrivate
    , boolean bContainer
    , boolean bVariable
    , DataAccess.Dst dst
    ) throws Exception {
      /*if(dataAccess.filepath !=null){
        //Special case in JZcmd: a "File: "
        if(FileSystem.isAbsolutePath(dataAccess.filepath)){
          return new File(dataAccess.filepath);
        } else {
          return new File(currdir, dataAccess.filepath);
        }
      } else */{
        calculateArguments(dataAccess);
        return DataAccess.access(dataAccess.datapath(), dataPool, accessPrivate, bContainer, bVariable, dst);
      }
    }
      
    
    
    void storeValue(DataAccess dataAccess
    , Map<String, DataAccess.Variable<Object>> dataPool
    , Object value
    , boolean accessPrivate
    )throws Exception {
      calculateArguments(dataAccess);
      dataAccess.storeValue(dataPool, value, accessPrivate);
    }

    
    
    private CalculatorExpr.Value calculateExpression(CalculatorExpr expr) 
    throws Exception {
      for(CalculatorExpr.Operation operation: expr.listOperations()){
        DataAccess datapath = operation.datapath();
        if(datapath !=null){
          calculateArguments(datapath);
        }
      }
      return expr.calcDataAccess(localVariables);
    }
    
    
    
    
    private void calculateArguments(DataAccess dataAccess) throws Exception {
      if(debug_dataAccessArguments){
        debug(); //set breakpoint into!
        debug_dataAccessArguments = false;
      }
      for(DataAccess.DatapathElement dataElement : dataAccess.datapath()){  //loop over all elements of the path with or without arguments.
        //check all datapath elements whether they have method calls with arguments:
        List<JZcmdScript.JZcmditem> fnArgsExpr = null;

        if(  dataElement instanceof JZcmdScript.JZcmdDatapathElementClass){
          JZcmdScript.JZcmdDatapathElementClass jzcmdDataElement = (JZcmdScript.JZcmdDatapathElementClass)dataElement;
          if(jzcmdDataElement.dpathLoader !=null){
            Object oLoader = dataAccess(jzcmdDataElement.dpathLoader, localVariables, bAccessPrivate, false, false, null);
            assert(oLoader instanceof ClassLoader);
            jzcmdDataElement.set_loader((ClassLoader)oLoader);
          }
          if(jzcmdDataElement.dpathClass !=null){
            Object o = dataAccess(jzcmdDataElement.dpathClass, localVariables, bAccessPrivate, false, false, null);
            assert(o instanceof Class<?>);
            jzcmdDataElement.set_Class((Class<?>)o);
          }
          fnArgsExpr = jzcmdDataElement.fnArgsExpr;
        }
        if(  dataElement instanceof JZcmdScript.JZcmdDatapathElement ){
          JZcmdScript.JZcmdDatapathElement jzcmdDataElement = (JZcmdScript.JZcmdDatapathElement)dataElement;
          if(jzcmdDataElement.indirectDatapath !=null){
            Object oIdent = dataAccess(jzcmdDataElement.indirectDatapath, localVariables, bAccessPrivate, false, false, null);
            dataElement.setIdent(oIdent.toString());
          }
          fnArgsExpr = jzcmdDataElement.fnArgsExpr;
        }
        if(fnArgsExpr !=null) {
          int nrofArgs = fnArgsExpr.size();
          Object[] args = new Object[nrofArgs];
          int iArgs = -1;
          for(JZcmdScript.JZcmditem expr: fnArgsExpr){
            Object arg = evalObject(expr, false);
            args[++iArgs] = arg;
          }
          dataElement.setActualArgumentArray(args);
        }
      }

    }
    
    
    
    /**Gets the value of the given Argument. Either it is a 
     * <ul>
     * <li>String from {@link JZcmdScript.JZcmditem#textArg}
     * <li>Object from {@link JZcmdScript.JZcmditem#dataAccess}
     * <li>String from {@link JZcmdScript.JZcmditem#statementlist} if not 'M'
     * <li>Map<String, Object> from statementlist if the arg.{@link JZcmdScript.JZcmditem#elementType} == 'M'.
     *   it is d 'dataStruct'. 
     * <li>Object from {@link JZcmdScript.JZcmditem#expression}.
     * <li>If the arg is instanceof {@link JZcmdScript.Argument} - an argument of a call subroutine, then
     *   <ul>
     *   <li>If the {@link JZcmdScript.Argument#filepath} is set, then that {@link FilePath} is transfered
     *     to a {@link JZcmdFilepath} and returned. 
     *     A {@link JZcmdFilepath} contains the reference to this execution level.
     *   <li>If the {@link JZcmdScript.Argument#accessFileset} is set, then that {@link JZcmdScript.AccessFilesetname}
     *     is transfered to an instance of {@link JZcmdAccessFileset} and that instance is returned.   
     * </ul>
     * @param arg
     * @return Object adequate the arg, maybe null
     * @throws Exception any Exception while evaluating.
     */
    public Object evalObject(JZcmdScript.JZcmditem arg, boolean bContainer) throws Exception{
      Object obj;
      short ret = 0;
      if(arg.textArg !=null) obj = arg.textArg;
      else if(arg.dataAccess !=null){
        //calculate arguments firstly:
        obj = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        //obj = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
      } else if(arg.statementlist !=null){
        if(arg.elementType == 'M') {  
          //a dataStruct
          final ExecuteLevel level = new ExecuteLevel(jzClass, threadData, this, localVariables);
          IndexMultiTable<String, DataAccess.Variable<Object>> newVariables = 
            new IndexMultiTable<String, DataAccess.Variable<Object>>(IndexMultiTable.providerString); 
          //fill the dataStruct with its values:
          ret = level.execute(arg.statementlist, null, 0, newVariables, -1); //Note: extra newVariables
          obj = ret == kException ? JZcmdExecuter.retException: newVariables;
        } else {
          //A statementlist as object can be only a text expression.
          //its value is returned as String.
          StringFormatter u = new StringFormatter();
          ret = executeNewlevel(jzClass, arg.statementlist, u, arg.statementlist.indentText, -1);
          obj = ret == kException ? JZcmdExecuter.retException:  u.toString();
        }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        obj = value.objValue();
      } else {
        obj = null;  //throw new IllegalArgumentException("JZcmd - unexpected, faulty syntax");
      }
      if(obj !=null && ret != kException && arg.conversion !=0){
        switch(arg.conversion){
          case 'E': {
            String value = obj.toString();
            if(FileSystem.isAbsolutePath(value)){
              obj = new File(value);
            } else {
              obj = new File(currdir, value);
            }
          } break;
          case 'F': {
            String value = obj.toString();
            obj = new JZcmdFilepath(this, value);
          } break;
          case 'G': {
            assert(arg.subitem instanceof JZcmdScript.AccessFilesetname);
            JZcmdScript.AccessFilesetname arg1 = (JZcmdScript.AccessFilesetname) arg.subitem;
            obj = new JZcmdAccessFileset(arg1, arg1.filesetVariableName, this);
          } break;
          default: assert(false);
        }
      }
      return obj;
    }
    
    
    
    
    /**Gets the value of the given Argument. It is a {@link JZcmdScript.JZcmditem#expression}
     * @param arg
     * @return
     * @throws Exception
     */
    public CalculatorExpr.Value evalValue(JZcmdScript.JZcmditem arg, boolean bContainer) throws Exception{
      if(arg.textArg !=null){
        return null;  //TODO
      }
      else if(arg.dataAccess !=null){
        CalculatorExpr.Value value;
        Object obj = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
        if(obj instanceof Float){
          value = new Value(((Float)obj).floatValue());
        } else if(obj instanceof Double){
          value = new Value(((Double)obj).doubleValue());
        } else if(obj instanceof Long){
          value = new Value(((Long)obj).longValue());
        } else if(obj instanceof Integer){
          value = new Value(((Integer)obj).intValue());
        } else if(obj instanceof Short){
          value = new Value(((Short)obj).shortValue());
        } else if(obj instanceof Byte){
          value = new Value(((Byte)obj).byteValue());
        } else if(obj instanceof Boolean){
          value = new Value(((Boolean)obj).booleanValue());
        } else {
          value = new Value(obj);
        }
        return value;
      } else if(arg.expression !=null) {
        return calculateExpression(arg.expression); 
      } else {
        return null;  //no value given.
      }
    }
    
    
    
    
    public boolean evalCondition(JZcmdScript.JZcmditem arg) throws Exception{
      boolean ret;
      if(arg.textArg !=null) return true;
      else if(arg.dataAccess !=null){
        try{
          Object obj = dataAccess(arg.dataAccess, localVariables, bAccessPrivate, false, false, null);
          if(obj instanceof Number){
            ret = ((Number)obj).intValue() !=0;
          } else if(obj instanceof Boolean){
            ret = ((Boolean)obj).booleanValue();
          } else {
            ret = obj !=null;
          }
        } catch(NoSuchElementException exc){
          ret = false;
        } catch(NoSuchFieldException exc){
          ret = false;
        } catch(NoSuchMethodException exc){
          ret = false;
        }
      } else if(arg.statementlist !=null){
        throw new IllegalArgumentException("JZcmdExecuter - unexpected, faulty syntax");
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        ret = value.booleanValue();
      } else throw new IllegalArgumentException("JZcmdExecuter - unexpected, faulty syntax");
      return ret;
      
    }
    

    
    
    /**Check whether osrc is a filepath, or create one.
     * @param osrc
     * @return
     */
    JZcmdFilepath convert2FilePath(Object osrc)
    { JZcmdFilepath ret;
      if(osrc instanceof JZcmdFilepath){
        ret = (JZcmdFilepath)osrc;
      } else {
        String src = osrc.toString();
        ret = new JZcmdFilepath(this, new FilePath(src));
      }
      return ret;
    }
    
    
    protected short exec_DefMapVariable(JZcmdScript.DefVariable statement,  Map<String, DataAccess.Variable<Object>> newVariables) 
    throws Exception   
    { short ret = kSuccess;
      Object value = evalObject(statement, false);
      if(value == JZcmdExecuter.retException) {
        ret = kException;
      } else {
        if(value == null) { //no initialization
          value = new TreeMap<String, Object>();
        }
        executeDefVariable(newVariables, (JZcmdScript.DefVariable)statement, 'M', value, true); 
      }
      return ret;
    }
    
    
    protected short exec_DefClassVariable(JZcmdScript.DefClassVariable statement, Map<String, DataAccess.Variable<Object>> newVariables) 
    throws Exception   
    { //Class name = java.path
      short ret = kSuccess;
      CharSequence value = evalString(statement);
      Class<?> clazz;
      if(statement.loader !=null){
        Object oLoader = dataAccess(statement.loader, localVariables, bAccessPrivate, false, false, null);  //get the loader
        if(!(oLoader instanceof ClassLoader)) throw new IllegalArgumentException("JZcmd.exec_DefClassVariable - faulty ClassLoader");
        ClassLoader loader = (ClassLoader)oLoader;
        clazz = loader.loadClass(value.toString());
      } else {
        clazz = Class.forName(value.toString()); 
      }
      executeDefVariable(newVariables, statement, 'C', clazz, false);
      return ret; 
    }

    
    
    
    
    protected short addClassLoader(JZcmdScript.DefClasspathVariable statement, Map<String, DataAccess.Variable<Object>> newVariables) 
    throws Exception {
      List<File> filesjar = new LinkedList<File>();
      for(JZcmdScript.AccessFilesetname fileset: statement.jarpaths){
        if(fileset.filesetVariableName !=null){  //fileset variable
          JZcmdAccessFileset zjars = new JZcmdAccessFileset(fileset, fileset.filesetVariableName, this);
          List<JZcmdFilepath> jars = zjars.listFilesExpanded();
          for(JZcmdFilepath jfilejar: jars){
            File filejar = new File(jfilejar.absfile().toString());
            if(!filejar.exists()) throw new IllegalArgumentException("JZcmd.addClasspath - file does not exist; " + filejar.getAbsolutePath());
            filesjar.add(filejar);
          }
        } else {
          final FilePath accessPath;
          //if(fileset.accessPath ==null){
            CharSequence sAccesspath = evalString(fileset);
            accessPath = new FilePath(sAccesspath.toString());
          //} else {
          //  accessPath = fileset.accessPath;
          //}
          JZcmdFilepath jfilejar = new JZcmdFilepath(this, accessPath);
          File filejar = new File(jfilejar.absfile().toString());
          if(!filejar.exists()) throw new IllegalArgumentException("JZcmd.addClasspath - file does not exist; " + filejar.getAbsolutePath());
          filesjar.add(filejar);
        }
      }
      URL[] urls = new URL[filesjar.size()];
      int ixurl = -1;
      for(File filejar: filesjar){
        URI uri = filejar.toURI();
        urls[++ixurl] = uri.toURL();
          
      }
      ClassLoader parentLoader = this.getClass().getClassLoader(); //classLoader from this class
      URLClassLoader loader = new URLClassLoader(urls, parentLoader);
      executeDefVariable(newVariables, statement, 'J', loader, true);
      return kSuccess;
    }
    
    
    
    
    
    protected void runThread(ExecuteLevel executeLevel, JZcmdScript.ThreadBlock statement, JZcmdThread threadVar){
      try{
        executeLevel.execute(statement.statementlist, JZcmdExecuter.this.textout, 0, executeLevel.localVariables, -1);
      } 
      catch(Exception exc){
        threadVar.exception = exc;
        //finishes the thread.
      }
    }
    

    protected void finishThread(JZcmdThread thread){
      synchronized(thread){
        thread.notifyAll();   //any other thread may wait for join
      }
      synchronized(JZcmdExecuter.this.threads){  //remove this thread from the list of threads.
        boolean bOk = threads.remove(thread);
        assert(bOk);
        if(JZcmdExecuter.this.threads.size() == 0){
          JZcmdExecuter.this.threads.notify();    //notify the waiting main thread to finish.
        }
      }

    }


    
    boolean isBreak(){ return isBreak; }
    
    

    
    /**Returns the absolute canonical path of the currdir variable. currdir is a {@link File}.
     * The file separator is '/'.
     */
    public String currdir(){ return currdir.getPath().replace('\\', '/'); }
    
    
    int debug(JZcmdScript.JZcmditem statement) //throws Exception
    {
      try{ CharSequence text = evalString(statement);
      
      } catch(Exception exc){
        //unexpected
      }
      Assert.stop();
      return 1;
    }
    
    
    public void debug_dataAccessArguments(){ debug_dataAccessArguments = true; }
    
    
    void debug(){
      Assert.stop();
    }
    

    
    /**Returns a one-line-information about the last Exception with Stacktrace information (max 20 levels).
     */
    public CharSequence excStacktraceinfo(){    
      CharSequence sExc = Assert.exceptionInfo("JZcmd.execute - exception at;" + threadData.excStatement.toString() + ";", threadData.exception, 0, 20);
      return sExc;
    }
    
    
    void throwIllegalDstArgument(CharSequence text, DataAccess dst, JZcmdScript.JZcmditem statement)
    throws IllegalArgumentException
    {
      StringBuilder u = new StringBuilder(100);
      u.append("JZcmd - ").append(text).append(";").append(dst);
      u.append("; in file ").append(statement.parentList.srcFile);
      u.append(", line ").append(statement.srcLine).append(" col ").append(statement.srcColumn);
      throw new IllegalArgumentException(u.toString());
    }



    @Override
    public Object getAttribute(String name)
    { DataAccess.Variable<Object> var = localVariables.get(name);
      if(var == null) return null;
      else return var.value();
    }



    @Override
    public Object getAttribute(String name, int scope)
    { switch(scope){
        case ScriptContext.ENGINE_SCOPE: return getAttribute(name); 
        case ScriptContext.GLOBAL_SCOPE: return scriptLevel.getAttribute(name); 
        default: throw new IllegalArgumentException("JZcmdExecuter.getAttribute - failed scope;" + scope);
      } //switch
    }



    @Override
    public int getAttributesScope(String name)
    {
      // TODO
      return ScriptContext.ENGINE_SCOPE;
    }



    @Override
    public Bindings getBindings(int scope)
    { switch(scope){
      case ScriptContext.ENGINE_SCOPE: return new JZcmdBindings(localVariables); 
      case ScriptContext.GLOBAL_SCOPE: return new JZcmdBindings(scriptLevel.localVariables); 
      default: throw new IllegalArgumentException("JZcmdExecuter.getBindings - failed scope;" + scope);
    } //switch
  }



    @Override
    public Writer getErrorWriter()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public Reader getReader()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public List<Integer> getScopes()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public Writer getWriter()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public Object removeAttribute(String name, int scope)
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public void setAttribute(String name, Object value, int scope)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setBindings(Bindings bindings, int scope)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setErrorWriter(Writer writer)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setReader(Reader reader)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setWriter(Writer writer)
    {
      // TODO Auto-generated method stub
      
    }
    
    
    /* (non-Javadoc)
     * @see org.vishia.util.FilePath.FilePathEnvAccess#getCurrentDir()
     */
    @Override public CharSequence getCurrentDir()
    {
      return currdir();
    }




    /* (non-Javadoc)
     * @see org.vishia.util.FilePath.FilePathEnvAccess#getValue(java.lang.String)
     */
    @Override public Object getValue(String variable) throws NoSuchFieldException
    { Object oValue;
      DataAccess.Variable<Object> varV = localVariables.get(variable);
      if(varV == null){
        if(variable.startsWith("$")){
          oValue = System.getenv(variable.substring(1)).replace('\\', '/');  
        } else {
          oValue = null;
        }
        if(oValue == null) {
          throw new NoSuchFieldException("JZcmdFilepath.getValue() - variable not found; " + variable);
        } 
      } else {
        oValue = varV.value();
        if(oValue instanceof JZcmdFilepath){
          oValue = ((JZcmdFilepath)oValue).data;  //the FilePath instance.
        } 
      }
      return oValue;
    }


    
  }  
  
  
  /**Simple class to build a countered number. 
   * toString() increments and returns the number.
   */
  public class NextNr {
    private int nr = 0;
    
    /**Start method, next access returns 1. */
    void start(){ nr = 0; }
    
    /**Set method, next access returns value. */
    void set(int value){ nr = value -1; }  //NOTE: pre-increment.
    
    /**Increments and returns the number. */
    @Override public String toString(){
      return "" + ++nr;
    }
  };
  
  
  
  
  
  
  void stop(){
    
    
  }

  
  
  /**A thread instance of JZcmd.
   */
 // protected class JZcmdThread  
  
  public static class CmdErrorLevelException extends Exception
  {
    private static final long serialVersionUID = 7785185972638755384L;
    
    public int errorLevel;
    
    public CmdErrorLevelException(int errorLevel){
      super("cmd error level = " + errorLevel);
      this.errorLevel = errorLevel;
    }
  }
  
  
  public static class JZcmdThrow extends Exception
  {
    private static final long serialVersionUID = 1L;

    public JZcmdThrow(String text){
      super(text);
    }
  }
  
  
  public static class ExitException extends Exception
  {
    private static final long serialVersionUID = 1L;
    
    public int exitLevel;
    
    public ExitException(int exitLevel){
      this.exitLevel = exitLevel;
    }
  }
  
  
  /**
  *
  */
 protected static class JZcmdBindings implements Bindings 
 { 
   private final IndexMultiTable<String, DataAccess.Variable<Object>> vars;

   public JZcmdBindings(IndexMultiTable<String, DataAccess.Variable<Object>> vars)
   { this.vars = vars;
   }

   @Override
   public Object put(String name, Object value)
   {
     DataAccess.Variable<Object> var = new DataAccess.Variable<Object>('O', name, value, false);
     return vars.put(name, var);
   }

    @Override
    public boolean containsKey(Object key){ return vars.containsKey(key); }
  
    @Override
    public Object get(Object key)
    { DataAccess.Variable<Object> var = vars.get(key);
      return var == null ? null : var.value();
    }
  
    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge)
    {
      // TODO Auto-generated method stub
      
    }
  
    @Override
    public Object remove(Object key)
    {
      // TODO Auto-generated method stub
      return null;
    }
  
    @Override
    public void clear()
    {
      // TODO Auto-generated method stub
      
    }
  
    @Override
    public boolean containsValue(Object value)
    {
      // TODO Auto-generated method stub
      return false;
    }
  
    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
      // TODO Auto-generated method stub
      return null;
    }
  
    @Override
    public boolean isEmpty()
    {
      // TODO Auto-generated method stub
      return false;
    }
  
    @Override
    public Set<String> keySet()
    {
      // TODO Auto-generated method stub
      return null;
    }
  
    @Override
    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }
  
    @Override
    public Collection<Object> values()
    {
      // TODO Auto-generated method stub
      return null;
    }
     
  }
 
 
  
  
  
  
}
