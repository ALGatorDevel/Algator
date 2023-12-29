import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.TESTS_PATH;
import si.fri.algator.execute.AbstractTestCaseGenerator;
import si.fri.algator.global.ErrorStatus;

/**
 * Type3 test case generator. 
 * Generator reads input matrices (A and B) and output matrix (C) from files. 
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type3 extends AbstractTestCaseGenerator {
  @Override
  public TestCase generateTestCase(Variables inputParameters) {
    String path      = inputParameters.getVariable(TESTS_PATH,    "")   .getStringValue();              
    int N            = inputParameters.getVariable("N",        1000).getIntValue();              

    String filenameA = inputParameters.getVariable("FilenameA", "").getStringValue();              
    String filenameB = inputParameters.getVariable("FilenameB", "").getStringValue();              
    String filenameC = inputParameters.getVariable("FilenameC", "").getStringValue();              
    
    int [][] A = Tools.readMatrixS(path, filenameA);
    int [][] B = Tools.readMatrixS(path, filenameB);
    int [][] C = Tools.readMatrixS(path, filenameC);
                    
    if (A == null || B == null || C == null) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Invalid parameters, can not generate matrices.");
      return null;
    }
               
    TestCase basicMatrixMulTestCase = new TestCase();                
    basicMatrixMulTestCase.setInput(new Input(A, B));    
    basicMatrixMulTestCase.getInput().setParameters(inputParameters);    
    basicMatrixMulTestCase.setExpectedOutput(new Output(C));
    
    return basicMatrixMulTestCase;
  }
}