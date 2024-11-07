package si.fri.algator.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import si.fri.algator.ausers.AUsersTools;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;

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
 
  

  private static String anonymousFileName     = "anonymous";
  private static boolean anonymousModeChecked = false;
  private static boolean anonymousMode        = false;
  // This method returns true if a file "$ALGATOR_ROOT/anonymous"  exists.
  // If this file exists, ALGator will not try to connect to MySQL server.
  // In anonymous mode all DB-related actions will be skipped, and all 
  // permissions (method can()) will be approved. In anonymous mode user 
  // can de everything. 
  public static boolean isAnonymousMode() {
    if (!anonymousModeChecked) {
      File anonymousFile = new File(new File(ATGlobal.getALGatorRoot()), anonymousFileName);
      anonymousMode = anonymousFile.exists();
      anonymousModeChecked = true;
    }
    return anonymousMode;
  }

  public static Connection getConnectionToDatabase() {
    if (isAnonymousMode()) return null;                

    if (conn == null) try {
      if (USERNAME.isEmpty() || PASSWORD.isEmpty() || CONN_STRING.isEmpty() || DATABASE.isEmpty()) {
        load_prop();  
      }

      if (USERNAME.isEmpty() || PASSWORD.isEmpty() || CONN_STRING.isEmpty() || DATABASE.isEmpty()) {
        ATLog.log("Settings for database connection missing or incorrect.", 1);
        return null;
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
 
  
  /**
   * Preveri obstoj baze in tabel ter pravilnost uporabniškega imena in gesla; če je vse OK, nadaljuje, sicer
   * izpiše obvestilo o napaki in konča izvajanje programa.
   */
  public static boolean databaseAccessGranted(String username, String password) {
    if (!Database.isAnonymousMode()) {
      // preverim obstoj tabel -> ce ne obstajajo, jih skusam ustvariti -> ce ne gre, koncam!
      if (!AUsersTools.databaseAndTablesExist()) {
        if (ATGlobal.verboseLevel > 0) 
          ATLog.log("The database is not initialized.",0  );
        return false;        
      }        
      if (!AUsersTools.checkUser(username, password)) {
        if (ATGlobal.verboseLevel > 0) 
          ATLog.log(String.format("Invalid username or password.", username),0  );
        return false;           
      }            
    } else if (ATGlobal.verboseLevel > 0) 
       ATLog.log("ALGator is running in the non-database mode.", 0);    
    return true;
  } 
}
