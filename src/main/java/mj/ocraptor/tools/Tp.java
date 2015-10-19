package mj.ocraptor.tools;

/**
 * Simple tupel.
 *
 * @author
 */
public class Tp<T1, T2> {

  public T1 getKey() {
    return key;
  }

  public void setKey(T1 key) {
    this.key = key;
  }

  public T2 getValue() {
    return value;
  }

  public void setValue(T2 value) {
    this.value = value;
  }

  private T1 key;
  private T2 value;

  /**
   *
   */
  public Tp(final T1 key, final T2 value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Tp<?, ?> other = (Tp<?, ?>) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    return true;
  }

}
