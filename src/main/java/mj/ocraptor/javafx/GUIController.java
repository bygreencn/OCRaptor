package mj.ocraptor.javafx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import mj.ocraptor.MainController;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.events.ProgressUpdate;
import mj.ocraptor.events.Queue;
import mj.ocraptor.events.QueueMonitor;
import mj.ocraptor.javafx.controllers.Confirmation;
import mj.ocraptor.javafx.controllers.HelpBrowser;
import mj.ocraptor.javafx.controllers.LoadingScreen;
import mj.ocraptor.javafx.controllers.MessageDialog;
import mj.ocraptor.javafx.controllers.SearchDialog;
import mj.ocraptor.javafx.controllers.SelectDatabase;

/**
 *
 *
 * @author
 */
public class GUIController extends Application {
  private Stage primaryStage;
  private Stage secondaryStage;
  private Stage thirdStage;
  private Stage forthStage;

  private MainController mainController;
  private Config cfg;
  private MessageDialog messageDialog;
  private GUITemplate template;
  private Queue<ProgressUpdate> messageQueue;
  private Queue<String> exceptionQueue;
  private Image mainIcon;

  private static GUIController instance;

  private LuceneResult searchResult;
  private String contentSearchLuceneQuery;
  private String lastContentSearch;
  private boolean showMoreDetails;
  private Thread exceptionWorker;
  private Localization localization;

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
      GUIController.class);

  /**
   *
   *
   * @param configuration
   */
  public boolean loadConfiguration(String configuration) throws Exception {
    final File configFile = new File(configuration);
    if (configFile.exists() && configFile.canWrite()) {
      final File dbFolder = new File(this.cfg.getProp(configuration, ConfigString.DATABASE_FOLDER));
      if (dbFolder.exists() && dbFolder.canWrite()) {
        this.cfg.setConfigUserFilePath(configuration);
        this.cfg.updateFileProperties();
        this.mainController.initMultiCoreProcessing();
        this.mainController.refresh();
        return true;
      }
    }
    return false;
  }

  // JavaFX Application Thread
  @Override
  public void start(Stage primaryStage) throws Exception {
    try {
      Thread.currentThread().setName(Config.APP_NAME + "JavaFX: started thread");
      GUIController.instance = this;
      final String iconPath = this.getClass().getResource(Icon.STAGE_ICON.getFileName()).toString();
      this.mainIcon = new Image(iconPath);

      this.mainController = MainController.inst();
      this.primaryStage = primaryStage;
      this.cfg = Config.inst();
      this.localization = Localization.instance();

      EventManager.instance().addGUIHandler();
      EventManager.instance().initLoggerAppender();

      String lastCheckPoint = this.cfg.getProp(this.cfg.getConfigMasterFilePath(),
          ConfigString.LAST_SESSION_CHECKPOINT);

      String lastUsedConfiguration = this.cfg.getProp(this.cfg.getConfigMasterFilePath(),
          ConfigString.LAST_USED_CONFIGURATION);

      boolean restoreSession = false;
      if (!lastUsedConfiguration.isEmpty()) {
        String path = this.cfg.getUserFolder() + File.separator + lastUsedConfiguration
            + Config.PROPERTIES_EXTENSION;
        try {
          restoreSession = this.loadConfiguration(path);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      // TODO: restore session ?
      if (restoreSession && lastCheckPoint.equals(SearchDialog.FXML)) {
        this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
        this.mainController.initDatabase();
        this.cfg.setProp(this.cfg.getConfigMasterFilePath(), ConfigString.LAST_SESSION_CHECKPOINT,
            "");
      } else {
        this.gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
      }

      // this.gotoPage(SettingsManager.FXML, SettingsManager.INIT_WIDTH,
      // SettingsManager.INIT_HEIGHT);
      // this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH,
      // SearchDialog.INIT_HEIGHT);

      this.primaryStage.getIcons().add(mainIcon);

      primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent we) {
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Thread.currentThread().setName(Config.APP_NAME + "JavaFX: exit application");
              exitApplication();
            }
          });
        }
      });

      // primaryStage.initStyle(StageStyle.UTILITY);
      // primaryStage.initStyle(StageStyle.UNDECORATED);

      this.exceptionQueue = new QueueMonitor<String>(10);
      if (exceptionWorker == null) {
        exceptionWorker = new Thread(exceptionWorker());
        exceptionWorker.setDaemon(true);
        exceptionWorker.start();
      }

    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  /**
   *
   *
   * @param results
   * @param contentSearch
   */
  public void setSearchResult(final LuceneResult results, final String contentSearch) {
    this.searchResult = results;
    this.contentSearchLuceneQuery = contentSearch;
    this.showMoreDetails = false;
  }

  /**
   *
   *
   */
  public void exitApplication() {
    if (this.mainController != null) {
      this.mainController.shutdown(true);
    }
    if (this.primaryStage != null) {
      this.primaryStage.close();
      this.primaryStage = null;
    }
    if (this.secondaryStage != null) {
      this.secondaryStage.close();
      this.secondaryStage = null;
    }
    if (this.thirdStage != null) {
      this.thirdStage.close();
      this.thirdStage = null;
    }
    if (this.forthStage != null) {
      this.forthStage.close();
      this.forthStage = null;
    }
  }

  /**
   *
   *
   */
  public void pauseIndexing() {
    if (this.template != null && this.template instanceof LoadingScreen) {
      ((LoadingScreen) this.template).pauseProcess();
    }
  }

  /**
   *
   *
   * @param question
   */
  public void showConfirmationDialog(String question) {
    this.showConfirmationDialog(question, null, 350, 100, false);
  }

  /**
   *
   *
   * @param question
   */
  public void showConfirmationDialog(String question, double initWidth, double initHeight) {
    this.showConfirmationDialog(question, null, initWidth, initHeight, false);
  }

  /**
   *
   *
   * @param question
   * @param handler
   * @param initWidth
   * @param initHeight
   */
  public void showConfirmationDialog(String question, EventHandler<ActionEvent> handler,
      double initWidth, double initHeight) {
    this.showConfirmationDialog(question, handler, initWidth, initHeight, true);
  }

  /**
   *
   *
   * @param question
   * @param handler
   */
  public void showConfirmationDialog(String question, EventHandler<ActionEvent> handler) {
    this.showConfirmationDialog(question, handler, 350, 100, true);
  }

  /**
   *
   *
   * @param template
   */
  public void showConfirmationDialog(String question, EventHandler<ActionEvent> handler,
      double initWidth, double initHeight, boolean showCancelButton) {
    try {
      Confirmation.setINIT_WIDTH(initWidth);
      Confirmation.setINIT_HEIGHT(initHeight);

      Confirmation page = (Confirmation) showAsPopupWindow(FXMLFile.CONFIRMATION.toString(), false,
          false, true, initWidth, initHeight);

      page.addExternalOKButtonHandler(handler);
      page.addExternalCancelButtonHandler(null);
      page.setConfirmationText(question);

      if (!showCancelButton) {
        page.removeCancelButton();
      }
      page.initUserComponents();
    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  /**
   *
   *
   * @param question
   * @param yesHandler
   * @param noHandler
   * @param addCancelButton
   */
  public void showYesNoDialog(String question, EventHandler<ActionEvent> yesHandler,
      EventHandler<ActionEvent> noHandler, boolean addCancelButton) {
    showYesNoDialog(question, yesHandler, noHandler, addCancelButton, 350, 100);
  }

  /**
   *
   *
   * @param template
   */
  public void showYesNoDialog(String question, EventHandler<ActionEvent> yesHandler,
      EventHandler<ActionEvent> noHandler, boolean addCancelButton, double width, double height) {
    try {
      Confirmation.setINIT_WIDTH(width);
      Confirmation.setINIT_HEIGHT(height);

      Confirmation page = (Confirmation) showAsPopupWindow(FXMLFile.CONFIRMATION.toString(), false,
          true, true, width, height);
      page.addExternalOKButtonHandler(yesHandler);
      page.addExternalNOButtonHandler(noHandler);
      if (addCancelButton) {
        page.addExternalCancelButtonHandler(null);
      }
      page.setConfirmationText(question);
      page.initUserComponents();

    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  /**
   *
   *
   * @param message
   */
  public void showSimpleMessage(String message) {
    Text blackText = new Text(message);
    showMessage(300, 400, 5, "...", Color.BLACK, true, blackText);
  }

  /**
   *
   *
   * @param template
   */
  public void showMessage(double width, double height, double rightPadding, String title,
      Color titleColor, boolean wrapText, Node... messages) {
    try {
      MessageDialog.setINIT_WIDTH(width);
      MessageDialog.setINIT_HEIGHT(height);
      MessageDialog page = (MessageDialog) showAsPopupWindow(FXMLFile.MESSAGE_DIALOG.toString(),
          false, false, false, MessageDialog.INIT_WIDTH, MessageDialog.INIT_HEIGHT);

      if (wrapText) {
        page.wrapText();
      }

      page.setRightPadding(rightPadding);
      page.addNodes(messages);
      page.setTitle(title, titleColor);
      page.initUserComponents();

    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private double lastPageWidth = 0;
  private double lastPageHeight = 0;

  /**
   *
   *
   * @param template
   */
  public void gotoPage(final String template, final Double width, final Double height) {
    try {
      this.template = (GUITemplate) replaceSceneContent(template, width, height);
      this.template.initUserComponents();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   *
   *
   * @param fxml
   * @return
   *
   * @throws Exception
   */
  private Node replaceSceneContent(final String fxml, final Double width, final Double height)
      throws Exception {
    FXMLLoader loader = new FXMLLoader();
    InputStream in = SelectDatabase.class.getResourceAsStream(fxml);
    loader.setBuilderFactory(new JavaFXBuilderFactory());
    loader.setLocation(SelectDatabase.class.getResource(fxml));

    AnchorPane page = null;
    try {
      page = (AnchorPane) loader.load(in);
    } catch (Exception e) {
      // TODO:
      e.printStackTrace();
    } finally {
      if (in != null) {
        in.close();
      }
    }

    // TODO: provokes
    // this.primaryStage.setMinWidth(width - 50);
    // this.primaryStage.setMinHeight(height - 30);

    page.setPrefSize(width, height);

    // store the stage width and height in case the user has resized the window
    double stageWidth = primaryStage.getWidth();
    double stageHeight = primaryStage.getHeight();

    try {
      if (!Double.isNaN(stageWidth)) {
        stageWidth -= (primaryStage.getWidth() - primaryStage.getScene().getWidth());
      }
      if (!Double.isNaN(stageHeight)) {
        stageHeight -= (primaryStage.getHeight() - primaryStage.getScene().getHeight());
      }
    } catch (Exception e) {
    }

    double currentPageWidth = page.getPrefWidth();
    double currentPageHeight = page.getPrefHeight();

    Scene scene = new Scene(page);
    primaryStage.setScene(scene);

    // TODO: split screen:
    boolean thresholdReached = (Math.abs(stageWidth - this.lastPageWidth) > 100 && Math.abs(
        stageHeight - this.lastPageHeight) > 200) || primaryStage.isMaximized() || primaryStage
            .isFullScreen();
    // boolean thresholdReached = primaryStage.isMaximized() ||
    // primaryStage.isFullScreen();

    if (!Double.isNaN(stageWidth) && thresholdReached) {
      page.setPrefWidth(stageWidth);
    }

    if (!Double.isNaN(stageHeight) && thresholdReached) {
      page.setPrefHeight(stageHeight);
    }

    primaryStage.setTitle(getText("TEMPLATE.TITLE_TEXT", Config.APP_NAME,
        Config.HARDCODED_VERSION));

    this.lastPageWidth = currentPageWidth;
    this.lastPageHeight = currentPageHeight;

    if (!primaryStage.isShowing()) {
      primaryStage.show();
      primaryStage.centerOnScreen();
    }
    primaryStage.sizeToScene();

    // javafx.geometry.Rectangle2D primScreenBounds =
    // Screen.getPrimary().getVisualBounds();
    // primaryStage.setX((primScreenBounds.getWidth() - primaryStage.getWidth())
    // / 2);
    // primaryStage.setY((primScreenBounds.getHeight() -
    // primaryStage.getHeight()) / 2);

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setName(Config.APP_NAME + "Closing SplashScreen");
        mainController.closeSplashScreen();
      }
    });

    // this.primaryStage.show();
    return (Node) loader.getController();
  }

  /**
   *
   *
   * @param fxml
   * @param exceptionStage
   */
  protected void showHelp(String url, String anchor) {
    try {
      HelpBrowser page = (HelpBrowser) showAsPopupWindow(FXMLFile.HELP_BROWSER.toString(), false,
          false, false, HelpBrowser.INIT_WIDTH, HelpBrowser.INIT_HEIGHT);
      page.initUserComponents();
      page.setURL(url, anchor);
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
  }

  /**
   *
   *
   * @param fxml
   */
  private Node showAsPopupWindow(String fxml, boolean exceptionStage, boolean setOwner,
      boolean forceUserToInteract, Double width, Double height) {
    AnchorPane root;
    try {
      Stage stage = new Stage();
      stage.setWidth(width);
      stage.setHeight(height);
      stage.setMinWidth(150);
      stage.setMinHeight(100);
      stage.getIcons().add(this.mainIcon);
      stage.setTitle(Config.APP_NAME);

      stage.setX(primaryStage.getX() + primaryStage.getWidth() / 2 - stage.getWidth() / 2);
      stage.setY(primaryStage.getY() + primaryStage.getHeight() / 2 - stage.getHeight() / 2);

      if (forceUserToInteract) {
        stage.initModality(Modality.APPLICATION_MODAL);
      } else if (setOwner) {
        stage.initOwner(primaryStage);
      }

      FXMLLoader loader = new FXMLLoader();
      InputStream in = SelectDatabase.class.getResourceAsStream(fxml);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      loader.setLocation(SelectDatabase.class.getResource(fxml));

      try {
        root = (AnchorPane) loader.load(in);
      } finally {
        if (in != null) {
          in.close();
        }
      }

      root.setPrefSize(width, height);
      Node node = (Node) loader.getController();
      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //
      if (node != null) {
        if (node instanceof HelpBrowser) {
          if (this.forthStage != null) {
            this.forthStage.close();
          }
          this.forthStage = stage;
        } else if (node instanceof MessageDialog) {
          if (!exceptionStage) {
            if (this.secondaryStage != null) {
              this.secondaryStage.close();
            }
            this.secondaryStage = stage;
          } else {
            this.thirdStage = stage;
          }
        } else if (node instanceof Confirmation) {
          if (this.secondaryStage != null) {
            this.secondaryStage.close();
          }
          this.secondaryStage = stage;
        }
      }

      stage.setScene(new Scene(root));
      stage.sizeToScene();
      stage.show();
      Platform.runLater(new Runnable() {
        @Override
        public void run() {
          stage.toFront();
        }
      });
      // ------------------------------------------------ //

      return node;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param property
   * @param placeHolders
   * @return
   */
  public String getText(final String property, final Object... placeHolders) {
    return localization.getText(property, placeHolders);
  }

  // ------------------------------------------------ //
  // -- exception handling
  // ------------------------------------------------ //

  /**
   *
   *
   * @param template
   */
  public void showExceptionScreen(String message) {
    try {
      if (message != null && !message.trim().isEmpty()) {
        final double width = 800;
        final double height = 500;

        MessageDialog.setINIT_WIDTH(width);
        MessageDialog.setINIT_HEIGHT(height);

        Text redText = null;
        boolean firstMessage = false;

        if (this.messageDialog == null) {
          this.messageDialog = (MessageDialog) showAsPopupWindow(FXMLFile.MESSAGE_DIALOG.toString(),
              true, false, false, width, height);
          firstMessage = true;
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              primaryStage.toFront();
            }
          });
        } else {
          firstMessage = false;
        }

        redText = new Text(message);
        redText.setFill(Color.BLACK);
        redText.setFont(Font.font(java.awt.Font.MONOSPACED, 13));

        if (this.messageDialog != null) {
          messageDialog.initUserComponents();
          messageDialog.addNodes(true, false, firstMessage, redText);
          messageDialog.enableCloseApplicationButton();
          messageDialog.enableHelpButton();
          messageDialog.setTitle(getText("ERROR.ERROR_OCCURRED"), Color.GREEN);
        }
      }
    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  private static final int REFRESH_TIME_EXCEPTION_IN_MS = 1000;
  private static final int FIRST_EXCEPTION_WAITTIME = 100;
  private long startTimeMessageDialog = System.currentTimeMillis();

  /**
   *
   *
   * @return
   */
  private Task<Object> exceptionWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: show exception screen - 1");
        startTimeMessageDialog = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
          String message = exceptionQueue.get();

          if (messageDialog == null || secondaryStage == null) {
            Thread.sleep(FIRST_EXCEPTION_WAITTIME);
          }

          long timePassed = System.currentTimeMillis() - startTimeMessageDialog;

          if (timePassed < REFRESH_TIME_EXCEPTION_IN_MS) {
            Thread.sleep(REFRESH_TIME_EXCEPTION_IN_MS - timePassed);
          }
          startTimeMessageDialog = System.currentTimeMillis();

          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Thread.currentThread().setName(Config.APP_NAME + "JavaFX: show exception screen - 2");
              showExceptionScreen(message);
            }
          });

        }
        return null;
      }
    };
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   * @return the stage
   */
  public Stage getPrimaryStage() {
    return primaryStage;
  }

  /**
   * @return the ocraptor
   */
  public MainController getParentController() {
    return mainController;
  }

  /**
   * @return the instance
   */
  public static GUIController instance() {
    return instance;
  }

  /**
   * @return the template
   */
  public GUITemplate getTemplate() {
    return template;
  }

  /**
   * @return the messageQueue
   */
  public Queue<ProgressUpdate> getMessageQueue() {
    return messageQueue;
  }

  /**
   * @param messageQueue
   *          the messageQueue to set
   */
  public void setMessageQueue(Queue<ProgressUpdate> messageQueue) {
    this.messageQueue = messageQueue;
  }

  /**
   * @return the exceptionQueue
   */
  public Queue<String> getExceptionQueue() {
    return exceptionQueue;
  }

  /**
   * @return the searchResult
   */
  public LuceneResult getSearchResult() {
    return searchResult;
  }

  /**
   * @return the contentSearch
   */
  public String getContentSearchLuceneQuery() {
    return contentSearchLuceneQuery;
  }

  /**
   * @return the showMoreDetails
   */
  public boolean showMoreDetails() {
    return showMoreDetails;
  }

  /**
   * @return the lastContentSearch
   */
  public String getLastContentSearch() {
    return lastContentSearch;
  }

  /**
   * @param lastContentSearch
   *          the lastContentSearch to set
   */
  public void setLastContentSearch(String lastContentSearch) {
    this.lastContentSearch = lastContentSearch;
  }

  /**
   * @param showMoreDetails
   *          the showMoreDetails to set
   */
  public void setShowMoreDetails(boolean showMoreDetails) {
    this.showMoreDetails = showMoreDetails;
  }
}
