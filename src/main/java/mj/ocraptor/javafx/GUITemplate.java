package mj.ocraptor.javafx;

import ij.plugin.BrowserLauncher;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.GrayFilter;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.javafx.controllers.SearchDialog;
import mj.ocraptor.tools.St;

public abstract class GUITemplate extends AnchorPane {

  @FXML
  protected ResourceBundle resources;

  @FXML
  protected URL location;

  @FXML
  protected Label title;

  @FXML
  protected ProgressIndicator progressIndicator;

  @FXML
  protected HBox progressBox;

  @FXML
  protected SplitPane pane;

  @FXML
  protected Button githubButton;

  @FXML
  protected Button resizeButton;

  @FXML
  protected Button helpButton;

  @FXML
  protected Label progressLabel;

  //
  protected GUIController g;

  protected Config cfg;

  public static final Integer DEFAULT_PROGRESS_ICON_SIZE = 53;
  public static final Integer SEARCH_PROGRESS_ICON_SIZE = 75;

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(GUITemplate.class);

  public void setProgress(double value, String text) {
    final GUITemplate template = this;
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        progressIndicator.setPrefWidth(DEFAULT_PROGRESS_ICON_SIZE);
        progressIndicator.setPrefHeight(DEFAULT_PROGRESS_ICON_SIZE);

        if (value == -2) {
          progressIndicator.setProgress(-1);
          progressLabel.setText("");
          progressIndicator.setVisible(false);
          progressBox.setVisible(false);
        } else {
          progressIndicator.setProgress(value);
          progressBox.setVisible(true);
          progressIndicator.setVisible(true);

          if (text != null && progressLabel != null) {
            progressLabel.setText(text);
          }

          if (value != -1 && template instanceof SearchDialog) {
            progressIndicator.setPrefWidth(SEARCH_PROGRESS_ICON_SIZE);
            progressIndicator.setPrefHeight(SEARCH_PROGRESS_ICON_SIZE);
          }
        }
      }
    });
  }

  /**
   *
   */
  @FXML
  public void initialize() {
    this.cfg = Config.inst();
    this.setProgress(-1.0, "");

    AnchorPane mainpane = (AnchorPane) this.pane.getParent();
    Theme theme = Theme.getByName(this.cfg.getProp(ConfigString.THEME));
    mainpane.getStylesheets().clear();
    mainpane.getStylesheets().add(this.getClass().getResource(theme.toString()).toString());

    this.g = GUIController.instance();
    this.initTemplateLabels();
    this.initLabels();
    this.initVisibility();

    this.initListeners();
  }

  @FXML
  protected void helpButtonClicked(ActionEvent event) {
    this.openHelpInBrowser(Config.getHelpFilePath(Localization.instance().getLocale()), this.getClass()
        .getSimpleName());
  }

  /**
   *
   *
   */
  public void initTemplateLabels() {
    // if (this.progressBox != null) {
    // this.progressBox.setVisible(false);
    // }

    // if (this.progressIndicator != null) {
    // this.progressIndicator.setVisible(false);
    // }
  }

  /**
   *
   *
   */
  public void close() {
    Stage stage = (Stage) this.pane.getScene().getWindow();
    stage.close();
  }

  private static final Integer IN_TRANSITION_TIME = 150;
  private static final Double MIN_TRANSITION_VALUE = 0.0;
  private static final Double MAX_TRANSITION_VALUE = 1.0;

  private static final Integer OUT_TRANSITION_TIME = 150;
  private static final Integer OUT_TRANSITION_TIME_FOR_BROWSER = 1500;

  /**
   *
   *
   * @param delay
   */
  protected void inTransitionAsWorker(int delay) {
    this.executeWorker(inTransitionWorker(delay));
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        pane.getScene().setCursor(Cursor.DEFAULT);
      }
    });
  }

  /**
   *
   *
   */
  public void initUserComponents() {
    this.initCustomComponents();
    this.initEventHandlers();

    if (this.helpButton != null) {
      this.addTooltip(this.helpButton, this.g.getText("HELP_TOOLTIP"), -100, -60);
    }

    if (this.githubButton != null) {
      this.addTooltip(this.githubButton, this.g.getText("TEMPLATE.OPEN_SOURCE_INFO"), 65, 0);
    }

    if (this.resizeButton != null) {
      this.addTooltip(this.resizeButton, this.g.getText("TEMPLATE.REFRESH_WINDOW"), -205, 0);
    }

    this.pane.getScene().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.F1) {
          openHelpInBrowser(Config.getHelpFilePath(Localization.instance().getLocale()), "");
        }
      }
    });

    this.inTransition();
  }

  /**
   *
   *
   * @return
   */
  protected Task<Object> showDelayedWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        Stage stage = (Stage) pane.getScene().getWindow();
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (stage != null) {
              stage.show();
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
  private Task<Object> inTransitionWorker(int delay) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        inTransition();
        return true;
      }
    };
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> outTransitionWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        outTransition();
        return true;
      }
    };
  }

  /**
   *
   *
   * @param node
   */
  protected void inTransition() {
    // if (this.cfg.getProp(this.cfg.getDefaultConfigFilePath(),
    // ConfigBool.ENABLE_ANIMATIONS)) {
    // FadeTransition fadeTransition = new
    // FadeTransition(Duration.millis(IN_TRANSITION_TIME), pane);
    // fadeTransition.setFromValue(MIN_TRANSITION_VALUE);
    // fadeTransition.setToValue(MAX_TRANSITION_VALUE);
    // fadeTransition.play();
    // }

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        if (progressIndicator != null) {
          progressIndicator.setVisible(false);
        }
        if (progressBox != null) {
          progressBox.setVisible(false);
        }
        pane.setOpacity(1.0);
        pane.getScene().setCursor(Cursor.DEFAULT);
      }
    });
  }

  /**
   *
   *
   */
  protected void outTransitionAsWorker() {
    this.executeWorker(outTransitionWorker());
  }

  /**
   *
   *
   * @param node
   */
  protected void outTransition() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        pane.requestFocus();
        pane.getScene().setCursor(Cursor.WAIT);
      }
    });

    if (!this.progressBox.isVisible()) {
      Platform.runLater(new Runnable() {
        @Override
        public void run() {
          if (progressBox != null) {
            progressBox.setVisible(true);
          }
          if (progressIndicator != null) {
            progressIndicator.setVisible(true);
          }
          pane.setOpacity(0.1);
        }
      });

      // if (this.pane != null) {
      // FadeTransition fadeTransition = new
      // FadeTransition(Duration.millis(OUT_TRANSITION_TIME),
      // this.pane);
      // fadeTransition.setFromValue(MAX_TRANSITION_VALUE);
      // fadeTransition.setToValue(MIN_TRANSITION_VALUE);
      // fadeTransition.play();
      // }
    }
  }

  /**
   *
   *
   * @param template
   */
  protected void gotoPage(final String template, final Double width, final Double height) {
    this.outTransition();
    this.executeWorker(pageWorker(template, width, height));
  }

  /**
   *
   *
   * @param worker
   */
  protected void executeWorker(Task<Object> worker) {
    if (this.progressIndicator != null) {
      // progressIndicator.progressProperty().bind(worker.progressProperty());
    }
    Thread th = new Thread(worker);
    th.setDaemon(true);
    th.start();
  }

  @FXML
  protected void githubButtonClicked(ActionEvent event) {
    outTransition();
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        try {
          BrowserLauncher.openURL(cfg.getProp(ConfigString.GITHUB_URL));
          inTransitionAsWorker(OUT_TRANSITION_TIME_FOR_BROWSER);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private Double currentXPos = null;
  private Double currentYPos = null;

  @FXML
  protected void resizeButtonClicked(ActionEvent event) {
    this.resizeMainPane();
  }

  /**
   *
   *
   * @param theme
   */
  protected void changeThemeTo(Theme theme) {
    AnchorPane mainpane = (AnchorPane) this.pane.getParent();
    Stage stage = g.getPrimaryStage();
    stage.hide();
    mainpane.getStylesheets().clear();
    mainpane.getStylesheets().add(this.getClass().getResource(theme.toString()).toString());
    stage.show();
  }

  /**
   *
   *
   */
  protected void resizeMainPane() {
    AnchorPane mainpane = (AnchorPane) this.pane.getParent();
    Stage stage = g.getPrimaryStage();

    double x = stage.getX();
    double y = stage.getY();
    stage.hide();

    stage.setMaximized(false);
    stage.setFullScreen(false);

    if (!Double.isNaN(x) && !Double.isNaN(y)) {
      if (!stage.isMaximized() && !stage.isFullScreen() && (mainpane.getWidth() < 810)
          && (mainpane.getHeight() < 700)) {
        stage.setX(x);
        stage.setY(y);
      } else {
        if (this.currentXPos != null && this.currentYPos != null) {
          stage.setX(this.currentXPos);
          stage.setY(this.currentYPos);
        }
      }
      this.currentXPos = this.currentXPos == null ? x : this.currentXPos;
      this.currentYPos = this.currentYPos == null ? y : this.currentYPos;
    }

    mainpane.setPrefWidth(this.getWindowWidth());
    mainpane.setPrefHeight(this.getWindowHeight());

    stage.show();
  }

  /**
   *
   *
   */
  protected void openHelpInBrowser(String helpFilePath, String anchor) {
    // outTransition();
    // inTransitionAsWorker(OUT_TRANSITION_TIME_FOR_BROWSER);

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        try {
          File helpFile = new File(helpFilePath);
          if (helpFile.exists()) {
            g.showHelp(helpFile.toURI().toString(), (anchor == null ? "" : "#" + anchor));
          }
        } catch (Exception e) {
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
  private Task<Object> pageWorker(final String template, final Double width, final Double height) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        try {
          Thread.sleep(150);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            g.gotoPage(template, width, height);
          }
        });
        return true;
      }
    };
  }

  /**
   *
   *
   * @param button
   * @param icon
   * @param translateX
   */
  protected void addImageIcon(ToggleButton button, Icon icon, int translateX) {
    ImageView image = new ImageView(this.getClass().getResource(icon.getFileName()).toString());
    image.setTranslateX(translateX);
    image.setFitHeight(14);
    image.setFitWidth(14);
    button.setGraphic(image);
  }

  /**
   *
   *
   * @param button
   * @param icon
   */
  protected void addImageIcon(Button button, Icon icon, int translateX) {
    ImageView image = new ImageView(this.getClass().getResource(icon.getFileName()).toString());
    image.setTranslateX(translateX);
    image.setFitHeight(14);
    image.setFitWidth(14);
    button.setGraphic(image);
  }

  /**
   *
   *
   * @return
   */
  protected Line getDivider(String styleClass, float strokeWidth, int rightDistance,
      Double... dashArrayElements) {
    Line redLine = new Line(0, 0, pane.getWidth() - rightDistance, 0);
    redLine.setManaged(true);
    redLine.setSmooth(true);
    redLine.setStrokeWidth(strokeWidth);
    redLine.getStrokeDashArray().addAll(dashArrayElements);
    redLine.getStyleClass().add(styleClass);

    pane.widthProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue,
          Number newValue) {
        redLine.setEndX((double) newValue - rightDistance);
      }
    });
    return redLine;
  }

  /**
   *
   *
   * @param image
   * @return
   *
   * @throws IOException
   */
  private javafx.scene.image.Image createImage(java.awt.Image image) throws IOException {
    if (!(image instanceof RenderedImage)) {
      BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
          BufferedImage.TYPE_INT_ARGB);
      Graphics g = bufferedImage.createGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      image = bufferedImage;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write((RenderedImage) image, "png", out);
    out.flush();
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    return new javafx.scene.image.Image(in);
  }

  /**
   *
   *
   * @param icon
   * @return
   */
  protected javafx.scene.image.Image getGreyscaleIcon(Icon icon) {
    javafx.scene.image.Image fxImage = null;
    try {
      BufferedImage bf = ImageIO.read(this.getClass().getResource(icon.getFileName()));

      ImageFilter filter = new GrayFilter(true, 70);
      ImageProducer producer = new FilteredImageSource(bf.getSource(), filter);
      Image mage = Toolkit.getDefaultToolkit().createImage(producer);
      fxImage = createImage(mage);
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
    return fxImage;
  }

  private static final String EXECUTION_ERROR = "Error executing your command";

  /**
   *
   *
   * @param filePath
   * @return
   */
  protected Task<Object> openFileWorker(String filePath) {
    return openFileWorker(filePath, false, 0);
  }

  final static double TOOLTIP_WIDTH = 200;

  // *INDENT-OFF*
  // TODO: style, can't be read directly from css-file
  final static String TOOLTIP_STYLE =
    "-fx-background-color:  #123462;" +
    "-fx-background-insets: 0;" +
    "-fx-background-radius: 0 0 0 0;" +
    "-fx-font-size:         13px;" +
    "-fx-padding:           0.333333em 0.666667em 0.333333em 0.666667em;" +
    "-fx-effect:            dropshadow( one-pass-box , #808080 , 5, 0.0 , 0 , 0 );" +
    "-fx-text-fill:         white;";
  // *INDENT-ON*

  /**
   *
   *
   * @param node
   * @param text
   * @param xOffset
   *          left offset
   * @param yOffset
   *          top offset
   */
  protected void addTooltip(Node node, String text, double xOffset, double yOffset) {
    try {
      final Tooltip tooltip = new Tooltip(text);
      tooltip.setStyle(TOOLTIP_STYLE);
      tooltip.setMaxWidth(TOOLTIP_WIDTH);
      tooltip.setWrapText(true);

      node.setOnMouseEntered(new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
          Point2D point = node.localToScene(0.0, 0.0);
          double posX = point.getX() + node.getScene().getX() + node.getScene().getWindow().getX()
              + xOffset;
          double posY = point.getY() + node.getScene().getY() + node.getScene().getWindow().getY()
              + yOffset;
          tooltip.show(node, posX, posY);
        }
      });

      EventHandler<Event> hideTooltip = new EventHandler<Event>() {
        @Override
        public void handle(Event event) {
          tooltip.hide();
        }
      };
      node.setOnMousePressed(hideTooltip);
      node.setOnMouseExited(hideTooltip);

      this.pane.getScene().addEventHandler(KeyEvent.KEY_PRESSED, hideTooltip);
    } catch (Exception e) {
    }
  }

  /**
   *
   *
   * @return
   */
  protected Task<Object> openFileWorker(String filePath, boolean openParentDirectory, int page) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        String[] errOutput = g.getParentController().getDb().openFile(filePath, page,
            openParentDirectory, 60);

        String error = errOutput[0];
        String command = errOutput[1];

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            List<Node> dialogMessage = null;
            if (command == null || command.trim().isEmpty()) {
              dialogMessage = new ArrayList<Node>();
              dialogMessage.add(new Text("Your command is empty"));
              g.showMessage(450, 150, 5, EXECUTION_ERROR, Color.DARKRED, false, dialogMessage
                  .toArray(new Node[dialogMessage.size()]));
            } else if (error != null && !error.trim().isEmpty()) {
              dialogMessage = new ArrayList<Node>();
              dialogMessage.add(new Text("Your command is not valid:\n"));
              Text commandString = new Text(command + "\n");
              commandString.setFill(Color.DARKRED);
              dialogMessage.add(commandString);
              dialogMessage.add(new Text("Stderr-message:\n"));
              Text errorString = new Text(error);
              errorString.setFill(Color.DARKRED);
              dialogMessage.add(errorString);

              if (cfg.getProp(//
                  ConfigBool.ENABLE_USER_COMMAND_STDERR)) {
                g.showMessage(500, 200, 5, EXECUTION_ERROR, Color.DARKRED, false, dialogMessage
                    .toArray(new Node[dialogMessage.size()]));
              }
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
  public Task<Object> propValidatingWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        String error = "";
        final String loadedConfigFile = cfg.getConfigUserFilePath();

        // ------------------------------------------------ //
        final String invalidConfigError = cfg.invalidConfigVersion(loadedConfigFile);
        if (invalidConfigError != null) {
          for (int i = 0; i < 50; i++) {
            if (GUIController.instance().getExceptionQueue() != null
                || Thread.currentThread().isInterrupted()) {
              break;
            }
            Thread.sleep(100);
          }
          error += invalidConfigError;
          error += "\nConfig: " + loadedConfigFile;
        }
        // ------------------------------------------------ //
        cfg.validateProperties(loadedConfigFile);
        final Map<String, LinkedHashSet<String>> notFoundProperties = cfg.getNotFoundProperties();
        final String configFile = loadedConfigFile;

        if (notFoundProperties.containsKey(configFile)) {
          final LinkedHashSet<String> properties = notFoundProperties.get(configFile);
          String missingProperties = "";
          for (String missingProperty : properties) {
            missingProperties += "\n -> " + missingProperty;
          }
          if (!missingProperties.isEmpty()) {
            missingProperties = St.removeLastLineBreak(missingProperties);
            if (!error.isEmpty()) {
              error += "\n\n";
            }
            error += "Missing properties in config file:\n" + configFile + missingProperties;
          }
        }
        // ------------------------------------------------ //
        if (!error.isEmpty()) {
          LOGGER.error(error);
        }
        return true;
      }
    };
  }

  public abstract void initCustomComponents();

  protected abstract void initVisibility();

  protected abstract void initLabels();

  protected abstract double getWindowWidth();

  protected abstract double getWindowHeight();

  protected abstract void initListeners();

  protected abstract void initEventHandlers();

  protected abstract void asserts();

}
