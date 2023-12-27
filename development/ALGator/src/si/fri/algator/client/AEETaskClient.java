package si.fri.algator.client;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.server.ASGlobal;
import si.fri.algator.entities.EComputerFamily;
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;
        
import static si.fri.algator.server.ASTools.ID_STATUS;
import static si.fri.algator.server.ASTools.ID_ANSWER;
import static si.fri.algator.server.ASTools.ID_MSG;
import static si.fri.algator.server.ASTools.decodeAnswer;
import si.fri.algator.server.ASTask;
import si.fri.algator.entities.CompCap;
import si.fri.algator.entities.EComputer;

/**
 *
 * @author tomaz
 */
public class AEETaskClient {
  private static String badRequestMsg(String request, JSONObject jAns) {
    return String.format("Bad request: '%s'. Return status: %d, Return message '%s'", 
            request, jAns.getInt(ID_STATUS), jAns.getString(ID_MSG));
  }
  
  /**
   * This method is used by TaskClient after the instalation to set the information about the server 
   * (name and port) and to obtain the FamilyID and ComputerID for this computer. 
   *   - ALGatorServerName, ALGatorServerPort
   *   - FamilyID/ComputerID of this computer
   */
  static void initTaskClient() {
    System.out.println("ALGator TaskClient configuration\n");
    
    ELocalConfig lconfig = ELocalConfig.getConfig();
    
    Scanner sc = new Scanner(System.in);
    
    // ALGatorServer connection info
    boolean serverSet = false;String server=""; int port=0;
    
    System.out.println("1) Information about ALGatorServer that is going to be used by this TaskClient");   
    while (!serverSet) {
      // ... servername 
      server = lconfig.getALGatorServerName();
        System.out.print(String.format("  Enter the name (or IP) of the ALGatorServer [%s]: ", server));
        String nServer = sc.nextLine().trim();
        server = nServer.isEmpty() ? server : nServer;
      // ... server port
      port   = lconfig.getALGatorServerPort();         
        System.out.print(String.format("  Enter the port number of the ALGatorServer [%d]: ", port));
        String sPort = sc.nextLine().trim();
        int nPort = 0;
        try {nPort = Integer.parseInt(sPort);} catch (Exception e) {}
        if (nPort != 0) port = nPort;
        
      System.out.println(String.format("  ... connecting to ALGatorServer (%s:%d)", server,port));
      String serverAns = Requester.askALGatorServer(server, port, ASGlobal.REQ_STATUS);
      if (serverAns.startsWith("Error")) {
        System.out.println("   ... " + serverAns);      
        System.out.println();
      } else serverSet=true;
    }
    lconfig.setALGatorServerName(server);
    lconfig.setALGatorServerPort(port);
    lconfig.saveEntity();
    System.out.println("  ... OK; information saved\n");


    String[] cID = lconfig.getComputerID().split("[.]");
    String computerID  = cID.length > 1 ? cID[1].trim():"";    
    String familyID    = cID.length > 0 ? cID[0].trim():"";
    String computerUID = lconfig.getComputerUID().trim();
    
    int tfIDX = 0;
    
    System.out.println("2) Information about available computer families:\n");
    
    // ask server for info about families
    String answer = Requester.askALGatorServer(server, port, ASGlobal.REQ_GETFAMILIES);
    JSONObject jAns = decodeAnswer(answer);
    if (jAns.getInt(ID_STATUS) != 0) { // if error occures...
      System.out.println(badRequestMsg(ASGlobal.REQ_GETFAMILIES, jAns));
      return;
    }
    
    JSONArray families = new JSONArray();
    try {families = (JSONArray) jAns.get(ID_ANSWER);} catch (Exception e) {}
    for (int i=0; i<families.length(); i++) {
      JSONObject family = families.getJSONObject(i);
      String tfID = family.getString(EComputerFamily.ID_FamilyID);
      if (familyID.equals(tfID)) tfIDX = i+1;
      
      System.out.printf("  %d : %s \n", i+1, family.toString(4).replaceAll("[{}]",""));
    }
    System.out.printf("  Select one of the above families or 0 to define a new family [%d]: ", tfIDX);
    String sFamily = sc.nextLine().trim();
    try {tfIDX = Integer.parseInt(sFamily);if (tfIDX < 0 || tfIDX > families.length()) tfIDX = 0;} catch (Exception e) {}
    System.out.println("  Selected family: " + tfIDX);
        
    // define a new family
    if (tfIDX == 0) {
      boolean familyOK = false;
      String id="", desc="", plat="", hard="";
      while (!familyOK) {        
        System.out.println("\n  Enter information (ID, description, platform, hardware) for the family this computer belongs to:");
        System.out.println("  (Example -  ID: F5, Description: Fast computers, Platform: Ubuntu 16.04, x64, Hardware: i7, 2.8Ghz, 32GB RAM)\n");
        System.out.print  ("      ID ["+id+"] : ");            String tid   = sc.nextLine(); id   = tid  .isEmpty() ? id : tid;
        System.out.print  ("      Description ["+desc+"] : "); String tdesc = sc.nextLine(); desc = tdesc.isEmpty() ? desc : tdesc;
        System.out.print  ("      Platform ["+plat+"] : ");    String tplat = sc.nextLine(); plat = tplat.isEmpty() ? plat : tplat;
        System.out.print  ("      Hardware ["+hard+"] : ");    String thard = sc.nextLine(); hard = thard.isEmpty() ? hard : thard;
        
        JSONObject jFamily = new JSONObject();
        jFamily.put(EComputerFamily.ID_Name, "Family-" + id);
        jFamily.put(EComputerFamily.ID_FamilyID, id);
        jFamily.put(EComputerFamily.ID_Desc, desc);
        jFamily.put(EComputerFamily.ID_Platform, plat);
        jFamily.put(EComputerFamily.ID_Hardware, hard);
        
        answer = Requester.askALGatorServer(server, port, ASGlobal.REQ_ADDFAMILY + " " + jFamily.toString(0));
        jAns = decodeAnswer(answer);
        if (jAns.getInt(ID_STATUS) != 0) {
          System.out.println("\n  ... " + jAns.getString(ID_MSG));
          id="";
          System.out.println();
        } else
          familyOK=true;
      }
      // if everything is OK, the answer contains the ID of a new computer
      familyID = jAns.getString(ID_ANSWER);
    } else {
      if (tfIDX > 0 && tfIDX <= families.length()) 
        familyID = families.getJSONObject(tfIDX-1).getString(EComputerFamily.ID_FamilyID);
      else {
        System.out.println("Invalid family.");
        return;
      }
    }

    System.out.println("\n3) Information about this computer:\n");
                   
    // ask server for info about computers
    JSONObject jFID = new JSONObject(); jFID.put(EComputer.ID_FamilyID, familyID);
    answer = Requester.askALGatorServer(server, port, ASGlobal.REQ_GETCOMPUTERS + " " + jFID.toString(0));
    jAns = decodeAnswer(answer);
    if (jAns.getInt(ID_STATUS) != 0) { // if error occures...
      System.out.println(badRequestMsg(ASGlobal.REQ_GETCOMPUTERS, jAns));
      return;
    }
    
    JSONArray computers = new JSONArray();
    int tcIDX = 0;
    try {computers = (JSONArray) jAns.get(ID_ANSWER);} catch (Exception e) {}
    for (int i=0; i<computers.length(); i++) {
      try {
        JSONObject comp = computers.getJSONObject(i);
        String tcID = comp.getString(EComputer.ID_ComputerID);
        if (computerID.equals(tcID)) tcIDX = i+1;
      
        System.out.printf("  %d : %s \n", i+1, comp.toString(4).replaceAll("[{}]",""));
      } catch (Exception e) {}
    }
    System.out.printf("  Select the number of this computer or enter 0 to define a new computer [%d]: ", tcIDX);
    String sComputer = sc.nextLine().trim();
    try {tcIDX = Integer.parseInt(sComputer);if (tcIDX < 0 || tcIDX > computers.length()) tcIDX = 0;} catch (Exception e) {}
    System.out.println("  Selected computer: " + tcIDX);

    
    // define a new computer
    if (tcIDX == 0) {
      boolean computerOK = false;
      String id="", desc="", ip="", cap="1100";
      while (!computerOK) {        
        System.out.println("\n  Enter information (ID, name, description, ip, capabilities) for this computer:");
        System.out.println("  (Example -  ID: C3, Description: My desktop, IP: 212.168.179.159, Capabilities: 1001)\n");
        System.out.print  ("      ID ["+id+"] : ");            String tid   = sc.nextLine(); id   = tid  .isEmpty() ? id : tid;
        System.out.print  ("      Description ["+desc+"] : "); String tdesc = sc.nextLine(); desc = tdesc.isEmpty() ? desc : tdesc;
        System.out.print  ("      IP address ["+ip+"] : ");    String tip = sc.nextLine(); ip = tip.isEmpty() ? ip : tip;
        System.out.print  ("      Capabilities (EM, CNT, JVM, QUICK; Example: 1100 for EM and CNT) ["+cap+"] : ");    String tcap = sc.nextLine(); cap = tcap.isEmpty() ? cap : tcap;
        
        JSONObject jFamily = new JSONObject();
        jFamily.put(EComputer.ID_Name, "Computer-" + id);
        jFamily.put(EComputer.ID_FamilyID, familyID);
        jFamily.put(EComputer.ID_ComputerID, id);
        jFamily.put(EComputer.ID_Desc, desc);
        jFamily.put(EComputer.ID_IP, ip);
        
        JSONArray caps = new JSONArray();if (cap.length()==4){
          if (cap.charAt(0)=='1')caps.put(CompCap.EM);
          if (cap.charAt(1)=='1')caps.put(CompCap.CNT);
          if (cap.charAt(2)=='1')caps.put(CompCap.JVM);
          if (cap.charAt(3)=='1')caps.put(CompCap.QUICK);
        }
        jFamily.put(EComputer.ID_Capabilities, caps);
        
        answer = Requester.askALGatorServer(server, port, ASGlobal.REQ_ADDCOMPUTER + " " + jFamily.toString(0));
        jAns = decodeAnswer(answer);
        if (jAns.getInt(ID_STATUS) != 0) {
          System.out.println("\n  ... " + jAns.getString(ID_MSG));
          id="";
          System.out.println();
        } else
          computerOK=true;
      }
      // if everything is OK, the answer contains the ID of a new computer
      JSONObject jjans = new JSONObject(); try {jjans=jAns.getJSONObject(ID_ANSWER);} catch (Exception e) {}
      computerID = "";  
      computerUID = "";
      try {
        computerID  = jjans.getString(EComputer.ID_ComputerID);
        computerUID = jjans.getString(EComputer.ID_ComputerUID);
      } catch (Exception e) {}
      
      if (computerUID==null || computerUID.length() != 10) {
        System.out.println("Invalid computer UID: " + computerUID);
        return;
      }
    } else {
      if (tcIDX > 0 && tcIDX <= computers.length()) {
        computerID  = computers.getJSONObject(tcIDX-1).getString(EComputer.ID_ComputerID);
        computerUID = computers.getJSONObject(tcIDX-1).getString(EComputer.ID_ComputerUID);
      } else {
        System.out.println("Invalid computer.");
        return;
      }
    }
    System.out.printf("\n  Your computer: %s.%s (%s)\n", familyID, computerID, computerUID);
    lconfig.setFamilyID(familyID);
    lconfig.setComputerID(computerID);
    lconfig.setComputerUID(computerUID);
    lconfig.saveEntity();
    System.out.println("  ... OK; information saved\n");
  }
  
  /**
   * Exit values: 0 ... OK
   *              200 ...   invalid nimber of parameters
   *              201 ...   unknown error
   *              other ... error reported by algator.Execute (ErrorStatus)
   * @param task
   * @return 
   */
  private static int runTask(ASTask task) {    
    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);
    
    // this line ensures that a subtask (executor) will be automatically closed 
    // when TaskClient will be closed (with CTRL-C, for example).
    // Without this line, the executor continued to run task in background eventhough TaskClient was stoped.  
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());    
    
    // max time to wait for task to finish; in the future this time should be read
    // from configuration files (should be testset-dependant)
    int secondsToWait = 10 * 60; // 10 minutes
    
    ExecuteWatchdog watchdog = new ExecuteWatchdog(secondsToWait*1000);
    executor.setWatchdog(watchdog);

    
    CommandLine cmdLine = new CommandLine("java");    

    //  !!!! to potrebujem samo v primeru, da TaskClient poganjam iz NetBeansa    
    // cmdLine.addArgument("-cp");
    // cmdLine.addArgument("/Users/Tomaz/Dropbox/FRI/ALGOSystem/ALGator/development/ALGator/dist/ALGator.jar");
   
    cmdLine.addArgument("algator.Execute"); 
    
    // Project
    cmdLine.addArgument(task.getString(ASTask.ID_Project));
    // Algorithm
    cmdLine.addArgument("-a");
    cmdLine.addArgument(task.getString(ASTask.ID_Algorithm));
    // Testset
    cmdLine.addArgument("-t");
    cmdLine.addArgument(task.getString(ASTask.ID_Testset));
    // Measurement type
    cmdLine.addArgument("-m");
    cmdLine.addArgument(task.getString(ASTask.ID_MType));
    // data_root
    cmdLine.addArgument("-dr");
    cmdLine.addArgument(ATGlobal.getALGatorDataRoot());
    // data_local
    cmdLine.addArgument("-dl");
    cmdLine.addArgument(ATGlobal.getALGatorDataLocal());    
    // Always execute
    cmdLine.addArgument("-e");
    // log into file
    //cmdLine.addArgument("-log");
    //cmdLine.addArgument("2");
    // be verbosive
    cmdLine.addArgument("-v");
    cmdLine.addArgument("2");

    
    // task info
    ASTask taskInfo = new ASTask();
    taskInfo.set(ASTask.ID_TaskID,      task.getFieldAsInt(ASTask.ID_TaskID));
    taskInfo.set(ASTask.ID_ComputerUID, task.getString(ASTask.ID_ComputerUID));
    taskInfo.set(ASTask.ID_Progress,    task.getFieldAsInt(ASTask.ID_Progress));    
    cmdLine.addArgument("-task");
    cmdLine.addArgument(taskInfo.toJSONString(0).replaceAll("\"", "'"));
    
    AEELog.log(": Starting    - " + task);
    int exitValue = 201;
    MyExecuteResultHandler resultHandler = null;
    try {
      exitValue = executor.execute(cmdLine);
      
      resultHandler = new MyExecuteResultHandler(exitValue);
      resultHandler.waitFor();
                  
      AEELog.log(": Completed   - " + task);
    } catch (Exception e) {
      if (watchdog.killedProcess()) {
        AEELog.log("Killed      - " + task);
        exitValue = ErrorStatus.PROCESS_KILLED.ordinal();
      } else {    
        AEELog.log(ASGlobal.ERROR_PREFIX + e.toString());
        AEELog.log("Failed   - " + task);
        
        try {
          Matcher m = Pattern.compile(".*\\(Exit value: (\\d+)\\)").matcher(e.toString());
          if (m.find()) 
            exitValue = Integer.parseInt(m.group(1));
        } catch (Exception ex) {}
      }  
    }
    if (resultHandler != null) exitValue = resultHandler.getExitValue();
    return exitValue;
  }

  public static void runClient(String hostName, int portNumber)  {
    String compID = ELocalConfig.getConfig().getComputerUID();
    AEELog.log        ("TaskClient: " + compID);    
        
    while(true) {        
        
        System.out.print("Task client '"+compID+"' connecting to server '"+hostName+"'...");
        while (true) {
          String ans = Requester.askALGatorServer(hostName, portNumber, ASGlobal.REQ_CHECK_Q);
          if (ans.equals(ASGlobal.REQ_CHECK_A)) break;
          
          System.out.print(".");System.out.flush();
          // sleep for a second            
          try {Thread.sleep(1000);} catch (InterruptedException ex) {}          
        }
        
        String msg = String.format("\rTask client '"+compID+"' connected to server '%s'%s.", hostName, "                                                 ");
        AEELog.log(        msg);
        while (true) {
          JSONObject reqJsno = new JSONObject(); reqJsno.put(EComputer.ID_ComputerUID, compID);
          String taskRequset = ASGlobal.REQ_GET_TASK + ASGlobal.STRING_DELIMITER + reqJsno.toString();
          
          String taskS = Requester.askALGatorServer(hostName, portNumber, taskRequset);
          
          if ((taskS != null) && !taskS.isEmpty()) {
            if (taskS.startsWith(ASGlobal.ERROR_SERVER_DOWN)) break;

            ASTask task = null; 
            try {
              JSONObject answer = new JSONObject(taskS); 
              // is status 0, then a task is available to be executed
              if (answer.getInt(ID_STATUS)==0) {
                task = new ASTask(answer.get(ID_ANSWER).toString());
              }
            } catch (Exception e) {}
            if (task != null) {
              int exitCode = runTask(task);
              
              
              if (exitCode == 242) {
                // 242 means that task was paused by server; this is a signal 
                // to client to send another REQ_GET_TASK request
              } else {
                int taskNo = task.getTaskID(); 

                String exitMsg = "?";
                switch (exitCode) {
                  case 0  : exitMsg = "Task completed successfully."; break;
                  case 202: exitMsg = "TaskID and ComputerID mismatch."; break;
                  case 203: exitMsg = "Invalid project name."; break;
                  case 204: exitMsg = "Problems with testset synchronization."; break;
                  case 205: exitMsg = "Invalid testset name."; break;
                  case 206: exitMsg = "Invalid algorithm name."; break;
                  case 207: exitMsg = "Invalid measurement type."; break;
                  case 208: exitMsg = "Problems with vmep configuration."; break;
                  case 214: exitMsg = "Problems with project synchronization."; break;
                  case 215: exitMsg = "The database is not initialized."; break;
                }
                JSONObject answer = new JSONObject();
                answer.put("ExitCode", exitCode); 
                answer.put(ASTask.ID_TaskID, task.getFieldAsInt(ASTask.ID_TaskID));
                answer.put("Message", exitMsg);
                
                String doneTaskRequest = (exitCode == 0 ? ASGlobal.REQ_CLOSE_TASK : ASGlobal.REQ_CANCEL_TASK)
                        + ASGlobal.STRING_DELIMITER + answer.toString();
               
                Requester.askALGatorServer(hostName, portNumber, doneTaskRequest);                                
              }
            }
          }          
          // sleep for a second
          try {Thread.sleep(1000);} catch (InterruptedException ex) {}
        }
    }
  }
}
