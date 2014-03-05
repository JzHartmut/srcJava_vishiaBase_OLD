package org.vishia.cmd;

public class ZGenFileset
{
  
  final ZGenExecuter.ExecuteLevel zgen;
  final ZGenScript.UserFileset data;
  
  public ZGenFileset(ZGenExecuter.ExecuteLevel zgen, ZGenScript.UserFileset data){
    this.zgen = zgen;
    this.data = data;
  }
  
}
