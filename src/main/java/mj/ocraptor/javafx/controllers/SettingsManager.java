package mj.ocraptor.javafx.controllers;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.console.Platform.Os;
import mj.ocraptor.javafx.GUITemplate;
import mj.ocraptor.javafx.Icon;
import mj.ocraptor.javafx.Theme;
import mj.ocraptor.tools.SystemTools;
import mj.ocraptor.tools.St;
import mj.ocraptor.tools.Tp;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.lang3.LocaleUtils;

public class SettingsManager extends GUITemplate {

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(SettingsManager.class);

  // *INDENT-OFF*
  public static double INIT_WIDTH  = 550;
  public static double INIT_HEIGHT = 400;
  public static final  String FXML = "SettingsManager.fxml";

  private static final String
    SAVED_STYLE_CLASS              = "settingsButtonSaved",
    SETTINGS_TITLE_CLASS           = "settingsTitle",
    SETTINGS_DIVIDER_CLASS         = "settingsManagerDivider",
    SETTINGS_BUTTON_DEFAULT_CLASS  = "settingsButtonDefault";
  // *INDENT-ON*

  private boolean breakSaving = false;
  private ObservableList<Node> children;
  private int handlerCount;
  private String originalTheme;

  private Config config;
  private List<EventHandler<ActionEvent>> saveHandlers;
  private List<ToggleButton> fileTypeButtons;
  private boolean fileButtonsToggled = true;

  @FXML
  private Button cancel;

  @FXML
  private VBox hboxContainer;

  @FXML
  private Button saveButton;

  @FXML
  private ToggleButton settingsButton;

  @FXML
  private Label feedbackMessage;

  @FXML
  private ScrollPane settingsScrollPane;

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @FXML
  void saveButtonClicked(ActionEvent event) {
    this.breakSaving = false;

    for (EventHandler<ActionEvent> handler : this.saveHandlers) {
      handler.handle(null);
      if (this.breakSaving) {
        break;
      }
    }

    if (!this.breakSaving) {
      this.g.getParentController().initMultiCoreProcessing();
      this.feedbackMessage.setText(g.getText("SETTINGS.SAVED"));

      final ObservableList<String> styleClasses = this.feedbackMessage.getStyleClass();
      styleClasses.clear();
      styleClasses.add(SAVED_STYLE_CLASS);
    }

    String newTheme = this.cfg.getProp(this.cfg.getConfigMasterFilePath(), ConfigString.THEME);

    if (!newTheme.equals(originalTheme)) {
      this.changeThemeTo(Theme.getByName(newTheme));
      this.originalTheme = newTheme;
    }
    this.executeWorker(saveWorker());
  }

  @FXML
  void cancelButtonClicked(ActionEvent event) {
    this.gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
  }

  @FXML
  void settingsButtonClicked(ActionEvent event) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        outTransition();
      }
    });

    this.executeWorker(changeSettingsWorker());
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  protected void initVisibility() {
    this.helpButton.setDisable(false);
  }

  @Override
  protected void initLabels() {
    this.feedbackMessage.setText(g.getText("SETTINGS.SAVE_REMINDER"));
    // TODO: style
    this.feedbackMessage.setTextFill(Color.GRAY);
    this.settingsButton.setText(g.getText("SETTINGS.MORE_BUTTON"));
    this.title.setText(g.getText("SETTINGS.TITLE"));
    this.cancel.setText(g.getText("BACK_BUTTON"));
    this.saveButton.setText(g.getText("SAVE"));
    this.helpButton.setText(g.getText("HELP"));
    this.addImageIcon(this.settingsButton, Icon.LIST, 0);
  }

  @Override
  public void initCustomComponents() {
    this.fileTypeButtons = new ArrayList<ToggleButton>();
    this.saveHandlers = new ArrayList<EventHandler<ActionEvent>>();
    this.children = this.hboxContainer.getChildren();
    this.config = Config.inst();
    this.updateFields();
    this.addStandardSettings();
    this.originalTheme = this.cfg.getProp(this.cfg.getConfigMasterFilePath(), ConfigString.THEME);

    this.pane.getScene().addEventHandler(KeyEvent.ANY, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
          //
        }
        if (event.getCode() == KeyCode.ESCAPE) {
          cancelButtonClicked(null);
        }
      }
    });
  }

  private Task<Object> changeSettingsWorker() {
    return new Task<Object>() {
      @Override
      protected Object call() throws Exception {
        Thread.sleep(500);
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (settingsButton.isSelected()) {
              addImageIcon(settingsButton, Icon.LIST_WHITE, 0);
              settingsButton.setText(g.getText("SETTINGS.LESS_BUTTON"));
              addAdvancedSettings();
            } else {
              while (children.size() > handlerCount) {
                children.remove(children.size() - 1);
              }
              while (saveHandlers.size() > handlerCount) {
                saveHandlers.remove(saveHandlers.size() - 1);
              }
              settingsButton.setText(g.getText("SETTINGS.MORE_BUTTON"));
              addImageIcon(settingsButton, Icon.LIST, 0);
            }
            inTransitionAsWorker(500);
          }
        });
        return true;
      }
    };
  }

  /**
   *
   *
   */
  private void addStandardSettings() {
    try {
      // ------------------------------------------------ //
      this.addTitle(g.getText("SETTINGS.TITLE_DETAILS", Config.HARDCODED_VERSION));

      this.addBooleanButton(g.getText("SETTINGS.ENABLE_OCR"), null, ConfigBool.ENABLE_IMAGE_OCR,
          false, false).getKey();
      // TODO: new feature
      // this.addBooleanButton(g.getText("SETTINGS.INCLUDE_FULLTEXT"), null,
      // ConfigBool.INCLUDE_FULLTEXT,
      // false, false).getKey();
      this.addBooleanButton(g.getText("SETTINGS.INCLUDE_METADATA"), null,
          ConfigBool.INCLUDE_METADATA, false, false).getKey();

      // ------------------------------------------------ //
      // TODO: Metadata button
      // this.addBooleanButton(g.getText("SETTINGS.ENABLE_METADATA"), null,
      // ConfigBool.INCLUDE_METADATA,
      // true, false).getKey();
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.FILETYPES_TO_INCLUDE"));
      // ------------------------------------------------ //
      this.addToggleAllButton(null, true);
      // ------------------------------------------------ //
      Tp<EventHandler<ActionEvent>, ToggleButton> filetypeButton;
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_IMAGE_FILES"), Icon.IMAGE,
          null, ConfigBool.INCLUDE_STANDALONE_IMAGE_FILES, false, false);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_PDF"), Icon.PDF, null,
          ConfigBool.INCLUDE_PDF_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_PS"), Icon.PS, null,
          ConfigBool.INCLUDE_POSTSCRIPT_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_EPUB"), Icon.RTF, null,
          ConfigBool.INCLUDE_EPUB_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_XOURNAL"), Icon.ADDON,
          null, ConfigBool.INCLUDE_XOURNAL_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_XML"), Icon.TEXT, null,
          ConfigBool.INCLUDE_XML_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_HTML"), Icon.TEXT, g
          .getText("SETTINGS.HTML_INFO"), ConfigBool.INCLUDE_HTML_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_TEXT"), Icon.TEXT, null,
          ConfigBool.INCLUDE_TEXT_FILES, false, false);
      this.fileTypeButtons.add(filetypeButton.getValue());
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.MS_OFFICE"));
      // ------------------------------------------------ //
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_WORD"), Icon.DOCX, null,
          ConfigBool.INCLUDE_MS_WORD_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_EXCEL"), Icon.XLSX, null,
          ConfigBool.INCLUDE_MS_EXCEL_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_PPT"), Icon.PPTX, null,
          ConfigBool.INCLUDE_MS_POWERPOINT_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_XPS"), Icon.XPS, null,
          ConfigBool.INCLUDE_MS_XPS_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_RTF"), Icon.RTF, g
          .getText("SETTINGS.OCR_NOT_SUPPORTED"), ConfigBool.INCLUDE_MS_RTF_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.INCLUDE_LIBRE"));
      // ------------------------------------------------ //
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_WRITER"), Icon.ODT, null,
          ConfigBool.INCLUDE_LO_WRITER_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_IMPRESS"), Icon.ODP, null,
          ConfigBool.INCLUDE_LO_IMPRESS_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_CALC"), Icon.ODS, null,
          ConfigBool.INCLUDE_LO_CALC_FILES, false, true);
      this.fileTypeButtons.add(filetypeButton.getValue());
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.UPCOMING_FILETYPES"));
      // ------------------------------------------------ //

      // ------------------------------------------------ //
      // ------------------------------------------------ //
      // this.addSeperator();
      // this.addTitle(g.getText("SETTINGS.IWORK9"));
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_PAGES"), Icon.ADDON,
          this.g.getText("SETTINGS.APPLE_WORK13_NOT_SUPPORTED"),
          ConfigBool.INCLUDE_APPLE_PAGES_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_NUMBERS"), Icon.ADDON,
          null, ConfigBool.INCLUDE_APPLE_NUMBERS_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_KEY"), Icon.ADDON, null,
          ConfigBool.INCLUDE_APPLE_KEY_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      final String notSupported = g.getText("SETTINGS.FILETYPE_NOT_SUPPORTED");
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_ARCHIVES"), Icon.ADDON,
          notSupported, ConfigBool.INCLUDE_ARCHIVES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_DJVU"), Icon.ADDON,
          notSupported, ConfigBool.INCLUDE_DJVU_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_PUBLISHER"), Icon.ADDON,
          notSupported, ConfigBool.INCLUDE_MS_PUBLISHER_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_ONENOTE"), Icon.ADDON,
          notSupported, ConfigBool.INCLUDE_MS_ONENOTE_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      filetypeButton = this.addBooleanButton(g.getText("SETTINGS.INCLUDE_EMAILS"), Icon.ADDON,
          notSupported, ConfigBool.INCLUDE_EMAIL_FILES, true, true);
      // this.fileTypeButtons.add(filetypeButton.getValue());
      // ------------------------------------------------ //
      this.handlerCount = this.children.size();
    } catch (NullPointerException e) {
      // TODO: logging
      e.printStackTrace();
    }
  }

  /**
   *
   *
   */
  private void addAdvancedSettings() {
    try {
      this.addCustomDivider();
      // ------------------------------------------------ //
      this.addTitle(g.getText("SETTINGS.OCR_SECTION"));
      // ------------------------------------------------ //
      this.addBooleanButton(g.getText("SETTINGS.INCLUDE_PREPROCESS_IMAGE"), null,
          ConfigBool.PRE_PROCESS_IMAGES_FOR_OCR, false, false).getKey();
      // ------------------------------------------------ //
      // this.addIntegerField(ConfigInteger.MAX_FULLTEXT_LENGTH, null, g
      // .getText("SETTINGS.MAX_FULLTEXT_LENGTH"), null, false, "");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MIN_IMAGE_SIZE_IN_KB, null, g
          .getText("SETTINGS.MIN_IMAGE_SIZE"), null, false, "KB");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MAX_IMAGE_SIZE_IN_KB, null, g
          .getText("SETTINGS.MAX_IMAGE_SIZE"), null, false, "KB");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MIN_IMAGE_WIDTH_FOR_OCR, null, g
          .getText("SETTINGS.MIN_IMAGE_WIDTH"), null, false, "Px");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MAX_IMAGE_WIDTH_FOR_OCR, null, g
          .getText("SETTINGS.MAX_IMAGE_WIDTH"), null, false, "Px");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MIN_IMAGE_HEIGHT_FOR_OCR, null, g
          .getText("SETTINGS.MIN_IMAGE_HEIGHT"), null, false, "Px");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MAX_IMAGE_HEIGHT_FOR_OCR, null, g
          .getText("SETTINGS.MAX_IMAGE_HEIGHT"), null, false, "Px");
      // ------------------------------------------------ //
      String langInfo = g.getText("SETTINGS.CURRENTLY_INSTALLED") + "\n";
      List<String> languageStrings = Config.getLanguageStrings();
      if (languageStrings != null && !languageStrings.isEmpty()) {
        for (String lang : languageStrings) {
          langInfo += lang + ", ";
        }
        langInfo = St.removeLastCharacters(langInfo, 2);
      }
      this.addStringField(g.getText("SETTINGS.OCR_LANG"), langInfo,
          ConfigString.DEFAULT_LANGUAGE_FOR_OCR, false);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.ERROR_HANDLING"));
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addBooleanButton(g.getText("SETTINGS.BUG_REPORTS"), null,
          ConfigBool.ENABLE_BUG_REPORT_SCREENS, false, false).getKey());
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addBooleanButton(g.getText("SETTINGS.PAUSE_INDEXING_ON_ERROR"),
          null, ConfigBool.PAUSE_ON_ERROR, false, false).getKey());
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addBooleanButton(g.getText("SETTINGS.APP_STD_ERR"), null,
          ConfigBool.ENABLE_USER_COMMAND_STDERR, false, false).getKey());
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addDropdownMenu(this.cfg.getConfigUserFilePath(), g
          .getText("SETTINGS.LOGGER_LEVEL"), null, ConfigString.LOGFILE_THRESHOLD_OUTPUT,
          new String[] { "FATAL", "ERROR", "INFO" }, 110, true));
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.INDEXING"));
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addBooleanButton(g.getText("SETTINGS.SHOW_ONLY_NEW"), null,
          ConfigBool.NEW_FILES_NOTIFICATION_ONLY, false, false).getKey());
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addBooleanButton(g.getText("SETTINGS.REMOVE_MISSING_FILES"), null,
          ConfigBool.ALWAYS_REMOVE_MISSING_FILES_FROM_DB, false, false).getKey());
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addBooleanButton(g.getText("SETTINGS.INCLUDE_HIDDEN_FOLDERS"),
          null, ConfigBool.INDEX_HIDDEN_FILES_AND_FOLDERS, false, true).getKey());
      // ------------------------------------------------ //
      int availableCores = Runtime.getRuntime().availableProcessors();
      if (availableCores > 1) {
        Integer[] cores = new Integer[availableCores + 1];
        for (int i = 0; i <= availableCores; i++)
          cores[i] = i;

        this.saveHandlers.add(this.addDropdownMenu(null, null, g.getText("SETTINGS.CPU_CORES"),
            ConfigInteger.NUMBER_OF_CPU_CORES_TO_USE, cores, 75, false));
      }
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.RMI_SERVER_PORT, null, g.getText("SETTINGS.RMI_PORT"),
          null, true, "");
      // ------------------------------------------------ //
      Tp<EventHandler<ActionEvent>, TextField> xms = this.addIntegerField(
          ConfigInteger.PROCESS_XMS, null, g.getText("SETTINGS.PROCESS_XMS"), null, true, "MB");
      Tp<EventHandler<ActionEvent>, TextField> xmx = this.addIntegerField(
          ConfigInteger.PROCESS_XMX, null, g.getText("SETTINGS.PROCESS_XMX"), null, true, "MB");

      this.saveHandlers.remove(xmx.getKey());
      this.saveHandlers.remove(xms.getKey());

      EventHandler<ActionEvent> memoryHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          try {
            Integer xmsValue = St.extractInteger(xms.getValue().getText());
            Integer xmxValue = St.extractInteger(xmx.getValue().getText());

            final SystemTools sigar = new SystemTools();
            long maxRam = sigar.getMaxRamInMB();
            maxRam -= 1024; // remove an estimated value for os memory
                            // allocation

            if (xmsValue < 64) { // lower end
              feedbackMessage.setText(g.getText("SETTINGS.XMS_TOO_LOW"));
              breakSaving = true;
            }

            if (xmsValue > xmxValue) {
              feedbackMessage.setText(g.getText("SETTINGS.XMS_TOO_HIGH"));
              breakSaving = true;
            }

            if (xmxValue > maxRam) {
              feedbackMessage.setText(g.getText("SETTINGS.XMX_TOO_HIGH"));
              breakSaving = true;
            }
          } catch (Exception e) {
          }
        }
      };
      this.saveHandlers.add(memoryHandler);
      this.saveHandlers.add(xmx.getKey());
      this.saveHandlers.add(xms.getKey());
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.PROCESSING_TIMEOUT_IN_SECONDS, null, "Timeout", null,
          true, " s");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MAX_TEXT_SIZE_IN_KB, null, g
          .getText("SETTINGS.MAX_TEXT_FILE_SIZE"), null, true, "KB");
      // ------------------------------------------------ //
      this.addStringField(g.getText("SETTINGS.TEXT_FILE_EXT"), null,
          ConfigString.TEXT_FILE_EXTENSIONS, false);
      // ------------------------------------------------ //
      this.addStringField(g.getText("SETTINGS.PASSWORDS"), g.getText("SETTINGS.PLANNED_INFO"),
          ConfigString.PASSWORDS_TO_USE, false);
      // ------------------------------------------------ //
      this.addStringField(g.getText("SETTINGS.IGNORING"), g.getText("SETTINGS.PLANNED_INFO"),
          ConfigString.FILES_AND_DIRS_TO_IGNORE, false);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.SEARCHING"));
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.DIALOG_SNIPPET_MAX_STRING_LENGTH, null, g
          .getText("SETTINGS.FULLTEXT_SNIPPET_LENGTH"), null, true, "");
      // ------------------------------------------------ //
      this.addIntegerField(ConfigInteger.MAX_SEARCH_RESULTS, null, g
          .getText("SETTINGS.MAX_SEARCH_RESULTS"), null, false, "");
      // ------------------------------------------------ //
      this.addBooleanButton(g.getText("SETTINGS.ENABLE_ENTRY_DELETION_BY_USER"), null,
          ConfigBool.ENABLE_ENTRY_DELETION_BY_USER, false, false).getKey();
      // ------------------------------------------------ //
      this.addStringField(g.getText("SETTINGS.STOPWORDS"), null, ConfigString.STOP_WORDS, true);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.GLOBAL"));
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addDropdownMenu(this.cfg.getConfigMasterFilePath(), g
          .getText("SETTINGS.DEFAULT_LANGUAGE"), null, ConfigString.DEFAULT_LOCALE, this.cfg
          .getProp(ConfigString.AVAILABLE_LOCALES).split(";"), 200, true));
      // ------------------------------------------------ //
      this.saveHandlers.add(this.addDropdownMenu(this.cfg.getConfigMasterFilePath(), g
          .getText("SETTINGS.DEFAULT_THEME"), null, ConfigString.THEME, Theme.valuesAsString(),
          200, true));
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.BASH_COMMAND"));
      // ------------------------------------------------ //
      ConfigString cmd = ConfigString.getByOs(ConfigString.DIRECTORY_OPEN_CMD_);
      Tp<EventHandler<ActionEvent>, TextField> app = this.addStringField(g.getText("DIRECTORY"),
          null, cmd, false);

      EventHandler<ActionEvent> dirHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          String text = app.getValue().getText().trim();
          feedbackMessage.setTextFill(Color.DARKRED);
          if (!text.isEmpty()) {
            if (!(text.contains("{0}") || text.contains("{1}"))) {
              feedbackMessage.setText(g.getText("SETTINGS.NOT_A_VALID_DIR_COMMAND"));
              breakSaving = true;
            }
          }
          if (!breakSaving) {
            cfg.setProp(cmd, text);
          }
        }
      };

      // overwrite standard event handler
      this.saveHandlers.remove(app.getKey());
      this.saveHandlers.add(dirHandler);

      // ------------------------------------------------ //
      this.addCMDField(ConfigString.getByOs(ConfigString.IMAGE_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.IMAGE"), false);
      this.addCMDField(ConfigString.getByOs(ConfigString.TEXT_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.TEXT"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.EPUB_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.EPUB"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.PDF_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.PDF"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.PS_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.PS"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.XOJ_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.XOURNAL"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.HTML_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.HTML"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.XML_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.XML"), true);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.MS_OFFICE"));
      // ------------------------------------------------ //
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_WORD_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.WORD"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_EXCEL_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.EXCEL"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_PPT_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.PPT"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_XPS_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.XPS"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_RTF_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.RTF"), true);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.INCLUDE_LIBRE"));
      // ------------------------------------------------ //
      this.addCMDField(ConfigString.getByOs(ConfigString.LO_WRITER_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.WRITER"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.LO_CALC_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.CALC"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.LO_IMPRESS_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.IMPRESS"), true);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.IWORK"));
      // ------------------------------------------------ //
      this.addCMDField(ConfigString.getByOs(ConfigString.APPLE_PAGES_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.PAGES"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.APPLE_NUMBERS_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.NUMBERS"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.APPLE_KEY_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.KEY"), true);
      // ------------------------------------------------ //
      this.addSeperator();
      this.addTitle(g.getText("SETTINGS.NOT_SUPPORTED"));
      // ------------------------------------------------ //
      this.addCMDField(ConfigString.getByOs(ConfigString.DJVU_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.DJVU"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.ARCHIVE_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.ARCHIVE"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_PUB_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.PUBLISHER"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.MS_ONE_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.ONENOTE"), true);
      this.addCMDField(ConfigString.getByOs(ConfigString.EMAIL_FILE_OPEN_CMD_), //
          null, g.getText("SETTINGS.EMAIL"), true);
      // ------------------------------------------------ //
    } catch (NullPointerException e) {
      LOGGER.error("Can not generate all setting entries", e);
      gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
    }
  }

  /**
   *
   *
   */
  private void addSeperator() {
    this.addTitle("");
  }

  /**
   *
   *
   * @param titleText
   */
  private void addTitle(String titleText) {
    final Text description = new Text(titleText);
    final ObservableList<String> styleClasses = description.getStyleClass();
    styleClasses.clear();
    styleClasses.add(SETTINGS_TITLE_CLASS);
    this.children.add(description);
  }

  /**
   *
   *
   * @param cmd
   * @param appString
   */
  private void addCMDField(ConfigString cmd, String infoString, String appString,
      boolean helpDisabled) {
    Tp<EventHandler<ActionEvent>, TextField> app = this.addStringField(appString, infoString, cmd,
        helpDisabled);
    this.saveHandlers.remove(app.getKey());
    this.saveHandlers.add(this.addDefaultAppEventHandler(cmd, app.getValue(), appString));
  }

  /**
   *
   *
   * @param cmd
   * @param textField
   * @param applicationName
   * @return
   */
  private EventHandler<ActionEvent> addDefaultAppEventHandler(ConfigString cmd,
      TextField textField, String applicationName) {

    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = textField.getText().trim();
        feedbackMessage.setTextFill(Color.DARKRED);
        if (!text.isEmpty()) {
          if (!text.contains("{0}")) {
            feedbackMessage.setText(g.getText("SETTINGS.NOT_A_VALID_COMMAND", applicationName));
            breakSaving = true;
          }
        }
        if (!breakSaving) {
          cfg.setProp(cmd, text);
        }
      }
    };
    return handler;

  }

  /**
   *
   *
   */
  private void addCustomDivider() {
    this.children.add(new Text(""));
    this.children.add(getCustomDivider());
    this.children.add(new Text(""));
  }

  /**
   *
   *
   * @return
   */
  private Line getCustomDivider() {
    return this.getDivider(SETTINGS_DIVIDER_CLASS, 2.0f, 50, 2d, 10d);
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
   * @param button
   */
  private void updateToggleButtonText(ToggleButton button) {
    // button.setSelected(button.isSelected());
    button.setText(button.isSelected() ? g.getText("YES") : g.getText("NO"));
    // TODO: style
    button.setTextFill(button.isSelected() ? Color.GREEN : Paint.valueOf("#4B0000"));
  }

  /**
   *
   *
   */
  private void updateFields() {
  }

  /**
   *
   *
   * @param labelText
   * @param iconPath
   * @param growLabel
   * @param minWidth
   * @return
   */
  private HBox createNewHbox(String labelText, String infoString, Icon iconPath, boolean growLabel,
      boolean boolButton, Double minWidth) {
    return createNewHbox(labelText, iconPath, growLabel, false, boolButton, minWidth, infoString);
  }

  /**
   *
   *
   * @param labelText
   * @param iconPath
   * @param growLabel
   * @param minWidth
   * @param infoText
   * @return
   */
  private HBox createNewHbox(String labelText, Icon iconPath, boolean growLabel, boolean disabled,
      boolean boolButton, Double minWidth, String infoText) {
    HBox hbox = new HBox();

    hbox.setSpacing(5);
    hbox.setAlignment(Pos.CENTER);

    HBox hboxLabel = new HBox();
    if (growLabel) {
      HBox.setHgrow(hboxLabel, Priority.ALWAYS);
    }
    hboxLabel.setAlignment(Pos.CENTER_LEFT);

    if (infoText != null && minWidth != null) {
      minWidth -= 35;
    }

    Label label = new Label();
    if (minWidth != null) {
      label.setMinWidth(minWidth);
    }

    label.setText(labelText);
    hboxLabel.getChildren().add(label);
    hbox.getChildren().add(hboxLabel);

    if (infoText != null) {
      ImageView infoIcon = new ImageView(this.getClass().getResource(Icon.INFO.getFileName())
          .toString());
      infoIcon.setCursor(Cursor.HAND);
      infoIcon.setFitHeight(30);
      infoIcon.setPickOnBounds(true);
      infoIcon.setPreserveRatio(true);
      infoIcon.setOnMouseClicked(new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent t) {
          g.showConfirmationDialog(infoText, 400, 150);
        }
      });
      hbox.getChildren().add(infoIcon);
    }

    if (iconPath != null) {
      String url = this.getClass().getResource(iconPath.getFileName()).toString();
      ImageView icon = null;
      if (disabled) {
        // TODO: performance hit, replace with image file
        icon = new ImageView(this.getGreyscaleIcon(iconPath));
      } else {
        icon = new ImageView(url);
      }
      icon.setFitHeight(30);
      // icon.setTranslateY(1);
      icon.setPickOnBounds(true);
      icon.setPreserveRatio(true);
      hbox.getChildren().add(icon);
    }
    return hbox;
  }

  /**
   *
   *
   * @param helpDisabled
   * @return
   */
  private Button createHelpButton(final double translateY, final String anchorString) {
    Button help = new Button();
    // TODO: style
    help.setMinWidth(40);
    help.setMaxWidth(40);
    help.setMinHeight(28);
    help.setMaxHeight(28);
    help.setTranslateY(translateY);
    this.addImageIcon(help, Icon.HELP, 1);

    if (anchorString != null) {
      EventHandler<ActionEvent> helpHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          String anchor = anchorString;
          anchor = anchor.replace("_" + Os.LINUX.name(), "");
          anchor = anchor.replace("_" + Os.WINDOWS.name(), "");
          anchor = anchor.replace("_" + Os.OSX.name(), "");
          openHelpInBrowser(Config.getHelpFilePath(Localization.instance().getLocale()), anchor);
        }
      };
      help.setOnAction(helpHandler);
    } else {
      help.setDisable(true);
    }

    return help;
  }

  /**
   *
   *
   * @param labelText
   * @param infoString
   * @param boolProperty
   * @param disabled
   * @param helpDisabled
   * @return
   */
  private Tp<EventHandler<ActionEvent>, ToggleButton> addBooleanButton(String labelText,
      String infoString, ConfigBool boolProperty, boolean disabled, boolean helpDisabled) {
    return addBooleanButton(null, labelText, null, infoString, boolProperty, disabled, helpDisabled);
  }

  /**
   *
   *
   * @param labelText
   * @param iconPath
   * @param infoString
   * @param boolProperty
   * @param disabled
   * @param helpDisabled
   * @return
   */
  private Tp<EventHandler<ActionEvent>, ToggleButton> addBooleanButton(String labelText,
      Icon iconPath, String infoString, ConfigBool boolProperty, boolean disabled,
      boolean helpDisabled) {
    return addBooleanButton(null, labelText, iconPath, infoString, boolProperty, disabled,
        helpDisabled);
  }

  /**
   *
   *
   * @param configFilePath
   * @param labelText
   * @param iconPath
   * @param infoString
   * @param boolProperty
   * @param disabled
   * @param helpDisabled
   * @return
   */
  private void addToggleAllButton(String infoString, boolean helpDisabled) {
    HBox hbox = this.createNewHbox(g.getText("SETTINGS.TOGGLE_FILETYPES"), null, true, false, true,
        null, infoString);

    Button toggleButton = new Button();
    toggleButton.setMinWidth(75);
    // TODO: style
    toggleButton.setStyle("-fx-font-size:16px;");
    toggleButton.setText("\u2193\u2193\u2193");

    EventHandler<ActionEvent> updateLabelHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        for (ToggleButton button : fileTypeButtons) {
          button.fire();
          button.setSelected(!fileButtonsToggled);
          updateToggleButtonText(button);
        }
        fileButtonsToggled = !fileButtonsToggled;
      }
    };

    toggleButton.setOnAction(updateLabelHandler);
    hbox.getChildren().add(toggleButton);

    hbox.getChildren().add(createHelpButton(0, helpDisabled == true ? null : "TOGGLE_ALL_BUTTON"));
    children.add(hbox);
  }

  /**
   *
   *
   * @param labelText
   * @param buttonText
   * @param iconPath
   * @param eventHandler
   */
  private Tp<EventHandler<ActionEvent>, ToggleButton> addBooleanButton(String configFilePath,
      String labelText, Icon iconPath, String infoString, ConfigBool boolProperty,
      boolean disabled, boolean helpDisabled) {
    HBox hbox = this.createNewHbox(labelText, iconPath, true, disabled, true, null, infoString);

    ToggleButton toggleButton = new ToggleButton();
    toggleButton.setDisable(disabled);
    toggleButton.setMinWidth(75);

    boolean toggleButtonStatus = false;
    if (configFilePath == null) {
      toggleButtonStatus = config.getProp(boolProperty);
    } else {
      toggleButtonStatus = config.getProp(configFilePath, boolProperty);
    }
    toggleButton.setSelected(toggleButtonStatus);
    updateToggleButtonText(toggleButton);

    EventHandler<ActionEvent> updateLabelHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        updateToggleButtonText(toggleButton);
      }
    };

    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        if (configFilePath == null) {
          config.setProp(boolProperty, toggleButton.isSelected());
        } else {
          config.setProp(configFilePath, boolProperty, toggleButton.isSelected());
        }
      }
    };

    toggleButton.setOnAction(updateLabelHandler);
    this.updateToggleButtonText(toggleButton);
    hbox.getChildren().add(toggleButton);

    hbox.getChildren().add(createHelpButton(0, helpDisabled == true ? null : boolProperty.name()));
    children.add(hbox);

    this.saveHandlers.add(handler);
    return new Tp<EventHandler<ActionEvent>, ToggleButton>(handler, toggleButton);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param labelText
   * @param iconPath
   * @param boolProperty
   * @param helpText
   * @return
   */
  private EventHandler<ActionEvent> addDropdownMenu(String configFilePath, String infoString,
      String labelText, ConfigInteger integerProperty, Integer[] possibleValues, double width,
      boolean helpDisabled) {

    HBox hbox = this.createNewHbox(labelText, infoString, null, true, false, null);

    ComboBox<Integer> dropdown = new ComboBox<Integer>();
    dropdown.setMinHeight(29);
    dropdown.setMaxHeight(29);
    dropdown.setPrefWidth(width);
    for (int i : possibleValues) {
      dropdown.getItems().add(i);
    }

    Integer loadedValue = null;
    if (configFilePath != null) {
      loadedValue = config.getProp(configFilePath, integerProperty);
    } else {
      loadedValue = config.getProp(integerProperty);
    }

    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        if (configFilePath != null) {
          config.setProp(configFilePath, integerProperty, (Integer) dropdown.getSelectionModel()
              .getSelectedItem());
        } else {
          config.setProp(integerProperty, (Integer) dropdown.getSelectionModel().getSelectedItem());
        }
      }
    };
    if (loadedValue != -1) {
      dropdown.getSelectionModel().select(loadedValue);
    }
    hbox.getChildren().add(dropdown);
    hbox.getChildren().add(
        createHelpButton(0, helpDisabled == true ? null : integerProperty.name()));
    children.add(hbox);

    return handler;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param labelText
   * @param iconPath
   * @param boolProperty
   * @param helpDisabled
   * @return
   */
  private EventHandler<ActionEvent> addDropdownMenu(String configFilePath, String labelText,
      String infoString, ConfigString stringProperty, String[] possibleValues, double width,
      boolean helpDisabled) {
    HBox hbox = this.createNewHbox(labelText, infoString, null, true, false, null);

    ComboBox<String> dropdown = new ComboBox<String>();
    dropdown.setMinWidth(width);
    dropdown.setMaxWidth(width);
    dropdown.setMinHeight(29);
    dropdown.setMaxHeight(29);

    for (String st : possibleValues) {
      if (stringProperty == ConfigString.DEFAULT_LOCALE) {
        dropdown.getItems().add(st.split("-")[1]);
      } else {
        dropdown.getItems().add(st);
      }
    }

    String loadedValue = null;
    if (configFilePath != null) {
      loadedValue = config.getProp(configFilePath, stringProperty);
    } else {
      loadedValue = config.getProp(stringProperty);
    }
    if (stringProperty == ConfigString.DEFAULT_LOCALE) {
      final BidiMap<String, String> langs = this.cfg.getGUILanguageStrings();
      if (langs.containsKey(loadedValue)) {
        loadedValue = langs.get(loadedValue).trim();
      }
    }

    if (!loadedValue.isEmpty()) {
      dropdown.getSelectionModel().select(loadedValue);
    }

    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String selectedItem = ((String) dropdown.getSelectionModel().getSelectedItem()).split("-")[0]
            .trim();
        if (stringProperty == ConfigString.DEFAULT_LOCALE) {
          final BidiMap<String, String> langs = cfg.getGUILanguageStrings();
          if (langs.containsValue(selectedItem)) {
            selectedItem = langs.getKey(selectedItem);
            Localization.instance().setLocale(LocaleUtils.toLocale(selectedItem));
          }
        }
        if (configFilePath != null) {
          cfg.setProp(configFilePath, stringProperty, selectedItem);
        } else {
          cfg.setProp(stringProperty, selectedItem);
        }
      }
    };
    hbox.getChildren().add(dropdown);

    hbox.getChildren()
        .add(createHelpButton(0, helpDisabled == true ? null : stringProperty.name()));
    children.add(hbox);

    return handler;
  }

  /**
   *
   *
   * @param labelText
   * @param buttonText
   * @param iconPath
   * @param eventHandler
   */
  private Tp<EventHandler<ActionEvent>, TextField> addStringField(String labelText,
      String infoString, ConfigString stringProperty, boolean helpDisabled) {
    HBox hbox = this.createNewHbox(labelText, infoString, null, false, false, 120d);

    HBox textBox = new HBox();
    // textBox.setAlignment(Pos.CENTER_RIGHT);
    TextField textfield = new TextField();
    textfield.setMinHeight(29);
    textfield.setMaxHeight(29);
    String loadedText = config.getProp(stringProperty);
    if (!loadedText.isEmpty()) {
      textfield.setText(loadedText);
    }
    textBox.setPadding(new Insets(0, 0, 0, 10));
    HBox.setHgrow(textBox, Priority.ALWAYS);
    HBox.setHgrow(textfield, Priority.ALWAYS);
    textBox.getChildren().add(textfield);
    hbox.getChildren().add(textBox);

    hbox.getChildren()
        .add(createHelpButton(0, helpDisabled == true ? null : stringProperty.name()));
    children.add(hbox);

    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = textfield.getText().trim();
        if (!text.isEmpty()) {
          cfg.setProp(stringProperty, text);
        }
      }
    };

    this.saveHandlers.add(handler);
    return new Tp<EventHandler<ActionEvent>, TextField>(handler, textfield);
  }

  /**
   *
   *
   * @param integerProperty
   * @param infoString
   * @param labelText
   * @param integerName
   * @param helpDisabled
   * @param unit
   * @return
   */
  private Tp<EventHandler<ActionEvent>, TextField> addIntegerField(ConfigInteger integerProperty,
      String infoString, String labelText, String integerName, boolean helpDisabled, String unit) {
    return addIntegerField(integerProperty, infoString, labelText, integerName, helpDisabled, unit,
        75);
  }

  /**
   *
   *
   * @param labelText
   * @param integerProperty
   * @param width
   * @param helpDisabled
   * @return
   */
  private Tp<EventHandler<ActionEvent>, TextField> addIntegerField(ConfigInteger integerProperty,
      String infoString, String labelText, String integerName, boolean helpDisabled, String unit,
      double width) {
    HBox hbox = this.createNewHbox(labelText, infoString, null, true, false, null);

    // textBox.setAlignment(Pos.CENTER_RIGHT);
    TextField textfield = new TextField();
    textfield.setMinHeight(29);
    textfield.setMaxHeight(29);
    textfield.setMinWidth(width);
    textfield.setMaxWidth(width);
    int loadedInteger = config.getProp(integerProperty);
    if (loadedInteger != -1) {
      textfield.setText(String.valueOf(loadedInteger) + " " + unit);
    }

    HBox.setHgrow(textfield, Priority.ALWAYS);
    hbox.getChildren().add(textfield);

    hbox.getChildren().add(
        createHelpButton(0, helpDisabled == true ? null : integerProperty.name()));
    children.add(hbox);
    EventHandler<ActionEvent> handler = getDefaultIntegerEventHandler(integerProperty, textfield,
        integerName, unit);
    this.saveHandlers.add(handler);
    return new Tp<EventHandler<ActionEvent>, TextField>(handler, textfield);
  }

  /**
   *
   *
   * @param property
   * @param textField
   * @param integerProp
   * @return
   */
  private EventHandler<ActionEvent> getDefaultIntegerEventHandler(ConfigInteger property,
      TextField textField, String integerProp, String unit) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = textField.getText().trim();
        feedbackMessage.setTextFill(Color.DARKRED);
        if (text.isEmpty()) {
          feedbackMessage.setText(g.getText("SETTINGS.STRING_EMPTY", integerProp));
          breakSaving = true;
        } else {
          Integer value = St.extractInteger(text);
          if (value != null) {
            cfg.setProp(property, value);
          } else {
            feedbackMessage.setText(g.getText("SETTINGS.NOT_A_NUMBER", integerProp));
            breakSaving = true;
          }
        }
      }
    };
    return handler;
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

        try {
          Thread.sleep(breakSaving ? 5000 : 500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (breakSaving) {
              feedbackMessage.setText(g.getText("SETTINGS.SAVE_REMINDER"));
              final ObservableList<String> styleClasses = feedbackMessage.getStyleClass();
              styleClasses.clear();
              styleClasses.add(SETTINGS_BUTTON_DEFAULT_CLASS);
            } else {
              gotoPage(EditDatabase.FXML, EditDatabase.INIT_WIDTH, EditDatabase.INIT_HEIGHT);
            }
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
        if ((event.isShiftDown() && (event.getCode() == KeyCode.J || event.getCode() == KeyCode.DOWN))) {
          settingsScrollPane.setVvalue(settingsScrollPane.getVvalue() + 0.05);
        } else if (event.isShiftDown()
            && (event.getCode() == KeyCode.K || event.getCode() == KeyCode.UP)) {
          settingsScrollPane.setVvalue(settingsScrollPane.getVvalue() - 0.05);
        } else if (event.getCode() == KeyCode.J || event.getCode() == KeyCode.DOWN) {
          settingsScrollPane.setVvalue(settingsScrollPane.getVvalue() + 0.005);
        } else if (event.getCode() == KeyCode.K || event.getCode() == KeyCode.UP) {
          settingsScrollPane.setVvalue(settingsScrollPane.getVvalue() - 0.005);
        }
      }
    });
  }
}
