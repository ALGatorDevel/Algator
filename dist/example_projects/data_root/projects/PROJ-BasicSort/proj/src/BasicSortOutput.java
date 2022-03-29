import java.util.Arrays;
import si.fri.algotest.execute.AbstractOutput;
import si.fri.algotest.execute.AbstractTestCase;

/**
 * A sort-project specific Output
 * @author tomaz
 */
public class BasicSortOutput extends AbstractOutput {

  /**
   * The result of the algorithm - a sorted array
   */
  public int [] sortedArray;

  public BasicSortOutput() {}

  
  public BasicSortOutput(int [] data) {
    sortedArray = data;
  }
  
  
  @Override
  public String toString() {
    return super.toString() + ", Data: " + basicsort.Tools.intArrayToString(sortedArray);
  }
  
  
  
  @Override
  protected Object getIndicatorValue(AbstractTestCase testCase, 
          AbstractOutput algorithmOutput, String indicatorName) 
  {
    BasicSortTestCase basicSortTestCase        = (BasicSortTestCase) testCase;
    BasicSortOutput   basicSortAlgorithmOutput = (BasicSortOutput) algorithmOutput;

    switch (indicatorName) {
      case "Check" :
        boolean checkOK = 
           Arrays.equals(basicSortAlgorithmOutput.sortedArray, 
                         basicSortTestCase.getExpectedOutput().sortedArray);
        return checkOK ? "OK" : "NOK";
    }
    
    return null;
  }

}
