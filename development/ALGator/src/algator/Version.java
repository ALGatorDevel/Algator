package algator;

import si.fri.algotest.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class Version {
  private static String version = "0.95";
  private static String date    = "Oktober 2019";
  
  public static String getVersion() {
    return String.format("version %s (%s)", version, date);
  }
  
  /**
   * Method returns the location of the classes of this project, i.e. the location of the JAR
   * file, if the program was executed from JAR, or the root folder of project's classes otherwise  
   */
  public static String getClassesLocation() {
    try {
      return Version.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    } catch (Exception e) {
      return "";
    }
  }
  
  public static void printVersion() {
    System.out.printf("ALGator, %s, build %s\n", getVersion(), ATGlobal.getBuildNumber());
    System.out.println();
    System.out.println("ALGATOR_ROOT:       " + ATGlobal.getALGatorRoot());
    System.out.println("ALGATOR_DATA_ROOT:  " + ATGlobal.getALGatorDataRoot());
    System.out.println("ALGATOR_DATA_LOCAL: " + ATGlobal.getALGatorDataLocal());
  }
  
  public static void main(String[] args) {
    printVersion();
  }
}
