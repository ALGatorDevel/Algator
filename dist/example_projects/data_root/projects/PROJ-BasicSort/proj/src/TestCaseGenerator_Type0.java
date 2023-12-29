
import java.util.Arrays;
import java.util.Random;
import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.PROPS;
import si.fri.algator.execute.AbstractTestCaseGenerator;
import si.fri.algator.global.ErrorStatus;


/**
 * Type0 (default) test case generator. 
 * Generator creates a random test case of size N with given DIST
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type0 extends AbstractTestCaseGenerator {
  @Override
  public TestCase generateTestCase(Variables generatingParameters) {
    int n = generatingParameters.getVariable("N", 0).getIntValue();
    if (n < 1) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Number to small");
      return null;
    }
    String dist = generatingParameters.getVariable("DIST", "RND").getStringValue();

    // prepare an array of integers of a given size
    int[] array = new int[n];

    switch (dist) {
      case "RND":
        Random rnd = new Random(System.currentTimeMillis());
        for (int i = 0; i < n; i++) {
          array[i] = Math.abs(rnd.nextInt(1000000));
        }
        break;
      case "SOR":
        for (int i = 0; i < n; i++) {
          array[i] = i;
        }
        break;
      case "INV":
        for (int i = 0; i < n; i++) {
          array[i] = n - i;
        }
        break;
    }

    // create a test case and set ...
    TestCase basicSortTestCase = new TestCase();

    // ... the input
    basicSortTestCase.setInput(new Input(array));

    // ... the input parameters (for the Type0 generator, the set of TestCaseParameters equals to the set of GeneratingParameters)
    Variables testcaseParameters = new Variables(generatingParameters);
    // add properties
    testcaseParameters.addProperty(PROPS, "Type", "Type0");
    basicSortTestCase.getInput().setParameters(testcaseParameters);

    // ... and the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new Output(expectedResultArray));

    return basicSortTestCase;
  }
}
