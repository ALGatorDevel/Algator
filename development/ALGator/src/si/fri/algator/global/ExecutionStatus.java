package si.fri.algator.global;

/**
 *
 * @author tomaz
 */
public enum ExecutionStatus {
  DONE,FAILED,KILLED, UNKNOWN;
  
  @Override
  public String toString() {
    switch (this) {
      case DONE:
        return "DONE";
      case FAILED:
        return "FAILED";
      case KILLED:
        return "KILLED";
      default:
        return "/unknown/";
    }
  }
}
