package mj;

import java.io.File;

import mj.configuration.Config;

import org.apache.commons.io.FileUtils;

public class Main {

  /**
   *
   *
   * @param args
   *
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    // PDFParser.convertPostScriptToPDF(new File("/home/foo/a/code/big_bang/ocraptor/res/misc/test-files/huckleberry.ps"));
    // System.exit(0);

    // String test = "shit bla\nFuck\n\n \n\n \nBla";
    // System.out.println(StringTools.removeRareCharacters(test));
    // System.exit(0);

    // MANUAL TEST {{{
    if (Config.devMode()) {
      System.out.println("!!!DEV MODE!!!");

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //

      boolean commandLineTest = true;

      // @formatter:off

      boolean
        resetDatabase    = true,
        showUserDialog   = false,
        verbose          = true,
        quiet            = false,
        showProgress     = false,
        userFolderConfig = true,
        useBuiltInJRE    = false,

        // CLI-only parameter:
        showHelp         = false,
        showExtendedHelp = false,
        showGUI          = true
      ;

      String
        // configFilePath      = Config.getMasterConfigFilePath(),
        configFilePath      = null,
        indexDirectory      = Config.getTestFolderPath(),
        // indexDirectory      = null,
        databaseDirectory   = FileUtils.getTempDirectoryPath() +
                              File.separator + Config.APP_NAME
      ;

      String[] testStrings = {
        // [0]
        "F< \"Discover Moses\" "   +
        "OR \"Chief\" "           +
        "OR \"Benecol Joghurt\" " +
        "OR \"PERSONS\" "         +
        "OR \"Langhorne\" "       +
        "OR \"Huckleberry\" "     +
        "OR \"Adventures\">"      ,
        // [1]
        "C<учреждении>",
        // [2]
        "C<huckleberry>",
        // [3]
        "C<persons>",
        // [4]
        "C<Ümlautü>"
      };
      // @formatter:on

      String testSearchString = null;
      testSearchString = testStrings[4];

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //

      File db = new File(databaseDirectory);
      if (!db.exists()) {
        db.mkdir();
      }

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //

      // @formatter:off
      args = new String[] {
          // ------------------------------------------------ //
            databaseDirectory != null ?  "-d" : ""
          , databaseDirectory != null ?  databaseDirectory : ""
          // ------------------------------------------------ //
          , indexDirectory    != null ?  "-i" : ""
          , indexDirectory    != null ?  indexDirectory    : ""
          // ------------------------------------------------ //
          , configFilePath    != null ?  "-c" : ""
          , configFilePath    != null ?  configFilePath    : ""
          // ------------------------------------------------ //
          , testSearchString  != null ?  "-l" : ""
          , testSearchString  != null ?  testSearchString  : ""
          // ------------------------------------------------ //
          , quiet                     ?  "-q" : ""
          // ------------------------------------------------ //
          , verbose                   ?  "-v" : ""
          // ------------------------------------------------ //
          , showUserDialog            ?  "-s" : ""
          // ------------------------------------------------ //
          , showHelp                  ?  "-h" : ""
          // ------------------------------------------------ //
          , showExtendedHelp          ?  "-H" : ""
          // ------------------------------------------------ //
          , resetDatabase             ?  "-r" : ""
          // ------------------------------------------------ //
          , userFolderConfig          ?  "-u" : ""
          // ------------------------------------------------ //
          , useBuiltInJRE             ?  "-b" : ""
          // ------------------------------------------------ //
          , showProgress              ?  "-p" : ""
          // ------------------------------------------------ //
          , showGUI                   ?  "-g" : ""
          // ------------------------------------------------ //
      };
      // @formatter:on

      if (!commandLineTest) {
        Config cfg = Config.init(
            // @formatter:off
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
            // @formatter:on
        MainController.init(cfg);
        return;
      }
    }

    MainController.init(args);
    // }}}
  }
}
