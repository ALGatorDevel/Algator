package si.fri.algotest.execute;

import java.io.Serializable;
import si.fri.algotest.entities.Variables;

/**
 * Test case = input + expected output of an  algorithm. 
 * 
 * @author tomaz
 */
public abstract class AbstractTestCase implements Serializable {
  
  private AbstractInput  input;
  private AbstractOutput expectedOutput;

  public AbstractInput getInput() {
    return input;
  }

  public void setInput(AbstractInput input) {
    this.input = input;
  }

  public AbstractOutput getExpectedOutput() {
    return expectedOutput;
  }

  public void setExpectedOutput(AbstractOutput expectedOutput) {
    this.expectedOutput = expectedOutput;
  }
  
  
  /**
   * Method is used to parse a string description  and to generate a corresponding test case. The
   * description of a test case is usually a line read from the test set's description file  
   * (TestSet.txt) in which one line describes one test case. The aim of this method is to parse 
   * a line and to create a corresponding input and output instances. The {@code path} 
   * gives the path of the test set description file (the root of all test set 
   * defining files; if a description line contrains a name of a file, this name
   * is usually relative to the path). 
   * Usuall implementation of this method creates a list of parameters that describe a test case
   * and calls the generateTestCase(parameters) method.
   */
  public abstract AbstractTestCase getTestCase(String testCaseDescriptionLine, String path);
  
  
  /**
   * Method generates a test case with given parameters. For example: if the 
   * parameters are "N=100" and "Group=RND" the method should generate a "random"
   * test case of size 100. 
   * This method is project dependant and is used a) as a helper method for the 
   * getTestCase() method and b) as a method called by the ALGator's evaluate
   * process to generate test cases for algorithm inspection and evaluation. 
   */
  public abstract AbstractTestCase generateTestCase(Variables parameters);
}
