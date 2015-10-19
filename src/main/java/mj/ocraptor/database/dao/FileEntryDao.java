package mj.ocraptor.database.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import mj.ocraptor.database.DBManager;
import mj.ocraptor.database.H2Database;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.tools.St;

public class FileEntryDao {
  public static final String TABLE_NAME = "FILES";
  public static final String COLUMN_FILE_HASH = "HASH";
  public static final String COLUMN_FILE_EXTENSION = "EXT";
  public static final String COLUMN_FILE_PATH = "PATH";

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(FileEntryDao.class);

  /**
   *
   *
   * @param entry
   * @param connection
   * @return
   *
   * @throws SQLException
   * @throws FileNotFoundException
   */
  public boolean insert(FileEntry entry, Connection connection) throws SQLException,
      FileNotFoundException {
    if (entry.getId() != null)
      throw new IllegalArgumentException("FileEntry instance already inserted!");

    PreparedStatement statement = null;
    try {
      String path = FileTools.multiplatformPath(entry.getPath());
      File file = new File(path);

      if (!file.exists()) {
        throw new FileNotFoundException("File to insert not found: \"" + file.getPath() + "\"");
      }
      updateHash(entry);
      String hash = entry.getHash();

      // *INDENT-OFF*
      String query =
        "INSERT INTO " +
        TABLE_NAME +
        " ( " +
        COLUMN_FILE_PATH +
        ", " +
        COLUMN_FILE_EXTENSION +
        ", " +
        COLUMN_FILE_HASH +
        " ) " +
        "VALUES ( ?, ?, ? )";
      // *INDENT-ON*

      statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, path);
      statement.setString(2, FileTools.getFileExtension(path));
      statement.setString(3, hash);
      statement.executeUpdate();

      // set generated id to FileEntry instance
      Integer id = DaoTools.getGeneratedId(statement.getGeneratedKeys());

      if (id != null) {
        entry.setId(id);
        connection.commit();

        FullText fullText = entry.getFullText();
        if (fullText != null && !fullText.isEmpty() && fullText.getId() == null) {
          fullText.setFileId(id);
          FullTextDao fulltextDao = new FullTextDao();
          fulltextDao.insert(fullText, connection);
        }
        return true;
      }

    } finally {
      if (statement != null) {
        statement.close();
      }
    }
    return false;
  }

  /**
   *
   *
   * @param entry
   * @param connection
   * @return
   * @throws Exception
   */
  public boolean updateDirty(FileEntry entry, Connection connection) throws Exception {
    if (entry.getId() == null)
      throw new IllegalArgumentException("Entry ID is null");
    this.removeById(entry.getId(), connection);
    this.removeGeneratedIds(entry);
    return this.insert(entry, connection);
  }

  /**
   *
   *
   * @param entry
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public boolean update(final FileEntry entry, final Connection connection) throws SQLException {
    if (entry.getId() == null) {
      throw new IllegalArgumentException("Entry ID is null");
    }

    PreparedStatement statement = null;
    try {
      this.updateHash(entry); // update md5 hash in case the file changed

      // TODO: implement proper update:
      return updateDirty(entry, connection);

      // ------------------------------------------------ //
      // updateHash(entry);
      // String path = entry.getPath(), hash = entry.getHash();
      // Integer id = entry.getId();

      // String query = "UPDATE " + TABLE_NAME //
      // + " SET " + COLUMN_FILE_PATH + "=?, " //
      // + COLUMN_FILE_HASH + "=? " //
      // + " WHERE ID=?";

      // statement = connection.prepareStatement(query);
      // statement.setString(1, path);
      // statement.setString(2, hash);
      // statement.setInt(3, id);

      // statement.executeUpdate();
      // connection.commit();
      // ------------------------------------------------ //
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (statement != null) {
        statement.close();
      }
    }
    return false;
  }

  /**
   *
   *
   * @param query
   *
   * @throws Exception
   */
  public void removeByPath(String path, Connection connection) throws Exception {
    PreparedStatement statement = null;
    try {
      statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE "
          + COLUMN_FILE_PATH + " = ?;");
      statement.setString(1, path);
      statement.executeUpdate();
      connection.commit();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (statement != null)
        statement.close();
    }
  }

  /**
   *
   *
   * @param query
   *
   * @throws Exception
   */
  public void removeById(Integer id, Connection connection) throws Exception {
    PreparedStatement statement = null;
    try {
      statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE ID=?;");
      statement.setInt(1, id);
      statement.executeUpdate();
      connection.commit();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (statement != null)
        statement.close();
    }
  }

  /**
   *
   *
   * @param id
   * @return
   * @throws SQLException
   */
  public FileEntry findById(int id, Connection connection) throws SQLException {
    FileEntry entry = new FileEntry();
    entry.setId(id);
    List<FileEntry> entries = findByExample(entry, 1, connection);
    return entries.size() == 1 ? entries.get(0) : null;
  }

  /**
   *
   *
   * @return
   * @throws SQLException
   */
  public FileEntry findByPageId(int id, Connection connection) throws SQLException {
    FullTextDao fullTextDao = new FullTextDao();
    FullText fullText = fullTextDao.findById(id, connection);
    if (fullText != null) {
      return findById(fullText.getFileId(), connection);
    }
    return null;
  }

  /**
   *
   *
   * @param path
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public FileEntry findByPath(String path, Connection connection) throws SQLException {
    FileEntry entry = new FileEntry();
    entry.setPath(path);
    List<FileEntry> entries = findByExample(entry, 1, connection);
    return entries.size() == 1 ? entries.get(0) : null;
  }

  /**
   *
   *
   * @param example
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public List<FileEntry> findByExample(FileEntry example, Connection connection)
      throws SQLException {
    return findByExample(example, 1, connection);
  }

  /**
   *
   *
   * @param example
   * @param connection
   * @return
   * @throws SQLException
   */
  public List<FileEntry> findByExample(FileEntry example, int limit, Connection connection)
      throws SQLException {
    List<FileEntry> entries = new ArrayList<FileEntry>();
    String selectStatement = "SELECT * FROM " + FileEntryDao.TABLE_NAME;
    HashMap<String, String> atts = new HashMap<String, String>();
    atts.put("ID", example.getId() != null ? String.valueOf(example.getId()) : null);
    atts.put(COLUMN_FILE_PATH, example.getPath());
    atts.put(COLUMN_FILE_HASH, example.getHash());
    selectStatement += DaoTools.getWhereStatement(atts, limit);

    ResultSet rs = null;
    Statement statement = null;
    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
      rs = statement.executeQuery(selectStatement);

      List<String[]> resultRows = DBManager.convertToList(rs);
      for (String[] r : resultRows) {
        Integer fileId = r[0] == null ? null : Integer.parseInt(r[0]);
        String filePath = r[1];
        String fileHash = r[3];

        if (fileEntryExists(filePath, connection, true)) {
          FileEntry entry = new FileEntry(filePath, fileHash);
          entry.setId(fileId);
          entries.add(entry);
        }
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
    return entries;
  }

  /**
   *
   *
   * @param example
   * @param limit
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public List<String> getAllFilePaths(Connection connection) throws SQLException {
    if (connection == null) {
      LOGGER.error("Connection to the database could not be established");
      return null;
    }

    List<String> entries = new ArrayList<String>();
    String selectStatement = "SELECT " + COLUMN_FILE_PATH + " FROM " + FileEntryDao.TABLE_NAME
        + ";";

    ResultSet rs = null;
    Statement statement = null;
    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
      rs = statement.executeQuery(selectStatement);

      List<String[]> resultRows = DBManager.convertToList(rs);
      for (String[] r : resultRows) {
        entries.add(r[0]);
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
    return entries;
  }

  /**
   *
   *
   * @param entry
   * @param connection
   * @param limit
   *
   * @throws SQLException
   */
  public void pullFullTextObjects(final FileEntry entry, final Connection connection,
      final int limitFullTextObjects, final int limitElements) throws SQLException {
    if (entry != null) {
      FullTextDao dao = new FullTextDao();
      List<FullText> fullTextObjects = dao.findByExample(new FullText().setFileId(entry.getId()),
          limitFullTextObjects, connection);
      //
      if (!fullTextObjects.isEmpty()) {
        entry.setFullText(fullTextObjects.get(0));
      }
    }
  }

  /**
   *
   *
   * @param filePath
   * @param connection
   * @param removeNotExisting
   * @return
   * @throws Exception
   */
  private boolean fileEntryExists(String filePath, Connection connection, boolean removeNotExisting)
      throws Exception {
    if (filePath != null && !filePath.trim().isEmpty()) {
      if (!(new File(filePath).exists())) {
        if (removeNotExisting) {
          removeByPath(filePath, connection);
        }
        return false;
      }
    }
    return true;
  }

  /**
   *
   *
   * @param entry
   */
  private void updateHash(final FileEntry entry) {
    final String path = entry.getPath();
    final File file = new File(path);
    final String hash = FileTools.calculateMD5FromFile(file);
    entry.setHash(hash);
  }

  /**
   *
   *
   * @param entry
   */
  private void removeGeneratedIds(FileEntry entry) {
    entry.setId(null);
    final FullText fullText = entry.getFullText();
    if (fullText != null) {
      fullText.setId(null);
    }
  }
}
