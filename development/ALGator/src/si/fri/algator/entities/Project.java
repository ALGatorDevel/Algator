package si.fri.algator.entities;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
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
  
  
  private final TreeMap<String, EAlgorithm> algorithms;
  private final TreeMap<String, ETestSet>   testsets;
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
    
    algorithms           = new TreeMap();
    testsets             = new TreeMap();
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
      String algFilename = ATGlobal.getALGORITHMfilename(eProject.getProjectRootDir(), algName);
      EAlgorithm eAlgorithm = new EAlgorithm(new File(algFilename));
      if (ErrorStatus.getLastErrorStatus().isOK()) {
	algorithms.put(algName, eAlgorithm);
      } else
	errors.add(ErrorStatus.getLastErrorStatus()); 
    }
    
    // read the testsets 
    String [] tsNames = eProject.getStringArray(EProject.ID_TestSets);
    for(String tsName : tsNames) {   
      String tsFilename = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataLocal(), eProject.getName(), tsName);
      ETestSet eTestset = new ETestSet(new File(tsFilename));
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
  
  
  public TreeMap<String, EAlgorithm> getAlgorithms() {
    return algorithms;
  }
  public TreeMap<String, ETestSet> getTestSets() {
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
   * Returns an array of indicators (merged from all result descriptions)
   */
  static public String[] getIndicators(HashMap<MeasurementType, EResult> resultDescriptions) {
    ArrayList<String> params = new ArrayList();
    
    for (EResult eRedDesc : resultDescriptions.values()) {
      String [] tParams = eRedDesc.getStringArray(EResult.ID_IndOrder);
      for (String param : tParams) 
        if (!params.contains(param))
          params.add(param);
    }
    
    return (String []) params.toArray(new String[0]);
  }
  
  /**
   * Returns an array of indicators for a given measurement type
   */
  static public String[] getIndicators(HashMap<MeasurementType, EResult> resultDescriptions, MeasurementType mType) {
    if (resultDescriptions == null) return new String []{};

    EResult eRedDesc = resultDescriptions.get(mType);
    if  (eRedDesc != null) 
      return eRedDesc.getStringArray(EResult.ID_IndOrder);
    else  
      return new String []{};
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
  
  public static JSONObject getProjectsAsJSON() {
      JSONArray ja = new JSONArray();
      for (String project : getProjects()) {
        ja.put(project.replaceAll("^PROJ-", ""));
      }     
      return new JSONObject().put("Projects", ja);    
  } 
}
