package org.vishia.stateMachine;

/**Base class for a composite state which is a parallel state inside another composite state.
 * @author Hartmut Schorrig
 *
 * @param <DerivedState>
 * @param <EnclosingState>
 */
public abstract class StateAdditionalParallelBase 
<DerivedState extends StateCompositeBase<DerivedState,?>, EnclosingState extends StateCompositeBase<EnclosingState,?>>
extends StateCompositeBase<DerivedState, EnclosingState> 
{ 
 
 /**Version, history and license.
  * <ul>
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

 
 protected StateAdditionalParallelBase(EnclosingState enclState, String stateId) {
   super(enclState, stateId);
 }

 /**package private*/ final void entryAdditionalParallelBase(){
   ctEntry +=1;
   dateLastEntry = System.currentTimeMillis();
   durationLast = 0;
   setStateParallel(null);
 }
 
 
 /**Sets the state of the composite state.
  * This method should be called
  * @param state Only states of the own composite are advisable. It is checked in compile time
  *   with the strong type check with the generic type of state. 
  * @return true if it is in state.
  */
 @Override
/*package private*/ void setState(StateSimpleBase<DerivedState> stateSimple) { //, EnumState stateNr) {
   super.setStateParallel(stateSimple);
 }


 
}
