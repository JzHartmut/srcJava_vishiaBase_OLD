package org.vishia.util.test;

import org.vishia.util.Assert;

public class TestCalculatorExpr
{
  public static void main(String[] args){
    testParenthesis();
  }

  
  
  private static void testParenthesis(){
    float y = 5+ ~- -(3*4);
    boolean b = !(13>12);
    Assert.check(y==0);
  }
  
  
}
