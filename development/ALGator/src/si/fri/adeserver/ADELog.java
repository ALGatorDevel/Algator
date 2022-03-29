package si.fri.adeserver;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 *
 * @author tomaz
 */
public class ADELog {
  public static boolean doVerbose = false;
  private static String logFileName = null;
  
  // maximal error size in bytes (to truncate very long error messages)
  private static int MAX_ERROR_SIZE = 1024;
  
  
  public static void log(String msg) {
    if (logFileName == null) {
      logFileName = ADEGlobal.getTaskserverLogFilename();
    }
    
    msg = msg.substring(0, Math.min(msg.length(), MAX_ERROR_SIZE));

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    String logMsg = String.format("%15s: %s", dateFormat.format(date), msg);
    
    try (PrintWriter pw = new PrintWriter(new FileWriter(logFileName, true))) {
      pw.println(logMsg);
      if (doVerbose)
        System.out.println(logMsg);
    } catch (Exception e) {
      // error in logging can not be loged
    }
  }
  
  /**
   * Returns the last n log messages
   */
  public static String getLog(int n) {
    try (Scanner sc = new Scanner(new File(logFileName))){      
      sc.useDelimiter("\\Z");
      String vrstice[] =  sc.next().split("\n");
      if (vrstice.length > 0) {
        n = Math.min(n, vrstice.length);
        String result=vrstice[vrstice.length-n];
        for (int i = vrstice.length - n + 1; i < vrstice.length; i++) {
          result += "\n" + vrstice[i];
        }
        return result;
      } else return "";
    } catch (Exception e) {
            return "Error reading log file: " + e.toString();
    }
  }
}
