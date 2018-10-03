package si.fri.algotest.users;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import si.fri.algotest.entities.EAlgatorConfig;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;

/**
 *
 * @author Gregor
 */
public class PermTools {

  private static   String USERNAME     = "";
  private static   String PASSWORD     = "";
  private static   String CONN_STRING  = "";
  private static   String DATABASE     = "";
  
  private   static String OPTIONS      = "?autoReconnect=true&useSSL=false";

  
  public static Connection connectToDatabase() {
    Connection conn = null;
    
    try {
      if (USERNAME.isEmpty() || PASSWORD.isEmpty() || CONN_STRING.isEmpty() || DATABASE.isEmpty()) {
        load_prop();
      }

      if (USERNAME.isEmpty() || PASSWORD.isEmpty() || CONN_STRING.isEmpty() || DATABASE.isEmpty()) {
        ATLog.log("Settings for database connection missing or incorrect.", 1);
        return conn;
      }

      conn = DriverManager.getConnection(CONN_STRING + "/" + DATABASE + OPTIONS, USERNAME, PASSWORD);
    } catch (SQLException e) {
      ATLog.log(e.toString(), 1);
    }
    return conn;
  }
  
  public static String getDatabase() {
    return DATABASE;
  }

  public static void load_prop() {
    EAlgatorConfig algatorConfig = EAlgatorConfig.getConfig();
    HashMap<String, Object> databaseInfo = algatorConfig.getDatabaseServerInfo();

    CONN_STRING = (String) databaseInfo.get("Connection");
    DATABASE    = (String) databaseInfo.get("Database");    
    USERNAME    = (String) databaseInfo.get("Username");
    PASSWORD    = (String) databaseInfo.get("Password");
  }
  
  public static int getID(String sql) {
    Connection conn = connectToDatabase();
    try {
      Statement stmt = (Statement) conn.createStatement();    
      ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
          return rs.getInt(1);
        }
    } catch (Exception e) {}
    return -1;
  }

  public static boolean setProjectPermissions(String proj_name) {
    Connection conn = connectToDatabase();
    if (conn == null) {
      return false;
    }

    int id_group = -1;
    int id_project = -1;
    int id_permission = -1;

    try {
      Statement stmt = (Statement) conn.createStatement();
      String proj_insert = "INSERT INTO " + DATABASE + ".entities(name,type) VALUES ('" + proj_name + "',1)";
      int result = stmt.executeUpdate(proj_insert, stmt.RETURN_GENERATED_KEYS);

      if (result > 0) {
        //get group id, entity id and permission id
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
          id_project = rs.getInt(1);
        }

        id_group      = getID("SELECT * from " + DATABASE + ".groups WHERE name='Everyone'");
        id_permission = getID("SELECT * from " + DATABASE + ".permissions WHERE permission_code='can_read'");
        
        String insert = "INSERT INTO " + DATABASE + ".permissions_group(id_group,id_entity,id_permission) VALUES (" + id_group + "," + id_project + "," + id_permission + ")";
        stmt.executeUpdate(insert);

        insert = "INSERT INTO " + DATABASE + ".owners(id_owner,id_entity) VALUES (1," + id_project + ")";
        result = stmt.executeUpdate(insert);

        if (result > 0) {
          ATLog.log("Project added to database", 1);
          return true;
        } else {
          ATLog.log("Error while adding project to database", 1);
          return true;
        }

      } else {
        ATLog.log("Error while adding project!", 1);
        return false;
      }

    } catch (SQLException e) {
      ATLog.log(e.toString(),1);
    }
    return false;
  }

  public static boolean setAlgorithmPermissions(String proj_name, String alg_name) {
    Connection conn = connectToDatabase();
    if (conn == null) {
      return false;
    }

    int id_group = -1;
    int id_project = -1;
    int id_permission = -1;
    int id_algorithm = -1;

    try {
      Statement stmt = (Statement) conn.createStatement();

      //get project id
      String select = "SELECT * from " + DATABASE + ".entities WHERE name='" + proj_name + "'";
      ResultSet rs = stmt.executeQuery(select);
      if (rs.next()) {
        id_project = rs.getInt(1);
      }

      String proj_insert = "INSERT INTO " + DATABASE + ".entities(name,type,id_parent) VALUES ('" + alg_name + "',2," + id_project + ")";
      int result = stmt.executeUpdate(proj_insert, stmt.RETURN_GENERATED_KEYS);

      if (result > 0) {
        //get group id, entity id and permission id
        rs = stmt.getGeneratedKeys();
        if (rs.next()) {
          id_algorithm = rs.getInt(1);
        }

        select = "SELECT * from " + DATABASE + ".groups WHERE name='Everyone'";
        rs = stmt.executeQuery(select);
        if (rs.next()) {
          id_group = rs.getInt(1);
        }

        select = "SELECT * from " + DATABASE + ".permissions WHERE permission_code='can_read'";
        rs = stmt.executeQuery(select);
        if (rs.next()) {
          id_permission = rs.getInt(1);
        }
        String insert = "INSERT INTO " + DATABASE + ".permissions_group(id_group,id_entity,id_permission) VALUES (" + id_group + "," + id_project + "," + id_permission + ")";
        result = stmt.executeUpdate(insert);

        if (result > 0) {
          ATLog.log("Algorithm added to database!",1);
          return true;
        } else {
          ATLog.log("Algorithm added to databse but failed to add permission can_read to everyone!",1);
          return true;
        }
      } else {
        ATLog.log("Error while adding algorithm!",1);
        return false;
      }
    } catch (SQLException e) {
      ATLog.log(e.toString(),1);
    }
    return false;
  }
}
