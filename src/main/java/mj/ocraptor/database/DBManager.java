package mj.ocraptor.database;

import static mj.ocraptor.file_handler.filter.FileType.CODE_HTML;
import static mj.ocraptor.file_handler.filter.FileType.CODE_XHTML;
import static mj.ocraptor.file_handler.filter.FileType.CODE_XHTML2;
import static mj.ocraptor.file_handler.filter.FileType.EPUB;
import static mj.ocraptor.file_handler.filter.FileType.LO_CALC;
import static mj.ocraptor.file_handler.filter.FileType.LO_IMPRESS;
import static mj.ocraptor.file_handler.filter.FileType.LO_WRITER;
import static mj.ocraptor.file_handler.filter.FileType.MS_EXCEL;
import static mj.ocraptor.file_handler.filter.FileType.MS_EXCEL_OXML;
import static mj.ocraptor.file_handler.filter.FileType.MS_OXPS;
import static mj.ocraptor.file_handler.filter.FileType.MS_POWERPOINT;
import static mj.ocraptor.file_handler.filter.FileType.MS_POWERPOINT_OXML;
import static mj.ocraptor.file_handler.filter.FileType.MS_RTF;
import static mj.ocraptor.file_handler.filter.FileType.MS_WORD;
import static mj.ocraptor.file_handler.filter.FileType.MS_WORD_OXML;
import static mj.ocraptor.file_handler.filter.FileType.PDF;
import static mj.ocraptor.file_handler.filter.FileType.PS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.console.AnsiColor;
import mj.ocraptor.console.COF;
import mj.ocraptor.console.Platform;
import mj.ocraptor.console.Platform.Os;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.FileEntryDao;
import mj.ocraptor.database.dao.FullText;
import mj.ocraptor.database.dao.FullTextDao;
import mj.ocraptor.database.dao.ResultError;
import mj.ocraptor.database.error.DBPathNotFoundException;
import mj.ocraptor.database.error.FilePermissionException;
import mj.ocraptor.database.error.LuceneIndexNotFoundException;
import mj.ocraptor.database.error.TableEmptyException;
import mj.ocraptor.database.error.TableNotFoundException;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.file_handler.executer.CommandExecutor;
import mj.ocraptor.file_handler.executer.handler_impl.SimpleOutput;
import mj.ocraptor.file_handler.filter.FileType;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.tools.St;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import dnl.utils.text.table.TextTable;

/**
 *
 * @author
 */
public class DBManager {
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DBManager.class);

  private H2Database h2DB;
  private String database;
  private String databaseDir;
  private LinkedList<File> invalidFiles;
  private Config cfg;

  private FileEntryDao fileEntryDao;
  private FullTextDao elementDao;

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

    // h2 tutorial
    // http://www.h2database.com/html/tutorial.html#fulltext

    // lucene queries
    // https://www.drupal.org/node/375446

    this.invalidFiles = new LinkedList<File>();
    this.cfg = Config.inst();
    final File dataPath = new File(this.databaseDir);

    if (cfg == null)
      throw new NullPointerException("CONFIG OBJECT NOT INITIALIZED!");

    if (this.h2DB == null) {
      this.h2DB = new H2Database(dataPath, this.database).initConnectionPool();
    }

    // {{{ check if database exists
    try {
      this.databaseIncomplete();
    } catch (DBPathNotFoundException | TableNotFoundException e) {
      COF.printText("--> Database not found, creating new one.");
      reset = true;
    } catch (FilePermissionException | LuceneIndexNotFoundException e) {
      LOGGER.error("try resetting this database", e);
    } catch (Exception e) {
      // TODO: shutdown application
    }
    // }}}

    if (reset) {
      COF.printEmptySeparator();
      COF.printLine(AnsiColor.BOLD.toString() + "Resetting database: \"" + this.database
          + "\" in folder:");
      COF.printLine(dataPath.toString());
      boolean success = this.reset();
      if (success) {
        try {
          this.databaseIncomplete();
        } catch (TableEmptyException e) {
        }
      }
    }


    // [print] [database] [tables] [syso]
    // print tables at database select
    if (Config.DEBUG) {
      printTables(true);
    }

    return this;
  }

  /**
   *
   *
   * @param table
   * @return
   * @throws Exception
   */
  public boolean tableExists(String table) throws Exception {
    List<String> tables = getAllTables();
    return tables.contains(table) ? true : false;
  }

  /**
   *
   *
   * @param connection
   * @return
   *
   * @throws Exception
   */
  public List<String> getAllTables() throws Exception {
    return getAllTables(null);
  }

  /**
   *
   *
   * @param connection
   * @return
   *
   * @throws Exception
   */
  public List<String> getAllTables(Connection connection) throws Exception {
    Connection con = null;
    try {
      List<String> tables = new ArrayList<String>();
      con = (connection == null) ? getConnection() : connection;
      if (con != null) {
        DatabaseMetaData metaData = con.getMetaData();
        ResultSet metaResultSet = metaData.getTables(null, null, "%", new String[] { "TABLE" });
        while (metaResultSet.next()) {
          String tableName = metaResultSet.getString(3);
          tables.add(tableName);
        }
        return tables;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection == null && con != null) {
        con.close();
      }
    }
    return null;
  }

  /**
   *
   *
   * @param tableName
   * @return
   *
   * @throws Exception
   */
  public String[] getColumnNames(String tableName) throws Exception {
    return getColumnNames(tableName, null);
  }

  /**
   *
   *
   * @param connection
   * @return
   *
   * @throws Exception
   */
  public String[] getColumnNames(String tableName, Connection connection) throws Exception {
    Connection con = null;
    try {
      con = (connection == null) ? getConnection() : connection;
      ResultSet rs = h2DB.executeQuery("SELECT * FROM " + tableName + ";", con);
      int columnCount = rs.getMetaData().getColumnCount();
      String[] columnNames = new String[columnCount];
      for (int i = 1; i <= columnCount; i++) {
        columnNames[i - 1] = rs.getMetaData().getColumnName(i);
      }
      return columnNames;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection == null && con != null) {
        con.close();
      }
    }
    return null;
  }

  /**
   *
   *
   * @param resultSet
   * @return
   */
  public static String[][] convertTo2DArray(ResultSet resultSet, int trimLength) {
    try {
      int columnCount = resultSet.getMetaData().getColumnCount();
      resultSet.last();
      int rowCount = resultSet.getRow();
      resultSet.beforeFirst();

      String[][] data = new String[rowCount][columnCount];
      int z = 0;
      while (resultSet.next()) {
        for (int i = 1; i <= columnCount; i++) {
          data[z][i - 1] = St.trimToLengthIndicatorRight(resultSet.getString(i), trimLength);
        }
        z++;
      }
      return data;
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   *
   *
   * @param resultSet
   * @return
   */
  public static List<String[]> convertToList(ResultSet resultSet) {
    List<String[]> list = new ArrayList<String[]>();
    try {
      int columnCount = resultSet.getMetaData().getColumnCount();
      while (resultSet.next()) {
        String[] row = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
          row[i - 1] = resultSet.getString(i);
        }
        list.add(row);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  /**
   *
   *
   */
  public void printTables(boolean shortened) throws Exception {
    Connection con = null;
    ResultSet resultSet = null;
    try {
      String output = "";
      con = getConnection();
      List<String> tables = getAllTables(con);
      if (tables == null) {
        return;
      }
      for (String table : tables) {
        try {
          resultSet = this.h2DB.executeQuery("SELECT * FROM " + table + ";", con);
          String[][] data = DBManager.convertTo2DArray(resultSet, 150);

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          new TextTable(getColumnNames(table, con), data).printTable(new PrintStream(outputStream),
              0);

          output += "TABLE: \"" + table + "\"\n";
          output += outputStream.toString("utf-8") + "\n\n";
        } catch (org.h2.jdbc.JdbcSQLException e) {
          // TODO:
        }
      }
      if (shortened) {
        COF.printLines(output);
      } else {
        System.out.println(output);
      }

    } catch (Exception e) {
      throw e;
    } finally {
      if (resultSet != null) {
        if (resultSet.getStatement() != null) {
          resultSet.getStatement().close();
        }
        resultSet.close();
      }
      if (con != null) {
        con.close();
      }
    }
  }

  public static final Integer TABLES_SIZE = 3;

  /**
   *
   *
   * @throws TableNotFoundException
   * @throws IOException
   * @throws FileNotFoundException
   * @throws FilePermissionException
   * @throws LuceneIndexNotFoundException
   */
  public void databaseIncomplete() throws LuceneIndexNotFoundException, FilePermissionException,
      DBPathNotFoundException, TableEmptyException, IOException, TableNotFoundException {

    // {{{ get a list of database tables
    File dbPath = this.h2DB.getDataPath();
    List<String> tables = null;
    try {
      tables = this.getAllTables();
    } catch (Exception e) {
      throw new DBPathNotFoundException(dbPath.getCanonicalPath());
    }
    // }}}

    if (tables != null) {

      // {{{ check if database path was created
      if (!dbPath.exists()) {
        throw new DBPathNotFoundException(dbPath.getCanonicalPath());
      } else if (!dbPath.canWrite() || !dbPath.canRead()) {
        throw new FilePermissionException("path: \"" + dbPath.getCanonicalPath() + "\"");
      }
      // }}}

      // {{{ check if all db tables are available
      if (tables.size() < TABLES_SIZE) {
        if (tables.isEmpty()) {
          //throw new TableNotFoundException();
        } else {
          throw new TableNotFoundException("current table list: " + tables.toString());
        }
      }
      // }}}

      // {{{ check if lucene index was created
      if (!luceneIndexAvailable()) {
        throw new LuceneIndexNotFoundException("Table list:" + tables.toString());
      }
      // }}}

      // {{{ count database entries
      if (countEntries() == 0) {
        throw new TableEmptyException();
      }
      // }}}

    }
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
    if (connection == null) {
      LOGGER.error("Connection to the database could not be established");
      return false;
    }

    if (forceReset) {
      // aa
      connection.close();
      this.h2DB.disconnect();
      // TODO: makes the next connect fail, investigate!
      //this.h2DB.removeDBDirectory();
      this.h2DB.makeDBDirectory();
      this.h2DB.initConnectionPool(true);
      connection = this.h2DB.getConnection();
    }

    if (forceReset) {
      this.h2DB.execute("DROP TABLE IF EXISTS " + FileEntryDao.TABLE_NAME, connection);
      this.h2DB.execute("DROP TABLE IF EXISTS " + FullTextDao.TABLE_NAME, connection);
      this.h2DB.execute("DROP TABLE IF EXISTS INDEXER", connection);

      if (tableExists(FileEntryDao.TABLE_NAME)) {
        // TODO:
        System.out.println("COULD NOT RESET DATABASE");
        System.exit(0);
      }
    }

    // *INDENT-OFF*
    String createStatement = "" //
       // ------------------------------------------------ //
       + "CREATE TABLE " + FileEntryDao.TABLE_NAME + " ( "
       + "ID INTEGER NOT NULL " + INCREMENT + ", "
       + FileEntryDao.COLUMN_FILE_PATH      + " VARCHAR(3000) NOT NULL, "
       + FileEntryDao.COLUMN_FILE_EXTENSION + " VARCHAR(16) NOT NULL, "
       + FileEntryDao.COLUMN_FILE_HASH      + " VARCHAR(32) NOT NULL"
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
       ;
    // *INDENT-ON*

    if (!tableExists(FileEntryDao.TABLE_NAME) && forceReset) {
      // TODO:
      // System.out.println("CREATING DATABASE: \"" + this.database +
      // "\" IN FOLDER:\n\"" + this.databaseDir
      // + "\"");
      h2DB.execute(createStatement, connection);
      Thread.sleep(50);
    }

    if (forceReset || !luceneIndexAvailable()) {
      FullTextLucene.init(connection);
      FullTextLucene.dropAll(connection);

      if (!luceneIndexAvailable()) {
        FullTextLucene.createIndex(connection, "PUBLIC", //
            FullTextDao.TABLE_NAME, FullTextDao.COLUMN_FILE_CONTENT + ", INDEX");
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
  public LuceneResult searchIndex(String luceneString, int maxResults, Connection connection)
      throws SQLException {
    if (connection == null) {
      LOGGER.error("Connection to the database could not be established");
      return null;
    }

    // https://www.drupal.org/node/375446
    LuceneResult result = new LuceneResult();

    ResultSet rs = null;
    Statement statement = null;
    try {
      rs = FullTextLucene.search(connection, luceneString, maxResults, 0, false);

      HashMap<FullText, Double> elements = new HashMap<FullText, Double>();
      List<String[]> resultRows = convertToList(rs);

      for (String[] foundEntry : resultRows) {
        if (foundEntry.length >= 2) {
          String whereStatement = foundEntry[0];
          double score = Double.parseDouble(foundEntry[1]);
          if (whereStatement != null) {
            FullText element = elementDao.findByFulltextStatement(whereStatement, maxResults,
                connection);
            if (element != null) {
              elements.put(element, score);
            }
          }
        }
      }
      if (!elements.isEmpty()) {
        result.setElements(elements);
      }
    } catch (Exception e) {
      result.setThrowable(e);
      LOGGER.info(null, e);
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
      entry = fileEntryDao.findByPath(FileTools.multiplatformPath(filePath), connection);
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
        } else if (FileType.isValidTextFile(file)) {
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
        } else if (FileType.is(file, CODE_XHTML, true) || FileType.is(file, CODE_XHTML2, true)
            || FileType.is(file, CODE_HTML, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.HTML_FILE_OPEN_CMD_, os));
        } else if (FileType.isValidXmlFile(file)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.XML_FILE_OPEN_CMD_, os));
        } else if (FileType.is(file, FileType.XOJ, true)) {
          command = this.cfg.getProp(ConfigString.getByOs(ConfigString.XOJ_FILE_OPEN_CMD_, os));
        }
        command = prepareCommand(command, file, page);
        LOGGER.info(command);
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

  /**
   *
   *
   * @param con
   * @param error
   * @return
   *
   * @throws Exception
   */
  public Integer countErrors(final Connection con, final ResultError error) throws Exception {
    ResultSet resultSet = null;
    Integer result = null;
    try {
      resultSet = this.h2DB
          .executeQuery("SELECT COUNT(*) FROM " + FullTextDao.TABLE_NAME + " WHERE "
              + FullTextDao.COLUMN_FILE_CONTENT + " = '" + error.getErrorCode() + "';", con);
      String[][] data = DBManager.convertTo2DArray(resultSet, 150);
      result = Integer.valueOf(data[0][0]);
    } catch (Exception e) {
      throw e;
    } finally {
      if (resultSet != null) {
        if (resultSet.getStatement() != null) {
          resultSet.getStatement().close();
        }
        resultSet.close();
      }
    }
    return result;
  }

  /**
   *
   *
   * @param con
   * @return
   *
   * @throws Exception
   */
  public Map<String, Integer> countExtensions(final Connection con) throws Exception {
    ResultSet resultSet = null;
    Map<String, Integer> extensions = new HashMap<String, Integer>();
    try {

      String column = FileEntryDao.COLUMN_FILE_EXTENSION;

      // *INDENT-OFF*
      resultSet = this.h2DB.executeQuery(
          "SELECT LCASE(" + column + "), "
          + "COUNT(" + column + ") AS COUNT FROM " + FileEntryDao.TABLE_NAME
          + " GROUP BY LCASE(" + column + ") ORDER BY COUNT DESC", con);
      // *INDENT-OFF*


      String[][] data = DBManager.convertTo2DArray(resultSet, 150);
      for (int i = 0; i < data.length; i++) {
          extensions.put(data[i][0], Integer.valueOf(data[i][1]));
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      if (resultSet != null) {
        if (resultSet.getStatement() != null) {
          resultSet.getStatement().close();
        }
        resultSet.close();
      }
    }
    return extensions;
  }


  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public Integer countEntries(){
    Connection connection = null;
    int entries = 0;
    try {
      connection = getConnection();
      entries = countEntries(connection);
    } catch(Exception e)
    {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e1) {
          e1.printStackTrace();
        }
      }
    }
    return entries;
  }

  /**
   *
   *
   * @param shortened
   * @param con
   *
   * @throws Exception
   */
  public Integer countEntries(final Connection con) throws Exception {
    ResultSet resultSet = null;
    Integer result = null;
    try {
      resultSet = this.h2DB.executeQuery("SELECT COUNT(*) FROM " + FileEntryDao.TABLE_NAME + ";",
          con);
      String[][] data = DBManager.convertTo2DArray(resultSet, 150);
      result = Integer.valueOf(data[0][0]);
    } catch (Exception e) {
      throw e;
    } finally {
      if (resultSet != null) {
        if (resultSet.getStatement() != null) {
          resultSet.getStatement().close();
        }
        resultSet.close();
      }
    }
    return result;
  }
}
