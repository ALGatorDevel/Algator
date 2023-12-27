package si.fri.algator.server;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.input.ReversedLinesFileReader;

/**
 *
 * @author tomaz
 */
public class ASLog {
  public static boolean doVerbose = false;
  private static String logFileName = null;
  
  // maximal error size in bytes (to truncate very long error messages)
  private static int MAX_ERROR_SIZE = 256;
  
  
  public static void log(String msg) {
    if (logFileName == null) {
      logFileName = ASGlobal.getALGatorServerLogFilename();
    }
    
    String sufix = msg.length() > MAX_ERROR_SIZE ? " ..." : "";
    msg = msg.substring(0, Math.min(msg.length(), MAX_ERROR_SIZE)) + sufix;

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
    try {
      int counter = 0; String result = ""; 
      ReversedLinesFileReader object = new ReversedLinesFileReader(new File(logFileName), Charset.forName("UTF-8"));
      while(counter < n) {
         result = object.readLine() + "\n" + result;
      counter++;
      }    
      return String.format("Content of '%s' (last %d lines) : \n %s\n", logFileName, n,result);      
    } catch (Exception e) {
      return "Error reading log file: " + e.toString();
    }
  }
}
