package si.fri.aeeclient;

import algator.*;
import static algator.Admin.initAlgatorSystem;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import si.fri.adeserver.ADEGlobal;
import si.fri.algotest.entities.ELocalConfig;
import si.fri.algotest.global.ATGlobal;

/**
 *
 * @author tomaz
 */
public class Requester {
  
  public static String programName;

  private static Options getOptions() {
    Options options = new Options();

    Option data_root = OptionBuilder.withArgName("folder")
            .withLongOpt("data_root")
            .hasArg(true)
            .withDescription("use this folder as data_root; default value in $ALGATOR_DATA_ROOT (if defined) or $ALGATOR_ROOT/data_root")
            .create("dr");

    Option data_local = OptionBuilder.withArgName("folder")
            .withLongOpt("data_local")
            .hasArg(true)
            .withDescription("use this folder as data_LOCAL; default value in $ALGATOR_DATA_LOCAL (if defined) or $ALGATOR_ROOT/data_local")
            .create("dl");    
    
    Option algator_root = OptionBuilder.withArgName("folder")
            .withLongOpt("algator_root")
            .hasArg(true)
            .withDescription("use this folder as algator_root; default value in $ALGATOR_ROOT")
            .create("r");

    Option server = OptionBuilder.withArgName("server_name")
            .withLongOpt("server")
            .hasArg(true)
            .withDescription("the name of the server with TaskServer")
            .create("s");
    
    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF (default), 1 = some, 2 = all")
            .create("v");
    
    Option init = OptionBuilder
	    .hasArg(false)
	    .withDescription("initialize the task client")
	    .create("init");  
    
    options.addOption(data_root);
    options.addOption(data_local);
    options.addOption(algator_root);
    options.addOption(server);
    options.addOption(verbose);
    options.addOption(init);

    options.addOption("h", "help", false,
            "print this message");

    return options;
  }

  private static void printMsg(Options options) {
    System.out.println("ALGator " + programName + ", " + Version.getVersion() + "\n");

    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("algator." + programName + " [options]", options);
  }
  
  public static String askTaskServer(String hostName, int portNumber, String request) {
    if (hostName == null)
      hostName   = ELocalConfig.getConfig().getTaskServerName();
    
    if (portNumber==0)
      portNumber = ELocalConfig.getConfig().getTaskServerPort();
    
    String compID = ELocalConfig.getConfig().getComputerID();

      try (
        Socket kkSocket = new Socket(hostName, portNumber);
        PrintWriter    toServer    = new PrintWriter(kkSocket.getOutputStream(), true);
        BufferedReader fromServer  = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));) 
      {
          request = Base64.getEncoder().encodeToString(request.getBytes());
          toServer.println(request);
          
          String response = fromServer.readLine();
          response = new String(Base64.getDecoder().decode(response));
                  ;
          return response;
          
      } catch (Exception e) {
        return String.format("Error: TaskServer on '%s:%d' is not running.", hostName, portNumber);
      }          
  }

  
  // type: 0 ... tastServer, 1...Request
  public static void do_main(String args[], int type) {
    Options options = getOptions();

    CommandLineParser parser = new BasicParser();
    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("h")) {
	printMsg(options);
        return;
      }
            
      String algatorRoot = ATGlobal.getALGatorRoot();
      if (line.hasOption("algator_root")) {
        algatorRoot = line.getOptionValue("algator_root");        
      }
      ATGlobal.setALGatorRoot(algatorRoot);

      String dataRoot = ATGlobal.getALGatorDataRoot();
      if (line.hasOption("data_root")) {
	dataRoot = line.getOptionValue("data_root");
      }
      ATGlobal.setALGatorDataRoot(dataRoot);
      
      String dataLocal = ATGlobal.getALGatorDataLocal();
      if (line.hasOption("data_local")) {
	dataLocal = line.getOptionValue("data_local");
      }
      ATGlobal.setALGatorDataLocal(dataLocal);            
      
      String serverName = null;
      if (line.hasOption("server")) {
	serverName = line.getOptionValue("server");
      }
            
      ATGlobal.verboseLevel = 0;
      if (line.hasOption("verbose")) {
        if (line.getOptionValue("verbose").equals("1")) {
          ATGlobal.verboseLevel = 1;
        }
        if (line.getOptionValue("verbose").equals("2")) {
          ATGlobal.verboseLevel = 2;
        }
      }
      
      if (line.hasOption("init")) {
        AEETaskClient.initTaskServer();
        return;
      }          
           
            
      if (type == 0) {
        AEETaskClient.runClient(serverName);
      } else {
        
        String request = "";
        for (String arg : line.getArgs()) 
          request += (request.isEmpty() ? "" : " ") + arg;
        

        System.out.println(askTaskServer(serverName, 0, request));
        return;
      }
            
      
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }
  
}
