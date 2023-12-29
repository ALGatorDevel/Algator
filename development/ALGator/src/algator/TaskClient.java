package algator;

import si.fri.algator.client.Requester;

/**
 *
 * @author tomaz
 */
public class TaskClient  {
  
  public static void main(String args[]) {
    Requester.programName = "TaskClient";
    Requester.do_main(args, 0);
  }
}
