package org.vishia.util;

public class DateOrder {

  private static int orderInTime = 0;
  
  private static long lastTime = 0;
  
  

  
  public final long date;
  public final int order;
  
  public DateOrder(){
    date = System.currentTimeMillis();
    if(date == lastTime){
      orderInTime +=1;
    } else {
      lastTime = date;
      orderInTime = 0;
    }
    order = orderInTime;
    
  }
  
}
