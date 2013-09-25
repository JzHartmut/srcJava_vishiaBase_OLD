package org.vishia.cmd;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
public class JbatchExecuter {
  
  
  /**Version and history
   * <ul>
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
  static final public int version = 20121010;

  protected Object XXXdata;
  
  protected String sError = null;
  
  /**Variable for any exception while accessing any java ressources. It is the $error variable of the script. */
  protected String accessError = null;
  
  public final boolean bWriteErrorInOutput;
  
  private boolean bAccessPrivate;
  
  protected final MainCmdLogging_ifc log;
  
  /**The output which is given by invocation of {@link #generate(Object, File, Appendable, boolean, Appendable)}
   */
  protected Appendable outFile;
  
  /**The java prepared generation script. */
  JbatchScript genScript;
  
  /**Instance for the main script part. */
  //Gen_Content genFile;

  /**Generated content of all script variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  final Map<String, Object> scriptVariables = new_Variables();
  
  /**Generated content of all script environment variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  //final Map<String, String> scriptEnvVariables = new TreeMap<String, String>();
  
  
  protected final Queue<JbatchThread> threads = new ConcurrentLinkedQueue<JbatchThread>();
  
  private boolean bScriptVariableGenerated;
  
  
  /**The newline char sequence. */
  String newline = "\r\n";
  

  public JbatchExecuter(MainCmdLogging_ifc log){
    this.log = log;
    bWriteErrorInOutput = false;
  }
  
  
  public void setOutfile(Appendable outfile){ 
    this.outFile = outfile; 
    scriptVariables.put("text", outfile);
  }
  
  public Appendable outFile(){ return outFile; }
  
  public void setScriptVariable(String name, Object content){
    scriptVariables.put(name, content);
  }
  
  
  
  /**Generates script-global variables.
   * 
   * @param genScript It should be the same how used on {@link #genContent(JbatchScript, Object, boolean, Appendable)}
   *   but it may be another one for special cases.
   * @param userData Used userdata for content of scriptvariables. It should be the same how used on 
   *   {@link #genContent(JbatchScript, Object, boolean, Appendable)} but it may be another one for special cases.
   * @param accessPrivate true than access to private data of userData
   * @return The built script variables. 
   *   One can evaluate some script variables before running {@link #genContent(JbatchScript, Object, boolean, Appendable)}.
   *   Especially it is used for {@link org.vishia.zmake.Zmake to set the currDir.} 
   * @throws IOException
   */
  public Map<String, Object> genScriptVariables(JbatchScript genScript, boolean accessPrivate) 
  throws IOException
  {
    this.genScript = genScript;
    //this.data = userData;
    this.bAccessPrivate = accessPrivate;
    CurrDir currDirWrapper = new CurrDir();
    currDirWrapper.currDir = new File(".").getAbsoluteFile().getParentFile();
    StringSeq cd = new StringSeq();
    cd.change(FileSystem.normalizePath(currDirWrapper.currDir.getAbsolutePath()));
    scriptVariables.put("$CD", cd);
    scriptVariables.put("currDir", currDirWrapper);
    scriptVariables.put("error", accessError);
    scriptVariables.put("mainCmdLogging", log);
    scriptVariables.put("nextNr", nextNr);
    scriptVariables.put("nrElementInContainer", null);
    scriptVariables.put("out", System.out);
    scriptVariables.put("err", System.err);
    scriptVariables.put("null", null);
    scriptVariables.put("jbat", this);
    //scriptVariables.put("debug", new ZbatchDebugHelper());
    scriptVariables.put("file", new FileSystem());

    for(JbatchScript.Statement scriptVariableScript: genScript.getListScriptVariables()){
      StringBuilder uVariable = new StringBuilder();
      ExecuteLevel genVariable = new ExecuteLevel(null, scriptVariables); //NOTE: use recent scriptVariables.
      genVariable.execute(scriptVariableScript.getSubContent(), uVariable, false);
      scriptVariables.put(scriptVariableScript.identArgJbat, uVariable); //Buffer.toString());
    }
    bScriptVariableGenerated = true;
    return scriptVariables;
  }
  


  
  public void reset(){
    bScriptVariableGenerated = false;
    scriptVariables.clear();
    sError = null;
  }
  
  
  /**Generates an output with the given script.
   * @param genScript Generation script in java-prepared form. Parse it with {@link #parseGenScript(File, Appendable)}.
   * @param out Any output. It is used for direct text output and it is stored as variable "text"
   *   to write "<+text>...<.+>" to output to it.
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  public void execute(JbatchScript genScript, boolean accessPrivate, boolean bWaitForThreads, Appendable out) 
  throws IOException
  {
    this.bAccessPrivate = accessPrivate;
    //this.data = userData;
    this.genScript = genScript;

    if(!bScriptVariableGenerated){
      genScriptVariables(genScript, accessPrivate);
    }
    setScriptVariable("text", out);
    JbatchScript.Statement contentScript = genScript.getFileScript();
    ExecuteLevel genFile = new ExecuteLevel(null, scriptVariables);
    String sError1 = genFile.execute(contentScript.subContent, out, false);
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

  
  /**Initializes, especially all script variables.
   * @param genScript Generation script in java-prepared form. Parse it with {@link #parseGenScript(File, Appendable)}.
   * @param out Any output. It is used for direct text output and it is stored as variable "text"
   *   to write "<+text>...<.+>" to output to it.
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  public String initialize(JbatchScript genScript, boolean accessPrivate) 
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
  public String execSub(JbatchScript.Statement statement, Map<String, Object> args
      , boolean accessPrivate, Appendable out) 
  throws IOException
  {
    ExecuteLevel level = new ExecuteLevel(null, scriptVariables);
    //The args should be added to the localVariables of the subroutines level:
    level.localVariables.putAll(args);
    //Executes the statements of the sub routine:
    String sError1 = level.execute(statement.subContent, out, false);
    return sError1;
  }
  
  
  
  
  public Object getScriptVariable(String name){ return scriptVariables.get(name); }

  
  
  

  protected Map<String, Object> xnew_Variables(){
    return new TreeMap<String, Object>();
    
  }
  

  protected Map<String, Object> new_Variables(){
    return new IndexMultiTable<String, Object>(IndexMultiTable.providerString);
  }
  
  
  public void runThread(ExecuteLevel executeLevel, JbatchScript.Statement statement){
    try{ 
      executeLevel.execute(statement.subContent, null, false);
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
  
  /**This class wraps any Object which is used for a local variable.
   * If an inner level changes the Object of the variable, all references to the variable's content
   * should be changed in all outer levels too. This can be done by unchanged reference to the variable itself
   * in all {@link ExecuteLevel#localVariables} maps but change of the reference of {@link LocalVariable#var}.
   */
  static class XXXLocalVariable{
    Object var;
  }
  
  /**Wrapper to generate a script with specified localVariables.
   * A new Wrapper is created on <:file>, <:subtext> or on abbreviated output, especially to generate into variables.
   *
   */
  public final class ExecuteLevel
  {
    /**Not used yet. Only for debug?
     * 
     */
    final ExecuteLevel parent;
    
    
    /**Generated content of local variables in this nested level including the {@link ZbatchExecuter#scriptVariables}.
     * The variables are type invariant on language level. The type is checked and therefore 
     * errors are detected on runtime only. */
    final Map<String, Object> localVariables;
    
    
    
    /**Constructs data for a local execution level.
     * @param parentVariables if given this variable are copied to the local ones.
     *   They contains the script variables too. If not given (null), only the script variables
     *   are copied into the {@link #localVariables}. Note that subroutines do not know the
     *   local variables of its calling routine! This argument is only set if nested statement blocks
     *   are to execute. 
     */
    public ExecuteLevel(ExecuteLevel parent, Map<String, Object> parentVariables)
    { this.parent = parent;
      localVariables = new_Variables();
      if(parentVariables == null){
        localVariables.putAll(scriptVariables);
      } else {
        localVariables.putAll(parentVariables);  //use the same if it is not a subText, only a 
      }
      localVariables.put("jbatSub", this);
      localVariables.put("jbatSubVariables", localVariables);
      localVariables.put("jbatExecuteLevel", this);
    }

    
    
    /**Put a value to a maybe inner local variable written with "name.subname.name".
     * Replace a given StringSeq with a given value as CharSequence by preserving the current instance
     * of StringSeq. It means all references to this StringSeq, especially script variables, are changed.
     * @param ident May contain dots
     * @param value The value to store.
     */
    void putOrReplaceLocalVariable(String ident, Object value){
      int start=0, end;
      Map<String, Object> var1 = localVariables;
      String ident1;
      while( (end = ident.indexOf('.')) >0){
        ident1 = ident.substring(start, end);
        @SuppressWarnings("unchecked")
        Map<String, Object> var2 = (Map<String, Object>)localVariables.get(ident1);
        if(var2 ==null){
          var2 = new TreeMap<String, Object>();
          var1.put(ident1, var2);
        }
        var1 = var2;
        start = end+1;
      }
      ident1 = ident.substring(start);
      ////
      Object oldvalue = var1.get(ident1);
      if(oldvalue !=null && oldvalue instanceof StringSeq && value instanceof CharSequence){
        //replace the inner CharSequence.
        ((StringSeq)oldvalue).change((CharSequence)value);
      } else {
        var1.put(ident1, value);
      }
    }
    
    
    
    
    /**Executes an inner script part maybe with a new level of nested local variables.
     * If the contentScript does not contain any variable definition 
     * (see @link {@link JbatchScript.StatementList#bContainsVariableDef}) then this level is used,
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
    public String executeNewlevel(JbatchScript.StatementList contentScript, final Appendable out, boolean bContainerHasNext) 
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
    public String execute(JbatchScript.StatementList contentScript, final Appendable out, boolean bContainerHasNext) 
    //throws IOException 
    {
      String sError = null;
      Appendable uBuffer = out;
      //Generate direct requested output. It is especially on inner content-scripts.
      int ixStatement = -1;
      //Iterator<JbatchScript.Statement> iter = contentScript.content.iterator();
      while(++ixStatement < contentScript.content.size()) { //iter.hasNext() && sError == null){
        JbatchScript.Statement contentElement = contentScript.content.get(ixStatement); //iter.next();
        //for(TextGenScript.ScriptElement contentElement: contentScript.content){
        try{    
          switch(contentElement.elementType){
          case 't': { 
            int posLine = 0;
            int posEnd;
            if(contentElement.textArg.startsWith("'''trans ==> dst"))
              stop();
            do{
              posEnd = contentElement.textArg.indexOf('\n', posLine);
              if(posEnd >= 0){ 
                uBuffer.append(contentElement.textArg.substring(posLine, posEnd));   
                uBuffer.append(newline);
                posLine = posEnd +1;  //after \n 
              } else {
                uBuffer.append(contentElement.textArg.substring(posLine));   
              }
              
            } while(posEnd >=0);  //output all lines.
          } break;
          case 'n': {
            uBuffer.append(newline);
          } break;
          case 'T': textAppendToVarOrOut(contentElement); break; 
          case 'S': setStringVariable(contentElement); break; 
          case 'P': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            localVariables.put(contentElement.identArgJbat, uBufferVariable);
          } break;
          case 'U': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            localVariables.put(contentElement.identArgJbat, uBufferVariable);
          } break;
          case 'L': {
            Object value = evalObject(contentElement, true); 
              //getContent(contentElement, localVariables, false);  //not a container
            localVariables.put(contentElement.identArgJbat, value);
            if(!(value instanceof Iterable<?>)) 
                throw new NoSuchFieldException("JbatExecuter - exec variable must be of type Iterable ;" + contentElement.identArgJbat);
          } break;
          case 'W': executeOpenfile(contentElement); break;
          case 'J': {
            if(contentElement.identArgJbat.equals("checkDeps"))
              stop();
            Object value = evalObject(contentElement, false);
            localVariables.put(contentElement.identArgJbat, value);
          } break;
          case 'e': executeDatatext(contentElement, out); break; 
          case 's': {
            executeSubroutine(contentElement, out);
          } break;
          case 'x': executeThread(contentElement); break;
          case 'm': executeMove(contentElement); break;
          case 'c': executeCmdline(contentElement); break;
          case 'd': executeChangeCurrDir(contentElement); break;
          case 'C': { //generation <:for:name:path> <genContent> <.for>
            executeForContainer(contentElement, out);
          } break;
          case 'B': { //statementBlock
            executeSubLevel(contentElement, out);  ///
          } break;
          case 'F': executeIfStatement(contentElement, out); break;
          case 'w': whileStatement(contentElement, out); break;
          case 'N': {
            executeIfContainerHasNext(contentElement, out, bContainerHasNext);
          } break;
          case '=': executeAssign(contentElement); break;
          case 'b': { sError = "break"; } break;
          case '?': break;  //don't execute a onerror, skip it.
          default: 
            uBuffer.append("Jbat - execute-unknown type; '" + contentElement.elementType + "' :ERROR=== ");
          }//switch
          
        } catch(Exception exc){
          //any statement has thrown an exception.
          //check onerror with proper error type anywhere after this statement, it is stored in the statement.
          //continue there.
          boolean found = false;
          char excType = '?';
          while(!found && ++ixStatement < contentScript.content.size()) { //iter.hasNext() && sError == null){
            contentElement = contentScript.content.get(ixStatement); //iter.next();
            char onerrorType;
            if(contentElement.elementType == '?' 
              && ((onerrorType = ((JbatchScript.Onerror)contentElement).errorType) == excType
                 || onerrorType == '?'  
              )  ){
              found = true;
            }
          }
          if(found){
            String sError1 = exc.getMessage();
            localVariables.put("errorMsg", sError1);
            executeSubLevel(contentElement, out);
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
    
    
    
    void executeForContainer(JbatchScript.Statement contentElement, Appendable out) throws Exception
    {
      JbatchScript.StatementList subContent = contentElement.getSubContent();  //The same sub content is used for all container elements.
      if(contentElement.identArgJbat.equals("include1"))
        stop();
      Object container = evalObject(contentElement, true);
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
            forExecuter.localVariables.put(contentElement.identArgJbat, foreachData);
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
            localVariables.put(contentElement.identArgJbat, foreachData);
            //genFor.
            execute(subContent, out, iter.hasNext());
          }
        }
      }
    }
    
    
    
    void executeIfContainerHasNext(JbatchScript.Statement hasNextScript, Appendable out, boolean bContainerHasNext) throws IOException{
      if(bContainerHasNext){
        //(new Gen_Content(this, false)).
        execute(hasNextScript.getSubContent(), out, false);
      }
    }
    
    
    
    /**it contains maybe more as one if block and else. 
     * @throws Exception */
    void executeIfStatement(JbatchScript.Statement ifStatement, Appendable out) throws Exception{
      Iterator<JbatchScript.Statement> iter = ifStatement.subContent.content.iterator();
      boolean found = false;  //if block found
      while(iter.hasNext() && !found ){
        JbatchScript.Statement contentElement = iter.next();
        switch(contentElement.elementType){
          case 'G': { //if-block
            
            found = executeIfBlock((JbatchScript.IfCondition)contentElement, out, iter.hasNext());
          } break;
          case 'E': { //elsef
            if(!found){
              execute(contentElement.subContent, out, false);
            }
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + contentElement.elementType + "' :ERROR=== ");
          }
        }//switch
      }//for
    }
    
    
    
    /**Executes a while statement. 
     * @throws Exception */
    void whileStatement(JbatchScript.Statement whileStatement, Appendable out) 
    throws Exception 
    {
      boolean cond;
      do{
        CalculatorExpr.Value check = whileStatement.expression.calcDataAccess(localVariables);
        cond = check.booleanValue();
        if(cond){
          execute(whileStatement.subContent, out, false);
        }
      } while(cond);  //if executed, check cond again.  
    }
    
    
    
    boolean executeIfBlock(JbatchScript.IfCondition ifBlock, Appendable out, boolean bIfHasNext) 
    throws Exception
    {
      //Object check = getContent(ifBlock, localVariables, false);
      
      CalculatorExpr.Value check;
      check = ifBlock.expression.calcDataAccess(localVariables);
      boolean bCheck = check.booleanValue();
      if(bCheck){
        execute(ifBlock.subContent, out, bIfHasNext);
      }
      return bCheck;
    }
    
    
    
    
    void setStringVariable(JbatchScript.Statement statement) 
    throws Exception 
    {
      CharSequence text = evalString(statement);
      if(statement.identArgJbat.equals("$CD")){
        //special handling of current directory:
        setCurrDir(text);  //normalize, set "currDir"
      } else {
        putOrReplaceLocalVariable(statement.identArgJbat, text);
      }
    } 
    
    
    /**Invocation for <+name>text<.+>
     * @param contentElement
     * @throws IOException 
     */
    void textAppendToVarOrOut(JbatchScript.Statement contentElement) throws IOException{
      
      String name = contentElement.identArgJbat;
      Appendable out1;
      Object variable = localVariables.get(name);
      boolean put;
      if(variable == null){
        throw new NoSuchElementException("JbatExecuter - textAppend, variable not found; "+ name);
      }
      if(variable instanceof StringSeq){
        out1 = ((StringSeq)variable).changeIt();
        put = false;
      }
      else if(variable instanceof Appendable){
        out1 = (Appendable)variable;  //append, it may be a StringBuilder.
        put = false;
      }
      else if(variable instanceof CharSequence){  //especially a String
        //don't change this charSequence, build a new one
        out1 = new StringBuilder(((CharSequence)variable));
        put = true;
      }
      else {
        throw new NoSuchElementException("JbatExecuter - textAppend, variable faulty type; " + variable.getClass().getName());
      }
      ExecuteLevel genContent = new ExecuteLevel(this, localVariables);
      genContent.execute(contentElement.subContent, out1, false);
      if(put){
        localVariables.put(name, out1);  //replace content.
      }
    }
    
    

    
    
    
    
    
    void executeSubroutine(JbatchScript.Statement contentElement, Appendable out) 
    throws IllegalArgumentException, Exception
    {
      JbatchScript.CallStatement callStatement = (JbatchScript.CallStatement)contentElement;
      boolean ok = true;
      final CharSequence nameSubtext;
      /*
      if(contentElement.name == null){
        //subtext name gotten from any data location, variable name
        Object oName = ascertainText(contentElement.expression, localVariables); //getContent(contentElement, localVariables, false);
        nameSubtext = DataAccess.getStringFromObject(oName, null);
      } else {
        nameSubtext = contentElement.name;
      }*/
      nameSubtext = evalString(callStatement.callName); 
      JbatchScript.Statement subtextScript = genScript.getSubtextScript(nameSubtext);  //the subtext script to call
      if(subtextScript == null){
        throw new NoSuchElementException("JbatExecuter - subroutine not found; " + nameSubtext);
      } else {
        ExecuteLevel subtextGenerator = new ExecuteLevel(this, null);
        if(subtextScript.arguments !=null){
          //build a Map temporary to check which arguments are used:
          TreeMap<String, CheckArgument> check = new TreeMap<String, CheckArgument>();
          for(JbatchScript.Argument formalArg: subtextScript.arguments) {
            check.put(formalArg.identArgJbat, new CheckArgument(formalArg));
          }
          //process all actual arguments:
          List<JbatchScript.Argument> referenceSettings = contentElement.getReferenceDataSettings();
          if(referenceSettings !=null){
            for( JbatchScript.Argument referenceSetting: referenceSettings){  //process all actual arguments
              Object ref;
              ref = evalObject(referenceSetting, false);
              //ref = ascertainValue(referenceSetting.expression, data, localVariables, false);       //actual value
              if(ref !=null){
                CheckArgument checkArg = check.get(referenceSetting.identArgJbat);      //is it a requested argument (per name)?
                if(checkArg == null){
                  ok = writeError("??: *subtext;" + nameSubtext + ": " + referenceSetting.identArgJbat + " faulty argument.?? ", out);
                } else {
                  checkArg.used = true;    //requested and resolved.
                  subtextGenerator.localVariables.put(referenceSetting.identArgJbat, ref);
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
                    subtextGenerator.localVariables.put(arg.formalArg.identArgJbat, ref);
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
        } else if(contentElement.getReferenceDataSettings() !=null){
          ok = writeError("??: *subtext;" + nameSubtext + " called with arguments, it has not one.??", out);
        }
        if(ok){
          subtextGenerator.execute(subtextScript.subContent, out, false);
        }
      }
    }
    
    
    
    
    /**executes statements in another thread.
     */
    private void executeThread(JbatchScript.Statement statement) 
    throws IOException
    { JbatchThread thread = new JbatchThread();
      synchronized(threads){
        threads.add(thread);
      }
      thread.executeLevel = this;
      thread.statement = statement;
      Thread threadmng = new Thread(thread, "jbat");
      threadmng.start();  
      //it does not wait on finishing this thread.
    }

    
    
    
    /**Generates or executes any sub content.
     * @param script
     * @param out
     * @return
     * @throws IOException
     */
    public String executeSubLevel(JbatchScript.Statement script, Appendable out) 
    //throws IOException
    {
      ExecuteLevel genContent;
      if(script.subContent.bContainsVariableDef){
        genContent = new ExecuteLevel(this, localVariables);
      } else {
        genContent = this;  //don't use an own instance, save memory and calculation time.
      }
      return genContent.execute(script.subContent, out, false);
    }

    
    
    void executeCmdline(JbatchScript.Statement contentElement) 
    throws IllegalArgumentException, Exception
    {
      boolean ok = true;
      final CharSequence sCmd;
      if(contentElement.identArgJbat == null){
        //cmd gotten from any data location, variable name
        sCmd = evalString(contentElement); 
      } else {
        sCmd = contentElement.identArgJbat;
      }
      String[] args;
      if(contentElement.arguments !=null){
        args = new String[contentElement.arguments.size() +1];
        int iArg = 1;
        for(JbatchScript.Argument arg: contentElement.arguments){
          String sArg = evalString(arg).toString(); //XXXascertainText(arg.expression, localVariables);
          args[iArg++] = sArg;
        }
      } else { 
        args = new String[1]; 
      }
      args[0] = sCmd.toString();
      List<Appendable> outCmd;
      if(contentElement.assignObj !=null){
        outCmd = new LinkedList<Appendable>();
        for(DataAccess assignObj1 : contentElement.assignObj){
          Object oOutCmd = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
          //Object oOutCmd = localVariables.get(contentElement.sVariableToAssign);
          if(oOutCmd instanceof Appendable){
            outCmd.add((Appendable)oOutCmd);
          } else {
            //TODO error
            //outCmd = null;
          }
        }
      } else {
        outCmd = null;
      }
      CmdExecuter cmdExecuter = new CmdExecuter();
      
      CurrDir currDir = (CurrDir)localVariables.get("currDir");
      //localVariables.
      cmdExecuter.setCurrentDir(currDir.currDir);
      cmdExecuter.execute(args, null, outCmd, null);
    }
    

    void executeChangeCurrDir(JbatchScript.Statement statement)
    throws Exception
    {
      CharSequence arg = evalString(statement);
      setCurrDir(arg); 
    }

    
    private void setCurrDir(CharSequence arg){
      String sCurrDir;
      final CharSequence arg1;
      StringSeq cd1 = (StringSeq)localVariables.get("$CD");
      if(FileSystem.isAbsolutePathOrDrive(arg)){
        arg1 = arg;
      } else {
        StringBuilder u = cd1.changeIt(); //new StringBuilder(cd1.length() + arg.length()+1);
        u.append('/').append(arg);   //concatenate a relativ path
        arg1 = u;
      }
      cd1.change(FileSystem.normalizePath(arg1));   //resolve "xxx/../xxx"
      sCurrDir = cd1.toString();
      CurrDir currDirWrapper = (CurrDir)localVariables.get("currDir");
      currDirWrapper.currDir = new File(sCurrDir);
      
    }
    
     
    
    private void executeDatatext(JbatchScript.Statement statement, Appendable out)  //<*datatext>
    throws IllegalArgumentException, Exception
    {
      Object obj = evalDatapathOrExpr(statement); //ascertainText(contentElement.expression, localVariables);
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
    
    void executeMove(JbatchScript.Statement statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence s1 = evalString(statement.arguments.get(0));
      CharSequence s2 = evalString(statement.arguments.get(1));
      File fileSrc = new File(s1.toString());
      File fileDst = new File(s2.toString());
      boolean bOk = fileSrc.renameTo(fileDst);
      if(!bOk) throw new IOException("JbatchExecuter - move not successfully; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
    void executeOpenfile(JbatchScript.Statement contentElement) 
    throws IllegalArgumentException, Exception
    {
      String sFilename = evalString(contentElement).toString();
      Writer writer = new FileWriter(sFilename);
      localVariables.put(contentElement.identArgJbat, writer);
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
     * @param contentElement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    void executeAssign(JbatchScript.Statement contentElement) 
    throws IllegalArgumentException, Exception
    {
      Object val = evalObject(contentElement, false);
      //Object val = ascertainValue(contentElement.expression, data, localVariables, false);
      if(contentElement.assignObj !=null){
        for(DataAccess assignObj1 : contentElement.assignObj){
        
          //It is a path to any object, get it:
          Object oOut = assignObj1.getDataObj(localVariables, bAccessPrivate, false);
          //
          if(oOut == null){
            //not found.
            if(assignObj1.datapath().size()==1){
              //only a name of a localVariable is given: 
              String name = assignObj1.datapath().get(0).ident;
              localVariables.put(name, val);
            } else {
              throw new NoSuchFieldException("AssignObject not found: " + assignObj1.datapath().toString());
            }
          } else {
            //
            //check its type:
            //
            if(oOut instanceof Appendable){
              ((Appendable)oOut).append(val.toString());
            } else {
              
              throw new NoSuchFieldException("AssignObject type not supported: " + assignObj1.datapath().toString());
            }
          }
        }
      }
      
    }
    
    
    
    /**Checks either the {@link JbatchScript.Argument#dataAccess} or, if it is null,
     * the {@link JbatchScript.Argument#expression}. Returns either the Object which is gotten
     * by the {@link DataAccess#getDataObj(Map, boolean, boolean)} or which is calculated
     * by the expression. Returns an instance of {@link CalculatorExpr.Value} if it is 
     * a result by an expression.
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalDatapathOrExpr(JbatchScript.Argument arg) throws Exception{
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
    
    
    
    public CharSequence evalString(JbatchScript.Argument arg) throws Exception{
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        Object o = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o.toString(); }
      } else if(arg.subContent !=null){
        StringBuilder u = new StringBuilder();
        executeNewlevel(arg.subContent, u, false);
        return StringSeq.create(u);
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        return value.stringValue();
      } else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
    }
    
    
    
    /**Gets the value of the given Argument. Either it is a 
     * <ul>
     * <li>String from {@link JbatchScript.Argument#textArg}
     * <li>Object from {@link JbatchScript.Argument#datapath()}
     * <li>Object from {@link JbatchScript.Argument#expression}
     * <li>
     * </ul>
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalObject(JbatchScript.Argument arg, boolean bContainer) throws Exception{
      Object obj;
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        obj = arg.dataAccess.getDataObj(localVariables, bAccessPrivate, false);
      } else if(arg.subContent !=null){
        StringBuilder u = new StringBuilder();
        executeNewlevel(arg.subContent, u, false);
        obj = u.toString();
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        obj = value.objValue();
      } else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
      return obj;
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
  
  
  
  /**This class enhances {@link CalculatorExpr} to process also a string expression <:>...<.>
   * as capability of Zbatch.
   */
  public static class ZbatchExpression extends CalculatorExpr
  {
    /**If need, a sub-content, maybe null.*/
    public JbatchScript.StatementList genString;

    
    
    /**Calculates a normal expression or a String expression of Zbatch style "<:>...<.>".
     * see {@link CalculatorExpr#calcDataAccess(java.util.Map, java.lang.Object[])}
     */
    @Override public CalculatorExpr.Value calcDataAccess(Map<String, Object> javaVariables, Object... args) 
    throws Exception{
      if(genString !=null){
        JbatchExecuter.ExecuteLevel executer = (JbatchExecuter.ExecuteLevel)javaVariables.get("jbatExecuteLevel");
        StringBuilder u = new StringBuilder();
        executer.executeNewlevel(genString, u, false);
        return new CalculatorExpr.Value(u.toString());
      } else {
        return super.calcDataAccess(javaVariables, args);
      }
    }
    
  }
  
  
  

  
  /**Class only to check argument lists and use default values for arguments. */
  private class CheckArgument
  {
    /**Reference to the formal argument. */
    final JbatchScript.Argument formalArg;
    
    /**Set to true if this argument is used. */
    boolean used;
    
    CheckArgument(JbatchScript.Argument formalArg){ this.formalArg = formalArg; }
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

  
  
  protected class JbatchThread implements Runnable
  {
    ExecuteLevel executeLevel;
    ////
    JbatchScript.Statement statement;

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
  
  
}
