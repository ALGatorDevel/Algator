package si.fri.algator.client;

import java.io.File;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class AEEGlobal {
    
  private static final String TASK_STATUS_FOLDER         = "taskclients";
  private static final String TASK_CLIENT_LOG_FILENAME   = "taskclient.log";

  public static String getTaskStatusFolder() {
    String aeeFolderName = ATGlobal.getLogFolder() + File.separator + TASK_STATUS_FOLDER + File.separator + ATGlobal.getThisComputerID();
    File aeeFolder       = new File(aeeFolderName);
    if (!aeeFolder.exists())
      aeeFolder.mkdirs();
    
    return aeeFolderName;
  }
  
  public static String getTaskClientLogFilename() {
    return getTaskStatusFolder()+ File.separator + TASK_CLIENT_LOG_FILENAME;
  }
  
}
