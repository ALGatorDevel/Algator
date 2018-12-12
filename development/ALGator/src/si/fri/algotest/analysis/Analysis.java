package si.fri.algotest.analysis;

import java.io.File;
import java.util.ArrayList;
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
import si.fri.algotest.execute.Notificator;
import si.fri.algotest.global.ATGlobal;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.global.ErrorStatus;
import si.fri.algotest.global.ExecutionStatus;
import si.fri.algotest.tools.PolyCounter;
import si.fri.algotest.tools.UniqueIDGenerator;

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

  public static ArrayList<Variables> runOne(String data_root, Project project, ArrayList<String> algorithms, Variables defaultParams, int timeLimit, int timesToExecute, MeasurementType mType, String instanceID, int whereToPrint) {
    ArrayList<Variables> results = new ArrayList<>();
    
    if (!Executor.projectMakeCompile(data_root, project.getName(), false).equals(ErrorStatus.STATUS_OK))
      return results;

    int testID = 0;
    for (String algName : algorithms) {
      if (!Executor.algorithmMakeCompile(data_root, project.getName(), algName, MeasurementType.EM, false).equals(ErrorStatus.STATUS_OK))
        continue;
            
      results.add(
        runOne(data_root, project, algName, defaultParams, timeLimit, timesToExecute, mType, instanceID, whereToPrint)
      );
    }    
    return results;
  }

  public static Variables runOne(String data_root, Project project, String algName, Variables defaultParams, int timeLimit, int timesToExecute, MeasurementType mType, String instanceID, int whereToPrint) {    
    EAlgorithm eAlgorithm = project.getAlgorithms().get(algName);
    if (eAlgorithm == null) {
      ATGlobal.verboseLevel = 1;
      ATLog.log(String.format("Algorithm '%s' does not exist.", algName), 1);

      System.exit(0);
    }

    EResult emResultDesc = project.getResultDescriptions().get(mType);
    if (emResultDesc == null) {
      emResultDesc = new EResult();
    }
    Variables parameters = emResultDesc.getParameters();

    // apply the given values of the parameters
    for (EVariable defParam : defaultParams) {
      EVariable param = parameters.getVariable(defParam.getName());
      if (param != null) {
        param.setValue(defParam.getValue());
      }
    }

    AbstractTestCase testCase = New.testCaseInstance(project).generateTestCase(parameters);
    
    Variables result = ExternalExecutor.runTestCase(project, algName, testCase, MeasurementType.EM, OtherTestsetName, 1, timesToExecute, timeLimit, null, instanceID);

    result.addVariable(EResult.getTestsetNameParameter(RunOneTestsetID));
    
    String resFilename = ATGlobal.getRESULTfilename(ATGlobal.getPROJECTroot(data_root, project.getName()), 
        algName, OtherTestsetName, mType, ATGlobal.getThisComputerID()
    );
    
    ExternalExecutor.printVariables(result, new File(resFilename), emResultDesc.getVariableOrder(), whereToPrint);
    
    return result;
  }

  public static ArrayList<Variables> getParameterLimits(String data_root, Project project, ArrayList<String> algorithms, String paramName, Variables parameters, int timeLimit, String instanceID, int whereToPrint, Notificator notificator) {
    ArrayList<Variables> results = new ArrayList<>();
        
    // get all enum parameters
    Variables enumParams = new Variables();    
    try {
      EResult emResultDesc = project.getResultDescriptions().get(MeasurementType.EM);        
      for (EVariable par : emResultDesc.getParameters()) {
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
        getParameterLimit(data_root, project, algorithms, paramName, parameters, timeLimit, curInstanceID, whereToPrint, notificator)
      );
      
      pc.nextValue();
    } 
    return results;
  }

  public static ArrayList<Variables> getParameterLimit(String data_root, Project project, ArrayList<String> algorithms, String paramName, Variables parameters, int timeLimit, String instanceID, int whereToPrint, Notificator notificator) {
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
      ExternalExecutor.printVariables(result, resultFile, emResultDesc.getVariableOrder(), whereToPrint);

      
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

    EVariable defParam = resultDesc.getParameters().getVariable(paramName);
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

    while (true) {
      param.setValue(curParamValue);
      AbstractTestCase testCase = New.testCaseInstance(project).generateTestCase(parameters);

      Variables result = ExternalExecutor.runTestCase(project, algName, testCase, MeasurementType.EM, OtherTestsetName, ++testID, 1, 2 * timeLimit, notificator, instanceID);
    
      result.addVariable(EResult.getTestsetNameParameter(FindLimitTestsetID));

      
      long time = result.getVariable(MY_TIMER).getLongValue(2 * timeLimit * 1000000);

      String status = (String) result.getVariable(EResult.passParName).getValue();
      if (time > 1000000 * timeLimit) {
        status = ExecutionStatus.KILLED.toString();
      }

      boolean killed = status.equals(ExecutionStatus.KILLED.toString()) || time > timeLimit * 1000000;

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

    return lastOKResult;
  }

}
