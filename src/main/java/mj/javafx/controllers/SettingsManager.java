package mj.javafx.controllers;

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

import mj.configuration.Config;
import mj.configuration.properties.ConfigBool;
import mj.configuration.properties.ConfigInteger;
import mj.configuration.properties.ConfigString;
import mj.console.Platform.Os;
import mj.javafx.GUITemplate;
import mj.javafx.Icon;
import mj.javafx.Theme;
import mj.tools.StringTools;

import org.apache.commons.collections4.BidiMap;

public class SettingsManager extends GUITemplate {

  // @formatter:off
  public static double  INIT_WIDTH  = 550;
  public static double  INIT_HEIGHT = 400;
  public static final   String FXML = "SettingsManager.fxml";
  // @formatter:on

  private boolean faultyOptions = false;
  private ObservableList<Node> children;
  private int handlerCount;
  private String originalTheme;

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
    this.faultyOptions = false;

    for (EventHandler<ActionEvent> handler : this.saveHandlers) {
      handler.handle(null);
    }

    if (!this.faultyOptions) {
      this.mainController.getMainController().initMultiCoreProcessing();
      this.feedbackMessage.setText("Settings saved!");
      this.feedbackMessage.setStyle("-fx-font-weight:bold;-fx-text-fill:green");
    }

    String newTheme = this.cfg.getProp(this.cfg.getDefaultConfigFilePath(), ConfigString.THEME);

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
    this.outTransition();
    if (settingsButton.isSelected()) {
      this.addImageIcon(this.settingsButton, Icon.LIST_WHITE, 0);
      this.settingsButton.setText("Less");
      this.addAdvancedSettings();
    } else {
      while (this.children.size() > this.handlerCount) {
        this.children.remove(this.children.size() - 1);
      }
      while (this.saveHandlers.size() > this.handlerCount) {
        this.saveHandlers.remove(this.saveHandlers.size() - 1);
      }
      this.settingsButton.setText("More");
      this.addImageIcon(this.settingsButton, Icon.LIST, 0);
    }
    this.inTransitionAsWorker(500);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private Config config;
  private List<EventHandler<ActionEvent>> saveHandlers;

  @Override
  protected void initVisibility() {
    this.helpButton.setDisable(false);
  }

  private static final String DEFAULT_MESSAGE = "Click \"Save\" after you are finished.";

  @Override
  protected void initLabels() {
    this.feedbackMessage.setText(DEFAULT_MESSAGE);
    this.feedbackMessage.setTextFill(Color.GRAY);
    this.settingsButton.setText("More");
    this.title.setText("Settings Manager");
    this.cancel.setText("Back");
    this.addImageIcon(this.settingsButton, Icon.LIST, 0);
  }

  @Override
  public void initCustomComponents() {
    this.saveHandlers = new ArrayList<EventHandler<ActionEvent>>();
    this.children = this.hboxContainer.getChildren();
    this.config = Config.inst();
    this.updateFields();
    this.addStandardSettings();
    this.originalTheme = this.cfg.getProp(this.cfg.getDefaultConfigFilePath(), ConfigString.THEME);
  }

  private static final String OCR_NOT_SUPPORTED_INFO = //
  "Optical character recognition (OCR) is not" + " yet supported for this filetype. "
      + "Only the included plaintext will be indexed. " + "I'm working on it.";

  private static final String FILETYPE_NOT_SUPPORTED_INFO = //
  "This filetype is not yet supported. I'm working on it.";

  private static final String APPLE_13_NOT_SUPPORTED = //
  "Apple iWork'13 is not yet supported. I'm working on it.";

  private static final String PLANNED_INFO = //
  "Planned feature. Not yet supported.";

  private static final String HTML_INFO = //
  "Optical character recognition (OCR) is not" + " yet supported for this filetype. "
      + "Only the included plaintext will be indexed,"
      + " (Hot)links will not be resolved. I'm working on it.";

  /**
   *
   *
   */
  private void addStandardSettings() {
    // ------------------------------------------------ //
    this.addTitle("Settings for OCRaptor (version: " + Config.getVersion() + ")");
    this.saveHandlers.add(this.addBooleanButton("Enable optical character recognition (OCR)", null,
        ConfigBool.ENABLE_IMAGE_OCR, false, false));
    this.saveHandlers.add(this.addBooleanButton("Include Metadata", null,
        ConfigBool.INCLUDE_METADATA, false, false));
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Filetypes to include");
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Include standalone image files", Icon.IMAGE, null,
        ConfigBool.INCLUDE_STANDALONE_IMAGE_FILES, false, false));
    this.saveHandlers.add(this.addBooleanButton("Include PDF files", Icon.PDF, null,
        ConfigBool.INCLUDE_PDF_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Postscipt files", Icon.PS, null,
        ConfigBool.INCLUDE_POSTSCRIPT_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include XML files", Icon.TEXT, null,
        ConfigBool.INCLUDE_XML_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include text files", Icon.TEXT, null,
        ConfigBool.INCLUDE_TEXT_FILES, false, false));
    this.saveHandlers.add(this.addBooleanButton("Include HTML files", Icon.TEXT, HTML_INFO,
        ConfigBool.INCLUDE_HTML_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Epub files", Icon.RTF, null,
        ConfigBool.INCLUDE_EPUB_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Xournal files", Icon.ADDON, null,
        ConfigBool.INCLUDE_XOURNAL_FILES, false, true));
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Microsoft Office");
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft Word files", Icon.DOCX, null,
        ConfigBool.INCLUDE_MS_WORD_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft Excel files", Icon.XLSX, null,
        ConfigBool.INCLUDE_MS_EXCEL_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft Powerpoint files", Icon.PPTX,
        null, ConfigBool.INCLUDE_MS_POWERPOINT_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft XPS files", Icon.XPS, null,
        ConfigBool.INCLUDE_MS_XPS_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft RTF files", Icon.RTF,
        OCR_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_MS_RTF_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft CHM files", Icon.ADDON,
        HTML_INFO, ConfigBool.INCLUDE_MS_CHM_FILES, false, true));
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Apple iWork'09");
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Include Apple iWork Pages files", Icon.ADDON,
        APPLE_13_NOT_SUPPORTED, ConfigBool.INCLUDE_APPLE_PAGES_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Apple iWork Numbers files", Icon.ADDON,
        null, ConfigBool.INCLUDE_APPLE_NUMBERS_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include Apple iWork Key files", Icon.ADDON, null,
        ConfigBool.INCLUDE_APPLE_KEY_FILES, false, true));
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("LibreOffice/OpenOffice");
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Include LibreOffice Writer files", Icon.ODT, null,
        ConfigBool.INCLUDE_LO_WRITER_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include LibreOffice Impress files", Icon.ODP,
        null, ConfigBool.INCLUDE_LO_IMPRESS_FILES, false, true));
    this.saveHandlers.add(this.addBooleanButton("Include LibreOffice Calc files", Icon.ODS, null,
        ConfigBool.INCLUDE_LO_CALC_FILES, false, true));
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Upcoming, but not yet supported filetypes");
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Include archive files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_ARCHIVES, true, true));
    this.saveHandlers.add(this.addBooleanButton("Include DjVu files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_DJVU_FILES, true, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft Publisher files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_MS_PUBLISHER_FILES, true, true));
    this.saveHandlers.add(this.addBooleanButton("Include Microsoft OneNote files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_MS_ONENOTE_FILES, true, true));
    this.saveHandlers.add(this.addBooleanButton("Include Evernote files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_EVERNOTE_FILES, true, true));
    this.saveHandlers.add(this.addBooleanButton("Include RSS Feed files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_RSS_FEEDS, true, true));
    this.saveHandlers.add(this.addBooleanButton("Include Email files", Icon.ADDON,
        FILETYPE_NOT_SUPPORTED_INFO, ConfigBool.INCLUDE_EMAIL_FILES, true, true));
    // ------------------------------------------------ //
    this.handlerCount = this.children.size();
  }

  /**
   *
   *
   */
  private void addAdvancedSettings() {
    this.addCustomDivider();
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Preprocess images for OCR", null,
        ConfigBool.PRE_PROCESS_IMAGES_FOR_OCR, false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton(this.cfg.getDefaultConfigFilePath(),
        "Enable animations", null, null, ConfigBool.ENABLE_ANIMATIONS, false, true));
    // ------------------------------------------------ //
    // this.saveHandlers.add(this.addBooleanButton("Enable debugging", null,
    // ConfigBool.DEBUG_MODE,
    // false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Only show new files while indexing", null,
        ConfigBool.NEW_FILES_NOTIFICATION_ONLY, false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Enable bug report screens", null,
        ConfigBool.ENABLE_BUG_REPORT_SCREENS, false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Pause indexing on error", null,
        ConfigBool.PAUSE_ON_ERROR, false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Enable app command stderr output", null,
        ConfigBool.ENABLE_USER_COMMAND_STDERR, false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Always remove missing files from database", null,
        ConfigBool.ALWAYS_REMOVE_MISSING_FILES_FROM_DB, false, false));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addBooleanButton("Include hidden folders and files", null,
        ConfigBool.INDEX_HIDDEN_FILES_AND_FOLDERS, false, true));
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.DIALOG_METADATA_MAX_STRING_LENGTH, null,
        "Max metadata snippet length", null, true);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.DIALOG_SNIPPET_MAX_STRING_LENGTH, null,
        "Max fulltext snippet length", null, true);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MIN_IMAGE_SIZE_IN_KB, null,
        "Min image file size in KB", null, false);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MAX_IMAGE_SIZE_IN_KB, null,
        "Max image file size in KB", null, false);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MIN_IMAGE_WIDTH_FOR_OCR, null,
        "Min image width in Pixel", null, false);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MAX_IMAGE_WIDTH_FOR_OCR, null,
        "Max image width in Pixel", null, false);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MIN_IMAGE_HEIGHT_FOR_OCR, null,
        "Min image height in Pixel", null, false);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MAX_IMAGE_HEIGHT_FOR_OCR, null,
        "Max image height in Pixel", null, false);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MAX_TEXT_SIZE_IN_KB, null,
        "Max text file size in KB", null, true);
    // ------------------------------------------------ //
    this.addDefaultIntegerField(ConfigInteger.MAX_SEARCH_RESULTS, null,
        "Max search results to show", null, false);
    // ------------------------------------------------ //
    int availableCores = Runtime.getRuntime().availableProcessors();
    if (availableCores > 1) {
      Integer[] cores = new Integer[availableCores + 1];
      for (int i = 0; i <= availableCores; i++)
        cores[i] = i;

      this.saveHandlers.add(this.addDropdownMenu(null, null, "Number of CPU-Cores to use",
          ConfigInteger.NUMBER_OF_CPU_CORES_TO_USE, cores, 75, false));
    }
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addDropdownMenu(this.cfg.getDefaultConfigFilePath(),
        "Default Language", null, ConfigString.DEFAULT_LOCALE, this.cfg.getProp(
            ConfigString.AVAILABLE_LOCALES).split(";"), 200, true));
    // ------------------------------------------------ //
    this.saveHandlers.add(this.addDropdownMenu(this.cfg.getDefaultConfigFilePath(),
        "Default Theme", null, ConfigString.THEME, Theme.valuesAsString(), 200, true));
    // ------------------------------------------------ //
    ConfigString password = ConfigString.PASSWORDS_TO_USE;
    TextField passwordField = this.addStringField("Passwords", PLANNED_INFO, password, false);
    EventHandler<ActionEvent> passwordHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = passwordField.getText().trim();
        if (!text.isEmpty()) {
          cfg.setProp(password, text);
        }
      }
    };
    this.saveHandlers.add(passwordHandler);
    // ------------------------------------------------ //
    String langInfo = "Currently installed language files:\n";
    List<String> languageStrings = Config.getLanguageStrings();
    if (languageStrings != null && !languageStrings.isEmpty()) {
      for (String lang : languageStrings) {
        langInfo += lang + ", ";
      }
      langInfo = StringTools.removeLastCharacters(langInfo, 2);
    }

    ConfigString lang = ConfigString.DEFAULT_LANGUAGE_FOR_OCR;
    TextField langField = this.addStringField("OCR Lang", langInfo, lang, false);
    EventHandler<ActionEvent> langHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = langField.getText().trim();
        if (!text.isEmpty()) {
          cfg.setProp(lang, text);
        }
      }
    };
    this.saveHandlers.add(langHandler);
    // ------------------------------------------------ //
    ConfigString textExt = ConfigString.TEXT_FILE_EXTENSIONS;
    TextField textExtField = this.addStringField("Text file ext.", null, textExt, false);
    EventHandler<ActionEvent> textExtHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = textExtField.getText().trim();
        if (!text.isEmpty()) {
          cfg.setProp(textExt, text);
        }
      }
    };
    this.saveHandlers.add(textExtHandler);
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Bash/Shell commands for opening files (see Help Pages)");
    // ------------------------------------------------ //
    ConfigString cmd = ConfigString.getByOs(ConfigString.DIRECTORY_OPEN_CMD_);
    TextField app = this.addStringField("Directory", null, cmd, false);
    EventHandler<ActionEvent> dirHandler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = app.getText().trim();
        feedbackMessage.setTextFill(Color.DARKRED);
        if (!text.isEmpty()) {
          if (!(text.contains("{0}") || text.contains("{1}"))) {
            feedbackMessage.setText("Not a valid command for the File Manager Application.");
            faultyOptions = true;
          }
        }
        if (!faultyOptions) {
          cfg.setProp(cmd, text);
        }
      }
    };
    this.saveHandlers.add(dirHandler);
    // ------------------------------------------------ //
    this.addCMDField(ConfigString.getByOs(ConfigString.IMAGE_FILE_OPEN_CMD_), //
        null, "Image", false);
    this.addCMDField(ConfigString.getByOs(ConfigString.TEXT_FILE_OPEN_CMD_), //
        null, "Text", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.EPUB_FILE_OPEN_CMD_), //
        null, "Epub", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.PDF_FILE_OPEN_CMD_), //
        null, "PDF", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.PS_FILE_OPEN_CMD_), //
        null, "PostScript", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.XOJ_FILE_OPEN_CMD_), //
        null, "Xournal", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.HTML_FILE_OPEN_CMD_), //
        null, "HTML", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.XML_FILE_OPEN_CMD_), //
        null, "XML", true);
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Microsoft Office");
    // ------------------------------------------------ //
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_WORD_FILE_OPEN_CMD_), //
        null, "MS Word", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_EXCEL_FILE_OPEN_CMD_), //
        null, "MS Excel", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_PPT_FILE_OPEN_CMD_), //
        null, "MS PowerPoint", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_XPS_FILE_OPEN_CMD_), //
        null, "MS XPS", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_RTF_FILE_OPEN_CMD_), //
        null, "MS RTF", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_CHM_FILE_OPEN_CMD_), //
        null, "MS Chm", true);
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("LibreOffice/OpenOffice");
    // ------------------------------------------------ //
    this.addCMDField(ConfigString.getByOs(ConfigString.LO_WRITER_FILE_OPEN_CMD_), //
        null, "LO Writer", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.LO_CALC_FILE_OPEN_CMD_), //
        null, "LO Calc", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.LO_IMPRESS_FILE_OPEN_CMD_), //
        null, "LO Impress", true);
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Apple iWork");
    // ------------------------------------------------ //
    this.addCMDField(ConfigString.getByOs(ConfigString.APPLE_PAGES_FILE_OPEN_CMD_), //
        null, "Apple Pages", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.APPLE_NUMBERS_FILE_OPEN_CMD_), //
        null, "Apple Numbers", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.APPLE_KEY_FILE_OPEN_CMD_), //
        null, "Apple Key", true);
    // ------------------------------------------------ //
    this.addTitle("");
    this.addTitle("Not supported yet:");
    // ------------------------------------------------ //
    this.addCMDField(ConfigString.getByOs(ConfigString.DJVU_FILE_OPEN_CMD_), //
        null, "DjVu", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.ENML_FILE_OPEN_CMD_), //
        null, "Evernote", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.ARCHIVE_FILE_OPEN_CMD_), //
        null, "Archive", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_PUB_FILE_OPEN_CMD_), //
        null, "MS Publisher", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.MS_ONE_FILE_OPEN_CMD_), //
        null, "MS OneNote", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.RSS_FEEDS_FILE_OPEN_CMD_), //
        null, "RSS Feed", true);
    this.addCMDField(ConfigString.getByOs(ConfigString.EMAIL_FILE_OPEN_CMD_), //
        null, "Email", true);
    // ------------------------------------------------ //
  }

  /**
   *
   *
   * @param titleText
   */
  private void addTitle(String titleText) {
    Label description = new Label(titleText);
    description.setStyle("-fx-text-fill: #123462;-fx-font-size:16px");
    this.children.add(description);
  }

  /**
   *
   *
   * @param property
   * @param description
   * @param integerName
   */
  private void addDefaultIntegerField(ConfigInteger property, String infoString,
      String description, String integerName, boolean helpDisabled) {
    TextField field = this.addIntegerField(description, infoString, property, 75, helpDisabled);
    this.saveHandlers.add(getDefaultIntegerEventHandler(property, field, integerName));
  }

  /**
   *
   *
   * @param property
   * @param textField
   * @param integerName
   * @return
   */
  private EventHandler<ActionEvent> getDefaultIntegerEventHandler(ConfigInteger property,
      TextField textField, String integerName) {
    EventHandler<ActionEvent> handler = new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        String text = textField.getText().trim();
        feedbackMessage.setTextFill(Color.DARKRED);
        if (text.isEmpty()) {
          feedbackMessage.setText(integerName + " IS EMPTY");
          faultyOptions = true;
        } else {
          try {
            int integer = Integer.parseInt(text);
            cfg.setProp(property, integer);
          } catch (Exception e) {
            feedbackMessage.setText(integerName + " IS NOT A NUMBER");
            faultyOptions = true;
          }
        }
      }
    };
    return handler;
  }

  /**
   *
   *
   * @param cmd
   * @param appString
   */
  private void addCMDField(ConfigString cmd, String infoString, String appString,
      boolean helpDisabled) {
    TextField app = this.addStringField(appString, infoString, cmd, helpDisabled);
    this.saveHandlers.add(this.addDefaultAppEventHandler(cmd, app, appString));
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
            feedbackMessage.setText("Not a valid command for the " + applicationName
                + " Application.");
            faultyOptions = true;
          }
        }
        if (!faultyOptions) {
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
    return this.getDivider("settingsManagerDivider", 2.0f, 50, 2d, 10d);
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
    button.setText(button.isSelected() ? "Yes" : "No");
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
          mainController.showConfirmationDialog(infoText, 400, 150);
        }
      });
      hbox.getChildren().add(infoIcon);
    }

    if (iconPath != null) {
      String url = this.getClass().getResource(iconPath.getFileName()).toString();
      ImageView icon = null;
      if (disabled) {
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
          openHelpInBrowser(Config.getHelpFilePath(mainController.getLocale()), anchor);
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
  private EventHandler<ActionEvent> addBooleanButton(String labelText, String infoString,
      ConfigBool boolProperty, boolean disabled, boolean helpDisabled) {
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
  private EventHandler<ActionEvent> addBooleanButton(String labelText, Icon iconPath,
      String infoString, ConfigBool boolProperty, boolean disabled, boolean helpDisabled) {
    return addBooleanButton(null, labelText, iconPath, infoString, boolProperty, disabled,
        helpDisabled);
  }

  /**
   *
   *
   * @param labelText
   * @param buttonText
   * @param iconPath
   * @param eventHandler
   */
  private EventHandler<ActionEvent> addBooleanButton(String configFilePath, String labelText,
      Icon iconPath, String infoString, ConfigBool boolProperty, boolean disabled,
      boolean helpDisabled) {
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
  private TextField addStringField(String labelText, String infoString,
      ConfigString stringProperty, boolean helpDisabled) {
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
    return textfield;
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
  private TextField addIntegerField(String labelText, String infoString,
      ConfigInteger integerProperty, double width, boolean helpDisabled) {
    HBox hbox = this.createNewHbox(labelText, infoString, null, true, false, null);

    // textBox.setAlignment(Pos.CENTER_RIGHT);
    TextField textfield = new TextField();
    textfield.setMinHeight(29);
    textfield.setMaxHeight(29);
    textfield.setMinWidth(width);
    textfield.setMaxWidth(width);
    int loadedInteger = config.getProp(integerProperty);
    if (loadedInteger != -1) {
      textfield.setText(String.valueOf(loadedInteger));
    }

    HBox.setHgrow(textfield, Priority.ALWAYS);
    hbox.getChildren().add(textfield);

    hbox.getChildren().add(
        createHelpButton(0, helpDisabled == true ? null : integerProperty.name()));
    children.add(hbox);
    return textfield;
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
          Thread.sleep(faultyOptions ? 5000 : 500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (faultyOptions) {
              feedbackMessage.setText(DEFAULT_MESSAGE);
              feedbackMessage.setStyle("-fx-font-weight:normal;-fx-text-fill:gray");
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
