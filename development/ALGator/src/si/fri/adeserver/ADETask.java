package si.fri.adeserver;

import java.util.ArrayList;
import java.util.Date;
import si.fri.algotest.entities.Entity;
import si.fri.algotest.entities.MeasurementType;

/**
 * ADE task. ADEServer holds an array of ADE tasks to be executed by AEE.
 * @author tomaz
 */
public class ADETask extends Entity {
  
  //public static final String HTML_TAG_NEW        = "<font color='FF0000'>[NEW]</font>";
  public static final String HTML_TAG_NEW        = "<span id='sbadge' title = 'info' class='badge background-color-alert'>NEW</span>";
  public static final String HTML_TAG_UPTODATE   = "<span id='sbadge' title = 'info' class='badge background-color-primary'>UP-TO-DATE</span>";
  public static final String HTML_TAG_OUTDATED   = "<span id='sbadge' title = 'info' class='badge background-color-msg'>OUTDATED</span>";
  public static final String HTML_TAG_CORRUPTED  = "<span id='sbadge' title = 'info' class='badge background-color-alert'>CORRUPTED</span>";
  public static final String HTML_TAG_QUEUED     = "<span id='sbadge' title = 'info' class='badge background-color-msg'>QUEUED</span>";
  public static final String HTML_TAG_RUNNING    = "<span id='sbadge' title = 'info' class='badge background-color-msg'>RUNNING !_!</span>";  
  
  public static final String ID_ADETask    = "ADETask";
  
  // Fields
  public static final String ID_TaskID     = "TaskID";     // int
  public static final String ID_Project    = "Project";    // String
  public static final String ID_Algorithm  = "Algorithm";  // String
  public static final String ID_Testset    = "Testset";    // String
  public static final String ID_MType      = "MType";      // String
  
  public static final String ID_Status           = "Status";           // String
  public static final String ID_StatusDate       = "StatusDate";       // String
  public static final String ID_AssignedComputer = "AssignedComputer"; // String
  
  
  // computers that are able to execute this task
  private ArrayList<String> computerCandidates;
  
  public ADETask() {
    super(ID_ADETask, 
	 new String [] {ID_TaskID, ID_Project, ID_Algorithm, ID_Testset, ID_MType, ID_Status, ID_StatusDate, ID_AssignedComputer});
   
    setRepresentatives(ID_Project, ID_Algorithm, ID_Testset, ID_MType);
    
    computerCandidates = new ArrayList<>();
  }
  
  /**
   * 
   * @param tmpTask tmpTask is created as a data holder; in such task the ID is not important, therefore it is not assigned; 
   */
  public ADETask(String project, String algorithm, String testset, String mType, boolean tmpTask) {
    this();
    
    if (!tmpTask) {
      int taskID = ADEGlobal.getNextTaskID();
      set(ID_TaskID,     taskID);
    }
    
    set(ID_Project,    project);
    set(ID_Algorithm,  algorithm);
    set(ID_Testset,    testset);
    set(ID_MType,      mType);
    
    setTaskStatus(TaskStatus.QUEUED, "none");
  }
 
    
  ADETask(String json) {
    this();
    initFromJSON(json);
  }

  public void setTaskStatus(TaskStatus status, String computer) {
    set(ID_Status,     status.toString());
    set(ID_StatusDate, Long.toString(new Date().getTime())); 
    
    if (computer != null)
      set(ID_AssignedComputer, computer);
  }
  
  public TaskStatus getTaskStatus() {
    String status = getField(ID_Status);
    if (status != null)
      return TaskStatus.getTaskStatus(status);
    else
      return TaskStatus.UNKNOWN;
  }
  
  public void addComputerCandidate(String candidate) {
    computerCandidates.add(candidate);
  }
  public void setCandidateComputers(ArrayList<String> compCand) {
    computerCandidates = compCand;
  }
  public ArrayList<String> getCandidateComputers() {
    return computerCandidates;
  }
  
  public MeasurementType getMType() {
    MeasurementType mt = MeasurementType.UNKNOWN;
    try { 
      mt = MeasurementType.valueOf(((String)getField(ADETask.ID_MType)).toUpperCase()); 
    } catch (Exception e) {}
    return mt;
  }
  
  
  @Override
  public String toString() {
    return getField(ID_TaskID)    + ADEGlobal.STRING_DELIMITER +
           getField(ID_Project)   + ADEGlobal.STRING_DELIMITER +
           getField(ID_Algorithm) + ADEGlobal.STRING_DELIMITER +
           getField(ID_Testset)   + ADEGlobal.STRING_DELIMITER +
           getField(ID_MType)     + ADEGlobal.STRING_DELIMITER +
           computerCandidates.toString();
  }
  
  public String statusMsg() {
    String result = "";
    if (get(ID_Status).equals(TaskStatus.RUNNING.toString()))
      return ADETools.getTaskStatus(this);
    else if (get(ID_Status).equals(TaskStatus.QUEUED.toString()))
      result = "Queue position: ";
        
    return "Status: "+ get(ID_Status) + "  " + result;
  }
  
  public String toStringPlus() {
    return toString() + " [" + statusMsg() + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ADETask) {
      return this.get(ID_Project).  equals(((ADETask)obj).get(ID_Project))   &&
             this.get(ID_Algorithm).equals(((ADETask)obj).get(ID_Algorithm)) &&
             this.get(ID_Testset).  equals(((ADETask)obj).get(ID_Testset))   &&
             this.get(ID_MType).    equals(((ADETask)obj).get(ID_MType));
    } else return false;
      
  }
  
  
  
}
