package org.vishia.fileRemote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;

/**This class supports comparison of files in a callback routine.
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteCallbackCmp implements FileRemoteAccessor.CallbackFile
{
  
  
    class CompareCtrl {
      
      /**Some Strings for start Strings to ignore comparison to end of line.
       * For example it contains "//" to ignore comments in source files.
       */
      final List<String> ignoreToEol = new LinkedList<String>();
      
      /**Entries with an array of 2 Strings, start and end of non-compare regions. */
      final List<String[]> ignoreFromTo = new LinkedList<String[]>();
      
      
    }
  
    private final CompareCtrl cmpCtrl = new CompareCtrl();
  
    private final FileRemote dir1, dir2;
    
    private final String basepath1;
    private final int zBasePath1;
    
    
    int mode;
    
    long minDiffTimestamp = 2000; 
    
    final static int cmp_onlyTimestamp = 1;
    final static int cmp_content = 2;
    final static int cmp_withoutLineend = 4;
    final static int cmp_withoutEndlineComment = 8;
    final static int cmp_withoutComment = 16;
    
    boolean aborted = false;
    
    FileRemoteCallbackCmp(FileRemote dir1, FileRemote dir2){
      this.dir1 = dir1; this.dir2 = dir2;
      dir2.refreshPropertiesAndChildren(null);        
      
      //try{ 
        basepath1 = FileSystem.normalizePath(dir1.getAbsolutePath()).toString();
        zBasePath1 = basepath1.length();
      //} catch(Exception exc){
      //  dir1 = null; //does not exists.
      //}
      cmpCtrl.ignoreToEol.add("Compilation time:");
      cmpCtrl.ignoreFromTo.add(new String[]{".epcannot:", ".epcannot.end:"});
    }
    
    @Override public void start()
    {
    }
    
    @Override public Result offerDir(FileRemote file){
      if(file == dir1){ return Result.cont; } //the first entry
      else {
        CharSequence path = FileSystem.normalizePath(file.getAbsolutePath());
        if(path.length() <= zBasePath1){
          //it should be file == dir1, but there is a second instance of the start directory.
          System.err.println("FileRemoteCallbackCmp - faulty FileRemote; " + path);
          return Result.cont;
        } else {
          CharSequence localPath = path.subSequence(zBasePath1+1, path.length());
          //System.out.println("FileRemoteCallbackCmp - dir; " + localPath);
          FileRemote file2 = dir2.child(localPath);
          if(!file2.exists()){
            file.setMarked(FileMark.cmpAlone);
            file.mark.setMarkParent(FileMark.cmpMissingFiles, false);
            return Result.skipSubtree;  //if it is a directory, skip it.        
          } else {
            file2.device.walkFileTree(file2, null, 1, callbackMarkSecondAlone);
            //waitfor
            //file2.refreshPropertiesAndChildren(null);        
            return Result.cont;
          }
        }
      }
    }
    
    /**Checks whether all files are compared or whether there are alone files.
     */
    @Override public Result finishedDir(FileRemote file){
      
      return Result.cont;      
    }
    
    
    @Override public Result offerFile(FileRemote file)
    {
      CharSequence path = FileSystem.normalizePath(file.getAbsolutePath());
      CharSequence localPath = path.subSequence(zBasePath1+1, path.length());
      //System.out.println("FileRemoteCallbackCmp - file; " + localPath);
      if(StringFunctions.compare(localPath, "supportBase/SupportBase.rpy")==0)
        Assert.stop();
      FileRemote file2 = dir2.child(localPath);
      if(!file2.exists()){
        file.setMarked(FileMark.cmpAlone);
        file.mark.setMarkParent(FileMark.cmpMissingFiles, false);
        return Result.skipSubtree;  //if it is a directory, skip it.        
      } else {
        file2.resetMarked(FileMark.cmpAlone);
        compareFile(file, file2);
        return Result.cont;
      }
    }

    
    
    @Override public boolean shouldAborted(){
      return aborted;
    }

    
    /**Compare two files.
     * @param file
     */
    void compareFile(FileRemote file1, FileRemote file2)
    {

      boolean equal, lenEqual;
      boolean equalDaylightSaved = false;
      boolean contentEqual;
      boolean contentEqualWithoutEndline;
      boolean readProblems;

      mode = cmp_withoutLineend;
  
      
      long date1 = file1.lastModified();
      long date2 = file2.lastModified();
      long len1 = file1.length();
      long len2 = file2.length();
      if(Math.abs(date1 - date2) > minDiffTimestamp && mode == cmp_onlyTimestamp){
        equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
        lenEqual = len1 == len2;
      } else if( ( Math.abs(date1 - date2 + 3600000) < minDiffTimestamp
                || Math.abs(date1 - date2 - 3600000) < minDiffTimestamp
                 ) && mode == cmp_onlyTimestamp){ 
        equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
      } else if(Math.abs(date1 - date2) < minDiffTimestamp && len1 == len2){
        //Date is equal, len is equal, don't spend time for check content.
        equal = equalDaylightSaved = lenEqual = true;
      } else {
        boolean doCmpr;
        //timestamp is not tested.
        if(len1 != len2){
          //different length
          if((mode & (cmp_withoutComment | cmp_withoutEndlineComment | cmp_withoutLineend)) !=0){
            //comparison is necessary because it may be equal without that features:
            doCmpr = true;
            equal = false;  //compare it, set only because warning.
          } else {
            equal = contentEqual = contentEqualWithoutEndline = lenEqual = false;
            doCmpr = false;
          }
        } else {
          doCmpr = true;
          equal = false;  //compare it, set only because warning.
          //Files are different in timestamp or timestamp is insufficient for comparison:
        }
        if(doCmpr){
          try{ equal = compareFileContent(file1, file2);
          } catch( IOException exc){
            readProblems = true; equal = false;
          }
        }
      }
      if(equal){
        file1.setMarked(FileMark.cmpContentEqual);
        file2.setMarked(FileMark.cmpContentEqual);
      } else {
        file1.setMarked(FileMark.cmpContentNotEqual);
        file2.setMarked(FileMark.cmpContentNotEqual);
        file1.mark.setMarkParent(FileMark.cmpFileDifferences, false);
        file2.mark.setMarkParent(FileMark.cmpFileDifferences, false);
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
        //check if an eol ignore String is contained:
        for(String sEol: cmpCtrl.ignoreToEol) {
          int z1 = s1.indexOf(sEol);
          if( z1 >=0){
            s1 = s1.substring(0, z1);    //shorten s1 to eol text
            int z2 = s2.indexOf(sEol);
            if(z2 >=0 && z2 == z1){
              s2 = s2.substring(0, z2);  //shorten s2 to eol text
            } //else: non't shorten, it is possible that s2 ends exactly without the sEol text. Than it is accepted.
            break; //break the for
          }
        }
        //check if an ignore String is contained, not after the eol!
        for(String[] fromTo: cmpCtrl.ignoreFromTo) {
          int z1 = s1.indexOf(fromTo[0]);
          if(z1 >=0){
            //from-marker was found:
            s1 = s1.substring(0, z1);
            //read the file lines till end was found:
            String s3;
            while( (s3 = r1.readLine()) !=null){
              int z3 = s3.indexOf(fromTo[1]);
              if(z3 >=0){
                s1 += s3.substring(z3 + fromTo[1].length());  //rest after to-string, maybe length=0
                break;  //break while readLine()
              }
            }
            int z2 = s2.indexOf(fromTo[0]);  //check second line whether the marker is contained too
            if(z2 >=0){
              s2 = s2.substring(0, z2);
              //read the file lines till end was found:
              String s4;
              while( (s4 = r2.readLine()) !=null){
                int z4 = s4.indexOf(fromTo[1]);
                if(z4 >=0){
                  s2 += s4.substring(z4 + fromTo[1].length());  //rest after to-string, maybe length=0
                  break;  //break while readLine()
                }
              }    
            } //else: accept that the s2 does not contain anything of this text part.
            //s1, s2 contains the start of line till from-String and the end of line after the to-String, compare it.
            //If the end marker is not found the rest to end of file is ignored.
            break; //break the for
          }
        }
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

  
    
    /**Callback to mark all files of the second directory as 'alone' on open directory.
     * If the files are found, there are marked as 'equal' or 'non equal' then, this selection
     * will be reset. 
     * 
     */
    final FileRemoteAccessor.CallbackFile callbackMarkSecondAlone = new FileRemoteAccessor.CallbackFile()
    {

      @Override
      public void finished()
      { }

      @Override
      public Result finishedDir(FileRemote file)
      { return Result.cont; }

      @Override
      public Result offerDir(FileRemote file)
      { return Result.cont; }

      @Override
      public Result offerFile(FileRemote file)
      { 
        file.setMarked(FileMark.cmpAlone);
        return Result.cont;
      }

      @Override
      public void start()
      { }
      
      @Override public boolean shouldAborted(){
        return false;
      }

      
    };
  
}
