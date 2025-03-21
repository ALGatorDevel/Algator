import si.fri.algator.execute.AbstractInput;

/**
 * A sort-project-specific input.
 * 
 * @author tomaz
 */
public class Input extends AbstractInput {

  /**
   * An array of data to be sorted.
   */
  public int [] arrayToSort;

  
  public Input(int [] data) {    
    this.arrayToSort = data;
  }
  
  @Override
  public String toString() {
    // Note that we use a method intArrayToString that was defined in the basicsort.Tools
    // class; this class was attached to the project using the "ProjectJARs" property
    return super.toString() + ", Data: " + basicsort.Tools.intArrayToString(arrayToSort);
  }
}