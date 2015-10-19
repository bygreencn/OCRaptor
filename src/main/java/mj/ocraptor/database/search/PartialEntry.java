package mj.ocraptor.database.search;

/**
 *
 *
 * @author
 * @version
 */
public class PartialEntry {

  private boolean pageKnown;
  private Integer page;
  private PartialEntryType type;

  /**
   * @param position
   * @param page
   * @param type
   *
   */
  public PartialEntry(final Integer page, final PartialEntryType type) {
    this.page = page;
    this.type = type;
    if (page != null) {
      pageKnown = true;
    }
  }

  /**
   * @return the pageKnown
   */
  public boolean isPageKnown() {
    return pageKnown;
  }

  /**
   * @return the page
   */
  public Integer getPage() {
    return page;
  }

  /**
   * @param page the page to set
   */
  public void setPage(Integer page) {
    this.page = page;
  }

  /**
   * @return the type
   */
  public PartialEntryType getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(PartialEntryType type) {
    this.type = type;
  }
}
