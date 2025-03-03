import si.fri.algator.execute.AbstractOutput;

/**
 * A sort-project-specific output.
 * 
 * @author tomaz
 */
public class Output extends AbstractOutput {

  /**
   * The result of the algorithm.
   */
  public int [] sortedArray;

  public Output() {}
  
  public Output(int [] data) {
    sortedArray = data;
  }
  
  @Override
  public String toString() {
    return super.toString() + ", Data: " + basicsort.Tools.intArrayToString(sortedArray);
  }
}