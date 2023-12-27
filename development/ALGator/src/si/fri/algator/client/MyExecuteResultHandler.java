package si.fri.algator.client;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;

/**
 * See also http://commons.apache.org/proper/commons-exec/xref-test/org/apache/commons/exec/TutorialTest.html
 * @author tomaz
 */
public class MyExecuteResultHandler extends DefaultExecuteResultHandler {

  private ExecuteWatchdog watchdog;

  public MyExecuteResultHandler(final ExecuteWatchdog watchdog) {
    this.watchdog = watchdog;
  }

  public MyExecuteResultHandler(final int exitValue) {
    super.onProcessComplete(exitValue);
  }

  @Override
  public void onProcessComplete(final int exitValue) {
    super.onProcessComplete(exitValue);
  }

  @Override
  public void onProcessFailed(final ExecuteException e) {
    super.onProcessFailed(e);
    if (watchdog != null && watchdog.killedProcess()) {
      // process timed out
    } else {
      // process failed 
    }
  }
}

