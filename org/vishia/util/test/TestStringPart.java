package org.vishia.util.test;

import org.vishia.util.Assert;
import org.vishia.util.StringPart;

public class TestStringPart
{
  public static void main(String[] args){
    test_getCircumScriptionToAnyChar();
  }
  
  
  private static void test_getCircumScriptionToAnyChar()
  {
    StringPart sp = new StringPart("y\\<\\:arg\\><textExpr?argExpr>");
    sp.seek(1);
    String res = sp.getCircumScriptionToAnyChar("<?");
    Assert.check(res.equals("\\<\\:arg\\>"));
  }

}
