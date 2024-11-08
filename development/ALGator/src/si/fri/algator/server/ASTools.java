package si.fri.algator.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.admin.Maintenance;
import si.fri.algator.admin.Tools;
import si.fri.algator.ausers.CanUtil;

import si.fri.algator.entities.CompCap;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EComputer;
import si.fri.algator.entities.EGenerator;
import si.fri.algator.entities.EPresenterN;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.ETestCase;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.tools.SortedArray;
import si.fri.algator.entities.Entity;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.UniqueIDGenerator;

/**
 *
 * @author tomaz
 */
public class ASTools {
  
    // one array of tasks is shared by all servers
  public static SortedArray<ASTask> activeTasks;
  // v tej vrsti so taski, ki so že zaprti in niso starejši od enega tedna;
  // po enem tednu se iz te vrste odstranijo in se zapišejo na konec datoteke tasks.archived
  public static SortedArray<ASTask> closedTasks;

  // tasks that were canceled by user; taskID is put into this set when user sends "cancelTask" request
  // and it is removed from set when task is really canceled; this set is used only for INPROGRESS
  // tasks, since these tasks can not be canceled immedeately, but only when TaskClient contacts ALGatorServer
  // (with taskResult, closeTask or getTask request)
  public static TreeMap<Integer, ASTaskStatus> pausedAndCanceledTasks;

  public static Hashtable<String, JSONObject> statusResults;
  
  static {
    // when a server is created, array of tasks is read from file
    if (activeTasks == null) {
      activeTasks = ASTools.readADETasks(0);
    }
    if (closedTasks == null) {
      closedTasks = ASTools.readADETasks(1);
    }

    if (pausedAndCanceledTasks == null) {
      pausedAndCanceledTasks = new TreeMap();
    }

    if (statusResults == null)
      statusResults = new Hashtable<>();
  }

  public static final int    OK_STATUS = 0;
  public static final String ID_STATUS = "Status";
  public static final String ID_MSG    = "Message";
  public static final String ID_ANSWER = "Answer";
  
    // servers's answer contains Status (0 ... OK, >0 ... error), Message (error message) and Answer ()
  public final static String ANSWER = "{\"" + ID_STATUS + "\":%d, \"" + ID_MSG + "\":\"%s\", \"" + ID_ANSWER + "\": %s%s%s}";

  /**
   * Returns server's answer string (answer is of type json)
   */
  static String jAnswer(int status, String msg, String answer) {
    return String.format(ANSWER, status, msg, "", answer, "");
  }

  /**
   * Returns server's answer string (answer is of type int)
   */
  static String iAnswer(int status, String msg, long answer) {
    return String.format(ANSWER, status, msg, "", Long.toString(answer), "");
  }

  /**
   * Returns server's answer string (answer is of type string)
   */
  static String sAnswer(int status, String msg, String answer) {
    return String.format(ANSWER, status, msg, "\"", answer, "\"");
  }

  /**
   * Decodes a string in json formnat and returns a json object. If error
   * occures method return json object with propertis ID_STATUS (-1), ID_MSG,
   * ID_ANSWER.
   */
  public static JSONObject decodeAnswer(String answer) {
    JSONObject result = new JSONObject();
    try {
      result = new JSONObject(answer);
    } catch (Exception e) {
      result.put(ID_MSG, "Error decoding answer: " + e.toString());
    }
    if (!result.has(ID_STATUS)) {
      result.put(ID_STATUS, -1);
    }
    if (!result.has(ID_MSG)) {
      result.put(ID_MSG, "");
    }
    if (!result.has(ID_ANSWER)) {
      result.put(ID_ANSWER, "");
    }
    return result;
  }
  
  
  /**
   * Converts Entity to json string.
   */
  public static String getJSONString(Entity ent, String... props) {
    String result = "";
    for (String prop : props) {
      String sp = "?";
      Object pp = ent.getField(prop);
      if (pp != null) {
        if (pp instanceof String) {
          sp = "\"" + (String) pp + "\"";
        }
        if (pp instanceof Integer) {
          sp = Integer.toString((Integer) pp);
        }
        if (pp instanceof Double) {
          sp = Double.toString((Double) pp);
        }
        if (pp instanceof JSONObject) {
          sp = ((JSONObject) pp).toString();
        }
        if (pp instanceof JSONArray) {
          sp = ((JSONArray) pp).toString();
        }
      }
      result += (result.isEmpty() ? "" : ", ") + String.format("\"%s\":%s", prop, sp);
    }
    return "{" + result + "}";
  }
  
/**** Supporting methods for getData request    */
  
  /**
   * Returns a list of all "visible" projects.
   */
  public static String getProjectsData(String uid) {
    String projectsRoot = ATGlobal.getPROJECTSfolder(ATGlobal.getALGatorDataRoot());
    String[] projects = new File(projectsRoot).list((dir,name) -> 
      { File proj = new File(dir, name);
        return proj.isDirectory() &&
               name.startsWith(String.format(ATGlobal.ATDIR_projRootDir,"")) &&
               new File(new File(proj, ATGlobal.ATDIR_projConfDir), ATGlobal.getPROJECTConfigName()).exists();
      });
    
    // TODO:  filter-out projects according to user rights 
    ArrayList<String> readableProjects = new ArrayList();
    for (String project : projects) {
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(
              ATGlobal.getALGatorDataRoot(), project.substring(5))));
      String eid = eProject.getString(Entity.ID_EID);
      if (CanUtil.can(uid, eid, "can_read")) readableProjects.add(eProject.getName());
    }
    return jAnswer(OK_STATUS, "Projects", 
        new JSONArray(readableProjects).toString()
    );  
  }
  
  public static JSONObject getProjectJSON(String uid, String projectName) {
    String fileName = ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName);
    EProject project = new EProject(new File(fileName));
    
    JSONObject result = new JSONObject(project.toJSONString());
    if (!CanUtil.can(uid, result.optString("eid", ""), "can_read"))
      return new JSONObject();
    
    String data_root = ATGlobal.getALGatorDataRoot();
    String proot = ATGlobal.getPROJECTroot(data_root, projectName);

    // remove "non-existing" or "non-visible" algorithms
    JSONArray algs = result.getJSONArray(EProject.ID_Algorithms);
    if (algs!=null) for (int i=algs.length()-1; i>=0; i--) {
      if (ATGlobal.algorithmExists(data_root, projectName, algs.getString(i))) {
        String aName = ATGlobal.getALGORITHMfilename(proot, algs.getString(i));
        EAlgorithm ea = new EAlgorithm(new File(aName));
        String eid = ea.getString("eid");
        if (!CanUtil.can(uid, eid, "can_read")) algs.remove(i);
      } else algs.remove(i);
    }
    // remove "non-existing" or "non-visible" testsets
    JSONArray tsts = result.getJSONArray(EProject.ID_TestSets);
    if (tsts!=null) for (int i=tsts.length()-1; i>=0; i--) {
      if (ATGlobal.testsetExists(data_root, projectName, tsts.getString(i))) {
        String tName = ATGlobal.getTESTSETfilename(data_root,projectName, tsts.getString(i));
        ETestSet et = new ETestSet(new File(tName));
        String eid = et.getString("eid");
        if (!CanUtil.can(uid, eid, "can_read")) tsts.remove(i);
      } else tsts.remove(i);
    }
    // remove "non-existing" or "non-visible" testsets
    JSONArray prts = result.getJSONArray(EProject.ID_MainProjPresenters);
    if (prts!=null) for (int i=prts.length()-1; i>=0; i--) {
      if (ATGlobal.presenterExists(data_root, projectName, prts.getString(i))) {
        String pName = ATGlobal.getPRESENTERFilename(data_root,projectName, prts.getString(i));
        EPresenterN ep = new EPresenterN(new File(pName));
        String eid = ep.getString("eid");
        if (!CanUtil.can(uid, eid, "can_read")) prts.remove(i);
      } else prts.remove(i);
    }
        
    return result;
  }
  
  public static String getProjectData(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");
    
    JSONObject result = getProjectJSON(uid, projectName);

    // POPRAVI! Dodaj le tiste računalnike, ki so že izvajali teste za ta problem
    JSONArray  compArray = new JSONArray(Arrays.asList(new String[]{"F0.C0", "F0.C1", "F1.C1", "F1.C2"}));
    result.put("Computers", compArray);
    
    return jAnswer(OK_STATUS, String.format("Project '%s'.", projectName), result.toString());
  }
  
  public static String getAlgorithmData(String projectName, String algorithmName, boolean deep) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");
    if (!ATGlobal.algorithmExists(ATGlobal.getALGatorDataRoot(), projectName, algorithmName))
      return sAnswer(3, String.format("Algorithm '%s' does not exist in project '%s'.",algorithmName, projectName), "");
    
    String fileName = ATGlobal.getALGORITHMfilename(
      ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
    EAlgorithm algorithm = new EAlgorithm(new File(fileName));
    
    JSONObject algorithmData = new JSONObject();
    algorithmData.put("Properties",  new JSONObject(algorithm.toJSONString()));
    if (deep) {
      String sourceFileName = ATGlobal.getALGORITHMsrc(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
      String content = ""; 
      try {content = new String(Files.readAllBytes(Paths.get(sourceFileName + File.separator + "Algorithm.java")));} catch (Exception e) {}
      algorithmData.put("FileContent", content);
      
      String htmlFileName = ATGlobal.getALGORITHMHtmlName(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
      String htmlContent = ""; 
      try {htmlContent = new String(Files.readAllBytes(Paths.get(htmlFileName)));} catch (Exception e) {}
      algorithmData.put("HtmlFileContent", htmlContent);
    }
    
    return jAnswer(OK_STATUS, String.format("Algorithm '%s'.", algorithmName), algorithmData.toString());    
  }  

  public static String getTestsetData(String projectName, String testsetName, boolean deep) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");
    if (!ATGlobal.testsetExists(ATGlobal.getALGatorDataRoot(), projectName, testsetName))
      return sAnswer(3, String.format("Testset '%s' does not exist in project '%s'.", testsetName, projectName), "");
    
    String fileName = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
    ETestSet testset = new ETestSet(new File(fileName));
    
    JSONObject testsetData = new JSONObject();
    testsetData.put("Properties",  new JSONObject(testset.toJSONString()));
    if (deep) {
      String dataFileName = ATGlobal.getTESTSETDATAfilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
      String content = ""; 
      try {content = new String(Files.readAllBytes(Paths.get(dataFileName)));} catch (Exception e) {}
      testsetData.put("FileContent", content);
    }
    
    return jAnswer(OK_STATUS, String.format("Testset '%s'.", testsetName), testsetData.toString());
  }  

  
  public static String getPresenter(String projectName, String presenterName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String fileName = ATGlobal.getPRESENTERFilename(ATGlobal.getALGatorDataRoot(), projectName, presenterName);

    File pFile = new File(fileName);    
    if (!pFile.exists())
      return sAnswer(3, String.format("Presenter '%s' does not exist in project '%s'.", presenterName, projectName), "");    
    
    EPresenterN presenter = new EPresenterN(pFile);
    String presenterJSON = "{}";
    try {
      presenterJSON = FileUtils.readFileToString(pFile, "UTF-8");
      JSONObject jObj= new JSONObject(presenterJSON);
      presenterJSON = jObj.optString("Presenter", "{}");
    } catch (Exception e) {}
    return jAnswer(OK_STATUS, String.format("Presenter '%s'.", presenterName), presenterJSON);
  }
  
  public static String getProjectSources(String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    

    JSONObject result = new JSONObject();
    String projectSrc = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName)); 
    String[][] srcs = {{"Input", "Input.java"}, {"Output", "Output.java"}, {"Algorithm", "ProjectAbstractAlgorithm.java"}, {"Tools", "Tools.java"} };
    for (String[] src : srcs) {
      String fileCont = ASTools.getFileContent(new File(projectSrc,src[1]).toString());
      result.put(src[0], Base64.getEncoder().encodeToString(fileCont.getBytes()));
   }
    
    String[] files = new File(projectSrc).list();
    
    JSONObject indicators = new JSONObject();    
    Pattern p = Pattern.compile(ATGlobal.INDICATOR_TEST_OFFSET+"(.*)[.]java");
    for (String file : files) {
      Matcher m = p.matcher(file);
      if (m.find()) {
        String indName = m.group(1);
        String fileCont = ASTools.getFileContent(new File(projectSrc,file).toString());
        indicators.put(indName, Base64.getEncoder().encodeToString(fileCont.getBytes()));
      }
    }
    result.put("Indicators", indicators);
    
    JSONObject generators = new JSONObject();    
    p = Pattern.compile("TestCaseGenerator_(.*)[.]java");
    for (String file : files) {
      Matcher m = p.matcher(file);
      if (m.find()) {
        String genName = m.group(1);
        String fileCont = ASTools.getFileContent(new File(projectSrc,file).toString());
        generators.put(genName, Base64.getEncoder().encodeToString(fileCont.getBytes()));
      }
    }
    result.put("Generators", generators);

    return jAnswer(OK_STATUS, String.format("Project '%s' sources.", projectName), result.toString());
  }

  public static String getProjectProps(String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);
    
    JSONObject result = new JSONObject();
    result.put("Algorithms", project.getAlgorithms().keySet());
    result.put("TestSets",   project.getTestSets().keySet());

    ArrayList<String> inputParams = new ArrayList(
            Arrays.asList(project.getTestCaseDescription().getInputParameters()));
    
    JSONObject jParams = new JSONObject();
    for (EVariable par : project.getTestCaseDescription().getParameters()) {
      if (!ETestCase.TESTCASE_PROPS.equals(par.getName())) {
       JSONObject param = par.toJSON(false);
       param.put("IsInputParameter", inputParams.contains(par.getName()));
       jParams.put(par.getName(), param);
      }
    }
    result.put("Parameters", jParams);
    
    String[] mTypes = {"em", "cnt", "jvm"};
    for (String mType : mTypes) {
      try {
        JSONObject jInd = new JSONObject();
        for (EVariable ind : project.getResultDescriptions().get(MeasurementType.mtOf(mType)).getIndicators()) { 
          jInd.put(ind.getName(), ind.toJSON(false));
        }
        result.put(mType.toUpperCase() + " indicators", jInd);      
      } catch (Exception e) {}
    }       
    
    HashMap<String, EGenerator> generators = project.getTestCaseDescription().getGenerators();
    JSONObject jGenerators = new JSONObject();
    for (String genKey : generators.keySet()) {
      EGenerator gen = generators.get(genKey);
      jGenerators.put(genKey, gen.toJSON(false));
    }
    result.put("Generators", jGenerators);


    
    return jAnswer(OK_STATUS, String.format("Project '%s' sources.", projectName), result.toString());
  }  


  public static String getProjectDocs(String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    

    JSONObject result = new JSONObject();
    String projectDoc = ATGlobal.getPROJECTdoc(ATGlobal.getALGatorDataRoot(), projectName); 
    String[][] docs = {{"Project", "project.html"}, {"Algorithm", "algorithm.html"}, {"References", "references.html"}, {"TestCase", "testcase.html"}, {"TestSet", "testset.html"}};
    for (String[] doc : docs) {
      String fileCont = ASTools.getFileContent(new File(projectDoc,doc[1]).toString());
      result.put(doc[0], Base64.getEncoder().encodeToString(fileCont.getBytes()));
   }

   JSONObject resources = new JSONObject(); 
   String resDir = ATGlobal.getPROJECTResourcesFolder(projectName);    
   File resFile = new File(resDir);
   if (resFile.exists() && resFile.isDirectory()) {
     for (String file : resFile.list()) {
       if ((new File(file)).isDirectory()) continue;
       resources.put(file, new File(projectDoc,file).lastModified());
     }
   }
   result.put("Resources", resources);    

   return jAnswer(OK_STATUS, String.format("Project '%s' docs.", projectName), result.toString());
  }
  
  public static String getProjectResource(String projectName, String resource) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    

    String resDir = ATGlobal.getPROJECTResourcesFolder(projectName);    
    File   resFile    = new File(resDir, resource);
    
    if (!resFile.exists())
      return sAnswer(3, String.format("Resource '%s' of project '%s' does not exist.",resource, projectName), "");    

    String resourceCont = Tools.encodeFileToBase64Binary(resFile.toString());
    return sAnswer(OK_STATUS, String.format("Project resource '%s'.", resource), resourceCont);
  }
  
/**** Supporting methods for getData request  ... end  */
  
  /**
   * Method gets the answer of an action in form "status:msg"; if status==0, 
   * method returns msgOK and msg, else msgNOK
   * 
   */
  private static String parsedAnswer(String result, String msgOK, String msgNOK) {
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    String msg = result.length()> 2 ? result.substring(2) : msgNOK;
    if (msg.startsWith("{"))
      return jAnswer(status, status==0 ? msgOK:msg, status==0?msg:"{}");         
    else
      return sAnswer(status, status==0 ? msgOK:msg, status==0?msg:"");   
  }
  
/**** Supporting methods for alter request    */
  
  public static String newProject(String uid, String projectName) {
    if (ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' already exists.",projectName), "");    
    
    String result = Maintenance.createProject(uid, projectName);
    return parsedAnswer(result,"Project created.","Error creating project."); 
  }

  
  public static String saveProjectGeneral(String projectName, Object data) {
    if (!(data instanceof JSONObject)) 
      return sAnswer(1, "Expecting 'Data' to be an JSON object.", "");    

    String projFilename = ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName);
    String result = saveJSONProperties(
       projFilename, "Project", new String[]{"Description", "Author", "Date"},(JSONObject) data);
    return parsedAnswer(result,"Project properties saved.","Error saving project properties."); 
  }

  // Data should contain information about html type and html content 
  // html type: "problemDescription", "testCases", "testSets", "projDescAlgorithms", "references"
  // html content: base64 encoded html file content
  public static String saveHTML(String projectName, Object data) {
    if (!(data instanceof JSONObject)) 
      return sAnswer(1, "Expecting 'Data' to be an JSON object.", "");    

    String result = saveHTMLToFile(projectName,(JSONObject) data);
    return parsedAnswer(result,"Project properties saved.","Error saving project properties."); 
  }

  
  public static String newPresenter(String projectName, int presenterType) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.createPresenter("", projectName, "", presenterType);
    return parsedAnswer(result,"Presenter created.","Error creating presenter."); 
  }

  public static String removePresenter(String projectName, String presenterName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.removePresenter(projectName, presenterName);
    return parsedAnswer(result,"Presenter removed.","Error removing presenter."); 
  }

  public static String savePresenter(String projectName, String presenterName, JSONObject presenterData) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    

    String presenterFilename = ATGlobal.getPRESENTERFilename(ATGlobal.getALGatorDataRoot(), projectName, presenterName);
    try {
      JSONObject presenter = new JSONObject();
      presenter.put(EPresenterN.ID_PresenterParameter, presenterData);
      
      PrintWriter pw = new PrintWriter(presenterFilename);
      pw.println(presenter.toString(2)); 
      pw.close();
      
      return sAnswer(OK_STATUS, "Presenter saved.", presenterName);
    } catch (Exception e) {
      return sAnswer(1, "Error saving presenter.", e.toString());
    }
  }
  
  public static String newParameter(String projectName, String parameterName, boolean isInput) {     
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String username = ""; // !!!
    String parDesc = String.format("{'Name':'%s', 'Description':'', 'Type':'string'}", parameterName);
    String result = Maintenance.addParameter(username, projectName, parDesc, String.format("{'isInputParameter':%s}", isInput ? "true" : "false"));
    return parsedAnswer(result,"Parameter added.", "Error adding parameter.");     
  }
  
  public static String removeParameter(String projectName, String parameterName) {
    
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.removeParameter(projectName, parameterName  );
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    String msg = result.length()> 2 ? result.substring(2) : "Error removing parameter.";
    
    return sAnswer(status, status==0 ? "Parameter removed.": msg, status==0?msg:"");
  }
  
  public static String saveParameter(String projectName, String parameterName, JSONObject parameter) {
    String testcaseFilename = ATGlobal.getTESTCASEDESCfilename(ATGlobal.getALGatorDataRoot(), projectName);

    String result = replaceJSONArrayElement(
       testcaseFilename, "TestCase", "Parameters", "Name", parameterName, parameter);
    return parsedAnswer(result,"Parameter saved.","Error saving parameter."); 
  }

  public static String newAlgorithm(String uid, String projectName, String algorithmName) {     
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.createAlgorithm(uid, projectName, algorithmName);
    return parsedAnswer(result,"Algorithm added.", "Error adding algorithm.");     
  }
  
  public static String saveAlgorithm(String projectName, String algorithmName, JSONObject algorithmData) {
    String algorithmFilename = ATGlobal.getALGORITHMfilename(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
    String result = saveJSONProperties(
       algorithmFilename, "Algorithm", new String[]{"Name","Description","ShortName", "Date", "Author", "Language"}, algorithmData.getJSONObject("Properties"));
    
    String fileSaved = "";
    
    String fileCont = algorithmData.optString("FileContent", "");
      if (!fileCont.isEmpty()) {
      String sourceFileName = ATGlobal.getALGORITHMsrc(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName) + File.separator + "Algorithm.java";
      fileSaved = " Source saved to file.";
      try {
        Files.write(Paths.get(sourceFileName), fileCont.getBytes());
      } catch (Exception e) {
        fileSaved = " Error saving source to file.";
      }     
    }

    String htmlCont = algorithmData.optString("HtmlFileContent", "");
    if (!htmlCont.isEmpty()) {
      String htmlFileName = ATGlobal.getALGORITHMHtmlName(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);      
      try {
        Files.write(Paths.get(htmlFileName), htmlCont.getBytes());
        fileSaved = " HTML saved to file.";        
      } catch (Exception e) {
        fileSaved = " Error saving HTML to file.";
      }     
    }
 
    return parsedAnswer(result,"Algorithm properties saved." + fileSaved, "Error saving testset properties."); 
  }
  public static String removeAlgorithm(String uid, String projectName, String algorithmName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    String result = Maintenance.removeAlgorithm(uid, projectName, algorithmName);    
    return parsedAnswer(result, "Algorithm removed.", "Error removing algorithm.");
  }  
  
  public static String newTestset(String uid, String projectName, String testsetName) {     
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.createTestset(uid, projectName, testsetName);
    return parsedAnswer(result,"Testset added.", "Error adding testset.");     
  }
  
  public static String removeTestset(String projectName, String testsetName) {
    
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.removeTestset(projectName, testsetName);
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    String msg = result.length()> 2 ? result.substring(2) : "Error removing testset.";
    
    return sAnswer(status, status==0 ? "Testset removed.": msg, status==0?msg:"");
  }
  public static String saveTestset(String projectName, String testsetName, JSONObject testsetData) {
    String testsetFilename = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
    String result = saveJSONProperties(
       testsetFilename, "TestSet", new String[]{"Name","ShortName","Description","N","TestRepeat","TimeLimit"}, testsetData.getJSONObject("Properties"));
    String fileCont = testsetData.optString("FileContent", "");
    
    String fileSaved = "";
    if (!fileCont.isEmpty()) {
      String dataFileName = ATGlobal.getTESTSETDATAfilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
      fileSaved = " Tests saved to file.";
      try {
        Files.write(Paths.get(dataFileName), fileCont.getBytes());
      } catch (Exception e) {
        fileSaved = " Error saving tests to file.";
      }
    }
    return parsedAnswer(result,"Testset properties saved." + fileSaved, "Error saving testset properties."); 
  }
  
     
  public static String newIndicator(String projectName, String indicatorName, String indicatorType, JSONObject meta) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String username = ""; // !!!
    String indType  = "indicator".equals(indicatorType) ? "int" : indicatorType; 
    String indDesc = String.format("{'Name':'%s', 'Description':'', 'Type':'%s', 'Meta':%s}", indicatorName, indType, meta.toString());
    String result = Maintenance.addIndicator(username, projectName, indDesc, "{}");
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    String msg = result.length()> 2 ? result.substring(2) : "Error adding indicator.";

    String code = ((status != 0) || (!"indicator".equals(indicatorType))) ? "" :
        ASTools.getFileContent(projectName, "proj/src/IndicatorTest_"+indicatorName+".java", true);
    return sAnswer(status, status==0 ? "Indicator added.":msg, code);
  }
  
  public static String removeIndicator(String projectName, String indicatorName, String type) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.removeIndicator(projectName, indicatorName, type);
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    String msg = result.length()> 2 ? result.substring(2) : "Error removing indicator.";
    
    return sAnswer(status, status==0 ? "Indicator removed.": msg, status==0?msg:"");
  }
  
  public static String saveIndicator(String projectName, JSONObject indicator, String type) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
        
    String result = Maintenance.saveIndicator(projectName, indicator, type);
    String tYpe=type.isEmpty()?"Indicator":(type.toUpperCase().charAt(0)+type.substring(1));
    return parsedAnswer(result,tYpe + " saved.", "Error saving "+type+".");     
  }
  
  public static String newGenerator(String projectName, String generatorName, JSONArray genParams) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    

    String username = "";
    List<String> list = new ArrayList<String>();
    for(int i = 0; i < genParams.length(); i++){list.add(genParams.getString(i));}

    String result = Maintenance.addTestCaseGenerator(
        username, projectName, generatorName, list.toArray(new String[0]));
        
    return parsedAnswer(result, "Generator added.", "Error adding generator.");     
  }
  public static String saveGenerator(String projectName, String generatorType, JSONObject generator, String code) {
    String testcaseFilename = ATGlobal.getTESTCASEDESCfilename(ATGlobal.getALGatorDataRoot(), projectName);

    String result = replaceJSONArrayElement(
       testcaseFilename, "TestCase", "Generators", "Type", generatorType, generator);
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    if (status != 0) 
      return sAnswer(5, "Error saving generator - element can not be replaced.", "");
    
    String generatorFilename = new File(new File(new File(ATGlobal.ATDIR_projConfDir), ATGlobal.ATDIR_srcDir),
            ATGlobal.getGENERATORFilename(generatorType)).getPath();
    String decodecCode = new String(Base64.getDecoder().decode(code));
    result = saveFile(projectName, generatorFilename, decodecCode);

    return parsedAnswer(result,"Generator saved.","Error saving generator."); 
  }
  
  public static String removeGenerator(String projectName, String generatorName) {    
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName))
      return sAnswer(2, String.format("Project '%s' does not exist.",projectName), "");    
    
    String result = Maintenance.removeGenerator(projectName, generatorName);
    int status = 0; try { status = Integer.parseInt(result.substring(0,1));} catch (Exception e) {}
    String msg = result.length()> 2 ? result.substring(2) : "Error removing generator.";
    
    return sAnswer(status, status==0 ? "Generator removed.": msg, status==0?msg:"");
  }

  
/**** Supporting methods for alter request  ... end  */  
  
  /**
   * Method reads a file with tasks and returns a list.
   * @param type 0 ... active tasks, 1 ... closed tasks, 2 ... archived tasks
   * @return 
   */
  public static SortedArray<ASTask> readADETasks(int type) {
    SortedArray<ASTask> tasks = new SortedArray<>();
    File taskFile = new File(ASGlobal.getADETasklistFilename(type));
    if (taskFile.exists()) {
      try (DataInputStream dis = new DataInputStream(new FileInputStream(taskFile));) {
        while (dis.available() > 0) {
          String line = dis.readUTF();
          ASTask task = new ASTask(line);
          tasks.add(task);
        }
      } catch (Exception e) {
        // if error ocures, nothing can be done
      }
    }
    return tasks;
  }

  /**
   * From closedTasks remove all tasks that are more than numberOfDays old 
   *   and write them to archivedTasks file. 
   */
  private static void removeAndArchiveOldTasks(SortedArray<ASTask> closedTasks, int numberOfDays) {
    ArrayList<ASTask> removedTasks = new ArrayList<>();
    
    long now = new Date().getTime();
    File archivedFile = new File(ASGlobal.getADETasklistFilename(2)); // archived tasks file
    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(archivedFile));) {
      Iterator<ASTask> it = closedTasks.iterator();
      while (it.hasNext()) {
        ASTask task = it.next();
        if (now - task.getTaskStatusDate() > numberOfDays*1000*60*60*24) {
          dos.writeUTF(task.toJSONString());
          removedTasks.add(task);
        }
      }
    } catch (Exception e) {
      // if error ocures, nothing can be done
      System.out.println(e);
    }
    
    for (ASTask removedTask : removedTasks) {
      closedTasks.remove(removedTask); 
    }
  }
  
  public static void writeADETasks(SortedArray<ASTask> tasks, int type) {
    File taskFile = new File(ASGlobal.getADETasklistFilename(type));
    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(taskFile));) {
      Iterator<ASTask> it = tasks.iterator();
      while (it.hasNext()) {
        dos.writeUTF(it.next().toJSONString());
      }
    } catch (Exception e) {
      // if error ocures, nothing can be done
    }
  }

  /**
   * Get the computerUID of a computer with given familyID and computerID
   */
  private static String getComputerUID(String famX, String compX) {
    ArrayList<EComputer> computers = EAlgatorConfig.getConfig().getComputers();
    for (EComputer computer : computers) {
      String fam = computer.getField(EComputer.ID_FamilyID);
      String com = computer.getField(EComputer.ID_ComputerID);
      if (famX.equals(fam) && compX.equals(com))
        return computer.getField(EComputer.ID_ComputerUID);
    }
    return "";
  }
  
  /**
   *  Among all the registered computers, find and return the one with a given uid.
   *  If no computer has that uid, method returns null.
   */
  private static EComputer getComputer(String uid) {
    // default computer (with uid=0) is F0.C0 (local computer)
    if ("0".equals(uid)) return new EComputer();
    
    ArrayList<EComputer> computers = EAlgatorConfig.getConfig().getComputers();
    for (EComputer computer : computers) {
      if (uid.equals(computer.getField(EComputer.ID_ComputerUID)))
        return computer;
     }
    return null;    
  }
  
  /**
   * Among all the registered computers, find the one with a given uid 
   * and return its ID. If no computer has that uid, method returns "/".
   */
  private static String getComputerID(String uid) {
    EComputer eC = getComputer(uid);
    return (eC!=null && eC.getField(EComputer.ID_ComputerID)!=null) ? eC.getField(EComputer.ID_ComputerID) : "/";
  }
  
  
    
  public static String familyOfComputer(String uid) {
    EComputer comp = getComputer(uid);
    if (comp != null)
      return comp.getString(EComputer.ID_FamilyID);
    else 
      return "?";
  }
  
  public static String getFamilyAndComputerName(String uid) {
    String result = "unknown";
    try {
     EComputer comp = getComputer(uid);
     if (comp != null)
       result = comp.getString(EComputer.ID_FamilyID) + "." + comp.getString(EComputer.ID_ComputerID);
    } catch (Exception e) {}
    return result;
  }
  
  /**
   * Method finds the first (most appropriate) task for computer uid. Task is appropriate if task's and computer's 
   * families match and if computers's capabilities are sufficient.
   * More appropritate tasks are queued before (i.e. have smaller index in queue than) less appropriate ones. 
   */
  public static ASTask findFirstTaskForComputer(SortedArray<ASTask> taskQueue, String uid, boolean allowInprogressTasks) {
    EComputer comp = getComputer(uid);
    if (comp == null || comp.getCapabilities() == null) return null;
    
    TreeSet<CompCap> cCapabilities = comp.getCapabilities();
    String        cfamily          = comp.getString(EComputer.ID_FamilyID);
    if (cfamily == null || cfamily.isEmpty()) return null;
    
        
    for (ASTask task: taskQueue) {
      if (!allowInprogressTasks && ASTaskStatus.INPROGRESS.equals(task.getTaskStatus())) continue;

      if (!(task.getTaskStatus().equals(ASTaskStatus.INPROGRESS) || task.getTaskStatus().equals(ASTaskStatus.PENDING) || task.getTaskStatus().equals(ASTaskStatus.QUEUED)))
        continue;
     
      // tasks with computer already assigned can only be executed by the same computer 
      String taskComputerID = task.getComputerUID();
      if (taskComputerID != null && !taskComputerID.equals(Entity.unknown_value) && !taskComputerID.isEmpty()) {
        if (uid.equals(taskComputerID))
          return task;
        else 
          continue;
      }
      
      String tFamily = task.getString(ASTask.ID_Family); if (tFamily == null) tFamily = "";
      String tmtype  = task.getString(ASTask.ID_MType);  if (tmtype  == null || tmtype.isEmpty()) tmtype = "em";
      CompCap requiredCapability = CompCap.capability(tmtype);
                  
      if ((tFamily.isEmpty() || tFamily.equals(cfamily)) && cCapabilities.contains(requiredCapability))
        return task;
    }
    return null;
  }

  
  public static String getResultFilename(ASTask task) {
    String projName    = (String) task.getField(ASTask.ID_Project); 
    String algName     = (String) task.getField(ASTask.ID_Algorithm); 
    String testsetName = (String) task.getField(ASTask.ID_Testset);
    
    
    String compID           = getFamilyAndComputerName((String) task.getField(ASTask.ID_ComputerUID));
    MeasurementType mType   = MeasurementType.mtOf    ((String) task.getField(ASTask.ID_MType));
    
    return ATGlobal.getRESULTfilename(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName), 
        algName, testsetName, mType, compID
    );
  }
  
  /**
   * Sets the status of a task and writes this status to the task status file
   */
  public static String getTaskStatusFilename(ASTask task) {
    return ATGlobal.getTaskStatusFilename(
       (String) task.getField(ASTask.ID_Project), (String) task.getField(ASTask.ID_Algorithm), 
       (String) task.getField(ASTask.ID_Testset), (String) task.getField(ASTask.ID_MType));
  }
  
  public static void setTaskStatus(ASTask task, ASTaskStatus status, String msg, String computer) {
    task.setTaskStatus(status, computer, msg);
    
    if (computer == null)
      computer = task.getField(ASTask.ID_ComputerUID);    
    logTaskStatus(task, status, msg, computer);
    
    // if task was closed --> move from "active" to "closed" queue
    if (ASTaskStatus.closedTaskStatuses.contains(status)) {
      activeTasks.remove(task); 
      ASTools.writeADETasks(activeTasks, 0);
      
      closedTasks.add(task);
      
      // "clean up" closedTasks ...
      removeAndArchiveOldTasks(closedTasks, 1);
      // ... and write the remaining queue to file
      ASTools.writeADETasks(closedTasks, 1);  
    } else {
      activeTasks.touch(task);    
      ASTools.writeADETasks(activeTasks, 0);
    }
  }
  
  /**
   * Writes the status of a task to the task status file.
   * Computer name is valid only when task status is "COMPLETED"
   */
  public static void logTaskStatus(ASTask task, ASTaskStatus status, String msg, String computer) {
    if (task==null) return;
    
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    String idtFilename = getTaskStatusFilename(task);
    
    String startDate="", statusMsg = "", endDate="";
    if (status.equals(ASTaskStatus.PENDING)) {      
      startDate = date + ": Task pending";
    } else try (Scanner sc = new Scanner(new File(idtFilename))) {
      startDate = sc.nextLine();
      statusMsg = sc.nextLine();
      endDate   = sc.nextLine();
    } catch (Exception e) {
      // on error - use default values
    }
    
    if (startDate.isEmpty()) 
      startDate = date + ": Task executed by ALGator";
    
    if (msg != null)
      statusMsg = msg;

    switch (status) {
      case INPROGRESS:
        statusMsg = date + ":INPROGRESS # " + computer + (statusMsg.isEmpty() ? "" : " # ") + statusMsg;
        break;
      case COMPLETED:  
        endDate   = date + ":COMPLETED # " + computer;
        break;
      case FAILED:
        endDate = date + ": Task failed on " + computer ;
        break;        
      case KILLED:
        endDate = date + ": Task killed on " + computer ;
        break;                
    }
      
    
    try (PrintWriter pw = new PrintWriter(new File(idtFilename))) {
     pw.println(startDate);
     pw.println(statusMsg);
     pw.print(endDate);
    } catch (Exception e) {
      // nothing can be done if error occures - ignore
    }
  }
  
  /**
   * Method finds in a given queue; if task does not exist, method returns null.
   */
  public static ASTask findTask(SortedArray<ASTask> tasks, int taskID) {
    for (ASTask task : tasks) {
      if (task.getTaskID()  == taskID) 
        return task;
    }
    return null;
  }
  
  // returns tasks in a queue for given (project, alg, tsts, mytpe); tasks might differ in family and computer
  public static ArrayList<ASTask> getTasks(SortedArray<ASTask> tasks, String project, String alg, String tst, String mType) {
    ArrayList<ASTask> rTasks = new ArrayList<>();
    for (ASTask task : tasks) {
      if (project.equals(task.getField(ASTask.ID_Project)) && alg.  equals(task.getField(ASTask.ID_Algorithm)) &&
          tst.    equals(task.getField(ASTask.ID_Testset)) && mType.equals(task.getField(ASTask.ID_MType))) 
        rTasks.add(task);
    }
    return rTasks;
  }
  
    /**
     * Method returns the last non-empty line from task status file.
     */
    public static String getTaskStatus(ASTask task) {
      String taskFilename = getTaskStatusFilename(task);
      String result = "";
      try (Scanner sc = new Scanner(new File(taskFilename))) {        
        while (sc.hasNextLine()) {
          String line = sc.nextLine();
          if (line != null && !line.trim().isEmpty())
            result =  line;
        }
      } catch (Exception e) {}
      return result;
    }

    
    
  /**
   * Returns an array of all tasks described by request. Request can have one, two, three or 
   * four parameters; if only one parameter is given (project) result contains all possible tasks
   * for this project. If all four parameters are given (project, algorithm, testset, mtype), result 
   * contains only one task. The tasks returned are strings with four parameters, 
   * i.e., "Sorting BubbleSort TestSet1 em".
   * @return 
   */
  public static ArrayList<String> getProjectTasks(ArrayList<String> params) {
    ArrayList<String> result = new ArrayList<>();
    
    if (params.size() < 1) return result; // no parameters, empty result
    
    // Test the project
    Project projekt = new Project(ATGlobal.getALGatorDataRoot(), params.get(0));
    if (!projekt.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) return result;
      
    // Test algorithms
    ArrayList<EAlgorithm> eAlgs;
    if (params.size() >= 2) {
      EAlgorithm alg = projekt.getAlgorithms().get(params.get(1));
      if (alg == null) return result;	
      eAlgs = new ArrayList(); 
      eAlgs.add(alg);
    } else {
       eAlgs = new ArrayList(projekt.getAlgorithms().values());
    }
    
    // Test testsets
    ArrayList<ETestSet> eTests;
    if (params.size() >= 3) {
      ETestSet test = projekt.getTestSets().get(params.get(2));
      if (test == null) return result;

      eTests = new ArrayList<>(); 
      eTests.add(test);
    } else {
       eTests = new ArrayList(projekt.getTestSets().values());
    }
        
    // Test mesurement type
    ArrayList<String> mtypes = new ArrayList<>();
    if (params.size() >= 4) 
      mtypes.add(params.get(3));
    else {
      mtypes.add(MeasurementType.EM.getExtension());
      mtypes.add(MeasurementType.CNT.getExtension());
      mtypes.add(MeasurementType.JVM.getExtension());
    }
      
    for (EAlgorithm ealg : eAlgs) {
      for (ETestSet ets: eTests) {
        for (String mtype : mtypes) {
          result.add(String.format("%s_%s_%s_%s", params.get(0), ealg.getName(), ets.getName(), mtype));
        }
      }
    }    
    return result;
  }
  
  
  /**
   * Method sets <mtype>ExecFamily parameter in the project of the task.
   * Method is called when computer cid starts executing task. 
   */
  public static void setComputerFamilyForProject(ASTask task, String family) {
    String mType = ((String) task.getField(ASTask.ID_MType)).toUpperCase();
    MeasurementType mt = MeasurementType.UNKNOWN;
    try {mt = MeasurementType.valueOf(mType); } catch (Exception e) {}
    if (mt.equals(MeasurementType.UNKNOWN)) return;

    
    String projectName = task.getField(ASTask.ID_Project);
    if (projectName == null || projectName.isEmpty()) return;
    
    String projectFileName = ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName);
    if (projectFileName == null || projectFileName.isEmpty()) return;
    
    File projectFile = new File(projectFileName);
    EProject project = new EProject(projectFile);
        
    project.setFamilyAndSave(mt, family, false);    
  }  
  
  /**
   * Checks existance of the project and algorithm. If they both exist, method 
   * returns "Family:familyName" else it returns error message string.
   */
  public static String checkTaskAndGetFamily(String projName, String algName, String tsName, String mType) {
    EProject proj = new EProject(new File(ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projName)));
    if (!ErrorStatus.getLastErrorStatus().equals(ErrorStatus.STATUS_OK)) 
      return String.format("Project '%s' does not exist.", projName);
    
    String[] algs = proj.getStringArray(EProject.ID_Algorithms);
    if (!Arrays.asList(algs).contains(algName))
      return String.format("Algorithm '%s' does not exist.", algName);
          
    if (mType == null) mType = "em";
    return "Family:" + proj.getProjectFamily(mType); 
  }
    
  static String getFilesAsHTMLList(File root, String indent, int prLength) {
    String result = "  " + indent + "<li><span class=\"treeNLI treeNLI-_hID_ treeCaret"+(indent.length()==0 ? " treeCaret-down" : "")+"\">"+ root.getName() + "</span>";
        
    // first add all files ...
    result += "\n" + "  " + indent + "<ul id=\"treeNUL\" class=\"treeNested"+(indent.length()==0 ? " treeActive" : "")+"\">";
    for(File file: root.listFiles()) {
      if (!file.isDirectory()) { 
        String dat = file.getAbsolutePath().substring(prLength).replace("\\", "/");
        result += "\n" + "    " + indent + "<li><span dat=\""+dat+"\" class=\"treeALI treeALI-_hID_\">"+file.getName()+"</span></li>";  
      }
    }
    // ... then folders
    for(File file: root.listFiles()) {
      if (file.isDirectory())
        result += "\n" + getFilesAsHTMLList(file, "  "+indent, prLength);
    }
    result += "\n" + "  " + indent + "</ul>";
    return result;
  }  
  
  public static String getProjectFiles(String projectName) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    
    File pr = new File(projectRoot);
    if (pr.exists())
      return "<ul id=\"treeUL\">\n" + getFilesAsHTMLList(pr, "", pr.getAbsolutePath().length()) +"\n</ul>";
    else return "";
  }
  
  public static String getFileContent(String filePath) {
    StringBuilder result = new StringBuilder();
    try {
      for (String l : Files.readAllLines(Paths.get(filePath))) 
        result.append((result.length()==0 ? "" : "\n")).append(l);      
    } catch (Exception e) {
      result = new StringBuilder("!!" + e.toString());
    }
    return result.toString();    
  }
  
  public static String getFileContent(String projectName, String fileName) {
    return getFileContent(projectName, fileName, false);
  }

  public static String getFileContent(String projectName, String fileName, boolean encode) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String filePath = projectRoot + File.separator + fileName;
    String result = getFileContent(filePath);
    if (encode) result = Base64.getEncoder().encodeToString(result.getBytes());
    return result;
  }
  
  
  public static String saveFile(String projectName, String fileName, String content) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String filePath = projectRoot + File.separator + fileName;
    try (PrintWriter pw = new PrintWriter(filePath)) {
      pw.print(content);
    } catch (Exception e) {
      return "1:" + e.toString();
    }
    return "0:File saved.";
  }
  
  public static String getMyIPAddress() {
    try(final DatagramSocket socket = new DatagramSocket()){
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
      return socket.getLocalAddress().getHostAddress();
    } catch (Exception e) {return "0.0.0.0";}
  }

  /**
   * Returns all folders with result files (e.g. F0.C0, F1.C0, ...)
   */  
  private static String[] getResultFolders(String projName) {
    File f = new File(ATGlobal.getRESULTSrootroot(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName)));
    
    File[] resultFolders = f.listFiles(path -> {return path.isDirectory() && path.getName().contains(".");});
    if (resultFolders==null) return new String[]{};
    
    String[] result = new String[resultFolders.length];
    for (int i = 0; i < resultFolders.length; i++) 
     result[i]=resultFolders[i].getName();
    return result;
  }

  /**
   * Returns all files that contain results for given (proj, alg, tst, mtype).
   * First file is always the default file, other files are files form other 
   * folders (e.g. F0.C1, F5.C2, ...)
   */
  private static ArrayList<String> getAllResultFiles(Project projekt, String algName, String tstName, String mType, String[] compIDs) {
    String defaultResultFile = ATTools.getTaskResultFileName(projekt, algName, tstName, mType);
    
    ArrayList<String> files = new ArrayList<>();
    files.add(defaultResultFile);
    
    String projRoot = projekt.getProjectRoot(); MeasurementType mt = MeasurementType.mtOf(mType);
    for (String compID : compIDs) {
      String folder = ATGlobal.getRESULTSroot(projRoot, compID);
      if (defaultResultFile.startsWith(folder)) continue;
      files.add(ATGlobal.getRESULTfilename(projRoot, algName, tstName, mt, compID));
    }
    return files;
  }
  
  /**
   * filename: /algator_root/data_root/projects/PROJ-X/results/FX.Cx/resfile.mtype)
   * result: FX
   * @return 
   */
  private static String getFamilyFromFilename(String filename) {
    try {
      Path resFileFolder = Paths.get(filename).getParent();
      String famComp   = resFileFolder.getName(resFileFolder.getNameCount()-1).toString();
    
      String[]      fc = famComp.split("[.]");
      return fc[0];
    } catch (Exception e) {
      return "";
    }
  }
  /**
   * filename: /algator_root/data_root/projects/PROJ-X/results/FX.Cx/resfile.mtype)
   * result: CX
   */
  private static String getComputerFromFilename(String filename) {
    try {
      Path resFileFolder = Paths.get(filename).getParent();
      String famComp   = resFileFolder.getName(resFileFolder.getNameCount()-1).toString();
    
      String[] fc = famComp.split("[.]");
      return fc.length > 1 ? fc[1] : "";
    } catch (Exception e) {
      return "";
    }
  }
  
  /**
   * Gets the filename (i.e. /algator_root/data_root/projects/PROJ-X/results/resfile.mtype) and
   * appends to given JSON object the following properties; 
   *   Fmy (familyID) Cmp (computerID),  CID (ComputerUID),  RS (ResultStatus), FS (FileStatus)
   */
  private static void resultFileStatus(JSONObject resTaskStatus, String fileName, Project projekt, String algName, String tstName, String mtype, int eNI) {                  
    // get family and computer name of result
    String comp      = getComputerFromFilename(fileName);
    String family    = getFamilyFromFilename(fileName);
    
    boolean uptodate = ATTools.resultsAreUpToDate(projekt, algName, tstName, mtype, fileName);
    int numberOfCompletedTests = ATTools.getNumberOfTests(fileName);   
    boolean complete = numberOfCompletedTests == eNI;	              
                      
    // Result status: 0 ... result file does not exist, 
    //                1 ...  some, but not all results exist
    //                2 ... all results exist, but they are outdated 
    //                4 ... all results exist and they are up-to-date
    int rs = 0;
    if (numberOfCompletedTests > 0 && numberOfCompletedTests < eNI) rs = 1;
    if (complete && !uptodate) rs = 2;
    if (complete && uptodate)  rs = 3;
    
    String fs  = String.format("(%d/%d)", numberOfCompletedTests, eNI);
    String cid = getComputerUID(family, comp);    

    resTaskStatus.put("Fmy", family);
    resTaskStatus.put("Cmp", comp);
    resTaskStatus.put("CID", cid);
    resTaskStatus.put("RS", rs);
    resTaskStatus.put("FS", fs);
  }
  
  /**
   * Appends to given JSON the following properties:  
   *   TS (task status: "PENDING" or IN "PROGRESS (x/y)"), TID (task id number)
   */
  private static void jsonTaskStatus(JSONObject resTaskStatus, ASTask task, int eNI) {
    String ts = ""; int tId = 0;
    if (task != null) {
      String taskStatus = task.getTaskStatus().toString();
    
      ts = task==null  ? "" : !task.getTaskStatus().equals(ASTaskStatus.INPROGRESS) ? taskStatus :
           String.format("%s (%d/%d)", taskStatus, task.getProgress(), eNI);    
    }
    resTaskStatus.put("TS", ts);
    resTaskStatus.put("TID", tId);
  }

  /**
   * Returns the status of projects results
   */
  public static JSONObject getResultStatus(String projName, String mType) {
      Project projekt = new Project(ATGlobal.getALGatorDataRoot(), projName);
      ArrayList<EAlgorithm>  eAlgs = new ArrayList(projekt.getAlgorithms().values());
      ArrayList<ETestSet> eTests = new ArrayList(projekt.getTestSets().values());

      // IDs of comupters that contributed results for this project
      String[] compIDs = getResultFolders(projName);
      
      String defaultProjectFamily = projekt.getEProject().getProjectFamily(mType);
      
      JSONArray results = new JSONArray();      
      String[] mTypes = mType.isEmpty() ? (new String[] {"em", "cnt", "jvm"}) : (new String[] {mType});
      for (EAlgorithm eAlg : eAlgs) {      
        for (ETestSet eTestSet : eTests) {
          for (String mtype : mTypes) {            
            int expectedNumberOfInstances = eTestSet.getFieldAsInt(ETestSet.ID_N, 0);            
                
            // all tasks for the triple (alg-tst-mtype) ...
            ArrayList<ASTask> tasks = getTasks(activeTasks, projName, eAlg.getName(), eTestSet.getName(), mtype);
            // ... and the default one (the one with empty or defaultFamily) 

            ArrayList<String> resultFiles = getAllResultFiles(projekt, eAlg.getName(), eTestSet.getName(), mtype, compIDs);
            HashMap<String, JSONArray> familyResults = new HashMap<>();      
            for (String resultFile : resultFiles) {
              if (resultFile.isEmpty()) continue;
              
              String family    = getFamilyFromFilename  (resultFile);
              String comp      = getComputerFromFilename(resultFile);
              
              // find the corresponding task
              ASTask task = null;
              for (ASTask cTask : tasks) {
                if (comp.equals(getComputerID(cTask.getComputerUID()))) {
                  task = cTask; break;
                }
              }
                            
              JSONObject resTaskStatus = new JSONObject();
              resultFileStatus(resTaskStatus, resultFile, projekt, eAlg.getName(), eTestSet.getName(), mType, expectedNumberOfInstances);
              jsonTaskStatus(resTaskStatus, task, expectedNumberOfInstances);
                            
              JSONArray fcArray = familyResults.get(family);
              if (fcArray == null) {fcArray = new JSONArray(); familyResults.put(family, fcArray);}
              
	      fcArray.put(resTaskStatus);
            }
            JSONObject resByFamiles = new JSONObject();
            for (String famKey : familyResults.keySet()) {
              resByFamiles.put(famKey, familyResults.get(famKey));
            }
              
            // default task and default result file status
            ASTask defaultTask = null;
            for (ASTask cTask : tasks) {
              String cFamily = cTask.getFamily();
              if (cFamily.equals(defaultProjectFamily)) {
                defaultTask = cTask; break;
              }
              if (cFamily.isEmpty()) defaultTask = cTask;
            }            
            String defFileName = ATTools.getTaskResultFileName(projekt, eAlg.getName(), eTestSet.getName(), mType);
            JSONObject defResTaskStatus = new JSONObject();
            resultFileStatus(defResTaskStatus, defFileName, projekt, eAlg.getName(), eTestSet.getName(), mType, expectedNumberOfInstances);            
            jsonTaskStatus(defResTaskStatus, defaultTask, expectedNumberOfInstances);
            defResTaskStatus.put("DF", defaultProjectFamily);

            
            JSONObject jObj = new JSONObject(String.format("{'Algorithm':'%s', 'TestSet':'%s', 'MType':'%s', 'Status':%s}", eAlg.getName(), eTestSet.getName(), mtype,  resByFamiles));
            jObj.put("DFTS", defResTaskStatus);
	    results.put(jObj);
	  }
        }
      }
      
      
      JSONObject result = new JSONObject();
      result.put("Results", results);
      result.put("MType", mTypes);
      result.put("Project", projName);
      result.put("AnswerID", UniqueIDGenerator.getNextID());
      result.put("Timestamp", System.currentTimeMillis());
             
      return result;  
  }   
  
  public static String saveJSONProperties(String filename, String entity, String[] properties, JSONObject newValues) {
    for (String property : properties) {
      if (!newValues.has(property))
        return String.format("2:Missing property '%s'.", property);
    }
    try {
      String jsonString = Files.lines(Paths.get(filename)).collect(Collectors.joining("\n"));
      JSONObject json = new JSONObject(jsonString);
      
      if (!json.has(entity) || !(json.get(entity) instanceof JSONObject))
        return String.format("3:Can't find '%s' in '%s'.", entity, filename);
      
      for (String property : properties) {
        ((JSONObject)json.get(entity)).put(property, newValues.get(property));
      }
      PrintWriter pw = new PrintWriter(new File(filename));
        pw.println(json.toString(2));
      pw.close();          
    } catch (Exception e) {
      return "1:"+e;
    }
    return "0:Properties changed.";
  }

  
  public static String saveHTMLToFile(String projectName, JSONObject data) {
    try {
      String filename;
      String type = data.optString("Type", "");
      switch (type) {
        case "problemDescription": filename="project.html"; break;
        case "testCases":          filename="testcase.html"; break;
        case "testSets":           filename="testset.html"; break;
        case "projDescAlgorithms": filename="algorithm.html"; break;
        case "references":         filename="references.html"; break;
        default:
          return "2:Invalid file type.";
      }
      String path = ATGlobal.getPROJECTdoc(ATGlobal.getALGatorDataRoot(), projectName);
      File htmlFile = new File(path, filename);
      String content = new String(Base64.getDecoder().decode(data.optString("Content", "")));
      FileUtils.writeStringToFile(htmlFile, content, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "1:"+e;
    }
    return "0:HTML description saved.";
  }

  
  public static String replaceJSONArrayElement(String filename, String entity, String arrayName, String elementId, String elementIdValue,  JSONObject newElementValue) {
    if (!newElementValue.has(elementId))
      return String.format("2:Missing elementID '%s' in newValue.", elementId);
    
    try {
      String jsonString = Files.lines(Paths.get(filename)).collect(Collectors.joining("\n"));
      JSONObject json = new JSONObject(jsonString);
      
      if (!json.has(entity) || !(json.get(entity) instanceof JSONObject))
        return String.format("3:Can't find '%s' in '%s'.", entity, filename);

      
      JSONObject eJson = json.getJSONObject(entity);
      
      if (!eJson.has(arrayName) || !(eJson.get(arrayName) instanceof JSONArray))
        return String.format("3:Array '%s' does not exist in '%s'.", arrayName, filename);
      
      boolean exists = false;
      JSONArray eltArray = (JSONArray)(eJson.get(arrayName));
      for (int i = 0; i < eltArray.length(); i++) {
        if (!(eltArray.get(i) instanceof JSONObject)) continue;
        Object idValue = ((JSONObject)eltArray.get(i)).get(elementId);
        if (elementIdValue.equals(idValue)) {
          exists = true;
          eltArray.put(i, newElementValue);
        }
      }
      if (!exists) 
        return String.format("4:No element with '%s'='%s' in %s.", elementId, elementIdValue, arrayName);

      PrintWriter pw = new PrintWriter(new File(filename));
        pw.println(json.toString(2));
      pw.close();          
    } catch (Exception e) {
      return "1:"+e;
    }
    return "0:Element replaced.";
  }
  
  public static String uploadStatic(JSONObject jObj) {
    try {
      String projectName = jObj.optString("ProjectName", "");
      String fileName    = jObj.optString("FileName", "");
      if (projectName.isEmpty() || fileName.isEmpty()) 
        return sAnswer(1, "Upload error", "Unknown project or file name.");
      
      byte[] content  = Base64.getDecoder().decode(jObj.optString("FileContent", ""));
      File savePath = new File(ATGlobal.getPROJECTResourcesFolder(projectName));
      if (!savePath.exists()) savePath.mkdirs();
      
      try (FileOutputStream fos = new FileOutputStream(new File(savePath, fileName));) {
        fos.write(content);
      } catch (Exception e) {
        return sAnswer(2, "Upload error", e.toString());
      }
      return sAnswer(OK_STATUS, "File uploaded successfully",  fileName);             
    } catch (Exception e) {
        return sAnswer(3, "Upload error",  e.toString());
    }
  }
  
  public static void main(String[] args) {
    System.out.println(getProjectData("u_42", "BasicSort"));
  }
}
