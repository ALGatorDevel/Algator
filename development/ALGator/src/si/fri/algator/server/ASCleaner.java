package si.fri.algator.server;

import java.util.Timer;
import java.util.TimerTask;
import si.fri.algator.global.ATLog;

/**
 * Razred se uporablja za redno čiščenje strežniških podatkov.
 * 
 * @author tomaz
 */
public class ASCleaner {
  static int ticks = 0;
  
  // this method is periodically called every minute 
  static void clearData() {
    ticks++;
        
    // tasks, that will preform every hour
    if (ticks % 60 == 0) {
      // clean activeTasks queue to remove old CANCELED and INPROGRESS tasks
      int cleanedTasks = ASTools.cleanActiveTaskQueue();
      if (cleanedTasks > 0)
        ASLog.log("Server data cleaner cleaned " + cleanedTasks + " tasks.");

      int cleanedAWR = ASTools.clearAWResults();
      if (cleanedAWR > 0)
        ASLog.log("Server data cleaner cleaned " + cleanedAWR + " awrs."); 
    }
    
    // tasks, that will preform every day
    if (ticks % (24*60) == 0) {      
      // ...     
    }

    // tasks, that will preform every month
    if (ticks % (24*60*30) == 0) {      
      // ...     
    }
    
  }
  
  static void runCleaningDeamon() {
    Timer timer = new Timer();

    // Schedule a task to run every 10 seconds
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        clearData();
      }
    }, 0, 60 * 1000); // Delay = 0ms, Period = 1 minute

  }
}
