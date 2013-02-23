package org.vishia.msgDispatch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**This class adapts an PrintStream such as System.err to the LogMessage-System.
 * The first part of the stream is converted to a message ident number. This allows especially the redirection of
 * outputs to {@link System#err} or {@link System#out} to the message system.
 * <br><br>
 * All characters from the output string till a semicolon or colon are used to build a message number from this text.
 * The {@link LogMessage} interface needs a number to identify the message, the {@link org.vishia.msgDispatch.MsgDispatcher} 
 * needs numbers to dispatch. If any identification string is used the first time, a number is created automatically.
 * If the same start text is used a second one (especially if an output was invoked a second time), the start text
 * is identified and the same number is used. One can sort messages with that number.
 * <br><br>
 * This first part is divide into 2 divisions: before and after a " - " to build message number ranges.
 * <br><br>
 * One should write all outputs in the form:
 * <pre>
 * System.err.println("Source of message - short message; some additional " + information);
 * </pre> 
 * This is a form which may be proper outside this class too. One should inform about the source of the message, 
 * then what's happen, then some more information. If one uses a semicolon as separator, it's able to present such messages
 * for example in an Excel sheet.
 * <br>
 * The first part of the message string is used to build a number, whereby a number range (group) is determined by the left division
 * before a " - " or '-'. Anytime if a message with the same first part is sent, the same number will be associated. 
 * Only at first time a number will be created.
 * <br>
 * Examples for separation first division (group):
 * <pre>
 * "Source - second division"
 * "Source-specification - second division"
 * "Source-second division"
 * </pre>
 * One should write " - " (with spaces left and right). This is used as separator. Only if a " - " is not found, a simple
 * '-' character is used as separator. If no separator is found, it is a non grouped message. One should use groups because
 * the message dispatcher can deal with ranges.
 * @author Hartmut Schorrig
 *
 */
public class MsgPrintStream
{
  /**Version, history and license.
   * <ul>
   * <li>2013-01-26 Hartmut new: {@link #setMsgGroupIdent(String, int, int)} to handle message dispatching 
   *   with known or probably texts.
   * <li>2013-01-26 Hartmut chg: Limitation of ident numbers to its range.   
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
  //@SuppressWarnings("hiding")
  public static final int version = 20130126;

  private final LogMessage logOut;
  
  private final AtomicInteger nextIdent;
  
  private final int identLast;
  
  private final AtomicInteger nextGroupIdent;
  
  private final int identGroupLast;

  private final int zGroup;
  
  private static class GroupIdent {
    final int identGroup, identLast;
    AtomicInteger nextIdentInGroup;
    
    GroupIdent(int identGroup, int identLast){
      this.identGroup = identGroup;
      this.identLast = identLast;
      nextIdentInGroup = new AtomicInteger(identGroup +1);
    }
  } //class GroupIdent
  
  /**Map of all Strings to an message ident number.
   */
  private final Map<String, Integer> idxIdent = new TreeMap<String, Integer>();
  
  private final Map<String, GroupIdent> idxGroupIdent = new TreeMap<String, GroupIdent>();
  
  private final PrintStream printStreamLog;

  
  
  /**Constructs.
   * Example:
   * <pre>
   * MsgPrintStream myPrintStream = new MsgPrintStream(myMsgDispatcher, 10000, 5000, 100);
   * </pre>
   * In this example the range from 1 to 9999 is used for messages which have a defined number outside of this class.
   * Any not numbered message is assigned in the range from 10000 till 149999 if it hasn't a group division,
   * and in groups with the start numbers 15000, 15100 etc. (till 32699 there are 176 groups). Note that the range 
   * which can be used is the positive integer range from 0 to about 2000000000. It may be better able to read to have
   * not so large numbers, but there are able to use. Note that you would not have so far different message texts.
   * 
   * @param logOut The output where all messages are written after identifier number building. 
   *   The {@link org.vishia.msgDispatch.MsgDispatcher} instance is proper to use.
   * @param identStart The first automatically created number used for non grouped message.
   *   Note that other messages have a known fixed number. Use a separate range in the positiv Integers range.   
   * @param sizeNoGroup number of the non grouped message identifiers. After this range the grouped messages starts. 
   *   Use a high enough number such as 1000 or 10000 or more.
   * @param sizeGroup Size of any group. Use a middle-high enough number such as 100 or 1000. 
   *   There won't be more as 1000 several message texts in one group - usually. 
   */
  public MsgPrintStream(LogMessage logOut, int identStart, int sizeNoGroup, int sizeGroup) {
    this.logOut = logOut;
    this.nextIdent = new AtomicInteger(identStart);
    this.identLast = identStart + sizeNoGroup -1;
    this.nextGroupIdent = new AtomicInteger(identStart + sizeNoGroup);
    this.identGroupLast = Integer.MAX_VALUE - sizeGroup;

    this.zGroup = sizeGroup;
    printStreamLog = new PrintStreamAdapter();
    //outStream = System.err;  //all not translated outputs
    //System.setErr(printStreamLog);  //redirect all System.err.println to the MsgDispatcher or the other given logOut
  }
  
  
  
  
  
  /**Returns the PrintStream which converts and redirects the output String to the given LogMessage output.
   * One can invoke:
   * <pre>
   * System.setErr(myMsgPrintStream.getPrintStreamLog);
   * </pre>
   * Then any output to System.err.println("text") will be redirected.
   * @return
   */
  public PrintStream getPrintStreamLog(){ return printStreamLog; }


  
  
  /**Creates a group with the given text and the given ident number range.
   * For this group the numbers inside te group are created automatically, but the group is disposed in a defined 
   * fix range. It have to be outside of the range given by constructor.
   * <br>
   * The method is proper to use if one knows some message texts and one is attempt to dispatch it.
   *  
   * @param msg The text till the exclusive " - " separation.
   * @param nrStart The first number of the group
   * @param nrLast The last number of the group.
   */
  public void setMsgGroupIdent(String msg, int nrStart, int nrLast){
      GroupIdent grpIdent = idxGroupIdent.get(msg);
      if(grpIdent ==null){
        grpIdent= new GroupIdent(nrStart, nrLast);
      }
  }
  
  
  private GroupIdent getMsgGroupIdent(String msg){
    GroupIdent grpIdent = idxGroupIdent.get(msg);
    if(grpIdent == null){
      int nextGrpIdent;
      int catastrophicCount = 0;
      int nextGrpIdent1;
      do{
        if(++catastrophicCount > 10000) throw new IllegalArgumentException("Atomic");
        nextGrpIdent = nextGroupIdent.get();
        nextGrpIdent1 = nextGrpIdent < identGroupLast ? nextGrpIdent + zGroup : nextGrpIdent;
      } while( !nextGroupIdent.compareAndSet(nextGrpIdent, nextGrpIdent1));
      grpIdent = new GroupIdent(nextGrpIdent, nextGrpIdent1-1);
      idxGroupIdent.put(msg, grpIdent);
    }
    return grpIdent;
  }
  
  
  
  
  private void convertToMsg(String s, Object... args) {
    int posSemicolon = s.indexOf(';');
    int posColon = s.indexOf(':');
    int posSep = posColon < 0 || posSemicolon < posColon ? posSemicolon : posColon;  //more left char of ; :
    final String sIdent;
    if(posSep >0){ sIdent = s.substring(0, posSep); }
    else { sIdent = s; }
    
    Integer nIdent = idxIdent.get(sIdent);
    if(nIdent == null){
      int nIdent1;
      int posGrp = sIdent.indexOf(" - ");
      if(posGrp < 0){
        posGrp =sIdent.indexOf('-');
      }
      
      if(posGrp >0){
        String sGrp = sIdent.substring(0, posGrp).trim();
        GroupIdent grpIdent = getMsgGroupIdent(sGrp);
        int catastrophicCount = 0;
        int nextIdentInGroup1;
        do{
          if(++catastrophicCount > 10000) throw new IllegalArgumentException("Atomic");
          nIdent1 = grpIdent.nextIdentInGroup.get();
          //the nextIdentInGroup is the current one used yet. Increment the nextIdentInGroup if admissible: 
          nextIdentInGroup1 = nIdent1 < grpIdent.identLast ? nIdent1 +1 : nIdent1;
        } while( !grpIdent.nextIdentInGroup.compareAndSet(nIdent1, nextIdentInGroup1));
      } else {  //no group 
        int catastrophicCount = 0;
        int nextIdent1;
        do{
          if(++catastrophicCount > 10000) throw new IllegalArgumentException("Atomic");
          nIdent1 = nextIdent.get();
          nextIdent1 = nIdent1 < identLast ? nIdent1 +1 : nIdent1;
        } while( !nextIdent.compareAndSet(nIdent1, nIdent1 + 1));
        
      }
      nIdent = new Integer(nIdent1);
      idxIdent.put(sIdent, nIdent);
    } //nIdent == null
    logOut.sendMsg(nIdent, s, args);
  }

  
  
  /**This class is used for all outputs to the {@link PrintStreamAdapter} which are not
   * gathered by the overridden methods of PrintStream.
   * There should not be such methods. Therefore the write-methods is not called.
   * But the outStream should not be empty.
   * 
   */
  private final OutputStream outStream = new OutputStream() {
    
    @Override
    public void write(int b) throws IOException
    {
    }
  }; //outStream 
  
  
  private class PrintStreamAdapter extends PrintStream {
    
    
    PrintStreamAdapter() {
      super(outStream);
    }
    
    
    /**This method from PrintStream is invoked if print(String), println(String), printf(String, args)
     * or format(String, args) is invoked from the PrintStream. Only this method have to be override
     * to implement the msg dispatching functionality.
     * 
     * @see java.io.PrintStream#print(java.lang.String)
     */
    @Override public void print(String s) { convertToMsg(s); }
    
    /**The println method is used usually. 
     * 
     * @see java.io.PrintStream#println(java.lang.String)
     */
    @Override public void println(String s) { convertToMsg(s); }

    @Override public PrintStream printf(String s, Object... args) { 
      convertToMsg(s, args); return this; 
    }
    
  } //class PrintStreamAdapter
  
}
