package si.fri.algator.ausers.dto;

/**
 *
 * @author Matej Bertoncelj
 */
public class DTOEntity {
  public final static String ETYPE_System     = "et0_S";
  public final static String ETYPE_Projects   = "et0_P";
  public final static String ETYPE_Algorithms = "et0_A";
  public final static String ETYPE_Testsets   = "et0_T";
  public final static String ETYPE_Presenters = "et0_R";
  public final static String ETYPE_Project    = "et1";
  public final static String ETYPE_Algorithm  = "et2";
  public final static String ETYPE_Testset    = "et3";
  public final static String ETYPE_Presenter  = "et4";
  public final static String ETYPE_Group      = "et5";

  private String id;
  private String name;
  private String entityType;
  private String owner;
  private String parent;
  private boolean isPrivate;

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

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
  }
}
