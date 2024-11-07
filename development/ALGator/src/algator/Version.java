package algator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import si.fri.algator.ausers.AUsersTools;
import si.fri.algator.database.Database;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;


/**
 *
 * @author tomaz
 */
public class Version {
  private static String version = "0.986";
  private static String date    = "November 2023";
  
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
  
  public static void printUsers() {
    Connection conn = Database.getConnectionToDatabase();
    if (conn==null) {
      System.out.println("Error: Can't connect to database.");
      return;
    }
    
    try {
      Statement stmt = (Statement) conn.createStatement();    
      ResultSet rs = stmt.executeQuery("SELECT * FROM authuser_user");
      System.out.print("Users in database:  ");
      while (rs.next()) {
          System.out.print(rs.getString("username") + (rs.isLast() ? "" :", "));
      }
      System.out.println("");
    } catch (Exception e) {
      System.out.println("Error: " + e.toString());
    }
  }
  
  public static void printVersion() {
    System.out.printf("ALGator, %s, build %s\n", getVersion(), ATGlobal.getBuildNumber());
    System.out.println();
    System.out.println("ALGATOR_ROOT:       " + ATGlobal.getALGatorRoot());
    System.out.println("ALGATOR_DATA_ROOT:  " + ATGlobal.getALGatorDataRoot());
    System.out.println("ALGATOR_DATA_LOCAL: " + ATGlobal.getALGatorDataLocal());  
    System.out.println("Anonymous mode:     " + Database.isAnonymousMode());
    
    if (!Database.isAnonymousMode() && !AUsersTools.databaseAndTablesExist()) {
      ATLog.log("The database is not initialized.",0  );
      return;
    }  
    
    if (!Database.isAnonymousMode())
      printUsers();
  }
  
  public static void main(String[] args) {
    printVersion();
  }
}
