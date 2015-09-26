package org.vishia.math;

import java.util.Arrays;

import org.vishia.util.Debugutil;


//Test with JZcmd:
//==JZcmd==
//JZcmd main(){ 
//JZcmd   Obj result = java org.vishia.math.CurveApproximation.test(); 
//JZcmd   Obj plotWindow = java org.vishia.gral.test.GralPlotWindow.create("Test CurveApproximation");
//JZcmd   Obj plot = plotWindow.canvas();
//JZcmd   Obj color = java org.vishia.gral.ifc.GralColor.getColor("red");
//JZcmd   Obj scaling = plot.userUnitsPerGrid(-5, -5, 0.4, 0.4);
//JZcmd   plot.drawLine(color, scaling, result);
//JZcmd   Obj color = java org.vishia.gral.ifc.GralColor.getColor("gn");
//JZcmd   plot.drawLine(color, scaling, java org.vishia.math.CurveApproximation.pointsTest());
//JZcmd   plotWindow.waitForClose();
//JZcmd 
//JZcmd 
//JZcmd 
//JZcmd 
//JZcmd }
//==endJZcmd==


/**This class contains some static methods for curve interpolation with given setting points.
 * 
 * @author Hartmut Schorrig
 *
 */
public class CurveInterpolation
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-09-26 Hartmut creation.   
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
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut@vishia.org
   * 
   */
  public static final String sVersion = "2015-09-26";

  
  /**This spline interpolation has the following propertied:
   * <ul>
   * <li>Any y-value in points is exactly represented by the associated x input. It means any point in points
   *   is exactly represented in the result. 
   * <li>The middle point of a segment in points is exactly represented in the result.
   * <li>The slope (dx/dreturn) left and right from a point in points is identically, it is the middle value of slope of the both segments.
   * <li>The slope in the middle of a segment is exact the slope of the segment.   
   * </ul>
   * See the following plot. The red curve is a result curve, the green curve is the curve of points with 4 points.
   * <img src="../../../../img/CurveInterpolation.png" />
   * 
   * @param x input
   * @param points array of ordered pair of numbers. points[...][0] is the x value. points[...][1] is the associated y-value.
   *   The x values have to be increasing.    
   * @param ixstart -1 if not used, possible index to enforce the search of x input in points. Result of {@link #search(float, float[][], int)}. 
   * @return The associated y value to x.
   */
  public static float splineInterpolation(float x, float[][] points, int ixstart) {
    int ix = search(x, points, ixstart);  //search x in points.
    //testIndex[++testIndex[0]] = ix;
    float d0, d1, d2;
    float x0, x1 = points[ix][0], x2;
    float y0, y1 = points[ix][1], y2;
    if(ix == 0){
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      d1 = (points[ix+1][1] -  points[ix  ][1]) / (points[ix+1][0] - points[ix][0]);
      d0 = d1;
      d2 = (points[ix+2][1] -  points[ix+1][1]) / (points[ix+2][0] - points[ix+1][0]);
    } else if (ix < points.length -2){
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      d0 = (points[ix  ][1] -  points[ix-1][1]) / (points[ix  ][0] - points[ix-1][0]);
      d1 = (points[ix+1][1] -  points[ix  ][1]) / (points[ix+1][0] - points[ix  ][0]);
      d2 = (points[ix+2][1] -  points[ix+1][1]) / (points[ix+2][0] - points[ix+1][0]);
    } else if (ix < points.length -1){
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      d0 = (points[ix  ][1] -  points[ix-1][1]) / (points[ix  ][0] - points[ix-1][0]);
      d1 = (points[ix+1][1] -  points[ix  ][1]) / (points[ix+1][0] - points[ix  ][0]);
      d2 = d1; //(points[ix+2][1] -  points[ix+1][1]) / (points[ix+2][0] - points[ix+1][0]);
    } else { //ix in last segment
      x2 = points[ix][0];
      y2 = points[ix][1];
      d0 = (points[ix  ][1] -  points[ix-1][1]) / (points[ix  ][0] - points[ix-1][0]);
      d1 = d2 = d0;
    }
    float dl = (d0 + d1)/2;  //slope on left point is arithmetic mean between left and used segment.
    float dr = (d1 + d2)/2;  //slope on right point is arithmetic mean between used and right segment.
    float xs = (x1 + x2)/2;  //half between x1..x2, half of segment.
    float ys = y1 + d1 * (xs - x1);  //exactly y in the middle of the used segment.
    float y;
    if(x == 7.25f){
      Debugutil.stop();
    }
  
    if(x < xs){         //left side in segment:
      float dx = x - x1;
      float fs = 1.0f - dx / (xs - x1);  //1.0 on left point, 0 in middle of segment.
      fs *= fs;                          //use 0.25 on quarter x of segment, more middle of segment.
      float y1y = y1 + dl * dx;          //value with slope on left point.
      float ysy = ys - d1 * (xs - x);    //value with slope of the segment from middle point of the segment.
      y = (fs) * y1y + (1-fs) * ysy;     //Use mean value weighted with position in segment.
    } else {            //right side in segment:
      float dx = x - xs;
      float fs = xs < x2 ? (x - xs) / (x2 - xs) : 1.0f;  //after right point, xs is x2
      fs *= fs;                          //use 0.25 on quarter x of segment, more middle of segment.
      float ysy = ys + d1 * (x - xs);    //value with slope of the segment from middle point of the segment.
      float y2y = y2 - dr * (x2 - x);    //value with slope on right point.
      y = (1-fs) * ysy + (fs) * y2y;     //Use mean value weighted with position in segment.
    }
    return y;
  }
  
  
  
  /**Binary search of x in points[...][0]
   * @param x
   * @param points
   * @param ixstart check whether x is in the correct segment, then fast return.
   * @return
   */
  public static int search(float x, float[][] points, int ixstart) {
    int zPoints = points.length;
    if(ixstart >=0 && points[ixstart][0] <= x && (ixstart >= zPoints-1 || x <=points[ixstart+1][0]) ){ 
      return ixstart;
    } else {
      //binary search
      int low = 0;
      int high = zPoints;
      int mid = 0;
      while (low < (high-1)) 
      {
        mid = (low + high) >> 1;
        if ( x >= points[mid][0])
        { low = mid;
          //equal = false;
          //if(x < points[mid+1][0]) {
          //  high = mid;
          //}
        }
        else { // dont check : if(cmp >0) because search always to left to found the leftest position
          high = mid;   //search in left part also if key before mid is equal
          mid = high;  //if it is the end
        }
      }
      return low;
      //return low > mid ? low : mid;
    }
  }
  
  
  public static float[][] pointsTest = {{0, 1}, {5, 10}, {10, 1.0f}, { 20, 14f}, {30, 20.0f}};
  
  
  public static float[][] pointsTest(){ return pointsTest; }
  
  private static int[] testIndex = new int[200];
  
  public static float[][] test(){
    Arrays.fill(testIndex, 0);
    //float[] xtest = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    float[][] result = new float[140][2];
    for(int ix = 0; ix < 140; ++ix){
      float x = (0.25f * ix) -2;
      result[ix][0] = x;
      result[ix][1] = splineInterpolation(x, pointsTest, -1);
    }
    return result;
  }
  
}
