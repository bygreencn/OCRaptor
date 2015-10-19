package mj.ocraptor.database.error;

/**
 *
 *
 * @author
 */
public class LuceneIndexNotFoundException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = 8305798345790321342L;
  /**
   *
   */
  private static final String MESSAGE = "Lucene index not found";

  /**
   *
   */
  public LuceneIndexNotFoundException() {
    super(MESSAGE);
  }

  /**
   * {@inheritDoc}
   * @see Exception#LuceneIndexNotFoundException(String)
   */
  public LuceneIndexNotFoundException(String s) {
    super(MESSAGE + ", " + s);
  }
}
