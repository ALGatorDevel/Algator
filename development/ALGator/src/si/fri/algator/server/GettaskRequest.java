package si.fri.algator.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.tools.ATTools;

/**
 *  Data about a getTask requests
 * @author tomaz
 */
public class GettaskRequest {
  public static final String GTR_NO_TASKS  = "no_task_assigned";
  public static final String GTR_ALG_EXEC  = "algorithm_execution";
  public static final String GTR_TASK_NEW  = "task_new";
  public static final String GTR_TASK_CONT = "task_continue";

  
  // number of last requests to store for each client
  private final static int MAX_REQUESTS = 100; 
  
  // interval to hold info about clients request; if the last client's
  // request was made before MAX_DELAY seconds, logs are deleted
  private final static int MAX_DELAY = 60;// 1 minute   60*60*24;  // 1 day 
  
  // a map of "getTask" requests send to this ALGatorServer
  // key=client_id, value=list of requests by this client
  static HashMap<String, ArrayList<GettaskRequest>> requests = new HashMap<>();
  
  
  long   timestamp;    // when request was made
  long   lastUpdate;   // when request was updated
  String cid;          // id of computer that made a request
  String cDesc;        // computer description (i.e. F0.C0)
  String response;     // what was a response (no_task_assigned, task_new, task_continue)
  String status;          // last update, done, killed, failed, paused, canceled
  String taskid;       // id of assigned task (or null if response = no_tasks)
  String taskProgress; // what was the progress of task when it was assigned to this request
  int    idle;         // how many times no_tasks was returned

  public GettaskRequest(long timestamp, String cid) {    
    this.timestamp = timestamp;
    this.cid       = cid;
    this.idle      = 0;
    this.status    = "last update"; 
  }
  
  public void setParams(String response, String taskid, String taskProgress) {
    this.response     = response;
    this.taskid       = taskid;
    this.taskProgress = taskProgress;
    
    this.lastUpdate   = System.currentTimeMillis();
  }
  
  public void setTimestamp(String status) {
    this.status       = status;
    this.lastUpdate   = System.currentTimeMillis();
  }
  
  public void setComputerDesc(String cDesc) {
    this.cDesc     = cDesc;
  }
  
  
  // remove log of client that did not send a request last MAX_DELAY seocnds
  static void sanitize() {
    Set<String> clients = requests.keySet();
    long now = System.currentTimeMillis();
    for (String client : clients) {
      ArrayList<GettaskRequest> rqList = requests.get(client);
      if (now - rqList.get(0).timestamp > 1000*MAX_DELAY)
        requests.remove(client);
    }
  }
  
  static GettaskRequest newGettaskRequest(String cid) {
    if (!requests.containsKey(cid)) // first request by this computer?
      requests.put(cid, new ArrayList());
    ArrayList<GettaskRequest> creqs = requests.get(cid);
    
    GettaskRequest gtr = null;
    long timestamp = System.currentTimeMillis();
    
    // if first loged response is "no_tasks", don't create another, just 
    // change timestamp and increase "idle" counter
    if (!creqs.isEmpty() && GTR_NO_TASKS.equals(creqs.get(0).response)) {
      gtr = creqs.get(0);
      gtr.timestamp = timestamp;
      gtr.idle++;
    } else {
      gtr = new GettaskRequest(timestamp, cid);
      gtr.response = GTR_NO_TASKS; // default response
    
      if (creqs.size()>=MAX_REQUESTS) // if list is "full", remove last
        creqs.remove(creqs.size()-1); 
      creqs.add(0,gtr);
    }
    
    sanitize();
    return gtr;
  }
  
  // find a GetTask request from a given computer that returned task with taskID
  static GettaskRequest findGettaskRequest(String cid, String taskID) {
    ArrayList<GettaskRequest> reqs = requests.get(cid);
    if (reqs != null) {
      for (GettaskRequest req : reqs) {
        if (taskID.equals(req.taskid)) return req;
      }
    }
    return null;
  }
  
  // search all requests and return the one belonging to task with given taskID
  static GettaskRequest findGettaskRequestByTaskID(String taskID) {
    for (ArrayList<GettaskRequest> reqs : requests.values()) {
      for (GettaskRequest req : reqs) {
        if (taskID.equals(req.taskid)) return req;
      }      
    }
    return null;
  }
  
  static String getClientStatusForRequest(GettaskRequest request) {
    String when     = ATTools.timeAgo(request.timestamp, " ago",  60);
    String updated  = ATTools.timeAgo(request.lastUpdate, " ago", 60);
    String response = request.response;
    String taskId   = request.taskid;
    String progress = request.taskProgress;
    String status   = request.status; 
    
    String suffix = "";
    
    switch (response) {
      case GTR_NO_TASKS:
        suffix = when.equals("Now") ? 
           String.format(" - client idle for %d s", request.idle) : "... client not connected";
        break;        
      case GTR_ALG_EXEC: case GTR_TASK_NEW: case GTR_TASK_CONT:
        suffix = String.format(" - task: %s, progress %s, %s: %s", taskId, progress, status, updated);
    }
    
    return String.format("%s (status set: %s)", response, when)  + suffix;
  }
  
  static JSONArray getActiveClients(boolean details) {
    JSONArray activeClients = new JSONArray();
    for (String taskClient : requests.keySet()) {
      ArrayList<GettaskRequest> reqs  = requests.get(taskClient);
      if (!reqs.isEmpty()) {
        JSONObject clientO = new JSONObject();
        clientO.put("Client", taskClient);
        clientO.put("ComputerName", reqs.get(0).cDesc);

        if (details) {
          JSONArray rStatus = new JSONArray();
          for (GettaskRequest req : reqs) {
            rStatus.put(getClientStatusForRequest(req));
          }
          clientO.put("RStatus", rStatus);
        } else {
          clientO.put("Status", getClientStatusForRequest(reqs.get(0)));
        }
        activeClients.put(clientO);
      }
    }
    return activeClients;
  }
  
  
}
