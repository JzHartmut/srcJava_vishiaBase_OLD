package org.vishia.fileRemote;

import org.vishia.states.StateComposite;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple;

public class FileRemoteCopy extends StateMachine
{
  
  FileRemoteCopy()
  {
    super();
  }
  
  
  
  
  
  
  class Ready extends StateSimple
  {
    
  }
  
  
  class Process extends StateComposite {
    
    class DirOrFile extends StateSimple {
      
    }
    
    class Subdir extends StateSimple {
      
    }
    
    class CopyFileContent extends StateSimple {
      
    }
    
    class NextFile extends StateSimple {
      
    }
    
    class Ask extends StateSimple {
      
    }
    
    
  }
  
}
