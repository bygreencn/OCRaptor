package mj.database;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.FullText;
import mj.extraction.result.document.MetaData;

public class SearchResult {
  private SortedSet<Map.Entry<FullText, Double>> elements;
  private SortedSet<Map.Entry<MetaData, Double>> metadata;
  private SortedSet<Map.Entry<FileEntry, Double>> fileEntries;


  /**
   *
   *
   */
  public void setElements(SortedSet<Map.Entry<FullText, Double>> elements) {
    this.elements = elements;
  }

  /**
   *
   *
   * @param metadata
   */
  public void setMetadata(SortedSet<Map.Entry<MetaData, Double>> metadata) {
    this.metadata = metadata;
  }


  /**
   * @param elements
   *          the elements to set
   */
  public void setElements(HashMap<FullText, Double> elements) {
    this.elements = new TreeSet<Map.Entry<FullText, Double>>(scoreComparator);
    this.elements.addAll(elements.entrySet());
  }

  /**
   * @param metadata
   *          the metadata to set
   */
  public void setMetadata(HashMap<MetaData, Double> metadata) {
    this.metadata = new TreeSet<Map.Entry<MetaData, Double>>(scoreComparator);
    this.metadata.addAll(metadata.entrySet());
  }

  /**
   *
   *
   * @param fileEntries
   */
  public void setFileEntries(HashMap<FileEntry, Double> fileEntries) {
    this.fileEntries = new TreeSet<Map.Entry<FileEntry, Double>>(scoreComparator);
    this.fileEntries.addAll(fileEntries.entrySet());
  }

  /**
   *
   */
  private Comparator<Map.Entry<?, Double>> scoreComparator = new Comparator<Map.Entry<?, Double>>() {
    @Override
    public int compare(Map.Entry<?, Double> p1, Map.Entry<?, Double> p2) {
      if (p1.getValue() < p2.getValue())
        return 1;
      return -1;
    }
  };

  /**
   * @return the elements
   */
  public SortedSet<Map.Entry<FullText, Double>> getElements() {
    return elements;
  }

  /**
   * @return the metadata
   */
  public SortedSet<Map.Entry<MetaData, Double>> getMetadata() {
    return metadata;
  }

  /**
   * @return the fileEntries
   */
  public SortedSet<Map.Entry<FileEntry, Double>> getFileEntries() {
    return fileEntries;
  }

}
