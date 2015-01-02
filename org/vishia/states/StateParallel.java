package org.vishia.states;


/**Base class for a composite state which is a parallel state inside another composite state.
 * @author Hartmut Schorrig
 *
 */
public abstract class StateParallel extends StateComposite
{
  /**Version, history and license.
   * <ul>
   * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateAdditionalParallelBase}, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2003 in C-language.
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public static final int version = 20120917;

  
  /**Only for the {@link StateMachine#topState} and GenStm*/
  public StateParallel(StateMachine stateMachine, StateSimple[] aSubstates, StateComposite[] aParallelstates){
    super(stateMachine, aSubstates, aParallelstates);
  }

  
  public StateParallel(){ super(); }
  
  
  /**package private*/ final void XXXentryAdditionalParallelBase(){
    ctEntry +=1;
    dateLastEntry = System.currentTimeMillis();
    durationLast = 0;
  }
  
  
 
  

}
