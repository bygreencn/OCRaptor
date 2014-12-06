package mj.javafx.controllers;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import mj.javafx.GUITemplate;
import mj.javafx.Icon;

public class Confirmation extends GUITemplate {
  // @formatter:off
  public static double  INIT_WIDTH  = 300;
  public static double  INIT_HEIGHT = 100;
  public static final   String FXML = "Confirmation.fxml";
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @FXML
  private Button cancelButton;

  @FXML
  private Button okButton;

  @FXML
  private Button noButton;

  @FXML
  private Text message;

  @FXML
  void okButtonClicked(ActionEvent event) {
    if (this.externalOKButtonHandler != null) {
      this.externalOKButtonHandler.handle(event);
    }
    this.close();
  }

  @FXML
  void cancelButtonClicked(ActionEvent event) {
    if (this.externalCancelButtonHandler != null) {
      this.externalCancelButtonHandler.handle(event);
    }
    this.close();
  }

  @FXML
  void noButtonClicked(ActionEvent event) {
    if (this.externalNOButtonHandler != null) {
      this.externalNOButtonHandler.handle(event);
    }
    this.close();
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initVisibility() {
    this.noButton.setManaged(false);
    this.noButton.setVisible(false);

    this.cancelButton.setManaged(false);
    this.cancelButton.setVisible(false);
  }

  @Override
  protected void initLabels() {
    this.okButton.setText("OK");
    this.noButton.setText("NO");
    this.cancelButton.setText("Cancel");

    this.addImageIcon(this.okButton, Icon.YES, 0);
    this.addImageIcon(this.noButton, Icon.DELETE, 0);
  }

  @Override
  public void initCustomComponents() {
    this.message.setWrappingWidth(this.getWindowWidth() - 30);
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

  private EventHandler<ActionEvent> externalOKButtonHandler;
  private EventHandler<ActionEvent> externalNOButtonHandler;
  private EventHandler<ActionEvent> externalCancelButtonHandler;

  /**
   *
   *
   * @param handler
   */
  public void addExternalOKButtonHandler(EventHandler<ActionEvent> handler) {
    this.externalOKButtonHandler = handler;
  }

  /**
   *
   *
   * @param handler
   */
  public void addExternalNOButtonHandler(EventHandler<ActionEvent> handler) {
    this.noButton.setManaged(true);
    this.noButton.setVisible(true);
    this.noButton.setDefaultButton(true);
    this.okButton.setDefaultButton(false);
    this.okButton.setText("Yes");

    this.addImageIcon(this.noButton, Icon.DELETE, 0);
    this.addImageIcon(this.okButton, Icon.YES, 0);
    this.externalNOButtonHandler = handler;
  }

  /**
   *
   *
   * @param handler
   */
  public void addExternalCancelButtonHandler(EventHandler<ActionEvent> handler) {
    this.cancelButton.setManaged(true);
    this.cancelButton.setVisible(true);
    this.externalCancelButtonHandler = handler;
  }


  /**
   *
   *
   */
  public void removeCancelButton() {
    this.cancelButton.setManaged(false);
    this.cancelButton.setVisible(false);
  }

  /**
   *
   *
   * @param text
   */
  public void setConfirmationText(String text) {
    this.message.setText(text);
  }

  /**
   * @param initWidth the initWidth to set
   */
  public static void setINIT_WIDTH(double initWidth) {
    Confirmation.INIT_WIDTH = initWidth;
  }

  /**
   * @param initHeight the initHeight to set
   */
  public static void setINIT_HEIGHT(double initHeight) {
    Confirmation.INIT_HEIGHT = initHeight;
  }

  @Override
  protected void initEventHandlers() {
    // TODO Auto-generated method stub

  }
}
