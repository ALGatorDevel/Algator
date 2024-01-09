package si.fri.algator.entities;

import java.io.File;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algator.global.ErrorStatus;


/**
 * A TestCase entity (attc file)
 * 
 * @author tomaz (2019)
 */
public class ETestCase extends Entity {  
  // Entity identifier
  public static final String ID_TestCase       ="TestCase";
  
  //Fields
  public static final String ID_inputParameters    = "InputParameters";	// String []
  public static final String ID_parameters          = "Parameters";             // EVariable []
  public static final String ID_generators          = "Generators";             // EGenerator []
  
  // The name of a test case PROPS parameter
  public static final String TESTCASE_PROPS         = "TC_PROPS";

  public static final String defaultGeneratorType   = "Type0";
  
  
  private Variables parameters;
  HashMap<String, EGenerator> generators;

  
   
  public ETestCase() {
     super(ID_TestCase, 
	 new String [] {ID_inputParameters, ID_parameters, ID_generators});
         set(ID_parameters, new JSONArray());
         
     export_name= false; // don't export 'name' property (since it is always 'testcase')
  }
  
  public ETestCase(File fileName) {
    this();
    initFromFile(fileName);    
  }
  
  public ETestCase(String json) {
    this();
    initFromJSON(json);
  }
    
  
  public String[] getInputParameters() {
    return ArrayUtils.add(getStringArray(ID_inputParameters), TESTCASE_PROPS);
  }
  
  public Variables getParameters() {  
    if (parameters != null) return parameters;
    
    try {
      Variables result = new Variables();      
      parameters = new Variables();
      
      JSONArray ja = getField(ID_parameters);
      if (ja != null) for (int i = 0; i < ja.length(); i++) {
	JSONObject jo = ja.getJSONObject(i);
        EVariable var = new EVariable(jo.toString());
	result.addVariable(var, true);
        parameters.addVariable(var, true);
      }
      
      result.addVariable(new EVariable(TESTCASE_PROPS,VariableType.JSONSTRING, ""));
            
      // Add all undefined parameters - default type for undefined parameter is INT
      String [] parameters = getStringArray(ID_inputParameters);
      for (String indicatorName : parameters) {
        if (result.getVariable(indicatorName) == null)
          result.addVariable(new EVariable(indicatorName, indicatorName, VariableType.INT, 0), true);
      }
                        
      this.parameters = result;      
      
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_NOT_A_RESULTPARAMETER_ARRAY, ID_parameters);
      
      this.parameters =  new Variables();
    }
    
    return this.parameters;
  }
  
  
 public HashMap<String, EGenerator> getGenerators() {  
    if (generators != null) return generators;

    generators = new HashMap<>();    
    try {
      JSONArray ja = getField(ID_generators);
      if (ja != null) for (int i = 0; i < ja.length(); i++) {
	JSONObject jo = ja.getJSONObject(i);
        EGenerator gen = new EGenerator(jo.toString());     
        String type = defaultGeneratorType;
        if (gen != null && gen.get(EGenerator.ID_Type) != null && gen.get(EGenerator.ID_Type) instanceof String)
          type = (String) gen.get(EGenerator.ID_Type);
	generators.put(type, gen);
      }      
      
      // ce dafault generator ni definiran v attc datoteki, ga dodam avtomatsko!
      if (!generators.containsKey(defaultGeneratorType)) {
        EGenerator gen = new EGenerator();
        gen.set(EGenerator.ID_Type, defaultGeneratorType);
        gen.set(EGenerator.ID_GPars, get(ID_inputParameters));
        generators.put(defaultGeneratorType, gen);
      }
    } catch (Exception e) {
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_NOT_A_GENERATORS_ARRAY, ID_generators);
    }    
    return this.generators;
  }  
}


