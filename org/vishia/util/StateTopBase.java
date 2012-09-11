package org.vishia.util;

public abstract class StateTopBase<EnumState extends Enum<EnumState>, Environment> 
extends StateComboBase<StateTopBase<EnumState, Environment>,EnumState,EnumState, Environment>{

  enum ENull{ Null}
  
  
  protected StateTopBase(EnumState id, Environment env) {
    super(null, null, id, env);
  }


  @Override final public int trans(Event ev){ return 0; }
  
}
