package org.vishia.util;

/**This class supports working with a short time which may be count in microseconds or any other unit, and its conversion
 * to an absolute timestamp.
 * @author Hartmut Schorrig
 *
 */
public class Timeshort {
  
  /**Version, history and license.
   * <ul>
   * <li>2012-10-14 Hartmut created as util class from well known usage.
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final int version = 20121014;
  
  
  
  /**The shorttime-stamp to the {@link #absTime} timestamp. Set with {@link GralCurveView#setTimePoint(long, int, float)}. */
  public int absTime_short;
  
  /**Any absolute  timestamp to the {@link #absTime_short}. Set with {@link GralCurveView#setTimePoint(long, int, float)}. */
  public long absTime;
  
  /**Milliseconds for 1 step of shorttime. */
  public float absTime_Millisec7short = 1.0f;


  public Timeshort(){}
  
  public Timeshort(Timeshort src){
    synchronized(this){
      absTime_short = src.absTime_short;
      absTime = src.absTime;
      absTime_Millisec7short = src.absTime_Millisec7short;
    }
  }
  
  
  public synchronized long absTimeshort(int timeshort){
    return (long)((timeshort - absTime_short) * absTime_Millisec7short) + absTime;
  }

  
  public synchronized void setTimePoint(long date, int timeshort, float millisecPerTimeshort){
    absTime_short = timeshort;
    absTime_Millisec7short = millisecPerTimeshort;
    absTime = date; //graphic thread: now complete and consistent.
  }
  
  
  public float millisec7short(){ return absTime_Millisec7short; }
  
  public synchronized float millisecShort(int timeshort){ return absTime_Millisec7short * (timeshort - absTime_short); }
  
  
}
