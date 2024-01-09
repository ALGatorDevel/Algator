package si.fri.algator.entities;

import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.global.ErrorStatus;
import si.fri.algator.global.ExecutionStatus;

/**
 * 
 * @author tomaz
 */
public class EResult extends Entity {
  /**
   * The numnber of fileds added to every result file.
   * Currently: 4 (the name of the algorithm, the testset, the test and pass (DONE/FAILED))
   */
  public static final int FIXNUM = 5;
  public static final String algParName        = "Algorithm"; 
  public static final String tstParName        = "Testset"; 
  public static final String instanceIDParName = "InstanceID";     // Unique instance identificator of a test instance
  public static final String passParName       = "Pass";           // DONE if algorithem finished within the given time limit, FAILED otherwise
  public static final String timeStampName     = "Timestamp";      // timestamp of test end
  public static final String errorParName      = "Error";          // if an error occures, this parameter contains error message
  
  // unique sequence number of a test in a tabel (id of table row)
  public static final String testNoParName   = "ID";     

  // Entity identifier
  public static final String ID_Result   = "Result";
  
  // Fields
  public static final String ID_IndOrder        = "IndicatorsOrder";   // String []
  public static final String ID_indicators      = "Indicators";       // EVariable []
  
  
  // variables of this result
  private Variables variables;
  
  private Variables indicators;
  
  // indicators added for special purposes (for example, for evaluation process)
  public Variables additionalIndicators = new Variables();
  
  
   public EResult() {
     
     super(ID_Result, 
	 new String [] {ID_IndOrder, ID_indicators});
         set(ID_indicators, new JSONArray());
         
     export_name= false; // don't export 'name' property (since it is always 'result_*')     
  }
  
  public EResult(File fileName) {
    this();
    initFromFile(fileName);

  }
  
  public EResult(String json) {
    this();
    initFromJSON(json);
  }
  
   /**
   * Method return an String array obtained from corresponding field. 
   */
  public Variables getVariables() {
    if (variables != null) {
      variables.addVariables(additionalIndicators, true);
      return variables;
    }
    
    try {
      Variables result = new Variables();
      
      // add FIXNUM default parameters ...
      result.addVariable(getAlgorithmNameParameter("/"), true);
      result.addVariable(getTestsetNameParameter("/"), true);
      result.addVariable(getInstanceIDParameter("/"), true);
      result.addVariable(EResult.getTimestampParameter(0), true);      
      // ... and the execution status indicator
      result.addVariable(getExecutionStatusIndicator(ExecutionStatus.UNKNOWN), false);
      
      JSONArray ja = getField(ID_indicators);
      indicators = new Variables();
      if (ja != null) for (int i = 0; i < ja.length(); i++) {
	JSONObject jo = ja.getJSONObject(i);
        EVariable var = new EVariable(jo.toString());
	result.addVariable(var, true);        
        indicators.addVariable(var, true);
      }
            
      // Add all undefined indicators - default type for undefined indicators is INT
      String [] indicators = getStringArray(ID_IndOrder);
      for (String indicatorName : indicators) {
        if (result.getVariable(indicatorName) == null)
          result.addVariable(new EVariable(indicatorName, indicatorName, VariableType.INT, 0), true);
      }
      
      // add all additional variables    
      result.addVariables(additionalIndicators, true); 
      
      variables = result;      
      
      return result;
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_NOT_A_RESULTPARAMETER_ARRAY, ID_indicators);
      
      return new Variables();
    }
  }
  
  public Variables getIndicators() {
    if (variables == null) getVariables();
    return indicators;
  }
    
  /**
   * Returns a parameter that represents the algorithm name
   */
  public static EVariable getAlgorithmNameParameter(String algName) {
    return new EVariable(algParName, "Algorithm name", VariableType.STRING, algName);
  }

  /**
   * Returns a parameter that represents the testset name
   */
  public static EVariable getTestsetNameParameter(String tstName) {
    return new EVariable(tstParName, "Testset name", VariableType.STRING, tstName);
  }
  
  /**
   * Returns a indicator that represents the success of the algorithm (DONE or KILLED)
   */
  public static EVariable getExecutionStatusIndicator(ExecutionStatus status) {
    return new EVariable(passParName, "Algorithm execution status", VariableType.STRING, status.toString());
  }
  /**
   * Returns a testID paremeter
   */
  public static EVariable getInstanceIDParameter(String testID) {
    return new EVariable(instanceIDParName, "Instance identificator", VariableType.STRING, testID);
  }

  /**
   * Returns a timestamp paremeter
   */
  public static EVariable getTimestampParameter(long timestamp) {    
    return new EVariable(timeStampName, "Timestamp identificator", VariableType.INT, timestamp);
  }
  
  
  /**
   * Returns an error indicator
   */
  public static EVariable getErrorIndicator(String errorMsg) {
    return new EVariable(errorParName, "Error message", VariableType.STRING, errorMsg.replaceAll("\n", " "));
  }
  
  
/* *****************************************
 *   Static methods 
 ***************************************** 
*/
  
   /**
   * Method returns the order of the variables to be printed in the result file. The 
   * variables are returned in the following order: defulat parameters (e.q. alg. name and testset name), 
   * test parameters (ID_ParOrder order) and  result indicators in ID_IndOrder order.
   */
  public static String [] getVariableOrder(ETestCase eTestCase, EResult eResult) {
    String [] orderA = eTestCase.getInputParameters();
    String [] orderB = eResult.  getStringArray(EResult  .ID_IndOrder);

    String [] order = new String[orderA.length  + orderB.length + FIXNUM];
    
    // Add "Algorithm", "TestSet" and "Pass" parameters to the set of output parameters.
    // The number of parameters added to every result line is defined in EResult.FIXNUM
    order[0] = EResult.algParName;
    order[1] = EResult.tstParName;
    order[2] = EResult.instanceIDParName;
    order[3] = EResult.timeStampName;
    order[4] = EResult.passParName;
    
    int k = FIXNUM;
    for (int i = 0; i < orderA.length; i++) 
      order[k++] = orderA[i];    
    for (int i = 0; i < orderB.length; i++) 
      order[k++] = orderB[i];
    
    return order;
  }

  
  

}
