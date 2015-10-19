package mj.ocraptor.database.error;

/**
 *
 *
 * @author
 */
public class TableEmptyException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = -7726658324345427106L;
  /**
   *
   */
  private static final String MESSAGE = "There are no database entries";

  /**
   *
   */
  public TableEmptyException() {
    super(MESSAGE);
  }

  /**
   * {@inheritDoc}
   * @see Exception#LuceneIndexNotFoundException(String)
   */
  public TableEmptyException(String s) {
    super(MESSAGE + ", " + s);
  }
}
