package si.fri.algator.global;

/**
 *
 * @author tomaz
 */
public class Answer {
  public ErrorStatus status;
  public String      message; 
  
  public Answer(ErrorStatus status) {
    this.status  = status;
    this.message = "";
  }

  public Answer(ErrorStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
