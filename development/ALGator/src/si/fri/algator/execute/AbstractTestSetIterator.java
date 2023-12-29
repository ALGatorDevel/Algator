package si.fri.algator.execute;

import java.io.IOException;
import si.fri.algator.entities.ETestSet;
import si.fri.algator.entities.Project;

/**
 *
 * @author tomaz
 */
public abstract class AbstractTestSetIterator  {

  protected ETestSet  testSet;
  protected Project  project;

  public AbstractTestSetIterator(Project project, ETestSet testSet) {
    this.project = project;
    this.testSet = testSet;    
  }
    
  // Initiates the iterator (i.e. opens files, sets envoronment, ...) so that
  // the first call to hasNext() and next() will iterate through the test set data
  public abstract void initIterator();
  
  /**
   * Returns true if this iterator has another test case 
   */
  public abstract boolean hasNext();
  
  /**
   * Reads the next test case (as a raw data)
   */
  public abstract void readNext();
  
  
  /**
   * Reads the i-th test case (as a raw data) and returns true if no error occures
   * or false otherwise (i.e. if i > number_of_all_tests).
   */
  public abstract boolean readTest(int testNumber);
  
  
  /**
   * Creates a new TestCase object for a raw data read by readNext() mathod. 
   * Consecutive calls to getCurrent() method must return different objects constructed
   * from the same input data.
   */
  public abstract AbstractTestCase getCurrent();
  
  /**
   * Closes the iterator source.
   */
  public abstract void close() throws IOException;
  
  public int getNumberOfTestInstances() {
    return (this.testSet != null) ? (Integer) testSet.getFieldAsInt(ETestSet.ID_N, 0) : 0;
  }

}
