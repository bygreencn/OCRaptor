package mj.javafx.controllers;

import static mj.configuration.properties.ConfigBool.INCLUDE_METADATA;
import static mj.configuration.properties.ConfigInteger.DIALOG_METADATA_MAX_STRING_LENGTH;
import static mj.configuration.properties.ConfigInteger.DIALOG_SNIPPET_MAX_STRING_LENGTH;
import static mj.configuration.properties.ConfigInteger.MAX_SEARCH_RESULTS;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import mj.configuration.Config;
import mj.console.ConsoleOutputFormatter;
import mj.console.ExtendedAscii;
import mj.events.EventManager;
import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.FileEntryDao;
import mj.extraction.result.document.FullText;
import mj.extraction.result.document.FullTextDao;
import mj.extraction.result.document.MetaData;
import mj.javafx.GUITemplate;
import mj.tools.StringTools;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SearchResult extends GUITemplate {
  // @formatter:off
  public static double INIT_WIDTH  = 700;
  public static double INIT_HEIGHT = 500;

  public  static final String FXML                  = "SearchResult.fxml";
  private static final String SNIPPET_TITLE_CLASS   = "snippetTitle";
  private static final String SNIPPET_TYPE_CLASS    = "snippetType";
  private static final String HIGHLIGHTED_SNIPPET   = "highlightedSnippet";
  private static final String METADATA_START        = "[";
  private static final String METADATA_END          = "]";
  private static final String METADATA_DIVIDER      = " | ";

  public static final Integer TAB_WIDTH = 10;
  private int resultSize, maxMetaDataLength, maxSnippetLength;
  private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(getClass());
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @FXML
  private Button backButton;

  @FXML
  private TextFlow resultText;

  @FXML
  private Button detailButton;

  @FXML
  private ScrollPane resultScrollPane;

  @Override
  protected void initLabels() {
    //
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
  }

  @FXML
  void backButtonClicked(ActionEvent event) {
    this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
  }

  @FXML
  void detailButtonClicked(ActionEvent event) {
    if (this.mainController.showMoreDetails()) {
      this.mainController.setShowMoreDetails(false);
    } else {
      this.mainController.setShowMoreDetails(true);
    }
    this.gotoPage(SearchResult.FXML, SearchResult.INIT_WIDTH, SearchResult.INIT_HEIGHT);
  }

  private static final String MORE_DETAILS = "Show more snippets";
  private static final String LESS_DETAILS = "Show less snippets";

  private Analyzer analyzer;
  private QueryParser qp;

  @Override
  public void initCustomComponents() {
    this.maxSnippetLength = this.cfg.getProp(DIALOG_SNIPPET_MAX_STRING_LENGTH)
        + Config.SEARCH_DELIMITER_START.toString().length()
        + Config.SEARCH_DELIMITER_END.toString().length();
    this.maxMetaDataLength = this.cfg.getProp(DIALOG_METADATA_MAX_STRING_LENGTH);

    // @formatter:off
    this.executeWorker(
        this.addResultToTextFlow(
          // ------------------------------------------------ //
          this.mainController.getSearchResult(), // results
          // meta-search-query for highlighting
          this.mainController.getMetaDataSearchLuceneQuery(),
          // fulltext-search-query for highlighting
          this.mainController.getContentSearchLuceneQuery(),
          null // id of file-entry to highlight (null == show all entries)
          // ------------------------------------------------ //
        )
    );
    // @formatter:on

    if (this.mainController.showMoreDetails()) {
      this.detailButton.setText(LESS_DETAILS);
    } else {
      this.detailButton.setText(MORE_DETAILS);
    }

    int maxSearchResults = this.cfg.getProp(MAX_SEARCH_RESULTS);
    this.resultSize = mainController.getSearchResult().getFileEntries().size();
    if (this.resultSize > maxSearchResults) {
      this.resultSize = maxSearchResults;
    }

    this.title.setText(resultSize + " " + (resultSize != 1 ? "entries" : "entry") + " found");

    this.analyzer = new StandardAnalyzer(Version.LUCENE_30);
    this.qp = new QueryParser(Version.LUCENE_30, "", analyzer);
    // TODO: Performance problems if enabled
    qp.setAllowLeadingWildcard(true);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param fileEntry
   * @param nodesToAdd
   */
  private void addFileDetails(FileEntry fileEntry, List<Node> nodesToAdd) {
    final File currentFile = new File(fileEntry.getPath());

    final Text filePrefix = new Text("FILENAME:\n");
    filePrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
    nodesToAdd.add(filePrefix);
    final Text file = new Text(currentFile.getName() + "\n");
    file.setTranslateX(TAB_WIDTH);
    nodesToAdd.add(file);

    final Text pathPrefix = new Text("DIRECTORY:\n");
    pathPrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
    nodesToAdd.add(pathPrefix);
    final String cleanedPath = StringTools.shortenHomePathInDirectory(currentFile.getParent());
    final Text path = new Text(cleanedPath + "\n");
    path.setTranslateX(TAB_WIDTH);
    nodesToAdd.add(path);
  }

  /**
   *
   *
   * @param fileEntry
   * @param metaDataSearch
   * @param nodesToAdd
   *
   * @throws Exception
   */
  private void addMetaData(FileEntry fileEntry, String metaDataSearch, List<Node> nodesToAdd,
      final Integer idToShow) throws Exception {
    if (metaDataSearch == null) {
      return;
    }

    final List<MetaData> metadataList = fileEntry.getMetadata();
    if (metadataList != null && !metadataList.isEmpty()) {
      final Text metadataPrefix = new Text("METADATA:\n");
      metadataPrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
      nodesToAdd.add(metadataPrefix);

      for (MetaData md : metadataList) {
        String mdContent = METADATA_START + md.getKey() + METADATA_DIVIDER + md.getValue()
            + METADATA_END;

        Query mdQuery = qp.parse(metaDataSearch);

        String output = prepareHighlight(mdContent, "", mdQuery,
            idToShow == null ? this.maxMetaDataLength : this.maxMetaDataLength * 3, 1, false);

        List<Text> highlights = highlightString(output);
        for (Text hg : highlights) {
          hg.setTranslateX(TAB_WIDTH);
          nodesToAdd.add(hg);
        }
        nodesToAdd.add(new Text("\n"));
      }
    }

    if (idToShow != null) {
      FullTextDao dao = new FullTextDao();
      FullText example = new FullText();
      example.setFileId(fileEntry.getId());
      Connection connection = null;
      try {
        connection = mainController.getMainController().getDb().getConnection();
        List<FullText> textObjects = dao.findByExample(example, 1, connection);
        if (!textObjects.isEmpty()) {
          FullText text = textObjects.get(0);
          Document doc = Jsoup.parse(text.getText());
          String clText = doc.text();

          int metadataStart = clText.indexOf("Meta=(");

          if (metadataStart > 10) {
            nodesToAdd.add(new Text("\n"));
            final Text fullTextPrefix = new Text("FULLTEXT-SNIPPET:\n");
            fullTextPrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
            nodesToAdd.add(fullTextPrefix);

            clText = clText.substring(0, metadataStart);
            int snippetLength = this.maxSnippetLength * 10;
            String cleanedText = StringTools.trimToLengthIndicatorRight(clText, snippetLength);

            Text sn = new Text(cleanedText);
            sn.setTranslateX(TAB_WIDTH);
            nodesToAdd.add(sn);
            nodesToAdd.add(new Text("\n"));
          }

        }
      } catch (Exception e) {
        e.printStackTrace();
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
  private void addFulltext(final FileEntry fileEntry, final String contentSearch,
      final List<Node> nodesToAdd, final Integer idToShow) throws Exception {
    final List<FullText> fullTextObjects = fileEntry.getFullTextObjects();

    if (fullTextObjects != null && !fullTextObjects.isEmpty() && contentSearch != null
        && qp != null) {
      Text snippetPrefix = new Text("SNIPPETS:\n");
      snippetPrefix.getStyleClass().add(SNIPPET_TITLE_CLASS);
      nodesToAdd.add(snippetPrefix);
      Query query = qp.parse(contentSearch);

      for (FullText fullText : fileEntry.getFullTextObjects()) {
        Document doc = Jsoup.parse(fullText.getText());
        String text = doc.text();

        if (text != null && !text.isEmpty()) {
          int snippetsToShow = 1;

          if (idToShow != null) {
            snippetsToShow = 100;
          } else if (this.mainController.showMoreDetails()) {
            snippetsToShow = 5;
          }

          String output = prepareHighlight(text, "", query, idToShow == null ? maxSnippetLength
              : maxSnippetLength * 3, snippetsToShow, true);

          List<Text> highlights = highlightString(output);
          for (Text hg : highlights) {
            hg.setTranslateX(TAB_WIDTH);
            nodesToAdd.add(hg);
          }
          nodesToAdd.add(new Text("\n"));
        }
      }
    }
    nodesToAdd.add(new Text(" \n"));
  }

  /**
   *
   *
   * @param fileEntry
   * @param nodesToAdd
   *
   * @throws Exception
   */
  private void addFileButton(final FileEntry fileEntry, final List<Node> nodesToAdd)
      throws Exception {
    Button openFileButton = new Button("Open file");

    // addImageIcon(openFileButton, Icon.FILE, 0);
    openFileButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        outTransition();
        inTransitionAsWorker(1000);
        // pagination
        Integer firstHitFullText = null;
        executeWorker(openFileWorker(fileEntry.getPath(), false, firstHitFullText == null ? 1
            : firstHitFullText));
      }
    });
    nodesToAdd.add(openFileButton);
    nodesToAdd.add(new Text("  "));

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        addTooltip(openFileButton, "Open highlighted file with the standard external viewer.", -50, 35);
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
    Button openDirButton = new Button("Directory");
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
        addTooltip(openDirButton, "Show highlighted file in the default file explorer.", -50, 35);
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
  private void addMetaDataButton(final FileEntry fileEntry, final List<Node> nodesToAdd)
      throws Exception {
    Button openMetadata = new Button("Metadata");
    // addImageIcon(openMetadata, Icon.TEXT, 0);
    openMetadata.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        Connection connection = null;
        FileEntryDao fileEntryDao = new FileEntryDao();
        try {
          connection = mainController.getMainController().getDb().getConnection();
          FileEntry updatedFileEntry = fileEntryDao.findById(fileEntry.getId(), connection);
          fileEntryDao.pullMetaData(updatedFileEntry, connection, 100);
          List<MetaData> md = updatedFileEntry.getMetadata();
          List<Node> messageMetaDataList = new ArrayList<Node>();
          for (MetaData m : md) {
            TextField key = new TextField(m.getKey());
            TextField value = new TextField(m.getValue());

            key.setEditable(false);
            value.setEditable(false);

            key.setMinWidth(260);
            key.setTranslateX(10);
            key.setTranslateY(10);

            value.setMinWidth(260);
            value.setTranslateX(10);
            value.setTranslateY(10);

            key.getStyleClass().add("metadataKey");
            value.getStyleClass().add("metadataValue");

            messageMetaDataList.add(key);
            Text separator = new Text(" :: ");
            separator.setFill(Color.TRANSPARENT);
            messageMetaDataList.add(separator);

            messageMetaDataList.add(value);
            messageMetaDataList.add(new Text(" \n\n"));
          }
          mainController.showMessage(600, 300, 5, "Metadata", null, true, messageMetaDataList
              .toArray(new Node[messageMetaDataList.size()]));
        } catch (Exception e) {
          e.printStackTrace();
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
    });
    if (cfg.getProp(INCLUDE_METADATA)) {
      nodesToAdd.add(openMetadata);
      nodesToAdd.add(new Text("  "));
    }
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

    Button openMetadata = new Button("Show details");
    // addImageIcon(openMetadata, Icon.TEXT, 0);
    openMetadata.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        outTransition();
        try {
          // @formatter:off
            executeWorker(
              addResultToDetailPopup(
                mainController.getSearchResult(), // results
                // meta-search-query for highlighting
                mainController.getMetaDataSearchLuceneQuery(),
                // fulltext-search-query for highlighting
                mainController.getContentSearchLuceneQuery(),
                idToShow
              )
            );
          // @formatter:on
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    nodesToAdd.add(openMetadata);
    nodesToAdd.add(new Text("  "));
  }

  /**
   *
   *
   * @param currentPseudoId
   */
  private void renderingAnimation(int currentPseudoId) {
    final double renderingProgress = ((double) currentPseudoId) / ((double) this.resultSize);
    EventManager.instance().searchProgressIndicator(renderingProgress,
        "Rendering: " + String.valueOf(currentPseudoId) + "/" + this.resultSize);
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
  private List<Node> getResultNodes(final mj.database.SearchResult resultEntries,
      final String metaDataSearch, final String contentSearch, final Integer idToShow) {
    try {
      if (resultEntries == null)
        throw new NullPointerException("No results are available...");

      final List<Node> nodesToAdd = new ArrayList<Node>();
      nodesToAdd.add(new Text(" \n"));

      // ------------------------------------------------ //
      // ------------------------------------------------ //

      int currentPseudoId = 0;
      for (Map.Entry<FileEntry, Double> flEntry : resultEntries.getFileEntries()) {
        // ------------------------------------------------ //

        if (currentPseudoId++ >= cfg.getProp(MAX_SEARCH_RESULTS))
          break;

        // ------------------------------------------------ //

        final FileEntry fileEntry = flEntry.getKey();

        renderingAnimation(currentPseudoId);
        if (idToShow != null && !fileEntry.getId().equals(idToShow))
          continue;

        addFileDetails(fileEntry, nodesToAdd);
        addMetaData(fileEntry, metaDataSearch, nodesToAdd, idToShow);
        addFulltext(fileEntry, contentSearch, nodesToAdd, idToShow);

        // control-buttons
        if (idToShow == null) {
          addDetailsButton(fileEntry.getId(), nodesToAdd);
          addFileButton(fileEntry, nodesToAdd);
          addDirectoryButton(fileEntry, nodesToAdd);
          addMetaDataButton(fileEntry, nodesToAdd);
        }

        // ------------------------------------------------ //

        nodesToAdd.add(new Text(" \n\n"));
        if (currentPseudoId < resultSize) {
          // nodesToAdd.add(getResultDivider());
          nodesToAdd.add(new Text(" \n"));
        }

        // ------------------------------------------------ //
      }

      if (!nodesToAdd.isEmpty()) {
        nodesToAdd.remove(nodesToAdd.size() - 1);
      }

      EventManager.instance().searchProgressIndicator(-2, "Done");
      return nodesToAdd;
    } catch (Exception e) {
      LOG.error("Can not render result", e);
    }
    return null;
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
  private Task<Object> addResultToDetailPopup(final mj.database.SearchResult resultEntries,
      final String metaDataSearch, final String contentSearch, final Integer idToShow) {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        List<Node> nodesToAdd = getResultNodes(resultEntries, metaDataSearch, contentSearch,
            idToShow);
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (nodesToAdd != null) {
              mainController.showMessage(680, 430, 30, "Details", null, true, nodesToAdd
                  .toArray(new Node[nodesToAdd.size()]));
              inTransition();
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
   * @param result
   * @return
   */
  private Task<Object> addResultToTextFlow(final mj.database.SearchResult resultEntries,
      final String metaDataSearch, final String contentSearch, final Integer idToShow) {

    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        List<Node> nodesToAdd = getResultNodes(resultEntries, metaDataSearch, contentSearch,
            idToShow);
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            resultText.getChildren().clear();
            for (Node node : nodesToAdd) {
              resultText.getChildren().add(node);
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
   * @param text
   * @return
   */
  private List<Text> highlightString(String text) {
    List<Text> textFragments = new ArrayList<Text>();

    if (text != null) {
      HashMap<Integer, String> tags = ConsoleOutputFormatter.getTagValues(text);

      // sort tags by their position
      List<Integer> keys = new ArrayList<Integer>(tags.keySet());
      class ScoreSorter implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
          return a.compareTo(b);
        }
      }
      Collections.sort(keys, new ScoreSorter());

      int index = 0, lastStop = 0;

      for (Integer ind : keys) {
        String highlightedString = tags.get(ind);

        try {
          // ------------------------------------------------ //
          Text fillerText = new Text(StringTools.removeSearchDelimiter(text
              .substring(lastStop, ind)));
          textFragments.add(fillerText);
          lastStop = ind + highlightedString.length();

          // ------------------------------------------------ //
          // snippet to highlight:
          Text fragment = null;
          fragment = new Text(StringTools.removeSearchDelimiter(highlightedString));
          fragment.getStyleClass().add(HIGHLIGHTED_SNIPPET);
          textFragments.add(fragment);

          // ------------------------------------------------ //
          if (++index == keys.size()) {
            textFragments.add(new Text(StringTools.removeSearchDelimiter(text.substring(lastStop,
                text.length()))));
          }
        } catch (Exception e) {
          System.out.println(ExceptionUtils.getStackTrace(e));
        }
      }
    }
    return textFragments;
  }

  /**
   *
   *
   * @param text
   * @param startString
   * @param query
   * @param analyzer
   * @param maxSnippetLength
   * @param maxWidthForOneLine
   * @return
   */
  private String prepareHighlight(String text, String startString, Query query,
      int maxSnippetLength, int maxHighlights, boolean fulltext) {

    StringBuffer output = new StringBuffer();
    try {
      String modifiedString = ConsoleOutputFormatter.getHighlightedField(query, analyzer, "", text);
      HashMap<Integer, String> tags = ConsoleOutputFormatter.getTagValues(modifiedString);

      // sort tags by their position
      List<Integer> keys = new ArrayList<Integer>(tags.keySet());
      class ScoreSorter implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
          return a.compareTo(b);
        }
      }
      Collections.sort(keys, new ScoreSorter());

      int lastPrintedIndex = 0;
      int i = 0;

      for (Integer ind : keys) {

        String firstHighlightString = tags.get(ind);

        if (ind + firstHighlightString.length() <= lastPrintedIndex)
          continue;

        if (i >= maxHighlights && maxHighlights <= 5) {
          break;
        }

        String snippetIndicator = "[...]";
        String[] temp = StringTools.findSn(modifiedString, firstHighlightString, snippetIndicator,
            ind, maxSnippetLength);

        String snippet = (fulltext ? ExtendedAscii.getAsciiAsString(174) + " " : "")
            + StringTools.normalizeDocumentText(temp[0] + firstHighlightString + temp[1]);

        // TODO: stephen king
        if (i >= maxHighlights && !snippet.contains("Meta=(")) {
          continue;
        }

        // ------------------------------------------------ //
        if (i++ > 0) {
          output.append("\n\n");
        }
        output.append(snippet);
        lastPrintedIndex = ind + firstHighlightString.length() + temp[1].length();

      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    return output.toString();
  }

  /**
   *
   *
   * @return
   */
  private Line getResultDivider() {
    return this.getDivider("searchResultDivider", 4.0f, 36, 2d, 21d);
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
