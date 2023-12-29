package si.fri.algator.users;

/**
 *
 * @author Gregor
 */
public class DBEntity {
    public int id;
    public String name;
    public int type;
    public int id_parent;

    public DBEntity(int id, String name, int type, int id_parent) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.id_parent = id_parent;
    }

  @Override
  public String toString() {
    return this.name;
  }
    
    
}
