package org.vishia.fileLocalAccessor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteAccessor;
import org.vishia.fileRemote.FileRemote.CallbackEvent;
import org.vishia.fileRemote.FileRemote.CmdEvent;

public class FileAccessorLocalJava7 implements FileRemoteAccessor
{

  @Override
  public boolean createNewFile(FileRemote file, CallbackEvent callback) throws IOException
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean delete(FileRemote file, CallbackEvent callback)
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<File> getChildren(FileRemote file, FileFilter filter)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isLocalFileSystem()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean mkdir(FileRemote file, boolean subdirs, CallbackEvent callback)
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public InputStream openInputStream(FileRemote file, long passPhase)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ReadableByteChannel openRead(FileRemote file, long passPhase)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WritableByteChannel openWrite(FileRemote file, long passPhase)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CmdEvent prepareCmdEvent(CallbackEvent evBack)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void refreshFileProperties(FileRemote file, CallbackEvent callback)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void refreshFilePropertiesAndChildren(FileRemote file, CallbackEvent callback)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void close() throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean setLastModified(FileRemote file, long time)
  {
    // TODO Auto-generated method stub
    return false;
  }
  
}
