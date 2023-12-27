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
    
    String[] cmd = new String[]{"rsync", "-ar", "--delete",  srcDir, destDir};
    ProcessBuilder pb = new ProcessBuilder(cmd);
    
    int val = -1;
    try {
      Process p = pb.start();
      val = p.waitFor();
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, e.toString());
    }
    return val;
  }

  public static boolean rsyncExists() {
    String[] cmd = new String[]{"rsync", "-h"};
    ProcessBuilder pb = new ProcessBuilder(cmd);
    int val = -1;
    try {
      Process p = pb.start();
      val = p.waitFor();
    } catch (IOException | InterruptedException e) {}
    return val == 0;
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
