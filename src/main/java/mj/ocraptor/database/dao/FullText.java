package mj.ocraptor.database.dao;

import mj.ocraptor.tools.SoftReferenceSer;

public class FullText implements java.io.Serializable {
  /**
   *
   */
  private static final long serialVersionUID = -4554099794331281117L;
  private Integer id;
  private Integer fileId;
  private SoftReferenceSer<String> text;

  /**
   *
   */
  public FullText() {
  }

  /**
   *
   *
   * @param position
   * @param type
   * @param text
   */
  public FullText(String text) {
    this.setText(text);
  }

  /**
   * @param fileId
   * @param text
   */
  public FullText(Integer fileId, String text) {
    this.fileId = fileId;
    this.setText(text);
  }

  /**
   * @return the id
   */
  public Integer getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  protected FullText setId(Integer id) {
    this.id = id;
    return this;
  }

  /**
   * @return the fileId
   */
  public Integer getFileId() {
    return fileId;
  }

  /**
   * @param fileId
   *          the fileId to set
   */
  public FullText setFileId(Integer fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * @return the text
   */
  public String getText() {
    if (this.text != null) {
      return text.get();
    } else {
      return null;
    }
  }

  /**
   * @param text
   *          the text to set
   */
  public FullText setText(String text) {
    this.text = new SoftReferenceSer<String>(new String(text));
    text = null;
    return this;
  }

  /**
   *
   *
   * @return
   */
  public boolean isEmpty() {
    if (this.text != null && this.text.get() != null) {
      return this.text.get().isEmpty();
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof FullText))
      return false;
    FullText other = (FullText) obj;
    if (fileId == null) {
      if (other.fileId != null)
        return false;
    } else if (!fileId.equals(other.fileId))
      return false;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.getText();
  }
}
