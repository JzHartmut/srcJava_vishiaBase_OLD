package org.vishia.cmd;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringSeq;


/**This class contains control data and sub-routines to generate output texts from internal data.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ZGenScript {
  /**Version, history and license.
   * <ul>
   * <li>2014-07-30 Hartmut chg {@link #translateAndSetGenCtrl(File)} returns void.
   * <li>2014-07-20 Hartmut chg Some syntactical changes.
   * <li>2013-07-14 Hartmut tree traverse enable because {@link Argument#parentList} and {@link StatementList#parentStatement}
   * <li>2013-06-20 Hartmut new: Syntax with extArg for textual Arguments in extra block
   * <li>2013-03-10 Hartmut new: <code><:include:path></code> of a sub script is supported up to now.
   * <li>2013-10-09 Hartmut new: <code><:scriptclass:JavaPath></code> is supported up to now.
   * <li>2013-01-13 Hartmut chg: The {@link ZbatchExpressionSet#ascertainValue(Object, Map, boolean, boolean, boolean)} is moved
   *   and adapted from TextGenerator.getContent. It is a feauture from the Expression to ascertain its value.
   *   That method and {@link ZbatchExpressionSet#text()} can be invoked from a user script immediately.
   *   The {@link ZbatchExpressionSet} is used in {@link org.vishia.zmake.ZmakeUserScript}.
   * <li>2013-01-02 Hartmut chg: localVariableScripts removed. The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   * <li>2012-12-24 Hartmut chg: Now the 'ReferencedData' are 'namedArgument' and it uses 'dataAccess' inside. 
   *   The 'dataAccess' is represented by a new {@link Statement}('e',...) which can have {@link ZbatchExpressionSet#constValue} 
   *   instead a {@link ZbatchExpressionSet#datapath}. 
   * <li>2012-12-24 Hartmut chg: {@link ZbnfDataPathElement} is a derived class of {@link DataAccess.DatapathElement}
   *   which contains destinations for argument parsing of a called Java-subroutine in a dataPath.  
   * <li>2012-12-23 Hartmut chg: A {@link Statement} and a {@link Argument} have the same usage aspects for arguments
   *   which represents values either as constants or dataPath. Use Argument as super class for ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link ZbatchExpressionSet#textArg} if a data path is given, use for formatting a numerical value.   
   * <li>2012-12-22 Hartmut new: Syntax as constant string inside. Some enhancements to set control: {@link #translateAndSetGenCtrl(StringPartBase)} etc.
   * <li>2012-12-22 Hartmut chg: <:if:...> uses {@link CalculatorExpr} for expressions.
   * <li>2012-11-24 Hartmut chg: @{@link Statement#datapath} with {@link DataAccess.DatapathElement} 
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-19 Hartmut chg: Renaming: {@link Statement} instead Zbnf_ScriptElement (shorter). The Scriptelement
   *   is the component for the genContent-Elements now instead Zbnf_genContent. This class contains attributes of the
   *   content elements. Only if a sub content is need, an instance of Zbnf_genContent is created as {@link Statement#statementlist}.
   *   Furthermore the {@link Statement#statementlist} should be final because it is only created if need for the special 
   *   {@link Statement#elementType}-types (TODO). This version works for {@link org.vishia.stateMGen.StateMGen}.
   * <li>2012-10-11 Hartmut chg Syntax changed of ZmakeGenCtrl.zbnf: datapath::={ <$?path>? \.}. 
   *   instead dataAccess::=<$?name>\.<$?elementPart>., it is more universal. adapted. 
   * <li>2012-10-10 new: Some enhancements, it is used for {@link org.vishia.zbatch.ZbatchExecuter} now too.
   * <li>2011-03-00 created.
   *   It is the concept of specialized {@link GralWidget}.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
  static final public int version = 20130310;

  final MainCmdLogging_ifc console;

  /**Helper to transfer parse result into the java classes {@link ZbnfMainGenCtrl} etc. */
  //final ZbnfJavaOutput parserGenCtrl2Java;

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  //ZbnfMainGenCtrl zTextGenCtrl;
  
  //final Map<String, Statement> zmakeTargets = new TreeMap<String, Statement>();
  
  final Map<String, Statement> subScriptsAll = new TreeMap<String, Statement>();
  
  
  
  /**List of the script variables in order of creation in the jbat script file and all includes.
   * The script variables can contain inputs of other variables which are defined before.
   * Therefore the order is important.
   * This list is stored firstly in the {@link StatementList#statements} in an instance of 
   * {@link ZbnfMainGenCtrl} and then transferred from all includes and from the main script 
   * to this container because the {@link ZbnfMainGenCtrl} is only temporary and a ensemble of all
   * Statements should be present from all included files. The statements do not contain
   * any other type of statement than script variables because only ScriptVariables are admissible
   * in the syntax. Outside of subroutines and main there should only exist variable definitions. 
   */
  private final List<Statement> listScriptVariables = new ArrayList<Statement>();

  /**The script element for the whole file. It shall contain calling of <code><*subtext:name:...></code> 
   */
  Statement scriptFile;
  
  /**The class which presents the script level. */
  ZGenClass scriptClass;
  
  //public String scriptclassMain;

  public ZGenScript(Object executer, MainCmdLogging_ifc console)
  { this.console = console;

  }

  public ZGenClass scriptClass(){ return scriptClass; }
  
  
  public void setFromIncludedScript(ZbnfMainGenCtrl includedScript){
    if(includedScript.getMainRoutine() !=null){
      //use the last found main, also from a included script but firstly from main.
      scriptFile = includedScript.getMainRoutine();   
    }
    if(includedScript.outer.scriptClass.statements !=null){
      listScriptVariables.addAll(includedScript.outer.scriptClass.statements);
    }

  }
  
  public final Statement getMain(){ return scriptFile; }
  
  
  public Statement getSubtextScript(CharSequence name){ return subScriptsAll.get(name.toString()); }
  
  
  public List<Statement> getListScriptVariables(){ return listScriptVariables; }




  
  
  
  
  
  
  
  
  /**Superclass for ScriptElement, but used independent for arguments.
   * @author hartmut
   *
   */
  public static class Argument  { //CalculatorExpr.Datapath{
    
    /**Hint to the source of this parsed argument or statement. */
    int srcLine, srcColumn;
    
    /**Necessary for throwing exceptions with the {@link StatementList#srcFile} in its text. */
    final StatementList parentList;
    
    /**Name of the argument. It is the key to assign calling argument values. */
    public String identArgJbat;
   
    /**Any calculation of data. */
    public CalculatorExpr expression;
    
    
    /**Any access to an Object, maybe content of a variable, maybe access to any Java data,
     * maybe invocation of a Java routine. */
    public DataAccess dataAccess;
  
    /**From Zbnf <""?textInStatement>, constant text, null if not used. */
    public StringSeq textArg; 
    
    
    
    /**If need, sub statements, maybe null. An argument may need a StatementList
     * to calculate the value of the argument it is is more complex. Alternatively
     * an Argument can be calculated with the {@link #expression} or with {@link #dataAccess}
     * or it is a simple {@link #textArg}.*/
    public StatementList statementlist;
    
    
    
    
    public Argument(StatementList parentList){
      if(parentList == null)
        Assert.stop();
      this.parentList = parentList;
    }
    
    public void set_name(String name){ this.identArgJbat = name; }
    
    public String getIdent(){ return identArgJbat; }
    
    public CalculatorExpr.SetExpr new_expression(){ return new CalculatorExpr.SetExpr(true, this); }
    
    public void add_expression(CalculatorExpr.SetExpr val){ 
      val.closeExprPreparation();
      expression = val.expr; 
    }
    
    /**From Zbnf: < condition>. A condition is an expression. It is the same like {@link #new_expression()}
     */
    public CalculatorExpr.SetExpr new_condition(){ return new_expression(); }
    
    public void add_condition(CalculatorExpr.SetExpr val){ add_expression(val); }
    
    public void set_text(String text){
      CharSequence cText;
      if(text.contains("\n=")){   //= on start of line, remove it
        StringBuilder u = new StringBuilder(text);
        cText = u;
        int pos = 0;
        while( (pos = u.indexOf("\n=",pos))>=0){
          u.replace(pos+1, pos+2, "");
        }
      } else {
        cText = text;
      }
      textArg = StringSeq.create(cText, true);  //let the text inside the StringBuilder.
      //if(statementlist == null){ statementlist = new StatementList(this); }
      //statementlist.set_text(text);
    }
    
    
    public void XXXset_nonEmptyText(String text){
      if(!StringFunctions.isEmptyOrOnlyWhitespaces(text)){
        set_text(text);
      }
    }
    
    
    
    public void XXXset_textReplf(String text){
      set_text(text);
    }
    
    
    
    /**From Zbnf, a part <:>...<.> */
    public StatementList new_textExpr(){ return this.statementlist = new StatementList(); }
    
    public void add_textExpr(StatementList val){}
    
    
    
    public DataAccess.DataAccessSet new_datapath(){ return new DataAccess.DataAccessSet(); }
    
    public void add_datapath(DataAccess.DataAccessSet val){ 
      dataAccess = val;
    }
    


    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    //@Override
    public Statement new_dataText(){ return new Statement(parentList, 'e', null); }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    //@Override
    public void add_dataText(Statement val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val); 
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);
    }
    

    
  }
  
  
  
  /**An element of the script, maybe a simple text, an condition etc.
   * It may have sub statements , see aggregation {@link #statementlist}. 
   * <br>
   * UML-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *   Statement                 Statements          Statement
   *        |                         |              !The sub statements
   *        |-----statementlist------>|                  |
   *        |                         |                  |
   *                                  |----statements--*>|
   * 
   * </pre> 
   */
  public static class Statement extends Argument
  {
    /**Designation what presents the element.
     * 
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>n</td><td>simple newline text</td></tr>
     * <tr><td>T</td><td>textual output to any variable or file</td></tr>
     * <tr><td>l</td><td>add to list</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>e</td><td>A datatext, from <*expression> or such.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #textArg}==null, {@link #statementlist} == null.</td></tr>
     * <tr><td>j</td><td>call of a static java method. {@link #identArgJbat}==its name, {@link #statementlist} == null.</td></tr>
     * <tr><td>c</td><td>cmd line invocation.</td></tr>
     * <tr><td>d</td><td>cd change current directory.</td></tr>
     * <tr><td>J</td><td>Object variable {@link #identArgJbat}==its name, {@link #statementlist} == null.</td></tr>
     * <tr><td>P</td><td>Pipe variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>U</td><td>Buffer variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>S</td><td>String variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>L</td><td>Container variable, a list</td></tr>
     * <tr><td>W</td><td>Opened file, a Writer in Java</td></tr>
     * <tr><td>=</td><td>assignment of an expression to a variable.</td></tr>
     * <tr><td>B</td><td>statement block</td></tr>
     * <tr><td>C</td><td><:for:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>E</td><td><:else> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>F</td><td><:if:condition:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>G</td><td><:elsif:condition:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>w</td><td>while(cond) {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>b</td><td>break</td></tr>
     * <tr><td>?</td><td><:if:...?gt> compare-operation in if</td></tr>
     * 
     * <tr><td>Z</td><td>a target,</td></tr>
     * <tr><td>Y</td><td>the file</td></tr>
     * <tr><td>xxxX</td><td>a subtext definition</td></tr>
     * </table> 
     */
    private char elementType;    
    
    
    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex datapath
     * null if not used. */
    public List<DataAccess> assignObjs;
    
    
    /**The variable which should be created or to which a value is assigned to. */
    public DataAccess variable;
    
    //public String value;
    
    //public List<String> path;
    
    
    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    public List<Argument> arguments;
    
    /**The statements in this sub-ScriptElement were executed if an exception throws
     * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
     * If it is null, no exception handling is done.
     * <br><br>
     * This block can contain any statements as error replacement. If they fails too,
     * the iferror-Block can contain an iferror too.
     * 
     */
    public List<Onerror> onerror;
    
    

    
    public Statement(StatementList parentList, char whatisit, StringSeq text)
    { super(parentList);
      this.elementType = whatisit;
      this.textArg = text;
      if("BNXYZvl".indexOf(whatisit)>=0){
        statementlist = new StatementList();
      }
      else if("IVL".indexOf(whatisit)>=0){
        statementlist = new StatementList(this);
      }
    }
    
    /*package private*/ char elementType(){ return elementType; }
    
    public List<Argument> getReferenceDataSettings(){ return arguments; }
    
    public StatementList getSubContent(){ return statementlist; }
    
    @Override
    public void set_name(String name){ this.identArgJbat = name; }
    
    
    public void set_formatText(String text){ this.textArg = StringSeq.create(text); }
    
    /**Gathers a text which is assigned to any variable or output. <+ name>text<.+>
     */
    public Statement new_textOut(){ return new Statement(parentList, 'T', null); }

    public void add_textOut(Statement val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val); 
    } 
    
    
    public void set_newline(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new Statement(parentList, 'n', null));   /// 
    }
    
    public Statement new_setEnvVar(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_setEnvVar(); 
    }

    public void add_setEnvVar(Statement val){ statementlist.add_setEnvVar(val); } 
    
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public Statement new_textVariable(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new Statement(parentList, 'S', null); 
    } 

    public void add_textVariable(Statement val){ statementlist.statements.add(val); statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);} 
    
    
    /**Defines a variable which is able to use as pipe.
     */
    public Statement new_Pipe(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new Statement(parentList, 'P', null); 
    } 

    public void add_Pipe(Statement val){ statementlist.statements.add(val); statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val); }
    
    /**Defines a variable which is able to use as String buffer.
     */
    public Statement new_StringBuffer(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new Statement(parentList, 'U', null); 
    } 

    public void add_StringBuffer(Statement val){ statementlist.statements.add(val);  statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);}
    
        
    /**Defines a variable which is able to use as container.
     */
    public Statement new_List(){ ////
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new Statement(parentList, 'L', null); 
    } 

    public void add_List(Statement val){ statementlist.statements.add(val);  statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as pipe.
     */
    public Statement new_Openfile(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new Statement(parentList, 'W', null); 
    } 

    public void add_Openfile(Statement val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);  
      statementlist.onerrorAccu = null; 
      statementlist.withoutOnerror.add(val);
    }
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public Statement new_objVariable(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.bContainsVariableDef = true; 
      return new Statement(parentList, 'J', null); 
    } 

    public void add_objVariable(Statement val){ statementlist.statements.add(val); statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);}
    
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_formalArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_formalArgument(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      arguments.add(val); }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_p1(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_p1(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      if(arguments.size() >=1){
        arguments.set(0,val);
      } else {  //size is 0
        arguments.add(val);
      }
    }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_p2(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_p2(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      if(arguments.size() >=2){
        arguments.set(1,val);
      } else {
        while(arguments.size() < 1){ arguments.add(null); }  //empty
        arguments.add(val);
      }
    }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_actualArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      arguments.add(val); }
    
    
    /**From Zbnf: [{ <datapath?-assign> = }] 
     */
    public DataAccess.DataAccessSet new_assign(){ return new DataAccess.DataAccessSet(); }
    
    public void add_assign(DataAccess.DataAccessSet val){ 
      if(variable == null){ variable = val; }
      else {
        if(assignObjs == null){ assignObjs = new LinkedList<DataAccess>(); }
        assignObjs.add(val); 
      }
    }

    
    /**From Zbnf: < variable?defVariable> 
     */
    public DataAccess.DataAccessSet new_defVariable(){ return new DataAccess.DataAccessSet(); }
    
    public void add_defVariable(DataAccess.DataAccessSet val){  //// 
      int whichStatement = "SPULJW".indexOf(elementType);
      char whichVariableType = "SPULOA".charAt(whichStatement);
      val.setTypeToLastElement(whichVariableType);
      //don't use the dataPath, it may be the path to the initial data.
      variable = val;
      //if(assignObjs == null){ assignObjs = new LinkedList<DataAccess>(); }
      //assignObjs.add(val); 
    }

     
    public Statement new_assignExpr(){ 
      return new Statement(parentList, '=', null); 
    } 

    public void add_assignExpr(Statement val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);  
      statementlist.onerrorAccu = null; 
      statementlist.withoutOnerror.add(val);
    }
    
    
    
    
    public Statement new_statementBlock(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'B', null);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_statementBlock(Statement val){}

    
    public void set_append(){
      if(elementType == '='){ elementType = '+'; }
      else throw new IllegalArgumentException("ZGenScript - unexpected set_append");
    }
    
    
    public Onerror new_onerror(){
      return new Onerror(parentList);
    }
    

    public void add_onerror(Onerror val){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);
      if(statementlist.onerrorAccu == null){ statementlist.onerrorAccu = new LinkedList<Onerror>(); }
      for( Statement previousStatement: statementlist.withoutOnerror){
        previousStatement.onerror = onerror;  
        //use the same onerror list for all previous statements without error designation.
      }
      statementlist.withoutOnerror.clear();  //remove all entries, they are processed.
    }

    
    public void set_breakBlock(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'b', null);
      statementlist.statements.add(statement);
    }
    
 
      
    public Statement new_forContainer()
    { if(statementlist == null) { statementlist = new StatementList(this); }
      return statementlist.new_forContainer();
    }
    
    public void add_forContainer(Statement val){statementlist.add_forContainer(val);}

    
    public Statement new_whileBlock()
    { if(statementlist == null) { statementlist = new StatementList(this); }
      return statementlist.new_whileBlock();
    }
    
    public void add_whileBlock(Statement val){statementlist.add_whileBlock(val); }

    
    public Statement new_if()
    { StatementList subGenContent = new StatementList(this);
      Statement statement = new Statement(parentList, 'F', null);
      statement.statementlist = subGenContent;  //The statement contains a genContent. 
      return statement;
    }
    
    public void add_if(Statement val){
      if(statementlist == null) { statementlist = new StatementList(this); }
      statementlist.statements.add(val);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(val);
      
    }

    
    public IfCondition new_ifBlock()
    { StatementList subGenContent = new StatementList(this);
      IfCondition statement = new IfCondition(parentList, 'G');
      statement.statementlist = subGenContent;  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_ifBlock(IfCondition val){}

    public Statement new_hasNext()
    { Statement statement = new Statement(parentList, 'N', null);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_hasNext(Statement val){}

    public Statement new_elseBlock()
    { StatementList subGenContent = new StatementList(this);
      Statement statement = new Statement(parentList, 'E', null);
      statement.statementlist = subGenContent;  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_elseBlock(Statement val){}

    
    
    /**From Zbnf: [{ Thread <datapath?defThreadVar> = }] 
     */
    public DataAccess.DataAccessSet new_defThreadVar(){ 
      return new DataAccess.DataAccessSet(); 
    }
    
    public void add_defThreadVar(DataAccess.DataAccessSet val){ 
      val.setTypeToLastElement('T');
      dataAccess = val;
      identArgJbat = "N";  //Marker for a new Variable.
    }

    
    /**From Zbnf: [{ Thread <datapath?assignThreadVar> = }] 
     */
    public DataAccess.DataAccessSet new_assignThreadVar(){ 
      return new DataAccess.DataAccessSet(); 
    }
    
    public void add_assignThreadVar(DataAccess.DataAccessSet val){ 
      dataAccess = val;
    }

    

    
    ////
    public Statement new_threadBlock()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_threadBlock();
    }
    
    public void add_threadBlock(Statement val){statementlist.add_threadBlock(val);}

    
    public Statement new_move()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_move();
    }
    
    public void add_move(Statement val){statementlist.add_move(val);}

    
    public Statement new_copy()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_copy();
    }
    
    public void add_copy(Statement val){statementlist.add_copy(val);}

    
    public CallStatement new_call()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      CallStatement statement = new CallStatement(parentList);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_call(CallStatement val){}

    

    public Statement new_cmdLine()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'c', null);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_cmdLine(Statement val){}

    

    public void set_cd(String val)
    { if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.set_cd(val);
    }
    
    

    public Statement new_cd()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      return statementlist.new_cd();
    }
    
    
    public void add_cd(Statement val)
    { statementlist.add_cd(val);
    }
    
    

    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void set_fnEmpty(String val){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'f', StringSeq.create(val));
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
    }
    
    public void set_outputValue(String text){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'o', StringSeq.create(text));
      statementlist.statements.add(statement); 
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
    }
    
    public void set_inputValue(String text){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'i', StringSeq.create(text));
      statementlist.statements.add(statement); 
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
    }
    
    //public void set_variableValue(String text){ subContent.content.add(new ScriptElement('v', text)); }
    
    public Statement new_forInputContent()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'I', null);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forInputContent(Statement val){}

    
    public Statement xxxnew_forVariable()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'V', null);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void xxxadd_forVariable(Statement val){} //empty, it is added in new_forList()

    
    public Statement new_forList()
    { if(statementlist == null){ statementlist = new StatementList(this); }
      Statement statement = new Statement(parentList, 'L', null);
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forList(Statement val){} //empty, it is added in new_forList()

    
    public Statement new_addToList(){ 
      Statement subGenContent = new Statement(parentList, 'l', null);
      statementlist.addToList.add(subGenContent.statementlist);
      return subGenContent;
    }
   
    
    public void add_addToList(Statement val)
    {
    }

    
    public void set_exitScript(int val){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.set_exitScript(val);
    }
    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void axxxdd_fnEmpty(Statement val){  }
    

    
    @Override public String toString()
    {
      switch(elementType){
      case 't': return textArg.toString();
      case 'S': return "String " + identArgJbat;
      case 'J': return "Obj " + identArgJbat;
      case 'P': return "Pipe " + identArgJbat;
      case 'U': return "Buffer " + identArgJbat;
      case 'o': return "(?outp." + textArg + "?)";
      case 'i': return "(?inp." + textArg + "?)";
      case 'e': return "<*" +   ">";  //expressions.get(0).datapath
      //case 'g': return "<$" + path + ">";
      case 's': return "call " + identArgJbat;
      case 'B': return "{ statementblock }";
      case '?': return "onerror";
      case 'I': return "(?forInput?)...(/?)";
      case 'L': return "(?forList " + textArg + "?)";
      case 'C': return "<:for:Container " + textArg + "?)";
      case 'F': return "if";
      case 'G': return "elsif";
      case 'N': return "<:hasNext> content <.hasNext>";
      case 'E': return "else";
      case 'Z': return "<:target:" + identArgJbat + ">";
      case 'Y': return "<:file>";
      case 'b': return "break;";
      case 'c': return "cmd;";
      case 'm': return "move;";
      case 'x': return "thread";
      case 'y': return "copy";
      case 'z': return "exit";
      case '=': return "assignExpr";
      case '+': return "appendExpr";
      default: return "(??" + elementType + " " + textArg + "?)";
      }
    }
    
    
  }

  
  
  public static class CmdLine extends Statement
  {
    
    CmdLine(StatementList parentList){
      super(parentList, 'c', null);
    }
    
  };
  
  
  
  public static class CallStatement extends Statement
  {
    
    public Argument callName;
    
    CallStatement(StatementList parentList){
      super(parentList, 's', null);
    }
    
    public Argument new_callName(){ return callName = new Argument(parentList); }
    
    public void set_callName(Argument val){}
    
  };
  
  
  
  public static class ExitStatement extends Statement
  {
    
    int exitValue;
    
    ExitStatement(StatementList parentList, int exitValue){
      super(parentList, 'z', null);
      this.exitValue = exitValue;
    }
  };
  
  
  
  public static class IfCondition extends Statement
  {
    
    public Statement XXXcondition;
    
    public boolean bElse;
    
    public CalculatorExpr XXXexpr;
    
    IfCondition(StatementList parentList, char whatis){
      super(parentList, whatis, null);
    }
    
    public Statement XXXnew_cmpOperation()
    { XXXcondition = new Statement(parentList, '?', null);
      return XXXcondition;
    }
    
    public void add_cmpOperation(Statement val){
      Assert.stop();
      /*
      String text;
      if(val.expression !=null && val.expression.values !=null && val.expression.values.size()==1
        && (text = val.expression.values.get(0).stringValue()) !=null && text.equals("else") ){
        bElse = true;
      }
      */        
    }


    

  }
  
  
  
  
  
  
  
  /**This class contains expressions for error handling.
   * The statements in this sub-ScriptElement were executed if an exception throws
   * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
   * If it is null, no exception handling is done.
   * <br><br>
   * This block can contain any statements as error replacement. If they fails too,
   * the iferror-Block can contain an iferror too.
   * 
 */
  public final static class Onerror extends Statement
  {
    /**From ZBNF */
    public int errorLevel;
    
    
    /**
     * <ul>
     * <li>'n' for notfound
     * <li>'f' file error
     * <li>'i' any internal exception.
     * </ul>
     * 
     */
    public char errorType = '?';
    
    public void set_errortype(String type){
      errorType = type.charAt(0); //n, i, f
    }
 
    Onerror(StatementList parentList){
      super(parentList, '?', null);
    }
 }
  
  
  
  
  
  /**Organization class for a list of script elements inside another Scriptelement.
   *
   */
  public static class StatementList
  {
    
    /**Hint to the source of this parsed argument or statement. */
    String srcFile = "srcFile-yet-unknown";
    
    final Argument parentStatement;
    
    /**True if < genContent> is called for any input, (?:forInput?) */
    //public final boolean XXXisContentForInput;
    
    public String cmpnName;
    
    public final List<Statement> statements = new ArrayList<Statement>();
    

    /**List of currently onerror statements.
     * This list is referenced in the appropriate {@link Statement#onerror} too. 
     * If an onerror statement will be gotten next, it is added to this list using this reference.
     * If another statement will be gotten next, this reference is cleared. So a new list will be created
     * for a later getting onerror statement. 
     */
    List<Onerror> onerrorAccu;

    
    List<Statement> withoutOnerror = new LinkedList<Statement>();
    
    
    /**True if the block {@link Argument#statementlist} contains at least one variable definition.
     * In this case the execution of the ScriptElement as a block should be done with an separated set
     * of variables because new variables should not merge between existing of the outer block.
     */
    public boolean bContainsVariableDef;

    
    /**Scripts for some local variable. This scripts where executed with current data on start of processing this genContent.
     * The generator stores the results in a Map<String, String> localVariable. 
     * 
     */
    //private final List<ScriptElement> localVariableScripts = new ArrayList<ScriptElement>();
    
    public final List<StatementList> addToList = new ArrayList<StatementList>();
    
    //public List<String> datapath = new ArrayList<String>();
    
    public StatementList()
    { this.parentStatement = null;
      //this.isContentForInput = false;
    }
        
    public StatementList(Argument parentStatement)
    { this.parentStatement = parentStatement;
      //this.isContentForInput = false;
    }
        
    /*
    public StatementList(Argument parentStatement, boolean isContentForInput)
    { this.parentStatement = parentStatement;
      //this.isContentForInput = isContentForInput;
    }
    */
        
    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public Statement new_setEnvVar(){ 
      return new Statement(this, 'S', null); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_setEnvVar(Statement val){ 
      //change the first identifier to $name
      val.variable.datapath().get(0).setIdent("$" + val.variable.datapath().get(0).ident());
      //val.identArgJbat = "$" + val.identArgJbat;
      statements.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    

    //public List<ScriptElement> getLocalVariables(){ return localVariableScripts; }
    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    public Statement new_dataText(){ return new Statement(this, 'e', null); }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    public void add_dataText(Statement val){ 
      statements.add(val);
      onerrorAccu = null; withoutOnerror.add(val);
    }
    
    /**Sets the textReplLf From ZBNF. 
     * Inside a <code>textExpr::=...<*|\<:|\<+|\<=|\<*|\<\.?textReplLf></code>.
     * The text is written in the source file, but the line feed character sequence may be another
     * int the generated text. Additional a left indent can be removed.
     * @param text
     */
    public void set_textReplLf(String text){
      CharSequence cText;
      if(text.contains("\n=")){
        StringBuilder u = new StringBuilder(text);
        cText = u;
        int pos = 0;
        while( (pos = u.indexOf("\n=",pos))>=0){
          u.replace(pos+1, pos+2, "");
        }
      } else {
        cText = text;
      }
      Statement statement = new Statement(this, 't', StringSeq.create(cText));
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    

    
    /**Sets the nonEmptyText From ZBNF. invokes {@link #set_textReplLf(String)} if the text contains
     * other characters as white spaces. 
     */
    public void set_nonEmptyText(String text){
      if(!StringFunctions.isEmptyOrOnlyWhitespaces(text)){
        set_textReplLf(text);
      }
    }
    
    
    public void set_newline(){
      Statement statement = new Statement(this, 'n', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    

    public Statement new_forContainer()
    { Statement statement = new Statement(this, 'C', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forContainer(Statement val){}


    public Statement new_whileBlock()
    { Statement statement = new Statement(this, 'w', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_whileBlock(Statement val){}


    public Statement new_threadBlock()
    { Statement statement = new Statement(this, 'x', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_threadBlock(Statement val){}


    public Statement new_move()
    { Statement statement = new Statement(this, 'm', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_move(Statement val){}


    public Statement new_copy()
    { Statement statement = new Statement(this, 'y', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_copy(Statement val){}


    public void set_cd(String val)
    { Statement statement = new Statement(this, 'd', null);
      statement.textArg = StringSeq.create(val);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    
    public Statement new_cd(){
      Statement statement = new Statement(this, 'd', null);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    
    public void add_cd(Statement val){}
    
    
    
    public void set_name(String name){
      cmpnName = name;
    }
    
    
    public void set_exitScript(int val){
      Statement statement = new ExitStatement(this, val);
      statements.add(statement);
    }  
    
    
    public void XXXadd_datapath(String val)
    {
      //datapath.add(val);
    }

    
    @Override public String toString()
    { return "genContent name=" + cmpnName + ":" + statements;
    }
  }
  

  
  
  /**A class in the ZGen syntax.
   * The class can contain statements, which are variable definitions of the class variable. 
   * Therefore this class extends the StatementList.
   */
  public class ZGenClass extends StatementList
  {
    
    List<ZGenClass> classes;
    
    final Map<String, Statement> subScripts = new TreeMap<String, Statement>();
    
    protected ZGenClass(){}
    
    
    public final List<ZGenClass> classes(){ return classes; }
    
    public final Map<String, Statement> subScripts(){ return subScripts; }
    
    public ZGenClass new_subClass(){ return new ZGenClass(); }
    
    public void add_subClass(ZGenClass val){ 
      if(classes == null){ classes = new ArrayList<ZGenClass>(); }
      classes.add(val); 
    }
    
    public Statement new_subScript(){ return new Statement(this, 'X', null); }
    
    public void add_subScript(Statement val){ 
      if(val.identArgJbat == null){
        val.identArgJbat = "main";
      }
      
      subScripts.put(val.identArgJbat, val); 
      String nameGlobal = cmpnName == null ? val.identArgJbat : cmpnName + "." + val.identArgJbat;
      subScriptsAll.put(nameGlobal, val); 
    }
    
    /**Defines a variable with initial value. <= <variableDef?textVariable> \<\.=\>
     */
    public Statement new_textVariable(){ return new Statement(this, 'S', null); }

    public void add_textVariable(Statement val){ listScriptVariables.add(val); } 
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public Statement new_objVariable(){ return new Statement(this, 'J', null); } ///

    public void add_objVariable(Statement val){ listScriptVariables.add(val); } 
    
    
    
  }
  
  
  
  
  
  
  /**Main class for ZBNF parse result.
   * This class has the enclosing class to store {@link ZbatchGenScript#subScriptsAll}, {@link ZbatchGenScript#listScriptVariables}
   * etc. while parsing the script. The <code><:file>...<.file></code>-script is stored here locally
   * and used as the main file script only if it is the first one of main or included script. The same behaviour is used  
   * <pre>
   * ZmakeGenctrl::= { <target> } \e.
   * </pre>
   */
  public final static class ZbnfMainGenCtrl extends ZGenClass
  {

    protected final ZGenScript outer;
    
    //public String scriptclass;
    
    public List<String> includes;
    
    /**The script element for the whole file of this script. 
     * It is possible that it is from a included script.
     * It shall contain calling of <code><*subtext:name:...></code> 
     */
    Statement mainScript;
    
    
    
    public ZbnfMainGenCtrl(ZGenScript outer){
      outer.super();   //ZGenClass is non-static, enclosing is outer.
      this.outer = outer;
      outer.scriptClass = outer.new ZGenClass();
    }
    
    /**Returns the main routine which may be parsed in this maybe included script. */
    public Statement getMainRoutine(){ return mainScript; }
    
    public void set_include(String val){ 
      if(includes ==null){ includes = new ArrayList<String>(); }
      includes.add(val); 
    }
    
    //public Statement new_ZmakeTarget(){ return new Statement(null, 'Z', null); }
    
    //public void add_ZmakeTarget(Statement val){ zmakeTargets.put(val.name, val); }
    
    
    public Statement new_mainScript(){ return mainScript = new Statement(outer.scriptClass, 'Y', null); }
    
    public void add_mainScript(Statement val){  }
    
    

  }
  


}
