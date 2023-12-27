package si.fri.algator.server;

import java.util.Date;
import si.fri.algator.entities.Entity;
import si.fri.algator.entities.MeasurementType;

/**
 * ADE task. ADEServer holds an array of ADE tasks to be executed by AEE.
 * @author tomaz
 */
public class ASTask extends Entity implements Comparable<ASTask> {    
  public static final String ID_ADETask          = "STask";
  
  // Fields
  public static final String ID_TaskID           = "TaskID";           // unique identifier
  
  public static final String ID_Project          = "Project";          // String
  public static final String ID_Algorithm        = "Algorithm";        // String
  public static final String ID_Testset          = "Testset";          // String
  public static final String ID_MType            = "MType";            // String
  public static final String ID_Family           = "Family";           // String (family to execute the task; if blank - any appropriate family is OK)
  
  
  public static final String ID_Status           = "Status";           // String
  public static final String ID_Progress         = "Progress";         // Integer - number of tests sucessfully completed
  public static final String ID_CreationDate     = "CreationDate";     // String - date of creation
  public static final String ID_StatusDate       = "StatusDate";       // String - date of status change
  public static final String ID_ComputerUID      = "ComputerUID";      // String - computer assigned to this task
  public static final String ID_Priority         = "Priority";         // Integer - 0 (low) ... infty (high); 5 default
  public static final String ID_Msg              = "Msg";              // String (info about status if available)
  
    
  public ASTask() {
    super(ID_ADETask, 
	 new String [] {ID_TaskID, ID_Status, ID_Progress, ID_Project, ID_Algorithm, ID_Testset, ID_MType, ID_Family, ID_CreationDate, ID_StatusDate, ID_ComputerUID, ID_Priority, ID_Msg},
       new Object [] {"",        "",        0,           "",         "",           "",         "em",     "",        0,               0,             "",             5,           ""});
    
   
    setRepresentatives(ID_Project, ID_Algorithm, ID_Testset, ID_MType, ID_ComputerUID, ID_Status, ID_Priority, ID_CreationDate, ID_StatusDate, ID_Progress, ID_Msg);    
  }
  
  /**
   * 
   * @param tmpTask is created as a data holder; in such task the ID is not important, therefore it is not assigned; 
   */
  public ASTask(String project, String algorithm, String testset, String mType) {
    this();
    
    set(ID_Project,    project);
    set(ID_Algorithm,  algorithm);
    set(ID_Testset,    testset);
    set(ID_MType,      mType);
  }
 
  public ASTask(String json) {
    this();
    initFromJSON(json);
    
    if (this.getField(ID_CreationDate) == null || this.getFieldAsLong(ID_CreationDate)==0)
      this.set(ID_CreationDate, new Date().getTime());
  }

  public final void assignID() {
    int taskID = ASGlobal.getNextTaskID();
    set(ID_TaskID,     taskID);      
    setName("Task-" + getFieldAsInt(ID_TaskID));
  }
  
  
  public final void setTaskStatus(ASTaskStatus status) {
    setTaskStatus(status, null);
  }
  public final void setTaskStatus(ASTaskStatus status, String computer) {
    setTaskStatus(status, computer, null);
  }
  public final void setTaskStatus(ASTaskStatus status, String computer, String msg) {
    set(ID_Status,     status.toString());    
    if (computer != null) set(ID_ComputerUID, computer);
    if (msg      != null) set(ID_Msg,         msg);
    
    set(ID_StatusDate, new Date().getTime()); 
  }

  private ASTaskStatus getTS(String key) {
    String status = getField(key);
    if (status != null)
      return ASTaskStatus.getTaskStatus(status);
    else
      return ASTaskStatus.UNKNOWN;
  }
  public ASTaskStatus getTaskStatus() {
    return getTS(ID_Status);
  }
    
  public long getTaskStatusDate() {
    return getFieldAsLong(ID_StatusDate);
  }
  
  
  public MeasurementType getMType() {
    MeasurementType mt = MeasurementType.UNKNOWN;
    try { 
      mt = MeasurementType.valueOf(((String)getField(ASTask.ID_MType)).toUpperCase()); 
    } catch (Exception e) {}
    return mt;
  }
  
  
  public int getTaskID() {
    return getFieldAsInt(ID_TaskID); 
  }
  public int getProgress() {
    return getFieldAsInt(ID_Progress); 
  }
  public String getComputerUID() {
    String computerUID = getString(ID_ComputerUID);
    return computerUID == null ? "" : computerUID; 
  }
  public String getFamily() {
    String family = getString(ID_Family);
    return family == null ? "" : family; 
  }

  
  
  public void setProgress(int progress) {
    set(ID_Progress, progress); 
  }
  
 
  public String statusMsg() {
    String result = "";
    if (getTaskStatus().equals(ASTaskStatus.INPROGRESS))
      return ASTools.getTaskStatus(this);
    else if (getTaskStatus().equals(ASTaskStatus.PENDING))
      result = "Queue position: ";
        
    return "Status: "+ getTaskStatus()+ "  " + result;
  }
  
  public String toStringPlus() {
    return toString() + " [" + statusMsg() + "]";
  }

  /**
   * Compares tasks according to Priority and StatusDate. Positive result means that this has higher priority than t.
   */
  public int compare(ASTask t) {
    int tP = getFieldAsInt(ID_Priority), oP = t.getFieldAsInt(ID_Priority);
    if (tP == oP) {
      int tD =   getFieldAsInt(ID_StatusDate);
      int oD = t.getFieldAsInt(ID_StatusDate);
      return oD - tD;
    } else 
      return tP-oP;
  }
  
  @Override
  /**
   * Sorting criteria:  Status (in_progress > queued | pending), Priority, dateAdded 
   */
  public int compareTo(ASTask t) {
    try {
      int compare = compare(t);
      if (compare == 0) {
        int tS =   getTaskStatus().equals(ASTaskStatus.INPROGRESS) ? 0 : (  getTaskStatus().equals(ASTaskStatus.QUEUED) ? 1 : (  getTaskStatus().equals(ASTaskStatus.PENDING) ? 2 : 3));
        int oS = t.getTaskStatus().equals(ASTaskStatus.INPROGRESS) ? 0 : (t.getTaskStatus().equals(ASTaskStatus.QUEUED) ? 1 : (t.getTaskStatus().equals(ASTaskStatus.PENDING) ? 2 : 3));
        return oS - tS; 
      } else
        return compare;
    } catch (Exception e) {return 0;}
  }
  
  
  
}
