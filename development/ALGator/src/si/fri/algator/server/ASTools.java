package si.fri.algator.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.admin.Maintenance;
import si.fri.algator.admin.Tools;
import si.fri.algator.ausers.AUsersTools;
import si.fri.algator.ausers.CanUtil;
import static si.fri.algator.ausers.CanUtil.accessDeniedString;
import si.fri.algator.ausers.EntitiesDAO;
import si.fri.algator.ausers.dto.DTOEntity;

import si.fri.algator.entities.CompCap;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EComputer;
import si.fri.algator.entities.EGenerator;
import si.fri.algator.entities.EPresenter;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestCase;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.tools.SortedArray;
import si.fri.algator.entities.Entity;
import si.fri.algator.execute.AbstractTestCase;
import si.fri.algator.tools.ATTools;
import static si.fri.algator.tools.ATTools.getTaskResultFileName;
import si.fri.algator.tools.UniqueIDGenerator;
import spark.Request;

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

  // results sent to ashell
  public static ConcurrentHashMap<String, JSONObject> statusResults = new ConcurrentHashMap<>();
  
  // results sent to webpage; used to calculate difference so that update can only send changes
  public static final ConcurrentHashMap<String, JSONObject> awResults = new ConcurrentHashMap<>();

  
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
  }
  
  public static final String COMMON_TST_FILES = "testsets_common";

  public static final int OK_STATUS = 0;
  public static final String ID_STATUS = "Status";
  public static final String ID_MSG = "Message";
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
  public static String sAnswer(int status, String msg, String answer) {
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

  /**
   * ** Supporting methods for getData request
   */
  /**
   * Returns a list of all "visible" projects.
   */
  public static String getProjectsData(String uid) {
    String projectsRoot = ATGlobal.getPROJECTSfolder(ATGlobal.getALGatorDataRoot());
    String[] projects = new File(projectsRoot).list((dir, name) -> {
      File proj = new File(dir, name);
      return proj.isDirectory()
              && name.startsWith(String.format(ATGlobal.ATDIR_projRootDir, ""))
              && new File(new File(proj, ATGlobal.ATDIR_projConfDir), ATGlobal.getPROJECTConfigName()).exists();
    });

    // TODO:  filter-out projects according to user rights 
    ArrayList<String> readableProjects = new ArrayList();
    for (String project : projects) {
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(
              ATGlobal.getALGatorDataRoot(), project.substring(5))));
      String eid = eProject.getString(Entity.ID_EID);
      if (CanUtil.can(uid, eid, "can_read")) {
        readableProjects.add(eProject.getName());
      }
    }
    return jAnswer(OK_STATUS, "Projects",
            new JSONArray(readableProjects).toString()
    );
  }

  public static JSONObject getProjectJSON(String uid, String projectName) {
    EProject project = EProject.getProject(projectName);

    JSONObject result = new JSONObject(project.toJSONString());
    if (!CanUtil.can(uid, result.optString("eid", ""), "can_read")) {
      return new JSONObject();
    }

    String data_root = ATGlobal.getALGatorDataRoot();
    String proot = ATGlobal.getPROJECTroot(data_root, projectName);

    if (result.optString(EProject.ID_ShortTitle, "").isEmpty())
      result.put(EProject.ID_ShortTitle, projectName);
    
    // remove "non-existing" or "non-visible" algorithms
    JSONArray algs = result.optJSONArray(EProject.ID_Algorithms);
    if (algs != null) {
      for (int i = algs.length() - 1; i >= 0; i--) {
        if (ATGlobal.algorithmExists(data_root, projectName, algs.getString(i))) {
          String aeid = EAlgorithm.getAlgorithm(projectName, algs.getString(i)).getEID();
          if (!CanUtil.can(uid, aeid, "can_read")) {
            algs.remove(i);
          }
        } else {
          algs.remove(i);
        }
      }
    }
    // remove "non-existing" or "non-visible" testsets
    JSONArray tsts = result.optJSONArray(EProject.ID_TestSets);
    if (tsts != null) {
      for (int i = tsts.length() - 1; i >= 0; i--) {
        if (ATGlobal.testsetExists(data_root, projectName, tsts.getString(i))) {
          String teid = ETestSet.getTestset(projectName, tsts.getString(i)).getEID();
          if (!CanUtil.can(uid, teid, "can_read")) {
            tsts.remove(i);
          }
        } else {
          tsts.remove(i);
        }
      }
    }
    // remove "non-existing" or "non-visible" presenters
    JSONArray prts = result.optJSONArray(EProject.ID_ProjPresenters);
    if (prts != null) {
      for (int i = prts.length() - 1; i >= 0; i--) {
        if (ATGlobal.presenterExists(data_root, projectName, prts.getString(i))) {
          String peid = EPresenter.getPresenter(projectName, prts.getString(i)).getEID();
          if (!CanUtil.can(uid, peid, "can_read")) {
            prts.remove(i);
          }
        } else {
          prts.remove(i);
        }
      }
    }
    
    // set of computers that executed algorithms of this project
    TreeSet<String> registeredComputers = new TreeSet();    
    for (EComputer computer : EAlgatorConfig.getConfig().getComputers()) 
      registeredComputers.add(computer.getFCName());
    String resultsPath = ATGlobal.getRESULTSrootroot(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName));
    JSONArray usedComputers = new JSONArray();
    
    String[] comps     = new File(resultsPath).list();
    if (comps != null) for (String comp : comps) 
      if (registeredComputers.contains(comp)) usedComputers.put(comp);
    
    result.put("Computers", usedComputers);

    return result;
  }

  public static String getProjectData(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    JSONObject result = getProjectJSON(uid, projectName);

    return jAnswer(OK_STATUS, String.format("Project '%s'.", projectName), result.toString());
  }

  public static String getProjectDescription(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    EProject project = EProject.getProject(projectName);
    String eid = project.getEID();
    if (!CanUtil.can(uid, eid, "can_read")) {
      return sAnswer(99, "getProjectDescription: " + accessDeniedString, accessDeniedString);
    }

    long lastModified = project.getLastModified(projectName, "Project");
    File pFile = new File(ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName));
    lastModified = Math.max(lastModified, pFile.lastModified()/1000);
    
    DTOEntity entity = EntitiesDAO.getEntity(eid);
    String owner = entity==null ? "" : entity.getOwner();
    String user = owner.isEmpty() ? "" : AUsersTools.getUser(owner).getUsername();

    int numberOfA = project.getStringArray(EProject.ID_Algorithms).length;
    int numberOfT = project.getStringArray(EProject.ID_TestSets).length;
    
    // redefine !!!!
    int popularity = numberOfT + numberOfA;
    
    JSONObject ansObj = new JSONObject();
    ansObj.put("eid", eid);
    ansObj.put("De", project.getString(EProject.ID_Description));
    ansObj.put("St", project.getString(EProject.ID_ShortTitle));

    ansObj.put("Tg", project.getStringArray(EProject.ID_Tags));
    ansObj.put("Na", numberOfA);
    ansObj.put("Nt", numberOfT);
    ansObj.put("Mo", lastModified);
    ansObj.put("Po", popularity);
    ansObj.put("Ow", owner);
    ansObj.put("On", user);
    
    return jAnswer(OK_STATUS, String.format("Project description for '%s'.", projectName), ansObj.toString());
  }

  public static String getAlgorithmData(String uid, String projectName, String algorithmName, boolean deep) {
    String aeid = EAlgorithm.getAlgorithm(projectName, algorithmName).getEID();
    if (!CanUtil.can(uid, aeid, "can_read")) {
      return sAnswer(1, String.format("Algorithm '%s' - access denied.", algorithmName), CanUtil.accessDeniedString);
    }

    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }
    if (!ATGlobal.algorithmExists(ATGlobal.getALGatorDataRoot(), projectName, algorithmName)) {
      return sAnswer(3, String.format("Algorithm '%s' does not exist in project '%s'.", algorithmName, projectName), "");
    }

    EAlgorithm algorithm = EAlgorithm.getAlgorithm(projectName, algorithmName);

    JSONObject algorithmData = new JSONObject();
    algorithmData.put("Properties", new JSONObject(algorithm.toJSONString()));
    if (deep) {
      String sourceFileName = ATGlobal.getALGORITHMsrc(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
      String content = "";
      try {
        content = new String(Files.readAllBytes(Paths.get(sourceFileName + File.separator + "Algorithm.java")));
      } catch (Exception e) {
      }
      algorithmData.put("FileContent", content);

      String htmlFileName = ATGlobal.getALGORITHMHtmlName(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
      String htmlContent = "";
      try {
        htmlContent = new String(Files.readAllBytes(Paths.get(htmlFileName)));
      } catch (Exception e) {
      }
      algorithmData.put("HtmlFileContent", htmlContent);
    }

    return jAnswer(OK_STATUS, String.format("Algorithm '%s'.", algorithmName), algorithmData.toString());
  }

  public static String getTestsetData(String uid, String projectName, String testsetName, boolean deep) {
    String teid = ETestSet.getTestset(projectName, testsetName).getEID();
    if (!CanUtil.can(uid, teid, "can_read")) {
      return sAnswer(1, String.format("Testset '%s' - access denied.", testsetName), CanUtil.accessDeniedString);
    }

    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }
    if (!ATGlobal.testsetExists(ATGlobal.getALGatorDataRoot(), projectName, testsetName)) {
      return sAnswer(3, String.format("Testset '%s' does not exist in project '%s'.", testsetName, projectName), "");
    }

    ETestSet testset = ETestSet.getTestset(projectName, testsetName);
    testset.setName(testsetName);

    JSONObject testsetData = new JSONObject();
    testsetData.put("Properties", new JSONObject(testset.toJSONString()));
    if (deep) {
      String dataFileName = ATGlobal.getTESTSETDATAfilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
      String content = ""; try {
        content = new String(Files.readAllBytes(Paths.get(dataFileName)));
      } catch (Exception e) {}
      testsetData.put("FileContent", content);
      
      JSONObject result = getTestSetFilesList(projectName, testsetName);
      testsetData.put("FilesList", result);
    }

    return jAnswer(OK_STATUS, String.format("Testset '%s'.", testsetName), testsetData.toString());
  }

  public static JSONObject getTestSetFilesList(String projectName, String testsetName) {
    String tsfileName = ATGlobal.getTESTSETRecourcesFilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
    File tsFolder = new File(tsfileName);
    JSONObject result = new JSONObject();
    if (tsFolder.exists()) {
      String[] files = tsFolder.list();
      for (String file : files) {
        File tsFile = new File(tsFolder, file);
        if (tsFile.isFile()) {
          result.put(file, tsFile.length());
        }
      }
    }
    return result;
  }
  
  public static String getTestsetFiles(String uid, String projectName, String testsetName) {
    String teid = ETestSet.getTestset(projectName, testsetName).getEID();
    if (!CanUtil.can(uid, teid, "can_read")) {
      return sAnswer(1, String.format("TestsetFiles '%s' - access denied.", testsetName), CanUtil.accessDeniedString);
    }

    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }
    if (!ATGlobal.testsetExists(ATGlobal.getALGatorDataRoot(), projectName, testsetName)) {
      return sAnswer(3, String.format("Testset '%s' does not exist in project '%s'.", testsetName, projectName), "");
    }
    JSONObject result = getTestSetFilesList(projectName, testsetName);
    return jAnswer(OK_STATUS, String.format("Testset '%s' files", testsetName), result.toString());
  }

  // general testset files (files in folder Testset__files) 
  public static String getTestsetsCommonFiles(String uid, String projectName) {
    String teid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, teid, "can_read")) {
      return sAnswer(1, String.format("TestsetFiles '%s' - access denied."), CanUtil.accessDeniedString);
    }
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }
    JSONObject result = getTestSetFilesList(projectName, ATGlobal.AT_DEFAULT_testsetres);
    return jAnswer(OK_STATUS, String.format("Testsets files"), result.toString());
  }

  
  public static String readFileAtMost64KB(File file) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
        byte[] buffer = new byte[65536]; // 64 KB buffer
        int bytesRead = fis.read(buffer);
        return new String(buffer, 0, bytesRead);
    }
  }
  
  public static String getTestsetFile(String uid, String projectName, String testsetName, String fileName) {
    String teid = ETestSet.getTestset(projectName, testsetName).getEID();
    if (!CanUtil.can(uid, teid, "can_read")) {
     return sAnswer(1, String.format("TestsetFiles '%s' - access denied.", testsetName), CanUtil.accessDeniedString);
    }

    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }
    if (!testsetName.equals(COMMON_TST_FILES) && !ATGlobal.testsetExists(ATGlobal.getALGatorDataRoot(), projectName, testsetName)) {
      return sAnswer(3, String.format("Testset '%s' does not exist in project '%s'.", testsetName, projectName), "");
    }
    
    File tsFolder = new File(ATGlobal.getTESTSETRecourcesFilename(
       ATGlobal.getALGatorDataRoot(), projectName, testsetName.equals(COMMON_TST_FILES) ? ATGlobal.AT_DEFAULT_testsetres : testsetName));
    File resFile = new File(tsFolder, fileName);
    
    if (!resFile.exists())
      return sAnswer(4, String.format("File '%s' does not exist.", fileName), "");

    try {
      String content = readFileAtMost64KB(resFile);
      content = Base64.getEncoder().encodeToString(content.getBytes());
      return sAnswer(OK_STATUS, String.format("File '%s'", fileName), content);
    } catch (Exception e) {
      return sAnswer(5, String.format("File '%s' reading error: %s",  e), "");    
    }
  }
  

  /*
    Reads only several fields (fds) from JSON file. 
  
    Example: input file:
  
    {
      "Presenter": {
        "Name": "mpPresenter2",
        "eid": "e_YahsrHUxgXGC",
        "Title": "All results"
      }
    }
  
    readFilteredJSON(file, ["Name", "eid"], "Presenter") returns
  
    {
      "Name": "mpPresenter2",
      "eid": "e_YahsrHUxgXGC",
    }
  
    If wrapper==null, input has no wrapper and readFilteredJSON reads 
    only fds fields from original JSON.
  */
  public static JSONObject readFilteredJSON(File pFile, String[] fds, String wrapper) throws IOException {
   try {
     String jsonText = FileUtils.readFileToString(pFile, "UTF-8");
  
      // parse JSON
      JSONObject readJSON = new JSONObject(jsonText);
      if (wrapper != null)
       readJSON = readJSON.getJSONObject(wrapper);
  
      // filtered object
      JSONObject filtered = new JSONObject();
      for (String key : fds) {
        if (readJSON.has(key)) {
          filtered.put(key, readJSON.get(key));
        }
      }
  
      return filtered;
   } catch (Exception e) {
     return new JSONObject();
   }  
}

  
  public static String getPresenter(String uid, String projectName, String presenterName, int deep) {
    String peid = EPresenter.getPresenter(projectName, presenterName).getEID();
    if (!CanUtil.can(uid, peid, "can_read")) {
      return sAnswer(1, String.format("Presenter '%s' - access denied.", presenterName), CanUtil.accessDeniedString);
    }

    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    EPresenter prs = EPresenter.getPresenter(projectName, presenterName);
    String fileName = ATGlobal.getPRESENTERFilename(ATGlobal.getALGatorDataRoot(), projectName, presenterName);

    File pFile = new File(fileName);
    if (!pFile.exists()) {
      return sAnswer(3, String.format("Presenter '%s' does not exist in project '%s'.", presenterName, projectName), "");
    }

    Set<String> nondeepFields = new HashSet<>(Arrays.asList(
     "Name", "Author", "Date", "Title", "ShortTitle", "Description", "LastModified", "Layout"));

    String presenterJSON = "{}";
    try {
      presenterJSON = FileUtils.readFileToString(pFile, "UTF-8");
      JSONObject jObj = new JSONObject(presenterJSON);
      jObj = (JSONObject) jObj.get("Presenter");
      jObj.put(Entity.ID_NAME, presenterName);
      jObj.put(Entity.ID_LAST_MODIFIED, prs.getLastModified(projectName, presenterName));
      
      // if not deep read of json file, return only nondeepFields fileds of jObj   
      if (deep==0) {
        JSONObject newJO = new JSONObject();
        for (String nondeepField : nondeepFields) {
          if (jObj.has(nondeepField)) newJO.put(nondeepField, jObj.get(nondeepField));
        }
        jObj = newJO;
      }
      
      presenterJSON = jObj.toString();
    } catch (Exception e) {}
    return jAnswer(OK_STATUS, String.format("Presenter '%s'.", presenterName), presenterJSON);
  }

  public static String getProjectSources(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_read")) {
      return sAnswer(99, "getProjectSources: " + accessDeniedString, accessDeniedString);
    }

    JSONObject result = new JSONObject();
    String projectSrc = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName));
    String[][] srcs = {{"Input", "Input.java"}, {"Output", "Output.java"}, {"Algorithm", "ProjectAbstractAlgorithm.java"}, {"Tools", "Tools.java"}};
    for (String[] src : srcs) {
      String fileCont = ASTools.getFileContent(new File(projectSrc, src[1]).toString());
      result.put(src[0], Base64.getEncoder().encodeToString(fileCont.getBytes()));
    }

    String[] files = new File(projectSrc).list();

    JSONObject indicators = new JSONObject();
    Pattern p = Pattern.compile(ATGlobal.INDICATOR_TEST_OFFSET + "(.*)[.]java");
    if (files != null) for (String file : files) {
      Matcher m = p.matcher(file);
      if (m.find()) { 
        String indName = m.group(1);
        String fileCont = ASTools.getFileContent(new File(projectSrc, file).toString());
        indicators.put(indName, Base64.getEncoder().encodeToString(fileCont.getBytes()));
      }
    }
    result.put("Indicators", indicators);

    JSONObject generators = new JSONObject();
    p = Pattern.compile("TestCaseGenerator_(.*)[.]java");
    if (files != null) for (String file : files) {
      Matcher m = p.matcher(file);
      if (m.find()) {
        String genName = m.group(1);
        String fileCont = ASTools.getFileContent(new File(projectSrc, file).toString());
        generators.put(genName, Base64.getEncoder().encodeToString(fileCont.getBytes()));
      }
    }
    result.put("Generators", generators);

    return jAnswer(OK_STATUS, String.format("Project '%s' sources.", projectName), result.toString());
  }

  public static String getProjectProps(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_read")) {
      return sAnswer(99, "getProjectProps: " + accessDeniedString, accessDeniedString);
    }

    Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);

    ArrayList<String> colors = new ArrayList<>();
    
    JSONObject result = new JSONObject();
    LinkedHashSet<String> algorithms = new LinkedHashSet();
    for (String algorithm : project.getAlgorithms().keySet()) {
      EAlgorithm eAlg = EAlgorithm.getAlgorithm(projectName, algorithm);
      String aeid = eAlg.getEID();
      if (CanUtil.can(uid, aeid, "can_read")) {
        algorithms.add(algorithm);
        colors.add(eAlg.getString(EAlgorithm.ID_Color, "-1"));
      }
    }
    result.put("Algorithms", algorithms);
    result.put("AlgorithmColors", colors);

    LinkedHashSet<String> testsets = new LinkedHashSet();
    for (String testset : project.getTestSets().keySet()) {
      String teid = ETestSet.getTestset(projectName, testset).getEID();
      if (CanUtil.can(uid, teid, "can_read")) {
        testsets.add(testset);
      }
    }
    result.put("TestSets", testsets);

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
        // add default indicators for EM
        //if (mType.equals("em")) for (String defInd : EResult.defaultIndicators) 
        //  jInd.put(defInd, new EVariable(defInd, "").toJSON(false));
        
        result.put(mType.toUpperCase() + " indicators", jInd);
      } catch (Exception e) {
      }
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

  public static String getProjectDocs(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_read")) {
      return sAnswer(99, "getProjectDocs: " + accessDeniedString, accessDeniedString);
    }

    JSONObject result = new JSONObject();
    String projectDoc = ATGlobal.getPROJECTdoc(ATGlobal.getALGatorDataRoot(), projectName);
    String[][] docs = {{"Project", "project.html"}, {"Algorithm", "algorithm.html"}, {"References", "references.html"}, {"TestCase", "testcase.html"}, {"TestSet", "testset.html"}};
    for (String[] doc : docs) {
      String fileCont = ASTools.getFileContent(new File(projectDoc, doc[1]).toString());
      result.put(doc[0], Base64.getEncoder().encodeToString(fileCont.getBytes()));
    }

    JSONObject resources = new JSONObject();
    String resDir = ATGlobal.getPROJECTResourcesFolder(projectName);
    File resFile = new File(resDir);
    if (resFile.exists() && resFile.isDirectory()) {
      for (String file : resFile.list()) {
        if ((new File(file)).isDirectory()) {
          continue;
        }
        resources.put(file, new File(projectDoc, file).lastModified());
      }
    }
    result.put("Resources", resources);

    return jAnswer(OK_STATUS, String.format("Project '%s' docs.", projectName), result.toString());
  }

  public static String getProjectResource(String uid, String projectName, String resource) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_read")) {
      return sAnswer(99, "getProjectResource: " + accessDeniedString, accessDeniedString);
    }

    String resDir = ATGlobal.getPROJECTResourcesFolder(projectName);
    File resFile = new File(resDir, resource);

    if (!resFile.exists()) {
      return sAnswer(3, String.format("Resource '%s' of project '%s' does not exist.", resource, projectName), "");
    }

    String resourceCont = Tools.encodeFileToBase64Binary(resFile.toString());
    return sAnswer(OK_STATUS, String.format("Project resource '%s'.", resource), resourceCont);
  }
  
  
  private static boolean arrayContains(JSONArray jsonArray, String element) {
    for (int i = 0; i < jsonArray.length(); i++) 
      if (jsonArray.getString(i).equals(element)) return true;
    return false;  
  }
  
  // what can be either "All" or array of entities, like: ["project", "algorithm:BubbleSort", "testsets"]  
  // (meaning: give me lastModified date of project, algorithm Bubblesort and all testsets) 
  public static String getLastModified(String uid, String projectName, Object what) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    EProject project  = EProject.getProject(projectName);
    String peid       = project.getEID();
    if (!CanUtil.can(uid, peid, "can_read")) {
      return sAnswer(99, "getProjectResource: " + accessDeniedString, accessDeniedString);
    }
    
    boolean all = "All".equals(what);
    JSONArray whatA = (all || !(what instanceof JSONArray)) ? new JSONArray() : (JSONArray) what;
    
    JSONObject result = new JSONObject();
    JSONObject algorithms = new JSONObject(); 
    JSONObject testsets   = new JSONObject(); 
    JSONObject presenters = new JSONObject(); 
    
    if (all || arrayContains(whatA, "project"))
      result.put("project", project.getLastModified(projectName, ""));
    
    if (all || arrayContains(whatA, "algorithms")) 
      for (String alg: project.getStringArray(EProject.ID_Algorithms)) {        
        EAlgorithm eAlg = EAlgorithm.getAlgorithm(projectName, alg);
        if (CanUtil.can(uid, eAlg.getEID(), "can_read"))
          algorithms.put(alg, eAlg.getLastModified(projectName, alg));
      }
    
    if (all || arrayContains(whatA, "testsets")) 
      for (String tst: project.getStringArray(EProject.ID_TestSets)) {
        ETestSet eTst = ETestSet.getTestset(projectName, tst);
        if (CanUtil.can(uid, eTst.getEID(), "can_read"))
          testsets.put(tst, eTst.getLastModified(projectName, tst));
      }
    if (all || arrayContains(whatA, "presenters")) 
      for (String prs: project.getStringArray(EProject.ID_ProjPresenters)) {
        EPresenter ePrs = EPresenter.getPresenter(projectName, prs);
        if (CanUtil.can(uid, ePrs.getEID(), "can_read"))
          presenters.put(prs, ePrs.getLastModified(projectName,prs));
      }
    
    for (int i = 0; i < whatA.length(); i++) {
      String wht = (String) whatA.get(i);
      String[] tokens = wht.split(":");
      if (tokens.length==2) {
        switch (tokens[0]) {
          case "algorithm"  : 
            EAlgorithm eAlg = EAlgorithm.getAlgorithm(projectName, tokens[1]);
            if (CanUtil.can(uid, eAlg.getEID(), "can_read"))
              algorithms.put(tokens[1], eAlg.getLastModified(projectName, tokens[1]));
            break;  
          case "testset"    : 
            ETestSet eTst = ETestSet.getTestset(projectName, tokens[1]);
            if (CanUtil.can(uid, eTst.getEID(), "can_read"))
              testsets.put(tokens[1], eTst.getLastModified(projectName, tokens[1]));            
          case "presenter"  : 
            EPresenter ePrs = EPresenter.getPresenter(projectName, tokens[1]);
            if (CanUtil.can(uid, ePrs.getEID(), "can_read"))
              presenters.put(tokens[1], ePrs.getLastModified(projectName, tokens[1]));                        
        }      
      }
    }
    if (algorithms.length() > 0) result.put("algorithms", algorithms);
    if (testsets.length()   > 0) result.put("testsets",   testsets);
    if (presenters.length() > 0) result.put("presenters", presenters);
    
    return jAnswer(OK_STATUS, "getLastModified results", result.toString());
  }
  
  /**
   * ** Supporting methods for getData request ... end
   */
  /**
   * Method gets the answer of an action in form "status:msg"; if status==0,
   * method returns msgOK and msg, else msgNOK
   *
   */
  private static String parsedAnswer(String result, String msgOK, String msgNOK) {
    int status = 0;
    int pos = result.indexOf(":");
    try {
      status = Integer.parseInt(result.substring(0, pos));
    } catch (Exception e) {
    }
    String msg = result.length() > 2 ? result.substring(pos + 1) : msgNOK;
    if (msg.startsWith("{")) {
      return jAnswer(status, status == 0 ? msgOK : msg, status == 0 ? msg : "{}");
    } else {
      return sAnswer(status, status == 0 ? msgOK : msg, status == 0 ? msg : "");
    }
  }

  /**
   * ** Supporting methods for alter request
   */
  public static String newProject(String uid, String projectName, String author, String date) {
    if (ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' already exists.", projectName), "");
    }

    if (!CanUtil.can(uid, "e0_P", "can_add_project")) {
      return sAnswer(99, "newProject: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.createProject(uid, projectName, author, date);
    return parsedAnswer(result, "Project created.", "Error creating project.");
  }

  public static String removeProject(String uid, String projectName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exists.", projectName), "");
    }

    EProject project = EProject.getProject(projectName);
    String eid = project.getEID();

    if (!CanUtil.can(uid, eid, "can_write")) {
      return sAnswer(99, "removeProject: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removeProject(projectName);
    return parsedAnswer(result, "Project removed.", "Error creating project.");
  }

  public static String saveProjectGeneral(String uid, String projectName, Object data) {
    if (!(data instanceof JSONObject)) {
      return sAnswer(1, "Expecting 'Data' to be an JSON object.", "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "saveProjectGeneral: " + accessDeniedString, accessDeniedString);
    }

    String projFilename = ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName);
    String result = saveJSONProperties(
            projFilename, "Project", new String[]{"ShortTitle", "Description", "Author", "Date", "ProjectJARs", "EMExecFamily", "Tags"}, (JSONObject) data);
    return parsedAnswer(result, "Project properties saved.", "Error saving project properties.");
  }

  // zip file should contain entire project (folder PROJ-<projectName>)
  // if parameter projectName is empty, the <projectName> will be used
  // if project with given mane exists, this method will add 01, 02, 03, ... to get unexisting project
  public static String importProject(String uid, String zipFileName, String projectName, boolean deleteZipFileFolder) {
    if (!CanUtil.can(uid, "e0_P", "can_import_project")) {
      return sAnswer(99, "Error importing project. ", accessDeniedString);
    } 
    String result = "";
    String tmpDir = ATGlobal.getTMPDir("unziped_projects");
    File   tmpPath = new File(tmpDir);    
    try {
      String error = ATTools.unzip(zipFileName, tmpDir);
      if (error.equals("OK")) {  
        String[] content = tmpPath.list();
        if (content.length == 1 && content[0].startsWith("PROJ-")) {
          String pName = content[0].substring(5);
          if (!projectName.isEmpty()) pName=projectName;
          
          // check if project with this name already exists
          int ext = 0;
          String impProjectName="";
          String projectRoot = "";
          while (ext < 99) {
            impProjectName = pName + (ext == 0 ? "" : String.format("%02d", ext));
            projectRoot    = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), impProjectName);
            if (!new File(projectRoot).exists()) break;
            ext++;
          }
          if (!impProjectName.isEmpty() && ext < 99) { // we have a new project name, project can be imported          
            String errorMsg = "";
            
            FileUtils.copyDirectory(new File(tmpPath, content[0]), new File(projectRoot));
            
            EProject project       = EProject.getProject(impProjectName);
            JSONObject projectJSON = new JSONObject(project.toJSONString());

            String projectEID = AUsersTools.getUniqueDBid("e_");
            
            project.set(Entity.ID_EID, projectEID);            
            project.saveEntity();                        
     
            String addToDBMsg=EntitiesDAO.addEntity(DTOEntity.ETYPE_Project, impProjectName, projectEID, uid,  "e0", true);
            if (addToDBMsg.startsWith("14:")) errorMsg = addToDBMsg;
            
            JSONArray algs = projectJSON.optJSONArray(EProject.ID_Algorithms);
            if (algs!=null) for (int i=0; i<algs.length(); i++) {
              String algName = algs.getString(i);
              String algEID  = AUsersTools.getUniqueDBid("e_");
              
              EAlgorithm eAlg = EAlgorithm.getAlgorithm(impProjectName, algName);
              eAlg.set(Entity.ID_EID, algEID);            
              eAlg.saveEntity();                        
              
              addToDBMsg=EntitiesDAO.addEntity(DTOEntity.ETYPE_Algorithm, algName, algEID, uid,  projectEID, true);
              if (addToDBMsg.startsWith("14:")) errorMsg += (errorMsg.isEmpty() ? "" : "; ") + addToDBMsg;          
            }

            JSONArray tsts = projectJSON.optJSONArray(EProject.ID_TestSets);
            if (tsts!=null) for (int i=0; i<tsts.length(); i++) {
              String tstName = tsts.getString(i);
              String tstEID  = AUsersTools.getUniqueDBid("e_");
              
              ETestSet eTst = ETestSet.getTestset(impProjectName, tstName);
              eTst.set(Entity.ID_EID, tstEID);            
              eTst.saveEntity();                        
              
              addToDBMsg=EntitiesDAO.addEntity(DTOEntity.ETYPE_Testset, tstName, tstEID, uid,  projectEID, true);
              if (addToDBMsg.startsWith("14:")) errorMsg += (errorMsg.isEmpty() ? "" : "; ") + addToDBMsg;          
            }

            JSONArray prss = projectJSON.optJSONArray(EProject.ID_ProjPresenters);
            if (prss!=null) for (int i=0; i<prss.length(); i++) {
              String prsName = prss.getString(i);
              String prsEID  = AUsersTools.getUniqueDBid("e_");
              
              String presenterFilename = ATGlobal.getPRESENTERFilename(ATGlobal.getALGatorDataRoot(), impProjectName, prsName);
              try {
                String fc = getFileContent(presenterFilename);
                JSONObject pObj = new JSONObject(fc);
                pObj.getJSONObject("Presenter").put(Entity.ID_EID, prsEID);
                writeFileContent(presenterFilename, pObj.toString(2));
              }catch (Exception e) {}
              
              addToDBMsg=EntitiesDAO.addEntity(DTOEntity.ETYPE_Presenter, prsName, prsEID, uid,  projectEID, true);
              if (addToDBMsg.startsWith("14:")) errorMsg += (errorMsg.isEmpty() ? "" : "; ") + addToDBMsg;          
            }
            
            if (errorMsg.isEmpty())
              result = sAnswer(OK_STATUS, "Import project.", String.format("Project '%s' imported successfully.", impProjectName));
            else {
              // TODO:
              // če sem prišel do sem, pomeni, da sem projekt razpakiral, presnel na novo 
              // lokacijo in v bazo vpisal nekaj entitet, potem ja pa prišlo do napake pri 
              // vnosu entitet v baZo. Sedaj (ker je prišlo do napake), bi
              // bilo treba vse to razveljaviti, da ne ostane nekaj na pol (pobriši nov direktorij 
              // in pobriši vse nove vnose v tabelo entitet)
              result = sAnswer(5, "Error importing project.", errorMsg);
            }
          } else 
            result = sAnswer(4, "Error importing project.", "Invalid project name.");
        } else
          result =  sAnswer(3, "Error importing project.", "The structure of the zip file in incorrect");
      } else 
        result =  sAnswer(2, "Error importing project.", error);
      
      // clean-up unziped files
      Tools.deleteDirectory(tmpPath);
      
      // clean-up upload folder
      if (deleteZipFileFolder) {
        File zipFile         = new File(zipFileName);
        File zipPath         = new File(zipFile.getParent());
        String zipFolderName = zipPath.getName();
        int numberOfFiles    = zipPath.list().length;
        
        // additional checking for safety
        if (zipFolderName.startsWith("tmp") && numberOfFiles == 1) 
          Tools.deleteDirectory(zipPath);
      }
    } catch (Exception e) {
      result =  sAnswer(1, "Error importing project.", e.toString());
    }
    return result;
  }
  
  
  // Data should contain information about html type and html content 
  // html type: html_desc, test_case_html_desc, test_sets_html_desc, algorithms_html_desc, project_ref_desc47 
  // html content: base64 encoded html file content
  public static String saveHTML(String uid, String projectName, Object data) {
    if (!(data instanceof JSONObject)) {
      return sAnswer(1, "Expecting 'Data' to be an JSON object.", "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "saveProjectGeneral: " + accessDeniedString, accessDeniedString);
    }

    String result = saveHTMLToFile(projectName, (JSONObject) data);
    return parsedAnswer(result, "Project properties saved.", "Error saving project properties.");
  }
  
  public static String removeJARFile(String uid, String projectName, String fileName) {
    String teid = EProject.getProject(projectName).getEID();
    
    if (!CanUtil.can(uid, teid, "can_write")) {
     return sAnswer(1, String.format("removeJARFile '%s' - access denied.", fileName), CanUtil.accessDeniedString);
    }
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    File jarFolder = new File(ATGlobal.getPROJECTlibPath(projectName));
    File jarFile   = new File(jarFolder, fileName);
    if (!jarFile.exists())
      return sAnswer(4, String.format("File '%s' does not exist.", fileName), "");

    if (jarFile.delete())
      return sAnswer(OK_STATUS, "Remove JAR file", String.format("File '%s' removed.", fileName));
    else
      return sAnswer(5, "Remove JAR file", String.format("Failed to remove file '%s'.", fileName));
  }
  

  public static String newPresenter(String uid, String projectName, int presenterType, String author, String date) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_add_presenter")) {
      return sAnswer(99, "newPresenter: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.createPresenter(uid, projectName, "", presenterType, author, date);
    return parsedAnswer(result, "Presenter created.", "Error creating presenter.");
  }

  public static String removePresenter(String uid, String projectName, String presenterName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EPresenter.getPresenter(projectName, presenterName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "removePresenter: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removePresenter(peid, projectName, presenterName);
    return parsedAnswer(result, "Presenter removed.", "Error removing presenter.");
  }

  public static String savePresenter(String uid, String projectName, String presenterName, JSONObject presenterData) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EPresenter.getPresenter(projectName, presenterName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "savePresenter: " + accessDeniedString, accessDeniedString);
    }

    String presenterFilename = ATGlobal.getPRESENTERFilename(ATGlobal.getALGatorDataRoot(), projectName, presenterName);
    try {
      JSONObject presenter = new JSONObject();
      presenter.put(EPresenter.ID_PresenterParameter, presenterData);

      PrintWriter pw = new PrintWriter(presenterFilename);
      pw.println(presenter.toString(2));
      pw.close();

      return sAnswer(OK_STATUS, "Presenter saved.", presenterName);
    } catch (Exception e) {
      return sAnswer(1, "Error saving presenter.", e.toString());
    }
  }

  public static String newParameter(String uid, String projectName, String parameterName, boolean isInput) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "newParameter: " + accessDeniedString, accessDeniedString);
    }

    String username = ""; // !!!
    String parDesc = String.format("{'Name':'%s', 'Description':'', 'Type':'string'}", parameterName);
    String result = Maintenance.addParameter(projectName, parDesc, String.format("{'isInputParameter':%s}", isInput ? "true" : "false"));
    return parsedAnswer(result, "Parameter added.", "Error adding parameter.");
  }

  public static String removeParameter(String uid, String projectName, String parameterName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "removeParameter: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removeParameter(projectName, parameterName);
    int status = 0;
    try {
      status = Integer.parseInt(result.substring(0, 1));
    } catch (Exception e) {
    }
    String msg = result.length() > 2 ? result.substring(2) : "Error removing parameter.";

    return sAnswer(status, status == 0 ? "Parameter removed." : msg, status == 0 ? msg : "");
  }

  // poskrbi, da bo tabela params vsebovala "name" natanko takrat, ko isInput==true
  static void ensureNamePresence(JSONArray params, String name, boolean isInput) {
    int index = -1;

    for (int i = 0; i < params.length(); i++) {
        if (name.equals(params.optString(i))) {
            index = i;
            break;
        }
    }
    if (isInput && index == -1) {
        params.put(name);
    } else if (!isInput && index != -1) {
        params.remove(index);
    }
}

  
  public static String saveParameter(String uid, String projectName, String parameterName, JSONObject parameter) {
    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "saveParameter: " + accessDeniedString, accessDeniedString);
    }

    try {
      String testcaseFilename = ATGlobal.getTESTCASEDESCfilename(ATGlobal.getALGatorDataRoot(), projectName);
    
      ETestCase tscd = ETestCase.getTestCaseDescription(projectName);
      boolean isInput = parameter.optBoolean("IsInput", false);
      // remove "isInput" from parameter description
      parameter.remove("IsInput");
      // create correct InputParameters array
      JSONArray iParams = tscd.getField(ETestCase.ID_inputParameters);
      ensureNamePresence(iParams, parameterName, isInput);
    
      JSONObject tmpJ = new JSONObject();
      tmpJ.put("InputParameters", iParams);
    
      String result = replaceJSONArrayElement(
            testcaseFilename, "TestCase", "Parameters", "Name", parameterName, parameter);
    
      result += ";" + saveJSONProperties(testcaseFilename, "TestCase", new String[]{"InputParameters"}, tmpJ);
    
      return parsedAnswer(result, "Parameter saved.", "Error saving parameter.");
    } catch (Exception e) {
      return sAnswer(2, "saveParameter error: " , e.toString());
    }
  }

  public static String newAlgorithm(String uid, String projectName, String algorithmName) {
    return newAlgorithm(uid, projectName, algorithmName, "", "");
  }

  public static String newAlgorithm(String uid, String projectName, String algorithmName, String author, String name) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_add_algorithm")) {
      return sAnswer(99, "newAlgorithm: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.createAlgorithm(uid, projectName, algorithmName, author, name);
    return parsedAnswer(result, "Algorithm added.", "Error adding algorithm.");
  }

  public static String saveAlgorithm(String uid, String projectName, String algorithmName, JSONObject algorithmData) {
    String aeid = EAlgorithm.getAlgorithm(projectName, algorithmName).getEID();
    if (!CanUtil.can(uid, aeid, "can_write")) {
      return sAnswer(99, "saveAlgorithm: " + accessDeniedString, accessDeniedString);
    }

    String algorithmFilename = ATGlobal.getALGORITHMfilename(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName), algorithmName);
    String result = saveJSONProperties(
            algorithmFilename, "Algorithm", new String[]{"Description", "ShortName", "Date", "Author", "Language", "Color"}, algorithmData.getJSONObject("Properties"));

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
    return parsedAnswer(result, "Algorithm properties saved." + fileSaved, "Error saving testset properties.");
  }

  public static String removeAlgorithm(String uid, String projectName, String algorithmName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String aeid = EAlgorithm.getAlgorithm(projectName, algorithmName).getEID();
    if (!CanUtil.can(uid, aeid, "can_write")) {
      return sAnswer(99, "removeAlgorithm: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removeAlgorithm(aeid, projectName, algorithmName);
    return parsedAnswer(result, "Algorithm removed.", "Error removing algorithm.");
  }

  public static String newTestset(String uid, String projectName, String testsetName, String author, String date) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_add_testset")) {
      return sAnswer(99, "newTestset: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.createTestset(uid, projectName, testsetName, author, date);
    return parsedAnswer(result, "Testset added.", "Error adding testset.");
  }

  public static String removeTestset(String uid, String projectName, String testsetName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String teid = ETestSet.getTestset(projectName, testsetName).getEID();
    if (!CanUtil.can(uid, teid, "can_write")) {
      return sAnswer(99, "removeTestset: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removeTestset(teid, projectName, testsetName);
    int status = 0;
    try {
      status = Integer.parseInt(result.substring(0, 1));
    } catch (Exception e) {
    }
    String msg = result.length() > 2 ? result.substring(2) : "Error removing testset.";

    return sAnswer(status, status == 0 ? "Testset removed." : msg, status == 0 ? msg : "");
  }

  public static String saveTestset(String uid, String projectName, String testsetName, JSONObject testsetData) {
    String teid = ETestSet.getTestset(projectName, testsetName).getEID();
    if (!CanUtil.can(uid, teid, "can_write")) {
      return sAnswer(99, "saveTestset: " + accessDeniedString, accessDeniedString);
    }

    String testsetFilename = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName);
    String result = saveJSONProperties(
            testsetFilename, "TestSet", new String[]{"Name", "ShortName", "Description", "N", "TestRepeat", "TimeLimit"}, testsetData.getJSONObject("Properties"));
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
    return parsedAnswer(result, "Testset properties saved." + fileSaved, "Error saving testset properties.");
  }

  public static String removeTestsetFile(String uid, String projectName, String testsetName, String fileName) {
    String teid = ""; // removing "common files" requires write access on project
    if (COMMON_TST_FILES.equals(testsetName)) {
      teid = EProject.getProject(projectName).getEID();
    } else 
      teid = ETestSet.getTestset(projectName, testsetName).getEID();
    
    if (!CanUtil.can(uid, teid, "can_write")) {
     return sAnswer(1, String.format("removeTestsetFile '%s' - access denied.", testsetName), CanUtil.accessDeniedString);
    }

    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }
    if (!COMMON_TST_FILES.equals(testsetName) && !ATGlobal.testsetExists(ATGlobal.getALGatorDataRoot(), projectName, testsetName)) {
      return sAnswer(3, String.format("Testset '%s' does not exist in project '%s'.", testsetName, projectName), "");
    }

    if (COMMON_TST_FILES.equals(testsetName)) testsetName = ATGlobal.AT_DEFAULT_testsetres; // common files are in folder "Testset__files"
    File tsFolder = new File(ATGlobal.getTESTSETRecourcesFilename(ATGlobal.getALGatorDataRoot(), projectName, testsetName));
    File resFile = new File(tsFolder, fileName);
    if (!resFile.exists())
      return sAnswer(4, String.format("File '%s' does not exist.", fileName), "");

    if (resFile.delete())
      return sAnswer(OK_STATUS, "Remove file", String.format("File '%s' removed.", fileName));
    else
      return sAnswer(5, "Remove file", String.format("Failed to remove file '%s'.", fileName));
  }
  
  
  public static String newIndicator(String uid, String projectName, String indicatorName, String indicatorType, JSONObject meta) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "newIndicator: " + accessDeniedString, accessDeniedString);
    }

    String indType = "indicator".equals(indicatorType) ? "int" : indicatorType;
    String indDesc = String.format("{'Name':'%s', 'Description':'', 'Type':'%s', 'Meta':%s}", indicatorName, indType, meta.toString());
    String result = Maintenance.addIndicator(projectName, indDesc, "{}");
    int status = 0;
    try {
      status = Integer.parseInt(result.substring(0, 1));
    } catch (Exception e) {
    }
    String msg = result.length() > 2 ? result.substring(2) : "Error adding indicator.";

    String code = ((status != 0) || (!"indicator".equals(indicatorType))) ? ""
            : ASTools.getFileContent(projectName, "proj/src/IndicatorTest_" + indicatorName + ".java", true);
    return sAnswer(status, status == 0 ? "Indicator added." : msg, code);
  }

  public static String removeIndicator(String uid, String projectName, String indicatorName, String type) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "removeIndicator: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removeIndicator(projectName, indicatorName, type);
    int status = 0;
    try {
      status = Integer.parseInt(result.substring(0, 1));
    } catch (Exception e) {
    }
    String msg = result.length() > 2 ? result.substring(2) : "Error removing indicator.";

    return sAnswer(status, status == 0 ? "Indicator removed." : msg, status == 0 ? msg : "");
  }

  public static String saveIndicator(String uid, String projectName, JSONObject indicator, String type) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "saveIndicator: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.saveIndicator(projectName, indicator, type);
    String tYpe = type.isEmpty() ? "Indicator" : (type.toUpperCase().charAt(0) + type.substring(1));
    return parsedAnswer(result, tYpe + " saved.", "Error saving " + type + ".");
  }

  public static String newGenerator(String uid, String projectName, String generatorName, JSONArray genParams) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "newGenerator: " + accessDeniedString, accessDeniedString);
    }

    List<String> list = new ArrayList<String>();
    for (int i = 0; i < genParams.length(); i++) {
      list.add(genParams.getString(i));
    }

    String result = Maintenance.addTestCaseGenerator(
            projectName, generatorName, list.toArray(new String[0]));

    return parsedAnswer(result, "Generator added.", "Error adding generator.");
  }

  public static String saveGenerator(String uid, String projectName, String generatorType, JSONObject generator, String code) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "saveGenerator: " + accessDeniedString, accessDeniedString);
    }

    String testcaseFilename = ATGlobal.getTESTCASEDESCfilename(ATGlobal.getALGatorDataRoot(), projectName);

    String result = replaceJSONArrayElement(
            testcaseFilename, "TestCase", "Generators", "Type", generatorType, generator);
    int status = 0;
    try {
      status = Integer.parseInt(result.substring(0, 1));
    } catch (Exception e) {
    }
    if (status != 0) {
      return sAnswer(5, "Error saving generator - element can not be replaced.", "");
    }

    String generatorFilename = new File(new File(new File(ATGlobal.ATDIR_projConfDir), ATGlobal.ATDIR_srcDir),
            ATGlobal.getGENERATORFilename(generatorType)).getPath();
    String decodecCode = new String(Base64.getDecoder().decode(code));
    result = saveFile(projectName, generatorFilename, decodecCode);

    return parsedAnswer(result, "Generator saved.", "Error saving generator.");
  }

  public static String removeGenerator(String uid, String projectName, String generatorName) {
    if (!ATGlobal.projectExists(ATGlobal.getALGatorDataRoot(), projectName)) {
      return sAnswer(2, String.format("Project '%s' does not exist.", projectName), "");
    }

    String peid = EProject.getProject(projectName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) {
      return sAnswer(99, "removeGenerator: " + accessDeniedString, accessDeniedString);
    }

    String result = Maintenance.removeGenerator(projectName, generatorName);
    int status = 0;
    try {
      status = Integer.parseInt(result.substring(0, 1));
    } catch (Exception e) {
    }
    String msg = result.length() > 2 ? result.substring(2) : "Error removing generator.";

    return sAnswer(status, status == 0 ? "Generator removed." : msg, status == 0 ? msg : "");
  }

  /**
   * ** Supporting methods for alter request ... end
   */
  /**
   * Method reads a file with tasks and returns a list.
   *
   * @param type 0 ... active tasks, 1 ... closed tasks, 2 ... archived tasks
   * @return
   */
  public static SortedArray<ASTask> readADETasks(int type) {
    SortedArray<ASTask> tasks = new SortedArray<>();
    File taskFile = new File(ASGlobal.getADETasklistFilename(type));
    if (taskFile.exists()) {
      try ( DataInputStream dis = new DataInputStream(new FileInputStream(taskFile));) {
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
   * From closedTasks remove all tasks that are more than numberOfDays old and
   * write them to archivedTasks file.
   */
  private static void removeAndArchiveOldTasks(SortedArray<ASTask> closedTasks, int numberOfDays) {
    ArrayList<ASTask> removedTasks = new ArrayList<>();

    long now = new Date().getTime();
    File archivedFile = new File(ASGlobal.getADETasklistFilename(2)); // archived tasks file
    try ( DataOutputStream dos = new DataOutputStream(new FileOutputStream(archivedFile));) {
      Iterator<ASTask> it = closedTasks.iterator();
      while (it.hasNext()) {
        ASTask task = it.next();
        if (now - task.getTaskStatusDate() > numberOfDays * 1000 * 60 * 60 * 24) {
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
    try ( DataOutputStream dos = new DataOutputStream(new FileOutputStream(taskFile));) {
      Iterator<ASTask> it = tasks.iterator();
      while (it.hasNext()) {
        dos.writeUTF(it.next().toJSONString());
      }
    } catch (Exception e) {
      // if error ocures, nothing can be done
    }
  }
  
  /**
   * From activeTasks remove all tasks that are CANCELED or INPROGRESS for more than 1 hour
   */
  public static int cleanActiveTaskQueue() {
    int activeTasksSize = activeTasks.size();
    
    activeTasks.removeIf(task -> {
      return ((ASTaskStatus.CANCELED.equals(((ASTask)task).getTaskStatus()) || ASTaskStatus.INPROGRESS.equals(((ASTask)task).getTaskStatus()) 
            && ((ASTask)task).sinceModified() > 60*60));
    });
    writeADETasks(activeTasks, 0);
    
    return activeTasksSize - activeTasks.size();
  }

  /**
   * Get the computerUID of a computer with given familyID and computerID
   */
  private static String getComputerUID(String famX, String compX) {
    ArrayList<EComputer> computers = EAlgatorConfig.getConfig().getComputers();
    for (EComputer computer : computers) {
      String fam = computer.getField(EComputer.ID_FamilyID);
      String com = computer.getField(EComputer.ID_ComputerID);
      if (famX.equals(fam) && compX.equals(com)) {
        return computer.getField(EComputer.ID_ComputerUID);
      }
    }
    return "";
  }

  /**
   * Among all the registered computers, find and return the one with a given
   * uid. If no computer has that uid, method returns null.
   */
  public static EComputer getComputer(String cuid) {
    // default computer (with uid=0) is F0.C0 (local computer)
    if ("0".equals(cuid)) {
      return new EComputer();
    }

    ArrayList<EComputer> computers = EAlgatorConfig.getConfig().getComputers();
    for (EComputer computer : computers) {
      if (cuid.equals(computer.getField(EComputer.ID_ComputerUID))) {
        return computer;
      }
    }
    return null;
  }

  /**
   * Among all the registered computers, find the one with a given uid and
   * return its ID. If no computer has that uid, method returns "/".
   */
  private static String getComputerID(String uid) {
    EComputer eC = getComputer(uid);
    return (eC != null && eC.getField(EComputer.ID_ComputerID) != null) ? eC.getField(EComputer.ID_ComputerID) : "/";
  }

  public static String familyOfComputer(String cuid) {
    EComputer comp = getComputer(cuid);
    if (comp != null) {
      return comp.getString(EComputer.ID_FamilyID);
    } else {
      return "?";
    }
  }

  public static String getFamilyAndComputerName(String uid) {
    String result = "unknown";
    try {
      EComputer comp = getComputer(uid);
      if (comp != null) {
        result = comp.getString(EComputer.ID_FamilyID) + "." + comp.getString(EComputer.ID_ComputerID);
      }
    } catch (Exception e) {
    }
    return result;
  }

  /**
   * Method finds the first (most appropriate) task for computer uid. Task is
   * appropriate if task's and computer's families match and if computers's
   * capabilities are sufficient. More appropritate tasks are queued before
   * (i.e. have smaller index in queue than) less appropriate ones.
   */
  public static ASTask findFirstTaskForComputer(SortedArray<ASTask> taskQueue, String cid, boolean allowInprogressTasks) {
    EComputer comp = getComputer(cid);
    if (comp == null || comp.getCapabilities() == null) {
      return null;
    }

    TreeSet<CompCap> cCapabilities = comp.getCapabilities();
    String cfamily = comp.getString(EComputer.ID_FamilyID);
    if (cfamily == null || cfamily.isEmpty()) {
      return null;
    }

    Set<ASTaskStatus> doableTasks = new HashSet<>(
      Arrays.asList(ASTaskStatus.INPROGRESS, ASTaskStatus.PENDING, ASTaskStatus.QUEUED)
    ); 
    
    for (ASTask task : taskQueue) {
      ASTaskStatus tStatus = task.getTaskStatus();
      if (!allowInprogressTasks && ASTaskStatus.INPROGRESS.equals(tStatus)) {
        continue;
      }

      if (!doableTasks.contains(tStatus)) { 
        continue;
      }

      // tasks with computer already assigned can only be executed by the same computer 
      String taskComputerID = task.getComputerUID();
      if (taskComputerID != null && !taskComputerID.equals(Entity.unknown_value) && !taskComputerID.isEmpty()) {
        if (cid.equals(taskComputerID)) {
          return task;
        } else {
          continue;
        }
      }

      String tFamily = task.getString(ASTask.ID_Family, "");
      if (tFamily == null) {
        tFamily = "";
      }
      String tmtype = task.getString(ASTask.ID_MType);
      if (tmtype == null || tmtype.isEmpty() || tmtype.equals(Entity.unknown_value)) {
        tmtype = "em";
      }
      CompCap requiredCapability = CompCap.capability(tmtype);

      if ((tFamily.isEmpty() || tFamily.equals(cfamily)) && cCapabilities.contains(requiredCapability)) {
        return task;
      }
    }
    return null;
  }

  public static String getResultFilename(ASTask task) {
    String projName       = (String) task.getField(ASTask.ID_Project);
    String algName        = (String) task.getField(ASTask.ID_Algorithm);
    String testsetName    = (String) task.getField(ASTask.ID_Testset);
    String compID         = getFamilyAndComputerName((String) task.getField(ASTask.ID_ComputerUID));
    MeasurementType mType = MeasurementType.mtOf((String) task.getField(ASTask.ID_MType));

    return ATGlobal.getRESULTfilename(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName),
            algName, testsetName, mType, compID
    );
  }
  
  // used to save results of misc tasks
  public static String getMiscResultFilename(ASTask task) {
    String projName = (String) task.getField(ASTask.ID_Project);
    String compID   = getFamilyAndComputerName((String) task.getField(ASTask.ID_ComputerUID));
    int taskID      = task.getField(ASTask.ID_EID);
    String taskType = task.getString(ASTask.ID_TaskType, "_");

    return ATGlobal.getRESULTMiscFilename(projName, compID, taskID+"", taskType);
  }
  
  public static void removeTaskResultFile(ASTask task) {
    String filename = getResultFilename(task);
    File   taskFile = new File(filename);
    if (taskFile.exists()) taskFile.delete();
  }
  

  /**
   * Sets the status of a task and writes this status to the task status file
   */
  public static String getTaskStatusFilename(ASTask task) {
    try {
      Object ttype = task.getField(ASTask.ID_TaskType);
      if (ttype == null || ASTask.ID_TaskType_Execute.equals(ttype)) {
        return ATGlobal.getTaskStatusFilename(
          (String) task.getField(ASTask.ID_Project), (String) task.getField(ASTask.ID_Algorithm),
          (String) task.getField(ASTask.ID_Testset), (String) task.getField(ASTask.ID_MType));        
      } else {
        String tid = task.getField(ASTask.ID_EID).toString();
        return ATGlobal.getNonexecTaskStatusFilename(
            (String) task.getField(ASTask.ID_Project), tid);
      }
    } catch (Exception e) {
      return "";
    }
  }

  public static void setTaskStatus(ASTask task, ASTaskStatus status, String msg, String computer) {
    task.setTaskStatus(status, computer, msg);

    if (computer == null) {
      computer = task.getField(ASTask.ID_ComputerUID);
    }
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
   * Writes the status of a task to the task status file. Computer name is valid
   * only when task status is "COMPLETED"
   */
  public static void logTaskStatus(ASTask task, ASTaskStatus status, String msg, String computer) {
    if (task == null) {
      return;
    }

    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    String idtFilename = getTaskStatusFilename(task);
    if (idtFilename.isEmpty()) return;

    String startDate = "", statusMsg = "", endDate = "";
    if (status.equals(ASTaskStatus.PENDING)) {
      startDate = date + ": Task pending";
    } else try ( Scanner sc = new Scanner(new File(idtFilename))) {
      startDate = sc.nextLine();
      statusMsg = sc.nextLine();
      endDate = sc.nextLine();
    } catch (Exception e) {
      // on error - use default values
    }

    if (startDate.isEmpty()) {
      startDate = date + ": Task executed by ALGator";
    }

    if (msg != null) {
      statusMsg = msg;
    }

    switch (status) {
      case INPROGRESS:
        statusMsg = date + ":INPROGRESS # " + computer + (statusMsg.isEmpty() ? "" : " # ") + statusMsg;
        break;
      case COMPLETED:
        endDate = date + ":COMPLETED # " + computer;
        break;
      case FAILED:
        endDate = date + ": Task failed on " + computer;
        break;
      case KILLED:
        endDate = date + ": Task killed on " + computer;
        break;
    }

    try ( PrintWriter pw = new PrintWriter(new File(idtFilename))) {
      pw.println(startDate);
      pw.println(statusMsg);
      pw.print(endDate);
    } catch (Exception e) {
      // nothing can be done if error occures - ignore
    }
  }

  /**
   * Find a task among active and closed tasks by task id.
   * Method returns a task or null is task with id does not exist
   */
  public static ASTask findTask(int taskID) {
    ASTask task = findTask(activeTasks, taskID);
    if (task != null) 
      return task;
    else return findTask(closedTasks, taskID);
  }

  
  /**
   * Method finds in a given queue; if task does not exist, method returns null.
   */
  public static ASTask findTask(SortedArray<ASTask> tasks, int taskID) {
    for (ASTask task : tasks) {
      if (task.getTaskID() == taskID) {
        return task;
      }
    }
    return null;
  }

  // returns tasks in a queue for given (project, alg, tsts, mytpe); tasks might differ in family and computer
  public static ArrayList<ASTask> getTasks(SortedArray<ASTask> tasks, String project, String alg, String tst, String mType) {
    ArrayList<ASTask> rTasks = new ArrayList<>();
    for (ASTask task : tasks) {
      if (project.equals(task.getField(ASTask.ID_Project)) && alg.equals(task.getField(ASTask.ID_Algorithm))
              && tst.equals(task.getField(ASTask.ID_Testset)) && mType.equals(task.getField(ASTask.ID_MType))) {
        rTasks.add(task);
      }
    }
    return rTasks;
  }

  /**
   * Method returns the last non-empty line from task status file.
   */
  public static String getTaskStatus(ASTask task) {
    String taskFilename = getTaskStatusFilename(task);   
    String result = "";
    if (!taskFilename.isEmpty()) try ( Scanner sc = new Scanner(new File(taskFilename))) {
      while (sc.hasNextLine()) {
        String line = sc.nextLine();
        if (line != null && !line.trim().isEmpty()) {
          result = line;
        }
      }
    } catch (Exception e) {
    }
    return result;
  }

  /**
   * Returns an array of all tasks described by request. Request can have one,
   * two, three or four parameters; if only one parameter is given (project)
   * result contains all possible tasks for this project. If all four parameters
   * are given (project, algorithm, testset, mtype), result contains only one
   * task. The tasks returned are strings with four parameters, i.e., "Sorting
   * BubbleSort TestSet1 em".
   *
   * @return
   */
  public static ArrayList<String> getProjectTasks(ArrayList<String> params) {
    ArrayList<String> result = new ArrayList<>();

    if (params.size() < 1) {
      return result; // no parameters, empty result
    }
    // Test the project
    Project projekt = new Project(ATGlobal.getALGatorDataRoot(), params.get(0));
    if (!projekt.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) {
      return result;
    }

    // Test algorithms
    ArrayList<EAlgorithm> eAlgs;
    if (params.size() >= 2) {
      EAlgorithm alg = projekt.getAlgorithms().get(params.get(1));
      if (alg == null) {
        return result;
      }
      eAlgs = new ArrayList();
      eAlgs.add(alg);
    } else {
      eAlgs = new ArrayList(projekt.getAlgorithms().values());
    }

    // Test testsets
    ArrayList<ETestSet> eTests;
    if (params.size() >= 3) {
      ETestSet test = projekt.getTestSets().get(params.get(2));
      if (test == null) {
        return result;
      }

      eTests = new ArrayList<>();
      eTests.add(test);
    } else {
      eTests = new ArrayList(projekt.getTestSets().values());
    }

    // Test mesurement type
    ArrayList<String> mtypes = new ArrayList<>();
    if (params.size() >= 4) {
      mtypes.add(params.get(3));
    } else {
      mtypes.add(MeasurementType.EM.getExtension());
      mtypes.add(MeasurementType.CNT.getExtension());
      mtypes.add(MeasurementType.JVM.getExtension());
    }

    for (EAlgorithm ealg : eAlgs) {
      for (ETestSet ets : eTests) {
        for (String mtype : mtypes) {
          result.add(String.format("%s_%s_%s_%s", params.get(0), ealg.getName(), ets.getName(), mtype));
        }
      }
    }
    return result;
  }

  /**
   * Method sets <mtype>ExecFamily parameter in the project of the task. Method
   * is called when computer cid starts executing task.
   */
  public static void setComputerFamilyForProject(ASTask task, String family) {
    String mType = task.getString(ASTask.ID_MType, MeasurementType.EM.toString()).toUpperCase();
    MeasurementType mt = MeasurementType.UNKNOWN;
    try {
      mt = MeasurementType.valueOf(mType);
    } catch (Exception e) {
    }
    if (mt.equals(MeasurementType.UNKNOWN)) {
      return;
    }

    String projectName = task.getField(ASTask.ID_Project);
    if (projectName == null || projectName.isEmpty()) {
      return;
    }

    String projectFileName = ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName);
    if (projectFileName == null || projectFileName.isEmpty()) {
      return;
    }

    File projectFile = new File(projectFileName);
    EProject project = new EProject(projectFile);

    project.setFamilyAndSave(mt, family, false);
  }

  /**
   * Checks existance of the project and algorithm. If they both exist, method
   * returns "Family:familyName" else it returns error message string.
   */
  public static String checkTaskAndGetFamily(String projName, String algName, String tsName, String mType) {
    EProject proj = EProject.getProject(projName);
    if (!ErrorStatus.getLastErrorStatus().equals(ErrorStatus.STATUS_OK)) {
      return String.format("Project '%s' does not exist.", projName);
    }

    String[] algs = proj.getStringArray(EProject.ID_Algorithms);
    if (!Arrays.asList(algs).contains(algName)) {
      return String.format("Algorithm '%s' does not exist.", algName);
    }

    String[] tsts = proj.getStringArray(EProject.ID_TestSets);
    if (!Arrays.asList(tsts).contains(tsName)) {
      return String.format("Testset '%s' does not exist.", tsName);
    }

    if (mType == null) {
      mType = "em";
    }
    return "Family:" + proj.getProjectFamily(mType);
  }

  static String getFilesAsHTMLList(File root, String indent, int prLength) {
    String result = "  " + indent + "<li><span class=\"treeNLI treeNLI-_hID_ treeCaret" + (indent.length() == 0 ? " treeCaret-down" : "") + "\">" + root.getName() + "</span>";

    // first add all files ...
    result += "\n" + "  " + indent + "<ul id=\"treeNUL\" class=\"treeNested" + (indent.length() == 0 ? " treeActive" : "") + "\">";
    for (File file : root.listFiles()) {
      if (!file.isDirectory()) {
        String dat = file.getAbsolutePath().substring(prLength).replace("\\", "/");
        result += "\n" + "    " + indent + "<li><span dat=\"" + dat + "\" class=\"treeALI treeALI-_hID_\">" + file.getName() + "</span></li>";
      }
    }
    // ... then folders
    for (File file : root.listFiles()) {
      if (file.isDirectory()) {
        result += "\n" + getFilesAsHTMLList(file, "  " + indent, prLength);
      }
    }
    result += "\n" + "  " + indent + "</ul>";
    return result;
  }

  public static String getProjectFiles(String projectName) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);

    File pr = new File(projectRoot);
    if (pr.exists()) {
      return "<ul id=\"treeUL\">\n" + getFilesAsHTMLList(pr, "", pr.getAbsolutePath().length()) + "\n</ul>";
    } else {
      return "";
    }
  }

  public static String getFileContent(String filePath) {
    StringBuilder result = new StringBuilder();
    try {
      for (String l : Files.readAllLines(Paths.get(filePath))) {
        result.append((result.length() == 0 ? "" : "\n")).append(l);
      }
    } catch (Exception e) {
      result = new StringBuilder("!!" + e.toString());
    }
    return result.toString();
  }
  
  public static void writeFileContent(String filePath, String fileContent) {
    try (PrintWriter pw = new PrintWriter(new File(filePath))){
      pw.print(fileContent);
    } catch (Exception e) {}
  }

  public static String getFileContent(String projectName, String fileName) {
    return getFileContent(projectName, fileName, false);
  }

  public static String getFileContent(String projectName, String fileName, boolean encode) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String filePath = projectRoot + File.separator + fileName;
    String result = getFileContent(filePath);
    if (encode) {
      result = Base64.getEncoder().encodeToString(result.getBytes());
    }
    return result;
  }

  public static String saveFile(String projectName, String fileName, String content) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String filePath = projectRoot + File.separator + fileName;
    try ( PrintWriter pw = new PrintWriter(filePath)) {
      pw.print(content);
    } catch (Exception e) {
      return "1:" + e.toString();
    }
    return "0:File saved.";
  }

  public static String getMyIPAddress() {
    try (final DatagramSocket socket = new DatagramSocket()) {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
      return socket.getLocalAddress().getHostAddress();
    } catch (Exception e) {
      return "0.0.0.0";
    }
  }

  /**
   * Returns all folders with result files (e.g. F0.C0, F1.C0, ...)
   */
  private static String[] getResultFolders(String projName) {
    File f = new File(ATGlobal.getRESULTSrootroot(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName)));

    File[] resultFolders = f.listFiles(path -> {
      return path.isDirectory() && path.getName().contains(".");
    });
    if (resultFolders == null) {
      return new String[]{};
    }

    String[] result = new String[resultFolders.length];
    for (int i = 0; i < resultFolders.length; i++) {
      result[i] = resultFolders[i].getName();
    }
    return result;
  }

  /**
   * Returns all files that contain results for given (proj, alg, tst, mtype).
   * First file is always the default file, other files are files form other
   * folders (e.g. F0.C1, F5.C2, ...)
   */
  private static ArrayList<String> getAllResultFilesForPATM(Project projekt, String algName, String tstName, String mType, String[] compIDs) {
    String defaultResultFile = ATTools.getTaskResultFileName(projekt, algName, tstName, mType);

    ArrayList<String> files = new ArrayList<>();
    files.add(defaultResultFile);

    String projRoot = projekt.getProjectRoot();
    MeasurementType mt = MeasurementType.mtOf(mType);
    for (String compID : compIDs) {
      String folder = ATGlobal.getRESULTSroot(projRoot, compID);
      if (defaultResultFile.startsWith(folder)) {
        continue;
      }
      files.add(ATGlobal.getRESULTfilename(projRoot, algName, tstName, mt, compID));
    }
    return files;
  }

  /**
   * filename:
   * /algator_root/data_root/projects/PROJ-X/results/FX.Cx/resfile.mtype)
   * result: FX
   *
   * @return
   */
  private static String getFamilyFromFilename(String filename) {
    try {
      Path resFileFolder = Paths.get(filename).getParent();
      String famComp = resFileFolder.getName(resFileFolder.getNameCount() - 1).toString();

      String[] fc = famComp.split("[.]");
      return fc[0];
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * filename:
   * /algator_root/data_root/projects/PROJ-X/results/FX.Cx/resfile.mtype)
   * result: CX
   */
  private static String getComputerFromFilename(String filename) {
    try {
      Path resFileFolder = Paths.get(filename).getParent();
      String famComp = resFileFolder.getName(resFileFolder.getNameCount() - 1).toString();

      String[] fc = famComp.split("[.]");
      return fc.length > 1 ? fc[1] : "";
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Gets the filename (i.e.
   * /algator_root/data_root/projects/PROJ-X/results/resfile.mtype) and appends
   * to given JSON object the following properties; Fmy (familyID) Cmp
   * (computerID), CID (ComputerUID), RS (ResultStatus), FS (FileStatus)
   */
  private static void resultFileStatus(JSONObject resTaskStatus, String fileName, Project projekt, String algName, String tstName, String mtype, int eNI) {
    // get family and computer name of result
    String comp   = getComputerFromFilename(fileName);
    String family = getFamilyFromFilename(fileName);

    boolean uptodate = ATTools.resultsAreUpToDate(projekt, algName, tstName, mtype, fileName);
    int numberOfCompletedTests = ATTools.getNumberOfTests(fileName);
    boolean complete = numberOfCompletedTests == eNI;

    // Result status: 0 ... result file does not exist, 
    //                1 ...  some, but not all results exist
    //                2 ... all results exist, but they are outdated 
    //                4 ... all results exist and they are up-to-date
    int rs = 0;
    if (numberOfCompletedTests > 0 && numberOfCompletedTests < eNI) {
      rs = 1;
    }
    if (complete && !uptodate) {
      rs = 2;
    }
    if (complete && uptodate) {
      rs = 3;
    }

    String fs = String.format("(%d/%d)", numberOfCompletedTests, eNI);
    String cid = getComputerUID(family, comp);

    resTaskStatus.put("Fmy", family);
    resTaskStatus.put("Cmp", comp);
    resTaskStatus.put("CID", cid);
    resTaskStatus.put("RS", rs);
    resTaskStatus.put("FS", fs);
  }

  /**
   * Appends to given JSON the following properties: TS (task status: "PENDING"
   * or IN "PROGRESS (x/y)"), TID (task id number)
   */
  private static void jsonTaskStatus(JSONObject resTaskStatus, ASTask task, int eNI) {
    String ts = "";
    int tId = 0;
    if (task != null) {
      String taskStatus = task.getTaskStatus().toString();

      ts = task == null ? "" : !task.getTaskStatus().equals(ASTaskStatus.INPROGRESS) ? taskStatus
              : String.format("%s (%d/%d)", taskStatus, task.getProgress(), eNI);
    }
    resTaskStatus.put("TS", ts);
    resTaskStatus.put("TID", tId);
  }

  
  /**
   * Method returns a list of all algorithms of a project projName that are listed in 
   * algorithms (* = all algorithms) and for which uid has "can_read" access
   */  
  private static List<String> getReadableAlgorithms(String uid, Project projekt, JSONArray algorithms) {
    List<String> allAlgs = projekt.getAlgorithms().values().stream().map(EAlgorithm::getName).collect(Collectors.toList());        
    List<String> tAlgs = new ArrayList();
    for (int i = 0; i < algorithms.length(); i++) {
      String algorithm = algorithms.getString(i);
      if (algorithm.equals("*")) {
        for (String tAlg : allAlgs) 
          if (!tAlgs.contains(tAlg)) tAlgs.add(tAlg);        
      } else if (!tAlgs.contains(algorithm)) tAlgs.add(algorithm);
    }
    return tAlgs.stream().filter(a -> {
      String aEid = EAlgorithm.getAlgorithm(projekt.getName(), a).getEID();
      return CanUtil.can(uid, aEid, "can_read");
    }).collect(Collectors.toList());
  }
  /**
   * Method returns a list of all tastsets of a project projName that are listed in 
   * tastsets (* = all tastsets) and for which uid has "can_read" access
   */    
  private static List<String> getReadableTestsets(String uid, Project projekt, JSONArray testsets) {
    List<String> allTsts = projekt.getTestSets().values().stream().map(ETestSet::getName).collect(Collectors.toList());
    List<String> tTsts = new ArrayList();
    for (int i = 0; i < testsets.length(); i++) {
      String testset = testsets.getString(i);
      if (testset.equals("*")) {
        for (String tTst : allTsts) 
          if (!tTsts.contains(tTst)) tTsts.add(tTst);
      } else if (!tTsts.contains(testset)) tTsts.add(testset);
    }
    return tTsts.stream().filter(t -> {
      String tEid = ETestSet.getTestset(projekt.getName(), t).getEID();
      return CanUtil.can(uid, tEid, "can_read");
    }).collect(Collectors.toList());
  }  
  
  private static String familyComputerOf(String filePath) {
    try {
      return Paths.get(filePath).getParent().getFileName().toString();
    } catch (Exception e) {
      return "";
    }
  }
  
  public static JSONObject getAWResults(String uid, String projName, JSONArray algorithms, JSONArray testsets, String mType, boolean isFirstRequest) {
    Project projekt = new Project(ATGlobal.getALGatorDataRoot(), projName);    
    
    String mt = MeasurementType.mtOf(mType).getExtension();

    List<String> algs = new ArrayList<>(); 
    if (isFirstRequest) 
      algs = getReadableAlgorithms(uid, projekt, algorithms);
    else 
      for (int i = 0; i < algorithms.length(); i++) algs.add(algorithms.getString(i));
    
    List<String> tsts = new ArrayList<>();
    if (isFirstRequest) 
      tsts = getReadableTestsets(uid, projekt, testsets);
    else 
      for (int i = 0; i < testsets.length(); i++) tsts.add(testsets.getString(i));
    
    JSONObject results = new JSONObject();

    for (int j = 0; j < tsts.size(); j++) {   
      String tst = tsts.get(j);
      ETestSet eTst = ETestSet.getTestset(projName, tst);
      int numberOfInstances = eTst.getFieldAsInt(ETestSet.ID_N, 0);
      
      for (int i = 0; i < algs.size(); i++) {
        String alg = algs.get(i);
        EAlgorithm eAlg = EAlgorithm.getAlgorithm(projName, alg);
        
        String resultFileName = getTaskResultFileName(projekt, alg, tst, mt);
        String defFam = ""; try {defFam = Paths.get(resultFileName).getParent().getFileName().toString();} catch (Exception e) {}
        
        boolean exists          = new File(resultFileName).exists();
        int     numberOfResults = ATTools.getNumberOfTests(resultFileName);
        boolean isUptodate      = ATTools.resultsAreUpToDate(projekt, alg, tst, mt, resultFileName);
        
        List<Path> allResults = ATTools.getAllResultFiles(projekt, alg, tst, mt, "");
        JSONArray allResA     = new JSONArray();
        for (Path allResult : allResults) {
          String fam = allResult.getParent().getFileName().toString();
          if (!fam.equals(defFam)) allResA.put(fam);
        }
        
        ArrayList<ASTask> tasks = getTasks(activeTasks, projName, alg, tst, mt);
        JSONObject tasksO       = new JSONObject();
        for (ASTask task : tasks) {
          JSONObject taskO = new JSONObject();
          taskO.put("s", task.getTaskStatus());                       // task status
          taskO.put("f", task.getFamily());                           // task Family
          taskO.put("y", task.get(ASTask.ID_Priority));               // task priority
          taskO.put("cca", CanUtil.canChangeTaskStatus(uid, task));   // can current user cancel this task?
          tasksO.put(""+task.getTaskID(), taskO);                     // taskID
        }
        
        boolean ce = CanUtil.can(uid,eAlg.getEID(), "can_execute") || CanUtil.can(uid,eTst.getEID(), "can_execute");
        
        JSONObject status = new JSONObject();
        status.put("e",   exists);
        status.put("f",   familyComputerOf(resultFileName));
        status.put("noi", numberOfInstances);
        status.put("nor", numberOfResults);
        status.put("upd", isUptodate);         
        status.put("tasks", tasksO);
        status.put("ce", ce);
        status.put("ar", allResA);
        
        String key = alg + "_#_" + tst + "_#_" + mt;
        results.put(key, status);
      }
    }

    JSONObject result = new JSONObject();
    result.put("Results",    results);
    result.put("Project",    projName);
    result.put("Algorithms", new JSONArray(algs));
    result.put("Testsets",   new JSONArray(tsts));
    result.put("MType",      mt);
    result.put("AnswerID",   UniqueIDGenerator.getNextID());
    result.put("Timestamp",  System.currentTimeMillis());

    return result;
  }
  
  /**
   * Remove entries in awResults that are older than one hour
   * @return 
   */
  public static int clearAWResults() {
    int noEntries = awResults.keySet().size();
    awResults.keySet().removeIf(key -> {
      long when = 0;
      try {when = awResults.get(key).getLong("Timestamp");} catch (Exception e) {}
      return (new Date().getTime() - when)/1000 > 60*60;
    }); 
    
    // return the number of removed elements
    return noEntries - awResults.keySet().size();
  }

  /**
   * Returns the status of projects results
   */
  public static JSONObject getResultStatus(String projName, String mType) {
    Project projekt = new Project(ATGlobal.getALGatorDataRoot(), projName);
    ArrayList<EAlgorithm> eAlgs = new ArrayList(projekt.getAlgorithms().values());
    ArrayList<ETestSet> eTests = new ArrayList(projekt.getTestSets().values());

    // IDs of comupters that contributed results for this project
    String[] compIDs = getResultFolders(projName);

    String defaultProjectFamily = projekt.getEProject().getProjectFamily(mType);

    JSONArray results = new JSONArray();
    String[] mTypes = mType.isEmpty() ? (new String[]{"em", "cnt", "jvm"}) : (new String[]{mType});
    for (EAlgorithm eAlg : eAlgs) {
      for (ETestSet eTestSet : eTests) {
        for (String mtype : mTypes) {
          int expectedNumberOfInstances = eTestSet.getFieldAsInt(ETestSet.ID_N, 0);

          // all tasks for the triple (alg-tst-mtype) ...
          ArrayList<ASTask> tasks = getTasks(activeTasks, projName, eAlg.getName(), eTestSet.getName(), mtype);
          // ... and the default one (the one with empty or defaultFamily) 

          ArrayList<String> resultFiles = getAllResultFilesForPATM(projekt, eAlg.getName(), eTestSet.getName(), mtype, compIDs);
          HashMap<String, JSONArray> familyResults = new HashMap<>();
          for (String resultFile : resultFiles) {
            if (resultFile.isEmpty()) {
              continue;
            }

            String family = getFamilyFromFilename(resultFile);
            String comp = getComputerFromFilename(resultFile);

            // find the corresponding task
            ASTask task = null;
            for (ASTask cTask : tasks) {
              if (comp.equals(getComputerID(cTask.getComputerUID()))) {
                task = cTask;
                break;
              }
            }

            JSONObject resTaskStatus = new JSONObject();
            resultFileStatus(resTaskStatus, resultFile, projekt, eAlg.getName(), eTestSet.getName(), mType, expectedNumberOfInstances);
            jsonTaskStatus(resTaskStatus, task, expectedNumberOfInstances);

            JSONArray fcArray = familyResults.get(family);
            if (fcArray == null) {
              fcArray = new JSONArray();
              familyResults.put(family, fcArray);
            }

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
              defaultTask = cTask;
              break;
            }
            if (cFamily.isEmpty()) {
              defaultTask = cTask;
            }
          }
          String defFileName = ATTools.getTaskResultFileName(projekt, eAlg.getName(), eTestSet.getName(), mType);
          JSONObject defResTaskStatus = new JSONObject();
          resultFileStatus(defResTaskStatus, defFileName, projekt, eAlg.getName(), eTestSet.getName(), mType, expectedNumberOfInstances);
          jsonTaskStatus(defResTaskStatus, defaultTask, expectedNumberOfInstances);
          defResTaskStatus.put("DF", defaultProjectFamily);

          JSONObject jObj = new JSONObject(String.format("{'Algorithm':'%s', 'TestSet':'%s', 'MType':'%s', 'Status':%s}", eAlg.getName(), eTestSet.getName(), mtype, resByFamiles));
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
      if (!newValues.has(property)) {
        return String.format("2:Missing property '%s'.", property);
      }
    }
    try {
      String jsonString = Files.lines(Paths.get(filename)).collect(Collectors.joining("\n"));
      JSONObject json = new JSONObject(jsonString);

      if (!json.has(entity) || !(json.get(entity) instanceof JSONObject)) {
        return String.format("3:Can't find '%s' in '%s'.", entity, filename);
      }

      for (String property : properties) {
        ((JSONObject) json.get(entity)).put(property, newValues.get(property));
      }
      PrintWriter pw = new PrintWriter(new File(filename));
      pw.println(json.toString(2));
      pw.close();
    } catch (Exception e) {
      return "1:" + e;
    }
    return "0:Properties changed.";
  }

  public static String saveHTMLToFile(String projectName, JSONObject data) {
    try {
      String filename;
      String type = data.optString("Type", "");
      switch (type) {
        case "html_desc":
          filename = "project.html";
          break;
        case "test_case_html_desc":
          filename = "testcase.html";
          break;
        case "test_sets_html_desc":
          filename = "testset.html";
          break;
        case "algorithms_html_desc":
          filename = "algorithm.html";
          break;
        case "project_ref_desc":
          filename = "references.html";
          break;
        default:
          return "2:Invalid file type.";
      }
      String path = ATGlobal.getPROJECTdoc(ATGlobal.getALGatorDataRoot(), projectName);
      File htmlFile = new File(path, filename);
      String content = new String(Base64.getDecoder().decode(data.optString("Content", "")));
      FileUtils.writeStringToFile(htmlFile, content, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "1:" + e;
    }
    return "0:HTML description saved.";
  }

  public static String replaceJSONArrayElement(String filename, String entity, String arrayName, String elementId, String elementIdValue, JSONObject newElementValue) {
    if (!newElementValue.has(elementId)) {
      return String.format("2:Missing elementID '%s' in newValue.", elementId);
    }

    try {
      String jsonString = Files.lines(Paths.get(filename)).collect(Collectors.joining("\n"));
      JSONObject json = new JSONObject(jsonString);

      if (!json.has(entity) || !(json.get(entity) instanceof JSONObject)) {
        return String.format("3:Can't find '%s' in '%s'.", entity, filename);
      }

      JSONObject eJson = json.getJSONObject(entity);

      if (!eJson.has(arrayName) || !(eJson.get(arrayName) instanceof JSONArray)) {
        return String.format("3:Array '%s' does not exist in '%s'.", arrayName, filename);
      }

      boolean exists = false;
      JSONArray eltArray = (JSONArray) (eJson.get(arrayName));
      for (int i = 0; i < eltArray.length(); i++) {
        if (!(eltArray.get(i) instanceof JSONObject)) {
          continue;
        }
        Object idValue = ((JSONObject) eltArray.get(i)).get(elementId);
        if (elementIdValue.equals(idValue)) {
          exists = true;
          eltArray.put(i, newElementValue);
        }
      }
      if (!exists) {
        return String.format("4:No element with '%s'='%s' in %s.", elementId, elementIdValue, arrayName);
      }

      PrintWriter pw = new PrintWriter(new File(filename));
      pw.println(json.toString(2));
      pw.close();
    } catch (Exception e) {
      return "1:" + e;
    }
    return "0:Element replaced.";
  }

  public static String uploadStatic(String uid, JSONObject jObj) {
    try {
      String projectName = jObj.optString("ProjectName", "");

      String peid = EProject.getProject(projectName).getEID();

      // tu bi moral zahtevati: can_write nad dotičnim projektom/algoritmov/testsetom, 
      // vendar nimam podatka, kateremu delu projekta priloga pripada; če bi dal 
      // can_write nad projektom, bi onemogočil dodajanja prilog avtorjem algoritmov....
      if (!CanUtil.can(uid, peid, "can_read")) {
        return sAnswer(99, "uploadStatic: " + "Access denied.", "Access denied.");
      }

      String fileName = jObj.optString("FileName", "");
      if (projectName.isEmpty() || fileName.isEmpty()) {
        return sAnswer(1, "Upload error", "Unknown project or file name.");
      }

      byte[] content = Base64.getDecoder().decode(jObj.optString("FileContent", ""));
      File savePath = new File(ATGlobal.getPROJECTResourcesFolder(projectName));
      if (!savePath.exists()) {
        savePath.mkdirs();
      }

      try ( FileOutputStream fos = new FileOutputStream(new File(savePath, fileName));) {
        fos.write(content);
      } catch (Exception e) {
        return sAnswer(2, "Upload error", e.toString());
      }
      return sAnswer(OK_STATUS, "File uploaded successfully", fileName);
    } catch (Exception e) {
      return sAnswer(3, "Upload error", e.toString());
    }
  }

  // Extract boundary from Content-Type header
  private static String getBoundary(String contentType) {
    String[] parts = contentType.split(";");
    for (String part : parts) {
      if (part.trim().startsWith("boundary=")) {
        return part.trim().substring("boundary=".length());
      }
    }
    return null;
  }

  // Parse the multipart data using the boundary
  private static List<MultipartPart> parseMultipartBody(String body, String boundary) {
    List<MultipartPart> parts = new ArrayList<>();
    String[] partsArray = body.split("--" + boundary);

    for (String part : partsArray) {
      if (part.isEmpty()) {
        continue; // Skip empty parts
      }
      MultipartPart multipartPart = new MultipartPart();
      String[] headersAndBody = part.split("\r\n\r\n", 2);

      if (headersAndBody.length == 2) {
        String headers = headersAndBody[0];
        String bodyContent = headersAndBody[1];

        // Extract content disposition and file name (if any)
        String[] headersArray = headers.split("\r\n");
        for (String header : headersArray) {
          if (header.startsWith("Content-Disposition:")) {
            String[] headerParts = header.split(";");
            for (String headerPart : headerParts) {
              headerPart = headerPart.trim();
              if (headerPart.startsWith("name=")) {
                multipartPart.setName(headerPart.split("=")[1].replace("\"", ""));
              }
              if (headerPart.startsWith("filename=")) {
                multipartPart.setFileName(headerPart.split("=")[1].replace("\"", ""));
              }
            }
          }
        }

        // Set the body content as value (for form fields or file content)
        multipartPart.setValue(bodyContent);
      }

      parts.add(multipartPart);
    }

    return parts;
  }

  // Save the uploaded file to disk as raw binary data
  private static void saveFileToDisk(String fileName, String fileContent) throws IOException {
    // Set up the directory where the file will be saved
    File uploadDir = new File("uploads");
    if (!uploadDir.exists()) {
      uploadDir.mkdir(); // Create directory if it doesn't exist
    }

    // Create the file where the content will be saved
    File uploadedFile = new File(uploadDir, fileName);

    // Write the file content (body content) to the file as raw bytes
    try ( FileOutputStream fileOutputStream = new FileOutputStream(uploadedFile)) {
      // Convert the file content (string) to bytes using a raw byte encoding (UTF-8 or whatever encoding)
      byte[] fileBytes = fileContent.getBytes();
      fileOutputStream.write(fileBytes);
      System.out.println("File saved successfully: " + uploadedFile.getAbsolutePath());
    }
  }

  // MultipartPart class to represent each part of the multipart body
  public static class MultipartPart {

    private String name;
    private String fileName;
    private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static String uploadMultipartX(String uid, Request request) {
    try {
      String body = request.body();
      String contentType = request.contentType();

      if (contentType != null && contentType.contains("multipart/form-data")) {
        String boundary = getBoundary(contentType);
        if (boundary != null) {
          List<MultipartPart> parts = parseMultipartBody(body, boundary);
          for (MultipartPart part : parts) {
            System.out.println("Part name: " + part.getName());
            if (part.getFileName() != null) {
              System.out.println("File name: " + part.getFileName());

              // Save the uploaded file to the disk
              saveFileToDisk(part.getFileName(), part.getValue());
            } else {
              System.out.println("Field: " + part.getValue());
            }
          }
        }
      }
      return sAnswer(OK_STATUS, "Upload suceeded.", "Files uploaded.");
    } catch (Exception e) {
      return sAnswer(2, "Upload error.", e.toString());
    }
  }

  public static String uploadMultipart(Request request) {
    if (!ServletFileUpload.isMultipartContent(request.raw())) {
      return sAnswer(1, "Upload error.", "Not a multipart content.");
    }

    DiskFileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(1024 * 1024 * 1024); // Max file size: 100MB

    try {
      List<FileItem> items = upload.parseRequest(request.raw());

      String projectName = "", type="", name="", uid = "_unknown_";
      for (FileItem item : items) {
        if (item.isFormField()) {
          String fieldName = item.getFieldName();
          if (fieldName.equals("ProjectName")) projectName = item.getString();
          if (fieldName.equals("type"))        type        = item.getString();
          if (fieldName.equals("name"))        name        = item.getString();
          if (fieldName.equals("uid"))         uid         = item.getString();
        }
      }
      
      // assign folder to upload to accorging to type of upload
      String folderName="";
      switch (type) {
        case "testset": 
          folderName = ATGlobal.getTESTSETRecourcesFilename(
             ATGlobal.getALGatorDataRoot(), projectName, name);
          break;
        case COMMON_TST_FILES:
          folderName = ATGlobal.getTESTSETRecourcesFilename(
             ATGlobal.getALGatorDataRoot(), projectName, ATGlobal.AT_DEFAULT_testsetres); 
          break;
        case "jar":  
          folderName = ATGlobal.getPROJECTlibPath(projectName);
          break;
        case "importProject":
          folderName = ATGlobal.getTMPDir("import");
          break;
      } 
      
      if (!folderName.isEmpty()) {
        File folder = new File(folderName);
        if (!folder.exists()) folder.mkdirs();
        
        JSONArray ulFiles =  new JSONArray();
        String ulErrors = "";
        for (FileItem item : items) {
          if (!item.isFormField()) {
            String fileName = item.getName();
            File uploadedFile = new File(folder, fileName);
            try {
              if (uploadedFile.exists()) uploadedFile.delete();
              item.write(uploadedFile);
              ulFiles.put(fileName);
            } catch (Exception e) {
              ulErrors += (ulErrors.isEmpty() ? "":", ") + fileName +":"+ e.toString();
            }
          }
        }
        
        JSONObject result = new JSONObject();
        result.put("Location", folderName);
        result.put("Files", ulFiles);
        
        return jAnswer(OK_STATUS, "Upload to " + projectName + " from " + uid , result.toString());
      } else {
        return sAnswer(3, "Upload error.", "Upload folder can not determined.");
      }
    } catch (Exception e) {
      return sAnswer(2, "Upload error.", e.toString());
    }
  }


  public static void main(String[] args) {
    System.out.println(
            importProject("u5_azp978scbf", "d:\\PROJ-BasicSort.zip", "", false)
    );
  }
}
