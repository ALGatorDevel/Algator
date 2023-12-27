package si.fri.algator.entities;

import java.io.File;
import org.json.JSONObject;
import si.fri.algator.server.ASGlobal;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class ELocalConfig extends Entity { 
  // a single instance of a local configuration file
  private static ELocalConfig config;
  
  // Entity identifier
  public static final String ID_Query             = "Config";  
  
  //Fields
  private static final String ID_FAMILYID          = "FamilyID";	 // String
  private static final String ID_COMPID            = "ComputerID";	 // String (unique (in a given family) identifier of a computer)
  private static final String ID_UID               = "ComputerUID";    	 // String (system-wide unique identifier of a computer, used to identify computer in requests)
  public  static final String ID_VMEP              = "VMEP";	         // String
  public  static final String ID_VMEPClasspath     = "VMEPClasspath";    // String
  
  public  static final String ID_ALGatorServerName = "ALGatorServerName";   // String
  public  static final String ID_ALGatorServerPort = "ALGatorServerPort";   // String
  
  public  static final String ID_RSyncServerPort   = "RSyncServerPort";  // String (port on which task server listens for rsync actions)
  public  static final String ID_DoSyncProjects    = "DoSyncProjects";   // boolean (set to false when TaskClient runs on the same machine as task client (this is true in testing environment))
  
  public  static final String ID_ALGatorUser       = "AlgatorUser";  // JSonObject
  private static final String ID_Username          = "Username";     // String
  private static final String ID_Password          = "Password";     // String
  
  public ELocalConfig() {
   super(ID_Query, 
	 new String [] {ID_ALGatorUser, ID_ALGatorServerName, ID_ALGatorServerPort, ID_FAMILYID, ID_COMPID, ID_UID, ID_VMEP, ID_VMEPClasspath, ID_RSyncServerPort, ID_DoSyncProjects},
         new Object [] {new JSONObject("{'"+ID_Username+"':'','"+ID_Password+"':''}"), "", "", "", "", "", "", "","",true});   
   setRepresentatives(ID_FAMILYID, ID_COMPID, ID_UID);
  }
  
  public ELocalConfig(File fileName) {
    this();
    initFromFile(fileName);
  }
 
  public static ELocalConfig getConfig() {
    if (config == null) {
      config = new ELocalConfig(new File(ATGlobal.getLocalConfigFilename()));
    }
    return config;
  }  
  
  public String getComputerID() {
    String family   = getField(ID_FAMILYID);
    String computer = getField(ID_COMPID);
    if (family != null && !family.isEmpty() && computer != null && !computer.isEmpty())
      return  family+ "." + computer;
    else return "";
  }
  public void setFamilyID(String fid) {
    set(ID_FAMILYID, fid);
  }
  public void setComputerID(String cid) {
    set(ID_COMPID, cid);
  }
  
  public String getComputerUID() {
    String uid   = getField(ID_UID);
    return uid == null ? "?" : uid;
  }
  public void setComputerUID(String cuid) {
    set(ID_UID, cuid);
  }
  
  
  public String getALGatorServerName() {
    String algatorServerName = getField(ID_ALGatorServerName);
    if (algatorServerName == null || algatorServerName.isEmpty())
      algatorServerName = ASGlobal.DEFAULT_TASK_SERVER_NAME;
    return algatorServerName;
  }
  public void setALGatorServerName(String name) {
    set(ID_ALGatorServerName, name);
  }
  
  public int getALGatorServerPort() {
    return getFieldAsInt(ID_ALGatorServerPort, ASGlobal.DEFAULT_TASK_SERVER_PORT);
  }
  public void setALGatorServerPort(int port) {
    set(ID_ALGatorServerPort, port);
  }

  public int getRSyncServerPort() {
    return getFieldAsInt(ID_RSyncServerPort, ASGlobal.DEFAULT_RSYNC_SERVER_PORT);
  }
  public void setRSyncServerPort(int port) {
    set(ID_RSyncServerPort, port);
  }

  public boolean isSyncProjects() {
    return getFieldAsBoolean(ID_DoSyncProjects, true);
  }

  
  public String getUsername() {
    Object uname = ATTools.jSONObjectToMap(getField(ID_ALGatorUser), ID_Username, ID_Password).get(ID_Username);
    return uname instanceof String ? (String)uname : "";
  }
  
  public String getPassword() {
    String pswd = (String) ATTools.jSONObjectToMap(getField(ID_ALGatorUser), ID_Username, ID_Password).get(ID_Password);
    return pswd instanceof String ? (String)pswd : "";
  }
  
  public void setUserName(String username) {
    JSONObject jObj = getField(ID_ALGatorUser);
    if (jObj != null)
      jObj.put(ID_Username, username);
  }
  
  public void setPassword(String password) {
    JSONObject jObj = getField(ID_ALGatorUser);
    if (jObj != null)
      jObj.put(ID_Password, password);
  }

  
  public static void main(String[] args) {
    System.out.println(ELocalConfig.getConfig().isSyncProjects());
    //elc.setUserName("polde");
    //elc.setPassword("pilde");
    
    //elc.saveEntity();
  }
}
