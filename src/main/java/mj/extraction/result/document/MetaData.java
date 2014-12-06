package mj.extraction.result.document;

public class MetaData {
  private Integer id;
  private Integer fileId;
  private String key;
  private String value;

  public static final String FILE_NAME = "MJ-FILENAME";
  public static final String FILE_PATH = "MJ-FILEPATH";

  /**
   *
   *
   * @param key
   * @param value
   */
  public MetaData(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * @param fileId
   * @param key
   * @param value
   */
  public MetaData(Integer fileId, String key, String value) {
    this.fileId = fileId;
    this.key = key;
    this.value = value;
  }

  /**
   *
   */
  public MetaData() {
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
  protected MetaData setId(Integer id) {
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
  public MetaData setFileId(Integer fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @param key
   *          the key to set
   */
  public MetaData setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value
   *          the value to set
   */
  public MetaData setValue(String value) {
    this.value = value;
    return this;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MetaData [fileId=" + fileId + ", key=" + key + ", value=" + value
        + "]";
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
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
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
    if (!(obj instanceof MetaData))
      return false;
    MetaData other = (MetaData) obj;
    if (fileId == null) {
      if (other.fileId != null)
        return false;
    } else if (!fileId.equals(other.fileId))
      return false;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }
}
