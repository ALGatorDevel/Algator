package si.fri.algator.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.users.UsersDatabase;
import si.fri.algator.users.UsersTools;

/**
 *
 * @author tomaz
 */
public class Database {
  
  private static Connection conn = null;
  
  
  private static   String USERNAME     = "";
  private static   String PASSWORD     = "";
  private static   String CONN_STRING  = "";
  public  static   String DATABASE     = "";
  
  private static final   String OPTIONS      = "?serverTimezone=UTC";

  
  // This method returns false if a file "$ALGATOR_ROOT/anonymous"  exists.
  // If this file exists, ALGator will not try to connect to MySQL server.
  // This file is used only in the docker container (to prevent connections
  // to mysql, because docker can not reach the server running on the host;
  // any attemp to connect to the server causes huge delays).
  public static boolean isDatabaseMode() {
    try {
      File f = new File(ATGlobal.getALGatorRoot(), "anonymous");
      return !f.exists();
    } catch (Exception e) {
      return true;
    }
  }
  
  public static Connection getConnectionToDatabase() {
    if (conn != null) return conn;        
    
    if (!isDatabaseMode()) return null;                
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
    Connection conn = getConnectionToDatabase();
    try {
      Statement stmt = (Statement) conn.createStatement();    
      ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
          return rs.getInt(1);
        }
    } catch (Exception e) {}
    return -1;
  }

  
  /**
   * Preveri obstoj baze in tabel ter pravilnost uporabniškega imena in gesla; če je vse OK, nadaljuje, sicer
   * izpiše obvestilo o napaki in konča izvajanje programa.
   */
  public static boolean databaseAccessGranted(String username, String password) {
    if (Database.isDatabaseMode()) {
      // preverim obstoj tabel -> ce ne obstajajo, jih skusam ustvariti -> ce ne gre, koncam!
      if (!UsersDatabase.databaseAndTablesExist()) {
        if (ATGlobal.verboseLevel > 0) 
          ATLog.log("The database is not initialized. Use 'java algator.Admin -init' before the first usage of ALGator.",0  );
        return false;        
      }        
      if (!UsersTools.checkUser(username, password)) {
        if (ATGlobal.verboseLevel > 0) 
          ATLog.log(String.format("Invalid username or password.", username),0  );
        return false;           
      }            
    } else if (ATGlobal.verboseLevel > 0) 
       ATLog.log("ALGator is running in the non-database mode.", 0);
    
    return true;
  } 
  
  // inicializacija vseh tabel (u_*, p_*, s_*)
  public static boolean init() {
    if (!isDatabaseMode()) return true;
    
    return UsersDatabase.init();
  }  
}
