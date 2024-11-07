package si.fri.algator.ausers.dto;

import java.util.Date;

/**
 *
 * @author tomaz
 */
public class DTOUser {
  public final static String USER_ROOT       = "u0_ro0jpj4wp";
  public final static String USER_ANONYMOUS  = "u1_an1s7eko9";
  public final static String USER_ALGATOR    = "u2_al2f5a19g";

  
  private String  uid;
  private String  username;
  private String  first_name;
  private String  last_name;
  private String  email;
  private Date    date_joined;
  private Date    last_login;
  private boolean is_superuser;  
  private boolean is_staff;
  private boolean is_active;

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirst_name() {
    return first_name;
  }

  public void setFirst_name(String first_name) {
    this.first_name = first_name;
  }

  public String getLast_name() {
    return last_name;
  }

  public void setLast_name(String last_name) {
    this.last_name = last_name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Date getDate_joined() {
    return date_joined;
  }

  public void setDate_joined(Date date_joined) {
    this.date_joined = date_joined;
  }

  public Date getLast_login() {
    return last_login;
  }

  public void setLast_login(Date last_login) {
    this.last_login = last_login;
  }

  public boolean isIs_superuser() {
    return is_superuser;
  }

  public void setIs_superuser(boolean is_superuser) {
    this.is_superuser = is_superuser;
  }

  public boolean isIs_staff() {
    return is_staff;
  }

  public void setIs_staff(boolean is_staff) {
    this.is_staff = is_staff;
  }

  public boolean isIs_active() {
    return is_active;
  }

  public void setIs_active(boolean is_active) {
    this.is_active = is_active;
  }

  @Override
  public String toString() {
    return "UserDTO{" + "uid=" + uid + ", username=" + username + ", first_name=" + first_name + ", last_name=" + last_name + ", email=" + email + ", date_joined=" + date_joined + ", last_login=" + last_login + ", is_superuser=" + is_superuser + ", is_staff=" + is_staff + ", is_active=" + is_active + '}';
  }  
}
