package mj.ocraptor.javafx.controllers;

import static mj.ocraptor.configuration.properties.ConfigInteger.DIALOG_SNIPPET_MAX_STRING_LENGTH;
import static mj.ocraptor.configuration.properties.ConfigInteger.MAX_SEARCH_RESULTS;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import mj.ocraptor.MainController;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.database.DBManager;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.FileEntryDao;
import mj.ocraptor.database.search.PartialEntry;
import mj.ocraptor.database.search.StyledSnippet;
import mj.ocraptor.database.search.StyledSnippetType;
import mj.ocraptor.database.search.TextProcessing;
import mj.ocraptor.events.Event;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.events.QueueMonitor;
import mj.ocraptor.file_handler.filter.FileType;
import mj.ocraptor.javafx.GUITemplate;
import mj.ocraptor.tools.St;
import mj.ocraptor.tools.Tp;

import org.apache.lucene.search.Query;

public class SearchResult extends GUITemplate {
  // ------------------------------------------------ //
  // *INDENT-OFF*
  public static final  double   INIT_WIDTH       = 700;
  public static final  double   INIT_HEIGHT      = 500;
  public static final  Integer  TAB_WIDTH        = 10;

  public static final  String FXML = "SearchResult.fxml";

  private static final String SNIPPET_TITLE_CLASS = "snippetTitle";
  // *INDENT-ON*

  // ------------------------------------------------ //

  private int resultSize, maxSnippetLength;
  private SortedSet<Map.Entry<FileEntry, Double>> resultSet;
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(SearchResult.class);

  private boolean cancelResultGeneration;

  private static final String HIGHLIGHTED_SNIPPET = "highlightedSnippet";

  // ------------------------------------------------ //

  @FXML
  private Button backButton;

  @FXML
  private Button removeButton;

  @FXML
  private TextFlow resultText;

  @FXML
  private Button detailButton;

  @FXML
  private ScrollPane resultScrollPane;

  @FXML
  private AnchorPane progressAnchor;

  @FXML
  private ProgressBar runningProgressBar;

  @FXML
  private ProgressBar progressBar;

  @FXML
  private Label searchResultPercentage;

  // ------------------------------------------------ //

  @Override
  protected void initLabels() {
    this.backButton.setText(g.getText("BACK_BUTTON"));
    this.helpButton.setText(g.getText("HELP"));
    this.removeButton.setText(g.getText("SEARCH_RESULT.REMOVE_ENTRIES"));
  }

  @Override
  protected void initListeners() {
    //
  }

  @Override
  protected void asserts() {
    //
  }

  @Override
  protected double getWindowHeight() {
    return INIT_HEIGHT;
  }

  @Override
  protected double getWindowWidth() {
    return INIT_WIDTH;
  }

  @Override
  protected void initVisibility() {
    this.helpButton.setDisable(false);
    this.runningProgressBar.setProgress(-1);
    if (!this.cfg.getProp(ConfigBool.ENABLE_ENTRY_DELETION_BY_USER)) {
      this.removeButton.setVisible(false);
      this.removeButton.setManaged(false);
    }
  }

  @FXML
  void backButtonClicked(ActionEvent event) {
    this.cancelResultGeneration = true;
    this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
  }

  @FXML
  void removeButtonClicked(ActionEvent event) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        Connection connection = null;
        final MainController controller = MainController.inst();
        final DBManager db = controller.getDb();
        try {
          connection = db.getConnection();
          FileEntryDao fileEntryDao = new FileEntryDao();

          for (Map.Entry<FileEntry, Double> fileEntry : resultSet) {
            final FileEntry file = fileEntry.getKey();
            fileEntryDao.removeByPath(file.getPath(), connection);
          }
          gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
        } catch (Exception e) {
          LOGGER.error("can not delete db entries", e);
        } finally {
          if (connection != null) {
            try {
              connection.close();
            } catch (SQLException e) {
              e.printStackTrace();
            }
          }
        }
      }
    };
    g.showConfirmationDialog(g.getText("SEARCH_RESULT.REMOVE_ENTRIES_DIALOG"), handler);
  }

  @FXML
  void detailButtonClicked(ActionEvent event) {
    if (this.g.showMoreDetails()) {
      this.g.setShowMoreDetails(false);
    } else {
      this.g.setShowMoreDetails(true);
    }
    this.cancelResultGeneration = true;
    this.gotoPage(SearchResult.FXML, SearchResult.INIT_WIDTH, SearchResult.INIT_HEIGHT);
  }

  @Override
  public void initCustomComponents() {
    // *INDENT-OFF*
    this.maxSnippetLength =
        this.cfg.getProp(DIALOG_SNIPPET_MAX_STRING_LENGTH)
        + Config.SEARCH_DELIMITER_START.toString().length()
        + Config.SEARCH_DELIMITER_END.toString().length();
    // ------------------------------------------------ //
    // add result to textflow
    QueueMonitor<List<Node>> nodesToAdd = getResultNodes(
    // ------------------------------------------------ //
    // results
        this.g.getSearchResult(),
        // fulltext-search-query for highlighting
        this.g.getContentSearchLuceneQuery(), null // id of file-entry to
                                                   // highlight (null == show
                                                   // all entries)
    );
    // ------------------------------------------------ //
    // *INDENT-ON*

    this.executeWorker(updateTextFlowWorker(nodesToAdd));

    if (this.g.showMoreDetails()) {
      this.detailButton.setText(g.getText("SEARCH_RESULT.SHOW_LESS"));
    } else {
      this.detailButton.setText(g.getText("SEARCH_RESULT.SHOW_MORE"));
    }

    int maxSearchResults = this.cfg.getProp(MAX_SEARCH_RESULTS);
    this.resultSet = g.getSearchResult().getFileEntries();
    this.resultSize = this.resultSet.size();

    if (this.resultSize > maxSearchResults) {
      this.resultSize = maxSearchResults;
    }

    String title = null;
    if (resultSize != 1) {
      title = g.getText("SEARCH_RESULT.RESULTS_FOUND_PLURAL", String.valueOf(resultSize));
    } else {
      title = g.getText("SEARCH_RESULT.RESULTS_FOUND_SINGULAR", String.valueOf(resultSize));
    }

    this.title.setText(title);
    this.cancelResultGeneration = false;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param fileEntry
   * @param nodesToAdd
   * @param score
   */
  private void addFileDetails(final FileEntry fileEntry, final List<Node> nodesToAdd,
      final Double score) {
    final File currentFile = new File(fileEntry.getPath());

    final Text filePrefix = new Text(g.getText("SEARCH_RESULT.FILENAME") + "\n");
    filePrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
    nodesToAdd.add(filePrefix);
    final Text file = new Text(currentFile.getName() + "\n");
    file.setTranslateX(TAB_WIDTH);
    nodesToAdd.add(file);

    final Text pathPrefix = new Text(g.getText("SEARCH_RESULT.DIRECTORY") + "\n");
    pathPrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
    nodesToAdd.add(pathPrefix);
    final String cleanedPath = St.shortenHomePathInDirectory(currentFile.getParent());
    final Text path = new Text(cleanedPath + "\n");
    path.setTranslateX(TAB_WIDTH);
    nodesToAdd.add(path);

    // *INDENT-OFF*
    // -- if someone needs the score --
    //
    // if (score != null) {
    //   final Text scorePrefix = new Text(g.getText("SEARCH_RESULT.SCORE") + "\n");
    //   scorePrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
    //   nodesToAdd.add(scorePrefix);
    //   final String cleanedScore = String.valueOf(St.formatDouble(score * 100d, 5));
    //   final Text scoreText = new Text(cleanedScore + "\n");
    //   scoreText.setTranslateX(TAB_WIDTH);
    //   nodesToAdd.add(scoreText);
    // }
    // *INDENT-ON*
  }

  /**
   *
   *
   * @param fileEntry
   * @param contentSearch
   * @param nodesToAdd
   * @param idToShow
   *
   * @return
   * @throws Exception
   */
  private Integer addFulltext(final FileEntry fileEntry, final String contentSearch,
      final List<Node> nodesToAdd, final Integer idToShow,
      final SortedMap<Integer, PartialEntry> positions) throws Exception {
    final String fullText = fileEntry.getFullTextString();
    Integer index = null;

    if (fullText != null && !fullText.isEmpty() && contentSearch != null) {
      Text snippetPrefix = new Text(g.getText("SEARCH_RESULT.SNIPPETS") + "\n");
      snippetPrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
      nodesToAdd.add(snippetPrefix);

      final Query query = TextProcessing.getQueryParser().parse(contentSearch);
      final String text = TextProcessing.postProcess(fullText);

      if (text != null && !text.isEmpty()) {
        int snippetsToShow = 1;

        if (idToShow != null) {
          snippetsToShow = 100;
        } else if (this.g.showMoreDetails()) {
          snippetsToShow = 5;
        }

        // *INDENT-OFF*
        Tp<String[], Integer[]> resultTupel =
          TextProcessing.prepareHighlight(
              text,
              "",
              query,
              idToShow == null ? maxSnippetLength : maxSnippetLength * 3,
              snippetsToShow,
              true);
        // *INDENT-ON*

        if (resultTupel == null) {
          // TODO: Text
          final Text errorText = new Text(g.getText("SEARCH_RESULT.CAN_NOT_GENERATE") + "\n\n");
          errorText.setFill(Color.RED);
          nodesToAdd.add(errorText);
          return null;
        }

        final String[] snippets = resultTupel.getKey();

        // position of first highlighted field
        // index = resultTupel.getValue();

        for (int i = 0; i < snippets.length; i++) {

          final String sn = snippets[i];
          index = resultTupel.getValue()[i];
          final StyledSnippet styledSnippet = TextProcessing.highlightString(text, sn, positions,
              index, fileEntry.getFile().getName());

          if (i > 0) {
            nodesToAdd.add(new Text("\n"));
          }

          // aa
          for (Tp<String, StyledSnippetType> snippet : styledSnippet.getSnippets()) {
            Text snippetText = new Text(snippet.getKey());
            snippetText.setTranslateX(TAB_WIDTH);

            // TODO: stylesheet please
            if (snippet.getValue() == StyledSnippetType.FULLTEXT) {
              snippetText.setFill(Color.BLACK);
            } else if (snippet.getValue() == StyledSnippetType.HIGHLIGHT) {
              snippetText.getStyleClass().add(HIGHLIGHTED_SNIPPET);
            } else if (snippet.getValue() == StyledSnippetType.IMAGE_TEXT) {
              // TODO: highlight image text not supported yet
            } else if (snippet.getValue() == StyledSnippetType.LINE_SEPERATOR) {
              snippetText.setFill(Color.BLUE);
            } else if (snippet.getValue() == StyledSnippetType.METADATA) {
              snippetText.setFill(Color.DARKMAGENTA);
            } else if (snippet.getValue() == StyledSnippetType.START_INDICATOR) {
              snippetText.setFill(Color.DARKGREEN);
            } else if (snippet.getValue() == StyledSnippetType.TRIMMED_INDICATOR) {
              snippetText.setFill(Color.DARKGREEN);
            }
            nodesToAdd.add(snippetText);
          }

          nodesToAdd.add(new Text("\n"));
        }
      }
    }
    // nodesToAdd.add(new Text(" \n"));
    return index;
  }

  /**
   *
   *
   * @param fileEntry
   * @param contentSearch
   * @param nodesToAdd
   * @param idToShow
   *
   * @throws Exception
   */
  private void addBrowser(final FileEntry fileEntry, final List<Node> nodesToAdd) throws Exception {
    // TODO: text
    final Hyperlink fulltextButton = new Hyperlink(g.getText("SEARCH_RESULT.SHOW_FULLTEXT"));
    fulltextButton.setStyle("-fx-font-size:15px");
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        addTooltip(fulltextButton, g.getText("SEARCH_RESULT.SHOW_FULLTEXT_TOOLTIP"), -50, 35);
      }
    });

    // ------------------------------------------------ //

    fulltextButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        outTransition();
        executeWorker(saveToXhtmlWorker(fileEntry));
      }
    });

    // ------------------------------------------------ //
    nodesToAdd.add(fulltextButton);
    nodesToAdd.add(new Text("  "));
  }

  /**
   *
   *
   * @param fileEntry
   * @return
   */
  private Task<Object> saveToXhtmlWorker(FileEntry fileEntry) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {

        final String fullText = fileEntry.getFullTextString();
        final File tempFile = TextProcessing.saveXhtmlToFile(fullText, g
            .getContentSearchLuceneQuery(), fileEntry.getFile());
        // ------------------------------------------------ //
        if (tempFile != null) {
          boolean validFile = true;

          try {
            if (!tempFile.exists()) {
              LOGGER.error("Fulltext XHTML File could not be generated.");
              validFile = false;
            } else if (!FileType.is(tempFile, FileType.CODE_XHTML)
                && !FileType.is(tempFile, FileType.CODE_XHTML2)) {
              LOGGER.error("Not a valid XHTML File");
              validFile = false;
            }
          } catch (Exception e) {
            // TODO: logging
            e.printStackTrace();
          }

          // *INDENT-OFF*
          if (validFile) {
            executeWorker(
              openFileWorker(
                tempFile.getPath(),
                false, // open directory
                1 // page
              )
            );
          }
          // *INDENT-ON*
        }
        // ------------------------------------------------ //
        inTransitionAsWorker(1000);
        return true;
      }
    };
  }

  /**
   *
   *
   * @param fileEntry
   * @param nodesToAdd
   *
   * @throws Exception
   */
  private void addFileButton(final FileEntry fileEntry, final List<Node> nodesToAdd,
      final Integer pageToOpen) throws Exception {
    Hyperlink openFileButton = new Hyperlink(g.getText("SEARCH_RESULT.OPEN_FILE"));
    openFileButton.setStyle("-fx-font-size:15px");

    openFileButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        outTransition();
        inTransitionAsWorker(1000);

        // *INDENT-OFF*
        executeWorker(
          openFileWorker(
            fileEntry.getPath(),
            false, // open directory
            pageToOpen == null ? 1 : pageToOpen // page
          )
        );
        // *INDENT-ON*
      }
    });
    nodesToAdd.add(openFileButton);
    nodesToAdd.add(new Text("  "));

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Change Clipboard button text");
        addTooltip(openFileButton, g.getText("SEARCH_RESULT.OPEN_FILE_TOOLTIP"), -50, 35);
      }
    });
  }

  /**
   *
   *
   * @param fileEntry
   * @param nodesToAdd
   *
   * @throws Exception
   */
  private void addDirectoryButton(final FileEntry fileEntry, final List<Node> nodesToAdd)
      throws Exception {
    Hyperlink openDirButton = new Hyperlink(g.getText("SEARCH_RESULT.SHOW_FILE"));
    openDirButton.setStyle("-fx-font-size:15px");
    // addImageIcon(openDirButton, Icon.FOLDER_OPEN, 0);
    openDirButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        outTransition();
        inTransitionAsWorker(1000);
        executeWorker(openFileWorker(fileEntry.getPath(), true, 1));
      }
    });
    nodesToAdd.add(openDirButton);
    nodesToAdd.add(new Text("  "));

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Open dir tooltip");
        addTooltip(openDirButton, g.getText("SEARCH_RESULT.OPEN_DIR_TOOLTIP"), -50, 35);
      }
    });
  }

  /**
   *
   *
   * @param idToShow
   * @param nodesToAdd
   *
   * @throws Exception
   */
  private void addDetailsButton(final Integer idToShow, final List<Node> nodesToAdd)
      throws Exception {
    final Hyperlink openDetails = new Hyperlink(g.getText("SEARCH_RESULT.SHOW_DETAILS"));
    openDetails.setStyle("-fx-font-size:15px");
    openDetails.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        outTransition();
        try {
          // *INDENT-OFF*
          executeWorker(
            addResultToDetailPopup(
              // results
              g.getSearchResult(),
              // fulltext-search-query for highlighting
              g.getContentSearchLuceneQuery(),
              idToShow
            )
          );
          // *INDENT-ON*

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    nodesToAdd.add(openDetails);
    nodesToAdd.add(new Text("  "));
  }

  /**
   *
   *
   * @param resultEntries
   * @param contentSearch
   * @param idToShow
   * @return
   */
  private QueueMonitor<List<Node>> getResultNodes(
      final mj.ocraptor.database.search.LuceneResult resultEntries, final String contentSearch,
      final Integer idToShow) {
    final QueueMonitor<List<Node>> partialReturnHandler = new QueueMonitor<List<Node>>(this.cfg
        .getProp(ConfigInteger.MAX_SEARCH_RESULTS));
    try {
      // ------------------------------------------------ //
      if (resultEntries == null)
        throw new NullPointerException(g.getText("SEARCH_RESULT.NO_RESULTS"));

      final Task<Object> progressTask = populationControllerWorker(resultEntries, contentSearch,
          partialReturnHandler, idToShow);
      executeWorker(progressTask);

      if (idToShow == null) {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            progressBar.progressProperty().bind(progressTask.progressProperty());
            searchResultPercentage.textProperty().bind(progressTask.messageProperty());
          }
        });
      }
      // ------------------------------------------------ //
    } catch (Exception e) {
      LOGGER.error("Can not render result", e);
    }
    return partialReturnHandler;
  }

  /**
   *
   *
   * @param resultEntries
   * @param contentSearch
   * @param returnEventHandler
   * @param idToShow
   * @return
   */
  private Task<Object> populationControllerWorker(
      final mj.ocraptor.database.search.LuceneResult resultEntries, final String contentSearch,
      final QueueMonitor<List<Node>> returnEventHandler, final Integer idToShow) {

    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        final Event<List<Node>> futureEvent = new Event<List<Node>>();
        final SortedSet<Map.Entry<FileEntry, Double>> entries = resultEntries.getFileEntries();

        int countEntry = 0;

        for (Map.Entry<FileEntry, Double> flEntry : entries) {
          try {
            if (idToShow != null && !flEntry.getKey().getId().equals(idToShow)) {
              continue;
            }

            if (cancelResultGeneration) {
              return false;
            }

            final Task<Object> populateEntryWorker = populateResultsWorker(flEntry, contentSearch,
                idToShow, futureEvent, flEntry.getValue());
            executeWorker(populateEntryWorker);

            if (idToShow == null) {
              updateProgress(countEntry, entries.size());
              // TODO: text
              updateMessage("File loaded:  " + countEntry + "/" + entries.size());
            }

            countEntry++;

            final List<Node> partialResult = futureEvent.get();
            returnEventHandler.put(partialResult, countEntry == entries.size());
          } catch (Exception e) {
            // TODO: logging
            e.printStackTrace();
          }
        }

        if (idToShow == null) {
          updateProgress(countEntry, entries.size());
          updateMessage("File loaded:  " + countEntry + "/" + entries.size());

          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              progressAnchor.setVisible(false);
            }
          });
        }

        return true;
      }
    };

  }

  /**
   *
   *
   * @param resultEntries
   * @param contentSearch
   * @param idToShow
   * @param nodesToAdd
   * @return
   */
  private Task<Object> populateResultsWorker(final Map.Entry<FileEntry, Double> flEntry,
      final String contentSearch, final Integer idToShow, final Event<List<Node>> futureEvent,
      final Double score) {

    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {

        final List<Node> nodesToAdd = new ArrayList<Node>();
        try {
          // nodesToAdd.add(new Text(StringUtils.repeat("Test\n", 10)));
          // Thread.sleep(1000);

          nodesToAdd.add(new Text(" \n"));

          int currentPseudoId = 0;

          // ------------------------------------------------ //

          // TODO:
          // if (currentPseudoId++ >= cfg.getProp(MAX_SEARCH_RESULTS))
          // break;

          // ------------------------------------------------ //

          final FileEntry fileEntry = flEntry.getKey();
          addFileDetails(fileEntry, nodesToAdd, score);
          // TODO: Metadata
          // addMetaData(fileEntry, metaDataSearch, nodesToAdd, idToShow);

          Integer pageToOpen = null;
          SortedMap<Integer, PartialEntry> positions = null;

          try {
            String original = fileEntry.getFullTextString();
            final String encodedXml = TextProcessing.encodePagePositions(original);
            if (cancelResultGeneration)
              return false;
            positions = TextProcessing.decodePagePositions(encodedXml);
          } catch (Exception e) {
            // TODO: logging
            e.printStackTrace();
          }

          // ------------------------------------------------ //

          final Integer index = addFulltext(fileEntry, contentSearch, nodesToAdd, idToShow,
              positions);

          if (index != null) {
            try {
              // System.out.println(positions.et(posi));
              if (cancelResultGeneration)
                return false;
              if (positions != null) {
                pageToOpen = TextProcessing.getPage(positions, index);
              }
            } catch (Exception e) {
              // TODO: logging
              e.printStackTrace();
            }
          }

          // ------------------------------------------------ //

          // control-buttons
          if (idToShow == null) {
            nodesToAdd.add(new Text(" "));
            addFileButton(fileEntry, nodesToAdd, pageToOpen);
            addSeparator(nodesToAdd);
            addDirectoryButton(fileEntry, nodesToAdd);
            addSeparator(nodesToAdd);
            addBrowser(fileEntry, nodesToAdd);
            addSeparator(nodesToAdd);
            addDetailsButton(fileEntry.getId(), nodesToAdd);
          }

          // ------------------------------------------------ //

          nodesToAdd.add(new Text(" \n\n"));
          if (currentPseudoId < resultSize) {
            nodesToAdd.add(new Text(" \n"));
          }
          // ------------------------------------------------ //

          nodesToAdd.remove(nodesToAdd.size() - 1);
          if (idToShow == null) {
            EventManager.instance().searchProgressIndicator(-2, g.getText("LOADING_SCREEN.DONE"));
          }

        } catch (Exception e) {
          // TODO: logging
          e.printStackTrace();
        } finally {
          futureEvent.put(nodesToAdd);
        }
        return true;
      }
    };
  }

  /**
   *
   *
   * @param nodes
   */
  private void addSeparator(final List<Node> nodes) {
    Text sep = new Text("| ");
    sep.setStyle("-fx-fill:grey");
    nodes.add(sep);
  }

  /**
   *
   *
   * @param returnEventHandler
   * @return
   */
  private Task<Object> updateTextFlowWorker(final QueueMonitor<List<Node>> returnEventHandler) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
          final List<Node> partialResult = returnEventHandler.get();
          Thread.sleep(100);
          if (partialResult != null) {
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                for (Node node : partialResult) {
                  resultText.getChildren().add(node);
                }
              }
            });
          } else {
            break;
          }
        }
        return true;
      }
    };

  }

  /**
   *
   *
   * @param resultEntries
   * @param metaDataSearch
   * @param contentSearch
   * @param idToShow
   * @return
   */
  private Task<Object> addResultToDetailPopup(
      final mj.ocraptor.database.search.LuceneResult resultEntries, final String contentSearch,
      final Integer idToShow) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        final List<Node> nodesToAdd = getResultNodes(resultEntries, contentSearch, idToShow).get();
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Detailed result popup");

            if (nodesToAdd != null) {
              g.showMessage(680, 430, 30, g.getText("SEARCH_RESULT.DETAILS"), null, true,
                  nodesToAdd.toArray(new Node[nodesToAdd.size()]));
              inTransition();
            }
          }
        });
        return true;
      }
    };
  }

  /**
  */
  @Override
  protected void initEventHandlers() {
    this.pane.getScene().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if ((event.isShiftDown() && (event.getCode() == KeyCode.J || event.getCode() == KeyCode.DOWN))) {
          resultScrollPane.setVvalue(resultScrollPane.getVvalue() + 0.05);
        } else if (event.isShiftDown()
            && (event.getCode() == KeyCode.K || event.getCode() == KeyCode.UP)) {
          resultScrollPane.setVvalue(resultScrollPane.getVvalue() - 0.05);
        } else if (event.getCode() == KeyCode.J || event.getCode() == KeyCode.DOWN) {
          resultScrollPane.setVvalue(resultScrollPane.getVvalue() + 0.005);
        } else if (event.getCode() == KeyCode.K || event.getCode() == KeyCode.UP) {
          resultScrollPane.setVvalue(resultScrollPane.getVvalue() - 0.005);
        }
      }
    });
  }
}
