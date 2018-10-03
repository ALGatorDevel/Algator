package si.fri.adeserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;
import static si.fri.adeserver.ADETaskServer.taskQueue;
import si.fri.algotest.entities.ComputerCapability;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.EComputer;
import si.fri.algotest.entities.EComputerFamily;
import si.fri.algotest.entities.EAlgatorConfig;
import si.fri.algotest.entities.EProject;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ErrorStatus;
import si.fri.algotest.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class ADETools {
  
  public static Vector<ADETask> readADETasks() {
    Vector<ADETask> tasks = new Vector<>();
    File taskFile = new File(ADEGlobal.getADETasklistFilename());
    if (taskFile.exists()) {
      try (DataInputStream dis = new DataInputStream(new FileInputStream(taskFile));) {
        while (dis.available() > 0) {
          String line = dis.readUTF();
          ADETask task = new ADETask(line);
          task.setCandidateComputers(ADETools.getCondidateComputersFor(task));
          tasks.add(task);
        }
      } catch (Exception e) {
        // if error ocures, nothing can be done
      }
    }
    return tasks;
  }

  public static void writeADETasks(Vector<ADETask> tasks) {
    File taskFile = new File(ADEGlobal.getADETasklistFilename());
    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(taskFile));) {
      for (ADETask aDETask : tasks) {
        dos.writeUTF(aDETask.toJSONString());
      }
    } catch (Exception e) {
      // if error ocures, nothing can be done
    }
  }


  
  /**
   * Sets the status of a task and writes this status to the task status file
   */
  public static String getTaskStatusFilename(ADETask task) {
    return ATGlobal.getTaskStatusFilename(
       (String) task.getField(ADETask.ID_Project), (String) task.getField(ADETask.ID_Algorithm), 
       (String) task.getField(ADETask.ID_Testset), (String) task.getField(ADETask.ID_MType));
  }
  
  public static void setTaskStatus(ADETask task, TaskStatus status, String msg, String computer) {
    task.setTaskStatus(status, computer);
    if (computer == null)
      computer = task.getField(ADETask.ID_AssignedComputer);
    
    
    writeTaskStatus(task, status, msg, computer);
    
    ADETools.writeADETasks(taskQueue);
    ADELog.log(status + " " + task.toString() + " [Computer:"+computer+"]");
  }
  
  /**
   * Writes the status of a task to the task status file.
   * Computer name is valid only when task status is "COMPLETED"
   */
  public static void writeTaskStatus(ADETask task, TaskStatus status, String msg, String computer) {
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    String idtFilename = getTaskStatusFilename(task);
    
    String startDate="", statusMsg = "", endDate="";
    if (status.equals(TaskStatus.QUEUED)) {      
      startDate = date + ": Task queued";
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
      case RUNNING:
        statusMsg = date + ":RUNNING # " + computer + (statusMsg.isEmpty() ? "" : " # ") + statusMsg;
        break;
      case COMPLETED:  
        endDate   = date + ":COMPLETED # " + computer;
        break;
      case FAILED:
        endDate = date + ": Task failed on " + computer ;
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
     * Method returns the last non-empty line from task status file.
     */
    public static String getTaskStatus(ADETask task) {
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
     * For all [comp] from [comps] check the file PROJ-[project]/results/[comp]/[algorithm]-[testset].[mtype] and return 
     *   ADETask.HTML_TAG_NEW if no such file exists                                          <br>
     *   ADETask.HTML_TAG_OUTDATED  if file is older than project's configurations            <br>
     *   ADETask.HTML_TAG_CORRUPTED if file does not have correct number of lines             <br>
     *   ADETask.HTML_TAG_UPTODATE if file is up to date (correct date and number of lines)   <br>
     * 
     *  Here <comp> is 
     */
    public static String getTaskResultFileStatus(Project project, ADETask task) { 
      String algorithmName = task.getField(ADETask.ID_Algorithm);
      String testsetName   = task.getField(ADETask.ID_Testset);
      String mtype         = task.getField(ADETask.ID_MType);

      String resultFileName = ATTools.getTaskResultFileName(project, algorithmName, testsetName, mtype);

      if (!new File(resultFileName).exists()) // file does not exist - task was not executed yet
        return ADETask.HTML_TAG_NEW;
      
      int expectedNumberOfLines = 0;
      ETestSet testset = project.getTestSets().get(testsetName);
      if (testset != null) try {
        expectedNumberOfLines = testset.getFieldAsInt(ETestSet.ID_N, 0);
      } catch (Exception e) {}

      int numberOfLines = ATTools.getNumberOfLines(resultFileName);
      
      if (!ATTools.resultsAreComplete(resultFileName, expectedNumberOfLines))
        return ADETask.HTML_TAG_CORRUPTED;
      
      if (ATTools.resultsAreUpToDate(project, algorithmName, testsetName, mtype, resultFileName))
        return ADETask.HTML_TAG_UPTODATE;
      else
        return ADETask.HTML_TAG_OUTDATED;
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
   * Returns all the computers tha are capable of executing current task. If project.<mtype>ExecFamily is 
   * defined, then only computers from this family are checked, otherwise, computers from all families are checked.
   */
  public static ArrayList<String> getCondidateComputersFor(ADETask task) {
    ArrayList<String> result = new ArrayList<>();    
    Project project = new Project(ATGlobal.getALGatorDataRoot(), (String) task.getField(ADETask.ID_Project));
    if (project.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) {
      // TODO: tu bi lahko preveril, če so algoritem, testset in mtype pravilni (so definirani v projektu)
            
      MeasurementType mt = task.getMType();      
      String myFamily = project.getEProject().getProjectFamily(mt);
            
      // generate a list of computer that are capable to execute a given task
      ComputerCapability requiredCompCap = ComputerCapability.getComputerCapability(mt.getExtension());      
      EAlgatorConfig config = EAlgatorConfig.getConfig();
      for(EComputerFamily ef : config.getFamilies()) {
        for (EComputer comp : ef.getComputers()) {
          if (myFamily.isEmpty() || ef.getField(EComputerFamily.ID_FamilyID).equals(myFamily)) {
            TreeSet<ComputerCapability> compcap = comp.getCapabilities();
            if (compcap.contains(requiredCompCap))
              result.add(ef.getField(EComputerFamily.ID_FamilyID) + "." + comp.getField(EComputer.ID_ComputerID));
          }
        }
      }
    }
    return result;
  }
  
  
  /**
   * Method sets <mtype>ExecFamily parameter in the project of the task.
   * Method is called when computer cid starts executing task. 
   */
  public static void setComputerFamilyForProject(ADETask task, String cid) {
    String mType = ((String) task.getField(ADETask.ID_MType)).toUpperCase();
    MeasurementType mt = MeasurementType.UNKNOWN;
    try {mt = MeasurementType.valueOf(mType); } catch (Exception e) {}
    if (mt.equals(MeasurementType.UNKNOWN)) return;

    String [] parts = cid.split("[.]");
    if (parts.length < 2) return;
    String myFamily = parts[1];
    
    String projectName = task.getField(ADETask.ID_Project);
    if (projectName == null || projectName.isEmpty()) return;
    
    String projectFileName = ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectName);
    if (projectFileName == null || projectFileName.isEmpty()) return;
    
    File projectFile = new File(projectFileName);
    EProject project = new EProject(projectFile);
        
    project.setFamilyAndSave(mt, myFamily, false);
    
  }
}
