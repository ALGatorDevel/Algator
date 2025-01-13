package si.fri.algator.execute;

import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.TESTS_PATH;
import static si.fri.algator.execute.AbstractTestCase.TESTSET_NAME;
import si.fri.algator.global.ATGlobal;

/**
 * Abstract class to be extended by project-dependant TestCaseGenerators.
 * @author tomaz
 */
public abstract class AbstractTestCaseGenerator {
  public abstract AbstractTestCase generateTestCase(Variables generatingParameters);
  
  public String getCommonResourcesPath(Variables generatingParameters) {
    String testsPath = generatingParameters.getVariable(TESTS_PATH, "").getStringValue();
    return ATGlobal.getTESTSETRecourcesFilename(testsPath);
  }
  
  public String getTestsetResourcesPath(Variables generatingParameters) {
    String testsPath   = generatingParameters.getVariable(TESTS_PATH, "").getStringValue();
    String testsetName = generatingParameters.getVariable(TESTSET_NAME, "").getStringValue();
    return ATGlobal.getTESTSETRecourcesFilename(testsPath, testsetName);
  }

}
