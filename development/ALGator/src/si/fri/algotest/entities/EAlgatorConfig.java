package si.fri.algotest.entities;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.tools.ATTools;

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
  public static final String ID_Families       = "ComputerFamilies";   // Family[]
  public static final String ID_DBServer       = "DatabaseServer";     // HashMap
  public static final String ID_TaskServerName = "TaskServerName";     // String
  public static final String ID_TaskServerPort = "TaskServerPort";     // String
  

  private ArrayList<EComputerFamily> families;
  private HashMap<String, Object> databaseServerInfo;
  
  public EAlgatorConfig() {  
   super(ID_ResultParameter, 
	 new String [] {ID_Families, ID_DBServer, ID_TaskServerName, ID_TaskServerPort});
  
   setRepresentatives(ID_ALGatorID, ID_Families);
  }
  
  public EAlgatorConfig(File fileName) {
    this();
    initFromFile(fileName);
  }
  
  public ArrayList<EComputerFamily> getFamilies() {
    if (families == null) {
      families = new ArrayList<>();      
      try {
        JSONArray ja = getField(ID_Families);
      
      
        for (int i = 0; i < ja.length(); i++) {
	  JSONObject jo = ja.getJSONObject(i);
	  families.add(new EComputerFamily(jo.toString()));
        }
      } catch (Exception e) {
        // ignore, if an error occures while parsing the ID_Families parameter
      }
      if (families.isEmpty()) {
        EComputerFamily cef = new EComputerFamily("{\"FamilyID\":\"F0\",\"Computers\":[{\"ComputerID\":\"C0\",\"Capabilities\":[\"AEE_EM\",\"AEE_CNT\",\"AEE_JVM\"]}]}");
        families.add(cef);
      }
    }    
    return families;
  }
  
  public HashMap<String, Object> getDatabaseServerInfo() {
    if (databaseServerInfo == null) {
      databaseServerInfo = new HashMap<>();
      databaseServerInfo.put("Connection", "");
      databaseServerInfo.put("Username", "");
      databaseServerInfo.put("Password", "");
    }
    try {
      JSONObject dbServerConfig = getField(ID_DBServer);
      databaseServerInfo = ATTools.jSONObjectToMap(dbServerConfig);
    } catch (Exception e) {}
    return databaseServerInfo;
  }  

  
  public static EAlgatorConfig getConfig() {
    if (config == null) {
      config = new EAlgatorConfig(new File(ATGlobal.getGlobalConfigFilename()));
    }
    return config;
  }  
}
