package mj.ocraptor.javafx.controllers;

// {{{
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import mj.ocraptor.MainController;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.database.DBManager;
import mj.ocraptor.database.dao.ResultError;
import mj.ocraptor.database.error.TableEmptyException;
import mj.ocraptor.javafx.DoughnutChart;
import mj.ocraptor.javafx.GUITemplate;
import mj.ocraptor.javafx.Icon;
import mj.ocraptor.tools.DataStructureTools;
import mj.ocraptor.tools.St;
import mj.ocraptor.tools.SystemTools;

// }}}

public class EditDatabase extends GUITemplate {

  // {{{

  // *INDENT-OFF*
  public static double INIT_WIDTH  = 550;
  public static double INIT_HEIGHT = 235;
  public static final  String FXML = "EditDatabase.fxml";
  // *INDENT-ON*

  private DBManager db;

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private boolean showResetDBMessage = true;
  private static final int MIN_EXPECTED_RAM_IN_MB = 2048;
  private static final int MIN_RAM_TO_KEEP_FREE_IN_MB = 512;

  private static final Integer INFO_SCREEN_WIDTH = 550;
  private static final Integer INFO_SCREEN_HEIGTH = 600;
  // }}}

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  // {{{
  @FXML
  private Button deleteButton;

  @FXML
  private Button searchButton;

  @FXML
  private Button configButton;

  @FXML
  private Button backButton;

  @FXML
  private Button updateButton;

  @FXML
  private Label emptyMessage;

  @FXML
  private Button infoButton;

  @FXML
  private Button addFolderButton;

  @FXML
  private VBox emptyMessageBox;

  @FXML
  private ListView<String> folderList = new ListView<String>();

  // }}}

  @FXML
  private void deleteButtonClicked(ActionEvent event) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        File configFile = new File(Config.inst().getConfigUserFilePath());
        if (!configFile.getAbsoluteFile().equals(new File(cfg.getConfigMasterFilePath())
            .getAbsoluteFile())) {
          final File database = new File(cfg.getProp(configFile.getAbsolutePath(),
              ConfigString.DATABASE_FOLDER));
          if (database.exists()) {
            FileUtils.deleteQuietly(database);
          }
          FileUtils.deleteQuietly(configFile);
          try {
            cfg.setConfigUserFilePath(cfg.getConfigMasterFilePath());
          } catch (Exception ex) {
            // TODO: logging
            ex.printStackTrace();
          }
          gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
        }
      }
    };
    this.g.showConfirmationDialog(g.getText("EDIT_DB.DELETE_DB_CONFIRMATION"), handler);
  }

  @FXML
  private void updateButtonClicked(ActionEvent event) {
    // ------------------------------------------------ //

    EventHandler<ActionEvent> finalYesHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        try {
          db.reset();
          try {
            db.databaseIncomplete();
          } catch (Exception ex) {
            gotoPage(LoadingScreen.FXML, LoadingScreen.INIT_WIDTH, LoadingScreen.INIT_HEIGHT);
          }
        } catch (Exception ex) {
          // TODO: logging
          ex.printStackTrace();
        }
      }
    };

    // ------------------------------------------------ //

    EventHandler<ActionEvent> yesHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        g.showYesNoDialog(g.getText("EDIT_DB.SECOND_CONFIRMATION"), finalYesHandler, null, false);
      }
    };

    EventHandler<ActionEvent> noHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        try {
          try {
            db.databaseIncomplete();
            gotoPage(LoadingScreen.FXML, LoadingScreen.INIT_WIDTH, LoadingScreen.INIT_HEIGHT);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } catch (Exception ex) {
          // TODO: logging
          ex.printStackTrace();
        }
      }
    };

    // ------------------------------------------------ //

    EventHandler<ActionEvent> ramYesHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        try {
          if (showResetDBMessage) {
            g.showYesNoDialog(g.getText("EDIT_DB.RESET_DATABASE_BEFORE_INDEXING"), yesHandler,
                noHandler, true);
          } else {
            finalYesHandler.handle(null);
          }

        } catch (Exception ex) {
          // TODO: logging
          ex.printStackTrace();
        }
      }
    };

    // ------------------------------------------------ //

    try {
      final SystemTools sigar = new SystemTools();
      final long maxClientRam = sigar.getMaxRamInMB();
      final long freeRam = sigar.getFreeRamInMB();
      final Integer xmxValue = cfg.getProp(ConfigInteger.PROCESS_XMX);

      String ramError = null;
      if (maxClientRam < MIN_EXPECTED_RAM_IN_MB) {
        ramError = g.getText("EDIT_DB.MAX_RAM_TOO_LOW", MIN_EXPECTED_RAM_IN_MB, maxClientRam);
      } else if (freeRam < (xmxValue + MIN_RAM_TO_KEEP_FREE_IN_MB)) {
        ramError = g.getText("EDIT_DB.FREE_RAM_TOO_LOW", xmxValue + MIN_RAM_TO_KEEP_FREE_IN_MB,
            freeRam);
      }

      if (ramError != null) {
        this.g.showYesNoDialog(ramError, ramYesHandler, null, false, 500, 150);
        return;
      }

    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    if (this.showResetDBMessage) {
      this.g.showYesNoDialog(g.getText("EDIT_DB.RESET_DATABASE_BEFORE_INDEXING"), yesHandler,
          noHandler, true);
    } else {
      finalYesHandler.handle(null);
    }

  }

  @FXML
  void addFolderButtonClicked(ActionEvent event) {
    DirectoryChooser fileChooser = new DirectoryChooser();
    fileChooser.setTitle(g.getText("EDIT_DB.DIRECTORY_CHOOSER_TITLE"));
    File file = fileChooser.showDialog(this.g.getPrimaryStage());
    if (file != null) {
      Config.inst().addFolderToIndex(file);
      this.updateFolderList();
    }
  }

  @FXML
  private void searchButtonClicked(ActionEvent event) {
    this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
  }

  /**
   *
   *
   * @param event
   */
  @FXML
  void infoButtonClicked(ActionEvent event) {
    final DBManager db = MainController.inst().getDb();
    Integer entryCount = null;
    final List<Node> infoScreen = new ArrayList<Node>();
    Connection connection = null;
    try {
      connection = db.getConnection();
      entryCount = db.countEntries(connection);

      Text notFinished = new Text(g.getText("EDIT_DB.NOT_FINISHED_YET") + "\n\n");
      notFinished.setFill(Color.DARKRED);
      infoScreen.add(notFinished);

      Text countText = new Text(St.zeroPadSpaces(entryCount, 5) + " " + g.getText(
          "EDIT_DB.DATABASE_ENTRY_COUNT") + "\n");
      countText.setFont(Font.font(java.awt.Font.MONOSPACED, 13));
      infoScreen.add(countText);

      // {{{ Show file size
      Long fileSizeInMB = FileUtils.sizeOfDirectory(new File(this.cfg.getDatabasePath())) / 1024
          / 1024;
      if (fileSizeInMB == 0) {
        fileSizeInMB = 1L;
      }
      Text sizeText = new Text(St.zeroPadSpaces(DataStructureTools.safeLongToInt(fileSizeInMB), 5)
          + " " + g.getText("EDIT_DB.SIZE_OCCUPIED") + "\n");
      sizeText.setFont(Font.font(java.awt.Font.MONOSPACED, 13));
      infoScreen.add(sizeText);
      // }}}

      // {{{ show a list of saved error codes
      for (final ResultError possibleError : ResultError.values()) {
        Integer errorCount = db.countErrors(connection, possibleError);
        // TODO: errorcount must be > 0
        if (errorCount != null && errorCount > 0) {
          final Text errorCountText = new Text(St.zeroPadSpaces(errorCount, 5) + " "
              + getExplanation(possibleError) + " - ");
          errorCountText.setFont(Font.font(java.awt.Font.MONOSPACED, 13));
          infoScreen.add(errorCountText);
          final Hyperlink searchHyperLink = new Hyperlink(g.getText("EDIT_DB.INFO_SHOW_ME"));
          searchHyperLink.setFont(Font.font(java.awt.Font.MONOSPACED, 13));
          searchHyperLink.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
              g.setLastContentSearch(possibleError.getErrorCode());
              searchButtonClicked(null);
            }
          });

          infoScreen.add(searchHyperLink);
          infoScreen.add(new Text("\n"));
        }
      }
      // }}}

      // {{{ Show pie chart for indexed file types
      Map<String, Integer> extensions = db.countExtensions(connection);
      if (extensions.size() > 2 && entryCount > 10) {
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        final ObservableList<PieChart.Data> extensionsChartData = FXCollections
            .observableArrayList();
        final DoughnutChart extensionsChart = new DoughnutChart(extensionsChartData);
        for (final String extension : extensions.keySet()) {
          int extensionCount = extensions.get(extension);
          final String percentage = St.formatDouble((((double) extensionCount / (double) entryCount)
              * 100), 1);
          extensionsChartData.add(new PieChart.Data(extension + " (" + percentage + "%)",
              extensionCount));
        }
        extensionsChart.setPrefWidth(INFO_SCREEN_WIDTH);

        Collections.sort(extensionsChartData, new Comparator<PieChart.Data>() {
          @Override
          public int compare(Data o1, Data o2) {
            Number value1 = (Number) o1.getPieValue();
            Number value2 = (Number) o2.getPieValue();
            return new BigDecimal(value1.toString()).compareTo(new BigDecimal(value2.toString()));
          }
        });

        extensionsChart.setTitle(g.getText("EDIT_DB.EXTENSIONS"));
        extensionsChart.setLegendVisible(false);

        infoScreen.add(new Text("\n\n"));
        vbox.getChildren().add(extensionsChart);
        infoScreen.add(vbox);
      }
      // }}}

    } catch (Exception e) {
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e) {
        }
      }
    }

    this.g.showMessage(INFO_SCREEN_WIDTH, INFO_SCREEN_HEIGTH, 0, g.getText("EDIT_DB.DATABASE_INFO"),
        Color.BLACK, true, infoScreen.toArray(new Node[infoScreen.size()]));
  }

  /**
   *
   *
   * @param error
   * @return
   */
  private String getExplanation(final ResultError error) {
    if (error == ResultError.TIMEOUT) {
      return g.getText("EDIT_DB.INFO_TIMEOUT_ERROR");
    } else if (error == ResultError.PARSING) {
      return g.getText("EDIT_DB.INFO_UNKNOWN_ERROR");
    } else if (error == ResultError.OCR) {
      return g.getText("EDIT_DB.OCR_ERROR");
    }
    return g.getText("EDIT_DB.INFO_UNDEFINED_ERROR");
  }

  @FXML
  private void configButtonClicked(ActionEvent event) {
    this.gotoPage(SettingsManager.FXML, SettingsManager.INIT_WIDTH, SettingsManager.INIT_HEIGHT);
  }

  @FXML
  private void backButtonClicked(ActionEvent event) {
    try {
      this.cfg.setConfigUserFilePath(this.cfg.getConfigMasterFilePath());
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    this.gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initVisibility() {
    this.helpButton.setDisable(false);

    this.emptyMessageBox.setManaged(true);
    this.emptyMessageBox.setVisible(true);

    this.infoButton.setDisable(true);
    this.updateButton.setDisable(true);
    this.searchButton.setDisable(true);
  }

  @Override
  protected void initLabels() {
    final String dbPath = St.getFileNameWithoutExtension(Config.inst().getConfigUserFilePath());
    this.title.setText(g.getText("EDIT_DB.TITLE", dbPath));
    this.emptyMessage.setText(g.getText("EDIT_DB.DROP_HERE"));
    this.emptyMessage.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent t) {
        addFolderButtonClicked(null);
      }
    });

    // *INDENT-OFF*
    this.addFolderButton.setText( g.getText("EDIT_DB.ADD_FOLDER_BUTTON"));
    this.updateButton.setText   ( g.getText("EDIT_DB.INDEX_BUTTON"));
    this.infoButton.setText     ( g.getText("EDIT_DB.INFO_BUTTON"));
    this.searchButton.setText   ( g.getText("EDIT_DB.SEARCH_BUTTON"));
    this.backButton.setText     ( g.getText("BACK_BUTTON"));
    this.deleteButton.setText   ( g.getText("DELETE"));
    this.configButton.setText   ( g.getText("EDIT_DB.CONFIG"));
    this.helpButton.setText     ( g.getText("HELP"));
    // *INDENT-ON*
  }

  @Override
  public void initCustomComponents() {
    // this.g.setLastContentSearch(null);
    this.db = this.g.getParentController().initDatabase();
    this.updateFolderList();

    // *INDENT-OFF*
    this.addTooltip(this.infoButton,   g.getText("EDIT_DB.INDEXED_FILE_INFOS"),     -50,  50);
    this.addTooltip(this.updateButton, g.getText("EDIT_DB.START_INDEXING_TOOLTIP"), -50,  50);
    this.addTooltip(this.configButton, g.getText("EDIT_DB.CONFIG_TOOLTIP"),         -50, -80);
    this.addTooltip(this.deleteButton, g.getText("EDIT_DB.DELETE_BUTTON_TOOLTIP"),  -50, -70);
    this.addTooltip(this.searchButton, g.getText("EDIT_DB.SEARCH_BUTTON_TOOLTIP"),  -50,  50);
    // *INDENT-ON*

    // touch config file
    this.cfg.setProp(ConfigString.LAST_TIME_USED, String.valueOf(new Date().getTime()));

    this.pane.getScene().addEventHandler(KeyEvent.ANY, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.S) {
          if (!searchButton.isDisabled()) {
            searchButtonClicked(null);
          }
        }
        if (event.getCode() == KeyCode.ESCAPE) {
          backButtonClicked(null);
        }
      }
    });

    this.pane.getScene().setOnDragOver(new EventHandler<DragEvent>() {
      @Override
      public void handle(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
          event.acceptTransferModes(TransferMode.COPY);
        } else {
          event.consume();
        }
      }
    });

    this.pane.getScene().setOnDragDropped(new EventHandler<DragEvent>() {
      @Override
      public void handle(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
          success = true;

          List<File> acceptedFiles = new ArrayList<File>();
          for (File file : db.getFiles()) {
            if (file.exists() && file.isDirectory() && file.canRead()) {
              acceptedFiles.add(file);
            }
          }

          for (File file : acceptedFiles) {
            Config.inst().addFolderToIndex(file);
          }

          updateFolderList();
        }
        event.setDropCompleted(success);
        event.consume();
      }
    });

    this.executeWorker(propValidatingWorker());
  }

  @Override
  protected void initListeners() {
  }

  @Override
  protected void asserts() {
    // TODO: javafx asserts
  }

  /**
   *
   */
  private class RemoveDirectoryButton extends ListCell<String> {
    HBox hbox = new HBox();
    Label label = new Label("");
    Pane pane = new Pane();
    Button button = new Button("");
    String lastItem;

    public RemoveDirectoryButton() {
      super();
      Insets padding = new Insets(0, 0, 0, 10);
      addImageIcon(button, Icon.TRASH_WHITE, 1);

      addTooltip(button, g.getText("EDIT_DB.REMOVE_DIR_TOOLTIP"), 50, 0);
      button.getStyleClass().add("removeDirectoryButton");
      button.setMinWidth(40);
      button.setMaxWidth(40);
      button.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
              if (lastItem != null) {
                Config.inst().removeFolderToIndex(St.resolveFilePath(lastItem));
                updateFolderList();
              }
            }
          };
          g.showConfirmationDialog(g.getText("EDIT_DB.REMOVE_DIR_CONFIRMATION"), handler);
        }
      });

      label.setPadding(padding);
      hbox.getChildren().addAll(button, label);
      hbox.setAlignment(Pos.CENTER_LEFT);
      HBox.setHgrow(pane, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
      super.updateItem(item, empty);
      setText(null);
      if (empty) {
        lastItem = null;
        setGraphic(null);
      } else {
        lastItem = item;
        label.setText(item != null ? item : "<null>");
        if (item != null) {
          File directory = new File(St.resolveFilePath(item));
          if (!directory.exists()) {
            // TODO: style
            label.setTextFill(Color.RED);
            addTooltip(label, g.getText("EDIT_DB.DIR_DOES_NOT_EXIST"), 50, 30);
          } else if (!directory.canRead()) {
            label.setTextFill(Color.ORANGE);
            addTooltip(label, g.getText("EDIT_DB.DIR_NOT_READABLE"), 50, 30);
          } else {
            label.setTextFill(Color.BLACK);
          }
        }
        setGraphic(hbox);
      }
    }
  }

  @Override
  protected double getWindowWidth() {
    return INIT_WIDTH;
  }

  @Override
  protected double getWindowHeight() {
    return INIT_HEIGHT;
  }

  /**
   *
   *
   */
  private void updateFolderList() {
    try {
      // try {
      // this.db.printTables(true);
      // } catch (Exception e) {
      // }
      ArrayList<String> folders = Config.inst().getFoldersToIndex();
      String[] folderStrings = new String[folders.size()];

      boolean allDirectoriesExist = true;
      for (int i = 0; i < folders.size(); i++) {
        final String folderPath = folders.get(i);
        final File folderFile = new File(folderPath);
        folderStrings[i] = St.shortenHomePathInDirectory(folderPath);
        if (!folderFile.exists() || !folderFile.canRead()) {
          allDirectoriesExist = false;
        }
      }

      ObservableList<String> layerList = FXCollections.observableArrayList(folderStrings);
      folderList.setItems(layerList);

      folderList.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
        @Override
        public ListCell<String> call(ListView<String> param) {
          return new RemoveDirectoryButton();
        }
      });

      boolean empty = layerList.isEmpty();
      this.emptyMessageBox.setManaged(empty);
      this.emptyMessageBox.setVisible(empty);
      this.updateButton.setDisable(empty);

      String date = this.cfg.getProp(ConfigString.LAST_TIME_MODIFIED);
      boolean incompleteDB = false;
      try {
        this.db.databaseIncomplete();
      } catch (Exception e) {
        incompleteDB = true;
      }
      boolean searchAvailable = empty || date.isEmpty() || incompleteDB || !allDirectoriesExist;

      this.searchButton.setDisable(searchAvailable);
      this.infoButton.setDisable(searchAvailable);
      this.showResetDBMessage = !searchAvailable;

    } catch (Exception e) {
      LOG.error("...", e);
      backButtonClicked(null);
    }
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initEventHandlers() {
  }
}
