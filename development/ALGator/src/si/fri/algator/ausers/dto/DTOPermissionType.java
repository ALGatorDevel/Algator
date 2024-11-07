package si.fri.algator.ausers.dto;

/**
 *
 * @author Matej Bertoncelj
 */
public class DTOPermissionType {
    private String id;
    private String name;
    private String codename;
    private long   value;

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

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codeName) {
        this.codename = codeName;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
