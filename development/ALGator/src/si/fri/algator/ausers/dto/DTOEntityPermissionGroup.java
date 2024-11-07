package si.fri.algator.ausers.dto;

/**
 *
 * @author Matej Bertoncelj
 */
public class DTOEntityPermissionGroup {
    private String id;
    private String entity;
    private String group;
    private long value;

    public String getId() {
        return id;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getGroup() {
        return group;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
