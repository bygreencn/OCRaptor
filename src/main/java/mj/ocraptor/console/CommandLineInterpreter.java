package mj.ocraptor.console;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineInterpreter {

  private static CommandLineInterpreter instance;

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private static final String //
    // *INDENT-OFF*
    HELP_OPTION                 = "help",
    HELP_OPTION_SHORT           = "h",
    HELP_DESCRIPTION            = "Shows this infopage.",

    HELP_EXTENDED_OPTION        = "extended-help",
    HELP_EXTENDED_OPTION_SHORT  = "H",
    HELP_EXTENDED_DESCRIPTION   = "Shows a detailed infopage.",

    GUI_OPTION                  = "gui",
    GUI_OPTION_SHORT            = "g",
    GUI_DESCRIPTION             = "Show GUI-Version.",

    CONFIG_OPTION               = "config-file",
    CONFIG_OPTION_SHORT         = "c",
    CONFIG_ARGUMENT             = "FILE",
    CONFIG_DESCRIPTION          = "Path to your configuration file.",

    DB_DIR_OPTION               = "db-directory",
    DB_DIR_OPTION_SHORT         = "d",
    DB_DIR_ARGUMENT             = "DIR",
    DB_DIR_DESCRIPTION          = "Path to your database directory [REQUIRED]",

    INDEX_DIR_OPTION            = "index",
    INDEX_DIR_OPTION_SHORT      = "i",
    INDEX_DIR_ARGUMENT          = "DIR",
    INDEX_DIR_DESCRIPTION       = "Path to the directory you want to index",

    FIND_STRINGS_OPTION         = "find",
    FIND_STRINGS_OPTION_SHORT   = "f",
    FIND_STRINGS_ARGUMENT       = "STRING",
    FIND_STRINGS_DESCRIPTION    = "Search database for given string",

    PROGRESS_OPTION             = "progress",
    PROGRESS_OPTION_SHORT       = "p",
    PROGRESS_DESCRIPTION        = "Count files and show a progress-bar (takes longer).",

    USERFOLDER_OPTION           = "userfolder",
    USERFOLDER_OPTION_SHORT     = "u",
    USERFOLDER_DESCRIPTION      = "Copy config-files to user-folder.",

    JRE_OPTION                  = "build-in-jre",
    JRE_OPTION_SHORT            = "b",
    JRE_DESCRIPTION             = "Use build-in JRE.",

    VERBOSE_OPTION              = "verbose",
    VERBOSE_OPTION_SHORT        = "v",
    VERBOSE_DESCRIPTION         = "Show more progress-information",

    QUIET_OPTION                = "quiet",
    QUIET_OPTION_SHORT          = "q",
    QUIET_DESCRIPTION           = "Suppress any output.",

    RESET_OPTION                = "reset-db",
    RESET_OPTION_SHORT          = "r",
    RESET_DESCRIPTION           = "Reset given database",

    SHOW_OPTION                 = "show-dialog",
    SHOW_OPTION_SHORT           = "s",
    SHOW_DESCRIPTION            = "Show open-file dialog";
  // *INDENT-ON*

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private static final String //
  NOT_INITIALIZED = "CommandLineInterpreter is not initialized";

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private Options options;

  // parsed strings
  private String configFilePath, dbDirectoryPath, indexDirectoryPath, searchString;

  // @formatter:off
  private boolean resetDB, showDialog, progressBar, verbose, quiet, gui, buildInJRE,
      copyToUserFolder;

  // @formatter:on

  /**
   *
   *
   * @return
   */
  public static CommandLineInterpreter instance() {
    if (instance == null) {
      instance = new CommandLineInterpreter();
    }
    return instance;
  }

  /**
   *
   */
  private CommandLineInterpreter() {
  }

  /**
   *
   *
   * @return
   */
  @SuppressWarnings("static-access")
  private void initOptions() {
    options = new Options();
    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    options.addOption(HELP_OPTION_SHORT, HELP_OPTION, false, HELP_DESCRIPTION);
    options.addOption(HELP_EXTENDED_OPTION_SHORT, HELP_EXTENDED_OPTION, false,
        HELP_EXTENDED_DESCRIPTION);
    options.addOption(GUI_OPTION_SHORT, GUI_OPTION, false, GUI_DESCRIPTION);

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    options.addOption(RESET_OPTION_SHORT, RESET_OPTION, false, RESET_DESCRIPTION);
    options.addOption(SHOW_OPTION_SHORT, SHOW_OPTION, false, SHOW_DESCRIPTION);
    options.addOption(VERBOSE_OPTION_SHORT, VERBOSE_OPTION, false, VERBOSE_DESCRIPTION);
    options.addOption(PROGRESS_OPTION_SHORT, PROGRESS_OPTION, false, PROGRESS_DESCRIPTION);
    options.addOption(USERFOLDER_OPTION_SHORT, USERFOLDER_OPTION, false, USERFOLDER_DESCRIPTION);
    options.addOption(JRE_OPTION_SHORT, JRE_OPTION, false, JRE_DESCRIPTION);
    options.addOption(QUIET_OPTION_SHORT, QUIET_OPTION, false, QUIET_DESCRIPTION);

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    Option configOption = OptionBuilder.withLongOpt(CONFIG_OPTION).withArgName(CONFIG_ARGUMENT)
        .withDescription(CONFIG_DESCRIPTION).hasArg().create(CONFIG_OPTION_SHORT);
    Option indexOption = OptionBuilder.withLongOpt(INDEX_DIR_OPTION)
        .withArgName(INDEX_DIR_ARGUMENT).withDescription(INDEX_DIR_DESCRIPTION).hasArg().create(
            INDEX_DIR_OPTION_SHORT);
    Option findOption = OptionBuilder.withLongOpt(FIND_STRINGS_OPTION).withArgName(
        FIND_STRINGS_ARGUMENT).withDescription(FIND_STRINGS_DESCRIPTION).hasArg().create(
        FIND_STRINGS_OPTION_SHORT);

    Option dbDirOption = OptionBuilder.withLongOpt(DB_DIR_OPTION).withArgName(DB_DIR_ARGUMENT)
        .withDescription(DB_DIR_DESCRIPTION).hasArg().create(DB_DIR_OPTION_SHORT);

    dbDirOption.setRequired(!this.gui);

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    options.addOption(configOption).addOption(dbDirOption).addOption(findOption).addOption(
        indexOption);
  }

  /**
   *
   *
   * @param args
   * @return
   */
  public CommandLineInterpreter parseArguments(String[] args) {
    if (instance == null)
      throw new NullPointerException(NOT_INITIALIZED);

    // ------------------------------------------------ //
    // -- manual search for help-arguments
    // ------------------------------------------------ //

    initOptions();
    for (String arg : args) {
      String cleanedArg = arg.trim().replaceFirst("[-]+", "");
      if (cleanedArg.equals(HELP_EXTENDED_OPTION_SHORT) || cleanedArg.equals(HELP_EXTENDED_OPTION)) {
        COF.printCLIExtendedHelp();
      }
      if (cleanedArg.equals(HELP_OPTION_SHORT) || cleanedArg.equals(HELP_OPTION)) {
        COF.printCLIHelp();
      }
      if (cleanedArg.equals(GUI_OPTION_SHORT) || cleanedArg.equals(GUI_OPTION)) {
        this.gui = true;
      }
    }
    initOptions();

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    CommandLineParser parser = new BasicParser();
    try {
      CommandLine line = parser.parse(options, args);
      if (line.hasOption(CONFIG_OPTION_SHORT)) {
        this.configFilePath = line.getOptionValue(CONFIG_OPTION_SHORT);
      }

      if (line.hasOption(DB_DIR_OPTION_SHORT)) {
        this.dbDirectoryPath = line.getOptionValue(DB_DIR_OPTION_SHORT);
      }

      if (line.hasOption(VERBOSE_OPTION_SHORT)) {
        this.verbose = true;
      }

      if (line.hasOption(PROGRESS_OPTION_SHORT)) {
        this.progressBar = true;
      }

      if (line.hasOption(USERFOLDER_OPTION_SHORT)) {
        this.copyToUserFolder = true;
      }

      if (line.hasOption(JRE_OPTION_SHORT)) {
        this.buildInJRE = true;
      }

      if (line.hasOption(QUIET_OPTION_SHORT)) {
        this.quiet = true;
      }

      if (line.hasOption(INDEX_DIR_OPTION_SHORT)) {
        this.indexDirectoryPath = line.getOptionValue(INDEX_DIR_OPTION_SHORT);
        if (line.hasOption(RESET_OPTION_SHORT)) {
          this.resetDB = true;
        }
      }

      if (line.hasOption(FIND_STRINGS_OPTION_SHORT)) {
        this.searchString = line.getOptionValue(FIND_STRINGS_OPTION_SHORT);

        if (line.hasOption(SHOW_OPTION_SHORT)) {
          this.showDialog = true;
        }
      }
    } catch (ParseException e) {
      COF.printCLIHelp(e.getMessage());
    }
    return this;
  }

  /**
   * @return the configFilePath
   */
  public String getUserConfigFilePath() {
    return configFilePath;
  }

  /**
   * @return the dbDirectoryPath
   */
  public String getDbDirectoryPath() {
    return dbDirectoryPath;
  }

  /**
   * @return the indexDirectoryPath
   */
  public String getIndexDirectoryPath() {
    return indexDirectoryPath;
  }

  /**
   * @return the searchString
   */
  public String getSearchString() {
    return searchString;
  }

  /**
   * @return the gui
   */
  public boolean isGui() {
    return gui;
  }

  /**
   * @return the copyToUserFolder
   */
  public boolean useUserFolderConfiguration() {
    return copyToUserFolder;
  }

  /**
   *
   *
   * @return
   */
  public boolean useBuiltInJRE() {
    return buildInJRE;
  }

  /**
   * /**
   *
   * @return the progressBar
   */
  public boolean showProgressBar() {
    return progressBar;
  }

  /**
   * @return the quiet
   */
  public boolean quiet() {
    return quiet;
  }

  /**
   * @return the verbose
   */
  public boolean verbose() {
    return verbose;
  }

  /**
   * @return the resetDB
   */
  public boolean resetDB() {
    return resetDB;
  }

  /**
   * @return the showDialog
   */
  public boolean waitForUserInput() {
    return showDialog;
  }

  /**
   * @return the options
   */
  public Options getOptions() {
    return options;
  }
}
