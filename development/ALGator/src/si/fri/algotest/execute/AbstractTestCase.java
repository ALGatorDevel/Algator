package si.fri.algotest.execute;

import java.io.Serializable;
import java.util.HashMap;
import si.fri.algotest.entities.EGenerator;
import si.fri.algotest.entities.ETestCase;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.Project;
import si.fri.algotest.entities.Variables;
import si.fri.algotest.global.ErrorStatus;

/**
 * Test case = input + expected output of an  algorithm. 
 * 
 * @author tomaz
 */
public abstract class AbstractTestCase implements Serializable {
  
  private AbstractInput  input;
  private AbstractOutput expectedOutput;

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
  
  
  public  AbstractTestCase getTestCase(Project project, String testCaseDescriptionLine, String path) {
    String[] parts = testCaseDescriptionLine.split(":");
    
    // which type of generator should be used to generate test case? default: TYPE0
    String type = ETestCase.defaultGeneratorType;
    if (parts.length > 0) type = parts[0].toUpperCase();
    if (type.isEmpty()) type = ETestCase.defaultGeneratorType;
    
    HashMap<String, EGenerator> generators = project.getTestCaseDescription().getGenerators();
    if (generators.containsKey(type)) {
      EGenerator gen = generators.get(type);
      if (gen!= null) {
        String [] genPar = gen.getGeneratingParameters();
        
        Variables inputParameters = new Variables();
        inputParameters.setVariable("Path", path);
        if (parts.length > 1) 
          inputParameters.setVariable("Test", parts[1]);
        
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
            inputParameters.addVariable(newParam); 
          } else
            inputParameters.setVariable(genPar[i], param); 
        }
        return generateTestCase(type, inputParameters);
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
  public AbstractTestCase generateTestCase(String type, Variables parameters) {
    switch (type) {
      case ETestCase.defaultGeneratorType: return testCaseGenerator (parameters);         
      case "TYPE1"                       : return testCaseGenerator1(parameters); 
      case "TYPE2"                       : return testCaseGenerator2(parameters); 
      case "TYPE3"                       : return testCaseGenerator3(parameters); 
      case "TYPE4"                       : return testCaseGenerator4(parameters); 
      case "TYPE5"                       : return testCaseGenerator5(parameters); 
      case "TYPE6"                       : return testCaseGenerator6(parameters); 
      case "TYPE7"                       : return testCaseGenerator7(parameters); 
      case "TYPE8"                       : return testCaseGenerator8(parameters);       
      case "TYPE9"                       : return testCaseGenerator9(parameters);       
    }
    return null;
  }
  
  public AbstractTestCase testCaseGenerator (Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator1(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator2(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator3(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator4(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator5(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator6(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator7(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator8(Variables parameters){return null;}
  public AbstractTestCase testCaseGenerator9(Variables parameters){return null;}
} 