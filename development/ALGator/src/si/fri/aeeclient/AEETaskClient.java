package si.fri.aeeclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import si.fri.adeserver.ADEGlobal;
import si.fri.algotest.entities.ELocalConfig;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class AEETaskClient {
  
  /**
   * Exit values: 0 ... OK
   *              200 ...   invalid nimber of parameters
   *              201 ...   unknown error
   *              other ... error reported by algator.Execute (ErrorStatus)
   * @param task
   * @return 
   */
  private static int runTask(String task) {
    String [] parts = task.split(ADEGlobal.STRING_DELIMITER);
    if (parts.length < 5) return 200; // error in task format
    
    DefaultExecutor executor = new DefaultExecutor();
    executor.setExitValue(0);
    
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
    cmdLine.addArgument(parts[1]);
    // Algorithm
    cmdLine.addArgument("-a");
    cmdLine.addArgument(parts[2]);
    // Testset
    cmdLine.addArgument("-t");
    cmdLine.addArgument(parts[3]);
    // Measurement type
    cmdLine.addArgument("-m");
    cmdLine.addArgument(parts[4]);
    // data_root
    cmdLine.addArgument("-dr");
    cmdLine.addArgument(ATGlobal.getALGatorDataRoot());
    // data_local
    cmdLine.addArgument("-dl");
    cmdLine.addArgument(ATGlobal.getALGatorDataLocal());    
    // Always execute
    cmdLine.addArgument("-e");
    // log into file
    cmdLine.addArgument("-log");
    cmdLine.addArgument("2");

    
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
        AEELog.log(ADEGlobal.ERROR_PREFIX + e.toString());
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

  public static void runClient(String hostName)  {
    if (hostName == null)
      hostName   = ELocalConfig.getConfig().getTaskServerName();
    
    int    portNumber = ADEGlobal.ADEPort;
    
    String compID = ELocalConfig.getConfig().getComputerID();
    
    boolean logTaskServerProblem = true;
    
    while(true) {  
      try (
              Socket kkSocket = new Socket(hostName, portNumber);
              PrintWriter    toServer    = new PrintWriter(kkSocket.getOutputStream(), true);
              BufferedReader fromServer  = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) 
      {
        logTaskServerProblem = true;
        AEELog.log(        "Task client connected to server - " + hostName);
        System.out.println("Task client connected to server - " + hostName);
        while (true) {
          String taskRequset = ADEGlobal.REQ_GET_NEXT_TASK + ADEGlobal.STRING_DELIMITER + compID;
          toServer.println(taskRequset);
          String task = fromServer.readLine();
          if ((task != null) && !task.isEmpty() && !task.equals(ADEGlobal.NO_TASKS)) {
            if (!ADEGlobal.isError(task)) {
              int exitCode = runTask(task);
              
              String taskNo = ""; try {taskNo = task.split(" ")[0];} catch (Exception e) {}
              toServer.println(ADEGlobal.REQ_COMPLETE_TASK + ADEGlobal.STRING_DELIMITER + taskNo + ADEGlobal.STRING_DELIMITER + exitCode);
            }
          }
          
          // sleep for a second
          try {Thread.sleep(1000);} catch (InterruptedException ex) {}
        }
        
      } catch (UnknownHostException e) {
        System.out.println("Unknown host " + hostName);
        AEELog.log        ("Unknown host " + hostName);
      } catch (java.net.ConnectException e) {
        if (logTaskServerProblem) {
          System.out.println("Not connected - TaskServer is not running at " + hostName);
          AEELog.log        ("Not connected - TaskServer is not running at " + hostName);
          logTaskServerProblem = false;
        }
      } catch (IOException e) {
        System.out.println("I/O error " + e);
        AEELog.log        ("I/O error " + e.toString());
      }
      
      // sleep for 5 seconds before next try
      try {Thread.sleep(5000);} catch (InterruptedException ex) {}
    }
  }
}
