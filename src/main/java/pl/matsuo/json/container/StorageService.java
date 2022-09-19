package pl.matsuo.json.container;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.h2.Driver;

@Slf4j
public class StorageService {

  final Driver h2;
  final Connection connection;

  public StorageService() {
    try {
      h2 = new Driver();
      connection = h2.connect("jdbc:h2:~/test", new Properties());

      String schema = IOUtils.resourceToString("/schema.sql", StandardCharsets.UTF_8);
      String[] split = schema.split("\r?\n\r?\n");
      Statement statement = connection.createStatement();
      for (String statementString : split) {
        log.info("Executing statement " + statementString);
        statement.execute(statementString);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void insert(Tables table, String fields, String values) {
    execute("INSERT INTO tbl_" + table.name() + " (" + fields + ") VALUES (" + values + ")");
  }

  public Integer findId(Tables table, String condition) {
    try {
      ResultSet resultSet = internalSelect(table, "id", condition);
      if (resultSet == null || !resultSet.next()) {
        return null;
      }
      return resultSet.getInt(1);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public ResultSet findById(Tables table, int id, String fields) {
    return select(table, fields, "id = " + id);
  }

  public ResultSet select(Tables table, String fields, String condition) {
    try {
      ResultSet resultSet = internalSelect(table, fields, condition);
      if (resultSet == null || !resultSet.next()) {
        return null;
      }
      return resultSet;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private ResultSet internalSelect(Tables table, String fields, String condition)
      throws SQLException {
    return execute("SELECT " + fields + " FROM tbl_" + table.name() + " WHERE " + condition);
  }

  public ResultSet execute(String query) {
    try {
      PreparedStatement statement = connection.prepareStatement(query);
      statement.execute();
      return statement.getResultSet();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
