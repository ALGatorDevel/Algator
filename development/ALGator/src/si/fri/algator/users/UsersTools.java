package si.fri.algator.users;

import com.google.gson.Gson;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.math.NumberUtils;
import si.fri.algator.global.ATLog;

import si.fri.algator.database.Database;
import static si.fri.algator.database.Database.DATABASE;
import static si.fri.algator.database.Database.getID;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.Password;

/**
 *
 * @author Gregor
 */
public class UsersTools {
  
  public static final int ALL_ENTITIES_ID = -3;
  public static final int ALL_PROJECTS_ID = -2;
  public static final int INVALID_ID      = -1;

  public static String format = "json";
  
  static String[] STATUS = {"inactive", "active"};
  
  public static boolean addgroup(String name) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      if (getGroupID(name, true) < 0) {
        String insert = "INSERT INTO " + Database.getDatabase() + ".u_groups(name) VALUES ('" + name + "')";

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
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
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

      String select = "SELECT * from " + Database.getDatabase() + ".u_group_users WHERE id_group=" + id_group + " AND id_user=" + id_user + "";
      ResultSet rs = stmt.executeQuery(select);

      if (rs.next()) {
        String delete = "DELETE FROM " + Database.getDatabase() + ".u_group_users WHERE id_group=" + id_group + " and id_user=" + id_user;
        int result = stmt.executeUpdate(delete);

        if (result > 0) {
          System.out.println(">>> User removed from group!");
          return true;
        } else {
          System.out.println(">>> Error while deleting user from group!");
          return false;
        }

      } else {
        String insert = "INSERT INTO " + Database.getDatabase() + ".u_group_users(id_group,id_user) VALUES ('" + id_group + "','" + id_user + "')";
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
          "SELECT * from " + Database.getDatabase() + ".u_entities WHERE name='%s' AND type=2 AND id_parent=%d", parts[1], id_project));
      } else { //looking for testset
        return getID(String.format(
          "SELECT * from " + Database.getDatabase() + ".u_entities WHERE name='%s' AND type=3 AND id_parent=%d", parts[2], id_project));      
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
        id_perm = getID(String.format("SELECT * from " + Database.getDatabase() + ".u_permissions WHERE permission_code='%s'", perm));            
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
         "SELECT * FROM " + Database.getDatabase() + ".u_permissions_%ss WHERE id_%s=%d AND id_entity=%d AND id_permission=%d",
         usergroup,usergroup,id_usergroup,id_entity,id_perm
      );
      
      if (Database.getConnectionToDatabase() == null) return "Invalid database connection.";
      
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      ResultSet rs = stmt.executeQuery(select);
            
      boolean permExists = rs.next();
  
      
      String sql;
      if (addPermission) {
        if (permExists) return "Permission already exists.";
        sql = String.format(
          "INSERT INTO " + Database.getDatabase() + ".u_permissions_%ss (id_%s,id_entity,id_permission) VALUES (%d,%d,%d)",
          usergroup, usergroup, id_usergroup, id_entity, id_perm
        );         
      } else {
        if (!permExists) return "Permission does not exist.";
        sql = String.format(
          "DELETE FROM " + Database.getDatabase() + ".u_permissions_%ss WHERE id_%s=%d AND id_entity=%d AND id_permission=%d",
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


  public static DBUser getUserByName(String name) {
    String sql  = String.format("SELECT * from %s.auth_user WHERE username='%s'", DATABASE, name);
    DBUser user = null;
    
    try {
      Connection conn = Database.getConnectionToDatabase();
      Statement stmt = (Statement) conn.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        user = new DBUser(rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getBoolean("is_active"));
      }
    } catch (Exception e) {}
    return user;
  }
  
  /**
   * Checks if the user is in a database and is the password is correct
   */
  public static boolean checkUser(String username, String password) {
    if (username == null || password==null || username.isEmpty() || password.isEmpty()) return false;
    
    DBUser user = getUserByName(username);
    if (user == null) return false;
    
    return Password.checkPassword(user.password, password);
  }
  
  public static void showUsers(String username) {
    String statement = "";
    if (username.equals("")) {
      statement = "SELECT * from " + Database.getDatabase() + ".auth_user";
    } else {
      statement = "SELECT * from " + Database.getDatabase() + ".auth_user WHERE username='" + username + "'";
    }
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      ResultSet rs = stmt.executeQuery(statement);
      if (format.equals("json")) {
        Gson gson = new Gson();
        ArrayList<DBUser> users = new ArrayList<>();
        while (rs.next()) {
          users.add(new DBUser(rs.getInt("id"), rs.getString("username"), rs.getString("password"), rs.getBoolean("is_active")));
        }
        System.out.println(gson.toJson(users));
        return;
      }
      if (!rs.isBeforeFirst()) {
        System.out.println(">>> User with this username does not exist!");
        return;
      }
      System.out.printf("%-5s %-20s %-20s\n", "id", "Username", "status");
      System.out.println("----------------------------------------");
      while (rs.next()) {
        String  lastName = rs.getString("username");
        boolean isActive = rs.getBoolean("is_active");
        String id       = rs.getString("id");
        System.out.printf("%-5s %-20s %-20s", id, lastName, STATUS[isActive?1:0]);
        System.out.println("");
      }
    } catch (SQLException e) {
      System.err.println(e);
    }
  }

  public static void showGroups() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String select = "SELECT * from " + Database.getDatabase() + ".u_groups";
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
      System.out.printf("%-20s %-15s\n", "Group name", "Status");
      System.out.println("------------------------------------");
      while (rs.next()) {
        String  groupName   = rs.getString("name");
        boolean groupStatus = rs.getBoolean("status");
        System.out.printf("%-20s %-15s", groupName, STATUS[groupStatus ? 1 : 0]);
        System.out.println("");
      }
    } catch (SQLException e) {
      System.err.println(e);
    }
  }

  public static void showPermissions() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String select = "SELECT * from " + Database.getDatabase() + ".u_permissions";
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
    DBEntities dbEntities = load_entites("");
    return dbEntities.projects;
  }
  
  public static void listEntities() {    
    DBEntities dbEntities = load_entites("");

    System.out.println("");
    String[] types = {"Proj", "Alg", "Test"};
    System.out.printf("%1$-6s %2$-16s %3$s\n", "ID", "Type", "Entity name");    
    System.out.println("-------------------------------------------");

    for (DBEntity project : dbEntities.projects) {
      System.out.printf("%1$-6s %2$-16s %3$s", project.id, types[project.type - 1], project.name);
      System.out.println("");
      if (dbEntities.alg_map != null && dbEntities.alg_map.containsKey(Integer.toString(project.id))) {
        for (DBEntity algorithm : dbEntities.alg_map.get(Integer.toString(project.id))) {
          System.out.printf("%1$-6s %2$-20s %3$s", algorithm.id, types[algorithm.type - 1], algorithm.name);
          System.out.println("");
        }
      }
      if (dbEntities.test_map != null && dbEntities.test_map.containsKey(Integer.toString(project.id))) {
        for (DBEntity testset : dbEntities.test_map.get(Integer.toString(project.id))) {
          System.out.printf("%1$-6s %2$-20s %3$s", testset.id, types[testset.type - 1], testset.name);
          System.out.println("");
        }
      }
    }
  }

  public static void changeUserStatus(String username, String isActive) {
    int iStatus = Integer.parseInt(isActive);

    if (iStatus < 0 || iStatus > 1) {
      System.out.println("Status number must be 0 or 1!");
      return;
    }
    if (getUserID(username, false) > 0) {
      try {
        Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
        String insert = "UPDATE " + Database.getDatabase() + ".auth_user SET is_active = " + isActive + " WHERE username = '" + username + "'";
        int result = stmt.executeUpdate(insert);

        if (result > 0) {
          System.out.println(">>> " + username + " status changed to: " + STATUS[iStatus]);
        } else {
          System.out.println(">>> Error while adding user to group!");
        }

      } catch (Exception e) {
      }
    }
  }

  public static void changeGroupStatus(String groupname, String status) {
    int iStatus = Integer.parseInt(status);

    if (iStatus < 0 || iStatus > 1) {
      System.out.println("Status number must be 0 or 1!");
      return;
    }
    if (getGroupID(groupname, false) > 0) {
      try {
        Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
        String insert = "UPDATE " + Database.getDatabase() + ".u_groups SET status = " + status + " WHERE name = '" + groupname + "'";
        int result = stmt.executeUpdate(insert);

        if (result > 0) {
          System.out.println(">>> " + groupname + " status changed to: " + STATUS[iStatus]);
        } else {
          System.out.println(">>> Error while adding user to group!");
        }

      } catch (Exception e) {
      }
    }
  }

  public static int getUserID(String username, boolean output) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".auth_user WHERE username='" + username + "'";

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

  public static int getGroupID(String groupname, boolean output) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".u_groups WHERE name='" + groupname + "'";
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

  public static int getPermID(String permname, boolean output) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".u_permissions WHERE permission_code='" + permname + "'";
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

  public static int getProjectID(String projectName, boolean output) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".u_entities WHERE name='" + projectName + "' AND type=1";
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

  public static ArrayList<Integer> projectAlgs(int id_project) {
    ArrayList<Integer> alg_ids = new ArrayList<Integer>();
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      String select = "SELECT * from " + Database.getDatabase() + ".u_entities WHERE id_parent=" + id_project + " AND type=2";
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
  
  public static DBEntities load_entites() {
    return load_entites("-all");
  }

  /**
   * param: "-all" ... load all entities, "" ... load all projects, "proj_name" ... load all entities of the given project
   */
  public static DBEntities load_entites(String param) {
    DBEntities dbEntities = new DBEntities();

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
      
      if (Database.getConnectionToDatabase() == null) return dbEntities;      
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String select = "SELECT * from " + Database.getDatabase() + ".u_entities" + where;
      ResultSet rs = stmt.executeQuery(select);
      while (rs.next()) {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int type = rs.getInt("type");
        int id_parent = rs.getInt("id_parent");
        List tmp_map = null;
        switch (type) {
          case 1:
            dbEntities.projects.add(new DBEntity(id, name, type, id_parent));
            break;
          case 2:
            tmp_map = dbEntities.alg_map.get(Integer.toString(id_parent));
            if (tmp_map == null) {
              tmp_map = new ArrayList<DBEntity>();
            }
            tmp_map.add(new DBEntity(id, name, type, id_parent));
            dbEntities.alg_map.put(Integer.toString(id_parent), tmp_map);
            dbEntities.algorthms.add(new DBEntity(id, name, type, id_parent));
            break;
          case 3:
            tmp_map = dbEntities.test_map.get(Integer.toString(id_parent));
            if (tmp_map == null) {
              tmp_map = new ArrayList<DBEntity>();
            }
            tmp_map.add(new DBEntity(id, name, type, id_parent));
            dbEntities.test_map.put(Integer.toString(id_parent), tmp_map);
            dbEntities.tests.add(new DBEntity(id, name, type, id_parent));
            break;
        }
      }      
    } catch (SQLException e) {
      ATLog.log(e.toString(), 0);
    }
    return dbEntities;
  }

  public static DBPermissions load_perm(String username) {
    DBPermissions dbPermissions = new DBPermissions();
    
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
    
      String selectProj=String.format(ATTools.getResourceFile("sql/project_permissions.sql"), username);
      String selectAlg = String.format(ATTools.getResourceFile("sql/algorithm_permissions.sql"), username);
      String selectTest = String.format(ATTools.getResourceFile("sql/testset_permissions.sql"), username);

      ResultSet rs_proj = stmt.executeQuery(selectProj);
      while (rs_proj.next()) {
        dbPermissions.project_permissions.add(new DBEntityPermission(rs_proj.getInt("id"), rs_proj.getString("name"), rs_proj.getInt("type"), rs_proj.getString("permissions"), rs_proj.getInt("parent_id"), rs_proj.getString("parent_name")));
      }

      ResultSet rs_alg = stmt.executeQuery(selectAlg);
      while (rs_alg.next()) {
        dbPermissions.algorithm_permissions.add(new DBEntityPermission(rs_alg.getInt("id"), rs_alg.getString("name"), rs_alg.getInt("type"), rs_alg.getString("permissions"), rs_alg.getInt("parent_id"), rs_alg.getString("parent_name")));
      }

      ResultSet rs_test = stmt.executeQuery(selectTest);
      while (rs_test.next()) {
        dbPermissions.test_permissions.add(new DBEntityPermission(rs_test.getInt("id"), rs_test.getString("name"), rs_test.getInt("type"), rs_test.getString("permissions"), rs_test.getInt("parent_id"), rs_test.getString("parent_name")));
      }

    } catch (Exception e) {
      ATLog.log(e.toString(),0);
    }
    
    return dbPermissions;
  }

  public static void print_permissions(String username) {
    DBPermissions dbPermissions = load_perm(username);
    
    if (format.equals("json")) {
      Gson gson = new Gson();
      List<DBEntityPermission> combined = new ArrayList<DBEntityPermission>();
      combined.addAll(dbPermissions.project_permissions);
      combined.addAll(dbPermissions.algorithm_permissions);
      combined.addAll(dbPermissions.test_permissions);
      System.out.println(gson.toJson(combined));
      return;
    }
    System.out.println("");
    String[] types = {"Proj", "Alg", "Test"};
    System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", "ID", "Type", "Entity name", "Permission", "P ID", "P name");
    System.out.println("");
    System.out.printf("___________________________________________________________________________");
    System.out.println("");
    for (DBEntityPermission project : dbPermissions.project_permissions) {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", project.id, types[project.type - 1], project.name, project.permission, project.parent_id, project.parent_name);
      System.out.println("");
    }
    for (DBEntityPermission algorithm : dbPermissions.algorithm_permissions) {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", algorithm.id, types[algorithm.type - 1], algorithm.name, algorithm.permission, algorithm.parent_id, algorithm.parent_name);
      System.out.println("");
    }
    for (DBEntityPermission test : dbPermissions.test_permissions) {
      System.out.printf("%1$-6s %2$-6s %3$-26s %4$-14s %5$-10s %6$-10s", test.id, types[test.type - 1], test.name, test.permission, test.parent_id, test.parent_name);
      System.out.println("");
    }

  }

  public static void userperm(String username, int entity_id) {
    String[] types = {"Proj", "Alg", "Test"};
    DBPermissions dbPermissions = load_perm(username);
    
    List<DBEntityPermission> perms = new ArrayList<DBEntityPermission>(dbPermissions.project_permissions);
    perms.addAll(dbPermissions.algorithm_permissions);
    perms.addAll(dbPermissions.test_permissions);
    
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
    DBPermissions dbPermissions = load_perm(username);
    for (DBEntityPermission project : dbPermissions.project_permissions) {
      if (project.id == entity_id) {
        if (project.permission.equals("all") || project.permission.equals(permission)) {
          return true;
        }
      }
    }
    for (DBEntityPermission algorithm : dbPermissions.algorithm_permissions) {
      if (algorithm.id == entity_id) {
        if (algorithm.permission.equals("all") || algorithm.permission.equals(permission)) {
          return true;
        }
      }
    }
    for (DBEntityPermission test : dbPermissions.test_permissions) {
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
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

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
              + "FROM " + Database.getDatabase() + ".u_entities AS t1 "
              + "JOIN " + Database.getDatabase() + ".u_owners AS t2 on t1.id=t2.id_entity "
              + "JOIN " + Database.getDatabase() + ".auth_user AS t3 on t2.id_owner=t3.id "
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
    DBEntities dbEntities = load_entites();
    try {
      int id      = INVALID_ID;
      int proj_id = INVALID_ID;
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      for (DBEntity proj : dbEntities.projects) {
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

      List<DBEntity> alg_list = dbEntities.alg_map.get(Integer.toString(proj_id));

      for (DBEntity alg : alg_list) {
        if (alg.name.equals(algorithmName)) {

          String select = "SELECT t1.id AS id_entity,"
                  + "t1.name AS entity_name,"
                  + "t1.type,"
                  + "t1.id_parent,"
                  + "t3.name AS username "
                  + "FROM " + Database.getDatabase() + ".u_entities AS t1 "
                  + "JOIN " + Database.getDatabase() + ".u_owners AS t2 on t1.id=t2.id_entity "
                  + "JOIN " + Database.getDatabase() + ".auth_user AS t3 on t2.id_owner=t3.id "
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
    DBEntities dbEntities = load_entites();
    
    try {
      int id      = INVALID_ID;
      int proj_id = INVALID_ID;
      int alg_id  = INVALID_ID;

      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();

      for (DBEntity proj : dbEntities.projects) {
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

      List<DBEntity> test_list = dbEntities.test_map.get(Integer.toString(proj_id));
      for (DBEntity test : test_list) {
        if (test.name.equals(testName)) {
          String select = "SELECT t1.id AS id_entity,"
                  + "t1.name AS entity_name,"
                  + "t1.type,"
                  + "t1.id_parent,"
                  + "t3.name AS username "
                  + "FROM " + Database.getDatabase() + ".u_entities AS t1 "
                  + "JOIN " + Database.getDatabase() + ".u_owners AS t2 on t1.id=t2.id_entity "
                  + "JOIN " + Database.getDatabase() + ".auth_user AS t3 on t2.id_owner=t3.id "
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
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String owner_find = String.format(
         "SELECT * FROM " + Database.getDatabase() + ".u_owners where id_entity=%s",  id_entity
      );
      boolean result = stmt.execute(owner_find);  

      String sql;
      ResultSet rst = stmt.getResultSet();
      if (rst.next()) 
        sql = String.format(
             "UPDATE " + Database.getDatabase() + ".u_owners SET id_owner=%s WHERE id_entity=%s", id_owner, id_entity
        );
      else
        sql = String.format(
             "INSERT INTO " + Database.getDatabase() + ".u_owners(id_owner,id_entity) VALUES (%s,%s)", id_owner, id_entity
        );        
      
      stmt.executeUpdate(sql);      
    } catch (Exception e) {
      System.out.println("Error: " + e.toString());
    }
  }
  
  // returns the list of Projects in data_root
  private static String[] getListOfProjects() {
    try {
      File file = new File(ATGlobal.getPROJECTSfolder(ATGlobal.getALGatorDataRoot()));
      String[] projects = file.list((File current, String name) -> new File(current, name).isDirectory());
      return Stream.of(projects).filter(p -> p.startsWith("PROJ-")).map(p -> p.substring(5)).toArray(String[]::new);
    } catch (Exception e) {return new String[]{};}
  }
  private static String[] getListOfAlgorithms(String projName) {
    try {
      File file = new File(ATGlobal.getALGORITHMpath(ATGlobal.getALGatorDataRoot(), projName));
      String[] algs = file.list((File current, String name) -> new File(current, name).isDirectory());
      return Stream.of(algs).filter(p -> p.startsWith("ALG-")).map(p -> p.substring(4)).toArray(String[]::new);
    } catch (Exception e) {return new String[]{};}      
  }

  private static String[] getListOfTestsets(String projName) {
    try {
      File file = new File(ATGlobal.getTESTSETpath(ATGlobal.getALGatorDataRoot(), projName));
      return Stream.of(file.listFiles()).filter((File t)  -> (t.isFile() && t.getName().endsWith(".atts"))).
        map((File t) -> t.getName().replace(".atts", "")).toArray(String[]::new);
    } catch (Exception e) {return new String[]{};}            
  }
  
  /**
   * Inserts the entty and returns the ID of inserted entity
   */
  private static int insertEntity(String entityName, int entityType, int id_parent) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String sql = id_parent==0 ?
              String.format("INSERT INTO %s.u_entities(name,type) VALUES ('%s',%d)", Database.getDatabase(), entityName, entityType) :
              String.format("INSERT INTO %s.u_entities(name,type,id_parent) VALUES ('%s',%d, %d)", Database.getDatabase(), entityName, entityType, id_parent) ;
   
      int result = stmt.executeUpdate(sql, stmt.RETURN_GENERATED_KEYS);

      int res = -1;
      if (result > 0) {
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
          res = rs.getInt(1);
        }
      }
      return res;
    } catch (Exception e) {
      return -1;
    }
  }
  
  public static int[] insertProjectToDB(String projName, String owner) {
    int proj_c=0, proj_x=0, alg_c=0, alg_x=0, test_c=0, test_x=0;
    
    ArrayList<Integer> newEntitiesID = new ArrayList<>();
    ArrayList<String>  newEntitiesNames = new ArrayList<>();
    
    DBEntities dbEntities = load_entites();
  
    boolean exists = false;
    int id_project = INVALID_ID;
    for (DBEntity pl : dbEntities.projects) {
      if (pl.name.equals(projName)) {
        exists = true; break;
      }
    }
    if (!exists) {
      id_project = insertEntity(projName, 1, 0);
      if (id_project>=0) {newEntitiesID.add(id_project);newEntitiesNames.add(projName); proj_c++;} else proj_x++;      
    }
    if (id_project < 0) try { // ce projekt ze obstaja, pridobim njegov id (da bom otroke vstavil pod njega)
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String proj_get = "SELECT id FROM " + Database.getDatabase() + ".u_entities WHERE name='" + projName + "' and type=1";
      ResultSet result = stmt.executeQuery(proj_get);
      while (result.next()) {
        id_project = Integer.parseInt(result.getString("id"));
      }
    } catch (Exception e) {}
    
    
    // ALGORITHMS
    if (id_project >= 0) {    
      String[] algorithms = getListOfAlgorithms(projName);
      for (String algName : algorithms) {
        exists = false;
        for (DBEntity al : dbEntities.algorthms) {
          if (al.name.equals(algName) && al.id_parent == id_project) {
            exists = true;break;
          }
        }
        if (!exists) {
          int id_algorithm = insertEntity(algName, 2, id_project);
          if (id_algorithm >=0) {alg_c++; newEntitiesID.add(id_algorithm);newEntitiesNames.add(projName+"/"+algName);}else alg_x++;
        }        
      }
      
      //TESTSET  
      for (String testName : getListOfTestsets(projName)) {
        exists = false;
        for (DBEntity tst : dbEntities.tests) {
          if (tst.name.equals(testName) && tst.id_parent == id_project) {
            exists = true; break;
          }
        }
        if (!exists) {
          int id_test = insertEntity(testName, 3, id_project);
          if (id_test>=0) {test_c++; newEntitiesID.add(id_test);newEntitiesNames.add(projName+"//"+testName);}else test_x++;
        }
      }
    }
    // set owner for all new entities
    for (Integer newEntity : newEntitiesID) 
      setowner(owner, String.valueOf(newEntity));
    
    System.out.println("Entites added: " + Arrays.toString(newEntitiesNames.toArray(new String[]{})));
    return new int[]{proj_c, proj_x, alg_c, alg_x, test_c, test_x};
  }
  
  public static void insertAllProjectsToDB(String owner) {   
    int proj_c = 0, alg_c = 0, test_c = 0, proj_x = 0, alg_x = 0, test_x = 0;
    for (String proj : getListOfProjects()) {
      System.out.println("Project: " + proj);
      int xc[] = insertProjectToDB(proj, owner);
      proj_c+=xc[0];proj_x+=xc[1];
      alg_c +=xc[2];alg_x +=xc[3];
      test_c+=xc[4];test_x+=xc[5];
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
  }

  public static ArrayList<DBOwner> load_owner_table() {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String select = "SELECT * FROM " + Database.getDatabase() + ".u_owners";
      ResultSet rs = stmt.executeQuery(select);
      ArrayList<DBOwner> owner_list = new ArrayList<DBOwner>();
      while (rs.next()) {
        int id = rs.getInt("id");
        int id_owner = rs.getInt("id_owner");
        int id_entity = rs.getInt("id_entity");
        owner_list.add(new DBOwner(id, id_owner, id_entity));
      }
      return owner_list;
    } catch (Exception e) {
      System.out.println(">>> Error loading owners");
    }
    return null;
  }

  public static void set_owner(int id_owner, int id_entity) {
    try {
      Statement stmt = (Statement) Database.getConnectionToDatabase().createStatement();
      String select = "SELECT * FROM " + Database.getDatabase() + ".u_owners where id_entity=" + id_entity;
      ResultSet rs = stmt.executeQuery(select);
      if (!rs.next()) {
        String insert = "INSERT INTO " + Database.getDatabase() + ".u_owners(id_owner,id_entity) VALUES (" + id_owner + "," + id_entity + ")";
        stmt.executeUpdate(insert);
      }
    } catch (Exception e) {
      System.out.println(e);
      System.out.println(">>> Error loading owners");
    }
  }
  
  public static int addGroupPermission(String groupName, String permission, int entityID) {
    int id_group      = getGroupID(groupName,  false);
    int id_permission = getPermID (permission, false);
    
    try {
      Statement stmt = Database.getConnectionToDatabase().createStatement();
      String insert = String.format("INSERT INTO %s.u_permissions_group(id_group,id_entity,id_permission) VALUES (%d,%d,%d)", DATABASE, id_group, entityID, id_permission);
      return stmt.executeUpdate(insert);
    } catch (Exception e) {
     return -1; 
    }
  }

  /**
   * Uporabljamo po vstavljanju projekta v sistem. Po vstavljanu entitet (algoritem, testset) se uporabi metodo setEntityPermissions
   */  
  public static boolean setProjectPermissions(String username, String proj_name) {
    int id_project   = getProjectID(proj_name, false);
    if (id_project < 0 ) {
      id_project = insertEntity(proj_name, 1, 0);
      set_owner(getUserID(username, false), id_project);
      int result = addGroupPermission("Everyone", "can_read", id_project);

      if (result > 0) {
        ATLog.log("Project added to database!",1);
        return true;
      }   
    } else {
      ATLog.log("Project already exists!",1);
      return false;
    }
    return false;
  }
 
  /**
   * Uporabljamo po vstavljanju entitete (algoritem, testset) v sistem. Po vstavljanu projekta se uporabi metodo setProjectPermissions
   */
  public static boolean setEntityPermissions(String username, String proj_name, String entity_name, int entity_type) {
    int id_project   = getProjectID(proj_name, false);
    if (id_project >= 0 ) {
      int id_entity = insertEntity(entity_name, entity_type, id_project);
      set_owner(getUserID(username, false), id_entity);
      int result = addGroupPermission("Everyone", "can_read", id_entity);

      if (result > 0) {
        String entname = entity_type == 2 ? "Algorithm" : "Testset";
        ATLog.log(entname + " added to database!",1);
        return true;
      }   
    } else {
      ATLog.log("Can't find parent project in the database!",1);
      return false;
    }
    return false;
  }
}
