import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import si.fri.algator.entities.EVariable;
import si.fri.algator.entities.Variables;
import static si.fri.algator.execute.AbstractTestCase.PROPS;
import static si.fri.algator.execute.AbstractTestCase.TESTS_PATH;
import si.fri.algator.execute.AbstractTestCaseGenerator;
import si.fri.algator.global.ErrorStatus;

/**
 * The generator reads n numbers from a given file, starting at a specified offset.
 *
 * @author tomaz
*/
public class TestCaseGenerator_Type1 extends AbstractTestCaseGenerator {
  @Override
  public TestCase generateTestCase(Variables generatingParameters) {
    String commonResourcesPath  = getCommonResourcesPath (generatingParameters);
    String testsetResourcesPath = getTestsetResourcesPath(generatingParameters);
    
    int    n        = generatingParameters.getVariable("N", 0).getIntValue();
    String filename = generatingParameters.getVariable("Filename", "").getStringValue();
    int offset      = generatingParameters.getVariable("Offset", 0).getIntValue();
    
    // prepare an array of integers of a given size
    int[] array = new int[n];

    String testFile = commonResourcesPath + File.separator + filename;
    try {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(testFile))));

      // skip the first "offset" numbers
      dis.skipBytes(offset * 4);

      int i = 0;
      while (i < n && dis.available() > 0) {
        array[i++] = dis.readInt();
      }
      if (i < n) {
        return null;
      }
      dis.close();
    } catch (Exception e) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Error reading file " + testFile);
      return null;
    }
    

    // create a test case and set ...
    TestCase basicSortTestCase = new TestCase();

    // ... the input
    basicSortTestCase.setInput(new Input(array));
    
    // ... the input parameters
    Variables testcaseParameters = new Variables(generatingParameters);
    testcaseParameters.addVariable(new EVariable("N", n)); 
    testcaseParameters.addVariable(new EVariable("DIST", "FILE"));
    testcaseParameters.addProperty(PROPS, "Type", "Type1");        
    testcaseParameters.addProperty(PROPS, "Filename", filename);
    testcaseParameters.addProperty(PROPS, "Offset", offset);
    basicSortTestCase.getInput().setParameters(testcaseParameters);

    // ... and the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new Output(expectedResultArray));

    return basicSortTestCase;
  }
}