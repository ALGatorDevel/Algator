package si.fri.algator.server;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.MultipartConfigElement;
import si.fri.algator.ausers.AUsersHelper;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.global.ATGlobal;
import static spark.Spark.*;


/**
 * ALGator ALGatorServer connects ALGator with the world.
 *
 * For more information about server's requests and answers, see ALGatorServer.docx
 *
 * @author tomaz
 */
public class Server {

  RequestProcessor processor;   /// processor that processes the requests
  
  private long timeStarted;
  
  public Server() {
    processor = new RequestProcessor(this);
  }
  
 
  public String getServerRunningTime() {
    long seconds = (new Date().getTime() - timeStarted) / 1000;
    int day = (int) TimeUnit.SECONDS.toDays   (seconds);
    long hours    = TimeUnit.SECONDS.toHours  (seconds) - (day * 24);
    long minute   = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
    long second   = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

    String result = second + " sec.";
    if (minute > 0) result = minute + " min" + ", " + result;    
    if (hours > 0)  result = hours + " h" + ", " + result;    
    if (day > 0)    result = day + " day(s)" + ", " + result;    

    return result;
  }
  
  public void run() {
    timeStarted = new java.util.Date().getTime();
    
    long maxFileSize         = 100000000; // max upload file size: 100 MB
    long maxRequestSize      = 100000000; 
    int fileSizeThreshold    = 1024; 
    String tmpUploadLocation = ATGlobal.getALGatorDataLocal() + File.separator + "webupload_tmp";
    
    int port    = EAlgatorConfig.getALGatorServerPort();
    String host = "localhost";  // =EAlgatorConfig.getALGatorServerName();
    
    port(port);
    
    //ipAddress("localhost");
    //if (!host.equals("localhost"))
    //  ipAddress(host);

    ASLog.log(String.format("ALGatorServer Initialized on %s:%s ", host, port));    
    
    threadPool(16);
    
    before((request, response) -> {
      request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tmpUploadLocation));    
//      request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tmpUploadLocation, maxFileSize, maxRequestSize, fileSizeThreshold));      
    });

    
    post("/*", (req, res) -> {
      String pParams = req.body();

      String path = req.pathInfo().toUpperCase();
      if (path.length() > 0 && path.charAt(0)=='/') path = path.substring(1);
            
      String uid = AUsersHelper.getUIDFromHeaders(req);
      
      if (!ASGlobal.nonlogableRequests.contains(path)) 
          ASLog.log("[REQUEST from "+uid+"]:  " + path + " " + pParams);
      
      String response = processor.processRequest(path, pParams, req, res, uid);
      if (response.startsWith("{"))
        res.header("Content-type", "application/json");
              
      if (!ASGlobal.nonlogableRequests.contains(path)) {
          ASLog.log(String.format("[RESPONSE]: %s", response.replaceAll("\n", "; ")));
      }

      return response;  
    });  
    
    // allow OPTIONS queries (before POST requests)
    options("/*", (req, res) ->{
      return "";
    });
    
    after((request, response) -> {      
      response.header("Access-Control-Allow-Origin", "*");
      response.header("Access-Control-Allow-Methods", "POST, OPTIONS");
      response.header("Access-Control-Allow-Headers", "Content-Type, Content-Encoding, SessionID");
      response.header("Access-Control-Max-Age", "5");

      response.header("Content-Encoding", "gzip");            
    });
 }
}
