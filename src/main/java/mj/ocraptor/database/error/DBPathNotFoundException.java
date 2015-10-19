package mj.ocraptor.database.error;

import java.io.IOException;

/**
 *
 *
 * @author
 */
public class DBPathNotFoundException extends IOException {

  /**
   *
   */
  private static final long serialVersionUID = -5408585483622450885L;

  /**
   *
   */
  private static final String MESSAGE = "Database path not found";

  /**
   *
   */
  public DBPathNotFoundException() {
    super(MESSAGE);
  }

  /**
   * {@inheritDoc}
   * @see Exception#LuceneIndexNotFoundException(String)
   */
  public DBPathNotFoundException(String s) {
    super(MESSAGE + ", " + s + ".");
  }
}
