package si.fri.algator.ausers.dto;

/**
 *
 * @author Matej Bertoncelj
 */
public class DTOGroup {
  public final static String GROUP_ROOT      = "g0_ro06ghe65";
  public final static String GROUP_ANONYMOUS = "g1_an15434hj";
  public final static String GROUP_EVERYONE  = "g2_ev26hedn7";

  public DTOGroup() {}
  
  public DTOGroup(String id, String name, String owner) {
    this.id = id;
    this.name = name;
    this.owner = owner;
  }
    
  public static DTOGroup getEveryoneGroup() {
    return new DTOGroup(GROUP_EVERYONE, "Everyone", DTOUser.USER_ALGATOR);
  }
  public static DTOGroup getAnonymousGroup() {
    return new DTOGroup(GROUP_ANONYMOUS, "Anonymous", DTOUser.USER_ALGATOR);
  }
  
    private String id;
    private String name;
    private String owner;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
