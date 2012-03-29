package org.vishia.msgDispatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**This class adapts an PrintStream to the LogMessage-System.
 * @author Hartmut Schorrig
 *
 */
public class MsgPrintStream
{
  
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
              if(++catastrophicCount > 10000) new IllegalArgumentException("Atomic");
              nextGrpIdent = nextGroupIdent.get();
            } while( !nextGroupIdent.compareAndSet(nextGrpIdent, nextGrpIdent + zGroup));
            grpIdent = new GroupIdent(nextGrpIdent);
            idxGroupIdent.put(sGrp, grpIdent);
          }
          int catastrophicCount = 0;
          do{
            if(++catastrophicCount > 10000) new IllegalArgumentException("Atomic");
            nIdent1 = grpIdent.nextIdentInGroup.get();
          } while( !grpIdent.nextIdentInGroup.compareAndSet(nIdent1, nIdent1 + 1));
        } else {  //no group 
          int catastrophicCount = 0;
          do{
            if(++catastrophicCount > 10000) new IllegalArgumentException("Atomic");
            nIdent1 = nextIdent.get();
          } while( nextIdent.compareAndSet(nIdent1, nIdent1 + 1));
          
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
    System.setErr(printStreamLog);  //redirect all System.err.println to the MsgDispatcher or the other given logOut
  }
  
  
}