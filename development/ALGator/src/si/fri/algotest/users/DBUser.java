package si.fri.algotest.users;

/**
 *
 * @author Gregor, Tomaž
 */
public class DBUser {
    public int id;
    public String name;
    public String password;
    public int status;

    public DBUser(int id, String name, String password, int status) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.status = status;
    }
    
}
