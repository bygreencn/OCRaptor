package mj.events;

import static mj.tools.StringTools.removeLastLineBreak;
import static mj.tools.StringTools.shortenHomePathInDirectory;
import static mj.tools.StringTools.trimToLengthIndicatorLeft;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.text.Text;

import mj.MainController;
import mj.configuration.Config;
import mj.configuration.properties.ConfigBool;
import mj.database.DBFileStatus;
import mj.database.SearchResult;
import mj.javafx.GUIController;
import mj.javafx.controllers.SelectDatabase;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 *
 *
 * @author
 */
public class EventGUI extends EventAbstr {
  private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(getClass());

  private static Long startTime;
  private static final int timeDifference = 10;

  private static Long startTimeCount;
  private static final int timeDifferenceCount = 100;

  private GUIController gui;
  private Config cfg;

  /**
   *
   */
  public EventGUI() {
    this.gui = GUIController.instance();
    this.cfg = Config.inst();
  }

  @Override
  protected void configFileNotFound(File invalidFile) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void countingFiles(Long currentCount, boolean finalCount) {
    if (finalCount || checkTimeRestrictionCount()) {
      if (!finalCount && currentCount < 10)
        return;

      Queue<ProgressIndicator> messageQueue = GUIController.instance().getMessageQueue();
      List<Node> progressText = new ArrayList<Node>();

      Text fileCount = new Text(this.gui.getText("EVENT_GUI.COUNTED_FILES", String
          .valueOf(currentCount)));
      fileCount.getStyleClass().add("countedFilesText");
      progressText.add(fileCount);

      Text finished = new Text((finalCount ? "\n"
          + this.gui.getText("EVENT_GUI.FILTERING_BY_FILETYPE") : ".")
          + "\n");
      finished.getStyleClass().add("filteringByFiletype");
      progressText.add(finished);

      try {
        messageQueue.put(new ProgressIndicator(progressText,
            finalCount ? ProgressType.COUNTING_FILES_FINISHED : ProgressType.COUNTING_FILES));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   *
   *
   * @return
   */
  private static boolean checkTimeRestriction() {
    if (startTime == null || System.currentTimeMillis() - startTime > timeDifference) {
      startTime = System.currentTimeMillis();
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @return
   */
  private static boolean checkTimeRestrictionCount() {
    if (startTimeCount == null || System.currentTimeMillis() - startTimeCount > timeDifferenceCount) {
      startTimeCount = System.currentTimeMillis();
      return true;
    }
    return false;
  }

  @Override
  protected void configFileNameInvalid() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void propertiesFileAlreadyExists() {
    // TODO Auto-generated method stub
  }

  private static final String PROCESSED_FILE_LABEL_CLASS = "processedFileLabel";
  private static final String PROCESSED_FILE_TEXT_CLASS = "processedFileText";

  @Override
  protected void printProcess(File file, Long filesCount, Long processedCount, boolean finalCount,
      DBFileStatus status) {

    boolean initialPost = this.gui.getMainController().showInitialCPUList();

    if (checkTimeRestriction() || initialPost) {
      Queue<ProgressIndicator> messageQueue = GUIController.instance().getMessageQueue();
      List<Node> progressText = new ArrayList<Node>();
      ProgressType progressType = null;

      // ------------------------------------------------ //
      final boolean showOnlyNewFiles = this.cfg.getProp(ConfigBool.NEW_FILES_NOTIFICATION_ONLY);

      if (file != null && status != null && status != DBFileStatus.NOT_SUPPORTED
          && (showOnlyNewFiles && status == DBFileStatus.NOT_FOUND || !showOnlyNewFiles)) {
        // file name
        Text processedFileLabel = new Text(this.gui.getText("EVENT_GUI.PROCESSED_FILE_LABEL")
            + "\n");
        processedFileLabel.getStyleClass().add(PROCESSED_FILE_LABEL_CLASS);
        progressText.add(processedFileLabel);
        Text processedFileText = new Text(file.getName() + "\n\n");
        processedFileText.getStyleClass().add(PROCESSED_FILE_TEXT_CLASS);
        progressText.add(processedFileText);

        // file directory
        Text filePathLabel = new Text(this.gui.getText("EVENT_GUI.PROCESSED_FILE_PATH_LABEL")
            + "\n");
        filePathLabel.getStyleClass().add("processedFilePathLabel");
        progressText.add(filePathLabel);
        Text filePathText = new Text(shortenHomePathInDirectory(file.getParent()) + "\n\n");
        filePathText.getStyleClass().add("processedFilePathText");
        progressText.add(filePathText);

        // classified as new/modified/not_changed
        Text classifiedLabel = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS") + "\n");
        classifiedLabel.getStyleClass().add("classifiedLabel");
        progressText.add(classifiedLabel);
        Text classifiedText = null;

        if (status == DBFileStatus.NOT_FOUND) {
          classifiedText = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS_NEW") + "\n\n");
          classifiedText.getStyleClass().add("classifiedAsNew");
          progressType = ProgressType.NEW_FILE_FOUND;
        } else if (status == DBFileStatus.UP_TO_DATE) {
          classifiedText = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS_UP_TO_DATE") + "\n\n");
          classifiedText.getStyleClass().add("classifiedAsUpToDate");
          progressType = ProgressType.KNOWN_FILE;
        } else {
          classifiedText = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS_MODIFIED") + "\n\n");
          classifiedText.getStyleClass().add("classifiedAsModified");
          progressType = ProgressType.MODIFIED_FILE;
        }
        classifiedText.getStyleClass().add("classifiedText");
        progressText.add(classifiedText);
      } else if (initialPost) {
        // start message
        Text startMessage = new Text(this.gui.getText("EVENT_GUI.STARTING", String
            .valueOf(filesCount)
            + "\n"));
        startMessage.getStyleClass().add("startMessageText");
        progressText.add(startMessage);
        progressType = ProgressType.STARTING;
      } else {
        // not supportet file-type:
      }

      // ------------------------------------------------ //

      ConcurrentHashMap<Long, File> threads = MainController.inst().getCurrentFileWorkers();

      final int maxNumberOfThreads = MainController.inst().getNumThreads();
      if (!threads.isEmpty()) {

        if (progressType == null) {
          progressType = ProgressType.THREAD_LIST_ONLY;
        }

        if (progressType == ProgressType.THREAD_LIST_ONLY || maxNumberOfThreads > 1) {

          int cpuIndex = 0;
          final Text cpuTitle = new Text(this.gui.getText("EVENT_GUI.LOOKING_AT") + " \n");
          progressText.add(cpuTitle);

          for (File threadFile : threads.values()) {
            String filePath = threadFile.getAbsolutePath();
            String extension = "(" + FilenameUtils.getExtension(filePath).toUpperCase() + ")";

            final Text cpu = new Text();
            if (maxNumberOfThreads > 1 && file != null
                && threadFile.getAbsoluteFile().equals(file.getAbsoluteFile())) {
              if (threads.values().size() > 1) {
                filePath = this.gui.getText("EVENT_GUI.CPU_THREAD_PICKING");
              } else {
                filePath = this.gui.getText("EVENT_GUI.CPU_THREAD_FINISHING");
              }
              extension = "";
              cpu.getStyleClass().add("cpuListChanging");
            }

            String shortFilePath = trimToLengthIndicatorLeft(filePath + " " + extension, 50);
            shortFilePath = shortenHomePathInDirectory(shortFilePath);

            cpu.setText(this.gui.getText("EVENT_GUI.CPU_THREAD", String.valueOf(++cpuIndex),
                shortFilePath)
                + "\n");

            cpu.getStyleClass().add("cpuListText");
            progressText.add(cpu);
          }
        }
      }

      // ------------------------------------------------ //

      try {
        ProgressIndicator indicator = new ProgressIndicator(initialPost ? 0 : processedCount,
            filesCount, progressText, progressType);

        messageQueue.put(indicator);
      } catch (InterruptedException e) {
        // e.printStackTrace();
      }
    }
  }

  @Override
  protected void cancelingIndexing() {
    Queue<ProgressIndicator> messageQueue = GUIController.instance().getMessageQueue();
    List<Node> progressText = new ArrayList<Node>();

    Text canceling = new Text(this.gui.getText("EVENT_GUI.CANCELING") + "\n");
    canceling.getStyleClass().add("cancelingMessage");
    progressText.add(canceling);

    try {
      ProgressIndicator ind = new ProgressIndicator(progressText, ProgressType.CANCELING);
      messageQueue.put(ind);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void printResult(SearchResult results, String metaDataSearch, String contentSearch,
      Integer idToShow, int maxMetaDataLength, int maxSnippetLength) {
    this.gui.setSearchResult(results, metaDataSearch, contentSearch);
  }

  @Override
  protected void searchProgressIndicator(double value, String text) {
    if (value > 0) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    GUIController.instance().getTemplate().setProgress(value, text);
  }

  @Override
  protected void initLoggerAppender(Logger logger) {
    String appenderName = "gui appender";
    if (logger.getAppender(appenderName) == null) {
      logger.addAppender(new GuiLogAppender());
    }
  }

  // TODO: jbig2-processing problems
  public static final String[] KNOWN_ISSUES = { "jbig2" };

  /**
   *
   */
  private class GuiLogAppender extends AppenderSkeleton {
    @Override
    protected void append(LoggingEvent event) {
      boolean exceptionScreenEnabled = cfg.getProp(ConfigBool.ENABLE_BUG_REPORT_SCREENS);

      if (GUIController.instance() != null && event != null
          && event.getLevel().isGreaterOrEqual(Level.ERROR) && exceptionScreenEnabled) {
        try {
          StringBuffer message = new StringBuffer();
          String generalMessage = event.getRenderedMessage();

          message.append("[" + event.getLevel() + "] " + new Date() + "\n");
          if (generalMessage != null && !generalMessage.trim().isEmpty()) {
            message.append("[MESSAGE] ");
            message.append(generalMessage + "\n");
          }
          ThrowableInformation info = event.getThrowableInformation();
          if (info != null) {
            Throwable e = info.getThrowable();
            if (e != null) {
              message.append("[STACKTRACE] ");
              message.append(ExceptionUtils.getFullStackTrace(e));
            }
          }
          for (String knownIssue : KNOWN_ISSUES) {
            if (message.toString().contains(knownIssue)) {
              return;
            }
          }

          if (cfg.getProp(ConfigBool.PAUSE_ON_ERROR)) {
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                gui.pauseIndexing();
              }
            });
          }

          GUIController.instance().getExceptionQueue().put(removeLastLineBreak(message.toString()));

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void close() {

    }

    public boolean requiresLayout() {
      return false;
    }

  }

  @Override
  protected void failedToProcessFile(String error, String filePath) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void removingMissingFiles(Integer filesDeleted, Integer filesToLookAt) {
    Queue<ProgressIndicator> messageQueue = GUIController.instance().getMessageQueue();
    List<Node> progressText = new ArrayList<Node>();
    ProgressType progressType = null;

    if (filesDeleted == null && filesToLookAt != null) {
      Text looking = new Text(this.gui.getText("EVENT_GUI.MISSING_FILES", String
          .valueOf(filesToLookAt)));
      looking.getStyleClass().add("missingFiles");
      progressText.add(looking);
      progressType = ProgressType.LOOKING_FOR_MISSING_FILES;
    }

    if (filesDeleted != null) {
      Text deleting = new Text(this.gui.getText("EVENT_GUI.REMOVED_FILES", String
          .valueOf(filesDeleted)));
      deleting.getStyleClass().add("removedFilesText");
      progressText.add(deleting);
      progressType = ProgressType.MISSING_FILES_REMOVED;
    }

    try {
      messageQueue.put(new ProgressIndicator(progressText, progressType));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void databaseConnectError(Exception e, String dataPath) {
    if (e.getMessage().contains("Database may be already in use")) {
      this.gui.showConfirmationDialog(this.gui.getText("EVENT_GUI.DB_ALREADY_IN_USE"));
    } else {
      LOG.error("Problem occurred connecting to database", e);
    }
    this.gui.gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
  }

  @Override
  protected void ocrEngineDeployError(Exception e) {

  }
}
