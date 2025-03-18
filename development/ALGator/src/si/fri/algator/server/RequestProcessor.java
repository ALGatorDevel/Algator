package si.fri.algator.server;

import algator.Admin;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import static si.fri.algator.server.ASTools.OK_STATUS;
import static si.fri.algator.server.ASTools.activeTasks;
import static si.fri.algator.server.ASTools.closedTasks;
import static si.fri.algator.server.ASTools.iAnswer;
import static si.fri.algator.server.ASTools.jAnswer;
import static si.fri.algator.server.ASTools.pausedAndCanceledTasks;
import static si.fri.algator.server.ASTools.sAnswer;
import static si.fri.algator.server.ASTools.statusResults;
import si.fri.algator.analysis.DataAnalyser;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.entities.EComputer;
import si.fri.algator.entities.EComputerFamily;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.SortedArray;
import static si.fri.algator.admin.Maintenance.synchronizators;
import si.fri.algator.ausers.AUsersTools;
import si.fri.algator.ausers.CanUtil;
import si.fri.algator.ausers.dto.DTOUser;
import si.fri.algator.ausers.dto.PermissionTypes;
import si.fri.algator.database.Database;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.Entity;
import spark.Request;
import spark.Response;

import static si.fri.algator.ausers.CanUtil.accessDeniedString;
import static si.fri.algator.entities.EAlgatorConfig.ID_DBServer;

/**
 * Methods to process the requests to ALGatorServer.
 * 
 * @author tomaz
 */
public class RequestProcessor {
  
  Server server;
  public RequestProcessor(Server server) {
    this.server = server;
  }
    
  public String processRequest(String command, String pParams, Request request, Response response, String uid) {
    // najprej pocistim morebitne pozabljene ukaze in datoteke
    ASCommandManager.sanitize();

    // request command
    command = (command == null || command.isEmpty()) ? "?": command.toUpperCase(); 

    // preverim, ali so parametri v obliki JSON niza
    JSONObject jObj = new JSONObject();
    if (pParams.length() > 0 && pParams.trim().startsWith("{")) {
      try {
        jObj = new JSONObject(pParams.trim().replace("\\", "\\\\"));
      } catch (Exception e) {}
    }

    switch (command) {
      
      // return my ID (ID of caller; taskClient's ID); no parameters
      case ASGlobal.REQ_WHO:
        return who(uid);
      
      case ASGlobal.REQ_DBINFO:
        return dbinfo(uid);  

      // verifying server presence; no parameters
      case ASGlobal.REQ_CHECK_Q:
        return ASGlobal.REQ_CHECK_A;

      // prints server status; no parameters  
      case ASGlobal.REQ_STATUS:
        return serverStatus(); 
        
      case ASGlobal.REQ_DBDATA:
        return notAnonymous();
        
      case ASGlobal.REQ_ADMIN_PRINTPATHS:
        return getServerPaths(uid);

      case ASGlobal.REQ_ADMIN_PRINTLOG:
        return getServerLog(uid, pParams);
       
      case ASGlobal.REQ_GETTIMESTAMP:
        return getTimeStamp();
        
      case ASGlobal.REQ_GETDATA:
        return getData(uid, jObj);  

      case ASGlobal.REQ_ALTER:
        return alter(uid, jObj);    
        
      case ASGlobal.REQ_DB:
        return db_request(uid, jObj);    
        
      // return the list of all tasks in the queue; no parameters  
      // (only tasks that are "visible" to the user are returned)
      case ASGlobal.REQ_GET_TASKS:
        return getTasks(uid, jObj);

      // appends task to the queue; parameters required: project algorithm testset mtype
      case ASGlobal.REQ_ADD_TASK:
        return addTask(uid, jObj);

      case ASGlobal.REQ_CANCEL_TASK:
        return changeTaskStatus(uid, jObj, ASTaskStatus.CANCELED);

      case ASGlobal.REQ_PAUSE_TASK:
        return changeTaskStatus(uid, jObj, ASTaskStatus.PAUSED);

      case ASGlobal.REQ_RESUME_TASK:
        return changeTaskStatus(uid, jObj, ASTaskStatus.UNKNOWN);

      case ASGlobal.REQ_SET_TASK_PRIORITY:
        return changeTaskPriority(uid, jObj);
        
        

      // storest the result of a test and tells client what to do next
      case ASGlobal.REQ_TASK_RESULT:
        return taskResult(uid, jObj);

      // prints the status of given task; parameters required: taskID
      case ASGlobal.REQ_TASK_STATUS:
        return taskStatus(uid, jObj);

      case ASGlobal.REQ_GETTASKRESULT:
        return getTaskResult(uid, jObj);        
        
      // returns task (project algorithm testset mtype); paramaters: computerID  
      case ASGlobal.REQ_GET_TASK:
        return getTask(uid, jObj);

      // removes task from the queue; parameters: taskID  
      case ASGlobal.REQ_CLOSE_TASK:
        return closeTask(uid, jObj);

      case ASGlobal.REQ_GETFILE:
        return getFile(uid, jObj);

      case ASGlobal.REQ_SAVEFILE:
        return saveFile(uid, jObj);

      case ASGlobal.REQ_QUERY_RES:
        return queryResult(uid, pParams);   
        
      case ASGlobal.REQ_QUERY:
        return runQuery(uid, jObj);   
        
      case ASGlobal.REQ_ADMIN:
        return admin(uid, pParams);
        
      case ASGlobal.COMMAND:
        return ASCommandManager.execute(uid, pParams);

      case ASGlobal.REQ_GETFAMILIES:
        return getFamilies(uid);

      case ASGlobal.REQ_ADDFAMILY:
        return addNewFamily(uid, jObj);

      case ASGlobal.REQ_GETCOMPUTERS:
        return getComputers(uid, jObj);

      case ASGlobal.REQ_ADDCOMPUTER:
        return addNewComputer(uid, jObj);

      case ASGlobal.REQ_GETPFILES:
        return getProjectFiles(uid, jObj);
        
      case ASGlobal.REQ_GETPROJECTLIST:
        return getProjectList(uid);

      case ASGlobal.REQ_GETRESULTSTATUS:
        return getResultStatus(uid, jObj);
        
      case ASGlobal.REQ_GETRESULTUPDATE:
        return getResultUpdate(uid, jObj);
        
      case ASGlobal.REQ_GETAWRESULTS:
        return getAWResults(uid, jObj);
      
      case ASGlobal.REQ_GETRESULTFILE:
        return getResultFile(uid, jObj);
                                
      case ASGlobal.REQ_UPLOAD_STATIC:
        return ASTools.uploadStatic(uid, jObj);
               
      default:
        return ASGlobal.getErrorString("Unknown request");
    }
  }
  
  private static String who(String uid) {
    DTOUser user = AUsersTools.getUser(uid);
    return String.format("Thread: %s, user: %s",
       Long.toString(Thread.currentThread().getId()), user != null ? user.getUsername() : "_unknown_");
  } 

  private static String dbinfo(String uid) {
    DTOUser user = AUsersTools.getUser(uid);
    String dbEngine   = ATTools.jSONObjectToMap(EAlgatorConfig.getConfig().getField(ID_DBServer), "Connection", "Database", "Username", "Password").get("Connection").toString();
    
    if (user.isIs_superuser()) 
      return String.format("DBEngine: %s", dbEngine);
    else
      return "Access denied.";
  } 

  
  public String serverStatus() {
    int p = 0, r = 0, pa = 0, q = 0;
    for (ASTask aDETask : activeTasks) {
      if (aDETask.getTaskStatus().equals(ASTaskStatus.PENDING)) {
        p++;
      }
      if (aDETask.getTaskStatus().equals(ASTaskStatus.INPROGRESS)) {
        r++;
      }
      if (aDETask.getTaskStatus().equals(ASTaskStatus.PAUSED)) {
        pa++;
      }
      if (aDETask.getTaskStatus().equals(ASTaskStatus.QUEUED)) {
        q++;
      }
    }

    String ip = ASTools.getMyIPAddress();
    int port  = EAlgatorConfig.getALGatorServerPort();
    return String.format("Server at %s:%d on for %s Thread: %d. Tasks: %d running, %d pending, %d queued, %d paused\n", 
       ip, port, server.getServerRunningTime(), Thread.currentThread().getId(), r, p, q, pa);
  }
  
  public String notAnonymous() {
    return Database.isAnonymousMode() ? "false" : "true";
  }
  
  public String getTimeStamp() {
    return iAnswer(OK_STATUS, "Tmestamp", new Date().getTime());
  }

  public String getServerPaths(String uid) {
    if (CanUtil.can(uid, "e0_S", "full_control"))
      return String.format("ALGATOR_ROOT=%s, DATA_ROOT=%s", ATGlobal.getALGatorRoot(), ATGlobal.getALGatorDataRoot());
    else
      return accessDeniedString;
  }
  
  public String getServerLog(String uid, String params) {
    if (CanUtil.can(uid, "e0_S", "full_control")) {
      int n = 10;
      try {
        n = Integer.parseInt(params);
      } catch (Exception e) {}
      return ASLog.getLog(n);
    } else
      return accessDeniedString;
  }
  
  /**
   * Returns data depending on Type parameter:
   *   - Type=Projects ...  list of all public projects; no other params required
   *   - Type=Project
   *   - Type=Algorit hm ... data of algorithm
   *   - Type=Presenter  ... (params: ProjectName, PresenterName)
   *   - Type=ProjectSources
   *   - Type=ProjectDocs
   *   - Type=ProjectResource
   *   - Type=ProjectProps
   * @return json data
   */
  public String getData(String uid, JSONObject jObj) {
    if (!jObj.has("Type")) 
      return sAnswer(1, "Invalid parameter. Expecting JSON with \"Type\" property.", "");

    String type = jObj.getString("Type");
    switch (type) {
    
      case "Projects":
        return ASTools.getProjectsData(uid);
      case "Project":
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=Project requires 'ProjectName' property.", "");
        return ASTools.getProjectData(uid, jObj.getString("ProjectName"));
      case "ProjectDescription":
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=Project requires 'ProjectName' property.", "");
        return ASTools.getProjectDescription(uid, jObj.getString("ProjectName"));
      case "JarFileContent":
        if (!(jObj.has("ProjectName")&& jObj.has("FileName")))
          return sAnswer(1, "getData of type=JarFileContent requires 'ProjectName' and 'FileName' properties.", "");
        return ASJARTools.getJarFileContent(uid, jObj.getString("ProjectName"), jObj.getString("FileName"));
        
      case "Algorithm":
        if (!(jObj.has("ProjectName")&& jObj.has("AlgorithmName")))
          return sAnswer(1, "getData of type=Algorithm requires 'ProjectName' and 'AlgorithmName' properties.", "");
        return ASTools.getAlgorithmData(uid, jObj.getString("ProjectName"), jObj.getString("AlgorithmName"), jObj.optBoolean("Deep", false));
      case "Testset":
        if (!(jObj.has("ProjectName")&& jObj.has("TestsetName")))
          return sAnswer(1, "getData of type=Testset requires 'ProjectName' and 'TestsetName' properties.", "");
        return ASTools.getTestsetData(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"), jObj.optBoolean("Deep", false));
      case "Presenter":
        if (!(jObj.has("ProjectName")&&jObj.has("PresenterName")))
          return sAnswer(1, "getData of type=Presenters requires 'ProjectName' and 'PresenterName' properties.", "");
        return ASTools.getPresenter(uid, jObj.getString("ProjectName"), jObj.getString("PresenterName"));

      case "TestsetFiles":
        if (!(jObj.has("ProjectName")&& jObj.has("TestsetName")))
          return sAnswer(1, "getData of type=TestsetFiles requires 'ProjectName' and 'TestsetName' properties.", "");
        return ASTools.getTestsetFiles(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"));
      case "TestsetsCommonFiles":
        if (!(jObj.has("ProjectName")))
          return sAnswer(1, "getData of type=TestsetsCommmonFiles requires 'ProjectName' properties.", "");
        return ASTools.getTestsetsCommonFiles(uid, jObj.getString("ProjectName"));        
      case "TestsetFile":
        if (!(jObj.has("ProjectName")&& jObj.has("TestsetName") && jObj.has("FileName")))
          return sAnswer(1, "getData of type=TestsetFiles requires 'ProjectName', 'TestsetName' and 'FileName' properties.", "");
        return ASTools.getTestsetFile(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"), jObj.getString("FileName"));
        
      case "ProjectSources": // project sources
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=ProjectSources requires 'ProjectName' property.", "");
        return ASTools.getProjectSources(uid, jObj.getString("ProjectName"));
      
      case "ProjectProps": // data for query form (algs, testsets, parameters, indicators)
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=ProjectProps requires 'ProjectName' property.", "");
        return ASTools.getProjectProps(uid, jObj.getString("ProjectName"));

      case "ProjectDocs": // all html files and list of resources
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=ProjectDocs requires 'ProjectName' property.", "");
        return ASTools.getProjectDocs(uid, jObj.getString("ProjectName"));
        
      case "ProjectResource": // a resource file
        if (!(jObj.has("ProjectName") && jObj.has("ResourceName")))
          return sAnswer(1, "getData of type=ProjectResource requires 'ProjectName' and 'ResourceName' properties.", "");
        return ASTools.getProjectResource(uid, jObj.getString("ProjectName"), jObj.getString("ResourceName"));                
      
      case "LastModified": // a resource file
        if (!(jObj.has("ProjectName") && jObj.has("What")))
          return sAnswer(1, "getData of type=LastModified requires 'ProjectName' and 'What' properties.", "");
        return ASTools.getLastModified(uid, jObj.getString("ProjectName"), jObj.get("What"));                
        
      default: return sAnswer(1, "Unknown type '"+type+"'.", "");
    }    
  }
  
  public String alter(String uid, JSONObject jObj) {
    if (!(jObj.has("Action")&&jObj.has("ProjectName")))
      return sAnswer(1, "Invalid parameter. Expecting JSON with 'Action' and 'ProjectName' properties.", "");
    
    // pri vsaki zahtevi "alter" se sinhroniziram na projekt (na ime root folderja)
    String projRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), jObj.getString("ProjectName"));
    if (!synchronizators.containsKey(projRoot)) synchronizators.put(projRoot, projRoot);
    String syncObject = synchronizators.get(projRoot);
    synchronized (syncObject) {          
      String action = jObj.getString("Action");
      switch (action) { 
        
        case "NewProject":
          if (!jObj.has("ProjectName"))
            return sAnswer(1, "Alter of type=NewProject requires 'ProjectName'  property.", "");
          String pAuthor = jObj.optString("Author", "");
          String pDate   = jObj.optString("Date", "");           
          return ASTools.newProject(uid, jObj.getString("ProjectName"), pAuthor, pDate);                 
        case "ImportProject":
          if (!jObj.has("Path") || !jObj.has("Filename"))
            return sAnswer(1, "Alter of type=NewProject requires 'Path', 'Filename' properties. The 'ProjectName' property is optional.", "");                    
          String projectName = jObj.optString("ProjectName", "");
          // if projectName == '?' ... let projectName be defined by the name of the zip file
          if (projectName.equals("?")) projectName = "";
          String path        = jObj.optString("Path", "");
          String filename    = jObj.optString("Filename", "");  
          String zipFileName  = new File(path, filename).getPath();
          return ASTools.importProject(uid, zipFileName, projectName, true);          
        case "RemoveProject":
          if (!jObj.has("ProjectName"))
            return sAnswer(1, "Alter of type=RemoveProject requires 'ProjectName'  property.", "");
          return ASTools.removeProject(uid, jObj.getString("ProjectName"));                    
        case "SaveProjectGeneral":
          if (!jObj.has("Data"))
            return sAnswer(1, "Alter of type=SaveProjectGeneral requires 'ProjectName' and 'Data' properties.", "");
          return ASTools.saveProjectGeneral(uid, jObj.getString("ProjectName"), jObj.get("Data"));
        case "SaveHTML":
          if (!jObj.has("Data"))
            return sAnswer(1, "Alter of type=SaveHTML requires 'ProjectName' and 'Data' properties.", "");
          return ASTools.saveHTML(uid, jObj.getString("ProjectName"), jObj.get("Data"));  
        case "RemoveJARFile":
          if (!(jObj.has("ProjectName")&& jObj.has("FileName")))
          return sAnswer(1, "alter of type=RemoveJARFile requires 'ProjectName' and 'FileName' properties.", "");
        return ASTools.removeJARFile(uid, jObj.getString("ProjectName"), jObj.getString("FileName"));

        case "NewAlgorithm":
          if (!jObj.has("AlgorithmName"))
            return sAnswer(1, "Alter of type=NewAlgorithm requires 'ProjectName' and 'AlgorithmName' properties.", "");
          String aAuthor = jObj.optString("Author", "");
          String aDate   = jObj.optString("Date", "");
          return ASTools.newAlgorithm(uid, jObj.getString("ProjectName"), jObj.getString("AlgorithmName"), aAuthor , aDate);                                      
        case "SaveAlgorithm":
          if (!(jObj.has("AlgorithmName")&&jObj.has("Algorithm")))
            return sAnswer(1, "Alter of type=SaveAlgorithm requires 'ProjectName' 'AlgorithmName' and 'Algorithm' properties.", "");
          JSONObject algorithm = new JSONObject();
          try {algorithm = jObj.getJSONObject("Algorithm");} catch (Exception e) {
            return sAnswer(2, "Algorithm not a JSON.", "");            
          }
          return ASTools.saveAlgorithm(uid, jObj.getString("ProjectName"), jObj.getString("AlgorithmName"), algorithm);   
        case "RemoveAlgorithm":
          if (!jObj.has("AlgorithmName"))
            return sAnswer(1, "Alter of type=RemoveAlgorithm requires 'ProjectName' and 'AlgorithmName' properties.", "");
          return ASTools.removeAlgorithm(uid, jObj.getString("ProjectName"), jObj.getString("AlgorithmName"));        
          
        case "NewTestset":
          if (!jObj.has("TestsetName"))
            return sAnswer(1, "Alter of type=NewTestset requires 'ProjectName' and 'TestsetName' properties.", "");
          String tAuthor = jObj.optString("Author", "");
          String tDate   = jObj.optString("Date", "");          
          return ASTools.newTestset(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"), tAuthor, tDate);                            
        case "SaveTestset":
          if (!(jObj.has("TestsetName")&&jObj.has("Testset")))
            return sAnswer(1, "Alter of type=SaveTestset requires 'ProjectName' 'TestsetName' and 'Testset' properties.", "");
          JSONObject testset = new JSONObject();
          try {testset = jObj.getJSONObject("Testset");} catch (Exception e) {
            return sAnswer(2, "Testset not a JSON.", "");            
          }
          return ASTools.saveTestset(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"), testset);                
        case "RemoveTestset":
          if (!jObj.has("TestsetName"))
            return sAnswer(1, "Alter of type=RemoveTestset requires 'ProjectName' and 'TestsetName' properties.", "");
          return ASTools.removeTestset(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"));        
        case "RemoveTestsetFile":
          if (!(jObj.has("ProjectName")&& jObj.has("TestsetName") && jObj.has("FileName")))
          return sAnswer(1, "alter of type=RemoveTestsetFile requires 'ProjectName', 'TestsetName' and 'FileName' properties.", "");
        return ASTools.removeTestsetFile(uid, jObj.getString("ProjectName"), jObj.getString("TestsetName"), jObj.getString("FileName"));

        case "NewPresenter":
          if (!jObj.has("PresenterType"))
            return sAnswer(1, "Alter of type=NewPresenters requires 'ProjectName' and 'PresenterType' properties.", "");
          String rAuthor = jObj.optString("Author", "");
          String rDate   = jObj.optString("Date", "");          
          return ASTools.newPresenter(uid, jObj.getString("ProjectName"), jObj.getInt("PresenterType"), rAuthor, rDate);          
        case "RemovePresenter":
          if (!(jObj.has("ProjectName")&&jObj.has("PresenterName")))
            return sAnswer(1, "Alter of type=RemovePresenters requires 'ProjectName' and 'PresenterName' properties.", "");
          return ASTools.removePresenter(uid, jObj.getString("ProjectName"), jObj.getString("PresenterName"));
        case "SavePresenter":
          if (!(jObj.has("ProjectName")&&jObj.has("PresenterName")&&jObj.has("PresenterData")))
            return sAnswer(1, "Alter of type=SavePresenters requires 'ProjectName', 'PresenterName' and 'PresenterData' properties.", "");
          return ASTools.savePresenter(uid, jObj.getString("ProjectName"), jObj.getString("PresenterName"), jObj.getJSONObject("PresenterData"));
                    
        case "NewParameter":
          if (!jObj.has("ParameterName"))
            return sAnswer(1, "Alter of type=NewParameter requires 'ProjectName', 'ParameterName' and 'IsInput' (optional, default=false)) properties.", "");
          boolean isInput = false; try {isInput = jObj.getBoolean("IsInput");} catch (Exception e) {}
          return ASTools.newParameter(uid, jObj.getString("ProjectName"), jObj.getString("ParameterName"), isInput);                            
        case "SaveParameter":
          if (!(jObj.has("ProjectName")&&jObj.has("ParameterName")&&jObj.has("Parameter")))
            return sAnswer(1, "Alter of type=SaveParameter requires 'ProjectName' 'ParameterName' and 'Parameter' properties.", "");
          JSONObject parameter = new JSONObject();
          try {parameter = jObj.getJSONObject("Parameter");} catch (Exception e) {
            return sAnswer(2, "Parameter not a JSON.", "");            
          }
          return ASTools.saveParameter(uid, jObj.getString("ProjectName"), jObj.getString("ParameterName"), parameter);                
        case "RemoveParameter":
          if (!(jObj.has("ProjectName")&&jObj.has("ParameterName")))
            return sAnswer(1, "Alter of type=RemoveParameter requires 'ProjectName' and 'ParameterName' properties.", "");
          return ASTools.removeParameter(uid, jObj.getString("ProjectName"), jObj.getString("ParameterName"));        
                    
        case "NewIndicator":
          if (!(jObj.has("ProjectName")&&jObj.has("IndicatorName")))
            return sAnswer(1, "Alter of type=NewIndicator requires 'ProjectName', 'IndicatorName', 'IndicatorType' (optional, default='indicator') and 'Meta' (optional, default:{}) properties.", "");
          JSONObject meta =           jObj.optJSONObject("Meta"); if (meta==null) meta = new JSONObject();
          return ASTools.newIndicator(uid, jObj.getString("ProjectName"), jObj.getString("IndicatorName"), jObj.optString("IndicatorType", "indicator"), meta);        
        case "RemoveIndicator":
          if (!(jObj.has("ProjectName")&&jObj.has("IndicatorName")))
            return sAnswer(1, "Alter of type=RemoveIndicator requires 'ProjectName' 'IndicatorName' and 'IndicatorType' (optional, default='indicator') properties.", "");
          return ASTools.removeIndicator(uid, jObj.getString("ProjectName"), jObj.getString("IndicatorName"), jObj.optString("IndicatorType", "indicator"));          
        case "SaveIndicator":
          if (!(jObj.has("ProjectName")&&jObj.has("Indicator")))
            return sAnswer(1, "Alter of type=SaveIndicator requires 'ProjectName' 'Indicator' and 'IndicatorType' (optional, default='indicator') properties.", "");
          return ASTools.saveIndicator(uid, jObj.getString("ProjectName"), jObj.getJSONObject("Indicator"), jObj.optString("IndicatorType", "indicator"));                
         
        case "NewGenerator":
          if (!(jObj.has("ProjectName")&&jObj.has("GeneratorName")))
            return sAnswer(1, "Alter of type=NewGenerator requires 'ProjectName', 'GeneratorName' and 'GeneratorParameters' (optional, default:[]) properties.", "");
          JSONArray genParams =           jObj.optJSONArray("GeneratorParameters"); if (genParams==null) genParams = new JSONArray();
          return ASTools.newGenerator(uid, jObj.getString("ProjectName"), jObj.getString("GeneratorName"), genParams);                
        case "SaveGenerator":
          if (!(jObj.has("ProjectName")&&jObj.has("GeneratorType")&&jObj.has("Generator")&&jObj.has("Code")))
            return sAnswer(1, "Alter of type=SaveParameter requires 'ProjectName' 'GeneratorType', 'Code' and 'Generator' properties.", "");
          JSONObject generator = new JSONObject();
          try {generator = jObj.getJSONObject("Generator");} catch (Exception e) {
            return sAnswer(2, "Generator not a JSON.", "");            
          }
          return ASTools.saveGenerator(uid, jObj.getString("ProjectName"), jObj.getString("GeneratorType"), generator, jObj.getString("Code"));                
        case "RemoveGenerator":
          if (!(jObj.has("ProjectName")&&jObj.has("GeneratorName")))
            return sAnswer(1, "Alter of type=RemoveGenerator requires 'ProjectName' and 'GeneratorName' properties.", "");
          return ASTools.removeGenerator(uid, jObj.getString("ProjectName"), jObj.getString("GeneratorName"));                  
          
        default: return sAnswer(1, "Unknown type '"+action+"'.", "");        
      }
    }
  }
  
  // testne funkcije, se ne bodo uporabljale v produkciji
  public String db_request(String uid, JSONObject jObj) {
    if (!jObj.has("Action"))
      return sAnswer(1, "Invalid parameter. Expecting JSON with 'Action' property.", "");
    
    String action = jObj.optString("Action", "").toUpperCase();
    switch (action) { 
      case "GETUSERS":
        if (CanUtil.can(uid, "e0_S", "can_edit_users")) {
          ArrayList<DTOUser> users = AUsersTools.readUsers();
          String ans="";for (DTOUser user : users) ans +=(ans.isEmpty()? "" : ", ") + user.toString();
          return sAnswer(OK_STATUS, "Users", "["+ans+"]");
        } else
          return sAnswer(99, "Users", accessDeniedString);
      case "CAN":        
        String eid      = jObj.optString("eid", "");
        String codename = jObj.optString("codename", "");
        String can      = String.format("can('%s', '%s', '%s')", uid, eid, codename);
        return sAnswer(OK_STATUS, can, CanUtil.can(uid, eid, codename)?"true":"false");
        
        default: return sAnswer(1, "Unknown type '"+action+"'.", "");                
    }   
  }

  
  /**
   * Create a new task and add it to waiting the queue. Taska are of two types: normal task (Ececute)
   * and misc task (Compile, RunOne, ...).
   * "Normal" task requires parameters like Project, Algorithm, TestSet, ...   
   * "Misc" task requires different parameters (depending on type of task)
   * 
   */
  public String addTask(String uid, JSONObject jObj) {
    boolean hasProject   = jObj.has(ASTask.ID_Project)   && !jObj.getString(ASTask.ID_Project).isEmpty();
    if (!hasProject) {
       return sAnswer(1, "Invalid parameter. Expecting JSON with at least \"Project\" property.", "");
    }
    
    String taskType      = jObj.has(ASTask.ID_TaskType) ? jObj.getString(ASTask.ID_TaskType) : ASTask.ID_TaskType_Execute;
    
    if (taskType.equals(ASTask.ID_TaskType_Execute)) { 
      boolean hasAlgorithm = jObj.has(ASTask.ID_Algorithm) && !jObj.getString(ASTask.ID_Algorithm).isEmpty();
      boolean hasTestset   = jObj.has(ASTask.ID_Testset)   && !jObj.getString(ASTask.ID_Testset).isEmpty();
      if (!(hasProject && hasAlgorithm && hasTestset)) {
        return sAnswer(1, "Invalid parameter. Expecting JSON with \"Project\", \"Algorithm\" and \"Testset\" properties.", "");
      }

      String mType = MeasurementType.mtOf(jObj.getString(ASTask.ID_MType)).getExtension();

      String taskOK = ASTools.checkTaskAndGetFamily(jObj.getString(ASTask.ID_Project), jObj.getString(ASTask.ID_Algorithm),jObj.getString(ASTask.ID_Testset), mType);
      if (!taskOK.startsWith("Family:")) {
        return sAnswer(2, "Invalid task. " + taskOK, "");
      }
      
      // here taskOK equals "Family:familyName" (which can be empty). If family in 
      // task is not explicitely set, we set it to "familyName" 
      if (!jObj.has(ASTask.ID_Family) || jObj.getString(ASTask.ID_Family).isEmpty()) {
        String fP[] = taskOK.split(":");
        jObj.put(ASTask.ID_Family, fP.length > 1 ? fP[1] : "");
      }
      
      String projectName   = jObj.getString(ASTask.ID_Project);
      String peid          = EProject.getProject(projectName).getEID(); 
      String algorithmName = jObj.getString(ASTask.ID_Algorithm);
      String aeid          = EAlgorithm.getAlgorithm(projectName, algorithmName).getEID(); 
      String testsetName   = jObj.getString(ASTask.ID_Testset);
      String teid          = ETestSet.getTestset(projectName, testsetName).getEID(); 
                
      ArrayList<ASTask> existingTasks = ASTools.getTasks(activeTasks, projectName, algorithmName, testsetName, mType);
      for (ASTask task : existingTasks) {
        if (task.getFamily().equals(jObj.get(ASTask.ID_Family))) {
          return sAnswer(3, "Task already exist.", "Can not duplicate task.");
        }
      }
      
      if (CanUtil.canExecuteTask(uid, aeid, teid)) {
        jObj.put(ASTask.ID_TaskOwner, uid);
        jObj.put(ASTask.ID_TaskType, ASTask.ID_TaskType_Execute);
        jObj.put(ASTask.ID_Project_EID, peid);
        jObj.put(ASTask.ID_Algorithm_EID, aeid);
        jObj.put(ASTask.ID_Testset_EID, teid); 
        jObj.put(ASTask.ID_MType, mType); 
      } else {
        return sAnswer(99, "AddTask", accessDeniedString);
      }
    } else {
        // tasktype specific settings
    }    
    
    ASTask task = new ASTask(jObj.toString());
    task.assignID();
    activeTasks.add(task);
    
    if (task.getComputerUID() != null && !task.getComputerUID().isEmpty()) 
      ASTools.setTaskStatus(task, ASTaskStatus.QUEUED, null, null);
    else 
      ASTools.setTaskStatus(task, ASTaskStatus.PENDING, null, null);

    return jAnswer(OK_STATUS, "Task added", task.toJSONString(0));      
  }

  /**
   * Odjemalec po koncu vsakega testa strežniku pošlje zahtevo "taskresuilt" s
   * parametri json(client:uid, task:taskID, result:json_result, testNo:
   * testNo).
   *
   * Strežnik -> nastavi task.progress=testNo -> result shrani v datoteko ->
   * poišče najbolj "vroč" naslednji task za odjemalca (nextTask) -> če
   * nextTask == currentTask ... pošlje CONTINUE sicer ... task.status nastavi
   * na QUEUED in pošlje BREAK
   *
   * Odjemalec: -> če prejme CONTINUE, nadaljuje z izvajanjem testa -> će prejme
   * BREAK (kar je lahko posledica prioritete taskov ali dejstva, da je bil task
   * prekinjen (CANCELED) ali dan na čakanje (PAUSED)) ... prekine izvajanje
   * testseta in kontrolo preda metodi runClient(); ta bo strežnik vprašala za
   * naslednje opravilo (getTask).
   */
  public String taskResult(String uid, JSONObject jObj) {
    String compUID = "";
    int taskID = 0;
    int testNo = 0;
    String result = "";
    String taskType = ASTask.ID_TaskType_Execute;
    try {
      compUID  = jObj.optString(ASTask.ID_ComputerUID, "");
      taskID   = jObj.optInt(ASTask.ID_TaskID, -1);
      testNo   = jObj.optInt("TestNo", -1);
      result   = jObj.optString("Result", "?");
      taskType = jObj.optString(ASTask.ID_TaskType, ASTask.ID_TaskType_Execute);
    } catch (Exception e) { }
    if (compUID.isEmpty() || taskID < 0 || (taskType.equals(ASTask.ID_TaskType_Execute) && testNo < 0) || result.isEmpty()) {
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + ASTask.ID_ComputerUID + "\", \"" + ASTask.ID_TaskID + "\", \"TestNo\" and \"Result\" properties.");
    }

    ASTask task = ASTools.findTask(activeTasks, taskID);    
    if (task != null) {
      if (taskType.equals(ASTask.ID_TaskType_Execute)) { // "normal" task
        // result can only be returned by someone that has "can_execute" permission over the tasks' algorithm
        if (!(uid.equals(DTOUser.USER_CLIENT) || CanUtil.can(uid, task.getString(ASTask.ID_Algorithm_EID), "can_execute"))) 
          return sAnswer(99, "taskResult", accessDeniedString);
  
        // if task is OK and if it belongs to the computer with UID = compUID ...
        if (compUID.equals(task.getComputerUID())) {
          // ... set task progress ...
          task.setProgress(testNo);
          ASTools.writeADETasks(activeTasks, 0);
  
          // ... write results to result file ...
          String rsFilename = ASTools.getResultFilename(task);
          ATTools.createFilePath(rsFilename);
          try ( PrintWriter pw = new PrintWriter(new FileWriter(new File(rsFilename), true));) {
            pw.println(result);
          } catch (Exception e) {}
          // ... and respond to client: 
          // BREAK ... if user has paused  or canceled task   
          ASTaskStatus pausedOrCanceledTaskStatus = pausedAndCanceledTasks.get(task.getTaskID());
          if (pausedOrCanceledTaskStatus != null) {
            ASTools.setTaskStatus(task, pausedOrCanceledTaskStatus, null, null);
            pausedAndCanceledTasks.remove(task.getTaskID());
            return sAnswer(OK_STATUS, "Result accepted, task "
                    + (ASTaskStatus.CANCELED.equals(pausedOrCanceledTaskStatus) ? "canceled" : "paused"), "BREAK");
          }
  
          // tole sem dodal, ker se izvajanje taska ni ustavilo, če je 
          // nekdo poslal zahtevo pauseTask. 
          if (task.getTaskStatus().equals(ASTaskStatus.PAUSED))
            return sAnswer(OK_STATUS, "Result accepted, task paused.", "PAUSED");
          
          // CONTINUE  ... if this task is to be continued
          // QUEUED    ... if another task if more appropropriate to be executed by this client
          ASTask nextTask = ASTools.findFirstTaskForComputer(activeTasks, compUID, false);
          if (nextTask == null || (task.compare(nextTask) >= 0)) {
            return sAnswer(OK_STATUS, "Result accepted, continue.", "CONTINUE");
          } else {
            ASTools.setTaskStatus(task, ASTaskStatus.QUEUED, null, null);
            return sAnswer(OK_STATUS, "Result accepted, task queued.", "QUEUED");
          }
  
        } else {
          return sAnswer(3, "Task does not belong to this computer", compUID);
        }
      } else { // "misc" task 
        task.set(ASTask.ID_TaskType, taskType);
        task.set(ASTask.ID_EID, taskID);
        String rsFilename = ASTools.getMiscResultFilename(task);
        try (PrintWriter pw = new PrintWriter(new File(rsFilename))) {
          pw.write(result); 
        } catch (Exception e) {}
        return sAnswer(OK_STATUS, "Result recorder.", "");
      }
    } else {
      return iAnswer(2, "Invalid task number", taskID);
    }
  }

  public String taskStatus(String uid, JSONObject jObj) {        
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + ASTask.ID_TaskID + "\" property.");
    if (!jObj.has(ASTask.ID_TaskID)) {
      return errorAnswer;
    }

    int taskID;
    try {
      taskID = jObj.getInt(ASTask.ID_TaskID);
    } catch (Exception e) {
      return errorAnswer;
    }

    ASTask task = ASTools.findTask(activeTasks, taskID);
    if (task == null) {
      task = ASTools.findTask(closedTasks, taskID);
    }
    if (task != null) {
      String peid = task.getString(ASTask.ID_Project_EID, Entity.EID_UNDEFINED); 
      if (CanUtil.can(uid, peid, "can_read")) {
        return jAnswer(OK_STATUS, "Task status", task.toJSONString(0));
      } else 
        return sAnswer(99, "taskStatus", accessDeniedString);
    }

    return iAnswer(2, "Unknown task.", taskID);
  }

  public String getTaskResult(String uid, JSONObject jObj) {        
    int taskID = jObj.optInt(ASTask.ID_TaskID, -1);    
    if (taskID==-1)
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + ASTask.ID_TaskID + "\" property.");

    ASTask task = ASTools.findTask(taskID);
    if (task == null)
      return sAnswer(2, "No task.", "Task with id=" +  taskID + " does not exist.");

    EComputer comp = ASTools.getComputer(task.getString("ComputerUID", ""));
    if (comp== null) return sAnswer(OK_STATUS, "Task result", "No computer assigned to execute the task.");
    
    String resultFileName="";
    if (task.getString(ASTask.ID_TaskType, "undefined").equals(ASTask.ID_TaskType_Execute)) {
      String projRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), task.getString(ASTask.ID_Project, ""));
      MeasurementType mt = MeasurementType.mtOf(task.getString(ASTask.ID_MType, ""));
      resultFileName = ATGlobal.getRESULTfilename(projRoot, task.getString(ASTask.ID_Algorithm, ""), task.getString(ASTask.ID_Testset, ""), mt, comp.getFCName());
    } else {      
      resultFileName = ATGlobal.getRESULTMiscFilename(task.getString("Project", ""), comp.getFCName(), task.getFieldAsInt("eid", 0)+"", task.getString("TaskType", ""));
    }
    String fileContent = "";
    File resFile = new File(resultFileName);
    if (resFile.exists() && resFile.lastModified() > task.getTaskCreationDate())
      try {fileContent = FileUtils.readFileToString(resFile, "UTF-8");} catch (Exception e){}    

    JSONObject result = new JSONObject();
    result.put("FileName", resultFileName);
    result.put("FileContent", fileContent);
    return jAnswer(OK_STATUS, "Task result", result.toString());
  }
  
  /**
   * Finds the next task that can be executed on a computer with given cid  
   * Call: getTask {ComputerID: cid}                
   * Return: NO_TASKS or Task (json(id proj alg testset mtype, progress))
   */
  public String getTask(String uid, JSONObject jObj) {
    if (!jObj.has(EComputer.ID_ComputerUID)) {
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + EComputer.ID_ComputerUID + "\" property.");
    }
    String cuid = jObj.getString(EComputer.ID_ComputerUID);
    String family = ASTools.familyOfComputer(cuid);
    if ("?".equals(family)) {
      return sAnswer(2, ASGlobal.NO_TASKS, "Invalid computer or computer family");
    }

    ASTask task = null;
    if (CanUtil.can(uid, "e0_P", "can_execute")) 
      task = ASTools.findFirstTaskForComputer(activeTasks, cuid, true);
    
    if (task != null) {
        ASTools.setTaskStatus(task, ASTaskStatus.INPROGRESS, null, cuid);
      ASTools.setComputerFamilyForProject(task, family);
      
      int progress = task.getProgress();
      // preden oddam task, pobrisem datoteko z rezultati, da se bodo rezultati
      // tega izvajanja shranili v prazno datoteko (rewrite namesto append)
      if (progress <= 0)
        ASTools.removeTaskResultFile(task);
      return jAnswer(0, "Task for computer " + cuid, task.toJSONString(0));
    } else {
      return sAnswer(3, ASGlobal.NO_TASKS, "No tasks available for this computer.");
    }
  }

  /**
   * Removes task from activeTasks list and writes status to the file. Call:
   * closeTask json(ExitCode, TaskID, Message) Return: "OK" or error message
   */
  public String closeTask(String uid, JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"ExitCode\",  \"" + ASTask.ID_TaskID + "\" and \"Message\" properties.");

    if (!jObj.has("ExitCode") || !jObj.has(ASTask.ID_TaskID)) {
      return errorAnswer;
    }

    int taskID, exitCode;
    String msg;
    try {
      taskID = jObj.getInt(ASTask.ID_TaskID);
      exitCode = jObj.getInt("ExitCode");
      msg = jObj.getString("Message");
    } catch (Exception e) {
      return errorAnswer;
    }

    // find a task with a given ID in activeTasks queue    
    ASTask task = ASTools.findTask(activeTasks, taskID);
    if (task != null) {
      String aeid           = task.getString(ASTask.ID_Algorithm_EID, Entity.EID_UNDEFINED);
      if (!CanUtil.can(uid, "e0_P", "can_execute")) return "NOK, access denied.";
      if (exitCode == 0) {
        ASTools.setTaskStatus(task, ASTaskStatus.COMPLETED, msg, null);
      } else if (exitCode == ErrorStatus.PROCESS_KILLED.ordinal()) {
        ASTools.setTaskStatus(task, ASTaskStatus.KILLED, msg, null);
      } else {
        ASTools.setTaskStatus(task, ASTaskStatus.FAILED, "Execution failed, : " + msg, null);
      }

      return "OK";
    } else {
      return "Task not found.";
    }
  }
  
  public String changeTaskPriority(String uid, JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with  \"" + ASTask.ID_TaskID + "\" and \"Priority\" properties.");

    if (!jObj.has(ASTask.ID_TaskID) || !jObj.has(ASTask.ID_Priority)) 
      return errorAnswer;
        
    int taskID   = jObj.optInt(ASTask.ID_TaskID, -1);
    int priority = jObj.optInt(ASTask.ID_Priority, 5);
    
    if (taskID < 0) return errorAnswer;
        
    // find a task with a given ID in activeTasks queue    
    ASTask task = ASTools.findTask(activeTasks, taskID);
    if (task != null) {
      String aeid = task.getString(ASTask.ID_Algorithm_EID);
      if (CanUtil.can(uid, aeid, "can_execute")) {            
        task.set(ASTask.ID_Priority, priority);
        activeTasks.touch(task);
        ASTools.writeADETasks(activeTasks, 0);
  
        return sAnswer(0, "OK", "Status changed");
      } else
        return sAnswer(99, "ChangeTaskStatus", accessDeniedString);
    } else {
      return iAnswer(2, "Task not found.", taskID);
    }
  }

  
  
  
  
  
  
  /**
   * Changes status - from INPROGRESS, PENDING, QUEUED -- to --> PAUSED or
   * CANCELED - from PAUSED -- to --> PENDING or QUEUED (in this case newStatus
   * is UNKNOWN) Call: pauseTask | cancelTask | resumeTask json(TaskID) Return:
   * "OK" (0) or error message
   */
  public String changeTaskStatus(String uid, JSONObject jObj, ASTaskStatus newStatus) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with  \"" + ASTask.ID_TaskID + "\" property.");

    if (!jObj.has(ASTask.ID_TaskID)) {
      return errorAnswer;
    }
        
    int taskID;
    try {
      taskID = jObj.getInt(ASTask.ID_TaskID);
    } catch (Exception e) {
      return errorAnswer;
    }
    int exitCode = 0;
    try {exitCode = jObj.getInt("ExitCode");} catch (Exception e) {}
        
    // find a task with a given ID in activeTasks queue    
    ASTask task = ASTools.findTask(activeTasks, taskID);
    if (task != null) {
      String aeid           = task.getString(ASTask.ID_Algorithm_EID);
      if (CanUtil.canChangeTaskStatus(uid, task)) {            
        if (newStatus.equals(ASTaskStatus.UNKNOWN)) { // resumeTask called
          // if current status is PAUSED, task is revitalized (to PENDING or QUEUED)
          if (task.getTaskStatus().equals(ASTaskStatus.PAUSED)) {
            if (task.getComputerUID() != null && !task.getComputerUID().isEmpty()) {
              ASTools.setTaskStatus(task, ASTaskStatus.QUEUED, null, null);
            } else {
              ASTools.setTaskStatus(task, ASTaskStatus.PENDING, null, null);
            }
          } else {
            return iAnswer(3, "Can not resume  - task is not paused.", taskID);
          }
        } else { // newStatus == CANCELED or PAUSED
          if (task.getTaskStatus().equals(ASTaskStatus.INPROGRESS)) {
            if (exitCode == 0) { // normal "cancelTask" 
              // this is new: besides puting task to pausedAndCanceledTasks, 
              // change also its status to CANCELED
              task.setTaskStatus(ASTaskStatus.CANCELED);
              
              pausedAndCanceledTasks.put(task.getTaskID(), newStatus);
            } else { // "cancelTask"  invoked by TaskClient due to execution error 
              ASTools.setTaskStatus(task, newStatus, jObj.optString("Message", ""), null);
            } 
          } else {
            ASTools.setTaskStatus(task, newStatus, null, null);
          }
        }
  
        return sAnswer(0, "OK", "Status changed");
      } else
        return sAnswer(99, "ChangeTaskStatus", accessDeniedString);
    } else {
      return iAnswer(2, "Task not found.", taskID);
    }
  }
  
  public String getTasks(String uid, JSONObject jObj) {    
      SortedArray<ASTask> tasks = null;
  
      String list = jObj.has("Type") ? jObj.getString("Type") : "";
      if (list.isEmpty() || "active".equals(list)) {
        tasks = activeTasks;
      }
      if ("closed".equals(list)) {
        tasks = closedTasks;
      } else if ("archived".equals(list)) {
        tasks = ASTools.readADETasks(2);
      }

      if (tasks == null) 
        return sAnswer(1, "Invalid type, 'active', 'closed' or 'archived' expected.", "");            
      
      // filter out tasks that not are visible to uid 
      ArrayList<ASTask> visibleTasks = new ArrayList<>();
      Iterator<ASTask> it = tasks.iterator();
      while (it.hasNext()) {
        ASTask task = it.next();        
        if (CanUtil.canExecuteTask(uid, task)) visibleTasks.add(task);
      }
      
      StringBuilder sb = new StringBuilder();
      for (ASTask task : visibleTasks) {
        String taskS = task.toJSONString(0);
        // change StatusDate and CreationDate from int to string representation
        taskS = ATTools.replaceDateL2S(taskS, "dd.MM.YY HH:mm:ss");
  
        sb.append((sb.length() > 0 ? ",\n" : "")).append(taskS);
      }
      return "[" + sb.toString() + "]";
  }

/**
   * Method returns an array of results produced by a given query. At least two
   * parameters are required (projectName and queryName) all other parameters
   * are passed to the query as query parameters.
   */
  public String queryResult(String uid, String params) {
    String parts[] = params.split(" ", 3); // pricakujem "projectName queryName <params>"
    if (parts.length < 2) {
      return ""; // required params missing
    }

    String[] queryParams = parts.length > 2 ? parts[2].split(" ") : new String[0];
    String computerID = null; // ne določim imena računalnika; če ta ne bo podan v poizvedbi, se bo izbrala "najbolj primerna" result datoteka

    // v poizvedbi se pred pošiljanjem vsi presledki nadomestijo z znakom _!_, da med prenosom ne pride do tezav (zmešnjava s parametri poizvedbe)
    String query = parts[1].replaceAll("_!_", " ");
    return DataAnalyser.getQueryResultTableAsString(uid, parts[0], query, queryParams, computerID);
  }
  

  /**
   * Method returns a String of results produced by a given query. 
   */  
  public String runQuery(String uid, JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"ProjectName\", \"Query\", \"ComputerID\" (optional) and \"Parameters\" (optional) properties.");
    
    String projName, query, compID;
    JSONArray jParams;String[] params = null;
    try {
      projName  = jObj.getString("ProjectName");
      query     = jObj.getJSONObject("Query").toString(); 
      compID    = jObj.optString("ComputerID", null);
      
      jParams   = jObj.optJSONArray("Parameters");     
      List<String> list = new ArrayList<>();
      if (jParams != null) for(int i = 0; i < jParams.length(); i++)
        list.add((String)jParams.get(i));
      params = list.toArray(new String[0]); 

    } catch (Exception e) {
      return errorAnswer;
    }

    if (projName.isEmpty() || query.isEmpty()) return errorAnswer;

    EProject eProject = EProject.getProject(projName);
    EQuery   eQuery = new EQuery(query, params);
    
    return sAnswer(OK_STATUS, "Query result", DataAnalyser.runQuery(uid, eProject, eQuery, compID).toString());
  }

  // vsebino datotek projekta lahko bere le nekdo, ki ima nad projektom "can_write" pravice
  public String getFile(String uid, JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" and \"File\" properties.");
    
    String projName, file;
    try {
      projName = jObj.getString("Project");
      file     = jObj.getString("File");
    } catch (Exception e) {
      return errorAnswer;
    }

    String peid = EProject.getProject(projName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) return sAnswer(99, "getfile: " + accessDeniedString, accessDeniedString);
    
    String answer = ASTools.getFileContent(projName, file);
    if (answer.startsWith("!!")) 
      return sAnswer(1, "Error reading file", answer.substring(2));
    else 
      return sAnswer(OK_STATUS, "File content", Base64.getEncoder().encodeToString(answer.getBytes())); 
  }
  
  // tu s pravicami zamižim na eno oko - datoteko lahko zapiše vsak, ki ima nad 
  // projektom (!) pravico "can_write" in ki pozna pot do datoteke. 
  // Pravzaprav je to dodatna ovira za avtorje algoritmov (ki bi algoritem sicer lahko pisali,
  // vendar če niso lastniki projekta, s to metodo ne morejo pisati!)  
  public String saveFile(String uid, JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\", \"File\", \"Content\" and \"Length\" properties.");
    
    String projName, file, content;int len;
    try {
      projName = jObj.getString("Project");
      file     = jObj.getString("File");
      content  = jObj.getString("Content");
      len      = jObj.getInt("Length");
    } catch (Exception e) {
      return errorAnswer;
    }
    if (projName.isEmpty() || file.isEmpty() || content.isEmpty()) return errorAnswer;
    
    String peid = EProject.getProject(projName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) return sAnswer(99, "savefile: " + accessDeniedString, accessDeniedString);
    
    if (content.length() != len)
      return sAnswer(2, "File save error", "File content length mismatch");

    content = new String(Base64.getDecoder().decode(content));
    
    String answer = ASTools.saveFile(projName, file, content);
    if (answer.startsWith("!!")) 
      return sAnswer(1, "Error saving file", answer.substring(2));
    else 
      return sAnswer(OK_STATUS, "OK", "File saved."); 
  }

  
  public String getFamilies(String uid) {
    String res = "";
    if (CanUtil.can(uid, "e0_S", "can_edit_clients")) {
      EAlgatorConfig config = EAlgatorConfig.getConfig();
      ArrayList<EComputerFamily> fam = config.getFamilies();
      for (EComputerFamily eF : fam) {
        res += (res.isEmpty() ? "" : ", ") + ASTools.getJSONString(eF, EComputerFamily.ID_FamilyID, EComputerFamily.ID_Desc, EComputerFamily.ID_Platform, EComputerFamily.ID_Hardware);
      }
    }
    return jAnswer(OK_STATUS, "Computer families", "[" + res + "]");
  }

  public String addNewFamily(String uid, JSONObject jsonFamily) {
    if (!CanUtil.can(uid, "e0_S", "can_edit_clients")) 
      return sAnswer(99, "addNewFamily: " + accessDeniedString, accessDeniedString);
    
    try {
      JSONObject family = new JSONObject(jsonFamily.toString());
      String thisFamilyID = family.getString(EComputerFamily.ID_FamilyID);
      if (thisFamilyID.contains(" ")) {
        return sAnswer(1, "Error - invalid character in FamilyID", "");
      }

      EAlgatorConfig config = EAlgatorConfig.getConfig();
      JSONArray ja = config.getField(si.fri.algator.entities.EAlgatorConfig.ID_Families);
      if (ja==null) { 
        ja=new JSONArray();config.set(si.fri.algator.entities.EAlgatorConfig.ID_Families, ja);
      }
      // preglej, ali id družine že obstaja - potem je ne moreš dodati
      for (int i = 0; i < ja.length(); i++) {
        if (thisFamilyID.equals(((JSONObject) ja.get(i)).get(EComputerFamily.ID_FamilyID))) {
          return sAnswer(2, "Error - family with this ID already exists.", "");
        }
      }

      ja.put(family);
      config.saveEntity();

      return sAnswer(OK_STATUS, "Family added", family.getString(EComputerFamily.ID_FamilyID));
    } catch (Exception e) {
      System.out.println(e);
    }

    return sAnswer(1, "Bad request", "Bad request");
  }

  public String getComputers(String uid, JSONObject request) {
    if (!CanUtil.can(uid, "e0_S", "can_edit_clients")) 
      return sAnswer(99, "getComputers: " + accessDeniedString, accessDeniedString);

    String familyID = request.optString("FamilyID", "");
    
    EAlgatorConfig config = EAlgatorConfig.getConfig();
    ArrayList<EComputer> comps = config.getComputers();
    String res = "";
    for (EComputer comp : comps) {
      if (familyID.isEmpty() || familyID.equals(comp.getField(EComputer.ID_FamilyID))) {
        res += (res.isEmpty() ? "" : ", ") + ASTools.getJSONString(comp, EComputer.ID_ComputerUID, EComputer.ID_ComputerID, EComputer.ID_FamilyID, EComputer.ID_Desc, EComputer.ID_Capabilities);
      }
    }
    return jAnswer(OK_STATUS, "Computers", "[" + res + "]");
  }

  public String addNewComputer(String uid, JSONObject jsonComputer) {
    if (!CanUtil.can(uid, "e0_S", "can_edit_clients")) 
      return sAnswer(99,"addNewComputer: " + accessDeniedString, accessDeniedString);
    
    try {
      JSONObject computer = new JSONObject(jsonComputer.toString());
      String thisComputerID = computer.getString(EComputer.ID_ComputerID);
      String thisFamilyID = computer.getString(EComputer.ID_FamilyID);
      if (thisComputerID.contains(" ")) {
        return sAnswer(1, "Error - invalid character in ComputerID", "");
      }

      EAlgatorConfig config = EAlgatorConfig.getConfig();
      JSONArray ja = config.getField(si.fri.algator.entities.EAlgatorConfig.ID_Computers);
      // preglej, ali id računalnika že obstaja - potem ga ne moreš dodati
      if (ja != null)
        for (int i = 0; i < ja.length(); i++) {
          if (thisFamilyID.equals(((JSONObject) ja.get(i)).get(EComputer.ID_FamilyID))
                && thisComputerID.equals(((JSONObject) ja.get(i)).get(EComputer.ID_ComputerID))) {
            return sAnswer(2, "Error - computer with this ID already exists.", "");
          }
        }
      else {
        ja = new JSONArray();
        config.set(si.fri.algator.entities.EAlgatorConfig.ID_Computers, ja); 
      }

      // set the unique id for this computer (this will be the identifier in requests)
      computer.put(EComputer.ID_ComputerUID, AUsersTools.getUniqueDBid("c_"));
      ja.put(computer);
      config.saveEntity();

      JSONObject ans = new JSONObject();
      ans.put(EComputer.ID_ComputerID, computer.getString(EComputer.ID_ComputerID));
      ans.put(EComputer.ID_ComputerUID, computer.getString(EComputer.ID_ComputerUID));
      return jAnswer(OK_STATUS, "Computer added", ans.toString());
    } catch (Exception e) {
    }

    return sAnswer(1, "Error - bad request", "");
  }

  public String getProjectFiles(String uid, JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" property.");

    if (!jObj.has("Project")) {
      return errorAnswer;
    }

    String projName;
    try {
      projName = jObj.getString("Project");
    } catch (Exception e) {
      return errorAnswer;
    }
    
    String peid = EProject.getProject(projName).getEID();
    if (!CanUtil.can(uid, peid, "can_write")) return sAnswer(99, "getProjectFiles: " + accessDeniedString, accessDeniedString);

    String answer = ASTools.getProjectFiles(projName);
    if (answer.isEmpty())
      return sAnswer(2,String.format("Invalid project name ´%s´.", projName), "Project "+projName+" does not exist.");
      
    return sAnswer(OK_STATUS, "Project files", Base64.getEncoder().encodeToString(answer.getBytes())); 
  }
  
  public String getProjectList(String uid) {
    return sAnswer(OK_STATUS, "Project list", Base64.getEncoder().encodeToString(Project.getProjectsAsJSON(uid).toString().getBytes()));
  }
  
  public String getResultStatus(String uid, JSONObject jObj) {    
    String mType   = jObj.optString("MType", "");
    String project = jObj.optString("Project", "");
    if (project.isEmpty()) 
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" and \"MType\" (optional) property.");
        
    String peid = EProject.getProject(project).getEID();
    if (!CanUtil.can(uid, peid, "can_execute"))
      return sAnswer(99, "getResultStatus: " + accessDeniedString, accessDeniedString);
    
    JSONObject resultStatus = ASTools.getResultStatus(project, mType);
    statusResults.put(resultStatus.optString("AnswerID", "0"), resultStatus);
    
    String answer = resultStatus.toString();             
    return sAnswer(OK_STATUS, "Result status", Base64.getEncoder().encodeToString(answer.getBytes()));
  }
  
  public String getResultUpdate(String uid, JSONObject jObj) {

    String id = jObj.optString("ID", "");
    if (id.isEmpty())
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"ID\" property.");    
    
    JSONObject prevStatus = statusResults.get(id);
    if (prevStatus == null)
      return sAnswer(2, "No results", "Results for this id do not exist.");
    
    String project = prevStatus.optString("Project", "");

    String peid = EProject.getProject(project).getEID();
    if (!CanUtil.can(uid, peid, "can_execute"))
      return sAnswer(99, "getResultUpdate: " + accessDeniedString, accessDeniedString);

    JSONArray mTypes = null; try {mTypes = new JSONArray(prevStatus.get("MType"));} catch (Exception e) {}
    String mType     = mTypes != null && mTypes.length() == 1 ? mTypes.getString(0) : "";
    
    JSONObject currStatus = ASTools.getResultStatus(project, mType);
    currStatus.put("AnswerID", id);
    statusResults.put(id, currStatus);
    
    JSONArray prevResults = prevStatus.getJSONArray("Results");
    JSONArray currResults = currStatus.getJSONArray("Results");
    if (prevResults.length() != currResults.length()) {    
      // major change -- all data is sent
      return jAnswer(OK_STATUS, "Major", currStatus.toString());      
    }      
        
    JSONArray diff = new JSONArray();
    for (int i=0; i < prevResults.length(); i++) {
      if (!prevResults.get(i).toString().equals(currResults.get(i).toString())) {
        JSONObject jLine = new JSONObject();
        jLine.put("Line", i);
        jLine.put("Value", currResults.get(i));
        diff.put(jLine);
      }
    }
    if (diff.length() > 0)
      // minor change -- only some lines of data is sent
      return jAnswer(OK_STATUS, "Minor", diff.toString());
    else
      return sAnswer(OK_STATUS, "NO_CHANGES", "");
  }  

  public String getAWResults(String uid, JSONObject jObj) {     
    String project = jObj.optString("Project", "");    
    if (project.isEmpty()) 
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" and \"MType\" (optional) property.");

    String answerID = jObj.optString("AnswerID", "0");
        
    // check for correctness (readability) of algorithms and testsets and transform "*" into list
    // this is true only at first call (get) and not in subsequential calls (update)
    boolean isFirstRequest = true;
    
    // if answerID exists in awResults, this is subsequential call of getAWResults (update);
    // parameters of such calls are not passed in request but stored in awResults.
    if (ASTools.awResults.containsKey(answerID)) {
      jObj = ASTools.awResults.get(answerID);
      isFirstRequest = false;
    }

    // this can happen only in some odd cases; usually when answerID is given,
    // this is a subsequential call and old results should be available
    if (!"0".equals(answerID) && isFirstRequest) {
      return sAnswer(2, "getAWResults: ", "Results for " + answerID + " do not exist.");
    }
    
    JSONArray algorithms = jObj.optJSONArray("Algorithms"); if (algorithms == null) algorithms = new JSONArray();
    JSONArray testsets   = jObj.optJSONArray("Testsets");   if (testsets   == null) testsets   = new JSONArray();      
    String mType         = jObj.optString   ("MType", "em");
    
    String peid = EProject.getProject(project).getEID();
    if (!CanUtil.can(uid, peid, "can_read"))
      return sAnswer(99, "getAWResults: " + accessDeniedString, accessDeniedString);
    
    JSONObject resultStatus = ASTools.getAWResults(uid, project, algorithms, testsets, mType, isFirstRequest);

    if (isFirstRequest) // at first request, answerID is generated ...
      answerID = resultStatus.optString("AnswerID", "0");
    else  // .. and it is reused in all subsequential calls 
      resultStatus.put("AnswerID",   answerID);
    
    ASTools.awResults.put(answerID, resultStatus);

    if (!isFirstRequest) { // this is update request, send only changed results
      // make a clone of resultStatus
      JSONObject oldResults = (JSONObject)jObj.get("Results");
      JSONObject newResults = (JSONObject)resultStatus.get("Results");
      resultStatus = new JSONObject(resultStatus.toString());
      
      JSONObject updatedResults = new JSONObject();

      Iterator<String> resIt = newResults.keys();
      while (resIt.hasNext()) {
        String resultKey = resIt.next();
        Object resultValue = newResults.get(resultKey);
        if (!oldResults.has(resultKey) || !oldResults.get(resultKey).toString().equals(resultValue.toString()))
          updatedResults.put(resultKey, resultValue);
      } 
      
      resultStatus.put("Results", updatedResults); 
      resultStatus.remove("Algorithms");
      resultStatus.remove("Testsets");
      resultStatus.remove("Project");
      resultStatus.remove("MType");
    }
    return jAnswer(OK_STATUS, "AW Result status", resultStatus.toString());
  }
  
  
    // vsebino results datoteke lahko bere le nekdo, ki ima nad projektom "can_read" pravice
  public String getResultFile(String uid, JSONObject jObj) {
    String projName  = jObj.optString("Project",   "");
    String algorithm = jObj.optString("Algorithm", "");
    String testset   = jObj.optString("Testset",   "");
    String mt        = jObj.optString("MType",     "").toLowerCase();
    String compID    = jObj.optString("compID",    "");
    
    if (projName.isEmpty() || algorithm.isEmpty() || testset.isEmpty() || mt.isEmpty())
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\", \"Algorithm\", \"Testset\" and \"em\" properties.");

    String peid = EProject.getProject(projName).getEID();
    if (!CanUtil.can(uid, peid, "can_read")) return sAnswer(99, "getResultFile: " + accessDeniedString, accessDeniedString);
    
    Project p = new Project(ATGlobal.getALGatorDataRoot(), projName);
    
    String resultFileName;
    if (compID.isEmpty())
      resultFileName = ATTools.getTaskResultFileName(p, algorithm, testset, mt);
    else 
      resultFileName = ATGlobal.getRESULTfilename(p.getProjectRoot(), algorithm, testset, MeasurementType.mtOf(mt), compID);
    
    String answer = ASTools.getFileContent(resultFileName);
    
    String fileN = "";
    try {
      Path pth = Paths.get(resultFileName);
      fileN = pth.getParent().getFileName().toString() + "/" + pth.getFileName().toString();
    } catch (Exception e) {}
    
    JSONObject ans = new JSONObject();
    ans.put("Filename", fileN);
    ans.put("Content", Base64.getEncoder().encodeToString(answer.getBytes()));
    return jAnswer(OK_STATUS, "Result file", ans.toString()); 
  }

  public String admin(String uid, String params) {
    if (!CanUtil.can(uid, "e0_S", "full_control")) return sAnswer(99, "admin: " + accessDeniedString, accessDeniedString);
    String[] args = params.split(" ");
    String response = Admin.do_admin(args).trim();
    while (response.endsWith("\n")) {
      response = response.substring(0, response.length() - 1);
    }
    return response;
  }
}
