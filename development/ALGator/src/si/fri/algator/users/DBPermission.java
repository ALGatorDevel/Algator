package si.fri.algator.users;

/**
 *
 * @author Gregor
 */
public class DBPermission {
    public int id;
    public String permission;
    public String permission_code;

    public DBPermission(int id, String permission, String permission_code) {
        this.id = id;
        this.permission = permission;
        this.permission_code = permission_code;
    }

}
