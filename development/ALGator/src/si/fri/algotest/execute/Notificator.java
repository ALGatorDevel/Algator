package si.fri.algotest.execute;

import si.fri.adeserver.STask;
import si.fri.adeserver.ADETools;
import si.fri.adeserver.TaskStatus;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.global.ExecutionStatus;

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
        STask tmpTask = new STask(proj, alg, testSet, mt.getExtension());
        ADETools.logTaskStatus(tmpTask, TaskStatus.INPROGRESS, statusMsg, ATGlobal.getThisComputerID());
      }
    };
    return notificator;
  }

}
