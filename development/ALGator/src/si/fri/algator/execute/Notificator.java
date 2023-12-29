package si.fri.algator.execute;

import si.fri.algator.server.ASTask;
import si.fri.algator.server.ASTools;
import si.fri.algator.server.ASTaskStatus;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ExecutionStatus;

/**
 * A notificator is a class used to comunicate between ATSystem and ATExecutor.
 * Each time the executor executes a test, it notifies the Notificator by
 * calling the notify method.
 *
 * @author tomaz
 */
public abstract class Notificator {

  // number of instances
  private int n = 0;

  // the number of task (used to write status messages)
  protected int taskID = 0;

  public Notificator() {
  }

  public Notificator(int n) {
    this.n = n;
  }

  public void setNumberOfInstances(int n) {
    this.n = n;
  }

  public int getN() {
    return this.n;
  }

  public void setTaskID(int taskID) {
    this.taskID = taskID;
  }

  /**
   * Called when i-th test (out of n) is finished
   *
   * @param i
   */
  public abstract void notify(int i, ExecutionStatus status);

  public static Notificator getNotificator(final String proj, final String alg, final String testSet, final MeasurementType mt) {
    Notificator notificator
            = new Notificator() {

      public void notify(int i, ExecutionStatus status) {
        if ((ATLog.getLogTarget() & ATLog.TARGET_STDOUT) != 0) {
          System.out.println(String.format("[%s, %s, %s]: test %3d / %-3d - %s",
                  alg, testSet, mt.getExtension(), i, this.getN(), status.toString()));
        }

        String statusMsg = String.format("%d/%d # %d%c", i, getN(), 100 * i / this.getN(), '%');
        ASTask tmpTask = new ASTask(proj, alg, testSet, mt.getExtension());
        ASTools.logTaskStatus(tmpTask, ASTaskStatus.INPROGRESS, statusMsg, ATGlobal.getThisComputerID());
      }
    };
    return notificator;
  }

}
