package mj.ocraptor.javafx.controllers;

import static mj.ocraptor.events.ProgressType.COUNTING_FILES;
import static mj.ocraptor.events.ProgressType.COUNTING_FILES_FINISHED;
import static mj.ocraptor.events.ProgressType.INDEX_FINISHED;
import static mj.ocraptor.events.ProgressType.KNOWN_FILE;
import static mj.ocraptor.events.ProgressType.MODIFIED_FILE;
import static mj.ocraptor.events.ProgressType.NEW_FILE_FOUND;
import static mj.ocraptor.events.ProgressType.PAUSED;
import static mj.ocraptor.events.ProgressType.STARTING;
import static mj.ocraptor.events.ProgressType.THREAD_LIST_ONLY;
import static mj.ocraptor.events.ProgressType.UPDATING_FILE_LIST;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.events.ProgressType;
import mj.ocraptor.events.ProgressUpdate;
import mj.ocraptor.events.QueueMonitor;
import mj.ocraptor.events.RingBuffer;
import mj.ocraptor.javafx.GUIController;
import mj.ocraptor.javafx.GUITemplate;
import mj.ocraptor.javafx.Icon;
import mj.ocraptor.rmi_server.RMIServerImpl;
import mj.ocraptor.tools.SystemTools;
import mj.ocraptor.tools.St;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadingScreen extends GUITemplate {

  // *INDENT-OFF*
  public static double  INIT_WIDTH  = 550,
                        INIT_HEIGHT = 400;

  private static final String
    STARTING_FOLDER_TEXT_CLASS  = "startingFolderText",
    STARTING_FOLDER_LABEL_CLASS = "startingFolderLabel",
    FINISHED_TEXT_CLASS         = "allJobsFinishedText",
    FINISHED_FOLDER_TEXT_CLASS  = "folderIndexedText",
    LOOKING_LABEL_CLASS         = "lookingAtLabelText",
    LOOKING_TEXT_CLASS          = "lookingAtFolderText",
    OLD_MESSAGE_CLASS           = "oldMessage";

  private static final int
    REFRESH_TIME_TEXT_IN_MS     = 300,
    REFRESH_TIME_LABEL_IN_MS    = 300,
    MESSAGE_RINGBUFFER_SIZE     = 3,
    HUNDRED_PERCENT             = 100,
    LOADING                     = -1;
  // *INDENT-ON*

  public static final String FXML = "LoadingScreen.fxml";
  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private Config config;
  private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.00");
  private String currentFolder;
  private int currentFolderIndex, folderCount;
  private boolean finished;
  private MainController controller;
  private long indexingStartTime;

  // ------------------------------------------------ //

  @FXML
  private Button cancelButton;

  @FXML
  private ToggleButton pauseButton;

  @FXML
  private ProgressBar progressBar;

  @FXML
  private ProgressBar folderProgressBar;

  @FXML
  private ProgressIndicator rightIndicator;

  @FXML
  private Label percentageLabel;

  @FXML
  private Label loadingScreenCounter;

  @FXML
  private Label cpu;

  @FXML
  private Label ram;

  @FXML
  private Label timeLabel;

  @FXML
  private TextFlow progressText;

  @FXML
  private VBox overlayLabelVbox;

  @FXML
  private HBox overlayLabelHbox;

  @FXML
  private Label overlayLabel;

  @FXML
  private Label overlayDetailsLabel;

  // ------------------------------------------------ //

  @FXML
  void cancelButtonClicked(ActionEvent event) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        executeWorker(shutdownWorker());
      }
    };

    if (!finished) {
      this.g.showConfirmationDialog(g.getText("LOADING_SCREEN.CANCEL_JOBS"), handler);
    } else {
      this.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
      g.getParentController().shutdown(false);
    }
  }

  @FXML
  void pauseButtonClicked(ActionEvent event) {
    this.pane.requestFocus();

    if (!finished) {
      this.executeWorker(pauseWorker());
      this.changeButtonImageColor();
      this.executeWorker(disableButtonWorker(pauseButton));
    } else {
      this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
      g.getParentController().shutdown(false);
    }
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initEventHandlers() {
    this.overlayLabelVbox.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent t) {
        if (!controller.paused()) {
          overlayLabelVbox.setVisible(false);
        }
      }
    });
  }

  @Override
  protected void initVisibility() {
    this.pauseButton.setDisable(false);
    this.folderProgressBar.setVisible(false);
  }

  @Override
  protected void initLabels() {
    this.title.setText(g.getText("LOADING_SCREEN.TITLE"));
    this.cancelButton.setText(g.getText("CANCEL"));
    this.pauseButton.setText(g.getText("LOADING_SCREEN.PAUSE"));
    this.addTooltip(this.rightIndicator, g.getText("LOADING_SCREEN.INDICATOR_INFO"), -205, 0);
    this.addTooltip(this.loadingScreenCounter, "test", -205, 0);
  }

  @Override
  public void initCustomComponents() {
    this.controller.setStatus(Status.INDEXING);
    this.g.setMessageQueue(new QueueMonitor<ProgressUpdate>(10));

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

    Task<Object> ramLabelWorker = ramWorker();
    ram.textProperty().bind(ramLabelWorker.messageProperty());
    executeWorker(ramLabelWorker);

    Task<Object> indicatorWorker = indicatorWorker();
    rightIndicator.progressProperty().bind(indicatorWorker.progressProperty());
    loadingScreenCounter.textProperty().bind(indicatorWorker.messageProperty());
    executeWorker(indicatorWorker);

    Task<Object> visibilityWorker = visibilityWorker();
    executeWorker(visibilityWorker);

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
    if (this.controller.getStatus() != Status.PAUSED) {
      this.pauseButtonClicked(null);
    }
  }

  /**
   *
   */
  public LoadingScreen() {
    this.config = Config.inst();
    this.gui = GUIController.instance();
    this.controller = MainController.inst();
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
        try {
          g.getParentController().pauseToggle();

          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Thread.currentThread().setName(Config.APP_NAME + "JavaFX: pausing indexing");
              if (controller.getStatus() == Status.PAUSED) {
                overlayLabelVbox.setVisible(true);
                overlayLabel.setText(gui.getText("LOADING_SCREEN.PAUSED"));
                overlayDetailsLabel.setText(gui.getText("LOADING_SCREEN.CLICK_RESUME"));
                pauseButton.setText(gui.getText("LOADING_SCREEN.RESUME"));
              } else {
                overlayLabelVbox.setVisible(false);
                pauseButton.setText(gui.getText("LOADING_SCREEN.PAUSE"));
              }
            }
          });
        } catch (Exception e) {
          // TODO: logging
          e.printStackTrace();
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
  private Task<Object> shutdownWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          g.getParentController().shutdown(false);
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Thread.currentThread().setName(Config.APP_NAME + "JavaFX: shutdown indexing");
              gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
            }
          });
        } catch (Exception e) {
          // TODO: logging
          e.printStackTrace();
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
  private Task<Object> timeWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          while (!controller.stopped() && !finished && !Thread.currentThread().isInterrupted()) {
            if (controller.getStatus() == Status.PAUSED) {
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

              String timeLabel = gui.getText("LOADING_SCREEN.TIME", display);
              updateMessage(timeLabel);
            }
            if (controller.getStatus() == Status.STOPPED) {
              break;
            }
            Thread.sleep(1000);
          }
          if (controller.getStatus() == Status.STOPPED) {
            updateMessage("...  " + gui.getText("LOADING_SCREEN.PLEASE_WAIT"));
          }
        } catch (Exception e) {
          // TODO: logging
          e.printStackTrace();
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
  private Task<Object> disableButtonWorker(final ToggleButton button) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          // ------------------------------------------------ //
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Thread.currentThread().setName(Config.APP_NAME + "JavaFX: temporary disable pause button");
              button.setDisable(true);
            }
          });
          Thread.sleep(3000);
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Thread.currentThread().setName(Config.APP_NAME + "JavaFX: enable pause button");
              button.setDisable(false);
            }
          });
          // ------------------------------------------------ //
        } catch (Exception e) {
          // TODO: logging
          e.printStackTrace();
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
  private Task<Object> indicatorWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Enable upper right indexing indicator");
        try {
          int i = 0, waitTime = 100;
          final Random random = new Random();

          updateProgress(0, 100);

          if (!rightIndicator.isVisible()) {
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                Thread.currentThread().setName(
                    "JavaFX: Enable upper right indexing indicator - sub");
                rightIndicator.setVisible(true);
              }
            });
          }

          while (!controller.stopped() && !Thread.currentThread().isInterrupted()) {
            // ------------------------------------------------ //
            final RMIServerImpl server = g.getParentController().getServer();

            if (server != null && loadingScreenCounter.isVisible()) {
              updateMessage(server.getImageOCRCount() + " - "
                  + St.format(server.getAllFulltextLength()));
            }

            if (rightIndicator.isVisible()) {
              if (i++ > 98) {
                i = 0;
                waitTime = random.nextInt(100);
              }
              if (random.nextInt(10) > 8) {
                if (waitTime > 5) {
                  waitTime = (int) ((double) waitTime / (double) 2);
                }
              }
              if (random.nextInt(10) > 7) {
                Thread.sleep(random.nextInt(500));
              }
              updateProgress(i, 100);
            }
            // ------------------------------------------------ //
            Thread.sleep(waitTime);
          }
        } catch (Exception e) {
          e.printStackTrace();
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
  private Task<Object> visibilityWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Upper right indicator - visibility worker");
        try {
          // ------------------------------------------------ //

          int waitTime = 100;

          final int maxClients = g.getParentController().getNumThreads();

          String progressIndicatorStyle = "";
          boolean showIndicator = false;

          boolean firstStart = true;

          while (!controller.stopped() && !Thread.currentThread().isInterrupted()) {
            final ObservableList<String> styleClasses = rightIndicator.getStyleClass();
            final RMIServerImpl server = g.getParentController().getServer();

            int clients = 0;

            if (server != null) {
              clients = server.getConnectedClientsSize();
            }
            String changeToStyle = null;

            if (server == null) {
              changeToStyle = "rightIndicatorNoServer";
            } else if (clients == maxClients) {
              changeToStyle = "rightIndicatorWorking";
            } else if (clients == 0) {
              changeToStyle = "rightIndicatorStopped";
            } else {
              changeToStyle = "rightIndicatorStarting";
            }

            // ------------------------------------------------ //

            if (!progressIndicatorStyle.equals(changeToStyle)) {
              if (!styleClasses.contains(changeToStyle)) {
                final String styleToRemove = progressIndicatorStyle;
                final String styleToAdd = changeToStyle;
                Platform.runLater(new Runnable() {
                  @Override
                  public void run() {
                    Thread.currentThread().setName(
                        "JavaFX: Upper right indicator - visibility worker - sub2");
                    styleClasses.remove(styleToRemove);
                    styleClasses.add(styleToAdd);
                  }
                });
                progressIndicatorStyle = changeToStyle;
              }
            }

            // ------------------------------------------------ //
            // -- show short start screen across the screen
            // ------------------------------------------------ //

            if (firstStart) {
              Platform.runLater(new Runnable() {
                @Override
                public void run() {
                  Thread.currentThread().setName(
                      "JavaFX: Upper right indicator - visibility worker - sub2");
                  overlayLabelVbox.setVisible(true);
                  overlayLabel.setText(gui.getText("LOADING_SCREEN.STARTING"));
                  overlayDetailsLabel.setText(gui.getText("LOADING_SCREEN.CAN_TAKE_A_WHILE"));
                }
              });

              Thread.sleep(2000);
              Platform.runLater(new Runnable() {
                @Override
                public void run() {
                  Thread.currentThread().setName(
                      "JavaFX: Upper right indicator - visibility worker - sub3");
                  overlayLabelVbox.setVisible(false);
                }
              });
              firstStart = false;
            }

            // ------------------------------------------------ //
            if (controller.paused() || controller.finished()) {
              if (showIndicator) {
                showIndicator = !showIndicator;
                Platform.runLater(new Runnable() {
                  @Override
                  public void run() {
                    Thread.currentThread().setName(
                        "JavaFX: Upper right indicator - visibility worker - sub3");
                    rightIndicator.setVisible(false);
                    if (controller.finished()) {
                      overlayLabelVbox.setVisible(true);
                      overlayLabel.setText(gui.getText("LOADING_SCREEN.FINISHED"));
                      overlayDetailsLabel.setText(gui.getText("LOADING_SCREEN.CLOSE_OVERLAY"));
                    }
                  }
                });
              }
            } else {
              if (!showIndicator) {
                showIndicator = !showIndicator;
                Platform.runLater(new Runnable() {
                  @Override
                  public void run() {
                    Thread.currentThread().setName(
                        "JavaFX: Upper right indicator - visibility worker - sub4");
                    rightIndicator.setVisible(true);
                    overlayLabelVbox.setVisible(false);
                  }
                });
              }
            }
            // ------------------------------------------------ //

            Thread.sleep(waitTime);
          }
        } catch (Exception e) {
          e.printStackTrace();
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
        try {
          final SystemTools sigar = new SystemTools();
          while (!controller.stopped() && !Thread.currentThread().isInterrupted()) {
            DecimalFormat df = new DecimalFormat("#.00");
            double percent = sigar.getCpuPercent();

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

            if (controller.getStatus() == Status.STOPPED) {
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

        } catch (Exception e) {

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
  private Task<Object> ramWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          final SystemTools sigar = new SystemTools();
          while (!controller.stopped() && !Thread.currentThread().isInterrupted()) {
            final String freeRam = sigar.getUsedRamInReadable();
            final String maxRam = sigar.getMaxRamInReadable();
            final long freeRamMB = sigar.getFreeRamInMB();
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                if (freeRamMB > 2000) {
                  ram.setTextFill(Color.GREEN);
                } else if (freeRamMB > 1000) {
                  ram.setTextFill(Color.BLACK);
                } else {
                  ram.setTextFill(Color.DARKRED);
                }
              }
            });

            updateMessage("RAM: " + freeRam + "/" + maxRam);
            Thread.sleep(1000);
          }
        } catch (Exception e) {
        }
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
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: indexWorker");
        try {
          indexingStartTime = System.currentTimeMillis();

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
            g.getMessageQueue().put(startingToIndexIndicator());

            // 'scan database for missing files on start'
            if (i == 0) {
              g.getParentController().removeMissingFiles();
            }

            // start to index current folder:
            MainController.inst().startIndexing();

            if (controller.getStatus() == Status.STOPPED) {
              break;
            }

            // 'folder(s) indexing finished'-message {{{
            updateProgress(folderCount, folderCount);
            boolean allDirectoriesIndexed = (i == folderCount - 1);
            g.getMessageQueue().put(finishedToIndexIndicator(allDirectoriesIndexed));
            if (allDirectoriesIndexed) {
              // everything is done
            }
            // }}
          }

          controller.setStatus(Status.INDEXING_FINISHED);

          // update buttons {{{
          if (!controller.stopped()) {
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
  private void changeButtonImageColor() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        ImageView image = null;
        if (controller.getStatus() == Status.PAUSED) {
          image = new ImageView(this.getClass().getResource(Icon.PLAY.toString()).toString());
        } else {
          image = new ImageView(this.getClass().getResource(Icon.PAUSE.toString()).toString());
        }
        image.setFitHeight(13);
        image.setFitWidth(13);
        image.setTranslateX(1);
        pauseButton.setGraphic(image);
      }
    });
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
  private ProgressUpdate startingToIndexIndicator() throws Exception {
    final String startText = gui.getText("LOADING_SCREEN.STARTING_TO_INDEX", String
        .valueOf(currentFolderIndex + 1), String.valueOf(folderCount))
        + "\n";
    final String folder = St.trimToLengthIndicatorLeft(St
        .shortenHomePathInDirectory(currentFolder), 100)
        + "\n";
    final List<Node> progressText = new ArrayList<Node>();

    final Text startingFolderLabel = new Text(startText);
    startingFolderLabel.getStyleClass().add(STARTING_FOLDER_LABEL_CLASS);
    final Text startingFolderText = new Text(folder);
    startingFolderText.getStyleClass().add(STARTING_FOLDER_TEXT_CLASS);

    progressText.add(startingFolderLabel);
    progressText.add(startingFolderText);

    final ProgressUpdate ind = new ProgressUpdate(progressText, STARTING);
    return ind;
  }

  /**
   *
   *
   * @param folderOnly
   * @return
   */
  private ProgressUpdate finishedToIndexIndicator(boolean allDirectoriesIndexed) throws Exception {
    final List<Node> progressText = new ArrayList<Node>();
    final Text folderDone = new Text(gui.getText("LOADING_SCREEN.FOLDER_INDEXED") + "\n");
    folderDone.getStyleClass().add(FINISHED_FOLDER_TEXT_CLASS);
    progressText.add(folderDone);

    if (allDirectoriesIndexed) {
      final Text success = new Text(gui.getText("LOADING_SCREEN.ALL_JOBS_FINISHED") + "\n");
      success.getStyleClass().add(FINISHED_TEXT_CLASS);
      progressText.add(success);
    }
    final ProgressUpdate ind = new ProgressUpdate(progressText, INDEX_FINISHED);
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
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: progressWorker");
        try {
          RingBuffer<ProgressUpdate> buffer = new RingBuffer<ProgressUpdate>(
              MESSAGE_RINGBUFFER_SIZE);

          long currentTime = System.currentTimeMillis();
          long startTimeText = currentTime + REFRESH_TIME_TEXT_IN_MS;
          long startTimeLabel = currentTime + REFRESH_TIME_LABEL_IN_MS;

          // {{{ updating text in the progressbar
          updateMessage(gui.getText("LOADING_SCREEN.STARTING"));
          updateProgress(-1, -1);
          // }}}

          while (!controller.stopped() && !Thread.currentThread().isInterrupted()) {

            boolean showImmediately = false;
            final ProgressUpdate indicator = g.getMessageQueue().get();

            // @formatter:off
            final ProgressType type = indicator.getProgressType();
            final List<Node> texts = indicator.getProgressText();
            final Long processed = indicator.getFilesProcessed();
            final Long count = indicator.getFilesCount();
            // @formatter:off

            // @formatter:off
            if (type == STARTING || type == COUNTING_FILES_FINISHED || type == PAUSED
                || type == INDEX_FINISHED || type == NEW_FILE_FOUND) {
              showImmediately = true;
            }
            if (type == UPDATING_FILE_LIST || type == COUNTING_FILES) {
              updateProgress(LOADING, LOADING);
            }
            // @formatter:on

            if (controller.getStatus() == Status.STOPPED && !showImmediately) {
              break;
            }

            currentTime = System.currentTimeMillis();
            final boolean refreshTimePassedLabel = (currentTime - startTimeLabel) > REFRESH_TIME_LABEL_IN_MS;
            if (refreshTimePassedLabel || showImmediately) {

              // set label and progress of main progressbar {{{
              if (controller.getStatus() != Status.PAUSED) {
                if (type == INDEX_FINISHED) {
                  updateMessage(gui.getText("LOADING_SCREEN.PROGRESS", String
                      .valueOf(HUNDRED_PERCENT)));
                  updateProgress(HUNDRED_PERCENT, HUNDRED_PERCENT);
                } else if (processed != null && count != null) {
                  // aaa
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
                final String currentDirPath = St.trimToLengthIndicatorLeft(new File(
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
              startTimeLabel = System.currentTimeMillis();
            }

            // finally, update main text flow {{{
            currentTime = System.currentTimeMillis();
            final boolean refreshTimePassedText = (currentTime - startTimeText) > REFRESH_TIME_TEXT_IN_MS;
            if (refreshTimePassedText || showImmediately) {
              if (controller.getStatus() != Status.PAUSED || type == PAUSED) {
                updateTextFlow(buffer);
                startTimeText = System.currentTimeMillis();
              }
            }
            // }}}
          }
          updateMessage(gui.getText("LOADING_SCREEN.CANCELING"));

        } catch (Exception e) {
          LOG.error("ProgressWorker failed", e);
        }
        return true;
      }
    };
  }

  /**
   *
   *
   * @param buffer
   */
  private void updateTextFlow(RingBuffer<ProgressUpdate> buffer) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: updateTextFlow");
        ObservableList<Node> nodesToRemoveFirst = progressText.getChildren();

        // nodesToRemoveFirst.addListener();
        for (int i = nodesToRemoveFirst.size() - 1; i >= 0; i--) {
          Node nodeToRemove = nodesToRemoveFirst.get(i);
          progressText.getChildren().remove(nodeToRemove);
          nodeToRemove = null;
        }

        final Iterator<ProgressUpdate> iterator = buffer.iterator();
        int iteratorCount = 1;

        try {
          while (iterator.hasNext()) {
            final ProgressUpdate indicator = iterator.next();
            if (iteratorCount > 1) {
              progressText.getChildren().add(new Text(" \n "));
            }
            progressText.getChildren().add(new Text(" \n"));
            for (final Node text : indicator.getProgressText()) {
              final Text txt = ((Text) text);
              if (!txt.getText().trim().isEmpty()) {
                if (iteratorCount > 1
                    && (iteratorCount != 2 || controller.getStatus() != Status.PAUSED)) {
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
        } catch (Exception e) {
          // TODO: logging
          e.printStackTrace();
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

}
