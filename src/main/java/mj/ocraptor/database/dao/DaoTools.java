package mj.ocraptor.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import mj.ocraptor.database.DBManager;

public class DaoTools {

  /**
   *
   *
   * @param set
   * @return
   * @throws SQLException
   */
  public static Integer getGeneratedId(ResultSet set) throws SQLException {
    Integer id = null;
    try {
      List<String[]> keyList = DBManager.convertToList(set);
      if (keyList != null && keyList.size() == 1) {
        String[] keys = keyList.get(0);
        if (keys.length == 1) {
          id = Integer.parseInt(keys[0]);
        }
      }
    } finally {
      if (set != null) {
        set.close();
      }
    }
    return id;
  }

  /**
   *
   *
   * @param atts
   * @return
   */
  public static String getWhereStatement(HashMap<String, String> atts, int limit) {
    StringBuffer statement = new StringBuffer();
    boolean emptyInstance = true;

    for (String value : atts.values()) {
      if (value != null)
        emptyInstance = false;
    }

    if (!emptyInstance) {
      statement.append(" WHERE ");
    }

    for (String key : atts.keySet()) {
      String value = atts.get(key);
      if (value != null && !value.trim().isEmpty()) {
        statement.append(key + "='" + value + "' AND ");
      }
    }

    if (!emptyInstance) {
      statement.replace(statement.length() - 4, statement.length(), "");
    }

    statement.append(" FETCH FIRST " + limit + " ROWS ONLY;");
    return statement.toString();
  }
}
