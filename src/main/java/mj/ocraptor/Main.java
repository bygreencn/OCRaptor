package mj.ocraptor;

import java.io.File;
import java.util.Set;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.console.AnsiColor;
import mj.ocraptor.console.COF;
import mj.ocraptor.database.error.DBPathNotFoundException;
import mj.ocraptor.tools.St;

import org.apache.commons.io.FileUtils;

public class Main {

  // ------------------------------------------------ //

  /**
   *
   *
   * @param args
   *
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    final boolean devMode = Config.devMode();

    if (devMode) {
      args = Main.testScenario(args);
    }

    if (args != null) {
      MainController.init(args);
    }

    if (devMode) {
      // Main.printAliveThreads();
    }

    COF.printExit();
  }

  // ------------------------------------------------ //

  /**
   *
   *
   *
   * @param args
   */
  private static void printArguments(String[] args) {
    String color = AnsiColor.GREEN_BACKGROUND.toString() + AnsiColor.WHITE.toString();
    COF.printLinesStretched(color + AnsiColor.BOLD + " \ncommandline arguments:\n ");
    COF.printTextStretched(color + St.arrayToString(args, "  ", true) + "\n ");
    COF.printSeparator();
    System.out.println();
  }

  // ------------------------------------------------ //

  /**
   * Testing stuff.
   *
   * @param args
   */
  private static String[] testScenario(String[] args) throws Exception {

    // ------------------------------------------------ //
    COF.printFilledLine(Config.APP_NAME + " - DEV MODE", true);

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //

    boolean commandLineTest = true;

    // *INDENT-OFF*
      // ------------------------------------------------ //
      boolean
      resetDatabase    = false,
      showUserDialog   = false,
      verbose          = true,
      quiet            = false,
      showProgress     = true,
      userFolderConfig = false, // found in user home dir
      useBuiltInJRE    = true,
      // CLI-only parameter:
      showHelp         = false,
      showExtendedHelp = false,
      showGUI          = true;
      // ------------------------------------------------ //
      String configFilePath = null;
             configFilePath = "cmd.properties";
      // ------------------------------------------------ //
      String indexDirectory = null;
             indexDirectory = "/home/foo/a/notes";
             //indexDirectory = Config.getTestFolderPath();
             // indexDirectory = Config.getTestFolderPath()
             //                  + "/single";
             // indexDirectory = "../../test";
      // ------------------------------------------------ //
      String databaseDirectory = null;
             // databaseDirectory = "../../testdb";
             databaseDirectory =
               FileUtils.getTempDirectoryPath() +
               File.separator +
               Config.APP_NAME_LOWER + "-db";
      // ------------------------------------------------ //
      String[] testStrings = {
        // [0]
        "\"Discover Moses\" "     +
        "OR \"Chief\" "           +
        "OR \"Benecol Joghurt\" " +
        "OR \"PERSONS\" "         +
        "OR \"Langhorne\" "       +
        "OR \"Huckleberry\" "     +
        "OR \"Adventures\""       ,
        // [1]
        "учреждении>",
        // [2]
        "huckleberry",
        // [3]
        "persons",
        // [4]
        "gustav",
        // [5]
        "\"\"trees, and, sure enough\"\"",
        // [6]
        "P.S.",
        // [7]
        "\"attempting to find a motive in this narrative will be\"",
        // [8]
        "\"\"lucene error\"",
        // [9]
        "txt",
      };
      // ------------------------------------------------ //
      String testSearchString = null;
      testSearchString = testStrings[8];
      // ------------------------------------------------ //
      // *INDENT-ON*

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //

    if (databaseDirectory != null) {
      File db = new File(databaseDirectory);
      if (!db.exists()) {
        db.mkdir();
      }
    }

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //

    // *INDENT-OFF*
      args = new String[] {
        // ------------------------------------------------ //
          databaseDirectory != null ?  "--db-directory"  : ""
        , databaseDirectory != null ?  databaseDirectory : ""
        // ------------------------------------------------ //
        , indexDirectory    != null ?  "--index" : ""
        , indexDirectory    != null ?  indexDirectory    : ""
        // ------------------------------------------------ //
        , configFilePath    != null ?  "--config-file"   : ""
        , configFilePath    != null ?  configFilePath    : ""
        // ------------------------------------------------ //
        , testSearchString  != null ?  "--find"          : ""
        , testSearchString  != null ?  testSearchString  : ""
        // ------------------------------------------------ //
        , quiet                     ?  "--quiet"         : ""
        // ------------------------------------------------ //
        , verbose                   ?  "--verbose"       : ""
        // ------------------------------------------------ //
        , showUserDialog            ?  "--show-dialog"   : ""
        // ------------------------------------------------ //
        , showHelp                  ?  "--help"          : ""
        // ------------------------------------------------ //
        , showExtendedHelp          ?  "--extended-help" : ""
        // ------------------------------------------------ //
        , resetDatabase             ?  "--reset-db"      : ""
        // ------------------------------------------------ //
        , userFolderConfig          ?  "--userfolder"    : ""
        // ------------------------------------------------ //
        , useBuiltInJRE             ?  "--build-in-jre"  : ""
        // ------------------------------------------------ //
        , showProgress              ?  "--progress"      : ""
        // ------------------------------------------------ //
        , showGUI                   ?  "--gui"           : ""
        // ------------------------------------------------ //
      };
      // *INDENT-ON*

    if (!commandLineTest) {
      // *INDENT-OFF*
        Config cfg = Config.init(
           resetDatabase,
           showUserDialog,
           verbose,
           quiet,
           showProgress,
           userFolderConfig,
           configFilePath,
           databaseDirectory,
           indexDirectory,
           testSearchString);
        // *INDENT-ON*

      MainController.init(cfg);
      args = null;
    }

    if (args != null) {
      Main.printArguments(args);
    }
    return args;
  }

  // ------------------------------------------------ //

  /**
   *
   *
   */
  private static void printAliveThreads() {
    final String coloring = AnsiColor.GREEN_BACKGROUND.toString() + AnsiColor.WHITE.toString();
    COF.printSeparator();
    COF.printTextStretched(coloring + "Threads that are still alive:\n ");
    final Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    final String currentThreadName = Thread.currentThread().getName();
    for (final Thread th : threadSet) {
      if (th.isAlive() && !th.isInterrupted()) {
        COF.printTextStretched(coloring
            + (currentThreadName.equals(th.getName()) ? AnsiColor.BOLD.toString() : "")
            + "Thread: " + th.getName());
      }
    }
  }

  // ------------------------------------------------ //
}
