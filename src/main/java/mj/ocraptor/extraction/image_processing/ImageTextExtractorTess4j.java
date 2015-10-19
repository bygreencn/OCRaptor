package mj.ocraptor.extraction.image_processing;

import java.awt.image.BufferedImage;
import java.io.File;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.console.Platform;
import mj.ocraptor.console.Platform.Os;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.file_handler.executer.CommandExecutor;
import mj.ocraptor.file_handler.executer.handler_impl.SimpleOutput;
import mj.ocraptor.tools.St;

import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;

import org.apache.commons.io.FileUtils;

public class ImageTextExtractorTess4j extends ImageTextExtractor {

  private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(getClass());

  private static final int WIDTH_THRESHOLD_TO_RESIZE = 1500;
  private static final int HEIGHT_THRESHOLD_TO_RESIZE = 1500;
  private static final int ONE_SECOND_IN_MS = 1000;
  private static final int CHECK_TIMEOUT_INTERVAL = 10;

  private String javaPath = "";

  /**
   *
   */
  public ImageTextExtractorTess4j() {
    super();
    init();
  }

  /**
   *
   *
   */
  private void init() {

    // try {
    //   ImageTextExtractorTess4j.copyNativeFiles();
    // } catch (Exception e) {
    //   String message = "Could not copy tesseracts native files. Shutting down...";
    //   LOG.error(message, e);
    //   JOptionPane.showMessageDialog(null, message + "\n\n"
    //       + StringTools.trimToLengthIndicatorRight(ExceptionUtils.getStackTrace(e), 500));
    //   // TODO: logging
    //   System.exit(1);
    // }

    if (this.cfg.useBuildInJRE()) {
      final String basePath = Config.getBinsFolder() + File.separator + "portable-java"
                              + File.separator;
      Os os = Platform.getSystem();
      if (os == Os.LINUX) {
        javaPath = basePath + "lin-x86-64/bin/";
      } else if (os == Os.OSX) {
        javaPath = basePath + "osx-x86-64/bin/";
      } else if (os == Os.WINDOWS) {
        javaPath = basePath + "win-x86-64\\bin\\";
      }
    }
  }


  private static final int PSM_MODE = 1;
  private static final String TESSERACT_FOLDER = "res/tess";

  /**
   *
   *
   * @param file
   * @param originalPath
   * @param language
   * @return
   */
  private String ocr(File file, String originalPath, String language) {
    // TODO:
    Tesseract1 instance = new Tesseract1();
    instance.setPageSegMode(PSM_MODE);
    instance.setDatapath(TESSERACT_FOLDER);
    instance.setLanguage(language);
    try {
      String result = instance.doOCR(file);
      return result;
    } catch (TesseractException e) {
      e.printStackTrace();
    }

    return null;
  }

  private static final String TESS4J_WRAPPER_MAIN_CLASS = "CommandLineInterpreter";

  /**
   *
   *
   * @param file
   * @param originalPath
   * @param language
   * @return
   *
   */
  private String ocrExternal(File file, String originalPath, String language) {
    String command = javaPath + "java -Dfile.encoding=UTF-8 -Xmx256m -cp \""
                     + Config.getTess4jWrapperBinPath() + (Platform.getSystem() == Os.WINDOWS ? ";" : ":")
                     + Config.getLibraryFolderPath() + "/*\" " + TESS4J_WRAPPER_MAIN_CLASS + " \""
                     + file.getAbsolutePath() + "\" \"" + language + "\"";

    // LOG.info(command);

    String errOutput = null, stdOutput = null;
    SimpleOutput eventHandler = new SimpleOutput();
    CommandExecutor bashExecuter = new CommandExecutor(Platform.getSystem(), eventHandler);
    bashExecuter.setCommand(command);
    Thread executorThread = new Thread(bashExecuter);
    MainController controller = MainController.inst();

    if (command != null && !command.trim().isEmpty()) {
      executorThread.start();
      final long startTime = System.currentTimeMillis();
      boolean pause = false;
      while (!Thread.currentThread().isInterrupted() && executorThread.isAlive()) {
        long duration = System.currentTimeMillis() - startTime;
        if (!pause) {
          pause = (controller.getStatus() == Status.PAUSED);
        }

        // monitoring, if timeout occures --> kill process
        try {
          Thread.sleep(CHECK_TIMEOUT_INTERVAL);
          final int timeout = this.cfg.getProp(ConfigInteger.PROCESSING_TIMEOUT_IN_SECONDS)
                              * ONE_SECOND_IN_MS;
          if (duration > timeout || (controller.getStatus() == Status.STOPPED) || pause) {
            bashExecuter.killProcess();
            if (duration > timeout) {
              EventManager.instance().failedToProcessFile(
                "Timeout processing image with OCR-Engine.", originalPath);
            }
            break;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      // if application is paused, kill all current processes
      // and restart them on resume
      if (pause) {
        while (!Thread.currentThread().isInterrupted()
               && (controller.getStatus() != Status.STOPPED)) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if (controller.getStatus() != Status.PAUSED) {
            return this.ocr(file, originalPath, language);
          }
        }
      }
    }

    errOutput = eventHandler.getErrOut();
    stdOutput = eventHandler.getStdOut();

    if (bashExecuter.isCommandStillRunning()) {
      executorThread.interrupt();
    }

    // @formatter:off
    String[] acceptedMessages = new String[] {
      "using default language params",
      "weak margin",
      "test blob"
    };
    // @formatter:on

    if (errOutput != null && !errOutput.trim().isEmpty()) {
      for (String acceptedMessage : acceptedMessages) {
        if (errOutput.toLowerCase().contains(acceptedMessage)) {
          return stdOutput;
        }
      }
      LOG.error(errOutput);
    }

    // System.out.println(stdOutput);
    // System.out.println(errOutput);
    return stdOutput;
  }

  /**
   *
   *
   * @param image
   * @param originalPath
   * @param language
   * @return
   */
  private String ocr(BufferedImage image, String originalPath, String language) {
    // TODO:
    return null;
  }

  /**
   *
   *
   * @return
   */
  private String extractText(BufferedImage image, File file, String originalPath,
                             String... languages) {
    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    String parsedText = null;
    try {
      String languageStrings = "";
      for (String lang : languages) {
        languageStrings += lang + "+";
      }
      languageStrings = St.removeLastCharacters(languageStrings, 1);

      if (image != null) {
        parsedText = ocr(image, originalPath, languageStrings);
      }
      if (file != null) {
        parsedText = ocr(file, originalPath, languageStrings);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return parsedText;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public String extractText(BufferedImage image, String originalPath, String... languages) {
    if (image != null && cfg.getProp(ConfigBool.ENABLE_IMAGE_OCR)) {
      BufferedImage processedImage = null;
      if (this.lastGivenBufferedImage != null && this.lastProcessedBufferedImage != null
          && this.lastGivenBufferedImage == image) {
        processedImage = this.lastProcessedBufferedImage;
      } else {
        processedImage = preProcessImage(image);
        this.lastGivenBufferedImage = image;
        this.lastProcessedBufferedImage = processedImage;
      }
      File tempImageFile = this.preprocessor.bufferedImageToFile(processedImage);
      if (tempImageFile != null) {
        return extractText(null, tempImageFile, originalPath, languages);
      }
    }
    return null;
  }

  /**
   *
   *
   * @return
   */
  @Override
  public String extractText(File file, String originalPath, String... languages) {
    if (file != null && cfg.getProp(ConfigBool.ENABLE_IMAGE_OCR)) {
      BufferedImage processedImage = null;
      if (this.lastGivenImageFile != null && this.lastProcessedBufferedImage != null
          && this.lastGivenImageFile == file) {
        processedImage = this.lastProcessedBufferedImage;
      } else {
        BufferedImage image = getPreprocessor().fileToBufferedImage(file);
        if (image != null) {
          processedImage = preProcessImage(image);
          this.lastGivenImageFile = file;
          this.lastProcessedBufferedImage = processedImage;
        }
      }
      File tempImageFile = this.preprocessor.bufferedImageToFile(processedImage);
      return extractText(null, tempImageFile, originalPath, languages);
    }
    return null;
  }

  /**
   *
   *
   * @param image
   * @return
   */
  private BufferedImage preProcessImage(BufferedImage image) {
    BufferedImage processedImage = null;
    if (image.getWidth() < WIDTH_THRESHOLD_TO_RESIZE
        || image.getHeight() < HEIGHT_THRESHOLD_TO_RESIZE) {
      processedImage = getPreprocessor().preprocessForOCR(image, true);
    } else {
      processedImage = getPreprocessor().preprocessForOCR(image, false);
    }
    return processedImage;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public static void copyNativeFiles() throws Exception {
    if (!Config.devMode()) {

      // try {
      final String nativeLibPath = Config.getTess4jNativeLibrariesFolderPath();
      if (nativeLibPath != null) {
        final File path = new File(nativeLibPath);
        if (path.exists() && path.isDirectory()) {
          final File[] libs = path.listFiles();
          File basePath = Config.getJarFileFolder();
          basePath = new File("");
          if (basePath != null) {
            for (File lib : libs) {
              final File newLibFile = new File(basePath.getParent(), lib.getName());
              if (!newLibFile.exists()) {
                FileUtils.copyFile(lib, newLibFile);
                newLibFile.deleteOnExit();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                  @Override
                  public void run() {
                    if (newLibFile.exists() && newLibFile.canWrite()) {
                      newLibFile.delete();
                    }
                  }
                });
              }
            }
          }
        }
      }
      // } catch (Exception e) {
      // String message =
      // "Could not copy tesseracts native files. Shutting down...";
      // LOG.error(message, e);
      // JOptionPane.showMessageDialog(null, message + "\n\n"
      // +
      // StringTools.trimToLengthIndicatorRight(ExceptionUtils.getStackTrace(e),
      // 500));
      // // TODO: logging
      // System.exit(1);
      // }
    }
  }

}
