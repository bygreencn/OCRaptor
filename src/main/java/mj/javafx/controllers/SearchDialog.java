package mj.javafx.controllers;

import java.io.StringReader;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import mj.MainController;
import mj.configuration.properties.ConfigBool;
import mj.javafx.GUITemplate;
import mj.javafx.Icon;
import mj.parser.ParseException;
import mj.parser.SearchStringParser;
import mj.parser.TokenMgrError;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchDialog extends GUITemplate {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  // @formatter:off
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 200;
  public static final   String FXML = "SearchDialog.fxml";
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @FXML
  private Button cancelButton;

  @FXML
  private Button searchButton;

  @FXML
  private TextArea searchField;

  @FXML
  private AnchorPane fullTextPane;

  @FXML
  private Button metaDataButton;

  @FXML
  private HBox fullTextHbox;

  @FXML
  private Label searchLabel;

  private boolean metaDataMode;

  @FXML
  void cancelButtonClicked(ActionEvent event) {
    this.setProgress(-1, "");
    this.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
  }

  @FXML
  void metaDataButtonClicked(ActionEvent event) {
    if (this.cfg.getProp(ConfigBool.INCLUDE_METADATA)) {
      this.outTransition();
      this.toggleSearchTypes();
      this.inTransitionAsWorker(200);

      if (!searchField.getText().trim().isEmpty()) {
        this.focusFullTextField();
      }
    }
  }

  @FXML
  void searchButtonClicked(ActionEvent event) {
    this.searchDatabase();
  }

  /**
   *
   *
   */
  private void searchDatabase() {
    try {
      if (!this.searchButton.isDisabled()) {
        boolean dbIncomplete = this.mainController.getMainController().getDb().databaseIncomplete(
            true);

        if (!dbIncomplete) {
          this.setProgress(-1, "Searching");
          this.outTransition();
          this.executeWorker(searchWorker());
        }

      }
    } catch (Exception e) {
      LOG.error("Search failed", e);
    }
  }

  /**
   *
   *
   */
  private void toggleSearchTypes() {
    if (this.cfg.getProp(ConfigBool.INCLUDE_METADATA)) {
      this.metaDataMode = !this.metaDataMode;
      if (this.metaDataMode) {
        this.metaDataButton.setText("Fulltext");
        this.searchField.setPromptText("Enter your metadata search query here.");
        this.searchLabel.setText("Metadata");
      } else {
        this.metaDataButton.setText("Metadata");
        this.searchField.setPromptText("Enter your fulltext search query here.");
        this.searchLabel.setText("Fulltext");
      }
    }
  }

  /**
   *
   *
   * @return
   */
  private Task<Object> searchWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        String searchString = createSearchStringStatement();
        MainController.inst().searchDatabase(searchString);

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (metaDataMode) {
              mainController.setLastMetaDataSearch(searchField.getText().trim());
              mainController.setLastContentSearch(null);
            } else {
              mainController.setLastContentSearch(searchField.getText().trim());
              mainController.setLastMetaDataSearch(null);
            }

            int resultSize = mainController.getSearchResult().getFileEntries().size();

            if (resultSize == 0) {
              mainController.showConfirmationDialog("No results for given query");
              inTransitionAsWorker(100);
              focusFullTextField();
            } else {
              gotoPage(SearchResult.FXML, SearchResult.INIT_WIDTH, SearchResult.INIT_HEIGHT);
            }
          }
        });
        return true;
      }
    };
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initVisibility() {
    this.helpButton.setDisable(false);
    this.searchButton.setDisable(true);
    this.addImageIcon(this.searchButton, Icon.SEARCH, 0);

    if (this.cfg.getProp(ConfigBool.INCLUDE_METADATA)) {
      this.metaDataButton.setVisible(true);
    } else {
      this.metaDataButton.setVisible(false);
      this.metaDataButton.setManaged(false);
    }
  }

  @Override
  protected void initLabels() {
    // TODO: text
    this.searchField.setText("");
    this.title.setText("Search database");
  }

  @Override
  public void initCustomComponents() {
    ChangeListener<Object> listener = new ChangeListener<Object>() {
      @Override
      public void changed(ObservableValue<? extends Object> arg0, Object oldPropertyValue,
          Object newPropertyValue) {
        checkIfInputIsValid();
      }
    };

    this.searchField.textProperty().addListener(listener);
    String lastContentSearch = this.mainController.getLastContentSearch();
    String lastMetaSearch = this.mainController.getLastMetaDataSearch();

    this.searchField.setPromptText("Enter your fulltext search query here.");

    if (lastContentSearch != null) {
      this.searchField.setText(lastContentSearch);
    } else if (lastMetaSearch != null) {
      this.searchField.setText(lastMetaSearch);
      this.toggleSearchTypes();
    }

    if (!this.searchField.getText().trim().isEmpty()) {
      this.focusFullTextField();
    }

    // this.searchField.setText("huckleberry");
    // this.searchField.setText("stephen king");
    this.checkIfInputIsValid();

    this.addTooltip(this.metaDataButton, "Switch between fulltext and metadata search.", -50, -60);
  }

  /**
   *
   *
   */
  private void removeText() {
    if (this.searchField.isFocused()) {
      this.searchField.setText("");
    }
  }

  /**
   *
   *
   */
  private void focusFullTextField() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        searchField.requestFocus();
        searchField.positionCaret(searchField.getText().length());
      }
    });
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

  /**
   *
   *
   */
  private void checkIfInputIsValid() {
    this.searchField.setText(this.searchField.getText().replace("\t", ""));
    final boolean fullTextEmpty = this.searchField.getText().trim().isEmpty();

    if (!fullTextEmpty) {
      this.searchButton.setDisable(false);
      this.addImageIcon(this.searchButton, Icon.SEARCH, 0);

      String exception = null;
      SearchStringParser parser = null;

      try {
        String statement = this.createSearchStringStatement();
        parser = new SearchStringParser(new StringReader(statement));
        parser.parse();
      } catch (TokenMgrError e) {
        exception = e.getMessage();
      } catch (ParseException e) {
        exception = e.getMessage();
      }

      if (exception == null) {
        this.searchButton.setDisable(false);
        this.addImageIcon(this.searchButton, Icon.SEARCH, 0);
      } else {
        this.searchButton.setDisable(true);
        this.addImageIcon(this.searchButton, Icon.SEARCH, 0);
      }

    } else {
      this.searchButton.setDisable(true);
      this.addImageIcon(this.searchButton, Icon.SEARCH, 0);
    }
  }

  /**
   * @return
   *
   *
   */
  private String createSearchStringStatement() {
    final boolean fullTextEmpty = this.searchField.getText().trim().isEmpty();
    String statement = null;

    if (!fullTextEmpty) {
      if (!metaDataMode) {
        statement = "C<" + this.searchField.getText().trim() + ">";
      } else {
        statement = "M<" + this.searchField.getText().trim() + ">";
      }
    }
    return statement;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param text
   */
  private void addText(String text, Integer pos) {
    if (this.searchField.isFocused()) {
      int position = this.searchField.getCaretPosition();
      if (pos != null) {
        position = pos;
      }
      if (position < 0) {
        position = this.searchField.getText().length();
      }
      final String currentText = this.searchField.getText().trim();
      final String leftText = //
      StringUtils.substring(currentText, 0, position).trim();
      final String rightText = //
      StringUtils.substring(currentText, position, currentText.length()).trim();
      this.searchField.setText((leftText + text + rightText).trim() + " ");
      this.searchField.positionCaret(leftText.length() + text.length());
    }
  }

  @Override
  protected void initEventHandlers() {

    this.pane.getScene().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
          if (searchField.isFocused()) {
            pane.requestFocus();
          } else {
            cancelButtonClicked(null);
          }
        } else if (event.getCode() == KeyCode.TAB) {
          focusFullTextField();
        } else if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
          searchDatabase();
        } else if (event.isControlDown() && event.getCode() == KeyCode.D) {
          removeText();
        } else if (event.isControlDown() && event.getCode() == KeyCode.J) {
          addText(" AND ", null);
        } else if (event.isControlDown() && event.getCode() == KeyCode.K) {
          addText(" OR ", null);
        } else if (event.isControlDown() && event.getCode() == KeyCode.L) {
          addText(" AND NOT ", null);
        } else if (event.isControlDown() && event.getCode() == KeyCode.M) {

          metaDataButtonClicked(null);
        } else if (metaDataMode && event.isControlDown() && event.getCode() == KeyCode.U) {
          addText(" KEY:", null);
        } else if (metaDataMode && event.isControlDown() && event.getCode() == KeyCode.I) {
          addText(" VALUE:", null);
        } else if (metaDataMode && event.isControlDown() && event.getCode() == KeyCode.N) {
          addText("\"", 0);
          addText("\"", -1);
        }
      }
    });
  }
}
