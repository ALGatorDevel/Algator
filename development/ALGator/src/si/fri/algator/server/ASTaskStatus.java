package si.fri.algator.server;

import java.util.Arrays;
import java.util.TreeSet;

/**
 *
 * @author tomaz
 */

public enum ASTaskStatus {
  UNKNOWN     , 
  // active tasks
  PENDING     ,  // waiting to be executed (no computer was assigned jet) ,
  QUEUED      ,  // waiting to be executed (computer was already assigned) ,
  INPROGRESS  ,  // currently being executed 
  PAUSED      ,  // task is currently paused, not competing for computer resources (status can be later changed to QUEUED, PENDING or CANCELED)
  // closed tasks
  COMPLETED   ,  // a completed task; it exists only for logging purpose
  CANCELED    ,  // task canceled by user
  FAILED      ,  // task that failed to execute (problems in configuration?)
  KILLED      ;  // task killed by the system (due to a time limit)// task killed by the system (due to a time limit)
  
  
  // statuses of closed tasks
  static TreeSet<ASTaskStatus> closedTaskStatuses = new TreeSet(Arrays.asList(new ASTaskStatus[]{COMPLETED, CANCELED, FAILED, KILLED})); 
  
  @Override
  public String toString() {
    switch (this) {
      case QUEUED:
        return "QUEUED";
      case INPROGRESS:
        return "INPROGRESS";
      case COMPLETED:
        return "COMPLETED";
      case PENDING:
        return "PENDING";        
      case CANCELED:
        return "CANCELED";
      case PAUSED:
        return "PAUSED";        
      case FAILED:
        return "FAILED";
      case KILLED:
        return "KILLED";        
    }
    return "/unknown/";
  }

  public static ASTaskStatus getTaskStatus(String status) {
    for (ASTaskStatus sts : ASTaskStatus.values()) {
      if (status.equals(sts.toString())) {
        return sts;
      }
    }    
    return UNKNOWN;
  }
}
