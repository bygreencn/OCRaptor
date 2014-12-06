package mj.database;

import static mj.file_handler.filter.FileType.CODE_HTML;
import static mj.file_handler.filter.FileType.CODE_XHTML;
import static mj.file_handler.filter.FileType.CODE_XML;
import static mj.file_handler.filter.FileType.EPUB;
import static mj.file_handler.filter.FileType.LO_CALC;
import static mj.file_handler.filter.FileType.LO_IMPRESS;
import static mj.file_handler.filter.FileType.LO_WRITER;
import static mj.file_handler.filter.FileType.MS_EXCEL;
import static mj.file_handler.filter.FileType.MS_EXCEL_OXML;
import static mj.file_handler.filter.FileType.MS_OXPS;
import static mj.file_handler.filter.FileType.MS_POWERPOINT;
import static mj.file_handler.filter.FileType.MS_POWERPOINT_OXML;
import static mj.file_handler.filter.FileType.MS_RTF;
import static mj.file_handler.filter.FileType.MS_WORD;
import static mj.file_handler.filter.FileType.MS_WORD_OXML;
import static mj.file_handler.filter.FileType.PDF;
import static mj.file_handler.filter.FileType.PS;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mj.configuration.Config;
import mj.configuration.properties.ConfigString;
import mj.console.ConsoleOutputFormatter;
import mj.console.Platform;
import mj.console.Platform.Os;
import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.FileEntryDao;
import mj.extraction.result.document.FullText;
import mj.extraction.result.document.FullTextDao;
import mj.extraction.result.document.MetaData;
import mj.extraction.result.document.MetaDataDao;
import mj.file_handler.executer.CommandExecutor;
import mj.file_handler.executer.handler_impl.SimpleOutput;
import mj.file_handler.filter.FileType;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

public class DBManager {
  private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(getClass());

  private H2Database h2DB;
  private String database;
  private String databaseDir;
  private LinkedList<File> invalidFiles;
  private String[] textFileExtensions;
  private Config cfg;

  private FileEntryDao fileEntryDao;
  private FullTextDao elementDao;
  private MetaDataDao metaDataDao;

  public static final String INCREMENT = "GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)";

  /**
   * @return the db
   */
  public H2Database getH2DB() {
    return h2DB;
  }

  /**
   * @return the databaseDir
   */
  public String getDatabaseDir() {
    return databaseDir;
  }

  /**
   * @return the invalidFiles
   */
  public LinkedList<File> getInvalidFiles() {
    return invalidFiles;
  }

  /**
   *
   *
   * @param file
   */
  public synchronized void addInvalidFile(File file) {
    this.invalidFiles.add(file);
  }

  /**
   *
   */
  public DBManager(String database, String databaseDir) {
    this.database = database;
    this.databaseDir = databaseDir;
  }

  /**
   *
   *
   * @return
   */
  public Connection getConnection() {
    if (this.h2DB != null) {
      return this.h2DB.getConnection();
    }
    return null;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public DBManager init(boolean reset) throws Exception {
    this.fileEntryDao = new FileEntryDao();
    this.elementDao = new FullTextDao();
    this.metaDataDao = new MetaDataDao();

    // h2 tutorial
    // http://www.h2database.com/html/tutorial.html#fulltext

    // lucene queries
    // https://www.drupal.org/node/375446

    this.invalidFiles = new LinkedList<File>();
    this.cfg = Config.inst();

    if (cfg == null)
      throw new NullPointerException("CONFIG OBJECT NOT INITIALIZED!");

    String textFileExtensionsFromProperties = cfg.getProp(ConfigString.TEXT_FILE_EXTENSIONS);
    if (textFileExtensionsFromProperties != null
        && !textFileExtensionsFromProperties.trim().isEmpty()) {
      textFileExtensions = textFileExtensionsFromProperties.split(";");
    }

    File dataPath = new File(this.databaseDir);
    if (this.h2DB == null) {
      this.h2DB = new H2Database(dataPath, this.database).initConnectionPool();
    }

    if (reset) {
      ConsoleOutputFormatter.printLine("RESETTING DATABASE: \"" + this.database + "\" IN FOLDER:");
      ConsoleOutputFormatter.printLine("\"" + dataPath + "\"");
      this.reset();
      this.databaseIncomplete(true);
    }

    // h2DB.printTables();
    return this;
  }

  public static final Integer TABLES_SIZE = 4;

  /**
   *
   *
   * @param logError
   * @return
   */
  public boolean databaseIncomplete(boolean logError) {
    String corruptionDetails = this.databaseIncomplete();
    if (corruptionDetails != null && logError) {
      LOG.error("Your data is corrupted, you should reset your database\n" + corruptionDetails);
    }
    return corruptionDetails != null;
  }

  /**
   *
   *
   * @return
   */
  private String databaseIncomplete() {
    String corruptionDetails = null;
    try {
      List<String> tables = this.h2DB.getAllTables();
      if (tables != null) {
        if (tables.size() < TABLES_SIZE) {
          if (tables.isEmpty()) {
            corruptionDetails = "[TABLES] DB-tables not found.";
          } else {
            corruptionDetails = "[TABLES] " + tables.toString();
          }
        }

        if (!luceneIndexAvailable()) {
          corruptionDetails = "[TABLES] Lucene index not found:\n" + tables.toString();
        }

        File dbPath = this.h2DB.getDataPath();
        if (!dbPath.exists()) {
          corruptionDetails = "[FILESYSTEM] Database-path was not created:\n\""
              + dbPath.getAbsolutePath() + "\"";
        } else if (!dbPath.canWrite() || !dbPath.canRead()) {
          corruptionDetails = "[FILESYSTEM] Permission-Error with database-path:\n\""
              + dbPath.getAbsolutePath() + "\"";
        }
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
    return corruptionDetails;
  }

  /**
   *
   *
   * @param forceReset
   * @return
   *
   * @throws Exception
   */
  public boolean reset() throws Exception {
    return reset(true);
  }

  /**
   *
   *
   * @param resetIfNecessary
   *
   * @throws Exception
   */
  private boolean reset(boolean forceReset) throws Exception {
    Connection connection = null;
    try {
      connection = this.h2DB.getConnection();
      return this.reset(forceReset, connection);
    } catch (Exception e) {
      // TODO:
      e.printStackTrace();
      return false;
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  /**
   *
   *
   * @param forceReset
   * @param connection
   *
   * @throws Exception
   */
  private boolean reset(boolean forceReset, Connection connection) throws Exception {

    if (forceReset) {
      connection.close();
      this.h2DB.removeDBDirectory();
      this.h2DB.initConnectionPool(true);
      connection = this.h2DB.getConnection();
    }

    if (forceReset) {
      this.h2DB.execute("DROP TABLE IF EXISTS " + FileEntryDao.TABLE_NAME, connection);
      this.h2DB.execute("DROP TABLE IF EXISTS " + MetaDataDao.TABLE_NAME, connection);
      this.h2DB.execute("DROP TABLE IF EXISTS " + FullTextDao.TABLE_NAME, connection);
      this.h2DB.execute("DROP TABLE IF EXISTS INDEXER", connection);

      if (this.h2DB.tableExists(FileEntryDao.TABLE_NAME)) {
        // TODO:
        System.out.println("COULD NOT RESET DATABASE");
        System.exit(0);
      }
    }

    // @formatter:off
    String createStatement = "" //
        // ------------------------------------------------ //
        + "CREATE TABLE " + FileEntryDao.TABLE_NAME + " ( "
        + "ID INTEGER NOT NULL " + INCREMENT + ", "
        + FileEntryDao.COLUMN_FILE_PATH + " VARCHAR(3000) NOT NULL, "
        + FileEntryDao.COLUMN_FILE_HASH + " VARCHAR(32) NOT NULL"
        + " ); "
        // ------------------------------------------------ //
        + "CREATE TABLE " + FullTextDao.TABLE_NAME + " ( "
        + "ID INTEGER NOT NULL " + INCREMENT + ", "
        + FileEntryDao.TABLE_NAME + "_ID INTEGER NOT NULL, "
        + FullTextDao.COLUMN_FILE_CONTENT + " CLOB, "
        + "INDEX" + " VARCHAR(8), "
        + "FOREIGN KEY(`" + FileEntryDao.TABLE_NAME + "_ID`) "
        + "REFERENCES `" + FileEntryDao.TABLE_NAME + "` (`ID`) "
        + "ON DELETE CASCADE ON UPDATE CASCADE"
        + " ); "
        // ------------------------------------------------ //
        + "CREATE TABLE " + MetaDataDao.TABLE_NAME + " ( "
        + "ID INTEGER NOT NULL " + INCREMENT + ", "
        + FileEntryDao.TABLE_NAME + "_ID INTEGER NOT NULL, "
        + MetaDataDao.COLUMN_METADATA_KEY + " VARCHAR(64), "
        + MetaDataDao.COLUMN_METADATA_VALUE + " CLOB, "
        + "INDEX" + " VARCHAR(8), "
        + "FOREIGN KEY(`" + FileEntryDao.TABLE_NAME + "_ID`) "
        + "REFERENCES `" + FileEntryDao.TABLE_NAME + "` (`ID`) "
        + "ON DELETE CASCADE ON UPDATE CASCADE"
        + " ); "
        // ------------------------------------------------ //
    ;
    // @formatter:on

    if (!h2DB.tableExists(FileEntryDao.TABLE_NAME) && forceReset) {
      // TODO:
      // System.out.println("CREATING DATABASE: \"" + this.database +
      // "\" IN FOLDER:\n\"" + this.databaseDir
      // + "\"");
      h2DB.execute(createStatement, connection);
      ConsoleOutputFormatter.printLine("[DONE]", true);
      Thread.sleep(50);
    }

    if (forceReset || !luceneIndexAvailable()) {
      FullTextLucene.init(connection);
      FullTextLucene.dropAll(connection);

      if (!luceneIndexAvailable()) {

        FullTextLucene.createIndex(connection, "PUBLIC", //
            FullTextDao.TABLE_NAME, FullTextDao.COLUMN_FILE_CONTENT + ", INDEX");

        FullTextLucene.createIndex(connection, "PUBLIC", //
            MetaDataDao.TABLE_NAME, MetaDataDao.COLUMN_METADATA_VALUE + ","
                + MetaDataDao.COLUMN_METADATA_KEY + ", INDEX");

      }
    }

    return true;
  }

  /**
   *
   *
   * @return
   *
   * @throws IOException
   */
  public boolean luceneIndexAvailable() throws IOException {
    boolean available = IndexReader.indexExists(FSDirectory.open(this.h2DB.getDataPath()));
    return available;
  }

  /**
   * @throws SQLException
   *
   *
   */
  public SearchResult searchIndex(String luceneString, int maxResults, Connection connection)
      throws SQLException {
    // https://www.drupal.org/node/375446
    SearchResult result = new SearchResult();

    ResultSet rs = null;
    Statement statement = null;
    try {
      // luceneString = "TEXT:Jedich AND VALUE:20140916_1519092.jpg";

      rs = FullTextLucene.search(connection, luceneString, maxResults, 0, false);

      HashMap<FullText, Double> elements = new HashMap<FullText, Double>();
      HashMap<MetaData, Double> metadata = new HashMap<MetaData, Double>();

      List<String[]> resultRows = H2Database.convertToList(rs);

      for (String[] foundEntry : resultRows) {
        if (foundEntry.length >= 2) {
          String whereStatement = foundEntry[0];
          double score = Double.parseDouble(foundEntry[1]);

          if (whereStatement != null) {
            if (whereStatement.contains("\"" + FullTextDao.TABLE_NAME + "\"")) {
              FullText element = elementDao.findByFulltextStatement(whereStatement, maxResults,
                  connection);
              if (element != null) {
                elements.put(element, score);
              }
            } else if (whereStatement.contains("\"" + MetaDataDao.TABLE_NAME + "\"")) {
              MetaData md = metaDataDao.findByFulltextStatement(whereStatement, maxResults,
                  connection);
              if (md != null) {
                metadata.put(md, score);
              }
            }
          }
        }
      }
      if (!elements.isEmpty()) {
        result.setElements(elements);
      }
      if (!metadata.isEmpty()) {
        result.setMetadata(metadata);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (rs != null)
        rs.close();
      if (statement != null) {
        statement.close();
      }
    }
    return result;
  }

  /**
   *
   *
   * @return
   */
  public FileEntry findMD5Hash(String filePath) {
    Connection connection = null;
    FileEntry entry = null;
    try {
      connection = this.getConnection();
      entry = fileEntryDao.findByPath(filePath, connection);
    } catch (Exception e) {
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
    return entry;
  }

  /**
   *
   *
   * @param propertyToComplete
   * @param file
   * @return
   */
  private String prepareCommand(String command, File file, int page) {
    if (command != null && !command.trim().isEmpty()) {
      Os os = Platform.getSystem();
      String filePath = file.getAbsolutePath();
      String dirPath = file.getParentFile().getAbsolutePath();

      if (os == Os.WINDOWS) {
        String filePrefix = FilenameUtils.getPrefix(filePath);
        filePath = filePrefix + "\"" + filePath.substring(filePrefix.length(), filePath.length())
            + "\"";
        String dirPrefix = FilenameUtils.getPrefix(dirPath);
        dirPath = dirPrefix + "\"" + dirPath.substring(dirPrefix.length(), dirPath.length()) + "\"";
      } else {
        filePath = "\"" + filePath + "\"";
      }

      command = MessageFormat.format(command, filePath, file.getParentFile().getAbsolutePath(),
          page, page - 1);
    }
    return command;
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public String[] openDirectory(String path, int waitingTime) {
    return this.openFile(path, 0, true, waitingTime);
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public String[] openFile(String path, int waitingTime) {
    return this.openFile(path, 0, false, waitingTime);
  }

  /**
   *
   *
   * @param path
   * @param page
   * @return
   */
  public String[] openFile(String path, int page, int waitingTime) {
    return this.openFile(path, page, false, waitingTime);
  }

  /**
   *
   *
   * @param files
   * @param id
   */
  public String[] openFile(String path, int page, boolean openParentDirectory, int waitingTime) {
    String command = null;
    try {
      File file = new File(path);

      if (file.exists() && file.isFile()) {
        Os os = Platform.getSystem();
        if (openParentDirectory) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.DIRECTORY_OPEN_CMD_));
        } else if (FileType.isValidImageFile(file)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.IMAGE_FILE_OPEN_CMD_));
        } else if (FileType.isValidTextFile(file, textFileExtensions)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.TEXT_FILE_OPEN_CMD_));
        } else if (FileType.is(file, EPUB, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.EPUB_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, PDF, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.PDF_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, PS, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.PS_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, MS_OXPS, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.MS_XPS_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, MS_RTF, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.MS_RTF_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, MS_WORD, true) || FileType.is(file, MS_WORD_OXML, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.MS_WORD_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, MS_EXCEL, true) || FileType.is(file, MS_EXCEL_OXML, true)) {
          command = this.cfg
              .getProp(ConfigString.getByOs(ConfigString.MS_EXCEL_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, MS_POWERPOINT, true)
            || FileType.is(file, MS_POWERPOINT_OXML, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.MS_PPT_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, LO_WRITER, true)) {
          command = this.cfg.getProp(ConfigString
              .getByOs(ConfigString.LO_WRITER_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, LO_CALC, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.LO_CALC_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, LO_IMPRESS, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.LO_IMPRESS_FILE_OPEN_CMD_,
              os));
        } else if (FileType.is(file, CODE_HTML, true) || FileType.is(file, CODE_XHTML, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.HTML_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, CODE_XML, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.XML_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, FileType.XOJ, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.XOJ_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, FileType.MS_CHM, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.MS_CHM_FILE_OPEN_CMD_, os));
        }
        command = prepareCommand(command, file, page);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    String errOutput = null;
    if (command != null && !command.trim().isEmpty()) {
      SimpleOutput eventHandler = new SimpleOutput();
      CommandExecutor bashExecuter = new CommandExecutor(Platform.getSystem(), eventHandler);
      bashExecuter.setCommand(command);
      ExecutorService es = Executors.newCachedThreadPool();
      es.execute(bashExecuter);
      es.shutdown();

      // collecting std/err-output for given time
      try {
        es.awaitTermination(waitingTime, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      errOutput = eventHandler.getErrOut();
    }

    return new String[] { errOutput, command };
  }
}
