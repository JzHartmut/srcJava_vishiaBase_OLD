package org.vishia.util;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CalculatorExpr
{
  
  /**Versio, history and license.
   * <ul>
   * <li>2012-12-22 some enhancements while using in {@link org.vishia.textGenerator.TextGenerator}.
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
  
  
  
  public static class Value{
    char type = '?';
    long longVal;
    double doubleVal;
    boolean boolVal;
    String stringVal;
    Object oVal;
    
    /**Returns a boolean value. If the type of content is a numeric, false is returned if the value is ==0.
     * If the type is a text, false is returned if the string is empty.
     * If the type is any other Object, false is returned if the object referenz is ==null.
     * @return The boolean value.
     */
    public boolean booleanValue()
    { switch(type){
        case 'L': return longVal !=0;
        case 'D': return doubleVal !=0;
        case 'Z': return boolVal;
        case 't': return stringVal.length() >0;
        case 'o': return oVal !=null;
        case '?': throw new IllegalArgumentException("the type is not determined while operation.");
        default: throw new IllegalArgumentException("unknown type char: " + type);
      }//switch
    }
  }
  
  
  
  private abstract static class ExpressionType{
    abstract char typeChar();
    abstract ExpressionType checkArgument(Value accu, Value val2, Object src);
  }
  
  
  private abstract static class Operator{
    abstract ExpressionType operate(ExpressionType Type, Value accu, Value arg);
  }
  
  
  
  private static final ExpressionType longExpr = new ExpressionType(){
    
    @Override public char typeChar() { return 'L'; }
    
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
  
  private static final ExpressionType booleanExpr = new ExpressionType(){

    @Override public char typeChar() { return 'Z'; }
    
    @Override public ExpressionType checkArgument(Value accu, Value val2, Object src) {
      if(src instanceof Long){ val2.boolVal = ((Long)src) !=0; }
      else if(src instanceof Double){ val2.boolVal = ((Double)src) !=0; }
      else if(src instanceof Boolean){ val2.boolVal = ((Boolean)src); }
      else throw new IllegalArgumentException("src type");
      return this;   //boolean remain boolean.
    }
    
  };
  
  
  private static final Operator setOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.longVal = arg.longVal; break;
        case 'D': accu.doubleVal = arg.doubleVal; break;
        case 'Z': accu.boolVal = arg.boolVal; break;
        case 't': accu.stringVal = arg.stringVal; break;
        case 'o': accu.oVal = arg.oVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator addOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.longVal += arg.longVal; break;
        case 'D': accu.doubleVal += arg.doubleVal; break;
        case 'Z': accu.doubleVal += arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator subOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.longVal -= arg.longVal; break;
        case 'D': accu.doubleVal -= arg.doubleVal; break;
        case 'Z': accu.doubleVal -= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator mulOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.longVal *= arg.longVal; break;
        case 'D': accu.doubleVal *= arg.doubleVal; break;
        case 'Z': accu.doubleVal *= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator divOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.longVal /= arg.longVal; break;
        case 'D': accu.doubleVal /= arg.doubleVal; break;
        case 'Z': accu.doubleVal /= arg.doubleVal; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return type;
    }
  };
  
   
  private static final Operator cmpEqOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.boolVal = accu.longVal == arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) < (Math.abs(accu.doubleVal) / 100000); break;
        case 'Z': accu.boolVal = accu.boolVal == arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.equals(arg.stringVal); break;
        case 'o': accu.boolVal = accu.oVal == null && arg.oVal == null || (accu.oVal !=null && arg.oVal !=null && accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpNeOperation = new Operator(){
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.boolVal = accu.longVal != arg.longVal; break;
        case 'D': accu.boolVal = Math.abs(accu.doubleVal - arg.doubleVal) >= (Math.abs(accu.doubleVal) / 100000); break;
        case 'Z': accu.boolVal = accu.boolVal != arg.boolVal; break;
        case 't': accu.boolVal = !accu.stringVal.equals(arg.stringVal); break;
        case 'o': accu.boolVal = !(accu.oVal == null && arg.oVal == null) || (accu.oVal !=null && arg.oVal !=null && !accu.oVal.equals(arg.oVal)); break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpLessThanOperation = new Operator(){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.boolVal = accu.longVal < arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal < arg.doubleVal; break;
        case 'Z': accu.boolVal = !accu.boolVal && arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) < 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) < 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpGreaterEqualOperation = new Operator(){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.boolVal = accu.longVal >= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal >= arg.doubleVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) >= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) >= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpGreaterThanOperation = new Operator(){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.boolVal = accu.longVal > arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal > arg.doubleVal; break;
        case 'Z': accu.boolVal = accu.boolVal && !arg.boolVal; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) > 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) > 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   
  private static final Operator cmpLessEqualOperation = new Operator(){
    @SuppressWarnings("unchecked")
    @Override public ExpressionType operate(ExpressionType type, Value accu, Value arg) {
      switch(type.typeChar()){
        case 'L': accu.boolVal = accu.longVal <= arg.longVal; break;
        case 'D': accu.boolVal = accu.doubleVal <= arg.doubleVal; break;
        case 'Z': accu.boolVal = true; break;
        case 't': accu.boolVal = accu.stringVal.compareTo(arg.stringVal) <= 0; break;
        case 'o': accu.boolVal = accu.oVal instanceof Comparable<?> && arg.oVal instanceof Comparable<?> ? ((Comparable)accu.oVal).compareTo(arg.oVal) <= 0 : false; break;
        default: throw new IllegalArgumentException("unknown type" + type.toString());
      }
      return booleanExpr;
    }
  };
  
   

  
  /**A Operation in the stack of operations.
   */
  private static final class Operation
  {
    /**The operation Symbol if it is a primitive. */
    final char operation;
    
    /**The operator for this operation. */
    Operator operator;
    
    /**Number of input variable from array. */
    final int ixVariable;
    
    /**A constant value. */
    final double value;
    
    /**A constant value. */
    final Object oValue;
    
    Operation(char operation, double value){ this.value = value; this.operation = operation; this.ixVariable = -1; this.oValue = null; }
    Operation(char operation, int ixVariable){ this.value = 0; this.operation = operation; this.ixVariable = ixVariable; this.oValue = null; }
    //Operation(char operation, Object oValue){ this.value = 0; this.operation = operation; this.ixVariable = -1; this.oValue = oValue; }
    Operation(Operator operator, int ixVariable){ this.value = 0; this.operator = operator; this.operation = '.'; this.ixVariable = ixVariable; this.oValue = null; }
    Operation(Operator operator, Object oValue){ this.value = 0; this.operator = operator; this.operation = '.'; this.ixVariable = -1; this.oValue = oValue; }
  }
  
  private final List<Operation> stackExpr = new LinkedList<Operation>();
  
  private String[] variables;
  
  
  
  static Map<String, Operator> operations;
  
  
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
   * @param sExpr
   */
  public String setExpr(String sExpr, String[] sIdentifier)
  {
    this.variables = sIdentifier;
    StringPart sp = new StringPart(sExpr);
    return multExpr(sp, '!', 1);
  }
  
  /**Converts the given expression in a stack operable form.
   * @param sExpr
   */
  public String setExpr(String sExpr)
  {
    this.variables = new String[]{"X"};
    StringPart sp = new StringPart(sExpr);
    return addExpr(sp, '!',1);
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
  public void addExprToStack(Object val, String operation){
    Operator operator = operations.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, val);
    stackExpr.add(stackelement);
  }
  
  
  public void addExprToStack(int ixInputValue, String operation){
    Operator operator = operations.get(operation);
    if(operator == null) throw new IllegalArgumentException("unknown Operation: " + operation);
    Operation stackelement = new Operation(operator, ixInputValue);
    stackExpr.add(stackelement);
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
            stackExpr.add(new Operation(operation, ix));
            ix = Integer.MAX_VALUE-1; //break;
          }
        }
        if(ix != Integer.MAX_VALUE){ //variable not found
          return("unknown variable" + sIdent);
        }
      } else if(sp.scanFloatNumber().scanOk()){
        stackExpr.add(new Operation(operation, sp.getLastScannedFloatNumber()));
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
    for(Operation oper: stackExpr){
      final double val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = oper.value; }
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
    for(Operation oper: stackExpr){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value; }
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
    for(Operation oper: stackExpr){
      final float val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = (float)oper.value; }
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
   * Before calculate, one should have defined the expression using 
   * @param args Array of some inputs
   * @return The result of the expression.
   */
  public Value calc(Object... args){
    Value accu = new Value();
    Value val2 = new Value();
    ExpressionType check = longExpr;
    for(Operation oper: stackExpr){
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
  
  
  
  
  
}
