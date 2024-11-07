package si.fri.algator.ausers;

import si.fri.algator.ausers.dto.DTOUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.RandomStringUtils;
import si.fri.algator.database.Database;
import si.fri.algator.global.ATLog;

/**
 *
 * @author tomaz
 */
public class AUsersTools {
  final static String userSelectSQL = "SELECT uid, username, first_name, last_name, email, date_joined, last_login, is_superuser, is_staff, is_active FROM ausers_user";    

  static final ArrayList<String> tabelePrograma = new ArrayList(Arrays.asList(new String[]
    {"auth_group", "auth_group_permissions", "auth_permission", "ausers_entities", 
     "ausers_entity_permission", "ausers_entitypermissiongroup", 
     "ausers_entitypermissionuser", "ausers_entitytype", "ausers_group", 
     "ausers_group_user", "ausers_permissiontype", "ausers_user", "ausers_user_groups", 
     "ausers_user_user_permissions", "django_admin_log", "django_content_type", 
     "django_migrations", "django_session",}));
  
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
      return new ArrayList<>(tabelePrograma);
    }    
  }  
    
  public static boolean databaseAndTablesExist(){   
    return getMissingTables().isEmpty();
  }

  public static boolean checkUser(String username, String password) {
    return true;
  }
  
  public static String getUniqueDBid(String prefix) {
    return prefix + RandomStringUtils.random(12, true, true);
  }
  
  public static ArrayList<DTOUser> readUsers() {
    return readUsers("");
  }
  public static ArrayList<DTOUser> readUsers(String where) {
    ArrayList<DTOUser> users = new ArrayList<>();
    final String sql = userSelectSQL + where;
    try {
      Connection con = Database.getConnectionToDatabase();
      if (con != null) {
        PreparedStatement stmt = con.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
          DTOUser user = new DTOUser(); 
        
          user.setUid(rs.getString("uid"));
          user.setUsername(rs.getString("username"));
          user.setFirst_name(rs.getString("first_name"));
          user.setLast_name(rs.getString("last_name"));
          user.setEmail(rs.getString("email"));
          user.setDate_joined(rs.getDate("date_joined"));
          user.setLast_login(rs.getDate("last_login"));
          user.setIs_staff(rs.getBoolean("is_staff"));
          user.setIs_superuser(rs.getBoolean("is_superuser"));
          user.setIs_active(rs.getBoolean("is_active"));
          
          users.add(user);
        } 
      }
    } catch (Exception e) {
      ATLog.log("Error reading 'authuser_users'. Error: " + e.getMessage(), 0);
    }
    return users;
  }
  
  public static DTOUser getUser(String uid) {
    ArrayList<DTOUser> users = readUsers(" where uid='"+uid+"'");
    if (!users.isEmpty())
      return users.get(0);
    else
      return null;
  }
}
