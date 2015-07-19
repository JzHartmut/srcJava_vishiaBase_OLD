package org.vishia.util;

/**This is an interface to use org.eclipse.jgit to enable the access to that package. The reason for usage this interface is:
 * In an application the jGit may be present or not. If it is not present or another implementation should be used for git access,
 * the application must not depended on the org.eclipse.jgit package immediately.   
 * @author hartmut
 *
 */
public interface JgitFactory_ifc
{
  /**Creates the Jgit_ifc instance and returns the repository access.
   * @param pathToArchive Usual absolute path, maybe relative to current directory of the system (not recommended)
   * @return null if the pathToArchive does not refer a valid git archive on file system.
   */
  Jgit_ifc getRepository(String pathToRepository, String pathToWorkdir);

}
