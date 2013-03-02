package org.vishia.bridgeC;


/**Represents a Java-complement to a C-struct which contains va_list reference from stdarg.h. 
 * The struct in C has the form:
 * <pre>
 * typedef struct Va_listFW_t
 * { 
 *   char const* typeArgs;    //types of arguments.
 *   va_list args;            //pointer to the last known variable in stack
 * } Va_listFW;
 * </pre>
 * In Java the instance contains a reference to {@link VaArgBuffer} only.
 * For compatibility with C-translated Java one can only initialize the {@link Va_list} with a variable
 * which is the variable argument of a methods argument list. In Java semantic you can initalize with any variable,
 * but that is not able to translate from Java to C (Java2C).
 * */
public class Va_list
{
  final VaArgBuffer buffer;
  
  public Va_list(VaArgBuffer buffer){ this.buffer = buffer; }
  
  /**Constructs a Va_list object which is the conterpart to a C-language <code>Va_listFW</code>.
   * The argument <code>args</code> in C-language is a pointer to variable arguments in the stack,
   * more exact to the last known variable before a variable argument list follows. This last known variable
   * should be a String Literal with contains the type characters like <code>"FIB"</code> by convention. 
   * That is a pointer of type <code>char const*</code>. 
   * In C-language the both values are gotten with
   * <pre>
   * myVA_list.typeArgs = args;
   * va_start(myVA_list.args, args);
   * </pre>
   * In Java a variable argument list is a <code>Object[]</code>.
   * @param args refers a variable argument list.
   */
  public Va_list(Object... args)
  { buffer = new VaArgBuffer(args);
  }
  
  /**Returns the stored arguments. In Java it is a Object[].
   * In C the va_list is used immediate.
   */
  public Object[] get(){ return buffer.get(); }
}
