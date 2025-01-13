package si.fri.algator.ausers;

import java.sql.CallableStatement;
import si.fri.algator.ausers.dto.DTOEntity;
import si.fri.algator.database.Database;
import si.fri.algator.global.ATLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import static si.fri.algator.ausers.AUsersHelper.isEmptyOrNull;
/**
 *
 * @author Matej Bertoncelj
 */
public class EntitiesDAO {

  private static final String QUERY_GET_ENTITY_BY_ID
          = "SELECT " + "   id, name, is_private, entity_type_id, owner_id, parent_id"
          + " FROM ausers_entities "
          + " WHERE id = ?";

  private static final String INSERT_ENTITY =
          "INSERT INTO ausers_entities (id, name, entity_type_id, is_private, owner_id, parent_id) "
          + "VALUES (?, ?, ?, ?, ?, ?)";
 
  private static final String DELETE_ENTITY =
          "DELETE FROM ausers_entities WHERE (id = ?)";
  
  private static final HashMap<String, String> parentSuffix = new HashMap<String, String>() {{
    put(DTOEntity.ETYPE_Project,"_P"); put(DTOEntity.ETYPE_Algorithm,"_A");
    put(DTOEntity.ETYPE_Testset,"_T"); put(DTOEntity.ETYPE_Presenter,"_R");
  }};
  
  public static DTOEntity getEntity(final String eid) {
    if (isEmptyOrNull(eid)) return null;

    Connection conn = Database.getConnectionToDatabase();
    if (conn == null) return null;

    try {
      DTOEntity entity = new DTOEntity();
      PreparedStatement stmt = conn.prepareStatement(QUERY_GET_ENTITY_BY_ID);
      stmt.setString(1, eid);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        entity.setId(rs.getString("id"));
        entity.setName(rs.getString("name"));
        entity.setPrivate(rs.getBoolean("is_private"));
        entity.setEntityType(rs.getString("entity_type_id"));
        entity.setOwner(rs.getString("owner_id"));
        entity.setParent(rs.getString("parent_id"));
        return entity;
      }
    } catch (Exception e) {
      ATLog.log("Problem at getting values from table 'ausers_entities'. Error: " + e.getMessage(), 0);
    }
    return null;
  }

  /**
   * Used to add project, algorithm, testset or presenter to Entites table.
   */
  public static String addEntity(String entityType, String name, String eid, String owner,  String parent, boolean isPrivate) {
    if (isEmptyOrNull(owner) || isEmptyOrNull(name)) 
      return "11:Add entity to DB error: null or empty owner or name.";

    Connection conn = Database.getConnectionToDatabase();
    if (conn == null) 
      return "12:Can not connect to database.";
   
    String parentEID = parent + parentSuffix.getOrDefault(entityType, "_S");
    if (getEntity(parentEID) != null) {    
      try {
        PreparedStatement stmt = conn.prepareStatement(INSERT_ENTITY);
        stmt.setString (1, eid);
        stmt.setString (2, name);
        stmt.setString (3, entityType);
        stmt.setBoolean(4, isPrivate);
        stmt.setString (5, owner);
        stmt.setString (6, parentEID); 
        stmt.execute();
        
        if (entityType.equals(DTOEntity.ETYPE_Project))
          return afterInsertProject(eid, owner);
        return "0:Entity added to DB.";
      } catch (Exception e) {
        return "13:Can not add entity to DB: " + e.toString();
      }
    } else {
      return "14:Can not add entity - parent does not exist.";
    }
  }
  
  public static String removeEntity(String eid) {
    try {
      Connection conn = Database.getConnectionToDatabase();
      if (conn == null) 
        return "11:Can not connect to database.";

      PreparedStatement stmt = conn.prepareStatement(DELETE_ENTITY);
      stmt.setString(1, eid);
      stmt.execute();
      return "0:Entity removed.";
    } catch (Exception e) {
      return "12: Error deleting entity: " + e.toString();
    }
  }

  
  public static String afterInsertProject(String eid, String owner) {
    Connection conn = Database.getConnectionToDatabase();
    if (conn == null) 
      return "1:Can not connect to database.";

    try {
      CallableStatement stmt = conn.prepareCall("{CALL after_insert_project(?, ?)}");
      stmt.setString (1, eid);
      stmt.setString (2, owner);
      stmt.executeQuery();
      return "0:AfterInsertProject OK.";
    } catch (Exception e) {
      return "2:Can not AfterInsertProject: " + e.toString();
    }
  }
}
