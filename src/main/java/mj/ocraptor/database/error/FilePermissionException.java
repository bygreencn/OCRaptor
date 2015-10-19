package mj.ocraptor.database.error;

import java.io.IOException;

/**
 *
 *
 * @author
 */
public class FilePermissionException extends IOException {

  /**
   *
   */
  private static final long serialVersionUID = -7067649072664668813L;
  /**
   *
   */
  private static final String MESSAGE = "File Permission Error";

  /**
   *
   */
  public FilePermissionException() {
    super(MESSAGE);
  }

  /**
   * {@inheritDoc}
   * @see Exception#LuceneIndexNotFoundException(String)
   */
  public FilePermissionException(String s) {
    super(MESSAGE + ", " + s + ".");
  }
}
