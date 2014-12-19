package org.vishia.fileRemote;

import java.io.FileFilter;


/**This interface is used as callback for {@link FileRemoteAccessor#getChildren(FileRemote, FileFilter)}
 * It is similar like the concept of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)}
 * with its visitor interface. But it is implemented for Java6-usage too.
 */
public interface FileRemoteCallback
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
  
  /**Invoked on start on walking through a directory.
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
  
  /**Invoked for any file or sub directory.
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
  
  
  /**Returns true if the file tree walking should be terminated respectively aborted.
   * This routine should be called in any walk tree routine.
   * The aborted state should be set from outside in the implementation classes.
   * @return true if should terminated.
   */
  boolean shouldAborted();

  
  
  /**Use this as template for anonymous implementation. Frequently without 'static'.*/
  static FileRemoteCallback callbackTemplate = new FileRemoteCallback()
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

    
    @Override public boolean shouldAborted(){
      return false;
    }
    
  };
  
  

  
}
