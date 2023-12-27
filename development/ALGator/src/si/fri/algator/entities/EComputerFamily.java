package si.fri.algator.entities;

import java.io.Serializable;

/**
 *
 * @author tomaz
 */
public class EComputerFamily extends Entity  implements Serializable {
  // Entity identifier
  public static final String ID_ResultParameter   = "Family";
  
  // Fields
  public static final String ID_Name         = "Name";         // String  
  public static final String ID_FamilyID     = "FamilyID";     // String
  public static final String ID_Desc         = "Description";  // String
  public static final String ID_Platform     = "Platform";     // String
  public static final String ID_Hardware     = "Hardware";     // String
  
  public EComputerFamily() {
   super(ID_ResultParameter, 
	 new String [] { ID_Name,     ID_FamilyID, ID_Desc, ID_Platform, ID_Hardware},
         new Object [] { "Family-F0", "F0",        "",      "",          ""         });
  
   setRepresentatives(ID_Name, ID_FamilyID, ID_Desc);
  }
    
  
  public EComputerFamily(String json) {
    this();
    initFromJSON(json);
  }
}
