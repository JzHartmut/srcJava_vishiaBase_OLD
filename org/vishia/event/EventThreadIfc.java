package org.vishia.event;

import java.util.EventObject;

public interface EventThreadIfc
{
  void storeEvent(EventObject ev);
  
  
  /**Removes this event from its queue if it is in the event queue.
   * If the element of type {@link EventWithDst} is found in the queue, it is designated with stateOfEvent = 'a'
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued. The event queue is a {@link ConcurrentLinkedQueue}.
   * @param ev The event which should be dequeued
   * @return true if found. 
   */
  public boolean removeFromQueue(EventObject ev);
  
  void addTimeOrder(EventTimeout order);
  
  /**Removes a time order, which was activated but it is not in the event execution queue.
   * If the time order is expired and it is in the event execution queue already, it is not removed.
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued.
   * @param order the timeout event or the time order.
   */
  boolean removeTimeOrder(EventTimeout order);
}
