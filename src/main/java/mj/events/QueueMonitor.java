package mj.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueMonitor<T> implements Queue<T> {
  private BlockingQueue<T> queue;

  /**
   *
   *
   * @param n
   */
  public QueueMonitor(int n) {
    queue = new LinkedBlockingQueue<T>(n);
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
   * {@inheritDoc}
   *
   * @see Queue#put(T)
   */
  public void put(T m) throws InterruptedException {
    // queue.put(m);
    queue.offer(m);
  }

  /**
   * {@inheritDoc}
   *
   * @see Queue#get()
   */
  public synchronized T get() throws InterruptedException {
    return queue.take();
  }

}
