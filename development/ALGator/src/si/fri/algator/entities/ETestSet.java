package si.fri.algator.entities;

import java.io.File;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.tools.ATTools;

/**
 * A TestSet entity. 
 * 
 * @author tomaz
 */
public class ETestSet extends Entity {  
  // Entity identifier
  public static final String ID_TestSet       ="TestSet";
  
  //Fields
  public static final String ID_Author        ="Author";	  // String
  public static final String ID_Date          ="Date";	          // String
  public static final String ID_ShortName     ="ShortName";	  // String
  public static final String ID_Desc          ="Description";     // String
  public static final String ID_N             ="N";		  // Integer
  public static final String ID_TestRepeat    ="TestRepeat";	  // Integer
  public static final String ID_TimeLimit     ="TimeLimit";	  // Integer  
  
  // returns an ETestSet with given name in current data_root folder
  public static ETestSet getTestset(String projectName, String testsetName) {
    String data_root = ATGlobal.getALGatorDataRoot();
    ETestSet tst = new ETestSet(new File(ATGlobal.getTESTSETfilename(data_root, projectName, testsetName)));
    tst.set(ID_LAST_MODIFIED, tst.getLastModified(projectName, testsetName));
    return tst;
  }
   
  public ETestSet() {
    super(ID_TestSet, 
      new String [] {ID_Author, ID_Date, ID_ShortName, ID_Desc, ID_N, ID_TestRepeat, ID_TimeLimit}
    );
    setRepresentatives(ID_ShortName, ID_Author);
    export_name = false;
  }
  
  public ETestSet(File fileName) {
    this();
    initFromFile(fileName);
    
    setRepresentatives(ID_ShortName, ID_Desc);
    
    // extract alg_name of algorithm from folder name: .../ALG-<alg_name>/algorithm.json)
    String   name  = ATTools.extractFileNamePrefix(fileName);
    setName(name);    
  }
  
  
  /**
   * The value of the "DescriptionFile" field
   */
  public String getTestSetDescriptionFile() {
    return getName() + ".txt";
  }
  
  @Override
  public long getLastModified(String projectName, String entityName) {
    String jsnoFileName = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataRoot(), projectName, entityName);
    String dataFileName = ATGlobal.getTESTSETDATAfilename(ATGlobal.getALGatorDataRoot(), projectName, entityName);
    File   tstjsonFile  = new File(jsnoFileName);
    File   tstdataFile  = new File(dataFileName);
    return Math.max(tstjsonFile.lastModified()/1000, tstdataFile.lastModified()/1000);
  }
}


