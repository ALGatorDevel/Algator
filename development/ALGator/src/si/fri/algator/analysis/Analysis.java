package si.fri.algator.analysis;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import si.fri.algator.entities.EAlgorithm;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.ETestCase;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.MeasurementType;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.VariableType;
import si.fri.algator.entities.Variables;
import si.fri.algator.execute.AbstractTestCase;
import si.fri.algator.execute.Executor;
import si.fri.algator.execute.ExternalExecutor;
import si.fri.algator.execute.New;
import si.fri.algator.execute.Notificator;
import si.fri.algator.global.ATGlobal;
import si.fri.algator.global.ATLog;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.global.ExecutionStatus;
import si.fri.algator.tools.PolyCounter;
import si.fri.algator.tools.UniqueIDGenerator;
import si.fri.algator.analysis.timecomplexity.Data;

/**
 *
 * @author tomaz
 */
public class Analysis {
  
  public static final String MY_TIMER              = "_Tmin_";
  
  public static String OtherTestsetName     = "Other";
  public static String RunOneTestsetID      = "_RunOne_";
  public static String FindLimitTestsetID   = "_FindLimit_";
  
  // when the relative difference between lastOK and lastKILLED value of parameter  
  // is <= precisionLevel, algorithm getParameterLimit will stop
  static final double precisionLevel        = 0.01; 

  public static ArrayList<Variables> runOne(String data_root, Project project, ArrayList<String> algorithms, Variables defaultParams, int timeLimit, int timesToExecute, MeasurementType mType, String instanceID, int whereToPrint, boolean asJSON) {
    ArrayList<Variables> results = new ArrayList<>();
    
    if (!Executor.projectMakeCompile(data_root, project.getName(), false).equals(ErrorStatus.STATUS_OK))
      return results;

    int testID = 0;
    for (String algName : algorithms) {
      if (!Executor.algorithmMakeCompile(data_root, project.getName(), algName, MeasurementType.EM, false).equals(ErrorStatus.STATUS_OK))
        continue;
            
      Variables result = runOne(data_root, project, algName, defaultParams, timeLimit, timesToExecute, mType, instanceID, whereToPrint, asJSON);
      if (result == null)
        break;
      else
        results.add(result);
    }    
    return results;
  }

  public static Variables runOne(String data_root, Project project, String algName, Variables defaultParams, int timeLimit, int timesToExecute, MeasurementType mType, String instanceID, int whereToPrint, boolean asJSON) {    
    EAlgorithm eAlgorithm = project.getAlgorithms().get(algName);
    if (eAlgorithm == null) {
      ATGlobal.verboseLevel = 1;
      ATLog.log(String.format("Algorithm '%s' does not exist.", algName), 1);

      return null;
    }

    EResult emResultDesc = project.getResultDescriptions().get(mType);
    if (emResultDesc == null) {
      emResultDesc = new EResult();
    }
    
    Variables parameters = project.getTestCaseDescription().getParameters();

    // apply the given values of the parameters
    for (EVariable defParam : defaultParams) {
      EVariable param = parameters.getVariable(defParam.getName());
      if (param != null) {
        param.setValue(defParam.getValue());
      }
    }
    
    String generatorType = "TYPE" + defaultParams.getVariable("GeneratorType", "0").getStringValue();

    URL[] urls               = New.getClassPathsForProjectAlgorithm(project, algName);
    String currentJobID      = New.generateClassloaderAndJobID(urls);    
    String testCaseClassName = project.getEProject().getTestCaseClassname();
    
    AbstractTestCase testCase = New.testCaseInstance(currentJobID, testCaseClassName).generateTestCase(generatorType, parameters);
    
    Variables result = ExternalExecutor.runTestCase(project, algName, testCase, currentJobID, MeasurementType.EM, OtherTestsetName, 1, timesToExecute, timeLimit, null, instanceID);

    result.addVariable(EResult.getTestsetNameParameter(RunOneTestsetID));
    
    String resFilename = ATGlobal.getRESULTfilename(ATGlobal.getPROJECTroot(data_root, project.getName()), 
        algName, OtherTestsetName, mType, ATGlobal.getThisComputerID()
    );
    
    ExternalExecutor.printVariables(result, new File(resFilename), EResult.getVariableOrder(project.getTestCaseDescription(), emResultDesc), whereToPrint, asJSON);
    
    New.removeClassLoader(currentJobID);
    
    return result;
  }

  public static ArrayList<Variables> getParameterLimits(String data_root, Project project, ArrayList<String> algorithms, String paramName, Variables parameters, int timeLimit, String instanceID, int whereToPrint, Notificator notificator, boolean asJSON) {
    ArrayList<Variables> results = new ArrayList<>();
        
    // get all enum parameters
    Variables enumParams = new Variables();    
    try {
      ETestCase eTestCase = project.getTestCaseDescription();
      for (EVariable par : eTestCase.getParameters()) {
        if (par != null && par.getType().equals(VariableType.ENUM)) { // non-null parameter
          // add only parameters with undefined value
          if (parameters.getVariable(par.getName()) == null) 
            enumParams.addVariable(par);
        }
      }
    } catch (Exception e) {}
    
    int numberOfEnumParams = enumParams.size();
    ArrayList<String> paramsValues[] = new ArrayList[numberOfEnumParams];
    int [] maxCounterValues          = new int      [numberOfEnumParams];
    for (int i = 0; i < numberOfEnumParams; i++) {
      paramsValues[i]     = enumParams.getVariable(i).getMetaStringArray("Values");
      maxCounterValues[i] = paramsValues[i].size()-1;
    }     
    PolyCounter pc = new PolyCounter(maxCounterValues);
    if (notificator != null) notificator.setNumberOfInstances(pc.getNumberOfValues());
    int testID = 0;
    while (!pc.overflow()) {
      int [] pcValues = pc.getValue();
      for (int i = 0; i < pcValues.length ; i++) {        
        EVariable newVarValue = 
          new EVariable(enumParams.getVariable(i).getName(), paramsValues[i].get(pcValues[i]));
        parameters.addVariable(newVarValue, true);          
      }

      String curInstanceID = instanceID + "-" + UniqueIDGenerator.formatNumber(++testID, 2);
      
      results.addAll(
        getParameterLimit(data_root, project, algorithms, paramName, parameters, timeLimit, curInstanceID, whereToPrint, notificator, asJSON)
      );
      
      pc.nextValue();
    } 
    return results;
  }

  public static ArrayList<Variables> getParameterLimit(String data_root, Project project, ArrayList<String> algorithms, String paramName, Variables parameters, int timeLimit, String instanceID, int whereToPrint, Notificator notificator, boolean asJSON) {
    Executor.projectMakeCompile(data_root, project.getName(), false);

    ArrayList<Variables> results = new ArrayList<>();
    
    // required to obtain variableOrder
    EResult emResultDesc = project.getResultDescriptions().get(MeasurementType.EM);
    if (emResultDesc == null) emResultDesc = new EResult();

    int testID=0;
    for (String algName : algorithms) {
      Executor.algorithmMakeCompile(data_root, project.getName(), algName, MeasurementType.EM, false);
     
      Variables result = getParameterLimit(project, algName, paramName, parameters, timeLimit, instanceID, whereToPrint, notificator);
      
      // print a result into a file and/or to stdout
      String resFilename = ATGlobal.getRESULTfilename(ATGlobal.getPROJECTroot(data_root, project.getName()), 
        algName, OtherTestsetName, MeasurementType.EM, ATGlobal.getThisComputerID()
      );
      File resultFile = new File(resFilename);
      ExternalExecutor.printVariables(result, resultFile, EResult.getVariableOrder(project.getTestCaseDescription(), emResultDesc), whereToPrint, asJSON);
      
      results.add(result);
    }
    
    return results;    
  }

  /**
   * Method runs algorihm for several times and try to find the value of the
   * parameter paramName for which the execution of the test case generated with
   * parameters+paramName lasts about timeLimit seconds.
   */
  public static Variables getParameterLimit(Project project, String algName, String paramName, Variables parameters, int timeLimit, String instanceID, int whereToPrint, Notificator notificator) {        
    // to control the execution time we need one additional indicator - time of execution (_time_)
    EResult resultDesc = project.getResultDescriptions().get(MeasurementType.EM);
    EVariable timer = new EVariable(MY_TIMER, VariableType.TIMER, 0);
    timer.setMeta("{\"ID\":0, \"STAT\":\"MIN\"}");
    resultDesc.additionalIndicators.addVariable(timer);

    Variables lastOKResult = null;

    EVariable defParam = project.getTestCaseDescription().getParameters().getVariable(paramName);
    if (defParam == null) defParam = new EVariable();
    
    int lastOKParamValue = defParam.getMeta("Min", 10);  // tu bi bilo treba vzeti privzeto min vrednost za parameter
    int lastKilledParamValue = 2 * lastOKParamValue; 
    int curParamValue        = 2 * lastOKParamValue; 

    long lastOKTime = 0;

    EVariable param = parameters.getVariable(paramName);
    if (param == null) {
      param = new EVariable(paramName, lastOKParamValue);
      parameters.addVariable(param);
    }

    int testID = 0;

    boolean bisectionMode = false;

    URL[] urls               = New.getClassPathsForProjectAlgorithm(project, algName);
    String currentJobID      = New.generateClassloaderAndJobID(urls);    
    String testCaseClassName = project.getEProject().getTestCaseClassname();
    
    
    while (true) {
      param.setValue(curParamValue);
      AbstractTestCase testCase = New.testCaseInstance(currentJobID, testCaseClassName).generateTestCase(ETestCase.defaultGeneratorType, parameters);

      Variables result = ExternalExecutor.runTestCase(project, algName, testCase, currentJobID, MeasurementType.EM, OtherTestsetName, ++testID, 1, timeLimit, notificator, instanceID);
      
      result.addVariable(EResult.getTestsetNameParameter(FindLimitTestsetID));
      result.addProperty(AbstractTestCase.PROPS, "t", timeLimit);            
      
      long time = result.getVariable(MY_TIMER).getLongValue(timeLimit * 1000000); 

      String status = (String) result.getVariable(EResult.passParName).getValue();
      if (time > 1000000 * timeLimit) {
        status = ExecutionStatus.KILLED.toString();
      }

      boolean killed = !status.equals(ExecutionStatus.DONE.toString()) || time > timeLimit * 1000000;

      if (ATGlobal.verboseLevel == 2) {
        System.out.println(String.format("%s=%9d, T=%9d, status=%s", paramName, curParamValue, time, status));
      }

      if (killed) {
        bisectionMode = true;
      }

      if (bisectionMode) {
        if (killed) {
          lastKilledParamValue = curParamValue;
        } else {
          lastOKParamValue = curParamValue;
          lastOKTime = time;

          lastOKResult = result.copy();
        }

        curParamValue = (lastOKParamValue + lastKilledParamValue) / 2;

      } else {
        lastOKTime = time;
        lastOKParamValue = curParamValue;
        curParamValue = 2 * curParamValue;

        lastOKResult = result.copy();
      }

      if (1.0 * Math.abs(curParamValue - lastOKParamValue) / lastOKParamValue < precisionLevel) {
        break;
      }
    }
    if (ATGlobal.verboseLevel == 2) {
      System.out.printf("Alg: %s, Param value: %d, time: %d\n", algName, lastOKParamValue, lastOKTime);
    }

    New.removeClassLoader(currentJobID);
    
    return lastOKResult;
  }
  
  public static Data getParameterLimitFullData(Project project, String algName, String paramName, Variables parameters, int timeLimit, String instanceID, int whereToPrint, Notificator notificator) {
    // to control the execution time we need one additional indicator - time of execution (_time_)
    EResult resultDesc = project.getResultDescriptions().get(MeasurementType.EM);
    EVariable timer = new EVariable(MY_TIMER, VariableType.TIMER, 0);
    timer.setMeta("{\"ID\":0, \"STAT\":\"MIN\"}");
    resultDesc.additionalIndicators.addVariable(timer);
    ArrayList<Double> xValues = new ArrayList<>();
    HashMap<Double, Double> mapping= new HashMap<>();

    Variables lastOKResult = null;

    EVariable defParam = project.getTestCaseDescription().getParameters().getVariable(paramName);
    if (defParam == null) defParam = new EVariable();

    int lastOKParamValue = defParam.getMeta("Min", 10);  // tu bi bilo treba vzeti privzeto min vrednost za parameter
    int lastKilledParamValue = 2 * lastOKParamValue;
    int curParamValue        = 2 * lastOKParamValue;

    long lastOKTime = 0;

    EVariable param = parameters.getVariable(paramName);
    if (param == null) {
      param = new EVariable(paramName, lastOKParamValue);
      parameters.addVariable(param);
    }

    int testID = 0;

    boolean bisectionMode = false;

    URL[] urls               = New.getClassPathsForProjectAlgorithm(project, algName);
    String currentJobID      = New.generateClassloaderAndJobID(urls);    
    String testCaseClassName = project.getEProject().getTestCaseClassname();    
    
    while (true) {
      param.setValue(curParamValue);
      AbstractTestCase testCase = New.testCaseInstance(currentJobID, testCaseClassName).generateTestCase(ETestCase.defaultGeneratorType, parameters);

      Variables result = ExternalExecutor.runTestCase(project, algName, testCase, currentJobID, MeasurementType.EM, OtherTestsetName, ++testID, 1, 2 * timeLimit, notificator, instanceID);

      result.addVariable(EResult.getTestsetNameParameter(FindLimitTestsetID));
      result.addProperty(AbstractTestCase.PROPS, "t", timeLimit);      

      long time = result.getVariable(MY_TIMER).getLongValue(2 * timeLimit * 1000000);

      String status = (String) result.getVariable(EResult.passParName).getValue();
      if (time > 1000000 * timeLimit) {
        status = ExecutionStatus.KILLED.toString();
      }

      boolean killed = status.equals(ExecutionStatus.KILLED.toString()) || time > timeLimit * 1000000;

      if (ATGlobal.verboseLevel == 2) {
        System.out.println(String.format("%s=%9d, T=%9d, status=%s", paramName, curParamValue, time, status));
      }
      if (status == "DONE"){
        mapping.put((double)curParamValue, (double)time);
      }

      if (killed) {
        bisectionMode = true;
      }

      if (bisectionMode) {
        if (killed) {
          lastKilledParamValue = curParamValue;
        } else {
          lastOKParamValue = curParamValue;
          lastOKTime = time;

          lastOKResult = result.copy();
        }

        curParamValue = (lastOKParamValue + lastKilledParamValue) / 2;

      } else {
        lastOKTime = time;
        lastOKParamValue = curParamValue;
        curParamValue = 2 * curParamValue;

        lastOKResult = result.copy();
      }

      if (1.0 * Math.abs(curParamValue - lastOKParamValue) / lastOKParamValue < precisionLevel) {
        break;
      }
    }
    if (ATGlobal.verboseLevel == 2) {
      System.out.printf("Alg: %s, Param value: %d, time: %d\n", algName, lastOKParamValue, lastOKTime);
    }

    // sort by increasing x values
    int n = mapping.size();
    double[] x = new double[n];
    double[] y = new double[n];
    int i = 0;
    for (double d : mapping.keySet()){
      x[i] = d;
      i++;
    }
    Arrays.sort(x);
    for (i=0; i < n; i++){
      y[i] = mapping.get(x[i]);    
    }
    
    New.removeClassLoader(currentJobID);
    
    return new Data("maxData", x,y);
  }
  

}
