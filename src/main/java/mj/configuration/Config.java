package mj.configuration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static mj.configuration.properties.ConfigString.AVAILABLE_LOCALES;
import static mj.configuration.properties.ConfigString.DATABASE_FOLDER;
import static mj.configuration.properties.ConfigString.FOLDERS_TO_INDEX;
import static mj.configuration.properties.ConfigString.OCRAPTOR_VERSION;
import static org.apache.commons.lang.StringUtils.repeat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import mj.configuration.properties.ConfigBool;
import mj.configuration.properties.ConfigInteger;
import mj.configuration.properties.ConfigString;
import mj.console.CommandLineInterpreter;
import mj.console.Platform;
import mj.console.Platform.Os;
import mj.events.EventManager;
import mj.file_handler.utils.FileTools;
import mj.tools.StringTools;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private static Config instance;
  private Os usedOs;
  private Properties userProperties;
  private File tikaMimeFile;

  // @formatter:off
  private boolean
      resetDatabase,
      waitForUserInput,
      verbose,
      quiet,
      showProgress,
      builtInJRE,
      copyToUserFolder,
      paused,
      shutdown
  ;
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  // @formatter:off
  private static final String
      FILES_FOLDER            = "res",
      USER_FOLDER             = "usr",
      LIBRARY_FOLDER          = "lib",
      TESS4J_FOLDER           = "tess",
      TESS4J_LANGUAGE_FOLDER  = "tessdata",
      TESS4J_WRAPPER_FOLDER   = "bins",
      TESS4J_NATIVE_FOLDER    = "tess4j-natives",
      TESS4J_WRAPPER_FILE     = "tess4j-wrapper.jar",
      TIKA_FOLDER             = "tika",
      CONFIG_FOLDER           = "cnfg",
      PROFILE_FOLDER_NAME     = "lang",

      TIKA_MIMETYPES          = "tika-mimetypes.xml",
      TIKA_PDF_PROPERTIES     = "pdf-parser.properties",
      MAIN_CONFIG_FILE        = "default.properties",

      LOG_FOLDER              = "log",
      MAIN_LOG_FILE           = "indexer.log",
      TESS_NATIVE_LOG_FILE    = "tesseract-2.log",

      HELP_FOLDER             = "help",
      HELP_HTML               = "help_{0}.html",

      TESS_NATIVE_LIN_64      = "lin-x86-64",
      TESS_NATIVE_OSX_64      = "osx-x86-64",
      TESS_NATIVE_WIN_32      = "win-x86-32",
      TESS_NATIVE_WIN_64      = "win-x86-64",

      HARDCODED_VERSION       = "0.7.1"
  ;
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  // @formatter:off
  private String
    userConfigFilePath,
    databasePath,
    directoryToIndex,
    searchString
  ;

  public static final String
    APP_NAME                      = "OCRaptor",
    SEARCH_DELIMITER_START_SINGLE = "<",
    SEARCH_DELIMITER_END_SINGLE   = ">",
    SEARCH_DELIMITER_START        = repeat(SEARCH_DELIMITER_START_SINGLE, 4),
    SEARCH_DELIMITER_END          = repeat(SEARCH_DELIMITER_END_SINGLE, 4)
  ;
  // @formatter:on

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

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
   * @param cli
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
   * @param update
   * @param waitForUserInput
   * @param verbose
   * @param quiet
   * @param showProgress
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
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param cli
   * @throws IOException
   */
  private Config(CommandLineInterpreter cli, boolean gui) throws IOException {
    if (!gui) {
      this.resetDatabase = cli.resetDB();
      this.waitForUserInput = cli.waitForUserInput();
      this.verbose = cli.verbose();
      this.quiet = cli.quiet();
      this.showProgress = cli.showProgressBar();
      this.setUserConfigFilePath(cli.getUserConfigFilePath());
      this.databasePath = cli.getDbDirectoryPath();
      this.directoryToIndex = cli.getIndexDirectoryPath();
      this.searchString = cli.getSearchString();
    } else {
      // using default config file path
      this.setUserConfigFilePath(null);
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
      boolean showProgress, boolean userUserFolder, String configFilePath, String dbPath,
      String directoryToIndex, String searchString) throws IOException {
    this.resetDatabase = reset;
    this.waitForUserInput = waitForUserInput;
    this.verbose = verbose;
    this.quiet = quiet;
    this.showProgress = showProgress;
    this.copyToUserFolder = userUserFolder;
    this.setUserConfigFilePath(configFilePath);
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
      FileTools.directoryIsValidAndNotNull(userHome, true, "home directory");
      File newConfigDir = null;
      if (Platform.getSystem() == Os.LINUX || Platform.getSystem() == Os.OSX) {
        newConfigDir = new File(userHome, "." + APP_NAME.toLowerCase());
      } else if (Platform.getSystem() == Os.WINDOWS) {
        newConfigDir = new File(userHome, APP_NAME);
      }
      return newConfigDir;
    } catch (FileNotFoundException e) {
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
      setUserConfigFilePath(this.getDefaultConfigFilePath());
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
    return getFileFolder() + File.separator + "misc/test-files";
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

  public static final String TARGET_FOLDER = "target";

  /**
   *
   *
   * @return
   */
  public static boolean devMode() {
    if (new File(TARGET_FOLDER + File.separator + LIBRARY_FOLDER).exists()) {
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
  public String getMainLogFile() {
    return getMainLogFolderPath() + File.separator + MAIN_LOG_FILE;
  }

  /**
   *
   *
   * @return
   */
  public String getDefaultConfigFilePath() {
    return getConfigFolder() + File.separator + MAIN_CONFIG_FILE;
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
  public String getConfigFolder() {
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
        return StringTools.getFileNameWithoutExtension(temp);
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

    String version = this.getProp(OCRAPTOR_VERSION);
    if (!version.trim().equals(HARDCODED_VERSION)) {
      LOG.error("Possible version conflict\nApplication-version: \"" + HARDCODED_VERSION
          + "\"\nConfig-version: \"" + version + "\"");
    }
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
      if (file.equals(this.getUserConfigFilePath())) {
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
    return getProp(this.getUserConfigFilePath(), boolProperty);
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
    LOG.error("Can't find Property: \n\"" + boolProperty.name() + "\"\nin config-file:\n\""
        + configFilePath + "\"\nPossible version mismatch?");
    throw new NullPointerException();
  }

  /**
   *
   *
   * @param intProperty
   * @return
   */
  public int getProp(ConfigInteger intProperty) {
    return getProp(this.getUserConfigFilePath(), intProperty);
  }

  /**
   *
   *
   * @param intProperty
   * @return
   */
  public int getProp(String configFilePath, ConfigInteger intProperty) {
    Properties prop = getPropertiesFromFile(configFilePath);
    if (intProperty != null) {
      String par = prop.getProperty(intProperty.name().trim());
      if (par != null)
        if (!par.trim().isEmpty()) {
          try {
            return Integer.parseInt(par.trim());
          } catch (NumberFormatException e) {
            return -1;
          }
        } else {
          return -1;
        }
    }
    throw new NullPointerException();
  }

  /**
   *
   *
   * @param stringProperty
   * @return
   */
  public String getProp(ConfigString stringProperty) {
    return getProp(this.getUserConfigFilePath(), stringProperty);
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
    throw new NullPointerException("Property missing: " + stringProperty);
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

  public static final int MAX_FILE_LENGTH = 50;

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

      if (!StringTools.isValidFileName(name, MAX_FILE_LENGTH)) {
        EventManager.instance().configFileNameInvalid();
      } else if (alreadyIn) {
        EventManager.instance().propertiesFileAlreadyExists();
      } else {
        File newConfigPath = new File(getUserFolder() + File.separator + name
            + PROPERTIES_EXTENSION);
        try {
          FileUtils.copyFile(new File(getDefaultConfigFilePath()), newConfigPath);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return newConfigPath;
      }
    }
    return null;
  }

  private static final String FILE_NOT_FOUND_ERROR = "Configuration file not found:";

  /**
   * @param configFilePath
   *          the configFilePath to set
   * @throws FileNotFoundException
   */
  public void setUserConfigFilePath(String configFilePath) throws FileNotFoundException {
    EventManager eventManager = EventManager.instance();
    File configFile = null;

    if (configFilePath != null) {
      configFile = new File(configFilePath);
      if (!configFile.exists() || !configFile.isFile() || !configFile.canWrite()) {
        eventManager.configFileNotFound(configFile);
      }
    } else {
      configFile = new File(getDefaultConfigFilePath());
      if (!configFile.exists() || !configFile.isFile() || !configFile.canWrite()) {
        // TODO: log
        throw new FileNotFoundException(FILE_NOT_FOUND_ERROR + " \n\""
            + configFile.getAbsolutePath() + "\"\n");
      }
    }

    this.userConfigFilePath = configFile.getAbsolutePath();

    try {
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
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param databaseFolder
   */
  public void saveDatabaseFolder(String configFilePath, String databaseFolder) {
    this.setProp(configFilePath, DATABASE_FOLDER.name(), databaseFolder);
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
    ArrayList<String> savedFolders = getFoldersToIndex();
    try {
      if (folder.exists() && folder.canRead()) {
        boolean alreadySaved = false;
        // TODO: relative path option
        for (String f : savedFolders) {
          File fileFromList = new File(f.trim());

          if (fileFromList.exists()
              && fileFromList.getAbsoluteFile().equals(folder.getAbsoluteFile())) {
            alreadySaved = true;
            break;
          }
        }

        if (!alreadySaved) {
          savedFolders.add(folder.getAbsolutePath());
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

  private static final String DB_DIR_NULL = //
  "DB DIRECTORY TO INDEX IS NULL";

  /**
   * @param dbPath
   *          the dbPath to set
   */
  public void setDatabasePath(String dbPath) {
    File dbDirectoryFile = null;
    if (dbPath != null && !dbPath.trim().isEmpty()) {
      dbDirectoryFile = new File(dbPath);
      if (!dbDirectoryFile.exists() || !dbDirectoryFile.isDirectory()) {
        // TODO:
        // cli.printHelp(DB_NOT_FOUND + ":\n\"" +
        // dbDirectoryFile.getAbsolutePath() + "\"");
      }
      if (!dbDirectoryFile.canWrite()) {
        // TODO:
        // cli.printHelp(DB_PERMISSION_ERROR + ":\n\"" +
        // dbDirectoryFile.getAbsolutePath() + "\"");
      }
    } else {
      throw new NullPointerException(DB_DIR_NULL);
    }
    this.databasePath = dbPath;
  }

  /**
   * @param directoryToIndex
   *          the directoryToIndex to set
   */
  public void setDirectoryToIndex(String directoryToIndex) {
    File indexDirectoryFile = null;
    if (directoryToIndex != null && !directoryToIndex.trim().isEmpty()) {
      indexDirectoryFile = new File(directoryToIndex);
      if (!indexDirectoryFile.exists() || !indexDirectoryFile.isDirectory()) {
        // TODO:
        // cli.printHelp(INDEX_DIR_NOT_FOUND + ":\n\"" +
        // indexDirectoryFile.getAbsolutePath() + "\"");
      }

      if (!indexDirectoryFile.canRead()) {
        // TODO:
        // cli.printHelp(INDEX_PERMISSION_ERROR + ":\n\"" +
        // indexDirectoryFile.getAbsolutePath() + "\"");
      }
    }
    this.directoryToIndex = directoryToIndex;
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
   * @return the configFilePath
   */
  public String getUserConfigFilePath() {
    return userConfigFilePath;
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
    if (this.searchString != null && !this.searchString.trim().isEmpty()
        && !this.searchString.startsWith("F<") && !this.searchString.endsWith(">")) {
      this.searchString = "F<" + this.searchString + ">";
    }
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

  /**
   * @return the paused
   */
  public boolean isPaused() {
    return paused;
  }

  /**
   * @param paused
   *          the paused to set
   */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  /**
   * @return the shutdown
   */
  public boolean isShutdown() {
    return shutdown;
  }

  /**
   * @param shutdown
   *          the shutdown to set
   */
  public void setShutdown(boolean shutdown) {
    this.shutdown = shutdown;
  }

  /**
   *
   *
   * @return
   */
  public static String getVersion() {
    return HARDCODED_VERSION;
  }
}
