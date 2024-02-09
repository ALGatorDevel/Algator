package si.fri.algator.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.entities.DeparamFilter;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.EProject;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestCase;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.NameAndAbrev;
import si.fri.algator.entities.Variables;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.VariableType;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.tools.ATTools;

/**
 *
 * @author tomaz
 */
public class DataAnalyser {

  public static final String operators = "\\+|\\-|\\*|/|=";

  /**
   * for all Parameters in filter, method checks the corresponding parameter in
   * the result; if the values doesn't match, return false, else return true
   * true
   *
   * @return
   */
  public static boolean parametersMatchFilter(Variables params, Variables filter) {
    if (filter == null) {
      return true;
    }

    try {
      for (EVariable refPar : filter) {
        Object refVal = refPar.get(EVariable.ID_Value);

        EVariable param = params.getVariable((String) refPar.getName());
        Object value = param.get(EVariable.ID_Value);

        if (!value.equals(refVal)) {
          return false;
        }
      }
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  /**
   * Method selects among all the data given in algResults only those lines that
   * meet given condidiotn.
   *
   * @param filter in the result returned by this method only the lines with the
   * value of fileds equal to the values in this parameterset will be perserved;
   * i.e. for all Parameters in filter method checks the corresponding parameter
   * in the result; if the values doesn't match, result will be skipped
   */
  public static ArrayList<Variables> selectData(HashMap<String, ArrayList<Variables>> algResults, Variables join,
          Variables filter, Variables testFields, Variables resultFields) {

    ArrayList<Variables> result = new ArrayList();
    Set<String> algKeys = algResults.keySet();

    for (String alg : algKeys) {
      for (Variables ps : algResults.get(alg)) {
        if (parametersMatchFilter(ps, filter)) {
          result.add(ps);
        }
      }
    }
    return result;
  }

  private static ResultPack getResultPack(ETestCase eTestCase, EResult eResultDesc, EResult eResultDesc0, EResult eResultDesc1) {
    ResultPack resPack = new ResultPack();

    Variables paramsAndIndicators = resPack.resultDescription.getVariables();

    // add the current resultdescription parameters to the resPack resultDescription parameterset
    paramsAndIndicators.addVariables(eTestCase.getParameters(), false);
    paramsAndIndicators.addVariables(eResultDesc.getVariables(), false);
    paramsAndIndicators.addVariables(eResultDesc0.getVariables(), false);
    paramsAndIndicators.addVariables(eResultDesc1.getVariables(), false);
    return resPack;
  }

  public static ReadResultsInitData readResultsInit(Project project, MeasurementType measurement) {

    if (project == null) {
      return null;
    }

    String resDescFilename = ATGlobal.getRESULTDESCfilename(project.getProjectRoot(), project.getName(), measurement);
    EResult eResultDesc = new EResult(new File(resDescFilename));

    Variables resultPS = Variables.join(project.getTestCaseDescription().getParameters(), eResultDesc.getVariables());

    // test parameters are only defined in EM file
    String[] testOrder = project.getTestCaseDescription().getInputParameters();
    if (testOrder == null) {
      testOrder = new String[0];
    }

    String[] resultOrder = eResultDesc.getStringArray(EResult.ID_IndOrder);

    return new ReadResultsInitData(resultOrder, testOrder, resultPS, eResultDesc);

  }

  /**
   * Method reads a file results[.ext] (where [.ext] is measurement dependant)
   * and appends results to the resPack. The key of a result in resPack.results
   * is tesset-test. If a result already exists in resPack.results, parameters
   * are appended to this resultset, otherwise a new resultSet is generated and
   * inserted into the map.
   */
  public static void readResults(ResultPack resPack, Project project,
          String algorithm, String testset, MeasurementType measurement, String computerID, ReadResultsInitData initData) {

    ArrayList<String> lines;

    String resFileName;
    // če imam eksplicitno podano ime racunalnika, vem, kje moram iskati rezultate ...
    if (computerID != null && !computerID.isEmpty()) {
      resFileName = ATGlobal.getRESULTfilename(project.getProjectRoot(), algorithm, testset, measurement, computerID);
    } // ... sicer pa poiščem najbolj primerno datoteko
    else {
      resFileName = ATTools.getTaskResultFileName(project, algorithm, testset, measurement.getExtension());
    }

    lines = new ArrayList<>();
    File resFile = new File(resFileName);

    try (Scanner sc = new Scanner(resFile)) {
      while (sc.hasNextLine()) {
        String line = sc.nextLine();
        if (line == null) {
          line = "";
        }
        lines.add(line);
      }
    } catch (Exception e) {
      ATLog.log("Can't read results: " + e, 1);
    }

    readResults(resPack, project, algorithm, testset, computerID, initData, lines);
  }

  /**
   * Method reads a file results[.ext] (where [.ext] is measurement dependant)
   * and appends results to the resPack. The key of a result in resPack.results
   * is tesset-test. If a result already exists in resPack.results, parameters
   * are appended to this resultset, otherwise a new resultSet is generated and
   * inserted into the map.
   */
  public static void readResults(ResultPack resPack, Project project,
          String algorithm, String testset,
          String computerID, ReadResultsInitData initData, ArrayList<String> lines) {

    Variables resultPS = initData.resultPS;
    ArrayList<String> resultOrder = initData.resultOrder;
    ArrayList<String> testOrder = initData.testOrder;

    String delim = ATGlobal.DEFAULT_CSV_DELIMITER;

    for (String line : lines) {
      JSONObject res  = new JSONObject();
      try {res = new JSONObject(line);} catch (Exception e) {}
      
      Variables algPS = resultPS.copy();

      // sets the value of default parameters
      String curTestset = res.optString(EResult.tstParName, testset);
      String testName   = res.optString(EResult.instanceIDParName, "test0");
      algPS.getVariable(EResult.algParName).       set(EVariable.ID_Value, res.optString(EResult.algParName, algorithm));
      algPS.getVariable(EResult.tstParName).       set(EVariable.ID_Value, curTestset);
      algPS.getVariable(EResult.instanceIDParName).set(EVariable.ID_Value, testName);
      algPS.getVariable(EResult.passParName).      set(EVariable.ID_Value, res.optString(EResult.passParName, "?"));

      ArrayList<String> trOrder = new ArrayList(testOrder); trOrder.addAll(resultOrder);
      for (String param : trOrder) {
        EVariable tP = algPS.getVariable(param);
        
        if (tP != null) {
          Object value = res.opt(tP.getName());

          if (value != null) {
            tP.set(EVariable.ID_Value, value);
          } else {
            tP.set(EVariable.ID_Value, tP.getType().getDefaultValue());
          }
        }

      }


      // add this ParameterSet to the ResultPack
      String key = curTestset + "-" + testName;
      Variables ps = resPack.getResult(key);
      // If ParameterSet for this testset-test doesn't exist ...
      if (ps == null) {
        // ... append a new testset to the map
        resPack.putResult(key, algPS);
      } else {
        // ... otherwise, add new parameters to existing parameterset
        ps.addVariables(algPS, false);
      }
    }
  }

  public static DeparamFilter[] getDeparamFilters(String[] filter) {
    if (filter == null) {
      return new DeparamFilter[0];
    }

    // filters without @()
    String[] clearFilter = new String[filter.length];

    Pattern pattern = Pattern.compile("(.*)@\\((.*)\\)");
    String range = "";

    for (int i = 0; i < filter.length; i++) {
      String curFilter = filter[i];
      if (filter[i].contains("@(")) {
        Matcher matcher = pattern.matcher(filter[i]);
        if (matcher.find()) {
          curFilter = matcher.group(1);
          range = matcher.group(2);
        }
      }
      clearFilter[i] = curFilter;
    }

    ArrayList<DeparamFilter> deFilterList = new ArrayList<>();
    if (range.isEmpty()) {
      DeparamFilter deFilter = new DeparamFilter(1, clearFilter);
      deFilterList.add(deFilter);
    } else {
      int iFrom, iTo, iStep;
      String rangeParts[] = range.split(",");
      try {
        iFrom = Integer.valueOf(rangeParts[0]);
        iTo = Integer.valueOf(rangeParts[1]);
        iStep = Integer.valueOf(rangeParts[2]);
      } catch (Exception e) {
        iFrom = iTo = iStep = 1;
      }
      for (int i = iFrom; i <= iTo; i = i + iStep) {
        String[] thisFilter = clearFilter.clone();
        for (int iTab = 0; iTab < thisFilter.length; iTab++) {
          thisFilter[iTab] = thisFilter[iTab].replaceAll("\\$1", Integer.toString(i));
        }
        deFilterList.add(new DeparamFilter(i, thisFilter));
      }
    }
    return (DeparamFilter[]) deFilterList.toArray(new DeparamFilter[1]);
  }

  public static String getFilterHeaderName(String[] filter) {
    return "#";
  }

  /**
   * Gets the query result as table of values. The input query can be a) query
   * written in json format and b) filename of a query written in a file. If
   * query is json, EQuery is generated and result is returned: otherwise, if
   * query is filename, method checks the file with name queryName+parameters.
   * If file exists and is fresh (never than project configuration files),
   * method returns its content, else it runs a query, writes the result to file
   * and returns the result.
   */
  public static String getQueryResultTableAsString(String projectname, String query, String[] params, String computerID) {
    if (projectname == null || projectname.isEmpty() || query == null || query.isEmpty()) {
      return "";
    }

    if (query.startsWith("{")) {
      EProject project = new EProject(new File(ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectname)));
      EQuery eQuery = new EQuery(query, params);
      return runQuery(project, eQuery, computerID).toString();
    } else {
      try {
        String queryname = query;
        String projectRoot = ATGlobal.getPROJECTroot(ATGlobal.getALGatorDataRoot(), projectname);
        {
          TableData td = runQuery(projectname, queryname, params, computerID);
          String result = td.toString();
          return result;
        }
      } catch (Exception e) {
        return "";
      }
    }
  }

  /**
   * Returns an array of entities (algorithms or testsets) used in thus query.
   * If a star ("*") is in the list of query's entity, method returns all
   * project's entities, otherwise it returns only entities listed in query.
   */
  private static String[] getQueryEntities(EProject project, EQuery query, String queryEntityID, String projectEntityID) {
    return getQueryEntities(query, queryEntityID, project.getStringArray(projectEntityID));
  }

  private static String[] getQueryEntities(EQuery query, String queryEntityID, String[] allEntities) {
    return getQueryEntities(query, queryEntityID, allEntities, "*");
  }

  private static String[] getQueryEntities(EQuery query, String queryEntityID, String[] allEntities, String asterisk) {
    String[] etts = query.getStringArray(queryEntityID);

    boolean containsAll = false; // is "*" in the list of entities?
    for (int i = 0; i < etts.length; i++) {
      if (etts[i].startsWith(asterisk)) {
        containsAll = true;
        break;
      }
    }
    if (containsAll) { // merge both - allParameters and current parameters
      String[] newEtts = new String[etts.length + allEntities.length];
      System.arraycopy(allEntities, 0,  newEtts, 0, allEntities.length);
      System.arraycopy(etts,        0 , newEtts, allEntities.length, etts.length); 
      
      // remove the asterisk and the duplicates
      ArrayList<String> aEtts = new ArrayList<>();
      for (String ett : newEtts) 
        if (!asterisk.equals(ett) && !aEtts.contains(ett))
          aEtts.add(ett);
      etts = aEtts.toArray(new String[]{});              
    }

    return etts;
  }

  public static TableData runQuery(String projectname, String queryname, String computerID) {
    return runQuery(projectname, queryname, null, computerID);
  }

  public static TableData runQuery(String projectname, String queryname, String[] params, String computerID) {
    EProject project = new EProject(new File(ATGlobal.getPROJECTfilename(ATGlobal.getALGatorDataRoot(), projectname)));
    File qFIle = new File(ATGlobal.getQUERYfilename(project.getProjectRootDir(), queryname));
    EQuery query = new EQuery(qFIle, params);

    return runQuery(project, query, computerID);
  }

  public static TableData runQuery(EProject project, EQuery query, String computerID) {
    return runQuery(project, query, computerID, null);
  }

  /**
   * Methos runs a given query. For NO_COUNT queries it calls runQuery_NO_COUNT
   * once, while for COUNT queries runQuery_NO_COUNT is called n times (n=number
   * of algorithm selected in query) and the results are joint into a single
   * tableData
   */
  public static TableData runQuery(EProject project, EQuery query, String computerID, Map<String, TableData> queryResults) {
    if (!query.isCount()) {
      return runQuery_NO_COUNT(project, query, computerID, queryResults);
    } else {

      TableData result = new TableData();

      String algorithms[] = getQueryEntities(project, query, EQuery.ID_Algorithms, EProject.ID_Algorithms);

      String[] origQueryFilter = query.getStringArray(EQuery.ID_Filter);

      // header
      result.header.add(getFilterHeaderName(origQueryFilter));

      for (String algorithm : algorithms) {
        result.header.add(new NameAndAbrev(algorithm).getAbrev() + ".COUNT");
      }

      //data
      DeparamFilter[] filters = getDeparamFilters(origQueryFilter);
      for (DeparamFilter curFilter : filters) {
        ArrayList line = new ArrayList();

        // first column of result = the value of the parameter
        line.add(curFilter.getParamValue());

        for (String algorithm : algorithms) {
          String[] enAlgoritemArray = {algorithm};
          JSONArray enALgoritemJArray = new JSONArray(enAlgoritemArray);
          query.set(EQuery.ID_Algorithms, enALgoritemJArray);
          query.set(EQuery.ID_Filter, new JSONArray(curFilter.getFilter()));
          TableData dataForAlg = runQuery_NO_COUNT(project, query, computerID);
          int algCount = (dataForAlg != null && dataForAlg.data != null) ? dataForAlg.data.size() : 0;
          line.add(algCount);
        }
        result.data.add(line);
      }
      return result;
    }
  }

  public static TableData runQuery_NO_COUNT(EProject eProject, EQuery query, String computerID) {
    return runQuery_NO_COUNT(eProject, query, computerID, null);
  }

  public static TableData runQuery_NO_COUNT(EProject eProject, EQuery query, String computerID, Map<String, TableData> queryResults) {
    TableData td = null;

    if (td != null && (queryResults == null || queryResults.size() == 0)) {
      TableData tdCache = new TableData();
      tdCache.header = td.header;
      tdCache.data = (ArrayList<ArrayList<Object>>) td.data.clone();
      td = tdCache;
    } else {
      td = new TableData();

      String queryComputerID = query.getField(EQuery.ID_ComputerID);
      // če ima query definiran computerID, potem se uporabi tega
      if (queryComputerID != null && !queryComputerID.isEmpty()) {
        computerID = queryComputerID;
      } else // če comupterID v poizvedbi ni naveden, pa uporabim podani computerID; če je ta prazen pa thisComputerID
      if (computerID == null || "".equals(computerID)) {
        computerID = ATGlobal.getThisComputerID();
      }

      String algorithms[] = getQueryEntities(eProject, query, EQuery.ID_Algorithms, EProject.ID_Algorithms);
      NameAndAbrev[] algs = query.getNATabFromJSONArray(algorithms);

      String tsts[] = getQueryEntities(eProject, query, EQuery.ID_TestSets, EProject.ID_TestSets);
      NameAndAbrev[] testsets = query.getNATabFromJSONArray(tsts);

      boolean combinedQuery = queryResults != null && queryResults.size() > 0;
      if (combinedQuery) {
        for (String ts : tsts) {
          if (!queryResults.containsKey(ts)) {
            combinedQuery = false;
            break;
          }
        }
      }

      if (queryResults == null && (algs == null || algs.length < 1 || testsets == null || testsets.length < 1)) {
        return td;
      }

      HashMap<String, ResultPack> results = new HashMap<>();

      String projectRootDir = eProject.getProjectRootDir();
      String data_root = ATGlobal.getDataRootFromProjectRoot(projectRootDir);
      Project project = new Project(data_root, eProject.getName());

      ArrayList errors = new ArrayList();
      HashMap resultDescriptions = new HashMap();
      Project.readResultDescriptions(eProject.getProjectRootDir(), eProject.getName(), resultDescriptions, errors);

      String[] allINParamaters = Project.getTestParameters(project.getTestCaseDescription());
      String inParameters[] = getQueryEntities(query, EQuery.ID_Parameters, allINParamaters);
      NameAndAbrev[] inPars = query.getNATabFromJSONArray(inParameters);

      // calculate EM parameters ...
      String[] allEMParamaters = Project.getIndicators(resultDescriptions, MeasurementType.EM);
      String emParameters[] = getQueryEntities(query, EQuery.ID_Indicators, allEMParamaters, "*EM");
      NameAndAbrev[] emPars = query.getNATabFromJSONArray(emParameters);
      // ... CNT parameters ...
      String[] allCNTParamaters = Project.getIndicators(resultDescriptions, MeasurementType.CNT);
      String cntParameters[] = getQueryEntities(query, EQuery.ID_Indicators, allCNTParamaters, "*CNT");
      NameAndAbrev[] cntPars = query.getNATabFromJSONArray(cntParameters);
      // ... JVM parameters ...
      String[] allJVMParamaters = Project.getIndicators(resultDescriptions, MeasurementType.JVM);
      String jvmParameters[] = getQueryEntities(query, EQuery.ID_Indicators, allJVMParamaters, "*JVM");
      NameAndAbrev[] jvmPars = query.getNATabFromJSONArray(jvmParameters);
      // ... and join all together
      int n = 0;
      ArrayList<NameAndAbrev> outParsAL = new ArrayList<>();
      for (NameAndAbrev emParam : emPars) {
        if (!outParsAL.contains(emParam) && !emParam.getName().startsWith("*")) {
          outParsAL.add(emParam);
        }
      }
      for (NameAndAbrev cntParam : cntPars) {
        if (!outParsAL.contains(cntParam) && !cntParam.getName().startsWith("*")) {
          outParsAL.add(cntParam);
        }
      }
      for (NameAndAbrev jvmParam : jvmPars) {
        if (!outParsAL.contains(jvmParam) && !jvmParam.getName().startsWith("*")) {
          outParsAL.add(jvmParam);
        }
      }

      ReadResultsInitData initDataEM = readResultsInit(project, MeasurementType.EM);
      ReadResultsInitData initDataCNT = readResultsInit(project, MeasurementType.CNT);
      ReadResultsInitData initDataJVM = readResultsInit(project, MeasurementType.JVM);

      List<String> sortedPars = new LinkedList<>();

      for (NameAndAbrev alg : algs) {
        ResultPack rPack = getResultPack(project.getTestCaseDescription(), initDataEM.eResultDesc, initDataCNT.eResultDesc, initDataJVM.eResultDesc);
        Variables resultPS = new Variables();
        if (queryResults != null) {
          readResults(queryResults, tsts, resultPS, alg, sortedPars, rPack);
        }
        for (NameAndAbrev ts : testsets) {
          if (queryResults == null || !queryResults.containsKey(ts.getAbrev())) {
            ATLog.disableLog(); // to prevent error messages on missing description files
            readResults(rPack, project, alg.getName(), ts.getName(), MeasurementType.EM, computerID, initDataEM);
            readResults(rPack, project, alg.getName(), ts.getName(), MeasurementType.CNT, computerID, initDataCNT);
            readResults(rPack, project, alg.getName(), ts.getName(), MeasurementType.JVM, computerID, initDataJVM);
            ATLog.enableLog();
          }
        }
        results.put(alg.getName(), rPack);
      }

      NameAndAbrev[] outPars = new NameAndAbrev[outParsAL.size()];

      outParsAL.toArray(outPars);

      // ver 1.0: the order of testset-test key is obtained from the first algorithm (this order
      // should be the same for all algorithms, therefore the selection of the algorithms  is arbitrary).
      // ver 2.0 (jan2016): The above statement is not true! If the results of the first algorithm are corrupted,
      // keyOrder can be null. Therefore, in this version we take the keyOrder with maximum number of keys.
      //ArrayList<String> keyOrder = null;
      //NameAndAbrev alg0 = null;
      //for (NameAndAbrev alg : algs) {
      //  if (keyOrder == null || keyOrder.size() < results.get(alg.getName()).keyOrder.size()) {
      //    alg0 = alg;
      //    keyOrder = results.get(alg0.getName()).keyOrder;
      //  }
      //}
      // ver 3.0: in new version some results file might contain tests that other don't. Therefore
      // we build keyOrder as a union of all test orders
      ArrayList<String> keyOrder = new ArrayList<>();
      for (NameAndAbrev alg : algs) {
        if (results.get(alg.getName()) != null && results.get(alg.getName()).keyOrder != null) {
          ArrayList<String> tmpKeyOrder = new ArrayList<>(results.get(alg.getName()).keyOrder);
          tmpKeyOrder.removeAll(keyOrder);
          keyOrder.addAll(tmpKeyOrder);
        }
      }

      // add headers for default test parameters
      td.header.add(EResult.testNoParName);  // ID of a table row

      td.header.add(EResult.tstParName);

      td.header.add(EResult.instanceIDParName);

      td.header.add(EResult.passParName);

      // Input (test) parameters + 4 default parameters (TestNo, TestSet, TestID, pass)
      td.numberOfInputParameters = inPars.length + 4;

      for (NameAndAbrev inPar : inPars) {
        td.header.add(inPar.getAbrev());
      }
      for (NameAndAbrev outPar : outPars) {
        String abrev = outPar.getAbrev();
        String[] exp = abrev.split(operators);
        for (NameAndAbrev alg : algs) {
          if (exp.length > 1) {
            td.header.add(alg.getAbrev() + ".(" + abrev + ")");
          } else {
            td.header.add(alg.getAbrev() + "." + abrev);
          }
        }
      }

      sortedPars.addAll(Arrays.asList(allINParamaters));
      sortedPars.addAll(Arrays.asList(allEMParamaters));
      sortedPars.addAll(Arrays.asList(allCNTParamaters));
      sortedPars.addAll(Arrays.asList(allJVMParamaters));

      Collections.sort(sortedPars,
              new LengthComparator());
      Collections.reverse(sortedPars);

      int testNUM = 0;
      for (String key : keyOrder) {
        testNUM++;
        ArrayList<Object> line = new ArrayList<>();

        // fina an algorithm that contains values for this key
        NameAndAbrev alg0 = null;
        for (NameAndAbrev alg : algs) {
          if (results.get(alg.getName()).getResult(key) != null) {
            alg0 = alg;
            break;
          }
        }

        if (alg0 != null) {
          Variables ps = results.get(alg0.getName()).getResult(key);

          // add values for 3 default test parameters
          line.add(testNUM);
          line.add(ps.getVariable(EResult.tstParName).get(EVariable.ID_Value));
          line.add(ps.getVariable(EResult.instanceIDParName).get(EVariable.ID_Value));
          line.add(ps.getVariable(EResult.passParName).get(EVariable.ID_Value));

          //line.add(testNUM);
          for (NameAndAbrev inPar : inPars) {
            Object value;
            try {
              String pName = inPar.getName();

              // if param is like tc_PROPS.Type, we have to split TP_PROPS and get only "Type" part of it ....
              if (pName.startsWith("TC_PROPS")) {
                value = "?";
                JSONObject tcProps = (JSONObject) ps.getVariable("TC_PROPS").get(EVariable.ID_Value);
                if (pName.contains(".")) {
                  String prop = pName.split("[.]")[1];
                  value = tcProps.opt(prop);
                                    
                  // check if parameter has a type defined
                  String parType = inPar.getType();
                  if (parType != null) {
                    switch (parType) {
                      case "int":
                        try {
                          value = Integer.parseInt(value.toString());
                        } catch (Exception e) {
                        }
                        break;
                      case "double":
                        try {
                          value = Double.parseDouble(value.toString());
                        } catch (Exception e) {
                        }
                        break;
                      case "long":
                        try {
                          value = Long.parseLong(value.toString());
                        } catch (Exception e) {
                        }
                        break;
                    }
                  }
                } else {
                  value = tcProps;
                }

                // ... else (for every other param)
              } else {
                EVariable parameter = ps.getVariable(pName);
                value = parameter.getValue();
              }
            } catch (Exception e) {
              value = "?";
            }
            line.add(value);
          }
        }

        // scans outParams and find its value for every algorithm-testset-test
        for (NameAndAbrev outPar : outPars) {
          String name = outPar.getName();
          String[] exp = name.split(operators);
          if (exp.length > 1) {
            name = AlgInterpreter.prepareExpression(name);
          }
          for (NameAndAbrev alg : algs) {
            Object value = "?";
            if (results.get(alg.getName()).getResult(key) != null) {
              try {
                if (exp.length > 1) {
                  value = getCalcField(name, results, sortedPars, key, algs, alg);
                } else {
                  Variables ps2 = results.get(alg.getName()).getResult(key);
                  EVariable parameter = ps2.getVariable(name);
                  value = parameter.getValue();
                }
              } catch (Exception e) {
                // to ne gre za napako ampak za neobstoječ parameter, kar pa 
                // rešujem z "value = ?"
                // System.out.println(e);
              }
            }
            line.add(value);
          }
        }

        td.data.add(line);
      }

      if (queryResults == null) {
        TableData tdCache = new TableData();
        tdCache.header = td.header;
        tdCache.data = (ArrayList<ArrayList<Object>>) td.data.clone();
      }
    }

    return runQuery_NO_COUNT(td, eProject, query, computerID);
  }

  private static void readResults(Map<String, TableData> queryResults, String[] tsts, Variables resultPS, NameAndAbrev alg, List<String> sortedPars, ResultPack rPack) {
    for (Entry<String, TableData> entry : queryResults.entrySet()) {
      boolean existsTs = false;
      for (int i = 0; i < tsts.length; i++) {
        if (tsts[i].equals(entry.getKey())) {
          existsTs = true;
          break;
        }
      }
      if (!existsTs) {
        continue;
      }
      TableData qTd = entry.getValue();
      boolean addCalcParams = true;
      ArrayList<String> resultOrder = new ArrayList<>();
      resultPS.addVariable(new EVariable(EResult.algParName, VariableType.STRING, ""));
      resultPS.addVariable(new EVariable(EResult.tstParName, VariableType.STRING, ""));
      resultPS.addVariable(new EVariable(EResult.instanceIDParName, VariableType.STRING, ""));
      resultPS.addVariable(new EVariable(EResult.passParName, VariableType.STRING, ""));
      for (int i = EResult.FIXNUM; i < qTd.header.size(); i++) {
        String varName = qTd.header.get(i);
        if (varName.startsWith(alg.getAbrev() + ".")) {
          varName = varName.replace(alg.getAbrev() + ".", "");
        }
        resultOrder.add(varName);
        resultPS.addVariable(new EVariable(varName, VariableType.STRING, ""));
      }
      for (ArrayList<Object> line : qTd.data) {
        Variables algPS = resultPS.copy();
        String testSet = (String) line.get(1);
        String testName = (String) line.get(2);
        String pass = (String) line.get(3);

        // sets the value of default parameters
        algPS.getVariable(EResult.algParName).set(EVariable.ID_Value, alg.getName());
        algPS.getVariable(EResult.tstParName).set(EVariable.ID_Value, testSet);
        algPS.getVariable(EResult.instanceIDParName).set(EVariable.ID_Value, testName);
        algPS.getVariable(EResult.passParName).set(EVariable.ID_Value, pass);

        // sets the value of result parameters
        for (String ind : resultOrder) {
          int lineFiledsPos = EResult.FIXNUM;
          while (lineFiledsPos < qTd.header.size()
                  && !qTd.header.get(lineFiledsPos).equals(ind)
                  && !qTd.header.get(lineFiledsPos).equals(alg.getAbrev() + "." + ind)) {
            lineFiledsPos++;
          }
          EVariable tP = algPS.getVariable(ind);
          if (tP != null) {
            if (addCalcParams) {
              if (tP.getName().contains(".")) {
                sortedPars.add(tP.getName().split("\\.")[1]);
              } else {
                sortedPars.add(tP.getName());
              }
            }
            if (lineFiledsPos < line.size()) {
              tP.set(EVariable.ID_Value, line.get(lineFiledsPos));
            } else {
              tP.set(EVariable.ID_Value, tP.getType().getDefaultValue());
            }
          }
        }
        // add this ParameterSet to the ResultPack
        String key = testSet + "-" + testName;
        Variables ps = rPack.getResult(key);
        // If ParameterSet for this testset-test doesn't exist ...
        if (ps == null) {
          // ... append a new testset to the map
          rPack.putResult(key, algPS);
        } else {
          // ... otherwise, add new parameters to existing parameterset
          ps.addVariables(algPS, false);
        }
        addCalcParams = false;
      }
    }
  }

  public static TableData runQuery_NO_COUNT(TableData td, EProject eProject, EQuery query, String computerID) {

    String[] filter = query.getStringArray(EQuery.ID_Filter);
    for (int i = 0; i < filter.length; i++) {
      try {
        td.filter(filter[i]);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    String[] groupby = query.getStringArray(EQuery.ID_GroupBy);
    for (int i = 0; i < groupby.length; i++) {
      td.groupBy(groupby[i]);
    }

    String[] sortby = query.getStringArray(EQuery.ID_SortBy);
    for (int i = 0; i < sortby.length; i++) {
      td.sort(sortby[i]);
    }

    return td;
  }

  private static Object getCalcField(String exp, HashMap<String, ResultPack> results, List<String> sortedParNames, String currentKey, NameAndAbrev[] algs, NameAndAbrev currentAlg) {
    for (NameAndAbrev alg : algs) {
      String algName = alg.getAbrev();
      if (exp.contains(algName)) {
        Variables ps2 = results.get(algName).getResult(currentKey);
        for (String outPar : sortedParNames) {
          exp = replaceVarInExp(outPar, ps2, exp, algName);
        }
      }
    }
    Variables ps2 = results.get(currentAlg.getName()).getResult(currentKey);
    for (String outPar : sortedParNames) {
      exp = replaceVarInExp(outPar, ps2, exp, "");
    }
    return AlgInterpreter.evalExpression(exp);
  }

  private static String replaceVarInExp(String name, Variables ps2, String exp, String alg) {
    if (alg == null) {
      alg = "";
    }
    if (!alg.equals("")) {
      alg = alg + ".";
    }
    EVariable parameter = ps2.getVariable(name);
    if (parameter != null) {
      Object value = parameter.getValue();
      exp = exp.replace("@" + alg + name, value.toString());
    }
    return exp;
  }

}
