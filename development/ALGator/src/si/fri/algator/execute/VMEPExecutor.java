package si.fri.algator.execute;

import algator.VMEPExecute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.Variables;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.global.ExecutionStatus;
import si.fri.algator.global.VMEPErrorStatus;

/**
 *
 * @author tomaz
 */
public class VMEPExecutor {
  
  /** How many times the process executed by vmep virtual machine is expected to be slower than process executed by normal vm */
  private static final int SLOW_FACTOR = 2;
  /** Maximum number of seconds for vmep startup */ 
  private static final int VMEPVM_DELAY = 5;
  
  
  /**
   * Iterates trought testset and executes each testcase. To execute a testcase, a VMEP virtual machine 
   * is used (method runWithLimitedTime). If execution is successful, a result  (one line) is copied from 
   * getJVMRESULTfilename to regular result file for this algorithm-testset. If execution failes, a line with 
   * error message is appended to result file.
   */
  public static void iterateTestSetAndRunAlgorithm(Project project, String algName, String currentJobID, 
          String testSetName, EResult resultDesc, AbstractTestSetIterator it, 
          Notificator notificator, File resultFile, boolean asJSON) {

    ArrayList<Variables> allAlgsRestuls = new ArrayList();
    VMEPErrorStatus executionStatus;
    
    String delim      = ATGlobal.DEFAULT_CSV_DELIMITER;
    EVariable algPar  = EResult.getAlgorithmNameParameter(algName);
    String algP       = algPar.getField(EVariable.ID_Value);
    EVariable tsPar   = EResult.getTestsetNameParameter(testSetName);
    String tsP        = tsPar.getField(EVariable.ID_Value);
    
    EVariable killedEx = EResult.getExecutionStatusIndicator(ExecutionStatus.KILLED);
    EVariable failedEx = EResult.getExecutionStatusIndicator(ExecutionStatus.FAILED);
      
    /* The name of the output file */
    String projectRoot = ATGlobal.getPROJECTroot(project.getDataRoot(), project.getName());
    
    // Maximum time allowed (in seconds) for one execution of one test; if the algorithm 
    // does not  finish in this time, the execution is killed
    int timeLimit = SLOW_FACTOR * 10;
    try {
       timeLimit = SLOW_FACTOR *  it.testSet.getFieldAsInt(ETestSet.ID_TimeLimit, 10);
    } catch (NumberFormatException e) {
        // if ETestSet.ID_TimeLimit parameter is missing, timelimit is set to 30 (sec) and exception is ignored
    }
    timeLimit += VMEPVM_DELAY;
    
    int testID = 0; // 
    try {
      while (it.hasNext()) {
        it.readNext();++testID;

        Variables result = new Variables();
        result.addVariable(algPar, true);
        result.addVariable(tsPar,  true);

        String tmpFolderName = ATGlobal.getTMPDir(project.getName());          
        
        AbstractInput testCase = it.getCurrent().getInput();
        if (testCase != null) {
          EVariable testP = testCase.getParameters().getVariable(EResult.instanceIDParName);
          result.addVariable(testP,  true);
          
          executionStatus = runWithLimitedTime(project.getName(), algName, testSetName, testID, 
                  tmpFolderName, project.getDataRoot(), ATGlobal.getALGatorDataLocal(), timeLimit);
        } else {
          executionStatus = VMEPErrorStatus.INVALID_TEST;
          ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_TEST, " ");
        }

        
        String testResultLine;
        if (!executionStatus.equals(VMEPErrorStatus.OK)) {
          if (executionStatus.equals(VMEPErrorStatus.KILLED)) {
            notificator.notify(testID,ExecutionStatus.KILLED);
            result.addVariable(killedEx, true);
            result.addVariable(EResult.getErrorIndicator(
              String.format("Killed after %d second(s)", timeLimit)), true);
          } else {
            notificator.notify(testID,ExecutionStatus.FAILED);
            result.addVariable(failedEx, true);
            result.addVariable(EResult.getErrorIndicator(
              ErrorStatus.getLastErrorMessage() + executionStatus.toString()), true);
          }
          testResultLine=result.toString(EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc), asJSON, delim);
        } else {
          String oneResultFilename = ATGlobal.getJVMRESULTfilename(tmpFolderName, algName, testSetName, testID);
          try (Scanner sc = new Scanner(new File(oneResultFilename))) {
            testResultLine = sc.nextLine();
            if (testResultLine.startsWith(algP + delim + tsP)) {
              notificator.notify(testID,ExecutionStatus.DONE);
            } else {
              notificator.notify(testID,ExecutionStatus.FAILED);
              result.addVariable(failedEx, true);
              result.addVariable(EResult.getErrorIndicator(
                VMEPErrorStatus.UNKNOWN.toString()), true);
              testResultLine=result.toString(EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc), asJSON, delim);
            }
          } catch (Exception e) {
            notificator.notify(testID,ExecutionStatus.FAILED);
            result.addVariable(failedEx, true);
            result.addVariable(EResult.getErrorIndicator(e.toString()), true);
            testResultLine=result.toString(EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc), asJSON, delim);
          }
        }  
        // append a line representing test results to the corresponding result file        
        PrintWriter pw = new PrintWriter(new FileWriter(resultFile, true));
          pw.println(testResultLine);
        pw.close();        
        
        ATGlobal.deleteTMPDir(tmpFolderName, project.getName());
      }
      it.close();
    } catch (IOException e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_RUN, e.toString());
    }
  }

  /**
   * Tries to obtain process exit code. If the process has not finished yet, 
   * method returns -1, else it returns process exit code.
   */
  private static int processExitCode (Process process) {
    try {
        int exit_code = process.exitValue();
        return exit_code;
    } catch (IllegalThreadStateException itse) {
        return -1;
    }
  }
  
  /**
   * 
   * Runs a given algorithm on a given test and waits for timeLimit seconds. Result of this method is 
   * v VMEPErrorStatus, further information about execution status are stored in ErrorStatus.lastErrorMessage.
   * @return 
   *   <code>VMEPErrorStatus.KILLED</code> if algorithm runs out of time                            <br>
   *   <code>VMEPErrorStatus.VMEPVM_ERROR</code> if problems occure during the initialization or execution phase<br>
   *   <code>VMEPErrorStatus.*</code> if algorithm exited with exit code different than 0 <br>
   *   <code>VMEPErrorStatus.OK</code> if algorithm exited normally <br>  

   * If algorithm finishes in time, </code>runWithLimitedTime</code> returns <code>VMEPErrorStatus.OK</code>
   */
  static VMEPErrorStatus runWithLimitedTime(String projectName, String algname, String testSetName, 
          int testID, String comFolder, String dataRoot, String dataLocal, int timeLimit) {
    
    ErrorStatus.setLastErrorMessage(ErrorStatus.STATUS_OK, "");
    
    Object result =  VMEPExecute.runWithVMEP(projectName,algname, testSetName, testID, comFolder, dataRoot, dataLocal);
      
    // during the process creation, an error occured
    if (result == null || !(result instanceof Process)) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_CANT_BE_CREATED, result == null ? "???" : result.toString());
      return VMEPErrorStatus.VMEPVM_ERROR;
    }
    
    Process externProcess = (Process) result;
    
    // loop and wait for process to finish
    int loop_per_sec  = 10;
    int secondsPassed = 0;
    whileloop: while (timeLimit > 0) {
      // loop for one second
      for(int i=0; i<loop_per_sec; i++) {
        if (processExitCode(externProcess) >= 0)
          break whileloop;        
        try {Thread.sleep(1000/loop_per_sec);} catch (InterruptedException e) {}
      }
      timeLimit--; secondsPassed++;
    }
    
    int exitCode = processExitCode(externProcess);
    
    if (exitCode < 0) { // process hasn't finised yet, it has to be killed
      try {
        ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_KILLED, String.format("(after %d sec.)", (int)secondsPassed)); 
        externProcess.destroy();
        return VMEPErrorStatus.KILLED;
      } catch (Exception e) {}
    }
    
    try {
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(externProcess.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(externProcess.getErrorStream()));
 
      String s;StringBuffer sb = new StringBuffer();
      while ((s = stdInput.readLine()) != null) sb.append(s);            
      while ((s = stdError.readLine()) != null) sb.append(s);
      
      if (exitCode != 0) {
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_EXECUTING_VMPEJVM, sb.toString() + "(ercode: "+exitCode+")");
        return VMEPErrorStatus.getErrorStatusByID(exitCode);
      } else {
        if (ATGlobal.verboseLevel > 0)
          System.out.println(sb);
        return  VMEPErrorStatus.OK;
      }
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.PROCESS_CANT_BE_CREATED, e.toString().replaceAll("\n", ""));
      return VMEPErrorStatus.VMEPVM_ERROR;
    }  
  }
  
   
  
  
  
}
