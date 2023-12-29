package si.fri.algator.users;

import java.util.ArrayList;

/**
 *
 * @author tomaz
 */
public class DBPermissions {
    public ArrayList<DBEntityPermission> project_permissions;
    public ArrayList<DBEntityPermission> algorithm_permissions;
    public ArrayList<DBEntityPermission> test_permissions;


  public DBPermissions() {   
    project_permissions   = new ArrayList<>();
    algorithm_permissions = new ArrayList<>();
    test_permissions      = new ArrayList<>();
  }        
}
