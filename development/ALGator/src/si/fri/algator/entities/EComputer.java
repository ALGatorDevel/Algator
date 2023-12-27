package si.fri.algator.entities;

import java.io.Serializable;
import java.util.TreeSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;

/**
 *
 * @author tomaz
 */
public class EComputer extends Entity  implements Serializable {
  // Entity identifier
  public static final String ID_ResultParameter   = "Computer";
  
  // Fields
  public static final String ID_Name         = "Name";         // String  
  public static final String ID_ComputerUID  = "ComputerUID";  // String (universal unique identifier of this computer)
  public static final String ID_ComputerID   = "ComputerID";   // String (family-wide identifier of computer) 
  public static final String ID_FamilyID     = "FamilyID";     // String
  public static final String ID_Desc         = "Description";  // String
  public static final String ID_IP           = "IP";           // String
  public static final String ID_Capabilities = "Capabilities"; // CompCap[]

  
  // The capabilities of this computer
  private TreeSet<CompCap> computerCapabilities;
  
  public EComputer() {  
   super(ID_ResultParameter, 
	 new String [] {ID_Name,       ID_ComputerUID, ID_FamilyID,   ID_ComputerID,  ID_Desc, ID_IP, ID_Capabilities}, 
         new Object [] {"Computer-C0", "0",             "F0",          "C0",           "",      "",    new JSONArray("['" + CompCap.EM + "','" + CompCap.CNT + "','"+ CompCap.QUICK +"']")});
  
   setRepresentatives(ID_ComputerUID, ID_Name, ID_ComputerID, ID_FamilyID, ID_Desc, ID_IP, ID_Capabilities);
  }
  
  public EComputer(String json) {
    this();
    initFromJSON(json);
  }

  public void assignUniqueID() {
    set(ID_ComputerUID, RandomStringUtils.random(10,true, true));
  }
  
  public TreeSet<CompCap> getCapabilities() {    
    if (computerCapabilities == null) {
      TreeSet result = new TreeSet();
      String [] cap = getStringArray(ID_Capabilities);
      for (int i = 0; i < cap.length; i++) 
        result.add(CompCap.capability(cap[i])); 
    
      computerCapabilities = result;
    }
    return computerCapabilities;
  }
  
}
