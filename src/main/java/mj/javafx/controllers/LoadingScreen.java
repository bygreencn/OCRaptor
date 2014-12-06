package mj.javafx.controllers;

import static mj.events.ProgressType.COUNTING_FILES;
import static mj.events.ProgressType.COUNTING_FILES_FINISHED;
import static mj.events.ProgressType.INDEX_FINISHED;
import static mj.events.ProgressType.KNOWN_FILE;
import static mj.events.ProgressType.MODIFIED_FILE;
import static mj.events.ProgressType.NEW_FILE_FOUND;
import static mj.events.ProgressType.PAUSED;
import static mj.events.ProgressType.STARTING;
import static mj.events.ProgressType.THREAD_LIST_ONLY;
import static mj.events.ProgressType.UPDATING_FILE_LIST;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import mj.MainController;
import mj.configuration.Config;
import mj.configuration.properties.ConfigString;
import mj.events.ProgressIndicator;
import mj.events.ProgressType;
import mj.events.QueueMonitor;
import mj.events.RingBuffer;
import mj.javafx.GUIController;
import mj.javafx.GUITemplate;
import mj.javafx.Icon;
import mj.tools.StringTools;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadingScreen extends GUITemplate {

  // @formatter:off
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 400;
  public static final   String FXML = "LoadingScreen.fxml";
  // @formatter:on

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private Config config;

  private static final String STARTING_FOLDER_TEXT_CLASS = "startingFolderText";
  private static final String STARTING_FOLDER_LABEL_CLASS = "startingFolderLabel";

  private static final String FINISHED_TEXT_CLASS = "allJobsFinishedText";
  private static final String FINISHED_FOLDER_TEXT_CLASS = "folderIndexedText";

  private static final String LOOKING_LABEL_CLASS = "lookingAtLabelText";
  private static final String LOOKING_TEXT_CLASS = "lookingAtFolderText";

  private static final String OLD_MESSAGE_CLASS = "oldMessage";

  private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.00");

  private static final int REFRESH_TIME_TEXT_IN_MS = 300;
  private static final int REFRESH_TIME_LABEL_IN_MS = 300;
  private static final int MESSAGE_RINGBUFFER_SIZE = 200;
  private static final int HUNDRED_PERCENT = 100;
  private static final int LOADING = -1;

  private String currentFolder;
  private int currentFolderIndex;
  private int folderCount;
  private boolean finished;

  private long indexingStartTime;

  @FXML
  private Button cancelButton;

  @FXML
  private ToggleButton pauseButton;

  @FXML
  private ProgressBar progressBar;

  @FXML
  private ProgressBar folderProgressBar;

  @FXML
  private Label percentageLabel;

  @FXML
  private Label cpu;

  @FXML
  private Label timeLabel;

  @FXML
  private TextFlow progressText;

  @FXML
  void cancelButtonClicked(ActionEvent event) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        executeWorker(shutdownWorker());
      }
    };

    if (!finished) {
      this.mainController.showConfirmationDialog(this.gui.getText("LOADING_SCREEN.CANCEL_JOBS"),
          handler);
    } else {
      this.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
    }
  }

  @FXML
  void pauseButtonClicked(ActionEvent event) {
    this.pane.requestFocus();

    if (!finished) {
      this.executeWorker(pauseWorker());
    } else {
      this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
    }

  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initVisibility() {
    this.pauseButton.setDisable(false);
    this.folderProgressBar.setVisible(false);
  }

  @Override
  protected void initLabels() {
    this.title.setText(this.gui.getText("LOADING_SCREEN.TITLE"));
  }

  @Override
  public void initCustomComponents() {
    this.mainController.setMessageQueue(new QueueMonitor<ProgressIndicator>(10));

    this.cfg.setProp(ConfigString.LAST_TIME_MODIFIED, String.valueOf(new Date().getTime()));

    Task<Object> indexWorker = indexWorker();
    folderProgressBar.progressProperty().bind(indexWorker.progressProperty());
    executeWorker(indexWorker);

    Task<Object> progressLabelWorker = progressWorker();
    progressBar.progressProperty().bind(progressLabelWorker.progressProperty());
    percentageLabel.textProperty().bind(progressLabelWorker.messageProperty());
    executeWorker(progressLabelWorker);

    Task<Object> cpuLabelWorker = cpuWorker();
    cpu.textProperty().bind(cpuLabelWorker.messageProperty());
    executeWorker(cpuLabelWorker);

    Task<Object> timeLabelWorker = timeWorker();
    timeLabel.textProperty().bind(timeLabelWorker.messageProperty());
    executeWorker(timeLabelWorker);
  }

  @Override
  protected void initListeners() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void asserts() {
    // TODO Auto-generated method stub

  }

  @Override
  protected double getWindowWidth() {
    return INIT_WIDTH;
  }

  @Override
  protected double getWindowHeight() {
    return INIT_HEIGHT;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private GUIController gui;

  /**
   *
   *
   */
  public void pauseProcess() {
    if (!this.cfg.isPaused()) {
      this.pauseButtonClicked(null);
    }
  }

  /**
   *
   */
  public LoadingScreen() {
    this.config = Config.inst();
    this.gui = GUIController.instance();
    config.setShowProgress(true);
    config.setVerbose(true);
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> pauseWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        mainController.getMainController().pauseToggle();
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (Config.inst().isPaused()) {

              List<Node> progressText = new ArrayList<Node>();
              Text pausing = new Text(gui.getText("LOADING_SCREEN.PAUSING_JOBS") + "\n");
              pausing.getStyleClass().add("pausingText");
              progressText.add(pausing);

              try {
                ProgressIndicator ind = new ProgressIndicator(progressText, PAUSED);
                mainController.getMessageQueue().put(ind);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              pauseButton.setText(gui.getText("LOADING_SCREEN.RESUME"));
            } else {
              pauseButton.setText(gui.getText("LOADING_SCREEN.PAUSE"));
            }
          }
        });
        return true;
      }
    };
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> shutdownWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        mainController.getMainController().shutdown(false);
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            config.setShutdown(false);
            gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
          }
        });
        return true;
      }
    };
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> timeWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        while (!config.isShutdown() && !finished) {
          if (config.isPaused()) {
            indexingStartTime += 1000;
          } else {
            final long pastTime = (System.currentTimeMillis() - indexingStartTime) / 1000;
            String display = String.format("%02d:%02d:%02d", pastTime / 3600,
                (pastTime % 3600) / 60, (pastTime % 60));
            while (display.startsWith("00:")) {
              display = display.replaceFirst("00:", "");
            }
            if (display.length() == 2) {
              display += "s";
            }

            String timeLabel = "Time: " + display + "";
            updateMessage(timeLabel);
          }
          if (config.isShutdown()) {
            break;
          }
          Thread.sleep(1000);
        }
        if (config.isShutdown()) {
          updateMessage("...  PLEASE WAIT");
        }
        return true;
      }
    };
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> cpuWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        Sigar sigar = new Sigar();
        while (!config.isShutdown()) {
          DecimalFormat df = new DecimalFormat("#.00");
          CpuPerc perc = sigar.getCpuPerc();
          double percent = perc.getCombined() * 100;

          String cpuLabel = "CPU: " + df.format(percent) + "%";
          updateMessage(cpuLabel);
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              if (percent > 90) {
                cpu.setTextFill(Color.DARKRED);
              } else if (percent > 50) {
                cpu.setTextFill(Color.BLACK);
              } else {
                cpu.setTextFill(Color.DARKGREEN);
              }
            }
          });
          if (config.isShutdown()) {
            break;
          }
          Thread.sleep(1000);
        }
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            cpu.setTextFill(Color.BLACK);
          }
        });
        return true;
      }
    };
  }

  /**
   *
   *
   * @param visible
   */
  private void toggleProgressBarVisibility(boolean visible) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        folderProgressBar.setVisible(visible);
      }
    });
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> indexWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          indexingStartTime = System.currentTimeMillis();
          Config.inst().setShutdown(false);
          Config.inst().setPaused(false);

          // amount of folders to index
          folderCount = config.getExistingFoldersToIndex().size();

          // iterate through given directories
          for (int i = 0; i < folderCount; i++) {

            // if more than one directory, make second progressbar visible {{{
            if (i > 0) {
              toggleProgressBarVisibility(true);
              updateProgress(i, folderCount);
            }
            // }}}

            // get and set path of current folder {{{
            currentFolder = config.getExistingFoldersToIndex().get(i);
            currentFolderIndex = i;
            config.setDirectoryToIndex(currentFolder);
            // }}}

            // 'starting to index'-message
            mainController.getMessageQueue().put(startingToIndexIndicator());

            // 'scan database for missing files on start'
            if (i == 0) {
              mainController.getMainController().removeMissingFiles();
            }

            // start to index current folder:
            MainController.inst().start();

            if (Config.inst().isShutdown()) {
              break;
            }

            // 'folder(s) indexing finished'-message {{{
            updateProgress(folderCount, folderCount);
            boolean allDirectoriesIndexed = (i == folderCount - 1);
            mainController.getMessageQueue().put(finishedToIndexIndicator(allDirectoriesIndexed));
            if (allDirectoriesIndexed) {
              // TODO: ask for restart
              // System.exit(20);
            }
            // }}
          }

          // update buttons {{{
          if (!Config.inst().isShutdown()) {
            finished = true;
            updateProgress(folderCount, folderCount);
            changeButtonsToFinishedState();
          }
          // }}}

        } catch (Exception e) {
          LOG.error("Index worker exception", e);
        } finally {
          finished = true;
        }
        return true;
      }
    };
  }

  /**
   *
   *
   */
  private void changeButtonsToFinishedState() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        cancelButton.setText(gui.getText("BACK_BUTTON"));
        pauseButton.setText(gui.getText("SEARCH_BUTTON"));

        ImageView image = new ImageView(this.getClass().getResource(Icon.SEARCH.toString())
            .toString());
        image.setFitHeight(14);
        image.setTranslateX(1);
        pauseButton.setGraphic(image);
      }
    });
  }

  /**
   *
   *
   * @return
   */
  private ProgressIndicator startingToIndexIndicator() throws Exception {
    final String startText = gui.getText("LOADING_SCREEN.STARTING_TO_INDEX", String
        .valueOf(currentFolderIndex + 1), String.valueOf(folderCount))
        + "\n";
    final String folder = StringTools.trimToLengthIndicatorLeft(StringTools
        .shortenHomePathInDirectory(currentFolder), 100)
        + "\n";
    final List<Node> progressText = new ArrayList<Node>();

    final Text startingFolderLabel = new Text(startText);
    startingFolderLabel.getStyleClass().add(STARTING_FOLDER_LABEL_CLASS);
    final Text startingFolderText = new Text(folder);
    startingFolderText.getStyleClass().add(STARTING_FOLDER_TEXT_CLASS);

    progressText.add(startingFolderLabel);
    progressText.add(startingFolderText);

    final ProgressIndicator ind = new ProgressIndicator(progressText, STARTING);
    return ind;
  }

  /**
   *
   *
   * @param folderOnly
   * @return
   */
  private ProgressIndicator finishedToIndexIndicator(boolean allDirectoriesIndexed)
      throws Exception {
    final List<Node> progressText = new ArrayList<Node>();
    final Text folderDone = new Text(gui.getText("LOADING_SCREEN.FOLDER_INDEXED") + "\n");
    folderDone.getStyleClass().add(FINISHED_FOLDER_TEXT_CLASS);
    progressText.add(folderDone);

    if (allDirectoriesIndexed) {
      final Text success = new Text(gui.getText("LOADING_SCREEN.ALL_JOBS_FINISHED") + "\n");
      success.getStyleClass().add(FINISHED_TEXT_CLASS);
      progressText.add(success);
    }
    final ProgressIndicator ind = new ProgressIndicator(progressText, INDEX_FINISHED);
    return ind;
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> progressWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        RingBuffer<ProgressIndicator> buffer = new RingBuffer<ProgressIndicator>(
            MESSAGE_RINGBUFFER_SIZE);

        long currentTime = System.currentTimeMillis();
        long startTimeText = currentTime + REFRESH_TIME_TEXT_IN_MS;
        long startTimeLabel = currentTime + REFRESH_TIME_LABEL_IN_MS;

        // {{{ updating text in the progressbar
        updateMessage(gui.getText("LOADING_SCREEN.STARTING"));
        updateProgress(-1, -1);
        // }}}

        while (!config.isShutdown()) {
          try {

            boolean showImmediately = false;
            final ProgressIndicator indicator = mainController.getMessageQueue().get();

            // @formatter:off
            final ProgressType type   = indicator.getProgressType();
            final List<Node> texts    = indicator.getProgressText();
            final Long processed      = indicator.getFilesProcessed();
            final Long count          = indicator.getFilesCount();
            // @formatter:off

            // @formatter:off
            if (type == STARTING ||
                type == COUNTING_FILES_FINISHED ||
                type == PAUSED ||
                type == INDEX_FINISHED ||
                type == NEW_FILE_FOUND) {
              showImmediately = true;
            }
            if (type == UPDATING_FILE_LIST ||
                type == COUNTING_FILES) {
                updateProgress(LOADING, LOADING);
            }
            // @formatter:on

            if (config.isShutdown() && !showImmediately) {
              break;
            }

            currentTime = System.currentTimeMillis();
            final boolean refreshTimePassedLabel = (currentTime - startTimeLabel) > REFRESH_TIME_LABEL_IN_MS;
            if (refreshTimePassedLabel || showImmediately) {

              // set label and progress of main progressbar {{{
              if (!cfg.isPaused()) {
                if (type == INDEX_FINISHED) {
                  updateMessage(gui.getText("LOADING_SCREEN.PROGRESS", String
                      .valueOf(HUNDRED_PERCENT)));
                  updateProgress(HUNDRED_PERCENT, HUNDRED_PERCENT);
                } else if (processed != null && count != null) {
                  updateProgress(processed, count);
                  final double percent = (double) processed / (double) count * HUNDRED_PERCENT;
                  if (!Double.isNaN(percent)) {
                    final String label = gui.getText("LOADING_SCREEN.PROGRESS", String
                        .valueOf(PERCENTAGE_FORMAT.format(percent)));
                    updateMessage(label);
                  }
                }
              } else if (type == UPDATING_FILE_LIST) {
                updateMessage(gui.getText("LOADING_SCREEN.MISSING_FILES"));
              } else if (type == COUNTING_FILES) {
                updateMessage(gui.getText("LOADING_SCREEN.COUNTING_FILES"));
              }
              // }}}

              if (texts.isEmpty()) {
                continue;
              }

              // indicate to user, which folder is going to be indexed {{{
              if (folderCount > 1
                  && (type == KNOWN_FILE || type == MODIFIED_FILE || type == NEW_FILE_FOUND || type == THREAD_LIST_ONLY)) {
                final String currentDirPath = StringTools.trimToLengthIndicatorLeft(new File(
                    currentFolder).getName(), 80);
                final Text directory = new Text(currentDirPath + "\n\n");
                directory.getStyleClass().add(LOOKING_TEXT_CLASS);
                final String lookingAtFolderText = gui.getText("LOADING_SCREEN.LOOKING_AT_FOLDER",
                    String.valueOf(currentFolderIndex + 1), String.valueOf(folderCount));
                final Text dirPrefix = new Text(lookingAtFolderText);
                dirPrefix.getStyleClass().add(LOOKING_LABEL_CLASS);
                texts.add(0, directory);
                texts.add(0, dirPrefix);
              }
              // }}}

              // add indicator to history-buffer
              buffer.add(indicator);

              // TODO:
              // @formatter:off
              // if (buffer.size() > 5) {
              //   Platform.runLater(new Runnable() {
              //     @Override
              //     public void run() {
              //       LOG.error("foo");
              //       pauseButtonClicked(null);
              //     }
              //   });
              // }
              // @formatter:on

              startTimeLabel = System.currentTimeMillis();
            }

            // finally, update main text flow {{{
            currentTime = System.currentTimeMillis();
            final boolean refreshTimePassedText = (currentTime - startTimeText) > REFRESH_TIME_TEXT_IN_MS;
            if (refreshTimePassedText || showImmediately) {
              if (!cfg.isPaused() || type == PAUSED) {
                updateTextFlow(buffer);
                startTimeText = System.currentTimeMillis();
              }
            }
            // }}}
          } catch (Exception e) {
            LOG.error("ProgressWorker failed", e);
          }
        }
        updateMessage("CANCELING");
        return true;
      }
    };
  }

  /**
   *
   *
   * @param buffer
   */
  private void updateTextFlow(RingBuffer<ProgressIndicator> buffer) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        progressText.getChildren().clear();
        final Iterator<ProgressIndicator> iterator = buffer.iterator();
        int iteratorCount = 1;

        while (iterator.hasNext()) {
          final ProgressIndicator indicator = iterator.next();
          if (iteratorCount > 1) {
            progressText.getChildren().add(new Text(" \n "));
          }
          progressText.getChildren().add(new Text(" \n"));
          for (final Node text : indicator.getProgressText()) {
            final Text txt = ((Text) text);
            if (!txt.getText().trim().isEmpty()) {
              if (iteratorCount > 1 && (iteratorCount != 2 || !cfg.isPaused())) {
                final ObservableList<String> classes = txt.getStyleClass();
                if (!classes.contains(OLD_MESSAGE_CLASS)) {
                  classes.add(OLD_MESSAGE_CLASS);
                }
              }
              progressText.getChildren().add((Text) text);
            }
          }
          iteratorCount++;
          progressText.getChildren().add(new Text(" \n "));
          progressText.getChildren().add(getCustomDivider());
        }
      }
    });
  }

  /**
   *
   *
   * @return
   */
  private Line getCustomDivider() {
    return this.getDivider("loadingScreenDivider", 1.0f, 50, 2d, 5d);
  }

  /**
   * @return the progressBar
   */
  public ProgressBar getProgressBar() {
    return progressBar;
  }

  /**
   * @return the progressText
   */
  public TextFlow getProgressText() {
    return progressText;
  }

  @Override
  protected void initEventHandlers() {
    // TODO Auto-generated method stub

  }
}
