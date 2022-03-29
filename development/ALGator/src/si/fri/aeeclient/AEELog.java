package si.fri.aeeclient;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author tomaz
 */
public class AEELog {
  private static String logFileName = null;
  
  // maximal error size in bytes (to truncate very long error messages)
  private static int MAX_ERROR_SIZE = 2048;
  
  
  public static void log(String msg) {
    if (logFileName == null) {
      logFileName = AEEGlobal.getTaskClientLogFilename();
    }
    
    msg = msg.substring(0, Math.min(msg.length(), MAX_ERROR_SIZE));

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    String logMsg = String.format("%15s: %s", dateFormat.format(date), msg);
    
    try (PrintWriter pw = new PrintWriter(new FileWriter(logFileName, true))) {
      pw.println(logMsg);
    } catch (Exception e) {
      // error in logging can not be loged
    }
  }
}
