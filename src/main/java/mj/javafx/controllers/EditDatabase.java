package mj.javafx.controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
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
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

import mj.configuration.Config;
import mj.configuration.properties.ConfigBool;
import mj.configuration.properties.ConfigString;
import mj.database.DBManager;
import mj.javafx.GUITemplate;
import mj.javafx.Icon;
import mj.tools.StringTools;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditDatabase extends GUITemplate {

  // @formatter:off
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 250;
  public static final   String FXML = "EditDatabase.fxml";
  // @formatter:on

  private DBManager db;

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private boolean showResetDBMessage = true;

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

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
  private Label emptyMessage2;

  @FXML
  private Button infoButton;

  @FXML
  private Button addFolderButton;

  @FXML
  private VBox emptyMessageBox;

  @FXML
  private ListView<String> folderList = new ListView<String>();

  @FXML
  private void deleteButtonClicked(ActionEvent event) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        File configFile = new File(Config.inst().getUserConfigFilePath());
        if (!configFile.getAbsoluteFile().equals(
            new File(cfg.getDefaultConfigFilePath()).getAbsoluteFile())) {
          final File database = new File(cfg.getProp(configFile.getAbsolutePath(),
              ConfigString.DATABASE_FOLDER));
          if (database.exists()) {
            FileUtils.deleteQuietly(database);
          }
          FileUtils.deleteQuietly(configFile);
          try {
            cfg.setUserConfigFilePath(cfg.getDefaultConfigFilePath());
          } catch (Exception ex) {
            // TODO: logging
            ex.printStackTrace();
          }
          gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
        }
      }
    };
    // TODO: text
    this.mainController.showConfirmationDialog("Delete this database?", handler);
  }

  @FXML
  private void updateButtonClicked(ActionEvent event) {

    EventHandler<ActionEvent> finalYesHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        try {
          db.reset();
          boolean incomplete = db.databaseIncomplete(true);
          if (!incomplete) {
            gotoPage(LoadingScreen.FXML, LoadingScreen.INIT_WIDTH, LoadingScreen.INIT_HEIGHT);
          }
        } catch (Exception ex) {
          // TODO: logging
          ex.printStackTrace();
        }
      }
    };

    EventHandler<ActionEvent> yesHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        mainController.showYesNoDialog("Are you sure?", finalYesHandler, null, false);
      }
    };

    EventHandler<ActionEvent> noHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent e) {
        try {
          boolean incomplete = db.databaseIncomplete(true);
          if (!incomplete) {
            gotoPage(LoadingScreen.FXML, LoadingScreen.INIT_WIDTH, LoadingScreen.INIT_HEIGHT);
          }
        } catch (Exception ex) {
          // TODO: logging
          ex.printStackTrace();
        }
      }
    };

    if (this.showResetDBMessage) {
      // TODO: text
      this.mainController.showYesNoDialog("Reset this database\nbefore indexing?", yesHandler,
          noHandler, true);
    } else {
      finalYesHandler.handle(null);
    }

  }

  @FXML
  void addFolderButtonClicked(ActionEvent event) {
    DirectoryChooser fileChooser = new DirectoryChooser();
    // TODO: text
    fileChooser.setTitle("Select directory to index.");
    File file = fileChooser.showDialog(this.mainController.getPrimaryStage());
    if (file != null) {
      Config.inst().addFolderToIndex(file);
      this.updateFolderList();
    }
  }

  @FXML
  private void searchButtonClicked(ActionEvent event) {
    this.gotoPage(SearchDialog.FXML, SearchDialog.INIT_WIDTH, SearchDialog.INIT_HEIGHT);
  }

  @FXML
  void infoButtonClicked(ActionEvent event) {
    this.mainController.showConfirmationDialog("This section is not finished yet. "
        + "It will contain detailed information about your indexed files.", 300, 150);
  }

  @FXML
  private void configButtonClicked(ActionEvent event) {
    this.gotoPage(SettingsManager.FXML, SettingsManager.INIT_WIDTH, SettingsManager.INIT_HEIGHT);
  }

  @FXML
  private void backButtonClicked(ActionEvent event) {
    try {
      this.cfg.setUserConfigFilePath(this.cfg.getDefaultConfigFilePath());
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
    // TODO:
    this.title.setText("Database: \""
        + StringTools.getFileNameWithoutExtension(Config.inst().getUserConfigFilePath()) + "\"");

    this.emptyMessage.setText("Drop here");
    this.emptyMessage.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent t) {
        addFolderButtonClicked(null);
      }
    });
    this.emptyMessage2.setText("No folders added yet.\nAdd the folders you want to index.");
    this.deleteButton.setText("Delete DB");
  }

  @Override
  public void initCustomComponents() {
    this.mainController.setLastContentSearch(null);
    this.mainController.setLastMetaDataSearch(null);
    this.mainController.setLastOperator(null);
    this.db = this.mainController.getMainController().initDatabase();
    this.updateFolderList();

    this.addTooltip(this.infoButton, "This section is not finished yet. "
        + "It will contain detailed information about your indexed files.", -50, 50);

    this.addTooltip(this.updateButton, "Start indexing your files (this process may take a while)",
        -50, 50);

    this.addTooltip(this.configButton, "Select the filetypes you want to index in this section",
        -50, -80);

    this.addTooltip(this.deleteButton, "Delete this database and all associated files.", -50, -70);

    // this.mainController.showConfirmationDialog("This section is not finished yet. "
    // + "It will contain detailed information about your indexed files.");

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
  }

  @Override
  protected void initListeners() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void asserts() {
    // TODO Auto-generated method stub
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

      // TODO:text
      addTooltip(button, "Remove this directory from your index", 50, 0);
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
                Config.inst().removeFolderToIndex(StringTools.resolveFilePath(lastItem));
                updateFolderList();
              }
            }
          };
          mainController.showConfirmationDialog("Remove this directory?", handler);
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
          File directory = new File(StringTools.resolveFilePath(item));
          if (!directory.exists()) {
            label.setTextFill(Color.RED);
            addTooltip(label, "This directory does not exist!", 50, 30);
          } else if (!directory.canRead()) {
            label.setTextFill(Color.ORANGE);
            addTooltip(label, "Can not read directory contents!", 50, 30);
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
      ArrayList<String> folders = Config.inst().getFoldersToIndex();
      String[] folderStrings = new String[folders.size()];
      for (int i = 0; i < folders.size(); i++) {
        folderStrings[i] = StringTools.shortenHomePathInDirectory(folders.get(i));
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
      boolean incompleteDB = this.db.databaseIncomplete(false);
      boolean searchAvailable = empty || date.isEmpty() || incompleteDB;

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
    // TODO Auto-generated method stub

  }
}
