package si.fri.algator.ausers.dto;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import si.fri.algator.database.Database;

/**
 *
 * @author tomaz
 */
public class PermissionTypes {

  static HashMap<String, DTOPermissionType> pTypes;

  public static HashMap<String, DTOPermissionType> getAllPermissionTypes() {
    if (pTypes == null) {
      HashMap<String, DTOPermissionType> permissionTypeMap = new HashMap<>();
      Connection conn = Database.getConnectionToDatabase();

      String query = "SELECT id, name, codename, value FROM ausers_permissiontype";
      try ( Statement stmt = conn.createStatement();  ResultSet rs = stmt.executeQuery(query)) {
        while (rs.next()) {
          DTOPermissionType dto = new DTOPermissionType();

          dto.setId(rs.getString("id"));
          dto.setName(rs.getString("name"));
          dto.setCodename(rs.getString("codename"));
          dto.setValue(rs.getLong("value"));

          permissionTypeMap.put(dto.getCodename(), dto);
        }
      } catch (Exception e) {
      }
      pTypes = permissionTypeMap;
    }
    return pTypes;
  }
  
  public static long getPermissionValue(String codename) {
    HashMap<String, DTOPermissionType> pTypes = getAllPermissionTypes();
    DTOPermissionType pt = pTypes.getOrDefault(codename, null);
    return pt == null ? 0 : pt.getValue();
  }
  
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    while (true) {
      String cn = sc.next();
      System.out.println(getPermissionValue(cn));
    }
  }
}
