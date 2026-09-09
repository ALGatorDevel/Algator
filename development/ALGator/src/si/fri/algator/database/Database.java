package si.fri.algator.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
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

  private static   String USERNAME     = "";
  private static   String PASSWORD     = "";
  private static   String CONN_STRING  = "";
  public  static   String DATABASE     = "";

  private static final   String OPTIONS      = "?serverTimezone=UTC";

  // data source (connections pool). Each caller must return its borrowed
  // Connection (try-with-resources / explicit close()) -- close() on a
  // pooled connection returns it to the pool, it does not disconnect.
  // Replaces the previous single shared static Connection, which was not
  // thread-safe: concurrent requests sharing one Connection/Statement/
  // ResultSet caused "ResultSet is from UPDATE. No Data.", "No operations
  // allowed after connection closed", and columnDefinition NPEs whenever two
  // requests touched the DB (e.g. permission checks) at the same time.
  static HikariDataSource connectionPool;
  private static synchronized void initConnectionPool() {
    if (connectionPool != null) return;

    if (USERNAME.isEmpty() || PASSWORD.isEmpty() || CONN_STRING.isEmpty() || DATABASE.isEmpty()) {
        load_prop();
    }
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(CONN_STRING + "/" + DATABASE + OPTIONS);
    config.setUsername(USERNAME);
    config.setPassword(PASSWORD);
    config.setMaximumPoolSize(16);   // Maximum number of connections in the pool
    config.setMinimumIdle(2);        // Minimum number of idle connections
    config.setIdleTimeout(30000);    // Idle timeout (30 seconds)
    config.setValidationTimeout(5000);
    config.setConnectionTimeout(10000);

    // optimization
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("useLocalSessionState", "true");
    config.addDataSourceProperty("rewriteBatchedStatements", "true");
    config.addDataSourceProperty("cacheResultSetMetadata", "true");
    config.addDataSourceProperty("cacheServerConfiguration", "true");
    config.addDataSourceProperty("elideSetAutoCommits", "true");
    config.addDataSourceProperty("maintainTimeStats", "false");

    connectionPool = new HikariDataSource(config);
  }

  public static Connection getConnectionToDatabase() {
    if (isAnonymousMode()) return null;
    try {
      if (connectionPool == null)
        initConnectionPool();
      return connectionPool.getConnection();
    } catch (Exception e1) {
      ATLog.log("DB connection failed: " + e1);
      return null;
    }
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
          ATLog.log("The database is not initialized.");
        return false;        
      }        
      if (!AUsersTools.checkUser(username, password)) {
        if (ATGlobal.verboseLevel > 0) 
          ATLog.log(String.format("Invalid username or password.", username));
        return false;           
      }            
    } else if (ATGlobal.verboseLevel > 0) 
       ATLog.log("ALGator is running in the non-database mode.");    
    return true;
  } 
  
  //////////////// **** ANONYMOUS mode checking **** //////////////////
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
}
