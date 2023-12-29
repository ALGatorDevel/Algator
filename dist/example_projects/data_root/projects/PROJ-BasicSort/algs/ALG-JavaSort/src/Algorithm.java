/**
 *
 * @author tomaz
 */
public class Algorithm extends ProjectAbstractAlgorithm {

  @Override
  protected Output execute(Input testCase) {
    Output result = new Output();

    execute(testCase.arrayToSort);    
    result.sortedArray = testCase.arrayToSort;
    
    return result;
  }
  
  public void execute(int[] data) {
    java.util.Arrays.sort(data);
  }

}