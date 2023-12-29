package algator;

import java.io.File;
import java.net.URL;
import java.util.Scanner;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import si.fri.algator.execute.AbstractTestCase;
import si.fri.algator.execute.ExternalExecutor;
import si.fri.algator.execute.New;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;

/**
 *
 * @author tomaz
 */
public class ExecuteOne {

  private static String introMsg = "ALGator ExecuteOne, " + Version.getVersion();
  

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
    
    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF, 1 = some (default), 2 = all")
            .create("v");
    
    Option timeLimit = OptionBuilder.withArgName("time_limit")
            .withLongOpt("timelimit")
            .hasArg(true)
            .withDescription("time limit before kill (in seconds); defalut: 10")
            .create("t");
    Option timesToExecute = OptionBuilder.withArgName("time_to_execute")
            .withLongOpt("timestoexecute")
            .hasArg(true)
            .withDescription("number of times to execute algorithm; defulat: 1")
            .create("te");    
    
    Option outputFormat = OptionBuilder.withLongOpt("output_format")
            .withArgName("format")	    
	    .hasArg(true)
	    .withDescription("the format of the output (json (default) or csv)")
	    .create("ofmt");     

    options.addOption(data_root);
    options.addOption(data_local);    
    options.addOption(algator_root);
    options.addOption(verbose);
    options.addOption(timeLimit);
    options.addOption(timesToExecute);
    options.addOption(outputFormat);
    
    options.addOption("h", "help", false, "print this help");
    
    return options;
  }

  private static void printMsg(Options options) {
    System.out.println(introMsg + "\n");
    
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("algator.ExecuteOne [options] project_name algorithm_name testset_name m_type test_id", options);

    return;
  }


  public static void main(String args[]) {    
    Options options = getOptions();
    CommandLineParser parser = new BasicParser();

    String projName,algName,tstName;
    MeasurementType  mType;
    String testID;
    int verboseLevel;
    
    String algatorRoot, dataRoot;
    
    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("h")) {
	printMsg(options);
        return;
      }            
      
      String[] curArgs = line.getArgs();
      if (curArgs.length != 5) {
        printMsg(options);
        return;
      } 
      
      projName  = curArgs[0];
      algName   = curArgs[1];
      tstName   = curArgs[2];
      
      mType     = curArgs[3].equals("cnt") ? MeasurementType.CNT : (curArgs[3].equals("jvm") ? MeasurementType.JVM : MeasurementType.EM);
      testID    = curArgs[4];
      
      verboseLevel = 1;
      if (line.hasOption("verbose")) {
        if (line.getOptionValue("verbose").equals("0"))
          verboseLevel = 0;
        if (line.getOptionValue("verbose").equals("2"))
          verboseLevel = 2;
      }
      ATGlobal.verboseLevel = verboseLevel;
      
      algatorRoot = ATGlobal.getALGatorRoot();
      if (line.hasOption("algator_root")) {
        algatorRoot = line.getOptionValue("algator_root");        
      }
      ATGlobal.setALGatorRoot(algatorRoot);

      dataRoot = ATGlobal.getALGatorDataRoot();
      if (line.hasOption("data_root")) {
	dataRoot = line.getOptionValue("data_root");
      }
      ATGlobal.setALGatorDataRoot(dataRoot);

      String dataLocal = ATGlobal.getALGatorDataLocal();
      if (line.hasOption("data_local")) {
	dataLocal = line.getOptionValue("data_local");
      }
      ATGlobal.setALGatorDataLocal(dataLocal);     
      
      int timeLimit = 1;
      if (line.hasOption("timelimit")) {
        try {
	  timeLimit = Integer.parseInt(line.getOptionValue("timelimit", "1"));
        } catch (Exception e) {}
      }
      int timesToExecute = 1;
      if (line.hasOption("timestoexecute")) {
        try {
	  timesToExecute = Integer.parseInt(line.getOptionValue("timestoexecute", "1"));
        } catch (Exception e) {}
      }      
           
      // Create and test the project
      Project project = new Project(dataRoot, projName);
      if (project == null || !project.getErrors().get(0).isOK()) {
        System.out.println("Invalid project.");
        return;
      }
      
      // Create and test the testset
      EAlgorithm eAlgorithm = project.getAlgorithms().get(algName);
      if (eAlgorithm == null) {
        System.out.println("Invalid algorithm.");
        return;
      }
      
      // Create and test the testset
      ETestSet eTestSet = project.getTestSets().get(tstName);
      if (eTestSet == null) {
        System.out.println("Invalid testset.");
        return;
      }

      // Test the testID
      if (testID.isEmpty()) {
        System.out.println("Invalid testID (test does not exist).");
        return;
      }
      
      // dva možna izpisa: csv ali json
      boolean asJSON = true;
      if (line.hasOption("output_format")) {
        asJSON = !"csv".equals(line.getOptionValue("output_format"));
      }
      
      runAlgorithm(project, algName, eTestSet, mType, testID, verboseLevel, timeLimit, timesToExecute, asJSON);    
      
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }
  
  // Runs one test
  private static void runAlgorithm(Project project, String algName, ETestSet eTestSet, 
          MeasurementType mType, String testID, int verboseLevel, int limit, int timesToExecute, boolean asJSON) {
    
    String qTestSetFileName = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataLocal(), project.getName(), "QTestSet");
    eTestSet.set(ETestSet.ID_N, 1);                        // only one test
    eTestSet.set(ETestSet.ID_TimeLimit,  limit);           // no time limit  
    eTestSet.set(ETestSet.ID_TestRepeat, timesToExecute);  // numbers of repetition
    
    String descFileName = ATGlobal.getTESTSroot(ATGlobal.getALGatorDataLocal(), project.getName()) +
             File.separator + eTestSet.getTestSetDescriptionFile();
    String path = new File(descFileName).getParent();
    
    String test = "?";
    try (Scanner sc = new Scanner(new File(descFileName))) {
      while(sc.hasNextLine()) {      
        String trTestId="";
        String line = sc.nextLine();       
        try {trTestId = line.split(":")[1];} catch (Exception e){}
        if (trTestId.equals(testID)) {
          test = line;
          break;
        }
      }
    } catch (Exception e) {}
    if (test.equals("?")) {
        System.out.println("Invalid testID (test does not exist).");
        return;
    }
    
    
    URL[] urls = New.getClassPathsForProjectAlgorithm(project, algName);
    String currentJobID = New.generateClassloaderAndJobID(urls);    
    String testCaseClassName = project.getEProject().getTestCaseClassname();
    
    AbstractTestCase testCase = New.testCaseInstance(currentJobID, testCaseClassName).getTestCase(project, test, path);
    
    Variables resultVariables = ExternalExecutor.runTestCase(project, algName, testCase, currentJobID, mType, eTestSet.getName(), 1, timesToExecute, 100, null, null);
    
    EResult resultDesc = project.getResultDescriptions().get(mType);
    if (resultDesc == null) {
      resultDesc = new EResult();
    }
    
    ExternalExecutor.printVariables(resultVariables, null, EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc), ATLog.TARGET_STDOUT, asJSON);    
    
    New.removeClassLoader(currentJobID); 
  }

}
