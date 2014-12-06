package mj;

import static mj.configuration.properties.ConfigBool.ALWAYS_REMOVE_MISSING_FILES_FROM_DB;
import static mj.configuration.properties.ConfigBool.DEBUG_MODE;
import static mj.configuration.properties.ConfigInteger.DIALOG_METADATA_MAX_STRING_LENGTH;
import static mj.configuration.properties.ConfigInteger.DIALOG_SNIPPET_MAX_STRING_LENGTH;
import static mj.configuration.properties.ConfigInteger.MAX_SEARCH_RESULTS;
import static mj.configuration.properties.ConfigInteger.NUMBER_OF_CPU_CORES_TO_USE;
import static mj.configuration.properties.ConfigString.DATABASE_NAME;
import static mj.console.ConsoleOutputFormatter.printEmptySeparator;
import static mj.console.ConsoleOutputFormatter.printEnd;
import static mj.console.ConsoleOutputFormatter.printLine;
import static mj.console.ConsoleOutputFormatter.printResult;
import static mj.console.ConsoleOutputFormatter.printScannerInfo;
import static mj.console.ConsoleOutputFormatter.printSeparator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import mj.configuration.Config;
import mj.console.AnsiColors;
import mj.console.CommandLineInterpreter;
import mj.console.ConsoleOutputFormatter;
import mj.database.DBManager;
import mj.database.SearchResult;
import mj.events.EventManager;
import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.FileEntryDao;
import mj.extraction.result.document.FullText;
import mj.extraction.result.document.MetaData;
import mj.file_handler.PausableExecutor;
import mj.file_handler.TextExtractor;
import mj.file_handler.TextExtractorThread;
import mj.file_handler.generator.SimpleListGenerator;
import mj.file_handler.utils.FileTools;
import mj.javafx.GUIController;
import mj.parser.ParseException;
import mj.parser.SearchStringParser;
import mj.parser.TokenMgrError;
import mj.tools.StringTools;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

/**
 *
 *
 * @author
 */
public class MainController {

  final long startTime = System.currentTimeMillis();

  private static MainController instance;
  private DBManager db;
  private SimpleListGenerator fileListGenerator;
  private int numThreads;
  private PausableExecutor executor;
  private Config cfg;
  private boolean checkedForMissingFiles;
  private int countProcessedFiles = 0;

  private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(getClass());

  // @formatter:off
  private static final String
      CONFIG_ERROR    = "CONFIG OBJECT NOT INITIALIZED!",
      DB_START        = "INITIALIZING EMBEDDED DATABASE",
      INDEX_START     = "STARTING INDEXING ... (this could take a while)",
      FILE_PATTERN    = "%n[%-5p]--%d{yyyy-MM-dd HH:mm:ss}--%c{1}%n[Thread:%t][%l]%n%m%n%n",
      KILL_MESSAGE    = "This takes longer than usual,\ndo you want to kill the application?"
  ;
  // @formatter:on

  /**
   *
   *
   * @return
   */
  public static MainController inst() {
    return instance;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param config
   * @return
   *
   * @throws IOException
   */
  public static MainController init(Config config) throws IOException {
    try {
      if (instance == null) {
        instance = new MainController();
      }

      instance.initConfiguration(config);
      instance.initLogger();
      instance.redirectStdErrors();
      instance.initMultiCoreProcessing();
      instance.initDatabase();

      Config.inst().prepareConfigurationDirectory();
      instance.start();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return instance;
  }

  /**
   *
   *
   * @param args
   * @return
   * @throws IOException
   */
  public static MainController init(String[] args) throws IOException {
    try {
      if (instance == null) {
        instance = new MainController();
      }

      CommandLineInterpreter cli = CommandLineInterpreter.instance();
      cli.parseArguments(args);

      if (cli.isGui()) {
        instance.initConfiguration(Config.initFromGui());
        instance.redirectStdErrors();
        instance.initMultiCoreProcessing();
        Config.inst().prepareConfigurationDirectory();
        instance.initLogger();
        instance.initGUI();
      } else {
        instance.initConfiguration(Config.initFromCLI());
        instance.redirectStdErrors();
        instance.initMultiCoreProcessing();
        instance.initDatabase();
        Config.inst().prepareConfigurationDirectory();
        instance.initLogger();

        if (Config.inst().getProp(ALWAYS_REMOVE_MISSING_FILES_FROM_DB)) {
          Config.inst().prepareConfigurationDirectory();
          instance.removeMissingFiles();
        }
        instance.start();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return instance;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   */
  private void initGUI() {
    // {{{
    Application.launch(GUIController.class, (java.lang.String[]) null);
    // }}}
  }

  /**
   *
   *
   * @return
   *
   * @throws IOException
   */
  private void initConfiguration(Config config) throws IOException {
    this.fileListGenerator = new SimpleListGenerator();
    this.numThreads = 1;
    this.cfg = config;
    ConsoleOutputFormatter.init(this.cfg.quietMode());
  }

  /**
   *
   *
   * @return
   */
  public void initMultiCoreProcessing() {
    // {{{
    Integer cores = cfg.getProp(NUMBER_OF_CPU_CORES_TO_USE);
    int availableCores = Runtime.getRuntime().availableProcessors();

    if (cores == null || cores >= availableCores || cores < 1) {
      this.numThreads = availableCores;
    } else {
      this.numThreads = cores;
    }

    if (cfg.getProp(DEBUG_MODE)) {
      printSeparator();
      printLine("[DEBUG] USING " + this.numThreads + " CPU CORE" + (cores > 1 ? "S" : ""));
    }
    // }}}
  }

  /**
   *
   *
   * @return
   * @throws FileNotFoundException
   */
  private void initLogger() throws FileNotFoundException {
    // {{{
    if (cfg == null)
      throw new NullPointerException(CONFIG_ERROR);

    // TODO: redirect java logging

    java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("");
    globalLogger.setUseParentHandlers(false);
    Handler[] handlers = globalLogger.getHandlers();
    for (Handler handler : handlers) {
      globalLogger.removeHandler(handler);
    }

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    FileAppender fileAppender = new FileAppender();

    // set directory
    File logFile = new File(this.cfg.getMainLogFile());
    FileTools.directoryIsValid(logFile.getParentFile(), false, "logfile-directory");
    fileAppender.setFile(logFile.getAbsolutePath());

    fileAppender.setLayout(new PatternLayout(FILE_PATTERN));
    fileAppender.setThreshold(Level.INFO);
    fileAppender.setAppend(true);
    fileAppender.activateOptions();

    // ------------------------------------------------ //
    // --
    // ------------------------------------------------ //
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();

    // TODO:
    logger.addAppender(fileAppender);

    EventManager.instance().initLoggerAppender(logger);
    // }}}
  }

  /**
   *
   *
   * @return
   */
  private void redirectStdErrors() {
    // OutputStream errStream = new OutputStream() {
    // @Override
    // public void write(int b) throws IOException {
    // //
    // }
    // };

    // PrintStream errPrint = new PrintStream(errStream) {
    // public void println(final String string) {
    // if (cfg.verbose())
    // System.out.println(string);
    // LOG.error(string);
    // }

    // public void print(final String string) {
    // if (cfg.verbose())
    // System.out.println(string);
    // LOG.error(string);
    // }
    // };

    // System.setErr(errPrint);

    // try {
    // System.setOut(new PrintStream(System.out, true, "UTF-8"));
    // } catch (UnsupportedEncodingException e) {
    // e.printStackTrace();
    // }
  }

  private ConcurrentHashMap<Long, File> currentFileWorkers;
  private boolean showInitialCPUList;

  /**
   *
   *
   * @return
   */
  public void removeMissingFiles() {
    this.checkedForMissingFiles = true;
    List<String> entries = null;
    Connection connection = null;
    try {
      FileEntryDao fileEntryDao = new FileEntryDao();
      connection = this.db.getConnection();
      entries = fileEntryDao.getAllFilePaths(connection);

      int countMissingFiles = 0;
      if (!entries.isEmpty()) {
        EventManager.instance().removingMissingFiles(null, entries.size());
        for (String filePath : entries) {
          File file = new File(filePath);

          if (!file.exists()) {
            fileEntryDao.removeByPath(filePath, connection);
            countMissingFiles++;
          }

          while (!Thread.currentThread().isInterrupted() && this.cfg.isPaused()) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          if (this.cfg.isShutdown()) {
            break;
          }
        }

        EventManager.instance().removingMissingFiles(countMissingFiles, entries.size());
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   *
   *
   */
  public synchronized void fileProcessed() {
    this.countProcessedFiles++;
    if (countProcessedFiles % 100 == 0) {
      System.gc();
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
  public void start() {
    // {{{

    if (cfg == null) {
      throw new NullPointerException();
    }

    if (cfg.isShutdown()) {
      return;
    }

    String directoryToIndex = this.cfg.getDirectoryToIndex();
    this.initDatabase();

    if (directoryToIndex != null) {

      if (!this.checkedForMissingFiles
          && Config.inst().getProp(ALWAYS_REMOVE_MISSING_FILES_FROM_DB)) {
        removeMissingFiles();
      }

      this.showInitialCPUList = true;
      this.currentFileWorkers = new ConcurrentHashMap<Long, File>();

      final ThreadFactory factory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable target) {
          final Thread thread = new Thread(target);
          LOG.debug("Creating new worker thread");
          thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
              LOG.error("Uncaught Exception", e);
            }
          });
          return thread;
        }
      };

      this.executor = new PausableExecutor(numThreads, factory);

      TextExtractorThread.resetCount();

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //
      TextExtractor handler = null;

      try {
        handler = new TextExtractor(db, executor);
        if (this.cfg.showProgress() || this.cfg.verbose()) {
          handler.countFilesOnly();
          fileListGenerator.generateFileList(directoryToIndex, handler);
          handler.stopCounting();
          long fileCount = handler.getFileCount();
          EventManager.instance().countingFiles(fileCount, true);
          TextExtractorThread.setFileCount(fileCount, cfg.verbose());
        }

        printLine(INDEX_START);
        printSeparator();
        printEmptySeparator();
        printSeparator();

        // start indexing
        fileListGenerator.generateFileList(directoryToIndex, handler);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // ------------------------------------------------ //
      // -- wait for threads to join
      // ------------------------------------------------ //

      executor.shutdown();
      while (executor.isTerminating()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      System.gc();

      // signal end
      EventManager.instance().printFolderFinished();
    } else {
      printSeparator();
    }

    String searchString = this.cfg.getSearchString();
    if (this.cfg.getSearchString() != null && searchString != null
        && !searchString.trim().isEmpty()) {
      this.searchDatabase(searchString);
    }
    // }}}
  }

  /**
   *
   *
   * @return
   */
  public DBManager initDatabase() {
    if (this.db == null) {
      try {
        printSeparator();
        printLine(DB_START);
        String database = this.cfg.getProp(DATABASE_NAME), //
        databaseDir = this.cfg.getDatabasePath();
        this.db = new DBManager(database, databaseDir).init(this.cfg.resetDatabase());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return this.db;
  }

  /**
   *
   *
   * @param searchString
   */
  public void searchDatabase(String searchString) {
    final EventManager echo = EventManager.instance();

    if (searchString != null && !searchString.trim().isEmpty()) {
      searchString = StringTools.normalizeDocumentText(searchString);
      echo.searchProgressIndicator(0.1, "Preparing search");

      this.initDatabase();
      echo.searchProgressIndicator(0.2);

      // @formatter:off
      final int maxSearchResults  = this.cfg.getProp(MAX_SEARCH_RESULTS);
      final int maxMetadataLength = this.cfg.getProp(DIALOG_METADATA_MAX_STRING_LENGTH);
      final int maxSnippetLength  = this.cfg.getProp(DIALOG_SNIPPET_MAX_STRING_LENGTH);
      // @formatter:on

      // ------------------------------------------------ //

      SearchStringParser parser = null;
      try {
        parser = new SearchStringParser(new StringReader(searchString));
        parser.parse();
      } catch (TokenMgrError e) {
        LOG.error(e.getMessage());
      } catch (ParseException e) {
        LOG.error(e.getMessage());
      }

      // TODO: join
      // Operator to join metadata and textsearch
      // Operator operator = parser.getOperator();
      // Boolean metaDataComesFirst = parser.metaDataFirst();

      String luceneContent = parser.getLuceneContentToken();
      String luceneMetaData = parser.getLuceneMetaDataToken();

      // ------------------------------------------------ //

      echo.searchProgressIndicator(0.5);
      Connection connection = null;
      SearchResult resultToDisplay = null;
      try {
        if (luceneContent != null || luceneMetaData != null) {
          // ------------------------------------------------ //
          connection = db.getConnection();
          echo.searchProgressIndicator(0.7, "Searching in database");
          if (luceneContent != null) {
            resultToDisplay = db.searchIndex(luceneContent + " AND INDEX:TX", maxSearchResults,
                connection);
          } else if (luceneMetaData != null) {
            resultToDisplay = db.searchIndex(luceneMetaData + " AND INDEX:MD", maxSearchResults,
                connection);
          } else {
            return;
          }
          // ------------------------------------------------ //

          FileEntryDao fileEntryDao = new FileEntryDao();
          echo.searchProgressIndicator(0.8, "Preparing result");

          HashMap<FileEntry, Double> fileEntriesInResult = new HashMap<FileEntry, Double>();
          List<FileEntry> joinedFileList = new ArrayList<FileEntry>();

          if (resultToDisplay != null && resultToDisplay.getElements() != null) {
            Iterator<Map.Entry<FullText, Double>> elIterator = resultToDisplay.getElements()
                .iterator();

            while (elIterator.hasNext()) {
              Map.Entry<FullText, Double> entry = elIterator.next();
              FullText element = entry.getKey();
              Double score = entry.getValue();
              FileEntry fileEntry = fileEntryDao.findById(element.getFileId(), connection);
              fileEntry.addFullText(element);
              fileEntriesInResult.put(fileEntry, score);

              if (fileEntry != null && !joinedFileList.contains(fileEntry)) {
                joinedFileList.add(fileEntry);
              }
            }
          }
          // ------------------------------------------------ //
          echo.searchProgressIndicator(0.85);
          if (resultToDisplay != null && resultToDisplay.getMetadata() != null) {
            Iterator<Map.Entry<MetaData, Double>> elIterator = resultToDisplay.getMetadata()
                .iterator();
            while (elIterator.hasNext()) {
              Map.Entry<MetaData, Double> entry = elIterator.next();
              MetaData md = entry.getKey();
              Double score = entry.getValue();
              FileEntry fileEntry = fileEntryDao.findById(md.getFileId(), connection);

              if (fileEntry != null) {
                if (joinedFileList.contains(fileEntry)) {
                  fileEntry = joinedFileList.get(joinedFileList.indexOf(fileEntry));
                  fileEntry.addMetaData(md);
                  score += fileEntriesInResult.get(fileEntry);
                } else {
                  fileEntry.addMetaData(md);
                  joinedFileList.add(fileEntry);
                }
                fileEntriesInResult.put(fileEntry, score);
              }
            }
          }
          // ------------------------------------------------ //

          resultToDisplay.setFileEntries(fileEntriesInResult);
          echo.searchProgressIndicator(0.95, "Rendering");
          echo.searchProgressIndicator(-1);

          // TODO:
          // System.out.println(fileEntriesInResult);
          // this.db.getH2DB().printTables();
        }
      } catch (Exception e) {
        // TODO: logging
        e.printStackTrace();
      } finally {
        if (connection != null) {
          try {
            connection.close();
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      }

      echo.printResult(resultToDisplay, luceneMetaData, luceneContent, null, maxMetadataLength,
          maxSnippetLength);

      scanForUserInput(resultToDisplay, luceneMetaData, luceneContent, maxMetadataLength,
          maxSnippetLength);

      printEnd();
    }
  }

  private static final String //
      SCAN_QUIT = "q",//
      SCAN_DETAILS = "s", //
      SCAN_DIR = "d";

  public static final int WAIT_FOR_STDERROR_IN_SEC = 3;

  /**
   *
   *
   * @param searchParser
   * @param result
   */
  private void scanForUserInput(final SearchResult results, String metaDataSearch,
      String contentSearch, int maxMetadataLength, int maxSnippetLength) {
    // {{{
    if (this.cfg.waitForUserInput() && GUIController.instance() == null
        && !results.getFileEntries().isEmpty()) {

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      printScannerInfo();
      while (!Thread.currentThread().isInterrupted()) {
        String line = "";
        try {
          line = in.readLine();
          if (line != null && !line.trim().isEmpty()) {
            line = line.trim().toLowerCase();
            Integer id = null;

            if (line.equals(SCAN_QUIT)) {
              System.out.println();
              return;
            }

            boolean openDirectory = false;
            boolean showDetails = false;

            if (line.startsWith(SCAN_DETAILS)) {
              id = Integer.parseInt(line.replaceFirst(SCAN_DETAILS, ""));
              showDetails = true;
            } else if (line.startsWith(SCAN_DIR)) {
              id = Integer.parseInt(line.replaceFirst(SCAN_DIR, ""));
              openDirectory = true;
            } else {
              id = Integer.parseInt(line);
            }

            SortedSet<Map.Entry<FileEntry, Double>> elements = results.getFileEntries();

            printSeparator();
            if (id > elements.size()) {
              printLine("not a valid id: " + id);
            } else if (showDetails) {
              printResult(results, metaDataSearch, contentSearch, id, maxMetadataLength,
                  maxSnippetLength);
            } else if (id != null) {
              int currentPseudoId = 1;
              for (Map.Entry<FileEntry, Double> flEntry : elements) {
                if (currentPseudoId == id) {
                  System.out.println("Opening file, please wait...");

                  FileEntry entry = flEntry.getKey();

                  String[] err = db.openFile(entry.getPath(), 0, openDirectory,
                      WAIT_FOR_STDERROR_IN_SEC);

                  String error = err[0];
                  String command = err[1];
                  Thread.sleep(10);

                  if (command == null || command.trim().isEmpty()) {
                    System.out.println("Your command is empty");
                  } else if (error != null && !error.trim().isEmpty()) {
                    printSeparator();
                    System.out
                        .println("\n" + AnsiColors.RED + "Used command:" + AnsiColors.DEFAULT);
                    System.out.println(command);
                    System.out.println(AnsiColors.RED
                        + "Your command threw an error. Stderr-message:" + AnsiColors.DEFAULT);
                    System.out.println(error);
                  }
                }
                currentPseudoId++;
              }
            }
            printSeparator();
          }
        } catch (Exception e) {
          printSeparator();
          printLine("not a valid command: " + line);
          printScannerInfo();
        }
      }
    }
    // }}}
  }

  /**
   *
   *
   */
  public void pause() {
    if (this.executor != null && !this.executor.isPaused()) {
      this.executor.pause();
    }
    this.cfg.setPaused(true);
  }

  /**
   *
   *
   */
  public void unpause() {
    if (this.executor != null && this.executor.isPaused()) {
      this.executor.resume();
    }
    this.cfg.setPaused(false);
  }

  /**
   *
   *
   */
  public void pauseToggle() {
    if (this.cfg.isPaused()) {
      this.unpause();
    } else {
      this.pause();
    }
  }

  /**
   *
   *
   */
  public void refresh() {
    if (this.db != null) {
      this.db.getH2DB().disconnect();
      this.db = null;
    }
  }

  /**
   *
   *
   */
  public void shutdown(boolean kill) {
    try {
      boolean killDialogVisible = false;

      if (executor != null && !executor.isTerminated()) {

        EventManager.instance().cancelingIndexing();
        this.cfg.setShutdown(true);
        this.unpause();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        for (int i = 0; i < 110; i++) {

          if (executor.isTerminated()) {
            break;
          }

          if (i > 70) {
            if (!killDialogVisible) {
              Platform.runLater(new Runnable() {
                @Override
                public void run() {
                  EventHandler<ActionEvent> yesHandler = new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                      System.exit(1);
                    }
                  };
                  GUIController.instance().showYesNoDialog(KILL_MESSAGE, yesHandler, null, false);
                }
              });
              killDialogVisible = true;
            }
          }

          if (i > 140) {
            System.exit(1);
          }

          if (i > 10 && kill) {
            System.exit(0);
          }

          Thread.sleep(1000);
        }
      }

      if (kill) {
        System.exit(0);
      }

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * @return the db
   */
  public DBManager getDb() {
    return db;
  }

  /**
   * @return the numThreads
   */
  public int getNumThreads() {
    return numThreads;
  }

  /**
   * @return the currentFileWorkers
   */
  public ConcurrentHashMap<Long, File> getCurrentFileWorkers() {
    return currentFileWorkers;
  }

  /**
   * @return the showInitialCPUList
   */
  public synchronized boolean showInitialCPUList() {
    return showInitialCPUList;
  }

  /**
   * @param showInitialCPUList
   *          the showInitialCPUList to set
   */
  public synchronized void setShowInitialCPUList(boolean showInitialCPUList) {
    this.showInitialCPUList = showInitialCPUList;
  }

  /**
   * @return the config
   */
  public Config getConfig() {
    return this.cfg;
  }

  /**
   *
   *
   */
  public void stopDebugTimer() {
    if (this.getConfig().getProp(DEBUG_MODE)) {
      final long estimatedTime = System.currentTimeMillis() - startTime;
      System.out.println("\n[DEBUG] RUNTIME IN MS: " + estimatedTime);
    }
  }

}
