package mj.ocraptor.javafx.controllers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.javafx.GUITemplate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.LocaleUtils;

public class SelectDatabase extends GUITemplate {
  // *INDENT-OFF*
  public static double INIT_WIDTH  = 550;
  public static double INIT_HEIGHT = 235;
  public static final  String FXML = "SelectDatabase.fxml";
  // *INDENT-ON*

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(SelectDatabase.class);

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
  private TableView<UserDB> dbTable;

  /**
   *
   *
   * @param event
   */
  @FXML
  private void selectButtonClicked(ActionEvent event) {
    try {
      boolean validConfiguration = g.loadConfiguration(selectedConfig);

      if (validConfiguration) {
        gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
      } else {
        EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent e) {
            File configFile = new File(selectedConfig);
            if (!configFile.getAbsoluteFile().equals(
                new File(cfg.getConfigUserFilePath()).getAbsoluteFile())) {
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
        g.showConfirmationDialog(g.getText("SELECT_DATABASE.DELETE_ENTRY"), handler, 350, 150, true);
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
    this.addTooltip(this.emptyMessage, g.getText("HELP_TOOLTIP"), 0, -43);
    // this.emptyMessage.setTooltip(new Tooltip(g.getText("HELP_TOOLTIP")));
  }

  @Override
  protected void initLabels() {
    this.emptyMessage.setText(g.getText("SELECT_DATABASE.NO_DATABASE_MESSAGE"));

    this.emptyMessage.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent t) {
        helpButtonClicked(null);
      }
    });

    this.addDBButton.setText(g.getText("SELECT_DATABASE.ADD_DB_BUTTON"));
    this.selectButton.setText(g.getText("SELECT"));
    this.title.setText(g.getText("SELECT_DATABASE.TITLE"));
  }

  @Override
  protected void initListeners() {
    dbTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<UserDB>() {
      @Override
      public void changed(ObservableValue<? extends UserDB> observable, UserDB oldValue,
          UserDB newValue) {

        selectButton.setDisable(false);
        if (newValue != null) {
          selectedConfig = newValue.getConfigFile().getAbsolutePath();
        }

      }
    });
  }

  /**
   *
   */
  private class UserDB {
    private File configFile;
    private File dbFolder;
    private Date lastModified;
    private String normalizedDate;

    final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy - HH:mm", Localization
        .instance().getLocale());

    /**
     * @return the configFile
     */
    public File getConfigFile() {
      return configFile;
    }

    /**
     * @param configFile
     *          the configFile to set
     */
    public void setConfigFile(File configFile) {
      this.configFile = configFile;
    }

    /**
     * @return the dbFolder
     */
    public File getDbFolder() {
      return dbFolder;
    }

    /**
     * @param dbFolder
     *          the dbFolder to set
     */
    public void setDbFolder(File dbFolder) {
      this.dbFolder = dbFolder;
    }

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
      return lastModified;
    }

    /**
     * @param lastModified
     *          the lastModified to set
     */
    public void setLastModified(Date lastModified) {
      this.lastModified = lastModified;
      setNormalizedDate(dateFormat.format(lastModified));
    }

    /**
     * @return the normalizedDate
     */
    public String getNormalizedDate() {
      return normalizedDate;
    }

    /**
     * @param normalizedDate
     *          the normalizedDate to set
     */
    public void setNormalizedDate(String normalizedDate) {
      this.normalizedDate = normalizedDate;
    }
  }

  @SuppressWarnings("unchecked")
  private void initList() {
    final List<UserDB> dbsToShow = new ArrayList<UserDB>();
    final List<File> configFiles = this.cfg.getUserConfigurationFiles();

    for (final File file : configFiles) {
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

      final UserDB dbToShow = new UserDB();
      dbToShow.setConfigFile(file);
      dbToShow.setLastModified(modifiedDate);
      dbToShow.setDbFolder(dbFolder);
      dbsToShow.add(dbToShow);
    }

    if (dbsToShow.isEmpty()) {
      this.emptyMessage.setVisible(true);
      this.dbTable.setVisible(false);
      return;
    } else {
      this.emptyMessage.setVisible(false);
      this.dbTable.setVisible(true);
    }

    dbTable.getItems().clear();
    dbTable.getItems().addAll(dbsToShow);

    final TableColumn<UserDB, String> configName = new TableColumn<UserDB, String>(g
        .getText("SELECT_DATABASE.NAME"));
    configName
        .setCellValueFactory(new Callback<CellDataFeatures<UserDB, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<UserDB, String> p) {
            return new ReadOnlyObjectWrapper<String>(FilenameUtils.removeExtension(p.getValue()
                .getConfigFile().getName()));
          }
        });

    final TableColumn<UserDB, String> lastModified = new TableColumn<UserDB, String>(g
        .getText("SELECT_DATABASE.MODIFIED"));
    lastModified
        .setCellValueFactory(new Callback<CellDataFeatures<UserDB, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<UserDB, String> p) {
            return new ReadOnlyObjectWrapper<String>(p.getValue().getNormalizedDate());
          }
        });

    final TableColumn<UserDB, String> dbPath = new TableColumn<UserDB, String>(g
        .getText("SELECT_DATABASE.DB_PATH"));
    dbPath
        .setCellValueFactory(new Callback<CellDataFeatures<UserDB, String>, ObservableValue<String>>() {
          public ObservableValue<String> call(CellDataFeatures<UserDB, String> p) {
            // TODO: validity check
            return new ReadOnlyObjectWrapper<String>(FileTools.multiplatformPath(p.getValue()
                .getDbFolder().getPath()));
          }
        });

    dbPath.setCellFactory(new Callback<TableColumn<UserDB, String>, TableCell<UserDB, String>>() {
      @Override
      public TableCell<UserDB, String> call(final TableColumn<UserDB, String> p) {
        return new StyledTableCell();
      }
    });

    // TODO: style
    configName.setPrefWidth(203);
    lastModified.setPrefWidth(140);
    dbPath.setPrefWidth(203);
    dbTable.getColumns().clear();
    dbTable.getColumns().addAll(configName, lastModified, dbPath);
  }

  // *INDENT-OFF*
  public static final String CSS_ORIGINAL = "cell-renderer-original";
  public static final String CSS_ERROR    = "cell-renderer-error";
  // *INDENT-ON*

  /**
   *
   */
  private class StyledTableCell extends TableCell<UserDB, String> {
    @Override
    protected void updateItem(final String item, final boolean empty) {
      super.updateItem(item, empty);
      setText(empty ? "" : item);
      getStyleClass().removeAll(CSS_ORIGINAL, CSS_ERROR);
      if (!empty) {
        final File directory = new File(item);
        if (directory.isFile() || !directory.exists()) {
          addTooltip(this, g.getText("EDIT_DB.DIR_DOES_NOT_EXIST"), 0, -45);
          getStyleClass().add(CSS_ERROR);
        } else if (!directory.canRead() || !directory.canWrite()) {
          addTooltip(this, g.getText("EDIT_DB.DIR_NOT_WRITABLE"), 0, -45);
          getStyleClass().add(CSS_ERROR);
        }
      }
    }
  }

  @Override
  public void initCustomComponents() {
    this.cfg = Config.inst();
    this.initList();

    // Locale defaultLocale = Locale.getDefault();
    // String defaultLanguage = defaultLocale.getDisplayLanguage();

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
      public void changed(ObservableValue<? extends String> ov, String t, String newLocaleTag) {
        if (!locales.isEmpty()) {
          for (String currentLocale : locales.split(";")) {
            if (!currentLocale.isEmpty()) {
              String[] parts = currentLocale.split("-");
              if (newLocaleTag.equals(parts[1])) {
                Localization.instance().setLocale(LocaleUtils.toLocale(parts[0]));
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

    this.executeWorker(propValidatingWorker());
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
   * @param locale
   */
  private void addToChoiceBox(Locale locale, String translation) {
    Locale defaultLocale = Locale.getDefault();
    String defaultLocaleFromProperties = this.cfg.getProp(ConfigString.DEFAULT_LOCALE);

    if (!defaultLocaleFromProperties.isEmpty()) {
      defaultLocale = LocaleUtils.toLocale(defaultLocaleFromProperties);
    }
    // String language = locale.getDisplayLanguage();
    languageBox.getItems().add(translation);

    if (locale == Locale.GERMAN) {
    }

    if (locale.getLanguage().equals(defaultLocale.getLanguage())) {
      languageBox.getSelectionModel().select(translation);
    }
  }

  @Override
  protected void initEventHandlers() {
    //
  }
}
