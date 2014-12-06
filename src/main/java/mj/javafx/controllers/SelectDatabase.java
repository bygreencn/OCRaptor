package mj.javafx.controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import mj.configuration.Config;
import mj.configuration.properties.ConfigString;
import mj.javafx.GUITemplate;
import mj.tools.StringTools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.LocaleUtils;

public class SelectDatabase extends GUITemplate {
  // @formatter:off
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 200;
  public static final   String FXML = "SelectDatabase.fxml";
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @FXML
  private Button addDBButton;

  @FXML
  private Button selectButton;

  @FXML
  private Label emptyMessage;

  @FXML
  private ComboBox<String> languageBox;

  @FXML
  private ListView<String> list = new ListView<String>();

  /**
   *
   *
   * @param event
   */
  @FXML
  private void selectButtonClicked(ActionEvent event) {
    try {
      String configurationPath = this.cfg.getUserFolder() + File.separator + selectedConfig
          + Config.PROPERTIES_EXTENSION;

      boolean validConfiguration = this.mainController.loadConfiguration(configurationPath);

      if (validConfiguration) {
        this.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
      } else {
        EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent e) {
            File configFile = new File(configurationPath);
            if (!configFile.getAbsoluteFile().equals(
                new File(cfg.getUserConfigFilePath()).getAbsoluteFile())) {
              final File database = new File(cfg.getProp(configFile.getAbsolutePath(),
                  ConfigString.DATABASE_FOLDER));
              if (database.exists()) {
                FileUtils.deleteQuietly(database);
              }
              FileUtils.deleteQuietly(configFile);
              gotoPage(SelectDatabase.FXML, SelectDatabase.INIT_WIDTH, SelectDatabase.INIT_HEIGHT);
            }
          }
        };
        this.mainController.showConfirmationDialog("Delete this entry?", handler);
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
  }

  /**
   *
   *
   * @param event
   */
  @FXML
  private void addDBButtonClicked(ActionEvent event) {
    this.gotoPage(AddDatabase.FXML, AddDatabase.INIT_WIDTH, AddDatabase.INIT_HEIGHT);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  //
  private String selectedConfig;

  @Override
  protected void initVisibility() {
    this.emptyMessage.setVisible(false);
    this.emptyMessage.setTooltip(new Tooltip("Help will be opened in your Internet Browser."));
  }

  @Override
  protected void initLabels() {
    this.emptyMessage.setText(this.mainController.getText("SELECT_DATABASE.NO_DATABASE_MESSAGE"));

    this.emptyMessage.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent t) {
        helpButtonClicked(null);
      }
    });

    this.addDBButton.setText(this.mainController.getText("SELECT_DATABASE.ADD_DB_BUTTON"));
    this.selectButton
        .setText(this.mainController.getText("SELECT_DATABASE.SELECT_DATABASE_BUTTON"));
    this.title.setText(this.mainController.getText("SELECT_DATABASE.TITLE"));
  }

  @Override
  protected void initListeners() {
    list.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
          String newValue) {
        selectButton.setDisable(false);
        if (newValue != null) {
          selectedConfig = newValue.split(":")[0].trim();
        }
      }
    });
  }

  private void initList() {
    List<File> configFiles = this.cfg.getUserConfigurationFiles();
    ArrayList<String> configFileNames = new ArrayList<String>();

    SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy - HH:mm", this.mainController
        .getLocale());

    for (File file : configFiles) {
      Date modifiedDate = null;
      try {
        modifiedDate = new Date(Long.parseLong(this.cfg.getProp(file.getAbsolutePath(),
            ConfigString.LAST_TIME_MODIFIED)));
      } catch (Exception e) {
      }
      if (modifiedDate == null) {
        modifiedDate = new Date(file.lastModified());
      }

      final File dbFolder = new File(this.cfg.getProp(file.getAbsolutePath(),
          ConfigString.DATABASE_FOLDER));
      boolean validFolder = true;

      if (!dbFolder.exists() || !dbFolder.canWrite()) {
        validFolder = false;
      }

      String name = StringTools.getFileNameWithoutExtension(file)
          + " : ["
          + (validFolder ? (this.mainController.getText("SELECT_DATABASE.MODIFIED") + ": " + dateFormat
              .format(modifiedDate))
              : "ERROR - CAN NOT READ FOLDER") + "]";
      configFileNames.add(name);
    }

    ObservableList<String> items = FXCollections.observableArrayList(configFileNames);

    if (items.size() == 0) {
      this.emptyMessage.setVisible(true);
    } else {
      list.setItems(items);
      list.getSelectionModel().selectFirst();
    }
  }

  @Override
  public void initCustomComponents() {
    this.cfg = Config.inst();
    this.initList();

    Locale defaultLocale = Locale.getDefault();
    String defaultLanguage = defaultLocale.getDisplayLanguage();

    String locales = this.cfg.getProp(ConfigString.AVAILABLE_LOCALES);

    if (!locales.isEmpty()) {
      String[] localesFromPropertyFile = locales.split(";");
      for (String currentLocale : localesFromPropertyFile) {
        if (!currentLocale.isEmpty()) {
          String[] parts = currentLocale.split("-");
          Locale localeItem = LocaleUtils.toLocale(parts[0]);
          this.addToChoiceBox(localeItem, parts[1]);
        }
      }
    }

    this.languageBox.valueProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue ov, String t, String newLocaleTag) {
        if (!locales.isEmpty()) {
          for (String currentLocale : locales.split(";")) {
            if (!currentLocale.isEmpty()) {
              String[] parts = currentLocale.split("-");
              if (newLocaleTag.equals(parts[1])) {
                mainController.setLocale(LocaleUtils.toLocale(parts[0]));
                cfg.setProp(ConfigString.DEFAULT_LOCALE, parts[0]);
                initLabels();
                initTemplateLabels();
                initList();
              }
            }
          }
        }
      }
    });

    this.pane.getScene().addEventHandler(KeyEvent.ANY, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.S) {
          if (!selectButton.isDisabled()) {
            selectButtonClicked(null);
          }
        }
        if (event.getCode() == KeyCode.A) {
          addDBButtonClicked(null);
        }
      }
    });
  }

  @Override
  protected void asserts() {
    assert list != null : "fx:id=\"list\" was not injected: check your FXML file 'SelectDatabase.fxml'.";
    assert emptyMessage != null : "fx:id=\"emptyMessage\" was not injected: check your FXML file 'SelectDatabase.fxml'.";
    assert addDBButton != null : "fx:id=\"addDBButton\" was not injected: check your FXML file 'SelectDatabase.fxml'.";
    assert languageBox != null : "fx:id=\"languageBox\" was not injected: check your FXML file 'SelectDatabase.fxml'.";
    assert selectButton != null : "fx:id=\"selectButton\" was not injected: check your FXML file 'SelectDatabase.fxml'.";
  }

  @Override
  protected double getWindowWidth() {
    return 550;
  }

  @Override
  protected double getWindowHeight() {
    return 200;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param locale
   */
  private void addToChoiceBox(Locale locale, String translation) {
    Locale defaultLocale = Locale.getDefault();
    String defaultLocaleFromProperties = this.cfg.getProp(ConfigString.DEFAULT_LOCALE);

    if (!defaultLocaleFromProperties.isEmpty()) {
      defaultLocale = LocaleUtils.toLocale(defaultLocaleFromProperties);
    }
    String language = locale.getDisplayLanguage();
    languageBox.getItems().add(translation);

    if (locale == Locale.GERMAN) {
    }

    if (locale.getLanguage().equals(defaultLocale.getLanguage())) {
      languageBox.getSelectionModel().select(translation);
    }
  }

  @Override
  protected void initEventHandlers() {
    // TODO Auto-generated method stub

  }
}
