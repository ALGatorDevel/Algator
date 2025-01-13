package si.fri.algator.ausers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
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
  public static final String accessDeniedString = "Access denied.";

  // Method gets array of entities and filters them according to "accessability" (can_read)
  public static String[] filterPermitted(String uid, String[] entities, Function<String, String> getEID) {
    ArrayList<String> filteredValues = new ArrayList();
    for (String entity : entities) {
      if (can(uid, getEID.apply(entity), "can_read")) filteredValues.add(entity);
    }
    return filteredValues.toArray(new String[0]);
  }
  
  // ena poizvedba can() vzame približno 6ms (izmerjeno z testSpeed() spodaj)
  public static boolean can(final String uid, String eid, final String codename) {
    long pValue = PermissionTypes.getValue(codename);    
    return can(uid, eid, pValue);
  }
    
  // ena poizvedba can() vzame približno 6ms (izmerjeno z testSpeed() spodaj)  
  public static boolean can(final String uid, String eid, final long pValue) { 
    // nonexisting right?
    if (pValue == 0) return false;

    // emtpy uid or eid?
    if (isEmptyOrNull(uid) || isEmptyOrNull(eid)) return false;
    
    // in Anonymous mode user can do everything
    if (Database.isAnonymousMode()) return true;
    
    // nonexisting user?
    EntitiesDAO dao = new EntitiesDAO();
    DTOUser user = AUsersTools.getUser(uid);
    if (user == null) user = AUsersTools.getUser(DTOUser.USER_ANONYMOUS);

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
      // add permissions ...
      permValue |= permissions.getOrDefault(eid, 0);
      // ... and step to parent (up to system root). 
      eid = hiearchy.getOrDefault(eid, null); 
      
      // check for ownership recursively: if uid is owner of 
     // any parent in the hiearchy, they have right to control
      if (eid != null) {
        DTOEntity eP = dao.getEntity(eid);
        if (eP != null && eP.getOwner().equals(uid)) return true;
      }
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

  
  public static void testSpeed() {
    String ent[] =   {"e_0RLOysxtJxjN","e_100","e_100_A","e_100_R","e_100_T","e_101","e_102","e_103","e_104","e_105","e_106","e_107","e_110","e_110_A","e_110_R","e_110_T","e_111","e_112","e_113","e_114","e_115","e_116","e_S5iYNZk5XmVh","e0_P","e0_S"};
    String usr[] =   {"u0_ro0jpj4wp","u1_an1s7eko9","u2_al2f5a19g","u_42","u_24"};
    String rig[] =   {"can_read","can_write","can_execute","can_add_project","can_add_algorithm","can_add_testset","can_add_presenter","can_edit_rights","can_edit_users","full_control"};

    for (int i = 0; i < usr.length; i++) 
      for (int j = 0; j < ent.length; j++) 
        for (int k = 0; k < rig.length; k++) 
          System.out.printf("Can(%s, %s, %s)=%s\n", usr[i], ent[j], rig[k], can(usr[i], ent[j], rig[k])?"yes":"no");
  }
  public static void main(String[] args) {
    //testSpeed(); 
    
    //boolean canRead = can("u_42", "e0_S", "can_edit_users");
    //System.out.println(canRead);

    System.out.println(getParentHierarchyMap("e_115").toString());
  }
}
