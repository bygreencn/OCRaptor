package mj.extraction.result.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import mj.database.H2Database;

public class FileEntryDao {
  public static final String TABLE_NAME = "FILES";
  public static final String COLUMN_FILE_HASH = "HASH";
  public static final String COLUMN_FILE_PATH = "PATH";

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
  public boolean insert(FileEntry entry, Connection connection)
      throws SQLException, FileNotFoundException {
    if (entry.getId() != null)
      throw new IllegalArgumentException("FileEntry instance already inserted!");

    PreparedStatement statement = null;
    try {
      String path = entry.getPath();
      File file = new File(path);
      if (!file.exists())
        throw new FileNotFoundException("File to insert not found: \""
            + file.getAbsolutePath() + "\"");
      updateHash(entry);
      String hash = entry.getHash();

      // @formatter:off
      String query =
        "INSERT INTO " +
        TABLE_NAME +
        " ( " +
        COLUMN_FILE_PATH +
        ", " +
        COLUMN_FILE_HASH +
        " ) " +
        "VALUES ( ?, ? )";
      // @formatter:on

      statement = connection.prepareStatement(query,
          Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, path);
      statement.setString(2, hash);
      statement.executeUpdate();

      // set generated id to FileEntry instance
      Integer id = DaoTools.getGeneratedId(statement.getGeneratedKeys());

      if (id != null) {
        entry.setId(id);
        connection.commit();

        List<FullText> fullTextObjects = entry.getFullTextObjects();
        if (fullTextObjects != null && !fullTextObjects.isEmpty()) {
          FullTextDao fulltextDao = new FullTextDao();
          for (FullText fulltext : fullTextObjects) {
            fulltext.setFileId(id);
            fulltextDao.insert(fulltext, connection);
          }
        }

        List<MetaData> metadata = entry.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
          MetaDataDao metaDataDao = new MetaDataDao();
          for (MetaData md : metadata) {
            md.setFileId(id);
            metaDataDao.insert(md, connection);
          }
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
  public boolean updateDirty(FileEntry entry, Connection connection)
      throws Exception {
    if (entry.getId() == null)
      throw new IllegalArgumentException("");
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
  public boolean update(FileEntry entry, Connection connection)
      throws SQLException {
    if (entry.getId() == null)
      throw new IllegalArgumentException("");

    PreparedStatement statement = null;
    try {
      // TODO: implement proper update:
      return updateDirty(entry, connection);

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
      statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FILE_PATH + " = ?;");
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
      statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME
          + " WHERE ID=?;");
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
  public FileEntry findByPageId(int id, Connection connection)
      throws SQLException {
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
  public FileEntry findByPath(String path, Connection connection)
      throws SQLException {
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
   * @throws SQLException
   */
  public List<FileEntry> findByExample(FileEntry example, int limit,
      Connection connection) throws SQLException {
    List<FileEntry> entries = new ArrayList<FileEntry>();
    String selectStatement = "SELECT * FROM " + FileEntryDao.TABLE_NAME;
    HashMap<String, String> atts = new HashMap<String, String>();
    atts.put("ID", example.getId() != null ? String.valueOf(example.getId())
        : null);
    atts.put(COLUMN_FILE_PATH, example.getPath());
    atts.put(COLUMN_FILE_HASH, example.getHash());
    selectStatement += DaoTools.getWhereStatement(atts, limit);

    ResultSet rs = null;
    Statement statement = null;
    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
      rs = statement.executeQuery(selectStatement);

      List<String[]> resultRows = H2Database.convertToList(rs);
      for (String[] r : resultRows) {
        Integer fileId = r[0] == null ? null : Integer.parseInt(r[0]);
        String filePath = r[1];
        String fileHash = r[2];

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
  public List<String> getAllFilePaths(Connection connection)
      throws SQLException {
    List<String> entries = new ArrayList<String>();
    String selectStatement = "SELECT " + COLUMN_FILE_PATH + " FROM "
        + FileEntryDao.TABLE_NAME + ";";

    ResultSet rs = null;
    Statement statement = null;
    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
      rs = statement.executeQuery(selectStatement);

      List<String[]> resultRows = H2Database.convertToList(rs);
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
   * @throws SQLException
   *
   *
   */
  public void pullMetaData(FileEntry entry, Connection connection, int limit)
      throws SQLException {
    if (entry != null) {
      MetaDataDao dao = new MetaDataDao();
      List<MetaData> md = dao.findByExample(
          new MetaData().setFileId(entry.getId()), limit, connection);
      entry.setMetadata(md);
    }
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
  public void pullFullTextObjects(FileEntry entry, Connection connection, int limitFullTextObjects,
      int limitElements) throws SQLException {
    if (entry != null) {
      FullTextDao dao = new FullTextDao();

      List<FullText> fullTextObjects = dao.findByExample(new FullText().setFileId(entry.getId()),
          limitFullTextObjects, connection);

      entry.setFullTextObjects(fullTextObjects);
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
  private boolean fileEntryExists(String filePath, Connection connection,
      boolean removeNotExisting) throws Exception {
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
  private void updateHash(FileEntry entry) {
    String path = entry.getPath();
    File file = new File(path);
    String hash = calculateMD5FromFile(file);
    entry.setHash(hash);
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws IOException
   */
  private static String calculateMD5FromFile(File file) {
    String md5 = null;
    try {
      FileInputStream fis = new FileInputStream(file);
      md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return md5;
  }

  /**
   *
   *
   * @param entry
   */
  private void removeGeneratedIds(FileEntry entry) {
    entry.setId(null);
    List<FullText> fullTextObjects = entry.getFullTextObjects();
    if (fullTextObjects != null && !fullTextObjects.isEmpty()) {
      for (FullText fullText : fullTextObjects) {
        fullText.setId(null);
      }
    }
    List<MetaData> metadata = entry.getMetadata();
    if (metadata != null && !metadata.isEmpty()) {
      for (MetaData md : metadata)
        md.setId(null);
    }
  }
}
