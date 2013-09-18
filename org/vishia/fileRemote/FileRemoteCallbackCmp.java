package org.vishia.fileRemote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.vishia.util.FileSystem;

public class FileRemoteCallbackCmp implements FileRemoteAccessor.CallbackFile
{
    FileRemote dir1, dir2;
    
    String basepath1;
    int zBasePath1;
    
    
    int mode;
    
    long minDiffTimestamp = 2000; 
    
    final static int onlyTimestamp = 1;
    final static int content = 2;
    final static int withoutLineend = 4;
    final static int withoutEndlineComment = 8;
    final static int withoutComment = 16;
    

    
    FileRemoteCallbackCmp(FileRemote dir1, FileRemote dir2){
      this.dir1 = dir1; this.dir2 = dir2;
      dir2.refreshPropertiesAndChildren(null);        
      
      //try{ 
        basepath1 = FileSystem.normalizePath(dir1.getAbsolutePath()).toString();
        zBasePath1 = basepath1.length();
      //} catch(Exception exc){
      //  dir1 = null; //does not exists.
      //}
    }
    
    @Override public void start()
    {
    }
    
    @Override public int offerFile(FileRemote file)
    {
      CharSequence path = FileSystem.normalizePath(file.getAbsolutePath());
      CharSequence localPath = path.subSequence(zBasePath1+1, path.length());
      FileRemote file2 = dir2.child(localPath);
      if(file2.isDirectory()){
        file2.refreshPropertiesAndChildren(null);        
      } else {
        
        if(file2.exists()){
          compareFile(file, file2);
        } else {
          file.setMarked(FileRemote.cmpAlone);
        }
      }
      return 0;
    }

    
    
    /**Compare two files.
     * @param file
     */
    void compareFile(FileRemote file1, FileRemote file2)
    {

      boolean equal, lenEqual;
      boolean equalDaylightSaved;
      boolean contentEqual;
      boolean contentEqualWithoutEndline;
      boolean readProblems;

      
      long date1 = file1.lastModified();
      long date2 = file2.lastModified();
      long len1 = file1.length();
      long len2 = file2.length();
      if(Math.abs(date1 - date2) > minDiffTimestamp && mode == onlyTimestamp){
        equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
        lenEqual = len1 == len2;
      } else if( ( Math.abs(date1 - date2 + 3600000) < minDiffTimestamp
                || Math.abs(date1 - date2 - 3600000) < minDiffTimestamp
                 ) && mode == onlyTimestamp){ 
        equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
      } else {
        //timestamp is not tested.
        if(len1 != len2){
          //different length
          equal = contentEqual = contentEqualWithoutEndline = lenEqual = false;
        } else {
          //Files are different in timestamp or timestamp is insufficient for comparison:
          try{ equal = compareFileContent(file1, file2);
          } catch( IOException exc){
            readProblems = true; equal = false;
          }
        }
      }
      if(equal){
        file1.setMarked(FileRemote.cmpContentEqual);
        file2.setMarked(FileRemote.cmpContentEqual);
      } else {
        file1.setMarked(FileRemote.cmpContentNotEqual);
        file2.setMarked(FileRemote.cmpContentNotEqual);
      }
    }
    
    
    
    
    /**Compare two files.
     * @param file
     * @throws FileNotFoundException 
     */
    boolean compareFileContent(FileRemote file1, FileRemote file2) 
    throws IOException
    {
      boolean bEqu = true;
      BufferedReader r1 =null, r2 = null;
      r1 = new BufferedReader(new FileReader(file1));
      r2 = new BufferedReader(new FileReader(file2));
      String s1, s2;
      while( bEqu && (s1 = r1.readLine()) !=null){
        s2 = r2.readLine();
        if(s2 ==null || !s1.equals(s2)){
          //check trimmed etc.
          bEqu = false;
        }
      }
      r1.close();
      r2.close();
      r1 = r2 = null;
      FileSystem.close(r1);
      FileSystem.close(r2);
      return bEqu;
    }  
    

    
    
    @Override public void finished()
    {
    }

  
  
}
