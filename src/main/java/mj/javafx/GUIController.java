package mj.javafx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

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

import mj.MainController;
import mj.configuration.Config;
import mj.configuration.properties.ConfigString;
import mj.database.SearchResult;
import mj.events.EventManager;
import mj.events.ProgressIndicator;
import mj.events.Queue;
import mj.events.QueueMonitor;
import mj.javafx.controllers.Confirmation;
import mj.javafx.controllers.EditDatabase;
import mj.javafx.controllers.HelpBrowser;
import mj.javafx.controllers.LoadingScreen;
import mj.javafx.controllers.MessageDialog;
import mj.javafx.controllers.SearchDialog;
import mj.javafx.controllers.SelectDatabase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.LocaleUtils;

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
  private Queue<ProgressIndicator> messageQueue;
  private Queue<String> exceptionQueue;
  private Image mainIcon;

  private static GUIController instance;

  private SearchResult searchResult;
  private String metaDataSearchLuceneQuery;
  private String contentSearchLuceneQuery;
  private String lastMetaDataSearch;
  private String lastContentSearch;
  private String lastOperator;
  private boolean showMoreDetails;

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
        this.cfg.setUserConfigFilePath(configuration);
        this.cfg.updateFileProperties();
        this.mainController.initMultiCoreProcessing();
        this.mainController.refresh();
        return true;
      }
    }
    return false;
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    try {
      GUIController.instance = this;
      final String iconPath = this.getClass().getResource(Icon.STAGE_ICON.getFileName()).toString();
      this.mainIcon = new Image(iconPath);

      this.mainController = MainController.inst();
      this.primaryStage = primaryStage;
      this.cfg = Config.inst();
      this.initResourceBundle();

      EventManager.instance().addGUIHandler();
      EventManager.instance().initLoggerAppender();

      String lastCheckPoint = this.cfg.getProp(this.cfg.getDefaultConfigFilePath(),
          ConfigString.LAST_SESSION_CHECKPOINT);

      String lastUsedConfiguration = this.cfg.getProp(this.cfg.getDefaultConfigFilePath(),
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

      if (restoreSession && lastCheckPoint.equals(SearchDialog.FXML)) {
        this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
        this.mainController.initDatabase();
        this.cfg.setProp(this.cfg.getDefaultConfigFilePath(), ConfigString.LAST_SESSION_CHECKPOINT, "");
        this.cfg.setProp(this.cfg.getDefaultConfigFilePath(), ConfigString.LAST_USED_CONFIGURATION, "");
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
              exitApplication();
            }
          });
        }
      });

      // primaryStage.initStyle(StageStyle.UTILITY);
      // primaryStage.initStyle(StageStyle.UNDECORATED);

      this.exceptionQueue = new QueueMonitor<String>(10);
      Thread th = new Thread(exceptionWorker());
      th.setDaemon(true);
      th.start();

    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  /**
   *
   *
   * @param results
   * @param metaDataSearch
   * @param contentSearch
   * @param maxMetaDataLength
   * @param maxSnippetLength
   */
  public void setSearchResult(final SearchResult results, final String metaDataSearch,
      final String contentSearch) {
    this.searchResult = results;
    this.metaDataSearchLuceneQuery = metaDataSearch;
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
          false, true, width, height);
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
    if (this.cfg.isShutdown())
      return null;

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
    boolean thresholdReached = (Math.abs(stageWidth - this.lastPageWidth) > 100 && Math
        .abs(stageHeight - this.lastPageHeight) > 200)
        || primaryStage.isMaximized() || primaryStage.isFullScreen();
    // boolean thresholdReached = primaryStage.isMaximized() ||
    // primaryStage.isFullScreen();

    if (!Double.isNaN(stageWidth) && thresholdReached) {
      page.setPrefWidth(stageWidth);
    }

    if (!Double.isNaN(stageHeight) && thresholdReached) {
      page.setPrefHeight(stageHeight);
    }

    primaryStage.setTitle(Config.APP_NAME);

    this.lastPageWidth = currentPageWidth;
    this.lastPageHeight = currentPageHeight;

    primaryStage.sizeToScene();
    primaryStage.show();

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
      stage.setMinWidth(width);
      stage.setMinHeight(height);
      stage.getIcons().add(this.mainIcon);
      stage.setTitle(Config.APP_NAME);

      stage.setX(primaryStage.getX() + primaryStage.getWidth() / 2 - stage.getWidth() / 2);
      stage.setY(primaryStage.getY() + primaryStage.getHeight() / 2 - stage.getHeight() / 2);

      if (forceUserToInteract) {
        stage.initModality(Modality.APPLICATION_MODAL);
      } else if (setOwner) {
        stage.initOwner(primaryStage);
      }

      stage.show();

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

      stage.setScene(new Scene(root));
      stage.sizeToScene();

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

  private static final String LANGUAGE_RESOURCE_FOLDER = "mj.javafx.controllers.text";
  private ResourceBundle rb;

  /**
   *
   *
   * @param locale
   */
  public void setLocale(Locale locale) {
    this.rb = ResourceBundle.getBundle(LANGUAGE_RESOURCE_FOLDER, locale);
  }

  /**
   *
   *
   * @return
   */
  public Locale getLocale() {
    return this.rb.getLocale();
  }

  /**
   *
   *
   */
  private void initResourceBundle() {
    Locale defaultLocale = Locale.ENGLISH;
    String defaultLocaleFromProperties = this.cfg.getProp(ConfigString.DEFAULT_LOCALE);
    if (!defaultLocaleFromProperties.isEmpty()) {
      defaultLocale = LocaleUtils.toLocale(defaultLocaleFromProperties);
    }
    this.rb = ResourceBundle.getBundle(LANGUAGE_RESOURCE_FOLDER, defaultLocale);
  }

  /**
   *
   *
   * @param property
   * @param placeHolders
   * @return
   */
  public String getText(String property, String... placeHolders) {
    if (placeHolders.length == 0) {
      return this.rb.getString(property);
    } else {
      return MessageFormat.format(this.rb.getString(property), placeHolders);
    }
  }

  // ------------------------------------------------ //
  // -- exception handling
  // ------------------------------------------------ //

  /**
   *
   *
   * @param template
   */
  public synchronized void showExceptionScreen(String message) {
    try {
      if (this.cfg.isShutdown()) {
        return;
      }

      if (message != null && !message.trim().isEmpty()) {
        final double width = 530;
        final double height = 300;

        MessageDialog.setINIT_WIDTH(width);
        MessageDialog.setINIT_HEIGHT(height);

        Text redText = null;
        boolean firstMessage = false;
        if (this.messageDialog == null || this.secondaryStage == null) {

          this.messageDialog = (MessageDialog) showAsPopupWindow(
              FXMLFile.MESSAGE_DIALOG.toString(), true, true, false, width, height);
          firstMessage = true;

        } else {
          firstMessage = false;
        }

        redText = new Text(message);
        redText.setFill(Color.RED);
        redText.setFont(Font.font(java.awt.Font.MONOSPACED, 13));

        if (this.messageDialog != null) {
          messageDialog.initUserComponents();
          messageDialog.addNodes(true, false, firstMessage, redText);
          messageDialog.enableCloseApplicationButton();
          messageDialog.enableHelpButton();
          // TODO: TEXT
          messageDialog.setTitle("An error occurred...", Color.GREEN);
        }

      }
    } catch (Exception ex) {
      // TODO:
      ex.printStackTrace();
    }
  }

  private static final int REFRESH_TIME_EXCEPTION_IN_MS = 100;
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
        while (!Thread.currentThread().isInterrupted() && !cfg.isShutdown()) {
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
  public MainController getMainController() {
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
  public Queue<ProgressIndicator> getMessageQueue() {
    return messageQueue;
  }

  /**
   * @param messageQueue
   *          the messageQueue to set
   */
  public void setMessageQueue(Queue<ProgressIndicator> messageQueue) {
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
  public SearchResult getSearchResult() {
    return searchResult;
  }

  /**
   * @return the metaDataSearch
   */
  public String getMetaDataSearchLuceneQuery() {
    return metaDataSearchLuceneQuery;
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
   * @return the lastMetaDataSearch
   */
  public String getLastMetaDataSearch() {
    return lastMetaDataSearch;
  }

  /**
   * @param lastMetaDataSearch
   *          the lastMetaDataSearch to set
   */
  public void setLastMetaDataSearch(String lastMetaDataSearch) {
    this.lastMetaDataSearch = lastMetaDataSearch;
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
   * @return the lastOperator
   */
  public String getLastOperator() {
    return lastOperator;
  }

  /**
   * @param lastOperator
   *          the lastOperator to set
   */
  public void setLastOperator(String lastOperator) {
    this.lastOperator = lastOperator;
  }

  /**
   * @param showMoreDetails
   *          the showMoreDetails to set
   */
  public void setShowMoreDetails(boolean showMoreDetails) {
    this.showMoreDetails = showMoreDetails;
  }
}
