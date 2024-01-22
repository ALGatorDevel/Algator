package si.fri.algator.execute;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import si.fri.algator.entities.EGenerator;
import si.fri.algator.entities.ETestCase;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Project;
import si.fri.algator.entities.Variables;
import si.fri.algator.global.ErrorStatus;

/**
 * Test case = input + expected output of an  algorithm. 
 * 
 * @author tomaz
 */
public abstract class AbstractTestCase implements Serializable {
  
  // name of the properties field in a test case
  public static final String PROPS = ETestCase.TESTCASE_PROPS;
  
  public static final String TESTS_PATH = "_tests_path_";
  
  private AbstractInput  input;
  private AbstractOutput expectedOutput;
  
  // used to hold indicators of the project
  private AbstractOutput defaultOutput;

  public AbstractTestCase() {
    defaultOutput = new AbstractOutput() {};
  }
  
  

  public AbstractInput getInput() {
    return input;
  }

  public void setInput(AbstractInput input) {
    this.input = input;
  }

  public AbstractOutput getExpectedOutput() {
    return expectedOutput;
  }

  public void setExpectedOutput(AbstractOutput expectedOutput) {
    this.expectedOutput = expectedOutput;
  }
  
  public AbstractOutput getDefaultOutput() {
    return defaultOutput;
  }
  
  
  public  AbstractTestCase getTestCase(Project project, String testCaseDescriptionLine, String path) {   
    String[] parts = testCaseDescriptionLine.split(":");
    
    // which type of generator should be used to generate test case? default: TYPE0
    String type = ETestCase.defaultGeneratorType;
    if (parts.length > 0) type = parts[0];
    if (type.isEmpty()) type = ETestCase.defaultGeneratorType;
    
    HashMap<String, EGenerator> generators = project.getTestCaseDescription().getGenerators();    
    if (generators.containsKey(type)) {
      EGenerator gen = generators.get(type);
      if (gen!= null) {
        String [] genPar = gen.getGeneratingParameters();
                
        Variables generatingParameters = new Variables();
        generatingParameters.setVariable(TESTS_PATH, path);        
        if (parts.length > 1) 
          generatingParameters.setVariable("Test", parts[1]);
        
        // at least one Property of TC_PROPS is always defined (i.e. the Type property)
        generatingParameters.addProperty(PROPS, "Type", type);
        
        Variables pars = project.getTestCaseDescription().getParameters();        
        if (genPar != null) for (int i = 0; i < genPar.length; i++) {
          String paramValue = null; if (parts.length > 2+i)  paramValue = parts[2+i]; 
          EVariable param   = pars.getVariable(genPar[i]);
          if (param != null){
            // če v seznamu parametrov obstaja parameter z imenom trenutnega "generating parametra", potem
            // ustvarim klon parametra iz seznama in mu dodalim vrednost (na ta način v klon prenesem tudi 
            // metadata in s tem mehanizem določanja privzetih vrednosti)
            EVariable newParam = new EVariable(genPar[i], param.getType(), null);
            newParam.setMetaData(param.getMetaData()); 
            if (paramValue==null || paramValue.isEmpty()) 
              newParam.setValue(param.getDefaultValue());
            else 
              newParam.setValue(paramValue);
            generatingParameters.addVariable(newParam); 
          } else
            generatingParameters.setVariable(genPar[i], paramValue); 
        }
        return generateTestCase(type, generatingParameters);
      } else {
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Invalid generator definition for type " + type);
        return null;
      }
    } else {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Invalid generator type: " + type);
      return null;
    }
  }
  
  
  /**
   * Method generates a test case with given parameters. For example: if the 
   * parameters are "N=100" and "Group=RND" the method should generate a "random"
   * test case of size 100. 
   * This method is project dependant and is used a) as a helper method for the 
   * getTestCase() method and b) as a method called by the ALGator's evaluate
   * process to generate test cases for algorithm inspection and evaluation. 
   */
  public AbstractTestCase generateTestCase(String type, Variables generatingParameters) {
    try {
      ClassLoader cl =  this.getClass().getClassLoader(); // get classloader that loaded testcase
      Class generatorClass = Class.forName("TestCaseGenerator_" + type, true, cl);
      AbstractTestCaseGenerator ait = (AbstractTestCaseGenerator)generatorClass.newInstance();
      Method generateTestCase = generatorClass.getDeclaredMethod("generateTestCase", generatingParameters.getClass());
      return (AbstractTestCase) generateTestCase.invoke(ait, generatingParameters);
    } catch (Exception e) {
      return null;
    }
  }
} 