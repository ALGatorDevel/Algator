package si.fri.algator.admin;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner; 
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.database.Database;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EGenerator;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestCase;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.VariableType;
import si.fri.algator.entities.Variables;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.users.UsersTools;

import static si.fri.algator.admin.Tools.copyFile;
import static si.fri.algator.admin.Tools.getSubstitutions;
import si.fri.algator.entities.Entity;

/**
 *
 * @author tomaz
 */
public class Maintenance {
  // hashmap of objects to syncronize on
  public static HashMap<String, String> synchronizators = new HashMap();
  

  /**
   * Method creates project and all of its parts (testset, algorithm, 
   * parameter, indicator, ...) and returns "0:Project created" if 
   * no error occured or error message (as returned by submethod)
   */
  public static String createAll(String username, String proj_name) {
    String msg = createProject(username, proj_name);
    try {if (msg.charAt(0)!='0') return msg;} catch (Exception e) {}
    
    String paramDesc = "{Name:N, Type:int, Meta:{Min:1, Max:1, Default:1, Step:1}}";
    msg = addParameter(username, proj_name, paramDesc, "{isInputParameter:true}");
    try {if (msg.charAt(0)!='0') return msg;} catch (Exception e) {}
    
    msg = addTestCaseGenerator(username, proj_name, "Type0", new String[]{"N"});
    try {if (msg.charAt(0)!='0') return msg;} catch (Exception e) {}

    msg = createTestset(username, proj_name, "TestSet0");
    try {if (msg.charAt(0)!='0') return msg;} catch (Exception e) {}
    
    String indDesc = "{Name:Tmin, Type:timer, Meta:{ID:0, STAT:MIN}}";
    msg = addIndicator(username, proj_name, indDesc, "");
    try {if (msg.charAt(0)!='0') return msg;} catch (Exception e) {}

    indDesc = "{Name:Check, Type:string}";
    msg = addIndicator(username, proj_name, indDesc, "");
    try {if (msg.charAt(0)!='0') return msg;} catch (Exception e) {}
    
    msg = createAlgorithm(username, proj_name, "Alg1");

    
    return "0:Project created.";
  }
  
  public static String createProject(String username, String proj_name) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projSrcFolder = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(dataroot, proj_name));
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String projConfFolder = ATGlobal.getPROJECTConfigFolder(dataroot, proj_name);
    String projDocFolder = ATGlobal.getPROJECTdoc(dataroot, proj_name);

    HashMap<String, String> substitutions = getSubstitutions(proj_name);

    System.out.println("Creating project " + proj_name);
    
    
    try {
      File projFolderFile = new File(projRoot);
      if (projFolderFile.exists()) {
        return String.format("1:Project %s already exists!\n", proj_name);
      }      
      
      projFolderFile.mkdirs();

      copyFile("templates/project.json", projConfFolder, "project.json", substitutions);

      copyFile("templates/result_em.json", projConfFolder, "result_em.json", substitutions);
      copyFile("templates/result_cnt.json", projConfFolder, "result_cnt.json", substitutions);
      copyFile("templates/result_jvm.json", projConfFolder, "result_jvm.json", substitutions);

      copyFile("templates/testcase.json", projConfFolder, "testcase.json", substitutions);

      copyFile("templates/PPPAbsAlgorithm", projSrcFolder, "ProjectAbstractAlgorithm.java", substitutions);
      copyFile("templates/PPPTestCase", projSrcFolder, "TestCase.java", substitutions);
      copyFile("templates/PPPInput", projSrcFolder, "Input.java", substitutions);
      copyFile("templates/PPPOutput", projSrcFolder, "Output.java", substitutions);
      copyFile("templates/PPPTools", projSrcFolder, "Tools.java", substitutions);

      copyFile("templates/PPP.html", projDocFolder, "project.html", substitutions);
      copyFile("templates/P_TS.html", projDocFolder, "testset.html", substitutions);
      copyFile("templates/P_TC.html", projDocFolder, "testcase.html", substitutions);
      copyFile("templates/P_AAA.html", projDocFolder, "algorithm.html", substitutions);
      copyFile("templates/P_REF.html", projDocFolder, "references.html", substitutions);

      if (Database.isDatabaseMode()) {
        UsersTools.setProjectPermissions(username, proj_name);
      }
    } catch (Exception e) {
      return String.format("2: Can not create project: " + e.toString());
    }
    return String.format("0:Project %s created.", proj_name);
  }

  public static String addParameter(String username, String proj_name) {
    Scanner sc = new Scanner(System.in);

    System.out.print("Parameter name: ");
    String name = sc.nextLine().trim();
    if (name.isEmpty()) {
      name = "name";
    }
    if (name.contains(" ")) {
      name = name.split(" ")[0];
    }

    System.out.print("Parameter description: ");
    String desc = sc.nextLine();

    String dtype = "string";
    System.out.print("Parameter type [" + dtype + "]: ");
    String type = sc.nextLine();
    if (type.isEmpty()) {
      type = dtype;
    }

    String dmeta = "{\"Min\": 1, \"Max\": 1, \"Step\": 1, \"Default\": 1}";
    System.out.print("Parameter meta [" + dmeta + "]: ");
    String meta = sc.nextLine();
    if (meta.isEmpty()) {
      meta = dmeta;
    }
    
    System.out.print("Is this an Input parameter (0 ... false, 1 ... true) [1]: ");
    String inputS = sc.nextLine();
    if (inputS.isEmpty()) inputS = "1";

    String paramDesc = String.format("{\"Name\":\"%s\", \"Description\":\"%s\", \"Type\":\"%s\", \"Meta\":%s}", name, desc, type, meta);
    
    boolean edit = false, isInput  = "1".equals(inputS);
    String opt   = String.format("{\"edit\":%s, \"isInputParameter\":%s}", edit, isInput); 
    
    return addParameter(username, proj_name, paramDesc, opt);
  }

/**
   * Adds a parameter to project proj_name. 
   * 
   * @param paramDesc ... json string with Name, Description, Type and Meta (optional) properties.
   * 
   * @param opt ... json string with "edit":true/false (edit or add parameter) 
   *                                 "isInputParamameter":true/false (is this an InputParameter?)
   * 
   * @return 0     if parameter was added succesfully, otherwise
   *         1 ... user has no rights to add parameters to this project
   *         2 ... invalid indDesc (not a jsno string?)
   *         3 ... missing or invalid name
   *         4 ... missing type
   *         5 ... invalid type
   *         6 ... indicator already exists
   *         7 ... invalid meta description (not a json?)
   *         8 ... cant edit, parameter does not exist
   * 
   * Example: paramDesc = {"Name":"Delta", "Type":"enum", "Meta":{"Values":["LO","HI","MED"]}}
   *          opt       = {"edit":true, "isInputParameter":false}
   */    
  public static String addParameter(String username, String proj_name, String paramDesc, String opt) {
    JSONObject param = new JSONObject();
    String name = "";

    
    try {
      param = new JSONObject(paramDesc);

      if (!param.has("Name") || (name = param.getString("Name")).isEmpty() || name.split(" ").length > 1) {        
        return "3:Missing or invalid name of parameter.";
      }
      if (!param.has("Type")) {        
        return "4:Missing 'Type' of parameter.";
      }
      String type = param.getString("Type");
      if (!(type.equals("string") || type.equals("int") || type.equals("double") || type.equals("enum"))) {
        return "5:Invalid 'Type' property (expecting 'int', 'string', 'double' or 'enum')";      
      }
      if (param.has("Meta")) {
        try {JSONObject meta = param.getJSONObject("Meta");} catch (Exception e) {
          return "7:Invalid Meta value (not a json?).";
        }
      }
    } catch (Exception e) {
      return "2:Invalid parameter description string (not a json?).";
    }
    
    System.out.println("Adding parameter " + name);        
    
    
    // read opts  (if opt is an invalid json string or propertis (edit 
    // or isInputParameter) are missing, the default values are used)
    boolean edit=false, isInputParameter=false;
    try {
      JSONObject optJ  = new JSONObject(opt);
      edit             = optJ.optBoolean("edit", false);
      isInputParameter = optJ.optBoolean("isInputParameter", false);
    } catch (Exception e) {}

    // all tests passed, add parameter to file
    String dataroot = ATGlobal.getALGatorDataRoot();
    ETestCase testcaseDescription = new ETestCase(
            new File(ATGlobal.getTESTCASEDESCfilename(dataroot, proj_name)));

    JSONArray parameters = new JSONArray();
    try {parameters = (JSONArray) testcaseDescription.get(ETestCase.ID_parameters);} catch (Exception e) {}
    int paramId = -1; // param already exists?
    for (int i = 0; i < parameters.length(); i++) {
      if ((parameters.get(i) instanceof JSONObject) && (name.equals(((JSONObject) parameters.get(i)).get("Name")))) {
        paramId = i; break;
      }
    }
        
    JSONArray inputParameters = new JSONArray();
    try {inputParameters = (JSONArray) testcaseDescription.get(ETestCase.ID_inputParameters);} catch (Exception e) {}
    int inputParamId = -1;
    for (int i = 0; i < inputParameters.length(); i++) {
      if (name.equals(inputParameters.get(i))) {
        inputParamId = i; break;
      }
    }
    String msg = "";
    if (edit) { //edit existing parameter
      if (paramId==-1)
        return "8:Can not edit (parameter does not exist).";
      parameters.put(paramId,param);
      if (isInputParameter) {
        if (inputParamId==-1) inputParameters.put(name);
      } else {
        if (inputParamId!=-1) inputParameters.remove(inputParamId);
      }
      msg = String.format("0:Values of parameter '%s' changed.", param.get("Name"), proj_name);
    } else {  // add parameter
      if (paramId != -1)
        return String.format("6:Parameter '%s' already exists in project %s.", name, proj_name);      
      parameters.put(param);      
      if (isInputParameter) {
        inputParameters.put(name);        
      }
      msg = String.format("0:Parameter '%s' added to project %s.", param.get("Name"), proj_name);
    }

    testcaseDescription.saveEntity();
    return msg;
  }

  public static String addIndicator(String username, String proj_name) {
    Scanner sc = new Scanner(System.in);

    System.out.print("Indicator name: ");
    String name = sc.nextLine().trim();
    if (name.isEmpty()) {
      name = "name";
    }
    if (name.contains(" ")) {
      name = name.split(" ")[0];
    }

    System.out.print("Indicator description: ");
    String desc = sc.nextLine();

    String dtype = "string";
    String type;
    while (true) {
      System.out.print("Indicator type (string, int, double, timer or counter) [" + dtype + "]: ");
      type = sc.nextLine();
      if (type.isEmpty()) {
        type = dtype;
      }
      if ((type.equals("string") || type.equals("int") || type.equals("double") || type.equals("timer") || type.equals("counter"))) {
        break;
      }
      System.out.println("Invalid value.");
    }

    String meta = "";
    if (type.equals("timer")) {
      String dTimerId = "0", timerId;
      System.out.print("Timer ID [" + dTimerId + "]: ");
      timerId = sc.nextLine();
      int tid = 0;
      try {
        tid = Integer.parseInt(timerId);
      } catch (Exception e) {
      }

      String dStat = "MIN", stat;
      while (true) {
        System.out.print("Timer STAT (MIN, MAX, AVG, SUM, FIRST, LAST, ALL or MED) [" + dStat + "]: ");
        stat = sc.nextLine();
        if (stat.isEmpty()) stat = dStat;
        if ((stat.equals("MIN") || stat.equals("MAX") || stat.equals("AVG") || stat.equals("SUM") || stat.equals("FIRST") || stat.equals("ALL") || stat.equals("LAST") || stat.equals("MED"))) {
          break;
        }
        System.out.println("Invalid value.");
      }

      meta = String.format("{\"ID\": %d, \"STAT\": \"%s\"}", tid, stat);
    }

    String paramDesc = meta.isEmpty()
            ? String.format("{\"Name\":\"%s\", \"Description\":\"%s\", \"Type\":\"%s\"}", name, desc, type)
            : String.format("{\"Name\":\"%s\", \"Description\":\"%s\", \"Type\":\"%s\", \"Meta\":%s}", name, desc, type, meta);

    boolean edit = false;
    String opt   = String.format("{\"edit\":%s}", edit);     
    
    return addIndicator(username, proj_name, paramDesc, opt);
  }

  
  // if IndicatorTest_<indName>.java already exists, method returns false, 
  // otherwise method creates this file and returns true.
  private static boolean createIndicatorTestFile(String projName, String indName) {
    String projSrcFolder = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName));
    
    // indicator test already exists?
    String indicatorTestFilename = ATGlobal.INDICATOR_TEST_OFFSET + indName + ".java";      
    boolean hasIndicatorTest = new File(projSrcFolder, indicatorTestFilename).exists();
    if (hasIndicatorTest) 
      return false;
    
    HashMap<String, String> substitutions = new HashMap<>();
    substitutions.put("<PPP>", projName);
    substitutions.put("<III>", indName);

    // create IndicatorTest_III.java file ...
    copyFile("templates/IndicatorTest_III",  projSrcFolder, indicatorTestFilename , substitutions);
    return true;
  }

  /**
   * Adds an indicator to project proj_name. 
   * @param indDesc json string with Name, Description, Type and Meta (optional) properties.
   * @return 0     if parameter was added succesfully, otherwise
   *         1 ... user has no rights to add parameters to this project
   *         2 ... invalid indDesc (not a jsno string?)
   *         3 ... missing or invalid name
   *         4 ... missing type
   *         5 ... invalid type
   *         6 ... indicator already exists
   *         7 ... missing meta description for timer
   *         8 ... invalid stat for timer
   *         9 ... invalid timer id
   *         10 .. cant edit, indicator does not exist
   */  
  public static String addIndicator(String username, String projName, String indDesc, String opt) {
    JSONObject indicator = new JSONObject();
    String type = "string", indName = "";

    try {
      indicator = new JSONObject(indDesc);

      if (!indicator.has("Name") || (indName = indicator.getString("Name")).isEmpty() || indName.split(" ").length > 1) {
        return "3:Missing or invalid name of indicator.";
      }

      if (!indicator.has("Type")) {
        return "4:Missing 'Type' of indicator.";
      }
      type = indicator.getString("Type");
      if (!(type.equals("string") || type.equals("int") || type.equals("double") || type.equals("timer") || type.equals("counter"))) {
        return "5:Invalid 'Type' property (expecting 'int', 'string', 'double', 'timer' or 'counter'";
      }
      
      if (type.equals("timer")) {
        String error7 = "7:Indicator of type 'timer' requires Meta description (json with ID and STAT).";
        if (!indicator.has("Meta")) 
          return error7;
        try {
          JSONObject metaJ = indicator.getJSONObject("Meta");
          if (!metaJ.has("STAT")) return error7;
          String stat = metaJ.getString("STAT");
          if (!(stat.equals("MIN") || stat.equals("MAX") || stat.equals("FIRST") || stat.equals("LAST") || stat.equals("MED") || stat.equals("ALL") || stat.equals("SUM")))
            return "8:Invalid STAT value.";
          if (!metaJ.has("ID")) return error7;
          try {
            int id = metaJ.getInt("ID");
          } catch (Exception e) {return "9:Invalid ID value.";}
        } catch (Exception e) {
          return error7;
        }
      }
    } catch (Exception e) {
      return "2:Invalid indicator description string (not a json?).";
    }
    
    System.out.println("Adding indicator " + indName);

    
    // read opts  (if opt is an invalid json string or property 
    // edit is missing, the default value is used)
    boolean edit=false;
    try {
      JSONObject optJ  = new JSONObject(opt);
      edit             = optJ.optBoolean("edit", false);
    } catch (Exception e) {}
    

    // all tests passed, add indicator to file
    String projRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projName);
    MeasurementType mt = "counter".equals(type) ? MeasurementType.CNT : MeasurementType.EM;
    EResult resultDescription = new EResult(new File(ATGlobal.getRESULTDESCfilename(projRoot, projName, mt)));

    JSONArray indicators = new JSONArray();
    try {indicators = (JSONArray) resultDescription.get(EResult.ID_indicators);} catch (Exception e) {}

    int indIdx = -1;
    for (int i = 0; i < indicators.length(); i++) {
      if ((indicators.get(i) instanceof JSONObject) && (indName.equals(((JSONObject) indicators.get(i)).get("Name")))) {
        indIdx = i; break;        
      }
    }
    
    String msg = "";
    if (edit) { // edit indicator
      if (indIdx == -1)
        return "10:Can not edit, indicator does not exist.";
      indicators.put(indIdx, indicator);
      msg = String.format("0:Values of indicator '%s' changed.", indName, projName);
    } else { // add indicator
      if (indIdx != -1)
        return String.format("6:Indicator '%s' already exists in project %s.", indName, projName);
      
      indicators.put(indicator);

      JSONArray indicatorsOrder;
      try {indicatorsOrder = (JSONArray) resultDescription.get(EResult.ID_IndOrder);} 
      catch (Exception e) {indicatorsOrder = new JSONArray(); resultDescription.set(EResult.ID_IndOrder, indicatorsOrder);}
      indicatorsOrder.put(indName);
      msg = String.format("0:Indicator '%s' added to project %s.", indName, projName);
    }
    resultDescription.saveEntity();
    
    // for regular indicators create IndicatorTest_ind.java
    if (!(type.equals("timer") || type.equals("counter"))) {
      if (!createIndicatorTestFile(projName, indName))
        msg +=String.format(" IndicatorTest_%s.java already exists.", indName);
    }
    
    return msg;
  }
  
  /**
   * 
   * @param projectName
   * @param indicatorName
   * @param type ... "timer", "counter", or "indicator"
   * @return 
   */
  public static String removeIndicator(String projectName, String indicatorName, String type) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    
    try {  
      MeasurementType mt = "counter".equals(type) ? MeasurementType.CNT : MeasurementType.EM;
      EResult resultDescription = new EResult(new File(ATGlobal.getRESULTDESCfilename(projRoot, projectName, mt)));

      String msg = "";
      // remove from "Indicators"
      JSONArray indicators = resultDescription.getField(EResult.ID_indicators);
      if (indicators!= null) for (int i=0; i<indicators.length(); i++) {
        if (indicatorName.equals(indicators.getJSONObject(i).get(Entity.ID_NAME))) {
          indicators.remove(i); msg += "Removed from Indicators. "; break;
        }
      }      
      // remove from "IndicatorOrder"
      JSONArray indicatorOrder = resultDescription.getField(EResult.ID_IndOrder);
      if (indicatorOrder != null) for (int i=0; i<indicatorOrder.length(); i++) {
        if (indicatorName.equals(indicatorOrder.get(i))) {
          indicatorOrder.remove(i); msg += "Removed from IndicatorsOrder. ";break;
        }
      }
      resultDescription.saveEntity();
      
      if ("indicator".equals(type)) {
        // remove IndicatorTest file
        String indFilename = ATGlobal.getIndicatorTestFilename(dataroot, projectName, indicatorName);
        boolean deleted = new File(indFilename).delete();
        if (deleted) msg += "IndicatotTest file removed. ";
      }      
      if (msg.isEmpty())
        return "2:Indicator does not exist.";
      else return "0:"+msg;
    } catch (Exception e) {
      return "1:Can not remove indicator: " + e.toString();
    }
  }
  
  public static String saveIndicator(String projectName, JSONObject jIndicator, String type) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectName);
    
    String tYpe=type.isEmpty()?"Indicator":(type.toUpperCase().charAt(0)+type.substring(1));
    try {      
      MeasurementType mt = "counter".equals(type) ? MeasurementType.CNT : MeasurementType.EM;

      // parse indicator json and get Name and Code
      String indicatorName = jIndicator.optString(Entity.ID_NAME, "<non existing>");
      String code          = jIndicator.optString("Code");
      code = new String(Base64.getDecoder().decode(code));
      jIndicator.remove("Code");
      
      EResult resultDescription = new EResult(new File(ATGlobal.getRESULTDESCfilename(projRoot, projectName, mt)));

      String msg = "";
      // change indicators in Indicator array
      JSONArray indicators = resultDescription.getField(EResult.ID_indicators);
      if (indicators!= null) for (int i=0; i<indicators.length(); i++) {
        if (indicatorName.equals(indicators.getJSONObject(i).get(Entity.ID_NAME))) {
          indicators.put(i, jIndicator); msg += tYpe +" changed. "; break;
        }
      }      
      resultDescription.saveEntity();
            
      if ("indicator".equals(type)) {
        // remove IndicatorTest file
        String indFilename = ATGlobal.getIndicatorTestFilename(dataroot, projectName, indicatorName);
        File indFile = new File(indFilename);
        if (indFile.exists()) try (PrintWriter pw = new PrintWriter(indFile);) {                    
          pw.println(code);
          msg += "IndicatotTest file changed. ";
        } catch (Exception e) {}
      }      
      if (msg.isEmpty())
        return "2:"+tYpe+" does not exist.";
      else return "0:"+msg;
    } catch (Exception e) {
      return "1:Can not save "+tYpe+": " + e.toString();
    }
  }
   

  /**
   * Adds an indicator test to project projName. 
   * @param indName ... the name of indicator to be tested by this test
   * @return 0     if indicator test was added succesfully, otherwise
   *         1 ... user has no rights to add parameters to this project
   *         2 ... project does not exist
   *         3 ... indicator does not exist
   *         4 ... test for this indicator already exists
   *         5 ... error adding indicator test
   * 
   */
  public static String addIndicatorTest(String username, String projName, String indName) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projSrcFolder = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(dataroot, projName));

    if (Database.isDatabaseMode() && !UsersTools.can_user(username, "can_write", projName)) 
      return "1:User " + username + " does not have permitions to add indicator tests.";
    
    try {
      boolean projectExists = new File(ATGlobal.getPROJECTfilename(dataroot, projName)).exists();
      if (!projectExists)
        return String.format("2:Project %s does not exist.", projName);
      
      EResult resultsDescription = new EResult(new File(ATGlobal.getRESULTDESCfilename(
              ATGlobal.getPROJECTroot(dataroot, projName), projName, MeasurementType.EM)));

      // indicator exists?
      JSONArray indicators = new JSONArray();
      try {indicators = (JSONArray) resultsDescription.get(EResult.ID_indicators);} catch (Exception e) {}
      int indIdx = -1;
      for (int i = 0; i < indicators.length(); i++) 
        if ((indicators.get(i) instanceof JSONObject) && (indName.equals(((JSONObject) indicators.get(i)).get("Name")))) {
          indIdx = i; break;        
        }
      if (indIdx == -1)
        return String.format("3:Indicator %s does not exist.", indName);
      
      
      // indicator test already exists?
      if (!createIndicatorTestFile(projName, indName))
        return String.format("4:Indicator test for indicator '%s' already exists.", indName);
      else 
        return "0:Indicator test created";
    } catch (Exception e) {
      return "5:Can not add indicator: " + e.toString();
    }
  }

  /**
   * Adds a testcase generator to project projName. 
   * @param genName ... the name (type) of generator
   * @return 0     if generator was added succesfully, otherwise
   *         1 ... user has no rights to add parameters to this project
   *         2 ... project does not exist
   *         3 ... parameter does not exist
   *         4 ... generator with this name already exists
   *         5 ... error adding generator
   * 
   */  
  public static String addTestCaseGenerator(String username, String projName, String genName, String[] params) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projSrcFolder = ATGlobal.getPROJECTsrc(ATGlobal.getPROJECTroot(dataroot, projName));

    if (Database.isDatabaseMode() && !UsersTools.can_user(username, "can_write", projName)) 
      return "1:User " + username + " does not have permitions to add generators.";
    
    System.out.println("Adding test case generator " + genName);    
    
    try {
      boolean projectExists = new File(ATGlobal.getPROJECTfilename(dataroot, projName)).exists();
      if (!projectExists)
        return String.format("2:Project %s does not exist.", projName);
      
      
      ETestCase testcaseDescription = new ETestCase(
              new File(ATGlobal.getTESTCASEDESCfilename(dataroot, projName)));

      // check the existence of generator ...
      JSONArray generators = new JSONArray();
      try {
        generators = (JSONArray) testcaseDescription.get(ETestCase.ID_generators);
      } catch (Exception e) {
      }
      boolean hasGenerator = false;
      for (int i = 0; i < generators.length(); i++) {
        if (generators.get(i) instanceof JSONObject && genName.equals(((JSONObject) generators.get(i)).get(EGenerator.ID_Type))) {
          hasGenerator = true;
          break;
        }
      }
      if (hasGenerator) {
        return String.format("4:Generator of type '%s' already exists.", genName);
      }

      // ... and parameters
      String missingParameter = "";
      Variables tcParams = testcaseDescription.getParameters();
      for (String param : params) {
        if (tcParams.getVariable(param) == null) {
          missingParameter = param;
          break;
        }
      }
      if (!missingParameter.isEmpty()) {
        return String.format("3:Parameter '%s' does not exist.",  missingParameter);
      }

      HashMap<String, String> substitutions = getSubstitutions(projName);
      substitutions.put("<TTT>", genName);

      String param = "<tip> <par> = generatingParameters.getVariable(\"<PAR>\", \"\").get<Tip>Value();";
      String paramSub = "";
      for (int i = 0; i < params.length; i++) {
        String param1 = param.replace("<PAR>", params[i]);
        param1 = param1.replace("<par>", params[i].toLowerCase());

        EVariable var = tcParams.getVariable(params[i]);
        if (var == null) {
          var = new EVariable();
        }
        if (VariableType.INT.equals(var.getType())) {
          param1 = param1.replace("<tip>", "int   ").replace("<Tip>", "Int");
        } else if (VariableType.DOUBLE.equals(var.getType())) {
          param1 = param1.replace("<tip>", "double").replace("<Tip>", "Double");
        } else {
          param1 = param1.replace("<tip>", "String").replace("<Tip>", "String");
        }

        paramSub += (paramSub.isEmpty() ? "" : "\n    ") + param1;
      }
      substitutions.put("<params>", paramSub);

      // create TestCaseGenetator_TTT.java file ...
      copyFile("templates/TestCaseGenerator_TTT", projSrcFolder, "TestCaseGenerator_" + genName + ".java", substitutions);

      // and add generator description to testcase.json file
      JSONObject generator = new JSONObject();
      generator.put(EGenerator.ID_Type, genName);
      generator.put(EGenerator.ID_Desc, "");
      generator.put(EGenerator.ID_GPars, new JSONArray(params));
      generators.put(generator);

      testcaseDescription.saveEntity();
      
      return "0:Testcase generator created.";
    } catch (Exception e) {
      return "5:Can not add generator: " + e.toString();
    }
  }

  public static String createAlgorithm(String username, String proj_name, String alg_name) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String algRoot = ATGlobal.getALGORITHMroot(projRoot, alg_name);
    String algSrc = ATGlobal.getALGORITHMsrc(projRoot, alg_name);
    String algDocFolder = ATGlobal.getALGORITHMdoc(projRoot, alg_name);

    HashMap<String, String> substitutions = getSubstitutions(proj_name);
    substitutions.put("<AAA>", alg_name);

    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
      String msg = createProject(username, proj_name);
      if (msg.charAt(0)!='0') {
        return msg;
      }
    }

    System.out.println("Adding algorithm " + alg_name);
    try {

      File algFolderFile = new File(algRoot);
      if (algFolderFile.exists()) {
        return String.format("1:Algorithm %s already exists!\n", alg_name);
      }
      algFolderFile.mkdirs();

      copyFile("templates/algorithm.json", algRoot, "algorithm.json", substitutions);
      copyFile("templates/AAAAlgorithm", algSrc, "Algorithm.java", substitutions);
      copyFile("templates/AAA.html", algDocFolder, "algorithm.html", substitutions);
      copyFile("templates/A_REF.html", algDocFolder, "references.html", substitutions);

      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      ArrayList a = new ArrayList<String>(Arrays.asList(eProject.getStringArray(EProject.ID_Algorithms)));
      a.add(alg_name);
      eProject.set(EProject.ID_Algorithms, a.toArray());
      eProject.saveEntity();

      if (Database.isDatabaseMode()) {
        UsersTools.setEntityPermissions(username, proj_name, alg_name, 2);
      }
    } catch (Exception e) {      
      return String.format("2:Can not create algorithm: " + e.toString());
    }
    return "0:Algorithm added";
  }

  public static String createTestset(String username, String proj_name, String testset_name) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String projRoot = ATGlobal.getPROJECTroot(dataroot, proj_name);
    String testsRoot = ATGlobal.getTESTSroot(dataroot, proj_name);
    String testsDocFolder = ATGlobal.getTESTSdoc(dataroot, proj_name);

    HashMap<String, String> substitutions = getSubstitutions(proj_name);
    substitutions.put("<TS>", testset_name);

    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
      String msg;
      if ((msg = createProject(username, proj_name)).charAt(0)!='0') {
        return msg;
      }
    }

    System.out.println("Adding test set " + testset_name);
    try {

      File testsetFolderFile = new File(testsRoot);
      if (!testsetFolderFile.exists()) {
        testsetFolderFile.mkdirs();
      }

      File testSetFile = new File(testsRoot + File.separator + testset_name + ".json");
      if (testSetFile.exists()) {        
        return String.format("1:Testset %s already exists!", testset_name);
      }

      copyFile("templates/TS.json", testsRoot, testset_name + ".json", substitutions);
      copyFile("templates/TS.txt", testsRoot, testset_name + ".txt", substitutions);

      copyFile("templates/TS.html", testsDocFolder, testset_name + ".html", substitutions);

      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      ArrayList ts = new ArrayList<String>(Arrays.asList(eProject.getStringArray(EProject.ID_TestSets)));
      ts.add(testset_name);
      eProject.set(EProject.ID_TestSets, ts.toArray());
      eProject.saveEntity();

      if (Database.isDatabaseMode()) {
        UsersTools.setEntityPermissions(username, proj_name, testset_name, 3);
      }
    } catch (Exception e) {      
      return String.format("Can not create test set: " + e.toString());
    }
    return "0:Test set created.";
  }

  public static String createPresenter(String username, String projName, String presenterName, int type) {
    if (presenterName == null || presenterName.isEmpty()) {
      presenterName = getNextAvailablePresenterName(projName, type);
    }

    String dataroot = ATGlobal.getALGatorDataRoot();
    String projRoot = ATGlobal.getPROJECTroot(dataroot, projName);
    String presenterRoot = ATGlobal.getPRESENTERSroot(dataroot, projName);

    HashMap<String, String> substitutions = getSubstitutions(projName);
    substitutions.put("<TP>", presenterName);

    // first create project if it does not exist
    File projFolderFile = new File(projRoot);
    if (!projFolderFile.exists()) {
        return "1:Project does not exist.";
    }
    System.out.println("Adding presenter " + presenterName);
    try {

      File presenterFolderFile = new File(presenterRoot);
      if (!presenterFolderFile.exists()) {
        presenterFolderFile.mkdirs();
      }

       
      File presenterFile = new File(ATGlobal.getPRESENTERFilename(dataroot, projName, presenterName));
      if (presenterFile.exists()) {
        System.out.printf("\n Presenter %s already exists!\n", presenterName);
        return "2:Presenter with this name already exists.";
      }

      copyFile("templates/TP.json", presenterRoot, presenterName + "." + ATGlobal.AT_FILEEXT_presenter, substitutions);
      copyFile("templates/TP.html", presenterRoot, presenterName + ".html", substitutions);

      String id = EProject.ID_ProjPresenters;
      switch (type) {
        case 0:
          id = EProject.ID_MainProjPresenters;
          break;
        case 1:
          id = EProject.ID_ProjPresenters;
          break;
        case 2:
          id = EProject.ID_MainAlgPresenters;
          break;
        case 3:
          id = EProject.ID_AlgPresenters;
          break;
      }

      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, projName)));
      ArrayList<String> tp = new ArrayList(Arrays.asList(eProject.getStringArray(id)));
      tp.add(presenterName);
      eProject.set(id, tp.toArray());
      eProject.saveEntity();

    } catch (Exception e) {
      return "3:Can not create presenter: " + e.toString();
    }
    return "0:"+presenterName;
  }

  public static String getNextAvailablePresenterName(String proj_name, int type) {
    String presenterName = "Presenter";

    String dataroot = ATGlobal.getALGatorDataRoot();
    String presenterPath = ATGlobal.getPRESENTERSroot(dataroot, proj_name);
    EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));

    String prefix = "";
    switch (type) {
      case 0:
        prefix = "mp";
        break;
      case 1:
        prefix = "p";
        break;
      case 2:
        prefix = "ma";
        break;
      case 3:
        prefix = "a";
        break;
    }
        
    ArrayList<String> tp = new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_MainProjPresenters)));
    tp.addAll(new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_ProjPresenters))));
    tp.addAll(new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_MainAlgPresenters))));
    tp.addAll(new ArrayList<>(Arrays.asList(eProject.getStringArray(EProject.ID_AlgPresenters))));
    // add all files in presenter path 
    tp.addAll(Arrays.asList(new File(presenterPath).list())); 
    
    String ext = "." + ATGlobal.AT_FILEEXT_presenter;
    tp.replaceAll(x -> {return x.endsWith(ext) ? x : x+ext;});
    
    int id = 1;
    while (true) {
      presenterName = prefix + "Presenter" + (id++);
      if (!tp.contains(presenterName+ext) && !new File(presenterPath, presenterName + ext).exists()) {
        break;
      }
    }

    return presenterName;
  }

  public static String removePresenter(String proj_name, String presenter_name) {
    String dataroot = ATGlobal.getALGatorDataRoot();
    String presenterRoot = ATGlobal.getPRESENTERSroot(dataroot, proj_name);

    try {
      EProject eProject = new EProject(new File(ATGlobal.getPROJECTfilename(dataroot, proj_name)));
      String[] presIDs = new String[]{EProject.ID_MainProjPresenters, EProject.ID_ProjPresenters, EProject.ID_MainAlgPresenters, EProject.ID_AlgPresenters};
      for (String presID : presIDs) {
        ArrayList<String> tp = new ArrayList<String>(Arrays.asList(eProject.getStringArray(presID)));
        if (tp.contains(presenter_name)) {
          tp.remove(presenter_name);
          eProject.set(presID, tp.toArray());
          eProject.saveEntity();

          new File(presenterRoot, presenter_name + "." + ATGlobal.AT_FILEEXT_presenter).delete();
          new File(presenterRoot, presenter_name + ".html").delete();

          return "0:Presenter " + presenter_name + " sucessfully removed.";
        }
      }
      return "2:Presenter " + presenter_name + " does not exist.";

    } catch (Exception e) {
      return "3:Can not remove presenter: " + e.toString();
    }
  }

  /**
   * Returns: list of all projects (if projectName.isEmpty), info about project
   * (if projectName is defined and algorithmName.isEmpty) and info about
   * algorithm (if both, projectName and algorithmName are defined).
   *
   * @param projectName
   * @param algorithmName
   * @return
   */
  public static String getInfo(String projectName, String algorithmName, boolean extended) {
    if (projectName.isEmpty()) {             // list of projects
      JSONObject projInfo = Project.getProjectsAsJSON();

      return projInfo.toString(2);
    } else if (algorithmName.isEmpty()) {    // project info
      Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);
      JSONObject projInfo = new JSONObject();

      projInfo.put("Name", project.getName());

      // list of algorithms
      TreeMap<String, EAlgorithm> algs = project.getAlgorithms();
      JSONArray jaA = new JSONArray();
      for (String algName : algs.keySet()) {
        jaA.put(algName);
      }
      projInfo.put("Algorithms", jaA);

      // list of algorithms
      TreeMap<String, ETestSet> testsets = project.getTestSets();
      JSONArray jaS = new JSONArray();
      for (String testsetName : testsets.keySet()) {
        jaS.put(testsetName);
      }
      projInfo.put("TestSets", jaS);

      if (extended) {
        JSONArray mpp = new JSONArray();

        projInfo.put("MainProjPresenters", new JSONArray(project.getEProject().getStringArray(EProject.ID_MainProjPresenters)));
        projInfo.put("ProjPresenters", new JSONArray(project.getEProject().getStringArray(EProject.ID_ProjPresenters)));
        projInfo.put("MainAlgPresenters", new JSONArray(project.getEProject().getStringArray(EProject.ID_MainAlgPresenters)));
        projInfo.put("AlgPresenters", new JSONArray(project.getEProject().getStringArray(EProject.ID_AlgPresenters)));

        ETestCase eTestCase = project.getTestCaseDescription();

        HashMap<MeasurementType, EResult> rDesc = project.getResultDescriptions();
        JSONObject jrDesc = new JSONObject();
        for (MeasurementType mType : rDesc.keySet()) {
          if (mType.equals(MeasurementType.JVM)) {
            continue;
          }

          EResult eRes = rDesc.get(mType);
          JSONObject params = new JSONObject();
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
      }

      return projInfo.toString(2);
    } else {                                 // algorithm info
      Project project = new Project(ATGlobal.getALGatorDataRoot(), projectName);
      if (project == null) {
        return "";
      }

      EAlgorithm alg = project.getAlgorithms().get(algorithmName);
      if (alg == null) {
        return "";
      }
      return alg.toJSONString();
    }
  }
}
