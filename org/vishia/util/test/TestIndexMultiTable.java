package org.vishia.util.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
    @Override public String toString(){ return name; }
  }
  
  IndexMultiTable.Provide<String> provider = new IndexMultiTable.Provide<String>(){

    @Override public String[] genArray(int size){ return new String[size]; }

    @Override public String genMax(){ return "zzzzzzzzzzzzz"; }

    @Override public String genMin(){ return " "; }
  };
  
  
  IndexMultiTable<String, Test> idx = new IndexMultiTable<String, Test>(provider);
  Map<String, Object> idx2 = new TreeMap<String, Object>();
  
  void test(){
    idx.shouldCheck(true);
    //Map<String, Test> idx = new TreeMap<String, Test>();
    Test next;
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
    idx.add("d2", new Test(",d2c"));    
    idx.add("d2", next = new Test(",d2e"));    
    idx.add("j2", new Test(",j2"));    
    idx.add("k2", new Test(",k2a"));    
    idx.add("d2", new Test(",d2f"));    
    idx.add("d2", new Test(",d2f1"));    
    idx.add("d2", new Test(",d2f2"));    
    idx.add("d2", new Test(",d2f3"));    
    idx.add("d2", new Test(",d2f4"));    
    idx.add("d2", new Test(",d2f5"));    
    idx.add("d2", new Test(",d2f6"));    
    idx.add("d2", new Test(",d2f7"));    
    idx.add("d2", new Test(",d2f8"));    
    idx.add("d2", new Test(",d2f9"));    
    idx.add("d2", new Test(",d2fa"));    
    idx.add("d2", new Test(",d2fb"));    
    idx.add("d2", new Test(",d2fc"));    
    idx.add("d2", new Test(",d2fd"));    
    idx.add("d2", new Test(",d2fe"));    
    idx.add("d2", new Test(",d2ff"));    
    idx.add("d2", new Test(",d2fg"));    
    idx.add("d2", new Test(",d2g"));    
    idx.add("l1", new Test(",l1"));    
    idx.add("k2", new Test(",k2b"));    
    idx.add("b2", new Test(",b2b"));    
    idx.add("ac", new Test(",ac"));    
    idx.addBefore("d2", new Test(",d2d"), next);    
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
  
  
  
  
  void testFile(File file){
    idx.shouldCheck(true);
    try{
      BufferedReader rd = new BufferedReader(new FileReader(file));
      String line;
      while((line = rd.readLine())!=null){
        int len = line.length();
        int lenKey = 5;
        for(int iline = 0; iline < len-lenKey; iline += lenKey){
          String key = line.substring(iline, iline + lenKey);
          Test value = new Test(key);
          idx.add(key, value);
          addTreemap(idx2,key, value);
        }
      }
    } catch(IOException exc){
      System.err.println("TestIndexMultiTable - IOException; "+ exc);
    }
    Iterator<Map.Entry<String, Object>> iterTreemap = idx2.entrySet().iterator();
    Iterator<Test> iterList = null;
    for(Map.Entry<String, Test> entry: idx.entrySet()){
      Test value = entry.getValue();
      
      //get the value2
      Test value2;
      if(iterList !=null && iterList.hasNext()){
        value2 = iterList.next();
      } else {
        Map.Entry<String, Object> entryTreemap = iterTreemap.next();
        Object treemapitem = entryTreemap.getValue();
        if(treemapitem instanceof ArrayList<?>){
          @SuppressWarnings("unchecked")
          ArrayList<Test> node = (ArrayList<Test>) treemapitem;
          iterList = (node).iterator();
          value2 = iterList.next();
        } else {
          value2 = (Test)treemapitem;
          iterList = null;
        }
      }
      assert(value == value2); //same order, same instances.
    }
  }
  
  
  
  
  
  /**This method fills a Map with maybe Objects with the same key.
   * All Objects with the same key are stored in an ArrayList, which is the member of the idx for this key.
   * @param idx Any Map, unified key
   * @param key The key
   * @param value The object
   */
  public static void addTreemap(Map<String, Object> idx, String key, Test value){
    Object valTreemap = idx.get(key);  //key containing?
    if(valTreemap!=null){
      //key already containing
      if(valTreemap instanceof ArrayList<?>){
        @SuppressWarnings("unchecked")
        ArrayList<Test> listnode = (ArrayList<Test>) valTreemap;
        listnode.add(value);  //append
      } else {
        assert(valTreemap instanceof Test);
        ArrayList<Test> listnode = new ArrayList<Test>();
        listnode.add((Test)valTreemap);   
        listnode.add(value);
        idx.put(key, listnode);
      }
    } else {
      idx.put(key, value);
    }
  }
  
  
  
  
  
  public static void main(String[] args){
    TestIndexMultiTable main = new TestIndexMultiTable();
    //main.test();
    main.testFile(new File("D:/vishia/Java/links.html")); //any text file to test
  }
}
