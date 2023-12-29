
import java.util.Arrays;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.PROPS;
import si.fri.algator.execute.AbstractTestCaseGenerator;
import si.fri.algator.global.ErrorStatus;

/**
 * Type2  test case generator. 
 * Generator generates input array from a given inline data 
 * (string with space-separated int values)
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type2 extends AbstractTestCaseGenerator {
  @Override
  public TestCase generateTestCase(Variables generatingParameters) {
    int    n        = generatingParameters.getVariable("N", 1).getIntValue();
    int[] array = new int[n];    
    
    String inline = generatingParameters.getVariable("Data", "").getStringValue();

    generatingParameters.addVariable(new EVariable("DIST", "INLINE"));


    if (inline.isEmpty()) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Invalid inline data value");  
      return null;
    }
    String data[] = inline.split(" ");
    if (data.length < n) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Not enough numbers in data");          
      return null;
    }
    try {
      for (int i = 0; i < n; i++) {
        array[i] = Integer.parseInt(data[i]);
      }
    } catch (Exception e) {
      return null;
    }

    // create a test case and set ...
    TestCase basicSortTestCase = new TestCase();

    // ... the input
    basicSortTestCase.setInput(new Input(array));

    // ... the input parameters
    Variables testcaseParameters = new Variables();
    testcaseParameters.addVariable(new EVariable("N", n)); 
    testcaseParameters.addVariable(new EVariable("DIST", "DATA"));
    testcaseParameters.addProperty(PROPS, "Type", "Type2");        
    basicSortTestCase.getInput().setParameters(testcaseParameters);

    // ... and the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new Output(expectedResultArray));

    return basicSortTestCase;
  }
}
