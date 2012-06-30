package org.vishia.msgDispatch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**This class adapts an PrintStream to the LogMessage-System.
 * The first part of the stream is converted to a message ident number.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgPrintStream
{
  /**Version, history and license.
   * <ul>
   * <li>2012-07-01 chg Hartmut: Now doesn't change System.err, but supplies {@link #getPrintStreamLog()}
   *   to set System.setOut(...) and System.setErr(...). 
   * <li>2011-10-00 Hartmut created
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  @SuppressWarnings("hiding")
  public static final int version = 2012001;

  final LogMessage logOut;
  
  AtomicInteger nextIdent = new AtomicInteger(100);
  
  AtomicInteger nextGroupIdent = new AtomicInteger(10000);

  int zGroup = 1000;
  
  private static class GroupIdent {
    final int identGroup;
    AtomicInteger nextIdentInGroup;
    
    GroupIdent(int ident){
      identGroup = ident;
      nextIdentInGroup = new AtomicInteger(ident +1);
    }
  } //class GroupIdent
  
  /**Map of all Strings to an message ident number.
   */
  Map<String, Integer> idxIdent = new TreeMap<String, Integer>();
  
  Map<String, GroupIdent> idxGroupIdent = new TreeMap<String, GroupIdent>();
  
  
  
  
  OutputStream outStream = new OutputStream() {
    
    @Override
    public void write(int b) throws IOException
    {
      // TODO Auto-generated method stub
      
    }
  }; //outStream 
  
  
  class PrintStreamAdapter extends PrintStream {
    
    
    PrintStreamAdapter() {
      super(outStream);
    }
    
    
    /**This method from PrintStream is invoked if print(String), println(String), printf(String, args)
     * or format(String, args) is invoked from the PrintStream. Only this method have to be override
     * to implement the msg dispatching functionality.
     * 
     * @see java.io.PrintStream#print(java.lang.String)
     */
    @Override public void print(String s) {
      int posSemicolon = s.indexOf(';');
      int posColon = s.indexOf(':');
      int posSep = posColon < 0 || posSemicolon < posColon ? posSemicolon : posColon;  //more left char of ; :
      final String sIdent;
      if(posSep >0){ sIdent = s.substring(0, posSep); }
      else { sIdent = s; }
      
      Integer nIdent = idxIdent.get(sIdent);
      if(nIdent == null){
        int nIdent1;
        int posGrp = sIdent.indexOf('-');
        
        if(posGrp >0){
          String sGrp = sIdent.substring(0, posGrp).trim();
          GroupIdent grpIdent = idxGroupIdent.get(sGrp);
          if(grpIdent == null){
            int nextGrpIdent;
            int catastrophicCount = 0;
            do{
              if(++catastrophicCount > 10000) throw new IllegalArgumentException("Atomic");
              nextGrpIdent = nextGroupIdent.get();
            } while( !nextGroupIdent.compareAndSet(nextGrpIdent, nextGrpIdent + zGroup));
            grpIdent = new GroupIdent(nextGrpIdent);
            idxGroupIdent.put(sGrp, grpIdent);
          }
          int catastrophicCount = 0;
          do{
            if(++catastrophicCount > 10000) throw new IllegalArgumentException("Atomic");
            nIdent1 = grpIdent.nextIdentInGroup.get();
          } while( !grpIdent.nextIdentInGroup.compareAndSet(nIdent1, nIdent1 + 1));
        } else {  //no group 
          int catastrophicCount = 0;
          do{
            if(++catastrophicCount > 10000) throw new IllegalArgumentException("Atomic");
            nIdent1 = nextIdent.get();
          } while( !nextIdent.compareAndSet(nIdent1, nIdent1 + 1));
          
        }
        nIdent = new Integer(nIdent1);
        idxIdent.put(sIdent, nIdent);
      } //nIdent == null
      logOut.sendMsg(nIdent, s);
    }

    
  } //class PrintStreamAdapter
  
  PrintStream printStreamLog = new PrintStreamAdapter();

  
  
  public MsgPrintStream(LogMessage logOut) {
    this.logOut = logOut;
    outStream = System.err;  //all not translated outputs
    //System.setErr(printStreamLog);  //redirect all System.err.println to the MsgDispatcher or the other given logOut
  }
  
  public PrintStream getPrintStreamLog(){ return printStreamLog; }
}
