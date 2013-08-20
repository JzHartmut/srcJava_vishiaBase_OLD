package org.vishia.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**This class provides a calculator for expressions. The expressions are given in string format 
 * and then converted to a stack oriented running model. 
 * <br><br>
 * <ul>
 * <li>Use {@link #setExpr(String)} to convert a String given expression to the internal format.
 * <li>Use {@link #calc(float)} for simple operations with one float input, especially for scaling values. It is fast.
 * <li>Use {@link #calc(Object...)} for universal expression calculation. 
 * </ul>
 * If the expression works with simple variable or constant values, it is fast. For example it is able to use
 * in a higher frequently graphic application to scale values.
 * <br><br>
 * A value or subroutine can use any java elements which are accessed via reflection mechanism.
 * Write "X * $classpath.Myclass.method(X)" to build an expression which's result depends on the called
 * static method.
 *  
 * @author Hartmut Schorrig
 *
 */
public class CalculatorExpr
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-08-18 Hartmut new: {@link Operation#unaryOperator}
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
  
   
   
  public static class DataPathItem extends DataAccess.DatapathElement
  {
    protected List<CalculatorExpr> paramExpr;

  }
   
   
  
  /**A path to any Java Object or method given with identifier names.
   * The access is organized using reflection.
   * <ul>
   * <li>This class can describe a left value. It may be a Container to which a value is added
   * or a {@link java.lang.Appendable}, to which a String is added.  
   * <li>This class can describe a value, which is the result of access to the last element of the path.
   * </ul>
   */
  public static class Datapath
  {
    /**The description of the path to any data if the script-element refers data. It is null if the script element
     * does not refer data. If it is filled, the instances are of type {@link ZbnfDataPathElement}.
     * If it is used in {@link DataAccess}, its base class {@link DataAccess.DatapathElement} are used. The difference
     * are the handling of actual values for method calls. See {@link ZbnfDataPathElement#actualArguments}.
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
     * <li>J I D F Z: long, double, boolean, the known Java characters for types see {@link java.lang.Class#getName()}
     * <li>o: The oVal contains any object.
     * <li>t: A String stored in stringVal,
     * <li>d: Access via the data path using reflection
     * </ul>
     */
    protected char type = '?';
    protected long longVal;
    protected double doubleVal;
    protected boolean boolVal;
    protected String stringVal;
    protected Object oVal;
    
    public Value(long val){ type = 'J'; longVal = val; }
    
    public Value(int val){ type = 'J'; longVal = val; }
    
    public Value(double val){ type = 'D'; doubleVal = val; }
    
    public Value(float val){ type = 'D'; doubleVal = val; }
    
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
        case 'I':
        case 'J': return longVal !=0;
        case 'D': return doubleVal !=0;
        case 'Z': return boolVal;
        case 't': return stringVal.length() >0;
        case 'o': return oVal !=null;
        case '?': throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public double doubleValue()
    { switch(type){
        case 'I':
        case 'J': return longVal;
        case 'D': return doubleVal;
        case 'Z': return boolVal ? 1.0 : 0;
        case 't': return Double.parseDouble(stringVal);
        case 'o': throw new IllegalArgumentException("Double expected, object given.");
        case '?': throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
    
    public String stringValue(){ 
      switch(type){
        case 'I':
        case 'J': return Long.toString(longVal);
        case 'D': return Double.toString(doubleVal);
        case 'Z': return Boolean.toString(boolVal);
        case 't': return stringVal;
        case 'o': return oVal ==null ? "null" : oVal.toString();
        default:  return "?" + type;
      }//switch
    }

    public Object objValue(){ 
      switch(type){
        case 'I':
        case 'J': return new Long(longVal);
        case 'D': return new Double(doubleVal);
        case 'Z': return new Boolean(boolVal);
        case 't': return stringVal;
        case 'o': return oVal;
        default:  return "?" + type;
      }//switch
    }

    @Override public String toString(){ 
      switch(type){
        case 'I':
        case 'J': return Long.toString(longVal);
        case 'D': return Double.toString(doubleVal);
        case 'Z': return Boolean.toString(boolVal);
        case 't': return stringVal;
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
    
    /**Checks the input value and set it to val2 maybe with converted type.
     * @param accu The accumalator contains the current type.
     * @param val2 ready to set with src.
     * @param src Any object of data.
     * @return type of the expression.
     */
    abstract ExpressionType checkArgument(Value accu, Value val2, Object src);
  }
  
  
  private abstract static class Operator{
    private final String name; 
    Operator(String name){ this.name = name; }
    abstract ExpressionType operate(ExpressionType Type, Value accu, Value arg);
    @Override public String toString(){ return name; }
  }
  
  
  private abstract static class UnaryOperator{
    private final String name; 
    UnaryOperator(String name){ this.name = name; }
    abstract ExpressionType operate(ExpressionType Type, Value val);
    @Override public String toString(){ return name; }
  }
  
  
  
  private static final ExpressionType startExpr = new ExpressionType(){
    
    @Override public char typeChar() { return '!'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value setit, Object src) {
      if(src instanceof String){ setit.stringVal = (String)src; return stringExpr; }
      else if(src instanceof Long){ setit.longVal = (Long)src; return longExpr; }
      else if(src instanceof Integer){ setit.longVal = (Integer)src; return longExpr; }
      else if(src instanceof Short){ setit.longVal = (Short)src; return longExpr; }
      else if(src instanceof Byte){ setit.longVal = (Byte)src; return longExpr; }
      else if(src instanceof Character){ setit.longVal = (Character)src; return longExpr; }  //use its UTF16-code.
      else if(src instanceof Double){ setit.doubleVal = (Double)src; 
        if(accu.type !='D') { accu.doubleVal = accu.doubleValue(); }  //work with double if long was stored.
        return longExpr;  //TODO
      }
      else { setit.oVal = src; return objExpr; }
    }
  };
  
  private static final ExpressionType stringExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 't'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value setit, Object src) {
      if(src instanceof String){ setit.stringVal = (String)src; return longExpr; }
      if(src instanceof Long){ setit.longVal = (Long)src; return this; }
      else if(src instanceof Integer){ setit.longVal = (Integer)src; return this; }
      else if(src instanceof Short){ setit.longVal = (Short)src; return this; }
      else if(src instanceof Byte){ setit.longVal = (Byte)src; return this; }
      else if(src instanceof Character){ setit.longVal = (Character)src; return this; }  //use its UTF16-code.
      else if(src instanceof Double){ setit.doubleVal = (Double)src; accu.doubleVal = accu.longVal;
        return longExpr;
      }
      else throw new IllegalArgumentException("src type");
    }

  };
  
  private static final ExpressionType longExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'J'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value setit, Object src) {
      if(src instanceof Long){ setit.longVal = (Long)src; return this; }
      else if(src instanceof Integer){ setit.longVal = (Integer)src; return this; }
      else if(src instanceof Short){ setit.longVal = (Short)src; return this; }
      else if(src instanceof Byte){ setit.longVal = (Byte)src; return this; }
      else if(src instanceof Character){ setit.longVal = (Character)src; return this; }  //use its UTF16-code.
      else if(src instanceof Double){ setit.doubleVal = (Double)src; accu.doubleVal = accu.longVal;
        return longExpr;
      }
      else throw new IllegalArgumentException("src type");
    }

  };
  
  protected static final ExpressionType objExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'o'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value setit, Object src) {
      throw new IllegalArgumentException("no operation available for Object type.");
    }

  };
  
  protected static final ExpressionType booleanExpr = new ExpressionType(){

    @Override public char typeChar() { return 'Z'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2, Object src) {
      if(src instanceof Long){ val2.boolVal = ((Long)src) !=0; }
      else if(src instanceof Double){ val2.boolVal = ((Double)src) !=0; }
      else if(src instanceof Boolean){ val2.boolVal = ((Boolean)src); }
      else throw new IllegalArgumentException("src type");
      return this;   //boolean remain boolean.
    }
    
  };
  
  
  private static final UnaryOperator boolOperation = new UnaryOperator("bool "){
    @Override public ExpressionType operate(ExpressionType type, Value accu) {
      accu.boolVal = accu.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
  };
  
   
  private static final UnaryOperator boolNotOperation = new UnaryOperator("b!"){
    @Override public ExpressionType operate(ExpressionType type, Value accu) {
      accu.boolVal = !accu.booleanValue();
      return booleanExpr;
    }
  };
  
   
  private static final UnaryOperator bitNotOperation = new UnaryOperator("~u"){
    @Override public ExpressionType operate(ExpressionType type, Value accu) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal = ~accu.longVal; break;
        //case 'D': accu.doubleVal = accu.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final UnaryOperator negOperation = new UnaryOperator("-u"){
    @Override public ExpressionType operate(ExpressionType type, Value accu) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal = -accu.longVal; break;
        case 'D': accu.doubleVal = -accu.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal; break;
        //case 't': accu.stringVal = accu.stringVal; break;
        //case 'o': accu.oVal = accu.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator setOperation = new Operator("!"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal = arg.longVal; break;
        case 'D': accu.doubleVal = arg.doubleVal; break;
        case 'Z': accu.boolVal = arg.boolVal; break;
        case 't': accu.stringVal = arg.stringVal; break;
        case 'o': accu.oVal = arg.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator addOperation = new Operator("+"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal += arg.longVal; break;
        case 'D': accu.doubleVal += arg.doubleVal; break;
        case 'Z': accu.doubleVal += arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator subOperation = new Operator("-"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal -= arg.longVal; break;
        case 'D': accu.doubleVal -= arg.doubleVal; break;
        case 'Z': accu.doubleVal -= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator mulOperation = new Operator("*"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal *= arg.longVal; break;
        case 'D': accu.doubleVal *= arg.doubleVal; break;
        case 'Z': accu.doubleVal *= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator divOperation = new Operator("/"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.longVal /= arg.longVal; break;
        case 'D': accu.doubleVal /= arg.doubleVal; break;
        case 'Z': accu.doubleVal /= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator cmpEqOperation = new Operator(".cmp."){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.boolVal = accu.longVal == arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) < (Math.abs(accu.doubleVal) / 100000); break;
        case 'Z': accu.boolVal = accu.boolVal == arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.equals(arg.stringVal); break;
        case 'o': accu.boolVal = accu.oVal == null && arg.oVal == null || (accu.oVal !=null && arg.oVal !=null && accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpNeOperation = new Operator("!="){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.boolVal = accu.longVal != arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) >= (Math.abs(accu.doubleVal) / 100000); break;
        case 'Z': accu.boolVal = accu.boolVal != arg.boolVal; break;
        case 't': accu.boolVal = !accu.stringVal.equals(arg.stringVal); break;
        case 'o': accu.boolVal = !(accu.oVal == null && arg.oVal == null) || (accu.oVal !=null && arg.oVal !=null && !accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator boolOrOperation = new Operator("||"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() || arg.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
  };
  
   
  private static final Operator boolAndOperation = new Operator("&&"){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      accu.boolVal = accu.booleanValue() && arg.booleanValue();
      accu.type = 'Z';
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpLessThanOperation = new Operator("<"){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.boolVal = accu.longVal < arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal < arg.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal && arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) < 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) < 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpGreaterEqualOperation = new Operator(">="){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.boolVal = accu.longVal >= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal >= arg.doubleVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) >= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) >= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpGreaterThanOperation = new Operator(">"){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.boolVal = accu.longVal > arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal > arg.doubleVal; break;
        case 'Z': accu.boolVal = accu.boolVal && !arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) > 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) > 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpLessEqualOperation = new Operator("<="){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'I':
        case 'J': accu.boolVal = accu.longVal <= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal <= arg.doubleVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) <= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) <= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   

  
  /**An Operation in the list of operations. It contains the operator and maybe a operand.
   * <ul>
   * <li>The operator operates with the current stack context.
   * <li>An unary operator will be applied to the value firstly. 
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
    private static final int kStackOperand = -2; 
     
    /**The operation Symbol if it is a primitive. 
     * <ul>
     * <li>! Take the value
     * <li>+ - * / :arithmetic operation
     * <li>& | : bitwise operation
     * <li>A O X: boolean operation and, or, xor 
     * */
    private char operation;
    
    /**The operator for this operation. */
    Operator operator;
    
    UnaryOperator unaryOperator;
    
    /**Number of input variable from array or special designation.
     * See {@link #kStackOperand} */
    int ixVariable;
    
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
      this.ixVariable = -1; 
    }
    
    public Operation(String operation){
      this.operation = operation.charAt(0);
      this.operator = operations.get(operation);
      this.ixVariable = -1; 
      this.oValue = null;
      this.value_d = 0;
    }
    
    Operation(char operation, double value){ this.value_d = value; this.operation = operation; this.ixVariable = -1; this.oValue = null; }
    Operation(char operation, int ixVariable){ this.value_d = 0; this.operation = operation; this.ixVariable = ixVariable; this.oValue = null; }
    //Operation(char operation, Object oValue){ this.value = 0; this.operation = operation; this.ixVariable = -1; this.oValue = oValue; }
    Operation(Operator operator, int ixVariable){ this.value_d = 0; this.operator = operator; this.operation = '.'; this.ixVariable = ixVariable; this.oValue = null; }
    Operation(Operator operator, Object oValue){ this.value_d = 0; this.operator = operator; this.operation = '.'; this.ixVariable = -1; this.oValue = oValue; }
  
    
    
    public boolean hasOperator(){ return operator !=null; }
    
    public boolean hasUnaryOperator(){ return unaryOperator !=null; }
    
    public void add_datapathElement(DataAccess.DatapathElement item){ 
      if(datapath == null){ datapath = new DataAccess();}
      datapath.add_datapathElement(item); 
    }
    
    public void set_intValue(int val){
      if(value == null){ value = new Value(); }
      value.type = 'I';
      value.longVal = val;
    }
    
    
    public void set_textValue(String val){
      if(value == null){ value = new Value(); }
      value.type = 't';
      value.stringVal = val;
    }

    /**Designates that the operand should be located in the top of stack. */
    public void setStackOperand(){ ixVariable = kStackOperand; }
    
    
    
    public boolean setUnaryOperator(String op){
      this.unaryOperator = unaryOperators.get(op);
      return this.operator !=null; 
    }
    
    public boolean setOperator(String op){
      this.operation = op.charAt(0);
      this.operator = operations.get(op);
      return this.operator !=null; 
    }
    
    @Override public String toString(){ 
      StringBuilder u = new StringBuilder();
      u.append(operator);
      if(unaryOperator !=null){ u.append(" ").append(unaryOperator); }
      if(ixVariable >=0) u.append(" arg[").append(ixVariable).append("]");
      else if(ixVariable == kStackOperand) u.append(" stack");
      else if(datapath !=null) u.append(datapath.toString());
      else if (oValue !=null) u.append(" oValue:").append(oValue.toString());
      else if (value !=null) u.append(" ").append(value.toString());
      else u.append(" double:").append(value_d);
      return u.toString();
    }
  }
  
  /**All Operations which acts with the accumulator and the stack of values.
   * They will be executed one after another. All calculation rules of prior should be regarded
   * in this order of operations already. It will not be checked here.
   */
  protected final List<Operation> stackOperations = new ArrayList<Operation>();
  
  private String[] variables;
  
  
  
  static Map<String, Operator> operations;
  static Map<String, UnaryOperator>  unaryOperators;
  
  
  public CalculatorExpr(){
    if(operations == null){
      operations = new TreeMap<String, Operator>();
      operations.put("!",  setOperation);   //set accu to operand
      operations.put("+",  addOperation);
      operations.put("-",  subOperation);
      operations.put("*",  mulOperation);
      operations.put("/",  divOperation);
      operations.put(">=", cmpGreaterEqualOperation);
      operations.put(">",  cmpGreaterThanOperation);
      operations.put("<=", cmpLessEqualOperation);
      operations.put("<",  cmpLessThanOperation);
      operations.put("!=", cmpNeOperation);
      operations.put("<>", cmpNeOperation);
      operations.put("==", cmpEqOperation);
      operations.put("lt", cmpLessThanOperation);
      operations.put("le", cmpLessEqualOperation);
      operations.put("gt", cmpGreaterThanOperation);
      operations.put("ge", cmpGreaterEqualOperation);
      operations.put("eq", cmpEqOperation);
      operations.put("ne", cmpNeOperation);
      operations.put("||", boolOrOperation);
      operations.put("&&", boolAndOperation);
      unaryOperators = new TreeMap<String, UnaryOperator>();
      unaryOperators.put("b",  boolOperation);   //not for boolean
      unaryOperators.put("b!",  boolNotOperation);   //not for boolean
      unaryOperators.put("~",  bitNotOperation);   //not for boolean
      unaryOperators.put("-",  negOperation);   //not for boolean

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
    StringPart sp = new StringPart(sExpr);
    return multExpr(sp, '!', 1);  //TODO addExpr
  }
  
  /**Converts the given expression in a stack operable form. One variable with name "X" will be created.
   * It means the expression can use "X" as variable.
   * @param sExpr For example "5.0*X" or "(X*X+1.5*X)"
   * @see #setExpr(String, String[])
   */
  public String setExpr(String sExpr)
  {
    this.variables = new String[]{"X"};
    StringPart sp = new StringPart(sExpr);
    return addExpr(sp, '!',1);
  }
  

  /**The outer expression is a add or subtract expression.
   * call recursively for any number of operands.
   * call {@link #multExpr(StringPart, char)} to get the argument values.
   * @param sp
   * @param operation The first operation.
   * @return this
   */
  private String addExpr(StringPart sp, char operation, int recursion)
  { String sError = null;
    if(recursion > 1000) throw new RuntimeException("recursion");
    sError = multExpr(sp, operation, recursion +1);
    if(sError == null && sp.length()>0){
      char cc = sp.getCurrentChar();
      if("+-".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return addExpr(sp, cc, recursion+1);
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
  private String multExpr(StringPart sp, char operation, int recursion)
  { if(recursion > 1000) throw new RuntimeException("recursion");
    try{
      if(sp.scanIdentifier().scanOk()){
        String sIdent = sp.getLastScannedString();
        int ix;
        for(ix = 0; ix< variables.length; ++ix){
          if(variables[ix].equals(sIdent)){
            stackOperations.add(new Operation(operation, ix));
            ix = Integer.MAX_VALUE-1; //break;
          }
        }
        if(ix != Integer.MAX_VALUE){ //variable not found
          return("unknown variable" + sIdent);
        }
      } else if(sp.scanFloatNumber().scanOk()){
        stackOperations.add(new Operation(operation, sp.getLastScannedFloatNumber()));
      }
    }catch(ParseException exc){
      return("ParseException float number"); 
    }
    if(sp.length()>0){
      char cc = sp.getCurrentChar();
      if("*/".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return multExpr(sp, cc, recursion+1);
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
    stackOperations.add(operation);
  }
  
  
  
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
    Operator operator = operations.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, val);
    stackOperations.add(stackelement);
  }
  
  
  public void XXXaddExprToStack(int ixInputValue, String operation){
    Operator operator = operations.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, ixInputValue);
    stackOperations.add(stackelement);
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
    for(Operation oper: stackOperations){
      final double val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = oper.value_d; }
      switch(oper.operation){
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
    for(Operation oper: stackOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operation){
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
    for(Operation oper: stackOperations){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value_d; }
      switch(oper.operation){
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
    for(Operation oper: stackOperations){
      //Get the operand either from args or from Operation
      Object oVal2;
      if(oper.ixVariable >=0){ oVal2 = args[oper.ixVariable]; }  //an input value
      else { oVal2 = oper.oValue; }                              //a constant value inside the Operation
      //
      //Convert the value adequate the given type of expression:
      check = check.checkArgument(accu, val2, oVal2);    //may change the type.
      //
      //executes the operation:
      check = oper.operator.operate(check, accu, val2);  //operate, may change the type if the operator forces it.
    }
    accu.type = check.typeChar();  //store the result type
    return accu;
  }
  
  

  
  
  
  
  
  /**Prepares the data access. First the arguments should be evaluated if a method will be called.
   * @param datapath
   * @param javaVariables
   * @return
   * @throws Exception
   */
  private Object getDataAccess(List<DataAccess.DatapathElement> datapath, Map<String, Object> javaVariables) 
  throws Exception {
    for(DataAccess.DatapathElement dataElement : datapath){  
      //loop over all elements of the path to check whether it is a method and it have arguments.
      DataPathItem zd = dataElement instanceof DataPathItem ? (DataPathItem)dataElement : null;
      //A DatapathItem contains the path to parameter.
      if(zd !=null && zd.paramExpr !=null){
        //it is a element with arguments, usual a method call. 
        zd.removeAllActualArguments();
        for(CalculatorExpr expr: zd.paramExpr){
          //evaluate its value.
          Value value = expr.calcDataAccess(javaVariables);
          zd.addActualArgument(value.objValue());
        }
      }
    }
    //Now access the data.
    Object oVal2 = DataAccess.getData(datapath, null, javaVariables, false, false);
    return oVal2;
  }
  
  
  
  
  
  
  /**Calculates the expression with possible access to any stored object data and access support via reflection.
   * An value can contain a {@link Datapath#datapath} which describe any class's field or method
   * which were found via a reflection access. The datapath is build calling {@link #setExpr(String)}
   * or {@link #setExpr(String, String[])} using the form "$var.reflectionpath"
   * 
   * @param javaVariables Any data which are access-able with its name. It is the first part of a datapath.
   * @param args Some args given immediately. Often numerical args.
   * @return The result wrapped with a Value instance. This Value contains the type info too. 
   * @throws Exception Any exception is possible. Especially {@link java.lang.NoSuchFieldException} or such
   *   if the access via reflection is done.
   */
  public Value calcDataAccess(Map<String, Object> javaVariables, Object... args) throws Exception{
    Value accu = new Value();
    Value val2 = new Value();
    ExpressionType check = startExpr;
    for(Operation oper: stackOperations){
      //Get the operand either from args or from Operation
      Object oVal2;
      if(oper.ixVariable >=0){ oVal2 = args[oper.ixVariable]; }  //an input value
      else if(oper.datapath !=null){
        oVal2 = oper.datapath.getDataObj(javaVariables, false);
      }
      else if(oper.value !=null){
        val2 = oper.value;
        oVal2 = val2.objValue();
      }
      else { oVal2 = oper.oValue; }                              //a constant value inside the Operation
      //
      //Convert the value adequate the given type of expression:
      check = check.checkArgument(accu, val2, oVal2);    //may change the type.
      //
      //executes the operation:
      check = oper.operator.operate(check, accu, val2);  //operate, may change the type if the operator forces it.
    }
    accu.type = check.typeChar();  //store the result type
    return accu;
  }
  
  
  
  
  
}
