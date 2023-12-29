package si.fri.algator.client;

import algator.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import si.fri.algator.server.ASGlobal;
import si.fri.algator.entities.EAlgatorConfig;
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.global.ATGlobal;

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
            .withDescription("the ALGatorServer name")
            .create("s");
    
    Option port = OptionBuilder.withArgName("server_port")
            .withLongOpt("port")
            .hasArg(true)
            .withDescription("the ALGatorServer port")
            .create("p");
    
    
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
    options.addOption(port);
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
   
  public static String askALGatorServer(String request) {
    String server = "localhost"; 
    int    port   = EAlgatorConfig.getALGatorServerPort();
    return askALGatorServer(server, port, request);
  }
  public static String askALGatorServer(String hostName, int port, String post) {    
    String[] parts = post.split(" ", 2);
    String request = parts[0];
    String body    = parts.length > 1 ? parts[1] : "";

    try {
      URL url = new URL(String.format("http://%s:%d/%s", hostName, port, request));

      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setConnectTimeout(3000); 
      con.setReadTimeout   (3000);
      con.setRequestMethod ("POST");

      if (!body.isEmpty()) {  // add post request parameters
        con.setDoOutput(true);
        OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
        osw.write(body);
        osw.flush();osw.close();
      }

      con.connect(); 
      
      int responseCode = con.getResponseCode();
      
      if (responseCode != 200)
        return "Error (respose code: "+responseCode+")";
      
      String response = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
      
      con.disconnect();
       
      return response;      
    } catch (MalformedURLException e) {
      return ASGlobal.ERROR_PREFIX + "Unknown host " + hostName;
    } catch (IOException e) {
      return ASGlobal.ERROR_SERVER_DOWN + e;
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
      
      String serverName = ELocalConfig.getConfig().getALGatorServerName();
      if (line.hasOption("server")) {
	serverName = line.getOptionValue("server");
      }
      
      int port = ELocalConfig.getConfig().getALGatorServerPort();
      if (line.hasOption("port")) {
	port = Integer.parseInt(line.getOptionValue("port"));
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
        AEETaskClient.initTaskClient();
        return;
      }       
      
      if (type == 0) {
        AEETaskClient.runClient(serverName, port);
      } else {        
        String request = String.join(" ", args);
        System.out.println(askALGatorServer(serverName, port, request));
      }            
      
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }
  
}
