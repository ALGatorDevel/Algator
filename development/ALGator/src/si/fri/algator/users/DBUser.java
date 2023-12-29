package si.fri.algator.users;

import java.sql.Date;

/**
 *
 * @author Tomaž
 */
public class DBUser {
    public int     id;
    public String  username;    
    public String  password;
    public String  first_name;
    public String  last_name;
    public String  email;
    public Date    date_joined;    
    public Date    last_login;         // read with getTimestamp(columnName)
    public boolean is_superuser;
    public boolean is_staff;
    public boolean is_active;

    
    public DBUser(int id, String username, String password, boolean is_active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.is_active = is_active;
    }
    
}
