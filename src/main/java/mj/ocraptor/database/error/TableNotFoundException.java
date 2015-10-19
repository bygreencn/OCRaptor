package mj.ocraptor.database.error;

/**
 *
 *
 * @author
 */
public class TableNotFoundException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = 8305798345790321342L;

  /**
   *
   */
  private static final String MESSAGE = "Database table(s) not found";

  /**
   *
   */
  public TableNotFoundException() {
    super(MESSAGE);
  }

  /**
   * {@inheritDoc}
   * @see Exception#LuceneIndexNotFoundException(String)
   */
  public TableNotFoundException(String s) {
    super(MESSAGE + ", " + s);
  }
}
