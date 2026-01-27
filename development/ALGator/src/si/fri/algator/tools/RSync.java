package si.fri.algator.tools;

/**
 *
 * @author Z.Zorman, tomaz
 *
 * 
 * Pomembno: Ce program teče na windows, za pravilno delovanje potrebuješ program  
 *     cygwin rsync (https://cygwin.com/install.html)
 */

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import si.fri.algator.global.ErrorStatus;

public class RSync {

  /**
   * Makes the destDir to be a mirror of srcDir.
   */
  public static int mirror(String srcDir, String destDir) {
    // to ensure that the files of srcDir will be copied into destDir 
    // (instead of copying a folder srcDir as a subfolder in destDir)
    if (!srcDir.endsWith("/")) {
      srcDir += "/";
    }

    // if destination folder does not exist -> algator creates an empty folder
    File ddir = new File(destDir);
    if (!ddir.exists()) {
      ddir.mkdirs();
    }

    // on Windows, filenames have to be modified
    if (OsUtils.isWindows()) {
      srcDir  = replaceDriveLetter(srcDir);
      destDir = replaceDriveLetter(destDir);
    } 
    
    CommandLine cmd = new CommandLine("rsync");
    cmd.addArgument("-ar");
    cmd.addArgument("--delete");
    cmd.addArgument(srcDir);
    cmd.addArgument(destDir);

    Executor executor = new DefaultExecutor();
    // kill if rsync does not finish in 1 hour (this time should be project dependant)
    executor.setWatchdog(ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get());
    // kill running process if jvm stops working
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());    
    
    int val = -1;
    try {
      val = executor.execute(cmd); 
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, e.toString());
    }
    return val;
  }

  public static boolean rsyncExists() {
    CommandLine cmd = new CommandLine("rsync");
    cmd.addArgument("-h");

    Executor executor = new DefaultExecutor();
    // kill after 5 seconds
    executor.setWatchdog(ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(5)).get());
    // kill running process if jvm stops working
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());    

    try {
      int exitValue = executor.execute(cmd);
      return exitValue == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Method converts filename from windows syntax to cygwin-rsync windows syntax.
   * Example: "C:\\drive\\to\\backup" -> "/cygdrive/C/drive/to/backup"
   * @author Ziga Zorman
   */
  private static String replaceDriveLetter(String dirPath) {
    if (dirPath.matches("((?i)[A-Z]):.*")) {
      return ("/cygdrive/" + dirPath.charAt(0) + dirPath.substring(2, dirPath.length())).replace("\\", "/");
    } else {
      return dirPath;
    }
  }
}
