package si.fri.algator.ausers.dto;

/**
 *
 * @author Matej Bertoncelj
 */
public class DTOEntityPermissionUser {
    private String id;
    private String entity;
    private String user;
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

    public String getUser() {
        return user;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
