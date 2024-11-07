package si.fri.algator.ausers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import si.fri.algator.ausers.dto.DTOEntity;
import si.fri.algator.ausers.dto.DTOUser;
import si.fri.algator.ausers.dto.DTOGroup;
import static si.fri.algator.ausers.AUsersHelper.isEmptyOrNull;
import si.fri.algator.ausers.dto.PermissionTypes;
import si.fri.algator.database.Database;

/**
 *
 * @author tomaz
 */
public class CanUtil {
  
  public static boolean can(final String uid, String eid, final String codename) {
    // emtpy uid or eid?
    if (isEmptyOrNull(uid) || isEmptyOrNull(eid)) return false;
    
    // in Anonymous mode user can do everything
    if (Database.isAnonymousMode()) return true;
    
    // nonexisting user?
    AUsersDAO dao = new AUsersDAO();
    DTOUser user = AUsersTools.getUser(uid);
    if (user == null) user = AUsersTools.getUser(DTOUser.USER_ANONYMOUS);

    // nonexisting right?
    long pValue = PermissionTypes.getPermissionValue(codename);
    if (pValue == 0) return false;

    // nonexisting entity
    DTOEntity e = dao.getEntity(eid);
    if (e == null) return true;
    
    // superuser or owner
    if (user.isIs_superuser() || e.getOwner().equals(uid))  return true;
    
    if (e.isPrivate()) return false;

    HashMap<String, Integer> permissions = getEntityPermissions(uid);
    HashMap<String, String> hiearchy = getParentHierarchyMap(eid);
    int permValue = 0;
    while (eid != null) {
      permValue |= permissions.getOrDefault(eid, 0);
      eid = hiearchy.getOrDefault(eid, null);
    }
    return contains(permValue, pValue);
  }

  // Returns HashMap with hiearchy (eid, parent) from given entity to the top (null)
  public static HashMap<String, String> getParentHierarchyMap(String id) {
    HashMap<String, String> hierarchyMap = new HashMap<>();
    Connection conn = Database.getConnectionToDatabase();
    String query = "SELECT parent_id FROM ausers_entities WHERE id = ?";
    if (conn != null) try ( PreparedStatement stmt = conn.prepareStatement(query)) {
      String currentId = id;
      while (currentId != null) {
        stmt.setString(1, currentId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
          String parentId = rs.getString("parent_id");
          hierarchyMap.put(currentId, parentId);
          currentId = parentId;
        } else {
          break;
        }
      }
    } catch (Exception e) {
    }
    return hierarchyMap;
  }

  // Returns Hashmap (entity_id, permissionValue) where permissionValue is sum (|) of all
  // permissions for this entity for user and all groups it belongs to
  public static HashMap<String, Integer> getEntityPermissions(String uid) {
    HashMap<String, Integer> permissions = new HashMap<>();
    Connection conn = Database.getConnectionToDatabase();
    if (conn != null) try {
      // does user with uid exist?
      String userQuery = "SELECT COUNT(*) FROM ausers_user WHERE uid = '" + uid + "'";
      PreparedStatement stmt = conn.prepareStatement(userQuery);
      ResultSet rs = stmt.executeQuery();
      boolean isUser = rs.next() && (rs.getInt(1) > 0);

      // Fetch group_ids for the user
      StringBuilder groupIds = new StringBuilder("'" + (isUser ? DTOGroup.GROUP_EVERYONE : DTOGroup.GROUP_ANONYMOUS) + "'");
      String groupQuery = "SELECT group_id FROM ausers_group_user WHERE user_id = '" + uid + "'";
      PreparedStatement groupStmt = conn.prepareStatement(groupQuery);
      ResultSet groupRs = groupStmt.executeQuery();
      while (groupRs.next()) {
        groupIds.append((groupIds.length() > 0 ? ",'" : "'") + groupRs.getString("group_id") + "'");
      }
      groupRs.close();

      // Fetch permissions from entitypermissionuser for the user
      String userPermissionsQuery = "SELECT entity_id, value FROM ausers_entitypermissionuser WHERE user_id = '" + uid + "'";
      PreparedStatement userPermStmt = conn.prepareStatement(userPermissionsQuery);
      ResultSet userPermRs = userPermStmt.executeQuery();
      while (userPermRs.next()) {
        String entityId = userPermRs.getString("entity_id");
        int value = userPermRs.getInt("value");
        permissions.put(entityId, value);
      }
      userPermRs.close();

      // Fetch group-based permissions if group_ids are present
      if (groupIds.length() > 0) {
        String groupPermissionsQuery
                = "SELECT entity_id, value FROM ausers_entitypermissiongroup WHERE group_id IN (" + groupIds + ")";
        Statement groupPermStmt = conn.createStatement();
        ResultSet groupPermRs = groupPermStmt.executeQuery(groupPermissionsQuery);
        while (groupPermRs.next()) {
          String entityId = groupPermRs.getString("entity_id");
          int value = groupPermRs.getInt("value");
          permissions.merge(entityId, value, (existingValue, newValue) -> existingValue | newValue);
        }
        groupPermRs.close();
      }
    } catch (Exception e) {
      System.out.println(e);
    }

    return permissions;
  }

  private static boolean contains(long id, long p) {
    return (id & p) == p;
  }
 
  public static void main(String[] args) {
    boolean canRead = can("u_42", "e0_S", "can_edit_users");
    System.out.println(canRead);

    System.out.println(getParentHierarchyMap("e_115").toString());
  }
}
