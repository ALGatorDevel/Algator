package algator;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.math.NumberUtils;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.users.DatabaseInit;

import si.fri.algotest.users.DBEntity;
import si.fri.algotest.users.DBEntityPermission;
import si.fri.algotest.users.DBGroup;
import si.fri.algotest.users.DBOwner;
import si.fri.algotest.users.PermTools;
import si.fri.algotest.users.DBPermission;
import si.fri.algotest.users.DBUser;
import si.fri.algotest.tools.ATTools;
import static si.fri.algotest.users.PermTools.getID;

/**
 *
 * @author Gregor
 */
public class Users {
  
  static final int ALL_ENTITIES_ID = -3;
  static final int ALL_PROJECTS_ID = -2;
  static final int INVALID_ID      = -1;

  private static Connection conn;

  private static ArrayList<DBEntity> project_list = new ArrayList<DBEntity>();
  private static ArrayList<DBEntity> alg_list;
  private static ArrayList<DBEntity> test_list;
  private static ArrayList<DBOwner> owner_list;
  private static Map<String, List<DBEntity>> alg_map = new HashMap<String, List<DBEntity>>();
  private static Map<String, List<DBEntity>> test_map = new HashMap<String, List<DBEntity>>();
  private static ArrayList<DBEntityPermission> project_permissions;
  private static ArrayList<DBEntityPermission> algorithm_permissions;
  private static ArrayList<DBEntityPermission> test_permissions;

  private static String format = "json";

  private static String help_msg =
              "init \n"
            + "   Inits database and creates tables.\n\n"
          
            + "adduser username password\n"
            + "   Creates new user, requires username and password.\n\n"

            + "addgroup groupname\n"
            + "   Creates new group, requires name.\n\n"
          
            + "moduser groupname username\n"
            + "   Adds/removes user to group, requires groupname and username\n\n"

            + "userperm username [entity]\n"
            + "   Returns premissions for username on specified enitity, where entity is:\n"
            + "     -all         ... return permissions for all entities\n"
            + "     <empty>      ... return permissions for all projects\n"
            + "     name | id    ... return permissions for the given entity and all subentites\n\n"

            + "canuser username permission enitity\n"           
            + "     Return true or false if username has specific permission on entity. \n\n"

            + "chmod perm who entity\n"
            + "      Changes permission for user (or group)\n"
            + "        perm:   +/- permission (e.g. +can_write or -can_read)\n"
            + "        who:    user or group (e.g. algator or :algator)\n"
            + "        entity: project (proj_name) or \n"
            + "                algorithm (proj_name/alg_name) or\n"
            + "                testset (proj_name//testset_name)\n"
            + "        Example: chmod +can_write joe Sorting/QuickSort\n\n"
          
            + "getentityid entity\n"
            + "       Prints the id of a given entity, -1 if not found.\n\n"
          
            + "setowner user entity\n"
            + "     Set  the ownership of the entity to the user.\n\n"
          
            + "set_default_owner\n"
            + "     Checks all the files (proj, alg, test) in system directory. If file\n"
            + "     has no owner, commands sets it on user id 1 - algator.\n\n"          
          
            + "showowner entity  \n"
            + "     Shows the owner owner of the entity.\n\n"
          
            + "showusers [username]\n"
            + "     Show all active users in the system. If arg <username> is passed to \n"
            + "     command, it shows only that specific user.\n\n"

            + "showgroups\n"
            + "     Show all active groups in the system.\n\n"

            + "showpermissions\n"
            + "     Show all permissions in the system.\n\n"
          
            + "listentities [-all] | [project_name] | [project_id]\n"
            + "     List the entities of the system. Parameter can be:\n"
            + "     -all                      ... list all entities\n"
            + "     <no parameter>            ... list all projects\n"
            + "     project_name | project_id ... list all entities of the project\n\n"          
          
            + "changeuserstatus username status\n"
            + "     Changes status of user active/inactive. Status must be int (0/1)\n\n"
          
            + "changegroupstatus groupname status\n"
            + "     Changes staus of group active/inactive. Status must be int (0/1)\n\n" 
          
            + "setformat json | string\n"
            + "     Sets the output print format";

  
  public static void help() {
    System.out.println(help_msg);
  }

  public static boolean adduser(String username, String password) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      int id_user  = INVALID_ID;
      int id_group = INVALID_ID;

      if (getUserID(username, true) < 0) {
        String insert = "INSERT INTO " + PermTools.getDatabase() + ".users(name,password) VALUES ('" + username + "','" + password + "')";

        int result = stmt.executeUpdate(insert, stmt.RETURN_GENERATED_KEYS);

        if (result > 0) {
          //get user id and group id
          ResultSet rs = stmt.getGeneratedKeys();
          if (rs.next()) {
            id_user = rs.getInt(1);
          }

          String select = "SELECT * from " + PermTools.getDatabase() + ".groups WHERE name='Everyone'";
          rs = stmt.executeQuery(select);
          if (rs.next()) {
            id_group = rs.getInt(1);
          }

          insert = "INSERT INTO " + PermTools.getDatabase() + ".group_users(id_user,id_group) VALUES (" + id_user + "," + id_group + ")";
          result = stmt.executeUpdate(insert);
          if (result > 0) {
            System.out.println(">>> User added!");
            return true;
          } else {
            System.out.println(">>> User added, but not added to group Everyone!");
            return true;
          }
        } else {
          System.out.println(">>> Error while adding user!");
          return false;
        }
      }
      return false;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  public static boolean addgroup(String name) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      if (getGroupID(name, true) < 0) {
        String insert = "INSERT INTO " + PermTools.getDatabase() + ".groups(name) VALUES ('" + name + "')";

        int result = stmt.executeUpdate(insert);

        if (result > 0) {
          System.out.println(">>> Group added!");
          return true;
        } else {
          System.out.println(">>> Error while adding group!");
          return false;
        }
      }
      return false;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  public static boolean moduser(String groupname, String username) {
    try {
      Statement stmt = (Statement) conn.createStatement();
      //init
      int id_group = getGroupID(groupname, false);
      int id_user  = getUserID(username, false);

      if (id_group < 0) {
        System.out.println(">>> Group with this name does not exist!");
        return false;
      }

      if (id_user < 0) {
        System.out.println(">>> User with this username does not exist!");
        return false;
      }

      String select = "SELECT * from " + PermTools.getDatabase() + ".group_users WHERE id_group=" + id_group + " AND id_user=" + id_user + "";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        String delete = "DELETE FROM " + PermTools.getDatabase() + ".group_users WHERE id_group=" + id_group + " and id_user=" + id_user;
        int result = stmt.executeUpdate(delete);

        if (result > 0) {
          System.out.println(">>> User removed from group!");
          return true;
        } else {
          System.out.println(">>> Error while deleting user from group!");
          return false;
        }

      } else {
        String insert = "INSERT INTO " + PermTools.getDatabase() + ".group_users(id_group,id_user) VALUES ('" + id_group + "','" + id_user + "')";
        int result = stmt.executeUpdate(insert);

        if (result > 0) {
          System.out.println(">>> User added to group!");
          return true;
        } else {
          System.out.println(">>> Error while adding user to group!");
          return false;
        }
      }
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  
  /**
   * Entity can be either a project (Sorting), algorithm (Sorting/BubbleSort)
   * or a testset (Sorting//TestSet0)
   */
  public static int findEntityId(String entity) {
    try {
      String [] parts = entity.split("[/]");
      
      String projectName = parts[0];
      int id_project = getProjectID(projectName, false);
      
      // looking for project?
      if (parts.length == 1)
        return id_project;
      
      // invalid project?
      if (id_project <= 0) return INVALID_ID;
      
      if (parts.length==2) { // looking for algorithm
        return getID(String.format(
          "SELECT * from " + PermTools.getDatabase() + ".entities WHERE name='%s' AND type=2 AND id_parent=%d", parts[1], id_project));
      } else { //looking for testset
        return getID(String.format(
          "SELECT * from " + PermTools.getDatabase() + ".entities WHERE name='%s' AND type=3 AND id_parent=%d", parts[2], id_project));      
      }
    } catch (Exception e) {
      return INVALID_ID;
    }      
  }
  
  /**
   * Returns empty string if no error, error message otherwise.
   */
  public static String chmod(String perm, String user, String entity) {   
    try {
      boolean addPermission = true;
      int id_perm           = INVALID_ID;
      if (perm.startsWith("+") || perm.startsWith("-")) {
        addPermission = perm.startsWith("+");
        perm = perm.substring(1);
        id_perm = getID(String.format("SELECT * from " + PermTools.getDatabase() + ".permissions WHERE permission_code='%s'", perm));            
        if (id_perm <= 0) return "Invalid permission code";
      } else return "Invalid permission format";
          
      String usergroup;
      int id_usergroup;
      if (user.startsWith(":")) {
        usergroup = "group";
        user=user.substring(1);
        id_usergroup = getGroupID(user, false);
      } else {
        usergroup = "user";
        id_usergroup = getUserID(user, false);
      }
      if (id_usergroup <= 0)
        return String.format("Invalid %s name (%s).", usergroup, user);
          
      int id_entity = findEntityId(entity);
      if (id_entity <= 0)
        return String.format("Entity %s does not exist.", entity);    
      
      
      String select = String.format(
         "SELECT * FROM " + PermTools.getDatabase() + ".permissions_%ss WHERE id_%s=%d AND id_entity=%d AND id_permission=%d",
         usergroup,usergroup,id_usergroup,id_entity,id_perm
      );
      conn = PermTools.connectToDatabase();
      if (conn==null) return "Invalid database connection.";
      
      Statement stmt = (Statement) conn.createStatement();
      ResultSet rs = stmt.executeQuery(select);
            
      boolean permExists = rs.next();
  
      
      String sql;
      if (addPermission) {
        if (permExists) return "Permission already exists.";
        sql = String.format(
          "INSERT INTO " + PermTools.getDatabase() + ".permissions_%ss (id_%s,id_entity,id_permission) VALUES (%d,%d,%d)",
          usergroup, usergroup, id_usergroup, id_entity, id_perm
        );         
      } else {
        if (!permExists) return "Permission does not exist.";
        sql = String.format(
          "DELETE FROM " + PermTools.getDatabase() + ".permissions_%ss WHERE id_%s=%d AND id_entity=%d AND id_permission=%d",
          usergroup, usergroup, id_usergroup, id_entity, id_perm
        );    
      }
      int result = stmt.executeUpdate(sql);
      if (result <= 0) {
        return "Error adding/removing permission";
      }

      
    } catch (Exception e) {
      return e.toString();
    }
      
    return "";
  }


  private static void showUsers(String username) {
    String statement = "";
    if (username.equals("")) {
      statement = "SELECT * from " + PermTools.getDatabase() + ".users";
    } else {
      statement = "SELECT * from " + PermTools.getDatabase() + ".users WHERE name='" + username + "'";
    }
    try {
      Statement stmt = (Statement) conn.createStatement();
      ResultSet rs = stmt.executeQuery(statement);
      if (format.equals("json")) {
        Gson gson = new Gson();
        ArrayList<DBUser> users = new ArrayList<>();
        while (rs.next()) {
          users.add(new DBUser(rs.getInt("id"), rs.getString("name"), rs.getInt("status")));
        }
        System.out.println(gson.toJson(users));
        return;
      }
      if (!rs.isBeforeFirst()) {
        System.out.println(">>> User with this username does not exist!");
        return;
      }
      System.out.printf("%1$-5s %2$-20s %3$10s", "id", "Username", "Status");
      System.out.println("");
      while (rs.next()) {
        String lastName = rs.getString("name");
        String status   = rs.getString("status");
        String id       = rs.getString("id");
        System.out.printf("%1$-5s %2$-20s %3$8s", id, lastName, status);
        System.out.println("");
      }
    } catch (SQLException e) {
      System.err.println(e);
    }
  }

  private static void showGroups() {
    try {
      Statement stmt = (Statement) conn.createStatement();
      String select = "SELECT * from " + PermTools.getDatabase() + ".groups";
      ResultSet rs = stmt.executeQuery(select);
      if (format.equals("json")) {
        Gson gson = new Gson();
        ArrayList<DBGroup> groups = new ArrayList<>();
        while (rs.next()) {
          groups.add(new DBGroup(rs.getInt("id"), rs.getString("name"), rs.getInt("status")));
        }
        System.out.println(gson.toJson(groups));
        return;
      }
      System.out.printf("%1$-20s %2$10s", "Group name", "Status");
      System.out.println("");
      while (rs.next()) {
        String groupName = rs.getString("name");
        System.out.printf("%1$-20s %2$8s", groupName, "1");
        System.out.println("");
      }
    } catch (SQLException e) {
      System.err.println(e);
    }
  }

  private static void showPermissions() {
    try {
      Statement stmt = (Statement) conn.createStatement();
      String select = "SELECT * from " + PermTools.getDatabase() + ".permissions";
      ResultSet rs = stmt.executeQuery(select);
      if (format.equals("json")) {
        Gson gson = new Gson();
        ArrayList<DBPermission> permissions = new ArrayList<>();
        while (rs.next()) {
          permissions.add(new DBPermission(rs.getInt("id"), rs.getString("permission"), rs.getString("permission_code")));
        }
        System.out.println(gson.toJson(permissions));
        return;
      }
      System.out.printf("%1$-20s %2$10s", "Permission", "Permission code");
      System.out.println("");
      while (rs.next()) {
        String lastName = rs.getString("permission");
        String status = rs.getString("permission_code");
        System.out.printf("%1$-20s %2$8s", lastName, status);
        System.out.println("");
      }
    } catch (SQLException e) {
      System.err.println(e);
    }
  }

  public static ArrayList<DBEntity> listProjects() {
    load_entites("");
    return project_list;
  }
  
  public static void listEntities() {    
    System.out.println("");
    String[] types = {"Proj", "Alg", "Test"};
    System.out.printf("%1$-6s %2$-16s %3$s\n", "ID", "Type", "Entity name");    
    System.out.println("-------------------------------------------");

    for (DBEntity project : project_list) {
      System.out.printf("%1$-6s %2$-16s %3$s", project.id, types[project.type - 1], project.name);
      System.out.println("");
      if (alg_map != null && alg_map.containsKey(Integer.toString(project.id))) {
        for (DBEntity algorithm : alg_map.get(Integer.toString(project.id))) {
          System.out.printf("%1$-6s %2$-20s %3$s", algorithm.id, types[algorithm.type - 1], algorithm.name);
          System.out.println("");
        }
      }
      if (test_map != null && test_map.containsKey(Integer.toString(project.id))) {
        for (DBEntity testset : test_map.get(Integer.toString(project.id))) {
          System.out.printf("%1$-6s %2$-20s %3$s", testset.id, types[testset.type - 1], testset.name);
          System.out.println("");
        }
      }
    }
  }

  private static void changeUserStatus(String username, String status) {
    String[] status_string = {"Activated", "Deactivated"};
    int status_number = Integer.parseInt(status);

    if (status_number < 0 || status_number > 1) {
      System.out.println("Status number must be 0 or 1!");
      return;
    }
    if (getUserID(username, false) > 0) {
      try {
        Statement stmt = (Statement) conn.createStatement();
        String insert = "UPDATE " + PermTools.getDatabase() + ".users SET status = " + status + " WHERE name = '" + username + "'";
        int result = stmt.executeUpdate(insert);

        if (result > 0) {
          System.out.println(">>> " + username + " status changed to: " + status_string[status_number]);
        } else {
          System.out.println(">>> Error while adding user to group!");
        }

      } catch (Exception e) {
      }
    }
  }

  private static void changeGroupStatus(String groupname, String status) {
    String[] status_string = {"Activated", "Deactivated"};
    int status_number = Integer.parseInt(status);

    if (status_number < 0 || status_number > 1) {
      System.out.println("Status number must be 0 or 1!");
      return;
    }
    if (getGroupID(groupname, false) > 0) {
      try {
        Statement stmt = (Statement) conn.createStatement();
        String insert = "UPDATE " + PermTools.getDatabase() + ".groups SET status = " + status + " WHERE name = '" + groupname + "'";
        int result = stmt.executeUpdate(insert);

        if (result > 0) {
          System.out.println(">>> " + groupname + " status changed to: " + status_string[status_number]);
        } else {
          System.out.println(">>> Error while adding user to group!");
        }

      } catch (Exception e) {
      }
    }
  }

  private static int getUserID(String username, boolean output) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".users WHERE name='" + username + "'";

      ResultSet rs = stmt.executeQuery(select);
      if (rs.next()) {
        int id_user = rs.getInt("id");
        if (output) {
          System.out.println(">>> User with this username already exists!");
        }
        return id_user;
      } else {
        return INVALID_ID;
      }
    } catch (SQLException e) {
      System.err.println(e);
      return INVALID_ID;
    }
  }

  private static int getGroupID(String groupname, boolean output) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".groups WHERE name='" + groupname + "'";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        int id_group = rs.getInt("id");
        if (output) {
          System.out.println(">>> Group with this name already exists!");
        }
        return id_group;
      } else {
        return INVALID_ID;
      }
    } catch (SQLException e) {
      System.err.println(e);
      return INVALID_ID;
    }
  }

  private static int getPermID(String permname, boolean output) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".permissions WHERE permission_code='" + permname + "'";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        int id_perm = rs.getInt("id");
        return id_perm;
      } else {
        if (output) {
          System.out.println(">>> Permission with this name does not exist!");
        }
        return INVALID_ID;
      }
    } catch (SQLException e) {
      System.err.println(e);
      return INVALID_ID;
    }
  }

  private static int getProjectID(String entityname, boolean output) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".entities WHERE name='" + entityname + "' AND type=1";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        int id_entity = rs.getInt("id");
        return id_entity;
      } else {
        if (output) {
          System.out.println(">>> Project with this name does not exist!");
        }
        return INVALID_ID;
      }
    } catch (SQLException e) {
      System.err.println(e);
      return INVALID_ID;
    }
  }

  private static ArrayList<Integer> projectAlgs(int id_project) {
    ArrayList<Integer> alg_ids = new ArrayList<Integer>();
    try {
      Statement stmt = (Statement) conn.createStatement();

      String select = "SELECT * from " + PermTools.getDatabase() + ".entities WHERE id_parent=" + id_project + " AND type=2";
      ResultSet rs = stmt.executeQuery(select);
      while (rs.next()) {
        alg_ids.add(rs.getInt(1));
      }
      return alg_ids;
    } catch (SQLException e) {
      System.err.println(e);
      return alg_ids;
    }
  }
  
  public static void load_entites() {
    load_entites("-all");
  }

  /**
   * param: "-all" ... load all entities, "" ... load all projects, "proj_name" ... load all entities of the given project
   */
  public static void load_entites(String param) {
    project_list = new ArrayList<DBEntity>();
    alg_list = new ArrayList<DBEntity>();
    test_list = new ArrayList<DBEntity>();
    alg_map = new HashMap<String, List<DBEntity>>();
    test_map = new HashMap<String, List<DBEntity>>();

    try {
      String where = "";
      int proj_id = INVALID_ID;
      if (!param.isEmpty() && !param.equals("-all")) { // param is proj_name or project_id        
        try {
          proj_id = Integer.parseInt(param);          
        } catch (Exception e) {}
        if (proj_id == INVALID_ID) {
          proj_id = findEntityId(param);
        }       
        where = " where id_parent="+proj_id+" or id="+proj_id;  // only entities whose parent is proj_id

      } else if (param.isEmpty()) {
        where = " where id_parent IS NULL";  // only projects
      }
      
      if (conn == null) conn = PermTools.connectToDatabase();
      if (conn==null) return;
      Statement stmt = (Statement) conn.createStatement();
      String select = "SELECT * from " + PermTools.getDatabase() + ".entities" + where;
      ResultSet rs = stmt.executeQuery(select);
      while (rs.next()) {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int type = rs.getInt("type");
        int id_parent = rs.getInt("id_parent");
        List tmp_map = null;
        switch (type) {
          case 1:
            project_list.add(new DBEntity(id, name, type, id_parent));
            break;
          case 2:
            tmp_map = alg_map.get(Integer.toString(id_parent));
            if (tmp_map == null) {
              tmp_map = new ArrayList<DBEntity>();
            }
            tmp_map.add(new DBEntity(id, name, type, id_parent));
            alg_map.put(Integer.toString(id_parent), tmp_map);
            alg_list.add(new DBEntity(id, name, type, id_parent));
            break;
          case 3:
            tmp_map = test_map.get(Integer.toString(id_parent));
            if (tmp_map == null) {
              tmp_map = new ArrayList<DBEntity>();
            }
            tmp_map.add(new DBEntity(id, name, type, id_parent));
            test_map.put(Integer.toString(id_parent), tmp_map);
            test_list.add(new DBEntity(id, name, type, id_parent));
            break;
        }
      }      
    } catch (SQLException e) {
      System.err.println(e);
    }
  }

  public static void load_perm(String username) {
    project_permissions = new ArrayList<DBEntityPermission>();
    algorithm_permissions = new ArrayList<DBEntityPermission>();
    test_permissions = new ArrayList<DBEntityPermission>();
    try {
      Statement stmt = (Statement) conn.createStatement();
    
      String selectProj=String.format(ATTools.getResourceFile("sql/project_permissions.sql"), username);
      String selectAlg = String.format(ATTools.getResourceFile("sql/algorithm_permissions.sql"), username);
      String selectTest = String.format(ATTools.getResourceFile("sql/testset_permissions.sql"), username);

      ResultSet rs_proj = stmt.executeQuery(selectProj);
      while (rs_proj.next()) {
        project_permissions.add(new DBEntityPermission(rs_proj.getInt("id"), rs_proj.getString("name"), rs_proj.getInt("type"), rs_proj.getString("permissions"), rs_proj.getInt("parent_id"), rs_proj.getString("parent_name")));
      }

      ResultSet rs_alg = stmt.executeQuery(selectAlg);
      while (rs_alg.next()) {
        algorithm_permissions.add(new DBEntityPermission(rs_alg.getInt("id"), rs_alg.getString("name"), rs_alg.getInt("type"), rs_alg.getString("permissions"), rs_alg.getInt("parent_id"), rs_alg.getString("parent_name")));
      }

      ResultSet rs_test = stmt.executeQuery(selectTest);
      while (rs_test.next()) {
        test_permissions.add(new DBEntityPermission(rs_test.getInt("id"), rs_test.getString("name"), rs_test.getInt("type"), rs_test.getString("permissions"), rs_test.getInt("parent_id"), rs_test.getString("parent_name")));
      }

    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  public static void print_permissions(String username) {
    load_perm(username);
    
    if (format.equals("json")) {
      Gson gson = new Gson();
      List<DBEntityPermission> combined = new ArrayList<DBEntityPermission>();
      combined.addAll(project_permissions);
      combined.addAll(algorithm_permissions);
      combined.addAll(test_permissions);
      System.out.println(gson.toJson(combined));
      return;
    }
    System.out.println("");
    String[] types = {"Proj", "Alg", "Test"};
    System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", "ID", "Type", "Entity name", "Permission", "P ID", "P name");
    System.out.println("");
    System.out.printf("___________________________________________________________________________");
    System.out.println("");
    for (DBEntityPermission project : project_permissions) {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", project.id, types[project.type - 1], project.name, project.permission, project.parent_id, project.parent_name);
      System.out.println("");
    }
    for (DBEntityPermission algorithm : algorithm_permissions) {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", algorithm.id, types[algorithm.type - 1], algorithm.name, algorithm.permission, algorithm.parent_id, algorithm.parent_name);
      System.out.println("");
    }
    for (DBEntityPermission test : test_permissions) {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", test.id, types[test.type - 1], test.name, test.permission, test.parent_id, test.parent_name);
      System.out.println("");
    }

  }

  public static void userperm(String username, int entity_id) {
    String[] types = {"Proj", "Alg", "Test"};
    load_perm(username);
    
    List<DBEntityPermission> perms = new ArrayList<DBEntityPermission>(project_permissions);
    perms.addAll(algorithm_permissions);
    perms.addAll(test_permissions);
    
    if (entity_id != ALL_ENTITIES_ID) {    
      if (entity_id == ALL_PROJECTS_ID) // retain only projects
        perms.removeIf(value -> value.parent_id != 0);      
      else // retain only entity and its children
        perms.removeIf(value -> (value.id != entity_id && value.parent_id != entity_id));      
    }
    
    if (format.equals("json")) {
      System.out.println(new Gson().toJson(perms));      
    } else {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", "ID", "Type", "Entity name", "Permission", "P ID", "P name");
      System.out.println("");
      System.out.printf("___________________________________________________________________________");
      System.out.println("");
      for (DBEntityPermission project : perms) {        
        System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", project.id, types[project.type - 1], project.name, project.permission, project.parent_id, project.parent_name);
        System.out.println("");
      }
    }
  }

  public static void user_permission(String username, String entity) {
    userperm(username, findEntityId(entity));
  }

  public static boolean can_user(String username, String permission, String entity) {
    return can_user(username, permission, findEntityId(entity));
  }
  
  public static boolean can_user(String username, String permission, int entity_id) {
    load_perm(username);
    for (DBEntityPermission project : project_permissions) {
      if (project.id == entity_id) {
        if (project.permission.equals("all") || project.permission.equals(permission)) {
          return true;
        }
      }
    }
    for (DBEntityPermission algorithm : algorithm_permissions) {
      if (algorithm.id == entity_id) {
        if (algorithm.permission.equals("all") || algorithm.permission.equals(permission)) {
          return true;
        }
      }
    }
    for (DBEntityPermission test : test_permissions) {
      if (test.id == entity_id) {
        if (test.permission.equals("all") || test.permission.equals(permission)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean showOwnerProj(String projectName) {
    try {
      Statement stmt = (Statement) conn.createStatement();

      int projExists = getProjectID(projectName, true);

      if (projExists < 0) {
        return false;
      }

      //get owner
      String select = "SELECT t1.id AS id_entity,"
              + "t1.name AS entity_name,"
              + "t1.type,"
              + "t1.id_parent,"
              + "t3.name AS username "
              + "FROM " + PermTools.getDatabase() + ".entities AS t1 "
              + "JOIN " + PermTools.getDatabase() + ".owners AS t2 on t1.id=t2.id_entity "
              + "JOIN " + PermTools.getDatabase() + ".users AS t3 on t2.id_owner=t3.id "
              + "WHERE t1.name='" + projectName + "'";

      ResultSet rs = stmt.executeQuery(select);
      if (rs.next()) {
        String username = rs.getString("username");
        if (format.equals("json")) {
          Gson gson = new Gson();
          System.out.println(gson.toJson(username));
        } else {
          System.out.println(">>> Owner for " + projectName + " is user " + username + ".");
        }
        return true;

      } else {
        if (format.equals("json")) {
          Gson gson = new Gson();
          System.out.println(gson.toJson("No owner"));
        } else {
          System.out.println(">>> No owner data found for this project!");
        }
        return false;
      }
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  public static boolean showOwnerAlg(String projectName, String algorithmName) {
    load_entites();
    try {
      int id      = INVALID_ID;
      int proj_id = INVALID_ID;
      Statement stmt = (Statement) conn.createStatement();

      for (DBEntity proj : project_list) {
        if (proj.name.equals(projectName)) {
          proj_id = proj.id;
        }
      }
      if (proj_id < 0) {
        if (format.equals("json")) {
          Gson gson = new Gson();
          System.out.println(gson.toJson("No project"));
        } else {
          System.out.println(">>> Project with this name does not exist!");
        }
        return false;
      }

      List<DBEntity> alg_list = alg_map.get(Integer.toString(proj_id));

      for (DBEntity alg : alg_list) {
        if (alg.name.equals(algorithmName)) {

          String select = "SELECT t1.id AS id_entity,"
                  + "t1.name AS entity_name,"
                  + "t1.type,"
                  + "t1.id_parent,"
                  + "t3.name AS username "
                  + "FROM " + PermTools.getDatabase() + ".entities AS t1 "
                  + "JOIN " + PermTools.getDatabase() + ".owners AS t2 on t1.id=t2.id_entity "
                  + "JOIN " + PermTools.getDatabase() + ".users AS t3 on t2.id_owner=t3.id "
                  + "WHERE t1.name='" + alg.name + "'"
                  + " AND t1.id_parent=" + proj_id + "";

          ResultSet rs = stmt.executeQuery(select);
          if (rs.next()) {
            String username = rs.getString("username");
            if (format.equals("json")) {
              Gson gson = new Gson();
              System.out.println(gson.toJson(username));
            } else {
              System.out.println(">>> Owner for " + algorithmName + " is user " + username + ".");
            }
            return true;

          } else {
            if (format.equals("json")) {
              Gson gson = new Gson();
              System.out.println(gson.toJson("No owner"));
            } else {
              System.out.println(">>> No owner data found for this algorithm!");
            }
            return false;
          }
        }
        if (format.equals("json")) {
          Gson gson = new Gson();
          System.out.println(gson.toJson("Does not exist"));
        } else {
          System.out.println(">>> Algorithm " + algorithmName + " in project " + projectName + " does not exist!");
        }
        return false;
      }
      return false;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  public static boolean showOwnerTest(String projectName, String testName) {
    //za popravit
    load_entites();
    try {
      int id      = INVALID_ID;
      int proj_id = INVALID_ID;
      int alg_id  = INVALID_ID;

      Statement stmt = (Statement) conn.createStatement();

      for (DBEntity proj : project_list) {
        if (proj.name.equals(projectName)) {
          proj_id = proj.id;
          break;
        }
      }
      if (proj_id < 0) {
        if (format.equals("json")) {
          Gson gson = new Gson();
          System.out.println(gson.toJson("Does not exist"));
        } else {
          System.out.println(">>> Project with this name does not exist!");
        }

        return false;
      }

      List<DBEntity> test_list = test_map.get(Integer.toString(proj_id));
      for (DBEntity test : test_list) {
        if (test.name.equals(testName)) {
          String select = "SELECT t1.id AS id_entity,"
                  + "t1.name AS entity_name,"
                  + "t1.type,"
                  + "t1.id_parent,"
                  + "t3.name AS username "
                  + "FROM " + PermTools.getDatabase() + ".entities AS t1 "
                  + "JOIN " + PermTools.getDatabase() + ".owners AS t2 on t1.id=t2.id_entity "
                  + "JOIN " + PermTools.getDatabase() + ".users AS t3 on t2.id_owner=t3.id "
                  + "WHERE t1.name='" + test.name + "'"
                  + " AND t1.id_parent=" + proj_id + "";

          ResultSet rs = stmt.executeQuery(select);
          if (rs.next()) {
            String username = rs.getString("username");
            if (format.equals("json")) {
              Gson gson = new Gson();
              System.out.println(gson.toJson(username));
            } else {
              System.out.println(">>> Owner for " + testName + " is user " + username + ".");
            }
            return true;
          
          } else {
            if (format.equals("json")) {
              Gson gson = new Gson();
              System.out.println(gson.toJson("No owner"));
            } else {
              System.out.println(">>> No owner data found for this testSet!");
            }
            return false;
          }
        }
        if (format.equals("json")) {
            Gson gson = new Gson();
            System.out.println(gson.toJson("Does not exist"));
        } else {
            System.out.println(">>> Project " + projectName + " does not have any TestSets!");
        }
          return false;        
      }
      return false;
    } catch (SQLException e) {
      System.err.println(e);
      return false;
    }
  }

  public static void setowner(String id_owner, String id_entity) {
    if (!NumberUtils.isNumber(id_owner))  id_owner = Integer.toString(getUserID(id_owner, false));
    if (!NumberUtils.isNumber(id_entity)) id_entity = Integer.toString(findEntityId(id_entity)); 
    try {
      Statement stmt = (Statement) conn.createStatement();
      String owner_find = String.format(
         "SELECT * FROM " + PermTools.getDatabase() + ".owners where id_entity=%s",  id_entity
      );
      boolean result = stmt.execute(owner_find);  

      String sql;
      ResultSet rst = stmt.getResultSet();
      if (rst.next()) 
        sql = String.format(
             "UPDATE " + PermTools.getDatabase() + ".owners SET id_owner=%s WHERE id_entity=%s", id_owner, id_entity
        );
      else
        sql = String.format(
             "INSERT INTO " + PermTools.getDatabase() + ".owners(id_owner,id_entity) VALUES (%s,%s)", id_owner, id_entity
        );        
      
      stmt.executeUpdate(sql);
      
      System.out.println(">> OK");
    } catch (Exception e) {
      System.out.println("Error: " + e.toString());
    }
  }
  
  public static void setDefaultOwnerForAllEntities() {
    load_entites();
    boolean exists;
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projRoot = ATGlobal.getPROJECTSfolder(dataroot);
    File file = new File(projRoot);
    int id_project;
    int proj_c = 0, alg_c = 0, test_c = 0, proj_x = 0, alg_x = 0, test_x = 0;
    String algName, testName;
    String[] projects = file.list(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        return new File(current, name).isDirectory();
      }
    });
    for (String proj : projects) {
      exists = false;
      id_project = INVALID_ID;
      String projName = proj.split("-")[1];
      for (DBEntity pl : project_list) {
        if (pl.name.equals(projName)) {
          exists = true;
        }
      }
      if (!exists) {
        try {
          Statement stmt = (Statement) conn.createStatement();
          String proj_insert = "INSERT INTO " + PermTools.getDatabase() + ".entities(name,type) VALUES ('" + projName + "',1)";
          int result = stmt.executeUpdate(proj_insert, stmt.RETURN_GENERATED_KEYS);

          if (result > 0) {
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
              id_project = rs.getInt(1);
            }
          }
          proj_c++;
          System.out.println(">>> Project " + projName + " inserted to database");
        } catch (Exception e) {
          proj_x++;
          System.out.println(">>> Could not insert project " + projName);
        }
      } else {
        try {
          Statement stmt = (Statement) conn.createStatement();
          String proj_get = "SELECT id FROM " + PermTools.getDatabase() + ".entities WHERE name='" + projName + "' and type=1";
          ResultSet result = stmt.executeQuery(proj_get);
          while (result.next()) {
            id_project = Integer.parseInt(result.getString("id"));
          }
        } catch (Exception e) {
          System.out.println(">>> Cannot select project ID of " + projName);
        }
      }
      exists = false;
      //ALG
      String algRoot = ATGlobal.getALGORITHMpath(dataroot, projName);
      file = new File(algRoot);
      String[] algorithms = file.list(new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
          return new File(current, name).isDirectory();
        }
      });
      if (algorithms != null) {
        for (String alg : algorithms) {
          algName = alg.split("-")[1];
          for (DBEntity al : alg_list) {
            if (al.name.equals(algName) && al.id_parent == id_project) {
              exists = true;
            }
          }
          if (!exists) {
            try {
              Statement stmt = (Statement) conn.createStatement();
              String alg_insert = "INSERT INTO " + PermTools.getDatabase() + ".entities(name,type,id_parent) VALUES ('" + algName + "',2," + id_project + ")";
              int result = stmt.executeUpdate(alg_insert, stmt.RETURN_GENERATED_KEYS);

              alg_c++;
              System.out.println(">>> Algorithm " + algName + " for project " + projName + " inserted to database");
            } catch (Exception e) {
              alg_x++;
              System.out.println(">>> Could not insert algorithm " + algName + " for project " + projName);
            }
          }
          exists = false;
        }
      }
      //TESTSET

      String testRoot = ATGlobal.getTESTSETpath(dataroot, projName);
      file = new File(testRoot);
      File[] testsets = file.listFiles();
      if (testsets != null) {
        for (File test : testsets) {
          if (test.isFile() && test.getName().contains(".atts")) {
            testName = test.getName().replace(".atts", "");
            for (DBEntity tst : test_list) {
              if (tst.name.equals(testName) && tst.id_parent == id_project) {
                exists = true;
              }
            }
            if (!exists) {
              try {
                Statement stmt = (Statement) conn.createStatement();
                String test_insert = "INSERT INTO " + PermTools.getDatabase() + ".entities(name,type,id_parent) VALUES ('" + testName + "',3," + id_project + ")";
                int result = stmt.executeUpdate(test_insert, stmt.RETURN_GENERATED_KEYS);
                test_c++;
                System.out.println(">>> Testset " + testName + " for project " + projName + " inserted to database");
              } catch (Exception e) {
                test_x++;
                System.out.println(">>> Could not insert testset " + testName + " for project " + projName);
              }
            }
            exists = false;
          }
        }
      }
    }
    System.out.println(">>> #################################");
    System.out.println(">>> ########## REPORT ###############");
    System.out.println(">>> #################################");
    System.out.println(">>>");
    System.out.println(">>> ADDED TO DATABASE:");
    System.out.println(">>>     PROJECTS: " + proj_c);
    System.out.println(">>>     ALGORITHMS: " + alg_c);
    System.out.println(">>>     TESTSETS: " + test_c);
    System.out.println(">>>");
    System.out.println(">>> ERROR WHILE ADDING:");
    System.out.println(">>>     PROJECTS: " + proj_x);
    System.out.println(">>>     ALGORITHMS: " + alg_x);
    System.out.println(">>>     TESTSETS: " + test_x);
    insert_owner_table();
  }

  public static void load_owner_table() {
    try {
      Statement stmt = (Statement) conn.createStatement();
      String select = "SELECT * FROM " + PermTools.getDatabase() + ".owners";
      ResultSet rs = stmt.executeQuery(select);
      owner_list = new ArrayList<DBOwner>();
      while (rs.next()) {
        int id = rs.getInt("id");
        int id_owner = rs.getInt("id_owner");
        int id_entity = rs.getInt("id_entity");
        owner_list.add(new DBOwner(id, id_owner, id_entity));
      }
    } catch (Exception e) {
      System.out.println(">>> Error loading owners");
    }
  }

  public static void set_owner_table(int id_owner, int id_entity) {
    try {
      Statement stmt = (Statement) conn.createStatement();
      String select = "SELECT * FROM " + PermTools.getDatabase() + ".owners where id_entity=" + id_entity;
      ResultSet rs = stmt.executeQuery(select);
      if (!rs.next()) {
        String insert = "INSERT INTO " + PermTools.getDatabase() + ".owners(id_owner,id_entity) VALUES (" + id_owner + "," + id_entity + ")";
        stmt.executeUpdate(insert);
      }
    } catch (Exception e) {
      System.out.println(e);
      System.out.println(">>> Error loading owners");
    }
  }

  public static void insert_owner_table() {
    load_owner_table();
    int count = 0;

    boolean inside;
    for (DBEntity proj : project_list) {
      inside = false;
      for (DBOwner own : owner_list) {
        if (proj.id == own.id_entity) {
          inside = true;
        }
      }
      if (!inside) {
        count++;
        set_owner_table(1, proj.id);
      }
    }
    for (DBEntity alg : alg_list) {
      inside = false;
      for (DBOwner own : owner_list) {
        if (alg.id == own.id_entity) {
          inside = true;
        }
      }
      if (!inside) {
        count++;
        set_owner_table(1, alg.id);
      }
    }
    for (DBEntity test : test_list) {
      inside = false;
      for (DBOwner own : owner_list) {
        if (test.id == own.id_entity) {
          inside = true;
        }
      }
      if (!inside) {
        count++;
        set_owner_table(1, test.id);
      }
    }
    System.out.println("ADDED OWNERS: " + count);
  }

  private static void help_for_command(String command) {
    help_for_command("Missing arguments", command);
  }  
  private static void help_for_command(String eMsg, String command) {
    if (!eMsg.isEmpty())
      System.out.println(">>> Error: " + eMsg + "\n");
    
    String [] msgs = help_msg.split("\n\n");
    for (String msg : msgs) {
      if (msg.trim().toUpperCase().startsWith(command.toUpperCase() + " "))
        System.out.println(msg);
    }
    System.out.println("");
  }
  
  public static String do_users(String ... sinput) {    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStream old = System.out;
    System.setOut(ps);

    main(sinput);

    System.out.flush();
    System.setOut(old);    
    return baos.toString();
  }
  
  public static boolean main_switch(String[] sinput) {
    boolean result = true;
    
    if (sinput.length > 1 && sinput[1].equals("?")) {
      System.out.println("");
      help_for_command("", sinput[0]);
      return true;
    }
    
    switch (sinput[0]) {
      case "exit":
        break;
      case "help":
        help();
        break;
      case "init":
        DatabaseInit.init();
        break;
      case "adduser":
        try {
          adduser(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("adduser");
        }
        break;
      case "addgroup":
        try {
          addgroup(sinput[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("addgroup");
        }
        break;
      case "moduser":
        try {
          moduser(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("moduser");
        }
        break;
      case "showusers":
        if (sinput.length > 1) {
          showUsers(sinput[1]);
        } else {
          showUsers("");
        }
        break;
      case "showgroups":
        showGroups();
        break;
      case "showpermissions":
        showPermissions();
        break;
      case "listentities":
        load_entites(sinput.length > 1 ? sinput[1] : "");
        
        if (format.equals("json")) {
          Gson gson = new Gson();
          List<DBEntity> combined = new ArrayList<DBEntity>();
          combined.addAll(project_list);
          combined.addAll(alg_list);
          combined.addAll(test_list);
          System.out.println(gson.toJson(combined));
        } else {
          listEntities();
        }
        break;
      case "chmod":
        try {
          String errorMsg = chmod(sinput[1], sinput[2], sinput[3]);
          if (!errorMsg.isEmpty())
            help_for_command(errorMsg, "chmod");          
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("chmod");
        }        
        break;
      case "getentityid":
        try {
          System.out.println(findEntityId(sinput[1]));
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("chmod");
        }        
        break;                
      case "changeuserstatus":
        try {
          changeUserStatus(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("changeuserstatus");
        }
        break;
      case "changegroupstatus":
        try {
          changeGroupStatus(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {          
          help_for_command("changegroupstatus");
        }
        break;
      case "userperm":
        try {          
          int entity_id=ALL_PROJECTS_ID;   // default
          
          if (sinput.length > 2) { 
            if (sinput[2].equals("-all"))
              entity_id = ALL_ENTITIES_ID; // all entities
            else if (NumberUtils.isNumber(sinput[2])) 
              entity_id = Integer.parseInt(sinput[2]);
            else
              entity_id = findEntityId(sinput[2]);
          }                    
          userperm(sinput[1], entity_id);
          
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("userperm");
        }
        break;
      case "canuser":
        try {
          if (format.equals("json")) 
            System.out.println(new Gson().toJson(can_user(sinput[1], sinput[2], sinput[3])));
          else
            System.out.println(can_user(sinput[1], sinput[2], sinput[3]));
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("canuser");
          System.out.println(e);
        }
        break;
      case "setowner":
        try {
          setowner(sinput[1], sinput[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println(e.toString());
          help_for_command("setowner");
        }
        break;
        
      case "set_default_owner":
        try {
          setDefaultOwnerForAllEntities();
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println(e.toString());
          help_for_command("set_default_owner");
        }
        break;
      case "showowner":
        try {
          String [] parts = sinput[1].split("/");
          if (parts.length == 1)
            showOwnerProj(parts[0]);
          else if (parts.length == 2)
            showOwnerAlg(parts[0], parts[1]);
          else if (parts.length == 3)
            showOwnerTest(parts[0], parts[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
          help_for_command("showowner");
        }
        break;
        
      case "setformat":
        if (sinput[1].equals("json")) format="json"; else format="string";
        break;
        
      default:
        result = false;
        break;
    }
    return result;
  }

  private static void doConsoleInput() {
    while (true) {
      Scanner reader = new Scanner(System.in);
      System.out.print(">>> ");
      String input = reader.nextLine();

      if (input.equals("")) {
        continue;
      }
      String[] sinput = input.split(" ");

      main_switch(sinput);

      if (input.equals("exit")) {
        break;
      }
    }
  }
  
  private static void doConsoleInputWithJLIne() {
    try {
      ConsoleReader reader = new ConsoleReader();

      reader.setPrompt(">>> ");

      List<Completer> completors = new LinkedList<Completer>();
      completors.add(new StringsCompleter("cls", "quit", "exit", "chmod", "getentityid",
              "help","init","adduser","addgroup","moduser","showusers",
              "showgroups","showpermissions","listentities","changeuserstatus",
              "changegroupstatus",
              "userperm","canuser","set_default_owner","setowner", "showowner", "setformat"
      ));

      for (Completer c : completors) {
        reader.addCompleter(c);
      }

      String line;
      PrintWriter out = new PrintWriter(reader.getOutput());

      while ((line = reader.readLine()) != null) {
        if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
          System.exit(0);
        }
        if (line.equalsIgnoreCase("cls")) {
          reader.clearScreen();
          continue;
        }
        
        String[] sinput = line.split(" ");
        boolean result = main_switch(sinput);

        if (!result) {
          out.println("Unknown command \"" + line + "\"");        
          out.flush();
        }
        
      }

    } catch (Exception e) {
      System.out.println("Error: " + e.toString());
    }
    
  }

  private static Options getOptions() {
    Options options = new Options();

    Option data_root = OptionBuilder.withArgName("folder")
	    .withLongOpt("data_root")
	    .hasArg(true)
	    .withDescription("use this folder as data_root; default value in $ALGATOR_DATA_ROOT (if defined) or $ALGATOR_ROOT/data_root")
	    .create("dr");
    
    Option algator_root = OptionBuilder.withArgName("folder")
            .withLongOpt("algator_root")
            .hasArg(true)
            .withDescription("use this folder as algator_root; default value in $ALGATOR_ROOT")
            .create("r");
    
    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF (default), 1 = some, 2 = all")
            .create("v");

    options.addOption(data_root);
    options.addOption(algator_root);
        
    options.addOption(verbose);
            
    options.addOption("u", "usage", false, "print usage guide");
    
    return options;
  }

  
  public static void main(String[] args) {

    Options options = getOptions();

    CommandLineParser parser = new BasicParser();
    try {
      CommandLine line = parser.parse(options, args);

      String[] curArgs = line.getArgs();
            
      String algatorRoot = ATGlobal.getALGatorRoot();
      if (line.hasOption("algator_root")) {
        algatorRoot = line.getOptionValue("algator_root");        
      }
      ATGlobal.setALGatorRoot(algatorRoot);

      String dataRoot = ATGlobal.getALGatorDataRoot();
      if (line.hasOption("data_root")) {
	dataRoot = line.getOptionValue("data_root");
      }
      ATGlobal.setALGatorDataRoot(dataRoot);

      ATGlobal.logTarget = ATLog.TARGET_STDOUT;
      ATLog.setLogTarget(ATGlobal.logTarget);

      conn = PermTools.connectToDatabase();
      if (conn == null) {
        String err = PermTools.preventConnection() ?
           "File $ALGATOR_ROOT/mysql-stop is preventing connection." :
           "Please, check your settings in the algator.acfg file.";
        
        System.out.println("Can not connect to database. " + err);
        return;
      }

      // execute action defined with arguments or ...
      if (curArgs.length > 0) {
        format = "json";
        main_switch(curArgs);
        return;
      }

      // ... open a console
      format = "string";
      System.out.println(
              "    ___     __    ______        __              \n"
            + "   /   |   / /   / ____/____ _ / /_ ____   _____\n"
            + "  / /| |  / /   / / __ / __ `// __// __ \\ / ___/\n"
            + " / ___ | / /___/ /_/ // /_/ // /_ / /_/ // /    \n"
            + "/_/  |_|/_____/\\____/ \\__,_/ \\__/ \\____//_/     \n"
            + "                                                ");
      System.out.println(">>> Welcome to Algator Users panel!");
      doConsoleInputWithJLIne();
      System.out.println(">>> Goodbye!");
    } catch (Exception e) {}

  }
}
