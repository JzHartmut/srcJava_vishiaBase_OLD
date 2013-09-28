package org.vishia.fileRemote;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.vishia.event.Event;
import org.vishia.fileLocalAccessor.FileRemoteAccessorLocalFile;
import org.vishia.fileRemote.FileAccessZip.FileZipData;

/**Interface for instances, which organizes a remote access to files.
 * One instance per transfer protocol are need.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface FileRemoteAccessor extends Closeable
{
  /**Version, history and license.
   * <ul>
   * <li>2012-09-14 Hartmut new: {@link CallbackFile}, {@link #walkFileTree(FileRemote, FileFilter, int, CallbackFile)}. 
   * <li>2012-08-12 Hartmut chg: Now it is an interface, not an abstract class, only formal.
   * <li>2012-08-12 Hartmut new: {@link #setLastModified(FileRemote, long)}. 
   * <li>2012-08-12 Hartmut bugfix: {@link #getChildren(FileRemote, FileFilter)} here only abstract.
   * <li>2012-08-12 Hartmut new: {@link #openInputStream(FileRemote, long)}
   * <li>2012-08-12 Hartmut new: {@link #getChildren(FileRemote, FileFilter)} implemented here.
   * <li>2012-08-12 Hartmut chg: up to now this is not an interface but an abstract class. It contains common method implementation.
   *   An derivation (or implementation of the interface before that change) may not need other base classes.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: {@link Commission#newDate} etc. 
   *   for {@link FileRemote#chgProps(String, int, int, long, org.vishia.fileRemote.FileRemote.CallbackEvent)}.
   * <li>2012-01-09 Hartmut new: This class extends from Closeable, because an implementation 
   *  may have an running thread which is need to close. A device should be closeable any time.
   * <li>2012-01-06 Hartmut new {@link #refreshFileProperties(FileRemote)}. 
   * <li>2011-12-31 Hartmut new {@link Commission} and {@link #addCommission(Commission)}. It is used
   *   to add commissions to the implementation class to do in another thread/via communication.
   * <li>2011-12-10 Hartmut creation: Firstly only the {@link FileRemoteAccessorLocalFile} is written.
   *   
   * </ul>
   * <br><br>
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
   */
  public static final int version = 20120310;
  
  //public final static int kOperation = 0xd00000, kFinishOk = 0xf10000, kFinishNok = 0xf10001
  //, kFinishError = 0xf1e3303, kNrofFilesAndBytes = 0xd00001, kCopyDir = 0xd0cd13;

  
  
  
  /**Gets the properties of the file from the physical file.
   * @param file the destination file object.
   * @param callback If null then the method waits for response from the maybe remote file system
   *   with a suitable timeout. 
   *   If not null then the method may return immediately without any waiting
   *   and the callback method in the {@link Event#callback()} is invoked maybe in another thread
   *   if the answer is gotten. 
   */
  public abstract void refreshFileProperties(FileRemote file, FileRemote.CallbackEvent callback);

  /**Gets the properties and the children of the file from the physical file.
   * <br><br>
   * The properties of the children are not gotten for the Standard-PC-Filesystem using Java-6. 
   * It may use too many calculation time.
   * Use {@link #refreshFileProperties(FileRemote, org.vishia.fileRemote.FileRemote.CallbackEvent)}
   * in a loop for any file only if it is necessary. Check the {@link FileRemote#isTested(long)} therefore.
   * <br><br>
   * For Java-7 the main properties of all children are gotten too, because the system call deliver it.
   * To document that, the {@link FileRemote#timeRefresh} is set to the new time.
   * For other remote file systems it should be also do so. Usual the main properties of the children
   * should be present if the name of the children is gotten. The main properties are length, 
   * timestamp last modified, read or write able.
   * 
   * @param file the destination file object.
   * @param callback If null then the method waits for response from the maybe remote file system
   *   with a suitable timeout. 
   *   If not null then the method may return immediately without any waiting
   *   and the callback method in the {@link Event#callback()} is invoked maybe in another thread
   *   if the answer is gotten. 
   */
  public abstract void refreshFilePropertiesAndChildren(FileRemote file, FileRemote.CallbackEvent callback);

  
  public abstract List<File> getChildren(FileRemote file, FileFilter filter);
  
  /**Walks through all children of the given file and call the {@link CallbackFile#offerFile(FileRemote)}
   * for any file. 
   * The callback is done in another thread, because it may be a result of communication.
   * This routine does not wait for finish. The last call {@link CallbackFile#finished()}
   * is the last action of that.
   *  
   * @param file The start file. With it {@link CallbackFile#start()} will be called.
   * @param filter Any filter which files will be accepted.
   * @param depth depth, at least 1 should be use.
   * @param callback The callback instance for any file.
   */
  public void walkFileTree(FileRemote file, FileFilter filter, int depth, CallbackFile callback);
  

  boolean setLastModified(FileRemote file, long time);
  
  
  public abstract boolean createNewFile(FileRemote file, FileRemote.CallbackEvent callback) throws IOException;

  /**Try to delete the file.
   * @param callback
   * @return If the callback is null, the method returns if the file is deleted or it can't be deleted.
   *   The it returns true if the file is deleted successfully. If the callback is not null, it returns true.
   */
  public abstract boolean delete(FileRemote file, FileRemote.CallbackEvent callback);
  
  public abstract boolean mkdir(FileRemote file, boolean subdirs, FileRemote.CallbackEvent callback);
  
  public abstract ReadableByteChannel openRead(FileRemote file, long passPhase);
  
  public abstract InputStream openInputStream(FileRemote file, long passPhase);
  
  public abstract WritableByteChannel openWrite(FileRemote file, long passPhase);
 
  //FileRemote[] listFiles(FileRemote parent);
  
  /**Creates or prepares a CmdEvent to send to the correct destination. The event is ready to use but not  occupied yet. 
   * If the evBack contains a CmdEvent as its opponent, it is used. In that way a non-dynamic event management
   * is possible. */
  public abstract FileRemote.CmdEvent prepareCmdEvent(Event<?, FileRemote.Cmd> evBack);

  
  public abstract boolean isLocalFileSystem();

  /**This interface is used as callback for {@link FileRemoteAccessor#getChildren(FileRemote, FileFilter)}
   */
  public interface CallbackFile
  {
    /**It is similar {@link java.nio.file.FileVisitResult}.
     * This class is defined here because it runs with Java-6 too.
     */
    public enum Result
    {
      cont
      , terminate
      , skipSiblings
      , skipSubtree
    }

    /**Invoked before start of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
     * or an adequate communication.
     */
    void start();
    
    /**Invoked for any directory.
     * It is invoked in the thread which executes a FileRemote action.
     * It is possible to create an event and store it in a queue but there are necessary some more events
     * it may not be good.
     * @param file
     * @return TODO information to abort, maybe boolean.
     */
    Result offerDir(FileRemote file);
    
    /**Invoked on end of walking through a directory.
     * It is invoked in the thread which executes a FileRemote action.
     * It is possible to create an event and store it in a queue but there are necessary some more events
     * it may not be good.
     * @param file
     * @return TODO information to abort, maybe boolean.
     */
    Result finishedDir(FileRemote file);
    
    /**Invoked for any file of the directory.
     * It is invoked in the thread which executes a FileRemote action.
     * It is possible to create an event and store it in a queue but there are necessary some more events
     * it may not be good.
     * @param file
     * @return TODO information to abort, maybe boolean.
     */
    Result offerFile(FileRemote file);
    
    /**Invoked after finishing a {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
     */
    void finished();
  }

  
  /**Use this as template for anonymous implementation. Frequently without 'static'.*/
  static FileRemoteAccessor.CallbackFile callbackTemplate = new FileRemoteAccessor.CallbackFile()
  {

    @Override public void start() {  }
    @Override public void finished() {  }

    @Override public Result offerDir(FileRemote file) {
      return Result.cont;      
    }
    
    @Override public Result finishedDir(FileRemote file) {
      return Result.cont;      
    }
    
    

    @Override public Result offerFile(FileRemote file) {
      return Result.cont;
    }

    
  };
  
  
  


}
