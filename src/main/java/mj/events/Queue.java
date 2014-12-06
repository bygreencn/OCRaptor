package mj.events;

public interface Queue<T> {
  public void put(T m) throws InterruptedException;

  public T get() throws InterruptedException;
}
