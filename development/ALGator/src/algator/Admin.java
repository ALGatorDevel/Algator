package algator;

import java.io.ByteArrayOutputStream;
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
import si.fri.algotest.entities.EAlgorithm;
import si.fri.algotest.entities.EProject;
import si.fri.algotest.entities.EResult;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.MeasurementType;
import si.fri.algotest.entities.Project;
import si.fri.algotest.users.DBEntity;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.users.PermTools;

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
    
    
    options.addOption(data_root);
    options.addOption(algator_root);    
    options.addOption(algorithm);
    options.addOption(verbose);
    
    options.addOption("h", "help", false,
	    "print this message");
    
    options.addOption("cp", "create_project", false,
	    "create a new project");
    options.addOption("ca", "create_algorithm", false,
	    "create a new algorithm for a given project");
    options.addOption("ct", "create_testset", false,
	    "create a new testset for a given project");    
    
    options.addOption("u", "usage", false, "print usage guide");
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

      if (line.hasOption("u")) {
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
        if (line.getOptionValue("verbose").equals("1"))
          ATGlobal.verboseLevel = 1;
        if (line.getOptionValue("verbose").equals("2"))
          ATGlobal.verboseLevel = 2;
      }
      
      
      if (line.hasOption("info")) {
        String project   = (curArgs.length != 0) ? curArgs[0] : "";
        String algorithm = line.hasOption("algorithm") ? line.getOptionValue("algorithm") : "";
        
        String info = getInfo(project, algorithm);
        System.out.println(info);
        
        return;
      }
      
      if (line.hasOption("create_project")) {
	if (curArgs.length != 1) {
          System.out.println("Invalid project name");
          printMsg(options); 
        } else {
          createProject(curArgs[0]);
          return;
        }
      }

      if (line.hasOption("create_algorithm")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid project or algorithm name");
          printMsg(options); 
        } else {
          createAlgorithm(curArgs[0], curArgs[1]);
          return;
        }
      }

      if (line.hasOption("create_testset")) {
	if (curArgs.length != 2) {
          System.out.println("Invalid project or test set name");
          printMsg(options); 
        } else {
          createTestset(curArgs[0], curArgs[1]);
          return;
        }
      }
      
      
      printMsg(options);
    } catch (ParseException ex) {
      printMsg(options);
    }
  }
  
  
  
  private static boolean createProject(String proj_name) {
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
      
      PermTools.setProjectPermissions(proj_name);
    } catch (Exception e) {
      System.out.println("Can not create project: " + e.toString());
      return false;
    }
    return true;
  }

  private static boolean createAlgorithm(String proj_name, String alg_name) {        
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
      if (!createProject(proj_name))
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

      PermTools.setAlgorithmPermissions(proj_name, alg_name);
    } catch (Exception e) {
      System.out.println("Can not create algorithm: " + e.toString());
      return false;
    }    
    return true;
  }


  private static boolean createTestset(String proj_name, String testset_name) {        
    String dataroot = ATGlobal.getALGatorDataRoot();         
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String testsRoot = ATGlobal.getTESTSroot(dataroot, proj_name);
    String testsDocFolder = ATGlobal.getTESTSdoc(dataroot, proj_name);

    
    HashMap<String,String> substitutions = getSubstitutions(proj_name);
    substitutions.put("<TS>", testset_name);
    
    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
      if (!createProject(proj_name))
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

    } catch (Exception e) {
      System.out.println("Can not create test set: " + e.toString());
      return false;
    }    
    return true;
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

      
      ArrayList<DBEntity> projects = Users.listProjects();   
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
      
      HashMap<MeasurementType,EResult> rDesc = project.getResultDescriptions();
      JSONObject jrDesc = new JSONObject();
      for(MeasurementType mType : rDesc.keySet()) {
        if (mType.equals(MeasurementType.JVM)) continue;
        
        EResult eRes = rDesc.get(mType);
        JSONObject params     = new JSONObject();
        JSONObject indicators = new JSONObject();
        
        for (EVariable var : eRes.getParameters()) {
          params.put(var.getName(), new JSONObject(var.toJSONString()));
        }
        
        for (EVariable var : eRes.getIndicators()) {
          indicators.put(var.getName(), new JSONObject(var.toJSONString()));
        }
        
        JSONObject curRD = new JSONObject();
        curRD.put("Parameters", params);
        curRD.put("Indicators", indicators);
        curRD.put("VariableOrder", eRes.getVariableOrder());
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
