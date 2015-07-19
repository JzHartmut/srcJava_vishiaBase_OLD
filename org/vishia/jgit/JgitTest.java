package org.vishia.jgit;

import org.vishia.util.Jgit_ifc;
//D:/vishia/Java/srcJava_vishiaBase/org/vishia/jgit/JgitTest.java
//==JZcmd==
//JZcmd java org.vishia.jgit.JgitTest.jztest();
//##JZcmd Obj a = java org.vishia.util.test.TestCalculatorExpr.testSimpleExpression();
//==endJZcmd==

public class JgitTest
{
  
  
  
  public static void jztest(){
    JgitFactory factory = new JgitFactory();
    Jgit_ifc git = factory.getRepository("D:/Bzr/Work-Git/vishia/docuSrc/.git", "D:/vishia/docuSrc");
    String status = git.status();
    System.out.println(status);
  }
}
