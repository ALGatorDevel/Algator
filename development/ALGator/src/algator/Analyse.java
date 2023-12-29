package algator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONObject;
import si.fri.algator.analysis.Analysis;
import si.fri.algator.analysis.DataAnalyser;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import si.fri.algator.execute.Notificator;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.tools.ATTools;
import si.fri.algator.tools.UniqueIDGenerator;
import si.fri.algator.analysis.timecomplexity.Data;
import si.fri.algator.analysis.timecomplexity.GA;
import si.fri.algator.analysis.timecomplexity.OutlierDetector;
import si.fri.algator.analysis.timecomplexity.PowerLawFunction;

/**
 *
 * @author tomaz
 */
public class Analyse {
  private static String introMsg = "ALGator Analyse, " + Version.getVersion();
          
  
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
    
    Option whereResults = OptionBuilder.withArgName("where_results")
            .withLongOpt("where_results")            
            .hasArg(true)
            .withDescription("where to print results (1 = stdout, 2 = file, 3 = both (default)")
            .create("w");
    
    
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
    Option measurement = OptionBuilder.withArgName("mtype_name")
	    .withLongOpt("mtype")
	    .hasArg(true)
	    .withDescription("the name of the measurement type to use (EM, CNT or JVM); if the measurement type is not given, the EM measurement type is used")
	    .create("m");

    Option instanceID = OptionBuilder.withArgName("instance_id")
	    .withLongOpt("instance_id")
	    .hasArg(true)
	    .withDescription("identifier of this test instance; default: unique random value")
	    .create("i");
    
    Option outputFormat = OptionBuilder.withLongOpt("output_format")
            .withArgName("format")	    
            .hasArg(true)
            .withDescription("the format of the output (json (default) or csv)")
            .create("ofmt"); 
    
    options.addOption(algorithm);
    options.addOption(data_root);
    options.addOption(data_local);    
    options.addOption(algator_root);
    options.addOption(param);
    options.addOption(params);
    options.addOption(timeLimit);
    options.addOption(timesToExecute);
    options.addOption(measurement);
    options.addOption(instanceID);
    options.addOption(outputFormat);
    
        
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
    String header = "functions: FindLimit | FindLimits | RunOne | TimeComplexity \n";
    formatter.printHelp("algator.Analyse [options] function project_name", header,  options, "");
  }

  private static void printUsage() {
    Scanner sc = new Scanner((new Chart()).getClass().getResourceAsStream("/data/AnalyseUsage.txt")); 
    while (sc.hasNextLine())
      System.out.println(sc.nextLine());    
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
      if (curArgs.length != 2) {
	printMsg(options);
        return;
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

      String instanceID = UniqueIDGenerator.getNextID();
      if (line.hasOption("instance_id"))
        instanceID = line.getOptionValue("instance_id");
        
      int whereToPrint = 3; // both, stdout and file
      if (line.hasOption("where_results")) try {
        whereToPrint = Integer.parseInt(line.getOptionValue("where_results"));
      } catch (Exception e) {}            
      
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
      
      // dva možna izpisa: csv ali json
      boolean asJSON = true;
      if (line.hasOption("output_format")) {
        asJSON = !"csv".equals(line.getOptionValue("output_format"));
      }
      
      // valid project?
      if (!ATGlobal.projectExists(dataRoot, projectName)) {
        ATGlobal.verboseLevel=1;
        ATLog.log("Project configuration file does not exist for " + projectName, 1);

        return;      
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
      
      //Notificator notificator = Notificator.getNotificator(projectName, "", FindLimitTestsetID, MeasurementType.EM);
      Notificator notificator = null;
      
      switch (function.toUpperCase()) {
        case "RUNONE":
          Analysis.runOne(dataRoot, project, algorithms, parameters, timeLimit, timesToExecute, mType, instanceID, whereToPrint, asJSON);
          break;
        
        case "FINDLIMIT":
          //TODO remove debug mode
          //si.fri.algotest.global.ATGlobal.debugMode = true;
          ATGlobal.verboseLevel = 2;
          
          if (parameterName.isEmpty()) {
            ATGlobal.verboseLevel=1;
            ATLog.log("Missing parameter (option -p).", 1);

            return;
          }
          ArrayList<Variables> results = 
            Analysis.getParameterLimit(dataRoot, project, algorithms, parameterName, parameters, timeLimit, instanceID, whereToPrint, notificator, asJSON);
          break;
                              
        case "FINDLIMITS":
          if (parameterName.isEmpty()) {
            ATGlobal.verboseLevel=1;
            ATLog.log("Missing parameter (option -p).", 1);

            return;     
          }
          
          Analysis.getParameterLimits(dataRoot, project, algorithms, parameterName, parameters, timeLimit, instanceID, whereToPrint, notificator, asJSON);
          break;          
          
        case "TIMECOMPLEXITY":
          //TODO remove debug mode
          //si.fri.algotest.global.ATGlobal.debugMode = true;

          String[] params = new String[]{"N AS N"};
          //String[] params = new String[]{""};
          //String[] groupBy = new String[]{""};
          String[] groupBy = new String[]{"N"}; //TODO
          String[] filter = new String[]{""};
          String[] sortBy = new String[]{""};
          String count = "0";
          String mode = parameters.getVariable("mode").getStringValue();
          String computerID = ATGlobal.getThisComputerID();



          switch (mode.toUpperCase()){
            case "SUBINTERVALS":
              // allow only one algorithm, use option -a to select algorithm
              String[] algs = new String[]{algorithms.get(0)};
              String testSet = parameters.getVariable("testSet").getStringValue();
              String observedValue = parameters.getVariable("observedValue").getStringValue();
              String[] testSets = new String[]{String.format("%s AS %s", testSet, testSet)};
              String[] indicators = new String[]{"*EM AS *EM"};
              String yColumn = String.format("%s.%s", algs[0], observedValue);
              EQuery q = new EQuery(algs, testSets,params,indicators,groupBy,sortBy,filter,count,computerID);
              si.fri.algator.analysis.TableData td = DataAnalyser.runQuery(project.getEProject(), q, computerID);
              int yIndex = td.header.indexOf(yColumn);

              double M = parameters.getVariable("M").getDoubleValue();
              int m = parameters.getVariable("m").getIntValue();
              double threshold = parameters.getVariable("threshold").getDoubleValue();
              int xIndex = td.header.indexOf("N"); // TODO N
              Data d = Data.dataFromTableData("test",td,xIndex,yIndex);
              ArrayList<Double> subs = d.findSubintervals(M,m,threshold,0);
              System.out.println("X-values of detected subintervals:");
              for (Double ind : subs){
                System.out.println(ind);
              }
              break;
            case "OUTLIERS":
              // allow only one algorithm, use option -a to select algorithm
              algs = new String[]{algorithms.get(0)};
              testSet = parameters.getVariable("testSet").getStringValue();
              observedValue = parameters.getVariable("observedValue").getStringValue();
              testSets = new String[]{String.format("%s AS %s", testSet, testSet)};
              indicators = new String[]{"*EM AS *EM"};
              yColumn = String.format("%s.%s", algs[0], observedValue);
              q = new EQuery(algs, testSets,params,indicators,groupBy,sortBy,filter,count,computerID);
              td = DataAnalyser.runQuery(project.getEProject(), q, computerID);
              yIndex = td.header.indexOf(yColumn);

              M = parameters.getVariable("M").getDoubleValue();
              xIndex = td.header.indexOf("N");
              d = Data.dataFromTableData("test",td,xIndex,yIndex);
              OutlierDetector.OutlierDetection od = OutlierDetector.detectOutliers(d,M);
              System.out.println("X-values of detected outliers:");
              for (Double ind : od.outlierXValues){
                System.out.println(ind);
              }
              break;
          case "LEASTSQUARESPREDICTOR":
            // allow only one algorithm, use option -a to select algorithm
              algs = new String[]{algorithms.get(0)};
              testSet = parameters.getVariable("testSet").getStringValue();
              observedValue = parameters.getVariable("observedValue").getStringValue();
              testSets = new String[]{String.format("%s AS %s", testSet, testSet)};
              indicators = new String[]{"*EM AS *EM"};
              yColumn = String.format("%s.%s", algs[0], observedValue);
              q = new EQuery(algs, testSets,params,indicators,groupBy,sortBy,filter,count,computerID);
              td = DataAnalyser.runQuery(project.getEProject(), q, computerID);
              yIndex = td.header.indexOf(yColumn);
              xIndex = td.header.indexOf("N");

              d = Data.dataFromTableData("test",td,xIndex,yIndex);
              Data prediction = d.LeastSquares();
              System.out.println("Predicted class:");
              System.out.println(prediction.GetBest());
              System.out.println("Predicted function:");
              System.out.println(prediction.GetFunction());
              break;
            case "GAPREDICTOR":
              // allow only one algorithm, use option -a to select algorithm
              algs = new String[]{algorithms.get(0)};
              testSet = parameters.getVariable("testSet").getStringValue();
              observedValue = parameters.getVariable("observedValue").getStringValue();
              int numberOfAllPopulations = parameters.getVariable("numberOfAllPopulations").getIntValue();
              int populationSize = parameters.getVariable("populationSize").getIntValue();
              int iter = parameters.getVariable("iter").getIntValue();
              int onePopulationIter = parameters.getVariable("onePopulationIter").getIntValue();
              int funcNumber = parameters.getVariable("funcNumber").getIntValue();
              testSets = new String[]{String.format("%s AS %s", testSet, testSet)};
              indicators = new String[]{"*EM AS *EM"};
              yColumn = String.format("%s.%s", algs[0], observedValue);
              q = new EQuery(algs, testSets,params,indicators,groupBy,sortBy,filter,count,computerID);
              td = DataAnalyser.runQuery(project.getEProject(), q, computerID);
              yIndex = td.header.indexOf(yColumn);
              xIndex = td.header.indexOf("N");

              d = Data.dataFromTableData("test",td,xIndex,yIndex);
              GA.GAPredictor(d,numberOfAllPopulations,populationSize,iter,onePopulationIter,funcNumber);
              System.out.println("Predicted function:");
              System.out.println(d.GetFunction());
              break;
            case "MAXDATA":
              // allow only one algorithm, use option -a to select algorithm
              algs = new String[]{algorithms.get(0)};
              Data  dd= Analysis.getParameterLimitFullData(project,algs[0],"N",parameters,timeLimit,instanceID,3,notificator);
              prediction = dd.LeastSquares();
              System.out.println("Predicted class:");
              System.out.println(prediction.GetBest());
              System.out.println("Predicted function:");
              System.out.println(prediction.GetFunction());
              break;
            case "COMPAREALGORITHMS":
              testSet = parameters.getVariable("testSet").getStringValue();
              observedValue = parameters.getVariable("observedValue").getStringValue();
              testSets = new String[]{String.format("%s AS %s", testSet, testSet)};
              indicators = new String[]{"*EM AS *EM"};

              for (String alg : algorithms){
                yColumn = String.format("%s.%s", alg, observedValue);
                q = new EQuery(new String[]{alg}, testSets,params,indicators,groupBy,sortBy,filter,count,computerID);
                td = DataAnalyser.runQuery(project.getEProject(), q, computerID);
                yIndex = td.header.indexOf(yColumn);
                xIndex = td.header.indexOf("N");
                d = Data.dataFromTableData("test",td,xIndex,yIndex);
                System.out.println(alg);
                System.out.println(d.NIntegrate());
              }
              break;
            case "POWERLAWPREDICTOR":
              // allow only one algorithm, use option -a to select algorithm
              algs = new String[]{algorithms.get(0)};
              testSet = parameters.getVariable("testSet").getStringValue();
              observedValue = parameters.getVariable("observedValue").getStringValue();
              testSets = new String[]{String.format("%s AS %s", testSet, testSet)};
              indicators = new String[]{"*EM AS *EM"};
              String data = parameters.getVariable("data").getStringValue();
              yColumn = String.format("%s.%s", algs[0], observedValue);
              q = new EQuery(algs, testSets,params,indicators,groupBy,sortBy,filter,count,computerID);
              td = DataAnalyser.runQuery(project.getEProject(), q, computerID);
              yIndex = td.header.indexOf(yColumn);


              xIndex = td.header.indexOf("N");
              d = Data.dataFromTableData("test",td,xIndex,yIndex);
              if (data.equals("all")){
                Data data2 = PowerLawFunction.getOptimalFittedData(d, -1, -1);
                System.out.println(data2.GetFunction());
              } else {
                subs = d.findSubintervals(2.2, 5, 4, 0);
                double[] xIntervals = new double[subs.size() + 1];
                double[] yIntervals = new double[subs.size() + 1];
                Collections.sort(subs);
                xIntervals[0] = d.X[0];
                yIntervals[0] = d.Y[0];
                int j = 1;
                for (Double ind : subs) {
                  int i = d.GetIndex(ind);
                  xIntervals[j] = ind;
                  yIntervals[j] = d.Y[i];
                  j++;
                }
                Data dataSub = new Data("subintervals", xIntervals, yIntervals);
                Data data2 = PowerLawFunction.getOptimalFittedData(dataSub, -1, -1);
                System.out.println(data2.GetFunction());
              }
              break;

          }
          break;          
          
        default:
          ATGlobal.verboseLevel=1;
          ATLog.log("Invalid function '" + projectName + "'.", 1);
          return;
      }           
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }
  
  public static Variables getParametersFromJSON(String paramsJSON) {
    HashMap<String, Object> params = new HashMap<>();
    try {
      params = ATTools.jSONObjectToMap(new JSONObject(paramsJSON));
    } catch (Exception e) {
    }
    Variables parameters = new Variables();
    for (String paramName : params.keySet()) {
      parameters.addVariable(new EVariable(paramName, params.get(paramName)));
    }
    return parameters;
  }
  
  private static void printResults(Project project, MeasurementType mt, ArrayList<Variables> results, boolean asJSON ) {
    EResult emResultDesc = project.getResultDescriptions().get(mt);
    String[] variablesOrder = EResult.getVariableOrder(project.getTestCaseDescription(), emResultDesc);     

    for (int i=0; i<results.size(); i++) {
      Variables result = results.get(i);
      if (result != null) {
        System.out.println(result.toString(variablesOrder, asJSON, ATGlobal.DEFAULT_CSV_DELIMITER));
      }
    }
  }
}
