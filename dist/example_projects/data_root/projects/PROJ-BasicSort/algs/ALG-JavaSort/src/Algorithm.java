/**
 *
 * @author tomaz
 */

public class Algorithm extends ProjectAbstractAlgorithm {
  @Override
  protected Output execute(Input testCase) {
    int[] array = testCase.arrayToSort.clone();
    java.util.Arrays.sort(array);     
    return new Output(array);
  }
}