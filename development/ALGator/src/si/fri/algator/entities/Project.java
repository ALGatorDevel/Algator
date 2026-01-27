package si.fri.algator.entities;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.ausers.CanUtil;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;

/**
 * A class that combines all the  information about the project (EProject, all
 * EAlgorithms and ETestSets).
 * @author tomaz
 */
public class Project {  
  // the data_root folder name ...
  private final String dataRoot;
  // .. and the project name
  private final String projectName;
  
  
  private final LinkedHashMap<String, EAlgorithm> algorithms;
  private final LinkedHashMap<String, ETestSet>   testsets;
  private final HashMap<MeasurementType, EResult>   resultDescriptions;
  private ETestCase testCaseDescription;
	  
  private final EProject eProject;
  
  /**
   * If an error occures when reading EProject file, the first entry of this list is
   * ERROR_INVALID_PROJECT. Otherwise, the first entry is STATUS_OK. Other entries
   * represent error that occure while loading algorithm, datasets and resultdescriptions.
   */
  ArrayList<ErrorStatus> errors;
 
  
  public Project(String dataRoot, String projectName) {
    
    this.dataRoot    = dataRoot;
    this.projectName = projectName;
    
    errors = new ArrayList();
    
    algorithms           = new LinkedHashMap();
    testsets             = new LinkedHashMap();
    resultDescriptions   = new HashMap();
    testCaseDescription  = null;
    
    // read the eProject
    String projFilename = ATGlobal.getPROJECTfilename(dataRoot, projectName);
    eProject = new EProject(new File(projFilename));
    if (!ErrorStatus.getLastErrorStatus().isOK()) {
      errors.add(ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_PROJECT, ""));
      return;
    }
 
    
    errors.add(ErrorStatus.STATUS_OK);
    
    // read the algorithms
    String [] algNames = eProject.getStringArray(EProject.ID_Algorithms);
    for(String algName : algNames) {   
      EAlgorithm eAlgorithm = EAlgorithm.getAlgorithm(projectName, algName);
      if (ErrorStatus.getLastErrorStatus().isOK()) {
	algorithms.put(algName, eAlgorithm);
      } else
	errors.add(ErrorStatus.getLastErrorStatus()); 
    }
    
    // read the testsets 
    String [] tsNames = eProject.getStringArray(EProject.ID_TestSets);
    for(String tsName : tsNames) {   
      ETestSet eTestset = ETestSet.getTestset(projectName, tsName);
      if (ErrorStatus.getLastErrorStatus().isOK()) {
	testsets.put(tsName, eTestset);
      } else
	errors.add(ErrorStatus.getLastErrorStatus()); 
    }
    
    // read the resultDescriptions
    readResultDescriptions(eProject.getProjectRootDir(), projectName, resultDescriptions, errors);
    
    String tcdFilename = ATGlobal.getTESTCASEDESCfilename(dataRoot, projectName);            
    testCaseDescription = new ETestCase(new File(tcdFilename));
  }

  public EProject getEProject() {
    return eProject;
  }
  

  public String getProjectRoot() {
    return ATGlobal.getPROJECTroot(this.dataRoot, getName());
  }

  public String getDataRoot() {
    return this.dataRoot;
  }

  public String getName() {
    return projectName;
    // return (eProject==null) ? "?" : eProject.getName();
  }
  
  
  public ArrayList<ErrorStatus> getErrors() {
    return errors;
  }
  
  
  public LinkedHashMap<String, EAlgorithm> getAlgorithms() {
    return algorithms;
  }
  public LinkedHashMap<String, ETestSet> getTestSets() {
    return testsets;
  }

  public HashMap<MeasurementType, EResult> getResultDescriptions() {
    return resultDescriptions;
  }

  public ETestCase getTestCaseDescription() {
    if (testCaseDescription==null)
      return new ETestCase();
    else
      return testCaseDescription;
  }  


  
  //*********************  static methods ************************

  
  /**
   * Returns an array of test parameters presented in testcase entity
   * @return 
   */
  static public String[] getTestParameters(ETestCase eTestCase) {
    if (eTestCase == null ) 
      return new String[0];
    else
      return eTestCase.getInputParameters();
  }
  
  /**
   * Returns an array of indicators for a given measurement type
   */
  static public String[] getIndicators(HashMap<MeasurementType, EResult> resultDescriptions, MeasurementType mType) {
    if (resultDescriptions == null) return new String []{};

    return getIndicators(resultDescriptions.get(mType), mType);
  }
  
  static public String[] getIndicators(EResult resultDescription, MeasurementType mType) {
    String[] indicators = (resultDescription != null) ?
      resultDescription.getStringArray(EResult.ID_IndOrder) : new String []{};
        
    // for EM measurements, we add default indicators: computerID, timestamp and pass
    if (MeasurementType.EM.equals(mType)) 
      indicators = extendWithDefaultIndicators(indicators, true);    
        
    return indicators;
  }
  
  
  // add three default indicators to array
  static public String[] extendWithDefaultIndicators(String[] indicators, boolean insertAtBeginning) {    
    if (indicators == null) indicators = new String[]{};
    String[] extended;

    // length of existing array; also: index of first element to be inserted    
    int originalLength = indicators.length;  
    
    if (insertAtBeginning) {
      extended = new String[originalLength+3];
      System.arraycopy(indicators, 0, extended, 3, originalLength); 
      originalLength = 0;
    } else
      extended = Arrays.copyOf(indicators, originalLength+3);
    
    extended[originalLength+0] = EResult.computerIDIndName;
    extended[originalLength+1] = EResult.timeStampIndName;
    extended[originalLength+2] = EResult.passIndName;
    return extended;
  }
  
  public static void readResultDescriptions(String projectRootDir, String projectName, 
                                            HashMap resultDescriptions, ArrayList errors)     {
    for(MeasurementType mType : MeasurementType.values()) {
      String rdFilename = ATGlobal.getRESULTDESCfilename(projectRootDir, projectName, mType);
      if (!mType.equals(MeasurementType.EM)) // only for EM type an error message is shown; 
        ATLog.disableLog();
      EResult eResrulDescription = new EResult(new File(rdFilename));
      ATLog.enableLog();
      if (ErrorStatus.getLastErrorStatus().isOK()) {
        resultDescriptions.put(mType, eResrulDescription);
      } else {
        errors.add(ErrorStatus.getLastErrorStatus()); 
      }
    }
  }
  
  /**
   * Method returns list of all projects in data_root/projects folder
   */
  public static String[] getProjects() {
    String projRoot = ATGlobal.getPROJECTSfolder(ATGlobal.getALGatorDataRoot());
    return new File(projRoot).list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(ATGlobal.ATDIR_projRootDir.replace("%s","")) && new File(dir,name).isDirectory();
      }
    });
  }
  
  // return names of all projects visible to uid
  public static JSONObject getProjectsAsJSON(String uid) {
    JSONArray ja = new JSONArray();
    for (String project : getProjects()) {
      String projName = project.replaceAll("^PROJ-", "");
      String peid = EProject.getProject(projName).getEID();
      if (CanUtil.can(uid, peid, "can_read"))
        ja.put(projName);
    }
    return  new JSONObject().put("Projects", ja);          
  } 
}
