package algator;

import static algator.Execute.syncTests;
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
import org.json.JSONArray;
import si.fri.algotest.analysis.Analysis;
import si.fri.algotest.analysis.DataAnalyser;
import si.fri.algotest.analysis.TableData;
import si.fri.algotest.database.Database;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.ELocalConfig;
import si.fri.algotest.entities.EQuery;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.entities.Variables;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.global.ErrorStatus;
import si.fri.timeComplexityAnalysis.Data;
import si.fri.timeComplexityAnalysis.OutlierDetector;

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
        
    Option username = OptionBuilder.withArgName("username")	    
	    .hasArg(true)
	    .withDescription("the name of the current user")
	    .create("u");

    Option password = OptionBuilder.withArgName("password")	    
	    .hasArg(true)
	    .withDescription("the password of the current user")
	    .create("p");    
    
    Option par = OptionBuilder.withArgName("parameters")	    
	    .hasArg(true)
	    .withDescription("the list of parameters")
	    .create("par");    
    Option ind = OptionBuilder.withArgName("indicators")	    
	    .hasArg(true)
	    .withDescription("the list of indicators")
	    .create("ind");    
    Option group = OptionBuilder.withArgName("groupBy")	    
	    .hasArg(true)
	    .withDescription("the list of groupBy criteria")
	    .create("group");    
    Option filter = OptionBuilder.withArgName("filter")	    
	    .hasArg(true)
	    .withDescription("the list of filter criteria")
	    .create("filter");    
    Option sort = OptionBuilder.withArgName("sort")	    
	    .hasArg(true)
	    .withDescription("the list of sortBy criteria")
	    .create("sort");    
    
    Option opt = OptionBuilder.withArgName("options")	    
	    .hasArg(true)
	    .withDescription("the list of options")
	    .create("opt");    

    
    options.addOption(algorithm);
    options.addOption(testset);
    options.addOption(data_root);
    options.addOption(data_local);    
    options.addOption(algator_root);
    options.addOption(measurement);
    options.addOption(par);
    options.addOption(ind);
    options.addOption(group);
    options.addOption(filter);
    options.addOption(sort);
    
    options.addOption(opt);
    
    
    options.addOption(username);
    options.addOption(password);
        
    options.addOption(verbose);
    options.addOption(logTarget);
    
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
  
  private static String[] getStringArrayFromJSON(String paramsJSON) {
    try {
      JSONArray ja = new JSONArray(paramsJSON);
      String[] result = new String[ja.length()];
      for (int i = 0; i < ja.length(); i++) {
        result[i] = ja.getString(i);
      }
      return result;
    } catch (Exception e) {
      return new String[0];
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
        System.out.println(introMsg + "\n");
	printMsg(options);
        return;
      }

      if (line.hasOption("use")) {
        System.out.println(introMsg + "\n");
        printUsage();
        return;
      }
      
      String[] curArgs = line.getArgs();
      if (curArgs.length != 1) {
        System.out.println(introMsg + "\n");
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
      
      String[] par = new String[]{"*"};
      if (line.hasOption("par")) {
        par = getStringArrayFromJSON(line.getOptionValue("par"));
      }
      
      String[] ind = new String[]{"*"+mType.getExtension().toUpperCase()};
      if (line.hasOption("ind")) {
        ind = getStringArrayFromJSON(line.getOptionValue("ind"));
      }
      
      String[] group = new String[0];
      if (line.hasOption("group")) {
        group = getStringArrayFromJSON(line.getOptionValue("group"));
      }
      String[] filter = new String[0];
      if (line.hasOption("filter")) {
        filter = getStringArrayFromJSON(line.getOptionValue("filter"));
      }
      String[] sort = new String[0];
      if (line.hasOption("sort")) {
        sort = getStringArrayFromJSON(line.getOptionValue("sort"));
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
      
      // before executing algorithms we sync test folder from data_root to data_local
      if (!syncTests(projectName)) 
        return;      
      
      TableData td = runQuery(dataRoot, projectName, algorithmName, testsetName, mType, par, ind, group, filter, sort);
      
      if (td == null) return;
      
      
      if (line.hasOption("opt")) {
        Variables opts = Analyse.getParametersFromJSON(line.getOptionValue("opt", "{}"));
        runOptions(td, opts);
      } else {
        System.out.println(td.toString());
      }
      
 
    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }
  
  /**
   * Metoda vrne stolpec z indeksom column. Če je columen negativen, to pomeni 
   * stolpce štete od zadnje strani (-1 zadnji, -2 predzadnji, ...). Vsebino stolpca 
   * pretvori v double; če pri pretvorbi pride do napake, se v tabelo zapiše 0.
   * 
   */
  private static double[] getColumn(TableData td, int column) {
    double[] result = new double[td.data.size()];
    if (column < 0)
      column += td.data.get(0).size();
    
    for (int i = 0; i < td.data.size(); i++) {
      double v = 0;
      try {
        try {
          v = ((Number)td.data.get(i).get(column)).doubleValue();
        } catch (Exception e){
          v = Double.parseDouble((String)td.data.get(i).get(column));
        }        
      } catch (Exception e) {}
      result[i] = v;
    }
    return result;
  }

  private static void runOptions(TableData td, Variables parameters) {
    EVariable xPar = parameters.getVariable("x");
    int xStolpec = (xPar==null) ? -2 : xPar.getIntValue(-2);
    
    EVariable yPar = parameters.getVariable("y");
    int yStolpec = (yPar==null) ? -1 : yPar.getIntValue(-1);
        
    double [] xVal = getColumn(td, xStolpec);
    double [] yVal = getColumn(td, yStolpec);
    
    EVariable vMode = parameters.getVariable("mode");
    String mode = vMode==null ? "none" : vMode.getStringValue();    
    switch (mode) {
      case "outliers":
        EVariable mPar = parameters.getVariable("M");
        double m = (mPar == null) ? 2 : mPar.getDoubleValue();
        
        Data data = new Data("outlier", xVal, yVal);
        OutlierDetector.OutlierDetection od = OutlierDetector.detectOutliers(data,m);
        
        System.out.println("X-values of detected outliers:");
        for (Double ind : od.outlierXValues){
          System.out.println(ind);
        }
        break;
    }
  }
  
  private static TableData runQuery(String dataRoot, String projName, String algName,
	  String testsetName, MeasurementType mType, String[] par, String[] ind,
          String[] groupBy, String[] filter, String[] sortBy) {
    
    if (!ATGlobal.projectExists(dataRoot, projName)) {
      ATGlobal.verboseLevel=1;
      ATLog.log("Project configuration file does not exist for " + projName, 1);

      return null;      
    }
    

    // Test the project
    Project projekt = new Project(dataRoot, projName);
    if (!projekt.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) {
      ATGlobal.verboseLevel=1;
      ATLog.log("Invalid project: " + projekt.getErrors().get(0).toString(), 1);

      return null;
    }
            
    // Test algorithms
    ArrayList<EAlgorithm> eAlgs;
    if (!algName.isEmpty()) {
      EAlgorithm alg = projekt.getAlgorithms().get(algName);
      if (alg == null) {
        ATGlobal.verboseLevel=1;
	ATLog.log("Invalid algorithm - " + algName, 1);
	return null;
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
	return null;
      }
      eTests = new ArrayList<>(); 
      eTests.add(test);
    } else {
       eTests = new ArrayList(projekt.getTestSets().values());
       eTests.add(otherTst);
    }
            
    int i=0; String [] sAlgs = new String[eAlgs.size()]; for (EAlgorithm eAlg : eAlgs ) {sAlgs[i++] = eAlg.getName();}
        i=0; String [] sTsts = new String[eTests.size()];for (ETestSet   eTst : eTests) {sTsts[i++] = eTst.getName();}
        
    
    EQuery eq = new EQuery(sAlgs, sTsts, par, ind, groupBy, filter, sortBy, "0", "");
    return DataAnalyser.runQuery(projekt.getEProject(), eq, "");
  }
}
