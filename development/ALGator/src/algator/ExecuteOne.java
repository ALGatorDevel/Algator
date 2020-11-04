package algator;

import java.io.File;
import java.util.Scanner;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.EResult;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.entities.Variables;
import si.fri.algotest.execute.AbstractTestCase;
import si.fri.algotest.execute.ExternalExecutor;
import si.fri.algotest.global.ATGlobal;

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

    options.addOption(data_root);
    options.addOption(data_local);    
    options.addOption(algator_root);
    options.addOption(verbose);
    
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
    int testID, verboseLevel;
    
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
      testID    = Integer.parseInt(curArgs[4]);
      
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
      int noInst = eTestSet.getFieldAsInt(ETestSet.ID_N, 0);            
      if (testID >= noInst) {
        System.out.println("Invalid testID (test does not exist).");
        return;
      }
      
      runAlgorithm(project, algName, eTestSet, mType, testID, verboseLevel);    
      
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }
  
  // Runs one test
  private static void runAlgorithm(Project project, String algName, ETestSet eTestSet, 
          MeasurementType mType, int testID, int verboseLevel) {
    
    String qTestSetFIleName = ATGlobal.getTESTSETfilename(ATGlobal.getALGatorDataLocal(), project.getName(), "QTestSet");
    eTestSet.set(ETestSet.ID_N, 1);           // only one test
    eTestSet.set(ETestSet.ID_TimeLimit,  0);  // no time limit  
    eTestSet.set(ETestSet.ID_TestRepeat, 1);  // repeate only once
    
    String descFileName = ATGlobal.getTESTSroot(ATGlobal.getALGatorDataLocal(), project.getName()) +
             File.separator + eTestSet.getTestSetDescriptionFile();

    String test = "?";
    try (Scanner sc = new Scanner(new File(descFileName))) {
      for(int i=0; i<testID; i++) sc.nextLine();
      test = sc.nextLine();
    } catch (Exception e) {}
    
    System.out.println(qTestSetFIleName);
    System.out.println(test);
    
    AbstractTestCase testCase=null; //!!!
    
    Variables resultVariables = ExternalExecutor.runTestCase(project, algName, testCase, mType, eTestSet.getName(), testID, 1, 100, null, null);
    //!!!!!!!
    // ExternalExecutor.printVariables(resultVariables, resultFile, EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc), whereToPrint);

    
    
  }

}
