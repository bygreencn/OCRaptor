package mj.extraction.result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.MetaData;
import mj.extraction.result.document.FullText;

import org.apache.tika.metadata.Metadata;

public class ExtractionResult {
  private List<FullText> fullTextObjects;
  private List<MetaData> metaData;
  private File parsedFile;

  /**
   * @param fileToExtractTextFrom
   */
  public ExtractionResult(File fileToExtractTextFrom) {
    this.parsedFile = fileToExtractTextFrom;
    this.fullTextObjects = new ArrayList<FullText>();
    this.metaData = new ArrayList<MetaData>();
  }

  /**
   *
   *
   * @param html
   */
  public void addElementsFromXhtml(String xhtml) {
    try {
      // ------------------------------------------------ //
      if (!xhtml.isEmpty()) {
        final FullText currentFullText = new FullText(xhtml);
        if (!this.fullTextObjects.contains(currentFullText)) {
          this.fullTextObjects.add(currentFullText);
        }
      }
      // ------------------------------------------------ //
    } catch (Exception e) {
      // TODO:
      e.printStackTrace();
    } finally {
    }
  }

  /**
   *
   *
   * @param metadata
   */
  public void addMetaData(Metadata metadata) {
    for (String md : metadata.names()) {
      if (md != null && !md.trim().isEmpty()) {
        String value = metadata.get(md);
        if (value != null && !value.trim().isEmpty()) {
          this.metaData.add(new MetaData(md, value));
        }
      }
    }
  }

  /**
   *
   *
   * @param entry
   * @return
   */
  public FileEntry generateEntry() {
    return generateEntry(null);
  }

  /**
   *
   *
   * @return
   */
  public FileEntry generateEntry(FileEntry dbEntry) {
    FileEntry entry = null;
    if (this.fullTextObjects != null && this.metaData != null) {
      // TODO: relative file path
      if (dbEntry == null) {
        entry = new FileEntry(this.parsedFile.getAbsolutePath());
      } else {
        entry = dbEntry;
      }
      if (!this.fullTextObjects.isEmpty())
        entry.setFullTextObjects(this.fullTextObjects);
      if (!this.metaData.isEmpty())
        entry.setMetadata(this.metaData);
    }
    return entry;
  }

  /**
   * @return the fullTextObjects
   */
  public List<FullText> getFullTextObjects() {
    return fullTextObjects;
  }

  /**
   * @return the metaData
   */
  public List<MetaData> getMetaData() {
    return metaData;
  }

  /**
   * @return the fileToExtractTextFrom
   */
  public File getParsedFile() {
    return parsedFile;
  }

}
