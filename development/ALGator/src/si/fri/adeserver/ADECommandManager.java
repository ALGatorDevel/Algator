package si.fri.adeserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ArrayUtils;
import si.fri.algotest.global.ATGlobal;

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
 * 
 * 
 * 
 * @author tomaz
 */
public class ADECommandManager {
        
  private static int nextID = 0;
  private static ConcurrentHashMap<Integer, ADECommand> commands = new ConcurrentHashMap<>();
  
  public static String execute(ArrayList<String> prms) {
    sanitize();
    
    if (prms.size() < 1) return "";    
    String [] params = (String []) prms.toArray(new String[0]);
    
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
      ADECommand command = commands.get(id);
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
    
    ADECommand command = new ADECommand(myID, ukaz, outputFilename);
    commands.put(myID, command);
    command.start();
    
    return myID;
  }
  
  private static boolean stop(int id) {
    ADECommand command = commands.get(id);
    if (command != null && command.getStatus().equals(ADECommandStatus.RUNNING)) {
      command.stop();
      return true;
    } else 
      return false; 
  } 
  
  private static int getStatus(int id) {
    ADECommand command = commands.get(id);
    if (command != null)
      return command.getStatus().getCode();
    else return 0; // unknown            
  }
  
  private static String getStatusString(int id) {
    ADECommand command = commands.get(id);
    if (command != null)
      return command.getStatusString();
    else return "unknown";       
  }
  
  private static String getOutput(int id) {
    ADECommand command = commands.get(id);
    if (command != null) {
      return command.getOutput();
    } else
      return "_NO_OUTPUT_AVAILABLE_";
  }
  
  public static void main(String[] args) {
    System.out.println(ADECommandStatus.ERROR.getCode());
  }
    
}