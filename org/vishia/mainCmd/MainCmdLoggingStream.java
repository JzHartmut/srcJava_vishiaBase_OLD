package org.vishia.mainCmd;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.util.Assert;

/**This class is a mainCmd logging output with adequate features as the {@link MainCmd} class,
 * but independent of that. It is intent to use if the logging features are other than in the 
 * main routine given, or if {@link MainCmd} is not used for the originally invocation of a Java program.
 * Especially it can be used for logging output inside a {@link org.vishia.cmd.JZcmdExecuter} for some
 * special routines.
 * @author Hartmut Schorrig
 *
 */
public class MainCmdLoggingStream implements MainCmdLogging_ifc
{

  /**Stream to output all logging information. */
  protected OutputStream out;
  
  private final StringBuilder u = new StringBuilder();
  
  final private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
  
  private static String sLeft = "                                                                ";
  
  
  
  /**This variable determines which level is output. See {@link MainCmdLogging_ifc#fineDebug} etc.*/
  int reportLevel = MainCmdLogging_ifc.debug;
  
  /**Create with given output stream, or empty.
   * @param out may be null, use {@link #openReportfile(String, boolean)} than.
   */
  public MainCmdLoggingStream(OutputStream out){
    this.out = out;
  }
  
  
  /**Create with given output stream, or empty.
   * @param out may be null, use {@link #openReportfile(String, boolean)} than.
   */
  public MainCmdLoggingStream(OutputStream out, int reportLevel){
    this.out = out;
    this.reportLevel = reportLevel;
  }
  
  
  @Override
  public void flushReport()
  { flush();
  }

  /**Returns 0
   * @see org.vishia.mainCmd.MainCmdLogging_ifc#getExitErrorLevel()
   */
  @Override
  public int getExitErrorLevel()
  { return 0;
  }

  @Override
  public int getReportLevel()
  { return reportLevel;
  }

  @Override
  public int getReportLevelFromIdent(int ident)
  {
    return ident <= MainCmdLogging_ifc.fineDebug ? ident : MainCmdLogging_ifc.info;
  }

  @Override
  public void openReportfile(String sFileReport, boolean bAppendReport)
      throws FileNotFoundException
  {
    out = new FileOutputStream(sFileReport);
  }

  @Override
  public void report(int nLevel, String string)
  { if( (nLevel & mReportLevel) <= reportLevel)
    { u.append(string);
    }
     
  }

  
  void writeBuffer(){
    try{ out.write(u.toString().getBytes()); }
    catch(IOException exc){
      System.err.println("MainCmdLoggingStream - IOException;");
    }
    u.setLength(0);
  }
  
  
  void writeln(int level, int nLeftMargin, CharSequence ss){
    if(u.length() >0){
      for(int i = 0; i < u.length(); ++i){
        if(u.charAt(i) < 0x20){
          u.setCharAt(i, '|');
        }
      }
      u.append("\n"); 
      writeBuffer(); 
    }
    u.append(dateFormat.format(new Date(System.currentTimeMillis())));
    u.append(level).append(": ").append(sLeft.subSequence(0, nLeftMargin)).append(ss);
  }
  
  
  @Override
  public void report(String sText, Throwable exception)
  { CharSequence ctext = Assert.exceptionInfo(sText, exception, 0, 20);
    writeln(MainCmdLogging_ifc.error, 0, ctext);
  }

  @Override
  public void reportln(int nLevel, int nLeftMargin, String string)
  {
    if( (nLevel & mReportLevel) <= reportLevel)
    { //writes also an error as info on display.
      writeln(nLevel, nLeftMargin, string);
    }  
  }

  @Override
  public void reportln(int nLevel, String string)
  { reportln(nLevel, 0 , string);
  }

  @Override
  public void setExitErrorLevel(int level)
  { 
  }

  @Override
  public void setOutputChannels(Appendable outP, Appendable errP)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setReportLevelToIdent(int ident, int nLevelActive)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeError(String sError)
  { reportln(MainCmdLogging_ifc.error, sError); 
  }

  @Override
  public void writeError(String sError, Throwable exception)
  { report(sError, exception); 
  }

  @Override
  public void writeInfo(String sInfo)
  { report(MainCmdLogging_ifc.info, sInfo); 
  }

  @Override
  public void writeInfoln(String sInfo)
  { reportln(MainCmdLogging_ifc.info, sInfo); 
  }

  @Override
  public void writeStackTrace(Exception exc)
  { exc.printStackTrace(new PrintStream(out, true));
  }

  @Override
  public void writeWarning(String sError)
  { reportln(MainCmdLogging_ifc.warning, sError); 
  }

  @Override
  public void close()
  {
    if(u.length() >0){ u.append("\n"); writeBuffer(); }
  }

  @Override
  public void flush()
  { if(u.length() >0){ writeBuffer(); }
    try{ out.flush();
    } catch(IOException exc){}
  }
  
  @Override
  public boolean isOnline()
  { return out !=null;
  }

  @Override
  public boolean sendMsg(int identNumber, String text, Object... args)
  { return sendMsgTime(identNumber, new OS_TimeStamp(System.currentTimeMillis()), text, args);
  }

  @Override
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args)
  {
    String sline = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args);
    try{ 
      out.write(sline.getBytes());
      out.write('\n');
    }
    catch(IOException exc){ }
    return true;
  }

  @Override
  public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args)
  { String line = "?";
    try{
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.get());
    } catch(Exception exc){
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + text;
    }
      try{ 
      out.write(line.getBytes()); 
      out.write('\n');
    }
    catch(Exception exc){ 
    }
    return true;
  }
}
  
