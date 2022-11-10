package si.fri.algotest.users;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.TreeSet;
import si.fri.algotest.database.Database;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.tools.ATTools;
import si.fri.algotest.tools.Password;
  
/**
 *
 * @author Gregor
 */
public class UsersDatabase {
 
  public static boolean databaseAndTablesExist(){   
    TreeSet<String> tabele = new TreeSet(Arrays.asList(new String[]
      {"u_entities", "u_groups", "u_group_users", "u_owners", "u_permissions", "u_permissions_group", "u_permissions_users", "u_users"}));
    
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      ResultSet rs = Database.getConnectionToDatabase().getMetaData().getTables(null, null, "u_%", new String[]{"TABLE"});
      while (rs.next()){
        tabele.remove(rs.getString("TABLE_NAME"));
      }
      return tabele.isEmpty();
      
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean init() {
    if (ATGlobal.verboseLevel > 2) ATLog.log("ALGator will now check for permission database and tables...", 0);

    if (Database.getConnectionToDatabase()==null) {
      if (ATGlobal.verboseLevel >= 2) ATLog.log("Error: Can't connect to database.", 0);
      return false;
    }

    String errorMsg="";
    errorMsg = createDatabase() + (errorMsg.isEmpty() ? "" : "; "+errorMsg);

    errorMsg = createEntities()          + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createGroups()            + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createUsersTable()             + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createPermissions()       + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createGroupsUsers()       + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createPermissionsGroup()  + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createPermissionsUsers()  + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = createOwners()            + (errorMsg.isEmpty() ? "" : ";"+errorMsg);

    //add algator, everyone and permissions
    // addNewUser("algator", "algator");
    errorMsg = insertEveryone()                               + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = insertPermission("can read", "can_read")       + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = insertPermission("can write", "can_write")     + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    errorMsg = insertPermission("can execute", "can_execute") + (errorMsg.isEmpty() ? "" : ";"+errorMsg);
    
    if (!errorMsg.isEmpty() && ATGlobal.verboseLevel >= 2) {
      ATLog.log("Error: " + errorMsg, 0);
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

  private static String createEntities() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_ENTITIES",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table Entities already exists!",0);
        return "";
      }

      //entities
      String sql = "CREATE TABLE IF NOT EXISTS `u_entities` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`name` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	`type` TINYINT(4) NOT NULL DEFAULT '1' COMMENT '1-project, 2-algorithm, 3-testet',\n"
              + "	`id_parent` INT(11) NULL DEFAULT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_entities_entities` (`id_parent`),\n"
              + "	CONSTRAINT `FK_entities_entities` FOREIGN KEY (`id_parent`) REFERENCES `u_entities` (`id`) ON UPDATE CASCADE ON DELETE SET NULL\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table Entities succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createGroups() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_GROUPS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table Groups already exists!",0);
        return "";
      }

      String sql = "CREATE TABLE `u_groups` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`name` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	`status` INT(1) NOT NULL DEFAULT '0' COMMENT '0 - active, 1 - inactive',\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	UNIQUE INDEX `name` (`name`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table AGroups succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createGroupsUsers() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_GROUP_USERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table group_users already exists!",0);
        return "";
      }

      //groups_users
      String sql = "CREATE TABLE `u_group_users` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_user` INT(11) NOT NULL,\n"
              + "	`id_group` INT(11) NOT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK__users` (`id_user`),\n"
              + "	INDEX `FK__groups` (`id_group`),\n"
              + "	CONSTRAINT `FK__groups` FOREIGN KEY (`id_group`) REFERENCES `u_groups` (`id`),\n"
              + "	CONSTRAINT `FK__users` FOREIGN KEY (`id_user`) REFERENCES `u_users` (`id`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table groups_users succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createOwners() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_OWNERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table owners already exists!",0);
        return "";
      }

      //owners
      String sql = "CREATE TABLE `u_owners` (\n"
              + "	`id` INT(1) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_owner` INT(1) NOT NULL,\n"
              + "	`id_entity` INT(1) NOT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_owners_users` (`id_owner`),\n"
              + "	INDEX `FK_owners_entities` (`id_entity`),\n"
              + "	CONSTRAINT `FK_owners_entities` FOREIGN KEY (`id_entity`) REFERENCES `u_entities` (`id`),\n"
              + "	CONSTRAINT `FK_owners_users` FOREIGN KEY (`id_owner`) REFERENCES `u_users` (`id`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table owners succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createPermissions() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_PERMISSIONS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table permissions already exists!",0);
        return "";
      }

      //permissions
      String sql = "CREATE TABLE `u_permissions` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`permission` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	`permission_code` VARCHAR(50) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	UNIQUE INDEX `permission_code` (`permission_code`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table permissions succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createPermissionsGroup() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_PERMISSIONS_GROUP",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table permissions_group already exists!",0);
        return "";
      }

      //permissions groups
      String sql = "CREATE TABLE `u_permissions_group` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_group` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_entity` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_permission` INT(11) NULL DEFAULT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_permissions_group_entities` (`id_entity`),\n"
              + "	INDEX `FK_permissions_group_groups` (`id_group`),\n"
              + "	INDEX `FK_permissions_group_permissions` (`id_permission`),\n"
              + "	CONSTRAINT `FK_permissions_group_entities` FOREIGN KEY (`id_entity`) REFERENCES `u_entities` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_group_groups` FOREIGN KEY (`id_group`) REFERENCES `u_groups` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_group_permissions` FOREIGN KEY (`id_permission`) REFERENCES `u_permissions` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table permissions_group succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);            
      return e.toString();
    }
  }

  private static String createPermissionsUsers() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_PERMISSIONS_USERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table permissions_users already exists!",0);
        return "";
      }

      //permissions users
      String sql = "CREATE TABLE `u_permissions_users` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_user` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_entity` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_permission` INT(11) NULL DEFAULT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_permissions_users_entities` (`id_entity`),\n"
              + "	INDEX `FK_permissions_users_groups` (`id_user`),\n"
              + "	INDEX `FK_permissions_users_permissions` (`id_permission`),\n"
              + "	CONSTRAINT `FK_permissions_users_entities` FOREIGN KEY (`id_entity`) REFERENCES `u_entities` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_users_groups` FOREIGN KEY (`id_user`) REFERENCES `u_groups` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_users_permissions` FOREIGN KEY (`id_permission`) REFERENCES `u_permissions` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table permissions_users succesfully created!",0);

      return "";
    } catch (SQLException e) {
      if (ATGlobal.verboseLevel > 2) ATLog.log("Error - " + e, 0);
      return e.toString();
    }
  }

  private static String createUsersTable() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      DatabaseMetaData meta = Database.getConnectionToDatabase().getMetaData();
      ResultSet res = meta.getTables(null, null, "U_USERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        if (ATGlobal.verboseLevel > 2) ATLog.log("Table users already exists!",0);
        return "";
      }

      
      String sql = ATTools.getResourceFile("sql/create_users.sql");
      
      int result = stmt.executeUpdate(sql);

      if (ATGlobal.verboseLevel > 2) ATLog.log("Table users succesfully created!",0);

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
      String select = String.format("SELECT * from %s.u_users WHERE name='%s'", Database.getDatabase(), username);
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        String msg = String.format("Users %s already exists!", username);
        ATLog.log(msg, 0);
        return msg;
      }

      //add algator user
      String insert = String.format("INSERT INTO u_users (name,password) VALUES ('%s','%s')", username, Password.encript(password));
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
      String select = String.format("SELECT * from %s.u_users WHERE name='%s'", Database.getDatabase(), username);
      ResultSet rs = stmt.executeQuery(select);

      if (!rs.next()) {
        String msg = String.format("Users %s does not exist!", username);
        if (ATGlobal.verboseLevel > 2) ATLog.log(msg, 0);
        return msg;
      }

      //add algator user
      String insert = String.format("UPDATE u_users SET password='%s' WHERE name='%s'", Password.encript(newPassword), username);

      int result = stmt.executeUpdate(insert);

      if (ATGlobal.verboseLevel > 2) ATLog.log(String.format("User %s added!", username),0);

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
      String insert = "INSERT INTO u_groups (name,status) VALUES ('Everyone',0);";

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
