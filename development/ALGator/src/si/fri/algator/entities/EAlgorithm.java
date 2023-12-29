  package si.fri.algator.entities;

import java.io.File;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class EAlgorithm extends Entity {
  // Entity identifier
  public static final String ID_Algorithm   ="Algorithm";  
  
  //Fields
  public static final String ID_ShortName       = "ShortName";      // String
  public static final String ID_Description     = "Description";    // String
  public static final String ID_Author          = "Author";	    // String
  public static final String ID_Date            = "Date";	    // String
  public static final String ID_Language        = "Language";       // String 
  
  
  public EAlgorithm() {
    super(ID_Algorithm, 
	 new String [] {ID_ShortName, ID_Description, ID_Author, ID_Date, ID_Language});
    setRepresentatives(ID_ShortName, ID_Author);
  }
  
  public EAlgorithm(File fileName) {
    this();
    initFromFile(fileName);
  } 
  
  public ErrorStatus copyAndComplile(String workingDir) {
    ErrorStatus curES = ErrorStatus.STATUS_OK;
    
    return curES;
  }
  
  public String getAlgorithmClassname() {
    return /* getName() +*/ "Algorithm";
  }
  
  public AlgorithmLanguage getLanguage() {
    String langDesc = getField(ID_Language);
    if (langDesc == null || langDesc.isEmpty())
      langDesc = "JAVA"; // default language
    
    return AlgorithmLanguage.getType(langDesc);
  }
}
