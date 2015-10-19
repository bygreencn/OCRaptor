package mj.ocraptor.javafx.controllers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import mj.ocraptor.MainController;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.database.error.TableEmptyException;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.javafx.GUITemplate;
import mj.ocraptor.javafx.Icon;

public class SearchDialog extends GUITemplate {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  // *INDENT-OFF*
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 235;
  public static final   String FXML = "SearchDialog.fxml";
  // *INDENT-ON*

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
  private HBox fullTextHbox;

  @FXML
  private Label searchLabel;

  @FXML
  void cancelButtonClicked(ActionEvent event) {
    this.setProgress(-1, "");
    this.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
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
        boolean dbIncomplete = false;
        // ------------------------------------------------ //
        try {
          this.g.getParentController().getDb().databaseIncomplete();
        } catch (Exception e) {
          dbIncomplete = true;
        }
        // ------------------------------------------------ //
        if (!dbIncomplete) {
          this.setProgress(-1, g.getText("SEARCH.SEARCHING"));
          this.outTransition();
          this.executeWorker(searchWorker());
        }
        // ------------------------------------------------ //
      }
    } catch (Exception e) {
      LOG.error("Search failed", e);
    }
  }

  /**
   *
   *
   * @param exception
   */
  private void luceneExceptionThrown(final Throwable exception) {
    // TODO: translation of most common lucene exceptions

    final List<Node> textNodes = new ArrayList<Node>();
    textNodes.add(new Text(g.getText("ERROR.LUCENE_ERROR")));
    textNodes.add(new Text("\n\n"));

    final Text rootCause = new Text(ExceptionUtils.getRootCauseMessage(exception));
    rootCause.setFill(Color.DARKRED);
    textNodes.add(rootCause);

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        g.showMessage(450d, 200d, 20d, g.getText("ERROR.LUCENE_TITLE"), Color.DARKRED, true,
            textNodes.toArray(new Node[textNodes.size()]));
      }
    });
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
        String searchString = searchField.getText().trim();
        MainController.inst().searchDatabase(searchString);

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            Thread.currentThread().setName(Config.APP_NAME + "JavaFX: User search");
            g.setLastContentSearch(searchField.getText().trim());

            final LuceneResult result = g.getSearchResult();
            final Throwable throwable = result.getThrowable();

            if (throwable != null) {
              luceneExceptionThrown(throwable);
              inTransitionAsWorker(100);
            } else if (result.getFileEntries().isEmpty()) {
              g.showConfirmationDialog(g.getText("SEARCH.NO_RESULTS"));
              inTransitionAsWorker(100);
              focusFullTextField();
            } else {
              // show result page to user
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
  }

  @Override
  protected void initLabels() {
    this.searchField.setText("");
    this.title.setText(g.getText("SEARCH.TITLE"));
    this.cancelButton.setText(g.getText("BACK_BUTTON"));
    this.searchButton.setText(g.getText("SEARCH_BUTTON"));
    this.searchLabel.setText(g.getText("SEARCH.FULLTEXT"));
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
    String lastContentSearch = this.g.getLastContentSearch();

    this.searchField.setPromptText(g.getText("SEARCH.FULLTEXT_PROMPT"));

    if (lastContentSearch != null) {
      this.searchField.setText(lastContentSearch);
    }

    if (!this.searchField.getText().trim().isEmpty()) {
      this.focusFullTextField();
    }

    this.checkIfInputIsValid();
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
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: focus search textfield");
        searchField.requestFocus();
        searchField.positionCaret(searchField.getText().length());
      }
    });
  }

  @Override
  protected void initListeners() {
    //
  }

  @Override
  protected void asserts() {
    // TODO: javafx asserts
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
    } else {
      this.searchButton.setDisable(true);
      this.addImageIcon(this.searchButton, Icon.SEARCH, 0);
    }
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
        }
      }
    });
  }
}
