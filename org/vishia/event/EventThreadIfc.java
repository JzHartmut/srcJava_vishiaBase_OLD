package org.vishia.event;

import java.util.EventObject;

public interface EventThreadIfc
{
  void storeEvent(EventObject ev);
  
  
  /**Removes this event from its queue if it is in the queue.
   * If the element is found in the queue, it is designated with stateOfEvent = 'a'
   * @param ev
   * @return true if found.
   */
  public boolean removeFromQueue(EventObject ev);
  
}
