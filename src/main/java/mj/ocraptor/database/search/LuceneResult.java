package mj.ocraptor.database.search;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.FullText;

public class LuceneResult {
  private SortedSet<Map.Entry<FullText, Double>> elements;
  private SortedSet<Map.Entry<FileEntry, Double>> fileEntries;
  private Throwable throwable;

  /**
   *
   *
   */
  public void setElements(SortedSet<Map.Entry<FullText, Double>> elements) {
    this.elements = elements;
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
   * @return the fileEntries
   */
  public SortedSet<Map.Entry<FileEntry, Double>> getFileEntries() {
    return fileEntries;
  }

  /**
   * @return the throwable
   */
  public Throwable getThrowable() {
    return throwable;
  }

  /**
   * @param throwable the throwable to set
   */
  public void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }

}
