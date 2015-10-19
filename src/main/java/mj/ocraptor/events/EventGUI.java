package mj.ocraptor.events;

import static mj.ocraptor.tools.St.removeLastLineBreak;
import static mj.ocraptor.tools.St.shortenHomePathInDirectory;
import static mj.ocraptor.tools.St.trimToLengthIndicatorLeft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import mj.ocraptor.MainController;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.database.DBFileStatus;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.javafx.GUIController;
import mj.ocraptor.javafx.controllers.EditDatabase;
import mj.ocraptor.javafx.controllers.SelectDatabase;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 *
 *
 * @author
 */
public class EventGUI extends EventAbstr {
  private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(EventGUI.class);

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
  protected void startCountingFiles() {
    // TODO Auto-generated method stub

  }

  @Override
  protected void countingFiles(Long currentCount, boolean finalCount) {
    if (finalCount || checkTimeRestrictionCount()) {
      if (!finalCount && currentCount < 10)
        return;

      Queue<ProgressUpdate> messageQueue = GUIController.instance().getMessageQueue();
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
        messageQueue.put(new ProgressUpdate(progressText,
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

  private List<File> lastProgressText = null;

  @Override
  protected void printProcess(File file, Long filesCount, Long processedCount, boolean finalCount,
      DBFileStatus status) {

    boolean initialPost = this.gui.getParentController().showInitialCPUList();

    ConcurrentHashMap<Long, File> threads = MainController.inst().getCurrentFileWorkers();
    List<File> threadIDs = new ArrayList<File>(threads.values());

    if (lastProgressText != null && lastProgressText.equals(threadIDs) && file == null) {
      return;
    }

    this.lastProgressText = threadIDs;

    if (checkTimeRestriction() || initialPost) {
      Queue<ProgressUpdate> messageQueue = GUIController.instance().getMessageQueue();
      List<Node> progressText = new ArrayList<Node>();
      ProgressType progressType = null;

      // ------------------------------------------------ //

      final int maxNumberOfThreads = MainController.inst().getNumThreads();
      if (!threads.isEmpty()) {

        if (progressType == null) {
          progressType = ProgressType.THREAD_LIST_ONLY;
        }

        if (progressType == ProgressType.THREAD_LIST_ONLY || maxNumberOfThreads > 1) {

          int cpuIndex = 0;
          final Text cpuTitle = new Text(this.gui.getText("EVENT_GUI.LOOKING_AT") + " \n");
          progressText.add(cpuTitle);

          // ------------------------------------------------ //
          for (File threadFile : threads.values()) {
            String filePath = threadFile.getPath();
            // String extension = "(" +
            // FilenameUtils.getExtension(filePath).toUpperCase() + ")";

            final Text cpu = new Text();
            final Text cpuDetails = new Text();

            if (maxNumberOfThreads > 1 && file != null
                && threadFile.getAbsoluteFile().equals(file.getAbsoluteFile())) {
              if (threads.values().size() > 1) {
                filePath = this.gui.getText("EVENT_GUI.CPU_THREAD_PICKING");
              } else {
                filePath = this.gui.getText("EVENT_GUI.CPU_THREAD_FINISHING");
              }
              cpuDetails.getStyleClass().add("cpuListChanging");
            }

            String shortFilePath = trimToLengthIndicatorLeft(filePath, 50);
            shortFilePath = shortenHomePathInDirectory(shortFilePath);
            shortFilePath = FileTools.multiplatformPath(shortFilePath);

            cpu.setText(this.gui.getText("EVENT_GUI.CPU_THREAD", String.valueOf(++cpuIndex)));
            cpu.getStyleClass().add("cpuListText");
            cpu.getStyleClass().add("cpuListTitle");

            cpuDetails.setText(" " + shortFilePath + "\n");
            cpuDetails.getStyleClass().add("cpuListText");

            progressText.add(cpu);
            progressText.add(cpuDetails);
          }
          // ------------------------------------------------ //
        }
      }

      // ------------------------------------------------ //

      final boolean showOnlyNewFiles = this.cfg.getProp(ConfigBool.NEW_FILES_NOTIFICATION_ONLY);

      if (file != null && status != null && status != DBFileStatus.NOT_SUPPORTED
          && (showOnlyNewFiles && status == DBFileStatus.NOT_FOUND || !showOnlyNewFiles)) {
        // file name
        Text processedFileLabel = new Text("\n"
            + this.gui.getText("EVENT_GUI.PROCESSED_FILE_LABEL") + "\n");
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
        String filePath = shortenHomePathInDirectory(FileTools.multiplatformPath(file.getParentFile().getPath()));
        Text filePathText = new Text(filePath + "\n\n");
        filePathText.getStyleClass().add("processedFilePathText");
        progressText.add(filePathText);

        // classified as new/modified/not_changed
        Text classifiedLabel = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS") + "\n");
        classifiedLabel.getStyleClass().add("classifiedLabel");
        progressText.add(classifiedLabel);
        Text classifiedText = null;

        if (status == DBFileStatus.NOT_FOUND) {
          classifiedText = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS_NEW") + "\n");
          classifiedText.getStyleClass().add("classifiedAsNew");
          progressType = ProgressType.NEW_FILE_FOUND;
        } else if (status == DBFileStatus.UP_TO_DATE) {
          classifiedText = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS_UP_TO_DATE") + "\n");
          classifiedText.getStyleClass().add("classifiedAsUpToDate");
          progressType = ProgressType.KNOWN_FILE;
        } else {
          classifiedText = new Text(this.gui.getText("EVENT_GUI.CLASSIFIED_AS_MODIFIED") + "\n");
          classifiedText.getStyleClass().add("classifiedAsModified");
          progressType = ProgressType.MODIFIED_FILE;
        }
        classifiedText.getStyleClass().add("classifiedText");
        progressText.add(classifiedText);
      } else if (initialPost) {
        // start message
        Text startMessage = new Text("\n"
            + this.gui.getText("EVENT_GUI.STARTING", String.valueOf(filesCount)) + "\n");
        startMessage.getStyleClass().add("startMessageText");
        progressText.add(startMessage);
        progressType = ProgressType.STARTING;
      } else {
        // not supportet file-type:
      }

      // ------------------------------------------------ //

      try {
        ProgressUpdate indicator = new ProgressUpdate(initialPost ? 0 : processedCount, filesCount,
            progressText, progressType);

        messageQueue.put(indicator);
      } catch (InterruptedException e) {
        // e.printStackTrace();
      }
    }
  }

  @Override
  protected void cancelingIndexing() {
    Queue<ProgressUpdate> messageQueue = GUIController.instance().getMessageQueue();
    List<Node> progressText = new ArrayList<Node>();

    Text canceling = new Text(this.gui.getText("EVENT_GUI.CANCELING") + "\n");
    canceling.getStyleClass().add("cancelingMessage");
    progressText.add(canceling);

    try {
      ProgressUpdate ind = new ProgressUpdate(progressText, ProgressType.CANCELING);
      messageQueue.put(ind);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void printResult(LuceneResult results, String contentSearch, Integer idToShow,
      int maxSnippetLength) {
    this.gui.setSearchResult(results, contentSearch);
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

      String threshold = Config.inst().getProp(ConfigString.LOGFILE_THRESHOLD_OUTPUT);
      Priority thresholdLevel = Level.ERROR;

      if (threshold != null) {
        if (threshold.equals("FATAL")) {
          thresholdLevel = Level.FATAL;
        } else if (threshold.equals("ERROR")) {
          thresholdLevel = Level.ERROR;
        } else if (threshold.equals("INFO")) {
          thresholdLevel = Level.INFO;
        } else if (threshold.equals("DEBUG")) {
          thresholdLevel = Level.DEBUG;
        }
      }

      // ------------------------------------------------ //

      if (GUIController.instance() != null && event != null
          && event.getLevel().isGreaterOrEqual(thresholdLevel) && exceptionScreenEnabled) {

        // ------------------------------------------------ //
        Queue<String> exceptionQueue = null;
        for (int i = 0; i < 50; i++) {
          if (exceptionQueue != null || Thread.currentThread().isInterrupted()) {
            break;
          }
          exceptionQueue = GUIController.instance().getExceptionQueue();
          try {
            Thread.sleep(100);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        }
        // ------------------------------------------------ //

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
              message.append(ExceptionUtils.getStackTrace(e));
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
                Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Pause Indexing on error");
                gui.pauseIndexing();
              }
            });
          }

          // ------------------------------------------------ //

          // *INDENT-OFF*
          String[] acceptedMessages = new String[] {
            "port already in use"
          };
          // *INDENT-ON*

          if (exceptionQueue != null) {
            boolean showMessage = true;
            final String guiMessage = message.toString();
            for (String acceptedMessage : acceptedMessages) {
              if (guiMessage.toLowerCase().contains(acceptedMessage)) {
                showMessage = false;
              }
            }
            if (showMessage) {
              exceptionQueue.put(removeLastLineBreak(message.toString()));
            }
          }

          // ------------------------------------------------ //

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void close() {
      // ---
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
    Queue<ProgressUpdate> messageQueue = GUIController.instance().getMessageQueue();
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
      messageQueue.put(new ProgressUpdate(progressText, progressType));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void databaseConnectError(Exception e, String dataPath) {
    if (e.getMessage() != null && e.getMessage().contains("Database may be already in use")) {
      this.gui.showConfirmationDialog(this.gui.getText("EVENT_GUI.DB_ALREADY_IN_USE"));
    } else {
      LOGGER.error("Problem occurred connecting to database", e);
    }
    this.gui.gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
  }

  @Override
  protected void ocrEngineDeployError(Exception e) {
    //
  }

  @Override
  protected void serverStarted() {
    //
  }

  @Override
  protected void serverProblem(Exception e) {
    try {
      MainController.inst().shutdown(false);
      Platform.runLater(new Runnable() {
        @Override
        public void run() {
          // ------------------------------------------------ //
          EventHandler<ActionEvent> okHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
              try {
                gui.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
              } catch (Exception ex) {
                LOGGER.error("Cant go back to EditDatabase", ex);
              }
            }
          };
          // ------------------------------------------------ //
          gui.showConfirmationDialog(gui.getText("LOADING_SCREEN.PORT_NOT_FOUND", cfg
              .getProp(ConfigInteger.RMI_SERVER_PORT)), okHandler, 350, 200, false);
          Thread.currentThread().setName(Config.APP_NAME + "JavaFX-Event: shutdown indexing");
          // ------------------------------------------------ //
        }
      });
    } catch (Exception e2) {
      // TODO: logging
      e2.printStackTrace();
    }
  }

  @Override
  protected void cantConnectToClients() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        gui.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
      }
    });
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        gui.showConfirmationDialog(gui.getText("EVENT_GUI.CANT_CONNECT_TO_CLIENTS"), 300, 150);
      }
    });
  }

}
