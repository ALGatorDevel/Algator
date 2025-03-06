import si.fri.algator.entities.Variables;
import si.fri.algator.execute.AbstractTestCaseGenerator;

/**
 * Type0 (default) test case generator. 
 * Generator creates a random test case of size N.
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type0 extends AbstractTestCaseGenerator {
  @Override
  public TestCase generateTestCase(Variables inputParameters) {
    int n        = inputParameters.getVariable("N",        1000).getIntValue(); 
    
    String testsetResourcesPath = getTestsetResourcesPath(inputParameters);
    TestCase testCase = Tools.randomTestCase(testsetResourcesPath, n, -1, true);
    testCase.getInput().setParameters(inputParameters);

    return testCase;
  }

}