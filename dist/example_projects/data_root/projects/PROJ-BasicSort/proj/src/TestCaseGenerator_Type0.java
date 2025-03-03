import java.util.Arrays;
import java.util.Random;
import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.PROPS;
import si.fri.algator.execute.AbstractTestCaseGenerator;
import si.fri.algator.global.ErrorStatus;


/** 
 * Generator creates a random test case of size N with given DIST.
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type0 extends AbstractTestCaseGenerator {
  // Random integers will be taken from the range [0, MAX_RND).
  private static int MAX_RND = 100_000;
  
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
        // random sequence of integers 
        Random rnd = new Random(System.currentTimeMillis());
        for (int i = 0; i < n; i++) {
          array[i] = Math.abs(rnd.nextInt(MAX_RND));
        }
        break;
      // an already ordered sequence
      case "SOR":
        for (int i = 0; i < n; i++) {
          array[i] = i;
        }
        break;
      // an inversely ordered sequence
      case "INV":
        for (int i = 0; i < n; i++) {
          array[i] = n - i;
        }
        break;
    }

    // Create a test case ...
    TestCase basicSortTestCase = new TestCase();

    // ... and set the input,
    basicSortTestCase.setInput(new Input(array));

    // ... the input parameters, ...
    Variables testcaseParameters = new Variables(generatingParameters);
    // ... and the properties.
    testcaseParameters.addProperty(PROPS, "Type", "Type0");
    basicSortTestCase.getInput().setParameters(testcaseParameters);

    // Set the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new Output(expectedResultArray));

    return basicSortTestCase;
  }
}