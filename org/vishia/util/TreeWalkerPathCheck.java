package org.vishia.util;

import org.vishia.util.SortedTreeWalkerCallback.Result;

/**This class is a tree walker which checks the names of nodes with a mask path.
 * @author Hartmut Schorrig
 *
 * @param <Type>
 */
public class TreeWalkerPathCheck<Type extends TreeNodeNamed_ifc>
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

  final SortedTreeWalkerCallback<Type> callback;
  
  final PathCheck check;
  
  public TreeWalkerPathCheck(SortedTreeWalkerCallback<Type> callback, String sPathCheck) {
    this.callback = callback;
    this.check = new PathCheck(sPathCheck);
  }
  
  public void start(Type startNode){ callback.start(startNode); }

  public SortedTreeWalkerCallback.Result offerParentNode(Type node, PathCheck check, PathCheck[] checkRet)
  {
    String sName = node.getName();
    PathCheck use =  check == null ? this.check : check;
    PathCheck ret = use.check(sName, true);
    checkRet[0] = ret;
    if(ret == null){ return Result.skipSubtree; }
    else return callback.offerParentNode(node);
  }

  public org.vishia.util.SortedTreeWalkerCallback.Result finishedParentNode(Type parentNode,
      org.vishia.util.SortedTreeWalkerCallback.Counters cnt, PathCheck check, PathCheck[] checkRet)
  {
    checkRet[0] = check.bAllTree ? check : check.parent;
    return callback.finishedParentNode(parentNode, cnt);
  }

  public org.vishia.util.SortedTreeWalkerCallback.Result offerLeafNode(Type leafNode, PathCheck check)
  {
    String sName = leafNode.getName();
    PathCheck use =  check == null ? this.check : check;
    PathCheck ret = use.next !=null ? null : use.check(sName, false); //it should be the last.
    if(ret == null){ return Result.skipSubtree; }
    else return callback.offerLeafNode(leafNode);
  }

  public void finished(Type startNode, org.vishia.util.SortedTreeWalkerCallback.Counters cnt)
  {
    callback.finished(startNode, cnt);
    
  }

  public boolean shouldAborted()
  {
    return callback.shouldAborted();
  }
  
}
