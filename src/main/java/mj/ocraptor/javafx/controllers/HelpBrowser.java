package mj.ocraptor.javafx.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mj.ocraptor.javafx.GUITemplate;

public class HelpBrowser extends GUITemplate {

  // @formatter:off
  public static double  INIT_WIDTH  = 700;
  public static double  INIT_HEIGHT = 500;
  public static final   String FXML = "HelpBrowser.fxml";
  // @formatter:on


  private String originalURL;
  private String originalAnchor;

  @FXML
  private WebView webView;

  @FXML
  private Button outline;

  @FXML
  private Button anchor;

  @FXML
  private Button contact;

  @FXML
  void anchorClicked(ActionEvent event) {
    this.gotoURL(this.originalURL + this.originalAnchor);
  }

  @FXML
  void outlineClicked(ActionEvent event) {
    this.gotoURL(this.originalURL + "#Outline");
  }

  @FXML
  void contactClicked(ActionEvent event) {
    this.gotoURL(this.originalURL + "#Contact");
  }

  @Override
  protected void initVisibility() {
    //
  }

  @Override
  protected void initLabels() {
    this.title.setText(g.getText("HELP_BROWSER.TITLE"));
    this.anchor.setText(g.getText("HELP_BROWSER.ANCHOR"));
    this.outline.setText(g.getText("HELP_BROWSER.OUTLINE"));
    this.contact.setText(g.getText("HELP_BROWSER.CONTACT_ME"));
  }

  /**
   */
  @Override
  public void initCustomComponents() {
    this.executeWorker(showDelayedWorker());
  }

  @Override
  protected double getWindowWidth() {
    return INIT_WIDTH;
  }

  @Override
  protected double getWindowHeight() {
    return INIT_HEIGHT;
  }

  @Override
  protected void initListeners() {
    //
  }

  @Override
  protected void asserts() {
    // TODO: javafx asserts
  }

  /**
   *
   *
   * @param url
   */
  private void gotoURL(String url) {
    final WebEngine webEngine = webView.getEngine();
    webEngine.load(url);
  }

  /**
   *
   *
   * @param url
   */
  public void setURL(String url, String anchor) {
    this.originalURL = url;
    this.originalAnchor = anchor;
    this.gotoURL(this.originalURL + anchor);
  }

  @Override
  protected void initEventHandlers() {
    //
  }
}
