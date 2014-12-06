package mj.database;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import mj.events.EventManager;
import mj.tools.StringTools;

import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;

import dnl.utils.text.table.TextTable;

public class H2Database {
  private static final String JDBC_EMBEDDED_DRIVER = "org.h2.Driver";
  private File dataPath;
  private JdbcConnectionPool connectionPool;

  /**
   *
   *
   * @param dataPath
   * @param dbName
   */
  public H2Database(File dataPath, String dbName) {
    this.dataPath = new File(dataPath + File.separator + dbName, dbName);
  }

  /**
   *
   *
   * @return
   *
   * @throws ClassNotFoundException
   */
  public H2Database initConnectionPool() throws ClassNotFoundException {
    return initConnectionPool(false);
  }

  /**
   *
   *
   * @return
   * @throws ClassNotFoundException
   */
  public H2Database initConnectionPool(boolean force) throws ClassNotFoundException {
    Class.forName(JDBC_EMBEDDED_DRIVER);

    if (force && connectionPool != null) {
      connectionPool.dispose();
    }

    if (connectionPool == null || force) {
      connectionPool = JdbcConnectionPool.create("jdbc:h2:" + dataPath.getAbsolutePath()
          + ";CREATE=TRUE", "sa", "sa");
      connectionPool.setMaxConnections(50);
    }

    return this;
  }

  /**
   *
   *
   * @return
   */
  public H2Database removeDBDirectory() {
    try {
      this.disconnect();
      if (dataPath.getParentFile().exists()) {
        FileUtils.deleteDirectory(dataPath.getParentFile());
      }
      dataPath.getParentFile().mkdir();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return this;
  }

  /**
   *
   * @return
   */
  public Connection getConnection() {
    Connection connection = null;
    try {
      try {
        connection = this.connectionPool.getConnection();
      } catch (IllegalStateException e) {
        if (e.getMessage().contains("Connection pool has been disposed.")) {
          this.initConnectionPool(true);
          connection = this.connectionPool.getConnection();
        } else {
          throw e;
        }
      }
    } catch (Exception e) {
      EventManager.instance().databaseConnectError(e, dataPath.getPath());
    }
    return connection;
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
   * @param query
   * @return
   *
   * @throws Exception
   */
  public ResultSet executeQuery(String query) throws Exception {
    return this.executeQuery(query, null);
  }

  /**
   *
   *
   * @param query
   *
   * @throws Exception
   */
  public void execute(String query) throws Exception {
    this.execute(query, null);
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
      ResultSet rs = executeQuery("SELECT * FROM " + tableName + ";", con);
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
          data[z][i - 1] = StringTools.trimToLengthIndicatorRight(resultSet.getString(i),
              trimLength);
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
   * @param query
   *
   * @throws Exception
   */
  public ResultSet executeQuery(String query, Connection connection) throws Exception {
    Connection con = null;
    Statement statement = null;
    try {
      con = (connection == null) ? getConnection() : connection;
      statement = con
          .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      return statement.executeQuery(query);
    } catch (Exception e) {
      throw e;
    } finally {
      if (connection == null && con != null) {
        con.close();
      }
    }
  }

  /**
   *
   *
   */
  public void printTables() throws Exception {
    Connection con = null;
    ResultSet resultSet = null;
    try {
      String output = "";
      con = getConnection();
      List<String> tables = getAllTables(con);
      for (String table : tables) {
        try {
          resultSet = executeQuery("SELECT * FROM " + table + ";", con);
          String[][] data = H2Database.convertTo2DArray(resultSet, 150);

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          new TextTable(getColumnNames(table, con), data).printTable(new PrintStream(outputStream),
              0);

          output += "TABLE: \"" + table + "\"\n";
          output += outputStream.toString("utf-8") + "\n\n";
        } catch (org.h2.jdbc.JdbcSQLException e) {
          // TODO:
        }
      }
      System.out.println(output);
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

  /**
   *
   *
   * @param query
   *
   * @throws Exception
   */
  public void execute(String query, Connection connection) throws Exception {
    Connection con = null;
    Statement statement = null;
    try {
      con = (connection == null) ? getConnection() : connection;
      statement = con.createStatement();
      statement.executeUpdate(query);
      con.commit();
    } catch (Exception e) {
      throw e;
    } finally {
      if (statement != null) {
        statement.close();
      }
      if (connection == null && con != null) {
        con.close();
      }
    }
  }

  /**
   *
   *
   * @return
   */
  public boolean disconnect() {
    try {
      this.connectionPool.dispose();
      DriverManager.getConnection("jdbc:h2:;shutdown=true");
    } catch (SQLException e) {
      // TODO:
      if (e.getMessage().equals("Derby system shutdown."))
        return true;
    }
    return false;
  }

  /**
   * @return the dataPath
   */
  public File getDataPath() {
    return dataPath;
  }
}
