package org.vishia.test;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.IndexMultiTable;

public class TestIndexMultiTable
{

  static class Test{
    String name;
    Test(String name){ this.name = name; }
  }
  
  IndexMultiTable.Provide<String> provider = new IndexMultiTable.Provide<String>(){

    @Override public String[] genArray(int size){ return new String[size]; }

    @Override public String genMax(){ return "zzzzzzzzzzzzz"; }

    @Override public String genMin(){ return " "; }
  };
  
  
  
  void test(){
    IndexMultiTable<String, Test> idx = new IndexMultiTable<String, Test>(provider);
    //Map<String, Test> idx = new TreeMap<String, Test>();
    idx.put("b2", new Test("b2"));    
    idx.put("n2", new Test("n2"));    
    idx.put("z2", new Test("z2"));    
    idx.put("m2", new Test("m2"));    
    idx.put("m1", new Test("m1"));    
    idx.put("ab", new Test("ab1"));    
    idx.put("ab", new Test("ab2"));    
    idx.put("d2", new Test("d2a"));    
    idx.put("d2", new Test("d2b"));    
    idx.put("d3", new Test("d3"));    
    idx.put("d4", new Test("d4"));    
    idx.put("d5", new Test("d5"));    

    Test value = idx.get("b21");
    System.out.println(value.name);
    
    Iterator<Test> iter = idx.iterator("d");
    Assert.stop();
    while(iter.hasNext()){
      Test obj = iter.next();
      System.out.println(obj.name);
    }
    
  }
  
  public static void main(String[] args){
    TestIndexMultiTable main = new TestIndexMultiTable();
    main.test();
  }
}
