package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.DataAccess;
import org.vishia.util.StringPart;


/**This class stores some prepared commands. The input of the store is a file, 
 * which contains the assembling of command lines maybe with placeholder for files.
 * <br><br>
 * Simple UML diagram:
 * <pre>
 * 
 * CmdStore ------*> PrepareCmd
 * </pre>
 * (See {@link org.vishia.util.Docu_UML_simpleNotation})
 * 
 * @author Hartmut Schorrig
 *
 */
public class CmdStore
{

  /**Version, history and license.
   * <ul>
   * <li>2013-09-08 Hartmut chg: {@link #addSubOfJZcmdClass(org.vishia.cmd.JZcmdScript.JZcmdClass, int)} now public
   *   because readCmdCfg(...) removed to {@link org.vishia.commander.FcmdExecuter}. It has dependencies
   *   to the Zbnf package {@link org.vishia.zcmd.JZcmd} which is not visible in this component by standalone compilation.
   *   The problem is: The {@link JZcmdScript} is visible here, but the used translator for the JZcmdScript needs ZBNF 
   * <li>2013-09-08 Hartmut new: {@link CmdBlock#zgenSub} may replace the {@link CmdBlock#listBlockCmds}
   *   and may replace the {@link PrepareCmd} in future, first test. 
   * <li>2012-02-19 Hartmut chg: {@link #readCmdCfg(File)} accepts $ENV, comment lines with // and #
   *   and start of command not with spaces on line start.
   * <li>2011-12-31 Hartmut chg {@link CmdBlock#title} is new, the syntax of configfile is changed.
   *   This class is used to capture all executables for a specified extension for The.file.Commander 
   * </ul>
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
   */
  public static int version = 20120609;
  
  /**Description of one command.
   */
  public static class CmdBlock
  {
    /**The identification for user in the selection list. */
    public String name;
  
    /**The title of the cmd, for selection. It is the part after ':' in the title line:
     * <pre>
     * ==name: title==
     * </pre>
     * */
    public String title;
    
    public final int level;
    
    /**Some commands of this block. */
    public final List<PrepareCmd> listBlockCmds = new LinkedList<PrepareCmd>();

    /**Any JZcmd subroutine which should be invoked instead of the {@link #listBlockCmds}. */
    protected final JZcmdScript.Subroutine zgenSub;
    
    
    /**Contains all commands read from the configuration file in the read order. */
    protected List<CmdBlock> listSubCmds;

    
    public CmdBlock(){
      zgenSub = null;
      this.level = 1;
    }
    
    public CmdBlock(JZcmdScript.Subroutine jbatSub, int level){
      this.zgenSub = jbatSub;
      this.level = level;
      this.name = jbatSub.name;
    }
    
    /**Assembles the arguments for JZcmd subroutine call.
     * The arguments are determined by the {@link JZcmdScript.Argument#identArgJbat}
     * @param getterFiles Access to given files.
     * @return Variable container with the requeste arguments.
     * @throws IllegalAccessException 
     */
    public Map<String, DataAccess.Variable<Object>> getArguments(CmdGetFileArgs_ifc getterFiles) {
      if(zgenSub !=null){
        getterFiles.prepareFileSelection();
        Map<String, DataAccess.Variable<Object>> args = new TreeMap<String, DataAccess.Variable<Object>>();
        if(zgenSub.formalArgs !=null) {
          try{
            for(JZcmdScript.DefVariable arg :zgenSub.formalArgs){
              String name1 = arg.getVariableIdent();
              if(name1.equals("file1")){ DataAccess.createOrReplaceVariable(args, "file1", 'O', getterFiles.getFile1(), true); }
              else if(name1.equals("file2")){ DataAccess.createOrReplaceVariable(args, "file2", 'O', getterFiles.getFile2(), true); }
              else if(name1.equals("file3")){ DataAccess.createOrReplaceVariable(args, "file3", 'O', getterFiles.getFile3(), true); }
              else if(name1.equals("dir1")){ DataAccess.createOrReplaceVariable(args, "dir1", 'O', getterFiles.getFile1().getParentFile(), true); }
              else if(name1.equals("dir2")){ DataAccess.createOrReplaceVariable(args, "dir2", 'O', getterFiles.getFile2().getParentFile(), true); }
              else if(name1.equals("dir3")){ DataAccess.createOrReplaceVariable(args, "dir3", 'O', getterFiles.getFile3().getParentFile(), true); }
            }
          } catch(IllegalAccessException exc){
            //It is not able to expect that file1 etc. are not accessible.
            throw new IllegalArgumentException(exc);
          }
        }
        return args;
      } else {
        return null;
      }
    }
    
    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command */
    public PrepareCmd new_cmd(){ return new PrepareCmd(); }
    
    
    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command */
    public void add_cmd(PrepareCmd cmd)
    { cmd.prepareListCmdReplace();
      listBlockCmds.add(cmd); 
      
    }
    
    /**Returns all commands which are contained in this CmdBlock. */
    public final List<PrepareCmd> getCmds(){ return listBlockCmds; }
    
    @Override public String toString(){ return name + listBlockCmds; }
    
  }
  
  /**Contains all commands read from the configuration file in the read order. */
  private final List<CmdBlock> listCmds = new LinkedList<CmdBlock>();

  /**Contains all commands read from the configuration file in the read order. */
  private final Map<String, CmdBlock> idxCmd = new TreeMap<String, CmdBlock>();

  private final String XXXsyntaxCmd = "Cmds::={ <cmd> }\\e. "
    + "cmd::= <* :?name> : { <*\\n?cmd> \\n } ."; 

  
  public CmdStore()
  {
  }
  
  
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command block */
  public CmdBlock new_CmdBlock(){ return new CmdBlock(); }
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command block. */
  public void add_CmdBlock(CmdBlock value){ listCmds.add(value); idxCmd.put(value.name, value); }

  
  
  
  public void addSubOfJZcmdClass(JZcmdScript.JZcmdClass jzcmdClass, int level){
    if(jzcmdClass.classes !=null) for(JZcmdScript.JZcmdClass zgenClassSub : jzcmdClass.classes){
      CmdBlock cmdBlock = new CmdBlock();
      listCmds.add(cmdBlock); 
      cmdBlock.name = zgenClassSub.cmpnName;
      //cmdBlock.listSubCmds = new ArrayList<CmdBlock>();
      //addSubOfZgenclass(cmdBlock.listSubCmds, zgenClassSub, level+1);
      addSubOfJZcmdClass(zgenClassSub, level+1);
    }
    if(jzcmdClass.subroutines !=null) for(Map.Entry<String, JZcmdScript.Subroutine> e: jzcmdClass.subroutines.entrySet()){
      JZcmdScript.Subroutine subRoutine = e.getValue();
      if(!subRoutine.name.startsWith("_")) { //ignore internal subroutines!
        CmdBlock cmdBlock = new CmdBlock(subRoutine, level);
        listCmds.add(cmdBlock); 
        idxCmd.put(cmdBlock.name, cmdBlock);
      }
    }
  }
  
  
  
  
  
  public String readCmdCfgOld(File cfgFile)
  { 
    //if(cfgFile.getName().endsWith(".jbat.cfg")){
    //  return readCmdCfgJbat(cfgFile, null);
    //}
    String sError = null;
    BufferedReader reader = null;
    try{
      reader = new BufferedReader(new FileReader(cfgFile));
    } catch(FileNotFoundException exc){ sError = "CommandSelector - cfg file not found; " + cfgFile; }
    if(reader !=null){
      CmdBlock actBlock = null;
      listCmds.clear();
      String sLine;
      StringPart spLine = new StringPart();
      StringBuilder uLine = new StringBuilder(1000);
      try{ 
        while( (sLine = reader.readLine()) !=null){
          if(sLine.contains("$")){
            uLine.setLength(0);
            uLine.append(sLine.trim());
            spLine.assignReplaceEnv(uLine);
            sLine = uLine.toString();
          } else {
            sLine = sLine.trim();
            spLine.assign(sLine);
          }
          if(sLine.length() ==0){
            
          } else if( sLine.startsWith("==")){
            final int posColon = sLine.indexOf(':');
            final int posEnd = sLine.indexOf("==", 2);  
            //a new command block
            if(actBlock !=null){ add_CmdBlock(actBlock); }  //the last one. 
            actBlock = new_CmdBlock();
            if(posColon >=0 && posColon < posEnd){
              actBlock.name = sLine.substring(2, posColon).trim();
              actBlock.title = sLine.substring(posColon+1, posEnd).trim();
            } else {
              actBlock.name = sLine.substring(2, posEnd).trim();
              actBlock.title = "";
            }
          } else if(sLine.startsWith("@")){
          } else if(sLine.startsWith("//")){
          } else if(sLine.startsWith("#")){
                  
          } else { // if(sLine.startsWith(" ")){  //a command line
            PrepareCmd cmd = actBlock.new_cmd();
            cmd.set_cmd(sLine.trim());
            //cmd.prepareListCmdReplace();
            actBlock.add_cmd(cmd);
          }      
        }
        if(actBlock !=null){ add_CmdBlock(actBlock); } 
      } 
      catch(IOException exc){ sError = "CommandStore - cfg file read error; " + cfgFile; }
      catch(IllegalArgumentException exc){ sError = "CommandStore - cfg file error; " + cfgFile + "; line: " + spLine + "; msg:"+ exc.getMessage(); }
      if(reader !=null){ try{ reader.close();} catch(IOException exc){}}
    }
    return sError;
  }
  
  
  /**Gets a named command
   * @param name The name given in configuration file
   * @return The prepared CmdBlock or null if not found.
   */
  public CmdBlock getCmd(String name){ return idxCmd.get(name); }
  
  /**Gets a contained commands for example to present in a selection list.
   * @return The list.
   */
  public final List<CmdBlock> getListCmds(){ return listCmds; }
  
}
