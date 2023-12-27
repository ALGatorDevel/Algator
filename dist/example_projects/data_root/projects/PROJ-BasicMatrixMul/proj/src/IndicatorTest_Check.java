import si.fri.algator.execute.AbstractIndicatorTest;

/**
 * IndicatorTest for "Check" indicator of BasicMatrixMul project
 * @author tomaz
 */
public class IndicatorTest_Check extends AbstractIndicatorTest<BasicMatrixMulTestCase, BasicMatrixMulOutput> {

  @Override
  public Object getValue(BasicMatrixMulTestCase testCase, BasicMatrixMulOutput algorithmOutput) {
     if (testCase.getExpectedOutput().C != null)
          return BasicMatrixMulTools.matrixEquals(
                  testCase.getExpectedOutput().C, algorithmOutput.C
                ) ? "OK" : "NOK";
        else
          return BasicMatrixMulTools.checkCorrectness(
                  testCase.getInput().A, testCase.getInput().B, algorithmOutput.C, 20
                ) ? "PROB" : "NOK";     
  }
}
