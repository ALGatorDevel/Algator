package algator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONObject;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.EResult;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.entities.VariableType;
import si.fri.algotest.entities.Variables;
import si.fri.algotest.execute.AbstractTestCase;
import si.fri.algotest.execute.Executor;
import si.fri.algotest.execute.ExternalExecutor;
import si.fri.algotest.execute.New;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.global.ExecutionStatus;
import si.fri.algotest.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class Analyse {
  private static String introMsg = "ALGator Analyse, " + Version.getVersion();
  
  static final String GENERATED_TestSetName = "[generated]";
  static final String MY_TIMER              = "_Tmin_";
      
  // when the relative difference between lastOK and lastKILLED value of parameter  
  // is <= precisionLevel, algorithm getParameterLimit will stop
  static final double precisionLevel        = 0.01;
  
  
  private static Options getOptions() {
    Options options = new Options();

    Option algorithm = OptionBuilder.withArgName("algorithm_name")
	    .withLongOpt("algorithm")
	    .hasArg(true)
	    .withDescription("the name of the algorithm to use; if the algorithm is not given, all the algorithms of a given project are used")
	    .create("a");
    
    Option data_root = OptionBuilder.withArgName("folder")
	    .withLongOpt("data_root")
	    .hasArg(true)
	    .withDescription("use this folder as data_root; default:  $ALGATOR_ROOT/data_root")
	    .create("dr");

    Option data_local = OptionBuilder.withArgName("folder")
            .withLongOpt("data_local")
            .hasArg(true)
            .withDescription("use this folder as data_local; default: $ALGATOR_ROOT/data_local")
            .create("dl");    
    
    Option algator_root = OptionBuilder.withArgName("folder")
            .withLongOpt("algator_root")
            .hasArg(true)
            .withDescription("use this folder as algator_root; default: $ALGATOR_ROOT")
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
    
    Option param = OptionBuilder.withArgName("parameter_name")
    	    .withLongOpt("parameter")
            .hasArg(true)
            .withDescription("the name of the parameter to use")
            .create("p");    
    Option params = OptionBuilder.withArgName("parameters")
            .withLongOpt("parameters")
            .hasArg(true)
            .withDescription("the value of parameters (json string)")
            .create("s");
    Option timeLimit = OptionBuilder.withArgName("time_limit")
            .withLongOpt("timelimit")
            .hasArg(true)
            .withDescription("time limit before kill (in seconds); defulat: 1")
            .create("t");
    Option timesToExecute = OptionBuilder.withArgName("time_to_execute")
            .withLongOpt("timestoexecute")
            .hasArg(true)
            .withDescription("number of times to execute algorithm; defulat: 1")
            .create("te");

    
    
    options.addOption(algorithm);
    options.addOption(data_root);
    options.addOption(data_local);    
    options.addOption(algator_root);
    options.addOption(param);
    options.addOption(params);
    options.addOption(timeLimit);
    options.addOption(timesToExecute);
    
        
    options.addOption(verbose);
    options.addOption(logTarget);
    
    options.addOption("h", "help", false,
	    "print this message");        
    options.addOption("u", "usage", false, "print usage guide");
    
    return options;
  }

  private static void printMsg(Options options) {    
    HelpFormatter formatter = new HelpFormatter();
    String header = "function: FindLimit | RunOne\n";
    formatter.printHelp("algator.Analyse [options] function project_name", header,  options, "");

    System.exit(0);
  }

  private static void printUsage() {
    Scanner sc = new Scanner((new Chart()).getClass().getResourceAsStream("/data/AnalyseUsage.txt")); 
    while (sc.hasNextLine())
      System.out.println(sc.nextLine());
    
    System.exit(0);
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
      }

      if (line.hasOption("u")) {
        printUsage();
      }

      String[] curArgs = line.getArgs();
      if (curArgs.length != 2) {
	printMsg(options);
      }

      String function    = curArgs[0]; 
      String projectName = curArgs[1];
      
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

      String algorithmName = "";
      if (line.hasOption("algorithm")) {
	algorithmName = line.getOptionValue("algorithm");
      }
      
      ATGlobal.verboseLevel = 0;
      if (line.hasOption("verbose")) {
        if (line.getOptionValue("verbose").equals("1"))
          ATGlobal.verboseLevel = 1;
        if (line.getOptionValue("verbose").equals("2"))
          ATGlobal.verboseLevel = 2;
      }
      
      ATGlobal.logTarget = ATLog.LOG_TARGET_STDOUT;
      if (line.hasOption("log")) {
        if (line.getOptionValue("log").equals("0"))
          ATGlobal.logTarget = ATLog.LOG_TARGET_OFF;
        if (line.getOptionValue("log").equals("2"))
          ATGlobal.logTarget = ATLog.LOG_TARGET_FILE;
        if (line.getOptionValue("log").equals("3"))
          ATGlobal.logTarget = ATLog.LOG_TARGET_FILE + ATLog.LOG_TARGET_STDOUT;
      }     
      ATLog.setLogTarget(ATGlobal.logTarget);

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
      
      
      String parameterName = "";
      if (line.hasOption("parameter")) {
	parameterName = line.getOptionValue("parameter");
      }
      
      String paramsJSON = "{}";
      if (line.hasOption("parameters")) {
	paramsJSON = line.getOptionValue("parameters");
      }
      Variables parameters = getParametersFromJSON(paramsJSON);
      
      // valid project?
      if (!ATGlobal.projectExists(dataRoot, projectName)) {
        ATGlobal.verboseLevel=1;
        ATLog.log("Project configuration file does not exist for " + projectName, 1);

        System.exit(0);      
      }
      
      Execute.syncTests(projectName);
      Project project = new Project(dataRoot, projectName);

      ArrayList<String> algorithms = new ArrayList<>();
      if (algorithmName.isEmpty()) {
        try {
          for (String eAlgName : project.getAlgorithms().keySet()) 
            algorithms.add(eAlgName);
        } catch (Exception e) {}        
      } else
        algorithms.add(algorithmName);
      
      switch (function.toUpperCase()) {
        case "RUNONE":
          runOne(dataRoot, project, algorithms, parameters, timeLimit, timesToExecute);
          break;
        
        case "FINDLIMIT":
          if (parameterName.isEmpty()) {
            ATGlobal.verboseLevel=1;
            ATLog.log("Missing parameter (option -p).", 1);

           System.exit(0);       
          }
          
          getParameterLimit(dataRoot, project, algorithms, parameterName, parameters, timeLimit);
          break;
          
        default:
          ATGlobal.verboseLevel=1;
          ATLog.log("Invalid function '" + projectName + "'.", 1);
          System.exit(0);                
      }           
    } catch (ParseException ex) {
      printMsg(options);
    }
  }
  
  private static Variables getParametersFromJSON(String paramsJSON) {
    HashMap<String, Object> params = new HashMap<>();
    try {
      params = ATTools.jSONObjectToMap(new JSONObject(paramsJSON));
    } catch (Exception e) {}
    Variables parameters = new Variables();
    for (String paramName: params.keySet()) {
      parameters.addVariable(new EVariable(paramName, params.get(paramName)));
    }
    return parameters;
  }

  private static void runOne(String data_root, Project project, ArrayList<String> algorithms, Variables defaultParams, int timeLimit, int timesToExecute) {          
    Executor.projectMakeCompile(data_root, project.getName(), false);

    for (String algName : algorithms) {
      Executor.algorithmMakeCompile(data_root, project.getName(), algName, MeasurementType.EM, false);
      runOne(data_root, project, algName, defaultParams, timeLimit, timesToExecute);
    }    
  }
  private static void runOne(String data_root, Project project, String algName, Variables defaultParams, int timeLimit, int timesToExecute) {  
    
    EAlgorithm eAlgorithm = project.getAlgorithms().get(algName);
    if (eAlgorithm == null) {
        ATGlobal.verboseLevel=1;
        ATLog.log(String.format("Algorithm '%s' does not exist.",algName), 1);

        System.exit(0);      
    }
    
    int testID = 0;
    
    EResult emResultDesc = project.getResultDescriptions().get(MeasurementType.EM);
    if (emResultDesc == null) emResultDesc = new EResult();
    Variables parameters = emResultDesc.getParameters();
    
    // apply the given values of the parameters
    for (EVariable defParam : defaultParams) {
      EVariable param = parameters.getVariable(defParam.getName());
      if (param != null) param.setValue(defParam.getValue());
    } 
    
    
 
    AbstractTestCase testCase = New.testCaseInstance(project).generateTestCase(parameters);
    Variables result = 
      ExternalExecutor.runTestCase(
          project, algName, testCase, MeasurementType.EM, GENERATED_TestSetName, ++testID, timesToExecute, timeLimit, null);
    
    System.out.println(
      result.toString(emResultDesc.getVariableOrder(), false, ATGlobal.DEFAULT_CSV_DELIMITER)
    );        
  }
  
  
  private static void getParameterLimit(String data_root, Project project, ArrayList<String> algorithms, String paramName, Variables parameters, int timeLimit) {            
    Executor.projectMakeCompile(data_root, project.getName(), false);    
    
    HashMap<String, Variables>  results = new HashMap<>();

    for (String algName : algorithms) {
      Executor.algorithmMakeCompile(data_root, project.getName(), algName, MeasurementType.EM, false);
      results.put(algName, getParameterLimit(project, algName, paramName, parameters, timeLimit));
    }    
    
    EResult emResultDesc = project.getResultDescriptions().get(MeasurementType.EM);
    
    for (String algName : algorithms) {
      Variables result = results.get(algName);
      if (result != null) {
        System.out.print(
          result.toString(emResultDesc.getVariableOrder(), false, ATGlobal.DEFAULT_CSV_DELIMITER)
        );        
        System.out.println(ATGlobal.DEFAULT_CSV_DELIMITER + result.getVariable(MY_TIMER).getLongValue());
      } else {
        System.out.println(algName + ATGlobal.DEFAULT_CSV_DELIMITER + "?");
      }
    }
  }

  /**
   * Method runs algorihm for several times and try to find the value of the parameter paramName
   * for which the execution of the test case generated with parameters+paramName lasts about 
   * timeLimit seconds. 
   */
  private static Variables getParameterLimit(Project project, String algName, String paramName, Variables parameters, int timeLimit) {    
    // to control the execution time we need one additional indicator - time of execution (_time_)
    EResult resultDesc = project.getResultDescriptions().get(MeasurementType.EM); 
    EVariable timer    = new EVariable(MY_TIMER, VariableType.TIMER, 0); timer.setMeta("{\"ID\":0, \"STAT\":\"MIN\"}");
    resultDesc.additionalIndicators.addVariable(timer);                   

    Variables lastOKResult   = null;
    
    int lastOKParamValue     = 5;  // tu bi bilo treba vzeti privzeto min vrednost za parameter
    int lastKilledParamValue = 10; // privzeta min vrednost*2
    int curParamValue        = 10; // privzeta min vrednost*2
    
    long lastOKTime          = 0;
        
    EVariable param = parameters.getVariable(paramName);
    if (param == null) {
      param = new EVariable(paramName, lastOKParamValue);
      parameters.addVariable(param);
    }
        
    int testID = 0;
    
    boolean bisectionMode = false;
    
    while (true) {       
      param.setValue(curParamValue);
      AbstractTestCase testCase = New.testCaseInstance(project).generateTestCase(parameters);

      Variables result = 
        ExternalExecutor.runTestCase(project, algName, testCase, MeasurementType.EM, GENERATED_TestSetName, ++testID, 1, 2*timeLimit, null);

      long time = result.getVariable(MY_TIMER).getLongValue(2*timeLimit*1000000);
      
      String status  = (String) result.getVariable(EResult.passParName).getValue();
      if (time > 1000000 * timeLimit) status =  ExecutionStatus.KILLED.toString();

      boolean killed = status.equals(ExecutionStatus.KILLED.toString()) || time > timeLimit*1000000;
            
      if (ATGlobal.verboseLevel == 2)
        System.out.println(String.format("%s=%9d, T=%9d, status=%s", paramName, curParamValue, time, status));
      
      if (killed) bisectionMode = true;
      
      if (bisectionMode) {
        if (killed) 
          lastKilledParamValue = curParamValue;
        else {
          lastOKParamValue     = curParamValue;
          lastOKTime           = time;

          lastOKResult         = result.copy();
        }
        
        curParamValue = (lastOKParamValue + lastKilledParamValue) / 2;
        
      } else {
        lastOKTime       = time;
        lastOKParamValue = curParamValue;
        curParamValue    = 2*curParamValue;
        
        lastOKResult         = result.copy();
      }
      
      
      if (1.0*Math.abs(curParamValue - lastOKParamValue) / lastOKParamValue < precisionLevel) break;      
    }
    if (ATGlobal.verboseLevel == 2)
      System.out.printf("Alg: %s, Param value: %d, time: %d\n", algName, lastOKParamValue, lastOKTime);
    
    return lastOKResult;
  }  
}
