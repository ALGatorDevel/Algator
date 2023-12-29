package si.fri.algator.server;

import algator.Version;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import si.fri.algator.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class ASCommand {

  private int ID;              // ID ukaza
  private String commandLine;  // primer: execute BasicSort -a QuickSort
  private String outputFile;   // ime izhodne datoteke (sem se shranjujejo rezultati izhoda)

  private ASCommandStatus status;
  private String           statusString;
  
  private Process proces;
  
  private boolean finalOutputRead; // ali je bil izhod po koncu procesa že prebran (v tem primeru lahko ta objekt odstranimo)
    
  private Date lastTouch = new Date(); // datum zadnjega zanimanja za ta objekt; če se dolgo nihče ne ukvarja z objektom, ga lahko odstranim

  public ASCommand(int ID, String commandLine, String outputFile) {
    this.ID          = ID;
    this.commandLine = commandLine;
    this.outputFile  = outputFile;
    
    this.finalOutputRead = false;

    this.status = ASCommandStatus.NEW;
  }

  public void start() {
    lastTouch = new Date();
    
    String[] parts = commandLine.split(" ");
    String   ukaz = parts[0];
    String[] args = ArrayUtils.removeElement(parts, ukaz);
    
    Object result = executeCommand(ukaz, args, outputFile);
    if (result instanceof Process) {
      status       = ASCommandStatus.RUNNING;
      statusString = status.toString();
      proces = (Process) result;
    } else {
      status       = ASCommandStatus.ERROR;
      statusString = (String) result;
      proces = null;      
    }
  }

  public ASCommandStatus getStatus() {
    lastTouch = new Date();
    
    if (status.equals(ASCommandStatus.RUNNING) && !proces.isAlive()) 
        status = ASCommandStatus.DONE;
    
    if (!status.equals(ASCommandStatus.ERROR))
      statusString = status.toString();
    
    return status;
  }
  
  public String getStatusString() {
    getStatus();
    return statusString;
  }

  public boolean wasFinalOutputRead() {
    lastTouch = new Date();
    
    return finalOutputRead;
  }
    
  
  public void stop() {
    lastTouch = new Date();
    
    proces.destroyForcibly();
    status = ASCommandStatus.STOPPED;
  }
  
  public String getOutput() {
    lastTouch = new Date();
    
    // izhod programa lahko večkrat prebiram med samim izvajanjem ukaza; ko pa se izvajanje enkrat konča, 
    // lahko izhod preberem samo še enkrat - in s tem omogočim, da se ta objekt pobrise!
    if (!getStatus().equals(ASCommandStatus.RUNNING)) finalOutputRead=true;
    
    try {
      return new String(Files.readAllBytes(Paths.get(this.outputFile)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return e.toString();
    }
  }
  
  // objekt lahko odstranime, če gre za pozabljen objekt (z njim se v zadnji uri nihče ni ukvarjal)
  // ali če je pripadajoči proces že končal in je bil izhod že prebran
  public boolean canObjectBeDestroyed() {
    long ageInMinutes = (lastTouch.getTime() - (new Date()).getTime()) / (1000*60);
    boolean forgotten = ageInMinutes > 60;
    return forgotten || wasFinalOutputRead();
  }
  
  public void deleteOutputFile() {
    File f = new File(this.outputFile);
    f.delete();
  }
  
  public static Object executeCommand(String ukaz, String[] args, String outputFilename) { 
    try {                 
      String classPath = Version.getClassesLocation();         
      // When running ALGator with NetBeans, getClassLocation() returns
      // a path to "classes" folder which is not enough to execute ALGator.
      // To enable running ALGator in Netbeans, we add local ALGator distribution to classpath
      if (!classPath.contains("ALGator.jar"))  {      
        classPath += File.pathSeparator +  "dist/ALGator.jar";
        classPath += File.pathSeparator +  "dist/lib/commons-cli-1.2.jar";
      }
      
      String path = System.getenv("JAVA_HOME");
      if (path==null) path ="";
      String jvmCommand = (path.isEmpty() ? "" : path + "/bin/") + "java";
    
      //if (classPath.startsWith("/")) classPath = classPath.substring(1);
      
      String[] command = ArrayUtils.addAll(new String[] {jvmCommand, "-cp", classPath, "-Xss1024k", "algator."+ukaz}, args);
      
      ProcessBuilder probuilder = new ProcessBuilder( command );
      Map<String, String> environment = probuilder.environment();
      environment.put("ALGATOR_ROOT", ATGlobal.getALGatorRoot());
      
      return probuilder.redirectOutput(new File(outputFilename)).start();      
    } catch (Exception e) {
      return e.toString();
    }
  }  
  
  public static void main(String[] args) {
    ASCommand command = new ASCommand(1, "Execute BasicSort -dr /Users/Tomaz/ALGATOR_ROOT/data_root -t TestSet1 -a QuickSort -v 2 -e", "output.my");    
    command.start();
    command.stop();
  }

}
