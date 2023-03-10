package si.fri.adeserver;

import algator.Admin;
import algator.Users;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algotest.analysis.DataAnalyser;
import si.fri.algotest.entities.EAlgatorConfig;
import si.fri.algotest.entities.EComputer;
import si.fri.algotest.entities.EComputerFamily;
import si.fri.algotest.entities.Entity;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ErrorStatus;
import si.fri.algotest.tools.ATTools;
import si.fri.algotest.tools.SortedArray;

/**
 * ALGator TaskServer connects ALGator with the world.
 *
 * For more information about server's requests and answers, see TaskServer.docx
 *
 * @author tomazž
 */
public class ADETaskServer implements Runnable {

  public final int OK_STATUS = 0;
  public static final String ID_STATUS = "Status";
  public static final String ID_MSG = "Message";
  public static final String ID_ANSWER = "Answer";

  // servers's answer contains Status (0 ... OK, >0 ... error), Message (error message) and Answer ()
  public final static String ANSWER = "{'" + ID_STATUS + "':%d, '" + ID_MSG + "':'%s', '" + ID_ANSWER + "': %s%s%s}";

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
    return String.format(ANSWER, status, msg, "'", answer, "'");
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

  // one array of tasks is shared by all servers
  public static SortedArray<STask> activeTasks;
  // v tej vrsti so taski, ki so že zaprti in niso starejši od enega tedna;
  // po enem tednu se iz te vrste odstranijo in se zapišejo na konec datoteke tasks.archived
  public static SortedArray<STask> closedTasks;

  // tasks that were canceled by user; taskID is put into this set when user sends "cancelTask" request
  // and it is removed from set when task is really canceled; this set is used only for INPROGRESS
  // tasks, since these tasks can not be canceled immedeately, but only when TaskClient contacts TaskServer
  // (with taskResult, closeTask or getTask request)
  private static TreeMap<Integer, TaskStatus> pausedAndCanceledTasks;

  private Socket connection;
  private int ID;  // server ID

  public ADETaskServer(Socket connection, int count) {
    // when a server is created, array of tasks is read from file
    if (activeTasks == null) {
      activeTasks = ADETools.readADETasks(0);
    }
    if (closedTasks == null) {
      closedTasks = ADETools.readADETasks(1);
    }

    if (pausedAndCanceledTasks == null) {
      pausedAndCanceledTasks = new TreeMap();
    }

    this.connection = connection;
    this.ID = count;
  }

  /**
   * Create a new task and add it to waiting queue. Call: addTask project_name
   * algorithm_name testset_name measurement_type Return: task_id or error
   * messsage if task can not be created.
   */
  public String addTask(JSONObject jObj) {
    boolean hasProject = jObj.has(STask.ID_Project) && !jObj.getString(STask.ID_Project).isEmpty();
    boolean hasAlgorithm = jObj.has(STask.ID_Algorithm) && !jObj.getString(STask.ID_Algorithm).isEmpty();
    boolean hasTestset = jObj.has(STask.ID_Testset) && !jObj.getString(STask.ID_Testset).isEmpty();
    if (!(hasProject && hasAlgorithm && hasTestset)) {
      return sAnswer(1, "Invalid parameter. Expecting JSON with \"Project\", \"Algorithm\" and \"Testset\" properties.", "");
    }

    String taskOK = ADETools.checkTaskAndGetFamily(jObj.getString(STask.ID_Project), jObj.getString(STask.ID_Algorithm),
            jObj.getString(STask.ID_Testset), jObj.getString(STask.ID_MType));
    if (!taskOK.startsWith("Family:")) {
      return sAnswer(2, "Invalid task. " + taskOK, "");
    }

    // here taskOK equals "Family:familyName" (which can be empty). If family in 
    // task is not explicitely set, we set it to "familyName" 
    if (!jObj.has(STask.ID_Family) || jObj.getString(STask.ID_Family).isEmpty()) {
      String fP[] = taskOK.split(":");
      jObj.put(STask.ID_Family, fP.length > 1 ? fP[1] : "");
    }

    STask task = new STask(jObj.toString());
    task.assignID();
    activeTasks.add(task);
    ADETools.setTaskStatus(task, TaskStatus.PENDING, null, null);

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
      compUID = jObj.getString(STask.ID_ComputerUID);
      taskID = jObj.getInt(STask.ID_TaskID);
      testNo = jObj.getInt("TestNo");
      result = jObj.getString("Result");
    } catch (Exception e) {
    }
    if (compUID.isEmpty() || taskID == 0 || testNo == 0 || result.isEmpty()) {
      return sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + STask.ID_ComputerUID + "\", \"" + STask.ID_TaskID + "\", \"TestNo\" and \"Result\" properties.");
    }

    STask task = ADETools.findTask(activeTasks, taskID);
    if (task != null) {
      // if task is OK and if it belongs to the computer with UID = compUID ...
      if (compUID.equals(task.getComputerUID())) {
        // ... set task progress ...
        task.setProgress(testNo);
        ADETools.writeADETasks(activeTasks, 0);

        // ... write results to result file ...
        String rsFilename = ADETools.getResultFilename(task);
        ATTools.createFilePath(rsFilename);
        try ( PrintWriter pw = new PrintWriter(new FileWriter(new File(rsFilename), true));) {
          pw.println(result);
        } catch (Exception e) {
        }

        // ... and respond to client: 
        // BREAK ... if user has paused  or canceled task   
        TaskStatus pausedOrCanceledTaskStatus = pausedAndCanceledTasks.get(task.getTaskID());
        if (pausedOrCanceledTaskStatus != null) {
          ADETools.setTaskStatus(task, pausedOrCanceledTaskStatus, null, null);
          pausedAndCanceledTasks.remove(task.getTaskID());
          return sAnswer(OK_STATUS, "Result accepted, task "
                  + (TaskStatus.CANCELED.equals(pausedOrCanceledTaskStatus) ? "canceled" : "paused"), "BREAK");
        }

        // CONTINUE  ... if this task is to be continued
        // QUEUED    ... if another task if more appropropriate to be executed by this client
        STask nextTask = ADETools.findFirstTaskForComputer(activeTasks, compUID, false);
        if (nextTask == null || (task.compare(nextTask) >= 0)) {
          return sAnswer(OK_STATUS, "Result accepted, continue.", "CONTINUE");
        } else {
          ADETools.setTaskStatus(task, TaskStatus.QUEUED, null, null);
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
    String errorAnswer = sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + STask.ID_TaskID + "\" property.");
    if (!jObj.has(STask.ID_TaskID)) {
      return errorAnswer;
    }

    int taskID;
    try {
      taskID = jObj.getInt(STask.ID_TaskID);
    } catch (Exception e) {
      return errorAnswer;
    }

    STask task = ADETools.findTask(activeTasks, taskID);
    if (task == null) {
      task = ADETools.findTask(closedTasks, taskID);
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
  private String getTask(JSONObject jObj) {
    if (!jObj.has(EComputer.ID_ComputerUID)) {
      return sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"" + EComputer.ID_ComputerUID + "\" property.");
    }

    String uid = jObj.getString(EComputer.ID_ComputerUID);
    String family = ADETools.familyOfComputer(uid);
    if ("?".equals(family)) {
      return sAnswer(2, ADEGlobal.NO_TASKS, "Invalid computer or computer family");
    }

    STask task = ADETools.findFirstTaskForComputer(activeTasks, uid, true);

    if (task != null) {
      ADETools.setTaskStatus(task, TaskStatus.INPROGRESS, null, uid);
      ADETools.setComputerFamilyForProject(task, family);
      return jAnswer(0, "Task for computer " + uid, task.toJSONString(0));
    } else {
      return sAnswer(3, ADEGlobal.NO_TASKS, "No tasks available for this computer.");
    }
  }

  /**
   * Removes task from activeTasks list and writes status to the file Call:
   * closeTask json(ExitCode, TaskID, Message) Return: "OK" or error message
   */
  private String closeTask(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"ExitCode\",  \"" + STask.ID_TaskID + "\" and \"Message\" properties.");

    if (!jObj.has("ExitCode") || !jObj.has(STask.ID_TaskID)) {
      return errorAnswer;
    }

    int taskID, exitCode;
    String msg;
    try {
      taskID = jObj.getInt(STask.ID_TaskID);
      exitCode = jObj.getInt("ExitCode");
      msg = jObj.getString("Message");
    } catch (Exception e) {
      return errorAnswer;
    }

    // find a task with a given ID in activeTasks queue    
    STask task = ADETools.findTask(activeTasks, taskID);
    if (task != null) {
      if (exitCode == 0) {
        ADETools.setTaskStatus(task, TaskStatus.COMPLETED, msg, null);
      } else if (exitCode == ErrorStatus.PROCESS_KILLED.ordinal()) {
        ADETools.setTaskStatus(task, TaskStatus.KILLED, msg, null);
      } else {
        ADETools.setTaskStatus(task, TaskStatus.FAILED, "Execution failed, : " + msg, null);
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
  private String changeTaskStatus(JSONObject jObj, TaskStatus newStatus) {
    String errorAnswer = sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with  \"" + STask.ID_TaskID + "\" property.");

    if (!jObj.has(STask.ID_TaskID)) {
      return errorAnswer;
    }

    int taskID;
    try {
      taskID = jObj.getInt(STask.ID_TaskID);
    } catch (Exception e) {
      return errorAnswer;
    }

    // find a task with a given ID in activeTasks queue    
    STask task = ADETools.findTask(activeTasks, taskID);
    if (task != null) {
      if (newStatus.equals(TaskStatus.UNKNOWN)) { // resumeTask called
        // if current status is PAUSED, task is revitalized (to PENDING or QUEUED)
        if (task.getTaskStatus().equals(TaskStatus.PAUSED)) {
          if (task.getComputerUID() != null && !task.getComputerUID().isEmpty()) {
            ADETools.setTaskStatus(task, TaskStatus.QUEUED, null, null);
          } else {
            ADETools.setTaskStatus(task, TaskStatus.PENDING, null, null);
          }
        } else {
          return iAnswer(3, "Can not resume  - task is not paused.", taskID);
        }
      } else { // newStatus == CANCELED or PAUSED
        if (task.getTaskStatus().equals(TaskStatus.INPROGRESS)) {
          pausedAndCanceledTasks.put(task.getTaskID(), newStatus);
        } else {
          ADETools.setTaskStatus(task, newStatus, null, null);
        }
      }

      return sAnswer(0, "OK", "Status changed");
    } else {
      return iAnswer(2, "Task not found.", taskID);
    }
  }

  /**
   * Method returns an array of results produced by a given query. At least two
   * parameters are required (projectName and queryName) all other parameters
   * are passed to the query as query parameters.
   */
  public String queryResult(ArrayList<String> params) {
    String result = "";
    if (params.size() < 2) {
      return "";
    }

    String[] queryParams = new String[params.size() - 2];
    for (int i = 2; i < params.size(); i++) {
      queryParams[i - 2] = params.get(i);
    }
    String computerID = null; // ne določim imena računalnika; če ta ne bo podan v poizvedbi, se bo izbrala "najbolj primerna" result datoteka

    // v poizvedbi se pred pošiljanjem vsi presledki nadomestijo z znakom _!_, da med prenosom ne pride do tezav (zmešnjava s parametri poizvedbe)
    String query = params.get(1).replaceAll("_!_", " ");
    return DataAnalyser.getQueryResultTableAsString(params.get(0), query, queryParams, computerID);
  }

  private String getServerRunningTime() {
    long seconds = (new Date().getTime() - timeStarted) / 1000;
    int day = (int) TimeUnit.SECONDS.toDays(seconds);
    long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
    long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
    long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

    String result = second + " sec.";
    if (minute > 0) {
      result = minute + " min" + ", " + result;
    }
    if (hours > 0) {
      result = hours + " h" + ", " + result;
    }
    if (day > 0) {
      result = day + " day(s)" + ", " + result;
    }

    return result;
  }

  private String getTasks(JSONObject jObj) {
    SortedArray<STask> tasks = null;

    String list = jObj.has("Type") ? jObj.getString("Type") : "";
    if (list.isEmpty() || "active".equals(list)) {
      tasks = activeTasks;
    }
    if ("closed".equals(list)) {
      tasks = closedTasks;
    } else if ("archived".equals(list)) {
      tasks = ADETools.readADETasks(2);
    }

    if (tasks == null) {
      return sAnswer(1, "Invalid type, 'active', 'closed' or 'archived' expected.", "");
    }

    StringBuilder sb = new StringBuilder();
    for (STask task : tasks) {
      String taskS = task.toJSONString(0);
      // change StatusDate and CreationDate from int to string representation
      taskS = ATTools.replaceDateL2S(taskS, "dd.MM.YY HH:mm:ss");

      sb.append((sb.length() > 0 ? ",\n" : "")).append(taskS);
    }
    return "[" + sb.toString() + "]";
  }

  private String serverStatus() {
    int p = 0, r = 0, pa = 0, q = 0;
    for (STask aDETask : activeTasks) {
      if (aDETask.getTaskStatus().equals(TaskStatus.PENDING)) {
        p++;
      }
      if (aDETask.getTaskStatus().equals(TaskStatus.INPROGRESS)) {
        r++;
      }
      if (aDETask.getTaskStatus().equals(TaskStatus.PAUSED)) {
        pa++;
      }
      if (aDETask.getTaskStatus().equals(TaskStatus.QUEUED)) {
        q++;
      }
    }

    return String.format("Server on for %s. Tasks: %d running, %d pending, %d queued, %d paused\n", getServerRunningTime(), r, p, q, pa);
  }

  private String getServerPaths() {
    return String.format("ALGATOR_ROOT=%s, DATA_ROOT=%s", ATGlobal.getALGatorRoot(), ATGlobal.getALGatorDataRoot());
  }

  private String getServerLog(ArrayList<String> params) {
    String nS;
    if (params.size() != 1) {
      nS = "10";
    } else {
      nS = params.get(0);
    }
    int n = 10;
    try {
      n = Integer.parseInt(nS);
    } catch (Exception e) {
    }

    return ADELog.getLog(n);
  }

  private String getFile(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" and \"File\" properties.");
    
    String projName, file;
    try {
      projName = jObj.getString("Project");
      file     = jObj.getString("File");
    } catch (Exception e) {
      return errorAnswer;
    }

    String answer = ADETools.getFileContent(projName, file);
    if (answer.startsWith("!!")) 
      return sAnswer(1, "Error reading file", answer.substring(2));
    else 
      return sAnswer(OK_STATUS, "File content", Base64.getEncoder().encodeToString(answer.getBytes())); 
  }
  
   private String saveFile(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\", \"File\", \"Content\" and \"Length\" properties.");
    
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
    
    String answer = ADETools.saveFile(projName, file, content);
    if (answer.startsWith("!!")) 
      return sAnswer(1, "Error saving file", answer.substring(2));
    else 
      return sAnswer(OK_STATUS, "OK", "File saved."); 
  }

  static String getJSONString(Entity ent, String... props) {
    String result = "";
    for (String prop : props) {
      String sp = "?";
      Object pp = ent.getField(prop);
      if (pp != null) {
        if (pp instanceof String) {
          sp = "'" + (String) pp + "'";
        }
        if (pp instanceof Integer) {
          sp = Integer.toString((Integer) pp);
        }
        if (pp instanceof Double) {
          sp = Double.toString((Double) pp);
        }
        if (pp instanceof JSONObject) {
          sp = ((JSONObject) pp).toString().replaceAll("\"", "'");
        }
        if (pp instanceof JSONArray) {
          sp = ((JSONArray) pp).toString().replaceAll("\"", "'");
        }
      }
      result += (result.isEmpty() ? "" : ", ") + String.format("'%s':%s", prop, sp);
    }
    return "{" + result + "}";
  }

  private String getTimeStamp() {
    return iAnswer(OK_STATUS, "Tmestamp", new Date().getTime());
  }

  private String getFamilies() {
    EAlgatorConfig config = EAlgatorConfig.getConfig();
    ArrayList<EComputerFamily> fam = config.getFamilies();
    String res = "";
    for (EComputerFamily eF : fam) {
      res += (res.isEmpty() ? "" : ", ") + getJSONString(eF, EComputerFamily.ID_FamilyID, EComputerFamily.ID_Desc, EComputerFamily.ID_Platform, EComputerFamily.ID_Hardware);
    }

    return jAnswer(OK_STATUS, "Computer families", "[" + res + "]");
  }

  private String addNewFamily(JSONObject jsonFamily) {
    String result = "Error - bad request";

    try {
      JSONObject family = new JSONObject(jsonFamily.toString());
      String thisFamilyID = family.getString(EComputerFamily.ID_FamilyID);
      if (thisFamilyID.contains(" ")) {
        return sAnswer(1, "Error - invalid character in FamilyID", "");
      }

      EAlgatorConfig config = EAlgatorConfig.getConfig();
      JSONArray ja = config.getField(si.fri.algotest.entities.EAlgatorConfig.ID_Families);
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
    }

    return sAnswer(1, result, "");
  }

  private String getComputers(JSONObject request) {
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
        res += (res.isEmpty() ? "" : ", ") + getJSONString(comp, EComputer.ID_ComputerUID, EComputer.ID_ComputerID, EComputer.ID_FamilyID, EComputer.ID_Desc, EComputer.ID_Capabilities);
      }
    }
    return jAnswer(OK_STATUS, "Computers", "[" + res + "]");
  }

  private String addNewComputer(JSONObject jsonComputer) {
    try {
      JSONObject computer = new JSONObject(jsonComputer.toString());
      String thisComputerID = computer.getString(EComputer.ID_ComputerID);
      String thisFamilyID = computer.getString(EComputer.ID_FamilyID);
      if (thisComputerID.contains(" ")) {
        return sAnswer(1, "Error - invalid character in ComputerID", "");
      }

      EAlgatorConfig config = EAlgatorConfig.getConfig();
      JSONArray ja = config.getField(si.fri.algotest.entities.EAlgatorConfig.ID_Computers);
      // preglej, ali id računalnika že obstaja - potem ga ne moreš dodati
      for (int i = 0; i < ja.length(); i++) {
        if (thisFamilyID.equals(((JSONObject) ja.get(i)).get(EComputer.ID_FamilyID))
                && thisComputerID.equals(((JSONObject) ja.get(i)).get(EComputer.ID_ComputerID))) {
          return sAnswer(2, "Error - computer with this ID already exists.", "");
        }
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

  private String getProjectFiles(JSONObject jObj) {
    String errorAnswer = sAnswer(1, ADEGlobal.ERROR_INVALID_NPARS, "Expecting JSON with \"Project\" property.");

    if (!jObj.has("Project")) {
      return errorAnswer;
    }

    String projName;
    try {
      projName = jObj.getString("Project");
    } catch (Exception e) {
      return errorAnswer;
    }

    String answer = ADETools.getProjectFiles(projName);
    if (answer.isEmpty())
      return sAnswer(2,String.format("Invalid project name ´%s´.", projName), "Project "+projName+" does not exist.");
      
    return sAnswer(OK_STATUS, "Project files", Base64.getEncoder().encodeToString(answer.getBytes())); 
  }

  private String processRequest(String request) {
    // najprej pocistim morebitne pozabljene ukaze in datoteke
    ADECommandManager.sanitize();

    String[] parts = request.split(" ");
    if (parts.length == 0) {
      return "";
    }

    // preverim, ali so parametri v obliki JSON niza
    JSONObject jObj = new JSONObject();
    if (parts.length > 1 && parts[1].trim().startsWith("{")) {
      int firstS = request.indexOf(" ");
      try {
        jObj = new JSONObject(request.substring(firstS).trim());
      } catch (Exception e) {
      }
    }

    parts[0] = parts[0].toUpperCase(); // request command

    // request parameters
    ArrayList<String> params = new ArrayList<>();
    for (int i = 1; i < parts.length; i++) {
      params.add(parts[i]);
    }

    switch (parts[0]) {

      // return my ID (ID of caller; taskClient's ID); no parameters
      case ADEGlobal.REQ_WHO:
        return Integer.toString(ID);

      // return the list of all tasks in the queue; no parameters  
      case ADEGlobal.REQ_GET_TASKS:
        return getTasks(jObj);

      // verifying server presence; no parameters
      case ADEGlobal.REQ_CHECK_Q:
        return ADEGlobal.REQ_CHECK_A;

      // prints server status; no parameters  
      case ADEGlobal.REQ_STATUS:
        return serverStatus();

      // appends task to the queue; parameters required: project algorithm testset mtype
      case ADEGlobal.REQ_ADD_TASK:
        return addTask(jObj);

      case ADEGlobal.REQ_CANCEL_TASK:
        return changeTaskStatus(jObj, TaskStatus.CANCELED);

      case ADEGlobal.REQ_PAUSE_TASK:
        return changeTaskStatus(jObj, TaskStatus.PAUSED);

      case ADEGlobal.REQ_RESUME_TASK:
        return changeTaskStatus(jObj, TaskStatus.UNKNOWN);

      // storest the result of a test and tells client what to do next
      case ADEGlobal.REQ_TASK_RESULT:
        return taskResult(jObj);

      // prints the status of given task; parameters required: taskID
      case ADEGlobal.REQ_TASK_STATUS:
        return taskStatus(jObj);

      // returns task (project algorithm testset mtype); paramaters: computerID  
      case ADEGlobal.REQ_GET_TASK:
        return getTask(jObj);

      // removes task from the queue; parameters: taskID  
      case ADEGlobal.REQ_CLOSE_TASK:
        return closeTask(jObj);

      case ADEGlobal.REQ_QUERY_RES:
        return queryResult(params);

      case ADEGlobal.REQ_ADMIN_PRINTPATHS:
        return getServerPaths();

      case ADEGlobal.REQ_ADMIN_PRINTLOG:
        return getServerLog(params);

      case ADEGlobal.REQ_USERS:
        return users(params);

      case ADEGlobal.REQ_ADMIN:
        return admin(params);

      case ADEGlobal.REQ_GETFILE:
        return getFile(jObj);

      case ADEGlobal.REQ_SAVEFILE:
        return saveFile(jObj);
        
      case ADEGlobal.COMMAND:
        return ADECommandManager.execute(params);

      case ADEGlobal.REQ_GETTIMESTAMP:
        return getTimeStamp();

      case ADEGlobal.REQ_GETFAMILIES:
        return getFamilies();

      case ADEGlobal.REQ_ADDFAMILY:
        return addNewFamily(jObj);

      case ADEGlobal.REQ_GETCOMPUTERS:
        return getComputers(jObj);

      case ADEGlobal.REQ_ADDCOMPUTER:
        return addNewComputer(jObj);

      case ADEGlobal.REQ_GETPFILES:
        return getProjectFiles(jObj);

      default:
        return ADEGlobal.getErrorString("Unknown request");
    }
  }

  private String users(ArrayList<String> params) {
    String[] args = new String[params.size()];
    for (int i = 0; i < params.size(); i++) {
      args[i] = params.get(i);
    }
    String response = Users.do_users(args).trim();
    while (response.endsWith("\n")) {
      response = response.substring(0, response.length() - 1);
    }

    return response;
  }

  private String admin(ArrayList<String> params) {
    String[] args = new String[params.size()];
    for (int i = 0; i < params.size(); i++) {
      args[i] = params.get(i);
    }
    String response = Admin.do_admin(args).trim();
    while (response.endsWith("\n")) {
      response = response.substring(0, response.length() - 1);
    }

    return response;
  }

  @Override
  public void run() {
    try ( Scanner sc = new Scanner(connection.getInputStream());  PrintWriter pw = new PrintWriter(connection.getOutputStream());) {

      while (sc.hasNextLine()) {
        String request = sc.nextLine();
        try {
          request = new String(Base64.getDecoder().decode(request));
        } catch (Exception e) {
          request = "";
        }

        String requestCommand = request.split(" ")[0];

        if (!ADEGlobal.nonlogableRequests.contains(requestCommand)) {
          ADELog.log("[REQUEST]:  " + request);
        }

        if (request.equals("Bye")) {
          pw.println("Byebye.");
          break;
        }

        String response = processRequest(request);
        //response = response.replaceAll("\r", "");   // nekateri nizi uporabljajo \r za lepši izpis, tu to odstranim, da ne moti       

        String responseC;
        try {
          responseC = Base64.getEncoder().encodeToString(response.getBytes());
        } catch (Exception e) {
          responseC = "<coding response problem>";
        }

        if (!ADEGlobal.nonlogableRequests.contains(requestCommand)) {
          ADELog.log(String.format("[RESPONSE]: %s", response.replaceAll("\n", "; ")));
        }

        pw.println(responseC);
        pw.flush();

        // some requests finish the communication imediately
        if (request.startsWith(ADEGlobal.REQ_STATUS)
                || request.startsWith(ADEGlobal.REQ_CHECK_Q)) {
          pw.close();
          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        connection.close();
      } catch (Exception e) {
        ADELog.log(ADEGlobal.ERROR_PREFIX + e.toString());
      }
    }
  }

  static long timeStarted;

  public static void runServer() {
    int count = 0;
    try {
      ServerSocket socket1 = new ServerSocket(EAlgatorConfig.getTaskServerPort());

      ADELog.log("TaskServer Initialized on " + socket1.getInetAddress().getHostAddress() + ":" + EAlgatorConfig.getTaskServerPort());

      timeStarted = new java.util.Date().getTime();
      while (true) {
        Socket connection = socket1.accept();
        Runnable runnable = new ADETaskServer(connection, ++count);
        Thread thread = new Thread(runnable);
        thread.start();
      }
    } catch (Exception e) {
      ADELog.log(ADEGlobal.ERROR_PREFIX + e.toString());
    }
  }

}
