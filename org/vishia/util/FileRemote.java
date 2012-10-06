package org.vishia.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;


/**This class describes a File, which may be localized at any maybe remote device or which may be a normal local file. 
 * A remote file should be accessed by FileRemoteChannel implementations. It may be any information
 * at an embedded hardware, not only in a standard network.
 * This class executes a remote access only if properties of the file are requested.
 * It stores information about the file without access. 
 * <br><br>
 * This class inherits from java.lang.File. The advantage is:
 * <ul>
 * <li>The class File defines the interface to the remote files too. No extra definition of access methods is need.
 * <li>Any reference to a file can store the reference to a remote file too.
 * <li>The implementation for local files is given without additional effort.
 * </ul> 
 * If the file is a local file, the standard java file access algorithm are used.
 * @author Hartmut Schorrig
 *
 */
public class FileRemote extends File
{
  public interface FileRemoteAccessorSelector
  {
    FileRemoteAccessor selectFileRemoteAccessor(String sPath);
  }

  private static final long serialVersionUID = -5568304770699633308L;

  /**Version, history and license.
   * <ul>
   * <li>2012-10-01 Hartmut chg: {@link #children} is now of super type File, not FileRemote. Nevertheless FileRemote objects
   *   are stored there. Experience is possible to store a File object returned from File.listFiles without wrapping, and
   *   replace that with a FileRemote object if necessary. listFiles() returns a File[] like its super method.
   * <li>2012-10-01 Hartmut new: {@link #isTested()}  
   * <li>2012-08-11 Hartmut new: method {@link #openInputStream(long)}. An application may need that, for example to create
   *   a {@link java.io.Reader} with the input stream. Some implementations, especially a local file and a {@link java.util.zip.ZipFile}
   *   supports that. An {@link java.io.InputStream} may force a blocking if data are not available yet for file in a remote device
   *   but that may be accepted. 
   * <li> 2012-08-11 Hartmut new: {@link #listFiles(FileFilter)} now implemented here. 
   * <li>2012-08-05 Hartmut chg: The super class File needs the correct path. So it is able to use for a local file nevertheless.
   *   What is with oFile if it is a FileRemote? should refer this? See change from 2012-01-01.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-28 Hartmut chg: Concept of remote files enhanced with respect to {@link FileAccessZip}.
   *   <ul>
   *   <li>New references {@link #parent} and {@link #children}. They are filled calling {@link #refreshPropertiesAndChildren(CallbackEvent)}.
   *   <li>More separation of java.io.File accesses. In the past only the local files were supported really.
   *   <li>new interface {@link FileRemoteAccessorSelector} and {@link #setAccessorSelector(FileRemoteAccessorSelector)}.
   *     The user can have any algorithm to select a {@link FileRemoteAccessor} depending on the
   *     path of the file. A prefix String may determine how the file is to access. If that routine
   *     is not called, the {@link FileRemoteAccessorLocalFile#selectLocalFileAlways}.
   *   <li>{@link #FileRemote(FileRemoteAccessor, FileRemote, String, String, long, long, int, Object)}
   *     has the parent as parameter. The parameter oFileP is stored now. It is any data to access the file object.
   *   <li>The constructor had access the file if length=-1 was given. But that is not the convention.
   *     An access may need execution and waiting time for a remote communication. The constructor
   *     should never wait. Instead the methods:
   *   <li>{@link #refreshProperties(CallbackEvent)} and {@link #refreshPropertiesAndChildren(CallbackEvent)}
   *     have to be called if the properties of the real file on the local system (java.io.File)
   *     or any remote system are need. That routines envisages the continuation of working
   *     with a callback event are invocation mechanism. For example if the file properties
   *     should be shown in a graphic application, the building of the graphic can't stop and wait 
   *     for more as some 100 milliseconds. It is better to clear a table and continue working in graphic. 
   *     If the properties are gotten from the remote system then the table will be filled.
   *     That may be invoked from another thread, the communication thread for the remote device
   *     or by an event mechanism (see {@link FileRemote.CallbackEvent} respectively {@link org.vishia.util.Event}.
   *   <li>The routine {@link #fromFile(File)} reads are properties of a local file if one is given.
   *     In that case the {@link #refreshProperties(CallbackEvent)} need not be invoked additionally.
   *   <li>{@link #openRead(long)} and {@link #openWrite(long)} accepts a non-given device.
   *     They select it calling {@link FileRemoteAccessorSelector#selectFileRemoteAccessor(String)}
   *   <li>All get methods {@link #length}, {@link #lastModified()}, {@link #isDirectory()} etc.
   *     now returns only the stored values. It may necessary to invoke {@link #refreshProperties(CallbackEvent)}
   *     in the application before they are called to get the correct values. The refreshing
   *     can't be called in that getter routines because they should not wait for communication.
   *     In the case of local files that access may be shorten in time, but it isn't known
   *     whether it is a local file. The user algorithm should work with remote files too if they are
   *     tested locally only. Therefore a different strategy to access properties are not proper to use.
   *   <li>{@link #getParentFile()} now uses the {@link #parent} reference. If it is null,
   *     a new FileRemote instance for the parent is created, but without access to the file,
   *     only with knowledge of the path string. Because the {@link #FileRemote(FileRemoteAccessor, FileRemote, String, String, long, long, int, Object)}
   *     will be gotten the parent of it too, all parent instances will be set recursively then.
   *   <li>{@link #listFiles()} now returns the {@link #children} only. If the user has not called
   *     {@link #refreshPropertiesAndChildren(CallbackEvent)}, it is empty.           
   *   </ul>
   * <li>2012-07-21 Hartmut new: {@link #delete(String, boolean, Event)} with given mask. TODO: It should done in 
   *   {@link org.vishia.util.FileRemoteAccessorLocalFile} in an extra thread.
   * <li>2012-03-10 Hartmut new: {@link #chgProps(String, int, int, long, CallbackEvent)}, {@link #countAllFileLength(CallbackEvent)}.
   *   Enhancements.
   * <li>2012-02-02 Hartmut chg: Now the {@link #sFile} (renamed from name) is empty if this describes
   *   an directory and it is known that it is an directory. The ctor is adapted therefore.
   *   {@link #getParent()} is changed. Some assertions are set.
   * <li>2012-02-02 Hartmut chg: Handling of relative paths: It is detected in ctor. TODO relative paths are not tested well. 
   * <li>2012-01-14 Hartmut chg: The toplevel directory contains only one slash in the {@link #sDir}
   *   and an empty name in {@link #key}. 
   * <li>2012-01-14 Hartmut new: {@link #getParentFile()} now implemented here.  
   * <li>2012-01-14 Hartmut new: {@link #fromFile(File)} to convert from a normal File instance.
   * <li>2012-01-06 Hartmut new: Some functionality for {@link #_setProperties(long, long, int, Object)}
   *   and symbolic linked paths.
   * <li>2012-01-01 Hartmut new: {@link #oFile}. In the future the superclass File should be used only as interface.
   *   TODO: For any file access the oFile-instance should be used by {@link #device}.
   * <li>2012-01-01 Hartmut new: {@link #copyTo(FileRemote, Event)}
   * <li>2011-12-10 Hartmut creation: It is needed for {@link org.vishia.commander.Fcmd}, this tool
   *   should work with remote files with any protocol for example FTP. But firstly it is implemented and tested
   *   only for local files. The concept is: 
   *   <ul>
   *   <li>All relevant information about a file are stored locally in this instance if they are known.
   *   <li>If information are unknown, on construction 'unknown' is stored, no access occurs.
   *     But if they are requested, an access is done.
   *   <li>Any instance of the interface {@link FileRemoteAccessor} is responsible to execute the remote access.
   *   <li>The access to local files are done with this class directly.
   *   </ul>
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
  public static final int version = 20120728;

  private static FileRemoteAccessorSelector accessorSelector;
  
  protected FileRemoteAccessor device;
  
  /**Reference file.
   * This field, the field {@link #referenceFileRef} and the reference inside {@link #referenceFileRef} 
   * are not used if this instance does not contain a relative path. 
   * That fields should but don't need to set to null.
   * <br>
   * If this field and    
   * 
   */
  protected FileRemote referenceFile;

  /**Alternative for referencing of the reference file. */
  protected FileRemote[] referenceFileRef;
  
  /**The directory path of the file. It ends with '/' always.
   * If the path is absolute, it doesn't contain any "/./" 
   * or "/../"-parts. Then it is an canonical absolute path.
   * An absolute path can contain a start string for a remote device designation.
   * <br> 
   * The directory path may be relative too. A relative path may be empty "". */
  protected final String sDir;
  
  /**The name with extension of the file. It is empty if this describes a directory. 
   * */
  protected final String sFile;
  
  /**The unique path to the file or directory entry. If the file is symbolic linked (on UNIX systems),
   * this field contains the non-linked direct path. But the {@link #sDir} contains the linked path. 
   */
  protected String sCanonicalPath;
  
  /**Timestamp of the file. */
  protected long date;
  
  /**Length of the file. */
  protected long length;
  
  /**Some flag bits. @see constants #mExist etc.*/
  int flags;

  FileRemote parent;
  
  /**The content of a directory. It contains all files, proper for return {@link #listFiles()} without filter. 
   * The content is valid at the time of calling {@link FileRemoteAccessor#refreshFilePropertiesAndChildren(FileRemote, Event)}.
   * It is possible that the content of the physical directory is changed meanwhile.
   * If this field should be returned without null, especially on {@link #listFiles()} and the file is a directory, 
   * the {@link FileRemoteAccessor#refreshFilePropertiesAndChildren(FileRemote, Event)} will be called.  
   * */
  File[] children;
  
  public final static int modeCopyReadOnlyMask = 0x00f
  , modeCopyReadOnlyNever = 0x1, modeCopyReadOnlyOverwrite = 0x3, modeCopyReadOnlyAks = 0;

  public final static int modeCopyExistMask = 0x0f0
  , modeCopyExistNewer = 0x10, modeCopyExistOlder = 0x20, modeCopyExistAll = 0x30, modeCopyExistAsk = 0;
  
  public final static int modeCopyCreateMask = 0xf00
  , modeCopyCreateNever = 0x200, modeCopyCreateYes = 0x300 , modeCopyCreateAsk = 0;
  
  public final static int  mExist =   1;
  public final static int  mCanRead =  2;
  public final static int  mCanWrite =  4;
  public final static int  mHidden = 0x08;
  public final static int  mDirectory = 0x10;
  public final static int  mFile =     0x20;
  public final static int  mExecute =     0x40;
  public final static int  mExecuteAny =     0x80;
  public final static int  mRelativePath = 0x100;
  public final static int  mAbsPath = 0x200;
  public final static int  mSymLinkedPath = 0x400;
  
  public final static int  mCanReadGrp =  0x0800;
  public final static int  mCanWriteGrp = 0x1000;
  public final static int  mExecuteGrp =  0x2000;
  public final static int  mCanReadAny =  0x4000;
  public final static int  mCanWriteAny = 0x8000;

  protected final static int  mAbsPathTested = 0x10000;
  protected final static int  mTested =        0x20000;
  //protected final static int mChildrenGotten = 0x40000;
  protected final static int mThreadIsRunning =0x80000;

  
  
  /*
  public final static int cmdCheckFile = 0xcecf1e, cmdCheck = 0xcec, cmdCopy = 0xc0b7, cmdDel = 0xde1ede
  , cmdMove = 0x307e, cmdChgProps = 0xc5a9e, cmdChgPropsRec = 0xc595ec
  , cmdCountLength = 0xc0311e39
  , cmdAbortFile = 0xab03df1e, cmdAbortDir = 0xab03dd13, cmdAbortAll = 0xab03da11;

  
  
  /**back event for ask * /
  public final static int evAskErrorCopy = 0xa58ecb7
  ;
  
  
  

  /**callback cmd * /
  public final static int acknSuccess = 0x50ce55
  , acknErrorOpen = 0xe30be6
  , acknErrorDelete = 0xe3de1ede
  , acknAbortFile = 0xab03df1e
  , acknAbortDir = 0xab03dd13
  , acknAbortAll = 0xab03da11;
  */
  
  
  /**This is the internal file object. It is handled by the device only. */
  Object oFile;
  
  
  /**Creates an instance without access to the physical file. Only The directory path and the name 
   * is stored and the device is identified by analysis of the prefix string of path. 
   * @param pathname device, path and name. The path may be relative. 
   *   Then the systems current directory is used as reference file. (TODO)
   */
  public FileRemote(String pathname)
  {
    this(getAccessorSelector().selectFileRemoteAccessor(pathname) //identify device by analyse of path.
        , null //parent is unknown without access
        , pathname, null   //identify the name by analyse of path.
        , 0, 0, 0, null);  //size etc. and  file object is unknown yet.
  }

  
  /**Creates an instance without access to the physical file. Only The path is stored. 
   */
  /**Creates an instance without access to the physical file. Only The directory path and the name 
   * is stored and the device is identified by analysis of the prefix string of path. 
   * @param dir device and path to the directory (parent). The dir may be relative.
   *   Then the systems current directory is used as reference file. (TODO)
   * @param name name of the file inside the given dir. 
   */
  public FileRemote(String dir, String name)
  {
    this(getAccessorSelector().selectFileRemoteAccessor(dir) //identify device by analyse of path.
        , null //parent is unknown without access
        , dir, name
        , 0, 0, 0, null);  //size etc. and  file object is unknown yet.
  }

  
  /**Creates an instance without access to the physical file. Only The path is stored. 
   * @param dir instance describes the directory (parent).
   * @param name name of the file inside the given dir. 
   */
  public FileRemote(FileRemote dir, String name)
  {
    this(dir.device, dir, dir.getPath(), name, 0, 0, 0, null);
  }

  
  
  
  /**Constructs the instance. If the length parameter is given or it is 0, 
   * this invocation does not force any access to the file system. The parameter may be given
   * by a complete communication or file access before construction of this. 
   * Then they are given as parameter for this constructor.
   * <br><br>
   * The parameter of the file (properties, length, date) can be given as 'undefined' 
   * using the 0 as value. Then the quest {@link #exists()} returns false. This instance
   * describes a File object only, it does not access to the file system.
   * The properties of the real file inclusively the length and date can be gotten 
   * from the file system calling {@link #refreshProperties(CallbackEvent)}. This operation may be
   * invoked in another thread (depending on the device) and may be need some operation time.
   *  
   * @param device The device which organizes the access to the file system.
   * @param sDirP The path to the directory.
   *   The standard path separator is the slash "/". 
   *   A backslash will be converted to slash internally, it isn't distinct from the slash.
   *   If this parameter ends with an slash or backslash and the name is null or empty, this is designated 
   *   as an directory descriptor. {@link #mDirectory} will be set in {@link #flags}.
   * @param sName Name of the file. If null then the name is gotten from the last part of path
   *   after the last slash or backslash.
   * @param length The length of the file. Maybe 0 if unknown. 
   * @param date Timestamp of the file. Maybe 0 if unknown.
   * @param flags Properties of the file. Maybe 0 if unknown.
   * @param oFileP an system file Object, may be null.
   */
  public FileRemote(final FileRemoteAccessor device
      , final FileRemote parent
      , final String sDirP, final String sName
      , final long length, final long date, final int flags
      , Object oFileP) {
    super(sDirP + (sName ==null ? "" : ("/" + sName)));  //it is correct if it is a local file. 
    //super("?");  //NOTE: use the superclass File only as interface. Don't use it as local file instance.
    String sPath = sDirP.replace('\\', '/');
    this.device = device;
    this.flags = flags;
    this.parent = parent;
    String name1;
    if(sName == null){
      int lenPath = sPath.length();
      int posSep = sPath.lastIndexOf('/'); //, lenPath-2);
      if(posSep >=0){
        this.sDir = sPath.substring(0, posSep+1);
        if(posSep == sPath.length()-1){
          name1 = "";  //it is a directory.
          this.flags |= mDirectory;
        } else {
          name1 = sPath.substring(posSep+1);
        }
      } else { //no / found, it is only a name:
        this.flags |= mRelativePath;
        this.sDir = "";
        name1 = sPath;
      }
    } else { //name is given:
      if(!sPath.endsWith("/")){ sPath += "/";}
      this.sDir = sPath;
      name1 = sName;
    }
    //if(name1.endsWith("/")){ this.name = name1.substring(0, name1.length()-1); }
    //else { this.name = name1; }
    this.sFile = name1;
    Assert.check(this.sFile !=null);
    Assert.check(this.sDir !=null);
    Assert.check(!name1.contains("/"));
    Assert.check(this.sDir.length() == 0 || this.sDir.endsWith("/"));
    //TODO Assert.check(!this.sDir.endsWith("//"));
    Assert.check(length >=0);
    oFile = oFileP;
    this.length = length;
    this.date = date;
    this.sCanonicalPath = this.sDir + this.sFile;  //maybe overwrite from setSymbolicLinkedPath
  }
  
 
  
  public static boolean setAccessorSelector(FileRemoteAccessorSelector accessorSelectorP){
    boolean wasSetAlready = accessorSelector !=null;
    accessorSelector = accessorSelectorP;
    return wasSetAlready;
  }
  
  
  private static FileRemoteAccessorSelector getAccessorSelector(){
    if(accessorSelector == null){
      accessorSelector = FileRemoteAccessorLocalFile.selectLocalFileAlways;
    }
    return accessorSelector;
  }
  
  
  
  
  /**Returns a FileRemote instance from a standard java.io.File instance.
   * If src is instanceof FileRemote already, it returns src.
   * Elsewhere it builds a new instance of FileRemote which inherits from File,
   * it is a new instance of File too.
   * @param src Any File or FileRemote instance.
   * @return src if it is instanceof FileRemote or a new Instance.
   */
  public static FileRemote fromFile(File src){
    if(src instanceof FileRemote){ return (FileRemote)src; }
    else {
      //it is a file description of standard java in the local file system.
      String sPath = src.getAbsolutePath();
      long len = 0;
      long date = 0;
      int fileProps = 0;
      if(src.exists()){ fileProps |= mExist; 
        len = src.length();
        date = src.lastModified();
        if(src.isDirectory()){ fileProps |= mDirectory; }
        if(src.canRead()){ fileProps |= mCanRead | mCanReadGrp | mCanReadAny; }
        if(src.canWrite()){ fileProps |= mCanWrite | mCanWriteGrp | mCanWriteAny; }
        if(src.canExecute()){ fileProps |= mExecute | mExecuteGrp | mExecuteAny; }
        if(src.isHidden()){ fileProps |= mHidden; }
      }
      FileRemoteAccessor accessor = FileRemoteAccessorLocalFile.getInstance();
      File dir1 = src.getParentFile();
      FileRemote dir;
      if(dir1 !=null){
        dir= FileRemote.fromFile(dir1);
      } else {
        dir = null;
      }
      return new FileRemote(accessor, dir, sPath, null, len, date, fileProps, src);
    }
  }
  
  
  /**Sets the properties to this.
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param length
   * @param date
   * @param flags
   * @param oFileP reference to a file Object, for example java.io.File for local files.
   */
  public void _setProperties(final long length, final long date, final int flags
      , Object oFileP) {
    this.length = length;
    this.date = date;
    this.flags = flags;
    this.oFile = oFileP;
  }
  
  
  /**Gets the properties of the file from the physical file.
   * @param callback
   */
  public void refreshProperties(CallbackEvent callback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    device.refreshFileProperties(this, callback);
  }
  
  
  /**Gets the properties of the file from the physical file.
   * @param callback
   */
  public void refreshPropertiesAndChildren(CallbackEvent callback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    device.refreshFilePropertiesAndChildren(this, callback);
  }
  
  
  /**Sets the reference for a given relative path to the named file. 
   * <br><br>
   * <b>Concepts of relative paths</b>:
   * <ul>
   * <li><b>Current directory</b>: In a local file-system most of the operation systems knows a <i>current directory</i> which is
   *   the base for relative paths. The <i>current directory</i> is a property of the running process on
   *   operation system. A java.io.File with a relative path references to this <i>current directory</i> 
   *   which can be gotten by system calls. <br>
   *   This class needs a direct reference to the <i>current directory</i> given with this method.
   * <li><b>relative link</b>: For links (browser, XML) another concept is usually: A relative path starts
   *   from that file which contains the link. This field should refer that source file then.
   * </ul>
   * This method should not be called with any reference if the file contains an absolute path. The reference
   * file is not used then.
   *  
   * @param ref If the file is given with a relative path, this is the reference file.
   */
  public void setReferenceFile(FileRemote ref){
    referenceFile = ref;
    referenceFileRef = null;
  }
  
  
  /**Sets the reference for a given relative path to the file references with ref.
   * This is a alternative to {@link #setReferenceFile(FileRemote)}. Only one of them should be invoked,
   * the last invocation wins. The advantage of this method: Changing of a referencing file
   * have to be done only in one instance, the ref. All files which references ref don't need to be changed. 
   * @param ref Reference to a FileRemote[1] which references the reference file for the relative path.
   */
  public void setReferenceFile(FileRemote[] ref){
    referenceFile = null;
    referenceFileRef = ref;
  }
  
  
  
  /**Sets this as a symbolic linked file or dir with the given path. 
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param pathP The path where the file is organized.
   */
  public void setSymbolicLinkedPath(String pathP){
    flags |= mSymLinkedPath;
    this.sCanonicalPath = pathP;
  }
  
  
  /**Sets this as a non-symbolic linked file or dir with the given path. 
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param pathP The absolute canonical path where the file is organized.
   */
  public void setCanonicalAbsPath(String pathP){
    flags |= mAbsPath;
    flags &= ~mSymLinkedPath;
    this.sCanonicalPath = pathP;
  }
  
  
  /**Check whether two files are at the same device. It means it can be copied, compared etc. remotely. 
   * @param other The other file to check
   * @return true if they are at the same device. The same device is given if the comparison
   *   of the {@link FileRemoteAccessor} instances of both files using {@link #device}.equals(other.device) 
   *   returns true.
   */
  public boolean sameDevice(FileRemote other){ return device.equals(other.device); }
  
  
  /**Opens a read access to this file.
   * If the file is remote, this method should return immediately with a prepared channel functionality (depending from implementation).
   * A communication with the remote device will be initiated to get the first bytes parallel in an extra thread.
   * If the first access will be done, that bytes will be returned without waiting.
   * If a non-blocking mode is used for the device, a {@link java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)}
   * invocation returns 0 if there are no bytes available in this moment. An polling invocation later may transfer that bytes.
   * In this kind a non blocking mode is possible.
   * 
   * @param passPhrase a pass phrase if the access is secured.
   * @return The channel to access.
   */
  public ReadableByteChannel openRead(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openRead(this, passPhrase);
  }
  

  /**Opens a read access to this file.
   * If the file is remote, this method should return immediately with a prepared stream functionality (depending from implementation).
   * A communication with the remote device will be initiated to get the first bytes parallel in an extra thread.
   * If the first access will be done, that bytes will be returned without waiting. 
   * But if the data are not supplied in this time, the InputStream.read() method blocks until data are available
   * or the end of file or any error is detected. That is the contract for a InputStream.
   * 
   * 
   * @param passPhrase a pass phrase if the access is secured.
   * @return The byte input stream to access.
   */
  public InputStream openInputStream(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openInputStream(this, passPhrase);
    
  }
  
  
  
  
  /**Opens a write access to this file.
   * @param passPhrase a pass phrase if the access is secured.
   * @return The channel to access.
   */
  public WritableByteChannel openWrite(long passPhrase){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.openWrite(this, passPhrase);
  }
  
  
  @Override public long length(){ 
    /*
    if(length ==-1){
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      if(device.isLocalFileSystem()) length = super.length();
      else device.setFileProperties(this);  //maybe wait for communication.
    }
    */
    return length; 
  }
  
  @Override public long lastModified(){ 
    /*
    if(date ==0){
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      if(device.isLocalFileSystem()) date = super.lastModified();
      else device.setFileProperties(this);  //maybe wait for communication.
    }
    */
    return date; 
  }
  
  
  public int getFlags(){ return flags; }
  
  
  @Override public String getName(){ return sFile; }
  
  /**Returns the parent path, it is the directory path. 
   * It is define by:
   * @see java.io.File#getParent()
   * @return path without ending slash.
   */
  @Override public String getParent(){ 
    File parentFile = getParentFile();  //
    return parentFile.getAbsolutePath();
    //int posSlash = sDir.indexOf('/');  //the first slash
    //if only one slash is present, return it. Elsewhere don't return the terminating slash.
    //String sParent = zDir > posSlash+1 ? sDir.substring(0, zDir-1): sDir;
    //return sParent; 
  }
  
  /**Gets the path of the file. For this class the path should be esteemed as canonical,
   * it is considered on constructor. 
   * The difference between this routine and {@link #getCanonicalPath()} is:
   * The canonical path under Unix-Systems (linux) is the linked path. 
   * The return path of this routine is the path without dissolving symbolic links.
   * @return path never ending with "/", but canonical, slash as separator. 
   */
  @Override public String getPath(){ 
    if(sFile !=null && sFile.length() > 0) return sDir + sFile;
    else {
      int zDir = sDir.length();
      Assert.check(sDir.charAt(zDir-1) == '/');
      if(zDir == 1 || zDir == 3 && sDir.charAt(1) == ':'){
        return sDir;  //with ending slash because it is root.
      } else {
        return sDir.substring(0, zDir-1);  //without /
      }
    }
  }
  
  @Override public String getCanonicalPath(){ return sCanonicalPath; }
  
  
  /**Gets the parent directory.
   * It creates a new instance of FileRemote with the path infos from {@link #sDir}.
   * 
   * @return null if this is the toplevel directory.
   */
  @Override public FileRemote getParentFile(){
    //final FileRemote parent;
    if(parent == null){
      String sParent;
      int zDir = sDir.length();
      int pDir;
      Assert.check(sDir.charAt(zDir-1) == '/'); //Should end with slash
      if(sFile == null || sFile.length() == 0){
        //it is a directory, get its parent path
        if(zDir > 1){
          pDir = sDir.lastIndexOf('/', zDir-2);
          if(pDir == 0 || (pDir == 2 && sDir.charAt(1) == ':')){
            //the root. 
            pDir +=1; //return inclusive the /
          }
          if(pDir >0){
            sParent = sDir.substring(0, pDir);  //without ending slash
          } else {
            sParent = null;  //sDir is forex "D:/", or "/" it hasn't a parent.
          }
        } else {
          sParent = null;    //sDir has only one char, it may be a "/", it hasn't a parent.
        }
      } else { //a sFile is given, the sDir is the parent.
        if(zDir == 1 || (zDir == 3 && sDir.charAt(1) == ':')){
          //the root. 
          pDir = zDir; //return inclusive the /
        } else {
          pDir = zDir -1;
        }
        sParent = sDir.substring(0, pDir);
      }
      if(sParent !=null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
        this.parent = new FileRemote(device, null, sParent, null, 0, 0, 0, null); 
      }
    }
    return this.parent;
    /*
    String sParent = getParent();
    if(sParent !=null){
      parent = new FileRemote(sParent);
    
    //if(sDir.indexOf('/') < sDir.length()-1 || name.length() > 0){
    //  parent = new FileRemote(sDir);
    } else {
      parent = null;
    }
    return parent;
    */
  }
  
  
  /**Returns true if the file was tested in the past. Returns false only if the file is created
   * and never refreshed.   */
  public boolean isTested(){ return (flags & mTested) == mTested; }
  
  
  
  @Override public boolean exists(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mExist) !=0; 
  }
  
  @Override public boolean isFile(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mFile) !=0; 
  }
  
  @Override public boolean isDirectory(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mDirectory) !=0; 
  }
  
  @Override public boolean canWrite(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mCanWrite) !=0; 
  }
  
  @Override public boolean canRead(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mCanRead) !=0; 
  }
  
  @Override public boolean canExecute(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mExecute) !=0; 
  }
  
  public boolean isSymbolicLink(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return (flags & mSymLinkedPath) !=0; 
  }
  
  
  @Override public String getAbsolutePath(){
    File ref = referenceFileRef !=null && referenceFileRef.length >=1 && referenceFileRef[0] !=null 
        ? referenceFileRef[0] : referenceFile;
    String sAbsPath;
    if(ref !=null){
      sAbsPath = ref.getAbsolutePath() + '/' + sDir + sFile;
    } else {
      sAbsPath = sDir + sFile;
    }
    return sAbsPath;
  }
  
  
  
  /**This method overrides java.io.File.listFiles() but returns Objects from this class type.
   * @see java.io.File#listFiles()
   * If the children files are gotten from the maybe remote file system, this method returns immediately
   * with that result. But it may be out of date. The user can call {@link #refreshPropertiesAndChildren(CallbackEvent)}
   * to get the new situation.
   * <br><br>
   * If the children are not gotten up to now they are gotten yet. The method blocks until the information is gotten,
   * see {@link FileRemoteAccessor#refreshFilePropertiesAndChildren(FileRemote, Event)} with null as event parameter.
   */
  @Override public File[] listFiles(){
    if(children == null){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFilePropertiesAndChildren(this, null);
    }
    return children;
  }
  
  
  
  @Override public File[] listFiles(FileFilter filter) {
    if(children == null){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFilePropertiesAndChildren(this, null);
    }
    List<File> children = device.getChildren(this, filter);
    File[] aChildren = new File[children.size()];
    return children.toArray(aChildren);
  }
  
  
  
  @Override public boolean delete(){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.delete(this, null);
  }
  
  /**Deletes a file maybe in a remote device. This is a send-only routine without feedback,
   * because the calling thread should not be waiting for success.
   * The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * @param backEvent The event for success.
   */
  public void delete(FileRemote.CallbackEvent backEvent){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(device.isLocalFileSystem()){
      boolean bOk;
      if(isDirectory()){
        bOk = FileSystem.rmdir(this);
      } else {
        bOk = ((File)oFile).delete();
      }
      backEvent.data1 = bOk? 0 : -1;
      backEvent.occupy(evSrc, true);
      backEvent.sendEvent(FileRemote.CallbackCmd.done);
    } else {
      //TODO
    }
  }
  
  
  
  /**Deletes a files given by path maybe in a remote device. This is a send-only routine without feedback,
   * because the calling thread should not be waiting for success.
   * The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * @param backEvent The event for success.
   */
  public void delete(String sPath, boolean deleteReadOnly, FileRemote.CallbackEvent backEvent){
    boolean bOk;
    List<File> listFiles = new LinkedList<File>();
    try{
      FileSystem.addFileToList(this, sPath, listFiles);
      bOk = true;
      for(File file: listFiles){
        if(file.isDirectory()){
          if(!FileSystem.rmdir(file)){ bOk = false; };
        } else {
          if(!file.canWrite()){
            file.setWritable(true);
          }
          if(!file.delete()){ bOk = false; };
        }
      }
    } catch(FileNotFoundException exc){
      bOk = false;
    }
    if(backEvent !=null){
      backEvent.occupy(evSrc, true);
      backEvent.data1 = bOk? 0 : -1;
      backEvent.sendEvent(FileRemote.CallbackCmd.done);
    }
  }
  
  
  
  /**Checks a file maybe in a remote device maybe a directory. 
   * This is a send-only routine without feedback, because the calling thread should not be waiting 
   * for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param callback This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param backEvent The event for success.
   */
  public void check(FileRemote.CallbackEvent evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    CmdEvent ev = device.prepareCmdEvent(evback);
    
    ev.filesrc =this;
    ev.filedst = null;
    ev.data1 = 0;
    ev.sendEvent(Cmd.check);
  }
  
  
  /**Copies a file or directory maybe in a remote device to another file in the same device. 
   * This is a send-only routine without feedback, because the calling thread should not be waiting 
   * for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param evback The event for success.
   */
  public void copyTo(FileRemote dst, FileRemote.CallbackEvent evback, int mode){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(dst.device == null){
      dst.device = getAccessorSelector().selectFileRemoteAccessor(dst.getAbsolutePath());
    }
    
    CmdEvent ev = device.prepareCmdEvent(evback);
    
    ev.filesrc =this;
    ev.filedst = dst;
    ev.data1 = mode;
    ev.sendEvent(Cmd.copy);
    
  }
  
  
  
  /**Moves a file maybe in a remote device to another file.
   * If the devices are the same, it sends a commission only to the device. The action is done in the
   * other device respectively in another thread {@link FileRemoteAccessorLocalFile#runCommissions}
   * if this is a local file on this computer.<br><br>
   * Depending on the file system the moving is a copy with deleting the source if this and dst are
   * at the same partition, then it is a lightweight operation. If this and dst are at different 
   * partitions, the operation file system will be copy it and then delete this. It means,
   * this operation can be need some time for large files.
   * <br><br> 
   * If this and dst are at different devices, this routine copies and deletes.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param backEvent The event for success.
   */
  public void moveTo(FileRemote dst, FileRemote.CallbackEvent evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    CmdEvent ev = device.prepareCmdEvent(evback);
    ev.filesrc = this;
    ev.filedst = dst;
    ev.sendEvent(Cmd.move);
  }
  
  
  
  
  
  
  
  /**Change the file properties maybe in a remote device.
   * A new name may be given as parameter. Some properties may be changed calling
   * {@link #setWritable(boolean)} etc. or they are given as parameter.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param newName A new name for the file. This parameter may be null, then the old name remain.
   * @param backEvent The event for success.
   */
  public void chgProps(String newName, int maskFlags, int newFlags, long newDate, FileRemote.CallbackEvent evback){
    CmdEvent ev = device.prepareCmdEvent(evback);
    ev.filesrc = this;
    ev.filedst = null;
    ev.newName = newName;
    ev.maskFlags = maskFlags;
    ev.newFlags = newFlags;
    ev.newDate = newDate;
    ev.sendEvent(Cmd.chgProps);
  }
  
  
  
  /**Change the file properties maybe in a remote device.
   * A new name may be given as parameter. Some properties may be changed calling
   * {@link #setWritable(boolean)} etc. or they are given as parameter.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param newName A new name for the file. This parameter may be null, then the old name remain.
   * @param backEvent The event for success.
   */
  public void chgPropsRecursive(int maskFlags, int newFlags, long newDate, FileRemote.CallbackEvent evback){
    CmdEvent ev = device.prepareCmdEvent(evback);
    ev.filesrc = this;
    ev.filedst = null;
    ev.newName = null;
    ev.maskFlags = maskFlags;
    ev.newFlags = newFlags;
    ev.newDate = newDate;
    ev.sendEvent(Cmd.chgPropsRecurs);
  }
  
  
  
  
  
  /**Count the sum of length of all files in this directory tree.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param newName A new name for the file. This parameter may be null, then the old name remain.
   * @param backEvent The event for success.
   */
  public void countAllFileLength(FileRemote.CallbackEvent evback){
    CmdEvent ev = device.prepareCmdEvent(evback);
    ev.filesrc = this;
    ev.filedst = null;
    ev.sendEvent(Cmd.countLength);
  }
  
  
  
  
  
  
  public enum Cmd {
    /**Ordinary value=0, same as {@link Event.Cmd#free}. */
    free ,
    /**Ordinary value=1, same as {@link Event.Cmd#reserve}. */
    reserve,  //first 2 ordinaries from Event.Cmd
    /**Check files. */
    check,
    move,
    /**Copy to dst.*/
    copy,
    chgProps,
    chgPropsRecurs,
    countLength,
    delete,
    /**Abort the currently action. */
    abortAll,
    /**Abort the currently action. */
    abortCopyDir,
    /**Abort the currently action. */
    abortCopyFile,
    /**Last. */
    last
  }
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
  };


  
  public static class CmdEvent extends Event
  {
    FileRemote filesrc, filedst;

    /**For {@link #kChgProps}: a new name. */
    String newName;
    
    /**For {@link #kChgProps}: new properties with bit designation see {@link FileRemote#flags}. 
     * maskFlags contains bits which properties should change, newFlags contains the value of that bit. */
    int maskFlags, newFlags;
    
    long newDate;
    
    
    public CmdEvent(EventSource evSrc, Object refData, EventConsumer dst, EventThread thread, Event callback){ 
      super(evSrc, refData, dst, thread, callback); 
    }
    
    public CmdEvent(){ 
      super(); 
    }
    
    public CmdEvent(EventSource evSrc, Object refData, EventConsumer dst, EventThread thread){ 
      super(evSrc, null, dst, thread, new CallbackEvent(refData, dst, thread)); 
    }

    @Override public CallbackEvent getOpponent(){ return (CallbackEvent)super.getOpponent(); }
    

    
    public boolean sendEvent(FileRemote.Cmd cmd){ return super.sendEvent(cmd); }
    
    @Override public FileRemote.Cmd getCmd(){ return (FileRemote.Cmd)super.getCmd(); }
    
    
  }
  
  
  
  
  
  /**Type for callback notification for any action with remote files.
   * The callback type contains an opponent {@link CmdEvent} object which is not occupied initially
   * to use for forward notification of the action. But the application need not know anything about it,
   * the application should only concern with this object. 
   * See {@link CallbackEvent#CallbackEvent(Object, EventConsumer, EventThread)}.
   */
  public static class CallbackEvent extends Event
  {
    FileRemote filesrc, filedst;

    /**For {@link #kChgProps}: a new name. */
    String newName;
    
    /**For {@link #kChgProps}: new properties with bit designation see {@link FileRemote#flags}. 
     * maskFlags contains bits which properties should change, newFlags contains the value of that bit. */
    int maskFlags, newFlags;
    
    long newDate;
    
    /**callback data: the yet handled file. It is a character array because it should not need a new instance. */
    public char[] fileName = new char[100];
    
    /**callback data: number of bytes in the yet handled file.  */
    public long nrofBytesInFile;
    
    /**callback data: number of bytes for the command.  */
    public long nrofBytesAll;
    
    /**callback data: number of files in the yet handled command.  */
    public int nrofFiles;
    
    //public FileRemote getRefData(){ return (FileRemote)super.getRefData(); }
    
    /**Creates the object of a callback event inclusive the instance of the forward event (used internally).
     * @param refData The referenced data for callback, used in the dst routine.
     * @param dst The routine which should be invoked with this event object if the callback is forced.
     * @param thread The thread which stores the event in its queue, or null if the dst can be called
     *   in the transmitters thread.
     */
    public CallbackEvent(Object refData, EventConsumer dst, EventThread thread){ 
      super(null, refData, dst, thread, new CmdEvent()); 
    }
    
    
    
    public boolean sendEvent(CallbackCmd cmd){ return super.sendEvent(cmd); }
    

    @Override public CallbackCmd getCmd(){ return (CallbackCmd)super.getCmd(); }
    
    @Override public CmdEvent getOpponent(){ return (CmdEvent)super.getOpponent(); }
    
    
    /**Aborts the action which was forced forward with this callback.
     * @return true if the forward event was sent.
     */
    public boolean abort(FileRemote.Cmd cmd){
      CmdEvent ev = getOpponent();
      FileRemote fileSrc;
      FileRemoteAccessor device;
      if( ev !=null && (fileSrc = ev.filesrc) !=null && (device = fileSrc.device) !=null){
        if((ev = device.prepareCmdEvent(this)) !=null){
          return ev.sendEvent(cmd);
        } 
        else {
          return false; //event occupying fails
        } 
      }
      else {
        return false; //event is not in use.
      }
      
    }
    
    
  }
  
  
  public enum CallbackCmd {
    /**Ordinary value=0, same as {@link Event.Cmd#free}. */
    free ,
    /**Ordinary value=1, same as {@link Event.Cmd#reserve}. */
    reserve,  //first 2 ordinaries from Event.Cmd
    /**A simple done feedback*/
    done,
    /**The operation is executed, but not successfully. */
    nok,
    /**Feedback, the operation is not executed. */
    error,
    
    /**Deletion error.*/
    errorDelete,
    
    /**Done message for the {@link Cmd#check} event. The event contains the number of files and bytes.*/
    doneCheck,
    
    /**Status event with processed number of files and bytes and the currently processed file path. 
     * This is only an intermediate message. The event can be removed from queue if it isn't processed
     * and replaced by a new event with the same Event object. 
     */
    nrofFilesAndBytes,
    
    /**Status event with the currently processed directory path. 
     * This is only an intermediate message. The event can be removed from queue if it isn't processed
     * and replaced by a new event with the same Event object. 
     */
    copyDir,

    /**callback to ask what to do because the source file or directory is not able to open. */
    askErrorSrcOpen,
    
    /**callback to ask what to do because the destination file or directory is not able to create. */
    askErrorDstCreate,
    
    /**callback to ask what to do because an copy file part error is occured. */
    askErrorCopy,
    
    acknAbortAll,
    
    acknAbortDir,
    
    acknAbortFile,
    
    last
  }
  
  
}
