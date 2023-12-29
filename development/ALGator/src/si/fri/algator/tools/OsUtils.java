package si.fri.algator.tools;

/**
 * 
 * @author Ziga Zorman
 */
public class OsUtils {

  private static String OS = null;

  public static String getOsName() {
    if (OS == null) {
      OS = System.getProperty("os.name").toUpperCase();
    }
    return OS;
  }

  public static boolean isWindows() {
    return getOsName().contains("WINDOWS");
  }

  public static boolean isUnix() {
    return getOsName().contains("UNIX");
  }

}
