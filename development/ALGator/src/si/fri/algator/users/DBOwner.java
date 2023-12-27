package si.fri.algator.users;

/**
 *
 * @author Gregor
 */
public class DBOwner {
    public int id;
    public int id_owner;
    public int id_entity;

    public DBOwner(int id, int id_owner, int id_entity) {
        this.id = id;
        this.id_owner = id_owner;
        this.id_entity = id_entity;
    }
}
