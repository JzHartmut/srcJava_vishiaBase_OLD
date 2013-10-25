package org.vishia.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.vishia.cmd.ZGenExecuter;
import org.vishia.cmd.ZGenScript;


/**This class provides a calculator for expressions. The expression are given 
 * in the reverse polish notation. It can be converted either from a simple string format
 * or from a {@link org.vishia.zbnf.ZbnfJavaOutput} output from a Zbnf parsing process 
 * and then converted to the internal format for a stack oriented running model. 
 * <br><br>
 * This class contains the expression itself and the capability to calculate in a single thread.
 * The calculation can be execute with one or some given values (usual float, integer type)
 * or it can access any Java internal variables using the capability of {@link DataAccess}.
 * <br><br>
 * <ul>
 * <li>Use {@link #setExpr(String)} to convert a String given expression to the internal format.
 * <li>Use {@link #calc(float)} for simple operations with one float input, especially for scaling values. It is fast.
 * <li>Use {@link #calc(Object...)} for universal expression calculation. 
 * </ul>
 * If the expression works with simple variable or constant values, it is fast. For example it is able to use
 * in a higher frequently graphic application to scale values.
 * <br><br>
 * TODO see DataAccess: A value or subroutine can use any java elements which are accessed via reflection mechanism.
 * Write "X * $classpath.Myclass.method(X)" to build an expression which's result depends on the called
 * static method.
 * <br><br>
 * To test the capability and the correctness use {@link org.vishia.util.test.TestCalculatorExpr}. 
 * @author Hartmut Schorrig
 *
 */
public class CalculatorExpr
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-10-19 Hartmut new: {@link SetExpr} should know all possibilities of {@link DataAccess.DataAccessSet}
   *   too because an expression may be an DataAccess only. Yet only {@link SetExpr#new_newJavaClass()} realized.
   * <li>2013-10-19 Hartmut new: The CalculatorExpr gets the capability to generate String expressions
   *   using the {@link ZGenExecuter} class. This is because some arguments of methods may be a String.
   *   If the {@link #genString} is set, the CalculatorExpr is a String expression.
   *   Now this class, the {@link DataAccess} and the {@link ZGenExecuter} are one cluster of functionality.
   * <li>2013-09-02 Hartmut new: {@link CalculatorExpr.SetExpr} to set from a ZbnfParseResult using {@link org.vishia.zbnf.ZbnfJavaOutput}.
   *   This class can be invoked without ZbnfParser too, it is independent of it. But it isn't practicable. 
   * <li>2013-09-02 Hartmut new: CalculatorExpr: now supports unary ( expression in parenthesis ). 
   * <li>2013-08-18 Hartmut new: {@link Operation#unaryOperator}
   * <li>2013-08-19 Hartmut chg: The {@link DataAccess.DatapathElement} is a attribute of a {@link Operation}, not of a {@link Value}.
   *   A value is only a container for constant values or results.
   * <li>2012-12-22 Hartmut new: Now a value can contain a list of {@link DataAccess.DatapathElement} to access inside java data 
   *   to evaluate the value. The concept is synchronized with {@link org.vishia.zbatch.ZbatchGenScript}, 
   *   but not depending on it. The JbatGenScript uses this class, this class participates on the development
   *   and requirements of jbat.
   * <li>Bugfix because String value and thrown Exception. The class needs a test environment. TODO
   * <li>2012-12-22 some enhancements while using in {@link org.vishia.ZbatchExecuter.TextGenerator}.
   * <li>2012-04-17 new {@link #calc(float)} for float and int
   * <li>TODO unary operator, function 
   * <li>2011-10-15 Hartmut creation. The ideas were created in 1979..80 by me.  
   * </ul>
   *
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
  public final static int version = 20121222;
  
   
   
   
  
  /**A path to any Java Object or method given with identifier names.
   * The access is organized using reflection.
   * <ul>
   * <li>This class can describe a left value. It may be a Container to which a value is added
   * or a {@link java.lang.Appendable}, to which a String is added.  
   * <li>This class can describe a value, which is the result of access to the last element of the path.
   * </ul>
   */
  public static class XXXDatapath
  {
    /**The description of the path to any data if the script-element refers data. It is null if the script element
     * does not refer data. If it is filled, the instances are of type {@link DataAccess.DatapathElementSet}.
     * If it is used in {@link DataAccess}, its base class {@link DataAccess.DatapathElement} are used. The difference
     * are the handling of actual values for method calls. See {@link DataAccess.DatapathElementSet#actualArguments}.
     */
    protected List<DataAccess.DatapathElement> datapath;
    
    public List<DataAccess.DatapathElement> datapath(){ return datapath; }
    
    public void add_datapathElement(DataAccess.DatapathElement item){ 
      if(datapath == null){
        datapath = new ArrayList<DataAccess.DatapathElement>();
      }
      datapath.add(item); 
    }

    
    
  }
   
   
  /**A value, maybe a constant, any given Object or an access description to a java program element.
   * 
   *
   */
  public static class Value { //extends Datapath{
    
    
    
    /**Type of the value. 
     * <ul>
     * <li>J I D F Z: long, int, double, boolean, the known Java characters for types see {@link java.lang.Class#getName()}
     * <li>o: The oVal contains any object.
     * <li>t: A character sequence stored in stringVal,
     * <li>d: Access via the data path using reflection
     * </ul>
     */
    protected char type = '?';
    protected long longVal;
    protected int intVal;
    protected double doubleVal;
    protected float floatVal;
    protected boolean boolVal;
    protected CharSequence stringVal;
    protected Object oVal;
    
    public Value(long val){ type = 'J'; longVal = val; }
    
    public Value(int val){ type = 'I'; intVal = val; }
    
    public Value(double val){ type = 'D'; doubleVal = val; }
    
    public Value(float val){ type = 'F'; floatVal = val; }
    
    public Value(boolean val){ type = 'Z'; boolVal = val; }
    
    public Value(char val){ type = 'C'; longVal = val; }
    
    public Value(String val){ type = 't'; stringVal = val; }
    
    public Value(Appendable val){ type = 'a'; oVal = val; }
    
    public Value(Object val){ type = 'o'; oVal = val; }
    
    //public Value(List<DataPathItem> datpath){ type = 'd'; this.datapath = datapath; }
    
    public Value(){ type = '?'; }
    
    /**Returns a boolean value. If the type of content is a numeric, false is returned if the value is ==0.
     * If the type is a text, false is returned if the string is empty.
     * If the type is any other Object, false is returned if the object referenz is ==null.
     * @return The boolean value.
     */
    public boolean booleanValue()
    { switch(type){
        case 'I': return intVal !=0;
        case 'J': return longVal !=0;
        case 'D': return doubleVal !=0;
        case 'F': return floatVal !=0;
        case 'C': return intVal !=0;
        case 'Z': return boolVal;
        case 't': return stringVal !=null && stringVal.length() >0;
        case 'o': return oVal !=null;
        case '?': throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public double doubleValue()
    { switch(type){
        case 'I': return intVal;
        case 'C': return intVal;
        case 'J': return longVal;
        case 'D': return doubleVal;
        case 'F': return floatVal;
        case 'Z': return boolVal ? 1.0 : 0;
        case 't': return Double.parseDouble(stringVal.toString());
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': return 7777777.0; //TODO throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    /**Returns the reference to the StringBuilder-buffer if the result is a concatenation of strings.
     * The StringBuilder-buffer can be changed after them in any public application, 
     * because the Value is only returned on end of calculation. 
     * Returns a reference to String in all other cases.
     * 
     * @return
     */
    public CharSequence stringValue(){ 
      switch(type){
        case 'I': return StringSeq.create(Integer.toString(intVal));
        case 'J': return StringSeq.create(Long.toString(longVal));
        case 'D': return StringSeq.create(Double.toString(doubleVal));
        case 'F': return StringSeq.create(Float.toString(floatVal));
        case 'C': return (new StringBuilder(1)).append((char)intVal);
        case 'Z': return StringSeq.create(Boolean.toString(boolVal));
        case 't': return stringVal;
        case 'o': return StringSeq.create(oVal ==null ? "null" : oVal.toString());
        case '?': return StringSeq.create("??");
        default:  return StringSeq.create("?" + type);
      }//switch
    }

    /**Converts the Value to the adequate Object representation, especially for the wrapped
     * primitive types. If the value contains an int, char, long, double, float, boolean,
     * it is converted to the wrapped presentation of that. If the value contains an text, represented 
     * by a reference to {@link java.lang.CharSequence}, this instance is returned. It may be a 
     * {@link java.lang.String} or a {@link java.lang.StringBuilder}.
     * If the value contains a reference to an object because the expression consists of only one
     * argument, a {@link Operation#datapath} of an {@link DataAccess}, this Object is returned.
     * 
     * @return an Object which presents the value.
     */
    public Object objValue(){ 
      switch(type){
        case 'I': return new Integer(intVal);
        case 'C': return new Character((char)intVal);
        case 'J': return new Long(longVal);
        case 'D': return new Double(doubleVal);
        case 'F': return new Float(doubleVal);
        case 'Z': return new Boolean(boolVal);
        case 't': return stringVal;
        case 'o': return oVal;
        default:  return "?" + type;
      }//switch
    }


    
    /**Returns true if the value stores an Object value.
     * Especially if the expression does contain only from a single dataAccess, the value
     * is an Object value.
     * @return false on stringValue or such. Tests {@link #type} == 'o'.
     */
    public boolean isObjValue(){ return type == 'o'; }
    
    
    @Override public String toString(){ 
      switch(type){
        case 'I': return Integer.toString(intVal);
        case 'J': return Long.toString(longVal);
        case 'D': return Double.toString(doubleVal);
        case 'F': return Float.toString(floatVal);
        case 'C': return "" + (char)intVal;
        case 'Z': return Boolean.toString(boolVal);
        case 't': return stringVal.toString();
        case 'o': return oVal ==null ? "null" : oVal.toString();
        case '?': return "??";
        default:  return "?" + type;
      }//switch
    }
  }
  
  
  
  /**Common interface for the type of expression.
   */
  private interface ExpressionType{
    abstract char typeChar();
    
    /**Checks the second argument whether it is matching to the current expression type 
     * which matches to the accu.
     * If the val2 provides another type, either it is converted to the current expression type
     * or another (higher) expression type is taken and the accumulator value is converted.
     * @param accu The accumulator maybe changed..
     * @param val2 the second operand is tested, may be changed.
     * @return type of the expression.
     */
    abstract ExpressionType checkArgument(Value accu, Value val2);
  }
  
  
  
  /**The type of val2 determines the expression type. The accu is not used because it is a set operation. 
   * The accu will be set with the following operation.
   */
  protected static final ExpressionType startExpr = new ExpressionType(){
    
    @Override public char typeChar() { return '!'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      accu.type = val2.type;
      switch(val2.type){
        case 'I': return intExpr; 
        case 'J': return longExpr; 
        case 'F': return floatExpr; 
        case 'D': return doubleExpr; 
        case 'C': return intExpr; 
        case 'Z': return booleanExpr; 
        case 't': return stringExpr; 
        case 'o': return objExpr;   //first operand is any object type. 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }

    @Override public String toString(){ return "Type=!"; }

  };
  
  private static final ExpressionType intExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'I'; }
    
    /**The current expression type is int. The accumulator is of type int. 
     * Change the expression type and convert the operands
     * if one of the operand have an abbreviating type.
     * @see org.vishia.util.CalculatorExpr.ExpressionType#checkArgument(org.vishia.util.CalculatorExpr.Value, org.vishia.util.CalculatorExpr.Value, java.lang.Object)
     */
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'C': case 'I': return this; 
        case 'J': accu.longVal = accu.intVal; accu.type = 'J'; return longExpr; 
        case 'F': accu.floatVal = accu.intVal; accu.type = 'F'; return floatExpr; 
        case 'D': accu.doubleVal = accu.intVal; accu.type = 'D'; return doubleExpr; 
        case 'Z': accu.boolVal = accu.intVal !=0; accu.type = 'Z'; return booleanExpr; 
        case 't': {
          try{ val2.longVal = Long.parseLong(val2.stringVal.toString());
            val2.type = 'J'; 
            return this; 
          } catch(Exception exc){ throw new IllegalArgumentException("CalculatorExpr - String converion error"); }
        }
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }

    @Override public String toString(){ return "Type=I"; }


  };

  private static final ExpressionType longExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'J'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'C': case 'I': val2.longVal = val2.intVal; val2.type = 'J'; return this; 
        case 'J': return this; 
        case 'F': accu.floatVal = accu.longVal; accu.type = 'F'; return floatExpr; 
        case 'D': accu.doubleVal = accu.longVal; accu.type = 'D'; return doubleExpr; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=J"; }


  };
  
  
  private static final ExpressionType floatExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'F'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'C': case 'I': val2.floatVal = val2.intVal; val2.type = 'F'; return this; 
        case 'J': val2.doubleVal = val2.longVal; val2.type = 'D'; return doubleExpr; 
        case 'F': return this; 
        case 'D': accu.doubleVal = accu.floatVal; accu.type = 'D'; return doubleExpr; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=F"; }


  };
  
  
  private static final ExpressionType doubleExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'D'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'C': case 'I': val2.doubleVal = val2.intVal; val2.type = 'D'; return this; 
        case 'J': val2.doubleVal = val2.longVal; val2.type = 'D'; return this; 
        case 'F': val2.doubleVal = val2.floatVal; val2.type = 'D'; return this; 
        case 'D': return this; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
    }
    @Override public String toString(){ return "Type=D"; }


  };
  
  
  
  protected static final ExpressionType booleanExpr = new ExpressionType(){

    @Override public char typeChar() { return 'Z'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      switch(val2.type){
        case 'C': 
        case 'I': val2.boolVal = val2.intVal !=0; val2.type = 'Z'; break;
        case 'J': val2.boolVal = val2.longVal != 0; val2.type = 'Z'; break;
        case 'F': val2.boolVal = val2.floatVal !=0; val2.type = 'Z'; break;
        case 'D': val2.boolVal = val2.floatVal !=0; val2.type = 'Z'; break;
        case 't': val2.boolVal = val2.stringVal !=null && val2.stringVal.length() >0; val2.type = 'Z'; break;
        case 'o': val2.boolVal = val2.oVal !=null; val2.type = 'Z'; break;
        case 'Z': break; 
        default: throw new IllegalArgumentException("src type");
      } //switch  
      return this;
    }
    @Override public String toString(){ return "Type=Z"; }
  };
  
  
  private static final ExpressionType stringExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 't'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      if(accu.type !='t'){
        //especially any object.
        accu.stringVal = accu.stringValue();
        accu.type = 't';
      }
      if(val2.type !='t'){ 
        val2.stringVal = val2.stringValue();
        val2.type = 't';
      }
      return this;
    }

    @Override public String toString(){ return "Type=t"; }


  };
  

  /**Only used for the first and only one operand.
   * 
   */
  private static final ExpressionType objExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'o'; }
    
    /**Converts both operands to strings and returns stringExpression
     * @see org.vishia.util.CalculatorExpr.ExpressionType#checkArgument(org.vishia.util.CalculatorExpr.Value, org.vishia.util.CalculatorExpr.Value)
     */
    @Override public ExpressionType checkArgument(Value accu, Value val2) {
      if(accu.type !='t'){
        //especially any object.
        accu.stringVal = accu.stringValue();
        accu.type = 't';
      }
      if(val2.type !='t'){ 
        val2.stringVal = val2.stringValue();
        val2.type = 't';
      }
      return stringExpr;
    }

    @Override public String toString(){ return "Type=t"; }


  };
  


  
  
  
  
  /**An operator for the current value (accu), the given second value maybe the operand from the {@link Operation},
   * or for some additional values in stack.
   * The operation are such as a set operation (set the arg to accu), add, subtract, negate, boolean operators etc or standard maths routines.
   * Instances of this class are defined in the {@link CalculatorExpr}.
   */
  public abstract static class Operator{
    private final String name; 
    protected Operator(String name){ this.name = name; }
    
    protected abstract ExpressionType operate(ExpressionType Type, Value accu, Value arg);
    
    protected abstract boolean isUnary();
    @Override public String toString(){ return name; }
  }
  
                                                  
  
  /**This class contains all Operators as static references.
   * TODO Idea-typedOperator: this class may be an interface instead, which is implemented for the known 
   * {@link ExpressionType}. Then the switch-case can be saved in calculation time.
   */
  protected static class Operators
  {
  
  static final Operator setOperation = new Operator("!"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.type = type.typeChar();
      switch(accu.type){
        case 'B': case 'S': 
        case 'C': case 'I': accu.intVal = arg.intVal; break;
        case 'J': accu.longVal = arg.longVal; break;
        case 'F': accu.floatVal = arg.floatVal; break;
        case 'D': accu.doubleVal = arg.doubleVal; break;
        case 'Z': accu.boolVal = arg.boolVal; break;
        case 't': accu.stringVal = arg.stringVal; break;
        case 'o': accu.oVal = arg.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   

  
  
  static final Operator boolOperation = new Operator("bool "){
    @Override public ExpressionType operate(ExpressionType type, Value val, Value val2) {
      val.boolVal = val.booleanValue();
      val.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  static final Operator boolNotOperation = new Operator("b!"){
    @Override public ExpressionType operate(ExpressionType type, Value val, Value value2) {
      val.boolVal = !val.booleanValue();
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  static final Operator bitNotOperation = new Operator("~u"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value value2) {
      switch(type.typeChar()){
        case 'B': case 'S': 
        case 'C': case 'I': accu.intVal = ~accu.intVal; break;
        case 'J': accu.longVal = ~accu.longVal; break;
        //case 'D': accu.doubleVal = accu.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  static final Operator negOperation = new Operator("-u"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value val2) {
      switch(type.typeChar()){
        case 'B': case 'S': 
        case 'C': case 'I': accu.intVal = -accu.intVal; break;
        case 'J': accu.longVal = -accu.longVal; break;
        case 'D': accu.doubleVal = -accu.doubleVal; break;
        case 'F': accu.floatVal = -accu.floatVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return true; }
  };
  
   
  static final Operator addOperation = new Operator("+"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal += arg.intVal; break;
        case 'J': accu.longVal += arg.longVal; break;
        case 'D': accu.doubleVal += arg.doubleVal; break;
        case 'F': accu.floatVal += arg.floatVal; break;
        case 'Z': accu.doubleVal += arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator subOperation = new Operator("-"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal -= arg.intVal; break;
        case 'J': accu.longVal -= arg.longVal; break;
        case 'D': accu.doubleVal -= arg.doubleVal; break;
        case 'F': accu.floatVal -= arg.floatVal; break;
        case 'Z': accu.doubleVal -= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator mulOperation = new Operator("*"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal *= arg.intVal; break;
        case 'J': accu.longVal *= arg.longVal; break;
        case 'D': accu.doubleVal *= arg.doubleVal; break;
        case 'F': accu.floatVal *= arg.floatVal; break;
        case 'Z': accu.doubleVal *= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator divOperation = new Operator("/"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.intVal /= arg.intVal; break;
        case 'J': accu.longVal /= arg.longVal; break;
        case 'D': accu.doubleVal /= arg.doubleVal; break;
        case 'F': accu.floatVal /= arg.floatVal; break;
        case 'Z': accu.doubleVal /= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator cmpEqOperation = new Operator(".cmp."){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal == arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal == arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) < (Math.abs(accu.doubleVal) / 100000); break;
        case 'F': accu.boolVal = Math.abs(accu.floatVal - arg.floatVal) < (Math.abs(accu.floatVal) / 100000.0f); break;
        case 'Z': accu.boolVal = accu.boolVal == arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.equals(accu.stringVal, arg.stringVal); break;
        case 'o': accu.boolVal = accu.oVal == null && arg.oVal == null || (accu.oVal !=null && arg.oVal !=null && accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator cmpNeOperation = new Operator("!="){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal != arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal != arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) >= (Math.abs(accu.doubleVal) / 100000); break;
        case 'F': accu.boolVal = Math.abs(accu.floatVal - arg.floatVal) >= (Math.abs(accu.floatVal) / 100000.0f); break;
        case 'Z': accu.boolVal = accu.boolVal != arg.boolVal; break;
        case 't': accu.boolVal = !StringFunctions.equals(accu.stringVal, arg.stringVal); break;
        case 'o': accu.boolVal = !(accu.oVal == null && arg.oVal == null) || (accu.oVal !=null && arg.oVal !=null && !accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator cmpLessThanOperation = new Operator("<"){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal < arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal < arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal < arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal < arg.floatVal; break;
        case 'Z': accu.boolVal = !accu.boolVal && arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) < 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) < 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator cmpGreaterEqualOperation = new Operator(">="){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal >= arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal >= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal >= arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal >= arg.floatVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) >= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) >= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator cmpGreaterThanOperation = new Operator(">"){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal > arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal > arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal > arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal > arg.floatVal; break;
        case 'Z': accu.boolVal = accu.boolVal && !arg.boolVal; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) > 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) > 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator cmpLessEqualOperation = new Operator("<="){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I': accu.boolVal = accu.intVal <= arg.intVal; break;
        case 'J': accu.boolVal = accu.longVal <= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal <= arg.doubleVal; break;
        case 'F': accu.boolVal = accu.floatVal <= arg.floatVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = StringFunctions.compare(accu.stringVal, arg.stringVal) <= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) <= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator boolOrOperation = new Operator("||"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() || arg.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
   
  static final Operator boolAndOperation = new Operator("&&"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() && arg.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
    @Override public boolean isUnary(){ return false; }
  };
  
  }

  
  /**An Operation in the list of operations. It contains the operator and maybe a operand.
   * <ul>
   * <li>The {@link Operation#operator} operates with the {@link CalculatorExpr#accu}, the {@link Operation#value}
   *   and maybe the {@link CalculatorExpr#stack}.
   * <li>An unary operator will be applied to the accu firstly. 
   * <li>The operand may be given as constant value, then it is stored with its type in {@link #value}.
   * <li>The operand may be given as index to the given input variables for expression calculation.
   *   Then {@link #ixVariable} is set >=0
   * <li>The operand may be referenced in top of stack, then {@link #ixVariable} = {@value #kStackOperand}
   * <li>The operand may be gotten from any java object, then {@link #datapath} is set.  
   * </ul>
   */
  public static class Operation
  {
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    protected static final int kArgumentUndefined = -4; 
     
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    public static final int kConstant = -1; 
     
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    public static final int kDatapath = -5; 
     
    /**Designation of {@link Operation#ixVariable} that the operand should be located in the top of stack. */
    public static final int kStackOperand = -2; 
     
    /**Designation of {@link Operation#ixVariable} that the operand is the accumulator for unary operation. */
    public static final int kUnaryOperation = -3; 
     
    /**The operation Symbol if it is a primitive. 
     * <ul>
     * <li>! Take the value
     * <li>+ - * / :arithmetic operation
     * <li>& | : bitwise operation
     * <li>A O X: boolean operation and, or, xor 
     * */
    @Deprecated private char operatorChar;
    
    /**The operator for this operation. 
     * TODO Idea-typedOperator: The Operation should know the type if its given operand
     * and it should know the type of the operations before. Then this operator can be selected
     * for that given type. If the type is unknown, the operator should be a type-indifferent one (which checks the type).*/
    protected Operator operator;
    
    /**Either it is null or the operation has a single unary operator for the argument. */
    Operator unaryOperator;
    
    /**Either it is null or the operation has some unary operators (more as one). */
    List<Operator> unaryOperators;
    
    /**Number of input variable from array or special designation.
     * See {@link #kStackOperand} */
    protected int ixVariable;
    
    /**A constant value. @deprecated, use value*/
    @Deprecated
    double value_d;
    
    /**A constant value. @deprecated, use value*/
    @Deprecated
    Object oValue;
    
    /**It is used for constant values which's type is stored there too. */
    protected Value value;
    
    /**Set if the value of the operation should be gotten by data access on calculation time. */
    protected DataAccess datapath;
    
    public Operation(){
      this.ixVariable = kArgumentUndefined;
    }
    
    public Operation(Operator op, int ixVariable){
      this.operator = op;
      this.operatorChar = op.name.charAt(0);  //not only for all, therefore deprecated.
      this.ixVariable = ixVariable; 
      if(op.isUnary()){
        assert(ixVariable == kUnaryOperation);
      }
    }
    
    public Operation(String op, int ixVariable){
      setOperator(op);
      this.ixVariable = ixVariable;
    }
    
    Operation(String operation, double value){ 
      this.value_d = value; this.value = new Value(value); 
      this.operator = getOperator(operation);
      this.operatorChar = this.operator.name.charAt(0);
      this.ixVariable = kConstant; 
      this.oValue = null; 
    }
    //Operation(char operation, int ixVariable){ this.value_d = 0; this.operation = operation; this.ixVariable = ixVariable; this.oValue = null; }
    //Operation(char operation, Object oValue){ this.value = 0; this.operation = operation; this.ixVariable = -1; this.oValue = oValue; }
    Operation(Operator operator, Object oValue){ 
      this.value_d = 0; this.operator = operator; this.operatorChar = operator.name.charAt(0); this.ixVariable = kConstant; this.oValue = oValue; 
    }
  
    
    
    public boolean hasOperator(){ return operator !=null; }
    
    public boolean hasUnaryOperator(){ return unaryOperator !=null || unaryOperators !=null; }
    
    public void add_datapathElement(DataAccess.DatapathElement item){ 
      if(datapath == null){ datapath = new DataAccess();}
      datapath.add_datapathElement(item); 
    }
    
    public void set_intValue(int val){
      if(value == null){ value = new Value(); }
      value.type = 'I';
      value.intVal = val;
    }
    
    
    public void set_charValue(char val){
      if(value == null){ value = new Value(); }
      value.type = 'C';
      value.intVal = val;
    }
    
    
    public void set_textValue(String val){
      if(value == null){ value = new Value(); }
      value.type = 't';
      value.stringVal = StringSeq.create(val);
    }

    /**Designates that the operand should be located in the top of stack. */
    public void setStackOperand(){ ixVariable = kStackOperand; }
    
    
    
    
    
    /**Sets one of some unary operators.
     * @param unary
     */
    public void addUnaryOperator(Operator unary){
      if(unaryOperators !=null){
        unaryOperators.add(unary);
      }
      else if(unaryOperator !=null){
        //it is the second one:
        unaryOperators = new ArrayList<Operator>();
        unaryOperators.add(unaryOperator);
        unaryOperators.add(unary);
        unaryOperator = null;  //it is added
      } else {
        unaryOperator = unary;
      }
      
    }
    
    public boolean addUnaryOperator(String op){
      Operator unary = operators.get(op);
      if(unary !=null) {
        addUnaryOperator(unary);
        return true;
      } else {
        return false;
      }
    }
    
    
    
    public void setOperator(Operator op){
      this.operator = op;
      this.operatorChar = op.name.charAt(0);
      if(op.isUnary()){
        ixVariable = kUnaryOperation;
      }
    }
    
    
    
    public boolean setOperator(String op){
      Operator op1 = operators.get(op);
      this.operatorChar = op1.name.charAt(0);
      if(op1 !=null){ 
        setOperator(op1);
      }
      return this.operator !=null; 
    }
    
    
    
    @Override public String toString(){ 
      StringBuilder u = new StringBuilder();
      u.append(operator);
      if(unaryOperator !=null){ u.append(" ").append(unaryOperator); }
      if(unaryOperators !=null){ u.append(" ").append(unaryOperators); }
      if(ixVariable >=0) u.append(" arg[").append(ixVariable).append("]");
      else if(ixVariable == kStackOperand) u.append(" stack");
      else if(datapath !=null) u.append(datapath.toString());
      else if (oValue !=null) u.append(" oValue:").append(oValue.toString());
      else if (value !=null) u.append(" ").append(value.toString());
      else u.append(" double:").append(value_d);
      return u.toString();
    }
  }
  
  
  /**This class provides a set interface to the CalculatorExpr. It contains only methods
   * to store set parse results for the expression, especially able to use for the ZBNF parser
   * using {@link org.vishia.zbnf.ZbnfJavaOutput}.
   * @author Hartmut Schorrig
   *
   */
  public static class SetExpr //extends CalculatorExpr
  {
    private CalculatorExpr.Operation actOperation; // = new Operation("!");
    
    private final List<CalculatorExpr.Operator> unaryOperators = new ArrayList<CalculatorExpr.Operator>(); // = new Operation("!");
    
    public final CalculatorExpr expr;
    
    protected final Object dbgParent;
    
    public SetExpr(boolean d, Object dbgParent){
      this.expr = new CalculatorExpr();
      this.dbgParent = dbgParent;
    }
    
    public SetExpr(boolean d){
      this.expr = new CalculatorExpr();
      this.dbgParent = null;
    }
    
    public SetExpr(CalculatorExpr expr){
      this.expr = expr;
      this.dbgParent = null;
    }
    
    
    public SetExpr(SetExpr parent){
      this.expr = parent.expr;
      this.dbgParent = parent;
    }
    
    
    /**Creates a new instance of this class for sub expressions (parenthesis, arguments).
     * This method should be overridden and should create the derived instance if a derived
     * expression algorithm is used.
     * 
     * @param parent may be null.
     * @return a new instance which refers the parent.
     */
    public SetExpr new_SetExpr(SetExpr parent){ return new SetExpr(parent); }
    
    
    
    /**From Zbnf, a part <:>...<.> */
    public ZGenScript.StatementList new_textExpr(){ 
      //JbatchExecuter.ZbatchExpression expr = (JbatchExecuter.ZbatchExpression)super.expr;
      return expr.genString = new ZGenScript.StatementList(); }
    
    /**From Zbnf, a part <:>...<.> */
    public void add_textExpr(ZGenScript.StatementList val){}
    

    
    
    public SetExpr new_boolOrOperation(){
      if(actOperation !=null){ addToOperations(); }
      return this;
    }
    
    
    /**Designates the end of a multiplication operation. Takes the operation into the expression list.
     * @param val this, unused
     */
    public void add_boolOrOperation(SetExpr val){
      if(actOperation ==null){
        actOperation = new CalculatorExpr.Operation();
        actOperation.setStackOperand();  
      }
      actOperation.setOperator("||");
      addToOperations(); 
    }
    
    
    
    public SetExpr XXXnew_boolStartOperation(){
      //if(actOperation !=null){ addToOperations(); }
      assert(actOperation == null);
      actOperation = new CalculatorExpr.Operation("!", -1);
      return this;
    }
    
    
    /**Designates the end of a multiplication operation. Takes the operation into the expression list.
     * @param val this, unused
     */
    public void XXXadd_boolStartOperation(SetExpr val){
      addToOperations(); 
    }
    
    
    public SetExpr new_boolAndOperation(){
      if(actOperation !=null){ addToOperations(); }
      return this;
    }
    
    
    /**Designates the end of a multiplication operation. Takes the operation into the expression list.
     * @param val this, unused
     */
    public void add_boolAndOperation(SetExpr val){
      if(actOperation ==null){
        actOperation = new CalculatorExpr.Operation();
        actOperation.setStackOperand();  
      }
      actOperation.setOperator("&&");
      addToOperations(); 
    }
    
    
    public void set_boolNot(String val){
      unaryOperators.add(CalculatorExpr.getOperator("u!"));
    }
    
    public SetExpr new_parenthesisCondition(){ 
      SetExpr subExpr = new_SetExpr(this);
      return subExpr;
    }
    
    public void add_parenthesisCondition(SetExpr val){
      if(val.actOperation !=null){
        val.addToOperations();  //if it is a start operation.
      }
      if(unaryOperators !=null){
        addUnaryToOperations();
      }
    }

    
    
    public SetExpr XXXnew_boolExpr(){ 
      return this;
    }

    
    public void XXXadd_boolExpr(SetExpr  val){ 
      if(actOperation !=null){
        if(actOperation.hasOperator()){
          actOperation.addUnaryOperator("b");
        }
        addToOperations();
      }
    }

    
    public SetExpr XXXnew_objExpr(){ 
      return this;
    }

    public void XXXadd_objExpr(SetExpr  val){ 
      if(actOperation ==null){
        actOperation = new CalculatorExpr.Operation();
        actOperation.setStackOperand();  
      }
      addToOperations(); 
    }

    public SetExpr new_cmpOperation(){
      if(actOperation !=null){
        addToOperations();  //if it is a start operation.
      }
      actOperation = new CalculatorExpr.Operation();
      return this;
    }
    
    public void add_cmpOperation(SetExpr val){
      addToOperations(); 
    }

    
    public void set_cmpOperator(String val){
      if(actOperation ==null){
        assert(false);
        actOperation = new CalculatorExpr.Operation();
      }
      actOperation.setOperator(val);
    }
    
    
    public SetExpr xxxnew_numExpression(){ 
      return this;
    }

    
    public void xxxadd_numExpression(SetExpr  val){ 
      
    }

    
    
    public void set_unaryOperator(String op){
      CalculatorExpr.Operator unaryOp = CalculatorExpr.getOperator("u" + op);
      assert(unaryOp !=null);  //should match to syntax prescript
      unaryOperators.add(unaryOp);
    }
    
    /**Designates, that a expression in parenthesis is given, which should be calculated firstly.
     * @return this
     */
    public SetExpr new_parenthesisExpr(){
      if(actOperation !=null){ //A start operation before
        addToOperations();
      }
      return this;
    }
    
    public void add_parenthesisExpr(SetExpr val){
      //assert(actOperation == null);  //it was added before on end of expression.   
      if(actOperation !=null){
        addToOperations();  //if it is a start operation.
      }
      //actOperation = new Operation();
      //actOperation.setStackOperand();  //NOTE the operation will be set by following set_multOperation() etc.
    }

    public SetExpr XXXnew_startOperation(){
      return this;
    }
    
    public void XXXadd_startOperation(SetExpr val){
      addToOperations(); 
    }

    /**Designates the start of a new adding operation. The first start value should be taken into the
     * stackOperation statement list as start operation.
     * @return this
     */
    public SetExpr new_addOperation(){
      if(actOperation !=null){ addToOperations(); }
      assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
      return this;  
    }
    
    /**Designates the end of an add operation. Takes the operation into the expression list.
     * @param val this, unused
     */
    public void add_addOperation(SetExpr val){
      if(actOperation ==null){
        actOperation = new CalculatorExpr.Operation();
        actOperation.setStackOperand();  
      }
      actOperation.setOperator("+");
      addToOperations(); 
    }

    public SetExpr new_multOperation(){
      if(actOperation !=null){ addToOperations(); }
      assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
      return this;
    }
    
    /**Designates the end of a multiplication operation. Takes the operation into the expression list.
     * @param val this, unused
     */
    public void add_multOperation(SetExpr val){
      if(actOperation ==null){
        actOperation = new CalculatorExpr.Operation();
        actOperation.setStackOperand();  
      }
      actOperation.setOperator("*");
      addToOperations(); 
    }


    /**A character is stored as integer. 
     * @param val
     */
    public void set_charValue(String val){
      if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
      actOperation.set_charValue(val.charAt(0));
    }
    
    
    /**Sets a value to the current operation. 
     * @param val
     */
    public void set_intValue(int val){
      if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
      actOperation.set_intValue(val);
    }
    
    
    
    public void set_text(String val){
      if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
      actOperation.set_textValue(val);
    }
    
    
    
    
    
    public void set_startVariable(String ident){
      if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = '@';
      element.ident = ident;
      actOperation.add_datapathElement(element);
    }
    
    public SetExpr new_datapath(){ return this; }
    
    public void add_datapath(SetExpr val){ 
      //actOperation.add_datapathElement(val); 
    }
    

    /**Creates a new {@link DataAccess.DatapathElementSet} instance for a {@link DataAccess.DatapathElement}
     * This method should be overridden if the arguments of a method should support derived features
     * of a derived instance of this class.
     * @return
     */
    public DataAccess.DatapathElementSet new_datapathElement(){ 
      return new DataAccess.DatapathElementSet(); 
    }
    
    public void add_datapathElement(DataAccess.DatapathElementSet val){ 
      if(actOperation == null){ actOperation = new CalculatorExpr.Operation(); }
      actOperation.add_datapathElement(val); 
    }
    
    
    public DataAccess.DatapathElementSet new_newJavaClass()
    { DataAccess.DatapathElementSet value = new DataAccess.DatapathElementSet();
      value.whatisit = '+';
      return value;
    }
    
    public void add_newJavaClass(DataAccess.DatapathElementSet val) { add_datapathElement(val); }


    
    

    /**This routine must be called at least. It adds a simple value to the operation list.
     * If any second value was added already, the routine does nothing.
     */
    public void closeExprPreparation(){
      if(actOperation !=null){
        addToOperations();
      }
    }
    
    private void addToOperations(){
      if(!actOperation.hasOperator()){
        actOperation.setOperator("!");  //it is initial value
      }
      if(unaryOperators.size()==1){
          actOperation.addUnaryOperator(unaryOperators.get(0));
      } else if(unaryOperators.size()>1) {
        ListIterator<CalculatorExpr.Operator> iter = unaryOperators.listIterator();
        while(iter.hasPrevious()){
          CalculatorExpr.Operator unary = iter.previous();
          actOperation.addUnaryOperator(unary);
        }
      }
      unaryOperators.clear();  //a new one is necessary.
      expr.addOperation(actOperation);
      actOperation = null;  //a new one is necessary.
    }


    
    /**Adds the {@link #actUnaryOperation} to the expression statements.
     * 
     */
    private void addUnaryToOperations(){
      if(unaryOperators !=null){
        ListIterator<CalculatorExpr.Operator> iter = unaryOperators.listIterator();
        while(iter.hasNext()){ iter.next(); }  //walk to end
        while(iter.hasPrevious()){             //operate backward
          CalculatorExpr.Operator unaryOp = iter.previous();
          CalculatorExpr.Operation unaryOperation = new CalculatorExpr.Operation(unaryOp, CalculatorExpr.Operation.kUnaryOperation);
          expr.addOperation(unaryOperation);    //add the unary as operation. Apply on accu.
        }
        unaryOperators.clear();  //a new one is necessary.
      }
    }
  }

  
  
  
  
  /**All Operations which acts with the accumulator and the stack of values.
   * They will be executed one after another. All calculation rules of prior should be regarded
   * in this order of operations already. It will not be checked here.
   */
  protected final List<Operation> listOperations = new ArrayList<Operation>();
  
  
  /**The stack of values used temporary. It is only in used while any calculate routine runs. 
   * The top of all values is not stored in the stack but in the accu. It means that often the stack 
   * is not used. */
  protected final Stack<Value> stack = new Stack<Value>();
  
  /**The top of stack is the accumulator for the current level of adequate operations,
   * for example all multiplications without stack changing. It is the left operand of an operation.
   * The right operand is given in the operation itself. It an operation acts with the last 2 stack levels,
   * the value of the top of stack (the accu) is set locally in the operation and the second value of the 
   * stack operands is set to the accu. */
  protected Value accu = new Value();
  
  protected String[] variables;
  
  
  
  
  /**An expression can be a String concatenation build with constant strings and data.
   * If this association is not null, the expression is calculated as String expression.*/
  protected ZGenScript.StatementList genString;

  
  
  /**Map of all available operators associated with its String expression.
   * It will be initialized with:
   * <ul>
   * <li>"!" Set the value to accu, set the accu to the stack.
   * <li>"+" "-" "*" "/" known arithmetic operators
   * <li>">=" "<=" ">" "<" "==" "!=" knwon compare operators
   * <li>"<>" other form of not equal operator
   * <li>"ge" "le" "gt" "lt" "eq" "ne" other form of comparators
   * <li>"||" "&&" known logical opeators
   * </ul>
   * Unary operators:
   * <ul> 
   * <li>"b" Convert to boolean.
   * <li>"b!" boolean not operator
   * <li>"u~" bit negation operator
   * <li>"u-" numeric negation
   * </ul>
   */
  protected static Map<String, Operator> operators;
  
  
  /**Map of all available unary operators associated with its String expression.
   * It will be initialized with:
   * <ul>
   * </ul>
   */
  //protected static Map<String, UnaryOperator>  unaryOperators;
  
  
  public CalculatorExpr(){
    if(operators == null){
      operators = new TreeMap<String, Operator>();
      operators.put("!",  Operators.setOperation);   //set accu to operand
      operators.put("+",  Operators.addOperation);
      operators.put("-",  Operators.subOperation);
      operators.put("*",  Operators.mulOperation);
      operators.put("/",  Operators.divOperation);
      operators.put(">=", Operators.cmpGreaterEqualOperation);
      operators.put(">",  Operators.cmpGreaterThanOperation);
      operators.put("<=", Operators.cmpLessEqualOperation);
      operators.put("<",  Operators.cmpLessThanOperation);
      operators.put("!=", Operators.cmpNeOperation);
      operators.put("<>", Operators.cmpNeOperation);
      operators.put("==", Operators.cmpEqOperation);
      operators.put("lt", Operators.cmpLessThanOperation);
      operators.put("le", Operators.cmpLessEqualOperation);
      operators.put("gt", Operators.cmpGreaterThanOperation);
      operators.put("ge", Operators.cmpGreaterEqualOperation);
      operators.put("eq", Operators.cmpEqOperation);
      operators.put("ne", Operators.cmpNeOperation);
      operators.put("||", Operators.boolOrOperation);
      operators.put("&&", Operators.boolAndOperation);
      operators.put("ub", Operators.boolOperation);   //not for boolean
      operators.put("u!", Operators.boolNotOperation);   //not for boolean
      operators.put("u~", Operators.bitNotOperation);   //not for boolean
      operators.put("u-", Operators.negOperation);   //not for boolean

    }
  }
  
  
  /**Separates a name and the parameter list from a given String.
   * @param expr An expression in form " name (params)"
   * @return A String[2].
   *   The ret[0] is the expr without leading and trailing spaces if the expr does not contain a "("
   *   The ret[0] is "" if the expr contains only white spaces before "(" or it starts with "("
   *   The ret[0] contains the string part before "(" without leading and trailing spaces.
   *   The ret[1] contains the part between "(...)" without "()".
   *   The ret[1] is null if no "(" is found in expr..
   *   If the trailing ")" is missing, it is accepting.
   */
  public static String[] splitFnNameAndParams(String expr){
    String[] ret = new String[2];
    int posSep = expr.indexOf('(');
    if(posSep >=0){
      ret[0] = expr.substring(0, posSep).trim();
      int posEnd = expr.lastIndexOf(')');
      if(posEnd < 0){ posEnd = expr.length(); }
      ret[1] = expr.substring(posSep+1, posEnd);
    } else {
      ret[0] = expr.trim();
      ret[1] = null;
    }
    return ret;
  }
  

  
  /**Separates String parameters from a list.
   * Implementation yet: only split
   * Planned: detects name(param, param) inside a parameter.
   * @param expr Any expression with Strings separated with colon
   * @return The split expression, all arguments are trimmed (without leading and trailing spaces).
   */
  public static String[] splitFnParams(String expr){
    String[] split = expr.split(",");
    for(int ii=0; ii<split.length; ++ii){
      split[ii] = split[ii].trim();
    }
    return split;
  }
  
  /**Converts the given expression in a stack operable form.
   * @param sExpr String given expression such as "X*(Y-1)+Z"
   * @param sIdentifier List of identifiers for variables.
   * @return null if ok or an error description.
   */
  public String setExpr(String sExpr, String[] sIdentifier)
  {
    this.variables = sIdentifier;
    StringPartOld sp = new StringPartOld(sExpr);
    return multExpr(sp, "!", 1);  //TODO addExpr
  }
  
  /**Converts the given expression in a stack operable form. One variable with name "X" will be created.
   * It means the expression can use "X" as variable.
   * @param sExpr For example "5.0*X" or "(X*X+1.5*X)"
   * @see #setExpr(String, String[])
   */
  public String setExpr(String sExpr)
  {
    this.variables = new String[]{"X"};
    StringPartOld sp = new StringPartOld(sExpr);
    return addExpr(sp, "!", 1);
  }
  

  /**The outer expression is a add or subtract expression.
   * call recursively for any number of operands.
   * call {@link #multExpr(StringPartOld, char)} to get the argument values.
   * @param sp
   * @param operation The first operation.
   * @return this
   */
  private String addExpr(StringPartOld sp, String operation, int recursion)
  { String sError = null;
    if(recursion > 1000) throw new RuntimeException("recursion");
    sError = multExpr(sp, operation, recursion +1);
    if(sError == null && sp.length()>0){
      char cc = sp.getCurrentChar();
      if("+-".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return addExpr(sp, ""+cc, recursion+1);
      }
    }
    return null;
  }
  
  
  /**The more inner expression is a mult or divide expression.
   * call recursively for any number of operands.
   * call functions to get the argument values. 
   * @param sp
   * @param operation
   * @return
   */
  private String multExpr(StringPartOld sp, String operation, int recursion)
  { if(recursion > 1000) throw new RuntimeException("recursion");
    try{
      if(sp.scanIdentifier().scanOk()){
        String sIdent = sp.getLastScannedString();
        int ix;
        for(ix = 0; ix< variables.length; ++ix){
          if(variables[ix].equals(sIdent)){
            listOperations.add(new Operation(operation, ix));
            ix = Integer.MAX_VALUE-1; //break;
          }
        }
        if(ix != Integer.MAX_VALUE){ //variable not found
          return("unknown variable" + sIdent);
        }
      } else if(sp.scanFloatNumber().scanOk()){
        listOperations.add(new Operation(operation, sp.getLastScannedFloatNumber()));
      }
    }catch(ParseException exc){
      return("ParseException float number"); 
    }
    if(sp.length()>0){
      char cc = sp.getCurrentChar();
      if("*/".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return multExpr(sp, ""+cc, recursion+1);
      }
    }
    return null;  //ok
  }
  

  
  /**Adds the given operation to the list of operations. The list of operations is executed one after another.
   * All calculation rules such as parenthesis, prior of multiplication, functions arguments etc.
   * should be regarded by the user. 
   * @param operation
   */
  public void addOperation(Operation operation){
    listOperations.add(operation);
  }
  
  
  /**Gets a operator to prepare operations.
   * @param op Any of "+", "-" etc.
   * @return null if op is faulty.
   */
  public static Operator getOperator(String op){ return operators.get(op); }
  
  
  
  /**Adds a operation to the execution stack.
   * Operation:
   * <ul>
   * <li>+, - * /
   * <li> < > = l, g 
   * </ul>
   * @param val
   * @param operation
   */
  public void XXXaddExprToStack(Object val, String operation){
    Operator operator = operators.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, val);
    listOperations.add(stackelement);
  }
  
  
  public void XXXaddExprToStack(int ixInputValue, String operation){
    Operator operator = operators.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, ixInputValue);
    listOperations.add(stackelement);
  }
  
  
  
  /**Calculate with more as one input value.
   * @param input
   * @return
   */
  public double calc(double[] input)
  { double val = 0;
    return val;
  }
  

  
  /**Calculates the expression with only one input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public double calc(double input)
  { double val = 0;
    for(Operation oper: listOperations){
      final double val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = oper.value_d; }
      switch(oper.operatorChar){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  

  
  /**Calculates the expression with only one float input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public float calc(float input)
  { float val = 0;
    for(Operation oper: listOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operatorChar){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  

  
  /**Calculates the expression with only one integer input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public float calc(int input)
  { float val = 0;
    for(Operation oper: listOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operatorChar){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  
  
  
  
  /**Calculates the expression with some inputs, often only 1 input.
   * Before calculate, one should have defined the expression using {@link #setExpr(String)}
   * or {@link #setExpr(String, String[])}.
   * @param args Array of some inputs
   * @return The result of the expression.
   */
  public Value calc(Object... args){
    Value accu = new Value();
    Value val2 = new Value();
    ExpressionType check = startExpr;
    for(Operation oper: listOperations){
      //Get the operand either from args or from Operation
      Object oVal2;
      if(oper.ixVariable >=0){ oVal2 = args[oper.ixVariable]; }  //an input value
      else { oVal2 = oper.oValue; }                              //a constant value inside the Operation
      //
      //Convert the value adequate the given type of expression:
      check = check.checkArgument(accu, val2);    //may change the type.
      //
      //executes the operation:
      check = oper.operator.operate(check, accu, val2);  //operate, may change the type if the operator forces it.
    }
    accu.type = check.typeChar();  //store the result type
    return accu;
  }
  
  

  
  
  
  
  
  
  
  
  
  /**Calculates the expression with possible access to any stored object data with access via reflection.
   * An value can contain a {@link Datapath#datapath} which describe any class's field or method
   * which were found via a reflection access. The datapath is build calling {@link #setExpr(String)}
   * or {@link #setExpr(String, String[])} using the form "$var.reflectionpath"
   * 
   * @param javaVariables Any data which are access-able with its name. It is the first part of a datapath.
   * @param args Some args given immediately. Often numerical args. Often not used.
   * @return The result wrapped with a Value instance. This Value contains also the type info. 
   * @throws Exception Any exception is possible. Especially {@link java.lang.NoSuchFieldException} or such
   *   if the access via reflection is done.
   */
  public Value calcDataAccess(Map<String, DataAccess.Variable> javaVariables, Object... args) throws Exception{
    if(genString !=null){
      ZGenExecuter.ExecuteLevel executer = (ZGenExecuter.ExecuteLevel)DataAccess.getVariable(javaVariables, "jbatExecuteLevel", true);
      StringBuilder u = new StringBuilder();
      executer.executeNewlevel(genString, u, false);
      return new CalculatorExpr.Value(u.toString());
    } else {
      accu = new Value(); //empty
      Value val3 = new Value();  //Instance to hold values for the right side operand.
      Value val2; //Reference to the right side operand
      ExpressionType type = startExpr;
      for(Operation oper: listOperations){
        if(accu.type == 'Z' && //special for boolean operation: don't evaluate an operand if it is not necessary.
            ( !accu.boolVal && oper.operator == Operators.boolAndOperation  //false remain false on and operation
            || accu.boolVal && oper.operator == Operators.boolOrOperation   //true remain true on or operation
          ) ){
          //don't get arguments, no side effect (like in Java, C etc.
        } else {
          //Get the operand either from args or from Operation
          Object oval2;
          if(oper.ixVariable >=0){ 
            val2 = val3;
            oval2 = args[oper.ixVariable];   //may throw ArrayOutOfBoundsException if less arguments
          }  //an input value
          else if(oper.ixVariable == Operation.kStackOperand){
            val2 = accu;
            accu = stack.pop();              //may throw Exception if the stack is emtpy.
            oval2 = null;
          }
          else if(oper.datapath !=null){
            val2 = val3;
            oval2 = oper.datapath.getDataObj(javaVariables, true, false);
            if(oval2 == null){
              //get data does not throw an exception, but returns null:
              val2.type = 'o'; val2.oVal = null;
            }
          }
          else {
            val2 = oper.value;              //immediate value.
            oval2 = null;
          }
          //
          //Convert a Object-wrapped value into its real representation.
          if(oval2 !=null){
            if(oval2 instanceof Long)             { val2.longVal =   ((Long)oval2).longValue(); val2.type = 'J'; }
            else if(oval2 instanceof Integer)     { val2.intVal = ((Integer)oval2).intValue(); val2.type = 'I'; }
            else if(oval2 instanceof Short)       { val2.intVal =   ((Short)oval2).intValue(); val2.type = 'I'; }
            else if(oval2 instanceof Byte)        { val2.intVal =    ((Byte)oval2).intValue(); val2.type = 'I'; }
            else if(oval2 instanceof Boolean)     { val2.boolVal = ((Boolean)oval2).booleanValue(); val2.type = 'Z'; }
            else if(oval2 instanceof Double)      { val2.doubleVal = ((Double)oval2).doubleValue(); val2.type = 'D'; }
            else if(oval2 instanceof Float)       { val2.floatVal = ((Float)oval2).floatValue(); val2.type = 'F'; }
            else if(oval2 instanceof StringSeq)   { val2.stringVal = (StringSeq)oval2; val2.type = 't'; }
            else                                  { val2.oVal = oval2; val2.type = 'o'; }
            val2.oVal = oval2;;
          }
          if(oper.operator == Operators.setOperation && accu.type != '?'){
            stack.push(accu);
            accu = new Value();
          }
          //Convert the value adequate the given type of expression:
          if(!oper.operator.isUnary()){  //if unary, don't change the type
            type = type.checkArgument(accu, val2);    //may change the type.
          }
          //
          //executes the operation:
          if(oper.unaryOperator !=null){
            oper.unaryOperator.operate(type, val2, null);   //change the right value
          }
          else if(oper.unaryOperators !=null){
            for(Operator unary: oper.unaryOperators){
              unary.operate(type, val2, null);   //change the right value
            }
          }
          type = oper.operator.operate(type, accu, val2);  //operate, may change the type if the operator forces it.
        }
      }
      return accu;
    }
  }
  
  
  
  
  
}
