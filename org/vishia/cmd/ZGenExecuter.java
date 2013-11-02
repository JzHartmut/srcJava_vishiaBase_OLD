package org.vishia.cmd;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;


import org.vishia.cmd.CmdExecuter;
import org.vishia.cmd.ZGenScript.Statement;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringSeq;


/**This class helps to generate texts from any Java-stored data controlled with a script. 
 * An instance of this class is used while {@link #generate(Object, File, File, boolean, Appendable)} is running.
 * You should not use the instance concurrently in more as one thread. But you can use this instance
 * for one after another call of {@link #generate(Object, File, File, boolean, Appendable)}.
 * <br><br>
 * The script is a simple text file which contains place holder for data and some control statements
 * for repeatedly generated data from any container.
 * <br><br>
 * The placeholder and control tags have the following form:
 * <ul>
 * <li> 
 *   <*path>: Access to any data element in the given data pool. The path starts from that object, which is given as input.
 *   Access to deeper nested elements are possible to write a dot. The return value is converted to a String.
 *   <br>
 *   The elements in the path can be public references, fields or method invocations. Methods can be given with constant parameters
 *   or with parameters stored in a script variable.
 *   <br>
 *   Example: <*data1.getData2().data2> 
 * <li>
 *   <:for:element:path>...<.for>: The text between the tags is generated for any member of the container, 
 *   which is designated with the path. The access to the elements is able to use the <*element.path>, where 'element'
 *   is any String identifier used in this for control. Controls can be nested.
 * <li>
 *   <:if:conditionpath>...<:elif>...<:else>...<.if>: The text between the tags is generated only if the condition is met. 
 * <li>
 *   <:switch:path><:case:value>...<:case:value>...<:else>...<.switch>: One variable is tested. The variable can be numerical or a String.
 *   Several values are tested.   
 * </ul> 
 * @author Hartmut
 *
 */
public class ZGenExecuter {
  
  
  /**Version and history
   * <ul>
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
   * 
   */
  @SuppressWarnings("hiding")
  static final public int version = 20131029;

  /**Variable for any exception while accessing any java ressources. It is the $error variable of the script. */
  protected String accessError = null;
  
  public final boolean bWriteErrorInOutput;
  
  protected boolean bAccessPrivate;
  
  protected final MainCmdLogging_ifc log;
  
  /**The output which is given by invocation of {@link #generate(Object, File, Appendable, boolean, Appendable)}
   */
  protected Appendable outFile;
  
  /**The java prepared generation script. */
  ZGenScript genScript;
  
  /**Instance for the main script part. */
  //Gen_Content genFile;

  /**Generated content of all script variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  final Map<String, DataAccess.Variable> scriptVariables = new_Variables();
  
  /**Generated content of all script environment variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  //final Map<String, String> scriptEnvVariables = new TreeMap<String, String>();
  
  
  protected final Queue<ZGenThread> threads = new ConcurrentLinkedQueue<ZGenThread>();
  
  private boolean bScriptVariableGenerated;
  
  
  /**The newline char sequence. */
  String newline = "\r\n";
  

  public ZGenExecuter(MainCmdLogging_ifc log){
    this.log = log;
    bWriteErrorInOutput = false;
  }
  
  
  public void setOutfile(Appendable outfile){ 
    this.outFile = outfile; 
    scriptVariables.put("text", new DataAccess.Variable('A', "text", outfile));
  }
  
  public Appendable outFile(){ return outFile; }
  
  public void setScriptVariable(String name, char type, Object content){
    DataAccess.setVariable(scriptVariables, name, type, content);
  }
  
  
  
  /**Generates script-global variables.
   * 
   * @param genScript It should be the same how used on {@link #genContent(ZGenScript, Object, boolean, Appendable)}
   *   but it may be another one for special cases.
   * @param userData Used userdata for content of scriptvariables. It should be the same how used on 
   *   {@link #genContent(ZGenScript, Object, boolean, Appendable)} but it may be another one for special cases.
   * @param accessPrivate true than access to private data of userData
   * @return The built script variables. 
   *   One can evaluate some script variables before running {@link #genContent(ZGenScript, Object, boolean, Appendable)}.
   *   Especially it is used for {@link org.vishia.zmake.Zmake to set the currDir.} 
   * @throws IOException
   */
  public Map<String, DataAccess.Variable> genScriptVariables(ZGenScript genScript, boolean accessPrivate) 
  throws IOException
  {
    this.genScript = genScript;
    //this.data = userData;
    this.bAccessPrivate = accessPrivate;
    CurrDir currDirWrapper = new CurrDir();
    currDirWrapper.currDir = new File(".").getAbsoluteFile().getParentFile();
    StringSeq cd = new StringSeq();
    cd.change(FileSystem.normalizePath(currDirWrapper.currDir.getAbsolutePath()));
    DataAccess.setVariable(scriptVariables, "$CD", 'E', cd);
    DataAccess.setVariable(scriptVariables, "currDir", 'O', currDirWrapper);
    DataAccess.setVariable(scriptVariables, "error", 'A', accessError);
    DataAccess.setVariable(scriptVariables, "mainCmdLogging", 'O', log);
    DataAccess.setVariable(scriptVariables, "nextNr", 'O', nextNr);
    DataAccess.setVariable(scriptVariables, "nrElementInContainer", 'O', null);
    DataAccess.setVariable(scriptVariables, "out", 'A', System.out);
    DataAccess.setVariable(scriptVariables, "err", 'A', System.err);
    DataAccess.setVariable(scriptVariables, "null", 'O', null);
    DataAccess.setVariable(scriptVariables, "jbat", 'O', this);
    DataAccess.setVariable(scriptVariables, "zgen", 'O', this);
    DataAccess.setVariable(scriptVariables, "file", 'O', new FileSystem());
    /*
    scriptVariables.put("$CD", new DataAccess.Variable('$', "$CD", cd));
    scriptVariables.put("currDir", new DataAccess.Variable('O', "currDir", currDirWrapper));
    scriptVariables.put("error", new DataAccess.Variable('U', "error", accessError));
    scriptVariables.put("mainCmdLogging", new DataAccess.Variable('O', "mainCmdLogging", log));
    scriptVariables.put("nextNr", new DataAccess.Variable('O', "nextNr", nextNr));
    scriptVariables.put("nrElementInContainer", new DataAccess.Variable('O', "nrElementInContainer", null));
    scriptVariables.put("out", new DataAccess.Variable('U', "out", System.out));
    scriptVariables.put("err", new DataAccess.Variable('U', "err", System.err));
    scriptVariables.put("null", new DataAccess.Variable('O', "null", null));
    scriptVariables.put("jbat", new DataAccess.Variable('O', "jbat", this));
    //scriptVariables.put("debug", new ZbatchDebugHelper());
    scriptVariables.put("file", new DataAccess.Variable('O', "file", new FileSystem()));
    */
    ExecuteLevel genVariable = new ExecuteLevel(null, scriptVariables); //NOTE: use recent scriptVariables.
    for(ZGenScript.Statement scriptVariableScript: genScript.getListScriptVariables()){
      try{
        Object value;
        switch(scriptVariableScript.elementType()){
          case 'S':  value = genVariable.evalString(scriptVariableScript); break;
          case 'J':  value = genVariable.evalObject(scriptVariableScript, true); break;
          default: value = "???";
        }
        List<DataAccess.DatapathElement> assignPath = scriptVariableScript.variable.datapath();
        if(assignPath.size() == 1 && assignPath.get(0).ident().equals("$CD")){
          //special handling of current directory:
          genVariable.setCurrDir((CharSequence)value);  //normalize, set "currDir"
        } else {
          scriptVariableScript.variable.storeValue(scriptVariables, value, true);
        }
      } catch(Exception exc){
        System.out.println("JbatchExecuter - Scriptvariable faulty; " );
      }
      /*
      StringBuilder uVariable = new StringBuilder();
      ExecuteLevel genVariable = new ExecuteLevel(null, scriptVariables); //NOTE: use recent scriptVariables.
      genVariable.execute(scriptVariableScript.getSubContent(), uVariable, false);
      scriptVariables.put(scriptVariableScript.identArgJbat, uVariable); //Buffer.toString());
      */
    }
    bScriptVariableGenerated = true;
    return scriptVariables;
  }
  


  
  public void reset(){
    bScriptVariableGenerated = false;
    scriptVariables.clear();
  }
  
  
  /**Generates an output with the given script.
   * @param genScript Generation script in java-prepared form. Parse it with {@link #parseGenScript(File, Appendable)}.
   * @param out Any output. It is used for direct text output and it is stored as variable "text"
   *   to write "<+text>...<.+>" to output to it.
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  /**
   * @param genScript The script
   * @param accessPrivate
   * @param bWaitForThreads should set to true if it is a command line invocation of Java,
   *   the exit should wait for all threads. May set to false if calling inside a long running application.
   * @param out Text output of <+text>....<.+>
   * @return exit level? 0
   * @throws IOException
   */
  public int execute(ZGenScript genScript, boolean accessPrivate, boolean bWaitForThreads, Appendable out) 
  throws IOException
  {
    this.bAccessPrivate = accessPrivate;
    //this.data = userData;
    this.genScript = genScript;

    if(!bScriptVariableGenerated){
      genScriptVariables(genScript, accessPrivate);
    }
    setScriptVariable("text", 'A', out);
    ZGenScript.Statement contentScript = genScript.getMain();
    ExecuteLevel genFile = new ExecuteLevel(null, scriptVariables);
    String sError1 = genFile.execute(contentScript.statementlist, out, false);
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
    return 0;
  }

  
  /**Initializes, especially all script variables.
   * @param genScript Generation script in java-prepared form. Parse it with {@link #parseGenScript(File, Appendable)}.
   * @param out Any output. It is used for direct text output and it is stored as variable "text"
   *   to write "<+text>...<.+>" to output to it.
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  public String initialize(ZGenScript genScript, boolean accessPrivate) 
  throws IOException
  {
    this.bAccessPrivate = accessPrivate;
    //this.data = userData;
    this.genScript = genScript;

    if(!bScriptVariableGenerated){
      genScriptVariables(genScript, accessPrivate);
    }
    return null;
  }

  
  /**Executes any sub routine.
   * @param statement
   * @param accessPrivate
   * @param out
   * @return
   * @throws IOException
   */
  public String execSub(ZGenScript.Statement statement, Map<String, DataAccess.Variable> args
      , boolean accessPrivate, Appendable out) 
  throws IOException
  {
    ExecuteLevel level = new ExecuteLevel(null, scriptVariables);
    //The args should be added to the localVariables of the subroutines level:
    level.localVariables.putAll(args);
    //Executes the statements of the sub routine:
    String sError1 = level.execute(statement.statementlist, out, false);
    return sError1;
  }
  
  
  
  
  public Object getScriptVariable(String name) throws NoSuchFieldException
  { return DataAccess.getVariable(scriptVariables, name, true); }

  
  
  

  protected Map<String, DataAccess.Variable> xnew_Variables(){
    return new TreeMap<String, DataAccess.Variable>();
    
  }
  

  protected Map<String, DataAccess.Variable> new_Variables(){
    return new IndexMultiTable<String, DataAccess.Variable>(IndexMultiTable.providerString);
  }
  
  
  public void runThread(ExecuteLevel executeLevel, ZGenScript.Statement statement){
    try{ 
      executeLevel.execute(statement.statementlist, null, false);
    } 
    catch(Exception exc){
      //TODO anything with the environment onerror statement?
      exc.printStackTrace(System.out);
    }
  }
  


  /**
   * @param sError
   * @param out
   * @return false to assign to an ok variable.
   * @throws IOException
   */
  boolean writeError(String sError, Appendable out) throws IOException{
    if(bWriteErrorInOutput){
      out.append(sError);
    } else {
      throw new IllegalArgumentException(sError); 
    }
    return false;

  }
  
  /**Wrapper to generate a script with specified localVariables.
   * A new Wrapper is created on any subroutine level. It is used in a {@link CalculatorExpr#calcDataAccess(Map, Object...)} 
   * to generate an expression independent of an environment.
   *
   */
  public final class ExecuteLevel
  {
    /**Not used yet. Only for debug?
     * 
     */
    public final ExecuteLevel parent;
    
    
    /**Generated content of local variables in this nested level including the {@link ZbatchExecuter#scriptVariables}.
     * The variables are type invariant on language level. The type is checked and therefore 
     * errors are detected on runtime only. */
    public final Map<String, DataAccess.Variable> localVariables;
    
    
    
    /**Constructs data for a local execution level.
     * @param parentVariables if given this variable are copied to the local ones.
     *   They contains the script variables too. If not given (null), only the script variables
     *   are copied into the {@link #localVariables}. Note that subroutines do not know the
     *   local variables of its calling routine! This argument is only set if nested statement blocks
     *   are to execute. 
     */
    public ExecuteLevel(ExecuteLevel parent, Map<String, DataAccess.Variable> parentVariables)
    { this.parent = parent;
      localVariables = new_Variables();
      if(parentVariables == null){
        localVariables.putAll(scriptVariables);
      } else {
        localVariables.putAll(parentVariables);  //use the same if it is not a subText, only a 
      }
      DataAccess.setVariable(localVariables,  "zgenSub", 'O', this);
    }

    
    
    /**Returns the log interface from the environment class. */
    public MainCmdLogging_ifc log(){ return log; }
    
    
    public void setLocalVariable(String name, char type, Object content){
      DataAccess.setVariable(localVariables, name, type, content);
    }
    
    
    
    
    
    /**Executes an inner script part maybe with a new level of nested local variables.
     * If the contentScript does not contain any variable definition 
     * (see @link {@link ZGenScript.StatementList#bContainsVariableDef}) then this level is used,
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
    public String executeNewlevel(ZGenScript.StatementList contentScript, final Appendable out, boolean bContainerHasNext) 
    throws IOException 
    { final ExecuteLevel level;
      if(contentScript.bContainsVariableDef){
        level = new ExecuteLevel(this, localVariables);
      } else {
        level = this;
      }
      return level.execute(contentScript, out, bContainerHasNext);
    }

  
    /**
     * @param contentScript
     * @param bContainerHasNext Especially for <:for:element:container>SCRIPT<.for> to implement <:hasNext>
     * @return
     * @throws IOException 
     */
    public String execute(ZGenScript.StatementList contentScript, final Appendable out, boolean bContainerHasNext) 
    //throws IOException 
    {
      String sError = null;
      Appendable uBuffer = out;
      //Generate direct requested output. It is especially on inner content-scripts.
      int ixStatement = -1;
      //Iterator<JbatchScript.Statement> iter = contentScript.content.iterator();
      while(++ixStatement < contentScript.statements.size() && sError == null) { //iter.hasNext() && sError == null){
        ZGenScript.Statement statement = contentScript.statements.get(ixStatement); //iter.next();
        //for(TextGenScript.ScriptElement statement: contentScript.content){
        try{    
          switch(statement.elementType()){
          case 't': { 
            int posLine = 0;
            int posEnd;
            if(statement.textArg.startsWith("'''trans ==> dst"))
              stop();
            do{
              posEnd = statement.textArg.indexOf('\n', posLine);
              if(posEnd >= 0){ 
                uBuffer.append(statement.textArg.substring(posLine, posEnd));   
                uBuffer.append(newline);
                posLine = posEnd +1;  //after \n 
              } else {
                uBuffer.append(statement.textArg.substring(posLine));   
              }
              
            } while(posEnd >=0);  //output all lines.
          } break;
          case 'n': {
            uBuffer.append(newline);
          } break;
          case 'T': textAppendToVarOrOut(statement); break; 
          case 'U': defineExpr(statement); break; //setStringVariable(statement); break; 
          case 'S': defineExpr(statement); break; //setStringVariable(statement); break; 
          case 'P': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            setLocalVariable(statement.identArgJbat, 'P', uBufferVariable);
          } break;
          case 'L': {
            Object value = evalObject(statement, true); 
              //getContent(statement, localVariables, false);  //not a container
            setLocalVariable(statement.identArgJbat, 'L', value);
            if(!(value instanceof Iterable<?>)) 
                throw new NoSuchFieldException("JbatExecuter - exec variable must be of type Iterable ;" + statement.identArgJbat);
          } break;
          case 'W': executeOpenfile(statement); break;
          case 'J': {
            if(statement.identArgJbat.equals("checkDeps"))
              stop();
            Object value = evalObject(statement, false);
            setLocalVariable(statement.identArgJbat, 'O', value);
          } break;
          case 'e': executeDatatext(statement, out); break; 
          case 's': {
            executeSubroutine(statement, out);
          } break;
          case 'x': executeThread(statement); break;
          case 'm': executeMove(statement); break;
          case 'c': executeCmdline(statement); break;
          case 'd': executeChangeCurrDir(statement); break;
          case 'C': { //generation <:for:name:path> <genContent> <.for>
            executeForContainer(statement, out);
          } break;
          case 'B': { //statementBlock
            executeSubLevel(statement, out);  ///
          } break;
          case 'F': executeIfStatement(statement, out); break;
          case 'w': whileStatement(statement, out); break;
          case 'N': {
            executeIfContainerHasNext(statement, out, bContainerHasNext);
          } break;
          case '=': assignExpr(statement); break;
          case '+': appendExpr(statement); break;
          case 'b': sError = "break"; break;
          case '?': break;  //don't execute a onerror, skip it.
          case 'z': throw new ZGenExecuter.ExitException(((ZGenScript.ExitStatement)statement).exitValue);  
          default: 
            uBuffer.append("Jbat - execute-unknown type; '" + statement.elementType() + "' :ERROR=== ");
          }//switch
          
        } catch(Exception exc){
          //any statement has thrown an exception.
          //check onerror with proper error type anywhere after this statement, it is stored in the statement.
          //continue there.
          boolean found = false;
          char excType;   //NOTE: the errortype in an onerror statement is the first letter of error keyword in syntax; notfound, file, internal, exit
          int errLevel = 0;
          if(exc instanceof ExitException){ excType = 'e'; errLevel = ((ExitException)exc).exitLevel; }
          else if(exc instanceof IOException){ excType = 'f'; }
          else if(exc instanceof NoSuchFieldException || exc instanceof NoSuchMethodException){ excType = 'n'; }
          else { excType = 'i'; }
          //Search the block of onerror after this statement.
          //Maybe use an index in any statement, to prevent search time.
          while(++ixStatement < contentScript.statements.size() && (statement = contentScript.statements.get(ixStatement)).elementType() != '?');
          if(ixStatement < contentScript.statements.size()){
            //onerror-block found.
            do { //search the appropriate error type:
              char onerrorType;
              ZGenScript.Onerror errorStatement = (ZGenScript.Onerror)statement;
              if( ((onerrorType = errorStatement.errorType) == excType
                || (onerrorType == '?' && excType != 'e')   //common onerror is valid for all excluding exit 
                )  ){
                found = excType != 'e' || errLevel >= errorStatement.errorLevel;  //if exit exception, then check errlevel
              }
            } while(!found && ++ixStatement < contentScript.statements.size() && (statement = contentScript.statements.get(ixStatement)).elementType() == '?');
          }
          if(found){
            String sError1 = exc.getMessage();
            setLocalVariable("errorMsg", 'S', sError1);
            executeSubLevel(statement, out);
          } else {
            sError = exc.getMessage();
            System.err.println("Jbat - execute-exception; " + exc.getMessage());
            exc.printStackTrace();
            //throw exc;
            throw new IllegalArgumentException (exc.getMessage());
          }
        }
      }//while
      
      return sError;
    }
    
    
    
    void executeForContainer(ZGenScript.Statement statement, Appendable out) throws Exception
    {
      ZGenScript.StatementList subContent = statement.getSubContent();  //The same sub content is used for all container elements.
      if(statement.identArgJbat.equals("include1"))
        stop();
      Object container = evalObject(statement, true);
      if(container instanceof String && ((String)container).startsWith("<?")){
        writeError((String)container, out);
      }
      else if(container !=null && container instanceof Iterable<?>){
        Iterator<?> iter = ((Iterable<?>)container).iterator();
        ExecuteLevel forExecuter = new ExecuteLevel(this, localVariables);
        //a new level for the for... statements. It contains the foreachData and maybe some more variables.
        while(iter.hasNext()){
          Object foreachData = iter.next();
          if(foreachData !=null){
            //Gen_Content genFor = new Gen_Content(this, false);
            //genFor.
            forExecuter.setLocalVariable(statement.identArgJbat, 'O', foreachData);
            //genFor.
            forExecuter.execute(subContent, out, iter.hasNext());
          }
        }
      }
      else if(container !=null && container instanceof Map<?,?>){
        Map<?,?> map = (Map<?,?>)container;
        Set<?> entries = map.entrySet();
        Iterator<?> iter = entries.iterator();
        while(iter.hasNext()){
          Map.Entry<?, ?> foreachDataEntry = (Map.Entry<?, ?>)iter.next();
          Object foreachData = foreachDataEntry.getValue();
          if(foreachData !=null){
            //Gen_Content genFor = new Gen_Content(this, false);
            //genFor.
            setLocalVariable(statement.identArgJbat, 'O', foreachData);
            //genFor.
            execute(subContent, out, iter.hasNext());
          }
        }
      }
    }
    
    
    
    void executeIfContainerHasNext(ZGenScript.Statement hasNextScript, Appendable out, boolean bContainerHasNext) throws IOException{
      if(bContainerHasNext){
        //(new Gen_Content(this, false)).
        execute(hasNextScript.getSubContent(), out, false);
      }
    }
    
    
    
    /**it contains maybe more as one if block and else. 
     * @throws Exception */
    void executeIfStatement(ZGenScript.Statement ifStatement, Appendable out) throws Exception{
      Iterator<ZGenScript.Statement> iter = ifStatement.statementlist.statements.iterator();
      boolean found = false;  //if block found
      while(iter.hasNext() && !found ){
        ZGenScript.Statement statement = iter.next();
        switch(statement.elementType()){
          case 'G': { //if-block
            boolean hasNext = iter.hasNext();
            found = executeIfBlock((ZGenScript.IfCondition)statement, out, hasNext);
          } break;
          case 'E': { //elsef
            if(!found){
              execute(statement.statementlist, out, false);
            }
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + statement.elementType() + "' :ERROR=== ");
          }
        }//switch
      }//for
    }
    
    
    
    /**Executes a while statement. 
     * @throws Exception */
    void whileStatement(ZGenScript.Statement whileStatement, Appendable out) 
    throws Exception 
    {
      boolean cond;
      do{
        CalculatorExpr.Value check = whileStatement.expression.calcDataAccess(localVariables);
        cond = check.booleanValue();
        if(cond){
          execute(whileStatement.statementlist, out, false);
        }
      } while(cond);  //if executed, check cond again.  
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
    boolean executeIfBlock(ZGenScript.IfCondition ifBlock, Appendable out, boolean bIfHasNext) 
    throws Exception
    {
      //Object check = getContent(ifBlock, localVariables, false);
      
      CalculatorExpr.Value check;
      boolean bCheck;
      try{
        check = ifBlock.expression.calcDataAccess(localVariables);
        bCheck = check.booleanValue();
      } catch(NoSuchElementException exc){
        bCheck = false;
      } catch(NoSuchFieldException exc){
        bCheck = false;
      } catch(NoSuchMethodException exc){
        bCheck = false;
      }
      if(bCheck){
        execute(ifBlock.statementlist, out, bIfHasNext);
      }
      return bCheck;
    }
    
    
    
    
    /**Creates or sets a string variable, maybe an environment variable.
     * It calls {@link DataAccess#storeValue(List, Map, Object, boolean)}.
     * It means the text value will be append to a {@link java.lang.Appendable}
     * or it replaces the CharSequence on a {@link org.vishia.util.StringSeq}.
     * If the variable is a script variable as StringSeq, its content is changed.
     * 
     * @param statement
     * @throws Exception
     */
    void XXXsetStringVariable(ZGenScript.Statement statement) 
    throws Exception 
    {
      List<DataAccess.DatapathElement> assignPath1 = 
        statement.assignObjs ==null || statement.assignObjs.size() ==0 ? null :
          statement.assignObjs.get(0).datapath();  
      if(assignPath1 !=null && assignPath1.get(0).ident().equals("dummy"))
        Assert.stop();
      
      CharSequence text = evalString(statement);
      
      if(statement.assignObjs !=null) for(DataAccess dataAccess: statement.assignObjs) {
        List<DataAccess.DatapathElement> assignPath = dataAccess.datapath();
        if(assignPath.size() == 1 && assignPath.get(0).ident().equals("$CD")){
          //special handling of current directory:
          setCurrDir(text);  //normalize, set "currDir"
        } else {
          DataAccess.storeValue(assignPath, localVariables, text, true);
          //putOrReplaceLocalVariable(statement.identArgJbat, text);
        }
      }
    } 
    
    
    
    /**Invocation for <+name>text<.+>.
     * It gets the Appendable from the assign variable
     * and executes {@link #execute(org.vishia.cmd.ZGenScript.StatementList, Appendable, boolean)}
     * with it.
     * @param statement the statement
     * @throws Exception 
     */
    void textAppendToVarOrOut(ZGenScript.Statement statement) throws Exception
    { Object variable = statement.variable.getDataObj(localVariables, bAccessPrivate, false); //getVariable(localVariables,name, false);
      if(!(variable instanceof Appendable)) {
        throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
      }
      Appendable out1 = (Appendable)variable;  //append, it may be a StringBuilder.
      if(statement.statementlist !=null){
        //executes the statement, use the Appendable to output immediately
        execute(statement.statementlist, out1, false);
      } else {
        //Any other text expression
        CharSequence text = evalString(statement);
        if(text !=null){
          out1.append(text);
        }
      }
    }
    
    

    
    
    
    
    
    void executeSubroutine(ZGenScript.Statement statement, Appendable out) 
    throws IllegalArgumentException, Exception
    {
      ZGenScript.CallStatement callStatement = (ZGenScript.CallStatement)statement;
      boolean ok = true;
      final CharSequence nameSubtext;
      /*
      if(statement.name == null){
        //subtext name gotten from any data location, variable name
        Object oName = ascertainText(statement.expression, localVariables); //getContent(statement, localVariables, false);
        nameSubtext = DataAccess.getStringFromObject(oName, null);
      } else {
        nameSubtext = statement.name;
      }*/
      nameSubtext = evalString(callStatement.callName); 
      ZGenScript.Statement subtextScript = genScript.getSubtextScript(nameSubtext);  //the subtext script to call
      if(subtextScript == null){
        throw new NoSuchElementException("JbatExecuter - subroutine not found; " + nameSubtext);
      } else {
        ExecuteLevel subtextGenerator = new ExecuteLevel(this, null);
        if(subtextScript.arguments !=null){
          //build a Map temporary to check which arguments are used:
          TreeMap<String, CheckArgument> check = new TreeMap<String, CheckArgument>();
          for(ZGenScript.Argument formalArg: subtextScript.arguments) {
            check.put(formalArg.identArgJbat, new CheckArgument(formalArg));
          }
          //process all actual arguments:
          List<ZGenScript.Argument> referenceSettings = statement.getReferenceDataSettings();
          if(referenceSettings !=null){
            for( ZGenScript.Argument referenceSetting: referenceSettings){  //process all actual arguments
              Object ref;
              ref = evalObject(referenceSetting, false);
              //ref = ascertainValue(referenceSetting.expression, data, localVariables, false);       //actual value
              if(ref !=null){
                CheckArgument checkArg = check.get(referenceSetting.identArgJbat);      //is it a requested argument (per name)?
                if(checkArg == null){
                  ok = writeError("??: *subtext;" + nameSubtext + ": " + referenceSetting.identArgJbat + " faulty argument.?? ", out);
                } else {
                  checkArg.used = true;    //requested and resolved.
                  subtextGenerator.setLocalVariable(referenceSetting.identArgJbat, 'O', ref);
                }
              } else {
                ok = writeError("??: *subtext;" + nameSubtext + ": " + referenceSetting.identArgJbat + " = ? not found.??", out);
              }
            }
            //check whether all formal arguments are given with actual args or get its default values.
            //if not all variables are correct, write error.
            for(Map.Entry<String, CheckArgument> checkArg : check.entrySet()){
              CheckArgument arg = checkArg.getValue();
              if(!arg.used){
                if(arg.formalArg.expression !=null){
                  Object ref = evalObject(arg.formalArg, false);
                  if(ref !=null){
                    subtextGenerator.setLocalVariable(arg.formalArg.identArgJbat, 'O', ref);
                  } else {
                    ok = writeError("??: *subtext;" + nameSubtext + ": " + arg.formalArg.identArgJbat + " not found.??", out);
                  }
                /*
                if(arg.formalArg.sumExpression.text !=null){
                  subtextGenerator.localVariables.put(arg.formalArg.name, arg.formalArg.sumExpression.text);
                } else if(arg.formalArg.sumExpression.datapath !=null){
                  Object ref = getContent(arg.formalArg, localVariables, false);
                  if(ref !=null){
                    subtextGenerator.localVariables.put(arg.formalArg.name, ref);
                  } else {
                    ok = writeError("??: *subtext;" + nameSubtext + ": " + arg.formalArg.name + " = ??> not found. ?>", out);
                  }
                */  
                } else {
                  ok = writeError("??: *subtext;" + nameSubtext + ": " + arg.formalArg.identArgJbat + "  missing on call.??", out);
                }
              }
            }
          }
        } else if(statement.getReferenceDataSettings() !=null){
          ok = writeError("??: *subtext;" + nameSubtext + " called with arguments, it has not one.??", out);
        }
        if(ok){
          subtextGenerator.execute(subtextScript.statementlist, out, false);
        }
      }
    }
    
    
    
    
    /**executes statements in another thread.
     * @throws Exception 
     */
    private void executeThread(ZGenScript.Statement statement) 
    throws Exception
    { final ZgenThreadResult result;
      if(statement.dataAccess !=null){
        try{
          if(statement.identArgJbat != null){  //marker for a new ThreadVariable
            result = new ZgenThreadResult();
            DataAccess.storeValue(statement.dataAccess.datapath(), localVariables, result, bAccessPrivate);
          } else { 
            //use existing thread variable, Exception if not found.
            result = (ZgenThreadResult)statement.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
          }
        } catch(Exception exc){
          throw new IllegalArgumentException("JbatchExecuter - thread assign failure; path=" + statement.dataAccess.toString());
        }
      } else {
        result = null;
      }
      ZGenThread thread = new ZGenThread(this, statement, result);
      synchronized(threads){
        threads.add(thread);
      }
      Thread threadmng = new Thread(thread, "ZGen");
      threadmng.start();  
      //it does not wait on finishing this thread.
    }

    
    
    
    /**Generates or executes any sub content.
     * @param script
     * @param out
     * @return
     * @throws IOException
     */
    public String executeSubLevel(ZGenScript.Statement script, Appendable out) 
    //throws IOException
    {
      ExecuteLevel genContent;
      if(script.statementlist.bContainsVariableDef){
        genContent = new ExecuteLevel(this, localVariables);
      } else {
        genContent = this;  //don't use an own instance, save memory and calculation time.
      }
      return genContent.execute(script.statementlist, out, false);
    }

    
    
    void executeCmdline(ZGenScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      boolean ok = true;
      final CharSequence sCmd;
      if(statement.identArgJbat == null){
        //cmd gotten from any data location, variable name
        sCmd = evalString(statement); 
      } else {
        sCmd = statement.identArgJbat;
      }
      String[] args;
      if(statement.arguments !=null){
        args = new String[statement.arguments.size() +1];
        int iArg = 1;
        for(ZGenScript.Argument arg: statement.arguments){
          String sArg = evalString(arg).toString(); //XXXascertainText(arg.expression, localVariables);
          args[iArg++] = sArg;
        }
      } else { 
        args = new String[1]; 
      }
      args[0] = sCmd.toString();
      List<Appendable> outCmd;
      if(statement.variable !=null){
        outCmd = new LinkedList<Appendable>();
        Object oOutCmd = statement.variable.getDataObj(localVariables, bAccessPrivate, false);
        if(oOutCmd instanceof Appendable){
          outCmd.add((Appendable)oOutCmd);
        } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
        }
        if(statement.assignObjs !=null){
          for(DataAccess assignObj1 : statement.assignObjs){
            oOutCmd = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
            if(oOutCmd instanceof Appendable){
              outCmd.add((Appendable)oOutCmd);
            } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
            }
        } }
      } else {
        outCmd = null;
      }
      CmdExecuter cmdExecuter = new CmdExecuter();
      
      CurrDir currDir = (CurrDir)DataAccess.getVariable(localVariables,"currDir", true);
      //localVariables.
      cmdExecuter.setCurrentDir(currDir.currDir);
      int errorlevel = cmdExecuter.execute(args, null, outCmd, null);
      if(errorlevel >0){
        //TODO check whether an onerror statement follows:
      }
      cmdExecuter.close();
    }
    

    void executeChangeCurrDir(ZGenScript.Statement statement)
    throws Exception
    {
      CharSequence arg = evalString(statement);
      setCurrDir(arg); 
    }

    
    protected void setCurrDir(CharSequence arg) throws NoSuchFieldException{
      String sCurrDir;
      final CharSequence arg1;
      Object cd1 = DataAccess.getVariable(localVariables,"$CD", true);
      StringBuilder u;
      boolean absPath = FileSystem.isAbsolutePathOrDrive(arg);
      //Change the content of the $CD to the absolute directory.
      if(cd1 instanceof StringBuilder){
        u = (StringBuilder)cd1;   //Use the StringBuilder inside the variable
      }
      else if(cd1 instanceof StringSeq){
        u = ((StringSeq)cd1).changeIt();
      } else {
        u = new StringBuilder();
        DataAccess.setVariable(localVariables,"$CD", 'S', u);
      }
      //u is referred in the variable as value.
      if(absPath){ 
        u.setLength(0);
        u.append(arg);
      } else {
        u.append('/').append(arg);
      }
      FileSystem.normalizePath(u);   //resolve "xxx/../xxx"
      sCurrDir = u.toString();
      CurrDir currDirWrapper = (CurrDir)DataAccess.getVariable(localVariables,"currDir", true);
      currDirWrapper.currDir = new File(sCurrDir);
      
    }
    
     
    
    private void executeDatatext(ZGenScript.Statement statement, Appendable out)  //<*datatext>
    throws IllegalArgumentException, Exception
    {
      Object obj = evalDatapathOrExpr(statement); //ascertainText(statement.expression, localVariables);
      if(obj instanceof CalculatorExpr.Value){
        obj = ((CalculatorExpr.Value)obj).objValue();
      }
      final CharSequence text;
      if(statement.textArg !=null){ //it is a format string:
         text = String.format(statement.textArg.toString(), obj);
      } else {
        text = obj.toString();
      }
      out.append(text); 
    }
    
    void executeMove(ZGenScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence s1 = evalString(statement.arguments.get(0));
      CharSequence s2 = evalString(statement.arguments.get(1));
      File fileSrc = new File(s1.toString());
      File fileDst = new File(s2.toString());
      boolean bOk = fileSrc.renameTo(fileDst);
      if(!bOk) throw new IOException("JbatchExecuter - move not successfully; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
    void executeOpenfile(ZGenScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      String sFilename = evalString(statement).toString();
      Writer writer = new FileWriter(sFilename);
      DataAccess.storeValue(statement.variable.datapath(), localVariables, writer, bAccessPrivate);
      //setLocalVariable(statement.identArgJbat, 'A', writer);
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
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void assignExpr(ZGenScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      Object val = evalObject(statement, false);
      DataAccess assignObj1 = statement.variable;
      Iterator<DataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        //Object dst = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
        DataAccess.Dst dstField = new DataAccess.Dst();
        Object dst = DataAccess.access(assignObj1.datapath(), null, localVariables, bAccessPrivate, false, true, dstField);
        if(dst instanceof DataAccess.Variable){
          DataAccess.Variable var = (DataAccess.Variable) dst; //assignObj1.accessVariable(localVariables, bAccessPrivate);
          dst = var.value();
          switch(var.type()){
            case 'A': {
              throwIllegalDstArgument("assign to appendable faulty", assignObj1, statement); 
            } break;
            case 'U': {
              assert(dst instanceof StringBuilder);            
              StringBuilder u = (StringBuilder) dst;
              u.setLength(0);
              if(!(val instanceof CharSequence)){
                val = val.toString();
              }
              u.append((CharSequence)val);
            } break;
            case 'S':{
              if(val instanceof String || val instanceof StringSeq && ((StringSeq)val).isUnmated()){
                var.setValue(val);
              } else {
                var.setValue(val.toString());
              }
            } break;
            default:{
              var.setValue(val);   //sets the value to the variable.
            }
          }//switch
        } else {
          //check whether the field is compatible with val
          dstField.set(val);
          //DataAccess.storeValue(assignObj1.datapath(), localVariables, val, bAccessPrivate);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
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
    void defineExpr(ZGenScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      Object init = evalObject(statement, false);  //maybe null
      Object val;
      switch(statement.elementType()){
        case 'U': {
          if(init == null){
            val = new StringBuilder(256);
          } else {
            CharSequence init1 = init instanceof CharSequence ? (CharSequence)init : init.toString();
            val = new StringBuilder(init1);
          }
        } break;
        case 'S':{
          if(init == null || init instanceof String || init instanceof StringSeq && ((StringSeq)init).isUnmated()){
            val = init;
          } else {
            val = init.toString();
          }
        } break;
        default: val = init;
      }
      statement.variable.storeValue(localVariables, val, bAccessPrivate);
      
    }
    
    
    
    /**Executes a <code>appendExpr::= [{ < datapath?assign > += }] < expression > ;.</code>.
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void appendExpr(ZGenScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      Object val = evalObject(statement, false);
      DataAccess assignObj1 = statement.variable;
      Iterator<DataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        Object dst = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
        if(dst instanceof Appendable){
          if(!(val instanceof CharSequence)){
            val = val.toString();
          }
          ((Appendable) dst).append((CharSequence)val);
        } else {
          throwIllegalDstArgument("dst should be Appendable", assignObj1, statement);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
    }
    
    
    
    /**Checks either the {@link ZGenScript.Argument#dataAccess} or, if it is null,
     * the {@link ZGenScript.Argument#expression}. Returns either the Object which is gotten
     * by the {@link DataAccess#getDataObj(Map, boolean, boolean)} or which is calculated
     * by the expression. Returns an instance of {@link CalculatorExpr.Value} if it is 
     * a result by an expression.
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalDatapathOrExpr(ZGenScript.Argument arg) throws Exception{
      if(arg.dataAccess !=null){
        Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o; }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        if(value.isObjValue()){ return value.objValue(); }
        else return value;
      } else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
    }
    
    
    
    public CharSequence evalString(ZGenScript.Argument arg) throws Exception{
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o.toString(); }
      } else if(arg.statementlist !=null){
        StringBuilder u = new StringBuilder();
        executeNewlevel(arg.statementlist, u, false);
        return StringSeq.create(u, true);
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        return value.stringValue();
      } else return null;  //it is admissible.
      //} else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
    }
    
    
    
    /**Gets the value of the given Argument. Either it is a 
     * <ul>
     * <li>String from {@link ZGenScript.Argument#textArg}
     * <li>Object from {@link ZGenScript.Argument#datapath()}
     * <li>Object from {@link ZGenScript.Argument#expression}
     * <li>
     * </ul>
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalObject(ZGenScript.Argument arg, boolean bContainer) throws Exception{
      Object obj;
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        obj = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
      } else if(arg.statementlist !=null){
        StringBuilder u = new StringBuilder();
        executeNewlevel(arg.statementlist, u, false);
        obj = u.toString();
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        obj = value.objValue();
      } else obj = null;  //throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
      return obj;
    }
    
    void throwIllegalDstArgument(CharSequence text, DataAccess dst, ZGenScript.Statement statement)
    throws IllegalArgumentException
    {
      StringBuilder u = new StringBuilder(100);
      u.append("ZGen - ").append(text).append(";").append(dst);
      u.append("; in file ").append(statement.parentList.srcFile);
      u.append(", line ").append(statement.srcLine).append(" col ").append(statement.srcColumn);
      throw new IllegalArgumentException(u.toString());
    }
    
  }    
  /**Small class instance to build a next number. 
   * Note: It is anonymous to encapsulate the current number value. 
   * The only one access method is Object.toString(). It returns a countered number.
   */
  private final Object nextNr = new Object(){
    int nr = 0;
    @Override
    public String toString(){
      return "" + ++nr;
    }
  };
  
  
  
  
  

  
  /**Class only to check argument lists and use default values for arguments. */
  private class CheckArgument
  {
    /**Reference to the formal argument. */
    final ZGenScript.Argument formalArg;
    
    /**Set to true if this argument is used. */
    boolean used;
    
    CheckArgument(ZGenScript.Argument formalArg){ this.formalArg = formalArg; }
  }
  
  /**It wrapps the File currDir because only this instance is referred in all localVariables.
   * Changing the currDir should not change the content of a localVariables association
   * but the reference of {@link CurrDir#currDir} instead. In this case all localvariables
   * and the scriptVariable gets the changed current directory.
   */
  protected static class CurrDir{
    File currDir;
    @Override public String toString(){ return currDir.getAbsolutePath(); }
  }
  
  void stop(){
    
    
  }

  
  
  /**State variable of a running thread or finished thread.
   * This instance will be notified if any waits (join operation)
   */
  protected class ZgenThreadResult implements CharSequence
  {
    StringBuilder uText = new StringBuilder();
    
    /**State of thread execution. 
     * <ul>
     * <li>i: init
     * <li>r: runs
     * <li>y: finished
     * </ul>
     */
    char state = 'i';

    
    protected void clear(){
      state = 'i';
    }
    
    @Override public char charAt(int index){ return uText.charAt(index); }

    @Override public int length(){ return uText.length(); }

    @Override public CharSequence subSequence(int start, int end){ return uText.subSequence(start, end); }
    
  }
  
  
  
  /**A thread instance of ZGen.
   */
  protected class ZGenThread implements Runnable
  {
    final ExecuteLevel executeLevel;

    final ZGenScript.Statement statement;

    final ZgenThreadResult result;
    
    public ZGenThread(ExecuteLevel executeLevel, Statement statement, ZgenThreadResult result)
    {
      this.executeLevel = executeLevel;
      this.statement = statement;
      this.result = result;
    }

    @Override public void run(){ 
      runThread(executeLevel, statement); 
      synchronized(threads){
        boolean bOk = threads.remove(this);
        assert(bOk);
        if(threads.size() == 0){
          threads.notify();
        }
      }
    }
    
  }
  
  
  public static class ExitException extends Exception
  {
    public int exitLevel;
    
    public ExitException(int exitLevel){
      this.exitLevel = exitLevel;
    }
  }
  
  
  
  
}
