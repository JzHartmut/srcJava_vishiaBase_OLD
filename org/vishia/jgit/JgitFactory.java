package org.vishia.jgit;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.vishia.util.JgitFactory_ifc;
import org.vishia.util.Jgit_ifc;

public class JgitFactory implements JgitFactory_ifc
{

  
  
  
  @Override public Jgit_ifc getRepository(String pathToRepository, String pathToWorkdir)
  {
    File fileArchive = new File(pathToRepository);
    FileRepositoryBuilder b = new FileRepositoryBuilder();
    b.setGitDir(fileArchive);
    b.setWorkTree(new File(pathToWorkdir));
    Repository repof;
    try {
      repof = b.build();
      //repof = new FileRepository(fileArchive);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return null;
    }
    Git repo = new Git(repof);
    JgitAccess repoA = new JgitAccess(repo, repof);
    return repoA;
  }
  

  
}
