package org.vishia.mainCmd;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**Simple adapter between given Appendable to required PrintStream.
 * It is especially to set 
 * <pre>
 * System.setOut(new PrintStreamAdapter("text at line begin", myAppendable));
 * </pre>
 * @author hartmut
 *
 */
public class PrintStreamAdapter extends PrintStream
{
     /**This class is used for all outputs to the {@link PrintStreamAdapter} which are not
   * gathered by the overridden methods of PrintStream.
   * There should not be such methods. Therefore the write-methods is not called.
   * But the outStream should not be empty.
   * 
   */
  final static class OutStream extends OutputStream{
    
    final Appendable out;
    
    OutStream(Appendable out){
      this.out = out;
    }
    
    @Override
    public void write(int b) throws IOException
    { out.append((char)b);
    }
  }; //outStream 
  

  
   final String pre;
    
   final Appendable out;
    //StringBuilder uLine = new StringBuilder();
    
    public PrintStreamAdapter(String pre, Appendable out) {
      super(new OutStream(out));
      this.out = out;
      this.pre = pre;
    }
    
    
    @Override public void print(String str){
      append(str);
    }
    
    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
      CharSequence cs = (csq == null ? "null" : csq);
      append(cs.subSequence(start, end).toString());
      return this;
    }

    
    /**This method is called if {@link java.io.OutputStream#append} was invoked
     * @see java.io.PrintStream#print(java.lang.String)
     */
    @Override public PrintStream append(CharSequence s) { 
      try{ out.append(s); } catch (IOException exc){}
      return this;
    }
    
    /**The println method is used usually. 
     * 
     * @see java.io.PrintStream#println(java.lang.String)
     */
    @Override public void println(Object o) { 
      try{ out.append(o.toString()).append("\n"); } catch (IOException exc){}
    }

    
    /**The println method is used usually. 
     * 
     * @see java.io.PrintStream#println(java.lang.String)
     */
    @Override public void println(String s) { 
      try{ out.append(s).append("\n"); } catch (IOException exc){}
    }

    @Override public PrintStream printf(String s, Object... args) { 
      try{ out.append(s).append("TODO args").append("\n"); } catch (IOException exc){}
      return this;
    }
    
  
}
