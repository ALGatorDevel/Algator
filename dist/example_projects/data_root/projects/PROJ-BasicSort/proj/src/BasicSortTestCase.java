
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Random;
import si.fri.algotest.entities.EVariable;
import si.fri.algotest.entities.Variables;
import si.fri.algotest.execute.AbstractTestCase;
import si.fri.algotest.global.ErrorStatus;

/**
 *
 * @author tomaz
 */
public class BasicSortTestCase extends AbstractTestCase {

  @Override
  public BasicSortInput getInput() {
    return (BasicSortInput) super.getInput();
  }

  @Override
  public BasicSortOutput getExpectedOutput() {
    return (BasicSortOutput) super.getExpectedOutput();
  }

  @Override
  // Default testCase generator. If gets the values of all the parameters (N and DIST)
  public BasicSortTestCase testCaseGenerator(Variables generatingParameters) {
    int    n        = generatingParameters.getVariable("N", 0).getIntValue();
    if (n < 1) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "Number to small");
      return null;
    }    
    
    String dist    = generatingParameters.getVariable ("DIST", "RND").getStringValue();

    // prepare an array of integers of a given size
    int[] array = new int[n];

    switch (dist) {
      case "RND":
        Random rnd = new Random(System.currentTimeMillis());
        for (int i = 0; i < n; i++) 
          array[i] = Math.abs(rnd.nextInt(1000000));        
        break;
      case "SOR":
        for (int i = 0; i < n; i++) 
          array[i] = i;
        break;
      case "INV":
        for (int i = 0; i < n; i++) 
          array[i] = n - i;
        break;
    }
    
    // create a test case and set ...
    BasicSortTestCase basicSortTestCase = new BasicSortTestCase();

    // ... the input
    basicSortTestCase.setInput(new BasicSortInput(array));

    // ... the input parameters (for the Type0 generator, the set of TestCaseParameters equals to the set of GeneratingParameters)
    Variables testcaseParameters = new Variables(generatingParameters);    
    // add properties
    testcaseParameters.addProperty(PROPS, "Type", "Type0");    
    basicSortTestCase.getInput().setParameters(testcaseParameters);


    // ... and the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new BasicSortOutput(expectedResultArray));

    return basicSortTestCase;

  }
  
  @Override
  // a generator that reads number from v given file (starting at the offest-th number). 
  public BasicSortTestCase testCaseGenerator1(Variables generatingParameters) {
    String path     = generatingParameters.getVariable("Path", "").getStringValue();
    int    n        = generatingParameters.getVariable("N", 0).getIntValue();
    String filename = generatingParameters.getVariable("Filename", "").getStringValue();
    int offset      = generatingParameters.getVariable("Offset", 0).getIntValue();
    
    // prepare an array of integers of a given size
    int[] array = new int[n];

    String testFile = path + File.separator + filename;
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
    BasicSortTestCase basicSortTestCase = new BasicSortTestCase();

    // ... the input
    basicSortTestCase.setInput(new BasicSortInput(array));
    
    // ... the input parameters
    Variables testcaseParameters = new Variables();
    testcaseParameters.addVariable(new EVariable("N", n)); 
    testcaseParameters.addVariable(new EVariable("DIST", "FILE"));
    testcaseParameters.addProperty(PROPS, "Type", "Type1");        
    testcaseParameters.addProperty(PROPS, "Filename", filename);
    testcaseParameters.addProperty(PROPS, "Offset", offset);
    basicSortTestCase.getInput().setParameters(testcaseParameters);

    // ... and the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new BasicSortOutput(expectedResultArray));

    return basicSortTestCase;

  }

  @Override
  public BasicSortTestCase testCaseGenerator2(Variables generatingParameters) {
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
    BasicSortTestCase basicSortTestCase = new BasicSortTestCase();

    // ... the input
    basicSortTestCase.setInput(new BasicSortInput(array));

    // ... the input parameters
    Variables testcaseParameters = new Variables();
    testcaseParameters.addVariable(new EVariable("N", n)); 
    testcaseParameters.addVariable(new EVariable("DIST", "DATA"));
    testcaseParameters.addProperty(PROPS, "Type", "Type2");        
    basicSortTestCase.getInput().setParameters(testcaseParameters);

    // ... and the expected output
    int[] expectedResultArray = array.clone();
    Arrays.sort(expectedResultArray);
    basicSortTestCase.setExpectedOutput(new BasicSortOutput(expectedResultArray));

    return basicSortTestCase;
  }
  
  
}
