package si.fri.algator.entities;

import java.io.File;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.tools.ATTools;

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
  
  
    // returns an EAlgorithm with given name in current data_root folder
  public static EAlgorithm getAlgorithm(String projectName, String algorithmName) {
    String project_root = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    EAlgorithm alg = new EAlgorithm(new File(ATGlobal.getALGORITHMfilename(project_root, algorithmName)));
    alg.set(ID_LAST_MODIFIED, alg.getLastModified(projectName, algorithmName));
    return alg;
  } 

  public EAlgorithm() {
    super(ID_Algorithm, 
	 new String [] {ID_ShortName, ID_Description, ID_Author, ID_Date, ID_Language});
    setRepresentatives(ID_ShortName, ID_Author);
    export_name = false;
  }
  
  public EAlgorithm(File fileName) {
    this();
    initFromFile(fileName);
    
    // extract alg_name of algorithm from folder name: .../ALG-<alg_name>/algorithm.json)
    String   name  = ATTools.getLastFolderName(fileName.getPath());
    String[] parts = name.split("-"); 
    setName(parts.length==2 ? parts[1] : name);
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

  @Override
  public long getLastModified(String projectName, String entityName) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String fileName    = ATGlobal.getALGORITHMfilename(projectRoot, entityName);
    String srcFileName = ATGlobal.getALGORITHMsrc(projectRoot, entityName);
    File algFile   = new File(fileName);
    File algSrcFile = new File(srcFileName);
    return Math.max(algFile.lastModified()/1000, algSrcFile.lastModified()/1000);
  }
  
}
