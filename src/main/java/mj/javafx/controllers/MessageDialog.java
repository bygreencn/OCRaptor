package mj.javafx.controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import mj.javafx.GUITemplate;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDialog extends GUITemplate {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  // @formatter:off
  public static double  INIT_WIDTH = 400;
  public static double  INIT_HEIGHT = 300;
  public static final   String FXML = "MessageDialog.fxml";
  // @formatter:on

  @FXML
  private TextFlow messageText;

  @FXML
  private Button closeApplicationButton;

  @FXML
  private Button copyButton;

  @FXML
  private Button closeWindowButton;

  @FXML
  private ScrollPane scrollPane;

  @FXML
  void closeApplicationButtonClicked(ActionEvent event) {
    this.mainController.exitApplication();
  }

  @FXML
  void copyButtonClicked(ActionEvent event) {
    this.executeWorker(clipboardWorker());
    copyButton.setText("Content copied!");
    copyButton.setTextFill(Color.DARKGREEN);
  }

  private Paint originalTextFill;

  /**
   *
   *
   * @return
   */
  private Task<Object> clipboardWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        StringBuffer plainText = new StringBuffer();

        for (Node textNode : messageText.getChildren()) {
          if (Line.class.isInstance(textNode)) {
            plainText.append(StringUtils.repeat("-", 100));
          } else if (Text.class.isInstance(textNode)) {
            Text text = (Text) textNode;
            plainText.append(text.getText());
          } else if (TextField.class.isInstance(textNode)) {
            TextField text = (TextField) textNode;
            plainText.append(text.getText());
          }
        }

        final ClipboardContent content = new ClipboardContent();
        content.putString(plainText.toString());

        if (!plainText.toString().trim().isEmpty()) {
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              Clipboard.getSystemClipboard().setContent(content);
            }
          });
        }
        Thread.sleep(5000);

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            copyButton.setText("Copy to Clipboard");
            copyButton.setTextFill(originalTextFill);
          }
        });
        return true;
      }
    };
  }

  @FXML
  void closeWindowButton(ActionEvent event) {
    // get a handle to the stage
    Stage stage = (Stage) copyButton.getScene().getWindow();
    // do what you have to do
    stage.close();
  }

  @Override
  protected void initVisibility() {
    this.helpButton.setManaged(false);
    this.helpButton.setVisible(false);

    this.closeApplicationButton.setManaged(false);
    this.closeApplicationButton.setVisible(false);

    this.originalTextFill = this.helpButton.getTextFill();
  }

  @Override
  protected void initLabels() {
    // TODO Auto-generated method stub
  }

  @Override
  public void initCustomComponents() {
    this.executeWorker(showDelayedWorker());
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private boolean autoScroll = true;

  @Override
  protected void initListeners() {
    messageText.heightProperty().addListener(new ChangeListener() {
      @Override
      public void changed(ObservableValue observable, Object oldvalue, Object newValue) {
        if (autoScroll) {
          scrollPane.setVvalue((Double) newValue);
          autoScroll = false;
        }
      }
    });
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

  /**
   *
   *
   * @param messages
   */
  public void addNodes(Node... messages) {
    this.addNodes(false, false, true, messages);
  }

  /**
   *
   *
   * @param message
   */
  public void addNodes(boolean useDivider, boolean autoScroll, boolean firstMessage, Node... nodes) {
    this.autoScroll = autoScroll;
    if (useDivider) {
      if (firstMessage) {
        this.messageText.getChildren().add(getDialogDivider());
      }
      this.messageText.getChildren().add(new Text(" \n"));
    }

    for (Node node : nodes) {
      this.messageText.getChildren().add(node);
    }

    this.messageText.getChildren().add(new Text("\n"));
    if (useDivider) {
      this.messageText.getChildren().add(getDialogDivider());
    }
  }

  /**
   *
   *
   * @return
   */
  private Line getDialogDivider() {
    return this.getDivider("messageDialogDivider", 1.0f, 35, 2d, 10d);
  }

  /**
   *
   *
   */
  public void setRightPadding(double padding) {
    this.messageText.setPadding(new Insets(5, padding, 5, 5));
  }

  /**
   *
   *
   */
  public void wrapText() {
    messageText.setMaxWidth(pane.getWidth() - 10);
    this.scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
    pane.widthProperty().addListener(new ChangeListener() {
      @Override
      public void changed(ObservableValue observable, Object oldvalue, Object newValue) {
        messageText.setMaxWidth((double) newValue - 10);
      }
    });
  }

  /**
   *
   *
   * @param title
   * @param color
   */
  public void setTitle(String title) {
    this.setTitle(title, Color.BLACK);
  }

  /**
   *
   *
   * @param title
   * @param color
   */
  public void setTitle(String title, Color color) {
    this.title.setText(title);
    if (color != null) {
      this.title.setTextFill(color);
    }
  }

  /**
   *
   *
   */
  public void enableHelpButton() {
    this.helpButton.setManaged(true);
    this.helpButton.setVisible(true);
  }

  /**
   *
   *
   */
  public void enableCloseApplicationButton() {
    this.closeApplicationButton.setManaged(true);
    this.closeApplicationButton.setVisible(true);
  }

  /**
   * @param initWidth
   *          the initWidth to set
   */
  public static void setINIT_WIDTH(double initWidth) {
    MessageDialog.INIT_WIDTH = initWidth;
  }

  /**
   * @param initHeight
   *          the initHeight to set
   */
  public static void setINIT_HEIGHT(double initHeight) {
    MessageDialog.INIT_HEIGHT = initHeight;
  }

  @Override
  protected void initEventHandlers() {
    // TODO Auto-generated method stub

  }
}
