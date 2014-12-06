package mj.javafx.controllers;

import java.io.File;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;

import mj.configuration.Config;
import mj.javafx.GUITemplate;
import mj.tools.StringTools;

public class AddDatabase extends GUITemplate {
  // @formatter:off
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 200;
  public static final   String FXML = "AddDatabase.fxml";
  // @formatter:on

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
  private void selectFolderButtonClicked(ActionEvent event) {
    // http://docs.oracle.com/javafx/2/ui_controls/file-chooser.htm
    DirectoryChooser fileChooser = new DirectoryChooser();
    fileChooser.setTitle("Open Resource Directory");
    File file = fileChooser.showDialog(this.mainController.getPrimaryStage());
    if (file != null) {
      folderField.setText(file.getAbsolutePath());
    }
  }

  @FXML
  private void cancelButtonClicked(ActionEvent event) {
    this.gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
  }

  @FXML
  private void saveButtonClicked(ActionEvent event) {
    this.executeWorker(saveWorker());
    this.errorField.setVisible(true);
    this.errorField.setText("Database created.");
    this.errorField.setTextFill(Paint.valueOf("GREEN"));
    this.errorField.setStyle("-fx-font-weight:bold");
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
    // {{{
    this.title.setText("Add Database");
    this.folderField.setPromptText("Select an empty folder for your Database");
    this.databaseName.setPromptText("Name your Database (max 50 characters)");
    this.selectFolderButton.setText("Select");
    this.cancelButton.setText("Cancel");
    this.saveButton.setText("Save");
    this.helpButton.setText("Help");
    // }}}
  }

  private String defaultPath;

  @Override
  public void initCustomComponents() {
    final File homeDir = this.cfg.getHomeConfigDir();
    boolean useUserDir = this.cfg.useUserFolderConfiguration();
    int count = 1;
    File database = new File((useUserDir ? homeDir + File.separator + "db" : "portable-db") + count);
    while (database.exists()) {
      database = new File((useUserDir ? homeDir + File.separator + "db" : "portable-db") + ++count);
    }
    this.defaultPath = database.getPath();
    this.folderField.setText(database.getPath());
    // String message = "Select an empty directory and a suitable name, then click 'Save'";
    // this.errorField.setText(message);
    this.addTooltip(this.selectFolderButton, "Select an empty folder for your Database", -205, 0);
    this.addTooltip(this.folderField, "Folder field", 0, -28);
    this.addTooltip(this.databaseName, "Name field", 0, 32);
  }

  @Override
  protected void asserts() {
    // TODO Auto-generated method stub
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

    if (!name.trim().isEmpty()) {
      if (!StringTools.isValidFileName(name, Config.MAX_FILE_LENGTH)) {
        message = "Not a valid database name.";
        validName = false;
      } else {
        validName = true;
        List<File> configFiles = this.cfg.getUserConfigurationFiles();
        for (File config : configFiles) {
          if (config.getName().toLowerCase().equals(
              (name + Config.PROPERTIES_EXTENSION).toLowerCase())) {
            validName = false;
            message = "This database name already exists.";
          }
        }
      }
    }

    if (!folder.trim().isEmpty()) {
      if (folder.trim().equals(this.defaultPath)) {
        validFolder = true;
      } else {
        File dir = new File(folder);
        if ((!dir.isDirectory() || !dir.exists())) {
          message = "Not a valid directory.";
        } else if (!dir.canWrite() || !dir.canRead()) {
          message = "Can't write directory.";
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

    if (message != null) {
      this.errorField.setStyle("-fx-font-weight:bold;-fx-text-fill:red");
    } else {
      this.errorField.setStyle("");
      if (validName && validFolder) {
        message = "Click 'Save' to create your database";
      } else {
        message = "Select an empty directory and a suitable name, then click 'Save'";
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
