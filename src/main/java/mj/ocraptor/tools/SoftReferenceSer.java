package mj.ocraptor.tools;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;

/**
 *
 *
 * @author
 */
public class SoftReferenceSer<T> implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = -1821770630532169650L;
  private SoftReference<T> wr;

  public SoftReferenceSer(T referent) {
    wr = new SoftReference<T>(referent);
  }

  public T get() {
    return wr.get();
  }

  /**
   * Write only content of SoftReference. SoftReference itself is not
   * seriazable.
   * 
   * @param out
   * @throws java.io.IOException
   */
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.writeObject(wr.get());
  }

  /**
   * Read saved content of SoftReference and construct new SoftReference.
   * 
   * @param in
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    wr = new SoftReference<T>((T) in.readObject());
  }
}
