package mj.ocraptor.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Event<T> implements Queue<T> {
  private BlockingQueue<T> queue;
  private boolean eventFired;

  /**
   *
   *
   * @param n
   */
  public Event() {
    this.queue = new LinkedBlockingQueue<T>(1);
  }

  /**
   *
   *
   * @return
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Object will be lost, if the queue already has one object.
   *
   * @see Queue#put(T)
   */
  public void put(T m) throws InterruptedException {
    queue.offer(m);
  }

  /**
   * {@inheritDoc}
   *
   * @see Queue#get()
   */
  public T get() throws InterruptedException {
    try {
      return queue.take();
    } finally {
      eventFired = true;
    }
  }

  /**
   *
   *
   * @param obj
   * @return
   *
   * @throws InterruptedException
   */
  public boolean contains(T obj) throws InterruptedException {
    return queue.contains(obj);
  }

  /**
   * @return the eventFired
   */
  public boolean fired() {
    return eventFired;
  }
}
