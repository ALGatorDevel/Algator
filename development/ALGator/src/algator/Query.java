package algator;

import static algator.Execute.syncTests;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.json.JSONArray;
import si.fri.algator.analysis.Analysis;
import si.fri.algator.analysis.DataAnalyser;
import si.fri.algator.analysis.TableData;
import si.fri.algator.database.Database;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.ELocalConfig;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.analysis.complexity.DataTools;
import si.fri.algator.analysis.complexity.FitData;
import si.fri.algator.analysis.complexity.FittingFunction;
import si.fri.algator.analysis.complexity.FunctionType;
import si.fri.algator.analysis.timecomplexity.Data;
import si.fri.algator.analysis.timecomplexity.OutlierDetector;

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
    Option compID = OptionBuilder.withArgName("cID")	    
	    .hasArg(true)
	    .withDescription("the compID")
	    .create("cID");    
    
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
    options.addOption(compID);

    
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
      
      String compID = "";
      if (line.hasOption("cID")) {
        compID = line.getOptionValue("cID");
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
      
      String username=localConfig.getUsername();
      if (line.hasOption("u")) {
	username = line.getOptionValue("u");
      }      
      String password=localConfig.getPassword();
      if (line.hasOption("p")) {
	password = line.getOptionValue("p");
      }           

      if (!Database.databaseAccessGranted(username, password)) return;
      
      // before executing algorithms we sync test folder from data_root to data_local
      if (!syncTests(projectName)) 
        return;      
      
      TableData td = runQuery(dataRoot, projectName, algorithmName, testsetName, mType, par, ind, group, filter, sort, compID);
      
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
      case "outliersold":
        EVariable mPar = parameters.getVariable("M");
        double m = (mPar == null) ? 2 : mPar.getDoubleValue();
        
        Data data = new Data("outlier", xVal, yVal);
        OutlierDetector.OutlierDetection od = OutlierDetector.detectOutliers(data,m);
        
        System.out.println("X-values of detected outliers:");
        for (Double ind : od.outlierXValues){
          System.out.println(ind);
        }
        break;
      case "fitold":
        Data fitData = new Data("fit", xVal, yVal);
        
        Data prediction = fitData.LeastSquares();
        System.out.println("Predicted class:");
        System.out.println(prediction.GetBest());
        System.out.println("Predicted function:");
        System.out.println(prediction.GetFunction());
        
        break;
      case "outliers":
        break;
        
        // fitanje izmerjenih podatkov: za vsako krivuljo iz FunctionType.* izračunam optimalen fit
        // in rmse ter rmspe dobljene krivulje in izmerjenih podatkov. Za fitanje vzamem le prvih 1/k 
        // podatkov, rmse in rmspe računam na vseh podatkih. Izpis funkcij je lahko urejen po rmse ali
        // po rmspe. Forma izpisa je lahko prilagojen za uporabo v jupytru ali algatorju. 
        //
        // paramateri: k=delilni faktor (default=1)  ... za fitanje uporabim 1/k podatkov 
        //             u=način urejanja (default=0)  ... 0 ... po rmse, 1 ... po rmspe
        //             out=tip izhoda   (default="") ... moznosti: "", "jupyter", "algator"
       case "fit":
         int kData = (parameters.getVariable("k") == null ? 1 : parameters.getVariable("k").getIntValue(1));
         int uData = (parameters.getVariable("u") == null ? 0 : parameters.getVariable("u").getIntValue(0));

         // vrednost parametra "output": FN ... izpis naj bo prilagojen za uvoz v jupyter
        String out = (parameters.getVariable("out") == null ? "" : parameters.getVariable("out").getStringValue());
         

        int fD = xVal.length/kData, sD = xVal.length - fD;
        double[] hX1 = new double[fD]; System.arraycopy(xVal, 0,  hX1, 0, fD);
        double[] hX2 = new double[sD]; System.arraycopy(xVal, fD, hX2, 0, sD);
        double[] hY1 = new double[fD]; System.arraycopy(yVal, 0,  hY1, 0, fD);
        double[] hY2 = new double[sD]; System.arraycopy(yVal, fD, hY2, 0, sD);
        
        class CFit implements Comparable<CFit>{
          double params[];
          FunctionType fType;
          double rmse;
          double rmspe; 
          double val; // rmse or rmspe (set will be sorted by this value)
  
          public int compareTo(CFit t) {
            return Double.compare(this.val, t.val);
          }                    
        }
        
         TreeSet<CFit> fits = new TreeSet<>();
        for (FunctionType fType: FunctionType.values()) {
          CFit cfit = new CFit();
          cfit.fType  = fType;
          cfit.params = FitData.findFit(hX1, hY1, fType);
          cfit.rmse   = DataTools.rmse (xVal, yVal, new FittingFunction(fType), cfit.params);
          cfit.rmspe  = DataTools.rmspe(xVal, yVal, new FittingFunction(fType), cfit.params);
          cfit.val = uData == 0 ? cfit.rmse : cfit.rmspe;
          fits.add(cfit);
        }
        int ik=0;
        for (CFit fit : fits) { 
          switch (out) {
            case "jupyter":
              String fn = fit.fType.toString(fit.params).replaceAll("[,]", ".").replaceAll("x\\^1", "*x").replaceAll("x\\^2", "*x*x")
                   .replaceAll("x\\^3", "*x*x*x").replaceAll("log[(]x[)]", "np.log(x)");
              if (fit.fType==FunctionType.CONST) fn += " + 0*x";
              System.out.println("# " + fit.fType.toString());
              System.out.printf("y%d=%s\n", ik, fn);
              System.out.printf("pylab.plot(x,y%d)\n", ik++);
              break;
            case "algator":
              String param = td.header.get(td.header.size()-2);
              System.out.printf("%s AS %S\n",
                fit.fType.toString(fit.params).replaceAll(",", ".")
                   .replaceAll("x\\^1", "*@PAR").replaceAll("x\\^2", "*@PAR*@PAR").replaceAll("x\\^3", "*@PAR*@PAR*@PAR")
                   .replaceAll("log[(]x[)]", "log(@PAR)").replaceAll("exp[(]x[)]", "exp(@PAR)")
                   .replaceAll("x", "*@PAR").replaceAll(" +", "").replaceAll("@PAR", "@"+param),
                fit.fType.toString()
              );
              break;
            default:
              System.out.printf("%-13s - %-80s - %12.2f %12.2f\n",
                fit.fType.toString(), fit.fType.toString(fit.params), fit.rmse,fit.rmspe);
          }
        }
        break;        
        
      // Recimo, da bi (x[i], y[i]) fitali s preprostimi funkcijami (a*x, a*x^2, a*x^3, ...), koliko je a?
      // Opcija "findf" za vse preproste funkcije izračuna a za vsak par (x[i],y[i]) iz ga izpiše.
      // Izpiše se tudi statistika koeficientov za vsako funkcijo.
        // to sem dodal, ker sem mislil, da bom na ta način lahko ugotovil, katera funkcija je najbolj primerna
        // za fitanje danih podatkov (mislil sem, da bo pri funkciji, ki je najbolj primerna, 
        // relativna varianca (ali katera druga statistična vrednost) najmanjša. Žal se izkaže, da je 
        // najmanjša varianca pri x^3, kar za quicksort gotovo ni dober rezultat.
        //
        // OPOMBA: podoben rezultat dobimo z uporabo opcije "fit" - krivulje CLINEAR, CLOG, CLOGLOG, ...
        //         imajo zelo podoben vodilni koeficient kot je povprečje a-jev, ki ga izpiše findf; 
        //         razlika je tudi v tem, da fit ne izpiše std.odklona (saj vodilni koef. računa drugače).
      case "findf":
       System.out.println("x: " + Arrays.toString(xVal));
       System.out.println("y: " + Arrays.toString(yVal));
               
        for (FunctionType fType: FunctionType.values()) {
          double koefs[] = FitData.calculateKoefs(xVal, yVal, fType);
          
          SummaryStatistics ss = new SummaryStatistics();
          for (double koef : koefs) ss.addValue(koef);  
          
          System.out.printf("%-13s - %s\n",
            fType.toString(), Arrays.toString(koefs));
          System.out.printf("Mean: %.15f, Variance: %.15f, RelV: %.15f\n\n", ss.getMean(), ss.getVariance(), ss.getVariance() / ss.getMean());
        }
        break;
    }
  }
  
  private static TableData runQuery(String dataRoot, String projName, String algName,
	  String testsetName, MeasurementType mType, String[] par, String[] ind,
          String[] groupBy, String[] filter, String[] sortBy, String compID) {
    
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
        
    
    EQuery eq = new EQuery(sAlgs, sTsts, par, ind, groupBy, filter, sortBy, "0", compID);
    return DataAnalyser.runQuery(projekt.getEProject(), eq, "");
  }
}
