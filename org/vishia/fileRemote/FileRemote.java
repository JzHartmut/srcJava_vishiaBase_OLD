package org.vishia.fileRemote;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.event.Event;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread;
import org.vishia.fileLocalAccessor.FileAccessorLocalJava6;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.MarkMask_ifc;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;


/**This class stores the name and some attributes of one File. 
 * The file may be localized on any maybe remote device or it may be a normal local file.
 * The information of the file (date, size, attributes) are accessible in a fast way. 
 * But therefore they may be correct or not: A file on its data storage can be changed or removed. 
 * The data of this class should be refreshed if it seems to be necessary. 
 * Such an refresh operation can be done if there is enough time, no traffic on system level. 
 * Then the information about the file may be actual and fast accessible if an application needs one of them.
 * <br><br>
 * In comparison to the standard {@link java.io.File} the information are gotten from the operation system in any case, 
 * the access is correct but slow.
 * <br><br>
 * A remote file should be accessed by {@link FileRemoteAccessor} implementations. It may be any information
 * on an embedded hardware, not only in a standard network. For example the access to zip archives are implemented by
 * {@link FileAccessZip}. A file can be present only with one line in a text document which contains path, size, date.
 * In this case the content is not available, but the properties of the file. See {@link org.vishia.util.FileList}.
 * <br><br>
 * This class stores the following information (overview)
 * <ul>
 * <li>path, timestamp, length, flags for read/write etc. like known for file systems.
 * <li>parent and children of this file in its file tree context.
 * <li>time of last refresh: {@link #timeRefresh} and {@link #timeChildren}. With this information
 *   an application can decide whether the file should be {@link #refreshProperties(CallbackEvent)}
 *   or whether its propertees seems to be actual valid.   
 * <li>Aggregation to its {@link FileRemoteAccessor}: {@link #device} to refresh.
 * <li>Aggregation to any data which are necessary to access the physical file for the 
 *   FileRemoteAccessor implementation. This is an instance of {@link java.io.File} for local files.
 * <li>Aggregation to its {@link FileCluster}: {@link #itsCluster}. 
 *   The cluster assures that only one FileRemote instance exists for one physical file inside an application.
 *   In this case some additional properties can be stored to the FileRemote instance like selected etc.
 *   which are seen application-width.
 * <li>Information about a mark state. There are some bits for simple mark or for mark that it is
 *   equal, new etc. in any comparison. This bits can be used by applications.  
 * </ul>
 * <br><br>
 * <b>How to create instances of this class?</b><br>
 * You must create or have at least one {@link FileCluster} firstly which is application-width.
 * The FileCluster prevents more instances of FileRemote for the same physical file and supports
 * additional properties for a file as instance of FileRemote.  
 * If you have any other FileRemote instance, it knows its {@link #itsCluster}.
 * You can create a child of an existing file by given path using {@link #child(CharSequence)} whereby
 * the child can be a deeper one, the path is a relative path from the given parent. This FileRemote instance
 * describes a possible existing file or maybe a non existing one, adequate like creation of instances
 * of {@link java.io.File}. Note that on {@link #refreshPropertiesAndChildren(CallbackEvent)} this instance will be removed
 * from the children list if it is not existing.
 * <br>
 * You can get any FileRemote instance with any absolute path calling {@link FileCluster#getFile(CharSequence)}.
 * This instance will be created if it is not existing, or it will be get from an existing instance
 * of this RemoteFile inside the cluster. The instance won't be deleted if the physical file is not existing.
 * For example you can create <pre>
 *   FileRemote testFile = theFileCluster.getFile("X:/MyPath/file.ext");
 * </pre>  
 * This instance is existing independent of the existence of such a physical file. But <pre>
 *   if(testFile.exists()){....
 * </pre>
 * may return false.   
 * <br><br>
 * This class inherits from {@link java.io.File} in the determination as interface. The advantage is:
 * <ul>
 * <li>The class File defines the interface to the remote files too. No extra definition of access methods is need.
 * <li>Any reference to a this class can be store in a reference of type java.io.File. The user can deal with it
 *   without knowledge about the FileRemote concept in a part of its software. Only the refreshing
 *   should be regulated in the proper part of the application. 
 * </ul> 
 * 
 * @author Hartmut Schorrig
 *
 */
public class FileRemote extends File implements MarkMask_ifc
{
  /**interface to build an instance, which decides about the instance for physical access to files
   * in knowledge of the path. The implementing instance should be present as singleton on instantiation of this class.
   */
  public interface FileRemoteAccessorSelector
  {
    /**The structure of path, usual a start string should be decide which access instance should be used.
     * For example "C:path" accesses the local disk but "$Device:path" should access an embedded device.
     * @param sPath
     * @return
     */
    FileRemoteAccessor selectFileRemoteAccessor(String sPath);
  }

  private static final long serialVersionUID = -5568304770699633308L;

  /**Version, history and license.
   * <ul>
   * <li>2014-07-30 Hartmut new: calling {@link FileAccessorLocalJava6#selectLocalFileAlways} in {@link #getAccessorSelector()}
   *   for compatibility with Java6. There the existence of java.nio.file.Files is checked, and the File access
   *   in Java7 is used if available. So this class runs with Java6 too, which is necessary in some established environments.
   * <li>2013-10-06 Hartmut new: {@link #getChildren(ChildrenEvent)} with aborting a last request.
   * <li>2013-09-15 Hartmut new: {@link #getChildren(ChildrenEvent)} should work proper with
   *   {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
   *   The event should be called back on 300 ms with the gathered files. If the access needs more time,
   *   it should be able to have more events to present a part of files, or to abort it.
   *   The concept is tested in java-6 yet.  
   * <li>2013-09-15 Hartmut new: Ideas from Java-7 java.nio.Files in the concept of FileRemote: 
   *   The FileRemote is an instance independent of the file system and stores the file's data. 
   *   If Java-7 is present, it should be used. But the Java-6 will be held compatible.
   * <li>2013-08-09 Hartmut chg: The {@link MarkMask_ifc} is renamed, all methods {@link #setMarked(int)}
   *   etc. are named 'marked' instead of 'selected'. 
   *   It is a problem of wording: The instances are only marked, not yet 'selected'. See application
   *   of this interface in {@link org.vishia.gral.widget.GralFileSelector}: A selected line is the current one
   *   or some marked lines.
   * <li>2013-08-09 Hartmut new: implements {@link MarkMask_ifc} for the {@link #cmprResult}.
   * <li>2013-07-29 Hartmut chg: {@link #moveTo(String, FileRemote, CallbackEvent)} now have a parameter with file names. 
   * <li>2013-05-24 Hartmut chg: The root will be designated with "/" as {@link #sFile}
   * <li>2013-05-24 Hartmut new {@link #cmprResult}, {@link #setMarked(int)}
   * <li>2013-05-05 Hartmut new {@link #isTested()}
   * <li>2013-05-05 Hartmut new {@link #mkdir()}, {@link #mkdirs()}, {@link #mkdir(boolean, CallbackEvent)}, 
   *   {@link #createNewFile()}. 
   *   TODO some operation uses still the super implementation of File.
   *   But the super class File should only be used as interface.
   * <li>2013-05-05 Hartmut chg: {@link #child(CharSequence)}  accepts a path to a sub child.
   *   New {@link #isChild(CharSequence)}, {@link #isParent(CharSequence)}.  
   * <li>2013-05-04 Hartmut redesigned ctor and order of elements. sDir does not end with "/" up to now.
   * <li>2013-04-30 Hartmut new: {@link #resetMarkedRecurs(int, int[])}
   * <li>2013-04-29 Hartmut chg: {@link #fromFile(FileCluster, File)} has the FileCluster parameter yet, necessary for the concept.
   *   This method is not necessary as far as possible because most of Files are a FileRemote instance yet by reference (Fcmd app)
   * <li>2013-04-28 Hartmut chg: re-engineering check and copy
   * <li>2013-04-26 Hartmut chg: The constructors of this class should be package private, instead the {@link FileCluster#getFile(String, String)}
   *   and {@link #child(String)} should be used to get an instance of this class. The instances which refers the same file
   *   on the file system are existing only one time in the application respectively in the {@link FileRemoteAccessor}. 
   *   In this case additional information can be set to files such as 'selected' or a comparison result. It is in progression.  
   * <li>2013-04-21 Hartmut new: The {@link #flags} contains bits for {@link #mChecked} to mark files as checked
   *   with {@link #check(FileRemote, CallbackEvent)}. With that the check state is able to view.
   * <li>2013-04-21 Hartmut new: The {@link #flags} contains bits for {@link #mCmpContent} etc. for comparison to view.  
   * <li>2013-04-21 Hartmut new: {@link #copyChecked(CallbackEvent, int)} in cohesion with changes 
   *   in {@link org.vishia.fileLocalAccessor.Copy_FileLocalAcc}.
   * <li>2013-04-12 Hartmut chg: Dedicated attributes for {@link CallbackCmd#successCode} etc.
   * <li>2013-04-07 Hartmut adapt: Event<?,?> with 2 generic parameter
   * <li>2013-04-07 Hartmut chg: {@link CallbackEvent} contains all the methods to do something with currently copying files,
   *   for example {@link CallbackEvent#copyOverwriteFile(int)} etc.
   * <li>2013-03-31 Hartmut chg: Event<Type>
   * <li>2012-11-16 Hartmut chg: Usage of {@link CmdEvent#filesrc} and filedst and {@link CallbackEvent#filedst} and dst
   *   instead {@link Event#getRefData()}.
   * <li>2012-11-11 Hartmut chg: The flag bit {@link #mDirectory} should be set always, especially also though {@link #mTested} is false.
   *   That should be assured by the {@link FileRemoteAccessor} implementation.
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
   *     is not called, the {@link FileAccessorLocalJava7#selectLocalFileAlways}.
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
   *     or by an event mechanism (see {@link FileRemote.CallbackEvent} respectively {@link org.vishia.event.Event}.
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
   *   {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7} in an extra thread.
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final int version = 20130524;

  public final static int modeCopyReadOnlyMask = 0x00f
  , modeCopyReadOnlyNever = 0x1, modeCopyReadOnlyOverwrite = 0x3, modeCopyReadOnlyAks = 0;

  public final static int modeCopyExistMask = 0x0f0
  , modeCopyExistNewer = 0x10, modeCopyExistOlder = 0x20, modeCopyExistAll = 0x30, modeCopyExistSkip = 0x40, modeCopyExistAsk = 0;
  
  public final static int modeCopyCreateMask = 0xf00
  , modeCopyCreateNever = 0x200, modeCopyCreateYes = 0x300 , modeCopyCreateAsk = 0;
  
  /**Info about the file stored in {@link #flags} returned with {@link #getFlags()}. */
  public final static int  mExist =   1, mCanRead =  2, mCanWrite =  4, mHidden = 0x08;
  
  /**Info whether the File is a directory. This flag-bit should be present always independent of the {@link #mTested} flag bit. */
  public final static int  mDirectory = 0x10, mFile =     0x20;
  
  /**Info whether it is an executable (executable flag on unix)*/
  public final static int  mExecute =     0x40, mExecuteAny =     0x80;
 
  /**Type of given path. */
  public final static int  mRelativePath = 0x100, mAbsPath = 0x200;
  
  /**A symbolic link in unix. */
  public final static int  mSymLinkedPath = 0x400;
  
  /**Group and any read and write permissions. */
  public final static int  mCanReadGrp =  0x0800, mCanWriteGrp = 0x1000, mExecuteGrp =  0x2000
  , mCanReadAny =  0x4000, mCanWriteAny = 0x8000;

  
  public final static int  mShouldRefresh = 0x10000;
  
  /**Set if the file is tested physically. If this bit is not set, all other flags are not representable and the file
   * may be only any path without respect to an existing file.
   */
  public final static int  mTested =        0x20000;
  //protected final static int mChildrenGotten = 0x40000;
  
  /**Set if a thread runs to get file properties. */
  public final static int mThreadIsRunning =0x80000;

  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public final static int mCmpTimeLen = 0x03000000;
  
  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public final static int mCmpContent = 0x0C000000;
  
  /**Instance of the application-width selector for {@link FileRemoteAccessor}.
   */
  private static FileRemoteAccessorSelector accessorSelector;
  
  /**Counter, any instance has an ident number. */
  private static int ctIdent = 0;
  

  
  /**A indent number, Primarily for debug and test. */
  private final int _ident;
  
  
  /**Any FileRemote instance should be member of a FileCluster. Files in the cluster can be located on several devices.
   * But they are selected commonly.
   * */
  public final FileCluster itsCluster;
  
  /**The device which manages the physical files. For the local file system the 
   * {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7} is used. */
  protected FileRemoteAccessor device;
  
  /**A mark and count instance for this file. It is null if it is not necessary. */
  public FileMark mark;
  
  /**The last time where the file was synchronized with its physical properties. */
  public long timeRefresh, timeChildren;
  
  /**The directory path of the file. It does not end with '/' except it is the root.
   * The directory path is absolute and normalized. It doesn't contain any "/./" 
   * or "/../"-parts. 
   * <br>
   * This absolute path can contain a start string for a remote device designation.
   * <br> 
   */  
  protected final String sDir;
  
  /**The name with extension of the file or directory name. 
   * */
  protected final String sFile;
  
  /**The unique path to the file or directory entry. If the file is symbolic linked (on UNIX systems),
   * this field contains the non-linked direct path. But the {@link #sDir} contains the linked path. 
   */
  protected String sCanonicalPath;
  
  /**Timestamp of the file. */
  protected long date, dateCreation, dateLastAccess;
  
  /**Length of the file. */
  protected long length;
  
  /**Some flag bits. See constants {@link #mExist} etc.*/
  protected int flags;

  /**The parent instance, it is the directory where the file is member of. This reference may be null 
   * if the parent instance is not used up to now. If it is filled, it is persistent.
   */
  FileRemote parent;
  
  /**The content of a directory. It contains all files, proper for return {@link #listFiles()} without filter. 
   * The content is valid at the time of calling {@link FileRemoteAccessor#refreshFilePropertiesAndChildren(FileRemote, Event)}.
   * It is possible that the content of the physical directory is changed meanwhile.
   * If this field should be returned without null, especially on {@link #listFiles()} and the file is a directory, 
   * the {@link FileRemoteAccessor#refreshFilePropertiesAndChildren(FileRemote, Event)} will be called.  
   * */
  private Map<String,FileRemote> children;
  
  /**This is the internal file object. It is handled by the device only. */
  Object oFile;
  
  
  
  
  
  /**Constructs an instance. The constructor is protected because only special methods
   * constructs an instance in knowledge of existing instances in {@link FileCluster}.
   * <br><br>
   * 
   *  
   * @param cluster The cluster where the file is member of. It can be null if the parent is given. 
   *   It have to be matching to the parent. It do not be null if a parent is not given. 
   *   A cluster can be created in an application invoking the constructor {@link FileCluster#FileCluster()}.
   * @param device The device which organizes the access to the file system. It may be null, then the device 
   *   will be gotten from the parent or from the sDirP.
   * @param parent The parent file if known or null. If it is null, the sDirP have to be given with the complete absolute path.
   *   If parent is given, this file will be added to the parent as child.
   * @param sPath The path to the directory. If the parent file is given, either it have to be match to or it should be null.
   *   The standard path separator is the slash "/". 
   *   A backslash will be converted to slash internally, it isn't distinct from the slash.
   *   If this parameter ends with an slash or backslash and the name is null or empty, this is designated 
   *   as an directory descriptor. {@link #mDirectory} will be set in {@link #flags}.
   * @param length The length of the file. Maybe 0 if unknown. 
   * @param date Timestamp of the file. Maybe 0 if unknown.
   * @param flags Properties of the file. Maybe 0 if unknown.
   * @param oFileP an system file Object, may be null.
   * @param OnlySpecialCall
   */
  /*
   *    * If the length parameter is given or it is 0, 
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

   */
  protected FileRemote(final FileCluster cluster, final FileRemoteAccessor device
      , final FileRemote parent
      , final CharSequence sPath //, CharSequence sName
      , final long length, final long dateLastModified, long dateCreation, long dateLastAccess, final int flags
      , Object oFileP, boolean OnlySpecialCall) {
    //super("??"); //sDirP + (sName ==null ? "" : ("/" + sName)));  //it is correct if it is a local file. 
    super(parent == null ? sPath.toString() : parent.getPath() + "/" + sPath);  //it is correct if it is a local file. 
    if(parent !=null){
      this.parent = parent;
      this.sDir = parent.getAbsolutePath();
      this.sFile = sPath.toString();
      if(cluster !=null && cluster != parent.itsCluster){ 
        throw new IllegalArgumentException("FileRemote.ctor - Mismatching cluster association; parent.itsCluster=" 
            + parent.itsCluster.toString() + ";  parameter cluster=" + cluster.toString() + ";");
      }
      this.itsCluster = parent.itsCluster;
    } else {
      this.parent = null;
      int posSep = StringFunctions.lastIndexOf(sPath, '/');
      if(posSep >=0){
        int zPath = sPath.length();
        this.sDir = sPath.subSequence(0, posSep).toString();
        if((posSep == 0 && zPath ==1) || (posSep ==2 && sPath.charAt(1)== ':' && zPath == 3)){
          //it is the root  
          this.sFile = "/";
        } else {
          this.sFile = sPath.subSequence(posSep+1, sPath.length()).toString();
          //sFile maybe "" if sPath ends with "/"
        }
      } else {
        this.sDir = "";  //it is a local file
        this.sFile = sPath.toString();
      }
      if(cluster == null) throw new IllegalArgumentException("FileRemote.ctor - cluster is null, should be given;");
      this.itsCluster = cluster;
    }
    //super("?");  //NOTE: use the superclass File only as interface. Don't use it as local file instance.
    this._ident = ++ctIdent;
    this.device = device;
    this.flags = flags;
    Assert.check(this.sDir !=null);
    //Assert.check(sName.contains("/"));
    //Assert.check(this.sDir.length() == 0 || this.sDir.endsWith("/"));
    //TODO Assert.check(!this.sDir.endsWith("//"));
    Assert.check(length >=0);
    oFile = oFileP;
    this.length = length;
    this.date = dateLastModified;
    this.dateCreation = dateCreation;
    this.dateLastAccess = dateLastAccess;
    this.sCanonicalPath = this.sDir + (sFile !=null ? "/"  + this.sFile : "");  //maybe overwrite from setSymbolicLinkedPath
    
    ///
    if(this.device == null){
      this.device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(parent !=null){
      parent.putNewChild(this);
    }
    //System.out.println("FileRemote - ctor; " + _ident + "; " + sCanonicalPath);
  }
  
  
  
  /**Gets or creates a child with the given name. 
   * If the child is not existing, it is create as a new FileRemote instance which is not tested.
   * @param sPathChild Name or relativ pathname of the child.
   *   It can be contain "/" or "\", if more as one level of child should be created.
   * @return The child, not null.
   */
  public FileRemote child(CharSequence sPathChild){
    return child(sPathChild, 0, 0, 0,0, 0);
  }
  
  /**Returns the instance of FileRemote which is the child of this. If the child does not exists
   * it is created and added as child. That is independent of the existence of the file on the file system.
   * A non existing child is possible, it may be created on file system later.
   * <br>
   * TODO: test if the path mets a file but the path is not on its end. Then it may be a IllegalArgumentException. 
   * @param sName Name of the child or a path to a deeper sub child. If the name ends with a slash
   *   or backslash, the returned instance will be created as subdirectory.
   * @return The child instance. 
   */
  private FileRemote child(CharSequence sPathChild, int flags, int length
      , long dateLastModified, long dateCreation, long dateLastAccess 
      ) {
    CharSequence pathchild1 = FileSystem.normalizePath(sPathChild);
    StringPart pathchild;
    int posSep1;
    if(( posSep1 = StringFunctions.indexOf(pathchild1, '/', 0))>=0){
      //NOTE: pathchild1 maybe an instanceof StringPartBase too, but it acts as a CharSequence! 
      pathchild = new StringPart(pathchild1, 0, pathchild1.length());
      pathchild1 = pathchild.lento('/');  //Start with this part of pathchild.
    } else {
      pathchild = null; //only one child level.
    }
    FileRemote file = this, child;
    boolean bCont = true;
    int flagNewFile = 0; //FileRemote.mDirectory;
    do{
      child = file.children == null ? null : file.children.get(pathchild1);
      if(child == null && pathchild !=null){  //a sub directory child
        //maybe the child directory is registered already, take it.
        child = itsCluster.check(file.getAbsolutePath(), pathchild1);
      } 
      if(child == null){
        child = new FileRemote(itsCluster, device, file, pathchild1, length
            , dateLastModified,dateCreation,dateLastAccess, flagNewFile, null, true);
      }
      if(pathchild !=null){
        file = child;
        pathchild1 = pathchild.fromEnd().seek(1).lento('/');
        if(!pathchild.found()){
          //no slash on end:
          flagNewFile = FileRemote.mFile;
          pathchild.len0end();   //also referred from pathchild1
          if(pathchild.length() ==0){
            bCont = false;
          }
          pathchild = null;   //ends.
        } 
      } else {
        bCont = false;  //set in the next loop after pathchild = null.
      }
    }while(bCont);
    return child;
  }
  
 
  public FileRemote getChild(CharSequence name){
    return children == null ? null : children.get(name);
  }
  
  
  /**Gets the Index of the children sorted by name.
   * @return
   */
  public Map<String,FileRemote> children() { return children; }
  
  
  //private Map<String, FileRemote> createChildrenList(){ return new TreeMap<String, FileRemote>(); } 
  
  private Map<String, FileRemote> createChildrenList(){ return new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString); } 

 
  public static boolean setAccessorSelector(FileRemoteAccessorSelector accessorSelectorP){
    boolean wasSetAlready = accessorSelector !=null;
    accessorSelector = accessorSelectorP;
    return wasSetAlready;
  }
  
  
  static FileRemoteAccessorSelector getAccessorSelector(){
    if(accessorSelector == null){
      accessorSelector = FileAccessorLocalJava6.selectLocalFileAlways;
      // accessorSelector = FileAccessorLocalJava7.selectLocalFileAlways;
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
  public static FileRemote fromFile(FileCluster cluster, File src){
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
      FileRemoteAccessor accessor = accessorSelector.selectFileRemoteAccessor(src.getAbsolutePath()); 
      File dir1 = src.getParentFile();
      FileRemote dir, file;
      if(dir1 !=null){
        dir= cluster.getDir(dir1.getAbsolutePath());
        file = dir.child(src.getName());
      } else {
        dir = null;
        file = cluster.getDir(src.getAbsolutePath());
      }
      file.length = len;
      file.flags = fileProps;
      file.date = date;
      file.oFile = src;
      return file; //new FileRemote(accessor, dir, sPath, null, len, date, fileProps, src);
    }
  }
  
  
  
 
  public FileRemoteAccessor device() { return device; }
  
  void putNewChild(FileRemote child){
    if(children == null){
      //children = new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);  //TreeMap<String, FileRemote>();
      children = createChildrenList();  
    }
    if(child.parent != this){
      if(child.parent != null)
        assert(false);
      child.parent = this;
    }
    children.put(child.sFile, child);
  }
  
  
  public void setShouldRefresh(){
    flags |= mShouldRefresh;
  }
  
  
  public void setDirShouldRefresh(){
    if(parent !=null) parent.setShouldRefresh();
  }
  
  
  public boolean shouldRefresh(){ return (flags & mShouldRefresh )!=0; }
  
  /**Marks the file with the given bits in mask.
   * @param mask
   * @return number of Bytes (file length)
   */
  public long setMarked(int mask){
    if(mark == null){
      mark = new FileMark(this);
    }
    mark.setMarked(mask, this);
    return length();
  }
  
  /**Resets the mark bits of this file.
   * @param mask Mask to reset bits.
   * @return number of Bytes (file length)
   */
  public long resetMarked(int mask){
    if(mark != null){
      mark.setNonMarked(mask, this);
      return length();
    }
    else return 0;
  }
  
  /**Resets the mark bits of this file and all children.
   * @param mask Mask to reset mark bits.
   * @param nrofFiles Output reference, will be incremented for all files which's mark are reseted.
   *   Maybe null, the unused.
   * @return Sum of all bytes (file length) of the reseted files.
   */
  public long resetMarkedRecurs(int mask, int[] nrofFiles){
    return resetMarkedRecurs(mask, nrofFiles, 0);  
  }
  
  
  
  private long resetMarkedRecurs(int mask, int[] nrofFiles, int recursion){
    long bytes = length();
    if(nrofFiles !=null){ nrofFiles[0] +=1; }
    //if(!isDirectory() && mark !=null){
    if(mark !=null){
      mark.setNonMarked(mask, this);
    }
    if(recursion > 1000) throw new RuntimeException("FileRemote - resetMarkedRecurs,too many recursion");
    if(children !=null){
      for(Map.Entry<String, FileRemote> item: children.entrySet()){
        FileRemote child = item.getValue();
        //if(child.isMarked(mask)){
          bytes += child.resetMarkedRecurs(mask, nrofFiles, recursion +1);
        //}
      }
    }
    return bytes;
  }
  
  /**Returns the mark of a {@link #mark} or 0 if it is not present.
   * @see org.vishia.util.MarkMask_ifc#getMark()
   */
  @Override public int getMark()
  { return mark == null ? 0 : mark.getMark();
  }


  /**resets a marker bit in the existing {@link #mark} or does nothing if the bit is not present.
   * @see org.vishia.util.MarkMask_ifc#setNonMarked(int, java.lang.Object)
   */
  @Override public int setNonMarked(int mask, Object data)
  { if(mark == null) return 0;
    else return mark.setNonMarked(mask, data);
  }


  /**marks a bit in the {@link #mark}, creates it if it is not existing yet.
   * @see org.vishia.util.MarkMask_ifc#setMarked(int, java.lang.Object)
   */
  @Override public int setMarked(int mask, Object data)
  { if(mark == null){ mark = new  FileMark(this); }
    return mark.setMarked(mask, data);
  }
  

  
  public boolean isMarked(int mask){ return mark !=null && (mark.getMark() & mask) !=0; }
  
  
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
  public void _setProperties(final long length
      , final long date, long dateCreation, long dateAccess
      , final int flags
      , Object oFileP) {
    this.length = length;
    this.date = date;
    this.dateCreation = dateCreation;
    this.dateLastAccess = dateAccess;
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
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return length; 
  }

  
  @Override public long lastModified(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return date; 
  }
  
  
  /**Returns the creation time of the file if the file system supports it. 
   * Elsewhere returns 0.
   */
  public long creationTime(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return dateCreation; 
  }
  
  
  /**Returns the last access time of the file if the file system supports it. 
   * Elsewhere returns 0.
   */
  public long lastAccessTime(){ 
    if((flags & mTested) ==0){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFileProperties(this, null);
    }
    return dateLastAccess; 
  }
  
  
  @Override public boolean setLastModified(long time) {
    this.date = time;
    if(oFile !=null){
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      return device.setLastModified(this, time);
    } else {
      return super.setLastModified(time);
    }
  }

  
  public Object oFile(){ return oFile; }
  
  public void setFileObject(Object oFile){ this.oFile = oFile; }
  
  public int getFlags(){ return flags; }
  
  
  @Override public String getName(){ return sFile; }
  
  /**Returns the parent path, it is the directory path. 
   * It is define by:
   * @see java.io.File#getParent()
   * @return path without ending slash.
   */
  @Override public String getParent(){ 
    File parentFile = getParentFile();  //
    if(parentFile == null){ return sDir; }
    else { return parentFile.getAbsolutePath(); }
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
  @Override public String getPath(){ return getPathChars().toString(); } 
  
  /**Returns the same as {@link #getPath()} but presents it as the StringBuilder instance which was used
   * to concatenate. 
   * @return
   */
  public CharSequence getPathChars(){
    int zFile = sFile == null ? 0 : sFile.length();
    if(zFile > 0){ 
      int zDir = sDir == null? 0: sDir.length();
      StringBuilder ret = new StringBuilder(zDir + 1 + zFile);
      if(zDir >0){
        ret.append(sDir);
        if(sDir.charAt(zDir-1) != '/' //does not end with "/"
          && sFile.charAt(0) !='/'    //root path has "/" in sFile
        ) { 
          ret.append('/'); 
        }
      }
      ret.append(sFile);
      return ret.toString();
    } else {
      return sDir;
      /*
      int zDir = sDir.length();
      //Assert.check(sDir.charAt(zDir-1) == '/');
      if(zDir == 1 || zDir == 3 && sDir.charAt(1) == ':'){
        return sDir;  //with ending slash because it is root.
      } else {
        return sDir.substring(0, zDir-1);  //without /
      }
      */
    }    
  }
  
  @Override public String getCanonicalPath(){ return sCanonicalPath; }
  
  
  
  /**Checks whether the given path describes a child (any deepness) of this file and returns the normalized child string.
   * @param path Any path may contain /../
   * @return null if path does not describe a child of this.
   *   The normalized child path from this directory path if it is a child.
   */
  public CharSequence isChild(CharSequence path){
    if(sFile !=null) return null; //a non-directory file does not have children.
    CharSequence path1 = FileSystem.normalizePath(path);
    int zDir = sDir.length();
    int zPath = path1.length();
    if(zPath > zDir && StringFunctions.startsWith(path1, sDir) && path1.charAt(zDir)== '/'){
      return path1.subSequence(zDir+1, zPath);
    }
    else return null;
  }
  
  
  
  /**Checks whether the given path describes a parent (any deepness) of this file and returns the normalized child string.
   * @param path Any path may contain /../
   * @return null if path does not describe a parent of this.
   *   The normalized parent path  if it is a parent.
   */
  public CharSequence isParent(CharSequence path){
    CharSequence path1 = FileSystem.normalizePath(path);
    CharSequence paththis = getPathChars();
    int zThis = paththis.length();
    int zPath = path1.length();
    if(zThis > zPath && StringFunctions.startsWith(paththis, path1) && paththis.charAt(zPath)== '/'){
      return path1;
    }
    else return null;
  }
  
  
  
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
      //Assert.check(sDir.charAt(zDir-1) == '/'); //Should end with slash
      if(sFile.equals("/")){
        sParent = null;  //root has no parent.
      }
      else if(sFile == null || sFile.length() == 0){
        //it is a directory, get its parent path
        if(zDir > 1){
          pDir = sDir.lastIndexOf('/', zDir-1);
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
        if(zDir == 0 || (zDir == 2 && sDir.charAt(1) == ':')){
          //the root. 
          pDir = zDir; //return inclusive the /
          sParent = sDir.substring(0, pDir) + "/";
        } else {
          pDir = zDir; // -1;
          sParent = sDir.substring(0, pDir);
        }
      }
      if(sParent !=null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
        this.parent = itsCluster.getFile(sParent, null); //new FileRemote(device, null, sParent, null, 0, 0, 0, null); 
        if(this.parent.children == null){
          //at least this is the child of the parent. All other children are unknown yet. 
          this.parent.children = createChildrenList(); //new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);  //TreeMap<String, FileRemote>();
          this.parent.children.put(this.sFile, this);
          this.parent.timeChildren = 0; //it may be more children. Not evaluated.
        }
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
  
  
  /**Returns true if the last time of refreshing is newer than since.
   * @param since A milliseconds after 1970
   * @return true if may actual.
   */
  public boolean isTested(long since){
    if(timeRefresh < since) return false;
    if(isDirectory() && timeChildren < since) return false;
    return true;
  }
  
  
  
  
  /**Returns true if the file seems to be existing.
   * If the FileRemote instance was never refreshed with the physical file system,
   * the {@link #refreshProperties(CallbackEvent)} is called yet. But if the file was refreshed already,
   * the existing flag is returned only. To assure that the existing of the file is correct,
   * call {@link #refreshProperties(CallbackEvent)} before this call on a proper time.
   * Note that an invocation of {@link java.io.File#exists()} may have the same problem. The file
   * may exist in the time of this call, but it may not exist ever more in the future if the application
   * will deal with it. Usage of a file for opening a reader or writer without Exception is the only one
   * assurance whether the file exists really. Note that a deletion of an opened file will be prevent
   * from the operation system.
   * @see java.io.File#exists()
   */
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
  
  
  
  
  /**Returns true if the FileRemote instance was created as directory
   * or if any {@link #refreshProperties(CallbackEvent)} call before this call
   * has detected that it is an directory.
   * @see java.io.File#isDirectory()
   */
  @Override public boolean isDirectory(){ 
    //NOTE: The mDirectory bit should be present any time. Don't refresh! 
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
    String sAbsPath;
    sAbsPath = getPath(); //sDir + sFile;
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
  @Override public FileRemote[] listFiles(){
    if(children == null){
      //The children are not known yet, get it:
      if(device == null){
        device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
      }
      device.refreshFilePropertiesAndChildren(this, null);
    }
    if(children == null) { return null; }
    else {
      FileRemote[] aChildren = new FileRemote[children.size()];
      int ix = -1;
      for(Map.Entry<String, FileRemote> item: children.entrySet()){
        aChildren[++ix] = item.getValue();
      }
      return aChildren;
    }
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
  
  
  
  
  /**Gets the children of this directory maybe as part of them if the access needs more time.
   * The routine ends immediately (does not block for file access), because the access is done
   * in an extra FileRemoteAccessor-Thread. The result is supplied by the callback event.
   * @param evback This event will be used to send files after a less time (300 ms yet)
   * TODO
   * @param time The time for event.
   * @return true if the cmd was send. false if anything hangs.
   */
  public boolean getChildren(ChildrenEvent evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    CmdEvent ev = evback.prepareCmd(1000, this, device);
    if(ev!=null){
      ev.filesrc = this;
      ev.filedst = null;
      ev.sendEvent(Cmd.getChildren);
      return true;
    }
    else return false;
  }
  
  
  
  
  
  
  @Override public boolean createNewFile() throws IOException {
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.createNewFile(this, null);
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
    device.delete(this, backEvent);
    if(false && device.isLocalFileSystem()){
      boolean bOk;
      if(isDirectory()){
        bOk = FileSystem.rmdir(this);
      } else {
        bOk = ((File)oFile).delete();
      }
      backEvent.successCode = bOk? 0 : -1;
      backEvent.occupy(evSrc, true);
      backEvent.sendEvent(FileRemote.CallbackCmd.done);
    } else {
      //TODO
    }
  }
  
  
  
  /**Deletes all checked and marked files in any level of this directory.
   * @param evback This event have to be processed and returned from calling 
   *   {@link #check(String, String, CallbackEvent)}.
   * @param mode
   */
  public void deleteChecked(FileRemote.CallbackEvent evback, int mode){ ////
    CmdEvent ev = evback.getOpponent();
    if(ev.occupy(evSrc, true)){
      ev.filesrc = this;
      ev.filedst = null;
      ev.modeCopyOper = mode;
      ev.sendEvent(Cmd.delChecked);
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
      bOk =FileSystem.addFileToList(this, sPath, listFiles);
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
    } catch(Exception exc){
      bOk = false;
    }
    if(backEvent !=null){
      backEvent.occupy(evSrc, true);
      backEvent.successCode = bOk? 0 : -1;
      backEvent.sendEvent(FileRemote.CallbackCmd.done);
    }
  }
  
  
  
  
  /**Creates the directory named by this abstract pathname.
   * This routine waits for execution on the file device. If it is a remote device, it may be spend some more time.
   * See {@link #mkdir(boolean, CallbackEvent)}.
   * For local file system see {@link java.io.File#mkdir()}.
   * @return true if this operation was successfully. False if not.
   */
  @Override public boolean mkdir(){
    return mkdir(false, null);
  }
  
  
  /**Creates the directory named by this abstract pathname , including any
   * necessary but nonexistent parent directories.  Note that if this
   * operation fails it may have succeeded in creating some of the necessary
   * parent directories.<br>
   * This routine waits for execution on the file device. If it is a remote device, it may be spend some more time.
   * See {@link #mkdir(boolean, CallbackEvent)}.
   * For local file system see {@link java.io.File#mkdir()}.
   * @return true if this operation was successfully. False if not.
   */
  @Override public boolean mkdirs(){
    return mkdir(true, null);
  }
  
  
  
  /**Creates the directory named by this abstract pathname
   * @param recursively Creates necessary but nonexistent parent directories.  Note that if this
   *   operation fails it may have succeeded in creating some of the necessary
   *   parent directories.
   * @param evback If given this routine does not wait. Instead the success will be sent with the given evback
   *  to the given destination routine given in its constructor {@link CallbackEvent#CallbackEvent(EventConsumer, EventThread, EventSource)}.
   *  If not given this routine waits till execution, see {@link #mkdir()}
   * @return false if unsuccessfully, true if evback !=null or successfully. 
   */
  public boolean mkdir(boolean recursively, FileRemote.CallbackEvent evback) {
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    return device.mkdir(this, recursively, evback);
  }

  
  
  /**Compare the both given file trees. The start files will be marked with {@link FileMark#markRoot}
   * Any child file will be marked with the bits
   * <ul>
   * <li>{@link FileMark#cmpAlone}
   * <li>{@link FileMark#cmpContentEqual}
   * <li>{@link FileMark#cmpContentNotEqual}
   * </ul>
   * Any child directory and this directories will be marked with {@link FileMark#markDir}
   * and conditional additional with 
   * <ul>
   * <li>{@link FileMark#cmpFileDifferences}
   * <li>{@link FileMark#cmpMissingFiles}
   * </ul>
   * The application is responsible for removing the mark after them. The application may show the mark
   * in any kind and then invoke {@link FileRemote#resetMarked(int)}.
   * @param dir1 Start dir
   * @param dir2 The other start dir for comparison.
   */
  public static void cmpFiles(FileRemote dir1, FileRemote dir2){
    //dir1 = file1; dir2 = file2;
    if(dir1.device == null){
      dir1.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dir1.getAbsolutePath());
    }
    if(dir2.device == null){
      dir2.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dir2.getAbsolutePath());
    }
    int markReset = FileMark.markDir | FileMark.markDir | FileMark.cmpAlone | FileMark.cmpContentEqual
      | FileMark.cmpFileDifferences | FileMark.cmpContentNotEqual | FileMark.cmpMissingFiles;
    dir1.resetMarkedRecurs(markReset, null);
    dir2.resetMarkedRecurs(markReset, null);
    dir1.setMarked(FileMark.markRoot);
    dir2.setMarked(FileMark.markRoot);
    FileRemoteCallbackCmp callback = new FileRemoteCallbackCmp(dir1, dir2);
    dir1.device.walkFileTree(dir1, null, Integer.MAX_VALUE, callback);
  }
  
  

  
  /**Checks a file maybe in a remote device maybe a directory. 
   * This is a send-only routine without immediate feedback, because the calling thread should not be waiting 
   * for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method in the other execution thread
   * or communication receiving thread.
   * 
   * @param sFiles Maybe null or empty. If given, some file names separated with ':' or " : " should be used
   *   in this directory to check and select.
   * @param sWildcardSelection Maybe null or empty. If given, it is the select mask for files in directories.
   * @param evback The event instance for success. It can be contain a ready-to-use {@link CmdEvent}
   *   as its opponent. then that is used.
   */
  public void check(String sFiles, String sWildcardSelection, FileRemote.CallbackEvent evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    CmdEvent ev = device.prepareCmdEvent(500, evback);
    
    ev.filesrc =this;
    ev.filedst = null;
    ev.namesSrc = sFiles;
    ev.maskSrc = sWildcardSelection;
    //ev.data1 = 0;
    ev.sendEvent(Cmd.check);
  }

  
  
  /**Copies a file or directory tree to another file in the same device. The device is any file system maybe in a remote device.  
   * This is a send-only routine without immediate feedback, because the calling thread should not be waiting 
   * for success. The copy process is done in another thread.
   * <br><br>
   * A feedback over the progress, for quests or the success of the copy process is given with evaluating of callback events. 
   * An event instance should be provided from the caller (param evback). This event instance is used and re-used
   * for multiple callback events. If the event is occupied still from the last callback, a next status callback is suppressed.
   * Therewith a flooding with non-processed events is prevented. If a quest is necessary or the success should be announced,
   * that request should be waiting till the event is available with a significant timeout. 
   * <br><br>
   * The mode of operation, given in param mode, can be determine how several situations should be handled:
   * <ul>
   * <li>Overwrite write protected files: Bits {@link #modeCopyReadOnlyMask}
   *   <ul>
   *   <li>{@link #modeCopyReadOnlyAks}: Send a callback event with the file name, wait for answer 
   *   </ul>
   * </ul>  
   * <br><br>
   * This routine sends a {@link CmdEvent} with {@link Cmd#copyChecked} to the destination. Before that the destination 
   * for the event is set with calling of {@link FileRemoteAccessor#prepareCmdEvent(CallbackEvent)}. 
   * That completes the given {@link CallbackEvent} with the necessary {@link CmdEvent} and its correct destination {@link EventConsumer}.
   * <br><br>
   * Some status messages and the success is notified from the other thread or remote device with invocation of the 
   * given {@link CallbackEvent}. After any status message was received the {@link CmdEvent} gotten as {@link Event#getOpponent()}
   * from the received {@link CallbackEvent} can be used to influence the copy process:
   * The commands of the callback are:
   * <ul>
   * <li>{@link CallbackCmd#done}: The last callback to designate the finish of succession.
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file exists and it is readonly. It can't be set writeable 
   *   though the {@link #modeCopyReadOnlyOverwrite} designation was sent in the mode argument. 
   * <li>{@link CallbackCmd#askDstOverwr}: The destination file exits. 
   *   Because {@link #modeCopyExistAsk} is given it is the request for asking whether it should be overridden. 
   * <li>{@link CallbackCmd#askDstReadonly}: The destination file exists and it is read only. 
   *   Because the {@link #modeCopyReadOnlyAks} is given it is the request for asking whether it should be overridden though.
   * <li>{@link CallbackCmd#askErrorDstCreate}: The destination file does not exists or it exists and it is writeable or set writeable.
   *   Nevertheless the creation or replacement of the file (open for write) fails. It is possible that the medium is read only
   *   or the user has no write access to the directory. Usual the copy of that file should be skipped sending {@link Cmd#abortCopyFile}. 
   *   On the other hand the user can clarify what's happen and then send {@link Cmd#overwr} to repeat it. 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * </ul>
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param evback The event for status messages and success.
   */
  public void copyChecked(FileRemote.CallbackEvent evback, int mode){
    CmdEvent ev = evback.getOpponent();
    ev.filesrc = null;
    ev.filedst = this;
    ev.modeCopyOper = mode;
    ev.sendEvent(Cmd.copyChecked);
  }
  
  

  
  
  /**Copies a file or directory tree to another file in the same device. The device is any file system maybe in a remote device.  
   * This is a send-only routine without immediate feedback, because the calling thread should not be waiting 
   * for success. The copy process is done in another thread.
   * <br><br>
   * A feedback over the progress, for quests or the success of the copy process is given with evaluating of callback events. 
   * An event instance should be provided from the caller (param evback). This event instance is used and re-used
   * for multiple callback events. If the event is occupied still from the last callback, a next status callback is suppressed.
   * Therewith a flooding with non-processed events is prevented. If a quest is necessary or the success should be announced,
   * that request should be waiting till the event is available with a significant timeout. 
   * <br><br>
   * The mode of operation, given in param mode, can be determine how several situations should be handled:
   * <ul>
   * <li>Overwrite write protected files: Bits {@link #modeCopyReadOnlyMask}
   *   <ul>
   *   <li>{@link #modeCopyReadOnlyAks}: Send a callback event with the file name, wait for answer 
   *   </ul>
   * </ul>  
   * <br><br>
   * This routine sends a {@link CmdEvent} with {@link Cmd#copyChecked} to the destination. Before that the destination 
   * for the event is set with calling of {@link FileRemoteAccessor#prepareCmdEvent(CallbackEvent)}. 
   * That completes the given {@link CallbackEvent} with the necessary {@link CmdEvent} and its correct destination {@link EventConsumer}.
   * <br><br>
   * Some status messages and the success is notified from the other thread or remote device with invocation of the 
   * given {@link CallbackEvent}. After any status message was received the {@link CmdEvent} gotten as {@link Event#getOpponent()}
   * from the received {@link CallbackEvent} can be used to influence the copy process:
   * The commands of the callback are:
   * <ul>
   * <li>{@link CallbackCmd#done}: The last callback to designate the finish of succession.
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file exists and it is readonly. It can't be set writeable 
   *   though the {@link #modeCopyReadOnlyOverwrite} designation was sent in the mode argument. 
   * <li>{@link CallbackCmd#askDstOverwr}: The destination file exits. 
   *   Because {@link #modeCopyExistAsk} is given it is the request for asking whether it should be overridden. 
   * <li>{@link CallbackCmd#askDstReadonly}: The destination file exists and it is read only. 
   *   Because the {@link #modeCopyReadOnlyAks} is given it is the request for asking whether it should be overridden though.
   * <li>{@link CallbackCmd#askErrorDstCreate}: The destination file does not exists or it exists and it is writeable or set writeable.
   *   Nevertheless the creation or replacement of the file (open for write) fails. It is possible that the medium is read only
   *   or the user has no write access to the directory. Usual the copy of that file should be skipped sending {@link Cmd#abortCopyFile}. 
   *   On the other hand the user can clarify what's happen and then send {@link Cmd#overwr} to repeat it. 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * <li>{@link CallbackCmd#askDstNotAbletoOverwr}: The destination file 
   * </ul>
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param evback The event for status messages and success.
   */
  public void copyTo(FileRemote dst, FileRemote.CallbackEvent evback, int mode){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    if(dst.device == null){
      dst.device = getAccessorSelector().selectFileRemoteAccessor(dst.getAbsolutePath());
    }
    
    CmdEvent ev = device.prepareCmdEvent(500, evback);
    
    ev.filesrc =this;
    ev.filedst = dst;
    ev.modeCopyOper = mode;
    ev.sendEvent(Cmd.copyChecked);

  }
  
  
  
  
  /**Moves this file or some files in this directory to another file(s) maybe in a remote device.
   * If the devices are the same, it sends a commission only to the device. 
   * The action is done in the other device respectively in another thread for local files
   * in {@link org.vishia.fileLocalAccessor.FileAccessorLocalJava7#executerCommission}.
   * <br><br>
   * Depending on the file system the moving may be a copy with deleting the source. 
   * But if this and dst are at the same partition, then it is a lightweight operation. 
   * If this and dst are at different partitions, the operation file system will be copy it 
   * and then delete this. It means, this operation can be need some time for large files.
   * <br><br> 
   * If this and dst are at different devices, this routine copies and deletes.
   * <br><br>
   * It is a send-only routine without feedback in this routine, 
   * because the calling thread should not be waiting for success. The success is notified with invocation of the 
   * {@link Event#callback}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param sFiles maybe null or "". Elsewhere it contains a list of files in this directory 
   *   which are to move. The files are separated with dots and can have white spaces before and after the dot
   *   for better readability. Write for example "myfile1.ext : myfile2.ext"
   *   If it is empty, this will be moved to dst.
   * @param dst The destination for move. If dst is a directory this file or directory tree 
   *   will be copied into. If dst is an existing file nothing will be done and an error message will be fed back. 
   *    If dst does not exist this will be stored as a new file or directory tree as dst.
   *    Note that this can be a file or a directory tree.
   * @param backEvent The event for success.
   */
  public void moveTo(String sFiles, FileRemote dst, FileRemote.CallbackEvent evback){
    if(device == null){
      device = getAccessorSelector().selectFileRemoteAccessor(getAbsolutePath());
    }
    CmdEvent ev = device.prepareCmdEvent(500, evback);
    ev.filesrc = this;
    ev.namesSrc = sFiles;
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
    CmdEvent ev = device.prepareCmdEvent(500, evback);
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
    CmdEvent ev = device.prepareCmdEvent(500, evback);
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
    CmdEvent ev = device.prepareCmdEvent(500, evback);
    ev.filesrc = this;
    ev.filedst = null;
    ev.sendEvent(Cmd.countLength);
  }
  
  
  /**It sends an abort event to the execution thread or to any remote device to abort any action with files.
   * It aborts {@link #copyTo(FileRemote, CallbackEvent, int)}, {@link #check(FileRemote, CallbackEvent)},
   * 
   */
  public void abortAction(){
    CmdEvent ev = device.prepareCmdEvent(500, null);
    ev.filesrc = this;
    ev.filedst = null;
    ev.sendEvent(Cmd.abortAll);
  }
  
  
  public int ident(){ return _ident; }
  
  
  @Override public String toString(){ return super.toString(); } //sDir + sFile + " @" + ident; }
  
  public enum Cmd {
    /**Ordinary value=0, same as {@link Event.Cmd#free}. */
    free ,
    /**Ordinary value=1, same as {@link Event.Cmd#reserve}. */
    reserve,  //first 2 ordinaries from Event.Cmd
    /**Check files. */
    check,
    move,
    moveChecked,
    /**Copy to dst.*/
    copyChecked,
    chgProps,
    chgPropsRecurs,
    countLength,
    delete,
    delChecked,
    mkDir,
    mkDirs,
    /**Abort the currently action. */
    abortAll,
    /**Abort the copy process of the current directory or skip this directory if it is asking a file. */
    abortCopyDir,
    /**Abort the copy process of the current file or skip this file if it is asking. */
    abortCopyFile,
    /**Overwrite a read-only file. */
    overwr,
    /**Gets the children. */
    getChildren,
    /**Last. */
    last
  }
  
  
  /**Possibilities for comparison. */
  public enum Ecmp{ ends, starts, contains, equals, always};
  
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
  };


  
  /**Event object for all commands to a remote device or other thread for file operations. It should be used for implementations
   * of {@link FileRemoteAccessor}.
   */
  public static class CmdEvent extends Event<FileRemote.Cmd, FileRemote.CallbackCmd>
  {
    /**Source and destination files for copy, rename, move or the only one filesrc. filedst may remain null then. */
    public FileRemote filesrc, filedst;

    /**List of names separated with ':' or " : " or empty. If not empty, filesrc is the directory of this files. */
    public String namesSrc;
    
    /**Wildcard mask to mark source files if the designated source file(s) are directories. Maybe empty, then all files are used.*/
    public String maskSrc;
    
    /**Designation of destination file names maybe wildcard mask. */
    public String nameDst;
    
    /**A order number for the handled file which is gotten from the callback event to ensure thats the right file. */
    //public int orderFile;
    
    /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
    public int modeCopyOper;
    
    /**For {@link #kChgProps}: a new name. */
    String newName;
    
    /**For {@link #kChgProps}: new properties with bit designation see {@link FileRemote#flags}. 
     * maskFlags contains bits which properties should change, newFlags contains the value of that bit. */
    int maskFlags, newFlags;
    
    /**A new time stamp. */
    long newDate;
    
    
    /**
     * @param evSrc
     * @param refData
     * @param dst
     * @param thread
     * @param callback
     */
    public CmdEvent(EventSource evSrc, EventConsumer dst, EventThread thread, CallbackEvent callback){ 
      super(evSrc, dst, thread, callback); 
    }
    
    /**Creates a non-occupied empty event.
     * 
     */
    public CmdEvent(){ 
      super(); 
    }
    
    /**Creates an event.
     * The event is non-occupied if either the evSrc is null or the dst is null.
     * If both are given, the event is occupied and can be {@link #sendEvent(Cmd)}.
     * A non-occupied event can be created as static data and re-used. 
     * Use {@link #occupy(EventSource, Object, EventConsumer, EventThread, boolean)} before {@link #sendEvent(Cmd)} then.
     * @param evSrc
     * @param fileSrc
     * @param fileDst
     * @param dst
     * @param thread
     */
    public CmdEvent(EventSource evSrc, FileRemote fileSrc, FileRemote fileDst, EventConsumer dst, EventThread thread){ 
      super(evSrc, dst, thread, null); 
    }

    /** Gets the callback event which is given on construction.
     * @see org.vishia.event.Event#getOpponent()
     */
    @Override public CallbackEvent getOpponent(){ return (CallbackEvent)super.getOpponent(); }
    
    public ChildrenEvent getOpponentChildrenEvent(){ return (ChildrenEvent)super.getOpponent(); }
    

    
    @Override public boolean sendEvent(FileRemote.Cmd cmd){ return super.sendEvent(cmd); }
    
    @Override public FileRemote.Cmd getCmd(){ return super.getCmd(); }

    public final FileRemote filesrc()
    {
      return filesrc;
    }

    public final FileRemote filedst()
    {
      return filedst;
    }

    public final int modeCopyOper()
    {
      return modeCopyOper;
    }

    public final String newName()
    {
      return newName;
    }

    public final int maskFlags()
    {
      return maskFlags;
    }

    public final int newFlags()
    {
      return newFlags;
    }

    public final long newDate()
    {
      return newDate;
    }
    
    
  }
  
  
  
  
  
  /**Type for callback notification for any action with remote files.
   * The callback type contains an opponent {@link CmdEvent} object which is not occupied initially
   * to use for forward notification of the action. But the application need not know anything about it,
   * the application should only concern with this object. 
   * See {@link CallbackEvent#CallbackEvent(Object, EventConsumer, EventThread)}.
   */
  public static class CallbackEvent extends Event<FileRemote.CallbackCmd, FileRemote.Cmd>
  {
    private FileRemote filesrc, filedst;

    /**Source of the forward event, the oppenent of this. It is the instance which creates the event. */
    private final EventSource evSrcCmd;
    
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
    
    public int successCode;
    
    public int promilleCopiedFiles, promilleCopiedBytes;
    
    /**A order number for the handled file which can be used to send the proper forward command. */
    //public int orderFile;
    
    /**Creates a non-occupied event. This event contains an EventSource which is used for the forward event.
     * @param dst
     * @param thread
     * @param evSrcCmd
     */
    public CallbackEvent(EventConsumer dst, EventThread thread, EventSource evSrcCmd){ 
      super(null, dst, thread, new CmdEvent()); 
      this.evSrcCmd = evSrcCmd;
    }
    
    
    
    
    
    /**Creates the object of a callback event inclusive the instance of the forward event (used internally).
     * @param refData The referenced data for callback, used in the dst routine.
     * @param dst The routine which should be invoked with this event object if the callback is forced.
     * @param thread The thread which stores the event in its queue, or null if the dst can be called
     *   in the transmitters thread.
     */
    public CallbackEvent(EventSource evSrc, FileRemote filesrc, FileRemote fileDst
        , EventConsumer dst, EventThread thread, EventSource evSrcCmd){ 
      super(null, dst, thread, new CmdEvent(evSrc,filesrc, fileDst, null, null)); 
      this.filesrc = filesrc;
      this.filedst = fileDst;
      this.evSrcCmd = evSrcCmd;
    }
    
    public boolean occupy(EventSource source, FileRemote fileSrc, boolean expect){
      boolean bOccupied = occupy(source, expect); 
      if(bOccupied){
         this.filesrc = fileSrc;
       }
      return bOccupied;
    }
    
    public boolean occupy(EventSource source, int orderId, boolean expect){
      boolean bOccupied = occupy(source, expect); 
      if(bOccupied){
         this.orderId = orderId;
       }
      return bOccupied;
    }
    
    public void setFileSrc(FileRemote fileSrc){ this.filesrc = fileSrc; }

    
    
    @Override
    public boolean sendEvent(CallbackCmd cmd){ return super.sendEvent(cmd); }
    

    @Override public CallbackCmd getCmd(){ return super.getCmd(); }
    
    @Override public CmdEvent getOpponent(){ return (CmdEvent)super.getOpponent(); }

    public FileRemote getFileSrc(){ return filesrc; }

    public FileRemote getFileDst(){ return filedst; }
    
    
    /**Skips or aborts the copying of the last file which's name was received by the callback event. The destination file is deleted. 
     * <ul>
     * <li>If the received callback designates that the copy process is waiting for an answer, this cmd skips exact that file. 
     * <li>If the callback event does only give a status information and the copy process is running, the current copied file is aborted.
     *   This feature can be used if the file is a large file which is copied to a slow destination (network). Sometimes especially
     *   large files are not useful to copy if there are large report files or generation results etc. but such files needs unnecessary time 
     *   to copy.
     * <li>If the last callback had given a status information, the user sends the abort file, the processing of the event needs some time
     *     and the event was received later while a next file is copied, that file is not aborted. For that reason the ident number
     *     of the callback message should be copied in this event.
     * </ul>  
     *   But if the abort event is go though. It is possible to abort
     *   a long-time-copying of a large file especially in network access. The user can see the percent of progress and send this
     *   aborting command for the current file. To 

     * @param associatedCallback
     * @param modeCopyOper
     */
    public void copySkipFile(int modeCopyOper){
      FileRemote.CmdEvent evcmd = getOpponent();
      if(evcmd.occupy(evSrcCmd, true)){
        evcmd.setOrderId(orderId);
        evcmd.modeCopyOper = modeCopyOper;
        evcmd.sendEvent(FileRemote.Cmd.abortCopyFile);
      }

    }
    
    
    /**Designates that the requested file which's name was received by the callback event should be overwritten. 
     * @param associatedCallback
     * @param modeCopyOper
     */
    public void copyOverwriteFile(int modeCopyOper){
      FileRemote.CmdEvent evcmd = getOpponent();
      if(evcmd.occupy(evSrcCmd, true)){
        evcmd.setOrderId(orderId);
        evcmd.modeCopyOper = modeCopyOper;
        evcmd.sendEvent(FileRemote.Cmd.overwr);
      }

    }
    
    

    /**Designates that the directory of the requested file which's name was received by the callback event 
     * should be skipped by the copy process. The current copying file is removed. The files which were copied
     * before this event was received are not removed. 
     * @param associatedCallback
     * @param modeCopyOper
     */
    public void copySkipDir(int modeCopyOper){
      FileRemote.CmdEvent evcmd = getOpponent();
      if(evcmd.occupy(evSrcCmd, true)){
        evcmd.setOrderId(orderId);
        evcmd.modeCopyOper = modeCopyOper;
        evcmd.sendEvent(FileRemote.Cmd.abortCopyDir);
      }

    }
    
    

    /**Designates that the copy process which was forced forward with this callback should be stopped and aborted. 
     * @param associatedCallback
     * @param modeCopyOper
     * @return true if the forward event was sent.
     */
    public boolean copyAbortAll(){
      FileRemote.CmdEvent ev = getOpponent();
      FileRemote fileSrc;
      FileRemoteAccessor device;
      if( ev !=null && (fileSrc = ev.filesrc) !=null && (device = fileSrc.device) !=null){
        if((ev = device.prepareCmdEvent(500, this)) !=null){
          return ev.sendEvent(Cmd.abortAll);
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
    
    /**callback to ask what to do on a file which is existing but able to overwrite. */
    askDstOverwr,
    
    /**callback to ask what to do on a file which is existing and read only. */
    askDstReadonly,
    
    /**callback to ask that the file is not able to overwrite. The user can try it ones more or can press skip. */
    askDstNotAbletoOverwr,
    
    /**callback to ask what to do because an copy file part error is occurred. */
    askErrorCopy,
    
    acknAbortAll,
    
    acknAbortDir,
    
    acknAbortFile,
    
    last
  }

  
  protected class CallbackWait implements EventConsumer{
    public CallbackWait(){  }

    @Override public int processEvent(Event<?, ?> ev)
    {
      synchronized(FileRemote.this){
        FileRemote.this.notify();
      }
      return 1;
    }
  }
  
  
  
  
  /**This inner non static class is only intent to access from a FileRemoteAccessor to any file.
   * It is not intent to use by any application. 
   */
  public class InternalAccess{
    public int setFlagBit(int bit){ flags |= bit; return flags; }
    
    public int clrFlagBit(int bit){ flags &= ~bit; return flags; }
    
    public int clrFlagBitChildren(int bit, int recursion){
      int nrofFiles = 1;
      if(recursion > 1000) throw new IllegalArgumentException("too many recursion in directory tree");
      clrFlagBit(bit);
      if(children !=null){
        for(Map.Entry<String, FileRemote> child: children.entrySet()){
          if(child !=null){ nrofFiles += child.getValue().internalAccess().clrFlagBitChildren(bit, recursion +1); }
        }
      }
      return nrofFiles;
    }
    
    public int setFlagBits(int mask, int bits){ flags &= ~mask; flags |= bits; return flags; }

    
    public void setLengthAndDate(long length, long dateLastModified, long dateCreation, long dateLastAccess){
      FileRemote.this.date = dateLastModified;
      FileRemote.this.dateLastAccess = dateLastAccess;
      FileRemote.this.dateCreation = dateCreation;
      FileRemote.this.length = length;
    }
    
    public int setOrClrFlagBit(int bit, boolean set){ if(set){ flags |= bit; } else { flags &= ~bit;} return flags; }
  
    
    public void setRefreshed(){ flags &= ~mShouldRefresh; timeRefresh = System.currentTimeMillis(); }
    
    public void setChildrenRefreshed(){ flags &= ~mShouldRefresh; timeRefresh = timeChildren = System.currentTimeMillis(); }
    
    public void newChildren(){ children = createChildrenList(); }
    
    /**Creates a new file as child of this file. It does not add the child itself because it may be gathered
     * in an seconst container and then exchanged. Only for internal use.
     * @return The new child, contains this as parent, but not added to this.
     */
    public FileRemote newChild(final CharSequence sPath
        , final long length, final long dateLastModified, long dateCreation, long dateLastAccess, final int flags
        , Object oFileP){
      return new FileRemote(itsCluster, device, FileRemote.this, sPath, length
          , dateLastModified, dateCreation,FileRemote.this.dateLastAccess, flags, oFileP, true);
    }
    
    public void putNewChild(FileRemote child){ FileRemote.this.putNewChild(child); }

    
    public void setChildren(Map<String,FileRemote> children){
      FileRemote.this.children = children; //simple replace, the FileRemoteAccessor supplies it correct.
    }
    
  }
  
  private final InternalAccess acc_ = new InternalAccess();
  
  /**This routine is only intent to access from a FileRemoteAccessor to any file.
   * It is not intent to use by any application. 
   */
  public InternalAccess internalAccess(){ return acc_; }

  
  
  /**Event class to send children files of a directory.
   * Internally the event creates a proper {@link CmdEvent} to forward the request to the execution thread.
   * An event instance can be held persistent. It is {@link Event#occupy(EventSource, boolean)}
   * if it is used as callback.
   * 
   */
  public static class ChildrenEvent extends Event<FileRemote.CallbackCmd, FileRemote.Cmd>
  {
    private final Queue<FileRemote> newChildren = new ConcurrentLinkedQueue<FileRemote>();

    private long startTime;

    public long orderNr;
    
    public FileRemote srcFile;
    
    /**Source of the forward event, the oppenent of this. It is the instance which creates the event. */
    private final EventSource evSrcCmd;
    
    public FileFilter filter; 
    public int depth; 
  
    protected boolean finished;
    
    /**The aborted flag is set from {@link #abortCmd()}*/
    protected boolean aborted;
    
    /**Creates a non-occupied event. This event contains an EventSource which is used for the forward event.
     * @param dst The callback routine.
     * @param thread A thread to store this callback event, or null if the callback should be execute in the source thread.
     * @param evSrcCmd The event source for the opponent command event.
     */
    public ChildrenEvent(EventConsumer dst, EventThread thread, EventSource evSrcCmd){ 
      super(null, dst, thread, new CmdEvent()); 
      this.evSrcCmd = evSrcCmd;
    }
    
    
    /**Abort the pending cmd. 
     * The cmd receiver is informed about the abort request. It should not answer with this callback event
     * but it should releases it.
     * <br><br>
     * The {@link #aborted} is set firstly. If the processing of the cmd event is pending yet, the flag will be seen
     * in the execution thread in the same process space, and a long working process will be terminated.
     * It is on local file system using {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)}.
     * <br><br>
     * If the cmd works with an remote device, the cmd may be processed already, but any other thread 
     * (for example a socket receiver thread) waits for an answer. For this reason a {@link Cmd#abortAll}
     * cmd is send to the destination to end its activity.
     * <br><br>
     * The {@link #getOpponent()} cmd event is occupied again to send the new request. The aborted flag is set to false
     * of course because it's a new request.
     * <br><br>
     * If any answer for the old request is received though the abort cmd was send, it is detected by the order number.
     * <br><br>
     * Be aware that the cmd receiver has received the abort cmd but it has transmitted an answer in the same time before.
     * Be aware that the cmd receiver may be off line, in that case it can't answer.
     * The cmd event should be released in a short time for thread switch.
     * Wait for the free cmd event with {@link #occupyRecall(int, EventSource, boolean)} with a short timeout
     * if it is need for further operations.
     * 
     * @return true if aborted, false if the cmd event hangs.  
     */
    public CmdEvent prepareCmd(int timeout, FileRemote file, FileRemoteAccessor device){
      int ok = 2;
      CmdEvent cmd;
      if(srcFile !=null){
        cmd = getOpponent();
        //any other request is pending, abort it.
        aborted = true;  //maybe recognized in the other thread already if the cmd is proceeding yet.
        srcFile = null;
        ok = cmd.occupyRecall(timeout, evSrcCmd, false); //should be proceeded in 1 second.
        if(ok == 1){ //only if it was processed 
          cmd.sendEvent(Cmd.abortAll);
        }
      }
      if(ok !=0){
        cmd = device.prepareCmdEvent(500, this);  //NOTE: it may should wait for cmd.sendEvent(Cmd.abortAll) 
        orderNr +=1;
        srcFile = file;
        aborted = false;
        return cmd;
      } else {
        return null;  //not available, anything hangs
      }
    }
    
    @Override public CmdEvent getOpponent(){ return (CmdEvent)super.getOpponent(); }

    /**Quest whether it is the last callback.
     * @return
     */
    public boolean isFinished(){ return finished; }

    /**Polls one file from the queue. If the return value is null, the queue is empty yet
     * and this action should be terminate. The event will be send newly if more files are available.
     * @return
     */
    public FileRemote poll(){
      FileRemote file = newChildren.poll();
      if(file == null){
        startTime = System.currentTimeMillis();
      }
      return file;
    }
  
  
    protected void offerChild(FileRemote child){
      newChildren.offer(child);
      long time = System.currentTimeMillis();
      if(time - startTime > 300 && !isOccupied()){  
        //after a minimum of time and only if the last evaluation of the event is finished:
        occupy(evSrcCmd, false);
        finished = false;
        sendEvent();
      }
      
    }

    /**The implementation of the callback interface 
     * for {@link FileRemoteAccessor#walkFileTree(FileRemote, FileFilter, int, org.vishia.fileRemote.FileRemoteAccessor.CallbackFile)}
     * It is used in {@link FileRemote#getChildren(ChildrenEvent)}.
     * It is an protected non-static inner instance of the class {@link ChildrenEvent} because its implementing routines
     * should have access to the event data, especially {@link ChildrenEvent#newChildren}.
     */
    public FileRemoteAccessor.CallbackFile callbackChildren = new FileRemoteAccessor.CallbackFile()
    {

      @Override public Result offerDir(FileRemote file){
        //offerChild(file);
        return Result.cont;
      }
      
      @Override public Result finishedDir(FileRemote file){
        return Result.cont;      
      }
      
      

      /* (non-Javadoc)
       * @see org.vishia.fileRemote.FileRemoteAccessor.CallbackFile#offerFile(org.vishia.fileRemote.FileRemote)
       */
      @Override public Result offerFile(FileRemote file)
      {
        offerChild(file);
        return Result.cont;
      }

      /**It is the last action from get children.
       * If there are some files yet in the queue, send the event for the last time.
       * But wait till the other thread has finished it.
       */
      @Override public void finished()
      { if(0 != occupyRecall(4000, evSrcCmd, false)){
          finished = true;
          sendEvent();
        } else { //receiver may hang.
        }
      }

      @Override public void start()
      {
        startTime = System.currentTimeMillis();
        finished = false;
      }
      
      @Override public boolean shouldAborted(){
        return aborted;
      }

    };
    
    
    
    
  }
  

  
  
}
