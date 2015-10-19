package mj.ocraptor.configuration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import static mj.ocraptor.configuration.properties.ConfigString.AVAILABLE_LOCALES;
import static mj.ocraptor.configuration.properties.ConfigString.DATABASE_FOLDER;
import static mj.ocraptor.configuration.properties.ConfigString.FOLDERS_TO_INDEX;

import static org.apache.commons.lang.StringUtils.repeat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.console.CommandLineInterpreter;
import mj.ocraptor.console.Platform;
import mj.ocraptor.console.Platform.Os;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.tools.St;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config implements java.io.Serializable {

  /**
   *
   */
  private static final long serialVersionUID = -8858998038624852010L;

  // ------------------------------------------------ //
  public static boolean DEBUG = false;
  // ------------------------------------------------ //

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private static Config instance;
  private Os usedOs;
  private Properties userProperties;
  private File tikaMimeFile, fullTextStylesheetFolder;
  private Map<String, LinkedHashSet<String>> notFoundProperties;
  private boolean relativeDatabasePath = false;
  private String basePath;

  public static final int MAX_FILE_LENGTH = 50;

  // *INDENT-OFF*
  private boolean
    resetDatabase,
    waitForUserInput,
    verbose,
    quiet,
    showProgress,
    builtInJRE,
    copyToUserFolder,
    clientDelayedShutdown
  ;
  // *INDENT-ON*

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  // *INDENT-OFF*
  private String
    userConfigFilePath,
    databasePath,
    directoryToIndex,
    searchString
  ;

  public static final String
    APP_NAME                      = "OCRaptor",
    APP_NAME_LOWER                = APP_NAME.toLowerCase(),
    SERVER_HOST                   = "127.0.0.1",
    SERVER_NAME                   = "RMIServer",
    SEARCH_DELIMITER_START_SINGLE = "<",
    SEARCH_DELIMITER_END_SINGLE   = ">",
    SEARCH_DELIMITER_START        = repeat(SEARCH_DELIMITER_START_SINGLE, 4),
    SEARCH_DELIMITER_END          = repeat(SEARCH_DELIMITER_END_SINGLE,   4),
    META_FILE_NAME                = "rpt:filename",
    META_FILE_PATH                = "rpt:filepath",
    FILE_NOT_FOUND_ERROR          = "Configuration file not found:",
    HARDCODED_VERSION             = "0.5.1-alpha" // MARKER:HIODLEVA
  ;

  // *INDENT-ON*

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  // *INDENT-OFF*
  private static final String
    FILES_FOLDER            = "res",
    USER_FOLDER             = "usr",
    LIBRARY_FOLDER          = "lib",
    TESS4J_FOLDER           = "tess",
    TESS4J_LANGUAGE_FOLDER  = "tessdata",
    TESS4J_WRAPPER_FOLDER   = "bins",
    TESS4J_NATIVE_FOLDER    = "tess4j-natives",
    TESS4J_WRAPPER_FILE     = "tess4j-wrapper.jar",
    OCRAPTOR_SUB            = APP_NAME_LOWER + ".jar",
    TIKA_FOLDER             = "tika",
    CONFIG_FOLDER           = "cnfg",
    PROFILE_FOLDER_NAME     = "lang",

    TIKA_MIMETYPES          = "tika-mimetypes.xml",
    TIKA_PDF_PROPERTIES     = "pdf-parser.properties",
    MAIN_CONFIG_FILE        = "default.properties",
    FULLTEXT_BROWSER_TMP    = APP_NAME_LOWER + "_fulltext",

    FULLTEXT_BROWSER_ORIG   = "browser-files",

    LOG_FOLDER              = "log",
    MAIN_LOG_FILE           = APP_NAME_LOWER + ".log",
    TESS_NATIVE_LOG_FILE    = "tesseract-2.log",

    HELP_FOLDER             = "help",
    HELP_HTML               = "help_{0}.html",

    TESS_NATIVE_LIN_64      = "lin-x86-64",
    TESS_NATIVE_OSX_64      = "osx-x86-64",
    TESS_NATIVE_WIN_32      = "win-x86-32",
    TESS_NATIVE_WIN_64      = "win-x86-64";
  // *INDENT-ON*

  // ------------------------------------------------ //
  // -- Config instance initialization
  // ------------------------------------------------ //

  /**
   *
   *
   * @param cfg
   * @return
   *
   * @throws IOException
   */
  public static synchronized Config init(Config cfg) throws IOException {
    if (instance == null) {
      instance = cfg;
    }
    return instance;
  }

  /**
   *
   *
   * @return
   *
   * @throws IOException
   */
  public static synchronized Config initFromGui() throws IOException {
    if (instance == null) {
      instance = new Config(CommandLineInterpreter.instance(), true);
    }
    return instance;
  }

  /**
   *
   *
   * @return
   * @throws IOException
   */
  public static synchronized Config initFromCLI() throws IOException {
    if (instance == null) {
      instance = new Config(CommandLineInterpreter.instance(), false);
    }
    return instance;
  }

  /**
   *
   *
   * @param reset
   * @param waitForUserInput
   * @param verbose
   * @param quiet
   * @param showProgress
   * @param userUserFolder
   * @param configFilePath
   * @param dbPath
   * @param directoryToIndex
   * @param searchString
   * @return
   * @throws IOException
   */
  public static synchronized Config init(boolean reset, boolean waitForUserInput, boolean verbose,
      boolean quiet, boolean showProgress, boolean userUserFolder, String configFilePath,
      String dbPath, String directoryToIndex, String searchString) throws IOException {

    instance = new Config(reset, waitForUserInput, verbose, quiet, showProgress, userUserFolder,
        configFilePath, dbPath, directoryToIndex, searchString);
    return instance;
  }

  // ------------------------------------------------ //
  // -- Setting main configuration file
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public String getConfigMasterFilePath() {
    return getConfigFolder() + File.separator + MAIN_CONFIG_FILE;
  }

  /**
   * @return the configFilePath
   */
  public String getConfigUserFilePath() {
    return userConfigFilePath;
  }

  /**
   * @param configFilePath
   *          the configFilePath to set
   * @throws FileNotFoundException
   */
  public void setConfigUserFilePath(final String configFilePath) throws FileNotFoundException {
    EventManager eventManager = EventManager.instance();
    File configFile = null;

    if (configFilePath != null) {
      configFile = new File(configFilePath);
      if (!configFile.exists() || !configFile.isFile() || !configFile.canWrite()) {
        eventManager.configFileNotFound(configFile);
      }
    } else {
      configFile = new File(getConfigMasterFilePath());
      if (!configFile.exists() || !configFile.isFile() || !configFile.canWrite()) {
        // TODO: log
        throw new FileNotFoundException(FILE_NOT_FOUND_ERROR + " \n\""
            + configFile.getAbsolutePath() + "\"\n");
      }
    }

    try {
      this.userConfigFilePath = configFile.getCanonicalPath();
      this.updateFileProperties();

      String dbProperty = this.getProp(DATABASE_FOLDER);
      if (dbProperty != null && !dbProperty.trim().isEmpty()) {
        this.setDatabasePath(dbProperty);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // ------------------------------------------------ //
  // -- Constructors
  // ------------------------------------------------ //

  /**
   *
   *
   * @param cli
   * @throws IOException
   */
  private Config(CommandLineInterpreter cli, boolean gui) throws IOException {
    this.basePath = FileTools.multiplatformPath(new File(""));
    if (!gui) {
      this.resetDatabase = cli.resetDB();
      this.waitForUserInput = cli.waitForUserInput();
      this.verbose = cli.verbose();
      this.quiet = cli.quiet();
      this.showProgress = cli.showProgressBar();
      this.setConfigUserFilePath(cli.getUserConfigFilePath());
      this.databasePath = cli.getDbDirectoryPath();
      this.directoryToIndex = cli.getIndexDirectoryPath();
      this.searchString = cli.getSearchString();
    } else {
      // using default config file path
      this.setConfigUserFilePath(null);
    }
    this.initSystemProperties();
    this.copyToUserFolder = cli.useUserFolderConfiguration();
    this.builtInJRE = cli.useBuiltInJRE();
  }

  /**
   *
   *
   * @param reset
   * @param update
   * @param waitForUserInput
   * @param verbose
   * @param quiet
   * @param showProgress
   * @param configFilePath
   * @param dbPath
   * @param directoryToIndex
   * @param searchString
   * @throws IOException
   */
  private Config(boolean reset, boolean waitForUserInput, boolean verbose, boolean quiet,
      boolean showProgress, boolean userFolder, String configFilePath, String dbPath,
      String directoryToIndex, String searchString) throws IOException {
    this.basePath = FileTools.multiplatformPath(new File(""));
    this.resetDatabase = reset;
    this.waitForUserInput = waitForUserInput;
    this.verbose = verbose;
    this.quiet = quiet;
    this.showProgress = showProgress;
    this.copyToUserFolder = userFolder;
    this.setConfigUserFilePath(configFilePath);
    this.databasePath = dbPath;
    this.directoryToIndex = directoryToIndex;
    this.searchString = searchString;
    this.initSystemProperties();
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public File getHomeConfigDir() {
    final File userHome = SystemUtils.getUserHome();
    try {
      FileTools.directoryIsValid(userHome, "Home directory");
      File newConfigDir = null;
      if (Platform.getSystem() == Os.LINUX || Platform.getSystem() == Os.OSX) {
        newConfigDir = new File(userHome, "." + APP_NAME_LOWER);
      } else if (Platform.getSystem() == Os.WINDOWS) {
        newConfigDir = new File(userHome, APP_NAME);
      }
      return newConfigDir;
    } catch (Exception e) {
      // TODO: log
      e.printStackTrace();
    }
    return null;
  }

  /**
   *
   *
   */
  public void prepareConfigurationDirectory() {
    try {
      File newConfigDir = getHomeConfigDir();
      if (copyToUserFolder && newConfigDir != null && !newConfigDir.exists()) {
        newConfigDir.mkdir();
        FileUtils.copyDirectory(new File(getFileFolder() + File.separator + CONFIG_FOLDER),
            newConfigDir);
      }
      setConfigUserFilePath(this.getConfigMasterFilePath());
    } catch (FileNotFoundException e) {
      // TODO:
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public static Config inst() {
    if (instance == null) {
      throw new NullPointerException("Config is not initialized");
    }
    return instance;
  }

  /**
   *
   *
   */
  private void initSystemProperties() {
    this.usedOs = Platform.getSystem();
    this.tikaMimeFile = new File(getTikaMimeFilePath());
    this.notFoundProperties = new HashMap<String, LinkedHashSet<String>>();
  }

  // ------------------------------------------------ //
  // -- TIKA:
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public static String getTikaFolder() {
    return getFileFolder() + File.separator + TIKA_FOLDER;
  }

  /**
   * @return the tikaMimeFile
   */
  public File getTikaMimeFile() {
    return this.tikaMimeFile;
  }

  /**
   *
   *
   * @return
   */
  public static String getTikaMimeFilePath() {
    return getTikaFolder() + File.separator + TIKA_MIMETYPES;
  }

  /**
   *
   *
   * @return
   */
  public static String getTikaPDFParserPropertiesFilePath() {
    return getTikaFolder() + File.separator + TIKA_PDF_PROPERTIES;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public BidiMap<String, String> getGUILanguageStrings() {
    final BidiMap<String, String> langs = new DualHashBidiMap<String, String>();
    final String[] langsFromProp = getProp(AVAILABLE_LOCALES).split(";");
    for (String st : langsFromProp) {
      final String[] parts = st.split("-");
      langs.put(parts[0], parts[1]);
    }
    return langs;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private static final String TRAINDATA_EXTENSION = ".traineddata";
  private static final String OSD_DATA = "osd";

  /**
   *
   *
   * @return
   */
  public static List<String> getLanguageStrings() {
    final File tess4jFolder = new File(getTess4jLanguageFolderPath());
    final List<String> files = new ArrayList<String>();
    final File[] filesToFilter = tess4jFolder.listFiles();

    if (tess4jFolder != null) {
      for (File file : filesToFilter) {
        if (file.getName().endsWith(TRAINDATA_EXTENSION)
            && !file.getName().equals(OSD_DATA + TRAINDATA_EXTENSION)) {
          files.add(FilenameUtils.getBaseName(file.getName()));
        }
      }
    }
    return files;
  }

  /**
   *
   *
   * @return
   */
  public static String getLangDetectProfilesFolder() {
    return getFileFolder() + File.separator + PROFILE_FOLDER_NAME;
  }

  /**
   *
   *
   * @return
   */
  public static String getTestFolderPath() {
    return "tst";
  }

  /**
   *
   *
   * @return
   */
  public static String getHelpFilePath(Locale locale) {
    return getFileFolder() + File.separator + HELP_FOLDER + File.separator
        + MessageFormat.format(HELP_HTML, locale.toString());
  }

  public static final String TARGET_FOLDER = "trg";

  /**
   *
   *
   * @return
   */
  public static boolean devMode() {
    // TODO:
    // if (new File(TARGET_FOLDER + File.separator + LIBRARY_FOLDER).exists()) {
    if (new File(TARGET_FOLDER).exists()) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @return
   */
  public static String getLibraryFolderPath() {
    if (devMode()) {
      return TARGET_FOLDER + File.separator + LIBRARY_FOLDER;
    } else {
      return LIBRARY_FOLDER;
    }
  }

  /**
   *
   *
   * @return
   */
  public static String getTess4jFolderPath() {
    return getFileFolder() + File.separator + TESS4J_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public static String getTess4jLanguageFolderPath() {
    return getTess4jFolderPath() + File.separator + TESS4J_LANGUAGE_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public static String getBinsFolder() {
    return getFileFolder() + File.separator + TESS4J_WRAPPER_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public static String getTess4jWrapperBinPath() {
    return getBinsFolder() + File.separator + TESS4J_WRAPPER_FILE;
  }

  /**
   *
   *
   * @return
   */
  public static String getParserBinPath() {
    return getBinsFolder() + File.separator + OCRAPTOR_SUB;
  }

  /**
   *
   *
   * @return
   */
  public static String getTess4jNativeLibsPath() {
    return getTess4jFolderPath() + File.separator + TESS4J_NATIVE_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public static String getTess4jNativeLibrariesFolderPath() {
    if (Platform.getSystem() == Os.OSX) {
      return getTess4jNativeLibsPath() + File.separator + TESS_NATIVE_OSX_64;
    }
    if (Platform.getSystem() == Os.LINUX) {
      return getTess4jNativeLibsPath() + File.separator + TESS_NATIVE_LIN_64;
    }
    if (Platform.getSystem() == Os.WINDOWS) {
      if (SystemUtils.OS_ARCH.contains("64")) {
        return getTess4jNativeLibsPath() + File.separator + TESS_NATIVE_WIN_64;
      } else {
        return getTess4jNativeLibsPath() + File.separator + TESS_NATIVE_WIN_32;
      }
    }
    return null;
  }

  /**
   *
   *
   * @return
   */
  public String getMainLogFolderPath() {
    return getConfigFolder() + File.separator + LOG_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public String getNativeTesseractLogFile() {
    return getMainLogFolderPath() + File.separator + TESS_NATIVE_LOG_FILE;
  }

  /**
   *
   *
   * @return
   */
  public String getMainLogFilePath() {
    return getMainLogFolderPath() + File.separator + MAIN_LOG_FILE;
  }

  /**
   *
   *
   * @return
   */
  public String getMainLogAbsoluteFilePath() {
    return new File(getMainLogFilePath()).getAbsolutePath();
  }

  /**
   *
   *
   * @return
   */
  public File getMainLogFile() {
    return new File(getMainLogFilePath()).getAbsoluteFile();
  }

  /**
   *
   *
   * @return
   */
  public static String getFileFolder() {
    return FILES_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public static File getFullTextStylesheetFolder() {
    return new File(getFileFolder() + File.separator + HELP_FOLDER + File.separator
        + FULLTEXT_BROWSER_ORIG);
  }

  /**
   *
   *
   * @return
   * @throws FileNotFoundException
   */
  public File getTempFullTextStylesheetFolder() {
    if (fullTextStylesheetFolder != null && fullTextStylesheetFolder.exists()) {
      return fullTextStylesheetFolder;
    }

    final File stylesheetFolder = getFullTextStylesheetFolder();
    if (stylesheetFolder.exists()) {
      final File tempStylesheetFolder = FileTools.getTempFolder(FULLTEXT_BROWSER_TMP, false);

      try {
        FileUtils.copyDirectory(stylesheetFolder, tempStylesheetFolder);
      } catch (IOException e) {
        // TODO: log
        e.printStackTrace();
      }

      if (tempStylesheetFolder.exists()) {
        this.fullTextStylesheetFolder = tempStylesheetFolder;
        return this.fullTextStylesheetFolder;
      }
    }

    if (fullTextStylesheetFolder == null || !fullTextStylesheetFolder.exists()) {
      this.fullTextStylesheetFolder = null;
    }

    return this.fullTextStylesheetFolder;
  }

  /**
   *
   *
   * @return
   */
  private String getConfigFolder() {
    String configFolder = null;
    if (this.copyToUserFolder) {
      configFolder = getHomeConfigDir().getAbsolutePath();
    } else {
      configFolder = getFileFolder() + File.separator + CONFIG_FOLDER;
    }
    return configFolder;
  }

  /**
   *
   *
   * @return
   */
  public String getUserFolder() {
    return getConfigFolder() + File.separator + USER_FOLDER;
  }

  /**
   *
   *
   * @return
   */
  public static File getJarFileFolder() {
    return new File(Config.class.getProtectionDomain().getCodeSource().getLocation().getPath());
  }

  /**
   *
   *
   * @return
   */
  public static String getJarFileName() {
    File temp = getJarFileFolder();
    if (temp != null && temp.isFile()) {
      if (temp.getName().endsWith(".jar")) {
        return St.getFileNameWithoutExtension(temp);
      }
    }
    return null;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   * @throws IOException
   *
   *
   */
  public void updateFileProperties() throws IOException {
    final File configFile = new File(userConfigFilePath);
    this.userProperties = new PropertiesManager(configFile.getAbsolutePath()).getProperties();

    // TODO: version-check:

    // String version = this.getProp(OCRAPTOR_VERSION);
    // if (!version.trim().equals(HARDCODED_VERSION)) {
    // LOG.error("Possible version conflict\nApplication-version: \"" +
    // HARDCODED_VERSION
    // + "\"\nConfig-version: \"" + version + "\"");
    // }
  }

  /**
   *
   *
   * @param property
   * @param bool
   * @return
   */
  public boolean setProp(ConfigBool property, boolean bool) {
    return setProp(this.userConfigFilePath, property, bool);
  }

  /**
   *
   *
   * @param property
   * @param bool
   */
  public boolean setProp(String configFilePath, ConfigBool property, boolean bool) {
    if (bool != getProp(configFilePath, property)) {
      this.setProp(configFilePath, property.name(), String.valueOf(bool).toLowerCase());
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param property
   * @param integer
   * @return
   */
  public boolean setProp(ConfigInteger property, int integer) {
    return setProp(this.userConfigFilePath, property, integer);
  }

  /**
   *
   *
   * @param property
   * @param bool
   */
  public boolean setProp(String configFilePath, ConfigInteger property, int integer) {
    if (integer != getProp(configFilePath, property)) {
      this.setProp(configFilePath, property.name(), String.valueOf(integer));
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param property
   * @param string
   * @return
   */
  public boolean setProp(ConfigString property, String string) {
    return setProp(this.userConfigFilePath, property, string);
  }

  /**
   *
   *
   * @param property
   * @param bool
   */
  public boolean setProp(String configFilePath, ConfigString property, String string) {
    if (!string.equals(getProp(configFilePath, property))) {
      this.setProp(configFilePath, property.name(), string);
      return true;
    }
    return false;
  }

  /**
   *
   *
   */
  private void setProp(String configFilePath, String property, String value) {
    checkNotNull(property);
    checkNotNull(value);
    PropertiesChanger propertiesChanger = new PropertiesChanger(true);
    value = value.replace("\\", "\\\\");

    FileInputStream in = null;
    FileOutputStream out = null;
    isNullOrEmpty(value);
    try {
      in = new FileInputStream(configFilePath);
      propertiesChanger.load(in);
      propertiesChanger.setProperty(property, value);
      out = new FileOutputStream(configFilePath);
      propertiesChanger.save(out);
      this.updateFileProperties();
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    } finally {
      try {
        if (in != null) {
          in.close();
        }
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param file
   * @return
   */
  public Properties getPropertiesFromFile(String file) {
    Properties prop = null;
    try {
      if (file.equals(this.getConfigUserFilePath())) {
        prop = userProperties;
      } else {
        // prop = new
        // PropertiesManager(getMasterConfigFilePath()).getProperties();
        prop = new PropertiesManager(file).getProperties();
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
    return prop;
  }

  /**
   *
   *
   * @param boolProperty
   * @return
   */
  public boolean getProp(ConfigBool boolProperty) {
    return getProp(this.getConfigUserFilePath(), boolProperty);
  }

  /**
   *
   *
   * @param property
   * @return
   */
  public boolean getProp(String configFilePath, ConfigBool boolProperty) {
    Properties prop = getPropertiesFromFile(configFilePath);
    if (boolProperty != null) {
      String par = prop.getProperty(boolProperty.name().trim());
      if (par != null) {
        if (par.trim().equalsIgnoreCase("true"))
          return true;
        else
          return false;
      }
    }
    throw new NullPointerException("Can not find property: " + boolProperty + " in "
        + configFilePath);
  }

  /**
   *
   *
   * @param intProperty
   * @return
   */
  public int getProp(ConfigInteger intProperty) {
    return getProp(this.getConfigUserFilePath(), intProperty);
  }

  /**
   *
   *
   * @param intProperty
   * @return
   */
  public Integer getProp(String configFilePath, ConfigInteger intProperty) {
    Properties prop = getPropertiesFromFile(configFilePath);
    if (intProperty != null) {
      String par = prop.getProperty(intProperty.name().trim());
      if (par != null)
        if (!par.trim().isEmpty()) {
          try {
            return Integer.parseInt(par.trim());
          } catch (NumberFormatException e) {
            return null;
          }
        } else {
          return null;
        }
    }
    throw new NullPointerException("Can not find property: " + intProperty + "in " + configFilePath);
  }

  /**
   *
   *
   * @param stringProperty
   * @return
   */
  public String getProp(ConfigString stringProperty) {
    return getProp(this.getConfigUserFilePath(), stringProperty);
  }

  /**
   *
   *
   * @param stringProperty
   * @return
   */
  public String getProp(String configFilePath, ConfigString stringProperty) {
    Properties prop = getPropertiesFromFile(configFilePath);
    if (prop == null) {
      throw new NullPointerException("Config init error: " + configFilePath);
    }

    if (stringProperty != null) {
      String trimmedString = prop.getProperty(stringProperty.name().trim());
      if (trimmedString != null) {
        return trimmedString.trim();
      }
    }
    throw new NullPointerException("Can not find property: " + stringProperty + "in "
        + configFilePath);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  public static final String PROPERTIES_EXTENSION = ".properties";

  /**
   *
   *
   * @return
   */
  public List<File> getUserConfigurationFiles() {
    File userFolder = new File(getUserFolder());
    List<File> files = new ArrayList<File>();
    File[] filesToSort = userFolder.listFiles();

    Arrays.sort(filesToSort, new Comparator<File>() {
      public int compare(File file1, File file2) {
        if (file1.lastModified() > file2.lastModified()) {
          return -1;
        } else if (file1.lastModified() < file2.lastModified()) {
          return 1;
        } else {
          return 0;
        }
      }
    });

    if (userFolder != null) {
      for (File file : filesToSort) {
        if (file.getName().endsWith(PROPERTIES_EXTENSION)) {
          files.add(file);
        }
      }
    }
    return files;
  }

  /**
   *
   *
   * @param name
   */
  public File createNewUserConfiguration(String name) {
    List<File> files = getUserConfigurationFiles();

    if (name != null) {
      boolean alreadyIn = false;

      for (File file : files) {
        if (file.getName().trim().toLowerCase().equals(
            name.trim().toLowerCase() + PROPERTIES_EXTENSION)) {
          alreadyIn = true;
        }
      }

      if (!St.isValidFileName(name, MAX_FILE_LENGTH)) {
        EventManager.instance().configFileNameInvalid();
      } else if (alreadyIn) {
        EventManager.instance().propertiesFileAlreadyExists();
      } else {
        File newConfigPath = new File(getUserFolder() + File.separator + name
            + PROPERTIES_EXTENSION);
        try {
          FileUtils.copyFile(new File(getConfigMasterFilePath()), newConfigPath);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return newConfigPath;
      }
    }
    return null;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param databaseFolder
   */
  public void saveDatabaseFolder(final String configFilePath, final String databaseFolder) {
    this.setDatabasePath(databaseFolder);
    this.setProp(configFilePath, DATABASE_FOLDER.name(), this.databasePath);
  }

  /**
   * @return the folderToIndex
   */
  public ArrayList<String> getFoldersToIndex() {
    String folderToIndex = getProp(FOLDERS_TO_INDEX);
    if (!isNullOrEmpty(folderToIndex)) {
      String[] folders = folderToIndex.split(";");
      return new ArrayList<String>(Arrays.asList(folders));
    }
    return new ArrayList<String>();
  }

  /**
   *
   *
   * @return
   */
  public ArrayList<String> getExistingFoldersToIndex() {
    String folderToIndex = getProp(FOLDERS_TO_INDEX);
    if (!isNullOrEmpty(folderToIndex)) {
      String[] folders = folderToIndex.split(";");
      ArrayList<String> existingFolders = new ArrayList<String>();
      for (String folder : folders) {
        File fileFromPath = new File(folder.trim());
        if (fileFromPath.isDirectory() && fileFromPath.exists()) {
          existingFolders.add(folder);
        }
      }
      return existingFolders;
    }
    return new ArrayList<String>();
  }

  /**
   *
   *
   * @param folder
   */
  public void addFolderToIndex(File folder) {
    final ArrayList<String> savedFolders = getFoldersToIndex();
    try {
      final String absoluteFolderPath = folder.getCanonicalPath();
      this.setDirectoryToIndex(absoluteFolderPath);

      if (!this.directoryToIndex.equals(absoluteFolderPath)) {
        folder = new File(this.directoryToIndex);
      }
      this.directoryToIndex = null;

      if (folder.exists() && folder.canRead()) {
        boolean alreadySaved = false;

        for (String f : savedFolders) {
          File fileFromList = new File(f.trim());

          // ------------------------------------------------ //
          if (fileFromList.exists()
              && fileFromList.getCanonicalPath().equals(folder.getCanonicalPath())) {
            alreadySaved = true;
            break;
          }
          // ------------------------------------------------ //
        }

        if (!alreadySaved) {
          savedFolders.add(FileTools.multiplatformPath(folder.getPath()));
        }

      } else {
        // TODO: gui-message
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    setFoldersToIndex(savedFolders);
  }

  /**
   *
   *
   * @param folders
   */
  public void setFoldersToIndex(List<String> folders) {
    checkNotNull(folders);
    StringBuffer folderString = new StringBuffer();

    for (String folder : folders) {
      folderString.append(folder.trim() + ";");
    }
    this.setProp(FOLDERS_TO_INDEX, folderString.toString());
  }

  /**
   *
   *
   * @param folder
   */
  public void removeFolderToIndex(String folder) {
    ArrayList<String> savedFolders = getFoldersToIndex();
    if (savedFolders != null) {
      savedFolders.remove(savedFolders.indexOf(folder));
    }
    setFoldersToIndex(savedFolders);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param path
   * @return
   */
  private String makeRelative(String path) {
    final String dbAbsolutePath = FileTools.multiplatformPath(new File(path).getAbsolutePath());
    final String basePathParent = FileTools.multiplatformPath(new File(this.basePath).getParent());
    final String commonPath = FileTools.getCommonPath(this.basePath, dbAbsolutePath);

    // System.out.println("DB Absolute Path: " + dbAbsolutePath);
    // System.out.println("Base Path Parent Path: " + basePathParent);
    // System.out.println("Common Path: " + commonPath);

    if (commonPath.startsWith(this.basePath)) {
      path = dbAbsolutePath.replace(this.basePath, "");
      if (path.startsWith("/")) {
        path = path.replaceFirst("/", "");
      }
    } else if (commonPath.startsWith(basePathParent)) {
      path = dbAbsolutePath.replace(basePathParent, "..");
      if (path.startsWith("/")) {
        path = path.replaceFirst("/", "");
      }
    }
    return path;
  }

  /**
   * @param dbPath
   *          the dbPath to set
   */
  public void setDatabasePath(String dbPath) {
    if (dbPath != null) {
      try {
        FileTools.directoryIsValid(dbPath, "Database path");
        dbPath = FileTools.multiplatformPath(dbPath);
        if (!this.useUserFolderConfiguration()) {
          if (dbPath.startsWith("..")) {
            this.relativeDatabasePath = true;
          } else {
            final String relativePath = makeRelative(dbPath);
            if (!relativePath.equals(dbPath)) {
              this.relativeDatabasePath = true;
              dbPath = relativePath;
            }
          }
        }
      } catch (Exception e) {
        this.databasePath = null;
        LOG.error(null, e);
        return;
      }
      this.databasePath = dbPath;
    }
  }

  /**
   * @param dirToIndex
   *          the directoryToIndex to set
   */
  public void setDirectoryToIndex(String dirToIndex) {
    if (dirToIndex != null) {
      try {
        FileTools.directoryIsValid(dirToIndex, "Directory to index");
        dirToIndex = FileTools.multiplatformPath(dirToIndex);

        if (!this.useUserFolderConfiguration() && this.relativeDatabasePath
            && !dirToIndex.startsWith("..")) {
          dirToIndex = makeRelative(dirToIndex);
        }

      } catch (Exception e) {
        this.directoryToIndex = null;
        LOG.error(null, e);
        return;
      }
      this.directoryToIndex = dirToIndex;
    }
  }

  /**
   *
   *
   */
  public void validateProperties() {
    this.validateProperties(getConfigMasterFilePath());
    this.setDatabasePath(this.databasePath);
    this.setDirectoryToIndex(this.directoryToIndex);
  }

  /**
   *
   *
   * @param configFilePath
   */
  public void validateProperties(final String configFilePath) {
    // TODO: check neccessity

    for (final ConfigString prop : ConfigString.values()) {
      try {
        if (!prop.toString().endsWith("_CMD_")) {
          this.getProp(configFilePath, prop);
        }
      } catch (Exception e) {
        this.logInvalidProperty(prop.toString(), configFilePath);
      }
    }

    for (final ConfigBool prop : ConfigBool.values()) {
      try {
        this.getProp(configFilePath, prop);
      } catch (Exception e) {
        this.logInvalidProperty(prop.toString(), configFilePath);
      }
    }

    for (final ConfigInteger prop : ConfigInteger.values()) {
      try {
        this.getProp(configFilePath, prop);
      } catch (Exception e) {
        this.logInvalidProperty(prop.toString(), configFilePath);
      }
    }
  }

  /**
   *
   *
   * @param property
   */
  private void logInvalidProperty(String property, String configPath) {
    if (this.notFoundProperties.containsKey(configPath)) {
      this.notFoundProperties.get(configPath).add(property);
    } else {
      final LinkedHashSet<String> props = new LinkedHashSet<String>();
      props.add(property);
      this.notFoundProperties.put(configPath, props);
    }
    // LOG.error("Can't find Property: \"" + property + "\"\nin config-file: \""
    // + configPath + "\"");
  }

  /**
   *
   *
   * @return
   */
  public String invalidConfigVersion() {
    return invalidConfigVersion(this.getConfigMasterFilePath());
  }

  /**
   *
   *
   * @return
   */
  public String invalidConfigVersion(String configFilePath) {
    final String configVersionProp = this.getProp(configFilePath, ConfigString.OCRAPTOR_VERSION);
    if (configVersionProp != null) {
      try {
        int configVersion = Integer.parseInt(configVersionProp.replace(".", ""));
        int binaryVersion = Integer.parseInt(HARDCODED_VERSION.replace(".", ""));
        if (configVersion == binaryVersion) {
          return null;
        } else {
          return "Binary version is: '" + HARDCODED_VERSION + "', config version is: '"
              + configVersionProp + "'";
        }
      } catch (Exception e) {
      }
    }
    throw new NullPointerException();
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   * @return the resetDatabase
   */
  public boolean resetDatabase() {
    return resetDatabase;
  }

  /**
   * @return the notFoundProperties
   */
  public Map<String, LinkedHashSet<String>> getNotFoundProperties() {
    return notFoundProperties;
  }

  /**
   * @return the clientDelayedShutdown
   */
  public boolean isClientDelayedShutdown() {
    return clientDelayedShutdown;
  }

  /**
   * @param clientDelayedShutdown
   *          the clientDelayedShutdown to set
   */
  public void setClientDelayedShutdown(boolean clientDelayedShutdown) {
    this.clientDelayedShutdown = clientDelayedShutdown;
  }

  /**
   * @return the relativeDatabasePath
   */
  public boolean hasRelativeDatabasePath() {
    return relativeDatabasePath;
  }

  /**
   * @return the basePath
   */
  public String getBasePath() {
    return basePath;
  }

  /**
   * @param resetDatabase
   *          the resetDatabase to set
   */
  public void setResetDatabase(boolean resetDatabase) {
    this.resetDatabase = resetDatabase;
  }

  /**
   * @return the waitForUserInput
   */
  public boolean waitForUserInput() {
    return waitForUserInput;
  }

  /**
   * @param waitForUserInput
   *          the waitForUserInput to set
   */
  public void setWaitForUserInput(boolean waitForUserInput) {
    this.waitForUserInput = waitForUserInput;
  }

  /**
   * @return the verbose
   */
  public boolean verbose() {
    return verbose;
  }

  /**
   * @param verbose
   *          the verbose to set
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * @return the quiet
   */
  public boolean quietMode() {
    return quiet;
  }

  /**
   * @param quiet
   *          the quiet to set
   */
  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  /**
   * @return the showProgress
   */
  public boolean showProgress() {
    return showProgress;
  }

  /**
   *
   *
   * @return
   */
  public boolean useUserFolderConfiguration() {
    return copyToUserFolder;
  }

  /**
   * @param showProgress
   *          the showProgress to set
   */
  public void setShowProgress(boolean showProgress) {
    this.showProgress = showProgress;
  }

  /**
   * @return the dbPath
   */
  public String getDatabasePath() {
    return databasePath;
  }

  /**
   * @return the directoryToIndex
   */
  public String getDirectoryToIndex() {
    return directoryToIndex;
  }

  /**
   * @return the searchString
   */
  public String getSearchString() {
    return searchString;
  }

  /**
   * @param searchString
   *          the searchString to set
   */
  public void setSearchString(String searchString) {
    this.searchString = searchString;
  }

  /**
   * @return the usedOs
   */
  public Os getUsedOs() {
    return usedOs;
  }

  /**
   *
   *
   * @return
   */
  public boolean useBuildInJRE() {
    return builtInJRE;
  }
}
