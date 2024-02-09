package si.fri.algator.entities;

import java.io.Serializable;
import org.json.JSONArray;

/**
 *
 * @author tomaz
 */
public class EGenerator extends Entity  implements Serializable {
  // Entity identifier
  public static final String ID_Generator   = "Generator";
  
  // Fields
  public static final String ID_Type    = "Type";                    // String
  public static final String ID_Desc    = "Description";             // String
  public static final String ID_GPars   = "GeneratingParameters";    // String[]

  
  String [] generatingParameters = null;
  
  public EGenerator() {  
   super(ID_Generator, 
	 new String [] {ID_Type,  ID_Desc, ID_GPars});   
  
   setRepresentatives(ID_Type, ID_Desc);
   export_name = false;
  }
  
  public EGenerator(String json) {
    this();
    initFromJSON(json);
  }

  @Override
  public Object get(String fieldKey) {
    if (ID_GPars.equals(fieldKey))
      return getGeneratingParameters();
    else
      return super.get(fieldKey);
  }
  
 public String[] getGeneratingParameters() {  
    if (generatingParameters != null) return generatingParameters;
    
    try {            
      JSONArray ja = getField(ID_GPars);
      generatingParameters = new String[ja.length()];
      if (ja != null) for (int i = 0; i < ja.length(); i++) {
	generatingParameters[i] = ja.getString(i);
      }                  
    } catch (Exception e) {      
      generatingParameters = new String[0];
    }
    
    return this.generatingParameters;
  }  
}
