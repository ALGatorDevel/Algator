/**
 *
 * @author tomaz
 */
public class JavaSortAlgorithm extends BasicSortAbsAlgorithm {

  @Override
  protected BasicSortOutput execute(BasicSortInput testCase) {
    BasicSortOutput result = new BasicSortOutput();

    execute(testCase.arrayToSort);    
    result.sortedArray = testCase.arrayToSort;
    
    return result;
  }
  
  public void execute(int[] data) {
    java.util.Arrays.sort(data);
  }

}