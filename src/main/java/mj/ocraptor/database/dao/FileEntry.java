package mj.ocraptor.database.dao;

import java.io.File;
import java.io.Serializable;

public class FileEntry implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = 3177346889602137505L;
  private Integer id;
  private String path;
  private String hash;
  private File file;
  private FullText fullText;

  private ResultError error;

  /**
   *
   */
  public FileEntry() {
    this.init();
  }


  /**
   *
   *
   * @param file
   */
  public FileEntry(final File file) {
    this.file = file;
    if (this.file != null) {
      this.path = file.getPath();
    }
    this.init();
  }

  /**
   *
   *
   * @param path
   */
  public FileEntry(final String path) {
    this.path = path;
    this.file = new File(path);
    this.init();
  }

  /**
   *
   */
  public FileEntry(final String path, final String hash) {
    this.path = path;
    this.file = new File(path);
    this.hash = hash;
    this.init();
  }

  /**
   *
   *
   */
  private void init() {
    //
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
   * @return the file
   */
  public File getFile() {
    return file;
  }

  /**
   * @param file the file to set
   */
  public void setFile(File file) {
    this.file = file;
  }

  /**
   * @return the fullText
   */
  public FullText getFullText() {
    return fullText;
  }


  /**
   *
   *
   * @return
   */
  public String getFullTextString() {
    if (fullText != null) {
      return fullText.toString();
    }
    return null;
  }

  /**
   * @param fullText the fullText to set
   */
  public void setFullText(final FullText fullText) {
    this.fullText = fullText;
  }


  /**
   *
   *
   * @param fullText
   */
  public void setFullText(final String fullText) {
    this.fullText = new FullText(fullText);
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

  /**
   * @return the error
   */
  public ResultError getError() {
    return error;
  }

  /**
   * @param error the error to set
   */
  public void setError(final ResultError error) {
    this.fullText = new FullText(error.getErrorCode());
    this.error = error;
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
