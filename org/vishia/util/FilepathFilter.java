package org.vishia.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**This class helps to check whether a String given path and name matches to a mask String. 
 * It is similar to {@link java.io.FilenameFilter} or {@link java.io.FilenFilter} but deals with CharSequences exclusively.
 * It prepares a String given file mask (with wild cards) to select files.
 * It contains ready to used variables to check a file path.
 * @author hartmut
 *
 */
public class FilepathFilter implements FilenameFilter
{
  
  static class NameFilter
  {
  
    final String sStartName, sEndName, sContainName;
    
    final int posContainName, posContainNameEnd;
  
    NameFilter(String sMask) {
      int posAsteriskName = sMask.indexOf('*');
      int posAsteriskName2 = sMask.indexOf('*', posAsteriskName+1);
      if(posAsteriskName >=0) {
        sStartName = posAsteriskName >0 ? sMask.substring(posAsteriskName) : null;
        int posEnd = posAsteriskName2 >=0? posAsteriskName2 +1 : posAsteriskName +1; 
        sEndName = posEnd < sMask.length() ? sMask.substring(posEnd) : null;
        //null if "start**end" or "start*end"
        if(posAsteriskName2 < 0 || posAsteriskName2 == posAsteriskName+1) {
          posContainName = posContainNameEnd = -1;
          sContainName = null;
        } else {
          posContainName = posAsteriskName+1;
          posContainNameEnd = sMask.length() - posAsteriskName -1;
          sContainName = sMask.substring(posContainName, posAsteriskName2); 
        }
      } else {
        sStartName = sMask;
        sEndName = null;
        sContainName = null;
        posContainName = posContainNameEnd = -1;
      }
      
    }
    
    
    public boolean checkName(String name)
    { int posContain;
      boolean bSelected = (sEndName == null && (sStartName == null  || sStartName.equals(name))
            || ( sStartName == null || name.startsWith(sStartName)) && (sEndName == null || name.endsWith(sEndName))
            )
            && (sContainName == null || (posContain = name.indexOf(sContainName)) >= posContainName && posContain < name.length()-posContainNameEnd )
            ;
      return bSelected;
    }

  }
  
  final String sStartDir, sEndDir;
    
  /**One or more entries for checking the file name or null to accept all names.  */
  final List<NameFilter> nameFilter;
  
  /**One or more entries for checking excluding the file name or null to exlude nothing. */
  final List<NameFilter> excludeNameFilter;
  
  /**If set then the path contains path/before/** /path/after. It means more levels are accepted (like 'recursively') */
  final boolean bAllTree;   //yet package used (2015-09)
  
  public FilepathFilter(String sMask)
  {
    if(sMask == null){
      sStartDir = sEndDir = null;
      nameFilter = null;
      excludeNameFilter = null;
      bAllTree = false;
    } else {
      int posName = sMask.lastIndexOf('/') +1;  //0 if no / found.
      if(posName == 0) { 
        //an '/' was not found, only a name.
        sStartDir = sEndDir = null; 
        bAllTree = false;
      }
      else {
        int posAllTree = sMask.indexOf("**");
        if(posAllTree >= posName){ posAllTree = -1; } //not in the directory part.
        if(posAllTree >=0){
          bAllTree = true;
          sStartDir = posAllTree >0 ? sMask.substring(0, posAllTree) : null;
          int posEnd = posAllTree +2; 
          sEndDir = posEnd < posName-1 ? sMask.substring(posEnd, posName -1) : null;
          //null if "start**end" or "start*end"
        } else {
          bAllTree = false;
          sStartDir = sMask.substring(0,posName-1);
          sEndDir = null;
        }
      }
      if(sMask.charAt(posName) == '\b') {
        excludeNameFilter = new ArrayList<NameFilter>();
        excludeNameFilter.add(new NameFilter(sMask.substring(posName+1)));
        nameFilter = null;
      } else {
        nameFilter = new ArrayList<NameFilter>();
        nameFilter.add(new NameFilter(sMask.substring(posName)));
        excludeNameFilter = null;
      }
    }

  }
  

  
  
  public boolean checkName(String name)
  { //first check whether the name is excluded.
    if(excludeNameFilter !=null){
      for(NameFilter entry: excludeNameFilter) {
        if(entry.checkName(name)) {
          return false;
        }
      }
    }
    //if not excluded, true if there is not a name filter:
    if(nameFilter == null) return true;
    //check name filter:
    for(NameFilter entry: nameFilter) {
      if(entry.checkName(name)) {
        return true;
      }
    }
    //nothing found:
    return false; //all checked.
  }
  
  
  boolean checkDir(CharSequence sPath)
  {
    boolean bSelected = sEndDir == null && (sStartDir == null  || StringFunctions.equals(sStartDir, sPath))
          || ( sStartDir == null || StringFunctions.startsWith(sPath, sStartDir)) && (sEndDir == null || StringFunctions.endsWith(sPath, sEndDir));

    return bSelected;
  }




  @Override public boolean accept(File dir, String name)
  {
    // TODO Auto-generated method stub
    CharSequence sDir = FileSystem.normalizePath(dir);
    
    return checkDir(sDir) && checkName(name);
  }
  
}
