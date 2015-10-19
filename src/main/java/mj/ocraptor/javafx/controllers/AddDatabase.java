package mj.ocraptor.javafx.controllers;

import java.io.File;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.javafx.GUITemplate;
import mj.ocraptor.tools.St;

public class AddDatabase extends GUITemplate {
  // *INDENT-OFF*
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 235;
  public static final   String FXML = "AddDatabase.fxml";

  private static final String
    DEFAULT_LABEL_CLASS = "defaultMessage",
    SAVE_LABEL_CLASS    = "savedMessage",
    ERROR_LABEL_CLASS   = "errorMessage";
  // *INDENT-ON*

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @FXML
  private Label errorField;

  @FXML
  private Button cancelButton;

  @FXML
  private TextField databaseName;

  @FXML
  private TextField folderField;

  @FXML
  private Button saveButton;

  @FXML
  private Button selectFolderButton;

  @FXML
  private Button reloadButton;

  @FXML
  private void selectFolderButtonClicked(ActionEvent event) {
    // http://docs.oracle.com/javafx/2/ui_controls/file-chooser.htm
    final DirectoryChooser fileChooser = new DirectoryChooser();
    fileChooser.setTitle(g.getText("ADD_DB.DIRCHOOSER_TITLE"));
    final File file = fileChooser.showDialog(this.g.getPrimaryStage());
    if (file != null) {
      folderField.setText(file.getAbsolutePath());
    }
  }

  @FXML
  private void cancelButtonClicked(ActionEvent event) {
    this.gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
  }

  @FXML
  private void reloadButtonClicked(ActionEvent event) {
    this.folderField.setText(this.defaultPath);
    this.databaseName.setText("");
  }

  @FXML
  private void saveButtonClicked(ActionEvent event) {
    this.executeWorker(saveWorker());
    this.errorField.setVisible(true);
    this.errorField.setText(g.getText("ADD_DB.CREATED"));
    final ObservableList<String> styleClasses = this.errorField.getStyleClass();
    styleClasses.clear();
    styleClasses.add(SAVE_LABEL_CLASS);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initVisibility() {
    this.errorField.setVisible(true);
    this.helpButton.setDisable(false);
  }

  @Override
  protected void initLabels() {
    // *INDENT-OFF*
    this.title.setText              (g.getText("ADD_DB.TITLE"));
    this.folderField.setPromptText  (g.getText("ADD_DB.SELECT_FOLDER"));
    this.databaseName.setPromptText (g.getText("ADD_DB.NAME_IT"));
    this.selectFolderButton.setText (g.getText("SELECT"));
    this.cancelButton.setText       (g.getText("CANCEL"));
    this.saveButton.setText         (g.getText("SAVE"));
    this.helpButton.setText         (g.getText("HELP"));
    this.reloadButton.setText       (g.getText("ADD_DB.RESET"));
    // *INDENT-ON*
  }

  private String defaultPath;

  @Override
  public void initCustomComponents() {
    final File homeDir = this.cfg.getHomeConfigDir();
    boolean useUserDir = this.cfg.useUserFolderConfiguration();
    int count = 1;

    final String dbString = g.getText("ADD_DB.DB");
    final String portableDbString = g.getText("ADD_DB.PORTABLE_DB");

    File database = new File((useUserDir ? homeDir + File.separator + dbString : portableDbString)
        + count);
    while (database.exists()) {
      database = new File((useUserDir ? homeDir + File.separator + dbString : portableDbString)
          + ++count);
    }
    this.defaultPath = database.getPath();
    this.folderField.setText(database.getPath());
    // String message =
    // "Select an empty directory and a suitable name, then click 'Save'";
    // this.errorField.setText(message);
    this.addTooltip(this.selectFolderButton, g.getText("ADD_DB.SELECT_TOOLTIP"), -205, 0);
    this.addTooltip(this.folderField, g.getText("ADD_DB.FOLDER_FIELD_TOOLTIP"), 0, -28);
    this.addTooltip(this.databaseName, g.getText("ADD_DB.NAME_FIELD_TOOLTIP"), 0, 32);
  }

  @Override
  protected void asserts() {
    // TODO javafx asserts
  }

  @Override
  protected void initListeners() {
    // {{{
    databaseName.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
        validityCheck();
      }
    });

    folderField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
        validityCheck();
      }
    });
    // }}}
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
  private void validityCheck() {
    // {{{
    String name = databaseName.getText().trim();
    String folder = folderField.getText().trim();

    if (name.length() > Config.MAX_FILE_LENGTH) {
      name = name.substring(0, Config.MAX_FILE_LENGTH);
      this.databaseName.setText(name);
    }

    boolean validName = false;
    boolean validFolder = false;

    String message = null;

    // ------------------------------------------------ //
    // -- validating name
    // ------------------------------------------------ //
    if (!name.trim().isEmpty()) {
      if (!St.isValidFileName(name, Config.MAX_FILE_LENGTH)) {
        message = g.getText("ADD_DB.NAME_NOT_VALID");
        validName = false;
      } else {
        validName = true;
        List<File> configFiles = this.cfg.getUserConfigurationFiles();
        for (File config : configFiles) {
          if (config.getName().toLowerCase().equals(
              (name + Config.PROPERTIES_EXTENSION).toLowerCase())) {
            validName = false;
            message = g.getText("ADD_DB.DATABASE_EXISTS");
          }
        }
      }
    }

    // ------------------------------------------------ //
    // -- validating folder
    // ------------------------------------------------ //

    if (!folder.trim().isEmpty()) {
      if (folder.trim().equals(this.defaultPath)) {
        validFolder = true;
      } else {
        File dir = new File(folder);
        if ((!dir.isDirectory() || !dir.exists())) {
          message = g.getText("ADD_DB.FOLDER_NOT_VALID");
        } else if (!dir.canWrite() || !dir.canRead()) {
          message = g.getText("ADD_DB.CANNOT_WRITE");
        } else if (new File(dir, "idx").exists()) {
          message = "This folder is already in use.";
        } else {
          validFolder = true;
        }
      }
    }

    if (validName && validFolder) {
      saveButton.setDisable(false);
    } else {
      saveButton.setDisable(true);
    }

    final ObservableList<String> styleClasses = this.errorField.getStyleClass();
    if (message != null) {
      styleClasses.clear();
      styleClasses.add(ERROR_LABEL_CLASS);
    } else {
      styleClasses.clear();
      styleClasses.add(DEFAULT_LABEL_CLASS);
      if (validName && validFolder) {
        message = g.getText("ADD_DB.CLICK_SAVE");
      } else {
        message = g.getText("ADD_DB.DESCRIPTION");
      }
    }
    this.errorField.setText(message);

    // }}}
  }

  /**
   *
   *
   * @return
   */
  public Task<Object> saveWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        String name = databaseName.getText().trim();
        String folder = folderField.getText().trim();

        if (folder.equals(defaultPath)) {
          new File(defaultPath).mkdirs();
        }

        File newConfigFile = cfg.createNewUserConfiguration(name);
        Config.inst().saveDatabaseFolder(newConfigFile.getAbsolutePath(), folder);

        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        // JavaFX-Thread:
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Cancel button clicked");
            cancelButtonClicked(null);
          }
        });
        return true;
      }
    };
  }

  @Override
  protected void initEventHandlers() {
    this.pane.getScene().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
          if (!saveButton.isDisabled()) {
            saveButtonClicked(null);
          }
        }
      }
    });
  }
}
