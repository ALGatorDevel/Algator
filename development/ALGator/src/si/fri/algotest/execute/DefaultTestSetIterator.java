package si.fri.algotest.execute;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import si.fri.algotest.entities.EResult;
import si.fri.algotest.entities.ETestSet;
import si.fri.algotest.entities.Project;
import si.fri.algotest.global.ATLog;
import si.fri.algotest.global.ErrorStatus;
import si.fri.algotest.tools.UniqueIDGenerator;

/**
 * DefaultTestSetIterator provides the methods to iterate throught  testsets in which 
 * each line of description file describes exactly one testcase. 
 * The project-dependant testsetiterator that extends this 
 * class has only to implement the getCurrent() method, which parses the current 
 * input line (currentInputLine) and produces corresponding testcase.    
 * @author tomaz
 */
public class DefaultTestSetIterator  extends AbstractTestSetIterator {

  private   Scanner inputSource;       // scanner used to iterate throught the Description file
  protected String  testFileName;      // the name of the file this iterator reads from
  protected String  filePath;          // path of a description file
  
  protected int     lineNumber;        // the number of the current line
  protected String  currentInputLine;  // the current input line 


  AbstractTestCase  testCaseInstance;
  
  public DefaultTestSetIterator(Project project, ETestSet testSet) {
    super(project, testSet);
  }

      
  
  // This method is used to report an error in input file
  protected void reportInvalidDataFormat(String note) {
    String msg = String.format("Invalid input data in file %s in line %d.", testFileName, lineNumber);
    if (!note.isEmpty())
      msg += " ("+note+")";
    
    ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR,msg);
  }

  @Override
  /**
   * The constructor of this class was changed and initIterator() has to be called manually!
   */
  public void initIterator() {
    if (testSet != null) {
      String fileName = testSet.getTestSetDescriptionFile();

      try {
        if (fileName == null) 
	  throw new Exception("Testset descritpion file does not exist.");
      
        filePath = testSet.entity_rootdir;      
        testFileName = filePath + File.separator + fileName;
      
        inputSource = new Scanner(new File(testFileName));
	lineNumber=0;        
      } catch (Exception e) {
	inputSource = null;
        ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR_INVALID_TESTSET, e.toString());
      } 
    }
  }

  @Override
  public boolean hasNext() {
    return (inputSource != null && inputSource.hasNextLine());
  }
  
  @Override
  public void readNext() {
    if (inputSource == null || !inputSource.hasNextLine()) {
      ErrorStatus.setLastErrorMessage(ErrorStatus.ERROR, "No more input to read!");
    }
    
    currentInputLine = inputSource.nextLine(); 
    if (currentInputLine == null) currentInputLine = "";
    lineNumber++;
  }

  @Override
  public boolean readTest(int testNumber) {
    if (lineNumber > testNumber) {
      try {
        inputSource.close();
        inputSource = new Scanner(new File(testFileName));
	lineNumber=0;
      } catch (Exception e) {
	inputSource = null;	
	ATLog.log("Error reading test with number " + testNumber+ "! Error: " + e.toString(), 2);
        return false;
      } 
    }
    
    while (testNumber > lineNumber && hasNext())
      readNext();
    
    return testNumber == lineNumber;
  }

  @Override
  public AbstractTestCase getCurrent() {
    AbstractTestCase testCase = null;
    try {
      if (testCaseInstance == null)
        testCaseInstance = New.testCaseInstance(project);
      
      testCase = testCaseInstance.getTestCase(currentInputLine, filePath);
      if (testCase == null) {
        reportInvalidDataFormat("");
        return null;
      }  
      
      // iterator labels each test with an unique label (which will probably be overriden by callers)      
      testCase.getInput().getParameters().addVariable(EResult.getInstanceIDParameter(UniqueIDGenerator.getNextID()), true);                              
    } catch (Exception e) {
      reportInvalidDataFormat("can not create testCase instance");     
    }
    return testCase;   
  }
 
  
    
 @Override
  public void close() throws IOException {
    inputSource.close();
  }
  

}
