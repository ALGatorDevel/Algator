import si.fri.algator.execute.AbstractIndicatorTest;

/**
 * IndicatorTest for "Check" indicator of BasicMatrixMul project.
 * 
 * @author tomaz
 */
public class IndicatorTest_Check extends AbstractIndicatorTest<TestCase, Output> {

  @Override
  public Object getValue(TestCase testCase, Output algorithmOutput) {
     if (testCase.getExpectedOutput().C != null)
          return Tools.matrixEquals(
                  testCase.getExpectedOutput().C, algorithmOutput.C
                ) ? "OK" : "NOK";
        else
          return Tools.checkCorrectness(
                  testCase.getInput().A, testCase.getInput().B, algorithmOutput.C, 20
                ) ? "PROB" : "NOK";     
  }
}
