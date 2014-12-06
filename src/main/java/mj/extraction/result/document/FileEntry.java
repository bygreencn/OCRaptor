package mj.extraction.result.document;

import java.util.ArrayList;
import java.util.List;

public class FileEntry {
  private Integer id;
  private String path;
  private String hash;
  private List<FullText> fullTextObjects;
  private List<MetaData> metadata;

  /**
   *
   */
  public FileEntry() {
    this.init();
  }

  /**
   *
   *
   * @param path
   */
  public FileEntry(String path) {
    this.path = path;
    this.init();
  }

  /**
   *
   */
  public FileEntry(String path, String hash) {
    this.path = path;
    this.hash = hash;
    this.init();
  }

  /**
   *
   *
   */
  private void init() {
    this.fullTextObjects = new ArrayList<FullText>();
    this.metadata = new ArrayList<MetaData>();
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
  protected FileEntry setId(Integer id) {
    this.id = id;
    return this;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @param path
   *          the path to set
   */
  public FileEntry setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * @return the hash
   */
  public String getHash() {
    return hash;
  }

  /**
   * @param hash
   *          the hash to set
   */
  public FileEntry setHash(String hash) {
    this.hash = hash;
    return this;
  }

  /**
   * @return the fullTextObjects
   */
  public List<FullText> getFullTextObjects() {
    return fullTextObjects;
  }

  /**
   * @param fullTextObjects
   *          the fullTextObjects to set
   */
  public void setFullTextObjects(List<FullText> fullTextObjects) {
    this.fullTextObjects = fullTextObjects;
  }

  /**
   * @return the metadata
   */
  public List<MetaData> getMetadata() {
    return metadata;
  }

  /**
   * @param metadata
   *          the metadata to set
   */
  public void setMetadata(List<MetaData> metadata) {
    this.metadata = metadata;
  }

  /**
   *
   *
   * @return
   */
  public FileEntry addFullText(FullText fullText) {
    if (fullText != null) {
      fullTextObjects.add(fullText);
    }
    return this;
  }

  /**
   *
   *
   * @param metadata
   * @return
   */
  public FileEntry addMetaData(MetaData metadata) {
    if (metadata != null) {
      this.metadata.add(metadata);
    }
    return this;
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
    result = prime * result + ((path == null) ? 0 : path.hashCode());
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
    if (!(obj instanceof FileEntry))
      return false;
    FileEntry other = (FileEntry) obj;
    if (path == null) {
      if (other.path != null)
        return false;
    } else if (!path.equals(other.path))
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
    return "FileEntry [id=" + id + ", path=" + path + ", hash=" + hash + "]";
  }

}
