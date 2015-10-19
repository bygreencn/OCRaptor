package mj.ocraptor.extraction.image_processing;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.util.List;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigString;

public abstract class ImageTextExtractor implements Closeable {

  protected final ImageTools preprocessor;

  protected BufferedImage lastGivenBufferedImage;
  protected BufferedImage lastProcessedBufferedImage;

  protected File lastGivenImageFile;
  protected Config cfg;
  private String[] languages;

  /**
   *
   */
  public ImageTextExtractor() {
    this.lastGivenImageFile = null;
    this.lastProcessedBufferedImage = null;

    this.cfg = Config.inst();
    this.preprocessor = new ImageTools();

    String rawLanguageString = this.cfg
                               .getProp(ConfigString.DEFAULT_LANGUAGE_FOR_OCR);

    if (rawLanguageString == null || rawLanguageString.trim().isEmpty()) {
      throw new NullPointerException();
    }

    this.languages = rawLanguageString.split(";");
    List<String> availableLanguageFiles = Config.getLanguageStrings();
    for (String lang : this.languages) {
      if (!availableLanguageFiles.contains(lang.trim())) {
        throw new NullPointerException("LANGUAGE TRAINDATA NOT FOUND: \""
                                       + lang + "\"");
      }
    }
  }

  /**
   *
   *
   * @param image
   * @param languages
   * @return
   *
   */
  public abstract String extractText(BufferedImage image, String originalPath,
                                     String... languages);

  /**
   *
   *
   * @param image
   * @return
   */
  public String extractText(BufferedImage image, String originalPath) {
    if (image != null && !ImageTools.imageSizeValid(image)) {
      return null;
    }
    if (this.checkApplicationState()) {
      return null;
    }
    return extractText(image, originalPath, languages);
  }

  /**
   *
   *
   * @param file
   * @param languages
   * @return
   */
  public abstract String extractText(File file, String originalPath,
                                     String... languages);

  /**
   *
   *
   * @param image
   * @return
   */
  public String extractText(File image, String originalPath) {
    if (image != null && !ImageTools.imageFileSizeValid(image)) {
      return null;
    }
    if (this.checkApplicationState()) {
      return null;
    }
    return extractText(image, originalPath, languages);
  }

  /**
   *
   *
   * @return
   */
  private boolean checkApplicationState() {
    final MainController controller = MainController.inst();
    if (controller != null && (controller.getStatus() == Status.STOPPED || controller.getStatus() == Status.INDEXING_FINISHED)) {
      return true;
    }
    while (!Thread.currentThread().isInterrupted()
           && controller != null && controller.getStatus() == Status.PAUSED) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   * @return the preprocessor
   */
  protected ImageTools getPreprocessor() {
    return preprocessor;
  }

  /**
   *
   *
   */
  public void close() {
    this.preprocessor.cleanCreatedFiles();
  }
}
