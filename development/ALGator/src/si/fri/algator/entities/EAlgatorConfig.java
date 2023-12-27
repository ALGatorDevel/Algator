package si.fri.algator.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.server.ASGlobal;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class EAlgatorConfig extends Entity  implements Serializable {
  // a single instance of a global configuration file
  private static EAlgatorConfig config;
  
  // Entity identifier
  public static final String ID_ResultParameter   = "Config";
  
  // Fields
  public static final String ID_ALGatorID      = "ALGatorID";          // String
  public static final String ID_Families       = "ComputerFamilies";   // EComputerFamily[]
  public static final String ID_Computers      = "Computers";          // EComputer[]
  public static final String ID_DBServer       = "DatabaseServer";     // HashMap
  
  public static final String ID_ALGatorServerName = "ALGatorServerName";     // String
  public static final String ID_ALGatorServerPort = "ALGatorServerPort";     // int

  
  public EAlgatorConfig() {  
   super(ID_ResultParameter, 
	 new String [] {ID_Families, ID_Computers, ID_DBServer, ID_ALGatorServerName, ID_ALGatorServerPort});
  
   setRepresentatives(ID_ALGatorID, ID_Families);
  }
  
  public EAlgatorConfig(File fileName) {
    this();
    initFromFile(fileName);
  }
  
  public ArrayList<EComputerFamily> getFamilies() {
    ArrayList<EComputerFamily> families = new ArrayList<>();      
    try {
      JSONArray ja = getField(ID_Families);
    
    
      for (int i = 0; i < ja.length(); i++) {
        JSONObject jo = ja.getJSONObject(i);
        families.add(new EComputerFamily(jo.toString()));
      }
    } catch (Exception e) {/* ignore, if an error occures while parsing the ID_Families parameter*/ }
    if (families.isEmpty()) 
      families.add(new EComputerFamily());    
    return families;
  }
        
  public ArrayList<EComputer> getComputers() {
    ArrayList<EComputer> computers = new ArrayList<>(); 
    try {
      JSONArray ja = getField(ID_Computers);
      for (int i = 0; i < ja.length(); i++) {
        JSONObject jo = ja.getJSONObject(i);
        computers.add(new EComputer(jo.toString()));
      }
    } catch (Exception e) {/* ignore, if an error occures while parsing the ID_Computers parameter*/ }
    if (computers.isEmpty()) 
      computers.add(new EComputer());        
    return computers;
  }
        
  
  public HashMap<String, Object> getDatabaseServerInfo() {
    return ATTools.jSONObjectToMap(getField(ID_DBServer), "Connection", "Database", "Username", "Password");
  }  

  public static String getALGatorServerName() {
    EAlgatorConfig config = getConfig();
    if (config != null && config.getString(ID_ALGatorServerName) != null && !config.getString(ID_ALGatorServerName).isEmpty())
      return config.getField(ID_ALGatorServerName);
    return ASGlobal.DEFAULT_TASK_SERVER_NAME;
  }
  public static int getALGatorServerPort() {
    EAlgatorConfig config = getConfig();
    if (config != null && config.getFieldAsInt(ID_ALGatorServerPort) != 0)
      return config.getFieldAsInt(ID_ALGatorServerPort);
    return ASGlobal.DEFAULT_TASK_SERVER_PORT;
  }
  
  public static EAlgatorConfig getConfig() {
    return new EAlgatorConfig(new File(ATGlobal.getGlobalConfigFilename()));
    
  }  
}
