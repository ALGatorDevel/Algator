package algator;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algotest.database.Database;
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.ELocalConfig;
import si.fri.algotest.entities.EProject;
import si.fri.algotest.entities.EResult;
import si.fri.algotest.entities.ETestCase;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.users.DBEntity;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.users.DBUser;
import si.fri.algotest.users.UsersDatabase;
import si.fri.algotest.users.UsersTools;

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
	    "create a new project");
    options.addOption("ca", "create_algorithm", false,
	    "create a new algorithm for a given project");
    options.addOption("ct", "create_testset", false,
	    "create a new testset for a given project");    
    options.addOption("cdp", "create_presenter", false,
	    "create a new presenter for a given project");
    options.addOption("rdp", "remove_presenter", false,
	    "remove a presenter for a project");

    
    options.addOption("use", "usage", false, "print usage guide");
    options.addOption("i", "info", false, "print info about entity");
    
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
  public static void initAlgatorSystem(String username, String password) {
    ATGlobal.verboseLevel=Math.max(2, ATGlobal.verboseLevel);
    ATLog.log("Initializing the system ...",0);
      
    // create a database and its tables
    boolean databaseInit = Database.init();
    if (databaseInit) {      
      ATLog.log("Creating a new user ...", 0);
      if (username==null || username.isEmpty()|| password==null || password.isEmpty()) {
        String up[] = getUsernameAndPassword();
        if (up==null) {
          ATLog.log("Empty username or pasword are not alowed.", 0);
          System.exit(0);
        }
        username=up[0];password=up[1];
      }      
      UsersDatabase.addNewUser(username, password);      
      
      ATLog.log("Done.",0);
    }
    
    
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
        System.exit(0);
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
      
      String username=localConfig.getField(ELocalConfig.ID_Username);
      if (line.hasOption("u")) {
	username = line.getOptionValue("u");
      }      
      String password=localConfig.getField(ELocalConfig.ID_Password);
      if (line.hasOption("p")) {
	password = line.getOptionValue("p");
      }            
      
     if (line.hasOption("init")) {
        initAlgatorSystem(username, password);
        System.exit(0);
      }          
           
      
      if (line.hasOption("info")) {
        String project   = (curArgs.length != 0) ? curArgs[0] : "";
        String algorithm = line.hasOption("algorithm") ? line.getOptionValue("algorithm") : "";
        
        String info = getInfo(project, algorithm);
        System.out.println(info);
        
        return;
      }        
      
            
      Database.checkDatabaseAccessAndExitOnError(username, password);
      
      if (line.hasOption("create_project")) {
	if (curArgs.length != 1) {
          System.out.println("Invalid project name");
          printMsg(options); 
        } else {
          createProject(username, curArgs[0]);
          return;
        }
      }

      if (line.hasOption("create_algorithm")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid project or algorithm name");
          printMsg(options); 
        } else {
          createAlgorithm(username, curArgs[0], curArgs[1]);
          return;
        }
      }

      if (line.hasOption("create_testset")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid project or test set name");
          printMsg(options); 
        } else {
          createTestset(username, curArgs[0], curArgs[1]);
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
  
  private static boolean createProject(String username, String proj_name) {
    String dataroot = ATGlobal.getALGatorDataRoot();         
    String projSrcFolder = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(dataroot, proj_name));
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String projConfFolder = ATGlobal.getPROJECTconfig(dataroot, proj_name);
    String testsFolder = ATGlobal.getTESTSroot(dataroot, proj_name);
    String projDocFolder = ATGlobal.getPROJECTdoc(dataroot,proj_name);
    
    HashMap<String,String> substitutions = getSubstitutions(proj_name);
        
    System.out.println("Creating project " + proj_name + " ...");
        
    try {                        
      File projFolderFile = new File(projRoot);
      if (projFolderFile.exists()) {
        System.out.printf("\n Project %s already exists!\n", proj_name);
        return false;
      } 
      
      projFolderFile.mkdirs();
      
      copyFile("templates/PPP.atp",            projConfFolder,  proj_name+".atp",                 substitutions);
      
      copyFile("templates/PPP-em.atrd",        projConfFolder,  proj_name+"-em.atrd",             substitutions);
      copyFile("templates/PPP-cnt.atrd",       projConfFolder,  proj_name+"-cnt.atrd",            substitutions);      
      copyFile("templates/PPP-jvm.atrd",       projConfFolder,  proj_name+"-jvm.atrd",            substitutions);
      
      copyFile("templates/PPP.attc",           projConfFolder,  proj_name+".attc",                substitutions);
      
      copyFile("templates/PPPAbsAlgorithm",    projSrcFolder,   proj_name+"AbsAlgorithm.java",    substitutions);
      copyFile("templates/PPPTestCase",        projSrcFolder,   proj_name+"TestCase.java",        substitutions);
      copyFile("templates/PPPInput",           projSrcFolder,   proj_name+"Input.java",           substitutions);
      copyFile("templates/PPPOutput",          projSrcFolder,   proj_name+"Output.java",          substitutions);
      copyFile("templates/PPPTools",           projSrcFolder,   proj_name+"Tools.java",           substitutions);
      
      copyFile("templates/PPP.html",           projDocFolder,   "project.html",                   substitutions);
      copyFile("templates/P_TS.html",          projDocFolder,   "testset.html",                   substitutions);      
      copyFile("templates/P_TC.html",          projDocFolder,   "testcase.html",                  substitutions);
      copyFile("templates/P_AAA.html",         projDocFolder,   "algorithm.html",                 substitutions);
      copyFile("templates/P_REF.html",         projDocFolder,   "references.html",                substitutions);
      
      UsersTools.setProjectPermissions(username, proj_name);
    } catch (Exception e) {
      System.out.println("Can not create project: " + e.toString());
      return false;
    }
    return true;
  }

  private static boolean createAlgorithm(String username, String proj_name, String alg_name) {        
    String dataroot = ATGlobal.getALGatorDataRoot();         
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String algRoot = ATGlobal.getALGORITHMroot(projRoot, alg_name);
    String algSrc  = ATGlobal.getALGORITHMsrc(projRoot, alg_name);
    String algDocFolder = ATGlobal.getALGORITHMdoc(projRoot, alg_name);
    
    HashMap<String,String> substitutions = getSubstitutions(proj_name);
    substitutions.put("<AAA>", alg_name);
    
    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
      if (!createProject(username, proj_name))
        return false;
    }    
    
    System.out.println("Creating algorithm " + alg_name +  " for the project " + proj_name);
    try {                              
      
      File algFolderFile = new File(algRoot);
      if (algFolderFile.exists()) {
        System.out.printf("\n Algorithm %s already exists!\n", alg_name);
        return false;
      }       
      algFolderFile.mkdirs();
      
      copyFile("templates/AAA.atal",           algRoot,         alg_name+".atal",                 substitutions);
      copyFile("templates/AAAAlgorithm",       algSrc,          alg_name+"Algorithm.java",        substitutions);
      copyFile("templates/AAA.html",           algDocFolder,    "algorithm.html",                 substitutions);
      copyFile("templates/A_REF.html",         algDocFolder,    "references.html",                substitutions);
      
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      ArrayList a = new ArrayList<String>(Arrays.asList(eProject.getStringArray(EProject.ID_Algorithms)));
        a.add(alg_name);
      eProject.set(EProject.ID_Algorithms, a.toArray());
      eProject.saveEntity();

      UsersTools.setEntityPermissions(username, proj_name, alg_name, 2);
    } catch (Exception e) {
      System.out.println("Can not create algorithm: " + e.toString());
      return false;
    }    
    return true;
  }


  private static boolean createTestset(String username, String proj_name, String testset_name) {        
    String dataroot = ATGlobal.getALGatorDataRoot();         
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String testsRoot = ATGlobal.getTESTSroot(dataroot, proj_name);
    String testsDocFolder = ATGlobal.getTESTSdoc(dataroot, proj_name);

    
    HashMap<String,String> substitutions = getSubstitutions(proj_name);
    substitutions.put("<TS>", testset_name);
    
    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
      if (!createProject(username, proj_name))
        return true;
    }    
    
    System.out.println("Creating test set " + testset_name +  " for the project " + proj_name);
    try {                              
      
      File testsetFolderFile = new File(testsRoot);
      if (!testsetFolderFile.exists()) {
        testsetFolderFile.mkdirs();
      }
      
      File testSetFile = new File(testsRoot + File.separator + testset_name+".atts");
      if (testSetFile.exists()) {
        System.out.printf("\n Testset %s already exists!\n", testset_name);
        return false;
      }
             
      
      
      copyFile("templates/TS.atts",            testsRoot,       testset_name+".atts",             substitutions);
      copyFile("templates/TS.txt",             testsRoot,       testset_name+".txt",              substitutions);
      
      copyFile("templates/TS.html",            testsDocFolder,  testset_name + ".html",           substitutions);
            
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      ArrayList ts = new ArrayList<String>(Arrays.asList(eProject.getStringArray(EProject.ID_TestSets)));
        ts.add(testset_name);
      eProject.set(EProject.ID_TestSets, ts.toArray());
      eProject.saveEntity();

      UsersTools.setEntityPermissions(username, proj_name, testset_name, 3);
    } catch (Exception e) {
      System.out.println("Can not create test set: " + e.toString());
      return false;
    }    
    return true;
  }


  private static String createPresenter(String username, String proj_name, String presenter_name, int type) {
    if (presenter_name==null || presenter_name.isEmpty())
      presenter_name = getNextAvailablePresenterName(proj_name, type);
    
    String dataroot = ATGlobal.getALGatorDataRoot();         
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String presenterRoot = ATGlobal.getPRESENTERSroot(dataroot, proj_name);

    
    HashMap<String,String> substitutions = getSubstitutions(proj_name);
    substitutions.put("<TP>", presenter_name);
    
    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
      if (!createProject(username, proj_name))
        return null;
    }    
    
    System.out.println("Creating presenter " + presenter_name +  " for the project " + proj_name);
    try {                              
      
      File presenterFolderFile = new File(presenterRoot);
      if (!presenterFolderFile.exists()) {
        presenterFolderFile.mkdirs();
      }
      
      File presenterFile = new File(presenterRoot + File.separator + presenter_name+".atpd");
      if (presenterFile.exists()) {
        System.out.printf("\n Presenter %s already exists!\n", presenter_name);
        return null;
      }
                         
      copyFile("templates/TP.atpd",  presenterRoot,  presenter_name + ".atpd", substitutions);
      
      copyFile("templates/TP.html",  presenterRoot,  presenter_name + ".html", substitutions);
            
      String id = EProject.ID_ProjPresenters;
      switch (type) {
        case 0: id = EProject.ID_MainProjPresenters;break;
        case 1: id = EProject.ID_ProjPresenters;    break;
        case 2: id = EProject.ID_MainAlgPresenters; break;
        case 3: id = EProject.ID_AlgPresenters;     break;
      }
      
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      ArrayList tp = new ArrayList<String>(Arrays.asList(eProject.getStringArray(id)));
        tp.add(presenter_name);
      eProject.set(id, tp.toArray());
      eProject.saveEntity();

    } catch (Exception e) {
      System.out.println("Can not create presenter: " + e.toString());
      return null;
    }    
    return presenter_name;
  }
    
  private static String getNextAvailablePresenterName(String proj_name, int type) {
    String presenterName = "Presenter";
    
    String dataroot = ATGlobal.getALGatorDataRoot();
    String presenterPath = ATGlobal.getPRESENTERSroot(dataroot, proj_name);    
    EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
    
    String prefix = "";
    switch (type) {
      case 0: prefix = "mp"; break;
      case 1: prefix = "p";  break;
      case 2: prefix = "ma"; break;
      case 3: prefix = "a";  break;
    }
  
      ArrayList<String> tp = new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_MainProjPresenters)));
           tp.addAll(new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_ProjPresenters))));
           tp.addAll(new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_MainAlgPresenters))));
           tp.addAll(new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_AlgPresenters))));      
      tp.replaceAll(x -> x.toUpperCase());
           
      int id=1;
      while (true) {
        presenterName = prefix + "Presenter" + id++;
        if (!tp.contains(presenterName.toUpperCase()) && !new File(presenterPath, presenterName+".atpd").exists()) break;
      }
                
    return presenterName;
  }
  
  
  private static String removePresenter(String proj_name, String presenter_name) {   
    String dataroot = ATGlobal.getALGatorDataRoot();         
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String presenterRoot = ATGlobal.getPRESENTERSroot(dataroot, proj_name);
    
    try {
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      String [] presIDs = new String[] {EProject.ID_MainProjPresenters, EProject.ID_ProjPresenters, EProject.ID_MainAlgPresenters,EProject.ID_AlgPresenters};
      for (String presID : presIDs) {
        ArrayList<String> tp = new ArrayList<String>(Arrays.asList(eProject.getStringArray(presID)));
        if (tp.contains(presenter_name)) {
          tp.remove(presenter_name);          
          eProject.set(presID, tp.toArray());
          eProject.saveEntity();
          
          new File(presenterRoot, presenter_name + ".atpd").delete();
          new File(presenterRoot, presenter_name + ".html").delete();
                 
          return "Presenter "+presenter_name + " sucessfully removed.";         
        }
      }
      return "Presenter "+presenter_name+" does not exist.";
      

    } catch (Exception e) {      
      return "Can not remove presenter: " + e.toString();
    }    
  }  
  /**
   * Returns: list of all projects (if projectName.isEmpty), info about project (if projectName is 
   * defined and algorithmName.isEmpty) and info about algorithm (if both, projectName and 
   * algorithmName are defined).
   * 
   * @param projectName
   * @param algorithmName
   * @return 
   */
  public static String getInfo(String projectName, String algorithmName) {
    if (projectName.isEmpty()) {             // list of projects
      JSONObject projInfo = new JSONObject();

      
      ArrayList<DBEntity> projects = UsersTools.listProjects();   
      JSONArray ja = new JSONArray();
      for (DBEntity project : projects) {
        ja.put(project.toString());
      }
      projInfo.put("Projects", ja);
      
      return projInfo.toString(2);
    } else if (algorithmName.isEmpty()) {    // project info
      Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);
      JSONObject projInfo = new JSONObject();
      
      projInfo.put("Name", project.getName());
      
      // list of algorithms
      TreeMap<String,EAlgorithm> algs = project.getAlgorithms();
      JSONArray jaA = new JSONArray();
      for (String algName : algs.keySet()) {
        jaA.put(algName);
      }
      projInfo.put("Algorithms", jaA);
      
      // list of algorithms
      TreeMap<String,ETestSet> testsets = project.getTestSets();
      JSONArray jaS = new JSONArray();
      for (String testsetName : testsets.keySet()) {
        jaS.put(testsetName);
      }
      projInfo.put("TestSets", jaS);
      
      
      JSONArray mpp = new JSONArray();
            
      projInfo.put("MainProjPresenters", new JSONArray(project.getEProject().getStringArray(EProject.ID_MainProjPresenters)));
      projInfo.put("ProjPresenters",     new JSONArray(project.getEProject().getStringArray(EProject.ID_ProjPresenters)));
      projInfo.put("MainAlgPresenters",  new JSONArray(project.getEProject().getStringArray(EProject.ID_MainAlgPresenters)));      
      projInfo.put("AlgPresenters",      new JSONArray(project.getEProject().getStringArray(EProject.ID_AlgPresenters)));      
      
      
      ETestCase eTestCase = project.getTestCaseDescription();
      
      HashMap<MeasurementType,EResult> rDesc = project.getResultDescriptions();
      JSONObject jrDesc = new JSONObject();
      for(MeasurementType mType : rDesc.keySet()) {
        if (mType.equals(MeasurementType.JVM)) continue;
        
        EResult eRes = rDesc.get(mType);
        JSONObject params     = new JSONObject();
        JSONObject indicators = new JSONObject();
        
        for (EVariable var : eTestCase.getParameters()) {
          params.put(var.getName(), new JSONObject(var.toJSONString()));
        }
        
        for (EVariable var : eRes.getIndicators()) {
          indicators.put(var.getName(), new JSONObject(var.toJSONString()));
        }
        
        JSONObject curRD = new JSONObject();
        curRD.put("Parameters", params);
        curRD.put("Indicators", indicators);
        curRD.put("VariableOrder", EResult.getVariableOrder(eTestCase, eRes));
        jrDesc.put(mType.name(), curRD);
      } 
      projInfo.put("Result", jrDesc);
      
      return projInfo.toString(2);
    } else {                                 // algorithm info
      Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);
      if (project == null) return "";
      
      EAlgorithm alg = project.getAlgorithms().get(algorithmName);
      if (alg == null) return "";
      return alg.toJSONString();
    }
  }
  
  
  //*******************************
  
  private static HashMap<String, String> getSubstitutions(String proj_name) {
    StringBuffer lc = new StringBuffer(proj_name);
    lc.setCharAt(0, Character.toLowerCase(proj_name.charAt(0)));
    String projNameCamelCase = lc.toString();

    SimpleDateFormat sdf = new SimpleDateFormat("MM, YYYY");
        
    HashMap<String, String> substitutions = new HashMap();
    substitutions.put("<PPP>", proj_name);
    substitutions.put("<pPP>", projNameCamelCase);
    substitutions.put("<today>", sdf.format(new Date()));   
    
    substitutions.put("\r", "\n");   
    
    return substitutions;
  }
  
  private static String readFile(String fileName) {
    try {
      ClassLoader classLoader = Admin.class.getClassLoader();
      InputStream fis = classLoader.getResourceAsStream(fileName);
      return new Scanner(fis).useDelimiter("\\Z").next();      
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    return "";
  }
  
  private static void writeFile(String fileName, String content) {
    try {
      // first creates a folder ...
      String filePath = FilenameUtils.getFullPath(fileName);
      File filePathFile = new File(filePath);
      filePathFile.mkdirs();
      
      // ... then writes a content
      PrintWriter pw = new PrintWriter(fileName);
      pw.print(content);
      pw.close();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }
    
  private static String replace(String source, String what, String with) {
    return source.replaceAll(what, with);
  }
  
  /**
   * Copies a template to destination folder  + makes substitutions. 
   */
  private static void copyFile(String tplName, String outputFolder, String outputFileName, HashMap<String, String> substitutions) { 
    String absAlg = readFile(tplName);
    for(String key: substitutions.keySet()) {
      absAlg = replace(absAlg, key, substitutions.get(key));
    }
    writeFile(new File(outputFolder, outputFileName).getAbsolutePath(), absAlg);
  }

}
