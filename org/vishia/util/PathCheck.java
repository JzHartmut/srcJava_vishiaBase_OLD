package org.vishia.util;

/**This class helps to check names whether they match with a path.
 * It is used especially to select files in a file tree, but it can be used for all other types of trees with named nodes too.
 * @author Hartmut Schorrig
 *
 */
public class PathCheck
{
  /**Version, history and license.
   * <ul>
   * <li>2015-05-25 Hartmut created for walking through a file tree but with universal approach.                  
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
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final String sVersion = "2015-05-25";

  /**Position in the sPath for this level.
   * 
   */
  final int pos, end;
  
  /**Start end end String of the name, null then no check. 
   * name.startsWith(start) && name.endswith(end) 
   * <table>
   * <tr><th>bEqu <th>start<th>sEnd  <th>check</tr>
   * <tr><td>false<td>given<td>null<td> name.starts(start) </tr>
   * <tr><td>false<td>given<td>given<td> name.startsWith(start) && name.endsWith(end) </tr>
   * <tr><td>false<td>null<td>given<td> name.endsWith(end) </tr>
   * <tr><td>true <td>given<td>unused<td> name.equals(start) </tr>
   * </table>
   * */
  final String start, sEnd;
  
  final boolean bEqu;
  
  final boolean bAllTree;
  
  final PathCheck parent;
  
  final PathCheck next;
  
  public PathCheck(String sPath){ this(sPath, null); }
  
  
  private PathCheck(String sPath, PathCheck parent) {
    this.parent = parent; // != null && parent.bAllTree ? parent: null; 
    int end1;
    if(parent == null) {
      pos = 0;
      end1 = sPath.indexOf('/');
    } else {
      pos = parent.end +1;
      end1 = sPath.indexOf('/', pos);
    }
    end = end1 < 0 ? sPath.length() : end1;
    int asterisk = sPath.indexOf('*', pos);
    bEqu = asterisk < 0 || asterisk >= end;
    if(bEqu) {
      bAllTree = false;
      start = sPath.substring(pos, end);
      sEnd = null;
    } else {
      start = asterisk == pos ? null : sPath.substring(pos, asterisk);
      if(asterisk == end-1 ) {
        sEnd = null;
        bAllTree = false;
      } else if(sPath.charAt(asterisk+1) == '*'){
        sEnd = sPath.substring(asterisk+2, end);
        bAllTree = true;
      } else {
        sEnd = sPath.substring(asterisk+1, end);
        bAllTree = false;
      }
    }
    if(end < sPath.length()) {
      next = new PathCheck(sPath, this);
    } else {
      next = null;
    }
  }
  
  
  /**Checks whether the name matches.
   * It returns null if the name does not match.
   * It returns this if the name matches with this.
   * It returns a parent if the name does not match with this but a parent checks "all tree" and the name matches. 
   * @param name
   * @return If not null if matches. The returned instance should be the parent for all sub instances.
   */
  PathCheck check(String name, boolean checkParent) {
    PathCheck check = null;
    if(bEqu && name.equals(start)) check = this;
    else if((start == null || name.startsWith(start)) && (sEnd == null || name.endsWith(sEnd))) { 
      check = this;
    } else if( checkParent && parent!= null && parent.bAllTree) {
      check = parent.check(name, false);
    } else {
      check = null;
    }
    return check;
  }
  
}
