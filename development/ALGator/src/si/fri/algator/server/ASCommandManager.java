package si.fri.algator.server;

import java.io.File;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ArrayUtils;
import si.fri.algator.global.ATGlobal;

/**
 * Razred se uporablja za poganjanje ALGator ukazov (Execute, Analyse) preko spleta.
 * 
 * Odjemalec lahko pošlje naslednja sporočila:
 *   - run command ... poženem ukaz; odjemalec kot odgovor dobi ID ukaza
 *   - status ID   ... vrnem status trenutnega ukaza (new / running / done / stoped)
 *   - output ID   ... vrnem output, ki ga je do sedaj izpisal ukaz
 *   - stop ID     ... poskusim ustaviti izvajanje ukaza
 *   - list        ... izpis seznama vseh trenutnih ukazov
 * 
 * @author tomaz
 */
public class ASCommandManager {
        
  private static int nextID = 0;
  private static ConcurrentHashMap<Integer, ASCommand> commands       = new ConcurrentHashMap<>();
  
  // kaj sem nazadnje vrnil, ko so me vprašali "getOutput(id)"; vrnjeno vrednos si zapomnim in jo
  // prihodnjič primerjam z novim outputom; če sta enaka, potem metoda getOutput vrne OUTPUT_NOT_CHANGED
  private static ConcurrentHashMap<Integer, String> prevCommandOutput  = new ConcurrentHashMap<>();
  
  public static String execute(String prms) {
    sanitize();
    
    String [] params = prms.split(" ");
    if (params.length < 1) return "";    
    
    int id;
    String akcija = params[0];
    switch (akcija) {
      case "run":
        String ukaz = String.join(" ",  ArrayUtils.remove(params, 0));
        return Integer.toString(run(ukaz));
      case "status":
        id = -1;
        try {id=Integer.parseInt(params[1]);} catch (Exception e) {}
        return Integer.toString(getStatus(id));
      case "output":
        id = -1;
        try {id=Integer.parseInt(params[1]);} catch (Exception e) {}
        return getOutput(id);
      case "stop":
        id = -1;
        try {id=Integer.parseInt(params[1]);} catch (Exception e) {}
        return Boolean.toString(stop(id));  
      case "list":
        return getCommandsList();
    }
    return "?";
  }
  
  
  /*
    Pocistim vse ukaze, ki so se zaključili in so je bil njihov output že prebran
  */
  public static void sanitize() {
    Set<Integer> ukazi = commands.keySet();
    for (Integer id : ukazi) {
      ASCommand command = commands.get(id);
      if (command != null && command.canObjectBeDestroyed()) {
        command.deleteOutputFile();
        commands.remove(id);
      }
    }
    
    // pobrišem še vse datoteke v cmdOutput, ki se jih ni nihče dotaknil že več kot en dan
    File cmdOutputFolder = new File(ATGlobal.getCommandOutputFolder());
    String [] outputs = cmdOutputFolder.list();
    if (outputs == null) return;
    for (String output : outputs) {
      File otpFIle = new File(cmdOutputFolder, output);
      long age = (new Date()).getTime() - otpFIle.lastModified();
      if (age > 1000*60*60*24) // is file older than a day?
        otpFIle.delete();
    }
  }
  
  private static String getCommandsList() {
    String result = "";
    for (Integer id : commands.keySet()) {
      result += "\n  " + String.format("%d (%s)", id, getStatusString(id));
    }
    result = (result.isEmpty() ? "No active commands." : "Active commands:") + result;
    return result;
  }
  
  public static int  run(String ukaz) {
    int myID = (nextID++);
    String outputFilename = ATGlobal.getCommandOutputFilename(myID);
    
    ASCommand command = new ASCommand(myID, ukaz, outputFilename);
    commands.put(myID, command);
    
    // pocistim morebitno shranjeno vrednost shranjenega izhoda
    prevCommandOutput.put(myID, ""); 
    
    command.start();
    
    return myID;
  }
  
  private static boolean stop(int id) {
    ASCommand command = commands.get(id);
    if (command != null && command.getStatus().equals(ASCommandStatus.RUNNING)) {
      command.stop();
      return true;
    } else 
      return false; 
  } 
  
  private static int getStatus(int id) {
    ASCommand command = commands.get(id);
    if (command != null)
      return command.getStatus().getCode();
    else return 0; // unknown            
  }
  
  private static String getStatusString(int id) {
    ASCommand command = commands.get(id);
    if (command != null)
      return command.getStatusString();
    else return "unknown";       
  }
  
  private static String getOutput(int id) {
    ASCommand command = commands.get(id);
    if (command != null) {
      String curOutput = command.getOutput();
      
      if (curOutput.equals(prevCommandOutput.get(id)))
        return "OUTPUT_NOT_CHANGED";
      else {
        prevCommandOutput.put(id, curOutput);
        return curOutput;
      }
     
    } else
      return "_NO_OUTPUT_AVAILABLE_";
  }
  
  public static void main(String[] args) {
    System.out.println(ASCommandStatus.ERROR.getCode());
  }
    
}
