package si.fri.algator.global;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/**
 * Logiram vse dogajanje. Ob zagonu programa vse morebitne napake pri preverjanju parametrov zapisujem v 
 * algator.log. Sporočila povezana z izvajanjem algoritma pa zapisujem v datoteko P-A-T-em.history. 
 * 
 * @author tomaz
 */
public class ATLog {
   
  public static final int TARGET_OFF    = 0;
  public static final int TARGET_STDOUT = 1;
  public static final int TARGET_FILE   = 2;

  private static boolean loggingEnabled = true;
  
  // maximal error size in bytes (to truncate very long error messages)
  private static int MAX_ERROR_SIZE = 2048;

  // filename for algator specific logging
  private static String algatorLogFileName = "";
  
  
  private static ArrayList<String> reportedErrors = new ArrayList<>();
  
  // filename for Project-Algorithm-Testset-EM specific logging
  private static String pateFilename = "";
  public static void setPateFilename(String pateFN) {
    pateFilename = pateFN;

    if (ATGlobal.logTarget == TARGET_FILE) {
      // ob priklopu na datoteko izpišem ločilo, da so logi jasno ločeni eden od drugega
      try (PrintWriter pw = new PrintWriter(new FileWriter(new File(pateFN), true))) {
        pw.println("-------------------------------------------------------------");
      } catch (Exception e) {}
    }
  }
  
  
  // returns lines of log file, skip first "from" lines
  // (if from==0, return all file; if from==1, return all file but first line
  public static ArrayList<String> readLogFile(int from) {
    ArrayList<String> result = new ArrayList();
    try (Scanner sc = new Scanner(new File(pateFilename))) {
      for (int i = 0; i < from; i++) sc.nextLine();
      while(sc.hasNextLine()) result.add(sc.nextLine());
    } catch (Exception e) {} 
    return result;
  }
  
  public static void disableLog() {
    loggingEnabled = false;
  }
  public static void enableLog(){
    loggingEnabled = true;
  }
  
  
  /**
   * Kam se logira:
   * - če je ATGlobal.logTarget & TARGET_STDOUT (1) != 0 .... logiras na stdout
   * - če je ATGlobal.logTarget & TARGET_FILE (2) != 0 
   *     če je pateFilename nastavljen -> v pateFilename
   *     sicer v algatorLogFileName
   * 
   * 
   */
  public static void log(String msg) {
    if (!loggingEnabled || ATGlobal.logTarget == TARGET_OFF || ATGlobal.verboseLevel == 0) 
      return;
    
    msg = msg.substring(0, Math.min(msg.length(), MAX_ERROR_SIZE));

    String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
    String logMsg = String.format("%15s: %s", date, msg);
    
    boolean alreadyReported = reportedErrors.contains(logMsg);
    if (!alreadyReported)
      reportedErrors.add(logMsg);
    
    // to prevent duplicated reporting 
    if (alreadyReported) return;

    
    // loggint to stdout
    if ((ATGlobal.logTarget & TARGET_STDOUT) != 0)
      System.out.println(logMsg);

    
    
    // loging to file
    if ((ATGlobal.logTarget & TARGET_FILE) != 0) {
      if (algatorLogFileName.isEmpty()) algatorLogFileName = ATGlobal.getAlgatorLogFilename();
      
      String fileToLog = algatorLogFileName;
    
      if (!pateFilename.isEmpty()) fileToLog = pateFilename;
      
      try (PrintWriter pw = new PrintWriter(new FileWriter(new File(fileToLog), true))) {
        pw.println(logMsg);
      } catch (Exception e) {} // nothing can be done          
    }      
  }
  
}
