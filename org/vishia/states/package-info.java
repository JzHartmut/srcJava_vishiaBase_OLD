/**
 * This package contains classes for state machine implementation.
 * <br><br>
 * To build a state machine you should build a maybe inner class derived from {@link org.vishia.states.StateMachine}. This class builds the frame for all states.
 * Any state should be defined as inner class derived from either ...or  
 * <ul>
 * <li>{@link org.vishia.states.StateSimple}: A simple state with transitions, an optional entry- and an exit-action.
 * <li>{@link org.vishia.states.StateComposite}: It contains more as one sub states.
 * <li>{@link org.vishia.states.StateParallel}: It contains more as one {@link org.vishia.states.StateAddParallel} which are {@link org.vishia.states.StateComposite} 
 * for parallel behavior.
 * </ul>
 * See example pattern {@link org.vishia.states.example.StateExampleSimple}.
 * <br><br>
 * Any state can contain an entry- and an exit action which can be build either ... or with:
 * <ul>
 * <li>Overriding the method {@link org.vishia.states.StateSimple#entry(org.vishia.event.Event)} respectively {@link StateSimple#exit()}.
 * <li>Set the association {@link org.vishia.states.StateSimple#entry} respectively {@link org.vishia.states.StateSimple#exit} 
 * with an Implementation of {@link org.vishia.states.StateAction}
 *   respectively {@link java.lang.Runnable} which can be located in any other class, usual as inner anonymous class. 
 *   In that cases the entry and exit can use data from any other class.
 * </ul>  
 * <br><br>
 * Any state can contain some transitions which can be build either ... or with:
 * <ul>
 * <li>An inner class derived from {@link org.vishia.states.StateSimple.StateTrans}.
 * <li>An instance of an anonymous inner derived class of {@link org.vishia.states.StateSimple.StateTrans}.
 * <li>An method with return value {@link org.vishia.states.StateSimple.StateTrans} 
 *   and 2 arguments {@link org.vishia.event.Event} and {@link org.vishia.states.StateSimple.StateTrans}
 *   which contains the creation of a StateTrans object, the condition test, an action and the {@link org.vishia.states.StateSimple.StateTrans#exitState()}
 *   and {@link org.vishia.states.StateSimple.StateTrans#entryState(org.vishia.event.Event)} invocation. This form shows only a simple method
 *   in a browser tree of the source code (outline in Eclipse).  
 * </ul>
 * The three forms of transition phrases gives possibilities for more or less complex transitions:
 * <ul>
 * <li>A simple transition with simple or without action, only with a state switch: Use a simple transition method.
 * <li>A transition which should call an action given with association to another instance: Use a derived transition class.
 * <li>A transition can have choice sub-transitions.
 * <li>A transition can fork to more as one parallel states.
 * <li>A transition can join parallel states.
 * <li>The difference between an anonymous derived instance and a derived class is less.
 * </ul>
 * On construction of the whole {@link StateMachine} all state classes are instantiated and listed in the StateMachine. 
 * All transitions are evaluated and transformed in a unified list of transition objects.
 * For all transitions the necessary exitState()- and entryState() operations are detected and stored, so that complex state switches
 * are executed correctly. The designation of destination states in the written source code uses the class names of the states. 
 * The transition evaluation process on construction searches and lists the instances of the states.
 * <br><br>
 * A state instance can be gotten with {@link org.vishia.states.StateMachine#getState(Class)}
 * <br><br>
 * The {@link org.vishia.states.StateMachine} can be quested whether a state is active yet: {@link org.vishia.states.StateMachine#isInState(Class)} 
 * or {@link org.vishia.states.StateMachine#isInState(StateSimple)}
 * <br><br>
 * The state machine can be animated using {@link org.vishia.states.StateMachine#processEvent(org.vishia.event.Event)} from an event queue or without events
 * by given a null argument, for example cyclically.
 * <br><br>
 * The state machine can be associated with an event queue and a timer using 
 * {@link org.vishia.states.StateMachine#setTimerAndThread(org.vishia.event.EventTimerMng, org.vishia.event.EventThread)}.
 */
package org.vishia.states;
