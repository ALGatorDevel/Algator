package si.fri.algator.users;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import si.fri.algator.database.Database;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.Password;
  
/**
 *
 * @author Gregor
 */
public class UsersDatabase {
  
    static final ArrayList<String> tabelePrograma = new ArrayList(Arrays.asList(new String[]
      {"u_entities", "u_groups", "u_group_users", "u_owners", "u_permissions", "u_permissions_group", "u_permissions_users", }));
  
 
  private static ArrayList<String> getMissingTables() {
    ArrayList<String> tabele = new ArrayList(tabelePrograma);
    
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      ResultSet rs = Database.getConnectionToDatabase().getMetaData().getTables(Database.DATABASE, null, null, new String[]{"TABLE"});
      while (rs.next()){
        String tName = rs.getString("TABLE_NAME");
        tabele.remove(tName);
      }
      return tabele;      
    } catch (Exception e) {
      return new ArrayList(tabelePrograma);
    }    
  }  
    
  public static boolean databaseAndTablesExist(){   
    return getMissingTables().isEmpty();
  }

  public static boolean init() {
    ArrayList<String> errorMsgs = new ArrayList<>();
    
    if (ATGlobal.verboseLevel > 2) ATLog.log("ALGator will now check for permission database and tables...", 0);

    if (Database.getConnectionToDatabase()==null) {
      if (ATGlobal.verboseLevel >= 2) ATLog.log("Error: Can't connect to database.", 0);
      return false;
    }

    errorMsgs.add(createDatabase()); 
    
    if (ATGlobal.verboseLevel > 2) {
      ATLog.log("Missing tables: "+Arrays.toString(getMissingTables().toArray()), 0);
    }
    for (String tabela : getMissingTables()) 
      errorMsgs.add(createTable(tabela));
    

    //add algator, everyone and permissions
    // addNewUser("algator", "algator");
    errorMsgs.add(insertEveryone());
    errorMsgs.add(insertPermission("can read", "can_read"));
    errorMsgs.add(insertPermission("can write", "can_write"));
    errorMsgs.add(insertPermission("can execute", "can_execute"));

    errorMsgs.removeAll(Collections.singleton("")); 
    
    if (!errorMsgs.isEmpty() && ATGlobal.verboseLevel >= 2) {
      ATLog.log("Error: " + Arrays.toString(errorMsgs.toArray()), 0); 
      return false;
    }
    
    return true;
  }

  
  private static String createDatabase() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      ResultSet rs = Database.getConnectionToDatabase().getMetaData().getCatalogs();

      while (rs.next()) {
        String databaseName = rs.getString(1);
        if (databaseName.equals(Database.getDatabase())) {
          if (ATGlobal.verboseLevel > 2) ATLog.log("Database already exists...",0);

          return "";
        }
      }
      rs.close();

      String sql = "CREATE DATABASE IF DOESN'T EXIST " + Database.getDatabase().toUpperCase();

      //try to create
      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Database " + Database.getDatabase() + " sucessfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createTable(String tableName) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(Database.DATABASE, null, tableName, new String[]{"TABLE"});

      //check if exists
      if (res.next() && tableName.equalsIgnoreCase(res.getString("TABLE_NAME"))) {
        
        if (ATGlobal.verboseLevel > 2) ATLog.log(String.format("Table '%s' already exists!", tableName),0);
        return "";
      }
      
      String sql = ATTools.getResourceFile(String.format("sql/create_%s.sql", tableName));
      
      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log(String.format("Table '%s' succesfully created!",tableName), 0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);
      return e.toString();
    }
  }

  public static String addNewUser(String username, String password) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      //check if user exists
      String select = String.format("SELECT * from %s.auth_user WHERE username='%s'", Database.getDatabase(), username);
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        String msg = String.format("Users %s already exists!", username);
        if (ATGlobal.verboseLevel > 2) ATLog.log(msg, 0);
        return msg;
      }

      //add algator user
      Timestamp now = new Timestamp(new Date().getTime());
      String insert = String.format("INSERT INTO auth_user (username,password,date_joined, is_superuser, is_staff, is_active, first_name, last_name, email) VALUES ('%s','%s', '%s', 0, 0, 1, '', '', '')", username, Password.encript(password), now.toString());
      int result = stmt.executeUpdate(insert);
      
      int user_id     = UsersTools.getUserID(username, false);
      int everyone_id = UsersTools.getGroupID("Everyone", false);
      String pInsert = String.format("INSERT INTO u_group_users (id_user, id_group) VALUES (%d,%d)", user_id, everyone_id);
      result = stmt.executeUpdate(pInsert);

      ATLog.log(String.format("User %s added!", username),0);

      return "";
    } catch (SQLException e) {
      ATLog.log("Error - " + e, 0);
      return e.toString();
    }
  }

    public static String passwd(String username, String newPassword) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      //check if user exists
      String select = String.format("SELECT * from %s.auth_user WHERE username='%s'", Database.getDatabase(), username);
      ResultSet rs = stmt.executeQuery(select);

      if (!rs.next()) {
        String msg = String.format("Users %s does not exist!", username);
        if (ATGlobal.verboseLevel > 2) ATLog.log(msg, 0);
        return msg;
      }

      //add algator user
      String insert = String.format("UPDATE auth_user SET password='%s' WHERE name='%s'", Password.encript(newPassword), username);

      int result = stmt.executeUpdate(insert);

      if (ATGlobal.verboseLevel > 2) ATLog.log(String.format("Password for user %s changed!", username),0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);
      return e.toString();
    }
  }

  
  private static String insertEveryone() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".u_groups WHERE name='Everyone'";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Group Everyone already exists!",0);
        return "";
      }

      //add everyone
      String insert = "INSERT INTO u_groups (name,status) VALUES ('Everyone',1);";

      int result = stmt.executeUpdate(insert);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Group Everyone added!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);
      return e.toString();
    }
  }

  private static String insertPermission(String permission, String permission_code) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".u_permissions WHERE permission_code='" + permission_code + "'";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Permission " + permission + " already exists!",0);
        return "";
      }

      //add everyone
      String insert = "INSERT INTO u_permissions (permission,permission_code) VALUES ('" + permission + "','" + permission_code + "');";

      int result = stmt.executeUpdate(insert);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Permission " + permission + " added!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);
      return e.toString();
    }
  }
}
