package si.fri.algator.entities;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class EProject extends Entity {
  // Entity identifier
  public static final String ID_Project   ="Project";
  
  //Fields
  public static final String ID_ShortTitle            = "ShortTitle";	        // String
  public static final String ID_Description           = "Description";	        // String

  public static final String ID_Author                = "Author";	        // String
  public static final String ID_Date                  = "Date";		        // String
  public static final String ID_Algorithms            = "Algorithms";	        // String []
  public static final String ID_TestSets              = "TestSets";	        // String []

  public static final String ID_ProjectJARs           = "ProjectJARs";          // Filename[]
  public static final String ID_AlgorithmJARs         = "AlgorithmJARs";        // Filename[]
  
  public static final String ID_Tags                  = "Tags";                 // String []
  
  public static final String ID_EMExecFamily          = "EMExecFamily";         // String
  public static final String ID_CNTExecFamily         = "CNTExecFamily";        // String
  public static final String ID_JVMExecFamily         = "JVMExecFamily";        // String
  
  public static final String ID_ProjPresenters        = "ProjPresenters";   // String []
  public static final String ID_AlgPresenters         = "AlgPresenters";    // String []
  
  // returns an EProject with given name in current data_root folder
  public static EProject getProject(String projectName) {
    String data_root = ATGlobal.getALGatorDataRoot();
    EProject prj = new EProject(new File(ATGlobal.getPROJECTfilename(data_root, projectName)));
    prj.set(ID_LAST_MODIFIED, prj.lastModified(projectName, ""));
    return prj;
  } 
  
  public EProject() {
   super(ID_Project, 
	 new String [] {ID_ShortTitle, ID_Description, ID_Author, ID_Date,  
	                ID_Algorithms, ID_TestSets, ID_Tags,
                        ID_ProjectJARs, ID_AlgorithmJARs, 
                        ID_EMExecFamily, ID_CNTExecFamily, ID_JVMExecFamily, 
                        ID_ProjPresenters, ID_AlgPresenters}
	);
   setRepresentatives(ID_NAME, ID_Author);
  }

  private static String extractProjectNameFromFileName(String fileName, String defaultName) {
    Pattern pattern = Pattern.compile(".*[\\\\/]PROJ-([^\\\\/]+)[\\\\/].*");     
    Matcher matcher = pattern.matcher(fileName.toString());
    return (matcher.matches() ? matcher.group(1) : defaultName);
  }
  
  public EProject(File fileName) {
    this();
    initFromFile(fileName);
    
    // name of a project is in path (not in "Name" property)
    fields.put(ID_NAME, extractProjectNameFromFileName(fileName.toString(), getName()));
  }
  
  
  public String getProjectRootDir() {
    return (entity_rootdir != null && entity_rootdir.endsWith("proj")) ?
      entity_rootdir.substring(0, entity_rootdir.length()-5) : entity_rootdir;
  }

  public String getProjectFamily(String mType) { 
    return getProjectFamily(MeasurementType.mtOf(mType));
  }
  public String getProjectFamily(MeasurementType mt) {
    String myFamily = getField(getMeasurementTypeFieldID(mt));
    return myFamily == null ? "" : myFamily;
  }
  
  /**
   * Sets the [mtype]ExecFamily field and saves settings to file.
   * @param mType
   * @param family
   * @param override 
   */
  public void setFamilyAndSave(MeasurementType mType, String family, boolean override) {
    String myFamily = getProjectFamily(mType);    
    
    if (override || myFamily == null || myFamily.isEmpty()) {
      String familyFieldID = getMeasurementTypeFieldID(mType);
      set(familyFieldID, family);
      saveEntity();
    }
  }    
  
  // Returns fieldID for given mesatrement type.
  public static String getMeasurementTypeFieldID(MeasurementType mt) {
    switch (mt) {
        case EM:
          return EProject.ID_EMExecFamily; 
        case CNT:
          return EProject.ID_CNTExecFamily; 
        case JVM:
          return EProject.ID_JVMExecFamily; 
    }
    return "?";
  }
  

  // Names of java classes for the project
  public String getAbstractAlgorithmClassname() {
    return /* getName() +*/ "ProjectAbstractAlgorithm";
  }
  public String getTestCaseClassname() {
    return /* getName() +*/ "TestCase";
  }
  public String getInputClassname() {
    return /* getName() +*/ "Input";
  }
  public String getOutputClassname() {
    return /* getName() +*/ "Output";
  }
  
  @Override
  // datum zadnje spremembe je datum spremembe poljubne src (.java) datoteke oziroma
  // poljubne .json datoteke (vse razen project.json)
  public long getLastModified(String projectName, String entityName) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);

    String srcRoot     = ATGlobal.getPROJECTsrc(projectRoot);
    File[] srcFiles    = new File(srcRoot).listFiles((dir, name) -> name.endsWith(".java"));
    
    String projRoot    = ATGlobal.getPROJECTConfigFolder(ATGlobal.getALGatorDataRoot(), projectName);
    File[] projFiles   = new File(projRoot).listFiles((dir, name) -> name.endsWith(".json") && !name.startsWith("project"));    

    long last = 0;
    for (File srcFile : srcFiles) 
      last = Math.max(last, srcFile.lastModified()/1000);
    for (File projFile : projFiles) 
      last = Math.max(last, projFile.lastModified()/1000);

    return last;
  }  
}
