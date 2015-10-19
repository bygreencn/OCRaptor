package mj.ocraptor.tools;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;

/**
 *
 *
 * @author
 */
public class WeakReferenceSer<T> implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = -1821770630532169650L;
  private WeakReference<T> wr;

  public WeakReferenceSer(T referent) {
    wr = new WeakReference<T>(referent);
  }

  public T get() {
    return wr.get();
  }

  /**
   * Write only content of WeakReference. WeakReference itself is not
   * seriazable.
   * 
   * @param out
   * @throws java.io.IOException
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.writeObject(wr.get());
  }

  /**
   * Read saved content of WeakReference and construct new WeakReference.
   * 
   * @param in
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    wr = new WeakReference<T>((T) in.readObject());
  }
}
