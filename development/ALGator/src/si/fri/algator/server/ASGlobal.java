package si.fri.algator.server;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.TreeSet;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class ASGlobal {
  public static final String    DEFAULT_TASK_SERVER_NAME  = "localhost";
  public static final int       DEFAULT_TASK_SERVER_PORT  = 12321;  
  public static final int       DEFAULT_RSYNC_SERVER_PORT = 12322;  
  
  /*
   * REQUESTS
   */
  public static final String REQ_WHO              = "WHO";            // no parameters
  public static final String REQ_STATUS           = "STATUS";         // no parameters

  
  public static final String REQ_GET_TASKS        = "GETTASKS";       // parameters: optional json(type=active,closed,archived)
  public static final String REQ_ADD_TASK         = "ADDTASK";        // parameters: json(project(required), algorithm(required), testset(required), mtype (default:em), priority (default: 5), family (default: ""))
  public static final String REQ_GET_TASK         = "GETTASK";        // parameters: json(computerID)
  public static final String REQ_CLOSE_TASK       = "CLOSETASK";      // parameters: json(ExitCode, TaskId, Message)
  public static final String REQ_TASK_STATUS      = "TASKSTATUS";     // parameters: json(TaskID)
  public static final String REQ_PAUSE_TASK       = "PAUSETASK";      // parameters: json(TaskID)
  public static final String REQ_CANCEL_TASK      = "CANCELTASK";     // parameters: json(TaskID)
  public static final String REQ_RESUME_TASK      = "RESUMETASK";     // parameters: json(TaskID)
  public static final String REQ_TASK_RESULT      = "TASKRESULT";     // parameters: json(ComputerUID, TaskID, TestNo, Result)
 
  public static final String REQ_QUERY_RES        = "GETQUERYRESULT"; // parameters: projectName
  public static final String REQ_QUERY            = "QUERY";          // parameters: json(projectName, query, ComputerID, Parameters)
  public static final String REQ_ADMIN_PRINTPATHS = "PRINTPATHS";     // no parameters
  public static final String REQ_ADMIN_PRINTLOG   = "PRINTLOG";       // parameters: numberOfLogs (default: 10)
  public static final String REQ_USERS            = "USERS";          // users (povezava do programa algator.Users)
  public static final String REQ_ADMIN            = "ADMIN";          // admin (povezava do programa algator.Admin)  
  public static final String REQ_GETFILE          = "GETFILE";        // parameters: json(ProjectName, fileName), result: file content 
  public static final String REQ_SAVEFILE         = "SAVEFILE";       // parameters: json(ProjectName, fileName, length, content)
  
  public static final String REQ_GETFAMILIES      = "GETFAMILIES";    // no parametes
  public static final String REQ_ADDFAMILY        = "ADDFAMILY";      // paremater: json object
  public static final String REQ_GETCOMPUTERS     = "GETCOMPUTERS";   // parameter: 'FamilyID' 
  public static final String REQ_ADDCOMPUTER      = "ADDCOMPUTER";    // paremater: json object
  
  public static final String REQ_GETTIMESTAMP     = "GETTIMESTAMP";   // no parametes
   
  public static final String REQ_GETPFILES        = "GETPFILES";      // parameter: json (Project)
  public static final String REQ_GETPROJECTLIST   = "GETPROJECTLIST"; // no parametes
  public static final String REQ_GETRESULTSTATUS  = "GETRESULTSTATUS"; // json (Project, mType)
  public static final String REQ_GETRESULTUPDATE  = "GETRESULTUPDATE"; // json (ID)
  
  public static final String REQ_GETDATA          = "GETDATA";         // json (Type, ...)
  public static final String REQ_ALTER            = "ALTER";           // json (Type, ...)
  
  // dodana funkcionalnost za poganjanje ALGator ukazov (execute, analyse, ...) v svojem procesu. Za podrobnosti glej komentar v ADECommand
  public static final String COMMAND              = "COMMAND";        // parameters: run / status / output / stop + params
  
  // a set of requests that do not log into log file
  public static final TreeSet<String> nonlogableRequests;
  static {
    nonlogableRequests = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    //nonlogableRequests.add(REQ_GET_TASK);        
    //nonlogableRequests.add(REQ_STATUS);
    //nonlogableRequests.add(REQ_WHO);
    //nonlogableRequests.add(REQ_LIST);    
    //nonlogableRequests.add(REQ_TASK_STATUS);
    //nonlogableRequests.add(REQ_ADMIN_PRINTLOG);
    //nonlogableRequests.add(REQ_ADMIN_PRINTPATHS);
  }
  

  /**
   * ALGatorServer healthy checking question and answer
   */
  public static final String REQ_CHECK_Q        = "HELLO";
  public static final String REQ_CHECK_A        = "ALGatorServer status: OK";
  
  /*
   * ERROR string  (the error message between client/server always starts with this string
   */
  public static final String ERROR_PREFIX  = "ERROR:: ";
  
  // the answer string, if on tasks is available for a given computer
  public static final String NO_TASKS = "NO_TASKS";
  
  public static final String ERROR_SERVER_DOWN  = "ERROR:: Server down - ";
  
  
  public static final String ERROR_INVALID_NPARS     = "Invalid number or type of parameters";
  public static final String ERROR_ERROR_CREATE_TASK = "Error occured when creating a task";
  
  // if a string holds more than one information, data if separated by STRING_DELIMITER
  public static final String STRING_DELIMITER  = " ";
  
  private static final String ALGATORSERVER_LOG_FOLDER    = "server";
  private static final String TASK_ID_FILENAME            = "task.number";
  private static final String TASK_LIST_FILENAME          = "task.";
  private static final String ALGATORSERVER_LOG_FILENAME  = "server.log";
  

  public static String getALGatorServerLogFolder() {
    String adeFolderName = ATGlobal.getLogFolder() + File.separator + ALGATORSERVER_LOG_FOLDER;
    File adeFolder       = new File(adeFolderName);
    if (!adeFolder.exists())
      adeFolder.mkdirs();
    
    return adeFolderName;
  }
   
  /**
   * Returns task file name.
   * @param type 0 ... active tasks, 1 ... closed tasks, 2 ... archived tasks
   * @return 
   */
  public static String getADETasklistFilename(int type) {
    String suffix;
    switch (type) {
      case 1 : suffix="closed"; break;
      case 2 : suffix="archived";break;
      default: suffix="active";break;
    }
    return  getALGatorServerLogFolder() + File.separator + TASK_LIST_FILENAME + suffix;
  }

  public static String getALGatorServerLogFilename() {
    return getALGatorServerLogFolder() + File.separator + ALGATORSERVER_LOG_FILENAME;
  }
 
  public static int getNextTaskID() {
    try {
     File folder = new File(getALGatorServerLogFolder());
     File file   = new File(folder, TASK_ID_FILENAME);
     
     int taskID = 0;
     if (file.exists()) {
       try (Scanner sc = new Scanner(file)) {
         taskID = sc.nextInt();
       }
     }
     try (PrintWriter pw = new PrintWriter(file)) {
       pw.println(++taskID);   
     }
     return taskID;
    } catch (Exception e) {
      return 0;
    }
  } 
  
  public static boolean isError(String msg) {
    return msg.startsWith(ERROR_PREFIX);
  }
  public static String getErrorString(String errorMsg) {
    return ERROR_PREFIX + errorMsg;
  }
  public static String getErrorMsg(String errorMsg) {
    if (isError(errorMsg)) 
      return errorMsg.substring(ERROR_PREFIX.length());
    else
      return "";
  }
  
}
