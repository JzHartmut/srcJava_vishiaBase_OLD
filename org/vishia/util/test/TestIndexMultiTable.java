package org.vishia.util.test;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;

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
    idx.add("b2", new Test(",b2"));    
    idx.add("n2", new Test(",n2"));    
    idx.add("z2", new Test(",z2"));    
    idx.add("m2", new Test(",m2"));    
    idx.add("m1", new Test(",m1"));    
    idx.add("ab", new Test(",ab1"));    
    idx.add("ab", new Test(",ab2"));    
    idx.add("d2", new Test(",d2a"));    
    idx.add("d2", new Test(",d2b"));    
    idx.add("d3", new Test(",d3"));    
    idx.add("d4", new Test(",d4"));    
    idx.add("i1", new Test(",i1"));    
    idx.add("j1", new Test(",j1"));    
    idx.add("k1", new Test(",k1"));    
    idx.add("i2", new Test(",i2"));    
    idx.add("j2", new Test(",j2"));    
    idx.add("k2", new Test(",k2a"));    
    idx.add("l1", new Test(",l1"));    
    idx.add("k2", new Test(",k2b"));    
    idx.add("b2", new Test(",b2b"));    
    idx.add("ac", new Test(",ac"));    
    idx.add("c5", new Test(",c5"));    

    Test value = idx.search("b21");
    System.out.println(value.name);
    StringBuilder utest = new StringBuilder(); 
    //check sorted content:
    for(Test test: idx){
      utest.append(test.name);
    }
    Assert.check(StringFunctions.equals(utest, ",ab1,ab2,ac,b2,b2b,c5,d2a,d2b,d3,d4,i1,i2,j1,j2,k1,k2a,k2b,l1,m1,m2,n2,z2"));
    //
    //Iterator starting from any point between:
    //
    utest.setLength(0);
    Iterator<Test> iter = idx.iterator("d");
    Assert.stop();
    while(iter.hasNext()){
      Test obj = iter.next();
      utest.append(obj.name);
      //System.out.println(obj.name);
    }
    Assert.check(StringFunctions.equals(utest, ",d2a,d2b,d3,d4,i1,i2,j1,j2,k1,k2a,k2b,l1,m1,m2,n2,z2"));
    
  }
  
  public static void main(String[] args){
    TestIndexMultiTable main = new TestIndexMultiTable();
    main.test();
  }
}
