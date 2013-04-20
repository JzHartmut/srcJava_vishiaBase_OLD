package org.vishia.test;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
    idx.put("b2", null);    
    idx.put("n2", null);    
    idx.put("z2", null);    
    //idx.put("m2", null);    
    //idx.put("m1", null);    
    //idx.put("ab", null);    
    //idx.put("ab", null);    
    idx.put("c2", null);    
    idx.put("d2", new Test("d2"));    
    idx.put("d3", new Test("d3"));    
    idx.put("d4", new Test("d4"));    
    idx.put("d5", new Test("d5"));    

    Iterator<Test> iter = idx.iterator("d");
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
