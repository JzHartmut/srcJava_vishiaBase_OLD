package org.vishia.util;


/**This is a working interface to use org.eclipse.jgit. The reason for usage this interface is:
 * In an application the jGit may be present or not. If it is not present or another implementation should be used for git access,
 * the application must not depended on the org.eclipse.jgit package immediately.   
 * @author hartmut
 *
 */
public interface Jgit_ifc
{
  String status();
}
