package mj.ocraptor.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;

import mj.ocraptor.events.EventManager;

public class H2Database {

  private static final String JDBC_EMBEDDED_DRIVER = "org.h2.Driver";
  private File dataPath;
  private JdbcConnectionPool connectionPool;

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
      H2Database.class);

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
      // *INDENT-OFF*
      connectionPool = JdbcConnectionPool.create(
          "jdbc:h2:"
          + dataPath.getAbsolutePath()
          + ";CREATE=TRUE", "sa", "sa");
      // *INDENT-ON*
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
      if (dataPath.getParentFile().exists()) {
        FileUtils.deleteDirectory(dataPath.getParentFile());
      }
      dataPath.getParentFile().mkdir();
    } catch (IOException e) {
      LOGGER.error("Remove database error", e);
    }
    return this;
  }

  /**
   *
   *
   * @return
   */
  public H2Database makeDBDirectory() {
    if (!dataPath.getParentFile().exists()) {
      dataPath.getParentFile().mkdir();
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

  // ------------------------------------------------ //

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
   * @param query
   *
   * @throws Exception
   */
  public ResultSet executeQuery(String query, Connection connection) throws Exception {
    Connection con = null;
    Statement statement = null;
    try {
      con = (connection == null) ? getConnection() : connection;
      statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
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
}
