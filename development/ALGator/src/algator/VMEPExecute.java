package algator;

import jamvm.vmep.InstructionMonitor;
import jamvm.vmep.Opcode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.execute.AbstractTestCase;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Variables;
import si.fri.algator.entities.VariableType;
import si.fri.algator.entities.Project;
import si.fri.algator.execute.AbstractInput;
import si.fri.algator.execute.AbstractAlgorithm;
import si.fri.algator.execute.AbstractTestSetIterator;
import si.fri.algator.execute.DefaultTestSetIterator;
import si.fri.algator.execute.ExternalExecutor;
import si.fri.algator.execute.New;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.global.ExecutionStatus;
import si.fri.algator.global.VMEPErrorStatus;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.UniqueIDGenerator;

/**
 * VMEPExecute class is used to execute algorithm with vmep virtual machine. It 
 * executes <code>algorithm</code> on a <code>testset</code> and writes results
 * into <code>Algorithm-Testset.jvm</code> file in <code>commPath</code>. 
 * During the execution of tests, notificator writes bytes to communicaiton 
 * file (on byte for each test). This file can be used by invoker of VMEPExecute
 * to prevent halting.
 * 
 * Method main() executes algorithm and exites with exit code VMEPErrorStatus

 * @author tomaz
 */
public class VMEPExecute {

  private static String introMsg = "ALGator VMEP Executor, " + Version.getVersion();
  

  private static Options getOptions() {
    Options options = new Options();

    Option data_root = OptionBuilder.withArgName("folder")
	    .withLongOpt("data_root")
	    .hasArg(true)
	    .withDescription("use this folder as data_root; default value in $ALGATOR_DATA_ROOT" )
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

    Option logTarget = OptionBuilder.withArgName("log_target")
            .hasArg(true)
            .withDescription("where to print information (1 = stdout (default), 2 = file, 3 = both")
            .create("log");

    options.addOption(data_root);
    options.addOption(data_local);
    options.addOption(algator_root);
    options.addOption(verbose);
    options.addOption(logTarget);
    
    options.addOption("use", "usage",    false, "print usage guide");
    options.addOption("h", "help", false,
	    "print this message");    

    
    
    return options;
  }

  private static void printMsg(Options options) {
    System.out.println(introMsg + "\n");
    
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("algator.VMEPExecute [options] project_name algorithm_name testset_name test_number comm_folder", options);
  }

  private static void printUsage() {
    System.out.println(introMsg + "\n");
    
    Scanner sc = new Scanner((new Chart()).getClass().getResourceAsStream("/data/VMEPExecutorUsage.txt")); 
    while (sc.hasNextLine())
      System.out.println(sc.nextLine());
    
    System.exit(VMEPErrorStatus.OK.getValue());
  }
    
  public static void runTest(String dataRoot, String projName, String algName, 
          String testsetName, int testNumber, String commFolder, boolean asJSON) {

    ATLog.setPateFilename(ATGlobal.getTaskHistoryFilename(projName, algName, testsetName, "jvm"));
    
    // Test the project
    Project projekt = new Project(dataRoot, projName);
    if (!projekt.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: Invalid project name - " + projName, 1);
      System.exit(VMEPErrorStatus.INVALID_PROJECT.getValue()); // invalid project
    }

    // Test algorithms
    if (algName == null || algName.isEmpty()) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: Invalid algorithm name - " + algName, 1);
      System.exit(VMEPErrorStatus.INVALID_ALGORITHM.getValue());
    }
    EAlgorithm alg = projekt.getAlgorithms().get(algName);
    if (alg == null) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: Invalid algorithm name - " + algName, 1);
      System.exit(VMEPErrorStatus.INVALID_ALGORITHM.getValue()); // invalid algorithm
    }

    // Test testsets
    if (testsetName == null || testsetName.isEmpty()) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: Invalid testset name - " + testsetName, 1);
      System.exit(VMEPErrorStatus.INVALID_TESTSET.getValue());
    }
    
    ETestSet testSet = projekt.getTestSets().get(testsetName);
    if (testSet == null) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: Invalid testset name - " + testsetName, 1);

      System.exit(VMEPErrorStatus.INVALID_TESTSET.getValue()); // invalid testset
    }    
    
    URL[] urls = New.getClassPathsForProjectAlgorithm(projekt, algName);
    String currentJobID = New.generateClassloaderAndJobID(urls);    
            
    AbstractTestSetIterator testsetIterator = new DefaultTestSetIterator(projekt, testSet, currentJobID);
    testsetIterator.initIterator();
    
    EResult resultDescription = projekt.getResultDescriptions().get(MeasurementType.JVM);
    if (resultDescription == null) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: JVM result description file does not exist - " + projName + ", " + algName, 3);
      System.exit(VMEPErrorStatus.INVALID_RESULTDESCRIPTION.getValue()); // JVM result descritpion does not exist
    }
    
    // Test testNumber
    int allTests = testsetIterator.getNumberOfTestInstances();
    if (testNumber > allTests) {
      if (ATGlobal.verboseLevel > 0)
        ATLog.log("VMEP Execute: Invalid test number - " + projName + ", " + algName + " - " + testNumber, 3);

      System.exit(VMEPErrorStatus.INVALID_TEST.getValue()); // invalid testset   
    }
    
    String resFilename = ATGlobal.getJVMRESULTfilename(commFolder, algName, testsetName, testNumber);
    
    runAlgorithmOnATest(projekt, algName, testsetName, currentJobID, testNumber, resultDescription, testsetIterator, resFilename, asJSON);
    
    New.removeClassLoader(currentJobID);
  }

  

  /**
   * 
   */
  public static void runAlgorithmOnATest(
    Project project, String algName, String testsetName, String currentJobID, int testNumber, EResult resultDesc,
          AbstractTestSetIterator testsetIterator, String resFilename, boolean asJSON) {
                
    // the order of parameters to be printed
    String[] order = EResult.getVariableOrder(project.getTestCaseDescription(), resultDesc);
    String delim   = ATGlobal.DEFAULT_CSV_DELIMITER;

    Variables result = new Variables();
    result.addVariable(EResult.getAlgorithmNameParameter(algName), true);
    result.addVariable(EResult.getTestsetNameParameter(testsetName), true);
    result.addVariable(EResult.getInstanceIDParameter(UniqueIDGenerator.getNextID()), true); // if testCase won't initialize, a testcase ID is giver here 
    result.addVariable(EResult.getTimestampParameter(System.currentTimeMillis()), true);    
    result.addVariable(EResult.getExecutionStatusIndicator(ExecutionStatus.UNKNOWN), true);
    
    // An error that appears as a result of JVM error is not caught by the following catch; however, the finally block
    // is always executed. If in finally block success is false, then an JVM error has occured and must be reported
    boolean success = false;
    try {
      // delete the content of the output file
      new FileWriter(resFilename).close();

      if (testsetIterator.readTest(testNumber)) {        
        
        AbstractTestCase testCase = testsetIterator.getCurrent(); 
        String algClassName = project.getAlgorithms().get(algName).getAlgorithmClassname();
        AbstractAlgorithm curAlg = New.algorithmInstance(currentJobID, algClassName, MeasurementType.JVM);
        curAlg.init(testCase); 
        
        AbstractInput input = testCase.getInput();
                   
        if (ATGlobal.verboseLevel == 2) {           
          ATLog.log(String.format("Project: %s, Algorithm: %s, TestSet: %s, Test: %d\n", project.getName(), algName, testsetName, testNumber), 2);
          ATLog.log(String.format("********* Before execution       *********************************************"), 2);
          ATLog.log(input.toString(), 2);
        }
        
        InstructionMonitor instrMonitor = new InstructionMonitor();
        instrMonitor.start();                    
        curAlg.run();          
        instrMonitor.stop();

        result = curAlg.done();
        
        if (ATGlobal.verboseLevel == 2) {
          ATLog.log("********* After execution        *********************************************", 2);
          ATLog.log(input.toString(), 3);
        }

        if (ATGlobal.verboseLevel == 2) 
          ATLog.log("********* Bytecode commands used *********************************************", 2);
                
        // write results to the result set.
        Variables pSet = Variables.join(project.getTestCaseDescription().getParameters(), resultDesc.getVariables());
        int[] instFreq=instrMonitor.getCounts();
        String toLog = "";
        for(int i=0;i<instFreq.length;i++){
          String pName = Opcode.getNameFor(i);
          if (pSet.getVariable(pName) != null) {
            result.addVariable(new EVariable(pName, "", VariableType.INT, instFreq[i]), true);
          }
          if (ATGlobal.verboseLevel == 2 && instFreq[i]!=0)
            toLog += " " + pName;
        }  
        if (ATGlobal.verboseLevel == 2)
            ATLog.log(toLog, 2);
        
        
        result.addVariable(EResult.getExecutionStatusIndicator(ExecutionStatus.DONE), true);
        result.addVariable(EResult.getAlgorithmNameParameter(algName), true);
        result.addVariable(EResult.getTestsetNameParameter(testsetName), true);
        result.addVariable(input.getParameters().getVariable(EResult.instanceIDParName));
        result.addVariable(EResult.getTimestampParameter(System.currentTimeMillis()), true);
      } else {
        result.addVariable(EResult.getExecutionStatusIndicator(ExecutionStatus.FAILED), true);
        result.addVariable(EResult.getErrorIndicator("Invaldi testset or test."), true);
      }
      success = true;
      
    } catch (IOException e) {
      result.addVariable(EResult.getExecutionStatusIndicator(ExecutionStatus.FAILED), true);
      result.addVariable(EResult.getErrorIndicator(e.toString()), true);

      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_CANT_RUN, e.toString());

      if (ATGlobal.verboseLevel > 0)
        ATLog.log(e.toString(), 3);
      
    } finally {
      if (!success) {
         result.addVariable(EResult.getErrorIndicator("Unknown JVM error"), true);
      }
      try (PrintWriter pw = new PrintWriter(new FileWriter(resFilename, true))) {                  
          pw.println(result.toString(order, asJSON, delim));
      } catch (IOException e) {
        if (ATGlobal.verboseLevel > 0)
          ATLog.log(e.toString(), 3);
      }
    }
  }
  
  /**
   * Runs the algorithm using vmep virtual machine. If execution is succesfull, the 
   * created process is returned else the error message is returned as a String
   */
  public static Object runWithVMEP(String project_name, String alg_name, String testset_name,
          int testID, String commFolder, String data_root, String data_local) {    
    try {
      ///* For real-time execution (classPath=..../ALGator.jar)
      String classPath = Version.getClassesLocation();
      //*/
    
      //*   In debug mode (when running ALGator with NetBeans) getClassLocation() returns
         // a path to "classes" folder which is not enough to execute ALGator.
         // To run ALGator in debug mode, we add local ALGator distribution
      if (!classPath.contains("ALGator.jar"))
        classPath += File.pathSeparator +  "dist/ALGator.jar";
      //*/
      
      String jvmCommand = "java";
      String vmepCmd = ELocalConfig.getConfig().getField(ELocalConfig.ID_VMEP);
      String vmepCP  = ELocalConfig.getConfig().getField(ELocalConfig.ID_VMEPClasspath);

      // dodane 20. 6. 2017
      EProject eProjekt = new EProject(new File(ATGlobal.getPROJECTfilename(data_root, project_name)));
      String projRoot = ATGlobal.getPROJECTroot(data_root, project_name);
      String algJARs = ATTools.buildJARList(eProjekt.getStringArray(EProject.ID_AlgorithmJARs), ATGlobal.getPROJECTlib(projRoot));      
      
      if (!vmepCmd.isEmpty()) 
        jvmCommand = vmepCmd;
      if (!vmepCP.isEmpty())
          classPath += File.pathSeparator + vmepCP;
      
      if (!algJARs.isEmpty())
        classPath += File.pathSeparator + algJARs;

      String[] command = {jvmCommand, "-cp", classPath, "-Xss1024k", "algator.VMEPExecute", 
        project_name, alg_name, testset_name, Integer.toString(testID), commFolder, 
        "-dr", data_root, "-dl", data_local, "-v", Integer.toString(ATGlobal.verboseLevel), 
        "-log", Integer.toString(ATGlobal.logTarget)};
            
      ProcessBuilder probuilder = new ProcessBuilder( command );
    
      return probuilder.start();      
    } catch (Exception e) {
      return e.toString();
    }
  }
  
  /**
   * Used to run the system. Parameters are given trought the arguments
   *
   * @param args
   */
  public static void main(String args[]) {
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
      if (curArgs.length != 5) {
	printMsg(options);
        return;
      }

      String projectName   = curArgs[0];
      String algorithmName = curArgs[1];
      String testsetName   = curArgs[2];
      String testNumberS   = curArgs[3];
      String commFolder    = curArgs[4];

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
      
      int testNumber = 1; // the first test in testset is the default test to run
      try {
        testNumber = Integer.parseInt(testNumberS);
      } catch (Exception e) {}
      
      ATGlobal.verboseLevel = 1;
      if (line.hasOption("verbose")) {
        if (line.getOptionValue("verbose").equals("0"))
          ATGlobal.verboseLevel = 0;
        if (line.getOptionValue("verbose").equals("2"))
          ATGlobal.verboseLevel = 2;
      }
      
      ATGlobal.logTarget = ATLog.TARGET_STDOUT;
      if (line.hasOption("log")) {
        if (line.getOptionValue("log").equals("2"))
          ATGlobal.logTarget = ATLog.TARGET_FILE;
        if (line.getOptionValue("log").equals("3"))
          ATGlobal.logTarget = ATLog.TARGET_FILE + ATLog.TARGET_STDOUT;
      }     
      ATLog.setLogTarget(ATGlobal.logTarget);

      // tu se ne bom nič zmišljeval - vmep naj izhod v vsakem primeru 
      // zapiše kot json; če bi želel to spremeniti, dodaj opcijo output_format
      // (poglej, kako je bilo to narejeno pri Execute .java)
      boolean asJSON = true;
      
      
      // Notify to the caller (message: JVM has started) 
      ExternalExecutor.initCommunicationFile(commFolder);
      ExternalExecutor.addToCommunicationFile(commFolder);
      
      
      runTest(dataRoot, projectName, algorithmName, testsetName, testNumber, commFolder, asJSON);

    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }


}
