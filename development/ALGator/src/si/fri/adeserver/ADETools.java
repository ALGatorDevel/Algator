package si.fri.adeserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import si.fri.algotest.entities.CompCap;
import si.fri.algotest.entities.EAlgatorConfig;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.EComputer;
import si.fri.algotest.entities.EProject;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ErrorStatus;
import si.fri.algotest.tools.SortedArray;
import static si.fri.adeserver.ADETaskServer.activeTasks;
import static si.fri.adeserver.ADETaskServer.closedTasks;
import si.fri.algotest.entities.Entity;

/**
 *
 * @author tomaz
 */
public class ADETools {
  
  /**
   * Method reads a file with tasks and returns a list.
   * @param type 0 ... active tasks, 1 ... closed tasks, 2 ... archived tasks
   * @return 
   */
  public static SortedArray<STask> readADETasks(int type) {
    SortedArray<STask> tasks = new SortedArray<>();
    File taskFile = new File(ADEGlobal.getADETasklistFilename(type));
    if (taskFile.exists()) {
      try (DataInputStream dis = new DataInputStream(new FileInputStream(taskFile));) {
        while (dis.available() > 0) {
          String line = dis.readUTF();
          STask task = new STask(line);
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
  private static void removeAndArchiveOldTasks(SortedArray<STask> closedTasks, int numberOfDays) {
    ArrayList<STask> removedTasks = new ArrayList<>();
    
    long now = new Date().getTime();
    File archivedFile = new File(ADEGlobal.getADETasklistFilename(2)); // archived tasks file
    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(archivedFile));) {
      Iterator<STask> it = closedTasks.iterator();
      while (it.hasNext()) {
        STask task = it.next();
        if (now - task.getTaskStatusDate() > numberOfDays*1000*60*60*24) {
          dos.writeUTF(task.toJSONString());
          removedTasks.add(task);
        }
      }
    } catch (Exception e) {
      // if error ocures, nothing can be done
      System.out.println(e);
    }
    
    for (STask removedTask : removedTasks) {
      closedTasks.remove(removedTask); 
    }
  }
  
  public static void writeADETasks(SortedArray<STask> tasks, int type) {
    File taskFile = new File(ADEGlobal.getADETasklistFilename(type));
    try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(taskFile));) {
      Iterator<STask> it = tasks.iterator();
      while (it.hasNext()) {
        dos.writeUTF(it.next().toJSONString());
      }
    } catch (Exception e) {
      // if error ocures, nothing can be done
    }
  }

  /**
   *  Among all the registered computers, find and return the one with a given uid.
   *  If no computer has that uid, method returns null.
   */
  private static EComputer getComputer(String uid) {
    ArrayList<EComputer> computers = EAlgatorConfig.getConfig().getComputers();
    for (EComputer computer : computers) {
      if (uid.equals(computer.getField(EComputer.ID_ComputerUID)))
        return computer;
     }
    return null;    
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
  public static STask findFirstTaskForComputer(SortedArray<STask> taskQueue, String uid, boolean allowInprogressTasks) {
    EComputer comp = getComputer(uid);
    if (comp == null || comp.getCapabilities() == null) return null;
    
    TreeSet<CompCap> cCapabilities = comp.getCapabilities();
    String        cfamily          = comp.getString(EComputer.ID_FamilyID);
    if (cfamily == null || cfamily.isEmpty()) return null;
    
        
    for (STask task: taskQueue) {
      if (!allowInprogressTasks && TaskStatus.INPROGRESS.equals(task.getTaskStatus())) continue;

      if (!(task.getTaskStatus().equals(TaskStatus.INPROGRESS) || task.getTaskStatus().equals(TaskStatus.PENDING) || task.getTaskStatus().equals(TaskStatus.QUEUED)))
        continue;
     
      // tasks with computer already assigned can only be executed by the same computer 
      String taskComputerID = task.getComputerUID();
      if (taskComputerID != null && !taskComputerID.equals(Entity.unknown_value) && !taskComputerID.isEmpty()) {
        if (uid.equals(taskComputerID))
          return task;
        else 
          continue;
      }
      
      String tFamily = task.getString(STask.ID_Family); if (tFamily == null) tFamily = "";
      String tmtype  = task.getString(STask.ID_MType);  if (tmtype  == null || tmtype.isEmpty()) tmtype = "em";
      CompCap requiredCapability = CompCap.capability(tmtype);
                  
      if ((tFamily.isEmpty() || tFamily.equals(cfamily)) && cCapabilities.contains(requiredCapability))
        return task;
    }
    return null;
  }

  
  public static String getResultFilename(STask task) {
    String projName    = (String) task.getField(STask.ID_Project); 
    String algName     = (String) task.getField(STask.ID_Algorithm); 
    String testsetName = (String) task.getField(STask.ID_Testset);
    
    
    String compID           = getFamilyAndComputerName((String) task.getField(STask.ID_ComputerUID));
    MeasurementType mType   = MeasurementType.mtOf    ((String) task.getField(STask.ID_MType));
    
    return ATGlobal.getRESULTfilename(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName), 
        algName, testsetName, mType, compID
    );
  }
  
  /**
   * Sets the status of a task and writes this status to the task status file
   */
  public static String getTaskStatusFilename(STask task) {
    return ATGlobal.getTaskStatusFilename(
       (String) task.getField(STask.ID_Project), (String) task.getField(STask.ID_Algorithm), 
       (String) task.getField(STask.ID_Testset), (String) task.getField(STask.ID_MType));
  }
  
  public static void setTaskStatus(STask task, TaskStatus status, String msg, String computer) {
    task.setTaskStatus(status, computer, msg);
    
    if (computer == null)
      computer = task.getField(STask.ID_ComputerUID);    
    logTaskStatus(task, status, msg, computer);
    
    // if task was closed --> move from "active" to "closed" queue
    if (TaskStatus.closedTaskStatuses.contains(status)) {
      activeTasks.remove(task); 
      ADETools.writeADETasks(activeTasks, 0);
      
      closedTasks.add(task);
      
      // "clean up" closedTasks ...
      removeAndArchiveOldTasks(closedTasks, 1);
      // ... and write the remaining queue to file
      ADETools.writeADETasks(closedTasks, 1);  
    } else {
      activeTasks.touch(task);    
      ADETools.writeADETasks(activeTasks, 0);
    }
  }
  
  /**
   * Writes the status of a task to the task status file.
   * Computer name is valid only when task status is "COMPLETED"
   */
  public static void logTaskStatus(STask task, TaskStatus status, String msg, String computer) {
    if (task==null) return;
    
    String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    String idtFilename = getTaskStatusFilename(task);
    
    String startDate="", statusMsg = "", endDate="";
    if (status.equals(TaskStatus.PENDING)) {      
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
  public static STask findTask(SortedArray<STask> tasks, int taskID) {
    for (STask task : tasks) {
      if (task.getTaskID()  == taskID) 
        return task;
    }
    return null;
  }
    /**
     * Method returns the last non-empty line from task status file.
     */
    public static String getTaskStatus(STask task) {
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
  public static void setComputerFamilyForProject(STask task, String family) {
    String mType = ((String) task.getField(STask.ID_MType)).toUpperCase();
    MeasurementType mt = MeasurementType.UNKNOWN;
    try {mt = MeasurementType.valueOf(mType); } catch (Exception e) {}
    if (mt.equals(MeasurementType.UNKNOWN)) return;

    
    String projectName = task.getField(STask.ID_Project);
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
  
  public static String getFileContent(String projectName, String fileName) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String filePath = projectRoot + File.separator + fileName;
    StringBuilder result = new StringBuilder();
    try {
      for (String l : Files.readAllLines(Paths.get(filePath))) 
        result.append((result.length()==0 ? "" : "\n")).append(l);      
    } catch (Exception e) {
      result = new StringBuilder("!!" + e.toString());
    }
    return result.toString();
  }
  
  public static String saveFile(String projectName, String fileName, String content) {
    String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    String filePath = projectRoot + File.separator + fileName;
    try (PrintWriter pw = new PrintWriter(filePath)) {
      pw.print(content);
    } catch (Exception e) {
      return "!!" + e.toString();
    }
    return "OK";
  }
  
  public static void main(String[] args) {
    System.out.println(getProjectFiles("BasicSort"));
  }
}
