package mj.ocraptor.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mj.ocraptor.database.DBManager;

public class FullTextDao {

  // @formatter:off
  public static final String TABLE_NAME           = "FULLTEXT";
  public static final String COLUMN_FILE_CONTENT  = "TEXT";
  // @formatter:on

  /**
   *
   *
   *
   * @param fullText
   * @param connection
   * @return
   *
   * @throws SQLException
   */
  public boolean insert(FullText fullText, Connection connection) throws SQLException {
    if (fullText.getId() != null)
      throw new IllegalArgumentException("Element instance already inserted!");

    PreparedStatement statement = null;
    try {
      Integer fileId = fullText.getFileId();
      String text = fullText.getText();

      // @formatter:off
      String query =
          "INSERT INTO " + TABLE_NAME
          + " ( "
          + FileEntryDao.TABLE_NAME + "_ID, "
          + COLUMN_FILE_CONTENT + ", "
          + "INDEX" + ") "
          + "VALUES ( ?, ?, ? ) ";
      // @formatter:on

      statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      statement.setInt(1, fileId);
      statement.setObject(2, text);
      statement.setObject(3, "TX");
      statement.executeUpdate();

      // set generated id to Element instance
      Integer id = DaoTools.getGeneratedId(statement.getGeneratedKeys());
      if (id != null)
        fullText.setId(id);

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
  public boolean update(FullText fullText, Connection connection) throws SQLException {
    if (fullText.getId() == null)
      throw new IllegalArgumentException("Element instance already inserted!");
    PreparedStatement statement = null;
    try {
      String text = fullText.getText();

      // @formatter:off
      String query =
        "UPDATE " +
        TABLE_NAME +
        " SET " +
        COLUMN_FILE_CONTENT + "=?, " +
        " WHERE ID=?";
      // @formatter:on

      statement = connection.prepareStatement(query);
      statement.setString(1, text);
      statement.setInt(2, fullText.getId());
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
  public FullText findById(int id, Connection connection) throws SQLException {
    FullText entry = new FullText();
    entry.setId(id);
    List<FullText> entries = findByExample(entry, 1, connection);
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
  public FullText findByFulltextStatement(String fulltextStatement, int limit, Connection connection)
      throws SQLException {
    List<FullText> elements = find(new FullText(), fulltextStatement, limit, connection);
    if (elements.size() == 1) {
      return elements.get(0);
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
  public List<FullText> findByExample(FullText example, int limit, Connection connection)
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
  private List<FullText> find(final FullText example, final String fulltextStatement,
      final int limit, final Connection connection) throws SQLException {

    List<FullText> entries = new ArrayList<FullText>();
    String selectStatement = "SELECT * FROM ";

    if (fulltextStatement == null)
      selectStatement += TABLE_NAME;

    Integer id = example.getId();
    Integer fileId = example.getFileId();
    String text = example.getText();

    HashMap<String, String> atts = new HashMap<String, String>();
    atts.put("ID", id != null ? String.valueOf(id) : null);
    atts.put(FileEntryDao.TABLE_NAME + "_ID", fileId != null ? String.valueOf(fileId) : null);
    atts.put(COLUMN_FILE_CONTENT, text);

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

      List<String[]> resultRows = DBManager.convertToList(rs);
      for (String[] r : resultRows) {
        id = r[0] == null ? null : Integer.parseInt(r[0]);
        fileId = r[1] == null ? null : Integer.parseInt(r[1]);
        text = r[2];

        FullText element = new FullText(fileId, text);
        element.setId(id);
        entries.add(element);
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
