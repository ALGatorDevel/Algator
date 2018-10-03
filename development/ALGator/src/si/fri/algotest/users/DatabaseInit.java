package si.fri.algotest.users;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Gregor
 */
public class DatabaseInit {

  private static Connection conn;

  public static void init() {
    System.out.println("ALGator will now check for permission database and tables...");

    conn = PermTools.connectToDatabase();
    if (conn==null) return;

    createDatabase();

    //create tables
    createEntities();
    createGroups();
    createUsers();
    createPermissions();
    createGroupsUsers();
    createPermissionsGroup();
    createPermissionsUsers();
    createOwners();

    //add algator, everyone and permissions
    insertAlgator();
    insertEveryone();

    insertPermission("can read", "can_read");
    insertPermission("can write", "can_write");
    insertPermission("can execute", "can_execute");
  }

  
  private static boolean createDatabase() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      ResultSet rs = conn.getMetaData().getCatalogs();

      while (rs.next()) {
        String databaseName = rs.getString(1);
        if (databaseName.equals(PermTools.getDatabase())) {
          System.out.println("Database already exists...");

          conn = PermTools.connectToDatabase();

          return false;
        }
      }
      rs.close();

      String sql = "CREATE DATABASE IF NOT EXISTS " + PermTools.getDatabase().toUpperCase();

      //try to insert
      int result = stmt.executeUpdate(sql);

      System.out.println("Database " + PermTools.getDatabase() + " sucessfully created!");

      conn = PermTools.connectToDatabase();

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createEntities() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "ENTITIES",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table Entities already exists!");
        return false;
      }

      //entities
      String sql = "CREATE TABLE IF NOT EXISTS `entities` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`name` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	`type` TINYINT(4) NOT NULL DEFAULT '1' COMMENT '1-project, 2-algorithm, 3-testet',\n"
              + "	`id_parent` INT(11) NULL DEFAULT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_entities_entities` (`id_parent`),\n"
              + "	CONSTRAINT `FK_entities_entities` FOREIGN KEY (`id_parent`) REFERENCES `entities` (`id`) ON UPDATE CASCADE ON DELETE SET NULL\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      System.out.println("Table Entities succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createGroups() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "GROUPS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table Groups already exists!");
        return false;
      }

      String sql = "CREATE TABLE `groups` (\n"
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

      System.out.println("Table Groups succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createGroupsUsers() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "GROUP_USERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table group_users already exists!");
        return false;
      }

      //groups_users
      String sql = "CREATE TABLE `group_users` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_user` INT(11) NOT NULL,\n"
              + "	`id_group` INT(11) NOT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK__users` (`id_user`),\n"
              + "	INDEX `FK__groups` (`id_group`),\n"
              + "	CONSTRAINT `FK__groups` FOREIGN KEY (`id_group`) REFERENCES `groups` (`id`),\n"
              + "	CONSTRAINT `FK__users` FOREIGN KEY (`id_user`) REFERENCES `users` (`id`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      System.out.println("Table groups_users succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createOwners() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "OWNERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table owners already exists!");
        return false;
      }

      //owners
      String sql = "CREATE TABLE `owners` (\n"
              + "	`id` INT(1) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_owner` INT(1) NOT NULL,\n"
              + "	`id_entity` INT(1) NOT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_owners_users` (`id_owner`),\n"
              + "	INDEX `FK_owners_entities` (`id_entity`),\n"
              + "	CONSTRAINT `FK_owners_entities` FOREIGN KEY (`id_entity`) REFERENCES `entities` (`id`),\n"
              + "	CONSTRAINT `FK_owners_users` FOREIGN KEY (`id_owner`) REFERENCES `users` (`id`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      System.out.println("Table owners succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createPermissions() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "PERMISSIONS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table permissions already exists!");
        return false;
      }

      //permissions
      String sql = "CREATE TABLE `permissions` (\n"
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

      System.out.println("Table permissions succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createPermissionsGroup() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "PERMISSIONS_GROUP",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table permissions_group already exists!");
        return false;
      }

      //permissions groups
      String sql = "CREATE TABLE `permissions_group` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_group` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_entity` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_permission` INT(11) NULL DEFAULT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_permissions_group_entities` (`id_entity`),\n"
              + "	INDEX `FK_permissions_group_groups` (`id_group`),\n"
              + "	INDEX `FK_permissions_group_permissions` (`id_permission`),\n"
              + "	CONSTRAINT `FK_permissions_group_entities` FOREIGN KEY (`id_entity`) REFERENCES `entities` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_group_groups` FOREIGN KEY (`id_group`) REFERENCES `groups` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_group_permissions` FOREIGN KEY (`id_permission`) REFERENCES `permissions` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      System.out.println("Table permissions_group succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createPermissionsUsers() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "PERMISSIONS_USERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table permissions_users already exists!");
        return false;
      }

      //permissions users
      String sql = "CREATE TABLE `permissions_users` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`id_user` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_entity` INT(11) NULL DEFAULT NULL,\n"
              + "	`id_permission` INT(11) NULL DEFAULT NULL,\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	INDEX `FK_permissions_users_entities` (`id_entity`),\n"
              + "	INDEX `FK_permissions_users_groups` (`id_user`),\n"
              + "	INDEX `FK_permissions_users_permissions` (`id_permission`),\n"
              + "	CONSTRAINT `FK_permissions_users_entities` FOREIGN KEY (`id_entity`) REFERENCES `entities` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_users_groups` FOREIGN KEY (`id_user`) REFERENCES `groups` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,\n"
              + "	CONSTRAINT `FK_permissions_users_permissions` FOREIGN KEY (`id_permission`) REFERENCES `permissions` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      System.out.println("Table permissions_users succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean createUsers() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      DatabaseMetaData meta = conn.getMetaData();
      ResultSet res = meta.getTables(null, null, "USERS",
              new String[]{"TABLE"});

      //check if exists
      if (res.next()) {
        System.out.println("Table users already exists!");
        return false;
      }

      //users
      String sql = "CREATE TABLE `users` (\n"
              + "	`id` INT(11) NOT NULL AUTO_INCREMENT,\n"
              + "	`name` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	`password` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',\n"
              + "	`status` INT(1) NOT NULL DEFAULT '0' COMMENT '0 - active, 1 - inactive',\n"
              + "	PRIMARY KEY (`id`),\n"
              + "	UNIQUE INDEX `name` (`name`)\n"
              + ")\n"
              + "COLLATE='utf8_slovenian_ci'\n"
              + "ENGINE=InnoDB\n"
              + "AUTO_INCREMENT=1\n"
              + ";";

      int result = stmt.executeUpdate(sql);

      System.out.println("Table users succesfully created!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean insertAlgator() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      //check if user exists
      String select = "SELECT * from " + PermTools.getDatabase() + ".users WHERE id=1";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        System.out.println("Users algator already exists!");
        return false;
      }

      //add algator user
      String insert = "INSERT INTO users (id,name,password,status) VALUES (1,'algator','root',0);";

      int result = stmt.executeUpdate(insert);

      System.out.println("User algator added!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean insertEveryone() {
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".groups WHERE name='Everyone'";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        System.out.println("Group Everyone already exists!");
        return false;
      }

      //add everyone
      String insert = "INSERT INTO groups (name,status) VALUES ('Everyone',0);";

      int result = stmt.executeUpdate(insert);

      System.out.println("Group Everyone added!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  private static boolean insertPermission(String permission, String permission_code) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".permissions WHERE permission_code='" + permission_code + "'";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        System.out.println("Permission " + permission + " already exists!");
        return false;
      }

      //add everyone
      String insert = "INSERT INTO permissions (permission,permission_code) VALUES ('" + permission + "','" + permission_code + "');";

      int result = stmt.executeUpdate(insert);

      System.out.println("Permission " + permission + " added!");

      return true;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }
}
