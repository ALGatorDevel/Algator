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
import org.asql.ASqlObject;
import org.json.JSONObject;
import si.fri.algator.analysis.DataAnalyser;
import si.fri.algator.analysis.view.ChartPanels;
import si.fri.algator.analysis.TableData;
import si.fri.algator.entities.EPresenter;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.Project;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class Chart {

  private static String introMsg = "ALGator Analyzer, " + Version.getVersion();

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

    Option query = OptionBuilder.withArgName("query_name")
            .withLongOpt("query")
            .hasArg(true)
            .withDescription("the name of the query to run")
            .create("q");

    Option queryOrigin = OptionBuilder.withArgName("[R|F|S]")
            .withLongOpt("query_origin")
            .hasArg(true)
            .withDescription("the origin of the query (R=data root folder, F=custom folder, S=standard input); default: R")
            .create("o");

    Option queryFormat = OptionBuilder.withArgName("JSON|ASQL")
            .withLongOpt("query_format")
            .hasArg(true)
            .withDescription("query format")
            .create("qf");

    Option computerID = OptionBuilder.withArgName("computer_id")
            .withLongOpt("cid")
            .hasArg(true)
            .withDescription("the ID of computer that produced results; default: this computer ID")
            .create("c");

    Option verbose = OptionBuilder.withArgName("verbose_level")
            .withLongOpt("verbose")
            .hasArg(true)
            .withDescription("print additional information (0 = OFF, 1 = some (default), 2 = all")
            .create("v");

    Option presenter = OptionBuilder.withArgName("presenter_name")
            .withLongOpt("presenter")
            .hasArg(true)
            .withDescription("the name of the presenter to open")
            .create("p");

    options.addOption(data_root);
    options.addOption(data_local);
    options.addOption(algator_root);
    options.addOption(query);
    options.addOption(presenter);
    options.addOption(queryOrigin);
    options.addOption(computerID);
    options.addOption(verbose);
    options.addOption(queryFormat);

    options.addOption("h", "help", false,
            "print this message");

    options.addOption("use", "usage", false,
            "print usage guide");

    return options;
  }

  private static void printMsg(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("algator.Chart [options] project_name", options);

  }

  private static void printUsage() {
    Scanner sc = new Scanner((new Chart()).getClass().getResourceAsStream("/data/ChartUsage.txt"));
    while (sc.hasNextLine()) {
      System.out.println(sc.nextLine());
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
      if (curArgs.length != 1) {
        printMsg(options);
        return;
      }

      boolean printTable = false;

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

      // for analysing the data_root folder is used as a default data source 
      String dataLocal = ATGlobal.getALGatorDataLocal();
      if (line.hasOption("data_local")) {
        dataLocal = line.getOptionValue("data_local");
      }
      ATGlobal.setALGatorDataLocal(dataLocal);

      ATGlobal.verboseLevel = 1;
      if (line.hasOption("verbose")) {
        if (line.getOptionValue("verbose").equals("0")) {
          ATGlobal.verboseLevel = 0;
        }
        if (line.getOptionValue("verbose").equals("2")) {
          ATGlobal.verboseLevel = 2;
        }
      }

      String projectName = curArgs[0];
      Project projekt = new Project(dataRoot, projectName);
      if (!projekt.getErrors().get(0).equals(ErrorStatus.STATUS_OK)) {
        System.out.println(projekt.getErrors().get(0));
        return;
      }

      String cid = ATGlobal.getThisComputerID();
      if (line.hasOption("cid")) {
        cid = line.getOptionValue("cid");
      }

      System.out.println(cid);

      EPresenter presenter = null;
      if (line.hasOption("presenter")) {
        presenter = getPresenter(line.getOptionValue("presenter"), dataRoot, projectName);
      }

      String origin = line.getOptionValue("query_origin");
      if (origin == null) {
        origin = "R";
      }

      if (!line.hasOption("query")) {
        System.out.println(introMsg + "\n");
        System.out.println("Data root = " + dataRoot);
      }

      if (line.hasOption("query") || "S".equals(origin)) {
        // if a query is given, run a query and print result ...
        String result = runQuery(projekt, line.getOptionValue("query"), origin, line.getOptionValue("query_format"), cid);
        System.out.println(result);
      } else {
        // ...else run a GUI analizer
        new ChartPanels(projekt, cid, presenter);
      }

    } catch (ParseException ex) {
      printMsg(options);
      return;
    }
  }

  private static EPresenter getPresenter(String presenter, String dataRoot, String projectName) {
    String presenterFileName = presenter;
    if (!presenterFileName.endsWith("."+ATGlobal.AT_FILEEXT_presenter))
      presenterFileName += "."+ATGlobal.AT_FILEEXT_presenter;
    
    String presenterRoot = ATGlobal.getPRESENTERSroot(ATGlobal.getPROJECTroot(dataRoot, projectName));
    EPresenter result =  new EPresenter();
    result.initFromFile(new File(presenterRoot + File.separator + presenterFileName));
    return result;
  }

  public static String runQuery(Project project, String queryName, String origin, String computerID) {
    return runQuery(project, queryName, origin, null, computerID);
  }

  public static String runQuery(Project project, String queryName, String origin, String format, String computerID) {
    EQuery query = new EQuery();
    String vsebina = "";
    switch (origin) {
      case "S":
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
          vsebina += sc.nextLine() + "\n";
        }
        if (!"ASQL".equals(format)) {
          JSONObject queryObject = new JSONObject(vsebina);
          query.initFromJSON(queryObject.get("Query").toString());
        }
        break;
      case "F":
      case "R":
        String fileName;
        if (origin.equals("F")) {
          fileName = queryName;
        } else {
          fileName = ATGlobal.getQUERYfilename(project.getEProject().getProjectRootDir(), queryName);
        }

        //File queryFN = new File(fileName);
        //if (!queryFN.exists()) fileName += "." + ATGlobal.AT_FILEEXT_query;
        if (!"ASQL".equals(format)) {
          query.initFromFile(new File(fileName));
        } else {
          vsebina = query.getFileText(new File(fileName));
        }
        break;
    }

    // debug: System.out.println("---> " + query.toJSONString());
    String result = ErrorStatus.getLastErrorMessage().equals(ErrorStatus.STATUS_OK)
            ? "Invalid query." : ErrorStatus.getLastErrorMessage();
    if ("ASQL".equals(format)) {
      TableData td = new ASqlObject(vsebina).runQuery(project);
      result = td.toString();
    } else {
      if (query != null & !query.toJSONString().equals("{}")) {
        // run query ...
        TableData td = DataAnalyser.runQuery(project.getEProject(), query, computerID);
        // ... and print table to screen
        result = td.toString();
      }
    }
    return result;
  }
}
