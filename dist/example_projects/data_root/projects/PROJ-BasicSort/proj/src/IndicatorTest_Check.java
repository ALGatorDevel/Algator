import java.util.Arrays;
import si.fri.algator.execute.AbstractIndicatorTest;

/**
 * IndicatorTest for "Check" indicator of BasicSort project.
 * 
 * @author tomaz
 */
public class IndicatorTest_Check extends AbstractIndicatorTest<TestCase, Output> {

  @Override
  public Object getValue(TestCase testCase, Output algorithmOutput) {
    return Arrays.equals(algorithmOutput.sortedArray, testCase.getExpectedOutput().sortedArray)
             ? "OK" : "NOK";
  }
}
