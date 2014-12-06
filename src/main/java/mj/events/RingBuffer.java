package mj.events;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RingBuffer<E> implements Collection<E> {

  private static final int DEFAULT_CAPACITY = 10;

  private E[] elementData;

  private int capacity;

  private int size;

  private int index;

  public RingBuffer(int capacity) throws IllegalArgumentException {
    if (capacity <= 0)
      throw new IllegalArgumentException("Illegal Capacity : " + capacity);

    this.capacity = capacity;
    clear();
  }

  /**
   * {@inheritDoc}
   * @see Object#RingBuffer()
   */
  public RingBuffer() {
    this(DEFAULT_CAPACITY);
  }

  /**
   *
   *
   * @param c
   *
   * @throws NullPointerException
   */
  @SuppressWarnings("unchecked")
  public RingBuffer(Collection<? extends E> c) throws NullPointerException {
    capacity = c.size();
    size = capacity;
    index = 0;
    elementData = c.toArray((E[]) new Object[capacity]);
  }

  /**
   * {@inheritDoc}
   * @see Collection#clear()
   */
  @SuppressWarnings("unchecked")
  public void clear() {
    elementData = (E[]) new Object[capacity];
    size = 0;
    index = 0;
  }

  /**
   * {@inheritDoc}
   * @see Collection#add(E)
   */
  public boolean add(E o) {
    index = (capacity + --index) % capacity;

    elementData[index] = o;

    if (size < capacity)
      size++;

    return true;
  }

  /**
   * {@inheritDoc}
   * @see Collection#contains(Object)
   */
  public boolean contains(Object o) {
    for (Object e : this)
      if (o == null ? e == null : o.equals(e))
        return true;

    return false;
  }

  /**
   * {@inheritDoc}
   * @see Collection#containsAll(Collection<?>)
   */
  public boolean containsAll(Collection<?> c) {
    if (c.size() > size)
      return false;

    for (Object o : c)
      if (!contains(o))
        return false;

    return true;
  }

  /**
   * {@inheritDoc}
   * @see Collection#isEmpty()
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * {@inheritDoc}
   * @see Collection#size()
   */
  public int size() {
    return size;
  }

  /**
   *
   *
   * @return
   */
  public int capacity() {
    return capacity;
  }

  /**
   * {@inheritDoc}
   * @see Collection#iterator()
   */
  public Iterator<E> iterator() {
    return new RingBufferIterator<E>(this, index, size);
  }

  /**
   * {@inheritDoc}
   * @see Collection#toArray()
   */
  public Object[] toArray() {
    Object[] a = new Object[size];

    int i = 0;
    for (E element : this)
      a[i++] = element;

    return a;
  }

  /**
   * {@inheritDoc}
   * @see Collection#toArray(T[])
   */
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) throws NullPointerException, ArrayStoreException {
    if (a.length < size)
      return (T[]) Arrays.copyOf(toArray(), size, a.getClass());

    System.arraycopy(toArray(), 0, a, 0, size);
    if (a.length > size)
      a[size] = null;

    return a;
  }

  /**
   * {@inheritDoc}
   * @see Collection#addAll(Collection<E>)
   */
  public boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @see Collection#remove(Object)
   */
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @see Collection#removeAll(Collection<?>)
   */
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @see Collection#retainAll(Collection<?>)
   */
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @see Object#toString()
   */
  public String toString() {
    return Arrays.toString(toArray());
  }

  /**
   *
   */
  private class RingBufferIterator<T> implements Iterator<T> {
    private int index;
    private int size;
    private RingBuffer<T> rb;

    /**
     *
     *
     * @param rb
     * @param index
     * @param size
     */
    private RingBufferIterator(RingBuffer<T> rb, int index, int size) {
      this.index = index;
      this.size = size;
      this.rb = rb;
    }

    /**
     * {@inheritDoc}
     * @see Iterator#hasNext()
     */
    public boolean hasNext() {
      return size > 0;
    }

    /**
     * {@inheritDoc}
     * @see Iterator#next()
     */
    public T next() throws NoSuchElementException {
      if (!hasNext())
        throw new NoSuchElementException();

      T element = rb.elementData[index];

      size--;
      index = (capacity + ++index) % rb.capacity;

      return element;
    }

    /**
     * {@inheritDoc}
     * @see Iterator#remove()
     */
    public void remove() throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }

  }

  /**
   *
   *
   * @return
   */
  public int length() {
    int count = 0;
    for (E e : elementData) {
      if (e != null) {
        count++;
      }
    }
    return count;
  }
}
