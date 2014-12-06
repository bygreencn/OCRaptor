package mj.extraction.result.document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mj.database.H2Database;

public class MetaDataDao {

  public static final String TABLE_NAME = "META_DATA";
  public static final String COLUMN_METADATA_KEY = "KEY";
  public static final String COLUMN_METADATA_VALUE = "VALUE";

  /**
   *
   *
   *
   * @param md
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public boolean insert(MetaData md, Connection connection) throws SQLException {
    if (md.getId() != null)
      throw new IllegalArgumentException("MetaData instance already inserted!");

    PreparedStatement statement = null;
    try {
      Integer fileId = md.getFileId();
      String mdKey = md.getKey();
      String mdValue = md.getValue();

      String query = "INSERT INTO " + TABLE_NAME //
          + " ( " //
          + FileEntryDao.TABLE_NAME + "_ID, " //
          + COLUMN_METADATA_KEY + ", " //
          + COLUMN_METADATA_VALUE + ", "//
          + "INDEX" + " ) VALUES ( ?, ?, ?, ? ) ";

      statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

      statement.setInt(1, fileId);
      statement.setObject(2, mdKey);
      statement.setObject(3, mdValue);
      statement.setObject(4, "MD");

      statement.executeUpdate();
      // set generated id to MetaData instance
      Integer id = DaoTools.getGeneratedId(statement.getGeneratedKeys());
      if (id != null)
        md.setId(id);

      connection.commit();
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
   *
   * @throws SQLException
   */
  public boolean update(MetaData md, Connection connection) throws SQLException {
    if (md.getId() == null)
      throw new IllegalArgumentException("MetaData instance already inserted!");

    PreparedStatement statement = null;

    try {
      Integer id = md.getId();
      Integer fileId = md.getFileId();
      String mdKey = md.getKey();
      String mdValue = md.getValue();

      String query = "UPDATE " + TABLE_NAME //
          + " SET " //
          + FileEntryDao.TABLE_NAME + "_ID=?, " //
          + COLUMN_METADATA_KEY + "=?, " //
          + COLUMN_METADATA_VALUE + "=?" //
          + " WHERE ID=?";
      statement = connection.prepareStatement(query);

      statement.setInt(1, fileId);
      statement.setString(2, mdKey);
      statement.setString(3, mdValue);
      statement.setInt(4, id);

      statement.executeUpdate();
      connection.commit();
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
  public void remove(Integer id, Connection connection) throws Exception {
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
  public MetaData findById(int id, Connection connection) throws SQLException {
    MetaData entry = new MetaData();
    entry.setId(id);
    List<MetaData> entries = findByExample(entry, 1, connection);
    return entries.size() == 1 ? entries.get(0) : null;
  }

  /**
   *
   *
   * @param fulltextStatement
   * @param limit
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public MetaData findByFulltextStatement(String fulltextStatement, int limit, Connection connection)
      throws SQLException {
    List<MetaData> meta = find(new MetaData(), fulltextStatement, limit, connection);
    if (meta.size() == 1) {
      return meta.get(0);
    }
    return null;
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
  public List<MetaData> findByExample(MetaData example, int limit, Connection connection)
      throws SQLException {
    return find(example, null, limit, connection);
  }

  /**
   *
   *
   * @param example
   * @param connection
   * @return
   * @throws SQLException
   */
  private List<MetaData> find(MetaData example, final String fulltextStatement, int limit,
      Connection connection) throws SQLException {
    List<MetaData> entries = new ArrayList<MetaData>();
    String selectStatement = "SELECT * FROM ";

    if (fulltextStatement == null)
      selectStatement += TABLE_NAME;

    Integer id = example.getId();
    Integer fileId = example.getFileId();
    String mdKey = example.getKey();
    String mdValue = example.getValue();

    HashMap<String, String> atts = new HashMap<String, String>();
    atts.put("ID", id != null ? String.valueOf(id) : null);
    atts.put(FileEntryDao.TABLE_NAME + "_ID", fileId != null ? String.valueOf(fileId) : null);
    atts.put(COLUMN_METADATA_KEY, mdKey);
    atts.put(COLUMN_METADATA_VALUE, mdValue);

    if (fulltextStatement == null) {
      selectStatement += DaoTools.getWhereStatement(atts, limit);
    } else {
      selectStatement += fulltextStatement;
    }

    ResultSet rs = null;
    Statement statement = null;
    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
      rs = statement.executeQuery(selectStatement);

      List<String[]> resultRows = H2Database.convertToList(rs);
      for (String[] r : resultRows) {

        id = r[0] == null ? null : Integer.parseInt(r[0]);
        fileId = r[1] == null ? null : Integer.parseInt(r[1]);
        mdKey = r[2];
        mdValue = r[3];

        MetaData md = new MetaData(fileId, mdKey, mdValue);
        md.setId(id);
        entries.add(md);
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
}
