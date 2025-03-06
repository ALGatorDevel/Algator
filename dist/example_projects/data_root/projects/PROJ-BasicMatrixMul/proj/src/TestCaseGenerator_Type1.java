import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.TESTS_PATH;
import si.fri.algator.execute.AbstractTestCaseGenerator;


/**
 * Type1 test case generator. 
 * Generator creates a random testcase with a given seed in which A, B and C are set.
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type1 extends AbstractTestCaseGenerator {
  @Override
  public TestCase generateTestCase(Variables inputParameters) {
    String path  = inputParameters.getVariable(TESTS_PATH,    "")   .getStringValue();    
    int n        = inputParameters.getVariable("N").getIntValue();              
    int seed     = inputParameters.getVariable("Seed").getIntValue();            

    String testsetResourcesPath = getTestsetResourcesPath(inputParameters);
    TestCase testCase = Tools.randomTestCase(testsetResourcesPath, n, seed, true);
    testCase.getInput().setParameters(inputParameters);

    return testCase;
  }
}