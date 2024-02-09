package si.fri.algator.server;

import algator.Admin;
import algator.Users;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
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
  
  
  public String processRequest(String command, String pParams) {
    // najprej pocistim morebitne pozabljene ukaze in datoteke
    ASCommandManager.sanitize();

    // request command
    command = (command == null || command.isEmpty()) ? "?": command.toUpperCase(); 

    // preverim, ali so parametri v obliki JSON niza
    JSONObject jObj = new JSONObject();
    if (pParams.length() > 0 && pParams.trim().startsWith("{")) {
      try {
        jObj = new JSONObject(pParams.trim());
      } catch (Exception e) {}
    }

    switch (command) {

      // return my ID (ID of caller; taskClient's ID); no parameters
      case ASGlobal.REQ_WHO:
        return Long.toString(Thread.currentThread().getId());//Integer.toString(id);

      // verifying server presence; no parameters
      case ASGlobal.REQ_CHECK_Q:
        return ASGlobal.REQ_CHECK_A;

      // prints server status; no parameters  
      case ASGlobal.REQ_STATUS:
        return serverStatus();      
        
      case ASGlobal.REQ_ADMIN_PRINTPATHS:
        return getServerPaths();

      case ASGlobal.REQ_ADMIN_PRINTLOG:
        return getServerLog(pParams);
       
      case ASGlobal.REQ_GETTIMESTAMP:
        return getTimeStamp();
        
      case ASGlobal.REQ_GETDATA:
        return getData(jObj);  

      case ASGlobal.REQ_ALTER:
        return alter(jObj);          
        
      // return the list of all tasks in the queue; no parameters  
      case ASGlobal.REQ_GET_TASKS:
        return getTasks(jObj);

      // appends task to the queue; parameters required: project algorithm testset mtype
      case ASGlobal.REQ_ADD_TASK:
        return addTask(jObj);

      case ASGlobal.REQ_CANCEL_TASK:
        return changeTaskStatus(jObj, ASTaskStatus.CANCELED);

      case ASGlobal.REQ_PAUSE_TASK:
        return changeTaskStatus(jObj, ASTaskStatus.PAUSED);

      case ASGlobal.REQ_RESUME_TASK:
        return changeTaskStatus(jObj, ASTaskStatus.UNKNOWN);

      // storest the result of a test and tells client what to do next
      case ASGlobal.REQ_TASK_RESULT:
        return taskResult(jObj);

      // prints the status of given task; parameters required: taskID
      case ASGlobal.REQ_TASK_STATUS:
        return taskStatus(jObj);

      // returns task (project algorithm testset mtype); paramaters: computerID  
      case ASGlobal.REQ_GET_TASK:
        return getTask(jObj);

      // removes task from the queue; parameters: taskID  
      case ASGlobal.REQ_CLOSE_TASK:
        return closeTask(jObj);

      case ASGlobal.REQ_GETFILE:
        return getFile(jObj);

      case ASGlobal.REQ_SAVEFILE:
        return saveFile(jObj);

      case ASGlobal.REQ_QUERY_RES:
        return queryResult(pParams);   
        
      case ASGlobal.REQ_QUERY:
        return runQuery(jObj);   
        
      case ASGlobal.REQ_USERS:
        return users(pParams);

      case ASGlobal.REQ_ADMIN:
        return admin(pParams);
        
      case ASGlobal.COMMAND:
        return ASCommandManager.execute(pParams);

      case ASGlobal.REQ_GETFAMILIES:
        return getFamilies();

      case ASGlobal.REQ_ADDFAMILY:
        return addNewFamily(jObj);

      case ASGlobal.REQ_GETCOMPUTERS:
        return getComputers(jObj);

      case ASGlobal.REQ_ADDCOMPUTER:
        return addNewComputer(jObj);

      case ASGlobal.REQ_GETPFILES:
        return getProjectFiles(jObj);
        
      case ASGlobal.REQ_GETPROJECTLIST:
        return getProjectList();

      case ASGlobal.REQ_GETRESULTSTATUS:
        return getResultStatus(jObj);
        
      case ASGlobal.REQ_GETRESULTUPDATE:
        return getResultUpdate(jObj);
        
      default:
        return ASGlobal.getErrorString("Unknown request");
    }
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
    return String.format("Server at %s on for %s Thread: %d. Tasks: %d running, %d pending, %d queued, %d paused\n", 
       ip, server.getServerRunningTime(), Thread.currentThread().getId(), r, p, q, pa);
  }
  
  public String getTimeStamp() {
    return iAnswer(OK_STATUS, "Tmestamp", new Date().getTime());
  }


  public String getServerPaths() {
    return String.format("ALGATOR_ROOT=%s, DATA_ROOT=%s", ATGlobal.getALGatorRoot(), ATGlobal.getALGatorDataRoot());
  }
  
  
  public String getServerLog(String params) {
    int n = 10;
    try {
      n = Integer.parseInt(params);
    } catch (Exception e) {}

    return ASLog.getLog(n);
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
  public String getData(JSONObject jObj) {
    if (!jObj.has("Type")) 
      return sAnswer(1, "Invalid parameter. Expecting JSON with \"Type\" property.", "");

    String type = jObj.getString("Type");
    switch (type) {
    
      case "Projects":
        return ASTools.getProjectsData();
      case "Project":
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=Project requires 'ProjectName' property.", "");
        return ASTools.getProjectData(jObj.getString("ProjectName"));
      case "Algorithm":
        if (!(jObj.has("ProjectName")&& jObj.has("AlgorithmName")))
          return sAnswer(1, "getData of type=Algorithm requires 'ProjectName' and 'AlgorithmName' properties.", "");
        return ASTools.getAlgorithmData(jObj.getString("ProjectName"), jObj.getString("AlgorithmName"));
      case "Presenter":
        if (!(jObj.has("ProjectName")&&jObj.has("PresenterName")))
          return sAnswer(1, "getData of type=Presenters requires 'ProjectName' and 'PresenterName' properties.", "");
        return ASTools.getPresenter(jObj.getString("ProjectName"), jObj.getString("PresenterName"));
        
      case "ProjectSources": // project sources
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=ProjectSources requires 'ProjectName' property.", "");
        return ASTools.getProjectSources(jObj.getString("ProjectName"));
      
      case "ProjectProps": // data for query form (algs, testsets, parameters, indicators)
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=ProjectProps requires 'ProjectName' property.", "");
        return ASTools.getProjectProps(jObj.getString("ProjectName"));

      case "ProjectDocs": // all html files and list of resources
        if (!jObj.has("ProjectName"))
          return sAnswer(1, "getData of type=ProjectDocs requires 'ProjectName' property.", "");
        return ASTools.getProjectDocs(jObj.getString("ProjectName"));
        
      case "ProjectResource": // a resource file
        if (!(jObj.has("ProjectName") && jObj.has("ResourceName")))
          return sAnswer(1, "getData of type=ProjectResource requires 'ProjectName' and 'ResourceName' properties.", "");
        return ASTools.getProjectResource(jObj.getString("ProjectName"), jObj.getString("ResourceName"));                
      
      default: return sAnswer(1, "Unknown type '"+type+"'.", "");
    }    
  }
  
  public String alter(JSONObject jObj) {
    if (!(jObj.has("Action")&&jObj.has("ProjectName")))
      return sAnswer(1, "Invalid parameter. Expecting JSON with 'Action' and 'ProjectName' properties.", "");
    
    // pri vsaki zahtevi "alter" se sinhroniziram na projekt (na ime root folderja)
    String projRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), jObj.getString("ProjectName"));
    if (!synchronizators.containsKey(projRoot)) synchronizators.put(projRoot, projRoot);
    String syncObject = synchronizators.get(projRoot);
    synchronized (syncObject) {          
      String action = jObj.getString("Action");
      switch (action) { 
      
        case "NewPresenter":
          if (!(jObj.has("ProjectName")&&jObj.has("PresenterType")))
            return sAnswer(1, "Alter of type=NewPresenters requires 'ProjectName' and 'PresenterType' properties.", "");
          return ASTools.newPresenter(jObj.getString("ProjectName"), jObj.getInt("PresenterType"));
          
        case "RemovePresenter":
          if (!(jObj.has("ProjectName")&&jObj.has("PresenterName")))
            return sAnswer(1, "Alter of type=RemovePresenters requires 'ProjectName' and 'PresenterName' properties.", "");
          return ASTools.removePresenter(jObj.getString("ProjectName"), jObj.getString("PresenterName"));
  
        case "SavePresenter":
          if (!(jObj.has("ProjectName")&&jObj.has("PresenterName")&&jObj.has("PresenterData")))
            return sAnswer(1, "Alter of type=SavePresenters requires 'ProjectName', 'PresenterName' and 'PresenterData' properties.", "");
          return ASTools.savePresenter(jObj.getString("ProjectName"), jObj.getString("PresenterName"), jObj.getJSONObject("PresenterData"));
  
        case "NewIndicator":
          if (!(jObj.has("ProjectName")&&jObj.has("IndicatorName")))
            return sAnswer(1, "Alter of type=NewIndicator requires 'ProjectName', 'IndicatorName', 'IndicatorType' (optional, default='indicator') and 'Meta' (optional, default:{}) properties.", "");
          JSONObject meta =           jObj.optJSONObject("Meta"); if (meta==null) meta = new JSONObject();
          return ASTools.newIndicator(jObj.getString("ProjectName"), jObj.getString("IndicatorName"), jObj.optString("IndicatorType", "indicator"), meta);        
  
        case "RemoveIndicator":
          if (!(jObj.has("ProjectName")&&jObj.has("IndicatorName")))
            return sAnswer(1, "Alter of type=RemoveIndicator requires 'ProjectName' 'IndicatorName' and 'IndicatorType' (optional, default='indicator') properties.", "");
          return ASTools.removeIndicator(jObj.getString("ProjectName"), jObj.getString("IndicatorName"), jObj.optString("IndicatorType", "indicator"));        
  
        case "SaveIndicator":
          if (!(jObj.has("ProjectName")&&jObj.has("Indicator")))
            return sAnswer(1, "Alter of type=SaveIndicator requires 'ProjectName' 'Indicator' and 'IndicatorType' (optional, default='indicator') properties.", "");
          return ASTools.saveIndicator(jObj.getString("ProjectName"), jObj.getJSONObject("Indicator"), jObj.optString("IndicatorType", "indicator"));                
         
        case "NewGenerator":
          if (!(jObj.has("ProjectName")&&jObj.has("GeneratorName")))
            return sAnswer(1, "Alter of type=NewGenerator requires 'ProjectName', 'GeneratorName' and 'GeneratorParameters' (optional, default:[]) properties.", "");
          JSONArray genParams =           jObj.optJSONArray("GeneratorParameters"); if (genParams==null) genParams = new JSONArray();
          return ASTools.newGenerator(jObj.getString("ProjectName"), jObj.getString("GeneratorName"), genParams);        
          
        default: return sAnswer(1, "Unknown type '"+action+"'.", "");        
      }
    }
  }
  
  
  /**
   * Create a new task and add it to waiting queue. Call: addTask project_name
   * algorithm_name testset_name measurement_type Return: task_id or error
   * messsage if task can not be created.
   */
  public String addTask(JSONObject jObj) {
    boolean hasProject = jObj.has(ASTask.ID_Project) && !jObj.getString(ASTask.ID_Project).isEmpty();
    boolean hasAlgorithm = jObj.has(ASTask.ID_Algorithm) && !jObj.getString(ASTask.ID_Algorithm).isEmpty();
    boolean hasTestset = jObj.has(ASTask.ID_Testset) && !jObj.getString(ASTask.ID_Testset).isEmpty();
    if (!(hasProject && hasAlgorithm && hasTestset)) {
      return sAnswer(1, "Invalid parameter. Expecting JSON with \"Project\", \"Algorithm\" and \"Testset\" properties.", "");
    }

    String mType = jObj.has(ASTask.ID_MType) ? jObj.getString(ASTask.ID_MType) : MeasurementType.EM.toString();
    String taskOK = ASTools.checkTaskAndGetFamily(jObj.getString(ASTask.ID_Project), jObj.getString(ASTask.ID_Algorithm),
            jObj.getString(ASTask.ID_Testset), mType);
    if (!taskOK.startsWith("Family:")) {
      return sAnswer(2, "Invalid task. " + taskOK, "");
    }

    // here taskOK equals "Family:familyName" (which can be empty). If family in 
    // task is not explicitely set, we set it to "familyName" 
    if (!jObj.has(ASTask.ID_Family) || jObj.getString(ASTask.ID_Family).isEmpty()) {
      String fP[] = taskOK.split(":");
      jObj.put(ASTask.ID_Family, fP.length > 1 ? fP[1] : "");
    }

    ASTask task = new ASTask(jObj.toString());
    task.assignID();
    activeTasks.add(task);
    ASTools.setTaskStatus(task, ASTaskStatus.PENDING, null, null);

    return jAnswer(OK_STATUS, "Task added", task.toJSONString(0));
  }

  /**
   * Odjemalec po koncu vsakega testa strežniku pošlje zahtevo "taskresuilt" s
   * parametri json(client:uid, task:taskID, result:json_result, testNo:
   * testNo).
   *
   * Strežnik -> nastavi task.progress=testNo -> result shrani v datoteko ->
   * poišlče najbolj "vroč" naslednji task za odjemalca (nextTast) -> če
   * nextTask == currentTask ... pošlje CONTINUE sicer ... task.status nastavi
   * na QUEUED in pošlje BREAK
   *
   * Odjemalec: -> če prejme CONTINUE, nadaljuje z izvajanjem testa -> će prejme
   * BREAK (kar je lahko posledica prioritete taskov ali dejstva, da je bil task
   * prekinjen (CANCELED) ali dan na čakanje (PAUSED)) ... prekine izvajanje
   * testseta in kontrolo preda metodi runClient(); ta bo strežnik vprašala za
   * naslednje opravilo (getTask).
   */
  public String taskResult(JSONObject jObj) {
    String compUID = "";
    int taskID = 0;
    int testNo = 0;
    String result = "";
    try {
      compUID = jObj.getString(ASTask.ID_ComputerUID);
      taskID = jObj.getInt(ASTask.ID_TaskID);
      testNo = jObj.getInt("TestNo");
      result = jObj.getString("Result");
    } catch (Exception e) {
    }
    if (compUID.isEmpty() || taskID == 0 || testNo == 0 || result.isEmpty()) {
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + ASTask.ID_ComputerUID + "\", \"" + ASTask.ID_TaskID + "\", \"TestNo\" and \"Result\" properties.");
    }

    ASTask task = ASTools.findTask(activeTasks, taskID);
    if (task != null) {
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
        } catch (Exception e) {
        }

        // ... and respond to client: 
        // BREAK ... if user has paused  or canceled task   
        ASTaskStatus pausedOrCanceledTaskStatus = pausedAndCanceledTasks.get(task.getTaskID());
        if (pausedOrCanceledTaskStatus != null) {
          ASTools.setTaskStatus(task, pausedOrCanceledTaskStatus, null, null);
          pausedAndCanceledTasks.remove(task.getTaskID());
          return sAnswer(OK_STATUS, "Result accepted, task "
                  + (ASTaskStatus.CANCELED.equals(pausedOrCanceledTaskStatus) ? "canceled" : "paused"), "BREAK");
        }

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
    } else {
      return iAnswer(2, "Invalid task number", taskID);
    }
  }

  public String taskStatus(JSONObject jObj) {
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
      return task.toJSONString(0);
    }

    return iAnswer(2, "Unknown task.", taskID);
  }

  /**
   * Finds the next task that can be executed on a computer with given cid    <br>
   * Call: getTask {ComputerID: cid}                                                    <br>
   * Return: NO_TASKS or Task (json(id proj alg testset mtype, progress))
   */
  public String getTask(JSONObject jObj) {
    if (!jObj.has(EComputer.ID_ComputerUID)) {
      return sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + EComputer.ID_ComputerUID + "\" property.");
    }

    String uid = jObj.getString(EComputer.ID_ComputerUID);
    String family = ASTools.familyOfComputer(uid);
    if ("?".equals(family)) {
      return sAnswer(2, ASGlobal.NO_TASKS, "Invalid computer or computer family");
    }

    ASTask task = ASTools.findFirstTaskForComputer(activeTasks, uid, true);

    if (task != null) {
      ASTools.setTaskStatus(task, ASTaskStatus.INPROGRESS, null, uid);
      ASTools.setComputerFamilyForProject(task, family);
      return jAnswer(0, "Task for computer " + uid, task.toJSONString(0));
    } else {
      return sAnswer(3, ASGlobal.NO_TASKS, "No tasks available for this computer.");
    }
  }

  /**
   * Removes task from activeTasks list and writes status to the file. Call:
   * closeTask json(ExitCode, TaskID, Message) Return: "OK" or error message
   */
  public String closeTask(JSONObject jObj) {
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

  /**
   * Changes status - from INPROGRESS, PENDING, QUEUED -- to --> PAUSED or
   * CANCELED - from PAUSED -- to --> PENDING or QUEUED (in this case newStatus
   * is UNKNOWN) Call: pauseTask | cancelTask | resumeTask json(TaskID) Return:
   * "OK" (0) or error message
   */
  public String changeTaskStatus(JSONObject jObj, ASTaskStatus newStatus) {
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
    try {exitCode = jObj.getInt(ASTask.ID_TaskID);} catch (Exception e) {}

    // find a task with a given ID in activeTasks queue    
    ASTask task = ASTools.findTask(activeTasks, taskID);
    if (task != null) {
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
          if (exitCode == 0) // normal "cancelTask" 
            pausedAndCanceledTasks.put(task.getTaskID(), newStatus);
          else { // "cancelTask"  invoked by TaskClient due to execution error 
            ASTools.setTaskStatus(task, newStatus, jObj.optString("Message", ""), null);
          } 
        } else {
          ASTools.setTaskStatus(task, newStatus, null, null);
        }
      }

      return sAnswer(0, "OK", "Status changed");
    } else {
      return iAnswer(2, "Task not found.", taskID);
    }
  }
  
  public String getTasks(JSONObject jObj) {
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

    if (tasks == null) {
      return sAnswer(1, "Invalid type, 'active', 'closed' or 'archived' expected.", "");
    }

    StringBuilder sb = new StringBuilder();
    for (ASTask task : tasks) {
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
  public String queryResult(String params) {
    String parts[] = params.split(" ", 3); // pricakujem "projectName queryName <params>"
    if (parts.length < 2) {
      return ""; // required params missing
    }

    String[] queryParams = parts.length > 2 ? parts[2].split(" ") : new String[0];
    String computerID = null; // ne določim imena računalnika; če ta ne bo podan v poizvedbi, se bo izbrala "najbolj primerna" result datoteka

    // v poizvedbi se pred pošiljanjem vsi presledki nadomestijo z znakom _!_, da med prenosom ne pride do tezav (zmešnjava s parametri poizvedbe)
    String query = parts[1].replaceAll("_!_", " ");
    return DataAnalyser.getQueryResultTableAsString(parts[0], query, queryParams, computerID);
  }
  

  /**
   * Method returns a String of results produced by a given query. 
   */  
  public String runQuery(JSONObject jObj) {
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

    EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projName)));
    EQuery   eQuery = new EQuery(query, params);
    
    return sAnswer(OK_STATUS, "Query result", DataAnalyser.runQuery(eProject, eQuery, compID).toString());
  }


  

  public String getFile(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" and \"File\" properties.");
    
    String projName, file;
    try {
      projName = jObj.getString("Project");
      file     = jObj.getString("File");
    } catch (Exception e) {
      return errorAnswer;
    }

    String answer = ASTools.getFileContent(projName, file);
    if (answer.startsWith("!!")) 
      return sAnswer(1, "Error reading file", answer.substring(2));
    else 
      return sAnswer(OK_STATUS, "File content", Base64.getEncoder().encodeToString(answer.getBytes())); 
  }
  
  public String saveFile(JSONObject jObj) {
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
    if (content.length() != len)
      return sAnswer(2, "File save error", "File content length mismatch");

    content = new String(Base64.getDecoder().decode(content));
    
    String answer = ASTools.saveFile(projName, file, content);
    if (answer.startsWith("!!")) 
      return sAnswer(1, "Error saving file", answer.substring(2));
    else 
      return sAnswer(OK_STATUS, "OK", "File saved."); 
  }

  
  public String getFamilies() {
    EAlgatorConfig config = EAlgatorConfig.getConfig();
    ArrayList<EComputerFamily> fam = config.getFamilies();
    String res = "";
    for (EComputerFamily eF : fam) {
      res += (res.isEmpty() ? "" : ", ") + ASTools.getJSONString(eF, EComputerFamily.ID_FamilyID, EComputerFamily.ID_Desc, EComputerFamily.ID_Platform, EComputerFamily.ID_Hardware);
    }

    return jAnswer(OK_STATUS, "Computer families", "[" + res + "]");
  }

  public String addNewFamily(JSONObject jsonFamily) {
    String result = "Error - bad request";

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

    return sAnswer(1, result, "");
  }

  public String getComputers(JSONObject request) {
    String familyID = "";
    try {
      familyID = request.getString("FamilyID");
    } catch (Exception e) {
    }

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

  public String addNewComputer(JSONObject jsonComputer) {
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

      // set the unique id for this computer (this wil be the identifier in requests)
      computer.put(EComputer.ID_ComputerUID, RandomStringUtils.random(10, true, true));
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

  public String getProjectFiles(JSONObject jObj) {
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

    String answer = ASTools.getProjectFiles(projName);
    if (answer.isEmpty())
      return sAnswer(2,String.format("Invalid project name ´%s´.", projName), "Project "+projName+" does not exist.");
      
    return sAnswer(OK_STATUS, "Project files", Base64.getEncoder().encodeToString(answer.getBytes())); 
  }
  
  public String getProjectList() {
    return sAnswer(OK_STATUS, "Project list", Base64.getEncoder().encodeToString(Project.getProjectsAsJSON().toString().getBytes()));
  }
  
  public String getResultStatus(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" and \"MType\" (optional) property.");

    String mType="", project;
    try {
      project = jObj.getString("Project");
    } catch (Exception e) {
      return errorAnswer;
    }
    if (jObj.has("MType")) 
      mType = jObj.getString("MType");
    
    JSONObject resultStatus = ASTools.getResultStatus(project, mType);
    statusResults.put(resultStatus.optString("AnswerID", "0"), resultStatus);
    
    String answer = resultStatus.toString();             
    return sAnswer(OK_STATUS, "Result status", Base64.getEncoder().encodeToString(answer.getBytes()));
  }
  
  public String getResultUpdate(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ASGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"ID\" property.");

    String id;
    try {
      id = jObj.getString("ID");
    } catch (Exception e) {
      return errorAnswer;
    }
    
    JSONObject prevStatus = statusResults.get(id);
    if (prevStatus == null)
      return sAnswer(2, "No results", "Results for this id do not exist.");
    
    String project = prevStatus.optString("Project", "");
    JSONArray mTypes = null; try {mTypes = new JSONArray(prevStatus.get("MType"));} catch (Exception e) {}
    String mType     = mTypes != null && mTypes.length() == 1 ? mTypes.getString(0) : "";
    
    JSONObject currStatus = ASTools.getResultStatus(project, mType);
    currStatus.put("AnswerID", id);
    statusResults.put(id, currStatus);
    
    JSONArray prevResults = prevStatus.getJSONArray("Results");
    JSONArray currResults = currStatus.getJSONArray("Results");
    if (prevResults.length() != currResults.length()) {    
      // major change -- all data is sent
      return sAnswer(OK_STATUS, "Major", currStatus.toString());      
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
    // minor change -- only some lines of data is sent
    return sAnswer(OK_STATUS, "Minor", diff.toString());
  }  
  
  public String users(String params) {
    String[] args = params.split(" ");
    String response = Users.do_users(args).trim();
    while (response.endsWith("\n")) {
      response = response.substring(0, response.length() - 1);
    }
    return response;
  }

  public String admin(String params) {
    String[] args = params.split(" ");
    String response = Admin.do_admin(args).trim();
    while (response.endsWith("\n")) {
      response = response.substring(0, response.length() - 1);
    }
    return response;
  }
}
