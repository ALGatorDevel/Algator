import si.fri.algotest.execute.AbstractInput;

/**
 * A sort-project specific Input
 * @author tomaz
 */
public class BasicSortInput extends AbstractInput {

  /**
   * An array of data to be sorted
   */
  public int [] arrayToSort;

  
  public BasicSortInput(int [] data) {    
    this.arrayToSort = data;
  }
  
  
  
  @Override
  public String toString() {
    // Note that we use a method intArrayToString that was defined in the basicsort.Tools
    // class; this class was attached to the project using the "ProjectJARs" property
    // in the BasicSort.atp configuration file.
    // For the details about basicsort.Tools class see proj/lib folder.
    return super.toString() + ", Data: " + basicsort.Tools.intArrayToString(arrayToSort);
  }
}
