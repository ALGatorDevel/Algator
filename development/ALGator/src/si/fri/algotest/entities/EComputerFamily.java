package si.fri.algotest.entities;

import java.io.Serializable;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author tomaz
 */
public class EComputerFamily extends Entity  implements Serializable {
  // Entity identifier
  public static final String ID_ResultParameter   = "Family";
  
  // Fields
  public static final String ID_FamilyID     = "FamilyID";     // String
  public static final String ID_Name         = "Name";         // String
  public static final String ID_Desc         = "Description";  // String
  public static final String ID_Platform     = "Platform";     // String
  public static final String ID_Hardware     = "Hardware";     // String
  public static final String ID_SystemType   = "SystemType";   // String  (32 or 64)
  public static final String ID_Computers    = "Computers";    // EComputer[]
  

  
  private ArrayList<EComputer> computers = null;
  
  public EComputerFamily() {
   super(ID_ResultParameter, 
	 new String [] {ID_FamilyID, ID_Name, ID_Desc, ID_Platform, ID_Hardware, ID_SystemType, ID_Computers});
  
   setRepresentatives(ID_FamilyID, ID_Name, ID_Desc);
  }
  
  public ArrayList<EComputer> getComputers() {
    if (computers == null) {      
      computers = new ArrayList<>();    
      JSONArray ja = getField(ID_Computers);
      for (int i = 0; i < ja.length(); i++) {
	JSONObject jo = ja.getJSONObject(i);
	computers.add(new EComputer(jo.toString()));
      }
    }
    return computers;
  }
  
  
  public EComputerFamily(String json) {
    this();
    initFromJSON(json);
  }
}
