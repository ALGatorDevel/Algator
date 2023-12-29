package si.fri.algator.execute;

import si.fri.algator.entities.Variables;

/**
 * Abstract class to be extended by project-dependant TestCaseGenerators.
 * @author tomaz
 */
public abstract class AbstractTestCaseGenerator {
  public abstract AbstractTestCase generateTestCase(Variables generatingParameters);
}
