package algator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import si.fri.algotest.analysis.Analysis;
import si.fri.algotest.analysis.DataAnalyser;
import si.fri.algotest.analysis.TableData;
import si.fri.algotest.database.Database;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.ELocalConfig;
import si.fri.algotest.entities.EQuery;
import si.fri.algotest.entities.EResult;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.execute.Executor;
import si.fri.algotest.execute.Notificator;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.tools.ATTools;
import si.fri.algotest.global.ErrorStatus;
import static si.fri.algotest.tools.ATTools.getTaskResultFileName;
import si.fri.algotest.tools.RSync;

/**
 *
 * @author tomaz
 */
public class Query {

  private static String introMsg = "ALGator Query, " + Version.getVersion();
  

  private static Options getOptions() {
    Options options = new Options();

    Option algorithm = OptionBuilder.withArgName("algorithm_name")
	    .withLongOpt("algorithm")
	    .hasArg(true)
	    .withDescription("the name of the algorithm to run; if the algorithm is not given, all the algorithms of a given project are run")
	    .create("a");

    Option testset = OptionBuilder.withArgName("testset_name")
	    .withLongOpt("testset")
	    .hasArg(true)
	    .withDescription("the name of the testset to use; if the testset is not given, all the testsets of a given project are used")
	    .create("t");

    Option measurement = OptionBuilder.withArgName("mtype_name")
	    .withLongOpt("mtype")
	    .hasArg(true)
	    .withDescription("the name of the measurement type to use (EM, CNT or JVM); if the measurement type is not given, the EM measurement type is used")
	    .create("m");
    

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
    
    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF (default), 1 = some, 2 = all")
            .create("v");
    
    Option logTarget = OptionBuilder.withArgName("log_target")
            .hasArg(true)
            .withDescription("where to print information (1 = stdout (default), 2 = file, 3 = both")
            .create("log");
    

    Option whereResults = OptionBuilder.withArgName("where_results")
            .hasArg(true)
            .withDescription("where to print results (1 = stdout, 2 = file (default), 3 = both (default)")
            .create("w");
    
    Option username = OptionBuilder.withArgName("username")	    
	    .hasArg(true)
	    .withDescription("the name of the current user")
	    .create("u");

    Option password = OptionBuilder.withArgName("password")	    
	    .hasArg(true)
	    .withDescription("the password of the current user")
	    .create("p");    
    
    options.addOption(algorithm);
    options.addOption(testset);
    options.addOption(data_root);
    options.addOption(data_local);    
    options.addOption(algator_root);
    options.addOption(measurement);
    
    options.addOption(username);
    options.addOption(password);
        
    options.addOption(verbose);
    options.addOption(logTarget);
    options.addOption(whereResults);
    
    options.addOption("h", "help", false,
	    "print this message");
        
    options.addOption("use", "usage", false, "print usage guide");
    
    return options;
  }

  private static void printMsg(Options options) {    
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("algator.Query [options] project_name", options);
  }

  private static void printUsage() {
    Scanner sc = new Scanner((new Chart()).getClass().getResourceAsStream("/data/QueryUsage.txt")); 
    while (sc.hasNextLine())
      System.out.println(sc.nextLine());   
    sc.close();
  }
  
  /**
   * Used to run the system. Parameters are given trought the arguments
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
        return;
      }

      if (line.hasOption("use")) {
        printUsage();
        return;
      }
      
      String[] curArgs = line.getArgs();
      if (curArgs.length != 1) {
	printMsg(options);
        return;
      }

      String projectName = curArgs[0];

      String algorithmName = "";
      String testsetName = "";
            
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

      if (line.hasOption("algorithm")) {
	algorithmName = line.getOptionValue("algorithm");
      }
      if (line.hasOption("testset")) {
	testsetName = line.getOptionValue("testset");
      }
      
      MeasurementType mType = MeasurementType.EM;
      if (line.hasOption("mtype")) {
	try {
          mType = MeasurementType.valueOf(line.getOptionValue("mtype").toUpperCase());
        } catch (Exception e) {}  
      }

      ATGlobal.verboseLevel = 0;
      if (line.hasOption("verbose")) {
        if (line.getOptionValue("verbose").equals("1"))
          ATGlobal.verboseLevel = 1;
        if (line.getOptionValue("verbose").equals("2"))
          ATGlobal.verboseLevel = 2;
      }
      
      ATGlobal.logTarget = ATLog.TARGET_STDOUT;
      if (line.hasOption("log")) {
        if (line.getOptionValue("log").equals("0"))
          ATGlobal.logTarget = ATLog.TARGET_OFF;
        if (line.getOptionValue("log").equals("2"))
          ATGlobal.logTarget = ATLog.TARGET_FILE;
        if (line.getOptionValue("log").equals("3"))
          ATGlobal.logTarget = ATLog.TARGET_FILE + ATLog.TARGET_STDOUT;
      }     
      ATLog.setLogTarget(ATGlobal.logTarget);      
                            
      int whereToPrint = 3; // both, stdout and file
      if (line.hasOption("where_results")) try {
        whereToPrint = Integer.parseInt(line.getOptionValue("where_results"));
      } catch (Exception e) {}                     
      
      ELocalConfig localConfig = ELocalConfig.getConfig();
      
      String username=localConfig.getField(ELocalConfig.ID_Username);
      if (line.hasOption("u")) {
	username = line.getOptionValue("u");
      }      
      String password=localConfig.getField(ELocalConfig.ID_Password);
      if (line.hasOption("p")) {
	password = line.getOptionValue("p");
      }           

      if (!Database.databaseAccessGranted(username, password)) return;
      
      
      
      runQuery(dataRoot, projectName, algorithmName, testsetName, mType, whereToPrint);
 
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }


  
  private static void runQuery(String dataRoot, String projName, String algName,
	  String testsetName, MeasurementType mType, int whereToPrint) {
    
    if (!ATGlobal.projectExists(dataRoot, projName)) {
      ATGlobal.verboseLevel=1;
      ATLog.log("Project configuration file does not exist for " + projName, 1);

      return;      
    }
    

    // Test the project
    Project projekt = new Project(dataRoot, projName);
    if (!projekt.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) {
      ATGlobal.verboseLevel=1;
      ATLog.log("Invalid project: " + projekt.getErrors().get(0).toString(), 1);

      return;
    }
            
    // Test algorithms
    ArrayList<EAlgorithm> eAlgs;
    if (!algName.isEmpty()) {
      EAlgorithm alg = projekt.getAlgorithms().get(algName);
      if (alg == null) {
        ATGlobal.verboseLevel=1;
	ATLog.log("Invalid algorithm - " + algName, 1);
	return;
      }
      eAlgs = new ArrayList(); 
      eAlgs.add(alg);
    } else {
       eAlgs = new ArrayList(projekt.getAlgorithms().values());
    }
    
    ETestSet otherTst = new ETestSet();
    otherTst.setName(Analysis.OtherTestsetName);
    
    // Test testsets
    ArrayList<ETestSet> eTests;
    if (!testsetName.isEmpty()) {
      ETestSet test = (testsetName.equals(Analysis.OtherTestsetName)) ? otherTst : projekt.getTestSets().get(testsetName);
      if (test == null) {
        ATGlobal.verboseLevel=1;
	ATLog.log("Invalid testset - " + testsetName, 1);
	return;
      }
      eTests = new ArrayList<>(); 
      eTests.add(test);
    } else {
       eTests = new ArrayList(projekt.getTestSets().values());
       eTests.add(otherTst);
    }
            
    String algs = "";
    for (int i = 0; i < eAlgs.size(); i++) 
      algs += (algs.isEmpty() ? "": ",") + String.format("\"%s\"", eAlgs.get(i).getName());
    algs = "[" + algs + "]";
    
    String tsts = "";
    for (int i = 0; i < eTests.size(); i++) 
      tsts += (tsts.isEmpty() ? "": ",") + String.format("\"%s\"", eTests.get(i).getName());
    tsts = "[" + tsts + "]";

    
    String params = "[\"*\"]";
    String indicators = "[\"*" + mType.getExtension().toUpperCase() + "\"]";
    
    String query = String.format(
      "{'Algorithms': %s, 'TestSets': %s, 'Parameters': %s, 'Indicators': %s, 'GroupBy': [], 'Filter': [], 'SortBy': [], 'ComputerID' : '','Count': '0'}",
        algs, tsts, params, indicators);
    
    EQuery eq = new EQuery(query, null);
    TableData td = DataAnalyser.runQuery(projekt.getEProject(), eq, "");
        
    System.out.println(td.toString());            
  }
}
