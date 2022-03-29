package si.fri.algotest.tools;

/**
 * Comments by Ziga Zorman:
 * Namestil sem si rsync iz tega paketa: https://cygwin.com/install.html (MinGW 
 * mi ni delal, ker so ga muèile poti do map zaradi ':')
 * Poleg tega, da nisem imel namešèenega rsync-a je bila težava še v tem, da sem moral 
 * popraviti absolutne poti do map katere se sinhronizirajo. Ker rsync razume samo linux 
 * notacijo (C:\tralala\ mu ne gre, sem moral nadomestiti z /cygdrive/c\tralala) - glej
 * metodo replaceDriveLetter.
 */

import java.io.File;
import java.io.IOException;
import si.fri.algotest.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
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

    //changed by Ziga Zorman
    //String[] cmd = new String[]{"rsync", "-az", "--delete", srcDir, destDir};
    //ProcessBuilder pb = new ProcessBuilder(cmd);
    if (OsUtils.isWindows()) {
      srcDir  = replaceDriveLetter(srcDir);
      destDir = replaceDriveLetter(destDir);
    }
    String[] cmd = new String[]{"rsync", "-a", srcDir, destDir};
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
    } catch (IOException | InterruptedException e) {
    }

    return val == 0;
  }

  /**
   * It states that rsync considers any directory with a colon to be remote, but
   * we can use cygwin or unix style representations:
   * "/cygdrive/<drive_letter><separator char>dir" or "/<drive_letter><separator
   * char>dir".
   *
   * @param dirPath - path to the directory
   * @return valid rsync path representation
   * @author Ziga Zorman
   */
  private static String replaceDriveLetter(String dirPath) {
    if (dirPath.matches("((?i)[A-Z]):.*")) {
      return "/cygdrive/" + dirPath.charAt(0) + dirPath.substring(2, dirPath.length());
    } else {
      return dirPath;
    }
  }

}
