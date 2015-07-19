package org.vishia.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.vishia.util.Jgit_ifc;

/**This class can be accessed via the interface {@link Jgit_ifc} if an application won't not depend on the org.eclipse.jgit implementation.
 * The user can access the {@link #repo} direct if one should work with the originally package. 
 * @author Hartmut Schorrig
 *
 */
public class JgitAccess implements Jgit_ifc
{
  public final Git repo;
  
  public final Repository repof;
  
  JgitAccess(Git repo, Repository repof){ this.repo = repo; this.repof = repof; }
  
  
  @Override public String status() 
  {
    try{ 
      Status status = repo.status().call();
      return status.toString();
    } catch(Exception exc){
      return exc.getMessage();
    }
  }
}
