package mj.ocraptor.events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueMonitor<T> implements Queue<T> {
  private BlockingQueue<T> queue;
  private boolean blockOnPut;
  private boolean closed;

  /**
   *
   *
   * @param n
   */
  public QueueMonitor(int n) {
    this(n, false);
  }

  /**
   *
   *
   * @param n
   */
  public QueueMonitor(int n, boolean blockOnPut) {
    this.queue = new LinkedBlockingQueue<T>(n);
    this.blockOnPut = blockOnPut;
  }

  /**
   * {@inheritDoc}
   *
   * @see Queue#put(T)
   */
  public void put(T m) throws InterruptedException {
    put(m, false);
  }


  /**
   *
   *
   * @param m
   *
   * @throws InterruptedException
   */
  public void putAndClose(T m) throws InterruptedException {
    put(m, true);
  }

  /**
   * {@inheritDoc}
   *
   * @see Queue#put(T)
   */
  public void put(T m, boolean close) throws InterruptedException {
    this.closed = close;
    if (blockOnPut) {
      queue.put(m);
    } else {
      queue.offer(m);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see Queue#get()
   */
  public T get() throws InterruptedException {
    if (!closed || !isEmpty()) {
      return queue.take();
    } else {
      return null;
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
  public boolean contains(T obj) {
    return queue.contains(obj);
  }

  /**
   *
   *
   * @param obj
   *
   * @throws InterruptedException
   */
  public void remove(T obj) {
    if (queue.contains(obj)) {
      queue.remove(obj);
    }
  }

  /**
   *
   *
   * @return
   */
  public int size() {
    return queue.size();
  }

  /**
   *
   *
   * @return
   */
  public void reset() {
    while (!queue.isEmpty()) {
      queue.poll();
    }
  }

  /**
   *
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public T[] toArray() {
    return (T[]) queue.toArray();
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
   * @return the closed
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Let's test some shit!
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException  {
    QueueMonitor<String> queue = new QueueMonitor<String>(2);
    queue.put("one");
    queue.putAndClose("two");

    for (int i = 0; i < 10; i++) {
      System.out.println(queue.get());
    }
  }
}
