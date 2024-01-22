package algator;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.PrintStream;
import java.util.Scanner;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import si.fri.algator.database.Database;
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.users.UsersDatabase;

import static si.fri.algator.admin.Maintenance.createProject;
import static si.fri.algator.admin.Maintenance.createAll;
import static si.fri.algator.admin.Maintenance.addIndicator;
import static si.fri.algator.admin.Maintenance.addIndicatorTest;
import static si.fri.algator.admin.Maintenance.addParameter;
import static si.fri.algator.admin.Maintenance.addTestCaseGenerator;
import static si.fri.algator.admin.Maintenance.createAlgorithm;
import static si.fri.algator.admin.Maintenance.createPresenter;
import static si.fri.algator.admin.Maintenance.createTestset;
import static si.fri.algator.admin.Maintenance.getInfo;
import static si.fri.algator.admin.Maintenance.removePresenter;


/**
 *
 * @author tomaz
 */
public class Admin {
  private static String introMsg = "ALGator Admin, " + Version.getVersion();
  

  private static Options getOptions() {
    Options options = new Options();

    Option data_root = OptionBuilder.withArgName("folder")
	    .withLongOpt("data_root")
	    .hasArg(true)
	    .withDescription("use this folder as data_root; default value in $ALGATOR_DATA_ROOT (if defined) or $ALGATOR_ROOT/data_root")
	    .create("dr");

    Option algator_root = OptionBuilder.withArgName("folder")
	    .withLongOpt("algator_root")
	    .hasArg(true)
	    .withDescription("use this folder as ALGATOR_ROOT")
	    .create("r");
    
    
    Option algorithm = OptionBuilder.withArgName("algorithm_name")
	    .withLongOpt("algorithm")
	    .hasArg(true)
	    .withDescription("the name of the algorithm to print info of")
	    .create("a");    
    
    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF (default), 1 = some, 2 = all")
            .create("v");
        
    Option presenterType = OptionBuilder.withArgName("presenter_type")
            .withLongOpt("pType")
            .hasArg(true)
            .withDescription("presenter type (0=MainProject, 1=Project (default), 2=MainAlg, 3=Alg); applicable for create_presenter")
            .create("pt");
    
    Option username = OptionBuilder.withArgName("username")	    
	    .hasArg(true)
	    .withDescription("the name of the current user")
	    .create("u");

    Option password = OptionBuilder.withArgName("password")	    
	    .hasArg(true)
	    .withDescription("the password of the current user")
	    .create("p");  
    
    Option init = OptionBuilder
	    .hasArg(false)
	    .withDescription("initialize the system")
	    .create("init");         
    
    options.addOption(data_root);
    options.addOption(algator_root);    
    options.addOption(algorithm);
    options.addOption(verbose);
    options.addOption(presenterType);
    
    options.addOption(username);
    options.addOption(password);
    
    options.addOption(init);

    
    options.addOption("h", "help", false,
	    "print this message");
        
    options.addOption("cp", "create_project", false,
	    "create a new project; args: project_name");
    options.addOption("call", "create_all", false,
	    "create a new project with all its components; args: project_name");
    
    options.addOption("ca", "create_algorithm", false,
	    "create a new algorithm for a given project; args: project_name algorithm_name");
    options.addOption("ct", "create_testset", false,
	    "create a new testset for a given project; args: project_name testset_name");    
    options.addOption("cdp", "create_presenter", false,
	    "create a new presenter for a given project; args: project_name presenter_name");
    options.addOption("rdp", "remove_presenter", false,
	    "remove a presenter for a project; args: project_name presenter_name");

    options.addOption("cit", "add_indicator_test", false,
	    "add a new indicator test; args: proj_name indicator_name");
    options.addOption("cgn", "add_generator", false,
	    "add a new generator; args: proj_name type comma_separated_list_of_parameters");
    options.addOption("cpa", "add_parameter", false,
	    "add a new parameter; args: proj_name [parameter_jsno_description]");
    options.addOption("cin", "add_indicator", false,
	    "add a new indicator; args: proj_name [indicator_jsno_description]");
        
    options.addOption("use", "usage", false, "print usage guide");
    options.addOption("i", "info", false, "print info about entity");
    options.addOption("ei", "extinfo", false, "print extended info about entity");
    
    
    return options;
  }

  private static void printMsg(Options options) {    
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("algator.Admin [options] <project_name> <algorithm_name>", options);
  }

  private static void printUsage() {
    Scanner sc = new Scanner((new Chart()).getClass().getResourceAsStream("/data/AdminUsage.txt")); 
    while (sc.hasNextLine())
      System.out.println(sc.nextLine());    
  }
  
  public static String do_admin(String[] sinput) {    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStream old = System.out;
    System.setOut(ps);

    main(sinput);

    System.out.flush();
    System.setOut(old);    
    return baos.toString();
  }
  
  private static String[] getUsernameAndPassword() {
    Console console = System.console();
    if (console == null) return null;
    
    String up[] = new String[2];
    up[0]=console.readLine("Username: ");
    
    do {
      up[1]=new String(console.readPassword("Password: "));
      String control = new String(console.readPassword("Password (check): "));
      if (!up[1].equals(control)) {
        System.out.println("Passwords do not match. Please try again.");      
        up[1]="";
      }
    } while (up[1].isEmpty());
    
    return up;
  }
  public static int initAlgatorSystem(String username, String password) {
    ATGlobal.verboseLevel=Math.max(2, ATGlobal.verboseLevel);
    ATLog.log("Initializing the system ...",0);
      
    // create a database and its tables
    boolean databaseInit = Database.init();
    if (databaseInit) {    
      
      if (ATGlobal.verboseLevel > 2) 
        ATLog.log("Creating a new user ...", 0);
      
      if (username==null || username.isEmpty()|| password==null || password.isEmpty()) {
        String up[] = getUsernameAndPassword();
        if (up==null) {
          ATLog.log("Empty username or pasword are not alowed.", 0);
          return -1;
        }
        username=up[0];password=up[1];
      }      
      UsersDatabase.addNewUser(username, password);      
      
      ATLog.log("Done.",0);      
    }
    return 0;    
  }
  
  /**
   * Used to run the system. Parameters are given through the arguments
   *
   * @param args
   */
  public static void main(String args[]) {  
    System.out.println(introMsg + "\n");
    
    Options options = getOptions();

    CommandLineParser parser = new BasicParser();
    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("h")) {
	printMsg(options);
      }

      if (line.hasOption("use")) {
        printUsage();
        return;
      }
      
      
      String[] curArgs = line.getArgs();

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

      ATGlobal.verboseLevel = 0;
      if (line.hasOption("verbose")) {        
        ATGlobal.verboseLevel = Integer.parseInt(line.getOptionValue("verbose"));
      }
      
      ELocalConfig localConfig = ELocalConfig.getConfig();
      
      String username=localConfig.getUsername();
      if (line.hasOption("u")) {
	username = line.getOptionValue("u");
      }      
      String password=localConfig.getPassword();
      if (line.hasOption("p")) {
	password = line.getOptionValue("p");
      }            
      
     if (line.hasOption("init")) {
        initAlgatorSystem(username, password);
        return;
      }          
           
      
      if (line.hasOption("info") || line.hasOption("extinfo")) {
        String project   = (curArgs.length != 0) ? curArgs[0] : "";
        String algorithm = line.hasOption("algorithm") ? line.getOptionValue("algorithm") : "";
        
        String info = getInfo(project, algorithm, line.hasOption("extinfo"));
        System.out.println(info);
        
        return;
      }        
                  
      if (!Database.databaseAccessGranted(username, password)) return;
      
      if (line.hasOption("create_project")) {
	if (curArgs.length != 1) {
          System.out.println("Invalid project name");
          printMsg(options); 
        } else {
          System.out.println(createProject(username, curArgs[0]));
          return;
        }
      }
      
      if (line.hasOption("create_all")) {
	if (curArgs.length != 1) {
          System.out.println("Invalid project name");
          printMsg(options); 
        } else {
          System.out.println(createAll(username, curArgs[0]));
          return;
        }
      }

        
      if (line.hasOption("add_parameter")) {
	if (curArgs.length < 1) {
          System.out.println("Invalid number of parameters (required: project_name)");
          return;
        } else {
          if (curArgs.length == 1)
            System.out.println(addParameter(username, curArgs[0]));
          else 
            System.out.println(addParameter(username, curArgs[0], curArgs[1], curArgs.length > 2 ? curArgs[2] : ""));
          return;
        }
      }

      if (line.hasOption("add_indicator")) {
	if (curArgs.length < 1) {
          System.out.println("Invalid number of parameters (required: project_name)");
          return;
        } else {
          if (curArgs.length == 1)
            System.out.println(addIndicator(username, curArgs[0]));
          else 
            System.out.println(addIndicator(username, curArgs[0], curArgs[1], curArgs.length > 2 ? curArgs[2] : ""));
          return;
        }
      }
            
      if (line.hasOption("add_indicator_test")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid number of parameters (required: project_name indicator_name)");
          return;
        } else {
          System.out.println(addIndicatorTest(username, curArgs[0], curArgs[1]));
          return;
        }
      }

      if (line.hasOption("add_generator")) {
	if (curArgs.length != 3) {
          System.out.println("Invalid number of parameters (required: project_name type_name comma_separated_list_of_parameters)");
          return;
        } else {
          System.out.println(addTestCaseGenerator(username, curArgs[0], curArgs[1], curArgs[2].split(",")));
          return;
        }
      }
            
      if (line.hasOption("create_algorithm")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid project or algorithm name");
          printMsg(options); 
        } else {
          System.out.println(createAlgorithm(username, curArgs[0], curArgs[1]));
          return;
        }
      }

      if (line.hasOption("create_testset")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid project or test set name");
          printMsg(options); 
        } else {
          System.out.println(createTestset(username, curArgs[0], curArgs[1]));
          return;
        }
      }

      if (line.hasOption("create_presenter")) {
	if (curArgs.length < 1) {
          System.out.println("Invalid project or presenter name");
          printMsg(options); 
        } else {
          String presenterName = "";
          if (curArgs.length == 2)
            presenterName = curArgs[1];
          
          int type = 1;
          if (line.hasOption("pType"))
            try{type=Integer.parseInt(line.getOptionValue("pType"));} catch (Exception e) {}
          
          String result = createPresenter(username, curArgs[0], presenterName, type);
          if (result!=null) System.out.println(result);
          return;
        }
      }

      if (line.hasOption("remove_presenter")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid number of parameters (exactly two required)");
          printMsg(options); 
        } else {          
          System.out.println(removePresenter(curArgs[0], curArgs[1]));
          return;
        }
      }
      
      
      printMsg(options);
    } catch (ParseException ex) {
      printMsg(options);
    }
  }
  
}
