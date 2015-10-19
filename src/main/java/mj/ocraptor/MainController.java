package mj.ocraptor;

import static mj.ocraptor.configuration.properties.ConfigBool.ALWAYS_REMOVE_MISSING_FILES_FROM_DB;
import static mj.ocraptor.configuration.properties.ConfigBool.DEBUG_MODE;
import static mj.ocraptor.configuration.properties.ConfigInteger.DIALOG_SNIPPET_MAX_STRING_LENGTH;
import static mj.ocraptor.configuration.properties.ConfigInteger.MAX_SEARCH_RESULTS;
import static mj.ocraptor.configuration.properties.ConfigInteger.NUMBER_OF_CPU_CORES_TO_USE;
import static mj.ocraptor.configuration.properties.ConfigString.DATABASE_NAME;
import static mj.ocraptor.console.COF.printEmptySeparator;
import static mj.ocraptor.console.COF.printLine;
import static mj.ocraptor.console.COF.printScannerInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.rmi.RemoteException;
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
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.console.AnsiColor;
import mj.ocraptor.console.COF;
import mj.ocraptor.console.CommandLineInterpreter;
import mj.ocraptor.database.DBManager;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.FileEntryDao;
import mj.ocraptor.database.dao.FullText;
import mj.ocraptor.database.dao.ResultError;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.database.search.TextProcessing;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.file_handler.PausableExecutor;
import mj.ocraptor.file_handler.TextExtractor;
import mj.ocraptor.file_handler.TextExtractorThread;
import mj.ocraptor.file_handler.generator.SimpleListGenerator;
import mj.ocraptor.file_handler.structures.FileList;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.javafx.GUIController;
import mj.ocraptor.javafx.Icon;
import mj.ocraptor.rmi_client.RMIClientController;
import mj.ocraptor.rmi_server.RMIServerImpl;
import mj.ocraptor.swing.SplashScreen;
import mj.ocraptor.tools.St;
import mj.ocraptor.tools.SystemTools;

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
  private RMIServerImpl server;
  private Thread serverThread;
  private RMIClientController clientsManager;
  private SplashScreen splashScreen;
  private Thread clientsThread;
  private Localization localization;

  private static final int maxClientStartupTimeInSec = 60;

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(MainController.class);


  // *INDENT-OFF*
  private static final String
    CONFIG_ERROR  = "Config object not initialized",
    DB_START      = "Initializing embedded H2 database from directory:",
    INDEX_START   = "Starting indexing ... (this could take a while)",
    FILE_PATTERN  = "%n[%-5p]--%d{yyyy-MM-dd HH:mm:ss}--%c{1}%n[Thread:%t][%l]%n%m%n%n",
    KILL_MESSAGE  = "This takes longer than usual,\ndo you want to kill the application?";
  // *INDENT-ON*

  // ------------------------------------------------ //
  private Status status = Status.STOPPED;

  // *INDENT-OFF*
  public enum Status {
    WAITING_FOR_SERVER_TO_START,
    WAITING_FOR_CLIENTS_TO_START,
    INDEXING,
    INDEXING_FINISHED,
    PAUSED,
    STOPPED,
    TEMPORARILY_STOPPED,
    KILLED
  }
  // *INDENT-ON*

  // ------------------------------------------------ //

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
   * RMIClient will not instantiate this class. {@inheritDoc}
   *
   * @see Object#MainController()
   */
  private MainController() {
    Thread.currentThread().setName(Config.APP_NAME + " - Main controller");
    FileTools.clearTempDirectory();
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
    if (instance == null) {
      try {
        instance = new MainController();
        final Config cfg = instance.initConfiguration(config);
        instance.initLogger(); // init after config
        cfg.validateProperties();
        instance.localization = Localization.instance();
        instance.redirectStdErrors();
        instance.initMultiCoreProcessing();
        instance.initDatabase();

        Config.inst().prepareConfigurationDirectory();
        instance.startIndexing();

      } catch (Exception e) {
        e.printStackTrace();
      }
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
    if (instance == null) {
      try {
        instance = new MainController();

        CommandLineInterpreter cli = CommandLineInterpreter.instance();
        cli.parseArguments(args);

        if (cli.isGui()) {
          instance.showSplashScreen();
          final Config cfg = instance.initConfiguration(Config.initFromGui());
          Config.inst().prepareConfigurationDirectory();
          instance.initLogger(); // init after config
          cfg.validateProperties();
          instance.localization = Localization.instance();
          instance.redirectStdErrors();
          instance.initMultiCoreProcessing();
          instance.initGUI();
        } else {
          final Config cfg = instance.initConfiguration(Config.initFromCLI());
          instance.initLogger(); // init after config
          cfg.validateProperties();
          instance.localization = Localization.instance();
          instance.redirectStdErrors();
          instance.initMultiCoreProcessing();
          instance.initDatabase();
          instance.startIndexing();
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
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
  private void showSplashScreen() {
    try {
      // SwingUtilities.invokeLater(new Runnable() {
      // public void run() {
      instance.splashScreen = new SplashScreen(SplashScreen.class.getResource(Icon.SPLASH_SCREEN
          .getFileName()));
      // });
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
  }

  /**
   *
   *
   */
  public void closeSplashScreen() {
    if (this.splashScreen != null) {
      this.splashScreen.close();
    }
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
   *
   * @throws IOException
   */
  private void initServer() throws IOException {
    if (this.status != Status.KILLED) {
      this.serverThread = new Thread(new Server());
      this.serverThread.start();
    }
  }

  /**
   *
   */
  private class Server implements Runnable {
    public void run() {
      Thread.currentThread().setName(Config.APP_NAME + " - RMI Server");
      server = new RMIServerImpl(numThreads);
      server.connect();
    }
  }

  /**
   *
   *
   *
   * @throws IOException
   */
  private Thread initClients() throws IOException {
    final Thread clientsThread = new Thread(new Clients());
    clientsThread.start();
    return clientsThread;
  }

  /**
   *
   */
  private class Clients implements Runnable {
    public void run() {
      Thread.currentThread().setName(Config.APP_NAME + "Indexing RMI client manager");
      if (clientsManager == null) {
        clientsManager = new RMIClientController(numThreads).init();
      }

      SystemTools sigar = null;

      while (!Thread.currentThread().isInterrupted()) {
        // System.err.println(Thread.currentThread().getName());
        try {
          if (sigar == null) {
            sigar = new SystemTools();
          }
          int connectedClientsCount = server.getConnectedClientsSize();
          int startedSubProcesses = clientsManager.getProcesses().size();

          // TODO: sleep before restarting
          // if (connectedClientsCount == numThreads - 1 && status ==
          // Status.INDEXING) {
          // Thread.sleep(10000);
          // }

          final boolean indexingPaused = getStatus() == Status.PAUSED;
          final boolean indexingStopped = getStatus() == Status.STOPPED;
          final boolean indexingFinished = getStatus() == Status.INDEXING_FINISHED;

          // ------------------------------------------------ //
          // *INDENT-OFF*
          // TODO: syso [list threads]
          // System.out.println(getStatus());
          // Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
          // for (Thread thread: threadSet) {
          //   System.out.println("Thread: " + thread.getId() + ":" + thread.getName());
          // }
          // *INDENT-ON*
          // ------------------------------------------------ //

          final long freeRam = sigar.getFreeRamInMB();
          final Integer xmxValue = cfg.getProp(ConfigInteger.PROCESS_XMX);

          if (server != null && connectedClientsCount < numThreads && !indexingStopped
              && !indexingPaused && !indexingFinished && freeRam > xmxValue + 1024) {
            if (connectedClientsCount == startedSubProcesses) {
              String clientID = St.generatePassword(130, 32);
              clientsManager.addSubProcess(clientID);
            } else {
              // TODO:
              // erst etwas warten, dann ueberpruefen
              // LOG.error("Zombie process!!!");
            }
          }
          Thread.sleep(100);
        } catch (InterruptedException e) {
          break;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   *
   * @throws IOException
   */
  private Config initConfiguration(Config config) throws IOException {
    this.fileListGenerator = new SimpleListGenerator();
    this.numThreads = 1;
    this.cfg = config;
    COF.setQuiet(this.cfg.quietMode());
    return this.cfg;
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
    File logFile = new File(this.cfg.getMainLogFilePath());
    try {
      FileTools.directoryIsValid(logFile.getParentFile(), "Logfile-directory");
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
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
      if (entries != null && !entries.isEmpty()) {
        EventManager.instance().removingMissingFiles(null, entries.size());
        for (String filePath : entries) {
          File file = new File(filePath);

          if (!file.exists()) {
            fileEntryDao.removeByPath(filePath, connection);
            countMissingFiles++;
          }

          while (!Thread.currentThread().isInterrupted() && getStatus() == Status.PAUSED) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          if (getStatus() == Status.STOPPED || getStatus() == Status.INDEXING_FINISHED) {
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

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  private boolean waitForServerToStart() {
    for (int i = 0; i < 20; i++) {
      if (this.server == null) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        return true;
      }
    }
    return false;
  }

  /**
   *
   *
   * @return
   */
  private boolean startClients() {
    // ------------------------------------------------ //
    boolean serverOnline = this.waitForServerToStart();
    if (!serverOnline) {
      return false;
    }
    // ------------------------------------------------ //

    // ------------------------------------------------ //
    if (this.server.getConnectedClientsSize() > 0) {
      boolean successfullyKilled = this.killClients();
      if (!successfullyKilled) {
        return false;
      }
    }
    // ------------------------------------------------ //

    // ------------------------------------------------ //
    if (this.clientsThread == null) {
      try {
        this.clientsThread = initClients();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    for (int i = 0; i < maxClientStartupTimeInSec; i++) {
      if (this.server.getConnectedClientsSize() > 0) {
        return true;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    // ------------------------------------------------ //

    return false;
  }

  /**
   *
   *
   * @return
   */
  private boolean killClients() {
    try {
      this.server.sendShutdownMessage();
      for (int i = 0; i < 10; i++) {
        if (this.server.getConnectedClientsSize() == 0) {
          return true;
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   *
   *
   */
  private void stopServerIfRunning() {
    if (this.serverThread != null && !this.serverThread.isInterrupted()) {
      try {
        this.server.disconnect();
        Thread.sleep(500);

        this.clientsManager.shutdown();
        this.clientsThread.interrupt();
        this.clientsThread = null;
        this.clientsManager = null;
        Thread.sleep(500);

        this.serverThread.interrupt();
        this.serverThread = null;
        Thread.sleep(500);

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   *
   *
   * @return
   */
  public void startIndexing() {
    // {{{

    if (cfg == null) {
      throw new NullPointerException();
    }

    if (this.getStatus() == Status.KILLED) {
      return;
    }

    COF.printEmptySeparator();
    COF.printText(AnsiColor.BOLD + "Using config file:");
    COF.printText(St.shortenHomePathInDirectory(cfg.getConfigUserFilePath()));
    COF.printEmptySeparator();
    COF.printText(AnsiColor.BOLD + "Using log file:");
    COF.printText(St.shortenHomePathInDirectory(cfg.getMainLogAbsoluteFilePath()));
    COF.printEmptySeparator();

    this.setStatus(Status.INDEXING);
    String directoryToIndex = this.cfg.getDirectoryToIndex();
    this.initDatabase();

    if (directoryToIndex != null) {
      if (!this.checkedForMissingFiles
          && Config.inst().getProp(ALWAYS_REMOVE_MISSING_FILES_FROM_DB)) {
        removeMissingFiles();
      }

      this.showInitialCPUList = true;
      this.currentFileWorkers = new ConcurrentHashMap<Long, File>();

      // ------------------------------------------------ //
      final ThreadFactory factory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable target) {
          final Thread thread = new Thread(target);
          LOGGER.debug("Creating new worker thread");
          thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
              LOGGER.error("Uncaught Exception", e);
            }
          });
          return thread;
        }
      };
      this.executor = new PausableExecutor(numThreads, factory);
      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //
      TextExtractor handler = null;

      try {
        handler = new TextExtractor(db, executor);

        TextExtractorThread.resetCount();
        if (this.cfg.showProgress() || this.cfg.verbose()) {
          handler.countFilesOnly();
          EventManager.instance().startCountingFiles();
          FileList list = fileListGenerator.generateFileList(directoryToIndex, handler);
          handler.stopCounting();

          if (list == null) {
            setStatus(Status.KILLED);
            return;
          }

          long fileCount = handler.getFileCount();
          EventManager.instance().countingFiles(fileCount, true);
          TextExtractorThread.setFileCount(fileCount, cfg.verbose());
        }

        // ------------------------------------------------ //
        // TODO: gui multiple directories, stopping server after every
        // directory???
        this.stopServerIfRunning();
        try {
          this.initServer();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        // ------------------------------------------------ //
        boolean successfullyStarted = this.startClients();
        if (!successfullyStarted) {
          LOGGER.error("Could not start clients in " + maxClientStartupTimeInSec + " seconds.");
          return;
        }

        printEmptySeparator();
        printLine(AnsiColor.BOLD.toString() + INDEX_START);
        printEmptySeparator();

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
          break;
        }
      }

      // signal end
      EventManager.instance().printFolderFinished();
    }

    final String searchString = this.cfg.getSearchString();

    if (this.cfg.getSearchString() != null && searchString != null
        && !searchString.trim().isEmpty()) {
      COF.printEmptySeparator();
      COF.printLineStretched(AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString()
          + "Searching database", true);

      // try {
      // Connection connection = this.db.getConnection();
      // this.db.countExtensions(connection);
      // connection.close();
      // } catch(Exception e)
      // {
      // e.printStackTrace();
      // }

      this.searchDatabase(searchString);
    }

    if (Config.devMode()) {
      try {
        //this.db.printTables(true);
      } catch (Exception e) {
      }
    }

    this.stopServerIfRunning();

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
        String database = this.cfg.getProp(DATABASE_NAME), databaseDir = this.cfg.getDatabasePath();
        COF.printLineStretched(AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString()
            + "Initializing database", true);
        printEmptySeparator();
        printLine(AnsiColor.BOLD + DB_START);
        printLine(St.shortenHomePathInDirectory(databaseDir));
        printEmptySeparator();
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
      searchString = St.normalizeDocumentText(searchString);
      echo.searchProgressIndicator(0.1, "Preparing search");

      this.initDatabase();
      echo.searchProgressIndicator(0.2);

      final int maxSearchResults = this.cfg.getProp(MAX_SEARCH_RESULTS);
      final int maxSnippetLength = this.cfg.getProp(DIALOG_SNIPPET_MAX_STRING_LENGTH);

      // ------------------------------------------------ //

      echo.searchProgressIndicator(0.5);
      Connection connection = null;
      LuceneResult resultToDisplay = null;
      try {
        if (searchString != null) {
          // ------------------------------------------------ //
          connection = db.getConnection();
          echo.searchProgressIndicator(0.7, "Searching in database");
          resultToDisplay = db.searchIndex(searchString, maxSearchResults, connection);

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
              if (fileEntry != null) {
                fileEntry.setFullText(element);
                fileEntriesInResult.put(fileEntry, score);

                if (fileEntry != null && !joinedFileList.contains(fileEntry)) {
                  joinedFileList.add(fileEntry);
                }
              }
            }
          }
          echo.searchProgressIndicator(0.85);
          // ------------------------------------------------ //

          if (resultToDisplay != null) {
            resultToDisplay.setFileEntries(fileEntriesInResult);
            echo.searchProgressIndicator(0.95, "Rendering");
            echo.searchProgressIndicator(-1);
          }
        }

        else {
          return;
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

      echo.printResult(resultToDisplay, searchString, null, maxSnippetLength);
      scanForUserInput(resultToDisplay, searchString, maxSnippetLength);
    }
  }

  private static final String //
      SCAN_QUIT = "q",//
      SCAN_DETAILS = "s", //
      SCAN_BROWSER = "b", //
      SCAN_DIR = "d";

  public static final int WAIT_FOR_STDERROR_IN_SEC = 3;

  /**
   *
   *
   * @param results
   * @param contentSearch
   * @param maxSnippetLength
   */
  private void scanForUserInput(final LuceneResult results, String contentSearch,
      int maxSnippetLength) {
    if (results == null) {
      return;
    }

    // {{{
    if (this.cfg.waitForUserInput() && GUIController.instance() == null
        && !results.getFileEntries().isEmpty()) {

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (!Thread.currentThread().isInterrupted()) {
        printScannerInfo();

        // System.exit(0);
        String line = "";
        try {
          line = in.readLine();

          System.out.println();
          if (line != null && !line.trim().isEmpty()) {
            line = line.trim().toLowerCase();
            Integer id = null;

            if (line.equals(SCAN_QUIT)) {
              System.out.println();
              return;
            }

            boolean openDirectory = false;
            boolean showDetails = false;
            boolean openBrowser = false;

            if (line.startsWith(SCAN_DETAILS)) {
              id = Integer.parseInt(line.replaceFirst(SCAN_DETAILS, ""));
              showDetails = true;
            } else if (line.startsWith(SCAN_DIR)) {
              id = Integer.parseInt(line.replaceFirst(SCAN_DIR, ""));
              openDirectory = true;
            } else if (line.startsWith(SCAN_BROWSER)) {
              id = Integer.parseInt(line.replaceFirst(SCAN_BROWSER, ""));
              openBrowser = true;
            } else {
              id = Integer.parseInt(line);
            }

            SortedSet<Map.Entry<FileEntry, Double>> elements = results.getFileEntries();

            if (id > elements.size()) {
              COF.printSeparator();
              COF.printLineStretched(AnsiColor.RED_BACKGROUND.toString()
                  + AnsiColor.WHITE.toString() + "Not a valid id: " + id);
            } else if (showDetails) {
              EventManager.instance().printResult(results, contentSearch, id, maxSnippetLength);
            } else if (id != null) {
              int currentPseudoId = 1;
              for (Map.Entry<FileEntry, Double> flEntry : elements) {
                if (currentPseudoId == id) {
                  // TODO: text
                  COF.printSeparator();
                  COF.printLineStretched(AnsiColor.GREEN_BACKGROUND.toString()
                      + AnsiColor.WHITE.toString() + "Opening file, please wait...");

                  FileEntry entry = flEntry.getKey();

                  String path = entry.getPath();

                  if (openBrowser) {
                    final File tempFile = TextProcessing.saveXhtmlToFile(entry.getFullTextString(),
                        contentSearch, entry.getFile());
                    if (tempFile.exists()) {
                      path = tempFile.getAbsolutePath();
                    }
                  }

                  final String[] err = db
                      .openFile(path, 0, openDirectory, WAIT_FOR_STDERROR_IN_SEC);

                  String error = err[0];
                  String command = err[1];
                  Thread.sleep(10);

                  if (command == null || command.trim().isEmpty()) {
                    COF.printSeparator();
                    COF.printEmptySeparator();
                    COF.printText(AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString()
                        + "Your command is empty");
                    COF.printEmptySeparator();
                  } else if (error != null && !error.trim().isEmpty()) {
                    COF.printSeparator();
                    COF.printEmptySeparator();
                    COF.printText(AnsiColor.BOLD.toString() + AnsiColor.RED_BACKGROUND.toString()
                        + AnsiColor.WHITE.toString()
                        + "Your command threw an error.\nUsed command:");
                    COF.printText(AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString()
                        + command + "\n ");
                    COF.printText(AnsiColor.BOLD.toString() + AnsiColor.RED_BACKGROUND.toString()
                        + AnsiColor.WHITE.toString() + "Stderr-message:");
                    COF.printText(AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString()
                        + St.trimToLengthIndicatorRight(error, 500));
                    COF.printEmptySeparator();
                  }
                }
                currentPseudoId++;
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          COF.printSeparator();
          COF.printTextStretched(AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString()
              + "not a valid command: " + line);
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
    this.setStatus(Status.PAUSED);
  }

  /**
   *
   *
   */
  public void unpause() {
    if (this.executor != null && this.executor.isPaused()) {
      this.executor.resume();
    }
    this.setStatus(Status.INDEXING);
  }

  /**
   *
   *
   */
  public void pauseToggle() {
    if (getStatus() == Status.PAUSED) {
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
  // TODO: refactor shutdown
  public void shutdown(boolean kill) {
    try {

      boolean killDialogVisible = false;
      this.setStatus(Status.KILLED);

      if (executor != null && !executor.isTerminated()) {
        if (this.executor.isPaused()) {
          this.executor.resume();
        }
        this.stopServerIfRunning();

        EventManager.instance().cancelingIndexing();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
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
   * @return the server
   */
  public RMIServerImpl getServer() {
    return server;
  }

  /**
   * @return the splashScreen
   */
  public SplashScreen getSplashScreen() {
    return splashScreen;
  }

  /**
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * @param status
   *          the status to set
   */
  public void setStatus(Status status) {
    // TODO: syso
    // System.out.println("setting status: " + status.toString());
    this.status = status;
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

  /**
   *
   *
   * @return
   */
  public boolean stopped() {
    return getStatus() == Status.STOPPED;
  }

  /**
   *
   *
   * @return
   */
  public boolean paused() {
    return getStatus() == Status.PAUSED;
  }

  /**
   *
   *
   * @return
   */
  public boolean finished() {
    return getStatus() == Status.INDEXING_FINISHED;
  }

  /**
   *
   *
   * @return
   */
  public boolean running() {
    return getStatus() == Status.INDEXING;
  }

  // ------------------------------------------------ //

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
}
