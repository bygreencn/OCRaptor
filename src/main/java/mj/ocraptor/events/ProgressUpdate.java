package mj.ocraptor.events;

import java.util.List;

import javafx.scene.Node;

public class ProgressUpdate {
  private Long filesProcessed;
  private Long filesCount;

  private List<Node> progressText;
  private ProgressType progressType;

  // ------------------------------------------------ //

  /**
   *
   *
   * @param progressNodes
   * @param type
   * @param immediate
   */
  public ProgressUpdate(List<Node> progressNodes, ProgressType type) {
    this(null, null, progressNodes, type);
  }

  /**
   *
   *
   * @param filesProcessed
   * @param filesCount
   * @param progressText
   * @param immediate
   */
  public ProgressUpdate(Long filesProcessed, Long filesCount, List<Node> progressText,
      ProgressType type) {
    this.filesCount = filesCount;
    this.filesProcessed = filesProcessed;
    this.progressText = progressText;
    this.progressType = type;
  }

  // ------------------------------------------------ //

  /**
   * @return the progressPercentage
   */
  public Long getFilesProcessed() {
    return filesProcessed;
  }

  /**
   * @param progressPercentage
   *          the progressPercentage to set
   */
  public void setFilesProcessed(Long progressPercentage) {
    this.filesProcessed = progressPercentage;
  }

  /**
   * @return the filesCount
   */
  public Long getFilesCount() {
    return filesCount;
  }

  /**
   * @param filesCount
   *          the filesCount to set
   */
  public void setFilesCount(Long filesCount) {
    this.filesCount = filesCount;
  }

  /**
   * @return the progressText
   */
  public List<Node> getProgressText() {
    return progressText;
  }

  /**
   * @param progressText
   *          the progressText to set
   */
  public void setProgressText(List<Node> progressText) {
    this.progressText = progressText;
  }

  /**
   * @return the progressType
   */
  public ProgressType getProgressType() {
    return progressType;
  }

  /**
   * @param progressType
   *          the progressType to set
   */
  public void setProgressType(ProgressType progressType) {
    this.progressType = progressType;
  }
}
